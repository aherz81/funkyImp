/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.time;

import com.sun.tools.javac.util.Pair;

/**
 *
 * @author Alexander Pöppl
 */
public class RangeRuntimeFunction implements RuntimeFunction {
    public static class Range {
        public final double rangeBegin;
        public final double rangeEnd;
        
        public Range(double rangeBegin, double rangeEnd) {
            if (rangeEnd < rangeBegin) {
                throw new IllegalArgumentException("End must be bigger or equal then begin.");
            } else {
                this.rangeBegin = rangeBegin;
                this.rangeEnd = rangeEnd;
            }
        }
    }
    
    public Pair<Range, RuntimeFunction>[] runtimeFunctions; 
    
    public RangeRuntimeFunction(Pair<Range, RuntimeFunction>... runtimeFunctions) {
        this.runtimeFunctions = runtimeFunctions;
    }

    public double getTimeFor(long inputSize) {
        for (Pair<Range, RuntimeFunction> funcPair : runtimeFunctions) {
            if (inputSize >= funcPair.fst.rangeBegin && inputSize <= funcPair.fst.rangeEnd) {
                return funcPair.snd.getTimeFor(inputSize);
            }
        }
        throw new IllegalArgumentException("No function for value!");
    }
}
