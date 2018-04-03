/*
 * Copyright 1999-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

//generate addrefs for: events, return of local variable, parallel path without join
//generate release for: last usage of symbol, if symbol is heap allocated (has any addrefs)
//allow dest update on last usage??
//make retval non-linear if it depends on addref symbol
//first usage (pers ds)

package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.jvm.Code;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.TypeTags.*;
import com.sun.tools.javac.main.JavaCompiler;

import java.util.Hashtable;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;

import org.jgrapht.*;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;

/**
 * here we calc transitively whether a method is blocking or calls a blocking or sampling method
 * FIXME: generate call graph and operate on that
 * @author aherz
 */


public class RefGen extends TreeScanner {
    protected static final Context.Key<RefGen> RefGenKey =
        new Context.Key<RefGen>();

    private final Names names;
    private final Log log;
    private final Symtab syms;
    private final Types types;
    private final Check chk;
    private       TreeMaker make;
    private       Lint lint;

//	private JCMethodDecl recursion=null;
    private Code code;

    private JCMethodDecl method;

	private boolean inside_return=false;
	private boolean inside_where=false;

	public DirectedGraph<JCTree, Arc> callGraph;
	public java.util.Map<JCTree,Integer> topolNodes;

    public static RefGen instance(Context context) {
        RefGen instance = context.get(RefGenKey);
        if (instance == null)
            instance = new RefGen(context);
        return instance;
    }

    protected RefGen(Context context) {
        context.put(RefGenKey, this);

        names = Names.instance(context);
        log = Log.instance(context);
        syms = Symtab.instance(context);
        types = Types.instance(context);
        chk = Check.instance(context);
        lint = Lint.instance(context);


		callGraph=JavaCompiler.instance(context).callGraph;
		topolNodes=JavaCompiler.instance(context).topolNodes;
        //code = Code.
    }

/* ************************************************************************
 * Visitor methods for statements and definitions
 *************************************************************************/

    private JCTree current_stat;

    /** Analyze a statement. Check that statement is reachable.
     */
    void scanStat(JCTree tree) {

        current_stat=(JCStatement)tree;
        scan(tree);
		current_stat=null;
    }

    /** Analyze list of statements.
     */
    void scanStats(List<? extends JCStatement> trees) {
        if (trees != null)
            for (List<? extends JCStatement> l = trees; l.nonEmpty(); l = l.tail)
                scanStat(l.head);
    }

    /** Analyze an expression. Make sure to set (un)inits rather than
     *  (un)initsWhenTrue(WhenFalse) on exit.
     */
    void scanExpr(JCTree tree) {
        if (tree != null) {
            scan(tree);
        }
    }

    /** Analyze a list of expressions.
     */
    void scanExprs(List<? extends JCExpression> trees) {
        if (trees != null)
            for (List<? extends JCExpression> l = trees; l.nonEmpty(); l = l.tail)
                scanExpr(l.head);
    }

    /** Analyze a condition. Make sure to set (un)initsWhenTrue(WhenFalse)
     *  rather than (un)inits on exit.
     */
    void scanCond(JCTree tree) {
            scan(tree);
    }

    /* ------------ Visitor methods for various sorts of trees -------------*/

    public void visitBlock(JCBlock tree) {
        scanStats(tree.stats);
    }

    public void visitMethodDef(JCMethodDecl tree) {

        if (!tree.analyse()) return;
        JCMethodDecl prevMethod = method;
        method=tree;

		method.sym.isSampling|=(method.sym.getReturnType() != null&&method.sym.name != method.sym.name.table.names.init&&(method.sym.owner.flags() & Flags.SINGULAR) != 0)|method.sym.isSampling;
		method.sym.isBlocking|=(method.sym.flags_field&Flags.BLOCKING)!=0;

		if((method.sym.flags_field&Flags.NONBLOCKING)!=0&&(method.sym.flags_field&Flags.LOOP)==0)
		{
			log.error(tree.pos,"nonblocking.nonloop",tree.name);
		}

		callGraph.addVertex(tree.sym.decl);

        try
        {
            scanStat(tree.body);
        }
        finally
        {
            method=prevMethod;
        }
    }


