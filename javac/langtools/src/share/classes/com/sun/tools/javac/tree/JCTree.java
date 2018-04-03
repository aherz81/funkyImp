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

package com.sun.tools.javac.tree;

import java.util.*;

import java.io.IOException;
import java.io.StringWriter;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.source.tree.*;

import static com.sun.tools.javac.code.BoundKind.*;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.*;

/**
 * Root class for abstract syntax tree nodes. It provides definitions
 * for specific tree nodes as subclasses nested inside.
 *
 * <p>Each subclass is highly standardized.  It generally contains
 * only tree fields for the syntactic subcomponents of the node.  Some
 * classes that represent identifier uses or definitions also define a
 * Symbol field that denotes the represented identifier.  Classes for
 * non-local jumps also carry the jump target as a field.  The root
 * class Tree itself defines fields for the tree's type and position.
 * No other fields are kept in a tree node; instead parameters are
 * passed to methods accessing the node.
 *
 * <p>Except for the methods defined by com.sun.source, the only
 * method defined in subclasses is `visit' which applies a given
 * visitor to the tree. The actual tree processing is done by visitor
 * classes in other packages. The abstract class Visitor, as well as
 * an Factory interface for trees, are defined as inner classes in
 * Tree.
 *
 * <p>To avoid ambiguities with the Tree API in com.sun.source all sub
 * classes should, by convention, start with JC (javac).
 *
 * <p><b>This is NOT part of any API supported by Sun Microsystems.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 *
 * @see TreeMaker
 * @see TreeInfo
 * @see TreeTranslator
 * @see Pretty
 */
public abstract class JCTree implements Tree, Cloneable, DiagnosticPosition {

    /* Tree tag values, identifying kinds of trees */

	public static String MAIN_METHOD_TYPE = "(int,String[one_d{-1}])int";
	public static String MAIN_METHOD_NAME = "main";

    /** Toplevel nodes, of type TopLevel, representing entire source files.
     */
    public static final int  TOPLEVEL = 1;

    /** Import clauses, of type Import.
     */
    public static final int IMPORT = TOPLEVEL + 1;

    /** Class definitions, of type ClassDef.
     */
    public static final int CLASSDEF = IMPORT + 1;

     /** ArgExp definitions, of type ArgExpression.
     */
    public static final int ARGEXPRESSION = CLASSDEF + 1;
//ALEX
    /** Domain definitions, of type DomainDef.
     */
    public static final int DOMITER = ARGEXPRESSION + 1;

    /** Domain definitions, of type DomainDef.
     */
    public static final int DOMDEF = DOMITER + 1;

    /** Method definitions, of type MethodDef.
     */
    public static final int METHODDEF = DOMDEF + 1;

    /** Variable definitions, of type VarDef.
     */
    public static final int VARDEF = METHODDEF + 1;

    /** The no-op statement ";", of type Skip
     */
    public static final int SKIP = VARDEF + 1;

    /** The no-op statement ";", of type Skip
     */
    public static final int CF = SKIP + 1;

    /** The no-op statement ";", of type Skip
     */
    public static final int SET = CF + 1;

    /** Blocks, of type Block.
     */
    public static final int BLOCK = SET + 1;

    /** Do-while loops, of type DoLoop.
     */
    public static final int DOLOOP = BLOCK + 1;

    /** While-loops, of type WhileLoop.
     */
    public static final int WHILELOOP = DOLOOP + 1;

    /** For-loops, of type ForLoop.
     */
    public static final int FORLOOP = WHILELOOP + 1;

    /** Foreach-loops, of type ForeachLoop.
     */
    public static final int FOREACHLOOP = FORLOOP + 1;

    /** Labelled statements, of type Labelled.
     */
    public static final int LABELLED = FOREACHLOOP + 1;

    /** Switch statements, of type Switch.
     */
    public static final int SWITCH = LABELLED + 1;

    /** Case parts in switch statements, of type Case.
     */
    public static final int CASE = SWITCH + 1;

    /** Synchronized statements, of type Synchonized.
     */
    public static final int SYNCHRONIZED = CASE + 1;

    /** Try statements, of type Try.
     */
    public static final int TRY = SYNCHRONIZED + 1;

    /** Catch clauses in try statements, of type Catch.
     */
    public static final int CATCH = TRY + 1;

    /** Conditional expressions, of type Conditional.
     */
    public static final int CONDEXPR = CATCH + 1;

    /** Conditional statements, of type If.
     */
    public static final int IF = CONDEXPR + 1;

    /** Conditional statements, of type If.
     */
    public static final int IFEXP = IF + 1;

    /** Expression statements, of type Exec.
     */
    public static final int EXEC = IFEXP + 1;

    /** Break statements, of type Break.
     */
    public static final int BREAK = EXEC + 1;

    /** Continue statements, of type Continue.
     */
    public static final int CONTINUE = BREAK + 1;

    /** Return statements, of type Return.
     */
    public static final int RETURN = CONTINUE + 1;

    /** Throw statements, of type Throw.
     */
    public static final int THROW = RETURN + 1;

    /** Assert statements, of type Assert.
     */
    public static final int ASSERT = THROW + 1;
    /** Assert statements, of type Assert.
     */
    public static final int PRAGMA = ASSERT + 1;

    /** Method invocation expressions, of type Apply.
     */
    public static final int APPLY = PRAGMA + 1;

    /** Class instance creation expressions, of type NewClass.
     */
    public static final int NEWCLASS = APPLY + 1;

    /** Array creation expressions, of type NewArray.
     */
    public static final int NEWARRAY = NEWCLASS + 1;

    /** Parenthesized subexpressions, of type Parens.
     */
    public static final int PARENS = NEWARRAY + 1;

    /** Assignment expressions, of type Assign.
     */
    public static final int ASSIGN = PARENS + 1;

    /** Where expressions, of type Where.
     */
    public static final int WHERE = ASSIGN + 1;

    /** Where expressions, of type Where.
     */
    public static final int FOR = WHERE + 1;

    /** Where expressions, of type Where.
     */
    public static final int SELECTCOND = FOR + 1;

    /** Where expressions, of type Where.
     */
    public static final int SELECT = SELECTCOND + 1;

    /** Type cast expressions, of type TypeCast.
     */
    public static final int TYPECAST = SELECT + 1;

    /** Type test expressions, of type TypeTest.
     */
    public static final int TYPETEST = TYPECAST + 1;

    /** Indexed array expressions, of type Indexed.
     */
    public static final int INDEXED = TYPETEST + 1;

    /** Selections, of type Select.
     */
    public static final int SELECTEXP = INDEXED + 1;

    /** Selections, of type Select.
     */
    public static final int CASEEXP = SELECTEXP + 1;

	public static final int SEQUENCE = CASEEXP + 1;

	public static final int JOIN = SEQUENCE + 1;

    /** Simple identifiers, of type Ident.
     */
    public static final int IDENT = JOIN + 1;

	/** Simple identifiers, of type Ident.
     */
    public static final int SIZEOF = IDENT + 1;

    /** Literals, of type Literal.
     */
    public static final int LITERAL = SIZEOF + 1;

    /** Basic type identifiers, of type TypeIdent.
     */
    public static final int TYPEIDENT = LITERAL + 1;

    /** Array types, of type TypeArray.
     */
    public static final int TYPEARRAY = TYPEIDENT + 1;

    /** Parameterized types, of type TypeApply.
     */
    public static final int TYPEAPPLY = TYPEARRAY + 1;

    /** Formal type parameters, of type TypeParameter.
     */
    public static final int TYPEPARAMETER = TYPEAPPLY + 1;

//ALEX
    /** Formal type parameters, of type TypeParameter.
     */
    public static final int DOMCONSTRAINTVALUE = TYPEPARAMETER + 1;

    /** Formal type parameters, of type TypeParameter.
     */
    public static final int DOMCONSTRAINT = DOMCONSTRAINTVALUE + 1;

    /** Formal type parameters, of type TypeParameter.
     */
    public static final int DOMPARAMETER = DOMCONSTRAINT + 1;

    /** Formal type parameters, of type TypeParameter.
     */
    public static final int CTPROPERTY = DOMPARAMETER + 1;

    /** Formal type parameters, of type TypeParameter.
     */
    public static final int DOMINSTANCE = CTPROPERTY + 1;

	public static final int DOMPARENT = DOMINSTANCE + 1;

	public static final int DOMUSAGE = DOMPARENT + 1;

//END
    /** Type argument.
     */
    public static final int WILDCARD = DOMUSAGE + 1;

    /** Bound kind: extends, super, exact, or unbound
     */
    public static final int TYPEBOUNDKIND = WILDCARD + 1;

    /** metadata: Annotation.
     */
    public static final int ANNOTATION = TYPEBOUNDKIND + 1;

    /** metadata: Modifiers
     */
    public static final int MODIFIERS = ANNOTATION + 1;

    /** Error trees, of type Erroneous.
     */
    public static final int ERRONEOUS = MODIFIERS + 1;

    /** Unary operators, of type Unary.
     */
    public static final int POS = ERRONEOUS + 1;             // +
    public static final int NEG = POS + 1;                   // -
    public static final int NOT = NEG + 1;                   // !
    public static final int COMPL = NOT + 1;                 // ~
    public static final int PREINC = COMPL + 1;              // ++ _
    public static final int PREDEC = PREINC + 1;             // -- _
    public static final int POSTINC = PREDEC + 1;            // _ ++
    public static final int POSTDEC = POSTINC + 1;           // _ --

    /** unary operator for null reference checks, only used internally.
     */
    public static final int NULLCHK = POSTDEC + 1;

    /** Binary operators, of type Binary.
     */
    public static final int OR = NULLCHK + 1;                // ||
    public static final int AND = OR + 1;                    // &&
    public static final int BITOR = AND + 1;                 // |
    public static final int BITXOR = BITOR + 1;              // ^
    public static final int BITAND = BITXOR + 1;             // &
    public static final int EQ = BITAND + 1;                 // ==
    public static final int NE = EQ + 1;                     // !=
    public static final int LT = NE + 1;                     // <
    public static final int GT = LT + 1;                     // >
    public static final int LE = GT + 1;                     // <=
    public static final int GE = LE + 1;                     // >=
    public static final int SL = GE + 1;                     // <<
    public static final int SR = SL + 1;                     // >>
    public static final int USR = SR + 1;                    // >>>
    public static final int SEQ = USR + 1;                    // >>>
    public static final int PLUS = SEQ + 1;                  // +
    public static final int MINUS = PLUS + 1;                // -
    public static final int MUL = MINUS + 1;                 // *
    public static final int DIV = MUL + 1;                   // /
    public static final int MOD = DIV + 1;                   // %

    /** Assignment operators, of type Assignop.
     */
    public static final int BITOR_ASG = MOD + 1;             // |=
    public static final int BITXOR_ASG = BITOR_ASG + 1;      // ^=
    public static final int BITAND_ASG = BITXOR_ASG + 1;     // &=

    public static final int SL_ASG = SL + BITOR_ASG - BITOR; // <<=
    public static final int SR_ASG = SL_ASG + 1;             // >>=
    public static final int USR_ASG = SR_ASG + 1;            // >>>=
    public static final int PLUS_ASG = USR_ASG + 1;          // +=
    public static final int MINUS_ASG = PLUS_ASG + 1;        // -=
    public static final int MUL_ASG = MINUS_ASG + 1;         // *=
    public static final int DIV_ASG = MUL_ASG + 1;           // /=
    public static final int MOD_ASG = DIV_ASG + 1;           // %=

    /** A synthetic let expression, of type LetExpr.
     */
    public static final int LETEXPR = MOD_ASG + 1;           // ala scheme

    /** The offset between assignment operators and normal operators.
     */
    public static final int ASGOffset = BITOR_ASG - BITOR;

	public static JCTree replace(JCTree cu, final Map<VarSymbol, JCExpression> map) {
			class Replace extends TreeTranslator {

//				int discardLoops=-1;

				public void scan(JCTree tree) {
					if (tree != null) {
						tree.accept(this);
					}
				}

				public void visitIdent(JCTree.JCIdent tree) {
					JCExpression e = map.get(tree.sym);
					if (e != null) {
						result=e;
					}
					else
						super.visitIdent(tree);
				}
			}
			Replace v = new Replace();
			//cu=(JCTree)(new TreeCopier(jc.make)).copy(cu);
			return v.translate(cu);
		}

	public static Set<VarSymbol> usedVars(JCTree cu) {
		class Find extends TreeScanner {

			Set<VarSymbol> result = new LinkedHashSet<VarSymbol>();

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitIdent(JCIdent tree) {

				if (tree.sym instanceof VarSymbol) {
					result.add((VarSymbol) tree.sym);
				}
				super.visitIdent(tree);
			}
		}
		Find v = new Find();
		v.scan(cu);
		return v.result;
	}

	public static Set<String> usedVarNames(JCTree cu) {
		class Find extends TreeScanner {

			Set<String> result = new LinkedHashSet<String>();

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitIdent(JCIdent tree) {

					result.add(tree.name.toString());
				super.visitIdent(tree);
			}
		}
		Find v = new Find();
		v.scan(cu);
		return v.result;
	}

	public static JCTree baseArray(final JCTree cu) {
		class Find extends TreeScanner {

			JCTree result = cu;

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitSelect(JCFieldAccess tree) {

				if((result==null||result.type.tag==TypeTags.ARRAY)&&tree.selected.type.getArrayType().tag==TypeTags.ARRAY)
				{
					result=tree.selected;
					super.visitSelect(tree);
				}
			}

            public void visitIndexed(JCArrayAccess tree) {
                result=tree.indexed;
                super.visitIndexed(tree);
            }
		}
		Find v = new Find();
		v.scan(cu);
		return v.result;
	}


	public static boolean usedOp(JCTree cu,final int op) {
		class Find extends TreeScanner {

			boolean result = false;

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitBinary(JCBinary tree) {

				if(tree.opcode==op)
				{
					result=true;
					return;
				}

				super.visitBinary(tree);
			}
		}
		Find v = new Find();
		v.scan(cu);
		return v.result;
	}

    /* The (encoded) position in the source file. @see util.Position.
     */
    public int pos;

    /* The type of this node.
     */
    public Type type;

	public int transitive_returns = 0;
	public int local_returns = 0;
	public boolean nop = false;
	public boolean nop_if_alone = false;
	public boolean isScheduler = false;

	public Set<VarSymbol> time=new LinkedHashSet<VarSymbol>();

	public long getTypeFlags()
	{
		if(type==null)
			return 0;
		return type.type_flags_field;
	}

    /* The tag of this node -- one of the constants declared above.
     */
    public abstract int getTag();

    public static class Arc// extends DefaultEdge
    {
        public JCTree t,s;
        public VarSymbol v;

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Arc other = (Arc) obj;
            if (this.t != other.t && (this.t == null || !this.t.equals(other.t))) {
                return false;
            }
            if (this.s != other.s && (this.s == null || !this.s.equals(other.s))) {
                return false;
            }
            if (this.v != other.v && (this.v == null || !this.v.equals(other.v))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (this.t != null ? this.t.hashCode() : 0);
            hash = 29 * hash + (this.s != null ? this.s.hashCode() : 0);
            hash = 29 * hash + (this.v != null ? this.v.hashCode() : 0);
            return hash;
        }

