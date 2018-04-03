/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.variables;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.cl.util.PrettyKernelStringBuilder;
import java.util.List;
import java.util.LinkedList;

/**
 *
 * @author Alexander Pöppl
 */
public class ArrayVariable extends KernelVariable {
    private String[] dimensions;
    private int[] dimensionSizes;
    private int totalSize;

    private String nameOnHost;
    private String nameOnGPU;

    public ArrayVariable(JCTree.JCIdent capturedIdentifier, LowerTreeImpl state) {
        super(capturedIdentifier, state);

    }

    @Override
    public String getGeneratedType() {
        Type.ArrayType arrType = (Type.ArrayType) this.type;
        return arrType.getComponentType().toString() + " *";
    }

    public String getGeneratedGPUName() {
        if (nameOnGPU == null) {
            nameOnGPU = "__" + getOriginalName() + "_GPU_" +this.state.domainIterState.uid;
        }
        return this.nameOnGPU;
    }

    @Override
    public List<String> getDeclarations() {
        Type.ArrayType arrType = (Type.ArrayType) this.type;
        List<String> res = new LinkedList<String>();

        computeDimensions();

        //Allocate memory locally and on the GPU
        String hostVar = getGeneratedType() + getGeneratedName() + "=" + getOriginalName() + "->toNative();";
        String gpuVar = "ocl_mem " + getGeneratedGPUName() + "=device.malloc(sizeof(" + arrType.getComponentType() + ")*" + totalSize + ",CL_MEM_READ_WRITE);";

        res.add(hostVar);
        res.add(gpuVar);

        //generate Variables for the dimensions
        for (int i = 0; i < this.dimensions.length; i++) {
            String indexVar = "int " + this.dimensions[i] + "=" + this.dimensionSizes[i] + ";";
            res.add(indexVar);
        }

        return res;
    }

    @Override
    public String getGPUCopyCode() {
        return getGeneratedGPUName()+ ".copyFrom("+getGeneratedName()+");";
    }

    @Override
    public List<String> getParams() {
        List<String> res = new LinkedList<String>();
        res.add(getGeneratedGPUName() + ".mem()");
        for (String dimensionName : dimensions) {
            res.add("&" + dimensionName);
        }
        return res;
    }

    @Override
    public String getCleanupCode() {
        return getGeneratedGPUName() + ".free();";
    }

    private void computeDimensions() {
        Type.ArrayType arrType = (Type.ArrayType) this.type;
        Type.DomainType dt = arrType.dom;

        dimensionSizes = dt.getSize(this.capturedIdentifier.pos(), dt.appliedParams.toArray(new JCTree.JCExpression[0]));
        this.totalSize = 1;
        for (int i = 0; i < dimensionSizes.length; i++) {
            totalSize *= dimensionSizes[i];
        }

        this.dimensions = new String[dimensionSizes.length];
        for (int i = 0; i < dimensions.length; i++) {
            String dimensionName = "__" + getOriginalName() + state.domainIterState.uid + "_dim_" + i;
            dimensions[i] = dimensionName;
        }
    }

    @Override
    public String getCLCParam() {
        PrettyKernelStringBuilder kernelPrinter = new PrettyKernelStringBuilder();
        kernelPrinter.append("__global ").append(getGeneratedType()).append(getGeneratedName()).append(", ");
        for (String dimName : dimensions) {
            kernelPrinter.append("int ").append(dimName).append(", ");
        }
        return kernelPrinter.toString();
    }


    public int getSize() {
        return totalSize;
    }
}
