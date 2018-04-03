
package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.TypeTags.*;
import com.sun.tools.javac.main.JavaCompiler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import java.util.Hashtable;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;

import org.jgrapht.*;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;


/** extends the type system to handle linearity (2 passes)
*/

public class Linear extends TreeScanner {
    protected static final Context.Key<Linear> LinearKey =
        new Context.Key<Linear>();

    private final Names names;
    private final Log log;
    private final Symtab syms;
    private final Types types;
    private final Check chk;
    private       TreeMaker make;
    private       Lint lint;
    private Work work;
    private Alias alias;

    private BaseSymbol base;

	boolean arg_out = false;
	boolean escapes = false;

	boolean inside_select=false;

	private VarSymbol whereSymbol = null;
	private JCWhere whereExp =null;

	boolean insideCondition=false;
	boolean insideApply=false;


    JCMethodDecl method = null;

	Map<VarSymbol,Integer> refList;
	//Map<VarSymbol,Set<List<JCExpression>>> indexRefs;

    boolean enableRef;

	//these are valid for the current branch:
	Map<VarSymbol,Set<List<JCExpression>>> alive_vars = null; //(sub-)objs that leave method (via retval or out arg
	Map<VarSymbol,Set<List<JCExpression>>> where_update = null; //(sub-)objs that are over written
	Map<VarSymbol,Set<List<JCExpression>>> update_vars = null; //(sub-)objs that are referenced
	Map<VarSymbol,Set<List<JCExpression>>> non_linear = null;
	//Set<VarSymbol> loc_vars = null;

	void putMap(Map<VarSymbol,Set<List<JCExpression>>> targetmap,Symbol s,List<JCExpression> index)
	{
		Set<List<JCTree.JCExpression>> access=targetmap.get((VarSymbol)s);
		if(access==null)
			access=new LinkedHashSet<List<JCExpression>>();
		access.add(index);
		targetmap.put((VarSymbol)s,access);
	}

	void accessSymbol(Map<VarSymbol,Set<List<JCExpression>>> targetmap,JCExpression tree)
	{

		Symbol s=TreeInfo.symbol(tree);
		Symbol original=s;

		//we only care about linear objects that are non-primitive
		if(inside_select||s==null||s.isStatic())
			return;

		Symbol thisSym;

		if(whereExp!=null)
			thisSym=TreeInfo.symbol(whereExp);
		else
			thisSym=((ClassSymbol)method.sym.owner).thisSym;

		List<JCExpression> index=null;
		if(tree.getTag()==JCTree.INDEXED)
		{
			s=TreeInfo.rootSymbol(tree);
			if(s.owner==method.sym.owner&&s!=thisSym)
			{
				s=thisSym;
				index=List.of((JCExpression)make.at(tree.pos).Select(make.at(tree.pos).Ident(thisSym), tree));
			}
			else
				index=List.of(tree);

		}
		else if(tree.getTag()==JCTree.SELECT)
		{
			s=TreeInfo.rootSymbol(tree);
			if(s.owner==method.sym.owner&&s!=thisSym)
			{
				s=thisSym;
				index=List.of((JCExpression)make.at(tree.pos).Select(make.at(tree.pos).Ident(thisSym), tree));
			}
			else
				index=List.of(tree);
		}
		else if(tree.getTag()==JCTree.IDENT)
		{
			s=TreeInfo.symbol(tree);
			if(s.owner==method.sym.owner&&s!=thisSym)
			{
				//prepend this. if necesssary
				s=thisSym;
				index=List.of((JCExpression)make.at(tree.pos).Select(make.at(tree.pos).Ident(thisSym), tree));
			}
		}
		else
			s=TreeInfo.symbol(tree);

		if(index!=null)
			TreeInfo.setSymbol(index.head, original);
		accessSymbol(targetmap,s,index,tree);
	}

	boolean isUnique(Symbol s,List<JCExpression> index)
	{
		if(index!=null)
			s=TreeInfo.symbol(index.head);
		return !(!(s.type.isLinear()&&!s.type.isReadLinear())||s.type.isPrimitive()||s.type.isSingular());
	}

