/*
 * Copyright 1999-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

//todo: one might eliminate uninits.andSets when monotonic

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
import java.util.LinkedHashMap;

import java.util.Hashtable;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Map;

import org.jgrapht.*;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;


/** This pass implements dataflow analysis for Java programs.
 *  Liveness analysis checks that every statement is reachable.
 *  Exception analysis ensures that every checked exception that is
 *  thrown is declared or caught.  Definite assignment analysis
 *  ensures that each variable is assigned when used.  Definite
 *  unassignment analysis ensures that no final variable is assigned
 *  more than once.
 *
 *  <p>The second edition of the JLS has a number of problems in the
 *  specification of these flow analysis problems. This implementation
 *  attempts to address those issues.
 *
 *  <p>First, there is no accommodation for a finally clause that cannot
 *  complete normally. For liveness analysis, an intervening finally
 *  clause can cause a break, continue, or return not to reach its
 *  target.  For exception analysis, an intervening finally clause can
 *  cause any exception to be "caught".  For DA/DU analysis, the finally
 *  clause can prevent a transfer of control from propagating DA/DU
 *  state to the target.  In addition, code in the finally clause can
 *  affect the DA/DU status of variables.
 *
 *  <p>For try statements, we introduce the idea of a variable being
 *  definitely unassigned "everywhere" in a block.  A variable V is
 *  "unassigned everywhere" in a block iff it is unassigned at the
 *  beginning of the block and there is no reachable assignment to V
 *  in the block.  An assignment V=e is reachable iff V is not DA
 *  after e.  Then we can say that V is DU at the beginning of the
 *  catch block iff V is DU everywhere in the try block.  Similarly, V
 *  is DU at the beginning of the finally block iff V is DU everywhere
 *  in the try block and in every catch block.  Specifically, the
 *  following bullet is added to 16.2.2
 *  <pre>
 *      V is <em>unassigned everywhere</em> in a block if it is
 *      unassigned before the block and there is no reachable
 *      assignment to V within the block.
 *  </pre>
 *  <p>In 16.2.15, the third bullet (and all of its sub-bullets) for all
 *  try blocks is changed to
 *  <pre>
 *      V is definitely unassigned before a catch block iff V is
 *      definitely unassigned everywhere in the try block.
 *  </pre>
 *  <p>The last bullet (and all of its sub-bullets) for try blocks that
 *  have a finally block is changed to
 *  <pre>
 *      V is definitely unassigned before the finally block iff
 *      V is definitely unassigned everywhere in the try block
 *      and everywhere in each catch block of the try statement.
 *  </pre>
 *  <p>In addition,
 *  <pre>
 *      V is definitely assigned at the end of a constructor iff
 *      V is definitely assigned after the block that is the body
 *      of the constructor and V is definitely assigned at every
 *      return that can return from the constructor.
 *  </pre>
 *  <p>In addition, each continue statement with the loop as its target
 *  is treated as a jump to the end of the loop body, and "intervening"
 *  finally clauses are treated as follows: V is DA "due to the
 *  continue" iff V is DA before the continue statement or V is DA at
 *  the end of any intervening finally block.  V is DU "due to the
 *  continue" iff any intervening finally cannot complete normally or V
 *  is DU at the end of every intervening finally block.  This "due to
 *  the continue" concept is then used in the spec for the loops.
 *
 *  <p>Similarly, break statements must consider intervening finally
 *  blocks.  For liveness analysis, a break statement for which any
 *  intervening finally cannot complete normally is not considered to
 *  cause the target statement to be able to complete normally. Then
 *  we say V is DA "due to the break" iff V is DA before the break or
 *  V is DA at the end of any intervening finally block.  V is DU "due
 *  to the break" iff any intervening finally cannot complete normally
 *  or V is DU at the break and at the end of every intervening
 *  finally block.  (I suspect this latter condition can be
 *  simplified.)  This "due to the break" is then used in the spec for
 *  all statements that can be "broken".
 *
 *  <p>The return statement is treated similarly.  V is DA "due to a
 *  return statement" iff V is DA before the return statement or V is
 *  DA at the end of any intervening finally block.  Note that we
 *  don't have to worry about the return expression because this
 *  concept is only used for construcrors.
 *
 *  <p>There is no spec in JLS2 for when a variable is definitely
 *  assigned at the end of a constructor, which is needed for final
 *  fields (8.3.1.2).  We implement the rule that V is DA at the end
 *  of the constructor iff it is DA and the end of the body of the
 *  constructor and V is DA "due to" every return of the constructor.
 *
 *  <p>Intervening finally blocks similarly affect exception analysis.  An
 *  intervening finally that cannot complete normally allows us to ignore
 *  an otherwise uncaught exception.
 *
 *  <p>To implement the semantics of intervening finally clauses, all
 *  nonlocal transfers (break, continue, return, throw, method call that
 *  can throw a checked exception, and a constructor invocation that can
 *  thrown a checked exception) are recorded in a queue, and removed
 *  from the queue when we complete processing the target of the
 *  nonlocal transfer.  This allows us to modify the queue in accordance
 *  with the above rules when we encounter a finally clause.  The only
 *  exception to this [no pun intended] is that checked exceptions that
 *  are known to be caught or declared to be caught in the enclosing
 *  method are not recorded in the queue, but instead are recorded in a
 *  global variable "Set<Type> thrown" that records the type of all
 *  exceptions that can be thrown.
 *
 *  <p>Other minor issues the treatment of members of other classes
 *  (always considered DA except that within an anonymous class
 *  constructor, where DA status from the enclosing scope is
 *  preserved), treatment of the case expression (V is DA before the
 *  case expression iff V is DA after the switch expression),
 *  treatment of variables declared in a switch block (the implied
 *  DA/DU status after the switch expression is DU and not DA for
 *  variables defined in a switch block), the treatment of boolean ?:
 *  expressions (The JLS rules only handle b and c non-boolean; the
 *  new rule is that if b and c are boolean valued, then V is
 *  (un)assigned after a?b:c when true/false iff V is (un)assigned
 *  after b when true/false and V is (un)assigned after c when
 *  true/false).
 *
 *  <p>There is the remaining question of what syntactic forms constitute a
 *  reference to a variable.  It is conventional to allow this.x on the
 *  left-hand-side to initialize a final instance field named x, yet
 *  this.x isn't considered a "use" when appearing on a right-hand-side
 *  in most implementations.  Should parentheses affect what is
 *  considered a variable reference?  The simplest rule would be to
 *  allow unqualified forms only, parentheses optional, and phase out
 *  support for assigning to a final field via this.x.
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Flow extends TreeScanner {
    protected static final Context.Key<Flow> flowKey =
        new Context.Key<Flow>();

    private final Names names;
    private final Log log;
    private final Symtab syms;
    private final Types types;
    private final Check chk;
    private       TreeMaker make;
    private       Lint lint;

	private boolean arg_out = false;

	private JCTree current_branch = null;

	private boolean allow_nop = false;

	boolean is_event = false;
	boolean is_evo = false;
	boolean is_atomic = false;
	private BaseSymbol base;

	int orderId=0;

	long return_flags=0;

    public static Flow instance(Context context) {
        Flow instance = context.get(flowKey);
        if (instance == null)
            instance = new Flow(context);
        return instance;
    }

    protected Flow(Context context) {
        context.put(flowKey, this);
		base=new BaseSymbol();
        names = Names.instance(context);
        log = Log.instance(context);
        syms = Symtab.instance(context);
        types = Types.instance(context);
        chk = Check.instance(context);
        lint = Lint.instance(context);
    }

    //private Hashtable<VarSymbol,Set<JCTree>> method.generated = new Hashtable<VarSymbol,Set<JCTree>>(301, 0.5f);

    private void addGenerated(VarSymbol s,JCTree t)
    {
		if(inreturn)
			return;
    	//multiple deps possible (decl and init seperated)
		Set<JCTree> deps=method.generated.get(s);
		if(null==deps)
		{

			Set<JCTree> ns=new LinkedHashSet<JCTree>();
			ns.add(t);
			method.generated.put(s, ns);

		}
		else
			deps.add(t);
    }

    private JCTree current_stat;

    /** A flag that indicates whether the last statement could
     *  complete normally.
     */
    private boolean alive;
    private boolean resume_alive;

	private boolean inreturn;

	private JCTree returntree=null;

    /** The set of definitely assigned variables.
     */
    Bits inits;

    /** The set of definitely unassigned variables.
     */
    Bits uninits;

    private boolean insideWhere;
	private JCWhere whereExp =null;

    //linearity analysis

    JCMethodDecl method = null;
    JCTree dg_env = null;

    boolean enableRef;

    /** The set of variables that are definitely unassigned everywhere
     *  in current try block. This variable is maintained lazily; it is
     *  updated only when something gets removed from uninits,
     *  typically by being assigned in reachable code.  To obtain the
     *  correct set of variables which are definitely unassigned
     *  anywhere in current try block, intersect uninitsTry and
     *  uninits.
     */
    Bits uninitsTry;

    /** When analyzing a condition, inits and uninits are null.
     *  Instead we have:
     */
    Bits initsWhenTrue;
    Bits initsWhenFalse;
    Bits uninitsWhenTrue;
    Bits uninitsWhenFalse;

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

    /** The list of possibly thrown declarable exceptions.
     */
    List<Type> thrown;

    /** The list of exceptions that are either caught or declared to be
     *  thrown.
     */
    List<Type> caught;

    /** Set when processing a loop body the second time for DU analysis. */
    boolean loopPassTwo = false;

    /*-------------------- Environments ----------------------*/

    /** A pending exit.  These are the statements return, break, and
     *  continue.  In addition, exception-throwing expressions or
     *  statements are put here when not known to be caught.  This
     *  will typically result in an error unless it is within a
     *  try-finally whose finally block cannot complete normally.
     */
    static class PendingExit {
        JCTree tree;
        Bits inits;
        Bits uninits;
        Type thrown;
        PendingExit(JCTree tree, Bits inits, Bits uninits) {
            this.tree = tree;
            this.inits = inits.dup();
            this.uninits = uninits.dup();
        }
        PendingExit(JCTree tree, Type thrown) {
            this.tree = tree;
            this.thrown = thrown;
        }
    }

    /** The currently pending exits that go from current inner blocks
     *  to an enclosing block, in source order.
     */
    ListBuffer<PendingExit> pendingExits;

    /*-------------------- Exceptions ----------------------*/

    /** Complain that pending exceptions are not caught.
     */
    /*
    void errorUncaught() {
        for (PendingExit exit = pendingExits.next();
             exit != null;
             exit = pendingExits.next()) {
            boolean synthetic = classDef != null &&
                classDef.pos == exit.tree.pos;
            log.error(exit.tree.pos(),
                      synthetic
                      ? "unreported.exception.default.constructor"
                      : "unreported.exception.need.to.catch.or.throw",
                      exit.thrown);
        }
    }
    */

    /** Record that exception is potentially thrown and check that it
     *  is caught.
     */
    void markThrown(JCTree tree, Type exc) {
        if (!chk.isUnchecked(tree.pos(), exc)) {
            if (!chk.isHandled(exc, caught))
                pendingExits.append(new PendingExit(tree, exc));
            thrown = chk.incl(exc, thrown);
        }
    }

    /*-------------- Processing variables ----------------------*/

    /** Do we need to track init/uninit state of this symbol?
     *  I.e. is symbol either a local or a blank final variable?
     */
    boolean trackable(VarSymbol sym) {
        return
            (sym.owner.kind == MTH ||
             ((sym.flags() & (FINAL | HASINIT | PARAMETER)) == FINAL &&
              ((sym.owner instanceof ClassSymbol)&&classDef.sym.isEnclosedBy((ClassSymbol)sym.owner)||sym.owner instanceof DomainSymbol)));
    }

    /** Initialize new trackable variable by setting its address field
     *  to the next available sequence number and entering it under that
     *  index into the vars array.
     */
    void newVar(VarSymbol sym) {
		if(sym.adr>=0)
			return;

        if (nextadr == vars.length) {
            VarSymbol[] newvars = new VarSymbol[nextadr * 2];
            System.arraycopy(vars, 0, newvars, 0, nextadr);
            vars = newvars;
        }
        sym.adr = nextadr;
		sym.scope = nextadr;
        vars[nextadr] = sym;
        inits.excl(nextadr);
        uninits.incl(nextadr);

		if(method!=null)
			method.local_vars.add(sym);

        nextadr++;
    }

    /** Record an initialization of a trackable variable.
     */
    void letInit(DiagnosticPosition pos, VarSymbol sym,boolean allowinout) {
        if (sym.adr >= firstadr && trackable(sym)) {

            //link var def to cur stat (for dep graph generation)
            addGenerated(sym,current_stat);

            if ((sym.flags() & FINAL) != 0) {

                if (!allowinout&&(sym.flags() & FINOUT) == FINOUT) {
                    log.error(pos, "inout.parameter.may.not.be.assigned",  sym);
                }
                else
                if (!uninits.isMember(sym.adr))
                {
                    if(!insideWhere&&(sym.type.type_flags_field&Flags.LINEAR)==0)
					{
	                    log.error(pos,
                              loopPassTwo
                              ? "var.might.be.assigned.in.loop"
                              : "var.might.already.be.assigned",
                              sym);
					}
                }
                else if (!inits.isMember(sym.adr))
                {
                    // reachable assignment
                    uninits.excl(sym.adr);
                    uninitsTry.excl(sym.adr);
                } else {
                    //log.rawWarning(pos, "unreachable assignment");//DEBUG
                    uninits.excl(sym.adr);
                }
            }
            inits.incl(sym.adr);
        } else if ((sym.flags() & FINAL) != 0) {
			//FIXME: what do we do here??
            //if(!insideWhere)
            //    log.error(pos, "var.might.already.be.assigned", sym);
        }


    }

    /** If tree is either a simple name or of the form this.name or
     *  C.this.name, and tree represents a trackable variable,
     *  record an initialization of the variable.
     */
    void letInit(JCTree tree,boolean allowinout) {
        tree = TreeInfo.skipParens(tree);
        if (tree.getTag() == JCTree.IDENT || tree.getTag() == JCTree.SELECT) {
            Symbol sym = TreeInfo.symbol(tree);

            //take care of auto defined syms
            VarSymbol vsym=(VarSymbol)sym;
            if(vsym.adr==-1&&trackable(vsym))
            {
                newVar(vsym);
            }

            letInit(tree.pos(), (VarSymbol)sym, allowinout);
        }
    }

    /** Check that trackable variable is initialized.
     */
    void checkInit(DiagnosticPosition pos, VarSymbol sym) {
        if ((sym.adr >= firstadr || sym.owner.kind != TYP) &&
            trackable(sym) &&
            !inits.isMember(sym.adr)) {
            log.error(pos, "var.might.not.have.been.initialized",
                      sym);
            inits.incl(sym.adr);
        }
    }

    /*-------------------- Handling jumps ----------------------*/

    /** Record an outward transfer of control. */
    void recordExit(JCTree tree) {
        pendingExits.append(new PendingExit(tree, inits, uninits));
        markDead();
    }

	void recordResume(JCTree tree) {
        pendingExits.append(new PendingExit(tree, inits, uninits));
		resume_alive = false;
        //markDead();
    }

    /** Resolve all breaks of this statement. */
    boolean resolveBreaks(JCTree tree,
                          ListBuffer<PendingExit> oldPendingExits) {
        boolean result = false;
        List<PendingExit> exits = pendingExits.toList();
        pendingExits = oldPendingExits;
        for (; exits.nonEmpty(); exits = exits.tail) {
            PendingExit exit = exits.head;
            if (exit.tree.getTag() == JCTree.BREAK &&
                ((JCBreak) exit.tree).target == tree) {
                inits.andSet(exit.inits);
                uninits.andSet(exit.uninits);

                result = true;
            } else {
                pendingExits.append(exit);
            }
        }
        return result;
    }

    /** Resolve all continues of this statement. */
    boolean resolveContinues(JCTree tree) {
        boolean result = false;
        List<PendingExit> exits = pendingExits.toList();
        pendingExits = new ListBuffer<PendingExit>();
        for (; exits.nonEmpty(); exits = exits.tail) {
            PendingExit exit = exits.head;
            if (exit.tree.getTag() == JCTree.CONTINUE &&
                ((JCContinue) exit.tree).target == tree) {
                inits.andSet(exit.inits);
                uninits.andSet(exit.uninits);
                result = true;
            } else {
                pendingExits.append(exit);
            }
        }
        return result;
    }

    /** Record that statement is unreachable.
     */
    void markDead() {
        inits.inclRange(firstadr, nextadr);
        uninits.inclRange(firstadr, nextadr);
        alive = false;
    }

    /** Split (duplicate) inits/uninits into WhenTrue/WhenFalse sets
     */
    void split() {
        initsWhenFalse = inits.dup();
        uninitsWhenFalse = uninits.dup();
        initsWhenTrue = inits;
        uninitsWhenTrue = uninits;
        inits = uninits = null;
    }

    /** Merge (intersect) inits/uninits from WhenTrue/WhenFalse sets.
     */
    void merge() {
        inits = initsWhenFalse.andSet(initsWhenTrue);
        uninits = uninitsWhenFalse.andSet(uninitsWhenTrue);
    }

