/*
 * Copyright 2011-2012 TU-München
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 */
package com.sun.tools.javac.tree;

import java.io.*;
import java.util.*;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.code.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;

import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import static com.sun.tools.javac.code.Flags.*;
import com.sun.tools.javac.comp.Work;
import com.sun.tools.javac.main.JavaCompiler;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DirectedWeightedMultigraph;

//this just provides basic services to LowerTreeImpl
public class LowerTree extends JCTree.Visitor {

	public Names names;
	public Work work;
	public JavaCompiler jc; //access other analysis results
	public final Types types;
        public final Log log;
	public TreeCopier copy;
	public JCMethodDecl method = null; //current method being lowered

        static StringBuilder debugStringOutput = new StringBuilder();

// ------------------- BEGIN : methods to dump DOT file with tasks ---------------

	String ff(float f)
	{
		//return new java.util.Formatter().format("%f", f).toString();
		return ""+f;
	}

	protected String getNode(Object tree) {
		return "N" + Math.abs(tree.hashCode());
	}

	protected String getNodeDef(JCTree tree, String color) {
		float w = (float) work.getWork(tree, method);
		float m = (float) work.getMem(tree, method);
		String info = "\\n[w:" + ff(w) + ",m:" + ff(m) + "]";
		return getNode(tree) + " [label=\"" + tree.toFlatString().replace('\\', ' ').replaceAll("\\\"", "\\\\\"") + info + "\", color=" + color + "]";
	}

	protected String getNodeDef(JCTree tree, String color, String info) {
		return getNode(tree) + " [label=\"" + tree.toFlatString().replace('\\', ' ').replaceAll("\\\"", "\\\\\"") + info + "\", color=" + color + "]";
	}

	protected String getNodeDef(String tree, String color, String info) {
		return getNode(tree) + " [label=\"" + tree.toString().replace('\\', ' ').replaceAll("\\\"", "\\\\\"") + info + "\", color=" + color + "]";
	}

	public void dgprint(Object s) throws IOException {
		if (outGraph == null) {
			return;
		}
		outGraph.write(Convert.escapeUnicode(s.toString()));
	}

	public void dgprintln(Object s) throws IOException {
		if (outGraph == null) {
			return;
		}
		dgprint(s);
		outGraph.write(lineSep);
	}

	void DumpSpawn(JCTree node, iTask tp, boolean count,Map<iTask, String> task_map) throws IOException {
		String refc = "";
		if (count) {
			refc = ", label=\"ref\"";
		}
		dgprint(getNode(node) + "->" + getNode(task_map.get(tp)) + "[style=dashed,color=yellow" + refc + "];\n");
	}

	void DumpSchedule(JCTree node, iTask tp,Map<iTask, String> task_map,boolean dump_kernel) throws IOException {
		DirectedWeightedMultigraph<JCTree, Arc> depGraph = method.depGraphImplicit;
		String name = task_map.get(tp);
		Set<JCTree> cn = tp.getCalcNodes();
		JCTree fcn = tp.getFirstCalcNode(method, cn);
		for (JCTree n : cn) {
			boolean connected = false;
			for (Arc a : depGraph.incomingEdgesOf(n)) {
				if (a.s == node) {
					connected = true;
					if (name == null) {
						dgprint(getNode(node) + "->" + getNode(n) + "[style=dashed];\n");
					} else if (dump_kernel) //
					{
						dgprint(getNode(node) + "->" + getNode(n) + "[style=dashed,color=green];\n");
					}
					break;
				}
			}
			if (!connected && n == fcn) {
				if (name == null) {
					dgprint(getNode(node) + "->" + getNode(n) + "[style=dashed];\n");
				} else if (dump_kernel) //
				{
					dgprint(getNode(node) + "->" + getNode(n) + "[style=dashed,color=green];\n");
				}
			}
		}
	}

	void DumpFinal(JCTree tree,boolean kernel) throws IOException {
		if(kernel)
			return;

		DirectedWeightedMultigraph<JCTree, Arc> depGraph = method.depGraphImplicit;

		dgprint(getNodeDef(method.final_value, "\"#FF0000\"", "") + ";\n");

		for(Arc a:depGraph.incomingEdgesOf(tree))
			dgprint(getNode(a.s) + "->" + getNode(tree) + "[style=dashed,color=red];\n");
	}

