package io.github.kensuke1984.kibrary.specfem;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.Location;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Yuki
 * 3-D model for specfem specified by Vs perturbation w.r.t. PREM at each location (lon, lat, depth)
 * For the method in model_ppm.f90 to correctly retrieve the Vs perturbations,
 * the 3-D model file must be written in that order:
 * depths (min to max)
 * longitudes (min to max)
 * latitudes (min to max)
 */
public class Make3DModel {
	
	private static double dx = 1.; //horizontal interval distance (deg).
	private static double dvs = -4.; //perturbation w.r.t. PREM

	public static void main(String[] args) {
//		List<PerturbationPoint> oneLayerModel = onePerturbationLayer(3480., 3630., 2.);
//		List<PerturbationPoint> checkerboard = checkerboardDppCACAR_100km(2.);
		List<PerturbationPoint> checkerboard = oneDegEach(dvs);
		Path outpath = Paths.get("/Users/YUKI/Desktop/checkerboard.inf");
		try {
			writeModel(checkerboard, outpath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static List<PerturbationPoint> onePerturbationLayer(double rmin, double rmax, double dvs) {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		int nr = (int) ((rmax - rmin) / dx) + 1;
		for (int k = 0; k < nr; k++) {
			for (int i = -180; i <= 179; i+=1) {
				for (int j = -89; j <= 89; j+=1) {
					Location location = new Location(j, i, rmax - k * 5);
					PerturbationPoint perturbation = new PerturbationPoint(location, dvs);
					perturbations.add(perturbation);
				}
			}
		}
		return perturbations;
	}
	
	public static List<PerturbationPoint> oneDegEach(double dvs) {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		double layerSign = 1;
		for (int k = 0; k <= 1;k++) {
			double r = 3630 - k * 150;
			double lonSign = 1;
			for (int i = 100; i <= 110; i+=1) {
				double latSign = 1;
				for (int j = -5; j <= 5; j+=1) {
					Location location = new Location(j, i, r);
					PerturbationPoint perturbation = new PerturbationPoint(location
							, layerSign * lonSign * latSign * dvs);
					perturbations.add(perturbation);
					latSign *= 1;
				}
				lonSign *= 1;
			}
			if (k < 3)
				layerSign *= 1;
		}
		return perturbations;
	}
	
	public static List<PerturbationPoint> anyStructure(double dvs) {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		double layerSign = 1;
		for (int k = 0; k <= 1;k++) {
			double r = 3780 - k * 300;
			double lonSign = 1;
			for (int i = 0; i <= 160; i+=5) {
				double latSign = 1;
				for (int j = -40; j <= 40; j+=5) {
					Location location = new Location(j, i, r);
					PerturbationPoint perturbation = new PerturbationPoint(location
							, layerSign * lonSign * latSign * dvs);
					perturbations.add(perturbation);
					latSign *= 1;
				}
				lonSign *= 1;
			}
			if (k < 3)
				layerSign *= 1;
		}
		return perturbations;
	}
		
	public static void writeModel(List<PerturbationPoint> perturbations, Path outpath) throws IOException {
		PolynomialStructure prem = PolynomialStructure.PREM;
		PrintWriter writer = new PrintWriter(new FileWriter(outpath.toString()));
		String line = "#lon(deg), lat(deg), depth(km), Vs-perturbation wrt PREM(%), Vs-PREM (km/s)";
		writer.println(line);
		for (PerturbationPoint pp : perturbations) {
			line = pp.toString() + String.format(" %.6f", prem.getVshAt(pp.getLocation().getR()));
			writer.println(line);
		}
		writer.close();
	}
	
	private static class PerturbationPoint {
		Location location;
		double dvs;
		public PerturbationPoint(Location location, double dvs) {
			this.location = location;
			this.dvs = dvs;
		}
		@Override
		public String toString() {
			return String.format("%.5f %.5f %.5f %.6f"
					, location.getLongitude()
					, location.getLatitude()
					, 6371. - location.getR()
					, dvs);
		}
		public Location getLocation() {
			return location;
		}
	}
}