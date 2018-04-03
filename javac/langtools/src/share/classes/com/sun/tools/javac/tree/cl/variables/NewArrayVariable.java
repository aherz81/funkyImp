/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.variables;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.cl.util.PrettyKernelStringBuilder;
import java.util.LinkedList;
import java.util.List;


/**
 *
 * @author Alexander Pöppl
 */
public class NewArrayVariable extends KernelVariable {

    private final JCTree.JCNewArray newArray;

    private int[] dimensionSizes;
    private String[] dimensions;


    public NewArrayVariable(JCTree.JCNewArray jcNewArray, LowerTreeImpl state) {
        super((JCTree.JCIdent)null, state);
        this.newArray = jcNewArray;
    }

    @Override
    public String getGeneratedName() {
        return null;
    }

    @Override
    public List<String> getParams() {
        List<String> params = new LinkedList<String>();

        for (String s : dimensions) {
            params.add("&" + s);
        }

        return params;
    }

    private void computeDimensions() {
        Type.ArrayType arrType = (Type.ArrayType) this.newArray.type;
        Type.DomainType dt = arrType.dom;

        dimensionSizes = dt.getSize(this.newArray.pos(), dt.appliedParams.toArray(new JCTree.JCExpression[0]));
        this.dimensions = new String[dimensionSizes.length];
        for (int i = 0; i < dimensions.length; i++) {
            String dimensionName = "__new_array_" + state.domainIterState.uid + "_dim_" + i;
            dimensions[i] = dimensionName;
        }
    }

    @Override
    public String getGeneratedType() {
        return null;
    }

    @Override
    public List<String> getDeclarations() {
        Type.ArrayType arrType = (Type.ArrayType) this.type;
        List<String> res = new LinkedList<String>();

        computeDimensions();

        //generate Variables for the dimensions
        for (int i = 0; i < this.dimensions.length; i++) {
            String indexVar = "int " + this.dimensions[i] + "=" + this.dimensionSizes[i] + ";";
            res.add(indexVar);
        }

        return res;
    }

    @Override
    public String getGPUCopyCode() {
        return null;
    }

    @Override
    public String getCleanupCode() {
        return null;
    }

    @Override
    public String getCLCParam() {
        PrettyKernelStringBuilder kernelPrinter = new PrettyKernelStringBuilder();
        for (String dimName : dimensions) {
            kernelPrinter.append("int ").append(dimName).append(", ");
        }
        return kernelPrinter.toString();
    }

    public int getSize() {
        int totalSize = 1;
        for (int dimSize : dimensionSizes) {
            totalSize *= dimSize;
        }
        return totalSize;
    }
}
