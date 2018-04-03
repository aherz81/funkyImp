package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Rational;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type.DomainConstraint;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;
import java.util.*;

class Domains {

    private static boolean containsNonZeroEntry(Iterable<java.util.List<Pair<Integer,VarSymbol>>> xs) {
        for(java.util.List<Pair<Integer,VarSymbol>> i : xs) {
			for (Pair<Integer, VarSymbol> co : i)
	            if(co.fst != 0) return true;
        }
        return false;
    }

    /** Tests if a constraint is fullfilled. */
    public static boolean checkConstraint(DomainConstraint constraint, Map<Name, JCTree.JCExpression> varenv) {

        // calculate left side
        int left = 0;
        for(Map.Entry<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>> e : constraint.coeffs.entrySet()) {
            if(! varenv.containsKey(e.getKey().name)) {
                // cannot check constraint, since not all values are known
                return true;
            }
			for(Pair<Integer,VarSymbol> co:e.getValue())
				left += co.fst * (Integer)varenv.get(e.getKey().name).type.constValue();
        }

        if(constraint.eq) {
            return left == constraint.constant;
        } else {
            return left <= constraint.constant;
        }

    }

    /** Transform a constraint into the representation needed by Simplex. */
    private static Simplex.Constraint transformConstraint(DomainConstraint c,
            int numIndices, Map<VarSymbol, Integer> varToPos) {

        Rational[] coeffs = new Rational[numIndices];
        Arrays.fill(coeffs, Rational.ZERO);
        Rational constant = new Rational(c.constant);

        for(Map.Entry<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>> e : c.coeffs.entrySet()) {
			for (Pair<Integer, VarSymbol> co : e.getValue())
			{
				int coeff = co.fst;
				int pos = varToPos.get(e.getKey());
				coeffs[pos] = coeffs[pos].add(new Rational(coeff));
			}
        }

        return new Simplex.Constraint(coeffs, constant);

    }

    /** Transform a ist of constraints into the representation needen by Simplex. */
    private static List<Simplex.Constraint> tranformConstraints(List<DomainConstraint> cs,
            List<VarSymbol> vars) {

        // make map from vars to positions
        int numVars = vars.length();
        Map<VarSymbol, Integer> varToPos = new LinkedHashMap<VarSymbol, Integer>();
        for(int i = 0; i < numVars; i++) {
            varToPos.put(vars.get(i), i);
        }

        // transform to simplex representation
        ListBuffer<Simplex.Constraint> constrBuffer = new ListBuffer<Simplex.Constraint>();
        for(DomainConstraint c : cs) {
            Simplex.Constraint cc = transformConstraint(c, numVars, varToPos);
            constrBuffer.add(cc);
            if(c.eq)
                constrBuffer.add(cc.negate());
        }

        return constrBuffer.toList();
    }

    /** Remove constraints which contain only constants. */
    public static List<DomainConstraint> removeConstantConstraints(List<DomainConstraint> cs) {

        ListBuffer<DomainConstraint> csWithoutConst = new ListBuffer<DomainConstraint>();

        for(DomainConstraint c : cs) {

            // at least one coefficient is not zero: constraint is useful
            if(containsNonZeroEntry(c.coeffs.values())) {
                 csWithoutConst.add(c);
            }

            // equality constraints with no coefficients must have a constant = 0
            else if(c.eq && c.constant != 0) {
                System.out.println("    ERROR: vec = (0) but constant != 0");
                return null;
            }

            // inequality constraints with no coefficients must have a constant >= 0
            else if(!c.eq && c.constant < 0) {
                System.out.println("    ERROR: vec = (0) but constant < 0");
                return null;
            }

            // otherwise the onstraint is valid, but not useful, we simply ignore it
            else {}
        }

        return csWithoutConst.toList();
    }

    /** Returns if a LE constraint is redundant. Does not work on EQ constraints */
    private static boolean isDomConstraintRedundantLE(Simplex.Constraint constraint,
            List<Simplex.Constraint> leCs, List<Simplex.Constraint> eqCs) {

        // convert EQ constraints to LE constraits
        ListBuffer<Simplex.Constraint> cs = new ListBuffer<Simplex.Constraint>();
        cs.addAll(leCs);
        for(Simplex.Constraint c : eqCs) {
            cs.add(c);
            cs.add(c.negate());
        }

        return isDomConstraintRedundantLE(constraint, cs.toList());

    }

