/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.jvm.Code;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.TaskSet;
import com.sun.tools.javac.util.iTask;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 *
 * @author Andreas Wagner
 */
public class Scheduler {
    
    //Members
    private Properties configFile = null;
    private Work work = null;
    private Map<JCTree, TaskSet> treeTaskMapping = null;
    private JCTree.JCMethodDecl method = null;
    private Schedule schedule = null;
    
    public Scheduler(Context context){
        JavaCompiler jc = JavaCompiler.instance(context);
	configFile = jc.configFile;
        work = Work.instance(context);
        treeTaskMapping = new LinkedHashMap<JCTree, TaskSet>();
    }
    
    /**
     * Creates a schedule with regard of communication costs
     * and infinitely many processors.
     * see task-scheduling book page ...
     */
    public void scheduleWithComCosts(JCTree.JCMethodDecl method, SimpleDirectedGraph<TaskSet, TaskArc> taskGraph){
        Map<JCTree,Integer> oldTopolNodes = method.topolNodes;
        this.method = method;
        //reestablish topological order (might have been lost due to copy-procedure)
        /*
        method.topolNodes = new LinkedHashMap<JCTree, Integer>();
        for (Iterator<JCTree> i = new TopologicalOrderIterator<JCTree, JCTree.Arc>(method.depGraph); i.hasNext();) {
            JCTree node = i.next();

            //assign unique topol ordered id for shortest paths
            method.topolNodes.put(node, method.topolNodes.size());
        }
        */
        //create mapping from tree to task
        for(TaskSet ts : taskGraph.vertexSet()){
            for (Iterator<JCTree> it = ts.iterator(); it.hasNext();) {
                JCTree tree = it.next();
                treeTaskMapping.put(tree, ts);
            }
        }
        
        
        Schedule s = doScheduling(taskGraph);
        s.printSchedule("schedule");
        schedule = s;
        //restore topol. nodes
        //method.topolNodes = oldTopolNodes;
    }
    
    public Schedule getSchedule(){
        return schedule;
    }
    
    /**
     * Creates a schedule using List Scheduling's Start Time Minimization.
     * See task scheduling book, section 5.3.4 (Algorithm 18)
     * @param taskGraph 
     */
    private Schedule doScheduling(SimpleDirectedGraph<TaskSet, TaskArc> taskGraph){
        //sort tasks according to topological order
        Map<Integer, TaskSet> topolOrder = new LinkedHashMap<Integer, TaskSet>();
        
        /*
        for(TaskArc a : taskGraph.edgeSet()){
            boolean b1 = taskGraph.containsVertex(a.getSrc());
            boolean b2 = taskGraph.containsVertex(a.getDst());
        }
        */
        for (Iterator<TaskSet> i = new TopologicalOrderIterator<TaskSet, TaskArc>(taskGraph); i.hasNext();) {
            TaskSet ts = i.next();
            if(ts.size() == 1 && ts.getNode().getTag() == JCTree.CF)
                continue;
            //assign unique topol ordered id for shortest paths
            topolOrder.put(topolOrder.size(), ts);
        }
        
        Map<TaskSet, Float> topLevels = calcTopLevels(topolOrder, taskGraph);
        
        Schedule schedule = new Schedule(work, method, topolOrder);
        
        //create implicit schedule        
        for(int i = 0; i < topolOrder.size(); i++){
            float w = work.getWork((iTask)topolOrder.get(i), method);
            float initialStart = topLevels.get(topolOrder.get(i)) - work.getWork((iTask)topolOrder.get(i), method);
            if(initialStart < 0){
                //start node
                initialStart = 0;
            }
            schedule.addInitial(topolOrder.get(i), initialStart);
        }
        schedule.printSchedule("before");
        
        //now do list scheduling with start time minimization for clustering
        //see algorithm 18 in the book
        for(int i = 0; i < topolOrder.size(); i++){
            //initialize
            //node n
            TaskSet n = topolOrder.get(i);
            //skip if branch-task
            if(n.size() == 1 && n.getNode().getTag() == JCTree.CF) continue;
            //calculate biggest drt for n, assuming all incoming communications are remote
            /*
            float t_min = 0.0f;
            Set<TaskArc> nIncoming = taskGraph.incomingEdgesOf(n);
            for(TaskArc edge : nIncoming){
                TaskSet src = edge.getSrc();
                if(src.size() == 1 && src.getNode().getTag() == JCTree.CF) src = taskGraph.incomingEdgesOf(src).iterator().next().getSrc();
                Processor srcProc = schedule.getProcForTask(src);
                //finish-time of predicessor-processor + comcosts (implicit)
                float tmp = schedule.getFinishTime(edge, taskGraph, srcProc, schedule.EMPTY_PROCESSOR);
                
                if(tmp > t_min)
                    t_min = tmp;
            }
            */
            float t_min = schedule.getProcForTask(n).getStartTime(n);
            //empty processor is default
            Processor p_min = schedule.EMPTY_PROCESSOR;
            //get predicessors
            Set<TaskArc> incoming = taskGraph.incomingEdgesOf(n);
            for(TaskArc edge : incoming){
                TaskSet pred = edge.getSrc();
                //don't handle condition-tasks!!! take if-stmt istead
                if(pred.size() == 1 && pred.getNode().getTag() == JCTree.CF) pred = taskGraph.incomingEdgesOf(pred).iterator().next().getSrc();
                Processor predproc = schedule.getProcForTask(pred);
                float comp = Math.max(schedule.getDRT(n, predproc, taskGraph), predproc.getFinishTime());
                if(t_min >= comp){
                    t_min = comp;
                    p_min = predproc;
                }
            }
            if(p_min != schedule.EMPTY_PROCESSOR){
                schedule.moveTask(n, p_min, t_min);
            }
        }
        return schedule;
    }
    
