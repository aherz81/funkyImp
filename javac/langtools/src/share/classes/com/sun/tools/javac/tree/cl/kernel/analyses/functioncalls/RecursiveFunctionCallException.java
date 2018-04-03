/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.functioncalls;

/**
 *
 * @author Alexander Pöppl
 */
public class RecursiveFunctionCallException extends RuntimeException {
    
    /**
     *
     */
    public static final long serialVersionUID = -1L;

    public RecursiveFunctionCallException(String message, Throwable t) {
        super(message, t);
    }
    
    public RecursiveFunctionCallException(String message) {
        super(message);
    }
    
}
