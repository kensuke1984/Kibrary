package io.github.kensuke1984.kibrary.util;

import io.github.kensuke1984.kibrary.math.geometry.Ellipse;
import org.apache.commons.math3.util.FastMath;

import java.util.Arrays;

/**
 * Earth utility.
 *
 * @author Kensuke Konishi
 * @version 0.1.2
 */
public final class Earth {
    /**
     * [km] Earth radius
     */
    public final static double EARTH_RADIUS = 6371;
    /**
     * [km] Equatorial radius
     */
    public final static double EQUATORIAL_RADIUS = 6378.137;
    /**
     * [km] Polar radius
     */
    public final static double POLAR_RADIUS = 6356.752314140356;
    /**
     * (1st) eccentricity
     */
    public final static double E = 0.08181919104281514;
    /**
     * flattening
     */
    public final static double FLATTENING = 1 / 298.257223563;
    /**
     * tire profile
     */
    public final static double N = 0.0016792443125758178;

    private Earth() {
    }

    /**
     * Compute length between points on lower and upper latitudes in the same
     * meridian. 。
     *
     * @param lowerLatitude [deg] geographic latitude
     * @param upperLatitude [deg] geographic latitude
     * @return [km] length of meridional part on the surface.
     */
    public static double getMeridionalParts(double lowerLatitude, double upperLatitude) {
        if (upperLatitude < lowerLatitude || lowerLatitude < -90 || 90 < lowerLatitude || upperLatitude < -90 ||
                90 < upperLatitude) throw new IllegalArgumentException(
                "input latitudes lower, upper: " + lowerLatitude + ", " + upperLatitude + " are invalid.");

        if (0 <= lowerLatitude) return getMeridionalParts(upperLatitude) - getMeridionalParts(lowerLatitude);
        else if (upperLatitude < 0) return getMeridionalParts(lowerLatitude) - getMeridionalParts(upperLatitude);
        return getMeridionalParts(lowerLatitude) + getMeridionalParts(upperLatitude);
    }

    /**
     * Compute an radius of a position on the surface after oval consideration
     *
     * @param position {@link HorizontalPosition} of a target point
     * @return [km] revised radius of the position after oval consideration
     */
    public static double getR(HorizontalPosition position) {
        double theta = position.getGeocentricLatitude();
        double r = 1 / (FastMath.cos(theta) * FastMath.cos(theta) / EQUATORIAL_RADIUS / EQUATORIAL_RADIUS +
                FastMath.sin(theta) * FastMath.sin(theta) / POLAR_RADIUS / POLAR_RADIUS);
        return FastMath.sqrt(r);
    }

    /**
     * Compute a length of major axis of similar oval, on which the location
     * exists, to the Earth
     *
     * @param location {@link Location} of a target point
     * @return [km] length of a major axis of the oval, similar to Earth, where the location exists.
     */
    public static double getExtendedShaft(Location location) {
        if (location.getR() == 0) {
            System.err.println("location has no radius information, the extended shaft for the surface is returned.");
            return EQUATORIAL_RADIUS;
        }
        double r2 = location.getR() * location.getR();
        double theta = location.getGeocentricLatitude();
        return FastMath.sqrt(r2 * FastMath.cos(theta) * FastMath.cos(theta) +
                r2 * FastMath.sin(theta) * FastMath.sin(theta) / (1 - E * E));
    }

    /**
     * Compute a distance along a meridian between the equator and
     * an input latitude.
     *
     * @param latitude [deg] geographic latitude [-90, 90]
     * @return [km] distance along the meridian between the equator and latitude.
     */
    private static double getMeridionalParts(double latitude) {
        if (latitude < -90 || 90 < latitude)
            throw new IllegalArgumentException("input latitude: " + latitude + " is invalid.");
        return getSUMforMeridionalParts(latitude) * EQUATORIAL_RADIUS;
    }

