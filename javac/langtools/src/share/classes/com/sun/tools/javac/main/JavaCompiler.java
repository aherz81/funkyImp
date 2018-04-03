/*
 * Copyright 1999-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.tools.javac.main;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.DiagnosticListener;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;

import com.sun.tools.javac.antlr.AntlrParserFactory;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.parser.*;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.jvm.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.DomainType;
import com.sun.tools.javac.comp.Scheduler.Schedule;
import com.sun.tools.javac.tree.JCTree.*;

import com.sun.tools.javac.file.RegularFileObject;
import com.sun.tools.javac.ibarvinok.ibarvinok;

import com.sun.tools.javac.processing.*;
import javax.annotation.processing.Processor;

import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static com.sun.tools.javac.util.ListBuffer.lb;

// TEMP, until we have a more efficient way to save doc comment info
import com.sun.tools.javac.parser.DocCommentScanner;

import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.util.List;
import java.util.*;
import javax.lang.model.SourceVersion;

import org.anarres.cpp.Jcpp;
import org.anarres.cpp.JavacPPException;
import org.jgrapht.*;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;


/** This class could be the main entry point for GJC when GJC is used as a
 *  component in a larger software system. It provides operations to
 *  construct a new compiler, and to run a new compiler on a set of source
 *  files.
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JavaCompiler implements ClassReader.SourceCompleter {


        //public static Context COMPILER_CONTEXT;

	/** The context key for the compiler. */
	protected static final Context.Key<JavaCompiler> compilerKey =
			new Context.Key<JavaCompiler>();

	/** Get the JavaCompiler instance for this context. */
	public static JavaCompiler instance(Context context) {
		JavaCompiler instance = context.get(compilerKey);
		if (instance == null) {
			instance = new JavaCompiler(context);
			gContext = context;
		}
		return instance;
	}

	/** The current version number as a string.
	 */
	public static String version() {
		return version("release");  // mm.nn.oo[-milestone]
	}

	/** The current full version number as a string.
	 */
	public static String fullVersion() {
		return version("full"); // mm.mm.oo[-milestone]-build
	}
	private static final String versionRBName = "com.sun.tools.javac.resources.version";
	private static ResourceBundle versionRB;

	private static String version(String key) {
		if (versionRB == null) {
			try {
				versionRB = ResourceBundle.getBundle(versionRBName);
			} catch (MissingResourceException e) {
				return Log.getLocalizedString("version.resource.missing", System.getProperty("java.version"));
			}
		}
		try {
			return versionRB.getString(key);
		} catch (MissingResourceException e) {
			return Log.getLocalizedString("version.unknown", System.getProperty("java.version"));
		}
	}

	/**
	 * Control how the compiler's latter phases (attr, flow, desugar, generate)
	 * are connected. Each individual file is processed by each phase in turn,
	 * but with different compile policies, you can control the order in which
	 * each class is processed through its next phase.
	 *
	 * <p>Generally speaking, the compiler will "fail fast" in the face of
	 * errors, although not aggressively so. flow, desugar, etc become no-ops
	 * once any errors have occurred. No attempt is currently made to determine
	 * if it might be safe to process a class through its next phase because
	 * it does not depend on any unrelated errors that might have occurred.
	 */
	protected static enum CompilePolicy {

		/**
		 * Just attribute the parse trees.
		 */
		ATTR_ONLY,
		/**
		 * Just attribute and do flow analysis on the parse trees.
		 * This should catch most user errors.
		 */
		CHECK_ONLY,
		/**
		 * Attribute everything, then do flow analysis for everything,
		 * then desugar everything, and only then generate output.
		 * This means no output will be generated if there are any
		 * errors in any classes.
		 */
		SIMPLE,
		/**
		 * Groups the classes for each source file together, then process
		 * each group in a manner equivalent to the {@code SIMPLE} policy.
		 * This means no output will be generated if there are any
		 * errors in any of the classes in a source file.
		 */
		BY_FILE,
		/**
		 * Completely process each entry on the todo list in turn.
		 * -- this is the same for 1.5.
		 * Means output might be generated for some classes in a compilation unit
		 * and not others.
		 */
		BY_TODO;

		static CompilePolicy decode(String option) {
			if (option == null) {
				return DEFAULT_COMPILE_POLICY;
			} else if (option.equals("attr")) {
				return ATTR_ONLY;
			} else if (option.equals("check")) {
				return CHECK_ONLY;
			} else if (option.equals("simple")) {
				return SIMPLE;
			} else if (option.equals("byfile")) {
				return BY_FILE;
			} else if (option.equals("bytodo")) {
				return BY_TODO;
			} else {
				return DEFAULT_COMPILE_POLICY;
			}
		}
	}
	private static CompilePolicy DEFAULT_COMPILE_POLICY = CompilePolicy.BY_TODO;

	protected static enum ImplicitSourcePolicy {

		/** Don't generate or process implicitly read source files. */
		NONE,
		/** Generate classes for implicitly read source files. */
		CLASS,
		/** Like CLASS, but generate warnings if annotation processing occurs */
		UNSET;

		static ImplicitSourcePolicy decode(String option) {
			if (option == null) {
				return UNSET;
			} else if (option.equals("none")) {
				return NONE;
			} else if (option.equals("class")) {
				return CLASS;
			} else {
				return UNSET;
			}
		}
	}
	/** The log to be used for error reporting.
	 */
	public Log log;
	/** Factory for creating diagnostic objects
	 */
	JCDiagnostic.Factory diagFactory;
	/** The tree factory module.
	 */
	public TreeMaker make;
	/** The class reader.
	 */
	protected ClassReader reader;
	/** The class writer.
	 */
	protected CodeWriter writer;
	/** The module for the symbol table entry phases.
	 */
	public Enter enter;
	/** The symbol table.
	 */
	public Symtab syms;
	/** The language version.
	 */
	protected Source source;
	/** The module for code generation.
	 */
	protected CodeGen gen;
	/** The name table.
	 */
	protected Names names;
	/** The attributor.
	 */
	public Attr attr;
	/** The attributor.
	 */
	protected Check chk;
	/** The flow analyzer.
	 */
	protected Flow flow;
	protected Linear linear;
	protected Alias alias;
	protected RefGen refGen;
        protected AliasGlobal aliasGlobal;
	protected Work work;

	protected Context context;

	private static Context gContext=null;
	public static JavaCompiler getCompiler()
	{
		return instance(gContext);
	}

	/** The type eraser.
	 */
	protected TransTypes transTypes;
	/** The syntactic sugar desweetener.
	 */
	protected Lower lower;
	/** The syntactic sugar desweetener.
	 */
	protected DesugarSyntax desugarSyntax;
	/** The syntactic sugar desweetener.
	 */
	protected DesugarPostAttr desugarPostAttr;

	protected PathGen pathGen;

	protected TaskGen taskGen;
	/** The annotation annotator.
	 */
	protected Annotate annotate;
	/** Force a completion failure on this name
	 */
	protected final Name completionFailureName;
	/** Type utilities.
	 */
	protected Types types;
	/** Access to file objects.
	 */
	public JavaFileManager fileManager;
	/** Factory for parsers.
	 */
	protected ParserFactory parserFactory;
	/** Optional listener for progress events
	 */
	protected TaskListener taskListener;
	/**
	 * Annotation processing may require and provide a new instance
	 * of the compiler to be used for the analyze and generate phases.
	 */
	protected JavaCompiler delegateCompiler;
	/**
	 * Flag set if any annotation processing occurred.
	 **/
	protected boolean annotationProcessingOccurred;
	/**
	 * Flag set if any implicit source files read.
	 **/
	protected boolean implicitSourceFilesRead;

	protected long ccTime = 0;

	private boolean doPathGen = false;

	public Properties profileData = null;

        public boolean supportOpenCL;
        public boolean useLocalOpenCLHack;
        public boolean useSimpleMemoryModel;
        public String localIdentifier;
        /**
         * Set if backend set to MPI
         **/
        public boolean supportMPI;

		public boolean verifyArrays;
		public boolean runTimeVerifyArrays;
        /**
         * Set if optimization for recursive MPI functions is enabled
         */
        public boolean optimizeRecursion;
        /**
         * Complete list of existing tasks
         */
        public Set<String> taskNames;
        /**
         * Complete list of existing tasks with namespace
         */
        public Set<String> taskNamesWithNamespace;
        /**
         * mapping of task names to tasknames with namespace
         */
        public Map<String, String> tasksToTasknameNamespaces;
        /**
         * taskCounter for mapping taskNames to integers
         * (used for #defines in header files)
         */
        public int taskCounter;

	public DirectedGraph<JCTree, Arc> callGraph= new org.jgrapht.graph.DefaultDirectedGraph<JCTree, Arc>(Arc.class);
	public java.util.Map<JCTree,Integer> topolNodes = new java.util.LinkedHashMap<JCTree,Integer>();
	//public static DirectedGraph<JCTree, Arc> callGraph = new org.jgrapht.graph.DefaultDirectedGraph<JCTree, Arc>(Arc.class);

