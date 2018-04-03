/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.kernel;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import com.sun.tools.javac.tree.cl.util.PrettyKernelStringBuilder;
import com.sun.tools.javac.tree.cl.variables.PrimitiveVariable;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Pair;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 *
 * @author Alexander Pöppl
 */
class DomainArgumentReTranslator {

    private final VariableEnvironment environment;
    private final LowerTreeImpl state;
    private final PrettyKernelStringBuilder printer;
    private final String currentPositionVarName;
    private final Set<String> neededVariables;

    public DomainArgumentReTranslator(LowerTreeImpl state, VariableEnvironment environment, PrettyKernelStringBuilder printer, String currentPositionVarName, Set<String> neededVariables) {
        this.state = state;
        this.environment = environment;
        this.printer = printer;
        this.currentPositionVarName = currentPositionVarName;
        this.neededVariables = neededVariables;
    }

    public void generateDomainArgumentCode() {
        generateCurrentPositionVarCode();
        setupDomainRecalculation();
    }

    private void generateCurrentPositionVarCode() {
        printer.append("size_t ").append(currentPositionVarName).append("=get_global_id(0);").appendNl();
    }

    private void setupDomainRecalculation() {
        JCTree.JCDomainIter currentDomainIteration = state.domainIterState.iter;
        Type.ArrayType originalType = (Type.ArrayType) currentDomainIteration.exp.type;
        List<JCTree.JCVariableDecl> domargs = currentDomainIteration.domargs;
        printDomainArgumentReinit(state.domainIterState.iter.pos(), state.domainIterState.iter, originalType, false, true);
    }

    private void printDomainArgumentReinit(JCDiagnostic.DiagnosticPosition pos, JCTree domain, Type.ArrayType at, boolean isIteratingInAProjection, boolean doNotReinterprete) {
        Type.ArrayType pt = at;
        Type.ArrayType base = pt.getBaseType();
        List<Pair<String, JCTree.JCVariableDecl>> domainArguments = environment.getDomainArguments();
        com.sun.tools.javac.util.List<Symbol.VarSymbol> indices = pt.dom.getIndexList(new LinkedHashMap<Symbol.VarSymbol, Symbol.VarSymbol>());
        int[] baseSize = base.dom.getSize(pos, base.dom.appliedParams.toArray(new JCTree.JCExpression[0]));

        boolean reinterprete = (!at.treatAsBaseDomain()) && !doNotReinterprete; //are we just looking at an x through 'glasses'?
        boolean dyn = pt.dom.isDynamic() || base.dom.isDynamic();
        if (reinterprete) {
            throw new RuntimeException("No reinterprete yet.");
        } else if (dyn) {
            throw new RuntimeException("No dynamic Arrays yet.");
        }

        for (int c = 0; c < domainArguments.size(); c++) {
            String domArgName = domainArguments.get(c).fst;
            if (this.neededVariables.contains(domArgName)) {
                //Only emit code when it is actually needed.
                printer.append("unsigned ").append(environment.getTypeForVariable(domArgName)).append(" ")
                        .append(environment.getNameForVariable(domArgName)).append("=");
                
                //when not reinterpreting a domain but accessing a domain by projection we must lookup the dim
                long stride = pt.dom.getDimStride(c, baseSize);

                printer.append("(").append(currentPositionVarName);
                if (stride != 1) {
                    printer.append("/").append(Long.toString(stride));
                }
                printer.append(")");
                printer.append("%").append(baseSize[c]).append(";").appendNl();
            }
        }
    }
}
