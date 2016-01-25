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
 * Information for {@link io.github.kensuke1984.kibrary.selection.DataSelection}
 * 
 * 
 * @version 0.1.7
 * 
 * @author kensuke
 *
 */
public class DataSelection extends ParameterFile {

	protected Set<SACComponent> components;
	protected Path obsPath;
	protected Path synPath;
	protected boolean isConvolved;
	protected Path timewindowInformationPath;
	protected Path staticCorrectionInformationPath;

	/**
	 * Minimum correlation coefficients
	 */
	protected double minCorrelation;

	/**
	 * Maximum correlation coefficients
	 */
	protected double maxCorrelation;

	/**
	 * Minimum variance
	 */
	protected double minVariance;

	/**
	 * Maximum variance
	 */
	protected double maxVariance;

	/**
	 * amplitude のしきい値
	 */
	protected double ratio;

	protected DataSelection(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		set();
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

	private void set() {

		workPath = Paths.get(reader.getString("workPath"));
		obsPath = getPath("obsPath");
		synPath = getPath("synPath");
		String[] str = reader.getStringArray("components"); // SacComponent
		components = Arrays.stream(str).map(SACComponent::valueOf).collect(Collectors.toSet());

		isConvolved = Boolean.parseBoolean(reader.getString("isConvolved"));
		minCorrelation = reader.getDouble("minCorrelation");
		maxCorrelation = reader.getDouble("maxCorrelation");
		minVariance = reader.getDouble("minVariance");
		maxVariance = reader.getDouble("maxVariance");
		ratio = reader.getDouble("ratio");
		timewindowInformationPath = getPath("timewindowInformationPath");
		staticCorrectionInformationPath = getPath("staticCorrectionInformationPath");
		// sacSamplingHz
		// =Double.parseDouble(reader.getFirstValue("sacSamplingHz")); TODO
		// sacSamplingHz = 20;
	}

	public static void outputList(Path outPath, OpenOption... options) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.selection.DataSelection");
			pw.println("#SacComponent[] components to be used");
			pw.println("components Z R T");
			pw.println("#Path of a working folder (fill path required)");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#Path of a root folder containing observed dataset");
			pw.println("obsPath .");
			pw.println("#Path of a root folder containing synthetic dataset");
			pw.println("synPath .");
			pw.println("#boolean isConvolved");
			pw.println("isConvolved true");
			pw.println("#Path of a time window information file");
			pw.println("timewindowInformationPath timewindow.dat");
			pw.println("#Path of a static correction file");
			pw.println("#If you do not want to consider static correction, then comment out the next line");
			pw.println("#staticCorrectionformationPath staticCorrection.dat");
			pw.println("#double sacSamplingHz");
			pw.println("#sacSamplingHz cant change now");
			pw.println("#double minCorrelation");
			pw.println("minCorrelation 0");
			pw.println("#double maxCorrelation");
			pw.println("maxCorrelation 1");
			pw.println("#double minVariance");
			pw.println("minVariance 0");
			pw.println("#double maxVariance");
			pw.println("maxVariance 2");
			pw.println("#double ratio");
			pw.println("ratio 2");
		}

		setExecutable(outPath);

	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("obsPath");
		parameterSet.add("synPath");
		parameterSet.add("components");
		parameterSet.add("isConvolved");
		parameterSet.add("minCorrelation");
		parameterSet.add("maxCorrelation");
		parameterSet.add("minVariance");
		parameterSet.add("maxVariance");
		parameterSet.add("ratio");
		parameterSet.add("timewindowInformationPath");
		return reader.containsAll(parameterSet);

	}

}
