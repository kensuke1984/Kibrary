package io.github.kensuke1984.kibrary.specfem;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.addons.EventCluster;

import java.io.FileWriter;
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
		
//		List<UnknownParameter> unknowns = UnknownParameterFile.read(Paths.get(args[0]));
//		
//		List<PerturbationPoint> checkerboard_4deg_4layers = checkerboardCATZ_4deg_4layers(2., unknowns);
//		Path outpath_4deg_4layers = Paths.get("checkerboard_4x4_4layers_2per.inf");
//		
//		List<PerturbationPoint> checkerboard_4deg_8layers = checkerboardCATZ_4deg_8layers(2., unknowns);
//		Path outpath_4deg_8layers = Paths.get("checkerboard_4x4_8layers_2per.inf");
		
//		List<PerturbationPoint> checkerboard = checkerboardCATZ_6deg_4layers(3., unknowns);
//		Path outpath = Paths.get("checkerboard_6x6_4layers_3per.inf");
		
//		List<PerturbationPoint> lowVelocityLens = velocityLens_CA(-5);
//		Path outpath_lowvelocitylens = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/low_velocity_lens_5per.inf");
		
//		List<PerturbationPoint> hlh = model_hlh();
//		Path outpath_hlh = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_HLH/model_hlh.txt");
		
//		List<PerturbationPoint> cl4 = from1DmodelsCl4();
//		Path outpath_cl4 = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/model_cl4.txt");
		
//		List<PerturbationPoint> cl3 = from1DmodelsCl3();
//		Path outpath_cl3 = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL3/model_cl3.txt");
		
		List<PerturbationPoint> cl4_simple = from1DmodelsCl4_simple();
		Path outpath_cl4_simple = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL3/model_cl4_simple.txt");
		
		List<PerturbationPoint> cl3_simple = from1DmodelsCl3_simple();
		Path outpath_cl3_simple = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL3/model_cl3_simple.txt");
		
		List<PerturbationPoint> cl5_simple = from1DmodelsCl5_simple();
		Path outpath_cl5_simple = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL3/model_cl5_simple.txt");
		
