package com.sun.tools.javac.code;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree;

import com.sun.tools.javac.main.JavaCompiler;

import java.util.LinkedHashMap;
import java.util.regex.*;
import java.util.Arrays;
import javax.lang.model.type.*;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.BoundKind.*;
import static com.sun.tools.javac.code.TypeTags.*;
import com.sun.tools.javac.ibarvinok.ibarvinok;

import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.TreeTranslator;
import java.util.Map;

import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Set;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/** This class represents Java types. The class itself defines the behavior of
 *  the following types:
 *  <pre>
 *  base types (tags: BYTE, CHAR, GROUP, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN),
 *  type `void' (tag: VOID),
 *  the bottom type (tag: BOT),
 *  the missing type (tag: NONE).
 *  </pre>
 *  <p>The behavior of the following types is defined in subclasses, which are
 *  all static inner classes of this class:
 *  <pre>
 *  class types (tag: CLASS, class: ClassType),
 *  array types (tag: ARRAY, class: ArrayType),
 *  method types (tag: METHOD, class: MethodType),
 *  package types (tag: PACKAGE, class: PackageType),
 *  type variables (tag: TYPEVAR, class: TypeVar),
 *  type arguments (tag: WILDCARD, class: WildcardType),
 *  polymorphic types (tag: FORALL, class: ForAll),
 *  the error type (tag: ERROR, class: ErrorType).
 *  </pre>
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 *  @see TypeTags
 */
public class Type implements PrimitiveType,Cloneable {

    /** Constant type: no type at all. */
    public static final JCNoType noType = new JCNoType(NONE);

    /** If this switch is turned on, the names of type variables
     *  and anonymous classes are printed with hashcodes appended.
     */
    public static boolean moreInfo = false;

	public long type_flags_field = 0;

	public Type original;

	public Type addFlag(long flag)
	{
		//tsym.complete(); //guarantee that type is complete before cloning
		Type t=(Type)clone();
		t.type_flags_field|=flag;
		t.original = original;
		return t;
	}

	public Type getArrayType()
	{
/*        
        if(toString().equals("String"))
        {            
            return new ArrayType(JavaCompiler.getCompiler().syms.charType,null,JavaCompiler.getCompiler().syms.arrayClass);
        }
*/
		if(tag!=TypeTags.ARRAY)
		{
			Type t;
			if(tag==TypeTags.TYPEVAR)
				t= ((TypeVar)this).bound;
			else if(tag==TypeTags.CLASS&&((ClassType)original).supertype_field!=null)
			{
				t= ((ClassType)original).supertype_field.getArrayType();
				if(t.tag!=TypeTags.ARRAY)
					t=this;
			}
			else
				t=this;

			if(t!=this&&this.type_flags_field!=0)
				return t.addFlag(type_flags_field);//retain uniqueness
			else
				return t;
		}
		return this;
	}

	public boolean isCast()
	{
		return (type_flags_field&Flags.HASINIT)!=0;
	}

	public boolean isProjection()
	{
        if(!(this instanceof ArrayType))
            return false;
        
		ArrayType at=(ArrayType)this;
		if(at.dom.projectionArgs!=null&&!at.dom.projectionArgs.isEmpty())
			return true;

		return (type_flags_field&Flags.BLOCK)!=0;
	}

	public boolean useIndirection()
	{
		return !(isProjection()||!((ArrayType)this).getRealType().treatAsBaseDomain());
	}

	public boolean containsPointer()
	{
		return getArrayType().tag==TypeTags.ARRAY||isPointer()||this.tsym.containsPointer();
	}

	public boolean isPointer()
	{
		//primitive types (int, etc) should only be pointers if they are used as return values
		//objects should always be passed as pointers
		return (type_flags_field&Flags.FOUT)==Flags.FOUT;
	}

	public boolean isSingular()
	{
		//primitive types (int, etc) should only be pointers if they are used as return values
		//objects should always be passed as pointers
		return (tsym.flags_field&Flags.SINGULAR)!=0;
	}

	public boolean isLinear()
	{
		//primitive types (int, etc) should only be pointers if they are used as return values
		//objects should always be passed as pointers
		return (type_flags_field&Flags.LINEAR)!=0;
	}

	public boolean isReadLinear()
	{
		//primitive types (int, etc) should only be pointers if they are used as return values
		//objects should always be passed as pointers
		return (type_flags_field&Flags.LINEARREAD)!=0;
	}

	public String toPrettyString()
	{
		return toString();
	}

    /** CTP
     *
     */
    public LinkedHashMap<Name, JCTree.JCExpression> lookup;

    /** The tag of this type.
     *
     *  @see TypeTags
     */
    public int tag;

    /** The defining class / interface / package / type variable
     */
    public TypeSymbol tsym;

    /**
     * The constant value of this type, null if this type does not
     * have a constant value attribute. Only primitive types and
     * strings (ClassType) can have a constant value attribute.
     * @return the constant value attribute of this type
     */
    public Object constValue() {
        return null;
    }

    public <R,S> R accept(Type.Visitor<R,S> v, S s) { return v.visitType(this, s); }

    /** Define a type given its tag and type symbol
     */
    public Type(int tag, TypeSymbol tsym) {
		this.original = this;
        this.tag = tag;
        this.tsym = tsym;
        this.lookup = new LinkedHashMap<Name, JCTree.JCExpression>();
    }

    public void AddCTP(Name name,JCTree.JCExpression exp)
    {
        lookup.put(name, exp);
    }

    public void AddSetCTP(Name name,JCTree.JCExpression exp)
    {
        JCTree.JCExpression set=GetCTP(name);
        if(set==null)
        {
            set=new JCTree.JCSet();
            AddCTP(name,set);
        }
        else if(set.getTag()!=JCTree.SET)
        {
            //error!
            return;
        }

        ((JCTree.JCSet)set).content.add(exp);
    }

    public JCTree.JCExpression GetCTP(Name name)
    {
        return lookup.get(name);
    }

    /** An abstract class for mappings from types to types
     */
    public static abstract class Mapping {
        private String name;
        public Mapping(String name) {
            this.name = name;
        }
        public abstract Type apply(Type t);
        public String toString() {
            return name;
        }
    }

	public static abstract class LookupMapping {
        private String name;
        public LookupMapping(String name) {
            this.name = name;
        }
        public abstract Type apply(Type t,java.util.Map<String,UndetVar> map);
        public String toString() {
            return name;
        }
    }

    /** map a type function over all immediate descendants of this type
     */
    public Type map(Mapping f) {
        return this;
    }

	public Type map(LookupMapping f) {
        return this;
    }

    /** map a type function over a list of types
     */
    public static List<Type> map(List<Type> ts, Mapping f) {
        if (ts.nonEmpty()) {
            List<Type> tail1 = map(ts.tail, f);
            Type t = f.apply(ts.head);
            if (tail1 != ts.tail || t != ts.head)
                return tail1.prepend(t);
        }
        return ts;
    }

	public static List<Type> map(List<Type> ts, LookupMapping f,java.util.Map<String,UndetVar> map) {
        if (ts.nonEmpty()) {
            List<Type> tail1 = map(ts.tail, f,map);
            Type t = f.apply(ts.head,map);
            if (tail1 != ts.tail || t != ts.head)
                return tail1.prepend(t);
        }
        return ts;
    }

	public boolean isConst()
	{
		if(tag==BOT)
			return true;
		return constValue()!=null;
	}

    /** Define a constant type, of the same kind as this type
     *  and with given constant value
     */
    public Type constType(Object constValue) {
        final Object value = constValue;
        assert tag <= BOOLEAN;
        return new Type(tag, tsym) {
                @Override
                public Object constValue() {
                    return value;
                }
                @Override
                public Type baseType() {
                    return tsym.type;
                }
            };
    }

    /**
     * If this is a constant type, return its underlying type.
     * Otherwise, return the type itself.
     */
    public Type baseType() {
        return this;
    }

    /** Return the base types of a list of types.
     */
    public static List<Type> baseTypes(List<Type> ts) {
        if (ts.nonEmpty()) {
            Type t = ts.head.baseType();
            List<Type> baseTypes = baseTypes(ts.tail);
            if (t != ts.head || baseTypes != ts.tail)
                return baseTypes.prepend(t);
        }
        return ts;
    }

    /** The Java source which this type represents.
     */
    public String toString() {
        String s="";
		if((type_flags_field&Flags.UNSIGNED)!=0)
			s+="unsigned ";

		s+= (tsym == null || tsym.name == null)
            ? "<none>"
            : tsym.name.toString();
        if (moreInfo && tag == TYPEVAR) s = s + hashCode();

        return s;
    }

	public String toString(String varname) {
        return toString();
    }

    /**
     * The Java source which this type list represents.  A List is
     * represented as a comma-spearated listing of the elements in
     * that list.
     */
    public static String toString(List<Type> ts) {
        if (ts.isEmpty()) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer();
            buf.append(ts.head.toString());
            for (List<Type> l = ts.tail; l.nonEmpty(); l = l.tail)
                buf.append(",").append(l.head.toString());
            return buf.toString();
        }
    }

    /**
     * The constant value of this type, converted to String
     */
    public String stringValue() {
        assert constValue() != null;
        if (tag == BOOLEAN)
            return ((Integer) constValue()).intValue() == 0 ? "false" : "true";
        else if (tag == CHAR)
            return String.valueOf((char) ((Integer) constValue()).intValue());
        else
            return constValue().toString();
    }

    /**
     * This method is analogous to isSameType, but weaker, since we
     * never complete classes. Where isSameType would complete a
     * class, equals assumes that the two types are different.
     */
    public boolean equals(Object t) {
        return super.equals(t);
    }

    public int hashCode() {
        return super.hashCode();
    }

    /** Is this a constant type whose value is false?
     */
    public boolean isFalse() {
        return
            tag == BOOLEAN &&
            constValue() != null &&
            ((Integer)constValue()).intValue() == 0;
    }

    /** Is this a constant type whose value is true?
     */
    public boolean isTrue() {
        return
            tag == BOOLEAN &&
            constValue() != null &&
            ((Integer)constValue()).intValue() != 0;
    }

    public String argtypes(boolean varargs) {
        List<Type> args = getParameterTypes();
        if (!varargs) return args.toString();
        StringBuffer buf = new StringBuffer();
        while (args.tail.nonEmpty()) {
            buf.append(args.head);
            args = args.tail;
            buf.append(',');
        }
        if (args.head.tag == ARRAY) {
            buf.append(((ArrayType)args.head).elemtype);
            buf.append("...");
        } else {
            buf.append(args.head);
        }
        return buf.toString();
    }

    /** Access methods.
     */
    public List<Type>        getTypeArguments()  { return List.nil(); }
    public Type              getEnclosingType() { return null; }
    public List<Type>        getParameterTypes() { return List.nil(); }
    public Type              getReturnType()     { return null; }
    public void              setReturnType(Type t)     { return ; }
    public List<Type>        getThrownTypes()    { return List.nil(); }
    public Type              getUpperBound()     { return null; }
    public Type              getLowerBound()     { return null; }

	public Type		getSafeUpperBound()
	{
		Type ub=getUpperBound();
		if(ub==null)
			ub=this;
		return ub;
	}

	public Type		getSafeLowerBound()
	{
		Type ub=getLowerBound();
		if(ub==null)
			ub=this;
		return ub;
	}

    public void setThrown(List<Type> ts) {
        throw new AssertionError();
    }

    /** Navigation methods, these will work for classes, type variables,
     *  foralls, but will return null for arrays and methods.
     */

   /** Return all parameters of this type and all its outer types in order
    *  outer (first) to inner (last).
    */
    public List<Type> allparams() { return List.nil(); }

    /** Does this type contain "error" elements?
     */
    public boolean isErroneous() {
        return false;
    }

    public static boolean isErroneous(List<Type> ts) {
		if(ts==null)
			return true;
        for (List<Type> l = ts; l.nonEmpty(); l = l.tail)
            if (l.head.isErroneous()) return true;
        return false;
    }

    /** Is this type parameterized?
     *  A class type is parameterized if it has some parameters.
     *  An array type is parameterized if its element type is parameterized.
     *  All other types are not parameterized.
     */
    public boolean isParameterized() {
        return false;
    }

    /** Is this type a raw type?
     *  A class type is a raw type if it misses some of its parameters.
     *  An array type is a raw type if its element type is raw.
     *  All other types are not raw.
     *  Type validation will ensure that the only raw types
     *  in a program are types that miss all their type variables.
     */
    public boolean isRaw() {
        return false;
    }

    public boolean isCompound() {
        return tsym.completer == null
            // Compound types can't have a completer.  Calling
            // flags() will complete the symbol causing the
            // compiler to load classes unnecessarily.  This led
            // to regression 6180021.
            && (tsym.flags() & COMPOUND) != 0;
    }

    public boolean isInterface() {
        return (tsym.flags() & INTERFACE) != 0;
    }

    public boolean isPrimitive() {
        return tag <= VOID;
    }

    /**
     * Does this type contain occurrences of type t?
     */
    public boolean contains(Type t) {
        return t == this;
    }

    public static boolean contains(List<Type> ts, Type t) {
        for (List<Type> l = ts;
             l.tail != null /*inlined: l.nonEmpty()*/;
             l = l.tail)
            if (l.head.contains(t)) return true;
        return false;
    }

    /** Does this type contain an occurrence of some type in `elems'?
     */
    public boolean containsSome(List<Type> ts) {
        for (List<Type> l = ts; l.nonEmpty(); l = l.tail)
            if (this.contains(ts.head)) return true;
        return false;
    }

    public boolean isSuperBound() { return false; }
    public boolean isExtendsBound() { return false; }
    public boolean isUnbound() { return false; }
    public Type withTypeVar(Type t) { return this; }

    /** The underlying method type of this type.
     */
    public MethodType asMethodType() { throw new AssertionError(); }

    /** Complete loading all classes in this type.
     */
    public void complete() {}

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public TypeSymbol asElement() {
        return tsym;
    }

    public TypeKind getKind() {
        switch (tag) {
        case BYTE:      return TypeKind.BYTE;
        case CHAR:      return TypeKind.CHAR;
        case GROUP:     return TypeKind.GROUP;
        case SHORT:     return TypeKind.SHORT;
        case INT:       return TypeKind.INT;
        case LONG:      return TypeKind.LONG;
        case FLOAT:     return TypeKind.FLOAT;
        case DOUBLE:    return TypeKind.DOUBLE;
        case BOOLEAN:   return TypeKind.BOOLEAN;
        case VOID:      return TypeKind.VOID;
        case BOT:       return TypeKind.NULL;
        case NONE:      return TypeKind.NONE;
        default:        return TypeKind.OTHER;
        }
    }

    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        if (isPrimitive())
            return v.visitPrimitive(this, p);
        else
            throw new AssertionError();
    }

    public static class WildcardType extends Type
            implements javax.lang.model.type.WildcardType {

        public Type type;
        public BoundKind kind;
        public TypeVar bound;

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitWildcardType(this, s);
        }

        public WildcardType(Type type, BoundKind kind, TypeSymbol tsym) {
            super(WILDCARD, tsym);
            assert(type != null);
            this.kind = kind;
            this.type = type;
        }
        public WildcardType(WildcardType t, TypeVar bound) {
            this(t.type, t.kind, t.tsym, bound);
        }

        public WildcardType(Type type, BoundKind kind, TypeSymbol tsym, TypeVar bound) {
            this(type, kind, tsym);
            this.bound = bound;
        }

        public boolean isSuperBound() {
            return kind == SUPER ||
                kind == UNBOUND;
        }
        public boolean isExtendsBound() {
            return kind == EXTENDS ||
                kind == UNBOUND;
        }
        public boolean isUnbound() {
            return kind == UNBOUND;
        }

        public Type withTypeVar(Type t) {
            //-System.err.println(this+".withTypeVar("+t+");");//DEBUG
            if (bound == t)
                return this;
            bound = (TypeVar)t;
            return this;
        }

        boolean isPrintingBound = false;
        public String toString() {
            StringBuffer s = new StringBuffer();
            s.append(kind.toString());
            if (kind != UNBOUND)
                s.append(type);
            if (moreInfo && bound != null && !isPrintingBound)
                try {
                    isPrintingBound = true;
                    s.append("{:").append(bound.bound).append(":}");
                } finally {
                    isPrintingBound = false;
                }
            return s.toString();
        }

        public Type map(Mapping f) {
            //- System.err.println("   (" + this + ").map(" + f + ")");//DEBUG
            Type t = type;
            if (t != null)
                t = f.apply(t);
            if (t == type)
                return this;
            else
                return new WildcardType(t, kind, tsym, bound);
        }

        public Type getExtendsBound() {
            if (kind == EXTENDS)
                return type;
            else
                return null;
        }

        public Type getSuperBound() {
            if (kind == SUPER)
                return type;
            else
                return null;
        }

        public TypeKind getKind() {
            return TypeKind.WILDCARD;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitWildcard(this, p);
        }
    }

    public static class ClassType extends Type implements DeclaredType {

        /** The enclosing type of this type. If this is the type of an inner
         *  class, outer_field refers to the type of its enclosing
         *  instance class, in all other cases it referes to noType.
         */
        private Type outer_field;

        /** The type parameters of this type (to be set once class is loaded).
         */
        public List<Type> typarams_field;

        /** A cache variable for the type parameters of this type,
         *  appended to all parameters of its enclosing class.
         *  @see #allparams
         */
        public List<Type> allparams_field;

        /** The supertype of this class (to be set once class is loaded).
         */
        public Type supertype_field;

        /** The interfaces of this class (to be set once class is loaded).
         */
        public List<Type> interfaces_field;

		public String template = null;

        public ClassType(Type outer, List<Type> typarams, TypeSymbol tsym) {
            super(CLASS, tsym);
            this.outer_field = outer;
            this.typarams_field = typarams;
            this.allparams_field = null;
            this.supertype_field = null;
            this.interfaces_field = null;
            /*
            // this can happen during error recovery
            assert
                outer.isParameterized() ?
                typarams.length() == tsym.type.typarams().length() :
                outer.isRaw() ?
                typarams.length() == 0 :
                true;
            */
        }

		public Map<String,Type> cache = new LinkedHashMap<String, Type>();

		public JCTree.JCClassDecl tree = null;

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitClassType(this, s);
        }

        public Type constType(Object constValue) {
            final Object value = constValue;
            Type t= new ClassType(getEnclosingType(), typarams_field, tsym) {
                    @Override
                    public Object constValue() {
                        return value;
                    }
                    @Override
                    public Type baseType() {
                        return tsym.type;
                    }
                };
			t.type_flags_field=type_flags_field;
			return t;
        }

		public String toPrettyString()
		{
			if(template!=null)
				return template;
			else
				return toString();
		}

        /** The Java source which this type represents.
         */
        public String toString() {
            StringBuffer buf = new StringBuffer();
            if (getEnclosingType().tag == CLASS && tsym.owner.kind == TYP&&false) {
                buf.append(getEnclosingType().toString());
                buf.append(".");
                buf.append(className(tsym, false));
            } else {
                buf.append(className(tsym, false));
            }
            if (getTypeArguments().nonEmpty()) {
                buf.append('<');
                buf.append(getTypeArguments().toString());
                buf.append(">");
            }
            return buf.toString();
        }
