package io.github.kensuke1984.kibrary.external;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Sac process made by SacLauncher
 * 
 * 
 * @author Kensuke Konishi
 * 
 * @version 0.1.0.1
 * 
 */
public class SAC extends ExternalProcess implements Closeable {

	private SAC(Process process) {
		super(process);
		standardInput = new PrintWriter(super.standardInput);
	}

	public static SAC createProcess() throws IOException {
		if (System.getenv("SACAUX") != null && isInPath("sac"))
			return new SAC(new ProcessBuilder("sac").start());
		throw new RuntimeException("No sac in PATH or No SACAUX is set.");
	}

	/**
	 * Make an order to Sac
	 * 
	 * @param line
	 *            command line for SAC
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
			standardInput.println("q");
			standardInput.flush();
			standardInput.close();
			process.waitFor();
			standardOutput.join();
			standardError.join();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Input for Sac
	 */
	private PrintWriter standardInput;
}
