package manhattan.datacorrection;

import filehandling.sac.SACComponent;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.Station;

/**
 * 
 * Static correction data for a raypath.<br>
 * <p>
 * <b> This class is IMMUTABlE.</b>
 * </p>
 *
 * When a time window for a synthetic is [t1, t2], then <br>
 * use a window of [t1-timeshift, t2-timeshift] in a observed one.<br>
 * 
 * Amplitude correction value is observed / synthetic
 * 
 * 
 * Time shift is rounded off to the second decimal place.
 * 
 * To identify which time window for a waveform, synStartTime is also used.
 * 
 * @version 0.1.1
 * 
 * 
 * @author kensuke
 *
 */
public class StaticCorrection implements Comparable<StaticCorrection> {

	@Override
	public int compareTo(StaticCorrection o) {
		int sta = station.compareTo(o.station);
		if (sta != 0)
			return sta;
		int id = eventID.compareTo(o.eventID);
		if (id != 0)
			return id;
		int comp = component.compareTo(o.component);
		if (comp != 0)
			return comp;
		int start = Double.compare(synStartTime, o.synStartTime);
		if (start != 0)
			return start;
		int shift = Double.compare(timeShift, o.timeShift);
		if (shift != 0)
			return shift;
		return Double.compare(amplitudeRatio, o.amplitudeRatio);
	}

	/**
	 * station
	 */
	private final Station station;

	/**
	 * event ID
	 */
	private final GlobalCMTID eventID;

	/**
	 * component
	 */
	private final SACComponent component;

	/**
	 * time shift [s]<br>
	 * Synthetic [t1, t2], Observed [t1 - timeShift, t2 - timeShift]
	 */
	private final double timeShift;

	/**
	 * amplitude correction: obs / syn
	 */
	private final double amplitudeRatio;

	/**
	 * start time of synthetic waveform
	 */
	private final double synStartTime;

	/**
	 * 
	 * When a time window for a synthetic is [synStartTime, synEndTime], then
	 * <br>
	 * use a window of [synStartTime-timeshift, synEndTime-timeshift] in a
	 * observed one.<br>
	 * Example, if you want to align a phase which arrives Ts in synthetic and
	 * To in observed, the timeshift will be Ts-To.<br>
	 * Amplitude ratio shall be observed / synthetic.
	 * 
	 * @param stationName
	 *            for shift
	 * @param eventID
	 *            for shift
	 * @param component
	 *            for shift
	 * @param synStartTime
	 *            for identification
	 * @param timeShift
	 *            value Synthetic [t1, t2], Observed [t1-timeShift,
	 *            t2-timeShift]
	 * @param amplitudeRatio
	 *            Observed / Synthetic
	 */
	public StaticCorrection(Station station, GlobalCMTID eventID, SACComponent component, double synStartTime,
			double timeShift, double amplitudeRatio) {
		this.station = station;
		this.eventID = eventID;
		this.component = component;
		this.synStartTime = Math.round(synStartTime * 100) / 100.0;
		this.timeShift = Math.round(timeShift * 100) / 100.0;
		this.amplitudeRatio = Math.round(amplitudeRatio * 100) / 100.0;
	}

	public Station getStation() {
		return station;
	}

	public GlobalCMTID getGlobalCMTID() {
		return eventID;
	}

	public SACComponent getComponent() {
		return component;
	}

	/**
	 * @return value of time shift (syn-obs)
	 */
	public double getTimeshift() {
		return timeShift;
	}

	/**
	 * @return value of ratio (obs / syn)
	 */
	public double getAmplitudeRatio() {
		return amplitudeRatio;
	}

	/**
	 * @return value of synthetic start time
	 */
	public double getSynStartTime() {
		return synStartTime;
	}

	@Override
	public String toString() {
		return station + " " + eventID + " " + component + " " + synStartTime + " " + timeShift + " " + amplitudeRatio;
	}

}
