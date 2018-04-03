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
//emitter for TBB/RT based tasks, also used for singulars
public class LowerTasksTBB extends Emitter {

	Map<VarSymbol, Set<iTask>> signal_map; //which paths depend on var symbol
	//ArrayList<iTask> dangling_paths = null; //was used fro dbg output
	boolean blocking_only = false; //are all spawns blocking?
	boolean branch_wait = false; //branches must do their own waiting

// ------------------- actual code emitter ---------------
	public LowerTasksTBB(LowerTreeImpl state) {
		super(state);
	}

	//spawn tasks that depend on result of tree
	void SpawnDependentTasks(JCTree tree) throws IOException {
		//spawn dependen tasks:
		if (state.methodpaths != null && state.method.depGraph.vertexSet().contains(tree)) {
			Set<Arc> out = state.method.depGraph.outgoingEdgesOf(tree); //get outgoing edges of graph from current node

			Set<VarSymbol> done = new LinkedHashSet<VarSymbol>();

			boolean is_empty = true;

			//check if any of the outgoing edges need signaling
			if (state.path_outcom != null) {
				for (Arc a : out) {
					if (state.path_outcom.contains(a.v) && !done.contains(a.v) && a.t.getTag() != JCTree.SKIP) {
						if (!is_empty) {
							break;
						}
						Set<iTask> set = signal_map.get(a.v);
						if (set != null) {
							for (iTask p : set) {
								if (!state.task_map.get(p).equals(state.inside_task) && (!blocking_only || p.containsForcedSpawn())) {//no self notification!
									is_empty = false;
									break;
								}
							}
						}
					}
				}
			}

			//FIXME: open kernel once and state.close as needed
			JCTree old_label_sched = state.loop_label_sched;
			boolean old_label_kernel = state.loop_label_kernel;

			if (state.path_outcom != null && !is_empty) {
				if (state.kernel && !blocking_only&&state.target.coreCount>1) { //emit two cases, one for state.kernel one std
					nl();
			int max_tasks=1024;
			int max_spwans=state.method.maxRecSpawn;
			int cores=(int) Math.min(10, state.target.coreCount * 2.5);
			print("if(__KERNEL_LEVEL<" + Math.min(max_tasks,(int)(Math.log(max_tasks)/Math.log(Math.min(max_spwans,cores)))) + ") {");
					indent();
					nl();

					if (!state.method_has_context) {
						state.kernel = false;
						state.inside_kernel = true;
						int joining = state.joining_paths;
						boolean taskret = state.task_return;
						state.joining_paths = state.kernel_joining_paths;
						state.task_return = state.kernel_task_return;

						genMethodTGHeader(state.method,tree.scheduler, false, true);
						state.joining_paths = joining;
						state.task_return = taskret;
						state.inside_kernel = false;
						state.kernel = true;
					}

					nl();
                    if(state.inside_task!=null)
                        print("task_handle* self_task=&SELF();");
                    else
                        print("task_handle* self_task=new (tbb::task::allocate_root()) tbb::empty_task();");
				}

				MethodSymbol old_redirect = state.redirect_recursion;

				for (int i = 0; i < 2; i++) {
					if (!state.kernel) {
						i++;
					} else if (i == 1 && !blocking_only) {
						undent();
						nl();
						print("} else {");
						indent();
						nl();
						if (state.method.restricted_impls.get(1) != null) { //use _IMPL1 when not in state.kernel
							state.redirect_recursion = state.method.restricted_impls.get(1).sym;
						}

					}
					Set<iTask> notified = new LinkedHashSet<iTask>();
					for (Arc a : out) { //find outgoing com:
						if (state.path_outcom.contains(a.v) && !done.contains(a.v) && a.t.getTag() != JCTree.SKIP) {
							Set<iTask> set = signal_map.get(a.v); //which paths wait on us?
							if (set != null) {
								for (iTask p : set) {
									if (!notified.contains(p)) { //did we already notify this task?
										notified.add(p);
										if (!state.task_map.get(p).equals(state.inside_task) && (!blocking_only || p.containsForcedSpawn())) {//no self notification!
											if (state.in_map.get(p) > 1 || state.joining_map.get(p)) { //do we refcount?
												if (state.in_map.get(p) > 1)//multiple inbound dependencies, do refcounting
												{
													state.DumpSpawn(tree, p, true, state.task_map);
													print("if(" + state.sequentialEmitter.context() + "->tasks.task_instance_" + state.task_map.get(p) + "->decrement_ref_count()==0)");

													indent();
													println();

													align();
												} else {
													state.DumpSpawn(tree, p, false, state.task_map);
												}

												//spawn pre-allocated (MethodTGBody) task
												if (p.getThreads().size() < 1) {

													print(state.sequentialEmitter.self_task() + "spawn(*" + state.sequentialEmitter.context() + "->tasks.task_instance_" + state.task_map.get(p) + ");");
												} else {
													String sspawn = "funky::Thread::SpawnTask<" + state.typeEmitter.getTypeNoVoid(state.method.type.getReturnType(), "unkown") + " >(" + state.sequentialEmitter.context() + "->tasks.task_instance_" + state.task_map.get(p)
															+ ", TaskThreadInitializer_" + state.current_class.replace('.', '_') + "::getThread" + p.getThreads().iterator().next().toString() + "())";
													print(sspawn + ";");
												}

												if (state.in_map.get(p) > 1) {
													undent();
												}
											} else//special case for non joining with 1 dependency (skip the refcount check and alloc task here)
											{
												state.DumpSpawn(tree, p, false, state.task_map);
												String task_name = state.task_map.get(p);
												println();
												align();
												if (!state.is_evo) {
													if (p.getThreads().size() < 1) {
														print(state.sequentialEmitter.self_task() + "spawn(*new(tbb::task::allocate_additional_child_of(*funky::TaskRoot<>::GetRoot())) " + task_name + "(" + state.sequentialEmitter.context() + "));");
													} else {
														String sspawn = "funky::Thread::SpawnTask<" + state.typeEmitter.getTypeNoVoid(state.method.type.getReturnType(), "unkown") + " >(new(tbb::task::allocate_additional_child_of(*funky::TaskRoot<>::GetRoot())) " + task_name + "(" + state.sequentialEmitter.context() + ")"
																+ ", TaskThreadInitializer_" + state.current_class.replace('.', '_') + "::getThread" + p.getThreads().iterator().next().toString() + "())";
														print(sspawn + ";");
													}

												} else {
													if (p.getThreads().size() < 1) {
														print(state.sequentialEmitter.self_task() + "spawn(*" + state.sequentialEmitter.context() + "->tasks.task_instance_" + state.task_map.get(p) + ");");//state.method body waits for this
													} else {
														String sspawn = "funky::Thread::SpawnTask<" + state.typeEmitter.getTypeNoVoid(state.method.type.getReturnType(), "unkown") + " >(state.sequentialEmitter.context()->tasks.task_instance_" + state.task_map.get(p)
																+ ", TaskThreadInitializer_" + state.current_class.replace('.', '_') + "::getThread" + p.getThreads().iterator().next().toString() + "())";
														print(sspawn + ";");

													}
												}
											}
											println();
											align();
										} else {
											assert (false);
										}
									}
								}
							}
							done.add(a.v);
						}
					}

					if (state.kernel && i == 1 && !blocking_only) {
						state.kernel = false;
						state.inside_kernel = true;
						int joining = state.joining_paths;
						boolean taskret = state.task_return;
						state.joining_paths = state.kernel_joining_paths;
						state.task_return = state.kernel_task_return;

						genMethodTGFooter(state.method);

						state.loop_label_sched = old_label_sched;
						state.loop_label_kernel = old_label_kernel;

						state.redirect_recursion = old_redirect;
						state.joining_paths = joining;
						state.task_return = taskret;
						state.inside_kernel = false;
						state.kernel = true;

						undent();
						nl();
						print("}");
					}
				}
			}
		}
	}

