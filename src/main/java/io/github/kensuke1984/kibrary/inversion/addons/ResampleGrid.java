package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.inversion.Physical3DParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ResampleGrid {
	
	private List<UnknownParameter> targetUnknowns;
	
	private List<UnknownParameter> resampledUnknowns;
	
	int samplingRate;
	
	private double dlat;
	
	private double dlon;
	
	private double newDlat;
	
	private double newDlon;
	
	private List<List<UnknownParameter>> mapping;
	
	private int[][] iTargetToResampled;
	
	private int[] iResampledToTarget;

	public static void main(String[] args) {
//		List<UnknownParameter> parameters = new ArrayList<>();
//		parameters.add(new Physical3DParameter(PartialType.MU, new Location(-2, -2, 6371), 1.));
//		parameters.add(new Physical3DParameter(PartialType.MU, new Location(-2, 2, 6371), 1.));
//		parameters.add(new Physical3DParameter(PartialType.MU, new Location(2, -2, 6371), 1.));
//		parameters.add(new Physical3DParameter(PartialType.MU, new Location(2, 2, 6371), 1.));
//		parameters.add(new Physical3DParameter(PartialType.LAMBDA, new Location(-2, -2, 6371), 1.));
//		parameters.add(new Physical3DParameter(PartialType.LAMBDA, new Location(-2, 2, 6371), 1.));
//		parameters.add(new Physical3DParameter(PartialType.LAMBDA, new Location(2, -2, 6371), 1.));
//		parameters.add(new Physical3DParameter(PartialType.LAMBDA, new Location(2, 2, 6371), 1.));
//		
//		ResampleGrid sampler = new ResampleGrid(parameters, 4, 4, 2);
//		
////		List<UnknownParameter> resampledOnce = sampler.upsample(1);
////		List<List<UnknownParameter>> mapping = sampler.getMapping();
////		
////		for (int i = 0; i < parameters.size(); i++) {
////			System.out.println(parameters.get(i));
////			mapping.get(i).forEach(p -> System.out.print(p + ", "));
////			System.out.println();
////		}
//		
//		
//		List<UnknownParameter> resampledTwice = sampler.getResampledUnkowns();
//		List<List<UnknownParameter>> mapping = sampler.getMapping();
//		int[][] iTargetToResampled = sampler.getiTargetToResampled();
//		
//		for (int i = 0; i < parameters.size(); i++) {
//			System.out.println(parameters.get(i));
//			mapping.get(i).forEach(p -> System.out.print(p + ", "));
//			System.out.println();
//		}
//		
//		System.out.println();
//		for (int i = 0; i < parameters.size(); i++) {
//			System.out.println(parameters.get(i));
////			mapping.get(i).forEach(p -> System.out.print(p + ", "));
//			for (int j : iTargetToResampled[i])
//				System.out.print(resampledTwice.get(j) + ", ");
//			System.out.println();
//		}
		
		Path parameterPath = Paths.get(args[0]);
		try {
			List<UnknownParameter> parameterstmp = UnknownParameterFile.read(parameterPath);
			double r0 = parameterstmp.stream().map(u -> u.getLocation().getR()).findFirst().get();
			PartialType type = parameterstmp.stream().map(UnknownParameter::getPartialType).findFirst().get();
			List<UnknownParameter> parameters = parameterstmp.stream()
					.filter(u -> u.getLocation().getR() == r0
							&& u.getPartialType().equals(type))
					.collect(Collectors.toList());
//			List<UnknownParameter> parameters = parameterstmp;
			
			double dl = parameterstmp.stream().mapToDouble(p -> Math.abs(p.getLocation().getLatitude() - parameterstmp.get(0).getLocation().getLatitude())).distinct().sorted().toArray()[1];
			
//			double dlon = 5.;
//			double dlat = 5.;
			
			double dlat = dl;
			double dlon = dl;
			
			System.out.println("dLat = dLon = " + dl);
			
			ResampleGrid sampler2 = new ResampleGrid(parameters, dlat, dlon, 2);
			List<UnknownParameter> parameters2 = sampler2.getResampledUnkowns();
			List<HorizontalPosition> horizontalPositions2 = sampler2.getResampledPositions();
			int[] iResampledToTarget2 = sampler2.getiResampledToTarget();
			
			ResampleGrid sampler3 = new ResampleGrid(parameters, dlat, dlon, 3);
			List<UnknownParameter> parameters3 = sampler3.getResampledUnkowns();
			List<HorizontalPosition> horizontalPositions3 = sampler3.getResampledPositions();
			int[] iResampledToTarget3 = sampler3.getiResampledToTarget();
			
			ResampleGrid sampler32 = new ResampleGrid(parameters2, sampler2.getNewDlat(), sampler2.getNewDlon(), 2);
			List<UnknownParameter> parameters32 = sampler32.getResampledUnkowns();
			List<HorizontalPosition> horizontalPositions32 = sampler32.getResampledPositions();
			int[] iResampledToTarget32 = sampler32.getiResampledToTarget();
			
//			String tmpString = "_" + Utilities.getTemporaryString();
			String tmpString = "";
			Path parameters2Path = Paths.get("newHorizontalPoints_sampled2" + tmpString + ".inf");
			Path parameters3Path = Paths.get("newHorizontalPoints_sampled3" + tmpString + ".inf");
//			Path parameters32Path = Paths.get("newParamaters_sampled32" + tmpString + ".inf");
			Path mapping2Path = Paths.get("horizontalMapping_sampled2" + tmpString + ".inf");
			Path mapping3Path = Paths.get("horizontalMapping_sampled3" + tmpString + ".inf");
			Path mapping32Path = Paths.get("horizontalMapping_sampled32" + tmpString + ".inf");
			
			PrintWriter pw_hp2 = new PrintWriter(parameters2Path.toFile());
			PrintWriter pw_hp3 = new PrintWriter(parameters3Path.toFile());
			
			PrintWriter pw2 = new PrintWriter(mapping2Path.toFile());
			PrintWriter pw3 = new PrintWriter(mapping3Path.toFile());
			PrintWriter pw32 = new PrintWriter(mapping32Path.toFile());
			
			for (int i = 0; i < horizontalPositions2.size(); i++) {
				pw_hp2.println(horizontalPositions2.get(i));
				pw2.println(horizontalPositions2.get(i).getLatitude() + " " + horizontalPositions2.get(i).getLongitude() + " " + "0." + " " + iResampledToTarget2[i]);
			}
			for (int i = 0; i < horizontalPositions3.size(); i++) {
				pw_hp3.println(horizontalPositions3.get(i));
				pw3.println(horizontalPositions3.get(i).getLatitude() + " " + horizontalPositions3.get(i).getLongitude() + " " + "0." + " " + iResampledToTarget3[i]);
			}
			for (int i = 0; i < horizontalPositions32.size(); i++)
				pw32.println(horizontalPositions32.get(i).getLatitude() + " " + horizontalPositions32.get(i).getLongitude() + " " + "0." + " " + iResampledToTarget32[i]);
			
			pw_hp2.close();
			pw_hp3.close();
			pw2.close();
			pw3.close();
			pw32.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public ResampleGrid(List<UnknownParameter> unknowns, double dlat, double dlon, int samplingRate) {
		this.targetUnknowns = unknowns;
		this.dlat = dlat;
		this.dlon = dlon;
		this.samplingRate = samplingRate;
		
		resampledUnknowns = upsample(samplingRate);
	}
	
	private List<UnknownParameter> upsample(int nfolds) {
		mapping = new ArrayList<>();
		
		List<UnknownParameter> resampled = new ArrayList<>();
		
		double dlonPrime = dlon;
		double dlatPrime = dlat;
		
		resampled = targetUnknowns;
		for (int i = 1; i < nfolds; i++) {
			resampled = upsampleOnce(resampled, dlatPrime, dlonPrime);
			dlonPrime /= 2.;
			dlatPrime /= 2.;
		}
		
		newDlat = dlatPrime;
		newDlon = dlonPrime;
		
		int n = 1;
		for (int i = 1; i < nfolds; i++)
			n *= 4;
		
		iTargetToResampled = new int[targetUnknowns.size()][n];
		iResampledToTarget = new int[resampled.size()];
		
		for (int i = 0; i < targetUnknowns.size(); i++) {
			List<UnknownParameter> tmpmap = new ArrayList<>();
			for (int j = 0; j < n; j++) {
				int iResampled = i * n + j;
				
				tmpmap.add(resampled.get(iResampled));
				
				iTargetToResampled[i][j] = iResampled;
				iResampledToTarget[iResampled] = i;
			}
			mapping.add(tmpmap);
		}
		
		return resampled;
	}
	
	private List<UnknownParameter> upsampleOnce(List<UnknownParameter> parameters, double dlon, double dlat) {
		List<UnknownParameter> resampled = new ArrayList<>();
		
		for (UnknownParameter p : parameters) {
			Location loc = p.getLocation();
			double lat = loc.getLatitude();
			double lon = loc.getLongitude();
			double r = loc.getR();
			Location loc1 = new Location(lat - dlat/4., lon - dlon/4., r);
			Location loc2 = new Location(lat - dlat/4., lon + dlon /4., r);
			Location loc3 = new Location(lat + dlat/4., lon - dlon /4., r);
			Location loc4 = new Location(lat + dlat/4., lon + dlon /4., r);
			
			resampled.add(new Physical3DParameter(p.getPartialType(), loc1, p.getWeighting() / 4.));
			resampled.add(new Physical3DParameter(p.getPartialType(), loc2, p.getWeighting() / 4.));
			resampled.add(new Physical3DParameter(p.getPartialType(), loc3, p.getWeighting() / 4.));
			resampled.add(new Physical3DParameter(p.getPartialType(), loc4, p.getWeighting() / 4.));
		}
		
		return resampled;
	}
	
	public List<List<UnknownParameter>> getMapping() {
		return mapping;
	}
	
	public int[][] getiTargetToResampled() {
		return iTargetToResampled;
	}
	
	public int[] getiResampledToTarget() {
		return iResampledToTarget;
	}
	
	public List<UnknownParameter> getTargetUnknowns() {
		return targetUnknowns;
	}
	
	public List<UnknownParameter> getResampledUnkowns() {
		return resampledUnknowns;
	}
	
	public List<HorizontalPosition> getResampledPositions() {
		return resampledUnknowns.stream().map(u -> u.getLocation().toHorizontalPosition()).distinct().collect(Collectors.toList());
	}
	
	public double getNewDlat() {
		return newDlat;
	}
	
	public double getNewDlon() {
		return newDlon;
	}
	
}
