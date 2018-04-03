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
public abstract class Expression {

    protected final LiteralExpression.LiteralType literalType;
    
    public Expression(LiteralExpression.LiteralType literalType) {
        this.literalType = literalType;
    }
    
    @Override
    public abstract String toString();

    public Expression simplify() {
        return this;
    }
    
    public int nodeCount() {
        return 1;
    }
}
