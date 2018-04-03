/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.kernel;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.cl.variables.ReturnValue;
import com.sun.tools.javac.tree.cl.variables.KernelVariable;
import com.sun.tools.javac.tree.cl.variables.PrimitiveVariable;
import com.sun.tools.javac.util.Pair;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;

/**
 * This class models Variable environments. If this code is used in the future, this part should be totally rewritten, or if not, at least refacored using optionals.
 * @author Alexander Pöppl
 */
public class VariableEnvironment {

    public enum VarType {

        NORMAL_VARIABLE, ITERATION_VARIABLE, RETURN_VARIABLE, INVALID_NAME
    }
    private final Map<String, Pair<String, JCTree.JCMethodDecl>> methods;
    private final Map<String, Pair<KernelVariable, JCTree.JCExpression>> variables;
    private final Pair<String, ReturnValue> returnValue;
    private final List<Pair<String, JCTree.JCVariableDecl>> domainArguments;
    private final LowerTreeImpl state;

    public VariableEnvironment(LowerTreeImpl state, Map<String, Pair<KernelVariable, JCTree.JCExpression>> variables, Pair<String, ReturnValue> returnValue, List<Pair<String, JCTree.JCVariableDecl>> domainArguments) {
        this.variables = variables;
        this.returnValue = returnValue;
        this.domainArguments = domainArguments;
        this.state = state;
        this.methods = new LinkedHashMap<String, Pair<String, JCTree.JCMethodDecl>>();
        
    }

    
    public String getNameForVariable(String originalName) {
        //Potentially throws NPE.
        return variables.get(originalName).fst.getGeneratedName();
    }

    public String getTypeForVariable(String originalName) {
        //Potentially throws NPE.
        return variables.get(originalName).fst.getGeneratedType();
    }

    public int getDimensionForDomainArgument(String domArg) {
        //Potentially throws NPE.
        return domainArguments.indexOf(domArg);
    }

    public VarType variableType(String varName) {
        if (varName.equals(returnValue.fst)) {
            return VarType.RETURN_VARIABLE;
        } else if (variables.keySet().contains(varName)) {
            return VarType.NORMAL_VARIABLE;
        } else if (domainArguments.contains(varName)) {
            return VarType.ITERATION_VARIABLE;
        } else {
            return VarType.INVALID_NAME;
        }
    }

    public void addDomainIterationVariable(String var) {
        Pair<String, JCTree.JCVariableDecl> domArgToBeAdded = null;
        for (Pair<String, JCTree.JCVariableDecl> domarg : this.domainArguments) {
            if (domarg.fst.equals(var)) {
                domArgToBeAdded = domarg;
            }
        }
        if (domArgToBeAdded == null) {
            throw new RuntimeException("The variable is not a domain Argument!");
        }
        PrimitiveVariable pVar = new PrimitiveVariable(domArgToBeAdded.snd, state);
        Pair<KernelVariable, JCTree.JCExpression> value = new Pair<KernelVariable, JCTree.JCExpression>(pVar, domArgToBeAdded.snd);
        this.variables.put(var, value);
    }

    public List<Pair<String, JCTree.JCVariableDecl>> getDomainArguments() {
        return domainArguments;
    }

    public String getNameForMethod(String originalName) {
        //Potentially throws NPE.
        return methods.get(originalName).fst;
    }
    
    public void addMethodName(JCTree.JCMethodDecl decl) {
        String originalMethodName = decl.name.toString();
        String derivedMethodName = getDerivedMethodName(decl);
        Pair<String, JCTree.JCMethodDecl> methodInfo = new Pair<String, JCTree.JCMethodDecl>(derivedMethodName, decl);
        methods.put(originalMethodName, methodInfo);
    }

    private String getDerivedMethodName(JCTree.JCMethodDecl decl) {
        StringBuilder sB = new StringBuilder();
        sB.append("__");
        if (decl.getModifiers().getFlags().contains(Modifier.STATIC)) {
            sB.append("s_");
        }
        JCTree returnType = decl.getReturnType();
        if (returnType instanceof JCTree.JCPrimitiveTypeTree) {
            sB.append(returnType.toString());
        } else if (returnType instanceof JCTree.JCArrayTypeTree) {
            JCTree.JCArrayTypeTree resArrayType = (JCTree.JCArrayTypeTree) returnType;
            sB.append(resArrayType.elemtype.toString()).append("_p");
        }
        sB.append("_").append(decl.name.toString());
        for (JCTree.JCVariableDecl param : decl.params) {
            String paramType;
            if (param.vartype instanceof JCTree.JCPrimitiveTypeTree) {
                paramType = param.vartype.toString();
            } else if (param.vartype instanceof JCTree.JCArrayTypeTree) {
                paramType = ((JCTree.JCArrayTypeTree) (param.vartype)).elemtype.toString() + "_p";
            } else {
                paramType = "void";
            }
            sB.append("_").append(paramType);
        }
        return sB.toString();
    }
    
    public String normalizeNameForVar(JCTree.JCIdent identifier) {
        return ("__" + (identifier.name.toString()).replace('\'', '_'));
    }
}
