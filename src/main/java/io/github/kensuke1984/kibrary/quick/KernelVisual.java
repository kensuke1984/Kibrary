package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTCatalog;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;

public class KernelVisual {

	public static void main(String[] args) throws IOException {
		Path partialIDPath = Paths.get(args[0]);
		Path partialPath = Paths.get(args[1]);
		
		PartialID[] partials = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialPath);
		
		Set<GlobalCMTID> idSet = new HashSet<>();
		Set<String> stationnameSet = new HashSet<>();
		idSet.add(new GlobalCMTID("201407291046A"));
		stationnameSet.add("109C");
		
		Path dir0 = Paths.get("KernelTemporalVisual");
		Files.createDirectories(dir0);
		for (PartialID partial : partials) {
			//write only particular stations/events
//			if (!idSet.contains(partial.getGlobalCMTID()))
//				continue;
//			if (!stationnameSet.contains(partial.getStation().getStationName()))
//				continue;
			
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
						+ "_" + partial.getPartialType() + "_" + partial.getSacComponent()
						+ String.format("_kernelTemporal_snapshots_t0%d.txt", (int) t0));
				
				Path filePath_sensitivity = dir3.resolve(new Phases(partial.getPhases()).toString()
						+ "_" + partial.getPartialType() + "_" + partial.getSacComponent()
						+ String.format("_sensitivityTemporal_snapshots_t0%d.txt", (int) t0));
				
				if (!Files.exists(filePath))
					Files.createFile(filePath);
				if (!Files.exists(filePath_sensitivity))
					Files.createFile(filePath_sensitivity);
				
				double cumulativeSensitivity = 0.;
					
				BufferedWriter writer2 = Files.newBufferedWriter(filePath, StandardOpenOption.APPEND);
				writer2.write(String.format("%s ", partial.getPerturbationLocation()));
				BufferedWriter writer2_s = Files.newBufferedWriter(filePath_sensitivity, StandardOpenOption.APPEND);
				writer2_s.write(String.format("%s ", partial.getPerturbationLocation()));
				for (int i = 0; i < data.length; i++) {
					writer2.write(String.format("%.5e "
							, data[i]));
					
					cumulativeSensitivity += data[i] * data[i];
					writer2_s.write(String.format("%.5e ", Math.sqrt(cumulativeSensitivity) / (i+1)));
				}
				writer2.newLine();
				writer2.close();
				writer2_s.newLine();
				writer2_s.close();
		}
	}
	
}