	void accessSymbol(Map<VarSymbol,Set<List<JCExpression>>> targetmap,Symbol s,List<JCExpression> index,JCExpression tree)
	{
		if(insideCondition&&!insideApply)
			return;

		if(!(s instanceof VarSymbol))
			return;

		if(escapes)
		{
			putMap(targetmap,s,index);
			return;
		}

		if(arg_out||s.owner.type.isSingular())//there is no need to verify linearity for singular objects
			return;//update_vars only contains referenced vars

		//check for double refs which violate linearity:
		Set<List<JCTree.JCExpression>> access=update_vars.get(s);
		if(access!=null)
		{
			if(access.contains(null)||index==null)
			{
				if(isUnique(s,index))
					log.error(tree.pos, "must.be.linear", s);

				putMap(non_linear,s,index);
			}
			else
			{
				//poor man's contains:
				for(List<JCTree.JCExpression> item:access)
				{
					//we don't discriminate indices (we cannot always prove that indices are not ident)
					if(discardIndex(item.head).toString().equals(discardIndex(index.head).toString()))
					{
						putMap(non_linear,s,index);

						if(isUnique(s,index))
							log.error(tree.pos, "must.be.linear", discardIndex(index.head));

						break;
					}
				}
			}
		}

		putMap(update_vars,s,index);
		putMap(method.sym.update_vars,s,index);

		return;
	}


	JCTree discardIndex(JCTree cu) {
			class Replace extends TreeTranslator {

				public void scan(JCTree tree) {
					if (tree != null) {
						tree.accept(this);
					}
				}

				public void visitIndexed(JCArrayAccess tree) {
					result=translate(tree.indexed);
				}

			}
			Replace v = new Replace();
			cu=(new TreeCopier(make)).copy(cu);
			return v.translate(cu);
		}

	void checkUpdate(JCMethodDecl tree,int pos,Map<VarSymbol,Set<List<JCTree.JCExpression>>> where_update)
	{
		if(update_vars.keySet().size()>0)
		{
			String vals="";
			boolean first=true;

			Set<VarSymbol> keys=update_vars.keySet();

			keys.retainAll(alive_vars.keySet());
			for(VarSymbol vs:keys)
			{
				if(isUnique(vs,null))
				{
					Set<List<JCTree.JCExpression>> upd = new LinkedHashSet<List<JCExpression>>(update_vars.get(vs));

					if(where_update!=null&&where_update.get(vs)!=null)
					{
						Set<List<JCTree.JCExpression>> was_upd=where_update.get(vs);
						//really ugly removeAll, as equal is not impl for JCTree
						for(Iterator<List<JCTree.JCExpression>> inl=upd.iterator();inl.hasNext();)
						{
							List<JCTree.JCExpression> nl=inl.next();
							for(List<JCTree.JCExpression> ul:was_upd)
								if(nl==ul||(nl!=null&&ul!=null&&nl.toString().equals(ul.toString()))||ul==null)
									inl.remove();
						}
					}

					if(!upd.isEmpty())
					{
						for(List<JCTree.JCExpression> el:upd)
						{
							if(!first)
								vals+=", ";
							vals+=vs.toString()+"[";
							if(el!=null)
							{
								vals+=el.head.toFlatString();
							}
							vals+="]";
							first=false;
						}
					}
				}
			}
			if(vals.length()>0)
				log.error(pos,"must.write.used.linear","{" + vals + "}");
		}
	}

    public static Linear instance(Context context) {
        Linear instance = context.get(LinearKey);
        if (instance == null)
            instance = new Linear(context);
        return instance;
    }

    protected Linear(Context context) {
        context.put(LinearKey, this);

        names = Names.instance(context);
        log = Log.instance(context);
        syms = Symtab.instance(context);
        types = Types.instance(context);
        chk = Check.instance(context);
        lint = Lint.instance(context);
        //work = Work.instance(context);
        alias = Alias.instance(context);
    }

    /** The current class being defined.
     */
    JCClassDecl classDef;

	JCTree current_statment=null;

    /** Set when processing a loop body the second time for DU analysis. */
    Set<JCTree> done;


    /*-------------------- Environments ----------------------*/

    /*-------------------- Exceptions ----------------------*/

    /*-------------- Processing variables ----------------------*/

    /** Do we need to track init/uninit state of this symbol?
     *  I.e. is symbol either a local or a blank final variable?
     */
    boolean trackable(VarSymbol sym) {
        return
            (sym.owner.kind == MTH ||
             ((sym.flags() & (FINAL | HASINIT | PARAMETER)) == FINAL &&
              classDef.sym.isEnclosedBy((ClassSymbol)sym.owner)));
    }


