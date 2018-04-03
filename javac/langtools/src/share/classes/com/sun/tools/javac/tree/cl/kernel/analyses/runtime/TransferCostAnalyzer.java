/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.runtime;

import com.sun.tools.javac.tree.cl.variables.ArrayVariable;
import com.sun.tools.javac.tree.cl.variables.NewArrayVariable;
import com.sun.tools.javac.tree.cl.variables.KernelVariable;
import com.sun.tools.javac.tree.cl.variables.ReturnValue;
import com.sun.tools.javac.tree.cl.variables.VariableAllocator;
import java.util.List;

/**
 *
 * @author Alexander Pöppl
 */
class TransferCostAnalyzer {

      
    private final VariableAllocator variableAllocator;
    private final CostModel model;
    
    TransferCostAnalyzer(final VariableAllocator variableAllocator, CostModel model) {
        this.variableAllocator = variableAllocator;
        this.model = model;
    }

    double getEstimatedCosts() {
        double toDeviceCosts = getCopyToDeviceCosts();
        double fromDeviceCosts = getCopyFromDeviceCosts();
        return toDeviceCosts + fromDeviceCosts;
    }

    private double getCopyToDeviceCosts() {
        List<KernelVariable> arrayTypeVars = this.variableAllocator.getArrays();
        double totalRuntime = 0.0;
        for (KernelVariable arrayVar : arrayTypeVars) {
            totalRuntime += getProjectedTransferTime(arrayVar);
        }
        return totalRuntime;
    }

    private double getProjectedTransferTime(final KernelVariable arrayVar) {
        int numberOfElements = 0;
        if (arrayVar instanceof ArrayVariable) {
            numberOfElements = ((ArrayVariable) arrayVar).getSize();
        } else if (arrayVar instanceof NewArrayVariable) {
            numberOfElements = ((NewArrayVariable) arrayVar).getSize();
        }
        return model.getToDeviceBaseCost() + (numberOfElements * model.getToDeviceElementCost());
    }

    private double getCopyFromDeviceCosts() {
        ReturnValue returnValue = variableAllocator.getReturnValue();
        int totalElements = returnValue.getReturnValueSize();
        return model.getFromDeviceBaseCost() + (totalElements * model.getFromDeviceElementCost());
    }
}