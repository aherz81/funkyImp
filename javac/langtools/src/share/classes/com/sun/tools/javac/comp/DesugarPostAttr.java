

package com.sun.tools.javac.comp;

import java.util.*;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.code.Type.*;

import com.sun.tools.javac.jvm.Target;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.TypeTags.*;
import static com.sun.tools.javac.jvm.ByteCodes.*;

/** This pass translates away some syntactic sugar: >>> operator.
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class DesugarPostAttr extends TreeTranslator {
    protected static final Context.Key<DesugarPostAttr> syntaxdesugarKey =
        new Context.Key<DesugarPostAttr>();

    public static DesugarPostAttr instance(Context context) {
        DesugarPostAttr instance = context.get(syntaxdesugarKey);
        if (instance == null)
            instance = new DesugarPostAttr(context);
        return instance;
    }

    private Names names;
    private Log log;
    private Symtab syms;
    private Resolve rs;
    private Check chk;
    private Attr attr;
    private TreeMaker make;
    private TreeCopier copy;
    private DiagnosticPosition make_pos;
    private LlvmClassWriter writer;
    private ClassReader reader;
    private ConstFold cfolder;
    private Target target;
    private Source source;
    private boolean allowEnums;
    private final Name dollarAssertionsDisabled;
    private final Name classDollar;
    private Types types;
    private boolean debugLower;

    //private Hashtable<String,JCTree> staticForIdents = new Hashtable<String,JCTree>(301, 0.5f);

    protected DesugarPostAttr(Context context) {
        context.put(syntaxdesugarKey, this);
        names = Names.instance(context);
        log = Log.instance(context);
        syms = Symtab.instance(context);
        rs = Resolve.instance(context);
        chk = Check.instance(context);
        attr = Attr.instance(context);
        make = TreeMaker.instance(context);
        copy = new TreeCopier(make);
        writer = LlvmClassWriter.instance(context);
        reader = ClassReader.instance(context);
        cfolder = ConstFold.instance(context);
        target = Target.instance(context);
        source = Source.instance(context);
        allowEnums = source.allowEnums();
        dollarAssertionsDisabled = names.
            fromString(target.syntheticNameChar() + "assertionsDisabled");
        classDollar = names.
            fromString("class" + target.syntheticNameChar());

        types = Types.instance(context);
        Options options = Options.instance(context);
        debugLower = options.get("debugsyntaxdesugar") != null;
    }


    /** A queue of all translated classes.
     */
//    JCTree translated;

    /** Environment for symbol lookup, set by translateTopLevelClass.
     */
    Env<AttrContext> attrEnv;


/**************************************************************************
 * Symbol manipulation utilities
 *************************************************************************/

    /** Report a conflict between a user symbol and a synthetic symbol.
     */
    private void duplicateError(DiagnosticPosition pos, Symbol sym) {
        if (!sym.type.isErroneous()) {
            log.error(pos, "synthetic.name.conflict", sym, sym.location());
        }
    }

/**************************************************************************
 * Tree building blocks
 *************************************************************************/

    /** Equivalent to make.at(pos.getStartPosition()) with side effect of caching
     *  pos as make_pos, for use in diagnostics.
     **/
    TreeMaker make_at(DiagnosticPosition pos) {
        make_pos = pos;
        return make.at(pos);
    }

/**************************************************************************
 * Translation methods
 *************************************************************************/

    /** Visitor method: Translate a single node.
     *  Attach the source position from the old tree to its replacement tree.
     */
    public <T extends JCTree> T translate(T tree) {
        if (tree == null) {
            return null;
        } else {
            make_at(tree.pos());
            T result = super.translate(tree);
            return result;
        }
    }
    
    public void visitDomIter(JCDomainIter tree) {
        super.visitDomIter(tree);
    }

    public void visitBinary(JCBinary tree) {
		super.visitBinary(tree);
        if(tree.apply!=null)
            result = translate(tree.apply);

   }

	public void visitUnary(JCUnary tree) {
		super.visitUnary(tree);
        if(tree.apply!=null)
            result = translate(tree.apply);
   }

/**************************************************************************
 * main method
 *************************************************************************/

    /** Translate a toplevel class and return a list consisting of
     *  the translated class and translated versions of all inner classes.
     *  @param env   The attribution environment current at the class definition.
     *               We need this for resolving some additional symbols.
     *  @param cdef  The tree representing the class definition.
     */
    public JCTree translateTopLevelClass(Env<AttrContext> env, JCTree cdef, TreeMaker make) {
        JCTree translated = null;
        attrEnv = env;
        this.make = make;
        try {
          translated = translate(cdef);
        } finally {
            // note that recursive invocations of this method fail hard
            this.make = null;
        }
        return translated;
    }


}
