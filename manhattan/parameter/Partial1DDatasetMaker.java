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
import filehandling.spc.PartialType;

/**
 * Information of {@link manhattan.waveformdata.Partial1DDatasetMaker}
 * 
 * 
 * @version 0.0.2
 * @since 2013/11/8
 * 
 * @version 0.1.0
 * @since 2013/12/2 eventファイルを不必要に
 * 
 * @version 0.1.1
 * @since 2014/8/19 Constructors changed.
 * 
 * @version 0.1.2
 * @since 2014/9/7 to Java 8
 * 
 * @version 0.1.3
 * @since 2014/9/11 {@link #checkElements()} installed.
 * 
 * 
 * @version 0.1.4
 * @since 2015/8/6 Source time function
 * 
 * @version 0.1.5
 * @since 2015/8/14 {@link IOException} {@link Path}
 * 
 * @author kensuke
 * 
 */
public class Partial1DDatasetMaker extends ParameterFile {
	protected boolean backward;

	protected int sourceTimeFunction;

	/**
	 * time length (DSM parameter)
	 */
	protected double tlen;

	/**
	 * step of frequency domain (DSM parameter)
	 */
	protected int np;

	/**
	 * radius of perturbation
	 */
	protected double[] bodyR;
	
	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Path tmp = null;
		if (args.length > 0)
			tmp = Paths.get(args[0]).toAbsolutePath();
		else
			tmp = readFileName();
		outputList(tmp, StandardOpenOption.CREATE_NEW);
	}

	protected Set<SACComponent> components;

	public static void outputList(Path outPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.waveformdata.Partial1DDatasetMaker");
			pw.println("#SacComponent[] components to be used");
			pw.println("components Z R T");
			pw.println("#Path of a working directory (full path required)");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#String if it is PREM, spector files are found in (event)/PREM ");
			pw.println("modelName PREM");
			pw.println("#Type source time function 0:none, 1:boxcar, 2:triangle.");
			pw.println("#or folder name containing *.stf if you want to your own GLOBALCMTID.stf ");
			pw.println("sourceTimeFunction 1");
			pw.println("#Path of a timewindow information file");
			pw.println("timewindowPath timewindow.dat");
			pw.println("#Path of a station information file");
			pw.println("stationInformationFilePath station.inf");
			pw.println("#PartialType[] compute types");
			pw.println("partialTypes PAR1 PAR2 PAR3 PAR4 PAR5 PARQ");
			pw.println("#Filter backward");
			pw.println("backward true");
			pw.println("#double minimum value of passband");
			pw.println("fmin 0.005");
			pw.println("#double maximum value of passband");
			pw.println("fmax 0.08");
			pw.println("#double time length (DSM parameter tlen)");
			pw.println("tlen 3276.8 (1638.4 6553.6 13107.2)");
			pw.println("#int step of frequency domain (DSM parameter np)");
			pw.println("np 512 (1024 256)");
			pw.println("#double");
			pw.println("#partialSamplingHz cant change now");
			pw.println("#double");
			pw.println("finalSamplingHz 1");
			pw.println("#radius for perturbation points");
			pw.println("bodyR 3505 3555 3605");
		}
		setExecutable(outPath);
	}

	/**
	 * bp, fp フォルダの下のどこにspcファイルがあるか 直下なら何も入れない（""）
	 */
	protected String modelName;
	/**
	 * Path of a timewindow information file
	 */
	protected Path timewindowPath;

	/**
	 * Partial types
	 */
	protected Set<PartialType> partialTypes;

	/**
	 * bandpassの最小周波数（Hz）
	 */
	protected double fmin;

	/**
	 * bandpassの最大周波数（Hz）
	 */
	protected double fmax;

	/**
	 * spcFileをコンボリューションして時系列にする時のサンプリングHz デフォルトは２０ TODOまだ触れない
	 */
	protected double partialSamplingHz = 20;

	/**
	 * 最後に時系列で切り出す時のサンプリングヘルツ(Hz)
	 */
	protected double finalSamplingHz;
	protected Path sourceTimeFunctionPath;
	protected Path stationInformationFilePath;

	protected Partial1DDatasetMaker(Path parameterPath) throws IOException {
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
		timewindowPath = getPath("timewindowPath");
		// eventInfoFile = newFile(reader.getFirstValue("eventInfoFile"));
		// pointFile = newFile(reader.getFirstValue("pointFile"));
		// System.out.println(reader.getFirstValue("pointFile"));
		String[] str = reader.getStringArray("components"); // SacComponent
		components = Arrays.stream(str).map(SACComponent::valueOf).collect(Collectors.toSet());

		try {
			sourceTimeFunction = Integer.parseInt(reader.getString("sourceTimeFunction"));
		} catch (Exception e) {
			sourceTimeFunction = -1;
			sourceTimeFunctionPath = getPath("sourceTimeFunction");
		}
		backward = Boolean.parseBoolean(reader.getString("backward"));

		modelName = reader.getString("modelName");

		partialTypes = Arrays.stream(reader.getStringArray("partialTypes")).map(PartialType::valueOf)
				.collect(Collectors.toSet());

		fmin = reader.getDouble("fmin");
		fmax = reader.getDouble("fmax");
		tlen = reader.getDouble("tlen");
		np = reader.getInt("np");
		String[] rStr = reader.getStringArray("bodyR");
		bodyR = Arrays.stream(rStr).mapToDouble(Double::parseDouble).toArray();
		// partialSamplingHz
		// =Double.parseDouble(reader.getFirstValue("partialSamplingHz")); TODO
		stationInformationFilePath= getPath("stationInformationFilePath");
		finalSamplingHz = Double.parseDouble(reader.getString("finalSamplingHz"));

	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("components");
		parameterSet.add("modelName");
		parameterSet.add("timewindowPath");
		parameterSet.add("partialTypes");
		parameterSet.add("fmin");
		parameterSet.add("fmax");
		parameterSet.add("np");
		parameterSet.add("tlen");
		parameterSet.add("sourceTimeFunction");
		parameterSet.add("backward");
		parameterSet.add("stationInformationFilePath");
		// parameterSet.add("partialSamplingHz"); TODO
		parameterSet.add("finalSamplingHz");
		 parameterSet.add("bodyR");
		return reader.containsAll(parameterSet);

	}

}
