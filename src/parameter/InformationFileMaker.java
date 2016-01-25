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

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;

/**
 * 
 * Information for {@link io.github.kensuke1984.kibrary.dsminformation.InformationFileMaker}
 * 
 * @version 0.0.2
 * @since 2014/02/05 {@link #tlen} {@link #np} installed
 * 
 * @version 0.0.3 to Java 8
 * @since 2014/9/7
 * 
 * @version 0.0.4
 * @since 2014/10/14 modified
 * 
 * @version 0.0.4.1
 * @since 2015/8/7 {@link IOException}
 * 
 * @version 0.0.5
 * @since 2015/8/8 {@link Path} base
 * 
 * 
 * @version 0.0.5.1
 * @since 2015/8/14
 * @author kensuke
 * 
 */
public class InformationFileMaker extends ParameterFile {

	/**
	 * np default: 1024
	 */
	protected int np;

	/**
	 * tlen default:3276.8
	 */
	protected double tlen;

	/**
	 * information file of locations of pertubation points.
	 */
	protected Path locationsPath;

	/**
	 * a structure to be used the default is PREM.
	 */
	protected PolynomialStructure ps;

	/**
	 * Information file name is header_[psv,sh].inf
	 */
	protected String header;

	protected Path stationInformationPath;

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

		outputList(tmp,StandardOpenOption.CREATE_NEW);
	}

	// private ParameterReader reader;

	protected InformationFileMaker(Path parameterPath) throws IOException {
		super(parameterPath);
		// this.parameterFile = parameterFile;
		// reader = new ParameterReader(this.parameterFile);
		if (!checkElements())
			throw new RuntimeException();
		set();
	}

	/**
	 * parameterのセット
	 */
	private void set() throws IOException {

		workPath = Paths.get(reader.getString("workPath"));
		Path psPath = getPath("psFile");
		ps = psPath == null ? PolynomialStructure.PREM : new PolynomialStructure(psPath);
		// System.exit(0);

		locationsPath = getPath("locationsPath");
		header = reader.getString("header");

		np = reader.getInt("np");
		tlen = reader.getDouble("tlen");
		stationInformationPath = getPath("stationInformationPath");
		// str = reader.getValues("partialTypes");

	}

	public static void outputList(Path outPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.dsminformation.InformationFileMaker");
			pw.println("#Path of a working folder (full path required)");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#Path of an information file for locations of perturbation point");
			pw.println("locationsPath pointLocations.inf");
			pw.println("#Path of a station information file");
			pw.println("stationInformationPath station.inf");
			pw.println("#header for names of information files (header_[psv, sh].inf)");
			pw.println("header PREM");
			pw.println("#int np must be 2^n");
			pw.println("np 1024 (256 512 2048)");
			pw.println("#double tlen must be 2^n/10");
			pw.println("tlen 3276.8 (1638.4 6553.6 13107.2)");
			pw.println("#polynomial structure file (can be blank)");
			pw.println("#if so or it doesn't exist model is an initial PREM");
			pw.println("psFile ");
		}

		setExecutable(outPath);

	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		// parameterSet.add("psFile");
		parameterSet.add("workPath");
		parameterSet.add("header");
		parameterSet.add("locationsPath");
		parameterSet.add("stationInformationPath");
		parameterSet.add("np");
		parameterSet.add("tlen");
		return reader.containsAll(parameterSet);
	}
}
