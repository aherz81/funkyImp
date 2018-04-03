/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.time;

/**
 *
 * @author Alexander Pöppl
 */
public class LinearRuntimeFunction implements RuntimeFunction {
    
    private final double slope;
    private final double yIntersect;
    
    public LinearRuntimeFunction(double slope, double yIntersect) {
        this.slope = slope;
        this.yIntersect = yIntersect;
    }

    public double getTimeFor(long inputSize) {
        return slope * inputSize + yIntersect;
    }
}
