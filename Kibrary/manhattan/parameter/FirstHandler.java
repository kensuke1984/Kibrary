package parameter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Information for {@link manhattan.firsthandler.FirstHandler}
 * 
 * @version 0.0.2
 * @since 2013/9/24
 * 
 * @version 0.0.3
 * @since 2014/2/3 rdseed evalresp can be changed
 * 
 * @version 0.0.4
 * @since 2014/8/14 {@link #removeIntermediateFile} installed
 * 
 * @version 0.0.5
 * @since 2014/9/7 to Java 8
 * 
 * @since 2014/9/11
 * @version 0.0.6 {@link #checkElements()} installed.
 * 
 * @since 2015/1/22
 * @version 0.0.7 set executable
 * 
 * @version 0.0.8
 * @since 2015/8/8 {@link IOException} {@link Path} base
 * 
 * @author kensuke
 * 
 */
public class FirstHandler extends ParameterFile {

	protected double samplingHz;

	/**
	 * 使うデータの震央距離の最小値
	 */
	protected double epicentralDistanceMin;
	/**
	 * 使うデータの震央距離の最大値
	 */
	protected double epicentralDistanceMax;
	/**
	 * which catalog to use 0:CMT 1: PDE
	 */
	protected int catalog;

	protected FirstHandler(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		set();
	}

	/**
	 * if remove intermediate file
	 */
	protected boolean removeIntermediateFile;

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Path tmp = null;
		if (0 < args.length)
			tmp = Paths.get(args[0]).toAbsolutePath();
		else
			tmp = readFileName();

		outputList(tmp, StandardOpenOption.CREATE_NEW);
	}

	/**
	 * parameterのセット
	 */
	private void set() {

		workPath = Paths.get(reader.getString("workPath"));
		// System.out.println(workDir);
		// trash = reader.getFirstValue("trash");
		String catalog = reader.getString("catalog");
		String[] epicentralDistanceRange = reader.getStringArray("epicentralDistanceRange");
		epicentralDistanceMin = Double.parseDouble(epicentralDistanceRange[0]);
		epicentralDistanceMax = Double.parseDouble(epicentralDistanceRange[1]);
		if (catalog.equals("cmt") || catalog.equals("CMT"))
			this.catalog = 0;
		else if (catalog.equals("pde") || catalog.equals("PDE"))
			this.catalog = 1;
		else {
			throw new RuntimeException("Invalid catalog name.");
		}
		samplingHz = 20; // TODO
		removeIntermediateFile = Boolean.parseBoolean(reader.getString("removeIntermediateFile"));
	}

	public static void outputList(Path outPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.firsthandler.FirstHandler");
			pw.println("#Path of a working folder (full path required)");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#String a name of catalog to use (cmt or pde)");
			pw.println("catalog cmt");
			pw.println("#double");
			pw.println("#samplingHz 20 (cant change now)");
			pw.println("#epicentral distance range");
			pw.println("epicentralDistanceRange 0 180");
			pw.println("#boolean remove intermediate files if its true");
			pw.println("removeIntermediateFile true");
		}
		setExecutable(outPath);
	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		// parameterSet.add("samplingHz"); TODO
		// parameterSet.add("psvsh");
		parameterSet.add("catalog");
		parameterSet.add("epicentralDistanceRange");
		parameterSet.add("removeIntermediateFile");
		// parameterSet.add("pointFile");
		return reader.containsAll(parameterSet);
	}

}