    /** Returns if a LE constraint is redundant. Does not work on EQ constraints */
    private static boolean isDomConstraintRedundantLE(Simplex.Constraint constraint,
            List<Simplex.Constraint> cs) {

        // optimize
        Simplex.Result result = Simplex.performOptimization(constraint.coeffs, cs);

        // if cs represents an empty domain => additional constraint is redundant
        if(result.isEmpty) {
            return true;
        }
        // if maxVal of constraint is not bound in cs, it is not redundant
        // (since this is equal to maxVal = +INF)
        else if(result.isUnbound) {
            return false;
        }
        // otherwise compare values
        else {
            return result.maxVal.compareTo(constraint.constant) <= 0;
        }

    }

    public static boolean areAllIndicesBound(List<DomainConstraint> cs, List<VarSymbol> vars) {

        int numIndices = vars.length();
        List<Simplex.Constraint> constraints = tranformConstraints(cs, vars);

        // test that all indices are bounded
        for(int i = 0; i < numIndices; i++) {

            Rational[] indexCostraint = new Rational[numIndices];
            Arrays.fill(indexCostraint, Rational.ZERO);
            indexCostraint[i] = Rational.ONE;

            Simplex.Result result = Simplex.performOptimization(indexCostraint, constraints);
            if(result.isUnbound) {
                //System.out.println("        index (" + vars.get(i) + ") is unbounded (maxVal == null)");
                return false;
            } else if(result.isEmpty) {
                //System.out.println("        index (" + vars.get(i) + ") has no valid value since the domain is empty");
            } else {
                //System.out.println("        index (" + vars.get(i) + ") is bounded by " + maxVal);
            }

        }

        return true;

    }

    /** Removes all constraints which are redundant */
    public static List<DomainConstraint> removeRedundantConstraints(
            List<VarSymbol> vars, List<DomainConstraint> cs) {
        return removeRedundantConstraints(vars, cs, Collections.<DomainConstraint>emptySet());

    }

    /** Removes all constraints which are redundant. Safe constraints cannot be removed. */
    public static List<DomainConstraint> removeRedundantConstraints(
            List<VarSymbol> vars, List<DomainConstraint> cs, Set<DomainConstraint> safeset) {

        //System.out.println("removeRedundantconstraints:  " + cs + " (safeset = " + safeset + ")");

        // 1. get all used indices and put them into an arbitrary order
        Map<VarSymbol, Integer> varToPos = new LinkedHashMap<VarSymbol, Integer>();
        int numIndices = vars.length();
        for(int i = 0; i < vars.length(); i++) {
            varToPos.put(vars.get(i), i);
        }
        //System.out.println("    numIndices = " + numIndices);

        // 2. Convert constraints to representation used by simplex
        Map<Simplex.Constraint, DomainConstraint> reprToConstraint = new LinkedHashMap<Simplex.Constraint, DomainConstraint>();
        ListBuffer<Simplex.Constraint> leConstrains = new ListBuffer<Simplex.Constraint>();
        ListBuffer<Simplex.Constraint> eqConstrains = new ListBuffer<Simplex.Constraint>();
        for(DomainConstraint c : cs) {
            Simplex.Constraint cc = transformConstraint(c, numIndices, varToPos);
            reprToConstraint.put(cc, c);
            if(c.eq)
                eqConstrains.add(cc);
            else
                leConstrains.add(cc);
        }
        //System.out.println("    leConstraints = " + leConstrains);
        //System.out.println("    eqConstraints = " + eqConstrains);

        // 3. remove redundant constraints by using the simplex algorithm

        // constraints to be tested
        List<Simplex.Constraint> leCsToBeSearched = leConstrains.toList();
        List<Simplex.Constraint> eqCsToBeSearched = eqConstrains.toList();

        // needed costraints are put into these lists
        ListBuffer<Simplex.Constraint> leCsNeeded = new ListBuffer<Simplex.Constraint>();
        ListBuffer<Simplex.Constraint> eqCsNeeded = new ListBuffer<Simplex.Constraint>();

        // test LE constraints
        while(! leCsToBeSearched.isEmpty()) {

            // take constraint
            Simplex.Constraint c = leCsToBeSearched.head;
            leCsToBeSearched = leCsToBeSearched.tail;

            // redundancy is checked compared to unchecked and needed constraints,
            // constraints already declared redundant are ignored to avoid cycles
            List<Simplex.Constraint> otherLeCs = leCsToBeSearched.appendList(leCsNeeded);

            // test for redunancy using simplex
            boolean isRedunant = isDomConstraintRedundantLE(c, otherLeCs, eqConstrains.toList());

            // put into list
            if(! isRedunant || safeset.contains(reprToConstraint.get(c))) leCsNeeded.add(c);

        }

        // test EQ constraints
        while(! eqCsToBeSearched.isEmpty()) {

            // take constraint
            Simplex.Constraint c = eqCsToBeSearched.head;
            eqCsToBeSearched = eqCsToBeSearched.tail;

            // redundancy is checked compare to unchecked and needed constraints,
            // constraints already declaed redundant are ignored to avoid cycles
            List<Simplex.Constraint> otherEqCs = eqCsToBeSearched.appendList(eqCsNeeded);

            // test for redunancy using simplex
            boolean isNeededLE = ! isDomConstraintRedundantLE(c, leCsNeeded.toList(), otherEqCs);
            boolean isNeededGE = ! isDomConstraintRedundantLE(c.negate(), leCsNeeded.toList(), otherEqCs);

            // put into list
            if(isNeededLE || isNeededGE || safeset.contains(reprToConstraint.get(c))) {
                eqCsNeeded.add(c);
            }

        }

        // convert back to DomainConstraint
        ListBuffer<DomainConstraint> neededConstraints = new ListBuffer<DomainConstraint>();
        for(Simplex.Constraint sc : leCsNeeded) {
            neededConstraints.add(reprToConstraint.get(sc));
        }
        for(Simplex.Constraint sc : eqCsNeeded) {
            neededConstraints.add(reprToConstraint.get(sc));
        }

        //System.out.println("    needed LE constraits: " + leCsNeeded);
        //System.out.println("    needed EQ constraits: " + eqCsNeeded);
        //System.out.println("    needed constraints: " + neededConstraints);

        return neededConstraints.toList();

    }

