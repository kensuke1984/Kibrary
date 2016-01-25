package io.github.kensuke1984.kibrary.external;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Gnuplot dialog
 * 
 * 
 * @author kensuke
 * @version 0.0.1
 */
public class Gnuplot extends ExternalProcess {
	private PrintWriter standardInput;

	private Gnuplot(Process process) {
		super(process);
		standardInput = new PrintWriter(process.getOutputStream());
	}

	public static Gnuplot createProcess() throws IOException {
		ProcessBuilder builder = null;
		if (isInPath("gnuplot"))
			builder = new ProcessBuilder("gnuplot");
		else
			throw new RuntimeException("No gnuplot in PATH.");

		return new Gnuplot(builder.start());
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
	 * Gnuplotに命令する
	 * 
	 * @param line
	 *            command line for SAC
	 */
	public void inputCMD(String line) {
		standardInput.println(line);
		standardInput.flush();
	}

}
