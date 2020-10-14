package io.github.kensuke1984.kibrary.dsminformation;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Information file for computation of forward propagation.
 * <p>
 * This class is <b>immutable</b>
 *
 * @author Kensuke Konishi
 * @version 0.0.6.2
 * @author anselme add information files for psv/sh fpcat (catalog)
 */
public class FPinfo extends DSMheader {

    private final GlobalCMTData EVENT;
    private final HorizontalPosition[] POSITIONS;
    private final double[] RADII;
    private final PolynomialStructure STRUCTURE;
    private final String OUTPUT;

    /**
     * @param event              source
     * @param outputDir          write folder
     * @param structure          structure
     * @param tlen               [s] must be a power of 2 (2<sup>n</sup>)/10
     * @param np                 must be a power of 2 (2<sup>n</sup>)
     * @param perturbationPointR will be copied
     * @param perturbationPoint  will be copied
     */
    public FPinfo(GlobalCMTData event, String outputDir, PolynomialStructure structure, double tlen, int np,
                  double[] perturbationPointR, HorizontalPosition[] perturbationPoint) {
        super(tlen, np);
        EVENT = event;
        OUTPUT = outputDir;
        STRUCTURE = structure;
        POSITIONS = perturbationPoint.clone();
        RADII = perturbationPointR.clone();
    }

    /**
     * Write an information file for shfp.
     *
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
            String[] structurePart = STRUCTURE.toSHlines();
            Arrays.stream(structurePart).forEach(pw::println);

            // source
            pw.println(EVENT.getCmtLocation().getR() + " " + EVENT.getCmtLocation().getLatitude() + " " +
                    EVENT.getCmtLocation().getLongitude());
            double[] mt = EVENT.getCmt().getDSMmt();
            pw.println(Arrays.stream(mt).mapToObj(Double::toString).collect(Collectors.joining(" ")) +
                    " Moment Tensor (1.e25 dyne cm)");

            // write info
            pw.println("c write directory");
            pw.println(OUTPUT + "/");
            pw.println(EVENT);
            pw.println("c events and stations");

            // horizontal positions for perturbation points
            pw.println(POSITIONS.length + " nsta");
            Arrays.stream(POSITIONS).forEach(pp -> pw.println(pp.getLatitude() + " " + pp.getLongitude()));

            // radii for perturbation points
            pw.println(RADII.length + " nr");
            Arrays.stream(RADII).forEach(pw::println);
            pw.println("end");
        }
    }
    
    /**
     * write the information file for shfpcat (catalog)
     * @param outPath
     * @param thetamin
     * @param thetamax
     * @param dtheta
     * @param options
     * @throws IOException
     * @author anselme
     */
    public void writeSHFPCAT(Path outPath, double thetamin, double thetamax, double dtheta, OpenOption... options) throws IOException {
		// if(true)return;
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structurePart = STRUCTURE.toSHlines();
			Arrays.stream(structurePart).forEach(pw::println);

			// source
			pw.println(EVENT.getCmtLocation().getR() + " 0. 0.");
			double[] mt = EVENT.getCmt().getDSMmt();
			pw.println(Arrays.stream(mt).mapToObj(Double::toString).collect(Collectors.joining(" "))
					+ " Moment Tensor (1.e25 dyne cm)");

			// output info
			pw.println("c output directory");
			pw.println(OUTPUT + "/");
			pw.println(EVENT.toString());
			pw.println("c events and stations");

			// catalogue epicentral distance sampling
			pw.println(thetamin + " " + thetamax + " " + dtheta);

			// radii for perturbation points
//			int nComment = (int) IntStream.range(0, commentPerturbationR.length)
//					.mapToObj(i -> commentPerturbationR[i]).filter(c -> c).count();
//			pw.println(RADII.length - nComment + " nr");
			pw.println(RADII.length + " nr");
			for (int i = 0; i < RADII.length; i++) {
//				if (commentPerturbationR[i])
//					pw.println("c " + RADII[i]);
//				else
				pw.println(RADII[i]);
			}
			pw.println("end");
		}
	}

    /**
     * Write an information file for psvfp
     *
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
            String[] structurePart = STRUCTURE.toPSVlines();
            Arrays.stream(structurePart).forEach(pw::println);

            // source
            pw.println(EVENT.getCmtLocation().getR() + " " + EVENT.getCmtLocation().getLatitude() + " " +
                    EVENT.getCmtLocation().getLongitude());
            double[] mt = EVENT.getCmt().getDSMmt();
            pw.println(Arrays.stream(mt).mapToObj(Double::toString).collect(Collectors.joining(" ")) +
                    " Moment Tensor (1.e25 dyne cm)");

            // write info
            pw.println("c write directory");
            pw.println(OUTPUT + "/");
            pw.println(EVENT);
            pw.println("c events and stations");

            // horizontal positions for perturbation points
            pw.println(POSITIONS.length + " nsta");
            Arrays.stream(POSITIONS).forEach(pp -> pw.println(pp.getLatitude() + " " + pp.getLongitude()));

            // radii for perturbation points
            pw.println(RADII.length + " nr");
            Arrays.stream(RADII).forEach(pw::println);
            pw.println("end");
        }
    }
    
    /**
     * write the information file for psvfpcat (catalog)
     * @param outPath
     * @param thetamin
     * @param thetamax
     * @param dtheta
     * @param options
     * @throws IOException
     */
    public void writePSVFPCAT(Path outPath, double thetamin, double thetamax, double dtheta, OpenOption... options) throws IOException {
		// if(true)return;
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structurePart = STRUCTURE.toPSVlines();
			Arrays.stream(structurePart).forEach(pw::println);

			// source
			pw.println(EVENT.getCmtLocation().getR() + " " + EVENT.getCmtLocation().getLatitude() + " "
					+ EVENT.getCmtLocation().getLongitude());
			double[] mt = EVENT.getCmt().getDSMmt();
			pw.println(Arrays.stream(mt).mapToObj(Double::toString).collect(Collectors.joining(" "))
					+ " Moment Tensor (1.e25 dyne cm)");

			// output info
			pw.println("c output directory");
			pw.println(OUTPUT + "/");
			pw.println(EVENT.toString());
			pw.println("c events and stations");
			
			// catalogue epicentral distance sampling
			pw.println(thetamin + " " + thetamax + " " + dtheta);

			// radii for perturbation points
//			int nComment = (int) IntStream.range(0, commentPerturbationR.length)
//					.mapToObj(i -> commentPerturbationR[i]).filter(c -> c).count();
//			pw.println(RADII.length - nComment + " nr");
			pw.println(RADII.length + " nr");
			for (int i = 0; i < RADII.length; i++) {
//				if (commentPerturbationR[i])
//					pw.println("c " + RADII[i]);
//				else
					pw.println(RADII[i]);
			}
			pw.println("end");
		}
	}

    /**
     * @return name of the write folder
     */
    public String getOutputDir() {
        return OUTPUT;
    }

    /**
     * @return radii for the perturbation points
     */
    public double[] getPerturbationPointDepth() {
        return RADII.clone();
    }

    /**
     * @return structure to be used
     */
    public PolynomialStructure getStructure() {
        return STRUCTURE;
    }
    
}
