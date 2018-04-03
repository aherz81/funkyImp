/*
 * Copyright 2011-2012 TU-München
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 */
package com.sun.tools.javac.tree;

import com.sun.tools.javac.tree.cl.util.KernelFileOutput;
import com.sun.tools.javac.tree.cl.kernel.KernelCodeGenerator;
import com.sun.tools.javac.tree.cl.variables.KernelInitializer;
import com.sun.tools.javac.tree.cl.variables.VariableAllocator;
import java.io.*;
import java.util.*;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.code.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.cl.kernel.analyses.runtime.RuntimeAnalysis;
import com.sun.tools.javac.tree.cl.util.PrettyStringBuilder;
import com.sun.tools.javac.tree.cl.util.ProfilingCodeGenerator;
import com.sun.tools.javac.tree.cl.variables.KernelVariable;

import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Prints out a tree as an indented Java source program.
 *
 * <p><b>This is NOT part of any API supported by Sun Microsystems. If you write
 * code that depends on this, you do so at your own risk. This code and its
 * internal interfaces are subject to change or deletion without notice.</b>
 */
/** 
 * OpenCL Data Parallel Code Emitter. Highly experimental. Use at own risk. By A. Poeppl
 * emitter for data parallel domain stuff
*/
public class LowerDataOCL extends LowerDataRT implements PrintDelegate, TreePrintDelegate {

    boolean inside_joining = false; //inside statement that contributes to finally?

// ------------------- actual code emitter ---------------
    public LowerDataOCL(LowerTreeImpl state) {
        super(state);
    }

