package io.github.kensuke1984.kibrary.dsminformation;

import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Information file for SSHPSV and SSHSH
 * <p>
 * This class is <b>IMMUTABLE</b>
 *
 * @author Kensuke Konishi
 * @version 0.0.6.1
 * @author anselme add system to comment out perturbations that are too close to the source and takes too long to compute
 * add PAR0 (partial for density)
 */
public class SshDSMinfo extends SyntheticDSMInfo {

    private final double[] RADII;
    private boolean[] commentPerturbationR;

    public SshDSMinfo(PolynomialStructure structure, GlobalCMTData event, Set<Station> stations, String outputDir,
                      double[] perturbationR, double tlen, int np) {
        super(structure, event, stations, outputDir, tlen, np);
        RADII = perturbationR.clone();
        
        commentPerturbationR = new boolean[perturbationR.length];
		double eventR = event.getCmtLocation().getR();
		for (int i = 0; i < perturbationR.length; i++) {
			commentPerturbationR[i] = false;
			if (Math.abs(eventR - perturbationR[i]) < 10.)
				commentPerturbationR[i] = true;
		}
    }

    /**
     * Creates a file for sshpsv(TI)
     *
     * @param psvPath write path
     * @param options options for writing
     * @throws IOException if any
     */
    public void writeTIPSV(Path psvPath, OpenOption... options) throws IOException {
    	try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(psvPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			Arrays.stream(STRUCTURE.toPSVlines()).forEach(pw::println);

			Location eventLocation = EVENT.getCmtLocation();

			// source
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			pw.println(
					Arrays.stream(EVENT.getCmt().getDSMmt()).mapToObj(Double::toString).collect(Collectors.joining(" "))
							+ " Moment Tensor (1.e25 dyne cm)");
			pw.println("c directory of outputs");
			pw.println(OUTPUT + "/");
			pw.println("PSV.spc");
			pw.println(STATIONS.size() + " nsta");
			STATIONS.stream().sorted().map(Station::getStringID).forEach(n -> {
				pw.println(n + "." + EVENT + ".PAR0");
				pw.println(n + "." + EVENT + ".PARA");
				pw.println(n + "." + EVENT + ".PARC");
				pw.println(n + "." + EVENT + ".PARF");
				pw.println(n + "." + EVENT + ".PARL");
				pw.println(n + "." + EVENT + ".PARN");
			});

			STATIONS.stream().sorted().map(Station::getPosition)
					.forEach(p -> pw.println(p.getLatitude() + " " + p.getLongitude()));
			
			int nComment = (int) IntStream.range(0, commentPerturbationR.length)
				.mapToObj(i -> commentPerturbationR[i]).filter(c -> c).count();
			pw.println(RADII.length - nComment + " nr");
			for (int i = 0; i < RADII.length; i++) {
				if (commentPerturbationR[i])
					pw.println("c " + RADII[i]);
				else
					pw.println(RADII[i]);
			}
			pw.println("end");

		}
    }

    /**
     * Creates a file for sshpsvi(isotropic)
     *
     * @param psvPath write path
     * @param options options for writing
     * @throws IOException if any
     */
    public void writeISOPSV(Path psvPath, OpenOption... options) throws IOException {
    	try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(psvPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			Arrays.stream(STRUCTURE.toPSVlines()).forEach(pw::println);
			Location eventLocation = EVENT.getCmtLocation();
			// source
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			pw.println(
					Arrays.stream(EVENT.getCmt().getDSMmt()).mapToObj(Double::toString).collect(Collectors.joining(" "))
							+ " Moment Tensor (1.e25 dyne cm)");
			pw.println("c directory of outputs");
			pw.println(OUTPUT + "/");
			pw.println("PSV.spc");

			pw.println(STATIONS.size() + " nsta");
			STATIONS.stream().sorted().map(Station::getStringID).forEach(n -> {
				pw.println(n + "." + EVENT + ".PAR0");
				pw.println(n + "." + EVENT + ".PAR1");
				pw.println(n + "." + EVENT + ".PAR2");
			});
			STATIONS.stream().sorted().map(Station::getPosition)
					.forEach(p -> pw.println(p.getLatitude() + " " + p.getLongitude()));

			int nComment = (int) IntStream.range(0, commentPerturbationR.length)
					.mapToObj(i -> commentPerturbationR[i]).filter(c -> c).count();
			pw.println(RADII.length - nComment + " nr");
			for (int i = 0; i < RADII.length; i++) {
				if (commentPerturbationR[i])
					pw.println("c " + RADII[i]);
				else
					pw.println(RADII[i]);
			}
			pw.println("end");

		}
    }

