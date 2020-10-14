package io.github.kensuke1984.kibrary.timewindow;

import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.anisotime.Phase;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Timewindow for a raypath (a pair of a source and a receiver).
 * <p>
 * This class is <b>IMMUTABLE</b>
 * </p>
 * <p>
 * The information contains a component, a station and a global CMT ID.
 *
 * @author Kensuke Konishi
 * @version 0.1.3
 */
/**
 * @author Anselme
 *
 */
public class TimewindowInformation extends Timewindow {

    /**
     * station
     */
    private final Station station;
    /**
     * event ID
     */
    private final GlobalCMTID id;
	/**
	 * component
	 */
	private final SACComponent component;
	/**
	 * seismic phases included in the timewindow (e.g. S, ScS)
	 */
	private final Phase[] phases; 

	public TimewindowInformation(double startTime, double endTime, Station station, GlobalCMTID id,
			SACComponent component, Phase[] phases) {
		super(startTime, endTime);
		this.id = id;
		this.component = component;
		this.station = station;
		this.phases = phases;
	}

    @Override
    public int compareTo(Timewindow o) {
        if (!(o instanceof TimewindowInformation)) return super.compareTo(o);
        TimewindowInformation ot = (TimewindowInformation) o;
        int sta = getStation().compareTo(ot.getStation());
        if (sta != 0) return sta;
        int id = getGlobalCMTID().compareTo(ot.getGlobalCMTID());
        if (id != 0) return id;
        int comp = getComponent().compareTo(ot.getComponent());
        if (comp != 0) return comp;
        return super.compareTo(o);
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((component == null) ? 0 : component.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((station == null) ? 0 : station.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        TimewindowInformation other = (TimewindowInformation) obj;
        if (component != other.component) return false;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        if (station == null) {
            if (other.station != null) return false;
        } else if (!station.equals(other.station)) return false;
        return true;
    }

    public Station getStation() {
        return station;
    }

    public GlobalCMTID getGlobalCMTID() {
        return id;
    }

	public SACComponent getComponent() {
		return component;
	}
	
	/**
	 * @return
	 * @author anselme
	 */
	public Phase[] getPhases() {
		return phases;
	}
	
	/**
	 * @return
	 * @author anselme
	 */
	public double getAzimuthDegree() {
		return Math.toDegrees(id.getEvent().getCmtLocation().getAzimuth(station.getPosition()));
	}
	
	/**
	 * @return
	 * @author anselme
	 */
	public double getDistanceDegree() {
		return Math.toDegrees(id.getEvent().getCmtLocation().getEpicentralDistance(station.getPosition()));
	}

	@Override
	public String toString() {
		List<String> phaseStrings = Stream.of(phases).filter(phase -> phase != null).map(Phase::toString).collect(Collectors.toList());
		return station + " " + id + " " + component + " " + startTime + " " + endTime + " " + String.join(",", phaseStrings);
	}

}
