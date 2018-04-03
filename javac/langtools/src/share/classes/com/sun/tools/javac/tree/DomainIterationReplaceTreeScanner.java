/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree;

import com.sun.tools.javac.code.Symbol;
import java.util.Map;

/**
 *
 * @author Alexander Pöppl
 */
public class DomainIterationReplaceTreeScanner extends TreeScanner {

    private final Map<String, Symbol.VarSymbol> map;

    public DomainIterationReplaceTreeScanner(Map<String, Symbol.VarSymbol> map) {
        this.map = map;
    }

    public void scan(JCTree tree) {
        if (tree != null) {
            tree.accept(this);
        }
    }

    public void visitVarDef(JCTree.JCVariableDecl tree) {
        Symbol.VarSymbol sym = map.get(tree.name.toString());
        if (sym != null) {
            tree.name = sym.name;
            tree.sym = sym;
        }
        super.visitVarDef(tree);
    }

    public void visitIdent(JCTree.JCIdent tree) {
        Symbol.VarSymbol sym = map.get(tree.name.toString());
        if (sym != null) {
            tree.name = sym.name;
            tree.sym = sym;
        }
        super.visitIdent(tree);
    }
}