    /*-------------------- Handling jumps ----------------------*/

    /** Split (duplicate) inits/uninits into WhenTrue/WhenFalse sets
     */
    void split() {
    }

    /** Merge (intersect) inits/uninits from WhenTrue/WhenFalse sets.
     */
    void merge() {
    }

/* ************************************************************************
 * Visitor methods for statements and definitions
 *************************************************************************/

    /** Analyze a definition.
     */
    void scanDef(JCTree tree) {
        scanStat(tree);
    }

    /** Analyze a statement. Check that statement is reachable.
     */
    void scanStat(JCTree tree) {
		current_statment=tree;
        scan(tree);
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

    public void visitClassDef(JCClassDecl tree) {
        if (tree.sym == null) return;

        JCClassDecl classDefPrev = classDef;
        Lint lintPrev = lint;


        classDef = tree;
        lint = lint.augment(tree.sym.attributes_field);

        try {

            // process all the static initializers
            for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                if (l.head.getTag() != JCTree.METHODDEF &&
                    (TreeInfo.flags(l.head) & STATIC) != 0) {
                    scanDef(l.head);
                }
            }

            // add intersection of all thrown clauses of initial constructors
            // to set of caught exceptions, unless class is anonymous.
            if (tree.name != names.empty) {
                boolean firstConstructor = true;
                for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                    if (TreeInfo.isInitialConstructor(l.head)) {
                        List<Type> mthrown =
                            ((JCMethodDecl) l.head).sym.type.getThrownTypes();
                        if (firstConstructor) {
                            firstConstructor = false;
                        } else {
                        }
                    }
                }
            }

            // process all the instance initializers
            for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                if (l.head.getTag() != JCTree.METHODDEF &&
                    (TreeInfo.flags(l.head) & STATIC) == 0) {
                    scanDef(l.head);
                }
            }

