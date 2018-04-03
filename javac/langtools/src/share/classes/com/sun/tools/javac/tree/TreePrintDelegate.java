/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree;

import java.io.IOException;

/**
 *
 * @author nax
 */
public interface TreePrintDelegate {
    public void printExpr(JCTree expr) throws IOException;
}
