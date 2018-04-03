/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.tree.cl.util;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.LowerTreeImpl;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexander Pöppl
 */
public class ProfilingCodeGenerator {
    
    private static final Map<Integer, Integer> positionResolver = new HashMap<Integer, Integer>();
    
    private final PrettyStringBuilder printer;
    private final JCTree tree;
    private final LowerTreeImpl state;
    private final int existingProfilersForPos;
    
    public ProfilingCodeGenerator(LowerTreeImpl compilerState, JCTree tree) {
        this.printer = new PrettyStringBuilder(compilerState);
        this.state = compilerState;
        this.tree = tree;
        if (positionResolver.containsKey(tree.pos)) {
            positionResolver.put(tree.pos, (positionResolver.get(tree.pos) + 1));
        } else {
            positionResolver.put(tree.pos, 0);
        }
        this.existingProfilersForPos = positionResolver.get(tree.pos);
    }
        
    public String printProfileRegistration(String profilingName) {
        printer.empty();
        printer.appendNl();
        //Symbol.VarSymbol vs = tree.time.iterator().next();
        printer.append("static funky::ProfileEntry* __profile__" + tree.pos + "_" + this.existingProfilersForPos +"=funky::Profile::RegisterCustom(\"" + tree.pos + "\",\"" + state.method.name + "\",\"" + profilingName + "\");");
        printer.appendNl();
        printer.append("tbb::tick_count __TIME__" + tree.pos + "_" + this.existingProfilersForPos +"=tbb::tick_count::now();");
        printer.appendNl();
        return printer.toString();
    }
    
    /**
     * Be careful, only use in the same scope as an preceding {@link appendProfileBegin()}. DEBUG USE! Prints code to measure the time passed since the refeference point and outputs it on the command line.
     * @return The StringBuilder
     */
    public String printProfilingEnd() {
        printer.empty();
        printer.appendNl();
        printer.append("__profile__" + tree.pos + "_" + this.existingProfilersForPos + "->AddMeasurement((tbb::tick_count::now()-__TIME__" + tree.pos + "_" + this.existingProfilersForPos + ").seconds()*1e6);");
        printer.appendNl();
        return printer.toString();
    }
    public String printProfilingEnd(String customValue) {
        printer.empty();
        printer.appendNl();
        printer.append("__profile__" + tree.pos + "_" + this.existingProfilersForPos + "->AddMeasurement(" + customValue + ");");
        printer.appendNl();
        return printer.toString();
    }
}

