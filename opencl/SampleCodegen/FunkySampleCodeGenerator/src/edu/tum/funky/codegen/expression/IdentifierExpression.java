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
    public class IdentifierExpression extends Expression {

        private final String ident;

        public IdentifierExpression(String ident) {
            super(LiteralExpression.LiteralType.INT);
            this.ident = ident;
        }

        @Override
        public String toString() {
            return ident;
        }
        
        public String getIdent() {
            return ident;
        }
    }