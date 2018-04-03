/*
 * Copyright 2011-2012 TU-München
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 */
package com.sun.tools.javac.tree;

import java.io.*;
import java.util.*;

import com.sun.tools.javac.util.*;
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
//emitter for data parallel domain stuff
public class CopyArray extends Emitter implements PrintDelegate, TreePrintDelegate {

// ------------------- actual code emitter ---------------
	public CopyArray(LowerTreeImpl state) {
		super(state);
	}


	//visit the actual array element in a domain (e.g. S1(j,k)) which must be replaced by the array access at position array[j,k]
	//mapping from higher-dim to one-d happens here
    
    JCMethodInvocation treeFrom=null;
    
	public void visitDomIndex(JCMethodInvocation tree) throws IOException {
        if(!state.domainIterState.stepForLoop)
        {
            treeFrom=tree;
            printArray(toArray,true);
        }
        else
        {
            JCIdent id = state.jc.make.Ident(state.jc.syms.reduce.name.table.names.fromString("__VAL_TMP" + (state.domainIterState.uid-2)));
            if(outerstate.reduce)
                print("__EXP_TMP" + (state.domainIterState.uid-2));
            else
                print("__EXP_TMP" + (state.domainIterState.uid-1));

            //FIXME: omit projection args, map CG to index!
            
            state.arrayEmitter.printAccess(toArray, id, toArray.dom.projectAccess(itertree.pos(),tree.args), false,true,null);

            print("=");
            
            print("__VAL_TMP" + (state.domainIterState.uid-1));            
                        
            state.arrayEmitter.printAccess(fromArray, id, fromArray.dom.projectAccess(itertree.pos(),treeFrom.args), false,false,null);
            
        }
	}
    
	JCTree.JCForLoop dumpForLoopInits(JCTree.JCMethodDecl cu) {
		class Find extends TreeScanner {

			JCTree.JCForLoop result = null;

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitForLoop(JCForLoop tree) {
                try {
                nl();
                if (tree.init.nonEmpty()) {
                    if (tree.init.head.getTag() == JCTree.VARDEF) {
                        printExpr(tree.init.head);
                        for (List<JCStatement> l = tree.init.tail; l.nonEmpty(); l = l.tail) {
                            JCVariableDecl vdef = (JCVariableDecl) l.head;
                            print(", " + vdef.name + " = ");
                            printExpr(vdef.init);
                        }
                    } else {
                        print("funky::uint32 ");
                        printExpr(tree.init.head);
                        for (List<JCStatement> l = tree.init.tail; l.nonEmpty(); l = l.tail) {
                            //for loop must decl vars:
                            print(", funky::uint32");
                            printExpr(l.head);
                        }
                    }
                }
                print("; ");
                } catch (IOException e) {
                    throw new LowerTree.UncheckedIOException(e);
                }                
                super.visitForLoop(tree);
			}
		}
		Find v = new Find();
		v.scan(cu);
		return v.result;
	}
    

