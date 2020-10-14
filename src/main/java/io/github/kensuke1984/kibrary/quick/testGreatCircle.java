package io.github.kensuke1984.kibrary.quick;

import edu.sc.seis.TauP.SphericalCoords;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;

public class testGreatCircle {

	public static void main(String[] args) {
		HorizontalPosition pos0 = new HorizontalPosition(-30, -90);
		HorizontalPosition pos1 = new HorizontalPosition(40, -120);
		double azimuth = Math.toDegrees(pos0.getGeographicalAzimuth(pos1));
		double distance = Math.toDegrees(pos0.getGeographicalDistance(pos1));
		double lat = SphericalCoords.latFor(pos0.getLatitude(), pos0.getLongitude(), distance, azimuth);
		double lon = SphericalCoords.lonFor(pos0.getLatitude(), pos0.getLongitude(), distance, azimuth);
		HorizontalPosition pos2 = new HorizontalPosition(lat, lon);
		System.out.println(pos1 + " " + pos2);
	}

}
