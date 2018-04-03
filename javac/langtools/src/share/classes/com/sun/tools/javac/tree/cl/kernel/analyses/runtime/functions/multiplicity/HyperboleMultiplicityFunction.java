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
public class HyperboleMultiplicityFunction implements MultiplicityFunction {
    
    private final double baseSize;
    private final double yOffset;
    
    public HyperboleMultiplicityFunction(double baseSize, double yOffset) {
        this.baseSize = baseSize;
        this.yOffset = yOffset;
    }

    public double getMultiplier(long numberOfOperations) {
        if (numberOfOperations == 0) {
            return 0.0;
        } else {
            return baseSize / numberOfOperations + yOffset;
        }
    }

    
    
}
