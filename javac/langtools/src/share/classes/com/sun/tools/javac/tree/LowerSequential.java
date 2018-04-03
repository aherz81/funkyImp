/*CONVERSION*//*
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

import static com.sun.tools.javac.code.Flags.*;
import com.sun.tools.javac.comp.Scheduler;
import org.jgrapht.DirectedGraph;

/**
 * Prints out a tree as an indented Java state.source program.
 *
 * <p>
 * <b>This is NOT part of any API supported by Sun Microsystems. If you write
 * code that depends on this, you do so at your own risk. This code and its
 * internal interfaces are subject to change or deletion without notice.</b>
 */
//emitter for std (non-parallel) stuff, uses other emitters wher appropriate
public class LowerSequential extends Emitter {

    DirectedGraph<JCTree, Arc> callGraph; //constructed else where, used to see whether we're inside a recursion
    java.util.Map<JCTree, Integer> topolNodes;
    Set<VarSymbol> tmpSymbol = null;
    boolean self_selected = false; //must use local_this-> when accessing member vars
    boolean needs_context = false; //does method need context?
    JCWhere whereExpr = null; //for atomic stuff
    boolean arg_out = false; //is current var input or output (read/write)
    boolean skip_new = false; //avoid emitting new since it has already been done
    boolean skip_this = false; //?? :)
    boolean disable_apply = false; //disable selector xyz.something, as xyz is patched with something else
    boolean forcegc = false; //force gc at the end of program?

    Map<VarSymbol, Pair<VarSymbol, Pair<Integer, JCExpression>>> matchmap = null;

    public LowerSequential(LowerTreeImpl state, DirectedGraph<JCTree, Arc> callGraph, Map<JCTree, Integer> topolNodes, boolean forcegc) {
        super(state);
        this.callGraph = callGraph;
        this.topolNodes = topolNodes;
        this.forcegc = forcegc;
    }

    //get code to access context in current environment (state.inside_task??)
    String context() {
        if (state.inside_task != null && !state.inside_task.equals(state.method.getID())) {
            return "context()";
        } else {
            if (state.method_has_context) {
                return "context";
            } else {
                return "((" + state.method.getID() + "_Context*)(SELFTASK(" + state.typeEmitter.getResType(state.method.restype) + ").context()))";
            }
        }
    }

    String self_task_name() {
        if (state.inside_task == null) {
            if (state.method_has_context) {
                return "*self_task";
            } else {
                return "SELF()";
            }
        } else {
            return "";
        }
    }
    
    //get code to access current task in current environment (state.inside_task??)
    String self_task() {
        if (state.inside_task == null) {
            if (state.method_has_context) {
                return "self_task->";
            } else {
                return "SELF().";
            }
        } else {
            return "";
        }
    }

    //groups are implemented as locks..call funky api to release lock..
    void releaseGroup() throws IOException {
        if (state.current_group.length() != 0) //exit current group
        {
            nl();
            print("funky::Group::releaseGroup(TaskGroupInitializer_" + state.current_class.replace('.', '_') + "::get" + state.current_group + "());");
            //nl();

            state.current_group = "";
        }
    }

    /**
     * Derived visitor state.method: print statement tree.
     */
    public void printStat(JCTree tree) throws IOException {

        Map<VarSymbol, JCExpression> oldLastProjectionArgs = state.lastProjectionArgs;

        //skip empty decls (local vars are emitted by genImplicitDecls)
        boolean empty_decl = (tree.getTag() == JCTree.VARDEF && ((JCVariableDecl) tree).sym.isLocal() && ((JCVariableDecl) tree).init == null);

        if (tree.nop || empty_decl) { //skip nops
            return;
        }

        state.current_tree = tree; //what is the statement we curently operate on

        state.debugPos(tree.pos); //dump dbg pos

        //handle possible different group:
        if (state.method != null && !tree.groups.isEmpty() && (state.inside_task != null || !state.method.sym.groups.equals(tree.groups.iterator().next())))//when inside grouped state.method and no spawn
        {
            if (!state.skipNL && tree.groups.size() > 0 && (tree.is_blocking || !state.current_group.equals(tree.groups.iterator().next()))) //exit group lock before blocking!
            {
                releaseGroup();

                if (tree.groups.size() > 0) {
                    nl();
                    print("funky::Group::aquireGroup(TaskGroupInitializer_" + state.current_class.replace('.', '_') + "::get" + tree.groups.iterator().next() + "());");
                    nl();
                    state.current_group = tree.groups.iterator().next();
                }
            }
        }

        //handle #TIME pragma
        if (!tree.time.isEmpty()) {
            VarSymbol vs = tree.time.iterator().next();
            nl();
            print("static funky::ProfileEntry* __profile__" + tree.pos + "=funky::Profile::RegisterCustom(\"" + tree.pos + "\",\"" + vs.owner.name + "\",\"" + vs.time + "\");");

            nl();
            print("tbb::tick_count __TIME__" + tree.pos + "=tbb::tick_count::now();");
        }

        //dump actual expression
        state.debugPos(tree.pos); //dump dbg pos
        printExpr(tree, TreeInfo.notExpression);

        if (tree.getTag() != JCTree.SKIP && tree.getTag() != JCTree.BLOCK && !state.skipNL) {
            println();
            align();
        }

        //finish timing
        if (!tree.time.isEmpty()) {
            nl();
            state.debugPos(tree.pos); //dump dbg pos
            print("__profile__" + tree.pos + "->AddMeasurement((tbb::tick_count::now()-__TIME__" + tree.pos + ").seconds()*1e6);");
            nl();
        }

        //spawn tasks that depend on this statement
        state.taskEmitter.SpawnDependentTasks(tree);

        state.lastProjectionArgs = oldLastProjectionArgs;

    }

    /**
     * Print a set of modifiers.
     */
    public void printFlags(long flags) throws IOException {
        if ((flags & SYNTHETIC) != 0) {
            print("/*synthetic*/ ");
        }
        if ((flags & ~(PUBLIC | PRIVATE | PROTECTED | FINAL | NATIVE | UNSIGNED) & StandardFlags) != 0) {
            if (state.header) {
                print(TreeInfo.flagNames(flags & ~(PUBLIC | PRIVATE | PROTECTED | FINAL | NATIVE | UNSIGNED) & (StandardFlags)));
                if ((flags & ~(PUBLIC | PRIVATE | PROTECTED | FINAL | NATIVE | UNSIGNED) & (StandardFlags)) != 0) {
                    print(" ");
                }

            } else {
                print(TreeInfo.flagNames(flags & ~(PUBLIC | PRIVATE | PROTECTED | FINAL | NATIVE | STATIC | UNSIGNED) & (StandardFlags)));
                if ((flags & ~(PUBLIC | PRIVATE | PROTECTED | FINAL | NATIVE | STATIC | UNSIGNED) & (StandardFlags)) != 0) {
                    print(" ");
                }
            }

        }
    }

    public void printAnnotations(List<JCAnnotation> trees) throws IOException {
        for (List<JCAnnotation> l = trees; l.nonEmpty(); l = l.tail) {
            printStat(l.head);
            println();
            align();
        }
    }

    /**
     * Print documentation comment, if it exists
     *
     * @param tree The tree for which a documentation comment should be printed.
     */
    public void printDocComment(JCTree tree) throws IOException {
        if (state.docComments != null) {
            String dc = state.docComments.get(tree);
            if (dc != null) {
                print("/**");
                println();
                int pos = 0;
                int endpos = LowerTree.lineEndPos(dc, pos);
                while (pos < dc.length()) {
                    align();
                    print(" *");
                    if (pos < dc.length() && dc.charAt(pos) > ' ') {
                        print(" ");
                    }
                    print(dc.substring(pos, endpos));
                    println();
                    pos = endpos + 1;
                    endpos = LowerTree.lineEndPos(dc, pos);
                }
                align();
                print(" */");
                println();
                align();
            }
        }
    }
//where

    /**
     * Print a block.
     */
    public void printBlock(List<? extends JCTree> stats) throws IOException {
        print("{");
        println();
        indent();
        printStats(stats);
        undent();
        align();
        print("}");
    }

    /**
     * Print a block.
     */
    public void printEnumBody(List<JCTree> stats) throws IOException {
        state.log.error(stats.head.pos, "not.impl", stats.head);

        print("{");
        println();
        indent();
        boolean first = true;
        for (List<JCTree> l = stats; l.nonEmpty(); l = l.tail) {
            if (isEnumerator(l.head)) {
                if (!first) {
                    print(",");
                    println();
                }
                align();
                printStat(l.head);
                first = false;
            }
        }
        print(";");
        println();
        for (List<JCTree> l = stats; l.nonEmpty(); l = l.tail) {
            if (!isEnumerator(l.head)) {
                align();
                printStat(l.head);
                println();
            }
        }
        undent();
        align();
        print("}");
    }

    /**
     * Is the given tree an enumerator definition?
     */
    boolean isEnumerator(JCTree t) {
        return t.getTag() == JCTree.VARDEF && (((JCVariableDecl) t).mods.flags & ENUM) != 0;
    }

    //is symbol used in def?
    boolean isUsed(final Symbol t, JCTree cdef) {
        class UsedVisitor extends TreeScanner {

            public void scan(JCTree tree) {
                if (tree != null && !result) {
                    tree.accept(this);
                }
            }
            boolean result = false;

            public void visitIdent(JCIdent tree) {
                if (tree.sym == t) {
                    result = true;
                }
            }
        }
        UsedVisitor v = new UsedVisitor();
        v.scan(cdef);
        return v.result;
    }

    /**
     * Print unit consisting of package clause and import statements in
     * toplevel, followed by class definition. if class definition == null,
     * print all definitions in toplevel.
     *
     * @param tree The toplevel tree
     * @param cdef The class definition, which is assumed to be part of the
     * toplevel tree.
     */
    public void printUnit(JCCompilationUnit tree, JCClassDecl cdef) throws IOException {

        state.docComments = tree.docComments;
        printDocComment(tree);

        if (state.outGraph != null && state.header) { //one graph per class
            dgprintln("strict digraph " + cdef.name.toString() + " {\n");
            dgprintln("node [shape=box]");
        }
        /*
         boolean firstImport = true;
         if(false)
         for (List<JCTree> l = tree.defs;
         l.nonEmpty() && (cdef == null || l.head.getTag() == JCTree.IMPORT
         || l.head.getTag() == JCTree.DOMDEF
         || l.head.getTag() == JCTree.CLASSDEF);
         l = l.tail) {
         if (l.head.getTag() == JCTree.IMPORT) {
         JCImport imp = (JCImport) l.head;
         Name name = TreeInfo.name(imp.qualid);
         if (name == name.table.names.asterisk
         || cdef == null
         || isUsed(TreeInfo.symbol(imp.qualid), cdef)) {
         if (firstImport) {
         firstImport = false;
         println();
         }
         printStat(imp);
         }
         } else if(false) {
         if (l.head.getTag() != JCTree.CLASSDEF) {
         printStat(l.head);
         }
         }
         }
         */

        if (cdef != null) {
            printStat(cdef);
            println();
        }

        if (state.outGraph != null && !state.header) {
            dgprintln("\n}");
        }

    }

