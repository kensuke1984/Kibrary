package manhattan.datacorrection;

import filehandling.sac.SACComponent;
import manhattan.globalcmt.GlobalCMTID;

/**
 * 
 * Time shift for static correction.<br>
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
 * @version 0.0.2
 * @since 2013/12/17 {@link GlobalCMTID}に
 * 
 * @version 0.0.5
 * @since 2014/10/13 {@link #synStartTime} installed.
 * 
 * @version 0.1.0
 * @since 2015/5/31 Amplitude ratio (observed / synthetic)
 * 
 * @version 0.1.1
 * @since 2015/9/14 station name must be now only 8 or less than 8 letters
 * 
 * 
 * @author kensuke
 *
 */
public class StaticCorrection {

	/**
	 * station name
	 */
	private final String stationName;

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
	public StaticCorrection(String stationName, GlobalCMTID eventID, SACComponent component, double synStartTime,
			double timeShift, double amplitudeRatio) {
		if (8 < stationName.length())
			throw new IllegalArgumentException("The station name" + stationName + "must be 8 or shorter");
		else
			this.stationName = stationName;
		this.eventID = eventID;
		this.component = component;
		this.synStartTime = Math.round(synStartTime * 100) / 100.0;
		this.timeShift = Math.round(timeShift * 100) / 100.0;
		this.amplitudeRatio = Math.round(amplitudeRatio * 100) / 100.0;
	}

	public String getStationName() {
		return stationName;
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

	public String toString() {
		return stationName + ' ' + eventID + ' ' + component + ' ' + synStartTime + ' ' + timeShift + ' '
				+ amplitudeRatio;
	}

}
