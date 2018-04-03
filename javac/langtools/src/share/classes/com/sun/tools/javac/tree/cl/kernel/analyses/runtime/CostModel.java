/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.kernel.analyses.runtime;

import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity.MultiplicityFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.time.RuntimeFunction;
import java.util.Map;

/**
 *
 * @author nax
 */
public interface CostModel {    
        static enum CostType {
        FLOAT_ADD,
        FLOAT_SUB,
        FLOAT_MUL,
        FLOAT_DIV,
        INT_ADD,
        INT_SUB,
        INT_MUL,
        INT_DIV,
        LOCAL_ACCESS,
        PRIVATE_ACCESS,
        GLOBAL_WRITE,
        CONSTANT_GLOBAL_READ,
        CACHED_GLOBAL_READ,
        GLOBAL_READ,
        IDENTICAL_GLOBAL_READ,
        COMPLEX_GLOBAL_READ,
        BASE_COST
    }
    
        
    
    public Map<CostType, RuntimeFunction> getRuntimeCosts();
    public Map<CostType, MultiplicityFunction> getMultiplicityFunction();
    
    public double getToDeviceElementCost();
    public double getToDeviceBaseCost();
    
    public double getFromDeviceElementCost();
    public double getFromDeviceBaseCost();
    
    public int getMaxWorkgroupSize();

    
}
