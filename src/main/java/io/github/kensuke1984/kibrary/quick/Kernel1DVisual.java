package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;

public class Kernel1DVisual {

	public static void main(String[] args) throws IOException {
		Path partialIDPath = Paths.get(args[0]);
		Path partialPath = Paths.get(args[1]);
		
		PartialID[] partials = PartialIDFile.read(partialIDPath, partialPath);
		
		List<PartialID> par0list = Stream.of(partials).filter(p -> p.getPartialType().equals(PartialType.PAR0)).collect(Collectors.toList());
		
		Path dir0 = Paths.get("KernelTemporalVisual");
		Path dir00 = Paths.get("Partials");
		Files.createDirectories(dir0);
		
		for (PartialID partial : partials) {
				Path dir1 = dir0.resolve(new Phases(partial.getPhases()).toString());
				Path dir11 = dir00.resolve(new Phases(partial.getPhases()).toString());
				if (!Files.exists(dir1)) {
					Files.createDirectories(dir1);
					Files.createDirectories(dir11);
				}
				
				double[] data = partial.getData();
				double samplingHz = partial.getSamplingHz();
				double distance = partial.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(partial.getStation().getPosition())
						* 180. / Math.PI;
				if (samplingHz != 1.)
					throw new RuntimeException("SamplingHz != 1 not yet supported");
				double t0 = partial.getStartTime();
				
				Path filePath2 = dir11.resolve(partial.getStation()
						+ "_" + new Phases(partial.getPhases()).toString()
						+ "_" + (int) partial.getPerturbationLocation().getR()
						+ "_" + partial.getPartialType()
						+ "_" + partial.getSacComponent()
						+ String.format("_kernel1DTemporal_snapshots_t0%d.txt", (int) t0));
				if (!Files.exists(filePath2))
					Files.createFile(filePath2);
				
				for (int i = 0; i < data.length; i++) {
					double t = (int) (t0 + i / samplingHz);
					Path filePath = dir1.resolve(new Phases(partial.getPhases()).toString() 
							+ "_" + partial.getPartialType()
							+ "_" + partial.getSacComponent()
							+ String.format("_kernel1DTemporal_snapshots_t0%d.txt", (int) t));
					if (!Files.exists(filePath))
						Files.createFile(filePath);
					
					Files.write(filePath, String.format("%.5f %.5f %.5e\n", distance
							, partial.getPerturbationLocation().getR()
							, data[i]).getBytes(), StandardOpenOption.APPEND);
					
					Files.write(filePath2, String.format("%.5f %.5e\n", t, data[i]).getBytes(), StandardOpenOption.APPEND);
				}
		}
	}
	
}