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
public class RandomArrayAccessExpression extends ArrayAccess {

    private final Expression xDiv;
    private final Expression yDiv;

    public RandomArrayAccessExpression(CodeGeneratorConfiguration config) {
        super("matrix");
        ExpressionTreeCreator creator = new ExpressionTreeCreator(LiteralExpression.LiteralType.INT, config);
        xDiv = creator
                .createTree()
                .filter(tree -> config.isComplexAccessAllowed() || tree.nodeCount() < 2 )
                .orElse(new LiteralExpression(LiteralExpression.LiteralType.INT, 1));
        yDiv = creator
                .createTree()
                .filter(tree -> config.isComplexAccessAllowed() || tree.nodeCount() < 2)
                .orElse(new LiteralExpression(LiteralExpression.LiteralType.INT, 1));
    }

    @Override
    public String toString() {
        String x = (xDiv instanceof BinaryOperationExpression) ? "(" + xDiv.toString() + ")" : xDiv.toString();
        String y = (yDiv instanceof BinaryOperationExpression) ? "(" + yDiv.toString() + ")" : yDiv.toString();
        return this.array + "[" + x + " % HEIGHT, " + y + " % WIDTH]";
    }
}
