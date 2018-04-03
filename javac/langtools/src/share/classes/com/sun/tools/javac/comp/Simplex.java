package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Rational;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import java.util.Arrays;

class Simplex {
        
    public static class Result {
        public boolean isUnbound;
        public boolean isEmpty;
        public Rational maxVal;
    }
            
	public static class Constraint {
		
	    public final Rational[] coeffs;
	    public final Rational constant;
	    
	    public Constraint(Rational[] coefficients, Rational value) {
			this.coeffs   = coefficients;
			this.constant = value;
		}
        
        public Constraint negate() {
            Rational[] newCoeffs = coeffs.clone();
            for(int i = 0; i < newCoeffs.length; i++)
                newCoeffs[i] = newCoeffs[i].negate();
            return new Constraint(newCoeffs, constant.negate());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < coeffs.length; i++) {
                if(i != 0) sb.append(", ");
                sb.append(coeffs[i]);                
            }
            return "[" + sb + " | " + constant + "]";
        }
        
	}
	
    /** Default maximal number of iterations allowed. */
	private static final int MAX_ITERATIONS = 100;
    
    /** Number of iterations already performed. */
    private int iterations;
    
    /** Linear objective function. */
    private final Rational[] f;
    
    /** Linear constraints. */
    private final List<Constraint> constraints;
    
    /** Simple tableau. */
    private Rational[][] tableau; // [row][col]

    /** Number of decision variables. */
    private final int numDecisionVariables;

    /** Number of slack variables. */
    private final int numSlackVariables;

    /** Number of artificial variables. */
    private int numArtificialVariables;
    
    /** Set to indicate that the solution is unbounded. */
    private boolean unbounded = false;
    
    /** Calculate the greatest value of f. 
     *  Returns null if no largest value exists since the simplex solution is unbounded. */
    public static Result performOptimization(Rational[] f, List<Constraint> leConstraints) {
        
        //System.out.println("PERFORM SIMPLEX OPTIMIZATION ...");
        //System.out.println("f = " + Arrays.deepToString(f));
        //System.out.println("constraints = " + leConstraints);
        
		Simplex simplex = new Simplex(f, leConstraints);
        Result result = simplex.optimize();
        
        /*System.out.println("final tableau:");
        for(Rational[] row : simplex.tableau) {
        	System.out.print("[ ");
        	for(Rational r : row) {
            	System.out.print(r + ", ");
            }
        	System.out.println("]");
        }
        System.out.println("DONE. RESULT = " + result);*/
        
		return result;
	}
     
    /** Build a tableau for a linear problem. */
    private Simplex(Rational[] f, List<Constraint> constraints) {
        this.f                      = f;
        this.constraints            = constraints;
        this.numDecisionVariables   = f.length;
        this.numSlackVariables      = constraints.size();
        this.numArtificialVariables = numNeededArtificialVars(constraints);
        this.tableau = createTableau();        
    }
        
    /** Create the tableau by itself. */
    private Rational[][] createTableau() {
    	
        // create a matrix of the correct size
        int width = numDecisionVariables + numSlackVariables + numArtificialVariables + getNumObjectiveFunctions() + 1;
        int height = constraints.size() + getNumObjectiveFunctions();
        Rational[][] matrix = new Rational[height][width];
        for(Rational[] row : matrix) {
            Arrays.fill(row, Rational.ZERO);
        }

        // add -1 to tableau for artificial function
        if (getNumObjectiveFunctions() == 2) {
            matrix[0][0] = Rational.MINUSONE;
        }
        
        // write function to tableau
        int zIndex = (getNumObjectiveFunctions() == 1) ? 0 : 1;
        matrix[zIndex][zIndex] = Rational.ONE;
    	for (int i = 0; i < f.length; i++) {
            matrix[zIndex][getNumObjectiveFunctions() + i] = f[i].negate();
        }
        matrix[zIndex][width - 1] = Rational.ZERO;
        
        // initialize the constraint rows
        int slackVar = 0;
        int artificialVar = 0;
        for (int i = 0; i < constraints.size(); i++) {
            
            // get constraint
            Constraint c = constraints.get(i);
            Rational[] constrCoeffs = c.coeffs;
            Rational constrConst = c.constant;
            boolean constIsLEQ = true; //c.isLEQ;
            
            // normalize constraint
            if (constrConst.compareTo(Rational.ZERO) < 0) {
            	constrCoeffs = constrCoeffs.clone();
            	for(int j = 0; j < constrCoeffs.length; j++) {
            		constrCoeffs[j] = constrCoeffs[j].negate();
            	}
                constrConst = constrConst.negate();
                constIsLEQ = ! constIsLEQ;
            }            
            
            // get the row for this constraint
            int row = getNumObjectiveFunctions() + i;

            // copy coefficients to tableau
            System.arraycopy(constrCoeffs, 0, matrix[row], getNumObjectiveFunctions(), constrCoeffs.length);
            
            // copy constant to tableau
            matrix[row][width - 1] = constrConst;

            // slack variables
            int slackVariableOffset = getNumObjectiveFunctions() + numDecisionVariables;
            matrix[row][slackVariableOffset + slackVar++] = constIsLEQ ? Rational.ONE : Rational.MINUSONE;

            // artificial variables
            if (! constIsLEQ) {
                int artificialVarOffset = getNumObjectiveFunctions() + numDecisionVariables + numSlackVariables;
                matrix[0][artificialVarOffset + artificialVar] = Rational.ONE;
                matrix[row][artificialVarOffset + artificialVar++] = Rational.ONE;
                for(int k = 0; k < matrix[0].length; k++) {
                	matrix[0][k] = matrix[0][k].subtract(matrix[row][k]);
                }
            }
        }

        return matrix;
    }
    
    /** Calculate the optimized value. 
     *  Returns null if no largest value exists since the simplex solution is unbounded. */
    private Result optimize() {
    	
        iterations = 0;
        
        // phase 1: find base solution
        if (numArtificialVariables != 0) {
        
	        // optimize
            while(doIteration()) {}
            
            // test if solution was unbounded
            if(unbounded) {
                throw new RuntimeException("Unbounded solution in phase 1");
                //Result result = new Result();
                //result.isEmpty = false;
                //result.isUnbound = true;
                //result.maxVal = null;
                //return result;
            }
	        
	        // if W is not zero then we have no feasible solution
	        if (! tableau[0][getWidth() - 1].equals(Rational.ZERO)) {
                //throw new RuntimeException("No solution in phase 1");
                Result result = new Result();
                result.isEmpty = true;
                result.isUnbound = false;
                result.maxVal = null;
                return result;
            }
            
            // convert to phase 2 tableau
            dropPhase1Objective();
        
        }
        
        // phase 2: find optimal solution
        while(doIteration()) {}
       
        // test if solution was unbounded
        if(unbounded) {
            Result result = new Result();
            result.isEmpty = false;
            result.isUnbound = true;
            result.maxVal = null;
            return result;
        }
        
        // return value in "solution" field
        Result result = new Result();
        result.isEmpty = false;
        result.isUnbound = false;
        result.maxVal = tableau[0][getWidth() - 1];
        return result;
    }
    
    /** Runs one iteration of the Simplex method on the given model. 
     *  Returns true if an iteration was performed, false if the tableau 
     *  is already optimal or the solution is unbounded.
     */
    private boolean doIteration() {
        
    	// check for maximum number of iterations
    	if (++iterations > MAX_ITERATIONS) {
            throw new RuntimeException("Simplex aborted after " + MAX_ITERATIONS + " iterations");
        }
        
    	// get pivot column
        int pivotCol = getPivotColumn();
        if(pivotCol == -1) return false; // already optimal
        
        // get pivot row
        int pivotRow = getPivotRow(pivotCol);
        if(pivotRow == -1) {
            unbounded = true;
            return false;
        }

        // set the pivot element to 1
        Rational pivotVal = tableau[pivotRow][pivotCol];
        divideRow(pivotRow, pivotVal);
        
        // set the rest of the pivot column to 0
        for (int i = 0; i < getHeight(); i++) {
            if (i != pivotRow) {
            	Rational multiplier = tableau[i][pivotCol];
                subtractRow(i, pivotRow, multiplier);
            }
        }
        
        return true;
    }
    
    /** Returns the column with the most negative coefficient in the objective function row. 
     *  If the tableau is already optimal, no such column can be found -> return -1; */
    private int getPivotColumn() {
        Rational minValue = Rational.ZERO;
        int minPos = -1;
        // ignore columns for objective functions (there are two in phase 1) and the constant column
        for (int i = getNumObjectiveFunctions(); i < getWidth() - 1; i++) {
            final Rational entry = tableau[0][i];
            if (entry.compareTo(minValue) < 0) {
                minValue = entry;
                minPos = i;
            }
        }
        return minPos;
    }

    /** Returns the row with the minimum ratio as given by the minimum ratio test (MRT). 
     *  If no row pivot row can be found, the solution is unbonded -> return -1 */
    private int getPivotRow(final int col) {
        
        // create a list of all the rows that tie for the lowest score in the minimum ratio test
        ListBuffer<Integer> minRatioPositions = new ListBuffer<Integer>();
        Rational minRatio = Rational.MAX_VALUE;
        // ignore rows for objective functions (there are two in phase 1)
        for (int i = getNumObjectiveFunctions(); i < getHeight(); i++) {
            final Rational rhs = tableau[i][getWidth() - 1];
            final Rational entry = tableau[i][col];
            if (entry.compareTo(Rational.ZERO) > 0) {
                final Rational ratio = rhs.divide(entry);
                final int cmp = ratio.compareTo(minRatio);
                if (cmp == 0) {
                    minRatioPositions.add(i);
                } else if (cmp < 0) {
                    minRatio = ratio;
                    minRatioPositions = new ListBuffer<Integer>();
                    minRatioPositions.add(i);
                }
            }
        }

        // unbounded solution
        if (minRatioPositions.isEmpty()) 
            return -1;
        
		// there's a degeneracy as indicated by a tie in the minimum ratio test
		// check if there's an artificial variable that can be forced out of the basis
        int artificialVarOffset = getNumObjectiveFunctions() + numDecisionVariables + numSlackVariables;
		for (int row : minRatioPositions) {
			for (int i = 0; i < numArtificialVariables; i++) {
				int column = i + artificialVarOffset;
				final Rational entry = tableau[row][column];
				if (entry.equals(Rational.ONE) && row == getBasicRow(column)) {
					return row;
				}
			}
		}

        return minRatioPositions.toList().get(0);
    }
    
    /** Get the number of objective functions in this tableau.
     *  (2 for Phase 1.  1 for Phase 2.) */
    private int getNumObjectiveFunctions() {
        return this.numArtificialVariables > 0 ? 2 : 1;
    }
    
    /** Get a count of GEQ constraints. */
    private int numNeededArtificialVars(List<Constraint> cs) {
        int count = 0;
        for (Constraint c : cs) {  
            if (c.constant.compareTo(Rational.ZERO) < 0) {
                ++count;
            }
        }        
        return count; 
    }
    
    /**
     * Checks whether the given column is basic.
     * @return the row that the variable is basic in. null if the column is not basic
     */
    private Integer getBasicRow(final int col) {
        Integer row = null;
        for (int i = 0; i < getHeight(); i++) {
            final Rational entry = tableau[i][col];
            if (entry.equals(Rational.ONE) && (row == null)) {
                row = i;
            } else if (! entry.equals(Rational.ZERO)) {
                return null;
            }
        }
        return row;
    }

    /** Removes the phase 1 objective function, positive cost non-artificial variables,
     *  and the non-basic artificial variables from this tableau. */
    private void dropPhase1Objective() {

        ListBuffer<Integer> columnsToDrop = new ListBuffer<Integer>();
        columnsToDrop.add(0);

        // positive cost non-artificial variables
        int artificialVarOffset = getNumObjectiveFunctions() + numDecisionVariables + numSlackVariables;
        for (int i = getNumObjectiveFunctions(); i < artificialVarOffset; i++) {
            final Rational entry = tableau[0][i];
            if (entry.compareTo(Rational.ZERO) > 0) {
                columnsToDrop.add(i);
            }
        }

        // non-basic artificial variables
        for (int i = 0; i < numArtificialVariables; i++) {
            int col = i + artificialVarOffset;
            if (getBasicRow(col) == null) {
                columnsToDrop.add(col);
            }
        }

        Rational[][] matrix = new Rational[getHeight() - 1][getWidth() - columnsToDrop.size()];
        for (int i = 1; i < getHeight(); i++) {
            int col = 0;
            for (int j = 0; j < getWidth(); j++) {
                if (!columnsToDrop.contains(j)) {
                    matrix[i - 1][col++] = tableau[i][j];
                }
            }
        }
        
        this.tableau = matrix;
        this.numArtificialVariables = 0;
    }
    
    /** Subtracts a multiple of one row from another. 
     *  (minuendRow = dividendRow / divisor) */
    private void divideRow(final int dividendRow, final Rational divisor) {
        for (int j = 0; j < getWidth(); j++) {
            tableau[dividendRow][j] = tableau[dividendRow][j].divide(divisor);
        }
    }

    /** Subtracts a multiple of one row from another.
     *  (minuendRow = minuendRow - multiple * subtrahendRow) */
    private void subtractRow(final int minuendRow, final int subtrahendRow, final Rational multiple) {
    	for(int j = 0; j < getWidth(); j++) {
    		tableau[minuendRow][j] = tableau[minuendRow][j].subtract(tableau[subtrahendRow][j].multiply(multiple));
    	}
    }

    /** Get the width of the tableau. */
    private int getWidth() {
        return tableau[0].length;
    }

    /** Get the height of the tableau. */
    private int getHeight() { 
    	return tableau.length;
    }
           
}
