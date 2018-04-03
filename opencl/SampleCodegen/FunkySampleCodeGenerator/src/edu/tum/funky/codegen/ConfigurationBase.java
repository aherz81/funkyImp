/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.tum.funky.codegen;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

/**
 * Convenience class for Java Property File handling.
 * @author Alexander Pöppl
 */
public class ConfigurationBase {

    private final Properties configValues;

    private ConfigurationBase(String file) throws IOException {
        this.configValues = new Properties();
        Path filePath = Paths.get(file);
        this.configValues.load(new FileInputStream(filePath.toFile()));
    }

    public static Optional<ConfigurationBase> load(String pathToConfig) {
        try {
            return Optional.of(new ConfigurationBase(pathToConfig));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    public Optional<String> getString(String key) {
        return Optional.ofNullable(this.configValues.getProperty(key));
    }

    public Optional<Integer> getInteger(String key) {
        try {
            return Optional.ofNullable(Integer.parseInt(this.configValues.getProperty(key)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public Optional<Boolean> getBoolean(String key) {
        return Optional.ofNullable(Boolean.parseBoolean(this.configValues.getProperty(key)));
    }

    public Optional<Long> getLong(String key) {
        try {
            return Optional.ofNullable(Long.parseLong(this.configValues.getProperty(key)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