    private static void swapRows(Rational[][] M, int row1, int row2) {
        int width = M[row1].length;
        for(int i = 0; i < width; i++) {
            Rational tmp = M[row1][i];
            M[row1][i] = M[row2][i];
            M[row2][i] = tmp;
        }
    }

    private static boolean isRowZero(Rational[][] M, int row) {
        int width = M[row].length;
        for(int i = 0; i < width; i++) {
            if(! M[row][i].equals(Rational.ZERO))
                return false;
        }
        return true;
    }

    private static void divideRowBy(Rational[][] M, int row, Rational factor) {
        int width = M[row].length;
        for(int i = 0; i < width; i++) {
            M[row][i] = M[row][i].divide(factor);
        }
    }

    private static void subtractMultipliedRow(Rational[][] M, int rowTo, int rowFrom, Rational factor) {
        int width = M[rowTo].length;
        for(int i = 0; i < width; i++) {
            M[rowTo][i] = M[rowTo][i].subtract(M[rowFrom][i].multiply(factor));
        }
    }

    private static void printMatrixAB(Rational[][] A, Rational[][] B) {
        int h = A.length;
        int wA = A[0].length;
        int wB = B[0].length;
        System.out.println("----------");
        for(int i = 0; i < h; i++) {
            System.out.print("[");
            for(int j = 0; j < wA; j++) {
                System.out.print(A[i][j] + ",");
            }
            System.out.print(" | ");
            for(int j = 0; j < wB; j++) {
                System.out.print(B[i][j] + ",");
            }
            System.out.println("],");
        }
        System.out.println("----------");
    }

