/**
 * 
 */
package manhattan.external;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Sac process made by SacLauncher
 * 
 * 
 * @author kensuke
 * @since 2014/10/02
 * @version 0.0.1
 * 
 * @version 0.0.2
 * @since 2015/4/24 {@link #createProcess()} installed.
 * 
 * @version 0.1.0
 * @since 2015/8/15 renew. {@link IOException}.
 * 
 */
public class Sac extends ExternalProcess implements Closeable {

	private Sac(Process process) {
		super(process);
		standardInput = new PrintWriter(super.standardInput);
	}

	public static Sac createProcess() throws IOException {
		ProcessBuilder builder = null;
		if (System.getenv("SACAUX") != null && isInPath("sac"))
			builder = new ProcessBuilder("sac");
		else
			throw new RuntimeException("No sac in PATH or No SACAUX is set.");
		return new Sac(builder.start());
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
