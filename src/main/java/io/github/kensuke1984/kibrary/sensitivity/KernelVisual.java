/**
 * 
 */
package io.github.kensuke1984.kibrary.sensitivity;

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

/**
 * @version 0.0.1
 * @since 2018/09/21
 * @author Yuki
 *
 */
public class KernelVisual {
	public static void main(String[] args) throws IOException {
		Path partialIDPath = Paths.get(args[0]);
		Path partialPath = Paths.get(args[1]);
		
		PartialID[] partials = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialPath);
		
		PartialID[] partials_MU = Stream.of(partials).filter(par -> par.getPartialType().equals(PartialType.MU)).collect(Collectors.toList()).toArray(new PartialID[0]);
		PartialID[] partials_L = Stream.of(partials).filter(par -> par.getPartialType().equals(PartialType.L)).collect(Collectors.toList()).toArray(new PartialID[0]);
		PartialID[] partials_N = Stream.of(partials).filter(par -> par.getPartialType().equals(PartialType.N)).collect(Collectors.toList()).toArray(new PartialID[0]);
		
		Set<GlobalCMTID> idSet = new HashSet<>();
		Set<String> stationnameSet = new HashSet<>();
		idSet.add(new GlobalCMTID("201407291046A"));
		stationnameSet.add("CCM");
		
