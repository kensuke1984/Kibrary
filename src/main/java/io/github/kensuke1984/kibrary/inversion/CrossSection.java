package io.github.kensuke1984.kibrary.inversion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
		
		Location[] locations = readWavefieldPoints(file);
		double[] values = readWavefieldValues(file);
		double[] rs = readRs(fileR);
		double[] layers = readLayers(fileR);
		Map<Location, Double> valueOfLocation = IntStream.range(0, locations.length)
                .mapToObj(i -> i)
                .collect(Collectors.toMap(i -> locations[i], i -> values[i]));
		
		HorizontalPosition[] extremities = readPosition(positionFile);
		HorizontalPosition startPos = extremities[0];
		HorizontalPosition endPos = extremities[1];
		Location centerLocation = startPos.getMidpoint(endPos)
				.toLocation(Earth.EARTH_RADIUS);
		double azimuth = startPos.getAzimuth(endPos);
//		double theta = 20. * Math.PI/180.;
		double theta = centerLocation.getEpicentralDistance(endPos) / 2.;
		double deltaTheta = Math.toRadians(1.);
		
		CrossSectionLine csline 
			= new CrossSectionLine(centerLocation, theta, azimuth, deltaTheta);
		HorizontalPosition[] positions = csline.getPositions();
		double[] thetaX = csline.getThetaX();
		
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
		
		Location[] crossSectionLocations 
			= new Location[positions.length * rs.length];
		double[] crossSectionValues
			= new double[positions.length * rs.length];
		double[] crossSectionThetaX 
			= new double[positions.length * rs.length];
		Complementation comp = new Complementation();
		
		for (int k = 0; k < positions.length; k++) {
			HorizontalPosition position = positions[k];
			for (int i = 0; i < rs.length; i++) {
				Location location = position.toLocation(rs[i]);
				Location[] nearPoints 
					= comp.getNearest4(locations, location);
				
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
				
				double[] nearPointsValue = new double[nearPoints.length];
				for (int j = 0; j < nearPoints.length; j++)
					nearPointsValue[j] = valueOfLocation.get(nearPoints[j]);
				double value 
					= comp.complement(nearPoints, nearPointsValue, location);
				
				// debug
//					System.out.print(value + " ; ");
//					for (int j = 0; j < nearPoints.length; j++)
//						System.out.print(nearPointsValue[j] + " ");
//					System.out.println();
				//
				
				crossSectionLocations[k * rs.length + i] = location;
				crossSectionValues[k * rs.length + i] = value;
				crossSectionThetaX[k * rs.length + i] = thetaX[k]
						* 180 / Math.PI;
			}
		}
		
		Path outfile = Paths.get("crossSection-" 
				+ file.getFileName().toString());
		try (BufferedWriter writer = Files.newBufferedWriter(outfile)) {
			for (int i = 0; i < crossSectionLocations.length; i++) {
				double r = crossSectionLocations[i].getR();
				double theta_tmp = crossSectionThetaX[i];
				writer.write(String.format("%.1f %.0f %.3e%n"
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
