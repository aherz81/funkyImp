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
public class SimpleArrayAccessExpression extends ArrayAccess {

    public SimpleArrayAccessExpression() {
        super("matrix");
    }

    public String toString() {
        return this.array + "[x,y]";
    }
}