//where
            private String className(Symbol sym, boolean longform) {
                if (sym.name.isEmpty() && (sym.flags() & COMPOUND) != 0) {
                    StringBuffer s = new StringBuffer(supertype_field.toString());
                    for (List<Type> is=interfaces_field; is.nonEmpty(); is = is.tail) {
                        s.append("&");
                        s.append(is.head.toString());
                    }
                    return s.toString();
                } else if (sym.name.isEmpty()) {
                    String s;
                    ClassType norm = (ClassType) tsym.type;
                    if (norm == null) {
                        s = Log.getLocalizedString("anonymous.class", (Object)null);
                    } else if (norm.interfaces_field != null && norm.interfaces_field.nonEmpty()) {
                        s = Log.getLocalizedString("anonymous.class",
                                                   norm.interfaces_field.head);
                    } else {
                        s = Log.getLocalizedString("anonymous.class",
                                                   norm.supertype_field);
                    }
                    if (moreInfo)
                        s += String.valueOf(sym.hashCode());
                    return s;
                } else if (longform) {
                    return sym.getQualifiedName().toString();
                } else {
                    return sym.name.toString();
                }
            }

        public List<Type> getTypeArguments() {
            if (typarams_field == null) {
                complete();
                if (typarams_field == null)
                    typarams_field = List.nil();
            }
            return typarams_field;
        }

        public boolean hasErasedSupertypes() {
            return isRaw();
        }

        public Type getEnclosingType() {
            return outer_field;
        }

        public void setEnclosingType(Type outer) {
            outer_field = outer;
        }

        public List<Type> allparams() {
            if (allparams_field == null) {
                allparams_field = getTypeArguments().prependList(getEnclosingType().allparams());
            }
            return allparams_field;
        }

		public Type getSuper()
		{
			return supertype_field;
		}

        public boolean isErroneous() {
            return
                getEnclosingType().isErroneous() ||
                isErroneous(getTypeArguments()) ||
                this != tsym.type && tsym.type.isErroneous();
        }

        public boolean isParameterized() {
            return allparams().tail != null;
            // optimization, was: allparams().nonEmpty();
        }

        /** A cache for the rank. */
        int rank_field = -1;

        /** A class type is raw if it misses some
         *  of its type parameter sections.
         *  After validation, this is equivalent to:
         *  allparams.isEmpty() && tsym.type.allparams.nonEmpty();
         */
        public boolean isRaw() {
            return
                this != tsym.type && // necessary, but not sufficient condition
                tsym.type.allparams().nonEmpty() &&
                allparams().isEmpty();
        }

        public Type map(Mapping f) {
            Type outer = getEnclosingType();
            Type outer1 = f.apply(outer);
            List<Type> typarams = getTypeArguments();
            List<Type> typarams1 = map(typarams, f);
            if (outer1 == outer && typarams1 == typarams) return this;
            else return new ClassType(outer1, typarams1, tsym);
        }

        public boolean contains(Type elem) {
            return
                elem == this
                || (isParameterized()
                    && (getEnclosingType().contains(elem) || contains(getTypeArguments(), elem)));
        }

        public void complete() {
            if (tsym.completer != null) tsym.complete();
        }

        public TypeKind getKind() {
            return TypeKind.DECLARED;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitDeclared(this, p);
        }
    }

    public static class DomainConstraint implements Cloneable {
        // constraints are stored as: coeffs * vars <OP> constant
        // OP can be either == or <=
        // variables are either: parameters, super parameters, indices, super indices
        public Map<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>> coeffs;
        public boolean eq; // true -> "==", false -> "<="
        public int constant;

        /** Makes a deep copy of this constraint.
         *  This creates is own copy of the coeff map. */
        public DomainConstraint deepCopy() {
            DomainConstraint newConstraint = new DomainConstraint();
            newConstraint.coeffs = new LinkedHashMap<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>>(coeffs);
            newConstraint.eq = eq;
            newConstraint.constant = constant;
            return newConstraint;
        }

		public boolean isMapping(DomainType dom)
		{
			if(dom.projectionArgs==null)
				return false;
			for(VarSymbol vs : coeffs.keySet())
				for(VarSymbol s:((DomainType)dom).projectionArgs)
					if(s==vs)
						return true;
			return false;
		}

		public String toISLString(Map<VarSymbol,VarSymbol> map,Map<VarSymbol,String> subst)
		{
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for(Map.Entry<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>> e : coeffs.entrySet()) {
				for(Pair<Integer,VarSymbol> c:e.getValue())
				{
					int i = c.fst;
					if(i < 0 && first)
						sb.append("- ");
					else if(i < 0)
						sb.append(" - ");
					else if(!first)
						sb.append(" + ");
					if(Math.abs(i)!=1)
					{
						sb.append(Math.abs(i));
						sb.append("*");
					}
					VarSymbol vs=map.get(e.getKey());

					if(vs==null)
						vs=e.getKey();

					String exp=null;
					if(subst!=null)
						exp=subst.get(vs);
					if(exp==null)
						sb.append(vs);
					else
						sb.append(exp);
					VarSymbol s = c.snd;
					if(s!=null)
					{
						vs=map.get(s);
						if(vs==null)
							vs=s;
						sb.append("*");

						exp=null;
						if(subst!=null)
							exp=subst.get(vs);
						if(exp==null)
							sb.append(vs);
						else
							sb.append(exp);
					}

					first = false;
				}
            }
            String op = eq ? " = " : " <= ";
            return sb.toString() + op + constant;
		}

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for(Map.Entry<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>> e : coeffs.entrySet()) {
				for(Pair<Integer,VarSymbol> c:e.getValue())
				{
					int i = c.fst;
					if(i < 0 && first)
						sb.append("- ");
					else if(i < 0)
						sb.append(" - ");
					else if(!first)
						sb.append(" + ");
					sb.append(Math.abs(i));
					sb.append("*");
					sb.append(e.getKey());
					Symbol s = c.snd;
					if(s!=null)
					{
						sb.append("*");
						sb.append(s.name);
					}
					first = false;
				}
            }
            String op = eq ? " == " : " <= ";
            return "[" + sb.toString() + op + constant + "]";
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }

    }

    public static class DomainType extends Type implements DeclaredType {

        /** The enclosing type of this type. If this is the type of an inner
         *  class, outer_field refers to the type of its enclosing
         *  instance class, in all other cases it referes to noType.
         */
        private Type outer_field;

        /** The type parameters of this type (to be set once class is loaded).
         */
        public List<Type> typarams_field;

        /** A cache variable for the type parameters of this type,
         *  appended to all parameters of its enclosing class.
         *  @see #allparams
         */
        public List<Type> allparams_field;

        /** The "interfaces" of this domain (to be set once class is loaded).
         *  Used for built-in selectors (e.g. reduce)
         */
        public List<Type> interfaces_field;

        /** The formal parameters of the domain.
         */
        public List<VarSymbol> formalParams;

        /** The actual parameters used in applied domains.
         *  Set to null if this type does not represent an applied domain.
         *  Applied parameters are always integers, therefore applied parent
         *  and result types are not represented this way.
         */
		public List<JCTree.JCExpression> appliedParams; // use DomParam here?
		public List<JCTree.JCExpression> appliedArgs; // use DomParam here?

		private boolean forceDynamic=false;

        /** The indices used for geting elements of the domain.
         */
        public List<VarSymbol> projectionArgs;

//		public int intraIterDims;

		public boolean usesVar(String name)
		{
			for(VarSymbol vs:formalParams)
				if(vs.name.toString().equals(name))
					return true;

			if(projectionArgs!=null)
				for(VarSymbol vs:projectionArgs)
					if(vs.name.toString().equals(name))
						return true;

			return false;
		}

		public Map<VarSymbol, JCExpression> getSymbolicProjectionArgMap()
		{
				Map<VarSymbol, JCExpression> map=new LinkedHashMap<VarSymbol, JCExpression>();

				if(appliedArgs!=null)
				{
					for(int i=0;i<appliedArgs.size();i++)
					{
						map.put(projectionArgs.get(i), appliedArgs.get(i));
					}
				}

				return map;
		}


		public void setupOneDVecs(String name)
		{
			interVectorOrder = new ArrayList<String>();
			interVectorOrder.add(name);
		}

		public ArrayList<String> getInterVectorOrder(JCDiagnostic.DiagnosticPosition pos)
		{
			return interVectorOrder;
		}

		private JCTree.JCMethodDecl getAccessTree(JCDiagnostic.DiagnosticPosition pos,int flag,boolean lexmin,boolean nullProj)
		{
			JavaCompiler jc=JavaCompiler.getCompiler();

			JCExpression[] params= new JCExpression[formalParams.size()];
			for(int i=0;i<formalParams.size();i++)
				params[i]=jc.make.Literal(-1);

			forceDynamic=true;
			JCTree.JCMethodDecl res;
			try
			{
				JCTree.JCCompilationUnit code=codegen(pos,params,(new ListBuffer<JCTree.JCVariableDecl>()).toList(),flag,false,lexmin,nullProj);
				JCTree.JCClassDecl classdecl = (JCTree.JCClassDecl) code.defs.get(1);
				JCTree.JCMethodDecl methd = (JCTree.JCMethodDecl) classdecl.defs.last();

				res=methd;
			}
			catch(Exception e)
			{
				res=null;
			}


			forceDynamic=false;
			return res;
		}



		private void calcinterVectors(JCDiagnostic.DiagnosticPosition pos)
		{
			findIter(getAccessTree(pos,ITER_IN_PROJECTION,false,false));
		}

		private Map<String,int[]> calcAccess(JCDiagnostic.DiagnosticPosition pos,int flag)
		{
			Map<String,int[]> res;
			try
			{
				res=findAccess(getAccessTree(pos,flag,false,false));
			}
			catch(Exception e)
			{
				res=new LinkedHashMap<String,int[]>();
			}

			return res;
		}


		<T extends JCTree> T replaceOffset(T cu, final String var, int offset) {
			class Replace extends TreeTranslator {
			   public <T extends JCTree> T translate(T tree) {
					if (tree == null) {
						return null;
					} else {
						JavaCompiler.getCompiler().make.at(tree.pos());
						T result = super.translate(tree);
						return result;
					}
				}
				public void visitIdent(JCTree.JCIdent tree) {
					if(tree.name.toString().equals(var))
						result=JavaCompiler.getCompiler().make.Parens(JavaCompiler.getCompiler().make.Binary(JCTree.PLUS, tree, JavaCompiler.getCompiler().make.Literal(1)));
					else
						result=tree;
				}
			}
			Replace v = new Replace();
			return v.translate(cu);
		}

		boolean containsProjectionList(JCTree.JCExpression cu, final List<VarSymbol> projectionArgs) {
		class Find extends TreeScanner {

			boolean foundVar=false;

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitIdent(JCTree.JCIdent tree) {
				for(VarSymbol vs:projectionArgs)
					foundVar|=vs.name.equals(tree.name);
			}

			}
			Find v = new Find();
			v.scan(cu);
			return v.foundVar;
		}


		List<JCTree.JCExpression> findAccessList(final JCTree.JCMethodDecl cu,final JCDiagnostic.DiagnosticPosition pos) {
		class Find extends TreeScanner {

			List<JCTree.JCExpression> list=null;

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}
			public void visitApply(JCTree.JCMethodInvocation tree) {
				if(((JCTree.JCIdent)(tree.meth)).name.toString().equals("S1"))
				{
					if(list!=null)
						JavaCompiler.getCompiler().log.error(pos,"internal","ambigous access in "+cu.body.toDeepString());
					list=tree.args;
				}
			}
			}
			Find v = new Find();
			v.scan(cu);
			return v.list;
		}

		Map<String,JCTree.JCExpression> findIterMinVal(JCTree.JCMethodDecl cu) {
			class Find extends TreeScanner {

			Map<String,JCTree.JCExpression> min=new LinkedHashMap<String, JCExpression>();

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}
			public void visitForLoop(JCTree.JCForLoop tree){
				Name n=((JCTree.JCIdent)(((JCTree.JCAssign) ((JCTree.JCExpressionStatement) tree.init.head).expr).lhs)).name;
				min.put(n.toString(), ((JCTree.JCAssign) ((JCTree.JCExpressionStatement) tree.init.head).expr).rhs);
				super.visitForLoop(tree);
			}
			}
			Find v = new Find();
			v.scan(cu);
			return v.min;
		}
		Map<String,JCTree.JCExpression> findIterMaxVal(JCTree.JCMethodDecl cu) {
			class Find extends TreeScanner {

			Map<String,JCTree.JCExpression> max=new LinkedHashMap<String, JCExpression>();

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}
			public void visitForLoop(JCTree.JCForLoop tree){
				Name n=((JCTree.JCIdent)(((JCTree.JCAssign) ((JCTree.JCExpressionStatement) tree.init.head).expr).lhs)).name;
				max.put(n.toString(), ((JCTree.JCBinary) tree.cond).rhs);
				super.visitForLoop(tree);
			}
			}
			Find v = new Find();
			v.scan(cu);
			return v.max;
		}

		int findAccessLoops(JCTree.JCMethodDecl cu) {
		class Find extends TreeScanner {

			int dim=0;

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}
			public void visitForLoop(JCTree.JCForLoop tree){
				dim++;
				super.visitForLoop(tree);
			}
			}
			Find v = new Find();
			v.scan(cu);
			return v.dim;
		}

		String getAccess(JCDiagnostic.DiagnosticPosition pos,int dim,int flag,String var,int offset)
		{
			List<JCTree.JCExpression> access=findAccessList(getAccessTree(pos,flag,true,false),pos);

			if(offset!=0)
				return replaceOffset(access.get(dim),var,offset).toString();
			else
				return access.get(dim).toString();
		}

		public List<JCTree.JCExpression> accessCast; //either direct access (non projection) or elt in proj
		public List<JCTree.JCExpression> accessCastMap; //either direct access (non projection) or elt in proj
		public List<JCTree.JCExpression> accessElement; //either direct access (non projection) or elt in proj
		public List<JCTree.JCExpression> accessProjection; //offset of projection with proj args ....
		private ArrayList<String> interVectorOrder=null;
		private Map<String,int[]> accessMap=null;
		private boolean sizeDependsOnProjection = false;
		//List<JCTree.JCExpression> accessCast; //access elt in casted dom!

		//public Map<String,int[]> dimVectors;
		//public int [] minProjectionVector;

		//private Map<String,int[]> interVectors=null;


        /** The parent of this domain (to be set once class is loaded).
         *  Use Type here instead of DomainType to allow error types.
         */
        public Type parentDomain;

        /** The parmeters of the parent.
         *  These have to be part of formalParams.
         */
        public List<VarSymbol> parentParams;

        /** The index tuple. (= indices of parent if a parent exists)
         */
        public List<VarSymbol> indices;

        /** The result domain type.
         */
        public Type resultDom;

        /** The parmeters of the result domain.
         */
        public List<JCExpression> resultDomParams;

        /** The constraints of this domain.
         */
        public List<DomainConstraint> constraints;

        //public List<DomainConstraint> parent_constraints;

        /** This is set to true if this domain has no parent.
         */
        public boolean isBaseDomain = true;

		public String getISLName()
		{
			return tsym.name.toString()+"__DOM";
		}

		public boolean isReinterpreteProjection()
		{
			return (!isBaseDomain&&(projectionArgs==null||projectionArgs.size()==0));
		}


		public List<JCTree.JCExpression> getParentParams()
		{
			ListBuffer<JCTree.JCExpression> res = new ListBuffer<JCTree.JCExpression>();
			if(parentParams!=null)
			{
				Map<VarSymbol, JCTree.JCExpression> varenv = new LinkedHashMap<VarSymbol, JCTree.JCExpression>();
				// add values of parameters
				for(int i = 0; i < formalParams.length(); i++) {
					varenv.put(formalParams.get(i), appliedParams.get(i));
				}

				for(VarSymbol vs:parentParams)
				{
					res.add(varenv.get(vs));
				}
			}

			return res.toList();
		}

		public List<JCTree.JCExpression> resultParams = null;

		public List<JCTree.JCExpression> getResultParams()
		{
			return resultParams;
		}

		void findIter(JCTree.JCMethodDecl cu) {
		class Find extends TreeScanner {

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitForLoop(JCTree.JCForLoop tree){

				Name n=((JCTree.JCIdent)(((JCTree.JCAssign) ((JCTree.JCExpressionStatement) tree.init.head).expr).lhs)).name;

				interVectorOrder.add(n.toString());

				super.visitForLoop(tree);
			}

			}
			Find v = new Find();
			v.scan(cu);
		}

		Map<String,int[]> findAccess(JCTree.JCMethodDecl cu) {
		class Find extends TreeScanner {

			boolean accessing=false;
//			Set<String> access=new LinkedHashSet<String>();
			Map<String,int[]> accessDim= new LinkedHashMap<String,int[]>();
			Map<String,Integer> accessStride= new LinkedHashMap<String,Integer>();
			int dim=0;

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitIdent(JCTree.JCIdent tree)
			{
				if(accessing)
				{
					int vec[]=accessDim.get(tree.name.toString());
					if(vec==null)
						vec=new int[getDim()];

					Integer s=accessStride.get(tree.name.toString());
					if(s!=null)
						vec[dim]=s;
					else
						vec[dim]=2;
					accessDim.put(tree.name.toString(),vec);
				}
				super.visitIdent(tree);
			}

			public void visitForLoop(JCTree.JCForLoop tree){

				Name n=((JCTree.JCIdent)(((JCTree.JCAssign) ((JCTree.JCExpressionStatement) tree.init.head).expr).lhs)).name;
				int stride;
				if(tree.step.head.expr.getTag()==JCTree.POSTINC)
					stride=1;
				else
					stride=2;
				accessStride.put(n.toString(),stride);
				super.visitForLoop(tree);
			}

			public void visitApply(JCTree.JCMethodInvocation tree) {
				if(((JCTree.JCIdent)(tree.meth)).name.toString().equals("S1"))
				{
					accessing=true;
					for(JCTree t:tree.args)
					{
						scan(t);
						dim++;
					}
					accessing=false;
				}

				super.visitApply(tree);

			}
			}
			Find v = new Find();
			v.scan(cu);
			return v.accessDim;
		}

		public boolean sizeIndepProjection()
		{
			return !sizeDependsOnProjection;
		}

		//somewhat ugly but we have to communicate with barvinok via Strings
		public void registerBarvinok(JCDiagnostic.DiagnosticPosition pos)
		{
			String isl=toISLString(true,false,null,null,null,false);
			String isl_nop=toISLString(parentDomain==null,false,null,null,null,false);
			String name=getISLName();

			//symbolically calc min and max and num of lattice points as function of params
			String res=ibarvinok.process(pos.getPreferredPosition(),name+":="+isl+";");

			//construct basic memory area
			//List<VarSymbol> inds=getIndexList(new LinkedHashMap<VarSymbol,VarSymbol>());
			//String mem=ibarvinok.process(pos.getPreferredPosition(),name+"_MEM:="+"{["+inds+"]->["+inds+"]}*"+name+";");

			interVectorOrder = new ArrayList<String>();
			accessMap=calcAccess(pos,ITER_IN_PROJECTION);

			if(!isBaseDomain)
			{
				//
				//if(projectionArgs!=null&&!projectionArgs.isEmpty())
				//	type_flags_field|=Flags.BLOCK;

				try
				{
					String simple=res.replaceAll("\\[", "").replaceAll("\\]", "");
					String [] pars=simple.split(":");
					pars=pars[0].split("-> \\{");

					String [] input=pars[0].split(",");
					//String [] output=pars[1].split(",");

					for(int i=0;i<input.length;i++)
						input[i]=input[i].replaceAll(" ", "");


					Set<String> out=new LinkedHashSet<String>(accessMap.keySet());

					for(VarSymbol pa:projectionArgs)
						out.remove(pa.name.toString());

					Set<String> in=new LinkedHashSet<String>();

					for(String s:input)
						in.add(s);

					in.removeAll(out);

					for(VarSymbol vs:formalParams)
						in.remove(vs.toString());

					String [] input_clean=new String[in.size()];
					int c=0;
					for(String s:input)
					{
						if(in.contains(s))
						{
							input_clean[c]=s;
							c++;
						}
					}

					//FIXME: use params command from barvinok to calc this rather than string fiddling

					res=res.replace(toISLParams(true,false,false,null,false), toISLParams(false,false,false,in,false));
					ibarvinok.process(name+"_PROJ:="+res+";");

					//codegen()
					if(!in.isEmpty())
						res=res.replace(toISLParams(false,false,false,in,false), toISLParams(false,false,false,null,false));
					else
						res=res.replace(toISLParams(false,false,false,in,false), toISLParams(false,false,true,null,false));

					if(!in.isEmpty())
					{
						String i=java.util.Arrays.toString(input_clean);
						String repl=i+" :";
						for(String os:out)
						{
							repl+=" exists "+os+": ";
						}

						res=res.replace("["+pars[1].trim()+"]"+" :",repl); //easier:[pars\proj] ->{ [proj pars]:exists indices : coinstrainst }
					}

					ibarvinok.process(name+"_IPROJ:="+res+";");

				}
				catch(Exception e)
				{
					JavaCompiler.getCompiler().log.error(pos,"internal","process projection");
				}

				calcinterVectors(pos);

			}
			else
			{
				//intraIterDims=getDim();
				List<VarSymbol> list=getIndexList(new LinkedHashMap<VarSymbol,VarSymbol>());
				for(VarSymbol s:list)
				{
					interVectorOrder.add(s.name.toString());
				}

			}

			ibarvinok.process(pos.getPreferredPosition(),name+"_NOP:="+isl_nop+";");
			if(!isBaseDomain)
			{
				ibarvinok.process(pos.getPreferredPosition(),name+"_min:=lexmin "+name+";");
				ibarvinok.process(pos.getPreferredPosition(),name+"_max:=lexmax "+name+";");
			}
			else
			{
				ibarvinok.process(pos.getPreferredPosition(),name+"_min:=lexmin "+name+"_NOP;");
				ibarvinok.process(pos.getPreferredPosition(),name+"_max:=lexmax "+name+"_NOP;");
			}
			ibarvinok.process(pos.getPreferredPosition(),name+"_card:=card "+name+";");

			if(resultDom!=null&&((DomainType)resultDom).projectionArgs!=null&&((DomainType)resultDom).projectionArgs.size()>0)
				ibarvinok.process(pos.getPreferredPosition(),name+"_CAST:="+toISLString(true,true,null,null,null,false)+";");

			if(this.resultDom!=null)
			{
				if(tsym.name.equals(resultDom.tsym.name))
					JavaCompiler.getCompiler().log.error(pos,"self.cast",this);
			}

			//new stuff:

			accessElement=findAccessList(getAccessTree(pos,ITER_IN_PROJECTION,false,true),pos); //either direct access (non projection) or elt in proj

			if(accessElement==null)
			{
				accessElement=findAccessList(getAccessTree(pos,ITER_IN_PROJECTION,false,false),pos);
				sizeDependsOnProjection=true;
			}

			if(resultDom!=null&&((DomainType)resultDom).projectionArgs!=null)
			{
				if(((DomainType)resultDom).projectionArgs.size()>0)
				{
					accessCast=findAccessList(getAccessTree(pos,SELECTION,false,true),pos); //either direct access (non projection) or elt in proj
					if(accessCast==null)
					{
						accessCast=findAccessList(getAccessTree(pos,SELECTION,false,false),pos);
						accessCastMap=findAccessList(getAccessTree(pos,SELECTION|NO_SUBST,false,false),pos);
					}
					else
						accessCastMap=findAccessList(getAccessTree(pos,SELECTION|NO_SUBST,false,true),pos);
				}
				//else
				//	JavaCompiler.getCompiler().log.error(pos,"internal",this);//FIXME
			}

			if(projectionArgs!=null&&projectionArgs.size()>0)
			{
				accessProjection=findAccessList(getAccessTree(pos,ITER_OVER_PROJECTIONS,true,false),pos); //offset of projection with proj args ....


				JCTree.JCMethodDecl ipp=getAccessTree(pos,ITER_IN_PROJECTION,false,false);

				//if(containsProjectionList(ipp,projectionArgs))
				try
				{
					List<JCTree.JCExpression> accessElementComplete=findAccessList(ipp,pos); //projection + iter access
					Map<String,JCTree.JCExpression> min=findIterMinVal(ipp);
					//Map<String,JCTree.JCExpression> max=findIterMaxVal(ipp);

					for(JCTree.JCExpression e:resultDomParams)
						if(containsProjectionList(e,projectionArgs))
						{
							sizeDependsOnProjection=true;
							break;
						}

					//verify that access in projection does not depend on projection!
					String innerOffset="[";

					innerOffset+=getIndexList(new LinkedHashMap<VarSymbol, VarSymbol>()); //only
					innerOffset+=",";
					innerOffset+=projectionArgs.toString();

					innerOffset+="]";

					innerOffset+=" -> { [";

					for(int i=0;i<accessElementComplete.size();i++)
					{
						if(i>0)
							innerOffset+=",";
						String itermin="";

						for(String it:accessMap.keySet())
						{
							if(accessMap.get(it)[i]!=0&&min.get(it)!=null)
							{
								String offs=min.get(it).toString();
								if(!offs.equals("0"))
									itermin+=" + " + offs;
							}
						}
						innerOffset+=accessElementComplete.get(i)+itermin+"-"+accessProjection.get(i);
					}

					innerOffset+="] }";

					String dependentOffset=ibarvinok.process(innerOffset+";");

					String args[]=dependentOffset.split("-> \\{");

					//must not depend on projection args:
					String innerIter="["+getIndexList(new LinkedHashMap<VarSymbol, VarSymbol>())+"] -> {" + args[1];
					dependentOffset=ibarvinok.processNoError(innerIter + ";");

					//FIXME: cannot iter over projections if this fails but direct access should be ok
					if(dependentOffset.equals("ERROR")&&false)
					{
						//iteration depends on projection!
						JavaCompiler.getCompiler().log.error(pos,"projection.iter.dependency",innerIter,this,projectionArgs);
					}
/*
					if(sizeDependsOnProjection)
					{
						accessProjection=findAccessList(getAccessTree(pos,ITER_OVER_PROJECTIONS,true,false)); //offset of projection with proj args ....
					}
*/
				}
				catch(Exception e)
				{
					JavaCompiler.getCompiler().log.error(pos,"internal","failed to verify that iter is independent of projection params");
				}
			}
		}

		public boolean domainAccessIsCast(DomainType dt)
		{
			if(dt.parentDomain==null)
				return true; //must be cast
			if(!dt.parentDomain.tsym.name.equals(tsym.name))
				return true;

			return false;
		}

		public boolean isIndexProjection(String index)
		{
			if(projectionArgs!=null)
			{
				List<VarSymbol> plist=projectionArgs;
				int size=plist.size();
				for(int d=0;d<size;d++)
				{
					if(plist.head.name.toString().equals(index))
						return true;
					plist=plist.tail;
				}
			}
			return false;
		}

		public long getDimStride(int dim,int [] base_size)//dim is either a projection param or an index, base_size is the size of the underlying object
		{
			long stride=1;

			for(int d=getDim()-1;d>dim;d--)
			{
				stride*=base_size[d];
			}

			return stride;
		}

		public void registerBarvinok()
		{
			if(isDynamic())
				return;
			//only needed for one_d!!
			String isl=toISLString(true,false,null,null,null,false);
			String isl_nop=toISLString(parentDomain==null,false,null,null,null,false);
			String name=getISLName();

			//symbolically calc min and max and num of lattice points as function of params
			String res=ibarvinok.process(name+":="+isl+";");

			ibarvinok.process(name+"_NOP:="+isl_nop+";");
			ibarvinok.process(name+"_min:=lexmin "+name+"_NOP;");
			ibarvinok.process(name+"_max:=lexmax "+name+"_NOP;");
			ibarvinok.process(name+"_card:=card "+name+";");

			//intraIterDims=1;

			List<VarSymbol> list=getIndexList(new LinkedHashMap<VarSymbol,VarSymbol>());
			//String mem=ibarvinok.process(name+"_MEM:="+"{["+list+"]->["+list+"]}*"+name+";");

			interVectorOrder = new ArrayList<String>();
			interVectorOrder.add(list.iterator().next().name.toString());

			ListBuffer<JCTree.JCExpression> lb=new ListBuffer<JCTree.JCExpression>();
			lb.add(JavaCompiler.getCompiler().make.Ident(list.iterator().next().name));
			accessElement=lb.toList(); //either direct access (non projection) or elt in proj

			accessMap=new LinkedHashMap<String, int[]>();
			int v[]={1};
			accessMap.put(list.iterator().next().name.toString(), v);

		}

		static final Pattern pdim=Pattern.compile(".*\\{ \\[((\\d+)(, \\d+)*)\\].*");
		static final Pattern ppdim=Pattern.compile(".*\\{ \\[(([^,;]+)(, [^,;]+)*)\\].*");
		static final Pattern argsdim=Pattern.compile("\\[(([^,;]+)(, [^,;]+)*)\\] ->");
		static final Pattern ppddim=Pattern.compile(".*; \\[(([^,]+)(, [^,]+)*)\\].*");
		static final Pattern pval=Pattern.compile(".*\\{ (\\d+) .*");
		static final Pattern sval1=Pattern.compile(".*\\{ (.*) }");
		static final Pattern sval2=Pattern.compile(".*\\{ (.*) :");
		static final Pattern ptrue=Pattern.compile(".*True");
		static final Pattern pempty=Pattern.compile(".*\\{  \\}");
		static final Pattern ppname=Pattern.compile("\\w+");

		public boolean isDynamic()
		{
			if(forceDynamic)
				return true;
			if(appliedParams==null)
				return false;

			for(JCTree.JCExpression p:appliedParams)
			{
				if(p.type.constValue()!=null&&p.type.constValue() instanceof Integer&&(Integer)p.type.constValue()!=-1)
					return false;
			}

			return true;
		}

		public boolean isFullyDynamic()
		{
			if(forceDynamic)
				return true;
			if(appliedParams==null)
				return false;

			for(JCTree.JCExpression p:appliedParams)
			{
				if(p.type.constValue()==null||(Integer)p.type.constValue()!=-1)
					return false;
			}

			return true;
		}

		String instantiateBarvinok(JCDiagnostic.DiagnosticPosition pos,JCExpression [] params, String value,int flags)
		{
			String result;
			if(formalParams!=null)
			{
				if(params.length<formalParams.size())
					JavaCompiler.getCompiler().log.error(pos,"internal","params supplied to domain insufficient");

				int c=0;
				StringBuffer map=new StringBuffer();
				for(VarSymbol p:formalParams)
				{
					if(c>=params.length)
						break;
					if(map.length()>0)
						map.append(" and ");
					map.append(p.name.toString()+"=");
					map.append(params[c]);
					c++;
				}

				if(projectionArgs!=null&&projectionArgs.length()>0)
				{
					for(VarSymbol p:projectionArgs)
					{
						if(c>=params.length)
							break;

						if(map.length()>0)
							map.append(" and ");
						map.append(p.name.toString()+"="+params[c]);
						c++;
					}
				}

				Set<String> extra=null;

				if((flags&SUBST_ONLY)==0)
					result=value+" % "+toISLParams((flags&ITER_IN_PROJECTION)==0&&(flags&ITER_OVER_PROJECTIONS)==0,(flags&SELECTION)!=0,false,extra,false)+"{:"+map.toString()+"}";
				else
					result="{:"+map.toString()+"}";
			}
			else
				result=value;

			return result;
		}

		int[] getTuple(JCDiagnostic.DiagnosticPosition pos,String tuple,int count)
		{
			Matcher m=pdim.matcher(tuple);
			int [] values=new int [count];
			for(int i=0;i<values.length;i++)
				values[i]=-1;

			if(m.find())
			{
				String [] svalues=m.group(1).split(", ");

				if(svalues.length!=count)
					return values;

				for(int i=0;i<count;i++)
				{
					try
					{
						values[i]=Integer.parseInt(svalues[i]);
					}
					catch(NumberFormatException nfe)
					{
						JavaCompiler.getCompiler().log.error(pos,"internal","failed to parse tuple value from barvinok '"+tuple+"'");
						values[i]=0;
					}

				}

				return values;
			}

			JavaCompiler.getCompiler().log.error(pos,"internal","failed to parse tuple value from barvinok '"+tuple+"'");
			return values;

		}

		String[] getGeneralStringTuple(JCDiagnostic.DiagnosticPosition pos,String tuple,int count)
		{
			String [] first= getStringTuple(pos,tuple,count);
			String [] second= getStringTupleDisjoined(pos,tuple,count);

			if(second==null)
				return first;

			int val_count_first=0;

			for(String s:first)
			{
				try
				{
					Integer.parseInt(s);
					val_count_first++;
				}
				catch(NumberFormatException nfe)
				{

				}
			}

			int val_count_second=0;

			for(String s:second)
			{
				try
				{
					Integer.parseInt(s);
					val_count_second++;
				}
				catch(NumberFormatException nfe)
				{

				}
			}

			if(val_count_second<val_count_first)
				return second;
			return first;

		}

		String[] getStringTuple(JCDiagnostic.DiagnosticPosition pos,String tuple,int count)
		{
			Matcher m=ppdim.matcher(tuple);
			String [] values=new String [count];

			if(m.find())
			{
				values=m.group(1).split(", ");

				if(values.length!=count)
					return null;

				return values;
			}

			JavaCompiler.getCompiler().log.error(pos,"internal","failed to parse tuple value from barvinok '"+tuple+"'");
			return values;

		}

		String[] getStringTupleAnyDim(JCDiagnostic.DiagnosticPosition pos,String tuple)
		{
			Matcher m=ppdim.matcher(tuple);

			String [] values=null;
			if(m.find())
			{
				values=m.group(1).split(", ");
				return values;
			}

			JavaCompiler.getCompiler().log.error(pos,"internal","failed to parse tuple value from barvinok '"+tuple+"'");
			return values;
		}


		String[] getStringArgsAnyDim(JCDiagnostic.DiagnosticPosition pos,String tuple)
		{
			Matcher m=argsdim.matcher(tuple);

			String [] values=null;
			if(m.find())
			{
				values=m.group(1).split(", ");
				return values;
			}

			JavaCompiler.getCompiler().log.error(pos,"internal","failed to parse tuple value from barvinok '"+tuple+"'");
			return values;
		}

		String[] getStringTupleDisjoined(JCDiagnostic.DiagnosticPosition pos,String tuple,int count)
		{
			Matcher m=ppddim.matcher(tuple);
			String [] values=new String [count];

			if(m.find())
			{
				values=m.group(1).split(", ");

				if(values.length!=count)
					return null;

				return values;
			}

			return null;

		}

		public String getStringVal(JCDiagnostic.DiagnosticPosition pos,String val,boolean fail)
		{
			int i=val.indexOf(":");

			if(i>=0)
			{
				int s=val.indexOf("{");
				return val.substring(s+1, i);
			}

			Matcher m=sval1.matcher(val);

			if(m.find())
			{
				return m.group(1);
			}

			if(fail)
				JavaCompiler.getCompiler().log.error(pos,"internal","failed to parse value from barvinok '"+val+"'");
			return "";
		}

		int getInt(JCDiagnostic.DiagnosticPosition pos,String val,boolean fail)
		{
			//FIXME: card may depend on vars

			Matcher m=pval.matcher(val);

			if(m.find())
			{
				try
				{
					return Integer.parseInt(m.group(1));
				}
				catch(NumberFormatException nfe)
				{
					if(fail)
						JavaCompiler.getCompiler().log.error(pos,"internal","failed to parse value from barvinok '"+val+"'");
				}
			}

			if(fail)
				JavaCompiler.getCompiler().log.error(pos,"internal","failed to parse value from barvinok '"+val+"'");
			return 0;
		}

		boolean isEmpty(JCDiagnostic.DiagnosticPosition pos,String val)
		{
			return pempty.matcher(val).find();
		}

		boolean getBoolean(JCDiagnostic.DiagnosticPosition pos,String val)
		{
			return ptrue.matcher(val).find();
		}

		//FIXME: pure arrays equals don't check for content but ref identitiy
		//using Arrays.asList is not safe as diferent arrays may hash to the same value
		static Map<String,int[]> cache_min=new LinkedHashMap<String,int[]>();
		static Map<String,int[]> cache_size=new LinkedHashMap<String,int[]>();
		static Map<String,int[]> cache_max=new LinkedHashMap<String,int[]>();
		static Map<String,Integer> cache_card=new LinkedHashMap<String,Integer>();

		public int[] getMin(JCDiagnostic.DiagnosticPosition pos,JCExpression [] params)
		{
			if(isDynamic())//must use version->SIZE
			{
				throw new AssertionError("invalid on dynamic domain");
			}

			//Integer[] param=appliedParams.toArray(new Integer[0]);

			DomainType dt=this;
			if(parentDomain!=null)
			{
				dt=(DomainType)parentDomain;
				params=translateToParentParams(params);
			}

			String proc=dt.instantiateBarvinok(pos,params,dt.getISLName()+"_min",ITER_IN_PROJECTION);
			int[] min=cache_min.get(proc);
			if(min!=null)
				return min;

			String result=ibarvinok.process(pos.getPreferredPosition(),proc+";");

			min= getTuple(pos,result,getDim());
			cache_min.put(proc, min);
			return min;
		}

		public String getMaxDynamic()
		{
			if(!isDynamic())//must use version->SIZE
			{
				throw new AssertionError("invalid on non-dynamic domain");
			}

			return "->GetCount()";
		}

		JCExpression [] translateToParentParams(JCExpression [] params)
		{
			Map<VarSymbol,JCExpression> map=new LinkedHashMap<VarSymbol, JCExpression>();

			int c=0;
			for(VarSymbol vs:formalParams)
			{
				map.put(vs, params[c]);
				c++;
			}

			JCExpression [] result=new JCExpression [parentParams.length()];
			c=0;
			for(VarSymbol vs:parentParams)
			{
				result[c]=map.get(vs);
				c++;
			}

			return result;
		}
