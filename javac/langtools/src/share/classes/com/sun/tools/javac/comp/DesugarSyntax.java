

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
public class DesugarSyntax extends TreeTranslator {
    protected static final Context.Key<DesugarSyntax> syntaxdesugarKey =
        new Context.Key<DesugarSyntax>();

    public static DesugarSyntax instance(Context context) {
        DesugarSyntax instance = context.get(syntaxdesugarKey);
        if (instance == null)
            instance = new DesugarSyntax(context);
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

    protected DesugarSyntax(Context context) {
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

    public void visitAssign(JCAssign tree) {
        tree.lhs = translate(tree.lhs);
        tree.rhs = translate(tree.rhs);

        if(tree.lhs.getTag()==JCTree.APPLY)
        {
            JCMethodInvocation mi = ((JCMethodInvocation)tree.lhs);
            mi.setTriggerReturn(tree.rhs,tree.pos());
            result = tree.lhs;
        }
        else
            result = tree;
    }

/*
//must use island grammer for CTReflections
    public void visitCTProperty(JCCTProperty tree)
    {
        //use type(symbol) rather than symbol!

        Type t = attr.attribTree(tree.exp, attrEnv,VAL|VAR|DOM|TYP,Type.noType);
        //t.tsym
        JCExpression exp=t.GetCTP(tree.getName());
        if(exp==null)
        {
            //error: unknown
            log.error(tree.pos(), "unkown.ctp",tree.exp,tree.name);
            tree.exp=translate(tree.exp);
            result=tree;
        }
        else
        {
            result=translate(exp);
        }
    }

    public void visitSelect(JCFieldAccess tree) {
        tree.name = translate(tree.name);
        tree.selected = translate(tree.selected);

        if(tree.name.getTag()==JCTree.APPLY&&((JCMethodInvocation)tree.name).meth.getTag()==JCTree.IDENT)
        {
            JCMethodInvocation meth=((JCMethodInvocation)tree.name);
            tree.name=meth.meth;
            meth.meth=tree;
            result=meth;
        }
        else if(tree.selected.getTag()==JCTree.IDENT&&((JCIdent)tree.selected).name==names.asterisk)
        {
            result = tree.name;
        }
        else
            result = tree;

    }

    public void visitFor(JCFor tree) {
        tree.exp = translate(tree.exp);
        //eval exp to set!

        //Type t = attr.attribTree(tree.exp, attrEnv,VAL|VAR|DOM|TYP,Type.noType);

        if(tree.exp.getTag()!=JCTree.SET)
        {
            log.error(tree.exp.pos(), "ctp.set.expected",tree.exp,tree.exp.getKind());
        }
        else
        {
            //iterate over set (MISSING)
            List<JCExpression> exp_list=((JCSet)tree.exp).getContent();

            for(List<JCExpression> s = exp_list; s.nonEmpty(); s = s.tail)
            {
                //associate current set value with tree.name
                staticForIdents.put(tree.name.toString(), s.head);

                List<JCTree> start;

                if(s.tail.isEmpty())
                    start=tree.content.tail;
                else
                    start=tree.content;

                for(List<JCTree> l = start; l.nonEmpty(); l = l.tail)
                {
                    //items MUST be translated here so
                    inject(translate(copy.copy(l.head))); //inject everything but the first item
                    //copy l.head (because we modify it but might need the unmodified tree later)
                }

                //end iterate over set
                if(!s.tail.isEmpty())
                    staticForIdents.remove(tree.name.toString());

            }

            result = translate(copy.copy(tree.content.head)); //return first item

            //left scope, remove
            staticForIdents.remove(tree.name.toString());
        }
    }
*/

/*
//ALEX: parser doesn't like f.meth<T>() (probably ambigous with a < b), must use <T>meth()
    public void visitApply(JCMethodInvocation tree) {
		//trafo meth<T>() into <T>meth()
		if(tree.meth.getTag()==JCTree.TYPEAPPLY)
		{
			tree.typeargs=((JCTypeApply)tree.meth).arguments;
			tree.meth=((JCTypeApply)tree.meth).clazz;
		}

		super.visitApply(tree);
	}
*/

    public void visitBinary(JCBinary tree) {

		if(tree.getTag()==JCTree.COMPL)
		{
			//convert (a~b)~c .. into join(a,b,c)

			ListBuffer<JCExpression> ops=new ListBuffer<JCExpression>();

			JCExpression lhs=tree;

			while(lhs.getTag()==JCTree.COMPL)
			{
				if(((JCBinary)lhs).rhs.getTag()!=JCTree.DOMITER&&!((JCBinary)lhs).rhs.toString().equals("null"))//we allow null which means we allow under spec
					log.error(((JCBinary)lhs).rhs.pos(),"join.non.iter");
				ops.add(translate(((JCBinary)lhs).rhs));
				lhs=((JCBinary)lhs).lhs;
			}

			ops.add(translate(lhs));

			result=make.Join(ops.toList());
			return;
		}

        tree.lhs = translate(tree.lhs);
        tree.rhs = translate(tree.rhs);

		//convert a >>> b(c,d) into b(c,d,a)
        if(tree.getTag()==JCTree.USR)
        {
            if(tree.rhs.getTag()!=JCTree.APPLY)
            {
                log.error(tree.rhs.pos(), "prob.found.req.1",
                  tree.rhs, tree.rhs, "method invocation","not compatible with operator '>>>'");
            }
            else
            {
				JCMethodInvocation mi = (JCMethodInvocation)tree.rhs;
				JCArgExpression ae = make.ArgExpression(tree.lhs,null);
				mi.args = mi.args.append(ae);
            }
            result = tree.rhs;
        }
        else
		{
            result = tree;
		}
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
