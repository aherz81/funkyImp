/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.util;

/**
 *
 * @author nax
 */
public class PrettyKernelStringBuilder  extends PrettyStringBuilder {

    private static final int INDENT_LENGTH = 4;
    
    private int indentLevel;
    
    public PrettyKernelStringBuilder() {
        this(0);
    }
    
    public PrettyKernelStringBuilder(int startIntendation) {
        super(null);
        this.indentLevel = startIntendation;
    }
    
    public void indent() {
        this.indentLevel++;
    }
    
    public void undent() {
        if (indentLevel > 0) {
            this.indentLevel--;
        }
    }
    
    @Override
    public PrettyStringBuilder appendAlign() {
        for (int i = 0; i < INDENT_LENGTH * indentLevel; i++) {
            sB.append(" ");
        }
        return this;
    }
    
    public int getIndentLevel() {
        return indentLevel;
    }
    
}
