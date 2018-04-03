/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package testresultqualitycheck;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 *
 * @author nax
 */
public class TestResultQualityCheck {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Invalid number of arguments.\nUsage:java -jar <tool> testFolder");
        } else {
            List<List<Double>> result = readFile(Paths.get(args[0]));
            double avg =  result.stream()
                    .filter(line -> line.get(0) > 250000) //Only relevant above that size.
                    .collect(Collectors.averagingDouble(line -> {
                double expected = line.get(2);
                double actual = line.get(3);
                return Math.abs(Math.max(expected,actual) / Math.min(expected, actual));
            }));
            System.out.println(avg);
        }
    }
    
    public static List<List<Double>> readFile(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            Function<List<String>, List<Double>> lineProcessor = l -> l.stream().filter(s -> s.length() > 0).map(Double::parseDouble).collect(Collectors.toCollection(ArrayList::new));
            return lines.stream().map(line -> line.split("\t")).map(Arrays::asList).map(lineProcessor).collect(Collectors.toList());
            
        } catch (Exception e) {
            throw new RuntimeException("An " + e.getClass().getName() + " occured while processing " + path.toString() , e);
        }
    }
    
}
