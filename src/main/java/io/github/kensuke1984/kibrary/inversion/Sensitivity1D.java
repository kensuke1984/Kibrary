package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.ArrayRealVector;

public class Sensitivity1D {
	private Map<PerturbationR_distance, Double> sensitivityMap;
	private PartialID[] ids;
	private Path outPath;
	Set<Phase> includePhases;
	
	public Sensitivity1D(Path partialPath, Path partialIDPath, Path outPath) throws IOException {
		this.sensitivityMap = new HashMap<>();
		this.ids = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialPath);
		this.outPath = outPath;
		compute();
		this.normalizePerDistance();
	}
	
	public Sensitivity1D(PartialID[] ids, Path outPath, Set<Phase> includedPhases) {
		this.ids = ids;
		this.sensitivityMap = new HashMap<>();
		this.outPath = outPath;
		this.includePhases = includedPhases;
		compute();
		this.normalizePerDistance();
	}
	
	public static void main(String[] args) throws IOException {
		Path partialPath = Paths.get("partial.dat");
		Path partialIDPath = Paths.get("partialID.dat");
		Set<Phase> S_ScS = Arrays.asList(Phase.S, Phase.ScS).stream().collect(Collectors.toSet());
		Set<Phase> ScS4 = Arrays.asList(Phase.create("ScSScSScSScS", false)).stream().collect(Collectors.toSet());
		Set<Phase> ScS3 = Arrays.asList(Phase.create("ScSScSScS", false)).stream().collect(Collectors.toSet());
		Set<Phase> SS_SSS = Arrays.asList(Phase.create("SS", false), Phase.create("SSS", false)).stream().collect(Collectors.toSet());
		Set<Phase> all = new HashSet<>();
		
		PartialID[] ids = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialPath);
		
		Path outPath = Paths.get("sensitivityMap-S_ScS.txt");
		Sensitivity1D s1DS_ScS = new Sensitivity1D(ids, outPath, S_ScS);
		s1DS_ScS.write();
		
		outPath = Paths.get("sensitivityMap-ScS4.txt");
		Sensitivity1D s1DScS4 = new Sensitivity1D(ids, outPath, ScS4);
		s1DScS4.write();
		
		outPath = Paths.get("sensitivityMap-ScS3.txt");
		Sensitivity1D s1DScS3 = new Sensitivity1D(ids, outPath, ScS3);
		s1DScS3.write();
		
		outPath = Paths.get("sensitivityMap-SS_SSS.txt");
		Sensitivity1D s1DSS_SSS = new Sensitivity1D(ids, outPath, SS_SSS);
		s1DSS_SSS.write();
		
		outPath = Paths.get("sensitivityMap-all.txt");
		Sensitivity1D s1D_all = new Sensitivity1D(ids, outPath, all);
		s1D_all.write();
	}
	
	public void write() throws IOException{
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))) {
			sensitivityMap.forEach((rDistance, sensitivity) -> {
				pw.println(rDistance.r + " " + rDistance.distance + " " + sensitivity);
			});
		}
	}
	
	public void compute() {
		for (PartialID id : ids) {
			if (!containPhases(id, includePhases))
				continue;
			PerturbationR_distance rDistance = new PerturbationR_distance(id);
			if (sensitivityMap.containsKey(rDistance)) {
				double sensitivity = idToSensitivity(id)
						+ sensitivityMap.get(rDistance);
				sensitivityMap.replace(rDistance, sensitivity);
			}
			else {
				double sensitivity = idToSensitivity(id);
				sensitivityMap.put(rDistance, sensitivity);
			}
		}
	}
	
	public PartialID[] getIds() {
		return ids;
	}
	
	private boolean containPhases(PartialID id, Set<Phase> includePhases) {
		boolean contains = false;
		for (Phase phase : id.getPhases()) {
			if (includePhases.contains(phase))
				contains = true;
		}
		return contains;
	}
	
	private double getMaxSensitivity() {
		double max = Double.MIN_VALUE;
		for(Map.Entry<PerturbationR_distance, Double> entry : sensitivityMap.entrySet())
			max = Math.max(max, entry.getValue());
		return max;
	}
	
	private Map<Double, Double> getMaxPerDistance() {
		Map<Double, Double> maxPerDistanceMap = new HashMap<>();
		for (int i = 0; i < 180; i+=2)
			maxPerDistanceMap.put((double) i, 0.);
		for(Map.Entry<PerturbationR_distance, Double> entry : sensitivityMap.entrySet()) {
			if (maxPerDistanceMap.get(entry.getKey().distance) < entry.getValue())
				maxPerDistanceMap.put(entry.getKey().distance, entry.getValue());
		}
		return maxPerDistanceMap;
	}
	
	private void normalize() {
		double max = getMaxSensitivity();
		for(Map.Entry<PerturbationR_distance, Double> entry : sensitivityMap.entrySet())
			sensitivityMap.replace(entry.getKey(), entry.getValue() / max);
	}
	
	private void normalizePerDistance() {
		Map<Double, Double> maxPerDistanceMap = getMaxPerDistance();
		for(Map.Entry<PerturbationR_distance, Double> entry : sensitivityMap.entrySet()) {
			double max = maxPerDistanceMap.get(entry.getKey().distance);
			if (max > 0)
				sensitivityMap.replace(entry.getKey(), entry.getValue() / max);
			else
				sensitivityMap.replace(entry.getKey(), Double.NaN);
		}
	}
	
	private static double idToSensitivity(PartialID id) {
		return new ArrayRealVector(id.getData()).dotProduct(new ArrayRealVector(id.getData()));
	}
	
	public static class PerturbationR_distance {
		public double r;
		public double distance;
		public PerturbationR_distance(double r, double distance) {
			this.r = r;
			this.distance = (int) (distance / 2.) * 2;
		}
		public PerturbationR_distance(PartialID id) {
			this.r = id.getPerturbationLocation().getR();
			double distance = (id.getGlobalCMTID().getEvent().getCmtLocation()
					.getEpicentralDistance(id.getStation().getPosition())
					* 180. / Math.PI);
			this.distance = (int) (distance / 2.) * 2;
		}
	}
}