        //removed hashcode equals cause it interferes with jgrapht

        public Arc(JCTree s,JCTree t,VarSymbol v)
        {
            this.s=s;
            this.t=t;
            this.v=v;
        }

		public String toString()
		{
			if(null!=v)
				return s.toFlatString()+"-("+v.toString()+")->"+t.toFlatString();
			else
				return s.toFlatString()+"-->"+t.toFlatString();
		}

    }

	public static class TopolComparator implements Comparator<JCTree> {
		java.util.Map<JCTree,Integer> topolNodes;
		TopolComparator(java.util.Map<JCTree,Integer> topolNodes)
		{
			this.topolNodes=topolNodes;
		}
	@Override
		public int compare(JCTree o1, JCTree o2) {
			if(topolNodes.get(o1)>topolNodes.get(o2))
				return 1;
			else if(topolNodes.get(o1)<topolNodes.get(o2))
				return -1;
			else
				return 0;
		}
	}

    public class DepGraphNode
    {
        //interface to build/access dependency graph:
        //public Set<Arc> dependent_childs;
        public boolean source_connected=true;
        public boolean target_connected=true;
        public JCTree source,self;
		final double FAR=1.0e32;

        //public double[] distance = null;
        //public JCTree[] prec = null;
        //public int topolId = -1;

		public Map<Pair<java.util.Map<JCTree,Integer>,DirectedGraph<JCTree, Arc>>,double[]> distanceCache=new LinkedHashMap<Pair<java.util.Map<JCTree,Integer>,DirectedGraph<JCTree, Arc>>,double[]>();
		public Map<Pair<java.util.Map<JCTree,Integer>,DirectedGraph<JCTree, Arc>>,JCTree[]> precCache=new LinkedHashMap<Pair<java.util.Map<JCTree,Integer>,DirectedGraph<JCTree, Arc>>,JCTree[]>();
        //public boolean distance_cached=false;

        public void InitDistance(int nodeCount,java.util.Map<JCTree,Integer> topolNodes,boolean self_connected,DirectedGraph<JCTree, Arc> depGraph)
        {
			double[] distance=distanceCache.get(topolNodes);
            //this node with topolId is source!!
            if(distance==null)
            {
                distance=new double[nodeCount];
				distanceCache.put(new Pair<java.util.Map<JCTree,Integer>,DirectedGraph<JCTree, Arc>>(topolNodes,depGraph), distance);
                JCTree[] prec=new JCTree[nodeCount];
				precCache.put(new Pair<java.util.Map<JCTree,Integer>,DirectedGraph<JCTree, Arc>>(topolNodes,depGraph), prec);

                for(int i=0;i<nodeCount;)
                {
                    distance[i]=Double.POSITIVE_INFINITY;
                    prec[i]=null;
                    i++;
                }

				if(self_connected)
					distance[topolNodes.get(self)]=0.0;
				else
					distance[topolNodes.get(self)]=0.5;
            }
        }

		public JCTree[] getTopolList(java.util.Map<JCTree,Integer> topolNodes)
		{
			JCTree[] topNodes=topolNodes.keySet().toArray(new JCTree[0]);
			java.util.Arrays.sort(topNodes, new TopolComparator(topolNodes));
			return topNodes;
		}

        //(Introduction to Algorithms chap 24.2) O(m+n) for DAG
        public void CalcDAGShortestPath(boolean verbose,java.util.Map<JCTree,Integer> topolNodes,DirectedGraph<JCTree, Arc> depGraph,boolean self_connected)
        {
			Pair<java.util.Map<JCTree,Integer>,DirectedGraph<JCTree, Arc>> hash=new Pair<java.util.Map<JCTree,Integer>,DirectedGraph<JCTree, Arc>>(topolNodes,depGraph);
            if(distanceCache.get(hash)!=null)
                return;

            InitDistance(depGraph.vertexSet().size(),topolNodes,self_connected,depGraph);

            Object[] topNodes=getTopolList(topolNodes);

			double[] distance=distanceCache.get(hash);

			JCTree[] prec=precCache.get(hash);

			//ALEX: call graph also uses this but has no topol order, so we start at 0 instead of self.topolId
			//FIXME: call graph is no DAG, so this is probably wrong (well, it's probably ok as long as we care about reachability only)
            for(int i = 0;i<topolNodes.size();)//skip nodes with smaller topolId (cannot be reachable from this node)
            {
                JCTree node=(JCTree)topNodes[i];
                Set<Arc> outEdges = depGraph.outgoingEdgesOf(node);
                for(Iterator<Arc> edges=outEdges.iterator();edges.hasNext();)
                {
                    //Relax()
                    Arc edge=edges.next();
                    int sid=topolNodes.get(node);
                    int tid=topolNodes.get(edge.t);

                    if(distance[tid]>distance[sid]+1)//FIXME: weight
                    {
                        distance[tid]=distance[sid]+1.0;
                        prec[tid]=node;
                    }

					if(node==self&&edge.t==self)
						distance[tid]=0.0;
                }
                i++;
            }
        }

		public double[] calcReaching(boolean verbose,java.util.Map<JCTree,Integer> topolNodes,DirectedGraph<JCTree, Arc> depGraph,boolean self_connected)
		{
            CalcDAGShortestPath(verbose,topolNodes,depGraph,self_connected);
			Pair<java.util.Map<JCTree,Integer>,DirectedGraph<JCTree, Arc>> hash=new Pair<java.util.Map<JCTree,Integer>,DirectedGraph<JCTree, Arc>>(topolNodes,depGraph);
			return distanceCache.get(hash);
		}

		public int getMaxReaching(boolean verbose,JCTree node1,JCTree node2,java.util.Map<JCTree,Integer> topolNodes,DirectedGraph<JCTree, Arc> depGraph,boolean self_connected)
		{
			JCTree [] topNodes=getTopolList(topolNodes);

			for(int t=topNodes.length-1;t>=0;t--)
			{
				int topid=topolNodes.get(topNodes[t]);
				double[] distance=topNodes[t].getDGNode().calcReaching(verbose,topolNodes,depGraph,self_connected);
				boolean r1=distance[topolNodes.get(node1)]<Double.POSITIVE_INFINITY;
				boolean r2=distance[topolNodes.get(node2)]<Double.POSITIVE_INFINITY;
				if(r1&&r2)
					return topid;
			}
			return 0;
		}

        public boolean IsReachable(boolean verbose,JCTree node,java.util.Map<JCTree,Integer> topolNodes,DirectedGraph<JCTree, Arc> depGraph,boolean self_connected)
        {
			double[] distance=calcReaching(verbose,topolNodes,depGraph,self_connected);

			double dist=distance[topolNodes.get(node)];

			if(self_connected)
				return (dist<Double.POSITIVE_INFINITY);
			else
				return (dist<0.5);
        }

        public List<JCTree> getShortestPath(boolean verbose,JCTree to,java.util.Map<JCTree,Integer> topolNodes,DirectedGraph<JCTree, Arc> depGraph)
        {
			if(to==self)
			{
				ListBuffer<JCTree> path=new ListBuffer<JCTree>();
				path.add(self);
				return path.toList();
			}

            CalcDAGShortestPath(verbose,topolNodes,depGraph,true);

			Pair<java.util.Map<JCTree,Integer>,DirectedGraph<JCTree, Arc>> hash=new Pair<java.util.Map<JCTree,Integer>,DirectedGraph<JCTree, Arc>>(topolNodes,depGraph);
            ArrayList<JCTree> inverse_path=new ArrayList<JCTree>();
			double[] distance=distanceCache.get(hash);

			JCTree[] prec=precCache.get(hash);
            double dist=distance[topolNodes.get(to)];
            if(dist<Double.POSITIVE_INFINITY)
            {
                int curId=topolNodes.get(to);
                inverse_path.add(to);
                if(dist>0.0)
                while(prec[curId]!=self)
                {
                    JCTree curPrec=prec[curId];
                    inverse_path.add(curPrec);
                    curId=topolNodes.get(curPrec);
                }

				inverse_path.add(self);

                //invert path
                ListBuffer<JCTree> path=new ListBuffer<JCTree>();

                int size=inverse_path.size();
                for(int c=0;c<size;)
                {
                    path.add(inverse_path.get(size-c-1));
                    c++;
                }

                return path.toList();
            }
            else
				return (new ListBuffer<JCTree>()).toList();
        }

        DepGraphNode(JCTree src)
        {
            self=src;
        }

        public void sourceConnect()
        {
            source_connected=true; //reenable deletion of anxillary inbound arcs
        }

        public void setSource(JCTree s)
        {
            if(source==null)//first setter is retained
                source=s;
        }

        public void addDependentChildJGT(DirectedGraph<JCTree, Arc> g,JCTree t,VarSymbol v,JCTree current_source,JCTree target)
        {
            if(target!=null&&target_connected)
            {
                target_connected=false; //remove anxillary arc from this node to the sink
                g.removeEdge(self, target);
            }

            t.getDGNode().setSource(current_source);//store souce for this node (last branch or meth decl)

            if(t.getDGNode().source_connected)
            {
                if(current_source!=null&&(t.getDGNode().source instanceof JCMethodDecl))//arcs from mehtdecl are a special case
                {
                    t.getDGNode().source_connected=false; //remove anxillary node from source to this node
                    g.removeEdge(current_source,t);
                }
            }

            if(t.getDGNode().source_connected)
            {
                if(t.getDGNode().source==source&&source!=null)//make sure that we are rooted at the same node
                {
                    t.getDGNode().source_connected=false; //remove anxillary node from source to this node
                    g.removeEdge(source, t);
                }
            }
/*
            GraphPath<JCTree, Arc> gp = (new DijkstraShortestPath<JCTree, Arc>(g, t, self)).getPath();
            if(gp!=null)
            {
                int i=0;
            }
*/
            boolean succ=g.addEdge(self,t,new Arc(self,t,v));
            assert(succ);
        }
    }

    private DepGraphNode dgNode=null;

	public void resetDGNode()
	{
		dgNode=null;
	}

    public DepGraphNode getDGNode()
    {
        if(dgNode==null)
            dgNode = new DepGraphNode(this);

        return dgNode;
    }

    private java.util.ArrayList<iTask> pathSets=null;

	public void resetPathSets()
	{
		pathSets=null;
	}

    public java.util.ArrayList<iTask> getPathSets()
    {
        if(pathSets==null)
            pathSets = new java.util.ArrayList<iTask>();

        return pathSets;
    }

    private Set<iTask> schedule = null;


	public void resetSchedule()
	{
		schedule=null;
	}

    public Set<iTask> getSchedule()
    {
        if(schedule==null)
            schedule=new LinkedHashSet<iTask>();

        return schedule;
    }

    public JCTree scheduler=null;

	public boolean is_blocking=false;

	public Set<String> groups = new LinkedHashSet<String>();
	public Set<String> threads = new LinkedHashSet<String>();

	public void copyNonAst(JCTree from)
	{
		this.dgNode=from.dgNode;
		this.groups=from.groups;
		this.is_blocking=from.is_blocking;
		this.local_returns=from.local_returns;
		this.nop=from.nop;
		this.nop_if_alone=from.nop_if_alone;
		this.pathSets=from.pathSets;
		this.schedule=from.schedule;
		this.scheduler=from.scheduler;
		this.threads=from.threads;
		this.time=from.time;
		this.transitive_returns=from.transitive_returns;
		this.type=from.type;
	}

    //public String getClassName() {this.getClass().getName();};

    /** Convert a tree to a pretty-printed string. */
    public String toString() {
        StringWriter s = new StringWriter();
        try {
            new Pretty(s, false, false, false).printExpr(this);
        }
        catch (IOException e) {
            // should never happen, because StringWriter is defined
            // never to throw any IOExceptions
            throw new AssertionError(e);
        }
        return s.toString();
    }


	public String toString(Map<VarSymbol,String> subst) {
        StringWriter s = new StringWriter();
        try {
            new Pretty(s, false, false, false,subst).printExpr(this);
        }
        catch (IOException e) {
            // should never happen, because StringWriter is defined
            // never to throw any IOExceptions
            throw new AssertionError(e);
        }
        return s.toString();
    }


    public String toDeepString() {
        StringWriter s = new StringWriter();
        try {
            new Pretty(s,true).printExpr(this);
        }
        catch (IOException e) {
            // should never happen, because StringWriter is defined
            // never to throw any IOExceptions
            throw new AssertionError(e);
        }
        return s.toString();
    }

