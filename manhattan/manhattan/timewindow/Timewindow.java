package manhattan.timewindow;

/**
 * Time window starting and ending time.<br>
 * <b>Those are round off to the 3rd decimal place.</b>
 * <p>
 * <b>This class is IMMUTABLE</b>
 * </p>
 * 
 * @version 0.1.0
 * @author Kensuke
 *
 */
public class Timewindow implements Comparable<Timewindow> {

	@Override
	public int compareTo(Timewindow o) {
		if (startTime < o.startTime)
			return -1;
		else if (startTime == o.startTime)
			return Double.compare(endTime, o.endTime);
		else
			return 1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(endTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(startTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Timewindow other = (Timewindow) obj;
		if (Double.doubleToLongBits(endTime) != Double.doubleToLongBits(other.endTime))
			return false;
		if (Double.doubleToLongBits(startTime) != Double.doubleToLongBits(other.startTime))
			return false;
		return true;
	}

	public Timewindow(double startTime, double endTime) {
		if (endTime < startTime)
			throw new IllegalArgumentException("startTime: " + startTime + " endTime: " + endTime + " are invalid");
		this.startTime = Math.round(startTime * 1000) / 1000.0;
		this.endTime = Math.round(endTime * 1000) / 1000.0;
	}

	@Override
	public String toString() {
		return startTime + " " + endTime;
	}

	/**
	 * @param timeWindow
	 *            to check
	 * @return if timeWindow and this overlap
	 */
	boolean overlap(Timewindow timeWindow) {
		return !(endTime < timeWindow.startTime || timeWindow.endTime < startTime);
	}

	/**
	 * Creates a new Timewindow from this and the input timewindow. If the two
	 * windows do not overlap, then the interval between them is also included.
	 * 
	 * 
	 * @param timeWindow
	 *            timewindow to merge
	 * @return timewindow new {@link Timewindow} for merged window
	 */
	Timewindow merge(Timewindow timeWindow) {
		double newStart = startTime < timeWindow.startTime ? startTime : timeWindow.startTime;
		double newEnd = timeWindow.endTime < endTime ? endTime : timeWindow.endTime;
		return new Timewindow(newStart, newEnd);
	}

	/**
	 * starting time round off to the third decimal place
	 */
	protected final double startTime;

	/**
	 * ending time round off to the third decimal place
	 */
	protected final double endTime;

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}

}
