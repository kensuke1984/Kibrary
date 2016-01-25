package io.github.kensuke1984.kibrary.util;

import org.apache.commons.math3.util.FastMath;

/**
 * Longitude (-180, 180]
 * 
 * If you input 200, then the value is considered -160. The value is rounded off
 * to the 4th decimal position.
 * 
 * <p>
 * This class is <b>IMMUTABLE</b>
 * </p>
 * 
 * 
 * @author Kensuke
 * 
 * @version 0.1.0
 * 
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
	private double longitude;

	/**
	 * [0, 2*pi) φ in spherical coordinates.
	 */
	private double phi;

	/**
	 * 
	 * @param longitude
	 *            [deg] [-180, 360)
	 */
	Longitude(double longitude) {
		if (!checkLongitude(longitude))
			throw new IllegalArgumentException(
					"The input longitude: " + longitude + " is invalid (must be [-180, 360).");
		// longitude = FastMath.round(longitude*10000)/10000.0;
		this.inLongitude = longitude;
		if (180 < longitude) {
			this.phi = FastMath.toRadians(longitude - 360);
			this.longitude = -360 + longitude;
		} else {
			this.phi = FastMath.toRadians(longitude);
			this.longitude = longitude;
		}
		adjust();
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
		if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
			return false;
		return true;
	}

	/**
	 * (-180, 180]
	 * 
	 * @return 緯度（度）
	 */
	public double getLongitude() {
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

	private void adjust() {
		longitude = FastMath.round(longitude * 10000) / 10000.0;
		phi = FastMath.round(phi * 10000) / 10000.0;
	}

	@Override
	public int compareTo(Longitude o) {
		return Double.compare(longitude, o.longitude);
	}

}
