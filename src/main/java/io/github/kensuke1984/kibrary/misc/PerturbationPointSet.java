/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;


/**
 * @version 0.0.1
 * @since 2015/12/01
 * @author Yuki
 *
 */
public class PerturbationPointSet {
	
	private static double minLat = 35.0;			// [-90, 90)
	private static double maxLat = 85.0;		// [minLat, 90]
	private static double minLon = 130.0;		// [-180, 360)
	private static double maxLon = 290.0;		// [minLon, 360]
//	private static double dltaDeg = 5.0;		//
	private static double dDistance	 = 150;			// CMBでの水平方向グリッド間隔(km)
	private static double RADIUSatCMB = 3480;	// CMBの半径(km)
	private static double dltaDeg = Math.toDegrees(dDistance/RADIUSatCMB);
	private static Path outFile = Paths.get("/Users/Yuki/Desktop/grid_" + String.valueOf((int)dDistance) +".inf");
	
	public static void main(String[] args){
		Set<Location> locSet = new HashSet<Location>();
		
		HorizontalPosition start = new HorizontalPosition(minLat, minLon);
		
		//Latitude方向に配置
		for (int i=0; i < (int)((maxLat-minLat)/dltaDeg)+1; i++) {			
		//Longitudinal方向に配置
			for (int j=0; j < (int)((Math.toRadians(maxLon-minLon)*RADIUSatCMB*Math.cos(Math.toRadians(start.getLatitude()+dltaDeg*i)))/dDistance)+1; j++) {
				Location newLoc = new Location(start.getLatitude()+dltaDeg*i,
										   start.getLongitude()+(Math.toDegrees(dDistance/RADIUSatCMB/Math.cos(Math.toRadians(start.getLatitude()+dltaDeg*i))))*j,
										   RADIUSatCMB);	
				locSet.add(newLoc);
			System.out.printf("%.3f %.3f%n", newLoc.getLatitude(), newLoc.getLongitude());
			}		
		}
		try {
			FileWriter fw = new FileWriter(outFile.toFile());
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);
		for (Location loc : locSet){
			if (loc.getLongitude()<0){
			pw.printf("%.3f %.3f%n",loc.getLatitude(), loc.getLongitude()+360);
			}
			else {
			pw.printf("%.3f %.3f%n",loc.getLatitude(), loc.getLongitude());
			}
		}
		pw.close();
		} catch (IOException e) {e.printStackTrace();}
//		System.out.println(newLoc);
	}
}
