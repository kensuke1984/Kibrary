package io.github.kensuke1984.kibrary.util.addons;

import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

public class AmplitudeRatioInformation {

	public static void main(String[] args) {
		Path correctionPath = Paths.get(args[0]);
		
		double azmin = 300;
		double azmax = 360;
		double daz = 10;
		int naz = (int) ((azmax - azmin) / daz);
		
		try {
			Set<StaticCorrection> corrections = StaticCorrectionFile.read(correctionPath);
			for (GlobalCMTID id : corrections.stream().map(c -> c.getGlobalCMTID()).collect(Collectors.toSet())) {
				for (int iaz = 0; iaz < naz; iaz++) {
					double az = azmin + iaz * daz;
					
					Set<StaticCorrection> corrSet = corrections.stream()
							.filter(c -> {
								if (!c.getGlobalCMTID().equals(id))
									return false;
								if (c.getAmplitudeRatio() > 3.5 || c.getAmplitudeRatio() < 1./3.5)
									return false;
								double aztmp = Math.toDegrees(c.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(c.getStation().getPosition()));
								if (aztmp < az || aztmp >= az + daz)
									return false;
								return true;
							}).collect(Collectors.toSet());
					int countCorrForEvent = (int) corrections.stream().filter(c -> c.getGlobalCMTID().equals(id)
							&& c.getAmplitudeRatio() <= 3.5 && c.getAmplitudeRatio() >= 1./3.5).count();
					
					if (corrSet.size() < (double) countCorrForEvent / naz)
						continue;
					
					Path outpath = Paths.get(id + ".az" + (int) (az) + ".staticcorrection" + ".dat");
					PrintWriter pw = new PrintWriter(outpath.toFile());
					double[] averageOverDistance = new double[120];
					int[] countDistance = new int[120];
					
					for (StaticCorrection corr : corrSet) {
						double azimuth = Math.toDegrees(id.getEvent().getCmtLocation().getAzimuth(corr.getStation().getPosition()));
						double distance = Math.toDegrees(id.getEvent().getCmtLocation().getEpicentralDistance(corr.getStation().getPosition()));
						int k = (int) (distance);
						averageOverDistance[k] += corr.getAmplitudeRatio();
						countDistance[k] += 1;
					}
					for (int k = 0; k < countDistance.length; k++) {
						if (countDistance[k] == 0)
							continue;
						averageOverDistance[k] /= countDistance[k];
						double distance = k;
						pw.println(distance + " " + averageOverDistance[k]);
					}
	//				for (StaticCorrection corr : corrections.stream()
	//						.filter(c -> c.getGlobalCMTID().equals(id)).collect(Collectors.toSet())) {
	//					double distance = Math.toDegrees(id.getEvent().getCmtLocation().getEpicentralDistance(corr.getStation().getPosition()));
	//					double azimuth = Math.toDegrees(id.getEvent().getCmtLocation().getAzimuth(corr.getStation().getPosition()));
	//					pw.println(corr.getTimeshift() + " " + corr.getAmplitudeRatio()  + " " + distance + " " + azimuth);
	//				}
					pw.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
