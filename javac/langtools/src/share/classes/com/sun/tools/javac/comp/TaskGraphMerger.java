/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.comp;

import com.sun.org.apache.bcel.internal.generic.NOP;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.comp.TaskGen.BoxVertex;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.Arc;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.TaskSet;
import com.sun.tools.javac.util.iTask;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaFileObject;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import org.jgrapht.ext.ComponentAttributeProvider;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 *
 * @author Andreas Wagner
 */
public class TaskGraphMerger {
    
    private RecDataAnalyzerOrig     originalAnalysis;
    private RecDataAnalyzerClone    cloneAnalysis;
    private TreeMaker               maker;
    private Map<JCTree, TaskSet>    taskMapping;
    private Map<iTask, Integer>     inputDepTasksForParamExpr;
    
    public TaskGraphMerger(RecDataAnalyzerOrig orig, RecDataAnalyzerClone clone, TreeMaker make){
        originalAnalysis = orig;
        cloneAnalysis = clone;
        maker = make;
        taskMapping = orig.getTaskMapping();
        taskMapping.putAll(clone.getTaskMapping());
        inputDepTasksForParamExpr = new LinkedHashMap<iTask, Integer>();
    }
    
    public SimpleDirectedGraph<TaskSet, TaskArc> merge(){
        
        SimpleDirectedGraph<TaskSet, TaskArc> graph1 = originalAnalysis.getTaskGraph();
        SimpleDirectedGraph<TaskSet, TaskArc> clone = cloneAnalysis.getTaskGraph();
        
        printGraph("orig.dot", graph1);
        printGraph("clone.dot", clone);
        
        merge(graph1, clone);
        connect(graph1, clone);
        
        //kill invocation in original graph
        JCMethodDecl orig = originalAnalysis.getMethodDeclaration();
        JCTree invoc = orig.cloneAssociationMap.get(originalAnalysis.getInvocationStatement());
        TaskSet invocTS = taskMapping.get(invoc);
        //check if TS will be empty -> remove completely
        if(invocTS.size()-1 == 0){
            Set<TaskArc> incoming = graph1.incomingEdgesOf(invocTS);
            Set<TaskArc> outgoing = graph1.outgoingEdgesOf(invocTS);
            graph1.removeVertex(invocTS);
            //graph1.removeAllEdges(incoming);
            //graph1.removeAllEdges(outgoing);          
        }
        else{
            //we have to reestablish the edges, because when we remove one stmt, the graph will not detect
            //that the task is basically still the same -> reconnect all edges of this node
            Set<TaskArc> incoming = graph1.incomingEdgesOf(invocTS);
            //reserve all data
            Set<TaskSet> srcVertices = new LinkedHashSet<TaskSet>();
            Map<TaskSet, Set<VarSymbol>> variables = new LinkedHashMap<TaskSet, Set<VarSymbol>>();
            for(TaskArc ta : incoming){
                srcVertices.add(ta.getSrc());
                Set<VarSymbol> tmp = new LinkedHashSet<VarSymbol>();
                tmp.addAll(ta.getVars());
                variables.put(ta.getSrc(), tmp);
            }
            Set<TaskArc> outgoing = graph1.outgoingEdgesOf(invocTS);
            Set<TaskSet> dstVertices = new LinkedHashSet<TaskSet>();
            for(TaskArc ta : outgoing){
                dstVertices.add(ta.getDst());
                Set<VarSymbol> tmp = new LinkedHashSet<VarSymbol>();
                tmp.addAll(ta.getVars());
                variables.put(ta.getDst(), tmp);
            }
            
            //remove the vertex
            graph1.removeVertex(invocTS);
            
            //remove the unwanted entry from ts
            invocTS.removeNode(originalAnalysis.getMethodDeclaration());
            
            //add modified ts
            graph1.addVertex(invocTS);
            
            //create new edges
            for(TaskSet ts : srcVertices){
                TaskArc newArc = new TaskArc(ts, invocTS, variables.get(ts));
                graph1.addEdge(ts, invocTS, newArc);
            }
            for(TaskSet ts : dstVertices){
                TaskArc newArc = new TaskArc(invocTS, ts, variables.get(ts));
                graph1.addEdge(invocTS, ts, newArc);
            }
            
        }
        
        mergeFinishNodes(graph1, clone);
        mergeSkipNodes(graph1, clone);
        
        //kill declaration in clone graph
        TaskSet declTS = taskMapping.get(cloneAnalysis.getMethodDeclaration());
        //check if TS will be empty -> then remove completely
        if(declTS.size()-1 == 0){
            graph1.removeVertex(declTS);
        }
        else{
            //we have to reestablish the edges, because when we remove one stmt, the graph will not detect
            //that the task is basically still the same -> reconnect all edges of this node
            Set<TaskArc> incoming = graph1.incomingEdgesOf(declTS);
            //reserve all data
            Set<TaskSet> srcVertices = new LinkedHashSet<TaskSet>();
            Map<TaskSet, Set<VarSymbol>> variables = new LinkedHashMap<TaskSet, Set<VarSymbol>>();
            for(TaskArc ta : incoming){
                srcVertices.add(ta.getSrc());
                Set<VarSymbol> tmp = new LinkedHashSet<VarSymbol>();
                tmp.addAll(ta.getVars());
                variables.put(ta.getSrc(), tmp);
            }
            Set<TaskArc> outgoing = graph1.outgoingEdgesOf(declTS);
            Set<TaskSet> dstVertices = new LinkedHashSet<TaskSet>();
            for(TaskArc ta : outgoing){
                dstVertices.add(ta.getDst());
                Set<VarSymbol> tmp = new LinkedHashSet<VarSymbol>();
                tmp.addAll(ta.getVars());
                variables.put(ta.getDst(), tmp);
            }
            
            //remove the vertex
            graph1.removeVertex(declTS);
            
            //remove the unwanted entry from ts
            declTS.removeNode(cloneAnalysis.getMethodDeclaration());
            
            //add modified ts
            graph1.addVertex(declTS);
            
            //get new followers
            Set<TaskSet> newFollowers = new LinkedHashSet<TaskSet>();
            for(JCTree stmt : declTS){
                Set<Arc> followers = cloneAnalysis.getDepGraph().outgoingEdgesOf(stmt);
                for(Arc a : followers){
                    TaskSet followingTS = taskMapping.get(a.t);
                    newFollowers.add(followingTS);
                }
            }
            
            //create new edges
            for(TaskSet ts : srcVertices){
                TaskArc newArc = new TaskArc(ts, declTS, variables.get(ts));
                graph1.addEdge(ts, declTS, newArc);
            }
            for(TaskSet ts : newFollowers){
                TaskArc newArc = new TaskArc(declTS, ts, variables.get(ts));
                if(!graph1.containsEdge(declTS, ts) && (declTS != ts) && graph1.containsVertex(ts)){
                    graph1.addEdge(declTS, ts, newArc);
                }
            }
            
        }
        
        //delete nodes which have no outgoing edges (e.g. which have been generated by variable-expressions)
        clean(graph1);
        
        printGraph("final.dot", graph1);
              
        return graph1;
    }
    