/*
		public int[] getIterSize(JCDiagnostic.DiagnosticPosition pos,JCExpression [] params)
		{
			if(isDynamic())//must use version->SIZE
			{
				throw new AssertionError("invalid on dynamic domain");
			}

			if(projectionArgs==null||projectionArgs.isEmpty())
			{
				int [] oo=new int[0];
				return oo;
			}

			int [] parentSize = ((DomainType)parentDomain).getSize( pos,translateToParentParams(params));

			int [] result=new int[projectionArgs.size()];

			for(int i=0;i<projectionArgs.size();i++)
			{
				int [] vec=dimVectors.get(projectionArgs.get(i).toString());
				int max=Integer.MAX_VALUE;
				for(int s=0;s<parentSize.length;s++)
				{
					if(vec[s]!=0)
					   max=Math.min(max,(parentSize[s])/vec[s]);
				}
				result[i]=max;
			}


			return result;
		}
*/
		public int[] getSize(JCDiagnostic.DiagnosticPosition pos,String lmin,String lmax)
		{
			return getSize(pos,lmin,lmax,getDim());
		}

		public int[] getSize(JCDiagnostic.DiagnosticPosition pos,String lmin,String lmax,int dim)
		{
			String[] smax=getGeneralStringTuple(pos,lmax,dim);
			String[] smin=getGeneralStringTuple(pos,lmin,dim);

			String diff="{[";

			for(int i=0;i<smax.length;i++)
			{
				if(diff.length()>2)
					diff+=", ";
				diff+=smax[i]+"-"+smin[i];
			}
			diff+="]}";

			String args;

			args=allISLParams(null);

			String size=ibarvinok.process(pos.getPreferredPosition(),args+diff+";");

			return getTuple(pos,size,dim);
		}

		public int[] getSize(JCDiagnostic.DiagnosticPosition pos,JCExpression [] params)
		{
			if(isDynamic())//must use version->SIZE
			{
				throw new AssertionError("invalid on dynamic domain");
			}

			String proc=instantiateBarvinok(pos,params,getISLName()+"_max",ITER_IN_PROJECTION);
			int[] size=cache_size.get(proc);
			if(size!=null)
				return size;

			String lmax=ibarvinok.process(pos.getPreferredPosition(),proc+";");
			String lmin=ibarvinok.process(pos.getPreferredPosition(),instantiateBarvinok(pos,params,getISLName()+"_min",ITER_IN_PROJECTION)+";");

			int[] res= getSize(pos,lmin,lmax);

			for(int i=0;i<res.length;i++)
				res[i]++;// add one!

			cache_size.put(proc,res);
			return res;
		}

		public int[] getMax(JCDiagnostic.DiagnosticPosition pos,JCExpression [] params)
		{
			if(isDynamic())//must use version->SIZE
			{
				throw new AssertionError("invalid on dynamic domain");
			}


			DomainType dt=this;
			if(parentDomain!=null)
			{
				dt=(DomainType)parentDomain;
				params=translateToParentParams(params);
			}
			String proc=dt.instantiateBarvinok(pos,params,dt.getISLName()+"_max",ITER_IN_PROJECTION);

			int[] max=cache_max.get(proc);
			if(max!=null)
				return max;

			String result=ibarvinok.process(pos.getPreferredPosition(),proc+";");

			max=getTuple(pos,result,getDim());
			cache_max.put(proc, max);
			return max;
		}

		public String SubstWithoutConstraints(String input,boolean subst)
		{
			String set[]=input.split("->");

			if(set.length<2)
				return input;

			String data[]=set[1].split(":");

			if(subst)
			{
				List<JCTree.JCExpression> rdp=resultDomParams;
				for(VarSymbol vs:((DomainType)resultDom).formalParams)
				{
					data[0]=data[0].replaceAll(vs.toString(),rdp.head.toString());
					rdp=rdp.tail;
				}
			}

			String out=data[0];
			if(data.length>1)
				out+="}";

			return out;
		}

		int sameSize=-1;

		public boolean resultHasSameSize(JCDiagnostic.DiagnosticPosition pos,JCExpression[] params)
		{
			//NOTE: this ONLY works if we do NOT postpone domain instantiation until the formal params are given (for dynamic doms anyways)

			if(sameSize<0&&resultDom!=null) //cache
			{
				String resSize=ibarvinok.process(pos.getPreferredPosition(),((DomainType)resultDom).getISLName()+"_card;");

				String out=SubstWithoutConstraints(resSize,true);

				String name;
				if(parentDomain!=null)
					name=((DomainType)parentDomain).getISLName();
				else
					name=getISLName();

				String Size=ibarvinok.process(pos.getPreferredPosition(),name+"_card;");

				String in=SubstWithoutConstraints(Size,false);
				String result="("+toISLParams(true,false,false,null,false)+in+")==("+toISLParams(true,false,false,null,false)+out+");";

				result=ibarvinok.process(pos.getPreferredPosition(),result);
				if(getBoolean(pos,result))
					sameSize=1;
				else
					sameSize=0;
			}

			return sameSize==1;
		}

		public String getCardDynamic()
		{
			return getMaxDynamic();
		}

		public String getCardString(JCDiagnostic.DiagnosticPosition pos,JCExpression [] params,boolean fail)
		{
			String proc=getISLName()+"_card";

			int prefPos=0;
			if(pos!=null)
				prefPos=pos.getPreferredPosition();
			if(params!=null)//must use version->SIZE
			{
				String res= ibarvinok.process(prefPos,proc+";");

				if(!forceDynamic)
				for(int i=0;i<formalParams.size();i++)//proper parsing??
					if(!params[i].toString().equals("-1"))
						res=res.replaceAll(formalParams.get(i).name.toString(), params[i].toString());

				return res;
			}
			else
				return proc;

			//return ibarvinok.process(prefPos,instantiateBarvinok(pos,params,proc,ITER_OVER_PROJECTIONS)+";");

		}

		public int getCard(JCDiagnostic.DiagnosticPosition pos,JCExpression [] params,boolean fail)
		{
			if(isDynamic())//must use version->SIZE
			{
				throw new AssertionError("invalid on dynamic domain");
			}

			String proc=getISLName()+"_card";
			Integer card=cache_card.get(proc+Arrays.toString(params));
			if(card!=null)
				return card;

			int prefPos=0;
			if(pos!=null)
				prefPos=pos.getPreferredPosition();

			String result=ibarvinok.process(prefPos,instantiateBarvinok(pos,params,proc,ITER_IN_PROJECTION)+";");

			card=getInt(pos,result,fail);
			cache_card.put(proc+Arrays.toString(params), card);
			return card;
		}

		public static final int ITER_IN_PROJECTION=1<<0;
		public static final int SELECTION=1<<1;
		public static final int ITER_OVER_PROJECTIONS=1<<2;
		public static final int SUBST_ONLY=1<<3;
		public static final int NO_SUBST=1<<4;
		public static final int KEEP_CONST=1<<4;

		String instantiate(JCDiagnostic.DiagnosticPosition pos,JCExpression [] params,int flags)//create instance of this domain
		{
			String value;
			int modflags=flags;
			if((flags&ITER_IN_PROJECTION)!=0&&!isBaseDomain&&parentDomain!=null)
				value=getISLName();
			else if((flags&SELECTION)!=0&&parentDomain!=null)
				value=getISLName()+"_CAST";
			else if((flags&ITER_OVER_PROJECTIONS)!=0&&!isBaseDomain&&parentDomain!=null)
				value=getISLName()+"_IPROJ";
			else
			{
				if(!isBaseDomain&&parentDomain!=null)
					value=getISLName()+"_PROJ";
				else
				{
					value=getISLName();
					modflags=flags&(~SELECTION&~ITER_OVER_PROJECTIONS);
				}
			}

			if(isDynamic())
				if((flags&KEEP_CONST)==0||isFullyDynamic())
					return value;

			return instantiateBarvinok(pos,params,value,modflags);
		}

		//ok if we have the same number of points
		//use codegen to get bijective mapping from one domain to the other
		public boolean canCastTo(JCDiagnostic.DiagnosticPosition pos,JCExpression [] params,DomainType dt,JCExpression [] dtparams)
		{
			if(!(isBaseDomain&&dt.isBaseDomain))
				if(getInterVectorOrder(pos).size()!=dt.getInterVectorOrder(pos).size())
					return false;

			if(isFullyDynamic()||dt.isFullyDynamic())
				return true;

			//symbolically verify that sizes are equal
			String c1=getCardString(pos,params,true);
			String c2=dt.getCardString(pos,dtparams,true);

			c1=getStringVal(pos,c1,true);
			c2=getStringVal(pos,c2,true);

			Set<String> addparams=new LinkedHashSet<String>();

			for(JCExpression e:params)
				for(VarSymbol s:JCTree.usedVars(e))
					addparams.add(s.name.toString());

			for(JCExpression e:dtparams)
				for(VarSymbol s:JCTree.usedVars(e))
					addparams.add(s.name.toString());

			for(VarSymbol vs:formalParams)
				addparams.remove(vs.name.toString());

			if(projectionArgs!=null)
				for(VarSymbol vs:projectionArgs)
					addparams.remove(vs.name.toString());

			for(VarSymbol vs:dt.formalParams)
				addparams.remove(vs.name.toString());

			if(dt.projectionArgs!=null)
				for(VarSymbol vs:dt.projectionArgs)
					addparams.remove(vs.name.toString());

			String diff=ibarvinok.process(allISLParams(addparams)+" { [ "+c1+"-("+c2+") ]};");

			String[] res= getStringTuple(pos,diff,1);

			if(res[0]==null)
				return false;

			boolean equal = res[0].equals("0");

			return equal;
			//if inner dimensions are identical then shapes are "equal"

		}

		/*
		 * matrix[x,y]==matrix.row[y].one_d[x];
		 * matrix gives base address, projections give offsets -> no copying
		 * one_d tmp=matrix.row[y].one_d[x] //here we actually copy
		 */

	JCTree replace(JCTree cu, final Map<String, VarSymbol> map, final JavaCompiler jc) {
			class Replace extends TreeTranslator {

//				int discardLoops=-1;

				public void scan(JCTree tree) {
					if (tree != null) {
						tree.accept(this);
					}
				}

				public void visitIf(JCTree.JCIf tree)
				{
					//FIXME: can we discard ifs? if not then we must scale the condition!

					scan(((JCTree.JCBlock)tree.thenpart).stats.head);
					result=((JCTree.JCBlock)tree.thenpart).stats.head;
				}

				public void visitVarDef(JCTree.JCVariableDecl tree) {
					VarSymbol sym = map.get(tree.name.toString());
					if (sym != null) {
						tree.name = sym.name;
						tree.sym = sym;
					}
					tree.sym.flags_field|=TASKLOCAL;
					result = tree;
				}

				public void visitIdent(JCTree.JCIdent tree) {
					VarSymbol sym = map.get(tree.name.toString());
					if (sym != null) {
						tree.name = sym.name;
						tree.sym = sym;
					}
					super.visitIdent(tree);
				}
			}
			Replace v = new Replace();
			cu=(JCTree)(new TreeCopier(jc.make)).copy(cu);
			return v.translate(cu);
		}


		static Map<String,JCTree.JCCompilationUnit> cache=new LinkedHashMap<String,JCTree.JCCompilationUnit>();

		public JCTree.JCCompilationUnit codegen(JCDiagnostic.DiagnosticPosition pos, JCExpression[] params, List<JCTree.JCVariableDecl> da,int flags,boolean attrib,boolean lexmin,boolean nullProj) {

			JavaCompiler jc=JavaCompiler.getCompiler();

			//construct fake class so we can reuse our parser
			String code = "import static ffi.math.*;\nclass CodeGen" + getISLName() +"_" + flags+"_"+(!isBaseDomain&&parentDomain!=null)+"_"+"_"+lexmin+"_"+attrib;

			for(JCExpression i:params)
			{
				if((Integer)i.type.constValue()!=null)
					code+="_"+Math.max(0, (Integer)i.type.constValue());
				else
					code+="_0";
			}

			code += "{";
			code += "static void S1(int ... dummy){}";
			code += "static void code(";
			if(isDynamic())
			{
				boolean first=true;
				int i=0;
				for(JCExpression e:params)
				{
					if(e.type.constValue()!=null&&((Integer)e.type.constValue())!=-1)
					{
						if(!first)
							code+=", ";
						code+="int "+e.toString();
						first=false;
					}
					else
					{
						if(!first)
							code+=", ";
						code+="int "+formalParams.get(i);
						first=false;
					}
					i++;
				}
			}
			code+=") {";

			List<VarSymbol> ind = getIndexList(new LinkedHashMap<VarSymbol, VarSymbol>());

			String indices = "["+ind+"]";

			if (indices.length() > 2) {
				code += "int " + indices.substring(1, indices.length() - 1) + ";";//declare vars
			}

			if(!isBaseDomain&&projectionArgs!=null&&projectionArgs.size()>0)
			{
				code += "int ";
				boolean first=true;
				for(VarSymbol vd:projectionArgs)
				{
					if(!first)
						code+=", ";
					code+=vd.name;
					first=false;
				}
				code+=";";
			}

			if((flags&SELECTION)!=0&&resultDom!=null&&((DomainType)resultDom).projectionArgs!=null)
			{
				code += "int ";
				boolean first=true;
				for(VarSymbol vd:((DomainType)resultDom).projectionArgs)
				{
					if(!first)
						code+=", ";
					code+=vd.name;
					first=false;
				}
				code+=";";

			}

			JCTree.JCCompilationUnit res=cache.get(code);
			if(res!=null)
			{
				Map<String, VarSymbol> map=new LinkedHashMap<String, VarSymbol>();
				if(isBaseDomain||(flags&ITER_IN_PROJECTION)!=0)
				{
					if(!da.isEmpty())
					{
						Iterator<JCTree.JCVariableDecl> dvar=da.iterator();
						Iterator<String> ivar=getInterVectorOrder(pos).iterator();

						while(ivar.hasNext()&&dvar.hasNext())
						{
							map.put(ivar.next().toString(), dvar.next().sym);
						}

						//replace domain's index by user's choice
						if(map.size()>0)
						{
							res= (JCTree.JCCompilationUnit)replace(res,map,jc);
						}
					}
				}
				else
				{
					//if(projectionArgs.length()!=da.length())
					//	jc.log.error(pos, "internal", "domain iter args do not match domain dimension");

					Iterator<VarSymbol> iind=projectionArgs.iterator();
					Iterator<JCTree.JCVariableDecl> ivar=da.iterator();

					while(iind.hasNext()&&ivar.hasNext())
					{
						map.put(iind.next().toString(), ivar.next().sym);
					}

					//replace domain's index by user's choice
					if(map.size()>0)
					{
						res= (JCTree.JCCompilationUnit)replace(res,map,jc);
					}
				}
				return res;
			}

			String put=code;

			String barvinok_return;
			/*
			if(isDynamic())//must use version->SIZE
			{
				barvinok_return="int __DYN_MAX;for("+ind.head+"=0;"+ind.head+"<__DYN_MAX;"+ind.head+"++){S1("+ind.head+");}";
			}
			else
			*/
			if(isDynamic())
			{
				if(params.length>0)
				{
					int i=0;
					for(VarSymbol vs:formalParams)
					{
						if(params[i].type.constValue()!=null&&((Integer)params[i].type.constValue())!=-1)
							code += "int "+vs.toString()+"="+params[i].toString()+";\n";

						i++;
					}
				}
			}

			String np="";

			if(nullProj&&projectionArgs!=null&&projectionArgs.size()>0&&(flags&NO_SUBST)==0)
			{
				boolean lfist=true;
				np+=" % ["+projectionArgs+"] -> {:";
				for(VarSymbol vs:projectionArgs)
				{
					if(!lfist)
						np+=" and ";
					np+=vs+" = 0 ";
				}

				np+="}";
			}

			if(!lexmin)
				barvinok_return=ibarvinok.process(pos.getPreferredPosition(), "codegen (" + instantiate(pos, params,flags) + np + ");");
			else
				barvinok_return=ibarvinok.process(pos.getPreferredPosition(), "codegen (lexmin("+getISLName() + np +") );");

			code += barvinok_return;
			code += "}}";

			if(barvinok_return.equals("ERROR"))
				return null;


			CharSequence cs = code;
			URI uri = null;

			try {
				uri = new URI(getISLName());
			} catch (URISyntaxException use) {
				jc.log.error(pos, "internal", "failed to create URI : "+use.toString());
			}

			int prevErrs=jc.errorCount();
			JCTree.JCCompilationUnit cu = jc.parse(new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE), cs);

			if(prevErrs!=jc.errorCount())
				jc.log.error(pos, "internal", "failed parse cloog generated code");


			JCTree.JCClassDecl classdecl=(JCTree.JCClassDecl)cu.defs.get(1);

			ListBuffer<JCTree.JCCompilationUnit> list=new ListBuffer<JCTree.JCCompilationUnit>();
			list.add(cu);

			try
			{
				if(attrib)
				{
					jc.enter.main(list.toList());

	//				((JCTree.JCClassDecl)cu.defs.head).sym.finish();

					jc.attr.attribCodeGen(pos, classdecl.sym);

					cache.put(put, cu);
				}
			}
			catch(Exception e)
			{
				jc.log.error(pos, "internal", "failed to typecheck cloog code");
			}

			Map<String, VarSymbol> map=new LinkedHashMap<String, VarSymbol>();

			if(isDynamic())
			{
				int i=0;
				for(VarSymbol vs:formalParams)
				{
					if(params[i].type.constValue()!=null&&(Integer)params[i].type.constValue()!=-1)
						map.put(vs.toString(),(VarSymbol)TreeInfo.symbol(params[i]));
					i++;
				}
			}

			if(isBaseDomain||(flags&ITER_IN_PROJECTION)!=0)
			{
				//if(ind.length()!=da.length())
				//	jc.log.error(pos, "internal", "domain iter args do not match domain dimension");
				if(!da.isEmpty())
				{
					Iterator<JCTree.JCVariableDecl> dvar=da.iterator();
					Iterator<String> ivar=getInterVectorOrder(pos).iterator();

					while(ivar.hasNext()&&dvar.hasNext())
					{
						map.put(ivar.next().toString(), dvar.next().sym);
					}

					//replace domain's index by user's choice
					if(map.size()>0)
					{
						res= (JCTree.JCCompilationUnit)replace(cu,map,jc);
					}
					else
						res=cu;
				}
				else
					res=cu;
			}
            else if(projectionArgs!=null)
			{
				//if(projectionArgs.length()!=da.length())
				//	jc.log.error(pos, "internal", "domain iter args do not match domain dimension");

				Iterator<VarSymbol> iind=projectionArgs.iterator();
				Iterator<JCTree.JCVariableDecl> ivar=da.iterator();

				while(iind.hasNext()&&ivar.hasNext())
				{
					map.put(iind.next().toString(), ivar.next().sym);
				}

				//replace domain's index by user's choice
				if(map.size()>0)
				{
					res= (JCTree.JCCompilationUnit)replace(cu,map,jc);
				}
				else
					res=cu;
			}

			if(isDynamic())
			{
				JCTree.JCClassDecl rt_classdecl=(JCTree.JCClassDecl)cu.defs.get(1);
				JCTree.JCMethodDecl rt_methd=(JCTree.JCMethodDecl)rt_classdecl.defs.last();
				for(JCTree.JCVariableDecl vd:rt_methd.params)
				{
					if(vd.sym!=null)
					{
						vd.sym.flags_field&=~PARAMETER; //these are not actual parameters but locally defined vars
						vd.sym.flags_field|=TASKLOCAL;
					}
				}
			}

			return res;
		}

		public enum DomainSpec
		{
			OverSpecified,
			UnderSpecified,
			Equal,
			Unknown
		}


		public List<JCTree.JCExpression> getAddressing(JCDiagnostic.DiagnosticPosition pos,Map<Symbol.VarSymbol, Symbol.VarSymbol> index_map, Type.ArrayType at, List<VarSymbol> inds, boolean iter_inside_projection, boolean no_reinterprete)
		{
			Type.ArrayType pt = at;

			JavaCompiler jc=JavaCompiler.getCompiler();

			boolean reinterprete = (!at.treatAsBaseDomain()) && !no_reinterprete;//are we just looking at an x through 'glasses' like two_d.one_d

			List<JCTree.JCExpression> addressing = null;

			Map<String, Symbol.VarSymbol> map = new LinkedHashMap<String, Symbol.VarSymbol>(); //repace strings from codegen by varsym so state.index_map can be filled with varsyms
			//index_map = new LinkedHashMap<Symbol.VarSymbol, JCTree.JCExpression>(); //values from codegen need to replaced by the used supplied index in vsistVarSymbol (sequential emitter)

			if (reinterprete) {
				if (at.isCast()) {//e.g. trace.one_d
					pt = at.getRealType();
				}

				if (!at.isCast()) {
					for (int i = 0; i < pt.dom.getInterVectorOrder(pos).size(); i++) {//inter iteration order
						Symbol.VarSymbol vs = new Symbol.VarSymbol(0, pt.dom.tsym.name.table.names.fromString(pt.dom.getInterVectorOrder(pos).get(i)), jc.syms.intType, null);
						vs.flags_field |= Flags.TASKLOCAL;
						map.put(vs.name.toString(), vs);

						index_map.put(inds.get(i),vs);
					}
				} else {
					for (int c = 0; c < ((Type.DomainType) pt.dom.resultDom).projectionArgs.size(); c++) {//cast uses result dom pars
						index_map.put(inds.get(c),((Type.DomainType) pt.dom.resultDom).projectionArgs.get(c));
						map.put(((Type.DomainType) pt.dom.resultDom).projectionArgs.get(c).name.toString(), ((Type.DomainType) pt.dom.resultDom).projectionArgs.get(c));
					}
				}

				if (at.isCast()) {

					//FIXME: do not use generic projection access if the value is fixed!
					addressing=pt.dom.accessCast;

					if(pt.dom.accessProjection!=null&&false)
					{
						ListBuffer<JCTree.JCExpression> lb=new ListBuffer<JCTree.JCExpression>();
						for(int i=0;i<pt.dom.accessCast.size();i++)
							lb.add(jc.make.Binary(JCTree.PLUS, pt.dom.accessProjection.get(i), pt.dom.accessCast.get(i)));
						addressing = lb.toList();
					}
				} else {
					//FIXME: do not use generic projection access if the value is fixed!
					addressing=pt.dom.accessElement;
					if(pt.dom.accessProjection!=null&&false)
					{
						ListBuffer<JCTree.JCExpression> lb=new ListBuffer<JCTree.JCExpression>();
						for(int i=0;i<pt.dom.accessElement.size();i++)
							lb.add(jc.make.Binary(JCTree.PLUS, pt.dom.accessProjection.get(i), pt.dom.accessElement.get(i)));
						addressing = lb.toList();
					}
				}

			} else { //dead code?
				if (iter_inside_projection) {
					for (int i = 0; i < pt.dom.getInterVectorOrder(pos).size(); i++) {
						Symbol.VarSymbol vs = new Symbol.VarSymbol(0, pt.dom.tsym.name.table.names.fromString(pt.dom.getInterVectorOrder(pos).get(i)), jc.syms.intType, null);
						vs.flags_field |= Flags.TASKLOCAL;
						map.put(vs.name.toString(), vs);

						index_map.put(inds.get(i),vs); //this mapping is used to replace the __RIs by the actual index value in visitVarSymbol in LowerSequential
					}
					addressing = pt.dom.accessElement;

				} else {
					for (int i = 0; i < pt.dom.projectionArgs.size(); i++) {

						if (pt.dom.projectionArgs.get(i) != inds.get(i)) {
							index_map.put(inds.get(i),pt.dom.projectionArgs.get(i)); //this mapping is used to replace the __RIs by the actual index value in visitVarSymbol in LowerSequential
						}

						map.put(pt.dom.projectionArgs.get(i).name.toString(), pt.dom.projectionArgs.get(i));
					}
					addressing = pt.dom.accessProjection;
				}
			}

			if(pt.dom.projectionArgs!=null)
			{
				for (int i = 0; i < pt.dom.projectionArgs.size(); i++) {
					map.put(pt.dom.projectionArgs.get(i).name.toString(), pt.dom.projectionArgs.get(i));
				}
			}

			if (!map.isEmpty()&&addressing!=null) { //state.index_map works only with the proper syms, so substitute them
				ListBuffer<JCTree.JCExpression> lb = new ListBuffer<JCTree.JCExpression>();
				for (JCTree.JCExpression e : addressing) {
					lb.add((JCTree.JCExpression)replace(e, map, jc));
				}
				addressing = lb.toList();
			}
			return addressing;
		}

		String getISLSubst(JCTree.JCMethodDecl md,Set<JCExpression> extraConstraints,Set<String> extra,Map<VarSymbol, JCExpression> lastProjectionArgs,Map<String, VarSymbol> basemap)
		{
			Map<VarSymbol,String> domsubst=new LinkedHashMap<VarSymbol, String>();

			Set<VarSymbol> extraVars=new LinkedHashSet<VarSymbol>();

			if(projectionArgs!=null)
			{
				for(VarSymbol vs:projectionArgs)
				{
					JCExpression val=lastProjectionArgs.get(vs);
					if(val!=null)
					{
						String exp="";
						exp+="(";
						exp+=val;
						exp+=")";

						domsubst.put(vs, exp);
						extraVars.addAll(JCTree.usedVars(val));
					}
					//FIXME: should uniquely rename unsubst vals so they are not identical to other domain's params by chance
				}
			}

            if(appliedParams!=null)
			for(int i=0;i<formalParams.size();i++)
			{
				JCExpression param=appliedParams.get(i);


				boolean constraint=JCTree.usedOp(param,JCTree.GT)||JCTree.usedOp(param,JCTree.GE)
								||JCTree.usedOp(param,JCTree.LT)||JCTree.usedOp(param,JCTree.LE);
				/*
				if(constraint&&((JCBinary)param).lhs.type.constValue() instanceof Integer)
				{
					param = ((JCBinary)param).lhs;
					constraint=false;
				}
				*/

				if(!constraint)
				{
					boolean round=JCTree.usedOp(param,JCTree.DIV);
					String exp="";
					if(round)
						exp+="[";
					else
						exp+="(";
					exp+=param;
					if(round)
						exp+="]";
					else
						exp+=")";

					domsubst.put(formalParams.get(i), exp);
				}

				extraVars.addAll(JCTree.usedVars(param));
			}

			Set<VarSymbol> extraSyms=new LinkedHashSet<VarSymbol>();

			if(extraConstraints!=null)
			{
				JavaCompiler jc=JavaCompiler.getCompiler();

				for(JCExpression e:md.getConstraintsTransitive(extraVars, extraSyms))
				{
					extraConstraints.add((JCExpression)replace(e,basemap,jc)); //must translate!
				}

				for(VarSymbol vs:extraSyms)
					extra.add(vs.toString());
			}
			else
			{
				for(VarSymbol vs:extraVars)
					extra.add(vs.toString());
			}

			return toISLString(true,false,domsubst,extraConstraints,extra,true);
		}

		/*
		 check linear access:
		 * Barvinok:
		 * Access:={[j,..]->[c*j+...,...]}; //must be linear!
		 * (Access(iter_space)+base)=base //iter_space=inner2d,base=2d
		 * is access join base>base?Att
		 */

		public enum AccessSecurity
		{
			Safe,
			Unknown,
			Error
		};

		public AccessSecurity safeAccess(JCDiagnostic.DiagnosticPosition pos,Map<VarSymbol, JCExpression> lastProjectionArgs,JCTree.JCMethodDecl md, List<JCTree.JCVariableDecl> domargs,boolean reduce,Type.ArrayType range,Type.ArrayType mem,List<JCExpression> access, boolean iter_inside_projection, boolean no_reinterprete)
		{

			JavaCompiler jc=JavaCompiler.getCompiler();

			Map<VarSymbol, VarSymbol> index_map = new LinkedHashMap<VarSymbol, VarSymbol>(); //values from codegen need to replaced by the used supplied index in vsistVarSymbol (sequential emitter)

			List<VarSymbol> inds = mem.dom.getIndexList(new LinkedHashMap<VarSymbol, VarSymbol>());

			List<JCExpression> addressing = getAddressing(pos,index_map,mem,inds,iter_inside_projection,no_reinterprete);

			if(addressing==null)
				return AccessSecurity.Error;//some other error

			Map<VarSymbol, JCExpression> translate_map = new LinkedHashMap<VarSymbol, JCExpression>(); //values from codegen need to replaced by the used supplied index in vsistVarSymbol (sequential emitter)

			ArrayList<String> ivo=range.dom.getInterVectorOrder(pos);

			Map<String, VarSymbol> basemap = new LinkedHashMap<String, VarSymbol>();

			List<VarSymbol> rangeinds = range.dom.getIndexList(new LinkedHashMap<VarSymbol, VarSymbol>());

			for(int i=0;i<domargs.size()-(reduce?1:0);i++)
			{
				VarSymbol index=null;
				for(VarSymbol vs:rangeinds)
				{
					if(vs.toString().equals(ivo.get(i)))
					{
						index=vs;
						break;
					}
				}
				basemap.put(domargs.get(i).name.toString(), index);
			}

			ArrayList<String> memivo=mem.dom.getInterVectorOrder(pos);

			for(VarSymbol vs:index_map.keySet())
			{
				Map<String, VarSymbol> map = new LinkedHashMap<String, VarSymbol>();
				map.put(vs.toString(), index_map.get(vs));
				for(int i=0;i<memivo.size();i++)
				{
					if(memivo.get(i).equals(vs.toString()))
					{
						JCExpression basexp=(JCExpression)replace(access.get(i),basemap,jc);
						translate_map.put(index_map.get(vs),(JCExpression)replace(basexp,map,jc));
						break;
					}
				}
			}

			if(lastProjectionArgs!=null)
			{
				for(VarSymbol vs:lastProjectionArgs.keySet())
				{
					JCExpression e=lastProjectionArgs.get(vs);
					if(e.type.constValue()!=null)//only subst const vals!
						translate_map.put(vs,e);
				}
			}

			ListBuffer<JCExpression> lb=new ListBuffer<JCExpression>();

			for(JCExpression e:addressing)
			{
				lb.add((JCExpression)JCTree.replace(e,translate_map));
			}

			addressing = lb.toList();

			//range.dom.

			Type.ArrayType basetype=mem;
			Type.ArrayType baserangetype=range;

			String basemem,baserange;

			String baseaddr;
			String projArgs="";
			Set<String> accessVars=new LinkedHashSet<String>();

			if(!mem.treatAsBaseDomain()&&mem.getRealType().dom.accessCastMap!=null)
			{
				List<JCTree.JCExpression> acc=mem.getRealType().dom.accessCastMap;

				for(JCExpression e:acc)
					for(VarSymbol vs:JCTree.usedVars(e))
						accessVars.add(vs.toString());

				basetype=mem.getHighestBase(mem);
				baserangetype=range; //get compat

				if(baserangetype.isCast())//FIXME: need compatible dom
					baserangetype=baserangetype.getRealType();
				else
				{
					acc=mem.dom.accessElement;
					//JavaCompiler jc=JavaCompiler.getCompiler();

					ListBuffer<JCTree.JCExpression> fixAcc=new ListBuffer<JCExpression>();
					ListBuffer<JCTree.JCExpression> fixAdd=new ListBuffer<JCExpression>();
					for(int j=0;j<acc.size();j++)
					{
						if(acc.get(j).getTag()==JCTree.ARGEXPRESSION)
						{
							JCTree index=((JCTree.JCArgExpression)acc.get(j)).exp1;
							if(index.getTag()!=JCTree.IDENT&&index.getTag()!=JCTree.LITERAL)
							{
								//FIXME: replace computations by inverse or find better way to get acc
								if(acc.get(j).toString().equals(addressing.get(j).toString()))
								{
									JCTree.JCIdent id=jc.make.Ident(mem.dom.tsym.name.table.fromString("__I"+j+"__"));
									fixAcc.add(id);
									fixAdd.add(id);
								}
							}
							else
							{
								fixAcc.add(acc.get(j));
								fixAdd.add(addressing.get(j));
							}
						}
						else
						{
							fixAcc.add(acc.get(j));
							fixAdd.add(addressing.get(j));
						}
					}
					acc=fixAcc.toList();
					addressing=fixAdd.toList();
				}

				baseaddr="["+acc.toString()+"]";

				List<VarSymbol> baseinds = mem.getRealType().dom.projectionArgs;
				if(baseinds!=null&&!baseinds.isEmpty())
				{
					projArgs=baseinds.toString();
				}
			}
			else
			{
				List<VarSymbol> baseinds = range.dom.getIndexList(new LinkedHashMap<VarSymbol, VarSymbol>());
				for(VarSymbol vs:baseinds)
					accessVars.add(vs.toString());

				baseaddr="["+baseinds.toString()+"]";
				basetype=mem;
			}

			Set<JCExpression> extraConstraints=new LinkedHashSet<JCExpression>();
			Set<String> extra=new LinkedHashSet<String>();


			basemem=basetype.dom.getISLSubst(md,null,extra,basetype.dom.getSymbolicProjectionArgMap(),basemap);
			extraConstraints.clear();
			baserange=baserangetype.dom.getISLSubst(md,extraConstraints,extra,baserangetype.dom.getSymbolicProjectionArgMap(),basemap);

			for(JCExpression e:addressing)
			{
				for(String vs:JCTree.usedVarNames(e))
				{
					String name=vs;
					if(!(accessVars.contains(name)||basetype.dom.usesVar(name)||baserangetype.dom.usesVar(name)))
						extra.add(name);
				}
			}

			if(!extra.isEmpty())
			{
				boolean first=projArgs.isEmpty();

				for(String s:extra)
				{
					if(!first)
						projArgs+=", ";

					projArgs+=s;
					first=false;
				}
			}

			String res=ibarvinok.processNoError("(range ("+"["+projArgs+"] ->{"+baseaddr+"->["+addressing.toString()+"]}*("+baserange+"*params("+basemem+")))<= ("+basemem+"));");

			if(res.equals("ERROR"))
				return AccessSecurity.Unknown;

			boolean val=getBoolean(pos,res);

			if(val)
				return AccessSecurity.Safe;

			return AccessSecurity.Error;
		}

		public class DomSpecError
		{
			public DomainSpec result;
			public String details;

			public DomSpecError(DomainSpec result,String details)
			{
				this.result=result;
				this.details=details;
			}
		}
		//check that join of all doms in the list = this dom and meet of all doms in list is empty
		public DomSpecError isEqual(JCDiagnostic.DiagnosticPosition pos,JCTree.JCMethodDecl md,JCExpression [] params,List<Pair<DomainType,JCExpression []>> join)
		{

			//meta programming at it's best:
			//we want to know if Obj==Obj_0 ~ Obj_1 ~ ... ~ Obj_n
			//so if the join of all objects on the right equals Obj and all objects on the right have mutually empty meet
			//to avoid doing all n! meets we calc the join step wise and meet with the current join:
			/* we let barvinok execute the following algo:
			* meet:={};
			* join:=Obj_0;
			* forall Obj_i, i in [1,..,n]:
			* {
			* meet:=join*Obj_i;
			* if(meet!={}) => error (overspecified)
			* join:=join+Obj_i;
			* }
			* if(join!=Obj) => error (underspecified)
			*/

			ibarvinok.process(pos.getPreferredPosition(),"meet:={};");

			ibarvinok.processNoError("join:="+join.head.fst.getISLSubst(md,new LinkedHashSet<JCExpression>(),new LinkedHashSet<String>(),join.head.fst.getSymbolicProjectionArgMap(),new LinkedHashMap<String, VarSymbol>())+";");

			Iterator<Pair<DomainType,JCExpression []>> pi=join.iterator();
			pi.next();//skip first

			String res;

			while(pi.hasNext())
			{
				Pair<DomainType,JCExpression []> p=pi.next();
				String inst=p.fst.getISLSubst(md,new LinkedHashSet<JCExpression>(),new LinkedHashSet<String>(),p.fst.getSymbolicProjectionArgMap(),new LinkedHashMap<String, VarSymbol>());

				res=ibarvinok.processNoError("meet:=join*("+inst+");");
				if(res.equals("ERROR"))
					return new DomSpecError(DomainSpec.Unknown,inst);
				boolean meet_empty=isEmpty(pos,res);

				if(!meet_empty)
				{
					String instprev="unknown";
					DomainType prev=null;
					for(Pair<DomainType,JCExpression []> pp:join)
					{
						prev=pp.fst;
						instprev=pp.fst.getISLSubst(md,new LinkedHashSet<JCExpression>(),new LinkedHashSet<String>(),pp.fst.getSymbolicProjectionArgMap(),new LinkedHashMap<String, VarSymbol>());
						res=ibarvinok.processNoError("("+instprev+")*("+inst+");");
						if(res.equals("ERROR"))
							return new DomSpecError(DomainSpec.Unknown,inst);

						if(!isEmpty(pos,res))
						{
							break;
						}
					}

					String over=p.fst.tsym.name+" overlaps with "+prev.tsym.name+" at "+ibarvinok.process(pos.getPreferredPosition(),"meet;");

					return new DomSpecError(DomainSpec.OverSpecified,over);
				}


				res=ibarvinok.processNoError("join:=join+("+inst+");");
				if(res.equals("ERROR"))
					return new DomSpecError(DomainSpec.Unknown,inst);
			}

			res=ibarvinok.processNoError("("+getISLSubst(md,new LinkedHashSet<JCExpression>(),new LinkedHashSet<String>(),getSymbolicProjectionArgMap(),new LinkedHashMap<String, VarSymbol>())+"*params(join))=join;");

			if(res.equals("ERROR"))
				return new DomSpecError(DomainSpec.Unknown,getISLSubst(md,new LinkedHashSet<JCExpression>(),new LinkedHashSet<String>(),getSymbolicProjectionArgMap(),new LinkedHashMap<String, VarSymbol>()));

			boolean join_id=getBoolean(pos,res);

			if(!join_id)
			{
				String missing=ibarvinok.process("("+getISLSubst(md,new LinkedHashSet<JCExpression>(),new LinkedHashSet<String>(),getSymbolicProjectionArgMap(),new LinkedHashMap<String, VarSymbol>())+"*params(join))-join;");

				return new DomSpecError(DomainSpec.UnderSpecified,missing);
			}

			return new DomSpecError(DomainSpec.Equal,"");
		}

        public DomainType(Type outer, List<Type> typarams, TypeSymbol tsym) {
            super(DOMAIN, tsym);
            this.outer_field = outer;
            this.typarams_field = typarams;
//			this.declarationPos = declarationPos;
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitDomainType(this, s);
        }

        public Type constType(Object constValue) {
            final Object value = constValue;
            return new ClassType(getEnclosingType(), typarams_field, tsym) {
                    @Override
                    public Object constValue() {
                        return value;
                    }
                    @Override
                    public Type baseType() {
                        return tsym.type;
                    }
                };
        }

        /** The Java source which this type represents.
         */
        public String toString() {
            StringBuffer buf = new StringBuffer();
            if (getEnclosingType().tag == DOMAIN && tsym.owner.kind == TYP) {
                buf.append(getEnclosingType().toString());
                buf.append(".");
                buf.append(className(tsym, false));
            } else {
                buf.append(className(tsym, true));
				if(appliedParams!=null)
				{
					buf.append("{");
					buf.append(appliedParams);
					buf.append("}");
				}
            }
            if (getTypeArguments().nonEmpty()) {
                buf.append("{");
                buf.append(getTypeArguments().toString());
                buf.append("}");
            }
            return buf.toString();
        }

		public int getDim()
		{
			if(parentDomain!=null)
				return ((DomainType)parentDomain).getDim();
			return indices.size();
		}

		public List<VarSymbol> getIndexList(Map<VarSymbol,VarSymbol> map)
		{
			if(parentDomain!=null)
			{
				mapParams(map);

				return ((DomainType)parentDomain).getIndexList(map);
			}
			else
			{
				ListBuffer<VarSymbol> ni=new ListBuffer<VarSymbol>();
				for(VarSymbol vs:indices)
				{
					VarSymbol lu=map.get(vs);
					if(lu!=null)
						ni.add(lu);
					else
						ni.add(vs);
				}
				return ni.toList();
			}
		}
