package manhattan.timewindow;


import filehandling.sac.SACComponent;
import manhattan.globalcmt.GlobalCMTID;

/**
 * ある震源観測点成分組み合わせのタイムウインドウ
 * 
 * @version 0.1.0
 * @since 2013/12/17 {@link GlobalCMTID}を用いる
 * 
 * 
 * @version 0.1.1
 * @since 2015/8/24 slim up <b>This class is IMMUTABLE</b>
 * @author Kensuke
 * 
 * 
 * @version 0.1.2
 * @since 2015/9/14 stationName must be 8 or less than 8 letters.s
 * 
 */
public class TimewindowInformation extends Timewindow {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((component == null) ? 0 : component.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((stationName == null) ? 0 : stationName.hashCode());
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
		if (stationName == null) {
			if (other.stationName != null)
				return false;
		} else if (!stationName.equals(other.stationName))
			return false;
		return true;
	}

	/**
	 * station name
	 */
	private final String stationName;

	/**
	 * event ID
	 */
	private final GlobalCMTID id;

	/**
	 * component
	 */
	private final SACComponent component;

	public TimewindowInformation(double startTime, double endTime, String stationName, GlobalCMTID id,
			SACComponent component) {
		super(startTime, endTime);
		if (8 < stationName.length())
			throw new IllegalArgumentException("the length of a station name " + stationName + " must be 8 or shorter");
		this.stationName = stationName;
		this.id = id;
		this.component = component;
	}

	public String getStationName() {
		return stationName;
	}

	public GlobalCMTID getGlobalCMTID() {
		return id;
	}

	public SACComponent getComponent() {
		return component;
	}

	@Override
	public String toString() {
		return stationName + " " + id + " " + component + " " + startTime + " " + endTime;
	}

}
