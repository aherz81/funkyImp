package edu.tum.funky.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author Alexander Pöppl
 */
public class ResultStatisticsGenerator {

    public static class Pair<L, R> {

        public final L fst;
        public final R snd;

        private Pair(L fst, R snd) {
            this.fst = fst;
            this.snd = snd;
        }

        public static <A, B> Pair<A, B> of(A left, B right) {
            return new Pair<>(left, right);
        }

        @Override
        public String toString() {
            return "(" + fst + ", " + snd + ")";
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Invalid usage. Usage: \n java -jar <NameOfJAR.jar> <PathToFolderContainingResults>");
            System.exit(1);
        }
        Path folderPath = Paths.get(args[0]);
        List<List<List<Double>>> x = readFiles(folderPath);
        Map<Integer, List<Double>> res = analyze(x);

        print(res);
        System.out.println("--------------");
        printAverage(res);
    }

    private static List<List<List<Double>>> readFiles(Path folderPath) throws IOException {
        if (!Files.isDirectory(folderPath)) {
            System.err.println("Invalid usage. Path needs to be a directory.");
            System.exit(1);
        }
        return Files.list(folderPath)
                .filter(file -> file.toString().endsWith(".csv"))
                .map(ResultStatisticsGenerator::readContents)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<List<Double>> readContents(Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            Function<String, List<Double>> lineProcessor = line -> Arrays
                    .asList(line.split("\t"))
                    .stream().filter(s -> s.length() > 0)
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());
            return lines.stream()
                    .map(lineProcessor)
                    .peek(l -> {
                        assert l.size() == 5 : "Malformed input in file: " + file.toString();
                    })
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            throw new RuntimeException("Encountered Exception in file " + file.toString(), e);
        }
    }

    public static Map<Integer, List<Double>> analyze(List<List<List<Double>>> data) {
        Map<Integer, List<Pair<Double, Double>>> result = new HashMap<>();
        for (int i = 1024; i <= 1024 * 1024 * 64; i *= 2) {
            result.put(i, new ArrayList<>());
        }
        data.stream().forEachOrdered(run -> {
            run.stream().
                    forEach(line -> {
                        int size = (int) Math.round(line.get(0));
                        Pair<Double, Double> pair = Pair.of(line.get(2), line.get(3));
                        result.get(size).add(pair);
                    });
        });
        Map<Integer, List<Double>> res = new HashMap<>();
        Function<Pair<Double, Double>, Double> calculateDistance = p -> Math.abs(Math.max(p.fst, p.snd) / Math.min(p.fst, p.snd));
        Function<List<Pair<Double, Double>>, List<Double>> calculateDistances = l -> l.stream().map(calculateDistance).collect(Collectors.toList());
        result.keySet().stream().forEach(key -> res.put(key, calculateDistances.apply(result.get(key))));
        return res;
    }

    private static void printAverage(Map<Integer, List<Double>> res) {
        res.keySet().stream().sorted().map(key
                -> key + "\t" + res.get(key).stream().collect(Collectors.averagingDouble(v -> v)
                )).forEach(System.out::println);
    }

    public static void print(Map<Integer, List<Double>> table) {
        table.keySet().stream().sorted().map(key
                -> key + "\t" + table.get(key).stream().map(v -> v + "\t").reduce("", (i, a) -> i + a)
        ).forEach(System.out::println);
    }
}
