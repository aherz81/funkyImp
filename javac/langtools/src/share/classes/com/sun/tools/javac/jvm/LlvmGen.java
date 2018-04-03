package com.sun.tools.javac.jvm;

import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.tree.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.jvm.LlvmCode.*;
import com.sun.tools.javac.jvm.Items.*;
import com.sun.tools.javac.tree.JCTree.*;
import java.util.Set;


public class LlvmGen extends JCTree.Visitor implements CodeGen {
    protected static final Context.Key<LlvmGen> genKey =
        new Context.Key<LlvmGen>();

    private LlvmCode code;

    public static LlvmGen instance(Context context) {
        LlvmGen instance = context.get(genKey);
        if (instance == null)
            //instance = new LlvmGen(context);
            instance = new LlvmGen();
        return instance;
    }

    public boolean genClass(Env<AttrContext> env, JCClassDecl cdef) {
        for (List<JCTree> l = cdef.defs; l.nonEmpty(); l = l.tail) {
            genDef(l.head);
        }
        return true;
    }

    private void genDef(JCTree tree) {
        tree.accept(this);
    }

    public void genStats(List<? extends JCTree> trees) {
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail)
            genDef(l.head);
    }

    @Override
    public void visitVarDef(JCVariableDecl that) {

        String name = that.name.toString();
        String type = LlvmCode.getTypeFor(that.type.tsym.kind);
        VarSymbol var = that.sym;
        boolean isLocal = var.isLocal();


        String assignOp = "";
        String op1 = "";
        String op2 = "";
        if (that.init instanceof JCBinary) {
            JCBinary binop = (JCBinary) that.init;
            assignOp = binop.operator.name.toString();
            op1 = binop.lhs.toString();
            op2 = binop.rhs.toString();
            if (assignOp.equals("+")) {
                code.genAddAssignment(name, type, isLocal, op1, op2);
            }
        } else if (that.init instanceof JCLiteral) {
            JCLiteral lit = (JCLiteral) that.init;
            String value = lit.value.toString();
            code.genConstantAssignment(name, type, isLocal, value);
        }
    }

    @Override
    public void visitMethodDef(JCMethodDecl that) {
        code = new LlvmCode();
        that.sym.llvmCode = code;
        String name = that.name.toString();

        //special handling for constructors - funcation names are not allowed to contain <>
        if (name.equals("<init>")) {
            name = "$init";
        }
        code.genMethod(name, (that.restype != null) ? LlvmCode.getTypeFor(that.restype.type.tsym.kind):"void");
        Set<iTask> paths = that.getSchedule();
        if (paths.isEmpty())//shouldn't happen
        {
            genDef(that.body);
        } else {
            for (iTask p : paths) {
                genDef(p.getPathBlock());
            }
        }
    }

    @Override
    public void visitBlock(JCBlock that) {
        genStats(that.stats);
        code.genEndBlock();
    }

    @Override
    public void visitReturn(JCReturn that) {
        if (that.getExpression() != null && that.getExpression() instanceof JCIdent) {
            JCIdent id = (JCIdent) that.getExpression();
            code.genReturnIdent(LlvmCode.getTypeFor(id.type.tsym.kind), id.name.toString());
        }
    }
}
