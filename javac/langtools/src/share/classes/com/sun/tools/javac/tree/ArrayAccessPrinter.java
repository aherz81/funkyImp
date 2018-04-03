/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.cl.util.PrettyKernelStringBuilder;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Alexander Herz, Alexander Pöppl
 */
public class ArrayAccessPrinter {

    private final LowerTreeImpl state;
    private final TreePrintDelegate parent;
    private final PrintDelegate printer;

    public ArrayAccessPrinter(LowerTreeImpl state, TreePrintDelegate parent, PrintDelegate printer) {
        this.state = state;
        this.printer = printer;
        this.parent = parent;
    }

    public List<JCTree.JCExpression> getAddressing(JCDiagnostic.DiagnosticPosition pos, JCTree domain, Type.ArrayType at, JCTree.JCExpression[] inds, boolean iter_inside_projection, boolean skip_inner_counter, boolean no_reinterprete) {
        Type.ArrayType pt = at;

        boolean reinterprete = (!at.treatAsBaseDomain()) && !no_reinterprete;//are we just looking at an x through 'glasses' like two_d.one_d

        List<JCTree.JCExpression> addressing = null;

        Map<String, Symbol.VarSymbol> map = new LinkedHashMap<String, Symbol.VarSymbol>(); //repace strings from codegen by varsym so state.index_map can be filled with varsyms

        state.index_map = new LinkedHashMap<Symbol.VarSymbol, JCTree.JCExpression>(); //values from codegen need to replaced by the used supplied index in vsistVarSymbol (sequential emitter)

        if (reinterprete) {
            if (at.isCast()) {//e.g. trace.one_d
                pt = at.getRealType();
            }

            if (!at.isCast()) {
                for (int i = 0; i < pt.dom.getInterVectorOrder(pos).size(); i++) {//inter iteration order
                    Symbol.VarSymbol vs = new Symbol.VarSymbol(0, state.names.fromString(pt.dom.getInterVectorOrder(pos).get(i)), state.jc.syms.intType, null);
                    vs.flags_field |= Flags.TASKLOCAL;
                    map.put(vs.name.toString(), vs);

                    state.index_map.put(vs, inds[i]);
                }
            } else {
                for (int c = 0; c < ((Type.DomainType) pt.dom.resultDom).projectionArgs.size(); c++) {//cast uses result dom pars
                    state.index_map.put(((Type.DomainType) pt.dom.resultDom).projectionArgs.get(c), inds[c]);
                    map.put(((Type.DomainType) pt.dom.resultDom).projectionArgs.get(c).name.toString(), ((Type.DomainType) pt.dom.resultDom).projectionArgs.get(c));
                }
            }

            if (at.isCast()) {
                addressing = pt.dom.accessCast;
            } else {
                addressing = pt.dom.accessElement;
            }

        } else {
            if (iter_inside_projection) {
                for (int i = 0; i < pt.dom.getInterVectorOrder(pos).size(); i++) {
                    Symbol.VarSymbol vs = new Symbol.VarSymbol(0, state.names.fromString(pt.dom.getInterVectorOrder(pos).get(i)), state.jc.syms.intType, null);
                    vs.flags_field |= Flags.TASKLOCAL;
                    map.put(vs.name.toString(), vs);

                    state.index_map.put(vs, inds[i]); //this mapping is used to replace the __RIs by the actual index value in visitVarSymbol in LowerSequential
                }
                addressing = pt.dom.accessElement;

            } else {
                for (int i = 0; i < pt.dom.projectionArgs.size(); i++) {
                    if (pt.dom.projectionArgs.get(i) != TreeInfo.symbol(inds[i])) {
                        state.index_map.put(pt.dom.projectionArgs.get(i), inds[i]); //this mapping is used to replace the __RIs by the actual index value in visitVarSymbol in LowerSequential
                    }
                    map.put(pt.dom.projectionArgs.get(i).name.toString(), pt.dom.projectionArgs.get(i));
                }
                addressing = pt.dom.accessProjection;
            }
        }

        Map<Symbol.VarSymbol, JCTree.JCExpression> mapConsts = new LinkedHashMap<Symbol.VarSymbol, JCTree.JCExpression>();

        for (int i = 0; i < pt.dom.appliedParams.size(); i++) {
            map.put(pt.dom.formalParams.get(i).name.toString(), pt.dom.formalParams.get(i));
            JCTree.JCExpression e = pt.dom.appliedParams.get(i);
            if (e.type.constValue() instanceof Integer && ((Integer) e.type.constValue()) > 0) {
                mapConsts.put(pt.dom.formalParams.get(i), e);
            }
        }

        if (!map.isEmpty()) { //state.index_map works only with the proper syms, so substitute them
            ListBuffer<JCTree.JCExpression> lb = new ListBuffer<JCTree.JCExpression>();
            for (JCTree.JCExpression e : addressing) {
                lb.add(replace(e, map, true));
            }
            addressing = lb.toList();
        }

        if (!mapConsts.isEmpty()) {
            ListBuffer<JCTree.JCExpression> lb = new ListBuffer<JCTree.JCExpression>();
            for (JCTree.JCExpression e : addressing) {
                lb.add((JCTree.JCExpression) JCTree.replace(e, mapConsts));
            }
            addressing = lb.toList();
        }
        return addressing;
    }

