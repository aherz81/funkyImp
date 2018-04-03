/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.util;

import com.sun.tools.javac.tree.LowerTreeImpl;

/**
 *
 * @author Alexander Pöppl
 */
public class PrettyStringBuilder {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    
    protected StringBuilder sB;
    private LowerTreeImpl state;
    
    public PrettyStringBuilder(LowerTreeImpl state) {
        this.state = state;
        sB = new StringBuilder();
    }

    public PrettyStringBuilder appendNl() {
        return this.appendLb().appendAlign();
    }
    
    public PrettyStringBuilder appendLb() {
        sB.append(LINE_SEPARATOR);
        return this;
    }

    public PrettyStringBuilder appendAlign() {
        for (int i = 0; i < state.lmargin; i++) {
            sB.append(" ");
        }
        return this;
    }
    
    public PrettyStringBuilder append(int i) {
        sB.append(i);
        return this;
    }
    
    public PrettyStringBuilder append(long l) {
        sB.append(l);
        return this;
    }
        
    public PrettyStringBuilder append(boolean b) {
        sB.append(b);
        return this;
    }
    
    public PrettyStringBuilder append(String s) {
        sB.append(s);
        return this;
    }
    
    public PrettyStringBuilder append(float f) {
        sB.append(f);
        return this;
    }
    
    public PrettyStringBuilder append(double d) {
        sB.append(d);
        return this;
    }
    
    public PrettyStringBuilder append(Object o) {
        sB.append(o);
        return this;
    }
    
    @Override
    public String toString() {
        return sB.toString();
    }
    
    public void empty() {
        this.sB = new StringBuilder();
    }
}