    /**
     * 第三扁平率を用いて子午線弧の近似値を求める際のべき級数の和（長軸によらない部分）
     * ある長軸aにおける弧の長さを求めるには aをかけなければならない
     *
     * @param latitude [-90, 90] geographical latitude
     * @return 長軸１ｋｍの時の 赤道から入力された緯度までの子午線弧の長さ
     */
    private static double getSUMforMeridionalParts(double latitude) {
        if (latitude < -90 || 90 < latitude)
            throw new IllegalArgumentException("input latitude: " + latitude + " is invalid.");
        double s = 0; // length
        // double N=0;
        double n2 = N * N;
        double n3 = N * n2;
        double n4 = n2 * n2;
        double phi = FastMath.toRadians(latitude);
        if (phi < 0) phi *= -1;

        s += (1 + n2 / 4 + n4 / 64) * phi;
        s += -1.5 * (N - n3 / 8) * FastMath.sin(phi * 2);
        s += 15 / 16.0 * (n2 - n4 / 4) * FastMath.sin(4 * phi);
        s += -35 / 48.0 * n3 * FastMath.sin(6 * phi);
        s += 315 / 512.0 * n4 * FastMath.sin(8 * phi);
        s /= 1 + N;
        return s;
    }

    /**
     * Compute epicentral distance between pos1 and pos2
     *
     * @param pos1 {@link HorizontalPosition} of a point
     * @param pos2 {@link HorizontalPosition} of a point
     * @return [rad] epicentral distance between pos1 and pos2
     */
    public static double getEpicentralDistance(HorizontalPosition pos1, HorizontalPosition pos2) {

        double theta1 = pos1.getTheta();
        double theta2 = pos2.getTheta();
        double phi1 = pos1.getPhi();
        double phi2 = pos2.getPhi();
        // cos a = a*b/|a|/|b|
        double cosAlpha = FastMath.sin(theta1) * FastMath.sin(theta2) * FastMath.cos(phi1 - phi2) +
                FastMath.cos(theta1) * FastMath.cos(theta2);
        return FastMath.acos(cosAlpha);
    }

    /**
     * @param eq      {@link HorizontalPosition} of source
     * @param station {@link HorizontalPosition} of station
     * @return [rad] azimuth of the station from the eq
     */
    public static double getAzimuth(HorizontalPosition eq, HorizontalPosition station) {
        double e = eq.getTheta();
        double s = station.getTheta();
        // System.out.println("eq:"+e+" station: "+s);
        double deltaPhi = -eq.getPhi() + station.getPhi();
        double delta = getEpicentralDistance(eq, station);
        double cos = (FastMath.cos(s) * FastMath.sin(e) - FastMath.sin(s) * FastMath.cos(e) * FastMath.cos(deltaPhi)) /
                FastMath.sin(delta);
        if (1 < cos) cos = 1;
        else if (cos < -1) cos = -1;
        double sin = FastMath.sin(s) * FastMath.sin(deltaPhi) / FastMath.sin(delta);
        double az = FastMath.acos(cos);
        return 0 <= sin ? az : -az + 2 * Math.PI;
    }

    /**
     * @param sourcePos   {@link HorizontalPosition} of a source
     * @param receiverPos {@link HorizontalPosition} of a receiver
     * @return [rad] Back azimuth of the receiver from the source
     */
    public static double getBackAzimuth(HorizontalPosition sourcePos, HorizontalPosition receiverPos) {
        double e = sourcePos.getTheta();
        double s = receiverPos.getTheta();
        double deltaPhi = sourcePos.getPhi() - receiverPos.getPhi();
        double delta = getEpicentralDistance(sourcePos, receiverPos);
        double cos = (FastMath.cos(e) * FastMath.sin(s) - FastMath.sin(e) * FastMath.cos(s) * FastMath.cos(deltaPhi)) /
                FastMath.sin(delta);
        double sin = FastMath.sin(e) * FastMath.sin(deltaPhi) / FastMath.sin(delta);
        double az = FastMath.acos(cos);
        return 0 <= sin ? az : -az + 2 * Math.PI;
    }