    public static boolean isProjectionDomainTranslated(
            List<VarSymbol> indices, List<VarSymbol> args, List<DomainConstraint> cs) {

        // If site is a projection domain, we have to show that all versions of
        // this projection have the same shape and only differ by their position.
        // This is done by showing that all constraints move by the same vector.
        // However we have to ensure that only constraits which are really part of
        // of the polyhedron are considered. Redudent ones have to be ignored because
        // they do not have to move in he same way. We only remove redundant constraints
        // containing indices. Also constraints only containing k-variables (=projection
        // arguments) can be ignored.

        // there are two classes of constraints:
        //
        // - constraints containing only k's
        //   -> these restrict the k-space and are eliminated for a specific k
        //   -> ignore them for movement
        //   -> removeredundant ones only if they are redundant wrt. other
        //      k-only-constraints
        //
        // - constraints containing both x's and k's
        //   -> these restrict x's dependent on k's
        //   -> they have to move correctly
        //   -> they can be redundant -> remove redundant ones
        //   -> use them in the algorithm to test this movement

        // 1. sort constraints by class
        ListBuffer<DomainConstraint> kCsBuf = new ListBuffer<DomainConstraint>(); // only k's
        ListBuffer<DomainConstraint> xCsBuf = new ListBuffer<DomainConstraint>(); // x's and k's
        for(DomainConstraint c : cs) {
            int numX = 0;
            for(VarSymbol v : c.coeffs.keySet()) {
                if(indices.contains(v)) numX++;
            }
            if(numX > 0) xCsBuf.add(c);
            else kCsBuf.add(c);
        }

        // 2. get all used indices and put them into an arbitrary order
        List<VarSymbol> allVars = indices.appendList(args);
        Map<VarSymbol, Integer> varToPos = new LinkedHashMap<VarSymbol, Integer>();
        int numIndices = allVars.length();
        for(int i = 0; i < allVars.length(); i++) {
            varToPos.put(allVars.get(i), i);
        }

        // 3. Convert constraints to representation used by simplex
        Map<Simplex.Constraint, DomainConstraint> reprToConstraint = new LinkedHashMap<Simplex.Constraint, DomainConstraint>();
        ListBuffer<Simplex.Constraint> kSimplexCsBuf = new ListBuffer<Simplex.Constraint>();
        ListBuffer<Simplex.Constraint> xSimplexCsBuf = new ListBuffer<Simplex.Constraint>();
        for(DomainConstraint c : kCsBuf) {
            Simplex.Constraint cc = transformConstraint(c, numIndices, varToPos);
            reprToConstraint.put(cc, c);
            kSimplexCsBuf.add(cc);
            if(c.eq) kSimplexCsBuf.add(cc.negate());
        }
        for(DomainConstraint c : xCsBuf) {
            Simplex.Constraint cc = transformConstraint(c, numIndices, varToPos);
            reprToConstraint.put(cc, c);
            xSimplexCsBuf.add(cc);
            if(c.eq) xSimplexCsBuf.add(cc.negate());
        }
        List<Simplex.Constraint> kSimplexCs = kSimplexCsBuf.toList();
        List<Simplex.Constraint> xSimplexCs = xSimplexCsBuf.toList();

        // 4. ensure that all x-only-Constraints are redundant
        ListBuffer<Simplex.Constraint> neededXBuf = new ListBuffer<Simplex.Constraint>();
        while(xSimplexCs.nonEmpty()) {

            // get next x constraint
            Simplex.Constraint c = xSimplexCs.head;
            xSimplexCs = xSimplexCs.tail;

            // redundancy is checked compared to unchecked and needed constraints,
            // constraints already declared redundant are ignored to avoid cycles
            List<Simplex.Constraint> otherCs =
                    kSimplexCs.appendList(xSimplexCs).appendList(neededXBuf);

            // test for redunancy using simplex
            boolean isRedunant = isDomConstraintRedundantLE(c, otherCs);

            // collect all non-redundant x-constraints
            if(! isRedunant) neededXBuf.add(c);

        }
        xSimplexCs = neededXBuf.toList();

        // 5. Add non-negativity constraints here if they are needed.
        // Since Simplex alwyas assumes non-negativity redundancychecking
        // may be difficult.
        // Maybe moving domain away from zero may help? (All x->x+1)

        // create copies of constraints moved by 1 in each X direction
        ListBuffer<Simplex.Constraint> csPlus1 = new ListBuffer<Simplex.Constraint>();
        for(Simplex.Constraint c : xSimplexCs.appendList(kSimplexCs)) {
            Rational[] newCoeffs = new Rational[c.coeffs.length];
            System.arraycopy(c.coeffs, 0, newCoeffs, 0, c.coeffs.length);
            Rational newConst = c.constant;
            for(VarSymbol v : indices) {
                int pos = varToPos.get(v);
                Rational a = c.coeffs[pos];
                newConst = newConst.add(a);
            }
            csPlus1.add(new Simplex.Constraint(newCoeffs, newConst));
        }

        // test for each index if "X >= 1" is redundant (-x <= -1)
        ListBuffer<Simplex.Constraint> nonNegConstraints = new ListBuffer<Simplex.Constraint>();
        for(VarSymbol v : indices) {
            // make test constraint
            int pos = varToPos.get(v);
            Rational[] coeffs = new Rational[numIndices];
            Arrays.fill(coeffs, Rational.ZERO);
            coeffs[pos] = Rational.MINUSONE;
            Simplex.Constraint c = new Simplex.Constraint(coeffs, Rational.MINUSONE);
            // test for redundancy
            boolean isRedundant = isDomConstraintRedundantLE(c, csPlus1.toList());
            // add x >= 0 to nonNegConstraints (-x <= 0)
            if(! isRedundant) {
                Simplex.Constraint nonNegConstr = new Simplex.Constraint(coeffs.clone(), Rational.ZERO);
                nonNegConstraints.add(nonNegConstr);
            }
        }
        // add nonNegConstraints to needed constraints
        xSimplexCs = xSimplexCs.appendList(nonNegConstraints);

        // 6. From now on, only kx-constraints are needed.
        //    Create Matrices A and mB = -B
        int numCs = xSimplexCs.length();
        int n = indices.length();
        int m = args.length();
        Rational[][] A = new Rational[numCs][n];
        Rational[][] mB = new Rational[numCs][m];
        for(int i = 0; i < numCs; i++) {
            Simplex.Constraint c = xSimplexCs.get(i);
            // fill A
            for(int j = 0; j < n; j++) {
                VarSymbol v = indices.get(j);
                int varpos = varToPos.get(v);
                A[i][j] = c.coeffs[varpos];
            }
            // fill B
            for(int j = 0; j < m; j++) {
                VarSymbol v = args.get(j);
                int varpos = varToPos.get(v);
                mB[i][j] = c.coeffs[varpos].negate();
            }
        }

        // 7. now we have to solve A*dx = mB*k for dx
        //    -> do gauss-elimination to transform A -> 1
        //    -> then dx = mB' * k
        //    -> mB' is our translation matrix T

        if(numCs < n) {
            // cannot do gauss elimination for
            //System.out.println("what now???");
            return false;
        }

        // try to put 1 in A[i][i]
        for(int i = 0; i < n; i++) {

            //printMatrixAB(A, mB);

            // find pivotRow >= i row with A[pivot][i] != 0
            int pivotRow = i;
            while(A[pivotRow][i].equals(Rational.ZERO)) {
                pivotRow++;
                if(pivotRow >= numCs) {
                    // cannot do gauss elimination for
                    //System.out.println("what now???");
                    return false;
                }
            }

            // swap pivot row with row i
            if(pivotRow != i) {
                swapRows(A, i, pivotRow);
                swapRows(mB, i, pivotRow);
            }
            //printMatrixAB(A, mB);

            // normalize row
            Rational d = A[i][i];
            divideRowBy(A, i, d);
            divideRowBy(mB, i, d);
            //printMatrixAB(A, mB);

            // subtract row from other rows
            for(int row = 0; row < numCs; row++) {
                if(row == i) continue;
                Rational f = A[row][i];
                subtractMultipliedRow(A, row, i, f);
                subtractMultipliedRow(mB, row, i, f);
            }
            //printMatrixAB(A, mB);

        }

        //printMatrixAB(A, mB);

        // A may have rows at the end which consist only of zeroes
        // we have to check, that B is also zero there
        for(int i = 0; i < numCs; i++) {
            if(isRowZero(A, i) && ! isRowZero(mB, i)) {
                //System.out.println("inconsistent zero row found!");
                return false;
            }
        }

        // all components B have to be integral
        for(int i = 0; i < numCs; i++) {
            for(int j = 0; j < m; j++) {
                if(! mB[i][j].isInt()) {
                    //System.out.println("non integral value in translation matrix!");
                    return false;
                }
            }
        }

        // make translation matrix T
        Rational[][] T = new Rational[n][m];
        for(int i = 0; i < n; i++) {
            System.arraycopy(mB[i], 0, T[i], 0, m);
        }

        return true;


    }

