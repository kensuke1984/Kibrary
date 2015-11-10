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

import manhattan.inversion.InverseMethodEnum;

/**
 * Information of {@link manhattan.inversion.LetMeInvert}
 * 
 * @version 0.0.2
 * @since 2013/9/25
 * 
 * @version 0.1.0
 * @since 2013/12/2 イベント情報ファイルを不要に
 * 
 * @version 0.1.1
 * @since 2014/9/7 to Java 8
 * 
 * @since 2014/9/11
 * @version 0.1.2 {@link #checkElements()} installed.
 * 
 * @version 0.1.2.1
 * @since 2015/8/7 {@link IOException}
 * 
 * @version 0.1.3
 * @since 2015/8/8 {@link Path} base
 * 
 * @author kensuke
 * 
 */
public class LetMeInvert extends ParameterFile {

	/**
	 * 観測波形、理論波形の入ったファイル (BINARY)
	 */
	protected Path waveformPath;

	/**
	 * 求めたい未知数を羅列したファイル (ASCII)
	 */
	protected Path unknownParameterListPath;

	/**
	 * partialIDの入ったファイル
	 */
	protected Path partialIDPath;
	/**
	 * partial波形の入ったファイル
	 */
	protected Path partialWaveformPath;

	/**
	 * 観測、理論波形のID情報
	 */
	protected Path waveIDPath;

	/**
	 * ステーション位置情報のファイル
	 */
	protected Path stationInformationPath;

//	/**
//	 * 何番目の解でボルン波形を作るか
//	 */
//	protected int[] bornAnswer = { 1, 5, 10 };

	/**
	 * どうやって方程式を解くか。 cg svd
	 */
	protected Set<InverseMethodEnum> inverseMethods;

	private void set() {

		workPath = Paths.get(reader.getString("workPath"));
		stationInformationPath = getPath("stationInformationPath");
		waveIDPath = getPath("waveIDPath");
		partialWaveformPath = getPath("partialWaveformPath");
		partialIDPath = getPath("partialIDPath");
		unknownParameterListPath = getPath("unknownParameterListPath");
		waveformPath = getPath("waveformPath");

		String[] alphaParts = reader.getStringArray("alpha");
		alpha = Arrays.stream(alphaParts).mapToDouble(Double::parseDouble).toArray();

//		String[] bornParts = reader.getStringArray("bornAnswer");
//		bornAnswer = Arrays.stream(bornParts).mapToInt(Integer::parseInt).toArray();

		String[] inverseMethodsStr = reader.getStringArray("inverseMethodsStr");
		inverseMethods = Arrays.stream(inverseMethodsStr).map(InverseMethodEnum::valueOf).collect(Collectors.toSet());
	}

	/**
	 * AIC計算に用いるα 独立データ数はn/αと考える
	 */
	protected double[] alpha = { 1, 12.5, 25, 125 };

	protected LetMeInvert(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		set();
	}

	protected LetMeInvert() {
		inverseMethods = new HashSet<>(Arrays.asList(InverseMethodEnum.values()));
	}

	public static void outputList(Path outPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.inversion.LetMeInvert");
			pw.println("#Path of a working folder (full path required)");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#Path of a waveID file");
			pw.println("waveIDPath waveID.dat");
			pw.println("#Path of a waveform file");
			pw.println("waveformPath waveform.dat");
			pw.println("#Path of a partial id file");
			pw.println("partialIDPath partialID.dat");
			pw.println("#Path of a partial waveform file");
			pw.println("partialWaveformPath partial.dat");
			pw.println("#Path of a unknown parameter list file");
			pw.println("unknownParameterListPath unknowns.inf");
			pw.println("#Path of a station information file");
			pw.println("stationInformationPath station.inf");
			pw.println("#double[] alpha it self");
			pw.println("alpha 1 12.5 25 125");
//			pw.println("#int[] born answer ");
//			pw.println("bornAnswer 1 5 10");
			pw.println("#inverseMethodsStr[] inverse methods (cg, svd) must be capital letters");
			pw.println("inverseMethodsStr CG SVD ");

		}
		setExecutable(outPath);
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

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("stationInformationPath");
		parameterSet.add("waveIDPath");
		// parameterSet.add("sacSamplingHz"); //TODO
		parameterSet.add("partialWaveformPath");
		parameterSet.add("partialIDPath");
		parameterSet.add("unknownParameterListPath");
		parameterSet.add("waveformPath");
		parameterSet.add("alpha");
//		parameterSet.add("bornAnswer");
		parameterSet.add("inverseMethodsStr");
		return reader.containsAll(parameterSet);
	}

}
