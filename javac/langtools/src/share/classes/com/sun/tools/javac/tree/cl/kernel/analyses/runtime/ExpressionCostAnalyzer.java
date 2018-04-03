/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.kernel.analyses.runtime;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.CostModel.CostType;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity.AlwaysUseSingleTimeMultiplicityFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity.HyperboleMultiplicityFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity.MultiplicityFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity.NoInfluenceMultiplicityFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity.ShadowedMultiplicityFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.time.LinearRuntimeFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.time.RuntimeFunction;
import com.sun.tools.javac.tree.cl.variables.VariableAllocator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeKind;

import static com.sun.tools.javac.tree.cl.kernel.analyses.runtime.CostModel.CostType.*;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.GT650CostModel;


/**
 *
 * @author Alexander Pöppl
 */
class ExpressionCostAnalyzer extends TreeScanner {

    private final Map<CostType, Integer> costCount;
    private double estimatedCosts;

    private final JCTree.JCExpression expression;
    private final LowerTreeImpl parentState;
    private final VariableAllocator varManager;
    private final long targetSize;

    private final Map<CostType, RuntimeFunction> costFunctions;
    private final Map<CostType, MultiplicityFunction> multiplicityModifiers;

    private final Set<String> cachedMemoryAccesses;


    ExpressionCostAnalyzer(final JCTree.JCDomainIter domainIteration, final VariableAllocator varManager, LowerTreeImpl parentState, CostModel model) {

        this.targetSize = varManager.getReturnValue().getReturnValueSize();
        this.expression = domainIteration.body;
        this.varManager = varManager;
        this.parentState = parentState;
        this.estimatedCosts = 0;
        this.cachedMemoryAccesses = new HashSet<String>();

        this.costCount = new EnumMap<CostType, Integer>(CostType.class);
        for (CostType t : CostType.values()) {
            this.costCount.put(t, 0);
        }
        this.costFunctions = model.getRuntimeCosts();
        this.multiplicityModifiers = model.getMultiplicityFunction();
    }

    public double getEstimatedCostOfKernelExpressions() {
        scan(expression);
        addBaseCost();
        calculateRuntimeCost();
        return this.estimatedCosts;
    }

    private void printDebugOutput() {
        int totalCosts = 0;
        for (CostType operation : this.costCount.keySet()) {
            double costForOperation = this.costCount.get(operation) * costFunctions.get(operation).getTimeFor(targetSize);
            System.out.println(operation + ":\t" + this.costCount.get(operation) + "\t" + costForOperation);
            totalCosts += costForOperation;
        }
        System.out.println("Total cost: " + totalCosts);
    }

    @Override
    public void visitBinary(JCTree.JCBinary tree) {
        int operator = tree.getTag();
        if (tree.type.getKind() == TypeKind.INT) {
            addIntOperatorCost(operator);
        } else if (tree.type.getKind() == TypeKind.FLOAT) {
            addFloatOperatorCost(operator);
        }
        super.visitBinary(tree);
    }

    @Override
    public void visitIdent(JCTree.JCIdent tree) {
        Type identType = tree.type;
        if (identType.isPrimitive()) {
            registerCosts(PRIVATE_ACCESS);
        }
    }

