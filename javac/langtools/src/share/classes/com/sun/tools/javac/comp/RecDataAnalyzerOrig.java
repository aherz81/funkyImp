/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.Arc;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCSkip;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.TreeCopier;
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
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 *
 * @author Andreas Wagner
 */
public class RecDataAnalyzerOrig extends TreeScanner {
    
    private       TreeMaker make;
    
    protected static final Context.Key<RecDataAnalyzerOrig> RecDataKey = 
            new Context.Key<RecDataAnalyzerOrig>();

    
    public static RecDataAnalyzerOrig instance(Context context) {
        RecDataAnalyzerOrig instance = context.get(RecDataKey);
        if (instance == null)
            instance = new RecDataAnalyzerOrig(context);
        return instance;
    }
    
    protected RecDataAnalyzerOrig(Context context) {
        context.put(RecDataKey, this);
        make = TreeMaker.instance(context);
    }
    
    private int                                     numArgs;
    private JCMethodDecl                            methodDeclaration;
    private TaskSet                                 methodDeclarationTS;
    private JCStatement                             currentStatement;
    private MethodSymbol                            methodName;
    private Map<Integer, VarSymbol>                 positionToParam;
    private boolean                                 recursiveCall;
    private DirectedWeightedMultigraph<JCTree, Arc> depGraph;
    private SimpleDirectedGraph<TaskSet, TaskArc>   taskGraph;
    private Map<JCTree, TaskSet>                    treeTaskMapping;
    public Map<TaskSet, TaskSet>                    originalTaskMapping;
    
