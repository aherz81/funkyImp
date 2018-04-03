/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.kernel.analyses.runtime;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.cl.variables.VariableAllocator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Pair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


/**
 *
 * @author Alexander Pöppl
 */
public class MemoryAccessComplexityAnalyzer extends TreeScanner {

    public static enum MemoryReadType {
        LOCAL_ACCESS, CONSTANT_READ, INTERVAL_READ, CONTINUOUS_READ, COMPLEX_READ
        
    }
    
    public static int APPROXIMATE_CACHE_SIZE = 12*1024;
    
    private final boolean useSimpleModel;
    private final java.util.List<Set<String>> usedIdentifiers;
    private final List<JCExpression> memoryAddressing;
    private final JCExpression indexedExpression;
    private final VariableAllocator varManager;
    private final int[] dimensions;
    private final java.util.List<Pair<String, JCTree.JCVariableDecl>> identifiers;
    
    private MemoryReadType readComplexityClass;
    private int numberOfNodes;
    private int currentNode = 0;
    
    public MemoryAccessComplexityAnalyzer(JCTree.JCArrayAccess memoryAddressing,  VariableAllocator varManager) {
        this.memoryAddressing = memoryAddressing.index;
        this.indexedExpression = memoryAddressing.indexed;
        this.numberOfNodes = 0;
        this.usedIdentifiers = new ArrayList<Set<String>>();
        Type.DomainType dt = ((Type.ArrayType)memoryAddressing.indexed.type.getArrayType()).dom;
        this.dimensions = dt.getSize(memoryAddressing.pos(), dt.appliedParams.toArray(new JCTree.JCExpression[0]));
        this.varManager = varManager;
        this.useSimpleModel = JavaCompiler.getCompiler().useSimpleMemoryModel;
        dt.getResultParams();
        identifiers = varManager.getDomainArgumentNames();
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation tree) {
        super.visitApply(tree);
        readComplexityClass = MemoryReadType.COMPLEX_READ;
        numberOfNodes++;
    }

    @Override
    public void visitIndexed(JCTree.JCArrayAccess tree) {
        super.visitIndexed(tree);
        readComplexityClass = MemoryReadType.COMPLEX_READ;
        numberOfNodes++;
    }

    @Override
    public void visitBinary(JCTree.JCBinary tree) {
        super.visitBinary(tree);
        numberOfNodes++;
    }

    @Override
    public void visitLiteral(JCTree.JCLiteral tree) {
        super.visitLiteral(tree);
        numberOfNodes++;
    }

    @Override
    public void visitIdent(JCTree.JCIdent tree) {
        super.visitIdent(tree);
        numberOfNodes++;
        this.usedIdentifiers.get(currentNode).add(tree.name.toString());
    }

    @Override
    public void visitUnary(JCTree.JCUnary tree) {
        super.visitUnary(tree);
        numberOfNodes++;
    }
           
    public MemoryReadType computeMemoryReadComplexity() {
        //Hack to enable evaluation of Local Memory.
        JavaCompiler jc = JavaCompiler.getCompiler();
        if (jc.useLocalOpenCLHack && this.indexedExpression.toString().equals(jc.localIdentifier)) {
            return MemoryReadType.LOCAL_ACCESS;
        }
        
        for (JCExpression indexEpression : memoryAddressing) {
            usedIdentifiers.add(new HashSet<String>());
            scan(indexEpression);
            currentNode++;
        }
        int memoryReadSpread = computeMemoryEntropy();
        if (readComplexityClass == MemoryReadType.COMPLEX_READ) {
            return MemoryReadType.COMPLEX_READ;
        } else if (memoryReadSpread == 1 && !useSimpleModel) {
            return MemoryReadType.CONSTANT_READ;
        } else if (memoryReadSpread <= APPROXIMATE_CACHE_SIZE && !useSimpleModel) {
            return MemoryReadType.INTERVAL_READ;
        } else {
            return MemoryReadType.CONTINUOUS_READ;
        }
    }
    
    private int computeMemoryEntropy() {
        int currentDimension = 0;
        int accessIntervalSize = 1;
        for (Set<String> identifierSet : this.usedIdentifiers) {
            if (!identifierSet.isEmpty()) {
                accessIntervalSize *= dimensions[currentDimension];
                for (int i = currentDimension + 1; i < identifiers.size(); i++) {
                    String identifierOfDim = identifiers.get(i).fst;
                    if (identifierSet.contains(identifierOfDim)) {
                        readComplexityClass = MemoryReadType.COMPLEX_READ;
                    }
                }
            }
            currentDimension++;
        }
        return accessIntervalSize;
    }
}
     