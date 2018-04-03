/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.runtime.functions;

import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.CostModel;
import static com.sun.tools.javac.tree.cl.kernel.analyses.runtime.CostModel.CostType.*;
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
public class K4000CostModel implements CostModel {
    
    public Map<CostType, RuntimeFunction> costFunctions;
    public Map<CostType, MultiplicityFunction> multiplicityModifiers;
    
    public final double toDeviceBaseCost = 429.56275 * 1e-6;
    public final double toDeviceElementCost = 0.000611096052562489 *1e-6;
    
    public final double fromDeviceBaseCost = 279.685 * 1e-6;
    public final double fromDeviceElementCost = 0.000219903658418094 * 1e-6;
    
    public K4000CostModel() {
        this.costFunctions = new EnumMap<CostModel.CostType, RuntimeFunction>(CostModel.CostType.class);
        this.costFunctions.put(FLOAT_ADD, new LinearRuntimeFunction(1.90856859662692e-06, 0));
        this.costFunctions.put(FLOAT_SUB, new LinearRuntimeFunction(1.90671708733714e-06, 0));
        this.costFunctions.put(FLOAT_MUL, new LinearRuntimeFunction(1.90114284589046e-06, 0));
        this.costFunctions.put(FLOAT_DIV, new LinearRuntimeFunction(5.25925556166454e-07, 0));

        this.costFunctions.put(INT_ADD, new LinearRuntimeFunction(1.89898704147485e-06, 0));
        this.costFunctions.put(INT_SUB, new LinearRuntimeFunction(1.90782598196872e-06, 0));
        this.costFunctions.put(INT_MUL, new LinearRuntimeFunction(1.82993720231424e-06, 0));
        this.costFunctions.put(INT_DIV, new LinearRuntimeFunction(4.6591868502335e-05, 0));
 
        this.costFunctions.put(PRIVATE_ACCESS, new LinearRuntimeFunction(0, 0));
        this.costFunctions.put(LOCAL_ACCESS, new LinearRuntimeFunction(7.46866667921024e-06, 0));
        this.costFunctions.put(BASE_COST, new LinearRuntimeFunction(3.27814412125736e-05, 7.2950637426182));
        this.costFunctions.put(GLOBAL_WRITE, new LinearRuntimeFunction(3.62895900495036e-05, 0));
        this.costFunctions.put(IDENTICAL_GLOBAL_READ, new LinearRuntimeFunction(1.09390209390148e-05, 0));
        
        this.costFunctions.put(CONSTANT_GLOBAL_READ, new LinearRuntimeFunction(1.09390209390148e-05, 0));
        this.costFunctions.put(CACHED_GLOBAL_READ, new LinearRuntimeFunction(4.62774888066786e-05, 0));
        this.costFunctions.put(GLOBAL_READ, new LinearRuntimeFunction(5.58154311269801e-05 , 0));
        this.costFunctions.put(COMPLEX_GLOBAL_READ, new LinearRuntimeFunction(0.0013770901047663, 0));

        this.multiplicityModifiers = new EnumMap<CostModel.CostType, MultiplicityFunction>(CostModel.CostType.class);
        this.multiplicityModifiers.put(INT_ADD, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(INT_SUB, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(INT_MUL, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(INT_DIV, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(FLOAT_ADD, new ShadowedMultiplicityFunction(2));
        this.multiplicityModifiers.put(FLOAT_SUB, new ShadowedMultiplicityFunction(2));
        this.multiplicityModifiers.put(FLOAT_MUL, new ShadowedMultiplicityFunction(2));
        this.multiplicityModifiers.put(FLOAT_DIV, new ShadowedMultiplicityFunction(2));
        this.multiplicityModifiers.put(PRIVATE_ACCESS, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(LOCAL_ACCESS, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(GLOBAL_WRITE, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(CONSTANT_GLOBAL_READ, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(CACHED_GLOBAL_READ, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(GLOBAL_READ, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(IDENTICAL_GLOBAL_READ, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(COMPLEX_GLOBAL_READ, new NoInfluenceMultiplicityFunction());
        this.multiplicityModifiers.put(BASE_COST, new NoInfluenceMultiplicityFunction());
    }

    public Map<CostType, RuntimeFunction> getRuntimeCosts() {
        return costFunctions;
    }

    public Map<CostType, MultiplicityFunction> getMultiplicityFunction() {
        return multiplicityModifiers;
    }

    public double getToDeviceElementCost() {
        return toDeviceElementCost; //To change body of generated methods, choose Tools | Templates.
    }

    public double getToDeviceBaseCost() {
        return toDeviceBaseCost; //To change body of generated methods, choose Tools | Templates.
    }

    public double getFromDeviceElementCost() {
        return fromDeviceElementCost; //To change body of generated methods, choose Tools | Templates.
    }

    public double getFromDeviceBaseCost() {
        return fromDeviceBaseCost; //To change body of generated methods, choose Tools | Templates.
    }
    
        public int getMaxWorkgroupSize() {
        return 1024;
    }
    
    public int getNumberOfExecutionUnits() {
        return -1;
    }
}
