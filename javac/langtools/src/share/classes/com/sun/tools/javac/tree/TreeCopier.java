/*
 * Copyright 2006-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.javac.tree;

import com.sun.source.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.TaskSet;
import java.util.Map;

/**
 * Creates a copy of a tree, using a given TreeMaker.
 * Names, literal values, etc are shared with the original.
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class TreeCopier<P> implements TreeVisitor<JCTree,P> {
    private TreeMaker M;

    /** Creates a new instance of TreeCopier */
    public TreeCopier(TreeMaker M) {
        this.M = M;
    }

    public <T extends JCTree> T copy(T tree) {
        T n = copy(tree, null);

		//copy rest of the data in JCTree
		if(n!=null)
			n.copyNonAst(tree);

		return n;
    }

    @SuppressWarnings("unchecked")
    public <T extends JCTree> T copy(T tree, P p) {
        if (tree == null)
            return null;
        T n = (T) (tree.accept(this, p));

		n.copyNonAst(tree);

		return n;
    }

    public <T extends JCTree> List<T> copy(List<T> trees) {
        return copy(trees, null);
    }

    public <T extends JCTree> List<T> copy(List<T> trees, P p) {
        if (trees == null)
            return null;
        ListBuffer<T> lb = new ListBuffer<T>();
        for (T tree: trees)
            lb.append(copy(tree, p));
        return lb.toList();
    }

    public JCTree visitAnnotation(AnnotationTree node, P p) {
        JCAnnotation t = (JCAnnotation) node;
        JCTree annotationType = copy(t.annotationType, p);
        List<JCExpression> args = copy(t.args, p);
        return M.at(t.pos).Annotation(annotationType, args);
    }

    public JCTree visitAssert(AssertTree node, P p) {
        JCAssert t = (JCAssert) node;
        JCExpression cond = copy(t.cond, p);
        JCExpression detail = copy(t.detail, p);
        return M.at(t.pos).Assert(cond, detail);
    }

	public JCTree visitPragma(PragmaTree node, P p) {
        JCPragma t = (JCPragma) node;
        JCExpression cond = copy(t.cond, p);
        JCExpression detail = copy(t.detail, p);
        return M.at(t.pos).Pragma(t.flag,cond, detail);
    }

    public JCTree visitAssignment(AssignmentTree node, P p) {
        JCAssign t = (JCAssign) node;
        JCExpression lhs = copy(t.lhs, p);
        JCExpression rhs = copy(t.rhs, p);
		JCExpression cond = copy(t.cond, p);
        return M.at(t.pos).Assign(lhs, rhs,t.aflags, cond);
    }

    public JCTree visitWhere(WhereTree node, P p) {
        JCWhere t = (JCWhere) node;
        JCExpression exp = copy(t.exp, p);
        JCStatement body = copy(t.body,p);
        JCExpression sexp = copy(t.sexp, p);
        JCWhere res= M.at(t.pos).Where(exp, body,sexp);
		return res;
    }

    public JCTree visitFor(ForTree node, P p) {
        JCFor t = (JCFor) node;
        JCExpression exp = copy(t.exp, p);
        List<JCTree> content = copy(t.content, p);
        return M.at(t.pos).StaticFor(t.name ,exp, content);
    }

    public JCTree visitSelectExp(SelectTree node, P p) {
        JCSelect t = (JCSelect) node;
        List<JCSelectCond> l = copy(t.list, p);
        return M.at(t.pos).Select(l);
    }

    public JCTree visitCaseExp(CaseExpTree node, P p) {
        JCCaseExp t = (JCCaseExp) node;
        JCExpression exp = copy(t.exp, p);
        List<JCSelectCond> l = copy(t.list, p);
        return M.at(t.pos).CaseExp(exp,l);
    }

    public JCTree visitSelectCond(SelectCondTree node, P p) {
        JCSelectCond t = (JCSelectCond) node;
        JCExpression cond = copy(t.cond, p);
        JCExpression res = copy(t.res, p);
        JCStatement stmnt = copy(t.stmnt, p);
        return M.at(t.pos).SelectCond(cond,res,stmnt);
    }

    public JCTree visitCompoundAssignment(CompoundAssignmentTree node, P p) {
        JCAssignOp t = (JCAssignOp) node;
        JCTree lhs = copy(t.lhs, p);
        JCTree rhs = copy(t.rhs, p);
        return M.at(t.pos).Assignop(t.getTag(), lhs, rhs);
    }

    public JCTree visitBinary(BinaryTree node, P p) {
        JCBinary t = (JCBinary) node;
        JCExpression lhs = copy(t.lhs, p);
        JCExpression rhs = copy(t.rhs, p);
        JCBinary res = M.at(t.pos).Binary(t.getTag(), lhs, rhs);
		res.apply = copy(t.apply, p);
		return res;
    }

    public JCTree visitSequence(SequenceTree node, P p) {
        JCSequence t = (JCSequence) node;
        List<JCExpression> seq = copy(t.seq, p);
        return M.at(t.pos).Sequence(seq);
    }

	public JCTree visitJoin(JoinTree node, P p) {
        JCJoinDomains t = (JCJoinDomains) node;
        List<JCExpression> doms = copy(t.doms, p);
        return M.at(t.pos).Join(doms);
    }

    public JCTree visitBlock(BlockTree node, P p) {
        JCBlock t = (JCBlock) node;
        List<JCStatement> stats = copy(t.stats, p);
        return M.at(t.pos).Block(t.flags, stats);
    }

    public JCTree visitBreak(BreakTree node, P p) {
        JCBreak t = (JCBreak) node;
        return M.at(t.pos).Break(t.label);
    }

    public JCTree visitCase(CaseTree node, P p) {
        JCCase t = (JCCase) node;
        JCExpression pat = copy(t.pat, p);
        List<JCStatement> stats = copy(t.stats, p);
        return M.at(t.pos).Case(pat, stats);
    }

    public JCTree visitCatch(CatchTree node, P p) {
        JCCatch t = (JCCatch) node;
        JCVariableDecl param = copy(t.param, p);
        JCBlock body = copy(t.body, p);
        return M.at(t.pos).Catch(param, body);
    }

    public JCTree visitClass(ClassTree node, P p) {
        JCClassDecl t = (JCClassDecl) node;
        JCModifiers mods = copy(t.mods, p);
        List<JCTypeParameter> typarams = copy(t.typarams, p);
        JCTree extending = copy(t.extending, p);
        List<JCExpression> implementing = copy(t.implementing, p);
        List<JCTree> defs = copy(t.defs, p);
        JCClassDecl n = M.at(t.pos).ClassDef(mods, t.name, typarams, extending, implementing, defs);
		n.singular = t.singular;
		n.sym = t.sym;
		return n;
    }