		Path dir0 = Paths.get("KernelTemporalVisual");
		Files.createDirectories(dir0);
		for (PartialID partial : partials_MU) {
			
			PartialID partial_L = null;
			PartialID partial_N = null;
			if (partials_L.length == partials_MU.length)
				partial_L = Stream.of(partials_L).filter(par -> par.getGlobalCMTID().equals(partial.getGlobalCMTID())
					&& par.getStation().equals(partial.getStation())
								&& par.getPerturbationLocation().equals(partial.getPerturbationLocation())
								&& par.getMinPeriod() == partial.getMinPeriod()
								&& par.getSacComponent().equals(partial.getSacComponent())
								&& new Phases(par.getPhases()).equals(new Phases(partial.getPhases()))
					).findFirst().get();
			if (partials_N.length == partials_MU.length)
				partial_N = Stream.of(partials_N).filter(par -> par.getGlobalCMTID().equals(partial.getGlobalCMTID())
					&& par.getStation().equals(partial.getStation())
								&& par.getPerturbationLocation().equals(partial.getPerturbationLocation())
								&& par.getMinPeriod() == partial.getMinPeriod()
								&& par.getSacComponent().equals(partial.getSacComponent())
								&& new Phases(par.getPhases()).equals(new Phases(partial.getPhases()))
					).findFirst().get();
//			if (partial.getStation().getStationName().equals("X060") 
//					&& new Phases(partial.getPhases()).equals(new Phases("S,SSS,ScS,sSS,sScSScS,sS,SS,ScSScS,sSSS,sScS")) ) {
//			if (new Phases(partial.getPhases()).equals(new Phases("S,SSS,ScS,sSS,sScSScS,sS,SS,ScSScS,sSSS,sScS"))) {
				Path dir1 = dir0.resolve(partial.getGlobalCMTID().toString());
				if (!Files.exists(dir1))
					Files.createDirectories(dir1);
				Path dir2 = dir1.resolve(partial.getStation().getName());
				if (!Files.exists(dir2))
					Files.createDirectories(dir2);
				Path dir3 = dir2.resolve(new Phases(partial.getPhases()).toString());
				if (!Files.exists(dir3))
					Files.createDirectories(dir3);
				
				double[] data_MU = partial.getData();
				double[] data_L = partial_L.getData();
				double[] data_N = partial_N.getData();
				
				double t0 = partial.getStartTime();
				
				Path filePath = null;
				Path filePath_L = null;
				Path filePath_N = null;
				
				if (partial_L != null) {
					filePath = dir3.resolve(new Phases(partial.getPhases()).toString()
						+ "_" + "L" + "_" + partial.getSacComponent()
						+ String.format("_kernelTemporal_snapshots_t0%d.txt", (int) t0));
					filePath_N = dir3.resolve(new Phases(partial.getPhases()).toString()
							+ "_" + "N" + "_" + partial.getSacComponent()
							+ String.format("_kernelTemporal_snapshots_t0%d.txt", (int) t0));
				}
				Path filePath_MU = dir3.resolve(new Phases(partial.getPhases()).toString()
						+ "_" + "MU" + "_" + partial.getSacComponent()
						+ String.format("_kernelTemporal_snapshots_t0%d.txt", (int) t0));
				
				if (!Files.exists(filePath_MU))
					Files.createFile(filePath_MU);
				
				
				Path filePath_sensitivity = null;
				Path filePath_L_sensitivity = null;
				Path filePath_N_sensitivity = null;
				if (partial_L != null) {
					filePath_sensitivity = dir3.resolve(new Phases(partial.getPhases()).toString()
						+ "_" + "L" + "_" + partial.getSacComponent()
						+ String.format("_sensitivityTemporal_snapshots_t0%d.txt", (int) t0));
					filePath_N_sensitivity = dir3.resolve(new Phases(partial.getPhases()).toString()
							+ "_" + "N" + "_" + partial.getSacComponent()
							+ String.format("_sensitivityTemporal_snapshots_t0%d.txt", (int) t0));
				}
				Path filePath_MU_sensitivity = dir3.resolve(new Phases(partial.getPhases()).toString()
						+ "_" + "MU" + "_" + partial.getSacComponent()
						+ String.format("_sensitivityTemporal_snapshots_t0%d.txt", (int) t0));
				
				if (!Files.exists(filePath_MU_sensitivity))
					Files.createFile(filePath_MU_sensitivity);
				
				double cumulativeSensitivity = 0.;
				double cumulativeSensitivity_MU = 0.;
				double cumulativeSensitivity_L = 0.;
				double cumulativeSensitivity_N = 0.;
				
				if (partial_L != null) {
					if (!Files.exists(filePath))
						Files.createFile(filePath);
					if (!Files.exists(filePath_L))
						Files.createFile(filePath_L);
					if (!Files.exists(filePath_N))
						Files.createFile(filePath_N);
				
					BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.APPEND);
					writer.write(String.format("%s ", partial.getPerturbationLocation()));
					BufferedWriter writer_s = Files.newBufferedWriter(filePath_sensitivity, StandardOpenOption.APPEND);
					writer_s.write(String.format("%s ", partial.getPerturbationLocation()));
					for (int i = 0; i < data_L.length; i++) {
						writer.write(String.format("%.5e "
								, data_L[i]));
						
						cumulativeSensitivity += data_L[i] * data_L[i];
						writer_s.write(String.format("%.5e ", Math.sqrt(cumulativeSensitivity) / (i+1)));
					}
					
					BufferedWriter writer3 = Files.newBufferedWriter(filePath_N, StandardOpenOption.APPEND);
					writer3.write(String.format("%s ", partial.getPerturbationLocation()));
					BufferedWriter writer3_s = Files.newBufferedWriter(filePath_N_sensitivity, StandardOpenOption.APPEND);
					writer3_s.write(String.format("%s ", partial.getPerturbationLocation()));
					for (int i = 0; i < data_N.length; i++) {
						writer3.write(String.format("%.5e "
								, data_N[i]));
						
						cumulativeSensitivity_N += data_N[i] * data_N[i];
						writer3_s.write(String.format("%.5e ", Math.sqrt(cumulativeSensitivity_N) / (i+1)));
					}
					
					writer.newLine();
					writer3.newLine();
					writer3_s.newLine();
					writer_s.newLine();
					writer.close();
					writer3.close();
					writer3_s.close();
					writer_s.close();
				}
					
				BufferedWriter writer2 = Files.newBufferedWriter(filePath_MU, StandardOpenOption.APPEND);
				writer2.write(String.format("%s ", partial.getPerturbationLocation()));
				BufferedWriter writer2_s = Files.newBufferedWriter(filePath_MU_sensitivity, StandardOpenOption.APPEND);
				writer2_s.write(String.format("%s ", partial.getPerturbationLocation()));
				for (int i = 0; i < data_MU.length; i++) {
					writer2.write(String.format("%.5e "
							, data_MU[i]));
					
					cumulativeSensitivity_MU += data_MU[i] * data_MU[i];
					writer2_s.write(String.format("%.5e ", Math.sqrt(cumulativeSensitivity_MU) / (i+1)));
				}
				writer2.newLine();
				writer2.close();
				writer2_s.newLine();
				writer2_s.close();
		}
	}
}
