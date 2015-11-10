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
import manhattan.dsminformation.PolynomialStructure;

/**
 * Information for {@link manhattan.waveformdata.PartialDatasetMaker}
 * 
 * @since 2014/8/19
 * @version 0.0.2 Constructors changed.
 * 
 * @since 2014/9/7
 * @version 0.0.3 to Java 8
 * 
 * 
 * @since 2014/9/11
 * @version 0.0.4 {@link #checkElements()} installed.
 * 
 * @version 0.0.5
 * @since 2014/10/19 output file names are now automatically given.
 * 
 * 
 *        workDir=. ok
 * 
 * @version 0.0.6
 * @since 2015/1/27 {@link #components} &rarr; {@link Set}
 * @version 0.0.7
 * @since 2015/2/13 {@link #partialTypes} &rarr; {@link Set}
 * 
 * @version 0.0.7.1
 * @since 2015/8/5 modified for {@link IOException}
 * 
 * @version 0.0.8 {@link Path} base
 * 
 * 
 * @author kensuke
 *
 */
public class PartialDatasetMaker extends ParameterFile {

	protected Set<SACComponent> components;

	/**
	 * BPinfo このフォルダの直下に 0000????を置く
	 */
	protected Path bpPath;
	/**
	 * FPinfo このフォルダの直下に イベントフォルダ（FP）を置く
	 */
	protected Path fpPath;

	protected boolean convolve;

	/**
	 * bp, fp フォルダの下のどこにspcファイルがあるか 直下なら何も入れない（""）
	 */
	protected String modelName;

	/**
	 * タイムウインドウ情報のファイル
	 */
	protected Path timewindowPath;

	/**
	 * set of partial type for computation
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

	/**
	 * structure for Q partial
	 */
	protected PolynomialStructure structure;

	protected PartialDatasetMaker(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		set();
	}

	/**
	 * parameterのセット
	 */
	private void set() throws IOException {

		workPath = Paths.get(reader.getString("workPath"));
		bpPath = getPath("bpPath");
		fpPath = getPath("fpPath");
		timewindowPath = getPath("timewindowPath");
		String[] str = reader.getStringArray("components"); // SacComponent
		components = Arrays.stream(str).map(SACComponent::valueOf).collect(Collectors.toSet());

		if (reader.containsKey("qinf"))
			structure = new PolynomialStructure(getPath("qinf"));

		convolve = Boolean.parseBoolean(reader.getString("convolve"));

		modelName = reader.getString("modelName");

		String[] parStr = reader.getStringArray("partialTypes");
		partialTypes =  Arrays.stream(parStr).map(PartialType::valueOf).collect(Collectors.toSet());

		fmin = reader.getDouble("fmin");
		fmax = reader.getDouble("fmax");

		// partialSamplingHz
		// =Double.parseDouble(reader.getFirstValue("partialSamplingHz")); TODO

		finalSamplingHz = Double.parseDouble(reader.getString("finalSamplingHz"));

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
			pw.println("#!java manhattan.waveformdata.PartialDatasetMaker");
			pw.println("#SacComponent[] components to be used");
			pw.println("components T R Z");
			pw.println("#Path of a working folder (full path required)");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#Path of a back propagate spc folder");
			pw.println("bpPath BPinfo");
			pw.println("#Path of a forward propagate spc folder");
			pw.println("fpPath FPinfo");
			pw.println("#String if it is PREM spector file is in bpdir/PREM ");
			pw.println("modelName PREM");
			pw.println("#boolean source time function consideration");
			pw.println("convolve true");
			pw.println("#Path of a time window file");
			pw.println("timewindowPath timewindow.dat");
			pw.println("#PartialType[] compute types");
			pw.println("partialTypes MU");
			pw.println("#double minimum value of passband");
			pw.println("fmin 0.005");
			pw.println("#double maximum value of passband");
			pw.println("fmax 0.08");
			pw.println("#double");
			pw.println("#partialSamplingHz cant change now");
			pw.println("#double");
			pw.println("finalSamplingHz 1");
			pw.println("#File for Qstructure (if no file, then PREM)");
			pw.println("#qinf prem.inf");
		}
		setExecutable(outPath);
	}

	@Override
	boolean checkElements() {

		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("fpPath");
		parameterSet.add("bpPath");
		parameterSet.add("components");
		parameterSet.add("modelName");
		parameterSet.add("timewindowPath");
		parameterSet.add("partialTypes");
		parameterSet.add("fmin");
		parameterSet.add("fmax");
		parameterSet.add("convolve");
		// parameterSet.add("partialSamplingHz"); TODO
		parameterSet.add("finalSamplingHz");
		return reader.containsAll(parameterSet);

	}
}
