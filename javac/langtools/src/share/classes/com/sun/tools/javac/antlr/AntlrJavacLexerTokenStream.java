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

import static com.sun.tools.javac.util.LayoutCharacters.CR;
import static com.sun.tools.javac.util.LayoutCharacters.FF;
import static com.sun.tools.javac.util.LayoutCharacters.LF;
import static com.sun.tools.javac.util.LayoutCharacters.TabInc;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;

/**
 * This token stream takes into account processing deprecated flag in the
 * document comment.
 *
 * @author Yang Jiang
 *
 */
public class AntlrJavacLexerTokenStream extends CommonTokenStream {
    CommentScanner commentScanner = null;

    public AntlrJavacLexerTokenStream(TokenSource tokenSource) {
        super(tokenSource);
        commentScanner = new CommentScanner();
    }

    public Token LT(int k) {
        Token ret = super.LT(k);
        if (ret != null && k == 1 && ret instanceof AntlrJavacToken) {
            // get the comment content from previous tokens, from another channel.
            AntlrJavacToken token = (AntlrJavacToken) ret;
            if (token.docComment == null) { // may have been processed already
                int i = token.getTokenIndex();
                if (i >= 1) {
                    Token t = null;
                    while (i >= 1) {
                        i--;
                        // get the first comment starts with "/**"
                        t = (Token) tokens.get(i);
                        if (t.getChannel() != channel) {
                            if (t.getText() != null && t.getText().trim().startsWith("/**")) {
                                break;
                            }
                        } else {
                            t = null;
                            break;
                        }
                    }
                    if (t != null) {
                        // scan for the content of the comment.
                        commentScanner.processComment(t.getText());
                        if (commentScanner.docComment != null) {
                            if (t.getText().trim().startsWith("/**")) {
                                token.docComment = commentScanner.docComment;
                                int ti = commentScanner.docComment.indexOf("@deprecated");
                                if (ti != -1 && commentScanner.docComment.length() >= ti + 11 + 1
                                        && Character.isWhitespace(commentScanner.docComment.charAt(ti + 11)) == true) {
                                    char ch = 0;
                                    while (--ti > 0) {
                                        ch = commentScanner.docComment.charAt(ti);
                                        if (ch == ' ' || ch == '\t')
                                            continue;
                                        else
                                            break;
                                    }
                                    if (ch == '\n' || ch == '\r') {
                                        token.deprecated = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * TODO: Antlr: another grammar to parse this.
     * Parsing comment.
     * Most copied from javac implementation.
     * @param s
     * @return
     */
    static class CommentScanner {
        private char[] buf;
        private int bp;
        private int buflen;
        private int col;
        private int docCommentCount;
        public String docComment = null;
        private char[] docCommentBuffer = new char[1024];
        private int unicodeConversionBp = 0;
        private char ch;

        private void expandCommentBuffer() {
            char[] newBuffer = new char[docCommentBuffer.length * 2];
            System.arraycopy(docCommentBuffer, 0, newBuffer, 0, docCommentBuffer.length);
            docCommentBuffer = newBuffer;
        }

        private void scanDocCommentChar() {
            scanChar();
            if (ch == '\\') {
                if (buf[bp + 1] == '\\' && unicodeConversionBp != bp) {
                    if (docCommentCount == docCommentBuffer.length)
                        expandCommentBuffer();
                    docCommentBuffer[docCommentCount++] = ch;
                    bp++;
                    col++;
                }
            }
        }

        private void scanChar() {
            bp++;
            ch = buf[bp];
            switch (ch) {
            case '\r': // return
                col = 0;
                break;
            case '\n': // newline
                if (bp == 0 || buf[bp - 1] != '\r') {
                    col = 0;
                }
                break;
            case '\t': // tab
                col = (col / TabInc * TabInc) + TabInc;
                break;
            case '\\': // possible Unicode
                col++;
                convertUnicode();
                break;
            default:
                col++;
                break;
            }
        }

        private void convertUnicode() {
            if (ch == '\\' && unicodeConversionBp != bp) {
                bp++;
                ch = buf[bp];
                col++;
                if (ch == 'u') {
                    do {
                        bp++;
                        ch = buf[bp];
                        col++;
                    } while (ch == 'u');
                    int limit = bp + 3;
                    if (limit < buflen) {
                        int d = digit(16);
                        int code = d;
                        while (bp < limit && d >= 0) {
                            bp++;
                            ch = buf[bp];
                            col++;
                            d = digit(16);
                            code = (code << 4) + d;
                        }
                        if (d >= 0) {
                            ch = (char) code;
                            unicodeConversionBp = bp;
                            return;
                        }
                    }
                    // "illegal.Unicode.esc", reported by base scanner
                } else {
                    bp--;
                    ch = '\\';
                    col--;
                }
            }
        }

        private int digit(int base) {
            char c = ch;
            int result = Character.digit(c, base);
            if (result >= 0 && c > 0x7f) {
                ch = "0123456789abcdef".charAt(result);
            }
            return result;
        }

        @SuppressWarnings("fallthrough")
        public void processComment(String s) {
            buf = s.toCharArray();
            buflen = buf.length;
            bp = 0;
            col = 0;

            docCommentCount = 0;
            docComment = null;

            boolean firstLine = true;

            // Skip over first slash
            scanDocCommentChar();
            // Skip over first star
            scanDocCommentChar();

            // consume any number of stars
            while (bp < buflen && ch == '*') {
                scanDocCommentChar();
            }
            // is the comment in the form /**/, /***/, /****/, etc. ?
            if (bp < buflen && ch == '/') {
                docComment = "";
                return;
            }

            // skip a newline on the first line of the comment.
            if (bp < buflen) {
                if (ch == LF) {
                    scanDocCommentChar();
                    firstLine = false;
                } else if (ch == CR) {
                    scanDocCommentChar();
                    if (ch == LF) {
                        scanDocCommentChar();
                        firstLine = false;
                    }
                }
            }

            outerLoop:

            // The outerLoop processes the doc comment, looping once
            // for each line. For each line, it first strips off
            // whitespace, then it consumes any stars, then it
            // puts the rest of the line into our buffer.
            while (bp < buflen) {

                // The wsLoop consumes whitespace from the beginning
                // of each line.
                wsLoop:

                while (bp < buflen) {
                    switch (ch) {
                    case ' ':
                        scanDocCommentChar();
                        break;
                    case '\t':
                        col = ((col - 1) / TabInc * TabInc) + TabInc;
                        scanDocCommentChar();
                        break;
                    case FF:
                        col = 0;
                        scanDocCommentChar();
                        break;
                    // Treat newline at beginning of line (blank line, no star)
                    // as comment text. Old Javadoc compatibility requires this.
                    /*
                     * --------------------------------- case CR: // (Spec 3.4)
                     * scanDocCommentChar(); if (ch == LF) { col = 0;
                     * scanDocCommentChar(); } break; case LF: // (Spec 3.4)
                     * scanDocCommentChar(); break;
                     * ---------------------------------
                     */
                    default:
                        // we've seen something that isn't whitespace;
                        // jump out.
                        break wsLoop;
                    }
                }

                // Are there stars here? If so, consume them all
                // and check for the end of comment.
                if (ch == '*') {
                    // skip all of the stars
                    do {
                        scanDocCommentChar();
                    } while (ch == '*');

                    // check for the closing slash.
                    if (ch == '/') {
                        // We're done with the doc comment
                        // scanChar() and breakout.
                        break outerLoop;
                    }
                } else if (!firstLine) {
                    // The current line does not begin with a '*' so we will
                    // indent it.
                    for (int i = 1; i < col; i++) {
                        if (docCommentCount == docCommentBuffer.length)
                            expandCommentBuffer();
                        docCommentBuffer[docCommentCount++] = ' ';
                    }
                }

                // The textLoop processes the rest of the characters
                // on the line, adding them to our buffer.
                textLoop: while (bp < buflen) {
                    switch (ch) {
                    case '*':
                        // Is this just a star? Or is this the
                        // end of a comment?
                        scanDocCommentChar();
                        if (ch == '/') {
                            // This is the end of the comment,
                            // set ch and return our buffer.
                            break outerLoop;
                        }
                        // This is just an ordinary star. Add it to
                        // the buffer.
                        if (docCommentCount == docCommentBuffer.length)
                            expandCommentBuffer();
                        docCommentBuffer[docCommentCount++] = '*';
                        break;
                    case ' ':
                    case '\t':
                        if (docCommentCount == docCommentBuffer.length)
                            expandCommentBuffer();
                        docCommentBuffer[docCommentCount++] = ch;
                        scanDocCommentChar();
                        break;
                    case FF:
                        scanDocCommentChar();
                        break textLoop; // treat as end of line
                    case CR: // (Spec 3.4)
                        scanDocCommentChar();
                        if (ch != LF) {
                            // Canonicalize CR-only line terminator to LF
                            if (docCommentCount == docCommentBuffer.length)
                                expandCommentBuffer();
                            docCommentBuffer[docCommentCount++] = (char) LF;
                            break textLoop;
                        }
                        /* fall through to LF case */
                    case LF: // (Spec 3.4)
                        // We've seen a newline. Add it to our
                        // buffer and break out of this loop,
                        // starting fresh on a new line.
                        if (docCommentCount == docCommentBuffer.length)
                            expandCommentBuffer();
                        docCommentBuffer[docCommentCount++] = ch;
                        scanDocCommentChar();
                        break textLoop;
                    default:
                        // Add the character to our buffer.
                        if (docCommentCount == docCommentBuffer.length)
                            expandCommentBuffer();
                        docCommentBuffer[docCommentCount++] = ch;
                        scanDocCommentChar();
                    }
                } // end textLoop
                firstLine = false;
            } // end outerLoop

            if (docCommentCount > 0) {
                int i = docCommentCount - 1;
                trailLoop: while (i > -1) {
                    switch (docCommentBuffer[i]) {
                    case '*':
                        i--;
                        break;
                    default:
                        break trailLoop;
                    }
                }
                docCommentCount = i + 1;

                // Store the text of the doc comment
                docComment = new String(docCommentBuffer, 0, docCommentCount);
            } else {
                docComment = "";
            }
        }
    }

}
