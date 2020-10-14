package io.github.kensuke1984.kibrary.quick;

import edu.sc.seis.TauP.SphericalCoords;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;

public class TransformAzimuths {

	public static void main(String[] args) {
		double[] azimuths = new double[] {321, 327, 333, 339, 345, 351.6};
		HorizontalPosition center_pos = new HorizontalPosition(-22.49657142857143, -66.35978571428572);
		
		HorizontalPosition new_center_pos = new HorizontalPosition(-22.86, -63.69);
		double[] new_azimuths = new double[azimuths.length];
		
		double dist = 35;
		for (int i = 0; i < azimuths.length; i++) {
			double lat = SphericalCoords.latFor(center_pos.getLatitude(), center_pos.getLongitude(), dist, azimuths[i]);
			double lon = SphericalCoords.lonFor(center_pos.getLatitude(), center_pos.getLongitude(), dist, azimuths[i]);
			HorizontalPosition az_pos = new HorizontalPosition(lat, lon);
			
			new_azimuths[i] = Math.toDegrees(new_center_pos.getAzimuth(az_pos));
			System.out.println(az_pos + " "  + Math.toDegrees(center_pos.getAzimuth(az_pos)));
		}
		
		for (int i = 0; i < azimuths.length; i++)
			System.out.printf("%.3f ", azimuths[i]);
		System.out.println();
		for (int i = 0; i < azimuths.length; i++)
			System.out.printf("%.3f ", new_azimuths[i]);
	}

}