//ALEX

    public JCTree visitDomain(DomainTree node, P p) {
        JCDomainDecl t = (JCDomainDecl) node;
        //JCModifiers mods = copy(t.mods, p);
        List<JCDomParameter> domparams = copy(t.domparams, p);
        List<JCDomParameter> domargs = copy(t.domargs, p);
        //JCTree extending = copy(t.extending, p);
        //List<JCExpression> implementing = copy(t.implementing, p);
		JCDomUsage domdef= copy(t.domdef,p);
        List<JCDomParameter> defs = copy(t.defs, p);
        List<JCTree> constraints = copy(t.constraints, p);
		JCDomParent parent= copy(t.parent,p);
        JCDomainDecl n = M.at(t.pos).DomDef(t.name, domparams, domargs, defs,domdef, constraints,parent);
		n.sym = t.sym;
		return n;
    }

    public JCTree visitDomIter(DomIterTree node, P p) {
        JCDomainIter t = (JCDomainIter) node;
        //JCModifiers mods = copy(t.mods, p);
        List<JCVariableDecl> domargs = copy(t.domargs, p);
        JCExpression exp = copy(t.exp, p);
        JCExpression body = copy(t.body, p);
        JCStatement sbody = copy(t.sbody, p);
        //JCTree extending = copy(t.extending, p);
        //List<JCExpression> implementing = copy(t.implementing, p);
		List<JCExpression> params = copy(t.params, p);
        JCDomainIter n = M.at(t.pos).DomIter(exp,t.name, domargs,body,sbody,params);
		n.sym = t.sym;
		n.mayReturn = t.mayReturn;
		n.valueAccess = t.valueAccess;
		n.valueOffsetAccess = t.valueOffsetAccess;
		n.iterType = t.iterType;

		return n;
    }

    public JCTree visitArgExpression(ArgExpressionTree node, P p) {
        JCArgExpression t = (JCArgExpression) node;
        JCExpression exp1 = copy(t.exp1,p);
        JCExpression exp2 = copy(t.exp2,p);
        //JCModifiers mods = copy(t.mods, p);
        return M.at(t.pos).ArgExpression(exp1,exp2);
    }

    public JCTree visitDomConstraint(DomConstraintTree node, P p) {
        JCDomConstraint t = (JCDomConstraint) node;
        int assign = t.assign;
        List<JCDomConstraintValue> right = t.right;
        List<JCDomConstraintValue> left = t.left;
        //JCModifiers mods = copy(t.mods, p);
        return M.at(t.pos).ConstrainDef(left, assign, right);
    }

    public JCTree visitDomConstraintValue(DomConstraintValueTree node, P p) {
        JCDomConstraintValue t = (JCDomConstraintValue) node;
        JCExpression coeff=copy(t.coeff,p);
        JCDomParameter parameter=copy(t.parameter,p);
        JCDomParameter parameter1=copy(t.parameter1,p);
        //JCModifiers mods = copy(t.mods, p);
		if(parameter1!=null)
			return M.at(t.pos).ConstraintValue(t.sign,parameter1,parameter);
		else
			return M.at(t.pos).ConstraintValue(t.sign,coeff,parameter);
    }

    public JCTree visitDomParameter(DomParameterTree node, P p) {
        JCDomParameter t = (JCDomParameter) node;
        return M.at(t.pos).DomParameter(t.name);
    }

    public JCTree visitCTProperty(CTPropertyTree node, P p) {
        JCCTProperty t = (JCCTProperty) node;
        JCExpression exp = copy(t.exp);
        return M.at(t.pos).CTP(exp,t.name);
    }

    public JCTree visitDomInstance(DomInstanceTree node, P p) {
        JCDomInstance t = (JCDomInstance) node;
		List<JCExpression> domparams=copy(t.domparams,p);
        return M.at(t.pos).DomInstance(t.name,domparams);
    }

    public JCTree visitDomParent(DomParentTree node, P p) {
        JCDomParent t = (JCDomParent) node;
		List<JCExpression> domparams=copy(t.domparams,p);
		List<JCDomParameter> domargs	=copy(t.domargs,p);
        return M.at(t.pos).DomParent(t.name,domparams,domargs);
    }

	public JCTree visitDomUsage(DomUsageTree node, P p) {
        JCDomUsage t = (JCDomUsage) node;
		List<JCDomParameter> domparams=copy(t.domparams,p);
        return M.at(t.pos).DomUsage(t.name,domparams);
    }


