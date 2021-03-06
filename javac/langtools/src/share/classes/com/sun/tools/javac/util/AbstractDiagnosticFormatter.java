/*
 * Copyright 2008-2009 Sun Microsystems, Inc.  All Rights Reserved.
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
package com.sun.tools.javac.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.api.DiagnosticFormatter;
import com.sun.tools.javac.api.DiagnosticFormatter.Configuration.DiagnosticPart;
import com.sun.tools.javac.api.DiagnosticFormatter.Configuration.MultilineLimit;
import com.sun.tools.javac.api.DiagnosticFormatter.PositionKind;
import com.sun.tools.javac.api.Formattable;
import com.sun.tools.javac.code.Printer;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.CapturedType;
import com.sun.tools.javac.file.JavacFileManager;

import static com.sun.tools.javac.util.JCDiagnostic.DiagnosticType.*;

/**
 * This abstract class provides a basic implementation of the functionalities that should be provided
 * by any formatter used by javac. Among the main features provided by AbstractDiagnosticFormatter are:
 *
 * <ul>
 *  <li> Provides a standard implementation of the visitor-like methods defined in the interface DiagnisticFormatter.
 *  Those implementations are specifically targeting JCDiagnostic objects.
 *  <li> Provides basic support for i18n and a method for executing all locale-dependent conversions
 *  <li> Provides the formatting logic for rendering the arguments of a JCDiagnostic object.
 * <ul>
 *
 */
public abstract class AbstractDiagnosticFormatter implements DiagnosticFormatter<JCDiagnostic> {

    /**
     * JavacMessages object used by this formatter for i18n.
     */
    protected JavacMessages messages;

    /**
     * Configuration object used by this formatter
     */
    private SimpleConfiguration config;

    /**
     * Current depth level of the disgnostic being formatted
     * (!= 0 for subdiagnostics)
     */
    protected int depth = 0;

    /**
     * Printer instance to be used for formatting types/symbol
     */
    protected Printer printer;

    /**
     * Initialize an AbstractDiagnosticFormatter by setting its JavacMessages object.
     * @param messages
     */
    protected AbstractDiagnosticFormatter(JavacMessages messages, SimpleConfiguration config) {
        this.messages = messages;
        this.config = config;
        this.printer = new FormatterPrinter();
    }

    public String formatKind(JCDiagnostic d, Locale l) {
        switch (d.getType()) {
            case FRAGMENT: return "";
            case NOTE:     return localize(l, "compiler.note.note");
            case WARNING:  return localize(l, "compiler.warn.warning");
            case ERROR:    return localize(l, "compiler.err.error");
            default:
                throw new AssertionError("Unknown diagnostic type: " + d.getType());
        }
    }

    @Override
    public String format(JCDiagnostic d, Locale locale) {
        printer = new FormatterPrinter();
        return formatDiagnostic(d, locale);
    }

    abstract String formatDiagnostic(JCDiagnostic d, Locale locale);

    public String formatPosition(JCDiagnostic d, PositionKind pk,Locale l) {
        assert (d.getPosition() != Position.NOPOS);
        return String.valueOf(getPosition(d, pk));
    }
    //where
    private long getPosition(JCDiagnostic d, PositionKind pk) {
        switch (pk) {
            case START: return d.getIntStartPosition();
            case END: return d.getIntEndPosition();
            case LINE: return d.getLineNumber();
            case COLUMN: return d.getColumnNumber();
            case OFFSET: return d.getIntPosition();
            default:
                throw new AssertionError("Unknown diagnostic position: " + pk);
        }
    }

    public String formatSource(JCDiagnostic d, boolean fullname, Locale l) {
        assert (d.getSource() != null);
        return fullname ? d.getSourceName() : d.getSource().getName();
    }

    /**
     * Format the arguments of a given diagnostic.
     *
     * @param d diagnostic whose arguments are to be formatted
     * @param l locale object to be used for i18n
     * @return a Collection whose elements are the formatted arguments of the diagnostic
     */
    protected Collection<String> formatArguments(JCDiagnostic d, Locale l) {
        ListBuffer<String> buf = new ListBuffer<String>();
        for (Object o : d.getArgs()) {
           buf.append(formatArgument(d, o, l));
        }
        return buf.toList();
    }

    /**
     * Format a single argument of a given diagnostic.
     *
     * @param d diagnostic whose argument is to be formatted
     * @param arg argument to be formatted
     * @param l locale object to be used for i18n
     * @return string representation of the diagnostic argument
     */
    protected String formatArgument(JCDiagnostic d, Object arg, Locale l) {
        if (arg instanceof JCDiagnostic) {
            String s = null;
            depth++;
            try {
                s = formatMessage((JCDiagnostic)arg, l);
            }
            finally {
                depth--;
            }
            return s;
        }
        else if (arg instanceof Iterable<?>) {
            return formatIterable(d, (Iterable<?>)arg, l);
        }
        else if (arg instanceof Type) {
			return ((Type)arg).toPrettyString();
            //return printer.visit((Type)arg, l);
        }
        else if (arg instanceof Symbol) {
            return printer.visit((Symbol)arg, l);
        }
        else if (arg instanceof JavaFileObject) {
            return JavacFileManager.getJavacBaseFileName((JavaFileObject)arg);
        }
        else if (arg instanceof Formattable) {
            return ((Formattable)arg).toString(l, messages);
        }
        else {
            return String.valueOf(arg);
        }
    }

