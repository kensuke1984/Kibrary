package io.github.kensuke1984.kibrary.external;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Shell process made by ShellLauncher
 *
 * @Year 2018
 * @author Yuki
 * @version 0.0.1
 */
public class SHELL extends ExternalProcess implements Closeable {

    /**
     * Input for Shell
     */
    private PrintWriter standardInput;

    private SHELL(Process process) {
        super(process);
        standardInput = new PrintWriter(super.standardInput);
    }

    public static SHELL createProcess() throws IOException {
        if (System.getenv("SHELL") != null && isInPath("zsh")) return new SHELL(new ProcessBuilder("zsh").start());
	if (System.getenv("SHELL") != null && isInPath("bash")) return new SHELL(new ProcessBuilder("bash").start());
        throw new RuntimeException("No *sh in PATH or No SHELL is set.");
    }

    /**
     * Make an order to Shell
     *
     * @param line command line for SHELL
     */
    public void inputCMD(String line) {
        synchronized (super.standardInput) {
            standardInput.println(line);
            standardInput.flush();
        }
    }

    @Override
    public void close() {
        try {
            standardInput.println("exit");
            standardInput.flush();
            standardInput.close();
            process.waitFor();
            standardOutput.join();
            standardError.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
