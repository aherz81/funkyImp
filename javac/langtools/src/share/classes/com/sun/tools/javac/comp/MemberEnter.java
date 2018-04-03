/*
 * Copyright 2003-2008 Sun Microsystems, Inc.  All Rights Reserved.
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
package com.sun.tools.javac.comp;

import java.util.*;
import java.util.Set;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;

import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import com.sun.tools.javac.code.Scope.Entry;
import static com.sun.tools.javac.code.TypeTags.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

//alex:test
import com.sun.tools.javac.main.JavaCompiler;

/**
 * This is the second phase of Enter, in which classes are completed by entering their members into
 * the class scope using MemberEnter.complete(). See Enter for an overview.
 *
 * <p><b>This is NOT part of any API supported by Sun Microsystems. If you write code that depends
 * on this, you do so at your own risk. This code and its internal interfaces are subject to change
 * or deletion without notice.</b>
 */
public class MemberEnter extends JCTree.Visitor implements Completer {

	protected static final Context.Key<MemberEnter> memberEnterKey =
			new Context.Key<MemberEnter>();
	/**
	 * A switch to determine whether we check for package/class conflicts
	 */
	final static boolean checkClash = true;
	private final Names names;
	private final Enter enter;
	private final Log log;
	private final Check chk;
	private final Attr attr;
	private final Symtab syms;
	private final TreeMaker make;
	private final ClassReader reader;
	private final Todo todo;
	private final Annotate annotate;
	private final Types types;
	private final JCDiagnostic.Factory diags;
	private final Target target;
	private final boolean skipAnnotations;
	private final JavaCompiler jc;

	public static MemberEnter instance(Context context) {
		MemberEnter instance = context.get(memberEnterKey);
		if (instance == null) {
			instance = new MemberEnter(context);
		}
		return instance;
	}

	protected MemberEnter(Context context) {
		context.put(memberEnterKey, this);
		names = Names.instance(context);
		enter = Enter.instance(context);
		log = Log.instance(context);
		chk = Check.instance(context);
		attr = Attr.instance(context);
		syms = Symtab.instance(context);
		make = TreeMaker.instance(context);
		reader = ClassReader.instance(context);
		todo = Todo.instance(context);
		annotate = Annotate.instance(context);
		types = Types.instance(context);
		diags = JCDiagnostic.Factory.instance(context);
		target = Target.instance(context);
		skipAnnotations =
				Options.instance(context).get("skipAnnotations") != null;

		//alex:test
		jc = JavaCompiler.instance(context);
	}
	/**
	 * A queue for classes whose members still need to be entered into the symbol table.
	 */
	ListBuffer<Env<AttrContext>> halfcompleted = new ListBuffer<Env<AttrContext>>();
	/**
	 * Set to true only when the first of a set of classes is processed from the halfcompleted
	 * queue.
	 */
	boolean isFirst = true;
	/**
	 * A flag to disable completion from time to time during member enter, as we only need to look
	 * up types. This avoids unnecessarily deep recursion.
	 */
	boolean completionEnabled = true;

	/* ---------- Processing import clauses ----------------
	 */
	/**
	 * Import all classes of a class or package on demand.
	 *
	 * @param pos Position to be used for error reporting.
	 * @param tsym The class or package the members of which are imported.
	 * @param toScope The (import) scope in which imported classes are entered.
	 */
	private void importAll(int pos,
			final TypeSymbol tsym,
			Env<AttrContext> env) {
		// Check that packages imported from exist (JLS ???).
		if (tsym.kind == PCK && tsym.members().elems == null && !tsym.exists()) {
			// If we can't find java.lang, exit immediately.
			if (((PackageSymbol) tsym).fullname.equals(names.java_lang)) {
				JCDiagnostic msg = diags.fragment("fatal.err.no.java.lang");
				throw new FatalError(msg);
			} else {
				log.error(pos, "doesnt.exist", tsym);
			}
		}
		final Scope fromScope = tsym.members();
		final Scope toScope = env.toplevel.starImportScope;
		for (Scope.Entry e = fromScope.elems; e != null; e = e.sibling) {
			if (e.sym.kind == TYP && !toScope.includes(e.sym)) {
				toScope.enter(e.sym, fromScope);
			}
		}
	}

	/**
	 * Import all static members of a class or package on demand.
	 *
	 * @param pos Position to be used for error reporting.
	 * @param tsym The class or package the members of which are imported.
	 * @param toScope The (import) scope in which imported classes are entered.
	 */
	private void importStaticAll(int pos,
			final TypeSymbol tsym,
			Env<AttrContext> env) {
		final JavaFileObject sourcefile = env.toplevel.sourcefile;
		final Scope toScope = env.toplevel.starImportScope;
		final PackageSymbol packge = env.toplevel.packge;
		final TypeSymbol origin = tsym;

		// enter imported types immediately
		new Object() {
			Set<Symbol> processed = new LinkedHashSet<Symbol>();

			void importFrom(TypeSymbol tsym) {
				if (tsym == null || !processed.add(tsym)) {
					return;
				}

				// also import inherited names
				importFrom(types.supertype(tsym.type).tsym);
				for (Type t : types.interfaces(tsym.type)) {
					importFrom(t.tsym);
				}

				final Scope fromScope = tsym.members();
				for (Scope.Entry e = fromScope.elems; e != null; e = e.sibling) {
					Symbol sym = e.sym;
					if (sym.kind == TYP
							&& (sym.flags() & STATIC) != 0
							&& staticImportAccessible(sym, packge)
							&& sym.isMemberOf(origin, types)
							&& !toScope.includes(sym)) {
						toScope.enter(sym, fromScope, origin.members());
					}
				}
			}
		}.importFrom(tsym);

		// enter non-types before annotations that might use them
		annotate.earlier(new Annotate.Annotator() {
			Set<Symbol> processed = new LinkedHashSet<Symbol>();

			public String toString() {
				return "import static " + tsym + ".*" + " in " + sourcefile;
			}

			void importFrom(TypeSymbol tsym) {
				if (tsym == null || !processed.add(tsym)) {
					return;
				}

				// also import inherited names
				importFrom(types.supertype(tsym.type).tsym);
				for (Type t : types.interfaces(tsym.type)) {
					importFrom(t.tsym);
				}

				final Scope fromScope = tsym.members();
				for (Scope.Entry e = fromScope.elems; e != null; e = e.sibling) {
					Symbol sym = e.sym;
					if (sym.isStatic() && sym.kind != TYP
							&& staticImportAccessible(sym, packge)
							&& !toScope.includes(sym)
							&& sym.isMemberOf(origin, types)) {
						toScope.enter(sym, fromScope, origin.members());
					}
				}
			}

			public void enterAnnotation() {
				importFrom(tsym);
			}
		});
	}

	// is the sym accessible everywhere in packge?
	boolean staticImportAccessible(Symbol sym, PackageSymbol packge) {
		int flags = (int) (sym.flags() & AccessFlags);
		switch (flags) {
			default:
			case PUBLIC:
				return true;
			case PRIVATE:
				return false;
			case 0:
			case PROTECTED:
				return sym.packge() == packge;
		}
	}

