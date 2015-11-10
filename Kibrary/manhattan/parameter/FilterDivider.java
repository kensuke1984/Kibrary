package parameter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import filehandling.sac.SACComponent;

/**
 * Information for {@link manhattan.selection.FilterDivider}
 * 
 * @version 0.0.2
 * @since 2013/9/25
 * 
 * @version 0.0.3 backwardパラメタを追加
 * 
 * @since 2014/9/7
 * @version 0.0.4 to Java 8
 * 
 * @since 2014/10/13
 * @version 0.0.5 outputList modified
 * 
 * 
 * @since 2014/12/26
 * @version 0.0.6 No more outDir and minor fixes
 * 
 * @version 0.0.7
 * @since 2015/1/27 {@link #components} &rarr; {@link Set}
 * 
 * @version 0.0.7.1
 * @since 2015/8/5 {@link IOException}
 * 
 * @version 0.0.8
 * @since 2015/8/8 {@link Path} base
 * 
 * @author kensuke
 * 
 */
public class FilterDivider extends ParameterFile {

	protected Path obsPath;
	protected Path synPath;
	protected String filter;
	protected double delta;
	protected Set<SACComponent> components;

	/**
	 * minimum frequency [Hz] フィルターバンドの最小周波数
	 */
	protected double lowFreq;

	/**
	 * maximum frequency [Hz] フィルターバンドの最大周波数
	 */
	protected double highFreq;

	/**
	 * backward true: zero-phase false: causal
	 */
	protected boolean backward;

	/**
	 * see Saito, n
	 */
	protected int np;

	protected FilterDivider(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		set();
	}

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException if an I/O error occurs
	 * @throws InterruptedException if anything..
	 */
	public static void main(String[] args) throws IOException,InterruptedException {
		Path tmp = null;
		if (0 < args.length)
			tmp = Paths.get(args[0]).toAbsolutePath();
		else
			tmp = readFileName();
		outputList(tmp, StandardOpenOption.CREATE_NEW);
	}

	/**
	 * Output a template file for FiltedrDivider
	 * 
	 * @param outPath
	 *            {@link Path} of a template file
	 * @param options for output
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void outputList(Path outPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.selection.FilterDivider");
			pw.println("#Path of a working folder (full path required)");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#SacComponent[] components to be used");
			pw.println("components Z R T");
			pw.println("#Path of a root folder containing observed dataset");
			pw.println("obsPath . obs");
			pw.println("#Path of a root folder containing synthetic dataset");
			pw.println("synPath . syn");
			pw.println("#double delta");
			pw.println("delta 0.05");
			pw.println("#double lowFreq");
			pw.println("lowFreq 0.005");
			pw.println("#double highFreq");
			pw.println("highFreq 0.08");
			pw.println("#Filter type (lowpass, highpass, bandpass, bandstop)");
			pw.println("filter bandpass");
			pw.println("#int np");
			pw.println("np 4");
			pw.println("#backward  true: zero phase false: causal");
			pw.println("backward true");

		}
		setExecutable(outPath);
	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("obsPath");
		parameterSet.add("synPath");
		parameterSet.add("delta");
		parameterSet.add("components");
		parameterSet.add("highFreq");
		parameterSet.add("lowFreq");
		parameterSet.add("np");
		parameterSet.add("backward");
		parameterSet.add("filter");
		return reader.containsAll(parameterSet);
	}

	/**
	 * parameterのセット
	 */
	private void set() {

		workPath = Paths.get(reader.getString("workPath"));
		obsPath = getPath("obsPath");
		synPath = getPath("synPath");
		delta = reader.getDouble("delta");
		highFreq = reader.getDouble("highFreq");
		lowFreq = reader.getDouble("lowFreq");
		String[] str = reader.getStringArray("components"); // SacComponent
		components = Arrays.stream(str).map(SACComponent::valueOf).collect(Collectors.toSet());

		filter = reader.getString("filter");
		backward = Boolean.parseBoolean(reader.getString("backward"));
		np = reader.getInt("np");
	}
}
