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
/*
import org.antlr.runtime.BitSet;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.MismatchedTokenException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
 *
 */

import org.antlr.runtime.*;

import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

/**
 * Super class for generated parser.
 * @author yang
 *
 */
public class AntlrJavacParser extends org.antlr.runtime.Parser implements com.sun.tools.javac.parser.Parser {

    protected TreeMaker T;
    protected Names names;
    protected Log log;
    protected AntlrJavacParserUtil pu;
    protected Source source;

    /**
     * To be set by lexer, if there is an lexing error.
     * Won't try parsing if this field is true.
     */
    private boolean lexingError = false;

    /**
     * Blank tree returned in case there is an error
     */
    protected JCCompilationUnit TREE_BLANK = null;

    protected boolean keepDocComment = true;
    protected boolean keepEndPosition = true;
    protected boolean keepLineMap = true;

    /**
     * For different source levels, whether an error regarding a specific feature has been displayed once.
     */
    protected boolean enumErrorDisplayed = false;
    protected boolean assertErrorDisplayed = false;
    protected boolean varArgErrorDisplayed = false;
    protected boolean foreachErrorDisplayed = false;
    protected boolean staticImportErrorDisplayed = false;
    protected boolean annotationErrorDisplayed = false;
    protected boolean genericErrorDisplayed = false;
    protected boolean moduleErrorDisplayed = false;

    public static int currentRulePosition = 0;

    /**
     * This is needed for building linemap. There is no good way to communicate between lexer and parser.
     * TODO: Get rid of this.
     */

    protected char[] rawInput;

    /**
     * Do not call this constructor.
     * @param input
     * @param state
     */
    protected AntlrJavacParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

    public JCCompilationUnit parseCompilationUnit(boolean devVerbose) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public JCExpression parseExpression() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public JCStatement parseStatement() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public JCExpression parseType() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void init(AntlrParserFactory fac, boolean keepDocComment, boolean keepEndPosition, boolean keepLineMap,
            char[] rawInput) {
        T = fac.F;
        this.names = fac.names;
        this.log = fac.log;
        this.source = fac.source;

        this.pu = new AntlrJavacParserUtil(keepDocComment, keepEndPosition);
        pu.setLog(log);
        pu.setTreeMaker(T);

        this.TREE_BLANK = T.at(0).TopLevel(com.sun.tools.javac.util.List.<JCAnnotation> nil(), null,
                com.sun.tools.javac.util.List.<JCTree> nil());

        this.keepDocComment = keepDocComment;
        this.keepEndPosition = keepEndPosition;
        this.keepLineMap = keepLineMap;

        this.rawInput = rawInput;
    }

    /**
     * If the TokenStream of this parser is constructed from an antlr lexer, indicates whether or not there
     * are errors while lexing.
     * Subclass must respect this error and not try to parsing if this returns true.
     * @return
     */
    public boolean isLexingError() {
        return lexingError;
    }

    public void setLexingError(boolean error) {
        this.lexingError = error;
    }

    /**
     * Override antlr default implementation, force displaying nothing.
     */

