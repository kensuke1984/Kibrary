package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.addons.EventCluster;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.sc.seis.TauP.SphericalCoords;

public class StaticCorrectionMap {

	public static void main(String[] args) throws IOException {
//		Path fujiStaticPath = Paths.get(args[0]);
		double dl = 1;
		boolean from1D = true;
		Path fujiStaticPath2 = null;
		if (args.length == 2)
			fujiStaticPath2 = Paths.get(args[1]);
		Path clusterPath = null;
		
//		int cluster_index = 4;
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/filtered_12.5-200s");
		int cluster_index = 4;
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/SIMPLE/filtered_12.5-200s_nex576");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL3/SIMPLE/filtered_12.5-200s_nex576");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/SIMPLE/SYNTHETIC_TEST/filtered_12.5s_PPMPREM_CL4");
		
		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/SIMPLE/synthetic_PREM_Q165");
		
		Path outpath = rootPath.resolve("corrections_map_ScS_12.5s.txt");
		Path outpath2 = rootPath.resolve("corrections_corridor_12.5s.txt");
		
//		clusterPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster-6deg.inf");
		clusterPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster-6deg_forSpecfemCorrections.inf");
		
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_FAR_SLAB/filtered_12.5-200s/fujiStaticCorrection_S_60deg.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_astfCCAmpcorr_12.5-200s/fujiStaticCorrection_S_60deg.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_DPP/filtered_12.5-200s/fujiStaticCorrection_ScS.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_HLH/filtered_12.5-200s/fujiStaticCorrection_ScS_70deg.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_MANTLE_NODPP/filtered_12.5-200s/fujiStaticCorrection_ScS.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_UPPER_MANTLE/filtered_stf_12.5-200s/fujiStaticCorrection_S.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_SOURCE_SIDE/filtered_12.5-200s/fujiStaticCorrection_S.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/filtered_12.5-200s/fujiStaticCorrection_ScS.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL3/filtered_12.5-200s/fujiStaticCorrection_ScS_median.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster3/synthetic_cl3s0_it2/filtered_nostf_12.5-200s/fujiStaticCorrection_ScS_with_prem.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL3/SIMPLE/filtered_12.5-200s/fujiStaticCorrection_ScS_longer.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/SIMPLE/filtered_12.5-200s/fujiStaticCorrection_ScS_longer.dat");
		Path fujiStaticPath = rootPath.resolve("fujiStaticCorrection_ScS_longer.dat");
		
