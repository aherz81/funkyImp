/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.variables;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.LowerTreeImpl;
import java.util.List;

/**
 *
 * @author Alexander Pöppl
 */
public abstract class KernelVariable {
    protected final JCExpression capturedIdentifier;
    protected final Type type;
    protected LowerTreeImpl state;
    
    private String varName;
    
    public KernelVariable(final JCIdent capturedIdentifier, final LowerTreeImpl state) {
        if (capturedIdentifier != null) {
            this.capturedIdentifier = capturedIdentifier;
            this.type = capturedIdentifier.type;
        } else {
            this.capturedIdentifier = null;
            this.type = null;
        }
        this.state = state;
    }
    
    public KernelVariable(final JCVariableDecl capturedIdentifier, final LowerTreeImpl state) {
        if (capturedIdentifier != null) {
            this.capturedIdentifier = capturedIdentifier;
            this.type = capturedIdentifier.type;
        } else {
            this.capturedIdentifier = null;
            this.type = null;
        }
        this.state = state;
    }
    

    public String getGeneratedName() {
        if (varName == null) {
                varName = "__" + getOriginalName() + "_" + this.state.domainIterState.uid;
        }
        return this.varName;
    }
    
    protected String getOriginalName() {
             if (this.capturedIdentifier instanceof JCIdent) {
                return ((JCIdent)this.capturedIdentifier).name.toString().replace('\'', '_');
            } else {
                return ((JCVariableDecl)this.capturedIdentifier).name.toString().replace('\'', '_');
            }
    }
    
    //Methods for the C++ part
    public abstract String getGeneratedType();
    public abstract List<String> getDeclarations();
    public abstract String getGPUCopyCode();
    public abstract List<String> getParams();
    public abstract String getCleanupCode();
    
    //Methods for the OpenCL-C part
    public abstract String getCLCParam();
}
