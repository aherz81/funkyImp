/*
 * Copyright 2011-2012 TU-München
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 */
package com.sun.tools.javac.tree;

import java.io.*;
import java.util.*;

import com.sun.tools.javac.util.*;
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
//emitter for singular (event objects), uses taks emitter
public class LowerSingular extends Emitter{
	boolean inside_trigger = false; //currently dumping trigger?

// ------------------- actual code emitter ---------------
	public LowerSingular(LowerTreeImpl state) {
		super(state);
	}

	//event task is only executed if direct execution (by getting the lock) fails
	//or if the compiler deems the event worth running in parallel (FIXME: true??)
	public void printEvent(JCMethodDecl tree, String name, Set<iTask> paths) throws IOException {

		if (state.header) {
			print("DECLARE_TASK(" + name + "," + tree.getID() + "_Context);");
			nl();
			nl();
			return;
		}

		print("BEGIN_TASK(" + tree.sym.enclClass().name + "," + name + "," + tree.getID() + "_Context)");
		println();
		align();

		print("{");
		indent();
		println();
		align();

		state.inside_method = true;

		//get reader lock (with option to uprade)
		print("funky::sustained_rw_mutex::scoped_upgradeable_lock lock(context()->params.self->mutex);");
		println();
		align();

		state.current_group = "";
		state.inside_task = name;

		//generate code for event body (rturn this where .. will upgrade lock)
		if (state.spawns > 0) {
			state.taskEmitter.state.taskEmitter.genMethodTGBody(state.method, false, tree, paths);

			state.taskEmitter.state.taskEmitter.genMethodTGFooter(tree);
		} else {
			state.sequentialEmitter.genImplicitDecls(paths);//task_local vars must be generated here

			state.sequentialEmitter.genMethodBody(tree);

			state.sequentialEmitter.genMethodFooter(tree);
		}

		state.sequentialEmitter.releaseGroup();

		state.inside_method = false;

		println();
		undent();
		align();
		print("}");

		println();
		align();
		print("END_TASK()");
		println();
		align();
		println();
		align();

		state.inside_task = null;

	}

	//type and instance that can store trigger info
	public void printTriggerContext(JCMethodDecl tree) throws IOException {
		print("TRIGGER(" + tree.getID() + "Type" + tree.pos + ",{");

		for (JCVariableDecl d : tree.params) {
			print(state.typeEmitter.state.typeEmitter.getType(d.vartype.type, d.name.toString()) + "* " + d.name + ";");
		}

		if (tree.getReturnType().type.tag != TypeTags.VOID) {
			print(state.typeEmitter.state.typeEmitter.getType(tree.getReturnType().type, "ret") + " ret;");
		}

		print("}, " + tree.getID() + "Instance" + tree.pos+",");
  
        
//CALL_TASK_SETTrigger1168        
        
        print("CALL_CONTEXT_SET" + tree.name + tree.pos+");");

		println();
		align();
		println();
		align();
	}

	//gen state.method to set trigger values
	public void printTriggerSet(JCMethodDecl tree) throws IOException {
		String name = tree.getID() + "Instance" + tree.pos;

		//print(tree.getStringHeader());
		String ns = "";
		if (!state.header) {
			ns = tree.sym.enclClass().name + "::";
		}

		print("void " + ns);
		state.sequentialEmitter.visitMethodSymbol(tree.sym);
		print("(");

		for (JCVariableDecl d : tree.params) {
			if (d != tree.params.head) {
				print(", ");
			}
			if (d.vartype.type.isPrimitive()) {
				print(state.typeEmitter.state.typeEmitter.getType(d.vartype.type, d.name.toString()) + " " + d.name);
			} else {
				print(state.typeEmitter.state.typeEmitter.getType(d.vartype.type, d.name.toString()) + "* " + d.name);
			}
		}

		if (tree.getReturnType().type.tag != TypeTags.VOID) {
			if (!tree.params.isEmpty()) {
				print(", ");
			}
			print(state.typeEmitter.state.typeEmitter.getType(tree.getReturnType().type, "ret") + " ret");
		}

		print(")");

		if (state.header) {
			print(";");
			return;
		}

		println();
		align();
		print("{");
		println();
		indent();
		align();

		print("//"+name + ".start_write();");//futex based : MAY BLOCK INDEF
		println();
		align();

//NO LOCK REQUIRED:

		if (tree.getReturnType().type.tag != TypeTags.VOID) {
			print(name + ".ret=ret;");
			println();
			align();
		}

		for (JCVariableDecl d : tree.params) {
			print(name + "." + d.name + "=" + d.name);
			println();
			align();
		}

		print(name + ".finish_write();"); //release
		println();
		undent();
		align();
		print("}");
		println();
		align();
	}