    /** Convert a tree to a pretty-printed string. */
    public String toFlatString() {
        StringWriter s = new StringWriter();
        try {
            new Pretty(s, false, false).printExpr(this);
        }
        catch (IOException e) {
            // should never happen, because StringWriter is defined
            // never to throw any IOExceptions
            throw new AssertionError(e);
        }
        String res=s.toString();

        return res.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "''");
    }

    /** Set position field and return this tree.
     */
    public JCTree setPos(int pos) {
        this.pos = pos;
        return this;
    }

    /** Set type field and return this tree.
     */
    public JCTree setType(Type type) {
        this.type = type;
        return this;
    }

    /** Visit this tree with a given visitor.
     */
    public abstract void accept(Visitor v);

    public abstract <R,D> R accept(TreeVisitor<R,D> v, D d);

    /** Return a shallow copy of this tree.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch(CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /** Get a default position for this tree node.
     */
    public DiagnosticPosition pos() {
        return this;
    }

    // for default DiagnosticPosition
    public JCTree getTree() {
        return this;
    }

    // for default DiagnosticPosition
    public int getStartPosition() {
        return TreeInfo.getStartPos(this);
    }

    // for default DiagnosticPosition
    public int getPreferredPosition() {
        return pos;
    }

    // for default DiagnosticPosition
    public int getEndPosition(Map<JCTree, Integer> endPosTable) {
        return TreeInfo.getEndPos(this, endPosTable);
    }

    /**
     * Everything in one source file is kept in a TopLevel structure.
     * @param pid              The tree representing the package clause.
     * @param sourcefile       The source file name.
     * @param defs             All definitions in this file (ClassDef, Import, and Skip)
     * @param packge           The package it belongs to.
     * @param namedImportScope A scope for all named imports.
     * @param starImportScope  A scope for all import-on-demands.
     * @param lineMap          Line starting positions, defined only
     *                         if option -g is set.
     * @param docComments      A hashtable that stores all documentation comments
     *                         indexed by the tree nodes they refer to.
     *                         defined only if option -s is set.
     * @param endPositions     A hashtable that stores ending positions of source
     *                         ranges indexed by the tree nodes they belong to.
     *                         Defined only if option -Xjcov is set.
     */
    public static class JCCompilationUnit extends JCTree implements CompilationUnitTree {
        public List<JCAnnotation> packageAnnotations;
        public JCExpression pid;
        public List<JCTree> defs;
        public JavaFileObject sourcefile;
        public PackageSymbol packge;
        public Scope namedImportScope;
        public Scope starImportScope;
        public long flags;
        public Position.LineMap lineMap = null;
        public Map<JCTree, String> docComments = null;
        public Map<JCTree, Integer> endPositions = null;
        protected JCCompilationUnit(List<JCAnnotation> packageAnnotations,
                        JCExpression pid,
                        List<JCTree> defs,
                        JavaFileObject sourcefile,
                        PackageSymbol packge,
                        Scope namedImportScope,
                        Scope starImportScope) {
            this.packageAnnotations = packageAnnotations;
            this.pid = pid;
            this.defs = defs;
            this.sourcefile = sourcefile;
            this.packge = packge;
            this.namedImportScope = namedImportScope;
            this.starImportScope = starImportScope;
        }
        @Override
        public void accept(Visitor v) { v.visitTopLevel(this); }

        public Kind getKind() { return Kind.COMPILATION_UNIT; }
        public List<JCAnnotation> getPackageAnnotations() {
            return packageAnnotations;
        }
        public List<JCImport> getImports() {
            ListBuffer<JCImport> imports = new ListBuffer<JCImport>();
            for (JCTree tree : defs) {
                if (tree.getTag() == IMPORT)
                    imports.append((JCImport)tree);
                else
                    break;
            }
            return imports.toList();
        }
        public JCExpression getPackageName() { return pid; }
        public JavaFileObject getSourceFile() {
            return sourcefile;
        }
        public Position.LineMap getLineMap() {
            return lineMap;
        }
        public List<JCTree> getTypeDecls() {
            List<JCTree> typeDefs;
            for (typeDefs = defs; !typeDefs.isEmpty(); typeDefs = typeDefs.tail)
                if (typeDefs.head.getTag() != IMPORT)
                    break;
            return typeDefs;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitCompilationUnit(this, d);
        }

        @Override
        public int getTag() {
            return TOPLEVEL;
        }
    }

    /**
     * An import clause.
     * @param qualid    The imported class(es).
     */
    public static class JCImport extends JCTree implements ImportTree {
        public boolean staticImport;
        public JCTree qualid;
        protected JCImport(JCTree qualid, boolean importStatic) {
            this.qualid = qualid;
            this.staticImport = importStatic;
        }
        @Override
        public void accept(Visitor v) { v.visitImport(this); }

        public boolean isStatic() { return staticImport; }
        public JCTree getQualifiedIdentifier() { return qualid; }

        public Kind getKind() { return Kind.IMPORT; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitImport(this, d);
        }

        @Override
        public int getTag() {
            return IMPORT;
        }
    }



    public static abstract class JCExpression extends JCTree implements ExpressionTree {
        boolean return_expression = false;
		@Override
        public JCExpression setType(Type type) {
            super.setType(type);
            return this;
        }
        @Override
        public JCExpression setPos(int pos) {
            super.setPos(pos);
            return this;
        }
    }

    public static abstract class JCStatement extends JCExpression implements StatementTree {
        /*
        @Override
        public JCStatement setType(Type type) {
            super.setType(type);
            return this;
        }
        @Override
        public JCStatement setPos(int pos) {
            super.setPos(pos);
            return this;
        }
        */
        public float EstimatedWork=0;
		public float EstimatedMem=0;

		public String getTaskID()
		{
			return ""+pos;
		}

		public Set<JCTree> getBlockStats()
		{
			Set<JCTree> res=new LinkedHashSet<JCTree>();
			if(getTag()==JCTree.BLOCK)
				res.addAll(((JCBlock)this).stats);
			else
				res.add(this);
			return res;
		}
    }

    public static abstract class JCSymbolExpression extends JCExpression {
        public abstract Symbol getSymbol();
        public abstract void setSymbol(Symbol s);
    }

    /**
     * A class definition.
     * @param modifiers the modifiers
     * @param name the name of the class
     * @param typarams formal class parameters
     * @param extending the classes this class extends
     * @param implementing the interfaces implemented by this class
     * @param defs all variables and methods defined in this class
     * @param sym the symbol
     */
    public static class JCClassDecl extends JCStatement implements ClassTree {
        public JCModifiers mods;
        public Name name;
        public List<JCTypeParameter> typarams;
        public JCTree extending;
        public List<JCExpression> implementing;
        public List<JCTree> defs;
        public ClassSymbol sym;
        public boolean singular;

		//need to store uids for triggers in context
		public Set<JCMethodInvocation> staticTrigger = null;
		public Set<JCMethodInvocation> nonStaticTrigger = null;

        protected JCClassDecl(JCModifiers mods,
                           Name name,
                           List<JCTypeParameter> typarams,
                           JCTree extending,
                           List<JCExpression> implementing,
                           List<JCTree> defs,
                           ClassSymbol sym)
        {
            this.mods = mods;
            this.name = name;
            this.typarams = typarams;
            this.extending = extending;
            this.implementing = implementing;
            this.defs = defs;
            this.sym = sym;
            singular = false;
        }

        protected JCClassDecl(JCModifiers mods,
                           Name name,
                           List<JCTypeParameter> typarams,
                           JCTree extending,
                           List<JCExpression> implementing,
                           List<JCTree> defs,
                           ClassSymbol sym,boolean singular)
        {
            this.mods = mods;
            this.name = name;
            this.typarams = typarams;
            this.extending = extending;
            this.implementing = implementing;
            this.defs = defs;
            this.sym = sym;
            this.singular = singular;
        }
        @Override
        public void accept(Visitor v) { v.visitClassDef(this); }

        public boolean isSingular() { return singular; }
        public Kind getKind() { return Kind.CLASS; }
        public JCModifiers getModifiers() { return mods; }
        public Name getSimpleName() { return name; }
        public List<JCTypeParameter> getTypeParameters() {
            return typarams;
        }
        public JCTree getExtendsClause() { return extending; }
        public List<JCExpression> getImplementsClause() {
            return implementing;
        }
        public List<JCTree> getMembers() {
            return defs;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitClass(this, d);
        }

        @Override
        public int getTag() {
            return CLASSDEF;
        }
    }

    public static class JCArgExpression extends JCExpression implements ArgExpressionTree {

        public JCExpression exp1,exp2;

        protected JCArgExpression(JCExpression exp1,JCExpression exp2)
        {
            this.exp1 = exp1;
            this.exp2 = exp2;
        }
        @Override
        public void accept(Visitor v) { v.visitArgExpression(this); }

        public Kind getKind() { return Kind.ARGEXPRESSION; }
        public JCExpression getExpression1(){return exp1;}
        public JCExpression getExpression2(){return exp2;}
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitArgExpression(this, d);
        }

        @Override
        public int getTag() {
            return ARGEXPRESSION;
        }
    }

	public static String fixName(String input)
	{
		return input.replace(' ', '_').replace('[', '_').replace(']', '_').replace('<', '_').replace('>', '_').replace('=', '_')
						.replace(',', '_').replace('(', '_').replace(')', '_').replace('.', '_').replace('{', '_').replace('}', '_').replace('&', '_')
				.replace('-', '0').replace('+', '1').replace('*', '2').replace('/', '3').replace('%', '4').replace('~', '5').replace('!', '6')
				;

	}

    /**
     * A method definition.
     * @param modifiers method modifiers
     * @param name method name
     * @param restype type of method return value
     * @param typarams type parameters
     * @param params value parameters
     * @param thrown exceptions thrown by this method
     * @param stats statements in the method
     * @param sym method symbol
     */

    public static class JCMethodDecl extends JCTree implements MethodTree {
        public JCModifiers mods;
        public Name name;
        public Map<iTask, Set<VarSymbol>> reuseableVars;
        public Map<iTask, JCExpression> paramExpressions;
        public Map<iTask, Integer> taskDepForParamIndex;
        public JCExpression restype;
        public List<JCTypeParameter> typarams;
        public List<JCVariableDecl> params;
        public List<JCExpression> thrown;
        public JCBlock body;
        public JCExpression defaultValue; // for annotation types
        public MethodSymbol sym;
		public Set<VarSymbol> local_vars;
		public JCStatement super_call;
		public ListBuffer<JCAssign> init_constructors;

		public ListBuffer<JCPragma> pragmas;
		public Set<Symbol> forceSyms;//,parallelSyms;
		public Set<Symbol> implicitSyms=new LinkedHashSet<Symbol>();
		public Map<VarSymbol,Set<Pair<VarSymbol,Pair<Integer,JCExpression>>>> constraintsSyms=new LinkedHashMap<VarSymbol,Set<Pair<VarSymbol,Pair<Integer,JCExpression>>>>();
		public Map<VarSymbol,Pair<VarSymbol,Pair<Integer,JCExpression>>> match=new LinkedHashMap<VarSymbol, Pair<VarSymbol, Pair<Integer, JCExpression>>>();
		public Map<VarSymbol,Set<JCExpression>> constraintsDeps=new LinkedHashMap<VarSymbol,Set<JCExpression>>();
		public Map<VarSymbol,Set<JCExpression>> unconstraintsDeps=new LinkedHashMap<VarSymbol,Set<JCExpression>>();

		//public Set<MethodSymbol> called_methods;
		public JCWhere atomic_where = null;

		public Set<VarSymbol> uses_field;
		public Map<JCTree,Set<JCTree>> readlinear=new LinkedHashMap<JCTree,Set<JCTree>>();
		public Map<JCTree,VarSymbol> readlinearsym=new LinkedHashMap<JCTree,VarSymbol>();
		public Set<JCTree> exits = new LinkedHashSet<JCTree>();

		public long return_flags=0;
		public JCReturn final_value=null;
        //
        //dependency graph
        public JCSkip dg_end;

        public DirectedWeightedMultigraph<JCTree, Arc> depGraph;
        public DirectedWeightedMultigraph<JCTree, Arc> depGraphImplicit;
        public Map<JCTree,JCTree> cloneAssociationMap;
		public SimpleDirectedGraph<TaskSet, DefaultEdge> hasseDiagram;
                public SimpleDirectedGraph<TaskSet, DefaultEdge> hasseFinal;
		public Map<iTask,Float> eft,est;
		public float pUUET=0.f;
		public float pUET=0.f;
		public float pPET=0.f;

		public Hashtable<VarSymbol,Set<JCTree>> generated;

		public Map<Pair<PathSet,PathSet>,Pair<Float,Float>> parallelWork;

        public java.util.ArrayList<JCTree> parallelPaths; //list of nodes that have multiple (internally unreachable) incident paths
        public Set<iTask> parallelTasks; //list of nodes that have multiple (internally unreachable) incident paths
        //public FloydWarshallShortestPaths<JCTree,Arc> reachability;
        public java.util.Map<JCTree,Integer> topolNodes;
        public java.util.Map<JCTree,Integer> orderNodes=new LinkedHashMap<JCTree, Integer>();
        //public int LocalTGWidth;
		public boolean context_ref_count;
		public boolean hasResume;
		public Set<iTask> dangling_paths;
		public Set<iTask> spawned_dangling_paths;
		public int maxRecSpawn=0;

		public Map<Integer,JCMethodDecl> restricted_impls=null;


		//get all constraints (transitively) connected to vs
		public Set<JCExpression> getConstraintsTransitive(Set<VarSymbol> vs,Set<VarSymbol> usedvars)
		{
			Set<JCExpression> res=new LinkedHashSet<JCExpression>();
			Set<VarSymbol> newvars=new LinkedHashSet<VarSymbol>();

			newvars.addAll(vs);

			while(!newvars.isEmpty())
			{
				Set<JCExpression> add=new LinkedHashSet<JCExpression>();
				for(VarSymbol nv:newvars)
				{
					Set<JCExpression> csts=constraintsDeps.get(nv);
					if(csts!=null)
					{
						add.addAll(csts);
						res.addAll(add);
					}
					usedvars.add(nv);
				}

				newvars.clear();
				for(JCExpression e:add)
					newvars.addAll(usedVars(e));
				newvars.removeAll(usedvars);
			}

			return res;
		}

		public boolean emit()
		{
			return (mods.flags&Flags.FINAL)!=0||analyse();
		}

		public boolean analyse()
		{
			if(name == name.table.names.init&&body.stats.size()==0&&init_constructors==null&&(sym!=null&&sym.enclClass().init_constructors==null))
			{
				return false; //avoid emitting empty constructors
			}

			return (body!=null&&typarams.size()==0||(sym!=null&&sym.type!=null&&(sym.type.type_flags_field&Flags.IMPLICITDECL)!=0));
		}

		public int hasSchedulerPaths(Set<iTask> paths) {
			int count = 0;
			Set<JCTree> set = new LinkedHashSet<JCTree>();
			for (Iterator<iTask> i = paths.iterator(); i.hasNext();) {
				iTask p = i.next();
				Set<JCTree> calcNodes = p.getCalcNodes(); //FIXME: cache these?
				JCTree fcn = p.getFirstCalcNode(this, calcNodes);
				//Integer in=in_map.get(p);
				if (fcn.getTag() != JCTree.CF&&!p.isCFDEPTo(this)) {
					if (set.contains(fcn.scheduler)) {
						count++;
					} else {
						set.add(fcn.scheduler);
					}

				}
				if (p.containsForcedSpawn()) {
					count++;
				}
			}
			return count;

		}

		public Set<Arc> getDanglingNodes()
		{
			Set<Arc> result=new LinkedHashSet<Arc>();
			Set<Arc> inedges=depGraph.incomingEdgesOf(dg_end);
			for(Arc a:inedges)
			{
				if(a.v==null)
					result.add(a);
			}
			return result;
		}

		public Set<Arc> getJoiningNodes()
		{
			Set<Arc> result=new LinkedHashSet<Arc>();
			Set<Arc> inedges=depGraph.incomingEdgesOf(dg_end);
			for(Arc a:inedges)
			{
				if(a.v!=null)
					result.add(a);
			}
			return result;
		}

		public String getID()
		{
			String s;
			if(name != name.table.names.init)
				s=this.name.toString();
			else
				s=this.sym.owner.toString();

			for(JCVariableDecl vd:this.params)
			{
				s+="_"+vd.sym.type.toString();
			}

			return fixName(s);
		}

		public void resetParallelPaths()
		{
			parallelPaths=null;
		}

		public void resetParallelTasks()
		{
			parallelTasks=null;
		}

        public java.util.List<JCTree> getParallelPaths()
        {
            if(parallelPaths==null)
                parallelPaths=new ArrayList<JCTree>();

            return parallelPaths;
        }


		public void sortParallelPaths(java.util.Map<JCTree,Integer> topolNodes)
		{
			if(parallelPaths!=null)
				Collections.sort(parallelPaths, new TopolComparator(topolNodes));

		}

		public Set<iTask> getHasseSchedules()
		{
            if(parallelTasks==null)
                parallelTasks=new LinkedHashSet<iTask>();

            return parallelTasks;

		}

		public Set<iTask> getAllSchedules()
		{
			Set<iTask> all=new LinkedHashSet<iTask>();
			for(Iterator<JCTree> i=getParallelPaths().iterator();i.hasNext();)
			{
				JCTree v=i.next();

				all.addAll(v.getPathSets());
			}
			if(all.isEmpty())
				return getHasseSchedules();
			return all;
		}

        protected JCMethodDecl(JCModifiers mods,
                            Name name,
                            JCExpression restype,
                            List<JCTypeParameter> typarams,
                            List<JCVariableDecl> params,
                            List<JCExpression> thrown,
                            JCBlock body,
                            JCExpression defaultValue,
                            JCSkip skip,
                            MethodSymbol sym)
        {
            this.mods = mods;
            this.name = name;
            this.restype = restype;
            this.typarams = typarams;
            this.params = params;
            this.thrown = thrown;
            this.body = body;
            this.defaultValue = defaultValue;
            this.sym = sym;
            this.dg_end = skip;
            local_vars=null;
            super_call=null;
            init_constructors=null;
            hasResume=false;
            pragmas=new ListBuffer<JCPragma>();
            forceSyms=new LinkedHashSet<Symbol>();
            //parallelSyms=new LinkedHashSet<Symbol>();
            uses_field=new LinkedHashSet<VarSymbol>();

            dangling_paths = null;
            eft=null;
            reuseableVars = new LinkedHashMap<iTask, Set<VarSymbol>>();
            paramExpressions = new LinkedHashMap<iTask, JCExpression>();
            taskDepForParamIndex = new LinkedHashMap<iTask, Integer>();
        }

        @Override
        public void accept(Visitor v) { v.visitMethodDef(this); }

        class SymbolReplace extends TreeScanner {
            Symbol find,replace;
            public void scan(JCTree tree) {
                if (tree!=null)
                    tree.accept(this);
            }
            public void visitIdent(JCIdent that) {
				if(that.sym==find)
					that.sym=replace;
                super.visitIdent(that);
            }
            public void visitSelect(JCFieldAccess that) {
				if(that.sym==find)
					that.sym=replace;
                super.visitSelect(that);
            }
/*
			public void visitApply(JCMethodInvocation that)
			{
                super.visitApply(that);
			}
*/
			public SymbolReplace(Symbol find, Symbol replace)
			{
				this.find=find;
				this.replace=replace;
			}
        }

		public JCMethodDecl cloneForPathGen(Name name,TreeCopier copy)
		{
			JCMethodDecl md=(JCMethodDecl)super.clone();
			md.name = name;
			md.sym=(MethodSymbol)this.sym.clone();
			md.sym.name=name;
			md.depGraph = new DirectedWeightedMultigraph<JCTree, Arc>(Arc.class);
			md.implicitSyms=this.implicitSyms;
			md.resetParallelPaths();
			md.resetParallelTasks();

			md.eft=null;
			md.est=null;

			SymbolReplace sr=new SymbolReplace(this.sym,md.sym);

			Map<JCTree,JCTree> map=new LinkedHashMap<JCTree,JCTree>();
			for(JCTree v:this.depGraph.vertexSet())
			{

				JCTree node;
				if(v==this)
					node=md;
				else
				{

					node=copy.copy(v);

					node.groups=v.groups;
					node.is_blocking=v.is_blocking;
					node.local_returns=v.local_returns;
					node.nop=v.nop;
					node.nop_if_alone=v.nop_if_alone;
					node.threads=v.threads;
					node.time=v.time;
					node.transitive_returns=v.transitive_returns;

					if(v instanceof JCStatement)
					{
						((JCStatement)node).EstimatedMem=((JCStatement)v).EstimatedMem;
						((JCStatement)node).EstimatedWork=((JCStatement)v).EstimatedWork;
					}

					//node=(JCTree)v.clone();
					sr.scan(node);
				}

				if(node.getTag()==JCTree.SKIP)
				{
					if(node.scheduler.getTag()!=JCTree.CF)
						md.dg_end = (JCSkip)node;
				}

				node.resetPathSets();
				node.resetSchedule();
				node.resetDGNode();
				node.getDGNode().self=node;

				map.put(v,node);
				md.depGraph.addVertex(node);
			}

			for(JCTree v:this.depGraph.vertexSet())
			{
				if(v.scheduler!=null)
					map.get(v).scheduler=map.get(v.scheduler);
				if(map.get(v)!=null)
					md.orderNodes.put(map.get(v),orderNodes.get(v));
			}

			if(md.final_value!=null)
				md.final_value=(JCReturn)map.get(md.final_value);

			if(md.atomic_where!=null)
				md.atomic_where=(JCWhere)map.get(md.atomic_where);

			for(Arc a:this.depGraph.edgeSet())
			{
				md.depGraph.addEdge(map.get(a.s),map.get(a.t),new Arc(map.get(a.s), map.get(a.t),a.v));
			}
                        md.cloneAssociationMap = map;
			return md;
		}

        public String getStringHeader() {
            StringWriter s = new StringWriter();
            new Pretty(s, false, false).visitMethodHeader(this);
            return s.toString();
        }


        public Kind getKind() { return Kind.METHOD; }
        public JCModifiers getModifiers() { return mods; }
        public Name getName() { return name; }
        public JCTree getReturnType() { return restype; }
        public List<JCTypeParameter> getTypeParameters() {
            return typarams;
        }
        public List<JCVariableDecl> getParameters() {
            return params;
        }
        public List<JCExpression> getThrows() {
            return thrown;
        }
        public JCBlock getBody() { return body; }
        public JCTree getDefaultValue() { // for annotation types
            return defaultValue;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitMethod(this, d);
        }

        @Override
        public int getTag() {
            return METHODDEF;
        }
  }

    /**
     * A variable definition.
     * @param modifiers variable modifiers
     * @param name variable name
     * @param vartype type of the variable
     * @param init variables initial value
     * @param sym symbol
     */
    public static class JCVariableDecl extends JCStatement implements VariableTree {
        public JCModifiers mods;
        public Name name;
        public JCExpression vartype;
        public JCExpression init;
        public VarSymbol sym;
		public com.sun.tools.javac.util.List<JCVariableDecl> list;

		protected JCVariableDecl(JCModifiers mods,
                         Name name,
                         JCExpression vartype,
                         JCExpression init,
                         VarSymbol sym) {

            this.mods = mods;
            this.name = name;
            this.vartype = vartype;
            this.init = init;
            this.sym = sym;
			this.list = null;
        }
		protected JCVariableDecl(JCModifiers mods,
                         Name name,
                         JCExpression vartype,
                         JCExpression init,
                         VarSymbol sym,com.sun.tools.javac.util.List<JCVariableDecl> list) {

            this.mods = mods;
            this.name = name;
            this.vartype = vartype;
            this.init = init;
            this.sym = sym;
			this.list = list;
        }
        @Override
        public void accept(Visitor v) { v.visitVarDef(this); }

        public Kind getKind() { return Kind.VARIABLE; }
        public JCModifiers getModifiers() { return mods; }
        public Name getName() { return name; }
        public JCTree getType() { return vartype; }
        public JCExpression getInitializer() {
            return init;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitVariable(this, d);
        }

        @Override
        public int getTag() {
            return VARDEF;
        }
    }

      /**
     * A no-op statement ";".
     */
    public static class JCSkip extends JCStatement implements EmptyStatementTree {
        protected JCSkip() {
        }
        @Override
        public void accept(Visitor v) { v.visitSkip(this); }

        public Kind getKind() { return Kind.EMPTY_STATEMENT; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitEmptyStatement(this, d);
        }

        @Override
        public int getTag() {
            return SKIP;
        }
    }

      /**
     * A no-op statement ";".
     */
    public static class JCCF extends JCSkip implements EmptyStatementTree {
        public JCTree condition;
        public boolean value;
		public JCTree exit=null;
		public Map<TaskSet,Integer> additionalRefs;


        protected JCCF(JCTree condition,boolean value) {
            this.condition=condition;
            this.value=value;
			additionalRefs=new LinkedHashMap<TaskSet, Integer>();
//			isScheduler = true;
        }
        @Override
        public void accept(Visitor v) { v.visitCF(this); }

        public Kind getKind() { return Kind.EMPTY_STATEMENT; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitCF(this, d);
        }

        @Override
        public int getTag() {
            return CF;
        }
    }

      /**
     * A no-op statement ";".
     */
    public static class JCSet extends JCExpression implements SetTree {
        public ListBuffer<JCExpression> content;
        public JCSet() {
            content= new ListBuffer<JCExpression>();
        }
        @Override
        public void accept(Visitor v) { v.visitSet(this); }

        public List<JCExpression> getContent() {return content.toList(); }
        public Kind getKind() { return Kind.SET; }

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitSet(this, d);
        }

        @Override
        public int getTag() {
            return SET;
        }
    }

    /**
     * A statement block.
     * @param stats statements
     * @param flags flags
     */
    public static class JCBlock extends JCStatement implements BlockTree {
        public long flags;
        public List<JCStatement> stats;
		public JCSkip block_exit = null;
        /** Position of closing brace, optional. */
        public int endpos = Position.NOPOS;
        public JCBlock(long flags, List<JCStatement> stats) {
            this.stats = stats;
            this.flags = flags;
        }
        @Override
        public void accept(Visitor v) { v.visitBlock(this); }

        public Kind getKind() { return Kind.BLOCK; }
        public List<JCStatement> getStatements() {
            return stats;
        }
        public boolean isStatic() { return (flags & Flags.STATIC) != 0; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitBlock(this, d);
        }

        @Override
        public int getTag() {
            return BLOCK;
        }
    }

    /**
     * A do loop
     */
    public static class JCDoWhileLoop extends JCStatement implements DoWhileLoopTree {
        public JCStatement body;
        public JCExpression cond;
        protected JCDoWhileLoop(JCStatement body, JCExpression cond) {
            this.body = body;
            this.cond = cond;
        }
        @Override
        public void accept(Visitor v) { v.visitDoLoop(this); }

        public Kind getKind() { return Kind.DO_WHILE_LOOP; }
        public JCExpression getCondition() { return cond; }
        public JCStatement getStatement() { return body; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitDoWhileLoop(this, d);
        }

        @Override
        public int getTag() {
            return DOLOOP;
        }
    }

    /**
     * A while loop
     */
    public static class JCWhileLoop extends JCStatement implements WhileLoopTree {
        public JCExpression cond;
        public JCStatement body;
        protected JCWhileLoop(JCExpression cond, JCStatement body) {
            this.cond = cond;
            this.body = body;
        }
        @Override
        public void accept(Visitor v) { v.visitWhileLoop(this); }

        public Kind getKind() { return Kind.WHILE_LOOP; }
        public JCExpression getCondition() { return cond; }
        public JCStatement getStatement() { return body; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitWhileLoop(this, d);
        }

        @Override
        public int getTag() {
            return WHILELOOP;
        }
    }

    /**
     * A for loop.
     */
    public static class JCForLoop extends JCStatement implements ForLoopTree {
        public List<JCStatement> init;
        public JCExpression cond;
        public List<JCExpressionStatement> step;
        public JCStatement body;
        protected JCForLoop(List<JCStatement> init,
                          JCExpression cond,
                          List<JCExpressionStatement> update,
                          JCStatement body)
        {
            this.init = init;
            this.cond = cond;
            this.step = update;
            this.body = body;
        }
        @Override
        public void accept(Visitor v) { v.visitForLoop(this); }

        public Kind getKind() { return Kind.FOR_LOOP; }
        public JCExpression getCondition() { return cond; }
        public JCStatement getStatement() { return body; }
        public List<JCStatement> getInitializer() {
            return init;
        }
        public List<JCExpressionStatement> getUpdate() {
            return step;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitForLoop(this, d);
        }

        @Override
        public int getTag() {
            return FORLOOP;
        }
    }

    /**
     * The enhanced for loop.
     */
    public static class JCEnhancedForLoop extends JCStatement implements EnhancedForLoopTree {
        public JCVariableDecl var;
        public JCExpression expr;
        public JCStatement body;
        protected JCEnhancedForLoop(JCVariableDecl var, JCExpression expr, JCStatement body) {
            this.var = var;
            this.expr = expr;
            this.body = body;
        }
        @Override
        public void accept(Visitor v) { v.visitForeachLoop(this); }

        public Kind getKind() { return Kind.ENHANCED_FOR_LOOP; }
        public JCVariableDecl getVariable() { return var; }
        public JCExpression getExpression() { return expr; }
        public JCStatement getStatement() { return body; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitEnhancedForLoop(this, d);
        }
        @Override
        public int getTag() {
            return FOREACHLOOP;
        }
    }

    /**
     * A labelled expression or statement.
     */
    public static class JCLabeledStatement extends JCStatement implements LabeledStatementTree {
        public Name label;
        public JCStatement body;
        protected JCLabeledStatement(Name label, JCStatement body) {
            this.label = label;
            this.body = body;
        }
        @Override
        public void accept(Visitor v) { v.visitLabelled(this); }
        public Kind getKind() { return Kind.LABELED_STATEMENT; }
        public Name getLabel() { return label; }
        public JCStatement getStatement() { return body; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitLabeledStatement(this, d);
        }
        @Override
        public int getTag() {
            return LABELLED;
        }
    }

    /**
     * A "switch ( ) { }" construction.
     */
    public static class JCSwitch extends JCStatement implements SwitchTree {
        public JCExpression selector;
        public List<JCCase> cases;
        protected JCSwitch(JCExpression selector, List<JCCase> cases) {
            this.selector = selector;
            this.cases = cases;
        }
        @Override
        public void accept(Visitor v) { v.visitSwitch(this); }

        public Kind getKind() { return Kind.SWITCH; }
        public JCExpression getExpression() { return selector; }
        public List<JCCase> getCases() { return cases; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitSwitch(this, d);
        }
        @Override
        public int getTag() {
            return SWITCH;
        }
    }

    /**
     * A "case  :" of a switch.
     */
    public static class JCCase extends JCStatement implements CaseTree {
        public JCExpression pat;
        public List<JCStatement> stats;
        protected JCCase(JCExpression pat, List<JCStatement> stats) {
            this.pat = pat;
            this.stats = stats;
        }
        @Override
        public void accept(Visitor v) { v.visitCase(this); }

        public Kind getKind() { return Kind.CASE; }
        public JCExpression getExpression() { return pat; }
        public List<JCStatement> getStatements() { return stats; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitCase(this, d);
        }
        @Override
        public int getTag() {
            return CASE;
        }
    }

    /**
     * A synchronized block.
     */
    public static class JCSynchronized extends JCStatement implements SynchronizedTree {
        public JCExpression lock;
        public JCBlock body;
        protected JCSynchronized(JCExpression lock, JCBlock body) {
            this.lock = lock;
            this.body = body;
        }
        @Override
        public void accept(Visitor v) { v.visitSynchronized(this); }

        public Kind getKind() { return Kind.SYNCHRONIZED; }
        public JCExpression getExpression() { return lock; }
        public JCBlock getBlock() { return body; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitSynchronized(this, d);
        }
        @Override
        public int getTag() {
            return SYNCHRONIZED;
        }
    }

    /**
     * A "try { } catch ( ) { } finally { }" block.
     */
    public static class JCTry extends JCStatement implements TryTree {
        public JCBlock body;
        public List<JCCatch> catchers;
        public JCBlock finalizer;
        protected JCTry(JCBlock body, List<JCCatch> catchers, JCBlock finalizer) {
            this.body = body;
            this.catchers = catchers;
            this.finalizer = finalizer;
        }
        @Override
        public void accept(Visitor v) { v.visitTry(this); }

        public Kind getKind() { return Kind.TRY; }
        public JCBlock getBlock() { return body; }
        public List<JCCatch> getCatches() {
            return catchers;
        }
        public JCBlock getFinallyBlock() { return finalizer; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitTry(this, d);
        }
        @Override
        public int getTag() {
            return TRY;
        }
    }

    /**
     * A catch block.
     */
    public static class JCCatch extends JCTree implements CatchTree {
        public JCVariableDecl param;
        public JCBlock body;
        protected JCCatch(JCVariableDecl param, JCBlock body) {
            this.param = param;
            this.body = body;
        }
        @Override
        public void accept(Visitor v) { v.visitCatch(this); }

        public Kind getKind() { return Kind.CATCH; }
        public JCVariableDecl getParameter() { return param; }
        public JCBlock getBlock() { return body; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitCatch(this, d);
        }
        @Override
        public int getTag() {
            return CATCH;
        }
    }

    /**
     * A ( ) ? ( ) : ( ) conditional expression
     */
    public static class JCConditional extends JCExpression implements ConditionalExpressionTree {
        public JCExpression cond;
        public JCExpression truepart;
        public JCExpression falsepart;
        protected JCConditional(JCExpression cond,
                              JCExpression truepart,
                              JCExpression falsepart)
        {
            this.cond = cond;
            this.truepart = truepart;
            this.falsepart = falsepart;
        }
        @Override
        public void accept(Visitor v) { v.visitConditional(this); }

        public Kind getKind() { return Kind.CONDITIONAL_EXPRESSION; }
        public JCExpression getCondition() { return cond; }
        public JCExpression getTrueExpression() { return truepart; }
        public JCExpression getFalseExpression() { return falsepart; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitConditionalExpression(this, d);
        }
        @Override
        public int getTag() {
            return CONDEXPR;
        }
    }

    /**
     * An "if ( ) { } else { }" block
     */
    public static class JCIf extends JCStatement implements IfTree {
        public JCExpression cond;
        public JCStatement thenpart;
        public JCStatement elsepart;

		//public JCCF cf_true=null;
		//public JCCF cf_false=null;
        protected JCIf(JCExpression cond,
                     JCStatement thenpart,
                     JCStatement elsepart)
        {
            this.cond = cond;
            this.thenpart = thenpart;
            this.elsepart = elsepart;
			isScheduler = true;
        }
        @Override
        public void accept(Visitor v) { v.visitIf(this); }

		public iTask getBranchTask(JCMethodDecl method,boolean val)
		{
			Set<iTask> paths = this.getSchedule();

			//filter the paths that are control flow dependent on this branch (DO NOT JUST USE THE TRUE/FALSE BRANCH STORED IN THE AST)
			for (iTask p : paths) {
				Set<JCTree> calc_nodes = p.getCalcNodes();
				if (!calc_nodes.isEmpty()) {
					JCTree fcn = calc_nodes.iterator().next();

					if (fcn.scheduler == this && fcn.getTag() == JCTree.CF) {
						if (val==((JCCF) fcn).value) {
							return p;
						}
					}
				}
			}

			return null;
		}

        public Kind getKind() { return Kind.IF; }
        public JCExpression getCondition() { return cond; }
        public JCStatement getThenStatement() { return thenpart; }
        public JCStatement getElseStatement() { return elsepart; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitIf(this, d);
        }
        @Override
        public int getTag() {
            return IF;
        }
    }

    public static class JCIfExp extends JCExpression implements IfExpTree {
        public JCExpression cond;
        public JCExpression thenpart;
        public JCExpression elsepart;
        protected JCIfExp(JCExpression cond,
                     JCExpression thenpart,
                     JCExpression elsepart)
        {
            this.cond = cond;
            this.thenpart = thenpart;
            this.elsepart = elsepart;
        }
        @Override
        public void accept(Visitor v) { v.visitIfExp(this); }

        public Kind getKind() { return Kind.IFEXP; }
        public JCExpression getCondition() { return cond; }
        public JCExpression getThenExp() { return thenpart; }
        public JCExpression getElseExp() { return elsepart; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitIfExp(this, d);
        }
        @Override
        public int getTag() {
            return IFEXP;
        }
    }

    /**
     * an expression statement
     * @param expr expression structure
     */
    public static class JCExpressionStatement extends JCStatement implements ExpressionStatementTree {
        public JCExpression expr;
        protected JCExpressionStatement(JCExpression expr)
        {
            this.expr = expr;
        }
        @Override
        public void accept(Visitor v) { v.visitExec(this); }

        public Kind getKind() { return Kind.EXPRESSION_STATEMENT; }
        public JCExpression getExpression() { return expr; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitExpressionStatement(this, d);
        }
        @Override
        public int getTag() {
            return EXEC;
        }
    }

    /**
     * A break from a loop or switch.
     */
    public static class JCBreak extends JCStatement implements BreakTree {
        public Name label;
        public JCTree target;
        protected JCBreak(Name label, JCTree target) {
            this.label = label;
            this.target = target;
        }
        @Override
        public void accept(Visitor v) { v.visitBreak(this); }

        public Kind getKind() { return Kind.BREAK; }
        public Name getLabel() { return label; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitBreak(this, d);
        }
        @Override
        public int getTag() {
            return BREAK;
        }
    }

    /**
     * A continue of a loop.
     */
    public static class JCContinue extends JCStatement implements ContinueTree {
        public Name label;
        public JCTree target;
        protected JCContinue(Name label, JCTree target) {
            this.label = label;
            this.target = target;
        }
        @Override
        public void accept(Visitor v) { v.visitContinue(this); }

        public Kind getKind() { return Kind.CONTINUE; }
        public Name getLabel() { return label; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitContinue(this, d);
        }
        @Override
        public int getTag() {
            return CONTINUE;
        }
    }

    /**
     * A return statement.
     */
    public static class JCReturn extends JCStatement implements ReturnTree {
        public JCExpression expr;
		public long flags;
        protected JCReturn(JCExpression expr,long flags) {
            this.expr = expr;
			this.flags = flags;
        }

		@Override
        public void accept(Visitor v) { v.visitReturn(this); }

        public Kind getKind() { return Kind.RETURN; }
        public JCExpression getExpression() { return expr; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitReturn(this, d);
        }
        @Override
        public int getTag() {
            return RETURN;
        }
    }

    /**
     * A throw statement.
     */
    public static class JCThrow extends JCStatement implements ThrowTree {
        public JCExpression expr;
        protected JCThrow(JCTree expr) {
            this.expr = (JCExpression)expr;
        }
        @Override
        public void accept(Visitor v) { v.visitThrow(this); }

        public Kind getKind() { return Kind.THROW; }
        public JCExpression getExpression() { return expr; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitThrow(this, d);
        }
        @Override
        public int getTag() {
            return THROW;
        }
    }

    /**
     * An assert statement.
     */
    public static class JCAssert extends JCStatement implements AssertTree {
        public JCExpression cond;
        public JCExpression detail;
        protected JCAssert(JCExpression cond, JCExpression detail) {
            this.cond = cond;
            this.detail = detail;
        }
        @Override
        public void accept(Visitor v) { v.visitAssert(this); }

        public Kind getKind() { return Kind.ASSERT; }
        public JCExpression getCondition() { return cond; }
        public JCExpression getDetail() { return detail; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitAssert(this, d);
        }
        @Override
        public int getTag() {
            return ASSERT;
        }
    }

   /**
     * A pragma statement.
     */
    public static class JCPragma extends JCStatement implements PragmaTree {
        public JCExpression cond;
        public JCExpression detail;

		public VarSymbol s1=null;
		public VarSymbol s2=null;

		public long flag;
        protected JCPragma(long flag,JCExpression cond, JCExpression detail) {
            this.cond = cond;
            this.detail = detail;
			this.flag = flag;
        }
        @Override
        public void accept(Visitor v) { v.visitPragma(this); }

        public Kind getKind() { return Kind.PRAGMA; }
        public JCExpression getCondition() { return cond; }
        public JCExpression getDetail() { return detail; }
		public long getType() {return flag;}
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitPragma(this, d);
        }
        @Override
        public int getTag() {
            return PRAGMA;
        }
    }

    /**
     * A method invocation
     */
    public static class JCMethodInvocation extends JCExpression implements MethodInvocationTree {
        public List<JCExpression> typeargs;
        public JCExpression meth,trigger;
        public DiagnosticPosition trigger_pos;
        public List<JCExpression> args;
        public Type varargsElement;

        protected JCMethodInvocation(List<JCExpression> typeargs,
                        JCExpression meth,
                        List<JCExpression> args)
        {
            this.typeargs = (typeargs == null) ? List.<JCExpression>nil()
                                               : typeargs;
            this.meth = meth;
            this.args = args;
            trigger = null;
            trigger_pos = null;
        }
        @Override
        public void accept(Visitor v) { v.visitApply(this); }

        public Kind getKind() { return Kind.METHOD_INVOCATION; }
        public List<JCExpression> getTypeArguments() {
            return typeargs;
        }

        public void setTriggerReturn(JCExpression e, DiagnosticPosition pos) { trigger=e; trigger_pos=pos; }
        public JCExpression getTriggerReturn() { return trigger; }
        public JCExpression getMethodSelect() { return meth; }
        public List<JCExpression> getArguments() {
            return args;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitMethodInvocation(this, d);
        }
        @Override
        public JCMethodInvocation setType(Type type) {
            super.setType(type);
            return this;
        }
        @Override
        public int getTag() {
            return(APPLY);
        }
    }

    /**
     * A new(...) operation.
     */
    public static class JCNewClass extends JCExpression implements NewClassTree {
        public JCExpression encl;
        public List<JCExpression> typeargs;
        public JCExpression clazz;
        public List<JCExpression> args;
        public JCClassDecl def;
        public Symbol constructor;
        public Type varargsElement;
        public Type constructorType;
        protected JCNewClass(JCExpression encl,
                           List<JCExpression> typeargs,
                           JCExpression clazz,
                           List<JCExpression> args,
                           JCClassDecl def)
        {
            this.encl = encl;
            this.typeargs = (typeargs == null) ? List.<JCExpression>nil()
                                               : typeargs;
            this.clazz = clazz;
            this.args = args;
            this.def = def;
        }
        @Override
        public void accept(Visitor v) { v.visitNewClass(this); }

        public Kind getKind() { return Kind.NEW_CLASS; }
        public JCExpression getEnclosingExpression() { // expr.new C< ... > ( ... )
            return encl;
        }
        public List<JCExpression> getTypeArguments() {
            return typeargs;
        }
        public JCExpression getIdentifier() { return clazz; }
        public List<JCExpression> getArguments() {
            return args;
        }
        public JCClassDecl getClassBody() { return def; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitNewClass(this, d);
        }
        @Override
        public int getTag() {
            return NEWCLASS;
        }
    }

    /**
     * A new[...] operation.
     */
    public static class JCNewArray extends JCExpression implements NewArrayTree {
        public JCExpression elemtype;
		public JCDomInstance dom;
        public List<JCExpression> dims;
        public List<JCExpression> elems;
        protected JCNewArray(JCExpression elemtype,
						   JCDomInstance dom,
                           List<JCExpression> dims,
                           List<JCExpression> elems)
        {
            this.elemtype = elemtype;
			this.dom = dom;
            this.dims = dims;
            this.elems = elems;
        }
        @Override
        public void accept(Visitor v) { v.visitNewArray(this); }

        public Kind getKind() { return Kind.NEW_ARRAY; }
        public JCExpression getType() { return elemtype; }
        public List<JCExpression> getDimensions() {
            return dims;
        }
        public List<JCExpression> getInitializers() {
            return elems;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitNewArray(this, d);
        }
        @Override
        public int getTag() {
            return NEWARRAY;
        }
    }

    /**
     * A parenthesized subexpression ( ... )
     */
    public static class JCParens extends JCExpression implements ParenthesizedTree {
        public JCExpression expr;
        protected JCParens(JCExpression expr) {
            this.expr = expr;
        }
        @Override
        public void accept(Visitor v) { v.visitParens(this); }

        public Kind getKind() { return Kind.PARENTHESIZED; }
        public JCExpression getExpression() { return expr; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitParenthesized(this, d);
        }
        @Override
        public int getTag() {
            return PARENS;
        }
    }

    /**
     * A assignment with "=".
     */
    public static class JCAssign extends JCExpression implements AssignmentTree {
        public JCExpression lhs;
        public JCExpression rhs;
		public long aflags;
		public JCExpression cond;
        protected JCAssign(JCExpression lhs, JCExpression rhs,long aflags,JCExpression cond) {
            this.lhs = lhs;
            this.rhs = rhs;
			this.aflags = aflags;
			this.cond = cond;
        }
        @Override
        public void accept(Visitor v) { v.visitAssign(this); }

        public Kind getKind() { return Kind.ASSIGNMENT; }
        public JCExpression getVariable() { return lhs; }
        public JCExpression getExpression() { return rhs; }
		public ExpressionTree getCond(){ return cond; };
		public long getAFlags(){ return aflags; };
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitAssignment(this, d);
        }
        @Override
        public int getTag() {
            return ASSIGN;
        }
    }

	public static class AtomicTarget
	{
		public enum AtomicType
		{
			PREINC,
			POSTINC,
			INC,
			CMPXCHG,
			UNKNOWN
		}

		public VarSymbol target = null;
		public AtomicType type = AtomicType.UNKNOWN;

		public JCExpression temp = null;//temp store for PRE/POSTINC, value expression for CMPXCHG
		public JCExpression comp = null;//compare expression for CMPXCHG
		public long aflag=0;
	}
    /**
     * A expression with "a where b".
     */
    public static class JCWhere extends JCExpression implements WhereTree {
        public JCExpression exp,sexp;
        public JCStatement body;

		public AtomicTarget atomic = null;

		public Set<JCAssign> writes = new LinkedHashSet<JCAssign>();
		public Map<JCAssign,Set<VarSymbol>> reads=new LinkedHashMap<JCAssign,Set<VarSymbol>>();
        public JCTree dg_end;

		//public JCSkip exit=null;

        protected JCWhere(JCExpression exp, JCStatement body,JCExpression sexp) {
            this.exp = exp;
            this.body = body;
            this.sexp = sexp;
//			isScheduler = true;
        }

        @Override
        public void accept(Visitor v) { v.visitWhere(this); }

        public Kind getKind() { return Kind.WHERE; }
        public JCExpression getExpression() { return exp; }
        public JCStatement getStatement() { return body; }
        public JCExpression getSExpression() { return sexp; }

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitWhere(this, d);
        }
        @Override
        public int getTag() {
            return WHERE;
        }
    }


    /**
     * static for (ident, exp) { content }
     */
    public static class JCFor extends JCStatement implements ForTree {
        public Name name;
        public JCExpression exp;
        public List<JCTree> content;

        protected JCFor(Name name,JCExpression exp, List<JCTree> content) {
            this.exp = exp;
            this.name = name;
            this.content = content;
        }
        @Override
        public void accept(Visitor v) { v.visitFor(this); }

        public Kind getKind() { return Kind.FOR; }
        public JCExpression getExpression() { return exp; }
        public Name getIdent() { return name; }
        public List<? extends JCTree> getContent() { return content; }

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitFor(this, d);
        }
        @Override
        public int getTag() {
            return FOR;
        }
    }

   /**
     * A expression with "a where b".
     */
    public static class JCSelectCond extends JCExpression implements SelectCondTree {
        public JCExpression cond,res;
        public JCStatement stmnt;

        protected JCSelectCond(JCExpression cond, JCExpression res, JCStatement stmnt) {
            this.cond = cond;
            this.res = res;
            this.stmnt = stmnt;
        }
        @Override
        public void accept(Visitor v) { v.visitSelectCond(this); }

        public Kind getKind() { return Kind.SELECTCOND; }
        public JCExpression getCond() { return cond; }
        public JCExpression getRes() { return res; }
        public JCStatement getStmnt() { return stmnt; }

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitSelectCond(this, d);
        }
        @Override
        public int getTag() {
            return SELECTCOND;
        }
    }

    /**
     * A expression with select { cond -> res | cond2 -> res 2 | ...}.
     */
    public static class JCSelect extends JCExpression implements SelectTree {
        public List<JCSelectCond> list;
        protected JCSelect(List<JCSelectCond> list) {
            this.list = list;
        }
        @Override
        public void accept(Visitor v) { v.visitSelectExp(this); }

        public Kind getKind() { return Kind.SELECT; }

        public List<JCSelectCond> getCondList() { return list; };

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitSelectExp(this, d);
        }
        @Override
        public int getTag() {
            return SELECTEXP;
        }
    }


    /**
     * A expression with select { cond -> res | cond2 -> res 2 | ...}.
     */
    public static class JCCaseExp extends JCExpression implements CaseExpTree {
        public List<JCSelectCond> list;
        public JCExpression exp;
        protected JCCaseExp(JCExpression exp,List<JCSelectCond> list) {
            this.list = list;
            this.exp = exp;
        }
        @Override
        public void accept(Visitor v) { v.visitCaseExp(this); }

        public Kind getKind() { return Kind.CASEEEXP; }

        public List<JCSelectCond> getCondList() { return list; }
        public JCExpression getExp( ) { return exp; }


        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitCaseExp(this, d);
        }
        @Override
        public int getTag() {
            return CASEEXP;
        }
    }


    /**
     * An assignment with "+=", "|=" ...
     */
    public static class JCAssignOp extends JCExpression implements CompoundAssignmentTree {
        private int opcode;
        public JCExpression lhs;
        public JCExpression rhs;
        public Symbol operator;
        protected JCAssignOp(int opcode, JCTree lhs, JCTree rhs, Symbol operator) {
            this.opcode = opcode;
            this.lhs = (JCExpression)lhs;
            this.rhs = (JCExpression)rhs;
            this.operator = operator;
        }
        @Override
        public void accept(Visitor v) { v.visitAssignop(this); }

        public Kind getKind() { return TreeInfo.tagToKind(getTag()); }
        public JCExpression getVariable() { return lhs; }
        public JCExpression getExpression() { return rhs; }
        public Symbol getOperator() {
            return operator;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitCompoundAssignment(this, d);
        }
        @Override
        public int getTag() {
            return opcode;
        }
    }

    /**
     * A unary operation.
     */
    public static class JCUnary extends JCExpression implements UnaryTree {
        private int opcode;
        public JCExpression arg;
        public Symbol operator;
		public JCMethodInvocation apply=null;

        protected JCUnary(int opcode, JCExpression arg) {
            this.opcode = opcode;
            this.arg = arg;
        }
        @Override
        public void accept(Visitor v) { v.visitUnary(this); }

        public Kind getKind() { return TreeInfo.tagToKind(getTag()); }
        public JCExpression getExpression() { return arg; }
        public JCTree getApply() { return apply; }
        public Symbol getOperator() {
            return operator;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitUnary(this, d);
        }
        @Override
        public int getTag() {
            return opcode;
        }

        public void setTag(int tag) {
            opcode = tag;
        }


        public String operatorName(int tag) {
            switch (tag) {
                case JCTree.POS:
                    return "+";
                case JCTree.NEG:
                    return "-";
                case JCTree.NOT:
                    return "!";
                case JCTree.COMPL:
                    return "~";
                case JCTree.PREINC:
                    return "++";
                case JCTree.PREDEC:
                    return "--";
                case JCTree.POSTINC:
                    return "++";
                case JCTree.POSTDEC:
                    return "--";
                case JCTree.NULLCHK:
                    return "<*nullchk*>";
                case JCTree.OR:
                    return "||";
                case JCTree.AND:
                    return "&&";
                case JCTree.EQ:
                    return "==";
                case JCTree.NE:
                    return "!=";
                case JCTree.LT:
                    return "<";
                case JCTree.GT:
                    return ">";
                case JCTree.LE:
                    return "<=";
                case JCTree.GE:
                    return ">=";
                case JCTree.BITOR:
                    return "|";
                case JCTree.BITXOR:
                    return "^";
                case JCTree.BITAND:
                    return "&";
                case JCTree.SL:
                    return "<<";
                case JCTree.SR:
                    return ">>";
                case JCTree.USR:
                    return ">>>";
                case JCTree.SEQ:
                    return ":";
                case JCTree.PLUS:
                    return "+";
                case JCTree.MINUS:
                    return "-";
                case JCTree.MUL:
                    return "*";
                case JCTree.DIV:
                    return "/";
                case JCTree.MOD:
                    return "%";
                default:
                    throw new Error();
            }
	}
    }

	public static class JCSequence extends JCExpression implements SequenceTree {
        public List<JCExpression> seq;
        protected JCSequence(List<JCExpression> seq) {
            this.seq = seq;
        }
        @Override
        public void accept(Visitor v) { v.visitSequence(this); }

		public List<JCExpression> getList()
		{
			return seq;
		}

        public Kind getKind() { return Kind.JOIN;  }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitSequence(this, d);
        }
        @Override
        public int getTag() {
            return SEQUENCE;
        }
    }

	public static class JCJoinDomains extends JCExpression implements JoinTree {

		public List<JCExpression> doms;
		public boolean allowUnderSpec=false;

		public JCJoinDomains(List<JCExpression> doms)
		{
			this.doms=doms;
		}

        @Override
        public void accept(Visitor v) { v.visitJoin(this); }

        public Kind getKind() { return Kind.JOIN; }

        @Override
        public List<JCExpression> getOperands() {
            return doms;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitJoin(this, d);
        }
        @Override
        public int getTag() {
            return JOIN;
        }
	}

    /**
     * A binary operation.
     */
    public static class JCBinary extends JCExpression implements BinaryTree {
        private int opcode;
        public JCExpression lhs;
        public JCExpression rhs;
        public Symbol operator;
		public JCMethodInvocation apply=null;
        protected JCBinary(int opcode,
                         JCExpression lhs,
                         JCExpression rhs,
                         Symbol operator) {
            this.opcode = opcode;
            this.lhs = lhs;
            this.rhs = rhs;
            this.operator = operator;
        }
        @Override
        public void accept(Visitor v) { v.visitBinary(this); }

        public Kind getKind() { return TreeInfo.tagToKind(getTag()); }
        public JCExpression getLeftOperand() { return lhs; }
        public JCExpression getRightOperand() { return rhs; }
        public JCTree getApply() { return apply; }
        public Symbol getOperator() {
            return operator;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitBinary(this, d);
        }
        @Override
        public int getTag() {
            return opcode;
        }

        public String operatorName(int tag) {
            switch (tag) {
                case JCTree.POS:
                    return "+";
                case JCTree.NEG:
                    return "-";
                case JCTree.NOT:
                    return "!";
                case JCTree.COMPL:
                    return "~";
                case JCTree.PREINC:
                    return "++";
                case JCTree.PREDEC:
                    return "--";
                case JCTree.POSTINC:
                    return "++";
                case JCTree.POSTDEC:
                    return "--";
                case JCTree.NULLCHK:
                    return "<*nullchk*>";
                case JCTree.OR:
                    return "||";
                case JCTree.AND:
                    return "&&";
                case JCTree.EQ:
                    return "==";
                case JCTree.NE:
                    return "!=";
                case JCTree.LT:
                    return "<";
                case JCTree.GT:
                    return ">";
                case JCTree.LE:
                    return "<=";
                case JCTree.GE:
                    return ">=";
                case JCTree.BITOR:
                    return "|";
                case JCTree.BITXOR:
                    return "^";
                case JCTree.BITAND:
                    return "&";
                case JCTree.SL:
                    return "<<";
                case JCTree.SR:
                    return ">>";
                case JCTree.USR:
                    return ">>>";
                case JCTree.SEQ:
                    return ":";
                case JCTree.PLUS:
                    return "+";
                case JCTree.MINUS:
                    return "-";
                case JCTree.MUL:
                    return "*";
                case JCTree.DIV:
                    return "/";
                case JCTree.MOD:
                    return "%";
                default:
                    throw new Error();
            }
	}
    }

    /**
     * A type cast.
     */
    public static class JCTypeCast extends JCExpression implements TypeCastTree {
        public JCTree clazz;
        public JCExpression expr;
        protected JCTypeCast(JCTree clazz, JCExpression expr) {
            this.clazz = clazz;
            this.expr = expr;
        }
        @Override
        public void accept(Visitor v) { v.visitTypeCast(this); }

        public Kind getKind() { return Kind.TYPE_CAST; }
        public JCTree getType() { return clazz; }
        public JCExpression getExpression() { return expr; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitTypeCast(this, d);
        }
        @Override
        public int getTag() {
            return TYPECAST;
        }
    }

    /**
     * A type test.
     */
    public static class JCInstanceOf extends JCExpression implements InstanceOfTree {
        public JCExpression expr;
        public JCTree clazz;
        protected JCInstanceOf(JCExpression expr, JCTree clazz) {
            this.expr = expr;
            this.clazz = clazz;
        }
        @Override
        public void accept(Visitor v) { v.visitTypeTest(this); }

        public Kind getKind() { return Kind.INSTANCE_OF; }
        public JCTree getType() { return clazz; }
        public JCExpression getExpression() { return expr; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitInstanceOf(this, d);
        }
        @Override
        public int getTag() {
            return TYPETEST;
        }
    }

    /**
     * An array selection
     */
    public static class JCArrayAccess extends JCExpression implements ArrayAccessTree {
        public JCExpression indexed;
        public List<JCExpression> index;
		public List<JCExpression> params;
        protected JCArrayAccess(JCExpression indexed, List<JCExpression> index, List<JCExpression> params) {
            this.indexed = indexed;
            this.index = index;
			this.params = params;
        }

        protected JCArrayAccess(JCExpression indexed, List<JCExpression> index) {
            this.indexed = indexed;
            this.index = index;
			this.params = null;
        }
        @Override
        public void accept(Visitor v) { v.visitIndexed(this); }

        public Kind getKind() { return Kind.ARRAY_ACCESS; }
        public JCExpression getExpression() { return indexed; }
        public List<JCExpression> getIndex() { return index; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitArrayAccess(this, d);
        }
        @Override
        public int getTag() {
            return INDEXED;
        }
    }

    /**
     * Selects through packages and classes
     * @param selected selected Tree hierarchie
     * @param selector name of field to select thru
     * @param sym symbol of the selected class
     */
    public static class JCFieldAccess extends JCSymbolExpression implements MemberSelectTree {
        public JCExpression selected;
        //public Name name;
        public JCExpression name;
        public Symbol sym;
		public List<JCExpression> params;

		public JCDomainIter conversion = null;
		public boolean repackage=false;

        protected JCFieldAccess(JCExpression selected, JCExpression name, Symbol sym,List<JCExpression> params) {
            this.selected = selected;
            this.name = name;
			this.params = params;
            this.sym = sym;
        }
        protected JCFieldAccess(JCExpression selected, JCExpression name, Symbol sym) {
            this.selected = selected;
            this.name = name;
			this.params = null;
            this.sym = sym;
        }

        @Override
        public Symbol getSymbol() {return sym;}

		@Override
        public void setSymbol(Symbol s) {sym=s;}

        @Override
        public void accept(Visitor v) { v.visitSelect(this); }

        public Kind getKind() { return Kind.MEMBER_SELECT; }
        public JCExpression getExpression() { return selected; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitMemberSelect(this, d);
        }
        public Name getIdentifier()
        {
            if(name.getTag()==IDENT)
                return ((JCIdent)name).getName();
            else
                return null;
        }
        @Override
        public int getTag() {
            return SELECT;
        }
    }

    /**
     * An identifier
     * @param idname the name
     * @param sym the symbol
     */
    public static class JCSizeOf extends JCExpression implements SizeOfTree {
        public JCExpression expr;

        protected JCSizeOf(JCExpression expr) {
            this.expr = expr;
        }

        @Override
        public void accept(Visitor v) { v.visitSizeOf(this); }

        public Kind getKind() { return Kind.SIZEOF; }
        public JCExpression getExpression(){ return expr; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitSizeOf(this, d);
        }
        public int getTag() {
            return SIZEOF;
        }
    }


    /**
     * An identifier
     * @param idname the name
     * @param sym the symbol
     */
    public static class JCIdent extends JCSymbolExpression implements IdentifierTree {
        public Name name;
        public Symbol sym;
        protected JCIdent(Name name, Symbol sym) {
            this.name = name;
            this.sym = sym;
        }
        @Override
        public Symbol getSymbol() {return sym;}
		@Override
        public void setSymbol(Symbol s) {sym=s;}

        @Override
        public void accept(Visitor v) { v.visitIdent(this); }

        public Kind getKind() { return Kind.IDENTIFIER; }
        public Name getName() { return name; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitIdentifier(this, d);
        }
        public int getTag() {
            return IDENT;
        }
    }

    /**
     * A constant value given literally.
     * @param value value representation
     */
    public static class JCLiteral extends JCExpression implements LiteralTree {
        public int typetag;
        public Object value;
        public JCLiteral(int typetag, Object value) {
            this.typetag = typetag;
            this.value = value;
        }
        @Override
        public void accept(Visitor v) { v.visitLiteral(this); }

        public Kind getKind() {
            switch (typetag) {
            case TypeTags.INT:
                return Kind.INT_LITERAL;
            case TypeTags.LONG:
                return Kind.LONG_LITERAL;
            case TypeTags.FLOAT:
                return Kind.FLOAT_LITERAL;
            case TypeTags.DOUBLE:
                return Kind.DOUBLE_LITERAL;
            case TypeTags.BOOLEAN:
                return Kind.BOOLEAN_LITERAL;
            case TypeTags.CHAR:
                return Kind.CHAR_LITERAL;
            case TypeTags.GROUP:
                return Kind.GROUP_LITERAL;
            case TypeTags.CLASS:
                return Kind.STRING_LITERAL;
            case TypeTags.BOT:
                return Kind.NULL_LITERAL;
            default:
                throw new AssertionError("unknown literal kind " + this);
            }
        }
        public Object getValue() {
            switch (typetag) {
                case TypeTags.BOOLEAN:
                    int bi = (Integer) value;
                    return (bi != 0);
                case TypeTags.CHAR:
                    int ci = (Integer) value;
                    char c = (char) ci;
                    if (c != ci)
                        throw new AssertionError("bad value for char literal");
                    return c;
                default:
                    return value;
            }
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitLiteral(this, d);
        }
        @Override
        public JCLiteral setType(Type type) {
            super.setType(type);
            return this;
        }
        @Override
        public int getTag() {
            return LITERAL;
        }
    }

    /**
     * Identifies a basic type.
     * @param tag the basic type id
     * @see TypeTags
     */
    public static class JCPrimitiveTypeTree extends JCExpression implements PrimitiveTypeTree {
        public int typetag;
        protected JCPrimitiveTypeTree(int typetag) {
            this.typetag = typetag;
        }
        @Override
        public void accept(Visitor v) { v.visitTypeIdent(this); }

        public Kind getKind() { return Kind.PRIMITIVE_TYPE; }
        public TypeKind getPrimitiveTypeKind() {
            switch (typetag) {
            case TypeTags.BOOLEAN:
                return TypeKind.BOOLEAN;
            case TypeTags.BYTE:
                return TypeKind.BYTE;
            case TypeTags.SHORT:
                return TypeKind.SHORT;
            case TypeTags.INT:
                return TypeKind.INT;
            case TypeTags.LONG:
                return TypeKind.LONG;
            case TypeTags.CHAR:
                return TypeKind.CHAR;
            case TypeTags.FLOAT:
                return TypeKind.FLOAT;
            case TypeTags.DOUBLE:
                return TypeKind.DOUBLE;
            case TypeTags.VOID:
                return TypeKind.VOID;
            default:
                throw new AssertionError("unknown primitive type " + this);
            }
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitPrimitiveType(this, d);
        }
        @Override
        public int getTag() {
            return TYPEIDENT;
        }
    }

    /**
     * An array type, A[]
     */
    public static class JCArrayTypeTree extends JCExpression implements ArrayTypeTree {
        public JCExpression elemtype;
		public boolean option;
		public JCDomInstance dom;
        protected JCArrayTypeTree(JCExpression elemtype,JCDomInstance dom,boolean option) {
            this.elemtype = elemtype;
			this.option = option;
			this.dom = dom;
        }
        @Override
        public void accept(Visitor v) { v.visitTypeArray(this); }

        public Kind getKind() { return Kind.ARRAY_TYPE; }
        public JCTree getType() { return elemtype; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitArrayType(this, d);
        }
        @Override
        public int getTag() {
            return TYPEARRAY;
        }
    }

    /**
     * A parameterized type, T<...>
     */
    public static class JCTypeApply extends JCExpression implements ParameterizedTypeTree {
        public JCExpression clazz;
        public List<JCExpression> arguments;
        protected JCTypeApply(JCExpression clazz, List<JCExpression> arguments) {
            this.clazz = clazz;
            this.arguments = arguments;
        }
        @Override
        public void accept(Visitor v) { v.visitTypeApply(this); }

        public Kind getKind() { return Kind.PARAMETERIZED_TYPE; }
        public JCTree getType() { return clazz; }
        public List<JCExpression> getTypeArguments() {
            return arguments;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitParameterizedType(this, d);
        }
        @Override
        public int getTag() {
            return TYPEAPPLY;
        }
    }

    /**
     * A formal class parameter.
     * @param name name
     * @param bounds bounds
     */
    public static class JCTypeParameter extends JCTree implements TypeParameterTree {
        public Name name;
        public List<JCExpression> bounds;
        protected JCTypeParameter(Name name, List<JCExpression> bounds) {
            this.name = name;
            this.bounds = bounds;
        }
        @Override
        public void accept(Visitor v) { v.visitTypeParameter(this); }

        public Kind getKind() { return Kind.TYPE_PARAMETER; }
        public Name getName() { return name; }
        public List<JCExpression> getBounds() {
            return bounds;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitTypeParameter(this, d);
        }
        @Override
        public int getTag() {
            return TYPEPARAMETER;
        }
    }