		Path fujiStaticPath_low = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/SIMPLE/synthetic_cl4_low/filtered_triangle_12.5-200s/fujiStaticCorrection_ScS.dat");
		Path fujiStaticPath_high = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/SIMPLE/synthetic_cl4_high/filtered_triangle_12.5-200s/fujiStaticCorrection_ScS.dat");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/SIMPLE/synthetic_PREM_Q165");
		
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_FAR_SLAB/filtered_12.5-200s/map");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_astfCCAmpcorr_12.5-200s/corrections");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_DPP/filtered_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_HLH/filtered_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_MANTLE_NODPP/filtered_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_UPPER_MANTLE/filtered_stf_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_SOURCE_SIDE/filtered_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/filtered_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL3/filtered_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster3/synthetic_cl3s0_it2/filtered_nostf_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL3/SIMPLE/filtered_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/SIMPLE/filtered_12.5-200s");
		
		List<EventCluster> clusters = new ArrayList<>();
		Set<Integer> clusterIndexSet = new HashSet<Integer>();
		if (clusterPath != null) clusters = EventCluster.readClusterFile(clusterPath);
		
		EventCluster.writeAzimuthSeparation(clusters, Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/azimuthSeparation_cluster-6deg_forSpecfemCorrections-v2.inf"));
		
		clusterIndexSet.add(cluster_index);
//		clusterIndexSet.add(4);
//		clusterIndexSet.add(5);
		Set<GlobalCMTID> usedIdSet = clusters.stream().filter(c -> clusterIndexSet.contains(c.getIndex())).map(c -> c.getID())
				.collect(Collectors.toSet());
		
//		double distance = 71;
//		Path distanceLinePath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/distance_line_cl4_71deg.txt");
//		HorizontalPosition pos = clusters.stream().filter(c -> c.getIndex() == cluster_index).findFirst().get().getCenterPosition();
//		HorizontalPosition[] line = distanceLine(pos, distance);
//		PrintWriter pw_distance = new PrintWriter(distanceLinePath.toFile());
//		for (int i = 0; i < line.length; i++)
//			pw_distance.println(line[i].getLongitude() + " " + line[i].getLatitude());
//		pw_distance.close();
		
		Set<StaticCorrection> fujiCorrections = new HashSet<>();
		
		EventCluster cluster = clusters.stream().filter(c -> c.getIndex() == cluster_index).findFirst().get();
		
		if (from1D) {
			Set<StaticCorrection> fujiCorrections_low = StaticCorrectionFile.read(fujiStaticPath_low);
			Set<StaticCorrection> fujiCorrections_high = StaticCorrectionFile.read(fujiStaticPath_high);
			
			double[] azSepLow = new double[2];
			if (cluster_index == 4)
				azSepLow = cluster.getAzimuthBound(3);
			else if (cluster_index  == 5)
				azSepLow = cluster.getAzimuthBound(4);
				
			for (StaticCorrection corr : fujiCorrections_low) {
				double azimuth = Math.toDegrees(corr.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(corr.getStation().getPosition()));
				if (azimuth < 180) azimuth += 360;
				if (azimuth >= azSepLow[0] && azimuth <= azSepLow[1])
					fujiCorrections.add(corr);
			}
			for (StaticCorrection corr : fujiCorrections_high) {
				double azimuth = Math.toDegrees(corr.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(corr.getStation().getPosition()));
				if (azimuth < 180) azimuth += 360;
				if (azimuth < azSepLow[0] || azimuth > azSepLow[1])
					fujiCorrections.add(corr);
			}
		}
		else fujiCorrections = StaticCorrectionFile.read(fujiStaticPath);
		
		fujiCorrections = fujiCorrections.stream().filter(c -> c.getAmplitudeRatio() < 5 && c.getAmplitudeRatio() > 0.2)
				.collect(Collectors.toSet());
		
		Set<StaticCorrection> corrSet = new HashSet<>();
		
		if (fujiStaticPath2 != null) {
			Set<StaticCorrection> fujiCorrections2 = StaticCorrectionFile.read(fujiStaticPath2);
			fujiCorrections.stream().forEach(corr -> {
				StaticCorrection corr2 = fujiCorrections2.stream().parallel().filter(c -> c.getGlobalCMTID().equals(corr.getGlobalCMTID())
						&& c.getStation().equals(corr.getStation())
						&& c.getSynStartTime() == corr.getSynStartTime()
						&& c.getComponent().equals(corr.getComponent()))
					.findFirst().get();
				corrSet.add(new StaticCorrection(corr.getStation(), corr.getGlobalCMTID()
					, corr.getComponent(), corr.getSynStartTime(), corr.getTimeshift() - corr2.getTimeshift()
					, corr.getAmplitudeRatio() / corr2.getAmplitudeRatio(), corr.getPhases()));
			});
			fujiCorrections = corrSet;
		}
		
		fujiCorrections = fujiCorrections.stream().filter(c -> usedIdSet.contains(c.getGlobalCMTID())).collect(Collectors.toSet());
		
		
		double[][][] mapCorr = null;
		if (from1D)
			mapCorr = averageMap(fujiCorrections, dl);
		else
			mapCorr = averageMap(fujiCorrections, dl, cluster);
//		double[][] mapCorr = averageMapAtStation(fujiCorrections);
		
//		Path outpath = Paths.get("corrections_map.txt");
//		Path outpath = rootPath.resolve("corrections_map_ScS_longer_65.txt");
//		Path outpath = rootPath.resolve("corrections_map_S-v2.txt");
		PrintWriter pw3 = new PrintWriter(outpath.toFile());
		for (int i = 0; i < mapCorr.length; i++) {
			double lon = i * dl + dl/2.;
			lon = Math.round(lon * 1e3) / 1e3;
			for (int j = 0; j < mapCorr[0].length; j++) {
				double lat = (j * dl - 90) + dl/2.;
				lat = Math.round(lat * 1e3) / 1e3;
				pw3.println(lon + " " + lat + " " + mapCorr[i][j][0] + " " + mapCorr[i][j][1]);
			}
		}
		pw3.close();
		
		if (clusterPath != null) {
	//		EventCluster cluster = clusters.stream().filter(c -> c.getIndex() == cluster_index).findFirst().get();
			double[][] averageCorridor = averageInCorridor(fujiCorrections, cluster.getAzimuthSlices(), cluster.getCenterPosition());
			PrintWriter pw4 = new PrintWriter(outpath2.toFile());
			for (int i = 0; i < averageCorridor.length; i++) {
				pw4.println(i + " " + averageCorridor[i][0] + " " + averageCorridor[i][1]);
			}
			pw4.close();
		}
	}
	
	
	public static double[][][] averageMap(Set<StaticCorrection> ratios, double dl) {
		int nlat = (int) (180 / dl);
		int nlon = (int) (360 / dl);
		double[][][] map = new double[nlon][nlat][2];
		int[][] count = new int[nlon][nlat];
		for (StaticCorrection corr : ratios) {
//			double lon = corr.getStation().getPosition().getLongitude();
//			if (lon < 0)
//				lon += 360;
//			double lat = corr.getStation().getPosition().getLatitude() + 90;
			
			double evtlat = corr.getGlobalCMTID().getEvent().getCmtLocation().getLatitude();
			double evtlon = corr.getGlobalCMTID().getEvent().getCmtLocation().getLongitude();
			double distance = Math.toDegrees(corr.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(corr.getStation().getPosition()));
			double azimuth = Math.toDegrees(corr.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(corr.getStation().getPosition()));
			
			if (distance > 75) continue;
			if (distance < 65) continue;
			
			double lat = SphericalCoords.latFor(evtlat, evtlon, distance/2., azimuth);
			double lon = SphericalCoords.lonFor(evtlat, evtlon, distance/2., azimuth);
			if (lon < 0)
				lon += 360;
			lat += 90;
			
			int ilon = (int) (lon / dl);
			int ilat = (int) (lat / dl);
//			if (ilon == 360)
//				ilon = 359;
//			if (ilat == 180)
//				ilat = 179;
			if (corr.getTimeshift() < 100.) {
//				map[ilon][ilat] += corr.getTimeshift();
				map[ilon][ilat][0] += corr.getAmplitudeRatio();
				map[ilon][ilat][1] += corr.getTimeshift();
				count[ilon][ilat] += 1;
			}
		}
		
		for (int i = 0; i < nlon; i++) {
			for (int j = 0; j < nlat; j++) {
				if (count[i][j] > 0)
					for (int k = 0; k < 2; k++) map[i][j][k] /= count[i][j];
			}
		}
		
		return map;
	}
	
	public static double[][][] averageMap(Set<StaticCorrection> ratios, double dl, EventCluster cluster) {
		int nlat = (int) (180 / dl);
		int nlon = (int) (360 / dl);
		double[][][] map = new double[nlon][nlat][2];
		int[][] count = new int[nlon][nlat];
		for (StaticCorrection corr : ratios) {
//			double lon = corr.getStation().getPosition().getLongitude();
//			if (lon < 0)
//				lon += 360;
//			double lat = corr.getStation().getPosition().getLatitude() + 90;
			
			double evtlat = cluster.getCenterPosition().getLatitude();
			double evtlon = cluster.getCenterPosition().getLongitude();
			double distance = Math.toDegrees(cluster.getCenterPosition().getEpicentralDistance(corr.getStation().getPosition()));
			double azimuth = Math.toDegrees(cluster.getCenterPosition().getAzimuth(corr.getStation().getPosition()));
			
			if (distance > 75) continue;
			if (distance < 65) continue;
			
			double lat = SphericalCoords.latFor(evtlat, evtlon, distance/2., azimuth);
			double lon = SphericalCoords.lonFor(evtlat, evtlon, distance/2., azimuth);
			if (lon < 0)
				lon += 360;
			lat += 90;
			
			int ilon = (int) (lon / dl);
			int ilat = (int) (lat / dl);
//			if (ilon == 360)
//				ilon = 359;
//			if (ilat == 180)
//				ilat = 179;
			if (corr.getTimeshift() < 100.) {
				map[ilon][ilat][0] += corr.getAmplitudeRatio();
				map[ilon][ilat][1] += corr.getTimeshift();
				count[ilon][ilat] += 1;
			}
		}
		
		for (int i = 0; i < nlon; i++) {
			for (int j = 0; j < nlat; j++) {
				if (count[i][j] > 0)
					for (int k = 0; k < 2; k++) map[i][j][k] /= count[i][j];
			}
		}
		
		return map;
	}
	
	public static double[][] averageInCorridor(Set<StaticCorrection> ratios, List<Double> azimuthSeparations, HorizontalPosition center) {
		double[][] averages = new double[azimuthSeparations.size() + 1][2];
		int[] counts = new int[azimuthSeparations.size() + 1];
		for (StaticCorrection corr : ratios) {
			double distance = Math.toDegrees(corr.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(corr.getStation().getPosition()));
			if (distance < 65) continue;
			if (distance > 70) continue;
//			if (distance > 69) continue;
			double azimuth = Math.toDegrees(center.getAzimuth(corr.getStation().getPosition()));
			if (azimuth < 180) azimuth += 360;
			int i = azimuthSeparations.size();
			for (int j = 0; j < azimuthSeparations.size(); j++)
				if (azimuth < azimuthSeparations.get(j)) {
					i = j;
					break;
				}
			averages[i][0] += corr.getAmplitudeRatio();
			averages[i][1] += corr.getTimeshift();
			counts[i] += 1;
			
		}
		for (int i = 0; i < counts.length; i++)
			for (int k = 0; k < 2; k++) averages[i][k] /= counts[i];
		return averages;
	}
	
	public static double[][][] averageMapAtStation(Set<StaticCorrection> ratios) {
		double[][][] map = new double[360][180][2];
		int[][] count = new int[360][180];
		for (StaticCorrection corr : ratios) {
			if (corr.getAmplitudeRatio() > 4. || corr.getAmplitudeRatio() < 1./4. || Double.isNaN(corr.getAmplitudeRatio()))
				continue;
			
			double lon = corr.getStation().getPosition().getLongitude();
			if (lon < 0)
				lon += 360;
			double lat = corr.getStation().getPosition().getLatitude() + 90;
			
			int ilon = (int) lon;
			int ilat = (int) lat;
			if (ilon == 360)
				ilon = 359;
			if (ilat == 180)
				ilat = 179;
			if (corr.getTimeshift() < 100.) {
				map[ilon][ilat][0] += corr.getAmplitudeRatio();
				map[ilon][ilat][1] += corr.getTimeshift();
				count[ilon][ilat] += 1;
			}
		}
		
		for (int i = 0; i < 360; i++) {
			for (int j = 0; j < 180; j++) {
				if (count[i][j] > 0)
					for (int k = 0; k < 2; k++) map[i][j][k] /= count[i][j];
			}
		}
		
		return map;
	}
	
	public static HorizontalPosition[] distanceLine(HorizontalPosition pos, double distance) {
		double daz = 0.5;
		int n = (int) (360 / daz);
		HorizontalPosition[] positions = new HorizontalPosition[n];
		for (int i = 0; i < n; i++) {
			double azimuth = i * daz;
			double lat = SphericalCoords.latFor(pos.getLatitude(), pos.getLongitude(), distance/2., azimuth);
			double lon = SphericalCoords.lonFor(pos.getLatitude(), pos.getLongitude(), distance/2., azimuth);
			positions[i] = new HorizontalPosition(lat, lon);
		}
		return positions;
	}
}
