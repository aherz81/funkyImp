
package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.TypeTags.*;

import java.util.Hashtable;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;

import org.jgrapht.*;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;


/** Calcs global alias info.
*/

public class AliasGlobal extends TreeScanner {
    protected static final Context.Key<AliasGlobal> AliasGlobalKey =
        new Context.Key<AliasGlobal>();

    private final Names names;
    private final Log log;
    private final Symtab syms;
    private final Types types;
    private final Check chk;
    private       TreeMaker make;
    private       Lint lint;

    private BaseSymbol base;
    private Work work;

    private boolean useLinearityInfo;

    private JCMethodDecl recursion;

	private boolean inside_where=false;
	private boolean inside_return=false;

    enum LocalPass
    {
        FIRST,FINISHED
    }

    LocalPass pass;

    public static AliasGlobal instance(Context context) {
        AliasGlobal instance = context.get(AliasGlobalKey);
        if (instance == null)
            instance = new AliasGlobal(context);
        return instance;
    }

    protected AliasGlobal(Context context) {
        context.put(AliasGlobalKey, this);

        names = Names.instance(context);
        log = Log.instance(context);
        syms = Symtab.instance(context);
        types = Types.instance(context);
        chk = Check.instance(context);
        lint = Lint.instance(context);
		work = Work.instance(context);

    }

    Bits loc_refs;
    Bits loc_linear;

    JCMethodDecl method = null;

    ListBuffer<VarSymbol> refList;

    boolean enableRef;

    /** The set of variables that are definitely unassigned everywhere
     *  in current try block. This variable is maintained lazily; it is
     *  updated only when something gets removed from uninits,
     *  typically by being assigned in reachable code.  To obtain the
     *  correct set of variables which are definitely unassigned
     *  anywhere in current try block, intersect uninitsTry and
     *  uninits.
     */

    Bits loc_refsWhenTrue;
    Bits loc_refsWhenFalse;
    Bits loc_linearWhenTrue;
    Bits loc_linearWhenFalse;

    /** A mapping from addresses to variable symbols.
     */
    VarSymbol[] vars;

    /** The current class being defined.
     */
    JCClassDecl classDef;

    /** The first variable sequence number in this class definition.
     */
    int firstadr;

    /** The next available variable sequence number.
     */
    int nextadr;


    /** Set when processing a loop body the second time for DU analysis. */
    boolean loopPassTwo = false;

    Set<JCTree> done;


    /*-------------------- Environments ----------------------*/

    /*-------------------- Exceptions ----------------------*/

    /*-------------- Processing variables ----------------------*/

    /** Do we need to track init/uninit state of this symbol?
     *  I.e. is symbol either a local or a blank final variable?
     */
    boolean trackable(VarSymbol sym) {
        return
            (sym.owner.kind == MTH ||
             ((sym.flags() & (FINAL | HASINIT | PARAMETER)) == FINAL &&
				sym.owner instanceof ClassSymbol&&
              classDef.sym.isEnclosedBy((ClassSymbol)sym.owner)));
    }

    /** Initialize new trackable variable by setting its address field
     *  to the next available sequence number and entering it under that
     *  index into the vars array.
     */
    void newVar(VarSymbol sym) {
        if (nextadr == vars.length) {
            VarSymbol[] newvars = new VarSymbol[nextadr * 2];
            System.arraycopy(vars, 0, newvars, 0, nextadr);
            vars = newvars;
        }
        sym.adr = nextadr;
        vars[nextadr] = sym;

        loc_refs.excl(nextadr);
        loc_linear.incl(nextadr);

        nextadr++;
    }

    /** Record an initialization of a trackable variable.
     */
    void letInit(JCTree at,DiagnosticPosition pos, VarSymbol sym,boolean allowinout,Set<VarSymbol> s) {
		//no aliasing for output params
        if (!s.isEmpty()&&sym.adr >= firstadr && trackable(sym)&&((sym.flags()&Flags.FOUT)==0||(sym.flags()&Flags.PARAMETER)==0))
        {
			VarSymbol entry=sym;

			Set<VarSymbol> cs=new LinkedHashSet<VarSymbol>();
			cs.addAll(s);

			for(Iterator<VarSymbol> i=cs.iterator();i.hasNext();)
			{
				VarSymbol n=i.next();
				if(n.scope>=sym.scope&&(sym.flags_field&Flags.IMPLICITDECL)==0)
					i.remove();
			}

			if(sym.aliasMapLinear.get(entry)!=null)
				cs.addAll(sym.aliasMapLinear.get(entry));


			sym.aliasMapLinear.put(entry, cs);//this is THE actual symbol used

        }

    }

