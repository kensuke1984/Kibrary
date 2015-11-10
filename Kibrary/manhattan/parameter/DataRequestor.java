/**
 * 
 */
package parameter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Information for {@link manhattan.datarequest.DataRequestor}
 * 
 * @since 2015/02/09
 * @version 0.0.1
 * @author kensuke
 * 
 * 
 * @version 0.0.2
 * @since 2015/8/14 {@link IOException} {@link Path} base
 * 
 * @version 0.0.3
 * @since 2015/9/11 only username can be used
 * 
 *
 */
public class DataRequestor extends ParameterFile {

	protected DataRequestor(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		set();
	}

	protected String institute;
	protected String mail;
	protected String email;
	protected String phone;
	protected String fax;
	// protected String label;
	protected String media;
	// protected String[] alternateMedia;
	protected String[] networks;
	protected LocalDate startDate;
	
	/**
	 * including the date
	 */
	protected LocalDate endDate;
	protected double lowerMw;
	protected double upperMw;
	protected double lowerLatitude;
	protected double upperLatitude;
	protected double lowerLongitude;
	protected double upperLongitude;
	/**
	 * not radius but distance from the surface
	 */
	protected double lowerDepth;
	/**
	 * not radius but distance from the surface
	 */
	protected double upperDepth;

	private void set() {
		institute = String.join(" ", reader.getStringArray("institute"));
		mail = String.join(" ", reader.getStringArray("mail"));
		email = reader.getString("email");
		phone = String.join(" ", reader.getStringArray("phone"));
		fax = String.join(" ", reader.getStringArray("fax"));
		// label = reader.getString("label");
		media = reader.getString("media");
		// alternateMedia = reader.getStringArray("alternateMedia");
		networks = reader.getStringArray("networks");
		lowerDepth = reader.getDouble("lowerDepth");
		lowerLatitude = reader.getDouble("lowerLatitude");
		lowerLongitude = reader.getDouble("lowerLongitude");
		lowerMw = reader.getDouble("lowerMw");
		upperDepth = reader.getDouble("upperDepth");
		upperLatitude = reader.getDouble("upperLatitude");
		upperLongitude = reader.getDouble("upperLongitude");
		upperMw = reader.getDouble("upperMw");

		startDate = LocalDate.parse(reader.getString("startDate"));
		endDate = LocalDate.parse(reader.getString("endDate"));
		headAdjustment = reader.getInt("headAdjustment");
		footAdjustment = reader.getInt("footAdjustment");

	}

	protected int headAdjustment;
	protected int footAdjustment;

	/*
	 * (non-Javadoc)
	 * 
	 * @see parameter.ParameterFile#checkElements()
	 */
	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("institute");
		parameterSet.add("mail");
		parameterSet.add("email");
		parameterSet.add("phone");
		parameterSet.add("fax");
		// parameterSet.add("label");
		parameterSet.add("media");
		// parameterSet.add("alternateMedia");
		parameterSet.add("networks");

		return reader.containsAll(parameterSet);
	}

	private static void outputList(Path outPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.datarequest.DataRequestor");
			String userName = System.getProperty("user.name");
			pw.println("#Institute institute");
			if (userName.equals("kensuke")) {
				pw.println("institute Academia Sinica");
				pw.println("#Mail of your institute");
				pw.println("mail 128, Sec. 2, Academia Road, Nangang, Taipei 11529, Taiwan");
				pw.println("#Phone number");
				pw.println("phone +886-2-2783-9910");
				pw.println("#Fax number");
				pw.println("fax +886-2-2783-9871");
			} else {
				pw.println("institute University of Tokyo");
				pw.println("#Mail of your institute");
				pw.println("mail 7-3-1 Hongo, Bunkyo, Tokyo, Japan");
				pw.println("#Phone number");
				pw.println("phone 03-5841-4290");
				pw.println("#Fax number");
				pw.println("fax 03-5841-8791");
			}
			pw.println("#Email address");
			pw.println("email waveformrequest2015@gmail.com");
			pw.println("#media");
			pw.println("media FTP");
			pw.println("#Network names for request");
			pw.println("#Note that it will make a request for all stations in the networks.");
			pw.println("networks II IU _US-All");
			pw.println("#Starting date yyyy-mm-dd");
			pw.println("startDate 1990-01-01");
			pw.println("#End date yyyy-mm-dd");
			pw.println("endDate 2014-12-31");
			pw.println("#Lower limit of Mw");
			pw.println("lowerMw 5.5");
			pw.println("#Upper limit of Mw");
			pw.println("upperMw 6.5");
			pw.println("#All geometrical filter is for seismic events.");
			pw.println("#Lower limit of latitude [deg] [-90:upperLatitude)");
			pw.println("lowerLatitude 0");
			pw.println("#Upper limit of latitude [deg] (lowerLatitude:90]");
			pw.println("upperLatitude 20");
			pw.println("#Lower limit of longitude [deg] [-180:upperLongitude)");
			pw.println("lowerLongitude 120");
			pw.println("#Upper limit of longitude [deg] (lowerLongitude:360]");
			pw.println("upperLongitude 180");
			pw.println("#Shallower limit of depth (Not radius)");
			pw.println("lowerDepth 150");
			pw.println("#Deeper limit of depth (Not radius)");
			pw.println("upperDepth 650");
			pw.println("#Adjustment at the head [min] (must be integer)");
			pw.println("headAdjustment -10");
			pw.println("#Adjustment at the foot [min] (must be integer)");
			pw.println("footAdjustment 120");
		}

		setExecutable(outPath);

	}

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

}
