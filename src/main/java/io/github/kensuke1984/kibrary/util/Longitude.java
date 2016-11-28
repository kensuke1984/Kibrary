package io.github.kensuke1984.kibrary.util;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;

/**
 * Longitude (-180, 180]
 * 
 * If you input 200, then the value is considered -160.
 * <p>
 * The value is rounded off to the 4th decimal position.
 * 
 * <p>
 * This class is <b>IMMUTABLE</b>
 * </p>
 * 
 * @author Kensuke Konishi
 * 
 * @version 0.1.0.2
 * 
 */
class Longitude implements Comparable<Longitude> {
	/**
	 * inputされた値 [-180, 360)
	 */
	private double inLongitude;

	/**
	 * (-180, 180] the geographic longitude. 計算等に使う値
	 */
	private float longitude;

	/**
	 * [0, 2*pi) φ in spherical coordinates.
	 */
	private double phi;
	
	/**
	 * epsilon to test equality within a range for this.longitude 
	 */
	private final float eps = 0.001f;

	/**
	 * 
	 * @param longitude
	 *            [deg] [-180, 360)
	 *            The longitude is rounded to 2 decimal places
	 */
	Longitude(double longitude) {
		if (!checkLongitude(longitude))
			throw new IllegalArgumentException(
					"The input longitude: " + longitude + " is invalid (must be [-180, 360).");
		inLongitude = longitude;
		if (180 < longitude) {
			phi = FastMath.toRadians(longitude - 360);
			this.longitude = -360f + (float) longitude;
		} else {
			phi = FastMath.toRadians(longitude);
			this.longitude = (float) longitude;
		}
	}

	/**
	 * check if the longitude works for this class [-180, 360)
	 * 
	 * @param longitude
	 * @return true or false
	 */
	private static boolean checkLongitude(double longitude) {
		return -180 <= longitude && longitude < 360;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(longitude);
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
		Longitude other = (Longitude) obj;
		if (Utilities.equalWithinEpsilon(longitude, other.longitude, eps))
			return false;
		return true;
	}
	
	/**
	 * (-180, 180]
	 * 
	 * @return 緯度（度）
	 */
	public float getLongitude() {
		return longitude;
	}

	/**
	 * [0, 2*pi)
	 * 
	 * @return φ(rad)
	 */
	public double getPhi() {
		return phi;
	}

	/**
	 * @return コンストラクタに代入された値
	 */
	public double getValue() {
		return inLongitude;
	}

	@Override
	public int compareTo(Longitude o) {
		return Double.compare(longitude, o.longitude);
	}

}