/*
		public List<VarSymbol> getParamIndexList(Map<VarSymbol,VarSymbol> map)
		{
			if(projectionArgs==null)
				return getIndexList(map);

			VarSymbol inds[]=getIndexList(map).toArray(new VarSymbol[0]);

			for(int d=0;d<projectionArgs.size();d++)
			{
				inds[getProjectionDim(d)]=projectionArgs.get(d);

			}
			ListBuffer<VarSymbol> ni=new ListBuffer<VarSymbol>();
			for(VarSymbol vs:inds)
				ni.add(vs);
			return ni.toList();
		}
*/
		public String getIndexConstraints(Map<VarSymbol,VarSymbol> map,Map<VarSymbol,String> subst)
		{
			StringBuffer buf = new StringBuffer();
			if(parentDomain!=null)
			{
				mapParams(map);

				return ((DomainType)parentDomain).getIndexConstraints(map,subst);
			}
			else
			{
				boolean first=true;
				for(VarSymbol vs:indices)
				{
					if(!first)
						buf.append(" and ");
					VarSymbol lu=map.get(vs);
					if(lu!=null)
						vs=lu;

					String s=null;
					if(subst!=null)
						s=subst.get(vs);
					if(s!=null)
						buf.append(s+">= 0");
					else
						buf.append(vs+">= 0");

					first=false;
				}

			}

			return buf.toString();
		}

		void mapParams(Map<VarSymbol,VarSymbol> map)
		{
			if(parentDomain!=null)
			{
				List<VarSymbol> ps=((DomainType)parentDomain).formalParams;
				for(VarSymbol vs:parentParams)
				{
					map.put(ps.head,vs);
					ps=ps.tail;
				}

				ps=((DomainType)parentDomain).indices;
				for(VarSymbol vs:indices)
				{
					map.put(ps.head,vs);
					ps=ps.tail;
				}
			}
		}
