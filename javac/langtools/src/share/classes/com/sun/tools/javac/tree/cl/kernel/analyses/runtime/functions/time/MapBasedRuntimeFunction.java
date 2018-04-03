/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.time;

import java.util.Map;

/**
 *
 * @author Alexander Pöppl
 */
public class MapBasedRuntimeFunction implements RuntimeFunction {

    private final Map<Long, Double> measuredValues;
    
    public MapBasedRuntimeFunction(Map<Long, Double> measuredValues) {
        this.measuredValues = measuredValues;
    }

    public double getTimeFor(long inputSize) {
        final double log = Math.log(inputSize) / Math.log(2);
        final long floor = (int) Math.pow(2, (int) Math.floor(log));
        final long ceil = (int) Math.pow(2, Math.ceil(log));
        if (ceil == floor) {
            return Math.abs(measuredValues.get(floor));
        } else {
            double ratio = (((double) inputSize) - ((double) floor)) / ((double) ceil - (double) floor);
            double fVal = Math.abs(measuredValues.get(floor));
            double cVal = Math.abs(measuredValues.get(ceil));
            return (ratio * cVal + (1 - ratio) * fVal);
        }
    }
}