// ALEX : DOMAINS

    /**
     * A Domiter definition.
     * @param name the name of the class

     */
    public static class JCDomainIter extends JCSymbolExpression implements DomIterTree {
        public Name name;
        public List<JCVariableDecl> domargs;
        public JCExpression exp,body;
        public JCStatement sbody;
        public Symbol sym;
		public List<JCExpression> params;
		public Type iterType = null;
		public boolean mayReturn = false;
		public boolean valueAccess = false;
		public boolean valueOffsetAccess = false;
		public boolean emptyInit = false;
		public boolean insideProjection = false;
		//public JCTree exit;
        public JCTree dg_end;

        protected JCDomainIter(JCExpression exp,
                           Name name,
                           List<JCVariableDecl> domargs,
                           JCExpression body,
                           JCStatement sbody,
						   List<JCExpression> params
                           )
        {
            this.name = name;
            this.domargs = domargs;
            this.exp = exp;
            this.body = body;
            this.sbody = sbody;
			this.params = params;
            sym=null;
//			isScheduler = true;
        }

        @Override
        public Symbol getSymbol() {return sym;}
		@Override
        public void setSymbol(Symbol s) {sym=s;}

        @Override
        public void accept(Visitor v) { v.visitDomIter(this); }

        public Kind getKind() { return Kind.DOMITER; }
//        public JCModifiers getModifiers() { return mods; }
        public Name getName() { return name; }

        public List<? extends VariableTree> getDomArgs()
        {
            return domargs;
        }

        public JCExpression getExpression(){ return exp; }

        public JCExpression getBody(){ return body; }

        public JCStatement getSBody(){ return sbody; }


        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitDomIter(this, d);
        }

        @Override
        public int getTag() {
            return DOMITER;
        }
    }


    /**
     * A class definition.
     * @param modifiers the modifiers
     * @param name the name of the class
     * @param typarams formal class parameters
     * @param extending the classes this class extends
     * @param implementing the interfaces implemented by this class
     * @param defs all variables and methods defined in this class
     * @param sym the symbol
     */
    public static class JCDomainDecl extends JCStatement implements DomainTree {
        public Name name;
        public List<JCDomParameter> domparams;
        public List<JCDomParameter> domargs;
        public List<JCDomParameter> defs;
		public JCDomUsage domdef;
        public List<JCTree> constraints;
		public JCDomParent parent;
        public DomainSymbol sym;
        protected JCDomainDecl(//JCModifiers mods,
                           Name name,
                           List<JCDomParameter> domparams,
                           List<JCDomParameter> domargs,
                           //JCTree extending,
                           //List<JCExpression> implementing,
                           List<JCDomParameter> defs,
						   JCDomUsage domdef,
                           List<JCTree> constraints,
                           DomainSymbol sym,
						   JCDomParent parent)
        {
            this.name = name;
            this.domparams = domparams;
            this.domargs = domargs;
//            this.implementing = implementing;
            this.defs = defs;
			this.domdef = domdef;
            this.constraints = constraints;
            this.sym = sym;
			this.parent = parent;
        }
        @Override
        public void accept(Visitor v) { v.visitDomainDef(this); }

        public Kind getKind() { return Kind.DOMAIN; }
//        public JCModifiers getModifiers() { return mods; }
        public Name getSimpleName() { return name; }


        public List<JCDomParameter> getDomParameters() {
            return domparams;
        }
/*        public JCTree getExtendsClause() { return extending; }
*/
/*
        public List<JCExpression> getImplementsClause() {
            return implementing;
        }
 *
 */
        public List<? extends DomParameterTree> getDomArgs()
        {
            return domargs;
        }

        public List<? extends Tree> getDefs()
        {
            return defs;
        }

        public List<JCTree> getConstraints() {
            return constraints;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitDomain(this, d);
        }

        @Override
        public int getTag() {
            return DOMDEF;
        }
    }

   /**
     * A term of a domain constraint, including a variable and its coefficient.
     * If the variable is null it represents a constant.
     */
    public static class JCDomConstraintValue extends JCTree implements DomConstraintValueTree {
        public int sign; // always (+1) or (-1), note: both sign an coeff can be negative
        public JCExpression coeff;
        public JCDomParameter parameter;
        public JCDomParameter parameter1;
        protected JCDomConstraintValue(int sign, JCExpression coeff, JCDomParameter parameter) {
            this.sign=sign;
            this.coeff=coeff;
            this.parameter=parameter;
        }
        protected JCDomConstraintValue(int sign, JCDomParameter param1, JCDomParameter parameter) {
            this.sign=sign;
            this.parameter1=param1;
            this.parameter=parameter;
        }
        @Override
        public void accept(Visitor v)
        {
            v.visitDomConstraintValue(this);
        }

        public Kind getKind() { return Kind.DOM_CONSTRAINT_VALUE; }
        //public Name getName() { return name; }

        public JCExpression getCoeff()
        {
            return coeff;
        }

        public JCDomParameter getParameter()
        {
            return parameter;
        }

		public JCDomParameter getParameter1()
        {
            return parameter1;
        }

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitDomConstraintValue(this, d);
        }
        @Override
        public int getTag() {
            return DOMCONSTRAINTVALUE;
        }
    }

   /**
     * A formal class parameter.
     * @param name name
     * @param bounds bounds
     */
    public static class JCDomConstraint extends JCTree implements DomConstraintTree {
        public List<JCDomConstraintValue> left;
        public int assign;
        public List<JCDomConstraintValue> right;

        //public List<JCExpression> bounds;
        protected JCDomConstraint(List<JCDomConstraintValue> left, int assign, List<JCDomConstraintValue> right) {
            this.left = left;
            this.assign = assign;
            this.right = right;
        }
        @Override
        public void accept(Visitor v)
        {
            v.visitDomConstraint(this);
        }

        public Kind getKind() { return Kind.DOM_CONSTRAINT; }

        public int getAssign()
        {
            return assign;
        }

        public List<JCDomConstraintValue> getLeftSide()
        {
            return left;
        }
        public List<JCDomConstraintValue> getRightSide()
        {
            return right;
        }

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitDomConstraint(this, d);
        }
        @Override
        public int getTag() {
            return DOMCONSTRAINT;
        }
    }

    /**
     * A formal class parameter.
     * @param name name
     * @param bounds bounds
     */
    public static class JCDomParameter extends JCTree implements DomParameterTree {
        public Name name;
        //public List<JCExpression> bounds;
        protected JCDomParameter(Name name) {
            this.name = name;
        }
        @Override
        public void accept(Visitor v)
        {
            v.visitDomParameter(this);
        }

        public Kind getKind() { return Kind.DOM_PARAMETER; }
        public Name getName() { return name; }

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitDomParameter(this, d);
        }
        @Override
        public int getTag() {
            return DOMPARAMETER;
        }
    }

    /**
     * A formal class parameter.
     * @param name name
     * @param bounds bounds
     */
    public static class JCCTProperty extends JCExpression implements CTPropertyTree {
        public Name name;
        public JCExpression exp;
        //public List<JCExpression> bounds;
        protected JCCTProperty(JCExpression exp,Name name) {
            this.name = name;
            this.exp = exp;
        }
        @Override
        public void accept(Visitor v)
        {
            v.visitCTProperty(this);
        }

        public Kind getKind() { return Kind.CT_PROPERTY; }
        public Name getName() { return name; }
        public JCExpression getExp() { return exp; }

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitCTProperty(this, d);
        }
        @Override
        public int getTag() {
            return CTPROPERTY;
        }
    }


    /**
     * A formal class parameter.
     * @param name name
     * @param bounds bounds
     */
    public static class JCDomInstance extends JCTree implements DomInstanceTree {
        //public Name name;
        //public List<JCExpression> bounds;s
        public Name name;
        public List<JCExpression> domparams;

        protected JCDomInstance(Name name, List<JCExpression> domparams) {
            this.name = name;
			this.domparams = domparams;
        }
        @Override
        public void accept(Visitor v)
        {
            v.visitDomInstance(this);
        }

        public Kind getKind() { return Kind.DOM_INSTANCE; }
        public Name getName() { return name; }
        public List<JCExpression> getParams() { return domparams; }

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitDomInstance(this, d);
        }
        @Override
        public int getTag() {
            return DOMINSTANCE;
        }
    }

    /**
     * A formal class parameter.
     * @param name name
     * @param bounds bounds
     */
    public static class JCDomParent extends JCTree implements DomParentTree {
        //public Name name;
        //public List<JCExpression> bounds;
        public Name name;
        public List<JCExpression> domparams;
		public List<JCDomParameter> domargs;

        protected JCDomParent(Name name, List<JCExpression> domparams,List<JCDomParameter> domargs) {
            this.name = name;
			this.domparams = domparams;
			this.domargs=domargs;
        }
        @Override
        public void accept(Visitor v)
        {
            v.visitDomParent(this);
        }

        public Kind getKind() { return Kind.DOM_PARENT; }
        public Name getName() { return name; }
        public List<JCExpression> getParams() { return domparams; }

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitDomParent(this, d);
        }
        @Override
        public int getTag() {
            return DOMINSTANCE;
        }
    }

   /**
     * A formal class parameter.
     * @param name name
     * @param bounds bounds
     */
    public static class JCDomUsage extends JCTree implements DomUsageTree {
        //public Name name;
        //public List<JCExpression> bounds;
        public Name name;
        public List<JCDomParameter> domparams;

        protected JCDomUsage(Name name, List<JCDomParameter> domparams) {
            this.name = name;
			this.domparams = domparams;
        }
        @Override
        public void accept(Visitor v)
        {
            v.visitDomUsage(this);
        }

        public Kind getKind() { return Kind.DOM_USAGE; }
        public Name getName() { return name; }
        public List<JCDomParameter> getParams() { return domparams; }

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitDomUsage(this, d);
        }
        @Override
        public int getTag() {
            return DOMUSAGE;
        }
    }

