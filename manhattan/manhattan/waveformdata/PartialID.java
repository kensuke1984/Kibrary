package manhattan.waveformdata;

import filehandling.sac.SACComponent;
import filehandling.sac.WaveformType;
import filehandling.spc.PartialType;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.Location;
import manhattan.template.Station;

/***
 * <p>
 * ID for a partial derivative
 * </p>
 * <b>This class is IMMUTABLE</b> <br>
 * 
 * type: String(s), Float(f), Integer(i), Long(l), Boolean(b)<br>
 * =Contents of information for one ID=<br>
 * Name of station (must be 8 or less letters) 8Byte(s)<br>
 * Name of network (must be 8 or less letters) 8Byte(s)<br>
 * Horizontal position of station (latitude longitude) 4Byte*2(f*2)<br>
 * Global CMT ID 15Byte(s)<br>
 * Component (ZRT) 1Byte<br>
 * Period minimum and maximum 4Byte*2(f*2) <br>
 * Start time 4Byte(f)<br>
 * Number of points 4Byte(i)<br>
 * Sampling Hz 4Byte(f)<br>
 * If one is convoluted or observed, true 1Byte(b)<br>
 * Position of a waveform for the ID 8Byte(l)<br>
 * partial type 1Byte<br>
 * Location of a perturbation point: latitude, longitude, radius 4Byte*3(f*3)
 * 
 * 
 * 
 * <p>
 * One ID volume:{@link #oneIDbyte}: {@value #oneIDbyte}
 * </p>
 * 
 * 
 * @since 2013/12/1
 * 
 * @version 0.2
 * 
 * @author kensuke
 * 
 */
public class PartialID extends BasicID {

	@Override
	public String toString() {
		return station + " " + station.getNetwork() + " " + globalCMTID + " " + sacComponent + " " + samplingHz + " "
				+ startTime + " " + npts + " " + minPeriod + " " + maxPeriod + " " + startByte + " " + isConvolved + " "
				+ pointLocation + " " + partialType;
	}

	/**
	 * File size for an ID
	 */
	public static final int oneIDbyte = 82;

	/**
	 * 摂動点の位置
	 */
	protected final Location pointLocation;

	/**
	 * パラメタの種類
	 */
	protected final PartialType partialType;

	public PartialID(Station station, GlobalCMTID eventID, SACComponent sacComponent, double samplingHz,
			double startTime, int npts, double minPeriod, double maxPeriod, long startByte, boolean isConvolved,
			Location perturbationLocation, PartialType partialType, double... waveformData) {
		super(WaveformType.PARTIAL, samplingHz, startTime, npts, station, eventID, sacComponent, minPeriod, maxPeriod,
				startByte, isConvolved, waveformData);
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
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		PartialID other = (PartialID) obj;
		if (partialType != other.partialType) {
			return false;
		}
		if (pointLocation == null) {
			if (other.pointLocation != null) {
				return false;
			}
		} else if (!pointLocation.equals(other.pointLocation)) {
			return false;
		}
		return true;
	}

	/**
	 * @param data
	 *            to be set
	 * @return {@link PartialID} with the input data
	 */
	@Override
	public PartialID setData(double[] data) {
		return new PartialID(station, globalCMTID, sacComponent, samplingHz, startTime, npts, minPeriod, maxPeriod,
				startByte, isConvolved, pointLocation, partialType, data);
	}

}