	public void visitIf(JCIf tree) {
		try {
			Set<iTask> paths = tree.getSchedule();

			iTask true_part = null;
			iTask false_part = null;

			//filter the paths that are control flow dependent on this branch (DO NOT JUST USE THE TRUE/FALSE BRANCH STORED IN THE AST)
			for (iTask p : paths) {
				Set<JCTree> calc_nodes = p.getCalcNodes();
				if (!calc_nodes.isEmpty()) {
					JCTree fcn = p.getFirstCalcNode(state.method, calc_nodes);

					if (fcn.scheduler == tree && fcn.getTag() == JCTree.CF) {
						if (((JCCF) fcn).value) {
							if (true_part != null) {
								state.log.error(tree.pos, "internal.double.branch");
							}

							true_part = p;
						} else {
							if (false_part != null) {
								state.log.error(tree.pos, "internal.double.branch");
							}

							false_part = p;
						}
					}
				}
			}

			Set<iTask> branches = new LinkedHashSet<iTask>();
			branches.add(true_part);
			state.DumpSchedule(tree, true_part, state.task_map, state.dump_kernel);
			state.DumpPath(true_part, state.task_map);

			Set<VarSymbol> old_outcom = state.path_outcom;
			state.path_outcom = true_part.getNullFreeOutSymbols(); //used int printStat to generate code to spwan dependent tasks

			nl();
			print("{");
			indent();
			nl();

			if (!state.is_event && state.inside_task != null && state.method.final_value != null && tree.elsepart != null && tree.elsepart.transitive_returns > 0 && state.task_return)//must refcount possible exits
			{
				nl();
				if (state.method.transitive_returns > 1) {
					print("if((" + state.sequentialEmitter.context() + "->exitcount-=" + tree.elsepart.transitive_returns + ")==0)");
				}
				print("context()->tasks.task_return->decrement_ref_count();");
			}

			boolean old_wait = branch_wait;
			branch_wait = false;//we must do our own waiting!

			printStats(true_part.getPathBlock().getStatements());

			if (branch_wait) {//&& !true_part.getOutSymbols(true_part.getCalcNodes()).isEmpty() we must wait anyways!!
				nl();
				print("" + state.sequentialEmitter.self_task() + "wait_for_all();"); //spawn indep tasks and wait for joining tasks to dec our refc
                if(state.inside_task==null)
                {
                    nl();
                    print("tbb::task::destroy(" + state.sequentialEmitter.self_task_name() + ");"); 
                }
			}
			branch_wait = old_wait;

			state.sequentialEmitter.releaseGroup();

			undent();
			nl();
			print("}");
			state.path_outcom = old_outcom;

			if (false_part != null) {
				state.DumpSchedule(tree, false_part, state.task_map, state.dump_kernel);
				state.DumpPath(false_part, state.task_map);

				branches.clear();
				branches.add(false_part);
				state.path_outcom = false_part.getNullFreeOutSymbols(); //used int printStat to generate code to spwan dependent tasks

				nl();
				print("else");
				nl();
				print("{");
				indent();
				nl();

				if (!state.is_event && state.inside_task != null && state.method.final_value != null && tree.thenpart.transitive_returns > 0 && state.task_return)//must refcount possible exits
				{
					nl();
					if (state.method.transitive_returns > 1) {
						print("if((" + state.sequentialEmitter.context() + "->exitcount-=" + tree.thenpart.transitive_returns + ")==0)");
					}
					print("context()->tasks.task_return->decrement_ref_count();");
				}

				old_wait = branch_wait;
				branch_wait = false;

				printStats(false_part.getPathBlock().getStatements());

				if (branch_wait) {//&& !false_part.getOutSymbols(false_part.getCalcNodes()).isEmpty()
					nl();
					print("" + state.sequentialEmitter.self_task() + "wait_for_all();"); //spawn indep tasks and wait for joining tasks to dec our refc
                    if(state.inside_task==null)
                    {
                        nl();
                        print("tbb::task::destroy(" + state.sequentialEmitter.self_task_name()+ ");"); 
                    }
				}

				branch_wait = old_wait;
				state.sequentialEmitter.releaseGroup();

				undent();
				nl();
				print("}");
				state.path_outcom = old_outcom;
			} else {
				nl();
				print("else");
				nl();
				print("{");
				indent();
				nl();
				if (state.inside_task != null && state.method.final_value != null && tree.thenpart.transitive_returns > 0 && state.task_return)//must refcount possible exits
				{

					nl();
					if (!state.is_event) {
						if (state.method.transitive_returns > 1) {
							print("if((" + state.sequentialEmitter.context() + "->exitcount-=" + tree.thenpart.transitive_returns + ")==0)");
						}
						print("context()->tasks.task_return->decrement_ref_count();");
					}

				}
				undent();
				nl();
				print("}");
			}
		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

	//emit task
	public JCTree printTask(JCMethodDecl tree, iTask p, JCBlock b, String name, Set<VarSymbol> inner, boolean dangling) {
		try {
			if (state.header) {
				print("DECLARE_TASK(" + name + "," + tree.getID() + "_Context);");
				nl();
				nl();
				return b.getStatements().last();
			}

			state.inside_task = name;

			print("BEGIN_TASK(" + tree.sym.enclClass().name + "," + name + "," + tree.getID() + "_Context)");

			println();
			align();

			print("{");
			indent();
			println();
			align();

			state.inside_method = true;

			if (state.profile > 0) {
				float task_work = state.work.getWork(p, state.method);
				float task_cor = state.work.getCorrection(p, state.method);
				String task_desc = "";
				for (JCStatement s : b.getStatements()) {
					task_desc += s.getTaskID() + ",";
				}
				nl();
				print("static funky::ProfileEntry* __PROFILE_ENTRY=funky::Profile::Register(\"" + name + "\",\"" + tree.sym.owner.name + "\",\"" + task_desc + "\"," + task_work + "," + task_cor + ");");
				nl();
				//state.profile_ENTRY->
				print("tbb::tick_count START_TASK=tbb::tick_count::now();");
			}

			state.current_group = "";

			if (state.debug_print_task) {
				nl();
				print("funky::Debug::StartTask(" + b.getStatements().head.getTaskID() + ");");
				print("printf(\"Task(%s):%016p on Thread(%016p)\\n\",\"" + name + "\",this,funky::Thread::getCurrentThread());");
			}

			//declare task local variables (and implicit decls a'b)
			for (VarSymbol vs : inner) {
				if (vs.tasklocal() && (vs.flags() & Flags.IMPLICITDECL) != 0 && vs.isVariable()) {
					state.typeEmitter.printType(vs);

					print(" ");
					state.sequentialEmitter.visitVarSymbol(vs, false);
					print(";");

					println();
					align();
				}
			}

			state.inner_symbols = inner;
			//emit code for task
			println();
			printStats(b.getStatements());

			state.inner_symbols = null;

			state.sequentialEmitter.releaseGroup();

			if (state.profile > 0) {
				nl();
				print("tbb::tick_count END_TASK=tbb::tick_count::now();");
				nl();
				//export time in us!
				print("__PROFILE_ENTRY->AddMeasurement((END_TASK - START_TASK).seconds()*1e6);"); //time in us
			}


			if (state.debug_print_task) {
				nl();
				print("printf(\"ExitTask(%s):%016p on Thread(%016p)\\n\",\"" + name + "\",this,funky::Thread::getCurrentThread());");
				nl();
				print("funky::Debug::FinishTask(" + b.getStatements().head.getTaskID() + ");");
			}

			state.inside_method = false;


			if (tree.context_ref_count || state.is_event) {
				nl();
				print("context()->release();");
			}
			undent();
			nl();
			print("}");

			println();
			align();
			print("END_TASK()");
			println();
			align();
			println();
			align();

			state.inside_task = null;

			return b.getStatements().last();


		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

	//context is a struct with 3 parts: state.method parameters, (task-shared) local params and task handles
	public void printContext(JCMethodDecl tree, Set<VarSymbol> com, Set<iTask> waiting_tasks) {
		try {

			print("CONTEXT(" + state.typeEmitter.getResType(tree.restype) + ", " + tree.getID() + "_Context,{");

			//implicit this parameter
			if ((tree.mods.flags & Flags.STATIC) == 0) {
				state.typeEmitter.printType(tree.sym.owner.type);
				print("self;");
			}

			//state.method parameters
			for (VarSymbol vs : tree.sym.params) {
				state.typeEmitter.printType(vs);
				if (!(vs.type instanceof Type.MethodType)) {
					print(" " + vs.name.toString().replace('\'', '_'));
				}
				print(";");
			}

			print("},{");

			//local vars
			for (VarSymbol vs : tree.local_vars) {
				if ((vs.flags() & Flags.PARAMETER) == 0 && vs.isLocal()) {
					if (com.contains(vs)) { //is it shared??
						state.typeEmitter.printType(vs);
						if (!(vs.type instanceof Type.MethodType)) {
							print(" " + vs.name.toString().replace('\'', '_'));
						}
						print(";");
					} else {
					}
				}
			}

			print("},{");
			//task handles

			for (iTask p : waiting_tasks) {
				int incom = state.in_map.get(p);
				boolean joining = state.joining_map.get(p);

				if (joining || incom != 1 || state.is_evo)//incom==1 optimized away, is spawned locally
				{
					print("task_handle* task_instance_" + state.task_map.get(p) + ";");
				}
			}

			//void methods my have to wait also
			if (state.method.transitive_returns > 0 && !(state.is_event || state.is_evo || state.is_constructor)) {
				print("task_handle* task_return;");
			}

			print("});");
			println();
			align();
			println();
			align();

		} catch (IOException e) {
			throw new LowerTree.UncheckedIOException(e);
		}
	}

//emit state.method entry (allocate and set context)
	public void genMethodTGHeader(JCMethodDecl tree,JCTree scheduler, boolean init_self, boolean label) throws IOException {

		println();
		align();

		if (!init_self) {
			print("task_handle* self_task;");
		} else {
            if(state.inside_task!=null)
                print("task_handle* self_task=&SELF();");
            else
                print("task_handle* self_task=new (tbb::task::allocate_root()) tbb::empty_task();");
		}

		if ((tree.sym.flags_field & Flags.LOOP) != 0 && label && state.loop_label_sched == null) {
			state.loop_label_sched = state.current_scheduler;
			state.loop_label_kernel = false;
			nl();
			print("LOOP_LABEL");
			print("" + state.current_scheduler.pos);
			print(":");
			nl();
		}

		println();
		align();

		state.method_has_context = true;

		if (state.is_context_refcount) { //void doesn't wait unless finally is used

			if (state.kernel || state.inside_kernel) {
				print(tree.getID() + "_Context* context=new " + tree.getID() + "_Context(__KERNEL_LEVEL");
			} else {
				print(tree.getID() + "_Context* context=new " + tree.getID() + "_Context(" + 0);
			}

			print(",false);");
			nl();
			print("funky::localContext<" + state.typeEmitter.getResType(state.method.restype) + " > lc(context);");

		} else //must be joining and event/sample can use stack allocated context
		{
			if (!state.is_event)//samples can use stack allocated context
			{
				print(tree.getID() + "_Context context_instance=" + tree.getID() + "_Context(");
				if (state.kernel || state.inside_kernel) {
					print("__KERNEL_LEVEL,false,false);");
				} else {
					print("0,false,false);");
				}
				print(tree.getID() + "_Context* context=&context_instance;");
			} else //event may be defered..must heap alloc
			{
				print(tree.getID() + "_Context* context=new " + tree.getID() + "_Context(1,false);");
				nl();
				print("funky::localContext<" + state.typeEmitter.getResType(state.method.restype) + " > lc(context);");
			}
		}

		if (state.current_scheduler.transitive_returns > 0 && tree.getAllSchedules().size() > 1 && tree.final_value != null && tree.transitive_returns > 1)//must refcount possible exits
		{
			nl();
			print("context->exitcount=" + tree.transitive_returns + ";");
		}

		//assign this to context, this may be const so we cast it away (for e.g. tail rec loops)
		if ((tree.mods.flags & Flags.STATIC) == 0) {
			println();
			align();
			if (!state.use_local_this) {
				print("context->params.self=const_cast<" + state.method.sym.owner.type + "*>(this);");
			} else {
				print("context->params.self=const_cast<" + state.method.sym.owner.type + "*>(lthis);");
			}
		}

		//init params
		for (VarSymbol vs : tree.sym.params) {
			println();
			align();
			print("context->params." + vs.name.toString().replace('\'', '_') + "=" + vs.name.toString().replace('\'', '_') + ";");
		}

		if(scheduler!=tree)
		{
			Set<iTask> paths = scheduler.getSchedule();

			Set<VarSymbol> frame=new LinkedHashSet<VarSymbol>();
			Set<VarSymbol> out=new LinkedHashSet<VarSymbol>();

			for(iTask t:paths)
			{
				out.addAll(t.getOutSymbols());
				Set<VarSymbol> syms=t.getInSymbols();
				syms.retainAll(state.com);
				frame.addAll(syms);
			}

			frame.removeAll(out);

			for (VarSymbol vs : frame) {
				if((vs.flags_field&Flags.PARAMETER)==0)
				{
					println();
					align();
					print("context->frame." + vs.name.toString().replace('\'', '_') + "=" + vs.name.toString().replace('\'', '_') + ";");
				}
			}
		}
	}

	public void genMethodBodyCore(JCTree scheduler, boolean no_wait, JCMethodDecl tree, Set<iTask> paths) throws IOException {
		//no parallelism but we must spawn blocking anyways
		Set<iTask> blocking = new LinkedHashSet<iTask>();
		Set<iTask> nonblocking = new LinkedHashSet<iTask>();

		Set<VarSymbol> block_outcom = new LinkedHashSet<VarSymbol>();
		Set<VarSymbol> nonblock_outcom = new LinkedHashSet<VarSymbol>();

		//find blocking tasks
		int old_joining = state.joining_paths;
		for (iTask ps : paths) {

			if (ps.getFirstCalcNode(state.method, ps.getCalcNodes()).scheduler == scheduler) {
				if (ps.containsForcedSpawn()) {
					if (state.joining_map.get(ps)) {
						state.joining_paths++;
					}

					blocking.add(ps);
					block_outcom.addAll(ps.getNullFreeOutSymbols());
				} else {
					nonblocking.add(ps);
					if (!state.kernel) {
						nonblock_outcom.addAll(ps.getNullFreeOutSymbols());
					}
				}
			}
		}

		boolean old_has_context = state.method_has_context;

		if (!blocking.isEmpty()) { //spawn blocking if any
			boolean need_context = false;
			for (iTask ps : blocking) {
				if (ps.getFirstCalcNode(state.method, ps.getCalcNodes()).scheduler == scheduler) {
					need_context = true;
					break;
				}
			}

			JCTree old_label_sched = state.loop_label_sched;
			boolean old_label_kernel = state.loop_label_kernel;
			boolean old_task_ret = state.task_return;
			state.task_return = state.kernel_task_return;
			if (need_context && state.spawns > 0 && !state.com.isEmpty() && !state.method_has_context) {
				genMethodTGHeader(state.method,scheduler, true, false);
			}

			state.path_outcom = block_outcom;
			genMethodTGBodyCore(scheduler, no_wait, tree, blocking, false);

			if (state.task_return & (!old_has_context || (state.kernel_task_return && !old_task_ret))) {
				nl();
				print(state.sequentialEmitter.context() + "->tasks.task_return->wait_for_all();");
				nl();
				print(state.sequentialEmitter.context() + "->tasks.task_return->parent()->decrement_ref_count();");
				nl();
				print(state.sequentialEmitter.context() + "->tasks.task_return->set_parent(NULL);");
				nl();
				print("tbb::task::destroy(*" + state.sequentialEmitter.context() + "->tasks.task_return);");
			}

			state.loop_label_sched = old_label_sched;
			state.loop_label_kernel = old_label_kernel;

			state.task_return = old_task_ret;

		}

		state.joining_paths = old_joining;
		if (!nonblocking.isEmpty()) { //do rest (if any)
			state.path_outcom = nonblock_outcom;
			genMethodTGBodyCore(scheduler, no_wait, tree, nonblocking, true);
		}

		state.method_has_context = old_has_context;

	}
    
    public void emitEntry(JCTree scheduler,Set<iTask> paths,boolean do_kernel) throws IOException
    {
        for (iTask p : paths) {
            //Set<JCTree> calc_nodes = p.getCalcNodes();

            if (p.containsNode(state.method))//ignore empty paths
            {
                Set<iTask> entry=new LinkedHashSet<iTask>();
                entry.add(p);
                state.path_outcom=p.getNullFreeOutSymbols();
                state.sequentialEmitter.printPathStats(entry, scheduler, do_kernel);
                state.DumpSpawn(scheduler, p, false, state.task_map);
            }
        }    
    }

	//alloc and spawn tasks (used by schedulers, like state.method, if/else branch, other control flow)
	public void genMethodTGBodyCore(JCTree scheduler, boolean no_wait, JCMethodDecl tree, Set<iTask> paths, boolean do_kernel) throws IOException {

		boolean header_written = false;

		int count_indep = 0;
		int count_evodep = 0;

		int old_joining_paths = state.joining_paths;

		if (scheduler != state.method) {
			state.joining_paths = 0;
		}

		boolean old_dump_kernel = state.dump_kernel;
		state.dump_kernel = do_kernel;
        
        int pathcount=0;
        for (iTask p : paths) {
            Set<JCTree> calc_nodes = p.getCalcNodes();

            if (calc_nodes.isEmpty()||p.containsNode(state.method))//ignore empty paths
            {
                continue;
            }
            
            pathcount++;
        }        

		if (!do_kernel&&pathcount>0) {
			ListBuffer<String> threadSpawn = new ListBuffer<String>();
			ListBuffer<String> deferSpawn = new ListBuffer<String>();
			//there is one task per iTask
			for (iTask p : paths) {
				Set<JCTree> calc_nodes = p.getCalcNodes();

				if (calc_nodes.isEmpty()||p.containsNode(state.method))//ignore empty paths
				{
					continue;
				}

				JCTree fcn = p.getFirstCalcNode(state.method, calc_nodes);
				if (fcn.scheduler != scheduler || fcn.getTag() == JCTree.CF) {
					continue;//are we the scheduler for this path (or is it control flow??)
				}

				state.DumpSchedule(scheduler, p, state.task_map, state.dump_kernel);

				if (!header_written)//we wait if there are any tasks to spawn before dumping the header
				{
					int joining_edges = state.joining_paths;
					if (joining_edges > 0 && !state.is_evo) {
						println();
						align();
						print("" + state.sequentialEmitter.self_task() + "set_ref_count(" + (joining_edges + 1) + ");"); //+1 for wait
					}
					if (state.task_return) {
						nl();
						print(state.sequentialEmitter.context() + "->tasks.task_return=new(tbb::task::allocate_root()) funky::Task<" + tree.getID() + "_Context>(NULL);");
						nl();
						print(state.sequentialEmitter.context() + "->tasks.task_return->set_ref_count(2);");
					}
					nl();
					print("tbb::task_list indep_tasks;");
					header_written = true;
				}

				int incom = state.in_map.get(p);
				String task_name = state.task_map.get(p);
				boolean is_joining = state.joining_map.get(p);

				if (is_joining) { //transitively joining (contributes to finally)
					println();
					align();
					//child for non evo because we might have to wait to do finally!
					state.joining_paths++;
					print("task_handle* task_instance_" + task_name + "=new(" + state.sequentialEmitter.self_task() + "allocate_child()) " + task_name + "(" + state.sequentialEmitter.context() + ");");
					count_evodep++;

					if (incom > 1) {
						println();
						align();
						print("task_instance_" + task_name + "->set_ref_count(" + incom + ");");
					}

					if (incom > 0) {
						println();
						align();
						print(state.sequentialEmitter.context() + "->tasks.task_instance_" + task_name + "=" + "task_instance_" + task_name + ";");
					}

					if (incom == 0) {
						println();
						align();
						state.DumpSpawn(scheduler, p, false, state.task_map);
						if (!state.is_evo) {
							if (p.getThreads().size() < 1) {
								deferSpawn.add("" + state.sequentialEmitter.self_task() + "spawn(*task_instance_" + task_name + ");");
							} else {
								String sspawn = "funky::Thread::SpawnTask<" + state.typeEmitter.getTypeNoVoid(state.method.type.getReturnType(), "unkown") + " >(task_instance_" + task_name
										+ ", TaskThreadInitializer_" + state.current_class.replace('.', '_') + "::getThread" + p.getThreads().iterator().next().toString() + "())";

								threadSpawn.add(sspawn + ";");
							}
						} else {
							count_indep++;
							if (p.getThreads().size() < 1) {
								print("indep_tasks.push_back(*task_instance_" + task_name + ");");	//can be run but we want to wait for it so delay until after set_RC
							} else {
								String sspawn = "funky::Thread::SpawnTask<" + state.typeEmitter.getTypeNoVoid(state.method.type.getReturnType(), "unkown") + " >(task_instance_" + task_name
										+ ", TaskThreadInitializer_" + state.current_class.replace('.', '_') + "::getThread" + p.getThreads().iterator().next().toString() + "())";

								threadSpawn.add(sspawn + ";");
							}
						}
					}

				} else { //dangling as it does not contribute to finally
					if (incom != 1 || state.is_evo || state.is_constructor) {
						//add dangling tasks to root so that they finish before we exit
						println();
						align();
						if (!(state.is_event || state.is_evo || state.is_constructor)) {
							print("task_handle* task_instance_" + task_name + "=new(tbb::task::allocate_additional_child_of(*funky::TaskRoot<>::GetRoot())) " + task_name + "(" + state.sequentialEmitter.context() + ");");
						} else {
							println();
							align();
							state.joining_paths++;
							print("task_handle* task_instance_" + task_name + "=new(" + state.sequentialEmitter.self_task() + "allocate_child()) " + task_name + "(" + state.sequentialEmitter.context() + ");");
							count_evodep++;
							if (incom == 1) {
								println();
								align();
								print(state.sequentialEmitter.context() + "->tasks.task_instance_" + task_name + "=" + "task_instance_" + task_name + ";");
							}
						}
					}

					if (incom > 1) {
						println();
						align();
						print("task_instance_" + task_name + "->set_ref_count(" + incom + ");");
						println();
						align();
						print(state.sequentialEmitter.context() + "->tasks.task_instance_" + task_name + "=" + "task_instance_" + task_name + ";");
					} else if (incom == 0) {
						count_indep++;
						println();
						align();
						state.DumpSpawn(scheduler, p, false, state.task_map);
						if (p.getThreads().size() < 1) {
							print("indep_tasks.push_back(*task_instance_" + task_name + ");");
						} else {
							String sspawn = "funky::Thread::SpawnTask<" + state.typeEmitter.getTypeNoVoid(state.method.type.getReturnType(), "unkown") + " >(task_instance_" + task_name
									+ ", TaskThreadInitializer_" + state.current_class.replace('.', '_') + "::getThread" + p.getThreads().iterator().next().toString() + "())";

							threadSpawn.add(sspawn + ";");
						}
					}
				}
			}

			if (count_evodep > 0 && (state.is_evo || state.is_constructor)) { //for evos we have to wait for all tasks
				nl();
				print("" + state.sequentialEmitter.self_task() + "set_ref_count(" + (count_evodep + 1) + ");");
			}
			if (scheduler != state.method) {
				int joining_edges = state.joining_paths;
				if (joining_edges > 0 && !state.is_evo) {
					println();
					align();
					print("" + state.sequentialEmitter.self_task() + "set_ref_count(" + (joining_edges + 1) + ");"); //+1 for wait
				}
			}

			for (String s : deferSpawn) {
				nl();
				print(s);
			}                                   

			if (count_indep > 0) { //do we need to wait?
				println();
				align();
				if (state.is_void && (state.method.return_flags & Flags.FINAL) == 0) {//if finally is not used and state.method returns void, no need to wait
					if (count_indep - threadSpawn.size() > 0) {
						print("" + state.sequentialEmitter.self_task() + "spawn(indep_tasks);");
					}
					for (String s : threadSpawn) {
						nl();
						print(s);
					}
                    emitEntry(scheduler,paths,do_kernel);

				} else {
					if (no_wait && !state.kernel)//flag for branches which emit their own special wait code
					{
						if (count_indep - threadSpawn.size() > 0) {
							print("" + state.sequentialEmitter.self_task() + "spawn(indep_tasks);");
						}
						for (String s : threadSpawn) {
							nl();
							print(s);
						}
						if(state.joining_paths>0)
							branch_wait = true;
                        emitEntry(scheduler,paths,do_kernel);
                        
					} else //we need to wait for the result
					{
						if (!(state.returns_inside_task||state.is_event || state.is_evo || state.is_constructor)) {
							if (count_indep - threadSpawn.size() > 0) {
								print("" + state.sequentialEmitter.self_task() + "spawn(indep_tasks);");
							}
							for (String s : threadSpawn) {
								nl();
								print(s);
							}
                            emitEntry(scheduler,paths,do_kernel);
                            
						} else {
							for (String s : threadSpawn) {
								nl();
								print(s);
							}
                            state.waiting=true;
                            emitEntry(scheduler,paths,do_kernel);
                            
							if (count_indep - threadSpawn.size() > 0) {
								print("" + state.sequentialEmitter.self_task() + "spawn_and_wait_for_all(indep_tasks);"); //spawn indep tasks and wait for joining tasks to dec our refc
							} else {
								print("" + state.sequentialEmitter.self_task() + "wait_for_all();");
							}
                           
						}
					}
				}
			} else {
                emitEntry(scheduler,paths,do_kernel);

				if (state.is_evo || (scheduler != state.method && state.joining_paths > 0)) {
					nl();
					if (state.inside_task == null) {
						print(state.sequentialEmitter.self_task());
					}
                    state.waiting=true;
					print("wait_for_all();");
				}
            
			}
		} else {
			state.sequentialEmitter.printPathStats(paths, scheduler, do_kernel);
		}

		state.dump_kernel = old_dump_kernel;
		state.joining_paths = old_joining_paths;
	}

	//alloc and spawn tasks (use dby schedulers, like state.method, if/else branch, other control flow)
	public void genMethodTGBody(JCTree scheduler, boolean no_wait, JCMethodDecl tree, Set<iTask> paths) throws IOException {

		boolean old_has_context = state.method_has_context;
		int joining = state.joining_paths;
		boolean taskret = state.task_return;
		JCTree old_label_sched = state.loop_label_sched;
		boolean old_label_kernel = state.loop_label_kernel;

		if (state.kernel&&state.target.coreCount>1) {
			nl();
			int max_tasks=1024;
			int max_spwans=tree.maxRecSpawn;
			int cores=(int) Math.min(10, state.target.coreCount * 2.5);
			print("if(__KERNEL_LEVEL<" + Math.min(max_tasks,(int)(Math.log(max_tasks)/Math.log(Math.min(max_spwans,cores)))) + ") {");
			indent();
			nl();

			if (!state.method_has_context) {
				state.kernel = false;
				state.inside_kernel = true;

				genMethodTGHeader(state.method,scheduler, true, true);

				state.inside_kernel = false;
				state.kernel = true;
			}
		}

		genMethodTGBodyCore(scheduler, no_wait && !state.kernel, tree, paths, false);

		if (state.kernel) {
			state.kernel = false;
			state.inside_kernel = true;
			if(state.target.coreCount>1)
				genMethodTGFooter(state.method);
			state.inside_kernel = false;
			state.kernel = true;

			state.task_return = false;

			undent();
			nl();
			if(state.target.coreCount>1)
				print("} else {");
			indent();
			println();

			state.method_has_context = old_has_context;

			MethodSymbol old_redirect = state.redirect_recursion;
			if (state.method.restricted_impls != null && state.method.restricted_impls.get(1) != null) {
				state.redirect_recursion = state.method.restricted_impls.get(1).sym;
			}

			genMethodBodyCore(scheduler, no_wait, tree, paths);

			state.redirect_recursion = old_redirect;

			undent();
			nl();
			if(state.target.coreCount>1)
				print("}");
		}

		state.joining_paths = joining;
		state.task_return = taskret;

		state.method_has_context = old_has_context;
		state.loop_label_sched = old_label_sched;
		state.loop_label_kernel = old_label_kernel;
	}

	public void genMethodTGFooter(JCMethodDecl tree) throws IOException {
		//clean up return task
		if (state.task_return) {
			nl();
			print(state.sequentialEmitter.context() + "->tasks.task_return->wait_for_all();");
			nl();
			print(state.sequentialEmitter.context() + "->tasks.task_return->parent()->decrement_ref_count();");
			nl();
			print(state.sequentialEmitter.context() + "->tasks.task_return->set_parent(NULL);");
			print("tbb::task::destroy(*" + state.sequentialEmitter.context() + "->tasks.task_return);");
		}
        else if(state.returns_inside_task&&state.joining_paths>0&&!state.waiting)
        {
            state.waiting=true;
            nl();
            print("" + state.sequentialEmitter.self_task() + "wait_for_all();");
        }        
        
        if(state.inside_task==null)
        {
            nl();
            print("tbb::task::destroy(" + state.sequentialEmitter.self_task_name() + ");"); 
        }
//handle finally:
		if (state.method.final_value != null) {
			println();
			align();

			state.allow_final = true;
			printExpr(state.method.final_value);
			state.DumpFinal(state.method.final_value, state.kernel);
			state.allow_final = false;
		}

		if (!state.is_event && !state.is_void && tree.getReturnType() != null && state.method_has_context && state.method.final_value == null) {
			println();
			align();
			print("return context->GetReturn();");

		}
		//state.loop_label_sched = null; //may be called before codegen of this method is finished
		state.loop_label_kernel = false;

	}

	//!! process output of PathGen, sets up all kinds of maps so we can find out which tasks must start which other tasks under which conditions
	//also prints the task
	public Set<iTask> preparePaths(JCMethodDecl tree) throws IOException {

		//get set of pathsets we might have to schedule (from TaskGen.java)
		Set<iTask> paths = new LinkedHashSet<iTask>(tree.getAllSchedules());


        state.task_return=false;
		//setup lookup tables

		state.joining_paths = 0;

		//translate pathset to task name
		state.task_map = new LinkedHashMap<iTask, String>();
		//incoming deps
		state.in_map = new LinkedHashMap<iTask, Integer>();
		//does path contribute to state.method output?
		state.joining_map = new LinkedHashMap<iTask, Boolean>();
		//lookup pathsets that wait for signal
		signal_map = new LinkedHashMap<VarSymbol, Set<iTask>>();

		Map<iTask, Set<VarSymbol>> signalFrom = new LinkedHashMap<iTask, Set<VarSymbol>>();

		//Map<iTask, Set<VarSymbol>> signalFrom = new LinkedHashMap<iTask, Set<VarSymbol>>();

		//tasks with input deps
		Set<iTask> waiting_tasks = new LinkedHashSet<iTask>();

		//state.names: avoid duplicate tasks (non ptr ident pathsets can gen same context/wait name)
		Set<String> waiting_names = new LinkedHashSet<String>();
		state.context_names = new LinkedHashSet<String>();

		//remove CF paths (CF handled by visitIf/...)
		for (Iterator<iTask> i = paths.iterator(); i.hasNext();) {
			iTask p = i.next();
			Set<JCTree> calcNodes = p.getCalcNodes(); //FIXME: cache these?
			JCTree fcn = p.getFirstCalcNode(state.method, calcNodes);
			if (fcn.getTag() == JCTree.CF || p.getPathBlock().getStatements().isEmpty() || (fcn.getTag() == JCTree.RETURN && fcn.nop)) {
				i.remove();
			}
		}

		//all local vars are task local
		if (tree.local_vars != null) {
			for (VarSymbol vs : tree.local_vars) {
				if ((vs.flags() & Flags.PARAMETER) == 0 && vs.isLocal()) {
					vs.flags_field |= Flags.TASKLOCAL; //non-shared local var, declare in task
				}
			}
		}

		state.com.clear();
		state.blocking_com.clear();

		int direct_joining = 0; //need directly joining paths to find out whether we need the context

		//we collect all vars that are shared between different tasks here, these vars must be part of the context rather than task local
		for (iTask p : paths) {
			Set<JCTree> calcNodes = p.getCalcNodes();

			//calc input deps for path:
			Set<JCTree> inc = p.getInComImplicit();

			inc.remove(tree); //state.method entry is fake dependency

			//remove CF and var decl in deps
			int num_cf = 0;
			for (JCTree t : inc) {
				if (t.getTag() == JCTree.CF) {
					num_cf++;
				}
				if (t.getTag() == JCTree.VARDEF && ((JCVariableDecl) t).init == null) {
					num_cf++;
				}
			}

			int incom = inc.size() - num_cf;

			state.in_map.put(p, incom);
			state.joining_map.put(p, p.isJoining());

            JCTree scheduler=p.getFirstCalcNode(state.method, calcNodes).scheduler;
			int loc_spawns = getSchedulerPaths(scheduler, paths, true);
            boolean loc_blocking_only;
            if(scheduler.getTag()!=JCTree.CF)
                loc_blocking_only = !(loc_spawns>0);
            else
                loc_blocking_only = !(loc_spawns>1);

			if (loc_blocking_only)//no need to do around with spawning on the right thread if there is only one task and we're on the right thread already
			{
				p.removeThread(state.method.sym.threads);
			}

			if (!p.getPathBlock().stats.isEmpty() && (!loc_blocking_only || p.containsForcedSpawn())) {

				boolean join = state.joining_map.get(p);
				//does path join
				if (join && !p.isCFDEPTo(tree)) {
					if (state.method.dangling_paths.contains(p)) {
						direct_joining++;
					}
					state.joining_paths++;//gives transitive join
				}

				//store path name
				JCTree fcn = p.getFirstCalcNode(state.method, calcNodes);

				JCBlock b = p.getPathBlock();//creates a block from the paths by generating and traversing a dag
				String name = tree.getID() + "_Task" + b.getStatements().head.getTaskID();
				state.task_map.put(p, name);


				state.com.addAll(p.getInSymbols());
				state.com.addAll(p.getOutSymbols());

				signalFrom.put(p, p.getOutSymbols());

				if (p.containsForcedSpawn()) {
					state.blocking_com.addAll(p.getInSymbols());
					state.blocking_com.addAll(p.getOutSymbols());
				}

				//store var deps
				//Set<JCTree> calcnodes = calcNodes;
				Set<VarSymbol> in = p.getInSymbolsImplicit();

				for (VarSymbol v : in) {
					if ((v.flags() & Flags.PARAMETER) == 0 && v.isLocal()) {//parameters are fake deps
						Set<iTask> set;
						if (signal_map.containsKey(v)) {
							set = signal_map.get(v);
						} else {
							set = new LinkedHashSet<iTask>();
						}

						set.add(p);

						signal_map.put(v, set); //paths in set must wait on variable v to be calculated before they can execute
					}
				}

				//if task has non-fake in deps or is not scheduled by the state.method, then it must wait (cannot be spawned in meth entry)
				if ((!in.isEmpty() || fcn.scheduler != state.method) && !waiting_names.contains(state.task_map.get(p))) {
					waiting_tasks.add(p);
					waiting_names.add(state.task_map.get(p));
				}

			}
			state.DumpPath(p, state.task_map);
		}


		//build date structures to figure out if we have multiple notifications from p1 to p2
		Map<iTask, Set<VarSymbol>> pathToInput = new LinkedHashMap<iTask, Set<VarSymbol>>();
		for (VarSymbol vs : signal_map.keySet()) {
			Set<iTask> set = signal_map.get(vs);

			for (iTask it : set) {
				Set<VarSymbol> input = pathToInput.get(it);

				if (input == null) {
					input = new LinkedHashSet<VarSymbol>();
				}

				input.add(vs);

				pathToInput.put(it, input);
			}
		}

		//remove unnecessary notifications
		for (iTask it : pathToInput.keySet()) {
            Set<JCTree> removedSrc = new LinkedHashSet<JCTree>();

			Set<VarSymbol> input = pathToInput.get(it);
			for (iTask signaler : signalFrom.keySet()) {
				if (signaler != it) {
					Set<VarSymbol> signaled = new LinkedHashSet<VarSymbol>(signalFrom.get(signaler));
					signaled.retainAll(input);
					if (signaled.size() > 1) //same paths signaled by more than one input
					{
						Map<VarSymbol, JCTree> source = signaler.getOutComMap();

						VarSymbol topolLargest = null;
						int topolId = 0;

						for (VarSymbol vs : signaled) {
							int nid = state.method.topolNodes.get(source.get(vs));
							if (nid > topolId) {
								topolId = nid;
								topolLargest = vs;
							}
						}

						for (VarSymbol vs : signaled) {
							if (vs != topolLargest) {
								signal_map.get(vs).remove(it);
                                if(!removedSrc.contains(source.get(vs)))
                                {
                                    removedSrc.add(source.get(vs));
                                    state.in_map.put(it, state.in_map.get(it) - 1);
                                }
								if (signal_map.get(vs).isEmpty()) {
									signal_map.remove(vs);
								}
							}
						}
					}
				}
			}
		}

		state.blocking_spawns = 0;
		state.spawns = hasSchedulerPaths(tree.getAllSchedules());

		if (state.spawns > 0) {

			if (state.header) {//emit task context
				if (!state.context_names.contains(tree.getID())) {
					state.context_names.add(tree.getID());
					printContext(tree, state.com, waiting_tasks);
				}
			}

			for (VarSymbol vs : tree.local_vars) {
				if ((vs.flags() & Flags.PARAMETER) == 0 && vs.isLocal()) {
						if (state.com.contains(vs)) { //is it shared??
							vs.flags_field &= ~Flags.TASKLOCAL; //non-shared local var, declare in task
						} else {
					}
				}
			}

			state.methodpaths = paths;

			//FIXME: can we be more progressive here??
			state.is_context_refcount = !(state.is_event || state.is_sample) && (state.method.dangling_paths.size() - direct_joining > 0);
			tree.context_ref_count = state.is_context_refcount;

            boolean task_return=false;
			//print actual tasks:
			for (iTask p : paths) {
                    Set<JCTree> calcNodes = p.getCalcNodesTransitive();

                    JCTree fcn=p.getFirstCalcNode(state.method, calcNodes);
                    JCTree scheduler = fcn.scheduler;
                    int scheduler_spawns = getSchedulerPaths(scheduler, paths, true);
                    boolean has_spwans;
                    if(scheduler.getTag()!=JCTree.CF)
                        has_spwans = scheduler_spawns>0;
                    else
                        has_spwans = scheduler_spawns>1;
                                                
                    blocking_only = getBlockingSchedulerPaths(scheduler, paths, true)==scheduler_spawns;
                    
                    if (!p.containsNode(state.method)&&!p.getPathBlock().stats.isEmpty() && (has_spwans || p.containsForcedSpawn()))//DO NOT USE CALC NODES: THEY CONTAIN JCTree.CF!
                    {
                        state.path_outcom = p.getNullFreeOutSymbols(); //used int printStat to generate code to spwan dependent tasks

                        //FIXME: set this to false if there are no inependent tasks??:
                        state.task_return = p.hasTransitiveReturns()&&scheduler.transitive_returns>1; //if this scheduler or a scheduler spawned from this scheduler has more than one return path then we must count
                        //state.task_return = scheduler.transitive_returns > 0;
                        task_return|=state.task_return;

                        printTask(tree, p, p.getPathBlock(), state.task_map.get(p), p.getInnerSymbols(calcNodes, state.method), state.is_context_refcount && state.method.dangling_paths.contains(p));

                        state.task_return = false;

                        state.path_outcom = null;
                    }
                    blocking_only = false;
			}
            
            state.task_return=task_return; //(any actual task that returns something??)

		}

		return paths;
	}

	public void fixRefsInBranches(JCCF tree) throws IOException
	{
		if(state.inside_task!=null)
		{
			//volatile vars may introduce asymetric refs in branches, which we fix here
			//additionalRefs is constructed in TaskSet.getInComImplicit
			int fixCount=0;
			for(TaskSet ts:tree.additionalRefs.keySet())
			{
				if(state.in_map.get(ts)!=1)
				{
					fixCount++;
					nl();
					print(state.sequentialEmitter.context() + "->tasks.task_instance_" + state.task_map.get(ts) + "->decrement_ref_count();");
				}
			}
			if(fixCount>0)
				nl();
		}
	}

	//how many tasks are spawned by scheduler?
	public int getSchedulerPaths(JCTree scheduler, Set<iTask> paths, boolean uncoditional) {
		int count = 0;

		for (Iterator<iTask> i = paths.iterator(); i.hasNext();) {
			iTask p = i.next();
			Set<JCTree> calcNodes = p.getCalcNodes(); //FIXME: cache these?
			JCTree fcn = p.getFirstCalcNode(state.method, calcNodes);
			Integer in = state.in_map.get(p);
			//tasks that depend only on CF
			if (!p.containsNode(state.method)&&fcn.scheduler == scheduler && fcn.getTag() != JCTree.CF && !p.isCFDEPTo(scheduler) && (uncoditional || in == null || in != 1)) {
				count++;
			}
		}

		return count;

	}
    
	//how many tasks are spawned by scheduler?
	public int getBlockingSchedulerPaths(JCTree scheduler, Set<iTask> paths, boolean uncoditional) {
		int count = 0;

		for (Iterator<iTask> i = paths.iterator(); i.hasNext();) {
			iTask p = i.next();
			Set<JCTree> calcNodes = p.getCalcNodes(); //FIXME: cache these?
			JCTree fcn = p.getFirstCalcNode(state.method, calcNodes);
			Integer in = state.in_map.get(p);
			//tasks that depend only on CF
			if (p.containsForcedSpawn()&&!p.containsNode(state.method)&&fcn.scheduler == scheduler && fcn.getTag() != JCTree.CF && !p.isCFDEPTo(scheduler) && (uncoditional || in == null || in != 1)) {
				count++;
			}
		}

		return count;

	}    

	public int hasSchedulerPaths(Set<iTask> paths) {
		int count = 0;
		Set<JCTree> set = new LinkedHashSet<JCTree>();
		for (Iterator<iTask> i = paths.iterator(); i.hasNext();) {
			iTask p = i.next();
			Set<JCTree> calcNodes = p.getCalcNodes(); //FIXME: cache these?
			JCTree fcn = p.getFirstCalcNode(state.method, calcNodes);
			//Integer in=state.in_map.get(p);

			if (!p.isCFDEPTo(state.method) && fcn.getTag() != JCTree.CF) {
				if (set.contains(fcn.scheduler)) {
					count++;
				} else {
					set.add(fcn.scheduler);
					/*
					if(!p.containsNode(fcn.scheduler))
						count++;
					*/
				}

			}
			if (p.containsForcedSpawn()) {
				count++;
				state.blocking_spawns++;
			}
		}

		if(!state.is_evo&&!state.method.spawned_dangling_paths.isEmpty())
			for(iTask it:state.method.spawned_dangling_paths)
				if(!it.containsNode(state.method))
					count++;

		return count;

	}
}
