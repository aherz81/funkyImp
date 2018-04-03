/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity;

/**
 * @author Alexander Pöppl
 */
public class InverseExponentialMultiplicityFunction implements MultiplicityFunction {
    /*  
     *   Exponential decay using function: offset + amplitude *exp(-x/eFoldingTime)
     */
    
    private final double amplitude;
    private final double eFoldingTime;
    private final double offset;
    
    public InverseExponentialMultiplicityFunction(double amplitude, double eFoldingTime, double offset) {
        this.amplitude = amplitude;
        this.eFoldingTime = eFoldingTime;
        this.offset = offset;
    }

    public double getMultiplier(long numberOfOperations) {
        return offset + amplitude * Math.exp(-numberOfOperations/eFoldingTime);
    }
}
