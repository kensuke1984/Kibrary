package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.math3.linear.ArrayRealVector;

public class KernelVisual {

	public static void main(String[] args) throws IOException {
		Path partialIDPath = Paths.get("partialID.dat");
		Path partialPath = Paths.get("partial.dat");
		
		PartialID[] partials = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialPath);
		
		Path dir0 = Paths.get("partialVisual");
//		Files.createDirectories(dir0);
//		
//		for (PartialID partial : partials) {
//			Path dirPath = dir0.resolve(partial.getStation().getStationName());
//			if (!Files.exists(dirPath))
//				Files.createDirectories(dirPath);
//			Path filePath = dirPath.resolve(new Phases(partial.getPhases()).toString() + "_sensi.txt");
//			if (!Files.exists(filePath))
//				Files.createFile(filePath);
			
//			if (partial.getStation().getStationName().equals("X040")) {
//				if (partial.getPerturbationLocation().getLongitude() == 40.0) {
//					double distance = partial.getStation().getPosition().getEpicentralDistance(partial.getGlobalCMTID().getEvent().getCmtLocation())
//					 * 180 / Math.PI;
//					System.out.println(distance + " " + partial.getPerturbationLocation() + " " + new ArrayRealVector(partial.getData()).getMaxValue());
//				}
//			}
//			else
//				continue;
			
//			double s = new ArrayRealVector(partial.getData()).getNorm();
//			Files.write(filePath, (partial.getPerturbationLocation().toString() + " " + s + "\n").getBytes(), StandardOpenOption.APPEND);
//		}
		
		dir0 = Paths.get("KernelTemporalVisual");
		Files.createDirectories(dir0);
		for (PartialID partial : partials) {
//			if (partial.getStation().getStationName().equals("X060") 
//					&& new Phases(partial.getPhases()).equals(new Phases("S,SSS,ScS,sSS,sScSScS,sS,SS,ScSScS,sSSS,sScS")) ) {
				Path dirPath = dir0.resolve(partial.getStation().getStationName());
				if (!Files.exists(dirPath))
					Files.createDirectories(dirPath);
				Path dir1 = dirPath.resolve(new Phases(partial.getPhases()).toString());
				if (!Files.exists(dir1))
					Files.createDirectories(dir1);
				
				
				double[] data = partial.getData();
				double t0 = partial.getStartTime();
				for (int i = 0; i < data.length; i++) {
					String t = String.format("%04d", (int) (t0 + i));
					Path filePath = dir1.resolve(new Phases(partial.getPhases()).toString() + "_kernelTemporal_snapshot" + t + ".txt");
					if (!Files.exists(filePath))
						Files.createFile(filePath);
					Files.write(filePath, (partial.getPerturbationLocation().toString() + " " + data[i] + "\n").getBytes(), StandardOpenOption.APPEND);
				}
//			}
		}
	}
	
}