    private void merge(SimpleDirectedGraph<TaskSet, TaskArc> graph1, SimpleDirectedGraph<TaskSet, TaskArc> graph2){
        for(TaskSet v : graph2.vertexSet()){
            graph1.addVertex(v);
        }
        for(TaskArc a : graph2.edgeSet()){
            Set<VarSymbol> set = new LinkedHashSet<VarSymbol>();
            set.addAll(a.getVars());
            graph1.addEdge(a.getSrc(), a.getDst(), 
                    new TaskArc(a.getSrc(), a.getDst(), set));
        }
        printGraph("afterMerge.dot", graph1);
    }
    
    private void connect(SimpleDirectedGraph<TaskSet, TaskArc> graph1, SimpleDirectedGraph<TaskSet, TaskArc> graph2){
        Map<Integer, JCExpression> paramExpressions = originalAnalysis.getArgumentExpressionMapping();
        Map<Integer, Set<Arc>> dependencies = originalAnalysis.getIncomingArgumentDependencies();
        Map<Integer, Set<Arc>> following = cloneAnalysis.getParamSuccessorMapping();
        Map<Integer, VarSymbol> params = cloneAnalysis.getParams();
        
        JCTree invocationNode = originalAnalysis.getInvocationStatement();
        JCMethodDecl orig = originalAnalysis.getMethodDeclaration();

        Set<TaskSet> newTSets = new LinkedHashSet<TaskSet>();
        
        for(Integer i : paramExpressions.keySet()){
            JCExpression expr = paramExpressions.get(i);
            DirectedWeightedMultigraph<JCTree, Arc> depGraph = orig.depGraph;
            Set<Arc> outgoing = following.get(i);
            VarSymbol variable = params.get(i);
            
            //make a connection node
            JCTree connectionNode = maker.Assignment(variable, expr);
            
            TaskSet tsNew = new TaskSet(depGraph, orig);
            tsNew.add(connectionNode);

            //collect each new taskset
            newTSets.add(tsNew);
            //add new taskset
            graph1.addVertex(tsNew);
            //connect with incoming 
            Set<Arc> dependency = dependencies.get(i);
            Set<TaskSet> in = new LinkedHashSet<TaskSet>();
            Map<TaskSet, TaskArc> map = new LinkedHashMap<TaskSet, TaskArc>();
            for(Arc a : dependency){
                //all edges in dependency apply to a variable, so a.v is never null
                if(in.contains(taskMapping.get(a.s))){
                    TaskArc arc = map.get(taskMapping.get(a.s));
                    arc.addVar(a.v);
                }
                else{
                    TaskArc arc = new TaskArc(taskMapping.get(a.s), tsNew, new LinkedHashSet<VarSymbol>());
                    arc.addVar(a.v);
                    inputDepTasksForParamExpr.put(taskMapping.get(a.s), i);
                    map.put(taskMapping.get(a.s), arc);
                    in.add(taskMapping.get(a.s));
                }
            }
            for(TaskSet ts : in){
                TaskArc ta = map.get(ts);
                graph1.addEdge(ta.getSrc(), ta.getDst(), ta);
            }
            
            //connect with outgoing
            Set<TaskSet> out = new LinkedHashSet<TaskSet>();
            Map<TaskSet, TaskArc> map2 = new LinkedHashMap<TaskSet, TaskArc>();
            
            for(Arc a : outgoing){
                TaskSet ts = taskMapping.get(a.t);
                if(out.contains(taskMapping.get(a.t))){
                    TaskArc arc = map2.get(taskMapping.get(a.t));
                    arc.addVar(a.v);
                }
                else{
                    TaskArc arc = new TaskArc(tsNew, ts, new LinkedHashSet<VarSymbol>());
                    arc.addVar(a.v);
                    map2.put(taskMapping.get(a.t), arc);
                    out.add(taskMapping.get(a.t));
                }
            }
            for(TaskSet ts : out){
                TaskArc ta = map2.get(ts);
                graph1.addEdge(ta.getSrc(), ta.getDst(), ta);
            }
            
        }
        //create variable independent connections
        TaskSet invocationTS = taskMapping.get(orig.cloneAssociationMap.get(invocationNode));
        Set<TaskArc> taskIncoming = graph1.incomingEdgesOf(invocationTS);
        Set<TaskArc> taskOutgoing = graph1.outgoingEdgesOf(invocationTS);
        
        for(TaskSet newts : newTSets){
            for(TaskArc arc : taskIncoming){
                if(arc.getVars().isEmpty()){
                    TaskArc newTA = new TaskArc(arc.getSrc(), newts, arc.getVars());
                    graph1.addEdge(newTA.getSrc(), newTA.getDst(), newTA);
                }
            }
        }
        printGraph("afterConnect.dot", graph1);
    }
    