    public void visitReturn(JCReturn tree) {
		inside_return=true;
        scanExpr(tree.expr);
		inside_return=false;
    }

    public void visitWhere(JCWhere tree) {
        scanExpr(tree.exp);
        boolean insideWherePrev=inside_where;
        inside_where=true;
        scanExpr(tree.sexp);
        inside_where=insideWherePrev;
    }

    JCMethodDecl recTestStart=null;

    public void visitApply(JCMethodInvocation tree) {

		if(method==null)
			return;
		//FIXME: build call graph and traverse that!!

		boolean old_blocking=method.sym.isBlocking;
		//boolean old_sampling=method.sym.isSampling;

		boolean old_inside_where = inside_where;
		boolean old_inside_return = inside_return;

        MethodSymbol meth = (MethodSymbol)TreeInfo.symbol(tree.meth);

		//do interprocedural check for blocking, sampling and recursion:

		boolean inside_event = (method.restype == null&&method.name != method.name.table.names.init);
		boolean inside_sample = (method.restype != null&&method.name != method.name.table.names.init&&(method.sym.owner.flags() & Flags.SINGULAR) != 0);

		method.sym.isSampling|=meth.isSampling;

		if(!(inside_event&&tree.getTriggerReturn()!=null))//trigger set is not actually bocking
			method.sym.isBlocking|=meth.isBlocking;


		for(Iterator<JCTree> impl=meth.implementations.iterator();impl.hasNext();)
		{
			JCMethodDecl md=(JCMethodDecl)impl.next();

			if((meth.flags_field&Flags.BLOCKING)!=0&&(meth.owner.flags_field&Flags.NATIVE)!=0)
			{
				((ClassSymbol)method.sym.owner).ffContext.add(md);
			}

			if(md.sym.decl!=null)
			{
				if(!callGraph.vertexSet().contains(md.sym.decl))
				{
					callGraph.addVertex(md.sym.decl);
	                scanExpr(md);
				}
				callGraph.addEdge(method.sym.decl, md.sym.decl,new Arc(method.sym.decl,md.sym.decl,(VarSymbol)null));
			}
		}

/*
        if(recTestStart==null&&!method.sym.mayBeRecursive)
        {
            recTestStart=method;

            for(Iterator<JCTree> impl=meth.implementations.iterator();impl.hasNext();)
            {
                JCMethodDecl md=(JCMethodDecl)impl.next();

                //recursion=md;
                scanExpr(md);
            }
            recTestStart=null;
        }
        else if(method==recTestStart&&!method.sym.mayBeRecursive)
        {
            method.sym.mayBeRecursive=true;
        }
*/
		method.sym.isSampling|=meth.isSampling;

		if(!(inside_event&&tree.getTriggerReturn()!=null))//trigger set is not actually bocking
		{
			method.sym.isBlocking|=meth.isBlocking|(meth.flags_field&Flags.BLOCKING)!=0;
			if(meth.isBlocking|(meth.flags_field&Flags.BLOCKING)!=0&&current_stat!=null)
				current_stat.is_blocking=true;
		}

		//boolean inside_event = (method.restype == null&&method.name != method.name.table.names.init);
		inside_where = old_inside_where;
		inside_return = old_inside_return;

		if(inside_event)
		{
			if(old_blocking!=method.sym.isBlocking)
				log.error(tree.pos,"blocking.inside.event",tree);
			if(inside_return&&inside_where&&method.sym.isSampling)//FORBID METH CALLS IN WHERE COMPLETELY???
				log.error(tree.pos,"sampling.inside.event.where",tree);
		}
		if(inside_sample)
		{
			if(old_blocking!=method.sym.isBlocking)
				log.error(tree.pos,"blocking.inside.sample",tree);
		}
    }


/**************************************************************************
 * main method
 *************************************************************************/

    /** Perform definite assignment/unassignment analysis on a tree.
     */
    public void analyzeTree(JCTree tree) {
        try {
			//FIXME: needed??

            scan(tree);

        } finally {
        }
    }
}