	//gen state.method to get trigger values
	public void printTriggerGet(JCMethodDecl tree) throws IOException {
		String name = tree.getID() + "Instance" + tree.pos;

		String ns = "";
		if (!state.header) {
			ns = tree.sym.enclClass().name + "::";
		}

		print(state.typeEmitter.state.typeEmitter.getType(tree.getReturnType().type) + " " + ns);
		state.sequentialEmitter.visitMethodSymbol(tree.sym);
		print("(");

		boolean first = true;
		for (JCVariableDecl d : tree.params) {
			if (d != tree.params.head) {
				print(", ");
			}
			first = false;
			print(state.typeEmitter.state.typeEmitter.getType(d.vartype.type, d.name.toString()) + "* " + d.name);
		}

		if (!first) {
			print(", ");
		}
		print("int* TRIGGER__UID");

		print(")");

		if (state.header) {
			print(";");
			return;
		}

		println();
		align();
		print("{");
		println();
		indent();
		align();

		print("*TRIGGER__UID=" + name + ".start_read(*TRIGGER__UID);");//again: BLOCKING MVAR
		println();
		align();

//NO LOCK REQUIRED:

		if (tree.getReturnType().type.tag != TypeTags.VOID) {
			print(state.typeEmitter.state.typeEmitter.getType(tree.getReturnType().type, "retval") + " retval=" + name + ".ret;");
			println();
			align();
		}

		for (JCVariableDecl d : tree.params) {
			print("*" + d.name + "=" + name + "." + d.name);
			println();
			align();
		}
        
        nl();
        print(name+".lock();");
        nl();
        print("if(!"+name+".queueEmpty())");
        nl();        
        print("{");
        indent();
        nl();
            nl();
            print(name+".finish_read();");
            nl();
            print(name+".start_write();");
            nl();
            print("funky::Thread::SpawnTask< int >("+name+".dequeue());");
            nl();
            print(name+".unlock();");
            nl();
            print("return retval;");
/*            
        TriggerInstance1168.lock();
        if(!TriggerInstance1168.queueEmpty())
        {
            TriggerInstance1168.finish_read();
            TriggerInstance1168.start_write();

            funky::Thread::SpawnTask< int >(TriggerInstance1168.dequeue());                

            TriggerInstance1168.unlock();
            return retval;
        }
        TriggerInstance1168.unlock();        
*/      
        undent();
        nl();
        print("}");
        nl();
        print(name+".unlock();");
        nl();

		print(name + ".finish_read();"); //release
		println();
		align();

		if (tree.body != null) //code executed when get succeeded
		{
			if (!state.is_atomic && !state.method.uses_field.isEmpty()) //needs lock if fields are used
			{
				print("funky::sustained_rw_mutex::scoped_reader_lock lock(mutex);");
				println();
				align();
			}

			state.inside_method = true;

			state.printStats(tree.body.getStatements()); //signal readyness for next item or such, must ret void

			state.inside_method = false;

			if (!state.is_atomic && !state.method.uses_field.isEmpty())//unlock
			{
				align();
				print("lock.release();");
				println();
				align();
			}
		}

		if (tree.getReturnType().type.tag != TypeTags.VOID) {
			print("return retval;");
		}

		println();
		undent();
		align();
		print("}");
		println();
		align();
	}

	//special version of above (CALL_CONTEXT) for setting trigger values
	public void printCallContextTriggerSet(JCMethodDecl tree) throws IOException {
		print("struct CALL_CONTEXT_SET" + tree.name + tree.pos + " : public funky::ContextBase<int>");
		nl();
		print("{");
		indent();
		nl();

		for (JCVariableDecl d : tree.params) {
			state.typeEmitter.printType(d.type, d.name.toString());
			print(" " + d.name + ";");
			nl();
		}

		if (tree.getReturnType().type.tag != TypeTags.VOID) {
			state.typeEmitter.printType(tree.getReturnType().type, "retval");
			print(" retval;");
			nl();
		}

		if ((tree.mods.flags & Flags.STATIC) == 0) {
			state.typeEmitter.printType(tree.sym.owner.type);
			print(" OO;");
			nl();
		}

		print("CALL_CONTEXT_SET" + tree.name + tree.pos + "(");

		for (JCVariableDecl d : tree.params) {
			if (d != tree.params.head) {
				print(", ");
			}
			state.typeEmitter.printType(d.vartype.type, d.name.toString());
			print(" " + d.name);
		}
		if (tree.getReturnType().type.tag != TypeTags.VOID) {
			if (tree.params.size() != 0) {
				print(", ");
			}
			print(state.typeEmitter.getType(tree.getReturnType().type, "retval") + " retval");
		}
		if ((tree.mods.flags & Flags.STATIC) == 0) {
			if (tree.params.size() != 0 || tree.getReturnType().type.tag != TypeTags.VOID) {
				print(", ");
			}
			state.typeEmitter.printType(tree.sym.owner.type);
			print(" OO");
		}

		print("): funky::ContextBase<int>(0xffffffff, true)");

		for (JCVariableDecl d : tree.params) {
			print(", ");
			print(d.name + "(" + d.name + ")");
		}

		if (tree.getReturnType().type.tag != TypeTags.VOID) {
			print(", ");
			print("retval(retval)");
		}

		if ((tree.mods.flags & Flags.STATIC) == 0) {
			print(", ");
			print("OO(OO)");
		}

		print("{}");
		undent();
		nl();
		print("};");
		nl();
		nl();

	}

