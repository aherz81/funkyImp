/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.variables;

import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.cl.util.PrettyStringBuilder;

/**
 *
 * @author Alexander Pöppl
 */
public class KernelInitializer {
    
    private static final String SUB_FOLDER = "";
    
    private static int domainKernelCounter = 0;
    
    private final PrettyStringBuilder printer;
    private final LowerTreeImpl state;
    
    private String kernelName;
    
    public KernelInitializer(LowerTreeImpl state) {
        this.printer = new PrettyStringBuilder(state);
        this.state = state;
    }
    
    
    public String getKernelInitCode() {
        printer.empty();
        printer.append("ocl_kernel ").append(getKernelName())
                .append("(&device,\"").append(getKernelFileName())
                .append("\", \"-cl-opt-disable\");").appendNl();
        
        return printer.toString();
    }
    
    public String getKernelName() {
        if (kernelName == null) {
            kernelName = state.method.name + "_" + domainKernelCounter++;
        }
        return kernelName;
    }
    
    public String getKernelFileName() {
        return "tmp/" + getKernelName() + ".cl";
    }
}