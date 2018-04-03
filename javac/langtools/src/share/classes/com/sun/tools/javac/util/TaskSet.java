/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.util;

/**
 *
 * @author aherz
 */
import java.util.*;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Symbol;

import org.jgrapht.graph.*;

import com.sun.tools.javac.jvm.Code;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeInfo;
import org.jgrapht.traverse.TopologicalOrderIterator;


//a TaskSet is a task (and has no un-fullfilled self dependencies)

@SuppressWarnings("serial")
public class TaskSet extends
LinkedHashSet<JCTree> implements iTask //LinkedHashSet makes precond deterministic!!!!!
{
    private DirectedWeightedMultigraph<JCTree, Arc> depGraph; //task graph for method
	JCBlock code_block; //cached code
	JCMethodDecl method; //method this path belongs to

	@Override
	public int hashCode() {
		return super.hashCode(); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o); //To change body of generated methods, choose Tools | Templates.
	}

    private TaskSet()
    {}

    public TaskSet(DirectedWeightedMultigraph<JCTree, Arc> depGraph,JCMethodDecl method)
    {
        this.depGraph=depGraph;
		code_block=null;
		this.method=method;
    }

    public void removeNode(JCTree item)
    {
		this.remove(item);
    }

	public boolean isIndependentTo(TaskSet other)
	{
		Set<JCTree> cn1=getCalcNodes();
		Set<JCTree> cn2=other.getCalcNodes();

		if(cn1.isEmpty()||cn2.isEmpty())
			return false;

		JCTree lc1=getLastCalcNode(method, cn1);
		JCTree lc2=other.getLastCalcNode(method, cn2);

		if(
			lc1.getDGNode().IsReachable(false, lc2, method.topolNodes, method.depGraph, true)
			||
			lc2.getDGNode().IsReachable(false, lc1, method.topolNodes, method.depGraph, true)
			)
		return false;
		return true;
	}

	public JCTree getNode()
	{
		if(this.size()!=1)
			return null;
		return this.iterator().next();
	}

	public boolean isReachableFrom(TaskSet other)
	{
		Set<JCTree> cn1=getCalcNodes();
		Set<JCTree> cn2=other.getCalcNodes();

		if(cn1.isEmpty()||cn2.isEmpty())
			return false;

		JCTree lc1=getLastCalcNode(method, cn1);
		JCTree fc2=other.getFirstCalcNode(method, cn2);

		return fc2.getDGNode().IsReachable(false, lc1, method.topolNodes, method.depGraph, true);
	}

	public boolean isLastReachableFrom(TaskSet other)
	{
		Set<JCTree> cn1=getCalcNodes();
		Set<JCTree> cn2=other.getCalcNodes();

		if(cn1.isEmpty()||cn2.isEmpty())
			return false;

		JCTree lc1=getLastCalcNode(method, cn1);
		JCTree lc2=other.getLastCalcNode(method, cn2);

		return lc2.getDGNode().IsReachable(false, lc1, method.topolNodes, method.depGraph, true);
	}

	public boolean containsForcedSpawn()
	{
		return containsBlocking()||containsThread();
	}

	//do not merge blocking paths, might have unknown dependencies
	public boolean containsBlocking()
	{
		Set<JCTree> calc=getCalcNodes();

		for(JCTree t:calc)
			if(t.is_blocking)
				return true;

		return false;
	}

	//do not merge blocking paths, might have unknown dependencies
	public boolean containsReturn()
	{
		Set<JCTree> calc=getCalcNodes();

		for(JCTree t:calc)
			if(t.getTag()==JCTree.RETURN&&(((JCReturn)t).flags&Flags.FINAL)==0 )
				return true;

		return false;
	}

	public void removeThread(String thread)
	{
		Set<JCTree> calc=getCalcNodes();

		for(JCTree t:calc)
			t.threads.remove(thread);
	}

	public boolean containsThread()
	{
		Set<JCTree> calc=getCalcNodes();

		for(JCTree t:calc)
			if(t.threads.size()>0)
				return true;

		return false;
	}

	//FIXME: make this an additional pass (MPIPaths) which runs after all local tg widths have been collected
	public int getParallelTasks()
	{
		int tasks=method.sym.LocalTGWidth;
		Set<JCTree> calc=getCalcNodes();

		for(JCTree t:calc)
		{
			if(t.getTag()==JCTree.APPLY&&((MethodSymbol)TreeInfo.symbol(t)).LocalTGWidth>tasks)
				tasks=((MethodSymbol)TreeInfo.symbol(t)).LocalTGWidth;
		}

		return tasks;
	}

	public Set<String> getGroups()
	{
		Set<String> grouped= new LinkedHashSet<String>();
		Set<JCTree> calc=getCalcNodes();

		for(JCTree t:calc)
		{
			if(t.groups!=null&&!t.groups.isEmpty())
			{
				grouped.addAll(t.groups);
			}
		}

		return grouped;
	}

	public Set<String> getThreads()
	{
		Set<String> grouped= new LinkedHashSet<String>();
		Set<JCTree> calc=getCalcNodes();

		for(JCTree t:calc)
		{
			if(t.threads!=null&&!t.threads.isEmpty())
			{
				grouped.addAll(t.threads);
			}
		}

		return grouped;
	}

	//topol smallest calc node
	public JCTree getFirstCalcNode(JCMethodDecl method,Set<JCTree> cn)
	{
		if(cn.isEmpty())
			return getFirstFromPathSet();

		JCTree first=cn.iterator().next();
		for(JCTree n:cn)
			if(method.topolNodes.get(n)<method.topolNodes.get(first))
				first=n;

		return first;
	}

	//topol largest calc node
	public JCTree getLastCalcNode(JCMethodDecl method,Set<JCTree> cn)
	{
		if(cn.isEmpty())
			return getLastFromPathSet();

		JCTree last=cn.iterator().next();
		for(JCTree n:cn)
			if(method.topolNodes.get(n)>method.topolNodes.get(last))
				last=n;

		return last;
	}

    public boolean containsNode(JCTree node)
    {
		return this.contains(node);
    }

	//get topol smallest node (may be non-calc node)
    public JCTree getFirstFromPathSet()
    {
		JCTree first=null;
		int topolid=method.topolNodes.size();
		for(JCTree t:this)
			if(method.topolNodes.get(t)<topolid)
			{
				first=t;
				topolid=method.topolNodes.get(t);
			}
        return first;
    }

	//get topol largest node (may be non-calc node)
    public JCTree getLastFromPathSet()
    {
		JCTree last=null;
		int topolid=-1;
		for(JCTree t:this)
			if(method.topolNodes.get(t)>topolid)
			{
				last=t;
				topolid=method.topolNodes.get(t);
			}
        return last;
    }

	//this method must mirror the splitting in PathGen DFPP
    public boolean isCalcPathNode(JCTree node,boolean local_first,boolean last,boolean local_last)
    {
		return true;
    }

	//add another path set to this one
	public void add(TaskSet from) {
		this.addAll(from);
	}

	TaskSet createMultiNode(JCTree node) {
		TaskSet set = new TaskSet(method.depGraphImplicit, method);
		set.add(node);
		return set;
	}

    public Set<JCTree> getCalcNodes()
    {
		Set<JCTree> cn=new LinkedHashSet<JCTree>();
        //return the actual amount of computation performed by this path
		for(JCTree v:this)
		{
			if(v instanceof JCStatement)
			{
				cn.add((JCStatement)v);
			}
		}

		return cn;
    }

    public Set<JCTree> getCalcNodesTransitive()
    {
		Set<JCTree> cn=new LinkedHashSet<JCTree>();
        //return the actual amount of computation performed by this path
		for(JCTree v:this)
		{
			if(v instanceof JCStatement)
			{
				cn.add((JCStatement)v);
				if(v.isScheduler)
				{
					for(Arc a:method.depGraph.outgoingEdgesOf(v))
					{
						TaskSet target=createMultiNode(a.t);

                                                if(target.getNode()!=null&&target.getNode().getTag()==JCTree.CF&&method.hasseDiagram.outDegreeOf(target)==1)
						{
							TaskSet branch=method.hasseDiagram.getEdgeTarget(method.hasseDiagram.outgoingEdgesOf(target).iterator().next());
							cn.addAll(branch.getCalcNodesTransitive());
						}
					}
				}

			}
		}

		return cn;
    }
    
    public Set<JCTree> getCalcNodesTransitiveImplicit()
    {
		Set<JCTree> cn=new LinkedHashSet<JCTree>();
        //return the actual amount of computation performed by this path
		for(JCTree v:this)
		{
			if(v instanceof JCStatement)
			{
				cn.add((JCStatement)v);
				if(v.isScheduler)
				{
					for(Arc a:depGraph.outgoingEdgesOf(v))
					{
						TaskSet target=createMultiNode(a.t);

                                                if(target.getNode()!=null&&target.getNode().getTag()==JCTree.CF&&method.hasseDiagram.outDegreeOf(target)==1)
						{
							TaskSet branch=method.hasseDiagram.getEdgeTarget(method.hasseDiagram.outgoingEdgesOf(target).iterator().next());
							cn.addAll(branch.getCalcNodesTransitiveImplicit());
						}
					}
				}

			}
		}

		return cn;
    }    

    public String toString()
    {
		return getTreeList(false).toString("\\n").replaceAll("\\\"", "\\\\\"").replaceAll("\n", "\\n");
    }

    public List<JCTree> getTreeList(boolean calcOnly)
	{
		ArrayList<JCTree> list = new ArrayList<JCTree>();
		if(calcOnly)
			for(JCTree t:getCalcNodes())
					list.add(t);
		else
			for(JCTree t:this)
					list.add(t);

		//sort by decreasing topol id (highest first)
		Collections.sort(list, new Comparator<JCTree>() {
			public int compare(JCTree a1, JCTree a2) {
                            return method.topolNodes.get(a1) - method.topolNodes.get(a2);
			}
		});

		ListBuffer<JCTree> lb=new ListBuffer<JCTree>();

		for(JCTree t:list)
			lb.add(t);

        return lb.toList();
	}


	public JCBlock getPathBlockFromSet(Set<JCTree> set,final JCMethodDecl md)
	{
		ArrayList<JCStatement> list = new ArrayList<JCStatement>();
		for(JCTree t:set)
			if(t instanceof JCStatement)
				list.add((JCStatement)t);

		Collections.sort(list, new Comparator<JCTree>() {
			public int compare(JCTree a1, JCTree a2) {
				return md.orderNodes.get(a1) - md.orderNodes.get(a2);
			}
		});
        ListBuffer<JCStatement> lb=new ListBuffer<JCStatement>();

		lb.addAll(list);

        return new JCBlock(0,lb.toList());
	}

    public JCBlock getPathBlockNoCache()
	{
		ArrayList<JCStatement> list = new ArrayList<JCStatement>();
		for(JCTree t:getCalcNodes())
			if(t instanceof JCStatement)
				list.add((JCStatement)t);

		//sort by decreasing topol id (highest first)
		Collections.sort(list, new Comparator<JCTree>() {
			public int compare(JCTree a1, JCTree a2) {
				return method.topolNodes.get(a1) - method.topolNodes.get(a2);
			}
		});
        ListBuffer<JCStatement> lb=new ListBuffer<JCStatement>();

		lb.addAll(list);

        return new JCBlock(0,lb.toList());
	}

    public JCBlock getPathBlock()
    {
		//cache code_block!!
		if(false&&code_block!=null)
			return code_block;

        code_block = getPathBlockNoCache();

		return code_block;
    }

    public Set<JCTree> getInCom()
    {
		Set<JCTree> calcNodes=getCalcNodesTransitive();
        Set<JCTree> comDone=new LinkedHashSet<JCTree>();
		Set<VarSymbol> symDone=new LinkedHashSet<VarSymbol>();
        for(Iterator<JCTree> cni=calcNodes.iterator();cni.hasNext();)
        {
            JCTree v=cni.next();
            Set<Arc> in = method.depGraph.incomingEdgesOf(v);
            for(Iterator<Arc> ai=in.iterator();ai.hasNext();)
            {
                Arc e=ai.next();
                if(e.v!=null&&!calcNodes.contains(e.s)&&!symDone.contains(e.v)&&!e.s.nop_if_alone)
                {
                    comDone.add(e.s);//we com this item only once
					symDone.add(e.v);//we only fulfill each dependency once (may come from branches)
                }
            }
        }
        return comDone;
    }

    public Set<JCTree> getInComImplicit()
    {
		Set<JCTree> calcNodes=getCalcNodesTransitiveImplicit();
        Set<JCTree> comDone=new LinkedHashSet<JCTree>();
		Set<VarSymbol> symDone=new LinkedHashSet<VarSymbol>();

		Map<JCTree,Integer> schedulerToDeps=new LinkedHashMap<JCTree, Integer>();

        for(Iterator<JCTree> cni=calcNodes.iterator();cni.hasNext();)
        {
            JCTree v=cni.next();
            Set<Arc> in = depGraph.incomingEdgesOf(v);
            for(Iterator<Arc> ai=in.iterator();ai.hasNext();)
            {
                Arc e=ai.next();

                if(e.v!=null&&!calcNodes.contains(e.s)&&!e.s.nop_if_alone)
                {
					Integer deps=schedulerToDeps.get(e.s.scheduler);

					if(deps==null)
						deps=0;

					deps++;

					schedulerToDeps.put(e.s.scheduler, deps);// store deps per scheduler

					if(!symDone.contains(e.v))
					{
						comDone.add(e.s);//we com this item only once
						symDone.add(e.v);//we only fulfill each dependency once (may come from branches)
					}
                }
            }

			int comCount=comDone.size();

			//check that all schedulers have the same amount of deps, if not store diff so it is fixed in visitCF at codegen
			for(JCTree sched:schedulerToDeps.keySet())
			{
				if(sched.getTag()==JCTree.CF&&schedulerToDeps.get(sched)!=comCount)
				{
					((JCCF)sched).additionalRefs.put(this, comCount-schedulerToDeps.get(sched));
				}
			}

        }
        return comDone;
    }

    public Set<VarSymbol> getInSymbols(JCTree v)
    {
		Set<JCTree> calcNodes=getCalcNodesTransitive();
        Set<VarSymbol> comDone=new LinkedHashSet<VarSymbol>();
        Set<Arc> in = method.depGraph.incomingEdgesOf(v);
        for(Iterator<Arc> ai=in.iterator();ai.hasNext();)
        {
            Arc e=ai.next();
            if(!calcNodes.contains(e.s)&&calcNodes.contains(e.t)&&e.v!=null)
            {
                comDone.add(e.v);//we com this item only once
            }
        }
        return comDone;
    }

    public Set<VarSymbol> getInSymbols()
    {
		Set<JCTree> calcNodes=getCalcNodesTransitive();
        Set<VarSymbol> comDone=new LinkedHashSet<VarSymbol>();
		Set<JCTree> outcom=getInCom();

		for(JCTree v:outcom)
		{
			Set<Arc> in = method.depGraph.outgoingEdgesOf(v);
			for(Iterator<Arc> ai=in.iterator();ai.hasNext();)
			{
				Arc e=ai.next();
				if(!calcNodes.contains(e.s)&&calcNodes.contains(e.t)&&e.v!=null)
				{
					comDone.add(e.v);//we com this item only once
				}
			}
		}
        return comDone;
    }

    public Set<VarSymbol> getInSymbolsImplicit()
    {
		Set<JCTree> calcNodes=getCalcNodesTransitiveImplicit();
        Set<VarSymbol> comDone=new LinkedHashSet<VarSymbol>();
		Set<JCTree> outcom=getInCom();

		for(JCTree v:outcom)
		{
			Set<Arc> in = depGraph.outgoingEdgesOf(v);
			for(Iterator<Arc> ai=in.iterator();ai.hasNext();)
			{
				Arc e=ai.next();
				if(!calcNodes.contains(e.s)&&calcNodes.contains(e.t)&&e.v!=null)
				{
					comDone.add(e.v);//we com this item only once
				}
			}
		}
        return comDone;
    }

    public boolean isJoining()
    {
        if(containsReturn())
            return true;
        
		Set<JCTree> calcNodes=getCalcNodesTransitiveImplicit();
		if(method.final_value==null)
			return false;

        for(Iterator<JCTree> cni=calcNodes.iterator();cni.hasNext();)
        {
            JCTree v=cni.next();
            
            for(Arc a: depGraph.outgoingEdgesOf(v))
            {
                if(a.t==method.final_value)
                    return true;
            }
            
			//if(v.getDGNode().IsReachable(false, method.final_value, method.topolNodes, depGraph, true))
			//	return true;
        }
        return false;
    }

    public boolean isFinal()
    {
		Set<JCTree> calcNodes=getCalcNodesTransitiveImplicit();
        for(Iterator<JCTree> cni=calcNodes.iterator();cni.hasNext();)
        {
            JCTree v=cni.next();
            Set<Arc> out = depGraph.outgoingEdgesOf(v);
            for(Iterator<Arc> ai=out.iterator();ai.hasNext();)
            {
                Arc e=ai.next();
				if(e.t.getTag()==JCTree.SKIP)
					return true;
            }
        }
        return false;
    }

    public Set<JCTree> getOutCom()
    {
		Set<JCTree> calcNodes=getCalcNodesTransitive();
        Set<JCTree> comDone=new LinkedHashSet<JCTree>();
        for(Iterator<JCTree> cni=calcNodes.iterator();cni.hasNext();)
        {
            JCTree v=cni.next();
            Set<Arc> out = method.depGraph.outgoingEdgesOf(v);
            for(Iterator<Arc> ai=out.iterator();ai.hasNext();)
            {
                Arc e=ai.next();
                if(!calcNodes.contains(e.t))
                {
                    comDone.add(e.s);//we com this item only once
                }
            }
        }
        return comDone;
    }

	public Map<VarSymbol,JCTree> getOutComMap()
	{
		Set<JCTree> calcNodes=getCalcNodesTransitive();
		Map<VarSymbol,JCTree> source=new LinkedHashMap<VarSymbol, JCTree>();
        for(Iterator<JCTree> cni=calcNodes.iterator();cni.hasNext();)
        {
            JCTree v=cni.next();
            Set<Arc> out = method.depGraph.outgoingEdgesOf(v);
            for(Iterator<Arc> ai=out.iterator();ai.hasNext();)
            {
                Arc e=ai.next();
				if(e.v!=null)
				{
					JCTree oldsrc=source.get(e.v);
					if(oldsrc==null||method.topolNodes.get(e.s)>method.topolNodes.get(oldsrc))
						oldsrc=e.s;
					source.put(e.v,oldsrc);//store topol largest only!
				}
            }
        }

		return source;

	}

    public Set<JCTree> getTargets()
    {
		Set<JCTree> calcNodes=getCalcNodesTransitive();
        Set<JCTree> comDone=new LinkedHashSet<JCTree>();
        for(Iterator<JCTree> cni=calcNodes.iterator();cni.hasNext();)
        {
            JCTree v=cni.next();
            Set<Arc> out = method.depGraph.outgoingEdgesOf(v);
            for(Iterator<Arc> ai=out.iterator();ai.hasNext();)
            {
                Arc e=ai.next();
                if(!calcNodes.contains(e.t))
                {
                    comDone.add(e.t);//we com this item only once
                }
            }
        }
        return comDone;
    }

    public Set<VarSymbol> getOutSymbols(JCTree v)
    {
		Set<JCTree> calcNodes=getCalcNodesTransitive();
        Set<VarSymbol> comDone=new LinkedHashSet<VarSymbol>();

        Set<Arc> out = method.depGraph.outgoingEdgesOf(v);
        for(Iterator<Arc> ai=out.iterator();ai.hasNext();)
        {
            Arc e=ai.next();
            if(!calcNodes.contains(e.t)&&calcNodes.contains(e.s)&&e.v!=null)
            {
                comDone.add(e.v);//we com this item only once
            }
        }
        return comDone;
    }

	//com symbols without uninitialized vars (avoid unnecessray notifications)
    public Set<VarSymbol> getNullFreeOutSymbols()
    {
		Set<JCTree> calcNodes=getCalcNodesTransitive();
        Set<VarSymbol> comDone=new LinkedHashSet<VarSymbol>();

		Set<JCTree> outcom=getOutCom();

		for(JCTree v:outcom)
		{
			Set<Arc> out = method.depGraph.outgoingEdgesOf(v);
			for(Iterator<Arc> ai=out.iterator();ai.hasNext();)
			{
				Arc e=ai.next();
				if(!calcNodes.contains(e.t)&&calcNodes.contains(e.s)&&e.v!=null
						&&!(e.s.getTag()==JCTree.VARDEF&&((JCVariableDecl)e.s).init==null))
				{
					comDone.add(e.v);//we com this item only once
				}
			}
		}
        return comDone;
    }

    public Set<VarSymbol> getOutSymbols()
    {
		Set<JCTree> calcNodes=getCalcNodesTransitive();
        Set<VarSymbol> comDone=new LinkedHashSet<VarSymbol>();

		Set<JCTree> outcom=getOutCom();

		for(JCTree v:outcom)
		{
			Set<Arc> out = method.depGraph.outgoingEdgesOf(v);
			for(Iterator<Arc> ai=out.iterator();ai.hasNext();)
			{
				Arc e=ai.next();
				if(!calcNodes.contains(e.t)&&calcNodes.contains(e.s)&&e.v!=null&&e.t.getTag()!=JCTree.SKIP)
				{
					comDone.add(e.v);//we com this item only once
				}
			}
		}
        return comDone;
    }

    public Set<JCTree> getInnerCom(Set<JCTree> calcNodes,JCMethodDecl method)
    {
		//calcNodes=getCalcNodesTransitive();
        Set<JCTree> comDone=new LinkedHashSet<JCTree>();
        for(Iterator<JCTree> cni=calcNodes.iterator();cni.hasNext();)
        {
            JCTree v=cni.next();
            Set<Arc> out = method.depGraph.outgoingEdgesOf(v);
            for(Iterator<Arc> ai=out.iterator();ai.hasNext();)
            {
                Arc e=ai.next();
                if(calcNodes.contains(e.t))
                {
                    comDone.add(e.s);//we com this item only once
                }
            }
            Set<Arc> in = method.depGraph.incomingEdgesOf(v);
            for(Iterator<Arc> ai=in.iterator();ai.hasNext();)
            {
                Arc e=ai.next();
                if(calcNodes.contains(e.s)||e.s.nop_if_alone)
                {
                    comDone.add(e.s);//we com this item only once
                }
            }
        }
        return comDone;
    }

    public Set<VarSymbol> getInnerSymbols(Set<JCTree> calcNodes,JCMethodDecl method)
    {
		//calcNodes=getCalcNodesTransitive();
        Set<VarSymbol> comDone=new LinkedHashSet<VarSymbol>();

		Set<JCTree> outcom=getInnerCom(calcNodes,method);

		Set<VarSymbol> used=new LinkedHashSet<VarSymbol>();

		for(JCTree t:calcNodes)
			used.addAll(JCTree.usedVars(t));

		for(JCTree v:outcom)
		{
			Set<Arc> out = method.depGraph.outgoingEdgesOf(v);
			for(Iterator<Arc> ai=out.iterator();ai.hasNext();)
			{
				Arc e=ai.next();
				if(calcNodes.contains(e.t)&&(e.v!=null||e.s.nop_if_alone))
				{
					comDone.add(e.v);//we com this item only once
				}
			}
		}

		for(VarSymbol vs:method.local_vars)
			if(used.contains(vs)&&vs.isLocal()&&vs.tasklocal()&&(vs.flags_field&Flags.PARAMETER)==0&&(vs.flags_field&Flags.IMPLICITDECL)!=0)
				comDone.add(vs);

        return comDone;
    }

    public int getCom() //accum var size in byte
    {
        //return the actual amount of communication performed by this path

        //Set<JCTree> calcNodes=getCalcNodesTransitive();

        Set<VarSymbol> comDone=getInSymbols();
        comDone.addAll(getOutSymbols());

		int size=0;

		for(VarSymbol vs:comDone)
			size+=Code.width(vs.type);

        return size;
    }

	Set<iTask> getInDeps()
	{
		Set<iTask> res=new LinkedHashSet<iTask>();
		for(DefaultEdge e:method.hasseDiagram.incomingEdgesOf(this))
			res.add(method.hasseDiagram.getEdgeSource(e));
		return res;
	}

	public boolean isCFDEPTo(JCTree root)
	{
		//return method.LocalTGWidth==1;
		//return false;

		//if((method.restype == null && method.name != method.name.table.names.init))
		//	return false;

		//count how many tasks are spawned by the scheduler
		JCTree sched=getFirstCalcNode(method,getCalcNodes()).scheduler;
		int count=0;
		for(iTask s:sched.getSchedule())
		{
            if(s.getFirstCalcNode(method, s.getCalcNodes()).getTag()!=JCTree.CF)
            {
                Set<iTask> deps=((TaskSet)s).getInDeps();
                for(iTask d:deps)
                {
                    JCTree fcn=d.getFirstCalcNode(method, d.getCalcNodes());
                    if(fcn==sched)
                        count++;
                }
            }
		}

		if(count>1)
			return false;

		JCTree fcs=getFirstCalcNode(method, getCalcNodes());
		boolean cf=fcs.getTag()==JCTree.CF||fcs.getTag()==JCTree.IF;
		for(iTask id:getInDeps())
		{
			JCTree fcn=id.getFirstCalcNode(method, id.getCalcNodes());
			if(fcn.scheduler!=root)
			{
				if(fcn.scheduler.getTag()!=JCTree.CF&&fcn.scheduler.getTag()!=JCTree.IF)
					return false;
				else
					cf=true;
				if(!id.isCFDEPTo(root))
					return false;
			}
			else if(!id.getInCom().isEmpty())
				return false;
		}

		return cf;//was there any CF?
	}
    
    public boolean hasTransitiveReturns()
    {
        Set<JCTree> cn1=getCalcNodes();
        for(JCTree t:cn1)
            if((t.getTag()==JCTree.RETURN&&(((JCReturn)t).flags&Flags.FINAL)==0)||t.transitive_returns>0)
                return true;
        
        return false;
    }

	public boolean isLastReachableFrom(iTask other)//O(N)
	{
		Set<JCTree> cn1=getCalcNodes();
		Set<JCTree> cn2=other.getCalcNodes();

		if(cn1.isEmpty()||cn2.isEmpty())
			return false;

		JCTree lc1=getLastCalcNode(method, cn1);//O(N)
		JCTree lc2=other.getLastCalcNode(method, cn2);

		return lc2.getDGNode().IsReachable(false, lc1, method.topolNodes, method.depGraph, true);//O(1)
	}
        /**
         * Andreas Wagner: deep copy
         */
        public TaskSet deepCopy(JCMethodDecl clonedMethodDecl, TreeCopier copy){

            TaskSet clone = new TaskSet(clonedMethodDecl.depGraph, clonedMethodDecl);

            for(Iterator<JCTree> it = this.iterator(); it.hasNext();){
                JCTree t = clonedMethodDecl.cloneAssociationMap.get(it.next());
                clone.add(t);
            }

            return clone;
        }

}