            // in an anonymous class, add the set of thrown exceptions to
            // the throws clause of the synthetic constructor and propagate
            // outwards.
            if (tree.name == names.empty) {
                for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                    if (TreeInfo.isInitialConstructor(l.head)) {
                        JCMethodDecl mdef = (JCMethodDecl)l.head;
                    }
                }
            }

            // process all the methods
            for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
                if (l.head.getTag() == JCTree.METHODDEF) {
                    scan(l.head);
                }
            }

        } finally {
            classDef = classDefPrev;
            lint = lintPrev;
        }
    }

	boolean SchedulerReachable(JCTree src, JCTree target)
	{
		while(src!=src.scheduler&&src!=target)
		{
			src=src.scheduler;
		}

		return src==target;
	}

    public void visitMethodDef(JCMethodDecl tree) {

        if(done.contains(tree)) return; //do fun only once per pass
        done.add(tree);

        if (!tree.analyse()) return;

        Map<VarSymbol,Integer> refListPrev=refList;
        boolean enableRefPrev=enableRef;

		alive_vars=new LinkedHashMap<VarSymbol, Set<List<JCExpression>>>();
		where_update=new LinkedHashMap<VarSymbol, Set<List<JCExpression>>>();
		update_vars=new LinkedHashMap<VarSymbol, Set<List<JCExpression>>>();
		non_linear=new LinkedHashMap<VarSymbol, Set<List<JCExpression>>>();
		//loc_vars=new LinkedHashSet<VarSymbol>();

        refList=new LinkedHashMap<VarSymbol,Integer>();
        enableRef=false;

        Lint lintPrev = lint;

        JCMethodDecl prevMethod = method;
        method = tree; //store current method


		//handle read linear args, prepare additional dependencies
		for(JCVariableDecl vd:method.params)
		{
			if(vd.type.isReadLinear())
			{
				Set<Arc> out=tree.depGraph.outgoingEdgesOf(method);
				for(Arc a:out)
				{
					if(a.v==vd.sym)
					{
						for(JCTree exit:method.exits)
						{
							if(SchedulerReachable(a.t.scheduler,exit.scheduler))
							{
								Set<JCTree> old=method.readlinear.get(exit);
								if(old==null)
									old=new LinkedHashSet<JCTree>();
								old.add(a.t);
								method.readlinear.put(exit, old);
							}
						}
					}
				}
			}
		}

		//add additional dependencies due to read linear access
        //if(false)


        if(!method.readlinear.isEmpty())
        {
            method.topolNodes = new java.util.LinkedHashMap<JCTree, Integer>();

            //construct Hasse Graph and build topol order list

            for (Iterator<JCTree> i = new TopologicalOrderIterator<JCTree, Arc>(tree.depGraph); i.hasNext();) {
                JCTree node = i.next();

                //assign unique topol ordered id for shortest paths
                method.topolNodes.put(node, method.topolNodes.size());
            }

            for(JCTree cast:method.readlinear.keySet())
            {
                Set<JCTree> srcs=method.readlinear.get(cast);

                //add dep from anything reachable by src to cast

                for(JCTree src:srcs)
                {

                    Set<JCTree> nodes=new LinkedHashSet<JCTree>();
                    for (Iterator<JCTree> i = new DepthFirstIterator<JCTree, Arc>(tree.depGraph,src); i.hasNext();) {
                        JCTree node=i.next();
                        if(node!=src&&node.scheduler==cast.scheduler&&src.getDGNode().IsReachable(false,node,tree.topolNodes,tree.depGraph,true)
                          &&!cast.getDGNode().IsReachable(false,node,tree.topolNodes,tree.depGraph,true))
                           nodes.add(node);


                    }

                    for(JCTree node:nodes)
                    {
                        VarSymbol linearReadSym=new VarSymbol(0,names.fromString("volatile ["+(cast).toString()+"]"),syms.intType,method.sym);
                        tree.depGraph.removeEdge(node, method.dg_end);
                        tree.depGraph.addEdge(node, cast,new Arc(node,cast,linearReadSym));
                        method.generated.put(linearReadSym, nodes);
                    }
                }
            }
            method.topolNodes=null;
        }
        
        
        Set<JCTree> volatileDeps=new LinkedHashSet<JCTree>();
                
        //if(false) //FIXME: breakes ubintree
        for(Arc a:tree.depGraph.incomingEdgesOf(method.dg_end))
        {
            if(a.v==null)
            {
                JCTree t=a.s;
                for(VarSymbol vs:JCTree.usedVars(t))
                    if((vs.flags()&Flags.LINEARREAD)!=0)
                    {
                        volatileDeps.add(t);
                        break;
                    }
            }
        }
        
        for(JCTree src:volatileDeps)
        {
            VarSymbol linearReadSym=new VarSymbol(0,names.fromString("volatile ["+src+"]"),syms.intType,method.sym);
            tree.depGraph.removeEdge(src, method.dg_end);
            tree.depGraph.addEdge(src, method.dg_end,new Arc(src, method.dg_end,linearReadSym));                            
        }

        lint = lint.augment(tree.sym.attributes_field);

		if((tree.sym.flags_field&Flags.BLOCKING)!=0)
		{
			for(JCVariableDecl vd:tree.params)
				if((vd.type.type_flags_field&Flags.LINEARREAD)!=0)
					log.error(tree.pos,"blocking.linear.read",tree,vd);
		}

        //first run (all not yet evaluated fun calls will yield 0, must make second run)

        try {
            for (List<JCVariableDecl> l = tree.params; l.nonEmpty(); l = l.tail) {
                JCVariableDecl def = l.head;
                scan(def);
           }

            scanStat(tree.body);

			for(VarSymbol vs:method.local_vars)
			{
				if(non_linear.get(vs)!=null)
					vs.flags_field&=~LINEAR;
				else
					vs.flags_field|=LINEAR;
			}

			checkUpdate(tree,tree.pos,where_update);

        } finally {
            refList = refListPrev;
            enableRef = enableRefPrev;
            lint = lintPrev;
            method = prevMethod;
            //vars = varsPrev;
        }
    }

    void linearityRef(VarSymbol s,int pos)
    {
        if(enableRef&&whereSymbol!=s&&!arg_out&&!inside_select)//is it an arg of cur fun?
        {
            if(!refList.containsKey(s))
                refList.put(s,pos);
        }
    }

    void stopLinearity(boolean old)
    {
        enableRef = old;
    }

    boolean startLinearity()
    {
        boolean old = enableRef;

        if(!enableRef)
            refList.clear();

        enableRef = true; //don't flood the refList unnecessarily

        return old;
    }

    public void visitVarDef(JCVariableDecl tree) {
        boolean track = trackable(tree.sym);
        if (tree.init != null) {
            Lint lintPrev = lint;
            lint = lint.augment(tree.sym.attributes_field);
            try{
                //we only want to find refs used by tree.init, so clear old refs
				boolean old=startLinearity();

                scanExpr(tree.init);

				stopLinearity(old);
            } finally {
                lint = lintPrev;
            }
        }
    }

    public void visitBlock(JCBlock tree) {
        scanStats(tree.stats);
    }

    public void visitCaseExp(JCCaseExp tree) {
		boolean old=startLinearity();
		scanExpr(tree.exp);
		stopLinearity(old);

        boolean hasDefault = false;

        if(tree.list.head.stmnt!=null)
            scanStat(tree.list.head.stmnt);
        else
            scanExpr(tree.list.head.res);

        for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {

            JCSelectCond c = l.head;

            if (c.cond == null)
                hasDefault = true;
            else
                scanExpr(c.cond);

            if(c.stmnt!=null)
                scanStat(c.stmnt);
            else
                scanExpr(c.res);

        }

    }

    public void visitSelect(JCFieldAccess tree) {
		boolean old = startLinearity();
		//find highest var
		if(tree.sym instanceof VarSymbol)
		{
			boolean oldsel=inside_select;
			inside_select=true;
			scanExpr(tree.selected);
			inside_select=oldsel;

			linearityRef((VarSymbol)tree.sym,tree.pos);
			accessSymbol(alive_vars,tree);
		}
		else
			scanExpr(tree.selected);

		stopLinearity(old);

    }

    public void visitSelectExp(JCSelect tree) {
        boolean hasDefault = false;

        if(tree.list.head.stmnt!=null)
            scanStat(tree.list.head.stmnt);
        else
            scanExpr(tree.list.head.res);

        for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {

            JCSelectCond c = l.head;

            if (c.cond == null)
                hasDefault = true;
            else
                scanExpr(c.cond);

            if(c.stmnt!=null)
                scanStat(c.stmnt);
            else
                scanExpr(c.res);
        }

    }

	//create new sets in map
	static Map<VarSymbol,Set<List<JCExpression>>> deepCopy(Map<VarSymbol,Set<List<JCExpression>>> input)
	{
		Map<VarSymbol,Set<List<JCExpression>>> res=new LinkedHashMap<VarSymbol, Set<List<JCExpression>>>();
		for(VarSymbol vs:input.keySet())
		{
			res.put(vs, new LinkedHashSet<List<JCExpression>>(input.get(vs)));
		}
		return res;
	}

    public void visitIf(JCIf tree) {

		Map<VarSymbol,Set<List<JCExpression>>> where_update_before=deepCopy(where_update);
		Map<VarSymbol,Set<List<JCExpression>>> alive_vars_before=deepCopy(alive_vars);
		Map<VarSymbol,Set<List<JCExpression>>> update_vars_before=deepCopy(update_vars);

		boolean old=startLinearity();
		scanCond(tree.cond);
		stopLinearity(old);

        scanStat(tree.thenpart);

        if (tree.elsepart != null) {

			Map<VarSymbol,Set<List<JCExpression>>> where_update_after_then=deepCopy(where_update);
			Map<VarSymbol,Set<List<JCExpression>>> alive_vars_after_then=deepCopy(alive_vars);
			Map<VarSymbol,Set<List<JCExpression>>> update_vars_after_then=deepCopy(update_vars);

			where_update=where_update_before;
			alive_vars=alive_vars_before;
			update_vars=update_vars_before;

            scanStat(tree.elsepart);

			//where_update/alive_vars : after else
			//where_update_after_then/alive_vars_after_then : after then

			for(VarSymbol vs:where_update_after_then.keySet())
			{
				Set<List<JCExpression>> set=where_update.get(vs);
				if(set!=null)
					set.addAll(where_update_after_then.get(vs));
				else
					set=where_update_after_then.get(vs);

				where_update.put(vs, set);
			}

			for(VarSymbol vs:alive_vars_after_then.keySet())
			{
				Set<List<JCExpression>> set=alive_vars.get(vs);
				if(set!=null)
					set.addAll(alive_vars_after_then.get(vs));
				else
					set=alive_vars_after_then.get(vs);

				alive_vars.put(vs, set);
			}

			for(VarSymbol vs:update_vars_after_then.keySet())
			{
				Set<List<JCExpression>> set=update_vars.get(vs);
				if(set!=null)
					set.addAll(update_vars_after_then.get(vs));
				else
					set=update_vars_after_then.get(vs);

				update_vars.put(vs, set);
			}

        } else {
			//where_update/alive_vars : after then
        }

    }

    public void visitIfExp(JCIfExp tree) {
		boolean old=startLinearity();
        scanCond(tree.cond);
		stopLinearity(old);

        scanStat(tree.thenpart);
        if (tree.elsepart != null) {
            scanStat(tree.elsepart);
        } else {
        }
    }

    public void visitWhere(JCWhere tree) {
        scanExpr(tree.exp);

		whereSymbol=(VarSymbol)TreeInfo.symbol(tree.exp);
		whereExp=tree;

		boolean oldEscapes=escapes;
		escapes=false;

		if(tree.sexp!=null)
			scanExpr(tree.sexp);
		else
			scanStat(tree.body);

		escapes=oldEscapes;

		whereSymbol=null;
		whereExp=null;
    }


    public void visitReturn(JCReturn tree)
    {
		boolean old=startLinearity();

		escapes=true;
        scanExpr(tree.expr);
		escapes=false;

		stopLinearity(old);

		checkUpdate(method,tree.pos,where_update);

		if((tree.flags&(Flags.SYNCHRONIZED|tree.flags&Flags.FINAL))!=0)//no resume
		{
			//we're done in this branch
			where_update.clear();
			alive_vars.clear();
			update_vars.clear();
		}
    }

	public void visitExec(JCExpressionStatement tree) {
		boolean old=startLinearity();
        super.visitExec(tree);
		stopLinearity(old);
    }

    public void visitApply(JCMethodInvocation tree) {

		//include implicit "this" if not static
		boolean oldApply=insideApply;
		insideApply=true;
		Symbol ms=TreeInfo.symbol(tree.meth);
		if(tree.meth.getTag()==JCTree.IDENT&&!inside_select&&(ms.flags_field&Flags.STATIC)==0)
		{
			linearityRef(((ClassSymbol)ms.owner).thisSym,tree.pos);
			accessSymbol(alive_vars,((ClassSymbol)ms.owner).thisSym,null,tree);
		}

        scanExpr(tree.meth);

        MethodSymbol meth =(MethodSymbol)TreeInfo.symbol(tree.meth);
		Iterator<VarSymbol> i=meth.params().iterator();

		boolean old_arg_out=arg_out;

		VarSymbol s=null;
		for(JCExpression e:tree.args)
		{
			if(i.hasNext())//var args
				s=i.next();
			arg_out=((s.flags_field&Flags.FOUT)!=0);

			scanExpr(e);
		}

		arg_out=old_arg_out;
		insideApply=oldApply;
    }

    public void visitNewClass(JCNewClass tree) {
        scanExpr(tree.encl);
        scanExprs(tree.args);
       // scan(tree.def);
        try {
            // If the new class expression defines an anonymous class,
            // analysis of the anonymous constructor may encounter thrown
            // types which are unsubstituted type variables.
            // However, since the constructor's actual thrown types have
            // already been marked as thrown, it is safe to simply include
            // each of the constructor's formal thrown types in the set of
            // 'caught/declared to be thrown' types, for the duration of
            // the class def analysis.
            scan(tree.def);
        }
        finally {
        }
    }

    public void visitNewArray(JCNewArray tree) {
        scanExprs(tree.dims);
        scanExprs(tree.elems);
    }

    public void visitAssert(JCAssert tree) {
        scanCond(tree.cond);
        if (tree.detail != null) {
            scanExpr(tree.detail);
        }
    }

    public void visitAssign(JCAssign tree) {
        JCTree lhs = TreeInfo.skipParens(tree.lhs);

		boolean old_argout=arg_out;
		arg_out=true;
        scanExpr(lhs);
		arg_out=old_argout;

		boolean old=startLinearity();
        scanExpr(tree.rhs);
		stopLinearity(old);

		//where block with more than one statement..make sure that reads and writes are performed in proper order..
		if(whereExp!=null&&whereExp.body!=null&&((JCBlock)whereExp.body).stats.size()>1)
		{
			whereExp.writes.add(tree);

			//FIXME: still uses refList??

			//used to break cycles in where updates (all reads before writes):

			Set<VarSymbol> readSyms=new LinkedHashSet<VarSymbol>();
			for(VarSymbol vs:refList.keySet())
				if(vs.owner.type==whereSymbol.owner.type&&!vs.name.equals(names._this))
					readSyms.add(vs);
			whereExp.reads.put(tree, readSyms);
			current_statment.nop = true;
		}

		if(whereExp != null)
		{
			VarSymbol vs=(VarSymbol)TreeInfo.symbol(tree.lhs);
			Symbol t=TreeInfo.symbol(whereExp);
			if(t==null||(vs.enclClass()!=t.enclClass()&&vs.enclClass()!=t.enclClass().getSuperclass().tsym)
					&&
			  (vs.flags_field&Flags.FOUT)==0)
				log.error(tree.pos, "invalid.where.target", tree.lhs,whereExp);

			escapes=true; //doesn't actually escape, just forces update into where_update
			arg_out=true; //doesn't matter here
			accessSymbol(where_update,tree.lhs); //reuse logic from accessSymbol to update where_update
			arg_out=old_argout;
			escapes=false;
		}
    }

	public void visitDomIter(JCDomainIter tree)
	{
		boolean old=startLinearity();
		//scanExpr(tree.exp); //only references domain, not actual var!!
		if(tree.body!=null)
			scanExpr(tree.body);
		else
			scanStat(tree.sbody);
		stopLinearity(old);
	}

	public void visitArgExpression(JCArgExpression tree) {

		if(tree.exp2==null)
		{
			boolean old=startLinearity();
			scanExpr(tree.exp1);
			stopLinearity(old);
		}
		else
		{
			JCExpression lhs = tree.exp1;
			JCExpression rhs = tree.exp2;

			if (tree.exp2==null) scanExpr(lhs);

			boolean old=startLinearity();
			scanExpr(rhs);
			stopLinearity(old);
		}

	}

    public void visitAssignop(JCAssignOp tree) {

        scanExpr(tree.lhs);
		boolean old=startLinearity();
        scanExpr(tree.rhs);
		stopLinearity(old);
    }

    public void visitUnary(JCUnary tree) {
        switch (tree.getTag()) {
        case JCTree.NOT:
            scanCond(tree.arg);

            break;
        case JCTree.PREINC: case JCTree.POSTINC:
        case JCTree.PREDEC: case JCTree.POSTDEC:
			boolean old=startLinearity();
            scanExpr(tree.arg);
			stopLinearity(old);
            break;
        default:
            scanExpr(tree.arg);
        }
    }

    public void visitBinary(JCBinary tree) {
		boolean oldApply=insideApply;
		insideApply=false;
		boolean oldCondition=insideCondition;
		insideCondition=true;

        switch (tree.getTag()) {
        case JCTree.AND:
            scanCond(tree.lhs);
            scanCond(tree.rhs);
            break;
        case JCTree.OR:
            scanCond(tree.lhs);
            scanCond(tree.rhs);
            break;
        default:
            scanExpr(tree.lhs);
            scanExpr(tree.rhs);
        }

		insideApply=oldApply;
		insideCondition=oldCondition;
    }

	public void visitIndexed(JCArrayAccess tree) {
		if(((Type.ArrayType)tree.indexed.type.getArrayType()).elemtype.isLinear())
		{
			VarSymbol vs=(VarSymbol)TreeInfo.symbol(tree.indexed);
			//USE update_vars instead of linRef for lin??
			linearityRef(vs,tree.getPreferredPosition()); //FIXME: precision!! (x.val == this.val)
			accessSymbol(alive_vars,tree);
		}

		for(JCExpression e:tree.index)
			scanExpr(e);

	}

    public void visitIdent(JCIdent tree) {
        if (tree.sym.kind == VAR)
        {
            linearityRef((VarSymbol)tree.sym,tree.getPreferredPosition());
			accessSymbol(alive_vars,tree);
        }
    }

    public void visitTopLevel(JCCompilationUnit tree) {
        // Do nothing for TopLevel since each class is visited individually
    }

/**************************************************************************
 * main method
 *************************************************************************/

    /** Perform definite assignment/unassignment analysis on a tree.
     */
    public void analyzeTree(JCTree tree, TreeMaker make) {
        try {
            this.make = make;
            base=new BaseSymbol();

            done=new LinkedHashSet<JCTree>();

            //for(int p=0;p<2;p++)
            {
                done.clear();

                enableRef = false;

                refList = new LinkedHashMap<VarSymbol,Integer>();

                this.classDef = null;

                scan(tree);
            }
        } finally {
            // note that recursive invocations of this method fail hard
            refList = null;
            this.make = null;
            this.classDef = null;
        }
    }
}