	void DumpPath(iTask ps,Map<iTask, String> task_map) throws IOException {
		DirectedWeightedMultigraph<JCTree, Arc> depGraph = method.depGraphImplicit;
		String name = task_map.get(ps);
		String hex;

		if (name == null) {
			hex = "000000";
		} else {
			hex = Integer.toHexString(ps.hashCode());
			hex = hex.substring(0, Math.min(6, hex.length() - 1));
			while (hex.length() < 6) {
				hex = hex + "0";
			}
		}

		String color = "\"#";
		color += hex;
		color += "\"";

		//Task root node
		if (name != null) {
			int w = (int) work.getWork(ps, method);
			int m = (int) work.getMem(ps, method);
			String info = "\\n[w:" + w + ",m:" + m + "]";
			dgprint(getNodeDef(name, color, info) + ";\n");
		}

		Set<JCTree> cn = ps.getCalcNodes();
		for (JCTree t : cn) {
			dgprint(getNodeDef(t, color) + ";\n");
		}

		Set<JCTree> startNodes=new LinkedHashSet<JCTree>();

		//connect task root
		if (name != null) {
			startNodes.add(ps.getFirstCalcNode(method, cn));
		}

		for (JCTree t : cn) {
			boolean reachable=false;
			for(JCTree start:startNodes)
				if(start.getDGNode().IsReachable(false, t, method.topolNodes, method.depGraphImplicit, true))
				{
					reachable=true;
					break;
				}

			if(!reachable)
				startNodes.add(t);

			DumpGraphNodeJGT(depGraph, t, cn);
		}

		if(name!=null)
		{
			for(JCTree start:startNodes)
				dgprint(getNode(name) + "->" + getNode(start) + ";\n");
		}
	}

	void DumpGraphNodeJGT(DirectedWeightedMultigraph<JCTree, Arc> depGraph, JCTree tree, Set<JCTree> cn) throws IOException {
		Set<Arc> childs = depGraph.outgoingEdgesOf(tree);
		if (childs != null) {
			Iterator<Arc> itr = childs.iterator();
			while (itr.hasNext()) {
				Arc gen = itr.next();

				if (cn.contains(gen.t)) {
					dgprint(getNode(tree) + "->" + getNode(gen.t) + ";\n");
				}
			}
		}
	}

// ------------------- actual code emitter ---------------

	public LowerTree(Context context, Writer out, Writer outGraph, boolean sourceOutput, boolean header, boolean lineDebugInfo, boolean forcegc) {
		this.out = out;
		this.outGraph = outGraph;
		this.sourceOutput = sourceOutput;
		this.header = header;
		this.lineDebugInfo = lineDebugInfo;
		log = Log.instance(context);
		work = Work.instance(context);
		names = Names.instance(context);

		jc = JavaCompiler.instance(context);

		copy = new TreeCopier(jc.make);
		types = Types.instance(context);

		target = jc.target;
	}
	protected JavaCompiler.Target target = null;
	Set<VarSymbol> inner_symbols = null;

	boolean skipNL = false;
	/**
	 * Set when we are producing source output. If we're not producing source output, we can
	 * sometimes give more detail in the output even though that detail would not be valid java
	 * soruce.
	 */
	protected final boolean sourceOutput;
	/**
	 * The output stream on which trees are printed.
	 */
	protected boolean header; //two passes, one for generating header files and one for cpp files
	Writer out; //output .h or .cpp
	Writer outGraph; //output .dot
	/**
	 * Indentation width (can be reassigned from outside).
	 */
	public int width = 4;

	protected DiagnosticSource source = null; //for debug output (#line pragmas)
	protected int cur_pos = -1; //debug pos
	protected boolean dpginfo_valid = false;
	protected boolean lineDebugInfo = false; //shall we emit dbg info?

//------------------------- DBG AND FORMATTING -------------------------