    public String getErrorMessage(RecognitionException e, String[] tokenNames) {
            String msg = e.getMessage();

            if ( e instanceof UnwantedTokenException ) {
                    UnwantedTokenException ute = (UnwantedTokenException)e;
                    String tokenName="<unknown>";
                    if ( ute.expecting== Token.EOF ) {
                            tokenName = "EOF";
                    }
                    else {
                        tokenName = "'"+TokenLookup.get(ute.expecting)+"'";
                    }
                    msg = "extraneous input "+getTokenErrorDisplay(ute.getUnexpectedToken())+
                            " expecting "+tokenName;
            }
            else if ( e instanceof MissingTokenException ) {
                    MissingTokenException mte = (MissingTokenException)e;
                    String tokenName="<unknown>";
                    if ( mte.expecting== Token.EOF ) {
                            tokenName = "EOF";
                    }
                    else {
                            tokenName = "'"+TokenLookup.get(mte.expecting)+"'";
    //                        tokenName = tokenNames[mte.expecting];
                    }
                    msg = "missing "+tokenName+" at "+getTokenErrorDisplay(e.token);
            }
            else if ( e instanceof MismatchedTokenException ) {
                    MismatchedTokenException mte = (MismatchedTokenException)e;
                    String tokenName="<unknown>";
                    if ( mte.expecting== Token.EOF ) {
                            tokenName = "EOF";
                    }
                    else {
                            tokenName = "'"+TokenLookup.get(mte.expecting)+"'";
//                            tokenName = tokenNames[mte.expecting];
                    }
                    msg = "mismatched input "+getTokenErrorDisplay(e.token)+
                            " expecting "+tokenName;
            }
            else if ( e instanceof MismatchedTreeNodeException ) {
                    MismatchedTreeNodeException mtne = (MismatchedTreeNodeException)e;
                    String tokenName="<unknown>";
                    if ( mtne.expecting==Token.EOF ) {
                            tokenName = "EOF";
                    }
                    else {
                            tokenName = "'"+TokenLookup.get(mtne.expecting)+"'";
//                            tokenName = tokenNames[mtne.expecting];
                    }
                    msg = "mismatched tree node: "+mtne.node+
                            " expecting "+tokenName;
            }
            else if ( e instanceof NoViableAltException ) {
                    NoViableAltException nvae = (NoViableAltException)e;
                    // for development, can add "decision=<<"+nvae.grammarDecisionDescription+">>"
                    // and "(decision="+nvae.decisionNumber+") and
                    // "state "+nvae.stateNumber
                    msg = "syntax error at "+getTokenErrorDisplay(e.token);
            }
            else if ( e instanceof EarlyExitException ) {
                    EarlyExitException eee = (EarlyExitException)e;
                    // for development, can add "(decision="+eee.decisionNumber+")"
                    msg = "required (...)+ loop did not match anything at input "+
                            getTokenErrorDisplay(e.token);
            }
            else if ( e instanceof MismatchedSetException ) {
                    MismatchedSetException mse = (MismatchedSetException)e;
                    msg = "mismatched input "+getTokenErrorDisplay(e.token)+
                            " expecting set "+mse.expecting;
            }
            else if ( e instanceof MismatchedNotSetException ) {
                    MismatchedNotSetException mse = (MismatchedNotSetException)e;
                    msg = "mismatched input "+getTokenErrorDisplay(e.token)+
                            " expecting set "+mse.expecting;
            }
            else if ( e instanceof FailedPredicateException ) {
                    FailedPredicateException fpe = (FailedPredicateException)e;
                    msg = "rule "+fpe.ruleName+" failed predicate: {"+
                            fpe.predicateText+"}?";
            }
            return msg;
    }


    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {

		int pos=((CommonToken) e.token).getStartIndex();
		String info="";
		if(e.token.getTokenIndex()==Token.EOF)//this gives a better error position
		{
			StackTraceElement[] stack=Thread.currentThread().getStackTrace();
			info=" while parsing "+stack[3].getMethodName();

			//pointing to the beginning of the file (((CommonToken) e.token).getStartIndex())
			//isn't very helpful here
			pos=currentRulePosition;
		}

		String msg = getErrorMessage(e, tokenNames);

		log.error(pos, "invalid", msg+info); //TODO, Error reporting

		//give error back to parser, so it can tell us where the problem originated
    }
/*
    public String getErrorMessage(RecognitionException e, String[] tokenNames) {
        log.error(((AntlrJavacToken) e.token).getStartIndex(), "invalid", "'"+((AntlrJavacToken) e.token).getText()+"'"); //TODO, Error reporting
        return super.getErrorMessage(e, tokenNames);
    }
*/
    protected void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException {
        throw new MismatchedTokenException(ttype, input);
    }

}
