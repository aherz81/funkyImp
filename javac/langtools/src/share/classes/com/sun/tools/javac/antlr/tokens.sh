echo "package com.sun.tools.javac.antlr;" > TokenLookup.java
echo "import java.util.HashMap;" >> TokenLookup.java
echo "public class TokenLookup" >> TokenLookup.java
echo "{" >> TokenLookup.java
echo "private static final HashMap<Integer, String> lookup = new HashMap<Integer, String>();" >> TokenLookup.java
echo "static {" >> TokenLookup.java
grep "'\(.*\)'=\(.*\)" $1 | sed -e "s/\\\\/\\\\\\\\/g" | sed -e "s/'\(.*\)'=\(.*\)/lookup.put(\2,\"\1\");/g" >> TokenLookup.java
echo "}" >> TokenLookup.java
echo "public static String get(Integer id){return lookup.get(id);}" >> TokenLookup.java
echo "}" >> TokenLookup.java