    private Map<TaskSet, Float> calcTopLevels(Map<Integer, TaskSet> topolOrder, SimpleDirectedGraph<TaskSet, TaskArc> taskGraph){
        
        Map<TaskSet, Float> topLevels = new LinkedHashMap<TaskSet, Float>();
        
        for(int i = 0; i < topolOrder.size(); i++){
            TaskSet ts = topolOrder.get(i);
            //get predicessors
            if(taskGraph.inDegreeOf(ts) == 0){
                //is a root
                topLevels.put(ts, 0.0f);
            }
            else{
                Set<TaskArc> incoming = taskGraph.incomingEdgesOf(ts);
                float sum = 0.0f;
                for(TaskArc edge : incoming){
                    TaskSet pred = edge.getSrc();
                    if(pred.size() == 1 && pred.getNode().getTag() == JCTree.CF) pred = taskGraph.incomingEdgesOf(pred).iterator().next().getSrc();
                    float tmpsum = topLevels.get(pred) + work.getWork((iTask)ts, method);
                    if(!edge.getVars().isEmpty()){
                        for(VarSymbol vs : edge.getVars()){
                            tmpsum += Code.width(vs.type)*10000;
                        }
                    }
                    if(tmpsum > sum)
                        sum = tmpsum;
                }
                topLevels.put(ts, sum);
            }
        }
        
        return topLevels;
    }
    
    private class Processor{
        
        //Node finish time
        private Map<TaskSet, Float> nft;
        //Node start time
        private Map<TaskSet, Float> nst;
        private Work work;
        private JCTree.JCMethodDecl method;
        private int procID;
        
        public Processor(Work work, JCTree.JCMethodDecl method, int id){
            nft = new LinkedHashMap<TaskSet, Float>();
            nst = new LinkedHashMap<TaskSet, Float>();
            this.method = method;
            this.work = work;
            this.procID = id;
        }
        
        public int getID(){
            return this.procID;
        }
        
        public void addTask(TaskSet task, Float start){
            nst.put(task, start);
            nft.put(task, start + work.getWork((iTask)task, method));
        }
        
        public void removeTask(TaskSet task){
            nst.remove(task);
            nft.remove(task);
        }
        
        public float getFinishTime(TaskSet task){
            return nft.get(task);
        }
        
        public float getStartTime(TaskSet task){
            return nst.get(task);
        }
        
        public float getFinishTime(){
            float finishTime = 0.0f;
            for(TaskSet ts : nft.keySet()){
                if(nft.get(ts) >= finishTime)
                    finishTime = nft.get(ts);
            }
            return finishTime;
        }
        
    }
    
    public class Schedule{
        //Dummy for empty processor
        protected Processor EMPTY_PROCESSOR = new Processor(null, null, -1);
        private Map<TaskSet, Processor> taskProcMapping;
        private Work work;
        private JCTree.JCMethodDecl method;
        private  Map<Integer, TaskSet> topolOrder;
        private int proc_counter;
        
        public Schedule(Work work, JCTree.JCMethodDecl method, Map<Integer, TaskSet> topolOrder){
            taskProcMapping = new LinkedHashMap<TaskSet, Processor>();
            this.work = work;
            this.method = method;
            this.topolOrder = topolOrder;
            this.proc_counter = 0;
        }
        
        public void addInitial(TaskSet ts, Float start){
            Processor p = new Processor(work, method, proc_counter++);
            p.addTask(ts, start);
            taskProcMapping.put(ts, p);
        }
        
