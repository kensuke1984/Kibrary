package io.github.kensuke1984.kibrary.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;

/**
 * Information file for computation of forward propagation.
 * 
 * @version 0.0.4.2
 * 
 * @author Kensuke Konishi
 */
public class FPinfo extends DSMheader {

	private GlobalCMTData event;
	private HorizontalPosition[] perturbationPoint;
	private double[] perturbationPointR;
	private PolynomialStructure ps;
	private String outputDir;

	/**
	 * @param event
	 *            source
	 * @param outputDir
	 *            output folder
	 * @param ps
	 *            structure
	 * @param tlen
	 *            must be a power of 2 (2<sup>n</sup>)/10
	 * @param np
	 *            must be a power of 2 (2<sup>n</sup>)
	 */
	public FPinfo(GlobalCMTData event, String outputDir, PolynomialStructure ps, double tlen, int np) {
		super(tlen, np);
		this.event = event;
		this.outputDir = outputDir;
		this.ps = ps;
	}

	public void setPerturbationPoint(HorizontalPosition[] perturbationPoint) {
		this.perturbationPoint = perturbationPoint;
	}

	public void setPerturbationPointR(double[] perturbationPointR) {
		this.perturbationPointR = perturbationPointR;
	}

	/**
	 * Write an information file for shfp.
	 * @param outPath Path for the file
	 * @param options for opening the file
	 * @throws IOException If an I/O error happens
	 */
	public void writeSHFP(Path outPath, OpenOption... options) throws IOException {
		// if(true)return;
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structure = ps.toSHlines();
			Arrays.stream(structure).forEach(pw::println);

			// source
			pw.println(event.getCmtLocation().getR() + " " + event.getCmtLocation().getLatitude() + " "
					+ event.getCmtLocation().getLongitude());
			double[] mt = event.getCmt().getDSMmt();
			Arrays.stream(mt).forEach(d -> pw.print(d + " "));
			pw.println();

			// output info
			pw.println("c output directory");
			pw.println(outputDir + "/");
			pw.println(event.toString());
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
	 * Write an information file for psvfp
	 * @param outPath Path for the file
	 * @param options for opening the file
	 * @throws IOException If an I/O error happens
	 */
	public void writePSVFP(Path outPath, OpenOption... options) throws IOException {
		// if(true)return;
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structure = ps.toPSVlines();
			Arrays.stream(structure).forEach(pw::println);

			// source
			pw.println(event.getCmtLocation().getR() + " " + event.getCmtLocation().getLatitude() + " "
					+ event.getCmtLocation().getLongitude());
			double[] mt = event.getCmt().getDSMmt();
			Arrays.stream(mt).forEach(d -> pw.print(d + " "));
			pw.println();

			// output info
			pw.println("c output directory");
			pw.println(outputDir + "/");
			pw.println(event.toString());
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

}
