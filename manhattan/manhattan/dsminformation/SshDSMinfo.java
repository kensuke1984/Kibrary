package manhattan.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.Station;

/**
 * sshpsv sshsh のinformation file
 * 
 * 
 * 
 * @version 0.0.2 iso用を追加
 * @since 2013/10/17
 * 
 * @version 0.0.3
 * @since 2014/9/5 to Java 8
 * 
 * @version 0.0.4
 * @since 2015/8/8 {@link IOException} {@link Path} base
 * 
 * @version 0.0.4.1
 * @since 2015/8/14
 * 
 * @author kensuke
 * 
 */
class SshDSMinfo extends SyntheticDSMInfo {

	private double[] perturbationR;

	public SshDSMinfo(PolynomialStructure ps, GlobalCMTID id, Set<Station> stations, String outputDir,
			double[] perturbationR, double tlen, int np) {
		super(ps, id, stations, outputDir, tlen, np);
		this.perturbationR = perturbationR;
	}

	@Override
	public void outPSV(Path psvPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(psvPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structure = ps.toPSVlines();
			Arrays.stream(structure).forEach(pw::println);

			// source
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			// double[] mt = momentTensor.getDSMmt();
			Arrays.stream(momentTensor).forEach(mt -> pw.print(mt + " "));
			pw.println("Moment Tensor (1.e25 dyne cm)");
			pw.println("c directory of outputs");
			pw.println(outputDir + "/");
			pw.println("PSV.spc");
			pw.println(stations.size() + " nr");

			stations.forEach(station -> {
				pw.println(station.getStationName() + "." + eventID + ".PARA");
				pw.println(station.getStationName() + "." + eventID + ".PARC");
				pw.println(station.getStationName() + "." + eventID + ".PARF");
				pw.println(station.getStationName() + "." + eventID + ".PARL");
				pw.println(station.getStationName() + "." + eventID + ".PARN");
			});

			stations.forEach(station -> pw.println(station.getPosition()));
			pw.println(perturbationR.length + " nsta");

			Arrays.stream(perturbationR).forEach(pw::println);
			pw.println("end");

		}
	}

	/**
	 * sshpsv i計算用のファイル出力
	 * 
	 * @param psvPath
	 * @param options
	 * @throws IOException
	 */
	void outPSVi(Path psvPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(psvPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structure = ps.toPSVlines();
			Arrays.stream(structure).forEach(pw::println);

			// source
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			// double[] mt = event.getCmt().getDSMmt();
			Arrays.stream(momentTensor).forEach(mt -> pw.print(mt + " "));
			pw.println("Moment Tensor (1.e25 dyne cm)");
			pw.println("c directory of outputs");
			pw.println(outputDir + "/");
			pw.println("PSV.spc");
			pw.println(stations.size() + " nr");
			stations.forEach(station -> pw.println(station.getStationName() + "." + eventID + ".PAR2"));

			stations.forEach(station -> pw.println(station.getPosition()));
			pw.println(perturbationR.length + " nsta");
			Arrays.stream(perturbationR).forEach(pw::println);
			pw.println("end");

		}
	}

	/**
	 * sshsh計算用のファイル出力
	 * 
	 * @param shPath
	 */
	@Override
	public void outSH(Path shPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(shPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structure = ps.toSHlines();
			Arrays.stream(structure).forEach(pw::println);

			// source
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			// double[] mt = event.getCmt().getDSMmt();
			Arrays.stream(momentTensor).forEach(mt -> pw.print(mt + " "));

			pw.println("Moment Tensor (1.e25 dyne cm)");
			pw.println("c directory of outputs");
			pw.println(outputDir + "/");
			pw.println("SH.spc");
			pw.println(stations.size() + " nr");

			stations.forEach(station -> {
				pw.println(station.getStationName() + "." + eventID + ".PARL");
				pw.println(station.getStationName() + "." + eventID + ".PARN");
			});
			stations.forEach(station -> pw.println(station.getPosition()));
			pw.println(perturbationR.length + " nsta");
			Arrays.stream(perturbationR).forEach(pw::println);
			pw.println("end");

		}

	}

	/**
	 * sshshi計算用のファイル出力
	 * 
	 * @param shPath
	 */
	void outSHi(Path shPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(shPath, options))) {
			// header
			String[] header = outputDSMHeader();
			Arrays.stream(header).forEach(pw::println);

			// structure
			String[] structure = ps.toSHlines();
			Arrays.stream(structure).forEach(pw::println);

			// source
			pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
					+ " r0(km), lat, lon (deg)");
			// double[] mt = event.getCmt().getDSMmt();
			Arrays.stream(momentTensor).forEach(mt -> pw.print(mt + " "));
			pw.println("Moment Tensor (1.e25 dyne cm)");
			pw.println("c directory of outputs");
			pw.println(outputDir + "/");
			pw.println("SH.spc");
			pw.println(stations.size() + " nr");

			stations.forEach(station -> pw.println(station.getStationName() + "." + eventID + ".PAR2"));

			stations.forEach(station -> pw.println(station.getPosition()));

			pw.println(perturbationR.length + " nsta");
			Arrays.stream(perturbationR).forEach(pw::println);
			pw.println("end");

		}

	}

}
