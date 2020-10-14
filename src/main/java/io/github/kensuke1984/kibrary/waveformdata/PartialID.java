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
 * This class is <b>IMMUTABLE</b> <br>
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
 * One ID volume:{@link PartialIDFile#oneIDByte}
 * </p>
 *
 * @version 0.2.0.1.1
 * @author Kensuke Konishi
 *
 */
public class PartialID extends BasicID {

    /**
     * location of perturbation
     */
    protected final Location POINT_LOCATION;
    /**
     * type of parameter
     */
    protected final PartialType PARTIAL_TYPE;

	@Override
	public String toString() {
		String partialString = STATION.getName() + " " + STATION.getNetwork() + " " + ID + " " + COMPONENT + " " + SAMPLINGHZ + " "
				+ START_TIME + " " + NPTS + " " + MIN_PERIOD + " " + MAX_PERIOD + " ";
		for (int i = 0; i < PHASES.length - 1; i++)
			partialString += PHASES[i] + ",";
		partialString += PHASES[PHASES.length - 1];
		partialString += " " + START_BYTE + " " + CONVOLUTE + " "
				+ POINT_LOCATION + " " + PARTIAL_TYPE;
		return partialString;
	}

    public Location getPerturbationLocation() {
        return POINT_LOCATION;
    }
	
	public PartialID(Station station, GlobalCMTID eventID, SACComponent sacComponent, double samplingHz,
			double startTime, int npts, double minPeriod, double maxPeriod, Phase[] phases, long startByte, boolean isConvolved,
			Location perturbationLocation, PartialType partialType, double... waveformData) {
		super(WaveformType.PARTIAL, samplingHz, startTime, npts, station, eventID, sacComponent, minPeriod, maxPeriod,
				phases, startByte, isConvolved, waveformData);
		PARTIAL_TYPE = partialType;
		POINT_LOCATION = perturbationLocation;
	}


    public PartialType getPartialType() {
        return PARTIAL_TYPE;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((PARTIAL_TYPE == null) ? 0 : PARTIAL_TYPE.hashCode());
        result = prime * result + ((POINT_LOCATION == null) ? 0 : POINT_LOCATION.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        PartialID other = (PartialID) obj;
        if (PARTIAL_TYPE != other.PARTIAL_TYPE) return false;
        if (POINT_LOCATION == null) {
            if (other.POINT_LOCATION != null) return false;
        } else if (!POINT_LOCATION.equals(other.POINT_LOCATION)) return false;
        return true;
    }


    /**
     * @param data to be set
     * @return {@link PartialID} with the input data
     */
	@Override
	public PartialID setData(double[] data) {
		return new PartialID(STATION, ID, COMPONENT, SAMPLINGHZ, START_TIME, data.length, MIN_PERIOD, MAX_PERIOD,
				PHASES, START_BYTE, CONVOLUTE, POINT_LOCATION, PARTIAL_TYPE, data);
	}

}
