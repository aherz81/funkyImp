/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions;

import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.CostModel;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.CostModel.CostType;
import static com.sun.tools.javac.tree.cl.kernel.analyses.runtime.CostModel.CostType.*;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity.AlwaysUseSingleTimeMultiplicityFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity.HyperboleMultiplicityFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity.MultiplicityFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity.NoInfluenceMultiplicityFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.multiplicity.ShadowedMultiplicityFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.time.LinearRuntimeFunction;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions.time.RuntimeFunction;
import java.util.EnumMap;
import java.util.Map;

/**
 *
 * @author Alexander Pöppl
 */
public class GT650CostModel implements CostModel {

    private final double toDeviceBaseCost = 333.943e-6;
    private final double toDeviceElementCost = 0.00220627e-6;
    private final double fromDeviceBaseCost = 159.372e-6;
    private final double fromDeviceElementCost = 0.0013795e-6;
    
    public Map<CostType, RuntimeFunction> costFunctions;
    public Map<CostType, MultiplicityFunction> multiplicityModifiers;
    
    public GT650CostModel() {
        this.costFunctions = new EnumMap<CostModel.CostType, RuntimeFunction>(CostModel.CostType.class);
        this.costFunctions.put(FLOAT_ADD, new LinearRuntimeFunction(3.2679908306701e-06, 0));
        this.costFunctions.put(FLOAT_SUB, new LinearRuntimeFunction(4.42040597789466e-06, 0));
        this.costFunctions.put(FLOAT_MUL, new LinearRuntimeFunction(4.44336371720927e-06, 0));
        this.costFunctions.put(FLOAT_DIV, new LinearRuntimeFunction(0.000586512010350116, 0));
        this.costFunctions.put(INT_ADD, new LinearRuntimeFunction(3.28593145810764e-06, 0));
        this.costFunctions.put(INT_SUB, new LinearRuntimeFunction(6.1655964896141e-06, 0));
        this.costFunctions.put(INT_MUL, new LinearRuntimeFunction(4.83050202348605e-06, 0));
        this.costFunctions.put(INT_DIV, new LinearRuntimeFunction(8.97819795983561e-05, 0));
        this.costFunctions.put(PRIVATE_ACCESS, new LinearRuntimeFunction(0, 0));
        this.costFunctions.put(LOCAL_ACCESS, new LinearRuntimeFunction(0.0001038783725, 0));
        this.costFunctions.put(BASE_COST, new LinearRuntimeFunction(0.000189885660433182, 5.72651881044402));
        this.costFunctions.put(GLOBAL_WRITE, new LinearRuntimeFunction(0.000253299033257269, 0));
        this.costFunctions.put(IDENTICAL_GLOBAL_READ, new LinearRuntimeFunction(0, 0));
        this.costFunctions.put(CONSTANT_GLOBAL_READ, new LinearRuntimeFunction(4.59504E-05, 0));
        this.costFunctions.put(CACHED_GLOBAL_READ, new LinearRuntimeFunction(4.59504E-05, 0));
        this.costFunctions.put(GLOBAL_READ, new LinearRuntimeFunction(9.941839271e-05, 0));
        this.costFunctions.put(COMPLEX_GLOBAL_READ, new LinearRuntimeFunction(0.00352022822511511, 3));

        this.multiplicityModifiers = new EnumMap<CostModel.CostType, MultiplicityFunction>(CostModel.CostType.class);
        this.multiplicityModifiers.put(INT_ADD, new AlwaysUseSingleTimeMultiplicityFunction());
        this.multiplicityModifiers.put(INT_SUB, new AlwaysUseSingleTimeMultiplicityFunction());
        this.multiplicityModifiers.put(INT_MUL, new AlwaysUseSingleTimeMultiplicityFunction());
        this.multiplicityModifiers.put(INT_DIV, new AlwaysUseSingleTimeMultiplicityFunction());
        this.multiplicityModifiers.put(FLOAT_ADD, new ShadowedMultiplicityFunction(12));
        this.multiplicityModifiers.put(FLOAT_SUB, new ShadowedMultiplicityFunction(12));
        this.multiplicityModifiers.put(FLOAT_MUL, new ShadowedMultiplicityFunction(12));
        this.multiplicityModifiers.put(FLOAT_DIV, new AlwaysUseSingleTimeMultiplicityFunction());
        this.multiplicityModifiers.put(PRIVATE_ACCESS, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(LOCAL_ACCESS, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(GLOBAL_WRITE, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(IDENTICAL_GLOBAL_READ, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(CONSTANT_GLOBAL_READ, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(CACHED_GLOBAL_READ, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(GLOBAL_READ, new HyperboleMultiplicityFunction(1, 0.4));
        this.multiplicityModifiers.put(COMPLEX_GLOBAL_READ, new HyperboleMultiplicityFunction(1, 0.25));
        this.multiplicityModifiers.put(BASE_COST, new NoInfluenceMultiplicityFunction());
    }

    public Map<CostType, RuntimeFunction> getRuntimeCosts() {
        return costFunctions;
    }

    public Map<CostType, MultiplicityFunction> getMultiplicityFunction() {
        return multiplicityModifiers;
    }

    public double getToDeviceElementCost() {
        return toDeviceElementCost; 
    }

    public double getToDeviceBaseCost() {
        return toDeviceBaseCost;
    }

    public double getFromDeviceElementCost() {
        return fromDeviceElementCost;
    }

    public double getFromDeviceBaseCost() {
        return fromDeviceBaseCost;
    }
    
    public int getMaxWorkgroupSize() {
        return 1024;
    }
    
    public int getNumberOfExecutionUnits() {
        return 384;
    }
}