    /**
     * Format an iterable argument of a given diagnostic.
     *
     * @param d diagnostic whose argument is to be formatted
     * @param it iterable argument to be formatted
     * @param l locale object to be used for i18n
     * @return string representation of the diagnostic iterable argument
     */
    protected String formatIterable(JCDiagnostic d, Iterable<?> it, Locale l) {
        StringBuilder sbuf = new StringBuilder();
        String sep = "";
        for (Object o : it) {
            sbuf.append(sep);
            sbuf.append(formatArgument(d, o, l));
            sep = ",";
        }
        return sbuf.toString();
    }

    /**
     * Format all the subdiagnostics attached to a given diagnostic.
     *
     * @param d diagnostic whose subdiagnostics are to be formatted
     * @param l locale object to be used for i18n
     * @return list of all string representations of the subdiagnostics
     */
    protected List<String> formatSubdiagnostics(JCDiagnostic d, Locale l) {
        List<String> subdiagnostics = List.nil();
        int maxDepth = config.getMultilineLimit(MultilineLimit.DEPTH);
        if (maxDepth == -1 || depth < maxDepth) {
            depth++;
            try {
                int maxCount = config.getMultilineLimit(MultilineLimit.LENGTH);
                int count = 0;
                for (JCDiagnostic d2 : d.getSubdiagnostics()) {
                    if (maxCount == -1 || count < maxCount) {
                        subdiagnostics = subdiagnostics.append(formatSubdiagnostic(d, d2, l));
                        count++;
                    }
                    else
                        break;
                }
            }
            finally {
                depth--;
            }
        }
        return subdiagnostics;
    }

    /**
     * Format a subdiagnostics attached to a given diagnostic.
     *
     * @param parent multiline diagnostic whose subdiagnostics is to be formatted
     * @param sub subdiagnostic to be formatted
     * @param l locale object to be used for i18n
     * @return string representation of the subdiagnostics
     */
    protected String formatSubdiagnostic(JCDiagnostic parent, JCDiagnostic sub, Locale l) {
        return formatMessage(sub, l);
    }

    /** Format the faulty source code line and point to the error.
     *  @param d The diagnostic for which the error line should be printed
     */
    protected String formatSourceLine(JCDiagnostic d, int nSpaces) {
        StringBuilder buf = new StringBuilder();
        DiagnosticSource source = d.getDiagnosticSource();
        int pos = d.getIntPosition();
        if (d.getIntPosition() == Position.NOPOS)
            throw new AssertionError();
        String line = (source == null ? null : source.getLine(pos));
        if (line == null)
            return "";
        buf.append(indent(line, nSpaces));
        int col = source.getColumnNumber(pos, false);
        if (config.isCaretEnabled()) {
            buf.append("\n");
            for (int i = 0; i < col - 1; i++)  {
                buf.append((line.charAt(i) == '\t') ? "\t" : " ");
            }
            buf.append(indent("^", nSpaces));
        }
        return buf.toString();
    }

    /**
     * Converts a String into a locale-dependent representation accordingly to a given locale.
     *
     * @param l locale object to be used for i18n
     * @param key locale-independent key used for looking up in a resource file
     * @param args localization arguments
     * @return a locale-dependent string
     */
    protected String localize(Locale l, String key, Object... args) {
        return messages.getLocalizedString(l, key, args);
    }

    public boolean displaySource(JCDiagnostic d) {
        return config.getVisible().contains(DiagnosticPart.SOURCE) &&
                d.getType() != FRAGMENT &&
                d.getIntPosition() != Position.NOPOS;
    }

    /**
     * Creates a string with a given amount of empty spaces. Useful for
     * indenting the text of a diagnostic message.
     *
     * @param nSpaces the amount of spaces to be added to the result string
     * @return the indentation string
     */
    protected String indentString(int nSpaces) {
        String spaces = "                        ";
        if (nSpaces <= spaces.length())
            return spaces.substring(0, nSpaces);
        else {
            StringBuilder buf = new StringBuilder();
            for (int i = 0 ; i < nSpaces ; i++)
                buf.append(" ");
            return buf.toString();
        }
    }

    /**
     * Indent a string by prepending a given amount of empty spaces to each line
     * of the string.
     *
     * @param s the string to be indented
     * @param nSpaces the amount of spaces that should be prepended to each line
     * of the string
     * @return an indented string
     */
    protected String indent(String s, int nSpaces) {
        String indent = indentString(nSpaces);
        StringBuilder buf = new StringBuilder();
        String nl = "";
        for (String line : s.split("\n")) {
            buf.append(nl);
            buf.append(indent + line);
            nl = "\n";
        }
        return buf.toString();
    }

    public SimpleConfiguration getConfiguration() {
        return config;
    }

