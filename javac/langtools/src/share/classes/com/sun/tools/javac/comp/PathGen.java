package com.sun.tools.javac.comp;

import java.util.*;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.code.Type.*;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.JavaCompiler.Target;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;

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
 * <p><b>This is NOT part of any API supported by Sun Microsystems. If you write code that depends
 * on this, you do so at your own risk. This code and its internal interfaces are subject to change
 * or deletion without notice.</b>
 */
public class PathGen
{

}

