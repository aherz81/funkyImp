/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.tum.funky.qc;

/**
 *
 * @author Alexander Pöppl
 */
public class QualityCheckConfiguration {
    
    public static enum AveragingType {
        DISTANCE, RATIO
    }

    private static final String AVERAGING_TYPE = "edu.tum.funky.qc.averaging";
    
    private final ConfigurationBase configBase;
    
    public QualityCheckConfiguration(ConfigurationBase configBase) {
        this.configBase = configBase;
    }
    
    public AveragingType getAveragingType() {
        return AveragingType.valueOf(configBase.getString(AVERAGING_TYPE).orElseThrow(() -> new IllegalArgumentException("Averaging type must be specified.")));
    }
   
}
