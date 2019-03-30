package io.github.kensuke1984.kibrary.specfem;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author Anselme
 * 3-D model for specfem specified by Vs perturbation w.r.t. PREM at each location (lon, lat, depth)
 * For the method in model_ppm.f90 to correctly retrieve the Vs perturbations,
 * the 3-D model file must be written in that order:
 * depths (min to max)
 * longitudes (min to max)
 * latitudes (min to max)
 */
public class Make3DModel {

	public static void main(String[] args) throws IOException {
//		List<PerturbationPoint> oneLayerModel = onePerturbationLayer(3480., 3630., 2.);
//		List<PerturbationPoint> checkerboard = checkerboardDppCACAR_100km(1.);
//		Path outpath = Paths.get("/Users/Anselme/checkerboard_5_100km_1per.inf");
		
//		double[] depths = new double[] {330, 410, 535, 660, 820};
//		double dD = 5.;
//		double nD = 0;
//		for (int i = 0; i < depths.length-1; i++) {
//			nD += (int) ((depths[i+1] - depths[i]) / dD);
//			System.out.println(depths[i+1] - depths[i]);
//		}
//		System.out.println(nD);
//		System.exit(0);
		
		List<UnknownParameter> unknowns = UnknownParameterFile.read(Paths.get(args[0]));
		
		List<PerturbationPoint> checkerboard_4deg_4layers = checkerboardCATZ_4deg_4layers(2., unknowns);
		Path outpath_4deg_4layers = Paths.get("checkerboard_4x4_4layers_2per.inf");
		
		List<PerturbationPoint> checkerboard_4deg_8layers = checkerboardCATZ_4deg_8layers(2., unknowns);
		Path outpath_4deg_8layers = Paths.get("checkerboard_4x4_8layers_2per.inf");
		
//		List<PerturbationPoint> checkerboard = checkerboardCATZ_6deg_4layers(3., unknowns);
//		Path outpath = Paths.get("checkerboard_6x6_4layers_3per.inf");
		
		try {
			writeModel(checkerboard_4deg_4layers, outpath_4deg_4layers);
			writeModel(checkerboard_4deg_8layers, outpath_4deg_8layers);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static List<PerturbationPoint> onePerturbationLayer(double rmin, double rmax, double dvs) {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		int nr = (int) ((rmax - rmin) / 5.) + 1;
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
	
	
	/**
	 * @can checkerboardDppCACAR_2 is the equivalent but simplified version
	 * @param dvs
	 * @return
	 */
	@Deprecated
	public static List<PerturbationPoint> checkerboardDppCACAR(double dvs) {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		for (int k = 0; k < 41;k++) {
			double r = 3880 - k * 10;
			double layerSign = Math.pow(-1, k / 5);
			if (k == 40)
				layerSign = Math.pow(-1, (k-1) / 5);
			double iCount = 0;
			double lonSign = 1;
			for (int i = -180; i <= 179; i+=1) {
				if (iCount == 5) {
					iCount = 0;
					lonSign *= -1;
				}
				iCount++;
				double jCount = 0;
				double latSign = 1;
				for (int j = -88; j <= 89; j++) {
					if (jCount == 5) {
						jCount = 0;
						latSign *= -1;
					}
					jCount++;
					Location location = new Location(j+.5, i, r);
					PerturbationPoint perturbation = new PerturbationPoint(location
							, layerSign * dvs);
//							, layerSign * lonSign * latSign * dvs);
					perturbations.add(perturbation);
				}
			}
		}
		return perturbations;
	}
	
	public static List<PerturbationPoint> checkerboardDppCACAR_2(double dvs) {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		double layerSign = 1;
		for (int k = 0; k <= 8;k++) {
			double r = 3880 - k * 50;
			double lonSign = 1;
			for (int i = -180; i <= 175; i+=5) {
				double latSign = 1;
				for (int j = -88; j <= 87; j+=5) {
					Location location = new Location(j+.5, i, r);
					PerturbationPoint perturbation = new PerturbationPoint(location
							, layerSign * dvs);
//							, layerSign * lonSign * latSign * dvs);
					perturbations.add(perturbation);
					latSign *= -1;
				}
				lonSign *= -1;
			}
			if (k != 8)
				layerSign *= -1;
		}
		return perturbations;
	}
	
	public static List<PerturbationPoint> checkerboardDppCACAR_100km(double dvs) {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		double layerSign = 1;
		for (int k = 0; k <= 4;k++) {
			double r = 3880 - k * 100;
			double lonSign = 1;
			for (int i = -180; i <= 175; i+=5) {
				double latSign = 1;
				for (int j = -88; j <= 87; j+=5) {
					Location location = new Location(j+.5, i, r);
					PerturbationPoint perturbation = new PerturbationPoint(location
							, layerSign * lonSign * latSign * dvs);
					perturbations.add(perturbation);
					latSign *= -1;
				}
				lonSign *= -1;
			}
			if (k < 3)
				layerSign *= -1;
		}
		return perturbations;
	}
	
	public static List<PerturbationPoint> checkerboardCATZ_4deg_4layers(double dvs, List<UnknownParameter> unknowns) {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		
		List<HorizontalPosition> positions = unknowns.stream().map(p -> p.getLocation().toHorizontalPosition()).distinct().collect(Collectors.toList());
		
		double[] depths = new double[] {330, 410, 535, 660, 820};
		
		double dD = 5.;
		int nD = 0;
		for (int i = 0; i < depths.length-1; i++) {
			nD += (int) ((depths[i+1] - depths[i]) / dD);
		}
		
		double minLat = 1e3;
		double maxLat = -1e3;
		double minLon = 1e3;
		double maxLon = -1e3;
		for (HorizontalPosition loci : positions) {
			if (loci.getLatitude() < minLat)
				minLat = loci.getLatitude();
			if (loci.getLongitude() < minLon)
				minLon = loci.getLongitude();
			if (loci.getLatitude() > maxLat)
				maxLat = loci.getLatitude();
			if (loci.getLongitude() > maxLon)
				maxLon = loci.getLongitude();
		}
		
		minLat -= 2;
		maxLat += 2;
		minLon -= 2;
		maxLon += 2;
		
		int nlat = (int) (Math.abs(maxLat - minLat) / 4);
		int nlon = (int) (Math.abs(maxLon - minLon) / 4);
		
		double layerSign = 1;
		int iDepth = 0;
		for (int k = 0; k <= nD;k++) {
			double depth = depths[0] + k  * dD;
			if (depth == depths[iDepth + 1]) {
				iDepth++;
				if (iDepth < 4)
					layerSign *= -1;
			}
			
			double r = 6371. - depth;
			double lonSign = 1;
			for (int i = 0; i <= nlon; i++) {
				double lon = minLon + 4 * i;
				double latSign = 1;
				for (int j = 0; j <= nlat; j++) {
					double lat = minLat + 4 * j;
					
					Location location = new Location(lat, lon, r);
					PerturbationPoint perturbation = new PerturbationPoint(location
							, layerSign * lonSign * latSign * dvs);
					perturbations.add(perturbation);
					latSign *= -1;
				}
				lonSign *= -1;
			}
			
			System.out.println(depth + " " + depths[iDepth] + " " + layerSign);
		}
		
		return perturbations;
	}
	
	public static List<PerturbationPoint> checkerboardCATZ_4deg_8layers(double dvs, List<UnknownParameter> unknowns) {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		
		List<HorizontalPosition> positions = unknowns.stream().map(p -> p.getLocation().toHorizontalPosition()).distinct().collect(Collectors.toList());
		
		double[] depths = new double[] {330, 370, 410, 472.5, 535, 597.5, 660, 740, 820};
		
		double dD = 2.5;
		int nD = 0;
		for (int i = 0; i < depths.length-1; i++) {
			nD += (int) ((depths[i+1] - depths[i]) / dD);
		}
		
		double minLat = 1e3;
		double maxLat = -1e3;
		double minLon = 1e3;
		double maxLon = -1e3;
		for (HorizontalPosition loci : positions) {
			if (loci.getLatitude() < minLat)
				minLat = loci.getLatitude();
			if (loci.getLongitude() < minLon)
				minLon = loci.getLongitude();
			if (loci.getLatitude() > maxLat)
				maxLat = loci.getLatitude();
			if (loci.getLongitude() > maxLon)
				maxLon = loci.getLongitude();
		}
		
		minLat -= 2;
		maxLat += 2;
		minLon -= 2;
		maxLon += 2;
		
		int nlat = (int) (Math.abs(maxLat - minLat) / 4);
		int nlon = (int) (Math.abs(maxLon - minLon) / 4);
		
		double layerSign = 1;
		int iDepth = 0;
		for (int k = 0; k <= nD; k++) {
			double depth = depths[0] + k  * dD;
			if (depth == depths[iDepth + 1]) {
				iDepth++;
				if (iDepth < depths.length - 1)
					layerSign *= -1;
			}
			
			double r = 6371. - depth;
			double lonSign = 1;
			for (int i = 0; i <= nlon; i++) {
				double lon = minLon + 4 * i;
				double latSign = 1;
				for (int j = 0; j <= nlat; j++) {
					double lat = minLat + 4 * j;
					
					Location location = new Location(lat, lon, r);
					PerturbationPoint perturbation = new PerturbationPoint(location
							, layerSign * lonSign * latSign * dvs);
					perturbations.add(perturbation);
					latSign *= -1;
				}
				lonSign *= -1;
			}
			
			System.out.println(depth + " " + depths[iDepth] + " " + layerSign);
		}
		
		return perturbations;
	}
	
	public static List<PerturbationPoint> checkerboardCATZ_6deg_4layers(double dvs, List<UnknownParameter> unknowns) {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		
		List<HorizontalPosition> positions = unknowns.stream().map(p -> p.getLocation().toHorizontalPosition()).distinct().collect(Collectors.toList());
		
		double[] depths = new double[] {330, 410, 535, 660, 820};
		
		double dD = 5.;
		int nD = 0;
		for (int i = 0; i < depths.length-1; i++) {
			nD += (int) ((depths[i+1] - depths[i]) / dD);
		}
		
		double minLat = 1e3;
		double maxLat = -1e3;
		double minLon = 1e3;
		double maxLon = -1e3;
		for (HorizontalPosition loci : positions) {
			if (loci.getLatitude() < minLat)
				minLat = loci.getLatitude();
			if (loci.getLongitude() < minLon)
				minLon = loci.getLongitude();
			if (loci.getLatitude() > maxLat)
				maxLat = loci.getLatitude();
			if (loci.getLongitude() > maxLon)
				maxLon = loci.getLongitude();
		}
		
		// recentering voxels
		minLat -= 3;
		maxLat += 3;
		minLon -= 3;
		maxLon += 3;
		
		int nlat = (int) (Math.abs(maxLat - minLat) / 6);
		int nlon = (int) (Math.abs(maxLon - minLon) / 6);
		
		double layerSign = 1;
		int iDepth = 0;
		for (int k = 0; k <= nD;k++) {
			double depth = depths[0] + k  * dD;
			if (depth == depths[iDepth + 1]) {
				iDepth++;
				if (iDepth < 4)
					layerSign *= -1;
			}
			
			double r = 6371. - depth;
			double lonSign = 1;
			for (int i = 0; i <= nlon; i++) {
				double lon = minLon + 6 * i;
				double latSign = 1;
				for (int j = 0; j <= nlat; j++) {
					double lat = minLat + 6 * j;
					
					Location location = new Location(lat, lon, r);
					PerturbationPoint perturbation = new PerturbationPoint(location
							, layerSign * lonSign * latSign * dvs);
					perturbations.add(perturbation);
					latSign *= -1;
				}
				lonSign *= -1;
			}
			
			System.out.println(depth + " " + depths[iDepth] + " " + layerSign);
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
	
	public List<PerturbationPoint> sortForPPM(List<PerturbationPoint> perturbations) {
		List<PerturbationPoint> sortedList = new ArrayList<>();
		
		double[] rs = perturbations.stream().mapToDouble(p -> p.getLocation().getR()).sorted().toArray();
		double[] lats = perturbations.stream().mapToDouble(p -> p.getLocation().getLatitude()).sorted().toArray();
		double[] lons = perturbations.stream().mapToDouble(p -> p.getLocation().getLongitude()).sorted().toArray();
		
		return sortedList;
	}
}


