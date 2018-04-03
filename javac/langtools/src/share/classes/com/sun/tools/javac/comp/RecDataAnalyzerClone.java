/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import static com.sun.tools.javac.comp.RecDataAnalyzerClone.RecDataKey;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.Arc;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.TaskSet;
import com.sun.tools.javac.util.iTask;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 *
 * @author Andreas Wagner
 */
public class RecDataAnalyzerClone extends TreeScanner{
    private       TreeMaker make;
    
    protected static final Context.Key<RecDataAnalyzerClone> RecDataKey = 
            new Context.Key<RecDataAnalyzerClone>();

    
    public static RecDataAnalyzerClone instance(Context context) {
        RecDataAnalyzerClone instance = context.get(RecDataKey);
        if (instance == null)
            instance = new RecDataAnalyzerClone(context);
        return instance;
    }
    
    protected RecDataAnalyzerClone(Context context) {
        context.put(RecDataKey, this);
        make = TreeMaker.instance(context);
    }
    
    private JCStatement                             currentStatement;
    private TaskSet                                 currentTaskSet;
    private JCMethodDecl                            methodDeclaration;
    private TaskSet                                 methodDeclarationTS;
    private MethodSymbol                            methodName;
    private DirectedWeightedMultigraph<JCTree, Arc> depGraph;
    private SimpleDirectedGraph<TaskSet, TaskArc>   taskGraph;
    private boolean                                 recursiveCall;
    private Map<Integer, Set<Arc>>                  paramSuccessorMapping;
    private Map<JCTree, JCTree>                     lookup;
    private Map<Integer, VarSymbol>                 params;
    private Set<JCTree>                             newJoinNodes;
    private Map<JCTree, TaskSet>                    treeTaskMapping;
    public Map<TaskSet, TaskSet>                    originalTaskMapping;

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        if(tree.sym.mayBeRecursive){
            //obtain some method info
            methodDeclaration = tree;
            methodName = tree.sym;
            skipNode = tree.dg_end;
            lookup = tree.cloneAssociationMap;
            recursiveCall = false;
            //for analysis - see visitApply
            mustReturnSomething = false;
            //also for analysis - see visitApply
            invocationAlreadyVisited = false;
            varDecls = new LinkedHashSet<JCTree>();
            invocationReplacements = new LinkedHashMap<JCTree, JCIdent>();
            depGraph = tree.depGraph;
            paramSuccessorMapping = new LinkedHashMap<Integer, Set<Arc>>();
            treeTaskMapping = new LinkedHashMap<JCTree, TaskSet>();
            params = new LinkedHashMap<Integer, VarSymbol>();
            returnStatements = new LinkedHashMap<Integer, JCTree>();
            newJoinNodes = new LinkedHashSet<JCTree>();
 
            TaskGraphCloner tgc = new TaskGraphCloner();
            taskGraph = tgc.clone(tree, make);
            originalTaskMapping = tgc.oldNew;
            
            //map trees to tasks
            for(TaskSet ts : taskGraph.vertexSet()){
                for (Iterator<JCTree> it = ts.iterator(); it.hasNext();) {
                    JCTree t = it.next();
                    treeTaskMapping.put(t, ts);
                }
            }
            
            //Reestablish transitive edges and calculate variable dependencies
            for(JCTree v : depGraph.vertexSet()){
                //  if(v == tree) continue;
                iTask src_task = treeTaskMapping.get(v);
                for(Arc a : depGraph.outgoingEdgesOf(v)){
                    iTask dst_task = treeTaskMapping.get(a.t);
                    if(src_task == dst_task) continue;
                    
                    if(!taskGraph.containsEdge((TaskSet)src_task, (TaskSet)dst_task)){
                        //add transitive edge
                        Set<VarSymbol> set = new HashSet<VarSymbol>();
                        TaskArc arc = new TaskArc((TaskSet)src_task, (TaskSet)dst_task, set);
                        taskGraph.addEdge((TaskSet)src_task, (TaskSet)dst_task, arc);
                    }
                    
                    if(a.v != null){
                        TaskArc arc = taskGraph.getEdge((TaskSet)src_task, (TaskSet)dst_task);
                        arc.addVar(a.v);
                    }
                    
                }
            }
            
            //scan the whole method-body and do neccessary transformations
            scanStat(tree.body);
            
            //remove unneccassary nodes
            Set<JCTree> stmtsToReplace = invocationReplacements.keySet();
            for(JCTree stmt : stmtsToReplace){
                //modify task graph
                TaskSet ts = treeTaskMapping.get(tree.cloneAssociationMap.get(stmt));
                ts.removeNode(tree.cloneAssociationMap.get(stmt));
                //modify dependency graph
                
                JCIdent replacement = invocationReplacements.get(stmt);
                Set<Arc> incoming = depGraph.incomingEdgesOf(lookup.get(stmt));
                Set<Arc> outgoing = depGraph.outgoingEdgesOf(lookup.get(stmt));
                depGraph.addVertex(replacement);
                newJoinNodes.add(replacement);
                for(Arc a : incoming){
                    depGraph.addEdge(a.s, replacement, new Arc(a.s, replacement, a.v));
                }
                for(Arc a : outgoing){
                    depGraph.addEdge(replacement, a.t, new Arc(replacement, a.t, a.v));
                }
                
                depGraph.removeVertex(lookup.get(stmt));
            }           
            
            //obtain successors of declaration, separated by variable
            for(int i = 0; i < tree.params.size(); i++){
                Set<Arc> successors = depGraph.outgoingEdgesOf(tree);
                Set<Arc> tmp = new LinkedHashSet<Arc>();
                for(Arc a : successors){
                    if(a.v != null && a.v == tree.params.get(i).sym){
                        tmp.add(a);
                    }
                }
                paramSuccessorMapping.put(i, tmp);
                params.put(i, tree.params.get(i).sym);
            }
        }
        
    }

    private boolean                mustReturnSomething;
    private boolean                invocationAlreadyVisited;
    private Map<JCTree, JCIdent>   invocationReplacements;   
    
    @Override
    public void visitApply(JCTree.JCMethodInvocation tree) {
        JCTree.JCIdent meth = (JCTree.JCIdent)tree.meth;
        //found a method invocation, check if it is a recursive call
        if(meth.sym.flatName().equals(methodName.flatName()) ){
            recursiveCall = true;
            mustReturnSomething = false;
            invocationAlreadyVisited = true;
            //replace the invocation with the return value or remove
            //scanStat(currentStatement);
            if(mustReturnSomething){
                //prepare for replacement
                invocationReplacements.put(currentStatement, recReturnValueVariable);
            }
            invocationAlreadyVisited = false;
            recursiveCall = false;
        }
    }

    @Override
    public void visitAssign(JCTree.JCAssign tree) {
        if(recursiveCall == true){
            mustReturnSomething = true;
            scan(tree.lhs);
            if(!invocationAlreadyVisited){
                scan(tree.rhs);
            }
        }
        else{
            //just an usual assignment
            super.visitAssign(tree);
        }
    }
    
    private JCIdent recReturnValueVariable;
            
    @Override
    public void visitIdent(JCIdent tree) {
        if(mustReturnSomething && recursiveCall){
            recReturnValueVariable = tree;
            treeTaskMapping.put(recReturnValueVariable, currentTaskSet);
        }
        super.visitIdent(tree);    
    }
    
    
    private JCTree.JCSkip skipNode;
    private JCTree        skipStatment;

    private Map<Integer, JCTree> returnStatements;
    
    @Override
    public void visitReturn(JCTree.JCReturn tree) {
        currentStatement = tree;
        currentTaskSet = treeTaskMapping.get(lookup.get(currentStatement));
        returnStatements.put(tree.pos, tree);
        super.visitReturn(tree);
    }
    
    
    /** 
     * Analyze list of statements.
     */
    public void scanStats(List<? extends JCTree.JCStatement> trees) {
        if (trees != null)
            for (List<? extends JCTree.JCStatement> l = trees; l.nonEmpty(); l = l.tail)
                scanStat(l.head);
    }
    
    
    /** Analyze a statement. Check that statement is reachable.
     */
    public void scanStat(JCTree tree) {
        currentStatement=(JCTree.JCStatement)tree;
        currentTaskSet = treeTaskMapping.get(lookup.get(currentStatement));
        scan(tree);
    }
    
    @Override
    public void visitBlock(JCTree.JCBlock tree) {
        scanStats(tree.stats);
        super.visitBlock(tree);
    }
    
    private Set<JCTree> varDecls;
    
    @Override
    public void visitVarDef(JCTree.JCVariableDecl tree) {
        varDecls.add(tree);
        super.visitVarDef(tree);
    }
    
    
    
    /**
     * ************************************************************************
     * main method - taken from RefGen
     * ************************************************************************
     */
    public void analyzeTree(JCTree tree) {
        try {

            scan(tree);

        } finally {
        }
    }

    /**
     * Getters
     */
    public DirectedWeightedMultigraph<JCTree, Arc> getDepGraph() {
        return depGraph;
    }
    
    public SimpleDirectedGraph<TaskSet, TaskArc> getTaskGraph(){
        return taskGraph;
    }

    public Map<Integer, Set<Arc>> getParamSuccessorMapping() {
        return paramSuccessorMapping;
    }

    public JCTree.JCSkip getSkipStatement() {
        return skipNode;
    }

    public Map<Integer, JCTree> getReturnStatements() {
        return returnStatements;
    }
    
    public Set<JCTree> getVarDecls(){
        return varDecls;
    }
    
    public Map<Integer, VarSymbol> getParams(){
        return params;
    }
    
    public JCMethodDecl getMethodDeclaration(){
        return methodDeclaration;
    }
    
    public Set<JCTree> getJoinNodes(){
        return newJoinNodes;
    }
    
    
    public Map<JCTree, TaskSet> getTaskMapping(){
        return treeTaskMapping;
    }

}