    /**
     * ************************************************************************
     * Visitor methods
     * ***********************************************************************
     */
    public void visitTopLevel(JCCompilationUnit tree) {
        try {
            printUnit(tree, null);
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitImport(JCImport tree) {
    }

    //obsolete?
    public void visitCTProperty(JCCTProperty tree) {
        try {
            printExpr(tree.exp);
            print("@");
            print(tree.name);
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

	//currently we only generate code for linux, here is what we would need for windows (app entry)
	/*

     #define WIN32_LEAN_AND_MEAN
     #include <windows.h>
     #include <tchar.h>
     #include <iostream>
     #include <signal.h>


     BOOL WINAPI ConsoleHandler(
     DWORD dwCtrlType   //  control signal type
     );

     int main(int argc, char *argv[])
     {
     if (SetConsoleCtrlHandler( (PHANDLER_ROUTINE)ConsoleHandler,TRUE)==FALSE)
     {
     // unable to install handler...
     // display message to the user
     printf("Unable to install handler!\n");
     return -1;
     }


     while(true)
     {

     }
     }

     BOOL WINAPI ConsoleHandler(DWORD CEvent)
     {
     char mesg[128];

     switch(CEvent)
     {
     case CTRL_C_EVENT:
     MessageBox(NULL,
     _T("CTRL+C received!"),_T("CEvent"),MB_OK);
     break;
     case CTRL_BREAK_EVENT:
     MessageBox(NULL,
     _T("CTRL+BREAK received!"),_T("CEvent"),MB_OK);
     break;
     case CTRL_CLOSE_EVENT:
     MessageBox(NULL,
     _T("Program being closed!"),_T("CEvent"),MB_OK);
     break;
     case CTRL_LOGOFF_EVENT:
     MessageBox(NULL,
     _T("User is logging off!"),_T("CEvent"),MB_OK);
     break;
     case CTRL_SHUTDOWN_EVENT:
     MessageBox(NULL,
     _T("User is logging off!"),_T("CEvent"),MB_OK);
     break;

     }
     return TRUE;
     }
     */
    //print linux app entry
    public void printAppEntry(JCClassDecl tree) throws IOException {

        state.debugPos(tree.pos);
        //FIMXE: PLATFORM SPECIFIC
        if (state.debug_print_task) {
            print("void sighandler(int sig){funky::Debug::Abort();exit(-1);}");
            nl();
            nl();
        }

        String cst;
        if ((tree.sym.flags_field & Flags.FIN) != 0)//do you want modifyable args
        {
            cst = "";
            print("CONTEXT(int,StartContext,{int argc; char** argv;},{},{});");
        } else {
            cst = "const ";
            print("CONTEXT(int,StartContext,{int argc; char const* const* argv;},{},{});");
        }
        /*
         * Andreas Wagner:
         * declare MPIMain
         */
        if (state.jc.supportMPI) {
            nl();
            print("void MPIMain(StartContext *sp);");
            nl();
            print("void MPIRegister();");
            nl();
        }
        nl();
        nl();

        //if (state.profile >= 0) { //redo computations for profiling?
            print("GLOBAL_TASK(RUN, StartContext){context()->SetReturn(" + tree.sym.name + "::main_int_String_one_d_01___int(context()->params.argc,(new funky::LinearArray<char*>::Version(new funky::LinearArray<char*>(context()->params.argc)))->map([&](funky::uint32 i){return context()->params.argv[i];},0,context()->params.argc)));}END_GLOBAL_TASK()");
            /*
        } else {
            nl();
            print("tbb::tick_count t0;"); //we time all runs
            nl();
            print("tbb::tick_count t1;"); //we time all runs
            nl();
            print("double tsum=0.f;"); //we time all runs
            nl();
            print("double tsq=0.f;"); //we time all runs
            nl();

            if (state.jc.supportMPI) {
                print("GLOBAL_TASK(RUN, StartContext){t0 = tbb::tick_count::now();"
                        + "context()->SetReturn(0);"
                        + "for(funky::uint32 i=0;i<" + Math.abs(state.profile) + ";i++){"
                        + "MPIMain(context());MPI::COMM_WORLD.Barrier();"
                        + "t1 = tbb::tick_count::now();"
                        + "double val=(t1 - t0).seconds();tsum+=val;tsq+=val*val;"
                        + "}t1 = tbb::tick_count::now();"
                        + "}END_GLOBAL_TASK()");

            } else {
                print("GLOBAL_TASK(RUN, StartContext){t0 = tbb::tick_count::now();"
                        + "context()->SetReturn(0);"
                        + "for(funky::uint32 i=0;i<" + Math.abs(state.profile) + ";i++){"
                        + "printf(\"<<PROFILING RUN %d of %d>>\\n\",i+1,"+Math.abs(state.profile)+");"
                        + tree.sym.name + "::main_int_String_one_d_01___int(0,NULL);"
                        + "t1 = tbb::tick_count::now();"
                        + "double val=(t1 - t0).seconds();tsum+=val;tsq+=val*val;"
                        + "}t1 = tbb::tick_count::now();"
                        + "}END_GLOBAL_TASK()");
            }
        }
    */

        nl();
        nl();
        print("GLOBAL_TASK(START, StartContext){set_ref_count(2);spawn_and_wait_for_all(*new( allocate_child()) RUN(context()));}END_GLOBAL_TASK()");
        nl();
        nl();

        //pseudo tasks to run before actual state.work in order to make it more likely that tbb threads are running
        print("GLOBAL_TASK(WORKER, StartContext){getWorkerThreadID();usleep(100000);}END_GLOBAL_TASK()");
        nl();
        nl();

//careful, with workers enabled FileIO regtest does not terminate
        int cores = Math.min(10, Math.max(1, ((int) state.target.coreCount)));
        print("GLOBAL_TASK(START_WORKERS, StartContext){set_ref_count(" + (cores + 1) + ");for(funky::uint32 i=0;i<" + (cores) + ";i++)spawn(*new (allocate_child()) WORKER(context()));wait_for_all();}END_GLOBAL_TASK()");
        nl();
        nl();

        print("int main(int argc, char* argv[])");
        nl();
        print("{");
        indent();

        nl();
        print("GC_INIT();");
        nl();
        print("tbb::task_scheduler_init();");

        //FIMXE: PLATFORM SPECIFIC
        if (state.debug_print_task) { //install sighandler for dbg
            nl();
            print("signal(SIGABRT, &sighandler);");
            print("signal(SIGTERM, &sighandler);");
            print("signal(SIGINT, &sighandler);");
            nl();
        }

        nl();
        print("StartContext sp;");
        nl();
        print("sp.params.argc = argc;");
        nl();
        print("sp.params.argv = argv;");
        nl();
        nl();

        /*
         * Andreas Wagner:
         * Boilerplate for MPI
         */
        if (state.jc.supportMPI) {
            print("MPI::Init(argc, argv);");
            nl();
            print("MPIRegister();");
            nl();
        }

        nl();

            //enable this to warm up worker threads:
        //CAREFUL: if you enable the following then FileIO regtest will not ternminate!
        print("tbb::task::spawn_root_and_wait(*new (tbb::task::allocate_root()) START_WORKERS(&sp));//encourage tbb to start worker threads before we start measuring");

        //print("funky::TaskRoot<>::GetRoot() = new(tbb::task::allocate_additional_child_of(*funky::TaskRoot<>::GetRoot())) START(&sp);");
        if (state.jc.supportOpenCL) {
            nl();
            print("device = ocl::getDevice(0,2);");
            nl();
            nl();
        }

        if(state.profile==0)
            state.profile=-1;

        if (state.profile != 0) { //redo computations for profiling?
            nl();
            print("double tsum=0.f;"); //we time all runs
            nl();
            print("double tsq=0.f;"); //we time all runs
            nl();
            print("tbb::tick_count t0;"); //we time all runs
            nl();
            print("for(funky::uint32 i=0;i<" + Math.abs(state.profile) + ";i++){");
            nl();
            print("printf(\"<<PROFILING RUN %d of %d>>\\n\",i+1,"+Math.abs(state.profile)+");");         
            nl();
            print("t0 = tbb::tick_count::now();");
        }

        //start main task
        nl();
        print("funky::TaskRoot<>::GetRoot() = new(tbb::task::allocate_root()) START(&sp);");

        if (state.debug_print_task) {
            nl();
            print("printf(\"Root(0x%016p)\\n\",funky::TaskRoot<>::GetRoot());");
        }

        nl();
        print("tbb::task::spawn_root_and_wait(*funky::TaskRoot<>::GetRoot());");

        if (state.profile != 0) {
            nl();
            print("double val=(tbb::tick_count::now() - t0).seconds();tsum+=val;tsq+=val*val;");
            nl();
            print("}");
        }
        //print stats
        if (state.profile != 0) {
            nl();
            print("double d=tsum/"+Math.abs(state.profile)+";");
            nl();
            print("printf(\"\\nretval: %d\\ntime: %f pm %f [s] %f pm %f [clocks]\\n\",sp.GetReturn(), d,sqrt(1.0/(" + Math.abs(state.profile) + "-1)*((double)tsq-tsum*(tsum/(double)" + Math.abs(state.profile) + ")))/sqrt(" + Math.abs(state.profile) + "),d*" + Math.min(1.0e10, (state.target.coreSpeed * 1.0e9)) + ",sqrt(1.0/(" + Math.abs(state.profile) + "-1)*((double)tsq-tsum*(tsum/(double)" + Math.abs(state.profile) + ".f)))*" + Math.min(1.0e10, (state.target.coreSpeed * 1.0e9)) + "/sqrt(" + Math.abs(state.profile) + ".f));");
        }

        if (state.profile > 0) {
            nl();
            print("funky::Profile::DumpProfileStats(\"profile.properties\"," + Math.abs(state.profile) + "," + (state.target.coreSpeed * 1e3) + ");");//clocks per us
        }
        
        nl();
        print("funky::Profile::DumpCustomProfileStats(1,1.0);");//clocks per us

        if (state.debug_print_task) {
            print("funky::Debug::Exit();");
        }

        /*
         * Andreas Wagner:
         * Boilerplate for MPI
         */
        if (state.jc.supportMPI) {
            nl();
            nl();
            print("int comsize = MPI::COMM_WORLD.Get_size();");
            nl();
            print("int myrank = MPI::COMM_WORLD.Get_rank();");
            nl();
            print("if(myrank == 1){");
            indent();
            nl();
            print("//shutdown scheduler");
            nl();
            print("int taskId = 0;");
            nl();
            print("MPI::COMM_WORLD.Send(&taskId, 1, MPI::INT, SCHEDULER, SHUTDOWN);");
            undent();
            nl();
            print("}");
        }

        if (forcegc) {
            print("int retval = sp.GetReturn();");
            nl();
            print("fprintf(stdout,\"Start GC Stats\\n\");fflush(stdout);");
            nl();
            print("GC_gcollect();");
            nl();
            print("GC_gcollect();");
            nl();
            print("GC_dump();");
            nl();
            if (state.jc.supportMPI) {
                print("MPI::Finalize();");
                nl();
            }
            print("return retval;");
            nl();
        } else {
            /* Andreas Wagner:
             * NOTE: MPI must not return anything else but 0
             */
            if (state.jc.supportMPI) {
                nl();
                print("MPI::Finalize();");
                nl();
                print("return 0;");
                nl();
            } else {
                print("return sp.GetReturn();");
                nl();
            }
        }
        undent();
        nl();
        print("}");

        /*
         * Andreas Wagner: Startup-Code for MPI
         */
        if (state.jc.supportMPI) {
            nl();
            nl();
            print("void MPIRegister(){");
            indent();
            nl();
            print("bool scheduler_ready = false;");
            nl();
            print("int myrank = MPI::COMM_WORLD.Get_rank();");
            nl();
            print("if(myrank != SCHEDULER)");
            nl();
            print("{");
            indent();
            nl();
            print("MPI::COMM_WORLD.Bcast(&scheduler_ready, 1, MPI::BOOL, SCHEDULER);");
            nl();
            print("if(scheduler_ready == true)");
            nl();
            print("{");
            indent();
            nl();
            print("MPI::COMM_WORLD.Send(&myrank, 1, MPI::INT, SCHEDULER, REGISTER_TAG);");
            undent();
            nl();
            print("} else");
            nl();
            print("{");
            indent();
            nl();
            print("std::cout<<\"scheduler not available. exiting...\"<<std::endl;");
            undent();
            nl();
            print("}");
            undent();
            nl();
            print("}");
            undent();
            nl();
            print("}");
            nl();
            nl();
        }

        if (state.jc.supportMPI) {
            nl();
            nl();
            print("void MPIMain(StartContext *sp)");
            nl();
            print("{");
            indent();
            nl();
            print("int myrank = MPI::COMM_WORLD.Get_rank();");
            nl();
            print("int comsize = MPI::COMM_WORLD.Get_size();");
            nl();
            print("if(myrank == 1)");
            nl();
            print("{");
            indent();
            nl();
            print("sp->SetReturn(" + tree.sym.name + "::main_int_String_one_d_01___int(0, NULL));");
            nl();
            print("int taskId = EXIT;");
            nl();
            print("for(int i = 2; i < comsize; i++) MPI::COMM_WORLD.Send(&taskId, 1, MPI::INT, i, EXIT);");
            nl();
            print("MPI::COMM_WORLD.Send(&taskId, 1, MPI::INT, SCHEDULER, EXIT);");
            nl();
            undent();
            nl();
            print("} else");
            nl();
            print("{");
            indent();
            nl();
            print("//process is worker");
            nl();
            print("bool serving = true;");
            nl();
            print("MPI::Status state;");
            nl();
            print("char* buffer = new char[1024];");
            nl();
            nl();
            for (String taskId : state.jc.taskNamesWithNamespace) {
                print(taskId + "* _" + state.jc.tasksToTasknameNamespaces.get(taskId) + " = ");
                print("new " + taskId + "();");
                nl();
                nl();
            }
            nl();
            print("while(serving)");
            nl();
            print("{");
            indent();
            nl();
            print("MPI::COMM_WORLD.Recv(buffer, 1024, MPI::CHAR, MPI_ANY_SOURCE, MPI_ANY_TAG, state);");
            nl();
            nl();
            print("switch(state.Get_tag())");
            nl();
            print("{");
            indent();
            nl();

            //taskIds
            for (String taskId : state.jc.taskNames) {
                print("case " + taskId.toUpperCase() + ":");
                indent();
                nl();
                print("_" + taskId + "->execute(buffer, state.Get_source());");
                nl();
                print("break;");
                undent();
                nl();
            }
            //exit case
            print("case EXIT:");
            indent();
            nl();
            print("serving = false;");
            nl();
            print("break;");
            undent();
            nl();
            //default case
            print("default:");
            indent();
            nl();
            print("std::cout<<\"Error - Task not known...\"<<std::endl;");
            nl();
            print("break;");
            undent();
            nl();
            undent();
            nl();
            print("}");
            undent();
            nl();
            print("}");
            //cleanup memory
            for (String taskId : state.jc.taskNamesWithNamespace) {
                print("delete _" + state.jc.tasksToTasknameNamespaces.get(taskId) + ";");
                nl();
            }
            print("delete buffer;");
            undent();
            nl();
            print("}");
            undent();
            nl();
            print("}");
        }
    }

    public void visitClassDef(JCClassDecl tree) {
        try {
            state.source = (new DiagnosticSource(tree.sym.sourcefile, null));

            if (state.header) {
                print("#ifndef __" + tree.sym.name.toString() + "__INCLUDE_GUARD");
                nl();
                print("#define __" + tree.sym.name.toString() + "__INCLUDE_GUARD");
                nl();
                if (state.jc.supportOpenCL) {
                    print("#define OPENCLBACKEND");
                    nl();
                }

                print("#include <Task.h>");
                nl();
                /*
                 * andreas wagner: include mpi.h
                 */
                if (state.jc.supportMPI) {
                    print("#include <mpi.h>");
                    nl();
                                    //the following file should be part of the runtime!!!
                    //it defines several tags for mpi communication and framework functions
                    print("#include \"../common/funkympi.h\"");
                    nl();
                }

                /*
                 * //brakes tests..should this really go into the header?
                 * //Question is: where else. It should be where the main method is.
                 * //In other cases it should be extern.
                 * //I am making it openCL optional for the moment. Is there a way to find out the class of the main method?
                 */
                if (state.jc.supportOpenCL) {
                    nl();
                    print("ocl_device device;");
                    nl();
                }

                for (Symbol s : tree.sym.referenced_classes) {
                    if (!s.name.equals(tree.sym.name)) {
                        if ((s.flags_field & Flags.NATIVE) == 0 || (s.flags_field & Flags.INTERFACE) == 0) {
                            if (!s.name.toString().equals("String") && !s.name.toString().equals("void") && !s.name.toString().equals("Array") && !s.name.toString().equals("Method") && !s.name.toString().equals("Object") && !s.name.toString().equals("global")) {
                                if (!s.type.isPrimitive()) {
                                    nl();
                                    if ((s.flags_field & Flags.NATIVE) != 0) {
                                        print("#include <" + s.name + ".h>");
                                    } else {
                                        if (tree.sym.flatname.toString().contains(".")) {
                                            print("#include <" + ((ClassSymbol) s).flatname.toString().replace('.', '/') + ".h>");
                                        } else {
                                            print("#include \"" + ((ClassSymbol) s).flatname.toString().replace('.', '/') + ".h\"");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                print("#include \"" + tree.sym.name + ".h\"");
                nl();
            }

            state.current_class = tree.name.toString();

            if (state.header && !tree.sym.taskGroups.isEmpty()) {
                nl();
                print("struct TaskGroupInitializer_" + tree.name + "{");
                nl();
                for (String s : tree.sym.taskGroups) {
                    print("static int get" + s + "(){");
                    nl();
                    print("static int id=funky::Group::registerGroup(\"" + s + "\");");
                    nl();
                    print("return id;");
                    nl();
                    print("}");
                    nl();
                }

                print("};");
                nl();
            }

            if (!state.header && !tree.sym.taskThreads.isEmpty()) {
                nl();
                print("struct TaskThreadInitializer_" + tree.name + "{");
                nl();
                for (String s : tree.sym.taskThreads) {
                    print("static funky::thread_handle* getThread" + s + "(){");
                    nl();
                    print("static funky::thread_handle* id=funky::Thread::registerThread(\"" + s + "\");");
                    nl();
                    print("return id;");
                    nl();
                    print("}");
                    nl();
                }

                print("};");
                nl();
            }

			//nl();
            Name enclClassNamePrev = state.enclClassName;
            state.enclClassName = tree.name;
            JCClassDecl prevEnclClass = state.enclClass;
            state.enclClass = tree;

            if (state.header) {

                println();
                align();

                printDocComment(tree);

                state.typeEmitter.printCPPTypeParams(tree.typarams, null);

                printAnnotations(tree.mods.annotations);

                String packing = "";

                if ((tree.sym.flags_field & Flags.PACKED) != 0) {
                    packing = "__attribute__ ((__packed__)) ";
                }

                boolean first_base = true;
                if ((tree.mods.flags & INTERFACE) != 0) {
                    print("struct " + packing + tree.name);
                    if (tree.implementing.nonEmpty()) {
                        print(" : ");
                        first_base = false;
                        print(tree.implementing.toString());
                    }
                } else {
                    if ((tree.mods.flags & ENUM) != 0) {
                        print("enum " + tree.name);
                    } else {
                        print("struct " + packing + tree.name);
                    }

                    if (tree.extending != null && tree.extending.type.tag == TypeTags.CLASS) {
                        if (first_base) {
                            first_base = false;
                            print(" : ");
                        } else {
                            print(", ");
                        }
                        print(tree.extending.type.toString("unknown"));
                    }

                    if (tree.implementing != null && tree.implementing.nonEmpty()) {
                        if (first_base) {
                            first_base = false;
                            print(" : ");
                        } else {
                            print(", ");
                        }

                        printExprs(tree.implementing);
                    }

                    if (tree.singular && (tree.sym.flags_field & Flags.ATOMIC) == 0) {
                        if (first_base) {
                            first_base = false;
                            print(" : ");
                        } else {
                            print(", ");
                        }

                        print("virtual funky::Singular");
                    }

                    //make user defined classes gc-able
                    if ((tree.sym.flags_field & Flags.PACKED) == 0) {
                        if (tree.extending == null || tree.extending.type.tag != TypeTags.ARRAY) {
                            if (first_base) {
                                first_base = false;
                                print(" : ");
                            } else {
                                print(", ");
                            }

                            print("virtual boehmgc::gc");
                        }
                    }
                }

            }
			//println();
            //align();

            if ((tree.mods.flags & ENUM) != 0) {
                printEnumBody(tree.defs);
            } else {

                if (state.header) {
                    nl();
                    print("{");
                    indent();
                    nl();

                    if (tree.staticTrigger != null) {
                        for (JCMethodInvocation mi : tree.staticTrigger) {
                            nl();
                            print("static int TRIGGER_UID_" + mi.pos + ";");
                        }
                    }

                    if (tree.nonStaticTrigger != null) {
                        for (JCMethodInvocation mi : tree.nonStaticTrigger) {
                            nl();
                            print("int TRIGGER_UID_" + mi.pos + ";");
                        }
                    }

                    for (JCMethodDecl md : tree.sym.ffContext) {
                        printCallContext(md, false);
                        printCallTask(md, false);
                    }

                    printStats(tree.defs);

                    undent();
                    nl();
                    print("};");

                    /**
                     * Andreas Wagner this creates IDs for tasks which will be
                     * used as tags
                     */
                    if (state.jc.supportMPI) {
                        for (String s : state.jc.taskNames) {
                            nl();
                            print("#define " + s.toUpperCase() + " " + 301 + state.jc.taskCounter++);
                            nl();

                        }
                        nl();
                        print("#define EXIT_OK ");
                        print(state.jc.taskCounter + 303);
                        nl();
                        print("#define NOTIFY_ID_TAG ");
                        print(state.jc.taskCounter + 304);
                        nl();
                        if (state.jc.optimizeRecursion) {
                            print("#define NOTIFY_REC ");
                            print(state.jc.taskCounter + 305);
                            nl();
                            print("#define NOTIFY_ID ");
                            print(state.jc.taskCounter + 306);
                            nl();
                            print("bool global_iterate;");
                            nl();
                        }
                    }

                } else {
                    if (tree.staticTrigger != null) {
                        for (JCMethodInvocation mi : tree.staticTrigger) {
                            nl();
                            print("int " + tree.name + "::TRIGGER_UID_" + mi.pos + "=0;");
                        }
                    }

                    for (JCMethodDecl md : tree.sym.ffContext) {
                        printCallTask(md, false);
                    }

                    printStats(tree.defs);
                }

                if (!state.header && (tree.sym.flags_field & Flags.FOUT) != 0) {
                    printAppEntry(tree);
                }
            }

            if (state.header) {
                nl();
                print("#endif //__" + tree.sym.name.toString() + "__INCLUDE_GUARD");
                nl();

            }

            state.enclClassName = enclClassNamePrev;
            state.enclClass = prevEnclClass;
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        } finally {
            state.source = null;
        }
    }

    public void visitMethodHeader(JCMethodDecl tree) {

        try {

            // when producing state.source output, omit anonymous constructors
            if (tree.name == tree.name.table.names.init
                    && state.enclClassName == null
                    && state.sourceOutput) {
                return;
            }
            printExpr(tree.mods);
            //state.typeEmitter.printTypeParameters(tree.typarams);

            Symbol s = tree.sym.enclClass();

            state.printCPPTypeParams(tree.typarams, ((Type.ClassType) s.type).typarams_field);

            if (tree.name == tree.name.table.names.init) {
                print(state.enclClassName != null ? state.enclClassName : tree.name);
            } else {
                if (tree.restype != null) {
                    printExpr(tree.restype);
                    if (!tree.restype.type.isPrimitive() && tree.restype.type.tag != TypeTags.VOID) {
                        print("*");
                    }
                } else {
                    print("event");
                }

                print(" " + tree.name);
            }
            print("(");
            printExprs(tree.params);
            print(")");
            if (tree.thrown.nonEmpty()) {
                print(" throws ");
                printExprs(tree.thrown);
            }

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
                    if (state.com.contains(vs)) { //is it shared??
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

//NO TASK GRAPH:
    public void genMethodHeader(JCMethodDecl tree) throws IOException {

        if ((tree.sym.flags_field & Flags.LOOP) != 0 && state.loop_label_sched == null) {
            state.loop_label_sched = state.current_scheduler;
            nl();
            print("LOOP_LABEL");
            if ((state.kernel || state.inside_kernel) && !state.method_has_context) {
                state.loop_label_kernel = true;
                print("_KERNEL");
            }
            print("" + state.current_scheduler.pos);
            print(":");
            nl();
        }

        state.method_has_context = false;
        if (state.method.hasResume) {
            if (state.method.getReturnType() != null) {
                if (state.method.getReturnType().type.tag != TypeTags.VOID) {
                    Type t = state.method.getReturnType().type;
                    String rettype = t.toString();
                    if (!t.isPrimitive()) {
                        rettype += "*";
                    }
                    nl();
                    print(rettype + " RESUME_STORE;");
                }

            }
            nl();
            print("bool RESUME_STORE_SET=false;");
        }
        if (tree.name != tree.name.table.names.init) {
            if (state.method.restype != null && (state.method.sym.owner.flags() & Flags.SINGULAR) != 0)//if not protected!
            {
                state.singularEmitter.enterSample();
            }
        }
    }

    public void genMethodBody(JCMethodDecl tree) throws IOException {
        printStats(tree.body.stats);
    }

    public void genMethodFooter(JCMethodDecl tree) throws IOException {

        if (state.method.hasResume && state.inside_task == null) {
            nl();
            if (state.method.getReturnType() != null && state.method.getReturnType().type.tag != TypeTags.VOID) {
                print("if(RESUME_STORE_SET)return RESUME_STORE;");
            } else {
                print("if(RESUME_STORE_SET)return;");
            }
        }

        if (state.method.final_value != null)//handle finally
        {
            nl();
            state.allow_final = true;
            printExpr(state.method.final_value);

            state.DumpFinal(state.method.final_value, state.kernel);

            state.allow_final = false;
        }
        //nl();
        state.loop_label_sched = null;
        state.loop_label_kernel = false;

    }

	//context to call a state.method asynchronously (BLOCKING methods)
    //stores variables necessary to perform state.method call in it's own task (in the I/O pool)
    public void printCallContext(JCMethodDecl tree, boolean is_trigger) throws IOException {
        print("struct CALL_CONTEXT" + tree.name + tree.pos + " : public funky::ContextBase< " + state.typeEmitter.getResType(tree.restype) + " >");
        nl();
        print("{");
        indent();
        nl();

        for (JCVariableDecl d : tree.params) {
            VarSymbol vs = d.sym;
            state.typeEmitter.printType(vs);
            if (!(vs.type instanceof Type.MethodType)) {
                print(" " + vs.name.toString().replace('\'', '_'));
            }
            print(";");
        }

        if ((tree.mods.flags & Flags.STATIC) == 0) {
            state.typeEmitter.printType(((ClassSymbol) tree.sym.owner).thisSym);
            print(" OO;");
            nl();
        }

        if (is_trigger) {
            print("int* TRIGGER__UID;");
            nl();
        }

        print("CALL_CONTEXT" + tree.name + tree.pos + "(");

        boolean first = true;
        for (JCVariableDecl d : tree.params) {
            first = false;
            if (d != tree.params.head) {
                print(",");
            }

            VarSymbol vs = d.sym;
            state.typeEmitter.printType(vs);
            if (!(vs.type instanceof Type.MethodType)) {
                print(" " + vs.name.toString().replace('\'', '_'));
            }
        }

        if ((tree.mods.flags & Flags.STATIC) == 0) {
            first = false;
            if (!tree.params.isEmpty()) {
                print(", ");
            }
            state.typeEmitter.printType(((ClassSymbol) tree.sym.owner).thisSym);
            print(" OO");
        }

        if (is_trigger) {
            if (!first) {
                print(", ");
            }
            first = false;
            print("int* TRIGGER__UID");
        }

        if (state.method != null && state.method.sym.mayBeRecursive) {
            if (!first) {
                print(", ");
            }
            print("funky::uint32 KERNEL_LEVEL=0");
            print("): funky::ContextBase< " + state.typeEmitter.getResType(tree.restype) + " >(KERNEL_LEVEL, false)");
        } else {
            print("): funky::ContextBase< " + state.typeEmitter.getResType(tree.restype) + " >(0, true)");
        }

        first = true;

        for (JCVariableDecl d : tree.params) {
            print(", ");
            first = false;
            print(d.name + "(" + d.name + ")");
        }

        if ((tree.mods.flags & Flags.STATIC) == 0) {
            print(", ");
            first = false;
            print("OO(OO)");
        }

        if (is_trigger) {
            if (!first) {
                print(", ");
            }
            first = false;
            print("TRIGGER__UID(TRIGGER__UID)");
        }

        print("{}");
        undent();
        nl();
        print("};");
        nl();
        nl();

    }

    //generate task that calls a blocking method
    public void printCallTask(JCMethodDecl tree, boolean is_trigger) throws IOException {
        if (state.header) {
            print("DECLARE_CONST_TASK(CALL_TASK" + tree.name + tree.pos + "," + "CALL_CONTEXT" + tree.name + tree.pos + ");");
            nl();
            nl();
            return;
        }

        /*
         * Andreas Wagner: For now untouched
         * Case distinction just in case something is needed!
         */
        if (state.jc.supportMPI) {
            print("BEGIN_TASK(" + state.enclClassName + "," + "CALL_TASK" + tree.name + tree.pos + "," + "CALL_CONTEXT" + tree.name + tree.pos + ")");
            nl();
            print("{");
            indent();
            nl();

            if (tree.getReturnType().type.tag != TypeTags.VOID) {
                Type t = tree.getReturnType().type;
                print("context()->SetReturn(");
            }

            if ((tree.mods.flags & Flags.STATIC) == 0) {
                print("context()->OO->");
            }

            visitMethodSymbol(tree.sym);
            print("(");

            boolean first = true;

            for (JCVariableDecl d : tree.params) {
                first = false;
                if (d != tree.params.head) {
                    print(", ");
                }
                print("context()->" + d.name);
            }

            if (is_trigger) {
                if (!first) {
                    print(", ");
                }
                first = false;
                print("context()->TRIGGER__UID");
            }

            if (tree.getReturnType().type.tag != TypeTags.VOID) {
                print(")");
            }
            print(");");

            undent();
            nl();
            print("}");
            nl();
            print("END_TASK()");
        } else {
            print("BEGIN_TASK(" + state.enclClassName + "," + "CALL_TASK" + tree.name + tree.pos + "," + "CALL_CONTEXT" + tree.name + tree.pos + ")");
            nl();
            print("{");
            indent();
            nl();

            if (tree.getReturnType().type.tag != TypeTags.VOID) {
                Type t = tree.getReturnType().type;
                print("context()->SetReturn(");
            }

            if ((tree.mods.flags & Flags.STATIC) == 0) {
                print("context()->OO->");
            }

            visitMethodSymbol(tree.sym);
            print("(");

            boolean first = true;

            for (JCVariableDecl d : tree.params) {
                first = false;
                if (d != tree.params.head) {
                    print(", ");
                }
                print("context()->" + d.name);
            }

            if (is_trigger) {
                if (!first) {
                    print(", ");
                }
                first = false;
                print("context()->TRIGGER__UID");
            }

            if (tree.getReturnType().type.tag != TypeTags.VOID) {
                print(")");
            }
            print(");");

            undent();
            nl();
            print("}");
            nl();
            print("END_TASK()");
        }
        nl();
        nl();
    }

    //emit var decls for implicitly declared vars (a'b)
    public void genImplicitDecls(Set<iTask> paths) throws IOException {

        if (paths.size() > 0) {
            Set<JCTree> calcNodes = new LinkedHashSet<JCTree>();
            for (iTask p : paths) {
                calcNodes.addAll(p.getCalcNodesTransitive());
            }
            Set<VarSymbol> inner = paths.iterator().next().getInnerSymbols(calcNodes, state.method);

            state.inner_symbols = inner;

            for (VarSymbol vs : inner) {
                if (vs.isLocal() && (vs.flags() & Flags.IMPLICITDECL) != 0 && vs.isVariable() && (vs.tasklocal() || state.kernel)) {
                    nl();
                    state.typeEmitter.printType(vs);
                    print(" ");
                    if (!(vs.type instanceof Type.MethodType)) {
                        visitVarSymbol(vs, true);
                    }
                    print(";");

                }
            }
            /*
             Set<VarSymbol> used=new LinkedHashSet<VarSymbol>();

             for(JCTree t:calcNodes)
             used.addAll(JCTree.usedVars(t));
             */
            for (VarSymbol vs : state.method.match.keySet()) {
//				Set<Pair<VarSymbol,Pair<Integer,JCExpression>>> deps=state.method.constraintsSyms.get(vs);
                Pair<VarSymbol, Pair<Integer, JCExpression>> from = state.method.match.get(vs);
                if (from != null) {
                    nl();
                    //Pair<VarSymbol,Pair<Integer,JCExpression>> from=deps.iterator().next();
                    print("funky::uint32 ");
                    visitVarSymbol(vs, true);
                    print("=");
                    state.arrayEmitter.printSize((Type.ArrayType) from.fst.type.getArrayType(), state.jc.make.Ident(from.fst), state.jc.make.Literal(from.snd.fst));

                    print(";");

                }
            }
        }
    }

    public void handleStaticVars(JCMethodDecl tree) throws IOException {
        ClassSymbol cs = state.method.sym.enclClass();
        //init static vars (static vars must be declared and initialized outside the constructor)
        if (!state.header && tree.name == tree.name.table.names.init && cs.init_constructors != null) {
            for (JCVariableDecl a : cs.init_constructors) {
                if ((a.mods.flags & Flags.STATIC) != 0) {
                    state.typeEmitter.printType(a.type, a.name.toString());
//FIXME: what if fun ptr
                    print(" ");
                    print(cs.name + "::");
                    print(a.name);
                    print("=");
                    printExpr(fixPointer(a.sym, a.init));
                    print(";");
                    nl();
                }
            }

        }
    }

    //state.method state.header
    public void handleHeader(JCMethodDecl tree, boolean inline) throws IOException {
        nl();
        printDocComment(tree);

        if (state.header && inline) {
            print("inline ");
        }

        //emit template params
        Symbol s = tree.sym.enclClass();

        state.printCPPTypeParams(tree.typarams, ((Type.ClassType) s.type).typarams_field);

        printExpr(tree.mods);

        //state.method has no body
        if (tree.body == null && (tree.mods.flags & Flags.FINAL) == 0) {
            if (state.header) {
                if ((tree.mods.flags & Flags.NATIVE) == 0) {
                    print("virtual ");
                } else {
                    print("extern ");
                }
            }
        }

		//(qualified) state.method name
        if (tree.name == tree.name.table.names.init) {//constructor
            if (!state.header) {
                print(state.method.sym.enclClass().name);
                state.typeEmitter.printCPPTemplateParams(((Type.ClassType) tree.sym.enclClass().type).typarams_field);
                print("::");
            }

            print(state.enclClassName != null ? state.enclClassName : tree.name);
        } else if (tree.name.toString().equals("finalize")) { //destructor (FIXME: won't be called unless class is gc_cleanup)
            if (!state.header) {
                print(state.method.sym.enclClass().name);
                state.typeEmitter.printCPPTemplateParams(((Type.ClassType) tree.sym.enclClass().type).typarams_field);
                print("::");
            }
            print("~" + state.method.sym.enclClass().name);
        } else {//std state.method
            if (!state.is_event) {

                print(state.typeEmitter.getTypeRestricted(tree.restype.type, "unkwnown"));

            } else {
                print("void");
            }
            if (!state.header) {
                print(" " + state.method.sym.enclClass().name + "::");
            } else {
                print(" ");
            }

            print(JCTree.fixName(tree.name.toString()) + JCTree.fixName(tree.sym.type.toString()));
            //print(tree.name);
            if (state.generate_into) {
                print("__FORWARD__");
            }
        }

        //args
        print("(");

        printExprs(tree.params);
        if (state.generate_into) {
            if (!tree.params.isEmpty()) {
                print(", ");
            }

            Type.ArrayType at = ((Type.ArrayType) tree.restype.type.getArrayType());
			//Type.DomainType dt = (Type.DomainType) at.dom.parentDomain.clone();
            //dt.appliedParams = at.dom.appliedParams;

            print(state.typeEmitter.getTypeRestricted(at, "unkwnown") + " __FORWARD__");
        }
        if ((state.kernel && state.spawns > 0) //||(tree.sym.mayBeRecursive&&(tree.sym.flags_field&Flags.BLOCKING)==0)
                ) {
            if (!tree.params.isEmpty() || state.generate_into) {
                print(", ");
            }
            print("funky::uint32 __KERNEL_LEVEL");
            if (state.header) {
                print("=0");
            }
            /**
             * Andreas Wagner: SPAWNING_LEVEL is used for MPI
             */
            if (state.jc.supportMPI) {
                print(", ");
                print("funky::uint32 SPAWNING_LEVEL");
                if (state.header) {
                    print("=0");
                }
            }
        }
        print(")");
    }

    /*
     * Andreas Wagner:
     * dedicated header generation for method_MPI (recursive reuse)
     */
    public void handleHeaderMPIRec(JCMethodDecl tree, boolean inline) throws IOException {
        nl();
        printDocComment(tree);

        if (state.header && inline) {
            print("inline ");
        }

        //emit template params
        Symbol s = tree.sym.enclClass();

        state.printCPPTypeParams(tree.typarams, ((Type.ClassType) s.type).typarams_field);

        printExpr(tree.mods);

        //state.method has no body
        if (tree.body == null && (tree.mods.flags & Flags.FINAL) == 0) {
            if (state.header) {
                if ((tree.mods.flags & Flags.NATIVE) == 0) {
                    print("virtual ");
                } else {
                    print("extern ");
                }
            }
        }

		//(qualified) state.method name
        if (tree.name == tree.name.table.names.init) {//constructor
            if (!state.header) {
                print(state.method.sym.enclClass().name);
                state.typeEmitter.printCPPTemplateParams(((Type.ClassType) tree.sym.enclClass().type).typarams_field);
                print("::");
            }

            print(state.enclClassName != null ? state.enclClassName : tree.name);
        } else if (tree.name.toString().equals("finalize")) { //destructor (FIXME: won't be called unless class is gc_cleanup)
            if (!state.header) {
                print(state.method.sym.enclClass().name);
                state.typeEmitter.printCPPTemplateParams(((Type.ClassType) tree.sym.enclClass().type).typarams_field);
                print("::");
            }
            print("~" + state.method.sym.enclClass().name);
        } else {//std state.method
            if (!state.is_event) {

                print(state.typeEmitter.getTypeRestricted(tree.restype.type, "unkwnown"));

            } else {
                print("void");
            }
            if (!state.header) {
                print(" " + state.method.sym.enclClass().name + "::");
            } else {
                print(" ");
            }

            print(JCTree.fixName(tree.name.toString()) + JCTree.fixName(tree.sym.type.toString()));
            //print(tree.name);
            if (state.generate_into) {
                print("__FORWARD__");
            }
        }
        print("_MPI");
        //args
        print("(");

        printExprs(tree.params);
        if (state.generate_into) {
            if (!tree.params.isEmpty()) {
                print(", ");
            }

            Type.ArrayType at = ((Type.ArrayType) tree.restype.type.getArrayType());
			//Type.DomainType dt = (Type.DomainType) at.dom.parentDomain.clone();
            //dt.appliedParams = at.dom.appliedParams;

            print(state.typeEmitter.getTypeRestricted(at, "unkwnown") + " __FORWARD__");
        }
        if ((state.kernel && state.spawns > 0) //||(tree.sym.mayBeRecursive&&(tree.sym.flags_field&Flags.BLOCKING)==0)
                ) {
            if (!tree.params.isEmpty() || state.generate_into) {
                print(", ");
            }
            print("funky::uint32 __KERNEL_LEVEL");
            if (state.header) {
                print("=0");
            }
            /**
             * Andreas Wagner: SPAWNING_LEVEL is used for MPI
             */
            print(", ");
            print("funky::uint32 SPAWNING_LEVEL");
            if (state.header) {
                print("=0");
            }

            //Variables which can be reused
            for (iTask task : tree.reuseableVars.keySet()) {
                if (state.task_map.get(task) != null && state.method.reuseableVars.get(task).size() > 0) {
                    print(", int");
                    print(" " + state.task_map.get(task));
                    print("_MPI_ID");
                    if (state.header) {
                        print("=0");
                    }
                }
            }

        }
        print(")");
    }

    //state.method entry
    public void handleEntry(JCMethodDecl tree) throws IOException {
        boolean first = true;
        if (tree.super_call != null) { //super class constructor
            print(" : ");
            printExpr(tree.super_call);
            first = false;
        }

        nl();
        print("{");

        indent();
        println();

        if (tree.name == state.names.init) {
            if (state.enclClass.nonStaticTrigger != null) {
                for (JCMethodInvocation mi : state.enclClass.nonStaticTrigger) {
                    nl();
                    print("int TRIGGER_UID_" + mi.pos + "=0;");
                }
            }

        }

        state.inside_method = true;

        if (state.method.init_constructors != null) { //initializers
            for (JCAssign a : state.method.init_constructors) {
                //if((TreeInfo.symbol(a.lhs).flags_field&Flags.ATOMIC)!=0)
                {
                    nl();
                    printExpr(a.lhs);
                    print("=");
                    printExpr(fixPointer(a.lhs, a.rhs));
                    print(";");
                }
            }
        }

        if (state.use_local_this) {
            nl();
            print(state.method.sym.owner.name.toString() + "* lthis=const_cast<" + state.method.sym.owner.type + "*>(this);");
//			print(state.method.sym.owner.name.toString() + "* lthis=this;");
        }

        ClassSymbol cs = state.method.sym.enclClass();

        if (tree.name == tree.name.table.names.init && cs.init_constructors != null) {
            for (JCVariableDecl a : cs.init_constructors) { //initializers from the class body (int a =0;)
                if ((a.mods.flags & Flags.STATIC) == 0) //		&&(a.sym.flags_field&Flags.ATOMIC)!=0)
                {
                    nl();
                    print(a.name);
                    print("=");
                    printExpr(fixPointer(a.sym, a.init));
                    print(";");
                }
            }
        }

		//Set<VarSymbol> used=JCTree.usedVars(state.method.body);
        matchmap = state.method.match;

        for (VarSymbol vs : state.method.constraintsSyms.keySet()) {
            Set<Pair<VarSymbol, Pair<Integer, JCExpression>>> set = state.method.constraintsSyms.get(vs);

            Pair<VarSymbol, Pair<Integer, JCExpression>> match = state.method.match.get(vs);
            if (match != null) //must verify constraints!
            {
                for (Pair<VarSymbol, Pair<Integer, JCExpression>> check : set) {
                    boolean constraint = (check.snd.snd.getTag() == JCTree.GT || check.snd.snd.getTag() == JCTree.GE
                            || check.snd.snd.getTag() == JCTree.LT || check.snd.snd.getTag() == JCTree.LE)
                            && (((JCBinary) check.snd.snd).lhs.getTag() == JCTree.IDENT);

                    boolean self = constraint && ((JCIdent) ((JCBinary) check.snd.snd).lhs).sym == vs;

                    if (!constraint || self) {
                        if (check != match) {
                            nl();
                            print("assert(");

                            JCExpression e = check.snd.snd;

                            if (constraint) {
                                e = ((JCIdent) ((JCBinary) check.snd.snd).lhs);
                            }

                            printExpr(e);//JCExpression,VarSymbol,Pair<VarSymbol,Pair<Integer,JCExpression>>
                            print("==");
                            state.arrayEmitter.printSize((Type.ArrayType) check.fst.type.getArrayType(), state.jc.make.Ident(check.fst), state.jc.make.Literal(check.snd.fst));
                            print(");");
							//assert(subst(check[x/match])==check.size[dim])
                            //state.arrayEmitter.printSize((Type.ArrayType)match.fst.type.getArrayType(),state.jc.make.Ident(match.fst),state.jc.make.Literal(match.snd.fst));
                        }
                        if (constraint) {
                            nl();
                            print("assert(");
                            printExpr(check.snd.snd);//JCExpression,VarSymbol,Pair<VarSymbol,Pair<Integer,JCExpression>>
                            print(");");

                        }
                    }
                }
            }
        }

        matchmap = null;

        //emit lock for events if anything lock worthy is used
        if (!state.is_atomic && state.is_event && (state.method.sym.isSampling)) { //FIXME: need lock also when calling non-public meths!
            align();
            print("funky::sustained_rw_mutex::scoped_upgradeable_lock lock;");
            println();
            align();
            //print("for(funky::uint32 spin=0;spin<100;spin++)");
            //println();
            //align();
            print("if(lock.try_acquire(mutex))");
            println();
            align();
            print("{");
            indent();
        }
    }

    //state.method exit
    public void handleExit(JCMethodDecl tree) throws IOException {
        releaseGroup();

        state.singularEmitter.handleExit(tree);

        state.inside_method = false;

//		println();
        undent();
        nl();
        print("}");
    }

    //main entry point for methods
    public void visitMethodDef(JCMethodDecl tree) {

        if (!tree.emit()) { //ignore templates, empty constructors etc
            return;
        }

        try {

            // when producing state.source output, omit anonymous constructors
            if (tree.name == tree.name.table.names.init
                    && state.enclClassName == null) {
                return;
            }

            state.method = tree;
            state.current_scheduler = tree;
            state.current_group = "";

            state.debugPos(tree.pos);

            state.is_linear = (state.method.sym.owner.flags() & Flags.LINEAR) != 0;
            state.is_static = (state.method.sym.flags_field & Flags.STATIC) != 0;
            state.is_constructor = tree.name == tree.name.table.names.init;

            state.is_event = (state.method.restype == null && tree.name != tree.name.table.names.init);
            state.is_sample = (state.method.restype != null && tree.name != tree.name.table.names.init && (state.method.sym.owner.flags() & Flags.SINGULAR) != 0) && (state.method.sym.flags_field & Flags.STATIC) != 0;
            state.is_evo = (state.method.sym.owner.flags() & Flags.SINGULAR) != 0 && !state.is_static;
            state.is_void = (tree.getReturnType() != null && tree.getReturnType().type.tag == TypeTags.VOID);
            state.is_atomic = (state.method.sym.owner.flags() & Flags.ATOMIC) != 0;

            state.use_local_this = ((state.method.sym.flags_field & Flags.LOOP) != 0 && (state.method.sym.flags_field & Flags.STATIC) == 0);

            state.kernel = false;
            state.atomic_processed = false;
            state.task_return = false;
            state.waiting=false;
            state.returns_inside_task=false;


            state.blocking_spawns = 0;
            state.spawns = 0;
			//online inline if there are no parallel paths:

            //dbg output .dot
            if (state.header) {
                float w = (float) state.work.getWork(tree, state.method);
                float m = (float) state.work.getMem(tree, state.method);

                String info = "\\n[w:" + ff(w) + ",m:" + ff(m);

                if (state.method.sym.LocalTGWidth > 0) {
                    info += ",p:" + state.method.sym.LocalTGWidth;
                }

                if (state.method.sym.mayBeRecursive) {
                    info += ",rec";
                }
                if (state.method.pUET != 0.f) {
                    info += ",pet:" + ff(state.method.pPET) + "/" + (state.method.pUET / state.method.pPET) + " NoPrec:" + ff(state.method.pUET) + " Naive:" + ff(state.method.pUUET) + " NaiveCores:" + state.method.sym.NaiveTGWidth + " SU:" + w / state.method.pPET + " SUpC:" + w / (state.method.pPET * state.method.sym.LocalTGWidth);

                    if (state.jc.dumpMerge) {
                        System.out.println(state.method.toString() + "::" + info + "]");
                    }
                }

                info += "]";

                dgprint(getNodeDef(tree, "\"#FF0000\"", info) + ";\n");
            }

			//FIXME: are funs not defed in headers inlined properly??
            //boolean inline = ((state.is_event && state.is_atomic)||(!state.is_event && state.is_static)) && state.taskEmitter.hasSchedulerPaths(tree.getAlvilSchedules()) == 0;
            boolean inline = ((state.method.sym.flags_field & Flags.INLINE) != 0 || (state.is_event && state.is_atomic)) && state.taskEmitter.hasSchedulerPaths(tree.getAllSchedules()) == 0;

            handleStaticVars(tree);

            boolean is_trigger = (tree.mods.flags & Flags.FINAL) != 0;

            //also for trigger get
            if ((state.method.sym.flags_field & Flags.BLOCKING) != 0) {
                if (state.header) {
                    state.sequentialEmitter.printCallContext(tree, is_trigger);//emit context for blocking state.method (constructor with args)
                }
                state.sequentialEmitter.printCallTask(tree, is_trigger); //emit task (FIXME: trigger: double?)
            }

            if (is_trigger) { //it's a trigger, trigger get is handled by default case, now we do the set case!
                state.singularEmitter.handleTrigger(tree, is_trigger);
                return;
            }

            state.is_context_refcount = false;

            //prepare task deps and print tasks:
            Set<iTask> paths = state.taskEmitter.preparePaths(tree);

            state.methodpaths = paths;

            state.singularEmitter.preparePaths(tree);

            if (state.method.sym.mayBeRecursive && state.spawns - state.blocking_spawns > 0) {
                state.kernel = true;
            }

            needs_context = state.spawns > 0 && (!state.kernel);//!state.com.isEmpty() : empty com not sufficient!! may need self (for Calltask)

            if (!state.header && inline) {
                return;
            }

            handleHeader(tree, inline);

            if (!state.is_event && !state.is_linear && !state.is_constructor && !state.is_static) {
                print(" const");//non-unique objects are immutable
            }
            if (((!state.header && !inline) || (state.header && inline)) && tree.body != null) {

                handleEntry(tree);

				//gen parallel/sequential body
                //state.task_return = state.method.transitive_returns > 0;

                //state.spawns all indep tasks (reused by other scheduling nodes like IF)
                if (needs_context) {
                    genImplicitDecls(paths);//task_local vars must be generated here
                    state.taskEmitter.genMethodTGHeader(tree, tree, true, true);
                } else {
                    genImplicitDecls(paths);//task_local vars must be generated here

                    genMethodHeader(tree);
                }

                if (state.taskEmitter.getSchedulerPaths(state.method, paths, true) > 0) {
                    state.taskEmitter.genMethodTGBody(state.method, false, tree, paths);
                } else {
                    state.task_return = false;
                    state.kernel_joining_paths = state.joining_paths;
                    state.kernel_task_return = state.task_return;
					//state.joining_paths = 0;
                    //state.task_return = false;

                    if (needs_context) {
                        genImplicitDecls(paths);//task_local vars must be generated here
                    }
                    state.taskEmitter.genMethodBodyCore(state.method, false, tree, paths);
                }

                //waits for tasks (if necessary)
                if (needs_context) {
                    state.taskEmitter.genMethodTGFooter(tree);
                } else {
                    genMethodFooter(tree);
                }

                handleExit(tree);

            } else {

                if (!(!state.header && inline)) {

                    if (!state.is_evo && !state.is_constructor && !state.is_linear && !state.is_static && (state.method.sym.owner.flags_field & Flags.LINEAR) != 0 && (state.method.sym.owner.flags_field & Flags.SINGULAR) == 0) {
                        print(" __restrict__");//unique objects do not alias
                    }
                    print(";");
                }
            }

            /*
             * Andreas Wagner: Code for Recursion optimization
             */
            if (state.header && state.jc.supportMPI && state.jc.optimizeRecursion && !tree.reuseableVars.isEmpty()) {
                handleHeaderMPIRec(tree, inline);
                print(";");
            }

            if (!state.header && state.jc.supportMPI && state.jc.optimizeRecursion && !tree.reuseableVars.isEmpty()) {
                visitMethodDefMPIRec(tree);
            }

            state.methodpaths = null;
            state.kernel = false;
            state.method_has_context = false;
            state.method = null;

            if (tree.restricted_impls != null) { //dump impls with less cores
                nl();
                nl();
                float oldCount = state.target.coreCount;
                for (Integer i : tree.restricted_impls.keySet()) {
                    state.target.coreCount = i;
                    visitMethodDef(tree.restricted_impls.get(i));
                }
                state.target.coreCount = oldCount;
            }

            if ((tree.sym.flags_field & Flags.ACYCLIC) != 0 && !state.generate_into) { //dump version of state.method that accepts __FORWARD__
                nl();
                nl();
                state.generate_into = true;
                boolean wasInside = state.insideParallelRegion;
                state.insideParallelRegion = true;
                visitMethodDef(tree);
                state.insideParallelRegion = wasInside;
                state.generate_into = false;
            }

        } catch (IOException e) {
            state.kernel = false;
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    /*
     * Andreas Wagner
     * dedicated method for generating a *_MPI method (recursion reuse)
     */
    public void visitMethodDefMPIRec(JCMethodDecl tree) {
        boolean old_in_mpi_rec = state.in_mpi_rec;
        state.in_mpi_rec = true;

        if (!tree.emit()) { //ignore templates, empty constructors etc
            return;
        }

        try {

            // when producing state.source output, omit anonymous constructors
            if (tree.name == tree.name.table.names.init
                    && state.enclClassName == null) {
                return;
            }

            state.method = tree;
            state.current_scheduler = tree;
            state.current_group = "";

            state.debugPos(tree.pos);

            state.is_linear = (state.method.sym.owner.flags() & Flags.LINEAR) != 0;
            state.is_static = (state.method.sym.flags_field & Flags.STATIC) != 0;
            state.is_constructor = tree.name == tree.name.table.names.init;

            state.is_event = (state.method.restype == null && tree.name != tree.name.table.names.init);
            state.is_sample = (state.method.restype != null && tree.name != tree.name.table.names.init && (state.method.sym.owner.flags() & Flags.SINGULAR) != 0) && (state.method.sym.flags_field & Flags.STATIC) != 0;
            state.is_evo = (state.method.sym.owner.flags() & Flags.SINGULAR) != 0 && !state.is_static;
            state.is_void = (tree.getReturnType() != null && tree.getReturnType().type.tag == TypeTags.VOID);
            state.is_atomic = (state.method.sym.owner.flags() & Flags.ATOMIC) != 0;

            state.use_local_this = ((state.method.sym.flags_field & Flags.LOOP) != 0 && (state.method.sym.flags_field & Flags.STATIC) == 0);

            //state.kernel = false;
            state.atomic_processed = false;
			//state.task_return = false;

			//state.blocking_spawns = 0;
            //state.spawns = 0;
            //online inline if there are no parallel paths:
			//FIXME: are funs not defed in headers inlined properly??
            //boolean inline = ((state.is_event && state.is_atomic)||(!state.is_event && state.is_static)) && state.taskEmitter.hasSchedulerPaths(tree.getAllSchedules()) == 0;
            boolean inline = ((state.method.sym.flags_field & Flags.INLINE) != 0 || (state.is_event && state.is_atomic)) && state.taskEmitter.hasSchedulerPaths(tree.getAllSchedules()) == 0;

            handleStaticVars(tree);

            boolean is_trigger = (tree.mods.flags & Flags.FINAL) != 0;

            //also for trigger get
            if ((state.method.sym.flags_field & Flags.BLOCKING) != 0) {
                //state.sequentialEmitter.printCallTask(tree, is_trigger); //emit task (FIXME: trigger: double?)
            }

            if (is_trigger) { //it's a trigger, trigger get is handled by default case, now we do the set case!
                //state.singularEmitter.handleTrigger(tree, is_trigger);
                return;
            }

            state.is_context_refcount = false;

			//prepare task deps and print tasks:
            //Set<iTask> paths = state.taskEmitter.preparePaths(tree);
            Set<iTask> paths = state.methodpaths;
			//state.methodpaths = paths;

			//state.singularEmitter.preparePaths(tree);
            if (state.method.sym.mayBeRecursive && state.spawns - state.blocking_spawns > 0) {
                state.kernel = true;
            }

            needs_context = state.spawns > 0 && (!state.kernel);//!state.com.isEmpty() : empty com not sufficient!! may need self (for Calltask)

            if (!state.header && inline) {
                return;
            }

            handleHeaderMPIRec(tree, inline);

            if (!state.is_event && !state.is_linear && !state.is_constructor && !state.is_static) {
                print(" const");//non-unique objects are immutable
            }
            if (((!state.header && !inline) || (state.header && inline)) && tree.body != null) {

                handleEntry(tree);

				//gen parallel/sequential body
                state.task_return = state.method.transitive_returns > 0;

                //state.spawns all indep tasks (reused by other scheduling nodes like IF)
                if (needs_context) {
                    state.taskEmitter.genMethodTGHeader(tree, tree.scheduler, true, true);
                } else {
                    genImplicitDecls(paths);//task_local vars must be generated here

                    genMethodHeader(tree);
                }

                if (state.taskEmitter.getSchedulerPaths(state.method, paths, true) > 1 && state.target.coreCount > 1) {
                    state.taskEmitter.genMethodTGBody(state.method, false, tree, paths);
                } else {
                    state.task_return = false;
                    state.kernel_joining_paths = state.joining_paths;
                    state.kernel_task_return = state.task_return;
					//state.joining_paths = 0;
                    //state.task_return = false;

                    if (needs_context) {
                        genImplicitDecls(paths);//task_local vars must be generated here
                    }
                    state.taskEmitter.genMethodBodyCore(state.method, false, tree, paths);
                }

                //waits for tasks (if necessary)
                if (needs_context) {
                    state.taskEmitter.genMethodTGFooter(tree);
                } else {
                    genMethodFooter(tree);
                }

                handleExit(tree);

            } else {

                if (!(!state.header && inline)) {

                    if (!state.is_evo && !state.is_constructor && !state.is_linear && !state.is_static && (state.method.sym.owner.flags_field & Flags.LINEAR) != 0 && (state.method.sym.owner.flags_field & Flags.SINGULAR) == 0) {
                        print(" __restrict__");//unique objects do not alias
                    }
                    print(";");
                }
            }

            state.methodpaths = null;
            state.kernel = false;
            state.method_has_context = false;
            state.method = null;

            if ((tree.sym.flags_field & Flags.ACYCLIC) != 0 && !state.generate_into) { //dump version of state.method that accepts __FORWARD__
                nl();
                nl();
                state.generate_into = true;
                state.insideParallelRegion = true;
                visitMethodDefMPIRec(tree);
                state.insideParallelRegion = false;
                state.generate_into = false;
            }

        } catch (IOException e) {
            state.kernel = false;
            throw new LowerTree.UncheckedIOException(e);
        }
        state.in_mpi_rec = old_in_mpi_rec;
    }

    public void visitVarDef(JCVariableDecl tree) {
        try {
            if (tree.type.tag == TypeTags.GROUP || tree.type.tag == TypeTags.THREAD) {
                return;
            }

            if (tree.sym.owner instanceof ClassSymbol && !state.header) {
                return;
            }

            if (tree.nop_if_alone) {
                return;
            }

            if ((tree.sym.flags() & Flags.PARAMETER) == 0) {
                println();
                align();
            }

            printDocComment(tree);

            if ((tree.mods.flags & ENUM) != 0) {
                print("/*public static final*/ ");
                print(tree.name);
                if (tree.init != null) {
                    print(" /* = ");
                    printExpr(tree.init);
                    print(" */");
                }
            } else {
                if (tree.init != null && tree.sym.time != null) {
                    nl();
                    print("static funky::ProfileEntry* __profile__" + tree.pos + "=funky::Profile::RegisterCustom(\"" + tree.pos + "\",\"" + tree.sym.owner.name + "\",\"" + tree.sym.time + "\");");

                    nl();
                    print("tbb::tick_count __TIME__" + tree.pos + "=tbb::tick_count::now();");
                }

                if ((tree.mods.flags & VARARGS) != 0) {
                    printExpr(((JCArrayTypeTree) tree.vartype).elemtype);

                    print("... " + tree.name); //FIXME: this code path doesn't state.work

                    state.log.error(tree.pos, "not.impl", tree);

                } else {
                    if (tree.type.tag != TypeTags.VOID && (!state.inside_method || (tree.sym.getSymbol().tasklocal() || (state.kernel && !state.blocking_com.contains(tree.sym))))) {

                        VarSymbol vs = tree.sym.getSymbol();
                        Symbol rs = TreeInfo.symbol(tree.init);

						//avoid NOPS
                        //FIXME: rhs ok?
                        if (rs != null && rs instanceof VarSymbol && vs.getSymbol() == ((VarSymbol) rs).getSymbol()) {
                            print("//");
                        }

                        printExpr(tree.mods);

                        if ((tree.sym.flags_field & Flags.ATOMIC) != 0)//make it atomic..if necessary :)
                        {
                            print("tbb::atomic< ");
                        }

                        if (!(tree.vartype.type instanceof Type.MethodType)) {
                            state.typeEmitter.printType(tree.sym); //not the same as print(state.typeEmitter.getType(tree.vartype.type, tree.name.toString()));!!
                        } else {
                            print(state.typeEmitter.getType(tree.vartype.type, tree.name.toString()));
                        }

                        if ((tree.sym.flags_field & Flags.ATOMIC) != 0) {
                            print(" >");
                        }
                    }

                    //if(tree.type.getArrayType().isProjection())
                    //    print("&&");
                    if (tree.type.tag != TypeTags.VOID && !(tree.vartype.type instanceof Type.MethodType)) {
                        if (tree.sym instanceof VarSymbol) {
                            if (tree.sym.tasklocal() || state.inside_task == null) {
                                print(" ");
                            }

                            visitVarSymbol((VarSymbol) tree.sym, true);
                        } else {
                            print(" " + tree.name);
                        }
                    }
                }

                if (tree.init != null) {
                    if (tree.sym.owner.kind == Kinds.TYP) {
                        print(" /* ");
                    }

                    VarSymbol vs = tree.sym;
                    Symbol rs = TreeInfo.symbol(tree.init);

                    VarSymbol rvs = null;
                    if (rs instanceof VarSymbol) {
                        rvs = (VarSymbol) rs;
                    }

                    if (!state.atomicEmitter.state.atomicEmitter.handleAtomic(vs, tree.init, rvs)) {
                        if (tree.type.tag != TypeTags.VOID) {
                            print(" = ");
                        }
                        printExpr(fixPointer((VarSymbol) tree.sym, tree.init));
                    }

                    if (tree.sym.owner.kind == Kinds.TYP) {
                        print(" */ ");
                    }

                }
                if (state.prec == TreeInfo.notExpression) {
                    print(";");
                }

                if (tree.sym.time != null) {
                    nl();
                    print("__profile__" + tree.pos + "->AddMeasurement((tbb::tick_count::now()-__TIME__" + tree.pos + ").seconds()*1e6);");
                }

            }
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitSkip(JCSkip tree) {
        try {
            print("");
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    //dump statements in a path (maybe for sequential execution) and gen dbg dot output
    public void printPathStats(Set<iTask> paths, JCTree scheduler, boolean do_kernel) throws IOException {
        Set<JCTree> g = new LinkedHashSet<JCTree>();
        for (iTask p : paths) {
            JCTree fcn = p.getFirstCalcNode(state.method, p.getCalcNodes());
            if (fcn.getTag() != JCTree.CF && (scheduler == null || fcn.scheduler == scheduler)) {

                state.DumpSchedule(scheduler, p, state.task_map, state.dump_kernel);

                g.addAll(p.getCalcNodes());
            }
        }

        printStats(paths.iterator().next().getPathBlockFromSet(g, state.method).getStatements());
    }

    public void visitCF(JCCF tree) {
        if (tree.condition == null && !state.allowUnconditional) {
            return;
        }

        JCTree old_scheduler = state.current_scheduler;
        state.current_scheduler = tree;
        boolean old_task_return = state.task_return;
        state.task_return = tree.transitive_returns > 1;
        boolean old_kernel_task_return = state.kernel_task_return;
        state.kernel_task_return = state.task_return;
        try {

            state.taskEmitter.fixRefsInBranches(tree);

            Set<iTask> paths = tree.getSchedule();

            //emit/spawn tasks which are CF dependent
            if (state.taskEmitter.getSchedulerPaths(tree, paths, true) > 1) {

                state.taskEmitter.genMethodTGBody(tree, true, state.method, paths);

            } else if (paths.size() > 0) {

				//state.task_return = false;//FIXME: ???
                //FIXME: state.kernel_task_return
                state.taskEmitter.genMethodBodyCore(tree, true, state.method, paths);
            }

        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        } finally {
            state.current_scheduler = old_scheduler;
            state.task_return = old_task_return;
            state.kernel_task_return = old_kernel_task_return;
        }
    }

    public void visitBlock(JCBlock tree) {
        try {
            boolean had_context = state.method_has_context;
            printFlags(tree.flags);
            printBlock(tree.stats);
            state.method_has_context = had_context;
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
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

            nl();
            print("if ");
            if (tree.cond.getTag() == JCTree.PARENS) {
                printExpr(tree.cond);
            } else {
                print("(");
                printExpr(tree.cond);
                print(")");
            }

            if (true_part == null) {
                if (state.inside_task != null) {
                    state.log.error(tree.pos, "internal.missing.branch");
                }

                print(" ");

                nl();
                print("{");
                indent();
                nl();

                //refcounting of possible returns (so finally can be executed if need be)
                if (!state.is_event && state.inside_task != null && state.method.final_value != null && tree.elsepart != null && tree.elsepart.transitive_returns > 0 && state.task_return)//must refcount possible exits
                {
                    nl();
                    if (state.method.transitive_returns > 1) {
                        print("if((" + context() + "->exitcount-=" + tree.elsepart.transitive_returns + ")==0)");
                    }
                    print("context()->tasks.task_return->decrement_ref_count();");
                }

                if (tree.thenpart instanceof JCBlock) { //unpack :)
                    printStats(((JCBlock) tree.thenpart).stats);
                } else {
                    printStat(tree.thenpart);
                }

                releaseGroup();

                undent();
                nl();
                print("}");

                if (tree.elsepart != null) {
                    print(" else ");

                    nl();
                    print("{");
                    indent();
                    nl();

                    if (!state.is_event && state.inside_task != null && state.method.final_value != null && tree.thenpart.transitive_returns > 0 && state.task_return)//must refcount possible exits
                    {
                        nl();
                        if (state.method.transitive_returns > 1) {
                            print("if((" + context() + "->exitcount-=" + tree.thenpart.transitive_returns + ")==0)");
                        }
                        print("context()->tasks.task_return->decrement_ref_count();");
                    }

                    if (tree.elsepart instanceof JCBlock) {
                        printStats(((JCBlock) tree.elsepart).stats);
                    } else {
                        printStat(tree.elsepart);
                    }

                    releaseGroup();

                    undent();
                    nl();
                    print("}");
                } else if (!state.is_event && state.inside_task != null && state.method.final_value != null && tree.thenpart.transitive_returns > 0 && state.task_return)//must refcount possible exits
                {

                    nl();
                    print("else ");
                    if (state.method.transitive_returns > 1) {
                        print("if((" + context() + "->exitcount-=" + tree.thenpart.transitive_returns + ")==0)");
                    }
                    print("context()->tasks.task_return->decrement_ref_count();");
                }

            } else {
                state.taskEmitter.visitIf(tree);
            }

        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitIdent(JCIdent tree) {
        try {

            if (tree.sym instanceof VarSymbol) {
                visitVarSymbol((VarSymbol) tree.sym, false);
            } else if (tree.sym instanceof MethodSymbol) {
                visitMethodSymbol((MethodSymbol) tree.sym);
            } else {
                if (tree.name.toString().equals("String")) {
                    print("char");
                } else if (tree.name.equals(tree.name.table.names._super)) {
                    if (tree.type.getArrayType().tag == TypeTags.ARRAY) {
                        print("__this__");
                    }
                } else {
                    print(tree.name);
                }
            }

        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitIfExp(JCIfExp tree) {
        try {
            //just package the if into a lambda
            print("([&]()->");

            state.typeEmitter.printType(tree.type);

            print("{");

            print("if ");
            if (tree.cond.getTag() == JCTree.PARENS) {
                printExpr(tree.cond);
            } else {
                print("(");
                printExpr(tree.cond);
                print(")");
            }
            print(" { ");
            print("return ");
            printExpr(tree.thenpart);
            print(";");
            print(" } ");
            if (tree.elsepart != null) {
                print(" else ");
                print(" { ");
                print("return ");
                printExpr(tree.elsepart);
                print(";");
                print(" } ");
            }
            print("}) ()");
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitExec(JCExpressionStatement tree) {
        try {
            printExpr(tree.expr);
            if (state.prec == TreeInfo.notExpression) {
                print(";");
            }
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    //translate tail-recursion into loop
    void tailRec(JCReturn tree, JCExpression exp) throws IOException {
        state.debugPos(tree.pos);
        JCMethodInvocation apply = (JCMethodInvocation) exp;
        MethodSymbol s = (MethodSymbol) TreeInfo.symbol(apply.meth);
        if (s == state.method.sym) {

            tmpSymbol = new LinkedHashSet<VarSymbol>();
            Iterator<VarSymbol> pi = s.params.iterator();
            for (JCExpression e : apply.args) {
                VarSymbol next = pi.next();
                nl();
                state.typeEmitter.printType(next);
                print(" ");
                visitVarSymbol(next, false);
                print("__TMP=");
                boolean ptr = next.type.isPointer();
                if (ptr) {
                    print("const_cast<" + next.type + "*>(");
                }
                visitVarSymbol(next, false);
                if (ptr) {
                    print(")");
                }
                print(";");

                tmpSymbol.add(next);

                nl();
                arg_out = true;
                visitVarSymbol(next, false);
                arg_out = false;
                print("=");

                if (ptr) {
                    print("const_cast<" + next.type + "*>(");
                }
                printExpr(e);
                if (ptr) {
                    print(")");
                }
                print(";");
            }

            tmpSymbol = null;

            if (apply.meth.getTag() != JCTree.IDENT) {
                nl();
                print("lthis=const_cast<" + state.method.sym.owner.type + "*>(");
                disable_apply = true;
                printExpr(apply.meth);
                disable_apply = false;

                print(");");

            }

			if (state.inside_kernel)
			{
				nl();
				print("__KERNEL_LEVEL++;");
			}

        } else {
            if ((tree.flags & Flags.FINAL) == 0) {
                state.log.error(tree.pos, "non.tail.recursive.loop", tree);
            }
        }
        nl();
        print("goto ");
        print("LOOP_LABEL");
        if (state.loop_label_kernel) {
            print("_KERNEL");
        }
        if (state.loop_label_sched == null) {
            print("DUMMY");
        } else {
            print("" + state.loop_label_sched.pos);
        }

        print(";");

        nl();
    }

    public void returnExp(JCReturn tree, JCExpression exp) throws IOException {

        if ((state.inside_task == null && (((tree.flags & (Flags.FINAL | Flags.SYNCHRONIZED)) == Flags.SYNCHRONIZED) || !state.method_has_context))) { //no task, just plain code
            printWhere();
            if (state.inside_task == null && (state.method.sym.flags_field & Flags.LOOP) != 0 && exp != null && exp.getTag() == JCTree.APPLY && TreeInfo.symbol(((JCMethodInvocation) exp).meth) == state.method.sym) {
                tailRec(tree, exp);
            } else {

                //cancel or finally:
                if ((tree.flags & (Flags.SYNCHRONIZED | Flags.FINAL)) != 0) {

//					nl();
                    releaseGroup();

                    if (state.is_void && exp != null) {
                        nl();
                        printExpr(exp);
                        print(";");
                    }

                    nl();
                    if (state.domainIterState.iter != null && state.domainIterState.iter.mayReturn && state.inside_task == null) {
                        print("__lambda_return" + state.domainIterState.iter.pos + "=true;");
                        if (!state.is_void && state.method.getReturnType() != null && exp != null) {
                            print(" __lambda_return_value" + state.domainIterState.iter.pos + "=");
                        }
                    } else {
                        print("return");
                    }

                    if (!state.is_void && state.method.getReturnType() != null && exp != null) {
                        print(" ");
                        printExpr(fixPointer(state.method.getReturnType(), exp));
                    }
                    print(";");
                } else //handle resume (return is not executed immidietly but postponed to the end of the block)
                {
                    nl();
                    print("RESUME_STORE_SET=true;");
                    nl();
                    if (state.method.getReturnType() != null && exp != null && state.method.getReturnType().type.tag != TypeTags.VOID) {
                        print("RESUME_STORE=");
                        printExpr(fixPointer(state.method.getReturnType(), exp));
                        print(";");
                    }
                }
            }
        } else if (state.method.getReturnType() != null) { //we're inside a task
            if (!state.is_void && exp != null) { //if we should return something and have a return expression

				//construct type
                //Type t = exp.type;
                if ((state.inside_task != null || state.method_has_context) && ((tree.flags & Flags.FINAL) == 0 || state.returns_inside_task))//no need to check if no one returns anyways
                {                    
                    /*
                     * Andreas Wagner: Context is local data structure
                     * so Return() makes no sense
                     *
                     */
                    if (state.jc.supportMPI) {
                        //print("if(context.Return()){");
                    } else {

                        print("if(" + context() + "->Return()){");//check if someone else has already returned
                    }
                }
                if ((tree.flags & Flags.FINAL) != 0 && state.joining_paths > 0 && !state.kernel)//wait for input deps of final exp (if any)
                {
                    if (state.jc.supportMPI) {

                    } else {
                        if(!state.waiting)
                            print("" + self_task() + "wait_for_all();");//wait for those tasks that contribute to the return expr
                    }
                }

                printWhere();

                if (state.inside_task == null && (state.method.sym.flags_field & Flags.LOOP) != 0 && exp != null && exp.getTag() == JCTree.APPLY && TreeInfo.symbol(((JCMethodInvocation) exp).meth) == state.method.sym) {
                    tailRec(tree, exp);
                } else {
                    //store return value (since it's a task, we just store the value and the state.method footer will actually return the value)

                    if (state.inside_task == null && (tree.flags & (Flags.SYNCHRONIZED | Flags.FINAL)) != 0) {
                        //FIXME: resume??
                        nl();
                        releaseGroup();

                        nl();
                        if (state.domainIterState.iter != null && state.domainIterState.iter.mayReturn && state.inside_task == null) {
                            print("__lambda_return" + state.domainIterState.iter.pos + "=true;");
                            print(" __lambda_return_value" + state.domainIterState.iter.pos + "=");
                        } else {
                            print("return ");
                        }
                        printExpr(fixPointer(state.method.getReturnType(), exp));
                        print(";");
                    } else {
                        nl();
                        /*
                         * Andreas Wagner: see above
                         */
                        if (state.jc.supportMPI) {
                            print("context.SetReturn(");
                        } else {
                            print("" + context() + "->SetReturn(");
                        }
                        printExpr(fixPointer(state.method.getReturnType(), exp));
                        print(");");

                        /*
                         * Andreas Wagner: check if we are a reused task and must notify ourselves
                         * FIXME: is this general enough?
                         */
                        /*
                         if(state.jc.supportMPI && state.jc.optimizeRecursion &&
                         exp.getTag() == JCTree.APPLY &&
                         TreeInfo.symbol(((JCMethodInvocation) exp).meth) == state.method.sym){
                         nl();
                         print("self_notified = true;");
                         nl();
                         }
                         */
                    }
                    //state.method footer of returning state.method waits for retval..so notify it!
                    if (!state.is_event && (tree.flags & Flags.FINAL) == 0 && state.inside_task != null && state.method.transitive_returns > 0 && state.task_return) {
                        /*
                         * Andreas Wagner: in MPI, notification is done by sending context back
                         */
                        if (state.jc.supportMPI) {
                            //context sending is done in footer
                        } else {
                            print("" + context() + "->tasks.task_return->decrement_ref_count();");
                        }
                    }
                }

                if (((tree.flags & Flags.FINAL) == 0 || state.returns_inside_task)) {
                    if ((tree.flags & (Flags.SYNCHRONIZED | Flags.FINAL)) != 0 && state.inside_task == null) {
                        print("} ");
                        if (state.method.getReturnType().type.tag != TypeTags.VOID) {
                            print("else return " + context() + "->GetReturn();");
                        } else {
                            print("else return;");
                        }
                    } else {
                        if (!state.jc.supportMPI) {
                            print("}");
                        }
                    }

                }
            } else { //we're returning from a void state.method
                nl();
                if (exp != null) {
                    if (state.returns_inside_task) {
                        print("if(" + context() + "->Return()){");
                    }

                    if (state.inside_task == null && (state.method.sym.flags_field & Flags.LOOP) != 0 && exp != null && exp.getTag() == JCTree.APPLY && TreeInfo.symbol(((JCMethodInvocation) exp).meth) == state.method.sym) {

                        if (state.joining_paths > 0)//wait for input eps of final exp (if any)
                        {
                            if (state.jc.supportMPI) {

                            } else {
                                print("" + self_task() + "wait_for_all();");
                            }
                        }

                        printWhere();

                        tailRec(tree, exp);

                    } else {

                        if (state.method.final_value != null && !state.is_event && (tree.flags & Flags.FINAL) == 0 && state.method.transitive_returns > 0 && state.task_return) {
                            print("" + context() + "->tasks.task_return->decrement_ref_count();");
                        }

                        if ((tree.flags & Flags.FINAL) != 0 && state.joining_paths > 0)//wait for input eps of final exp (if any)
                        {
                            if (state.jc.supportMPI) {

                            } else {
                                print("" + self_task() + "wait_for_all();");
                            }
                        }
                        printWhere();

                        printExpr(exp);
                        print(";");
                    }
                    if (state.returns_inside_task) {
                        print("}");
                    }
                } //indicate that the state.method finished (without result)..needed for finally in void methods
                else {
                    printWhere();
                    print(context() + "->Return();");
                    if (state.method.final_value != null && !state.is_event && (tree.flags & Flags.FINAL) == 0 && state.method.transitive_returns > 0 && state.task_return) {
                        print("" + context() + "->tasks.task_return->decrement_ref_count();");
                    }
                }
            }
        }
    }

    public void handleWhereAssignments(JCWhere w) throws IOException {
        Set<VarSymbol> reads = new LinkedHashSet<VarSymbol>();

        for (JCAssign a : w.writes) {
            reads.addAll(w.reads.get(a));
        }

        Set<JCAssign> assigns = new LinkedHashSet<JCAssign>();
        assigns.addAll(w.writes);

        int count;
        int last_count;

        //ugly fix-point iteration to remove non-cyclic writes
        do {
            last_count = assigns.size();
            for (Iterator<JCAssign> i = assigns.iterator(); i.hasNext();) {
                JCAssign a = i.next();
                if (!reads.contains((VarSymbol) TreeInfo.symbol(a.lhs))) //nothing depends on output!
                {
                    nl();
                    printExpr(a);
                    print(";");
                    i.remove();
                }
            }
            count = assigns.size();
        } while (count != last_count);

		//the rest are cyclic!!
        //could do something clever with dep graph...but who cares :P
        reads.clear();
        for (JCAssign a : assigns) {
            reads.addAll(w.reads.get(a));
        }

        tmpSymbol = new LinkedHashSet<VarSymbol>();

        for (VarSymbol vs : reads) {
			//we're lazy and just create a temp copy of all cyclic read deps
            //hopefully the c++ compiler will optimize the unnecessary temps
            nl();
            state.typeEmitter.printType(vs);

            print(" " + vs.name + "__TMP = ");
            visitVarSymbol(vs, false);
            print(";");
            tmpSymbol.add(vs);//use tmp symbol instead of real symbol
        }

        for (JCAssign a : assigns)//do the assignments using the tmps
        {
            nl();
            printExpr(a);
            print(";");
        }

        tmpSymbol = null;
    }

    public void printWhere() throws IOException {
        if (whereExpr == null) {
            return;
        }
        if (state.is_event && whereExpr.atomic != null) {
			//nl();
            //print("//ATOMIC:");
            if (!state.atomic_processed)//should be a cmp&swap..otherwise we have a problem
            {
                //INC is only ok if finally is the only!stmt
                if (whereExpr.atomic.type != AtomicTarget.AtomicType.CMPXCHG) {
                    if (whereExpr.atomic.type != AtomicTarget.AtomicType.INC || state.method.body.stats.length() > 1) {
                        state.log.error(whereExpr.pos, "non.atomic.computation", whereExpr);
                    }
                }

                if (whereExpr.body != null) {
                    printStat(whereExpr.body);
                    handleWhereAssignments(whereExpr);
                } else {
                    printExpr(whereExpr.sexp);

                    print(";");
                }
            }
        } else {
            if (state.is_event && !state.method.uses_field.isEmpty()) { //must upgrade mutex before writing to event object
                if (state.inside_task == null) {
                    print("mutex.upgrade();");
                    println();
                    align();
                } else {
                    print(context() + "->params.self->mutex.upgrade();");
                    println();
                    align();
                }
            }

            if (whereExpr.body != null) {
                printStat(whereExpr.body);
                handleWhereAssignments(whereExpr);
            } else {
                printExpr(whereExpr.sexp);

                print(";");
            }
        }
        whereExpr = null;
    }

    //special case when returning an a where b...
    public void visitReturnWhere(JCReturn tree) throws IOException {
        //emit where body before return (obviously) :P
        whereExpr = ((JCWhere) tree.expr);

        if (!state.is_event) {
            nl();
            if (state.inside_task == null && (state.method.sym.owner.flags_field & Flags.SINGULAR) != 0) //FIXME: is sample?
            {
                printWhere();

                print("return;");//WHY THIS??
            } else {
                returnExp(tree, whereExpr.exp); //generate the actual return
            }

        } else {
            printWhere();
        }

        whereExpr = null;
    }

    public void visitReturn(JCReturn tree) {
        try {

            //finally is ignored unless we specifically allow it in genMethod*Footer
            if ((tree.flags & Flags.FINAL) != 0 && !state.allow_final)//finally only allowed in state.method footer
            {
                return;
            }
            
            if (state.inside_task!=null&&(tree.flags & Flags.FINAL) == 0)
                state.returns_inside_task=true;

            if (tree.expr != null) {
                tree.expr.return_expression = true;
            }

            //handle: return x where y; separately
            if (tree.expr instanceof JCWhere) {
                visitReturnWhere(tree);
            } else {
                returnExp(tree, tree.expr);
            }

        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitPragma(JCPragma tree) {
        //nothing to do...
    }

    public void visitAssert(JCAssert tree) {
        try {
            //just forward asserts
            nl();
            print("assert (");
            printExpr(tree.cond);
            print(");");
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitSizeOf(JCSizeOf tree) {
        try {
            //sizeof needed for calculating data sizes :)
            print("sizeof(");
            printExpr(tree.expr);
            print(")");
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }
    
    String getThis(JCMethodInvocation tree) throws IOException
    {
        if (state.inside_task == null) {
            if (state.use_local_this) {
                return "lthis";
            }
            return "this";
        } else {
            return (context() + "->params.self");
        }
    }

    public void visitApply(JCMethodInvocation tree) {
        try {
            if (tree == state.domainIterState.index) {
                state.arrayEmitter.visitDomIndex(tree);
                return;
            }

            Symbol msym = TreeInfo.symbol(tree.meth);
            if (msym instanceof MethodSymbol) {
                MethodSymbol ms = (MethodSymbol) msym;
                if (ms.isDomainProjection) {
                    state.arrayEmitter.visitProjection(tree);
                    return;
                }
                if (ms.name == state.names.resize) {
                    state.arrayEmitter.visitResize(tree);
                    return;
                }
            }

            boolean pass_object = false;
            boolean trigger_get = false;
            boolean is_kernel = false;
            boolean will_be_kernel = false;

            Symbol s = TreeInfo.symbol(tree.meth);
            MethodSymbol ms = null;
            if (s instanceof MethodSymbol) {
                ms = ((MethodSymbol) s);
            }

            boolean forward = state.domainIterState.forwardCall != null && state.domainIterState.forwardCall == ms;

            if (forward) {
                state.domainIterState.forwardCall = null;
            }

            if (ms != null && ms.mayBeRecursive) {
				//FIXME: must respect blocking unless we're inside blocked call (so not in task)
                //check wehther we're inside the recursion or not!
                if (((MethodSymbol) s).decl.getDGNode().IsReachable(false, state.method.sym.decl, topolNodes, callGraph, true)) {
                    will_be_kernel = true;
                }
            }

            if ((s.flags() & Flags.FINAL) != 0 && tree.getTriggerReturn() != null) { //trigger set (defer to I/O pool)
                //FIXME: wrap this in lambda?
                //set
           		
           		String name = getThis(tree)+"->"+((MethodSymbol)s).triggerName;
                nl();
                print(name+".lock();");
                nl();
                //print("funky::Thread::SpawnTask< " + state.typeEmitter.getTypeNoVoid(tree.meth.type.getReturnType(), "unkown") + " >(new(tbb::task::allocate_additional_child_of(*funky::TaskRoot<>::GetRoot())) ");
                //triggertaskSET(&ContextSET(args,tree.getTriggerReturn()));
                if (s instanceof MethodSymbol) {
                    JCMethodDecl md = (JCMethodDecl) ((MethodSymbol) s).implementations.iterator().next();
                    String mdname = md.name.toString() + md.pos;

                    String type = state.typeEmitter.getTypePure(s.owner.type);

                    if ((s.owner.flags_field & Flags.NATIVE) != 0) {
                        type = state.typeEmitter.getTypePure(state.method.sym.owner.type);
                    }

                    type.replaceAll("\\.", "::");

                    print("funky::ConstTask<"+type + "::CALL_CONTEXT_SET" + mdname+">* __TRIGGER_SET"+tree.pos+"=(new(tbb::task::allocate_additional_child_of(*funky::TaskRoot<>::GetRoot())) ");
                    print(type + "::CALL_TASK_SET" + mdname + "(*new " + type + "::CALL_CONTEXT_SET" + mdname);
                    pass_object = true;
                }
                pass_object = true;
            } else if (((s.flags() & Flags.BLOCKING) != 0 && !will_be_kernel) || state.is_blocking)//general case for blocking call
            {
				//get
                //get self!
                //boolean do_thread=(s.flags() & Flags.FORCE_PARALLEL) != 0;

                if (tree.meth.type.getReturnType().tag == TypeTags.VOID) {
                    print("funky::Thread::SpawnTask< " + state.typeEmitter.getTypeNoVoid(tree.meth.type.getReturnType(), "unkown") + " >(new(tbb::task::allocate_additional_child_of(*funky::TaskRoot<>::GetRoot())) ");
                } else {

                    if (state.jc.supportMPI) {
                        String self = "";

                        self = "SELF().";

                        print("funky::Thread::BlockTask< " + state.typeEmitter.getTypeNoVoid(tree.meth.type.getReturnType(), "unkown") + " >(new(" + self + "allocate_child()) ");
                    } else {
                        String self = "";
                        if (state.inside_task == null) {
                            self = "SELF().";
                        }
                        print("funky::Thread::BlockTask< " + state.typeEmitter.getTypeNoVoid(tree.meth.type.getReturnType(), "unkown") + " >(new(" + self + "allocate_child()) ");

                    }
                }
                //triggertask(&Context(args),??);
                if (ms != null) {
                    JCMethodDecl md = (JCMethodDecl) (ms).implementations.iterator().next();
                    String name = md.name.toString() + md.pos;
                    String type = state.typeEmitter.getTypePure(s.owner.type);
                    if ((s.owner.flags_field & Flags.NATIVE) != 0) {
                        type = state.typeEmitter.getTypePure(state.method.sym.owner.type);
                    }

                    type.replaceAll("\\.", "::");

                    if (tree.meth.type.getReturnType().tag == TypeTags.VOID) {
                        print(type + "::CALL_TASK" + name + "(*new " + type + "::CALL_CONTEXT" + name);
                    } else {
                        print(type + "::CALL_TASK" + name + "(" + type + "::CALL_CONTEXT" + name);
                    }
                    pass_object = true;
                    if ((s.flags() & Flags.FINAL) != 0 && tree.getTriggerReturn() == null) {
                        trigger_get = true;
                    }
                }
            } else {//std, just emit call
                printExpr(tree.meth);

                if (forward) {
                    print("__FORWARD__");
                }
            }

            boolean old_blocking = state.is_blocking;
            state.is_blocking = false;

            boolean old_arg_out = arg_out;

            //arguments...
            print("(");

            boolean first = true;

            boolean lis_static = false;
            //printExprs(tree.args);
            if (s instanceof MethodSymbol) {
                MethodSymbol meth = (MethodSymbol) s;
                lis_static = (meth.flags_field & Flags.STATIC) != 0;
                Iterator<VarSymbol> pi = meth.params.iterator();
                for (JCExpression e : tree.args) {
					if(e.getTag()!=JCTree.NEWARRAY||!((JCNewArray)e).elems.isEmpty())//skip empty elipses
					{
						first = false;
						if (e != tree.args.head) {
							print(", ");
						}

						VarSymbol sym = pi.next();

						arg_out = ((sym.flags_field & Flags.FOUT) != 0);

						//handle out ars
						if (((sym.flags_field & Flags.FINOUT) != Flags.FOUT && trigger_get)) //in trigger get case all args are out instead of in..so give address
						{
							print("&");
						}

						printExpr(fixPointer(sym, e));
					}
                }
            } else {
                printExprs(tree.args, ", ");
                first = false;
            }

            if (tree.getTriggerReturn() != null) {
                if (tree.args.size() > 0) {
                    print(", ");
                }
                first = false;
                printExpr(tree.getTriggerReturn()); //pass address of trigger return val as last arg (convention)
            }
            if (pass_object) {//pass this as last arg
                disable_apply = true;
                if (!lis_static) {
                    if (!tree.args.isEmpty() || tree.getTriggerReturn() != null) {
                        print(", ");
                    }
                    first = false;
                    if (tree.meth instanceof JCIdent) {//WHY THIS?
                        if (state.inside_task == null) {
                            if (state.use_local_this) {
                                print("l");
                            }
                            print("this");
                        } else {
                            print(context() + "->params.self");
                        }
                    } else {
                        printExpr(tree.meth);
                    }
                }
                disable_apply = false;
            }

            if (trigger_get)//pass current UID
            {
                if (!first) {
                    print(", ");
                }
                print("&");
                if ((state.method.sym.flags_field & Flags.STATIC) == 0) {
                    if (state.inside_task == null) {
                        if (state.use_local_this) {
                            print("l");
                        }
                        print("this");
                    } else {
                        print(context() + "->params.self");
                    }
                    print("->");
                }
                print("TRIGGER_UID_" + tree.pos);
            }

            is_kernel = will_be_kernel;

            if (forward) {
                if (!tree.args.isEmpty() || tree.getTriggerReturn() != null || pass_object) {
                    print(", ");
                }

                state.arrayEmitter.printForwardObject();
            }

            if (is_kernel && state.spawns > 0 && state.redirect_recursion == null) {
                if (!tree.args.isEmpty() || tree.getTriggerReturn() != null || pass_object || forward) {
                    print(", ");
                }
                if (state.inside_task != null) {
                    /**
                     * Andreas Wagner: kernel_level in MPI is spawning_level
                     */
                    if (state.jc.supportMPI) {
                                        //spawning_level is already extracted from context
                        //kernel_level is atm 0, when MPI is used
                        print("0, spawning_level+1");
                        //provide additional parameters if recursion optimization is used
                        if (state.jc.optimizeRecursion) {
                            //Variables which can be reused
                            for (iTask task : state.method.reuseableVars.keySet()) {
                                if (state.task_map.get(task) != null && state.task_map.get(task) != state.inside_task) {
                                    print(", ");
                                    print(" " + state.task_map.get(task));
                                    print("_MPI_ID");
                                }
                            }
                        }
                    } else {
                        print(context() + "->kernel_level+1");
                    }
                } else {
                    print("__KERNEL_LEVEL+1");
                }
            }

            if (pass_object) {
                print("))");
            }

            print(")");
            
            if ((s.flags() & Flags.FINAL) != 0 && tree.getTriggerReturn() != null) { //trigger set (defer to I/O pool)
                print(";");
           		String name = getThis(tree)+"->"+((MethodSymbol)s).triggerName;
                nl();
                print("if("+name+".can_write())");
                nl();        
                print("{");
                indent();
                nl();
                    nl();
                    print(name+".start_write();");
                    nl();
                    print("funky::Thread::SpawnTask< int >(__TRIGGER_SET"+tree.pos+");");
                undent();
                nl();
                print("}");
                nl();
                print("else");
                nl();
                print("{");
                indent();
                nl();
                    print(name+".enqueue(__TRIGGER_SET"+tree.pos+");");
                    
                undent();
                nl();
                print("}");
                nl();
                print(name+".unlock()");                
                /*
                
            TriggerInstance1168.lock();    
            
            if(TriggerInstance1168.can_write())
            {
                TriggerInstance1168.start_write();
                funky::Thread::SpawnTask< int >(new(tbb::task::allocate_additional_child_of(*funky::TaskRoot<>::GetRoot())) EVO::CALL_TASK_SETTrigger1168(*new EVO::CALL_CONTEXT_SETTrigger1168(ni, this)));                
            }
            else
            {
                TriggerInstance1168.enqueue(new(tbb::task::allocate_additional_child_of(*funky::TaskRoot<>::GetRoot())) EVO::CALL_TASK_SETTrigger1168(*new EVO::CALL_CONTEXT_SETTrigger1168(ni, this)));
            }
            
            TriggerInstance1168.unlock();
                
                
                */
            }

            state.is_blocking = old_blocking;
            arg_out = old_arg_out;

        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitNewClass(JCNewClass tree) {
        try {

            Type ot = tree.type;
            if (ot.tag == TypeTags.CLASS && ((Type.ClassType) ot).getArrayType().tag == TypeTags.ARRAY) {
                //handle case where class is actually an array
                ot = ((Type.ClassType) ot).getArrayType();
                Type.ArrayType at = (Type.ArrayType) ot;
                state.arrayEmitter.printNewArray(tree.pos(), at.dom, at.elemtype, null, null);
                return;
            }

            if (tree.encl != null) {
                printExpr(tree.encl);
                print(".");
            }

            if (!skip_new) {
                print("new ");

                //MALLOC_ATOMIC for classes gives problems
                if (false && (TreeInfo.symbol(tree.clazz).flags_field & Flags.PARAMETER) == 0) {
                    print("(GC_MALLOC_ATOMIC(sizeof(" + state.typeEmitter.getType(tree.clazz.type) + "))) ");
                }

                print(state.typeEmitter.getTypePure(tree.clazz.type));
                print("(");
            }
            printExprs(tree.args);
            if (!skip_new) {
                print(")");
            }

            if (tree.def != null) {
                Name enclClassNamePrev = state.enclClassName;
                state.enclClassName
                        = tree.def.name != null ? tree.def.name
                                : tree.type != null && tree.type.tsym.name != tree.type.tsym.name.table.names.empty
                                        ? tree.type.tsym.name : null;
                if ((tree.def.mods.flags & Flags.ENUM) != 0) {
                    print("/*enum*/");
                }
                printBlock(tree.def.defs);
                state.enclClassName = enclClassNamePrev;
            }
            skip_new = false;
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitNewArray(JCNewArray tree) {
        //REDIRECT
    }

    public void visitParens(JCParens tree) {
        try {
            print("(");
            printExpr(tree.expr);
            print(")");
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

	//fixPointer methods take care to (de-)reference symbols as required
    //for state.method arguments!
    public JCTree fixPointer(VarSymbol target, JCTree source) {
        try {
            if (target.type.tag == TypeTags.TYPEVAR) {
                return source;
            }
            boolean targPtr = ((target.flags_field & Flags.PARAMETER) != 0) && target.type.isPointer();
            Symbol s = TreeInfo.symbol(source);
            boolean scPtr = (s != null && (s.flags_field & Flags.PARAMETER) != 0) && source.type.isPointer();

            if (targPtr != scPtr) {
                //must convert source to target
                if (targPtr) {
                    print("&");
                } else {
                    if (source.getTag() == JCTree.NEWCLASS) {
                        skip_new = true;
                    } else {
                        print("*");
                    }
                }
            }

        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
        return source;
    }

    //force pointer
    public JCTree fixPointer(boolean target_pointer, JCTree source) {
        try {
            Symbol s = TreeInfo.symbol(source);
            boolean scPtr = (s != null && (s.flags_field & Flags.PARAMETER) != 0) && source.type.isPointer();

            if ((!target_pointer) == scPtr) {
                //must convert source to target
                if (target_pointer) {
                    print("&");
                } else {
                    if (source.getTag() == JCTree.NEWCLASS) {
                        skip_new = true;
                    } else {
                        if (!source.type.isPrimitive()) {
                            print("*");
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
        return source;
    }

    //for assignments!
    public JCTree fixPointer(JCTree target, JCTree source) {
        try {
            if (target.type.tag == TypeTags.TYPEVAR) {
                return source;
            }
            Symbol s = TreeInfo.symbol(target);
            boolean targPtr = (s != null && (s.flags_field & Flags.PARAMETER) != 0) && target.type.isPointer();
            s = TreeInfo.symbol(source);
            boolean scPtr = (s != null && (s.flags_field & Flags.PARAMETER) != 0) && source.type.isPointer();

            if (targPtr != scPtr) {
                //must convert source to target
                if (targPtr) {
                    print("&");
                } else {
                    if (source.getTag() == JCTree.NEWCLASS) {
                        skip_new = true;
                    } else {
                        print("*");
                    }
                }
            }
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
        return source;
    }

    public void visitAssign(JCAssign tree) {
        try {
            if (state.method.init_constructors == null || !state.method.init_constructors.contains(tree)) {

                VarSymbol vs = (VarSymbol) TreeInfo.symbol(tree.lhs);
                Symbol rs = TreeInfo.symbol(tree.rhs);

                VarSymbol rvs = null;
                if (rs instanceof VarSymbol) {
                    rvs = (VarSymbol) rs;
                }

				//avoid NOPS
                //FIXME: rhs ok?
                //boolean skip=false;
                if (rs instanceof VarSymbol && vs.getSymbol() == ((VarSymbol) rs).getSymbol()) {
                    print("//");
                }

                state.open(state.prec, TreeInfo.assignPrec);

                if ((vs.flags_field & Flags.PARAMETER) == 0 && ((state.inside_task != null && vs.tasklocal()) || (state.inside_task == null && vs.isLocal())) && (state.inner_symbols!=null&&!state.inner_symbols.contains(vs) && !state.com.contains(vs))) {
                    //if lhs is an unused implicit variable then we have not declared it anywhere, so we skip the assignment
                    printExpr(fixPointer(false, tree.rhs), TreeInfo.assignPrec);
                } else if ((vs.flags_field & Flags.FOUT) != 0 && (vs.flags_field & Flags.PARAMETER) != 0)//for out vars we do not return a pointer
                {

                    print("*");//deref

                    boolean oao = arg_out;
                    arg_out = true;
                    printExpr(tree.lhs, TreeInfo.assignPrec + 1);
                    arg_out = oao;
                    //print(" = ");
                    if (!state.atomicEmitter.handleAtomic(vs, tree.rhs, rvs)) {
                        print(" = ");
                        printExpr(fixPointer(false, tree.rhs), TreeInfo.assignPrec);
                    }
                } else {
                    boolean oao = arg_out;
                    arg_out = true;
                    printExpr(tree.lhs, TreeInfo.assignPrec + 1);
                    arg_out = oao;
                    //
                    if (!state.atomicEmitter.handleAtomic(vs, tree.rhs, rvs)) {
                        print(" = ");
                        printExpr(fixPointer(tree.lhs, tree.rhs), TreeInfo.assignPrec);
                    }
                }

                state.close(state.prec, TreeInfo.assignPrec);

            }
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitWhere(JCWhere tree) {
        try {
            //state.log.error(tree.pos, "not.impl", tree);

            print("[&]()->");
            state.typeEmitter.printType(tree.exp.type);
            print("{");
            if (tree.body != null) {

                if (tree.dg_end != null) {
                    iTask ps = state.current_tree.getSchedule().iterator().next();
                    state.DumpSchedule(state.current_tree, ps, state.task_map, state.dump_kernel);
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
                    releaseGroup();

                } else {
                    printStat(tree.body);
                }
            } else {
                printExpr(tree.sexp);
            }
            print(";");
            print("return ");
            printExpr(tree.exp);
            print(";}()");
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitFor(JCFor tree) {
        try {
            state.log.error(tree.pos, "not.impl", tree);
            print("#for ( " + tree.name.toString() + " , ");

            printExpr(tree.exp);
            print(" ) {");
            printStats(tree.content);
            print(" }");

        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitSelectCond(JCSelectCond tree) {
        try {
            state.log.error(tree.pos, "not.impl", tree);
            print(" ");

            if (tree.cond != null) {
                printExpr(tree.cond);
            } else {
                print(" _ ");
            }

            print(" : ");
            if (tree.res != null) {
                print("{ ");
                printExpr(tree.res);
                print(" }");
            } else {
                printStat(tree.stmnt);
            }
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitSelectExp(JCSelect tree) {
        try {
            state.log.error(tree.pos, "not.impl", tree);
            print(" select ");
            print(" { ");

            for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {
                printExpr(l.head);
            }
            print(" } ");
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitCaseExp(JCCaseExp tree) {
        //obsolete
        try {
            state.log.error(tree.pos, "not.impl", tree);
            print(" case ");
            printExpr(tree.exp);
            print(" { ");

            for (List<JCSelectCond> l = tree.list; l.nonEmpty(); l = l.tail) {
                printExpr(l.head);
            }
            print(" } ");
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

	//handle [x'new=x] unique arguments!
    //TODO Is this behavior correct? Portential bug.
    public void visitArgExpression(JCArgExpression tree) {
        try {
            if (tree.exp2 == null) {
                printExpr(tree.exp1);
            } else {
                printExpr(tree.exp1);
            }
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public String operatorName(int tag) {
        switch (tag) {
            case JCTree.POS:
                return "+";
            case JCTree.NEG:
                return "-";
            case JCTree.NOT:
                return "!";
            case JCTree.COMPL:
                return "~";
            case JCTree.PREINC:
                return "++";
            case JCTree.PREDEC:
                return "--";
            case JCTree.POSTINC:
                return "++";
            case JCTree.POSTDEC:
                return "--";
            case JCTree.NULLCHK:
                return "<*nullchk*>";
            case JCTree.OR:
                return "||";
            case JCTree.AND:
                return "&&";
            case JCTree.EQ:
                return "==";
            case JCTree.NE:
                return "!=";
            case JCTree.LT:
                return "<";
            case JCTree.GT:
                return ">";
            case JCTree.LE:
                return "<=";
            case JCTree.GE:
                return ">=";
            case JCTree.BITOR:
                return "|";
            case JCTree.BITXOR:
                return "^";
            case JCTree.BITAND:
                return "&";
            case JCTree.SL:
                return "<<";
            case JCTree.SR:
                return ">>";
            case JCTree.USR:
                return ">>>";
            case JCTree.SEQ:
                return ":";
            case JCTree.PLUS:
                return "+";
            case JCTree.MINUS:
                return "-";
            case JCTree.MUL:
                return "*";
            case JCTree.DIV:
                return "/";
            case JCTree.MOD:
                return "%";
            default:
                throw new Error();
        }
    }

    public void visitAssignop(JCAssignOp tree) {
        try {
            state.open(state.prec, TreeInfo.assignopPrec);
            printExpr(tree.lhs, TreeInfo.assignopPrec + 1);
            print(" " + operatorName(tree.getTag() - JCTree.ASGOffset) + "= ");
            printExpr(tree.rhs, TreeInfo.assignopPrec);
            state.close(state.prec, TreeInfo.assignopPrec);
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitUnary(JCUnary tree) {
        try {
            int ownprec = TreeInfo.opPrec(tree.getTag());
            String opname = operatorName(tree.getTag());
            state.open(state.prec, ownprec);
            if (tree.getTag() <= JCTree.PREDEC) {
                print(opname);
                printExpr(tree.arg, ownprec);
            } else {
                printExpr(tree.arg, ownprec);
                print(opname);
            }
            state.close(state.prec, ownprec);
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    //a::b::c
    public void visitSequence(JCSequence tree) {
        try {
            boolean oldSkip = state.skipNL;
            state.skipNL = true;
            for (JCExpression e : tree.seq) {
                nl();
                printStat(e);
                print(";");
            }
            state.skipNL = oldSkip;
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitBinary(JCBinary tree) {
        try {
            int ownprec = TreeInfo.opPrec(tree.getTag());
            String opname = operatorName(tree.getTag());
            state.open(state.prec, ownprec);
            printExpr(tree.lhs, ownprec);
            print(" " + opname + " ");
            printExpr(tree.rhs, ownprec + 1);
            state.close(state.prec, ownprec);
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitSelect(JCFieldAccess tree) {
        try {

            if (tree.conversion != null) {

                print("/*CONVERSION*/");
                printExpr(tree.conversion);//compiler generated translation from one domain to another, see visitSelectin Attr.java
                return;
            }

            Type t = tree.selected.type;

            if (tree.repackage) //cast of dyn sized domain
            {
                Type.ArrayType at = (Type.ArrayType) t;
                if (at.dom != null && (at.dom.isDynamic())) {
                    state.arrayEmitter.printNewVersion(tree.pos(), tree.selected, at.dom, at.elemtype, tree.params);
                    return;
                }
            }

//			boolean array_class_cast = false;
            if (tree.selected.type.tag == TypeTags.CLASS) {//do not emit selection from native
                if (((tree.sym).flags_field & Flags.STATIC) != 0 && (((ClassSymbol) tree.selected.type.tsym).flags_field & Flags.NATIVE) != 0) {
                    print(tree.name);
                    return;
                }
                /*
                 if (((Type.ClassType) tree.selected.type).getArrayType().tag == TypeTags.ARRAY&&tree.sym.type.tag==TypeTags.METHOD) {
                 array_class_cast = true;
                 print("((");
                 print(state.typeEmitter.getTypeInner(tree.selected.type, "unknown", false));
                 print(")");
                 if(!((Type.ArrayType)((Type.ClassType) tree.selected.type).getArrayType()).dom.isBaseDomain)
                 print("funky::rvalue_address");
                 print("(");
                 }
                 */
            }

            //for constructors: drop this.
            if (tree.selected.toString().equals("this")) {
                if (skip_this || (state.inside_task != null && !state.is_event)) {
                    visitSymbol(tree, tree.sym);
                    return;
                }
            }

            if ((tree.selected.getTag() == JCTree.IDENT || tree.selected.getTag() == JCTree.TYPEAPPLY) && (tree.sym.flags_field & Flags.STATIC) != 0) {
                print(tree.selected.type.toString("unknown"));//avoid printing type modifiers (* etc)
            } else {
                if (!tree.selected.toString().equals("super")) {
                    printExpr(tree.selected, TreeInfo.postfixPrec);
                } else {
                    print(state.typeEmitter.getType(tree.selected.type));
                }
            }
            /*
             if (array_class_cast) {
             print("))");
             }
             */
            if (t.getArrayType().tag == TypeTags.ARRAY && tree.sym.type.tag == TypeTags.DOMAIN) //nothing to emit here
            {
                return;
            }

            self_selected = true;
            if (!disable_apply) {

                if ((tree.selected.getTag() == JCTree.IDENT || tree.selected.getTag() == JCTree.TYPEAPPLY) && (tree.sym.flags_field & Flags.STATIC) != 0) {
                    print("::");//namespace
                } else if ((tree.selected.type.isPointer() || !tree.selected.type.isPrimitive())) {
					//c++ want's ./-> acess for (non-) pointers
                    //arrays element type can have option, if not it's NOT a pointer, even for non-primitive types
                    print("->");
                } else {
                    print(".");
                }

                if (tree.sym instanceof MethodSymbol && ((MethodSymbol) tree.sym).triggerName != null && (tree.sym.flags_field & Flags.BLOCKING) != 0)//trigger state.method without "()" querries if trigger is ready for get
                {
                    //FIXME: is this still supported?
                    print(((MethodSymbol) tree.sym).triggerName + ".can_read(");
                    if ((state.method.sym.flags_field & Flags.STATIC) == 0) {
                        if (state.inside_task == null) {
                            if (state.use_local_this) {
                                print("l");
                            }
                            print("this");
                        } else {
                            print(context() + "->params.self");
                        }
                        print("->");
                    }
                    print("TRIGGER_UID_" + tree.pos);
                    print(")");

                } else {
                    //tail rec loops that overwrite this...
                    if (state.use_local_this && !tree.name.equals(state.names._this) && !self_selected && TreeInfo.symbol(tree).owner.kind == Kinds.TYP) {
                        print("lthis->");
                    }

                    if (state.redirect_recursion != null && tree.sym == state.method.sym) {
                        print(JCTree.fixName(state.redirect_recursion.name.toString()) + JCTree.fixName(state.redirect_recursion.type.toString()));
                    } else {
                        if (tree.sym.type.tag == TypeTags.METHOD) {
                            print(JCTree.fixName(tree.sym.name.toString()) + JCTree.fixName(tree.sym.type.toString()));
                        } else {
                            print(tree.sym.name);
                        }
                    }
                }
            }
            self_selected = false;

        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    //patches various things when a var symbol is encountered
    public void visitVarSymbol(VarSymbol vs, boolean disallow_alias) throws IOException {

        vs = vs.getSymbol(); //get real symbol (from aliasGlobal), linear vars refer to the first var of the chain

        if (vs == state.symbolFind) { //might need to replace for atomics
            vs = (VarSymbol) state.symbolReplace;
        }

        if (vs.name.equals(vs.name.table.names._super)) {
            print("__this__");
            return;
        }

        if (matchmap != null) {
            Pair<VarSymbol, Pair<Integer, JCExpression>> match = matchmap.get(vs);
            if (match != null) {
                state.arrayEmitter.printSize((Type.ArrayType) match.fst.type.getArrayType(), state.jc.make.Ident(match.fst), state.jc.make.Literal(match.snd.fst));
                return;
            }
        }

        boolean cast_array_class = vs.name == state.names._this && vs.type.tag == TypeTags.CLASS && ((Type.ClassType) vs.type).getArrayType().tag == TypeTags.ARRAY;
        if (cast_array_class) { //class inherits from array, cast to wrapper type
            print("__");
        }

        boolean done = false;

        if (!done && state.index_map != null) { //fix params for reinterpreted domain
            JCExpression e = state.index_map.get(vs);
            if (e != null) {
                print("(");
                printExpr(e);
                print(")");
                done = true;
            }
        }

        if (!done && state.lastProjectionArgs != null) { //fix params for reinterpreted domain
            JCExpression e = state.lastProjectionArgs.get(vs);
            if (e != null) {
                print("(");
                printExpr(e);
                print(")");
                done = true;
            }
        }

        if (!done && state.subst_map != null) { //fix params for reinterpreted domain
            String e = state.subst_map.get(vs);
            if (e != null) {
                print("(");
                print(e);
                print(")");
                done = true;
            }
        }

        if (!done && state.method != null && state.method.constraintsSyms != null && state.method.constraintsSyms.get(vs) != null) {
            print(vs.name.toString().replace('\'', '_') + "__CONSTRAINT");
            done = true;
        }

        if (!done) {//have we already emited something?

            if (!arg_out && tmpSymbol != null && tmpSymbol.contains(vs)) {
                print(vs.name.toString().replace('\'', '_') + "__TMP");//add __TMP for tailrec
                return;
            }

            //where is the variable..inside context, local, ...
            if (state.inside_task != null && !vs.tasklocal()) { //get var from context
                /**
                 * Andreas Wagner: special var-handling for MPI
                 */
                if (state.jc.supportMPI) {
                    if ((vs.flags() & Flags.STATIC) == 0) {
                        if ((vs.flags() & Flags.PARAMETER) != 0) {
                            print("context.params.");
                        } else if (vs.isLocal()) {
                            print("context.frame.");
                        } else {
                            print(context() + "->params.self->");//must be this->
                        }
                    } else {
                        print(state.typeEmitter.getType(vs.owner.type) + "::");
                    }

                    String name = vs.toString();

                    if (state.inside_task == null && vs.name.equals(state.names._this) && state.use_local_this) {
                        print("l");
                    }
                    print(name.replace('\'', '_'));//FIXME: duplicate state.names?
                } else {
                    if ((vs.flags() & Flags.STATIC) == 0) {
                        if ((vs.flags() & Flags.PARAMETER) != 0) {
                            print(context() + "->params.");
                        } else if (vs.isLocal()) {
                            print(context() + "->frame.");
                        } else if(state.inside_task != null &&vs.name.equals(state.names._this)){
                            print(context() + "->params.self");//must be this->
                        } else {
                            print(context() + "->params.self->");//must be this->
                        }
                    } else {
                        print(state.typeEmitter.getType(vs.owner.type) + "::");
                    }

					if(state.inside_task == null ||!vs.name.equals(state.names._this))
					{
						String name = vs.toString();

						if (state.inside_task == null && vs.name.equals(state.names._this) && state.use_local_this) {
							print("l");
						}
						print(name.replace('\'', '_'));//FIXME: duplicate state.names?
					}
                }
            } else {
                if (state.inside_task != null && vs.name.toString().equals("this")) {
                    print(context() + "->params.self");
                } else {

                    if (state.method_has_context && (vs.flags() & Flags.PARAMETER) == 0 && (!vs.tasklocal() && !state.kernel) && vs.isLocal()) {
                        print(context() + "->frame.");
                    } else if (state.inside_task == null && vs.name.equals(state.names._this) && state.use_local_this) {
                        print("l");
                    }

                    if (state.use_local_this && !vs.name.equals(state.names._this) && !self_selected && vs.owner != null && vs.owner.kind == Kinds.TYP) {
                        print("lthis->");
                    }

                    String name = vs.toString();
                    print(name.replace('\'', '_'));
                }
            }
        }

        if (cast_array_class) { //state.close braket
            print("__");
        }
    }

    //patch state.method symbols
    public void visitMethodSymbol(MethodSymbol vs) throws IOException {

        //generate "this." if necessary
        if (state.inside_task != null) {
            if ((vs.flags_field & Flags.STATIC) == 0) {
                print(context() + "->params.self->");
            }

        } else if (state.use_local_this && !self_selected && vs.owner.kind == Kinds.TYP) {
            print("lthis->");
        }

        if (vs == state.method.sym && state.redirect_recursion != null) {
            vs = state.redirect_recursion; //call IMPL1 instead of meth
        }

        if ((vs.owner.flags_field & Flags.NATIVE) != 0 && (vs.flags_field & Flags.STATIC) != 0) {
            print(vs.name);
        } else {
            /**
             * Andreas Wagner modification for MPI recursion optimization
             */
            if (state.jc.supportMPI && state.jc.optimizeRecursion && vs == state.method.sym && state.method.reuseableVars.size() > 0) {
                if (state.inside_task != null) {
                    print(JCTree.fixName(vs.name.toString()) + JCTree.fixName(vs.type.toString()) + "_MPI");
                } else {
                    //falback to non-optimized version
                    print(JCTree.fixName(vs.name.toString()) + JCTree.fixName(vs.type.toString()));
                }
            } else {
                print(JCTree.fixName(vs.name.toString()) + JCTree.fixName(vs.type.toString()));
            }
        }

    }

    public void visitSymbol(JCTree tree, Symbol s) throws IOException {

        if (s instanceof VarSymbol) {
            visitVarSymbol((VarSymbol) s, false);
        } else if (s instanceof MethodSymbol) {
            visitMethodSymbol((MethodSymbol) s);
        } else {
            //patch in lthis, if necessary
            if (state.use_local_this && !self_selected && s.owner.kind == Kinds.TYP) {
                print("lthis->");
            }
            print(tree);
        }

    }

    public void visitLiteral(JCLiteral tree) {
        try {
            switch (tree.typetag) {
                case TypeTags.INT:
                    print(tree.value.toString());
                    break;
                case TypeTags.LONG:
                    print(tree.value + "L");
                    break;
                case TypeTags.FLOAT:
                    print(tree.value + "F");
                    break;
                case TypeTags.DOUBLE:
                    print(tree.value.toString());
                    break;
                case TypeTags.CHAR:
                    print("\'"
                            + Convert.quote(
                                    String.valueOf((char) ((Number) tree.value).intValue()))
                            + "\'");
                    break;
                case TypeTags.BOOLEAN:
                    print(((Number) tree.value).intValue() == 1 ? "true" : "false");
                    break;
                case TypeTags.BOT:
                    print("NULL");
                    break;
                default:
                    print("\"" + Convert.quote(tree.value.toString()) + "\"");
                    break;
            }
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitModifiers(JCModifiers mods) {
        try {
            printAnnotations(mods.annotations);
            printFlags(mods.flags);

            if (mods.group != null) {
                print("/*group(");//just a comment..
                printExpr(mods.group);
                print(")*/ ");
            }

        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

    public void visitAnnotation(JCAnnotation tree) {
        try {
            state.log.error(tree.pos, "not.impl", tree);
            print("@");
            printExpr(tree.annotationType);
            print("(");
            printExprs(tree.args);
            print(")");
        } catch (IOException e) {
            throw new LowerTree.UncheckedIOException(e);
        }
    }

}
