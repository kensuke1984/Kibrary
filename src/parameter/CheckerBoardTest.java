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
 * 
 * Information for {@link io.github.kensuke1984.kibrary.inversion.CheckerBoardTest}
 * 
 * @version 0.0.2
 * @since 2013/11/15
 * 
 * 
 * @since 2014/8/19
 * @version 0.0.3 Constructors changed
 * 
 * @version 0.0.4
 * @since 2014/9/7 to Java 8
 * 
 * @version 0.0.4.1
 * @since 2015/8/6 {@link IOException}
 * 
 * @version 0.0.5
 * @since 2015/8/8 {@link Path} base
 * 
 * @author kensuke
 *
 */
public class CheckerBoardTest extends ParameterFile {

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Path tmp = null;
		if (0<args.length ) {
			tmp = Paths.get(args[0]).toAbsolutePath();
		} else
			tmp = readFileName();
		outputList(tmp, StandardOpenOption.CREATE_NEW);
	}

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

	protected double noisePower;

	/**
	 * psudoMの元になるファイル
	 */
	protected Path inputDataPath;

	protected CheckerBoardTest(Path parameterPath) throws IOException{
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		// this.parameterFile = parameterFile;
		// reader = new ParameterReader(this.parameterFile);
		set();
	}

	protected CheckerBoardTest() {
	}

	protected boolean noise;

	private void set() {
		workPath = Paths.get(reader.getString("workPath"));
		waveIDPath = getPath("waveIDPath");
		partialWaveformPath = getPath("partialWaveformPath");
		partialIDPath = getPath("partialIDPath");
		unknownParameterListPath = getPath("unknownParameterListPath");
		waveformPath = getPath("waveformPath");
		inputDataPath = getPath("inputDataPath");
		noise = Boolean.parseBoolean(reader.getString("noise"));
		noisePower = reader.getDouble("noisePower");
		iterate = Boolean.parseBoolean(reader.getString("iterate"));
		// System.exit(0);
	}

	protected boolean iterate;

	public static void outputList(Path outPath, OpenOption... options) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.inversion.CheckerBoardTest");
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
			pw.println("#Path of an unknown parameter list file");
			pw.println("unknownParameterListPath unknowns.inf");
			pw.println("#Path of an input data list file");
			pw.println("inputDataPath input.inf");
			pw.println("#for Iterate");
			pw.println("iterate false");
			pw.println("#boolean noise");
			pw.println("noise true");
			pw.println("#noise power");
			pw.println("noisePower 1000");
		}
		setExecutable(outPath);
	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("waveIDPath");
		// parameterSet.add("sacSamplingHz"); //TODO
		parameterSet.add("partialWaveformPath");
		parameterSet.add("partialIDPath");
		parameterSet.add("unknownParameterListPath");
		parameterSet.add("waveformPath");
		parameterSet.add("inputDataPath");
		parameterSet.add("noise");
		parameterSet.add("iterate");
		parameterSet.add("noisePower");
		return reader.containsAll(parameterSet);
	}
}
