package com.sun.tools.javac.comp;

import java.util.*;

import java.io.IOException;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.code.Type.*;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.JavaCompiler.Target;
import java.io.BufferedWriter;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.ext.*;

/**
 * This is where the magic happens! The task graph of each method is split into paths (fragments) at
 * all split/join vertices. That gives the maximum possible parallelism (paths are translated into
 * tasks later on). Parallel paths are joined (tasks merged into one bigger task) if the work
 * estimation says that the paths do not do enough work. Also the scheduling (which node schedules
 * which paths/tasks) is generated.
 *
 * Here we must also take into account: - Blocking - Grouping (missing) - Control Flow
 *
 * The work/com/overhead calc currently is a placeholder (marked with MAGIC)
 *
 * <p>
 * <b>This is NOT part of any API supported by Sun Microsystems. If you write code that depends on
 * this, you do so at your own risk. This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
class ExraTask {

	public float w, est;

	public ExraTask(float w, float est) {
		this.w = w;
		this.est = est;
	}

	public static void add(Map<TaskSet, ExraTask> map, TaskSet t, float w, float est) {
		ExraTask et = map.get(t);
		if (et == null) {
			map.put(t, new ExraTask(w, est));
		} else {
			map.put(t, new ExraTask(Math.max(w, et.w), Math.max(est, et.est)));
		}
	}
}

class findRecursion extends TreeScanner {

	int recursion = 0;

	public void scan(JCTree tree) {
		if (tree != null) {
			tree.accept(this);
		}
	}
    public void visitIf(JCIf tree) {
        scan(tree.cond);//exclude branches
    }

	public void visitApply(JCMethodInvocation that) {
		if(((MethodSymbol)(TreeInfo.symbol(that.meth))).mayBeRecursive)
			recursion++;

		super.visitApply(that);
	}

	static int getRecursion(JCTree t) {
		findRecursion fr = new findRecursion();
		fr.scan(t);
		return fr.recursion;
	}
}

public class TaskGen extends TreeScanner {

	protected static final Context.Key<TaskGen> pathgenKey
			= new Context.Key<TaskGen>();

	public static TaskGen instance(Context context) {
		TaskGen instance = context.get(pathgenKey);
		if (instance == null) {
			instance = new TaskGen(context);
		}
		return instance;
	}
	private Names names;
	private Log log;
	private TreeMaker make;
	private Work work;
	private TreeCopier copy;
	private Target target;
	private boolean disablePathMerge;
	private boolean disableParallelPaths;
	private boolean verbose = false;
	private JCMethodDecl method;
	private JavaFileManager fileManager;

	/**
	 * Andreas Wagner introduced new mapping from unoptimized tasksets to optimized tasksets. used
	 * later in MPI part
	 */
	private Map<iTask, iTask> unoptimizedToOptimized;

	private boolean disableErrors = false;

	boolean dumpMerge = false;
	int count = 0;

	Properties configFile = null;

	boolean existsConfig(String name) {
		if (configFile == null) {
			return false;
		}
		String prop = configFile.getProperty(name);
		return prop != null;
	}

	float getConfig(String name, float default_value) {
		if (configFile == null) {
			return default_value;
		}
		String prop = configFile.getProperty(name);
		if (prop == null) {
			return default_value;
		} else {
			try {
				return Float.parseFloat(prop);
			} catch (NumberFormatException e) {
				//FIXME: error
				return default_value;
			}
		}
	}

	float rho;
	float a;
	float lin;
	float smin;
	float omin;

	//private Hashtable<String,JCTree> staticForIdents = new Hashtable<String,JCTree>(301, 0.5f);
	protected TaskGen(Context context) {
		context.put(pathgenKey, this);
		names = Names.instance(context);
		log = Log.instance(context);
		make = TreeMaker.instance(context);
		copy = new TreeCopier(make);
		//target = Target.instance(context);
		//source = Source.instance(context);

		work = Work.instance(context);

		Options options = Options.instance(context);

		disablePathMerge = options.get("-NOPATHMERGE") != null;

		if (disablePathMerge) {
			System.out.println("NOTE: path merging disabled");
		}

		disableParallelPaths = options.get("-NOPARALLELPATHS") != null;

		if (disableParallelPaths) {
			System.out.println("NOTE: parallel paths disabled");
		}

		verbose = options.get("-verbose") != null;

		JavaCompiler jc = JavaCompiler.instance(context);
		configFile = jc.configFile;

		dumpMerge = options.get("-DUMPMERGE") != null;

		if (!existsConfig("lin")) {
			log.warning("speedup.model.missing");
		}

		rho = getConfig("rho", 1.35f);
		a = getConfig("a", 1948.f);
		omin = getConfig("omin", 651.0f); //base overhead
		lin = getConfig("lin", 0.5f); //we want at least 50% of relative linear speedup
		smin = getConfig("smin", 1.0f); //we want absolute at least sequential speed

		unoptimizedToOptimized = new LinkedHashMap<iTask, iTask>();

	}

	float etp2(float ts) {
		return (-((2 * rho * rho - 3 * rho) * ts) / 6 + ts + a);
	}

	float ratio(float ts) {
		return (float) Math.sqrt((etp2(ts) - omin) / ts);
	}

	float etp(float ts, int n) {
		if (n == 1) {
			return ts;
		} else {
			return (float) Math.pow(ratio(ts), n) * ts + omin;
		}
	}

	//eval fit for speedup
	float speedup(float tsum, float ts, int n) {
		return tsum / etp(ts, n);
	}

	//pragmas may force (non-)joining of paths
	boolean HandlePragma(Set<JCPragma> pragmas, String reason) {
		boolean abbort_merge = false;

		for (JCPragma p : pragmas) {
			if ((p.flag & Flags.FORCE_PARALLEL) != 0 && target.allowPragma) {
				if (verbose && !disableErrors) {
					log.warning(p.pos, "force.no.merge.paths", p, reason);
				}

				abbort_merge = true;
			}
		}

		if (!abbort_merge) {
			for (JCPragma p : pragmas) {
				if ((p.flag & Flags.PARALLEL) != 0 && !disableErrors) {
					log.warning(p.pos, "merge.paths", p, reason);
				}
			}
		}

		return abbort_merge;
	}

	//find target value of pragma
	Set<VarSymbol> getTargets(TaskSet cn)//FIXME: make me tree visitor!
	{
		Set<VarSymbol> target = new LinkedHashSet<VarSymbol>();
		for (JCTree t : cn) {
			Symbol s = null;

			if (t.getTag() == JCTree.EXEC) {
				t = ((JCExpressionStatement) t).expr;
			}
			if (t.getTag() == JCTree.ASSIGN) {
				s = TreeInfo.symbol(((JCAssign) t).lhs);
			}
			if (t.getTag() == JCTree.VARDEF) {
				s = ((JCVariableDecl) t).sym;
			}

			if (s != null) {
				target.add((VarSymbol) s);
			}
		}
		return target;
	}

	//check for pragmas and that there are no impossible pragmas
	boolean Pragmas(String reason, TaskSet p1, TaskSet p2) {
		Set<JCPragma> pragmas = checkPragma(p1, p2);

		return !(HandlePragma(pragmas, "(" + p1.getPathBlockNoCache() + "|" + p2.getPathBlockNoCache() + ")" + " : " + reason));
	}

	//is it at all allowed to merge p1 and p2
	boolean mayMerge(JCTree fcn1, JCTree fcn2, TaskSet p1, TaskSet p2) {

		if ((fcn1.getTag() == JCTree.SKIP || fcn2.getTag() == JCTree.SKIP)) {
			return false;
		}

		//if p1 and p2 are forced onto different threads then they are of course unmergeable
		if (!p1.getThreads().equals(p2.getThreads())) {
			return false;
		}

		//blocking paths are scheduled on extra threads (so fixed # of worker threads is not blocked)
		if (p1.containsBlocking() || p2.containsBlocking()) {
			//blocking threads can only be merged if they are depend
			Set<VarSymbol> s1 = p1.getInSymbols();
			s1.retainAll(p2.getOutSymbols());
			Set<VarSymbol> s2 = p2.getInSymbols();
			s2.retainAll(p1.getOutSymbols());

			if (!((p2.containsBlocking() && !s1.isEmpty()) || (p1.containsBlocking() && !s2.isEmpty()))) //if paths depend on each other then we can merge them even if they block
			{
				return false;
			}
		}

		//branches are handled extra and must not be merged
		if (!((fcn1.getTag() == JCTree.IF && fcn2.getTag() == JCTree.CF) || (fcn1.getTag() == JCTree.CF && fcn2.getTag() == JCTree.IF)) && fcn1.scheduler != fcn2.scheduler) {
			return false;
		}

		//cannot merge paths with different schedulers
		if (fcn1.scheduler != fcn2 && fcn2.scheduler != fcn1 && fcn1.scheduler != fcn2.scheduler) {
			return false;
		}

		//finally statements must not be merged
		if ((fcn1.getTag() == JCTree.RETURN && fcn1.nop) || (fcn2.getTag() == JCTree.RETURN && fcn2.nop)) {
			return false;
		}

		//do not merge CF edges
		if ((fcn1.getTag() == JCTree.CF || fcn2.getTag() == JCTree.CF)) {
			return false;
		}

		return true;
	}

	//check that pragmas are not contradictory
	Set<JCPragma> checkPragma(TaskSet cn1, TaskSet cn2) {
		Set<JCPragma> pragmas = new LinkedHashSet<JCPragma>();
		Set<VarSymbol> p1s = getTargets(cn1);
		Set<VarSymbol> p2s = getTargets(cn2);

		if (!p1s.isEmpty() && !p2s.isEmpty()) {
			for (JCPragma p : method.pragmas) {
				if ((p1s.contains(p.s1) && p2s.contains(p.s2))
						|| (p1s.contains(p.s2) && p2s.contains(p.s1))) {
					pragmas.add(p);
				}
				if ((p1s.contains(p.s1) && p1s.contains(p.s2))
						|| (p2s.contains(p.s2) && p2s.contains(p.s1))) {
					if (p.flag == Flags.FORCE_PARALLEL && !disableErrors) {
						log.error(p.pos, "force.impossible", p);
					}
				}
			}
		}
		return pragmas;
	}

	TaskSet createMultiNode(JCTree node) {
		TaskSet set = new TaskSet(method.depGraphImplicit, method);
		set.add(node);
		return set;
	}

	//my trans red impl (JGraphT doesn't provide this), it's not optimal
	//also build map from node to topol id
	SimpleDirectedGraph<TaskSet, DefaultEdge> transitiveReduction(DirectedWeightedMultigraph<JCTree, Arc> g) {
		SimpleDirectedGraph<TaskSet, DefaultEdge> res = new SimpleDirectedGraph<TaskSet, DefaultEdge>(DefaultEdge.class);
		for (Iterator<JCTree> i = new TopologicalOrderIterator<JCTree, Arc>(g); i.hasNext();) {
			JCTree node = i.next();

			//construct depGraphImplicit where only deps on the node with highest topology are stored
			//method.depGraphImplicit.addVertex(node);
			TaskSet snode = createMultiNode(node);
			res.addVertex(snode);
			ArrayList<Arc> list = new ArrayList<Arc>(g.incomingEdgesOf(node));
			if (list.size() > 0) {
				Collections.sort(list, new Comparator<Arc>() {
					public int compare(Arc a1, Arc a2) {
						return method.topolNodes.get(a2.s) - method.topolNodes.get(a1.s);
					}
				});
				for (Arc a : list) {
					DijkstraShortestPath<TaskSet, DefaultEdge> sp = (new DijkstraShortestPath<TaskSet, DefaultEdge>(res, createMultiNode(g.getEdgeSource(a)), snode));
					if (sp.getPathLength() == Double.POSITIVE_INFINITY) {
						res.addEdge(createMultiNode(g.getEdgeSource(a)), snode);
					}
				}
			}
		}

		return res;
	}

	class APSP //not nicely provided by jgrapht
	{

		class SSSP {

			TaskSet self;
			float[] distance = null;
			float[] NSPdistance = null;
			float[] distanceWork = null;
			float[] NSPdistanceWork = null;
			TaskSet[] prec = null;
			TaskSet[] precWork = null;
			int topolId;

			SSSP(TaskSet self, int topolId) {
				this.self = self;
				this.topolId = topolId;
			}

			public String toString() {
				return self.toString();
			}

			void merge(TaskSet a, TaskSet b, TaskSet merged, int tid, int oid, boolean linear) {
				float da = getDistanceNSP(a);
				float db = getDistanceNSP(b);

				boolean longer = false;

				float daWork = getDistanceNSPWork(a);
				float dbWork = getDistanceNSPWork(b);

				float mergeDist = Math.min(da, db);

				if (!linear && da == 1.f && db > 1.f && db < Float.POSITIVE_INFINITY) {
					longer = true;
					mergeDist = Math.max(da, db);
					for (JCTree t : a) {
						prec[topolNodes.get(createMultiNode(t)).topolId] = prec[topolNodes.get(createMultiNode(b.iterator().next())).topolId];
					}
				}

				if (!linear && db == 1.f && da > 1.f && da < Float.POSITIVE_INFINITY) {
					longer = true;
					mergeDist = Math.max(da, db);
					for (JCTree t : b) {
						prec[topolNodes.get(createMultiNode(t)).topolId] = prec[topolNodes.get(createMultiNode(a.iterator().next())).topolId];
					}
				}
				//only if we can reach the merged node then there may be a difference:
				if ((da < Float.POSITIVE_INFINITY || db < Float.POSITIVE_INFINITY))//&&!(da<Float.POSITIVE_INFINITY&&db<Float.POSITIVE_INFINITY)
				{
					SSSP ssspA = NSPtopolNodes.get(a);
					SSSP ssspB = NSPtopolNodes.get(b);

					for (SSSP sssp : NSPtopolNodes.values()) {
						if (sssp.topolId != tid && sssp.topolId != oid)//skip a and b
						//if(sssp.topolId!=topolId)
						{
							float oldDist = NSPdistance[sssp.topolId];
							float newDist = oldDist;
							//is new way shorter?
							float at = ssspA.getDistanceNSP(sssp.self);
							if (da == Float.POSITIVE_INFINITY) {
								newDist = mergeDist + at;
							}
							float bt = ssspB.getDistanceNSP(sssp.self);
							if (db == Float.POSITIVE_INFINITY) {
								newDist = mergeDist + bt;
							}

							boolean exitlonger = false;
							if (!linear && at == 1.f && bt > 1.f && bt < Float.POSITIVE_INFINITY) {
								exitlonger = true;
								newDist = mergeDist + bt;
								for (JCTree t : sssp.self) {
									java.util.LinkedList<TaskSet> path = ssspB.getPathNSP(createMultiNode(sssp.self.iterator().next()));
									path.addFirst(createMultiNode(ssspB.self.iterator().next()));

									prec[topolNodes.get(createMultiNode(t)).topolId] = path.get(path.size() - 2);
								}
							} else if (!linear && bt == 1.f && at > 1.f && at < Float.POSITIVE_INFINITY) {
								exitlonger = true;
								newDist = mergeDist + at;
								for (JCTree t : sssp.self) {
									java.util.LinkedList<TaskSet> path = ssspA.getPathNSP(createMultiNode(sssp.self.iterator().next()));
									path.addFirst(createMultiNode(ssspA.self.iterator().next()));

									prec[topolNodes.get(createMultiNode(t)).topolId] = path.get(path.size() - 2);
								}
							}

							if (!exitlonger) {
								NSPdistance[sssp.topolId] = Math.min(newDist, oldDist);
							} else {
								NSPdistance[sssp.topolId] = Math.max(newDist, oldDist);
							}

							if (db == Float.POSITIVE_INFINITY) {
								for (JCTree t : b) {
									prec[topolNodes.get(createMultiNode(t)).topolId] = createMultiNode(a.iterator().next());
								}
							}
							if (da == Float.POSITIVE_INFINITY) {
								for (JCTree t : a) {
									prec[topolNodes.get(createMultiNode(t)).topolId] = createMultiNode(b.iterator().next());
								}
							}

							if (!linear && oldDist == 1.f && newDist > 1.f && newDist < Float.POSITIVE_INFINITY) {
								g.removeEdge(self, sssp.self);//remove transitive edge
								NSPdistance[sssp.topolId] = newDist;
							}

							if (!linear && ((oldDist == 1.f && newDist > 1.f) || oldDist == Float.POSITIVE_INFINITY) && newDist < Float.POSITIVE_INFINITY) {
								float dda = ssspA.getDistanceNSP(sssp.self);
								float ddb = ssspB.getDistanceNSP(sssp.self);

								//fix path??
								if (dda > 0.f && dda < ddb) {
									java.util.LinkedList<TaskSet> path = ssspA.getPathNSP(createMultiNode(sssp.self.iterator().next()));
									path.addFirst(createMultiNode(ssspA.self.iterator().next()));
									for (JCTree t : sssp.self) {
										prec[topolNodes.get(createMultiNode(t)).topolId] = path.get(path.size() - 2);
									}
								} else if (ddb > 0.f && ddb < Float.POSITIVE_INFINITY) {
									java.util.LinkedList<TaskSet> path = ssspB.getPathNSP(createMultiNode(sssp.self.iterator().next()));
									path.addFirst(createMultiNode(ssspB.self.iterator().next()));
									for (JCTree t : sssp.self) {
										prec[topolNodes.get(createMultiNode(t)).topolId] = path.get(path.size() - 2);
									}
								}
							}

							at = ssspA.getDistanceNSPWork(sssp.self);
							if (daWork == Float.POSITIVE_INFINITY && getDistanceNSPWork(sssp.self) > dbWork + at) {
								NSPdistanceWork[sssp.topolId] = dbWork + at;
							}
							bt = ssspB.getDistanceNSPWork(sssp.self);
							if (dbWork == Float.POSITIVE_INFINITY && getDistanceNSPWork(sssp.self) > daWork + bt) {
								NSPdistanceWork[sssp.topolId] = daWork + bt;
							}
						}
					}
				}

				NSPdistance[oid] = Float.POSITIVE_INFINITY;//other is not reachable anymore
				NSPdistanceWork[oid] = Float.POSITIVE_INFINITY;//other is not reachable anymore

				if (!longer) {
					NSPdistance[tid] = Math.min(da, db); //new merge node
					NSPdistanceWork[tid] = Math.min(daWork, dbWork); //new merge node
				} else {
					NSPdistance[tid] = Math.max(da, db); //new merge node
					NSPdistanceWork[tid] = Math.max(daWork, dbWork); //new merge node
				}
			}

			void initFromMerge(TaskSet a, TaskSet b) {
				int nodeCount = topolNodes.keySet().size();//keep original array size!!
				NSPdistance = new float[nodeCount];
				NSPdistanceWork = new float[nodeCount];

				SSSP ssspA = NSPtopolNodes.get(a);
				SSSP ssspB = NSPtopolNodes.get(b);

				NSPtopolNodes.remove(a);//remove a/b
				NSPtopolNodes.remove(b);

				NSPdistance[topolId] = 0.f; //self distance
				NSPdistanceWork[topolId] = 0.f; //self distance

				for (SSSP sssp : NSPtopolNodes.values())//does not contain a/b anymore
				{
					if (sssp != this) {
						NSPdistance[sssp.topolId] = Math.min(ssspA.getDistanceNSP(sssp.self), ssspB.getDistanceNSP(sssp.self));
						NSPdistanceWork[sssp.topolId] = Math.min(ssspA.getDistanceNSPWork(sssp.self), ssspB.getDistanceNSPWork(sssp.self));
					}
					//sssp.NSPdistance[sssp.topolId]=Math.min(sssp.getDistanceNSP(a),
				}

				prec = new TaskSet[nodeCount];
				NSPtopolNodes.put(a, ssspA);//readd a/b
				NSPtopolNodes.put(b, ssspB);//

				for (TaskSet target : topolNodes.keySet()) {
					if (NSPtopolNodes.get(unmergedToMerged.get(target.getNode())) == null) {
						continue;
					}

					float da = ssspA.getDistanceNSP(unmergedToMerged.get(target.getNode()));
					float db = ssspB.getDistanceNSP(unmergedToMerged.get(target.getNode()));

					if (da <= db) {
						prec[topolNodes.get(target).topolId] = ssspA.prec[topolNodes.get(target).topolId];
					} else if (db < da) {
						prec[topolNodes.get(target).topolId] = ssspB.prec[topolNodes.get(target).topolId];
					}

				}
				NSPtopolNodes.remove(a);//remove a/b
				NSPtopolNodes.remove(b);

			}

			void calcSSSP() //O(N+M)
			{
				int nodeCount = topolNodes.keySet().size();

				distance = new float[nodeCount];
				prec = new TaskSet[nodeCount];
				distanceWork = new float[nodeCount];
				precWork = new TaskSet[nodeCount];

				for (int i = 0; i < nodeCount; i++) {
					distance[i] = Float.POSITIVE_INFINITY;
					distanceWork[i] = Float.POSITIVE_INFINITY;
					prec[i] = null;
					precWork[i] = null;
				}

				distance[topolId] = 0.f;
				distanceWork[topolId] = 0.f;

				for (int i = topolId + 1; i < topolNodes.size();)//skip nodes with smaller topolId (cannot be reachable from this node)
				{
					TaskSet node = mapNodes.get(i);
					Set<DefaultEdge> outEdges = g.outgoingEdgesOf(node);
					for (Iterator<DefaultEdge> edges = outEdges.iterator(); edges.hasNext();) {
						//Relax()
						DefaultEdge edge = edges.next();
						int sid = topolNodes.get(node).topolId;
						int tid = topolNodes.get(g.getEdgeTarget(edge)).topolId;

						if (distance[tid] > distance[sid] + 1)//FIXME: weight
						{
							distance[tid] = distance[sid] + 1;
							prec[tid] = node;
						}
						float workTarget = work.getWork((iTask) g.getEdgeTarget(edge), method);
						if (distanceWork[tid] > distanceWork[sid] + workTarget)//FIXME: weight
						{
							distanceWork[tid] = distanceWork[sid] + workTarget;
							precWork[tid] = node;
						}
					}
					i++;
				}
				NSPdistance = Arrays.copyOf(distance, distance.length);
				NSPdistanceWork = Arrays.copyOf(distanceWork, distanceWork.length);

			}

			float getDistance(TaskSet target) //O(1)
			{
				return distance[topolNodes.get(target).topolId];
			}

			float getDistanceWork(TaskSet target) //O(1)
			{
				return distanceWork[topolNodes.get(target).topolId];
			}

			float getDistanceNSP(TaskSet target) //O(1)
			{
				return NSPdistance[NSPtopolNodes.get(target).topolId];
			}

			float getDistanceNSPWork(TaskSet target) //O(1)
			{
				return NSPdistanceWork[NSPtopolNodes.get(target).topolId];
			}

			java.util.List<TaskSet> getPath(TaskSet target) {
				java.util.LinkedList<TaskSet> path = new LinkedList<TaskSet>();

				if (getDistanceNSP(target) < Float.POSITIVE_INFINITY) {
					TaskSet cur = target;

					while (!cur.equals(self)) {
						path.addFirst(cur);
						cur = prec[topolNodes.get(cur).topolId];
					}

					//path.addFirst(self);
					//also add self?->no
				}

				return path;
			}

			java.util.LinkedList<TaskSet> getPathNSP(TaskSet target) {
				java.util.LinkedList<TaskSet> path = new LinkedList<TaskSet>();

				if (getDistanceNSP(unmergedToMerged.get(target.getNode())) < Float.POSITIVE_INFINITY) {
					TaskSet cur = target;

					while (!unmergedToMerged.get(cur.getNode()).equals(self)) {
						path.addFirst(cur);
						cur = prec[topolNodes.get(cur).topolId];
						if (path.size() > 20) {
							int shit = 0;
						}
					}

					//path.addFirst(self);
					//also add self?->no
				}

				return path;
			}

			java.util.List<TaskSet> getPathWork(TaskSet target) {
				java.util.LinkedList<TaskSet> path = new LinkedList<TaskSet>();

				if (getDistanceWork(target) < Float.POSITIVE_INFINITY) {
					TaskSet cur = target;

					while (!cur.equals(self)) {
						path.addFirst(cur);
						cur = precWork[topolNodes.get(cur).topolId];
					}

					//path.addFirst(self);
					//also add self?->no
				}

				return path;
			}
		}

		Map<TaskSet, SSSP> topolNodes = new LinkedHashMap<TaskSet, SSSP>();
		Map<Integer, TaskSet> mapNodes = new LinkedHashMap<Integer, TaskSet>();

		Map<TaskSet, SSSP> NSPtopolNodes = new LinkedHashMap<TaskSet, SSSP>();
		Map<Integer, TaskSet> NSPmapNodes = new LinkedHashMap<Integer, TaskSet>();
		SimpleDirectedGraph<TaskSet, DefaultEdge> g;

		void merge(TaskSet a, TaskSet b, TaskSet merged, boolean linear) {
			int ta = NSPtopolNodes.get(a).topolId;
			int tb = NSPtopolNodes.get(b).topolId;
			int tid = Math.max(ta, tb);
			int oid = Math.min(ta, tb);

			for (SSSP sssp : NSPtopolNodes.values()) {
				if (sssp.topolId != tid && sssp.topolId != oid) {
					sssp.merge(a, b, merged, tid, oid, linear); //update dist to all nodes via merged
				}
			}
			//replace a/b by merged
			SSSP sssp = new SSSP(merged, tid);
			NSPtopolNodes.put(merged, sssp);
			NSPmapNodes.put(tid, merged);
			NSPmapNodes.remove(oid);

			sssp.initFromMerge(a, b);
		}

		APSP(SimpleDirectedGraph<TaskSet, DefaultEdge> g) {
			this.g = g;
			for (Iterator<TaskSet> i = new TopologicalOrderIterator<TaskSet, DefaultEdge>(g); i.hasNext();) {
				TaskSet node = i.next();
				topolNodes.put(node, new SSSP(node, topolNodes.size()));
				mapNodes.put(topolNodes.size(), node);
			}

			for (TaskSet ts : topolNodes.keySet()) {
				topolNodes.get(ts).calcSSSP();
			}

			NSPtopolNodes = new LinkedHashMap<TaskSet, SSSP>(topolNodes);
			NSPmapNodes = new LinkedHashMap<Integer, TaskSet>(mapNodes);
		}

		boolean reachable(TaskSet source, TaskSet target) {
			return (getDistance(source, target) < Float.POSITIVE_INFINITY);
		}

		float getDistance(TaskSet source, TaskSet target) {
			float res;
			if (source == null) {
				res = Float.POSITIVE_INFINITY;
			} else {
				res = topolNodes.get(source).getDistance(target);
			}

			return res;
		}

		float getDistanceWork(TaskSet source, TaskSet target) {
			float res;
			if (source == null) {
				res = Float.POSITIVE_INFINITY;
			} else {
				res = topolNodes.get(source).getDistanceWork(target);
			}

			return res;
		}

		float getDistanceNSP(TaskSet source, TaskSet target) {
			float res;
			if (source == null) {
				res = Float.POSITIVE_INFINITY;
			} else {
				res = NSPtopolNodes.get(source).getDistanceNSP(target);
			}

			return res;
		}

		float getDistanceNSPWork(TaskSet source, TaskSet target) {
			float res;
			if (source == null) {
				res = Float.POSITIVE_INFINITY;
			} else {
				res = NSPtopolNodes.get(source).getDistanceNSPWork(target);
			}

			return res;
		}

		java.util.List<TaskSet> getPath(TaskSet source, TaskSet target) {
			java.util.List<TaskSet> res;

			if (source == null) {
				res = new LinkedList<TaskSet>();
			} else {
				res = topolNodes.get(source).getPath(target);
			}

			return res;
		}

		java.util.List<TaskSet> getPathNSP(TaskSet source, TaskSet target) {
			java.util.List<TaskSet> res;

			if (source == null || target == null) {
				res = new LinkedList<TaskSet>();
			} else {
				res = NSPtopolNodes.get(unmergedToMerged.get(source.getNode())).getPathNSP(target);
			}

			return res;
		}

		java.util.List<TaskSet> getPathWork(TaskSet source, TaskSet target) {
			java.util.List<TaskSet> res;

			if (source == null) {
				res = new LinkedList<TaskSet>();
			} else {
				res = topolNodes.get(source).getPathWork(target);
			}

			return res;
		}
	}

	SimpleDirectedGraph<TaskSet, DefaultEdge> transitiveReduction(final SimpleDirectedGraph<TaskSet, DefaultEdge> g) {
		final java.util.Map<TaskSet, Integer> topolNodes = new LinkedHashMap<TaskSet, Integer>();

		SimpleDirectedGraph<TaskSet, DefaultEdge> res = new SimpleDirectedGraph<TaskSet, DefaultEdge>(DefaultEdge.class);

		for (Iterator<TaskSet> i = new TopologicalOrderIterator<TaskSet, DefaultEdge>(g); i.hasNext();) {
			TaskSet node = i.next();

			//assign unique topol ordered id for shortest paths
			topolNodes.put(node, topolNodes.size());

			//construct hasse diagram where only deps on the node with highest topology are stored
			res.addVertex(node);

			ArrayList<DefaultEdge> list = new ArrayList<DefaultEdge>(g.incomingEdgesOf(node));
			if (list.size() > 0) {
				Collections.sort(list, new Comparator<DefaultEdge>() {
					public int compare(DefaultEdge a1, DefaultEdge a2) {
						return topolNodes.get(g.getEdgeSource(a2)) - topolNodes.get(g.getEdgeSource(a1));
					}
				});
				for (DefaultEdge a : list) {

					DijkstraShortestPath<TaskSet, DefaultEdge> sp = (new DijkstraShortestPath<TaskSet, DefaultEdge>(res, g.getEdgeSource(a), node));
					if (sp.getPathLength() == Double.POSITIVE_INFINITY) {
						res.addEdge(g.getEdgeSource(a), node);
					}
				}
			}
		}

		return res;
	}

	class Loop {

		public Pair<TaskSet, TaskSet> paths;
		public TaskSet commonAncestor, max1, max2;

		Loop(Pair<TaskSet, TaskSet> paths, TaskSet commonAncestor, TaskSet max1, TaskSet max2) {
			this.paths = paths;
			this.commonAncestor = commonAncestor;
			this.max1 = max1;
			this.max2 = max2;
		}
	}

	class CAI {

		public TaskSet ca, max1, max2;
		public float est1, eft1, est2, eft2;

		public CAI(TaskSet ca, TaskSet max1, TaskSet max2, float est1, float eft1, float est2, float eft2) {
			this.ca = ca;
			this.max1 = max1;
			this.max2 = max2;
			this.est1 = est1;
			this.eft1 = eft1;
			this.est2 = est2;
			this.eft2 = eft2;
		}
	};

	SimpleDirectedGraph<TaskSet, DefaultEdge> unmergedGraph = null;
	Map<JCTree, TaskSet> unmergedToMerged = null;
	Map<Pair<TaskSet, TaskSet>, Loop> parallelComputationAncestor = null;
	Map<Pair<TaskSet, TaskSet>, Loop> parallelComputationDescendend = null;
	Map<JCTree, Set<JCTree>> scheduler = null;
	java.util.List<Pair<JCTree, JCTree>> parallelTasks = null;
	Map<JCTree, Set<JCTree>> predecessors = null;

	class DeferableTopolOrder {

		SimpleDirectedGraph<TaskSet, DefaultEdge> g;
		Set<TaskSet> done;
		Queue<TaskSet> todo;
		TaskSet current;
		boolean postpone, finished;

		DeferableTopolOrder(SimpleDirectedGraph<TaskSet, DefaultEdge> g) {
			this.g = new SimpleDirectedGraph<TaskSet, DefaultEdge>(DefaultEdge.class);
			for (TaskSet v : g.vertexSet()) {
				this.g.addVertex(v);
			}
			for (DefaultEdge e : g.edgeSet()) {
				this.g.addEdge(g.getEdgeSource(e), g.getEdgeTarget(e));
			}

			todo = new LinkedList<TaskSet>();
			done = new LinkedHashSet<TaskSet>();
			current = null;
			postpone = false;
			finished = true;
			for (TaskSet ts : g.vertexSet()) {
				if (g.inDegreeOf(ts) == 0) {
					todo.add(ts);
				}
			}
		}

		Set<TaskSet> getDone() {
			return done;
		}

		boolean isPostponed() {
			return postpone;
		}

		TaskSet next() {
			assert (finished == true); //make sure finished was called!

			postpone = false;

			if (todo.isEmpty()) {
				return null;
			}

			current = todo.poll();
			finished = false;
			return current;
		}

		void postpone() {
			postpone = true;
		}

		private void finish(TaskSet ts) //must call finish on node
		{
			finished = true;

			if (postpone) {
				todo.add(ts);
			} else {
				done.add(ts);
				for (DefaultEdge o : g.outgoingEdgesOf(ts)) {
					boolean nodeReady = true;
					for (DefaultEdge i : g.incomingEdgesOf(g.getEdgeTarget(o))) {
						if (!done.contains(g.getEdgeSource(i))) {
							nodeReady = false;
							break;
						}
					}
					if (nodeReady) {
						todo.add(g.getEdgeTarget(o));
					}
				}
			}
		}
	}

	void updateMerged(TaskSet a, TaskSet b, TaskSet m, boolean linear) //O(N^2)
	{
		float est = 0;
		float eft = 0;

		if (method.est != null) {

			if (!linear) {
				for (JCTree t : m) {
					TaskSet n = unmergedToMerged.get(t);
					est = Math.max(method.est.get(n), est);
				}
			} else {
				//if the merged tasks are linear then we want the smaller of the ests from both nodes
				float esta = 0;
				for (JCTree t : a) {
					TaskSet n = unmergedToMerged.get(t);
					esta = Math.max(method.est.get(n), esta);
				}
				float estb = 0;
				for (JCTree t : a) {
					TaskSet n = unmergedToMerged.get(t);
					estb = Math.max(method.est.get(n), estb);
				}
				est = Math.min(esta, estb);
			}

			method.est.put(m, est);
			eft = est + work.getWork((iTask) m, method);
			method.eft.put(m, eft);
		}
		//if we have merged inside merge (because merging create a new join node) we may have to update the est/eft of the follow up nodes

		apsp.merge(a, b, m, linear); //update new shortest paths

		for (JCTree t : m) {
			unmergedToMerged.put(t, m);
		}

		if (method.est != null) {
			//FIXME: update all reachable nodes
			float delta = Math.abs(method.est.get(a) - method.est.get(b));
			//only the start times cause shifts
			if (delta > 0.f) {
				for (DefaultEdge e : method.hasseDiagram.outgoingEdgesOf(m)) {
					TaskSet n = method.hasseDiagram.getEdgeTarget(e);
					if (method.est.get(n) != null) {
						if (eft > method.est.get(n)) {
							method.est.put(n, est + delta);
							method.eft.put(n, eft + delta);
						}

					}
				}
			}
		}

	}

	boolean mayMerge(TaskSet a, TaskSet b) {
		if (a == null || b == null) {
			return false;
		}
		Set<JCTree> cn1 = a.getCalcNodes();
		Set<JCTree> cn2 = b.getCalcNodes();

		JCTree fcn1 = a.getFirstCalcNode(method, cn1);
		JCTree fcn2 = b.getFirstCalcNode(method, cn2);

		return mayMerge(fcn1, fcn2, a, b);
	}

	TaskSet mergeLinear(TaskSet prev, TaskSet node, boolean allowMergeExit) {
		if (!mayMerge(prev, node)) {
			return node;
		}

		TaskSet merged = new TaskSet(method.depGraphImplicit, method);

		unoptimizedToOptimized.put(prev, merged);
		unoptimizedToOptimized.put(node, merged);

		//c.a. fix: for all nodes that have node or prev as c.a.: merged is new c.a.
		//use LinkedHashMap from original c.a. (before any parallelism based merging was done) to new c.a.
		merged.addAll(node);
		merged.addAll(prev);

		method.hasseDiagram.addVertex(merged);

		for (DefaultEdge e : method.hasseDiagram.incomingEdgesOf(prev)) {
			if (!merged.equals(method.hasseDiagram.getEdgeSource(e))) {
				method.hasseDiagram.addEdge(method.hasseDiagram.getEdgeSource(e), merged);
			}
		}

		for (DefaultEdge e : method.hasseDiagram.outgoingEdgesOf(node)) {
			if (!merged.equals(method.hasseDiagram.getEdgeTarget(e))) {
				method.hasseDiagram.addEdge(merged, method.hasseDiagram.getEdgeTarget(e));
			}
		}

		method.hasseDiagram.removeVertex(node);
		method.hasseDiagram.removeVertex(prev);

		//setup mapping from unmerged nodes to merged node
		updateMerged(prev, node, merged, true);//O(N^2)

		if (dumpMerge && !allowMergeExit) {
			count++;
			dumpGraph("count_" + count + "");

			System.err.println("count: " + count + " merged {" + prev.toString() + "} and {" + node.toString() + "}\n");
		}
		return merged;
	}

	void mergeParallel(Set<TaskSet> a) //translate unmerged tasks into merged tasks
	{
		TaskSet merged = null;

		for (TaskSet ts : a) {
			if (merged != null) {
				mergeParallel(merged, ts);
			} else {
				merged = ts;
			}
		}

	}

	int getPathLengthApprox(TaskSet from, TaskSet to)//O(N) -1 if no connection, 1 of path=1 >1 otherwise
	{
		float l = apsp.getDistanceNSP(from, to);
		if (l == Float.POSITIVE_INFINITY) {
			return -1;
		}
		if (l == 1) {
			for (DefaultEdge e : method.hasseDiagram.incomingEdgesOf(to)) {
				if (method.hasseDiagram.getEdgeSource(e).equals(from)) {
					return 1;
				}
			}
			return 2;
		}

		return (int) l;
	}

	/*
	 int getPathLength(TaskSet from, TaskSet to) //O(N), MUST INPUT UNMOD VERTS! ONLY WORKS FOR LOOPS
	 {
	 //O(N) with precalced paths
	 //GraphPath<TaskSet, DefaultEdge> gp = (new DijkstraShortestPath<TaskSet, DefaultEdge>(unmergedGraph, from, to)).getPath();

	 java.util.List<TaskSet> gp = apsp.getPath(from, to);

	 if (gp == null||gp.isEmpty()) {
	 return -1;
	 }

	 Set<TaskSet> mergedNodes = new LinkedHashSet<TaskSet>();
	 for (TaskSet ts : gp)//O(N)
	 {
	 mergedNodes.add(unmergedToMerged.get(ts.getNode()));
	 }

	 return mergedNodes.size();
	 }
	 */
	TaskSet mergeParallel(TaskSet a, TaskSet b)//O(N^2)
	{
		TaskSet ra = unmergedToMerged.get(a.getNode());
		TaskSet rb = unmergedToMerged.get(b.getNode());

		if (!mayMerge(ra, rb)) {
			return ra;
		}

		if (ra.equals(rb)) {
			return null;
		}

		if (getPathLengthApprox(ra, rb) >= 0 || getPathLengthApprox(rb, ra) >= 0) {
			return ra;//
		}
		if (dumpMerge) {
			Set<TaskSet> p1 = new LinkedHashSet<TaskSet>();
			Set<TaskSet> p2 = new LinkedHashSet<TaskSet>();

			CAI cai = getParallelNodesAncestor(a, b, p1, p2, method.est, method.eft);

			System.err.println(cai.ca.toString() + "->" + p1.toString());
			System.err.println(cai.ca.toString() + "->" + p2.toString());
		}

		TaskSet merged = new TaskSet(method.depGraphImplicit, method);

		merged.addAll(ra);
		merged.addAll(rb);

		method.hasseDiagram.addVertex(merged);

		Set<TaskSet> src = new LinkedHashSet<TaskSet>();

		for (DefaultEdge e : method.hasseDiagram.incomingEdgesOf(ra)) { //O(N)

			TaskSet ri = method.hasseDiagram.getEdgeSource(e);

			int l = getPathLengthApprox(ri, rb);//O(N)

			if (method.hasseDiagram.containsVertex(ri) && !merged.equals(ri)) {
				src.add(ri);
				if (l <= 1) {
					method.hasseDiagram.addEdge(ri, merged);
				} else {
					//fixpath (skip edge to a)
					int fix = 0;
					//apsp.
					//method.hasseDiagram.addEdge(ri, merged);
				}
			}
		}

		for (DefaultEdge e : method.hasseDiagram.incomingEdgesOf(rb)) {
			TaskSet ri = method.hasseDiagram.getEdgeSource(e);

			int l = getPathLengthApprox(ri, ra);

			if (method.hasseDiagram.containsVertex(ri) && !merged.equals(ri)) {
				src.add(ri);
				if (l <= 1) {
					method.hasseDiagram.addEdge(ri, merged);
				} else {
					//fixpath
					int fix = 0;
					//method.hasseDiagram.addEdge(ri, merged);
				}
			}
		}

		for (DefaultEdge e : method.hasseDiagram.outgoingEdgesOf(ra)) {
			TaskSet ro = method.hasseDiagram.getEdgeTarget(e);

			int l = getPathLengthApprox(rb, ro);

			if (method.hasseDiagram.containsVertex(ro) && !merged.equals(ro)) {
				if (l <= 1) {
					method.hasseDiagram.addEdge(merged, ro);
				} else {
					//fixpath
					int fix = 0;
					//method.hasseDiagram.addEdge(merged, ro);
				}
			}
		}

		for (DefaultEdge e : method.hasseDiagram.outgoingEdgesOf(rb)) {
			TaskSet ro = method.hasseDiagram.getEdgeTarget(e);

			int l = getPathLengthApprox(ra, ro);

			if (method.hasseDiagram.containsVertex(ro) && !merged.equals(ro)) {
				if (l <= 1) {
					method.hasseDiagram.addEdge(merged, ro);
				} else {
					//fixpath
					int fix = 0;
					//method.hasseDiagram.addEdge(merged, ro);
				}
			}
		}

		updateMerged(ra, rb, merged, false);//O(N^2)

		method.hasseDiagram.removeVertex(ra);
		method.hasseDiagram.removeVertex(rb);

		//System.err.println(method.hasseDiagram.toString());
		if (dumpMerge) {
			count++;
			System.err.println("count: " + count + " merged par {" + ra.toString() + "} and {" + rb.toString() + "}\n");
			dumpGraph("count_" + count + "");

		}

		if (method.hasseDiagram.inDegreeOf(merged) > 1)//we may have created a new join node, so we must check it as well
		{
			MergeTasks(merged, method);

		}

		for (TaskSet ts : src) {
			fixLinearChainPrev(ts, false);
		}

		merged = fixLinearChainPrev(merged, false);

		return merged;
	}

	//int lin=0;
	TaskSet fixLinearChainPrev(TaskSet node, boolean allowMergeExit) {
		if (!method.hasseDiagram.vertexSet().contains(node)) {
			return node;
		}

		if (method.hasseDiagram.inDegreeOf(node) == 1) {
			DefaultEdge e = method.hasseDiagram.incomingEdgesOf(node).iterator().next();
			TaskSet prev = method.hasseDiagram.getEdgeSource(e);

			if (((method.hasseDiagram.inDegreeOf(prev) == 1 && method.hasseDiagram.outDegreeOf(prev) == 1)
					|| (method.hasseDiagram.inDegreeOf(prev) == 0 && method.hasseDiagram.outDegreeOf(prev) == 1))) {
				//found linear chain (will be executed sequentially anyways)..so merge
				TaskSet res = mergeLinear(prev, node, allowMergeExit);
				//dumpGraph("lin_"+lin+"");
				//lin++;
				return res;
			}
		}

		return node;
	}

	boolean reachableFromTo(TaskSet p1, TaskSet p2) {
		//return (new DijkstraShortestPath<TaskSet, DefaultEdge>(method.hasseDiagram, p1, p2)).getPathLength() < Double.POSITIVE_INFINITY;
		return apsp.getDistance(p1, p2) < Float.POSITIVE_INFINITY;
	}

	//inefficient c.a. calc
	Set<TaskSet> findCommonAncestors(TaskSet p1, TaskSet p2) {
		Set<TaskSet> ancestors = new LinkedHashSet<TaskSet>();

		Map<TaskSet, Integer> topolids = new LinkedHashMap<TaskSet, Integer>();

		//calc all ancestors:
		int topid = 0;
		for (Iterator<TaskSet> i = new TopologicalOrderIterator<TaskSet, DefaultEdge>(method.hasseDiagram); i.hasNext();) {
			TaskSet node = i.next();
			topolids.put(node, topid);
			topid++;
			if (reachableFromTo(node, p1) && reachableFromTo(node, p2)) {
				ancestors.add(node);
			}
		}

		Set<TaskSet> lowestAncestors = new LinkedHashSet<TaskSet>();

		//check for mutually unreachable ancestors
		for (TaskSet a : ancestors) {
			boolean lowest = true;
			for (TaskSet b : ancestors) {
				if (a != b
						&& (reachableFromTo(a, b))) {
					lowest = false;
					break;
				}
			}
			if (lowest) {
				lowestAncestors.add(a);
			}
		}

		return lowestAncestors;
	}

	Set<TaskSet> findCommonDescendends(TaskSet p1, TaskSet p2) {

		Set<TaskSet> ancestors = new LinkedHashSet<TaskSet>();

		if (p1.getNode().scheduler != p2.getNode().scheduler) {
			return ancestors;
		}

		Map<TaskSet, Integer> topolids = new LinkedHashMap<TaskSet, Integer>();

		//calc all descs:
		int topid = 0;
		for (Iterator<TaskSet> i = new TopologicalOrderIterator<TaskSet, DefaultEdge>(method.hasseDiagram); i.hasNext();) {
			TaskSet node = i.next();
			topolids.put(node, topid);
			topid++;

			if (node.getNode().scheduler == p1.getNode().scheduler && reachableFromTo(p1, node) && reachableFromTo(p2, node)) {
				ancestors.add(node);
			}
		}

		Set<TaskSet> lowestDescendends = new LinkedHashSet<TaskSet>();

		ancestors.remove(createMultiNode(method.final_value));

		//check for mutually unreachable descendends
		for (TaskSet a : ancestors) {
			boolean lowest = true;
			for (TaskSet b : ancestors) {
				if (a != b
						&& (reachableFromTo(b, a))) {
					lowest = false;
					break;
				}
			}
			if (lowest) {
				lowestDescendends.add(a);
			}
		}

		return lowestDescendends;
	}

	TaskSet findCommonAncestor(TaskSet p1, TaskSet p2) //O(N) returns CA with minimum minimal distance from p1/p2
	{
		Set<TaskSet> cas = findCommonAncestors(p1, p2); //O(1)
		TaskSet max = null;
		float minDist = Float.MAX_VALUE;
		for (TaskSet ts : cas) {
			//float d1 = (float) (new DijkstraShortestPath<TaskSet, DefaultEdge>(method.hasseDiagram, ts, p1)).getPathLength();//O(1)
			//float d2 = (float) (new DijkstraShortestPath<TaskSet, DefaultEdge>(method.hasseDiagram, ts, p2)).getPathLength();//O(1)
			float d1 = apsp.getDistanceWork(ts, p1);
			float d2 = apsp.getDistanceWork(ts, p2);

			if (Math.min(d1, d2) < minDist) {
				minDist = Math.min(d1, d2);
				max = ts;
			}
		}

		return max;
	}

	TaskSet findCommonDescendend(TaskSet p1, TaskSet p2) //O(N) returns CA with minimum minimal distance from p1/p2
	{
		Set<TaskSet> cas = findCommonDescendends(p1, p2); //O(1)
		TaskSet max = null;
		float minDist = Float.MAX_VALUE;
		for (TaskSet ts : cas) {
			float d1 = apsp.getDistanceWork(p1, ts);
			float d2 = apsp.getDistanceWork(p2, ts);

			if (Math.min(d1, d2) < minDist) {
				minDist = Math.min(d1, d2);
				max = ts;
			}
		}

		return max;
	}

	CAI getParallelNodesAncestor(TaskSet nodesi, TaskSet nodesj, Set<TaskSet> p1, Set<TaskSet> p2, Map<iTask, Float> est, Map<iTask, Float> eft) {
		Loop l = parallelComputationAncestor.get(new Pair<TaskSet, TaskSet>(nodesi, nodesj));
		if (l == null) {
			l = parallelComputationAncestor.get(new Pair<TaskSet, TaskSet>(nodesj, nodesi));
		}

		if (l == null) {
			return null;
		}

		float est1 = 0;
		float est2 = 0;
		float eft1 = 0;
		float eft2 = 0;

		ArrayList<TaskSet> ap1 = new ArrayList<TaskSet>();
		ArrayList<TaskSet> ap2 = new ArrayList<TaskSet>();

		java.util.List<TaskSet> np1 = apsp.getPathNSP(l.commonAncestor, l.paths.fst);
		java.util.List<TaskSet> np2 = apsp.getPathNSP(l.commonAncestor, l.paths.snd);

		for (TaskSet ts : np1) //O(N)
		{
			TaskSet rn = unmergedToMerged.get(ts.getNode());
			if (!p1.contains(rn)) {
				p1.add(rn);//O(1)
				ap1.add(rn);
			}
		}
		for (TaskSet ts : np2) //O(N)
		{
			TaskSet rn = unmergedToMerged.get(ts.getNode());
			if (!p2.contains(rn)) {
				p2.add(rn);//O(1)
				ap2.add(rn);
			}
		}

		Set<TaskSet> both = new LinkedHashSet<TaskSet>(p1);
		both.retainAll(p2);

		TaskSet rca = unmergedToMerged.get(l.commonAncestor.getNode());
		int start1 = 0;
		int start2 = 0;

		if (!both.isEmpty()) {
			for (int i = ap1.size() - 1; i >= 0; i--) {
				if (both.contains(ap1.get(i))) {
					rca = ap1.get(i);
					start1 = i + 1;
					break;
				}
			}
			for (int i = ap2.size() - 1; i >= 0; i--) {
				if (both.contains(ap2.get(i))) {
					start2 = i + 1;
					break;
				}
			}
		}

		p1.clear();
		for (int i = start1; i < ap1.size(); i++) //O(N)
		{
			p1.add(ap1.get(i));
		}

		p2.clear();
		for (int i = start2; i < ap2.size(); i++) //O(N)
		{
			p2.add(ap2.get(i));
		}

		if (ap1.size() > 0) {
			est1 = est.get(ap1.get(0));
			eft1 = eft.get(ap1.get(ap1.size() - 1));
		}
		if (ap2.size() > 0) {
			est2 = est.get(ap2.get(0));
			eft2 = eft.get(ap2.get(ap2.size() - 1));
		}
		return new CAI(l.commonAncestor, l.max1, l.max2, est1, eft1, est2, eft2);
	}

	float estmin = Float.MAX_VALUE;
	float estmax = 0;
	float eftmax = 0;
	float workMax = 0;
	float est = Float.MAX_VALUE;

	void handleAD(CAI cai, int i, int j, Set<TaskSet> p1, Set<TaskSet> p2, ArrayList<TaskSet> nodes, Map<TaskSet, ExraTask> addNodes, boolean maymerge) {
		if (cai.ca != null) {
			//find max work done by node(i) (relative to any partner):
			//float st=cai.est1;
			//float et=cai.eft1;
			//float pw=Math.min(cai.eft1-cai.est1,cai.eft2-cai.est2);
			float pw = cai.eft1 - cai.est1;

			estmax = Math.max(estmax, Math.max(cai.est1, cai.est2));
			eftmax = Math.max(eftmax, Math.max(cai.eft1, cai.eft2));
			est = Math.min(est, cai.est1);

			Set<String> groups1 = new LinkedHashSet<String>();
			Set<String> groups2 = new LinkedHashSet<String>();
			Set<String> threads1 = new LinkedHashSet<String>();
			Set<String> threads2 = new LinkedHashSet<String>();

			for (TaskSet ts : p1) {
				groups1.addAll(ts.getGroups());
				threads1.addAll(ts.getThreads());
			}
			for (TaskSet ts : p2) {
				groups2.addAll(ts.getGroups());
				threads2.addAll(ts.getThreads());
			}

			groups1.retainAll(groups2);
			threads1.retainAll(threads2);

			if ((!groups1.isEmpty() || !threads1.isEmpty()) && maymerge) {
				Set<TaskSet> merge = new LinkedHashSet<TaskSet>();
				//mergeParallel(nodes.get(i),nodes.get(j));
				for (TaskSet ts : p1) {
					Set<String> r = ts.getGroups();
					r.retainAll(groups1);
					if (!r.isEmpty()) {
						merge.add(createMultiNode(ts.iterator().next()));
					}
					r = ts.getThreads();
					r.retainAll(threads1);
					if (!r.isEmpty()) {
						merge.add(createMultiNode(ts.iterator().next()));
					}
				}
				for (TaskSet ts : p2) {
					Set<String> r = ts.getGroups();
					r.retainAll(groups1);
					if (!r.isEmpty()) {
						merge.add(createMultiNode(ts.iterator().next()));
					}
					r = ts.getThreads();
					r.retainAll(threads1);
					if (!r.isEmpty()) {
						merge.add(createMultiNode(ts.iterator().next()));
					}
				}
				mergeParallel(merge);
			} else if (mayMerge(nodes.get(i), nodes.get(j))) {
				workMax = Math.max(pw, workMax);
			} else //the two verts at the bottom of loop may not be mergeable due to constraint(e.g. different branch), find something mergeable higher up:
			if (mayMerge(cai.max1, cai.max2)) {
				workMax = Math.max(pw, workMax);
				if (!nodes.get(i).equals(cai.max1)) {
					estmin = Math.min(estmin, est);
					ExraTask.add(addNodes, cai.max1, cai.eft2 - cai.est2, est);
				}
				if (!nodes.get(j).equals(cai.max2)) {
					estmin = Math.min(estmin, est);
					ExraTask.add(addNodes, cai.max2, cai.eft2 - cai.est2, est);
				}
			}
		}

	}

	void MergeTasks(TaskSet v, JCMethodDecl md) { //there are at most N non-merged join verts and < N merged join verts so < 2*N = O(N) MergeTasks calls

		ArrayList<Task> al = new ArrayList<Task>();

		ArrayList<TaskSet> nodes = new ArrayList<TaskSet>();

		Set<TaskSet> contains = new LinkedHashSet<TaskSet>();

		if (v.getNode() != null)//if v is a merged node then it has no alternative siblings
		{
			for (Iterator<Pair<TaskSet, TaskSet>> ipair = todo.keySet().iterator(); ipair.hasNext();) {
				Pair<TaskSet, TaskSet> pair = ipair.next();

				if (v.getNode().scheduler == pair.fst.getNode().scheduler && apsp.reachable(pair.fst, v) && apsp.reachable(pair.snd, v)) {
					if (!todo.get(pair).equals(v)) {
						dto.postpone();
						return;
					} else {
						ipair.remove();
					}
				}
			}
		}

		if (method.final_value != null) {
			contains.add(createMultiNode(method.final_value));
		}

		//Set<DefaultEdge> es = unmergedGraph.incomingEdgesOf(createMultiNode(t));
		{
			Set<DefaultEdge> es = method.hasseDiagram.incomingEdgesOf(v);

			for (DefaultEdge e : es) {
				if (!contains.contains(method.hasseDiagram.getEdgeSource(e))) {
					TaskSet src = method.hasseDiagram.getEdgeSource(e);
					//for(JCTree st:src)
					nodes.add(createMultiNode(src.getFirstCalcNode(method, src.getCalcNodes()))); //just use one representant
					contains.add(method.hasseDiagram.getEdgeSource(e));
				}
			}
		}

		//Map<Pair<TaskSet,TaskSet>,Pair<Set<TaskSet>,Set<TaskSet>>> loop=new LinkedHashMap<Pair<TaskSet, TaskSet>, Pair<Set<TaskSet>, Set<TaskSet>>>();
//O(N^3)
		Map<TaskSet, ExraTask> addNodes = new LinkedHashMap<TaskSet, ExraTask>();

		estmin = Float.MAX_VALUE;
		estmax = 0;
		eftmax = 0;

		//generate all pairs O(N^2)
		for (int i = 0; i < nodes.size(); i++) {
			workMax = 0;
			est = Float.MAX_VALUE;
			for (int j = 0; j < nodes.size(); j++) {
				if (i != j) {
					//get path O(1)
					Set<TaskSet> p1 = new LinkedHashSet<TaskSet>();
					Set<TaskSet> p2 = new LinkedHashSet<TaskSet>();

					CAI ancestor = getParallelNodesAncestor(nodes.get(i), nodes.get(j), p1, p2, method.est, method.eft);//O(N)
					handleAD(ancestor, i, j, p1, p2, nodes, addNodes, true);
				}
			}
			estmin = Math.min(estmin, est);
			al.add(new Task(nodes.get(i), workMax, est)); //max parallelism for nodes[i]
		}

		for (TaskSet ts : addNodes.keySet()) {
			al.add(new Task(ts, addNodes.get(ts).w, addNodes.get(ts).est));
		}

		if (al.size() <= 1) {
			nodes.clear();
			if (method.final_value != null) {
				for (Task t : al) {
					if (t.task.getNode() == method.final_value) {
						Set<DefaultEdge> es = unmergedGraph.incomingEdgesOf(createMultiNode(method.final_value));

						for (DefaultEdge e : es) {
							if (!contains.contains(unmergedGraph.getEdgeSource(e))) {
								nodes.add(unmergedGraph.getEdgeSource(e));
								contains.add(unmergedGraph.getEdgeSource(e));
							}
						}

						for (TaskSet n : nodes) {
							eftmax = Math.max(eftmax, method.eft.get(unmergedToMerged.get(n.getNode())));
						}

						eftmax += work.getWork(method.final_value, method);
						break;
					}
				}
			}

			method.est.put(v, eftmax);
			method.eft.put(v, method.est.get(v) + work.getWork((iTask) v, method));
			return;
		}

		java.util.List<Bin> result = new ArrayList<Bin>();

		int cores = Math.min((int) target.coreCount, al.size());

		float max = 0;
		float sum = 0;
		float maxW = 0;
		float sumW = 0;
		float sumWactual = 0;

		estmax -= estmin;

		for (Task t : al) {
			t.est -= estmin;
			sumW += t.work;
			sumWactual += work.getWork((iTask) unmergedToMerged.get(t.task.getNode()), method);
			maxW = Math.max(maxW, t.work);
			sum += t.work + t.est;
			max = Math.max(max, t.work + t.est);
			boolean isDangling = true;
			for (JCTree st : t.task) {
				if (!method.dangling_paths.contains(createMultiNode(st))) {
					isDangling = false;
					break;
				}
			}
			if (isDangling) {
				sumWactual += work.getWork((iTask) unmergedToMerged.get(t.task.getNode()), method);//we assume that something at least as big as the dangling task runs in the background
			}
		}

//		float topt = (float) Math.max(max, Math.ceil(al.size() / cores) * sum / al.size());
		disableErrors = true;

		float C = MULTIFIT(20, cores, al, result, max, sum, maxW, sumW);

		float etpMin = Float.MAX_VALUE;
		int coresBest = cores;
		//this does not necessarily pick the minimum et!
		if (!disablePathMerge) {
			while (cores > 1 && speedup(sumWactual, C - estmax, cores) < Math.max(smin, lin * cores)) //O(cores)*O(Speedup=MULTIFIT) = O(N^2)
			{
				/*
				 float RT=C;
				 int pcount=0;
				 for (Bin b : result) {
				 JCTree fcn=b.merged.getFirstCalcNode(method, b.merged.getCalcNodes());
				 if(fcn.scheduler.getTag()!=JCTree.CF&&fcn.scheduler.getTag()!=JCTree.IF)
				 pcount++;
				 }

				 if(pcount>1&&result.size()>1)
				 RT=etp(RT);

				 if(RT<etpMin)
				 {
				 etpMin=etp(C);
				 coresBest=cores;
				 }
				 */

				cores--;
				coresBest = cores;
				C = MULTIFIT(20, cores, al, result, max, sum, maxW, sumW);
			}
		}

		disableErrors = false;

		C = MULTIFIT(20, coresBest, al, result, max, sum, maxW, sumW); //run once more with errors/warnings enabled
		//with cores=O(N) this is also O(N^3)

		float RT = C;

		int pcount = 0;
		if (!disablePathMerge) {
			for (Bin b : result) {
				JCTree fcn = b.merged.getFirstCalcNode(method, b.merged.getCalcNodes());
				if (fcn.scheduler.getTag() != JCTree.CF && fcn.scheduler.getTag() != JCTree.IF) {
					pcount++;
				}
			}
		} else {
			//RT=0.f;
			for (TaskSet b : nodes) {
				JCTree fcn = b.getFirstCalcNode(method, b.getCalcNodes());
				if (fcn.scheduler.getTag() != JCTree.CF && fcn.scheduler.getTag() != JCTree.IF) {
					pcount++;
				}
				//RT=Math.max(RT,work.getWork((iTask)unmergedToMerged.get(b.getNode()), method));
			}
			for (TaskSet ts : addNodes.keySet()) {
				JCTree fcn = ts.getFirstCalcNode(method, ts.getCalcNodes());
				if (fcn.scheduler.getTag() != JCTree.CF && fcn.scheduler.getTag() != JCTree.IF) {
					pcount++;
				}

			}

		}

//O(N^3) ..we merge max N nodes where merging a node is O(N^2)!
		if (!disablePathMerge) {
			for (Bin b : result) //O(cores)
			{
				if (b.getTasks().size() > 1) {
					mergeParallel(b.getTasks());//O(N^2) //merge non loops
				}
			}
		}

		estmin = Float.MAX_VALUE;
		estmax = 0;
		eftmax = 0;

		nodes = new ArrayList<TaskSet>();

		contains = new LinkedHashSet<TaskSet>();

		if (method.final_value != null) {
			contains.add(createMultiNode(method.final_value));
		}

		//Set<DefaultEdge> es = unmergedGraph.incomingEdgesOf(createMultiNode(t));
		{
			Set<DefaultEdge> es = method.hasseDiagram.incomingEdgesOf(v);

			for (DefaultEdge e : es) {
				if (!contains.contains(method.hasseDiagram.getEdgeSource(e))) {
					TaskSet src = method.hasseDiagram.getEdgeSource(e);
					//for(JCTree st:src)
					nodes.add(createMultiNode(src.getFirstCalcNode(method, src.getCalcNodes()))); //just use one representant
					contains.add(method.hasseDiagram.getEdgeSource(e));
				}
			}
		}

		RT = 0.f;
		//generate all pairs O(N^2)
		for (int i = 0; i < nodes.size(); i++) {
			workMax = 0;
			est = Float.MAX_VALUE;
			for (int j = 0; j < nodes.size(); j++) {
				if (i != j) {
					//get path O(1)
					Set<TaskSet> p1 = new LinkedHashSet<TaskSet>();
					Set<TaskSet> p2 = new LinkedHashSet<TaskSet>();

					CAI ancestor = getParallelNodesAncestor(nodes.get(i), nodes.get(j), p1, p2, method.est, method.eft);//O(N)
					handleAD(ancestor, i, j, p1, p2, nodes, addNodes, true);
				}
			}
			estmin = Math.min(estmin, est);
			RT = Math.max(workMax, RT);
		}

		float PT = RT;

		PT = etp(PT, pcount);

		if (nodes.size() == 1) {
			TaskSet node = nodes.iterator().next();
			estmin = 0.f;
			PT = method.eft.get(unmergedToMerged.get(node.getNode()));
		}

		method.est.put(v, estmin + PT); //cannot start before precond is supplied, RT is relative to estmin
		method.eft.put(v, method.est.get(v) + work.getWork((iTask) v, method));
	}

	//calc max parallelism for md (assumes the branch with more parallelism is taken)
	int getMaxParallelism(SimpleDirectedGraph<TaskSet, DefaultEdge> schedule) {
		int maxTG = 0;

		Map<iTask, Set<iTask>> parallelPaths = new LinkedHashMap<iTask, Set<iTask>>();

		Map<iTask, Set<iTask>> reachMap = new LinkedHashMap<iTask, Set<iTask>>();
		Map<iTask, Set<iTask>> indepMap = new LinkedHashMap<iTask, Set<iTask>>();

		ArrayList<TaskSet> nodes = new ArrayList<TaskSet>();//O(N+M)
		for (Iterator<TaskSet> ni = new TopologicalOrderIterator<TaskSet, DefaultEdge>(schedule); ni.hasNext();) {
			TaskSet v = ni.next();
			nodes.add(v);
		}

		for (iTask ps : nodes) {//O(N)
			reachMap.put(ps, new LinkedHashSet<iTask>());
			indepMap.put(ps, new LinkedHashSet<iTask>());
		}

		//O(N^3)
		for (int i = 0; i < nodes.size(); i++) {//O(N^2)
			for (int j = i + 1; j < nodes.size(); j++) {
				if (i != j && (nodes.get(i).getNode() == null || (nodes.get(i).getNode() != method.final_value && nodes.get(i).getNode() != method))) {
					iTask p1 = nodes.get(i);
					iTask p2 = nodes.get(j);

					boolean p1r = ((TaskSet) p1).isLastReachableFrom(p2); //O(N)
					boolean p2r = ((TaskSet) p2).isLastReachableFrom(p1); //O(N)
					if (p1r) {
						Set<iTask> s = reachMap.get(p2);
						s.add(p1);
						reachMap.put(p2, s);
					}
					if (p2r) {
						Set<iTask> s = reachMap.get(p1);
						s.add(p2);
						reachMap.put(p1, s);
					}
					if (!p1r && !p2r) {

						JCTree smaller = p1.getFirstCalcNode(method, p1.getCalcNodes());
						JCTree bigger = p2.getFirstCalcNode(method, p2.getCalcNodes());

						//check that paths are actually independent (and not in two different branches)
						if (method.topolNodes.get(smaller) > method.topolNodes.get(bigger)) {
							JCTree tmp = smaller;
							smaller = bigger;
							bigger = tmp;
						}

						boolean cfdep = false;

						while (bigger.scheduler != smaller.scheduler) { //O(N)
							if (bigger.getTag() == JCTree.CF) {
								break;
							}
							bigger = bigger.scheduler;
							if (method.topolNodes.get(smaller) > method.topolNodes.get(bigger)) {
								JCTree tmp = smaller;
								smaller = bigger;
								bigger = tmp;
							}
						}
						if (bigger.getTag() == JCTree.CF || smaller.getTag() == JCTree.CF) {
							cfdep = true;
						}

						if (!cfdep) {
							Set<iTask> s = indepMap.get(p1);
							s.add(p2);
							indepMap.put(p1, s);

							s = indepMap.get(p2);
							s.add(p1);
							indepMap.put(p2, s);
						}
					}
				}
			}
		}

		method.maxRecSpawn=0;
		for (iTask ks : indepMap.keySet()) {
			Set<iTask> is = new LinkedHashSet<iTask>(indepMap.get(ks));

			//not all independent paths can run in parallel (they might depend on each other)
			for (iTask ss : indepMap.get(ks)) {//O(N)
				is.removeAll(reachMap.get(ss));//O(N)
			}

			int locRecSpawn=0;
			for (iTask ss : is) {
				for(JCTree t:ss.getCalcNodes())
					locRecSpawn+=findRecursion.getRecursion(t);
			}
			for(JCTree t:ks.getCalcNodes())
				if(t!=method)
					locRecSpawn+=findRecursion.getRecursion(t);

			method.maxRecSpawn= Math.max(method.maxRecSpawn, locRecSpawn);

			maxTG = Math.max(maxTG, is.size());
			parallelPaths.put(ks, is);
		}

		return maxTG;
	}

	float calcFinishTime(SimpleDirectedGraph<TaskSet, DefaultEdge> schedule, Map<iTask, Float> eftmap, Map<iTask, Float> estmap, TaskSet v, boolean realistic) {
		if (!realistic) {
			return 0;
		}

		ArrayList<TaskSet> nodes = new ArrayList<TaskSet>();

		Set<TaskSet> contains = new LinkedHashSet<TaskSet>();

		estmin = Float.MAX_VALUE;
		estmax = 0;
		eftmax = 0;

		nodes = new ArrayList<TaskSet>();

		contains = new LinkedHashSet<TaskSet>();

		if (method.final_value != null) {
			contains.add(createMultiNode(method.final_value));
		}

		//Set<DefaultEdge> es = unmergedGraph.incomingEdgesOf(createMultiNode(t));
		{
			Set<DefaultEdge> es = method.hasseDiagram.incomingEdgesOf(v);

			for (DefaultEdge e : es) {
				if (!contains.contains(method.hasseDiagram.getEdgeSource(e))) {
					TaskSet src = method.hasseDiagram.getEdgeSource(e);
					//for(JCTree st:src)
					nodes.add(createMultiNode(src.getFirstCalcNode(method, src.getCalcNodes()))); //just use one representant
					contains.add(method.hasseDiagram.getEdgeSource(e));
				}
			}
		}

		Map<TaskSet, ExraTask> addNodes = new LinkedHashMap<TaskSet, ExraTask>();

		float PT = 0.f;
		//generate all pairs O(N^2)
		for (int i = 0; i < nodes.size(); i++) {
			workMax = 0;
			est = Float.MAX_VALUE;
			for (int j = 0; j < nodes.size(); j++) {
				if (i != j) {
					//get path O(1)
					Set<TaskSet> p1 = new LinkedHashSet<TaskSet>();
					Set<TaskSet> p2 = new LinkedHashSet<TaskSet>();

					CAI ancestor = getParallelNodesAncestor(nodes.get(i), nodes.get(j), p1, p2, estmap, eftmap);//O(N)
					handleAD(ancestor, i, j, p1, p2, nodes, addNodes, false);
				}
			}

			PT = Math.max(workMax, PT);
			estmin = Math.min(estmin, est);
		}

		for (TaskSet ts : addNodes.keySet()) {
			PT = Math.max(addNodes.get(ts).w, PT);
		}

		int pcount = 0;
		for (TaskSet b : nodes) {
			JCTree fcn = b.getFirstCalcNode(method, b.getCalcNodes());
			if (fcn.scheduler.getTag() != JCTree.CF && fcn.scheduler.getTag() != JCTree.IF) {
				pcount++;
			}
			//PT=Math.max(PT,work.getWork((iTask)unmergedToMerged.get(b.getNode()), method));
		}

		for (TaskSet ts : addNodes.keySet()) {
			JCTree fcn = ts.getFirstCalcNode(method, ts.getCalcNodes());
			if (fcn.scheduler.getTag() != JCTree.CF && fcn.scheduler.getTag() != JCTree.IF) {
				pcount++;
			}

		}

		PT = PT;
		PT = etp(PT, pcount);

		if (nodes.size() == 1) {
			TaskSet node = nodes.iterator().next();
			estmin = 0.f;
			PT = eftmap.get(unmergedToMerged.get(node.getNode()));
		}
		//est.put(v, estmax+PT); //cannot start before precond is supplied, RT is relative to estmin
		//eft.put(v, est.get(v) + work.getWork((iTask) v, method));

		return estmin + PT;
	}

	//predict schedule exec time on our model with infinitely many cores
	//when realistic is false, we do optimal scheduling on inf cores (a node is executed as soon as all inputs ar ready) (already O(N^2))
	//when realistic is true, we also model scheduling overhead
	float predictSET(SimpleDirectedGraph<TaskSet, DefaultEdge> schedule, boolean realistic) {

		//APSP oldapsp=apsp;
		//apsp=new APSP(schedule);
		Map<iTask, Float> eft = new LinkedHashMap<iTask, Float>();
		Map<iTask, Float> est = new LinkedHashMap<iTask, Float>();
		//float last = 0.f;

		for (Iterator<TaskSet> i = new TopologicalOrderIterator<TaskSet, DefaultEdge>(schedule); i.hasNext();) {
			TaskSet node = i.next();

			int in = schedule.inDegreeOf(node);

			if (in == 0) {
				est.put(node, 0.f);
				eft.put(node, est.get(node) + work.getWork((iTask) node, method));//only method entry
			} else if (in == 1) {
				TaskSet src = method.hasseDiagram.getEdgeSource(method.hasseDiagram.incomingEdgesOf(node).iterator().next());

				Set<JCTree> incom = node.getInComImplicit();

				float eftval;
				if (!incom.isEmpty()) //figure out exact spawn time (spawns do not always happen at the end of a task node)
				{
					eftval = est.get(src);
					JCBlock b = src.getPathBlock();
					for (JCStatement s : b.stats) {
						eftval += work.getWork(s, method);
						incom.remove(s);
						if (incom.isEmpty()) {
							break;
						}
					}
				} else {
					eftval = eft.get(src);
				}

				est.put(node, eftval);
				eft.put(node, est.get(node) + work.getWork((iTask) node, method));
			} else {
				float predft = 0.f;
				for (DefaultEdge e : schedule.incomingEdgesOf(node)) {
					predft = Math.max(predft, eft.get(schedule.getEdgeSource(e)));
				}
				predft = Math.max(predft, calcFinishTime(schedule, eft, est, node, realistic));
				est.put(node, predft);
				eft.put(node, est.get(node) + work.getWork((iTask) node, method));//translate task time into real time
			}

			//last = eft.get(node);
		}

		if (realistic) {
			dumpGraphEFT("SET", est, eft);
		}

		//apsp=oldapsp;
		return eft.get(createMultiNode(method.dg_end));//eft from sink
	}

	//stuff to dump dot file
	class NOP<T> implements ComponentAttributeProvider<T> {

		public Map<String, String> getComponentAttributes(T component) {
			return new LinkedHashMap<String, String>();
		}
	}

	class BoxVertex implements ComponentAttributeProvider<TaskSet> {

		public Map<String, String> getComponentAttributes(TaskSet component) {
			Map<String, String> properties = new LinkedHashMap<String, String>();
			properties.put("shape", "box");
			return properties;
		}
	}

	class WeightedBoxVertex implements ComponentAttributeProvider<JCTree> {

		public Map<String, String> getComponentAttributes(JCTree component) {
			Map<String, String> properties = new LinkedHashMap<String, String>();
			properties.put("shape", "box");
			return properties;
		}
	}

	class EmptyEdge<E> implements EdgeNameProvider<E> {

		public String getEdgeName(E edge) {
			return "";
		}
	}

	void dumpWeightedGraph(String name) {
		if (!dumpMerge) {
			return;
		}

		class EFTNameProvider implements VertexNameProvider<JCTree> {

			public String getVertexName(JCTree v) {
				return v.toFlatString().replace("\"", "'");
			}
		}

		class EFTEdge implements EdgeNameProvider<Arc> {

			public String getEdgeName(Arc edge) {
				if (edge.v != null) {
					return edge.v.toString();
				} else {
					return "";
				}
			}
		}

		//output.removeVertex(createMultiNode(method.dg_end));
		try {
			JavaFileObject file = fileManager.getJavaFileForOutput(CLASS_OUTPUT,
					method.sym.owner.toString() + "/" + method.toFlatString() + "/" + name,
					JavaFileObject.Kind.DEPGRAPH,
					null);

			(new DOTExporter<JCTree, Arc>(new IntegerNameProvider<JCTree>(), new EFTNameProvider(), new EFTEdge(), new WeightedBoxVertex(), new NOP<Arc>())).export(new BufferedWriter(file.openWriter()), method.depGraph);
		} catch (IOException e) {
		}
	}

	void dumpGraph(String name) {
		if (!dumpMerge) {
			return;
		}

		if (method.eft != null) {
			dumpGraphEFT(name, method.est, method.eft);
			return;
		}

		SimpleDirectedGraph<TaskSet, DefaultEdge> output = (SimpleDirectedGraph<TaskSet, DefaultEdge>) method.hasseDiagram.clone();

		//output.removeVertex(createMultiNode(method.dg_end));
		try {
			JavaFileObject file = fileManager.getJavaFileForOutput(CLASS_OUTPUT,
					method.sym.owner.toString() + "/" + method.toFlatString() + "/" + name,
					JavaFileObject.Kind.DEPGRAPH,
					null);

			(new DOTExporter<TaskSet, DefaultEdge>(new IntegerNameProvider<TaskSet>(), new StringNameProvider<TaskSet>(), new EmptyEdge<DefaultEdge>(), new BoxVertex(), new NOP<DefaultEdge>())).export(new BufferedWriter(file.openWriter()), output);
		} catch (IOException e) {
		}
	}

	void dumpGraphEFT(String name, final Map<iTask, Float> est, final Map<iTask, Float> eft) {
		if (!dumpMerge) {
			return;
		}

		class EFTEdge implements EdgeNameProvider<DefaultEdge> {

			public String getEdgeName(DefaultEdge edge) {
				if (eft.get(method.hasseDiagram.getEdgeSource(edge)) != null) {
					return "drt: " + new java.util.Formatter().format("%.2f", eft.get(method.hasseDiagram.getEdgeSource(edge))).toString();
				} else {
					return "drt: ?";
				}
			}
		}

		class EFTNameProvider implements VertexNameProvider<TaskSet> {

			public String getVertexName(TaskSet v) {
				return "est: " + new java.util.Formatter().format("%.2f", est.get(v)).toString() + "\\n------\\n" + v.toString() + "\\n------\\n" + "w: " + work.getWork((iTask) v, method);
			}
		}
		SimpleDirectedGraph<TaskSet, DefaultEdge> output = (SimpleDirectedGraph<TaskSet, DefaultEdge>) method.hasseDiagram.clone();

		//output.removeVertex(createMultiNode(method.dg_end));
		try {
			JavaFileObject file = fileManager.getJavaFileForOutput(CLASS_OUTPUT,
					method.sym.owner.toString() + "/" + method.toFlatString() + "/" + name,
					JavaFileObject.Kind.DEPGRAPH,
					null);

			(new DOTExporter<TaskSet, DefaultEdge>(new IntegerNameProvider<TaskSet>(), new EFTNameProvider(), new EFTEdge(), new BoxVertex(), new NOP<DefaultEdge>())).export(new BufferedWriter(file.openWriter()), output);
		} catch (IOException e) {
		}
	}

	//O(n+m)
	void removeLinearChains(boolean allowMergeExit) {
		for (Iterator<TaskSet> i = new TopologicalOrderIterator<TaskSet, DefaultEdge>(method.hasseDiagram); i.hasNext();) {
			TaskSet node = i.next();
			JCTree n = node.getNode();
			if (allowMergeExit && n != null && n.nop_if_alone) //merge decls with scheduler
			{
				node = mergeParallel(createMultiNode(n.scheduler), node);
			} else {
				fixLinearChainPrev(node, allowMergeExit);
			}
		}
	}

	class Bin {

		private Set<TaskSet> tasks = new LinkedHashSet<TaskSet>();
		public float length;
		public TaskSet merged = null;

		public void add(TaskSet t) {
			tasks.add(t);
			if (merged == null) {
				merged = (TaskSet) t.clone();
			} else {
				merged.addAll(t);
			}
		}

		public Set<TaskSet> getTasks() {
			return tasks;
		}

		public Bin(float length) {
			this.length = length;
		}

		public String toString() {
			return merged.toString() + " : " + length;
		}
	}

	class Task {

		public TaskSet task;
		public float work;
		public float est;

		public Task(TaskSet task, float work, float est) {
			this.task = task;
			this.work = work;
			this.est = est;
		}

		public String toString() {
			return task.toString() + ":" + work;
		}
	}

	boolean mayMerge(Bin b, float binlength, Task t) {

		if (binlength - b.length < t.work + t.est) { //we fill in backwards (decreasing eft) only if task + est fits before stuff already inside
			return false;
		}

		TaskSet p1 = b.merged;

		if (p1 == null) {
			return true;
		}

		TaskSet p2 = t.task;

		Set<JCTree> cn1 = p1.getCalcNodes();
		Set<JCTree> cn2 = p2.getCalcNodes();

		JCTree fcn1 = p1.getFirstCalcNode(method, cn1);
		JCTree fcn2 = p2.getFirstCalcNode(method, cn2);

		if (!mayMerge(fcn1, fcn2, p1, p2)) {
			return false;
		}

		Set<JCPragma> pragmas = checkPragma(p1, p2);

		if (HandlePragma(pragmas, "(" + p1.getPathBlockNoCache() + "|" + p2.getPathBlockNoCache() + ")")) {
			return false;
		}

		return true;
	}

	//first fit bin packing, O(NlogN)
	int FF(float binlength, java.util.List<Task> tasks, java.util.List<Bin> result) {
		result.clear();
		result.add(new Bin(0));

		for (Task task : tasks) {
			float w = task.work;
			int cur = 0;
			while (cur < result.size() && !mayMerge(result.get(cur), binlength, task)) {
				cur++;
			}

			if (cur >= result.size()) {
				if (w <= binlength) {
					result.add(new Bin(0));
				} else {
					return 0;
				}
			}

			result.get(cur).add(task.task);
			result.get(cur).length += w; //only add work (not est) to end of bin
		}
		int correction = 0;
		for (Bin b : result) {
			if (b.length < 0.0) {
				correction += b.tasks.size() - 1;
			}
		}
		return result.size() + correction;
	}

	static int sign(float f) {
		if (f < 0.f) {
			return -1;
		} else if (f > 0.f) {
			return 1;
		}

		return 0;
	}

	//return with max bount 1.220*OPT+1/2^k the best fit of the tasks into c bins (map tasks onto cores) O(NlogN+kNC)
	float MULTIFIT(int k, int c, java.util.List<Task> tasks, java.util.List<Bin> result, float max, float sum, float maxW, float sumW)//k gives max iterations
	{
		//binary search for best bound
		boolean disabled = disableErrors;
		disableErrors = true;
		//sort tasks once! //O(NlogN)
		Collections.sort(tasks, new Comparator<Task>() {
			public int compare(Task a1, Task a2) {
				return sign(a2.work + a2.est - (a1.work + a1.est));
			}
		});

		float Cl = Math.max(sumW / c, maxW);
		float Cu = Math.max(2 * (sum / c), max);

		float C;

		float Cbest = Cu;//store best solution
		int dist = Integer.MAX_VALUE;

		for (int i = 0; i < k; i++) {
			C = (Cl + Cu) / 2.f;
			result.clear();
			int b = FF(C, tasks, result);
			if (b <= c) {
				int delta = c - b;
				if (delta < dist || delta == dist && C < Cbest) {
					Cbest = C;
					dist = delta;
				}
				Cu = C;
			} else {
				int delta = b - c;
				if (delta < dist || delta == dist && C < Cbest) {
					Cbest = C;
					dist = delta;
				}
				Cl = C;
			}
		}

		result.clear();
		disableErrors = disabled;

		FF(Cbest, tasks, result);

		return Cbest;
	}

	void doDepGraphImplForPrettyPrint(JCMethodDecl tree) {
		method.topolNodes = new java.util.LinkedHashMap<JCTree, Integer>();

		//construct Hasse Graph and build topol order list
		method.depGraphImplicit = new DirectedWeightedMultigraph<JCTree, Arc>(Arc.class);

		for (Iterator<JCTree> i = new TopologicalOrderIterator<JCTree, Arc>(tree.depGraph); i.hasNext();) {
			JCTree node = i.next();

			//assign unique topol ordered id for shortest paths
			method.topolNodes.put(node, method.topolNodes.size());

			//construct depGraphImplicit where only deps on the node with highest topology are stored
			method.depGraphImplicit.addVertex(node);
			ArrayList<Arc> list = new ArrayList<Arc>(tree.depGraph.incomingEdgesOf(node));
			if (list.size() > 0) {
				Collections.sort(list, new Comparator<Arc>() {
					public int compare(Arc a1, Arc a2) {
						return method.topolNodes.get(a2.s) - method.topolNodes.get(a1.s);
					}
				});
				for (Arc a : list) {
					if (!a.s.getDGNode().IsReachable(verbose, node, method.topolNodes, method.depGraphImplicit, true)) {
						method.depGraphImplicit.addEdge(a.s, node, new Arc(a.s, node, a.v));
					}
				}
			}
		}

	}

	APSP apsp = null;
	//Set<Pair<TaskSet,TaskSet>> done=null;
	Map<Pair<TaskSet, TaskSet>, TaskSet> todo = null;
	DeferableTopolOrder dto = null;

	//parallelism is handled for each method seperatly (one Task Graph per Method)
	//assumes COM = 0
	public void visitMethodDef(JCMethodDecl tree) {
		method = tree;

		if (!tree.analyse()) { //do not analyze templates or empty constructors
			return;
		}

		if (dumpMerge) {
			System.err.println(method.toFlatString());
		}

		count = 0;

		doDepGraphImplForPrettyPrint(tree);

		//done=new LinkedHashSet<Pair<TaskSet, TaskSet>>();
		//construct Hasse Graph and build topol order list
		dumpWeightedGraph("weighted");
		method.hasseDiagram = transitiveReduction(method.depGraph); //in theory : O(n*(n+m))

		//here each node is a set of just one item!
		unmergedGraph = new SimpleDirectedGraph<TaskSet, DefaultEdge>(DefaultEdge.class);
		for (TaskSet v : method.hasseDiagram.vertexSet()) {
			unmergedGraph.addVertex(v);
		}
		for (DefaultEdge e : method.hasseDiagram.edgeSet()) {
			unmergedGraph.addEdge(method.hasseDiagram.getEdgeSource(e), method.hasseDiagram.getEdgeTarget(e));
		}

		//setup mapping from unmerged nodes to merged node
		unmergedToMerged = new LinkedHashMap<JCTree, TaskSet>();

		//O(N)
		for (TaskSet v : method.hasseDiagram.vertexSet()) {
			JCTree t = v.iterator().next();
			unmergedToMerged.put(t, createMultiNode(t));
		}

		tree.dangling_paths = new LinkedHashSet<iTask>();
		for (DefaultEdge e : tree.hasseDiagram.incomingEdgesOf(createMultiNode(tree.dg_end))) {
			TaskSet ts = tree.hasseDiagram.getEdgeSource(e);
			boolean returns = false;
			JCTree sched = ts.getFirstCalcNode(method, ts.getCalcNodes()).scheduler;
			if (sched.getTag() == JCTree.CF && ((JCCF) sched).condition == null) {
				returns = true;
			} else {
				for (JCTree t : ts) {
					Arc a = method.depGraphImplicit.getEdge(t, tree.dg_end);
					if (a == null || a.v != null) {
						returns = true;
					}
					if (t == method || t.getTag() == JCTree.RETURN) {
						returns = true;
						break;
					}
				}
			}
			if (!returns) //all non returning paths
			{
				TaskSet dt = tree.hasseDiagram.getEdgeSource(e);
				float workDT = work.getWorkAll(dt, method);
				if (speedup(2.f * workDT, workDT, 2) > Math.max(smin, lin * 2)) {
					tree.dangling_paths.add(dt);
				}
			}
		}

		apsp = new APSP(method.hasseDiagram);//APSP O(N^3)

		parallelComputationAncestor = new LinkedHashMap<Pair<TaskSet, TaskSet>, Loop>();
		parallelComputationDescendend = new LinkedHashMap<Pair<TaskSet, TaskSet>, Loop>();

		//precalc common ancestor: theory O(n^3) (or best mat mult)
		//precacl shortest paths O(N^3)
		//precalc parallel comp O(N^3)
		//O(N^2): all vertex pairs
		for (TaskSet v : method.hasseDiagram.vertexSet()) {
			for (TaskSet w : method.hasseDiagram.vertexSet()) {
				if (v != w) {
					TaskSet ca = findCommonAncestor(v, w);//O(N) //here we choose ca with max distance (should include node wheights)
					//O(N):
					//GraphPath<TaskSet, DefaultEdge> gp1 = (new DijkstraShortestPath<TaskSet, DefaultEdge>(method.hasseDiagram, ca, v)).getPath();
					//GraphPath<TaskSet, DefaultEdge> gp2 = (new DijkstraShortestPath<TaskSet, DefaultEdge>(method.hasseDiagram, ca, w)).getPath();

					java.util.List<TaskSet> gp1 = apsp.getPath(ca, v);
					java.util.List<TaskSet> gp2 = apsp.getPath(ca, w);

					java.util.List<TaskSet> nodes1 = new ArrayList<TaskSet>();
					java.util.List<TaskSet> nodes2 = new ArrayList<TaskSet>();

					TaskSet n1 = null;
					if (!gp1.isEmpty()) {
						n1 = gp1.get(0);
					}
					TaskSet n2 = null;
					if (!gp2.isEmpty()) {
						n2 = gp2.get(0);
					}

					TaskSet m1 = null;
					TaskSet m2 = null;

					for (TaskSet e : gp1) {
						nodes1.add(e);
						if (mayMerge(e, n1)) {
							n1 = e;
						}
						m1 = e;
					}

					for (TaskSet e : gp2) {
						nodes2.add(e);
						if (mayMerge(e, n2)) {
							n2 = e;
						}
						m2 = e;
					}

					parallelComputationAncestor.put(new Pair<TaskSet, TaskSet>(v, w), new Loop(new Pair<TaskSet, TaskSet>(m1, m2), ca, n1, n2));

					TaskSet cd = findCommonDescendend(v, w);//O(N) //here we choose cd with max distance (should include node wheights)
					if (cd != null) {
						gp1 = apsp.getPath(v, cd);
						gp2 = apsp.getPath(w, cd);

						nodes1 = new ArrayList<TaskSet>();
						nodes2 = new ArrayList<TaskSet>();

						n1 = null;
						if (!gp1.isEmpty()) {
							n1 = gp1.get(0);
						}
						m1 = n1;
						n2 = null;
						if (!gp2.isEmpty()) {
							n2 = gp2.get(0);
						}
						m2 = n2;

						for (TaskSet e : gp1) {
							if (e != cd) //exclude cd and start nodes
							{
								nodes1.add(e);
								if (mayMerge(e, n1)) {
									n1 = e;
								}
							}
						}

						for (TaskSet e : gp2) {
							if (e != cd) {
								nodes2.add(e);
								if (mayMerge(e, n2)) {
									n2 = e;
								}
							}
						}
						parallelComputationDescendend.put(new Pair<TaskSet, TaskSet>(v, w), new Loop(new Pair<TaskSet, TaskSet>(m1, m2), cd, n1, n2));
					}
				}
			}
		}

		predecessors = new LinkedHashMap<JCTree, Set<JCTree>>();

		for (DefaultEdge e : method.hasseDiagram.edgeSet())//O(M)
		{
			Set<JCTree> preds = predecessors.get(method.hasseDiagram.getEdgeTarget(e).getNode());

			if (preds == null) {
				preds = new LinkedHashSet<JCTree>();
			}

			preds.add(method.hasseDiagram.getEdgeSource(e).getNode());

			predecessors.put(method.hasseDiagram.getEdgeTarget(e).getNode(), preds);
		}

		dumpGraph("hasse");

		if (method.final_value != null)//finally must be processed seperately
		{
			TaskSet fin = createMultiNode(method.final_value);//brakes hasse!
			for (DefaultEdge i : method.hasseDiagram.incomingEdgesOf(fin)) {
				for (DefaultEdge o : method.hasseDiagram.outgoingEdgesOf(fin)) {
					method.hasseDiagram.addEdge(method.hasseDiagram.getEdgeSource(i), method.hasseDiagram.getEdgeTarget(o));
				}
			}
			method.hasseDiagram.removeVertex(fin);
			method.hasseDiagram = transitiveReduction(method.hasseDiagram); //fix hasse
		}

		dumpGraph("hasse_no_final");

		//remove unnecessary linear chains
		removeLinearChains(false); //in theory : O(n+m)

		//must run predictSET after first removeLinearChains to remove linear deps
		method.pUET = predictSET(method.hasseDiagram, true);
		method.pUUET = predictSET(method.hasseDiagram, false);
		method.sym.NaiveTGWidth = getMaxParallelism(method.hasseDiagram) + 1;

		dumpGraph("hasse_lin");
		//this is the initial state of our graph, setup precalc

		method.est = new LinkedHashMap<iTask, Float>();
		method.eft = new LinkedHashMap<iTask, Float>();
		//visit all join nodes
//O(N^3)
		//O(n+m)

		dto = new DeferableTopolOrder(method.hasseDiagram);

		todo = new LinkedHashMap<Pair<TaskSet, TaskSet>, TaskSet>();

		TaskSet node = dto.next();

		do {
			TaskSet dtonode = node; //node may be overwritten later
			TaskSet original = (TaskSet) node.clone();

			int in = method.hasseDiagram.inDegreeOf(node);

			if (in == 0) {
				method.est.put(node, 0.f);//only method entry
				method.eft.put(node, method.est.get(node) + work.getWork((iTask) node, method));//only method entry
			} else if (in == 1) {
				TaskSet src = method.hasseDiagram.getEdgeSource(method.hasseDiagram.incomingEdgesOf(node).iterator().next());

				Set<JCTree> incom = dtonode.getInComImplicit();

				float eftval;
				if (!incom.isEmpty()) //figure out exact spawn time (spawns do not always happen at the end of a task node)
				{
					eftval = method.est.get(src);
					JCBlock b = src.getPathBlock();
					for (JCStatement s : b.stats) {
						eftval += work.getWork(s, method);
						incom.remove(s);
						if (incom.isEmpty()) {
							break;
						}
					}
				} else {
					eftval = method.eft.get(src);
				}

				method.est.put(node, eftval);
				method.eft.put(node, method.est.get(node) + work.getWork((iTask) node, method));

				//may also need to remove here
				if (node.getNode() != null)//if node is a merged node then it has no alternative siblings
				{
					for (Iterator<Pair<TaskSet, TaskSet>> ipair = todo.keySet().iterator(); ipair.hasNext();) {
						Pair<TaskSet, TaskSet> pair = ipair.next();

						if (node.getNode().scheduler == pair.fst.getNode().scheduler && apsp.reachable(pair.fst, node) && apsp.reachable(pair.snd, node)) {
							if (todo.get(pair).equals(node)) {
								ipair.remove();
							}
						}
					}
				}
			}

			node = fixLinearChainPrev(node, false); //O(N)

			if (method.hasseDiagram.inDegreeOf(node) > 1) {
				MergeTasks(node, tree);
			}

			if (!dto.isPostponed()) {
				TaskSet next = fixLinearChainPrev(node, false); //O(N)

				if (next != node && method.hasseDiagram.inDegreeOf(next) > 1) {
					MergeTasks(next, tree);
				}

				if (!dto.isPostponed()) {
					for (TaskSet ts : dto.getDone()) //check if pair if current and prev node leads to a highest common descendent that must be processed first
					{
						JCTree max1 = ts.getLastCalcNode(method, ts.getCalcNodes());
						JCTree max2 = dtonode.getLastCalcNode(method, dtonode.getCalcNodes());

						if (!apsp.reachable(createMultiNode(max1), createMultiNode(max2)) && !apsp.reachable(createMultiNode(max2), createMultiNode(max1))) {
							Loop l = parallelComputationDescendend.get(new Pair(createMultiNode(max1), createMultiNode(max2)));
							if (l == null) {
								l = parallelComputationDescendend.get(new Pair(createMultiNode(max2), createMultiNode(max1)));
							}

							if (l != null) {
								todo.put(new Pair(createMultiNode(max1), createMultiNode(max2)), l.commonAncestor);
							}
						}
					}
				}

				node = next;
			} else {
				int i = 0;
			}

			dto.finish(original);
			node = dto.next();
		} while (node != null);

		dumpGraph("merged");

		//patch finally back in
		if (method.final_value != null) {

			TaskSet fin = createMultiNode(method.final_value);
			method.hasseDiagram.addVertex(fin);

			Set<Pair<TaskSet, TaskSet>> toBeremoved = new HashSet<Pair<TaskSet, TaskSet>>();

			for (DefaultEdge a : unmergedGraph.outgoingEdgesOf(fin)) {
				TaskSet childOfFinal = unmergedGraph.getEdgeTarget(a);
				if (!childOfFinal.toString().equals(";")) {
					for (DefaultEdge b : method.hasseDiagram.incomingEdgesOf(unmergedToMerged.get(childOfFinal.getNode()))) {
						if (!method.hasseDiagram.getEdgeSource(b).equals(fin)) {
							toBeremoved.add(new Pair<TaskSet, TaskSet>(method.hasseDiagram.getEdgeSource(b), unmergedToMerged.get(childOfFinal.getNode())));
						}
					}
				}
			}

			for (Pair<TaskSet, TaskSet> p : toBeremoved) {
				method.hasseDiagram.removeEdge(p.fst, p.snd);
			}

			dumpGraph("fix_final_childs");

			float est = 0;

			for (DefaultEdge a : unmergedGraph.incomingEdgesOf(fin)) {
				if (!unmergedToMerged.get(unmergedGraph.getEdgeSource(a).getNode()).equals(fin)) {
					est = Math.max(est, method.eft.get(unmergedToMerged.get(unmergedGraph.getEdgeSource(a).getNode())));
					method.hasseDiagram.addEdge(unmergedToMerged.get(unmergedGraph.getEdgeSource(a).getNode()), fin);
				}
			}
			for (DefaultEdge a : unmergedGraph.outgoingEdgesOf(fin)) {
				if (!unmergedToMerged.get(unmergedGraph.getEdgeTarget(a).getNode()).equals(fin)) {
					method.hasseDiagram.addEdge(fin, unmergedToMerged.get(unmergedGraph.getEdgeTarget(a).getNode()));
				}
			}

			method.est.put(fin, est);
			method.eft.put(fin, method.est.get(fin) + work.getWork(method.final_value, method));

			if (method.hasseDiagram.inDegreeOf(fin) > 1) {
				count = 100;
				MergeTasks(fin, tree);
			}
		}

		dumpGraph("patched");

		//remove unnecessary linear chains
		removeLinearChains(true); //in theory : O(n+m)

		//method.pPET = predictSET(method.hasseDiagram,true);
		method.pPET = method.eft.get(createMultiNode(method.dg_end));

		dumpGraphEFT("final", method.est, method.eft);
		/**
		 * Andreas Wagner
		 */
		method.hasseFinal = (SimpleDirectedGraph<TaskSet, DefaultEdge>) method.hasseDiagram.clone();

		generatePaths(tree, method.hasseDiagram); //destroys hasse property and others

		//FIXME: genere deps from cf target to if node in case cf target is not spawned!
	}

	boolean isNopOnlyTask(TaskSet node) {
		JCTree t = node.getNode();

		if (t != null) {
			if (t.getTag() == JCTree.SKIP || t == method.final_value || t == method) {
				return true;
			}
		}

		Set<JCTree> scn = node.getCalcNodes();
		//take care to remove NOP paths!!
		JCTree fcn = node.getFirstCalcNode(method, scn);
		boolean skipall = fcn.nop;
		if (skipall) {
			for (JCTree cn : scn)//remove all NOP only paths (some "empty" paths will remain, containing IF->CF)
			{
				if (!cn.nop) {
					skipall = false;
					break;
				}
			}
		}

		return skipall;
	}

	void generatePaths(JCMethodDecl tree, SimpleDirectedGraph<TaskSet, DefaultEdge> g) {
		//optimize g so that one task is calculated locally (no spawn)
		Set<TaskSet> processed = new LinkedHashSet<TaskSet>();
		Set<com.sun.tools.javac.util.List<TaskSet>> toBeMerged = new LinkedHashSet<com.sun.tools.javac.util.List<TaskSet>>();

		//ALEX: optimization disabled for task prediction!!
		//if(false)
		for (Iterator<TaskSet> i = new TopologicalOrderIterator<TaskSet, DefaultEdge>(method.hasseDiagram); i.hasNext();) {
			TaskSet node = i.next();

			if (!processed.contains(node)) {
				ListBuffer<TaskSet> list = new ListBuffer<TaskSet>();
				list.add(node);
				TaskSet prev = node;
				while (method.hasseDiagram.outDegreeOf(node) >= 1) {
					for (DefaultEdge e : method.hasseDiagram.outgoingEdgesOf(node)) {
						node = method.hasseDiagram.getEdgeTarget(e);
						if (method.hasseDiagram.inDegreeOf(node) == 1 && mayMerge(prev, node)) {
							break;
						}
					}
					if (method.hasseDiagram.inDegreeOf(node) > 1) {
						break;
					}
					processed.add(node);
					list.add(node);
					prev = node;
				}
				toBeMerged.add(list.toList());
			}
		}

		for (com.sun.tools.javac.util.List<TaskSet> list : toBeMerged) {
			TaskSet m = list.head;
			for (TaskSet t : list.tail) {
				m = mergeLinear(m, t, true);
			}
		}

		dumpGraph("optimized");

		method.sym.LocalTGWidth = getMaxParallelism(method.hasseDiagram) + 1;

		//prepare for execution
		for (Iterator<TaskSet> i = new TopologicalOrderIterator<TaskSet, DefaultEdge>(method.hasseDiagram); i.hasNext();) {
			TaskSet node = i.next();

			if (!isNopOnlyTask(node)) {
				method.getHasseSchedules().add(node);

				Set<JCTree> cn = node.getCalcNodes();

				//handle groups and threads
				((ClassSymbol) (method.sym.owner)).taskGroups.addAll(node.getGroups());
				((ClassSymbol) (method.sym.owner)).taskThreads.addAll(node.getThreads());

				JCTree fcn = node.getFirstCalcNode(method, cn);
				if (fcn.scheduler != null) {
					fcn.scheduler.getSchedule().add(node); //mark that scheduler should spawn the path starting with node
				}
			}
		}

		tree.dangling_paths = new LinkedHashSet<iTask>();
		for (DefaultEdge e : tree.hasseDiagram.incomingEdgesOf(createMultiNode(tree.dg_end))) {
			TaskSet ts = tree.hasseDiagram.getEdgeSource(e);
			boolean returns = false;
			for (JCTree t : ts) {
				if (t.getTag() == JCTree.RETURN) {
					returns = true;
					break;
				}
			}
			if (!returns) //all non returning paths
			{
				tree.dangling_paths.add(tree.hasseDiagram.getEdgeSource(e));
			}
		}

		tree.spawned_dangling_paths = new LinkedHashSet<iTask>();
		for (DefaultEdge e : tree.hasseDiagram.incomingEdgesOf(createMultiNode(tree.dg_end))) {
			TaskSet ts = tree.hasseDiagram.getEdgeSource(e);
			boolean returns = false;
			JCTree sched = ts.getFirstCalcNode(method, ts.getCalcNodes()).scheduler;
			if (sched.getTag() == JCTree.CF && ((JCCF) sched).condition == null) {
				returns = true;
			} else {
				for (JCTree t : ts) {
					Arc a = method.depGraphImplicit.getEdge(t, tree.dg_end);
					if (a == null || a.v != null) {
						returns = true;
					}
					if (t == method || t.getTag() == JCTree.RETURN) {
						returns = true;
						break;
					}
				}
			}
			if (!returns) //all non returning paths
			{
				TaskSet dt = tree.hasseDiagram.getEdgeSource(e);
				float workDT = work.getWorkAll(dt, method);
				if (speedup(2.f * workDT, workDT, 2) > Math.max(smin, lin * 2)) {
					tree.spawned_dangling_paths.add(dt);
				}
			}
		}

		//generate impl with no parallelism
		if (method.sym.mayBeRecursive && target.coreCount != 1 && method.hasSchedulerPaths(method.getAllSchedules()) >= 1) {
			//cloning is not quite trivial!
			JCMethodDecl md = (JCMethodDecl) method.cloneForPathGen(names.fromString(method.name.toString() + "__IMPL1"), copy);

			method.restricted_impls = new LinkedHashMap<Integer, JCMethodDecl>();

			method.restricted_impls.put(1, md);

			float oldcc = target.coreCount;
			float oldoh = target.threadOverhead;
			boolean oldap = target.allowPragma;

			target.coreCount = 1;
			target.threadOverhead = Float.MAX_VALUE;//no parallelism
			target.allowPragma = false; //so force doesn't overwrite coreCount

			boolean old = disablePathMerge;
			disablePathMerge = false;
			visitMethodDef(md);
			disablePathMerge = old;

			target.allowPragma = oldap;
			target.threadOverhead = oldoh;
			target.coreCount = oldcc;
		}
	}

	/**
	 * ************************************************************************
	 * main method ***********************************************************************
	 */
	/**
	 * Perform definite assignment/unassignment analysis on a tree.
	 */
	public void analyzeTree(JCTree tree, TreeMaker make, Target target, JavaFileManager fileManager) {
		try {
			this.make = make;
			this.target = target;
			this.fileManager = fileManager;
			scan(tree);
		} finally {
			// note that recursive invocations of this method fail hard
			this.make = null;
		}
	}

	public Map<iTask, iTask> getUnoptimizedToOptimized() {
		return unoptimizedToOptimized;
	}
}