        public void moveTask(TaskSet ts, Processor pnew, Float start){
            Processor pold = getProcForTask(ts);
            pold.removeTask(ts);
            taskProcMapping.remove(ts);
            pnew.addTask(ts, start);
            taskProcMapping.put(ts, pnew);
        }
        
        public Processor getProcForTask(TaskSet ts){
            return taskProcMapping.get(ts);
        }
        
        public Set<Processor> getAllProcWithTasks(){
            Set<Processor> result = new LinkedHashSet<Processor>();
            for(Processor p: taskProcMapping.values()){
                if(p.nft.keySet().size() > 0)
                    result.add(p);
            }
            return result;
        }
        
        public int getProcNum(){
            return proc_counter;
        }
        
        public float getFinishTime(TaskArc edge, SimpleDirectedGraph<TaskSet, TaskArc> taskGraph, Processor src, Processor dst){
            TaskSet a = edge.getSrc();
            if(a.size() == 1 && a.getNode().getTag() == JCTree.CF) a = taskGraph.incomingEdgesOf(a).iterator().next().getSrc();
            Processor p = getProcForTask(a);
            if(src == dst){
                return p.getFinishTime(a);
            }
            else{
                float tmp = 0.0f;
                for(VarSymbol vs : edge.getVars()){
                    tmp += Code.width(vs.type)*10000;
                }
                return p.getFinishTime(a) + tmp;
            }
        }
        
        public float getDRT(TaskSet ts, Processor p, SimpleDirectedGraph<TaskSet, TaskArc> taskGraph){
            //get predicessors
            Set<TaskArc> incoming = taskGraph.incomingEdgesOf(ts);
            float drt = 0.0f;
            for(TaskArc edge : incoming){
                TaskSet pred = edge.getSrc();
                if(pred.size() == 1 && pred.getNode().getTag() == JCTree.CF) pred = taskGraph.incomingEdgesOf(pred).iterator().next().getSrc();
                Processor predProc = getProcForTask(pred);
                float tmp = getFinishTime(edge, taskGraph, predProc, p);
                if(tmp > drt)
                    drt = tmp;
            }
            return drt;
        }
        
        public Set<TaskSet> getTaskAt(Float time){
            Set<TaskSet> result = new LinkedHashSet<TaskSet>();
            for(Processor p : getAllProcWithTasks()){
                for(TaskSet ts : p.nst.keySet())
                    if(p.nst.get(ts).compareTo(time) == 0)
                        result.add(ts);
            }
            return result;
        }
        
        public Set<Float> getAllStartTimes(){
            Set<Float> result = new LinkedHashSet<Float>();
            for(Processor p : getAllProcWithTasks()){
                for(Float f : p.nst.values())
                    result.add(f);
            }
            return result;
        }
        
        public void printSchedule(String filename){
            //destination file
            File f = new File("/home/andreas/" + filename +".csv");
            //output string, not efficient but fits our needs here
            String output = "";
            
            Set<Float> starttimes = getAllStartTimes();
            int numproc = getProcNum();
            int numstarttimes = starttimes.size();
            List<Float> starttimeslist = new LinkedList<Float>();
            for(Iterator<Float> it = starttimes.iterator(); it.hasNext();){
                starttimeslist.add(it.next());
            }
            
            Collections.sort(starttimeslist);
            
            //set up a 2dim-array
            String[][] array = new String[numproc][numstarttimes];
            for(int i = 0; i < numstarttimes; i++){
                Set<TaskSet> set = getTaskAt(starttimeslist.get(i));
                for(TaskSet ts : set){
                    int procid = getProcForTask(ts).getID();
                    if(array[procid][i] != null){
                        array[procid][i] += " // "+ts.toString();
                    }
                    else{
                        array[procid][i] = ts.toString();
                    }
                    array[procid][i] += "(ft: "+ getProcForTask(ts).nft.get(ts) + ")";
                }
            }
            
            for(int i = 0; i < numstarttimes; i++){
                output += starttimeslist.get(i) + "|";
                for(int j = 0; j < numproc; j++){
                    if(array[j][i] != null){
                        output += array[j][i].toString() + "|";
                    }
                    else{
                        output += "|";
                    }
                }
                output += "\n";
            }
            
            try {
                PrintWriter pw = new PrintWriter(f);
                pw.print(output);
                pw.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Scheduler.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
        }
        
    }
    
    private float getConfig(String name, float default_value) {
            if (configFile == null) {
                    return default_value;
            }
            String prop = configFile.getProperty(name);
            if (prop == null) {
                    return default_value;
            } else {
                    try {
                            return Float.parseFloat(prop);
                    } catch (NumberFormatException e) {
                            //FIXME: error
                            return default_value;
                    }
            }
    }
}
