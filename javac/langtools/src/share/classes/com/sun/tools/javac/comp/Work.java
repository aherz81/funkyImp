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

//todo: one might eliminate uninits.andSets when monotonic

package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
//import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.jvm.Code;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.TypeTags.*;
import com.sun.tools.javac.main.JavaCompiler;
import java.io.File;
import java.util.LinkedHashMap;

import java.util.Hashtable;
import java.util.Set;
import java.util.Properties;
import java.util.Iterator;
import java.util.Map;

import org.jgrapht.*;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;


//FIXME: domains,SSE
//FIXME: ref vs. value

/** This pass estimates the average work and mem accesses for each statement/method
 * Basically we're guessing all values which are statically unknown and use average instructions times from the config file
 */
public class Work extends TreeScanner {
    protected static final Context.Key<Work> workKey =
        new Context.Key<Work>();

	private final int recursiveLoop = 1000; //how often is a (non tail recursive) loop iterated
	private final int recursiveTailLoop = 10000; //how often is a tail recursive loop iterated
	private final int inlineClocks = 100; //we assume that funs which require less than this work are inlined
	private final int dynamicArrayCard = 1000; //size of dynamic array
	private final float cachedAccessRatio = 0.8f; //ratio of mem accesses which are cache hits (do not read/write to mem)

    private final Names names;
    private final Log log;
    private final Symtab syms;
    private final Types types;
    private final Check chk;
    private       TreeMaker make;
    private       Lint lint;

	private boolean includeBranch=false;

    private Code code;

    float workSum=0;
	float memSum=0;

	//for data parallel stuff (so we can check for off loading via MPI)
	float maxMem=0;
	int maxTask=0;

	float reg_size=8;//default 64 bit
	float sse_bank_size=4;

    boolean cacheStmtEstimates=false;
	boolean atomic_operation=false;
	boolean fixStmtEstimates=false;
	boolean sse=false;

	boolean transitive=true;

	JCMethodDecl method=null;
	String curClass="";

	public void fixEstimates()
	{
		fixStmtEstimates=true;
	}

	Properties configFile=null;
	JavaCompiler.Target target;

    public static Work instance(Context context) {
        Work instance = context.get(workKey);
        if (instance == null)
            instance = new Work(context);
        return instance;
    }

	class ProfileData
	{
		public float work;
		public float dev;
		public float est;
		public int samples;

		public ProfileData(float work, float dev, int samples)
		{
			this.work=work;//work correction per stmt
			this.dev=dev;
			this.samples=samples;
		}
	}

	java.util.Map<Pair<String,Integer>,ProfileData> workCorrection=null;

    protected Work(Context context) {
        context.put(workKey, this);

        names = Names.instance(context);
        log = Log.instance(context);
        syms = Symtab.instance(context);
        types = Types.instance(context);
        chk = Check.instance(context);
        lint = Lint.instance(context);

		JavaCompiler jc=JavaCompiler.instance(context);
		configFile = jc.configFile;
		target = jc.target;
		reg_size=getConfig("REG_SIZE",64)/8;//reg size in bytes
		sse_bank_size=getConfig("SSE_BANK_SIZE",4);//reg size in bytes

		if(jc.profileData!=null)
		{
			try
			{
				workCorrection=new java.util.LinkedHashMap<Pair<String,Integer>,ProfileData>();

				String tasklist=jc.profileData.getProperty("TASKS");
				String runs=jc.profileData.getProperty("RUNS");

				String[] tasks= tasklist.split(",");

				for(String task:tasks)
				{
					String _class=jc.profileData.getProperty(task+"_DESC");
					String stmtlist=jc.profileData.getProperty(task+"_STMTS");
					String[] stmts= stmtlist.split(",");

					float workcor=(Float.parseFloat(jc.profileData.getProperty(task+"_MES"))/Float.parseFloat(jc.profileData.getProperty(task+"_EST")));
					float oldcor=Float.parseFloat(jc.profileData.getProperty(task+"_COR"));
					float stddev=Float.parseFloat(jc.profileData.getProperty(task+"_STDDEV"));
					int samples=Integer.parseInt(jc.profileData.getProperty(task+"_SAMPLES"));

					ProfileData data=new ProfileData(workcor*oldcor,stddev,samples);

					for(String stmt:stmts)
						workCorrection.put(new Pair<String,Integer>(_class,Integer.parseInt(stmt)), data);
				}
			}
			catch(Exception e)
			{
				//FIXME: output that file is corrupted
				workCorrection=null;
				log.warning("profile.corrupt", e.getLocalizedMessage());
			}

		}
        //code = Code.
    }

