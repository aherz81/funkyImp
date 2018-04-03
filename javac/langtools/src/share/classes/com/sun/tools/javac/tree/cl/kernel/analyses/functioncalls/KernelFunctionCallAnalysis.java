/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.kernel.analyses.functioncalls;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.Arc;
import com.sun.tools.javac.tree.LowerTreeImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
//import javax.media.j3d.Sound;

import org.jgrapht.*;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.traverse.ClosestFirstIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 * This class implements an analysis to construct a call graph.
 *
 * @author Alexander Pöppl
 */
public class KernelFunctionCallAnalysis {

    private final LowerTreeImpl state;
    private final JCTree tree;
    private final DirectedGraph<JCTree, Arc> callGraph;
    private final Map<JCTree, Integer> topologicalNodes;

    /**
     * Main constructor.
     *
     * @param tree The root of the call graph analysis.
     * @param state the Compiler state.
     */
    public KernelFunctionCallAnalysis(JCTree tree, LowerTreeImpl state) {
        if (!state.inside_method) {
            throw new IllegalArgumentException("This analysis may only be called inside a method.");
        }
        this.state = state;
        this.tree = tree;
        this.callGraph = state.jc.callGraph;
        this.topologicalNodes = state.jc.topolNodes;
    }

    public List<JCTree.JCMethodDecl> getNeededFunctions() {
        List<JCTree.JCMethodDecl> orderedMethods;
        Set<JCTree> methods = this.callGraph.vertexSet();
        CalledFunctionScanner calledFunctionScanner = new CalledFunctionScanner(this.callGraph);
        calledFunctionScanner.scan(tree);
        Set<JCTree.JCMethodDecl> calledMethods = calledFunctionScanner.getCalledMethods();
        checkForRecursion(calledMethods);
        orderedMethods = getNeededMethods(calledMethods);
        return orderedMethods;
    }

    public void checkForRecursion(Set<JCTree.JCMethodDecl> calledFunctions) {
        if (state.method.sym.mayBeRecursive) {
            //We only need to generate code for part of the method, this part may not be recursive.
            //This needs to be checked out, hence a Cycle detection is performed on all called functions.
            //If the whole method we are generating code for is not recursive, there's nothing to worry about.
            CycleDetector<JCTree, Arc> cycleDetector = new CycleDetector<JCTree, Arc>(callGraph);
            boolean foundCycle = false;
            for (JCTree.JCMethodDecl method : calledFunctions) {
                foundCycle |= cycleDetector.detectCyclesContainingVertex(method);
            }
            if (foundCycle) {
                throw new RecursiveFunctionCallException("Recursive Function calls are not allowed in OpenCL.");
            }
        }
    }

    private List<JCTree.JCMethodDecl> getNeededMethods(Set<JCTree.JCMethodDecl> calledMethods) {
        final List<JCTree.JCMethodDecl> methods = new ArrayList<JCTree.JCMethodDecl>(collectMethods(calledMethods));
        final Map<JCTree, Integer> topologicalOrder = this.topologicalNodes;
        Collections.sort(methods, new Comparator<JCTree.JCMethodDecl>() {
            public int compare(JCTree.JCMethodDecl o1, JCTree.JCMethodDecl o2) {
                int o1Rank = topologicalOrder.get(o1);
                int o2Rank = topologicalOrder.get(o2);
                return o2Rank - o1Rank;
            }
        });
        return methods;
    }

    private Set<JCTree.JCMethodDecl> collectMethods(Set<JCTree.JCMethodDecl> calledMethods) {
        Set<JCTree.JCMethodDecl> allMethods = new LinkedHashSet<JCTree.JCMethodDecl>();
        for (JCTree.JCMethodDecl m : calledMethods) {
            allMethods.add(m);
            ClosestFirstIterator<JCTree, Arc> cfIt = new ClosestFirstIterator<JCTree, Arc>(callGraph, m);
            while (cfIt.hasNext()) {
                JCTree.JCMethodDecl foundMethod = (JCTree.JCMethodDecl) cfIt.next();
                allMethods.add(foundMethod);
            }
        }
        return allMethods;
    }
}
