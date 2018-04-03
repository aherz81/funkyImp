/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.util;

/**
 *
 * @author aherz
 */
import java.util.*;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Symbol;

import org.jgrapht.graph.*;

import com.sun.tools.javac.jvm.Code;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeInfo;


//a PathSet is a task (and has no un-fullfilled self dependencies)

public interface iTask
{

    public boolean hasTransitiveReturns();
    
    public void removeNode(JCTree item);

	public boolean containsForcedSpawn();

	//do not merge blocking paths, might have unknown dependencies
	public boolean containsBlocking();

	//do not merge blocking paths, might have unknown dependencies
	public boolean containsReturn();

	public void removeThread(String thread);

	public boolean containsThread();

	public Set<String> getGroups();

	public Set<String> getThreads();

	//topol smallest calc node
	public JCTree getFirstCalcNode(JCMethodDecl method,Set<JCTree> cn);

	//topol largest calc node
	public JCTree getLastCalcNode(JCMethodDecl method,Set<JCTree> cn);

    public boolean containsNode(JCTree node);

	//get topol smallest node (may be non-calc node)
    public JCTree getFirstFromPathSet();

	//get topol largest node (may be non-calc node)
    public JCTree getLastFromPathSet();

    public Set<JCTree> getCalcNodes();

	public Set<JCTree> getCalcNodesTransitive();

    public String toString();

    public JCBlock getPathBlock();

    public JCBlock getPathBlockNoCache();

	public JCBlock getPathBlockFromSet(Set<JCTree> set,final JCMethodDecl md);

    public Set<JCTree> getInCom();

    //public Set<VarSymbol> getInSymbols(JCTree v,Set<JCTree> calcNodes);

	public Set<VarSymbol> getInSymbolsImplicit();

    public Set<VarSymbol> getInSymbols();

    public boolean isJoining();

    public boolean isFinal();

    public Set<JCTree> getOutCom();

    public Set<JCTree> getTargets();

    //public Set<VarSymbol> getOutSymbols(JCTree v,Set<JCTree> calcNodes);

	//com symbols without uninitialized vars (avoid unnecessray notifications)
    public Set<VarSymbol> getNullFreeOutSymbols();

    public Set<VarSymbol> getOutSymbols();

    //public Set<JCTree> getInnerCom(Set<JCTree> calcNodes,JCMethodDecl method);

    public Set<VarSymbol> getInnerSymbols(Set<JCTree> calcNodes,JCMethodDecl method);

    public int getCom(); //accum var size in byte

	public boolean isCFDEPTo(JCTree root);

	public Set<JCTree> getInComImplicit();

	public Map<VarSymbol,JCTree> getOutComMap();

}
