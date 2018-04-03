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
public class AlwaysUseSingleTimeMultiplicityFunction implements MultiplicityFunction {

    public double getMultiplier(long numberOfOperations) {
        if (numberOfOperations == 0) {
            return 0.0;
        } else {
            return 1.0 / (double) numberOfOperations;
        }
    }  
}
