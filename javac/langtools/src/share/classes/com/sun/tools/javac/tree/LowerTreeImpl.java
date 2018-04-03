/*
 * Copyright 2011-2012 TU-München
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 */
package com.sun.tools.javac.tree;

import java.io.*;
import java.util.*;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.code.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;

/**
 * Prints out a tree as an indented Java source program.
 *
 * <p><b>This is NOT part of any API supported by Sun Microsystems. If you write code that depends
 * on this, you do so at your own risk. This code and its internal interfaces are subject to change
 * or deletion without notice.</b>
 */
//forward visitor calls to proper emitter
public class LowerTreeImpl extends LowerTree {

	public LowerAtomicTBB atomicEmitter;
	public LowerDataRT arrayEmitter;
	public CopyArray arrayCopyEmitter;
	public LowerDataOCL openCLArrayEmitter;
        //public LowerTasksMPI taskEmitter;
    public LowerTasksTBB taskEmitter;
	public LowerTypeImpl typeEmitter;
	public LowerSingular singularEmitter;
	public LowerSequential sequentialEmitter;


// ------------------- actual code emitter ---------------
	public LowerTreeImpl(Context context, Writer out, Writer outGraph, boolean sourceOutput, boolean header, boolean lineDebugInfo, boolean fgc) {
		super(context, out, outGraph, sourceOutput, header, lineDebugInfo, fgc);
		debug_print_task = fgc;
		profile = jc.profile;

		//create different emitters and pass ref to state
		atomicEmitter = new LowerAtomicTBB(this);
		arrayEmitter = new LowerDataRT(this);
        arrayCopyEmitter = new CopyArray(this);
		openCLArrayEmitter = new LowerDataOCL(this);

        taskEmitter = new LowerTasksTBB(this);
        //taskEmitter = new LowerTasksMPI(this);

		typeEmitter = new LowerTypeImpl(this);
		singularEmitter = new LowerSingular(this);
		sequentialEmitter = new LowerSequential(this,jc.callGraph,jc.topolNodes,fgc);
	}

	//------------------- NOTE ----------------------------
	//the emitter is split into several more or less orthogonal parts (Sequential, Singular, DataParallel, TaskParallel, OpenCL, MPI)
	//since much of the state is needed in most parts, a reference to the complete state is passed to the parts at construction
	//so in this file all calls are redirected to the proper part of the emitter

	//various maps that store info calced in preparePaths
	public Map<iTask, String> task_map; //get name from path
	public Map<iTask, Integer> in_map; //get input deps from path
	public Map<iTask, Boolean> joining_map; //does path contribute to finally expression

	public Map<VarSymbol, JCExpression> index_map = null; //for reinterpreted domains (replace var returned from cloog by user supplied index)
	public Map<Symbol, String> subst_map = null; //for reinterpreted domains (replace var returned from cloog by user supplied index)
	public Map<VarSymbol, JCExpression> lastProjectionArgs=null; //m.proj(exp). ... : we store the previous projection arg so it can be used later on (iter/domsize may depend on proj arg)

	public Set<VarSymbol> path_outcom = null; //vars that have outbound dependencies

	public int joining_paths = 0; //number of tasks in a method that contribute to finally (not necessarily directly but also transitively)
	public int kernel_joining_paths = 0; //same as before but when generating two versions for recursive parallel stuff like fib)
	public boolean task_return = true; //does the current task contain any return statements (cancel or resume)
    public int branch_returns = 0; //number of already encountered returns
    public boolean returns_inside_task = false;
	public boolean kernel_task_return = false;
    public boolean waiting = false; //did we already emit a wait_for_all on self_task?
	//tailRec:
	public boolean use_local_this = false; //tail rec methods cannot use this ptr for writing
	public JCTree loop_label_sched = null; //tail rec loop label
	public boolean loop_label_kernel = false;
        public boolean in_mpi_rec = false; //indicates if in an _MPI method
	//replace a symbol by a different symbol (e.g. for atomic ops)
	public Symbol symbolFind = null; //if this sym is encountered during output ... then the following sym is used instead
	public Symbol symbolReplace = null; //see prev comment
	public boolean atomic_processed = false; //was it possible to translate statement into atomic op?

