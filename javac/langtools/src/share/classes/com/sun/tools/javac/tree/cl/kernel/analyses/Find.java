/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.kernel.analyses;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;

/**
 *
 * @author Alexander Herz
 */
public class Find extends TreeScanner {

    JCTree.JCMethodInvocation result = null;

    public void scan(JCTree tree) {
        if (tree != null) {
            tree.accept(this);
        }
    }

    public void visitApply(JCTree.JCMethodInvocation tree) {
        result = tree;
    }
    
    public JCTree.JCMethodInvocation getResult() {
        return result;
    }
}
