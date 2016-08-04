package io.github.kensuke1984.kibrary.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

/**
 * Information file for TIPSV and TISH
 * 
 * @version 0.1.7
 * @author Kensuke Konishi
 * 
 */
public class SyntheticDSMInfo extends DSMheader {

	protected final PolynomialStructure structure;

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
		this.structure = structure;
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
			String[] structurePart = structure.toPSVlines();
			Arrays.stream(structurePart).forEach(pw::println);

			// source
			pw.println("c parameter for the source");
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			pw.println(Arrays.stream(momentTensor).mapToObj(Double::toString).collect(Collectors.joining(" "))
					+ " Moment Tensor (1.e25 dyne cm)");

			// station
			pw.println("c parameter for the station");
			pw.println("c the number of stations");
			pw.println(stations.size() + " nsta");
			pw.println("c latitude longitude (deg)");

			stations.stream().sorted().map(Station::getPosition)
					.forEach(p -> pw.println(p.getLatitude() + " " + p.getLongitude()));

			// output
			pw.println("c parameter for the output file");
			stations.stream().sorted().map(Station::getStationName)
					.forEach(n -> pw.println(outputDir + "/" + n + "." + eventID + "PSV.spc"));
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
			String[] structurePart = structure.toSHlines();
			Arrays.stream(structurePart).forEach(pw::println);

			// source
			pw.println("c parameter for the source");
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			pw.println(Arrays.stream(momentTensor).mapToObj(Double::toString).collect(Collectors.joining(" "))
					+ " Moment Tensor (1.e25 dyne cm)");

			// station
			pw.println("c parameter for the station");
			pw.println("c the number of stations");
			pw.println(stations.size() + " nsta");
			pw.println("c latitude longitude (deg)");
			stations.stream().sorted().map(Station::getPosition)
					.forEach(p -> pw.println(p.getLatitude() + " " + p.getLongitude()));

			// output
			pw.println("c parameter for the output file");
			stations.stream().sorted().map(Station::getStationName)
					.forEach(n -> pw.println(outputDir + "/" + n + "." + eventID + "SH.spc"));
			pw.println("end");
		}
	}

}
