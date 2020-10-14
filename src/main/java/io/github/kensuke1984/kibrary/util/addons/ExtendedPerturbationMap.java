package io.github.kensuke1984.kibrary.util.addons;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.util.Location;

public class ExtendedPerturbationMap {

	public static void main(String[] args) throws IOException {
		Path perturbationLayerPath = Paths.get(args[0]);
		Path inputPath = Paths.get(args[1]);
		Path outpath = Paths.get("extendedInput.inf");
		
		double[] perturbationRs = readLayers(perturbationLayerPath);
		Map<Location, Double> perturbationMap = readInput(inputPath);
		
		double dL = 4.;
		
		Map<Location, Double> extended = extendedPerturbationMap(perturbationMap, dL, perturbationRs);
		
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (Location loc : extended.keySet())
			pw.println(loc + " " + extended.get(loc));
		pw.close();
	}

	
	public static Map<Location, Double> extendedPerturbationMap(Map<Location, Double> perturbationMap, double dL, double[] perturbationRs) {
		Map<Location, Double> extended = new HashMap<>();
		double minLat = 1e3;
		double maxLat = -1e3;
		double minLon = 1e3;
		double maxLon = -1e3;
		
		Set<Location> locations = perturbationMap.keySet();
		for (Location loci : locations) {
			double dvs = perturbationMap.get(loci);
			extended.put(loci, dvs);
			
			if (loci.getLatitude() < minLat)
				minLat = loci.getLatitude();
			if (loci.getLongitude() < minLon)
				minLon = loci.getLongitude();
			if (loci.getLatitude() > maxLat)
				maxLat = loci.getLatitude();
			if (loci.getLongitude() > maxLon)
				maxLon = loci.getLongitude();
			
			Location[] additionalLocs = new Location[] {new Location(loci.getLatitude(), loci.getLongitude() + dL, loci.getR())
			, new Location(loci.getLatitude(), loci.getLongitude() - dL, loci.getR())
			, new Location(loci.getLatitude() + dL, loci.getLongitude(), loci.getR())
			, new Location(loci.getLatitude() - dL, loci.getLongitude(), loci.getR())
			, new Location(loci.getLatitude() + dL, loci.getLongitude() + dL, loci.getR())
			, new Location(loci.getLatitude() + dL, loci.getLongitude() - dL, loci.getR())
			, new Location(loci.getLatitude() - dL, loci.getLongitude() + dL, loci.getR())
			, new Location(loci.getLatitude() - dL, loci.getLongitude() - dL, loci.getR())};
			
			Set<Location> thisRLocations = locations.stream()
					.filter(loc -> loc.getR() == loci.getR()).collect(Collectors.toSet());
			boolean[] isAdds = new boolean[additionalLocs.length];
			for (int j = 0; j < isAdds.length; j++)
				isAdds[j] = true;
			for (Location loc : thisRLocations) {
				for (int k = 0; k < additionalLocs.length; k++) {
					if (loc.equals(additionalLocs[k]))
						isAdds[k] = false;
				}
			}
			
				for (int j = 0; j < additionalLocs.length; j++) {
					if (isAdds[j] && !extended.containsKey(additionalLocs[j])) {
						extended.put(additionalLocs[j], 0.);
					}
				}
		}
		
		// fill remaining voxels with NaN
		int nLon = (int) ((maxLon - minLon) / dL) + 3;
		int nLat = (int) ((maxLat - minLat) / dL) + 3;
		for (int k=0; k < perturbationRs.length; k++) {
			double r = perturbationRs[k];
			for (int i=0; i < nLon; i++) {
				for (int j=0; j < nLat; j++) {
					double lon = minLon + (i - 1) * dL;
					double lat = minLat + (j - 1) * dL;
					Location loc = new Location(lat, lon, r);
					if (!extended.containsKey(loc))
						extended.put(loc, Double.NaN);
				}
			}
		}

		return extended;
	}
	
	public static double[] readLayers(Path perturbationLayerPath) throws IOException {
		List<Double> rList = Files.readAllLines(perturbationLayerPath).stream().map(s -> Double.parseDouble(s.trim().split(" ")[0]))
				.collect(Collectors.toList());
		double[] rs = new double[rList.size()];
		for (int i = 0; i < rs.length; i++)
			rs[i] = rList.get(i);
		
		return rs;
	}
	
	public static Map<Location, Double> readInput(Path inputPath) throws IOException {
		Map<Location, Double> perturbationMap = new HashMap<>();
		Files.readAllLines(inputPath).stream().forEach(s -> {
			double lat = Double.parseDouble(s.trim().split(" ")[0]);
			double lon = Double.parseDouble(s.trim().split(" ")[1]);
			double r = Double.parseDouble(s.trim().split(" ")[2]);
			double dvs = Double.parseDouble(s.trim().split(" ")[3]);
			perturbationMap.put(new Location(lat, lon, r), dvs);
		});
		
		return perturbationMap;
	}
}