	float getCorrection(String _class,int id)
	{
		if(workCorrection!=null)
		{
			ProfileData pd=workCorrection.get(new Pair<String,Integer>(_class,id));
			if(pd!=null)
			{
				return pd.work;
			}
			else
				return 1.f;
		}
		else
			return 1.f;
	}


	float getConfig(String name,float default_value)
	{
		if(configFile==null)
			return default_value;
		String prop=configFile.getProperty(name);
		if(prop==null)
			return default_value;
		else
		{
			try {
				return Float.parseFloat(prop);
			} catch (NumberFormatException e)
			{
				//FIXME: error
				return default_value;
			}
		}
	}

	int getConfig(String name,int default_value)
	{
		if(configFile==null)
			return default_value;
		String prop=configFile.getProperty(name);
		if(prop==null)
			return default_value;
		else
		{
			try {
				return Integer.parseInt(prop);
			} catch (NumberFormatException e)
			{
				//FIXME: error
				return default_value;
			}
		}
	}

    public float getWork(JCTree t,JCMethodDecl method)
    {
		curClass=method.sym.owner.name.toString();
        float res= analyzeTree(t,false);
		curClass="";
		return res;
        //return 2;
    }

    public float getWorkEntry(TaskSet path, JCMethodDecl method)
    {
		curClass=method.sym.owner.name.toString();
		transitive=false;
        float res=getWorkAll(path,method);
		transitive=true;
		curClass="";
		return res;
        //return 2;
    }

    public float getWorkMergePath(java.util.List<JCTree> path,JCMethodDecl method)
    {
		float work=0.f;
		for(JCTree t:path)
			work+=getWork(t,method);
        return work;
    }

    public float getWork(Set<JCTree> nodes,JCMethodDecl method)
    {
        //return the actual amount of computation performed by this path
        float work=0;

        for(Iterator<JCTree> ni=nodes.iterator();ni.hasNext();)
            work+=getWork(ni.next(),method);

        return work;
    }

    public float getWorkSet(Set<TaskSet> nodes,JCMethodDecl method)
    {
        //return the actual amount of computation performed by this path
        float work=0;

        for(Set<JCTree> TS:nodes)
            work+=getWork(TS,method);

        return work;
    }

	//get average path correction
    public float getCorrection(iTask path,JCMethodDecl method)
	{
		String tclass=method.sym.owner.name.toString();
        Set<JCTree> nodes=path.getCalcNodes();

		float correction=0.f;

	    for(JCTree t:nodes)
			correction+=getCorrection(tclass,t.pos);

		return correction/nodes.size();
	}

    public float getWork(iTask path,JCMethodDecl method)
    {
        //return the actual amount of computation performed by this path
		workSum=0;

        Set<JCTree> nodes=path.getCalcNodes();

        return getWork(nodes,method);
    }

    public float getWorkAll(TaskSet path,JCMethodDecl method)
    {
        //return the actual amount of computation performed by this path
		float res=0;

		for(JCTree t:path)
			res+=analyzeTree(t,true);

		return res;
    }

    public float getMem(JCTree t,JCMethodDecl method)
    {
        analyzeTree(t,false);
		return memSum;
        //return 2;
    }

