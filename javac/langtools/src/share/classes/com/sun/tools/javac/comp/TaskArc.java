/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.util.TaskSet;
import java.util.Set;
import org.jgrapht.graph.DefaultEdge;

/**
 * Arc for TaskGraphs
 * @author Andreas Wagner
 */
@SuppressWarnings("serial")
public class TaskArc{
    
    private Set<VarSymbol> vars;
    private TaskSet src;
    private TaskSet dst;
    
    public TaskArc(TaskSet s, TaskSet t, Set<VarSymbol> v){
        this.src = s;
        this.dst = t;
        this.vars = v;
    }

    public Set<VarSymbol> getVars() {
        return vars;
    }

    public TaskSet getSrc() {
        return src;
    }

    public TaskSet getDst() {
        return dst;
    }
    
    public void addVar(VarSymbol vs){
        this.vars.add(vs);
    }
    
    @Override
    public String toString(){
        if(null!=vars)
            return src.toString()+"-("+vars.toString()+")->"+dst.toString();
        else
            return src.toString()+"-->"+dst.toString();
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.dst != null ? this.dst.hashCode() : 0);
        hash = 29 * hash + (this.src != null ? this.src.hashCode() : 0);
        //hash = 29 * hash + (this.vars != null ? this.vars.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TaskArc other = (TaskArc) obj;
        if (this.vars != other.vars && (this.vars == null || !this.vars.equals(other.vars))) {
            return false;
        }
        if (this.src != other.src && (this.src == null || !this.src.equals(other.src))) {
            return false;
        }
        if (this.dst != other.dst && (this.dst == null || !this.dst.equals(other.dst))) {
            return false;
        }
        return true;
    }


}
