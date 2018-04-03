/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.tum.funky.qc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author nax
 */
public class TestResultQualityCheck {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Invalid number of arguments.\nUsage: java -jar <tool> <config> <resultTable>");
        } else {
            try {
                QualityCheckConfiguration config = new QualityCheckConfiguration(ConfigurationBase.load(args[0]).get());
                BinaryOperator<Double> ratio = (a, b) -> Math.abs(a / b);
                BinaryOperator<Double> distance = (a, b) -> Math.abs(Math.max(a, b) / Math.min(a, b));
                List<List<Double>> result = readFile(Paths.get(args[1]));
                double avg = crunch(result, (config.getAveragingType() == QualityCheckConfiguration.AveragingType.DISTANCE) ? distance : ratio);
                System.out.println(avg);
            } catch (Exception e) {
                System.err.println("An error occured while processing file " + args[0] + ".\n");
                System.err.println(e.toString());
                throw e;
            }
        }
    }

    private static double crunch(List<List<Double>> data, BinaryOperator<Double> operation) {
        return data.stream()
                .filter(line -> line.get(0) > 250000) //Only relevant above that size.
                .filter(line -> line.size() > 3) //Skip failed tests.
                .collect(Collectors.averagingDouble(line -> {
                    double expected = line.get(2);
                    double actual = line.get(3);
                    return operation.apply(expected, actual);
                }));
    }

    public static List<List<Double>> readFile(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        Function<List<String>, List<Double>> lineProcessor = l -> l.stream().filter(s -> s.length() > 0).map(Double::parseDouble).collect(Collectors.toCollection(ArrayList::new));
        return lines.stream().map(line -> line.split("\t")).map(Arrays::asList).map(lineProcessor).collect(Collectors.toList());
    }
}
