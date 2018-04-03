/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.functioncalls;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jgrapht.DirectedGraph;

/**
 * @author Alexander Pöppl
 */
public class CalledFunctionScanner extends TreeScanner {

    private final DirectedGraph<JCTree, JCTree.Arc> callGraph;
    private final Set<JCTree.JCMethodDecl> calledMethods;
    
    public CalledFunctionScanner(DirectedGraph<JCTree, JCTree.Arc> callGraph) {
        this.callGraph = callGraph;
        this.calledMethods = new LinkedHashSet<JCTree.JCMethodDecl>();
    }
    
    @Override
    public void visitApply(JCTree.JCMethodInvocation invocation) {
        JCTree.JCIdent methName = (JCTree.JCIdent) invocation.meth;
        for (JCTree method : callGraph.vertexSet()) {
            if (((JCTree.JCMethodDecl) method).sym.equals(methName.sym)) {
                this.calledMethods.add((JCTree.JCMethodDecl) method);
            }
        }
    }
    
    public Set<JCTree.JCMethodDecl> getCalledMethods() {
        return this.calledMethods;
    }
    
}
