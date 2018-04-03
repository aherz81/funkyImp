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
public class NoInfluenceMultiplicityFunction implements MultiplicityFunction {
    
    public double getMultiplier(long numberOfOperations) {
        return 1.0;
    }

}
