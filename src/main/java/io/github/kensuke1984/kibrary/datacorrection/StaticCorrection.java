package io.github.kensuke1984.kibrary.datacorrection;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import org.apache.commons.math3.util.Precision;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Static correction data for a raypath.<br>
 * <p>
 * This class is <b>IMMUTABlE</b>
 * <p>
 * When a time window for a synthetic is [t1, t2], then <br>
 * use a window of [t1-timeshift, t2-timeshift] in a observed one.<br>
 * and amplitude observed dataset is divided by the AMPLITUDE.
 * <p>
 * In short, time correction value is relative pick time in synthetic - the one
 * in observed.
 * <p>
 * Amplitude correction value (AMPLITUDE) is observed /
 * synthetic.
 * <p>
 * Time shift is rounded off to the second decimal place.
 * <p>
 * To identify which time window for a waveform, SYNTHETIC_TIME is also used.
 *
 * @author Kensuke Konishi
 * @version 0.1.1.2
 * @author anselme add phase information
 */
public class StaticCorrection implements Comparable<StaticCorrection> {

	private final Station STATION;
	private final GlobalCMTID ID;
	private final SACComponent COMPONENT;
	/**
	 * time shift [s]<br>
	 * Synthetic [t1, t2], Observed [t1 - TIME, t2 - TIME]
	 */
	private final double TIME;
	/**
	 * amplitude correction: obs / syn<br>
	 * Observed should be divided by this value.
	 */
	private final double AMPLITUDE;
	/**
	 * start time of synthetic waveform
	 */
	private final double SYNTHETIC_TIME;
	
	/**
	 * phases in windows
	 */
	private final Phase[] PHASES;

    /**
     * When a time window for a synthetic is [start, end], then
     * use a window of [start-timeshift, end-timeshift] in the corresponding
     * observed one.<br>
     * Example, if you want to align a phase which arrives Ts in synthetic and
     * To in observed, the timeshift will be Ts-To.<br>
     * Amplitude ratio shall be observed / synthetic. Observed will be divided by this value.
     * <p>
     * synStartTime may be used only for identification when your dataset contain multiple time windows in one waveform.
     *
     * @param station        for shift
     * @param eventID        for shift
     * @param component      for shift
     * @param synStartTime   for identification
     * @param timeShift      value Synthetic [t1, t2], Observed [t1-timeShift,
     *                       t2-timeShift]
     * @param amplitudeRatio Observed / Synthetic, an observed waveform will be divided by this value.
     */
	public StaticCorrection(Station station, GlobalCMTID eventID, SACComponent component, double synStartTime,
			double timeShift, double amplitudeRatio, Phase[] phases) {
		STATION = station;
        ID = eventID;
        COMPONENT = component;
        SYNTHETIC_TIME = Precision.round(synStartTime, 2);
        TIME = Precision.round(timeShift, 2);
        AMPLITUDE = Precision.round(amplitudeRatio, 2);
		PHASES = phases;
	}
	
    @Override
    public int compareTo(StaticCorrection o) {
        int sta = STATION.compareTo(o.STATION);
        if (sta != 0) return sta;
        int id = ID.compareTo(o.ID);
        if (id != 0) return id;
        int comp = COMPONENT.compareTo(o.COMPONENT);
        if (comp != 0) return comp;
        int start = Double.compare(SYNTHETIC_TIME, o.SYNTHETIC_TIME);
        if (start != 0) return start;
        int shift = Double.compare(TIME, o.TIME);
        if (shift != 0) return shift;
        return Double.compare(AMPLITUDE, o.AMPLITUDE);
    }

	public Station getStation() {
		return STATION;
	}

	public GlobalCMTID getGlobalCMTID() {
		return ID;
	}

	public SACComponent getComponent() {
		return COMPONENT;
	}

	/**
	 * @return value of time shift (syn-obs)
	 */
	public double getTimeshift() {
		return TIME;
	}

	/**
	 * @return value of ratio (obs / syn)
	 */
	public double getAmplitudeRatio() {
		return AMPLITUDE;
	}

    /**
     * @return value of synthetic start time for the identification when you use multiple time windows.
     */
	public double getSynStartTime() {
		return SYNTHETIC_TIME;
	}
	
	public Phase[] getPhases() {
		return PHASES;
	}

	@Override
	public String toString() {
		List<String> phaseStrings = 
				Stream.of(PHASES).filter(phase -> phase != null).map(Phase::toString).collect(Collectors.toList());
		return STATION.getName() + " " + STATION.getNetwork() + " " + STATION.getPosition() + " " + ID 
				+ " " + COMPONENT + " " + SYNTHETIC_TIME + " " + TIME + " " + AMPLITUDE + " " + String.join(",", phaseStrings);
	}

}
