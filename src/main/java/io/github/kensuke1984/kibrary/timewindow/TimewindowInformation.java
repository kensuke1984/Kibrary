package io.github.kensuke1984.kibrary.timewindow;


import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * Timewindow for a raypath (a pair of a source and a receiver).
 * <p>This class is <b>IMMUTABLE</b></p>
 * 
 * The information contains a component, a station and a global CMT ID.
 * 
 * 
 * @version 0.1.2
 * 
 * @author Kensuke Konishi
 */
public class TimewindowInformation extends Timewindow {

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((component == null) ? 0 : component.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((station == null) ? 0 : station.hashCode());
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
		TimewindowInformation other = (TimewindowInformation) obj;
		if (component != other.component)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (station == null) {
			if (other.station != null)
				return false;
		} else if (!station.equals(other.station))
			return false;
		return true;
	}

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

	public TimewindowInformation(double startTime, double endTime, Station station, GlobalCMTID id,
			SACComponent component) {
		super(startTime, endTime);
		this.id = id;
		this.component = component;
		this.station = station;
	}

	public Station getStation(){
		return station;
	}
	
	
	public GlobalCMTID getGlobalCMTID() {
		return id;
	}

	public SACComponent getComponent() {
		return component;
	}

	@Override
	public String toString() {
		return station + " " + id + " " + component + " " + startTime + " " + endTime;
	}

}
