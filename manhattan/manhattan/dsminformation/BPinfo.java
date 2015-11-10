package manhattan.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import manhattan.template.HorizontalPosition;

/**
 * 
 * @version 0.0.1 back propagate計算用の情報
 * @version 0.0.2
 * @since 2013/9/25
 * 
 * @version 0.0.3
 * @since 2014/9/5 to Java 8
 * 
 * @version 0.0.4
 * @since 2015/8/8 {@link Path} base
 * 
 * @version 0.0.4.1
 * @since 2015/8/21
 * 
 * @author Kensuke
 * 
 * 
 */
class BPinfo extends DSMheader {

	private String outputDir;
	private String staName;

	private double[] perturbationPointR;
	private HorizontalPosition[] perturbationPoint;
	// private double sourceR;
	private HorizontalPosition station;
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
	BPinfo(HorizontalPosition station, String outputDir, PolynomialStructure ps, double tlen, int np) {
		super(tlen, np);
		this.station = station;
		this.outputDir = outputDir;
		this.ps = ps;
		// this.sourceR=sourceR;
	}

	public String getStaName() {
		return staName;
	}

	public void setPerturbationPoint(HorizontalPosition[] perturbationPoint) {
		this.perturbationPoint = perturbationPoint;
	}

	public void setPerturbationPointR(double[] perturbationPointR) {
		this.perturbationPointR = perturbationPointR;
	}

	public void outputPSVBP(Path outPath) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structure = ps.toPSVlines();
			Arrays.stream(structure).forEach(pw::println);

			// source
			pw.println("0 " + // BPINFOには震源深さいらない
					station.getLatitude() + " " + station.getLongitude());

			// output info
			pw.println("c output directory");
			pw.println(outputDir + "/");
			pw.println(staName);
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

	public void outputSHBP(Path outPath) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structure = ps.toSHlines();
			Arrays.stream(structure).forEach(pw::println);

			pw.println("0 " + // BPINFOには震源深さいらない
					station.getLatitude() + " " + station.getLongitude());

			// output info
			pw.println("c output directory");
			pw.println(outputDir + "/");
			pw.println(staName);
			pw.println("c events and stations");

			// nr
			int nr = perturbationPoint.length;
			pw.println(nr + " nr");
			Arrays.stream(perturbationPoint).forEach(pp -> pw.println(pp.getLatitude() + " " + pp.getLongitude()));

			// nsta
			// nsta
			int nsta = perturbationPointR.length;
			pw.println(nsta + " nsta");
			Arrays.stream(perturbationPointR).forEach(pw::println);
			pw.println("end");
		}
	}

	public void setPerturbationPointDepth(double[] perturbationPointDepth) {
		this.perturbationPointR = perturbationPointDepth;
	}

	public void setPs(PolynomialStructure ps) {
		this.ps = ps;
	}

	public void setStaName(String staName) {
		this.staName = staName;
	}
}
