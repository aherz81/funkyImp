/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.kernel;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.ArrayAccessPrinter;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.PrintDelegate;
import com.sun.tools.javac.tree.TreePrintDelegate;
import com.sun.tools.javac.tree.cl.util.PrettyKernelStringBuilder;
import java.io.IOException;

/**
 *
 * @author Alexander Pöppl
 */
class ArrayAccessEmitter extends SpecializedEmitter implements PrintDelegate, TreePrintDelegate {

    private final JCTree.JCArrayAccess arrayAccess;

    ArrayAccessEmitter(OpenCLCodeEmitter parent, LowerTreeImpl state, VariableEnvironment environment, PrettyKernelStringBuilder arrayPrinter, JCTree.JCArrayAccess arrayAccess) {
        super(parent, state, environment, arrayPrinter);
        this.arrayAccess = arrayAccess;
    }

    String printArrayAccess() {
        Type.ArrayType at = (Type.ArrayType) this.arrayAccess.indexed.type;
        //First we need to print the Array that is being indexed.

        //Hack to enable local memory in evaluation.
        if (state.jc.useLocalOpenCLHack && this.arrayAccess.indexed.toString().equals(state.jc.localIdentifier)) {
            printer.append("__loc").append("[");
        } else {
            printer.append("(");
            parentEmitter.scan(this.arrayAccess.indexed);
            printer.append(")").append("[");
        }
        //Next, print the Array offset.
        ArrayAccessPrinter arrayAccessPrinter = new ArrayAccessPrinter(state, this, this);
        try {
            arrayAccessPrinter.generateDomainOffsetCode(this.arrayAccess.pos(), this.arrayAccess, at, this.arrayAccess.index.toArray(new JCTree.JCExpression[0]), true, false, false);
        } catch (IOException e) {
            throw new RuntimeException("This should not happen here. No IO performed.", e);
        }
        printer.append("]");
        
        return printer.toString();
    }

    public void print(Object name) {
        this.printer.append(name);
    }

    public void printExpr(JCTree tree) {
        this.parentEmitter.scan(tree);
    }
}
