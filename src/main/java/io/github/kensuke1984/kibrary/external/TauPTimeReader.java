package io.github.kensuke1984.kibrary.external;

import io.github.kensuke1984.anisotime.Phase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>
 * Utility class to handle with taup_time in TauP package
 * </p>
 * <p>
 * taup_time must be in PATH and it must run correctly.<br>
 * All the standard output and errors will go to the bit bucket
 * <p>
 * PREM is used for travel times.
 *
 * @author Kensuke Konishi
 * @version 0.3.2.2
 * @see <a href=http://www.seis.sc.edu/taup/>TauP</a>
 * <p>
 * <p>
 * TODO phase
 */
public final class TauPTimeReader {

    private static final String path = "taup_time";

    static {
        initialize();
    }

    private TauPTimeReader() {
    }

    private static void initialize() {
        if (!ExternalProcess.isInPath(path)) throw new RuntimeException(path + " is not in PATH");
    }

    /**
     * @param eventR             radius (km) !!not depth from the surface!!
     * @param epicentralDistance [deg] targetDistance
     * @param phases             to look for
     * @return travel times for the phase if theres a multiplication, all values
     * will be returned
     */
    public static Set<TauPPhase> getTauPPhase(double eventR, double epicentralDistance, Phase... phases) {
        Set<Phase> phaseSet = new HashSet<>(Arrays.asList(phases));
        return getTauPPhase(eventR, epicentralDistance, phaseSet);
    }

    /**
     * @param eventR             radius of seismic source !!not depth from the surface!!
     * @param epicentralDistance [deg] target epicentral distance
     * @param phaseSet           set of seismic phase.
     * @return {@link Set} of TauPPhases.
     */
    public static Set<TauPPhase> getTauPPhase(double eventR, double epicentralDistance, Set<Phase> phaseSet) {
        return toPhase(operateTauPTime(eventR, epicentralDistance, phaseSet));
    }

    /**
     * TauPの結果の出力を読み込む
     *
     * @param eventR             [km]
     * @param epicentralDistance [deg]
     * @param phase              set of phase
     * @return result lines
     */
    private static List<String> operateTauPTime(double eventR, double epicentralDistance, Set<Phase> phase) {
        String[] cmd = makeCMD(eventR, epicentralDistance, phase);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectError(ExternalProcess.bitBucket);
        try {
            Process p = pb.start();
            List<String> outLines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) outLines.add(line);
            }
            if (p.waitFor() != 0) throw new RuntimeException("Launching TauP faced problems");
            return outLines;
        } catch (Exception e) {
            System.err.println("Error occured. Could not find the time.");
            e.printStackTrace();
            return null;
        }
    }

    private static Set<TauPPhase> toPhase(List<String> lines) {
        if (lines == null || lines.size() <= 6) return Collections.emptySet();
        return IntStream.range(5, lines.size() - 1).mapToObj(lines::get).map(TauPTimeReader::toPhase)
                .collect(Collectors.toSet());
    }

    /**
     * Distance Depth Phase Travel Ray Param Takeoff Incident Purist distance
     * Purist name の順ではいっている文
     *
     * @param line to read
     * @return TauPPhase
     */
    private static TauPPhase toPhase(String line) {
        String[] parts = line.trim().split("\\s+");
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
     * @param eventR             [km] RADIUS (NOT depth from the surface)
     * @param epicentralDistance [deg]
     * @param phases             phase set
     * @return command
     */
    private static String[] makeCMD(double eventR, double epicentralDistance, Set<Phase> phases) {
        String phase = phases.stream().map(Object::toString).collect(Collectors.joining(","));
        String cmd = path + " -h " + (6371 - eventR) + " -deg " + epicentralDistance + " -model prem -ph " + phase;
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
