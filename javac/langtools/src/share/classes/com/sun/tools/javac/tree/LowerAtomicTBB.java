/*
 * Copyright 2011-2012 TU-München
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 */
package com.sun.tools.javac.tree;

import java.io.*;

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
//emitter for atomic (singulars), uses TBB
public class LowerAtomicTBB extends Emitter{

	public LowerAtomicTBB(LowerTreeImpl state) {
		super(state);
	}

	//try to emit atomic ops:
	void printAtomic(VarSymbol find, VarSymbol replace, JCTree computation, VarSymbol target, boolean emit_target) throws IOException {
		switch (computation.getTag()) {
			case JCTree.IDENT://simple assign!
			case JCTree.LITERAL://simple assign!
				print("=");
				printExpr(computation);
				break;
			case JCTree.MINUS: {
				JCBinary op = (JCBinary) computation;
				JCExpression lhs, rhs;

				Symbol s = TreeInfo.symbol(op.lhs);
				if (s == find) {
					s = replace;
				}

				if (s != target) {
					lhs = op.rhs;
					rhs = op.lhs;
				} else {
					lhs = op.lhs;
					rhs = op.rhs;
				}

				state.symbolFind = find;
				state.symbolReplace = replace;

				//lhs is target
				if (emit_target) {
					printExpr(lhs);
				}

				print(".fetch_and_add(-");
				printExpr(rhs);
				print(")");

				if (!(find != null || replace != null))//postinc
				{
					print("-");//calc post value!
					printExpr(rhs);
				}

				state.symbolFind = null;
				state.symbolReplace = null;

				break;
			}
			case JCTree.PLUS: {
				JCBinary op = (JCBinary) computation;
				JCExpression lhs, rhs;

				Symbol s = TreeInfo.symbol(op.lhs);
				if (s == find) {
					s = replace;
				}

				if (s != target) {
					lhs = op.rhs;
					rhs = op.lhs;
				} else {
					lhs = op.lhs;
					rhs = op.rhs;
				}

				state.symbolFind = find;
				state.symbolReplace = replace;

				//lhs is target
				if (emit_target) {
					printExpr(lhs);
				}

				print(".fetch_and_add(");
				printExpr(rhs);
				print(")");

				if (!(find != null || replace != null))//postinc
				{
					print("+");//calc post value!
					printExpr(rhs);
				}

				state.symbolFind = null;
				state.symbolReplace = null;

				break;
			}
			default://tbb supports only fetch_and_add/comp&swap/swap
				state.log.error(computation.pos, "non.atomic.computation", computation);
		}
	}

	//translate op into atomic op
	public boolean handleAtomic(VarSymbol lvs, JCTree rhs, VarSymbol rvs) throws IOException {
		if (state.method == null || state.method.atomic_where == null) {
			return false;
		}

		switch (state.method.atomic_where.atomic.type) {
			case POSTINC:
				if (lvs == (VarSymbol) TreeInfo.symbol(state.method.atomic_where.atomic.temp))//temp must be VarSymbol!
				{
					print(" = ");
					printAtomic(null, null, rhs, state.method.atomic_where.atomic.target, true);//do rhs as atomic but swap first by second symbol
					state.atomic_processed = true;
					return true;//found the setup
				}
				if (rvs == (VarSymbol) state.method.atomic_where.atomic.target)//was swap?
				{
					print("=" + rvs + ".fetch_and_store(" + state.method.atomic_where.atomic.temp + ")");
					state.atomic_processed = true;
					return true;//found the setup
				}
				break;
			case INC:
				if (lvs == (VarSymbol) state.method.atomic_where.atomic.target)//temp must be VarSymbol!
				{
					//print(" = ");
					printAtomic(null, null, rhs, state.method.atomic_where.atomic.target, false);//do rhs as atomic but swap first by second symbol
					state.atomic_processed = true;
					return true;//found the setup
				}
				break;
			case PREINC:
				if (rvs == state.method.atomic_where.atomic.target)//temp is atomic op (a+b),...
				{
					//extract op from state.method.atomic_where.atomic
					print(" = ");
					printAtomic(lvs, rvs, state.method.atomic_where.atomic.temp, state.method.atomic_where.atomic.target, true);//do rhs as atomic but swap first by second symbol
					state.atomic_processed = true;
					return true;//found the setup
				}
				break;

			case CMPXCHG:
				if (lvs == state.method.atomic_where.atomic.target) {
					print(".compare_and_swap(" + state.method.atomic_where.atomic.temp + "," + state.method.atomic_where.atomic.comp + ")");
					if (state.method.atomic_where.atomic.aflag != Flags.ATOMIC)//do we want the value or the boolean version?
					{
						print("==" + state.method.atomic_where.atomic.temp);
					}
					state.atomic_processed = true;
					return true;
				}
				break;
		}


		return false;
	}

}
