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
import com.sun.tools.javac.tree.cl.util.ProfilingCodeGenerator;

import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

/**
 * Prints out a tree as an indented Java source program.
 *
 * <p><b>This is NOT part of any API supported by Sun Microsystems. If you write code that depends
 * on this, you do so at your own risk. This code and its internal interfaces are subject to change
 * or deletion without notice.</b>
 */
//emitter for data parallel domain stuff
public class LowerDataRT extends Emitter implements PrintDelegate, TreePrintDelegate {

	boolean inside_joining = false; //inside statement that contributes to finally?

// ------------------- actual code emitter ---------------
	public LowerDataRT(LowerTreeImpl state) {
		super(state);
	}

	//a projection describes a subset of a domain and is specified as on offset
	public void visitProjection(JCMethodInvocation tree) throws IOException {

		Type.ArrayType at = ((Type.ArrayType) tree.type.getArrayType());

		if (state.lastProjectionArgs == null)//record different projection params for later use
		{
			state.lastProjectionArgs = new LinkedHashMap<VarSymbol, JCExpression>();
		}

		for (int i = 0; i < tree.args.size(); i++) {
			VarSymbol vs = ((Type.DomainType) ((Type.ArrayType) tree.type.getArrayType()).dom).projectionArgs.get(i);
			JCExpression e = tree.args.get(i);
			if (TreeInfo.symbol(e) != vs) {
				state.lastProjectionArgs.put(vs, e);
			}
		}

		JCTree.JCFieldAccess sel = ((JCTree.JCFieldAccess) tree.meth);

		if (at.treatAsBaseDomain()) {
			printExpr(sel.selected);
		} else {
			newProjection(at, sel.selected, tree.args);
		}
	}

	//dyn arrays can be resized
	public void visitResize(JCMethodInvocation tree) throws IOException {

		JCTree.JCFieldAccess sel = ((JCTree.JCFieldAccess) tree.meth);
		printExpr(sel.selected);
		print("->SetCount(");
		printExpr(tree.args.head);
		print(")");
	}

	public void visitDomainDef(JCDomainDecl tree) {
		//completely handled by compiler...
	}

	//visit the actual array element in a domain (e.g. S1(j,k)) which must be replaced by the array access at position array[j,k]
	//mapping from higher-dim to one-d happens here
	public void visitDomIndex(JCMethodInvocation tree) throws IOException {

        if(state.domainIterState.iter==null)
        {
            state.arrayCopyEmitter.visitDomIndex(tree);
            return;
        }
        
		boolean fix = state.domainIterState.fix_inner_counter;
		state.domainIterState.fix_inner_counter = true;

		if (state.domainIterState.consecutive && state.domainIterState.iter != null && state.domainIterState.iter.type.tag != TypeTags.VOID && !state.domainIterState.void_iter) {
			print("return ");
		}

		if(state.domainIterState.inner==null)
			nl();

		if (state.domainIterState.iter.body != null) {
			Type bt = state.domainIterState.iter.body.type;
			Type at = bt.getArrayType();

			boolean isProjection = false;

			if (at.tag == TypeTags.ARRAY) {
				isProjection = !((Type.ArrayType) at).treatAsBaseDomain();
			}

			if (!isProjection && !bt.isPointer() && !bt.isPrimitive() && state.domainIterState.iter.type.tag != TypeTags.VOID && !state.domainIterState.void_iter) {
				print("*(");
			}

			if (!state.domainIterState.consecutive) {
				//print array[x,y]=
				JCDomainIter itree = state.domainIterState.iter;

				Type.ArrayType iat = (Type.ArrayType) itree.exp.type.getArrayType();

				JCIdent id = state.jc.make.Ident(state.jc.syms.reduce.name.table.names.fromString("__VAL_TMP" + state.domainIterState.uid));
				id.type = iat;
                
                if((Type.ArrayType.getCodeGenFlags(state.domainIterState.iter, state.domainIterState.iter.iterType)&Type.DomainType.ITER_OVER_PROJECTIONS)!=0)
                {
                    //generate result into temporary and copy required part into result later
                    /*
                    print("//");
                    print("__EXP_TMP" + (state.domainIterState.uid+1)+"=");                    
            		printDomainOffset(id.pos(), id, (Type.ArrayType) state.domainIterState.iter.body.type.getArrayType(), tree.args.toArray(new JCTree.JCExpression[0]), true, false, false);
                    */
                    Type.ArrayType dat=(Type.ArrayType)state.domainIterState.iter.body.type.getArrayType();
                    String type;

                    if (!dat.getRealType().treatAsBaseDomain()) {
                        type = state.typeEmitter.getType(dat.getRealType());
                    } else {
                        type = state.typeEmitter.getType(dat);
                    }
                    
                    if (!state.domainIterState.reduce) 
                    {//handle state.target
                        LowerTreeImpl.IterState baseIter=state.domainIterState;

                        if (state.nestedIterations.size() > 1) {
                            int prevIter=state.nestedIterations.size()-2;

                            while(prevIter>0&&state.nestedIterations.elementAt(prevIter).reduce)
                                prevIter--;

                            baseIter=state.nestedIterations.elementAt(prevIter);
                        }                    
                        
                        nl();
                        print(type + "__EXP_TMP" + (state.domainIterState.uid+1) + "=");
                        if (!dat.treatAsBaseDomain()) {
                            print(type + "(");
                        }

                        print("__EXP_TMP" + baseIter.uid);
                        if (!baseIter.type.treatAsBaseDomain()) {
                            print(".object");
                        }

                        if (!dat.treatAsBaseDomain()) {
                            //get proper object
                            print(",");

                            Type.ArrayType ot=(Type.ArrayType)baseIter.iter.iterType.getArrayType();

                            JCExpression[] indices=new JCExpression[baseIter.iter.domargs.size()];

                            int i=0;
                            for(JCVariableDecl vd:baseIter.iter.domargs)
                            {
                                indices[i]=state.jc.make.Ident(vd.sym);
                                i++;
                            }

                            printDomainOffset(tree.pos(),baseIter.iter.exp, ot, indices , false, false, true);
                            print(")");
                        }
                        else if (!dat.treatAsBaseDomain()) {
                            print(", __VAL_TMP" + state.domainIterState.uid + ".offset)");
                        }


                        print(";");
                    }                    
                    
                    nl();
                    print(type + "__VAL_TMP" + (state.domainIterState.uid+1) + "=");
                    
                    nl();
                }            
                else
                {
                    print("__EXP_TMP" + state.domainIterState.uid);

                    printAccess((Type.ArrayType) iat, id, tree.args, false,true,null);

                    //FIXME: may require memcpy for non prim items (like row)

                    print("=");
                }

			}

			printStat(state.domainIterState.iter.body);

			if (!isProjection && !bt.isPointer() && !bt.isPrimitive() && state.domainIterState.iter.type.tag != TypeTags.VOID && !state.domainIterState.void_iter) {
				print(")");
			}
            
            
            if(!state.domainIterState.consecutive&&(Type.ArrayType.getCodeGenFlags(state.domainIterState.iter, state.domainIterState.iter.iterType)&Type.DomainType.ITER_OVER_PROJECTIONS)!=0)
            {
                print(";");
                                
                //read from tree.iterType, write to site
                Type.ArrayType fromIter=(Type.ArrayType)state.domainIterState.iter.iterType.getArrayType();
                Type.ArrayType toIter=(Type.ArrayType)state.domainIterState.iter.body.type.getArrayType();                                     

                state.arrayCopyEmitter.printCopyArray(state.domainIterState.iter,fromIter,toIter);

                if(!state.domainIterState.void_iter)
                {
                    nl();
                    print("return ");
                    print("__EXP_TMP" + (state.domainIterState.uid));
                }
            }                

		} else {
			printStat(state.domainIterState.iter.sbody);
		}

		state.domainIterState.fix_inner_counter = fix;

	}