    //get work of all nodes except start and end
    public float getMemMergePath(java.util.List<JCTree> path,JCMethodDecl method)
    {
		memSum=0;
        if(path.size()<=1)
            return 0.f;


        int mem=0;
        Iterator<JCTree> i=path.iterator();
        for(i.next();i.hasNext();)
        {
            JCTree t=i.next();
            if(i.hasNext())
                mem+=getMem(t,method);
        }

        return mem;
    }

    public float getMem(Set<JCTree> nodes,JCMethodDecl method)
    {
        //return the actual amount of computation performed by this path
        int mem=0;

        for(Iterator<JCTree> ni=nodes.iterator();ni.hasNext();)
            mem+=getMem(ni.next(),method);

        return mem;
    }

    public float getMem(iTask path,JCMethodDecl method)
    {
        //return the actual amount of computation performed by this path
		memSum=0;

        Set<JCTree> nodes=path.getCalcNodes();

        return getMem(nodes,method);
    }

/* ************************************************************************
 * Visitor methods for statements and definitions
 *************************************************************************/

    /** Analyze a definition.
     */
    void scanDef(JCTree tree) {
        //scanStat(tree);
    }

    /** Analyze a statement. Check that statement is reachable.
     */
    void scanStat(JCTree tree) {

        JCStatement stmt=(JCStatement)tree;
        if(!fixStmtEstimates&&(stmt.EstimatedWork==0||cacheStmtEstimates))
        {
            float oldWork=workSum;
			float oldMem=memSum;

			workSum=0;
			memSum=0;

			atomic_operation=false;

            scan(tree);

			if(!atomic_operation&&cacheStmtEstimates)
			{
				stmt.EstimatedWork=workSum;
				stmt.EstimatedMem=memSum;
			}
			else
			{
				stmt.EstimatedWork=0;
				stmt.EstimatedMem=0;
			}

			workSum+=oldWork;
			memSum+=oldMem;
        }
        else
		{
            workSum+=stmt.EstimatedWork;
			workSum*=getCorrection(curClass,tree.pos);
			memSum+=stmt.EstimatedMem;
		}
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

    public void visitMethodDef(JCMethodDecl tree) {
        if (!tree.analyse()) return;
		method=tree;
		curClass=method.sym.owner.name.toString();

		if(fixStmtEstimates&&transitive)
		{
			workSum=method.sym.EstimatedWork;
			memSum=method.sym.EstimatedMem;
			return;
		}

		boolean inlined=((fixStmtEstimates||cacheStmtEstimates)&&method.sym.EstimatedWork<inlineClocks);

		if(!inlined)
		{
			workSum+=getConfig("PUSH",2.0f);//create stack frame
			workSum+=getConfig("SUB",2.0f);//create stack frame
			workSum+=getConfig("MOV",2.0f);//create stack frame
		}

		if(transitive)
		{
			float oldMem=memSum;
			float oldWork=workSum;

			includeBranch=true;
			scan(tree.body);
			includeBranch=false;

			float transWork=workSum;
			float transMem=memSum;

			memSum=oldMem;
			workSum=oldWork;
			scan(tree.body);
			memSum=transMem;
			workSum=transWork;

			if(!inlined)
				workSum+=getConfig("RET",2.0f);//create stack frame
		}

		curClass="";
		method=null;
    }

	private float getMemAccess(Type t)
	{
		return (1.f-cachedAccessRatio)*Code.width(t);
	}

    public void visitVarDef(JCVariableDecl tree)
    {

        //assign:
		if(tree.init!=null)
		{
			if (!(tree.init instanceof JCIdent))
			{
				float oldWork=workSum;
		        scanExpr(tree.init);
				if(workSum!=oldWork) //did anything happen?
				{
					float size=getMemAccess(tree.sym.type);
					workSum+=Math.max(1, size/reg_size)*getConfig("MOV",2.0f);//FIXME arrays??
					memSum+=Math.max(1, size);
				}
				else
					workSum+=0.01f;
			}

			workSum+=tree.sym.work;
		}
	}

    public void visitBlock(JCBlock tree) {
        scanStats(tree.stats);
    }

    public void visitWhere(JCWhere tree) {
		if(tree.atomic!=null)
		{
			memSum+=getMemAccess(tree.exp.type);
			switch(tree.atomic.type)
			{
				case POSTINC:
					workSum+=getConfig("LOCKEDADD",11.0f);
					workSum+=getConfig("ADD",2.0f);	//calc post value
					break;
				case PREINC:
					workSum+=getConfig("LOCKEDADD",11.0f);
					break;
				case INC:
					workSum+=getConfig("LOCKEDSWAP",11.0f);
					break;
				case CMPXCHG:
					workSum+=getConfig("LOCKEDCMPXCHG",11.0f);
					break;
			}
		}
		else
		{
			scanExpr(tree.exp);
			if(tree.sexp!=null)
				scanExpr(tree.sexp);
			else
				scanStat(tree.body);
		}
    }

    public void visitSelect(JCFieldAccess tree) {

        workSum+=getConfig("MOV",2.0f);//deref
		memSum+=reg_size; // 32/64 bit?
        scanExpr(tree.selected);
    }

    public void visitDomIter(JCDomainIter tree) {
		float oldWorkSum=workSum;
		float oldMemSum=memSum;

		workSum=0;
		memSum=0;

		boolean oldsse=sse;
		if((tree.body!=null&&tree.body.type.isPrimitive()))
			sse=true; //for primitive types we assume vectorization
		scanExpr(tree.body);
		sse=oldsse;

		float deltaWork=workSum;
		float deltaMem=memSum;

		workSum+=oldWorkSum;
		memSum+=oldMemSum;

		Type.ArrayType at=(Type.ArrayType)tree.exp.type.getArrayType();

		int card;

		if(at.dom.appliedParams!=null&&!at.dom.isDynamic())
			card=at.dom.getCard(tree.pos(),at.dom.appliedParams.toArray(new JCExpression[0]),false);
		else
			card=dynamicArrayCard; //any way to get better values here???

		if(card==0)
			card=dynamicArrayCard;

		if((tree.body!=null&&tree.body.type.isPrimitive()))
			card/=sse_bank_size; //for primitive types we assume vectorization

		workSum+=card*deltaWork;
		memSum+=card*deltaMem;

		/*
		 * ok, this is tricky:
		 * data parallelism with bad comp/data size ratio (most stuff operating on primitive data)
		 * doesn't scale well because of the limited memory bandwith (imagine 4 cores reading and writing
		 * using SSE).
		 * In addition, all data parallelism is managed dynamically (tbb::auto_partitioner()).
		 * So by assuming that data parallel stuff is executed sequentially in the work estimation,
		 * we favor task parallelism over data parallelism (tasks may scale better).
		 *
		 * So we allocate threads and mem bandwidth for the tasks (for our estimates) and the data parallelism dynamically
		 * takes what is left.
		 */
	}

    public void visitSelectExp(JCSelect tree) {
        //FIXME: missing

        if(tree.list.head.stmnt!=null)
            scanStat(tree.list.head.stmnt);
        else
            scanExpr(tree.list.head.res);

        for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {
            JCSelectCond c = l.head;

            if (c.cond != null)
                scanExpr(c.cond);

            if(c.stmnt!=null)
                scanStat(c.stmnt);
            else
                scanExpr(c.res);
        }

    }

	//FIXME: visitArray iter

    public void visitIf(JCIf tree) {

        scanCond(tree.cond);

        workSum+=getConfig("CMP",1.0f);//branch taken?
        workSum+=getConfig("CJMP",2.0f);//branch taken?

        float oldWork=workSum;
		float oldMem=memSum;

		workSum=0;
		memSum=0;

        scanStat(tree.thenpart);//handled seperatly

		if(!includeBranch)
		{
			workSum=0;
			memSum=0;
		}

        if (tree.elsepart != null) {

            scanStat(tree.elsepart);

			if(!includeBranch)
			{
				workSum=0;
				memSum=0;
			}
			else
			{
				workSum=(2*workSum)/2.0f;
				memSum=(2*memSum)/2.0f;
			}

        }

		workSum+=getConfig("JMP",2.0f);//skip else part

		workSum+=oldWork;
		memSum+=oldMem;

        //dg_env.dg.cleanOldEnv(old_env);
    }

    public void visitIfExp(JCIfExp tree) {
        scanCond(tree.cond);

        float oldWork=workSum;
		workSum=0;
        scanExpr(tree.thenpart);
        float thenWork=workSum;
        if (tree.elsepart != null) {
            workSum=0;
            scanExpr(tree.elsepart);
            float elseWork=workSum;

            workSum=oldWork+(thenWork+ elseWork)/2.f;

        }
    }


    public void visitReturn(JCReturn tree) {

		boolean inlined=((fixStmtEstimates||cacheStmtEstimates)&&method.sym.EstimatedWork<inlineClocks);

		if (!inlined||!(tree.expr instanceof JCIdent))
			scanExpr(tree.expr);

		//if((tree.flags&Flags.FINAL)==0)


		if(!inlined)
		{
	        workSum+=getConfig("MOV",2.0f);//mov to ret reg
	        workSum+=getConfig("RET",2.0f);//
		}

        // if not initial constructor, should markDead instead of recordExit
    }

    public void visitThrow(JCThrow tree) {
        scanExpr(tree.expr);
    }
/*
    public void visitAgExpression(JCArgExpression tree) {
        scanExpr(tree.exp1);
        if(tree.exp2!=null)
            scanExpr(tree.exp2);
    }
*/

    public void visitApply(JCMethodInvocation tree) {

        //if(tree.)



		if(tree.meth.getTag()==JCTree.SELECT)
		{
	        scanExprs(tree.args);
			Symbol s=TreeInfo.symbol(((JCFieldAccess)tree.meth).selected);
			if(s!=null&&s.name.toString().equals("math"))
			{
				if(((JCFieldAccess)tree.meth).name.toString().equals("sqrt"))
				{
					memSum+=Math.max(reg_size,getMemAccess(tree.args.head.type));
					if(sse)
					{
						memSum+=Math.max(reg_size,getMemAccess(tree.args.head.type))*(sse_bank_size-1);
						workSum+=getConfig("SSE_SQRTA",2.0f)*sse_bank_size;//skip else part
					}
					else
					{
						workSum+=getConfig("FLD",2.0f);//skip else part
						workSum+=getConfig("FSQRT",30.0f);//skip else part
					}
					return;
				}
				if(((JCFieldAccess)tree.meth).name.toString().equals("sin"))
				{
					memSum+=Math.max(reg_size,getMemAccess(tree.args.head.type));
					workSum+=getConfig("FLD",2.0f);//skip else part
					workSum+=getConfig("FSIN",70.0f);//skip else part
					return;
				}
				if(((JCFieldAccess)tree.meth).name.toString().equals("cos"))
				{
					memSum+=Math.max(reg_size,getMemAccess(tree.args.head.type));
					workSum+=getConfig("FLD",2.0f);//skip else part
					workSum+=getConfig("FCOS",70.0f);//skip else part
					return;
				}
				if(((JCFieldAccess)tree.meth).name.toString().equals("abs"))
				{
					memSum+=Math.max(reg_size,getMemAccess(tree.args.head.type));
					workSum+=getConfig("FLD",2.0f);//skip else part
					workSum+=getConfig("FABS",1.0f);//skip else part
					return;
				}
				if(((JCFieldAccess)tree.meth).name.toString().equals("tan"))
				{
					memSum+=Math.max(reg_size,getMemAccess(tree.args.head.type));
					workSum+=getConfig("FLD",2.0f);//skip else part
					workSum+=getConfig("FTAN",115.0f);//skip else part
					return;
				}
				if(((JCFieldAccess)tree.meth).name.toString().equals("atan"))
				{
					memSum+=Math.max(reg_size,getMemAccess(tree.args.head.type));
					workSum+=getConfig("FLD",2.0f);//skip else part
					workSum+=getConfig("FATAN",12.0f);//skip else part
					return;
				}
			}
		}

		float oldMem=memSum;
		float oldWork=workSum;

		memSum=0;
		workSum=0;

        MethodSymbol meth = (MethodSymbol)TreeInfo.symbol(tree.meth);
		boolean is_loop=((meth.flags_field&Flags.LOOP)!=0);
		boolean inlined=((fixStmtEstimates||cacheStmtEstimates)&&meth.EstimatedWork<inlineClocks);

		if(!inlined)
		{
			scanExprs(tree.args);
			for(JCTree t:tree.args)//function call
			{
				memSum+=Math.max(reg_size,getMemAccess(t.type));//function call
				if(!is_loop)
					workSum+=getConfig("PUSH",2.0f);//skip else part
				else
					workSum+=getConfig("MOV",2.0f);//skip else part
			}

			if(!is_loop)
			{
				memSum+=2*reg_size;//safe some regs
				workSum+=getConfig("PUSH",2.0f)*2;//

				workSum+=getConfig("CALL",2.0f);//skip else part
			}
		}

        workSum+=meth.EstimatedWork;
		memSum+=meth.EstimatedMem;

		if(!inlined&&!is_loop)
		{
			workSum+=getConfig("SUB",2.0f);//fix esp

			memSum+=2*reg_size;//restore some regs
			workSum+=getConfig("POP",2.0f)*4;//
		}
        //scanExpr(tree.meth);

		//transitive:
		int factor=1;
		if(cacheStmtEstimates&&meth.mayBeRecursive)//well...we don't know much here
		{
			if((meth.flags_field&Flags.LOOP)!=0)
				factor=recursiveTailLoop;//ok it's a loop so it doesn't eat the stack, can be repeated many times without trouble
			else
				factor=recursiveLoop;//may eat stack, for performance reasons recursion depth should be limited
		}

		workSum=oldWork+(workSum)*(factor);
		memSum=oldMem+(memSum)*(factor);
    }

    public void visitNewClass(JCNewClass tree) {

        workSum+=getConfig("ALLOC_OVERHEAD",1000.0f);//call to malloc/new
		MethodSymbol meth = (MethodSymbol)tree.constructor;
        workSum+=meth.EstimatedWork;//call constructor

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
        if (!(lhs instanceof JCIdent)) scanExpr(lhs);

        float size=getMemAccess(lhs.type);

		//FIXME: SSE
		if (!(tree.rhs instanceof JCIdent))
		{
			float oldWork=workSum;

			scanExpr(tree.rhs);

			if(workSum!=oldWork)
			{
				workSum+=getConfig("MOV",2.0f)*Math.ceil(size/(float)reg_size);

				memSum+=2*getMemAccess(lhs.type); //read right, write left
			}
			else
				workSum+=0.01f;
		}

		if(TreeInfo.symbol(lhs)!=null)//??
			workSum+=TreeInfo.symbol(lhs).work;
    }

    public void visitAssignop(JCAssignOp tree) {
        scanExpr(tree.lhs);

        scanExpr(tree.rhs);

        float size=getMemAccess(tree.lhs.type);
        workSum+=getConfig("MOV",2.0f)*(Math.ceil(size/(float)reg_size));
		memSum+=2*getMemAccess(tree.lhs.type); //read right, write left
    }

    public void visitUnary(JCUnary tree) {
		memSum+=2*getMemAccess(tree.type); //read right, write left
        switch (tree.getTag()) {
        case JCTree.NOT:
            scanCond(tree.arg);
            workSum+=getConfig("NOT",2.0f);

            break;
        case JCTree.PREINC: case JCTree.POSTINC:
        case JCTree.PREDEC: case JCTree.POSTDEC:
            scanExpr(tree.arg);
            workSum+=getConfig("ADD",2.0f);
            break;
        default:
            scanExpr(tree.arg);
            workSum+=getConfig("NEG",2.0f);
        }
    }

    public void visitBinary(JCBinary tree) {
		//memSum+=getMemAccess(tree.lhs.type)+getMemAccess(tree.rhs.type); //2*read right

		boolean is_integer=(tree.lhs.type.tag==TypeTags.INT&tree.lhs.type.tag==TypeTags.INT);

        switch (tree.getTag()) {
        case JCTree.AND:
            scanCond(tree.lhs);
            scanCond(tree.rhs);
			if(sse)
			{
	            workSum+=getConfig("SSE_ANDA",2.0f);
				memSum+=reg_size*(sse_bank_size-1);
			}
			else
				workSum+=getConfig("AND",2.0f);
            break;
        case JCTree.OR:
            scanCond(tree.lhs);
            scanCond(tree.rhs);
			if(sse)
			{
	            workSum+=getConfig("SSE_ORA",2.0f);
				memSum+=reg_size*(sse_bank_size-1);
			}
			else
            workSum+=getConfig("OR",2.0f);
            break;
        case JCTree.PLUS:
        case JCTree.MINUS:
            scanExpr(tree.lhs);
            scanExpr(tree.rhs);
			if(sse)
			{
	            workSum+=getConfig("SSE_ADDA",2.0f);
				memSum+=reg_size*(sse_bank_size-1);
			}
			else
				workSum+=getConfig(is_integer? "ADD" : "FADD",2.0f);
            break;
        case JCTree.MUL:
            scanExpr(tree.lhs);
            scanExpr(tree.rhs);
			if(sse)
			{
	            workSum+=getConfig("SSE_MULA",2.0f);
				memSum+=reg_size*(sse_bank_size-1);
			}
			else
	            workSum+=getConfig(is_integer? "MUL" : "FMUL",3.0f);
            break;
        case JCTree.DIV:
            scanExpr(tree.lhs);
            scanExpr(tree.rhs);
			if(sse)
			{
	            workSum+=getConfig("SSE_DIVA",2.0f);
				memSum+=reg_size*(sse_bank_size-1);
			}
			else
	            workSum+=getConfig(is_integer? "DIV" : "FDIV",20.0f);
            break;
        default:
			//FIXME: float or int?
            scanExpr(tree.lhs);
            scanExpr(tree.rhs);
            workSum+=getConfig("CMP",20.0f);

        }
    }

    public Map<VarSymbol, Float> varMemConsumption = new LinkedHashMap<VarSymbol, Float>();
    public void visitIdent(JCIdent tree) {
		if((tree.sym.flags_field&Flags.ATOMIC)!=0)
		{
			atomic_operation=true;
		}

		/**
		 * Andreas Wagner: added to lookup memory consumption of individual variables
		 */
		if(tree.sym instanceof VarSymbol)
			varMemConsumption.put((VarSymbol)tree.sym, getMemAccess(tree.type));

		memSum+=getMemAccess(tree.type); //read ident
		if(tree.type.tag==TypeTags.FLOAT||tree.type.tag==TypeTags.DOUBLE)
			workSum+=getConfig("FLD",3.0f)+getConfig("FST",3.0f);
    }

    public void visitTypeCast(JCTypeCast tree) {
        super.visitTypeCast(tree);
        if (!tree.type.isErroneous()
            && lint.isEnabled(Lint.LintCategory.CAST)
            && types.isSameType(tree.expr.type, tree.clazz.type)) {
            log.warning(tree.pos(), "redundant.cast", tree.expr.type);
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
    public float analyzeTree(JCTree tree,boolean cacheStmtEstimates) {
        try {
//            this.make = make;
//            if(true)
//            return 2;

			  this.cacheStmtEstimates=cacheStmtEstimates;


//            inits = new Bits();
//            uninits = new Bits();

            workSum=0;
			memSum=0;
			if(tree instanceof JCStatement)
				scanStat(tree);
			else
				scan(tree);

            return workSum;
        } finally {
        }
    }
}