    /** If tree is either a simple name or of the form this.name or
     *  C.this.name, and tree represents a trackable variable,
     *  record an initialization of the variable.
     */
    void letInit(JCTree tree,boolean allowinout,Set<VarSymbol> s) {
        tree = TreeInfo.skipParens(tree);
        if (tree.getTag() == JCTree.IDENT || tree.getTag() == JCTree.SELECT) {
            Symbol sym = TreeInfo.symbol(tree);

            //take care of auto defined syms
            VarSymbol vsym=(VarSymbol)sym;
            if(vsym.adr==-1&&trackable(vsym))
            {
                newVar(vsym);
            }

            letInit(tree,tree.pos(), (VarSymbol)sym, allowinout,s);
        }
    }

    /*-------------------- Handling jumps ----------------------*/

    /** Split (duplicate) inits/uninits into WhenTrue/WhenFalse sets
     */
    void split() {

        loc_refsWhenFalse = loc_refs.dup();
        loc_linearWhenFalse = loc_linear.dup();
        loc_refsWhenTrue = loc_refs;
        loc_linearWhenTrue = loc_linear;
        loc_refs = loc_linear = null;
    }

    /** Merge (intersect) inits/uninits from WhenTrue/WhenFalse sets.
     */
    void merge() {
    }

/* ************************************************************************
 * Visitor methods for statements and definitions
 *************************************************************************/

    /** Analyze a definition.
     */
    void scanDef(JCTree tree) {
        scanStat(tree);
    }

    /** Analyze a statement. Check that statement is reachable.
     */
    void scanStat(JCTree tree) {
        scan(tree);
    }

    /** Analyze list of statements.
     */
    void scanStats(List<? extends JCStatement> trees) {
        if (trees != null)
            for (List<? extends JCStatement> l = trees; l.nonEmpty(); l = l.tail)
                scanStat(l.head);
    }

    /** Analyze an expression. Make sure to set (un)inits rather than
     *  (un)initsWhenTrue(WhenFalse) on exit.
     */
    void scanExpr(JCTree tree) {
        if (tree != null) {
            scan(tree);
        }
    }

    /** Analyze a list of expressions.
     */
    void scanExprs(List<? extends JCExpression> trees) {
        if (trees != null)
            for (List<? extends JCExpression> l = trees; l.nonEmpty(); l = l.tail)
                scanExpr(l.head);
    }

    /** Analyze a condition. Make sure to set (un)initsWhenTrue(WhenFalse)
     *  rather than (un)inits on exit.
     */
    void scanCond(JCTree tree) {
		/*
        if (tree.type.isFalse()) {

            loc_refsWhenTrue = loc_refs.dup();
            loc_refsWhenTrue.inclRange(firstadr, nextadr);
            loc_linearWhenTrue = loc_linear.dup();
            loc_linearWhenTrue.inclRange(firstadr, nextadr);
            loc_refsWhenFalse = loc_refs;
            loc_linearWhenFalse = loc_linear;

        } else if (tree.type.isTrue()) {

            //if (inits == null) merge();
            loc_refsWhenFalse = loc_refs.dup();
            loc_refsWhenFalse.inclRange(firstadr, nextadr);
            loc_linearWhenFalse = loc_linear.dup();
            loc_linearWhenFalse.inclRange(firstadr, nextadr);
            loc_refsWhenTrue = loc_refs;
            loc_linearWhenTrue = loc_linear;
        } else
		 *
		 */
		{
            scan(tree);
            if (loc_refs != null) split();
        }
        loc_refs = loc_linear = null;
    }

    /* ------------ Visitor methods for various sorts of trees -------------*/

