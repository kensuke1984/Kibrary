package io.github.kensuke1984.kibrary.util;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;

/**
 * Latitude [-90, 90] The value is rounded off to the fourth decimal position.
 * 
 * <p>
 * This class is <b>IMMUTABLE</b>
 * </p>
 * 
 * @version 0.0.6.2
 * 
 * @author Kensuke Konishi
 * 
 */
class Latitude implements Comparable<Latitude> {

	@Override
	public int compareTo(Latitude o) {
		return Double.compare(geographicLatitude, o.geographicLatitude);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
//		long temp;
//		temp = Double.doubleToLongBits(geographicLatitude);
//		result = prime * result + (int) (temp ^ (temp >>> 32));
		int temp = (int) (geographicLatitude);
		result = prime * result + temp;
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
		Latitude other = (Latitude) obj;
		if (Utilities.equalWithinEpsilon(geographicLatitude, other.geographicLatitude, eps))
			return false;
		return true;
	}

	/**
	 * Geographic latitude 地理緯度 [-90, 90]
	 */
	private double geographicLatitude;

	/**
	 * 地心緯度 [-90, 90]
	 */
	private double geocentricLatitude;

	/**
	 * [0, pi] radian θ in spherical coordinates.
	 */
	private double theta;

	private double inGeographicLatitude;
	
	/**
	 * epsilon to test equality within a range for this.latitude 
	 */
	private final double eps = 1e-4;

	/**
	 * @param 地理緯度
	 *            [deg] geographicLatitude [-90, 90]
	 *            The geographical latitude is rounded to 4 decimal places
	 */
	Latitude(double geographicLatitude) {
		inGeographicLatitude = geographicLatitude;
		this.geographicLatitude = inGeographicLatitude;
		if (!checkLatitude(geographicLatitude))
			throw new IllegalArgumentException(
					"The input latitude: " + geographicLatitude + " is invalid (must be [-90, 90]).");
		geocentricLatitude = Earth.toGeocentric(FastMath.toRadians(geographicLatitude));
		theta = 0.5 * Math.PI - geocentricLatitude;
	}

	public double getInGeographicLatitude() {
		return inGeographicLatitude;
	}

	/**
	 * check if the latitude works for this class
	 * 
	 * @param latitude
	 * @return true or false
	 */
	private static boolean checkLatitude(double latitude) {
		return -90 <= latitude && latitude <= 90;
	}

	/**
	 * @return geographic latitude [deg] 地理緯度
	 */
	public double getLatitude() {
		return geographicLatitude;
	}

	/**
	 * @return geocentric latitude [rad]
	 */
	public double getGeocentricLatitude() {
		return geocentricLatitude;
	}

	/**
	 * @return theta [radian]
	 */
	public double getTheta() {
		return theta;
	}

	/**
	 * @param theta
	 *            [rad] spherical coordinates 球座標 [0, &pi;]
	 * @return geographic latitude [deg] 地理緯度(度）
	 */
	static double toLatitude(double theta) {
		if (theta < 0 || Math.PI < theta)
			throw new IllegalArgumentException("Invalid theta(must be[0, pi]): " + theta + " @"
					+ Thread.currentThread().getStackTrace()[1].getMethodName());

		double geocentric = 0.5 * Math.PI - theta;
		return FastMath.toDegrees(Earth.toGeographical(geocentric));
	}

}