    static public class SimpleConfiguration implements Configuration {

        protected Map<MultilineLimit, Integer> multilineLimits;
        protected EnumSet<DiagnosticPart> visibleParts;
        protected boolean caretEnabled;

        public SimpleConfiguration(Set<DiagnosticPart> parts) {
            multilineLimits = new LinkedHashMap<MultilineLimit, Integer>();
            setVisible(parts);
            setMultilineLimit(MultilineLimit.DEPTH, -1);
            setMultilineLimit(MultilineLimit.LENGTH, -1);
            setCaretEnabled(true);
        }

        @SuppressWarnings("fallthrough")
        public SimpleConfiguration(Options options, Set<DiagnosticPart> parts) {
            this(parts);
            String showSource = null;
            if ((showSource = options.get("showSource")) != null) {
                if (showSource.equals("true"))
                    visibleParts.add(DiagnosticPart.SOURCE);
                else if (showSource.equals("false"))
                    visibleParts.remove(DiagnosticPart.SOURCE);
            }
            String diagOpts = options.get("diags");
            if (diagOpts != null) {//override -XDshowSource
                Collection<String> args = Arrays.asList(diagOpts.split(","));
                if (args.contains("short")) {
                    visibleParts.remove(DiagnosticPart.DETAILS);
                    visibleParts.remove(DiagnosticPart.SUBDIAGNOSTICS);
                }
                if (args.contains("source"))
                    visibleParts.add(DiagnosticPart.SOURCE);
                if (args.contains("-source"))
                    visibleParts.remove(DiagnosticPart.SOURCE);
            }
            String multiPolicy = null;
            if ((multiPolicy = options.get("multilinePolicy")) != null) {
                if (multiPolicy.equals("disabled"))
                    visibleParts.remove(DiagnosticPart.SUBDIAGNOSTICS);
                else if (multiPolicy.startsWith("limit:")) {
                    String limitString = multiPolicy.substring("limit:".length());
                    String[] limits = limitString.split(":");
                    try {
                        switch (limits.length) {
                            case 2: {
                                if (!limits[1].equals("*"))
                                    setMultilineLimit(MultilineLimit.DEPTH, Integer.parseInt(limits[1]));
                            }
                            case 1: {
                                if (!limits[0].equals("*"))
                                    setMultilineLimit(MultilineLimit.LENGTH, Integer.parseInt(limits[0]));
                            }
                        }
                    }
                    catch(NumberFormatException ex) {
                        setMultilineLimit(MultilineLimit.DEPTH, -1);
                        setMultilineLimit(MultilineLimit.LENGTH, -1);
                    }
                }
            }
            String showCaret = null;
            if (((showCaret = options.get("showCaret")) != null) &&
                showCaret.equals("false"))
                    setCaretEnabled(false);
            else
                setCaretEnabled(true);
        }

        public int getMultilineLimit(MultilineLimit limit) {
            return multilineLimits.get(limit);
        }

        public EnumSet<DiagnosticPart> getVisible() {
            return EnumSet.copyOf(visibleParts);
        }

        public void setMultilineLimit(MultilineLimit limit, int value) {
            multilineLimits.put(limit, value < -1 ? -1 : value);
        }


        public void setVisible(Set<DiagnosticPart> diagParts) {
            visibleParts = EnumSet.copyOf(diagParts);
        }

        /**
         * Shows a '^' sign under the source line displayed by the formatter
         * (if applicable).
         *
         * @param caretEnabled if true enables caret
         */
        public void setCaretEnabled(boolean caretEnabled) {
            this.caretEnabled = caretEnabled;
        }

        /**
         * Tells whether the caret display is active or not.
         *
         * @param caretEnabled if true the caret is enabled
         */
        public boolean isCaretEnabled() {
            return caretEnabled;
        }
    }

    /**
     * An enhanced printer for formatting types/symbols used by
     * AbstractDiagnosticFormatter. Provides alternate numbering of captured
     * types (they are numbered starting from 1 on each new diagnostic, instead
     * of relying on the underlying hashcode() method which generates unstable
     * output). Also detects cycles in wildcard messages (e.g. if the wildcard
     * type referred by a given captured type C contains C itself) which might
     * lead to infinite loops.
     */
    protected class FormatterPrinter extends Printer {

        List<Type> allCaptured = List.nil();
        List<Type> seenCaptured = List.nil();

        @Override
        protected String localize(Locale locale, String key, Object... args) {
            return AbstractDiagnosticFormatter.this.localize(locale, key, args);
        }

        @Override
        public String visitCapturedType(CapturedType t, Locale locale) {
            if (seenCaptured.contains(t))
                return localize(locale, "compiler.misc.type.captureof.1",
                    allCaptured.indexOf(t) + 1);
            else {
                try {
                    seenCaptured = seenCaptured.prepend(t);
                    allCaptured = allCaptured.append(t);
                    return localize(locale, "compiler.misc.type.captureof",
                        allCaptured.indexOf(t) + 1,
                        visit(t.wildcard, locale));
                }
                finally {
                    seenCaptured = seenCaptured.tail;
                }
            }
        }
    }
}
