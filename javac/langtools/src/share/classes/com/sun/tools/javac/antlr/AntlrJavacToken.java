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

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;

import com.sun.tools.javac.util.Name;


public class AntlrJavacToken extends CommonToken {

    private static final long serialVersionUID = 1L;

    /** the Name object used by javac for tree construction */
    public Name name = null;

    /** the string value of this token */
    public String stringVal = null;

    /** for numbers, the radix */
    public int radix = 0;

    /** if a deprecated mark has been set */
    public boolean deprecated = false;

    /** the document comment associated with this note */
    public String docComment = null;

    public AntlrJavacToken(CharStream input, int type, int channel, int start, int stop) {
        super(input, type, channel, start, stop);
    }

    public String getText() {
        String text = super.getText();
        StringBuffer buf = new StringBuffer(text.length());
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\\' && i < len - 1 && text.charAt(i + 1) == 'u') {
                while (text.charAt(++i) == 'u') {
                }
                int code = Character.digit(text.charAt(i++), 16);
                code = (code << 4) + Character.digit(text.charAt(i++), 16);
                code = (code << 4) + Character.digit(text.charAt(i++), 16);
                code = (code << 4) + Character.digit(text.charAt(i), 16);
                buf.append((char) code);
                continue;
            }
            buf.append(c);
        }
        return buf.toString();
    }
}
