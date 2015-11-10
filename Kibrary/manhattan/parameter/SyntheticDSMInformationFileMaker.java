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
 * Information for {@link manhattan.dsminformation.SyntheticDSMInformationFileMaker}
 * 
 * @version 0.0.2
 * @since 2013/7/2
 * 
 * @version 0.1.0
 * @since 2013/12/3
 * 
 * @version 0.1.1
 * @since 2014/8/19
 * 
 * 
 * @version 0.1.2 to Java 8
 * @since 2014/9/7
 * 
 * @version 0.1.3 {@link #checkElements()} installed.
 * @since 2014/9/11
 * 
 * @version 0.1.4
 * @since 2014/10/16 modified
 * 
 * 
 * @version 0.1.5
 * @since 2014/10/17 model name and output directory name are now same.
 * 
 * @version 0.1.6
 * @since 2015/1/27 {@link #components} &rarr; {@link Set}
 * 
 * 
 * @version 0.1.7
 * @since 2015/8/8 {@link IOException} {@link Path} base
 * 
 * @version 0.1.7.1
 * @since 2015/8/14
 * 
 * @author kensuke
 * 
 */
public class SyntheticDSMInformationFileMaker extends ParameterFile {

	/**
	 * 周波数ステップ数 2の累乗でないといけない
	 */
	protected int np;

	/**
	 * 時間空間での長さ ２の累乗の１０分の一でないといけない
	 */
	protected double tlen;

	/**
	 * 観測波形を選択する成分
	 */
	protected Set<SACComponent> components;

	/**
	 * Information file name is header_[psv,sh].inf (default:PREM)
	 */
	protected String header;

	/**
	 * structure file instead of PREM
	 */
	protected Path psPath;

	protected SyntheticDSMInformationFileMaker(Path parameterPath) throws IOException {
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
		String[] str = reader.getStringArray("components"); // SacComponent
		components = Arrays.stream(str).map(SACComponent::valueOf).collect(Collectors.toSet());

		header = reader.getString("header");
		// System.exit(0);

		if (reader.containsKey("psPath"))
			psPath = getPath("psPath");

		np = reader.getInt("np");
		tlen = reader.getDouble("tlen");
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
			pw.println("#!java manhattan.dsminformation.SyntheticDSMInformationFileMaker");
			pw.println("#SacComponent[] components to be used");
			pw.println("components Z R T");
			pw.println("#Path of a working folder (full path required)");
			if (outPath.getParent() != null)
				pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			else
				throw new RuntimeException("UNEXPECTED");
			pw.println("#header for names of information files (header_[psv, sh].inf)");
			pw.println("header PREM");
			pw.println("#Path of a structure file you want to use.");
			pw.println("#psPath PREM.psv.model");
			pw.println("#np  must be a power of 2");
			pw.println("np 512 (256 1024 2048)");
			pw.println("#tlen must be (a power of 2)/10");
			pw.println("tlen 6553.6 (1638.4 3276.8 13107.2)");
		}
		setExecutable(outPath);

	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("components");
		parameterSet.add("header");
		// parameterSet.add("psFile");
		parameterSet.add("np");
		parameterSet.add("tlen");
		return reader.containsAll(parameterSet);
	}

}
