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
import com.sun.tools.javac.tree.TreeInfo;


//a PathSet is a task (and has no un-fullfilled self dependencies)

@SuppressWarnings("serial")
public class PathSet extends
java.util.ArrayList<CodePath>
{
    private DirectedWeightedMultigraph<JCTree, Arc> depGraph; //task graph for method
	JCBlock code_block; //cached code
	JCMethodDecl method; //method this path belongs to

    private PathSet()
    {}

    public PathSet(DirectedWeightedMultigraph<JCTree, Arc> depGraph,JCMethodDecl method)
    {
        this.depGraph=depGraph;
		code_block=null;
		this.method=method;
    }

    public void removeNode(JCTree item)
    {
        for(CodePath cp:this)
        {
            cp.remove(item);
        }
    }

	public boolean isIndependentTo(PathSet other)
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

	public boolean isReachableFrom(PathSet other)
	{
		Set<JCTree> cn1=getCalcNodes();
		Set<JCTree> cn2=other.getCalcNodes();

		if(cn1.isEmpty()||cn2.isEmpty())
			return false;

		JCTree lc1=getLastCalcNode(method, cn1);
		JCTree fc2=other.getFirstCalcNode(method, cn2);

		return fc2.getDGNode().IsReachable(false, lc1, method.topolNodes, method.depGraph, true);
	}

	public boolean isLastReachableFrom(PathSet other)
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
        for(Iterator<CodePath> p=iterator();p.hasNext();)
        {
            CodePath l=p.next();
            for(Iterator<JCTree> ap=l.iterator();ap.hasNext();)
            {
                JCTree tt=ap.next();
                if(tt==node)
                    return true;
            }
        }

        return false;
    }

	//get topol smallest node (may be non-calc node)
    public JCTree getFirstFromPathSet()
    {
        JCTree first=this.get(this.size()-1).get(0);
        for(Iterator<CodePath> li=this.iterator();li.hasNext();)
        {
            JCTree lfirst=li.next().get(0);
            if(method.topolNodes.get(lfirst)<method.topolNodes.get(first))
                first=lfirst;
        }
        return first;
    }

	//get topol largest node (may be non-calc node)
    public JCTree getLastFromPathSet()
    {
        JCTree last=this.get(0).get(0);
        for(Iterator<CodePath> li=this.iterator();li.hasNext();)
        {
			CodePath cp=li.next();
            JCTree llast=cp.get(cp.size()-1);
            if(method.topolNodes.get(llast)>method.topolNodes.get(last))
                last=llast;
        }
        return last;
    }

	//this method must mirror the splitting in PathGen DFPP
    public boolean isCalcPathNode(JCTree node,boolean local_first,boolean last,boolean local_last)
    {
		//splits mostly depend on id and od:
        int od=method.depGraphImplicit.outDegreeOf(node);
        int id=method.depGraphImplicit.inDegreeOf(node);

		if(node.getTag()==JCTree.CF||node.isScheduler)//special case for schedulers/CF
			od=Math.max(2,od);

        if(id<1)
            return false;

        return      (od<2&&id<2&&!last) //neither split nor join
                    ||
                    (od>1&&id<2&&!local_first)//split
                    ||
                    (id>1&&od<2&&local_first&&!last) //join
                    ||
                    (id>1&&od>1&&local_first&&local_last); //split/join
    }

	//add another path set to this one
	public void add(PathSet from) {
		for (CodePath p : from) {
			add(p);
		}
	}

    public Set<JCTree> getCalcNodesTransitive()
	{
		return null;
	}

    public Set<JCTree> getCalcNodes()
    {
        //return the actual amount of computation performed by this path
        Set<JCTree> nodes=new LinkedHashSet<JCTree>();

        for(Iterator<CodePath> p=this.iterator();p.hasNext();)
        {
            CodePath l=p.next();
            boolean first=true;
            for(Iterator<JCTree> ap=l.iterator();ap.hasNext();)
            {
                JCTree tt=ap.next();
                if(isCalcPathNode(tt,first,tt==getLastFromPathSet(),!ap.hasNext()))
                {
					nodes.add(tt);
                }
                first=false;
            }
        }

        return nodes;
    }

    public String toString()
    {
        String spath="[";

		List<JCStatement> stats=getPathBlockNoCache().stats;

		if(stats!=null)
			spath+=stats.toString();

        return spath+"]";
    }

	public JCBlock getPathBlockFromSet(Set<JCTree> set,final JCMethodDecl md)
	{
		ArrayList<JCStatement> list = new ArrayList<JCStatement>();
		for(JCTree t:set)
			list.add((JCStatement)t);

		Collections.sort(list, new Comparator<JCTree>() {
			public int compare(JCTree a1, JCTree a2) {
				return md.topolNodes.get(a1) - md.topolNodes.get(a2);
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

    public Set<JCTree> getInCom(Set<JCTree> calcNodes)
    {
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

    public Set<JCTree> getInComImplicit(Set<JCTree> calcNodes)
    {
        Set<JCTree> comDone=new LinkedHashSet<JCTree>();
		Set<VarSymbol> symDone=new LinkedHashSet<VarSymbol>();
        for(Iterator<JCTree> cni=calcNodes.iterator();cni.hasNext();)
        {
            JCTree v=cni.next();
            Set<Arc> in = depGraph.incomingEdgesOf(v);
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

    public Set<VarSymbol> getInSymbols(JCTree v,Set<JCTree> calcNodes)
    {
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

    public Set<VarSymbol> getInSymbols(Set<JCTree> calcNodes)
    {
        Set<VarSymbol> comDone=new LinkedHashSet<VarSymbol>();
		Set<JCTree> outcom=getInCom(calcNodes);

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

	public Set<VarSymbol> getInSymbolsImplicit(Set<JCTree> calcNodes)
	{
        Set<VarSymbol> comDone=new LinkedHashSet<VarSymbol>();
		Set<JCTree> outcom=getInCom(calcNodes);

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

    public boolean isJoining(Set<JCTree> calcNodes)
    {
        for(Iterator<JCTree> cni=calcNodes.iterator();cni.hasNext();)
        {
            JCTree v=cni.next();
            Set<Arc> out = depGraph.outgoingEdgesOf(v);
            for(Iterator<Arc> ai=out.iterator();ai.hasNext();)
            {
                Arc e=ai.next();
				//nodes that contribute to a finally expression
				if(e.v!=null&&e.t.getTag()==JCTree.RETURN&&(((JCReturn)e.t).flags&Flags.FINAL)!=0)
					return true;
            }
        }
        return false;
    }

    public boolean isFinal(Set<JCTree> calcNodes)
    {
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

    public Set<JCTree> getOutCom(Set<JCTree> calcNodes)
    {
        Set<JCTree> comDone=new LinkedHashSet<JCTree>();
        for(Iterator<JCTree> cni=calcNodes.iterator();cni.hasNext();)
        {
            JCTree v=cni.next();
            Set<Arc> out = depGraph.outgoingEdgesOf(v);
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

    public Set<JCTree> getTargets(Set<JCTree> calcNodes)
    {
        Set<JCTree> comDone=new LinkedHashSet<JCTree>();
        for(Iterator<JCTree> cni=calcNodes.iterator();cni.hasNext();)
        {
            JCTree v=cni.next();
            Set<Arc> out = depGraph.outgoingEdgesOf(v);
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

    public Set<VarSymbol> getOutSymbols(JCTree v,Set<JCTree> calcNodes)
    {
        Set<VarSymbol> comDone=new LinkedHashSet<VarSymbol>();

        Set<Arc> out = depGraph.outgoingEdgesOf(v);
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
    public Set<VarSymbol> getNullFreeOutSymbols(Set<JCTree> calcNodes)
    {
        Set<VarSymbol> comDone=new LinkedHashSet<VarSymbol>();

		Set<JCTree> outcom=getOutCom(calcNodes);

		for(JCTree v:outcom)
		{
			Set<Arc> out = depGraph.outgoingEdgesOf(v);
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

    public Set<VarSymbol> getOutSymbols(Set<JCTree> calcNodes)
    {
        Set<VarSymbol> comDone=new LinkedHashSet<VarSymbol>();

		Set<JCTree> outcom=getOutCom(calcNodes);

		for(JCTree v:outcom)
		{
			Set<Arc> out = depGraph.outgoingEdgesOf(v);
			for(Iterator<Arc> ai=out.iterator();ai.hasNext();)
			{
				Arc e=ai.next();
				if(!calcNodes.contains(e.t)&&calcNodes.contains(e.s)&&e.v!=null)
				{
					comDone.add(e.v);//we com this item only once
				}
			}
		}
        return comDone;
    }

    public Set<JCTree> getInnerCom(Set<JCTree> calcNodes,JCMethodDecl method)
    {
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

        Set<JCTree> calcNodes=getCalcNodes();

        Set<VarSymbol> comDone=getInSymbols(calcNodes);
        comDone.addAll(getOutSymbols(calcNodes));

		int size=0;

		for(VarSymbol vs:comDone)
			size+=Code.width(vs.type);

        return size;
    }

	public boolean isCFDEPTo(JCTree root)
	{
		return false;
	}

	public Map<VarSymbol,JCTree> getOutComMap(Set<JCTree> calcNodes)
	{
		return null;
	}

}