	//method properties set in visitMethodDecl
	public boolean is_event = false; //is method event from EVO
	public boolean is_linear = false; //is return type linear?
	public boolean is_static = false; //is method static
	public boolean is_operator = false; //is method a operator
	public boolean is_constructor = false; //iis method a constructor
	public boolean is_evo = false; //is method inside EVO
	public boolean is_sample = false; //is method a sampling method from EVOs
	public boolean is_void = false; //does method not return anything
	public boolean is_context_refcount = false; //do we need a heap allocated context or is it guaranteed that the comntext is not required after the method exits
	public boolean is_atomic = false; //may contain only atomic ops
	public boolean is_blocking = false; //may not return indefinitely

	public JCTree current_scheduler = null;//method or CF
	public boolean method_has_context = false; //if there are tasks inside the method we need a context for communicated vars, ret vals, ... (basically a stack frame)
	public boolean inside_method = false; //are we currently generating code from inside a method
	public boolean insideArray = false; //special handling when printing element type of an array
	public boolean generate_into = false; //forward array into method call to avoid copying
	public MethodSymbol redirect_recursion; //switch to _IMPL1 version of method (for kernel stuff like fib)
	public int spawns = 0; //how many tasks are spawned by current_method
	public int blocking_spawns = 0; //how many blocking spawns?
	public Set<VarSymbol> com = new LinkedHashSet<VarSymbol>(); //which variables are communicated by current task
	public Set<VarSymbol> blocking_com = new LinkedHashSet<VarSymbol>(); //which blocking variables are communicated by current task
	//kernel: recursive methods that spawn tasks will do so only for a limited amount of recursion levels
	public boolean kernel = false; //do we need switch from soawning to non-spawning?
	public boolean inside_kernel = false; //are we inside non_spawning section
	public boolean dump_kernel = false; //for dbg output (dot file)
	public Set<iTask> methodpaths = null; //all paths associated with current_method
	public int profile = 0; //dump profiling code (if >0)
	public String current_group = ""; //what is the current group (if any) -> stuff in the same group are never executed concurrently
	public String current_class = ""; //name of the class being compiled
	public JCClassDecl enclClass = null; //decl of current class
	public JCTree current_tree = null; //what is the current statement being processed?
	public boolean allowUnconditional = false; //where {} and domiter {} have unconditional CF blocks which are not always enabled
	public boolean allow_final = false; //finally constructs are skipped unless specifically enabled when generating method footer
	public String inside_task = null; //what is the task we currently process (null if none)

	public boolean debug_print_task = true; //custom sig handler for dbg
	public Set<String> context_names = null; //already created contexts
	public Set<VarSymbol> time = new LinkedHashSet<VarSymbol>();//for #TIME pragma

	public boolean insideParallelRegion = false; //are we inside a parallel region? -> only parallelize outer loops

	//we have a stack of IterState instances to hold the info of the (possibly nested) domain iterations
	public static class IterState implements Cloneable {

		public JCDomainIter iter = null; //AST tre node
        public boolean stepForLoop = false; //standard codegen for for loops or special case for step wise execution
		public JCTree enclosing = null; //statement node that contains the dom iter
		public JCTree.JCCompilationUnit code = null; //code of encountered type
		public JCTree.JCForLoop inner = null; //inner for loop from cloog
		public JCTree.JCMethodInvocation index = null; //S1(j,k) index to access from cllog
		public Set<VarSymbol> used = null; //used vars
		public VarSymbol inner_counter = null; //counter var of inner most for loop
		public boolean fix_inner_counter = false; //apply offset correction to inner (if encountered)
		public Set<VarSymbol> counter = null; //all counters
		public Set<VarSymbol> usedcounter = null; //all counters
		public boolean void_iter = false; //does iteration produce result? (if actual write is pushed into inner loops then this may be true even if you would expect a result!)
		public MethodSymbol forwardCall = null; //forward output to method (instead of copying method result)
		public boolean consecutive = false; //can we apply a vectorizig map or do we need a traditional for loop
		public boolean reduce = false; //is iter a reduction
		public boolean noreturn = false; //are there any returns in the iter?
		public JCDomainIter reduceiter = null; // reduction of partially reduced items
		public Type.ArrayType domain = null; //array type with dt domain from visitDomIter for fowrards
		public Type.ArrayType type=null; //at as calced in visitDomIter
		public int uid; //UID so we can access outer data in nested iters
		public int loopCount=0;

		IterState(int uid)
		{
			this.uid=uid;
		}

		public IterState clone() {
			try {
				IterState res= (IterState) super.clone();
				return res;
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}
	}

	public IterState domainIterState = new IterState(0);
	public Stack<IterState> nestedIterations = new Stack<IterState>();

