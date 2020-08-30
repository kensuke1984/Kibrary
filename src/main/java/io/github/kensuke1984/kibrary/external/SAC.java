package io.github.kensuke1984.kibrary.external;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.NoSuchFileException;

/**
 * Sac process made by SACLauncher
 *
 * @author Kensuke Konishi
 * @version 0.1.1
 */
public class SAC extends ExternalProcess implements Closeable {

    /**
     * Input for SAC
     */
    private final PrintWriter STANDARD_INPUT;

    private SAC(Process process) {
        super(process);
        STANDARD_INPUT = new PrintWriter(super.standardInput);
    }

    /**
     * @return SAC operating in a simple process 'sac'. Please care about the working folder.
     * @throws IOException if any
     */
    public static SAC createProcess() throws IOException {
        if (System.getenv("SACAUX") != null && isInPath("sac")) return new SAC(new ProcessBuilder("sac").start());
        throw new NoSuchFileException("No sac in PATH or SACAUX is not set.");
    }

    /**
     * Make an order to SAC
     *
     * @param line command line for SAC
     */
    public void inputCMD(String line) {
        synchronized (super.standardInput) {
            STANDARD_INPUT.println(line);
            STANDARD_INPUT.flush();
        }
    }

    @Override
    public void close() {
        try {
            STANDARD_INPUT.println("q");
            STANDARD_INPUT.flush();
            STANDARD_INPUT.close();
            process.waitFor();
            standardOutput.join();
            standardError.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
