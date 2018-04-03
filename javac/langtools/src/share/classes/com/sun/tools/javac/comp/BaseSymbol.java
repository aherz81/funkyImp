
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

import java.util.Hashtable;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;

import org.jgrapht.*;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;


/** This pass implements dataflow analysis for Java programs.
 *  Liveness analysis checks that every statement is reachable.
 *  Exception analysis ensures that every checked exception that is
 *  thrown is declared or caught.  Definite assignment analysis
 *  ensures that each variable is assigned when used.  Definite
 *  unassignment analysis ensures that no final variable is assigned
 *  more than once.
*/


public class BaseSymbol extends TreeScanner {
    /** Analyze a definition.
     */
    JCTree symbol;

    public void visitIdent(JCIdent tree) {
        if (tree.sym.kind == VAR&&symbol==null)
        {
            symbol=tree;
        }
    }
/*
 //FIXME: needed??
    public void visitSelect(JCFieldAccess tree) {
        if (tree.sym instanceof VarSymbol &&symbol==null)
        {
            symbol=tree;
        }
    }
*/
    public void visitApply(JCMethodInvocation tree) {
        if(symbol==null)
        {
            symbol=tree;
        }
    }

    public JCTree getBaseSymbol(JCExpression exp)
    {
  //      symbol=null;

        //scan(exp);
		if(exp!=null&&exp.getTag()==JCTree.IDENT)
			return exp;
		else
			return null;

//        return symbol;
    }

    public Set<VarSymbol> getRealSymbols(Symbol s)
    {
        Set<VarSymbol> vs=new LinkedHashSet<VarSymbol>();

        if(s instanceof VarSymbol)
        {
            Set<VarSymbol> ks=((VarSymbol)s).aliasMapLinear.keySet();

            for(Iterator<VarSymbol> i=ks.iterator();i.hasNext();)
            {
                vs.addAll(((VarSymbol)s).aliasMapLinear.get(i.next()));
            }

            if(vs.isEmpty())
                vs.add((VarSymbol)(s));
        }

        return vs;
    }

    public VarSymbol getRealSymbol(Symbol s,JCTree t)
    {
        VarSymbol vs=null;

        if(s instanceof VarSymbol)
        {
            if(!((VarSymbol)s).aliasMapLinear.get(t).isEmpty())
                vs=((VarSymbol)s).aliasMapLinear.get(t).iterator().next();
            else
                vs=(VarSymbol)(s);
        }

        return vs;
    }

    //this is interesting for refcounting
    public boolean mayReturnLocal(JCMethodDecl md)
    {
        MethodSymbol meth = md.sym;

        for(Iterator<VarSymbol> i=meth.retValAliasLinear.iterator();i.hasNext();)
        {
            if(!meth.params.contains(i.next()))
                return true;
        }

        return false;
    }

    //this is interesting for refcounting
    public boolean isReturnLocal(JCMethodDecl md,VarSymbol i)
    {
        MethodSymbol meth = md.sym;
        return !meth.params.contains(i);
    }

    public Set<VarSymbol> getReturnArgs(JCMethodDecl md)
    {
        MethodSymbol meth = md.sym;
        Set<VarSymbol> s=new LinkedHashSet<VarSymbol>();

        for(Iterator<VarSymbol> i=meth.retValAliasLinear.iterator();i.hasNext();)
        {
            VarSymbol vs=i.next();
            if(meth.params.contains(vs))
                s.add(vs);
        }
        return s;
    }

    public boolean isReachable(VarSymbol arg,Set<VarSymbol> locret)
    {
        for(Iterator<VarSymbol> i=locret.iterator();i.hasNext();)
        {
            Set<VarSymbol> roots=VarSymbol.getVarInstance(i.next(), true,new LinkedHashSet<VarSymbol>());
            if(roots.contains(arg))
                return true;
        }

        return false;
    }

    public Set<VarSymbol> getAliasFromBase(JCTree sym,boolean traceinstance)
    {
        if(sym instanceof JCIdent)
        {
            VarSymbol vs=((VarSymbol)((JCIdent)sym).sym);
            if(!traceinstance)
            {
                Set<VarSymbol> s=new LinkedHashSet<VarSymbol>();
				if((vs.owner.flags_field&Flags.SINGULAR)==0) //do not alias singular member
		            s.add(vs);
                return s;
            }
            else
            {
				if((vs.owner.flags_field&Flags.SINGULAR)==0) //do not alias singular member
					return VarSymbol.getVarInstance(vs, true,new LinkedHashSet<VarSymbol>());
				else
					return new LinkedHashSet<VarSymbol>();
            }
        }

        if(sym instanceof JCMethodInvocation)
        {

            MethodSymbol meth = (MethodSymbol)TreeInfo.symbol(((JCMethodInvocation)sym).meth);
            if(!traceinstance)
                return meth.retValAlias;

            Set<VarSymbol> s=new LinkedHashSet<VarSymbol>();

            List<JCExpression> args = ((JCMethodInvocation)sym).getArguments();
            for (List<VarSymbol> l = meth.params; l.nonEmpty(); l = l.tail)
            {
                boolean arg_reachable=isReachable(l.head,meth.retValAlias);

                if(arg_reachable)//&&meth.ReturnValueLinear)
                {
                    JCTree vs=getBaseSymbol(args.head);
                    Set<VarSymbol> ls=getAliasFromBase(vs,traceinstance);
                    s.addAll(ls);
                }

                args = args.tail;
            }

            return s; //checkinput args!!
        }

        return new LinkedHashSet<VarSymbol>();
    }


    public Set<VarSymbol> getAliasGlobalFromBase(JCTree sym)
    {
        if(sym instanceof JCIdent)
        {
			VarSymbol vs=((VarSymbol)((JCIdent)sym).sym);
			if((vs.owner.flags_field&Flags.SINGULAR)==0) //do not alias singular member
			{
				return VarSymbol.getVarInstanceLinear(vs,new LinkedHashSet<VarSymbol>());
			}
			else return new LinkedHashSet<VarSymbol>();
        }

        if(sym instanceof JCMethodInvocation)
        {

            MethodSymbol meth = (MethodSymbol)TreeInfo.symbol(((JCMethodInvocation)sym).meth);

            Set<VarSymbol> s=new LinkedHashSet<VarSymbol>();

            List<JCExpression> args = ((JCMethodInvocation)sym).getArguments();
            for (List<VarSymbol> l = meth.params; l.nonEmpty(); l = l.tail)
            {
                boolean arg_reachable=isReachable(l.head,meth.retValAlias);

                if(arg_reachable)//&&meth.ReturnValueLinear)
                {
                    JCTree vs=getBaseSymbol(args.head);
                    Set<VarSymbol> ls=getAliasGlobalFromBase(vs);
                    s.addAll(ls);
                }

                args = args.tail;
            }

            return s; //checkinput args!!
        }

        return new LinkedHashSet<VarSymbol>();
    }

}

