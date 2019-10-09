/**
 * 
 */
package io.github.kensuke1984.kibrary.specfem;

/**
 * @version 0.0.1
 * @since 2018/03/10
 * @author Yuki
 *
 */
public class ImaginaryStationsSet {
	private static double eventLat = 0.0;
	private static double eventLon = 0.0;
	private static double minXI = 0.;
	private static double maxXI = 359.;
	private static double minDELTA = 30.;
	private static double maxDELTA = 90.;
	private static double stationLat;
	private static double stationLon;
	
	public static void main(String[] args){
		int i = 0;
		for (double DELTA=minDELTA; DELTA <= maxDELTA; DELTA++) {
			for (double XI=minXI; XI <= maxXI; XI++){
				stationLocation(eventLat, eventLon, XI, DELTA);
				i++;
				System.out.printf("%05d %s %.6f %.6f %.3f %.3f %.3f %.3f\n", i, "DSM", stationLat, stationLon, 0.0, 0.0, DELTA, XI);
			}	
		}
	}
	
	private static void stationLocation(double eventLat, double eventLon, double azimuth, double gcarc) {
		double theta_Erad = Math.PI/2 - Math.toRadians(eventLat);
		double xi_rad = Math.toRadians(azimuth);
		double delta_rad = Math.toRadians(gcarc);
		
		//compute theta_S(rad).
		double sin_delta = Math.sin(delta_rad);
		double cos_delta = Math.cos(delta_rad);
		double cos_xi = Math.cos(xi_rad);
		double cos_theta_Erad = Math.cos(theta_Erad);
		double sin_theta_Erad = Math.sin(theta_Erad);
		double theta_Srad = Math.acos(sin_theta_Erad * (sin_delta*cos_xi + cos_delta*cos_theta_Erad));
//		System.out.println(theta_Srad+" "+gcarc);
		
			//compute phi_S(rad).
			double sin_xi = Math.sin(xi_rad);
			double sin_theta_Srad = Math.sin(theta_Srad);
			double phi_Erad = Math.toRadians(eventLon);
			double cos_theta_Srad = Math.cos(theta_Srad);
			double phi_Srad;
			if (gcarc<=90)
//				if (theta_Srad!=0)
					phi_Srad = phi_Erad + Math.asin(sin_xi*sin_delta/sin_theta_Srad);
//				else
//					phi_Srad = phi_Erad;
			else
				phi_Srad = phi_Erad - Math.acos((cos_delta - cos_theta_Erad*cos_theta_Srad)/(sin_theta_Erad*sin_theta_Srad));				
			
			//compute event Lat. & Lon.
			double lamda_Srad = Math.PI/2 -theta_Srad;
			stationLat = Math.toDegrees(lamda_Srad);
			stationLon = Math.toDegrees(phi_Srad);
//			System.out.println(gcarc+" "+sin_delta+" "+phi_Srad);
	}
}
