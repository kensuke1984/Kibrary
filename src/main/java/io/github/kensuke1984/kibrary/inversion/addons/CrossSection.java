package io.github.kensuke1984.kibrary.inversion.addons;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.kensuke1984.kibrary.external.gmt.CrossSectionLine;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;

public class CrossSection {

	public static void main(String[] args) {
		// file with lat lon depth dVs
		Path file = Paths.get(args[0]);
		// fileR with depths points of the ouput cross-section
		Path fileR = Paths.get(args[1]);
		// positionFile with the start point (lat lon) and end point (lat lon) of the output cross-section
		Path positionFile = Paths.get(args[2]);
		
		boolean resampleHorizontal = false;
		
		double dL = 2.;
		
		double maxSearchRange = Math.toRadians(5.) * 6371.;
		maxSearchRange = 250;
		System.out.println("Maximum search range in vertical direction = " + maxSearchRange + " km");
		
		Location[] locations = readWavefieldPoints(file);
		Location[] finalLocations = locations;
		double[] values = readWavefieldValues(file);
//		double[] values_zeromean = readWavefieldValues_zeromean(file);
		double[] rs = readRs(fileR);
		double[] layers = readLayers(fileR);
		
		HorizontalPosition[] extremities = readPosition(positionFile);
		double lonMin = extremities[0].getLongitude() < extremities[1].getLongitude() ? extremities[0].getLongitude() : extremities[1].getLongitude();
		double lonMax = extremities[0].getLongitude() > extremities[1].getLongitude() ? extremities[0].getLongitude() : extremities[1].getLongitude();
		double latMin = extremities[0].getLatitude() < extremities[1].getLatitude() ? extremities[0].getLatitude() : extremities[1].getLatitude();
		double latMax = extremities[0].getLatitude() > extremities[1].getLatitude() ? extremities[0].getLatitude() : extremities[1].getLatitude();
//		lonMin -= 25;
//		lonMax += 25;
//		latMin -= 25;
//		latMax += 25;
		lonMin -= 5;
		lonMax += 5;
		latMin -= 5;
		latMax += 5;
		
		System.out.println("Using range (lon1,lon2,lat1,lat2) = " + lonMin + " " + lonMax + " " + latMin + " " + latMax);
		
		Map<Location, Double> valueOfLocation = new HashMap<>();
		for (int i = 0; i < locations.length; i++) {
			double lon = locations[i].getLongitude();
			double lat = locations[i].getLatitude();
			if (lon >= lonMin && lon <= lonMax && lat >= latMin && lat <= latMax) {
				if (valueOfLocation.containsKey(locations[i]))
					System.out.println(locations[i] + " " + values[i]);
				valueOfLocation.put(locations[i], values[i]);
			}
		}
		
		locations = valueOfLocation.keySet().toArray(new Location[valueOfLocation.size()]);
		
		System.out.println(valueOfLocation.size() + " points");
//		Map<Location, Double> valueOfLocation = IntStream.range(0, locations.length)
//                .mapToObj(i -> i)
//                .collect(Collectors.toMap(i -> finalLocations[i], i -> values[i]));
//		Map<Location, Double> valueOfLocation_zeromean = IntStream.range(0, locations.length)
//                .mapToObj(i -> i)
//                .collect(Collectors.toMap(i -> finalLocations[i], i -> values_zeromean[i]));
		
		if (resampleHorizontal) {
			valueOfLocation = resampleHorizontal(valueOfLocation, dL);
//			valueOfLocation_zeromean = resampleHorizontal(valueOfLocation_zeromean, dL);
			locations = valueOfLocation.keySet().stream().collect(Collectors.toSet()).toArray(new Location[0]);
		}
		
		HorizontalPosition startPos = extremities[0];
		HorizontalPosition endPos = extremities[1];
		Location centerLocation = startPos.getMidpoint(endPos)
				.toLocation(Earth.EARTH_RADIUS);
		double azimuth = centerLocation.getAzimuth(endPos);
//		double theta = 20. * Math.PI/180.;
		double theta = centerLocation.getEpicentralDistance(endPos) + Math.toRadians(0.);
		double deltaTheta = Math.toRadians(0.5);
		
		System.out.println("Theta = " + Math.toDegrees(theta));
		
		CrossSectionLine csline 
			= new CrossSectionLine(centerLocation, theta, azimuth, deltaTheta);
		HorizontalPosition[] positions = csline.getPositions();
		double[] thetaX = csline.getThetaX();
		
		System.out.println(">Writing crosssection line file");
		Path outfileLine = Paths.get("crosssectionLine-" 
				+ positionFile.getFileName());
		try {
			PrintWriter pw = new PrintWriter(outfileLine.toFile());
			for (HorizontalPosition position : positions)
				pw.println(position.getLongitude() + " " + position.getLatitude());
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		Worker worker = new Worker(positions, rs
//				, locations, valueOfLocation
//				, thetaX);
//		int nThreads = Runtime.getRuntime().availableProcessors();
//		ExecutorService exec = Executors.newFixedThreadPool(nThreads);
//		exec.execute(worker);
//		exec.shutdown();
//		while (!exec.isTerminated())
//			try {
//				Thread.sleep(1000);
//			} catch (Exception e) {
//			}
//		Location[] crossSectionLocations = worker.getCrossSectionLocations();
//		double[] crossSectionThetaX = worker.getCrossSectionThetaX();
//		double[] crossSectionValues = worker.getCrossSectionValues();
		
		
		System.out.println(">Computing crosssection");
		Location[] crossSectionLocations 
			= new Location[positions.length * rs.length];
		double[] crossSectionValues
			= new double[positions.length * rs.length];
		double[] crossSectionValues_zeromean
			= new double[positions.length * rs.length];
		double[] crossSectionThetaX 
			= new double[positions.length * rs.length];
		Complementation comp = new Complementation();
		
		for (int k = 0; k < positions.length; k++) {
			HorizontalPosition position = positions[k];
			for (int i = 0; i < rs.length; i++) {
				Location location = position.toLocation(rs[i]);
				Location[] nearPoints 
					= comp.getNearest4(locations, location, maxSearchRange);
//					= comp.getNearest(locations, location);
//				System.out.println(nearPoints[0] + " " + location);
				
//				double dL = 5.;
//				double dR = layers[i];
//				Location[] nearPoints 
//					= comp.get8CellNodes(locations, location, dR, dL);
				
				// debug
//				System.out.print(location + " : ");
//				for (int j = 0; j < nearPoints.length; j++)
//					System.out.print(nearPoints[j] + ", ");
//				System.out.println();
				//
				
				// Working with the 8-nodes cell !!NOT WORKING!!
//				double dR = 50.;
//				double dL = 2.;
//				nearPoints = comp.get8CellNodes(locations, location, dR, dL);
				
				double[] nearPointsValue = new double[nearPoints.length];
//				double[] nearPointsValue_zeromean = new double[nearPoints.length];
				for (int j = 0; j < nearPoints.length; j++) {
					nearPointsValue[j] = valueOfLocation.get(nearPoints[j]);
//					nearPointsValue_zeromean[j] = valueOfLocation_zeromean.get(nearPoints[j]);
				}
				
				// Working with only the nearest point to enhance horizontal heterogeneities
//				nearPoints = new Location[] {nearPoints[0]};
//				nearPointsValue = new double[] {nearPointsValue[0]};
				
				double value 
					= comp.complement(nearPoints, nearPointsValue, location);
//				double value_zeromean 
//					= comp.complement(nearPoints, nearPointsValue_zeromean, location);
				
				if (Double.isNaN(value)) {
					System.err.println("CS--->" + location + " " + value);
					for (int j = 0; j < nearPoints.length; j++)
						System.err.println(nearPoints[j] + " " + nearPointsValue[j]);
				}
				
//				!!NOT WORKING!!
//				double value 
//				= comp.complementEnhanceHorizontal(nearPoints, nearPointsValue, location);
//				double value_zeromean 
//				= comp.complementEnhanceHorizontal(nearPoints, nearPointsValue_zeromean, location);
				
				// debug
//					System.out.print(value + " ; ");
//					for (int j = 0; j < nearPoints.length; j++)
//						System.out.print(nearPointsValue[j] + " ");
//					System.out.println();
				//
				
				crossSectionLocations[k * rs.length + i] = location;
				crossSectionValues[k * rs.length + i] = value;
//				crossSectionValues_zeromean[k * rs.length + i] = value_zeromean;
				crossSectionThetaX[k * rs.length + i] = thetaX[k]
						* 180 / Math.PI;
			}
			System.out.println((k+1) + "/" + positions.length);
		}
		
		Path outfile = Paths.get("crosssection-" + positionFile.getFileName().toString().split(".txt")[0] + "-" 
				+ file.getFileName());
		try (BufferedWriter writer = Files.newBufferedWriter(outfile)) {
			for (int i = 0; i < crossSectionLocations.length; i++) {
				double r = crossSectionLocations[i].getR();
				double theta_tmp = crossSectionThetaX[i];
//				writer.write(String.format("%.1f %.0f %.4e %.4e%n"
//						, theta_tmp
//						, r
//						, crossSectionValues[i]
//						, crossSectionValues_zeromean[i]));
				writer.write(String.format("%.1f %.0f %.4e%n"
						, theta_tmp
						, r
						, crossSectionValues[i]));
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
	}
	
	private static Location[] readWavefieldPoints(Path file) {
		List<Location> locs = new ArrayList<>();
		try {
			BufferedReader reader = Files.newBufferedReader(file);
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] ss = line.split("\\s+");
				Location loc = new Location(Double.parseDouble(ss[0])
						, Double.parseDouble(ss[1])
						, Double.parseDouble(ss[2]));
				locs.add(loc);
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
		return locs.toArray(new Location[0]);
	}

	private static double[] readWavefieldValues(Path file) {
		List<Double> ys = new ArrayList<>();
		try {
			BufferedReader reader = Files.newBufferedReader(file);
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] ss = line.split("\\s+");
				Double y = Double.parseDouble(ss[3]);
				ys.add(y);
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
		double[] y = new double[ys.size()];
		for (int i = 0; i < y.length; i++)
			y[i] = ys.get(i);
		return y;
	}
	
	private static double[] readWavefieldValues_zeromean(Path file) {
		List<Double> ys = new ArrayList<>();
		try {
			BufferedReader reader = Files.newBufferedReader(file);
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] ss = line.split("\\s+");
				Double y = Double.parseDouble(ss[4]);
				ys.add(y);
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
		double[] y = new double[ys.size()];
		for (int i = 0; i < y.length; i++)
			y[i] = ys.get(i);
		return y;
	}
	
	private static double[] readRs(Path file) {
		List<Double> ys = new ArrayList<>();
		try {
			BufferedReader reader = Files.newBufferedReader(file);
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] ss = line.split("\\s+");
				Double y = Double.parseDouble(ss[0]);
				ys.add(y);
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
		double[] y = new double[ys.size()];
		for (int i = 0; i < y.length; i++)
			y[i] = ys.get(i);
		return y;
	}
	
	private static double[] readLayers(Path file) {
		List<Double> ys = new ArrayList<>();
		try {
			BufferedReader reader = Files.newBufferedReader(file);
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] ss = line.split("\\s+");
				Double y = Double.parseDouble(ss[1]);
				ys.add(y);
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
		double[] y = new double[ys.size()];
		for (int i = 0; i < y.length; i++)
			y[i] = ys.get(i);
		return y;
	}
	
	/**
	 * @param file
	 * lat1 lon1
	 * lat2 lon2
	 * @return
	 */
	private static HorizontalPosition[] readPosition(Path file) {
		HorizontalPosition[] positions = new HorizontalPosition[2];
		try {
			BufferedReader reader = Files.newBufferedReader(file);
			// start position
			String line = reader.readLine();
			String[] ss = line.split("\\s+");
			positions[0] = new HorizontalPosition(Double.parseDouble(ss[0])
					, Double.parseDouble(ss[1]));
			// end position
			line = reader.readLine();
			ss = line.split("\\s+");
			positions[1] = new HorizontalPosition(Double.parseDouble(ss[0])
					, Double.parseDouble(ss[1]));
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
		return positions;
	}
	
	private static Map<Location, Double> resampleHorizontal(Map<Location, Double> perturbationMap, double dL) {
		Complementation comp = new Complementation();
		
		Map<Location, Double> extended = new HashMap<>();
		
		Set<Location> locations = perturbationMap.keySet();
		for (Location loci : locations) {
			double dvs = perturbationMap.get(loci);
			extended.put(loci, dvs);
			
			
			Location[] additionalLocs = new Location[] {new Location(loci.getLatitude(), loci.getLongitude() + dL, loci.getR())
			, new Location(loci.getLatitude(), loci.getLongitude() - dL, loci.getR())
			, new Location(loci.getLatitude() + dL, loci.getLongitude(), loci.getR())
			, new Location(loci.getLatitude() - dL, loci.getLongitude(), loci.getR())
			, new Location(loci.getLatitude() + dL, loci.getLongitude() + dL, loci.getR())
			, new Location(loci.getLatitude() + dL, loci.getLongitude() - dL, loci.getR())
			, new Location(loci.getLatitude() - dL, loci.getLongitude() + dL, loci.getR())
			, new Location(loci.getLatitude() - dL, loci.getLongitude() - dL, loci.getR())
			, new Location(loci.getLatitude() + 2*dL, loci.getLongitude(), loci.getR())
			, new Location(loci.getLatitude() + 2*dL, loci.getLongitude() + dL, loci.getR())
			, new Location(loci.getLatitude() + 2*dL, loci.getLongitude() - dL, loci.getR())
			, new Location(loci.getLatitude() + dL, loci.getLongitude() + 2*dL, loci.getR())
			, new Location(loci.getLatitude() + dL, loci.getLongitude() - 2*dL, loci.getR())
			, new Location(loci.getLatitude(), loci.getLongitude() + 2*dL, loci.getR())
			, new Location(loci.getLatitude(), loci.getLongitude() - 2*dL, loci.getR())
			, new Location(loci.getLatitude() - dL, loci.getLongitude() + 2*dL, loci.getR())
			, new Location(loci.getLatitude() - dL, loci.getLongitude() - 2*dL, loci.getR())
			, new Location(loci.getLatitude() - 2*dL, loci.getLongitude() - dL, loci.getR())
			, new Location(loci.getLatitude() - 2*dL, loci.getLongitude(), loci.getR())
			, new Location(loci.getLatitude() - 2*dL, loci.getLongitude() + dL, loci.getR())};
			
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
		
		double minLat = 1e3;
		double maxLat = -1e3;
		double minLon = 1e3;
		double maxLon = -1e3;
		
		for (Location loci : extended.keySet()) {
			if (loci.getLatitude() < minLat)
				minLat = loci.getLatitude();
			if (loci.getLongitude() < minLon)
				minLon = loci.getLongitude();
			if (loci.getLatitude() > maxLat)
				maxLat = loci.getLatitude();
			if (loci.getLongitude() > maxLon)
				maxLon = loci.getLongitude();
		}
		
		List<HorizontalPosition> newPositions = new ArrayList<>();
		double newDL = dL / 2.;
		int nlat = (int) (Math.abs(maxLat - minLat) / newDL) + 6;
		int nlon = (int) (Math.abs(maxLon - minLon) / newDL) + 6;
		
		for (int ilat = 0; ilat < nlat; ilat++) {
			for (int ilon = 0; ilon < nlon; ilon++) {
				double lon = minLon + (ilon - 3) * newDL;
				double lat = minLat + (ilat - 3) * newDL;
				HorizontalPosition pos = new HorizontalPosition(lat, lon);
				newPositions.add(pos);
			}
		}
		
		List<Double> radii = locations.stream().map(loc -> loc.getR()).distinct().collect(Collectors.toList());
		Collections.sort(radii);
		
		Map<Location, Double> resampled = new HashMap<>();
		
		for (double r : radii) {
			Location[] thisRLocations = locations.stream()
					.filter(loc -> loc.getR() == r).collect(Collectors.toSet()).toArray(new Location[0]);
			for (HorizontalPosition pos : newPositions) {
				Location location = pos.toLocation(r);
				Location[] nearest = comp.getNearest4(thisRLocations, location);
				double[] values = new double[nearest.length];
				for (int i = 0; i < nearest.length; i++) {
					Location loc = nearest[i];
					values[i] = extended.get(loc);
				}
				double value = comp.complement(nearest, values, location);
				
				if (Double.isNaN(value)) {
					System.err.println("--->" + location + " " + value);
					for (int i = 0; i < nearest.length; i++)
						System.err.println(nearest[i] + " " + values[i]);
				}
				
				resampled.put(location, value);
			}
		}
		
		return resampled;
	}
	
	private static class Worker implements Runnable {
		HorizontalPosition[] positions;
		double[] rs;
		Location[] locations;
		Map<Location, Double> valueOfLocation;
		double[] thetaX;
		Location[] crossSectionLocations;
		double[] crossSectionValues;
		double[] crossSectionThetaX;
		
		public Worker(HorizontalPosition[] positions
				, double[] rs, Location[] locations
				, Map<Location, Double> valueOfLocation
				, double[] thetaX) {
			this.positions = positions;
			this.rs = rs;
			this.locations = locations;
			this.valueOfLocation = valueOfLocation;
			this.thetaX = thetaX;
			crossSectionLocations 
				= new Location[positions.length * rs.length];
			crossSectionValues
				= new double[positions.length * rs.length];
			crossSectionThetaX 
				= new double[positions.length * rs.length];
		}
		
		public void run() {
			Complementation comp = new Complementation();
			
			for (int k = 0; k < positions.length; k++) {
				HorizontalPosition position = positions[k];
				for (int i = 0; i < rs.length; i++) {
					Location location = position.toLocation(rs[i]);
					Location[] nearPoints 
						= comp.getNearest4(locations, location);
					double[] nearPointsValue = new double[4];
					for (int j = 0; j < 4; j++)
						nearPointsValue[j] = valueOfLocation.get(nearPoints[j]);
					double value 
						= comp.complement(nearPoints, nearPointsValue, location);
					crossSectionLocations[k * rs.length + i] = location;
					crossSectionValues[k * rs.length + i] = value;
					crossSectionThetaX[k * rs.length + i] = thetaX[k]
							* 180 / Math.PI;
				}
			}
		}
		
		
		public double[] getCrossSectionValues() {
			return this.crossSectionValues;
		}
		
		public double[] getCrossSectionThetaX() {
			return this.crossSectionValues;
		}
		
		public Location[] getCrossSectionLocations() {
			return this.crossSectionLocations;
		}
	}
}