    @Override
    public void visitMethodDef(JCMethodDecl tree){
        if(tree.sym.mayBeRecursive){
            //obtain some method info
            numArgs = tree.params.size();
            methodDeclaration = tree;
            skipNode = tree.dg_end;
            methodName = tree.sym;
            positionToParam = new LinkedHashMap<Integer, VarSymbol>();
            returnTaskSets = new LinkedHashMap<Integer, TaskSet>();
            incomingArgumentDependencies = new LinkedHashMap<Integer, Set<Arc>>();
            recursiveCall = false;
            depGraph = tree.depGraph;
            methodDeclarationTS = currentTaskSet;
            treeTaskMapping = new LinkedHashMap<JCTree, TaskSet>();
            
            returnStatements = new LinkedHashMap<Integer, JCTree>();

            for(int i = 0; i < numArgs; i++){
                positionToParam.put(i, tree.params.get(i).sym);
            }
            
            //clone hasse diagram
            TaskGraphCloner tgc = new TaskGraphCloner();
            taskGraph = tgc.clone(tree, make);
            originalTaskMapping = tgc.oldNew;
            
            
            //Create a map from tree to task
            for (TaskSet task : taskGraph.vertexSet()){
                for (Iterator<JCTree> it = task.iterator(); it.hasNext();) {
                    JCTree t = it.next();
                    treeTaskMapping.put(t, task);
                }
            }
            
            //Reestablish transitive edges and calculate variable dependencies
            for(JCTree v : depGraph.vertexSet()){
                //if(v == tree) continue;
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
            
            skipNodeTS = treeTaskMapping.get(methodDeclaration.dg_end);
            //
            //scan the whole method-body
            scanStat(tree.body);
            
        }
    }
    
    private JCMethodInvocation          invocationNode;
    private TaskSet                     invocationTaskSets;
    private JCStatement                 invocationStatement;
    private Set<VarSymbol>              argumentDependencies;
    private Map<Integer, Set<Arc>>      incomingArgumentDependencies;
    private Map<Integer, JCExpression>  argumentExpressionMapping;
    private Set<Arc>                    followingNodes;
    
    
    @Override
    public void visitApply(JCMethodInvocation tree) {
        JCIdent meth = (JCTree.JCIdent)tree.meth;
        //found a method invocation, check if it is a recursive call
        if(meth.sym.flatName().equals(methodName.flatName())){
            //this is a recursive call
            recursiveCall = true;
            invocationNode = tree;
            invocationStatement = currentStatement;
            invocationTaskSets = currentTaskSet;
            argumentExpressionMapping = new LinkedHashMap<Integer, JCExpression>();
            followingNodes = new LinkedHashSet<Arc>();
            //check each argument of the call
            for(int i = 0; i < tree.args.size(); i++){
                argumentDependencies = new LinkedHashSet<VarSymbol>();
                JCExpression expr = tree.args.get(i);
                scan(expr);
                //dependencies are in argumentDependencies
                Set<Arc> incoming = depGraph.incomingEdgesOf(methodDeclaration.cloneAssociationMap.get(invocationStatement));
                Set<Arc> dependencies = new LinkedHashSet<Arc>();
                for(Arc a : incoming){
                    //check if edge is an dependency for current expression
                    if(a.v != null && argumentDependencies.contains(a.v)){
                        dependencies.add(a);
                    }
                }
                //add dependencies to overall mapping
                incomingArgumentDependencies.put(i, dependencies);
                argumentExpressionMapping.put(i, expr);
                Set<Arc> tmp = depGraph.outgoingEdgesOf(methodDeclaration.cloneAssociationMap.get(currentStatement));
                for(Arc a : tmp){
                    followingNodes.add(a);
                }
            }
            recursiveCall = false;
        }
    }

    private JCSkip      skipNode;
    private TaskSet     skipNodeTS;
    private JCStatement skipStatment;
    private TaskSet     skipStatmentTS;
    private Map<Integer, JCTree> returnStatements;
    private Map<Integer, TaskSet> returnTaskSets;
    
    
    @Override
    public void visitReturn(JCReturn tree) {
        returnStatements.put(tree.pos, tree);
        returnTaskSets.put(tree.pos, currentTaskSet);
        currentStatement = tree;
        currentTaskSet = treeTaskMapping.get(methodDeclaration.cloneAssociationMap.get(currentStatement));
        super.visitReturn(tree);
        
    }
   
    @Override
    public void visitIdent(JCIdent tree) {
        if(recursiveCall){
            argumentDependencies.add((VarSymbol)tree.sym);
        }
        super.visitIdent(tree);
    }
   
    /** Analyze list of statements.
     */
        public void scanStats(List<? extends JCTree.JCStatement> trees) {
        if (trees != null)
            for (List<? extends JCTree.JCStatement> l = trees; l.nonEmpty(); l = l.tail)
                scanStat(l.head);
    }
    
    
    /** Analyze a statement. Check that statement is reachable.
     */
    private TaskSet currentTaskSet;    
    
    public void scanStat(JCTree tree) {
        currentStatement=(JCTree.JCStatement)tree;
        currentTaskSet = treeTaskMapping.get(methodDeclaration.cloneAssociationMap.get(currentStatement));
        scan(tree);
    }
    
    @Override
    public void visitBlock(JCTree.JCBlock tree) {
        scanStats(tree.stats);
        super.visitBlock(tree);
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
     * getters
     */
    public int getNumArgs() {
        return numArgs;
    }

    public Map<Integer, VarSymbol> getPositionToParam() {
        return positionToParam;
    }

    public TaskSet getInvocationTaskSet() {
        return invocationTaskSets;
    }
    
    public JCTree getInvocationStatement(){
        return invocationStatement;
    }

    public Map<Integer, Set<Arc>> getIncomingArgumentDependencies() {
        return incomingArgumentDependencies;
    }

    public Map<Integer, JCExpression> getArgumentExpressionMapping() {
        return argumentExpressionMapping;
    }

    public Set<Arc> getFollowingNodes() {
        return followingNodes;
    }

    public TaskSet getSkipTaskSet() {
        return skipNodeTS;
    }
    
    public JCTree getSkipStatement(){
        return methodDeclaration.dg_end;
    }

    public Map<Integer, JCTree> getReturnStatements() {
        return returnStatements;
    }
    
    public Map<Integer, TaskSet> getReturnTaskSets(){
        return returnTaskSets;
    }
    
    public DirectedWeightedMultigraph<JCTree, Arc> getDepGraph() {
        return depGraph;
    }
    
    public SimpleDirectedGraph<TaskSet, TaskArc> getTaskGraph(){
        return taskGraph;
    }
    
    public JCMethodDecl getMethodDeclaration(){
        return methodDeclaration;
    }
    
    public TaskSet getMethodDeclarationTaskSet(){
        return methodDeclarationTS;
    }
    
    public Map<JCTree, TaskSet> getTaskMapping(){
        return treeTaskMapping;
    }
    
}
