/*
 * Copyright 2011-2012 TU-München
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 */
package com.sun.tools.javac.tree;

import java.io.*;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.code.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;


/**
 * Prints out a tree as an indented Java source program.
 *
 * <p><b>This is NOT part of any API supported by Sun Microsystems. If you write code that depends
 * on this, you do so at your own risk. This code and its internal interfaces are subject to change
 * or deletion without notice.</b>
 */
//emitter for all type related stuff
public class LowerTypeImpl extends Emitter{
	boolean insidePure = false; //special handling when printing element type of an array (no pointer '*')
// ------------------- actual code emitter ---------------
	public LowerTypeImpl(LowerTreeImpl state) {
		super(state);
	}

	/**
	 * If type parameter list is non-empty, print it enclosed in "<...>" brackets.
	 */
	public void printTypeParameters(List<JCTypeParameter> trees) throws IOException {
		if (trees.nonEmpty()) {
			print("< ");
			printExprs(trees);
			print(" >");
		}
	}

	public void printCPPTypeParams(List<JCTypeParameter> typarams, List<Type> typarams_class) {
		//obsolete..we instantiate templates in Attr.java
		try {
			if (!typarams.isEmpty() || (!state.header && (typarams_class != null && typarams_class.size() > 0))) {
				print("template< ");
				for (JCTypeParameter tp : typarams) {
					if (tp != typarams.head) {
						print(", ");
					}
					print("typename ");
					printExpr(tp);
				}
				if (typarams_class != null && !state.header) {
					for (Type tp : typarams_class) {
						if (typarams.size() > 0) {
							print(", ");
						}
						print("typename ");
						printType(tp);
					}
				}
				print(" >");
				println();
				align();
			}
		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

	public void printCPPTemplateParams(List<Type> typarams_class) {
		//obsolete..we instantiate templates in Attr.java
		try {
			if ((typarams_class != null && typarams_class.size() > 0)) {
				print("< ");
				if (typarams_class != null) {
					for (Type tp : typarams_class) {
						if (tp != typarams_class.head) {
							print(", ");
						}
						printType(tp);
					}
				}
				print(" >");
			}
		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

	boolean isConst(Type t) { //const correctness ...
		if (t.tag == TypeTags.CLASS && ((Type.ClassType) t).getArrayType().tag == TypeTags.ARRAY) {
			return false;
		}
		return t.tag != TypeTags.TYPEVAR && (t.type_flags_field & Flags.FINAL) != 0 && t.tag != TypeTags.METHOD;
	}

	public String getResType(JCExpression e) {
		if (e != null) {
			return getTypeNoVoid(e.type, "unkown");
		} else {
			return "int";
		}
	}


	public void visitTypeCast(JCTypeCast tree) {
		try {
			if ((tree.expr.type.getArrayType().tag == TypeTags.ARRAY && tree.clazz.type.getArrayType().tag == TypeTags.ARRAY)
                    ||tree.clazz.type.toString().equals("String")) {
				state.arrayEmitter.visitTypeCast(tree);
				return;
			}

			state.open(state.prec, TreeInfo.prefixPrec);
			print("(");
			printExpr(tree.clazz);
			if (tree.clazz.getTag()==JCTree.IDENT&&!tree.type.isPrimitive()) {
				print("*");
			}
			print(")");
			printExpr(tree.expr, TreeInfo.prefixPrec);
			state.close(state.prec, TreeInfo.prefixPrec);
		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

	public void visitTypeTest(JCInstanceOf tree) {
		try {
			//obsolete
			state.log.error(tree.pos, "not.impl", tree);
			state.open(state.prec, TreeInfo.ordPrec);
			printExpr(tree.expr, TreeInfo.ordPrec);
			print(" instanceof ");
			printExpr(tree.clazz, TreeInfo.ordPrec + 1);
			state.close(state.prec, TreeInfo.ordPrec);
		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

	//--------------------------- TYPE PRINTING -----------------------
	public void printType(VarSymbol vs) {
		printType(vs.type, vs.name.toString());
	}

	public String getType(Type t) {
		return getType(t, "unkown");
	}

	//no mods like pointer and const
	public String getTypePure(Type t) {
		boolean oia = insidePure;
		insidePure = true;
		boolean oldIA = state.insideArray;
		state.insideArray = true;
		String res = getType(t);
		state.insideArray = oldIA;
		insidePure = oia;
		return res;
	}

	//type including pointer (but not const mod)
	public String getTypeNoConst(Type t) {
		boolean oia = insidePure;
		insidePure = true;
		String res = getType(t);
		insidePure = oia;
		return res;
	}

	//type including const (but not pointer mod)
	public String getTypeNoPointer(Type t) {
		boolean oldIA = state.insideArray;
		state.insideArray = true;

		String res = getType(t);
		state.insideArray = oldIA;
		return res;
	}

	public String getType(Type t, String n) {
		return getTypeInner(t, n, true);
	}

	public String getTypeName(Type t, String name) {

		switch (t.tag) {
			case TypeTags.BYTE:
				return ("byte");
			case TypeTags.CHAR:
				return ("char");
			case TypeTags.GROUP:
				return ("group");
			case TypeTags.SHORT:
				return ("short");
			case TypeTags.INT:
				return ("int");
			case TypeTags.LONG:
				return ("long");
			case TypeTags.FLOAT:
				return ("float");
			case TypeTags.DOUBLE:
				return ("double");
			case TypeTags.BOOLEAN:
				return ("bool");
			case TypeTags.VOID:
				return ("void");
			default:
				if (name.equals("String")) {
					return "char*";
				}

				return (name);
		}

	}

	public String getTypeInner(Type t, String n, boolean replace_array_class) {
		if (replace_array_class && t.tag == TypeTags.CLASS && ((Type.ClassType) t).getArrayType().tag == TypeTags.ARRAY) {
			return getType(((Type.ClassType) t).getArrayType(), n);
		}

		boolean projection = false;

		String res = "";

		String name = t.toString(n.toString());

		Type elt = null;
        boolean ptrDone=false;

		if (t.tag == TypeTags.ARRAY) {
			Type.ArrayType at = (Type.ArrayType) t;
			Type.DomainType dt = (Type.DomainType) at.dom;

			if (dt == null) {
				res += getTypePure(at.elemtype) + "*";
			} else {
				elt = at.elemtype;
				//int bucket_size = dt.getBucketSize(state.log, state.current_tree.pos(), dt.appliedParams.toArray(new JCTree.JCExpression[0]));

				//FIXME: use persistent array if updated with 'where'

				if (!at.treatAsBaseDomain()) {
					projection = true;
				}

				res += "funky::LinearArray< " + getTypePure(at.elemtype) + " >::Version*";
			}
		} else {
			if (!insidePure && isConst(t)) {
				res += " const ";
			}

			res += getTypeName(t, name);

			if ((!state.insideArray && t.tsym.owner.kind != Kinds.TYP && !t.isPrimitive()) || (state.insideArray && t.isPointer())) {
				res += "*";
                //ptrDone=true;
			}
		}

		if ((t.tag != TypeTags.METHOD && t.tag != TypeTags.TYPEVAR && !state.insideArray)) {
			if (!ptrDone&&t.isPointer()) {//handle FOUT (not FINOUT) for output args
				res += "*";
			}
		}

		return state.arrayEmitter.wrapProjection(projection, elt, res);
	}

	public String getTypeNoVoid(Type t, String n) {
		String res = getType(t, n);

		if (res.equals("void")) {
			return "int";
		}
		return res;
	}

	public String getTypeRestricted(Type t, String n) {
		String res = getType(t, n);

		if (t.isPointer() && (t.type_flags_field & Flags.ATOMIC) == 0 && (t.type_flags_field & Flags.LINEAR) != 0 && (t.type_flags_field & Flags.SINGULAR) == 0) {
			res += " __restrict__ ";//unique objects do not alias
		}
		return res;
	}

	public void printType(Type t, String n) {
		try {

			print(getTypeRestricted(t, n.toString()));
		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

	public void printType(Type t) {
		try {

			print(getTypeRestricted(t, "unknown"));
		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

	public void visitTypeIdent(JCPrimitiveTypeTree tree) {
		try {
			switch (tree.typetag) {
				case TypeTags.BYTE:
					print("byte");
					break;
				case TypeTags.CHAR:
					print("char");
					break;
				case TypeTags.GROUP:
					print("group");
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
					print("bool");
					break;
				case TypeTags.VOID:
					print("void");
					break;
				default:
					print("error");
					break;
			}
		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

	public void visitTypeArray(JCArrayTypeTree tree) {
		try {
			printBaseElementType(tree);
		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

	// Prints the inner element type of a nested array
	protected void printBaseElementType(JCArrayTypeTree tree) throws IOException {
		JCTree elem = tree.elemtype;
		while (elem instanceof JCWildcard) {
			elem = ((JCWildcard) elem).inner;
		}
		if (elem instanceof JCArrayTypeTree) {
			printBaseElementType((JCArrayTypeTree) elem);
		} else {
			printExpr(elem);
		}
	}

	public void visitTypeApply(JCTypeApply tree) {
		//dropped template args
		printType(tree.type);
	}

	public void visitTypeParameter(JCTypeParameter tree) {
		try {
			//obsolete?
			print(tree.name);

		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

}
