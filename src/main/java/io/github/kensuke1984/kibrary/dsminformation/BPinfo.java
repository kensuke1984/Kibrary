package io.github.kensuke1984.kibrary.dsminformation;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Information file for computation of back propagation.
 * <p>
 * This class is <b>immutable</b>
 *
 * @author Kensuke Konishi
 * @version 0.0.6
 * @author anselme add information files for psv/sh bpcat (catalog)
 */
public class BPinfo extends DSMheader {

    private final String OUTPUT;

    private final double[] RADII;
    private final HorizontalPosition[] POSITIONS;
    // private double sourceR;
    private final Station STATION;
    private final PolynomialStructure STRUCTURE;

    /**
     * @param station              Information of station
     * @param outputDir            the name of the write folder
     * @param structure            velocity structure
     * @param tlen                 must be a power of 2 / 10 (2<sup>n</sup>)/10
     * @param np                   must be a power of 2 (2<sup>n</sup>)
     * @param perturbationPointR   will be copied
     * @param perturbationPosition will be copied
     */
    public BPinfo(Station station, String outputDir, PolynomialStructure structure, double tlen, int np,
                  double[] perturbationPointR, HorizontalPosition[] perturbationPosition) {
        super(tlen, np);
        STATION = station;
        OUTPUT = outputDir;
        STRUCTURE = structure;
        RADII = perturbationPointR.clone();
        POSITIONS = perturbationPosition.clone();
    }
    
	public BPinfo(String outputDir, PolynomialStructure structure, double tlen, int np,
			double[] perturbationPointR, HorizontalPosition[] perturbationPosition) {
		super(tlen, np);
		STATION = null;
		OUTPUT = outputDir;
		STRUCTURE = structure;
		RADII = perturbationPointR.clone();
		POSITIONS = perturbationPosition.clone();
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

    /**
     * Write an information file for psvbp
     *
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
            Arrays.stream(STRUCTURE.toPSVlines()).forEach(pw::println);

            // source
            HorizontalPosition stationPosition = STATION.getPosition();
            pw.println("0 " + // BPINFOには震源深さいらない
                    stationPosition.getLatitude() + " " + stationPosition.getLongitude());

            // write info
            pw.println("c write directory");
            pw.println(OUTPUT + "/");
            pw.println(STATION.getName());
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
     * write the information file for psvbpcat (psvbp catalog)
     * @param outPath
     * @param thetamin
     * @param thetamax
     * @param dtheta
     * @param options
     * @throws IOException
     * @author anselme
     */
    public void writePSVBPCat(Path outPath, double thetamin, double thetamax, double dtheta, OpenOption... options) 
    		throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			Arrays.stream(STRUCTURE.toPSVlines()).forEach(pw::println);

			// source
			pw.println("0. 0. 0.");

			// output info
			pw.println("c output directory");
			pw.println(OUTPUT + "/");
			pw.println("340A_TA");
			pw.println("c events and stations");

			// catalogue epicentral distance sampling
			pw.println(thetamin + " " + thetamax + " " + dtheta);

			// radii for perturbation points
			pw.println(RADII.length + " nr");
			Arrays.stream(RADII).forEach(pw::println);
			pw.println("end");
		}
	}

    /**
     * Write an information file for shbp
     *
     * @param outPath Path for the file
     * @param options for opening the file
     * @throws IOException if an I/O error happens
     */
    public void writeSHBP(Path outPath, OpenOption... options) throws IOException {

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
            // header
            String[] header = outputDSMHeader();
            Arrays.stream(header).forEach(pw::println);

            // structure
            Arrays.stream(STRUCTURE.toSHlines()).forEach(pw::println);

            HorizontalPosition stationPosition = STATION.getPosition();
            pw.println("0 " + // BPINFOには震源深さいらない
                    stationPosition.getLatitude() + " " + stationPosition.getLongitude());

            // write info
            pw.println("c write directory");
            pw.println(OUTPUT + "/");
            pw.println(STATION.getName());
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
     * write the information file for shbpcat (shbp catalog)
     * @param outPath
     * @param thetamin
     * @param thetamax
     * @param dtheta
     * @param options
     * @throws IOException
     * @author anselme
     */
    public void writeSHBPCat(Path outPath, double thetamin, double thetamax, double dtheta, OpenOption... options) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			Arrays.stream(STRUCTURE.toSHlines()).forEach(pw::println);

			// source
			pw.println("0. 0. 0.");

			// output info
			pw.println("c output directory");
			pw.println(OUTPUT + "/");
			pw.println("340A_TA");
			pw.println("c events and stations");

			// catalogue epicentral distance sampling
			pw.println(thetamin + " " + thetamax + " " + dtheta);

			// radii for perturbation points
			pw.println(RADII.length + " nr");
			Arrays.stream(RADII).forEach(pw::println);
			pw.println("end");
		}
	}

}