//	protected Context context;

	public class Target {

		public float coreCount = Float.POSITIVE_INFINITY;
		public float coreSpeed = Float.POSITIVE_INFINITY;
		public float memSpeed = Float.POSITIVE_INFINITY;
		public float netSpeed = 0f;
		public float threadOverhead = 3000.f; //
		public float mpiOverhead = 100000.f;
		public float regSize=64;
		public boolean allowPragma = true;
	}

	public Properties configFile=null;

	/** Construct a new compiler using a shared context.
	 */
	public JavaCompiler(final Context context) {
		this.context = context;

		context.put(compilerKey, this);

		// if fileManager not already set, register the JavacFileManager to be used
		if (context.get(JavaFileManager.class) == null) {
			JavacFileManager.preRegister(context);
		}

		names = Names.instance(context);
		log = Log.instance(context);
		diagFactory = JCDiagnostic.Factory.instance(context);
		reader = ClassReader.instance(context);
		make = TreeMaker.instance(context);

		enter = Enter.instance(context);
		todo = Todo.instance(context);

		fileManager = context.get(JavaFileManager.class);

		try {
			// catch completion problems with predefineds
			syms = Symtab.instance(context);
		} catch (CompletionFailure ex) {
			// inlined Check.completionError as it is not initialized yet
			log.error("cant.access", ex.sym, ex.getDetailValue());
			if (ex instanceof ClassReader.BadClassFile) {
				throw new Abort();
			}
		}
		this.context=context;


		Options options = Options.instance(context);

		if (options.get("-LLVM") != null) {
			gen = LlvmGen.instance(context);
			writer = LlvmClassWriter.instance(context);
		} else {
			gen = Gen.instance(context);
			writer = ClassWriter.instance(context);
		}

		verbose = options.get("-verbose") != null;
		//jcpp          = options.get("-nopp")          == null;
		regression = options.get("-regression") != null;

		jcpp_args = options.get("-PP");
		jcpp = jcpp_args != null;

		if(jcpp)
		{
			pp=new Jcpp();
		}

		String work_args=options.get("-WORK");
		if(work_args != null)
		{
			try {
				profileData=new Properties();
				profileData.load(new java.io.FileReader(work_args));
				System.out.println("Note: loaded profile file: "+work_args);
			}
			catch(java.io.IOException e)
			{
				log.warning("config.not.found",work_args,(new File (".")).getAbsolutePath(), e.getLocalizedMessage());
				profileData=null;
			}
		}


		if(options.get("-checkarrays")!=null)
		{
			log.warning("info", "Note: emitting array out of bounds checks");
		}

		if(options.get("-PROFILE")!=null)
		{
			profile = Integer.parseInt(options.get("-PROFILE"));
		}

               if (options.get(OptionName.USELOCALHACK) != null) {
                   this.useLocalOpenCLHack = true;
                   this.localIdentifier = options.get(OptionName.USELOCALHACK);
               }

		dump_barvinok=options.get("-BARVINOK")!=null;

		config_args = options.get("-CONFIG");

		if(config_args!=null)
		{
			configFile = new Properties();
			try
			{
				configFile.load(new java.io.FileReader(config_args));
				System.out.println("Note: loaded config file: "+config_args);
				System.out.println("Architecture: "+configFile.getProperty("ARCH"));

				try {
					this.target.coreCount = Float.parseFloat(configFile.getProperty("CORES"));
					this.target.coreSpeed = Float.parseFloat(configFile.getProperty("SPEED"));
					this.target.memSpeed = Float.parseFloat(configFile.getProperty("MEM_THROUGPUT"));
					this.target.regSize = Float.parseFloat(configFile.getProperty("REG_SIZE"));
					this.target.threadOverhead = Float.parseFloat(configFile.getProperty("THREAD_OVERHEAD"));
					log.note("target.platform", this.target.coreCount, this.target.coreSpeed, this.target.memSpeed);

					if (configFile.getProperty("NET_THROUGPUT") != null) {
						this.target.netSpeed = Float.parseFloat(configFile.getProperty("NET_THROUGPUT"));
						this.target.mpiOverhead = Float.parseFloat(configFile.getProperty("MPI_OVERHEAD"));
						log.note("target.mpi", this.target.netSpeed);
					}

					System.out.println("Speedup per Core: "+configFile.getProperty("lin"));
				} catch (NumberFormatException e) {
					log.error("target.error", target);
				}


			}
			catch(java.io.IOException e)
			{
				log.error("config.not.found",config_args,(new File (".")).getAbsolutePath(), e.getLocalizedMessage());
				configFile=null;
			}
		}

		source = Source.instance(context);
		attr = Attr.instance(context);
		chk = Check.instance(context);
		flow = Flow.instance(context);
		linear = Linear.instance(context);
		alias = Alias.instance(context);
		aliasGlobal = AliasGlobal.instance(context);
		refGen = RefGen.instance(context);
                work = Work.instance(context);
		transTypes = TransTypes.instance(context);
		lower = Lower.instance(context);

		desugarSyntax = DesugarSyntax.instance(context);
		desugarPostAttr = DesugarPostAttr.instance(context);
//		if(doPathGen)
//			pathGen = PathGen.instance(context);
		taskGen = TaskGen.instance(context);

		annotate = Annotate.instance(context);
		types = Types.instance(context);
		taskListener = context.get(TaskListener.class);

		reader.sourceCompleter = this;

		cc = options.get("-CC");

		depGraph = options.get("-DEPGRAPH") != null;

		supportOpenCL = options.get(OptionName.USEOPENCL) != null;

                useSimpleMemoryModel = options.get(OptionName.SIMPLEMEMMODEL) != null;
                
		supportMPI = options.get(OptionName.MPI) != null;

		optimizeRecursion = options.get(OptionName.OPTIMIZERECURSION) != null;

		verifyArrays = options.get(OptionName.VERIFYARRAYS) != null;

		runTimeVerifyArrays = options.get(OptionName.RTVERIFYARRAYS) != null;

		forceGC=options.get("-FORCEGCCLEANUP") != null;

		dumpMerge=options.get("-DUMPMERGE") != null;

		emitCPPFlag = options.get("-CPP") != null;

		dumpAnalysis = options.get("-ANALYSIS") != null;

		if (jcpp_args == null) {
			jcpp_args = "";
		}

		sourceOutput = options.get("-printsource") != null; // used to be -s
		stubOutput = options.get("-stubs") != null;
		relax = options.get("-relax") != null;
		printFlat = options.get("-printflat") != null;
		skipGenerate = options.get("-skipgenerate") != null;
		attrParseOnly = options.get("-attrparseonly") != null;
		encoding = options.get("-encoding");
		lineDebugInfo = options.get("-g:") == null
				|| options.get("-g:lines") != null;
		genEndPos = options.get("-Xjcov") != null
				|| context.get(DiagnosticListener.class) != null;
		devVerbose = options.get("dev") != null;
		processPcks = options.get("process.packages") != null;
		werror = options.get("-Werror") != null;

		verboseCompilePolicy = options.get("verboseCompilePolicy") != null;

		if (attrParseOnly) {
			compilePolicy = CompilePolicy.ATTR_ONLY;
		} else {
			compilePolicy = CompilePolicy.decode(options.get("compilePolicy"));
		}

		implicitSourcePolicy = ImplicitSourcePolicy.decode(options.get("-implicit"));

		completionFailureName =
				(options.get("failcomplete") != null)
				? names.fromString(options.get("failcomplete"))
				: null;

		String parser = options.get("parser");
		//useAntlrParser = (parser != null && parser.equalsIgnoreCase("antlr"));
		useAntlrParser = (parser == null || parser.equalsIgnoreCase("antlr"));

		if (useAntlrParser) {
			AntlrParserFactory.preRegister(context);
		}

		parserFactory = ParserFactory.instance(context);

                taskNames = new LinkedHashSet<String>();
                taskNamesWithNamespace = new LinkedHashSet<String>();
                tasksToTasknameNamespaces = new LinkedHashMap<String, String>();
                taskCounter = 0;
	}

	/* Switches:
	 */
	/** Verbose output.
	 */
	public boolean verbose;
	/** run jcpp.
	 */
	public String cc;
	public boolean jcpp;
	public boolean read_profile;
	public int profile=0;
	public boolean regression;
	public boolean depGraph;
	public boolean forceGC;
	public boolean dumpMerge;
	public boolean dumpAnalysis;
	public boolean emitCPPFlag;
	public String jcpp_args;
	public String config_args;
	public Target target = new Target();
	BufferedWriter outGraph = null;
	/** Emit plain Java source files rather than class files.
	 */
	public boolean sourceOutput;

	public boolean dump_barvinok;
	/** Emit stub source files rather than class files.
	 */
	public boolean stubOutput;
	/** Generate attributed parse tree only.
	 */
	public boolean attrParseOnly;
	/** Switch: relax some constraints for producing the jsr14 prototype.
	 */
	boolean relax;
	/** Debug switch: Emit Java sources after inner class flattening.
	 */
	public boolean printFlat;
	/** Debug switch: Emit Java sources after inner class flattening.
	 */
	public boolean skipGenerate;
	/** The encoding to be used for source input.
	 */
	public String encoding;
	/** Generate code with the LineNumberTable attribute for debugging
	 */
	public boolean lineDebugInfo;
	/** Switch: should we store the ending positions?
	 */
	public boolean genEndPos;
	/** Switch: should we debug ignored exceptions
	 */
	protected boolean devVerbose;
	/** Switch: should we (annotation) process packages as well
	 */
	protected boolean processPcks;
	/** Switch: treat warnings as errors
	 */
	protected boolean werror;
	/** Switch: is annotation processing requested explitly via
	 * CompilationTask.setProcessors?
	 */
	protected boolean explicitAnnotationProcessingRequested = false;
	/**
	 * The policy for the order in which to perform the compilation
	 */
	protected CompilePolicy compilePolicy;
	/**
	 * The policy for what to do with implicitly read source files
	 */
	protected ImplicitSourcePolicy implicitSourcePolicy;
	/**
	 * (Temp) Whether or not to use the ANTLR parser
	 */
	protected boolean useAntlrParser;
	/**
	 * Report activity related to compilePolicy
	 */
	public boolean verboseCompilePolicy;
	/** A queue of all as yet unattributed classes.
	 */
	public Todo todo;

	public Jcpp pp=null;

	protected enum CompileState {

		TODO(0),
		ATTR(1),
		FLOW(2),
		LINEAR(3),;

		CompileState(int value) {
			this.value = value;
		}

		boolean isDone(CompileState other) {
			return value >= other.value;
		}
		private int value;
	};

	protected class CompileStates extends LinkedHashMap<Env<AttrContext>, CompileState> {

		private static final long serialVersionUID = 1812267524140424433L;

		boolean isDone(Env<AttrContext> env, CompileState cs) {
			CompileState ecs = get(env);
			return ecs != null && ecs.isDone(cs);
		}
	}
	private CompileStates compileStates = new CompileStates();
	/** The set of currently compiled inputfiles, needed to ensure
	 *  we don't accidentally overwrite an input file when -s is set.
	 *  initialized by `compile'.
	 */
	protected Set<JavaFileObject> inputFiles = new LinkedHashSet<JavaFileObject>();

	/** The number of errors reported so far.
	 */
	public int errorCount() {
		if (delegateCompiler != null && delegateCompiler != this) {
			return delegateCompiler.errorCount();
		} else {
			if (werror && log.nerrors == 0 && log.nwarnings > 0) {
				log.error("warnings.and.werror");
			}
		}
		return log.nerrors;
	}

	protected final <T> Queue<T> stopIfError(Queue<T> queue) {
		if (errorCount() == 0) {
			return queue;
		} else {
			return ListBuffer.lb();
		}
	}

	protected final <T> List<T> stopIfError(List<T> list) {
		if (errorCount() == 0) {
			return list;
		} else {
			return List.nil();
		}
	}

	/** The number of warnings reported so far.
	 */
	public int warningCount() {
		if (delegateCompiler != null && delegateCompiler != this) {
			return delegateCompiler.warningCount();
		} else {
			return log.nwarnings;
		}
	}

	/** Whether or not any parse errors have occurred.
	 */
	public boolean parseErrors() {
		return parseErrors;
	}

	/** Try to open input stream with given name.
	 *  Report an error if this fails.
	 *  @param filename   The file name of the input stream to be opened.
	 */
	public CharSequence readSource(JavaFileObject filename) {
		try {
			inputFiles.add(filename);

			//will be replaced by comp time reflections!!
			if (jcpp) {
				try {


					//FIXME: pass args to jcpp
					String args = jcpp_args;

					if (args.length() > 0) {
						args = args + ",";
					}

					args = args + filename.toUri().getPath();
					String[] ppargs = args.split(",");

					try {
						CharSequence s = pp.run(ppargs);
						((JavacFileManager) fileManager).setContent(filename, s.toString());
					} catch (JavacPPException e) {
						int pos = log.currentSource().findPos(e.line, e.col);
						log.error(pos, "error.pp.loc", e.getLocalizedMessage());
						return null;
					}

					/*
					FileWriter fstream = new FileWriter(filename.toUri().getPath()+".ppdmp");
					BufferedWriter out = new BufferedWriter(fstream);
					out.write(s.toString());
					out.close();
					 */

				} catch (Exception e) {
					//generic pp error
					log.error("error.pp.file", filename, e.getLocalizedMessage());
					return null;
				}

			}


			return filename.getCharContent(false);
		} catch (IOException e) {
			log.error("error.reading.file", filename, e.getLocalizedMessage());
			return null;
		}
	}

	/** Parse contents of input stream.
	 *  @param filename     The name of the file from which input stream comes.
	 *  @param input        The input stream to be parsed.
	 */
	public JCCompilationUnit parse(JavaFileObject filename, CharSequence content) {
		long msec = now();
		JCCompilationUnit tree = make.TopLevel(List.<JCTree.JCAnnotation>nil(),
				null, List.<JCTree>nil());
		if (content != null) {
			if (verbose) {
				printVerbose("parsing.started", filename);
			}
			if (taskListener != null) {
				TaskEvent e = new TaskEvent(TaskEvent.Kind.PARSE, filename);
				taskListener.started(e);
			}
			int initialErrorCount = log.nerrors;
			Parser parser = parserFactory.newParser(content, keepComments(), genEndPos, lineDebugInfo);
			tree = parser.parseCompilationUnit(devVerbose);
			/*
			if(tree.defs.isEmpty())
			{
			log.error(tree.pos(), "premature.eof");
			}
			 */
			parseErrors |= (log.nerrors > initialErrorCount);
			if (verbose) {
				printVerbose("parsing.done", Long.toString(elapsed(msec)));
			}
		}

		tree.sourcefile = filename;

		if (content != null && taskListener != null) {
			TaskEvent e = new TaskEvent(TaskEvent.Kind.PARSE, tree);
			taskListener.finished(e);
		}

		return tree;
	}
	// where
	public boolean keepComments = false;

	protected boolean keepComments() {
		return keepComments || sourceOutput || stubOutput;
	}

	/** Parse contents of file.
	 *  @param filename     The name of the file to be parsed.
	 */
	@Deprecated
	public JCTree.JCCompilationUnit parse(String filename) throws IOException {
		JavacFileManager fm = (JavacFileManager) fileManager;
		return parse(fm.getJavaFileObjectsFromStrings(List.of(filename)).iterator().next());
	}

	/** Parse contents of file.
	 *  @param filename     The name of the file to be parsed.
	 */
	public JCTree.JCCompilationUnit parse(JavaFileObject filename) {
		JavaFileObject prev = log.useSource(filename);
		try {
			JCTree.JCCompilationUnit t = parse(filename, readSource(filename));
			if (t.endPositions != null) {
				log.setEndPosTable(filename, t.endPositions);
			}
			return t;
		} finally {
			log.useSource(prev);
		}
	}

	/** Resolve an identifier.
	 * @param name      The identifier to resolve
	 */
	public Symbol resolveIdent(String name) {
		if (name.equals("")) {
			return syms.errSymbol;
		}
		JavaFileObject prev = log.useSource(null);
		try {
			JCExpression tree = null;
			for (String s : name.split("\\.", -1)) {
				if (!SourceVersion.isIdentifier(s)) // TODO: check for keywords
				{
					return syms.errSymbol;
				}
				tree = (tree == null) ? make.Ident(names.fromString(s))
						: make.Select(tree, make.Ident(names.fromString(s)));
			}
			JCCompilationUnit toplevel =
					make.TopLevel(List.<JCTree.JCAnnotation>nil(), null, List.<JCTree>nil());
			toplevel.packge = syms.unnamedPackage;
			return attr.attribIdent(tree, toplevel);
		} finally {
			log.useSource(prev);
		}
	}

	/** Emit plain Java source for a class.
	 *  @param env    The attribution environment of the outermost class
	 *                containing this class.
	 *  @param cdef   The class definition to be printed.
	 */
	JavaFileObject printSource(Env<AttrContext> env, JCClassDecl cdef) throws IOException {
		JavaFileObject outFile = fileManager.getJavaFileForOutput(CLASS_OUTPUT,
				cdef.sym.flatname.toString(),
				JavaFileObject.Kind.SOURCE3,
				null);
		if (inputFiles.contains(outFile)) {
			log.error(cdef.pos(), "source.cant.overwrite.input.file", outFile);
			return null;
		} else {
			BufferedWriter out = new BufferedWriter(outFile.openWriter());
			try {
				new Pretty(out, true, dumpAnalysis, true).printUnit(env.toplevel, cdef);
				if (verbose) {
					printVerbose("wrote.file", outFile);
				}
			} finally {
				out.close();
			}
			return outFile;
		}
	}

	/** Emit plain Java source for a class.
	 *  @param env    The attribution environment of the outermost class
	 *                containing this class.
	 *  @param cdef   The class definition to be printed.
	 */
	JavaFileObject emitCPP(Env<AttrContext> env, JCClassDecl cdef) throws IOException {

		if ((cdef.sym.flags_field & Flags.NATIVE) != 0) {
			return null;
		}
		if ((cdef.sym.flags_field & Flags.INTERFACE) != 0) {
			return null;
		}

		JavaFileObject outFile = fileManager.getJavaFileForOutput(CLASS_OUTPUT,
				cdef.sym.flatname.toString(),
				JavaFileObject.Kind.CLASS2,
				null);


		if(skipGenerate)
			return outFile;

		if (inputFiles.contains(outFile)) {
			log.error(cdef.pos(), "source.cant.overwrite.input.file", outFile);
			return null;
		} else {
			BufferedWriter out = new BufferedWriter(outFile.openWriter());
			try {
				new LowerTreeImpl(context,out, outGraph, true, false, lineDebugInfo,forceGC).printUnit(env.toplevel, cdef);
				if (verbose) {
					printVerbose("wrote.file", outFile);
				}
			} finally {
				out.close();
				if(outGraph!=null)
					outGraph.close();
			}
			return outFile;
		}
	}

	JavaFileObject emitCPPHeader(Env<AttrContext> env, JCClassDecl cdef) throws IOException {

		if ((cdef.sym.flags_field & Flags.NATIVE) != 0) {
			return null;
		}
		JavaFileObject outFile = fileManager.getJavaFileForOutput(CLASS_OUTPUT,
				cdef.sym.flatname.toString(),
				JavaFileObject.Kind.HEADER,
				null);

		JavaFileObject outGraphFile = null;

		if(depGraph)
			outGraphFile=fileManager.getJavaFileForOutput(CLASS_OUTPUT,
				cdef.sym.flatname.toString()+"_Tasks",
				JavaFileObject.Kind.DEPGRAPH,
				null);

		if(skipGenerate)
			return outFile;

		if (inputFiles.contains(outFile)) {
			log.error(cdef.pos(), "source.cant.overwrite.input.file", outFile);
			return null;
		} else {
			BufferedWriter out = new BufferedWriter(outFile.openWriter());
			if(depGraph)
				 outGraph = new BufferedWriter(outGraphFile.openWriter());
			try {
				new LowerTreeImpl(context,out,outGraph, true, true, lineDebugInfo,forceGC).printUnit(env.toplevel, cdef);
				if (verbose) {
					printVerbose("wrote.file", outFile);
				}
			} finally {
				out.close();
			}
			return outFile;
		}
	}

	JavaFileObject emitExport(Env<AttrContext> env, JCClassDecl cdef) throws IOException {

		if ((cdef.sym.flags_field & Flags.EXPORT) == 0) {
			return null;
		}
		JavaFileObject outFile = fileManager.getJavaFileForOutput(CLASS_OUTPUT,
				cdef.sym.flatname.toString(),
				JavaFileObject.Kind.SOURCE3,
				null);

		if (inputFiles.contains(outFile)) {
			log.error(cdef.pos(), "source.cant.overwrite.input.file", outFile);
			return null;
		} else {
			BufferedWriter out = new BufferedWriter(outFile.openWriter());
			try {
				new EmitImportable(context,out).printUnit(env.toplevel, cdef);
				if (verbose) {
					printVerbose("wrote.file", outFile);
				}
			} finally {
				out.close();
			}
			return outFile;
		}
	}

	/** Emit plain Java source for a class.
	 *  @param env    The attribution environment of the outermost class
	 *                containing this class.
	 *  @param cdef   The class definition to be printed.
	 */
	JavaFileObject genGraph(Env<AttrContext> env, JCClassDecl cdef, Work work) throws IOException {

		if ((cdef.sym.flags_field & Flags.NATIVE) != 0) {
			return null;
		}
		if ((cdef.sym.flags_field & Flags.INTERFACE) != 0) {
			return null;
		}

		JavaFileObject outFile = fileManager.getJavaFileForOutput(CLASS_OUTPUT,
				cdef.sym.flatname.toString(),
				JavaFileObject.Kind.DEPGRAPH,
				null);
		if (inputFiles.contains(outFile)) {
			log.error(cdef.pos(), "source.cant.overwrite.input.file", outFile);
			return null;
		} else {
			BufferedWriter out = new BufferedWriter(outFile.openWriter());
			try {
				//why does allownl make a difference for graph output???
				new Pretty(out, true, true, true, work, dumpAnalysis).printUnit(env.toplevel, cdef);
				if (verbose) {
					printVerbose("wrote.file", outFile);
				}

			} finally {
				out.close();
			}
		}
		return outFile;
	}

	/** Generate code and emit a class file for a given class
	 *  @param env    The attribution environment of the outermost class
	 *                containing this class.
	 *  @param cdef   The class definition from which code is generated.
	 */
	JavaFileObject genCode(Env<AttrContext> env, JCClassDecl cdef) throws IOException {
		try {
			if (gen.genClass(env, cdef)) {
				return writer.writeClass(cdef.sym);
			}
		} catch (ClassWriter.PoolOverflow ex) {
			log.error(cdef.pos(), "limit.pool");
		} catch (ClassWriter.StringOverflow ex) {
			log.error(cdef.pos(), "limit.string.overflow",
					ex.value.substring(0, 20));
		} catch (CompletionFailure ex) {
			chk.completionError(cdef.pos(), ex);
		}
		return null;
	}

	/** Complete compiling a source file that has been accessed
	 *  by the class file reader.
	 *  @param c          The class the source file of which needs to be compiled.
	 *  @param filename   The name of the source file.
	 *  @param f          An input stream that reads the source file.
	 */
	public void complete(ClassSymbol c) throws CompletionFailure {
//      System.err.println("completing " + c);//DEBUG
		if (completionFailureName == c.fullname) {
			throw new CompletionFailure(c, "user-selected completion failure by class name");
		}
		JCCompilationUnit tree;
		JavaFileObject filename = c.classfile;
		JavaFileObject prev = log.useSource(filename);

		try {

			tree = parse(filename, readSource(filename));
		} catch (Exception e) {
			log.error("error.reading.file", filename, e);
			tree = make.TopLevel(List.<JCTree.JCAnnotation>nil(), null, List.<JCTree>nil());
		} finally {
			log.useSource(prev);
		}

		if (taskListener != null) {
			TaskEvent e = new TaskEvent(TaskEvent.Kind.ENTER, tree);
			taskListener.started(e);
		}

		enter.complete(List.of(tree), c);

		if (taskListener != null) {
			TaskEvent e = new TaskEvent(TaskEvent.Kind.ENTER, tree);
			taskListener.finished(e);
		}

		if (enter.getEnv(c) == null) {
			boolean isPkgInfo =
					tree.sourcefile.isNameCompatible("package-info",
					JavaFileObject.Kind.SOURCE)
					|| tree.sourcefile.isNameCompatible("package-info",
					JavaFileObject.Kind.SOURCE2)|| tree.sourcefile.isNameCompatible("package-info",
					JavaFileObject.Kind.SOURCE3);
			if (isPkgInfo) {
				if (enter.getEnv(tree.packge) == null) {
					JCDiagnostic diag =
							diagFactory.fragment("file.does.not.contain.package",
							c.location());
					throw reader.new BadClassFile(c, filename, diag);
				}
			} else {
				JCDiagnostic diag =
						diagFactory.fragment("file.doesnt.contain.class",
						c.getQualifiedName());
				throw reader.new BadClassFile(c, filename, diag);
			}
		}

		implicitSourceFilesRead = true;
	}

	public void complete(DomainSymbol c) throws CompletionFailure {
//      System.err.println("completing " + c);//DEBUG
		if (completionFailureName == c.fullname) {
			throw new CompletionFailure(c, "user-selected completion failure by class name");
		}
		JCCompilationUnit tree;
		JavaFileObject filename = c.classfile;
		JavaFileObject prev = log.useSource(filename);

		try {
			tree = parse(filename, filename.getCharContent(false));
		} catch (IOException e) {
			log.error("error.reading.file", filename, e);
			tree = make.TopLevel(List.<JCTree.JCAnnotation>nil(), null, List.<JCTree>nil());
		} finally {
			log.useSource(prev);
		}

		if (taskListener != null) {
			TaskEvent e = new TaskEvent(TaskEvent.Kind.ENTER, tree);
			taskListener.started(e);
		}

		enter.complete(List.of(tree), c);

		if (taskListener != null) {
			TaskEvent e = new TaskEvent(TaskEvent.Kind.ENTER, tree);
			taskListener.finished(e);
		}

		if (enter.getEnv(c) == null) {
			boolean isPkgInfo =
					tree.sourcefile.isNameCompatible("package-info",
					JavaFileObject.Kind.SOURCE)
					|| tree.sourcefile.isNameCompatible("package-info",
					JavaFileObject.Kind.SOURCE2)|| tree.sourcefile.isNameCompatible("package-info",
					JavaFileObject.Kind.SOURCE3);
			if (isPkgInfo) {
				if (enter.getEnv(tree.packge) == null) {
					JCDiagnostic diag =
							diagFactory.fragment("file.does.not.contain.package",
							c.location());
					throw reader.new BadClassFile(c, filename, diag);
				}
			} else {
				JCDiagnostic diag =
						diagFactory.fragment("file.doesnt.contain.class",
						c.getQualifiedName());
				throw reader.new BadClassFile(c, filename, diag);
			}
		}

		implicitSourceFilesRead = true;
	}
	/** Track when the JavaCompiler has been used to compile something. */
	private boolean hasBeenUsed = false;
	private long start_msec = 0;
	public long elapsed_msec = 0;
	/** Track whether any errors occurred while parsing source text. */
	private boolean parseErrors = false;

	public void compile(List<JavaFileObject> sourceFileObject)
			throws Throwable {
		compile(sourceFileObject, List.<String>nil(), null);
	}

	/**
	 * Main method: compile a list of files, return all compiled classes
	 *
	 * @param sourceFileObjects file objects to be compiled
	 * @param classnames class names to process for annotations
	 * @param processors user provided annotation processors to bypass
	 * discovery, {@code null} means that no processors were provided
	 */
	public void compile(List<JavaFileObject> sourceFileObjects,
			List<String> classnames,
			Iterable<? extends Processor> processors)
			throws IOException // TODO: temp, from JavacProcessingEnvironment
	{

		if (processors != null && processors.iterator().hasNext()) {
			explicitAnnotationProcessingRequested = true;
		}
		// as a JavaCompiler can only be used once, throw an exception if
		// it has been used before.
		if (hasBeenUsed) {
			throw new AssertionError("attempt to reuse JavaCompiler");
		}
		hasBeenUsed = true;

		start_msec = now();
		try {
			initProcessAnnotations(processors);
			String funky_path=System.getenv().get("FUNKY_LIB_PATH");
			//funky_path = com.sun.tools.javac.Main.FUNKY_LIB_PATH;
            if(funky_path==null) {
                throw new RuntimeException("env var FUNKY_LIB_PATH not set");
            }
			File file = new File(funky_path+"/lib/libibarvinok.so");
			File osxLib = new File(funky_path+"/lib/libibarvinok.dylib");
                        if(!file.exists() && !osxLib.exists())
				throw new RuntimeException(funky_path+"/lib/libibarvinok.so does not exist");

			//make sure that FUNKY_LIB_PATH/lib is in LD_LIBRARY_PATH
//			System.getenv().put("LD_LIBRARY_PATH",funky_path+"/lib:"+System.getenv().get("LD_LIBRARY_PATH"));
//			System.getenv().put("LIBRARY_PATH",funky_path+"/lib:"+System.getenv().get("LIBRARY_PATH"));

			ibarvinok.init(log,dump_barvinok);

			//register one_d
			((DomainType)syms.one_d.type).registerBarvinok();

			// These method calls must be chained to avoid memory leaks
			delegateCompiler = processAnnotations(enterTrees(stopIfError(parseFiles(sourceFileObjects))),
					classnames);

			delegateCompiler.compile2();
			delegateCompiler.close();

			ibarvinok.shutdown();

			elapsed_msec = delegateCompiler.elapsed_msec;
		} catch (Abort ex) {
			if (devVerbose) {
				ex.printStackTrace();
			}
		}

	}

	/**
	 * The phases following annotation processing: attribution,
	 * desugar, and finally code generation.
	 */
	private void compile2() {
		try {
			switch (compilePolicy) {
/*
				case ATTR_ONLY:
					attribute(todo);
					break;

				case CHECK_ONLY:
					flow(attribute(todo));
					break;

				case SIMPLE:
					generate(desugar(flow(attribute(todo))));
					break;

				case BY_FILE: {
					Queue<Queue<Env<AttrContext>>> q = todo.groupByFile();
					while (!q.isEmpty() && errorCount() == 0) {
						generate(desugar(flow(attribute(q.remove()))));
					}
				}
				break;
*/
				case BY_TODO:

					//ALEX: apply passes i to all classes before applying pass i+1 on any class!

					//this is strictly necessary, because pass i+1 of class j may depend on pass i of class k!=j

					java.util.ArrayList<Env<AttrContext>> phase = new java.util.ArrayList<Env<AttrContext>>();

					for (Env<AttrContext> env : todo) {
						phase.add(env);
					}

					//convert some special syntax constructs into easier to handle syntax (a>>>b() and a~b~c)
					for (int i = 0; i < phase.size(); i++) {
						phase.set(i, desugarSyntax(phase.get(i)));
					}

					attr.before_typecheck = false;
//					for(Env<AttrContext> res:attr.templatesInstances)
//						res.enclClass.sym.complete();

					//type check
					for (int i = 0; i < phase.size(); i++) {
						phase.set(i, attribute(phase.get(i)));
					}

					//some desugaring (class op class -> class.operatorop(class)) must happen after type check
					for (int i = 0; i < phase.size(); i++) {
						phase.set(i, desugarPostAttr(phase.get(i)));
					}

					for(Env<AttrContext> res:attr.templatesInstances)
						if(res.tree.getTag()!=JCTree.CLASSDEF||((JCClassDecl)res.tree).typarams.size()==0)
							desugarPostAttr(res);

					java.util.ArrayList<Queue<Env<AttrContext>>> phase_two = new java.util.ArrayList<Queue<Env<AttrContext>>>();


					//flow analysis + (per method) task graph construction
					for (int i = 0; i < phase.size(); i++) {

						//discard templates
						Env<AttrContext> res=phase.get(i);

						if(res.tree.getTag()!=JCTree.CLASSDEF||((JCClassDecl)res.tree).typarams.size()==0)
						{
							phase_two.add(flow(res));
							attr.templatesInstances.remove(res);
						}
					}

					//add template instances to processing chain
					for(Env<AttrContext> res:attr.templatesInstances)
						if(res.tree.getTag()!=JCTree.CLASSDEF||((JCClassDecl)res.tree).typarams.size()==0)
							phase_two.add(flow(res));

					//some vars uniquely alias others, find em
					for (int i = 0; i < phase_two.size(); i++) {
						phase_two.set(i, alias(phase_two.get(i)));
					}

					//this pass calcs call graph in callGraph for the program
					for (int i = 0; i < phase_two.size(); i++) {
						phase_two.set(i, refgen(phase_two.get(i)));
					}

					//find loops in call graph:
					for (Iterator<JCTree> i = new BreadthFirstIterator<JCTree, Arc>(callGraph); i.hasNext();) {
						JCTree node = i.next();

						//assign unique topol ordered id for shortest paths
						topolNodes.put(node,topolNodes.size());
					}

					for(JCTree md:callGraph.vertexSet())
					{
						if(md.getDGNode().IsReachable(false, md, topolNodes, callGraph,false))
						{
							((JCMethodDecl)md).sym.mayBeRecursive=true;

							//ALEX: is this wise?
							//we don't know if a recurusive method will terminate, by seperating it from all other code we guarantee progress
							//unless method is marked as nonblocking
							if((((JCMethodDecl)md).sym.flags_field&Flags.LOOP)!=0&&(((JCMethodDecl)md).sym.flags_field&Flags.NONBLOCKING)==0)
								((JCMethodDecl)md).sym.flags_field|=Flags.BLOCKING;
						}
					}

                                        //type check that vars marked as unique/linear are actually linear
					for (int i = 0; i < phase_two.size(); i++) {
						phase_two.set(i, linear(phase_two.get(i)));
					}

					//with linearity info we can calc aliases, but they are not inter procedural
					for (int i = 0; i < phase_two.size(); i++) {
						phase_two.set(i, aliasGlobal(phase_two.get(i)));
					}

					work.fixEstimates();//aliasGlobal calcs work/mem estimates pass 2 (alias calcs pass 1)
					//after aliasGlobal is finished, only the cached estimates should be used
					java.util.ArrayList<Queue<Pair<Env<AttrContext>, JCClassDecl>>> phase_three = new java.util.ArrayList<Queue<Pair<Env<AttrContext>, JCClassDecl>>>();

					for (int i = 0; i < phase_two.size(); i++) {
						phase_three.add(desugar(phase_two.get(i)));
					}

					for (int i = 0; i < phase_three.size(); i++) {
						phase_three.set(i,taskgen(phase_three.get(i)));
					}


					//old javac pass..does some stuff we might need?

                                        /**
                                         * Andreas Wagner: Calculate which parts of a recursion could be reused
                                         */

                                        if(supportMPI == true && optimizeRecursion == true){
                                            RecDataAnalyzerOrig analyzer = RecDataAnalyzerOrig.instance(context);
                                            RecDataAnalyzerClone cloneanalyzer = RecDataAnalyzerClone.instance(context);

                                            for(JCTree md: callGraph.vertexSet()){
                                                if(((JCMethodDecl)md).sym.mayBeRecursive){
                                                    JCMethodDecl m = (JCMethodDecl) md;
                                                    //prepare mapping

                                                    for(iTask tmp : m.hasseFinal.vertexSet()){
                                                        if(taskGen.getUnoptimizedToOptimized().get(tmp) == null)
                                                            taskGen.getUnoptimizedToOptimized().put(tmp, tmp);
                                                    }

                                                    //analyzer.analyzeTree(m.cloneForPathGen(m.name, new TreeCopier(make)));
                                                    analyzer.analyzeTree(m.cloneForPathGen(m.name, new TreeCopier(make)));
                                                    cloneanalyzer.analyzeTree(m.cloneForPathGen(m.name, new TreeCopier(make)));
                                                    m.paramExpressions = new LinkedHashMap<iTask, JCExpression>();
                                                    TaskGraphMerger tgm = new TaskGraphMerger(analyzer, cloneanalyzer, make);
                                                    SimpleDirectedGraph<TaskSet, TaskArc> tg = tgm.merge();
                                                    Scheduler s = new Scheduler(context);
                                                    s.scheduleWithComCosts(analyzer.getMethodDeclaration(), tg);
                                                    //Get tasks from analysis phase
                                                    Map<TaskSet,TaskSet> original = analyzer.originalTaskMapping;
                                                    Map<TaskSet,TaskSet> clone = cloneanalyzer.originalTaskMapping;
                                                    //Map tasks
                                                    Map<TaskSet,TaskSet> cloneToclone = new LinkedHashMap<TaskSet, TaskSet>();
                                                    //reverse mapping for Tasks
                                                    Map<TaskSet,TaskSet> cloneToOriginal = new LinkedHashMap<TaskSet, TaskSet>();
                                                    for(TaskSet ts : original.keySet()){
                                                        TaskSet clone1 = original.get(ts);
                                                        TaskSet clone2 = clone.get(ts);
                                                        cloneToclone.put(clone1, clone2);
                                                        cloneToOriginal.put(clone1, ts);
                                                    }
                                                    Schedule schedule = s.getSchedule();
                                                    //check if taskset of original is scheduled on same processor as taskset of clone
                                                    for(TaskSet clone1 : cloneToclone.keySet()){
                                                        TaskSet clone2 = cloneToclone.get(clone1);
                                                        if(schedule.getProcForTask(clone1) != null &&
                                                                schedule.getProcForTask(clone2) != null &&
                                                                schedule.getProcForTask(clone1) == schedule.getProcForTask(clone2)){
                                                            //task input variables might be reused
                                                            iTask t1 = taskGen.getUnoptimizedToOptimized().get(cloneToOriginal.get(clone1));
                                                            Set<VarSymbol> vs = t1.getOutSymbols();
                                                            m.reuseableVars.put(t1, vs);
                                                            //decide which task generate input for method application
                                                            Map<iTask, Integer> inputDeps = tgm.getParamExprInputDeps();
                                                            for(iTask t : inputDeps.keySet()){
                                                                int idx = inputDeps.get(t);
                                                                iTask optimizedTask = taskGen.getUnoptimizedToOptimized().get(cloneToOriginal.get(t));
                                                                m.taskDepForParamIndex.put(optimizedTask, idx);
                                                                m.paramExpressions.put(optimizedTask, analyzer.getArgumentExpressionMapping().get(idx));
                                                            }
                                                        }
                                                    }

                                                }
                                            }
                                        }

					Queue<JavaFileObject> results = new java.util.LinkedList<JavaFileObject>();

					//code generation (emit C++ or other stuff)
					for (int i = 0; i < phase_three.size(); i++) {
						generate(phase_three.get(i), results);
					}

					//run some compiler on the emitted code (g++-4.7 or icpc)
					if (cc != null && !results.isEmpty() && log.nerrors == 0) {
						long msec = now();

						//if (verbose) {
							printVerbose("cc.started",null);
						//}

						String files = new String();
						for (JavaFileObject f : results) {
							files += f.toString() + " ";
						}
						String cmd = cc;
						while (cmd.contains("{files}")) {
							cmd = cmd.replace("{files}", files);
						}
						try {
							String funky_path=System.getenv().get("FUNKY_LIB_PATH");
							if(funky_path==null)
								throw new RuntimeException("env var FUNKY_LIB_PATH not set");

							File file = new File(funky_path+"/lib/libFUNKY.a");
							if(!file.exists())
								throw new RuntimeException(funky_path+"/lib/libFUNKY.a does not exist, is FUNKY_LIB_PATH set to the install dir?");

							Process p = Runtime.getRuntime().exec(cmd, null);
							//Process p=Runtime.getRuntime().exec("/bin/bash ");
							BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
							String line = reader.readLine();
							while (line != null) {
								//line.matches(line)
								//line = line.replace("(", ":");
								//line = line.replace(")", ": ");
								System.err.println(line);
								line = reader.readLine();
							}

							reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
							line = reader.readLine();
//							if(error_count>0)
							while (line != null) {
								System.out.println(line);
								line = reader.readLine();
							}

							int error_count = p.waitFor();
							if (error_count > 0) {
								log.error("cc.err", cmd);
							}
/*
							JavaFileObject outFile = fileManager.getJavaFileForOutput(CLASS_OUTPUT,
									"Makefile",
									JavaFileObject.Kind.MAKEFILE,
									null);

							BufferedWriter out = new BufferedWriter(outFile.openWriter());
							try {
								out.write("# funky imp generated makefile\n");
								out.write("build:\n\t"+cmd);
							} finally {
								out.close();
							}
*/

						} catch (IOException e1) {
							String reason = e1.toString();
							log.error("cc.fail", cmd, reason);
						} catch (InterruptedException e2) {
						} finally {
							ccTime = elapsed(msec);
							//if (verbose) {
								printVerbose("cc.done",Long.toString(ccTime));
							//}

						}


					}



					/*
					while (!todo.isEmpty())
					generate(
					desugar(
					refgen(
					pathgen(
					aliasGlobal(
					linear(
					alias(
					flow(
					attribute(
					desugarSyntax(
					todo.remove()
					))))))))));
					 *
					 */
					break;



				default:
					assert false : "unknown compile policy";
			}
		} catch (Abort ex) {
			if (devVerbose) {
				ex.printStackTrace();
			}
		}

		if (verbose) {
			elapsed_msec = elapsed(start_msec);
			printVerbose("total", Long.toString(elapsed_msec-ccTime));
		}

		reportDeferredDiagnostics();


		if (!log.hasDiagnosticListener()) {
			printCount("error", errorCount());
			printCount("warn", warningCount());
		}
	}
	private List<JCClassDecl> rootClasses;

	/**
	 * Parses a list of files.
	 */
	public List<JCCompilationUnit> parseFiles(List<JavaFileObject> fileObjects) throws IOException {
		if (errorCount() > 0) {
			return List.nil();
		}

		//parse all files
		ListBuffer<JCCompilationUnit> trees = lb();
		for (JavaFileObject fileObject : fileObjects) {
			trees.append(parse(fileObject));
		}
		return trees.toList();
	}

	/**
	 * Enter the symbols found in a list of parse trees.
	 * As a side-effect, this puts elements on the "todo" list.
	 * Also stores a list of all top level classes in rootClasses.
	 */
	public List<JCCompilationUnit> enterTrees(List<JCCompilationUnit> roots) {
		//enter symbols for all files
		if (taskListener != null) {
			for (JCCompilationUnit unit : roots) {
				TaskEvent e = new TaskEvent(TaskEvent.Kind.ENTER, unit);
				taskListener.started(e);
			}
		}

		enter.main(roots);

		if (taskListener != null) {
			for (JCCompilationUnit unit : roots) {
				TaskEvent e = new TaskEvent(TaskEvent.Kind.ENTER, unit);
				taskListener.finished(e);
			}
		}

		//If generating source, remember the classes declared in
		//the original compilation units listed on the command line.
		if (sourceOutput || stubOutput) {
			ListBuffer<JCClassDecl> cdefs = lb();
			for (JCCompilationUnit unit : roots) {
				for (List<JCTree> defs = unit.defs;
						defs.nonEmpty();
						defs = defs.tail) {
					if (defs.head instanceof JCClassDecl) {
						cdefs.append((JCClassDecl) defs.head);
					}
				}
			}
			rootClasses = cdefs.toList();
		}
		return roots;
	}
	/**
	 * Set to true to enable skeleton annotation processing code.
	 * Currently, we assume this variable will be replaced more
	 * advanced logic to figure out if annotation processing is
	 * needed.
	 */
	boolean processAnnotations = false;
	/**
	 * Object to handle annotation processing.
	 */
	JavacProcessingEnvironment procEnvImpl = null;


	/**
	 * Check if we should process annotations.
	 * If so, and if no scanner is yet registered, then set up the DocCommentScanner
	 * to catch doc comments, and set keepComments so the parser records them in
	 * the compilation unit.
	 *
	 * @param processors user provided annotation processors to bypass
	 * discovery, {@code null} means that no processors were provided
	 */
	public void initProcessAnnotations(Iterable<? extends Processor> processors) {
		// Process annotations if processing is not disabled and there
		// is at least one Processor available.
		Options options = Options.instance(context);
		if (options.get("-proc:none") != null) {
			processAnnotations = false;
		} else if (procEnvImpl == null) {
			procEnvImpl = new JavacProcessingEnvironment(context, processors);
			processAnnotations = procEnvImpl.atLeastOneProcessor();

			if (processAnnotations) {
				if (context.get(Scanner.Factory.scannerFactoryKey) == null) {
					DocCommentScanner.Factory.preRegister(context);
				}
				options.put("save-parameter-names", "save-parameter-names");
				reader.saveParameterNames = true;
				keepComments = true;
				if (taskListener != null) {
					taskListener.started(new TaskEvent(TaskEvent.Kind.ANNOTATION_PROCESSING));
				}


			} else { // free resources
				procEnvImpl.close();
			}
		}
	}

	// TODO: called by JavacTaskImpl
	public JavaCompiler processAnnotations(List<JCCompilationUnit> roots) throws IOException {
		return processAnnotations(roots, List.<String>nil());
	}

	/**
	 * Process any anotations found in the specifed compilation units.
	 * @param roots a list of compilation units
	 * @return an instance of the compiler in which to complete the compilation
	 */
	public JavaCompiler processAnnotations(List<JCCompilationUnit> roots,
			List<String> classnames)
			throws IOException { // TODO: see TEMP note in JavacProcessingEnvironment
		if (errorCount() != 0) {
			// Errors were encountered.  If todo is empty, then the
			// encountered errors were parse errors.  Otherwise, the
			// errors were found during the enter phase which should
			// be ignored when processing annotations.

			if (todo.isEmpty()) {
				return this;
			}
		}

		// ASSERT: processAnnotations and procEnvImpl should have been set up by
		// by initProcessAnnotations

		// NOTE: The !classnames.isEmpty() checks should be refactored to Main.

		if (!processAnnotations) {
			// If there are no annotation processors present, and
			// annotation processing is to occur with compilation,
			// emit a warning.
			Options options = Options.instance(context);
			if (options.get("-proc:only") != null) {
				log.warning("proc.proc-only.requested.no.procs");
				todo.clear();
			}
			// If not processing annotations, classnames must be empty
			if (!classnames.isEmpty()) {
				log.error("proc.no.explicit.annotation.processing.requested",
						classnames);
			}
			return this; // continue regular compilation
		}

		try {
			List<ClassSymbol> classSymbols = List.nil();
			List<PackageSymbol> pckSymbols = List.nil();
			if (!classnames.isEmpty()) {
				// Check for explicit request for annotation
				// processing
				if (!explicitAnnotationProcessingRequested()) {
					log.error("proc.no.explicit.annotation.processing.requested",
							classnames);
					return this; // TODO: Will this halt compilation?
				} else {
					boolean errors = false;
					for (String nameStr : classnames) {
						Symbol sym = resolveIdent(nameStr);
						if (sym == null || (sym.kind == Kinds.PCK && !processPcks)) {
							log.error("proc.cant.find.class", nameStr);
							errors = true;
							continue;
						}
						try {
							if (sym.kind == Kinds.PCK) {
								sym.complete();
							}
							if (sym.exists()) {
								Name name = names.fromString(nameStr);
								if (sym.kind == Kinds.PCK) {
									pckSymbols = pckSymbols.prepend((PackageSymbol) sym);
								} else {
									classSymbols = classSymbols.prepend((ClassSymbol) sym);
								}
								continue;
							}
							assert sym.kind == Kinds.PCK;
							log.warning("proc.package.does.not.exist", nameStr);
							pckSymbols = pckSymbols.prepend((PackageSymbol) sym);
						} catch (CompletionFailure e) {
							log.error("proc.cant.find.class", nameStr);
							errors = true;
							continue;
						}
					}
					if (errors) {
						return this;
					}
				}
			}
			JavaCompiler c = procEnvImpl.doProcessing(context, roots, classSymbols, pckSymbols);
			if (c != this) {
				annotationProcessingOccurred = c.annotationProcessingOccurred = true;
			}
			return c;
		} catch (CompletionFailure ex) {
			log.error("cant.access", ex.sym, ex.getDetailValue());
			return this;

		}
	}

	boolean explicitAnnotationProcessingRequested() {
		Options options = Options.instance(context);
		return explicitAnnotationProcessingRequested
				|| options.get("-processor") != null
				|| options.get("-processorpath") != null
				|| options.get("-proc:only") != null
				|| options.get("-Xprint") != null;
	}

	/**
	 * Attribute a list of parse trees, such as found on the "todo" list.
	 * Note that attributing classes may cause additional files to be
	 * parsed and entered via the SourceCompleter.
	 * Attribution of the entries in the list does not stop if any errors occur.
	 * @returns a list of environments for attributd classes.
	 */
	public Queue<Env<AttrContext>> attribute(Queue<Env<AttrContext>> envs) {
		ListBuffer<Env<AttrContext>> results = lb();
		while (!envs.isEmpty()) {
			results.append(attribute(envs.remove()));
		}
		return results;
	}

	/**
	 * Attribute a parse tree.
	 * @returns the attributed parse tree
	 */
	public Env<AttrContext> attribute(Env<AttrContext> env) {
		if (compileStates.isDone(env, CompileState.ATTR)) {
			return env;
		}

		if (verboseCompilePolicy) {
			Log.printLines(log.noticeWriter, "[attribute " + env.enclClass.sym + "]");
		}
		if (verbose) {
			if(env.tree.getTag()==JCTree.DOMDEF)
				printVerbose("checking.attribution", ((JCDomainDecl)env.tree).sym);
			else
				printVerbose("checking.attribution", env.enclClass.sym);
		}

		if (taskListener != null) {
			TaskEvent e = new TaskEvent(TaskEvent.Kind.ANALYZE, env.toplevel, env.enclClass.sym);
			taskListener.started(e);
		}

		JavaFileObject prev = log.useSource(
				env.enclClass.sym.sourcefile != null
				? env.enclClass.sym.sourcefile
				: env.toplevel.sourcefile);
		try {
			if (env.tree != null) {
				if(env.tree.getTag()==JCTree.DOMDEF)
					attr.attribDomain(env.tree.pos(), ((JCDomainDecl)env.tree).sym);
				else
					attr.attribClass(env.tree.pos(), env.enclClass.sym);
			}

			compileStates.put(env, CompileState.ATTR);
		} finally {
			log.useSource(prev);
		}

		return env;
	}

	/**
	 * Perform dataflow checks on attributed parse trees.
	 * These include checks for definite assignment and unreachable statements.
	 * If any errors occur, an empty list will be returned.
	 * @returns the list of attributed parse trees
	 */
	public Queue<Env<AttrContext>> flow(Queue<Env<AttrContext>> envs) {
		ListBuffer<Env<AttrContext>> results = lb();
		for (Env<AttrContext> env : envs) {
			flow(env, results);
		}
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	public Queue<Env<AttrContext>> flow(Env<AttrContext> env) {
		ListBuffer<Env<AttrContext>> results = lb();
		flow(env, results);
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	protected void flow(Env<AttrContext> env, Queue<Env<AttrContext>> results) {
		try {
			if (errorCount() > 0) {
				return;
			}

			if (relax || compileStates.isDone(env, CompileState.FLOW)) {
				results.add(env);
				return;
			}

			if (verboseCompilePolicy) {
				Log.printLines(log.noticeWriter, "[flow " + env.enclClass.sym + "]");
			}
			JavaFileObject prev = log.useSource(
					env.enclClass.sym.sourcefile != null
					? env.enclClass.sym.sourcefile
					: env.toplevel.sourcefile);
			try {
				make.at(Position.FIRSTPOS);
				TreeMaker localMake = make.forToplevel(env.toplevel);
				flow.analyzeTree(env.tree, localMake);
				compileStates.put(env, CompileState.FLOW);

				if (errorCount() > 0) {
					return;
				}

				results.add(env);
			} finally {
				log.useSource(prev);
			}
		} finally {
			if (taskListener != null) {
				TaskEvent e = new TaskEvent(TaskEvent.Kind.ANALYZE, env.toplevel, env.enclClass.sym);
				taskListener.finished(e);
			}
		}
	}

	/**
	 * Prepare attributed parse trees, in conjunction with their attribution contexts,
	 * for source or code generation.
	 * If any errors occur, an empty list will be returned.
	 * @returns a list containing the classes to be generated
	 */
	public Queue<Env<AttrContext>> desugarSyntax(Queue<Env<AttrContext>> envs) {
		ListBuffer<Env<AttrContext>> results = lb();
		while (!envs.isEmpty()) {
			results.append(desugarSyntax(envs.remove()));
		}
		return results;
	}

	/**
	 * Prepare attributed parse trees, in conjunction with their attribution contexts,
	 * for source or code generation. If the file was not listed on the command line,
	 * the current implicitSourcePolicy is taken into account.
	 * The preparation stops as soon as an error is found.
	 */
	protected Env<AttrContext> desugarSyntax(final Env<AttrContext> env) {
//        if (errorCount() > 0)
//            return env;

		if (implicitSourcePolicy == ImplicitSourcePolicy.NONE
				&& !inputFiles.contains(env.toplevel.sourcefile)) {
			return env;
		}


		if (verboseCompilePolicy) {
			Log.printLines(log.noticeWriter, "[desugarSyntax " + env.enclClass.sym + "]");
		}

		JavaFileObject prev = log.useSource((env.enclClass != null && env.enclClass.sym.sourcefile != null)
				? env.enclClass.sym.sourcefile
				: env.toplevel.sourcefile);
		try {
			//save tree prior to rewriting
			JCTree untranslated = env.tree;

			make.at(Position.FIRSTPOS);
			TreeMaker localMake = make.forToplevel(env.toplevel);

			env.tree = desugarSyntax.translateTopLevelClass(env, env.tree, localMake);

			if (errorCount() != 0) {
				return env;
			}

		} finally {
			log.useSource(prev);
		}
		return env;
	}

	/**
	 * Prepare attributed parse trees, in conjunction with their attribution contexts,
	 * for source or code generation.
	 * If any errors occur, an empty list will be returned.
	 * @returns a list containing the classes to be generated
	 */
	public Queue<Env<AttrContext>> desugarPostAttr(Queue<Env<AttrContext>> envs) {
		ListBuffer<Env<AttrContext>> results = lb();
		while (!envs.isEmpty()) {
			results.append(desugarPostAttr(envs.remove()));
		}
		return results;
	}

	/**
	 * Prepare attributed parse trees, in conjunction with their attribution contexts,
	 * for source or code generation. If the file was not listed on the command line,
	 * the current implicitSourcePolicy is taken into account.
	 * The preparation stops as soon as an error is found.
	 */
	protected Env<AttrContext> desugarPostAttr(final Env<AttrContext> env) {
//        if (errorCount() > 0)
//            return env;

		if (implicitSourcePolicy == ImplicitSourcePolicy.NONE
				&& !inputFiles.contains(env.toplevel.sourcefile)) {
			return env;
		}


		if (verboseCompilePolicy) {
			Log.printLines(log.noticeWriter, "[desugarSyntax " + env.enclClass.sym + "]");
		}

		JavaFileObject prev = log.useSource((env.enclClass != null && env.enclClass.sym.sourcefile != null)
				? env.enclClass.sym.sourcefile
				: env.toplevel.sourcefile);
		try {
			//save tree prior to rewriting
			JCTree untranslated = env.tree;

			make.at(Position.FIRSTPOS);
			TreeMaker localMake = make.forToplevel(env.toplevel);

			env.tree = desugarPostAttr.translateTopLevelClass(env, env.tree, localMake);

			if (errorCount() != 0) {
				return env;
			}

		} finally {
			log.useSource(prev);
		}
		return env;
	}


	/**
	 * Prepare attributed parse trees, in conjunction with their attribution contexts,
	 * for source or code generation.
	 * If any errors occur, an empty list will be returned.
	 * @returns a list containing the classes to be generated
	 */
	public Queue<Pair<Env<AttrContext>, JCClassDecl>> desugar(Queue<Env<AttrContext>> envs) {
		ListBuffer<Pair<Env<AttrContext>, JCClassDecl>> results = lb();
		for (Env<AttrContext> env : envs) {
			desugar(env, results);
		}
		return stopIfError(results);
	}

	/**
	 * Prepare attributed parse trees, in conjunction with their attribution contexts,
	 * for source or code generation. If the file was not listed on the command line,
	 * the current implicitSourcePolicy is taken into account.
	 * The preparation stops as soon as an error is found.
	 */
	protected void desugar(final Env<AttrContext> env, Queue<Pair<Env<AttrContext>, JCClassDecl>> results) {
		if (false&&errorCount() > 0) {
			return;
		}

		if (implicitSourcePolicy == ImplicitSourcePolicy.NONE
				&& !inputFiles.contains(env.toplevel.sourcefile)) {
			return;


		}

		/**
		 * As erasure (TransTypes) destroys information needed in flow analysis,
		 * including information in supertypes, we need to ensure that supertypes
		 * are processed through attribute and flow before subtypes are translated.
		 */
		class ScanNested extends TreeScanner {

			Set<Env<AttrContext>> dependencies = new LinkedHashSet<Env<AttrContext>>();

			public void visitClassDef(JCClassDecl node) {
				Type st = types.supertype(node.sym.type);
				if (st.tag == TypeTags.CLASS) {
					ClassSymbol c = st.tsym.outermostClass();
					Env<AttrContext> stEnv = enter.getEnv(c);
					if (stEnv != null && env != stEnv) {
						if (dependencies.add(stEnv)) {
							scan(stEnv.tree);
						}
					}
				}
				super.visitClassDef(node);
			}
		}
		ScanNested scanner = new ScanNested();
		scanner.scan(env.tree);
		/*
		for (Env<AttrContext> dep : scanner.dependencies) {
			if (!compileStates.isDone(dep, CompileState.FLOW)) {
				flow(attribute(dep));
			}
		}
		*/

		//We need to check for error another time as more classes might
		//have been attributed and analyzed at this stage
		if (errorCount() > 0) {
			return;
		}

		if (verboseCompilePolicy) {
			Log.printLines(log.noticeWriter, "[desugar " + env.enclClass.sym + "]");
		}

		JavaFileObject prev = log.useSource(env.enclClass.sym.sourcefile != null
				? env.enclClass.sym.sourcefile
				: env.toplevel.sourcefile);
		try {
			//save tree prior to rewriting
			JCTree untranslated = env.tree;

			make.at(Position.FIRSTPOS);
			TreeMaker localMake = make.forToplevel(env.toplevel);

			if (env.tree instanceof JCCompilationUnit) {
				if (!(stubOutput || sourceOutput || printFlat)) {
					List<JCTree> pdef = lower.translateTopLevelClass(env, env.tree, localMake);
					if (pdef.head != null) {
						assert pdef.tail.isEmpty();
						results.add(new Pair<Env<AttrContext>, JCClassDecl>(env, (JCClassDecl) pdef.head));
					}
				}
				return;
			}

			if (stubOutput) {
				//emit stub Java source file, only for compilation
				//units enumerated explicitly on the command line
				JCClassDecl cdef = (JCClassDecl) env.tree;
				if (untranslated instanceof JCClassDecl
						&& rootClasses.contains((JCClassDecl) untranslated)
						&& ((cdef.mods.flags & (Flags.PROTECTED | Flags.PUBLIC)) != 0
						|| cdef.sym.packge().getQualifiedName() == names.java_lang)) {
					results.add(new Pair<Env<AttrContext>, JCClassDecl>(env, removeMethodBodies(cdef)));
				}
				return;
			}

			env.tree = transTypes.translateTopLevelClass(env.tree, localMake);

			if (errorCount() != 0) {
				return;
			}

			if (sourceOutput) {
				//emit standard Java source file, only for compilation
				//units enumerated explicitly on the command line
				JCClassDecl cdef = (JCClassDecl) env.tree;
				if (untranslated instanceof JCClassDecl
						&& rootClasses.contains((JCClassDecl) untranslated)) {
					results.add(new Pair<Env<AttrContext>, JCClassDecl>(env, cdef));
				}
				return;
			}

			//translate out inner classes
			List<JCTree> cdefs = lower.translateTopLevelClass(env, env.tree, localMake);

			if (errorCount() != 0) {
				return;
			}

			//generate code for each class
			for (List<JCTree> l = cdefs; l.nonEmpty(); l = l.tail) {
				JCClassDecl cdef = (JCClassDecl) l.head;
				results.add(new Pair<Env<AttrContext>, JCClassDecl>(env, cdef));
			}
		} finally {
			log.useSource(prev);
		}

	}

	/**
	 * Perform dataflow checks on attributed parse trees.
	 * These include checks for definite assignment and unreachable statements.
	 * If any errors occur, an empty list will be returned.
	 * @returns the list of attributed parse trees
	 */
	public Queue<Env<AttrContext>> alias(Queue<Env<AttrContext>> envs) {
		ListBuffer<Env<AttrContext>> results = lb();
		for (Env<AttrContext> env : envs) {
			alias(env, results);
		}
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	public Queue<Env<AttrContext>> alias(Env<AttrContext> env) {
		ListBuffer<Env<AttrContext>> results = lb();
		alias(env, results);
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	protected void alias(Env<AttrContext> env, Queue<Env<AttrContext>> results) {
		try {
			if (verboseCompilePolicy) {
				Log.printLines(log.noticeWriter, "[alies " + env.enclClass.sym + "]");
			}
			JavaFileObject prev = log.useSource(
					env.enclClass.sym.sourcefile != null
					? env.enclClass.sym.sourcefile
					: env.toplevel.sourcefile);
			try {
				make.at(Position.FIRSTPOS);
				TreeMaker localMake = make.forToplevel(env.toplevel);

				alias.analyzeTree(env.tree, localMake, compileStates.get(env) == CompileState.LINEAR);

				//compileStates.put(env, CompileState.FLOW);

				if (errorCount() > 0) {
					return;
				}

				results.add(env);
			} finally {
				log.useSource(prev);
			}
		} finally {
			if (taskListener != null) {
				TaskEvent e = new TaskEvent(TaskEvent.Kind.ANALYZE, env.toplevel, env.enclClass.sym);
				taskListener.finished(e);
			}
		}
	}

	/**
	 * Perform dataflow checks on attributed parse trees.
	 * These include checks for definite assignment and unreachable statements.
	 * If any errors occur, an empty list will be returned.
	 * @returns the list of attributed parse trees
	 */
	public Queue<Env<AttrContext>> aliasGlobal(Queue<Env<AttrContext>> envs) {
		ListBuffer<Env<AttrContext>> results = lb();
		for (Env<AttrContext> env : envs) {
			aliasGlobal(env, results);
		}
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	public Queue<Env<AttrContext>> aliasGlobal(Env<AttrContext> env) {
		ListBuffer<Env<AttrContext>> results = lb();
		aliasGlobal(env, results);
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	protected void aliasGlobal(Env<AttrContext> env, Queue<Env<AttrContext>> results) {
		try {
			if (verboseCompilePolicy) {
				Log.printLines(log.noticeWriter, "[alies " + env.enclClass.sym + "]");
			}
			JavaFileObject prev = log.useSource(
					env.enclClass.sym.sourcefile != null
					? env.enclClass.sym.sourcefile
					: env.toplevel.sourcefile);
			try {
				make.at(Position.FIRSTPOS);
				TreeMaker localMake = make.forToplevel(env.toplevel);

				aliasGlobal.analyzeTree(env.tree, localMake);

				//compileStates.put(env, CompileState.FLOW);

				if (errorCount() > 0) {
					return;
				}

				results.add(env);
			} finally {
				log.useSource(prev);
			}
		} finally {
			if (taskListener != null) {
				TaskEvent e = new TaskEvent(TaskEvent.Kind.ANALYZE, env.toplevel, env.enclClass.sym);
				taskListener.finished(e);
			}
		}
	}

	/**
	 * Perform dataflow checks on attributed parse trees.
	 * These include checks for definite assignment and unreachable statements.
	 * If any errors occur, an empty list will be returned.
	 * @returns the list of attributed parse trees
	 */
	public Queue<Env<AttrContext>> linear(Queue<Env<AttrContext>> envs) {
		ListBuffer<Env<AttrContext>> results = lb();
		for (Env<AttrContext> env : envs) {
			linear(env, results);
		}
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	public Queue<Env<AttrContext>> linear(Env<AttrContext> env) {
		ListBuffer<Env<AttrContext>> results = lb();
		linear(env, results);
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	protected void linear(Env<AttrContext> env, Queue<Env<AttrContext>> results) {
		try {
			if (verboseCompilePolicy) {
				Log.printLines(log.noticeWriter, "[linear " + env.enclClass.sym + "]");
			}
			JavaFileObject prev = log.useSource(
					env.enclClass.sym.sourcefile != null
					? env.enclClass.sym.sourcefile
					: env.toplevel.sourcefile);
			try {
				make.at(Position.FIRSTPOS);
				TreeMaker localMake = make.forToplevel(env.toplevel);

				linear.analyzeTree(env.tree, localMake);

				compileStates.put(env, CompileState.LINEAR);

				if (errorCount() > 0) {
					return;
				}

				results.add(env);
			} finally {
				log.useSource(prev);
			}
		} finally {
			if (taskListener != null) {
				TaskEvent e = new TaskEvent(TaskEvent.Kind.ANALYZE, env.toplevel, env.enclClass.sym);
				taskListener.finished(e);
			}
		}
	}

	/**
	 * Perform dataflow checks on attributed parse trees.
	 * These include checks for definite assignment and unreachable statements.
	 * If any errors occur, an empty list will be returned.
	 * @returns the list of attributed parse trees
	 */
	public Queue<Env<AttrContext>> pathgen(Queue<Env<AttrContext>> envs) {
		ListBuffer<Env<AttrContext>> results = lb();
		for (Env<AttrContext> env : envs) {
			pathgen(env, results);
		}
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	public Queue<Env<AttrContext>> pathgen(Env<AttrContext> env) {
		ListBuffer<Env<AttrContext>> results = lb();
		pathgen(env, results);
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	protected void pathgen(Env<AttrContext> env, Queue<Env<AttrContext>> results) {
		try {
			if (verboseCompilePolicy) {
				Log.printLines(log.noticeWriter, "[pathgen " + env.enclClass.sym + "]");
			}
			JavaFileObject prev = log.useSource(
					env.enclClass.sym.sourcefile != null
					? env.enclClass.sym.sourcefile
					: env.toplevel.sourcefile);
			try {
				make.at(Position.FIRSTPOS);
				TreeMaker localMake = make.forToplevel(env.toplevel);
//				pathGen.analyzeTree(env.tree, localMake, target);
				compileStates.put(env, CompileState.FLOW);

				/*
				if (errorCount() > 0) {
					return;
				}
				 *
				 */

				results.add(env);
			} finally {
				log.useSource(prev);
			}
		} finally {
			if (taskListener != null) {
				TaskEvent e = new TaskEvent(TaskEvent.Kind.ANALYZE, env.toplevel, env.enclClass.sym);
				taskListener.finished(e);
			}
		}
	}


	public Queue<Pair<Env<AttrContext>, JCClassDecl>> taskgen(Queue<Pair<Env<AttrContext>, JCClassDecl>> envs) {
		ListBuffer<Pair<Env<AttrContext>, JCClassDecl>> results = lb();
		for (Pair<Env<AttrContext>, JCClassDecl> p: envs) {
			results.add(new Pair<Env<AttrContext>, JCClassDecl>(taskgen(p.fst).element(),p.snd));
		}
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	public Queue<Env<AttrContext>> taskgen(Env<AttrContext> env) {
		ListBuffer<Env<AttrContext>> results = lb();
		taskgen(env, results);
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	protected void taskgen(Env<AttrContext> env, Queue<Env<AttrContext>> results) {
		try {
			if (verboseCompilePolicy) {
				Log.printLines(log.noticeWriter, "[taskgen " + env.enclClass.sym + "]");
			}
			JavaFileObject prev = log.useSource(
					env.enclClass.sym.sourcefile != null
					? env.enclClass.sym.sourcefile
					: env.toplevel.sourcefile);
			try {
				make.at(Position.FIRSTPOS);
				TreeMaker localMake = make.forToplevel(env.toplevel);
				taskGen.analyzeTree(env.tree, localMake, target,  fileManager);

				/*
				if (errorCount() > 0) {
					return;
				}
				 *
				 */

				results.add(env);
			} finally {
				log.useSource(prev);
			}
		} finally {
			if (taskListener != null) {
				TaskEvent e = new TaskEvent(TaskEvent.Kind.ANALYZE, env.toplevel, env.enclClass.sym);
				taskListener.finished(e);
			}
		}
	}


	/**
	 * Perform dataflow checks on attributed parse trees.
	 * These include checks for definite assignment and unreachable statements.
	 * If any errors occur, an empty list will be returned.
	 * @returns the list of attributed parse trees
	 */
	public Queue<Env<AttrContext>> refgen(Queue<Env<AttrContext>> envs) {
		ListBuffer<Env<AttrContext>> results = lb();
		for (Env<AttrContext> env : envs) {
			refgen(env, results);
		}
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	public Queue<Env<AttrContext>> refgen(Env<AttrContext> env) {
		ListBuffer<Env<AttrContext>> results = lb();
		refgen(env, results);
		return stopIfError(results);
	}

	/**
	 * Perform dataflow checks on an attributed parse tree.
	 */
	protected void refgen(Env<AttrContext> env, Queue<Env<AttrContext>> results) {
		try {
			if (verboseCompilePolicy) {
				Log.printLines(log.noticeWriter, "[refgen " + env.enclClass.sym + "]");
			}
			JavaFileObject prev = log.useSource(
					env.enclClass.sym.sourcefile != null
					? env.enclClass.sym.sourcefile
					: env.toplevel.sourcefile);
			try {
				make.at(Position.FIRSTPOS);
				TreeMaker localMake = make.forToplevel(env.toplevel);
				refGen.analyzeTree(env.tree);
				//compileStates.put(env, CompileState.FLOW);

				if (errorCount() > 0) {
					return;
				}

				results.add(env);
			} finally {
				log.useSource(prev);
			}
		} finally {
			if (taskListener != null) {
				TaskEvent e = new TaskEvent(TaskEvent.Kind.ANALYZE, env.toplevel, env.enclClass.sym);
				taskListener.finished(e);
			}
		}
	}

	/** Generates the source or class file for a list of classes.
	 * The decision to generate a source file or a class file is
	 * based upon the compiler's options.
	 * Generation stops if an error occurs while writing files.
	 */
	public void generate(Queue<Pair<Env<AttrContext>, JCClassDecl>> queue) {
		generate(queue, null);
	}

	public void generate(Queue<Pair<Env<AttrContext>, JCClassDecl>> queue, Queue<JavaFileObject> results) {

		boolean usePrintSource = (stubOutput || sourceOutput || printFlat);

		for (Pair<Env<AttrContext>, JCClassDecl> x : queue) {
			Env<AttrContext> env = x.fst;
			JCClassDecl cdef = x.snd;

			if (verboseCompilePolicy) {
				Log.printLines(log.noticeWriter, "[generate "
						+ (usePrintSource ? " source" : "code")
						+ " " + cdef.sym + "]");
			}

			if (taskListener != null) {
				TaskEvent e = new TaskEvent(TaskEvent.Kind.GENERATE, env.toplevel, cdef.sym);
				taskListener.started(e);
			}

			JavaFileObject prev = log.useSource(env.enclClass.sym.sourcefile != null
					? env.enclClass.sym.sourcefile
					: env.toplevel.sourcefile);
			try {
				JavaFileObject file;

				if (depGraph) {
					genGraph(env, cdef, work);
				}

				if (usePrintSource) {
					if (!skipGenerate)
						file = printSource(env, cdef);
					else
						file=null;
				} else {
					if (emitCPPFlag) {

						emitCPPHeader(env, cdef);

						//if (results != null && file2 != null)
						//	results.add(file2);

						emitExport(env, cdef);

						file = emitCPP(env, cdef);
					} else {

						if (!skipGenerate)
							file = genCode(env, cdef);
						else
							file=null;
					}
				}
				if (results != null && file != null) {
					results.add(file);
				}
			} catch (IOException ex) {
				log.error(cdef.pos(), "class.cant.write",
						cdef.sym, ex.getMessage());
				return;
			} finally {
				log.useSource(prev);
			}

			if (taskListener != null) {
				TaskEvent e = new TaskEvent(TaskEvent.Kind.GENERATE, env.toplevel, cdef.sym);
				taskListener.finished(e);
			}
		}
	}

	// where
	Map<JCCompilationUnit, Queue<Env<AttrContext>>> groupByFile(Queue<Env<AttrContext>> envs) {
		// use a LinkedHashMap to preserve the order of the original list as much as possible
		Map<JCCompilationUnit, Queue<Env<AttrContext>>> map = new LinkedHashMap<JCCompilationUnit, Queue<Env<AttrContext>>>();
		for (Env<AttrContext> env : envs) {
			Queue<Env<AttrContext>> sublist = map.get(env.toplevel);
			if (sublist == null) {
				sublist = new ListBuffer<Env<AttrContext>>();
				map.put(env.toplevel, sublist);
			}
			sublist.add(env);
		}
		return map;
	}

	JCClassDecl removeMethodBodies(JCClassDecl cdef) {
		final boolean isInterface = (cdef.mods.flags & Flags.INTERFACE) != 0;
		class MethodBodyRemover extends TreeTranslator {

			public void visitMethodDef(JCMethodDecl tree) {
				tree.mods.flags &= ~Flags.SYNCHRONIZED;
				for (JCVariableDecl vd : tree.params) {
					vd.mods.flags &= ~Flags.FINAL;
				}
				tree.body = null;
				super.visitMethodDef(tree);
			}

			public void visitVarDef(JCVariableDecl tree) {
				if (tree.init != null && tree.init.type.constValue() == null) {
					tree.init = null;
				}
				super.visitVarDef(tree);
			}

			public void visitClassDef(JCClassDecl tree) {
				ListBuffer<JCTree> newdefs = lb();
				for (List<JCTree> it = tree.defs; it.tail != null; it = it.tail) {
					JCTree t = it.head;
					switch (t.getTag()) {
						case JCTree.CLASSDEF:
							if (isInterface
									|| (((JCClassDecl) t).mods.flags & (Flags.PROTECTED | Flags.PUBLIC)) != 0
									|| (((JCClassDecl) t).mods.flags & (Flags.PRIVATE)) == 0 && ((JCClassDecl) t).sym.packge().getQualifiedName() == names.java_lang) {
								newdefs.append(t);
							}
							break;
						case JCTree.METHODDEF:
							if (isInterface
									|| (((JCMethodDecl) t).mods.flags & (Flags.PROTECTED | Flags.PUBLIC)) != 0
									|| ((JCMethodDecl) t).sym.name == names.init
									|| (((JCMethodDecl) t).mods.flags & (Flags.PRIVATE)) == 0 && ((JCMethodDecl) t).sym.packge().getQualifiedName() == names.java_lang) {
								newdefs.append(t);
							}
							break;
						case JCTree.VARDEF:
							if (isInterface || (((JCVariableDecl) t).mods.flags & (Flags.PROTECTED | Flags.PUBLIC)) != 0
									|| (((JCVariableDecl) t).mods.flags & (Flags.PRIVATE)) == 0 && ((JCVariableDecl) t).sym.packge().getQualifiedName() == names.java_lang) {
								newdefs.append(t);
							}
							break;
						default:
							break;
					}
				}
				tree.defs = newdefs.toList();
				super.visitClassDef(tree);
			}
		}
		MethodBodyRemover r = new MethodBodyRemover();
		return r.translate(cdef);
	}

	public void reportDeferredDiagnostics() {
		if (annotationProcessingOccurred
				&& implicitSourceFilesRead
				&& implicitSourcePolicy == ImplicitSourcePolicy.UNSET) {
			if (explicitAnnotationProcessingRequested()) {
				log.warning("proc.use.implicit");
			} else {
				log.warning("proc.use.proc.or.implicit");
			}
		}
		chk.reportDeferredDiagnostics();
	}

	/** Close the compiler, flushing the logs
	 */
	public void close() {
		close(true);
	}

	public void close(boolean disposeNames) {
		rootClasses = null;
		reader = null;
		make = null;
		writer = null;
		enter = null;
		if (todo != null) {
			todo.clear();
		}
		todo = null;
		parserFactory = null;
		syms = null;
		source = null;
		attr = null;
		chk = null;
		gen = null;
		flow = null;
		transTypes = null;
		desugarSyntax = null;
		lower = null;
		annotate = null;
		types = null;

		log.flush();
		try {
			fileManager.flush();
		} catch (IOException e) {
			throw new Abort(e);
		} finally {
			if (names != null && disposeNames) {
				names.dispose();
			}
			names = null;
		}
	}

	/** Output for "-verbose" option.
	 *  @param key The key to look up the correct internationalized string.
	 *  @param arg An argument for substitution into the output string.
	 */
	protected void printVerbose(String key, Object arg) {
		Log.printLines(log.noticeWriter, Log.getLocalizedString("verbose." + key, arg));
	}

	/** Print numbers of errors and warnings.
	 */
	protected void printCount(String kind, int count) {
		if (count != 0) {
			String text;
			if (count == 1) {
				text = Log.getLocalizedString("count." + kind, String.valueOf(count));
			} else {
				text = Log.getLocalizedString("count." + kind + ".plural", String.valueOf(count));
			}
			Log.printLines(log.errWriter, text);
			log.errWriter.flush();
		}
	}

	private static long now() {
		return System.currentTimeMillis();
	}

	private static long elapsed(long then) {
		return now() - then;
	}

	public void initRound(JavaCompiler prev) {
		keepComments = prev.keepComments;
		start_msec = prev.start_msec;
		hasBeenUsed = true;
	}

	public static void enableLogging() {
		Logger logger = Logger.getLogger(com.sun.tools.javac.Main.class.getPackage().getName());
		logger.setLevel(Level.ALL);

		for (Handler h : logger.getParent().getHandlers()) {
			h.setLevel(Level.ALL);
		}
	}
}
