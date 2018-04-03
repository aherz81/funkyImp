/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.kernel;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.cl.kernel.analyses.UsageOfIterationVariablesAnalysis;
import com.sun.tools.javac.tree.cl.kernel.analyses.functioncalls.KernelFunctionCallAnalysis;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.RuntimeAnalysis;
import com.sun.tools.javac.tree.cl.util.PrettyKernelStringBuilder;
import com.sun.tools.javac.tree.cl.variables.ArrayVariable;
import com.sun.tools.javac.tree.cl.variables.KernelInitializer;
import com.sun.tools.javac.tree.cl.variables.KernelVariable;
import com.sun.tools.javac.tree.cl.variables.ReturnValue;
import com.sun.tools.javac.tree.cl.variables.VariableAllocator;
import com.sun.tools.javac.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alexander Pöppl
 */
public class KernelCodeGenerator {

    private final JCTree tree;
    private final LowerTreeImpl state;
    private final OpenCLCodeEmitter codeEmitter;
    private final VariableAllocator variableAllocator;
    private final KernelInitializer kernelInitializer;
    private final PrettyKernelStringBuilder printer;
    private final Map<String, Pair<KernelVariable, JCTree.JCExpression>> variables;
    private Pair<String, ReturnValue> returnValue;
    private final VariableEnvironment environment;
    private final String currentPositionVarName;

    public KernelCodeGenerator(JCTree tree, LowerTreeImpl state, VariableAllocator variableAllocator, KernelInitializer kernelInitializer) {
        this.state = state;
        this.tree = tree;
        this.variableAllocator = variableAllocator;
        this.kernelInitializer = kernelInitializer;
        this.variables = variableAllocator.getVariableMap();
        this.printer = new PrettyKernelStringBuilder();
        this.returnValue = new Pair<String, ReturnValue>(variableAllocator.getReturnValueName(), variableAllocator.getReturnValue());
        this.environment = new VariableEnvironment(state, variables, returnValue, variableAllocator.getDomainArgumentNames());
        this.codeEmitter = new OpenCLCodeEmitter(state, printer, environment);
        this.currentPositionVarName = "__CUR_POS_" + state.domainIterState.uid;
    }

    public String getKernelCode() {
        //First, find out the functions called by the kernel. Topologically sorted. We will need to compile them as well.
        KernelFunctionCallAnalysis callAnalysis = new KernelFunctionCallAnalysis(this.tree, this.state);
        List<JCTree.JCMethodDecl> neededFunctions = callAnalysis.getNeededFunctions();

        for (JCTree.JCMethodDecl decl : neededFunctions) {
            this.environment.addMethodName(decl);
        }

        //Dump Function headers.
        this.printer.append("//FUNCTION HEADER DECLS").appendNl();
        this.codeEmitter.clState.position.push(OpenCLCodeEmitterState.CurrentPosition.INSIDE_FUNCTION_HEADER);
        for (JCTree.JCMethodDecl decl : neededFunctions) {
            this.codeEmitter.printMethodHeader(decl);
            this.printer.append(";").appendNl();
        }
        this.codeEmitter.clState.position.pop();

        //Print function declarations.
        this.printer.appendNl().append("//FUNCTION BODY DECLS").appendNl();
        for (JCTree.JCMethodDecl decl : neededFunctions) {
            this.codeEmitter.scan(decl);
        }

        //In the end, generate code for the kernel itself.
        this.printer.appendNl().append("//KERNEL CODE").appendNl();
        handleKernelCode();

        return printer.toString();
    }

    private void handleKernelCode() {
        // Handle the kernel header
        handleKernelHeader();

        // Open the function
        printer.appendNl().append("{");
        printer.indent();
        printer.appendNl();

        //Find out if we need to know about the iteration variables.
        handleDomainArgumentRetranslation();

        //We need to write into the array.
        handleReturnValueAssignment();

        //Do fancy compilation of the inner code.
        this.codeEmitter.clState.position.push(OpenCLCodeEmitterState.CurrentPosition.INSIDE_KERNEL);
        this.codeEmitter.scan(this.tree);
        this.codeEmitter.clState.position.pop();

        //The scanned entity is an expression in the default case, so we need to close with a semicolon.
        this.printer.append(";");

        //Close the function
        printer.undent();
        printer.appendNl();
        printer.append("}");
    }

    private void handleKernelHeader() {
        //Print name and dark incantations.
        printer.append("__kernel void ").append(kernelInitializer.getKernelName()).append("(");
        
        //Print Parameters.
        handleParameterList();
        printer.append(")");
    }

    private void handleParameterList() {
        for (String s : this.variables.keySet()) {
            KernelVariable var = variables.get(s).fst;
            printer.append(var.getCLCParam());
        }
        returnValue = new Pair<String, ReturnValue>(variableAllocator.getReturnValueName(), variableAllocator.getReturnValue());
        printer.append(returnValue.snd.getCLCParam());
    }

    private void handleReturnValueAssignment() {
        printer.append(this.returnValue.fst).append("[").append(currentPositionVarName).append("]=");
    }

    private void handleDomainArgumentRetranslation() {
        //Find variables that need to be synthesized in the kernel body.
        UsageOfIterationVariablesAnalysis usageAnalysis = new UsageOfIterationVariablesAnalysis(this.variableAllocator.getDomainArgumentNames());
        usageAnalysis.scan(tree);
        Set<String> neededVariables = usageAnalysis.getNecessaryIterationVariables();
        
        //Hack that enables evaluation of Local Memory.
        if (state.jc.useLocalOpenCLHack && variableAllocator.getVariableMap().containsKey(state.jc.localIdentifier)) {
            KernelVariable localVar = variableAllocator.getVariableMap().get(state.jc.localIdentifier).fst;
            if (!(localVar instanceof ArrayVariable)) {
                printer.append("local float __loc;").appendNl();
            } else {
                ArrayVariable localArray = (ArrayVariable) localVar;
                printer.append("local float __loc["+ localArray.getSize() +"];").appendNl();
            }
        }
        
        
        //If we do, add them to the environment, and generate code for them.
        if (!neededVariables.isEmpty()) {
            for (String var : usageAnalysis.getNecessaryIterationVariables()) {
                environment.addDomainIterationVariable(var);
            }
            DomainArgumentReTranslator translator = new DomainArgumentReTranslator(state, environment, printer, currentPositionVarName, neededVariables);
            translator.generateDomainArgumentCode();
        } else {
            printer.append("size_t ")
                    .append(this.currentPositionVarName)
                    .append(" = get_global_id(0);")
                    .appendNl();
        }
    }
}
