package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import edu.sc.seis.TauP.SphericalCoords;

public class StaticCorrectionMap {

	public static void main(String[] args) throws IOException {
		Path fujiStaticPath = Paths.get(args[0]);
		Path fujiStaticPath2 = null;
		if (args.length == 2)
			fujiStaticPath2 = Paths.get(args[1]);
		
		Set<StaticCorrection> fujiCorrections = StaticCorrectionFile.read(fujiStaticPath);
		Set<StaticCorrection> corrSet = new HashSet<>();
		
		if (fujiStaticPath2 != null) {
			Set<StaticCorrection> fujiCorrections2 = StaticCorrectionFile.read(fujiStaticPath2);
			fujiCorrections.stream().forEach(corr -> {
				StaticCorrection corr2 = fujiCorrections2.stream().parallel().filter(c -> c.getGlobalCMTID().equals(corr.getGlobalCMTID())
						&& c.getStation().equals(corr.getStation())
						&& c.getSynStartTime() == corr.getSynStartTime()
						&& c.getComponent().equals(corr.getComponent()))
					.findFirst().get();
				corrSet.add(new StaticCorrection(corr.getStation(), corr.getGlobalCMTID()
					, corr.getComponent(), corr.getSynStartTime(), corr.getTimeshift() - corr2.getTimeshift()
					, corr.getAmplitudeRatio() / corr2.getAmplitudeRatio(), corr.getPhases()));
			});
			fujiCorrections = corrSet;
		}
			
		
		double[][] mapCorr = averageMap(fujiCorrections);
		
		Path outpath3 = Paths.get("corrections_map.txt");
		PrintWriter pw3 = new PrintWriter(outpath3.toFile());
		for (int i = 0; i < 360; i++) {
			double lon = i;
			for (int j = 0; j < 180; j++) {
				pw3.println(lon + " " + (j - 90) + " " + mapCorr[i][j]);
			}
		}
		pw3.close();
		
	}
	
	public static double[][] averageMap(Set<StaticCorrection> ratios) {
		double[][] map = new double[360][180];
		int[][] count = new int[360][180];
		for (StaticCorrection corr : ratios) {
//			double lon = corr.getStation().getPosition().getLongitude();
//			if (lon < 0)
//				lon += 360;
//			double lat = corr.getStation().getPosition().getLatitude() + 90;
			
			double evtlat = corr.getGlobalCMTID().getEvent().getCmtLocation().getLatitude();
			double evtlon = corr.getGlobalCMTID().getEvent().getCmtLocation().getLongitude();
			double distance = Math.toDegrees(corr.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(corr.getStation().getPosition()));
			double azimuth = Math.toDegrees(corr.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(corr.getStation().getPosition()));
			
			double lat = SphericalCoords.latFor(evtlat, evtlon, distance/2., azimuth);
			double lon = SphericalCoords.lonFor(evtlat, evtlon, distance/2., azimuth);
			if (lon < 0)
				lon += 360;
			lat += 90;
			
			int ilon = (int) lon;
			int ilat = (int) lat;
			if (ilon == 360)
				ilon = 359;
			if (ilat == 180)
				ilat = 179;
			if (corr.getTimeshift() < 100.) {
				map[ilon][ilat] += corr.getTimeshift();
				count[ilon][ilat] += 1;
			}
		}
		
		for (int i = 0; i < 360; i++) {
			for (int j = 0; j < 180; j++) {
				if (count[i][j] > 0)
					map[i][j] /= count[i][j];
			}
		}
		
		return map;
	}
}
