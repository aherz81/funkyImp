/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.jvm;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.io.IOException;
import javax.tools.JavaFileObject;

/**
 *
 * @author aherz
 */
public interface CodeWriter
{
    /** Thrown when the constant pool is over full.
     */
    public static class PoolOverflow extends Exception {
        private static final long serialVersionUID = 0;
        public PoolOverflow() {}
    }
    public static class StringOverflow extends Exception {
        private static final long serialVersionUID = 0;
        public final String value;
        public StringOverflow(String s) {
            value = s;
        }
    }
    public JavaFileObject writeClass(ClassSymbol c)
            throws IOException, PoolOverflow, StringOverflow;
}
