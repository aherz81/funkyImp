/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.jvm;

import com.sun.tools.javac.comp.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.jvm.Code.*;
import com.sun.tools.javac.jvm.Items.*;
import com.sun.tools.javac.tree.JCTree.*;


/**
 *
 * @author aherz
 */
public interface CodeGen {
    public boolean genClass(Env<AttrContext> env, JCClassDecl cdef);
}