    public void generateDomainOffsetCode(JCDiagnostic.DiagnosticPosition pos, JCTree domain, Type.ArrayType at, JCTree.JCExpression[] inds, boolean iter_inside_projection, boolean skip_inner_counter, boolean no_reinterprete) throws IOException {

        //Map<Symbol.VarSymbol, JCTree.JCExpression> oldMap=state.index_map;
        List<JCTree.JCExpression> addressing = getAddressing(pos, domain, at, inds, iter_inside_projection, skip_inner_counter, no_reinterprete);

        Type.ArrayType pt = at;
        Type.ArrayType base = pt.getBaseType();

        int innerCounterDim = -1;
        if (skip_inner_counter) {
            innerCounterDim = base.dom.getDim() - 1; //last dim handled by a map
        }

        //addressing contains the actual memory access using the domain's iteration vars, like [a,a] for trace
        //state.index_map is used to substitute a by the actual index supplied by the user e.g. a->10
        int dimension = addressing.size();

        int actualDims = dimension;

        boolean dyn = pt.dom.isDynamic();// pt.getBaseType().dom.isDynamic() || base.dom.isDynamic();

        if (pt.getBaseType().dom.isDynamic() || base.dom.isDynamic()) {
            //must calc base from pt (type was casted from dyn to non-dyn so we cannot use base to calc offset)
            base = pt;//FIXME: do we need to calc pt.parentDomain??
        }

        if (innerCounterDim >= 0 && !dyn) {
            actualDims--;
        }

        if (actualDims == 0 || ((domain.type != null && domain.type.getArrayType() instanceof Type.ArrayType && ((Type.ArrayType) domain.type.getArrayType()).useIndirection()) && addressing.toString().equals("0")))//is there any offset?
        {
            printer.print("0");
            printConstTail();
            return;
        }

        if (dyn) { //calc offset dynamically (size not known statically)
            domain = JCTree.baseArray(domain);
            parent.printExpr(domain);
            //
            //if(((Type.ArrayType)domain.type.getArrayType()).treatAsBaseDomain())
            if ((domain.type != null && !((Type.ArrayType) domain.type.getArrayType()).useIndirection()) || (domain.type == null && !at.useIndirection())) {
                printer.print(".");
            } else {
                printer.print("->");
            }

            printer.print("getOffsetDim(");
        }

        //iter_inside_projection: true -> normal access inside object
        //false -> access outer object
        boolean first = true;

        int baseSize[] = null;
        if (!dyn) { //static size known?
            baseSize = base.dom.getSize(pos, base.dom.appliedParams.toArray(new JCTree.JCExpression[0]));
        }

        //translate access like [a,a] into a flat memory access like [a*width+a]
//        for (int c = 0; c < dimension; c++) {
        for (int c = dimension - 1; c >= 0; c--) {

            if (c != innerCounterDim) {
                long stride = 1;
                if (!dyn) {
                    Type.ArrayType bt=pt;
                    if(at.isCast())
                        bt=at.getRealType(); //with a cast, the dimension of the cast source must be used (e.g. 2d matrix) not target (e.g. row)
                    
                    stride = bt.dom.getDimStride(c, baseSize);
                }

                if (!first) {
                    if (!dyn) {
                        printer.print(" + "); //no static offsets for dyn access
                    } else {
                        printer.print(", ");
                    }
                }

                if (stride != 0) {
                    printer.print("(");
                    parent.printExpr(addressing.get(c));//get dynamic index from cloog's code
                    printer.print(")");

                    if (stride != 1) {
                        printer.print(" * " + stride);
                        printConstTail();//add "L" on 64bit systems
                    }
                } else {
                    printer.print("0");
                    printConstTail();
                }

                first = false;
            } else if (dyn) {
                if (!first) {
                    if (!dyn) {
                        printer.print(" + "); //no static offsets for dyn access
                    } else {
                        printer.print(", ");
                    }
                }
                printer.print("0");
                printConstTail();
                first = false;
            }
        }

        if (dyn) {
            printer.print(")");
        }

        //state.index_map=oldMap;
    }

    //replace indices for dom iter -x
    <T extends JCTree> T replace(T cu, final Map<String, Symbol.VarSymbol> map, boolean deepcopy) {
        DomainIterationReplaceTreeScanner v = new DomainIterationReplaceTreeScanner(map);
        if (deepcopy) {
            cu = (T) state.copy.copy(cu);
        }
        v.scan(cu);
        return cu;
    }

    private void printConstTail() throws IOException {
        if (state.target.regSize > 8) {
            printer.print("L");
        }
    }

    @Override
    public String toString() {
        return this.printer.toString();
    }
}
