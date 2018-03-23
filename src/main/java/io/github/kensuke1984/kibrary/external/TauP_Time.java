package io.github.kensuke1984.kibrary.external;

import edu.sc.seis.TauP.Alert;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauPException;
import io.github.kensuke1984.anisotime.Phase;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * successor of TauPTimeReader.
 * <p>
 * Utility class to handle with taup_time in TauP.
 * </p>
 * PREM is used for travel times.
 *
 * @author Kensuke Konishi
 * @version 0.0.1
 * @see <a href='https://www.seis.sc.edu/taup/'>TauP</a>
 */
public final class TauP_Time extends edu.sc.seis.TauP.TauP_Time {
    private TauP_Time() {
    }

    /**
     * @param eventR             [km] radius of seismic source !!not depth from the surface!!
     * @param epicentralDistance [deg] target epicentral distance
     * @param phaseSet           set of seismic phase.
     * @return {@link Set} of TauPPhases.
     */
    public static Set<TauPPhase> getTauPPhase(double eventR, double epicentralDistance, Set<Phase> phaseSet)
            throws IOException, TauModelException, TauPException {
        return toPhase(operateTauPTime(eventR, epicentralDistance, phaseSet));
    }

    /**
     * list output lines from TauP
     *
     * @param eventR             [km]
     * @param epicentralDistance [deg]
     * @param phase              set of phase
     * @return result lines
     */
    private static List<String> operateTauPTime(double eventR, double epicentralDistance, Set<Phase> phase)
            throws IOException, TauPException, TauModelException {
        String[] cmd = makeCMD(eventR, epicentralDistance, phase);
        try (StringWriter writer = new StringWriter()) {
            TauP_Time tauPTime = new TauP_Time();
            String[] noComprendoArgs = tauPTime.parseCmdLineArgs(cmd);
            printNoComprendoArgs(noComprendoArgs);
            tauPTime.init();
            PrintWriter printWriter = new PrintWriter(writer);
            tauPTime.setWriter(printWriter);
            tauPTime.start();
            return Arrays.asList(writer.toString().split("\\n"));
//            tauPTime.destroy();
        } catch (TauModelException e) {
            Alert.error("Caught TauModelException", e.getMessage());
            throw e;
        } catch (TauPException e) {
            Alert.error("Caught TauPException", e.getMessage());
            throw e;
        }
    }

    private static Set<TauPPhase> toPhase(List<String> lines) {
        if (lines == null || lines.size() <= 5) return Collections.emptySet();
        return IntStream.range(5, lines.size()).mapToObj(lines::get).map(TauP_Time::toPhase)
                .collect(Collectors.toSet());
    }

    /**
     * @param line containing Distance, Depth, Phase, Travel, Ray Param, Takeoff, Incident, Purist distance,
     *             Purist name
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
        String puristName = parts[9];
        return new TauPPhase(distance, depth, phaseName, travelTime, rayParameter, takeoff, incident, puristDistance,
                puristName);
    }

    /**
     * creates a command for TauP
     *
     * @param eventR             [km] RADIUS (NOT depth from the surface)
     * @param epicentralDistance [deg]
     * @param phases             phase set
     * @return command
     */
    private static String[] makeCMD(double eventR, double epicentralDistance, Set<Phase> phases) {
        String phase = phases.stream().map(Object::toString).collect(Collectors.joining(","));
        String cmd = "-h " + (6371 - eventR) + " -deg " + epicentralDistance + " -model prem -ph " + phase;
        return cmd.split("\\s+");
    }
}
