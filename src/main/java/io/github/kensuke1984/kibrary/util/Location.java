package io.github.kensuke1984.kibrary.util;

import io.github.kensuke1984.kibrary.math.geometry.RThetaPhi;
import io.github.kensuke1984.kibrary.math.geometry.XYZ;
import org.apache.commons.math3.util.Precision;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Location
 * </p>
 * Latitude (-180, 180) Longitude（-90, 90）Radius [0,&infin;)
 * <p>
 * <b>This class is IMMUTABLE</b>.
 * </p>
 * <p>
 * This class rounds off values at the 4th decimal point.
 *
 * @author Kensuke Konishi
 * @version 0.1.1.3
 * @author anselme add methods used for BP/FP catalog
 */
public class Location extends HorizontalPosition {

    /**
     * [km] radius rounded off to the 3 decimal places.
     */
    private final double R;
    /**
     * equals within epsilon
     */
    private double eps = 1e-4;

    /**
     * @param latitude  [deg] geographical latitude
     * @param longitude [deg] longitude
     * @param r         [km] radius
     */
    public Location(double latitude, double longitude, double r) {
        super(latitude, longitude);
//        R = Precision.round(r, 3);
        R = r;
    }

    public static double toLatitude(double theta) {
        return Latitude.toLatitude(theta);
    }

    /**
     * Sorting order is latitude &rarr; longitude &rarr; radius.
     */
    @Override
    public int compareTo(HorizontalPosition o) {
        int horizontalCompare = super.compareTo(o);
        if (horizontalCompare != 0 || !(o instanceof Location)) return horizontalCompare;
        return Double.compare(R, ((Location) o).R);
    }

    /**
     * @return [km] radius (not depth)
     */
    public double getR() {
        return R;
    }

    /**
     * @return {@link RThetaPhi} of this
     */
    public RThetaPhi getRThetaPhi() {
        return new RThetaPhi(R, getTheta(), getPhi());
    }

    /**
     * Cartesian coordinate
     *
     * @return {@link XYZ} of this
     */
    public XYZ toXYZ() {
        return RThetaPhi.toCartesian(R, getTheta(), getPhi());
    }
    
	/**
	 * used for FP/BP catalog
	 * @return
	 * @author anselme
	 */
	public XYZ toXYZGeographical() {
		double theta = Math.toRadians(90. - getLatitude());
		return RThetaPhi.toCartesian(R, theta, getPhi());
	}
	
	/**
	 * @return
	 * @author anselme
	 */
	public HorizontalPosition toHorizontalPosition() {
		return new HorizontalPosition(getLatitude(), getLongitude());
	}

    /**
     * @param location {@link Location} to compute distance with
     * @return [km] one-line distance from the location
     */
    public double getDistance(Location location) {
        return location.toXYZ().getDistance(toXYZ());
    }
    
	/**
	 * @param location
	 * @return
	 * @author anselme
	 */
	public double getDistanceGeographical(Location location) {
		return location.toXYZGeographical().getDistance(toXYZGeographical());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
//		long temp;
//		temp = Double.doubleToLongBits(R);
//		result = prime * result + (int) (temp ^ (temp >>> 32));
//		int temp = (int) (R / eps / 10);
		int temp = (int) R;
		result = prime * result + temp; 
		return result;
	}

    /**
     *@author anselme equals within epsilon
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        Location other = (Location) obj;
//        return Double.doubleToLongBits(R) == Double.doubleToLongBits(other.R);
        return Utilities.equalWithinEpsilon(R, other.R, eps);
    }

    /**
     * @param locations to be sorted.
     * @return locations in the order of the distance from this.
     */
    public Location[] getNearestLocation(Location[] locations) {
        Location[] newLocations = locations.clone();
        Arrays.parallelSort(newLocations, Comparator.comparingDouble(this::getDistance));
        return newLocations;
    }

	/**
	 * @param locations
	 * @param maxSearchRange
	 * @return
	 * @author anselme
	 */
	public Location[] getNearestLocation(Location[] locations, double maxSearchRange) {
		Location[] newLocations = Arrays.stream(locations).parallel().filter(loc -> {
	//		System.out.println(loc + " " + this.toString() + " " + this.getDistance(loc));
			return Math.abs(this.R - loc.getR()) < maxSearchRange;
		}).collect(Collectors.toList()).toArray(new Location[0]);
	//	System.out.println(newLocations.length);
		Arrays.parallelSort(newLocations, Comparator.comparingDouble(this::getDistance));
		return newLocations;
	}
	
	/**
	 * @param locations
	 * @return
	 * @author anselme
	 */
	public Location[] getNearestHorizontalLocation(Location[] locations) {
		Location[] newLocations = Arrays.stream(locations)
				.filter(loc -> loc.getR() == this.getR())
				.collect(Collectors.toList())
				.toArray(new Location[0]);
		Arrays.sort(newLocations, Comparator.comparingDouble(this::getDistance));
		return newLocations;
	}

    @Override
    public String toString() {
        return super.toString() + ' ' + R;
    }
    
}
