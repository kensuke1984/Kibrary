package io.github.kensuke1984.kibrary.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;

/**
 * Information file for computation of back propagation.
 * 
 * 
 * @version 0.0.4.3
 * 
 * @author Kensuke Konishi
 * 
 * 
 */
public class BPinfo extends DSMheader {

	private String outputDir;

	private double[] perturbationPointR;
	private HorizontalPosition[] perturbationPoint;
	// private double sourceR;
	private Station station;
	private PolynomialStructure structure;

	public String getOutputDir() {
		return outputDir;
	}

	public double[] getPerturbationPointDepth() {
		return perturbationPointR;
	}

	public PolynomialStructure getPs() {
		return structure;
	}

	/**
	 * @param station
	 *            stationの位置
	 * @param outputDir
	 *            書き込むフォルダ （相対パス）
	 * @param structure
	 * @param tlen
	 *            must be a power of 2 /10
	 * @param np
	 *            must be a power of 2
	 */
	public BPinfo(Station station, String outputDir, PolynomialStructure structure, double tlen, int np) {
		super(tlen, np);
		this.station = station;
		this.outputDir = outputDir;
		this.structure = structure;
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
			Arrays.stream(structure.toPSVlines()).forEach(pw::println);

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
			Arrays.stream(structure.toSHlines()).forEach(pw::println);

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


	public void setStructure(PolynomialStructure structure) {
		this.structure = structure;
	}

}