    /** Move the domain as close as possible to the origin. (by integral values only) */
    public static List<DomainConstraint> moveDomainToOrigin(List<VarSymbol> vars, List<DomainConstraint> cs) {

        // 1. Convert constraints to representation used by simplex
        List<Simplex.Constraint> constraints = tranformConstraints(cs, vars);

        // 2. calculate how far we can move into the direction of variable v
        // (distance values should be positive)
        Map<VarSymbol, Integer> moveDist = new LinkedHashMap<VarSymbol, Integer>();
        for(int i = 0; i < vars.length(); i++) {
            Rational[] indexCostraint = new Rational[vars.length()];
            Arrays.fill(indexCostraint, Rational.ZERO);
            indexCostraint[i] = Rational.MINUSONE;
            Simplex.Result result =  Simplex.performOptimization(indexCostraint, constraints);
            if(result.isEmpty) {
                // domain is empty
                moveDist.put(vars.get(i), 0);
            }
            else if(result.isUnbound) {
                // this should never happen
                throw new RuntimeException("Unbound solution in moveDomainToOrigin()");
            }
            else {
                Rational minVal = result.maxVal.negate();
                moveDist.put(vars.get(i), minVal.toInt());
            }
        }

        // 3. transform constraints (this works for both LE and EQ constraints)
        ListBuffer<DomainConstraint> transformedConstraints = new ListBuffer<DomainConstraint>();
        for(DomainConstraint c : cs) {
            DomainConstraint transformed = c.deepCopy();
            for(VarSymbol v : vars) {
				if(c.coeffs.get(v)!=null)
				for (Pair<Integer, VarSymbol> co : c.coeffs.get(v))
				{
					int coeff = co.fst;
					transformed.constant -= coeff * moveDist.get(v);
				}
            }
            transformedConstraints.add(transformed);
        }

        return transformedConstraints.toList();
    }

