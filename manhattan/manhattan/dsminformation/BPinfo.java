package manhattan.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;

import manhattan.template.HorizontalPosition;
import manhattan.template.Station;

/**
 * Information file for computation of back propagation.
 * 
 * 
 * @version 0.0.4.2
 * 
 * @author Kensuke
 * 
 * 
 */
public class BPinfo extends DSMheader {

	private String outputDir;

	private double[] perturbationPointR;
	private HorizontalPosition[] perturbationPoint;
	// private double sourceR;
	private Station station;
	private PolynomialStructure ps;

	public String getOutputDir() {
		return outputDir;
	}

	public double[] getPerturbationPointDepth() {
		return perturbationPointR;
	}

	public PolynomialStructure getPs() {
		return ps;
	}

	/**
	 * @param station
	 *            stationの位置
	 * @param outputDir
	 *            書き込むフォルダ （相対パス）
	 * @param ps
	 * @param tlen
	 *            must be 2^n/10
	 * @param np
	 *            must be 2^n
	 */
	public BPinfo(Station station, String outputDir, PolynomialStructure ps, double tlen, int np) {
		super(tlen, np);
		this.station = station;
		this.outputDir = outputDir;
		this.ps = ps;
		// this.sourceR=sourceR;
	}

	public void setPerturbationPoint(HorizontalPosition[] perturbationPoint) {
		this.perturbationPoint = perturbationPoint;
	}

	public void setPerturbationPointR(double[] perturbationPointR) {
		this.perturbationPointR = perturbationPointR;
	}

	/**
	 * Write an information file for psvbp
	 * @param outPath Path for the file
	 * @param options for opening the file
	 * @throws IOException If an I/O error happens
	 */
	public void writePSVBP(Path outPath, OpenOption... options) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structure = ps.toPSVlines();
			Arrays.stream(structure).forEach(pw::println);

			// source
			HorizontalPosition stationPosition = station.getPosition();
			pw.println("0 " + // BPINFOには震源深さいらない
					stationPosition.getLatitude() + " " + stationPosition.getLongitude());

			// output info
			pw.println("c output directory");
			pw.println(outputDir + "/");
			pw.println(station.getStationName());
			pw.println("c events and stations");

			// nr
			int nr = perturbationPoint.length;
			pw.println(nr + " nr");
			Arrays.stream(perturbationPoint).forEach(pp -> pw.println(pp.getLatitude() + " " + pp.getLongitude()));
			// nsta
			int nsta = perturbationPointR.length;
			pw.println(nsta + " nsta");
			Arrays.stream(perturbationPointR).forEach(pw::println);
			pw.println("end");
		}
	}

	/**
	 * Write an information file for shbp
	 * 
	 * @param outPath
	 *            Path for the file
	 * @param options
	 *            for opening the file
	 * @throws IOException
	 *             if an I/O error happens
	 */
	public void writeSHBP(Path outPath, OpenOption... options) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structure = ps.toSHlines();
			Arrays.stream(structure).forEach(pw::println);

			HorizontalPosition stationPosition = station.getPosition();
			pw.println("0 " + // BPINFOには震源深さいらない
					stationPosition.getLatitude() + " " + stationPosition.getLongitude());

			// output info
			pw.println("c output directory");
			pw.println(outputDir + "/");
			pw.println(station.getStationName());
			pw.println("c events and stations");

			// nr
			int nr = perturbationPoint.length;
			pw.println(nr + " nr");
			Arrays.stream(perturbationPoint).forEach(pp -> pw.println(pp.getLatitude() + " " + pp.getLongitude()));

			// nsta
			int nsta = perturbationPointR.length;
			pw.println(nsta + " nsta");
			Arrays.stream(perturbationPointR).forEach(pw::println);
			pw.println("end");
		}
	}


	public void setPs(PolynomialStructure ps) {
		this.ps = ps;
	}

}
