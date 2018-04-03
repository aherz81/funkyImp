/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel;

import com.sun.tools.javac.tree.JCTree;
import java.util.Stack;

/**
 *
 * @author Alexander Pöppl
 */
class OpenCLCodeEmitterState {
    enum CurrentPosition {
        TOP_LEVEL, INSIDE_FUNCTION_HEADER, INSIDE_FUNCTION, INSIDE_VAR_DECL, INSIDE_KERNEL
    }
    
    /**
     *  Stores the current context type.
     */
    Stack<CurrentPosition> position;
    
    /**
     *  Where the compiler currently is. If we are looking at a method declaration ({@link CurrentPosition} header or body), then the function will be stored here. Null otherwise.
     */
    JCTree.JCMethodDecl currentFunction;

    /**
     *  Where the compiler currently is. If we are looking at a variable declaration, it will be stored here. Null otherwise.
     */
    JCTree.JCVariableDecl varDecl;

    
    /**
     * If the compiler is inside a binary Expression, this shows how far in it is. This is neccessary for the righthand side.
     */
    int binaryExpressionDepth;
    
    
    
    public OpenCLCodeEmitterState() {
        this.position = new Stack<CurrentPosition>();
        this.position.push(OpenCLCodeEmitterState.CurrentPosition.TOP_LEVEL);

    }
}
