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
 * 
 * Information for {@link manhattan.waveformdata.ObservedSyntheticDatasetMaker}
 * 
 * @version 0.0.8.1
 * 
 * @author kensuke
 * 
 */
public class ObservedSyntheticDatasetMaker extends ParameterFile {

	/**
	 * {@link Path} of a root folder containing observed dataset
	 */
	protected Path obsPath;

	/**
	 * {@link Path} of a root folder containing synthetic dataset
	 */
	protected Path synPath;

	/**
	 * {@link Path} of a timewindow information file
	 */
	protected Path timewindowInformationPath;

	/**
	 * {@link Path} of a static correction file
	 */
	protected Path staticCorrectionPath;

	/**
	 * Sacのサンプリングヘルツ （これと異なるSACはスキップ）
	 */
	protected double sacSamplingHz;

	/**
	 * 切り出すサンプリングヘルツ
	 */
	protected double finalSamplingHz;

	/**
	 * データセットに含む成分
	 */
	protected Set<SACComponent> components;

	/**
	 * コンボリューションしてあるデータを使うか
	 */
	protected boolean isConvolved;

	/**
	 * 観測波形と理論波形のタイムシフトを行うか
	 */
	protected boolean timeShift;

	/**
	 * if it corrects amplitude ratio
	 */
	protected boolean amplitudeCorrection;

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Path tmp = null;
		if (0 < args.length)
			tmp = Paths.get(args[0]).toAbsolutePath();
		else
			tmp = readFileName();
		outputList(tmp, StandardOpenOption.CREATE_NEW);
	}

	protected ObservedSyntheticDatasetMaker(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		set();
	}

	/**
	 * parameterのセット
	 */
	private void set() {

		workPath = Paths.get(reader.getString("workPath"));
		obsPath = getPath("obsPath");
		synPath = getPath("synPath");
		timewindowInformationPath = getPath("timewindowInformationPath");
		String[] str = reader.getStringArray("components"); // SacComponent
		components = Arrays.stream(str).map(SACComponent::valueOf).collect(Collectors.toSet());

		isConvolved = Boolean.parseBoolean(reader.getString("isConvolved"));
		timeShift = Boolean.parseBoolean(reader.getString("timeShift"));
		staticCorrectionPath = getPath("staticCorrectionPath");
		amplitudeCorrection = Boolean.parseBoolean(reader.getString("amplitudeCorrection"));
		if ((timeShift || amplitudeCorrection) && (null == staticCorrectionPath || !Files.exists(staticCorrectionPath)))
			throw new RuntimeException("staticCorrectionPath is blank or invalid");

		// sacSamplingHz
		// =Double.parseDouble(reader.getFirstValue("sacSamplingHz")); TODO
		sacSamplingHz = 20;
		finalSamplingHz = reader.getDouble("finalSamplingHz");

	}

	public static void outputList(Path outPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.waveformdata.ObservedSyntheticDatasetMaker");
			pw.println("#SacComponent[] components to be used");
			pw.println("components Z R T");
			pw.println("#Path of a working directory");
			pw.println("workPath . (" + outPath.getParent().toAbsolutePath() + ")");
			pw.println("#Path of a root folder containing observed dataset");
			pw.println("obsPath .");
			pw.println("#Path of a root folder containing synthetic dataset");
			pw.println("synPath .");
			pw.println("#Path of a timewindow information file");
			pw.println("timewindowInformationPath timewindow.dat");
			pw.println("#boolean convolved");
			pw.println("isConvolved true");
			pw.println("#boolean timeShift");
			pw.println("timeShift true");
			pw.println("#boolean amplitudeCorrection");
			pw.println("amplitudeCorrection false");
			pw.println("#Path of a static correction file");
			pw.println("staticCorrectionPath staticCorrection.dat");
			pw.println("#double");
			pw.println("#sacSamplingHz cant change now");
			pw.println("#double");
			pw.println("finalSamplingHz 1");

		}
		setExecutable(outPath);
	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("obsPath");
		parameterSet.add("synPath");
		parameterSet.add("timewindowInformationPath");
		// parameterSet.add("staticCorrectionPath");
		// parameterSet.add("sacSamplingHz"); //TODO
		parameterSet.add("components");
		parameterSet.add("isConvolved");
		parameterSet.add("finalSamplingHz");
		parameterSet.add("timeShift");
		parameterSet.add("amplitudeCorrection");
		return reader.containsAll(parameterSet);
	}

}
