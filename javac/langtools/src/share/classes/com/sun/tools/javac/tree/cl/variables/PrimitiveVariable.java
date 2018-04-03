/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.variables;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.LowerTreeImpl;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Alexander Pöppl
 */
public class PrimitiveVariable extends KernelVariable {
    
    public PrimitiveVariable(JCTree.JCIdent capturedIdentifier, LowerTreeImpl state) {
        super(capturedIdentifier, state);
        
    }
    
    public PrimitiveVariable(JCTree.JCVariableDecl varDecl, LowerTreeImpl state) {
        super(varDecl, state);
    }
    
    @Override
    public String getGeneratedType() {
        return this.type.toString();
    }
    
    @Override
    public List<String> getDeclarations() {
        List<String> res = new LinkedList<String>();
        String varDecl = getGeneratedType() + " " + getGeneratedName() + "=" + getOriginalName() + ";"; 
        res.add(varDecl);
        return res;
    }
    
    @Override
    public String getGPUCopyCode() {
        return null;
    }
    
    @Override
    public List<String> getParams() {
        List<String> res = new LinkedList<String>();
        res.add("&" + getGeneratedName());
        return res;
    }
    
    @Override 
    public String getCleanupCode() {
        return null;
    }

    @Override
    public String getCLCParam() {
        return getGeneratedType() + " " + getGeneratedName() + ", ";
    }
    
    

}