//END

    public JCTree visitConditionalExpression(ConditionalExpressionTree node, P p) {
        JCConditional t = (JCConditional) node;
        JCExpression cond = copy(t.cond, p);
        JCExpression truepart = copy(t.truepart, p);
        JCExpression falsepart = copy(t.falsepart, p);
        return M.at(t.pos).Conditional(cond, truepart, falsepart);
    }

    public JCTree visitContinue(ContinueTree node, P p) {
        JCContinue t = (JCContinue) node;
        return M.at(t.pos).Continue(t.label);
    }

    public JCTree visitDoWhileLoop(DoWhileLoopTree node, P p) {
        JCDoWhileLoop t = (JCDoWhileLoop) node;
        JCStatement body = copy(t.body, p);
        JCExpression cond = copy(t.cond, p);
        return M.at(t.pos).DoLoop(body, cond);
    }

    public JCTree visitErroneous(ErroneousTree node, P p) {
        JCErroneous t = (JCErroneous) node;
        List<? extends JCTree> errs = copy(t.errs, p);
        return M.at(t.pos).Erroneous(errs);
    }

    public JCTree visitExpressionStatement(ExpressionStatementTree node, P p) {
        JCExpressionStatement t = (JCExpressionStatement) node;
        JCExpression expr = copy(t.expr, p);
        return M.at(t.pos).Exec(expr);
    }

    public JCTree visitEnhancedForLoop(EnhancedForLoopTree node, P p) {
        JCEnhancedForLoop t = (JCEnhancedForLoop) node;
        JCVariableDecl var = copy(t.var, p);
        JCExpression expr = copy(t.expr, p);
        JCStatement body = copy(t.body, p);
        return M.at(t.pos).ForeachLoop(var, expr, body);
    }

    public JCTree visitForLoop(ForLoopTree node, P p) {
        JCForLoop t = (JCForLoop) node;
        List<JCStatement> init = copy(t.init, p);
        JCExpression cond = copy(t.cond, p);
        List<JCExpressionStatement> step = copy(t.step, p);
        JCStatement body = copy(t.body, p);
        return M.at(t.pos).ForLoop(init, cond, step, body);
    }

    public JCTree visitIdentifier(IdentifierTree node, P p) {
        JCIdent t = (JCIdent) node;
        JCIdent n = M.at(t.pos).Ident(t.name);
		n.sym = t.sym;
		return n;
	}

	public JCTree visitSizeOf(SizeOfTree node, P p) {
        JCSizeOf t = (JCSizeOf) node;
		JCExpression e=copy(t.expr, p);
        JCSizeOf n = M.at(t.pos).SizeOf(e);
		return n;
	}

    public JCTree visitIf(IfTree node, P p) {
        JCIf t = (JCIf) node;
        JCExpression cond = copy(t.cond, p);
        JCStatement thenpart = copy(t.thenpart, p);
        JCStatement elsepart = copy(t.elsepart, p);
        JCIf res= M.at(t.pos).If(cond, thenpart, elsepart);
		return res;
	}