	//-------------- SEQUENTIAL EMITTER ------------------------

	/**
	 * Derived visitor method: print statement tree.
	 */
	public void printStat(JCTree tree) throws IOException {
		sequentialEmitter.printStat(tree);
	}

	public void printAnnotations(List<JCAnnotation> trees) throws IOException {
		sequentialEmitter.printAnnotations(trees);
	}

	/**
	 * Print documentation comment, if it exists
	 *
	 * @param tree The tree for which a documentation comment should be printed.
	 */
	public void printDocComment(JCTree tree) throws IOException {
		sequentialEmitter.printDocComment(tree);
	}

	/**
	 * Print a block.
	 */
	public void printBlock(List<? extends JCTree> stats) throws IOException {
		sequentialEmitter.printBlock(stats);
	}

	/**
	 * Print a block.
	 */
	public void printEnumBody(List<JCTree> stats) throws IOException {
		sequentialEmitter.printEnumBody(stats);
	}

	/**
	 * Print unit consisting of package clause and import statements in toplevel, followed by class
	 * definition. if class definition == null, print all definitions in toplevel.
	 *
	 * @param tree The toplevel tree
	 * @param cdef The class definition, which is assumed to be part of the toplevel tree.
	 */
	public void printUnit(JCCompilationUnit tree, JCClassDecl cdef) throws IOException {
		sequentialEmitter.printUnit(tree, cdef);
	}

	/**
	 * ************************************************************************
	 * Visitor methods ***********************************************************************
	 */
	public void visitTopLevel(JCCompilationUnit tree) {
		sequentialEmitter.visitTopLevel(tree);
	}

	public void visitImport(JCImport tree) {
		sequentialEmitter.visitImport(tree);
	}

	//obsolete?
	public void visitCTProperty(JCCTProperty tree) {
		sequentialEmitter.visitCTProperty(tree);
	}

	public void visitClassDef(JCClassDecl tree) {
		sequentialEmitter.visitClassDef(tree);
	}

	public void visitMethodHeader(JCMethodDecl tree) {
		sequentialEmitter.visitMethodHeader(tree);
	}

	public void visitMethodDef(JCMethodDecl tree) {
		sequentialEmitter.visitMethodDef(tree);
	}

	public void visitVarDef(JCVariableDecl tree) {
		sequentialEmitter.visitVarDef(tree);
	}

	public void visitSkip(JCSkip tree) {
		sequentialEmitter.visitSkip(tree);
	}

	public void visitCF(JCCF tree) {
		sequentialEmitter.visitCF(tree);
	}

	public void visitBlock(JCBlock tree) {
		sequentialEmitter.visitBlock(tree);
	}

	public void visitIf(JCIf tree) {
		sequentialEmitter.visitIf(tree);
	}

	public void visitIfExp(JCIfExp tree) {
		sequentialEmitter.visitIfExp(tree);
	}

	public void visitExec(JCExpressionStatement tree) {
		sequentialEmitter.visitExec(tree);
	}

	public void visitReturn(JCReturn tree) {
		sequentialEmitter.visitReturn(tree);
	}

	public void visitPragma(JCPragma tree) {
		//nothing to do...
	}

	public void visitAssert(JCAssert tree) {
		sequentialEmitter.visitAssert(tree);
	}

	public void visitSizeOf(JCSizeOf tree) {
		sequentialEmitter.visitSizeOf(tree);
	}

	public void visitApply(JCMethodInvocation tree) {
		sequentialEmitter.visitApply(tree);
	}

	public void visitNewClass(JCNewClass tree) {
		sequentialEmitter.visitNewClass(tree);
	}

	public void visitParens(JCParens tree) {
		sequentialEmitter.visitParens(tree);
	}

	public void visitAssign(JCAssign tree) {
		sequentialEmitter.visitAssign(tree);
	}

	public void visitWhere(JCWhere tree) {
		sequentialEmitter.visitWhere(tree);
	}

	public void visitFor(JCFor tree) {
		sequentialEmitter.visitFor(tree);
	}

	public void visitSelectCond(JCSelectCond tree) {
		sequentialEmitter.visitSelectCond(tree);
	}

	public void visitSelectExp(JCSelect tree) {
		sequentialEmitter.visitSelectExp(tree);
	}

	public void visitCaseExp(JCCaseExp tree) {
		sequentialEmitter.visitCaseExp(tree);
	}

	//handle [x'new=x] unique arguments!
	public void visitArgExpression(JCArgExpression tree) {
		sequentialEmitter.visitArgExpression(tree);
	}

