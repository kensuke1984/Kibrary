/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.util.FastMath;

import io.github.kensuke1984.kibrary.inversion.PerturbationPoint;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.Location;

/**
 * @author kensuke -> modified for dx(km) x dy(km) grids by Yuki S.
 * @since 2015/03/10 
 * @version 0.0.1
 * 
 * @version 0.1.0 for dx(km) x dy(km) grids
 */
class SampleVolumeComputation {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String dir = "/mnt/doremi/suzuki/Anisotropy/analysis/PREM_12s/threedPartial_300x100km";
//		String dir = "/Users/Yuki/Dropbox/";
		double dr = 100;			//鉛直方向 (km)
		double dlon = 300;		//水平軽度方向(km)
		double dlat = 300;		//水平緯度方向(km)
			//horizontalPointFile, perturbationPointFile
			// InformationFileMaker したときにできるもの
		File horizontalPointFile = new File(dir + "horizontalPoint.inf");	
		File perturbationPointFile = new File(dir + "perturbationPoint.inf");	
		PerturbationPoint pp = new PerturbationPoint(horizontalPointFile, perturbationPointFile);
		Location[] location = pp.getPerturbationLocation();
//		for (Location loc : location){
//			System.out.println(loc.getLatitude()+" "+loc.getLongitude()+" "+loc.getR());
//		}
		//Map<Location, Double> volumeMap = null;
		ArrayList<String> vMap = new ArrayList<String>();
		
		Arrays.stream(location).forEachOrdered(loc -> {
			double radius = loc.getR();
			Location tmpLoc1 = loc.toLocation(radius - 0.5 * dr);
			double mincj = Earth.getExtendedShaft(tmpLoc1);
			Location tmpLoc2 = loc.toLocation(radius + 0.5 * dr);
			double maxcj = Earth.getExtendedShaft(tmpLoc2);
			double theLat = loc.getLatitude();
			double theLon = loc.getLongitude();
			double rDash = radius * Math.cos(Math.toRadians(theLat));
			
		});
		
		for (int i=0; i < location.length; i++){
			double chojiku = Earth.getExtendedShaft(location[i]);
			double radius = location[i].getR();
			Location tmploc1 = location[i].toLocation(radius - 0.5 * dr);
			double mincj = Earth.getExtendedShaft(tmploc1);
			Location tmploc2 = location[i].toLocation(radius + 0.5 * dr);
			double maxcj = Earth.getExtendedShaft(tmploc2);
			double theLat = location[i].getLatitude();
			double theLon = location[i].getLongitude();
			double radi = location[i].getR();
			double rDash = radi * FastMath.cos(FastMath.toRadians(theLat));
			double minlat = theLat - FastMath.toDegrees(dlat/2/radi);
			double maxlat = theLat + FastMath.toDegrees(dlat/2/radi);
			double minlon = theLon - FastMath.toDegrees(dlon/2/rDash);
			double maxlon = theLon + FastMath.toDegrees(dlon/2/rDash);
			if (minlon < -180) {
				minlon += 360;
				maxlon += 360;
			}
			if (theLon < 0){
				theLon += 360;
			}
			double volume = Earth.getVolume(mincj, maxcj, minlat, maxlat, minlon, maxlon);
			//volumeMap.put(location[i], volume);
			vMap.add(String.format("%s %.3f %.3f %.3f %f%n", "MU", theLat, theLon, radi, volume));
//			System.out.println(theLat+" "+theLon+" "+volume);
		}
		
		try {
			PrintWriter pwV = new PrintWriter(new BufferedWriter(new FileWriter(dir + "unknowns.inf",true)));
			for (String s : vMap){
				pwV.print(s);
//				System.out.println(s);
			}
			pwV.close();
		} catch (IOException e) {
			System.out.println("ファイルを作成できません");
			e.printStackTrace();
		}
		
//		pp.setDlatitude(5);//deg
//		pp.setDlongitude(5);//deg
//		pp.setDr(50); //
//		pp.computeVolumes();
//		Map<Location, Double> vMap=pp.getVolumeMap();
//		Location hoshiiloc= new Location(15.0, -165.0, 3505.0);//これはhorizontalpointfileのなかにないといけない
		
//		double sokonoV = vMap.get(hoshiiloc);//入ってない場合はエラー

//		System.out.println(sokonoV);
	   
	}

}
