package io.github.kensuke1984.kibrary;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Runtime environment
 *
 * @author Kensuke Konishi
 * @version 0.0.1.1
 */
public class Environment {
    public final static Path KIBRARY_HOME = Paths.get(System.getProperty("user.home") + "/.Kibrary");

    private Environment() {
    }

    /**
     * Shows environment information
     *
     * @param args will be ignored
     */
    public static void main(String[] args) {
//		System.getProperties().keySet().forEach(System.out::println);
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("User name: " + System.getProperty("user.name"));
        System.out.println("Language: " + System.getProperty("user.language"));
        System.out.println("Time zone: " + System.getProperty("user.timezone"));
        System.out.println("Home directory: " + System.getProperty("user.home"));
        System.out.println("Java Specification version: " + System.getProperty("java.specification.version"));
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Java Virtual version: " + System.getProperty("java.vm.version"));
        System.out.println("Java Runtime version: " + System.getProperty("java.runtime.version"));
        System.out.println("Available processor: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Max memory[GB]: " + Runtime.getRuntime().maxMemory() / 1000000000.0);
    }

}
