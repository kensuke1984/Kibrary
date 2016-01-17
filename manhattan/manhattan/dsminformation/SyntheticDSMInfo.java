package manhattan.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.Location;
import manhattan.template.Station;

/**
 * Information file for TIPSV and TISH
 * 
 * @version 0.1.6
 * 
 * 
 * @author kensuke
 * 
 */
public class SyntheticDSMInfo extends DSMheader {

	protected final PolynomialStructure ps;

	protected final String outputDir;

	/**
	 * <b>unmodifiable</b>
	 */
	protected final Set<Station> stations;

	protected final GlobalCMTID eventID;

	/**
	 * @param structure
	 *            of velocity
	 * @param eventID
	 *            {@link GlobalCMTID}
	 * @param stations
	 *            ステーション情報
	 * @param outputDir
	 *            name of outputDir (relative PATH)
	 * @param tlen
	 *            tlen[s]
	 * @param np
	 *            np
	 */
	public SyntheticDSMInfo(PolynomialStructure structure, GlobalCMTID eventID, Set<Station> stations, String outputDir,
			double tlen, int np) {
		super(tlen, np);
		this.ps = structure;
		this.eventID = eventID;
		this.stations = Collections.unmodifiableSet(stations);
		this.outputDir = outputDir;
		momentTensor = eventID.getEvent().getCmt().getDSMmt();
		eventLocation = eventID.getEvent().getCmtLocation();
	}

	/**
	 * the moment tensor of the event
	 */
	protected final double[] momentTensor;

	/**
	 * the location of the event
	 */
	protected final Location eventLocation;

	/**
	 * PSV計算用のファイル出力
	 * 
	 * @param psvPath
	 *            Path of an output file
	 * @param options
	 *            for output
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void writePSV(Path psvPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(psvPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structure = ps.toPSVlines();
			Arrays.stream(structure).forEach(pw::println);

			// source
			pw.println("c parameter for the source");
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			// double[] mt = momentTensor;
			Arrays.stream(momentTensor).forEach(mt -> pw.print(mt + " "));
			pw.println("Moment Tensor (1.e25 dyne cm)");

			// station
			pw.println("c parameter for the station");
			pw.println("c the number of stations");
			pw.println(stations.size() + " nr");
			pw.println("c latitude longitude (deg)");
			stations.forEach(station -> pw.println(station.getPosition()));

			// output
			pw.println("c parameter for the output file");
			stations.forEach(
					station -> pw.println(outputDir + "/" + station.getStationName() + "." + eventID + "PSV.spc"));
			pw.println("end");

		}
	}

	/**
	 * SH計算用のファイル出力
	 * 
	 * @param outPath
	 *            output path
	 * @param options
	 *            for output
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void writeSH(Path outPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structure = ps.toSHlines();
			Arrays.stream(structure).forEach(pw::println);

			// source
			pw.println("c parameter for the source");
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			// double[] mt = momentTensor.getDSMmt();
			Arrays.stream(momentTensor).forEach(mt -> pw.print(mt + " "));
			pw.println("Moment Tensor (1.e25 dyne cm)");

			// station
			pw.println("c parameter for the station");
			pw.println("c the number of stations");
			pw.println(stations.size() + " nr");
			pw.println("c latitude longitude (deg)");
			stations.forEach(station -> pw.println(station.getPosition()));

			// output
			pw.println("c parameter for the output file");
			stations.forEach(
					station -> pw.println(outputDir + "/" + station.getStationName() + "." + eventID + "SH.spc"));
			pw.println("end");

		}
	}

	public Set<Station> getStation() {
		return stations;
	}

}
