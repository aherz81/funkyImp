/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.variables;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Alexander Pöppl
 */
public class VariableCapturer extends TreeScanner {
    
    private final Map<String, JCTree.JCExpression> occuringIdentifiers;
    private final Map<String, JCTree.JCVariableDecl> occuringDeclarations;
    private final JCTree tree;
    
    public VariableCapturer(JCTree tree) {
        this.occuringIdentifiers = new LinkedHashMap<String, JCTree.JCExpression>();
        this.occuringDeclarations = new LinkedHashMap<String, JCTree.JCVariableDecl>();
        this.tree = tree;
    }

    public Map<String, JCTree.JCExpression> getNeccesaryVariables() {
        scan(this.tree);
        return this.occuringIdentifiers;
    }
    
    @Override
    public void visitIdent(JCTree.JCIdent tree) {
        occuringIdentifiers.put(tree.name.toString(), tree);
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl tree) {
        //FIXME Filter out locally declared Vars
        occuringDeclarations.put(null, tree);
        
    }
    
    
}