    //code gen for iteration over domains...
    public void visitDomIter(JCDomainIter tree) {

        LowerTreeImpl.IterState oldIterState = state.domainIterState.clone();

        state.domainIterState = new LowerTreeImpl.IterState(state.nestedIterations.size());

        state.nestedIterations.push(state.domainIterState);

        //setup domain iter state...
        state.domainIterState.iter = tree;
        state.domainIterState.enclosing = state.current_tree;

        Type.ArrayType at = (Type.ArrayType) tree.exp.type.getArrayType();

        Type.DomainType dt = ((Type.DomainType) tree.sym.type.clone());


        state.domainIterState.reduce = dt.tsym.name.toString().equals("reduce");

        if (!state.domainIterState.reduce && tree.params != null) {
            dt.appliedParams = tree.params;
        } else {
            dt.appliedParams = at.dom.appliedParams;
        }

        Type.ArrayType dat = (Type.ArrayType) at.clone();
        dat.dom = dt;
        state.domainIterState.domain = dat;

        List<JCVariableDecl> cur = tree.domargs;

        if (state.domainIterState.reduce) {
            ListBuffer<JCVariableDecl> buf = new ListBuffer<JCVariableDecl>();

            while (cur.tail.size() > 0) {
                buf.add(cur.head);
                cur = cur.tail;
            }

            cur = buf.toList();

            Type.ArrayType rt = at.getRealType();
            if ((at.dom.isBaseDomain && at.dom.getDim() != rt.dom.getDim())) {
                dt = at.dom;
            } else {
                dt = at.getRealType().dom;
            }
        }

        int flag = Type.ArrayType.getCodeGenFlags(tree,at);

        //call barvinok/cloog to get code that iterates over/inside domain/projection
        state.domainIterState.code = dt.codegen(tree.pos(), dt.appliedParams.toArray(new JCTree.JCExpression[0]), cur, flag,true,false,false);
        Type.ArrayType rt = at.getRealType(); //will the real iteration pls stand up, if dom is reinterpreted, then rt!=at

        try {
            if (tree.body != null && tree.body.getTag() == JCTree.APPLY) {
                MethodSymbol s = (MethodSymbol) TreeInfo.symbol(tree.body);
                if ((s.flags_field & Flags.ACYCLIC) != 0 && s.getReturnType() != null && s.getReturnType().tag == TypeTags.ARRAY) {
                    state.domainIterState.forwardCall = s;
                }
            }

            state.domainIterState.used = JCTree.usedVars(tree.body);

            if (!at.dom.isBaseDomain) {
                int count = 0;
                if (dt.projectionArgs != null) {
                    for (VarSymbol vs : dt.projectionArgs) {
                        count++;
                        state.domainIterState.used.remove(vs);
                    }
                }
            }

            Map<String, VarSymbol> map = new LinkedHashMap<String, VarSymbol>();

            for (VarSymbol vs : state.domainIterState.used) {
                map.put(vs.toString(), vs);
            }

            if (dt.projectionArgs != null) {
                for (VarSymbol vs : dt.projectionArgs) {
                    map.put(vs.toString(), vs);
                }
            }

            //replace def vars be used vars
            state.domainIterState.code = replace(state.domainIterState.code, map, true);

            JCTree.JCClassDecl classdecl = (JCTree.JCClassDecl) state.domainIterState.code.defs.get(1);
            JCTree.JCMethodDecl methd = (JCTree.JCMethodDecl) classdecl.defs.last();

            state.domainIterState.inner = inner_for(methd);

            if (state.domainIterState.inner == null) {
                state.log.error(tree.pos, "internal", "failed to find inner most for loop");
                return;//error
            }

            state.domainIterState.index = apply_index(methd);
            state.domainIterState.inner_counter = (VarSymbol) TreeInfo.symbol(((JCTree.JCAssign) ((JCTree.JCExpressionStatement) state.domainIterState.inner.init.head).expr).lhs);

            state.domainIterState.used.remove(state.domainIterState.inner_counter);

            if (!at.treatAsBaseDomain()) {
                for (JCTree.JCVariableDecl vd : cur) {
                    state.domainIterState.used.remove(vd.sym);
                }
            }

            List<JCStatement> stats = methd.body.stats;

            state.domainIterState.counter = new LinkedHashSet<VarSymbol>();
            state.domainIterState.usedcounter = new LinkedHashSet<VarSymbol>();
            //skip decls (that were artficially added so we can parse cloog's code)
            while (stats.head.getTag() == JCTree.VARDEF) {
                state.domainIterState.counter.add(((JCVariableDecl) stats.head).sym);
                stats = stats.tail;
            }

            Set<VarSymbol> outer_symbols = new LinkedHashSet<VarSymbol>();
            outer_symbols.addAll(state.domainIterState.used);
            outer_symbols.removeAll(state.domainIterState.counter);

            Symbol from = TreeInfo.symbol(tree.exp);

            if (from instanceof VarSymbol) {
                outer_symbols.add((VarSymbol) from);
            }

            /*state.domainIterState.forwardCall!=null&&*/
            //do we actually produce a result?
            state.domainIterState.void_iter = !state.domainIterState.reduce && (tree.body == null || (!state.types.isCastable(at.elemtype, tree.body.type) && tree.type.tag != TypeTags.VOID));

            state.domainIterState.consecutive = true;
            boolean readonly = (tree.body == null || tree.type.tag == TypeTags.VOID || state.domainIterState.forwardCall != null);

            if (!state.domainIterState.reduce && !readonly) {

                state.domainIterState.consecutive = at.isCompatibleToRealTypeAndIterable(state.jc, at.dom, tree.pos());
                //check if we can apply a map on the last dimension
                //so the last dim must be indexed by the innermost loop and the index must not show up anywhere else
                if (state.domainIterState.consecutive) {
                    List<JCExpression> args = state.domainIterState.index.args;
                    while (!args.isEmpty()) {
                        if ((!args.tail.isEmpty() && findIndex(args.head, state.domainIterState.inner_counter))
                                || (args.tail.isEmpty() && TreeInfo.symbol(args.head) != state.domainIterState.inner_counter)) {
                            state.domainIterState.consecutive = false;
                            break;
                        }
                        args = args.tail;
                    }
                }
            }


            //need special handling if return is used inside iter
            if (state.domainIterState.iter.mayReturn && state.inside_task == null) {
                nl();
                print("bool __lambda_return" + state.domainIterState.iter.pos + "=false;");
                if (!state.is_void && state.method.getReturnType() != null) {
                    nl();
                    state.typeEmitter.printType(state.method.getReturnType().type);
                    print(" __lambda_return_value" + state.domainIterState.iter.pos + ";");
                }
            }

            //since the iteration is an expression we package into a lambda...
            nl();
            print("(" + getCaptured(outer_symbols, state.generate_into && tree.return_expression, false) + "()");
            String type = printLamdaReturn(at, tree);

            nl();
            print("{");
            indent();


            nl();
            print(type);
            if (!at.treatAsBaseDomain()) {
                //print("&");//avoid copy of projection
            }
            print("__VAL_TMP" + state.domainIterState.uid + "="); //get input into a defined var
            printExpr(tree.exp);
            print(";");

            //get state.target into __EXP_TMP...
            if (state.domainIterState.reduce) {
                if (!tree.type.isPrimitive()) {
                    //FIXME: reuse accum if unique?
                    String val = "__VAL_TMP" + state.domainIterState.uid;
                    if (tree.type.tag != TypeTags.VOID) {//handle state.target
                        nl();
                        print(type + "__EXP_TMP" + state.domainIterState.uid + "=");
                        if (!at.treatAsBaseDomain()) {
                            print(type + "(");
                        }

                        if (state.nestedIterations.size() > 1) {
                            print("__EXP_TMP" + oldIterState.uid);
                            if (!at.treatAsBaseDomain()) {
                                //get proper object
                                print(",");
                                //printExpr(oldIterState.iter.exp);
                                Type.ArrayType ot = (Type.ArrayType) oldIterState.iter.iterType.getArrayType();

                                JCExpression[] indices = new JCExpression[oldIterState.iter.domargs.size()];

                                int i = 0;
                                for (JCVariableDecl vd : oldIterState.iter.domargs) {
                                    indices[i] = state.jc.make.Ident(vd.sym);
                                    i++;
                                }

                                printDomainOffset(tree.pos(), oldIterState.iter.exp, ot, indices, false, false, false);
                                //ot.dom.ge
                            }

                            print(")");
                        } else {
                            print(val);
                            if (!at.treatAsBaseDomain()) {
                                print(", __VAL_TMP" + state.domainIterState.uid + ".offset)");
                            }
                        }


                        print(";");
                    }

                    //copy initial accum into target and use target as accum!
                    //special case for accum initial 0 (no need to copy anything as the GC automatically clears the allocated output)
                    if (!tree.emptyInit) {
                        Type.ArrayType tp = (Type.ArrayType) tree.params.head.type.getArrayType();
                        nl(); //just memcpy from accum to output
                        print("memcpy(");
                        print("__EXP_TMP" + state.domainIterState.uid);
                        if (!at.treatAsBaseDomain()) {
                            print(".object");
                        }
                        print("->toNative(),");
                        printExpr(tree.params.head);
                        if (!tp.treatAsBaseDomain()) {
                            print(".object");
                        }
                        print("->toNative(),");

                        if (at.dom.isDynamic() && tp.dom.isDynamic()) {
                            printExpr(tree.params.head);
                            if (!tp.treatAsBaseDomain()) {
                                print(".object");
                            }
                            print("->getSizeDim()");
                        } else {
                            int size;
                            if (!tp.dom.isDynamic()) {
                                size = tp.dom.getCard(tree.pos(), tp.dom.appliedParams.toArray(new JCExpression[0]),true);
                            } else {
                                size = at.dom.getCard(tree.pos(), at.dom.appliedParams.toArray(new JCExpression[0]),true);
                            }
                            print("" + size);
                        }

                        print(");");

                    }

                }
            } else if ((state.generate_into && tree.return_expression) || inside_joining) {//handle forwarding
                if (tree.type.tag != TypeTags.VOID) {
                    nl();
                    print(type + "__EXP_TMP" + state.domainIterState.uid + "=");

                    if (!at.treatAsBaseDomain()) {
                        print(type + "(");
                    }

                    print("__FORWARD__");

                    //FIXME: do we need to add any offset here?
					/*
                     if (!at.treatAsBaseDomain()) {
                     print(", __VAL_TMP" + state.domainIterState.uid + ".offset");
                     }
                     */
                    print(");");
                }
            } else {//default case

                String val = "__VAL_TMP" + state.domainIterState.uid;
                if (tree.exp.getTag() != JCTree.NEWARRAY && tree.type.tag != TypeTags.VOID) {
                    if (!at.treatAsBaseDomain()) {
                        val += ".object"; //unpack projection
                    }

                    //we try to avoid copying if value is linear..unless "wild" stencil is used
                    if (!state.domainIterState.reduce) {
                        if (!tree.exp.type.isLinear() || tree.valueOffsetAccess) {
                            val += "->getNewVersion()";
                        }
                    }
                }

                if (tree.type.tag != TypeTags.VOID) {//handle state.target
                    nl();
                    print(type + "__EXP_TMP" + state.domainIterState.uid + "=");
                    if (!at.treatAsBaseDomain()) {
                        print(type + "(");
                    }

                    if (state.nestedIterations.size() > 1) {
                        print("__EXP_TMP" + oldIterState.uid);
                    } else {
                        print(val);
                    }

                    if (!at.treatAsBaseDomain()) {
                        print(", __VAL_TMP" + state.domainIterState.uid + ".offset)");
                    }

                    print(";");
                }
            }

            if (at.dom.isDynamic()) {//set params (used by generated code) to the proper values
                for (int i = 0; i < at.dom.appliedParams.size(); i++) {
                    JCExpression e = at.dom.appliedParams.get(i);
                    if (e.type.constValue() == null || (Integer) e.type.constValue() == -1) {
                        nl();
                        print("funky::uint32 " + at.dom.formalParams.get(i).name.toString() + "=__VAL_TMP" + state.domainIterState.uid);
                        if (!at.treatAsBaseDomain()) {
                            print(".object");
                        }
                        print("->getDim(" + i + ");");
                    }
                }
            }

            boolean old_joining = inside_joining;
            inside_joining = false;



            //BEGIN DOMAIN ITERATION CODE DONE BY ALEX P.
            //Runtime Debug!
            //Do OpenCL preparations here.
            ProfilingCodeGenerator compileProfilingGenerator = new ProfilingCodeGenerator(state, tree);
            print(compileProfilingGenerator.printProfileRegistration("Compile"));
            
            KernelInitializer initializer = new KernelInitializer(state);
            String kernelInitCode = initializer.getKernelInitCode();
            print(kernelInitCode);
            nl();
            print(compileProfilingGenerator.printProfilingEnd());
            
            VariableAllocator kernelVariableManager = new VariableAllocator(state);

            
            ProfilingCodeGenerator copyProfilingGenerator = new ProfilingCodeGenerator(state, tree);
            print(copyProfilingGenerator.printProfileRegistration("CopyTo"));

            String lambdaReturnType = state.typeEmitter.getType(at);

            //Find all vars that need to be captured.
            String allocatedVariables = kernelVariableManager.handleVariableAllocation(lambdaReturnType);
            print(allocatedVariables);
            nl();

            //Copy memory chunks to GPU.
            String copyCode = kernelVariableManager.handleVariableCopying();
            print(copyCode);
            nl();

            //add parameters for the kernel
            String parameterSetup = kernelVariableManager.handleParameterSetup(initializer.getKernelName());
            print(parameterSetup);
            nl();
            //End of OpenCL preparations

            //Brace yourself. Kernel Compilation is coming.
            new RuntimeAnalysis(tree, state, kernelVariableManager).getProjectedRuntime();

            compileKernel(initializer, kernelVariableManager);

            print(copyProfilingGenerator.printProfilingEnd());
            ProfilingCodeGenerator runProfilingCodeGenerator = new ProfilingCodeGenerator(state, tree);
            print(runProfilingCodeGenerator.printProfileRegistration("GPURun"));
            nl();
            //Execute the Kernel!
            print("//Execute the Kernel!");
            nl();
            //FIXME Compiler Magic Value!
            final int retValSize = kernelVariableManager.getReturnValue().getReturnValueSize();
            print("size_t __wg" + initializer.getKernelName() + " = " + initializer.getKernelName()+".getOptimalWgSize("+ retValSize +");");
            nl();
            print("int id = " + initializer.getKernelName() + ".timedRun(__wg" + initializer.getKernelName() + ", " + retValSize + ");");
            //print(initializer.getKernelName()+".run(512, " + kernelVariableManager.getReturnValue().getReturnValueSize() + ");");
            /*static funky::ProfileEntry* __profile__2993_2=funky::Profile::RegisterCustom("2993","transpose_8192_8192","GPURun");
                tbb::tick_count __TIME__2993_2=tbb::tick_count::now();
                static funky::ProfileEntry* __profile__2993_mod=funky::Profile::RegisterCustom("2993_", "transpose_8192_8192", "GPURUN_v2");
                //Execute the Kernel!
                int id = transpose_8192_8192_16.timedRun(512, 67108864);
                //transpose_8192_8192_16.run(512, 67108864);
                device.finish();
                double runtime = transpose_8192_8192_16.getRunTime(id);
                __profile__2993_2->AddMeasurement(runtime*1e3);
                
                */
            nl();

            //Wait until done.
            print("device.finish();");
            print("double runtime = " + initializer.getKernelName() + ".getRunTime(id);");
            nl();
            print(runProfilingCodeGenerator.printProfilingEnd("runtime*1e3"));
            nl();
            
            
            ProfilingCodeGenerator copyBackProfilingCodeGenerator = new ProfilingCodeGenerator(state, tree);
            print(copyBackProfilingCodeGenerator.printProfileRegistration("copyBack"));
            nl();
            
            
            //Get the result from the GPU.
            String returnCode = kernelVariableManager.handleReturnValueRetrieval(lambdaReturnType);
            print(returnCode);
            nl();
            
            print(copyBackProfilingCodeGenerator.printProfilingEnd());
            nl();
            //Perform the Cleanup. This is done automatically, thanks to C++'s RAII
            //String cleanupCode = kernelVariableManager.handleCleanup();
            //print(cleanupCode);
            
            //touch releveant arrays so that GC doesn't kill them before GPU has finished copying
            for(KernelVariable var:kernelVariableManager.getArrays())
            {
                if(var.getGeneratedName()!=null)
                {
                    nl();
                    print("(("+var.getGeneratedType()+" volatile)"+var.getGeneratedName()+")[0]; //touch to avoid premature collection");
                }
            }            
            
            
            nl();

            inside_joining = old_joining;
                      
            if (!state.domainIterState.reduce && tree.type.tag != TypeTags.VOID) {//ret result
                String returnVarName = kernelVariableManager.getReturnValueName();

                print("return " + returnVarName + ";");
            }
            undent();
            nl();
            print("}) ()");//finish lambda

            //more handling of returning iter
            if (state.domainIterState.iter.mayReturn && state.inside_task == null) {
                print(";");

                nl();
                print("if(__lambda_return" + state.domainIterState.iter.pos + ")return ");
                if (!state.is_void && state.method.getReturnType() != null) {
                    print("__lambda_return_value" + state.domainIterState.iter.pos);
                }
            }

        } catch (IOException e) {
            state.log.error(tree.pos(), "internal", "failed to find inner most for loop");
        } finally {
            state.domainIterState = oldIterState;
            state.nestedIterations.pop();
        }

    }