    public void visitClassDef(JCClassDecl tree) {
        if (tree.sym == null) return;

        JCClassDecl classDefPrev = classDef;
        int firstadrPrev = firstadr;
        int nextadrPrev = nextadr;
        Lint lintPrev = lint;

		if(tree.extending!=null&&tree.extending.type.tag==TypeTags.CLASS)
		{
			nextadr=((ClassSymbol)TreeInfo.symbol(tree.extending)).nextAdr;
		}

        if (tree.name != names.empty) {
            firstadr = nextadr;
        }
        classDef = tree;
        lint = lint.augment(tree.sym.attributes_field);

        try {
            // define all the static fields
            for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                if (l.head.getTag() == JCTree.VARDEF) {
                    JCVariableDecl def = (JCVariableDecl)l.head;
                    if ((def.mods.flags & STATIC) != 0) {
                        VarSymbol sym = def.sym;
                        if (trackable(sym))
                            newVar(sym);
                    }
                }
            }

            // process all the static initializers
            for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                if (l.head.getTag() != JCTree.METHODDEF &&
                    (TreeInfo.flags(l.head) & STATIC) != 0) {
                    scanDef(l.head);
                }
            }

            // add intersection of all thrown clauses of initial constructors
            // to set of caught exceptions, unless class is anonymous.
            if (tree.name != names.empty) {
                boolean firstConstructor = true;
                for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                    if (TreeInfo.isInitialConstructor(l.head)) {
                        List<Type> mthrown =
                            ((JCMethodDecl) l.head).sym.type.getThrownTypes();
                        if (firstConstructor) {
                            firstConstructor = false;
                        } else {
                        }
                    }
                }
            }

            // define all the instance fields
            for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                if (l.head.getTag() == JCTree.VARDEF) {
                    JCVariableDecl def = (JCVariableDecl)l.head;
                    if ((def.mods.flags & STATIC) == 0) {
                        VarSymbol sym = def.sym;
                        if (trackable(sym))
                            newVar(sym);
                    }
                }
            }

            // process all the instance initializers
            for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                if (l.head.getTag() != JCTree.METHODDEF &&
                    (TreeInfo.flags(l.head) & STATIC) == 0) {
                    scanDef(l.head);
                }
            }

            // in an anonymous class, add the set of thrown exceptions to
            // the throws clause of the synthetic constructor and propagate
            // outwards.
            if (tree.name == names.empty) {
                for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                    if (TreeInfo.isInitialConstructor(l.head)) {
                        JCMethodDecl mdef = (JCMethodDecl)l.head;
                    }
                }
            }

            // process all the methods
            for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                if (l.head.getTag() == JCTree.METHODDEF) {
                    scan(l.head);
                }
            }

        } finally {
            nextadr = nextadrPrev;
            firstadr = firstadrPrev;
            classDef = classDefPrev;
            lint = lintPrev;
        }
    }

    public void visitMethodDef(JCMethodDecl tree) {

        if(done.contains(tree)) return; //do fun only once per pass
        done.add(tree);

        if (!tree.analyse()) return;

        Bits loc_refsPrev = loc_refs.dup();
        Bits loc_linearPrev = loc_linear.dup();

        int nextadrPrev = nextadr;
        int firstadrPrev = firstadr;

//        firstadr = 0;
//        nextadr = 0;

        ListBuffer<VarSymbol> refListPrev=refList;
        boolean enableRefPrev=enableRef;

        refList=new ListBuffer<VarSymbol>();
        enableRef=false;


        Lint lintPrev = lint;

        JCMethodDecl prevMethod = method;
        method = tree; //store current method
		
        method.sym.EstimatedWork=work.analyzeTree(tree,true);

        method.sym.EstimatedMem=work.memSum;


        //insert in arguments (methdecl is source)

        lint = lint.augment(tree.sym.attributes_field);

        //first run (all not yet evaluated fun calls will yield 0, must make second run)

        try {
			int old_nextadr=nextadr;
            boolean isInitialConstructor =
                TreeInfo.isInitialConstructor(tree);

            if (!isInitialConstructor)
                firstadr = nextadr;
            for (List<JCVariableDecl> l = tree.params; l.nonEmpty(); l = l.tail) {
                JCVariableDecl def = l.head;
                scan(def);
           }
            // else we are in an instance initializer block;
            // leave caught unchanged.

//          System.out.println("analyse [" + pass + "] : "+tree.toFlatString());
            scanStat(tree.body);



            for(int i=old_nextadr;i<vars.length;i++)
            {
                //reset adr for later pass
                if(vars[i]!=null)
                    vars[i].adr=-1;
            }

//          System.out.println("done [" + pass + "] : "+tree.toFlatString());

        } finally {
            refList = refListPrev;
            enableRef = enableRefPrev;
            loc_refs = loc_refsPrev;
            loc_linear = loc_linearPrev;
            nextadr = nextadrPrev;
            firstadr = firstadrPrev;
            lint = lintPrev;
            method = prevMethod;
            if (vars != null) for (int i=0; i<vars.length; i++)
                vars[i] = null;
        }
    }

    void linearityRef(VarSymbol s)
    {
        if(enableRef)//is it an arg of cur fun?
        {
            if(s.adr<0)
                newVar(s);

            if(!refList.contains(s))
                refList.add(s);
        }
    }

    boolean startLinearity()
    {
        boolean old = enableRef;

        if(!enableRef)
            refList.clear();

        enableRef = true; //don't flood the refList unnecessarily

        return old;
    }

    void stopLinearity(boolean old)
    {
        enableRef = old;

        if(enableRef)
            return;

        //assignment to tree.sym of all vars in refList (update linearity)
        for(List<VarSymbol> l = refList.toList();l.nonEmpty(); l = l.tail)
        {
            //assert(l.head.adr>=0);
            if(l.head.adr==-1)
            {
                newVar(l.head);
            }
            if(l.head.adr>=0) //this has addr -1??
            {
                if(loc_refs!=null) //might be in conditionl branch...
                {
                    if(loc_refs.isMember(l.head.adr)) //already has ref?
                        loc_linear.excl(l.head.adr); //>1 refs -> not linear
                    else
                        loc_refs.incl(l.head.adr); //one ref
                }
                else
                {
                    if(loc_refsWhenFalse.isMember(l.head.adr)) //already has ref?
                        loc_linearWhenFalse.excl(l.head.adr); //>1 refs -> not linear
                    else
                        loc_refsWhenFalse.incl(l.head.adr); //one ref
                    if(loc_refsWhenTrue.isMember(l.head.adr)) //already has ref?
                        loc_linearWhenTrue.excl(l.head.adr); //>1 refs -> not linear
                    else
                        loc_refsWhenTrue.incl(l.head.adr); //one ref
                }
            }
        }
    }

    public void visitVarDef(JCVariableDecl tree) {
        boolean track = trackable(tree.sym);
        if (track && tree.sym.owner.kind == MTH) newVar(tree.sym);
        if (tree.init != null) {
            Lint lintPrev = lint;
            lint = lint.augment(tree.sym.attributes_field);
            try{
                //we only want to find refs used by tree.init, so clear old refs

                boolean old=startLinearity();

                scanExpr(tree.init);

                stopLinearity(old);

                JCTree sym=base.getBaseSymbol(tree.init);

                Set<VarSymbol> s=base.getAliasGlobalFromBase(sym);

                if (track) letInit(tree,tree.pos(), tree.sym,false,s);
            } finally {
                lint = lintPrev;
            }
        }
    }

    public void visitBlock(JCBlock tree) {
        int nextadrPrev = nextadr;
        scanStats(tree.stats);

		for(int i=nextadrPrev;i<nextadr;i++)
			if(vars[i]!=null)
				vars[i].adr=-1;

        nextadr = nextadrPrev;
    }

    public void visitCaseExp(JCCaseExp tree) {
        int nextadrPrev = nextadr;

        boolean old=startLinearity();
        scanExpr(tree.exp);
        stopLinearity(old);

        boolean hasDefault = false;

        Bits start_loc_refs = loc_refs.dup();
        Bits start_loc_linear = loc_linear.dup();

        if(tree.list.head.stmnt!=null)
            scanStat(tree.list.head.stmnt);
        else
            scanExpr(tree.list.head.res);

        Bits loc_refsSwitch = loc_refs.dup();
        Bits loc_linearSwitch = loc_linear.dup();

        loc_linear = start_loc_linear.dup();

        for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {

            JCSelectCond c = l.head;

            if (c.cond == null)
                hasDefault = true;
            else
                scanExpr(c.cond);

            if(c.stmnt!=null)
                scanStat(c.stmnt);
            else
                scanExpr(c.res);

            loc_refsSwitch.andSet(loc_refs);
            loc_linearSwitch.andSet(loc_linear);
            loc_refs = start_loc_refs.dup();
            loc_linear = start_loc_linear.dup();
        }


        if (!hasDefault) {
            loc_refs.andSet(loc_refsSwitch);
            loc_linear.andSet(loc_linearSwitch);
        }
        else
        {
            loc_refs = loc_refsSwitch;
            loc_linear = loc_linearSwitch;
        }

		for(int i=nextadrPrev;i<nextadr;i++)
			if(vars[i]!=null)
				vars[i].adr=-1;

        nextadr = nextadrPrev;
    }

    public void visitSelect(JCFieldAccess tree) {
        boolean old = startLinearity();

        scanExpr(tree.selected);
        //super.visitSelect(tree);

        stopLinearity(old);
    }

    public void visitSelectExp(JCSelect tree) {
        int nextadrPrev = nextadr;
        boolean hasDefault = false;

        Bits start_loc_refs = loc_refs.dup();
        Bits start_loc_linear = loc_linear.dup();

        if(tree.list.head.stmnt!=null)
            scanStat(tree.list.head.stmnt);
        else
            scanExpr(tree.list.head.res);

        Bits loc_refsSwitch = loc_refs.dup();
        Bits loc_linearSwitch = loc_linear.dup();

        loc_linear = start_loc_linear.dup();

        for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {

            JCSelectCond c = l.head;

            if (c.cond == null)
                hasDefault = true;
            else
                scanExpr(c.cond);

            if(c.stmnt!=null)
                scanStat(c.stmnt);
            else
                scanExpr(c.res);

            loc_refsSwitch.andSet(loc_refs);
            loc_linearSwitch.andSet(loc_linear);
            loc_refs = start_loc_refs.dup();
            loc_linear = start_loc_linear.dup();
        }


        if (!hasDefault) {
            loc_refs.andSet(loc_refsSwitch);
            loc_linear.andSet(loc_linearSwitch);
        }
        else
        {
            loc_refs = loc_refsSwitch;
            loc_linear = loc_linearSwitch;
        }

		for(int i=nextadrPrev;i<nextadr;i++)
			if(vars[i]!=null)
				vars[i].adr=-1;

        nextadr = nextadrPrev;
    }


    public void visitIf(JCIf tree) {
        boolean old=startLinearity();
        scanCond(tree.cond);
        stopLinearity(old);

        Bits loc_refsBeforeElse = loc_refsWhenFalse;
        Bits loc_linearBeforeElse = loc_linearWhenFalse;
        loc_refs = loc_refsWhenTrue;
        loc_linear = loc_linearWhenTrue;

        scanStat(tree.thenpart);

        if (tree.elsepart != null) {
            Bits loc_refsAfterThen = loc_refs.dup();
            Bits loc_linearAfterThen = loc_linear.dup();
            loc_refs = loc_refsBeforeElse;
            loc_linear = loc_linearBeforeElse;

            scanStat(tree.elsepart);

            loc_refs.andSet(loc_refsAfterThen);
            loc_linear.andSet(loc_linearAfterThen);

        } else {
            loc_refs.orSet(loc_refsBeforeElse);
            loc_linear.andSet(loc_linearBeforeElse);
        }
    }

    public void visitIfExp(JCIfExp tree) {
        boolean old=startLinearity();
        scanCond(tree.cond);
        stopLinearity(old);

        Bits loc_refsBeforeElse = loc_refsWhenFalse;
        Bits loc_linearBeforeElse = loc_linearWhenFalse;
        loc_refs = loc_refsWhenTrue;
        loc_linear = loc_linearWhenTrue;
        scanStat(tree.thenpart);
        if (tree.elsepart != null) {
            Bits loc_refsAfterThen = loc_refs.dup();
            Bits loc_linearAfterThen = loc_linear.dup();
            loc_refs = loc_refsBeforeElse;
            loc_linear = loc_linearBeforeElse;
            scanStat(tree.elsepart);
            loc_refs.andSet(loc_refsAfterThen);
            loc_linear.andSet(loc_linearAfterThen);
        } else {
            loc_refs.andSet(loc_refsBeforeElse);
            loc_linear.andSet(loc_linearBeforeElse);
        }
    }

    public void visitWhere(JCWhere tree) {
        scanExpr(tree.exp);

        boolean old=enableRef;

        enableRef=false;

		//FIXME: nested where?
		boolean old_where=inside_where;
		inside_where=true;

        ListBuffer<VarSymbol> localrefList = refList;
        refList=new ListBuffer<VarSymbol>();

		if(tree.sexp!=null)
			scanExpr(tree.sexp);
		else
			scanStat(tree.body);


        refList=localrefList;
		inside_where=old_where;

        enableRef=old;
    }


    public void visitReturn(JCReturn tree)
    {
        //boolean old=startTrackLinearity();

        boolean old=startLinearity();

		inside_return=true;

        scanExpr(tree.expr);

		inside_return=false;

        stopLinearity(old);

        JCTree sym=base.getBaseSymbol(tree.expr);

        Set<VarSymbol> s=base.getAliasGlobalFromBase(sym);

        //boolean linear=stopTrackLinearity(old);

        if(false&&pass==LocalPass.FINISHED)
        {
            //this is 'may'
            method.sym.retValAliasLinear.addAll(s);
        }
    }

    public void visitApply(JCMethodInvocation tree) {
        scanExpr(tree.meth);
        scanExprs(tree.args);
    }

    public void visitNewClass(JCNewClass tree) {
        scanExpr(tree.encl);
        scanExprs(tree.args);
       // scan(tree.def);
        try {
            // If the new class expression defines an anonymous class,
            // analysis of the anonymous constructor may encounter thrown
            // types which are unsubstituted type variables.
            // However, since the constructor's actual thrown types have
            // already been marked as thrown, it is safe to simply include
            // each of the constructor's formal thrown types in the set of
            // 'caught/declared to be thrown' types, for the duration of
            // the class def analysis.
            scan(tree.def);
        }
        finally {
        }
    }

    public void visitNewArray(JCNewArray tree) {
        scanExprs(tree.dims);
        scanExprs(tree.elems);
    }

    public void visitAssert(JCAssert tree) {
        Bits loc_refsExit = loc_refs.dup();
        Bits loc_linearExit = loc_linear.dup();
        scanCond(tree.cond);
        loc_linearExit.andSet(loc_linearWhenTrue);
        if (tree.detail != null) {
            loc_refs = loc_refsWhenFalse;
            loc_linear = loc_linearWhenFalse;
            scanExpr(tree.detail);
        }

        loc_refs = loc_refsExit;
        loc_linear = loc_linearExit;
    }

    public void visitAssign(JCAssign tree) {
        JCTree lhs = TreeInfo.skipParens(tree.lhs);

        if (!(lhs instanceof JCIdent)) scanExpr(lhs);

        boolean old=startLinearity();

        scanExpr(tree.rhs);

        stopLinearity(old);

        JCTree sym=base.getBaseSymbol(tree.rhs);

        Set<VarSymbol> s=base.getAliasGlobalFromBase(sym);

        letInit(lhs,false,s);

        //JCTree lhs_sym=base.getBaseSymbol(tree.lhs);


    }

	public void visitArgExpression(JCArgExpression tree) {

		if(tree.exp2==null)
		{
			boolean old=startLinearity();
			scanExpr(tree.exp1);
			stopLinearity(old);
		}
		else
		{
			JCTree lhs = tree.exp1;
			JCExpression rhs = tree.exp2;

			if (tree.exp2==null) scanExpr(lhs);

			boolean old=startLinearity();

			scanExpr(rhs);

			stopLinearity(old);

			JCTree sym=base.getBaseSymbol(rhs);

			Set<VarSymbol> s=base.getAliasGlobalFromBase(sym);

			letInit(lhs,false,s);
		}

	}

    public void visitAssignop(JCAssignOp tree) {
        scanExpr(tree.lhs);

        boolean old=startLinearity();
        scanExpr(tree.rhs);
        stopLinearity(old);

        JCTree sym=base.getBaseSymbol(tree.rhs);

        Set<VarSymbol> s=base.getAliasGlobalFromBase(sym);

        letInit(tree.lhs,false,s);
    }

    public void visitUnary(JCUnary tree) {
        switch (tree.getTag()) {
        case JCTree.NOT:
            scanCond(tree.arg);
            Bits t = loc_refsWhenFalse;
            loc_refsWhenFalse = loc_refsWhenTrue;
            loc_refsWhenTrue = t;
            t = loc_linearWhenFalse;
            loc_linearWhenFalse = loc_linearWhenTrue;
            loc_linearWhenTrue = t;

            break;
        case JCTree.PREINC: case JCTree.POSTINC:
        case JCTree.PREDEC: case JCTree.POSTDEC:
            boolean old=startLinearity();
            scanExpr(tree.arg);
            stopLinearity(old);

            JCTree sym=base.getBaseSymbol(tree.arg);
            Set<VarSymbol> s=base.getAliasGlobalFromBase(sym);

            letInit(tree.arg,false,s); //lin=false???
            break;
        default:
            scanExpr(tree.arg);
        }
    }

    public void visitBinary(JCBinary tree) {
        switch (tree.getTag()) {
        case JCTree.AND:
            scanCond(tree.lhs);
            Bits loc_refsWhenFalseLeft = loc_refsWhenFalse;
            Bits loc_linearWhenFalseLeft = loc_linearWhenFalse;
            loc_refs = loc_refsWhenTrue;
            loc_linear = loc_linearWhenTrue;
            scanCond(tree.rhs);
            loc_refsWhenFalse.andSet(loc_refsWhenFalseLeft);
            loc_linearWhenFalse.andSet(loc_linearWhenFalseLeft);
            break;
        case JCTree.OR:
            scanCond(tree.lhs);
            Bits loc_refsWhenTrueLeft = loc_refsWhenTrue;
            Bits loc_linearWhenTrueLeft = loc_linearWhenTrue;
            loc_refs = loc_refsWhenFalse;
            loc_linear = loc_linearWhenFalse;
            scanCond(tree.rhs);
            loc_refsWhenTrue.andSet(loc_refsWhenTrueLeft);
            loc_linearWhenTrue.andSet(loc_linearWhenTrueLeft);
            break;
        default:
            scanExpr(tree.lhs);
            scanExpr(tree.rhs);
        }
    }

    public void visitIdent(JCIdent tree) {
        if (tree.sym.kind == VAR)
        {
            linearityRef((VarSymbol)tree.sym);
        }
    }

    public void visitTopLevel(JCCompilationUnit tree) {
        // Do nothing for TopLevel since each class is visited individually
    }

/**************************************************************************
 * main method
 *************************************************************************/

    /** Perform definite assignment/unassignment analysis on a tree.
     */
    public void analyzeTree(JCTree tree, TreeMaker make) {
        try {
            this.make = make;
            base=new BaseSymbol();
            done=new LinkedHashSet<JCTree>();

            //for(int p=0;p<2;p++)
            {
                done.clear();

                enableRef = false;

                loc_refs = new Bits();
                loc_linear = new Bits();

                refList = new ListBuffer<VarSymbol>();

                if (vars == null)
                    vars = new VarSymbol[32];
                else
                    for (int i=0; i<vars.length; i++)
                        vars[i] = null;

                firstadr = 0;
                nextadr = 0;

                this.classDef = null;
/*
                if(p==0)
                    pass=LocalPass.FIRST;
                else
                    pass=LocalPass.FINISHED;
*/
                recursion = null;

                scan(tree);
            }
        } finally {
            // note that recursive invocations of this method fail hard
            loc_refs = loc_linear = null;
            refList = null;
            if (vars != null) for (int i=0; i<vars.length; i++)
                vars[i] = null;
            firstadr = 0;
            nextadr = 0;
            this.make = null;
            this.classDef = null;
        }
    }
}
