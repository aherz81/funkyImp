package com.sun.tools.javac.jvm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class LlvmCode {
    
    private ArrayList<String> codeBuf = new ArrayList<String>();
    
    private int currentStackTempVarCount = 0;
    
    private boolean returnPending = true;
    
    public static String getTypeFor(int i) {
        return "i32";
    }
    
    public static String getTypeFor(float f) {
        return "float";
    }
    
    public static String getTypeFor(double d) {
        return "double";
    }
    
    public static String getTypeFor(boolean b) {
        return "i1";
    }
    
    public static String getTypeFor(char c) {
        return "i16";
    }
    
    public static String getTypeFor(byte y) {
        return "i8";
    }
    
    public void genAddAssignment(final String result, final String type, 
            boolean isLocal, final String op1, final String op2) {
        codeBuf.add(buildAssignment(result, isLocal) + "add " + type + " " + op1 + ", " + op2);
    }
    
    public void genConstantAssignment(final String result, final String type, 
            boolean isLocal, final String constant) {
        //LLVM optimizes an alloca, store constant, load constant to add 0 anyway
        genAddAssignment(result, type, isLocal, constant, "0");
    }
    
    public void genVoidMain() {
        codeBuf.add("define void @main() {");
    }
    
    public void genIntMain() {
        codeBuf.add("define i32 @main() {");
    }
    
    public void genMethod(final String methodName, final String retType) {
        codeBuf.add("define " + retType + " @" + methodName + "() {");
    }
    
    public void genEndBlock() {
        if (returnPending) {
            codeBuf.add("ret void");
        }
        codeBuf.add("}");
    }
    
    public void genReturnIdent(final String retType, final String retval) {
        returnPending = false;
        codeBuf.add("ret " + retType + " %" + retval);
    }

    public List<String> getCodeBuf() {
        return Collections.unmodifiableList(codeBuf);
    }
    
    private String buildAssignment(final String variable, boolean isLocal) {
        if (isLocal) return buildVariableName(variable, isLocal) + " = ";
        else return buildVariableName(variable, isLocal) + " = global ";
    }
    
    private String buildVariableName(final String variable, boolean isLocal) {
        if (isLocal) return "%" + variable;
        else return "@" + variable;
    }
    
    
}
