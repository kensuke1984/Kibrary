package io.github.kensuke1984.kibrary.util;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;

/**
 * Latitude [-90, 90] The value is rounded off to the fourth decimal position.
 *
 * This class is <b>IMMUTABLE</b>
 *
 * @author Kensuke Konishi
 * @version 0.0.6.3
 * @author anselme modify equals within epsilon
 */
class Latitude implements Comparable<Latitude> {

    /**
     * geographic latitude [deg] [-90, 90]
     */
    private double geographicLatitude;
    /**
     * geocentric latitude [deg] [-90, 90]
     */
    private double geocentricLatitude;
    /**
     * [0, &pi;] radian &theta; in spherical coordinates.
     */
    private double theta;
    private double inGeographicLatitude;
	/**
	 * epsilon to test equality within a range for this.latitude 
	 */
	private final double eps = 1e-4;

    /**
     * @param geographicLatitude [deg] [-90, 90]
     */
    Latitude(double geographicLatitude) {
        inGeographicLatitude = geographicLatitude;
        this.geographicLatitude = Precision.round(geographicLatitude, 4);
        if (!checkLatitude(geographicLatitude)) throw new IllegalArgumentException(
                "The input latitude: " + geographicLatitude + " is invalid (must be [-90, 90]).");
        geocentricLatitude = Earth.toGeocentric(FastMath.toRadians(geographicLatitude));
        theta = 0.5 * Math.PI - geocentricLatitude;
    }

    /**
     * check if the latitude works for this class
     *
     * @param latitude [deg]
     * @return if the latitude is valid
     */
    private static boolean checkLatitude(double latitude) {
        return -90 <= latitude && latitude <= 90;
    }

    /**
     * @param theta [rad] spherical coordinates [0, &pi;]
     * @return geographic latitude [deg]
     */
    static double toLatitude(double theta) {
        if (theta < 0 || Math.PI < theta) throw new IllegalArgumentException(
                "Invalid theta(must be[0, pi]): " + theta + " @" +
                        Thread.currentThread().getStackTrace()[1].getMethodName());

        double geocentric = 0.5 * Math.PI - theta;
        return FastMath.toDegrees(Earth.toGeographical(geocentric));
    }

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

    /**
     *@author anselme compare within eps
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Latitude other = (Latitude) obj;
        // return Double.doubleToLongBits(geographicLatitude) == Double.doubleToLongBits(other.geographicLatitude);
        return Utilities.equalWithinEpsilon(geographicLatitude, other.geographicLatitude, eps);
    }

    public double getInGeographicLatitude() {
        return inGeographicLatitude;
    }

    /**
     * @return geographic latitude [deg]
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
	
}
