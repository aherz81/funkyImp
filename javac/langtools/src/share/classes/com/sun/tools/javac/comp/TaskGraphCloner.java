/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.comp;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.TaskSet;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree.Arc;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.jgrapht.ext.ComponentAttributeProvider;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 *
 * @author Andreas Wagner
 * Util class for cloning task graphs
 */
public class TaskGraphCloner {
    public Map<TaskSet, TaskSet> oldNew;
    
    public SimpleDirectedGraph<TaskSet, TaskArc> clone(JCMethodDecl tree, TreeMaker make){
        SimpleDirectedGraph<TaskSet, TaskArc> clone = new SimpleDirectedGraph<TaskSet, TaskArc>(TaskArc.class);
        
        
        //clone hasse diagram
        oldNew = new LinkedHashMap<TaskSet, TaskSet>();
        TreeCopier copy = new TreeCopier(make);
        printGraph2("hasse.dot", tree.hasseFinal);
        
         
        for(TaskSet ts : tree.hasseFinal.vertexSet()){
            TaskSet tsNew = ts.deepCopy(tree, copy);
            clone.addVertex(tsNew);
            oldNew.put(ts, tsNew);
        }
        for(DefaultEdge edgge : tree.hasseFinal.edgeSet()){
            TaskSet newSrc = oldNew.get(tree.hasseFinal.getEdgeSource(edgge));
            TaskSet newDst = oldNew.get(tree.hasseFinal.getEdgeTarget(edgge));
            
            Set<VarSymbol> hs = new LinkedHashSet<VarSymbol>();
            
            clone.addEdge(newSrc, newDst, new TaskArc(newSrc, newDst, hs));
        }
        
        //reestablish topological order
        Map<JCTree, Integer> topolOrder = new LinkedHashMap<JCTree, Integer>();
        for (Iterator<JCTree> i = new TopologicalOrderIterator<JCTree, Arc>(tree.depGraph); i.hasNext();) {
            JCTree t = i.next();
            
            //assign unique topol ordered id for shortest paths
            topolOrder.put(t, topolOrder.size());
        }
        
        tree.topolNodes = topolOrder;
        
        return clone;
    }
    
    private static void printGraph2(String name, SimpleDirectedGraph<TaskSet, DefaultEdge> graph){
        class EFTEdge implements EdgeNameProvider<DefaultEdge> {

			public String getEdgeName(DefaultEdge edge) {
				return "";
			}
		}

		class EFTNameProvider implements VertexNameProvider<TaskSet>{

			public String getVertexName(TaskSet v)
			{
                            String name = "";
                            for(JCTree tree : v)
                                name = name + tree.toFlatString() + "\\n";
                            return name;
			}
		}
                
                class NOP<T> implements ComponentAttributeProvider<T> {

		public Map<String, String> getComponentAttributes(T component) {
			return new HashMap<String, String>();
		}
	}

	class BoxVertex implements ComponentAttributeProvider<TaskSet> {

		public Map<String, String> getComponentAttributes(TaskSet component) {
			Map<String, String> properties= new HashMap<String, String>();
			properties.put("shape", "box");
			return properties;
		}
	}
                
                try {
			File file = new File("/home/andreas/"+name);

			(new DOTExporter<TaskSet, DefaultEdge>(new IntegerNameProvider<TaskSet>(), new EFTNameProvider(), new EFTEdge(), new BoxVertex(), new NOP<DefaultEdge>())).export(new BufferedWriter(new FileWriter(file)), graph);
		} catch (IOException e) {
		}
	}
}
