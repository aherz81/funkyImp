/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.variables;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.cl.util.PrettyStringBuilder;

/**
 *
 * @author nax
 */
public class ReturnValue {

    private final LowerTreeImpl state;
    private final PrettyStringBuilder printer;
    private String returnType;
    private String returnValOnHost;
    private String returnValOnGPU;
    private int returnValSize;
    private int[] returnValDimSizes;

    public ReturnValue(LowerTreeImpl state, PrettyStringBuilder printer) {
        this.state = state;
        this.printer = printer;
    }

    public void handleReturnSpace(String lambdaReturnType) {
        Type.ArrayType iterationType = ((Type.ArrayType) state.domainIterState.iter.iterType).getRealType();
        Type.DomainType domainType = iterationType.dom;
        returnValDimSizes = domainType.getSize(state.domainIterState.iter.pos(), domainType.appliedParams.toArray(new JCTree.JCExpression[0]));
        returnValSize = 1;
        for (int i = 0; i < returnValDimSizes.length; i++) {
            returnValSize *= returnValDimSizes[i];
        }
        this.returnValOnHost = "__return_val_" + state.domainIterState.uid;
        this.returnValOnGPU = "__return_val_GPU_" + state.domainIterState.uid;
        
        
        //printer.append("__return_" + state.domainIterState.uid);
        
        Type.ArrayType domainIterReturnType = (Type.ArrayType) state.domainIterState.iter.iterType;
        String lambdaReturnWithoutStar = lambdaReturnType;
        String componentReturnType = domainIterReturnType.getComponentType().toString();
        if (lambdaReturnWithoutStar.endsWith("*")) {
            lambdaReturnWithoutStar = lambdaReturnWithoutStar.substring(0, lambdaReturnWithoutStar.length() - 1);
        }
        printer.append("funky::LinearArray<").append(componentReturnType).append(">")
                .append(" *").append(getReturnValueIntermediateName()).append("=new ").append("funky::LinearArray<").append(componentReturnType).append(">(")
                .append(this.returnValSize).append(",").append("true);").appendNl();

        printer.append(lambdaReturnType).append(" ").append(this.getReturnValueName());

        printer.append("=new ").append(lambdaReturnWithoutStar).append("(").append(getReturnValueIntermediateName())
                .append(",").append(this.returnValDimSizes.length).append(",");

        for (int i = 0; i < this.returnValDimSizes.length; i++) {
            printer.append(this.returnValDimSizes[i]);
            if (i < this.returnValDimSizes.length - 1) {
                printer.append(",");
            } else {
                printer.append(");").appendNl();
            }
        }

        //Allocate Local Memory on the host.
        this.returnType = iterationType.getComponentType().toString() + " *";
        printer.append(returnType).append(this.returnValOnHost);
        printer.append("=").append("__return_" + state.domainIterState.uid).append("->toNative();");
        printer.appendNl();        

        //Memory on the GPU.
        printer.append("ocl_mem ").append(this.returnValOnGPU);
        printer.append("=device.malloc(").append("sizeof(").append(iterationType.getComponentType()).append(")*").append(returnValSize).append(",CL_MEM_WRITE_ONLY);");
        printer.appendNl();
    }

    public String getParam() {
        return returnValOnGPU + ".mem()";
    }

    public String handleReturnValueRetrieval(String lambdaReturnType) {
        this.printer.empty();
/*        
        Type.ArrayType domainIterReturnType = (Type.ArrayType) state.domainIterState.iter.iterType;

        String lambdaReturnWithoutStar = lambdaReturnType;
        String componentReturnType = domainIterReturnType.getComponentType().toString();
        if (lambdaReturnWithoutStar.endsWith("*")) {
            lambdaReturnWithoutStar = lambdaReturnWithoutStar.substring(0, lambdaReturnWithoutStar.length() - 1);
        }
*/        

        printer.append(this.returnValOnGPU).append(".copyTo(").append(this.returnValOnHost).append(");").appendNl();


/*
        printer.append("funky::LinearArray<").append(componentReturnType).append(">")
                .append(" *").append(getReturnValueIntermediateName()).append("=new ").append("funky::LinearArray<").append(componentReturnType).append(">(")
                .append(this.returnValSize).append(",").append(this.returnValOnHost).append(", true);").appendNl();

        printer.append(lambdaReturnType).append(" ").append(this.getReturnValueName());

        printer.append("=new ").append(lambdaReturnWithoutStar).append("(").append(getReturnValueIntermediateName())
                .append(",").append(this.returnValDimSizes.length).append(",");

        for (int i = 0; i < this.returnValDimSizes.length; i++) {
            printer.append(this.returnValDimSizes[i]);
            if (i < this.returnValDimSizes.length - 1) {
                printer.append(",");
            } else {
                printer.append(");").appendNl();
            }
        }
*/

        return this.printer.toString();
    }

    public void handleCleanup() {
        printer.append(this.returnValOnGPU).append(".free();").appendNl();
    }

    public String getReturnValueName() {
        return "__return_" + this.state.domainIterState.uid;
    }

    public int getReturnValueSize() {
        return this.returnValSize;
    }

    private String getReturnValueIntermediateName() {
        return "__return_LINARR" + this.state.domainIterState.uid;
    }

    public String getCLCParam() {
        return "__global " + returnType + getReturnValueName();
    }
}
