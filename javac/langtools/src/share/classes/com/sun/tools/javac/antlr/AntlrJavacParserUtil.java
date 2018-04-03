/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.javac.antlr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.runtime.Token;

import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.Convert;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;

/**
 * This class hosts methods used by AntlrJavacParser.
 * @author Yang Jiang
 *
 */
public class AntlrJavacParserUtil {
    private Map<JCTree, Integer> endPositions;
    private Map<JCTree, String> docComments;
    private Log log;
    private TreeMaker T;
    private boolean isStoreDocComment;
    private boolean isStoreEndPostion;

    public AntlrJavacParserUtil() {
        this(true, true);
    }

    public AntlrJavacParserUtil(boolean isStoreDocComment, boolean isStoreEndPostion) {
        this.isStoreDocComment = isStoreDocComment;
        this.isStoreEndPostion = isStoreEndPostion;
        if (isStoreDocComment == true) {
            docComments = new LinkedHashMap<JCTree, String>();
        }
        if (isStoreEndPostion == true) {
            endPositions = new LinkedHashMap<JCTree, Integer>();
        }
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public void setTreeMaker(TreeMaker t) {
        this.T = t;
    }

    public Map<JCTree, Integer> getEndPositions() {
        return this.endPositions;
    }

    public Map<JCTree, String> getDocComments() {
        return this.docComments;
    }

    public void storeEnd(JCTree tree, Token token) {
        if (isStoreEndPostion == false) {
            return;
        }
        endPositions.put(tree, ((AntlrJavacToken) token).getStopIndex() + 1);
    }

    public void storeEnd(JCTree tree, int pos) {
        if (isStoreEndPostion == false) {
            return;
        }
        endPositions.put(tree, pos);
    }

    public void attach(JCTree tree, String dc) {
        if (isStoreDocComment == false) {
            return;
        }
        docComments.put(tree, dc);
    }

    @SuppressWarnings("unchecked")
    public void appendList(ListBuffer buf, com.sun.tools.javac.util.List list) {
        for (int i = 0; i < list.size(); i++) {
            buf.append(list.get(i));
        }
    }

    /**
     * the position is used reversely as the sequence they appears.
     */
    public JCExpression makeTypeArray(JCExpression type,JCTree.JCDomInstance dom, int dim, ArrayList pos, ArrayList endPos,boolean option) {
        for (int i = dim - 1; i >= 0; i--) {
            type = T.at(((Integer) pos.get(i)).intValue()).TypeArray(type,dom,option);
            storeEnd(type, ((Integer) endPos.get(endPos.size() - 1)).intValue() + 1);
        }
        return type;
    }

    public com.sun.tools.javac.util.List<JCStatement> toStatementList(com.sun.tools.javac.util.List<JCExpression> list,
            ArrayList endPos) {
        ListBuffer<JCStatement> ret = new ListBuffer();
        int i = 0;
        for (JCExpression expr : list) {
            JCStatement stmt = T.at(expr.pos).Exec(checkExprStat(expr));
            ret.append(stmt);
            storeEnd(stmt, ((Integer) endPos.get(i++)).intValue() + 1);
        }
        return ret.toList();
    }

    public com.sun.tools.javac.util.List<JCExpressionStatement> toExprStatementList(
            com.sun.tools.javac.util.List<JCExpression> list, ArrayList endPos) {
        ListBuffer<JCExpressionStatement> ret = new ListBuffer();
        int i = 0;
        for (JCExpression expr : list) {
            JCExpressionStatement stmt = T.at(expr.pos).Exec(checkExprStat(expr));
            ret.append(stmt);
            storeEnd(stmt, ((Integer) endPos.get(i++)).intValue() + 1);
        }
        return ret.toList();
    }

    public boolean checkFloatError(Float f, String s, int pos) {
        if (f.floatValue() == 0.0f && !isZero(s)) {
            log.error(pos, "fp.number.too.small");
            return false;
        } else if (f.floatValue() == Float.POSITIVE_INFINITY) {
            log.error(pos, "fp.number.too.large");
            return false;
        }
        return true;
    }

    public boolean checkDoubleError(Double n, String s, int pos) {
        if (n.doubleValue() == 0.0d && !isZero(s)) {
            log.error(pos, "fp.number.too.small");
            return false;
        } else if (n.doubleValue() == Double.POSITIVE_INFINITY) {
            log.error(pos, "fp.number.too.large");
            return false;
        }
        return true;
    }

    boolean isZero(String s) {
        char[] cs = s.toCharArray();
        int base = ((Character.toLowerCase(s.charAt(1)) == 'x') ? 16 : 10);
        int i = ((base == 16) ? 2 : 0);
        while (i < cs.length && (cs[i] == '0' || cs[i] == '.'))
            i++;
        return !(i < cs.length && (Character.digit(cs[i], base) > 0));
    }

    public JCExpression parseInt(Token t) {
        AntlrJavacToken token = (AntlrJavacToken) t;
        JCExpression ret = null;
        try {
            if (token.getType() == JavaParser.INTLITERAL) {
                ret = T.at(token.getStartIndex()).Literal(TypeTags.INT,
                        Convert.string2int(token.stringVal, token.radix));
            } else {
                ret = T.at(token.getStartIndex()).Literal(TypeTags.LONG,
                        new Long(Convert.string2long(token.stringVal, token.radix)));
            }
        } catch (NumberFormatException ex) {
            ret = T.Erroneous();
            log.error(token.getStartIndex(), "int.number.too.large", token.stringVal);
        }
        storeEnd(ret, token);
        return ret;
    }

    public JCExpression parseFloat(Token t) {
        AntlrJavacToken token = (AntlrJavacToken) t;
        JCExpression ret = null;
        String ts = (token.radix == 16 ? "0x" + token.stringVal : token.stringVal);
        try {
            if (token.getType() == JavaParser.FLOATLITERAL) {
                Float n = null;
                try {
                    n = Float.valueOf(ts);
                } catch (NumberFormatException ex) {
                    n = Float.NaN;
                }

                if (checkFloatError(n, ts, token.getStartIndex()) == false) {
                    ret = T.Erroneous();
                } else {
                    ret = T.at(token.getStartIndex()).Literal(TypeTags.FLOAT, n);
                    storeEnd(ret, token);
                }
            } else {
                Double n = null;
                try {
                    n = Double.valueOf(ts);
                } catch (NumberFormatException ex) {
                    n = Double.NaN;
                }
                if (checkDoubleError(n, ts, token.getStartIndex()) == false) {
                    ret = T.Erroneous();
                } else {
                    ret = T.at(token.getStartIndex()).Literal(TypeTags.DOUBLE, n);
                    storeEnd(ret, token);
                }
            }
        } catch (NumberFormatException ex) {
            ret = T.Erroneous();
            log.error(token.getStartIndex(), "int.number.too.large", token.stringVal);
        }
        return ret;
    }

    public JCExpression parseMinusInt(Token t, int minusPosition) {
        JCExpression ret = null;
        AntlrJavacToken token = (AntlrJavacToken) t;
        boolean isInt = true;
        int tag = TypeTags.INT;
        Object val = null;
        if (token.getType() != JavaParser.INTLITERAL) {
            isInt = false;
            tag = TypeTags.LONG;
        }
        String prefix = (token.radix == 10) ? "-" : "";
        try {
            if (isInt == true)
                val = Convert.string2int(prefix + token.stringVal, token.radix);
            else
                val = new Long(Convert.string2long(prefix + token.stringVal, token.radix));
        } catch (NumberFormatException ex) {
            log.error(token.getStartIndex(), "int.number.too.large", prefix + token.stringVal);
            return T.Erroneous();
        }

        ret = T.at(token.getStartIndex()).Literal(tag, val);
        storeEnd(ret, token);
        if (token.radix != 10) {
            ret = T.at(minusPosition).Unary(JCTree.NEG, ret);
        }
        return ret;
    }

    /**
     * Verify the positions of these token are adjacent.
     */
    boolean verifyPosition(Token... tokens) {
        int pre = ((AntlrJavacToken) tokens[0]).getStartIndex();
        for (int i = 1; i < tokens.length; i++) {
            pre++;
            if (((AntlrJavacToken) tokens[i]).getStartIndex() != pre) {
                return false;
            }
        }
        return true;
    }

    JCExpression checkExprStat(JCExpression t) {
        if(t==null)
            return null;

        switch (t.getTag()) {
        case JCTree.USR:
        case JCTree.SEQ:
        case JCTree.DOMITER:
        case JCTree.SEQUENCE:
        case JCTree.SELECTEXP:
        case JCTree.CASEEXP:
        case JCTree.PREINC:
        case JCTree.PREDEC:
        case JCTree.POSTINC:
        case JCTree.POSTDEC:
        case JCTree.ASSIGN:
        case JCTree.BITOR_ASG:
        case JCTree.BITXOR_ASG:
        case JCTree.BITAND_ASG:
        case JCTree.SL_ASG:
        case JCTree.SR_ASG:
        case JCTree.USR_ASG:
        case JCTree.PLUS_ASG:
        case JCTree.MINUS_ASG:
        case JCTree.MUL_ASG:
        case JCTree.DIV_ASG:
        case JCTree.MOD_ASG:
        case JCTree.APPLY:
        case JCTree.PRAGMA:
        case JCTree.NEWCLASS:
        case JCTree.ERRONEOUS:
            return t;
        default:
            log.error(t.pos, "not.stmt");
            return T.at(t.pos).Erroneous(com.sun.tools.javac.util.List.<JCTree> of(t));
        }
    }

}
