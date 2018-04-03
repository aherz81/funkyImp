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
public class LiteralExpression extends Expression {

    public enum LiteralType {

        INT, FLOAT
    }

    private final float value;

    public LiteralExpression(LiteralExpression.LiteralType literalType, float value) {
        super(literalType);
        this.value = value;
    }

    @Override
    public String toString() {
        if (this.literalType == LiteralType.INT) {
            return Integer.toString(Math.round(value));
        } else {
            return Float.toString(value) + "f";
        }
    }

    public float getFloatValue() {
        return value;
    }

    public int getIntValue() {
        return Math.round(value);
    }
}