    /**
     * Transform a geographic latitude to geocentric.
     *
     * @param geographical [rad] geographic latitude [-&pi;/2, &pi;/2]
     * @return [rad] geocentric latitude [-&pi;/2, &pi;/2]
     */
    static double toGeocentric(double geographical) {
        if (0.5 * Math.PI < Math.abs(geographical))
            throw new IllegalArgumentException("geographical latitude: " + geographical + " must be [-pi/2, pi/2].");
        double ratio = POLAR_RADIUS / EQUATORIAL_RADIUS;
        return FastMath.atan(ratio * ratio * FastMath.tan(geographical));
    }

    /**
     * Transform a geocentric latitude to geographic.
     *
     * @param geocentric [rad] geocentric latitude
     * @return geographical [rad] geographic latitude
     */
    static double toGeographical(double geocentric) {
        double ratio = EQUATORIAL_RADIUS / POLAR_RADIUS;
        return FastMath.atan(ratio * ratio * FastMath.tan(geocentric));
    }

    /**
     * 長軸a 緯度 経度 に囲まれた領域の体積を求める<br>
     *
     * @param startA         [km] start of major axis [0,endA)
     * @param endA           [km] end of major axis (startA, ∞]
     * @param startLatitude  [deg] [-90, endLatitude]
     * @param endLatitude    [deg] [startLatitude, 90]
     * @param startLongitude [deg] [-180, endLongitude]
     * @param endLongitude   [deg] [startLongitude, 180]
     * @return 長軸startAからendAまでの楕円弧 緯度 経度 に囲まれた領域の体積
     */
    public static double getVolume(double startA, double endA, double startLatitude, double endLatitude,
                                   double startLongitude, double endLongitude) {
        // radius
        if (endA <= startA || startA < 0)
            throw new IllegalArgumentException("endA: " + endA + " must be bigger than startA: " + startA);

        if (startLongitude < 0 && 180 < endLongitude) throw new IllegalArgumentException(
                "startLongitude: " + startLongitude + ". endLongitude: " + endLongitude +
                        ". Input region must be [0:360] or [-180:180].");

        // //latitude
        if (endLatitude <= startLatitude || startLatitude < -90 || 90 < endLatitude) throw new IllegalArgumentException(
                "startLatitude: " + startLatitude + " must be [-90, endLatitude]. endLatitude: " + endLatitude +
                        " must be [startLatitude, 90].");
        // longitude
        if (startLongitude < -180 || 360 < endLongitude || endLongitude <= startLongitude)
            throw new IllegalArgumentException(
                    "startLongitude: " + startLongitude + " must be [-180, endLongitude). endLongitude: " +
                            endLongitude + " must be (startLongitude, 360].");

        // double dr =1;
        double dr = (endA - startA) * 0.01;
        double dLatitude = (endLatitude - startLatitude) * 0.01;
        int nr = (int) ((endA - startA) / dr) + 1;
        double[] rs = new double[nr];
        Arrays.setAll(rs, i -> startA + i * dr);
        if (startA == 0) rs[0] = 1e-8; // TODO どうするか
        rs[nr - 1] = endA;

        int nLatitude = (int) ((endLatitude - startLatitude) / dLatitude) + 1;
        double[] latitudes = new double[nLatitude];
        Arrays.setAll(latitudes, i -> startLatitude + i * dLatitude);
        latitudes[nLatitude - 1] = endLatitude;

        double v = 0;
        double dPhi = FastMath.toRadians(endLongitude - startLongitude);
        for (int ir = 0; ir < nr - 1; ir++)
            for (int iLatitude = 0; iLatitude < nLatitude - 1; iLatitude++)
                v += rs[ir] * Math.cos(toGeocentric(FastMath.toRadians(latitudes[iLatitude]))) * dPhi *
                        (getCrossSection(rs[ir], rs[ir + 1], latitudes[iLatitude], latitudes[iLatitude + 1]));
        return v;
    }

