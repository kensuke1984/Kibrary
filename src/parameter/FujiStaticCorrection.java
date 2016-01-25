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

import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * Information for {@link io.github.kensuke1984.kibrary.datacorrection.FujiStaticCorrection}
 * 
 * @version 0.0.2
 * @since 2013/10/10
 * 
 * @version 0.0.3
 * @since 2014/4/1 {@link #obsPath} {@link #synPath} installed
 * 
 * @version 0.0.4
 * @since 2014/8/19 Constructors changed.
 * 
 * @version 0.0.5
 * @since 2014/9/7 to Java 8
 * 
 * 
 * @version 0.0.6
 * @since 2014/9/11 {@link #checkElements()} installed.
 * 
 * @version 0.0.7
 * @since 2014/10/13 modified.
 * 
 * @version 0.0.8
 * @since 2015/1/23 output name is automatically determined.
 * 
 * 
 * @version 0.1.0
 * @since 2015/5/31 amplitude correction
 * 
 * @version 0.1.1
 * @since 2015/8/8 {@link IOException} {@link Path} base
 * 
 * @author kensuke
 * 
 */
public class FujiStaticCorrection extends ParameterFile {

	/**
	 * components for computation
	 */
	protected Set<SACComponent> components;

	/**
	 * コンボリューションされている波形かそうでないか （両方は無理）
	 */
	protected boolean isConvolved;

	/**
	 * range for searching [s] ±searchRange秒の中でコリレーション最大値探す
	 */
	protected double searchRange;

	/**
	 * the directory for observed data
	 */
	protected Path obsPath;

	/**
	 * the directory for synthetic data
	 */
	protected Path synPath;

	/**
	 * sampling Hz [Hz] in sac files
	 */
	protected double sacSamplingHz;

	/**
	 * シグナルとみなすかどうかの最大振幅から見ての比率
	 */
	protected double threshold;

	protected Path timewindowInformationPath;

	protected FujiStaticCorrection(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		set();
	}

	private void set() {

		workPath = Paths.get(reader.getString("workPath"));
		synPath = getPath("synPath");
		obsPath = getPath("obsPath");
		timewindowInformationPath = getPath("timewindowInformationPath");

		String[] str = reader.getStringArray("components"); // SacComponent
		components = Arrays.stream(str).map(SACComponent::valueOf).collect(Collectors.toSet());

		isConvolved = Boolean.parseBoolean(reader.getString("isConvolved"));
		// sacSamplingHz
		// =Double.parseDouble(reader.getFirstValue("sacSamplingHz")); TODO
		sacSamplingHz = 20;
		searchRange = reader.getDouble("searchRange");
		threshold = reader.getDouble("threshold");
	}

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

	public static void outputList(Path outPath, OpenOption... options) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.datacorrection.FujiStaticCorrection");
			pw.println("#SacComponent[] components to be used");
			pw.println("components Z R T");
			pw.println("#Path of a working folder (full path required)");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#Path of a root directory containing observed dataset");
			pw.println("obsPath . obs");
			pw.println("#Path of a root directory containing synthetic dataset");
			pw.println("synPath . syn");
			pw.println("#Path of a timeWindowInformation file");
			pw.println("timewindowInformationPath timewindow.dat");
			pw.println("#boolean isConvolved");
			pw.println("isConvolved true");
			pw.println("#double sacSamplingHz");
			pw.println("#sacSamplingHz cant change now");
			pw.println("#double threshold");
			pw.println("threshold 0.2");
			pw.println("#double searchRange [s]");
			pw.println("searchRange 10");

		}
		setExecutable(outPath);
	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("components");
		parameterSet.add("isConvolved");
		parameterSet.add("timewindowInformationPath");
		parameterSet.add("synPath");
		parameterSet.add("obsPath");
		parameterSet.add("searchRange");
		// parameterSet.add("sacSamplingHz"); //TODO
		parameterSet.add("threshold");
		return reader.containsAll(parameterSet);

	}

}
