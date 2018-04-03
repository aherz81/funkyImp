/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.runtime;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.K4000CostModel;
import com.sun.tools.javac.tree.cl.variables.VariableAllocator;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexander Pöppl
 */
public class RuntimeAnalysis {
    
    private static final CostModel targetGPU = new K4000CostModel();
    
    private final TransferCostAnalyzer transferCostAnalyzer;
    private final ExpressionCostAnalyzer expressionCostAnalyzer;
    private final DegreeOfParallelismCostAnalyzer parallelismCostAnalyzer;
    
    private final LowerTreeImpl state;
    private final JCTree.JCDomainIter domainIteration;
    private final VariableAllocator variableAllocator;
    
    private static Map<String, ArrayList> runtimes;
    
    static {
        runtimes = new HashMap<String, ArrayList>();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            
            public void run() {
                Path output = Paths.get("/Users/nax/Desktop/bench");
            }
        }));
    }
    
    public RuntimeAnalysis(JCTree.JCDomainIter domIter, LowerTreeImpl state, VariableAllocator variableAllocator) {
        this.domainIteration = domIter;
        this.state = state;
        this.variableAllocator = variableAllocator;
        this.transferCostAnalyzer = new TransferCostAnalyzer(this.variableAllocator, targetGPU);
        this.expressionCostAnalyzer = new ExpressionCostAnalyzer(this.domainIteration, this.variableAllocator, state, targetGPU);
        this.parallelismCostAnalyzer = new DegreeOfParallelismCostAnalyzer(variableAllocator, targetGPU);
    }
    
    public double getProjectedRuntime() {
        double estimatedTransferCosts = transferCostAnalyzer.getEstimatedCosts() *1e6; //convert to microseconds
        double estimatedKernelRuntimeCosts = expressionCostAnalyzer.getEstimatedCostOfKernelExpressions();
        double workGroupSizeInfluenceMultiplier = parallelismCostAnalyzer.getWorkSizeMultiplier();
        System.out.println("Cost analysis(" + state.method.name.toString() + "): " + estimatedTransferCosts + "\t" + (estimatedKernelRuntimeCosts * workGroupSizeInfluenceMultiplier) + "\t");
        return estimatedTransferCosts + estimatedKernelRuntimeCosts * workGroupSizeInfluenceMultiplier ;
    } 
}
