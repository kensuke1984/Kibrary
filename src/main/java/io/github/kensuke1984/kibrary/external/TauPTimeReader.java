package io.github.kensuke1984.kibrary.external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.sc.seis.TauP.TauModel;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Earth;

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
 * @version 0.3.2.1
 * @see <a href=http://www.seis.sc.edu/taup/>TauP</a>
 * 
 * 
 * TODO phase
 * 
 * @author Kensuke Konishi
 * 
 */
public final class TauPTimeReader {

	private TauPTimeReader() {
	}

	private static final String path = "taup_time";

	static {
		try {
			initialize();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	private static void initialize() throws RuntimeException {
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
	 * @return travel times for the phase if there is multiplication, all values
	 *         will be returned
	 */
	public static Set<TauPPhase> getTauPPhase(double eventR, double epicentralDistance, Phase... phases) {
		Set<Phase> phaseSet = new HashSet<>(Arrays.asList(phases));
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
	public static Set<TauPPhase> getTauPPhase(double eventR, double epicentralDistance, Set<Phase> phaseSet, String model) {
		return toPhase(operateTauPTime(eventR, epicentralDistance, phaseSet, model));
	}
	
	public static List<TauPPhase> getTauPPhaseList(double eventR, double epicentralDistance, Set<Phase> phaseSet, String model) {
		return toPhaseList(operateTauPTime(eventR, epicentralDistance, phaseSet, model));
	}
	
	public static Set<TauPPhase> getTauPPhase(double eventR, double epicentralDistance, Set<Phase> phaseSet) {
		return toPhase(operateTauPTime(eventR, epicentralDistance, phaseSet, "prem"));
	}

	/**
	 * TauPの結果の出力を読み込む
	 * 
	 * @param eventR
	 * @param epicentralDistance
	 * @param phase
	 * @return result lines
	 */
	private static List<String> operateTauPTime(double eventR, double epicentralDistance, Set<Phase> phase, String model) {
		String[] cmd = makeCMD(eventR, epicentralDistance, phase, model);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectError(ExternalProcess.bitBucket);
//		pb.redirectError(Paths.get("error_taup.log").toFile());
		try {
			Process p = pb.start();
			List<String> outLines = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = br.readLine()) != null) {
					outLines.add(line);
				}
			}
			return outLines;
		} catch (Exception e) {
			System.out.println("Error occured");
			System.out.println("could not find the time");
			e.printStackTrace();
			return null;
		}
	}
	
	public static double extrapolate_sS(double eventR, double epicentralDistance, String model) {
		double[] depths = new double[] {50., 60., 75., 200., 260., 310., 350.};
		double[] distances = new double[] {14., 16., 17., 18., 19., 20.};
		if (model.toLowerCase().equals("ak135")) {
			depths = new double[] {80., 130, 200., 270., 310., 350.};
			distances = new double[] {16., 17., 18, 19, 20.};
		}
		double traveltime = -1;
		
		double depth = Earth.EARTH_RADIUS - eventR;
		int iDepth = -1;
		for (int i = 0; i < depths.length - 1; i++) {
			if (depth < depths[i+1] && depth >= depths[i])
				iDepth = i;
		}
		if (iDepth != -1) {
			double distance1 = distances[iDepth];
			double distance2 = distance1 + .5;
			Set<Phase> phases = new HashSet<>();
			phases.add(Phase.create("sS"));
			
			List<Double> traveltimes1 = toPhase(operateTauPTime(eventR, distance1, phases, model))
				.stream().mapToDouble(p -> p.getTravelTime()).boxed()
				.collect(Collectors.toList());
			List<Double> traveltimes2 = toPhase(operateTauPTime(eventR, distance2, phases, model))
					.stream().mapToDouble(p -> p.getTravelTime()).boxed()
					.collect(Collectors.toList());
			Collections.sort(traveltimes1);
			Collections.sort(traveltimes2);
			
			try {
				double firstArrival1 = traveltimes1.get(0);
				double firstArrival2 = traveltimes2.get(0);
				traveltime = (firstArrival2 - firstArrival1) / (distance2 - distance1) 
					* (epicentralDistance - distance1) + firstArrival1;
			} catch (IndexOutOfBoundsException e) {
				System.err.println("Error: " + eventR + " " + epicentralDistance + " " + depths[iDepth] + " " + distances[iDepth]);
				e.printStackTrace();
			}
		}
		return traveltime;
	}
	
	private static List<String> operateTauPTime(double eventR, double epicentralDistance, Set<Phase> phase) {
		return operateTauPTime(eventR, epicentralDistance, phase, "prem");
	}

	private static Set<TauPPhase> toPhase(List<String> lines) {
		if (lines == null || lines.size() <= 6) {
			return Collections.emptySet();
		}
		return IntStream.range(5, lines.size() - 1).mapToObj(lines::get).map(TauPTimeReader::toPhase)
				.collect(Collectors.toSet());
	}
	
	private static List<TauPPhase> toPhaseList(List<String> lines) {
		if (lines == null || lines.size() <= 6) {
			return Collections.emptyList();
		}
		return IntStream.range(5, lines.size() - 1).mapToObj(lines::get).map(TauPTimeReader::toPhase)
				.collect(Collectors.toList());
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
		Phase phaseName = Phase.create(parts[2]);
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
	 * @param phases
	 *            phase set
	 * @return command
	 */
	private static String[] makeCMD(double eventR, double epicentralDistance, Set<Phase> phases) {
		String phase = phases.stream().map(Object::toString).collect(Collectors.joining(","));
		String cmd = path + " -h " + (6371 - eventR) + " -deg " + epicentralDistance + " -model prem -ph " + phase;
		return cmd.split("\\s+");
	}
	
	private static String[] makeCMD(double eventR, double epicentralDistance, Set<Phase> phases, String model) {
		String phase = phases.stream().map(Object::toString).collect(Collectors.joining(","));
		String cmd = path + " -h " + (6371 - eventR) + " -deg " + epicentralDistance + " -model " + model + " -ph " + phase;
		return cmd.split("\\s+");
	}
/*
 * 
	public static TimewindowInformation timewindow(SACFileName sacname) throws IOException, TauModelException {
		double startTime = 0;
		double endTime = 0;
		double GCARC = sacname.readHeader().getValue(SACHeaderEnum.GCARC);
		Station station = sacname.read().getStation();
		
		TauP_Time timetool = new TauP_Time("prem");
		timetool.parsePhaseList("SKKS");
		timetool.depthCorrect(Earth.EARTH_RADIUS - sacname.read().getEventLocation().getR());
		
		timetool.calculate(GCARC);
		
//		List<Double> times = new ArrayList<>();
//		
//		timetool.getArrivals().stream()
//			.filter(arrival -> arrival.getDistDeg() == GCARC)
//			.forEach(arrival -> times.add(arrival.getTime()));
//		
//		Collections.sort(times);
		
//		startTime = times.get(0);
//		endTime = times.get(times.size() - 1);
		
		startTime = timetool.getArrival(0).getTime() - 170;
		endTime = startTime + 340;
		
		TimewindowInformation tw = new TimewindowInformation(startTime, endTime
				, station, sacname.getGlobalCMTID(), sacname.getComponent());
		
		return tw;
	}
 */
}
