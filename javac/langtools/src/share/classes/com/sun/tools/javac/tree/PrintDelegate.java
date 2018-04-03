/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree;

import java.io.IOException;

/**
 *
 * @author Alexander Pöppl
 */
public interface PrintDelegate {
    public void print(Object name) throws IOException;
}