    private void mergeSkipNodes(SimpleDirectedGraph<TaskSet, TaskArc> graph1, SimpleDirectedGraph<TaskSet, TaskArc> graph2){
        Set<Arc> originalFollowing = originalAnalysis.getFollowingNodes();
        JCMethodDecl origDecl = originalAnalysis.getMethodDeclaration();
        JCTree cloneSkip = cloneAnalysis.getSkipStatement();
        Set<TaskArc> incomingOfSkip = graph1.incomingEdgesOf(taskMapping.get(cloneSkip));
        JCTree originalSkip = originalAnalysis.getSkipStatement();
        
        for(Arc a : originalFollowing){
            for(TaskArc b : incomingOfSkip){
                
                if(b.getVars().contains(a.v)){
                    TaskSet s = b.getSrc();
                    TaskSet t = taskMapping.get(a.t);
                    if(graph1.containsEdge(s, t)){
                        TaskArc ta = graph1.getEdge(s, t);
                        ta.getVars().add(a.v);
                    }
                    else{
                        Set<VarSymbol> set = new LinkedHashSet<VarSymbol>();
                        set.add(a.v);
                        graph1.addEdge(s, t, new TaskArc(s,t, set));
                    }
                    
                }
                else{
                    TaskSet s = b.getSrc();
                    TaskSet t = taskMapping.get(originalSkip);
                    graph1.addEdge(s, t, new TaskArc(s,t,new LinkedHashSet<VarSymbol>()));
                }
            }
        }
        //delete skip-node
        graph1.removeVertex(taskMapping.get(cloneSkip));
        printGraph("afterSkipMerging.dot", graph1);
    }
    
