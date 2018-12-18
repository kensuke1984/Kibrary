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

import edu.sc.seis.TauP.TauModel;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;

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
public final class TauPPierceReader {

	private TauPPierceReader() {
	}

	private static final String path = "taup_pierce";

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
	public static List<Info> getPierceInfo(Location eventLocation, HorizontalPosition stationPosition, String model, Phase... phases) {
		Set<Phase> phaseSet = new HashSet<>(Arrays.asList(phases));
		return toPhase(operateTauPPierce(eventLocation, stationPosition, phaseSet, model));
	}
	
	public static List<Info> getPierceInfo(Location eventLocation, HorizontalPosition stationPosition, String model, double pierceDepth, Phase... phases) {
		Set<Phase> phaseSet = new HashSet<>(Arrays.asList(phases));
		return toPhase(operateTauPPierce(eventLocation, stationPosition, phaseSet, model, pierceDepth), pierceDepth);
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
	public static List<Info> getPierceInfo(Location eventLocation, HorizontalPosition stationPosition, String model, Set<Phase> phaseSet) {
		return toPhase(operateTauPPierce(eventLocation, stationPosition, phaseSet, model));
	}
	
	/**
	 * TauPの結果の出力を読み込む
	 * 
	 * @param eventR
	 * @param epicentralDistance
	 * @param phase
	 * @return result lines
	 */
	private static List<String> operateTauPPierce(Location eventLocation, HorizontalPosition stationPosition, Set<Phase> phase, String model, double pierceDepth) {
		String[] cmd = makeCMD(eventLocation, stationPosition, phase, model, pierceDepth);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectError(ExternalProcess.bitBucket);
		try {
			Process p = pb.start();
			p.waitFor();
			List<String> outLines = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = br.readLine()) != null) {
					outLines.add(line);
//					System.out.println(line);
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
	
	private static List<String> operateTauPPierce(Location eventLocation, HorizontalPosition stationPosition, Set<Phase> phase, String model) {
		String[] cmd = makeCMD(eventLocation, stationPosition, phase, model);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectError(ExternalProcess.bitBucket);
		try {
			Process p = pb.start();
			p.waitFor();
			List<String> outLines = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = br.readLine()) != null) {
					outLines.add(line);
//					System.out.println(line);
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
	
	private static List<String> operateTauPPierce(Location eventLocation, HorizontalPosition stationPosition, Set<Phase> phase) {
		return operateTauPPierce(eventLocation, stationPosition, phase);
	}

	private static List<Info> toPhase(List<String> lines) {
		List<Info> infos = new ArrayList<>();
		if (lines == null || lines.size() == 0) {
			return Collections.emptyList();
		}
//		if (lines.size() % 2 != 0)
//			throw new RuntimeException("Unreadable output of taup_pierce");
//		for (int i = 0; i < lines.size() / 2; i++) {
//			String[] tmpLines = new String[] {lines.get(2 * i).trim(), lines.get(2 * i + 1).trim()};
//			infos.add(new Info(tmpLines));
//		}
		String[] tmpLines = new String[] {lines.get(0).trim(), lines.get(1).trim(), lines.get(2).trim(), lines.get(3).trim()};
		infos.add(new Info(tmpLines));
		return infos;
	}
	
	private static List<Info> toPhase(List<String> lines, double pierceDepth) {
		List<Info> infos = new ArrayList<>();
		if (lines == null || lines.size() == 0) {
			return Collections.emptyList();
		}
//		if (lines.size() % 2 != 0)
//			throw new RuntimeException("Unreadable output of taup_pierce");
//		for (int i = 0; i < lines.size() / 2; i++) {
//			String[] tmpLines = new String[] {lines.get(2 * i).trim(), lines.get(2 * i + 1).trim()};
//			infos.add(new Info(tmpLines));
//		}
		String[] tmpLines = lines.toArray(new String[0]);
		infos.add(new Info(tmpLines, pierceDepth));
		return infos;
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
	private static String[] makeCMD(Location eventLocation, HorizontalPosition stationPosition, Set<Phase> phases) {
		return makeCMD(eventLocation, stationPosition, phases, "prem");
	}
	
	private static String[] makeCMD(Location eventLocation, HorizontalPosition stationPosition, Set<Phase> phases, String model) {
		String phase = phases.stream().map(Object::toString).collect(Collectors.joining(","));
//		System.out.println(phase);
		if (!(phase.trim().equals("ScS") || phase.trim().equals("PcP")))
			throw new RuntimeException("Error: at the moment only ScS and PcP are supported for taup_pierce reader");
		String cmd = path 
				+ " -h " + (6371 - eventLocation.getR()) 
				+ " -evt " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
				+ " -sta " + stationPosition.getLatitude() + " " + stationPosition.getLongitude()
				+ " -model " + model
				+ " -ph " + phase 
				+ " -pierce 2491,2891 -nodiscon";
		return cmd.split("\\s+");
	}
	
	private static String[] makeCMD(Location eventLocation, HorizontalPosition stationPosition, Set<Phase> phases, String model, double pierceDepth) {
		String phase = phases.stream().map(Object::toString).collect(Collectors.joining(","));
//		System.out.println(phase);
		if (!(phase.trim().equals("ScS") || phase.trim().equals("PcP")))
			throw new RuntimeException("Error: at the moment only ScS and PcP are supported for taup_pierce reader");
		String cmd = path 
				+ " -h " + (6371 - eventLocation.getR()) 
				+ " -evt " + eventLocation.getLatitude() + " " + eventLocation.getLongitude()
				+ " -sta " + stationPosition.getLatitude() + " " + stationPosition.getLongitude()
				+ " -model " + model
				+ " -ph " + phase 
				+ " -pierce " + pierceDepth;
		return cmd.split("\\s+");
	}
	
	public static class Info {
		private Phase phase;
		private double travelTime;
		private double distance;
		private Location turningPoint;
		private Location leavePoint;
		private Location enterPoint;
		
		public Info(String[] lines, double pierceDepth) {
//			if (lines.length != 2)
//				throw new RuntimeException("Input should consit of two lines");
			if (lines.length == 4)
				parseOutputDpp(lines);
			else
				parseOutputS(lines, pierceDepth);
		}
		
		public Info(String[] lines) {
//			if (lines.length != 2)
//				throw new RuntimeException("Input should consit of two lines");
			if (lines.length == 4)
				parseOutputDpp(lines);
			else
				throw new RuntimeException("Error: Specify a pierce depth");
		}
		
		private void parseOutputDpp(String[] lines) {
//			System.out.println("-->\n" + lines[0] + "\n" + lines[1]);
			String[] parts0 = lines[0].split("\\s+");
			String[] parts1 = lines[1].split("\\s+");
			String[] parts2 = lines[2].split("\\s+");
			String[] parts3 = lines[3].split("\\s+");
			phase = Phase.create(parts0[1], false);
			travelTime = Double.parseDouble(parts0[3]);
			distance = Double.parseDouble(parts0[6]);
			turningPoint = new Location(Double.parseDouble(parts2[3])
					,Double.parseDouble(parts2[4])
					,6371. - Double.parseDouble(parts2[1]));
			enterPoint = new Location(Double.parseDouble(parts1[3])
					,Double.parseDouble(parts1[4])
					,6371. - Double.parseDouble(parts1[1]));
			leavePoint = new Location(Double.parseDouble(parts3[3])
					,Double.parseDouble(parts3[4])
					,6371. - Double.parseDouble(parts3[1]));
		}
		
		private void parseOutputS(String[] lines, double pierceDepth) {
			Location[] enterPoints = new Location[6];
			Location[] leavePoints = new Location[6];
			double[] rayparams = new double[6];
			int count = -1;
			int iMinRayParam = -1;
			double minRayParam = Double.MIN_VALUE;
			boolean foundEnterPoint = false;
			Location previousLoc = new Location(0, 0, 7000);
			for (String line : lines) {
				if (line.startsWith(">")) {
					foundEnterPoint = false;
					count++;
					rayparams[count] = Double.parseDouble(line.split("rayParam")[1].trim().split("\\s+")[0]);
					if (rayparams[count] > minRayParam) {
						iMinRayParam = count;
						minRayParam = rayparams[count];
					}
//					System.out.println(rayparams[count]);
				}
				else {
//					System.out.println(line);
//					String[] tmps = line.trim().split("\\s+"); 
//					for (String tmp : tmps)
//						System.out.println(tmp);
					double[] s = Arrays.stream(line.trim().split("\\s+")).mapToDouble(Double::parseDouble).toArray();
					double depth = s[1];
					if (Utilities.equalWithinEpsilon(depth, pierceDepth, 1.)) {
						if (!foundEnterPoint) {
							enterPoints[count] = new Location(s[3], s[4], Earth.EARTH_RADIUS - depth);
							foundEnterPoint = true;
						}
						else
							leavePoints[count] = new Location(s[3], s[4], Earth.EARTH_RADIUS - depth);
					}
					if (previousLoc.getR() > depth) {
						turningPoint = previousLoc;
					}
				}
			}
			
			enterPoint = enterPoints[iMinRayParam];
			leavePoint = leavePoints[iMinRayParam];
			
//			System.out.println(enterPoint + " " + leavePoint);
		}
		
		public Phase getPhase() {
			return phase;
		}
		
		public double getTravelTime() {
			return travelTime;
		}
		
		public double getDistance() {
			return distance;
		}
		
		public Location getTurningPoint() {
			return turningPoint;
		}
		
		public Location getEnterPoint() {
			return enterPoint;
		}
		
		public Location getLeavePoint() {
			return leavePoint;
		}
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
