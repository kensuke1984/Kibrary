package io.github.kensuke1984.kibrary.timewindow;

import org.apache.commons.math3.util.Precision;

/**
 * Time window starting and ending time.<br>
 * <b>Those are round off to the 3rd decimal place.</b>
 * <p>
 * This class is <b>IMMUTABLE</b>
 * </p>
 *
 * @author Kensuke Konishi
 * @version 0.1.0.2
 */
public class Timewindow implements Comparable<Timewindow> {

    /**
     * starting time round off to the third decimal place
     */
    protected final double startTime;
    /**
     * ending time round off to the third decimal place
     */
    protected final double endTime;

    /**
     * startTime must be less than endTime
     *
     * @param startTime start time of the window
     * @param endTime   end time of the window
     */
    public Timewindow(double startTime, double endTime) {
        if (endTime < startTime)
            throw new IllegalArgumentException("startTime: " + startTime + " endTime: " + endTime + " are invalid");
        this.startTime = Precision.round(startTime, 3);
        this.endTime = Precision.round(endTime, 3);
    }

    @Override
    public int compareTo(Timewindow o) {
        int c = Double.compare(startTime, o.startTime);
        return c != 0 ? c : Double.compare(endTime, o.endTime);
    }

    @Override
    public int hashCode() {
        int prime = 31;
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
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Timewindow other = (Timewindow) obj;
        return Double.doubleToLongBits(endTime) == Double.doubleToLongBits(other.endTime) &&
                Double.doubleToLongBits(startTime) == Double.doubleToLongBits(other.startTime);
    }

    @Override
    public String toString() {
        return startTime + " " + endTime;
    }

    /**
     * @param timeWindow to check
     * @return if timeWindow and this overlap
     */
    boolean overlap(Timewindow timeWindow) {
        return timeWindow.startTime <= endTime && startTime <= timeWindow.endTime;
    }

    /**
     * Creates a new Timewindow from this and the input timewindow. If the two
     * windows do not overlap, then the interval between them is also included.
     *
     * @param timeWindow timewindow to merge
     * @return timewindow new {@link Timewindow} for merged window
     */
    Timewindow merge(Timewindow timeWindow) {
        double newStart = startTime < timeWindow.startTime ? startTime : timeWindow.startTime;
        double newEnd = timeWindow.endTime < endTime ? endTime : timeWindow.endTime;
        return new Timewindow(newStart, newEnd);
    }

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}
	
	public double getLength() {
		return endTime - startTime;
	}

}
