package io.github.kensuke1984.kibrary.util;

import io.github.kensuke1984.kibrary.math.geometry.Ellipse;
import io.github.kensuke1984.kibrary.math.geometry.Point2D;
import io.github.kensuke1984.kibrary.math.geometry.RThetaPhi;
import io.github.kensuke1984.kibrary.math.geometry.XYZ;
import org.apache.commons.math3.util.FastMath;

/**
 * <p>
 * Position
 * </p>
 * Latitude Longitude
 * <p>
 * This class is <b>immutable</b>.
 * </p>
 *
 * @author Kensuke Konishi
 * @version 0.1.1.3
 */
public class HorizontalPosition implements Comparable<HorizontalPosition> {

    private final Latitude latitude;
    private final Longitude longitude;

    /**
     * Creates an instance with geographic latitude and longitude
     *
     * @param latitude  [deg] geographic latitude [-90, 90] {@link Location#latitude}
     * @param longitude [deg] (-180, 360)
     */
    public HorizontalPosition(double latitude, double longitude) {
        this.latitude = new Latitude(latitude);
        this.longitude = new Longitude(longitude);
    }

    /**
     * Sorting order is Latitude &rarr; Longitude
     */
    @Override
    public int compareTo(HorizontalPosition o) {
        int lat = latitude.compareTo(o.latitude);
        return lat != 0 ? lat : longitude.compareTo(o.longitude);
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + latitude.hashCode();
        result = prime * result + longitude.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        HorizontalPosition other = (HorizontalPosition) obj;
        return latitude.equals(other.latitude) && longitude.equals(other.longitude);
    }

    /**
     * @param r [km] radius
     * @return Location newly created with the input r (deep copy)
     */
    public Location toLocation(double r) {
        return new Location(latitude.getLatitude(), longitude.getLongitude(), r);
    }

    /**
     * @param position {@link HorizontalPosition} to compute azimuth with
     * @return locationとのazimuth [rad]
     */
    public double getAzimuth(HorizontalPosition position) {
        return Earth.getAzimuth(this, position);
    }

    /**
     * @param position {@link HorizontalPosition} to compute back azimuth with
     * @return locationとのback azimuth [rad]
     */
    public double getBackAzimuth(HorizontalPosition position) {
        return Earth.getBackAzimuth(this, position);
    }

    /**
     * @return geographic latitude [deg] [-90, 90]
     */
    public double getLatitude() {
        return latitude.getLatitude();
    }

    /**
     * (-180:180]
     *
     * @return geographic longitude [deg]
     */
    public double getLongitude() {
        return longitude.getLongitude();
    }

    /**
     * &theta; in spherical coordinates
     *
     * @return &theta; [rad] in spherical coordinate.
     */
    public double getTheta() {
        return latitude.getTheta();
    }

    /**
     * &phi; in spherical coordinate
     *
     * @return phi [rad] in spherical coordinate.
     */
    public double getPhi() {
        return longitude.getPhi();
    }

    /**
     * @param horizontalPosition {@link HorizontalPosition}
     * @return epicentral distance [rad] between this and horizontalPosition
     */
    public double getEpicentralDistance(HorizontalPosition horizontalPosition) {
        return Earth.getEpicentralDistance(horizontalPosition, this);
    }

    /**
     * 元点loc0と入力locとの大円上の中点を求める 半径は考慮しない locとloc0のなす震央距離を⊿
     * loc0を北極に持って行ったときのlocの経度をphi1 とすると、点(r, ⊿/2, 0)
     * をｚ軸周りにphi１回転して、loc0を北極から元の位置に戻す作業をすればいい
     *
     * @param position {@link HorizontalPosition} of target
     * @return {@link HorizontalPosition} of the center between the position and
     * this
     */
    public HorizontalPosition getMidpoint(HorizontalPosition position) {
        double delta = getEpicentralDistance(position); // locとthis との震央距離
        // System.out.println("delta: " + delta);
        // theta = ⊿/2の zx平面上の点
        XYZ midXYZ = new RThetaPhi(1, delta * 0.5, 0).toCartesian();
        // locの点
        XYZ locXYZ = position.toXYZ(Earth.EARTH_RADIUS);
        // thisをzx面上に戻したときのloc
        locXYZ = locXYZ.rotateaboutZ(-1 * getPhi());
        // loc0を北極に
        locXYZ = locXYZ.rotateaboutY(-1 * getTheta());
        RThetaPhi locRTP = locXYZ.toSphericalCoordinate();
        // その時の phi1
        double phi1 = locRTP.getPhi();
        // System.out.println("phi1 " + phi1);
        midXYZ = midXYZ.rotateaboutZ(phi1);
        midXYZ = midXYZ.rotateaboutY(getTheta());
        midXYZ = midXYZ.rotateaboutZ(getPhi());
        RThetaPhi midRTP = midXYZ.toSphericalCoordinate();
        // System.out.println(midRTP);
        return new HorizontalPosition(Latitude.toLatitude(midRTP.getTheta()), FastMath.toDegrees(midRTP.getPhi()));
        // System.out.println(midLoc);
    }

