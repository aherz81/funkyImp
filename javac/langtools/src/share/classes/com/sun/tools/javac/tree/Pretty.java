/*
 * Copyright 1999-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.javac.tree;

import java.io.*;
import java.util.*;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.code.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.comp.Work;

import static com.sun.tools.javac.code.Flags.*;

import org.jgrapht.*;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;


/** Prints out a tree as an indented Java source program.
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Pretty extends JCTree.Visitor {

    Work work;
	JCMethodDecl method=null;
	Map<VarSymbol,String> subst=null;

    public Pretty(Writer out, boolean sourceOutput,boolean dump_analysis,boolean fake) {
        this.out = out;
        this.sourceOutput = sourceOutput;
        this.work=null;
        this.dump_analysis=dump_analysis;
		this.subst=subst;
    }

    public Pretty(Writer out, boolean sourceOutput,boolean dump_analysis,boolean fake,Map<VarSymbol,String> subst) {
        this.out = out;
        this.sourceOutput = sourceOutput;
        this.work=null;
        this.dump_analysis=dump_analysis;
		this.subst=subst;
    }

    public Pretty(Writer out, boolean sourceOutput,boolean dump_graph, boolean allownl,Work work,boolean dump_analysis) {
        this.out = out;
        this.sourceOutput = sourceOutput;
        this.dump_graph = dump_graph;
        this.allownl = allownl;
        this.work=work;
        this.dump_analysis=dump_analysis;
    }

    public Pretty(Writer out, boolean sourceOutput, boolean allownl) {
        this.out = out;
        this.sourceOutput = sourceOutput;
        this.allownl = allownl;
        this.work=null;
    }

	public Pretty(Writer out, boolean deep) {
        this.out = out;
        this.allownl = true;
        this.deep = deep;
		this.sourceOutput=false;
    }

    private boolean dump_graph=false;
    private boolean dump_analysis=false;


    /** Set when we are producing source output.  If we're not
     *  producing source output, we can sometimes give more detail in
     *  the output even though that detail would not be valid java
     *  soruce.
     */
    private final boolean sourceOutput;

    String class_name;

    boolean allownl = true;
	boolean deep=false;
    /** The output stream on which trees are printed.
     */
    Writer out;

    /** Indentation width (can be reassigned from outside).
     */
    public int width = 4;

    /** The current left margin.
     */
    int lmargin = 0;

    /** The enclosing class name.
     */
    Name enclClassName;

    /** A hashtable mapping trees to their documentation comments
     *  (can be null)
     */
    Map<JCTree, String> docComments = null;

    /** Align code to be indented to left margin.
     */
    void align() throws IOException {
        if(dump_graph)return;
        for (int i = 0; i < lmargin; i++) out.write(" ");
    }

    /** Increase left margin by indentation width.
     */
    void indent() {
        lmargin = lmargin + width;
    }

    /** Decrease left margin by indentation width.
     */
    void undent() {
        lmargin = lmargin - width;
    }

    /** Enter a new precedence level. Emit a `(' if new precedence level
     *  is less than precedence level so far.
     *  @param contextPrec    The precedence level in force so far.
     *  @param ownPrec        The new precedence level.
     */
    void open(int contextPrec, int ownPrec) throws IOException {
        if(dump_graph)return;
        if (ownPrec < contextPrec) out.write("(");
    }

    /** Leave precedence level. Emit a `(' if inner precedence level
     *  is less than precedence level we revert to.
     *  @param contextPrec    The precedence level we revert to.
     *  @param ownPrec        The inner precedence level.
     */
    void close(int contextPrec, int ownPrec) throws IOException {
        if(dump_graph)return;
        if (ownPrec < contextPrec) out.write(")");
    }

    /** Print string, replacing all non-ascii character with unicode escapes.
     */
    public void print(Object s) throws IOException {
        if(dump_graph)return;
        out.write(Convert.escapeUnicode(s.toString()));
    }

    /** Print new line.
     */
    public void println() throws IOException {
        if(dump_graph)return;
        if(allownl)
            out.write(lineSep);
    }

    /** Print string, replacing all non-ascii character with unicode escapes.
     */
    public void dgprint(Object s) throws IOException {
        if(!dump_graph)return;
        out.write(Convert.escapeUnicode(s.toString()));
    }

    /** Print new line.
     */
    public void dgprintln(Object s) throws IOException {
        if(!dump_graph)return;
        dgprint(s);
        out.write(lineSep);
    }

    String lineSep = System.getProperty("line.separator");

    /**************************************************************************
     * Traversal methods
     *************************************************************************/

    /** Exception to propogate IOException through visitXXX methods */
    private static class UncheckedIOException extends Error {
        static final long serialVersionUID = -4032692679158424751L;
        UncheckedIOException(IOException e) {
            super(e.getMessage(), e);
        }
    }

    /** Visitor argument: the current precedence level.
     */
    int prec;

    /** Visitor method: print expression tree.
     *  @param prec  The current precedence level.
     */
    public void printExpr(JCTree tree, int prec) throws IOException {
        int prevPrec = this.prec;
        try {
            this.prec = prec;
            if (tree == null) print("/*missing*/");
            else {
                tree.accept(this);
            }
        } catch (UncheckedIOException ex) {
            IOException e = new IOException(ex.getMessage());
            e.initCause(ex);
            throw e;
        } finally {
            this.prec = prevPrec;
        }
    }

    /** Derived visitor method: print expression tree at minimum precedence level
     *  for expression.
     */
    public void printExpr(JCTree tree) throws IOException {
        printExpr(tree, TreeInfo.noPrec);
    }

	private String getNode(JCTree tree,DirectedWeightedMultigraph<JCTree, Arc> depGraph)
	{
		return "N"+Math.abs(depGraph.hashCode())+"_"+Math.abs(tree.hashCode());
	}

    private String getNodeDef(JCTree tree,DirectedWeightedMultigraph<JCTree, Arc> depGraph)
    {
        return  getNode(tree,depGraph) +" [label=\""+tree.toFlatString()+"\""+"]";
    }

    private void DumpGraphNodeJGT(DirectedWeightedMultigraph<JCTree, Arc> depGraph,JCTree tree) throws IOException
    {
        if(!dump_graph)return;
        Set<Arc> childs=depGraph.outgoingEdgesOf(tree);
        if(childs!=null)
        {
            Iterator<Arc> itr=childs.iterator();
            while(itr.hasNext())
            {
                Arc gen = itr.next();

                if(gen.s.getTag()==JCTree.IF)
                {
                    dgprintln(getNode(gen.s,depGraph)+"[shape=diamond]");
                }

                if(tree instanceof JCMethodDecl)
                {
                    //dgprint("\""+"("+tree.pos().getPreferredPosition()+") "+((JCMethodDecl)tree).getStringHeader()+"\"" + "->" + getNode(gen.t));
                    dgprint(getNode(tree,depGraph) + "->" + getNode(gen.t,depGraph));
                    if(gen.t.getDGNode().source_connected&&gen.v==null)
                        dgprintln(" [style=dashed,color=red] ");
                    else if(gen.v==null)
                        if(tree instanceof JCCF)
                            dgprintln(" [color=grey,label=CF] ");
                        else
                            dgprintln(" [color=grey,label=CFD] ");
                    else
                        dgprintln(" [color=grey,label=\""+gen.v.name.toString()+"\"] ");
                }
                else
                {
					dgprint(getNode(tree,depGraph) + "->" + getNode(gen.t,depGraph));

                    if(tree.getDGNode().target_connected&&gen.v==null)
                        dgprintln(" [style=dashed,color=red] ");
                    else if(gen.v==null)
                    {
                        if(tree instanceof JCCF)
                            dgprintln(" [color=grey,label=CF] ");
                        else
                            dgprintln(" [color=grey,label=CFD] ");
                    }
                    else
                        dgprintln(" [color=grey,label=\""+gen.v.name.toString()+"\"] ");
                }
            }
        }
    }

    /** Derived visitor method: print statement tree.
     */
    public void printStat(JCTree tree) throws IOException {

        printExpr(tree, TreeInfo.notExpression);

    }

    /** Derived visitor method: print list of expression trees, separated by given string.
     *  @param sep the separator string
     */
    public <T extends JCTree> void printExprs(List<T> trees, String sep) throws IOException {
        if (trees.nonEmpty()) {
            printExpr(trees.head);
            for (List<T> l = trees.tail; l.nonEmpty(); l = l.tail) {
                print(sep);
                printExpr(l.head);
            }
        }
    }

    /** Derived visitor method: print list of expression trees, separated by commas.
     */
    public <T extends JCTree> void printExprs(List<T> trees) throws IOException {
        printExprs(trees, ", ");
    }

    /** Derived visitor method: print list of statements, each on a separate line.
     */
    public void printStats(List<? extends JCTree> trees) throws IOException {
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail) {
            align();
            printStat(l.head);
            println();
        }
    }

    /** Print a set of modifiers.
     */
    public void printFlags(long flags) throws IOException {
        if ((flags & SYNTHETIC) != 0) print("/*synthetic*/ ");
        print(TreeInfo.flagNames(flags));
        if ((flags & StandardFlags) != 0) print(" ");
        if ((flags & ANNOTATION) != 0) print("@");
    }

    public void printAnnotations(List<JCAnnotation> trees) throws IOException {
        for (List<JCAnnotation> l = trees; l.nonEmpty(); l = l.tail) {
            printStat(l.head);
            println();
            align();
        }
    }

    /** Print documentation comment, if it exists
     *  @param tree    The tree for which a documentation comment should be printed.
     */
    public void printDocComment(JCTree tree) throws IOException {
        if (docComments != null) {
            String dc = docComments.get(tree);
            if (dc != null) {
                print("/**"); println();
                int pos = 0;
                int endpos = lineEndPos(dc, pos);
                while (pos < dc.length()) {
                    align();
                    print(" *");
                    if (pos < dc.length() && dc.charAt(pos) > ' ') print(" ");
                    print(dc.substring(pos, endpos)); println();
                    pos = endpos + 1;
                    endpos = lineEndPos(dc, pos);
                }
                align(); print(" */"); println();
                align();
            }
        }
    }