/*
		public Map<VarSymbol,VarSymbol> getProjectToIndex(Map<VarSymbol,VarSymbol> map,Set<VarSymbol> pp)
		{
			if(parentDomain!=null)
			{
				mapParams(map);
				return ((DomainType)parentDomain).getProjectToIndex(map,pp);
			}
			else
			{
				Set<VarSymbol> inds=new LinkedHashSet<VarSymbol>();
				inds.addAll(getIndexList(new LinkedHashMap<VarSymbol, VarSymbol>()));
				Map<VarSymbol,VarSymbol> pmap=new LinkedHashMap<VarSymbol, VarSymbol>();
				for(DomainConstraint dc:constraints)
				{
					Set<VarSymbol> proj=new LinkedHashSet<VarSymbol>();
					Set<VarSymbol> index=new LinkedHashSet<VarSymbol>();
					for(Map.Entry<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>> e : dc.coeffs.entrySet())
						for(Pair<Integer,VarSymbol> c:e.getValue())
						{
							VarSymbol vs=map.get(e.getKey());

							if(vs==null)
								vs=e.getKey();

							if(inds.contains(vs))
								index.add(vs);
							if(pp.contains(vs))
								proj.add(vs);

							VarSymbol s = c.snd;
							if(s!=null)
							{
								vs=map.get(s);
								if(vs==null)
									vs=s;

								if(inds.contains(vs))
									index.add(vs);
								if(pp.contains(vs))
									proj.add(vs);

							}
						}
					if(!index.isEmpty()&&!proj.isEmpty())
					{
						pmap.put(index.iterator().next(), proj.iterator().next());
					}
				}
				return pmap;

			}


		}
*/
		public String getConstraints(Map<VarSymbol,VarSymbol> map,Map<VarSymbol,String> subst,boolean project,boolean parent,boolean include_mappings)
		{
			StringBuffer buf = new StringBuffer();

			if(project||parent)
			for(DomainConstraint dc:constraints)
			{
				if(include_mappings||!dc.isMapping(this))
					buf.append(" and "+dc.toISLString(map,subst));
			}

			mapParams(map);

			if(parentDomain!=null)
			{
				buf.append(((DomainType)parentDomain).getConstraints(map,subst,true,parent,false));
			}
			if(parent&&resultDom!=null)
			{
				buf.append(((DomainType)resultDom).getConstraints(map,subst,true,parent,include_mappings));
			}

			return buf.toString();
		}

		private <T> String ListString(List<T> l)
		{
			StringBuffer buf = new StringBuffer();

			boolean first=true;
			for(T t:l)
			{
				if(!first)
					buf.append(", ");
				buf.append(t.toString());
				first=false;
			}

			return buf.toString();
		}

		private String allISLParams(Set<String> addparams)
		{
			StringBuffer buf = new StringBuffer();
			if(formalParams!=null)
			{
				buf.append("[");
				//buf.append(appliedParams);

				buf.append(formalParams.toString(", "));
				if(addparams!=null&&!addparams.isEmpty())
				{
					for(String s:addparams)
					{
						if(buf.length()>1)
							buf.append(", ");
						buf.append(s);
					}
				}
			}

			if(buf.length()>1)
				buf.append(", ");
			buf.append(ListString(getIndexList(new LinkedHashMap<VarSymbol,VarSymbol>())));

			if(projectionArgs!=null&&projectionArgs.length()>0)
			{
				if(buf.length()>1)
					buf.append(", ");
				buf.append(ListString(projectionArgs));
			}



			buf.append("] ->");
			return buf.toString();
		}

        public int getInterVectorDim(String index)
        {
            List<VarSymbol> inds= getIndexList(new LinkedHashMap<VarSymbol, VarSymbol>());
            for(int i=0;i<inds.size();i++)
            {
                if(inds.get(i).toString().equals(index))
                    return i;
            }
            return -1;
                
        }
        
        public List<JCExpression> projectAccess(JCDiagnostic.DiagnosticPosition pos,List<JCExpression> inargs)
        {
            ListBuffer<JCExpression> args=new ListBuffer<JCExpression>();
            
            for(int i=0;i<getInterVectorOrder(pos).size();i++)
            {
                args.add(inargs.get(getInterVectorDim(getInterVectorOrder(pos).get(i))));
            }
        
            return args.toList();
        }
        

		private String toISLParams(boolean project,boolean parent,boolean non_project,Set<String> addparams,boolean skipformals)
		{
			StringBuffer buf = new StringBuffer();
			if(formalParams!=null)
			{
				buf.append("[");
				//buf.append(appliedParams);

				if(!skipformals)
					buf.append(formalParams.toString(", "));
				if(addparams!=null&&!addparams.isEmpty())
				{
					for(String s:addparams)
					{
						if(buf.length()>1)
							buf.append(", ");
						buf.append(s);
					}
				}

				if(non_project)
				{
					if(buf.length()>1)
						buf.append(", ");
					buf.append(ListString(getIndexList(new LinkedHashMap<VarSymbol,VarSymbol>())));
				}
				else
				{
					if(project&&projectionArgs!=null&&projectionArgs.length()>0)
					{
						if(buf.length()>1)
							buf.append(", ");
						buf.append(ListString(projectionArgs));
					}

					if(parent&&resultDom!=null&&((DomainType)resultDom).projectionArgs!=null&&((DomainType)resultDom).projectionArgs.length()>0)
					{
						if(buf.length()>1)
							buf.append(", ");
						buf.append(ListString(((DomainType)resultDom).projectionArgs));
					}
				}

				buf.append("] ->");
			}
			else if(projectionArgs!=null&&projectionArgs.length()>0)
			{
				buf.append("[");
				buf.append(ListString(projectionArgs));
				buf.append("] ->");
			}
			return buf.toString();
		}

        public String toISLString(boolean project,boolean parent,Map<VarSymbol,String> subst,Set<JCExpression> extraConstraints,Set<String> extraVars,boolean skipformals) {
            StringBuilder buf = new StringBuilder();
			buf.append(toISLParams(project,parent,false,extraVars,skipformals));

			buf.append("{");

			List<VarSymbol> inds=getIndexList(new LinkedHashMap<VarSymbol,VarSymbol>());

			if(subst!=null)//rename base indices to something save, so that our substitutions do not interfere
			{
				for(VarSymbol vs:inds)
				{
					subst.put(vs, "_"+vs.toString()+"_");
				}
			}

			buf.append("[");
			boolean first=true;
			for(VarSymbol vs:inds)
			{
				if(!first)
					buf.append(", ");

				String s=null;
				if(subst!=null)
					s=subst.get(vs);
				if(s!=null)
					buf.append(s);
				else
					buf.append(vs.toString());
				first=false;
			}
			buf.append("] : ");

			buf.append(getIndexConstraints(new LinkedHashMap<VarSymbol,VarSymbol>(),subst));

			buf.append(getConstraints(new LinkedHashMap<VarSymbol,VarSymbol>(),subst,project,parent,true));

			if(extraConstraints!=null)
			{
				Set<String> done=new LinkedHashSet<String>();
				for(JCExpression e:extraConstraints)
				{
					String cst=e.toString(subst);

					if(!done.contains(cst))
					{
						buf.append(" and ");
						buf.append(cst.replaceAll("!", "not ").replaceAll("==", "=").replaceAll("%", " mod ").replaceAll("\\|\\|", " or "));
						done.add(cst);
					}
				}
			}

			buf.append("}");
            return buf.toString();
        }