    /**
     * @param r radius
     * @return {@link XYZ} at radius ｒ
     */
    public XYZ toXYZ(double r) {
        return RThetaPhi.toCartesian(r, getTheta(), getPhi());
    }

    @Override
    public String toString() {
        return latitude.getLatitude() + " " + longitude.getLongitude();
    }

    /**
     * @return geocentric latitude [rad]
     */
    public double getGeocentricLatitude() {
        return latitude.getGeocentricLatitude();
    }

    /**
     * d = 2・N・ψ
     * <p>
     * ここに， 地点1の緯度φ1，経度λ1，地点2の緯度φ2，経度λ2のときの直交座標地を それぞれ （x1，y1，z1），（x2，y2，z2）
     * とすると2地点間の直距離 Rn は
     * <p>
     * A = 6378140m（地球赤道半径），B = 6356755m（地球極半径），ｅ**2 = (A**2 - B**2)/A**2 e：離心率
     * <p>
     * N1 = A/sqrt(1 - e**2・sin**2φ1)， N_2 = A/sqrt(1 - e2・sin**2φ2) x1 =
     * N_1・cosφ1cosλ_1， x2 = N_2・cosφ_2cosλ_2 y1 = N_1・cosφ1sinλ_1， y2 =
     * N_2・cosφ_2sinλ_2 z1 = N_1・(1 - e**2)sinφ_1， z2 = N_2・(1 - e**2)sinφ_2
     * <p>
     * 中心から2地点を見込んだ中心角の半分の角 ψ は
     * <p>
     * ψ = sin-1( (Rn/2)/N )， N = (N1 + N2)/2
     * <p>
     * ただし，計算に使用した緯度・経度はラジアンに変換。標高は無視して計算。 TODO ずれる
     *
     * @param position {@link HorizontalPosition} of target
     * @return 大円上での距離 精度はそこまでよくない
     */
    public double getPath(HorizontalPosition position) {
        double distance;
        Ellipse e = new Ellipse(Earth.EQUATORIAL_RADIUS, Earth.POLAR_RADIUS);

        double r0 = e.toR(0.5 * Math.PI - getTheta());
        double r1 = e.toR(0.5 * Math.PI - position.getTheta());

        // System.out.println(a + " " + Earth.EQUATORIAL_RADIUS);

        XYZ xyz0 = toXYZ(r0);
        XYZ xyz = position.toXYZ(r1);
        // System.out.println(xyz0+" \n"+xyz);
        double r = xyz.getDistance(xyz0);

        double n1 = Earth.EQUATORIAL_RADIUS / FastMath.sqrt(1 -
                Earth.E * Earth.E * FastMath.sin(FastMath.toRadians(getLatitude())) *
                        FastMath.sin(FastMath.toRadians(getLatitude())));
        // System.out.println(n1 + " " + N1);
        double n2 = Earth.EQUATORIAL_RADIUS / FastMath.sqrt(1 -
                Earth.E * Earth.E * FastMath.sin(FastMath.toRadians(position.getLatitude())) *
                        FastMath.sin(FastMath.toRadians(position.getLatitude())));
        double n = (n1 + n2) / 2;
        double kai = FastMath.asin(r / 2 / n);
        distance = 2 * kai * n;
        return distance;
    }

    /**
     * @return Point2D of this
     */
    public Point2D toPoint2D() {
        return new Point2D(getLongitude(), getLatitude());
    }
	
	/**
	 * @param azimuth
	 * @param distance
	 * @return HorizontalPosition located at distance from self along azimuth
	 * @author anselme
	 */
	public HorizontalPosition fromAzimuth(double azimuth, double distance) {
		double alpha = azimuth * Math.PI / 180;
		double GCARC = distance * Math.PI / 180;
		
		double costheta = Math.cos(GCARC) * Math.cos(getTheta())
				+ Math.sin(GCARC) * Math.sin(getTheta()) * Math.cos(alpha);
		double sintheta = Math.sqrt(1 - costheta * costheta);
		
		double theta = Math.acos(costheta);
		
		double tmpCos = Math.min( (Math.cos(GCARC) - Math.cos(getTheta()) * costheta)
					/ (Math.sin(getTheta()) * sintheta)
				, 1.);
		tmpCos = Math.max(tmpCos, -1.);
		
		double phi = getPhi() - Math.acos( tmpCos );
		
		double lat = 90 - theta * 180 / Math.PI;
		double lon = phi * 180 / Math.PI;
		
		if (lon < -180)
			lon = lon + 360;
		
		return new HorizontalPosition(lat, lon);
	}
	
	public double getGeographicalAzimuth(HorizontalPosition position) {
		return Earth.getGeographicalAzimuth(this, position);
	}
	
	public double getGeographicalDistance(HorizontalPosition horizontalPosition) {
		return Earth.getGeographicalDistance(horizontalPosition, this);
	}

}