/* ************************************************************************
 * Visitor methods for statements and definitions
 *************************************************************************/

    /** Analyze a definition.
     */
    void scanDef(JCTree tree) {
        scanStat(tree);
        if (tree != null && tree.getTag() == JCTree.BLOCK && !alive) {
            log.error(tree.pos(),
                      "initializer.must.be.able.to.complete.normally");
        }
    }

    /** Analyze a statement. Check that statement is reachable.
     */
    void scanStat(JCTree tree) {

		if(method!=null)
			method.orderNodes.put(tree, orderId);

		orderId++;

        if(tree.getTag()==JCTree.SKIP)
            return;

		if(tree.nop&&!allow_nop)
			return;

        if (!alive && tree != null) {
            log.error(tree.pos(), "unreachable.stmt");
            if (tree.getTag() != JCTree.SKIP)
			{
				alive = true;
				resume_alive = true;
			}
        }

        JCTree last = current_stat;
        current_stat = tree;
        //add edge from source and to final
        if(dg_env!=null&&!(current_stat instanceof JCBlock))
        {
            method.depGraph.addVertex(tree);
            dg_env.getDGNode().addDependentChildJGT(method.depGraph,current_stat,null,dg_env,method.dg_end);

            //store who is responsible to schedule this node (CF dependent statements must be scheduled by the CF statement)
            tree.scheduler=dg_env;

            current_stat.getDGNode().sourceConnect();
            current_stat.getDGNode().addDependentChildJGT(method.depGraph,method.dg_end,null,null,null);
/*
            if(dg_env.getTag()==JCTree.CF)//all nodes in cf flow through exit node
            {
                method.depGraph.addVertex(((JCCF)dg_env).exit);
                current_stat.getDGNode().addDependentChildJGT(method.depGraph,((JCCF)dg_env).exit,null,dg_env,method.dg_end);
            }
*/
        }

        scan(tree);

        current_stat=last;
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
            if (inits == null) merge();
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
            if (inits == null) merge();
            initsWhenTrue = inits.dup();
            initsWhenTrue.inclRange(firstadr, nextadr);
            uninitsWhenTrue = uninits.dup();
            uninitsWhenTrue.inclRange(firstadr, nextadr);
            initsWhenFalse = inits;
            uninitsWhenFalse = uninits;

        } else if (tree.type.isTrue()) {
            if (inits == null) merge();
            initsWhenFalse = inits.dup();
            initsWhenFalse.inclRange(firstadr, nextadr);
            uninitsWhenFalse = uninits.dup();
            uninitsWhenFalse.inclRange(firstadr, nextadr);
            initsWhenTrue = inits;
            uninitsWhenTrue = uninits;

            //if (inits == null) merge();
        } else
		 *
		 */
		{
            scan(tree);
            if (inits != null) split();
        }
        inits = uninits = null;
    }

    /* ------------ Visitor methods for various sorts of trees -------------*/

    public void visitClassDef(JCClassDecl tree) {
        if (tree.sym == null) return;

        JCClassDecl classDefPrev = classDef;
        List<Type> thrownPrev = thrown;
        List<Type> caughtPrev = caught;
        boolean alivePrev = alive;
		boolean resume_alivePrev = resume_alive;
		boolean inreturnPrev = inreturn;
        int firstadrPrev = firstadr;
        int nextadrPrev = nextadr;
        ListBuffer<PendingExit> pendingExitsPrev = pendingExits;
        Lint lintPrev = lint;

		if(tree.extending!=null&&tree.extending.type.tag==TypeTags.CLASS)
		{
			nextadr=((ClassSymbol)TreeInfo.symbol(tree.extending)).nextAdr;
		}

        pendingExits = new ListBuffer<PendingExit>();
        if (tree.name != names.empty) {
            caught = List.nil();
            firstadr = nextadr;
        }
        classDef = tree;
        thrown = List.nil();
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
                    //errorUncaught();
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
                            caught = mthrown;
                            firstConstructor = false;
                        } else {
                            caught = chk.intersect(mthrown, caught);
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
                    //errorUncaught();
                }
            }

            // in an anonymous class, add the set of thrown exceptions to
            // the throws clause of the synthetic constructor and propagate
            // outwards.
            if (tree.name == names.empty) {
                for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                    if (TreeInfo.isInitialConstructor(l.head)) {
                        JCMethodDecl mdef = (JCMethodDecl)l.head;
                        mdef.thrown = make.Types(thrown);
                        mdef.sym.type.setThrown(thrown);
                    }
                }
                thrownPrev = chk.union(thrown, thrownPrev);
            }

			tree.sym.nextAdr = nextadr;

            // process all the methods
            for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                if (l.head.getTag() == JCTree.METHODDEF) {
                    scan(l.head);
                    //errorUncaught();
                }
            }

            thrown = thrownPrev;
        } finally {
            pendingExits = pendingExitsPrev;
            alive = alivePrev;
			resume_alive = resume_alivePrev;
			inreturn = inreturnPrev;
            nextadr = nextadrPrev;
            firstadr = firstadrPrev;
            caught = caughtPrev;
            classDef = classDefPrev;
            lint = lintPrev;
        }
    }

    public void visitMethodDef(JCMethodDecl tree) {
        if (!tree.analyse()) return;

		orderId=0;
        List<Type> caughtPrev = caught;
        List<Type> mthrown = tree.sym.type.getThrownTypes();
        Bits initsPrev = inits.dup();
        Bits uninitsPrev = uninits.dup();

        int nextadrPrev = nextadr;
        int firstadrPrev = firstadr;
        Lint lintPrev = lint;


        JCMethodDecl prevMethod = method;
        method = tree; //store current method
        dg_env = method;

		method.local_vars=new LinkedHashSet<VarSymbol>();

		current_branch = tree;

		is_event = (method.restype == null && tree.name != method.name.table.names.init);
		is_evo = (method.sym.owner.flags() & Flags.SINGULAR)!=0&&(method.sym.flags_field&Flags.STATIC)==0;
		is_atomic= (method.sym.owner.flags_field&Flags.ATOMIC)!=0;

        method.getDGNode().setSource(method);//method is own source
        method.dg_end.getDGNode().setSource(method.dg_end);

        method.depGraph=new DirectedWeightedMultigraph<JCTree, Arc>(Arc.class);
        //ParanoidGraph<JCTree, Arc> pg =new ParanoidGraph<JCTree, Arc>(method.depGraph);
        //method.depGraph = pg;

        method.depGraph.addVertex(method);
        method.depGraph.addVertex(method.dg_end);
        method.dg_end.scheduler=method;

		method.scheduler=method;

		method.generated = new Hashtable<VarSymbol,Set<JCTree>>(301, 0.5f);

        current_stat = tree;
        //insert in arguments (methdecl is source)

        lint = lint.augment(tree.sym.attributes_field);

        assert pendingExits.isEmpty();

        try {
            boolean isInitialConstructor =
                TreeInfo.isInitialConstructor(tree);

            if (!isInitialConstructor)
                firstadr = nextadr;
            for (List<JCVariableDecl> l = tree.params; l.nonEmpty(); l = l.tail) {
                JCVariableDecl def = l.head;
                scan(def);

                if((l.head.sym.flags() & FINOUT)==FINOUT)
                {
                    //???
                    inits.incl(def.sym.adr);
                    uninits.incl(def.sym.adr);

                    addGenerated(def.sym,tree);//argument depends on methdecl
                }
                else if((l.head.sym.flags() & FIN)!=0)
                {
                    inits.incl(def.sym.adr);
                    uninits.excl(def.sym.adr);

                    addGenerated(def.sym,tree);//argument depends on methdecl
                }
                else if((l.head.sym.flags() & FOUT)!=0)
                {
                    inits.excl(def.sym.adr);
                    uninits.incl(def.sym.adr);
                }
                else
                {
                    assert(false); //should never happen
                }
           }
            if (isInitialConstructor)
                caught = chk.union(caught, mthrown);
            else if ((tree.sym.flags() & (BLOCK | STATIC)) != BLOCK)
                caught = mthrown;
            // else we are in an instance initializer block;
            // leave caught unchanged.

			int old_nextadr=nextadr;
			//create virtual addr for writeable arguments
			for(JCVariableDecl v:method.params)
			{
				if ((v.mods.flags&Flags.FOUT)!=0)
					newVar(v.sym);
			}

            alive = true;
			resume_alive = true;

			//implicit decls have method scope
			for(Symbol vs:method.implicitSyms)
			{
				if((vs.flags_field&Flags.IMPLICITDECL)!=0)
					newVar((VarSymbol)vs);
			}

			for(Symbol vs:method.constraintsSyms.keySet())
			{
				newVar((VarSymbol)vs);
				inits.incl(((VarSymbol)vs).adr);
				uninits.excl(((VarSymbol)vs).adr);
			}

			scanStat(tree.body);

            if ((alive&&resume_alive) && tree.sym.type.getReturnType().tag != VOID&&(tree.mods.flags&Flags.FINAL)==0&&method.final_value==null)
                log.error(TreeInfo.diagEndPos(tree.body), "missing.ret.stmt");

			for(JCVariableDecl v:method.params)
			{
				if ((v.mods.flags&Flags.FOUT)!=0&&!inits.isMember(v.sym.adr))
                {
                    if(!insideWhere)
					{
	                    log.error(v,
                              "var.might.not.have.been.initialized",
                              v.sym);
					}
                }
			}

            if (isInitialConstructor) {
                for (int i = firstadr; i < nextadr; i++)
                    if (vars[i].owner == classDef.sym)
                        checkInit(TreeInfo.diagEndPos(tree.body), vars[i]);
            }
            List<PendingExit> exits = pendingExits.toList();
            pendingExits = new ListBuffer<PendingExit>();
            while (exits.nonEmpty()) {
                PendingExit exit = exits.head;
                exits = exits.tail;
                if (exit.thrown == null) {
                    assert exit.tree.getTag() == JCTree.RETURN;
                    if (isInitialConstructor) {
                        inits = exit.inits;
                        for (int i = firstadr; i < nextadr; i++)
                            checkInit(exit.tree.pos(), vars[i]);
                    }
                } else {
                    // uncaught throws will be reported later
                    pendingExits.append(exit);
                }
            }

            for(int i=old_nextadr;i<vars.length;i++)
            {
                //reset adr for later pass
                if(vars[i]!=null)
                    vars[i].adr=-1;
            }

			nextadr=old_nextadr;


        } finally {
            inits = initsPrev;
            uninits = uninitsPrev;
            nextadr = nextadrPrev;
            firstadr = firstadrPrev;
            caught = caughtPrev;
            lint = lintPrev;
            method = prevMethod;
            dg_env = prevMethod;
        }
    }

    void linearityRef(VarSymbol s)
    {
//		if(arg_out)
//			return;

		if(insideWhere&&(s.type.type_flags_field&Flags.LINEARREAD)!=0)
			log.error(current_stat.pos, "linear.read.inside.where",s);

        //FIXME: a.b.c subexp u.v.w??!!
        Set<JCTree> childs=method.generated.get(s);

        if(childs!=null&&dg_env!=null)
        {
			for(JCTree gen : childs)
			{
				if(gen!=current_stat)//no self loops!
				{
                    gen.getDGNode().addDependentChildJGT(method.depGraph,current_stat,s,dg_env,method.dg_end);

					if(inreturn)
					{
						current_stat.getDGNode().addDependentChildJGT(method.depGraph,method.dg_end,s,dg_env,method.dg_end);
						gen.getDGNode().addDependentChildJGT(method.depGraph,returntree,s,dg_env,method.dg_end);
					}
				}
			}
        }
        else if(inreturn)
        {
            current_stat.getDGNode().addDependentChildJGT(method.depGraph,method.dg_end,s,dg_env,method.dg_end);
            //gen.getDGNode().addDependentChildJGT(method.depGraph,returntree,s,dg_env,method.dg_end);
        }


        if(!enableRef||arg_out)//is it an arg of cur fun?
		{
			if(method!=null)
				method.local_vars.add(s);
		}

    }

    public void visitJoin(JCJoinDomains tree) {
		//for(JCExpression vd:tree.doms)
		//	scanExpr(vd);
		super.visitJoin(tree);
	}

	JCDomainIter currentIter=null;

	public void visitIndexed(JCArrayAccess tree) {

		//annotate iteration with info whether there is any access to array and if it accesses the curent VAL
		if(currentIter!=null&&TreeInfo.symbol(tree.indexed)==TreeInfo.symbol(currentIter.exp))
		{
			currentIter.valueAccess=true;
			if(tree.index.size()!=currentIter.domargs.size())
				currentIter.valueOffsetAccess=true;
			else
			{
				List<JCVariableDecl> vars=currentIter.domargs;
				for(JCExpression e:tree.index)
				{
					if(TreeInfo.symbol(e)!=vars.head.sym)
					{
						currentIter.valueOffsetAccess=true;
						break;
					}
					vars=vars.tail;
				}
			}
		}

		super.visitIndexed(tree); //MUST CALL SUPER!!
		//int i=0;
	}

    public void visitDomIter(JCDomainIter tree) {
		//super.visitDomIter(tree);

//		current_stat.isScheduler = true;

		JCDomainIter oldIter=currentIter;
		currentIter=tree;

		for(JCVariableDecl vd:tree.domargs)
		{
			visitVarDef(vd);
			if(trackable(vd.sym))
			{
				inits.incl(vd.sym.adr);
				uninits.excl(vd.sym.adr);
			}
		}

		if(tree.params!=null)
			for(JCExpression e:tree.params)
				scanExpr(e);

		scanExpr(tree.exp);
		if(tree.body!=null)
			scanExpr(tree.body);
		else
		{
			JCTree old_env=dg_env;

			current_stat.isScheduler = true;
			tree.dg_end = make.NewCF(null, true,tree.pos);
			tree.dg_end.scheduler=current_stat;
			method.depGraph.addVertex(tree.dg_end);
			current_stat.getDGNode().addDependentChildJGT(method.depGraph,tree.dg_end,null,current_stat,method.dg_end);

			dg_env = tree.dg_end;

			scanStat(tree.sbody);

			dg_env = old_env;
		}

		if(tree.body!=null&&tree.body.getTag()==JCTree.APPLY)
		{
			MethodSymbol s=(MethodSymbol)TreeInfo.symbol(tree.body);
			if(s.getReturnType()!=null&&s.getReturnType().tag==TypeTags.ARRAY)
			{
				s.flags_field|=Flags.ACYCLIC;//should create 2 versions, a standard one and one writing into RESULT
			}
		}

		currentIter=oldIter;
	}

    public void visitVarDef(JCVariableDecl tree) {
        boolean track = trackable(tree.sym);
        if (track && (tree.sym.owner.kind == MTH||tree.sym.owner.kind == (DOM|TYP))) newVar(tree.sym);
        if (tree.init != null) {
            Lint lintPrev = lint;
            lint = lint.augment(tree.sym.attributes_field);
            try{
                //we only want to find refs used by tree.init, so clear old refs
				Symbol s=null;
				if(tree.init!=null)
					s=TreeInfo.symbolRead(tree.init);

				if(!insideWhere&&tree.init!=null&&(s!=null&&s instanceof VarSymbol&&s.owner instanceof ClassSymbol)&&
					(tree.init.type.type_flags_field&Flags.LINEAR)!=0)
					log.error(tree.init.pos,"invalid.linear",s);

                scanExpr(tree.init);

                if (track) letInit(tree.pos(), tree.sym,false);
            } finally {
                lint = lintPrev;
            }
        }
		else
		{
            if (track)
			{
				//letInit(tree.pos(), tree.sym,false);
				if (tree.sym.adr >= firstadr && trackable(tree.sym)&&(tree.sym.flags_field&Flags.PARAMETER)==0&&tree.sym.isLocal())
                        addGenerated(tree.sym,current_stat);
			}
		}
    }

    public void visitBlock(JCBlock tree) {

        int nextadrPrev = nextadr;

		if(method==null)
			return;
		//we init only once and there is no local shadowing
		//Hashtable<VarSymbol,Set<JCTree>> old_table=(Hashtable<VarSymbol,Set<JCTree>>)method.generated.clone();

        scanStats(tree.stats);

		//inject finally here:
		if(current_stat==method.body&&method.final_value!=null)
		{
			allow_nop=true;
			scanStat(method.final_value); //add finally at the very end of method, here we also analyze it(allow_nop)
			allow_nop=false;
		}

		for(int i=nextadrPrev;i<nextadr;i++)
			if(vars[i]!=null)
				vars[i].adr=-1;

        nextadr = nextadrPrev;

		//FIXME: is this ok??
		//method.generated = old_table;

    }

    public void visitDoLoop(JCDoWhileLoop tree) {
        assert(false);
        /*
        ListBuffer<PendingExit> prevPendingExits = pendingExits;
        boolean prevLoopPassTwo = loopPassTwo;
        pendingExits = new ListBuffer<PendingExit>();
        do {
            Bits uninitsEntry = uninits.dup();
            scanStat(tree.body);
            alive |= resolveContinues(tree);
            scanCond(tree.cond);
            if (log.nerrors != 0 ||
                loopPassTwo ||
                uninitsEntry.diffSet(uninitsWhenTrue).nextBit(firstadr)==-1)
                break;
            inits = initsWhenTrue;
            uninits = uninitsEntry.andSet(uninitsWhenTrue);
            loopPassTwo = true;
            alive = true;
        } while (true);
        loopPassTwo = prevLoopPassTwo;
        inits = initsWhenFalse;
        uninits = uninitsWhenFalse;
        alive = alive && !tree.cond.type.isTrue();
        alive |= resolveBreaks(tree, prevPendingExits);

         */
        //dead!
    }

    public void visitWhere(JCWhere tree) {

//		current_stat.isScheduler = true;

        scanExpr(tree.exp);

		if((method.restype == null && method.name != method.name.table.names.init)&&inreturn&&(tree.exp.type.tsym.flags_field&Flags.LINEAR)!=0&&(return_flags&(Flags.FINAL))==0)
		{
			log.error(tree.pos,"unique.where.must.be.final",tree.exp,tree);
		}

        boolean insideWherePrev=insideWhere;
        insideWhere=true;
		JCWhere old=whereExp;
		whereExp=tree;
		if(tree.sexp!=null)
			scanExpr(tree.sexp);
		else
		{
			JCTree old_env=dg_env;
			current_stat.isScheduler = true;
			tree.dg_end = make.NewCF(null, true,tree.pos);
			tree.dg_end.scheduler=current_stat;

			method.depGraph.addVertex(tree.dg_end);
	        current_stat.getDGNode().addDependentChildJGT(method.depGraph,tree.dg_end,null,current_stat,method.dg_end);

			dg_env = tree.dg_end; //FIXME: tree is not a statement

			scanStat(tree.body);

			dg_env = old_env;

		}
        insideWhere=insideWherePrev;
		whereExp=old;
    }

    public void visitWhileLoop(JCWhileLoop tree) {
        assert(false);
        /*
        ListBuffer<PendingExit> prevPendingExits = pendingExits;
        boolean prevLoopPassTwo = loopPassTwo;
        Bits initsCond;
        Bits uninitsCond;
        pendingExits = new ListBuffer<PendingExit>();
        do {
            Bits uninitsEntry = uninits.dup();
            scanCond(tree.cond);
            initsCond = initsWhenFalse;
            uninitsCond = uninitsWhenFalse;
            inits = initsWhenTrue;
            uninits = uninitsWhenTrue;
            alive = !tree.cond.type.isFalse();
            scanStat(tree.body);
            alive |= resolveContinues(tree);
            if (log.nerrors != 0 ||
                loopPassTwo ||
                uninitsEntry.diffSet(uninits).nextBit(firstadr) == -1)
                break;
            uninits = uninitsEntry.andSet(uninits);
            loopPassTwo = true;
            alive = true;
        } while (true);
        loopPassTwo = prevLoopPassTwo;
        inits = initsCond;
        uninits = uninitsCond;
        alive = resolveBreaks(tree, prevPendingExits) ||
            !tree.cond.type.isTrue();

         */
    }

    public void visitForLoop(JCForLoop tree) {
		log.error(tree.pos,"for.loop");
        assert(false);
        /*
        ListBuffer<PendingExit> prevPendingExits = pendingExits;
        boolean prevLoopPassTwo = loopPassTwo;
        int nextadrPrev = nextadr;
        scanStats(tree.init);
        Bits initsCond;
        Bits uninitsCond;
        pendingExits = new ListBuffer<PendingExit>();
        do {
            Bits uninitsEntry = uninits.dup();
            if (tree.cond != null) {
                scanCond(tree.cond);
                initsCond = initsWhenFalse;
                uninitsCond = uninitsWhenFalse;
                inits = initsWhenTrue;
                uninits = uninitsWhenTrue;
                alive = !tree.cond.type.isFalse();
            } else {
                initsCond = inits.dup();
                initsCond.inclRange(firstadr, nextadr);
                uninitsCond = uninits.dup();
                uninitsCond.inclRange(firstadr, nextadr);
                alive = true;
            }
            scanStat(tree.body);
            alive |= resolveContinues(tree);
            scan(tree.step);
            if (log.nerrors != 0 ||
                loopPassTwo ||
                uninitsEntry.dup().diffSet(uninits).nextBit(firstadr) == -1)
                break;
            uninits = uninitsEntry.andSet(uninits);
            loopPassTwo = true;
            alive = true;
        } while (true);
        loopPassTwo = prevLoopPassTwo;
        inits = initsCond;
        uninits = uninitsCond;
        alive = resolveBreaks(tree, prevPendingExits) ||
            tree.cond != null && !tree.cond.type.isTrue();
        nextadr = nextadrPrev;

         */
    }

    public void visitForeachLoop(JCEnhancedForLoop tree) {
        assert(false);
        /*
        visitVarDef(tree.var);

        ListBuffer<PendingExit> prevPendingExits = pendingExits;
        boolean prevLoopPassTwo = loopPassTwo;
        int nextadrPrev = nextadr;
        scan(tree.expr);
        Bits initsStart = inits.dup();
        Bits uninitsStart = uninits.dup();

        //letInit(tree.pos(), tree.var.sym);
        scanExpr(tree.var);

        pendingExits = new ListBuffer<PendingExit>();
        do {
            Bits uninitsEntry = uninits.dup();
            scanStat(tree.body);
            alive |= resolveContinues(tree);
            if (log.nerrors != 0 ||
                loopPassTwo ||
                uninitsEntry.diffSet(uninits).nextBit(firstadr) == -1)
                break;
            uninits = uninitsEntry.andSet(uninits);
            loopPassTwo = true;
            alive = true;
        } while (true);
        loopPassTwo = prevLoopPassTwo;
        inits = initsStart;
        uninits = uninitsStart.andSet(uninits);
        resolveBreaks(tree, prevPendingExits);
        alive = true;
        nextadr = nextadrPrev;
         *
         */
    }

    public void visitLabelled(JCLabeledStatement tree) {
        ListBuffer<PendingExit> prevPendingExits = pendingExits;
        pendingExits = new ListBuffer<PendingExit>();
        scanStat(tree.body);
        alive |= resolveBreaks(tree, prevPendingExits);
    }

    public void visitCaseExp(JCCaseExp tree) {
        int nextadrPrev = nextadr;

        scanExpr(tree.exp);

        boolean hasDefault = false;

        Bits start_inits = inits.dup();
        Bits start_uninits = uninits.dup();

        boolean may_survive = false;

        if(tree.list.head.stmnt!=null)
            scanStat(tree.list.head.stmnt);
        else
            scanExpr(tree.list.head.res);

        Bits initsSwitch = inits.dup();
        Bits uninitsSwitch = uninits.dup();

        uninits = start_uninits.dup();

        for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {

            alive = true;
            JCSelectCond c = l.head;

            if (c.cond == null)
                hasDefault = true;
            else
                scanExpr(c.cond);

            if(c.stmnt!=null)
                scanStat(c.stmnt);
            else
                scanExpr(c.res);

            may_survive|=alive;

            //implicit break for every case!
            initsSwitch.andSet(inits);
            uninitsSwitch.andSet(uninits);
            inits = start_inits.dup();
            uninits = start_uninits.dup();

        }


        if (!hasDefault) {
            inits.andSet(initsSwitch);
            uninits.andSet(uninitsSwitch);
            alive = true; //assume non exhaustive match
        }
        else
        {
            inits = initsSwitch;
            uninits = uninitsSwitch;
        }

        alive |= may_survive;

		for(int i=nextadrPrev;i<nextadr;i++)
			if(vars[i]!=null)
				vars[i].adr=-1;

        nextadr = nextadrPrev;
    }

    public void visitSelect(JCFieldAccess tree) {

        scanExpr(tree.selected);

        //super.visitSelect(tree);
    }

    public void visitSelectExp(JCSelect tree) {
        int nextadrPrev = nextadr;
        boolean hasDefault = false;

        Bits start_inits = inits.dup();
        Bits start_uninits = uninits.dup();

        boolean may_survive = false;

        if(tree.list.head.stmnt!=null)
            scanStat(tree.list.head.stmnt);
        else
            scanExpr(tree.list.head.res);

        Bits initsSwitch = inits.dup();
        Bits uninitsSwitch = uninits.dup();

        uninits = start_uninits.dup();

        for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {

            alive = true;
            JCSelectCond c = l.head;

            if (c.cond == null)
                hasDefault = true;
            else
                scanExpr(c.cond);

            if(c.stmnt!=null)
                scanStat(c.stmnt);
            else
                scanExpr(c.res);

            may_survive|=alive;

            //implicit break for every case!
            initsSwitch.andSet(inits);
            uninitsSwitch.andSet(uninits);
            inits = start_inits.dup();
            uninits = start_uninits.dup();
        }


        if (!hasDefault) {
            inits.andSet(initsSwitch);
            uninits.andSet(uninitsSwitch);
            alive = true; //assume non exhaustive match
        }
        else
        {
            inits = initsSwitch;
            uninits = uninitsSwitch;
        }

        alive |= may_survive;

		for(int i=nextadrPrev;i<nextadr;i++)
			if(vars[i]!=null)
				vars[i].adr=-1;

        nextadr = nextadrPrev;
    }

	public Hashtable<VarSymbol,Set<JCTree>> deepCopy(Hashtable<VarSymbol,Set<JCTree>> map)
	{
		Hashtable<VarSymbol,Set<JCTree>> result=new Hashtable<VarSymbol,Set<JCTree>>();
		for(VarSymbol v:map.keySet())
		{
			Set<JCTree> ns=new LinkedHashSet<JCTree>();
			ns.addAll(map.get(v));
			result.put(v,ns);
		}
		return result;
	}

	public void join(Hashtable<VarSymbol,Set<JCTree>> res,Hashtable<VarSymbol,Set<JCTree>> map)
	{
		for(VarSymbol v:map.keySet())
		{
			Set<JCTree> cur=res.get(v);
			if(cur==null)
				cur=new LinkedHashSet<JCTree>();

			cur.addAll(map.get(v));

			res.put(v, cur);
		}
	}

    public void visitIf(JCIf tree) {

        scanCond(tree.cond);

        Bits initsBeforeElse = initsWhenFalse;
        Bits uninitsBeforeElse = uninitsWhenFalse;
        inits = initsWhenTrue;
        uninits = uninitsWhenTrue;

        JCTree old_env = dg_env;

        JCCF cf_true=make.NewCF(tree.cond, true,tree.pos);
		/*
        cf_true.exit=make.at(tree.pos).Skip();
        cf_true.exit.scheduler=cf_true;
        cf_true.exit.nop=true;
		* */
        method.depGraph.addVertex(cf_true);
		//method.depGraph.addVertex(cf_true.exit);

        tree.getDGNode().addDependentChildJGT(method.depGraph,cf_true,null,tree,method.dg_end);
        cf_true.scheduler=tree;
//		tree.cf_true=cf_true;

		Hashtable<VarSymbol,Set<JCTree>> old_generated=deepCopy(method.generated);

        dg_env = cf_true;

		JCTree old_branch = current_branch;
		current_branch = tree.thenpart;
        scanStat(tree.thenpart);

		old_branch.transitive_returns+=current_branch.transitive_returns;
		cf_true.transitive_returns=current_branch.transitive_returns;
		cf_true.local_returns=current_branch.local_returns;

		Hashtable<VarSymbol,Set<JCTree>> true_generated=deepCopy(method.generated);
		method.generated=old_generated;

        if (tree.elsepart != null)
		{
			current_branch = tree.elsepart;
            JCCF cf_false=make.NewCF(tree.cond, false,tree.pos);
			/*
            cf_false.exit=make.at(tree.pos+1).Skip();
            cf_false.exit.scheduler=cf_false;
            cf_false.exit.nop=true;
			*/

            method.depGraph.addVertex(cf_false);
			//method.depGraph.addVertex(cf_false.exit);

            tree.getDGNode().addDependentChildJGT(method.depGraph,cf_false,null,tree,method.dg_end);
            cf_false.scheduler=tree;
            cf_false.getDGNode().addDependentChildJGT(method.depGraph,method.dg_end, null, null, null);
            dg_env = cf_false;
//			tree.cf_false=cf_false;

            boolean aliveAfterThen = alive;
            alive = true;
            Bits initsAfterThen = inits.dup();
            Bits uninitsAfterThen = uninits.dup();
            inits = initsBeforeElse;
            uninits = uninitsBeforeElse;

            scanStat(tree.elsepart);

			old_branch.transitive_returns+=current_branch.transitive_returns;
			cf_false.transitive_returns=current_branch.transitive_returns;
			cf_false.local_returns=current_branch.local_returns;

            inits.andSet(initsAfterThen);
            uninits.andSet(uninitsAfterThen);

            alive = alive | aliveAfterThen;
        } else {
            inits.andSet(initsBeforeElse);
            uninits.andSet(uninitsBeforeElse);
            alive = true;
        }

		join(method.generated,true_generated);

		current_branch = old_branch;

        dg_env = old_env;
    }

    public void visitIfExp(JCIfExp tree) {
        scanCond(tree.cond);

        Bits initsBeforeElse = initsWhenFalse;
        Bits uninitsBeforeElse = uninitsWhenFalse;
        inits = initsWhenTrue;
        uninits = uninitsWhenTrue;
        scanExpr(tree.thenpart);
        if (tree.elsepart != null) {
            boolean aliveAfterThen = alive;
            alive = true;
            Bits initsAfterThen = inits.dup();
            Bits uninitsAfterThen = uninits.dup();
            inits = initsBeforeElse;
            uninits = uninitsBeforeElse;
	        scanExpr(tree.elsepart);
    //        scanStat(tree.elsepart);
            inits.andSet(initsAfterThen);
            uninits.andSet(uninitsAfterThen);
            alive = alive | aliveAfterThen;
        } else {
            inits.andSet(initsBeforeElse);
            uninits.andSet(uninitsBeforeElse);
            alive = true;
        }
    }

    public void visitBreak(JCBreak tree) {
        recordExit(tree);
    }

    public void visitContinue(JCContinue tree) {
        recordExit(tree);
    }

    public void visitReturn(JCReturn tree) {

		if((tree.flags&(Flags.FINAL))==0)//only count non finally
		{
			current_branch.transitive_returns ++;
			current_branch.local_returns ++;
			method.exits.add(current_stat);
		}

		//make sure tail recursion is marked as loop
		if(tree.expr!=null&&tree.expr.getTag()==JCTree.APPLY)
		{
			if(TreeInfo.symbol(((JCMethodInvocation)tree.expr).meth)==method.sym)
				method.sym.flags_field|=Flags.LOOP;
		}

		inreturn = true;
		return_flags=tree.flags;
		returntree = tree;
        scanExpr(tree.expr);
		inreturn = false;

		Symbol s=null;
		if(tree.expr!=null)
			s=TreeInfo.symbolRead(tree.expr);

		if(!insideWhere&&tree.expr!=null&&(s!=null&&s instanceof VarSymbol&&!s.name.equals(names._this)&&s.owner instanceof ClassSymbol)&&
			(tree.expr.type.type_flags_field&Flags.LINEAR)!=0)
			log.error(tree.expr.pos,"invalid.linear",s);

		for(JCVariableDecl v:method.params)
		{

			if ((v.mods.flags&Flags.FOUT)!=0&&(v.sym.flags_field&Flags.PARAMETER)!=0)
			{
				if(method.generated.get(v.sym)!=null)
				for(JCTree gen:method.generated.get(v.sym)) //add deps to decl and init of var
					if(gen!=current_stat)
						gen.getDGNode().addDependentChildJGT(method.depGraph,current_stat,v.sym,current_stat,method.dg_end);

				if(!inits.isMember(v.sym.adr))
					log.error(v,
						  "var.might.not.have.been.initialized",
						  v.sym);
			}
		}

		if((tree.flags&(Flags.SYNCHRONIZED|Flags.FINAL))==0)
		{
			method.hasResume=true;
			recordResume(tree); //produces retval but does NOT kill current branch
		}
		else
			recordExit(tree);

    }

    public void visitThrow(JCThrow tree) {
        scanExpr(tree.expr);
        markThrown(tree, tree.expr.type);
        markDead();
    }


    public void visitApply(JCMethodInvocation tree) {


        scanExpr(tree.meth);

//        scanExprs(tree.args);

        //Type t=attr.attribTree(tree.meth, env,VAL|VAR|DOM|TYP,Type.noType);

		Symbol sym=TreeInfo.symbol(tree.meth);
		if((sym instanceof MethodSymbol))
		{
			MethodSymbol meth =(MethodSymbol)sym;
			Iterator<VarSymbol> i=meth.params().iterator();

			boolean old_arg_out=arg_out;

			VarSymbol s=null;
			for(JCExpression e:tree.args)
			{
				if(i.hasNext())//var args
					s=i.next();
				arg_out=((s.flags_field&Flags.FOUT)!=0);

				if(arg_out&&TreeInfo.symbol(e)!=null&&TreeInfo.symbol(e).time!=null)
				{
					current_stat.time.add((VarSymbol)TreeInfo.symbol(e));
				}

				scanExpr(e);
			}

			arg_out=old_arg_out;
			//scanExprs(tree.args);

	//		if((meth.flags_field&Flags.BLOCKING)!=0&&tree.getTriggerReturn()==null)
	//			current_stat.is_blocking=true;

			if(meth.groups.length()>0)
				current_stat.groups.add(meth.groups);
			if(meth.threads.length()>0)
				current_stat.threads.add(meth.threads);

			if(tree.meth.type!=null)
				for (List<Type> l = tree.meth.type.getThrownTypes(); l.nonEmpty(); l = l.tail)
					markThrown(tree, l.head);
		}
		else
			log.error(tree.pos(), "unknown.domain",sym);
    }

    public void visitNewClass(JCNewClass tree) {
        scanExpr(tree.encl);
        scanExprs(tree.args);
       // scan(tree.def);
        for (List<Type> l = tree.constructorType.getThrownTypes();
             l.nonEmpty();
             l = l.tail) {
            markThrown(tree, l.head);
        }
        List<Type> caughtPrev = caught;
        try {
            // If the new class expression defines an anonymous class,
            // analysis of the anonymous constructor may encounter thrown
            // types which are unsubstituted type variables.
            // However, since the constructor's actual thrown types have
            // already been marked as thrown, it is safe to simply include
            // each of the constructor's formal thrown types in the set of
            // 'caught/declared to be thrown' types, for the duration of
            // the class def analysis.
            if (tree.def != null)
                for (List<Type> l = tree.constructor.type.getThrownTypes();
                     l.nonEmpty();
                     l = l.tail) {
                    caught = chk.incl(l.head, caught);
                }
            scan(tree.def);
        }
        finally {
            caught = caughtPrev;
        }
    }

    public void visitNewArray(JCNewArray tree) {

        scanExprs(tree.dims);
        scanExprs(tree.elems);
    }

    public void visitAssert(JCAssert tree) {
        Bits initsExit = inits.dup();
        Bits uninitsExit = uninits.dup();
        scanCond(tree.cond);
        uninitsExit.andSet(uninitsWhenTrue);
        if (tree.detail != null) {
            inits = initsWhenFalse;
            uninits = uninitsWhenFalse;
            scanExpr(tree.detail);
        }
        //???
        inits = initsExit;
        uninits = uninitsExit;
    }

    public void visitAssign(JCAssign tree) {
        JCTree lhs = TreeInfo.skipParens(tree.lhs);
        //if (!(lhs instanceof JCIdent))  //why this???

		//FIXME: handle varinit!
		if(insideWhere&&is_atomic)
		{
			whereExp.atomic = new AtomicTarget();//store info about atomic update
			method.atomic_where = whereExp; //store info at method
			whereExp.atomic.target=(VarSymbol) TreeInfo.symbol(tree.lhs);

			whereExp.atomic.target.flags_field|=Flags.ATOMIC;//mark variable as atomic..so we can use tbb::atomic<>

			whereExp.atomic.temp=tree.rhs;

			//allow assignments on non-primitive...
			//if(!whereExp.atomic.target.type.isPrimitive())
			//	log.error(tree.pos,"atomic.non.primitive", whereExp.atomic.target,whereExp.atomic.target.type);

			//
			if(tree.cond!=null)
			{
				//comp exchg!
				whereExp.atomic.comp=tree.cond;
				whereExp.atomic.aflag=tree.aflags;
				whereExp.atomic.type=AtomicTarget.AtomicType.CMPXCHG;
			}
			else
			{
				Symbol s=TreeInfo.symbol(tree.rhs);

				if(method.uses_field.isEmpty())
				{
					whereExp.atomic.type=AtomicTarget.AtomicType.INC;
				}
				else if(s!=null)//!=null if rhs is just an item (postinc)
				{
					whereExp.atomic.type=AtomicTarget.AtomicType.POSTINC;
				}
				else //(preinc) hopefully an atomic op!
				{
					whereExp.atomic.type=AtomicTarget.AtomicType.PREINC;
				}
			}
		}

		//scan rhs first so that atomic access sees reads before writes!
		//, this is ok here since we don't collect any references (like alias, ...)
		scanExpr(tree.rhs);

		boolean oao=arg_out;
		arg_out=true;
		scanExpr(lhs);
		arg_out=oao;


		Symbol s=null;
		if(tree.rhs!=null)
			s=TreeInfo.symbolRead(tree.rhs);

		/*
		Symbol ls=TreeInfo.symbol(lhs);
		if(!insideWhere&&(ls.flags_field&Flags.PARAMETER)!=0&&(ls.flags_field&Flags.FOUT)!=0)
			log.error(tree.pos,"outparam.outside.where",ls);
		*/

		if(TreeInfo.symbol(lhs).time!=null)
		{
			current_stat.time.add((VarSymbol)TreeInfo.symbol(lhs));
		}

/*
		if(!insideWhere&&tree.rhs!=null&&(s!=null&&s instanceof VarSymbol&&s.owner instanceof ClassSymbol)&&
			(tree.rhs.type.type_flags_field&Flags.LINEAR)!=0)
			log.error(tree.rhs.pos,"invalid.linear",s);
*/
        //letInit(lhs,false);
    }

	public void visitArgExpression(JCArgExpression tree) {
        JCTree lhs = tree.exp1;

		if(tree.exp2==null)
		{
			scanExpr(lhs);

		}

		if(tree.exp2!=null)
		{
			boolean old_out=arg_out;
			arg_out=false;
			scanExpr(tree.exp2);
			arg_out=old_out;
			scanExpr(lhs);

		}

	}

    public void visitAssignop(JCAssignOp tree) {
        scanExpr(tree.lhs);

        scanExpr(tree.rhs);

        letInit(tree.lhs,false);
    }

    public void visitUnary(JCUnary tree) {
        switch (tree.getTag()) {
        case JCTree.NOT:
            scanCond(tree.arg);
            Bits t = initsWhenFalse;
            initsWhenFalse = initsWhenTrue;
            initsWhenTrue = t;
            t = uninitsWhenFalse;
            uninitsWhenFalse = uninitsWhenTrue;
            uninitsWhenTrue = t;

            break;
        case JCTree.PREINC: case JCTree.POSTINC:
        case JCTree.PREDEC: case JCTree.POSTDEC:
            scanExpr(tree.arg);
            letInit(tree.arg,false);
            break;
        default:
            scanExpr(tree.arg);
        }
    }

    public void visitBinary(JCBinary tree) {
        switch (tree.getTag()) {
        case JCTree.AND:
            scanCond(tree.lhs);
            Bits initsWhenFalseLeft = initsWhenFalse;
            Bits uninitsWhenFalseLeft = uninitsWhenFalse;
            inits = initsWhenTrue;
            uninits = uninitsWhenTrue;
            scanCond(tree.rhs);
            initsWhenFalse.andSet(initsWhenFalseLeft);
            uninitsWhenFalse.andSet(uninitsWhenFalseLeft);
            break;
        case JCTree.OR:
            scanCond(tree.lhs);
            Bits initsWhenTrueLeft = initsWhenTrue;
            Bits uninitsWhenTrueLeft = uninitsWhenTrue;
            inits = initsWhenFalse;
            uninits = uninitsWhenFalse;
            scanCond(tree.rhs);
            initsWhenTrue.andSet(initsWhenTrueLeft);
            uninitsWhenTrue.andSet(uninitsWhenTrueLeft);
            break;
        default:
            scanExpr(tree.lhs);
            scanExpr(tree.rhs);
        }
    }

    public void visitIdent(JCIdent tree) {
        if (tree.sym.kind == VAR)
        {
			if(method!=null&&tree.sym.owner==method.sym.owner&&(tree.sym.flags_field&Flags.STATIC)==0)
			{
				if(!tree.name.equals(names._this))//this is no field..what about arrays and selects?
				{

					if(is_event&&is_evo&&is_atomic)
					{
						if(method.uses_field.contains(tree.sym)||method.uses_field.size()>0)
						{
							boolean allow_second_access=whereExp.atomic.type!=AtomicTarget.AtomicType.CMPXCHG&&arg_out;
							if(!(allow_second_access&&method.uses_field.contains(tree.sym)))
								log.error(tree.pos,"atomic.fields",method.uses_field);
						}
					}

					method.uses_field.add((VarSymbol)tree.sym);
				}
			}

			if(!arg_out)
			{
				checkInit(tree.pos(), (VarSymbol)tree.sym);
	            linearityRef((VarSymbol)tree.sym);
			}
			else
			{
				letInit(tree,arg_out&&((tree.sym.flags_field&Flags.FINOUT)==Flags.FINOUT||(tree.sym.type.type_flags_field&Flags.LINEAR)!=0));
				if(tree.sym.isLocal()&&(tree.sym.flags_field&Flags.PARAMETER)==0)
					linearityRef((VarSymbol)tree.sym);
			}
        }
    }

    public void visitTypeCast(JCTypeCast tree) {
        super.visitTypeCast(tree);

		if(tree.expr.type.isReadLinear()&&(tree.clazz.type.isLinear()&&!tree.clazz.type.isReadLinear()))
		{
			if(tree.expr.getTag()!=JCTree.IDENT)
				log.error(tree.pos, "complex.linear.read.cast",tree.expr);
			else
			{
				VarSymbol vs=((VarSymbol)TreeInfo.symbol(tree.expr));

				Set<JCTree> childs=method.generated.get(vs);

				for(JCTree t:childs)
				{
					if(t.getTag()==JCTree.EXEC&&((JCTree.JCExpressionStatement)t).expr.getTag()==JCTree.ASSIGN)
					{
						Symbol src=(VarSymbol)TreeInfo.symbol(((JCAssign)(((JCTree.JCExpressionStatement)t).expr)).rhs);
						if(src==null||!src.type.isLinear()||src.type.isReadLinear())
							log.error(t.pos, "must.use.read.linear.from.linear",tree.expr,t);
					}
					if(t.getTag()==JCTree.VARDEF)
					{
						Symbol src=TreeInfo.symbol(((JCTree.JCVariableDecl)t).init);
						if(src==null||!src.type.isLinear()||src.type.isReadLinear())
							log.error(t.pos, "must.use.read.linear.from.linear",tree.expr,t);
					}
                    else if((t.getTag()==JCTree.EXEC&&((JCTree.JCExpressionStatement)t).expr.getTag()==JCTree.ASSIGN&&(TreeInfo.symbol(((JCTree.JCAssign)((JCTree.JCExpressionStatement)t).expr).lhs).flags_field&Flags.IMPLICITDECL)!=0&&((JCTree.JCAssign)((JCTree.JCExpressionStatement)t).expr).lhs.type.isReadLinear()))
                    {
						Symbol src=TreeInfo.symbol(((JCTree.JCAssign)((JCTree.JCExpressionStatement)t).expr).rhs);
						if(src==null||!src.type.isLinear()||src.type.isReadLinear())
							log.error(t.pos, "must.use.read.linear.from.linear",tree.expr,t);
                    }
					else
						log.error(t.pos, "must.use.read.linear.from.linear",tree.expr,t);
				}

				method.readlinear.put(current_stat,childs);
				method.readlinearsym.put(current_stat,vs);
				vs.flags_field|=Flags.LINEARREAD; //mark as used
			}
		}

        if (!tree.type.isErroneous()
            && lint.isEnabled(Lint.LintCategory.CAST)
            && types.isSameType(tree.expr.type, tree.clazz.type)) {
            log.warning(tree.pos(), "redundant.cast", tree.expr.type);
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

            enableRef = false;
            insideWhere = false;

            inits = new Bits();
            uninits = new Bits();

            uninitsTry = new Bits();
            initsWhenTrue = initsWhenFalse =
                uninitsWhenTrue = uninitsWhenFalse = null;
            if (vars == null)
                vars = new VarSymbol[32];
            else
                for (int i=0; i<vars.length; i++)
                    vars[i] = null;
            firstadr = 0;
            nextadr = 0;
            pendingExits = new ListBuffer<PendingExit>();
            alive = true;
			inreturn = false;
            this.thrown = this.caught = null;
            this.classDef = null;
            scan(tree);
        } finally {
            // note that recursive invocations of this method fail hard
            inits = uninits = uninitsTry = null;
            initsWhenTrue = initsWhenFalse =
                uninitsWhenTrue = uninitsWhenFalse = null;
            if (vars != null) for (int i=0; i<vars.length; i++)
                vars[i] = null;
            firstadr = 0;
            nextadr = 0;
            pendingExits = null;
            this.make = null;
            this.thrown = this.caught = null;
            this.classDef = null;
        }
    }
}