    private void compileKernel(KernelInitializer initializer, VariableAllocator allocator) throws IOException {
        KernelCodeGenerator kernelCodeGen = new KernelCodeGenerator(state.domainIterState.iter.body, state, allocator, initializer);
        String kernelCode = kernelCodeGen.getKernelCode();
        String fileName = initializer.getKernelName();

        KernelFileOutput fileOut = new KernelFileOutput(fileName, state);
        fileOut.writeToFile(kernelCode);
    }

    void printClosureBegin(JCDomainIter tree) {
        try {
            print("[&]() ");
            printLamdaReturn((Type.ArrayType) tree.exp.type.getArrayType(), tree);
            print(" {");
            indent();
            nl();
            print("if (1) {");
            indent();
            nl();
            print("return");
            nl();
        } catch (IOException ex) {
            Logger.getLogger(LowerDataOCL.class.getName()).log(Level.SEVERE, null, ex);
            throw new LowerTree.UncheckedIOException(ex);
        }
    }

    void printClosureMiddle(JCDomainIter tree) {
        try {
            print(";");
            undent();
            nl();
            print("} else {");
            indent();
            nl();
            print("return");
            nl();
        } catch (IOException ex) {
            Logger.getLogger(LowerDataOCL.class.getName()).log(Level.SEVERE, null, ex);
            throw new LowerTree.UncheckedIOException(ex);
        }
    }

    void printClosureEnd() {
        try {
            print(";");
            undent();
            nl();
            print("}");
            undent();
            nl();
            print("}()");    
        } catch (IOException ex) {
            Logger.getLogger(LowerDataOCL.class.getName()).log(Level.SEVERE, null, ex);
            throw new LowerTree.UncheckedIOException(ex);
        }
    }

    private String printLamdaReturn(Type.ArrayType at, JCDomainIter tree) throws IOException {
        //FIXME: ->proper type..needed for ICPC!
        String type;
        if (!at.getRealType().treatAsBaseDomain()) {
            type = state.typeEmitter.getType(at.getRealType());
        } else {
            type = state.typeEmitter.getType(at);
        }
        //			state.domainIterState.consecutive=reduce||at.isCompatibleToRealTypeAndIterable(state.jc,at.dom, tree.pos());
        if (state.domainIterState.reduce) {
            print("->" + state.typeEmitter.getType(tree.body.type));
        } else {
            print("->" + type);
        }
        return type;
    }
}