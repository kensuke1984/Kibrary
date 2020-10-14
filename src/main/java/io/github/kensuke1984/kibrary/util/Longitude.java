package io.github.kensuke1984.kibrary.util;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;

/**
 * Longitude (-180, 180]
 * If you input 200, then the value is considered -160.
 * The value is rounded off to the 4th decimal position.
 * This class is <b>IMMUTABLE</b>
 *
 * @author Kensuke Konishi
 * @version 0.1.0.4
 */
class Longitude implements Comparable<Longitude> {
    /**
     * input value [-180, 360) [deg]
     */
    private final double INPUT_LONGITUDE;

    /**
     * (-180, 180] geographic longitude [deg]
     */
    private double longitude;

    /**
     * [0, 2*&pi;) &phi; in spherical coordinates [rad]
     */
    private double phi;
	/**
	 * epsilon to test equality within a range for this.longitude 
	 */
	private final double eps = 1e-4;

    /**
     * @param longitude [deg] [-180, 360)
     */
    Longitude(double longitude) {
        if (!checkLongitude(longitude)) throw new IllegalArgumentException(
                "The input longitude: " + longitude + " is invalid (must be [-180, 360).");
        INPUT_LONGITUDE = longitude;
        if (180 < longitude) {
            phi = FastMath.toRadians(longitude - 360);
            this.longitude = -360 + longitude;
        } else {
            phi = FastMath.toRadians(longitude);
            this.longitude = longitude;
        }
        this.longitude = Precision.round(this.longitude, 4);
        phi = Precision.round(phi, 4);
    }

    /**
     * check if the longitude is [-180, 360)
     *
     * @param longitude [deg]
     * @return if the longitude is valid
     */
    private static boolean checkLongitude(double longitude) {
        return -180 <= longitude && longitude < 360;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
//		long temp;
//		temp = Double.doubleToLongBits(longitude);
//		result = prime * result + (int) (temp ^ (temp >>> 32));
		int temp = (int) longitude;
		result = prime * result + temp;
		return result;
	}

    /**
     *@author anselme equals within epsilon
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Longitude other = (Longitude) obj;
//        return Double.doubleToLongBits(longitude) == Double.doubleToLongBits(other.longitude);
        return Utilities.equalWithinEpsilon(longitude, other.longitude, eps);
    }

    /**
     * (-180, 180]
     *
     * @return longitude [deg]
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * [0, 2*&pi;)
     *
     * @return &phi; [rad]
     */
    public double getPhi() {
        return phi;
    }

    /**
     * @return raw value input to constructor
     */
    public double getValue() {
        return INPUT_LONGITUDE;
    }

    @Override
    public int compareTo(Longitude o) {
        return Double.compare(longitude, o.longitude);
    }

}
