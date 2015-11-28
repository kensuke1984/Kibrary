package manhattan.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import manhattan.globalcmt.GlobalCMTData;
import manhattan.template.HorizontalPosition;

/**
 * Information file for computation of forward propagation.
 * 
 * @version 0.0.4.1
 * 
 * @author Kensuke Konishi
 */
class FPinfo extends DSMheader {

	private GlobalCMTData event;
	private HorizontalPosition[] perturbationPoint;
	private double[] perturbationPointR;
	private PolynomialStructure ps;
	private String outputDir;

	/**
	 * @param event
	 * @param outputDir
	 * @param ps
	 * @param tlen
	 *            must be 2^n/10
	 * @param np
	 *            must be 2^n
	 */
	FPinfo(GlobalCMTData event, String outputDir, PolynomialStructure ps, double tlen, int np) {
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

	public void outputSHFP(Path outPath) throws IOException {
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

	public void outputPSVFP(Path outPath) throws IOException {
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