    /** Returns if two domains are equal. */
    public static boolean areDomainsEqual(List<VarSymbol> vars, List<DomainConstraint> cs1, List<DomainConstraint> cs2) {

        // 1. Convert constraints to representation used by simplex
        List<Simplex.Constraint> simplexConstr1 = tranformConstraints(cs1, vars);
        List<Simplex.Constraint> simplexConstr2 = tranformConstraints(cs2, vars);

        // 2. two domains are equal if every constraint is redundant with regard to
        //    the set of consraints of the other domain
        for(Simplex.Constraint c : simplexConstr1) {
            boolean r = isDomConstraintRedundantLE(c, simplexConstr2);
            if(! r) return false;
        }
        for(Simplex.Constraint c : simplexConstr2) {
            boolean r = isDomConstraintRedundantLE(c, simplexConstr1);
            if(! r) return false;
        }
        return true;

    }

    /** Tests if all points in site appear in exactly one projection.
     */
    public static boolean testProjectionIterationPoints(
            final List<VarSymbol> siteVars,
            final List<DomainConstraint> siteCs,
            final List<VarSymbol> projVars,
            final List<VarSymbol> projArgs,
            final List<DomainConstraint> projCs) {

        // 1. Convert constraints to representation used by simplex
        List<Simplex.Constraint> siteSimplexCs =
                tranformConstraints(siteCs, siteVars);
        List<Simplex.Constraint> projSimplexCs =
                tranformConstraints(projCs, projVars.appendList(projArgs));

        // 2. calculate min and max values for each x-dimension of SITE
        int numVars = siteVars.length();
        int numArgs = projArgs.length();
        int[] maxBounds = new int[numVars + numArgs];
        int[] minBounds = new int[numVars + numArgs];
        for(int i = 0; i < numVars; i++) {
            Rational[] f = new Rational[numVars];
            Arrays.fill(f, Rational.ZERO);
            f[i] = Rational.ONE;
            Simplex.Result maxRes = Simplex.performOptimization(f, siteSimplexCs);
            f[i] = Rational.MINUSONE;
            Simplex.Result minRes = Simplex.performOptimization(f, siteSimplexCs);
            if(maxRes.isEmpty || minRes.isEmpty) {
                // domain is empty -> no points availale -> just return
                System.out.println("site domain empty");
                return true;
            }
            else if(maxRes.isUnbound || minRes.isUnbound) {
                // domain is unbound -> cannot iterate over all points
                System.out.println("site domain unbound");
                return false;
            }
            else {
                // maxval has to be rounded down to integer
                maxBounds[i] = maxRes.maxVal.toInt();
                // minval has to be rounded up
                Rational min = minRes.maxVal.negate();
                int minInt = min.isInt() ? min.toInt() : (min.toInt() + 1);
                minBounds[i] = minInt;
            }
        }

        // 3. calculate min and max values for each k-dimension of the PROJECTION
        for(int i = 0; i < numArgs; i++) {
            Rational[] f = new Rational[numVars + numArgs];
            Arrays.fill(f, Rational.ZERO);
            f[numVars + i] = Rational.ONE;
            Simplex.Result maxRes = Simplex.performOptimization(f, projSimplexCs);
            f[numVars + i] = Rational.MINUSONE;
            Simplex.Result minRes = Simplex.performOptimization(f, projSimplexCs);
            if(maxRes.isEmpty || minRes.isEmpty) {
                // domain is empty -> no points availale -> just return
                System.out.println("proj domain empty");
                return false; // site isalreadyknown to be not empty
            }
            else if(maxRes.isUnbound || minRes.isUnbound) {
                // domain is unbound -> cannot iterate over all points
                System.out.println("proj domain unbound");
                return false;
            }
            else {
                // maxval has to be rounded down to integer
                maxBounds[numVars + i] = maxRes.maxVal.toInt();
                // minval has to be rounded up
                Rational min = minRes.maxVal.negate();
                int minInt = min.isInt() ? min.toInt() : (min.toInt() + 1);
                minBounds[numVars + i] = minInt;
            }
        }

        // 4. make structures to store errors
        boolean[] errElementNotFound = { false };
        boolean[] errElementNotUnique = { false };

        // store values of coords here
        int[] fixedVars = new int[numVars + numArgs];
        Arrays.fill(fixedVars, -1);
        testProjectionIterationPoints_siteRec(
                siteVars.toArray(new VarSymbol[0]),
                projVars.appendList(projArgs).toArray(new VarSymbol[0]),
                0, fixedVars, siteCs, projCs,
                minBounds, maxBounds,
                errElementNotFound, errElementNotUnique);

        // if an element was not fond, it has to be part of two projections
        if(errElementNotFound[0]) {
            System.out.println("overlapping projections!");
            return false;
        }
        // if set is not empty, some pointsare on no projection
        if(errElementNotUnique[0]) {
            System.out.println("element(s) not in projection!");
            return false;
        }

        return true;
    }

