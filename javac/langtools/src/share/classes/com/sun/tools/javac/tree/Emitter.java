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
import org.jgrapht.DirectedGraph;

/**
 * Prints out a tree as an indented Java source program.
 *
 * <p><b>This is NOT part of any API supported by Sun Microsystems. If you write code that depends
 * on this, you do so at your own risk. This code and its internal interfaces are subject to change
 * or deletion without notice.</b>
 */
public class Emitter {

	public LowerTreeImpl state;

	public Emitter(LowerTreeImpl state) {
		this.state = state;
	}

	public String ff(float f)
	{
		return state.ff(f);
	}

	public void nl() throws IOException {
		state.nl();
	}

	public void print(Object name) throws IOException {
		state.print(name);
	}

	public void printConstTail() throws IOException
	{
		if(state.target.regSize>8)
			print("L");
	}

	public void println() throws IOException {
		state.println();
	}

	public void align() throws IOException {
		state.align();
	}

	public void indent() throws IOException {
		state.indent();
	}

	public void undent() throws IOException {
		state.undent();
	}

	public void printExpr(JCTree tree, int prec) throws IOException {
		state.printExpr(tree, prec);
	}

	public void printExpr(JCTree tree) throws IOException {
		state.printExpr(tree);
	}

	public <T extends JCTree> void printExprs(List<T> trees, String sep) throws IOException {
		state.printExprs(trees, sep);
	}

	public <T extends JCTree> void printExprs(List<T> trees) throws IOException {
		state.printExprs(trees);
	}

	public void printStat(JCTree tree) throws IOException {
		state.printStat(tree);
	}

	public void printStats(List<? extends JCTree> trees) throws IOException {
		state.printStats(trees);
	}

	protected String getNode(Object tree) {
		return state.getNode(tree);
	}

	protected String getNodeDef(JCTree tree, String color) {
		return state.getNodeDef(tree, color);
	}

	protected String getNodeDef(JCTree tree, String color, String info) {
		return state.getNodeDef(tree, color, info);
	}

	public void dgprint(Object s) throws IOException {
		state.dgprint(s);
	}

	public void dgprintln(Object s) throws IOException {
		state.dgprintln(s);
	}
}
