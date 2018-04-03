package com.sun.tools.javac.jvm;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.jvm.ClassWriter;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import java.io.IOException;
import java.io.OutputStream;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.ByteBuffer;
import com.sun.tools.javac.util.Name;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;


public class LlvmClassWriter implements CodeWriter {
    protected static final Context.Key<LlvmClassWriter> classWriterKey =
        new Context.Key<LlvmClassWriter>();
    
    private final JavaFileManager fileManager;
    private final Names names;
    private Types types;
    ByteBuffer sigbuf = new ByteBuffer();
    private final ArrayList<String> buf;
    
    public static LlvmClassWriter instance(Context context) {
        LlvmClassWriter instance = context.get(classWriterKey);
        if (instance == null)
            instance = new LlvmClassWriter(context);
            //instance = new LlvmClassWriter();
        return instance;
    }

    private LlvmClassWriter(Context context) {
        fileManager = context.get(JavaFileManager.class);
        names = Names.instance(context);
        types = Types.instance(context);
        buf = new ArrayList<String>();
    }

    public JavaFileObject writeClass(ClassSymbol c) throws IOException, PoolOverflow, StringOverflow {
        JavaFileObject outFile
            = fileManager.getJavaFileForOutput(StandardLocation.CLASS_OUTPUT,
                                               c.flatname.toString(),
                                               JavaFileObject.Kind.CLASS,
                                               c.sourcefile);
        OutputStream out = outFile.openOutputStream();
        try {
            writeClassFile(out, c);
            out.close();
            out = null;
        } finally {
            if (out != null) {
                // if we are propogating an exception, delete the file
                out.close();
                outFile.delete();
                outFile = null;
            }
        }
        return outFile; // may be null if write failed
    }

    public Object xClassName(Type t) {
        if (t.tag == TypeTags.CLASS) {
            return names.fromUtf(ClassFile.externalize(t.tsym.flatName()));
        } else if (t.tag == TypeTags.ARRAY) {
            return typeSig(types.erasure(t));
        } else {
            throw new AssertionError("xClassName");
        }
    }
    
    Name typeSig(Type type) {
        assert sigbuf.length == 0;
        Name n = sigbuf.toName(names);
        sigbuf.reset();
        return n;
    }

    private void writeClassFile(OutputStream out, ClassSymbol c) {
        writeMethods(c.members().elems);
        PrintWriter w = new PrintWriter(out);
        try {
            for(String s : buf) w.println(s);
        } finally {
            w.flush();
            w.close();
        }
    }
    
    void writeMethods(Scope.Entry e) {
        List<MethodSymbol> methods = List.nil();
        for (Scope.Entry i = e; i != null; i = i.sibling) {
            if (i.sym.kind == Kinds.MTH && (i.sym.flags() & Flags.HYPOTHETICAL) == 0) {
                methods = methods.prepend((MethodSymbol) i.sym);
            }
        }
        while (methods.nonEmpty()) {
            writeMethod(methods.head);
            methods = methods.tail;
        }
    }

    private void writeMethod(MethodSymbol m) {
        if (m.llvmCode != null) {
            writeCode(m.llvmCode);
        }
    }

    private void writeCode(LlvmCode code) {
        buf.addAll(code.getCodeBuf());
    }
    
}
