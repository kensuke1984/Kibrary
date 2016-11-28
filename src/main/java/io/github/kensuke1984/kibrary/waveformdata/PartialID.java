package io.github.kensuke1984.kibrary.waveformdata;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

/***
 * <p>
 * ID for a partial derivative
 * </p>
 * <b>This class is IMMUTABLE</b> <br>
 * 
 * =Contents of information for one ID=<br>
 * Name of station<br>
 * Name of network<br>
 * Horizontal position of station (latitude longitude)<br>
 * Global CMT ID<br>
 * Component (ZRT)<br>
 * Period minimum and maximum<br>
 * Start time<br>
 * Number of points<br>
 * Sampling Hz<br>
 * If one is convoluted or observed, true<br>
 * Position of a waveform for the ID<br>
 * partial type<br>
 * Location of a perturbation point: latitude, longitude, radius
 * 
 * 
 * 
 * <p>
 * One ID volume:{@link #oneIDbyte}: {@value #oneIDbyte}
 * </p>
 * 
 * 
 * @version 0.2.0.1
 * 
 * @author Kensuke Konishi
 * 
 */
public class PartialID extends BasicID {

	@Override
	public String toString() {
		String partialString = STATION.getStationName() + " " + STATION.getNetwork() + " " + ID + " " + COMPONENT + " " + SAMPLINGHZ + " "
				+ START_TIME + " " + NPTS + " " + MIN_PERIOD + " " + MAX_PERIOD + " ";
		for (int i = 0; i < PHASES.length - 1; i++)
			partialString += PHASES[i] + ",";
		partialString += PHASES[PHASES.length - 1];
		partialString += " " + START_BYTE + " " + CONVOLUTE + " "
				+ pointLocation + " " + partialType;
		return partialString;
	}

	/**
	 * 摂動点の位置
	 */
	protected final Location pointLocation;

	/**
	 * パラメタの種類
	 */
	protected final PartialType partialType;
	
	public PartialID(Station station, GlobalCMTID eventID, SACComponent sacComponent, double samplingHz,
			double startTime, int npts, double minPeriod, double maxPeriod, Phase[] phases, long startByte, boolean isConvolved,
			Location perturbationLocation, PartialType partialType, double... waveformData) {
		super(WaveformType.PARTIAL, samplingHz, startTime, npts, station, eventID, sacComponent, minPeriod, maxPeriod,
				phases, startByte, isConvolved, waveformData);
		this.partialType = partialType;
		this.pointLocation = perturbationLocation;
	}
	
	public Location getPerturbationLocation() {
		return pointLocation;
	}

	public PartialType getPartialType() {
		return partialType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((partialType == null) ? 0 : partialType.hashCode());
		result = prime * result + ((pointLocation == null) ? 0 : pointLocation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PartialID other = (PartialID) obj;
		if (partialType != other.partialType)
			return false;
		if (pointLocation == null) {
			if (other.pointLocation != null)
				return false;
		} else if (!pointLocation.equals(other.pointLocation))
			return false;
		return true;
	}

	/**
	 * @param data
	 *            to be set
	 * @return {@link PartialID} with the input data
	 */
	@Override
	public PartialID setData(double[] data) {
		return new PartialID(STATION, ID, COMPONENT, SAMPLINGHZ, START_TIME, NPTS, MIN_PERIOD, MAX_PERIOD,
				PHASES, START_BYTE, CONVOLUTE, pointLocation, partialType, data);
	}

}
