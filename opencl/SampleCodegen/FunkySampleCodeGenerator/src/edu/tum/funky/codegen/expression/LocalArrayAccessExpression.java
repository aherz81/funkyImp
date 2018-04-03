/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.tum.funky.codegen.expression;

import edu.tum.funky.codegen.CodeGeneratorConfiguration;
import edu.tum.funky.codegen.ExpressionTreeCreator;

/**
 *
 * @author Alexander Pöppl
 */
public class LocalArrayAccessExpression extends ArrayAccess {
    
    private final Expression access;
    
    public LocalArrayAccessExpression(CodeGeneratorConfiguration config) {
        super("xxx");
        ExpressionTreeCreator creator = new ExpressionTreeCreator(LiteralExpression.LiteralType.INT, config);
        access = creator
                .createTree()
                .filter(tree -> config.isComplexAccessAllowed() || tree.nodeCount() < 2 )
                .orElse(new LiteralExpression(LiteralExpression.LiteralType.INT, 1));
    }

    @Override
    public String toString() {
        String indexExp = (access instanceof BinaryOperationExpression) ? "(" + access + ")" : access.toString();
        return this.array + "[" + indexExp + " & 0x7F]";
    }
}
