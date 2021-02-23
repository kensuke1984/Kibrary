package io.github.kensuke1984.kibrary;

import io.github.kensuke1984.kibrary.util.Utilities;
import org.apache.commons.lang3.EnumUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Main procedures in Kibrary
 *
 * @author Kensuke Konishi
 * @version 0.0.6.1
 */
public interface Operation {

    static Path findPath() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("."), "*.properties")) {
            List<Path> list = new ArrayList<>();
            int i = 1;
            for (Path path : stream) {
                System.err.println(i++ + ": " + path);
                list.add(path);
            }
            if (list.isEmpty()) throw new NoSuchFileException("No property file is found");
            System.err.print("Which one do you want to use as a property file? [1-" + list.size() + "] ");
            String input = Utilities.readInputLine();
            if (input.isEmpty()) System.exit(9);
            return list.get(Integer.parseInt(input) - 1);
        }
    }

    /**
     * @param args [a name of procedure] (a property file) <br>
     *             -l to show the list of procedures
     * @throws Exception if any
     */
    static void main(String[] args) throws Exception {
        if (args.length == 0) {
            Manhattan.printList();
            System.err.print("Which one do you want to operate? [1-" + Manhattan.values().length + "] ");
            String input = Utilities.readInputLine();
            if (input.isEmpty()) System.exit(1);
            args = new String[]{Manhattan.valueOf(Integer.parseInt(input)).toString()};
        }

        if (args[0].equals("-l")) {
            Manhattan.printList();
            return;
        }

        String[] arguments = Arrays.stream(args).skip(1).toArray(String[]::new);

        if (EnumUtils.isValidEnum(Manhattan.class, args[0])) Manhattan.valueOf(args[0]).invokeMain(arguments);
        else {
            Properties prop = new Properties();
            prop.load(Files.newBufferedReader(Paths.get(args[0])));
            if (!prop.containsKey("manhattan")) throw new RuntimeException("'manhattan' is not set in " + args[0]);
            String manhattan = prop.getProperty("manhattan");
            if (!EnumUtils.isValidEnum(Manhattan.class, manhattan))
                throw new RuntimeException(manhattan + " is not a valid name of Manhattan.");
            try {
                Manhattan.valueOf(manhattan).invokeMain(new String[]{args[0]});
            } catch (Exception e) {
                System.err.println("Could not run " + manhattan + " due to " + e.getCause());
            }
        }
    }

    Path getWorkPath();

    Properties getProperties();

    /**
     * This method creates a file for the properties as the path.
     *
     * @param path    a path for the file (should be *.properties)
     * @param options if any
     * @throws IOException if any
     */
    default void writeProperties(Path path, OpenOption... options) throws IOException {
        Properties p = getProperties();
        p.setProperty("manhattan", getClass().getSimpleName());
        p.store(Files.newBufferedWriter(path, options), "This properties for " + getClass().getName());
    }

    default Path getPath(String key) {
        String path = getProperties().getProperty(key).trim();
        if (path.startsWith("/")) return Paths.get(path);
        return getWorkPath().resolve(path);
    }

    void run() throws Exception;
}