/*
    public JCTree visitApply(MethodInvocationTree node, P p) {
        JCMethodInvocation t = (JCMethodInvocation) node;
		List<JCExpression> typeargs =copy(t.typeargs,p);
		JCExpression fn=copy(t.meth,p);
		List<JCExpression> args = copy(t.args,p);

        JCMethodInvocation res= M.at(t.pos).Apply(typeargs, fn, args);
		return res;
	}
*/
    public JCTree visitCF(EmptyStatementTree node, P p) {
        JCCF t = (JCCF) node;
        JCTree cond = copy(t.condition, p);
		//JCSkip exit = (JCSkip)copy(t.exit,p);
        JCCF res = M.at(t.pos).NewCF(cond,t.value,t.pos);
		res.additionalRefs = t.additionalRefs; //clone??
		res.exit = t.exit;
		return res;
    }

    public JCTree visitIfExp(IfExpTree node, P p) {
        JCIfExp t = (JCIfExp) node;
        JCExpression cond = copy(t.cond, p);
        JCExpression thenpart = copy(t.thenpart, p);
        JCExpression elsepart = copy(t.elsepart, p);
        return M.at(t.pos).IfExp(cond, thenpart, elsepart);
    }

    public JCTree visitImport(ImportTree node, P p) {
        JCImport t = (JCImport) node;
        JCTree qualid = copy(t.qualid, p);
        return M.at(t.pos).Import(qualid, t.staticImport);
    }

    public JCTree visitArrayAccess(ArrayAccessTree node, P p) {
        JCArrayAccess t = (JCArrayAccess) node;
        JCExpression indexed = copy(t.indexed, p);
        List<JCExpression> index = copy(t.index, p);
        List<JCExpression> params = copy(t.params, p);
        return M.at(t.pos).Indexed(indexed, index,params);
    }

    public JCTree visitLabeledStatement(LabeledStatementTree node, P p) {
        JCLabeledStatement t = (JCLabeledStatement) node;
        JCStatement body = copy(t.body, p);
        return M.at(t.pos).Labelled(t.label, t.body);
    }

    public JCTree visitLiteral(LiteralTree node, P p) {
        JCLiteral t = (JCLiteral) node;
        return M.at(t.pos).Literal(t.typetag, t.value);
    }

    public JCTree visitMethod(MethodTree node, P p) {
        JCMethodDecl t  = (JCMethodDecl) node;
        JCModifiers mods = copy(t.mods, p);
        JCExpression restype = copy(t.restype, p);
        List<JCTypeParameter> typarams = copy(t.typarams, p);
        List<JCVariableDecl> params = copy(t.params, p);
        List<JCExpression> thrown = copy(t.thrown, p);
        JCBlock body = copy(t.body, p);
        JCExpression defaultValue = copy(t.defaultValue, p);
        JCMethodDecl n = M.at(t.pos).MethodDef(mods, t.name, restype, typarams, params, thrown, body, defaultValue);
		n.sym = t.sym;

		//FIXME: copy more??
		//n.implicitSyms = t.implicitSyms;

		return n;
    }

    public JCTree visitMethodInvocation(MethodInvocationTree node, P p) {
        JCMethodInvocation t = (JCMethodInvocation) node;
        List<JCExpression> typeargs = copy(t.typeargs, p);
        JCExpression meth = copy(t.meth, p);
        List<JCExpression> args = copy(t.args, p);
        JCMethodInvocation n= M.at(t.pos).Apply(typeargs, meth, args);
		n.trigger=t.trigger;
		return n;
    }

    public JCTree visitModifiers(ModifiersTree node, P p) {
        JCModifiers t = (JCModifiers) node;
        List<JCAnnotation> annotations = copy(t.annotations, p);
		JCExpression group= copy(t.group,p);
		JCExpression thread= copy(t.thread,p);
		JCExpression work= copy(t.work,p);
		JCExpression task= copy(t.task,p);
		JCExpression mem= copy(t.mem,p);
        return M.at(t.pos).Modifiers(t.flags, annotations,group,thread, work, task, mem);
    }

    public JCTree visitNewArray(NewArrayTree node, P p) {
        JCNewArray t = (JCNewArray) node;
        JCExpression elemtype = copy(t.elemtype, p);
        List<JCExpression> dims = copy(t.dims, p);
        List<JCExpression> elems = copy(t.elems, p);
		JCDomInstance dom = copy(t.dom,p);
        return M.at(t.pos).NewArray(elemtype,dom, dims, elems);
    }

    public JCTree visitNewClass(NewClassTree node, P p) {
        JCNewClass t = (JCNewClass) node;
        JCExpression encl = copy(t.encl, p);
        List<JCExpression> typeargs = copy(t.typeargs, p);
        JCExpression clazz = copy(t.clazz, p);
        List<JCExpression> args = copy(t.args, p);
        JCClassDecl def = copy(t.def, p);
        return M.at(t.pos).NewClass(encl, typeargs, clazz, args, def);
    }

    public JCTree visitParenthesized(ParenthesizedTree node, P p) {
        JCParens t = (JCParens) node;
        JCExpression expr = copy(t.expr, p);
        return M.at(t.pos).Parens(expr);
    }

    public JCTree visitReturn(ReturnTree node, P p) {
        JCReturn t = (JCReturn) node;
        JCExpression expr = copy(t.expr, p);
        return M.at(t.pos).Return(expr,t.flags);
    }

    public JCTree visitMemberSelect(MemberSelectTree node, P p) {
        JCFieldAccess t = (JCFieldAccess) node;
        JCExpression selected = copy(t.selected, p);
        JCExpression selector = copy(t.name, p);
        List<JCExpression> params = copy(t.params, p);
        JCFieldAccess n = M.at(t.pos).Select(selected, selector,params);
		n.sym = t.sym;
		return n;
    }

    public JCTree visitEmptyStatement(EmptyStatementTree node, P p) {
        JCSkip t = (JCSkip) node;
        return M.at(t.pos).Skip();
    }

    public JCTree visitSet(SetTree node, P p) {
        //FIXME: copy set
        JCSet t = (JCSet) node;
        //return M.at(t.pos).Set();
        return t;
    }

    public JCTree visitSwitch(SwitchTree node, P p) {
        JCSwitch t = (JCSwitch) node;
        JCExpression selector = copy(t.selector, p);
        List<JCCase> cases = copy(t.cases, p);
        return M.at(t.pos).Switch(selector, cases);
    }

    public JCTree visitSynchronized(SynchronizedTree node, P p) {
        JCSynchronized t = (JCSynchronized) node;
        JCExpression lock = copy(t.lock, p);
        JCBlock body = copy(t.body, p);
        return M.at(t.pos).Synchronized(lock, body);
    }

    public JCTree visitThrow(ThrowTree node, P p) {
        JCThrow t = (JCThrow) node;
        JCTree expr = copy(t.expr, p);
        return M.at(t.pos).Throw(expr);
    }

    public JCTree visitCompilationUnit(CompilationUnitTree node, P p) {
        JCCompilationUnit t = (JCCompilationUnit) node;
        List<JCAnnotation> packageAnnotations = copy(t.packageAnnotations, p);
        JCExpression pid = copy(t.pid, p);
        List<JCTree> defs = copy(t.defs, p);
        JCCompilationUnit out = (JCCompilationUnit)t.clone();//M.at(t.pos).TopLevel(packageAnnotations, pid, defs);

		out.packageAnnotations = packageAnnotations;
		out.pid = pid;
		out.defs = defs;
		out.pos = t.pos;

		return out;
    }

    public JCTree visitTry(TryTree node, P p) {
        JCTry t = (JCTry) node;
        JCBlock body = copy(t.body, p);
        List<JCCatch> catchers = copy(t.catchers, p);
        JCBlock finalizer = copy(t.finalizer, p);
        return M.at(t.pos).Try(body, catchers, finalizer);
    }

    public JCTree visitParameterizedType(ParameterizedTypeTree node, P p) {
        JCTypeApply t = (JCTypeApply) node;
        JCExpression clazz = copy(t.clazz, p);
        List<JCExpression> arguments = copy(t.arguments, p);
        return M.at(t.pos).TypeApply(clazz, arguments);
    }

    public JCTree visitArrayType(ArrayTypeTree node, P p) {
        JCArrayTypeTree t = (JCArrayTypeTree) node;
        JCExpression elemtype = copy(t.elemtype, p);
		JCDomInstance dom = copy(t.dom, p);
        return M.at(t.pos).TypeArray(elemtype,dom,t.option);
    }

    public JCTree visitTypeCast(TypeCastTree node, P p) {
        JCTypeCast t = (JCTypeCast) node;
        JCTree clazz = copy(t.clazz, p);
        JCExpression expr = copy(t.expr, p);
        return M.at(t.pos).TypeCast(clazz, expr);
    }

    public JCTree visitPrimitiveType(PrimitiveTypeTree node, P p) {
        JCPrimitiveTypeTree t = (JCPrimitiveTypeTree) node;
        return M.at(t.pos).TypeIdent(t.typetag);
    }

    public JCTree visitTypeParameter(TypeParameterTree node, P p) {
        JCTypeParameter t = (JCTypeParameter) node;
        List<JCExpression> bounds = copy(t.bounds, p);
        return M.at(t.pos).TypeParameter(t.name, bounds);
    }

    public JCTree visitInstanceOf(InstanceOfTree node, P p) {
        JCInstanceOf t = (JCInstanceOf) node;
        JCExpression expr = copy(t.expr, p);
        JCTree clazz = copy(t.clazz, p);
        return M.at(t.pos).TypeTest(expr, clazz);
    }

    public JCTree visitUnary(UnaryTree node, P p) {
        JCUnary t = (JCUnary) node;
        JCExpression arg = copy(t.arg, p);
        JCUnary res = M.at(t.pos).Unary(t.getTag(), arg);
		res.apply = copy(t.apply, p);
		return res;
    }

    public JCTree visitVariable(VariableTree node, P p) {
        JCVariableDecl t = (JCVariableDecl) node;
        JCModifiers mods = copy(t.mods, p);
        JCExpression vartype = copy(t.vartype, p);
        JCExpression init = copy(t.init, p);
        JCVariableDecl n = M.at(t.pos).VarDef(mods, t.name, vartype, init);
		n.sym = t.sym;
		return n;
    }

    public JCTree visitWhileLoop(WhileLoopTree node, P p) {
        JCWhileLoop t = (JCWhileLoop) node;
        JCStatement body = copy(t.body, p);
        JCExpression cond = copy(t.cond, p);
        return M.at(t.pos).WhileLoop(cond, body);
    }

    public JCTree visitWildcard(WildcardTree node, P p) {
        JCWildcard t = (JCWildcard) node;
        TypeBoundKind kind = M.at(t.kind.pos).TypeBoundKind(t.kind.kind);
        JCTree inner = copy(t.inner, p);
        return M.at(t.pos).Wildcard(kind, inner);
    }

    public JCTree visitOther(Tree node, P p) {
        JCTree tree = (JCTree) node;
        switch (tree.getTag()) {
            case JCTree.LETEXPR: {
                LetExpr t = (LetExpr) node;
                List<JCVariableDecl> defs = copy(t.defs, p);
                JCTree expr = copy(t.expr, p);
                return M.at(t.pos).LetExpr(defs, expr);
            }
            default:
                throw new AssertionError("unknown tree tag: " + tree.getTag());
        }
    }

}
