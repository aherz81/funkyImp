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
import com.sun.tools.javac.tree.cl.util.PrettyStringBuilder;


/**
 * Prints out a tree as an indented Java source program.
 *
 * <p><b>This is NOT part of any API supported by Sun Microsystems. If you write
 * code that depends on this, you do so at your own risk. This code and its
 * internal interfaces are subject to change or deletion without notice.</b>
 */
//emitter for MPI
/**
 * Highly experimental initially copied from LowerTasksTBB
 *
 * @author Andreas Wagner
 */
public class LowerTasksMPI extends Emitter implements ILowerTasks {

    Map<VarSymbol, Set<iTask>> signal_map; //which paths depend on var symbol
    Map<iTask, Set<VarSymbol>> signalFrom; //which tasks signal which var symbol to which other task
    Map<iTask, String> notify_IDs;
    Map<iTask, String> mpi_IDs;
    Map<iTask, Set<VarSymbol>> arrays_to_notify;
    Map<iTask, Set<VarSymbol>> vars_to_notify;
    Map<iTask, Set<iTask>> tasks_to_notify;
    Map<iTask, Set<VarSymbol>> task_parameters;
    Map<iTask, String> workerNames;
    Map<iTask, Boolean> uses_global_iterate;
    //ArrayList<iTask> dangling_paths = null; //was used fro dbg output
    boolean blocking_only = false; //are all spawns blocking?
    boolean branch_wait = false; //branches must do their own waiting
    PrettyStringBuilder psb; //for debug purposes

    //modify print-method and nl-method to work with PrettyStringBuilder
    @Override
    public void nl() throws IOException {
        super.nl();
        psb.appendNl();
    }

    @Override
    public void print(Object name) throws IOException {
        super.print(name);
        psb.append(name);
    }

// ------------------- actual code emitter ---------------
    public LowerTasksMPI(LowerTreeImpl state) {
        super(state);
        psb = new PrettyStringBuilder(state);
        notify_IDs = new LinkedHashMap<iTask, String>();
        arrays_to_notify = new LinkedHashMap<iTask, Set<VarSymbol>>();
        vars_to_notify = new LinkedHashMap<iTask, Set<VarSymbol>>();
        tasks_to_notify = new LinkedHashMap<iTask, Set<iTask>>();
        mpi_IDs = new LinkedHashMap<iTask, String>();
        task_parameters = new LinkedHashMap<iTask, Set<VarSymbol>>();
        uses_global_iterate = new LinkedHashMap<iTask, Boolean>();
        
    }

