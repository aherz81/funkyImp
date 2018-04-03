/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.tum.funky.codegen;

import edu.tum.funky.codegen.util.RecursiveFileDeleter;
import edu.tum.funky.codegen.expression.Expression;
import edu.tum.funky.codegen.expression.LiteralExpression;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alexander Poeppl
 */
public class FunkySampleCodeGenerator {

    public static final String LINE_TERM = "\\\n";

    public static final String TEMPLATE_BEGIN
            = "// @PARAM: -PP \"\" -g:none -CPP -d ./tmp -cp ../../../../../../input/current/\n"
            + "// shows how to run a custom shell script as a test:\n"
            + "// @CUSTOM ./custom.sh\n"
            + "// as can be seen from the cmdl, the CWD is set to the .funky file\n"
            + "// @TEST noerror\n"
            + "\n"
            + "\n"
            + "import ffi.stdio;\n"
            + "import stdlib.vector;\n"
            + "import stdlib.gVector;\n"
            + "\n"
            + "#define TYPE float\n"
            + "#define FUNCNAME(HEIGHT, WIDTH) transpose_ ## HEIGHT ## _ ## WIDTH\n"
            + "\n"
            + "#define BENCHMARK(HEIGHT, WIDTH) static TYPE[two_d{HEIGHT, WIDTH}] FUNCNAME(HEIGHT,WIDTH) (TYPE matrix[two_d{HEIGHT, WIDTH}], float xxx[one_d{128}])\\\n"
            + "         {\\\n"
            + "             cancel new TYPE[two_d{HEIGHT, WIDTH}].\\[x,y]{";

