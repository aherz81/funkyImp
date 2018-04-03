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
public abstract class ArrayAccess extends Expression {

    protected final String array;

    protected ArrayAccess(String array) {
        super(LiteralExpression.LiteralType.INT);
        this.array = array;
    }
}
