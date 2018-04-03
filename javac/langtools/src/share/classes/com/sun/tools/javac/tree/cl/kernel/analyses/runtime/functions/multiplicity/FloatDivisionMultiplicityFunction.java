/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity;

/**
 *
 * @author Alexander Pöppl
 */
public class FloatDivisionMultiplicityFunction implements MultiplicityFunction {

    
    
    public double getMultiplier(long numberOfOperations) {
        if (numberOfOperations < 2) {
            return 1;
        } else if (numberOfOperations < 64) {
            return 1.5;
        } else {
            return 4;
        }
    }
    
}