//where
            private String className(Symbol sym, boolean longform) {
                if (sym.name.isEmpty() && (sym.flags() & COMPOUND) != 0) {
                    StringBuffer s = new StringBuffer(parentDomain.toString());
                    for (List<Type> is=interfaces_field; is.nonEmpty(); is = is.tail) {
                        s.append("&");
                        s.append(is.head.toString());
                    }
                    return s.toString();
                } else if (sym.name.isEmpty()) {
                    String s;
                    ClassType norm = (ClassType) tsym.type;
                    if (norm == null) {
                        s = Log.getLocalizedString("anonymous.class", (Object)null);
                    } else if (norm.interfaces_field != null && norm.interfaces_field.nonEmpty()) {
                        s = Log.getLocalizedString("anonymous.class",
                                                   norm.interfaces_field.head);
                    } else {
                        s = Log.getLocalizedString("anonymous.class",
                                                   norm.supertype_field);
                    }
                    if (moreInfo)
                        s += String.valueOf(sym.hashCode());
                    return s;
                } else if (longform) {
                    return sym.getQualifiedName().toString();
                } else {
                    return sym.name.toString();
                }
            }

        public List<Type> getTypeArguments() {
            if (typarams_field == null) {
                complete();
                if (typarams_field == null)
                    typarams_field = List.nil();
            }
            return typarams_field;
        }

        public boolean hasErasedSupertypes() {
            return isRaw();
        }

        public Type getEnclosingType() {
            return outer_field;
        }

        public void setEnclosingType(Type outer) {
            outer_field = outer;
        }

        public List<Type> allparams() {
            if (allparams_field == null) {
                allparams_field = getTypeArguments().prependList(getEnclosingType().allparams());
            }
            return allparams_field;
        }

        public boolean isErroneous() {
            return
                getEnclosingType().isErroneous() ||
                isErroneous(getTypeArguments()) ||
                this != tsym.type && tsym.type.isErroneous();
        }

        public boolean isParameterized() {
            return allparams().tail != null;
            // optimization, was: allparams().nonEmpty();
        }

        /** A cache for the rank. */
        int rank_field = -1;

        /** A class type is raw if it misses some
         *  of its type parameter sections.
         *  After validation, this is equivalent to:
         *  allparams.isEmpty() && tsym.type.allparams.nonEmpty();
         */
        public boolean isRaw() {
            return
                this != tsym.type && // necessary, but not sufficient condition
                tsym.type.allparams().nonEmpty() &&
                allparams().isEmpty();
        }

        public Type map(Mapping f) {
            Type outer = getEnclosingType();
            Type outer1 = f.apply(outer);
            List<Type> typarams = getTypeArguments();
            List<Type> typarams1 = map(typarams, f);
            if (outer1 == outer && typarams1 == typarams) return this;
            else return new ClassType(outer1, typarams1, tsym);
        }

        public boolean contains(Type elem) {
            return
                elem == this
                || (isParameterized()
                    && (getEnclosingType().contains(elem) || contains(getTypeArguments(), elem)));
        }

        public void complete() {
            if (tsym.completer != null) tsym.complete();
        }

        public TypeKind getKind() {
            return TypeKind.DECLARED;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitDeclared(this, p);
        }

        /** Returns the domain constraints of this domain.
         *  If the domain is applied, the parameters are replaced by their values. */
        public List<DomainConstraint> getRealConstraints() {
            ListBuffer<DomainConstraint> cs = new ListBuffer<DomainConstraint>();
            for(DomainConstraint c : constraints) {
                DomainConstraint newconstr = new DomainConstraint();
                newconstr.coeffs = new LinkedHashMap<VarSymbol, java.util.List<Pair<Integer,VarSymbol>>>();
                newconstr.eq = c.eq;
                newconstr.constant = c.constant;
                for(VarSymbol v : c.coeffs.keySet()) {
                    if(formalParams.contains(v)) {
                        int value = (Integer)appliedParams.get(formalParams.indexOf(v)).type.constValue();
						for(Pair<Integer,VarSymbol> co:c.coeffs.get(v))
	                        newconstr.constant -= value * co.fst;
                    } else {
                        newconstr.coeffs.put(v, c.coeffs.get(v));
                    }
                }
                cs.add(newconstr);
            }
            return cs.toList();
        }

    }

    public static class ErasedClassType extends ClassType {
        public ErasedClassType(Type outer, TypeSymbol tsym) {
            super(outer, List.<Type>nil(), tsym);
        }

        @Override
        public boolean hasErasedSupertypes() {
            return true;
        }
    }

    public static class ArrayType extends Type
            implements javax.lang.model.type.ArrayType {

        public Type elemtype;
		public DomainType dom;
		public ArrayType realtype;
//		public boolean convert = false;

        public ArrayType(Type elemtype, DomainType dom, TypeSymbol arrayClass) {
            super(ARRAY, arrayClass);
            this.elemtype = elemtype;
			this.dom = dom;

			//this.type_flags_field|=Flags.FOUT;

			realtype = this;
        }

		public boolean treatAsBaseDomain()
		{
			return (dom.projectionArgs==null||dom.projectionArgs.isEmpty())&&(dom.isBaseDomain||getRealType().dom.isReinterpreteProjection());
		}

		public ArrayType getHighestBase(ArrayType candidate)
		{
			if(!treatAsBaseDomain())
			{
				if(realtype==this)
					return getBaseType();

				return realtype.getHighestBase(realtype);
			}
			else
			{
				if(realtype==this)
					return candidate;
				return realtype.getHighestBase(candidate);
			}
		}
/*
		public boolean treatAsBaseDomain()
		{
			return getHighestBase(this)==this;
		}
*/
		public ArrayType getParentType()
		{
			if(dom.parentDomain==null)
				return this;
			Type.DomainType dt=(Type.DomainType)dom.parentDomain.clone();
			dt.appliedParams=dom.appliedParams;
			return new Type.ArrayType(elemtype,dt, tsym);
		}

		public ArrayType getNonDynamic()
		{
			if(this.realtype==this)//||(type_flags_field&Flags.COMPOUND)!=0)
				return this;
			else{
				//if prev type is a projection then that is our real type
				if(this.realtype.dom.isDynamic())
					return this;
				else
					return this.realtype.getNonDynamic();
			}
		}

		public ArrayType getRealType()
		{
			if(this.realtype==this)//||(type_flags_field&Flags.COMPOUND)!=0)
				return this;
			else{
				//if prev type is a projection then that is our real type
				if((this.realtype.type_flags_field&Flags.ABSTRACT)!=0)
					return this.realtype;
				return this.realtype.getRealType();
			}
		}

		public boolean getRealTypeDyn()
		{
			if(dom.isDynamic())
				return true;
			if(this.realtype==this)//||(type_flags_field&Flags.COMPOUND)!=0)
				return false;
			else{
				//if prev type is a projection then that is our real type
				if((this.realtype.type_flags_field&Flags.ABSTRACT)!=0)
					return this.realtype.dom.isDynamic();
				return this.realtype.getRealTypeDyn();
			}
		}

		public ArrayType getBaseType()
		{
			boolean isUnique=isLinear();
			if(getRealType()==this)
			{
				if(isUnique)
					return (ArrayType)this.getParentType().addFlag(Flags.LINEAR);
				else
					return this.getParentType();
			}
			else
			{
				if(isUnique)
					return (ArrayType)getRealType().getBaseType().addFlag(Flags.LINEAR);
				else
					return getRealType().getBaseType();
			}
		}

		public ArrayType getStartType()
		{
			if(getRealType()==this)
				return this;
			else
				return getRealType().getStartType();
		}

		static public int getCodeGenFlags(JCDomainIter tree,Type t) {
			int flag = 0;

            if ((tree.name!=null&&!tree.name.toString().equals("reduce")&&!tree.name.toString().equals("size"))||(t.type_flags_field&Flags.WORK)!=0) {
				flag |= Type.DomainType.ITER_OVER_PROJECTIONS; //iter over all rows
			} else {
				flag |= Type.DomainType.ITER_IN_PROJECTION; //iter over vals in row
			}

			return flag;
		}

		JCTree.JCMethodInvocation apply_index(JCTree.JCMethodDecl cu) {
		class Find extends TreeScanner {

				JCTree.JCMethodInvocation result = null;

				public void scan(JCTree tree) {
					if (tree != null) {
						tree.accept(this);
					}
				}

				public void visitApply(JCTree.JCMethodInvocation mi) {
					result = mi;
					super.visitApply(mi);
				}
			}
			Find v = new Find();
			v.scan(cu);
			return v.result;
		}

		public boolean isCompatibleToRealTypeAndIterable(JavaCompiler jc, DomainType projDom,JCDiagnostic.DiagnosticPosition pos) {

			Type.ArrayType at = this;
			Type.ArrayType rt = at.getRealType();

			//check thet inner dim is flat and continous
			String innerRt=rt.dom.getInterVectorOrder(pos).get(rt.dom.getInterVectorOrder(pos).size()-1);

			int v2[]=rt.dom.accessMap.get(innerRt);

			if(v2[v2.length-1]!=1) //is stride of inner dim = 1?
				return false;

			for(int d=0;d<v2.length-1;d++) //is inner dim flat?
				if(v2[d]!=0)
					return false;

			//check that used type has the same iter as the real type (e.g. one_d!=trace)
			String innerAt=at.dom.getInterVectorOrder(pos).get(at.dom.getInterVectorOrder(pos).size()-1);
			int v1[]=at.dom.accessMap.get(innerAt);

			if(v1.length!=v2.length)
				return false;

			for(int d=0;d<v2.length;d++)
				if(v1[d]!=v2[d])
					return false;

			return true;
		}

		public ArrayType(Type elemtype, DomainType dom, TypeSymbol arrayClass, ArrayType realtype) {
            super(ARRAY, arrayClass);
            this.elemtype = elemtype;
			this.dom = dom;
			//this.type_flags_field|=Flags.FOUT;
			this.realtype = realtype;
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitArrayType(this, s);
        }

        public String toString() {
            String prefix = "";
            if(realtype != null && realtype != this && dom!=null&& ! dom.isBaseDomain) {
                prefix = realtype + ".";
            }

			String sdom = "";
			if(dom!=null)
				sdom = dom.toString();
			String opt=(elemtype.isPointer()?"&":"");
			return prefix + elemtype + opt+ "[" + sdom + "]";
        }

        public boolean equals(Object obj) {
            return
                this == obj ||
                (obj instanceof ArrayType &&
                 this.elemtype.equals(((ArrayType)obj).elemtype));
        }

        public int hashCode() {
            return (ARRAY << 5) + elemtype.hashCode();
        }

        public List<Type> allparams() { return elemtype.allparams(); }

        public boolean isErroneous() {
            return elemtype.isErroneous();
        }

        public boolean isParameterized() {
            return elemtype.isParameterized();
        }

        public boolean isRaw() {
            return elemtype.isRaw();
        }

        public Type map(Mapping f) {
			//FIXME??
            Type elemtype1 = f.apply(elemtype);
            if (elemtype1 == elemtype) return this;
            else return new ArrayType(elemtype1, null, tsym);
        }

        public boolean contains(Type elem) {
            return elem == this || elemtype.contains(elem);
        }

        public void complete() {
            elemtype.complete();
        }

        public Type getComponentType() {
            return elemtype;
        }

        public TypeKind getKind() {
            return TypeKind.ARRAY;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitArray(this, p);
        }
    }

    public static class MethodType extends Type
                    implements Cloneable, ExecutableType {

        public List<Type> argtypes;
        public Type restype;
        public List<Type> thrown;
		public String template=null;

        public MethodType(List<Type> argtypes,
                          Type restype,
                          List<Type> thrown,
                          TypeSymbol methodClass) {
            super(METHOD, methodClass);
            this.argtypes = argtypes;
            this.restype = restype;
            this.thrown = thrown;
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitMethodType(this, s);
        }

        /** The Java source which this type represents.
         *
         *  XXX 06/09/99 iris This isn't correct Java syntax, but it probably
         *  should be.
         */
        public String toString() {

            return "(" + argtypes + ")" + restype;
        }

		public String toString(String varname) {

            return restype +"(*"+varname+")"+"(" + argtypes + ")";
        }

		public String toPrettyString()
		{
			if(template!=null)
				return template;
			else
				return toString();
		}

        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof MethodType))
                return false;
            MethodType m = (MethodType)obj;
            List<Type> args1 = argtypes;
            List<Type> args2 = m.argtypes;
            while (!args1.isEmpty() && !args2.isEmpty()) {
                if (!args1.head.equals(args2.head))
                    return false;
                args1 = args1.tail;
                args2 = args2.tail;
            }
            if (!args1.isEmpty() || !args2.isEmpty())
                return false;
            return restype.equals(m.restype);
        }

        public int hashCode() {
            int h = METHOD;
            for (List<Type> thisargs = this.argtypes;
                 thisargs.tail != null; /*inlined: thisargs.nonEmpty()*/
                 thisargs = thisargs.tail)
                h = (h << 5) + thisargs.head.hashCode();
            return (h << 5) + this.restype.hashCode();
        }

        public List<Type>        getParameterTypes() { return argtypes; }
        public Type              getReturnType()     { return restype; }
        public void              setReturnType(Type t)     { restype=t; }
        public List<Type>        getThrownTypes()    { return thrown; }

        public void setThrown(List<Type> t) {
            thrown = t;
        }

        public boolean isErroneous() {
            return
                isErroneous(argtypes) ||
                restype != null && restype.isErroneous();
        }

        public Type map(Mapping f) {
            List<Type> argtypes1 = map(argtypes, f);
            Type restype1 = f.apply(restype);
            List<Type> thrown1 = map(thrown, f);
            if (argtypes1 == argtypes &&
                restype1 == restype &&
                thrown1 == thrown) return this;
            else return new MethodType(argtypes1, restype1, thrown1, tsym);
        }

        public boolean contains(Type elem) {
            return elem == this || contains(argtypes, elem) || restype.contains(elem);
        }

        public MethodType asMethodType() { return this; }

        public void complete() {
            for (List<Type> l = argtypes; l.nonEmpty(); l = l.tail)
                l.head.complete();
            restype.complete();
            for (List<Type> l = thrown; l.nonEmpty(); l = l.tail)
                l.head.complete();
        }

        public List<TypeVar> getTypeVariables() {
            return List.nil();
        }

        public TypeSymbol asElement() {
            return null;
        }

        public TypeKind getKind() {
            return TypeKind.EXECUTABLE;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitExecutable(this, p);
        }
    }

    public static class PackageType extends Type implements NoType {

        PackageType(TypeSymbol tsym) {
            super(PACKAGE, tsym);
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitPackageType(this, s);
        }

        public String toString() {
            return tsym.getQualifiedName().toString();
        }

        public TypeKind getKind() {
            return TypeKind.PACKAGE;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitNoType(this, p);
        }
    }

    public static class TypeVar extends Type implements TypeVariable {

        /** The bound of this type variable; set from outside.
         *  Must be nonempty once it is set.
         *  For a bound, `bound' is the bound type itself.
         *  Multiple bounds are expressed as a single class type which has the
         *  individual bounds as superclass, respectively interfaces.
         *  The class type then has as `tsym' a compiler generated class `c',
         *  which has a flag COMPOUND and whose owner is the type variable
         *  itself. Furthermore, the erasure_field of the class
         *  points to the first class or interface bound.
         */
        public Type bound = null;
        public Type lower;

        public TypeVar(Name name, Symbol owner, Type lower) {
            super(TYPEVAR, null);
            tsym = new TypeSymbol(0, name, this, owner);
            this.lower = lower;
        }

        public TypeVar(TypeSymbol tsym, Type bound, Type lower) {
            super(TYPEVAR, tsym);
            this.bound = bound;
            this.lower = lower;
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitTypeVar(this, s);
        }

        public Type getUpperBound() { return bound; }

        int rank_field = -1;

        public Type getLowerBound() {
            return lower;
        }

        public TypeKind getKind() {
            return TypeKind.TYPEVAR;
        }

        public boolean isCaptured() {
            return false;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitTypeVariable(this, p);
        }
    }

    /** A captured type variable comes from wildcards which can have
     *  both upper and lower bound.  CapturedType extends TypeVar with
     *  a lower bound.
     */
    public static class CapturedType extends TypeVar {

        public Type lower;
        public WildcardType wildcard;

        public CapturedType(Name name,
                            Symbol owner,
                            Type upper,
                            Type lower,
                            WildcardType wildcard) {
            super(name, owner, lower);
            assert lower != null;
            this.bound = upper;
            this.lower = lower;
            this.wildcard = wildcard;
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitCapturedType(this, s);
        }

        public Type getLowerBound() {
            return lower;
        }

        @Override
        public boolean isCaptured() {
            return true;
        }

        @Override
        public String toString() {
            return "capture#"
                + (hashCode() & 0xFFFFFFFFL) % PRIME
                + " of "
                + wildcard;
        }
        static final int PRIME = 997;  // largest prime less than 1000
    }

    public static abstract class DelegatedType extends Type {
        public Type qtype;
        public DelegatedType(int tag, Type qtype) {
            super(tag, qtype.tsym);
            this.qtype = qtype;
        }
        public String toString() { return qtype.toString(); }
        public List<Type> getTypeArguments() { return qtype.getTypeArguments(); }
        public Type getEnclosingType() { return qtype.getEnclosingType(); }
        public List<Type> getParameterTypes() { return qtype.getParameterTypes(); }
        public Type getReturnType() { return qtype.getReturnType(); }
        public void              setReturnType(Type t)     { qtype.setReturnType(t); }
        public List<Type> getThrownTypes() { return qtype.getThrownTypes(); }
        public List<Type> allparams() { return qtype.allparams(); }
        public Type getUpperBound() { return qtype.getUpperBound(); }
        public Object clone() { DelegatedType t = (DelegatedType)super.clone(); t.qtype = (Type)qtype.clone(); return t; }
        public boolean isErroneous() { return qtype.isErroneous(); }
    }

    public static class ForAll extends DelegatedType
            implements Cloneable, ExecutableType {
        public List<Type> tvars;

        public ForAll(List<Type> tvars, Type qtype) {
            super(FORALL, qtype);
            this.tvars = tvars;
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitForAll(this, s);
        }

        public String toString() {
            return "<" + tvars + ">" + qtype;
        }

        public List<Type> getTypeArguments()   { return tvars; }

        public void setThrown(List<Type> t) {
            qtype.setThrown(t);
        }

        public Object clone() {
            ForAll result = (ForAll)super.clone();
            result.qtype = (Type)result.qtype.clone();
            return result;
        }

        public boolean isErroneous()  {
            return qtype.isErroneous();
        }

        public Type map(Mapping f) {
            return f.apply(qtype);
        }

        public boolean contains(Type elem) {
            return qtype.contains(elem);
        }

        public MethodType asMethodType() {
            return qtype.asMethodType();
        }

        public void complete() {
            for (List<Type> l = tvars; l.nonEmpty(); l = l.tail) {
                ((TypeVar)l.head).bound.complete();
            }
            qtype.complete();
        }

        public List<TypeVar> getTypeVariables() {
            return List.convert(TypeVar.class, getTypeArguments());
        }

        public TypeKind getKind() {
            return TypeKind.EXECUTABLE;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitExecutable(this, p);
        }
    }

    /** A class for instantiatable variables, for use during type
     *  inference.
     */
    public static class UndetVar extends DelegatedType {
        public List<Type> lobounds = List.nil();
        public List<Type> hibounds = List.nil();
        public Type inst = null;

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitUndetVar(this, s);
        }

        public UndetVar(Type origin) {
            super(UNDETVAR, origin);
        }

        public String toString() {
            if (inst != null) return inst.toString();
            else return qtype + "?";
        }

        public Type baseType() {
            if (inst != null) return inst.baseType();
            else return this;
        }
    }

    /** Represents VOID or NONE.
     */
    static class JCNoType extends Type implements NoType {
        public JCNoType(int tag) {
            super(tag, null);
        }

        @Override
        public TypeKind getKind() {
            switch (tag) {
            case VOID:  return TypeKind.VOID;
            case NONE:  return TypeKind.NONE;
            default:
                throw new AssertionError("Unexpected tag: " + tag);
            }
        }

        @Override
        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitNoType(this, p);
        }
    }

    static class BottomType extends Type implements NullType {
        public BottomType() {
            super(TypeTags.BOT, null);
			//type_flags_field|=Flags.FOUT;
        }

        @Override
        public TypeKind getKind() {
            return TypeKind.NULL;
        }

        @Override
        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitNull(this, p);
        }

        @Override
        public Type constType(Object value) {
            return this;
        }

        @Override
        public String stringValue() {
            return "null";
        }
    }

    public static class ErrorType extends ClassType
            implements javax.lang.model.type.ErrorType {

        private Type originalType = null;

        public ErrorType(Type originalType, TypeSymbol tsym) {
            super(noType, List.<Type>nil(), null);
            tag = ERROR;
            this.tsym = tsym;
            this.originalType = (originalType == null ? noType : originalType);
        }

        public ErrorType(ClassSymbol c, Type originalType) {
            this(originalType, c);
            c.type = this;
            c.kind = ERR;
            c.members_field = new Scope.ErrorScope(c);
        }

        public ErrorType(Name name, TypeSymbol container, Type originalType) {
            this(new ClassSymbol(PUBLIC|STATIC|ACYCLIC, name, null, container), originalType);
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitErrorType(this, s);
        }

        public Type constType(Object constValue) { return this; }
        public Type getEnclosingType()          { return this; }
        public Type getReturnType()              { return this; }
        public Type asSub(Symbol sym)            { return this; }
        public Type map(Mapping f)               { return this; }

        public boolean isGenType(Type t)         { return true; }
        public boolean isErroneous()             { return true; }
        public boolean isCompound()              { return false; }
        public boolean isInterface()             { return false; }

        public List<Type> allparams()            { return List.nil(); }
        public List<Type> getTypeArguments()     { return List.nil(); }

        public TypeKind getKind() {
            return TypeKind.ERROR;
        }

        public Type getOriginalType() {
            return originalType;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitError(this, p);
        }
    }