    private static void testProjectionIterationPoints_siteRec(
            VarSymbol[] siteVars, VarSymbol[] projVars,
            int thisVar, int[] fixedVars, // -1 for not yet fixed vars
            List<DomainConstraint> siteCs,
            List<DomainConstraint> projCs,
            int[] minBounds, int[] maxBounds,
            boolean[] errElementNotFound, boolean[] errElementNotUnique) {

        // test if we already reached the deepest level
        if(thisVar >= siteVars.length) {

            // Now we have a point in the site domain with coords 'fixedVars'.
            // We can now check the number of points on the projection with
            // this x-coords.
            int[] foundCounter = { 0 };

            testProjectionIterationPoints_projRec(
                    projVars, thisVar, fixedVars,
                    projCs, minBounds, maxBounds,
                    foundCounter);

            if(foundCounter[0] == 0) {
                errElementNotFound[0] = true;
            }
            else if(foundCounter[0] > 1) {
                errElementNotUnique[0] = true;
            }

            return;

        }

        int min = minBounds[thisVar];
        int max = maxBounds[thisVar];
        ListBuffer<DomainConstraint> stillNeededConstraints = new ListBuffer<DomainConstraint>();

        // get constraints containing only the picked var and fied vars
        for(DomainConstraint c : siteCs) {

            // test if constraints is used at this level
            boolean usesFreeVar = false;
            int left = c.constant;
            for(int i = 0; i < siteVars.length; i++) {
                if(i == thisVar || ! c.coeffs.containsKey(siteVars[i])) continue;
                // test if constraint can be used here
                if(fixedVars[i] == -1) {
                    usesFreeVar = true;
                    break;
                }
                // add to left ("-" since it s moved to the left side)
				for (Pair<Integer, VarSymbol> co : c.coeffs.get(siteVars[i]))
	                left -= co.fst * fixedVars[i];
            }

            // if constraint is not used it is needen on a lower level
            if(usesFreeVar) {
                stillNeededConstraints.add(c);
                break;
            }

            // constraint can be used
            // divide by coefficient of thisVar
			for (Pair<Integer, VarSymbol> co : c.coeffs.get(siteVars[thisVar]))
			{
				int thisCoeff = co.fst;
				if(c.eq) {
					if(left % thisCoeff != 0) return; // no possible integral value
					int value = left / thisCoeff;
					if(value < min || value > max) return;
					min = value;
					max = value;
				}
				else if(thisCoeff > 0) {
					int cMax = left / thisCoeff;
					if(cMax <= max) max = cMax;
				}
				else if(thisCoeff < 0) {
					// switch from <= to >=
					left = -left;
					thisCoeff = -thisCoeff;
					int cMin = left / thisCoeff;
					if(left % thisCoeff != 0) cMin++;
					if(cMin >= min) min = cMin;
				}
			}
        }

        // make iteration
        for(int i = min; i <= max; i++) {
            fixedVars[thisVar] = i;
            testProjectionIterationPoints_siteRec(
                    siteVars, projVars, thisVar + 1, fixedVars,
                    stillNeededConstraints.toList(), projCs,
                    minBounds, maxBounds,
                    errElementNotFound, errElementNotUnique);
        }
        fixedVars[thisVar] = -1;

    }

