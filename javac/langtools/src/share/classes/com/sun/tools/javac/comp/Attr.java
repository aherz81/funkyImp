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
 * version 2 for more details (a copy is included in the LICENSE filve that
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

import com.sun.source.tree.*;
import java.util.*;
import java.util.Set;
//import javax.lang.model.element.ElementKind;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.code.Type.*;

import com.sun.source.util.SimpleTreeVisitor;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import com.sun.tools.javac.code.Scope;
import static com.sun.tools.javac.code.TypeTags.*;
import com.sun.tools.javac.main.JavaCompiler;
//import com.sun.tools.javac.main.JavaCompiler;

/**
 * This is the main context-dependent analysis phase in GJC. It encompasses name
 * resolution, type checking and constant folding as subtasks. Some subtasks
 * involve auxiliary classes.
 *
 * @see Check
 * @see Resolve
 * @see ConstFold
 * @see Infer
 *
 * <p><b>This is NOT part of any API supported by Sun Microsystems. If you write
 * code that depends on this, you do so at your own risk. This code and its
 * internal interfaces are subject to change or deletion without notice.</b>
 */
public class Attr extends JCTree.Visitor {

    protected static final Context.Key<Attr> attrKey =
            new Context.Key<Attr>();
    final Names names;
    final Log log;
    final Symtab syms;
    final Resolve rs;
    final Check chk;
    final MemberEnter memberEnter;
    final TreeMaker make;
    final TreeCopier copy;
    final ConstFold cfolder;
    final Enter enter;
    final Target target;
    final Types types;
    final JCDiagnostic.Factory diags;
	final JavaCompiler jc;
    final Annotate annotate;
    boolean inside_select = false;
    boolean allowModify = false;
    boolean returnUnique = false;
    boolean insideLinear = false;
    boolean insideProjection = false;
	int insideDomainParams=-1;
	JCExpression domParamTree=null;
	VarSymbol insideVarDecl=null;

	//Map<VarSymbol,Set<JCExpression>> constraintsDeps=null;

    Map<VarSymbol, Set<List<JCTree.JCExpression>>> alive_vars;
    public java.util.Set<Env<AttrContext>> templatesInstances = new java.util.LinkedHashSet<Env<AttrContext>>();

	JCFieldAccess constructExp=null;
	Map<VarSymbol, JCExpression> lastProjectionArgs=null;

    public static Attr instance(Context context) {
        Attr instance = context.get(attrKey);
        if (instance == null) {
            instance = new Attr(context);
        }
        return instance;
    }

    protected Attr(Context context) {
        context.put(attrKey, this);

        names = Names.instance(context);
        log = Log.instance(context);
        syms = Symtab.instance(context);
        rs = Resolve.instance(context);
        chk = Check.instance(context);
        memberEnter = MemberEnter.instance(context);
        make = TreeMaker.instance(context);
        copy = new TreeCopier(make);
        enter = Enter.instance(context);
        cfolder = ConstFold.instance(context);
        target = Target.instance(context);
        types = Types.instance(context);
        diags = JCDiagnostic.Factory.instance(context);
        annotate = Annotate.instance(context);
		jc = JavaCompiler.instance(context);
        Options options = Options.instance(context);

        Source source = Source.instance(context);
        allowGenerics = source.allowGenerics();
        allowVarargs = source.allowVarargs();
        allowEnums = source.allowEnums();
        allowBoxing = source.allowBoxing();
        allowCovariantReturns = source.allowCovariantReturns();
        allowAnonOuterThis = source.allowAnonOuterThis();
        relax = (options.get("-retrofit") != null
                || options.get("-relax") != null);
        useBeforeDeclarationWarning = options.get("useBeforeDeclarationWarning") != null;
    }
    /**
     * Switch: relax some constraints for retrofit mode.
     */
    boolean relax;
    boolean insideAccess = false;
    /**
     * Switch: support generics?
     */
    boolean allowGenerics;
    /**
     * Switch: allow variable-arity methods.
     */
    boolean allowVarargs;
    /**
     * Switch: support enums?
     */
    boolean allowEnums;
    /**
     * Switch: support boxing and unboxing?
     */
    boolean allowBoxing;
    /**
     * Switch: support covariant result types?
     */
    boolean allowCovariantReturns;
    /**
     * Switch: allow references to surrounding object from anonymous objects
     * during constructor call?
     */
    boolean allowAnonOuterThis;
    boolean inside_branch = false;
	JCExpression inside_cond=null;
    JCDomainIter domiter = null;
	Type domIterType = null;
	boolean insideMethodAttrib=false;
    /**
     * Switch: warn about use of variable before declaration? RFE: 6425594
     */
    boolean useBeforeDeclarationWarning;

    /**
     * Check kind and type of given tree against protokind and prototype. If
     * check succeeds, store type in tree and return it. If check fails, store
     * errType in tree and return it. No checks are performed if the prototype
     * is a method type. It is not necessary in this case since we know that
     * kind and type are correct.
     *
     * @param tree The tree whose kind and type is checked
     * @param owntype The computed type of the tree
     * @param ownkind The computed kind of the tree
     * @param pkind The expected kind (or: protokind) of the tree
     * @param pt The expected type (or: prototype) of the tree
     */
    Type check(JCTree tree, Type owntype, int ownkind, int pkind, Type pt) {
        if (owntype.tag != ERROR && pt.tag != METHOD && pt.tag != FORALL) {
            if ((ownkind & ~pkind) == 0 || (ownkind == MTH && pkind == VAL)) {
                owntype = chk.checkType(tree.pos(), owntype, pt);
            } else {
				if(pt.tag!=TypeTags.NONE)
				{
					log.error(tree.pos(), "unexpected.type",
							kindNames(pkind),
							kindName(ownkind));
					owntype = types.createErrorType(owntype);
				}
				else
				{
					int i=0;
				}
            }
        }
        tree.type = owntype;
        return owntype;
    }

    /**
     * Is given blank final variable assignable, i.e. in a scope where it may be
     * assigned to even though it is final?
     *
     * @param v The blank final variable.
     * @param env The current environment.
     */
    boolean isAssignableAsBlankFinal(VarSymbol v, Env<AttrContext> env) {
        Symbol owner = env.info.scope.owner;
        // owner refers to the innermost variable, method or
        // initializer block declaration at this point.
        return v.owner == owner
                || ((owner.name == names.init
                //((v.flags() & HASINIT) == 0)
                || // i.e. we are in a constructor
                owner.kind == VAR || // i.e. we are in a variable initializer
                (owner.flags() & BLOCK) != 0) // i.e. we are in an initializer block
                && v.owner == owner.owner
                && ((v.flags() & STATIC) != 0) == Resolve.isStatic(env));
    }

    /**
     * Check that variable can be assigned to.
     *
     * @param pos The current source code position.
     * @param v The assigned varaible
     * @param base If the variable is referred to in a Select, the part to the
     * left of the `.', null otherwise.
     * @param env The current environment.
     */
    void checkAssignable(DiagnosticPosition pos, VarSymbol v, JCTree base, Env<AttrContext> env) {
        if ((!v.type.equals(syms.threadType) && !v.type.equals(syms.groupType))
                && ((env.info.scope.owner.flags() & WHEREBLOCK) == 0)
                && (v.flags() & FINAL) != 0
                && ((v.flags() & HASINIT) != 0
                || !((base == null
                || (base.getTag() == JCTree.IDENT && TreeInfo.name(base) == names._this))
                && isAssignableAsBlankFinal(v, env)))) {
            log.error(pos, "cant.assign.val.to.final.var", v);
        }
    }

    /**
     * Does tree represent a static reference to an identifier? It is assumed
     * that tree is either a SELECT or an IDENT. We have to weed out selects
     * from non-type names here.
     *
     * @param tree The candidate tree.
     */
    boolean isStaticReference(JCTree tree) {
        if (tree.getTag() == JCTree.SELECT) {
            Symbol lsym = TreeInfo.symbol(((JCFieldAccess) tree).selected);
            if (lsym == null || lsym.kind != TYP) {
                return false;
            }
        }
        return true;
    }

    /**
     * Is this symbol a type?
     */
    static boolean isType(Symbol sym) {
        return sym != null && (sym.kind & (DOM | TYP)) != 0;
    }

    /**
     * The current `this' symbol.
     *
     * @param env The current environment.
     */
    Symbol thisSym(DiagnosticPosition pos, Env<AttrContext> env) {
        return rs.resolveSelf(pos, env, env.enclClass.sym, names._this);
    }

    /**
     * Attribute a parsed identifier.
     *
     * @param tree Parsed identifier name
     * @param topLevel The toplevel to use
     */
    public Symbol attribIdent(JCTree tree, JCCompilationUnit topLevel) {

        if (((JCIdent) tree).name.toString().contains("__")) {
            log.error(tree, "double.underline.reserved");
        }

        Env<AttrContext> localEnv = enter.topLevelEnv(topLevel);
        localEnv.enclClass = make.ClassDef(make.Modifiers(0),
                syms.errSymbol.name,
                null, null, null, null);
        localEnv.enclClass.sym = syms.errSymbol;
        return tree.accept(identAttributer, localEnv);
    }
    // where
    private TreeVisitor<Symbol, Env<AttrContext>> identAttributer = new IdentAttributer();

    private class IdentAttributer extends SimpleTreeVisitor<Symbol, Env<AttrContext>> {

        @Override
        public Symbol visitMemberSelect(MemberSelectTree node, Env<AttrContext> env) {
            Symbol site = visit(node.getExpression(), env);
            if (site.kind == ERR) {
                return site;
            }
            Name name = (Name) node.getIdentifier();
            if (site.kind == PCK) {
                env.toplevel.packge = (PackageSymbol) site;
                return rs.findIdentInPackage(env, (TypeSymbol) site, name, TYP | PCK);
            } else {
                env.enclClass.sym = (ClassSymbol) site;
                return rs.findMemberType(env, site.asType(), name, (TypeSymbol) site);
            }
        }

        @Override
        public Symbol visitIdentifier(IdentifierTree node, Env<AttrContext> env) {
            return rs.findIdent(env, (Name) node.getName(), TYP | PCK);
        }
    }

    public Type coerce(Type etype, Type ttype) {
        return cfolder.coerce(etype, ttype);
    }

    public Type attribType(JCTree node, TypeSymbol sym) {
        Env<AttrContext> env = enter.typeEnvs.get(sym);
        Env<AttrContext> localEnv = env.dup(node, env.info.dup());
        return attribTree(node, localEnv, Kinds.TYP, Type.noType);
    }

    public Env<AttrContext> attribExprToTree(JCTree expr, Env<AttrContext> env, JCTree tree) {
        breakTree = tree;
        JavaFileObject prev = log.useSource(null);
        try {
            attribExpr(expr, env);
        } catch (BreakAttr b) {
            return b.env;
        } finally {
            breakTree = null;
            log.useSource(prev);
        }
        return env;
    }

    public Env<AttrContext> attribStatToTree(JCTree stmt, Env<AttrContext> env, JCTree tree) {
        breakTree = tree;
        JavaFileObject prev = log.useSource(null);
        try {
            attribStat(stmt, env);
        } catch (BreakAttr b) {
            return b.env;
        } finally {
            breakTree = null;
            log.useSource(prev);
        }
        return env;
    }
    private JCTree breakTree = null;

    private static class BreakAttr extends RuntimeException {

        static final long serialVersionUID = -6924771130405446405L;
        private Env<AttrContext> env;

        private BreakAttr(Env<AttrContext> env) {
            this.env = env;
        }
    }
    /* ************************************************************************
     * Visitor methods
     *************************************************************************/
    /**
     * Visitor argument: the current environment.
     */
    Env<AttrContext> env;
    /**
     * Visitor argument: the currently expected proto-kind.
     */
    int pkind;
    /**
     * Visitor argument: the currently expected proto-type.
     */
    Type pt;
    /**
     * Visitor argument: true if an expression can have an arraytype of a
     * non-base domain.
     */
    boolean allowNonBaseDomains;
    /**
     * Visitor result: the computed type.
     */
    Type result;
    boolean statement_removed = false;
    boolean method_argument = false;

    /**
     * Visitor method: attribute a tree, catching any completion failure
     * exceptions. Return the tree's type.
     *
     * @param tree The tree to be visited.
     * @param env The environment visitor argument.
     * @param pkind The protokind visitor argument.
     * @param pt The prototype visitor argument.
     */
    Type attribTree(JCTree tree, Env<AttrContext> env, int pkind, Type pt, boolean allowNonBaseDomains) {
        Env<AttrContext> prevEnv = this.env;
        int prevPkind = this.pkind;
        Type prevPt = this.pt;
        boolean prevAllowNonBaseDomains = this.allowNonBaseDomains;
        try {
            this.env = env;
            this.pkind = pkind;
            this.pt = pt;
            this.allowNonBaseDomains = allowNonBaseDomains;
            tree.accept(this);
            if (tree == breakTree) {
                throw new BreakAttr(env);
            }
            return result;
        } catch (CompletionFailure ex) {
            tree.type = syms.errType;
            return chk.completionError(tree.pos(), ex);
        } finally {
            this.env = prevEnv;
            this.pkind = prevPkind;
            this.pt = prevPt;
            this.allowNonBaseDomains = prevAllowNonBaseDomains;
        }
    }

    /**
     * Visitor method: attribute a tree, catching any completion failure
     * exceptions. Return the tree's type.
     *
     * @param tree The tree to be visited.
     * @param env The environment visitor argument.
     * @param pkind The protokind visitor argument.
     * @param pt The prototype visitor argument.
     */
    Type attribTree(JCTree tree, Env<AttrContext> env, int pkind, Type pt) {
        return attribTree(tree, env, pkind, pt, false);
    }

    /**
     * Derived visitor method: attribute an expression tree.
     */
    public Type attribExpr(JCTree tree, Env<AttrContext> env, Type pt) {
        return attribTree(tree, env, VAL | TYP, pt.tag != ERROR ? pt : Type.noType);
    }

    /**
     * Derived visitor method: attribute an expression tree with no constraints
     * on the computed type.
     */
    Type attribExpr(JCTree tree, Env<AttrContext> env) {
        return attribTree(tree, env, VAL | TYP, Type.noType);
    }

    Type attribType(JCTree tree, Env<AttrContext> env, long flags) {
        insideLinear = (flags & Flags.LINEAR) != 0;
        Type result = attribTree(tree, env, TYP, Type.noType);

        if (result != null && !result.isPrimitive() && (result.type_flags_field & Flags.LINEAR) == 0 && !insideLinear) {
            result = result.addFlag(Flags.FINAL);
        }
        insideLinear = false;
        return result;
    }

    /**
     * Derived visitor method: attribute a type tree.
     */
    Type attribType(JCTree tree, Env<AttrContext> env) {
        Type result = attribTree(tree, env, TYP, Type.noType);

        if (result != null && !result.isPrimitive() && (result.type_flags_field & Flags.LINEAR) == 0 && !insideLinear) {
            result = result.addFlag(Flags.FINAL);
        }

        return result;
    }

    /**
     * Derived visitor method: attribute a type tree.
     */
    Type attribArgType(JCTree tree, Env<AttrContext> env) {
        Type result = attribTree(tree, env, TYP | VAL, Type.noType);

        if (!result.isPrimitive() && (result.type_flags_field & Flags.LINEAR) == 0) {
            result = result.addFlag(Flags.FINAL);
        }

        return result;
    }

    /**
     * Derived visitor method: attribute a type tree.
     */
    Type attribTypeVar(JCTree tree, Env<AttrContext> env) {
        Type result = attribTree(tree, env, TYP | DOM, Type.noType);
        return result;
    }

    /**
     * Derived visitor method: attribute a statement or definition tree.
     */
    public Type attribStat(JCTree tree, Env<AttrContext> env) {
		Map<VarSymbol, JCExpression> oldArgs=lastProjectionArgs;
        Type res=attribTree(tree, env, NIL, Type.noType);
		lastProjectionArgs=oldArgs;
		return res;
    }

    /**
     * Attribute a list of expressions, returning a list of types.
     */
    List<Type> attribExprs(List<JCExpression> trees, Env<AttrContext> env, Type pt) {
        ListBuffer<Type> ts = new ListBuffer<Type>();
        for (List<JCExpression> l = trees; l.nonEmpty(); l = l.tail) {
            ts.append(attribExpr(l.head, env, pt));
        }
        return ts.toList();
    }

    /**
     * Attribute a list of statements, returning nothing.
     */
    <T extends JCTree> void attribStats(List<T> trees, Env<AttrContext> env) {
        for (List<T> l = trees; l.nonEmpty(); l = l.tail) {
            statement_removed = false;
            attribStat(l.head, env);
            if (statement_removed) {
                l.head = (T) make.Skip();
            }
        }
    }

    /**
     * Attribute the arguments in a method call, returning a list of types.
     */
    List<Type> attribArgs(List<JCExpression> trees, Env<AttrContext> env) {
        ListBuffer<Type> argtypes = new ListBuffer<Type>();
        for (List<JCExpression> l = trees; l.nonEmpty(); l = l.tail) {
            argtypes.append(chk.checkNonVoid(
                    l.head.pos(), types.upperBound(attribTree(l.head, env, VAL, Infer.anyPoly))));
        }
        return argtypes.toList();
    }

    /**
     * Attribute the arguments in a method call, returning a list of types.
     *
     *
     */
    List<Type> attribArgs(List<JCExpression> trees, Env<AttrContext> env, List<VarSymbol> args) {
        ListBuffer<Type> argtypes = new ListBuffer<Type>();
        for (List<JCExpression> l = trees; l.nonEmpty(); l = l.tail) {
            int kind;
            if ((args.head.flags() & FOUT) != 0) {
                kind = VAR;
            } else {
                kind = VAL;
            }

            argtypes.append(chk.checkNonVoid(
                    l.head.pos(), types.upperBound(attribTree(l.head, env, kind, Infer.anyPoly))));
            if (!args.tail.isEmpty())//var args may have less input
            {
                args = args.tail;
            }
        }
        return argtypes.toList();
    }

    /**
     * Attribute a type argument list, returning a list of types.
     */
    List<Type> attribTypes(List<JCExpression> trees, Env<AttrContext> env) {
        ListBuffer<Type> argtypes = new ListBuffer<Type>();
        for (List<JCExpression> l = trees; l.nonEmpty(); l = l.tail) {
            argtypes.append(chk.checkNonVoid(l.head.pos(), attribType(l.head, env)));
        }
        return argtypes.toList();
    }

    List<Type> attribArgTypes(List<JCExpression> trees, Env<AttrContext> env) {
        ListBuffer<Type> argtypes = new ListBuffer<Type>();
        for (List<JCExpression> l = trees; l.nonEmpty(); l = l.tail) {
            argtypes.append(chk.checkNonVoid(l.head.pos(), attribArgType(l.head, env)));
        }
        return argtypes.toList();
    }

    /**
     * Attribute type variables (of generic classes or methods). Compound types
     * are attributed later in attribBounds.
     *
     * @param typarams the type variables to enter
     * @param env the current environment
     */
    void attribTypeVariables(List<JCTypeParameter> typarams, Env<AttrContext> env) {
        for (JCTypeParameter tvar : typarams) {
            TypeVar a = (TypeVar) tvar.type;
            a.tsym.flags_field |= UNATTRIBUTED;
            a.bound = Type.noType;
            if (!tvar.bounds.isEmpty()) {
                List<Type> bounds = List.of(attribTypeVar(tvar.bounds.head, env));
                for (JCExpression bound : tvar.bounds.tail) {
                    bounds = bounds.prepend(attribType(bound, env));
                }
                types.setBounds(a, bounds.reverse());
            } else {
                // if no bounds are given, assume a single bound of
                // java.lang.Object.
                types.setBounds(a, List.of(syms.objectType));
            }
            a.tsym.flags_field &= ~UNATTRIBUTED;
        }
        for (JCTypeParameter tvar : typarams) {
            chk.checkNonCyclic(tvar.pos(), (TypeVar) tvar.type);
        }
        attribStats(typarams, env);
    }

    void attribBounds(List<JCTypeParameter> typarams) {
        for (JCTypeParameter typaram : typarams) {
            Type bound = typaram.type.getUpperBound();
            if (bound != null && bound.tsym instanceof ClassSymbol) {
                ClassSymbol c = (ClassSymbol) bound.tsym;
                if ((c.flags_field & COMPOUND) != 0) {
                    assert (c.flags_field & UNATTRIBUTED) != 0 : c;
                    attribClass(typaram.pos(), c);
                }
            }
        }
    }

    /**
     * Attribute the type references in a list of annotations.
     */
    void attribAnnotationTypes(List<JCAnnotation> annotations,
            Env<AttrContext> env) {
        for (List<JCAnnotation> al = annotations; al.nonEmpty(); al = al.tail) {
            JCAnnotation a = al.head;
            attribType(a.annotationType, env);
        }
    }

    /**
     * Attribute type reference in an `extends' or `implements' clause.
     *
     * @param tree The tree making up the type reference.
     * @param env The environment current at the reference.
     * @param classExpected true if only a class is expected here.
     * @param interfaceExpected true if only an interface is expected here.
     */
    Type attribBase(JCTree tree,
            Env<AttrContext> env,
            boolean classExpected,
            boolean interfaceExpected,
            boolean checkExtensible) {
        Type t = attribType(tree, env);
        return checkBase(t, tree, env, classExpected, interfaceExpected, checkExtensible);
    }

    Type attribBaseDom(JCTree tree, Env<AttrContext> env) {
        // super- and result domains
        Type t = attribType(tree, env);
        return t;
    }

    Type checkBase(Type t,
            JCTree tree,
            Env<AttrContext> env,
            boolean classExpected,
            boolean interfaceExpected,
            boolean checkExtensible) {
        if (t.tag == TYPEVAR && !classExpected && !interfaceExpected) {
            // check that type variable is already visible
            if (t.getUpperBound() == null) {
                log.error(tree.pos(), "illegal.forward.ref");
                return types.createErrorType(t);
            }
        } else {
            t = chk.checkClassType(tree.pos(), t, checkExtensible | !allowGenerics);
        }
        if (interfaceExpected && (t.tsym.flags() & INTERFACE) == 0) {
            log.error(tree.pos(), "intf.expected.here");
            // return errType is necessary since otherwise there might
            // be undetected cycles which cause attribution to loop
            return types.createErrorType(t);
        } else if (checkExtensible
                && classExpected
                && (t.tsym.flags() & INTERFACE) != 0) {
            log.error(tree.pos(), "no.intf.expected.here");
            return types.createErrorType(t);
        }
        if (checkExtensible
                && ((t.tsym.flags() & FINAL) != 0)) {
            log.error(tree.pos(),
                    "cant.inherit.from.final", t.tsym);
        }
        chk.checkNonCyclic(tree.pos(), t);
        return t;
    }

    //local classdef!
    public void visitClassDef(JCClassDecl tree) {
        // Local classes have not been entered yet, so we need to do it now:
        if ((env.info.scope.owner.kind & (VAR | MTH)) != 0) {
            enter.classEnter(tree, env);
        }

        ClassSymbol c = tree.sym;
        if (c == null) {
            // exit in case something drastic went wrong during enter.
            result = null;
        } else {
            // make sure class has been completed:
            c.complete();

            // If this class appears as an anonymous class
            // in a superclass constructor call where
            // no explicit outer instance is given,
            // disable implicit outer instance from being passed.
            // (This would be an illegal access to "this before super").
            if (env.info.isSelfCall
                    && env.tree.getTag() == JCTree.NEWCLASS
                    && ((JCNewClass) env.tree).encl == null) {
                c.flags_field |= NOOUTERTHIS;
            }
            attribClass(tree.pos(), c);
            result = tree.type = c.type;
        }
    }

    //local domains!
    public void visitDomainDecl(JCDomainDecl tree) {
        // Local classes have not been entered yet, so we need to do it now:
        if ((env.info.scope.owner.kind & (VAR | MTH)) != 0) {
            enter.classEnter(tree, env);
        }

        DomainSymbol c = tree.sym;
        if (c == null) {
            // exit in case something drastic went wrong during enter.
            result = null;
        } else {
            // make sure class has been completed:
            c.complete();

//            attribClass(tree.pos(), c);
            result = tree.type = c.type;
        }
    }

    public void visitArgExpression(JCArgExpression tree) {
        //check types

        Type t = attribTree(tree.exp1, env, VAL, pt);

        if (tree.exp2 != null) {
            attribExpr(tree.exp2, env, t);
        }

        tree.type = result = tree.exp1.type;
    }

    public void visitWhere(JCWhere tree) {

        Env<AttrContext> localEnv = memberEnter.whereEnv(tree, env);

        attribTree(tree.exp, localEnv.dup(tree), VAL, Type.noType);

        if ((tree.exp.type.type_flags_field & Flags.LINEAR) == 0) {
            log.error(tree.pos, "where.target.not.linear", tree.exp);
        }

        if (tree.sexp != null) {
            attribExpr(tree.sexp, localEnv);
        }

        if (tree.body != null) {
            attribStat(tree.body, localEnv);
        }

        localEnv.info.scope.leave();

        tree.type = tree.exp.type;

        result = tree.exp.type;

        if (pt.tsym != null) {
            Warner warn = chk.convertWarner(tree.pos(), result, pt);
            if (!types.isConvertible(result, pt, warn)) {
                chk.typeError(tree.pos(), diags.fragment("incompatible.types"), result, pt);
            }
        }
    }

    public void visitDomConstraint(JCDomConstraint tree) {
        result = Type.noType;
    }
/*    
    public JCDomainIter constructIteration(ArrayType at)
    {
        //ListBuffer<JCExpression> lb = new ListBuffer<JCExpression>();

        JCDomInstance inst = make.DomInstance(at.dom.tsym.name, at.dom.appliedParams);

        JCExpression ta = make.TypeArray(make.Type(at.elemtype), inst, at.isPointer());

        JCExpression exp = make.NewArray(ta, inst, (new ListBuffer<JCExpression>()).toList(), null);

//        JCExpression sexp = make.Indexed((JCFieldAccess) copy.copy(tree), indices.toList());
        List<VarSymbol> inds = at.dom.getIndexList(new LinkedHashMap<VarSymbol, VarSymbol>());

        //ListBuffer<JCExpression> indices = new ListBuffer<JCExpression>();
        ListBuffer<JCVariableDecl> args = new ListBuffer<JCVariableDecl>();
        for (VarSymbol vs : inds) {
            args.add(make.VarDef(vs, null));
        //    indices.add(make.Ident(vs));
        }

        JCTree conversion = make.DomIter(exp, null, args.toList(), make.Skip(), null, null);

        attribTree(conversion, env, TYP, Type.noType);           
        
        return (JCDomainIter)conversion;
    }
*/
    public void visitDomIter(JCDomainIter tree) {
        // expr.\dom(i,j) { ... i ... j ... }


        // this error type is used if an this expression cannot be typechecked
        Type owntype = types.createErrorType(tree.type);

        // Attribute the qualifier expression, and determine its type (if any).
        boolean oldAccess = insideAccess;
        insideAccess = true;
        Type t = attribTree(tree.exp, env, VAL, Infer.anyPoly);
        insideAccess = oldAccess;

        t = t.getArrayType();

        if (!types.isArray(t)) {
            log.error(tree.pos(), "array.req.but.found", t);
            tree.type = owntype;
            result = owntype;
            return;
        }

        //? env.enclClass.sym.recAddRef(t);
        //? t = capture(t);

        ArrayType site = (ArrayType) t;
        Type res;

		//if(site)

		ArrayType base=site.getStartType();
		if(base.dom.parentDomain!=null&&site.dom.parentDomain!=null)
			 log.error(tree.pos(), "projection.direct.access", site.dom);

		//String base=site.toString();

        // there are different kinds iterations:
        // 1. no iteration domain -> seletion iteration
        if (tree.name == null) {
            boolean oldInsideProjection = insideProjection;
            insideProjection = false;

            tree.sym = site.dom.tsym;

            // check number of indices
            if (tree.domargs.length() != site.dom.getInterVectorOrder(tree.pos()).size()) {
                log.error(tree.pos(), "domain.wrong.number.of.indices", site.dom);
            }

            // create a new environment for attributing the domain iteration
            Env<AttrContext> localEnv = memberEnter.domiterEnv(tree, env);

            // attribute indices, this also adds them to the environment
            for (List<JCVariableDecl> l = tree.domargs; l.nonEmpty(); l = l.tail) {
                attribStat(l.head, localEnv);
                l.head.sym.flags_field |= Flags.PARAMETER | TASKLOCAL;
            }

			Type oldType=domIterType;
			domIterType = t;
            // atribute body which can either be an expression or a statement(-list)
            JCDomainIter oldIter = domiter;
            domiter = tree;
            Type bodyType = null;
            if (tree.body != null) {
                bodyType = attribExpr(tree.body, localEnv);
            } else if (tree.sbody != null) {
                bodyType = attribStat(tree.sbody, localEnv);
            }

            if (domiter.mayReturn && oldIter != null) {
                oldIter.mayReturn = true;
            }
            domiter = oldIter;
			domIterType = oldType;

            // leave local environment since we are done with the body
            localEnv.info.scope.leave();

            // calculate result type of iteration
            if (bodyType == null || bodyType.tag == TypeTags.VOID) {
                // if body is empty (?) or has typ 'void', iteration also has type void
                res = syms.voidType;
            } else {
                // body can have any type, it is used as the element type of the result type

                Type pta = pt.getArrayType();
                if (pta.tag == TypeTags.ARRAY) {
                    bodyType = ((ArrayType) pta).elemtype;
                }

                //DomainType dt = site.dom;
                //res = new ArrayType(bodyType, dt, syms.arrayClass);
				res = (ArrayType)site.clone(); //MUST clone complete array type (rather than just the domain) or we loose the projection info
				((ArrayType) res).elemtype = bodyType;
                res.type_flags_field = site.type_flags_field;
                ((ArrayType) res).elemtype.type_flags_field = ((ArrayType) t).elemtype.type_flags_field;
            }

            tree.iterType = t;
            insideProjection = oldInsideProjection;

        } // 2. reduce
        else if (tree.name.equals(names.reduce)) {

            // use the predefined reduce symbol
            tree.sym = syms.reduce;

            // domain parameters are not allowed here
            if (tree.params == null || tree.params.length() != 1) {
                log.error(tree.pos(), "reduce.expression.with.domain.parameter");
            }

            Type space;

            if (insideProjection) {
                space = site; //reduce over domains
            } else {
                space = site.elemtype;
            }

            if (tree.params != null) {
                Type tp = attribExpr(tree.params.head, env);
				if (!types.isCastable(tp, space)) {
					if(!(tp.constValue() instanceof Integer&&((Integer)tp.constValue())==0)) //NOTE: we especially allow "0" as initial accum for any reduction
					{
						chk.typeError(tree.pos(), diags.fragment("incompatible.types"), tp, space);
					}
					else
					{
						if(domIterType!=null)
						{
							tree.params.head.type = domIterType;
							tree.emptyInit = true;
							space = domIterType;
						}
						else
							tree.params.head.type = space;
					}
				}
				else
					space = tp;
            }

            // reduce has always two indices
			int iterdims;

			if(insideProjection&&site.dom.projectionArgs!=null)
				iterdims=site.dom.projectionArgs.size();
			else
				iterdims=site.dom.getInterVectorOrder(tree.pos()).size();

            if (tree.domargs.length() != iterdims+1) { //iteration inds + accum
                log.error(tree.pos(), "wrong.number.of.indices.for.reduce",iterdims+1, tree.domargs.length());
            }

            // create a new environment for attributing the body
            Env<AttrContext> localEnv = memberEnter.domiterEnv(tree, env);

            // attribute indices, this also adds them to the environment
            for (List<JCVariableDecl> idx = tree.domargs; idx.nonEmpty(); idx = idx.tail) {
                // Indices of domain iterations are automatically integers.
                // This is not true for reduce iterations, however.
                // Therefore it has to be changed here.

                //first args are counter(int)
                //second arg is accum(elt.type)

                VarSymbol s;
                if (!idx.tail.isEmpty()) {
                    s = new VarSymbol(Flags.FINAL, idx.head.name, syms.intType, env.info.scope.owner);
                    idx.head.type = syms.intType;
                } else {
                    s = new VarSymbol(Flags.FINAL, idx.head.name, space, env.info.scope.owner);
                    idx.head.type = space;
                }
                localEnv.info.scope.enter(s);
                idx.head.sym = s;
                idx.head.sym.flags_field |= Flags.PARAMETER | TASKLOCAL;

            }

            // get type of body expression
            // (statement bodies are not allowed for reduce)
            JCDomainIter oldIter = domiter;
            domiter = tree;
            Type bodyType;
            if (tree.body != null) {
                bodyType = attribExpr(tree.body, localEnv);
            } else {
                log.error(tree.pos(), "non.expression.body.in.reduce.expr");
                bodyType = null;
            }
            if (domiter.mayReturn && oldIter != null) {
                oldIter.mayReturn = true;
            }

			domiter = oldIter;

            // leave local environment since we are done with the body
            localEnv.info.scope.leave();

            // body type has to be element type
            if (bodyType != null) {
                if (!types.isCastable(bodyType, space)) {
                    chk.typeError(tree.pos(), diags.fragment("incompatible.types"), bodyType, space);
                }
            }

            res = space;
            res.type_flags_field = space.type_flags_field;
            tree.iterType = t;            
                       

        } // 3. projection iteration
        else {

            boolean oldInsideProjection = insideProjection;
            insideProjection = true;
            // get the domain symbol of the iteration
            Type[] argtypes = new Type[tree.domargs.length()];
            Arrays.fill(argtypes, syms.intType);
            Symbol dsym = rs.resolveQualifiedMethod(tree.pos(), env, site, tree.name, List.from(argtypes), List.<Type>nil());
            if (!(dsym instanceof DomainSymbol)) {
                log.error(tree.pos(), "domain.not.a.domain", tree.getName());
                tree.type = owntype;
                result = owntype;
                return;
            }
            tree.sym = dsym;

            // check number of projection args
            DomainType projDomType = (DomainType) dsym.type;
            if (tree.domargs.length() != projDomType.projectionArgs.length()) {
                log.error(tree.pos(), "domain.wrong.number.of.indices", tree.getName());
            }

            // calculate reslt type of projection site.projDom(...)
            // this also checks, that projection args are bound
            ArrayType projResult = getProjectionResult(tree.pos(), site, projDomType, tree.params,null);

            // create a new environment for attributing the domain iteration
            Env<AttrContext> localEnv = memberEnter.domiterEnv(tree, env);

            // attribute indices, this also adds them to the environment
            for (List<JCVariableDecl> l = tree.domargs; l.nonEmpty(); l = l.tail) {
                attribStat(l.head, localEnv);
                l.head.sym.flags_field |= Flags.PARAMETER | TASKLOCAL;
            }

            // attribute body which can either be an expression or a statement(-list)
			Type oldType=domIterType;
			domIterType = projResult;
            JCDomainIter oldIter = domiter;
            domiter = tree;
            Type bodyType = null;
            if (tree.body != null) {
                bodyType = attribExpr(tree.body, localEnv);
            } else if (tree.sbody != null) {
                bodyType = attribStat(tree.sbody, localEnv);
            }
            if (domiter.mayReturn && oldIter != null) {
                oldIter.mayReturn = true;
            }
            domiter = oldIter;
			domIterType = oldType;

            // leave local environment since we are done with the body
            localEnv.info.scope.leave();

            // calculate result type of iteration
            if (bodyType == null || bodyType.tag == TypeTags.VOID) {

                // if body is empty (?) or has typ 'void', iteration also has type void
                res = syms.voidType;
                tree.iterType = projResult;

            } // body has to be an arraytype with the expected domain, its element
            // type can be arbitrary and is used as the element type of the result type
            else {

                bodyType = bodyType.getArrayType();
                // check that body type is an array
                if ((bodyType.tag != TypeTags.ARRAY)) {
                    log.error(tree.pos(), "array.req.but.found", bodyType);
                    tree.type = owntype;
                    result = owntype;
                    return;
                }
                ArrayType at = (ArrayType) bodyType;

                // calculate expected type
                ArrayType expectedBodyType = new ArrayType(at.elemtype, projResult.dom, syms.arrayClass);

                // check that body the domain is correct
                if (!types.isConvertible(at, expectedBodyType)) {
                    chk.typeTagError(tree.pos(), expectedBodyType, at);
                    //log.error(tree.pos(), "type.found.req", at, expectedBodyType);
                    tree.type = owntype;
                    result = owntype;
                    return;
                }

                DomainType dt = (DomainType) site.dom.clone();

                Type elt = at.elemtype;
                Type pta = pt.getArrayType();
                if (pta.tag == TypeTags.ARRAY) {
                    elt = ((ArrayType) pta).elemtype;
                }

                res = new ArrayType(elt, dt, syms.arrayClass);
                res.type_flags_field = site.type_flags_field;

                tree.iterType = expectedBodyType;
            }

            insideProjection = oldInsideProjection;
        }

        // check result type
        if (pt.tsym != null) {
            Warner warn = chk.convertWarner(tree.pos(), res, pt);
            if (!types.isConvertible(res, pt, warn)) {
                chk.typeError(tree.pos(), diags.fragment("incompatible.types"), res, pt);
            }
        }


        //dom iter always return unique values:
        //if input is unique and overwritten then it stays unqiue
        //if input is not unique, then we copy
        res = res.addFlag(Flags.LINEAR);

        tree.type = res;
        result = res;

    }

    void calcResultParams(DomainType dt) {
        if (dt.resultDomParams != null && dt.resultParams == null && dt.appliedParams != null)//calculate result dom params tp be used for type checking later
        {
            ListBuffer<JCExpression> res = new ListBuffer<JCExpression>();

            if (dt.resultDomParams != null) {
                Map<String, Object> varenv = new LinkedHashMap<String, Object>();
                // add values of parameters
                for (int i = 0; i < dt.formalParams.length(); i++) {
					JCExpression e=dt.appliedParams.get(i);
					boolean constraint=(e.getTag()==JCTree.GT||e.getTag()==JCTree.GE
							||e.getTag()==JCTree.LT||e.getTag()==JCTree.LE)
							&&(((JCBinary)e).lhs.getTag()==JCTree.IDENT);

					if(constraint)
						e=((JCBinary)e).lhs;

                    varenv.put(dt.formalParams.get(i).name.toString(), e);
                }

				Env<AttrContext> localEnv =
                    env.dup(null, env.info.dup(env.info.scope.dup()));

				if(dt.projectionArgs!=null)
				{

					for(VarSymbol vs:dt.projectionArgs)
					{
						vs.flags_field|=HASINIT|FINAL|TASKLOCAL;
						localEnv.info.scope.enter(vs);
					}
				}
                for (JCExpression e : dt.resultDomParams) {
                    e = replace(e, varenv, true);

                    Type t = attribExpr(e, localEnv);
                    res.add(e);
                }

	            localEnv.info.scope.leave();
            }


            dt.resultParams = res.toList();
        }

    }

    public void visitDomParent(JCDomParent tree) {
        // = result type

        // get domain smybol, resolveDomain returns a ClassDef if domain does not exist
        Symbol s = rs.resolveDomain(tree.pos(), tree.getName(), env);
        if (!(s instanceof DomainSymbol)) {
            tree.type = null;
            result = null;
            return;
        }

        // a result domain has to be a base domain
        DomainType dt = (DomainType) s.type;

        if (false&&!dt.isBaseDomain) {
            log.error(tree.pos(), "domain.not.a.base.domain", tree.getName());
            tree.type = null;
            result = null;
            return;
        }

        calcResultParams(dt);
        //for(JCExpression e : tree.domparams)
        //{
        //	Type t = attribExpr(e,env,syms.intType);
        //}

        tree.type = dt;
        result = dt;
    }

    public void visitDomInstance(JCDomInstance tree) {

        // A JCDomainInstance is a domain with arguments. (e.g. two_d{1,2})
        // Result domains and supertypes are NOT represented by JCDomainInstance

        // We have to check that the number of supplied arguments is correct
        // and that all arguments are (not necessarily literal) integer constants.

        // get domain symbol, resolveDomain returns a ClassDef if domain does not exist
        Symbol s = rs.resolveDomain(tree.pos(), tree.getName(), env);
        if (!(s instanceof DomainSymbol)) {
            System.out.println(env);
            log.error(tree.pos(), "domain.not.a.domain", tree.getName());
            tree.type = null;
            result = null;
            return;
        }

        DomainType dt = (DomainType) s.type;

        // check that domain type is not already applied
        if (dt.appliedParams != null) {
            log.error(tree.pos(), "domain.already.applied", s);
        }

        // check that supplied arguments are integer constants and add them to params
        ListBuffer<JCExpression> paramsBuffer = new ListBuffer<JCExpression>();
		int dim=0;
        for (JCExpression e : tree.domparams) {
			insideDomainParams=dim;
			domParamTree=e;

			attribExpr(e, env);

			boolean constraint=(e.getTag()==JCTree.GT||e.getTag()==JCTree.GE
					||e.getTag()==JCTree.LT||e.getTag()==JCTree.LE)
					&&(((JCBinary)e).lhs.type.constValue() instanceof Integer);

			JCExpression check=e;

			if(constraint)
				check=((JCBinary)e).lhs;

            e.type = attribExpr(check, env);

            paramsBuffer.add(check);
			dim++;
        }
		insideDomainParams=-1;
		domParamTree=null;


        List<JCExpression> params = paramsBuffer.toList();

        // test that the correct number of parameters was supplied
        if (params.length() != dt.formalParams.length()) {
            log.error(tree.pos(), "domain.wrong.number.of.params", s);
            ListBuffer<JCExpression> ps = new ListBuffer<JCExpression>();
            for (int i = 0; i < dt.formalParams.length(); i++) {
                if (params.size() > i) {
                    ps.add(params.get(i));
                } else {
                    ps.add(make.Literal(0));
                }
            }
            params = ps.toList();
        }

        // create new domain type with its own params
        dt = (DomainType) dt.clone();
        dt.appliedParams = params;

        // check that the domain is valid
        isAppliedDomainValid(tree.pos(), dt);

        calcResultParams(dt);

        tree.type = dt;
        result = dt;
    }

    public void visitDomUsage(JCDomUsage tree) {
        // = supertype

        // get domain symbol, resolveDomain returns a ClassDef if domain does not exist
        Symbol s = rs.resolveDomain(tree.pos(), tree.getName(), env);
        if (!(s instanceof DomainSymbol)) {
            log.error(tree.pos(), "domain.not.a.domain", tree.getName());
            tree.type = null;
            result = null;
            return;
        }

        // a superdomain has to be a base domain
        DomainType dt = (DomainType) s.type;
        if (false&&!dt.isBaseDomain) {
            log.error(tree.pos(), "domain.not.a.base.domain", tree.getName());
            tree.type = null;
            result = null;
            return;
        }

        /*DomainType dt = (DomainType) s.type;

         // test number of parameters
         if(tree.domparams.length() != dt.formalParams.length()) {
         log.error(tree.pos(), "domain.wrong.number.of.params", s);
         }

         // test that parameters are defined in environment
         for(JCDomParameter p : tree.domparams) {
         Object ps = env.info.scope.lookup(p.name);
         if(ps == null) {
         //log.error(tree.pos(), "compiler.err.wrong.number.type.args");
         }
         }*/
        calcResultParams(dt);

        tree.type = s.type;
        result = s.type;

    }

    public void visitMethodDef(JCMethodDecl tree) {
        MethodSymbol m = tree.sym;

		//if(tree.name.toString().contains("__"))
		//	log.error(tree.pos,"invalid.name",tree.name);

        boolean oldRU = returnUnique;

        returnUnique = false;
//		String flat=tree.getStringHeader();
        JCDomainIter oldIter = domiter;
        domiter = null;

        Map<VarSymbol, Set<List<JCExpression>>> old_alive = alive_vars;
        alive_vars = new LinkedHashMap<VarSymbol, Set<List<JCExpression>>>();

        Lint lint = env.info.lint.augment(m.attributes_field, m.flags());
        Lint prevLint = chk.setLint(lint);
        try {
            chk.checkDeprecatedAnnotation(tree.pos(), m);

            attribBounds(tree.typarams);

            // If we override any other methods, check that we do so properly.
            // JLS ???
            chk.checkOverride(tree, m);

            // Create a new environment with local scope
            // for attributing the method.
            Env<AttrContext> localEnv = memberEnter.methodEnv(tree, env);

            localEnv.info.lint = lint;

            // Enter all type parameters into the local method scope.
            for (List<JCTypeParameter> l = tree.typarams; l.nonEmpty(); l = l.tail) {
                localEnv.info.scope.enterIfAbsent(l.head.type.tsym);
            }

            ClassSymbol owner = env.enclClass.sym;
            if ((owner.flags() & ANNOTATION) != 0
                    && tree.params.nonEmpty()) {
                log.error(tree.params.head.pos(),
                        "intf.annotation.members.cant.have.params");
            }

			Scope enclScope = enter.enterScope(localEnv);

			for(Symbol s:tree.constraintsSyms.keySet())
				enclScope.enter(s);

            method_argument = true;

            // Attribute all value parameters.
            for (List<JCVariableDecl> l = tree.params; l.nonEmpty(); l = l.tail) {
                attribStat(l.head, localEnv);

                if (l.head.type.isReadLinear()) {
                    if (l.head.type.isPointer()) {
                        log.error(tree.pos, "return.linear.read", tree, l.head);
                    }
                    tree.sym.flags_field |= Flags.LINEARREAD;
                }

            }
            method_argument = false;

            // Check that type parameters are well-formed.
            chk.validate(tree.typarams, localEnv);
            if ((owner.flags() & ANNOTATION) != 0
                    && tree.typarams.nonEmpty()) {
                log.error(tree.typarams.head.pos(),
                        "intf.annotation.members.cant.have.type.params");
            }

            // Check that result type is well-formed.
            chk.validate(tree.restype, localEnv);
            if ((owner.flags() & ANNOTATION) != 0) {
                chk.validateAnnotationType(tree.restype);
            }

            if ((owner.flags() & ANNOTATION) != 0) {
                chk.validateAnnotationMethod(tree.pos(), m);
            }

            if ((tree.mods.flags & Flags.FINAL) != 0) {
                tree.sym.triggerName = tree.getID() + "Instance" + tree.pos;
            }

			//FIXME: constrains only on parameters??
			for(VarSymbol vs:tree.constraintsSyms.keySet())
			{
				Set<Pair<VarSymbol,Pair<Integer,JCExpression>>> set=tree.constraintsSyms.get(vs);

				if(!set.isEmpty()) //must verify constraints!
				{
					Pair<VarSymbol,Pair<Integer,JCExpression>> match=null;
					for(Pair<VarSymbol,Pair<Integer,JCExpression>> entry:set)
					{
						boolean constraint=(entry.snd.snd.getTag()==JCTree.GT||entry.snd.snd.getTag()==JCTree.GE
								||entry.snd.snd.getTag()==JCTree.LT||entry.snd.snd.getTag()==JCTree.LE)
								&&(((JCBinary)entry.snd.snd).lhs.getTag()==JCTree.IDENT&&((JCIdent)((JCBinary)entry.snd.snd).lhs).sym==vs);

						if(constraint||entry.snd.snd.getTag()==JCTree.IDENT&&((JCIdent)entry.snd.snd).sym==vs)
						{
							match=entry;
						}

						if(constraint)
						{
							Set<JCExpression> cset=tree.constraintsDeps.get(vs);

							if(cset==null)
								cset=new LinkedHashSet<JCExpression>();

							cset.add(entry.snd.snd);

							tree.constraintsDeps.put(vs,cset);
						}
					}
					if(match==null)
						log.error("no.dom.parameter.match", vs);

					tree.match.put(vs, match);
				}
			}

            if (tree.body == null) {
                // Empty bodies are only allowed for
                // abstract, native, or interface methods, or for methods
                // in a retrofit signature class.
                if ((owner.flags() & INTERFACE) == 0
                        && (tree.mods.flags & (ABSTRACT | FINAL)) == 0
                        && !relax && (env.enclClass.mods.flags & NATIVE) == 0) {
                    log.error(tree.pos(), "missing.meth.body.or.decl.abstract");
                }
                if (tree.defaultValue != null) {
                    if ((owner.flags() & ANNOTATION) == 0) {
                        log.error(tree.pos(),
                                "default.allowed.in.intf.annotation.member");
                    }
                }
            } else if ((owner.flags() & INTERFACE) != 0) {
                log.error(tree.body.pos(), "intf.meth.cant.have.body");
            } else if ((tree.mods.flags & ABSTRACT) != 0) {
                log.error(tree.pos(), "abstract.meth.cant.have.body");
            } else if ((tree.mods.flags & NATIVE) != 0) {
                log.error(tree.pos(), "native.meth.cant.have.body");
            } else {
                // Add an implicit super() call unless an explicit call to
                // super(...) or this(...) is given
                // or we are compiling class java.lang.Object.
                if (tree.name == names.init && owner.type != syms.objectType) {
                    JCBlock body = tree.body;
                    Type sup = ((ClassType) owner.type).getSuper();
                    if (sup == null || sup.tag != TypeTags.ARRAY) {
                        if (body.stats.isEmpty() || !TreeInfo.isSelfCall(body.stats.head)) {
                            tree.super_call = memberEnter.SuperCall(make.at(body.pos),
                                    List.<Type>nil(),
                                    List.<JCVariableDecl>nil(),
                                    false, sup, null);
                        } else if (TreeInfo.isSelfCall(body.stats.head)) {
                            JCMethodInvocation mi = TreeInfo.firstConstructorCall(tree);
                            List<Type> argtypes = attribArgs(mi.args, localEnv);
                            List<Type> typarams = attribArgs(mi.typeargs, localEnv);
                            List<JCVariableDecl> params = make.Params(argtypes, syms.noSymbol);
                            tree.super_call = memberEnter.SuperCall(make.at(body.pos), typarams, params, false, sup, mi.args);
                            body.stats.head = make.at(body.pos).Skip();
                        }
                    }
                    if ((env.enclClass.sym.flags() & ENUM) != 0
                            && (tree.mods.flags & GENERATEDCONSTR) == 0
                            && TreeInfo.isSuperCall(body.stats.head)) {
                        // enum constructors are not allowed to call super
                        // directly, so make sure there aren't any super calls
                        // in enum constructors, except in the compiler
                        // generated one.
                        log.error(tree.body.stats.head.pos(),
                                "call.to.super.not.allowed.in.enum.ctor",
                                env.enclClass.sym);
                    }

                }

                returnUnique = (tree.mods.flags & Flags.LINEAR) != 0;
                // Attribute method body.
                if (tree.analyse()) //unless template
                {
                    attribStat(tree.body, localEnv);
                }

            }

            if (tree.mods.group != null) {
                attribTree(tree.mods.group, localEnv, VAR, syms.groupType);
                tree.sym.groups = tree.mods.group.toString();

                if ((tree.mods.flags & Flags.BLOCKING) != 0) {
                    log.error(tree.pos(), "block.group");
                }
            }

            if (tree.mods.thread != null) {
                attribTree(tree.mods.thread, localEnv, VAR, syms.threadType);
                tree.sym.threads = tree.mods.thread.toString();

                if ((tree.mods.flags & Flags.BLOCKING) != 0) {
                    log.error(tree.pos(), "block.group");
                }
                tree.sym.flags_field |= Flags.FORCE_PARALLEL;
            }

            if (tree.mods.work != null) {
                if ((env.enclClass.sym.flags_field & Flags.NATIVE) == 0) {
                    log.error(tree.pos(), "mod.non.native", tree.mods.work);
                }
                Type t = attribTree(tree.mods.work, localEnv, VAL, syms.intType);
                tree.sym.EstimatedWork = (Integer) t.constValue();

            }
            if (tree.mods.task != null) {
                if ((env.enclClass.sym.flags_field & Flags.NATIVE) == 0) {
                    log.error(tree.pos(), "mod.non.native", tree.mods.task);
                }
                Type t = attribTree(tree.mods.task, localEnv, VAL, syms.intType);
                tree.sym.LocalTGWidth = ((Integer) t.constValue()) + 1;
            }
            if (tree.mods.mem != null) {
                if ((env.enclClass.sym.flags_field & Flags.NATIVE) == 0) {
                    log.error(tree.pos(), "mod.non.native", tree.mods.mem);
                }
                Type t = attribTree(tree.mods.mem, localEnv, VAL, syms.intType);
                tree.sym.EstimatedMem = (Integer) t.constValue();
            }

            if ((tree.mods.flags & Flags.NONBLOCKING) != 0)//supposed to be tail recursive
            {
                tree.sym.flags_field |= Flags.NONBLOCKING;
            }

			if ((tree.mods.flags & Flags.INLINE) != 0)//supposed to be tail recursive
            {
                tree.sym.flags_field |= Flags.INLINE;
            }

            //if it's a trigger..make all args out
            for (List<JCVariableDecl> l = tree.params; l.nonEmpty(); l = l.tail) {
                if ((tree.mods.flags & Flags.FINAL) != 0) {
                    if ((l.head.sym.flags() & Flags.FIN) != 0) {
                        log.error(l.head.pos(), "trigger.in.arg");
                    }
                    l.head.sym.flags_field |= Flags.FOUT;
                    l.head.getType().type = l.head.getType().type.addFlag(Flags.FOUT);
                    tree.sym.flags_field |= Flags.BLOCKING;
                }

                if ((l.head.sym.flags() & Flags.FINOUT) == 0) {
                    l.head.sym.flags_field |= Flags.FIN;
                }
            }

            localEnv.info.scope.leave();

            chk.validateAnnotations(tree.mods.annotations, m);

            if ((tree.sym.flags_field & Flags.STATIC) != 0 && tree.name.toString().equals(JCTree.MAIN_METHOD_NAME) && tree.sym.type.toString().equals(JCTree.MAIN_METHOD_TYPE)) {
                env.enclClass.sym.flags_field |= Flags.FOUT;//mark as main class
                if ((tree.sym.params.get(1).flags_field & FOUT) != 0) //note
                {
                    env.enclClass.sym.flags_field |= Flags.FIN;
                }
            }

            //mark const types!
            if (tree.restype != null && !tree.restype.type.isPrimitive() && (tree.restype.type.type_flags_field & Flags.LINEAR) == 0) {
                tree.restype.type = tree.restype.type.addFlag(Flags.FINAL);
            }

            if ((tree.mods.flags & Flags.LINEAR) != 0) {
                tree.restype.type = tree.restype.type.addFlag(Flags.LINEAR);
            }

            if (tree.restype != null && (tree.restype.type.type_flags_field & Flags.LINEARREAD) != 0) {
                log.error(tree.pos, "return.linear.read", tree, tree.restype);
            }



            //tree.sym.decl = tree;
        } finally {
            chk.setLint(prevLint);
            domiter = oldIter;
            returnUnique = oldRU;
            alive_vars = old_alive;
        }
    }

    public void visitVarDef(JCVariableDecl tree) {
        // Local variables have not been entered yet, so we need to do it now:
		//if(!tree.name.toString().equals("__this__")&&tree.name.toString().contains("__"))
		//	log.error(tree.pos,"invalid.name",tree.name);

        boolean is_member;
        if (env.info.scope.owner.kind == MTH
                || ((env.info.scope.owner.kind & DOM) != 0)) {
            is_member = false;
            if (tree.sym != null) {
                // parameters have already been entered
                env.info.scope.enter(tree.sym);
            } else {
                memberEnter.memberEnter(tree, env);
                annotate.flush();
            }
        } else {
            is_member = true;
            //owner is class?
            if (tree.init != null) {
                ClassSymbol cs = env.enclClass.sym;
                if (cs.init_constructors == null) {
                    cs.init_constructors = new ListBuffer<JCVariableDecl>();
                }
                cs.init_constructors.add(tree);
            }
        }

        VarSymbol v = tree.sym;

        if (tree.init == null && tree.sym.owner.kind == Kinds.MTH && (tree.sym.flags_field & Flags.PARAMETER) == 0) {
            tree.nop_if_alone = true;
            v.flags_field |= Flags.IMPLICITDECL;
        }

        if (env.enclClass.singular) {
            v.flags_field |= PRIVATE;
        }

        if ((tree.mods.flags & Flags.FINOUT) == Flags.FOUT)//we store the in/out mods with the symbol
        {
            //IMPORTAND:
            //if ONLY the FOUT mod is set:
            if (is_member)//if class attribute
            {
                v.flags_field |= Flags.FOUT; //it's an option (allow recursive data structures), not inlined, NOT stored with type but symbol
            } else//method argument
            {
                v.type = v.type.addFlag(Flags.FOUT); //mark that type is a pointer to the result
            }
            if (is_member) {
                v.owner.flags_field |= Flags.PARAMETER | TASKLOCAL;
            }

            //if both flags are set FINOUT, then the passed object will be modified, so passing the object as usual (as pointer) is sufficient
        }

        if ((v.flags_field & Flags.FOUT) != 0 && method_argument && env.enclClass.singular && (env.enclClass.mods.flags & Flags.ATOMIC) == 0) {
            log.error(tree.pos, "out.non.atomic", v);
        }

        if (!v.type.isPrimitive() && v.type.tag == TypeTags.CLASS) {
            env.enclClass.sym.recAddRef(v.type, false);
        }

        if ((tree.mods.flags & Flags.LINEAR) != 0) {
            if (v.isLocal() && (v.flags_field & Flags.PARAMETER) == 0 && !v.type.isPrimitive()) {
                if (v.type.tag != TypeTags.ARRAY) {
                    log.error(tree.pos, "linear.object", v.type);
                }
            }
            v.type = v.type.addFlag(Flags.LINEAR);
        }

        if ((tree.mods.flags & Flags.LINEARREAD) != 0) {
            v.type = v.type.addFlag(Flags.LINEARREAD);
            if (v.type.isLinear()) {
                log.error(tree.pos, "volatile.linear", v);
            }
        }

        if ((tree.mods.flags & Flags.UNSIGNED) != 0) {
            v.type = v.type.addFlag(Flags.UNSIGNED);
        }

        //groups are static by default
        if (v.type.tag == TypeTags.GROUP || v.type.tag == TypeTags.THREAD) {
            v.flags_field |= STATIC | HASINIT;
        }

        if ((v.flags_field & Flags.FINOUT) == Flags.FINOUT) {
            if ((v.type.type_flags_field & Flags.LINEAR) == 0) {
                log.error(tree.pos, "inout.not.linear", v);
            }
        }

        if ((v.type.type_flags_field & Flags.LINEAR) != 0 && (v.type.tsym.flags_field & Flags.SINGULAR) == 0 && (v.flags_field & Flags.STATIC) == 0) {
            if (v.owner == env.enclClass.sym && (env.enclClass.type.type_flags_field & Flags.LINEAR) == 0) {
                log.error(tree.pos, "linear.inside.nonlinear.class", v);
            }
        }

        //all non primitive vars that are not member vars of a class are pointers!
		/*
         if(v.owner.kind!=Kinds.TYP&&!v.type.isPrimitive())
         {
         v.type=v.type.addFlag(Flags.FOUT);
         }
         */

        if ((v.type.tsym.flags_field & Flags.INTERFACE) != 0 && (v.flags_field & Flags.FOUT) == 0 && v.owner.type.tag == TypeTags.CLASS) {
            log.error(tree.pos, "inst.of.unknown", v);
        }

        //mark const types!
        if (!v.type.isPrimitive() && (!v.type.isLinear())) {
            v.type = v.type.addFlag(Flags.FINAL);
        }

        if ((v.flags_field & Flags.ATOMIC) != 0) {
            v.type = v.type.addFlag(Flags.ATOMIC);
        }

        if (env.enclClass.singular) {
            v.type = v.type.addFlag(Flags.SINGULAR);
        }

        Lint lint = env.info.lint.augment(v.attributes_field, v.flags());
        Lint prevLint = chk.setLint(lint);

        // Check that the variable's declared type is well-formed.
        chk.validate(tree.vartype, env);

        try {
            chk.checkDeprecatedAnnotation(tree.pos(), v);

            if (tree.init != null) {
                if ((v.flags_field & FINAL) != 0 && tree.init.getTag() != JCTree.NEWCLASS) {
                    // In this case, `v' is final.  Ensure that it's initializer is
                    // evaluated.
                    v.getConstValue(); // ensure initializer is evaluated
                } else {
                    // Attribute initializer in a new environment
                    // with the declared variable as owner.
                    // Check that initializer conforms to variable's declared type.
                    Env<AttrContext> initEnv = memberEnter.initEnv(tree, env);
                    initEnv.info.lint = lint;
                    // In order to catch self-references, we set the variable's
                    // declaration position to maximal possible value, effectively
                    // marking the variable as undefined.
                    initEnv.info.enclVar = v;
                    attribExpr(tree.init, initEnv, v.type);
                }
            }

            if (!allowModify) {
                v.flags_field |= Flags.FINAL;
            } else {
                v.flags_field &= ~Flags.FINAL;
            }

            if (tree.init != null && (v.type.type_flags_field & Flags.LINEAR) != 0) {
                if (!v.type.isPrimitive() && !tree.init.type.isConst() && (tree.init.type.type_flags_field & Flags.LINEAR) == 0) {
                    if (v.type.getArrayType().tag != TypeTags.ARRAY || (tree.init.getTag() != JCTree.NEWARRAY && tree.init.getTag() != JCTree.NEWCLASS)) {
                        log.error(tree.pos, "copy.unique", tree, tree.init);
                    }
                }
            }


            result = tree.type = v.type;

            if (is_member) {
                v.owner.flags_field |= (tree.type.tsym.flags_field & Flags.PARAMETER);
            }

            chk.validateAnnotations(tree.mods.annotations, v);


            Type ot = v.owner.type;
            if (is_member && ot.tag == TypeTags.CLASS && ((Type.ClassType) ot).getArrayType().tag == TypeTags.ARRAY) {
                log.error(tree.pos(), "member.in.array");
            }

			if(result.getArrayType().tag==TypeTags.ARRAY)
			{
				ArrayType base=((ArrayType)result.getArrayType()).getStartType();
				if(base.dom!=null&&!base.dom.sizeIndepProjection())
					log.error(tree.pos(),"declare.dependent.projection", result);
			}

        } finally {
            chk.setLint(prevLint);
        }
    }

    public void visitSkip(JCSkip tree) {
        result = null;
    }

    public void visitBlock(JCBlock tree) {
        if (env.info.scope.owner.kind == TYP) {
            // Block is a static or instance initializer;
            // let the owner of the environment be a freshly
            // created BLOCK-method.
            Env<AttrContext> localEnv =
                    env.dup(tree, env.info.dup(env.info.scope.dupUnshared()));
//ALEX: removed this, what is it good for? env owner could be TYP
/*
             localEnv.info.scope.owner =
             new MethodSymbol(tree.flags | BLOCK, names.empty, null,
             env.info.scope.owner);
             */
            if ((tree.flags & STATIC) != 0) {
                localEnv.info.staticLevel++;
            }
            attribStats(tree.stats, localEnv);
        } else {
            // Create a new local environment with a local scope.
            Env<AttrContext> localEnv =
                    env.dup(tree, env.info.dup(env.info.scope.dup()));

			Map<VarSymbol, Set<JCExpression>> oldConstraints=env.enclMethod.constraintsDeps;
			env.enclMethod.constraintsDeps=new LinkedHashMap<VarSymbol, Set<JCExpression>>();
			env.enclMethod.constraintsDeps.putAll(oldConstraints);

            attribStats(tree.stats, localEnv);

			env.enclMethod.constraintsDeps=oldConstraints;

            localEnv.info.scope.leave();
        }
        result = null;
    }

    public void visitDoLoop(JCDoWhileLoop tree) {
        attribStat(tree.body, env.dup(tree));
        attribExpr(tree.cond, env, syms.booleanType);
        result = null;
    }

    public void visitWhileLoop(JCWhileLoop tree) {
        attribExpr(tree.cond, env, syms.booleanType);
        attribStat(tree.body, env.dup(tree));
        result = null;
    }

    public void visitForLoop(JCForLoop tree) {
        Env<AttrContext> loopEnv =
                env.dup(env.tree, env.info.dup(env.info.scope.dup()));
        attribStats(tree.init, loopEnv);
        if (tree.cond != null) {
            attribExpr(tree.cond, loopEnv, syms.booleanType);
        }
        loopEnv.tree = tree; // before, we were not in loop!
        attribStats(tree.step, loopEnv);
        attribStat(tree.body, loopEnv);
        loopEnv.info.scope.leave();
        result = null;
    }

    public void visitForeachLoop(JCEnhancedForLoop tree) {

        result = null;
    }

    public void visitLabelled(JCLabeledStatement tree) {
        // Check that label is not used in an enclosing statement
        Env<AttrContext> env1 = env;
        while (env1 != null && env1.tree.getTag() != JCTree.CLASSDEF) {
            if (env1.tree.getTag() == JCTree.LABELLED
                    && ((JCLabeledStatement) env1.tree).label == tree.label) {
                log.error(tree.pos(), "label.already.in.use",
                        tree.label);
                break;
            }
            env1 = env1.next;
        }

        attribStat(tree.body, env.dup(tree));
        result = null;
    }

    public void visitSwitch(JCSwitch tree) {
        Type seltype = attribExpr(tree.selector, env);

        Env<AttrContext> switchEnv =
                env.dup(tree, env.info.dup(env.info.scope.dup()));

        boolean enumSwitch =
                allowEnums
                && (seltype.tsym.flags() & Flags.ENUM) != 0;
        if (!enumSwitch) {
            seltype = chk.checkType(tree.selector.pos(), seltype, syms.intType);
        }

        // Attribute all cases and
        // check that there are no duplicate case labels or default clauses.
        Set<Object> labels = new LinkedHashSet<Object>(); // The set of case labels.
        boolean hasDefault = false;      // Is there a default label?
        for (List<JCCase> l = tree.cases; l.nonEmpty(); l = l.tail) {
            JCCase c = l.head;
            Env<AttrContext> caseEnv =
                    switchEnv.dup(c, env.info.dup(switchEnv.info.scope.dup()));
            if (c.pat != null) {
                if (enumSwitch) {
                    Symbol sym = enumConstant(c.pat, seltype);
                    if (sym == null) {
                        log.error(c.pat.pos(), "enum.const.req");
                    } else if (!labels.add(sym)) {
                        log.error(c.pos(), "duplicate.case.label");
                    }
                } else {
                    Type pattype = attribExpr(c.pat, switchEnv, seltype);
                    if (pattype.tag != ERROR) {
                        if (pattype.constValue() == null) {
                            log.error(c.pat.pos(), "const.expr.req");
                        } else if (labels.contains(pattype.constValue())) {
                            log.error(c.pos(), "duplicate.case.label");
                        } else {
                            labels.add(pattype.constValue());
                        }
                    }
                }
            } else if (hasDefault) {
                log.error(c.pos(), "duplicate.default.label");
            } else {
                hasDefault = true;
            }
            attribStats(c.stats, caseEnv);
            caseEnv.info.scope.leave();
            addVars(c.stats, switchEnv.info.scope);
        }

        switchEnv.info.scope.leave();
        result = null;
    }
    // where

    /**
     * Add any variables defined in stats to the switch scope.
     */
    private static void addVars(List<JCStatement> stats, Scope switchScope) {
        for (; stats.nonEmpty(); stats = stats.tail) {
            JCTree stat = stats.head;
            if (stat.getTag() == JCTree.VARDEF) {
                switchScope.enter(((JCVariableDecl) stat).sym);
            }
        }
    }
    // where

    /**
     * Return the selected enumeration constant symbol, or null.
     */
    private Symbol enumConstant(JCTree tree, Type enumType) {
        if (tree.getTag() != JCTree.IDENT) {
            log.error(tree.pos(), "enum.label.must.be.unqualified.enum");
            return syms.errSymbol;
        }
        JCIdent ident = (JCIdent) tree;
        Name name = ident.name;
        for (Scope.Entry e = enumType.tsym.members().lookup(name);
                e.scope != null; e = e.next()) {
            if (e.sym.kind == VAR) {
                Symbol s = ident.sym = e.sym;
                ((VarSymbol) s).getConstValue(); // ensure initializer is evaluated
                ident.type = s.type;
                return ((s.flags_field & Flags.ENUM) == 0)
                        ? null : s;
            }
        }
        return null;
    }

    public void visitSynchronized(JCSynchronized tree) {
        chk.checkRefType(tree.pos(), attribExpr(tree.lock, env));
        attribStat(tree.body, env);
        result = null;
    }

    public void visitTry(JCTry tree) {
        // Attribute body
        result = null;
    }

    public void visitConditional(JCConditional tree) {
        attribExpr(tree.cond, env, syms.booleanType);
        attribExpr(tree.truepart, env);
        attribExpr(tree.falsepart, env);
        result = check(tree,
                capture(condType(tree.pos(), tree.cond.type,
                tree.truepart.type, tree.falsepart.type)),
                VAL, pkind, pt);
    }
    //where

    /**
     * Compute the type of a conditional expression, after checking that it
     * exists. See Spec 15.25.
     *
     * @param pos The source position to be used for error diagnostics.
     * @param condtype The type of the expression's condition.
     * @param thentype The type of the expression's then-part.
     * @param elsetype The type of the expression's else-part.
     */
    private Type condType(DiagnosticPosition pos,
            Type condtype,
            Type thentype,
            Type elsetype) {
        Type ctype = condType1(pos, condtype, thentype, elsetype);

        // If condition and both arms are numeric constants,
        // evaluate at compile-time.
        return ((condtype.constValue() != null)
                && (thentype.constValue() != null)
                && (elsetype.constValue() != null))
                ? cfolder.coerce(condtype.isTrue() ? thentype : elsetype, ctype)
                : ctype;
    }

    /**
     * Compute the type of a conditional expression, after checking that it
     * exists. Does not take into account the special case where condition and
     * both arms are constants.
     *
     * @param pos The source position to be used for error diagnostics.
     * @param condtype The type of the expression's condition.
     * @param thentype The type of the expression's then-part.
     * @param elsetype The type of the expression's else-part.
     */
    private Type condType1(DiagnosticPosition pos, Type condtype,
            Type thentype, Type elsetype) {
        // If same type, that is the result
        if (types.isSameType(thentype, elsetype)) {
            return thentype.baseType();
        }

        Type thenUnboxed = (!allowBoxing || thentype.isPrimitive())
                ? thentype : types.unboxedType(thentype);
        Type elseUnboxed = (!allowBoxing || elsetype.isPrimitive())
                ? elsetype : types.unboxedType(elsetype);

        // Otherwise, if both arms can be converted to a numeric
        // type, return the least numeric type that fits both arms
        // (i.e. return larger of the two, or return int if one
        // arm is short, the other is char).
        if (thenUnboxed.isPrimitive() && elseUnboxed.isPrimitive()) {
            // If one arm has an integer subrange type (i.e., byte,
            // short, or char), and the other is an integer constant
            // that fits into the subrange, return the subrange type.
            if (thenUnboxed.tag < INT && elseUnboxed.tag == INT
                    && types.isAssignable(elseUnboxed, thenUnboxed)) {
                return thenUnboxed.baseType();
            }
            if (elseUnboxed.tag < INT && thenUnboxed.tag == INT
                    && types.isAssignable(thenUnboxed, elseUnboxed)) {
                return elseUnboxed.baseType();
            }

            for (int i = BYTE; i < VOID; i++) {
                Type candidate = syms.typeOfTag[i];
                if (types.isSubtype(thenUnboxed, candidate)
                        && types.isSubtype(elseUnboxed, candidate)) {
                    return candidate;
                }
            }
        }

        // Those were all the cases that could result in a primitive
        if (allowBoxing) {
            if (thentype.isPrimitive()) {
                thentype = types.boxedClass(thentype).type;
            }
            if (elsetype.isPrimitive()) {
                elsetype = types.boxedClass(elsetype).type;
            }
        }

        if (types.isSubtype(thentype, elsetype)) {
            return elsetype.baseType();
        }
        if (types.isSubtype(elsetype, thentype)) {
            return thentype.baseType();
        }

        if (!allowBoxing || thentype.tag == VOID || elsetype.tag == VOID) {
            log.error(pos, "neither.conditional.subtype",
                    thentype, elsetype);
            return thentype.baseType();
        }

        // both are known to be reference types.  The result is
        // lub(thentype,elsetype). This cannot fail, as it will
        // always be possible to infer "Object" if nothing better.
        return types.lub(thentype.baseType(), elsetype.baseType());
    }

	public void visitAssert(JCAssert tree)
	{
		inside_cond=tree.cond;
        attribExpr(tree.cond, env, syms.booleanType);
		inside_cond=null;
	}

    public void visitIf(JCIf tree) {

		Map<VarSymbol, Set<JCExpression>> oldConstraints=env.enclMethod.constraintsDeps;
		env.enclMethod.constraintsDeps=new LinkedHashMap<VarSymbol, Set<JCExpression>>();


		//env.enclMethod.constraintsDeps.putAll(oldConstraints);
		inside_cond=tree.cond;
        attribExpr(tree.cond, env, syms.booleanType);
		inside_cond=null;
        boolean old_branch = inside_branch;
        inside_branch = true;


		Map<VarSymbol, Set<JCExpression>> copyConstraints=new LinkedHashMap<VarSymbol, Set<JCExpression>>();
		copyConstraints.putAll(env.enclMethod.constraintsDeps);

		for(VarSymbol vs:oldConstraints.keySet())
		{
			Set<JCExpression> set=env.enclMethod.constraintsDeps.get(vs);
			if(set==null)
				set=new LinkedHashSet<JCExpression>();
			set.addAll(oldConstraints.get(vs));
			env.enclMethod.constraintsDeps.put(vs, set);
		}

        attribStat(tree.thenpart, env);

		env.enclMethod.constraintsDeps=oldConstraints;
        if (tree.elsepart != null) {
			for(VarSymbol vs:copyConstraints.keySet())
			{
				Set<JCExpression> notset=new LinkedHashSet<JCExpression>();
				//FIXME: negate!
				for(JCExpression e:copyConstraints.get(vs))
				{
					notset.add(make.Unary(JCTree.NOT, e));
				}
				env.enclMethod.constraintsDeps.put(vs, notset);
			}

            attribStat(tree.elsepart, env);
        }
		env.enclMethod.constraintsDeps=oldConstraints;

        chk.checkEmptyIf(tree);
        inside_branch = old_branch;
        result = null;
    }

    public void visitIfExp(JCIfExp tree) {
		Map<VarSymbol, Set<JCExpression>> oldConstraints=env.enclMethod.constraintsDeps;
		env.enclMethod.constraintsDeps=new LinkedHashMap<VarSymbol, Set<JCExpression>>();
		inside_cond=tree.cond;

        attribExpr(tree.cond, env, syms.booleanType);

		inside_cond=null;
        boolean old_branch = inside_branch;
        inside_branch = true;


		Map<VarSymbol, Set<JCExpression>> copyConstraints=new LinkedHashMap<VarSymbol, Set<JCExpression>>();
		copyConstraints.putAll(env.enclMethod.constraintsDeps);

		for(VarSymbol vs:oldConstraints.keySet())
		{
			Set<JCExpression> set=env.enclMethod.constraintsDeps.get(vs);
			if(set==null)
				set=new LinkedHashSet<JCExpression>();
			set.addAll(oldConstraints.get(vs));
			env.enclMethod.constraintsDeps.put(vs, set);
		}

        Type t1 = attribExpr(tree.thenpart, env);

		env.enclMethod.constraintsDeps=oldConstraints;

        if (tree.elsepart != null) {
			for(VarSymbol vs:copyConstraints.keySet())
			{
				Set<JCExpression> notset=new LinkedHashSet<JCExpression>();
				//FIXME: negate!
				for(JCExpression e:copyConstraints.get(vs))
				{
					notset.add(make.Unary(JCTree.NOT, e));
				}
				env.enclMethod.constraintsDeps.put(vs, notset);
			}
            attribExpr(tree.elsepart, env, t1);
        }
		env.enclMethod.constraintsDeps=oldConstraints;

        chk.checkEmptyIfExp(tree);

        tree.type = t1;
        inside_branch = old_branch;

        result = check(tree, capture(t1), VAL, pkind, pt);;
    }

    public void visitSelectCond(JCSelectCond tree) {
        if (tree.cond != null) {
            attribExpr(tree.cond, env, pt);
        }

        if (tree.res != null) {
            result = attribExpr(tree.res, env);
        } else {
            attribStat(tree.stmnt, env);
            result = Type.noType;
        }

    }

    public void visitSelectExp(JCSelect tree) {

        Type shared = attribTree(tree.list.head, env, VAL, syms.booleanType);

        DiagnosticPosition pos;

        if (tree.list.head != null) {
            if (tree.list.head.res != null) {
                pos = tree.list.head.res.pos();
            } else {
                pos = tree.list.head.stmnt.pos();
            }
        } else {
            pos = tree.pos();
        }

        chk.checkType(pos, shared, pt);

        boolean has_default = false;

        for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {
            Type s = attribTree(l.head, env, VAL, syms.booleanType);
            chk.checkType(l.head.pos(), s, shared);

            if (l.head.cond == null) {
                if (!has_default) {
                    has_default = true;
                } else {
                    log.error(l.head.cond.pos(), "multi.default");
                }
            } else {
                pos = l.head.cond.pos();
            }
        }

        if (!pt.equals(Type.noType) && !has_default) {
            //FIXME:check whether list is exhausting?
            log.error(pos, "missing.default");
        }

        tree.type = shared;

        result = tree.type;
    }

    public void visitCaseExp(JCCaseExp tree) {

        attribExpr(tree.exp, env, syms.intType);

        Type shared = attribTree(tree.list.head, env, VAL, syms.intType);

        DiagnosticPosition pos;

        if (tree.list.head != null) {
            if (tree.list.head.res != null) {
                pos = tree.list.head.res.pos();
            } else {
                pos = tree.list.head.stmnt.pos();
            }
        } else {
            pos = tree.pos();
        }

        chk.checkType(pos, shared, pt);

        boolean has_default = false;

        for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {
            Type s = attribTree(l.head, env, VAL, syms.intType);
            chk.checkType(l.head.pos(), s, shared);

            if (l.head.cond == null) {
                if (!has_default) {
                    has_default = true;
                } else {
                    log.error(l.head.cond.pos(), "multi.default");
                }
            } else {
                pos = l.head.cond.pos();
            }
        }

        if (!pt.equals(Type.noType) && !has_default) {
            //FIXME:check whether list is exhausting?
            log.error(pos, "missing.default");
        }

        tree.type = pt;

        result = tree.type;
    }

    public void visitExec(JCExpressionStatement tree) {
        attribExpr(tree.expr, env);
        result = null;
    }

    public void visitBreak(JCBreak tree) {
        tree.target = findJumpTarget(tree.pos(), tree.getTag(), tree.label, env);
        result = null;
    }

    public void visitContinue(JCContinue tree) {
        tree.target = findJumpTarget(tree.pos(), tree.getTag(), tree.label, env);
        result = null;
    }
    //where

    /**
     * Return the target of a break or continue statement, if it exists, report
     * an error if not. Note: The target of a labelled break or continue is the
     * (non-labelled) statement tree referred to by the label, not the tree
     * representing the labelled statement itself.
     *
     * @param pos The position to be used for error diagnostics
     * @param tag The tag of the jump statement. This is either Tree.BREAK or
     * Tree.CONTINUE.
     * @param label The label of the jump statement, or null if no label is
     * given.
     * @param env The environment current at the jump statement.
     */
    private JCTree findJumpTarget(DiagnosticPosition pos,
            int tag,
            Name label,
            Env<AttrContext> env) {
        // Search environments outwards from the point of jump.
        Env<AttrContext> env1 = env;
        LOOP:
        while (env1 != null) {
            switch (env1.tree.getTag()) {
                case JCTree.LABELLED:
                    JCLabeledStatement labelled = (JCLabeledStatement) env1.tree;
                    if (label == labelled.label) {
                        // If jump is a continue, check that target is a loop.
                        if (tag == JCTree.CONTINUE) {
                            if (labelled.body.getTag() != JCTree.DOLOOP
                                    && labelled.body.getTag() != JCTree.WHILELOOP
                                    && labelled.body.getTag() != JCTree.FORLOOP
                                    && labelled.body.getTag() != JCTree.FOREACHLOOP) {
                                log.error(pos, "not.loop.label", label);
                            }
                            // Found labelled statement target, now go inwards
                            // to next non-labelled tree.
                            return TreeInfo.referencedStatement(labelled);
                        } else {
                            return labelled;
                        }
                    }
                    break;
                case JCTree.DOLOOP:
                case JCTree.WHILELOOP:
                case JCTree.FORLOOP:
                case JCTree.FOREACHLOOP:
                    if (label == null) {
                        return env1.tree;
                    }
                    break;
                case JCTree.SWITCH:
                    if (label == null && tag == JCTree.BREAK) {
                        return env1.tree;
                    }
                    break;
                case JCTree.METHODDEF:
                case JCTree.CLASSDEF:
                    break LOOP;
                default:
            }
            env1 = env1.next;
        }
        if (label != null) {
            log.error(pos, "undef.label", label);
        } else if (tag == JCTree.CONTINUE) {
            log.error(pos, "cont.outside.loop");
        } else {
            log.error(pos, "break.outside.switch.loop");
        }
        return null;
    }

    public void visitReturn(JCReturn tree) {
        // Check that there is an enclosing method which is
        // nested within than the enclosing class.

        if (env.enclMethod == null
                || env.enclMethod.sym.owner != env.enclClass.sym) {
            log.error(tree.pos(), "ret.outside.meth");

        } else {
            // Attribute return expression, if it exists, and check that
            // it conforms to result type of enclosing method.

            Symbol m = env.enclMethod.sym;

            //if((env.enclMethod.return_flags&tree.flags&Flags.FINAL)!=0)
            //	log.error(tree.pos,"multiple.final");

            if ((tree.flags & Flags.FINAL) != 0) {
                if (env.enclMethod.final_value != null && env.enclMethod.final_value != tree)//templates may do this multiple times..
                {
                    log.error(tree.pos, "multiple.final");
                }

                //mark the finally exp; tree as a nop inside the dep graph
                tree.nop = true;

                //and store the final value with the method for analysis
                env.enclMethod.final_value = tree;

            } else {
                if (domiter != null) {
                    domiter.mayReturn = true;
                }
            }

            env.enclMethod.return_flags |= tree.flags;

            Type rt = m.type.getReturnType();

            if ((env.enclMethod.mods.flags & Flags.FINAL) != 0) //
            {
                rt = syms.voidType;
            }

            boolean inside_event = (env.enclMethod.restype == null && env.enclMethod.name != env.enclMethod.name.table.names.init);
            if (inside_event && rt.tag == VOID && env.enclClass.isSingular()) {
                rt = env.enclClass.type;
            }

            if (env.enclClass.isSingular() && (tree.flags & Flags.FINAL) == 0 && inside_event) {
                log.error(tree.pos(), "non.finally.in.singular");
            }

            if (rt.tag == VOID) {
                if (tree.expr != null) {
                    attribExpr(tree.expr, env, rt);
                }
            } else if (tree.expr == null) {
                log.error(tree.pos(), "missing.ret.val");
            } else {
                Type t = attribExpr(tree.expr, env, rt);
                if (returnUnique && (t.type_flags_field & Flags.LINEAR) == 0) {
                    if (!tree.expr.type.isPrimitive() && !tree.expr.type.isConst()) {
                        if (tree.expr.type.getArrayType().tag != TypeTags.ARRAY || (tree.expr.getTag() != JCTree.NEWARRAY && tree.expr.getTag() != JCTree.NEWCLASS)) {
                            log.error(tree.pos(), "copy.unique", tree.expr, "return value");
                        }
                    }
                }
            }

        }
        result = null;
    }

    public void visitThrow(JCThrow tree) {

        result = null;
    }

    public void visitPragma(JCPragma tree) {

        tree.nop = true; //does not contribute to code generation..easier than filtering all nops with a tree translator (e.g. remove finally)

        Type first = attribExpr(tree.cond, env);

        Type second = attribExpr(tree.detail, env);

        env.enclMethod.pragmas.add(tree);

        if (tree.flag == Flags.WORK) {
            if (!check(tree.detail, second, VAL, VAL, syms.doubleType).isErroneous()) {
                if (tree.detail.type.constValue() != null) {
					Number n=(Number)tree.detail.type.constValue();
                    TreeInfo.symbol(tree.cond).work = n.floatValue() ;
                    tree.s1 = (VarSymbol) TreeInfo.symbol(tree.detail);
                } else {
                    log.error(tree.pos(), "no.constant", tree.detail);
                }
            }
        } else if (tree.flag == Flags.TIME) {
            if (!check(tree.detail, second, VAL, VAL, syms.stringType).isErroneous()) {
                if (tree.detail.type.constValue() != null) {
                    TreeInfo.symbol(tree.cond).time = (String) tree.detail.type.constValue();
                    tree.s1 = (VarSymbol) TreeInfo.symbol(tree.detail);
                } else {
                    log.error(tree.pos(), "no.string", tree.detail);
                }
            }
        } else {
            check(tree.detail, first, VAR, VAR, Type.noType);
            check(tree.detail, second, VAR, VAR, Type.noType);
            tree.s1 = (VarSymbol) TreeInfo.symbol(tree.detail);
            tree.s2 = (VarSymbol) TreeInfo.symbol(tree.cond);
        }

        result = null;
    }

    String getCacheId(List<JCExpression> actuals) {
        String res = "";

        boolean first = true;
        for (JCExpression e : actuals) {
            if (!first) {
                res += "_";
            }
            if (e.type.getArrayType().tag==TypeTags.ARRAY&&e.type.isLinear()) {
                res += "U";
            }
            res += e.toString();

            first = false;
        }

        return res;
    }

    public MethodSymbol instantiateTemplate(List<Type> formals, MethodType mt, MethodSymbol ms, List<JCExpression> actuals) {

        //String cid=getCacheId(actuals);
        MethodSymbol s = ms.cache.get(getCacheId(actuals));
        if (s == null) {
            for (JCExpression e : actuals) {
                env.enclClass.sym.recAddRef(e.type, false);
            }

            Type t;
            //not yet instantiated
            Iterator<Type> fa = formals.iterator();
            Iterator<JCExpression> ia = actuals.iterator();

            Map<String, Object> map = new LinkedHashMap<String, Object>();

            while (fa.hasNext() && ia.hasNext()) {
				JCExpression e=(JCExpression)copy.copy(ia.next());
                map.put(fa.next().tsym.toString(), e);
            }

            JCTree instance = replace(ms.decl, map, true);

            Iterator<JCTypeParameter> pi = ((JCMethodDecl) ms.decl).typarams.iterator();
            Iterator<JCExpression> ai = actuals.iterator();
            Iterator<Type> fi = formals.iterator();


            ListBuffer<JCTypeParameter> tp = new ListBuffer<JCTypeParameter>();

            while (pi.hasNext() && ai.hasNext() && fi.hasNext()) {
                JCTypeParameter p = pi.next();
                JCExpression a = ai.next();
                Type f = fi.next();
                if (types.isSameType(a.type, f)) {
                    tp.add(p);
                }
            }

            ((JCMethodDecl) instance).typarams = tp.toList();//empty!

			if((ms.owner.flags_field&Flags.NATIVE)==0&&(ms.flags_field&Flags.STATIC)!=0)
				((JCMethodDecl) instance).name = names.fromString(((JCMethodDecl) instance).name + "__" + JCTree.fixName(getCacheId(actuals)));

            ((JCMethodDecl) instance).sym = (MethodSymbol) ms.clone();
            ((JCMethodDecl) instance).sym.name = ((JCMethodDecl) instance).name;

            //Env<AttrContext> outer=enter.typeEnvs.get(ms.owner);

            ListBuffer<JCTree> defs = new ListBuffer<JCTree>();

            for (JCTree d : ((ClassType) ms.owner.type).tree.defs) {
                if (d.getTag() != JCTree.METHODDEF || ((JCMethodDecl) d).typarams.size() == 0) {
                    defs.add(d);
                }
            }

            defs.add(instance);

            ((ClassType) ms.owner.type).tree.defs = defs.toList();

            //deepcopy current env
            Env<AttrContext> source = deepdupto(enter.typeEnvs.get(ms.owner));
            memberEnter.memberEnter(env.toplevel, source);

            Env<AttrContext> owner = enter.getEnv(ms.owner.type.tsym);
            JavaFileObject prev = log.useSource(
                    owner.enclClass.sym.sourcefile != null
                    ? owner.enclClass.sym.sourcefile
                    : owner.toplevel.sourcefile);
            try {
                memberEnter.memberEnter(instance, source);

                t = ((JCMethodDecl) instance).sym.type;
                ((MethodType) t).template = ms.name.toString() + "<" + actuals + ">";

                s = ((JCMethodDecl) instance).sym;
                ms.cache.put(getCacheId(actuals), s);


                Env<AttrContext> lintEnv = owner;
                while (lintEnv.info.lint == null) {
                    lintEnv = lintEnv.next;
                }

                Lint lint = source.info.lint;

                source.info.lint = lintEnv.info.lint.augment(ms.owner.attributes_field, ms.owner.flags());

                attribStat(instance, source);

                source.info.lint = lint;
            } finally {
                log.useSource(prev);
            }


        }
        return s;
    }

    /**
     * Visitor method for method invocations. NOTE: The method part of an
     * application will have in its type field the return type of the method,
     * not the method's type itself!
     */
    public void visitApply(JCMethodInvocation tree) {

        // The local environment of a method application is
        // a new environment nested in the current one.
        Env<AttrContext> localEnv = env.dup(tree, env.info.dup());

        // The types of the actual method arguments.
        List<Type> argtypes;

        // The types of the actual method type arguments.
        List<Type> typeargtypes = null;

        Name methName = TreeInfo.name(tree.meth);

        boolean isConstructorCall =
                methName == names._super;

        if (isConstructorCall) {
            // We are seeing a ...this(...) or ...super(...) call.
            // Check that this is the first statement in a constructor.
            if (checkFirstConstructorStat(tree, env)) {

                // Record the fact
                // that this is a constructor call (using isSelfCall).
                localEnv.info.isSelfCall = true;

                // Attribute arguments, yielding list of argument types.
                argtypes = attribArgs(tree.args, localEnv);
                typeargtypes = attribTypes(tree.typeargs, localEnv);

                // Variable `site' points to the class in which the called
                // constructor is defined.
                Type site = env.enclClass.sym.type;
                if (methName == names._super) {
                    if (site == syms.objectType) {
                        log.error(tree.meth.pos(), "no.superclass", site);
                        site = types.createErrorType(syms.objectType);
                    } else {
                        site = types.supertype(site);
                    }
                }

                if (site.tag == CLASS) {
                    if (site.getEnclosingType().tag == CLASS) {
                        // we are calling a nested class

                        if (tree.meth.getTag() == JCTree.SELECT) {
                            JCTree qualifier = ((JCFieldAccess) tree.meth).selected;

                            // We are seeing a prefixed call, of the form
                            //     <expr>.super(...).
                            // Check that the prefix expression conforms
                            // to the outer instance type of the class.
                            chk.checkRefType(qualifier.pos(),
                                    attribExpr(qualifier, localEnv,
                                    site.getEnclosingType()));
                        } else if (methName == names._super) {
                            // qualifier omitted; check for existence
                            // of an appropriate implicit qualifier.
                            rs.resolveImplicitThis(tree.meth.pos(),
                                    localEnv, site);
                        }
                    } else if (tree.meth.getTag() == JCTree.SELECT) {
                        log.error(tree.meth.pos(), "illegal.qual.not.icls",
                                site.tsym);
                    }

                    // if we're calling a java.lang.Enum constructor,
                    // prefix the implicit String and int parameters
                    if (site.tsym == syms.enumSym && allowEnums) {
                        argtypes = argtypes.prepend(syms.intType).prepend(syms.stringType);
                    }

                    // Resolve the called constructor under the assumption
                    // that we are referring to a superclass instance of the
                    // current instance (JLS ???).
                    boolean selectSuperPrev = localEnv.info.selectSuper;
                    localEnv.info.selectSuper = true;
                    localEnv.info.varArgs = false;
                    Symbol sym = rs.resolveConstructor(
                            tree.meth.pos(), localEnv, site, argtypes, typeargtypes);
                    localEnv.info.selectSuper = selectSuperPrev;

                    // Set method symbol to resolved constructor...
                    TreeInfo.setSymbol(tree.meth, sym);

                    // ...and check that it is legal in the current context.
                    // (this will also set the tree's type)
                    Type mpt = newMethTemplate(argtypes, typeargtypes);
                    checkId(tree.meth, site, sym, localEnv, MTH,
                            mpt, tree.varargsElement != null);
                }
                // Otherwise, `site' is an error type and we do nothing
            }
            result = tree.type = syms.voidType;
        } else {
            // Otherwise, we are seeing a regular method call.
            // Attribute the arguments, yielding list of argument types, ...
            argtypes = attribArgs(tree.args, localEnv);
            typeargtypes = attribTypes(tree.typeargs, localEnv);

            // ... and attribute the method using as a prototype a methodtype
            // whose formal argument types is exactly the list of actual
            // arguments (this will also set the method symbol).
            Type mpt = newMethTemplate(argtypes, typeargtypes);
            localEnv.info.varArgs = false;
			boolean oldInside=insideMethodAttrib;

			insideMethodAttrib=true;
            Type mtype = attribExpr(tree.meth, localEnv, mpt);
			insideMethodAttrib=oldInside;

            // method not found -> ClassSymbol as error
            if (!(TreeInfo.symbol(tree.meth) instanceof MethodSymbol)||mtype.isErroneous()) {
				check(tree, capture(mtype), VAL, pkind, pt);
                result = mtype;
                return;
            }


            MethodSymbol ms = (MethodSymbol) TreeInfo.symbol(tree.meth);


            if (ms.decl != null) {
                if (((JCMethodDecl) ms.decl).typarams.size() > 0) {
                    //mtype.getParameterTypes()
                    List<JCExpression> tparams;

                    if (!tree.typeargs.isEmpty()) {
                        tparams = tree.typeargs;
                    } else {
                        ListBuffer<JCExpression> actuals = new ListBuffer<JCExpression>();

                        for (Type t : ((ForAll) ((MethodType) mtype).restype).tvars) {
							if(t.constValue()!=null)
							{
								if(t.constValue() instanceof Integer)
									actuals.add(make.Literal(t.constValue()));
								else
									actuals.add((JCExpression)t.constValue());

							}
							else
								actuals.add(make.Type(t));
                        }
                        tparams = actuals.toList();
                    }
                    List<Type> formals = ((ForAll) ms.type).tvars;

                    ms = instantiateTemplate(formals, (MethodType) mtype, ms, tparams);

                    mtype = ms.type;

                    TreeInfo.setsymbol(tree.meth, ms);
                }
            } else if (((MethodType) mtype).restype.tag == TypeTags.FORALL) {
                //we store the actual type instances in ((ForAll)((MethodType)mtype).restype).tvars so we can instatntiate the template (above code),
                //but if we don't have the code (it's a native method) then we need to restore the 'normal' type:
                if (((ForAll) ((MethodType) mtype).restype).tvars.size() == 1) {
                    ((MethodType) mtype).restype = ((ForAll) ((MethodType) mtype).restype).tvars.head;
                }
            }

            boolean isDomProj = ms.isDomainProjection;

            if (localEnv.info.varArgs) {
                assert mtype.isErroneous() || tree.varargsElement != null;
            }

            // Compute the result type.
            Type restype = mtype.getReturnType();
            assert restype.tag != WILDCARD : mtype;
            Type triggertype = restype;
            if (tree.getTriggerReturn() != null) {
                restype = syms.voidType;
            } else if ((ms.flags_field & Flags.FINAL) != 0) {
                //create context specific UID container
                if ((env.enclMethod.sym.flags_field & Flags.STATIC) != 0) {
                    if (env.enclClass.staticTrigger == null) {
                        env.enclClass.staticTrigger = new LinkedHashSet<JCMethodInvocation>();
                    }
                    env.enclClass.staticTrigger.add(tree);
                } else {
                    if (env.enclClass.nonStaticTrigger == null) {
                        env.enclClass.nonStaticTrigger = new LinkedHashSet<JCMethodInvocation>();
                    }
                    env.enclClass.nonStaticTrigger.add(tree);
                }
            }

            if (ms instanceof MethodSymbol) {
                if (tree.getTriggerReturn() == null) //triggers have out only!
                {
                    attribArgs(tree.args, localEnv, ms.params());
                }
            }

            if (!isDomProj && tree.getTriggerReturn() != null) {
                MethodSymbol sym = (MethodSymbol) rs.resolveMethod(tree, localEnv, methName, argtypes, typeargtypes);
                if ((sym.flags() & Flags.FINAL) == 0) {
                    log.error(tree.trigger_pos, "cannot.assign.non.final",
                            TreeInfo.name(tree.meth));
                }

                attribExpr(tree.getTriggerReturn(), localEnv, triggertype);
            }


            // as a special case, array.clone() has a result that is
            // the same as static type of the array being cloned
            if (tree.meth.getTag() == JCTree.SELECT
                    && allowCovariantReturns
                    && methName == names.clone
                    && types.isArray(((JCFieldAccess) tree.meth).selected.type.getArrayType())) {
                restype = ((JCFieldAccess) tree.meth).selected.type;
            }

            // as a special case, x.getClass() has type Class<? extends |X|>
            if (allowGenerics
                    && methName == names.getClass && tree.args.isEmpty()) {
                Type qualifier = (tree.meth.getTag() == JCTree.SELECT)
                        ? ((JCFieldAccess) tree.meth).selected.type
                        : env.enclClass.sym.type;
                restype = new ClassType(restype.getEnclosingType(),
                        List.<Type>of(new WildcardType(types.erasure(qualifier),
                        BoundKind.EXTENDS,
                        syms.boundClass)),
                        restype.tsym);
            }

            if (methName == names.resize && restype.tag == TypeTags.DOMAIN) {
                Type t = ((JCFieldAccess) tree.meth).selected.type;
                //int i=0;
                restype = t;
                //restype.addFlag(FIN)
            }
            /*
             if(methName == names.split&&restype.tag==TypeTags.DOMAIN)
             {
             Type t=((JCFieldAccess)tree.meth).selected.type;
             //int i=0;
             restype=t;
             //restype.addFlag(FIN)
             }
             */
            // Check that value of resulting type is admissible in the
            // current context.  Also, capture the return type
            //tree.type = result;

            if ((ms.flags_field & Flags.LINEAR) != 0) {
                restype = restype.addFlag(Flags.LINEAR);
            }

            result = check(tree, capture(restype), VAL, pkind, pt);
        }

        chk.validate(tree.typeargs, localEnv);
    }
    //where

    /**
     * Check that given application node appears as first statement in a
     * constructor call.
     *
     * @param tree The application node
     * @param env The environment current at the application.
     */
    boolean checkFirstConstructorStat(JCMethodInvocation tree, Env<AttrContext> env) {
        JCMethodDecl enclMethod = env.enclMethod;
        if (enclMethod != null && enclMethod.name == names.init) {
            JCBlock body = enclMethod.body;
            if (body.stats.head.getTag() == JCTree.EXEC
                    && ((JCExpressionStatement) body.stats.head).expr == tree) {
                return true;
            }
        }
        log.error(tree.pos(), "call.must.be.first.stmt.in.ctor",
                TreeInfo.name(tree.meth));
        return false;
    }

    /**
     * Obtain a method type with given argument types.
     */
    Type newMethTemplate(List<Type> argtypes, List<Type> typeargtypes) {
        MethodType mt = new MethodType(argtypes, null, null, syms.methodClass);
        return (typeargtypes == null) ? mt : (Type) new ForAll(typeargtypes, mt);
    }

    public void visitNewClass(JCNewClass tree) {

        Type owntype = types.createErrorType(tree.type);

        // The local environment of a class creation is
        // a new environment nested in the current one.
        Env<AttrContext> localEnv = env.dup(tree, env.info.dup());

        // The anonymous inner class definition of the new expression,
        // if one is defined by it.
        JCClassDecl cdef = tree.def;

        // If enclosing class is given, attribute it, and
        // complete class name to be fully qualified
        JCExpression clazz = tree.clazz; // Class field following new
        JCExpression clazzid = // Identifier in class field
                (clazz.getTag() == JCTree.TYPEAPPLY)
                ? ((JCTypeApply) clazz).clazz
                : clazz;

        JCExpression clazzid1 = clazzid; // The same in fully qualified form

        if (tree.encl != null) {
            // We are seeing a qualified new, of the form
            //    <expr>.new C <...> (...) ...
            // In this case, we let clazz stand for the name of the
            // allocated class C prefixed with the type of the qualifier
            // expression, so that we can
            // resolve it with standard techniques later. I.e., if
            // <expr> has type T, then <expr>.new C <...> (...)
            // yields a clazz T.C.
            Type encltype = chk.checkRefType(tree.encl.pos(),
                    attribExpr(tree.encl, env));
            clazzid1 = make.at(clazz.pos).Select(make.Type(encltype),
                    ((JCIdent) clazzid));
            if (clazz.getTag() == JCTree.TYPEAPPLY) {
                clazz = make.at(tree.pos).
                        TypeApply(clazzid1,
                        ((JCTypeApply) clazz).arguments);
            } else {
                clazz = clazzid1;
            }
//          System.out.println(clazz + " generated.");//DEBUG
        }

        // Attribute clazz expression and store
        // symbol + type back into the attributed tree.
        Type clazztype = chk.checkClassType(
                tree.clazz.pos(), attribType(clazz, env), true);

        if (clazztype.isReadLinear()) {
            log.error(tree.pos, "new.volatile");
        }

        chk.validate(clazz, localEnv);
        if (tree.encl != null) {
            // We have to work in this case to store
            // symbol + type back into the attributed tree.
            tree.clazz.type = clazztype;
            TreeInfo.setSymbol(clazzid, TreeInfo.symbol(clazzid1));
            clazzid.type = ((JCIdent) clazzid).sym.type;
            if (!clazztype.isErroneous()) {
                if (cdef != null && clazztype.tsym.isInterface()) {
                    log.error(tree.encl.pos(), "anon.class.impl.intf.no.qual.for.new");
                } else if (clazztype.tsym.isStatic()) {
                    log.error(tree.encl.pos(), "qualified.new.of.static.class", clazztype.tsym);
                }
            }
        } else if (!clazztype.tsym.isInterface()
                && clazztype.getEnclosingType().tag == CLASS) {
            // Check for the existence of an apropos outer instance
            rs.resolveImplicitThis(tree.pos(), env, clazztype);
        }

        // Attribute constructor arguments.
        List<Type> argtypes = attribArgs(tree.args, localEnv);
        List<Type> typeargtypes = attribTypes(tree.typeargs, localEnv);

        // If we have made no mistakes in the class type...
        if (clazztype.tag == CLASS) {
            // Enums may not be instantiated except implicitly
            if (allowEnums
                    && (clazztype.tsym.flags_field & Flags.ENUM) != 0
                    && (env.tree.getTag() != JCTree.VARDEF
                    || (((JCVariableDecl) env.tree).mods.flags & Flags.ENUM) == 0
                    || ((JCVariableDecl) env.tree).init != tree)) {
                log.error(tree.pos(), "enum.cant.be.instantiated");
            }
            // Check that class is not abstract
            if (cdef == null
                    && (clazztype.tsym.flags() & (ABSTRACT | INTERFACE)) != 0) {
                log.error(tree.pos(), "abstract.cant.be.instantiated",
                        clazztype.tsym);
            } else if (cdef != null && clazztype.tsym.isInterface()) {
                // Check that no constructor arguments are given to
                // anonymous classes implementing an interface
                if (!argtypes.isEmpty()) {
                    log.error(tree.args.head.pos(), "anon.class.impl.intf.no.args");
                }

                if (!typeargtypes.isEmpty()) {
                    log.error(tree.typeargs.head.pos(), "anon.class.impl.intf.no.typeargs");
                }

                // Error recovery: pretend no arguments were supplied.
                argtypes = List.nil();
                typeargtypes = List.nil();
            } // Resolve the called constructor under the assumption
            // that we are referring to a superclass instance of the
            // current instance (JLS ???).
            else {
                localEnv.info.selectSuper = cdef != null;
                localEnv.info.varArgs = false;
                tree.constructor = rs.resolveConstructor(
                        tree.pos(), localEnv, clazztype, argtypes, typeargtypes);
                tree.constructorType = checkMethod(clazztype,
                        tree.constructor,
                        localEnv,
                        tree.args,
                        argtypes,
                        typeargtypes,
                        localEnv.info.varArgs);
                if (localEnv.info.varArgs) {
                    assert tree.constructorType.isErroneous() || tree.varargsElement != null;
                }
            }

            if (cdef != null) {
                // We are seeing an anonymous class instance creation.
                // In this case, the class instance creation
                // expression
                //
                //    E.new <typeargs1>C<typargs2>(args) { ... }
                //
                // is represented internally as
                //
                //    E . new <typeargs1>C<typargs2>(args) ( class <empty-name> { ... } )  .
                //
                // This expression is then *transformed* as follows:
                //
                // (1) add a STATIC flag to the class definition
                //     if the current environment is static
                // (2) add an extends or implements clause
                // (3) add a constructor.
                //
                // For instance, if C is a class, and ET is the type of E,
                // the expression
                //
                //    E.new <typeargs1>C<typargs2>(args) { ... }
                //
                // is translated to (where X is a fresh name and typarams is the
                // parameter list of the super constructor):
                //
                //   new <typeargs1>X(<*nullchk*>E, args) where
                //     X extends C<typargs2> {
                //       <typarams> X(ET e, args) {
                //         e.<typeargs1>super(args)
                //       }
                //       ...
                //     }
                if (Resolve.isStatic(env)) {
                    cdef.mods.flags |= STATIC;
                }

                if (clazztype.tsym.isInterface()) {
                    cdef.implementing = List.of(clazz);
                } else {
                    cdef.extending = clazz;
                }

                attribStat(cdef, localEnv);

                // If an outer instance is given,
                // prefix it to the constructor arguments
                // and delete it from the new expression
                if (tree.encl != null && !clazztype.tsym.isInterface()) {
                    tree.args = tree.args.prepend(makeNullCheck(tree.encl));
                    argtypes = argtypes.prepend(tree.encl.type);
                    tree.encl = null;
                }

                // Reassign clazztype and recompute constructor.
                clazztype = cdef.sym.type;
                Symbol sym = rs.resolveConstructor(
                        tree.pos(), localEnv, clazztype, argtypes,
                        typeargtypes, true, tree.varargsElement != null);
                assert sym.kind < AMBIGUOUS || tree.constructor.type.isErroneous();
                tree.constructor = sym;
                tree.constructorType = checkMethod(clazztype,
                        tree.constructor,
                        localEnv,
                        tree.args,
                        argtypes,
                        typeargtypes,
                        localEnv.info.varArgs);
            }

            if (tree.constructor != null && tree.constructor.kind == MTH) {
                owntype = clazztype;
            }
        }

        if (owntype.getArrayType() != null) {
            owntype = owntype.addFlag(Flags.LINEAR);//new array sare linear
        }
        Type ptype = owntype;//.addFlag(Flags.FOUT);

        if (!ptype.isPrimitive() && ptype.tag == TypeTags.CLASS) {
            env.enclClass.sym.recAddRef(ptype, false);
        }


        result = check(tree, ptype, VAL, pkind, pt);

        if (result != null && (result.tsym.flags_field & Flags.PACKED) != 0) {
            log.error(tree.pos(), "new.packed", result);
        }
        chk.validate(tree.typeargs, localEnv);
    }

    /**
     * Make an attributed null check tree.
     */
    public JCExpression makeNullCheck(JCExpression arg) {
        // optimization: X.this is never null; skip null check
        Name name = TreeInfo.name(arg);
        if (name == names._this || name == names._super) {
            return arg;
        }

        int optag = JCTree.NULLCHK;
        JCUnary tree = make.at(arg.pos).Unary(optag, arg);
        tree.operator = syms.nullcheck;
        tree.type = arg.type;
        return tree;
    }

    public void visitNewArray(JCNewArray tree) {
        Type owntype = types.createErrorType(tree.type);
        Type elemtype;

        if (tree.elemtype != null && !tree.dom.domparams.isEmpty()) {
            Type dom = attribType(tree.dom, env);
            if (dom instanceof DomainType && !((DomainType) dom).isBaseDomain) {
                log.error(tree.pos(), "domain.not.a.base.domain", dom);
            }
            elemtype = attribType(tree.elemtype, env);

			boolean fromConstruct=false;
            for (JCExpression e : ((DomainType) dom).appliedParams) {
                if (e.type.constValue() instanceof Integer) {
                    if ((Integer) e.type.constValue() == -1) {
						if(constructExp==null)
							log.error(tree.pos(), "invalid.dyn.domain.size", dom);
						else
							fromConstruct=true;
                    }
                }
            }

			if(fromConstruct)
			{
				//dom=(DomainType)dom.clone();
				ListBuffer<JCExpression> pars= new ListBuffer<JCExpression>();
				for(int d=0;d<tree.dom.domparams.size();d++)
				{
					ListBuffer<JCExpression> lb=new ListBuffer<JCExpression>();
					lb.add(make.Literal(d));
					JCExpression e=make.Indexed(make.Select(constructExp, make.Ident(names.fromString("size"))),lb.toList());
					pars.add(e);
					attribExpr(e, env, syms.intType);
				}
				tree.dom.domparams=pars.toList();
				attribType(tree.dom, env);
			}

            if (!elemtype.isPrimitive() && elemtype.tag == TypeTags.CLASS) {
                env.enclClass.sym.recAddRef(elemtype, false);
            }

            chk.validate(tree.elemtype, env);
            owntype = elemtype;
            for (List<JCExpression> l = tree.dims; l.nonEmpty(); l = l.tail) {
                attribExpr(l.head, env, syms.intType);
                owntype = new ArrayType(owntype, (DomainType) dom, syms.arrayClass);
            }
        } else if (tree.elemtype != null) {
            elemtype = attribType(tree.elemtype, env);
            chk.validate(tree.elemtype, env);
            owntype = elemtype;

            elemtype = attribType(tree.elemtype, env);
            chk.validate(tree.elemtype, env);
            owntype = elemtype;
            for (List<JCExpression> l = tree.dims; l.nonEmpty(); l = l.tail) {
                attribExpr(l.head, env, syms.intType);
                owntype = new ArrayType(owntype, null, syms.arrayClass);
            }
        } else {
            // we are seeing an untyped aggregate { ... }
            // this is allowed only if the prototype is an array
            if (pt.tag == ARRAY) {
                elemtype = types.elemtype(pt);
            } else {
                if (pt.tag != ERROR) {
                    log.error(tree.pos(), "illegal.initializer.for.type",
                            pt);
                }
                elemtype = types.createErrorType(pt);
            }

        }
        if (tree.elems != null) {
            attribExprs(tree.elems, env, elemtype);
            DomainType dom = ((ArrayType) pt).dom;
            if (dom.tsym != syms.one_d) {
                log.error(tree.pos(), "array.initializer.only.for.one_d", dom);
            }
            if ((Integer) dom.appliedParams.get(0).type.constValue() != tree.elems.length()) {
				if((Integer) dom.appliedParams.get(0).type.constValue()!=-1)//dynamic is ok!
					log.error(tree.pos(), "array.initializer.wrong.size", dom.appliedParams.get(0), tree.elems.length());
            }
            owntype = new ArrayType(elemtype, dom, syms.arrayClass);
        }
        /*
         if (!types.isReifiable(elemtype))
         log.error(tree.pos(), "generic.array.creation");
         */

        owntype = owntype.addFlag(Flags.LINEAR);//new array sare linear

        result = check(tree, owntype, VAL, pkind, pt);
    }

    public void visitParens(JCParens tree) {
        Type owntype = attribTree(tree.expr, env, pkind, pt);
        result = check(tree, owntype, pkind, pkind, pt);
        Symbol sym = TreeInfo.symbol(tree);
        //FIXME: what is this test good for??
        if (false && sym != null && (sym.kind & (TYP | PCK)) != 0) {
            log.error(tree.pos(), "illegal.start.of.type");
        }
    }

    public void visitAssign(JCAssign tree) {

        //FIXME: out args missing, -> FLOW
        Type owntype = attribTree(tree.lhs, env.dup(tree), VAR, Type.noType);

        Type capturedType = capture(owntype);
        Type rhs=attribExpr(tree.rhs, env, owntype);

        if (tree.lhs.type.isLinear()) {
            if (!tree.lhs.type.isPrimitive() && tree.rhs.type != null && !tree.rhs.type.isConst() && (tree.rhs.type.type_flags_field & Flags.LINEAR) == 0) {
                if (tree.lhs.type.getArrayType().tag != TypeTags.ARRAY || (tree.rhs.getTag() != JCTree.NEWARRAY && tree.rhs.getTag() != JCTree.NEWCLASS)) {
                    log.error(tree.pos, "copy.unique", tree.lhs, tree.rhs);
                }
            }
        }

        result = check(tree, rhs, VAL, pkind, capturedType);

        if (tree.cond != null && tree.aflags != Flags.ATOMIC)//exp?exp2=exp3 returns true if atomic cmp&swap succeeded
        {
            result = syms.booleanType;
        }

        if (env.enclMethod.name == names.init) //mark construction of member vals
        {
            //Symbol s=((JCSymbolExpression)tree.lhs).getSymbol();
            Symbol s = TreeInfo.symbol(tree.lhs);

            if (s.owner == env.enclClass.sym) {
                if (!tree.lhs.type.isPointer() && !tree.lhs.type.isPrimitive()) {
                    if (env.enclMethod.init_constructors == null) {
                        env.enclMethod.init_constructors = new ListBuffer<JCAssign>();
                    }
                    env.enclMethod.init_constructors.add(tree);
                    statement_removed = true;
                }
            }
        }
    }

    public void visitAssignop(JCAssignOp tree) {
        // Attribute arguments.
        Type owntype = attribTree(tree.lhs, env, VAR, Type.noType);
        Type operand = attribExpr(tree.rhs, env);
        // Find operator.
        Symbol operator = tree.operator = rs.resolveBinaryOperator(
                tree.pos(), tree.getTag() - JCTree.ASGOffset, env,
                owntype, operand);

        if (operator.kind == MTH) {
            chk.checkOperator(tree.pos(),
                    (OperatorSymbol) operator,
                    tree.getTag() - JCTree.ASGOffset,
                    owntype,
                    operand);
            chk.checkDivZero(tree.rhs.pos(), operator, operand);
            chk.checkCastable(tree.rhs.pos(),
                    operator.type.getReturnType(),
                    owntype);
        }
        result = check(tree, owntype, VAL, pkind, pt);
    }

    public void visitUnary(JCUnary tree) {
        // Attribute arguments.
        Type argtype = (JCTree.PREINC <= tree.getTag() && tree.getTag() <= JCTree.POSTDEC)
                ? attribTree(tree.arg, env, VAR, Type.noType)
                : chk.checkNonVoid(tree.arg.pos(), attribExpr(tree.arg, env));

        // Find operator.
        Symbol operator = tree.operator =
                rs.resolveUnaryOperator(tree.pos(), tree.getTag(), env, argtype);

        Type owntype = types.createErrorType(tree.type);

        if (argtype.tag == TypeTags.CLASS)//we need type info so we cannot do this in DesugarSyntax pass
        {
            if (tree.arg.type.tag == TypeTags.ARRAY) {
                ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
                //args.add(make.ArgExpression(tree.arg,null));
                //JCExpression tp = make.Type(tree.arg.type);
                tree.apply = make.Apply(new ListBuffer<JCExpression>().toList(), make.Select(tree.arg, make.Ident(names.fromString("operator" + operator.name.toString()))), args.toList());
                tree.type = result = attribExpr(tree.apply, env);
            } else {
                ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
                tree.apply = make.Apply(new ListBuffer<JCExpression>().toList(), make.Select(tree.arg, make.Ident(names.fromString("operator" + operator.name.toString()))), args.toList());
                tree.type = result = attribExpr(tree.apply, env);
            }
            return;
        }


        if (operator.kind == MTH) {
            owntype = (JCTree.PREINC <= tree.getTag() && tree.getTag() <= JCTree.POSTDEC)
                    ? tree.arg.type
                    : operator.type.getReturnType();
            int opc = ((OperatorSymbol) operator).opcode;

            // If the argument is constant, fold it.
            if (argtype.constValue() != null) {
                Type ctype = cfolder.fold1(opc, argtype);
                if (ctype != null) {
                    owntype = cfolder.coerce(ctype, owntype);

                    // Remove constant types from arguments to
                    // conserve space. The parser will fold concatenations
                    // of string literals; the code here also
                    // gets rid of intermediate results when some of the
                    // operands are constant identifiers.
                    if (tree.arg.type.tsym == syms.stringType.tsym) {
                        tree.arg.type = syms.stringType;
                    }
                }
            }
        }
        result = check(tree, owntype, VAL, pkind, pt);
    }

    public void visitBinary(JCBinary tree) {
        // Attribute arguments.

        if (tree.getTag() == JCTree.SEQ) {
            attribExpr(tree.lhs, env);
            attribExpr(tree.rhs, env);
            result = tree.type = Type.noType;

            return;
        }

        Type left = chk.checkNonVoid(tree.lhs.pos(), attribExpr(tree.lhs, env));
		Type origLeft=left;


        Type right = chk.checkNonVoid(tree.lhs.pos(), attribExpr(tree.rhs, env));

		if(left.tag==TypeTags.TYPEVAR)
			left=right;

        // Find operator.
        Symbol operator = tree.operator =
                rs.resolveBinaryOperator(tree.pos(), tree.getTag(), env, left, right);

        if (left.tag == TypeTags.CLASS && (tree.getTag() == JCTree.PLUS || tree.getTag() == JCTree.MINUS || tree.getTag() == JCTree.MUL || tree.getTag() == JCTree.DIV || tree.getTag() == JCTree.MOD || tree.getTag() == JCTree.COMPL))//we need type info so we cannot do this in DesugarSyntax pass
        {
            if (tree.lhs.type.tag == TypeTags.ARRAY) {
                //translate array operatorY X into array.operatorY(array,X)
                ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
                //args.add(make.ArgExpression(tree.lhs,null));
                args.add(make.ArgExpression(tree.rhs, null));
                //JCExpression tp = make.Type(tree.lhs.type);
                tree.apply = make.Apply(new ListBuffer<JCExpression>().toList(), make.Select(tree.lhs, make.Ident(names.fromString("operator" + operator.name.toString()))), args.toList());
                tree.type = result = attribExpr(tree.apply, env);
            } else {
                //translate class operatorY X into class.operatorY(X)
                ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
                args.add(make.ArgExpression(tree.rhs, null));
                tree.apply = make.Apply(new ListBuffer<JCExpression>().toList(), make.Select(tree.lhs, make.Ident(names.fromString("operator" + operator.name.toString()))), args.toList());
                tree.type = result = attribExpr(tree.apply, env);
            }
            //tree=tree.apply is performed in PostAttrDesugar (because Attrs is only a Visitor rather than a Translator)

            return;
        }

        Type owntype = types.createErrorType(tree.type);
        if (operator.kind == MTH) {
            owntype = operator.type.getReturnType();
            int opc = chk.checkOperator(tree.lhs.pos(),
                    (OperatorSymbol) operator,
                    tree.getTag(),
                    left,
                    right);

            // If both arguments are constants, fold them.
            if (left.constValue() != null && right.constValue() != null) {
                Type ctype = cfolder.fold2(opc, left, right);
                if (ctype != null) {
                    owntype = cfolder.coerce(ctype, owntype);

                    // Remove constant types from arguments to
                    // conserve space. The parser will fold concatenations
                    // of string literals; the code here also
                    // gets rid of intermediate results when some of the
                    // operands are constant identifiers.
                    if (tree.lhs.type.tsym == syms.stringType.tsym) {
                        tree.lhs.type = syms.stringType;
                    }
                    if (tree.rhs.type.tsym == syms.stringType.tsym) {
                        tree.rhs.type = syms.stringType;
                    }
                }
            }

            // Check that argument types of a reference ==, != are
            // castable to each other, (JLS???).
            if ((opc == ByteCodes.if_acmpeq || opc == ByteCodes.if_acmpne)) {
                if (!types.isCastable(left, right, new Warner(tree.pos()))) {
                    log.error(tree.pos(), "incomparable.types", left, right);
                }
            }

            chk.checkDivZero(tree.rhs.pos(), operator, right);
        }

		if(origLeft.tag==TypeTags.TYPEVAR)
			owntype = tree.type = result = origLeft;

        result = check(tree, owntype, VAL, pkind, pt);


    }

    public void visitTypeCast(JCTypeCast tree) {
        Type clazztype = attribType(tree.clazz, env);
        chk.validate(tree.clazz, env);
        Type exprtype = attribExpr(tree.expr, env, Infer.anyPoly);

        clazztype = clazztype.addFlag(exprtype.type_flags_field & Flags.LINEAR);//retain linearity of casted type

        if (clazztype.tag == TypeTags.ARRAY && exprtype.tag == TypeTags.ARRAY && ((ArrayType) clazztype).dom != null && ((ArrayType) exprtype).dom != null) {
            log.error(tree.pos(), "no.domain.cast", tree);
        }

        Type owntype = chk.checkCastable(tree.expr.pos(), exprtype, clazztype);
        if (exprtype.constValue() != null) {
            owntype = cfolder.coerce(exprtype, owntype);
        }

        if (!owntype.isPrimitive() && owntype.tag == TypeTags.CLASS) {
            env.enclClass.sym.recAddRef(owntype, false);
        }

        //all non primitive vars that are not member vars of a class are pointers!
		/*
         if(!owntype.isPrimitive())
         {
         owntype=owntype.addFlag(Flags.FOUT);
         }
         */

        result = check(tree, capture(owntype), VAL, pkind, pt);

        if (result.tsym instanceof ClassSymbol) {
            env.enclClass.sym.recAddRef(result, false);
        }
    }

    public void visitTypeTest(JCInstanceOf tree) {
        Type exprtype = chk.checkNullOrRefType(
                tree.expr.pos(), attribExpr(tree.expr, env));
        Type clazztype = chk.checkReifiableReferenceType(
                tree.clazz.pos(), attribType(tree.clazz, env));
        chk.validate(tree.clazz, env);
        chk.checkCastable(tree.expr.pos(), exprtype, clazztype);
        result = check(tree, syms.booleanType, VAL, pkind, pt);
    }

    public void visitIndexed(JCArrayAccess tree) {
        // array with indices (e.g. arr[1,2])

        Type owntype = types.createErrorType(tree.type);

        // attribute array expression
        boolean oldAccess = insideAccess;
        insideAccess = true;
        Type atype = attribTree(tree.indexed, env, VAL, Type.noType);
        insideAccess = oldAccess;

        atype = atype.getArrayType();
        
        if((atype.type_flags_field&Flags.WORK)!=0)
            log.error(tree.pos, "array.access.out.of.bounds", tree,atype); //FIXME error msg

		if(domiter!=null)
		{
			//Symbol s=TreeInfo.rootSymbol(domiter.exp);
			if(tree.indexed.type.getArrayType().tag!=TypeTags.ARRAY)
			{
				tree.type=result=owntype;
				return;
			}

			{
				Type.ArrayType mem=(Type.ArrayType)tree.indexed.type.getArrayType();
				boolean isReduce=domiter.sym.name.toString().equals("reduce");
				//Type.ArrayType bt=(Type.ArrayType)at.getBaseType().getArrayType();

				Type.ArrayType range=(Type.ArrayType)domiter.exp.type.getArrayType();

				Type.DomainType.AccessSecurity as=range.dom.safeAccess(tree.pos(),lastProjectionArgs,env.enclMethod,domiter.domargs,isReduce,range,mem,tree.index,true,false);
				//FIXME: currently broken check:

				if(jc.verifyArrays)
				{
					if(as==Type.DomainType.AccessSecurity.Error)
					{
						log.error(tree.pos, "array.access.out.of.bounds", tree,mem.getBaseType());
						range.dom.safeAccess(tree.pos(),lastProjectionArgs,env.enclMethod,domiter.domargs,isReduce,range,mem,tree.index,true,false);
					}
					if(as==Type.DomainType.AccessSecurity.Unknown)
					{
						log.warning(tree.pos, "array.access.out.of.bounds",tree,mem.getBaseType());
						range.dom.safeAccess(tree.pos(),lastProjectionArgs,env.enclMethod,domiter.domargs,isReduce,range,mem,tree.index,true,false);
					}
				}
			}
		}

        // attribute indices and ensure that they are integer expressions
        // also get constant indices
        // (javac ListBuffer cannot contain null !!!)
        java.util.ArrayList<Integer> constants = new java.util.ArrayList<Integer>();
        for (JCExpression e : tree.index) {
            Type t = attribExpr(e, env, syms.longType);
            Object constVal = t.constValue();
            if (constVal != null && constVal instanceof Integer) {
                constants.add((Integer) constVal);
            } else {
                constants.add(null); // unknown value
            }
        }

        // ensure that indexed value is an array
        if (types.isArray(atype)) {
			ArrayType site=((ArrayType)atype);
			ArrayType base=site.getStartType();
			if(base.dom!=null&&base.dom.parentDomain!=null&&site.dom.parentDomain!=null)
				 log.error(tree.pos(), "projection.direct.access", site.dom);


            ArrayType at = (ArrayType) atype;

            if (at.dom == null) {
                log.error(tree.pos(), "native.array.access");

                return;
            }

            // test that the right number or indices was supplied
            if (tree.index.size() != at.dom.getInterVectorOrder(tree.pos()).size()) {

                log.error(tree.pos(), "domain.wrong.number.of.indices.req.found",
                        at.dom, at.dom.getInterVectorOrder(tree.pos()).size(), tree.index.size());

            } else {

                // we cannot check for all indices if they are valid under
                // the constraints of the domain, however we can check a least
                // those that are known at compile time
                Map<Name, JCExpression> varenv = new LinkedHashMap<Name, JCExpression>();
                // add values of parameters
                for (int i = 0; i < at.dom.formalParams.length(); i++) {
                    varenv.put(at.dom.formalParams.get(i).name, at.dom.appliedParams.get(i));
                }
                // add values of indices, if they are known at compile-time
                for (int i = 0; i < at.dom.getInterVectorOrder(tree.pos()).size(); i++) {
                    if (constants.get(i) != null) {
                        int c = constants.get(i);
                        if (c < 0) {
                            log.error(tree.pos(), "domain.negative.index", c);
                        }
                        varenv.put(at.dom.indices.get(i).name, make.Literal(c));
                    }
                }
                // ensure that no constraint is violated
                if (!at.dom.tsym.name.toString().equals("size") && !at.dom.isDynamic() && at.dom.constraints != null) {
                    for (DomainConstraint c : at.dom.constraints) {
                        if (!Domains.checkConstraint(c, varenv)) {
                            log.error(tree.pos(), "domain.constraint.violated", at, c);
                        }
                    }
                }

            }

            // get result type of array
            owntype = at.elemtype;

        } else if (atype.tag != ERROR) {
            //if(!atype.toString().equals("String"))
                log.error(tree.pos(), "array.req.but.found", atype);
            /*    
            else
            {
                owntype = syms.charType;
            }
            */    
        }

        // check types
        if ((pkind & VAR) == 0) {
            owntype = capture(owntype);
        }
        result = check(tree, owntype, VAR, pkind, pt);

    }

    public void visitSizeOf(JCSizeOf tree) {
        attribTree(tree.expr, env, VAL | TYP, Type.noType);
        result = tree.type = syms.longType;
    }

    public void visitIdent(JCIdent tree) {
//ALEX!!!!

        Symbol sym;
        boolean varArgs = false;

        // Find symbol
        if (pt.tag == METHOD || pt.tag == FORALL) {
            // If we are looking for a method, the prototype `pt' will be a
            // method type with the type of the call's arguments as parameters.
            env.info.varArgs = false;
            sym = rs.resolveMethod(tree.pos(), env, tree.name, pt.getParameterTypes(), pt.getTypeArguments());
            varArgs = env.info.varArgs;
        } else if (tree.sym != null && tree.sym.kind != VAR) {
            sym = tree.sym;
        } else {
			//
			//insideVarDecl!=null&&insideDomainParams>=0
			sym = rs.resolveIdent(tree.pos(), env, tree.name, pkind,insideVarDecl,insideDomainParams,domParamTree);
/*
			if(allowImplicit)
			{
				Env<AttrContext> methEnv=env;
				while(methEnv.next!=null&&methEnv.tree!=env.enclMethod.body)
					methEnv=methEnv.next;

				Scope enclScope = enter.enterScope(methEnv);

				VarSymbol v=new VarSymbol();
				v.flags_field|=Flags.IMPLICITDECL;
				v.flags_field&=~Flags.PARAMETER;
				if(v.type.isPrimitive())
				{
					v.type=(Type)v.type.clone();
					v.type.type_flags_field&=~Flags.FOUT;
				}

				if (chk.checkUnique(tree.pos(), v, enclScope)) {
					chk.checkTransparentVar(tree.pos(), v, enclScope);
					enclScope.enter(v);
				}
				env.enclMethod.implicitSyms.add(v);
				v.pos = tree.pos().getStartPosition();

			}
*/
			if(tree.name.equals(names._super)&&env.enclClass.sym.type.getArrayType().tag == TypeTags.ARRAY)
			{
				sym.type=env.enclClass.extending.type;
			}
        }

        tree.sym = sym;

        // (1) Also find the environment current for the class where
        //     sym is defined (`symEnv').
        // Only for pre-tiger versions (1.4 and earlier):
        // (2) Also determine whether we access symbol out of an anonymous
        //     class in a this or super call.  This is illegal for instance
        //     members since such classes don't carry a this$n link.
        //     (`noOuterThisPath').
        boolean noOuterThisPath = false;
        Env<AttrContext> symEnv = env;

        if (env.enclClass.sym.owner.kind != PCK && // we are in an inner class
                (sym.kind & (VAR | MTH | TYP)) != 0
                && sym.owner.kind == TYP
                && tree.name != names._this && tree.name != names._super) {

            // Find environment in which identifier is defined.
            while (symEnv.outer != null
                    && !sym.isMemberOf(symEnv.enclClass.sym, types)) {
                if ((symEnv.enclClass.sym.flags() & NOOUTERTHIS) != 0) {
                    noOuterThisPath = !allowAnonOuterThis;
                }
                symEnv = symEnv.outer;
            }
        }

        // If symbol is a variable, ...
        if (sym.kind == VAR) {
            VarSymbol v = (VarSymbol) sym;

            // ..., evaluate its initializer, if it has one, and check for
            // illegal forward reference.
            checkInit(tree, env, v, false);

            // If symbol is a local variable accessed from an embedded
            // inner class check that it is final.
            if (v.owner.kind == MTH
                    && v.owner != env.info.scope.owner
                    && (v.flags_field & FINAL) == 0 && !allowModify) {
                //ALEX allow acces to outer defs
                if (env.info.scope.owner.kind != (DOM | TYP) || env.info.scope.owner.type.tag != DOMAIN) {
                    log.error(tree.pos(),
                            "local.var.accessed.from.icls.needs.final",
                            v);
                }
            }

            // If we are expecting a variable (as opposed to a value), check
            // that the variable is assignable in the current environment.
            if (pkind == VAR) {
                checkAssignable(tree.pos(), v, null, env);
            }
        }

        // In a constructor body,
        // if symbol is a field or instance method, check that it is
        // not accessed before the supertype constructor is called.
        if ((symEnv.info.isSelfCall || noOuterThisPath)
                && (sym.kind & (VAR | MTH)) != 0
                && sym.owner.kind == TYP
                && (sym.flags() & STATIC) == 0) {
            chk.earlyRefError(tree.pos(), sym.kind == VAR ? sym : thisSym(tree.pos(), env));
        }
        Env<AttrContext> env1 = env;
        if (sym.kind != ERR && sym.kind != TYP && sym.owner != null && sym.owner != env1.enclClass.sym) {
            // If the found symbol is inaccessible, then it is
            // accessed through an enclosing instance.  Locate this
            // enclosing instance:
            while (env1.outer != null && !rs.isAccessible(env, env1.enclClass.sym.type, sym)) {
                env1 = env1.outer;
            }
        }
        result = checkId(tree, env1.enclClass.sym.type, sym, env, pkind, pt, varArgs);

		if(env.enclMethod!=null&&env.enclMethod.constraintsDeps!=null&&inside_cond!=null)
		{
			Symbol s=tree.sym;

			if(env.enclMethod.constraintsSyms.get(s)!=null||s.type.tag==TypeTags.INT)
			{
				VarSymbol vs=(VarSymbol)s;
				Set<JCExpression> set=env.enclMethod.constraintsDeps.get(vs);

				if(set==null)
					set=new LinkedHashSet<JCExpression>();
				set.add(inside_cond);
				env.enclMethod.constraintsDeps.put(vs, set);
			}
/*
			else
			{
				VarSymbol vs=(VarSymbol)s;
				Set<JCExpression> set=env.enclMethod.constraintsDeps.get(vs);

				if(set==null)
					set=new LinkedHashSet<JCExpression>();
				set.add(inside_cond);
				env.enclMethod.constraintsDeps.put(vs, set);

			}
*/
		}
    }

    <T extends JCTree> T replaceSymbol(T cu, final Map<VarSymbol, JCExpression> map, boolean deepcopy) {
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
				JCExpression e=map.get(tree.sym);
				if(e!=null)
					result=e;
				else
					result=tree;
			}
		}
        Replace v = new Replace();
        if (deepcopy) {
            cu = (T) copy.copy(cu);
        }

		if(map==null)
			return cu;
        return v.translate(cu);
	}


    <T extends JCTree> T replace(T cu, final Map<String, Object> map, boolean deepcopy) {
        class Replace extends TreeTranslator {

            Map<Symbol, Symbol> syms = new LinkedHashMap<Symbol, Symbol>();
			boolean isClass=false;

            public <T extends JCTree> T translate(T tree) {
                if (tree == null) {
                    return null;
                } else {
                    make.at(tree.pos());
                    T result = super.translate(tree);
                    return result;
                }
            }
            JCVariableDecl current_decl = null;

            void apply(JCTree tree, Name name) {
                Object sym = map.get(name.toString());
                if (sym != null) {
                    if (sym instanceof Integer) {
                        result = make.Literal(sym);
                    } else if (sym instanceof Type) {
                        if (((Type) sym).isPrimitive()) {
                            result = make.Type((Type) sym);
                        } else {
                            result = make.Ident(((Type) sym).tsym.name);
                        }
                    } else if (sym instanceof JCExpression) {
                        JCExpression e = (JCExpression) sym;
                        result = e;

                    }
                } else {
                    result = tree;
                }
            }

            public void visitTypeParameter(JCTypeParameter tree) {
                super.visitTypeParameter(tree);
                apply(tree, tree.name);
            }

			public void visitClassDef(JCClassDecl cd)
			{
				isClass=true;
				super.visitClassDef(cd);
				isClass=false;
			}

			public void visitMethodDef(JCMethodDecl tree) {

				if(!isClass)
				{
					super.visitMethodDef(tree);
					return;
				}

				tree.mods = translate(tree.mods);
				tree.restype = translate(tree.restype);

				ListBuffer<JCTypeParameter> ops=new ListBuffer<JCTypeParameter>();
				for(JCTypeParameter tp:tree.typarams)
					ops.add(tp);
				List<JCTypeParameter> ntyparams = ops.toList();
				ntyparams = translateTypeParams(ntyparams);

				List<JCTypeParameter> otyparams = tree.typarams; //do not touch these!

				while(otyparams!=null)
				{
					if(ntyparams.head!=otyparams.head)
					{
						log.error(tree.pos(),"duplicate.typepramater", otyparams.head);
						break;
					}

					ntyparams=ntyparams.tail;
					otyparams=otyparams.tail;
				}

				//tree.typarams = otyparams;

				tree.params = translateVarDefs(tree.params);
				tree.thrown = translate(tree.thrown);
				tree.body = translate(tree.body);
				result = tree;
			}

            public void visitVarDef(JCVariableDecl tree) {
                current_decl = tree;
                super.visitVarDef(tree);
                current_decl = null;
            }

            public void visitApply(JCMethodInvocation tree) {
                ListBuffer<JCExpression> buf = new ListBuffer<JCExpression>();

                for (JCExpression e : tree.typeargs) {
                    apply(e, TreeInfo.name(e));
                    buf.add((JCExpression) result);
                }

                tree.typeargs = buf.toList();
                super.visitApply(tree);
            }

            public void visitIdent(JCTree.JCIdent tree) {
                apply(tree, tree.name);
            }
        }
        Replace v = new Replace();
        if (deepcopy) {
            cu = (T) copy.copy(cu);
        }

        return v.translate(cu);
    }

    public void visitSelect(JCFieldAccess tree) {
        // Determine the expected kind of the qualifier expression.
        int skind = 0;
        if (tree.getIdentifier() == names._this || tree.getIdentifier() == names._super
                || tree.getIdentifier() == names._class) {
            skind = TYP;
        } else {
            if ((pkind & PCK) != 0) {
                skind = skind | PCK;
            }
            if ((pkind & TYP) != 0) {
                skind = skind | TYP | PCK;
            }
            if ((pkind & (VAL | MTH)) != 0) {
                skind = skind | VAL | TYP;
            }
        }

        // Attribute the qualifier expression, and determine its symbol (if any).
        boolean old_select = inside_select;
        inside_select = true;
		boolean oldInside=insideMethodAttrib;

		insideMethodAttrib=false;

        Type site = attribTree(tree.selected, env, skind, Infer.anyPoly);
        
        insideMethodAttrib=oldInside;
        
        inside_select = old_select;
        boolean errArrayMeth=false;
        Symbol arrayMeth=null;
        if (site.getArrayType().tag == TypeTags.ARRAY) {
            //must fix first argument for method calls on arrays (we translate array op (args) into type(array).op(array,arg) as a static call
            //
            if ((pt.tag == TypeTags.FORALL && (((ForAll) pt).qtype).tag == TypeTags.METHOD)) {

				if(!(TreeInfo.symbol(tree.selected) instanceof ClassSymbol)) //only if the trafo was not yet applied!
				{
					Name name = tree.getIdentifier();

					//check that we're not seeing an interface (reduce,size,resize)
					boolean is_interface = name.equals(names.resize);
					for (Type t : ((ArrayType) site.getArrayType()).dom.interfaces_field) {
						if (t.tsym.name.equals(name)) {
							is_interface = true;
							break;
						}
					}

					if (!is_interface) {
						//first try method in class extending array:
						//we actually look for something with an additional first arg
						((MethodType) ((ForAll) pt).qtype).argtypes = ((MethodType) ((ForAll) pt).qtype).argtypes.prepend(site);
						//also, we are supplying the left part of left.method() as argument: left.method(left):
						((JCMethodInvocation) env.tree).args = ((JCMethodInvocation) env.tree).args.prepend(make.ArgExpression(tree.selected, null));
						//finally, the selected part becomes static: type(left).method(left)
						JCExpression selected = tree.selected;

						tree.selected = make.Type(site);

						arrayMeth = rs.findQualifiedMethod(tree.pos(), env, site, tree.getIdentifier(), pt.getParameterTypes(), pt.getTypeArguments(),false);
                        Symbol s=arrayMeth;

						if (s.type.isErroneous()) {//if no such method exists..restore old values so that domain can be found
							tree.selected = selected;
                            errArrayMeth=true;
                            ((MethodType) ((ForAll) pt).qtype).argtypes=((MethodType) ((ForAll) pt).qtype).argtypes.tail;
                            ((JCMethodInvocation) env.tree).args=((JCMethodInvocation) env.tree).args.tail;
						}
					}
				}
            }
        }



        if ((pkind & (PCK | TYP)) == 0) {
            site = capture(site); // Capture field access
        }
        // don't allow T.class T[].class, etc
        if (skind == TYP) {
            Type elt = site;
            while (elt.tag == ARRAY) {
                elt = ((ArrayType) elt).elemtype;
            }
            if (elt.tag == TYPEVAR) {
                log.error(tree.pos(), "type.var.cant.be.deref");
                result = types.createErrorType(tree.type);
                return;
            }
        }

        // If qualifier symbol is a type or `super', assert `selectSuper'
        // for the selection. This is relevant for determining whether
        // protected symbols are accessible.
        Symbol sitesym = TreeInfo.symbol(tree.selected);
        boolean selectSuperPrev = env.info.selectSuper;
        env.info.selectSuper =
                sitesym != null
                && sitesym.name == names._super;

        // If selected expression is polymorphic, strip
        // type parameters and remember in env.info.tvars, so that
        // they can be added later (in Attr.checkId and Infer.instantiateMethod).
        if (tree.selected.type.tag == FORALL) {
            ForAll pstype = (ForAll) tree.selected.type;
            env.info.tvars = pstype.tvars;
            site = tree.selected.type = pstype.qtype;
        }

        // Determine the symbol represented by the selection.
        env.info.varArgs = false;
        Symbol sym = selectSym(tree, site, env, pt, pkind | DOM);

        boolean allowarray = tree.selected.type.tag == TypeTags.CLASS && ((Type.ClassType) tree.selected.type).getArrayType().tag == TypeTags.ARRAY && (sym.type.tag == TypeTags.METHOD || sym.type.tag == TypeTags.FORALL);
        if (!site.isPrimitive() && site.tag == TypeTags.CLASS) {
            env.enclClass.sym.recAddRef(site, allowarray);
        }

        // domain operations have to be handled seperately
        boolean isDomainProjection=false;
		/*
				= (site.getSafeUpperBound().getArrayType().tag == TypeTags.ARRAY)
                && (sym instanceof DomainSymbol)
                && (env.tree instanceof JCMethodInvocation)
                && (((JCMethodInvocation) env.tree).meth == tree);
		*/

        boolean isDomainCast=false;
		/*
		= (site.getSafeUpperBound().getArrayType().tag == TypeTags.ARRAY)
                && (sym instanceof DomainSymbol)
                && !isDomainProjection;
		*/
        //&& ! ((Type.ArrayType)site).dom.isDynamic();//dynamic stuff is always just reinterpreted

		if((site.getSafeUpperBound().getArrayType().tag == TypeTags.ARRAY)
                && (sym instanceof DomainSymbol))
		{
            ArrayType siteType = (ArrayType) site.getSafeUpperBound().getArrayType();
            DomainType toDomType = (DomainType) sym.type.clone();

			//may still be a projection
			isDomainCast=siteType.dom.domainAccessIsCast(toDomType);

			isDomainProjection=!isDomainCast;
		}
        else if(sym instanceof MethodSymbol&&errArrayMeth)
        {
            tree.type=arrayMeth.type;
            tree.sym=arrayMeth;
            result=arrayMeth.type;
            return;
        }


        // domain parameters are only allowed for projections
//        if(! isDomainProjection && tree.params != null && tree.params.length() != 0) {
//            log.error(tree.pos(), "domain.parameters.at.non.domain.projection");
//        }

        if (tree.params != null) {
            for (JCExpression e : tree.params) {
                attribExpr(e, env, syms.intType);
            }
        }

        // size
        if (isDomainCast && sym.name.equals(names.size)) {

            tree.sym = sym;

            //FIXME: dims is the number of dynamic dimensions
            int dims = 1;

            // make int[one_d{dims}] result type

            DomainType resdom = (DomainType) sym.type;
            ListBuffer<JCExpression> lb = new ListBuffer<JCExpression>();
            lb.add(make.Literal(dims));
            resdom.appliedParams = lb.toList();
            Type owntype = new ArrayType(syms.intType, resdom, syms.arrayClass, (ArrayType) tree.selected.type);

            env.info.selectSuper = selectSuperPrev;
            result = check(tree, owntype, VAL, pkind, pt);
            env.info.tvars = List.nil();
            
            return;

        } // domain projections
        else if (isDomainProjection) {

            ArrayType siteType = (ArrayType) site.getSafeUpperBound().getArrayType();
            DomainType projDomType = (DomainType) sym.type;

            ArrayType resultType;

            if(!sym.name.equals(names.fromString("reduce"))&&(site.type_flags_field&Flags.WORK)!=0)
                log.error(tree.pos,"cast.as.projection",site,sym.type);//FIXME: own error

			if((env.tree instanceof JCMethodInvocation)&&insideMethodAttrib)//not when inside args
			{
				resultType = getProjectionResult(tree.pos(), siteType, projDomType,tree.params,((JCMethodInvocation) env.tree).args);
				resultType = (ArrayType)resultType.addFlag(Flags.BLOCK); //tasg as projection
				// the argtypes of the projection "method" are integers
				Type[] argTypesArray = new Type[projDomType.projectionArgs.length()];
				Arrays.fill(argTypesArray, syms.intType);

				// create method type
				Type owntype = new MethodType(List.from(argTypesArray), resultType, List.<Type>nil(),
						syms.methodClass);

				// check that type and kind are compatible with prototype and protokind.
				env.info.selectSuper = selectSuperPrev;
				result = check(tree, owntype, sym.kind, pkind, pt);
				env.info.tvars = List.nil();

				// set tree.sym to a valid method symbol since this is needed by later phases
				MethodSymbol msym = new MethodSymbol(0, sym.name, owntype, sym);
				msym.isDomainProjection = true;
				msym.params = projDomType.projectionArgs;
				tree.sym = msym;

				//Type.ArrayType at = ((Type.ArrayType) tree.type.getArrayType());

				if(lastProjectionArgs==null)//record different projection params for later use
					lastProjectionArgs=new LinkedHashMap<VarSymbol, JCExpression>();

				//store values for projection params so we can use them for conversions!
				for(int i=0;i<Math.min(projDomType.projectionArgs.size(),((JCMethodInvocation)env.tree).args.size());i++)
				{
					VarSymbol vs=projDomType.projectionArgs.get(i);
					JCExpression e=((JCMethodInvocation)env.tree).args.get(i);
					if(TreeInfo.symbol(e)!=vs)
						lastProjectionArgs.put(vs,e);
				}

			}
			else
			{
				//log.error(tree.pos,"array.project.brackets", tree);
				resultType = getProjectionResult(tree.pos(), siteType, projDomType, tree.params,null);
                resultType.type_flags_field|=Flags.WORK;
				tree.sym = sym;
				result = check(tree, resultType, VAL, pkind, pt);
			}

            return;

        } // domain casts
        else if (isDomainCast) {
            if((site.type_flags_field&Flags.WORK)!=0)
                log.error(tree.pos,"cast.as.projection",site,sym.type);//FIXME: own error
            
            tree.sym = sym;

            ArrayType siteType = (ArrayType) site.getSafeUpperBound().getArrayType();
            DomainType castDomType = (DomainType) sym.type.clone();
			//castDomType.isBaseDomain = false;
            Type owntype;

            boolean nop = false;
            // same domain -> do nothing (emit warning?)
            if (sym == siteType.dom.tsym) {
                owntype = siteType;
                nop = true;
            } // "real" cast
            else {
                owntype = getCastResult(tree.pos(), siteType, castDomType, tree.params);
            }

			if((env.tree instanceof JCMethodInvocation)&& (((JCMethodInvocation) env.tree).meth == tree))
				log.error(tree.pos,"cast.as.projection",siteType,castDomType);

            env.info.selectSuper = selectSuperPrev;
			if(!nop&&!((ArrayType)owntype).dom.isBaseDomain)
				owntype = owntype.addFlag(Flags.HASINIT); //mark thet step from prev to this dom is a cast
			//tree.type = result; //must store result
            result = check(tree, owntype, VAL, pkind, pt);
            env.info.tvars = List.nil();


            if (!nop && !insideAccess) {

                Type lower;

                if (((ArrayType) owntype.getArrayType()).dom.resultDom == null) {
                    lower = siteType;
                } else {
                    lower = owntype;
                }

                java.util.ArrayList<JCTree.JCExpression> ffparams = new java.util.ArrayList<JCTree.JCExpression>();
                for (VarSymbol vs : ((ArrayType) lower.getArrayType()).dom.formalParams) {
                    ffparams.add(make.Ident(vs));
                }

                if (((ArrayType) lower.getArrayType()).dom.resultHasSameSize( tree.pos(), ffparams.toArray(new JCExpression[0]))) {
                    if (((ArrayType) owntype.getArrayType()).dom.isDynamic()) {
                        tree.repackage = true; //dyn doms must be repackaged
                    }
                    //static sized doms don't need any change
                } else {
                    //ok, this is a real conversion, not just a different look at the same thing
					/*
                     matrix.trace().one_d; //casted to base domain
                     * ==
                     new int[one_d{MATRIX_SIZE}].\[i] {matrix.trace().one_d[i]};
                     */

                    //FIXME: does this work for dynamic domains?
					//result.type_flags_field|=Flags.HASINIT; //mark as real cast

                    ListBuffer<JCExpression> lb = new ListBuffer<JCExpression>();

                    Map<String, Object> map = new LinkedHashMap<String, Object>();

                    Iterator<JCExpression> vals = siteType.dom.appliedParams.iterator();
                    Iterator<VarSymbol> fparams = siteType.dom.formalParams.iterator();

                    while (vals.hasNext() && fparams.hasNext()) {
                        map.put(fparams.next().toString(), vals.next());
                    }

                    if (((ArrayType) owntype.getArrayType()).dom.isDynamic()) {
                        for (JCExpression e : ((ArrayType) owntype.getArrayType()).dom.appliedParams) {
                            lb.add(e);//clone and substitute values
                        }
                    } else if (siteType.dom.resultDomParams != null) {
                        for (JCExpression p : siteType.dom.resultDomParams) {
                            lb.add(replace(p, map, true));//clone and substitute values
                        }
                    }

                    List<JCExpression> params = lb.toList();

                    //construct array type wrapper

                    JCDomInstance inst = make.DomInstance(((Type.DomainType) sym.type).tsym.name, params);

                    JCExpression ta = make.TypeArray(make.Type(siteType.elemtype), inst, owntype.isPointer());

					//we use projection args before visiting the actual projection (in codegen) so we must substitute the projection args by their expressions
					//which we aquired previously in lastProjectionArgs
                    JCExpression exp = replaceSymbol(make.NewArray(ta, inst, (new ListBuffer<JCExpression>()).toList(), null),lastProjectionArgs,false);

                    List<VarSymbol> inds = ((Type.DomainType) sym.type).getIndexList(new LinkedHashMap<VarSymbol, VarSymbol>());

                    ListBuffer<JCExpression> indices = new ListBuffer<JCExpression>();
                    ListBuffer<JCVariableDecl> args = new ListBuffer<JCVariableDecl>();
                    for (VarSymbol vs : inds) {
                        args.add(make.VarDef(vs, null));
                        indices.add(make.Ident(vs));
                    }

                    JCExpression sexp = make.Indexed((JCFieldAccess) copy.copy(tree), indices.toList());


                    tree.conversion = make.DomIter(exp, null, args.toList(), sexp, null, null);


					JCFieldAccess oldConstruct=constructExp;
					constructExp=(JCFieldAccess)copy.copy(tree);
					constructExp.conversion=null;//no converion here!
                    attribTree(tree.conversion, env, skind, Type.noType);
					constructExp=oldConstruct;
                }
            }


            return;

        }

        if (sym.exists() && !isType(sym) && (pkind & (PCK | TYP)) != 0) {
            site = capture(site);
            sym = selectSym(tree, site, env, pt, pkind);
        }
        boolean varArgs = env.info.varArgs;
        tree.sym = sym;

        if (site.tag == TYPEVAR && !isType(sym) && sym.kind != ERR) {
            while (site.tag == TYPEVAR) {
                site = site.getUpperBound();
            }
            site = capture(site);
        }

        // If that symbol is a variable, ...
        if (sym.kind == VAR) {
            VarSymbol v = (VarSymbol) sym;

            // ..., evaluate its initializer, if it has one, and check for
            // illegal forward reference.
            checkInit(tree, env, v, true);

            // If we are expecting a variable (as opposed to a value), check
            // that the variable is assignable in the current environment.
            if (pkind == VAR) {
                checkAssignable(tree.pos(), v, tree.selected, env);
            }
        }

        // Disallow selecting a type from an expression
        if (isType(sym) && (sitesym == null || (sitesym.kind & (TYP | PCK)) == 0)) {
            tree.type = check(tree.selected, pt,
                    sitesym == null ? VAL : sitesym.kind, TYP | PCK, pt);
        }

        if (isType(sitesym)) {
            if (sym.name == names._this) {
                // If `C' is the currently compiled class, check that
                // C.this' does not appear in a call to a super(...)
                if (env.info.isSelfCall
                        && site.tsym == env.enclClass.sym) {
                    chk.earlyRefError(tree.pos(), sym);
                }
            } else {
                // Check if type-qualified fields or methods are static (JLS)
                if ((sym.flags() & STATIC) == 0
                        && sym.name != names._super
                        && (sym.kind == VAR || sym.kind == MTH)) {
                    rs.access(rs.new StaticError(sym),
                            tree.pos(), site, sym.name, true);
                }
            }
        }

        // If we are selecting an instance member via a `super', ...
        if (env.info.selectSuper && (sym.flags() & STATIC) == 0) {

            // Check that super-qualified symbols are not abstract (JLS)
            rs.checkNonAbstract(tree.pos(), sym);

            if (site.isRaw()) {
                // Determine argument types for site.
                Type site1 = types.asSuper(env.enclClass.sym.type, site.tsym);
                if (site1 != null) {
                    site = site1;
                }
            }
        }

        env.info.selectSuper = selectSuperPrev;
        result = checkId(tree, site, sym, env, pkind | DOM, pt, varArgs);
        env.info.tvars = List.nil();
    }
    //where

    /**
     * Determine symbol referenced by a Select expression,
     *
     * @param tree The select tree.
     * @param site The type of the selected expression,
     * @param env The current environment.
     * @param pt The current prototype.
     * @param pkind The expected kind(s) of the Select expression.
     */
    private Symbol selectSym(JCFieldAccess tree,
            Type site,
            Env<AttrContext> env,
            Type pt,
            int pkind) {
        DiagnosticPosition pos = tree.pos();
        Name name = tree.getIdentifier();

        switch (site.tag) {
            case PACKAGE:
                return rs.access(
                        rs.findIdentInPackage(env, site.tsym, name, pkind),
                        pos, site, name, true);
            case ARRAY:
            case CLASS:
                if (pt.tag == METHOD || pt.tag == FORALL) {
                    return rs.resolveQualifiedMethod(
                            pos, env, site, name, pt.getParameterTypes(), pt.getTypeArguments());
                } else if (name == names._this || name == names._super) {
                    return rs.resolveSelf(pos, env, site.tsym, name);
                } else if (name == names._class) {
                    // In this case, we have already made sure in
                    // visitSelect that qualifier expression is a type.
                    Type t = syms.classType;
                    List<Type> typeargs = allowGenerics
                            ? List.of(types.erasure(site))
                            : List.<Type>nil();
                    t = new ClassType(t.getEnclosingType(), typeargs, t.tsym);
                    return new VarSymbol(
                            STATIC | PUBLIC | FINAL, names._class, t, site.tsym);
                } else {
                    if (site.tag == TypeTags.CLASS) {
                        site = ((ClassType) site).getArrayType();
                    }
                    if (site.tag == ARRAY) {
                        site = ((ArrayType) site).dom;
                    }
                    // We are seeing a plain identifier as selector.
                    Symbol sym = syms.errSymbol;
                    if (site == null) {
                        log.error(tree.pos, "invalid.domain.type", tree);
                    } else {
                        sym = rs.findIdentInType(env, site, name, pkind);
                    }
                    if ((pkind & ERRONEOUS) == 0) {
                        sym = rs.access(sym, pos, site, name, true);
                    }
                    return sym;
                }
            case WILDCARD:
                throw new AssertionError(tree);
            case TYPEVAR:
                // Normally, site.getUpperBound() shouldn't be null.
                // It should only happen during memberEnter/attribBase
                // when determining the super type which *must* be
                // done before attributing the type variables.  In
                // other words, we are seeing this illegal program:
                // class B<T> extends A<T.foo> {}
                Symbol sym = (site.getUpperBound() != null)
                        ? selectSym(tree, capture(site.getUpperBound()), env, pt, pkind)
                        : null;
                if (sym == null || isType(sym)) {
                    log.error(pos, "type.var.cant.be.deref");
                    return syms.errSymbol;
                } else {
                    Symbol sym2 = (sym.flags() & Flags.PRIVATE) != 0
                            ? rs.new AccessError(env, site, sym)
                            : sym;
                    rs.access(sym2, pos, site, name, true);
                    return sym;
                }
            case ERROR:
                // preserve identifier names through errors
                return types.createErrorType(name, site.tsym, site).tsym;
            default:
                // The qualifier expression is of a primitive type -- only
                // .class is allowed for these.
                if (name == names._class) {
                    // In this case, we have already made sure in Select that
                    // qualifier expression is a type.
                    Type t = syms.classType;
                    Type arg = types.boxedClass(site).type;
                    t = new ClassType(t.getEnclosingType(), List.of(arg), t.tsym);
                    return new VarSymbol(
                            STATIC | PUBLIC | FINAL, names._class, t, site.tsym);
                } else {
                    log.error(pos, "cant.deref", site);
                    return syms.errSymbol;
                }
        }
    }

    /**
     * Determine symbol referenced by a JCDomainIter expression,
     *
     * @param tree The select tree.
     * @param site The type of the selected expression,
     * @param env The current environment.
     * @param pt The current prototype.
     * @param pkind The expected kind(s) of the Select expression.
     */
    private Symbol selectSym(JCDomainIter tree,
            Type site,
            Env<AttrContext> env,
            Type pt,
            int pkind) {
        DiagnosticPosition pos = tree.pos();
        Name name = tree.getName();

        switch (site.tag) {
            case PACKAGE:
                return rs.access(
                        rs.findIdentInPackage(env, site.tsym, name, pkind),
                        pos, site, name, true);
            case DOMAIN: {
                Symbol sym = rs.findIdentInType(env, site, name, pkind);
                if ((pkind & ERRONEOUS) == 0) {
                    sym = rs.access(sym, pos, site, name, true);
                }

                return sym;

            }
            case ARRAY: {
                //Symbol sym = rs.resolveDomain(tree.pos(),tree.getName(),env);
                Symbol sym = rs.findIdentInType(env, site, name, pkind);
                if ((pkind & ERRONEOUS) == 0) {
                    sym = rs.access(sym, pos, site, name, true);
                }

                return sym;
                //return sym;
            }
            case CLASS:
                if (pt.tag == METHOD || pt.tag == FORALL) {
                    return rs.resolveQualifiedMethod(
                            pos, env, site, name, pt.getParameterTypes(), pt.getTypeArguments());
                } else if (name == names._this || name == names._super) {
                    return rs.resolveSelf(pos, env, site.tsym, name);
                } else if (name == names._class) {
                    // In this case, we have already made sure in
                    // visitSelect that qualifier expression is a type.
                    Type t = syms.classType;
                    List<Type> typeargs = allowGenerics
                            ? List.of(types.erasure(site))
                            : List.<Type>nil();
                    t = new ClassType(t.getEnclosingType(), typeargs, t.tsym);
                    return new VarSymbol(
                            STATIC | PUBLIC | FINAL, names._class, t, site.tsym);
                } else {
                    // We are seeing a plain identifier as selector.
                    Symbol sym = rs.findIdentInType(env, site, name, pkind);
                    if ((pkind & ERRONEOUS) == 0) {
                        sym = rs.access(sym, pos, site, name, true);
                    }
                    return sym;
                }
            case WILDCARD:
                throw new AssertionError(tree);
            case TYPEVAR:
                // Normally, site.getUpperBound() shouldn't be null.
                // It should only happen during memberEnter/attribBase
                // when determining the super type which *must* be
                // done before attributing the type variables.  In
                // other words, we are seeing this illegal program:
                // class B<T> extends A<T.foo> {}
                Symbol sym = (site.getUpperBound() != null)
                        ? selectSym(tree, capture(site.getUpperBound()), env, pt, pkind)
                        : null;
                if (sym == null || isType(sym)) {
                    log.error(pos, "type.var.cant.be.deref");
                    return syms.errSymbol;
                } else {
                    Symbol sym2 = (sym.flags() & Flags.PRIVATE) != 0
                            ? rs.new AccessError(env, site, sym)
                            : sym;
                    rs.access(sym2, pos, site, name, true);
                    return sym;
                }
            case ERROR:
                // preserve identifier names through errors
                return types.createErrorType(name, site.tsym, site).tsym;
            default:
                // The qualifier expression is of a primitive type -- only
                // .class is allowed for these.
                if (name == names._class) {
                    // In this case, we have already made sure in Select that
                    // qualifier expression is a type.
                    Type t = syms.classType;
                    Type arg = types.boxedClass(site).type;
                    t = new ClassType(t.getEnclosingType(), List.of(arg), t.tsym);
                    return new VarSymbol(
                            STATIC | PUBLIC | FINAL, names._class, t, site.tsym);
                } else {
                    log.error(pos, "cant.deref", site);
                    return syms.errSymbol;
                }
        }
    }

    /**
     * Determine type of identifier or select expression and check that (1) the
     * referenced symbol is not deprecated (2) the symbol's type is safe (
     *
     * @see checkSafe) (3) if symbol is a variable, check that its type and kind
     * are compatible with the prototype and protokind. (4) if symbol is an
     * instance field of a raw type, which is being assigned to, issue an
     * unchecked warning if its type changes under erasure. (5) if symbol is an
     * instance method of a raw type, issue an unchecked warning if its argument
     * types change under erasure. If checks succeed: If symbol is a constant,
     * return its constant type else if symbol is a method, return its result
     * type otherwise return its type. Otherwise return errType.
     *
     * @param tree The syntax tree representing the identifier
     * @param site If this is a select, the type of the selected expression,
     * otherwise the type of the current class.
     * @param sym The symbol representing the identifier.
     * @param env The current environment.
     * @param pkind The set of expected kinds.
     * @param pt The expected type.
     */
    Type checkId(JCTree tree,
            Type site,
            Symbol sym,
            Env<AttrContext> env,
            int pkind,
            Type pt,
            boolean useVarargs) {
        if (pt.isErroneous()) {
            return types.createErrorType(site);
        }
        Type owntype; // The computed type of this identifier occurrence.
        switch (sym.kind) {
            case TYP | DOM:
                owntype = sym.type;
                break;
            case TYP:
                // For types, the computed type equals the symbol's type,
                // except for two situations:
                owntype = sym.type;
                if (owntype.tag == CLASS) {
                    Type ownOuter = owntype.getEnclosingType();

                    // (a) If the symbol's type is parameterized, erase it
                    // because no type parameters were given.
                    // We recover generic outer type later in visitTypeApply.
                    if (owntype.tsym.type.getTypeArguments().nonEmpty()) {
                        owntype = types.erasure(owntype);
                    } // (b) If the symbol's type is an inner class, then
                    // we have to interpret its outer type as a superclass
                    // of the site type. Example:
                    //
                    // class Tree<A> { class Visitor { ... } }
                    // class PointTree extends Tree<Point> { ... }
                    // ...PointTree.Visitor...
                    //
                    // Then the type of the last expression above is
                    // Tree<Point>.Visitor.
                    else if (ownOuter.tag == CLASS && site != ownOuter) {
                        Type normOuter = site;
                        if (normOuter.tag == CLASS) {
                            normOuter = types.asEnclosingSuper(site, ownOuter.tsym);
                        }
                        if (normOuter == null) // perhaps from an import
                        {
                            normOuter = types.erasure(ownOuter);
                        }
                        if (normOuter != ownOuter) {
                            owntype = new ClassType(
                                    normOuter, List.<Type>nil(), owntype.tsym);
                        }
                    }
                }
                break;
            case VAR:
                VarSymbol v = (VarSymbol) sym;
                // Test (4): if symbol is an instance field of a raw type,
                // which is being assigned to, issue an unchecked warning if
                // its type changes under erasure.
                if (allowGenerics
                        && pkind == VAR
                        && v.owner.kind == TYP
                        && (v.flags() & STATIC) == 0
                        && (site.tag == CLASS || site.tag == TYPEVAR)) {
                    Type s = types.asOuterSuper(site, v.owner);
                    if (s != null
                            && s.isRaw()
                            && !types.isSameType(v.type, v.erasure(types))) {
                        chk.warnUnchecked(tree.pos(),
                                "unchecked.assign.to.var",
                                v, s);
                    }
                }
                // The computed type of a variable is the type of the
                // variable symbol, taken as a member of the site type.
                owntype = (sym.owner.kind == TYP
                        && sym.name != names._this && sym.name != names._super)
                        ? types.memberType(site, sym)
                        : sym.type;

                if (env.info.tvars.nonEmpty()) {
                    Type owntype1 = new ForAll(env.info.tvars, owntype);
                    for (List<Type> l = env.info.tvars; l.nonEmpty(); l = l.tail) {
                        if (!owntype.contains(l.head)) {
                            log.error(tree.pos(), "undetermined.type", owntype1);
                            owntype1 = types.createErrorType(owntype1);
                        }
                    }
                    owntype = owntype1;
                }

                // If the variable is a constant, record constant value in
                // computed type.
                if (v.getConstValue() != null && isStaticReference(tree)) {
                    owntype = owntype.constType(v.getConstValue());
                }

                if (pkind == VAL) {
                    owntype = capture(owntype); // capture "names as expressions"
                }
                break;
            case MTH: {
                if (pt.tag == TypeTags.NONE) {
                    if ((sym.flags_field & Flags.STATIC) == 0) {
                        log.error(tree.pos(), "non.static.functionpointer", sym);
                    }
                    owntype = sym.type;
                } else if (env.tree instanceof JCMethodInvocation) {
                    JCMethodInvocation app = (JCMethodInvocation) env.tree;
                    owntype = checkMethod(site, sym, env, app.args,
                            pt.getParameterTypes(), pt.getTypeArguments(),
                            env.info.varArgs);
                } else {
                    owntype = sym.type;
                }
                break;
            }
            case PCK:
            case ERR:
                owntype = sym.type;
                break;
            default:
                throw new AssertionError("unexpected kind: " + sym.kind
                        + " in tree " + tree);
        }

        // Test (1): emit a `deprecation' warning if symbol is deprecated.
        // (for constructors, the error was given when the constructor was
        // resolved)
        if (sym.name != names.init
                && (sym.flags() & DEPRECATED) != 0
                && (env.info.scope.owner.flags() & DEPRECATED) == 0
                && sym.outermostClass() != env.info.scope.owner.outermostClass()) {
            chk.warnDeprecated(tree.pos(), sym);
        }

        if ((sym.flags() & PROPRIETARY) != 0) {
            log.strictWarning(tree.pos(), "sun.proprietary", sym);
        }

        // Test (3): if symbol is a variable, check that its type and
        // kind are compatible with the prototype and protokind.
        return check(tree, owntype, sym.kind, pkind, pt);
    }

    /**
     * Check that variable is initialized and evaluate the variable's
     * initializer, if not yet done. Also check that variable is not referenced
     * before it is defined.
     *
     * @param tree The tree making up the variable reference.
     * @param env The current environment.
     * @param v The variable's symbol.
     */
    private void checkInit(JCTree tree,
            Env<AttrContext> env,
            VarSymbol v,
            boolean onlyWarning) {
//          System.err.println(v + " " + ((v.flags() & STATIC) != 0) + " " +
//                             tree.pos + " " + v.pos + " " +
//                             Resolve.isStatic(env));//DEBUG

        // A forward reference is diagnosed if the declaration position
        // of the variable is greater than the current tree position
        // and the tree and variable definition occur in the same class
        // definition.  Note that writes don't count as references.
        // This check applies only to class and instance
        // variables.  Local variables follow different scope rules,
        // and are subject to definite assignment checking.
        if ((env.info.enclVar == v || v.pos > tree.pos)
                && v.owner.kind == TYP
                && canOwnInitializer(env.info.scope.owner)
                && v.owner == env.info.scope.owner.enclClass()
                && ((v.flags() & STATIC) != 0) == Resolve.isStatic(env)
                && (env.tree.getTag() != JCTree.ASSIGN
                || TreeInfo.skipParens(((JCAssign) env.tree).lhs) != tree)) {
            String suffix = (env.info.enclVar == v)
                    ? "self.ref" : "forward.ref";
            if (!onlyWarning || isStaticEnumField(v)) {
                log.error(tree.pos(), "illegal." + suffix);
            } else if (useBeforeDeclarationWarning) {
                log.warning(tree.pos(), suffix, v);
            }
        }

        v.getConstValue(); // ensure initializer is evaluated

        checkEnumInitializer(tree, env, v);
    }

    /**
     * Check for illegal references to static members of enum. In an enum type,
     * constructors and initializers may not reference its static members unless
     * they are constant.
     *
     * @param tree The tree making up the variable reference.
     * @param env The current environment.
     * @param v The variable's symbol.
     * @see JLS 3rd Ed. (8.9 Enums)
     */
    private void checkEnumInitializer(JCTree tree, Env<AttrContext> env, VarSymbol v) {
        // JLS 3rd Ed.:
        //
        // "It is a compile-time error to reference a static field
        // of an enum type that is not a compile-time constant
        // (15.28) from constructors, instance initializer blocks,
        // or instance variable initializer expressions of that
        // type. It is a compile-time error for the constructors,
        // instance initializer blocks, or instance variable
        // initializer expressions of an enum constant e to refer
        // to itself or to an enum constant of the same type that
        // is declared to the right of e."
        if (isStaticEnumField(v)) {
            ClassSymbol enclClass = env.info.scope.owner.enclClass();

            if (enclClass == null || enclClass.owner == null) {
                return;
            }

            // See if the enclosing class is the enum (or a
            // subclass thereof) declaring v.  If not, this
            // reference is OK.
            if (v.owner != enclClass && !types.isSubtype(enclClass.type, v.owner.type)) {
                return;
            }

            // If the reference isn't from an initializer, then
            // the reference is OK.
            if (!Resolve.isInitializer(env)) {
                return;
            }

            log.error(tree.pos(), "illegal.enum.static.ref");
        }
    }

    /**
     * Is the given symbol a static, non-constant field of an Enum? Note: enum
     * literals should not be regarded as such
     */
    private boolean isStaticEnumField(VarSymbol v) {
        return Flags.isEnum(v.owner)
                && Flags.isStatic(v)
                && !Flags.isConstant(v)
                && v.name != names._class;
    }

    /**
     * Can the given symbol be the owner of code which forms part if class
     * initialization? This is the case if the symbol is a type or field, or if
     * the symbol is the synthetic method. owning a block.
     */
    private boolean canOwnInitializer(Symbol sym) {
        return (sym.kind & (VAR | TYP)) != 0
                || (sym.kind == MTH && (sym.flags() & BLOCK) != 0);
    }
    Warner noteWarner = new Warner();

    /**
     * Check that method arguments conform to its instantation.
     *
     */
    public Type checkMethod(Type site,
            Symbol sym,
            Env<AttrContext> env,
            final List<JCExpression> argtrees,
            List<Type> argtypes,
            List<Type> typeargtypes,
            boolean useVarargs) {
        // Test (5): if symbol is an instance method of a raw type, issue
        // an unchecked warning if its argument types change under erasure.
        if (allowGenerics
                && (sym.flags() & STATIC) == 0
                && (site.tag == CLASS || site.tag == TYPEVAR)) {
            Type s = types.asOuterSuper(site, sym.owner);
            if (s != null && s.isRaw()
                    && !types.isSameTypes(sym.type.getParameterTypes(),
                    sym.erasure(types).getParameterTypes())) {
                chk.warnUnchecked(env.tree.pos(),
                        "unchecked.call.mbr.of.raw.type",
                        sym, s);
            }
        }

        // Compute the identifier's instantiated type.
        // For methods, we need to compute the instance type by
        // Resolve.instantiate from the symbol's type as well as
        // any type arguments and value arguments.
        noteWarner.warned = false;
        Type owntype = rs.instantiate(env,
                site,
                sym,
                argtypes,
                typeargtypes,
                false,
                useVarargs,
                noteWarner);
        boolean warned = noteWarner.warned;

        // If this fails, something went wrong; we should not have
        // found the identifier in the first place.
        if (owntype == null) {
            if (!pt.isErroneous()) {
                log.error(env.tree.pos(),
                        "internal.error.cant.instantiate",
                        sym, site,
                        Type.toString(pt.getParameterTypes()));
            }
            owntype = types.createErrorType(site);
        } else {

            // System.out.println("call   : " + env.tree);
            // System.out.println("method : " + owntype);
            // System.out.println("actuals: " + argtypes);
            List<Type> formals = owntype.getParameterTypes();
            Type last = useVarargs ? formals.last() : null;
            if (sym.name == names.init
                    && sym.owner == syms.enumSym) {
                formals = formals.tail.tail;
            }
            List<JCExpression> args = argtrees;
            while (formals.head != last) {
                JCTree at = args.head;
                JCArgExpression ae = (JCArgExpression) at;
                JCTree arg = ae.exp1;
                Warner warn = chk.convertWarner(arg.pos(), arg.type, formals.head);
                assertConvertible(arg, arg.type, formals.head, warn);
                warned |= warn.warned;
                args = args.tail;
                formals = formals.tail;
            }
            if (useVarargs) {
                Type varArg = types.elemtype(last);
                while (args.tail != null) {
                    JCTree at = args.head;
                    JCArgExpression ae = (JCArgExpression) at;
                    JCTree arg = ae.exp1;

                    Warner warn = chk.convertWarner(arg.pos(), arg.type, varArg);
                    assertConvertible(arg, arg.type, varArg, warn);
                    warned |= warn.warned;
                    args = args.tail;
                }
            } else if ((sym.flags() & VARARGS) != 0 && allowVarargs
                    && false //we don't care
                    ) {
                // non-varargs call to varargs method
                Type varParam = owntype.getParameterTypes().last();
                Type lastArg = argtypes.last();
                if (types.isSubtypeUnchecked(lastArg, types.elemtype(varParam))
                        && !types.isSameType(types.erasure(varParam), types.erasure(lastArg))) {
                    log.warning(argtrees.last().pos(), "inexact.non-varargs.call",
                            types.elemtype(varParam),
                            varParam);
                }
            }

            if (warned && sym.type.tag == FORALL) {
                chk.warnUnchecked(env.tree.pos(),
                        "unchecked.meth.invocation.applied",
                        kindName(sym),
                        sym.name,
                        rs.methodArguments(sym.type.getParameterTypes()),
                        rs.methodArguments(argtypes),
                        kindName(sym.location()),
                        sym.location());
                owntype = new MethodType(owntype.getParameterTypes(),
                        types.erasure(owntype.getReturnType()),
                        owntype.getThrownTypes(),
                        syms.methodClass);
            }
            if (useVarargs) {
                JCTree tree = env.tree;
                Type argtype = owntype.getParameterTypes().last();
                if (!types.isReifiable(argtype)) {
                    chk.warnUnchecked(env.tree.pos(),
                            "unchecked.generic.array.creation",
                            argtype);
                }
                Type elemtype = types.elemtype(argtype);
                switch (tree.getTag()) {
                    case JCTree.APPLY:
                        ((JCMethodInvocation) tree).varargsElement = elemtype;
                        break;
                    case JCTree.NEWCLASS:
                        ((JCNewClass) tree).varargsElement = elemtype;
                        break;
                    default:
                        throw new AssertionError("" + tree);
                }
            }
        }
        return owntype;
    }

    private void assertConvertible(JCTree tree, Type actual, Type formal, Warner warn) {
        if (types.isConvertible(actual, formal, warn)) {

			Type atf=formal.getArrayType();
			Type ata=actual.getArrayType();

			boolean fd=false;
			if(atf.tag==TypeTags.ARRAY&&((ArrayType)atf).dom!=null)
				fd=((ArrayType)atf).dom.isDynamic();
			boolean ad=false;
			if(ata.tag==TypeTags.ARRAY&&((ArrayType)ata).dom!=null)
				ad=((ArrayType)ata).dom.isDynamic();

			if(!ad&&fd)
			{
				log.warning(tree.pos(), "info", "auto conversion from static to dynamic array not yet implemented");
			}

            return;
        }

        if (formal.isCompound()
                && types.isSubtype(actual, types.supertype(formal))
                && types.isSubtypeUnchecked(actual, types.interfaces(formal), warn)) {
            return;
        }

        if (false) {
            // TODO: make assertConvertible work
            chk.typeError(tree.pos(), diags.fragment("incompatible.types"), actual, formal);
            throw new AssertionError("Tree: " + tree
                    + " actual:" + actual
                    + " formal: " + formal);
        }
    }

    public void visitLiteral(JCLiteral tree) {
        result = check(
                tree, litType(tree.typetag).constType(tree.value), VAL, pkind, pt);
    }
    //where

    /**
     * Return the type of a literal with given type tag.
     */
    Type litType(int tag) {
        return (tag == TypeTags.CLASS) ? syms.stringType : syms.typeOfTag[tag];
    }

    public void visitTypeIdent(JCPrimitiveTypeTree tree) {
        result = check(tree, syms.typeOfTag[tree.typetag], TYP, pkind, pt);
    }

    public void visitTypeArray(JCArrayTypeTree tree) {
        Type etype = attribType(tree.elemtype, env);

        if (!etype.isPrimitive() && etype.tag == TypeTags.CLASS) {
            env.enclClass.sym.recAddRef(etype, false);
        }

        if (tree.option && !etype.isPrimitive()) {
            etype = etype.addFlag(FOUT);
            tree.elemtype.type = etype;
        }

        Type type;
        if (tree.dom != null) {
            Symbol s = rs.findIdent(env, tree.dom.getName(), TYP | DOM);
            if (!tree.dom.domparams.isEmpty()) {
                DomainType dom = (DomainType) attribType(tree.dom, env);
                calcResultParams(dom);
                type = new ArrayType(etype, dom, syms.arrayClass);
            } else {
                if (s.exists()) {
                    log.error(tree.pos, "invalid.domain.type", s);
                    type = Type.noType;
                } else {
                    type = new ArrayType(etype, null, syms.arrayClass);
                }
            }
        } else {
            type = new ArrayType(etype, null, syms.arrayClass);
        }

        //tree.dom.domparams
        result = check(tree, type, TYP, pkind, pt);
    }
    public boolean before_typecheck = true;
    public boolean force_linear = false;

    /**
     * Duplicate this environment into a given Environment, using its tree and
     * info, and copying all other fields.
     */
    public Env<AttrContext> deepdupto(Env<AttrContext> that) {
        if (that == null) {
            return null;
        }
        Env<AttrContext> next = new Env<AttrContext>();
        next.outer = deepdupto(that.outer);
        next.next = deepdupto(that.next);
        next.toplevel = (JCCompilationUnit) copy.copy(that.toplevel);
        next.toplevel.namedImportScope = next.toplevel.namedImportScope.dup();
        next.toplevel.starImportScope = next.toplevel.starImportScope.dup();
        next.enclClass = that.enclClass;
        next.enclMethod = that.enclMethod;
        next.info = that.info.dup();
        next.tree = copy.copy(that.tree);
        return next;
    }

    public Type instantiateTemplate(JCTypeApply tree, ClassType ct, List<JCExpression> actuals) {
        boolean old_force = force_linear;
//		force_linear|=(ct.type_flags_field&Flags.LINEAR)!=0;
        if (force_linear && ct.tree.extending != null) {
            JCTree instance = ct.tree.extending;
            while (instance.getTag() == JCTree.TYPEAPPLY) {
                instance = ((JCTypeApply) instance).clazz;
            }

            if (instance.getTag() != JCTree.TYPEARRAY) {
                force_linear = false;
            }
        } else {
            force_linear = false;
        }
        Type t = ct.cache.get(actuals.toString() + (force_linear ? "U" : ""));
        if (t == null) {
            //not yet instantiated
            Iterator<Type> fa = ct.tsym.type.getTypeArguments().iterator();
            Iterator<JCExpression> ia = actuals.iterator();

            Map<String, Object> map = new LinkedHashMap<String, Object>();

            while (fa.hasNext() && ia.hasNext()) {
                map.put(fa.next().tsym.toString(), ia.next());
            }

			JavaFileObject prev = log.useSource(enter.getEnv(ct.tsym).enclClass.sym.sourcefile);
            JCTree instance = replace(ct.tree, map, true);

            if (force_linear) {
                ((JCClassDecl) instance).mods.flags |= Flags.LINEAR;
            }

            ((JCClassDecl) instance).typarams = List.nil();

            ((JCClassDecl) instance).name = names.fromString(((JCClassDecl) instance).name + "__" + JCTree.fixName(actuals.toString("__")) + (force_linear ? "U" : ""));

            Env<AttrContext> outer = env.enclosing(JCTree.TOPLEVEL);

            Env<AttrContext> source = deepdupto(outer);
            memberEnter.memberEnter(enter.getEnv(ct.tsym).toplevel, source);

            //we get the symbols in our current environment and those symbols that are defined in the toplevel of the template

            outer = enter.getEnv(ct.tsym);

            JavaFileObject sprev = source.toplevel.sourcefile;
            source.toplevel.sourcefile = outer.enclClass.sym.sourcefile != null
                    ? outer.enclClass.sym.sourcefile
                    : outer.toplevel.sourcefile;

            log.useSource(source.enclClass.sym.sourcefile);

            try {

                t = enter.classEnter(instance, source);

                ((ClassType) t).template = ct.tree.name.toString() + "<" + actuals + ">";

                ((ClassType) t).tree = (JCClassDecl) instance;

                ct.cache.put(actuals.toString() + (force_linear ? "U" : ""), t);

                if (!before_typecheck) {
                    t.tsym.finish();
                    attribClass(tree, (ClassSymbol) t.tsym);
                }

            } finally {
                log.useSource(prev);
                source.toplevel.sourcefile = sprev;
            }
            templatesInstances.add(enter.typeEnvs.get(t.tsym));
            //templates

        }
        force_linear = old_force;
        return t;
    }

    /**
     * Visitor method for parameterized types. Bound checking is left until
     * later, since types are attributed before supertype structure is
     * completely known
     */
    public void visitTypeApply(JCTypeApply tree) {
        // Attribute functor part of application and make sure it's a class.
        Type clazztype = attribType(tree.clazz, env);

        Type owntype = null;

        if (clazztype.tag == CLASS) {
            // Attribute type parameters
            List<Type> actuals = attribArgTypes(tree.arguments, env);

            List<Type> formals = clazztype.tsym.type.getTypeArguments();

            if (actuals.length() == formals.length()) {
                List<Type> a = actuals;
                List<Type> f = formals;
                boolean free = false;
                while (a.nonEmpty()) {
                    a.head = a.head.withTypeVar(f.head);
                    if (a.head.tag == TypeTags.TYPEVAR) {
                        free = true;
                    }
                    a = a.tail;
                    f = f.tail;
                }

                if (!free) {
                    owntype = instantiateTemplate(tree, (ClassType) clazztype, tree.arguments);
                } else {
                    // Compute the proper generic outer
                    Type clazzOuter = clazztype.getEnclosingType();
                    if (clazzOuter.tag == CLASS) {
                        Type site;
                        if (tree.clazz.getTag() == JCTree.IDENT) {
                            site = env.enclClass.sym.type;
                        } else if (tree.clazz.getTag() == JCTree.SELECT) {
                            site = ((JCFieldAccess) tree.clazz).selected.type;
                        } else {
                            throw new AssertionError("" + tree);
                        }
                        if (clazzOuter.tag == CLASS && site != clazzOuter) {
                            if (site.tag == CLASS) {
                                site = types.asOuterSuper(site, clazzOuter.tsym);
                            }
                            if (site == null) {
                                site = types.erasure(clazzOuter);
                            }
                            clazzOuter = site;
                        }
                    }
                    owntype = new ClassType(clazzOuter, actuals, clazztype.tsym);
                }

            } else {
                if (formals.length() != 0) {
                    log.error(tree.pos(), "wrong.number.type.args",
                            Integer.toString(formals.length()));
                } else {
                    log.error(tree.pos(), "type.doesnt.take.params", clazztype.tsym);
                }
                owntype = types.createErrorType(tree.type);
            }
        }

        result = check(tree, owntype, TYP, pkind, pt);

    }

    public void visitTypeParameter(JCTypeParameter tree) {
        TypeVar a = (TypeVar) tree.type;
        Set<Type> boundSet = new LinkedHashSet<Type>();
        if (a.bound.isErroneous()) {
            return;
        }
        List<Type> bs = types.getBounds(a);
        if (tree.bounds.nonEmpty()) {
            // accept class or interface or typevar as first bound.
            Type b = checkBase(bs.head, tree.bounds.head, env, false, false, false);
            boundSet.add(types.erasure(b));
            if (b.isErroneous()) {
                a.bound = b;
            } else if (b.tag == TYPEVAR) {
                // if first bound was a typevar, do not accept further bounds.
                if (tree.bounds.tail.nonEmpty()) {
                    log.error(tree.bounds.tail.head.pos(),
                            "type.var.may.not.be.followed.by.other.bounds");
                    tree.bounds = List.of(tree.bounds.head);
                    a.bound = bs.head;
                }
            } else {
                // if first bound was a class or interface, accept only interfaces
                // as further bounds.
                for (JCExpression bound : tree.bounds.tail) {
                    bs = bs.tail;
                    Type i = checkBase(bs.head, bound, env, false, true, false);
                    if (i.isErroneous()) {
                        a.bound = i;
                    } else if (i.tag == CLASS) {
                        chk.checkNotRepeated(bound.pos(), types.erasure(i), boundSet);
                    }
                }
            }
        }
        bs = types.getBounds(a);

        // in case of multiple bounds ...
        if (bs.length() > 1) {
            // ... the variable's bound is a class type flagged COMPOUND
            // (see comment for TypeVar.bound).
            // In this case, generate a class tree that represents the
            // bound class, ...
            JCTree extending;
            List<JCExpression> implementing;
            if ((bs.head.tsym.flags() & INTERFACE) == 0) {
                extending = tree.bounds.head;
                implementing = tree.bounds.tail;
            } else {
                extending = null;
                implementing = tree.bounds;
            }
            JCClassDecl cd = make.at(tree.pos).ClassDef(
                    make.Modifiers(PUBLIC | ABSTRACT),
                    tree.name, List.<JCTypeParameter>nil(),
                    extending, implementing, List.<JCTree>nil());

            ClassSymbol c = (ClassSymbol) a.getUpperBound().tsym;
            assert (c.flags() & COMPOUND) != 0;
            cd.sym = c;
            c.sourcefile = env.toplevel.sourcefile;

            // ... and attribute the bound class
            c.flags_field |= UNATTRIBUTED;
            Env<AttrContext> cenv = enter.classEnv(cd, env);
            enter.typeEnvs.put(c, cenv);
        }
    }

    public void visitWildcard(JCWildcard tree) {
        //- System.err.println("visitWildcard("+tree+");");//DEBUG
        Type type = (tree.kind.kind == BoundKind.UNBOUND)
                ? syms.objectType
                : attribType(tree.inner, env);
        result = check(tree, new WildcardType(chk.checkRefType(tree.pos(), type),
                tree.kind.kind,
                syms.boundClass),
                TYP, pkind, pt);
    }

    public void visitAnnotation(JCAnnotation tree) {
        log.error(tree.pos(), "annotation.not.valid.for.type", pt);
        result = tree.type = syms.errType;
    }

    public void visitErroneous(JCErroneous tree) {
        if (tree.errs != null) {
            for (JCTree err : tree.errs) {
                attribTree(err, env, ERR, pt);
            }
        }
        result = tree.type = syms.errType;
    }

    public void visitSequence(JCSequence s) {
        for (JCExpression e : s.seq) {
            attribExpr(e, env);
        }

        result = s.seq.head.type;
    }

    public void visitJoin(JCJoinDomains s) {
        ListBuffer<Pair<DomainType, JCExpression[]>> doms = new ListBuffer<Pair<DomainType, JCExpression[]>>();
        Type element = null;
		ArrayType at = null;
        for (JCExpression e : s.doms) {
            Type t = attribExpr(e, env);
//			e.type=t;
			if(t==syms.botType)
				s.allowUnderSpec=true;
			else if(t.tag==TypeTags.ARRAY)
			{
				at=((ArrayType) t);

				//FIXME: must translate at backwards (reverse lenses) to get root domain, this is just a hack.
				if(at.isCast())
					at=at.getRealType();

	            doms.add(new Pair<DomainType, JCExpression[]>(at.dom, at.dom.appliedParams.toArray(new JCExpression[0])));

				if (element == null) {
					element = ((ArrayType) t).elemtype;
				}

				if (!types.isSameType(((ArrayType) t).elemtype, element)) {
					log.error(e.pos(), "unexpected.type",
							element,
							((ArrayType) t).elemtype);
				}

				at=(ArrayType)e.type;

			}
			else
				log.error(e.pos(), "unexpected.type",
							e.type,
							"ArrayType");
        }

		if(at==null)
		{
			log.error(s.pos(), "unexpected.type",
			s,
			"ArrayType");
			result = new ArrayType(syms.errType, null, syms.arrayClass);
			s.type = result;
			return;
		}

		at=at.getBaseType();

        DomainType dom = (DomainType) at.dom;

        Map<String, JCExpression> params_map = new LinkedHashMap<String, JCExpression>();

        Iterator<JCExpression> ap = dom.appliedParams.iterator();
        Iterator<VarSymbol> fp = dom.formalParams.iterator();

        while (ap.hasNext() && fp.hasNext()) {
            params_map.put(fp.next().toString(), ap.next());
        }

        ListBuffer<JCExpression> params = new ListBuffer<JCExpression>();

        DomainType pdom;



        if ((DomainType) at.dom.parentDomain != null) {
            pdom = (DomainType) at.dom.parentDomain;
        } else {
            pdom = at.dom;
        }

        for (VarSymbol vs : pdom.formalParams) {
            params.add(params_map.get(vs.toString()));
        }

		DomainType resdom = (DomainType) pdom.clone();
		resdom.appliedParams = params.toList();
        DomainType.DomSpecError ds = (resdom).isEqual(s.pos(),env.enclMethod, params.toArray(new JCExpression[0]), doms.toList());

        DomainType parent = (DomainType) pdom.clone();
        parent.appliedParams = params.toList();
        result = new ArrayType(element, parent, syms.arrayClass);

		if(at.isLinear())
			result=result.addFlag(Flags.LINEAR);

        if (ds.result == DomainType.DomainSpec.Equal) {
        }
		//FIXME: currently check is incorrect
		else if(jc.verifyArrays)
		{
			if(ds.result==DomainType.DomainSpec.Unknown)
			{
				log.warning(s.pos(), "domain.unknown.spec",ds.details);
			}
			else {
				if (ds.result == DomainType.DomainSpec.UnderSpecified) {
					if(!s.allowUnderSpec)
					{
						log.error(s.pos(), "domain.under.spec",ds.details);
						result = types.createErrorType(dom.parentDomain);
					}
				} else {
					log.error(s.pos(), "domain.over.spec",ds.details);
					result = types.createErrorType(dom.parentDomain);
				}
			}
		}
        s.type = result;
    }

    /**
     * Default visitor method for all other trees.
     */
    public void visitTree(JCTree tree) {
        throw new AssertionError();
    }

    /**
     * Main method: attribute class definition associated with given class
     * symbol. reporting completion failures at the given position.
     *
     * @param pos The source position at which completion errors are to be
     * reported.
     * @param c The class symbol whose definition will be attributed.
     */
    public void attribDomain(DiagnosticPosition pos, DomainSymbol c) {
        try {
            annotate.flush();
            attribDomain(c);
        } catch (CompletionFailure ex) {
            chk.completionError(pos, ex);
        }
    }

    public void attribClass(DiagnosticPosition pos, ClassSymbol c) {
        try {
            annotate.flush();
            attribClass(c);
        } catch (CompletionFailure ex) {
            chk.completionError(pos, ex);
        }
    }

    public void attribCodeGen(DiagnosticPosition pos, ClassSymbol c) {
        try {
            annotate.flush();
            allowModify = true;
            attribClass(c);
        } catch (CompletionFailure ex) {
            chk.completionError(pos, ex);
        } finally {
            allowModify = false;
        }
    }

    /**
     * Attribute class definition associated with given class symbol.
     *
     * @param c The class symbol whose definition will be attributed.
     */
    void attribClass(ClassSymbol c) throws CompletionFailure {
        if (c.type.tag == ERROR) {
            return;
        }

        //ALEX: template

        if (c.type.isParameterized()) {
            return;
        }

        // Check for cycles in the inheritance graph, which can arise from
        // ill-formed class files.
        chk.checkNonCyclic(null, c.type);

        Type st = types.supertype(c.type);
        if ((c.flags_field & Flags.COMPOUND) == 0) {
            // First, attribute superclass.
            if (st.tag == CLASS) {
                attribClass((ClassSymbol) st.tsym);
            }

            // Next attribute owner, if it is a class.
            if (c.owner.kind == TYP && c.owner.type.tag == CLASS) {
                attribClass((ClassSymbol) c.owner);
            }
        }

        // The previous operations might have attributed the current class
        // if there was a cycle. So we test first whether the class is still
        // UNATTRIBUTED.
        if ((c.flags_field & UNATTRIBUTED) != 0) {
            c.flags_field &= ~UNATTRIBUTED;

            // Get environment current at the point of class definition.
            Env<AttrContext> env = enter.typeEnvs.get(c);

            // The info.lint field in the envs stored in enter.typeEnvs is deliberately uninitialized,
            // because the annotations were not available at the time the env was created. Therefore,
            // we look up the environment chain for the first enclosing environment for which the
            // lint value is set. Typically, this is the parent env, but might be further if there
            // are any envs created as a result of TypeParameter nodes.
            Env<AttrContext> lintEnv = env;
            while (lintEnv.info.lint == null) {
                lintEnv = lintEnv.next;
            }

            // Having found the enclosing lint value, we can initialize the lint value for this class
            env.info.lint = lintEnv.info.lint.augment(c.attributes_field, c.flags());

            Lint prevLint = chk.setLint(env.info.lint);
            JavaFileObject prev = log.useSource(c.sourcefile);

            try {
                // java.lang.Enum may not be subclassed by a non-enum
                if (st.tsym == syms.enumSym
                        && ((c.flags_field & (Flags.ENUM | Flags.COMPOUND)) == 0)) {
                    log.error(env.tree.pos(), "enum.no.subclassing");
                }

                // Enums may not be extended by source-level classes
                if (st.tsym != null
                        && ((st.tsym.flags_field & Flags.ENUM) != 0)
                        && ((c.flags_field & (Flags.ENUM | Flags.COMPOUND)) == 0)
                        && !target.compilerBootstrap(c)) {
                    log.error(env.tree.pos(), "enum.types.not.extensible");
                }
                attribClassBody(env, c);

                chk.checkDeprecatedAnnotation(env.tree.pos(), c);
            } finally {
                log.useSource(prev);
                chk.setLint(prevLint);
            }

        }
    }

    /**
     * Attribute domain definition associated with given domain symbol.
     *
     * @param c The domain symbol whose definition will be attributed.
     */
    void attribDomain(DomainSymbol c) throws CompletionFailure {
        if (c.type.tag == ERROR) {
            return;
        }

        // Check for cycles in the inheritance graph, which can arise from
        // ill-formed class files.
        chk.checkNonCyclic(null, c.type);
        /*
         Type st = types.supertype(c.type);
         if ((c.flags_field & Flags.COMPOUND) == 0) {
         // First, attribute superclass.
         if (st.tag == CLASS)
         attribClass((ClassSymbol)st.tsym);

         // Next attribute owner, if it is a class.
         if (c.owner.kind == TYP && c.owner.type.tag == CLASS)
         attribClass((ClassSymbol)c.owner);
         }

         */

        // The previous operations might have attributed the current class
        // if there was a cycle. So we test first whether the class is still
        // UNATTRIBUTED.
        if ((c.flags_field & UNATTRIBUTED) != 0) {
            c.flags_field &= ~UNATTRIBUTED;

            // Get environment current at the point of class definition.
            Env<AttrContext> env = enter.typeEnvs.get(c);

            // The info.lint field in the envs stored in enter.typeEnvs is deliberately uninitialized,
            // because the annotations were not available at the time the env was created. Therefore,
            // we look up the environment chain for the first enclosing environment for which the
            // lint value is set. Typically, this is the parent env, but might be further if there
            // are any envs created as a result of TypeParameter nodes.
            Env<AttrContext> lintEnv = env;
            while (lintEnv.info.lint == null) {
                lintEnv = lintEnv.next;
            }

            // Having found the enclosing lint value, we can initialize the lint value for this class
            env.info.lint = lintEnv.info.lint.augment(c.attributes_field, c.flags());

            Lint prevLint = chk.setLint(env.info.lint);
            JavaFileObject prev = log.useSource(c.sourcefile);

            try {
                attribDomainBody(env, c);

                chk.checkDeprecatedAnnotation(env.tree.pos(), c);
            } finally {
                log.useSource(prev);
                chk.setLint(prevLint);
            }

        }
    }

    public void visitImport(JCImport tree) {
        // nothing to do
    }

    /**
     * Finish the attribution of a class.
     */
    private void attribClassBody(Env<AttrContext> env, ClassSymbol c) {

        JCClassDecl tree = (JCClassDecl) env.tree;
        assert c == tree.sym;


        if ((tree.mods.flags & Flags.ATOMIC) != 0 && !tree.singular) {
            log.error(tree.pos,
                    "atomic.non.singular");
        }

        if ((tree.mods.flags & Flags.PACKED) != 0) {
            c.flags_field |= Flags.PACKED;
        }
        // Validate annotations
        chk.validateAnnotations(tree.mods.annotations, c);

        c.recAddRef(c.type, true);

        // Validate type parameters, supertype and interfaces.
        attribBounds(tree.typarams);
        chk.validate(tree.typarams, env);
        chk.validate(tree.extending, env);
        chk.validate(tree.implementing, env);

        if (tree.extending != null) {
            if ((tree.extending.type.tsym.flags_field & Flags.LINEAR) != 0 && (c.type.tsym.flags_field & Flags.LINEAR) == 0) {
                log.error(tree.pos, "derive.linear", c.name, tree.extending);
            }

            c.recAddRef(tree.extending.type, true);
            //tree.extending.type.tsym.
        }

        for (JCExpression e : tree.implementing) {
            c.recAddRef(e.type, true);
        }

        // If this is a non-abstract class, check that it has no abstract
        // methods or unimplemented methods of an implemented interface.
        if ((c.flags() & (ABSTRACT | INTERFACE)) == 0) {
            if (!relax) {
                chk.checkAllDefined(tree.pos(), c);
            }
        }

        if ((c.flags() & ANNOTATION) != 0) {
            if (tree.implementing.nonEmpty()) {
                log.error(tree.implementing.head.pos(),
                        "cant.extend.intf.annotation");
            }
            if (tree.typarams.nonEmpty()) {
                log.error(tree.typarams.head.pos(),
                        "intf.annotation.cant.have.type.params");
            }
        } else {
            // Check that all extended classes and interfaces
            // are compatible (i.e. no two define methods with same arguments
            // yet different return types).  (JLS 8.4.6.3)
            chk.checkCompatibleSupertypes(tree.pos(), c.type);
        }

        // Check that class does not import the same parameterized interface
        // with two different argument lists.
        chk.checkClassBounds(tree.pos(), c.type);

        tree.type = c.type;


        boolean assertsEnabled = false;
        assert assertsEnabled = true;
        if (assertsEnabled) {
            for (List<JCTypeParameter> l = tree.typarams;
                    l.nonEmpty(); l = l.tail) {
                assert env.info.scope.lookup(l.head.name).scope != null;
            }
        }
        /*
         // Check that a generic class doesn't extend Throwable
         if (!c.type.allparams().isEmpty() && types.isSubtype(c.type, syms.throwableType))
         log.error(tree.extending.pos(), "generic.throwable");
         */
        // Check that all methods which implement some
        // method conform to the method they implement.
        chk.checkImplementations(tree);

        for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
            // Attribute declaration
            attribStat(l.head, env);
            // Check that declarations in inner classes are not static (JLS 8.1.2)
            // Make an exception for static constants.
            if (c.owner.kind != PCK
                    && ((c.flags() & STATIC) == 0 || c.name == names.empty)
                    && (TreeInfo.flags(l.head) & (STATIC | INTERFACE)) != 0) {
                Symbol sym = null;
                if (l.head.getTag() == JCTree.VARDEF) {
                    sym = ((JCVariableDecl) l.head).sym;
                }
                if (sym == null
                        || sym.kind != VAR
                        || ((VarSymbol) sym).getConstValue() == null) {
                    log.error(l.head.pos(), "icls.cant.have.static.decl");
                }
            }
        }

        // Check for cycles among non-initial constructors.
        chk.checkCyclicConstructors(tree);

        // Check for cycles among annotation elements.
        chk.checkNonCyclicElements(tree);


        //FIXME:test following
        //when extendeing a class that contains pointers then we also contain pointers
        Symbol ext = TreeInfo.symbol(tree.extending);
        if (ext != null) {
            tree.sym.flags_field |= ext.flags_field & Flags.PARAMETER;
        }

        // Check for proper use of serialVersionUID
        if (env.info.lint.isEnabled(Lint.LintCategory.SERIAL)
                && isSerializable(c)
                && (c.flags() & Flags.ENUM) == 0
                && (c.flags() & ABSTRACT) == 0) {
            checkSerialVersionUID(tree, c);
        }
    }

    /**
     * Finish the attribution of a class.
     */
    private void attribDomainBody(Env<AttrContext> env, DomainSymbol c) {
        JCDomainDecl tree = (JCDomainDecl) env.tree;
        assert c == tree.sym;

        // Validate annotations
        //chk.validateAnnotations(tree.mods.annotations, c);

        // Validate type parameters, supertype and interfaces.
        /*
         attribBounds(tree.typarams);
         chk.validate(tree.typarams, env);
         chk.validate(tree.extending, env);
         chk.validate(tree.implementing, env);

         */

//            chk.checkCompatibleSupertypes(tree.pos(), c.type);

        // Check that class does not import the same parameterized interface
        // with two different argument lists.
//        chk.checkClassBounds(tree.pos(), c.type);

        tree.type = c.type;

        boolean assertsEnabled = false;
        assert assertsEnabled = true;
        if (assertsEnabled) {
            /*
             for (List<JCTypeParameter> l = tree.typarams;
             l.nonEmpty(); l = l.tail)
             assert env.info.scope.lookup(l.head.name).scope != null;

             */
        }

        // Check that all methods which implement some
        // method conform to the method they implement.
        //      chk.checkImplementations(tree);

        for (List<JCTree> l = tree.constraints; l.nonEmpty(); l = l.tail) {
            // Attribute declaration
            attribStat(l.head, env);
            // Check that declarations in inner classes are not static (JLS 8.1.2)
            // Make an exception for static constants.
            if (c.owner.kind != PCK
                    && ((c.flags() & STATIC) == 0 || c.name == names.empty)
                    && (TreeInfo.flags(l.head) & (STATIC | INTERFACE)) != 0) {
                Symbol sym = null;
                if (l.head.getTag() == JCTree.VARDEF) {
                    sym = ((JCVariableDecl) l.head).sym;
                }
                if (sym == null
                        || sym.kind != VAR
                        || ((VarSymbol) sym).getConstValue() == null) {
                    log.error(l.head.pos(), "icls.cant.have.static.decl");
                }
            }
        }
    }
    // where

    /**
     * check if a class is a subtype of Serializable, if that is available.
     */
    private boolean isSerializable(ClassSymbol c) {
        return false;
    }

    /**
     * Check that an appropriate serialVersionUID member is defined.
     */
    private void checkSerialVersionUID(JCClassDecl tree, ClassSymbol c) {

        // check for presence of serialVersionUID
        Scope.Entry e = c.members().lookup(names.serialVersionUID);
        while (e.scope != null && e.sym.kind != VAR) {
            e = e.next();
        }
        if (e.scope == null) {
            log.warning(tree.pos(), "missing.SVUID", c);
            return;
        }

        // check that it is static final
        VarSymbol svuid = (VarSymbol) e.sym;
        if ((svuid.flags() & (STATIC | FINAL))
                != (STATIC | FINAL)) {
            log.warning(TreeInfo.diagnosticPositionFor(svuid, tree), "improper.SVUID", c);
        } // check that it is long
        else if (svuid.type.tag != TypeTags.LONG) {
            log.warning(TreeInfo.diagnosticPositionFor(svuid, tree), "long.SVUID", c);
        } // check constant
        else if (svuid.getConstValue() == null) {
            log.warning(TreeInfo.diagnosticPositionFor(svuid, tree), "constant.SVUID", c);
        }
    }

    private Type capture(Type type) {
        return types.capture(type);
    }

    public boolean canProjectTo(ArrayType site, DomainType projDom) {
        if (!projDom.isBaseDomain) {
            Symbol sym = rs.testQualifiedMethod(
                    env, site, projDom.tsym.name, pt.getParameterTypes(), pt.getTypeArguments());

            return sym.kind < AMBIGUOUS;
        } else {
            Type dt = site;
            if (site.tag == ARRAY) {
                dt = site.dom;
            }
            // We are seeing a plain identifier as selector.
            Symbol sym = rs.findIdentInType(env, dt, projDom.tsym.name, pkind);
            return sym.kind < AMBIGUOUS;
        }
    }

    /**
     * Returns the result domain of projecting an array of type siteType to
     * domain projDom. Projection parameters are inferred if null is passed to
     * this function. If the projection is not possible, null is returned.
     * example: getProjectionResult(pos, int[two_d{4,4}], row, null) =
     * int[row{4,4}]
     */
    private ArrayType getProjectionResult(DiagnosticPosition pos, ArrayType site, DomainType projDom, List<JCExpression> projParams, List<JCExpression> projArgs) {

        int paramsNeeded = projDom.formalParams.length();
        ListBuffer<JCExpression> ps = new ListBuffer<JCExpression>();

        // no params given, we have to infer them
        if (projParams == null) {

            if (site.dom.isDynamic()) {
				for(JCExpression e:site.dom.appliedParams)
					if(e.type.constValue()!=null&&e.type.constValue() instanceof Integer&&((Integer)e.type.constValue())==-1)
					{
						log.error(pos, "domain.cannot.infer.param.inconsistent", projDom, "any");
						break;
					}
            }

            // get map of inferred parameters from parent
            Map<Name, JCExpression> parentParamMap = new LinkedHashMap<Name, JCExpression>();
            for (int i = 0; i < projDom.parentParams.length(); i++) {
                Name name = projDom.parentParams.get(i).name;
                if (site.dom.appliedParams.size() < i + 1) {
                    log.error(pos, "domain.cannot.infer.param.inconsistent", projDom, name);
                } else {
                    JCExpression value = site.dom.appliedParams.get(i);

                    if (parentParamMap.containsKey(name) && parentParamMap.get(name) != value) {
                        log.error(pos, "domain.cannot.infer.param.inconsistent", projDom, name);
                    } else {
                        parentParamMap.put(name, value);
                    }
                }
            }

            // generate params for new type
            for (int i = 0; i < projDom.formalParams.length(); i++) {
                Name p = projDom.formalParams.get(i).name;
                if (!parentParamMap.containsKey(p)) {
                    log.error(pos, "domain.cannot.infer.param", projDom, p);
                    ps.add(make.Literal(0));
                } else {
                    ps.add(parentParamMap.get(p));
                }
            }


        } // params given, ensure correctness
        else {

            // ensure that the right number of parameters is supplied
            if (projParams.length() != paramsNeeded) {
                log.error(pos, "domain.wrong.number.of.params", projDom);
            }

            // convert params to integers
            Map<Name, JCExpression> paramMap = new LinkedHashMap<Name, JCExpression>();
            if (projParams != null) {
                for (int i = 0; i < projParams.length(); i++) {
                    Type t = attribExpr(projParams.get(i), env);
                    Integer n = (Integer) t.constValue();
                    if (n != null) {
                        paramMap.put(projDom.formalParams.get(i).name, projParams.get(i));
                        ps.add(projParams.get(i));
                    }
                }
            }


            // test that parameters of projection and site match
            //FIXME: run time check for dyn array?
            if (!site.dom.isDynamic()) {
                JCExpression parameter[] = new JCExpression[projDom.parentParams.size()];
                int i = 0;
                for (VarSymbol vs : projDom.parentParams) {
                    if (!paramMap.containsKey(vs.name)) {
                        log.error(pos, "domain.not.a.parameter", projDom, vs);
                        parameter[i] = make.Literal(0);
                    } else {
                        parameter[i] = paramMap.get(vs.name);
                    }
                    i++;
                }

                int size = ((DomainType) projDom.parentDomain).getCard(pos, parameter,true);
                int real_size = projDom.getCard(pos, site.dom.appliedParams.toArray(new JCExpression[0]),true);
                if (size != real_size) {
                    log.error(pos, "domain.invalid.projection.parameters", projDom, "(size " + size + ", required " + real_size + ")");
                }
            } else {
                for (JCExpression e : projParams) {
                    ps.add(e);
                }
            }
            /*
             if(!site.dom.isDynamic())
             for(int i = 0; i < projDom.parentParams.length(); i++) {
             VarSymbol param = projDom.parentParams.get(i);
             if(! paramMap.containsKey(param.name)) {
             log.error(pos, "domain.not.a.parameter", projDom, param);
             } else {
             int projValue = paramMap.get(param.name);
             if(site.dom.appliedParams.size()<i+1)
             log.error(pos, "domain.invalid.projection.parameter", projDom, param);
             else
             {
             int siteValue = site.dom.appliedParams.get(i);
             if(projValue != siteValue) {
             log.error(pos, "domain.invalid.projection.parameter", projDom, param);
             }
             }
             }
             }
             */


        }

        // create new domain type to change parameters
        DomainType newdt = (DomainType) projDom.clone();
        newdt.appliedParams = ps.toList();
		newdt.appliedArgs = projArgs;

        // check that the domain is valid
        isAppliedDomainValid(pos, newdt);

        calcResultParams(newdt);


        // create array type, from element type, domain type and parameters

        ArrayType res;

        if(site.dom.isDynamic()==newdt.isDynamic())
                res = new ArrayType(site.elemtype, newdt, syms.arrayClass,site);
        else
                res = new ArrayType(site.elemtype, newdt, syms.arrayClass);

        //Flags.ABSTRACT is used by getRealType() (in ArrayType) to move backwards in the select chain to th next projection
        //if the projection can be accessed exactly like the real type, then we omit the "stop" so we do not produce tons of unnecessary Projection(X,0)
        //if(!res.isCompatibleToRealTypeAndIterable(jc, site.dom, pos))
        res.type_flags_field |= Flags.ABSTRACT; //mark as projection!
        return res;

    }

    /**
     * Returns the result domain of casting an array of type siteType to domain
     * castDom. example: getCastResult(pos, int[row{4,4}], one_d) =
     * int[one_d{4}]
     */
    private ArrayType getCastResult(DiagnosticPosition pos, ArrayType siteType, DomainType castDom, List<JCExpression> projParams) {

        DomainType siteDom = siteType.dom;

        boolean dynargs = true;
        // create map from parameters to values
        final Map<Name, JCExpression> paramMap = new LinkedHashMap<Name, JCExpression>();
        for (int i = 0; i < siteDom.formalParams.length(); i++) {
            if (siteDom.appliedParams.get(i).type.constValue() == null || (Integer) siteDom.appliedParams.get(i).type.constValue() != -1) {
                dynargs = false;
            }
            paramMap.put(siteDom.formalParams.get(i).name, siteDom.appliedParams.get(i));
        }

        // to calculate a result parameter, we replace all known variables
        // by their values and the use the normal constant folding
        TreeCopier<Object> copier = new TreeCopier<Object>(make) {
            public JCTree visitIdentifier(IdentifierTree node, Object p) {
                JCIdent t = (JCIdent) node;
                if (paramMap.containsKey(t.name)) {
                    return paramMap.get(t.name);
                } else {
                    return super.visitIdentifier(node, p);
                }
            }
        };

        DomainType resdom = (DomainType) castDom.clone();

        ListBuffer<JCExpression> ps = new ListBuffer<JCExpression>();
        // calculate result domain parameters
        if (!siteDom.isDynamic()) {
            if (projParams != null) {
                for (JCExpression e : projParams) {
                    ps.add(e);
                }
            } else {
				if (siteDom.resultDom != null && !((DomainType) siteDom.resultDom).tsym.name.equals(resdom.tsym.name) && projParams == null) {
						//log.error(pos, "explicit.dom.params.required", siteDom, castDom);
						int size []=castDom.getSize(pos, siteDom.appliedParams.toArray(new JCExpression [0]));
						for (int val : size) {
							ps.add(make.Literal(val)); // error
						}
				}
				else
				{
					Env<AttrContext> localEnv =
						env.dup(null, env.info.dup(env.info.scope.dup()));

					if(siteDom.projectionArgs!=null)
					{

						for(VarSymbol vs:siteDom.projectionArgs)
						{
							vs.flags_field|=HASINIT|FINAL|TASKLOCAL;
							localEnv.info.scope.enter(vs);
						}
					}

					for (JCExpression resParam : siteDom.resultDomParams) {
						JCExpression copiedResParam = copier.copy(resParam);
						//copiedResParam.pos=pos.getPreferredPosition();
						int ec=jc.errorCount();
						Type t = attribExpr(copiedResParam, localEnv);

						if(ec!=jc.errorCount())
							log.error(pos, "domain.invalid.result.parameter.expr", castDom, resParam);

						Integer value = (Integer) t.constValue();
						if (value != null) {
							if(value==-1)
								log.error(pos, "domain.invalid.result.parameter.expr", castDom, resParam);
							ps.add(copiedResParam);
						} else {
							ps.add(copiedResParam); // error
							//log.error(pos, "domain.invalid.result.parameter.expr", castDom, resParam);
						}
					}

		            localEnv.info.scope.leave();
				}
            }

        } else {
            if (projParams != null) {
                for (JCExpression e : projParams) {
                    ps.add(e);
                }
            } else {
				Env<AttrContext> localEnv =
					env.dup(null, env.info.dup(env.info.scope.dup()));

				if(siteDom.projectionArgs!=null)
				{

					for(VarSymbol vs:siteDom.projectionArgs)
					{
						vs.flags_field|=HASINIT|FINAL|TASKLOCAL;
						localEnv.info.scope.enter(vs);
					}
				}


                for (JCExpression resParam : siteDom.resultDomParams) {
                    if (dynargs) {
                        ps.add(make.Literal(-1));
                    } else {
                        JCExpression copiedResParam = copier.copy(resParam);
                        Type t = attribExpr(copiedResParam, env);
                        ps.add(copiedResParam);
                    }
                }

	            localEnv.info.scope.leave();

            }
        }
        resdom.appliedParams = ps.toList();

        // copy result domain to add parameters

        // a projection is just another interpretation of a base domain!
        resdom.isBaseDomain = siteType.dom.isBaseDomain;

        // check that the domain is valid
        isAppliedDomainValid(pos, resdom);

        // test if site and result are compatible
        isDomainCastable(pos, siteType.dom, resdom);

        // make new array type from element type, domain an parameters
        ArrayType res= new ArrayType(siteType.elemtype, resdom, syms.arrayClass, siteType);

		//res.type_flags_field|=Flags.COMPOUND;
		//res.toString();

		return res;
    }

    private static enum ProjIndexClass {

        BOUND, FREE
    };

    private Map<VarSymbol, Integer> getBoundIndices(List<DomainConstraint> cs) {

        // fill all vars into map
        Map<VarSymbol, Integer> boundValues = new LinkedHashMap<VarSymbol, Integer>();

        // use constraints to find more restricted or bound vars
        boolean changed = true;
        while (changed) {
            changed = false;

            // use all constraints
            for (DomainConstraint c : cs) {

                // only EQ constraints can make BOUND vars
                if (!c.eq) {
                    continue;
                }

                // count number of bound vars
                int numNotBound = 0;
                VarSymbol notBoundVar = null;
                for (VarSymbol v : c.coeffs.keySet()) {
                    if (!boundValues.containsKey(v)) {
                        numNotBound++;
                        notBoundVar = v;
                    }
                }

                // if constraint is EQ and all vars except one are bound -> all vars are bound
                if (numNotBound == 1) {

                    // calulate value of new bound var
                    int res = c.constant;
                    for (VarSymbol v : c.coeffs.keySet()) {
                        if (boundValues.containsKey(v)) {
                            for (Pair<Integer, VarSymbol> p : c.coeffs.get(v)) {
                                res -= p.fst * boundValues.get(v);
                            }
                        }
                    }

                    // add new var with value
                    boundValues.put(notBoundVar, res);
                    changed = true;

                }
            }

        }

        return boundValues;
    }

    /**
     * Test if a domain is castable into another domain. This does not check the
     * declared "result-type". Instead the structure is combared. (e.g. is
     * row{4,5} castable to one_d{5} )
     */
    private boolean isDomainCastable(DiagnosticPosition pos, DomainType site, DomainType goal) {

        //use barvinok to check if card site==card goal
        boolean result= site.canCastTo(pos, site.appliedParams.toArray(new JCExpression[0]), goal, goal.appliedParams.toArray(new JCExpression[0]));

		if(!result)
		{
			String siteSize=site.getStringVal(pos, site.getCardString(pos, site.appliedParams.toArray(new JCExpression[0]), false),false);
			String goalSize=goal.getStringVal(pos, goal.getCardString(pos, goal.appliedParams.toArray(new JCExpression[0]), false),false);
			log.error(pos, "domain.cast.to.non.equivalent.domain", site,siteSize, goal,goalSize);
		}

		return result;

        /*
         // Seperate vars of site into classes.
         Map<VarSymbol, ProjIndexClass> projIdxClasses = getProjectionIndexClasses(site);
         ListBuffer<VarSymbol> freeVarsBuf = new ListBuffer<VarSymbol>();

         // order of indices has to be preserved
         for(VarSymbol v : site.indices) {
         if(! boundVars.containsKey(v))
         freeVarsBuf.add(v);
         }
         List<VarSymbol> siteFreeVars = freeVarsBuf.toList();

         // Number of free indices must match the number of indices in the goal domain
         if(siteFreeVars.length() != goal.indices.length()) {
         log.error(pos, "domain.cast.to.non.equivalent.domain", site, goal);
         return false;
         }

         // Remove bound vars from constraints and replace them with their values
         ListBuffer<DomainConstraint> csWithoutBoundVars = new ListBuffer<DomainConstraint>();
         for(DomainConstraint c : siteConstr) {
         DomainConstraint newconstr = c.deepCopy();
         newconstr.coeffs = new LinkedHashMap<VarSymbol, Integer>();
         for(VarSymbol v : c.coeffs.keySet()) {
         if(boundVars.containsKey(v)) {
         newconstr.constant -= c.coeffs.get(v) * boundVars.get(v);
         } else {
         newconstr.coeffs.put(v, c.coeffs.get(v));
         }
         }
         csWithoutBoundVars.add(newconstr);
         }
         siteConstr = csWithoutBoundVars.toList();

         // Remove constraints which contain only constants.
         // This chacks also for incosistencies. (1 = 0 etc.)
         siteConstr = Domains.removeConstantConstraints(siteConstr);
         if(siteConstr == null) {
         log.error(pos, "domain.cast.to.non.equivalent.domain", site, goal);
         return false;
         }

         // Move the domain as close as possible to the origin.
         siteConstr = Domains.moveDomainToOrigin(siteFreeVars, siteConstr);
         if(siteConstr == null) {
         log.error(pos, "domain.cast.to.non.equivalent.domain", site, goal);
         return false;
         }

         // create mappings between site and goal indices
         // (siteFreeVars is still ordered)
         Map<VarSymbol, VarSymbol> siteToGoalVars = new LinkedHashMap<VarSymbol, VarSymbol>();
         Map<VarSymbol, VarSymbol> goalToSiteVars = new LinkedHashMap<VarSymbol, VarSymbol>();
         for(int i = 0; i < siteFreeVars.length(); i++) {
         VarSymbol siteVar = siteFreeVars.get(i);
         VarSymbol goalVar = goal.indices.get(i);
         siteToGoalVars.put(siteVar, goalVar);
         goalToSiteVars.put(goalVar, siteVar);
         }

         // use siteToGoalMap to convert costraints to goal vars.
         // this is needed to compare them
         ListBuffer<DomainConstraint> convSiteConstrBuf = new ListBuffer<DomainConstraint>();
         for(DomainConstraint c : siteConstr) {

         DomainConstraint conv = c.deepCopy();
         conv.coeffs = new LinkedHashMap<VarSymbol, Integer>();

         for(VarSymbol v : c.coeffs.keySet()) {
         VarSymbol convVar = siteToGoalVars.get(v);
         int coeffVal = c.coeffs.get(v);
         conv.coeffs.put(convVar, coeffVal);
         }

         convSiteConstrBuf.add(conv);

         }

         // get real constraints of the domain
         // (applied parameters are already replaced by thei values here)
         List<DomainConstraint> goalConstr = goal.getRealConstraints();

         // remove constraints which contain only constans
         goalConstr = Domains.removeConstantConstraints(goalConstr);
         if(goalConstr == null) {
         log.error(pos, "domain.cast.to.non.equivalent.domain", site, goal);
         return false;
         }

         // move the domain as close as possible to the origin
         goalConstr = Domains.moveDomainToOrigin(goal.indices, goalConstr);
         if(siteConstr == null) {
         log.error(pos, "domain.cast.to.non.equivalent.domain", site, goal);
         return false;
         }

         // compare constraints
         if(! Domains.areDomainsEqual(goal.indices, convSiteConstrBuf.toList(), goalConstr)) {
         log.error(pos, "domain.cast.to.non.equivalent.domain", site, goal);
         return false;
         }

         return true;
         */
    }

    /**
     * Checks if a domain with applied params is valid. This checks, that - no
     * variable is unbonded - no constraint is violated
     */
    private boolean isAppliedDomainValid(DiagnosticPosition pos, DomainType dt) {

        if (dt.isDynamic()) {
            return true;
        }

        // domain has to be finite
        List<VarSymbol> vars = dt.indices;
        if (dt.projectionArgs != null) {
            vars = vars.appendList(dt.projectionArgs); // do these have to bebound, too?
        }
        /*
         if(! Domains.areAllIndicesBound(dt.getRealConstraints(), vars)) {
         log.error(pos, "domain.not.finite", dt);
         return false;
         }
         */

        // test that no constraints are violated
        if (!dt.isDynamic() && dt.constraints != null) {
            Map<Name, JCExpression> varenv = new LinkedHashMap<Name, JCExpression>();
            for (int i = 0; i < dt.formalParams.length(); i++) {
                varenv.put(dt.formalParams.get(i).name, dt.appliedParams.get(i));
            }
            for (DomainConstraint c : dt.constraints) {
                if (!Domains.checkConstraint(c, varenv)) {
                    log.error(pos, "domain.constraint.violated", dt, c);
                    return false;
                }
            }
        }

        return true;

    }
}
