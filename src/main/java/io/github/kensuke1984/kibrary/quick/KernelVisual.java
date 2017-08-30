package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.math3.linear.ArrayRealVector;

public class KernelVisual {

	public static void main(String[] args) throws IOException {
		Path partialIDPath = Paths.get(args[0]);
		Path partialPath = Paths.get(args[1]);
		
		PartialID[] partials = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialPath);
		
//		Path dir0 = Paths.get("partialVisual");
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
		
		Path dir0 = Paths.get("KernelTemporalVisual");
		Files.createDirectories(dir0);
		for (PartialID partial : partials) {
//			if (partial.getStation().getStationName().equals("X060") 
//					&& new Phases(partial.getPhases()).equals(new Phases("S,SSS,ScS,sSS,sScSScS,sS,SS,ScSScS,sSSS,sScS")) ) {
//			if (new Phases(partial.getPhases()).equals(new Phases("S,SSS,ScS,sSS,sScSScS,sS,SS,ScSScS,sSSS,sScS"))) {
				Path dir1 = dir0.resolve(partial.getGlobalCMTID().toString());
				if (!Files.exists(dir1))
					Files.createDirectories(dir1);
				Path dir2 = dir1.resolve(partial.getStation().getStationName());
				if (!Files.exists(dir2))
					Files.createDirectories(dir2);
				Path dir3 = dir2.resolve(new Phases(partial.getPhases()).toString());
				if (!Files.exists(dir3))
					Files.createDirectories(dir3);
				
				double[] data = partial.getData();
				double t0 = partial.getStartTime();
				Path filePath = dir3.resolve(new Phases(partial.getPhases()).toString() 
						+ String.format("_kernelTemporal_snapshots_t0%d.txt", (int) t0));
				if (!Files.exists(filePath))
					Files.createFile(filePath);
				BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.APPEND);
				writer.write(String.format("%s ", partial.getPerturbationLocation()));
				for (int i = 0; i < data.length; i++) {
					writer.write(String.format("%.3e "
							, data[i]));
//					String t = String.format("%04d", (int) (t0 + i));
//					Path filePath = dir3.resolve(new Phases(partial.getPhases()).toString() + "_kernelTemporal_snapshot" + t + ".txt");
//					if (!Files.exists(filePath))
//						Files.createFile(filePath);
//					Files.write(filePath, (partial.getPerturbationLocation().toString() + " " + data[i] + "\n").getBytes(), StandardOpenOption.APPEND);
				}
				writer.newLine();
				writer.close();
//			}
		}
	}
	
}