    /**
     * Creates a file for sshsh(TI)
     *
     * @param shPath  write path
     * @param options options for writing
     * @throws IOException if any
     */
    public void writeTISH(Path shPath, OpenOption... options) throws IOException {
    	try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(shPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			Arrays.stream(STRUCTURE.toSHlines()).forEach(pw::println);
			Location eventLocation = EVENT.getCmtLocation();
			// source
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			pw.println(
					Arrays.stream(EVENT.getCmt().getDSMmt()).mapToObj(Double::toString).collect(Collectors.joining(" "))
							+ " Moment Tensor (1.e25 dyne cm)");
			pw.println("c directory of outputs");
			pw.println(OUTPUT + "/");
			pw.println("SH.spc");

			pw.println(STATIONS.size() + " nsta");
			STATIONS.stream().sorted().map(Station::getStringID).forEach(n -> {
				pw.println(n + "." + EVENT + ".PAR0");
				pw.println(n + "." + EVENT + ".PARL");
				pw.println(n + "." + EVENT + ".PARN");
			});

			STATIONS.stream().sorted().map(Station::getPosition)
					.forEach(p -> pw.println(p.getLatitude() + " " + p.getLongitude()));
			int nComment = (int) IntStream.range(0, commentPerturbationR.length)
					.mapToObj(i -> commentPerturbationR[i]).filter(c -> c).count();
			pw.println(RADII.length - nComment + " nr");
			for (int i = 0; i < RADII.length; i++) {
				if (commentPerturbationR[i])
					pw.println("c " + RADII[i]);
				else
					pw.println(RADII[i]);
			}
			pw.println("end");
		}
    }

    /**
     * Creates a file for sshshi(isotropic)
     *
     * @param shPath  write path
     * @param options for writing
     * @throws IOException if any
     */
    public void writeISOSH(Path shPath, OpenOption... options) throws IOException {
    	try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(shPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			Arrays.stream(STRUCTURE.toSHlines()).forEach(pw::println);
			Location eventLocation = EVENT.getCmtLocation();
			// source
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			pw.println(
					Arrays.stream(EVENT.getCmt().getDSMmt()).mapToObj(Double::toString).collect(Collectors.joining(" "))
							+ " Moment Tensor (1.e25 dyne cm)");
			pw.println("c directory of outputs");
			pw.println(OUTPUT + "/");
			pw.println("SH.spc");
			pw.println(STATIONS.size() + " nsta");
			STATIONS.stream().sorted().map(Station::getStringID).forEach(n -> {
				pw.println(n + "." + EVENT + ".PAR0");
				pw.println(n + "." + EVENT + ".PAR2");
			});
			STATIONS.stream().sorted().map(Station::getPosition)
					.forEach(p -> pw.println(p.getLatitude() + " " + p.getLongitude()));

			int nComment = (int) IntStream.range(0, commentPerturbationR.length)
					.mapToObj(i -> commentPerturbationR[i]).filter(c -> c).count();
			pw.println(RADII.length - nComment + " nr");
			for (int i = 0; i < RADII.length; i++) {
				if (commentPerturbationR[i])
					pw.println("c " + RADII[i]);
				else
					pw.println(RADII[i]);
			}
			pw.println("end");
		}
    }

    /**
     * @return [km] radii for the perturbation points
     */
    public double[] getPerturbationPointDepth() {
        return RADII.clone();
    }
}
