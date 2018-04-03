/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.runtime;

import com.sun.tools.javac.tree.cl.variables.VariableAllocator;

/**
 *
 * @author Alexander Pöppl
 */
class DegreeOfParallelismCostAnalyzer {
    
    private final int maxWorkgroupSize;
    private final int workGroupSize;
    private final CostModel model;
    
    DegreeOfParallelismCostAnalyzer(VariableAllocator varManager, CostModel model) {
        this.workGroupSize = getWorkGroupSize(varManager.getReturnValue().getReturnValueSize());
        this.model = model;
        this.maxWorkgroupSize = model.getMaxWorkgroupSize();
        
        
    }

    public double getWorkSizeMultiplier() {
        return 2;//13.04 * Math.exp(-workGroupSize / 12.211124) + 26.102 * Math.exp(-workGroupSize / 2.28329) + 1;
    }
    
    private int getWorkGroupSize(int numberOfWorkItems) {
        for (int i = maxWorkgroupSize; i > 0; i--) {
            if (numberOfWorkItems % i == 0) {
                return i;
            }
        }
        return 1;
    }

}
