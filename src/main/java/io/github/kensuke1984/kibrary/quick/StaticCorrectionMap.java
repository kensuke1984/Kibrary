package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.util.EventCluster;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.sc.seis.TauP.SphericalCoords;

public class StaticCorrectionMap {

	public static void main(String[] args) throws IOException {
//		Path fujiStaticPath = Paths.get(args[0]);
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_FAR_SLAB/filtered_12.5-200s/fujiStaticCorrection_ScS_60deg.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/fujiStaticCorrection_ScS_60deg.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_DPP/filtered_12.5-200s/fujiStaticCorrection_ScS.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_HLH/filtered_12.5-200s/fujiStaticCorrection_ScS_70deg.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_MANTLE_NODPP/filtered_12.5-200s/fujiStaticCorrection_ScS.dat");
//		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_UPPER_MANTLE/filtered_stf_12.5-200s/fujiStaticCorrection_S.dat");
		Path fujiStaticPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_SOURCE_SIDE/filtered_12.5-200s/fujiStaticCorrection_ScS.dat");
		
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_FAR_SLAB/filtered_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_DPP/filtered_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_HLH/filtered_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_MANTLE_NODPP/filtered_12.5-200s");
//		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_UPPER_MANTLE/filtered_stf_12.5-200s");
		Path rootPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_SOURCE_SIDE/filtered_12.5-200s");
		
		Path fujiStaticPath2 = null;
		if (args.length == 2)
			fujiStaticPath2 = Paths.get(args[1]);
		
		Path clusterPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster-6deg.inf");
		List<EventCluster> cluster = EventCluster.readClusterFile(clusterPath);
		Set<Integer> clusterIndexSet = new HashSet<Integer>();
//		clusterIndexSet.add(3);
		clusterIndexSet.add(4);
//		clusterIndexSet.add(5);
		Set<GlobalCMTID> usedIdSet = cluster.stream().filter(c -> clusterIndexSet.contains(c.getIndex())).map(c -> c.getID())
				.collect(Collectors.toSet());
		
		System.out.println(usedIdSet.size());
		
		Set<StaticCorrection> fujiCorrections = StaticCorrectionFile.read(fujiStaticPath);
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
		
		
//		double[][] mapCorr = averageMap(fujiCorrections);
		double[][] mapCorr = averageMapAtStation(fujiCorrections);
		
//		Path outpath3 = Paths.get("corrections_map.txt");
		Path outpath3 = rootPath.resolve("corrections_map.txt");
		PrintWriter pw3 = new PrintWriter(outpath3.toFile());
		for (int i = 0; i < 360; i++) {
			double lon = i;
			for (int j = 0; j < 180; j++) {
				pw3.println(lon + " " + (j - 90) + " " + mapCorr[i][j]);
			}
		}
		pw3.close();
		
	}
	
	public static double[][] averageMap(Set<StaticCorrection> ratios) {
		double[][] map = new double[360][180];
		int[][] count = new int[360][180];
		for (StaticCorrection corr : ratios) {
//			double lon = corr.getStation().getPosition().getLongitude();
//			if (lon < 0)
//				lon += 360;
//			double lat = corr.getStation().getPosition().getLatitude() + 90;
			
			double evtlat = corr.getGlobalCMTID().getEvent().getCmtLocation().getLatitude();
			double evtlon = corr.getGlobalCMTID().getEvent().getCmtLocation().getLongitude();
			double distance = Math.toDegrees(corr.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(corr.getStation().getPosition()));
			double azimuth = Math.toDegrees(corr.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(corr.getStation().getPosition()));
			
			double lat = SphericalCoords.latFor(evtlat, evtlon, distance/2., azimuth);
			double lon = SphericalCoords.lonFor(evtlat, evtlon, distance/2., azimuth);
			if (lon < 0)
				lon += 360;
			lat += 90;
			
			int ilon = (int) lon;
			int ilat = (int) lat;
			if (ilon == 360)
				ilon = 359;
			if (ilat == 180)
				ilat = 179;
			if (corr.getTimeshift() < 100.) {
				map[ilon][ilat] += corr.getTimeshift();
				count[ilon][ilat] += 1;
			}
		}
		
		for (int i = 0; i < 360; i++) {
			for (int j = 0; j < 180; j++) {
				if (count[i][j] > 0)
					map[i][j] /= count[i][j];
			}
		}
		
		return map;
	}
	
	public static double[][] averageMapAtStation(Set<StaticCorrection> ratios) {
		double[][] map = new double[360][180];
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
//				map[ilon][ilat] += corr.getTimeshift();
				map[ilon][ilat] += corr.getAmplitudeRatio();
				count[ilon][ilat] += 1;
			}
		}
		
		for (int i = 0; i < 360; i++) {
			for (int j = 0; j < 180; j++) {
				if (count[i][j] > 0)
					map[i][j] /= count[i][j];
			}
		}
		
		return map;
	}
}