	/**
	 * Import statics types of a given name. Non-types are handled in Attr.
	 *
	 * @param pos Position to be used for error reporting.
	 * @param tsym The class from which the name is imported.
	 * @param name The (simple) name being imported.
	 * @param env The environment containing the named import scope to add to.
	 */
	private void importNamedStatic(final DiagnosticPosition pos,
			final TypeSymbol tsym,
			final Name name,
			final Env<AttrContext> env) {
		if (tsym.kind != TYP) {
			log.error(pos, "static.imp.only.classes.and.interfaces");
			return;
		}

		final Scope toScope = env.toplevel.namedImportScope;
		final PackageSymbol packge = env.toplevel.packge;
		final TypeSymbol origin = tsym;

		// enter imported types immediately
		new Object() {
			Set<Symbol> processed = new LinkedHashSet<Symbol>();

			void importFrom(TypeSymbol tsym) {
				if (tsym == null || !processed.add(tsym)) {
					return;
				}

				// also import inherited names
				importFrom(types.supertype(tsym.type).tsym);
				for (Type t : types.interfaces(tsym.type)) {
					importFrom(t.tsym);
				}

				for (Scope.Entry e = tsym.members().lookup(name);
						e.scope != null;
						e = e.next()) {
					Symbol sym = e.sym;
					if (sym.isStatic()
							&& sym.kind == TYP
							&& staticImportAccessible(sym, packge)
							&& sym.isMemberOf(origin, types)
							&& chk.checkUniqueStaticImport(pos, sym, toScope)) {
						toScope.enter(sym, sym.owner.members(), origin.members());
					}
				}
			}
		}.importFrom(tsym);

		// enter non-types before annotations that might use them
		annotate.earlier(new Annotate.Annotator() {
			Set<Symbol> processed = new LinkedHashSet<Symbol>();
			boolean found = false;

			public String toString() {
				return "import static " + tsym + "." + name;
			}

			void importFrom(TypeSymbol tsym) {
				if (tsym == null || !processed.add(tsym)) {
					return;
				}

				// also import inherited names
				importFrom(types.supertype(tsym.type).tsym);
				for (Type t : types.interfaces(tsym.type)) {
					importFrom(t.tsym);
				}

				for (Scope.Entry e = tsym.members().lookup(name);
						e.scope != null;
						e = e.next()) {
					Symbol sym = e.sym;
					if (sym.isStatic()
							&& staticImportAccessible(sym, packge)
							&& sym.isMemberOf(origin, types)) {
						found = true;
						if (sym.kind == MTH
								|| sym.kind != TYP && chk.checkUniqueStaticImport(pos, sym, toScope)) {
							toScope.enter(sym, sym.owner.members(), origin.members());
						}
					}
				}
			}

			public void enterAnnotation() {
				JavaFileObject prev = log.useSource(env.toplevel.sourcefile);
				try {
					importFrom(tsym);
					if (!found) {
						log.error(pos, "cant.resolve.location",
								KindName.STATIC,
								name, List.<Type>nil(), List.<Type>nil(),
								Kinds.typeKindName(tsym.type),
								tsym.type);
					}
				} finally {
					log.useSource(prev);
				}
			}
		});
	}

	/**
	 * Import given class.
	 *
	 * @param pos Position to be used for error reporting.
	 * @param tsym The class to be imported.
	 * @param env The environment containing the named import scope to add to.
	 */
	private void importNamed(DiagnosticPosition pos, Symbol tsym, Env<AttrContext> env) {
		if ((tsym.kind&(TYP|DOM))!=0
				&& chk.checkUniqueImport(pos, tsym, env.toplevel.namedImportScope)) {
			env.toplevel.namedImportScope.enter(tsym, tsym.owner.members());
		}
	}

	/**
	 * Construct method type from method signature.
	 *
	 * @param typarams The method's type parameters.
	 * @param params The method's value parameters.
	 * @param res The method's result type, null if it is a constructor.
	 * @param thrown The method's thrown exceptions.
	 * @param env The method's (local) environment.
	 */
	Type signature(List<JCTypeParameter> typarams,
			List<JCVariableDecl> params,
			JCTree res,
			long flags,
			List<JCExpression> thrown,
			Env<AttrContext> env) {

		// Enter and attribute type parameters.
		List<Type> tvars = enter.classEnter(typarams, env);
		attr.attribTypeVariables(typarams, env);

		// Enter and attribute value parameters.
		ListBuffer<Type> argbuf = new ListBuffer<Type>();
		for (List<JCVariableDecl> l = params; l.nonEmpty(); l = l.tail) {
			memberEnter(l.head, env);
			argbuf.append(l.head.vartype.type);
		}

		// Attribute result type, if one is given.
		Type restype = res == null ? syms.voidType : attr.attribType(res, env, flags);
/*
		if (restype != null && !restype.isPrimitive() && restype.tag != TypeTags.VOID) {
			restype = restype.addFlag(Flags.FOUT);
			res.type = restype;
		}
*/
		// Attribute thrown exceptions.
		ListBuffer<Type> thrownbuf = new ListBuffer<Type>();
		for (List<JCExpression> l = thrown; l.nonEmpty(); l = l.tail) {
			Type exc = attr.attribType(l.head, env);
			if (exc.tag != TYPEVAR) {
				exc = chk.checkClassType(l.head.pos(), exc);
			}
			thrownbuf.append(exc);
		}
		Type mtype = new MethodType(argbuf.toList(),
				restype,
				thrownbuf.toList(),
				syms.methodClass);
		return tvars.isEmpty() ? mtype : new ForAll(tvars, mtype);
	}

	Type signature(List<JCTypeParameter> typarams,
			List<JCVariableDecl> params,
			Type restype,
			List<JCExpression> thrown,
			Env<AttrContext> env) {

		// Enter and attribute type parameters.
		List<Type> tvars = enter.classEnter(typarams, env);
		attr.attribTypeVariables(typarams, env);

		// Enter and attribute value parameters.
		ListBuffer<Type> argbuf = new ListBuffer<Type>();
		for (List<JCVariableDecl> l = params; l.nonEmpty(); l = l.tail) {
			memberEnter(l.head, env);
			argbuf.append(l.head.vartype.type);
		}

		// Attribute result type, if one is given.
		//Type restype = res == null ? syms.voidType : attr.attribType(res, env);
/*
		if (restype != null && !restype.isPrimitive() && restype.tag != TypeTags.VOID) {
			restype = restype.addFlag(Flags.FOUT);
			//res.type=restype;
		}
*/
		// Attribute thrown exceptions.
		ListBuffer<Type> thrownbuf = new ListBuffer<Type>();
		for (List<JCExpression> l = thrown; l.nonEmpty(); l = l.tail) {
			Type exc = attr.attribType(l.head, env);
			if (exc.tag != TYPEVAR) {
				exc = chk.checkClassType(l.head.pos(), exc);
			}
			thrownbuf.append(exc);
		}
		Type mtype = new MethodType(argbuf.toList(),
				restype,
				thrownbuf.toList(),
				syms.methodClass);
		return tvars.isEmpty() ? mtype : new ForAll(tvars, mtype);
	}

	/* ********************************************************************
	 * Visitor methods for member enter
	 *********************************************************************/
	/**
	 * Visitor argument: the current environment
	 */
	protected Env<AttrContext> env;

	/**
	 * Enter field and method definitions and process import clauses, catching any completion
	 * failure exceptions.
	 */
	protected void memberEnter(JCTree tree, Env<AttrContext> env) {
		Env<AttrContext> prevEnv = this.env;
		try {
			this.env = env;
			tree.accept(this);
		} catch (CompletionFailure ex) {
			chk.completionError(tree.pos(), ex);
		} finally {
			this.env = prevEnv;
		}
	}