//		try {
//			writeModel(checkerboard_4deg_4layers, outpath_4deg_4layers);
//			writeModel(checkerboard_4deg_8layers, outpath_4deg_8layers);
//			writeModel(lowVelocityLens, outpath_lowvelocitylens);
//			writeModel(hlh, outpath_hlh);
//			writeModel(cl4, outpath_cl4);
//			writeModel(cl3, outpath_cl3);
//			writeModel(cl4_simple, outpath_cl3_simple);
//			writeModel(cl3_simple, outpath_cl3_simple);
//			writeModel(cl5_simple, outpath_cl5_simple);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
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
	
	public static List<PerturbationPoint> velocityLens_CA(double dvs) {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		
		double lonmin = -77;
		double lonmax = -74;
		double latmin = 6;
		double latmax = 12;
		double dl = 1;
		int nlat = (int) ((latmax - latmin) / dl) + 1;
		int nlon = (int) ((lonmax - lonmin) / dl) + 1;
		
		double h = 50;
		double[] depths = new double[] {2891 - h, 2891};
		
		for (double depth : depths) {
			for (int ilon = 0; ilon <nlon; ilon++) {
				double lon = lonmin + ilon * dl;
				for (int ilat = 0; ilat < nlat; ilat++) {
					double lat = latmin + ilat * dl;
					Location loc = new Location(lat, lon, 6371 - depth);
					perturbations.add(new PerturbationPoint(loc, dvs));
				}
			}
		}
		
		return perturbations;
	}
	
	public static List<PerturbationPoint> from1DmodelsCl4() {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		
		Path eventClusterPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster-6deg.inf");
		Path root = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster4/oneDPartial_sw_it1/inversion/each_6deg/8s/SPECTRUM/NEW/");
		Path root2 = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/oneDPartialPREM_Q165/inversion/40km/8s/each_3deg/NEW/");
		Path cl4az0Path = root.resolve("lmi_7279_AB_vs_cl4_az0_l04_g02_lq12_gq06_semucb_ef1.5_noAmpCorr");
		Path cl4az1Path = root.resolve("lmi_7279_AB_vs_cl4_az1_l04_g02_lq12_gq06_semucb_ef1.5_noAmpCorr");
		Path cl4az2Path = root.resolve("lmi_7279_AB_vs_cl4_az2_l04_g02_lq12_gq06_semucb_ef1.5_noAmpCorr");
		Path cl4az3Path = root2.resolve("lmi_7079_AB_vs_cl4_az3_l02_g02_lq06_gq08_semucb_noAmpCorr");
		Path cl4az4Path = root.resolve("lmi_7279_AB_vs_cl4_az4_l04_g02_lq12_gq06_semucb_ef1.5_noAmpCorr");
		Path cl4az5Path = root.resolve("lmi_7279_AB_vs_cl4_az5_l04_g02_lq12_gq06_semucb_ef1.5_noAmpCorr");
		
		List<EventCluster> clusters = null;
		try {
			clusters = EventCluster.readClusterFile(eventClusterPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<Double, Double> vel0 = readVel(cl4az0Path);
		Map<Double, Double> vel1 = readVel(cl4az1Path);
		Map<Double, Double> vel2 = readVel(cl4az2Path);
		Map<Double, Double> vel3 = readVel(cl4az3Path);
		Map<Double, Double> vel4 = readVel(cl4az4Path);
		Map<Double, Double> vel5 = readVel(cl4az5Path);
		
		double lonmin = -120;
		double lonmax = -50;
		double latmin = -20;
		double latmax = 40;
		double dl = 0.25;
		int nlat = (int) ((latmax - latmin) / dl) + 1;
		int nlon = (int) ((lonmax - lonmin) / dl) + 1;
		
		double h = 40;
		
		double[] depths = new double[] {2371,2411,2451,2491,2531,2571,2611,2651,2691,2731,2771,2811,2851,2891};
		
		EventCluster cluster = clusters.stream().filter(c -> c.getIndex() == 4).findFirst().get();
		
		for (double depth : depths) {
			for (int ilon = 0; ilon <nlon; ilon++) {
				double lon = lonmin + ilon * dl;
				for (int ilat = 0; ilat < nlat; ilat++) {
					double lat = latmin + ilat * dl;
					Location loc = new Location(lat, lon, 6371 - depth);
					double dv = 0;
					if (depth == 2371)
						dv = 0.;
					else {
						double azimuth = Math.toDegrees(cluster.getCenterPosition().getAzimuth(loc));
						if (azimuth < 180) azimuth += 360;
						
						int iaz = -1;
						for (int i = 0; i < 6; i++) {
							double azmin = cluster.getAzimuthBound(i)[0];
							double azmax = cluster.getAzimuthBound(i)[1];
							
	//						if (i == 0) azmin = 0;
							if (i == 5) azmax = 720;
							if (azimuth >= azmin && azimuth < azmax) iaz = i;
						}
						
						double tmpdepth = depth == 2891 ? depth : depth + h;
						
						switch (iaz) {
						case 0:
							dv = vel0.get(tmpdepth);
							break;
						case 1:
							dv = vel1.get(tmpdepth);
							break;
						case 2:
							dv = vel2.get(tmpdepth);
							break;
						case 3:
							dv = vel3.get(tmpdepth);
							break;
						case 4:
							dv = vel4.get(tmpdepth);
							break;
						case 5:
							dv = vel5.get(tmpdepth);
							break;
						default:
							break;
						}
					}
					
					perturbations.add(new PerturbationPoint(loc, dv));
				}
			}
		}
		
		return perturbations;
	}
	
	public static List<PerturbationPoint> from1DmodelsCl4_simple() {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		
		Path eventClusterPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster-6deg.inf");
		Path root = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster4/oneDPartial_sw_it1/inversion/each_6deg/8s/SPECTRUM/NEW/");
		Path root2 = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/oneDPartialPREM_Q165/inversion/40km/8s/each_3deg/NEW/");
		Path cl4az0Path = root.resolve("lmi_7279_AB_vs_cl4_az0_l04_g02_lq12_gq06_semucb_ef1.5_noAmpCorr");
		Path cl4az1Path = root.resolve("lmi_7279_AB_vs_cl4_az1_l04_g02_lq12_gq06_semucb_ef1.5_noAmpCorr");
		Path cl4az2Path = root.resolve("lmi_7279_AB_vs_cl4_az2_l04_g02_lq12_gq06_semucb_ef1.5_noAmpCorr");
		Path cl4az3Path = root2.resolve("lmi_7079_AB_vs_cl4_az3_l02_g02_lq06_gq08_semucb_noAmpCorr");
		Path cl4az4Path = root.resolve("lmi_7279_AB_vs_cl4_az4_l04_g02_lq12_gq06_semucb_ef1.5_noAmpCorr");
		Path cl4az5Path = root.resolve("lmi_7279_AB_vs_cl4_az5_l04_g02_lq12_gq06_semucb_ef1.5_noAmpCorr");
		
		List<EventCluster> clusters = null;
		try {
			clusters = EventCluster.readClusterFile(eventClusterPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<Double, Double> vel0 = readVel(cl4az0Path);
		Map<Double, Double> vel1 = readVel(cl4az1Path);
		Map<Double, Double> vel2 = readVel(cl4az2Path);
		Map<Double, Double> vel3 = readVel(cl4az3Path);
		Map<Double, Double> vel4 = readVel(cl4az4Path);
		Map<Double, Double> vel5 = readVel(cl4az5Path);
		
//		double dv0 = vel0.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv0 /= vel0.values().size();
//		double dv1 = vel1.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv1 /= vel1.values().size();
//		double dv2 = vel2.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv2 /= vel2.values().size();
//		double dv3 = vel3.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv3 /= vel3.values().size();
//		double dv4 = vel4.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv4 /= vel4.values().size();
//		double dv5 = vel5.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv5 /= vel5.values().size();
//		
//		double dv_high_cmb = (dv0 + dv1 + dv2 + dv4 + dv5) / 5. * (2891 - 2400) / 150. * 2;
//		double dv_low_cmb = dv3 * (2891 - 2400) / 150. * 2;
//		
//		System.out.println(dv_high_cmb + " " + dv_low_cmb);
		
		Map<Double, Double> vel_high = new HashMap<>();
		Map<Double, Double> vel_low = vel3;
		
		PolynomialStructure model = null;
		try {
			model = new PolynomialStructure(Paths.get("/work/anselme/POLY/sw_it1.poly"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (Double depth : vel0.keySet()) {
			double dv = 0;
			if (depth > 2600)
				dv = (vel0.get(depth) + vel1.get(depth) + vel2.get(depth) + vel4.get(depth) + vel5.get(depth)) / 5.;
			else {
				double r = 6371 - depth;
				dv = (model.getVshAt(r) - PolynomialStructure.PREM.getVshAt(r)) / PolynomialStructure.PREM.getVshAt(r) * 100;
			}
			vel_high.put(depth, dv);
		}
		
		Path outpath_low = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/model_low.txt");
		Path outpath_high = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/model_high.txt");
		try {
			PrintWriter pw_low = new PrintWriter(outpath_low.toFile());
			PrintWriter pw_high = new PrintWriter(outpath_high.toFile());
			
			List<Double> sortedDepths = vel0.keySet().stream().collect(Collectors.toList());
			Collections.sort(sortedDepths);
			
			for (Double depth : sortedDepths) {
				double vlow = PolynomialStructure.PREM.getVshAt(6371 - depth) * (1 + vel_low.get(depth) / 100.);
				double vhigh = new PolynomialStructure(Paths.get("/work/anselme/POLY/sw_it1.poly")).getVshAt(6371. - depth)
						* (1 + vel_high.get(depth) / 100.);
				pw_low.println(depth + " " + vlow + " " + vlow + " " + vlow);
				pw_high.println(depth + " " + vhigh + " " + vhigh + " " + vhigh);
			}
			
			pw_low.close();
			pw_high.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		double lonmin = -120;
		double lonmax = -50;
		double latmin = -20;
		double latmax = 40;
		double dl = 0.25;
		int nlat = (int) ((latmax - latmin) / dl) + 1;
		int nlon = (int) ((lonmax - lonmin) / dl) + 1;
		
		double h = 40;
		
		double[] depths = new double[] {2371,2411,2451,2491,2531,2571,2611,2651,2691,2731,2771,2811,2851,2891};
		
		EventCluster cluster = clusters.stream().filter(c -> c.getIndex() == 4).findFirst().get();
		
		for (double depth : depths) {
			for (int ilon = 0; ilon <nlon; ilon++) {
				double lon = lonmin + ilon * dl;
				for (int ilat = 0; ilat < nlat; ilat++) {
					double lat = latmin + ilat * dl;
					Location loc = new Location(lat, lon, 6371 - depth);
					double dv = 0;
					if (depth == 2371)
						dv = 0.;
					else {
						double azimuth = Math.toDegrees(cluster.getCenterPosition().getAzimuth(loc));
						if (azimuth < 180) azimuth += 360;
						
						int iaz = -1;
						for (int i = 0; i < 6; i++) {
							double azmin = cluster.getAzimuthBound(i)[0];
							double azmax = cluster.getAzimuthBound(i)[1];
							
	//						if (i == 0) azmin = 0;
							if (i == 5) azmax = 720;
							if (azimuth >= azmin && azimuth < azmax) iaz = i;
						}
						
						double tmpdepth = depth == 2891 ? depth : depth + h;
						
						switch (iaz) {
						case 0:
							dv = vel_high.get(tmpdepth);
							break;
						case 1:
							dv = vel_high.get(tmpdepth);
							break;
						case 2:
							dv = vel_high.get(tmpdepth);
							break;
						case 3:
							dv = vel_low.get(tmpdepth);
							break;
						case 4:
							dv = vel_high.get(tmpdepth);
							break;
						case 5:
							dv = vel_high.get(tmpdepth);
							break;
						default:
							break;
						}
					}
					
					perturbations.add(new PerturbationPoint(loc, dv));
				}
			}
		}
		
		return perturbations;
	}
	
	
	
	public static List<PerturbationPoint> from1DmodelsCl3() {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		
		Path eventClusterPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster-6deg.inf");
		Path root = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster3/oneDPartial_cl3s0_it2/inversion/40km/8s/NEW");
		Path root2 = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/oneDPartialPREM_Q165/inversion/40km/8s/each_3deg/NEW");
		Path cl3az0Path = root.resolve("lmi_7079_AB_vs_cl3_az0_l08_g04_lq12_gq12_semucb_noAmpCorr");
		Path cl3az1Path = root.resolve("lmi_7079_AB_vs_cl3_az1_l08_g04_lq12_gq12_semucb_noAmpCorr");
		Path cl3az2Path = root.resolve("lmi_7079_AB_vs_cl3_az2_l08_g04_lq12_gq12_semucb_noAmpCorr");
		Path cl3az3Path = root.resolve("lmi_7079_AB_vs_cl3_az3_l08_g04_lq12_gq12_semucb_noAmpCorr");
		Path cl3az4Path = root2.resolve("lmi_7079_4AB_vs_cl3_az4_cl5_az1_l04_g02_lq06_gq12_semucb_noAmpCorr");
		Path cl3az5Path = root.resolve("lmi_7079_AB_vs_cl3_az3_l08_g04_lq12_gq12_semucb_noAmpCorr");
		
		List<EventCluster> clusters = null;
		try {
			clusters = EventCluster.readClusterFile(eventClusterPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<Double, Double> vel0 = readVel(cl3az0Path);
		Map<Double, Double> vel1 = readVel(cl3az1Path);
		Map<Double, Double> vel2 = readVel(cl3az2Path);
		Map<Double, Double> vel3 = readVel(cl3az3Path);
		Map<Double, Double> vel4 = readVel(cl3az4Path);
		Map<Double, Double> vel5 = readVel(cl3az5Path);
		
		double lonmin = -120;
		double lonmax = -50;
		double latmin = -20;
		double latmax = 40;
		double dl = 0.25;
		int nlat = (int) ((latmax - latmin) / dl) + 1;
		int nlon = (int) ((lonmax - lonmin) / dl) + 1;
		
		double h = 40;
		
		double[] depths = new double[] {2371,2411,2451,2491,2531,2571,2611,2651,2691,2731,2771,2811,2851,2891};
		
		EventCluster cluster = clusters.stream().filter(c -> c.getIndex() == 3).findFirst().get();
		
		for (double depth : depths) {
			for (int ilon = 0; ilon <nlon; ilon++) {
				double lon = lonmin + ilon * dl;
				for (int ilat = 0; ilat < nlat; ilat++) {
					double lat = latmin + ilat * dl;
					Location loc = new Location(lat, lon, 6371 - depth);
					double dv = 0;
					if (depth == 2371)
						dv = 0.;
					else {
						double azimuth = Math.toDegrees(cluster.getCenterPosition().getAzimuth(loc));
						if (azimuth < 180) azimuth += 360;
						
						int iaz = -1;
						for (int i = 0; i < 6; i++) {
							double azmin = cluster.getAzimuthBound(i)[0];
							double azmax = cluster.getAzimuthBound(i)[1];
							
	//						if (i == 0) azmin = 0;
							if (i == 5) azmax = 720;
							if (azimuth >= azmin && azimuth < azmax) iaz = i;
						}
						
						double tmpdepth = depth == 2891 ? depth : depth + h;
						
						switch (iaz) {
						case 0:
							dv = vel0.get(tmpdepth);
							break;
						case 1:
							dv = vel1.get(tmpdepth);
							break;
						case 2:
							dv = vel2.get(tmpdepth);
							break;
						case 3:
							dv = vel3.get(tmpdepth);
							break;
						case 4:
							dv = vel4.get(tmpdepth);
							break;
						case 5:
							dv = vel5.get(tmpdepth);
							break;
						default:
							break;
						}
					}
					
					perturbations.add(new PerturbationPoint(loc, dv));
				}
			}
		}
		
		return perturbations;
	}
	
	public static List<PerturbationPoint> from1DmodelsCl3_simple() {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		
		Path eventClusterPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster-6deg.inf");
		Path root = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster3/oneDPartial_cl3s0_it2/inversion/40km/8s/NEW");
		Path root2 = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/oneDPartialPREM_Q165/inversion/40km/8s/each_3deg/NEW");
		Path cl3az0Path = root.resolve("lmi_7079_AB_vs_cl3_az0_l08_g04_lq12_gq12_semucb_noAmpCorr");
		Path cl3az1Path = root.resolve("lmi_7079_AB_vs_cl3_az1_l08_g04_lq12_gq12_semucb_noAmpCorr");
		Path cl3az2Path = root.resolve("lmi_7079_AB_vs_cl3_az2_l08_g04_lq12_gq12_semucb_noAmpCorr");
		Path cl3az3Path = root.resolve("lmi_7079_AB_vs_cl3_az3_l08_g04_lq12_gq12_semucb_noAmpCorr");
		Path cl3az4Path = root2.resolve("lmi_7079_4AB_vs_cl3_az4_cl5_az1_l04_g02_lq06_gq12_semucb_noAmpCorr");
		Path cl3az5Path = root.resolve("lmi_7079_AB_vs_cl3_az3_l08_g04_lq12_gq12_semucb_noAmpCorr");
		
		List<EventCluster> clusters = null;
		try {
			clusters = EventCluster.readClusterFile(eventClusterPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<Double, Double> vel0 = readVel(cl3az0Path);
		Map<Double, Double> vel1 = readVel(cl3az1Path);
		Map<Double, Double> vel2 = readVel(cl3az2Path);
		Map<Double, Double> vel3 = readVel(cl3az3Path);
		Map<Double, Double> vel4 = readVel(cl3az4Path);
		Map<Double, Double> vel5 = readVel(cl3az5Path);
		
//		double dv0 = vel0.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv0 /= vel0.values().size();
//		double dv1 = vel1.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv1 /= vel1.values().size();
//		double dv2 = vel2.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv2 /= vel2.values().size();
//		double dv3 = vel3.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv3 /= vel3.values().size();
//		double dv4 = vel4.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv4 /= vel4.values().size();
//		double dv5 = vel5.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv5 /= vel5.values().size();
//		
//		double dv_high_cmb = (dv0 + dv1 + dv2 + dv3 + dv5) / 5. * (2891 - 2400) / 150. * 2;
//		double dv_low_cmb = dv4 * (2891 - 2400) / 150. * 2;
		
//		System.out.println(dv_high_cmb + " " + dv_low_cmb);
		
		Map<Double, Double> vel_high = new HashMap<>();
		Map<Double, Double> vel_low = vel4;
		
		for (Double depth : vel0.keySet()) {
			double dv = 0;
			if (depth > 2600)
				dv = (vel0.get(depth) + vel1.get(depth) + vel2.get(depth) + vel3.get(depth)) / 4.;
			vel_high.put(depth, dv);
		}
		
		Path outpath_low = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL3/model_low.txt");
		Path outpath_high = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL3/model_high.txt");
		try {
			PrintWriter pw_low = new PrintWriter(outpath_low.toFile());
			PrintWriter pw_high = new PrintWriter(outpath_high.toFile());
			
			List<Double> sortedDepths = vel0.keySet().stream().collect(Collectors.toList());
			Collections.sort(sortedDepths);
			
			for (Double depth : sortedDepths) {
				double vlow = PolynomialStructure.PREM.getVshAt(6371 - depth) * (1 + vel_low.get(depth) / 100.);
				double vhigh = new PolynomialStructure(Paths.get("/work/anselme/POLY/cl3az0_it2.poly")).getVshAt(6371. - depth)
						* (1 + vel_high.get(depth) / 100.);
				pw_low.println(depth + " " + vlow + " " + vlow + " " + vlow);
				pw_high.println(depth + " " + vhigh + " " + vhigh + " " + vhigh);
			}
			
			pw_low.close();
			pw_high.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		double lonmin = -120;
		double lonmax = -50;
		double latmin = -20;
		double latmax = 40;
		double dl = 0.25;
		int nlat = (int) ((latmax - latmin) / dl) + 1;
		int nlon = (int) ((lonmax - lonmin) / dl) + 1;
		
		double h = 40;
		
		double[] depths = new double[] {2371,2411,2451,2491,2531,2571,2611,2651,2691,2731,2771,2811,2851,2891};
		
		EventCluster cluster = clusters.stream().filter(c -> c.getIndex() == 3).findFirst().get();
		
		for (double depth : depths) {
			for (int ilon = 0; ilon <nlon; ilon++) {
				double lon = lonmin + ilon * dl;
				for (int ilat = 0; ilat < nlat; ilat++) {
					double lat = latmin + ilat * dl;
					Location loc = new Location(lat, lon, 6371 - depth);
					double dv = 0;
					if (depth == 2371)
						dv = 0.;
					else {
						double azimuth = Math.toDegrees(cluster.getCenterPosition().getAzimuth(loc));
						if (azimuth < 180) azimuth += 360;
						
						int iaz = -1;
						for (int i = 0; i < 6; i++) {
							double azmin = cluster.getAzimuthBound(i)[0];
							double azmax = cluster.getAzimuthBound(i)[1];
							
	//						if (i == 0) azmin = 0;
							if (i == 5) azmax = 720;
							if (azimuth >= azmin && azimuth < azmax) iaz = i;
						}
						
						double tmpdepth = depth == 2891 ? depth : depth + h;
						
						switch (iaz) {
						case 0:
							dv = vel_high.get(tmpdepth);
							break;
						case 1:
							dv = vel_high.get(tmpdepth);
							break;
						case 2:
							dv = vel_high.get(tmpdepth);
							break;
						case 3:
							dv = vel_high.get(tmpdepth);
							break;
						case 4:
							dv = vel_low.get(tmpdepth);
							break;
						case 5:
							dv = vel_high.get(tmpdepth);
							break;
						default:
							break;
						}
					}
					
					perturbations.add(new PerturbationPoint(loc, dv));
				}
			}
		}
		
		return perturbations;
	}
	
	public static List<PerturbationPoint> from1DmodelsCl5_simple() {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		
		Path eventClusterPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster-6deg.inf");
		Path root = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster3/oneDPartial_cl3s0_it2/inversion/40km/8s/NEW");
		Path root2 = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/oneDPartialPREM_Q165/inversion/40km/8s/each_3deg/NEW");
		Path cl3az0Path = root.resolve("lmi_7079_AB_vs_cl3_az0_l08_g04_lq12_gq12_semucb_noAmpCorr");
		Path cl3az1Path = root.resolve("lmi_7079_AB_vs_cl3_az1_l08_g04_lq12_gq12_semucb_noAmpCorr");
		Path cl3az2Path = root.resolve("lmi_7079_AB_vs_cl3_az2_l08_g04_lq12_gq12_semucb_noAmpCorr");
		Path cl3az3Path = root.resolve("lmi_7079_AB_vs_cl3_az3_l08_g04_lq12_gq12_semucb_noAmpCorr");
		Path cl3az4Path = root2.resolve("lmi_7079_4AB_vs_cl3_az4_cl5_az1_l04_g02_lq06_gq12_semucb_noAmpCorr");
		Path cl3az5Path = root.resolve("lmi_7079_AB_vs_cl3_az3_l08_g04_lq12_gq12_semucb_noAmpCorr");
		
		List<EventCluster> clusters = null;
		try {
			clusters = EventCluster.readClusterFile(eventClusterPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<Double, Double> vel0 = readVel(cl3az0Path);
		Map<Double, Double> vel1 = readVel(cl3az1Path);
		Map<Double, Double> vel2 = readVel(cl3az2Path);
		Map<Double, Double> vel3 = readVel(cl3az3Path);
		Map<Double, Double> vel4 = readVel(cl3az4Path);
		Map<Double, Double> vel5 = readVel(cl3az5Path);
		
//		double dv0 = vel0.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv0 /= vel0.values().size();
//		double dv1 = vel1.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv1 /= vel1.values().size();
//		double dv2 = vel2.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv2 /= vel2.values().size();
//		double dv3 = vel3.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv3 /= vel3.values().size();
//		double dv4 = vel4.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv4 /= vel4.values().size();
//		double dv5 = vel5.values().stream().reduce((v1, v2) -> v1 + v2).get();
//		dv5 /= vel5.values().size();
//		
//		double dv_high_cmb = (dv0 + dv1 + dv2 + dv3 + dv5) / 5. * (2891 - 2400) / 150. * 2;
//		double dv_low_cmb = dv4 * (2891 - 2400) / 150. * 2;
		
//		System.out.println(dv_high_cmb + " " + dv_low_cmb);
		
		Map<Double, Double> vel_high = new HashMap<>();
		Map<Double, Double> vel_low = vel4;
		
		for (Double depth : vel0.keySet()) {
			double dv = 0;
			if (depth > 2600)
				dv = (vel0.get(depth) + vel1.get(depth) + vel2.get(depth) + vel3.get(depth)) / 4.;
			vel_high.put(depth, dv);
		}
		
		double lonmin = -120;
		double lonmax = -50;
		double latmin = -20;
		double latmax = 40;
		double dl = 0.25;
		int nlat = (int) ((latmax - latmin) / dl) + 1;
		int nlon = (int) ((lonmax - lonmin) / dl) + 1;
		
		double h = 40;
		
		double[] depths = new double[] {2371,2411,2451,2491,2531,2571,2611,2651,2691,2731,2771,2811,2851,2891};
		
		EventCluster cluster = clusters.stream().filter(c -> c.getIndex() == 5).findFirst().get();
		
		for (double depth : depths) {
			for (int ilon = 0; ilon <nlon; ilon++) {
				double lon = lonmin + ilon * dl;
				for (int ilat = 0; ilat < nlat; ilat++) {
					double lat = latmin + ilat * dl;
					Location loc = new Location(lat, lon, 6371 - depth);
					double dv = 0;
					if (depth == 2371)
						dv = 0.;
					else {
						double azimuth = Math.toDegrees(cluster.getCenterPosition().getAzimuth(loc));
						if (azimuth < 180) azimuth += 360;
						
						int iaz = -1;
						for (int i = 0; i < 3; i++) {
							double azmin = cluster.getAzimuthBound(i)[0];
							double azmax = cluster.getAzimuthBound(i)[1];
							
	//						if (i == 0) azmin = 0;
							if (i == 2) azmax = 720;
							if (azimuth >= azmin && azimuth < azmax) iaz = i;
						}
						
						double tmpdepth = depth == 2891 ? depth : depth + h;
						
						switch (iaz) {
						case 0:
							dv = vel_high.get(tmpdepth);
							break;
						case 1:
							dv = vel_low.get(tmpdepth);
							break;
						case 2:
							dv = vel_high.get(tmpdepth);
							break;
						default:
							break;
						}
					}
					
					perturbations.add(new PerturbationPoint(loc, dv));
				}
			}
		}
		
		return perturbations;
	}
	
	private static Map<Double, Double> readVel(Path inversionRootPath) {
		PolynomialStructure model = PolynomialStructure.PREM;
		Map<Double, Double> velMap = new HashMap<Double, Double>();
		List<String> lines = null;
		try {
			lines = Files.readAllLines(inversionRootPath.resolve("CG/velocityCG12.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] ss = lines.get(1).split("\\s+");
		double r = Double.valueOf(ss[0]);;
		double depth = 6371. - r;
		double dv = (Double.valueOf(ss[1]) - model.getVshAt(r)) / Double.valueOf(ss[2]) * 100;
		velMap.put(depth, dv);
		for (int i = 3; i < lines.size() - 1; i += 2) {
			ss = lines.get(i).split("\\s+");
			r = Double.valueOf(ss[0]);
			depth = 6371. - r;
			dv = (Double.valueOf(ss[1]) - model.getVshAt(r)) / Double.valueOf(ss[2]) * 100;
			velMap.put(depth, dv);
		}
		ss = lines.get(lines.size() - 1).split("\\s+");
		depth = 6371. - Double.valueOf(ss[0]);
		dv = 0.;
		velMap.put(depth, dv);
		return velMap;
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
	
	public static List<PerturbationPoint> model_hlh() {
		List<PerturbationPoint> perturbations = new ArrayList<>();
		
		double lonmin = -84;
		double lonmax = -64;
		double latmin = 2;
		double latmax = 9;
		double dl = .25;
		int nlat = (int) ((latmax - latmin) / dl) + 1;
		int nlon = (int) ((lonmax - lonmin) / dl) + 1;
		
		double h = 250;
		double[] depths = new double[] {2891 - h, 2891};
		
		for (double depth : depths) {
			for (int ilon = 0; ilon <nlon; ilon++) {
				double lon = lonmin + ilon * dl;
				for (int ilat = 0; ilat < nlat; ilat++) {
					double lat = latmin + ilat * dl;
					Location loc = new Location(lat, lon, 6371 - depth);
					double dvs = 0;
					if (lat >= 3 && lat < 8) {
						if (lon >= -83 && lon < -78)
							dvs = 2.5;
						else if (lon >= -78 && lon < -73)
							dvs = -1.5;
						else if (lon >= -73 && lon < -65)
							dvs = 3;
					}
					perturbations.add(new PerturbationPoint(loc, dvs));
				}
			}
		}
		
		return perturbations;
	}
}


