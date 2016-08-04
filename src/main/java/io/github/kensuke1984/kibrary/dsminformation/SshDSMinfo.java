package io.github.kensuke1984.kibrary.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

/**
 * Information file for SSHPSV and SSHSH
 * 
 * <p>
 * This class is <b>IMMUTABLE</b>
 * 
 * 
 * @version 0.0.5.1
 * 
 * @author kensuke
 * 
 */
public class SshDSMinfo extends SyntheticDSMInfo {

	private double[] perturbationR;

	public SshDSMinfo(PolynomialStructure structure, GlobalCMTID id, Set<Station> stations, String outputDir,
			double[] perturbationR, double tlen, int np) {
		super(structure, id, stations, outputDir, tlen, np);
		this.perturbationR = perturbationR;
	}

	/**
	 * sshpsv(TI)計算用のファイル出力
	 * 
	 * @param psvPath
	 *            output path
	 * @param options
	 *            options for writing
	 * @throws IOException
	 *             if any
	 */
	public void writeTIPSV(Path psvPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(psvPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			Arrays.stream(structure.toPSVlines()).forEach(pw::println);

			// source
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			pw.println(Arrays.stream(momentTensor).mapToObj(Double::toString).collect(Collectors.joining(" "))
					+ " Moment Tensor (1.e25 dyne cm)");
			pw.println("c directory of outputs");
			pw.println(outputDir + "/");
			pw.println("PSV.spc");
			pw.println(stations.size() + " nsta");
			stations.stream().sorted().map(Station::getStationName).forEach(n -> {
				pw.println(n + "." + eventID + ".PARA");
				pw.println(n + "." + eventID + ".PARC");
				pw.println(n + "." + eventID + ".PARF");
				pw.println(n + "." + eventID + ".PARL");
				pw.println(n + "." + eventID + ".PARN");
			});

			stations.stream().sorted().map(Station::getPosition)
					.forEach(p -> pw.println(p.getLatitude() + " " + p.getLongitude()));
			pw.println(perturbationR.length + " nr");
			Arrays.stream(perturbationR).forEach(pw::println);
			pw.println("end");

		}
	}

	/**
	 * sshpsvi(isotropic)計算用のファイル出力
	 * 
	 * @param psvPath
	 *            output path
	 * @param options
	 *            options for writing
	 * @throws IOException
	 *             if any
	 */
	public void writeISOPSV(Path psvPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(psvPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			Arrays.stream(structure.toPSVlines()).forEach(pw::println);

			// source
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			pw.println(Arrays.stream(momentTensor).mapToObj(Double::toString).collect(Collectors.joining(" "))
					+ " Moment Tensor (1.e25 dyne cm)");
			pw.println("c directory of outputs");
			pw.println(outputDir + "/");
			pw.println("PSV.spc");

			pw.println(stations.size() + " nsta");
			stations.stream().sorted().map(Station::getStationName)
					.forEach(n -> pw.println(n + "." + eventID + ".PAR2"));
			stations.stream().sorted().map(Station::getPosition)
					.forEach(p -> pw.println(p.getLatitude() + " " + p.getLongitude()));

			pw.println(perturbationR.length + " nr");
			Arrays.stream(perturbationR).forEach(pw::println);
			pw.println("end");

		}
	}

	/**
	 * sshsh(TI)計算用のファイル出力
	 * 
	 * @param shPath
	 *            output path
	 * @param options
	 *            options for writing
	 * @throws IOException
	 *             if any
	 */
	public void writeTISH(Path shPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(shPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			Arrays.stream(structure.toSHlines()).forEach(pw::println);

			// source
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			pw.println(Arrays.stream(momentTensor).mapToObj(Double::toString).collect(Collectors.joining(" "))
					+ " Moment Tensor (1.e25 dyne cm)");
			pw.println("c directory of outputs");
			pw.println(outputDir + "/");
			pw.println("SH.spc");
			
			pw.println(stations.size() + " nsta");
			stations.stream().sorted().map(Station::getStationName).forEach(n -> {
				pw.println(n + "." + eventID + ".PARL");
				pw.println(n + "." + eventID + ".PARN");
			});
			
			stations.stream().sorted().map(Station::getPosition)
					.forEach(p -> pw.println(p.getLatitude() + " " + p.getLongitude()));
			pw.println(perturbationR.length + " nr");
			Arrays.stream(perturbationR).forEach(pw::println);
			pw.println("end");
		}

	}

	/**
	 * sshshi(isotropic)計算用のファイル出力
	 * 
	 * @param shPath
	 *            output path
	 * @param options
	 *            for writing
	 * @throws IOException
	 *             if any
	 */
	public void writeISOSH(Path shPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(shPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			Arrays.stream(structure.toSHlines()).forEach(pw::println);

			// source
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			pw.println(Arrays.stream(momentTensor).mapToObj(Double::toString).collect(Collectors.joining(" "))
					+ " Moment Tensor (1.e25 dyne cm)");
			pw.println("c directory of outputs");
			pw.println(outputDir + "/");
			pw.println("SH.spc");
			pw.println(stations.size() + " nsta");
			stations.stream().sorted().map(Station::getStationName)
					.forEach(n -> pw.println(n + "." + eventID + ".PAR2"));
			stations.stream().sorted().map(Station::getPosition)
					.forEach(p -> pw.println(p.getLatitude() + " " + p.getLongitude()));

			pw.println(perturbationR.length + " nr");
			Arrays.stream(perturbationR).forEach(pw::println);
			pw.println("end");
		}

	}

}
