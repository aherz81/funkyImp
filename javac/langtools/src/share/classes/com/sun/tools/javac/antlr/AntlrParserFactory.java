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

import org.antlr.runtime.CommonTokenStream;

import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.util.Context;

public class AntlrParserFactory extends ParserFactory {
    private Context context;

    public static void preRegister(final Context context) {
        context.put(parserFactoryKey, new Context.Factory<ParserFactory>() {
            public ParserFactory make() {
                return new AntlrParserFactory(context);
            }
        });
    }

    protected AntlrParserFactory(Context context) {
        super(context);
        this.context = context;
    }

    public Parser newParser(CharSequence input, boolean keepDocComments, boolean keepEndPos, boolean keepLineMap) {
        if ("true".equalsIgnoreCase(options.get("antlrdebug"))) {
            System.out.println("Parsing with antlr");
        }

        JavaLexer javal = new JavaLexer(new AntlrJavacStringStream(input.toString()), context, keepLineMap);
        CommonTokenStream stream = new AntlrJavacLexerTokenStream(javal);
        char[] rawInput = null;
        if (keepLineMap == true) {
            rawInput = input.toString().toCharArray();
        }
        return new JavaParser(stream, this, keepDocComments, keepEndPos, keepLineMap, rawInput);
    }
}
