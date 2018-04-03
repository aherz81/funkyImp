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

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CharStreamState;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

/**
 * Using this stream to feed the lexer.
 * It converts Unicode escaped sequences (\ u ****) to regular chars.
 * @author Yang Jiang
 *
 */
public class AntlrJavacStringStream extends ANTLRStringStream {
    int ch;
    boolean pslash = false;

    protected Context context;
    protected Log log;
    private boolean hasError = false;

    public void setContext(Context context) {
        this.context = context;
        this.log = Log.instance(context);
    }

    public boolean hasError() {
        return hasError;
    }

    /**
     * Get a copy of the string of input as char array.
     * @return
     */
    public char[] getCharArray() {
        return super.data;
    }

    public AntlrJavacStringStream(String input) {
        super(input);
    }

    public void consume() {
        if (p >= n)
            return;
        int oldP = p;
        scanOneChar();
        charPositionInLine = charPositionInLine + (p - oldP);
        if (ch == '\n') {
            line++;
            charPositionInLine = 0;
        }
    }

    /**
     * TODO Antlr not supporting minus
     */
    public int LA(int i) {
        if (hasError == true) {
            return CharStream.EOF; //TODO Antlr - not lexing anymore on sight of any error?
        }
        if (i == 0) {
            return 0;
        }
        if ((p + i - 1) >= n) {
            return CharStream.EOF;
        }
        boolean oldHasError = hasError;
        boolean oldPslash = pslash;
        int oldp = p;
        for (int j = 0; j < i && ch != CharStream.EOF; j++) {
            scanOneChar();
        }
        hasError = oldHasError;
        pslash = oldPslash;
        p = oldp;
        return ch;
    }

    private void scanOneChar() {
        if (p >= n) {
            ch = CharStream.EOF;
        }
        ch = data[p++];
        if (ch == '\\') {
            if (pslash == true) {
                pslash = false;
                return;
            }
            if (p < n && data[p] == 'u') {
                while (p < n && data[p] == 'u') {
                    p++;
                }
                int limit = p + 4;
                if (limit <= n - 1) {
                    int d = digit(16, data[p++]);
                    int code = d;
                    while (p < limit && d >= 0) {
                        d = digit(16, data[p++]);
                        code = (code << 4) + d;
                    }
                    if (d >= 0) {
                        ch = (char) code;
                        return;
                    }
                }
                hasError = true;
                ch = CharStream.EOF;
                log.error(p, "illegal.unicode.esc");
            } else {
                pslash = true;
            }
            return;
        }
        pslash = false;
        if (ch == '\u001A') {
            ch = CharStream.EOF;
            return;
        }
    }

    private int digit(int base, char c) {
        int result = Character.digit(c, base);
        if (result >= 0 && c > 0x7f) {
            log.error(p, "illegal.nonascii.digit");
            ch = "0123456789abcdef".charAt(result);
        }
        return result;
    }

    class AntlrJavacCharStreamState extends CharStreamState {
        boolean hasError = false;
        boolean pslash = false;
        /** Index into the char stream of next lookahead char */
        int p;

        /** What line number is the scanner at before processing buffer[p]? */
        int line;

        /** What char position 0..n-1 in line is scanner before processing buffer[p]? */
        int charPositionInLine;
    }

    /**
     * NOTE: TODO: Antlr: this overridden method is copied from the Antlr implementation and should be
     * changed if new version comes out.
     */
    public int mark() {
        if (markers == null) {
            markers = new ArrayList();
            markers.add(null); // depth 0 means no backtracking, leave blank
        }
        markDepth++;
        AntlrJavacCharStreamState state = null;
        if (markDepth >= markers.size()) {
            state = new AntlrJavacCharStreamState();
            markers.add(state);
        } else {
            state = (AntlrJavacCharStreamState) markers.get(markDepth);
        }
        state.p = p;
        state.line = line;
        state.charPositionInLine = charPositionInLine;
        state.hasError = hasError;
        state.pslash = pslash;
        lastMarker = markDepth;
        return markDepth;
    }

    /**
     * NOTE: TODO: Antlr: this overridden method is copied from the Antlr implementation and should be
     * changed if new version comes out.
     */
    public void rewind(int m) {
        AntlrJavacCharStreamState state = (AntlrJavacCharStreamState) markers.get(m);
        // restore stream state
        seek(state.p);
        line = state.line;
        charPositionInLine = state.charPositionInLine;
        hasError = state.hasError;
        pslash = state.pslash;
        release(m);
    }
}