	public void visitAssignop(JCAssignOp tree) {
		sequentialEmitter.visitAssignop(tree);
	}

	public void visitUnary(JCUnary tree) {
		sequentialEmitter.visitUnary(tree);
	}

	//a::b::c
	public void visitSequence(JCSequence tree) {
		sequentialEmitter.visitSequence(tree);
	}

	public void visitBinary(JCBinary tree) {
		sequentialEmitter.visitBinary(tree);
	}

	public void visitSelect(JCFieldAccess tree) {
		sequentialEmitter.visitSelect(tree);
	}

	public void visitIdent(JCIdent tree) {
 		sequentialEmitter.visitIdent(tree);
	}

	public void visitLiteral(JCLiteral tree) {
		sequentialEmitter.visitLiteral(tree);
	}

	public void visitModifiers(JCModifiers tree) {
		sequentialEmitter.visitModifiers(tree);
	}

	public void visitAnnotation(JCAnnotation tree) {
		sequentialEmitter.visitAnnotation(tree);
	}

	//-------------- ERROR HANDLING ------------------------

	public void visitErroneous(JCErroneous tree) {
		try {
			print("(ERROR)");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void visitTree(JCTree tree) {
		try {
			print("(UNKNOWN: " + tree.getClass().getName() + ")");
			println();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}


	//-------------- ARRAY EMITTER ------------------------

	public void visitDomainDef(JCDomainDecl tree) {
            if (jc.supportOpenCL) {
                openCLArrayEmitter.visitDomainDef(tree);
            } else {
                arrayEmitter.visitDomainDef(tree);
            }
	}

	public void visitForLoop(JCForLoop tree) {
            if (jc.supportOpenCL) {
                openCLArrayEmitter.visitForLoop(tree);
            } else {
                arrayEmitter.visitForLoop(tree);
            }
	}

	public void visitDomIter(JCDomainIter tree) {
            if (jc.supportOpenCL) {
                openCLArrayEmitter.printClosureBegin(tree);
                openCLArrayEmitter.visitDomIter(tree);
                openCLArrayEmitter.printClosureMiddle(tree);
                arrayEmitter.visitDomIter(tree);
                openCLArrayEmitter.printClosureEnd();
            } else {
                arrayEmitter.visitDomIter(tree);
            }
	}

	public void visitNewArray(JCNewArray tree) {
            if (jc.supportOpenCL) {
                openCLArrayEmitter.visitNewArray(tree);
            } else {
                arrayEmitter.visitNewArray(tree);
            }
	}

	//can join several domains a~b~c
	public void visitJoin(JCJoinDomains tree) {
            if (jc.supportOpenCL) {
                openCLArrayEmitter.visitJoin(tree);
            } else {
                arrayEmitter.visitJoin(tree);
            }
	}

	//array[i1,..,in]
	public void visitIndexed(JCArrayAccess tree) {
	    if (jc.supportOpenCL) {
                openCLArrayEmitter.visitIndexed(tree);
            } else {
                arrayEmitter.visitIndexed(tree);
            }
        }

	//-------------- TYPE EMITTER ------------------------

	/**
	 * If type parameter list is non-empty, print it enclosed in "<...>" brackets.
	 */
	public void printTypeParameters(List<JCTypeParameter> trees) throws IOException {
		typeEmitter.printTypeParameters(trees);
	}

	public void printCPPTypeParams(List<JCTypeParameter> typarams, List<Type> typarams_class) {
		typeEmitter.printCPPTypeParams(typarams, typarams_class);
	}

	public void printCPPTemplateParams(List<Type> typarams_class) {
		typeEmitter.printCPPTemplateParams(typarams_class);
	}

	public void visitTypeCast(JCTypeCast tree) {
		typeEmitter.visitTypeCast(tree);
	}

	public void visitTypeTest(JCInstanceOf tree) {
		typeEmitter.visitTypeTest(tree);
	}

	public void visitTypeIdent(JCPrimitiveTypeTree tree) {
		typeEmitter.visitTypeIdent(tree);
	}

	public void visitTypeArray(JCArrayTypeTree tree) {
		typeEmitter.visitTypeArray(tree);
	}

	public void visitTypeApply(JCTypeApply tree) {
		typeEmitter.visitTypeApply(tree);
	}

	public void visitTypeParameter(JCTypeParameter tree) {
		typeEmitter.visitTypeParameter(tree);
	}
}