//ALEX

    public static class DomainErrorType extends DomainType
            implements javax.lang.model.type.ErrorType {

        private Type originalType = null;

        public DomainErrorType(Type originalType, TypeSymbol tsym) {
            super(noType, List.<Type>nil(), null);
            tag = ERROR;
            this.tsym = tsym;
            this.originalType = (originalType == null ? noType : originalType);
        }

        public DomainErrorType(DomainSymbol c, Type originalType) {
            this(originalType, c);
            c.type = this;
            c.kind = ERR;
            c.constraints_field = new Scope.ErrorScope(c);
        }

        public DomainErrorType(Name name, TypeSymbol container, Type originalType) {
            this(new DomainSymbol(PUBLIC|STATIC|ACYCLIC, name, null, container), originalType);
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitDomainErrorType(this, s);
        }

        public Type constType(Object constValue) { return this; }
        public Type getEnclosingType()          { return this; }
        public Type getReturnType()              { return this; }
        public Type asSub(Symbol sym)            { return this; }
        public Type map(Mapping f)               { return this; }

        public boolean isGenType(Type t)         { return true; }
        public boolean isErroneous()             { return true; }
        public boolean isCompound()              { return false; }
        public boolean isInterface()             { return false; }

        public List<Type> allparams()            { return List.nil(); }
        public List<Type> getTypeArguments()     { return List.nil(); }

        public TypeKind getKind() {
            return TypeKind.ERROR;
        }

        public Type getOriginalType() {
            return originalType;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitError(this, p);
        }
    }


    /**
     * A visitor for types.  A visitor is used to implement operations
     * (or relations) on types.  Most common operations on types are
     * binary relations and this interface is designed for binary
     * relations, that is, operations on the form
     * Type&nbsp;&times;&nbsp;S&nbsp;&rarr;&nbsp;R.
     * <!-- In plain text: Type x S -> R -->
     *
     * @param <R> the return type of the operation implemented by this
     * visitor; use Void if no return type is needed.
     * @param <S> the type of the second argument (the first being the
     * type itself) of the operation implemented by this visitor; use
     * Void if a second argument is not needed.
     */
    public interface Visitor<R,S> {
        R visitDomainType(DomainType t, S s);
        R visitClassType(ClassType t, S s);
//        R visitDomainType(ClassType t, S s);
        R visitWildcardType(WildcardType t, S s);
        R visitArrayType(ArrayType t, S s);
        R visitMethodType(MethodType t, S s);
        R visitPackageType(PackageType t, S s);
        R visitTypeVar(TypeVar t, S s);
        R visitCapturedType(CapturedType t, S s);
        R visitForAll(ForAll t, S s);
        R visitUndetVar(UndetVar t, S s);
        R visitErrorType(ErrorType t, S s);
        R visitDomainErrorType(DomainErrorType t, S s);
        R visitType(Type t, S s);
    }
}