	//emit dbg information, if necessary
	void debugPos(int pos) throws IOException {
		if (!lineDebugInfo) {
			return;
		}
		if(dpginfo_valid)
			return;
		dpginfo_valid=true;
        
		if (source != null) {
			long nline = source.getLineNumber(pos);
			String nfile = source.getFile().toString();

			out.write(lineSep);
			align();
			print("#line " + nline + " \"" + nfile + "\"");
			out.write(lineSep);
			align();
		} else {
			out.write(lineSep);
			align();
			print("#line " + 1 + " " + "\"unkown\"");
			out.write(lineSep);
			align();
		}
		cur_pos = pos;
	}
	/**
	 * The current left margin.
	 */
	public int lmargin = 0;
	/**
	 * The enclosing class name.
	 */
	public Name enclClassName;
	/**
	 * A hashtable mapping trees to their documentation comments (can be null)
	 */
	public Map<JCTree, String> docComments = null;

	/**
	 * Align code to be indented to left margin.
	 */
	void align() throws IOException {
		for (int i = 0; i < lmargin; i++) {
			out.write(" ");
                        debugStringOutput.append(" ");
		}
	}

	/**
	 * Increase left margin by indentation width.
	 */
	void indent() {
		lmargin = lmargin + width;
	}

	/**
	 * Decrease left margin by indentation width.
	 */
	void undent() {
		lmargin = lmargin - width;
	}

	static int lineEndPos(String s, int start) {
		int pos = s.indexOf('\n', start);
		if (pos < 0) {
			pos = s.length();
		}
		return pos;
	}

	public static int getLineNumber() {
		return Thread.currentThread().getStackTrace()[2].getLineNumber();
	}

	public static String getFile() {
		return Thread.currentThread().getStackTrace()[2].getFileName();
	}

	/**
	 * Enter a new precedence level. Emit a `(' if new precedence level is less than precedence
	 * level so far.
	 *
	 * @param contextPrec The precedence level in force so far.
	 * @param ownPrec The new precedence level.
	 */
	void open(int contextPrec, int ownPrec) throws IOException {
		if (ownPrec < contextPrec) {
			out.write("(");
		}
	}

	/**
	 * Leave precedence level. Emit a `(' if inner precedence level is less than precedence level we
	 * revert to.
	 *
	 * @param contextPrec The precedence level we revert to.
	 * @param ownPrec The inner precedence level.
	 */
	void close(int contextPrec, int ownPrec) throws IOException {
		if (ownPrec < contextPrec) {
			out.write(")");
		}
	}

	/**
	 * Print string, replacing all non-ascii character with unicode escapes.
	 */
	public void print(Object s) throws IOException {
		out.write(Convert.escapeUnicode(s.toString()));
                debugStringOutput.append(s.toString());
		dpginfo_valid=false;
	}

	/**
	 * Print new line.
	 */
	public void println() throws IOException {
		out.write(lineSep);
                debugStringOutput.append(lineSep);
		debugPos(cur_pos);
	}

	public void nl() throws IOException {
		println();
		align();
	}

	String lineSep = System.getProperty("line.separator");

	/**
	 * ************************************************************************
	 * Traversal methods ***********************************************************************
	 */
	/**
	 * Exception to propogate IOException through visitXXX methods
	 */
	protected static class UncheckedIOException extends Error {

		static final long serialVersionUID = -4032692679158424751L;

		UncheckedIOException(IOException e) {
			super(e.getMessage(), e);
		}
	}
	/**
	 * Visitor argument: the current precedence level.
	 */
	int prec;

	/**
	 * Visitor method: print expression tree.
	 *
	 * @param prec The current precedence level.
	 */
	public void printExpr(JCTree tree, int prec) throws IOException {
		int prevPrec = this.prec;
		try {
			this.prec = prec;
			if (tree == null) {
				print("/*missing*/");
			} else {
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

	/**
	 * Derived visitor method: print expression tree at minimum precedence level for expression.
	 */
	public void printExpr(JCTree tree) throws IOException {
		printExpr(tree, TreeInfo.noPrec);
	}

	/**
	 * Derived visitor method: print list of expression trees, separated by given string.
	 *
	 * @param sep the separator string
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

	/**
	 * Derived visitor method: print list of expression trees, separated by commas.
	 */
	public <T extends JCTree> void printExprs(List<T> trees) throws IOException {
		printExprs(trees, ", ");
	}

	public void printStat(JCTree tree) throws IOException {}
	/**
	 * Derived visitor method: print list of statements, each on a separate line.
	 */
	public void printStats(List<? extends JCTree> trees) throws IOException {
		for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail) {
			align();
			printStat(l.head);
			println();
		}
	}


}
