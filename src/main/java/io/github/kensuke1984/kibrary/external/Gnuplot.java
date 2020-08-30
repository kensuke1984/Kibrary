package io.github.kensuke1984.kibrary.external;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.NoSuchFileException;

/**
 * Gnuplot dialog
 *
 * @author Kensuke Konishi
 * @version 0.0.2
 */
public class Gnuplot extends ExternalProcess {
    private PrintWriter standardInput;

    private Gnuplot(Process process) {
        super(process);
        standardInput = new PrintWriter(process.getOutputStream());
    }

    public static Gnuplot createProcess() throws IOException {
        if (isInPath("gnuplot")) return new Gnuplot(new ProcessBuilder("gnuplot").start());
        throw new NoSuchFileException("No gnuplot in PATH.");
    }

    public void close() throws InterruptedException {
        standardInput.println("q");
        standardInput.flush();
        standardInput.close();
        process.waitFor();
        standardOutput.join();
        standardError.join();
    }

    /**
     * make orders in Gnuplot
     *
     * @param line command line for SAC
     */
    public void inputCMD(String line) {
        standardInput.println(line);
        standardInput.flush();
    }

}