//where
    static int lineEndPos(String s, int start) {
        int pos = s.indexOf('\n', start);
        if (pos < 0) pos = s.length();
        return pos;
    }

    /** If type parameter list is non-empty, print it enclosed in "<...>" brackets.
     */
    public void printTypeParameters(List<JCTypeParameter> trees) throws IOException {
        if (trees.nonEmpty()) {
            print("<");
            printExprs(trees);
            print(">");
        }
    }

    /** Print a block.
     */
    public void printBlock(List<? extends JCTree> stats) throws IOException {
        print("{");
        println();
        indent();
        printStats(stats);
        undent();
        align();
        print("}");
    }

    /** Print a block.
     */
    public void printEnumBody(List<JCTree> stats) throws IOException {
        print("{");
        println();
        indent();
        boolean first = true;
        for (List<JCTree> l = stats; l.nonEmpty(); l = l.tail) {
            if (isEnumerator(l.head)) {
                if (!first) {
                    print(",");
                    println();
                }
                align();
                printStat(l.head);
                first = false;
            }
        }
        print(";");
        println();
        for (List<JCTree> l = stats; l.nonEmpty(); l = l.tail) {
            if (!isEnumerator(l.head)) {
                align();
                printStat(l.head);
                println();
            }
        }
        undent();
        align();
        print("}");
    }

    /** Is the given tree an enumerator definition? */
    boolean isEnumerator(JCTree t) {
        return t.getTag() == JCTree.VARDEF && (((JCVariableDecl) t).mods.flags & ENUM) != 0;
    }

    /** Print unit consisting of package clause and import statements in toplevel,
     *  followed by class definition. if class definition == null,
     *  print all definitions in toplevel.
     *  @param tree     The toplevel tree
     *  @param cdef     The class definition, which is assumed to be part of the
     *                  toplevel tree.
     */
    public void printUnit(JCCompilationUnit tree, JCClassDecl cdef) throws IOException {

        if(dump_graph)
        {
            dgprintln("digraph "+cdef.name.toString()+" {\n");
            dgprintln("node [shape=box]");
        }

		if(cdef!=null)
			class_name=cdef.sym.flatname.toString();
		else
			class_name="unkown";
        docComments = tree.docComments;
        printDocComment(tree);
        if (tree.pid != null) {
            print("package ");
            printExpr(tree.pid);
            print(";");
            println();
        }
        boolean firstImport = true;
        for (List<JCTree> l = tree.defs;
        l.nonEmpty() && (cdef == null || l.head.getTag() == JCTree.IMPORT
                || l.head.getTag() == JCTree.DOMDEF
                || l.head.getTag() == JCTree.CLASSDEF);
        l = l.tail) {
            if (l.head.getTag() == JCTree.IMPORT) {
                JCImport imp = (JCImport)l.head;
                Name name = TreeInfo.name(imp.qualid);
                if (name == name.table.names.asterisk ||
                        cdef == null ||
                        isUsed(TreeInfo.symbol(imp.qualid), cdef)) {
                    if (firstImport) {
                        firstImport = false;
                        println();
                    }
                    printStat(imp);
                }
            } else {
                if(l.head.getTag() != JCTree.CLASSDEF)
                    printStat(l.head);
            }
        }
        if (cdef != null) {
            printStat(cdef);
            println();
        }

        if(dump_graph)
        {
            dgprintln("\n}");
        }

    }
    // where
    boolean isUsed(final Symbol t, JCTree cdef) {
        class UsedVisitor extends TreeScanner {
            public void scan(JCTree tree) {
                if (tree!=null && !result) tree.accept(this);
            }
            boolean result = false;
            public void visitIdent(JCIdent tree) {
                if (tree.sym == t) result = true;
            }
        }
        UsedVisitor v = new UsedVisitor();
        v.scan(cdef);
        return v.result;
    }

    /**************************************************************************
     * Visitor methods
     *************************************************************************/

    public void visitTopLevel(JCCompilationUnit tree) {
        try {
            printUnit(tree, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitImport(JCImport tree) {
        try {
            print("import ");
            if (tree.staticImport) print("static ");
            printExpr(tree.qualid);
            print(";");
            println();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitCTProperty(JCCTProperty tree) {
        try {
            printExpr(tree.exp);
            print("@");
            print(tree.name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

	public void visitDomConstraintValue(JCDomConstraintValue dcv)
	{
		try{
			if((dcv.coeff != null||dcv.parameter1 != null) && dcv.parameter != null) {
                print((dcv.sign == 1) ? "+ " : "- ");
				if(dcv.parameter1==null)
					printExpr(dcv.coeff);
				else
					printExpr(dcv.parameter1);
                print("*");
				printExpr(dcv.parameter);
            }
            else if(dcv.coeff != null) {
                print((dcv.sign == 1) ? "+ " : "- ");
                printExpr(dcv.coeff);
            }
            else if(dcv.parameter != null) {
                print((dcv.sign == 1) ? "+ " : "- ");
                printExpr(dcv.parameter);
            }
            else {
                print("<invalid>");
            }
		}
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
	}
	public void visitJoin(JCJoinDomains tree) {
		try{
			printExpr(tree.doms.head);
			for(JCExpression cv:tree.doms)
			{
				print("~");
				printExpr(cv);
			}
		}
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
	}

	public void visitDomParent(JCDomParent tree)
	{
		try{
			print(tree.name+"{");
			for(JCExpression cv:tree.domparams)
			{
				if(cv!=tree.domparams.iterator().next())
					print(",");
				printExpr(cv);
			}
			print("}");
			print("(");
			for(JCDomParameter cv:tree.domargs)
			{
				if(cv!=tree.domargs.iterator().next())
					print(",");
				printExpr(cv);
			}
			print(")");
		}
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
	}

	public void visitDomInstance(JCDomInstance tree)
	{
		try{
			print(tree.name+"{");
			for(JCExpression cv:tree.domparams)
			{
				if(cv!=tree.domparams.iterator().next())
					print(",");
				printExpr(cv);
			}
			print("}");
		}
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
	}

	public void visitDomParameter(JCDomParameter p)
	{
		try{
			print(p.name);
		}
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
	}

	public void visitDomUsage(JCDomUsage p)
	{
		try{
			print(p.name+"{");
			for(JCDomParameter dp:p.getParams())
			{
				if(dp!=p.getParams().head)
					print(",");
				printExpr(dp);
			}
			print("}");
		}
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
	}

	public void visitDomConstraint(JCDomConstraint p)
	{
		try{
               for(JCDomConstraintValue cv : p.left) {
                    printExpr(cv);
                    print(" ");
                }

                switch(p.assign)
                {
                    case JCTree.EQ: print(" = "); break;
                    case JCTree.LT: print(" < "); break;
                    case JCTree.GT: print(" > "); break;
                    case JCTree.LE: print(" <= "); break;
                    case JCTree.GE: print(" >= "); break;
                }

                for(JCDomConstraintValue cv : p.right) {
                    printExpr(cv);
                    print(" ");
                }
		}
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
	}

	public void visitDomainDef(JCDomainDecl tree)
    {
        try{
            print("domain " + tree.name +"{");
            for(List<JCDomParameter> l = tree.domparams; l.nonEmpty(); l = l.tail)
            {
                JCDomParameter p=((JCDomParameter)l.head);
                printExpr(p);
                if(l.length()>1)
                    print(",");
            }
            print("}(");
            for(List<JCDomParameter> l = tree.domargs; l.nonEmpty(); l = l.tail)
            {
                JCDomParameter p=((JCDomParameter)l.head);
                printExpr(p);
                if(l.length()>1)
                    print(",");
            }
            print(")");

            //missing parent

            print("{ ");

            if(tree.defs!=null)
            {
				if(tree.domdef!=null)
					printExpr(tree.domdef);

                print("(");
                for(List<JCDomParameter> l = tree.defs; l.nonEmpty(); l = l.tail)
                {
                    JCDomParameter p=((JCDomParameter)l.head);
                    printExpr(p);
                    if(l.length()>1)
                        print(",");
                }
                print(")");
            }
            print(" | ");

            for(List<JCTree> l = tree.constraints; l.nonEmpty(); l = l.tail)
            {
                JCDomConstraint p=((JCDomConstraint)l.head);
				printExpr(p);

                if(l.length()>1)
                    print(" & ");
            }


            print(" }");
            println();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitClassDef(JCClassDecl tree) {
        try {
            println(); align();
            printDocComment(tree);
            printAnnotations(tree.mods.annotations);
            printFlags(tree.mods.flags & ~INTERFACE);
            Name enclClassNamePrev = enclClassName;
            enclClassName = tree.name;
            if ((tree.mods.flags & INTERFACE) != 0) {
                print("interface " + tree.name);
                printTypeParameters(tree.typarams);
                if (tree.implementing.nonEmpty()) {
                    print(" extends ");
                    printExprs(tree.implementing);
                }
            } else {
                if ((tree.mods.flags & ENUM) != 0)
                    print("enum " + tree.name);
                else
                    print("class " + tree.name);
                printTypeParameters(tree.typarams);
                if (tree.extending != null) {
                    print(" extends ");
                    printExpr(tree.extending);
                }
                if (tree.implementing.nonEmpty()) {
                    print(" implements ");
                    printExprs(tree.implementing);
                }
            }
            print(" ");
            if ((tree.mods.flags & ENUM) != 0) {
                printEnumBody(tree.defs);
            } else {
                printBlock(tree.defs);
            }
            enclClassName = enclClassNamePrev;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitMethodHeader(JCMethodDecl tree) {

        try {

            // when producing source output, omit anonymous constructors
            if (tree.name == tree.name.table.names.init &&
                    enclClassName == null&&tree.sym.owner==null &&
                    sourceOutput) return;
            printExpr(tree.mods);
            printTypeParameters(tree.typarams);
            if (tree.name == tree.name.table.names.init) {
				if(tree.sym.owner!=null)
					print(tree.sym.owner);
				else
					print(enclClassName != null ? enclClassName : tree.name);
            } else {
                if(tree.restype!=null)
                    printExpr(tree.restype);
                else
                    print("event");

                print(" " + tree.name);
            }
            print("(");
            printExprs(tree.params);
            print(")");
            if (tree.thrown.nonEmpty()) {
                print(" throws ");
                printExprs(tree.thrown);
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private String getNodeWork(JCTree tree,String info,JCMethodDecl method)
    {
		if(tree.getTag()==JCTree.METHODDEF)
		{
			if(((JCMethodDecl)tree).sym.mayBeRecursive)
				return "\""+"("+tree.pos().getPreferredPosition()+") rec "+((JCMethodDecl)tree).sym.name+info+"["+work.getWork(tree, method)+"]"+"\"";
			else
				return "\""+"("+tree.pos().getPreferredPosition()+") "+((JCMethodDecl)tree).sym.name+info+"["+work.getWork(tree, method)+"]"+"\"";
		}
		else if(tree.getTag()==JCTree.IF||tree.getTag()==JCTree.CF)//reconnect branches
			return "\""+"("+tree.pos().getPreferredPosition()+") "+tree.toFlatString()+"[CF"+tree.pos+"]"+"["+work.getWork(tree, method)+"]"+"\"";
		else
			return "\""+"("+tree.pos().getPreferredPosition()+") "+tree.toFlatString()+info+"["+work.getWork(tree, method)+"]"+"\"";
    }

    public void dumpJGT(JCMethodDecl tree) throws IOException
    {
        for(JCTree t:tree.depGraph.vertexSet())
        {
			dgprint(getNodeDef(t,tree.depGraph)+";\n");
		}
        for(JCTree t:tree.depGraph.vertexSet())
        {
            DumpGraphNodeJGT(tree.depGraph,t);
        }

		if(tree.depGraphImplicit.edgeSet().size()!=tree.depGraph.edgeSet().size())
		{
		    for(JCTree t:tree.depGraphImplicit.vertexSet())
			{
				dgprint(getNodeDef(t,tree.depGraphImplicit)+";\n");
			}
	        for(JCTree t:tree.depGraphImplicit.vertexSet())
			{
				DumpGraphNodeJGT(tree.depGraphImplicit,t);
			}
		}

    }

    public void visitMethodDef(JCMethodDecl tree) {

        try {

                // when producing source output, omit anonymous constructors
                if (tree.name == tree.name.table.names.init &&
                        enclClassName == null &&
                        sourceOutput) return;
				method=tree;

                if(dump_graph&&tree.depGraph!=null)
                    dumpJGT(tree);

                //println(); align();
                //printDocComment(tree);
                printExpr(tree.mods);
                printTypeParameters(tree.typarams);
                if (tree.name == tree.name.table.names.init) {
                    print(enclClassName != null ? enclClassName : tree.name);
                } else {
                    if(tree.restype!=null)
                    {
                        printExpr(tree.restype);

                        if(dump_analysis)
                        {
                            if(tree.sym.ReturnValueLinear)
                                print("!");

                            Set<VarSymbol> s=tree.sym.retValAliasLinear;
                            if(s!=null&&s.size()>0)
                            {
                                print("{");
                                for(Iterator<VarSymbol> i=s.iterator();i.hasNext();)
                                {
    //                                print(i.next());
                                    visitVarSymbol(i.next(),true);

                                    if(i.hasNext())
                                        print(",");
                                }
                                print("}");
                            }


                            if(tree.sym.mayBeRecursive)
                                print("@");
                        }
                    }
                    else
                        print("event");

                    print(" " + tree.name);
                }


                print("(");
                printExprs(tree.params);
                print(")");
                if (tree.thrown.nonEmpty()) {
                    print(" throws ");
                    printExprs(tree.thrown);
                }
				/*
                if (tree.body != null&&allownl) {
                    print(" ");
                    printStat(tree.body);
                } else {
                    print(";");
                }
				*/

				method=null;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitVarDef(JCVariableDecl tree) {
        try {
            if (docComments != null && docComments.get(tree) != null) {
                println(); align();
            }
            printDocComment(tree);
            if ((tree.mods.flags & ENUM) != 0) {
                print("/*public static final*/ ");
                print(tree.name);
                if (tree.init != null) {
                    print(" /* = ");
                    printExpr(tree.init);
                    print(" */");
                }
            } else {
                printExpr(tree.mods);

                if ((tree.mods.flags & VARARGS) != 0) {
                    printExpr(((JCArrayTypeTree) tree.vartype).elemtype);

                    print("... " + tree.name);

                    if(dump_analysis&&(tree.sym.flags_field&LINEAR)!=0)
                        print("!");

                    if(tree.sym instanceof VarSymbol)
                    {
                        Set<VarSymbol> s= ((VarSymbol)tree.sym).aliasMapLinear.get(tree);
                        //boolean root_linear=(((VarSymbol)tree.sym).flags_field&LINEAR)!=0;

                        if(dump_analysis&&s!=null&&s.size()>0)
                        {
                            print("{");
                            for(Iterator<VarSymbol> i=s.iterator();i.hasNext();)
                            {
                                //print(i.next());
                                visitVarSymbol(i.next(),true);
                                if(i.hasNext())
                                    print(",");
                            }
                            print("}");
                        }
                    }


                } else {
                    printExpr(tree.vartype);
					if((tree.getTypeFlags()&Flags.FOUT)!=0)
						print("*");

                    if(tree.sym instanceof VarSymbol)
                    {
                        print(" ");
                        visitVarSymbol((VarSymbol)tree.sym,false);
                    }
                    else
                        print(" " + tree.name);

                    if(dump_analysis&&tree.sym instanceof VarSymbol)
                    {
                        Set<VarSymbol> s= ((VarSymbol)tree.sym).aliasMapLinear.get(tree);
                        //boolean root_linear=(((VarSymbol)tree.sym).flags_field&LINEAR)!=0;
                        if(s!=null&&s.size()>0)
                        {
                            print("{");
                            for(Iterator<VarSymbol> i=s.iterator();i.hasNext();)
                            {
                                //print(i.next());
                                visitVarSymbol(i.next(),true);

                                if(i.hasNext())
                                    print(",");
                            }
                            print("}");
                        }
                    }
                }

                if (tree.init != null) {
                    print(" = ");
                    printExpr(tree.init);
                }
                if (prec == TreeInfo.notExpression) print(";");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitSkip(JCSkip tree) {
        try {
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    public void visitCF(JCCF tree) {
        try {
			if(tree.condition!=null)
			{
				printExpr(tree.condition);
				print(" == "+tree.value);
			}
			else
				print("true");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitBlock(JCBlock tree) {
        try {
            printFlags(tree.flags);
            printBlock(tree.stats);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitDoLoop(JCDoWhileLoop tree) {
        try {
            print("do ");
            printStat(tree.body);
            align();
            print(" while ");
            if (tree.cond.getTag() == JCTree.PARENS) {
                printExpr(tree.cond);
            } else {
                print("(");
                printExpr(tree.cond);
                print(")");
            }
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitWhileLoop(JCWhileLoop tree) {
        try {
            print("while ");
            if (tree.cond.getTag() == JCTree.PARENS) {
                printExpr(tree.cond);
            } else {
                print("(");
                printExpr(tree.cond);
                print(")");
            }
            print(" ");
            printStat(tree.body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitForLoop(JCForLoop tree) {
        try {
            print("for (");
            if (tree.init.nonEmpty()) {
                if (tree.init.head.getTag() == JCTree.VARDEF) {
                    printExpr(tree.init.head);
                    for (List<JCStatement> l = tree.init.tail; l.nonEmpty(); l = l.tail) {
                        JCVariableDecl vdef = (JCVariableDecl)l.head;
                        print(", " + vdef.name + " = ");
                        printExpr(vdef.init);
                    }
                } else {
                    printExprs(tree.init);
                }
            }
            print("; ");
            if (tree.cond != null) printExpr(tree.cond);
            print("; ");
            printExprs(tree.step);
            print(") ");
            printStat(tree.body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitForeachLoop(JCEnhancedForLoop tree) {
        try {
            print("for (");
            printExpr(tree.var);
            print(" : ");
            printExpr(tree.expr);
            print(") ");
            printStat(tree.body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitLabelled(JCLabeledStatement tree) {
        try {
            print(tree.label + ": ");
            printStat(tree.body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitSwitch(JCSwitch tree) {
        try {
            print("switch ");
            if (tree.selector.getTag() == JCTree.PARENS) {
                printExpr(tree.selector);
            } else {
                print("(");
                printExpr(tree.selector);
                print(")");
            }
            print(" {");
            println();
            printStats(tree.cases);
            align();
            print("}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitCase(JCCase tree) {
        try {
            if (tree.pat == null) {
                print("default");
            } else {
                print("case ");
                printExpr(tree.pat);
            }
            print(": ");
            println();
            indent();
            printStats(tree.stats);
            undent();
            align();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitSynchronized(JCSynchronized tree) {
        try {
            print("synchronized ");
            if (tree.lock.getTag() == JCTree.PARENS) {
                printExpr(tree.lock);
            } else {
                print("(");
                printExpr(tree.lock);
                print(")");
            }
            print(" ");
            printStat(tree.body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTry(JCTry tree) {
        try {
            print("try ");
            printStat(tree.body);
            for (List<JCCatch> l = tree.catchers; l.nonEmpty(); l = l.tail) {
                printStat(l.head);
            }
            if (tree.finalizer != null) {
                print(" finally ");
                printStat(tree.finalizer);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitCatch(JCCatch tree) {
        try {
            print(" catch (");
            printExpr(tree.param);
            print(") ");
            printStat(tree.body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitConditional(JCConditional tree) {
        try {
            open(prec, TreeInfo.condPrec);
            printExpr(tree.cond, TreeInfo.condPrec);
            print(" ? ");
            printExpr(tree.truepart, TreeInfo.condPrec);
            print(" : ");
            printExpr(tree.falsepart, TreeInfo.condPrec);
            close(prec, TreeInfo.condPrec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitIf(JCIf tree) {
        try {
            print("if ");
            if (tree.cond.getTag() == JCTree.PARENS) {
                printExpr(tree.cond);
            } else {
                print("(");
                printExpr(tree.cond);
                print(")");
            }

            if(deep&&allownl)
            {
                print(" ");
                printStat(tree.thenpart);
                if (tree.elsepart != null) {
                    print(" else ");
                    printStat(tree.elsepart);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitIfExp(JCIfExp tree) {
        try {
            print("if ");
            if (tree.cond.getTag() == JCTree.PARENS) {
                printExpr(tree.cond);
            } else {
                print("(");
                printExpr(tree.cond);
                print(")");
            }
            print(" { ");
            printExpr(tree.thenpart);
            print(" } ");
            if (tree.elsepart != null) {
                print(" else ");
                print(" { ");
                printExpr(tree.elsepart);
                print(" } ");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitExec(JCExpressionStatement tree) {
        try {
            printExpr(tree.expr);
            if (prec == TreeInfo.notExpression) print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitBreak(JCBreak tree) {
        try {
            print("break");
            if (tree.label != null) print(" " + tree.label);
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitContinue(JCContinue tree) {
        try {
            print("continue");
            if (tree.label != null) print(" " + tree.label);
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitReturn(JCReturn tree) {
        try {
			if((tree.flags&Flags.FINAL)!=0)
				print("finally");
			else if((tree.flags&Flags.SYNCHRONIZED)!=0)
				print("cancel");
			else
				print("resume");

            if (tree.expr != null) {
                print(" ");
                printExpr(tree.expr);
            }
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitThrow(JCThrow tree) {
        try {
            print("throw ");
            printExpr(tree.expr);
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitAssert(JCAssert tree) {
        try {
            print("assert ");
            printExpr(tree.cond);
            if (tree.detail != null) {
                print(" : ");
                printExpr(tree.detail);
            }
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitPragma(JCPragma tree) {
        try {
			if(tree.flag==Flags.WORK)
				print("#WORK");
			else if(tree.flag==Flags.PARALLEL)
				print("#PARALLEL");
			else if(tree.flag==Flags.TIME)
				print("#TIME");
			else
				print("#FORCE");
			print("(");
			printExpr(tree.cond);
			print(",");
			printExpr(tree.detail);
			print(")");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitApply(JCMethodInvocation tree) {
        try {
            if (!tree.typeargs.isEmpty()) {
                if (tree.meth.getTag() == JCTree.SELECT) {
                    JCFieldAccess left = (JCFieldAccess)tree.meth;
                    printExpr(left.selected);
                    print(".<");
                    printExprs(tree.typeargs);
                    print(">" + left.name);
                } else {
                    print("<");
                    printExprs(tree.typeargs);
                    print(">");
                    printExpr(tree.meth);
                }
            } else {
                printExpr(tree.meth);
            }
            print("(");
            printExprs(tree.args);
            print(")");

            if(tree.getTriggerReturn()!=null)
            {
                print(" = ");
                printExpr(tree.getTriggerReturn());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitNewClass(JCNewClass tree) {
        try {
            if (tree.encl != null) {
                printExpr(tree.encl);
                print(".");
            }
            print("new ");
            if (!tree.typeargs.isEmpty()) {
                print("<");
                printExprs(tree.typeargs);
                print(">");
            }
            printExpr(tree.clazz);
            print("(");
            printExprs(tree.args);
            print(")");
            if (tree.def != null) {
                Name enclClassNamePrev = enclClassName;
                enclClassName =
                        tree.def.name != null ? tree.def.name :
                            tree.type != null && tree.type.tsym.name != tree.type.tsym.name.table.names.empty
                                ? tree.type.tsym.name : null;
                if ((tree.def.mods.flags & Flags.ENUM) != 0) print("/*enum*/");
                printBlock(tree.def.defs);
                enclClassName = enclClassNamePrev;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitNewArray(JCNewArray tree) {
        try {
            if (tree.elemtype != null) {
                if(tree.elemtype.type!=null&&tree.elemtype.type.tsym.name.toString().equals("Object"))
                {
                    printExprs(tree.elems);//hack to allow ellipsis with Object
                    return;
                }
                print("new ");
                JCTree elem = tree.elemtype;
                if (elem instanceof JCArrayTypeTree)
                    printBaseElementType((JCArrayTypeTree) elem);
                else
                    printExpr(elem);
				if(tree.dims!=null)
                for (List<JCExpression> l = tree.dims; l.nonEmpty(); l = l.tail) {
                    print("[");
                    printExpr(l.head);
                    print("]");
                }
                if (elem instanceof JCArrayTypeTree)
                    printBrackets((JCArrayTypeTree) elem);
            }
            if (tree.elems != null) {
                if (tree.elemtype != null) { print("[");printExpr(tree.dom);print("]"); }
                print("{");
                printExprs(tree.elems);
                print("}");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitParens(JCParens tree) {
        try {
            print("(");
            printExpr(tree.expr);
            print(")");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitAssign(JCAssign tree) {
        try {
            open(prec, TreeInfo.assignPrec);
            printExpr(tree.lhs, TreeInfo.assignPrec + 1);
            print(" = ");
            printExpr(tree.rhs, TreeInfo.assignPrec);
            close(prec, TreeInfo.assignPrec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitWhere(JCWhere tree) {
        try {
            printExpr(tree.exp);
            print(" where ");
            if(tree.body!=null)
                printStat(tree.body);
            else
                printExpr(tree.sexp);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitFor(JCFor tree) {
        try {
            print("#for ( "+tree.name.toString()+" , ");

            printExpr(tree.exp);
            print(" ) {");
            printStats(tree.content);
            print(" }");

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitSelectCond(JCSelectCond tree) {
        try {
            print(" ");

            if(tree.cond!=null)
                printExpr(tree.cond);
            else
                print(" _ ");

            print(" : ");
            if(tree.res!=null)
            {
                print("{ ");
                printExpr(tree.res);
                print(" }");
            }
            else
                printStat(tree.stmnt);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitSelectExp(JCSelect tree) {
        try {
            print(" select ");
            print(" { ");

            for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {
                printExpr(l.head);
            }
            print(" } ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitCaseExp(JCCaseExp tree) {
        try {
            print(" case ");
            printExpr(tree.exp);
            print(" { ");

            for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {
                printExpr(l.head);
            }
            print(" } ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitDomIter(JCDomainIter tree) {
        try {
            printExpr(tree.exp);
            print(".\\");
            if(tree.name != null) {
                print(tree.name);
            }
			if(tree.params != null) {
                print("{");
                printExprs(tree.params);
                print("}");
            }
            print("(");
			if(tree.domargs!=null)
            for(List<JCVariableDecl> l = tree.domargs; l.nonEmpty(); l = l.tail)
            {
                //JCDomParameter p=((JCDomParameter)l.head);
                //print(p.name);
                visitVarDef(l.head);
                if(l.length()>1)
                    print(",");
            }
            print(")");
            if(tree.body!=null)
            {
                print(" { ");
                printExpr(tree.body);
                print(" } ");
            }
            if(tree.sbody!=null)
                printStat(tree.sbody);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitArgExpression(JCArgExpression tree) {
        try {
            if(tree.exp2==null)
                printExpr(tree.exp1);
            else
            {
                print("[ ");
                printExpr(tree.exp1);
                print(" , ");
                printExpr(tree.exp2);
                print(" ]");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String operatorName(int tag) {
        switch(tag) {
            case JCTree.POS:     return "+";
            case JCTree.NEG:     return "-";
            case JCTree.NOT:     return "!";
            case JCTree.COMPL:   return "~";
            case JCTree.PREINC:  return "++";
            case JCTree.PREDEC:  return "--";
            case JCTree.POSTINC: return "++";
            case JCTree.POSTDEC: return "--";
            case JCTree.NULLCHK: return "<*nullchk*>";
            case JCTree.OR:      return "||";
            case JCTree.AND:     return "&&";
            case JCTree.EQ:      return "==";
            case JCTree.NE:      return "!=";
            case JCTree.LT:      return "<";
            case JCTree.GT:      return ">";
            case JCTree.LE:      return "<=";
            case JCTree.GE:      return ">=";
            case JCTree.BITOR:   return "|";
            case JCTree.BITXOR:  return "^";
            case JCTree.BITAND:  return "&";
            case JCTree.SL:      return "<<";
            case JCTree.SR:      return ">>";
            case JCTree.USR:     return ">>>";
            case JCTree.SEQ:     return ":";
            case JCTree.PLUS:    return "+";
            case JCTree.MINUS:   return "-";
            case JCTree.MUL:     return "*";
            case JCTree.DIV:     return "/";
            case JCTree.MOD:     return "%";
            default: throw new Error();
        }
    }

    public void visitAssignop(JCAssignOp tree) {
        try {
            open(prec, TreeInfo.assignopPrec);
            printExpr(tree.lhs, TreeInfo.assignopPrec + 1);
            print(" " + operatorName(tree.getTag() - JCTree.ASGOffset) + "= ");
            printExpr(tree.rhs, TreeInfo.assignopPrec);
            close(prec, TreeInfo.assignopPrec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitUnary(JCUnary tree) {
        try {
            int ownprec = TreeInfo.opPrec(tree.getTag());
            String opname = operatorName(tree.getTag());
            open(prec, ownprec);
            if (tree.getTag() <= JCTree.PREDEC) {
                print(opname);
                printExpr(tree.arg, ownprec);
            } else {
                printExpr(tree.arg, ownprec);
                print(opname);
            }
            close(prec, ownprec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitBinary(JCBinary tree) {
        try {
            int ownprec = TreeInfo.opPrec(tree.getTag());
            String opname = operatorName(tree.getTag());
            open(prec, ownprec);
            printExpr(tree.lhs, ownprec);
            print(" " + opname + " ");
            printExpr(tree.rhs, ownprec + 1);
            close(prec, ownprec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTypeCast(JCTypeCast tree) {
        try {
            open(prec, TreeInfo.prefixPrec);
            print("(");
            printExpr(tree.clazz);
            print(")");
            printExpr(tree.expr, TreeInfo.prefixPrec);
            close(prec, TreeInfo.prefixPrec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTypeTest(JCInstanceOf tree) {
        try {
            open(prec, TreeInfo.ordPrec);
            printExpr(tree.expr, TreeInfo.ordPrec);
            print(" instanceof ");
            printExpr(tree.clazz, TreeInfo.ordPrec + 1);
            close(prec, TreeInfo.ordPrec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitIndexed(JCArrayAccess tree) {
        try {
            if(tree.params != null)
			{
	            print("{");
                printExprs(tree.params);
		        print("}");
			}
            printExpr(tree.indexed, TreeInfo.postfixPrec);
            print("[");
            printExprs(tree.index);
            print("]");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitSelect(JCFieldAccess tree) {
        try {
            printExpr(tree.selected, TreeInfo.postfixPrec);
            print(".");
			printExpr(tree.name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitVarSymbol(VarSymbol vs,boolean showaccess) throws IOException
    {
        if(vs.heapAllocated&&dump_analysis)
            print("#");

		String sub=null;
		if(subst!=null)
			sub=subst.get(vs);
		if(sub==null)
			print(vs);
		else
			print(sub);

        if(dump_analysis)
        {
            if((vs.flags_field&LINEAR)==0)
            {
                if(showaccess)
                    print("*");
            }
            else
                print("!");
        }
    }

    public void visitSizeOf(JCSizeOf tree) {
		try {
			print("sizeof(");
			printExpr(tree.expr);
			print(")");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
	}

    public void visitIdent(JCIdent tree) {
        try {

            if(tree.sym instanceof VarSymbol)
                visitVarSymbol((VarSymbol)tree.sym,false);
            else
			{
				if(tree.sym instanceof ClassSymbol)
					if(tree.type.isConst())
						print("const ");
                print(tree.name);
			}

            if(tree.sym instanceof VarSymbol)
            {
                boolean root_linear=(((VarSymbol)tree.sym).flags_field&LINEAR)!=0;
                Set<VarSymbol> s= ((VarSymbol)tree.sym).aliasMapLinear.get(tree);
                if(s!=null&&s.size()>0)
                {
                    if(dump_analysis)
                    {
                        print("{");
                        for(Iterator<VarSymbol> i=s.iterator();i.hasNext();)
                        {
                            //print(i.next());
                            visitVarSymbol(i.next(),true);
                            if(i.hasNext())
                                print(",");
                        }
                        print("}");
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitLiteral(JCLiteral tree) {
        try {
            switch (tree.typetag) {
                case TypeTags.INT:
                    print(tree.value.toString());
                    break;
                case TypeTags.LONG:
                    print(tree.value + "L");
                    break;
                case TypeTags.FLOAT:
                    print(tree.value + "F");
                    break;
                case TypeTags.DOUBLE:
                    print(tree.value.toString());
                    break;
                case TypeTags.CHAR:
                    print("\'" +
                            Convert.quote(
                            String.valueOf((char)((Number)tree.value).intValue())) +
                            "\'");
                    break;
                case TypeTags.BOOLEAN:
                    print(((Number)tree.value).intValue() == 1 ? "true" : "false");
                    break;
                case TypeTags.BOT:
                    print("null");
                    break;
                default:
                    print("\"" + Convert.quote(tree.value.toString()) + "\"");
                    break;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTypeIdent(JCPrimitiveTypeTree tree) {
        try {
            switch(tree.typetag) {
                case TypeTags.BYTE:
                    print("byte");
                    break;
                case TypeTags.CHAR:
                    print("char");
                    break;
                case TypeTags.GROUP:
                    print("group");
                    break;
                case TypeTags.THREAD:
                    print("thread");
                    break;
                case TypeTags.SHORT:
                    print("short");
                    break;
                case TypeTags.INT:
                    print("int");
                    break;
                case TypeTags.LONG:
                    print("long");
                    break;
                case TypeTags.FLOAT:
                    print("float");
                    break;
                case TypeTags.DOUBLE:
                    print("double");
                    break;
                case TypeTags.BOOLEAN:
                    print("boolean");
                    break;
                case TypeTags.VOID:
                    print("void");
                    break;
                default:
                    print("error");
                    break;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTypeArray(JCArrayTypeTree tree) {
        try {
            //printBaseElementType(tree);
            printExpr(tree.elemtype);
//			if(tree.elemtype.type.isPointer())
//				print("&");
            //printBrackets(tree);
			if(tree.dom!=null)
			{
				print("[");
				printExpr(tree.dom);
				print("]");
			}
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Prints the inner element type of a nested array
    private void printBaseElementType(JCArrayTypeTree tree) throws IOException {
        JCTree elem = tree.elemtype;
        while (elem instanceof JCWildcard)
            elem = ((JCWildcard) elem).inner;
        if (elem instanceof JCArrayTypeTree)
            printBaseElementType((JCArrayTypeTree) elem);
        else
            printExpr(elem);
    }

    // prints the brackets of a nested array in reverse order
    private void printBrackets(JCArrayTypeTree tree) throws IOException {
        JCTree elem;
        while (true) {
            elem = tree.elemtype;
            print("[");
			printExpr(tree.dom);
            print("]");
            if (!(elem instanceof JCArrayTypeTree)) break;
            tree = (JCArrayTypeTree) elem;
        }
    }

    public void visitTypeApply(JCTypeApply tree) {
        try {
            printExpr(tree.clazz);
            print("<");
            printExprs(tree.arguments);
            print(">");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTypeParameter(JCTypeParameter tree) {
        try {
            print(tree.name);
            if (tree.bounds.nonEmpty()) {
                print(" extends ");
                printExprs(tree.bounds, " & ");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void visitWildcard(JCWildcard tree) {
        try {
            print(tree.kind);
            if (tree.kind.kind != BoundKind.UNBOUND)
                printExpr(tree.inner);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind tree) {
        try {
            print(String.valueOf(tree.kind));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitErroneous(JCErroneous tree) {
        try {
            print("(ERROR)");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitLetExpr(LetExpr tree) {
        try {
            print("(let " + tree.defs + " in " + tree.expr + ")");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitModifiers(JCModifiers mods) {
        try {
            printAnnotations(mods.annotations);
            printFlags(mods.flags);
            if(mods.group!=null)
            {
                print("group(");
                printExpr(mods.group);
                print(") ");
            }
            if(mods.thread!=null)
            {
                print("thread(");
                printExpr(mods.thread);
                print(") ");
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitAnnotation(JCAnnotation tree) {
        try {
            print("@");
            printExpr(tree.annotationType);
            print("(");
            printExprs(tree.args);
            print(")");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

	public void visitSequence(JCSequence tree) {
        try {
			for(JCExpression e:tree.seq)
			{
				if(e!=tree.seq.head)
					print("::");
				printExpr(e);
				println();
			}
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTree(JCTree tree) {
        try {
            print("(UNKNOWN: " + tree.getClass().getName() + ")");
            println();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