	//generate task to call trigger set state.method
	public void printCallTaskTriggerSet(JCMethodDecl tree) throws IOException {
		if (state.header) {
			print("DECLARE_CONST_TASK(CALL_TASK_SET" + tree.name + tree.pos + "," + "CALL_CONTEXT_SET" + tree.name + tree.pos + ");");
			nl();
			nl();
			return;
		}

		print("BEGIN_TASK(" + tree.sym.enclClass().name + "," + "CALL_TASK_SET" + tree.name + tree.pos + "," + "CALL_CONTEXT_SET" + tree.name + tree.pos + ")");
		nl();
		print("{");
		indent();
		nl();

		if ((tree.mods.flags & Flags.STATIC) == 0) {
			print("context()->OO->");
		}

		state.sequentialEmitter.visitMethodSymbol(tree.sym);
		print("(");

		for (JCVariableDecl d : tree.params) {
			if (d != tree.params.head) {
				print(", ");
			}
			print("context()->" + d.name);
		}

		if (tree.getReturnType().type.tag != TypeTags.VOID) {
			if (tree.params.size() > 0) {
				print(", ");
			}
			print("context()->retval");
		}

		print(");");

		undent();
		nl();
		print("}");
		nl();
		print("END_TASK()");
		nl();
		nl();
	}

	public void preparePaths(JCMethodDecl tree) throws IOException {
		if (!state.is_atomic && state.is_event && tree.getBody().getStatements().size() > 0) {
			if (state.header) {
				if (!state.context_names.contains(tree.getID())) {
					state.context_names.add(tree.getID());
					state.sequentialEmitter.printContext(tree, new LinkedHashSet<VarSymbol>(), new LinkedHashSet<iTask>());
				}
			}

			String name = tree.getID() + "_Event" + tree.getBody().getStatements().head.getTaskID();
			state.path_outcom = null;
			state.singularEmitter.printEvent(tree, name, state.methodpaths);
		}
	}

	public void handleExit(JCMethodDecl tree) throws IOException
	{
		if (!state.is_atomic && state.is_event && (!state.method.uses_field.isEmpty() || state.method.sym.isSampling)) { //if lock aquisition fails, we defer the event (spawn corresponding task)
			nl();
			print("return;");
			undent();
			nl();
			print("}");
			println();
			align();

			state.taskEmitter.genMethodTGHeader(tree,tree, true, true);
			//nl();
			println();
			align();
            
            //FIXME: should block here of evo has to many pending operations
            
			String name = tree.getID() + "_Event" + tree.getBody().getStatements().head.getTaskID();
			print("funky::Thread::SpawnTask<int>(new(tbb::task::allocate_additional_child_of(*funky::TaskRoot<>::GetRoot())) " + name + "(context));");
			println();

			state.loop_label_sched = null;
			state.loop_label_kernel = false;

		}
	}

	public void handleTrigger(JCMethodDecl tree, boolean is_trigger) throws IOException {

		//trigger set context
		if (state.header) {
			printCallContextTriggerSet(tree);//constructor with args
		}
		printCallTaskTriggerSet(tree);

		if (state.header) {
			printTriggerContext(tree); //storage to hold current trigger values
		}

		inside_trigger = true;

		//emit trigger get/set methods
		printTriggerSet(tree);

		nl();

		printTriggerGet(tree);

		inside_trigger = false;
	}

		public void enterSample() throws IOException {
		//aquire lock if necessary
		if (!state.is_atomic && (state.method.mods.flags & Flags.PRIVATE) == 0 && state.method.sym.isSampling) //no need to lock if the state.method can be called only from already locked methods
		{
			align();
			print("funky::sustained_rw_mutex::scoped_reader_lock lock(mutex);");
			println();
		}
	}

}
