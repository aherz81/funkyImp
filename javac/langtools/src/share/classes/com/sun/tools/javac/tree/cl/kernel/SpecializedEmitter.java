/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.cl.util.PrettyKernelStringBuilder;

/**
 *
 * @author Alexander Pöppl
 */
public abstract class SpecializedEmitter {
    protected VariableEnvironment environment;
    protected OpenCLCodeEmitter parentEmitter;
    protected PrettyKernelStringBuilder printer;
    protected LowerTreeImpl state;

    public SpecializedEmitter(OpenCLCodeEmitter parent, LowerTreeImpl state, VariableEnvironment environment, PrettyKernelStringBuilder printer) {
        this.state = state;
        this.environment = environment;
        this.printer = printer;
        this.parentEmitter = parent;

    }
}
