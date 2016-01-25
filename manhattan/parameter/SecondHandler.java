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
 * Parameter file for {@link io.github.kensuke1984.kibrary.selection.SecondHandler}
 * 
 * @version 0.0.2
 * @since 2013/9/25
 * 
 * @version 0.0.3
 * @since 2013/11/21 地理的判断をstationとeventに分割
 * 
 * @version 0.0.4
 * @since 2014/9/7 to Java 8
 * 
 * @version 0.0.5
 * @since 2014/9/11 {@link #checkElements()} installed.
 * 
 * @version 0.0.7
 * @since 2015/1/21 slightly changed
 * 
 * @version 0.0.7.1
 * @since 2015/5/20 latitude values changed
 * 
 * @version 0.0.8
 * @since 2015/8/8 {@link IOException} {@link Path} base
 * 
 * @version 0.0.8.1
 * @since 2015/8/9 {@link InterruptedException}
 * 
 * @version 0.0.9
 * @since 2015/8/25 Bug fixed.
 * 
 * 
 * @author kensuke
 *
 */
public class SecondHandler extends ParameterFile {

	/**
	 * DEPMIN DEPMAX DEPMEN がnanでないかどうか
	 */
	protected boolean checkNaNinDEPMS;

	/**
	 * SACのDELTA
	 */
	protected double delta;

	/**
	 * NPTSによる判定を行うか
	 */
	protected boolean checkNPTS;
	protected int npts;

	/**
	 * [0, 180] GCARCによる判定を行うか
	 */
	protected boolean checkGCARC;
	protected double minGCARC;
	protected double maxGCARC;

	/**
	 * [-90, 90] station Latitudeによる判定を行うか
	 */
	protected boolean checkStationLatitude;
	protected double minStationLatitude;
	protected double maxStationLatitude;
	/**
	 * [-90, 90] event Latitudeによる判定を行うか
	 */
	protected boolean checkEventLatitude;
	protected double minEventLatitude;
	protected double maxEventLatitude;

	/**
	 * (-180, 180] station Longitudeによる判定を行うか
	 */
	protected boolean checkStationLongitude;
	protected double minStationLongitude;
	protected double maxStationLongitude;
	/**
	 * (-180, 180] event Longitudeによる判定を行うか
	 */
	protected boolean checkEventLongitude;
	protected double minEventLongitude;
	protected double maxEventLongitude;

	protected SecondHandler(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		set();
	}

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws InterruptedException
	 *             if something
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		Path tmp = null;
		if (0 < args.length)
			tmp = Paths.get(args[0]).toAbsolutePath();
		else
			tmp = readFileName();

		outputList(tmp, StandardOpenOption.CREATE_NEW);
	}

	/**
	 * parameterのセット
	 */
	private void set() {

		workPath = Paths.get(reader.getString("workPath"));
		checkNaNinDEPMS = Boolean.parseBoolean(reader.getString("depMs"));
		delta = reader.getDouble("delta");
		// System.out.println(delta);
		checkGCARC = Boolean.parseBoolean(reader.getString("checkGCARC"));
		checkStationLatitude = Boolean.parseBoolean(reader.getString("checkStationLatitude"));
		checkStationLongitude = Boolean.parseBoolean(reader.getString("checkStationLongitude"));
		checkEventLatitude = Boolean.parseBoolean(reader.getString("checkEventLatitude"));
		checkEventLongitude = Boolean.parseBoolean(reader.getString("checkEventLongitude"));
		checkNPTS = Boolean.parseBoolean(reader.getString("checkNPTS"));
		minGCARC = reader.getDouble("minGCARC");
		maxGCARC = reader.getDouble("maxGCARC");
		minStationLatitude = reader.getDouble("minStationLatitude");
		maxStationLatitude = reader.getDouble("maxStationLatitude");
		minStationLongitude = reader.getDouble("minStationLongitude");
		maxStationLongitude = reader.getDouble("maxStationLongitude");
		minEventLatitude = reader.getDouble("minEventLatitude");
		maxEventLatitude = reader.getDouble("maxEventLatitude");
		minEventLongitude = reader.getDouble("minEventLongitude");
		maxEventLongitude = reader.getDouble("maxEventLongitude");
		npts = Integer.parseInt(reader.getString("npts"));

	}

	private static void outputList(Path outPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.selection.SecondHandler");
			pw.println("#Path of a working folder (full path is required)");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#double delta");
			pw.println("delta 0.05");
			pw.println("#boolean depMs");
			pw.println("depMs true");
			pw.println("#boolean checkNPTS");
			pw.println("checkNPTS true");
			pw.println("#int npts");
			pw.println("npts 131072 (32768 65536)");
			pw.println("#boolean checkGCARC");
			pw.println("checkGCARC true");
			pw.println("#double minGCARC");
			pw.println("minGCARC 0");
			pw.println("#double maxGCARC");
			pw.println("maxGCARC 180");
			pw.println("#boolean checkStationLatitude");
			pw.println("checkStationLatitude false");
			pw.println("#double minStationLatitude");
			pw.println("minStationLatitude -90");
			pw.println("#double maxStationLatitude");
			pw.println("maxStationLatitude 90");
			pw.println("#boolean checkStationLongitude");
			pw.println("checkStationLongitude false");
			pw.println("#double minStationLongitude");
			pw.println("minStationLongitude -180");
			pw.println("#double maxStationLongitude");
			pw.println("maxStationLongitude 360");
			pw.println("#boolean checkEventLatitude");
			pw.println("checkEventLatitude false");
			pw.println("#double minEventLatitude");
			pw.println("minEventLatitude -90");
			pw.println("#double maxEventLatitude");
			pw.println("maxEventLatitude 90");
			pw.println("#boolean checkEventLongitude");
			pw.println("checkEventLongitude false");
			pw.println("#double minEventLongitude");
			pw.println("minEventLongitude -180");
			pw.println("#double maxEventLongitude");
			pw.println("maxEventLongitude 360");
		}
		setExecutable(outPath);

	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("depMs");
		parameterSet.add("delta");
		parameterSet.add("checkNPTS");
		parameterSet.add("npts");
		parameterSet.add("checkGCARC");
		parameterSet.add("minGCARC");
		parameterSet.add("maxGCARC");
		parameterSet.add("checkStationLatitude");
		parameterSet.add("minStationLatitude");
		parameterSet.add("maxStationLatitude");
		parameterSet.add("checkStationLongitude");
		parameterSet.add("minStationLongitude");
		parameterSet.add("maxStationLongitude");
		parameterSet.add("checkEventLatitude");
		parameterSet.add("minEventLatitude");
		parameterSet.add("maxEventLatitude");
		parameterSet.add("checkEventLongitude");
		parameterSet.add("minEventLongitude");
		parameterSet.add("maxEventLongitude");
		// parameterSet.add("pointFile");
		return reader.containsAll(parameterSet);
	}

}
