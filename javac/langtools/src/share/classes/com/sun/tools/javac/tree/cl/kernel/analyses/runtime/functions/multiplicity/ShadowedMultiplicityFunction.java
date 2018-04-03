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
public class ShadowedMultiplicityFunction implements MultiplicityFunction {

    private final int shadowedOperations;

    public ShadowedMultiplicityFunction(int shadowedOperations) {
        if (shadowedOperations <= 0) {
            throw new IllegalArgumentException("Number of Shadowed Operations must be positive.");
        }
        this.shadowedOperations = shadowedOperations;
    }

    public double getMultiplier(long numberOfOperations) {
        if (numberOfOperations == 0) {
            return 1;
        } else {
            if (numberOfOperations <= shadowedOperations) {
                return 1 / numberOfOperations;
            } else {
                return (numberOfOperations - shadowedOperations) / numberOfOperations;
            }
        }
    }
}
