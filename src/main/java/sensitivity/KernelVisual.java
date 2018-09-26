/**
 * 
 */
package sensitivity;

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
		
		PartialID[] partials_MU = Stream.of(partials).filter(par -> par.getPartialType().equals(PartialType.MU)).collect(Collectors.toList()).toArray(new PartialID[0]);
		PartialID[] partials_LAMBDA = Stream.of(partials).filter(par -> par.getPartialType().equals(PartialType.LAMBDA)).collect(Collectors.toList()).toArray(new PartialID[0]);
		
		Set<GlobalCMTID> idSet = new HashSet<>();
		Set<String> stationnameSet = new HashSet<>();
		idSet.add(new GlobalCMTID("201407291046A"));
		stationnameSet.add("CCM");
		
		Path dir0 = Paths.get("KernelTemporalVisual");
		Files.createDirectories(dir0);
		for (PartialID partial : partials_MU) {
			//write only particular stations/events
//			if (!idSet.contains(partial.getGlobalCMTID()))
//				continue;
//			if (!stationnameSet.contains(partial.getStation().getStationName()))
//				continue;
			
			PartialID partial_LAMBDA = null;
			if (partials_LAMBDA.length == partials_MU.length)
				partial_LAMBDA = Stream.of(partials_LAMBDA).filter(par -> par.getGlobalCMTID().equals(partial.getGlobalCMTID())
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
				double[] data_LAMBDAMU = null;
				double[] data_LAMBDA = null;
				if (partial_LAMBDA != null) {
					data_LAMBDAMU = partial.getData();
					data_LAMBDA = partial_LAMBDA.getData();
				}
				
				if (partial_LAMBDA != null) {
					for (int i = 0; i < data_LAMBDAMU.length; i++)
						data_LAMBDAMU[i] += 2 * data_LAMBDA[i];
				}
				
				double t0 = partial.getStartTime();
				
				Path filePath = null;
				Path filePath_LAMBDA = null;
				if (partial_LAMBDA != null) {
					filePath = dir3.resolve(new Phases(partial.getPhases()).toString()
						+ "_" + "LAMBDA2MU" + "_" + partial.getSacComponent()
						+ String.format("_kernelTemporal_snapshots_t0%d.txt", (int) t0));
					filePath_LAMBDA = dir3.resolve(new Phases(partial.getPhases()).toString()
							+ "_" + "LAMBDA" + "_" + partial.getSacComponent()
							+ String.format("_kernelTemporal_snapshots_t0%d.txt", (int) t0));
				}
				Path filePath_MU = dir3.resolve(new Phases(partial.getPhases()).toString()
						+ "_" + "MU" + "_" + partial.getSacComponent()
						+ String.format("_kernelTemporal_snapshots_t0%d.txt", (int) t0));
				
				if (!Files.exists(filePath_MU))
					Files.createFile(filePath_MU);
				
				
				Path filePath_sensitivity = null;
				Path filePath_LAMBDA_sensitivity = null;
				if (partial_LAMBDA != null) {
					filePath_sensitivity = dir3.resolve(new Phases(partial.getPhases()).toString()
						+ "_" + "LAMBDA2MU" + "_" + partial.getSacComponent()
						+ String.format("_sensitivityTemporal_snapshots_t0%d.txt", (int) t0));
					filePath_LAMBDA_sensitivity = dir3.resolve(new Phases(partial.getPhases()).toString()
							+ "_" + "LAMBDA" + "_" + partial.getSacComponent()
							+ String.format("_sensitivityTemporal_snapshots_t0%d.txt", (int) t0));
				}
				Path filePath_MU_sensitivity = dir3.resolve(new Phases(partial.getPhases()).toString()
						+ "_" + "MU" + "_" + partial.getSacComponent()
						+ String.format("_sensitivityTemporal_snapshots_t0%d.txt", (int) t0));
				
				if (!Files.exists(filePath_MU_sensitivity))
					Files.createFile(filePath_MU_sensitivity);
				
				double cumulativeSensitivity = 0.;
				double cumulativeSensitivity_MU = 0.;
				double cumulativeSensitivity_LAMBDA = 0.;
				
				if (partial_LAMBDA != null) {
					if (!Files.exists(filePath))
						Files.createFile(filePath);
					if (!Files.exists(filePath_LAMBDA))
						Files.createFile(filePath_LAMBDA);
				
					BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.APPEND);
					writer.write(String.format("%s ", partial.getPerturbationLocation()));
					BufferedWriter writer_s = Files.newBufferedWriter(filePath_sensitivity, StandardOpenOption.APPEND);
					writer_s.write(String.format("%s ", partial.getPerturbationLocation()));
					for (int i = 0; i < data_LAMBDAMU.length; i++) {
						writer.write(String.format("%.5e "
								, data_LAMBDAMU[i]));
						
						cumulativeSensitivity += data_LAMBDAMU[i] * data_LAMBDAMU[i];
						writer_s.write(String.format("%.5e ", Math.sqrt(cumulativeSensitivity) / (i+1)));
					}
					
					BufferedWriter writer3 = Files.newBufferedWriter(filePath_LAMBDA, StandardOpenOption.APPEND);
					writer3.write(String.format("%s ", partial.getPerturbationLocation()));
					BufferedWriter writer3_s = Files.newBufferedWriter(filePath_LAMBDA_sensitivity, StandardOpenOption.APPEND);
					writer3_s.write(String.format("%s ", partial.getPerturbationLocation()));
					for (int i = 0; i < data_LAMBDA.length; i++) {
						writer3.write(String.format("%.5e "
								, data_LAMBDA[i]));
						
						cumulativeSensitivity_LAMBDA += data_LAMBDA[i] * data_LAMBDA[i];
						writer3_s.write(String.format("%.5e ", Math.sqrt(cumulativeSensitivity_LAMBDA) / (i+1)));
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
				
//					String t = String.format("%04d", (int) (t0 + i));
//					Path filePath = dir3.resolve(new Phases(partial.getPhases()).toString() + "_kernelTemporal_snapshot" + t + ".txt");
//					if (!Files.exists(filePath))
//						Files.createFile(filePath);
//					Files.write(filePath, (partial.getPerturbationLocation().toString() + " " + data[i] + "\n").getBytes(), StandardOpenOption.APPEND);
//			}
		}
	}
}