	//for loops are forbidden in funky, but cloog returns for loops
	public void visitForLoop(JCForLoop tree) {

		try {

            nl();
            if (tree.cond != null) {
                print("if(");
                printExpr(tree.cond);
                print(")");
                nl();
            }

            printStat(tree.body);
                        
            undent();
            nl();

            printExprs(tree.step);
            print(";");
			

		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}
    
    public void printArray(Type.ArrayType at,boolean step)
    {
		LowerTreeImpl.IterState oldIterState = state.domainIterState.clone();

		state.domainIterState = new LowerTreeImpl.IterState(state.nestedIterations.size());

		state.nestedIterations.push(state.domainIterState);

		//setup domain iter state...
		state.domainIterState.iter = null; //use this as flag to disconcern std dom iter from array copy?
		state.domainIterState.enclosing = state.current_tree;

		int flag = Type.DomainType.ITER_IN_PROJECTION;
                       
        ListBuffer<JCVariableDecl> fromVars=new ListBuffer<JCVariableDecl>();
        List<VarSymbol> indsFrom=at.dom.getIndexList(new LinkedHashMap<VarSymbol, VarSymbol>());
                
        
        //Map<String, VarSymbol> map = new LinkedHashMap<String, VarSymbol>();
        for(VarSymbol vs:indsFrom)
        {
            VarSymbol nvs=vs.clone(vs.name.table.names.fromString("__CG__"+index));
            fromVars.add(state.jc.make.VarDef(nvs,null));
            index++;
        }
        
		//call barvinok/cloog to get code that iterates over/inside domain/projection
		state.domainIterState.code = at.dom.codegen(itertree.pos(), at.dom.appliedParams.toArray(new JCTree.JCExpression[0]), fromVars.toList(), flag,true,false,false);
		//Type.ArrayType rt = at.getRealType(); //will the real iteration pls stand up, if dom is reinterpreted, then rt!=at


        //replace def vars be used vars
        //state.domainIterState.code = state.arrayEmitter.replace(state.domainIterState.code, map, true);        
        
		try {
			JCTree.JCClassDecl classdecl = (JCTree.JCClassDecl) state.domainIterState.code.defs.get(1);
			JCTree.JCMethodDecl methd = (JCTree.JCMethodDecl) classdecl.defs.last();

			//state.domainIterState.inner = state.arrayEmitter.inner_for(methd);

			state.domainIterState.index = state.arrayEmitter.apply_index(methd);
            
            //emit code to iter over target and to step increment source
            state.domainIterState.stepForLoop=step;
            
            if(!step)
            {
                ListBuffer<JCVariableDecl> inner_fromVars=new ListBuffer<JCVariableDecl>();
                List<VarSymbol> inner_indsFrom=toArray.dom.getIndexList(new LinkedHashMap<VarSymbol, VarSymbol>());

                int oldIndex=index;
                //Map<String, VarSymbol> map = new LinkedHashMap<String, VarSymbol>();
                for(VarSymbol vs:inner_indsFrom)
                {
                    VarSymbol nvs=vs.clone(vs.name.table.names.fromString("__CG__"+index));
                    inner_fromVars.add(state.jc.make.VarDef(nvs,null));
                    index++;
                }
                JCTree.JCCompilationUnit inner_code = toArray.dom.codegen(itertree.pos(), toArray.dom.appliedParams.toArray(new JCTree.JCExpression[0]), inner_fromVars.toList(), flag,true,false,false);
                JCTree.JCClassDecl inner_classdecl = (JCTree.JCClassDecl) inner_code.defs.get(1);
                JCTree.JCMethodDecl inner_methd = (JCTree.JCMethodDecl) inner_classdecl.defs.last();                
                dumpForLoopInits(inner_methd);
                index=oldIndex;
            }
            
            printStats(methd.body.stats);
            state.domainIterState.stepForLoop=false;

		} catch (IOException e) {
			state.log.error(itertree.pos(), "internal", "failed to find inner most for loop");
		} finally {
			state.domainIterState = oldIterState;
			state.nestedIterations.pop();
		}    
    }

    int index=0;
    JCTree itertree=null;
    Type.ArrayType toArray=null;
    Type.ArrayType fromArray=null;
    Type.ArrayType fromBody=null;
    LowerTreeImpl.IterState outerstate=null;
    
	//code gen for array copy: "__EXP_TMP" + state.domainIterState.uid [coord1]="__VAL_TMP" + state.domainIterState.uid[coord2]
	public void printCopyArray(JCTree tree,Type.ArrayType from, Type.ArrayType to) {
        assert(itertree==null);//no recursive entry
        index=0;
        itertree=tree;
        toArray=to;
        fromArray=from;
        fromBody=state.domainIterState.type;
        outerstate=state.domainIterState;
                
        if (!fromBody.getRealType().treatAsBaseDomain()) {
            fromBody = fromBody.getRealType();
        }
        
        printArray(from,false);
        itertree=null;
        state.domainIterState.stepForLoop=false;
	}

}