	//generate new version! of an array
	public void printNewVersion(DiagnosticPosition pos, JCExpression from, Type.DomainType dt, Type elt, List<JCExpression> dyn_size) throws IOException {
		//int bucket_size = dt.getBucketSize(state.log, pos, dt.appliedParams.toArray(new JCTree.JCExpression[0]));
		nl();
		String tp = "funky::LinearArray< " + state.typeEmitter.getTypePure(elt) + " >";

		if (dt.isDynamic()) {
			print("(new " + tp + "::Version" + "(");

			printExpr(from);
			print("->GetArray()");
			if (dyn_size != null) {
				print(", " + dyn_size.size() + ", ");
				printExprs(dyn_size);
			}

			print(")");
			print(")");
		}
	}

	//create a new array
	public void printNewArray(DiagnosticPosition pos, Type.DomainType dt, Type elt, List<JCExpression> dyn_size, JCTree init) throws IOException {
		if (dt == null) {
			print("funky::LinearArray< " + state.typeEmitter.getTypePure(elt) + " >::alloc(");

			if (dyn_size.size() > 1) {
				print("funky::LinearArray< " + state.typeEmitter.getTypePure(elt) + " >::getDimSize(");
				print(dyn_size.size() + ", ");
				printExprs(dyn_size);
				print(")");
			} else {
				printExpr(dyn_size.head);
			}

			if(!elt.containsPointer())
			{
				print(",true");
			}

			print(")");
		} else {
			nl();
			String tp = "funky::LinearArray< " + state.typeEmitter.getTypePure(elt) + " >";

			if (dt.isDynamic()) {
				print("new " + tp + "::Version" + "(new " + tp + "(");
				if (dyn_size != null) {
					if (dyn_size.size() > 1) {
						print("funky::LinearArray< " + state.typeEmitter.getTypeNoConst(elt) + " >::getDimSize(");
						print(dyn_size.size() + ", ");
						printExprs(dyn_size);
						print(")");
					} else {
						printExpr(dyn_size.head);
					}
				} else {
					print("0");
				}
				if (init != null) {
					print(", ");
					printExpr(init);
				}
				else if(!elt.containsPointer())
				{
					print(",true");
				}
				print(")");

				if (dyn_size != null) {
					print(", " + dyn_size.size() + ", ");
					printExprs(dyn_size);
				}

				print(")");
			} else {
				int size = 1;
				int[] max = dt.getSize(pos, dt.appliedParams.toArray(new JCTree.JCExpression[0]));

				for (int i : max) {
					size *= i;
				}

				print("new " + tp + "::Version" + "(new " + tp + "(" + size);

				if (init != null) {
					print(", ");
					printExpr(init);
				}
				else if(!elt.containsPointer())
				{
					print(",true");
				}
				print(")");
				print(")");
			}
		}
		//FIXME: use persistent array if updated with 'where'
	}

