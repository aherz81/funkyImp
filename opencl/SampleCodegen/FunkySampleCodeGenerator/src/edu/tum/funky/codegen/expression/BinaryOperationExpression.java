/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.tum.funky.codegen.expression;

/**
 *
 * @author Alexander Pöppl
 */
public class BinaryOperationExpression extends Expression {

    private final Expression left;
    private final Expression right;
    private final String operator;

    public BinaryOperationExpression(Expression left, Expression right, String operator, LiteralExpression.LiteralType literalType) {
        super(literalType);        
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        String leftCode = ((left instanceof BinaryOperationExpression) ? "(" + left.toString() + ")" : left.toString()) + " ";
        String rightCode = " " + ((right instanceof BinaryOperationExpression) ? "(" + right.toString() + ")" : right.toString());
        return leftCode + operator + rightCode;
    }

    @Override
    public int nodeCount() {
        return left.nodeCount() + right.nodeCount();
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    @Override
    public Expression simplify() {
        Expression simplifiedLeft = left.simplify();
        Expression simplifiedRight = right.simplify();
        if (simplifiedLeft instanceof LiteralExpression && simplifiedRight instanceof LiteralExpression) {
            LiteralExpression l = (LiteralExpression) simplifiedLeft;
            LiteralExpression r = (LiteralExpression) simplifiedRight;
            switch (operator) {
                case "+":
                    return new LiteralExpression(literalType, l.getIntValue() + r.getIntValue());
                case "-":
                    return new LiteralExpression(literalType, l.getFloatValue() - r.getFloatValue());
                case "*":
                    return new LiteralExpression(literalType, l.getIntValue() * r.getIntValue());
                case "/":
                    return new LiteralExpression(literalType, l.getFloatValue() / r.getFloatValue());
            }
        } else if (simplifiedLeft instanceof LiteralExpression && ((LiteralExpression) simplifiedLeft).getIntValue() == 0) {
            switch (operator) {
                case "*":
                case "/":
                    return new LiteralExpression(literalType, 0);
                case "+":
                    return simplifiedRight;
            }
        } else if (simplifiedRight instanceof LiteralExpression && ((LiteralExpression) simplifiedRight).getIntValue() == 0) {
            switch (operator) {
                case "*":
                    return new LiteralExpression(literalType, 0);
                case "-":
                case "+":
                    return simplifiedLeft;
            }
        } else if (operator.equals("-") && simplifiedLeft instanceof IdentifierExpression && simplifiedRight instanceof IdentifierExpression
                && ((IdentifierExpression) simplifiedLeft).getIdent().equals(((IdentifierExpression) simplifiedRight).getIdent())) {
            return new LiteralExpression(literalType, 0);
        }
        return new BinaryOperationExpression(simplifiedLeft, simplifiedRight, this.operator, literalType);
        
    }
}
