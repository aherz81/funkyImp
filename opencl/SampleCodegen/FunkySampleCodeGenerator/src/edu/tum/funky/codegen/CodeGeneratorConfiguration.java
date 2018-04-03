/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.tum.funky.codegen;

/**
 *
 * @author Alexander Pöppl
 */
public class CodeGeneratorConfiguration {
    /**
     * Allow memory accesses that are not constant or simple. (other than i.e a[123,3] or a[x,y])
     */
    public static final String ALLOW_COMPLEX_ACCESSES = "edu.tum.funky.codegen.allow-complex";
    
    /**
     * Allow floating point division. 
     */
    public static final String ALLOW_FLOAT_DIVISION = "edu.tum.funky.codegen.allow-float-div";
    
    /**
     * Maximum number of nodes in the randomly generated expression tree.
     */
    public static final String MAX_NUMBER_OF_NODES = "edu.tum.funky.codegen.max-nodes";
    
    /**
     * Minimum number of nodes in the randomly generated expression tree.
     */
    public static final String MIN_NUMBER_OF_NODES = "edu.tum.funky.codegen.min-nodes";
    
    /**
     * Number of examples to be generated.
     */
    public static final String NUMBER_OF_EXAMPLES = "edu.tum.funky.codegen.generated-examples";
    
    /**
     * Ouput path. Shouldn't be changed from the value currently in the repository.
     */
    public static final String OUTPUT_PATH = "edu.tum.funky.codegen.output-path";
    
    /**
     * Start index for sample names. Optionally set, this is the number for the first test case. May be used if more than one kind of test case is to be generated.
     */
    private static final String START_INDEX = "edu.tum.funky.codegen.start-index";
    
    private final ConfigurationBase configBase;
    
    public CodeGeneratorConfiguration(ConfigurationBase configBase) {
        this.configBase = configBase;
    }
    
    public boolean isComplexAccessAllowed() {
        return configBase.getBoolean(ALLOW_COMPLEX_ACCESSES).orElse(Boolean.TRUE);
    }
    
    public boolean isFloatDivisionAllowed() {
        return configBase.getBoolean(ALLOW_FLOAT_DIVISION).orElse(Boolean.TRUE);
    }
    
    public int getMaximumNumberOfNodes() {
        return configBase.getInteger(MAX_NUMBER_OF_NODES).filter(x -> x > 0).orElse(50);
    }
    
    public int getMinimumNumberOfNodes() {
        return configBase.getInteger(MIN_NUMBER_OF_NODES).filter(x -> x > 0).orElse(1);
    }
    
    public int getNumberOfExamples() {
        return configBase.getInteger(NUMBER_OF_EXAMPLES).orElse(100);
    }
    
    public int getStartIndex() {
        return configBase.getInteger(START_INDEX).orElse(0);
    }
    
    public String getOutputPath() throws NoSuchFieldException {
        return configBase.getString(OUTPUT_PATH).orElseThrow(() -> new NoSuchFieldException("OutputPath must be specified."));
    }
}
