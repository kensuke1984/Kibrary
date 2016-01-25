package io.github.kensuke1984.kibrary.external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>
 * Utility class to handle with taup_time in TauP package
 * </p>
 * 
 * taup_time must be in PATH and it must run correctly.<br>
 * All the standard output and errors will go to the bit bucket
 * 
 * PREM is used for travel times.
 * 
 * @version 0.3.1
 * @see <a href=http://www.seis.sc.edu/taup/>TauP</a>
 * 
 * 
 * @author kensuke
 * 
 */
public final class TauPTimeReader {

	private TauPTimeReader() {
	}

	private static final String path = "taup_time";

	static {
		initialize();
	}

	private static void initialize() {
		if (!ExternalProcess.isInPath(path))
			throw new RuntimeException(path + " is not in PATH");
	}

	/**
	 * @param eventR
	 *            radius (km) !!not depth from the surface!!
	 * @param epicentralDistance
	 *            [deg] targetDistance
	 * @param phases
	 *            to look for
	 * @return travel times for the phase if theres a multiplication, all values
	 *         will be returned
	 */
	public static Set<TauPPhase> getTauPPhase(double eventR, double epicentralDistance, TauPPhaseName... phases) {
		Set<TauPPhaseName> phaseSet = new HashSet<>(Arrays.asList(phases));
		return getTauPPhase(eventR, epicentralDistance, phaseSet);
	}

	/**
	 * @param eventR
	 *            radius of seismic source !!not depth from the surface!!
	 * @param epicentralDistance
	 *            [deg] target epicentral distance
	 * @param phaseSet
	 *            set of seismic phase.
	 * @return {@link Set} of TauPPhases.
	 */
	public static Set<TauPPhase> getTauPPhase(double eventR, double epicentralDistance, Set<TauPPhaseName> phaseSet) {
		return toPhase(operateTauPTime(eventR, epicentralDistance, phaseSet));
	}

	/**
	 * TauPの結果の出力を読み込む
	 * 
	 * @param eventR
	 * @param epicentralDistance
	 * @param phase
	 * @return result lines
	 */
	private static List<String> operateTauPTime(double eventR, double epicentralDistance, Set<TauPPhaseName> phase) {
		String[] cmd = makeCMD(eventR, epicentralDistance, phase);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectError(ExternalProcess.bitBucket);
		try {
			Process p = pb.start();
			List<String> outLines = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line = null;
				while ((line = br.readLine()) != null)
					outLines.add(line);
			}
			return outLines;
		} catch (Exception e) {
			System.out.println("Error occured");
			System.out.println("could not find the time");
			e.printStackTrace();
			return null;
		}
	}

	private static Set<TauPPhase> toPhase(List<String> lines) {
		if (lines == null || lines.size() <= 6)
			return Collections.emptySet();
		return IntStream.range(5, lines.size() - 1).mapToObj(lines::get).map(TauPTimeReader::toPhase)
				.collect(Collectors.toSet());
	}

	/**
	 * Distance Depth Phase Travel Ray Param Takeoff Incident Purist distance
	 * Purist name の順ではいっている文
	 * 
	 * @param line
	 * @return
	 */
	private static TauPPhase toPhase(String line) {
		String[] parts = line.trim().split("\\s+");
		// System.out.println(line+"hi");
		double distance = Double.parseDouble(parts[0]);
		double depth = Double.parseDouble(parts[1]);
		TauPPhaseName phaseName = TauPPhaseName.valueOf(parts[2]);
		double travelTime = Double.parseDouble(parts[3]);
		double rayParameter = Double.parseDouble(parts[4]);
		double takeoff = Double.parseDouble(parts[5]);
		double incident = Double.parseDouble(parts[6]);
		double puristDistance = Double.parseDouble(parts[7]);
		// parts[8] = "="
		String puristName = parts[9];
		// System.exit(0);
		return new TauPPhase(distance, depth, phaseName, travelTime, rayParameter, takeoff, incident, puristDistance,
				puristName);
	}

	/**
	 * TauPに投げる命令文を作る
	 * 
	 * @param eventR
	 *            [km] RADIUS (NOT depth from the surface)
	 * @param epicentralDistance
	 *            [deg]
	 * @param phase
	 * @return
	 */
	private static String[] makeCMD(double eventR, double epicentralDistance, Set<TauPPhaseName> phases) {
		Object[] phaseO = phases.toArray();
		StringBuffer phase = new StringBuffer(phaseO[0].toString());
		for (int i = 1; i < phaseO.length; i++)
			phase.append("," + phaseO[i].toString());
		String cmd = path + " -h " + (6371 - eventR) + " -deg " + epicentralDistance + " -model prem -ph " + phase;
		return cmd.split("\\s+");
	}

}
