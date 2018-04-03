/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.kernel;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.cl.util.PrettyKernelStringBuilder;
import com.sun.tools.javac.util.Convert;
import java.util.Set;
import javax.lang.model.element.Modifier;

/**
 *
 * @author Alexander Pöppl
 */
public class OpenCLCodeEmitter extends TreeScanner {

    /**
     * State object for the OpenCL part.
     */
    OpenCLCodeEmitterState clState;
    
    /**
     * Parent state of the compilation. Coming from the non-OpenCL part.
     */
    private final LowerTreeImpl state;
    
    
    private final PrettyKernelStringBuilder printer;
    private final VariableEnvironment environment;

    public OpenCLCodeEmitter(LowerTreeImpl state, PrettyKernelStringBuilder printer, VariableEnvironment environment) {
        this.state = state;
        this.printer = printer;
        this.environment = environment;
        this.clState = new OpenCLCodeEmitterState();
    }

    @Override
    public void scan(JCTree tree) {
        super.scan(tree);
    }

    @Override
    public void visitTopLevel(JCTree.JCCompilationUnit tree) {
        assert false;
        scan(tree.packageAnnotations);
        scan(tree.pid);
        scan(tree.defs);
    }

    @Override
    public void visitImport(JCTree.JCImport tree) {
        assert false;
        scan(tree.qualid);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl tree) {
        assert false;
        scan(tree.mods);
        scan(tree.typarams);
        scan(tree.extending);
        scan(tree.implementing);
        scan(tree.defs);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void visitDomainDef(JCTree.JCDomainDecl tree) {
        assert false;
        scan(tree.constraints);
        scan(tree.defs);
        scan(tree.domargs);
        scan(tree.domparams);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void visitDomainIter(JCTree.JCDomainIter tree) {
        assert false;
        scan(tree.exp);
        scan(tree.domargs);
        if (tree.body != null) {
            scan(tree.body);
        }
        if (tree.sbody != null) {
            scan(tree.sbody);
        }
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree) {
        this.clState.position.push(OpenCLCodeEmitterState.CurrentPosition.INSIDE_FUNCTION_HEADER);
        this.clState.currentFunction = tree;
        printMethodHeader(tree);
        this.clState.position.pop();
        scan(tree.thrown);
        scan(tree.defaultValue);
        this.clState.position.push(OpenCLCodeEmitterState.CurrentPosition.INSIDE_FUNCTION);
        scan(tree.body);
        printer.appendNl();

        this.clState.currentFunction = null;
        this.clState.position.pop();
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl tree) {
        this.clState.position.push(OpenCLCodeEmitterState.CurrentPosition.INSIDE_VAR_DECL);
        this.clState.varDecl = tree;
        scan(tree.mods);
        scan(tree.vartype);
        printer.append(" __").append(tree.name.toString());
        if (tree.init != null) {
            printer.append("=");
            scan(tree.init);
        }
        this.clState.varDecl = null;
        this.clState.position.pop();
        if (!(this.clState.position.peek() == OpenCLCodeEmitterState.CurrentPosition.INSIDE_FUNCTION_HEADER)) {
            printer.append(";").appendNl();
        }

    }

    @Override
    public void visitSkip(JCTree.JCSkip tree) {
        assert false;
        throw new UnsupportedOperationException("Not implemented yet.");

    }

    @Override
    public void visitBlock(JCTree.JCBlock tree) {
        printer.appendNl().append("{");
        printer.indent();
        printer.appendNl();
        scan(tree.stats);
        printer.undent();
        printer.appendNl();
        printer.append("}");
        printer.appendNl();
    }

    @Override
    public void visitDoLoop(JCTree.JCDoWhileLoop tree) {
        assert false;
        scan(tree.body);
        scan(tree.cond);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void visitWhileLoop(JCTree.JCWhileLoop tree) {
        assert false;
        scan(tree.cond);
        scan(tree.body);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitForLoop(JCTree.JCForLoop tree) {
        assert false;
        scan(tree.init);
        scan(tree.cond);
        scan(tree.step);
        scan(tree.body);
        throw new UnsupportedOperationException("Not implemented yet.");

    }

    public void visitForeachLoop(JCTree.JCEnhancedForLoop tree) {
        assert false;
        scan(tree.var);
        scan(tree.expr);
        scan(tree.body);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitLabelled(JCTree.JCLabeledStatement tree) {
        assert false;
        scan(tree.body);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitSwitch(JCTree.JCSwitch tree) {
        assert false;
        scan(tree.selector);
        scan(tree.cases);
        throw new UnsupportedOperationException("Not implemented yet.");

    }

    public void visitCase(JCTree.JCCase tree) {
        assert false;
        scan(tree.pat);
        scan(tree.stats);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitSynchronized(JCTree.JCSynchronized tree) {
        assert false;
        scan(tree.lock);
        scan(tree.body);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitTry(JCTree.JCTry tree) {
        assert false;
        scan(tree.body);
        scan(tree.catchers);
        scan(tree.finalizer);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitCatch(JCTree.JCCatch tree) {
        assert false;
        scan(tree.param);
        scan(tree.body);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitConditional(JCTree.JCConditional tree) {
        assert false;
        scan(tree.cond);
        scan(tree.truepart);
        scan(tree.falsepart);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitIf(JCTree.JCIf tree) {
        printer.append("if ");
        scan(tree.cond);
        printer.indent();
        printer.appendNl();
        scan(tree.thenpart);
        printer.undent();
        printer.appendNl().append("else");
        printer.indent();
        printer.appendNl();
        scan(tree.elsepart);
        printer.undent();
        printer.appendNl();
    }

    public void visitSet(JCTree.JCSet tree) {
        assert false;
        scan(tree.getContent());
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void visitExec(JCTree.JCExpressionStatement tree) {
        scan(tree.expr);
        printer.append(";").appendNl();
    }

    public void visitBreak(JCTree.JCBreak tree) {
        assert false;
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitContinue(JCTree.JCContinue tree) {
        assert false;
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitReturn(JCTree.JCReturn tree) {
        printer.append("return ");
        scan(tree.expr);
        printer.append(";");
    }

    public void visitWhere(JCTree.JCWhere tree) {
        scan(tree.exp);
        scan(tree.sexp);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitThrow(JCTree.JCThrow tree) {
        assert false;
        scan(tree.expr);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitAssert(JCTree.JCAssert tree) {
        assert false;
        scan(tree.cond);
        scan(tree.detail);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitApply(JCTree.JCMethodInvocation tree) {
        String originalMethodName = tree.meth.toString();
        String derivedMethodName = environment.getNameForMethod(originalMethodName);
        printer.append(derivedMethodName);
        printer.append("(");
        int remainingArguments = tree.args.size();
        for (JCTree.JCExpression argExp : tree.args) {
            scan(argExp);
            if (--remainingArguments > 0) {
                printer.append(", ");
            }
        }
        printer.append(")");
    }

    public void visitNewClass(JCTree.JCNewClass tree) {
        assert false;
        scan(tree.encl);
        scan(tree.clazz);
        scan(tree.args);
        scan(tree.def);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitNewArray(JCTree.JCNewArray tree) {
        assert false;
        scan(tree.elemtype);
        scan(tree.dims);
        scan(tree.elems);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitParens(JCTree.JCParens tree) {
        printer.append("(");
        scan(tree.expr);
        printer.append(")");
    }

    public void visitAssign(JCTree.JCAssign tree) {
        JCTree.JCIdent ident = (JCTree.JCIdent) tree.lhs;
        if (!tree.lhs.type.isPrimitive()) {
            throw new UnsupportedOperationException("Not implemented yet.");
        } else {
            printer.append(ident.type.toString());
            printer.append(" ");
            scan(ident);
            printer.append(" = ");
            scan(tree.rhs);
        }
    }

    public void visitAssignop(JCTree.JCAssignOp tree) {
        assert false;
        scan(tree.lhs);
        printer.append(" = ");
        scan(tree.rhs);
    }

    public void visitUnary(JCTree.JCUnary tree) {
        String opname = tree.operatorName(tree.getTag());
        printer.append(opname);
        printer.append("(");
        scan(tree.arg);
        printer.append(")");
    }

    public void visitBinary(JCTree.JCBinary tree) {
        int ownprec;
        if (tree instanceof JCTree.JCBinary) {
            ownprec = TreeInfo.opPrec(tree.getTag());
        } else {
            ownprec = -1;
        }
        int leftprec;
        if (tree.lhs instanceof JCTree.JCBinary) {
            leftprec = TreeInfo.opPrec(tree.lhs.getTag());
        } else if (tree.lhs instanceof JCTree.JCLiteral || tree.lhs instanceof JCTree.JCIdent) {
            leftprec = Integer.MAX_VALUE;
        } else {
            leftprec = -1;
        }
        int rightprec;
        if (tree.rhs instanceof JCTree.JCBinary) {
            rightprec = TreeInfo.opPrec(tree.rhs.getTag());
        } else if (tree.lhs instanceof JCTree.JCLiteral || tree.lhs instanceof JCTree.JCIdent) {
            rightprec = Integer.MAX_VALUE;
        } else {
            rightprec = -1;
        }
        String opname = tree.operatorName(tree.getTag());
        if (ownprec > leftprec) {
            printer.append("(");
        }
        scan(tree.lhs);
        if (ownprec > leftprec) {
            printer.append(")");
        }
        printer.append(opname);
        if (ownprec > rightprec + 1) {
            printer.append("(");
        }
        scan(tree.rhs);
        if (ownprec > rightprec + 1) {
            printer.append(")");
        }
    }

    public void visitTypeCast(JCTree.JCTypeCast tree) {
        printer.append("(");
        scan(tree.clazz);
        printer.append(")");
        scan(tree.expr);
    }

    public void visitTypeTest(JCTree.JCInstanceOf tree) {
        assert false;
        scan(tree.expr);
        scan(tree.clazz);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitIndexed(JCTree.JCArrayAccess tree) {
        ArrayAccessEmitter arrayAccessEmitter = new ArrayAccessEmitter(this, state, environment, printer, tree);
        String arrayAccess = arrayAccessEmitter.printArrayAccess();
    }

    public void visitSelect(JCTree.JCFieldAccess tree) {
        assert false;
        scan(tree.selected);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitIdent(JCTree.JCIdent tree) {
        VariableEnvironment.VarType varType = environment.variableType(tree.name.toString());
        String varName;
        if (this.clState.position.peek() == OpenCLCodeEmitterState.CurrentPosition.INSIDE_KERNEL
                && varType == VariableEnvironment.VarType.NORMAL_VARIABLE) {
            varName = environment.getNameForVariable(tree.name.toString());
            printer.append(varName);
        } else if (this.clState.position.peek() == OpenCLCodeEmitterState.CurrentPosition.INSIDE_KERNEL
                && state.index_map != null) {
            JCTree.JCExpression replacedExp;
            replacedExp = state.index_map.get((Symbol.VarSymbol) tree.sym);
            scan(replacedExp);
        } else if (this.clState.position.peek() == OpenCLCodeEmitterState.CurrentPosition.INSIDE_KERNEL
                && state.lastProjectionArgs != null) {
            JCTree.JCExpression replacedExp;
            replacedExp = state.lastProjectionArgs.get((Symbol.VarSymbol) tree.sym);
            scan(replacedExp);
        } else {
            if (state.index_map != null && state.index_map.containsKey((Symbol.VarSymbol) tree.sym)) {
                scan(state.index_map.get((Symbol.VarSymbol) tree.sym));
            } else {
                varName = environment.normalizeNameForVar(tree);
                printer.append(varName);
            }

        }
    }

    public void visitSizeOf(JCTree.JCSizeOf tree) {
        assert false;
        scan(tree.expr);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitLiteral(JCTree.JCLiteral tree) {
        String literal;
        switch (tree.typetag) {
            case TypeTags.INT:
                literal = tree.value.toString();
                break;
            case TypeTags.LONG:
                literal = tree.value + "L";
                break;
            case TypeTags.FLOAT:
                literal = tree.value + "F";
                break;
            case TypeTags.DOUBLE:
                literal = tree.value.toString();
                break;
            case TypeTags.CHAR:
                literal = "\'"
                        + Convert.quote(
                                String.valueOf((char) ((Number) tree.value).intValue()))
                        + "\'";
                break;
            case TypeTags.BOOLEAN:
                literal = (((Number) tree.value).intValue() == 1) ? "1" : "0";
                break;
            case TypeTags.BOT:
                literal = "NULL";
                break;
            default:
                literal = "\"" + Convert.quote(tree.value.toString()) + "\"";
                break;
        }
        printer.append(literal);
    }

    public void visitTypeIdent(JCTree.JCPrimitiveTypeTree tree) {
        String type;
        switch (tree.typetag) {
            case TypeTags.BYTE:
                type = "byte";
                break;
            case TypeTags.CHAR:
                type = "char";
                break;
            case TypeTags.GROUP:
                type = "group";
                break;
            case TypeTags.SHORT:
                type = "short";
                break;
            case TypeTags.INT:
                type = "int";
                break;
            case TypeTags.LONG:
                type = "long";
                break;
            case TypeTags.FLOAT:
                type = "float";
                break;
            case TypeTags.DOUBLE:
                type = "double";
                break;
            case TypeTags.BOOLEAN:
                type = "bool";
                break;
            case TypeTags.VOID:
                type = "void";
                break;
            default:
                type = "error";
                break;
        }
        printer.append(type).append(" ");
    }

    public void visitTypeArray(JCTree.JCArrayTypeTree tree) {
        if (clState.position.contains(OpenCLCodeEmitterState.CurrentPosition.INSIDE_FUNCTION_HEADER)) {
            printer.append("global ");
        }
        scan(tree.elemtype);
        printer.append("* ");
    }

    public void visitTypeApply(JCTree.JCTypeApply tree) {
        assert false;
        scan(tree.clazz);
        scan(tree.arguments);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitTypeParameter(JCTree.JCTypeParameter tree) {
        assert false;
        scan(tree.bounds);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void visitWildcard(JCTree.JCWildcard tree) {
        assert false;
        scan(tree.kind);
        if (tree.inner != null) {
            scan(tree.inner);
        }
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void visitTypeBoundKind(JCTree.TypeBoundKind that) {
        assert false;
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitModifiers(JCTree.JCModifiers tree) {
        if (this.clState.position.peek() == OpenCLCodeEmitterState.CurrentPosition.INSIDE_FUNCTION_HEADER) {
            Set<Modifier> modifiers = tree.getFlags();
            if (!modifiers.contains(Modifier.STATIC)) {
                throw new IllegalArgumentException("Only static methods are allowed at the moment.");
            }
            if (tree.annotations.size() > 0) {
                throw new IllegalArgumentException("No annotations supported yet.");
            }
        } else if (this.clState.position.peek() == OpenCLCodeEmitterState.CurrentPosition.INSIDE_VAR_DECL) {
            Set<Modifier> modifiers = tree.getFlags();
            //Do nothing with them for the moment.
        }
        assert false;
        scan(tree.annotations);
    }

    public void visitAnnotation(JCTree.JCAnnotation tree) {
        assert false;
        scan(tree.annotationType);
        scan(tree.args);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitErroneous(JCTree.JCErroneous tree) {
        assert false;
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitLetExpr(JCTree.LetExpr tree) {
        assert false;
        scan(tree.defs);
        scan(tree.expr);
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitArgExpression(JCTree.JCArgExpression tree) {
        assert false;
        scan(tree.exp1);

        if (tree.exp2 != null) {
            scan(tree.exp2);
        }
    }

    public void visitSequence(JCTree.JCSequence tree) {
        assert false;
        for (JCTree.JCExpression e : tree.seq) {
            scan(e);
        }
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitJoin(JCTree.JCJoinDomains tree) {
        assert false;
        for (JCTree.JCExpression e : tree.doms) {
            scan(e);
        }
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitDomIter(JCTree.JCDomainIter tree) {
        assert false;
        for (JCTree.JCVariableDecl e : tree.domargs) {
            scan(e);
        }

        scan(tree.exp);

        if (tree.body != null) {
            scan(tree.body);
        } else {
            scan(tree.sbody);
        }
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void visitTree(JCTree tree) {
        assert false;
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void printMethodHeader(JCTree.JCMethodDecl tree) {

        scan(tree.mods);
        scan(tree.restype);
        String originalFunctionName = tree.name.toString();
        String functionName = environment.getNameForMethod(originalFunctionName);
        printer.append(" ").append(functionName).append("(");

        scan(tree.typarams);
        int remainingParams = tree.params.size();
        for (JCTree.JCVariableDecl param : tree.params) {
            scan(param);
            if (--remainingParams > 0) {
                printer.append(", ");
            }
        }
        printer.append(")");
    }
}
