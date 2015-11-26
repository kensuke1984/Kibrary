package manhattan.template;


import org.apache.commons.math3.util.FastMath;

/**
 * 緯度 [-90, 90] 小数点5桁以下を四捨五入
 * 
 * <p>
 * This class is <b>IMMUTABLE</b>
 * </p>
 * 
 * @version 0.0.2
 * @since 2014/1/12
 * 
 * @version 0.0.3
 * @since 2014/8/12 default に
 * 
 * 
 * @version 0.0.5
 * @since 2014/11/14 Math -> FastMath
 * 
 * @version 0.0.5.1
 * @since 2015/4/14
 * 
 * @version 0.0.6
 * @since 2015/8/18 {@link IllegalArgumentException}
 * 
 * 
 * @author kensuke
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
		long temp;
		temp = Double.doubleToLongBits(geographicLatitude);
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
		Latitude other = (Latitude) obj;
		if (Double.doubleToLongBits(geographicLatitude) != Double.doubleToLongBits(other.geographicLatitude))
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
	 * @param 地理緯度
	 *            [deg] geographicLatitude [-90, 90]
	 */
	Latitude(double geographicLatitude) {
		inGeographicLatitude = geographicLatitude;
		this.geographicLatitude = geographicLatitude;
		adjust();
		if (!checkLatitude(geographicLatitude))
			throw new IllegalArgumentException(
					"The input latitude: " + geographicLatitude + " is invalid (must be [-90, 90]).");
		// this.geographicLatitude=geographicLatitude;
		this.geocentricLatitude = Earth.toGeocentric(FastMath.toRadians(geographicLatitude));
		this.theta = 0.5 * Math.PI - geocentricLatitude;
	}

	public double getInGeographicLatitude() {
		return inGeographicLatitude;
	}

	private void adjust() {
		geographicLatitude = FastMath.round(geographicLatitude * 10000) / 10000.0;
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
	 *            [rad] spherical coordinates 球座標 [0, pi]
	 * @return 地理緯度(度）
	 */
	static double toLatitude(double theta) {
		if (theta < 0 || Math.PI < theta) {
			System.out.println("Invalid theta(must be[0, pi]): " + theta + " @"
					+ Thread.currentThread().getStackTrace()[1].getMethodName());
		}

		double geocentric = 0.5 * Math.PI - theta;
		return FastMath.toDegrees(Earth.toGeographical(geocentric));
	}

}