	public void visitNewArray(JCNewArray tree) {
		try {

			if (tree.elemtype == null) {

				//generate alloc + init with switch stmt if sizes fit??
				//package in lambda:
				nl();
				print("([&]()->" + state.typeEmitter.getType(tree.type));
				nl();
				print("{");
				indent();

				nl();
				print(state.typeEmitter.getType(tree.type) + " __VAL__=");

				ListBuffer<JCExpression> buf = new ListBuffer<JCExpression>();
				buf.add(state.jc.make.Ident(((Type.DomainType) ((Type.ArrayType) tree.type).dom).tsym.name));
				List<JCExpression> dyn_size = buf.toList();
				printNewArray(tree.pos(), ((Type.ArrayType) tree.type).dom, ((Type.ArrayType) tree.type).elemtype, dyn_size, null);
				print(";");

				nl();
				print("__VAL__->mapNewVersion([&](funky::uint32 i)->" + state.typeEmitter.getType(((Type.ArrayType) tree.type).elemtype));
				nl();
				print("{");
				indent();
				nl();
				print("switch(i)");
				nl();
				print("{");
				indent();
				for (int i = 0; i < tree.elems.size(); i++) {
					nl();
					print("case " + i + ": return " + tree.elems.get(i) + ";");
				}
				nl();
				print("default : return -1;");
				undent();
				nl();
				print("}");
				undent();
				nl();
				print("},0," + tree.elems.size() + ",0,__VAL__);");

				nl();
				print("return __VAL__;");
				undent();
				nl();
				print("}");

				print(")()");

				//state.log.error(tree.pos, "not.impl", "array initializers");
				return;
			}

			if (state.generate_into && tree.return_expression) {
				print("__FORWARD__");
				return;
			}

			if (tree.elemtype.type.tsym.name.equals(state.names.Object)) {
				if (tree.elems.isEmpty()) {
					print("NULL");
				} else {
					printExprs(tree.elems);//hack to allow ellipsis with Object
				}
				return;
			}

			Type.DomainType dt = (Type.DomainType) tree.dom.type;

			List<JCExpression> dyn_size;
			if (dt != null) {
				dyn_size = tree.dom.domparams;
			} else {
				ListBuffer<JCExpression> buf = new ListBuffer<JCExpression>();
				buf.add(state.jc.make.Ident(tree.dom.name));
				dyn_size = buf.toList();
			}

			printNewArray(tree.pos(), dt, ((Type.ArrayType) tree.elemtype.type).elemtype, dyn_size, null);

		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

	//can join several domains a~b~c
	public void visitJoin(JCJoinDomains tree) {
		try {
			//FIXME: package into lambda
			nl();
			print("([&]()->");
			print(state.typeEmitter.getType(tree.type));
			nl();
			print("{");
			indent();

			Type.ArrayType at = (Type.ArrayType) tree.type.getArrayType();

			nl();
			//FIXME: projections??
			String type;

			if (!at.getRealType().treatAsBaseDomain()) {
				type = state.typeEmitter.getType(at.getRealType());
			} else {
				type = state.typeEmitter.getType(at);
			}


			print(type + " __FORWARD__=");

			ListBuffer<JCExpression> dyns=new ListBuffer<JCExpression>();

			for(JCExpression e:at.dom.appliedParams)
			{
				boolean constraint=(e.getTag()==JCTree.GT||e.getTag()==JCTree.GE
						||e.getTag()==JCTree.LT||e.getTag()==JCTree.LE)
						&&(((JCBinary)e).lhs.getTag()==JCTree.IDENT);

				JCExpression real=e;

				if(constraint)
					real=((JCBinary)e).lhs;

				dyns.add(real);
			}

			printNewArray(tree.pos(), at.dom, at.elemtype, dyns.toList(), null);
			print(";");

			boolean old_join = inside_joining;

			inside_joining = true;

			nl();
			print("funky::LinearArray<>::Version::forceall(");

			nl();
			print("[&]");
			nl();
			print("(funky::uint32 __VAL__");

			print(")->void");

			nl();

			print("{");

			nl();indent();

			print("switch(__VAL__)");
			print("{");

			nl();indent();

			//FIXME: non iterations must be copied into __FORWARD__
			for (int i=0;i<tree.doms.size();i++) {
				print("case "+i+":");nl();
				JCExpression e = tree.doms.get(i);
				if(e.type!=state.jc.syms.botType)
				{
					nl();
					printExpr(e);
					print(";");
				}
				nl();
				print("break;");nl();

			}

			undent();nl();
			print("}");

			undent();nl();
			print("}");

			print(", 0");
			print(", "+tree.doms.size());
			print(");");


			inside_joining = old_join;

			nl();
			print("return __FORWARD__;");
			undent();
			nl();
			print("}) ()");

		} catch (IOException e) {
			inside_joining = false;
			throw new LowerTree.UncheckedIOException(e);
		}

	}

	//small analyses to find stuff inside code generate by barvinok/cloog
	JCTree.JCMethodInvocation inner_apply(JCTree.JCMethodDecl cu) {
		class Find extends TreeScanner {

			JCTree.JCMethodInvocation result = null;

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitApply(JCMethodInvocation tree) {
				result = tree;
			}
		}
		Find v = new Find();
		v.scan(cu);
		return v.result;
	}

	boolean findIndex(JCTree.JCExpression e, final VarSymbol index) {
		class Find extends TreeScanner {

			boolean result = false;

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitIdent(JCIdent tree) {
				if (tree.sym == index) {
					result = true;
				}
			}
		}
		Find v = new Find();
		v.scan(e);
		return v.result;
	}

	JCTree.JCForLoop inner_for(JCTree.JCMethodDecl cu) {
		class Find extends TreeScanner {

			JCTree.JCForLoop result = null;

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitForLoop(JCForLoop tree) {
				result = tree;
				super.visitForLoop(tree);
			}
		}
		Find v = new Find();
		v.scan(cu);
		return v.result;
	}

	JCTree.JCMethodInvocation apply_index(JCTree.JCMethodDecl cu) {
		class Find extends TreeScanner {

			JCTree.JCMethodInvocation result = null;

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitApply(JCMethodInvocation mi) {
				result = mi;
				super.visitApply(mi);
			}
		}
		Find v = new Find();
		v.scan(cu);
		return v.result;
	}

	//for loops are forbidden in funky, but cloog returns for loops
	public void visitForLoop(JCForLoop tree) {
        
        if(state.domainIterState.stepForLoop)
        {
            state.arrayCopyEmitter.visitForLoop(tree);
            return;
        }
        
        
		try {

            if(state.domainIterState.usedcounter!=null)
            {
                if (tree.init.head instanceof JCExpressionStatement && ((JCExpressionStatement) tree.init.head).expr instanceof JCAssign) {
                    state.domainIterState.usedcounter.add((VarSymbol) TreeInfo.symbol(((JCAssign) ((JCExpressionStatement) tree.init.head).expr).lhs));
                } else if (tree.init.head.getTag() == JCTree.VARDEF) {
                    state.domainIterState.usedcounter.add((VarSymbol) TreeInfo.symbol(tree.init.head));
                }
            }

			boolean innerfor = (tree == state.domainIterState.inner);

			state.domainIterState.loopCount++;

			if (innerfor) {
//				prepareInnerFor(tree.body);
				if (state.domainIterState.consecutive) {
					visitInnerFor(tree);
					return;
				}
			}

			if (state.domainIterState.reduce && !state.domainIterState.iter.nop) {

				JCDomainIter innertree = state.domainIterState.iter;
				Symbol accum = innertree.domargs.get(innertree.domargs.size() - 1).sym;
				state.domainIterState.reduceiter = replace(state.domainIterState.iter, accum, accum.name, state.names.fromString("__JOIN__"));

				if(state.domainIterState.reduceiter.nop&&false)
				{
					state.log.error(state.domainIterState.iter.pos, "internal","failed to deduce join of reductions");
				}
				else
				{
					visitInnerFor(tree);

					return;
				}
			}

			if (!state.domainIterState.reduce && !state.insideParallelRegion && tree.init.head instanceof JCExpressionStatement && ((JCExpressionStatement) tree.init.head).expr instanceof JCAssign
					&& tree.cond instanceof JCBinary) {

				//FIXME: what about complex iterations??
				nl();
				print("funky::LinearArray<>::Version::forall(");

				nl();
				print("[&]");
				nl();
				print("(funky::uint32 ");
				printExpr(((JCAssign) ((JCExpressionStatement) tree.init.head).expr).lhs);
				print(")->void");

				nl();

				boolean oldPar = state.insideParallelRegion;
				state.insideParallelRegion = true;

				printStat(tree.body);

				state.insideParallelRegion = oldPar;

				print(", ");
				printExpr(((JCAssign) ((JCExpressionStatement) tree.init.head).expr).rhs);
				print(", ");
				printExpr(((JCBinary) tree.cond).rhs);

				print(" + 1");
				print(");");

			} else {
				/*
				 nl();
				 print("#pragma vector aligned");
				 nl();
				 print("#pragma simd vectorlength(16)");
				 nl();
				 print("#pragma unroll");
				 nl();
				 */

				nl();
				print("for (");
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
				if (tree.cond != null) {
					printExpr(tree.cond);
				}
				print("; ");
				printExprs(tree.step);
				print(") ");

				printStat(tree.body);

			}

		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

	//offset of item [0,..,0] into array (e.g. due to projection)
	public void printOffset(DiagnosticPosition pos) throws IOException {

		JCDomainIter tree = state.domainIterState.iter;
		Type.ArrayType at = (Type.ArrayType) tree.exp.type.getArrayType();

		if (state.domainIterState.reduce || tree.type.tag == TypeTags.VOID || state.domainIterState.void_iter) {
			return;
		}

		print(", ");

		if (!at.treatAsBaseDomain()) {
			if (state.generate_into) {
				print("__FORWARD__");
			} else {
				print("__VAL_TMP" + state.domainIterState.uid);
			}
			print(".offset");
		}

		//nl();

		//print("const funky::uint32 offset" + state.domainIterState.uid + "=");

		//FIXME: 0 if iter of at and rt are not compatible (see visitIndexed for test)
		if (at.treatAsBaseDomain()) {
			JCTree.JCExpression[] inds;//=tree.domargs.toArray(new JCTree.JCExpression[0]);

			List<JCVariableDecl> cur = tree.domargs;

			if (state.domainIterState.reduce) {
				inds = new JCTree.JCExpression[1];
				inds[0] = state.jc.make.Literal(TypeTags.INT, 0);
			} else {
				inds = new JCTree.JCExpression[cur.size()];
				for(int i=0;i<cur.size();i++)
					inds[i]=state.jc.make.Ident(cur.get(i).sym);
			}

			print(" + ");

			if (at.isCompatibleToRealTypeAndIterable(state.jc, at.dom, tree.pos())) {
				printDomainOffset(tree.pos(), state.jc.make.Ident(state.names.fromString("__VAL_TMP" + state.domainIterState.uid)), at, inds, true, true, at.getRealType().treatAsBaseDomain());
			} else {
				printDomainOffset(tree.pos(), state.jc.make.Ident(state.names.fromString("__VAL_TMP" + state.domainIterState.uid)), at.getRealType().getParentType(), inds, true, true, true);
			}
		}

		if (!at.treatAsBaseDomain()) {
			print(" + ");
			LowerTreeImpl.IterState oldIterState = state.domainIterState;
			Type.ArrayType ot = (Type.ArrayType) oldIterState.iter.iterType.getArrayType();

			JCExpression[] indices = new JCExpression[oldIterState.iter.domargs.size()];

			int i = 0;
			for (JCVariableDecl vd : oldIterState.iter.domargs) {
				indices[i] = state.jc.make.Ident(vd.sym);
				i++;
			}

			printDomainOffset(tree.pos(), oldIterState.iter.exp, ot, indices, true, true, at.getRealType().treatAsBaseDomain());
		}

	}

	public void printForwardObject() throws IOException {
		if (!state.domainIterState.domain.treatAsBaseDomain()) {
			JCIdent id = state.jc.make.Ident(state.jc.syms.reduce.name.table.names.fromString("__EXP_TMP" + state.domainIterState.uid));
			id.type = state.domainIterState.domain;

			ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();

			for (JCVariableDecl vd : state.domainIterState.iter.domargs) {
				args.add(state.jc.make.Ident(vd.sym));
			}

			newProjection(state.domainIterState.domain, id, args.toList());
		} else {
			print("__EXP_TMP" + state.domainIterState.uid);
		}
	}

	//dump iteration range including offsets
	public void printRange(JCExpression from, JCExpression to) throws IOException {
		//JCDomainIter tree = state.domainIterState.iter;
		//Type.ArrayType at = (Type.ArrayType) tree.exp.type.getArrayType();

		printExpr(from);

		print(", ");

		printExpr(to);

		print(" + 1");

		printRangeOffset();
	}

	public void printRangeOffset() throws IOException {

		printOffset(state.domainIterState.iter.pos());
	}

	//transform ast for reduce
	<T extends JCTree> T replace(T cu, final Symbol accum, final Name acc, final Name join) {
		class Replace extends TreeScanner {

			boolean found = false;

			public void scan(JCTree tree) {
				if (tree != null) {
					tree.accept(this);
				}
			}

			public void visitApply(JCTree.JCMethodInvocation tree)
			{
				super.visitApply(tree);
				ListBuffer<JCExpression> args=new ListBuffer<JCExpression>();
				for(JCExpression e:tree.args)
				{
					Set<VarSymbol> used = JCTree.usedVars(e);

					if(!used.contains((VarSymbol)accum))
					{
						args.add(state.jc.make.Ident(join));
					}
					else if(used.size()==1)
					{
						found = true;
						args.add(state.jc.make.Ident(acc));
					}
				}
				tree.args=args.toList();

			}

			public void visitBinary(JCTree.JCBinary tree) {
				if (TreeInfo.symbol(tree.lhs) == accum) {
					tree.rhs = state.jc.make.Ident(join);
					tree.lhs = state.jc.make.Ident(acc);
					found = true;
				} else if (TreeInfo.symbol(tree.rhs) == accum) {
					tree.lhs = state.jc.make.Ident(join);
					tree.rhs = state.jc.make.Ident(acc);
					found = true;
				} else {
					super.visitBinary(tree);
				}
			}
		}
		Replace v = new Replace();
		cu = (T) (new TreeCopier(state.jc.make)).copy(cu);
		v.scan(cu);
		cu.nop = !v.found;
		return cu;
	}

	void printInitialAccum(JCDomainIter tree) throws IOException {

		if (tree.params.head.type.isPrimitive()) {
			print("static_cast< ");
			print(state.typeEmitter.getType(tree.body.type));
			print(" >(");
			printExpr(tree.params.head);
			if (tree.params.head.type.isPrimitive()) {
				print(")");
			}
		} else {
			print("__EXP_TMP" + state.domainIterState.uid);
			//if (!at.treatAsBaseDomain())
			//	print(".object");
		}

	}

	//code for the inner most for loop (should be vectorized)
	public void visitInnerFor(JCTree.JCForLoop forloop) throws IOException {
        
        /*
        if(state.domainIterState.iter==null)
        {
            state.arrayCopyEmitter.visitInnerFor(forloop);
            return;
        }
        */
                
		JCDomainIter tree = state.domainIterState.iter;

		Type.ArrayType at = (Type.ArrayType) tree.exp.type.getArrayType();

		if (state.domainIterState.reduce) {
			if (!state.domainIterState.noreturn) {
				nl();
				print("return ");
			} else {
				nl();
				print(state.typeEmitter.getType(state.domainIterState.iter.body.type));
				print(" __JOIN__=");
			}
		}

		print("__VAL_TMP" + state.domainIterState.uid);

		Type.ArrayType bt;
		if (!at.getRealType().treatAsBaseDomain()) {
			bt = at.getRealType();
		} else {
			bt = at;
		}

		if (!bt.treatAsBaseDomain()) {
			print(".object");
		}

		if (state.domainIterState.reduce) {
			print("->reduce");
		} else {
			if (tree.type.tag != TypeTags.VOID && !state.domainIterState.void_iter) {
				print("->mapNewVersion");
			} else {
				print("->mapVoid");
			}
		}

		if (state.insideParallelRegion&&state.domainIterState.loopCount>1) //only parallelize outer most loops
		{
			print("_seq");
		}

		print("(");
		nl();
		print(getCaptured(state.domainIterState.used, false, at.treatAsBaseDomain() && tree.domargs.size() > 1));
		nl();
		print("(funky::uint32 ");

		Symbol reduceCounter = null;

		if (state.domainIterState.reduce) {
			reduceCounter = TreeInfo.symbol(((JCTree.JCAssign) ((JCTree.JCExpressionStatement) forloop.init.head).expr).lhs);
			print(reduceCounter.name);
			//take care to print proper accumulator value
			print(", " + state.typeEmitter.getType(tree.body.type) + " " + tree.domargs.get(tree.domargs.size() - 1).name);
		} else {
			Symbol s = TreeInfo.symbol(((JCTree.JCAssign) ((JCTree.JCExpressionStatement) forloop.init.head).expr).lhs);
			print(s.name);
		}
		state.insideArray = true;

		print(")->");

		if (!state.domainIterState.void_iter) {
			print(state.typeEmitter.getType(tree.body.type));
		} else {
			print("void");
		}
		state.insideArray = false;

		nl();

		boolean oldPar = state.insideParallelRegion;
		state.insideParallelRegion = true;
		boolean noret = state.domainIterState.noreturn;

		if (state.domainIterState.reduceiter != null) {
			state.domainIterState.noreturn = true;
		}


		if (tree.dg_end != null) {
			iTask ps = state.domainIterState.enclosing.getSchedule().iterator().next();
			state.DumpSchedule(state.domainIterState.enclosing, ps, state.task_map, state.dump_kernel);
			state.DumpPath(ps, state.task_map);

			Set<VarSymbol> old_outcom = state.path_outcom;
			state.path_outcom = ps.getNullFreeOutSymbols(); //used int printStat to generate code to spwan dependent tasks

			boolean oldAllow = state.allowUnconditional;
			state.allowUnconditional = true;
			print("{");
			indent();
			nl();
			printStats(ps.getPathBlock().getStatements());
			undent();
			nl();
			print("}");
			state.allowUnconditional = oldAllow;
			state.path_outcom = old_outcom;
			state.sequentialEmitter.releaseGroup();
		} else {
			printStat(forloop.body);//missing task graph..can this happen?
		}

		state.insideParallelRegion = oldPar;
		state.domainIterState.noreturn = noret;


		if (state.domainIterState.reduce)//initial accumulator
		{
			print(", ");

			//cast initial accumulator to proper type to avoid conversion in LinearArray templates
			printInitialAccum(tree);

		}

		print(", ");

		JCExpression rhs = ((JCBinary) forloop.cond).rhs;

		printRange(((JCTree.JCAssign) ((JCTree.JCExpressionStatement) forloop.init.head).expr).rhs, rhs);

		if (tree.type.tag != TypeTags.VOID && !state.domainIterState.void_iter) {
			if (!state.domainIterState.reduce) {
				print(", __EXP_TMP" + state.domainIterState.uid);
				if (!at.treatAsBaseDomain()) {
					print(".object");
				}
			}
		}

		if (state.domainIterState.reduce&&false) {
			state.domainIterState.iter = replace(state.domainIterState.iter, tree.domargs.get(tree.domargs.size() - 1).sym, state.names.fromString("__ACCUM__"), state.names.fromString("__JOIN__"));
			if (!state.domainIterState.iter.nop) {
				print(", ");
				print(getCaptured(state.domainIterState.used, false, at.treatAsBaseDomain() && tree.domargs.size() > 1));

				print("(" + state.typeEmitter.getType(tree.body.type) + " __ACCUM__" + ", " + state.typeEmitter.getType(state.domainIterState.iter.body.type) + " __JOIN__" + ")->");
				if (!state.domainIterState.void_iter) {
					print(state.typeEmitter.getType(tree.body.type));
				} else {
					print("void");
				}

				printStat(forloop.body);
			}
		}

		print(");");

		if (state.domainIterState.reduce && state.domainIterState.noreturn) {
			Symbol accum = state.domainIterState.iter.domargs.get(state.domainIterState.iter.domargs.size() - 1).sym;

			nl();
			print(" " + accum.name + "=");
			printStat(state.domainIterState.reduceiter.body);
			print(";");

			nl();
			print("return " + accum.name + ";");

		}
	}

	String getCaptured(Set<VarSymbol> set, boolean forward, boolean offset) {
		StringBuilder result = new StringBuilder();

		result.append("[");

		result.append("&"); //just use default capture

		result.append("]");

		return result.toString();
	}

	//replace indices for dom iter -x
	<T extends JCTree> T replace(T cu, final Map<String, VarSymbol> map, boolean deepcopy) {
		DomainIterationReplaceTreeScanner v = new DomainIterationReplaceTreeScanner(map);
		if (deepcopy) {
			cu = (T) state.copy.copy(cu);
		}
		v.scan(cu);
		return cu;
	}

	public void visitTypeCast(JCTypeCast tree) {

		try {

			Type.ArrayType st = (Type.ArrayType) tree.expr.type.getArrayType();
			Type.ArrayType tt = null;
            
            if(!tree.clazz.type.toString().equals("String"))
                tt=(Type.ArrayType) tree.clazz.type.getArrayType();

			if (tt!=null&&st.dom == tt.dom) {
				return;
			}
			if (st.dom == null) {
				//from native to domain:
				//pass native as second arg to constructor of LinearArray:
				state.arrayEmitter.printNewArray(tree.pos(), tt.dom, tt.elemtype, tt.dom.appliedParams, tree.expr);
				return;
			} else if (tt==null||tt.dom == null) {
				//from domain to native:
				print("(" + state.typeEmitter.getTypePure(st.elemtype) + "*)");

				printExpr(tree.expr);
				//argument sets whether
				boolean atomic = st.elemtype.isPrimitive();
				if (!atomic) {
					atomic = ((st.elemtype.tsym.owner.flags_field & Flags.PARAMETER) == 0);
				}

				print("->toNative()");
				return;
			}
			printExpr(tree.expr);
			return;


		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}
	//do we need special handling for reinterpreted domain or can we use __VAL__

	//code gen for iteration over domains...
	public void visitDomIter(JCDomainIter tree) {


		LowerTreeImpl.IterState oldIterState = state.domainIterState.clone();

		state.domainIterState = new LowerTreeImpl.IterState(state.nestedIterations.size());

		state.nestedIterations.push(state.domainIterState);

		//setup domain iter state...
		state.domainIterState.iter = tree;
		state.domainIterState.enclosing = state.current_tree;

		Type.ArrayType at = (Type.ArrayType) tree.exp.type.getArrayType();

		Type.DomainType dt = ((Type.DomainType) tree.sym.type.clone());


		state.domainIterState.type = at;
		state.domainIterState.reduce = dt.tsym.name.toString().equals("reduce");

		if (!state.domainIterState.reduce && tree.params != null) {
			dt.appliedParams = tree.params;
		} else {
			dt.appliedParams = at.dom.appliedParams;
		}

		Type.ArrayType dat = (Type.ArrayType) at.clone();
		dat.dom = dt;
		state.domainIterState.domain = dat;

		List<JCVariableDecl> cur = tree.domargs;

		if (state.domainIterState.reduce) {
			ListBuffer<JCVariableDecl> buf = new ListBuffer<JCVariableDecl>();

			while (cur.tail.size() > 0) {
				buf.add(cur.head);
				cur = cur.tail;
			}

			cur = buf.toList();

			Type.ArrayType rt = at.getRealType();
			if ((at.type_flags_field&Flags.WORK)!=0||(at.dom.isBaseDomain && at.dom.getDim() != rt.dom.getDim())) {
				dt = at.dom;
			} else {
				dt = at.getRealType().dom;
			}
		}

		int flag = Type.ArrayType.getCodeGenFlags(tree, at);
        
		//call barvinok/cloog to get code that iterates over/inside domain/projection
		state.domainIterState.code = dt.codegen(tree.pos(), dt.appliedParams.toArray(new JCTree.JCExpression[0]), cur, flag,true,false,false);
		//Type.ArrayType rt = at.getRealType(); //will the real iteration pls stand up, if dom is reinterpreted, then rt!=at

		try {
			if (tree.body != null && tree.body.getTag() == JCTree.APPLY) {
				MethodSymbol s = (MethodSymbol) TreeInfo.symbol(tree.body);
				if ((s.flags_field & Flags.ACYCLIC) != 0 && s.getReturnType() != null && s.getReturnType().tag == TypeTags.ARRAY) {
					state.domainIterState.forwardCall = s;
				}
			}

			state.domainIterState.used = JCTree.usedVars(tree.body);

			if (!at.dom.isBaseDomain) {
				if (dt.projectionArgs != null) {
					for (VarSymbol vs : dt.projectionArgs) {
						state.domainIterState.used.remove(vs);
					}
				}
			}

			Map<String, VarSymbol> map = new LinkedHashMap<String, VarSymbol>();

			for (VarSymbol vs : state.domainIterState.used) {
				map.put(vs.toString(), vs);
			}

			if (dt.projectionArgs != null) {
				for (VarSymbol vs : dt.projectionArgs) {
					map.put(vs.toString(), vs);
				}
			}

			//replace def vars be used vars
			state.domainIterState.code = replace(state.domainIterState.code, map, true);

			JCTree.JCClassDecl classdecl = (JCTree.JCClassDecl) state.domainIterState.code.defs.get(1);
			JCTree.JCMethodDecl methd = (JCTree.JCMethodDecl) classdecl.defs.last();

			state.domainIterState.inner = inner_for(methd);

			state.domainIterState.index = apply_index(methd);

			if (state.domainIterState.inner != null) {
				state.domainIterState.inner_counter = (VarSymbol) TreeInfo.symbol(((JCTree.JCAssign) ((JCTree.JCExpressionStatement) state.domainIterState.inner.init.head).expr).lhs);

				state.domainIterState.used.remove(state.domainIterState.inner_counter);
			}
			else
			{
				int i=0;
			}

			if (!at.treatAsBaseDomain()) {
				for (JCTree.JCVariableDecl vd : cur) {
					state.domainIterState.used.remove(vd.sym);
				}
			}

			List<JCStatement> stats = methd.body.stats;

			state.domainIterState.counter = new LinkedHashSet<VarSymbol>();
			state.domainIterState.usedcounter = new LinkedHashSet<VarSymbol>();
			//skip decls (that were artficially added so we can parse cloog's code)
			while (stats.head.getTag() == JCTree.VARDEF) {
				state.domainIterState.counter.add(((JCVariableDecl) stats.head).sym);
				stats = stats.tail;
			}

			Set<VarSymbol> outer_symbols = new LinkedHashSet<VarSymbol>();
			outer_symbols.addAll(state.domainIterState.used);
			outer_symbols.removeAll(state.domainIterState.counter);

			Symbol from = TreeInfo.symbol(tree.exp);

			if (from instanceof VarSymbol) {
				outer_symbols.add((VarSymbol) from);
			}

			/*state.domainIterState.forwardCall!=null&&*/
			//do we actually produce a result?
			Type et=at.elemtype;
			if(at.isLinear())
				et=et.addFlag(Flags.LINEAR);
			state.domainIterState.void_iter = !state.domainIterState.reduce && (tree.body == null || (!state.types.isCastable(et, tree.body.type) && tree.type.tag != TypeTags.VOID));

			state.domainIterState.consecutive = true;
			boolean readonly = (tree.body == null || tree.type.tag == TypeTags.VOID || state.domainIterState.forwardCall != null);

			if (!state.domainIterState.reduce && !readonly) {

				if(state.domainIterState.inner != null)
				{
					state.domainIterState.consecutive = at.isCompatibleToRealTypeAndIterable(state.jc, at.dom, tree.pos());
					//check if we can apply a map on the last dimension
					//so the last dim must be indexed by the innermost loop and the index must not show up anywhere else
					if (state.domainIterState.consecutive) {
						List<JCExpression> args = state.domainIterState.index.args;
						while (!args.isEmpty()) {
							if ((!args.tail.isEmpty() && findIndex(args.head, state.domainIterState.inner_counter))
									|| (args.tail.isEmpty() && TreeInfo.symbol(args.head) != state.domainIterState.inner_counter)) {
								state.domainIterState.consecutive = false;
								break;
							}
							args = args.tail;
						}
					}
				}
				else
					state.domainIterState.consecutive=false;
			}
            
            if(state.domainIterState.forwardCall==null&&state.domainIterState.reduce&&tree.exp.type.isProjection())//take care not to overwrite stuff
            {
                //reduction must copy accumulator which destroys rest of the output if we generate directly into a projection
                state.domainIterState.consecutive=false;
                //code gen for assigning non primitive values not yet impl
            }
            
            if(state.domainIterState.forwardCall==null&&state.domainIterState.void_iter&&tree.body!=null&&tree.body.type.getArrayType() instanceof Type.ArrayType)
            {
                state.domainIterState.consecutive=false;
                //void iter 
            }
            

			//need special handling if return is used inside iter
			if (state.domainIterState.iter.mayReturn && state.inside_task == null) {
				nl();
				print("bool __lambda_return" + state.domainIterState.iter.pos + "=false;");
				if (!state.is_void && state.method.getReturnType() != null) {
					nl();
					state.typeEmitter.printType(state.method.getReturnType().type);
					print(" __lambda_return_value" + state.domainIterState.iter.pos + ";");
				}
			}

			//since the iteration is an expression we package into a lambda...
			nl();
			print("(" + getCaptured(outer_symbols, state.generate_into && tree.return_expression, false) + "()");
			//FIXME: ->proper type..needed for ICPC!
			String type;

			if (!at.getRealType().treatAsBaseDomain()) {
				type = state.typeEmitter.getType(at.getRealType());
			} else {
				type = state.typeEmitter.getType(at);
			}

//			state.domainIterState.consecutive=reduce||at.isCompatibleToRealTypeAndIterable(state.jc,at.dom, tree.pos());

			if(tree.type.tag==TypeTags.VOID)
				print("->" + state.typeEmitter.getType(tree.type));
			else
			{
				if (state.domainIterState.reduce) {
					print("->" + state.typeEmitter.getType(tree.body.type));
				} else {
					print("->" + type);
				}
			}

			nl();
			print("{");
			indent();

            Symbol rs=TreeInfo.rootSymbol(tree.exp);
            
            if(rs==null||rs instanceof VarSymbol)
            {
                nl();
                print(type);
                print("__VAL_TMP" + state.domainIterState.uid + "="); //get input into a defined var
                printExpr(tree.exp);
                print(";");
            }

			LowerTreeImpl.IterState baseIter=null;

			if (state.nestedIterations.size() > 1) {
				int prevIter=state.nestedIterations.size()-2;

				while(prevIter>0&&state.nestedIterations.elementAt(prevIter).reduce)
					prevIter--;

				baseIter=state.nestedIterations.elementAt(prevIter);
			}

			//get state.target into __EXP_TMP...
			if (state.domainIterState.reduce) {
				//if (!tree.type.isPrimitive())
				{
					//FIXME: reuse accum if unique?
					String val = "__VAL_TMP" + state.domainIterState.uid;
					if (tree.type.tag != TypeTags.VOID) {//handle state.target
						nl();
						print(type + "__EXP_TMP" + state.domainIterState.uid + "=");
						if (!at.treatAsBaseDomain()) {
							print(type + "(");
						}

						if (state.nestedIterations.size() > 1) {
							print("__EXP_TMP" + baseIter.uid);
							if (!baseIter.type.treatAsBaseDomain()) {
								print(".object");
							}
							if (!at.treatAsBaseDomain()) {
								//get proper object
								print(",");
								//printExpr(oldIterState.iter.exp);
								Type.ArrayType ot = (Type.ArrayType) baseIter.iter.iterType.getArrayType();

								JCExpression[] indices = new JCExpression[baseIter.iter.domargs.size()];

								int i = 0;
								for (JCVariableDecl vd : baseIter.iter.domargs) {
									indices[i] = state.jc.make.Ident(vd.sym);
									i++;
								}

								printDomainOffset(tree.pos(), baseIter.iter.exp, ot, indices, false, false, true);
								//ot.dom.ge
							}

							print(")");
						} else {
							print(val);
							if (!at.treatAsBaseDomain()) {
								print(", __VAL_TMP" + state.domainIterState.uid + ".offset)");
							}
						}


						print(";");
					}

					//copy initial accum into target and use target as accum!
					//special case for accum initial 0 (no need to copy anything as the GC automatically clears the allocated output)
					if (!tree.emptyInit&&!tree.type.isPrimitive()) {
						Type.ArrayType tp = (Type.ArrayType) tree.params.head.type.getArrayType();
						nl(); //just memcpy from accum to output
						print("memcpy(");
						print("__EXP_TMP" + state.domainIterState.uid);
						if (!at.treatAsBaseDomain()) {
							print(".object");
						}
						print("->toNative(),");
						printExpr(tree.params.head);
						if (!tp.treatAsBaseDomain()) {
							print(".object");
						}
						print("->toNative(),");

						if (at.dom.isDynamic() && tp.dom.isDynamic()) {
							printExpr(tree.params.head);
							if (!tp.treatAsBaseDomain()) {
								print(".object");
							}
							print("->getSizeDim()");
						} else {
							int size;
							if (!tp.dom.isDynamic()) {
								size = tp.dom.getCard(tree.pos(), tp.dom.appliedParams.toArray(new JCExpression[0]), true);
							} else {
								size = at.dom.getCard(tree.pos(), at.dom.appliedParams.toArray(new JCExpression[0]), true);
							}
							print("" + size);
						}

						print(");");

					}

				}
			} else if ((state.generate_into && tree.return_expression) || inside_joining) {//handle forwarding
				if (tree.type.tag != TypeTags.VOID) {
					nl();
					print(type + "__EXP_TMP" + state.domainIterState.uid + "=");

					if (!at.treatAsBaseDomain()) {
						print(type + "(");
					}

					print("__FORWARD__");

					//FIXME: do we need to add any offset here?
					/*
					 if (!at.treatAsBaseDomain()) {
					 print(", __VAL_TMP" + state.domainIterState.uid + ".offset");
					 }
					 */
					 if (!at.treatAsBaseDomain())
						print(")");
					print(";");
				}
			} else {//default case

				String val = "__VAL_TMP" + state.domainIterState.uid;
				if (tree.exp.getTag() != JCTree.NEWARRAY && tree.type.tag != TypeTags.VOID) {
					if (!at.treatAsBaseDomain()) {
						val += ".object"; //unpack projection
					}

					//we try to avoid copying if value is linear..unless "wild" stencil is used
					if (!state.domainIterState.reduce) {
						if (!tree.exp.type.isLinear() || tree.valueOffsetAccess) {
							val += "->getNewVersion()";
						}
					}
				}

				if (tree.type.tag != TypeTags.VOID) {//handle state.target
					nl();
					print(type + "__EXP_TMP" + state.domainIterState.uid + "=");
					if (!at.treatAsBaseDomain()) {
						print(type + "(");
					}

					if (state.nestedIterations.size() > 1) {
						print("__EXP_TMP" + baseIter.uid);
						if (!baseIter.type.treatAsBaseDomain()) {
							print(".object");
						}
					} else {
						print(val);
					}

					if (!at.treatAsBaseDomain()&&state.nestedIterations.size() > 1) {
						//get proper object
						print(",");

						Type.ArrayType ot=(Type.ArrayType)baseIter.iter.iterType.getArrayType();

						JCExpression[] indices=new JCExpression[baseIter.iter.domargs.size()];

						int i=0;
						for(JCVariableDecl vd:baseIter.iter.domargs)
						{
							indices[i]=state.jc.make.Ident(vd.sym);
							i++;
						}

						printDomainOffset(tree.pos(),baseIter.iter.exp, ot, indices , false, false, true);
						print(")");
					}
					else if (!at.treatAsBaseDomain()) {
						print(", __VAL_TMP" + state.domainIterState.uid + ".offset)");
					}


					print(";");
				}
			}

			if (at.dom.isDynamic()) {//set params (used by generated code) to the proper values
				Map<Symbol, String> oldmap=new LinkedHashMap<Symbol, String>();
				if(state.subst_map!=null)
					oldmap.putAll(state.subst_map);
				else
					state.subst_map=new LinkedHashMap<Symbol, String>();

				if(at.getBaseType().dom.isDynamic())
				{
					for(int i = 0; i < at.dom.formalParams.size(); i++)
					{
						Symbol sym=at.dom.formalParams.get(i);
						String s="__VAL_TMP" + state.domainIterState.uid;
						if (!at.treatAsBaseDomain()) {
							s+=".object";
						}
						s+="->getDim(" + i + ")";
						state.subst_map.put(sym,s);
					}
				}

				for (int i = 0; i < at.dom.appliedParams.size(); i++) {
					JCExpression e = at.dom.appliedParams.get(i);
					if (e.type.constValue() == null || (Integer) e.type.constValue() == -1) {
						nl();
						print("funky::uint32 " + at.dom.formalParams.get(i).name.toString() + "=");
						{
							//must use e with params replaced by dynamic values for dynamic arrays unless e==-1
							if(e.type.constValue()==null||((Integer)e.type.constValue())!=-1)
								printExpr(e);
							else
								print(state.subst_map.get(at.dom.formalParams.get(i)));
							print(";");
						}
					}
				}
				state.subst_map=oldmap;
			}

			boolean old_joining = inside_joining;
			inside_joining = false;
            if (state.jc.profile > 0) {
                ProfilingCodeGenerator proGen = new ProfilingCodeGenerator(state, tree);
                print(proGen.printProfileRegistration("WholeCPURun"));
                //dump iteration
                printStats(stats);
                print(proGen.printProfilingEnd());

            } else {
                //nl();//dump actual iteration
                printStats(stats);

            }


			inside_joining = old_joining;

			if (!state.domainIterState.reduce && tree.type.tag != TypeTags.VOID) {//ret result
				align();
				print("return __EXP_TMP" + state.domainIterState.uid + ";");
			}
			undent();
			nl();
			print("}) ()");//finish lambda

			//more handling of returning iter
			if (state.domainIterState.iter.mayReturn && state.inside_task == null) {
				print(";");

				nl();
				print("if(__lambda_return" + state.domainIterState.iter.pos + ")return ");
				if (!state.is_void && state.method.getReturnType() != null) {
					print("__lambda_return_value" + state.domainIterState.iter.pos);
				}
			}

		} catch (IOException e) {
			state.log.error(tree.pos(), "internal", "failed to find inner most for loop");
		} finally {
            //if((state.domainIterState.void_iter||state.domainIterState.reduce)&&!state.domainIterState.consecutive)
            oldIterState.consecutive=true; //no need to copy again??
			state.domainIterState = oldIterState;
			state.nestedIterations.pop();
		}

	}

	void printDomainOffset(DiagnosticPosition pos, JCTree domain, Type.ArrayType at, JCTree.JCExpression[] inds, boolean iter_inside_projection, boolean skip_inner_counter, boolean no_reinterprete) throws IOException {
            ArrayAccessPrinter arrayAccess = new ArrayAccessPrinter(state, this, this);

			Map<VarSymbol, JCExpression> oldmap=null;

			if(state.index_map!=null)
			{
				oldmap=new LinkedHashMap<VarSymbol, JCExpression>();
				oldmap.putAll(state.index_map);
			}

            arrayAccess.generateDomainOffsetCode(pos, domain, at, inds, iter_inside_projection, skip_inner_counter, no_reinterprete);

			if(oldmap!=null)
				state.index_map=oldmap;
	}

	public void printAccess(Type.ArrayType at, JCExpression obj, List<JCExpression> index, boolean no_reinterprete,boolean write, Type.ArrayType accessType) throws IOException {
		boolean rtcheck=state.jc.runTimeVerifyArrays;
        if(accessType==null)
            accessType=at;
		if (!accessType.useIndirection()) {
			print(".");
		} else {
			print("->");
		}

		if (at.dom.isDynamic() && index.size() > 1) {
			//use accessDim to access dynamically sized doms with >1 dim
			print("accessDim(");


			if(rtcheck)
			{
				//lambda for oob check
				print("__RTOOB(");
			}

			boolean first = true;
			for (int c = index.size() - 1; c >= 0; c--) //REVERTED ORDER
			{
				if (!first) {
					print(", ");
				}
				printExpr(index.get(c));
				first = false;
			}
		} else {
			if(write)
				print("get(");
			else
				print("access(");

			if(rtcheck)
			{
				//lambda for oob check
				print("__RTOOB(");
			}

			printDomainOffset(obj.pos(), obj, at, index.toArray(new JCTree.JCExpression[0]), true, false, no_reinterprete);

		}

		if(rtcheck)
		{
			print(", ");
			JCTree base=JCTree.baseArray(obj);
			printExpr(base, TreeInfo.postfixPrec);
			if (!((Type.ArrayType)base.type.getArrayType()).treatAsBaseDomain()) {
				print(".object");
			}


			print("->GetCount())");
		}

		print(")");

	}

	public void printSize(Type.ArrayType at,JCExpression base,JCExpression dimexp) throws IOException
	{
		Type.ArrayType rt = at.getRealType();

		int[] max = null;

		if (!rt.dom.isDynamic()) {//&&!at.getRealTypeDyn() brakes image test
			max = rt.dom.getSize(base.pos(), rt.dom.appliedParams.toArray(new JCTree.JCExpression[0]));
		} else {
			printExpr(base);
			if (!rt.useIndirection()) {
				print(".");
			} else {
				print("->");
			}
			print("getDim(");
			printExpr(dimexp);
			print(")");
			return;
		}

		int dim = at.dom.getDim();

		if (dim == 1) {
			if (max != null) {
				print((max[0]));
			} else {
				printExpr(base);
				print(rt.dom.getMaxDynamic());
			}
		} else {
			print("([&]()->int{");
			print("int size[" + at.dom.getDim() + "]={");

			boolean first = true;
			for (int s : max) {
				if (!first) {
					print(", ");
				}

				print((s));

				first = false;
			}

			print("};");

			print("return size[");
			printExpr(dimexp);
			print("];");
			print("}) ()");
		}
	}

	//array[i1,..,in]
	public void visitIndexed(JCArrayAccess tree) {
		try {



			Type.ArrayType at = ((Type.ArrayType) tree.indexed.type.getArrayType());

			//special case for .size[dim]
			if (at.dom.tsym.name.toString().equals("size")) {

				printSize(at,tree.indexed,tree.index.head);
				return;
			}


			//we expect pointer for non primitives
			if (!((Type.ArrayType) tree.indexed.type.getArrayType()).elemtype.isPointer() && !((Type.ArrayType) tree.indexed.type.getArrayType()).elemtype.isPrimitive()) {
				print("(&");
			}
			Map<VarSymbol, JCExpression> oldLastProjectionArgs = state.lastProjectionArgs;

			printExpr(tree.indexed, TreeInfo.postfixPrec);

			printAccess(at, tree.indexed, tree.index, false,false,null);

			if (!((Type.ArrayType) tree.indexed.type.getArrayType()).elemtype.isPointer() && !((Type.ArrayType) tree.indexed.type.getArrayType()).elemtype.isPrimitive()) {
				print(")");
			}

			state.lastProjectionArgs = oldLastProjectionArgs;

		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		} finally {
			//state.index_map = null;
		}
	}

	//put array into projection
	public String wrapProjection(boolean projection, Type elt, String t) {
		if (projection) {
			return "funky::Projection< " + state.typeEmitter.getTypeNoPointer(elt) + " >";
		} else {
			return t;
		}
	}

	//create projection for object
	public void newProjection(Type t, JCTree.JCExpression object, List<JCTree.JCExpression> args) throws IOException {
		print(state.typeEmitter.getType(t) + "(");
		printExpr(object);
		print(", ");

		Type.ArrayType at = ((Type.ArrayType) t.getArrayType());

		if (at.dom.projectionArgs != null && at.dom.projectionArgs.size() > 0) {
			printDomainOffset(object.pos(), object, at, args.toArray(new JCTree.JCExpression[0]), false, false, true);
		} else {
			print("0");
		}

		print(")");
	}
}
