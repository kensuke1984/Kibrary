package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class WriteStaticCorrectionMap {

	public static void main(String[] args) throws IOException {
		Path fujiStaticPath = Paths.get(args[0]);
		
		Set<StaticCorrection> fujiCorrections = StaticCorrectionFile.read(fujiStaticPath);
		
		double[][] mapCorr = averageMap(fujiCorrections);
		
		Path outpath = Paths.get("corrections_map.txt");
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (int i = 0; i < 360; i++) {
			for (int j = 0; j < 180; j++) {
				pw.println(i + " " + (j - 90) + " " + mapCorr[i][j]);
			}
		}
		pw.close();
		
		Path outpath4 = Paths.get("correctionAtStation.txt");
		PrintWriter pw4 = new PrintWriter(outpath4.toFile());
		for (StaticCorrection corr : fujiCorrections) {
			pw4.println(corr.getStation() + " " + corr.getStation().getPosition() + " " + corr.getTimeshift());
		}
		pw4.close();
	}
	
	public static double[][] averageMap(Set<StaticCorrection> ratios) {
		double[][] map = new double[360][180];
		int[][] count = new int[360][180];
		for (StaticCorrection corr : ratios) {
			double lon = corr.getStation().getPosition().getLongitude();
			if (lon < 0)
				lon += 360;
			double lat = corr.getStation().getPosition().getLatitude() + 90;
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
