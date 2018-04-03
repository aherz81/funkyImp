/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.iTask;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author andreas wagner
 */
public interface ILowerTasks {

    void genMethodBodyCore(JCTree scheduler, boolean no_wait, JCTree.JCMethodDecl tree, Set<iTask> paths) throws IOException;

    //alloc and spawn tasks (use dby schedulers, like state.method, if/else branch, other control flow)
    void genMethodTGBody(JCTree scheduler, boolean no_wait, JCTree.JCMethodDecl tree, Set<iTask> paths) throws IOException;

    //alloc and spawn tasks (used by schedulers, like state.method, if/else branch, other control flow)
    void genMethodTGBodyCore(JCTree scheduler, boolean no_wait, JCTree.JCMethodDecl tree, Set<iTask> paths, boolean do_kernel) throws IOException;

    void genMethodTGFooter(JCTree.JCMethodDecl tree) throws IOException;

    //emit state.method entry (allocate and set context)
    void genMethodTGHeader(JCTree.JCMethodDecl tree, JCTree scheduler, boolean init_self, boolean label) throws IOException;

    //how many tasks are spawned by scheduler?
    int getSchedulerPaths(JCTree scheduler, Set<iTask> paths, boolean uncoditional);

    int hasSchedulerPaths(Set<iTask> paths);

    //!! process output of PathGen, sets up all kinds of maps so we can find out which tasks must start which other tasks under which conditions
    //also prints the task
    Set<iTask> preparePaths(JCTree.JCMethodDecl tree) throws IOException;

    //context is a struct with 3 parts: state.method parameters, (task-shared) local params and task handles
    void printContext(JCTree.JCMethodDecl tree, Set<Symbol.VarSymbol> com, Set<iTask> waiting_tasks);
    
    //spawn tasks that depend on result of tree
    void SpawnDependentTasks(JCTree tree) throws IOException;
    
            //emit task
    JCTree printTask(JCTree.JCMethodDecl tree, iTask p, JCTree.JCBlock b, String name, Set<Symbol.VarSymbol> inner, boolean dangling);

    void visitIf(JCTree.JCIf tree);
    
}