    private void clean(SimpleDirectedGraph<TaskSet, TaskArc> graph1){
        Set<TaskSet> toEliminate = new LinkedHashSet<TaskSet>();
        TaskSet skipTS = originalAnalysis.getSkipTaskSet();
        for(TaskSet ts : graph1.vertexSet()){
            if(graph1.outDegreeOf(ts) == 0 && ts != skipTS)
                toEliminate.add(ts);
        }
        graph1.removeAllVertices(toEliminate);
    }
    
    private void mergeFinishNodes(SimpleDirectedGraph<TaskSet, TaskArc> graph1, SimpleDirectedGraph<TaskSet, TaskArc> graph2){
        Map<Integer, JCTree> originalReturns = originalAnalysis.getReturnStatements();
        Map<Integer, JCTree> cloneReturns = cloneAnalysis.getReturnStatements();
        JCMethodDecl orig = originalAnalysis.getMethodDeclaration();
        JCMethodDecl clone = cloneAnalysis.getMethodDeclaration();
        
        for(Integer i : originalReturns.keySet()){
            JCTree origReturn = originalReturns.get(i);
            JCTree cloneReturn = cloneReturns.get(i);
            TaskSet origReturnTask = taskMapping.get(orig.cloneAssociationMap.get(origReturn));
            TaskSet cloneReturnTask = taskMapping.get(clone.cloneAssociationMap.get(cloneReturn));
            if(graph1.containsVertex(origReturnTask) && graph1.containsVertex(cloneReturnTask)){
                Set<TaskArc> cloneIncoming = graph1.incomingEdgesOf(taskMapping.get(cloneAnalysis.getMethodDeclaration().cloneAssociationMap.get(cloneReturn)));

                for(TaskArc a : cloneIncoming){
                    TaskSet s = graph1.getEdgeSource(a);
                    TaskSet t = taskMapping.get(originalAnalysis.getMethodDeclaration().cloneAssociationMap.get(origReturn));
                    Set<VarSymbol> set = new LinkedHashSet<VarSymbol>();
                    set.addAll(a.getVars());
                    graph1.addEdge(s, t, new TaskArc(s, t, set));
                }
                graph1.removeVertex(taskMapping.get(cloneAnalysis.getMethodDeclaration().cloneAssociationMap.get(cloneReturn)));
            }
        }
        
        printGraph("afterFinishMerging.dot", graph1);
    }
    
    public Map<iTask, Integer> getParamExprInputDeps(){
        return inputDepTasksForParamExpr;
    }
    
    /**
     * Util
     */
    
    
    private void printGraph(String name, SimpleDirectedGraph<TaskSet, TaskArc> graph) {
		class EFTEdge implements EdgeNameProvider<TaskArc> {

			public String getEdgeName(TaskArc edge) {
                            String name = "";
                            for(VarSymbol vs : edge.getVars())
                                name += vs.toString();
                            return name;
			}
		}

		class EFTNameProvider implements VertexNameProvider<TaskSet>{

			public String getVertexName(TaskSet v)
			{
                            String name = "";
                            for(JCTree tree : v.getTreeList(false))
                                name = name + tree.toFlatString() + "\\n";
                            return name;
			}
		}
                
                class NOP<T> implements ComponentAttributeProvider<T> {

		public Map<String, String> getComponentAttributes(T component) {
			return new LinkedHashMap<String, String>();
		}
	}

	class BoxVertex implements ComponentAttributeProvider<TaskSet> {

		public Map<String, String> getComponentAttributes(TaskSet component) {
			Map<String, String> properties= new LinkedHashMap<String, String>();
			properties.put("shape", "box");
			return properties;
		}
	}
                
                try {
			File file = new File("/home/andreas/"+name);

			(new DOTExporter<TaskSet, TaskArc>(new IntegerNameProvider<TaskSet>(), new EFTNameProvider(), new EFTEdge(), new BoxVertex(), new NOP<TaskArc>())).export(new BufferedWriter(new FileWriter(file)), graph);
		} catch (IOException e) {
		}
	}
}