    private static void testProjectionIterationPoints_projRec(
            VarSymbol[] projVars, int thisVar, int[] fixedVars, // -1 for not yet fixed vars
            List<DomainConstraint> projCs,
            int[] minBounds, int[] maxBounds,
            int[] foundCounter) {

        // test if we already reached the deepest level
        if(thisVar >= fixedVars.length) {
            // possible value found, increment counter
            foundCounter[0]++;
            return;
        }

        int min = minBounds[thisVar];
        int max = maxBounds[thisVar];
        ListBuffer<DomainConstraint> stillNeededConstraints = new ListBuffer<DomainConstraint>();

        // get constraints containing only the picked var and fied vars
        for(DomainConstraint c : projCs) {

            // test if constraints is used at this level
            boolean usesFreeVar = false;
            int left = c.constant;
            for(int i = 0; i < projVars.length; i++) {
                if(i == thisVar || ! c.coeffs.containsKey(projVars[i])) continue;
                // test if constraint can be used here
                if(fixedVars[i] == -1) {
                    usesFreeVar = true;
                    break;
                }
                // add to left ("-" since it s moved to the left side)
				for (Pair<Integer, VarSymbol> co : c.coeffs.get(projVars[i]))
					left -= co.fst * fixedVars[i];
            }

            // if constraint is not used it is needen on a lower level
            if(usesFreeVar) {
                stillNeededConstraints.add(c);
                break;
            }

            // projection constraints not depending on k is not fulfilled -> no point
            if(! c.coeffs.containsKey(projVars[thisVar])) {
                if(c.eq && left != 0)
                    return;
                else if(! c.eq && left < 0)
                    return;
                else
                    continue;
            }

            // constraint can be used
            // divide by coefficient of thisVar
			for (Pair<Integer, VarSymbol> co : c.coeffs.get(projVars[thisVar]))
			{
				int thisCoeff = co.fst;
				if(c.eq) {
					if(left % thisCoeff != 0) return; // no possible integral value
					int value = left / thisCoeff;
					if(value < min || value > max) return;
					min = value;
					max = value;
				}
				else if(thisCoeff > 0) {
					int cMax = left / thisCoeff;
					if(cMax <= max) max = cMax;
				}
				else if(thisCoeff < 0) {
					// switch from <= to >=
					left = -left;
					thisCoeff = -thisCoeff;
					int cMin = left / thisCoeff;
					if(left % thisCoeff != 0) cMin++;
					if(cMin >= min) min = cMin;
				}
			}
        }

        // make iteration
        for(int i = min; i <= max; i++) {
            fixedVars[thisVar] = i;
            testProjectionIterationPoints_projRec(
                    projVars, thisVar + 1, fixedVars,
                    projCs, minBounds, maxBounds,
                    foundCounter);
        }
        fixedVars[thisVar] = -1;

    }



}