    /**
     * Compute volume with in an input range.
     * Note that, for instance, the range is [point-0.5*dX, point+0.5*dX]
     *
     * @param point      center location
     * @param dr         [km] radius
     * @param dLatitude  [deg] in geographical latitude
     * @param dLongitude [deg]
     * @return volume [km<sup>3</sup>]
     */
    public static double getVolume(Location point, double dr, double dLatitude, double dLongitude) {
        double r = point.getR();
        if (r <= 0) throw new IllegalArgumentException("location has an invalid R: " + r);

        double latitude = point.getLatitude();
        double longitude = point.getLongitude();
        double startA = getExtendedShaft(point.toLocation(r - 0.5 * dr));
        double endA = getExtendedShaft(point.toLocation(r + 0.5 * dr));
        return getVolume(startA, endA, latitude - 0.5 * dLatitude, latitude + 0.5 * dLatitude,
                longitude - 0.5 * dLongitude, longitude + 0.5 * dLongitude);
    }

    /**
     * @param startA        geographic latitude [0, endA)
     * @param endA          geographic latitude (startA, ∞]
     * @param startLatitude [-90, endLatitude]
     * @param endLatitude   [startLatitude, 90]
     * @return 長径がstartAからendAまでの楕円上のstartLatitudeからendLatitudeまでの断面積
     */
    public static double getCrossSection(double startA, double endA, double startLatitude, double endLatitude) {
        if (endA < startA || startA < 0)
            throw new IllegalArgumentException("endA: " + endA + " must be bigger than startA: " + startA);
        if (endLatitude < startLatitude || startLatitude < -90 || 90 < endLatitude) throw new IllegalArgumentException(
                "startLatitude: " + startLatitude + " must be [-90, endLatitude]. endLatitude: " + endLatitude +
                        " must be [startLatitude, 90].");

        Ellipse el0 = new Ellipse(startA, startA - startA * FLATTENING);
        Ellipse el1 = new Ellipse(endA, endA - endA * FLATTENING);
        double theta0 = toGeocentric(FastMath.toRadians(startLatitude));
        double theta1 = toGeocentric(FastMath.toRadians(endLatitude));
        if (theta0 < 0) {
            theta0 += Math.PI;
            theta1 += Math.PI;
        }
        double s0 = el0.getS(theta0, theta1);
        double s1 = el1.getS(theta0, theta1);
        return s1 - s0;
    }
    
	/**
	 * @param eq
	 * @param station
	 * @return
	 * @author anselme
	 */
	public static double getGeographicalAzimuth(HorizontalPosition eq, HorizontalPosition station) {
		double e = (90. - eq.getLatitude()) * Math.PI / 180.;
		double s = (90. - station.getLatitude()) * Math.PI / 180.;
		// System.out.println("eq:"+e+" station: "+s);
		double deltaPhi = -eq.getPhi() + station.getPhi();
		double delta = getGeographicalDistance(eq, station);
		double cos = (FastMath.cos(s) * FastMath.sin(e) - FastMath.sin(s) * FastMath.cos(e) * FastMath.cos(deltaPhi))
				/ FastMath.sin(delta);
		if (1 < cos)
			cos = 1;
		else if (cos < -1)
			cos = -1;
		double sin = FastMath.sin(s) * FastMath.sin(deltaPhi) / FastMath.sin(delta);
		double az = FastMath.acos(cos);
		// System.out.println(cos+" "+az);
		// System.out.println(az*180/Math.PI);
		return 0 <= sin ? az : -az + 2 * Math.PI;
	}
	
	/**
	 * @param loc1
	 * @param loc2
	 * @return
	 * @author anselme
	 */
	public static double getGeographicalDistance(HorizontalPosition loc1, HorizontalPosition loc2) {
		double theta1 = (90. - loc1.getLatitude()) * Math.PI / 180.;
		double theta2 = (90. - loc2.getLatitude()) * Math.PI / 180.;
		double phi1 = loc1.getPhi();
		double phi2 = loc2.getPhi();

		/*
		 * cos a = a*b/|a|/|b|
		 */
		double cosAlpha = FastMath.sin(theta1) * FastMath.sin(theta2) * FastMath.cos(phi1 - phi2)
				+ FastMath.cos(theta1) * FastMath.cos(theta2);

		return FastMath.acos(cosAlpha);
	}
    
}