    @Override
    public void visitIndexed(JCTree.JCArrayAccess tree) {
        int numberOfDimensions = tree.index.size();
        MemoryAccessComplexityAnalyzer memoryAccessComplexityAnalyzer = new MemoryAccessComplexityAnalyzer(tree, varManager);
        MemoryAccessComplexityAnalyzer.MemoryReadType accessComplexity = memoryAccessComplexityAnalyzer.computeMemoryReadComplexity();
        System.out.println("Access Complexity: " + accessComplexity + " (" + tree + ")");
        switch (accessComplexity) {
            case CONSTANT_READ:
                registerCosts(CONSTANT_GLOBAL_READ);
                break;
            case INTERVAL_READ:
                registerCosts(CACHED_GLOBAL_READ);
                break;
            case CONTINUOUS_READ:
                if (!this.cachedMemoryAccesses.contains(tree.toString())) {
                    registerCosts(GLOBAL_READ);
                    this.cachedMemoryAccesses.add(tree.toString());
                } else {
                    registerCosts(IDENTICAL_GLOBAL_READ);
                }
                break;
            case COMPLEX_READ:
                if (!this.cachedMemoryAccesses.contains(tree.toString())) {
                    registerCosts(COMPLEX_GLOBAL_READ);
                    this.cachedMemoryAccesses.add(tree.toString());
                }
                break;
            case LOCAL_ACCESS:
                registerCosts(LOCAL_ACCESS);
                break;
        }

        for (int i = 0; i < numberOfDimensions; i++) {
            registerCosts(INT_ADD);
            registerCosts(INT_MUL);
        }
        scan(tree.indexed);
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation tree) {
        scan(tree.getArguments());
        JCTree.JCMethodDecl method = null;
        if (tree.meth instanceof JCTree.JCIdent) {
            JCTree.JCIdent methodIdentifier = (JCTree.JCIdent) tree.meth;
            for (JCTree methodCandidate : this.parentState.jc.callGraph.vertexSet()) {
                if (((JCTree.JCMethodDecl) methodCandidate).sym.equals(methodIdentifier.sym)) {
                    method = (JCTree.JCMethodDecl) methodCandidate;
                }
            }
        }
        if (method != null) {
            scan(method.body);
        }
    }

    private void addFloatOperatorCost(int operator) {
        switch (operator) {
            case JCTree.PLUS:
                registerCosts(FLOAT_ADD);
                break;
            case JCTree.MINUS:
                registerCosts(FLOAT_SUB);
                break;
            case JCTree.MUL:
                registerCosts(FLOAT_MUL);
                break;
            case JCTree.DIV:
                registerCosts(FLOAT_DIV);
                break;
        }
    }

    private void addIntOperatorCost(int operator) {
        switch (operator) {
            case JCTree.PLUS:
                registerCosts(INT_ADD);
                break;
            case JCTree.MINUS:
                registerCosts(INT_SUB);
                break;
            case JCTree.MUL:
                registerCosts(INT_MUL);
                break;
            case JCTree.DIV:
                registerCosts(INT_DIV);
                break;
        }
    }

    private void addBaseCost() {
        //To find out the position in the array, several divisions and modulos need to be performed.
        final int numberOfDimensions = this.varManager.getDomainArgumentNames().size();
        
        //Practical examples show that the costs for the write to be negligible. Hence they will be ignored.
        //registerCosts(GLOBAL_WRITE);
        registerCosts(BASE_COST);
        for (int i = 0; i < numberOfDimensions; i++) {
            registerCosts(INT_DIV);
            registerCosts(INT_DIV);
        }
        registerCosts(INT_MUL);
    }

    private void registerCosts(final CostType operator) {
        this.costCount.put(operator, this.costCount.get(operator) + 1);
    }

    private void calculateRuntimeCost() {
        StringBuilder sB = new StringBuilder().append('\n');
        for (CostType operator : this.costCount.keySet()) {
            double operationCost = this.costFunctions.get(operator).getTimeFor(this.targetSize);
            double multiplier = this.multiplicityModifiers.get(operator).getMultiplier(this.costCount.get(operator));
            double estimatedCost = operationCost * (double) this.costCount.get(operator) * multiplier;
            this.estimatedCosts += estimatedCost;
            String tabs = (operator.name().length() < 8) ? "\t\t\t" : (operator.name().length() < 15) ? "\t\t" : "\t";
            sB.append(operator)
                    .append(tabs)
                    .append(this.costCount.get(operator)).append("\t")
                    .append(estimatedCost).append("\n");
        }
        System.out.print(sB);
    }
}