	/**
	 * Enter members from a list of trees.
	 */
	void memberEnter(List<? extends JCTree> trees, Env<AttrContext> env) {
		for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail) {
			memberEnter(l.head, env);
		}
	}

	/**
	 * Enter members for a class.
	 */
	void finishClass(JCClassDecl tree, Env<AttrContext> env) {

		//ALEX: make it a template
		if(tree.typarams.size()>0)
			return;

		if ((tree.mods.flags & Flags.ENUM) != 0
				&& (types.supertype(tree.sym.type).tsym.flags() & Flags.ENUM) == 0) {
			addEnumMembers(tree, env);
		}
		memberEnter(tree.defs, env);
	}

	/**
	 * Enter members for a class.
	 */
	void finishDomain(JCDomainDecl tree, Env<AttrContext> env) {
		memberEnter(tree.defs, env);
	}

	/**
	 * Add the implicit members for an enum type to the symbol table.
	 */
	private void addEnumMembers(JCClassDecl tree, Env<AttrContext> env) {
		JCExpression valuesType = make.Type(new ArrayType(tree.sym.type, null, syms.arrayClass));

		// public static T[] values() { return ???; }
		JCMethodDecl values = make.
				MethodDef(make.Modifiers(Flags.PUBLIC | Flags.STATIC),
				names.values,
				valuesType,
				List.<JCTypeParameter>nil(),
				List.<JCVariableDecl>nil(),
				List.<JCExpression>nil(), // thrown
				null, //make.Block(0, Tree.emptyList.prepend(make.Return(make.Ident(names._null)))),
				null);
		memberEnter(values, env);

		// public static T valueOf(String name) { return ???; }
		JCMethodDecl valueOf = make.
				MethodDef(make.Modifiers(Flags.PUBLIC | Flags.STATIC),
				names.valueOf,
				make.Type(tree.sym.type),
				List.<JCTypeParameter>nil(),
				List.of(make.VarDef(make.Modifiers(Flags.PARAMETER),
				names.fromString("name"),
				make.Type(syms.stringType), null)),
				List.<JCExpression>nil(), // thrown
				null, //make.Block(0, Tree.emptyList.prepend(make.Return(make.Ident(names._null)))),
				null);
		memberEnter(valueOf, env);

		// the remaining members are for bootstrapping only
		if (!target.compilerBootstrap(tree.sym)) {
			return;
		}

		// public final int ordinal() { return ???; }
		JCMethodDecl ordinal = make.at(tree.pos).
				MethodDef(make.Modifiers(Flags.PUBLIC | Flags.FINAL),
				names.ordinal,
				make.Type(syms.intType),
				List.<JCTypeParameter>nil(),
				List.<JCVariableDecl>nil(),
				List.<JCExpression>nil(),
				null,
				null);
		memberEnter(ordinal, env);

		// public final String name() { return ???; }
		JCMethodDecl name = make.
				MethodDef(make.Modifiers(Flags.PUBLIC | Flags.FINAL),
				names._name,
				make.Type(syms.stringType),
				List.<JCTypeParameter>nil(),
				List.<JCVariableDecl>nil(),
				List.<JCExpression>nil(),
				null,
				null);
		memberEnter(name, env);

		// public int compareTo(E other) { return ???; }
		MethodSymbol compareTo = new MethodSymbol(Flags.PUBLIC,
				names.compareTo,
				new MethodType(List.of(tree.sym.type),
				syms.intType,
				List.<Type>nil(),
				syms.methodClass),
				tree.sym);
		memberEnter(make.MethodDef(compareTo, null), env);
	}

	public void visitTopLevel(JCCompilationUnit tree) {
		if (tree.starImportScope.elems == null) {

		// check that no class exists with same fully qualified name as
		// toplevel package
		if (checkClash && tree.pid != null) {
			Symbol p = tree.packge;
			while (p.owner != syms.rootPackage) {
				p.owner.complete(); // enter all class members of p
				if (syms.classes.get(p.getQualifiedName()) != null) {
					log.error(tree.pos,
							"pkg.clashes.with.class.of.same.name",
							p);
				}
				p = p.owner;
			}
		}

		// process package annotations
		annotateLater(tree.packageAnnotations, env, tree.packge);

			// Import-on-demand java.lang.
			//ALEX: DO NOT IMPORT java.lang!
			//importAll(tree.pos, reader.enterPackage(names.java_lang), env);
			// we must have already processed this toplevel
		}

		// Process all import clauses.
		memberEnter(tree.defs, env);
	}

	// process the non-static imports and the static imports of types.
	public void visitImport(JCImport tree) {
		JCTree imp = tree.qualid;
		Name name = TreeInfo.name(imp);
		TypeSymbol p;

		// Create a local environment pointing to this tree to disable
		// effects of other imports in Resolve.findGlobalType
		Env<AttrContext> localEnv = env.dup(tree);

		// Attribute qualifying package or class.
		JCFieldAccess s = (JCFieldAccess) imp;
		p = attr.
				attribTree(s.selected,
				localEnv,
				tree.staticImport ? TYP : (TYP | PCK),
				Type.noType).tsym;
		if (name == names.asterisk) {
			// Import on demand.
			chk.checkCanonical(s.selected);
			if (tree.staticImport) {
				importStaticAll(tree.pos, p, env);
			} else {
				importAll(tree.pos, p, env);
			}
		} else {
			// Named type import.
			if (tree.staticImport) {
				importNamedStatic(tree.pos(), p, name, localEnv);
				chk.checkCanonical(s.selected);
			} else {
				TypeSymbol c = attribImportType(imp, localEnv).tsym;
				chk.checkCanonical(imp);
				importNamed(tree.pos(), c, env);
			}
		}
	}

	<T extends JCTree> T replace(T cu,final JCTree __this__) {
			class Replace extends TreeTranslator {

				public <T extends JCTree> T translate(T tree) {
					if (tree == null) {
						return null;
					} else {
						make.at(tree.pos());
						T result = super.translate(tree);
						return result;
					}
				}


				public void visitIdent(JCTree.JCIdent tree) {
					if(!(tree.name.equals(names._this)))
						super.visitIdent(tree);
					else
						result=__this__;

				}
			}
			Replace v = new Replace();
			return v.translate(cu);
		}

	public void visitMethodDef(JCMethodDecl tree) {
		Scope enclScope = enter.enterScope(env);

		//all methods in class derived from array type are operators
		boolean is_operator=env.enclClass.extending!=null&&env.enclClass.extending.type!=null&&env.enclClass.extending.type.getArrayType().tag==TypeTags.ARRAY&&(tree.name != tree.name.table.names.init);

		if(is_operator)
		{
			//may be executed multipe times on same tree?
			if(tree.params.size()==0||!tree.params.head.name.equals(names.fromString("__this__")))
			{
				tree.params=tree.params.prepend(make.VarDef(make.Modifiers(Flags.PARAMETER),names.fromString("__this__"),make.Type(env.enclClass.sym.thisSym.type), null));
				tree.mods.flags|=Flags.STATIC;
				tree.body=replace(tree.body,make.Ident(names.fromString("__this__")));
			}
		}

		MethodSymbol m = new MethodSymbol(0, tree.name, null, enclScope.owner, tree);
		m.flags_field = chk.checkFlags(tree.pos(), tree.mods.flags, m, tree);
		tree.sym = m;
		tree.sym.decl = tree;

		Env<AttrContext> localEnv = methodEnv(tree, env);

		// Compute the method type
		m.type = signature(tree.typarams, tree.params,
				tree.restype,tree.mods.flags, tree.thrown,
				localEnv);

		// Set m.params
		ListBuffer<VarSymbol> params = new ListBuffer<VarSymbol>();
		JCVariableDecl lastParam = null;
		for (List<JCVariableDecl> l = tree.params; l.nonEmpty(); l = l.tail) {
			JCVariableDecl param = lastParam = l.head;
			assert param.sym != null;
			params.append(param.sym);
		}
		m.params = params.toList();

		// mark the method varargs, if necessary
		if (lastParam != null && (lastParam.mods.flags & Flags.VARARGS) != 0) {
			m.flags_field |= Flags.VARARGS;
		}

		if(is_operator){
			m.flags_field|=Flags.BRIDGE; //mark as array operator
		}

		localEnv.info.scope.leave();
		if (chk.checkUnique(tree.pos(), m, enclScope)) {
			enclScope.enter(m);
		}
		annotateLater(tree.mods.annotations, localEnv, m);
		if (tree.defaultValue != null) {
			annotateDefaultValueLater(tree.defaultValue, localEnv, m);
		}
	}

	/**
	 * Create a fresh environment for method bodies.
	 *
	 * @param tree The method definition.
	 * @param env The environment current outside of the method definition.
	 */
	Env<AttrContext> methodEnv(JCMethodDecl tree, Env<AttrContext> env) {
		Env<AttrContext> localEnv =
				env.dup(tree, env.info.dup(env.info.scope.dupUnshared()));
		localEnv.enclMethod = tree;
		localEnv.info.scope.owner = tree.sym;
		if ((tree.mods.flags & STATIC) != 0) {
			localEnv.info.staticLevel++;
		}
		return localEnv;
	}

	/**
	 * Create a fresh environment for domain iterations.
	 *
	 * @param tree The dom iter definition.
	 * @param env The environment current outside of the method definition.
	 */
	Env<AttrContext> domiterEnv(JCDomainIter tree, Env<AttrContext> env) {
		Env<AttrContext> localEnv =
				env.dup(tree, env.info.dup(env.info.scope.dupUnshared()));
		//localEnv.enclMethod = tree;
		//localEnv.enclMethod
		localEnv.info.scope.owner = tree.sym;
		//if ((tree.mods.flags & STATIC) != 0) localEnv.info.staticLevel++;
		return localEnv;
	}

	/**
	 * Create a fresh environment for domain iterations.
	 *
	 * @param tree The dom iter definition.
	 * @param env The environment current outside of the method definition.
	 */
	Env<AttrContext> whereEnv(JCWhere tree, Env<AttrContext> env) {
		Env<AttrContext> localEnv =
				env.dup(tree, env.info.dup(env.info.scope.dupUnshared()));
		//localEnv.enclMethod = tree;
		//localEnv.enclMethod
		localEnv.info.scope.owner.flags_field |= WHEREBLOCK;
		//localEnv.info.scope.owner = tree.sym;
		//if ((tree.mods.flags & STATIC) != 0) localEnv.info.staticLevel++;
		return localEnv;
	}

	Env<AttrContext> localEnv( Env<AttrContext> env) {
		Env<AttrContext> localEnv =
				env.dup(null, env.info.dup(env.info.scope.dupUnshared()));
		//localEnv.enclMethod = tree;
		//localEnv.enclMethod
		//localEnv.info.scope.owner = tree.sym;
		//if ((tree.mods.flags & STATIC) != 0) localEnv.info.staticLevel++;
		return localEnv;
	}

	public void visitVarDef(JCVariableDecl tree) {

		Env<AttrContext> localEnv = env;
		if ((tree.mods.flags & STATIC) != 0
				|| (env.info.scope.owner.flags() & INTERFACE) != 0) {
			localEnv = env.dup(tree, env.info.dup());
			localEnv.info.staticLevel++;
		}

		Scope enclScope = enter.enterScope(env);
		VarSymbol v =
				new VarSymbol(0, tree.name, null, enclScope.owner);

		attr.insideVarDecl=v;
		attr.attribType(tree.vartype, localEnv,tree.mods.flags);
		attr.insideVarDecl=null;

/*
		if ((tree.mods.flags & Flags.FOUT) != 0) {
			tree.sym.flags_field|=Flags.FOUT;
		}
*/
		if ((tree.mods.flags & Flags.UNSIGNED) != 0) {
			tree.vartype.type = tree.vartype.type.addFlag(Flags.UNSIGNED);
		}

		v.type=tree.vartype.type;

		v.flags_field = chk.checkFlags(tree.pos(), tree.mods.flags, v, tree);

		if (tree.list != null) {
			ListBuffer<JCTypeParameter> params = new ListBuffer<JCTypeParameter>();
			ListBuffer<JCExpression> thrown = new ListBuffer<JCExpression>();
			v.type = tree.vartype.type = signature(params.toList(), tree.list,
					v.type, thrown.toList(),
					localEnv);

			//v.type=new MethodType();
		}

		tree.sym = v;
		if (tree.init != null) {
			v.flags_field |= HASINIT;
			if ((v.flags_field & FINAL) != 0 && tree.init.getTag() != JCTree.NEWCLASS) {
				Env<AttrContext> initEnv = getInitEnv(tree, env);
				initEnv.info.enclVar = v;
				v.setLazyConstValue(initEnv(tree, initEnv), log, attr, tree.init);
			}
		}
		if (chk.checkUnique(tree.pos(), v, enclScope)) {
			chk.checkTransparentVar(tree.pos(), v, enclScope);
			enclScope.enter(v);

			//ALEX: CTP:
			//FIXME: handle different owners (fn/class)
			if (enclScope.owner.type != null) {
				enclScope.owner.type.AddSetCTP(names.MemberVariables, make.Ident(tree.getName()));
			}
		}
		annotateLater(tree.mods.annotations, localEnv, v);
		v.pos = tree.pos;
	}

	/**
	 * Create a fresh environment for a variable's initializer. If the variable is a field, the
	 * owner of the environment's scope is be the variable itself, otherwise the owner is the method
	 * enclosing the variable definition.
	 *
	 * @param tree The variable definition.
	 * @param env The environment current outside of the variable definition.
	 */
	Env<AttrContext> initEnv(JCVariableDecl tree, Env<AttrContext> env) {
		Env<AttrContext> localEnv = env.dupto(new AttrContextEnv(tree, env.info.dup()));
		if (tree.sym.owner.kind == TYP) {
			localEnv.info.scope = new Scope.DelegatedScope(env.info.scope);
			localEnv.info.scope.owner = tree.sym;
		}
		if ((tree.mods.flags & STATIC) != 0
				|| (env.enclClass.sym.flags() & INTERFACE) != 0) {
			localEnv.info.staticLevel++;
		}
		return localEnv;
	}


	/**
	 * Default member enter visitor method: do nothing
	 */
	public void visitTree(JCTree tree) {
	}

	public void visitErroneous(JCErroneous tree) {
		memberEnter(tree.errs, env);
	}

	public Env<AttrContext> getMethodEnv(JCMethodDecl tree, Env<AttrContext> env) {
		Env<AttrContext> mEnv = methodEnv(tree, env);
		mEnv.info.lint = mEnv.info.lint.augment(tree.sym.attributes_field, tree.sym.flags());
		for (List<JCTypeParameter> l = tree.typarams; l.nonEmpty(); l = l.tail) {
			mEnv.info.scope.enterIfAbsent(l.head.type.tsym);
		}
		for (List<JCVariableDecl> l = tree.params; l.nonEmpty(); l = l.tail) {
			mEnv.info.scope.enterIfAbsent(l.head.sym);
		}
		return mEnv;
	}

	public Env<AttrContext> getInitEnv(JCVariableDecl tree, Env<AttrContext> env) {
		Env<AttrContext> iEnv = initEnv(tree, env);
		return iEnv;
	}

	/* ********************************************************************
	 * Type completion
	 *********************************************************************/
	Type attribImportType(JCTree tree, Env<AttrContext> env) {
		assert completionEnabled;
		try {
			// To prevent deep recursion, suppress completion of some
			// types.
			completionEnabled = false;
			return attr.attribType(tree, env);
		} finally {
			completionEnabled = true;
		}
	}

	/* ********************************************************************
	 * Annotation processing
	 *********************************************************************/
	/**
	 * Queue annotations for later processing.
	 */
	void annotateLater(final List<JCAnnotation> annotations,
			final Env<AttrContext> localEnv,
			final Symbol s) {
		if (annotations.isEmpty()) {
			return;
		}
		if (s.kind != PCK) {
			s.attributes_field = null; // mark it incomplete for now
		}
		annotate.later(new Annotate.Annotator() {
			public String toString() {
				return "annotate " + annotations + " onto " + s + " in " + s.owner;
			}

			public void enterAnnotation() {
				assert s.kind == PCK || s.attributes_field == null;
				JavaFileObject prev = log.useSource(localEnv.toplevel.sourcefile);
				try {
					if (s.attributes_field != null
							&& s.attributes_field.nonEmpty()
							&& annotations.nonEmpty()) {
						log.error(annotations.head.pos,
								"already.annotated",
								kindName(s), s);
					}
					enterAnnotations(annotations, localEnv, s);
				} finally {
					log.useSource(prev);
				}
			}
		});
	}

	/**
	 * Check if a list of annotations contains a reference to java.lang.Deprecated.
     *
	 */
	private boolean hasDeprecatedAnnotation(List<JCAnnotation> annotations) {
		for (List<JCAnnotation> al = annotations; al.nonEmpty(); al = al.tail) {
			JCAnnotation a = al.head;
			if (a.annotationType.type == syms.deprecatedType && a.args.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Enter a set of annotations.
	 */
	private void enterAnnotations(List<JCAnnotation> annotations,
			Env<AttrContext> env,
			Symbol s) {
		ListBuffer<Attribute.Compound> buf =
				new ListBuffer<Attribute.Compound>();
		Set<TypeSymbol> annotated = new LinkedHashSet<TypeSymbol>();
		if (!skipAnnotations) {
			for (List<JCAnnotation> al = annotations; al.nonEmpty(); al = al.tail) {
				JCAnnotation a = al.head;
				Attribute.Compound c = annotate.enterAnnotation(a,
						syms.annotationType,
						env);
				if (c == null) {
					continue;
				}
				buf.append(c);
				// Note: @Deprecated has no effect on local variables and parameters
				if (!c.type.isErroneous()
						&& s.owner.kind != MTH
						&& types.isSameType(c.type, syms.deprecatedType)) {
					s.flags_field |= Flags.DEPRECATED;
				}
				if (!annotated.add(a.type.tsym)) {
					log.error(a.pos, "duplicate.annotation");
				}
			}
		}
		s.attributes_field = buf.toList();
	}

	/**
	 * Queue processing of an attribute default value.
	 */
	void annotateDefaultValueLater(final JCExpression defaultValue,
			final Env<AttrContext> localEnv,
			final MethodSymbol m) {
		annotate.later(new Annotate.Annotator() {
			public String toString() {
				return "annotate " + m.owner + "."
						+ m + " default " + defaultValue;
			}

			public void enterAnnotation() {
				JavaFileObject prev = log.useSource(localEnv.toplevel.sourcefile);
				try {
					enterDefaultValue(defaultValue, localEnv, m);
				} finally {
					log.useSource(prev);
				}
			}
		});
	}

	/**
	 * Enter a default value for an attribute method.
	 */
	private void enterDefaultValue(final JCExpression defaultValue,
			final Env<AttrContext> localEnv,
			final MethodSymbol m) {
		m.defaultValue = annotate.enterAttributeValue(m.type.getReturnType(),
				defaultValue,
				localEnv);
	}

	/* ********************************************************************
	 * Source completer
	 *********************************************************************/
	public void complete(DomainSymbol dsym) throws CompletionFailure {
		// symbol and parameters have already been added in Enter
		// here we have to add parent, result type, indics and projection arguments

		DomainType dt = (DomainType) dsym.type;
		Env<AttrContext> env = enter.typeEnvs.get(dsym);
		JCDomainDecl tree = (JCDomainDecl) env.tree;

		boolean wasFirst = isFirst;
		isFirst = false;

		JavaFileObject prev = log.useSource(env.toplevel.sourcefile);
		try {

			// Save class environment for later member enter (2) processing.
			halfcompleted.append(env);

			// Mark class as not yet attributed.
			dsym.flags_field |= UNATTRIBUTED;

			// If this is a toplevel-class, make sure any preceding import
			// clauses have been seen.
			if (dsym.owner.kind == PCK) {
				memberEnter(env.toplevel, env.enclosing(JCTree.TOPLEVEL));
				todo.append(env);
			} else if (dsym.owner.kind == TYP) {
				dsym.owner.complete();
			}

			// create an environment for evaluating the base clauses
			Env<AttrContext> baseEnv = baseEnv(tree, env);

			// create map from names to symbols for all variables in this domain
			// = params, projArgs, indices (this inlcudes parentParams)
			Map<Name, VarSymbol> varmap = new LinkedHashMap<Name, VarSymbol>();
			Map<Name, VarSymbol> parent_varmap = new LinkedHashMap<Name, VarSymbol>();
			for (VarSymbol v : dt.formalParams) {
				varmap.put(v.name, v);
			}

			// if there is a parent domain, this is a projection
			if (!dt.isBaseDomain) {

				// if there is a parent declaration, attribute it first
				// (this does NOT attribute the parent domain)
				DomainType parentdom = (DomainType) attr.attribBaseDom(tree.domdef, baseEnv);
				dt.parentDomain = parentdom;

				// test that parent is a base domain (is this realy neccessary?)
				if (false&&!parentdom.isBaseDomain) {
					log.error(tree.pos(), "domain.not.a.base.domain", parentdom.tsym);
				}

				// test number of parameters for parent declaration
				if (tree.domdef.domparams.length() != parentdom.formalParams.length()) {
					log.error(tree.pos(), "domain.wrong.number.of.params", parentdom.tsym);
				}

				// ensure that all parameters of the parent declaration are also
				// parameters of the projection domain
				ListBuffer<VarSymbol> parentParams = new ListBuffer<VarSymbol>();
				for (JCDomParameter p : tree.domdef.domparams) {
					Entry ps = env.info.scope.lookup(p.name);
					// symbol already exists
					if (ps.sym != null && ps.sym instanceof VarSymbol
							&& dt.formalParams.contains((VarSymbol) ps.sym)) {
						parentParams.add((VarSymbol) ps.sym);
					} // else create symbol for supertype parameter
					else {
						log.error(tree.pos(), "domain.not.a.parameter", dsym, p.name);
						VarSymbol psym = new VarSymbol(0, p.name, syms.intType, dsym);
						parentParams.add(psym);
					}
				}
				dt.parentParams = parentParams.toList();

				// the number of indices of this domain has to match he number
				// of indices of the parent
				if (tree.defs.length() != parentdom.indices.length()) {
					log.error(tree.pos(), "domain.wrong.number.of.indices", parentdom.tsym);
				}

				// add projection arguments
				ListBuffer<VarSymbol> projectionArgs = new ListBuffer<VarSymbol>();
				for (JCDomParameter index : tree.domargs) {
					if (varmap.containsKey(index.name)) {
						log.error(tree.pos(), "domain.variable.already.defined", dsym, index.name);
					} else {
						VarSymbol isym = new VarSymbol(0, index.name, syms.intType, dsym);
						projectionArgs.add(isym);
						varmap.put(index.name, isym);
						//env.info.scope.enter(isym);
					}
				}
				dt.projectionArgs = projectionArgs.toList();

			} // no parent domain -> no projection but base domain
			else {

				// in a base domain no projection arguments are allowed
				if (tree.domargs.length() != 0) {
					log.error(tree.pos(), "domain.wrong.number.of.indices", dsym);
				}

			}

			// create indices
			ListBuffer<VarSymbol> indices = new ListBuffer<VarSymbol>();
			for (JCDomParameter p : tree.defs) {
				// get varsymbol if variale is a projection arg of the domain
				// an index symbol may not alrady exist
				if (varmap.containsKey(p.name)) {
					log.error(tree.pos(), "domain.variable.already.defined", dsym, p.name);
					indices.add(null);
				} else {
					VarSymbol varsym = new VarSymbol(0, p.name, syms.intType, dsym);
					indices.add(varsym);
					varmap.put(p.name, varsym);
					//env.info.scope.enter(varsym);
				}
			}
			dt.indices = indices.toList();
/*
			// result type declarations are only allowed for non-base domains
			if (dt.isBaseDomain && tree.parent != null) {
				log.error(tree.pos(), "domain.result.type.in.base.domain", dsym);
			} // if there is a result type declaration, attribute it first
			// (this does NOT attribute the domain)
			else
*/
			if (tree.parent != null) {

				// get supertype domain
				DomainType resultDom = (DomainType) attr.attribBaseDom(tree.parent, baseEnv);
				resultDom = (DomainType) resultDom.clone();
				dt.resultDom = resultDom;

				// test number of parameters
				if (tree.parent.domparams.length() != resultDom.formalParams.length()) {
					log.error(tree.pos(), "domain.wrong.number.of.params", resultDom.tsym);
				}

				dt.resultDomParams = tree.parent.domparams;

				// add projection arguments
				ListBuffer<VarSymbol> projectionArgs = new ListBuffer<VarSymbol>();
				for (JCDomParameter index : tree.parent.domargs) {
					if (varmap.containsKey(index.name) || parent_varmap.containsKey(index.name)) {
						log.error(tree.pos(), "domain.variable.already.defined", dsym, index.name);
					} else {
						VarSymbol isym = new VarSymbol(0, index.name, syms.intType, dsym);
						projectionArgs.add(isym);
						parent_varmap.put(index.name, isym);
						//env.info.scope.enter(isym);
					}
				}
				resultDom.projectionArgs = projectionArgs.toList();


			}

			// add constraints to domain type and check that all used variables are defined
			ListBuffer<DomainConstraint> constraints = new ListBuffer<DomainConstraint>();
			ListBuffer<DomainConstraint> parent_constraints = new ListBuffer<DomainConstraint>();

			// constraints of this domain
			parent_varmap.putAll(varmap);
			for (JCTree c : tree.constraints) {
				DomainConstraint cs = makeConstraint((JCDomConstraint) c, varmap, tree, dsym, false);
				if (cs != null) {
					constraints.add(cs);
				} else {
					cs = makeConstraint((JCDomConstraint) c, parent_varmap, tree, dsym, true);
					if (cs != null) {
						parent_constraints.add(cs);
					}
					//parent_constraints.add(cs);
				}
			}

			if (dt.resultDom != null) {
				((DomainType) dt.resultDom).constraints = parent_constraints.toList();
			}

			// constraints of the parent domain
			if (!dt.isBaseDomain) {

				DomainType baseDt = (DomainType) dt.parentDomain;

				// create mapping from names in base domain to variables in this domain
				Map<VarSymbol, VarSymbol> basevarmap = new LinkedHashMap<VarSymbol, VarSymbol>();
				for (int i = 0; i < baseDt.formalParams.length(); i++) {
					VarSymbol parentVar = baseDt.formalParams.get(i);
					VarSymbol childVar = dt.parentParams.get(i);
					basevarmap.put(parentVar, childVar);
				}
				for (int i = 0; i < baseDt.indices.length(); i++) {
					VarSymbol parentVar = baseDt.indices.get(i);
					VarSymbol childVar = dt.indices.get(i);
					basevarmap.put(parentVar, childVar);
				}

				// add base constraints
				for (DomainConstraint c : baseDt.constraints) {
					DomainConstraint cs = convertParentConstraint(c, basevarmap);
					if (cs != null) {
						constraints.add(cs);
					}
				}

			}

			dt.constraints = constraints.toList();

			// Determine interfaces.
			ListBuffer<Type> interfaces = new ListBuffer<Type>();
			//Set<Type> interfaceSet = new LinkedHashSet<Type>();
			interfaces.addAll(syms.domain_default_interfaces); //add size and reduce

			if ((dsym.flags_field & ANNOTATION) != 0) {
				dt.interfaces_field = List.of(syms.annotationType);
			} else {
				dt.interfaces_field = interfaces.toList();
			}

			// check that no package exists with same fully qualified name,
			// but admit classes in the unnamed package which have the same
			// name as a top-level package.
			if (checkClash
					&& dsym.owner.kind == PCK && dsym.owner != syms.unnamedPackage
					&& reader.packageExists(dsym.fullname)) {
				log.error(tree.pos, "clash.with.pkg.of.same.name", dsym);
			}

			dt.registerBarvinok(tree.pos());

		} catch (CompletionFailure ex) {
			chk.completionError(tree.pos(), ex);
		} finally {
			log.useSource(prev);
		}

		// Enter all member fields and methods of a set of half completed
		// classes in a second phase.
		if (wasFirst) {
			try {
				while (halfcompleted.nonEmpty()) {
					finish(halfcompleted.next());
				}
			} finally {
				isFirst = true;
			}

			// commit pending annotations
			annotate.flush();
		}
	}

	private DomainConstraint makeConstraint(JCDomConstraint constraint,
			Map<Name, VarSymbol> domvarMap, JCDomainDecl tree, DomainSymbol dsym, boolean allow_parent) {

		// lists for used variables and coefficients
		Map<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>> coeffs = new LinkedHashMap<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>>();
		int constant = 0;

		// check lhs
        /*if(domvarMap.containsKey(constraint.name)) {
		 VarSymbol v = domvarMap.get(constraint.name);
		 int old = (coeffs.containsKey(v)) ? coeffs.get(v) : 0;
		 coeffs.put(v, old + 1);
		 } else {
		 log.error(tree.pos(), "domain.not.a.parameter.or.index", dsym, constraint.name);
		 return null;
		 }*/
		for (JCDomConstraintValue cv : constraint.left) {

			// get coefficient (use 1 as a default value if none exists)
			int coeff = 1;
			if (cv.coeff != null) {
				Object constval = attr.attribExpr(cv.coeff, env).constValue();
				if (constval != null && constval instanceof Integer) {
					coeff = (Integer) constval;
				} else {
					log.error(tree.pos(), "domain.invalid.literal.in.constraint", cv.coeff, dsym);
					return null;
				}
			}

			// Multiply coefficient by sign, note that both cv.sign and
			// cv.coeff can be negative at the same time.  In this case
			// they cancel out each other.
			coeff *= cv.sign;

			// varaible with coefficient
			if (cv.parameter != null) {
				if (domvarMap.containsKey(cv.parameter.name) || allow_parent) {
					VarSymbol v = domvarMap.get(cv.parameter.name);
					int newVal = coeff;
					java.util.List<Pair<Integer,VarSymbol>> ll= coeffs.get(v);
					if(ll==null)
						ll=new java.util.LinkedList<Pair<Integer,VarSymbol>>();
					Pair<Integer,VarSymbol> pp=new Pair<Integer,VarSymbol>(newVal,cv.parameter1==null ? null:domvarMap.get(cv.parameter1.name));
					ll.add(pp);
					coeffs.put(v, ll);
				} else {
					//log.error(tree.pos(), "domain.not.a.parameter.or.index", dsym, cv.parameter.name);
					return null;
				}
			} // constant only
			else {
				constant -= coeff; // "-" because it was moved to the other side
			}

		}

		// check rhs
		for (JCDomConstraintValue cv : constraint.right) {

			// get coefficient (use 1 as a default value if none exists)
			int coeff = 1;
			if (cv.coeff != null) {
				Object constval = attr.attribExpr(cv.coeff, env).constValue();
				if (constval != null && constval instanceof Integer) {
					coeff = (Integer) constval;
				} else {
					log.error(tree.pos(), "domain.invalid.literal.in.constraint", cv.coeff, dsym);
					return null;
				}
			}

			// Multiply coefficient by sign, note that both cv.sign and
			// cv.coeff can be negative at the same time.  In this case
			// they cancel out each other.
			coeff *= cv.sign;

			// varaible with coefficient
			if (cv.parameter != null) {
				if (domvarMap.containsKey(cv.parameter.name)) {
					VarSymbol v = domvarMap.get(cv.parameter.name);
					int newVal = - coeff;
					java.util.List<Pair<Integer,VarSymbol>> ll= coeffs.get(v);
					if(ll==null)
						ll=new java.util.LinkedList<Pair<Integer,VarSymbol>>();
					Pair<Integer,VarSymbol> pp=new Pair<Integer,VarSymbol>(newVal,cv.parameter1==null ? null:domvarMap.get(cv.parameter1.name));
					ll.add(pp);
					coeffs.put(v, ll);
				} else {
					log.error(tree.pos(), "domain.not.a.parameter.or.index", dsym, cv.parameter.name);
					return null;
				}
			} // constant only
			else {
				constant += coeff;
			}

		}

		// change signs to convert from >=/> to <=/<
		if (constraint.assign == JCTree.GE || constraint.assign == JCTree.GT) {
			for (Map.Entry<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>> e : coeffs.entrySet()) {
				java.util.List<Pair<Integer,VarSymbol>> ll=new java.util.LinkedList<Pair<Integer,VarSymbol>>();
				for(Pair<Integer,VarSymbol> p:e.getValue())
				{
					ll.add(new Pair<Integer,VarSymbol>(p.fst * -1,p.snd));
				}
				coeffs.put(e.getKey(), ll);
			}
			constant *= -1;
		}

		// substract 1 from constant to convert from < to <=
		if (constraint.assign == JCTree.LT || constraint.assign == JCTree.GT) {
			constant -= 1;
		}

		// add only valid constraints since invalid ones can't be used anyway
		DomainConstraint constr = new DomainConstraint();
		constr.coeffs = coeffs;
		constr.constant = constant;
		constr.eq = (constraint.assign == JCTree.EQ);
		return constr;

	}

	private DomainConstraint convertParentConstraint(DomainConstraint parentConstr,
			Map<VarSymbol, VarSymbol> varmap) {

		// two different parent variables can be mapped to the same base variable
		Map<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>> coeffs = new LinkedHashMap<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>>();
		for (VarSymbol parentVar : parentConstr.coeffs.keySet()) {
			VarSymbol childVar = varmap.get(parentVar);
			java.util.List<Pair<Integer,VarSymbol>> ll=new java.util.LinkedList<Pair<Integer,VarSymbol>>();
			for(Pair<Integer,VarSymbol> p:parentConstr.coeffs.get(parentVar))
			{
				ll.add(p);
				if(coeffs.get(childVar)!=null)
				{
					for(Pair<Integer,VarSymbol> cp:coeffs.get(childVar))
						ll.add(new Pair<Integer,VarSymbol>(cp.fst,cp.snd));
				}
			}
			//if(!ll.isEmpty())
			coeffs.put(childVar, ll);
		}

		DomainConstraint constr = new DomainConstraint();
		constr.coeffs = coeffs;
		constr.constant = parentConstr.constant;
		constr.eq = parentConstr.eq;
		return constr;

	}

	Type makeLinear(Type t)
	{
		Type c=(Type)t.clone();
		c.original=c;
		c.type_flags_field|=LINEAR;
		if(c.tag==CLASS&&((ClassType)c).supertype_field!=null)
			((ClassType)c).supertype_field=makeLinear(((ClassType)c).supertype_field);
		return c;
	}

	/**
	 * Complete entering a class.
	 *
	 * @param sym The symbol of the class to be completed.
	 */
	public void complete(Symbol sym) throws CompletionFailure {
		// Suppress some (recursive) MemberEnter invocations
		if (!completionEnabled) {
			// Re-install same completer for next time around and return.
			assert (sym.flags() & Flags.COMPOUND) == 0;
			sym.completer = this;
			return;
		}

		if (sym instanceof DomainSymbol) {
			complete((DomainSymbol) sym);
			return;
		}

		ClassSymbol c = (ClassSymbol) sym;
		ClassType ct = (ClassType) c.type;
		Env<AttrContext> env = enter.typeEnvs.get(c);
		JCClassDecl tree = (JCClassDecl) env.tree;
		boolean wasFirst = isFirst;
		isFirst = false;

		JavaFileObject prev = log.useSource(env.toplevel.sourcefile);
		try {
			// Save class environment for later member enter (2) processing.
			halfcompleted.append(env);

			((ClassType)c.type).tree = (JCClassDecl)env.tree;


			// Mark class as not yet attributed.
			c.flags_field |= UNATTRIBUTED;

			// If this is a toplevel-class, make sure any preceding import
			// clauses have been seen.
			if (c.owner.kind == PCK) {
				memberEnter(env.toplevel, env.enclosing(JCTree.TOPLEVEL));
				todo.append(env);
			}

			if (c.owner.kind == TYP) {
				c.owner.complete();
			}

			// create an environment for evaluating the base clauses
			Env<AttrContext> baseEnv = baseEnv(tree, env);

			if((ct.type_flags_field&Flags.LINEAR)!=0)
				attr.force_linear=true;
			// Determine supertype.
			Type supertype =
					(tree.extending != null&&tree.typarams.size()==0)
					? attr.attribBase(tree.extending, baseEnv, true, false, true)
					: ((tree.mods.flags & Flags.ENUM) != 0 && !target.compilerBootstrap(c))
					? attr.attribBase(enumBase(tree.pos, c), baseEnv,
					true, false, false)
					: (c.fullname == names.java_lang_Object || c.fullname == names.Object)
					? Type.noType
					: syms.objectType;

			if((ct.type_flags_field&Flags.LINEAR)!=0)
			{
				attr.force_linear=false;
				supertype=makeLinear(supertype);
			}

			ct.supertype_field = supertype;


			if(ct.original!=ct)
				((ClassType)ct.original).supertype_field = supertype;

			// Determine interfaces.
			ListBuffer<Type> interfaces = new ListBuffer<Type>();
			Set<Type> interfaceSet = new LinkedHashSet<Type>();
			List<JCExpression> interfaceTrees = tree.implementing;
			if ((tree.mods.flags & Flags.ENUM) != 0 && target.compilerBootstrap(c)) {
				// add interface Comparable<T>
				/*
				 interfaceTrees =
				 interfaceTrees.prepend(make.Type(new ClassType(syms.comparableType.getEnclosingType(),
				 List.of(c.type),
				 syms.comparableType.tsym)));
				 // add interface Serializable
				 interfaceTrees =
				 interfaceTrees.prepend(make.Type(syms.serializableType));

				 */
			}
			for (JCExpression iface : interfaceTrees) {
				Type i = attr.attribBase(iface, baseEnv, false, true, true);
				if (i.tag == CLASS) {
					interfaces.append(i);
					chk.checkNotRepeated(iface.pos(), types.erasure(i), interfaceSet);
				}
			}
			if ((c.flags_field & ANNOTATION) != 0) {
				ct.interfaces_field = List.of(syms.annotationType);
			} else {
				ct.interfaces_field = interfaces.toList();
			}

			if (c.fullname == names.java_lang_Object || c.fullname == names.Object) {
				if (tree.extending != null) {
					chk.checkNonCyclic(tree.extending.pos(),
							supertype);
					ct.supertype_field = Type.noType;
				} else if (tree.implementing.nonEmpty()) {
					chk.checkNonCyclic(tree.implementing.head.pos(),
							ct.interfaces_field.head);
					ct.interfaces_field = List.nil();
				}
			}

			// Annotations.
			// In general, we cannot fully process annotations yet,  but we
			// can attribute the annotation types and then check to see if the
			// @Deprecated annotation is present.
			attr.attribAnnotationTypes(tree.mods.annotations, baseEnv);
			if (hasDeprecatedAnnotation(tree.mods.annotations)) {
				c.flags_field |= DEPRECATED;
			}
			annotateLater(tree.mods.annotations, baseEnv, c);

			chk.checkNonCyclic(tree.pos(), c.type);

			attr.attribTypeVariables(tree.typarams, baseEnv);

			// Add default constructor if needed.
			if ((c.flags() & INTERFACE) == 0
					&& !TreeInfo.hasConstructors(tree.defs)) {
				List<Type> argtypes = List.nil();
				List<Type> typarams = List.nil();
				List<Type> thrown = List.nil();
				long ctorFlags = 0;
				boolean based = false;
				if (c.name.isEmpty()) {
					JCNewClass nc = (JCNewClass) env.next.tree;
					if (nc.constructor != null) {
						Type superConstrType = types.memberType(c.type,
								nc.constructor);
						argtypes = superConstrType.getParameterTypes();
						typarams = superConstrType.getTypeArguments();
						ctorFlags = nc.constructor.flags() & VARARGS;
						if (nc.encl != null) {
							argtypes = argtypes.prepend(nc.encl.type);
							based = true;
						}
						thrown = superConstrType.getThrownTypes();
					}
				}
				JCTree constrDef = DefaultConstructor(make.at(tree.pos), c,
						typarams, argtypes, thrown,
						ctorFlags, based);
				tree.defs = tree.defs.prepend(constrDef);
			}

			// If this is a class, enter symbols for this and super into
			// current scope.
			if ((c.flags_field & INTERFACE) == 0) {
				Type t = (Type) c.type;//.addFlag(Flags.FOUT);

				VarSymbol thisSym =
						new VarSymbol(FINAL | HASINIT, names._this, t, c);

				if ((thisSym.type.type_flags_field & Flags.LINEAR) == 0 && (thisSym.owner instanceof ClassSymbol || (thisSym.flags_field & Flags.FOUT) == 0)) {
					thisSym.type = thisSym.type.addFlag(Flags.FINAL);
				}

				c.thisSym = thisSym;



//				if((c.flags_field&Flags.LINEAR)!=0)
//					thisSym.type.type_flags_field|=Flags.LINEAR;

				thisSym.pos = Position.FIRSTPOS;
				env.info.scope.enter(thisSym);
				if (ct.supertype_field.tag == CLASS) {
					VarSymbol superSym =
							new VarSymbol(FINAL | HASINIT, names._super,
							ct.supertype_field, c);
					superSym.pos = Position.FIRSTPOS;
					env.info.scope.enter(superSym);
				}
			}

			// check that no package exists with same fully qualified name,
			// but admit classes in the unnamed package which have the same
			// name as a top-level package.
			if (checkClash
					&& c.owner.kind == PCK && c.owner != syms.unnamedPackage
					&& reader.packageExists(c.fullname)) {
				log.error(tree.pos, "clash.with.pkg.of.same.name", c);
			}

		} catch (CompletionFailure ex) {
			chk.completionError(tree.pos(), ex);
		} finally {
			log.useSource(prev);
		}


		// Enter all member fields and methods of a set of half completed
		// classes in a second phase.
		if (wasFirst||sym.must_finish) {
			try {

				while (halfcompleted.nonEmpty()) {
					finish(halfcompleted.next());
				}

				halfcompleted.clear();
			} finally {
				isFirst = true;
			}

			// commit pending annotations
			annotate.flush();
		}
	}

	private Env<AttrContext> baseEnv(JCClassDecl tree, Env<AttrContext> env) {
		Scope typaramScope = new Scope(tree.sym);
		if (tree.typarams != null) {
			for (List<JCTypeParameter> typarams = tree.typarams;
					typarams.nonEmpty();
					typarams = typarams.tail) {
				typaramScope.enter(typarams.head.type.tsym);
			}
		}
		Env<AttrContext> outer = env.outer; // the base clause can't see members of this class
		Env<AttrContext> localEnv = outer.dup(tree, outer.info.dup(typaramScope));
		localEnv.baseClause = true;
		localEnv.outer = outer;
		localEnv.info.isSelfCall = false;
		return localEnv;
	}

	private Env<AttrContext> baseEnv(JCDomainDecl tree, Env<AttrContext> env) {
		Scope typaramScope = new Scope(tree.sym);
		/*
		 if (tree.typarams != null)
		 for (List<JCTypeParameter> typarams = tree.typarams;
		 typarams.nonEmpty();
		 typarams = typarams.tail)
		 typaramScope.enter(typarams.head.type.tsym);
		 */
		Env<AttrContext> outer = env.outer; // the base clause can't see members of this class
		Env<AttrContext> localEnv = outer.dup(tree, outer.info.dup(typaramScope));
		localEnv.baseClause = true;
		localEnv.outer = outer;
		localEnv.info.isSelfCall = false;
		return localEnv;
	}

	/**
	 * Enter member fields and methods of a class
	 *
	 * @param env the environment current for the class block.
	 */
	public void finish(Env<AttrContext> env) {
		JavaFileObject prev = log.useSource(env.toplevel.sourcefile);
		try {
			if (env.tree instanceof JCClassDecl) {
				JCClassDecl tree = (JCClassDecl) env.tree;
				finishClass(tree, env);
			} else if (env.tree instanceof JCDomainDecl) {
				JCDomainDecl tree = (JCDomainDecl) env.tree;
				finishDomain(tree, env);
			}
		} finally {
			log.useSource(prev);
		}
	}

	/**
	 * Generate a base clause for an enum type.
	 *
	 * @param pos The position for trees and diagnostics, if any
	 * @param c The class symbol of the enum
	 */
	private JCExpression enumBase(int pos, ClassSymbol c) {
		JCExpression result = make.at(pos).
				TypeApply(make.QualIdent(syms.enumSym),
				List.<JCExpression>of(make.Type(c.type)));
		return result;
	}

	/**
	 * Generate a base clause for an enum type.
	 *
	 * @param pos The position for trees and diagnostics, if any
	 * @param c The class symbol of the enum
	 */
	private JCExpression enumBase(int pos, DomainSymbol c) {
		JCExpression result = make.at(pos).
				TypeApply(make.QualIdent(syms.enumSym),
				List.<JCExpression>of(make.Type(c.type)));
		return result;
	}

	/* ***************************************************************************
	 * tree building
	 ****************************************************************************/
	/**
	 * Generate default constructor for given class. For classes different from java.lang.Object,
	 * this is:
	 *
	 * c(argtype_0 x_0, ..., argtype_n x_n) throws thrown { super(x_0, ..., x_n) }
	 *
	 * or, if based == true:
	 *
	 * c(argtype_0 x_0, ..., argtype_n x_n) throws thrown { x_0.super(x_1, ..., x_n) }
	 *
	 * @param make The tree factory.
	 * @param c The class owning the default constructor.
	 * @param argtypes The parameter types of the constructor.
	 * @param thrown The thrown exceptions of the constructor.
	 * @param based Is first parameter a this$n?
	 */
	JCTree DefaultConstructor(TreeMaker make,
			ClassSymbol c,
			List<Type> typarams,
			List<Type> argtypes,
			List<Type> thrown,
			long flags,
			boolean based) {
		List<JCVariableDecl> params = make.Params(argtypes, syms.noSymbol);
		List<JCStatement> stats = List.nil();
		JCStatement super_call = null;
		if (c.type != syms.objectType) {
			super_call = SuperCall(make, typarams, params, based, ((ClassType) c.type).getSuper(), null);
		}
		if ((c.flags() & ENUM) != 0
				&& (types.supertype(c.type).tsym == syms.enumSym
				|| target.compilerBootstrap(c))) {
			// constructors of true enums are private
			flags = (flags & ~AccessFlags) | PRIVATE | GENERATEDCONSTR;
		} else {
			flags |= (c.flags() & AccessFlags) | GENERATEDCONSTR;
		}
		if (c.name.isEmpty()) {
			flags |= ANONCONSTR;
		}
		JCMethodDecl result = make.MethodDef(
				make.Modifiers(flags),
				names.init,
				null,
				make.TypeParams(typarams),
				params,
				make.Types(thrown),
				make.Block(0, stats),
				null);
		result.super_call = super_call;
		return result;
	}

	/**
	 * Generate call to superclass constructor. This is:
	 *
	 * super(id_0, ..., id_n)
	 *
	 * or, if based == true
	 *
	 * id_0.super(id_1,...,id_n)
	 *
	 * where id_0, ..., id_n are the names of the given parameters.
	 *
	 * @param make The tree factory
	 * @param params The parameters that need to be passed to super
	 * @param typarams The type parameters that need to be passed to super
	 * @param based Is first parameter a this$n?
	 */
	JCExpressionStatement SuperCall(TreeMaker make,
			List<Type> typarams,
			List<JCVariableDecl> params,
			boolean based, Type s, List<JCExpression> values) {

		if (s.tsym.name.equals(names.fromString("Object"))) {
			return null;
		}

		JCExpression meth;
		if (based) {
			meth = make.Select(make.Ident(params.head), make.Ident(s.tsym));
			params = params.tail;
		} else {
			meth = make.Ident(s.tsym);
		}
		List<JCExpression> typeargs = typarams.nonEmpty() ? make.Types(typarams) : null;
		if (values == null) {
			return make.Exec(make.Apply(typeargs, meth, make.Idents(params)));
		} else {
			return make.Exec(make.Apply(typeargs, meth, values));
		}
	}
}
