package io.github.kensuke1984.kibrary.util;

import java.util.Arrays;
import java.util.Comparator;

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
 * @version 0.1.1.2
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
		return Double.compare(r,  ((Location) o).r);
	}

	/**
	 * 小数点4桁目を四捨五入
	 */
	private final double r;

	/**
	 * @param latitude
	 *            [deg] 地理緯度（度）
	 * @param longitude
	 *            [deg] 経度（度）
	 * @param r
	 *            [km] radius
	 */
	public Location(double latitude, double longitude, double r) {
		super(latitude, longitude);
		this.r = Precision.round(r, 3);
	}

	/**
	 * @return radius (not depth)
	 */
	public double getR() {
		return r;
	}

	/**
	 * 
	 * @return {@link RThetaPhi} of this
	 */
	public RThetaPhi getRThetaPhi() {
		return new RThetaPhi(r, getTheta(), getPhi());
	}

	/**
	 * Cartesian coordinate
	 * 
	 * @return {@link XYZ} of this
	 */
	public XYZ toXYZ() {
		return RThetaPhi.toCartesian(r, getTheta(), getPhi());
	}

	/**
	 * @param location
	 *            {@link Location} to compute distance with
	 * @return locationとの直線距離
	 */
	public double getDistance(Location location) {
		XYZ xyz0 = toXYZ();
		XYZ xyz = location.toXYZ();
		return xyz.getDistance(xyz0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(r);
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
		if (Double.doubleToLongBits(r) != Double.doubleToLongBits(other.r))
			return false;
		return true;
	}

	/**
	 * @param locations
	 *            並び替える元。 直接はいじらない
	 * @return locations をthisに近い順で並び替えて返す。
	 */
	public Location[] getNearestLocation(Location[] locations) {
		Location[] newLocations = locations.clone();
		Arrays.sort(newLocations, Comparator.comparingDouble(o -> o.getDistance(this)));
		return newLocations;
	}

	public static double toLatitude(double theta) {
		return Latitude.toLatitude(theta);
	}

	@Override
	public String toString() {
		return super.toString() + ' ' + r;
	}
}
