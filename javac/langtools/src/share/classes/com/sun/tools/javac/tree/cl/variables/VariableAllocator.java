/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.variables;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.cl.util.PrettyStringBuilder;
import com.sun.tools.javac.util.Pair;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alexander Pöppl
 */
public class VariableAllocator {

    private final LowerTreeImpl state;
    private final PrettyStringBuilder printer;
    private Set<KernelVariable> variables;
    private Map<String, Pair<KernelVariable, JCTree.JCExpression>> variableMap;
    private List<Pair<String, JCTree.JCVariableDecl>> domainArgumentNames;
    private ReturnValue returnValue;

    public VariableAllocator(LowerTreeImpl state) {
        this.state = state;
        this.printer = new PrettyStringBuilder(state);
        this.variables = new LinkedHashSet<KernelVariable>();
        this.variableMap = new LinkedHashMap<String, Pair<KernelVariable, JCTree.JCExpression>>();
    }

    public String handleVariableAllocation(String lambdaReturnType) {
        this.printer.empty();
        this.handleVariables();
        this.handleReturnSpace(lambdaReturnType);
        return this.printer.toString();
    }

    private void handleVariables() {
        //Do we need to capture variables?
        JCTree.JCExpression iterationBody = state.domainIterState.iter.body;


        VariableCapturer varCap = new VariableCapturer(iterationBody);
        Map<String, JCTree.JCExpression> neccessaryVariables = varCap.getNeccesaryVariables();

        //Domain Arguments are not from outside, but more like a parameter for the domain iteration.
        getDomainArgumentNames();

        //We also need the domain that is being iterated on.
        neccessaryVariables.put(state.domainIterState.iter.exp.toString(), state.domainIterState.iter.exp);


        for (Pair<String, JCTree.JCVariableDecl> domArg : domainArgumentNames) {
            neccessaryVariables.remove(domArg.fst);
        }

        for (String identifier : neccessaryVariables.keySet()) {
            handleVarCode(neccessaryVariables.get(identifier));
        }
    }

    private void handleVarCode(JCTree.JCExpression identifier) {
        Type identifierType = identifier.type;
        KernelVariable var;

        if (identifier instanceof JCTree.JCNewArray) {
            var = new NewArrayVariable((JCTree.JCNewArray) identifier, state);
            variableMap.put("newArray", new Pair<KernelVariable, JCTree.JCExpression>(var, identifier));
        } else if (identifier instanceof JCTree.JCIdent) {
            if (identifier instanceof JCTree.JCIdent && identifierType instanceof Type.ArrayType) {
                var = new ArrayVariable((JCTree.JCIdent) identifier, state);
            } else if (identifierType instanceof Type.MethodType) {
                //TODO handle methods!
                return;
            } else if (identifierType instanceof Type && identifierType.isPrimitive()) {
                var = new PrimitiveVariable((JCTree.JCIdent) identifier, state);
            } else {
                //Are there any other cases?
                return;
            }
            variableMap.put(((JCTree.JCIdent) identifier).name.toString(), new Pair<KernelVariable, JCTree.JCExpression>(var, identifier));
        } else {
            return;
        }

        variables.add(var);
        List<String> decls = var.getDeclarations();
        for (String decl : decls) {
            printer.append(decl).appendNl();
        }
    }

    public String handleVariableCopying() {
        this.printer.empty();

        for (KernelVariable var : this.variables) {
            String copyCode = var.getGPUCopyCode();
            if (copyCode != null) {
                printer.append(copyCode).appendNl();
            }
        }

        return printer.toString();
    }

    public String handleParameterSetup(String kernelName) {
        this.printer.empty();
        List<String> parameters = new LinkedList<String>();

        for (KernelVariable var : variables) {
            parameters.addAll(var.getParams());
        }

        printer.append(kernelName).append(".setArgs(");
        for (String parameter : parameters) {
            if (parameter != null) {
                printer.append(parameter).append(",");
            }
        }
        printer.append(this.returnValue.getParam()).append(");");
        return printer.toString();
    }

    public String handleReturnValueRetrieval(String lambdaReturnType) {
        return this.returnValue.handleReturnValueRetrieval(lambdaReturnType);
    }

    private void handleReturnSpace(String lambdaReturnType) {
        this.returnValue = new ReturnValue(state, printer);
        this.returnValue.handleReturnSpace(lambdaReturnType);
    }

    public String handleCleanup() {
        printer.empty();

        for (KernelVariable var : variables) {
            String cleanupCode = var.getCleanupCode();
            if (cleanupCode != null) {
                printer.append(cleanupCode).appendNl();
            }

        }
        this.returnValue.handleCleanup();

        return printer.toString();
    }

    public String getReturnValueName() {
        return this.returnValue.getReturnValueName();
    }

    public List<Pair<String, JCTree.JCVariableDecl>> getDomainArgumentNames() {
        if (domainArgumentNames == null) {
            domainArgumentNames = new LinkedList<Pair<String, JCTree.JCVariableDecl>>();
            List<JCTree.JCVariableDecl> domainArguments = state.domainIterState.iter.domargs;
            for (JCTree.JCVariableDecl decl : domainArguments) {
                domainArgumentNames.add(new Pair<String, JCTree.JCVariableDecl>(decl.name.toString(), decl));
            }
        }
        return this.domainArgumentNames;
    }

    public Map<String, Pair<KernelVariable, JCTree.JCExpression>> getVariableMap() {
        return variableMap;
    }

    public ReturnValue getReturnValue() {
        return returnValue;
    }
    
    public List<KernelVariable> getArrays() {
        List<KernelVariable> arrayTypeVars = new ArrayList<KernelVariable>();
        for (KernelVariable kV : this.variables) {
            if (kV instanceof ArrayVariable || kV instanceof NewArrayVariable) {
                arrayTypeVars.add(kV);
            }
        }
        return arrayTypeVars;
    }
}