// END DOMAINS

    public static class JCWildcard extends JCExpression implements WildcardTree {
        public TypeBoundKind kind;
        public JCTree inner;
        protected JCWildcard(TypeBoundKind kind, JCTree inner) {
            kind.getClass(); // null-check
            this.kind = kind;
            this.inner = inner;
        }
        @Override
        public void accept(Visitor v) { v.visitWildcard(this); }

        public Kind getKind() {
            switch (kind.kind) {
            case UNBOUND:
                return Kind.UNBOUNDED_WILDCARD;
            case EXTENDS:
                return Kind.EXTENDS_WILDCARD;
            case SUPER:
                return Kind.SUPER_WILDCARD;
            default:
                throw new AssertionError("Unknown wildcard bound " + kind);
            }
        }
        public JCTree getBound() { return inner; }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitWildcard(this, d);
        }
        @Override
        public int getTag() {
            return WILDCARD;
        }
    }

    public static class TypeBoundKind extends JCTree {
        public BoundKind kind;
        protected TypeBoundKind(BoundKind kind) {
            this.kind = kind;
        }
        @Override
        public void accept(Visitor v) { v.visitTypeBoundKind(this); }

        public Kind getKind() {
            throw new AssertionError("TypeBoundKind is not part of a public API");
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            throw new AssertionError("TypeBoundKind is not part of a public API");
        }
        @Override
        public int getTag() {
            return TYPEBOUNDKIND;
        }
    }

    public static class JCAnnotation extends JCExpression implements AnnotationTree {
        public JCTree annotationType;
        public List<JCExpression> args;
        protected JCAnnotation(JCTree annotationType, List<JCExpression> args) {
            this.annotationType = annotationType;
            this.args = args;
        }
        @Override
        public void accept(Visitor v) { v.visitAnnotation(this); }

        public Kind getKind() { return Kind.ANNOTATION; }
        public JCTree getAnnotationType() { return annotationType; }
        public List<JCExpression> getArguments() {
            return args;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitAnnotation(this, d);
        }
        @Override
        public int getTag() {
            return ANNOTATION;
        }
    }

    public static class JCModifiers extends JCTree implements com.sun.source.tree.ModifiersTree {
        public long flags;
        public List<JCAnnotation> annotations;
        public JCExpression group = null;
        public JCExpression thread = null;
        public JCExpression work = null;
        public JCExpression task = null;
        public JCExpression mem = null;
        /*
        protected JCModifiers(long flags, List<JCAnnotation> annotations) {
            this.flags = flags;
            this.annotations = annotations;
        }
        */
        protected JCModifiers(long flags, List<JCAnnotation> annotations,JCExpression group) {
            this.flags = flags;
            this.annotations = annotations;
            this.group = group;
        }

		protected JCModifiers(long flags, List<JCAnnotation> annotations,JCExpression group,JCExpression thread,JCExpression work,JCExpression task,JCExpression mem) {
            this.flags = flags;
            this.annotations = annotations;
            this.group = group;
            this.work = work;
            this.task = task;
            this.mem = mem;
			this.thread = thread;
        }
        @Override
        public void accept(Visitor v) { v.visitModifiers(this); }

        public Kind getKind() { return Kind.MODIFIERS; }
        public Set<Modifier> getFlags() {
            return Flags.asModifierSet(flags);
        }
        public List<JCAnnotation> getAnnotations() {
            return annotations;
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitModifiers(this, d);
        }
        @Override
        public int getTag() {
            return MODIFIERS;
        }
    }

    public static class JCErroneous extends JCExpression
            implements com.sun.source.tree.ErroneousTree {
        public List<? extends JCTree> errs;
        protected JCErroneous(List<? extends JCTree> errs) {
            this.errs = errs;
        }
        @Override
        public void accept(Visitor v) { v.visitErroneous(this); }

        public Kind getKind() { return Kind.ERRONEOUS; }

        public List<? extends JCTree> getErrorTrees() {
            return errs;
        }

        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            return v.visitErroneous(this, d);
        }
        @Override
        public int getTag() {
            return ERRONEOUS;
        }
    }

    /** (let int x = 3; in x+2) */
    public static class LetExpr extends JCExpression {
        public List<JCVariableDecl> defs;
        public JCTree expr;
        protected LetExpr(List<JCVariableDecl> defs, JCTree expr) {
            this.defs = defs;
            this.expr = expr;
        }
        @Override
        public void accept(Visitor v) { v.visitLetExpr(this); }

        public Kind getKind() {
            throw new AssertionError("LetExpr is not part of a public API");
        }
        @Override
        public <R,D> R accept(TreeVisitor<R,D> v, D d) {
            throw new AssertionError("LetExpr is not part of a public API");
        }
        @Override
        public int getTag() {
            return LETEXPR;
        }
    }

    /** An interface for tree factories
     */
    public interface Factory {
        JCCompilationUnit TopLevel(List<JCAnnotation> packageAnnotations,
                                   JCExpression pid,
                                   List<JCTree> defs);
        JCImport Import(JCTree qualid, boolean staticImport);

// ALEX

        JCDomParameter DomParameter( Name name );

        JCCTProperty CTP( JCExpression exp, Name name );

        JCDomInstance DomInstance( Name name, List<JCExpression> domparams );

		JCDomParent DomParent( Name name, List<JCExpression> domparams, List<JCDomParameter> domargs );

        JCDomConstraintValue ConstraintValue(int sign, JCExpression literal,
                JCDomParameter dom_param);

		JCDomConstraintValue ConstraintValue(int sign, JCDomParameter param,
                JCDomParameter dom_param);

        JCDomConstraint ConstrainDef(List<JCDomConstraintValue> left, int assign,
        	List<JCDomConstraintValue> right);

        JCDomainDecl DomDef( Name name,
                          List<JCDomParameter> domparams,
                          List<JCDomParameter> domargs,
                          //List<JCExpression> implementing,
                          List<JCDomParameter> defs,
						  JCDomUsage domdef,
                          List<JCTree> constraints,
						  JCDomParent parent);

        JCArgExpression ArgExpression( JCExpression exp1,
                          JCExpression exp2);

// END

        JCClassDecl ClassDef(JCModifiers mods,
                          Name name,
                          List<JCTypeParameter> typarams,
                          JCTree extending,
                          List<JCExpression> implementing,
                          List<JCTree> defs);

        JCClassDecl SingularDef(JCModifiers mods,
                          Name name,
                          List<JCTypeParameter> typarams,
                          JCTree extending,
                          List<JCExpression> implementing,
                          List<JCTree> defs);

        JCMethodDecl MethodDef(JCModifiers mods,
                            Name name,
                            JCExpression restype,
                            List<JCTypeParameter> typarams,
                            List<JCVariableDecl> params,
                            List<JCExpression> thrown,
                            JCBlock body,
                            JCExpression defaultValue);

       JCMethodDecl EventDef(JCModifiers mods,
                            Name name,
                            //JCExpression restype,
                            List<JCTypeParameter> typarams,
                            List<JCVariableDecl> params,
                            List<JCExpression> thrown,
                            JCBlock body,
                            JCExpression defaultValue);

        JCVariableDecl VarDef(JCModifiers mods,
                      Name name,
                      JCExpression vartype,
                      JCExpression init);

		JCVariableDecl VarDef(JCModifiers mods,
                      Name name,
                      JCExpression vartype,
                      JCExpression init,com.sun.tools.javac.util.List<JCVariableDecl> list);



        JCSkip Skip();
        JCBlock Block(long flags, List<JCStatement> stats);
        JCDoWhileLoop DoLoop(JCStatement body, JCExpression cond);
        JCWhileLoop WhileLoop(JCExpression cond, JCStatement body);
        JCForLoop ForLoop(List<JCStatement> init,
                        JCExpression cond,
                        List<JCExpressionStatement> step,
                        JCStatement body);
        JCEnhancedForLoop ForeachLoop(JCVariableDecl var, JCExpression expr, JCStatement body);
        JCLabeledStatement Labelled(Name label, JCStatement body);
        JCSwitch Switch(JCExpression selector, List<JCCase> cases);
        JCCase Case(JCExpression pat, List<JCStatement> stats);
        JCSynchronized Synchronized(JCExpression lock, JCBlock body);
        JCTry Try(JCBlock body, List<JCCatch> catchers, JCBlock finalizer);
        JCCatch Catch(JCVariableDecl param, JCBlock body);
        JCConditional Conditional(JCExpression cond,
                                JCExpression thenpart,
                                JCExpression elsepart);
        JCIf If(JCExpression cond, JCStatement thenpart, JCStatement elsepart);
        JCIfExp IfExp(JCExpression cond, JCExpression thenpart, JCExpression elsepart);
        JCExpressionStatement Exec(JCExpression expr);
        JCBreak Break(Name label);
        JCContinue Continue(Name label);
        JCReturn Return(JCExpression expr,long flags);
        JCThrow Throw(JCTree expr);
        JCAssert Assert(JCExpression cond, JCExpression detail);
        JCMethodInvocation Apply(List<JCExpression> typeargs,
                    JCExpression fn,
                    List<JCExpression> args);
        JCNewClass NewClass(JCExpression encl,
                          List<JCExpression> typeargs,
                          JCExpression clazz,
                          List<JCExpression> args,
                          JCClassDecl def);
        JCNewArray NewArray(JCExpression elemtype,
						  JCDomInstance dom,
                          List<JCExpression> dims,
                          List<JCExpression> elems);

        JCSet Set();
        JCCF  NewCF(JCTree condition,boolean value,int pos);
		JCJoinDomains Join(List<JCExpression> doms);
        JCParens Parens(JCExpression expr);
        JCAssign Assign(JCExpression lhs, JCExpression rhs,long aflags,JCExpression cond);
        JCAssign Assign(JCExpression lhs, JCExpression rhs);
        JCWhere Where(JCExpression exp,JCStatement stmnt,JCExpression sexp);
        JCFor StaticFor(Name name, JCExpression exp, List<JCTree> content);
        JCSelect Select(List<JCSelectCond> list);
        JCCaseExp CaseExp(JCExpression exp,List<JCSelectCond> list);
        JCSelectCond SelectCond(JCExpression cond,JCExpression res,JCStatement stmnt);
        JCDomainIter DomIter(JCExpression exp,Name name,List<JCVariableDecl> domargs,JCExpression sexp,JCStatement sbody, List<JCExpression> params);
        JCAssignOp Assignop(int opcode, JCTree lhs, JCTree rhs);
        JCUnary Unary(int opcode, JCExpression arg);
        JCBinary Binary(int opcode, JCExpression lhs, JCExpression rhs);
        JCTypeCast TypeCast(JCTree expr, JCExpression type);
        JCInstanceOf TypeTest(JCExpression expr, JCTree clazz);
        JCArrayAccess Indexed(JCExpression indexed, List<JCExpression> index);
        JCFieldAccess Select(JCExpression selected, JCExpression selector);
        JCIdent Ident(Name idname);
        JCLiteral Literal(int tag, Object value);
        JCPrimitiveTypeTree TypeIdent(int typetag);
        JCArrayTypeTree TypeArray(JCExpression elemtype,JCDomInstance dom,boolean option);
        JCTypeApply TypeApply(JCExpression clazz, List<JCExpression> arguments);
        JCTypeParameter TypeParameter(Name name, List<JCExpression> bounds);
        JCWildcard Wildcard(TypeBoundKind kind, JCTree type);
        TypeBoundKind TypeBoundKind(BoundKind kind);
        JCAnnotation Annotation(JCTree annotationType, List<JCExpression> args);
        JCModifiers Modifiers(long flags, List<JCAnnotation> annotations, JCExpression group, JCExpression thread, JCExpression work, JCExpression task, JCExpression mem);
        JCModifiers Modifiers(long flags, List<JCAnnotation> annotations);
        JCErroneous Erroneous(List<? extends JCTree> errs);
        LetExpr LetExpr(List<JCVariableDecl> defs, JCTree expr);
    }

    /** A generic visitor class for trees.
     */
    public static abstract class Visitor {
        public void visitTopLevel(JCCompilationUnit that)    { visitTree(that); }
        public void visitImport(JCImport that)               { visitTree(that); }
        public void visitClassDef(JCClassDecl that)          { visitTree(that); }
        public void visitArgExpression(JCArgExpression that) { visitTree(that); }
        public void visitDomainDef(JCDomainDecl that)        { visitTree(that); }
        public void visitDomIter(JCDomainIter that)          { visitTree(that); }
        public void visitMethodDef(JCMethodDecl that)        { visitTree(that); }
        public void visitVarDef(JCVariableDecl that)         { visitTree(that); }
        public void visitSkip(JCSkip that)                   { visitTree(that); }
        public void visitCF(JCCF that)                   { visitTree(that); }
        public void visitSet(JCSet that)                   { visitTree(that); }
        public void visitBlock(JCBlock that)                 { visitTree(that); }
        public void visitDoLoop(JCDoWhileLoop that)          { visitTree(that); }
        public void visitWhileLoop(JCWhileLoop that)         { visitTree(that); }
        public void visitForLoop(JCForLoop that)             { visitTree(that); }
        public void visitForeachLoop(JCEnhancedForLoop that) { visitTree(that); }
        public void visitLabelled(JCLabeledStatement that)   { visitTree(that); }
        public void visitSwitch(JCSwitch that)               { visitTree(that); }
        public void visitCase(JCCase that)                   { visitTree(that); }
        public void visitSynchronized(JCSynchronized that)   { visitTree(that); }
        public void visitTry(JCTry that)                     { visitTree(that); }
        public void visitCatch(JCCatch that)                 { visitTree(that); }
        public void visitConditional(JCConditional that)     { visitTree(that); }
        public void visitIf(JCIf that)                       { visitTree(that); }
        public void visitIfExp(JCIfExp that)                       { visitTree(that); }
        public void visitExec(JCExpressionStatement that)    { visitTree(that); }
        public void visitBreak(JCBreak that)                 { visitTree(that); }
        public void visitContinue(JCContinue that)           { visitTree(that); }
        public void visitReturn(JCReturn that)               { visitTree(that); }
        public void visitThrow(JCThrow that)                 { visitTree(that); }
        public void visitAssert(JCAssert that)               { visitTree(that); }
        public void visitPragma(JCPragma that)               { visitTree(that); }
        public void visitApply(JCMethodInvocation that)      { visitTree(that); }
        public void visitNewClass(JCNewClass that)           { visitTree(that); }
        public void visitNewArray(JCNewArray that)           { visitTree(that); }
        public void visitParens(JCParens that)               { visitTree(that); }
        public void visitAssign(JCAssign that)               { visitTree(that); }
        public void visitWhere(JCWhere that)                 { visitTree(that); }
        public void visitFor(JCFor that)                 { visitTree(that); }
        public void visitSelectCond(JCSelectCond that)                 { visitTree(that); }
        public void visitSelectExp(JCSelect that)            { visitTree(that); }
        public void visitCaseExp(JCCaseExp that)            { visitTree(that); }
        public void visitAssignop(JCAssignOp that)           { visitTree(that); }
        public void visitUnary(JCUnary that)                 { visitTree(that); }
        public void visitBinary(JCBinary that)               { visitTree(that); }
        public void visitJoin(JCJoinDomains that)               { visitTree(that); }
        public void visitSequence(JCSequence that)               { visitTree(that); }
        public void visitTypeCast(JCTypeCast that)           { visitTree(that); }
        public void visitTypeTest(JCInstanceOf that)         { visitTree(that); }
        public void visitIndexed(JCArrayAccess that)         { visitTree(that); }
        public void visitSelect(JCFieldAccess that)          { visitTree(that); }
        public void visitIdent(JCIdent that)                 { visitTree(that); }
        public void visitSizeOf(JCSizeOf that)                 { visitTree(that); }
        public void visitLiteral(JCLiteral that)             { visitTree(that); }
        public void visitTypeIdent(JCPrimitiveTypeTree that) { visitTree(that); }
        public void visitTypeArray(JCArrayTypeTree that)     { visitTree(that); }
        public void visitTypeApply(JCTypeApply that)         { visitTree(that); }
        public void visitTypeParameter(JCTypeParameter that) { visitTree(that); }
//ALEX
        public void visitDomParameter(JCDomParameter that)   { visitTree(that); }
        public void visitCTProperty(JCCTProperty that)   { visitTree(that); }
        public void visitDomInstance(JCDomInstance that)   { visitTree(that); }
        public void visitDomParent(JCDomParent that)   { visitTree(that); }
        public void visitDomUsage(JCDomUsage that)   { visitTree(that); }
        public void visitDomConstraintValue(JCDomConstraintValue that)   { visitTree(that); }
        public void visitDomConstraint(JCDomConstraint that)   { visitTree(that); }

        public void visitWildcard(JCWildcard that)           { visitTree(that); }
        public void visitTypeBoundKind(TypeBoundKind that)   { visitTree(that); }
        public void visitAnnotation(JCAnnotation that)       { visitTree(that); }
        public void visitModifiers(JCModifiers that)         { visitTree(that); }
        public void visitErroneous(JCErroneous that)         { visitTree(that); }
        public void visitLetExpr(LetExpr that)               { visitTree(that); }

        public void visitTree(JCTree that)                   { assert false; }
    }

}
