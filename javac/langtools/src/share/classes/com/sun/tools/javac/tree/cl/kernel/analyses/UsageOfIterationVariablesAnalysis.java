/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Pair;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Alexander Pöppl
 */
public class UsageOfIterationVariablesAnalysis extends TreeScanner {

    private final Set<String> iterationVariables;
    private final Set<String> usedIterationVariables;
    
    public UsageOfIterationVariablesAnalysis(List<Pair<String, JCTree.JCVariableDecl>> iterationVariables) {
        this.iterationVariables = new LinkedHashSet<String>();
        for (Pair<String, JCTree.JCVariableDecl> var : iterationVariables) {
            this.iterationVariables.add(var.fst);
        }
        this.usedIterationVariables = new LinkedHashSet<String>();
    }
    
    @Override
    public void scan(JCTree tree) {
        if (tree != null) {
            tree.accept(this);
        }
    }

    @Override
    public void visitIdent(JCTree.JCIdent tree) {
        final String ident = tree.name.toString();
        if (this.iterationVariables.contains(ident)) {
            usedIterationVariables.add(ident);
        }
    }

    public Set<String> getNecessaryIterationVariables() {
        return usedIterationVariables;
    }
    
    
}