    //spawn tasks that depend on result of tree
    @Override
    public void SpawnDependentTasks(JCTree tree) throws IOException {
        //spawn dependen tasks:
        if (state.methodpaths != null && state.method.depGraph.vertexSet().contains(tree)) {
            Set<Arc> out = state.method.depGraph.outgoingEdgesOf(tree); //get outgoing edges of graph from current node

            Set<VarSymbol> done = new LinkedHashSet<VarSymbol>();
            Set<iTask> to_notify = new LinkedHashSet<iTask>();

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
                                    to_notify.add(p);
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
                if (state.kernel && !blocking_only) { //emit two cases, one for state.kernel one std
                    nl();
                    print("if(__SPAWNING_LEVEL < 5){");
                    indent();
                    nl();

                    if (!state.method_has_context) {
                        state.kernel = false;
                        state.inside_kernel = true;
                        int joining = state.joining_paths;
                        boolean taskret = state.task_return;
                        state.joining_paths = state.kernel_joining_paths;
                        state.task_return = state.kernel_task_return;

                        genMethodTGHeader(state.method, tree.scheduler, false, true);
                        state.joining_paths = joining;
                        state.task_return = taskret;
                        state.inside_kernel = false;
                        state.kernel = true;
                    }

                    nl();
                    print("self_task=&SELF();");
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
                
                    //notify other tasks if neccessary
                    if(to_notify.size() > 0){
                        nl();
                        boolean must_memcpy = false;
                        for(iTask task : to_notify){
                            if(vars_to_notify.get(task) != null && vars_to_notify.get(task).size() > 0){
                                must_memcpy = true;
                                break;
                            }
                        }
                        if(must_memcpy){
                            print("//notify");
                            nl();
                            print("memcpy(notify_buffer, &context, sizeof(context));");
                            nl();
                        }
                        for(iTask task : to_notify){
                            if(vars_to_notify.get(task) != null && vars_to_notify.get(task).size() > 0){
                                //only one notification for whole context
                                print("MPI::COMM_WORLD.Send(notify_buffer, 1024, MPI::BYTE, ");
                                print(notify_IDs.get(task));
                                print(", ");
                                print(state.inside_task.toUpperCase());
                                print(");");
                                nl();
                            }
                        }
                        for(iTask task : to_notify){
                            if(arrays_to_notify.get(task) != null && arrays_to_notify.get(task).size() > 0){
                                Set<VarSymbol> arrays = arrays_to_notify.get(task);
                                for(VarSymbol vs : arrays){
                                    print("MPI::COMM_WORLD.Send(");
                                    state.sequentialEmitter.visitVarSymbol(vs, true);
                                    print("->toNative(), ");
                                    state.sequentialEmitter.visitVarSymbol(vs, true);
                                    print("->GetCount() * sizeof(");
                                    state.sequentialEmitter.visitVarSymbol(vs, true);
                                    print("->get(0))");
                                    print(", MPI::BYTE, ");
                                    print(notify_IDs.get(task));
                                    print(", ");
                                    print(state.inside_task.toUpperCase());
                                    print(");");
                                    nl();
                                }
                            }
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
                //print("" + state.sequentialEmitter.self_task() + "wait_for_all();"); //spawn indep tasks and wait for joining tasks to dec our refc
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
                    //print("" + state.sequentialEmitter.self_task() + "wait_for_all();"); //spawn indep tasks and wait for joining tasks to dec our refc
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
                print("class " + name);
                nl();
                print("{");
                indent();
                nl();
                print("private:");
                nl();
                nl();
                print("public:");
                indent();
                nl();
                print(name + "();");
                nl();
                print("~" + name + "();");
                nl();
                print("void execute(char* buffer, int source);");
                undent();
                undent();
                nl();
                print("};");
                nl();
                nl();

                state.jc.taskNames.add(name);
                String namespaceAndName = tree.sym.enclClass().name + "::" + name;
                state.jc.taskNamesWithNamespace.add(namespaceAndName);
                state.jc.tasksToTasknameNamespaces.put(namespaceAndName, name);

                return b.getStatements().last();
            }

            state.inside_task = name;

            //MPI-Task 
            //FIXME: improve and find good templates
            //Constructor
            print(tree.sym.enclClass().name + "::" + name + "::" + name + "(){}");
            nl();
            nl();
            //Destructor
            print(tree.sym.enclClass().name + "::" + name + "::~" + name + "(){}");
            nl();
            nl();
            //Begin of execute()-Method
            print("void " + tree.sym.enclClass().name + "::" + name + "::" + "execute(char* buffer, int source)");
            nl();
            print("{");
            indent();
            nl();

            state.inside_method = true;

            //FIXME: modify profiling
            if (state.profile > 0) {
                float task_work = state.work.getWork(p, state.method);
                float task_cor = state.work.getCorrection(p, state.method);
                String task_desc = "";
                for (JCStatement s : b.getStatements()) {
                    task_desc += s.getTaskID() + ",";
                }
                nl();
                print("static funky::ProfileEntry* state.profile_ENTRY=funky::Profile::Register(\"" + name + "\",\"" + tree.sym.owner.name + "\",\"" + task_desc + "\"," + task_work + "," + task_cor + ");");
                nl();
                //state.profile_ENTRY->
                print("tbb::tick_count START_TASK=tbb::tick_count::now();");
            }

            state.current_group = "";

            //FIXME: modify debugging
            if (state.debug_print_task) {
                nl();
                print("funky::Debug::StartTask(" + b.getStatements().head.getTaskID() + ");");
                print("printf(\"Task(%s):0x%016p on Thread(0x%016p)\\n\",\"" + name + "\",this,funky::Thread::getCurrentThread());");
            }
            
            //calculate, which variables are notifications and which are initially needed
            Set<VarSymbol> all_incoming = p.getInSymbols();
            Set<VarSymbol> parameters = new LinkedHashSet<VarSymbol>();
            Map<iTask, Set<VarSymbol>> incoming_signal_tasks = new LinkedHashMap<iTask, Set<VarSymbol>>();
            Map<iTask, Set<VarSymbol>> incoming_signal_arrays_tasks = new LinkedHashMap<iTask, Set<VarSymbol>>();
            Set<VarSymbol> incoming_signal_vars = new LinkedHashSet<VarSymbol>();
            Set<VarSymbol> incoming_signal_arrays = new LinkedHashSet<VarSymbol>();
            
            //Get variables which generate notification for this task
            for(VarSymbol incoming : all_incoming){
                Set<iTask> tasks_notified_by_var = signal_map.get(incoming);
                if(tasks_notified_by_var != null){
                    for(iTask task : tasks_notified_by_var){
                        if(task == p){
                            if(incoming.type.getArrayType().tag == TypeTags.ARRAY){
                                incoming_signal_arrays.add(incoming);
                            }
                            else{
                                incoming_signal_vars.add(incoming);
                            }
                        }
                    }
                }
            }
            
            //calculate which tasks notify this task
            for(iTask task : signalFrom.keySet()){
                Set<VarSymbol> notified = signalFrom.get(task);
                for(VarSymbol vs : incoming_signal_arrays){
                    if(notified.contains(vs)){
                        if(incoming_signal_arrays_tasks.get(task) == null){
                            incoming_signal_arrays_tasks.put(task, new LinkedHashSet<VarSymbol>());
                        }
                        incoming_signal_arrays_tasks.get(task).add(vs);
                    }
                }
            }
            
            for(iTask task : signalFrom.keySet()){
                Set<VarSymbol> notified = signalFrom.get(task);
                for(VarSymbol vs : incoming_signal_vars){
                    if(notified.contains(vs)){
                        if(incoming_signal_tasks.get(task) == null){
                            incoming_signal_tasks.put(task, new LinkedHashSet<VarSymbol>());
                        }
                        incoming_signal_tasks.get(task).add(vs);
                    }
                }
            }
            
            //calculate initial vars from signaled vars
            for(VarSymbol incoming : all_incoming){
                if(!incoming_signal_vars.contains(incoming) && !incoming_signal_arrays.contains(incoming)){
                    parameters.add(incoming);
                }
            }
            task_parameters.put(p, parameters);
            
            //calculate which tasks notifiy this task
            Set<iTask> tasks_signaling_me = new LinkedHashSet<iTask>();
            Set<JCTree> incoming_stmts = p.getInCom();
            
            for(iTask task : signalFrom.keySet()){
                if(task != p){
                    for(JCTree t : incoming_stmts){
                        if(!(t instanceof JCMethodDecl)  && task.containsNode(t)){
                            tasks_signaling_me.add(task);
                        }
                    }
                } 
            }
            
            //declaring notification IDs
            if(tasks_signaling_me.size() > 0){
                print("//declare notification-IDs");
                nl();
                for(iTask task : tasks_signaling_me){
                    print("int " + state.task_map.get(task).toUpperCase() + "_MPI_ID;");
                    nl();
                }
            }
            
            print("//receiving mandatory data");
            nl();
            print(tree.getID() + "_Context context;");
            nl();
            print("memcpy(&context, buffer, sizeof(context));");
            nl();
            print("MPI::Status state;");
            nl();

            
            //check if arrays follow
            boolean need_array_calc = false;
            if(parameters.size() > 0){
                for(VarSymbol vs : parameters){
                    if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                        need_array_calc = true;
                        break;
                    }
                }
                if(need_array_calc){
                    print("int __array_size;");
                    nl();
                }
            }
            for(VarSymbol vs : parameters){
                if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                    print("//receive heap allocated object (array)");
                    nl();
                    //probe to get amount of elements
                    print("MPI::COMM_WORLD.Probe(source, DATA_TAG, state);");
                    nl();
                    //get number of elements
                    print("__array_size = state.Get_count(MPI::BYTE);");
                    nl();
                    //allocate space for array
                    state.sequentialEmitter.visitVarSymbol(vs, false);
                    //FIXME: this is probably just bullshit...
                    print(" = new funky::LinearArray< ");
                    state.typeEmitter.printType(((Type.ArrayType)vs.type.getArrayType()).elemtype);
                    print(" > ::Version(new funky::LinearArray< ");
                    state.typeEmitter.printType(((Type.ArrayType)vs.type.getArrayType()).elemtype);
                    print(" >(");
                    state.typeEmitter.printExprs(((Type.ArrayType)vs.type.getArrayType()).dom.appliedParams);
                    print(",true));");
                    nl();
                    print("MPI::COMM_WORLD.Recv(");
                    state.sequentialEmitter.visitVarSymbol(vs, false);
                    //copy array into reserved space
                    print("->toNative(), __array_size, MPI::BYTE, state.Get_source(), state.Get_tag());");
                    nl();
                }
            }
            
            //create receive-buffer if there are tasks which notify us
            boolean need_notification_buffer = false;
            if(tasks_signaling_me.size() > 0){
                int i = 0;
                
                for (iTask task : tasks_signaling_me) {
                    if(state.method.sym.mayBeRecursive && state.jc.optimizeRecursion && state.method.reuseableVars.get(task) != null){
                        //we do nothing if incoming task can be reused
                    }
                    else{
                        need_notification_buffer = true;
                        break;
                    }
                }
                if(need_notification_buffer){
                    print("char* receive_buffer = new char[1024];");
                    nl();
                    print(tree.getID() + "_Context receive_context;");
                }
                nl();
            }
            
            if(p.containsReturn() || p.isFinal()){
                //prepare send buffer
                print("char* send_buffer = new char[1024];");
                nl();
            }
            
            //prepare for notification: this task -> other task
            arrays_to_notify = new LinkedHashMap<iTask, Set<VarSymbol>>();
            vars_to_notify = new LinkedHashMap<iTask, Set<VarSymbol>>();
            tasks_to_notify = new LinkedHashMap<iTask, Set<iTask>>();
            //which tasks do we need to notify?
            Set<VarSymbol> outSymbols = p.getOutSymbols();
            if(outSymbols.size() > 0){
                //declare a general notify buffer for context
                print("char* notify_buffer = new char[1024];");
                nl();
                for(VarSymbol vs : outSymbols){
                    Set<iTask> tasks = signal_map.get(vs);
                    if(tasks != null){
                        for(iTask task : tasks){
                            //if this task reuses his calculations, don't notify other tasks
                            Set<VarSymbol> reusedVars = state.method.reuseableVars.get(p);

                            if(state.method.sym.mayBeRecursive && state.jc.optimizeRecursion && reusedVars != null && reusedVars.contains(vs)){
                                //don't notify other task
                            }
                            else{
                                //notify other task
                                //check if variable is an array -> reserve buffer space later
                                if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                                    if(arrays_to_notify.get(task) == null){
                                        arrays_to_notify.put(task, new LinkedHashSet<VarSymbol>());
                                    }
                                    arrays_to_notify.get(task).add(vs);
                                }
                                else{
                                    if(vars_to_notify.get(task) == null){
                                        vars_to_notify.put(task, new LinkedHashSet<VarSymbol>());
                                    }
                                    vars_to_notify.get(task).add(vs);
                                }
                                if(tasks_to_notify.get(p) == null){
                                    tasks_to_notify.put(p, new LinkedHashSet<iTask>());
                                }
                                tasks_to_notify.get(p).add(task);
                            }
                        }
                    }
                }
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
            //spawning_level is mandatory and can be obtained from context.kernel_level
            print("int spawning_level = context.kernel_level;");
            nl();
            
            //if we optimize recursion, we need to know the MPI-IDs of tasks to notify about a new round
            //if the task which triggers the recursion is reused itself, handle it different
           
            
            if(state.method.sym.mayBeRecursive && state.jc.optimizeRecursion){
                //if(!state.method.reuseableVars.containsKey(p)){
                    nl();
                    print("//get MPI-IDs for reusable tasks");
                    nl();
                    for(iTask task : state.method.reuseableVars.keySet()){
                        if(state.task_map.get(task) != null && task != p){
                            print("int ");
                            String mpi_id = state.task_map.get(task) + "_MPI_ID";
                            mpi_IDs.put(task, mpi_id);
                            print(mpi_id);
                            print(";");
                            nl();
                            print("MPI::COMM_WORLD.Recv(&" + mpi_id + ", 1, MPI::INT, source, NOTIFY_ID, state);");
                            nl();
                        }
                    }
                //}
            }
            
            //loop label for reuse
            if(state.method.sym.mayBeRecursive && state.jc.optimizeRecursion){
                //check if this task is reused and is the task where recursion occurs
                nl();
                print("REUSE_LOOP_LABEL:");
                nl();
            }
            
            //special handling for tasks which invoke recursion or have no outcom
            if(state.method.sym.mayBeRecursive && state.jc.optimizeRecursion && p.getOutSymbols().size() == 0){
                print("global_iterate = false;");
                nl();
            }
            
            //get to know who to notify
            if(tasks_to_notify.size() > 0){
                print("//receive MPI_IDs of tasks to notify");
                notify_IDs = new LinkedHashMap<iTask, String>();
                nl();
                int i = 0;
                for(iTask task : tasks_to_notify.get(p)){
                    notify_IDs.put(task, "notify_id_"+i++);
                    print("int " + notify_IDs.get(task) + ";");
                    nl();
                    print("MPI::COMM_WORLD.Recv(&" + notify_IDs.get(task) + ", 1, MPI::INT, source, NOTIFY_ID_TAG);");
                    nl();
                }
            }
            
            //wait for incoming dependencies
            if (tasks_signaling_me.size() > 0) {
                nl();
                print("//receive notifications");
                
                nl();
                for (iTask task : tasks_signaling_me) {
                    Set<VarSymbol> notify_from_task = signalFrom.get(task);
                    Set<VarSymbol> arrays = new LinkedHashSet<VarSymbol>();
                    int var_count = 0;
                    for(VarSymbol vs : notify_from_task){
                        if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                            arrays.add(vs);
                        }
                        var_count++;
                    }
                    if(state.method.sym.mayBeRecursive && state.jc.optimizeRecursion && state.method.reuseableVars.get(task) != null){
                        //if variables, only one receive is needed
                        boolean need_receive = false;
                        if(var_count > 0){
                            //check if receive is neccessary
                            for(VarSymbol vs : notify_from_task){
                                if(!state.method.reuseableVars.get(task).contains(vs)){
                                    need_receive = true;
                                    //we immediately break and receive context
                                    break;
                                }
                            }
                            if(need_receive){
                                print("MPI::COMM_WORLD.Probe(MPI_ANY_SOURCE, MPI_ANY_TAG, state);");
                                nl();
                            }
                        }
                        //decide which variable was transmitted by which mpi-task
                        if(need_receive){
                            for (iTask sender : tasks_signaling_me) {
                                nl();
                                print("if (state.Get_tag() == ");
                                print(state.task_map.get(sender).toUpperCase());
                                print("){");
                                indent();
                                nl();
                                //copy variables needed
                                if(incoming_signal_tasks.get(sender) != null){
                                    print("MPI::COMM_WORLD.Recv(receive_buffer, 1024, MPI::BYTE, MPI_ANY_SOURCE, ");
                                    print(state.task_map.get(sender).toUpperCase());
                                    print(", state);");
                                    nl();
                                    print("memcpy(&receive_context, receive_buffer, sizeof(receive_context));");
                                    nl();
                                    for(VarSymbol vs : incoming_signal_tasks.get(sender)){
                                        print("context.frame.");
                                        print(vs.name.toString().replace('\'', '_'));
                                        print(" = receive_context.frame.");
                                        print(vs.name.toString().replace('\'', '_'));
                                        print(";");
                                        nl();
                                    }
                                }
                                //copy arrays needed
                                if(incoming_signal_arrays_tasks.get(sender) != null){
                                    if(!need_array_calc){
                                        print("int __array_size;");
                                        nl();
                                    }
                                    for(VarSymbol vs : incoming_signal_arrays_tasks.get(sender)){
                                        print("//receive heap allocated object (array)");
                                        nl();
                                        //probe to get amount of elements
                                        print("MPI::COMM_WORLD.Probe(MPI_ANY_SOURCE, MPI_ANY_TAG, state);");
                                        nl();
                                        //get number of elements
                                        print("__array_size = state.Get_count(MPI::BYTE);");
                                        nl();
                                        //allocate space for array
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        //FIXME: this is probably just bullshit...
                                        print(" = new funky::LinearArray< ");
                                        state.typeEmitter.printType(((Type.ArrayType)vs.type.getArrayType()).elemtype);
                                        print(" > ::Version(new funky::LinearArray< ");
                                        state.typeEmitter.printType(((Type.ArrayType)vs.type.getArrayType()).elemtype);
                                        print(" >(");
                                        state.typeEmitter.printExprs(((Type.ArrayType)vs.type.getArrayType()).dom.appliedParams);
                                        print(",true));");
                                        nl();
                                        print("MPI::COMM_WORLD.Recv(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        //copy array into reserved space
                                        print("->toNative(), __array_size, MPI::BYTE, state.Get_source(), state.Get_tag());");
                                        nl();
                                    }
                                }

                                //if recursion optimization is used, cache senders for reuse
                                if(state.method.sym.mayBeRecursive && state.jc.optimizeRecursion){
                                    print(state.task_map.get(sender));
                                    print("_MPI_ID = state.Get_source();");
                                    nl();
                                }
                                undent();
                                nl();
                                //close branch
                                print("}");
                                nl();
                            }
                        }
                    }
                    else{
                        //if variables, only one receive is needed
                        boolean mpi_probed = false;
                        if(var_count > 0){
                            print("MPI::COMM_WORLD.Probe(MPI_ANY_SOURCE, MPI_ANY_TAG, state);");
                            mpi_probed = true;
                        }

                        //decide which variable was transmitted by which mpi-task
                        for (iTask sender : tasks_signaling_me) {
                            nl();
                            print("if (state.Get_tag() == ");
                            print(state.task_map.get(sender).toUpperCase());
                            print("){");
                            indent();
                            nl();
                            //receive context if neccessary
                            if(incoming_signal_tasks.get(sender) != null && incoming_signal_tasks.get(sender).size() > 0){
                                print("MPI::COMM_WORLD.Recv(receive_buffer, 1024, MPI::BYTE, state.Get_source(), state.Get_tag());");
                                nl();
                                print("memcpy(&receive_context, receive_buffer, sizeof(receive_context));");
                            }
                            //copy variables needed
                            if(incoming_signal_tasks.get(sender) != null){
                                for(VarSymbol vs : incoming_signal_tasks.get(sender)){
                                    print("context.frame.");
                                    print(vs.name.toString().replace('\'', '_'));
                                    print(" = receive_context.frame.");
                                    print(vs.name.toString().replace('\'', '_'));
                                    print(";");
                                }
                            }
                            //copy arrays needed
                            if(incoming_signal_arrays_tasks.get(sender) != null){
                                if(!need_array_calc){
                                    print("int __array_size;");
                                    nl();
                                }
                                for(VarSymbol vs : incoming_signal_arrays_tasks.get(sender)){
                                    print("//receive heap allocated object (array)");
                                    nl();
                                    if(!mpi_probed){
                                        //probe to get amount of elements
                                        print("MPI::COMM_WORLD.Probe(MPI_ANY_SOURCE, ");
                                        print(state.task_map.get(sender).toUpperCase());
                                        print(", state);");
                                        nl();
                                    }
                                    //get number of elements
                                    print("__array_size = state.Get_count(MPI::BYTE);");
                                    nl();
                                    //allocate space for array
                                    state.sequentialEmitter.visitVarSymbol(vs, false);
                                    //FIXME: this is probably just bullshit...
                                    print(" = new funky::LinearArray< ");
                                    state.typeEmitter.printType(((Type.ArrayType)vs.type.getArrayType()).elemtype);
                                    print(" > ::Version(new funky::LinearArray< ");
                                    state.typeEmitter.printType(((Type.ArrayType)vs.type.getArrayType()).elemtype);
                                    print(" >(");
                                    state.typeEmitter.printExprs(((Type.ArrayType)vs.type.getArrayType()).dom.appliedParams);
                                    print(",true));");
                                    nl();
                                    print("MPI::COMM_WORLD.Recv(");
                                    state.sequentialEmitter.visitVarSymbol(vs, false);
                                    //copy array into reserved space
                                    print("->toNative(), __array_size, MPI::BYTE, state.Get_source(), state.Get_tag());");
                                    nl();
                                }
                            }

                            //if recursion optimization is used, cache senders for reuse
                            if(state.method.sym.mayBeRecursive && state.jc.optimizeRecursion){
                                print(state.task_map.get(sender).toUpperCase());
                                print("_MPI_ID = state.Get_source();");
                                nl();
                            }
                            undent();
                            nl();
                            //close branch
                            print("}");
                            nl();
                        }
                    }
                }
            }
            nl();

            state.inner_symbols = inner;
            
            //emit code for task
            println();
            printStats(b.getStatements());
            
            
            //print code for recursion optimization if neccessary
            if(state.method.sym.mayBeRecursive && state.jc.optimizeRecursion && tree.reuseableVars.get(p) != null && !tree.reuseableVars.get(p).isEmpty()){
                nl();
                print("//wait if next iteration neccessary");
                nl();
                print("bool iterate;");
                nl();
                print("MPI::COMM_WORLD.Recv(&iterate, 1, MPI::BOOL, MPI_ANY_SOURCE, NOTIFY_REC, state);");
                nl();
                print("if(iterate == true)");
                nl();
                print("{");
                indent();
                nl();
                //copy parameters by invoking param-expression
                if(state.method.taskDepForParamIndex.get(p) != null){
                    int paramIdx = state.method.taskDepForParamIndex.get(p);
                    JCVariableDecl param = state.method.params.get(paramIdx);
                    JCExpression paramExpr = state.method.paramExpressions.get(p);
                    state.sequentialEmitter.visitVarSymbol(param.sym, true);
                    print(" = ");
                    state.sequentialEmitter.printExpr(paramExpr);
                    print(";");
                }
                nl();
                //next iteration
                print("goto REUSE_LOOP_LABEL;");
                undent();
                nl();
                print("}");
                nl();
            }
            
            //special handling for tasks which are suggested for reuse, 
            //but initiate the recursion themselves
            //This currently works if recursion is called within a return statement
            //TODO: also optimize when not called within a return statement
            if(state.method.sym.mayBeRecursive && 
                    state.jc.optimizeRecursion && 
                    tree.reuseableVars.get(p) != null && 
                    !tree.reuseableVars.get(p).isEmpty() &&
                    p.containsReturn()){
                uses_global_iterate.put(p, true);
                nl();
                print("//wait if next iteration neccessary");
                nl();
                print("if(global_iterate == true)");
                nl();
                print("{");
                indent();
                nl();
                //copy parameters by invoking param-expression
                if(state.method.taskDepForParamIndex.get(p) != null){
                    int paramIdx = state.method.taskDepForParamIndex.get(p);
                    JCVariableDecl param = state.method.params.get(paramIdx);
                    JCExpression paramExpr = state.method.paramExpressions.get(p);
                    state.sequentialEmitter.visitVarSymbol(param.sym, true);
                    print(" = ");
                    state.sequentialEmitter.printExpr(paramExpr);
                    print(";");
                }
                nl();
                //next iteration
                print("goto REUSE_LOOP_LABEL;");
                undent();
                nl();
                print("}");
                nl();
                print("else");
                nl();
                print("{");
                indent();
                nl();
                for(iTask task : state.method.reuseableVars.keySet()){
                    if(state.task_map.get(task) != null && state.method.reuseableVars.get(task).size() > 0){
                        print("MPI::COMM_WORLD.Send(&global_iterate, 1, MPI::BOOL,");
                        print(state.task_map.get(task) + "_MPI_ID");
                        print(", NOTIFY_REC);");
                        nl();
                    }
                }
                undent();
                nl();
                print("}");
                nl();
            }

            state.inner_symbols = null;

            state.sequentialEmitter.releaseGroup();

            if (state.profile > 0) {
                nl();
                print("tbb::tick_count END_TASK=tbb::tick_count::now();");
                nl();
                //export time in us!
                print("state.profile_ENTRY->AddMeasurement((END_TASK - START_TASK).seconds()*1e6);"); //time in us
            }

            nl();
            //send results back, if any
            if (p.containsReturn() || p.isFinal()) {
                print("//send result back to parent");
                nl();
                print("memcpy(send_buffer, &context, sizeof(context));");
                nl();
                print("MPI::COMM_WORLD.Send(send_buffer, 1024, MPI::BYTE, source, DATA_TAG);");
                
                /*
                print("//send heap allocated object (array)");
                nl();
                print("MPI::COMM_WORLD.Send(context->GetResult()->toNative(), context->GetResult()->GetCount() * sizeof(context->GetResult()->get(0))");
                print(", MPI::BYTE, source, DATA_TAG);");
                nl();
                */
                
            }

            //release MPI process
            nl();
            print("//work finished -> mark task as available");
            nl();
            print("int mpi_release_var;");
            nl();
            print("MPI::COMM_WORLD.Send(&mpi_release_var, 1, MPI::INT, SCHEDULER, RELEASE_TAG);");
            
            //delete all allocated buffers
            if(p.containsReturn() || p.isFinal()){
                nl();
                print("delete send_buffer;");
                nl();
            }
            
            if(outSymbols.size() > 0){
                nl();
                print("delete notify_buffer;");
                nl();
            }
            
            if(need_notification_buffer){
                nl();
                print("delete receive_buffer;");
                nl();
            }

            if (state.debug_print_task) {
                nl();
                print("printf(\"ExitTask(%s):0x%016p on Thread(0x%016p)\\n\",\"" + name + "\",this,funky::Thread::getCurrentThread());");
                nl();
                print("funky::Debug::FinishTask(" + b.getStatements().head.getTaskID() + ");");
            }

            state.inside_method = false;

            /*
            if (tree.context_ref_count || state.is_event) {
                nl();
                print("context()->release();");
            }
            */
            undent();
            nl();
            print("}");
            nl();
            nl();

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
            /*
             if ((tree.mods.flags & Flags.STATIC) == 0) {
             state.typeEmitter.printType(tree.sym.owner.type);
             print("self;");
             }
             */
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
            /*
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
             */
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
    public void genMethodTGHeader(JCMethodDecl tree, JCTree scheduler, boolean init_self, boolean label) throws IOException {
        println();
        align();

        nl();

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

        print(tree.getID() + "_Context context_instance=" + tree.getID() + "_Context(");
        if (state.kernel || state.inside_kernel) {
            print("SPAWNING_LEVEL,false,false);");
        } else {
            print("0,false,false);");
        }

        //init params
        for (VarSymbol vs : tree.sym.params) {
            println();
            align();
            print("context_instance.params." + vs.name + "=" + vs.name + ";");
        }
        //provide pointer
        nl();
        print(tree.getID() + "_Context* context = &context_instance;");
        
        
        if(scheduler!=tree){
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

        if(state.method.sym.mayBeRecursive && state.jc.optimizeRecursion && state.in_mpi_rec){
            nl();
            print("bool another_round = true;");
            nl();
            print("//for task-local communication");
            nl();
            print("global_iterate = true;");
            nl();
        }

        nl();
        nl();
        print("char* buffer = new char[1024];");
        nl();
        print("memcpy(buffer, &context_instance, sizeof(context_instance));");
        nl();

        /*
         if (state.is_context_refcount) { //void doesn't wait unless finally is used

         if (state.kernel || state.inside_kernel) {
         print(tree.getID() + "_Context* context=new " + tree.getID() + "_Context(__KERNEL_LEVEL");
         } else {
         print(tree.getID() + "_Context* context=new " + tree.getID() + "_Context(" + 0);
         }

         print(",false);");
         nl();
         print("funky::localContext<"+state.typeEmitter.getResType(state.method.restype)+" > lc(context);");

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
         print("funky::localContext<"+state.typeEmitter.getResType(state.method.restype)+" > lc(context);");
         }
         }
         */
        /*
         if (state.current_scheduler.transitive_returns > 0 && tree.getAllSchedules().size() > 1 && tree.final_value != null && tree.transitive_returns > 1)//must refcount possible exits
         {
         nl();
         print("context->exitcount=" + tree.transitive_returns + ";");
         }
         */
        /*
         if ((tree.mods.flags & Flags.STATIC) == 0) {
         println();
         align();
         if (!state.use_local_this) {
         print("context->params.self=const_cast<" + state.method.sym.owner.type + "*>(this);");
         } else {
         print("context->print.self=const_cast<" + state.method.sym.owner.type + "*>(lthis);");
         }
         }
         */
    }

    //called for every path (obviously)
    public void genMethodBodyCore(JCTree scheduler, boolean no_wait, JCMethodDecl tree, Set<iTask> paths) throws IOException {
//must spawn blocking
        nl();
        Set<iTask> blocking = new LinkedHashSet<iTask>();
        Set<iTask> nonblocking = new LinkedHashSet<iTask>();

        Set<VarSymbol> block_outcom = new LinkedHashSet<VarSymbol>();
        Set<VarSymbol> nonblock_outcom = new LinkedHashSet<VarSymbol>();

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

        if (!blocking.isEmpty()) {
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
                genMethodTGHeader(state.method, scheduler, true, false);
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
        if (!nonblocking.isEmpty()) {
            state.path_outcom = nonblock_outcom;
            genMethodTGBodyCore(scheduler, no_wait, tree, nonblocking, true);
        }

        state.method_has_context = old_has_context;

    }


    //alloc and spawn tasks (used by schedulers, like state.method, if/else branch, other control flow)
    public void genMethodTGBodyCore(JCTree scheduler, boolean no_wait, JCMethodDecl tree, Set<iTask> paths, boolean do_kernel) throws IOException {
        nl();
        boolean header_written = false;
        int count_indep = 0;
        int count_evodep = 0;
        
        Set<iTask> returning_tasks = new LinkedHashSet<iTask>();
        
        Map<iTask, String> mpiID = new LinkedHashMap<iTask, String>();
        Map<iTask, Set<VarSymbol>> frame_copy = new LinkedHashMap<iTask, Set<VarSymbol>>();

        int old_joining_paths = state.joining_paths;

        if (scheduler != state.method) {
            state.joining_paths = 0;
        }

        boolean old_dump_kernel = state.dump_kernel;
        state.dump_kernel = do_kernel;

        if (!do_kernel) {
            ListBuffer<String> threadSpawn = new ListBuffer<String>();
            ListBuffer<String> deferSpawn = new ListBuffer<String>();
            Map<iTask, String> buffers = new LinkedHashMap<iTask, String>();

            workerNames = new LinkedHashMap<iTask, String>();
            Set<String> waitingReceives = new LinkedHashSet<String>();
            Set<String> deferredReceives = new LinkedHashSet<String>();
            int taskCounter = 0;

            //FIXME: must be topological order!
            java.util.Deque<iTask> pathsSorted = new LinkedList<iTask>();
            for (iTask t : paths) {
                if (state.in_map.get(t) == 0) {
                    pathsSorted.addLast(t);
                } else {
                    pathsSorted.addFirst(t);
                }
            }
            
            Set<iTask> mpiID_obtained = new LinkedHashSet<iTask>();
            nl();
            int counter = 0;
            for(iTask task : pathsSorted){
                if(mpiID.get(task) == null && state.task_map.get(task) != null){
                    String name = state.task_map.get(task) + "_MPI_ID";
                    
                    mpiID.put(task, name);
                    if(!state.in_mpi_rec){
                        print("int " + name + " = Util::get_next_free_worker();");
                        nl();
                    }
                    mpiID_obtained.add(task);
                }
            }
                
           //there is one task per iTask p
            for (iTask p : pathsSorted) {
                Set<JCTree> calc_nodes = p.getCalcNodes();

                if (calc_nodes.isEmpty())//ignore empty paths
                {
                    continue;
                }

                nl();

                JCTree fcn = p.getFirstCalcNode(state.method, calc_nodes);
                if (fcn.scheduler != scheduler || fcn.getTag() == JCTree.CF) {
                    continue;//are we the scheduler for this path (or is it control flow??)
                }

                state.DumpSchedule(scheduler, p, state.task_map, state.dump_kernel);

                if (!header_written)//we wait for the joining paths, set our refcount
                {

                    //Set<Arc> joining = tree.getJoiningNodes();
                    int joining_edges = state.joining_paths;
                    if (joining_edges > 0 && !state.is_evo) {
                        //println();
                        //align();
                        //print("" + state.sequentialEmitter.self_task() + "set_ref_count(" + (joining_edges + 1) + ");"); //+1 for wait
                    }
                    /*
                     if (state.task_return) {
                     nl();
                     print(state.sequentialEmitter.context() + "->tasks.task_return=new(tbb::task::allocate_additional_child_of(*funky::TaskRoot<>::GetRoot())) funky::Task<" + tree.getID() + "_Context>(NULL);");
                     nl();
                     print(state.sequentialEmitter.context() + "->tasks.task_return->set_ref_count(2);");
                     }
                     */
                    nl();
                    //print("tbb::task_list indep_tasks;");
                    header_written = true;
                }

                int incom = state.in_map.get(p);
                String task_name = state.task_map.get(p);
                boolean is_joining = state.joining_map.get(p);

                if (is_joining) {
                    if(state.in_mpi_rec){
                        //in a _MPI method
                    }
                    else{
                        //standard method
                        //get worker
                        nl();
                        /*
                        print("int " + mpiID.get(p) + " = ");
                        print("Util::get_next_free_worker();");
                        */
                        nl();
                        //send data
                        print("MPI::COMM_WORLD.Send(buffer, 1024, MPI::BYTE, ");
                        print(mpiID.get(p));
                        print(", ");
                        print(task_name.toUpperCase());
                        print(");");
                        nl();
                        //Send additional arrays
                        Set<VarSymbol> taskParameters = task_parameters.get(p);
                        //check if arrays follow
                        for(VarSymbol vs : taskParameters){
                            if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                                print("//send heap allocated object (array)");
                                nl();
                                print("MPI::COMM_WORLD.Send(");
                                state.sequentialEmitter.visitVarSymbol(vs, false);
                                print("->toNative(), ");
                                state.sequentialEmitter.visitVarSymbol(vs, false);
                                print("->GetCount() * sizeof(");
                                state.sequentialEmitter.visitVarSymbol(vs, false);
                                print("->get(0))");
                                print(", MPI::BYTE, ");
                                print(mpiID.get(p));
                                print(", DATA_TAG);");
                                nl();
                            }
                        }
                        //if task generates input for another task, tell him the id of other task
                        //but only, if it must notify
                        printNotificationSubmissions(p, task_name, mpiID, mpiID.get(p));
                        
                        
                    }
                    
                } else {
                    if (incom != 1 || state.is_evo || state.is_constructor) {
                        //add dangling tasks to root so that they finish before we exit
                        println();
                        align();
                        if (!(state.is_event || state.is_evo || state.is_constructor)) {
                            //TODO:
                        } else {
                            println();
                            align();
                            state.joining_paths++;
                            //print("task_handle* task_instance_" + task_name + "=new(" + state.sequentialEmitter.self_task() + "allocate_child()) " + task_name + "(" + state.sequentialEmitter.context() + ");");
                            count_evodep++;
                            if (incom == 1) {
                                println();
                                align();
                                print(state.sequentialEmitter.context() + "->tasks.task_instance_" + task_name + "=" + "task_instance_" + task_name + ";");
                            }
                        }
                    }

                    if (incom >= 1) {
                        if(state.in_mpi_rec){
                            //print optimized method for recursion _MPI
                            if(state.method.reuseableVars.get(p) != null){
                                if(state.task_map.get(p) != null && state.method.reuseableVars.get(p).size() > 0){
                                    //mpi_id of task is a method parameter
                                    //only start new iteration
                                    print("MPI::COMM_WORLD.Send(&another_round, 1, MPI::BOOL,");
                                    print(state.task_map.get(p) + "_MPI_ID");
                                    print(", NOTIFY_REC);");
                                }
                            }
                            else{
                                //this case works basically as usual
                                //we must obtain a new mpi-id
                                nl();
                               
                                print("int " + mpiID.get(p) + " = ");
                                print("Util::get_next_free_worker();");
                                
                                nl();
                                //send data
                                print("MPI::COMM_WORLD.Send(buffer, 1024, MPI::BYTE, ");
                                print(mpiID.get(p));
                                print(", ");
                                print(task_name.toUpperCase());
                                print(");");
                                nl();
                                //Send additional arrays
                                Set<VarSymbol> taskParameters = task_parameters.get(p);
                                //check if arrays follow
                                for(VarSymbol vs : taskParameters){
                                    if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                                        print("//send heap allocated object (array)");
                                        nl();
                                        print("MPI::COMM_WORLD.Send(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->toNative(), ");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->GetCount() * sizeof(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->get(0))");
                                        print(", MPI::BYTE, ");
                                        print(mpiID.get(p));
                                        print(", DATA_TAG);");
                                        nl();
                                    }
                                }
                                //if task generates input for another task, tell him the id of the other task
                                //but only, if it must notify
                                printNotificationSubmissions(p, task_name, mpiID, mpiID.get(p));
                                
                            }
                        }
                        else{
                            //print normal method
                            if(state.jc.optimizeRecursion && state.method.reuseableVars.get(p) != null){
                                //with some optimizations for recurions
                                if(!mpiID_obtained.contains(p)){
                                    //only demand ID if we have not done already
                                    nl();
                                    print("int " + mpiID.get(p) + " = ");
                                    print("Util::get_next_free_worker();");
                                    nl();
                                }
                                nl();
                                print("MPI::COMM_WORLD.Send(buffer, 1024, MPI::BYTE, ");
                                print(mpiID.get(p));
                                print(", ");
                                print(task_name.toUpperCase());
                                print(");");
                                nl();
                                //Send additional arrays
                                Set<VarSymbol> taskParameters = task_parameters.get(p);
                                //check if arrays follow
                                for(VarSymbol vs : taskParameters){
                                    if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                                        print("//send heap allocated object (array)");
                                        nl();
                                        print("MPI::COMM_WORLD.Send(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->toNative(), ");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->GetCount() * sizeof(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->get(0))");
                                        print(", MPI::BYTE, ");
                                        print(mpiID.get(p));
                                        print(", DATA_TAG);");
                                        nl();
                                    }
                                }
                                //if task generates input for another task, tell him the id of other task
                                //but only, if it must notify
                                printNotificationSubmissions(p, task_name, mpiID, mpiID.get(p));
                              
                            }
                            else{
                                //standard
                                /*
                                print("int " + mpiID.get(p) + " = Util::get_next_free_worker();");
                                nl();
                                */
                                print("MPI::COMM_WORLD.Send(buffer, 1024, MPI::BYTE, ");
                                print(mpiID.get(p));
                                print(", ");
                                print(task_name.toUpperCase());
                                print(");");
                                nl();
                                //check if arrays follow
                                Set<VarSymbol> taskParameters = task_parameters.get(p);
                                for(VarSymbol vs : taskParameters){
                                    if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                                        print("//send heap allocated object (array)");
                                        nl();
                                        print("MPI::COMM_WORLD.Send(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->toNative(), ");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->GetCount() * sizeof(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->get(0))");
                                        print(", MPI::BYTE, ");
                                        print(mpiID.get(p));
                                        print(", DATA_TAG);");
                                        nl();
                                    }
                                }
                                //if task generates input for another task, tell him the id of other task
                                Set<VarSymbol> taskOutCom = p.getOutSymbols();
                                for (VarSymbol vs : taskOutCom) {
                                    Set<iTask> children = signal_map.get(vs);
                                    if (children != null) {
                                        for (iTask task : children) {
                                            if (task != p) {
                                                print("MPI::COMM_WORLD.Send(&");
                                                print(mpiID.get(task));
                                                print(", 1, MPI::INT, ");
                                                print(mpiID.get(p));
                                                print(", ");
                                                print(task_name.toUpperCase());
                                                print(");");
                                                nl();
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    } else if (incom == 0) {
                        if(state.in_mpi_rec){
                            //_MPI method
                            if(state.method.reuseableVars.get(p) != null){
                                if(state.task_map.get(p) != null && state.method.reuseableVars.get(p).size() > 0){
                                    //mpi_id of task is a method parameter
                                    //only start new iteration
                                    print("MPI::COMM_WORLD.Send(&another_round, 1, MPI::BOOL,");
                                    print(state.task_map.get(p) + "_MPI_ID");
                                    print(", NOTIFY_REC);");
                                }
                            }
                            else{
                                //this case works basically as usual
                                //we must obtain a new mpi-id
                                nl();
                                /*
                                print("int " + mpiID.get(p) + " = ");
                                print("Util::get_next_free_worker();");
                                */
                                nl();
                                //send data
                                print("MPI::COMM_WORLD.Send(buffer, 1024, MPI::BYTE, ");
                                print(mpiID.get(p));
                                print(", ");
                                print(task_name.toUpperCase());
                                print(");");
                                nl();
                                //Send additional arrays
                                Set<VarSymbol> taskParameters = task_parameters.get(p);
                                //check if arrays follow
                                for(VarSymbol vs : taskParameters){
                                    if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                                        print("//send heap allocated object (array)");
                                        nl();
                                        print("MPI::COMM_WORLD.Send(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->toNative(), ");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->GetCount() * sizeof(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->get(0))");
                                        print(", MPI::BYTE, ");
                                        print(mpiID.get(p));
                                        print(", DATA_TAG);");
                                        nl();
                                    }
                                }
                                //if task generates input for another task, tell him the id of other task
                                //but only, if it must notify
                                printNotificationSubmissions(p, task_name, mpiID, mpiID.get(p));
                                
                            }
                        }
                        else{
                            if(state.jc.optimizeRecursion && state.method.sym.mayBeRecursive && state.method.reuseableVars.get(p) != null){
                                //standard method with optimization
                                //with some optimizations for recurions
                                if(!mpiID_obtained.contains(p)){
                                    //only demand ID if we have not done already
                                    nl();
                                    print("int " + mpiID.get(p) + " = ");
                                    print("Util::get_next_free_worker();");
                                    nl();
                                }
                                nl();
                                print("MPI::COMM_WORLD.Send(buffer, 1024, MPI::BYTE, ");
                                print(mpiID.get(p));
                                print(", ");
                                print(task_name.toUpperCase());
                                print(");");
                                nl();
                                //Send additional arrays
                                Set<VarSymbol> taskParameters = task_parameters.get(p);
                                //check if arrays follow
                                for(VarSymbol vs : taskParameters){
                                    if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                                        print("//send heap allocated object (array)");
                                        nl();
                                        print("MPI::COMM_WORLD.Send(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->toNative(), ");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->GetCount() * sizeof(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->get(0))");
                                        print(", MPI::BYTE, ");
                                        print(mpiID.get(p));
                                        print(", DATA_TAG);");
                                        nl();
                                    }
                                }
                                //if task generates input for another task, tell him the id of other task
                                //but only, if it must notify
                                printNotificationSubmissions(p, task_name, mpiID, mpiID.get(p));
                                 
                            }
                            else{
                                //standard version
                                //standard
                                /*
                                print("int " + mpiID.get(p) + " = Util::get_next_free_worker();");
                                */
                                nl();
                                print("MPI::COMM_WORLD.Send(buffer, 1024, MPI::BYTE, ");
                                print(mpiID.get(p));
                                print(", ");
                                print(task_name.toUpperCase());
                                print(");");
                                nl();
                                //check if arrays follow
                                Set<VarSymbol> taskParameters = task_parameters.get(p);
                                for(VarSymbol vs : taskParameters){
                                    if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                                        print("//send heap allocated object (array)");
                                        nl();
                                        print("MPI::COMM_WORLD.Send(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->toNative(), ");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->GetCount() * sizeof(");
                                        state.sequentialEmitter.visitVarSymbol(vs, false);
                                        print("->get(0))");
                                        print(", MPI::BYTE, ");
                                        print(mpiID.get(p));
                                        print(", DATA_TAG);");
                                        nl();
                                    }
                                }
                                //if task generates input for another task, tell him the id of other task
                                printNotificationSubmissions(p, task_name, mpiID, mpiID.get(p));
                                
                            }
                        }
                    }
                    if (p.isFinal()) {
                        if(!state.in_mpi_rec){
                            //prepare task-return
                            prepareTaskReturn(taskCounter, mpiID, p, waitingReceives, frame_copy, returning_tasks, deferredReceives, buffers);
                        }
                        else{
                            //we print optimized version
                            //if function is tail-recursive, we needn't expect a return value
                            if(uses_global_iterate.get(p) != null && uses_global_iterate.get(p) == true){
                                //do nothing
                            }
                            else{
                                prepareTaskReturn(taskCounter, mpiID, p, waitingReceives, frame_copy, returning_tasks, deferredReceives, buffers);
                            }
			
                        }
                    }
                }
                taskCounter++;
            }
            
            //now send every task the IDs of the other tasks
            if(state.jc.optimizeRecursion){
                if(!state.in_mpi_rec){
                    for(iTask p : pathsSorted){
                        if(state.task_map.get(p) != null && state.method.reuseableVars.get(p) != null && !state.method.reuseableVars.get(p).isEmpty()){
                            nl();
                            print("//send MPI-IDs of reused tasks to " + state.task_map.get(p));
                            nl();
                            for(iTask reusedTask : state.method.reuseableVars.keySet()){
                                if(mpiID.get(reusedTask) != null && reusedTask != p){
                                    print("MPI::COMM_WORLD.Send(&");
                                    print(mpiID.get(reusedTask));
                                    print(", 1, MPI::INT, ");
                                    print(mpiID.get(p));
                                    print(", NOTIFY_ID);");
                                    nl();
                                }
                            }
                        }
                    }
                }
            }
            
            nl();
            //FIXME: replace by Request::Waitall()
            for (String s : waitingReceives) {
                nl();
                print(s + ".Wait();");
            }
            Set<String> contexts = new LinkedHashSet<String>();
            //copy values from receive-buffers into local context
            for(iTask task : frame_copy.keySet()){
                nl();
                String context_name = "rec_buffer_" + state.task_map.get(task);
                contexts.add(context_name);
                print(tree.getID() + "_Context " + context_name + ";");
                nl();
                print("memcpy(&");
                print(context_name);
                print(", ");
                print(buffers.get(task));
                print(", sizeof(");
                print(context_name);
                print("));");
                /* must be treated separately
                if(task.containsReturn()){
                    nl();
                    print("context_instance.SetReturn(");
                    print(context_name);
                    print(".GetReturn());");
                }
                */
                for(VarSymbol vs : frame_copy.get(task)){
                    nl();
                    state.sequentialEmitter.visitVarSymbol(vs, false);
                    print(" = ");
                    print(context_name);
                    print(".frame.");
                    print(vs.name.toString().replace('\'', '_'));
                    print(";");
                }
                nl();
                
            }
            
            //shutdown worker processes
            if(state.kernel){
                if(!state.in_mpi_rec && 
                        state.method.sym.mayBeRecursive && 
                        state.jc.optimizeRecursion && 
                        tree.reuseableVars.size() > 0 &&
                        state.method.reuseableVars.keySet().size() < state.task_map.keySet().size()){
                    nl();
                    print("//shutdown worker processes");
                    nl();
                    print("bool another_round = false;");
                    nl();
                    for(iTask task : state.method.reuseableVars.keySet()){
                        if(mpi_IDs.get(task) != null && state.task_map.get(task) != null && state.method.reuseableVars.get(task).size() > 0){
                            print("MPI::COMM_WORLD.Send(&another_round, 1, MPI::BOOL, ");
                            print(mpi_IDs.get(task));
                            print(", NOTIFY_REC);");
                            nl();
                        }
                    }
                }
            }
            
            /*
            if(returning_tasks.size() > 0){
                nl();
                nl();
                print("int __array_size;");
                nl();
                print("MPI::Status state;");
                nl();
                
                for(iTask task : returning_tasks){
                    String context_name = "rec_buffer_" + state.task_map.get(task);
                    if(!contexts.contains(context_name)){
                        print(tree.getID() + "_Context " + context_name + ";");
                        nl();
                        print("memcpy(&");
                        print(context_name);
                        print(", ");
                        print(buffers.get(task));
                        print(", sizeof(");
                        print(context_name);
                        print("));");
                    }
                }
            }
            
            //copy return value
            for(iTask returning_task : returning_tasks){
                if(returning_task.getOutComMap().size() > 0){
                    nl();
                    
                    String context_name = "rec_buffer_" + state.task_map.get(returning_task);
                    
                    /* print(tree.getID() + "_Context " + context_name + ";");
                    nl();
                    print("memcpy(&");
                    print(context_name);
                    print(", ");
                    print(buffers.get(returning_task));
                    print(", sizeof(");
                    print(context_name);
                    print("));");
                    
                    for(VarSymbol vs : returning_task.getOutComMap().keySet()){
                        nl();
                        if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                            print("//receive heap allocated return value (array)");
                            nl();
                            //probe to get amount of elements
                            print("MPI::COMM_WORLD.Probe(");
                            print(state.task_map.get(returning_task) + "_MPI_ID");
                            print(", DATA_TAG, state);");
                            nl();
                            //get number of elements
                            print("__array_size = state.Get_count(MPI::BYTE);");
                            nl();
                            //allocate space for array
                            state.sequentialEmitter.visitVarSymbol(vs, false);
                            //FIXME: this is probably just bullshit...
                            print(" = new funky::LinearArray< ");
                            state.typeEmitter.printType(((Type.ArrayType)vs.type.getArrayType()).elemtype);
                            print(" > ::Version(new funky::LinearArray< ");
                            state.typeEmitter.printType(((Type.ArrayType)vs.type.getArrayType()).elemtype);
                            print(" >(");
                            state.typeEmitter.printExprs(((Type.ArrayType)vs.type.getArrayType()).dom.appliedParams);
                            print(",true));");
                            nl();
                            print("MPI::COMM_WORLD.Recv(");
                            state.sequentialEmitter.visitVarSymbol(vs, false);
                            //copy array into reserved space
                            print("->toNative(), __array_size, MPI::BYTE,");
                            print(state.task_map.get(returning_task) + "_MPI_ID");
                            print(", DATA_TAG, state);");
                            nl();
                            print("context->SetReturn(");
                            state.sequentialEmitter.visitVarSymbol(vs, false);
                            print(");");
                            nl();
                        }
                        else{
                            nl();
                            print("context->SetReturn(");
                            print(context_name);
                            print(".GetReturn()");
                            print(");");
                            nl();
                        }
                    }
                }
            }
            */
            nl();


            if (count_evodep > 0 && (state.is_evo || state.is_constructor)) {
                //nl();
                //print("" + state.sequentialEmitter.self_task() + "set_ref_count(" + (count_evodep + 1) + ");");
                //else
                //	print(""+state.sequentialEmitter.self_task()+"set_ref_count("+(count_join+1)+");");//set_ref_count(2) for return_task?
            }
            if (scheduler != state.method) {
                int joining_edges = state.joining_paths;
                if (joining_edges > 0 && !state.is_evo) {
                    println();
                    align();
                    //print("" + state.sequentialEmitter.self_task() + "set_ref_count(" + (joining_edges + 1) + ");"); //+1 for wait
                }
            }

            if (count_indep > 0) {
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
                        branch_wait = true;
                    } else //we need to wait for the result
                    {
                        if (!(state.is_event || state.is_evo || state.is_constructor)) {
                            if (count_indep - threadSpawn.size() > 0) {
                                //print("" + state.sequentialEmitter.self_task() + "spawn(indep_tasks);");
                            }
                            for (String s : threadSpawn) {
                                nl();
                                print(s);
                            }
                        } else {
                            for (String s : threadSpawn) {
                                nl();
                                print(s);
                            }
                            if (count_indep - threadSpawn.size() > 0) {
                                //print("" + state.sequentialEmitter.self_task() + "spawn_and_wait_for_all(indep_tasks);"); //spawn indep tasks and wait for joining tasks to dec our refc
                            } else {
                                //print("" + state.sequentialEmitter.self_task() + "wait_for_all();");
                            }
                        }
                    }
                }
            } else {
                if (state.is_evo || (scheduler != state.method && state.joining_paths > 0)) {
                    nl();
                    if (state.inside_task == null) {
                        //print(state.sequentialEmitter.self_task());
                    }
                    //print("wait_for_all();");

                }
            }
            nl();
            nl();
            for(iTask t : buffers.keySet()){
                print("delete " + buffers.get(t) + ";");
                nl();
            }
            print("delete buffer;");
            nl();
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

        if (state.kernel) {
            nl();
            print("if(SPAWNING_LEVEL < 5){");
            indent();
            nl();
            
//			state.joining_paths = state.kernel_joining_paths;
//			state.task_return = state.kernel_task_return;

            if (!state.method_has_context) {
                state.kernel = false;
                state.inside_kernel = true;
                genMethodTGHeader(state.method, scheduler, true, true);
                state.inside_kernel = false;
                state.kernel = true;
            }
        }

        genMethodTGBodyCore(scheduler, no_wait && !state.kernel, tree, paths, false);

        if (state.kernel) {
            state.kernel = false;
            state.inside_kernel = true;
            
            genMethodTGFooter(state.method);
//			state.joining_paths = joining;
//			state.task_return = taskret;
            state.inside_kernel = false;
            state.kernel = true;

            state.task_return = false;
            
            undent();
            nl();
            
            print("} else {");
            indent();
            println();

            state.method_has_context = old_has_context;

//			state.joining_paths = state.kernel_joining_paths;
//			state.task_return = state.kernel_task_return;

            MethodSymbol old_redirect = state.redirect_recursion;
            if (state.method.restricted_impls != null && state.method.restricted_impls.get(1) != null) {
                state.redirect_recursion = state.method.restricted_impls.get(1).sym;
            }

            genMethodBodyCore(scheduler, no_wait, tree, paths);

            state.redirect_recursion = old_redirect;

            undent();
            nl();
            print("}");
        }

        state.joining_paths = joining;
        state.task_return = taskret;

        state.method_has_context = old_has_context;
        state.loop_label_sched = old_label_sched;
        state.loop_label_kernel = old_label_kernel;
    }

    public void genMethodTGFooter(JCMethodDecl tree) throws IOException {
        //if (!state.is_void)
        {

            //clean up return task
            if (state.task_return) {
                /*
                 nl();
                 print(state.sequentialEmitter.context() + "->tasks.task_return->wait_for_all();");
                 nl();
                 print(state.sequentialEmitter.context() + "->tasks.task_return->parent()->decrement_ref_count();");
                 nl();
                 print(state.sequentialEmitter.context() + "->tasks.task_return->set_parent(NULL);");
                 print("tbb::task::destroy(*" + state.sequentialEmitter.context() + "->tasks.task_return);");
                 */
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


            /*
             if(!state.is_context_refcount)
             {
             nl();
             print("while(context_instance.refcount.load()>1);");//not very nice, active wait for refcount (we cannot exit the method before alls refs are released)
             }
             */
            if (!state.is_event && !state.is_void && tree.getReturnType() != null && state.method_has_context && state.method.final_value == null) {
                println();
                align();
                Type t = tree.getReturnType().type;
                String rettype = t.toString();
                if (!t.isPrimitive()) {
                    rettype += "*";
                }

                print("return context->GetReturn();");

            }
            state.loop_label_sched = null;
            state.loop_label_kernel = false;

        }

    }

    //!! process output of PathGen, sets up all kinds of maps so we can find out which tasks must start which other tasks under which conditions
    //also prints the task
    public Set<iTask> preparePaths(JCMethodDecl tree) throws IOException {

        //get set of pathsets we might have to schedule (from TaskGen.java)
        Set<iTask> paths = tree.getAllSchedules();

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

        //used later to distinguish notifying tasks
        signalFrom = new LinkedHashMap<iTask, Set<VarSymbol>>();

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

            boolean loc_blocking_only = getSchedulerPaths(p.getFirstCalcNode(state.method, calcNodes).scheduler, paths, true) <= 1;

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
                                state.in_map.put(it, state.in_map.get(it) - 1);
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
        state.spawns = hasSchedulerPaths(paths);

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

            //print actual tasks:
            for (iTask p : paths) {
                Set<JCTree> calcNodes = p.getCalcNodesTransitive();

                JCTree scheduler = p.getFirstCalcNode(state.method, calcNodes).scheduler;
                int scheduler_spawns = getSchedulerPaths(scheduler, paths, true);
                blocking_only = scheduler_spawns <= 1;
                if (!p.getPathBlock().stats.isEmpty() && (!blocking_only || p.containsForcedSpawn()))//DO NOT USE CALC NODES: THEY CONTAIN JCTree.CF!
                {
                    state.path_outcom = p.getNullFreeOutSymbols(); //used int printStat to generate code to spwan dependent tasks

                    //FIXME: set this to false if there are no inependent tasks??:
                    state.task_return = scheduler.transitive_returns > 0; //if this scheduler or a scheduler spawned from this scheduler has more than one return path then we must count

                    printTask(tree, p, p.getPathBlock(), state.task_map.get(p), p.getInnerSymbols(calcNodes, state.method), state.is_context_refcount && state.method.dangling_paths.contains(p));

                    state.task_return = false;

                    state.path_outcom = null;
                }
                blocking_only = false;
            }

        }

        return paths;
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
            if (fcn.scheduler == scheduler && fcn.getTag() != JCTree.CF && !p.isCFDEPTo(scheduler) && (uncoditional || in == null || in != 1)) {
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
                }

            }
            if (p.containsForcedSpawn()) {
                count++;
                state.blocking_spawns++;
            }
        }
        return count;

    }

    //added to confirm with LowerTasksTBB
    public void fixRefsInBranches(JCCF tree) throws IOException {
        if (state.inside_task != null) {
            //volatile vars may introduce asymetric refs in branches, which we fix here
            //additionalRefs is constructed in TaskSet.getInComImplicit
            int fixCount = 0;
            for (TaskSet ts : tree.additionalRefs.keySet()) {
                if (state.in_map.get(ts) != 1) {
                    fixCount++;
                    nl();
                    print(state.sequentialEmitter.context() + "->tasks.task_instance_" + state.task_map.get(ts) + "->decrement_ref_count();");
                }
            }
            if (fixCount > 0) {
                nl();
            }
        }
    }
    
    /*
     * decide for each task which other MPI_IDs must be transferred
     */
    private void printNotificationSubmissions(iTask p, String task_name, Map<iTask, String> mpiIDs, String next) throws IOException{
        Set<VarSymbol> all_notify = signalFrom.get(p);
        Set<VarSymbol> vars_to_notify = new LinkedHashSet<VarSymbol>();
        Set<VarSymbol> arrays_to_notify = new LinkedHashSet<VarSymbol>();
        
        for(VarSymbol vs : all_notify){
            if(vs.type.getArrayType().tag == TypeTags.ARRAY){
                arrays_to_notify.add(vs);
            }
            else{
                vars_to_notify.add(vs);
            }
        }
        
        boolean must_notify = false;
        
        for(VarSymbol vs : vars_to_notify){
            //get tasks which must be notified
            Set<iTask> tasks = signal_map.get(vs);
            for(iTask task : tasks){
                Set<VarSymbol> reuseableVars = state.method.reuseableVars.get(task);
                if(reuseableVars == null || !reuseableVars.contains(vs)){
                    //we must notify
                    must_notify = true;
                }
            }
        }
        
        if(must_notify){
            for(VarSymbol vs : vars_to_notify){
                Set<iTask> tasks = signal_map.get(vs);
                for(iTask task : tasks){
                    print("MPI::COMM_WORLD.Send(&");
                    print(mpiIDs.get(task));
                    print(", 1, MPI::INT, ");
                    print(next);
                    print(", NOTIFY_ID_TAG);");
                    nl();
                }
            }
        }
        //same with arrays
        must_notify = false;
        for(VarSymbol vs : arrays_to_notify){
            //get tasks which must be notified
            Set<iTask> tasks = signal_map.get(vs);
            if(tasks != null){
                for(iTask task : tasks){
                    Set<VarSymbol> reuseableVars = state.method.reuseableVars.get(task);
                    if(reuseableVars == null || !reuseableVars.contains(vs)){
                        //we must notify
                        must_notify = true;
                    }
                }
            }
        }
        
        if(must_notify){
            for(VarSymbol vs : vars_to_notify){
                Set<iTask> tasks = signal_map.get(vs);
                for(iTask task : tasks){
                    print("MPI::COMM_WORLD.Send(&");
                    print(mpiIDs.get(task));
                    print(", 1, MPI::INT, ");
                    print(next);
                    print(", NOTIFY_ID_TAG);");
                    nl();
                }
            }
        }
    }
    
    private void prepareTaskReturn(int taskCounter, 
            Map<iTask, String> mpiID, 
            iTask p, 
            Set<String> waitingReceives, 
            Map<iTask, Set<VarSymbol>> frame_copy, 
            Set<iTask> returning_tasks,
            Set<String> deferredReceives,
            Map<iTask, String> buffers) throws IOException{
        //task returns something, prepare receives
        String mpiRequest = "request" + taskCounter;
        String recbuffer = "receive_buffer_" + mpiID.get(p);
        waitingReceives.add(mpiRequest);
        nl();
        print("char* ");
        print(recbuffer);
        print(" = new char[1024];");
        nl();
        print("MPI::Request ");
        print(mpiRequest);
        print(" = MPI::COMM_WORLD.Irecv(");
        print(recbuffer);
        print(", 1024, MPI::BYTE, ");
        print(mpiID.get(p));
        print(", DATA_TAG);");

        Set<VarSymbol> frame=new LinkedHashSet<VarSymbol>();
        Set<VarSymbol> out=new LinkedHashSet<VarSymbol>();

        out.addAll(p.getOutSymbols());
        Set<VarSymbol> syms=p.getInSymbols();
        syms.retainAll(state.com);
        frame.addAll(syms);

        frame.removeAll(out);

        for(VarSymbol vs : frame){
            if((vs.flags_field & Flags.PARAMETER) == 0){
                if(frame_copy.get(p) == null){
                    frame_copy.put(p, new LinkedHashSet<VarSymbol>());
                }
                frame_copy.get(p).add(vs);
            }
        }

        returning_tasks.add(p);

        //p.getOutSymbols()
        StringBuilder sb = new StringBuilder();
        deferredReceives.add(sb.toString());
        buffers.put(p, recbuffer);
    }
}
