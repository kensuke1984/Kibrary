package io.github.kensuke1984.kibrary.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Precision;

import io.github.kensuke1984.kibrary.math.geometry.RThetaPhi;
import io.github.kensuke1984.kibrary.math.geometry.XYZ;

/**
 * <p>
 * Location
 * </p>
 * Latitude (-180, 180) Longitude（-90, 90）Radius [0,&infin;)
 * 
 * <p>
 * <b>This class is IMMUTABLE</b>.
 * </p>
 * 
 * This class rounds off values at the 4th decimal point.
 * 
 * @version 0.1.1.3
 * 
 * @author Kensuke Konishi
 * 
 */
public class Location extends HorizontalPosition {

	/**
	 * Sorting order is latitude &rarr; longitude &rarr; radius.
	 */
	@Override
	public int compareTo(HorizontalPosition o) {
		int horizontalCompare = super.compareTo(o);
		if (horizontalCompare != 0 || !(o instanceof Location))
			return horizontalCompare;
		return Double.compare(R,  ((Location) o).R);
	}

	/**
	 * [km] radius rounded off to the 3 decimal places.
	 */
	private final double R;

	/**
	 * @param latitude
	 *            [deg] geographical latitude
	 * @param longitude
	 *            [deg] longitude
	 * @param r
	 *            [km] radius
	 */
	public Location(double latitude, double longitude, double r) {
		super(latitude, longitude);
		R = Precision.round(r, 3);
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
	 * @return {@link XYZ} of this
	 */
	public XYZ toXYZ() {
		return RThetaPhi.toCartesian(R, getTheta(), getPhi());
	}

	/**
	 * @param location
	 *            {@link Location} to compute distance with
	 * @return [km] one-line distance from the location
	 */
	public double getDistance(Location location) {
		return location.toXYZ().getDistance(toXYZ());
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(R);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		Location other = (Location) obj;
		if (Double.doubleToLongBits(R) != Double.doubleToLongBits(other.R))
			return false;
		return true;
	}

	/**
	 * @param locations
	 *          to be sorted. 
	 * @return locations in the order of the distance from this.
	 */
	public Location[] getNearestLocation(Location[] locations) {
		Location[] newLocations = locations.clone();
		Arrays.sort(newLocations, Comparator.comparingDouble(this::getDistance));
		return newLocations;
	}
	
	public Location[] getNearestHorizontalLocation(Location[] locations) {
		Location[] newLocations = Arrays.stream(locations)
				.filter(loc -> loc.getR() == this.getR())
				.collect(Collectors.toList())
				.toArray(new Location[0]);
		Arrays.sort(newLocations, Comparator.comparingDouble(this::getDistance));
		return newLocations;
	}

	public static double toLatitude(double theta) {
		return Latitude.toLatitude(theta);
	}

	@Override
	public String toString() {
		return super.toString() + ' ' + R;
	}
}
