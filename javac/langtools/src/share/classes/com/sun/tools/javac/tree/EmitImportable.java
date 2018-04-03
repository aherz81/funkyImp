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

//import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import static com.sun.tools.javac.code.Flags.*;

/** Prints out a tree as an indented Java source program.
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class EmitImportable extends JCTree.Visitor {

	Names names;

	public EmitImportable(Context context, Writer out) {
		this.out = out;
		log = Log.instance(context);
		names = Names.instance(context);
	}
	/** Set when we are producing source output.  If we're not
	 *  producing source output, we can sometimes give more detail in
	 *  the output even though that detail would not be valid java
	 *  soruce.
	 */
	public int width = 4;

	String class_name;
	/** The output stream on which trees are printed.
	 */
	Writer out;
	/** Indentation width (can be reassigned from outside).
	 */
	private final Log log;

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
		for (int i = 0; i < lmargin; i++) {
			out.write(" ");
		}
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
		if (ownPrec < contextPrec) {
			out.write("(");
		}
	}

	/** Leave precedence level. Emit a `(' if inner precedence level
	 *  is less than precedence level we revert to.
	 *  @param contextPrec    The precedence level we revert to.
	 *  @param ownPrec        The inner precedence level.
	 */
	void close(int contextPrec, int ownPrec) throws IOException {
		if (ownPrec < contextPrec) {
			out.write(")");
		}
	}

	/** Print string, replacing all non-ascii character with unicode escapes.
	 */
	public void print(Object s) throws IOException {
		out.write(Convert.escapeUnicode(s.toString()));
	}

	/** Print new line.
	 */
	public void println() throws IOException {
		out.write(lineSep);
	}

	public void nl() throws IOException {
		println();
		align();
	}

	/** Print string, replacing all non-ascii character with unicode escapes.
	 */
	public void dgprint(Object s) throws IOException {
		out.write(Convert.escapeUnicode(s.toString()));
	}

	/** Print new line.
	 */
	public void dgprintln(Object s) throws IOException {
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

	/** Derived visitor method: print expression tree at minimum precedence level
	 *  for expression.
	 */
	public void printExpr(JCTree tree) throws IOException {
		printExpr(tree, TreeInfo.noPrec);
	}

	/** Derived visitor method: print statement tree.
	 */
	public void printStat(JCTree tree) throws IOException {

		printExpr(tree, TreeInfo.notExpression);

		if (tree.getTag() != JCTree.SKIP && tree.getTag() != JCTree.BLOCK) {
			println();
			align();
		}

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
		if ((flags & SYNTHETIC) != 0) {
			print("/*synthetic*/ ");
		}
		if ((flags & StandardFlags) != 0) {
				print(TreeInfo.flagNames(flags & StandardFlags));
		}
		if ((flags & StandardFlags) != 0) {
			print(" ");
		} else if ((flags & StandardFlags) != 0) {
			print(" ");
		}
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
				print("/**");
				println();
				int pos = 0;
				int endpos = lineEndPos(dc, pos);
				while (pos < dc.length()) {
					align();
					print(" *");
					if (pos < dc.length() && dc.charAt(pos) > ' ') {
						print(" ");
					}
					print(dc.substring(pos, endpos));
					println();
					pos = endpos + 1;
					endpos = lineEndPos(dc, pos);
				}
				align();
				print(" */");
				println();
				align();
			}
		}
	}
//where

	static int lineEndPos(String s, int start) {
		int pos = s.indexOf('\n', start);
		if (pos < 0) {
			pos = s.length();
		}
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

		class_name = cdef.sym.flatname.toString();
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
				JCImport imp = (JCImport) l.head;
				Name name = TreeInfo.name(imp.qualid);
				if (name == name.table.names.asterisk
						|| cdef == null
						|| isUsed(TreeInfo.symbol(imp.qualid), cdef)) {
					if (firstImport) {
						firstImport = false;
						println();
					}
					printStat(imp);
				}
			} else {
				if (l.head.getTag() != JCTree.CLASSDEF) {
					printStat(l.head);
				}
			}
		}
		if (cdef != null) {
			printStat(cdef);
			println();
		}

	}
	// where

	boolean isUsed(final Symbol t, JCTree cdef) {
		class UsedVisitor extends TreeScanner {

			public void scan(JCTree tree) {
				if (tree != null && !result) {
					tree.accept(this);
				}
			}
			boolean result = false;

			public void visitIdent(JCIdent tree) {
				if (tree.sym == t) {
					result = true;
				}
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
			if (tree.staticImport) {
				print("static ");
			}
			printExpr(tree.qualid);
			print(";");
			println();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void visitClassDef(JCClassDecl tree) {
		try {

            println(); align();
            printDocComment(tree);
            printAnnotations(tree.mods.annotations);
            printFlags((tree.mods.flags & ~INTERFACE& ~EXPORT)|PUBLIC|NATIVE);
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
		} finally {
		}
	}

	public void visitMethodHeader(JCMethodDecl tree) {

		try {

			if(tree.is_blocking)
				print("blocking ");
			print("work("+((int)(tree.sym.EstimatedWork))+") ");
			print("tasks("+(int)tree.sym.LocalTGWidth+") ");
			print("mem("+(int)tree.sym.EstimatedMem+") ");

//			printExpr(tree.mods)
			print(tree.getStringHeader());
			print(";");

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void visitMethodDef(JCMethodDecl tree) {

		try {

			// when producing source output, omit anonymous constructors
			if (tree.name == tree.name.table.names.init
					&& enclClassName == null) {
				return;
			}

			if(tree.is_blocking)
				print("blocking ");
			print("work("+((int)(tree.sym.EstimatedWork))+") ");
			print("tasks("+(int)tree.sym.LocalTGWidth+") ");
			print("mem("+(int)tree.sym.EstimatedMem+") ");
			print(tree.getStringHeader());
			print(";");

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void visitVarDef(JCVariableDecl tree) {
		try {
			if (tree.type.tag == TypeTags.GROUP) {
				return;
			}

			if ((tree.sym.flags() & Flags.PARAMETER) == 0) {
				println();
				align();
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


				print(tree.toFlatString());

				if (tree.init != null) {
					if (tree.sym.owner.kind == Kinds.TYP) {
						print(" /* ");
					}

				}
				if (prec == TreeInfo.notExpression) {
					print(";");
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void visitSkip(JCSkip tree) {
		try {
			print("");
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

	public void visitPragma(JCPragma tree) {
		//nothing to do...
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

	public void visitModifiers(JCModifiers mods) {
		try {
			printAnnotations(mods.annotations);
			printFlags(mods.flags&(~Flags.EXPORT));

			if (mods.group != null) {
				print("group(");
				printExpr(mods.group);
				print(") ");
			}

			if (mods.thread != null) {
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
			log.error(tree.pos, "not.impl", tree);
			print("@");
			printExpr(tree.annotationType);
			print("(");
			printExprs(tree.args);
			print(")");
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