    public static final String TEMPLATE_END = "};\\\n"
            + "         }\n"
            + "\n"
            + "#define MATRIX_NAME(HEIGHT, WIDTH) matrix_ ## HEIGHT ## _ ## WIDTH       \n"
            + "#define BENCHMARK_CALL(HEIGHT, WIDTH) TYPE MATRIX_NAME(HEIGHT, WIDTH)[two_d{HEIGHT, WIDTH}] = new TYPE[two_d{HEIGHT, WIDTH}].\\[x,y]{(x*10)+(9-y)};\\\n"
            + "                                      FUNCNAME(HEIGHT, WIDTH)(MATRIX_NAME(HEIGHT, WIDTH), toloc)\n"
            + "        \n"
            + "domain two_d{x,y}:one_d{x*y}(o) = { (j,k) | j<x & k<y }\n"
            + "\n"
            + "public class cur\n"
            + "{\n"
            + "            BENCHMARK(32,32)\n"
            + "            BENCHMARK(32,64)\n"
            + "            BENCHMARK(64,64)\n"
            + "            BENCHMARK(64,128)\n"
            + "\n"
            + "            BENCHMARK(128,128)\n"
            + "            BENCHMARK(256,128)\n"
            + "            BENCHMARK(256,256)\n"
            + "            BENCHMARK(256,512)\n"
            + "\n"
            + "            BENCHMARK(512,512)\n"
            + "            BENCHMARK(512,1024)\n"
            + "            BENCHMARK(1024,1024)\n"
            + "            BENCHMARK(2048,1024)\n"
            + "\n"
            + "            BENCHMARK(2048,2048)\n"
            + "            BENCHMARK(4096,2048)\n"
            + "            BENCHMARK(4096,4096)\n"
            + "            BENCHMARK(4096,8192)\n"
            + "\n"
            + "            BENCHMARK(8192,8192)\n"
            + "\n"
            + "        \n"
            + "        static int main(int argc, inout unique String[one_d{-1}] argv)\n"
            + "        {\n"
            + "            float[one_d{128}] toloc = new float[one_d{128}].\\[x]{x};\n" 
            + "\n"
            + "            BENCHMARK_CALL(32,32);\n"
            + "            BENCHMARK_CALL(32,64);\n"
            + "            BENCHMARK_CALL(64,64);\n"
            + "            BENCHMARK_CALL(64,128);\n"
            + "\n"
            + "            BENCHMARK_CALL(128,128);\n"
            + "            BENCHMARK_CALL(256,128);\n"
            + "            BENCHMARK_CALL(256,256);\n"
            + "            BENCHMARK_CALL(256,512);\n"
            + "\n"
            + "            BENCHMARK_CALL(512,512);\n"
            + "            BENCHMARK_CALL(512,1024);\n"
            + "            BENCHMARK_CALL(1024,1024);\n"
            + "            BENCHMARK_CALL(2048,1024);\n"
            + "\n"
            + "            BENCHMARK_CALL(2048,2048);\n"
            + "            BENCHMARK_CALL(4096,2048);\n"
            + "            BENCHMARK_CALL(4096,4096);\n"
            + "            BENCHMARK_CALL(4096,8192);\n"
            + "\n"
            + "            BENCHMARK_CALL(8192, 8192);\n"
            + "\n"
            + "            \n"
            + "            finally 0;\n"
            + "        }\n"
            + "}\n"
            + "";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Incorrect usage. \nUsage: java -jar <ProgName> <path/to/config.properties>");
            System.exit(1);
        }

        Optional<ConfigurationBase> configBase = ConfigurationBase.load(args[0]);
        if (!configBase.isPresent()) {
            System.err.println("Configuration file not present at path " + args[0] + ". Exiting.");
            System.exit(1);
        } else {
            CodeGeneratorConfiguration config = new CodeGeneratorConfiguration(configBase.get());
            try {
                generateRandomExamples(config);
            } catch (NoSuchFieldException ex) {
                System.err.println(ex.getMessage());
                System.exit(1);
            }
        }
    }

    public static void generateMemoryAccess() {
        ExpressionTreeCreator creator = new ExpressionTreeCreator(LiteralExpression.LiteralType.INT, null);
        Map<Integer, List<Expression>> expressions = new HashMap<>();
        
        //Condition checks that there are at least 20 of each node count. (1-20)
        while (!(expressions.keySet().size() == 20 
                && expressions.keySet().stream().allMatch(l -> expressions.get(l) != null
                    && expressions.get(l).size() >= 20))) {
            Optional<Expression> exp = creator.createTree();
            exp.filter(e -> e.nodeCount() <= 20).ifPresent(e -> {
                int numberOfNodes = e.nodeCount();
                List<Expression> exes = (expressions.get(numberOfNodes) == null) ? new ArrayList<>() : expressions.get(numberOfNodes);
                exes.add(e);
                expressions.put(numberOfNodes, exes);
            });
        }
        System.out.println("vector<vector<string>> memoryAccesses = {");
        expressions.keySet().stream().
                map(e -> expressions.get(e)).
                forEach(l -> {
                    System.out.print("\t{");
                    l.stream().limit(20).map(e -> "\"" + e.toString() + "\", ").forEach(System.out::print);
                    System.out.println("},");
                });
        System.out.println("};");

    }

    public static void generateRandomExamples(CodeGeneratorConfiguration config) throws IOException, NoSuchFieldException {
        Path p = Paths.get(config.getOutputPath());
        //Files.walkFileTree(p, new RecursiveFileDeleter());
        //Path newFolder = Files.createDirectory(p);
        final ExpressionTreeCreator codeGen = new ExpressionTreeCreator(LiteralExpression.LiteralType.FLOAT, config);
        final int startOffset = config.getStartIndex();
        for (int i = 0; i < config.getNumberOfExamples(); i++) {
            Optional<Expression> e;
            do {
                e = codeGen.createTree().filter(exp -> {
                    int nodeCount = exp.nodeCount();
                    boolean inUpperBound = nodeCount <= config.getMaximumNumberOfNodes();
                    boolean inLowerBound = nodeCount >= config.getMinimumNumberOfNodes();
                    return inLowerBound && inUpperBound;
                });
            } while (!e.isPresent());
            Path newFile = p.resolve("file_" + fill(config.getNumberOfExamples() + startOffset, i + startOffset) + ".f");
            Files.deleteIfExists(newFile);
            Files.createFile(newFile);
            String wholeCode = TEMPLATE_BEGIN + e.get() + TEMPLATE_END;
            Files.write(newFile, wholeCode.getBytes(Charset.defaultCharset()));
            System.out.println("" + i + ":\t" + e.get().toString());
        }
    }
    
    private static String fill(int max, int cur) {
        String maxS = Integer.toString(max);
        String curS = Integer.toString(cur);
        while (curS.length() < maxS.length()) {
            curS = "0" + curS;
        }
        return curS;
    }

}