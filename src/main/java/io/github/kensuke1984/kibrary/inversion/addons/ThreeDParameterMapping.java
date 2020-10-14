package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.inversion.Physical3DParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ThreeDParameterMapping {
	
	private UnknownParameter[] originalUnknowns;
	private UnknownParameter[] unknowns;
	private int[] iOriginalToNew;
	private int[][] iNewToOriginal;
	private Path inputHorizontalMapping;
	private Path inputVerticalMapping;
	private double[] newLayerWidths;
	private double[] newRadii;
	
	public static void main(String[] args) {
		Path unknownPath = Paths.get(args[0]);
		Path inputVerticalMapping = Paths.get(args[1]);
//		Path inputHorizontalMapping = Paths.get(args[2]);
		
		double dlon = 2;
		double dlat = 2;
		int samplingRate = 2;
		
		try {
			List<UnknownParameter> originalUnknowns = UnknownParameterFile.read(unknownPath);
		
//			ThreeDParameterMapping mapping = new ThreeDParameterMapping(inputHorizontalMapping, inputVerticalMapping, originalUnknowns);
			
			ResampleGrid sampler = new ResampleGrid(originalUnknowns, dlat, dlon, samplingRate);
			ThreeDParameterMapping mapping = new ThreeDParameterMapping(sampler, inputVerticalMapping);
			
			UnknownParameter[] newUnknowns = mapping.getNewUnknowns();
			UnknownParameter[] newOriginalUnknowns = mapping.getOriginalUnknowns();
			
			for (int iNew = 0; iNew < newUnknowns.length; iNew++) {
//				System.out.println(">+");
				System.out.println("> " + newUnknowns[iNew]);
//				System.out.println(">-");
				for (int iOriginal : mapping.getiNewToOriginal(iNew))
					System.out.println(newOriginalUnknowns[iOriginal]);
			}
			
//			for (int iNew = 0; iNew < newUnknowns.length; iNew++) {
//				System.out.println(newUnknowns[iNew]);
//			}
//			double[] newRadii = mapping.getNewRadii();
//			double[] newLayerWidhts = mapping.getNewLayerWidths();
//			for (int i = 0; i < newRadii.length; i++)
//				System.out.println(newRadii[i] + " " + newLayerWidhts[i]);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ThreeDParameterMapping(Path inputHorizontalMapping, Path inputVerticalMapping, UnknownParameter[] originalUnknowns) {
		this.inputHorizontalMapping = inputHorizontalMapping;
		this.inputVerticalMapping = inputVerticalMapping;
		this.originalUnknowns = originalUnknowns;
		
		map();
		make();
	}
	
	public ThreeDParameterMapping(ResampleGrid resampler, Path inputVerticalMapping) {
		this.inputHorizontalMapping = null;
		this.inputVerticalMapping = inputVerticalMapping;
		this.originalUnknowns = resampler.getResampledUnkowns().toArray(new UnknownParameter[0]);
		
		map(resampler);
		make();
	}
	
	public void map() {
		HorizontalParameterMapping horizontalMapping = new HorizontalParameterMapping(originalUnknowns, inputHorizontalMapping);
		ParameterMapping verticalMapping = new ParameterMapping(originalUnknowns, inputVerticalMapping);
		
		this.newLayerWidths = verticalMapping.getNewLayerWidths();
		this.newRadii = verticalMapping.getNewRadii();
		
		int[][] iNewToOriginalHorizontal = horizontalMapping.getiNewToOriginal();
		int[] iOriginalToNewVertical = verticalMapping.getiOriginalToNew();
		
		List<Set<Integer>> tmpIOriginalList = new ArrayList<>();
		List<Set<Integer>> tmpIHorizontalRedundantList = new ArrayList<>();
		for (int i = 0; i < iNewToOriginalHorizontal.length; i++) {
			Set<Integer> iOriginal3D = new HashSet<>();
			for (int j = 0; j < iNewToOriginalHorizontal[i].length; j++) {
				int iOriginal = iNewToOriginalHorizontal[i][j];
				int iNewVertical = iOriginalToNewVertical[iOriginal];
				int[] iOriginals = verticalMapping.getiNewToOriginal(iNewVertical);
				for (int k : iOriginals)
					iOriginal3D.add(k);
			}
			tmpIOriginalList.add(iOriginal3D);
			
			Set<Integer> iHorizontalRedundant = new HashSet<>();
			for (int j : iOriginal3D)
				iHorizontalRedundant.add(horizontalMapping.getiOriginalToNew(j));
			
			tmpIHorizontalRedundantList.add(iHorizontalRedundant);
		}
		
		List<Set<Integer>> iNewToOrignalList = new ArrayList<>();
		List<Set<Integer>> filledList = new ArrayList<>();
		iNewToOrignalList.add(tmpIOriginalList.get(0));
		filledList.add(tmpIHorizontalRedundantList.get(0));
		for (int i = 1; i < iNewToOriginalHorizontal.length; i++) {
			boolean filled = false;
			Set<Integer> redundants = tmpIHorizontalRedundantList.get(i);
			for  (Set<Integer> tmpSet : filledList) {
				if (tmpSet.equals(redundants)) {
					filled = true;
					break;
				}
			}
			if (!filled) {
				iNewToOrignalList.add(tmpIOriginalList.get(i));
				filledList.add(tmpIHorizontalRedundantList.get(i));
			}
		}
		
		iNewToOriginal = new int[iNewToOrignalList.size()][];
		for (int i = 0; i < iNewToOrignalList.size(); i++) {
			Set<Integer> iSet = iNewToOrignalList.get(i);
			iNewToOriginal[i] = new int[iSet.size()];
			int counter = 0;
			for (int j : iSet) {
				iNewToOriginal[i][counter] = j;
				counter++;
			}
		}
		
		unknowns = new UnknownParameter[iNewToOriginal.length];
	}
	
	public void map(ResampleGrid resampler) {
		HorizontalParameterMapping horizontalMapping = new HorizontalParameterMapping(resampler.getTargetUnknowns(), resampler.getResampledUnkowns()
				, resampler.getiTargetToResampled(), resampler.getiResampledToTarget());
		ParameterMapping verticalMapping = new ParameterMapping(originalUnknowns, inputVerticalMapping);
		
		this.newLayerWidths = verticalMapping.getNewLayerWidths();
		this.newRadii = verticalMapping.getNewRadii();
		
		int[][] iNewToOriginalHorizontal = horizontalMapping.getiNewToOriginal();
		int[] iOriginalToNewVertical = verticalMapping.getiOriginalToNew();
		
		List<Set<Integer>> tmpIOriginalList = new ArrayList<>();
		List<Set<Integer>> tmpIHorizontalRedundantList = new ArrayList<>();
		for (int i = 0; i < iNewToOriginalHorizontal.length; i++) {
			Set<Integer> iOriginal3D = new HashSet<>();
			for (int j = 0; j < iNewToOriginalHorizontal[i].length; j++) {
				int iOriginal = iNewToOriginalHorizontal[i][j];
				int iNewVertical = iOriginalToNewVertical[iOriginal];
				int[] iOriginals = verticalMapping.getiNewToOriginal(iNewVertical);
				for (int k : iOriginals)
					iOriginal3D.add(k);
			}
			tmpIOriginalList.add(iOriginal3D);
			
			Set<Integer> iHorizontalRedundant = new HashSet<>();
			for (int j : iOriginal3D)
				iHorizontalRedundant.add(horizontalMapping.getiOriginalToNew(j));
			
			tmpIHorizontalRedundantList.add(iHorizontalRedundant);
		}
		
		List<Set<Integer>> iNewToOrignalList = new ArrayList<>();
		List<Set<Integer>> filledList = new ArrayList<>();
		iNewToOrignalList.add(tmpIOriginalList.get(0));
		filledList.add(tmpIHorizontalRedundantList.get(0));
		for (int i = 1; i < iNewToOriginalHorizontal.length; i++) {
			boolean filled = false;
			Set<Integer> redundants = tmpIHorizontalRedundantList.get(i);
			for  (Set<Integer> tmpSet : filledList) {
				if (tmpSet.equals(redundants)) {
					filled = true;
					break;
				}
			}
			if (!filled) {
				iNewToOrignalList.add(tmpIOriginalList.get(i));
				filledList.add(tmpIHorizontalRedundantList.get(i));
			}
		}
		
		iNewToOriginal = new int[iNewToOrignalList.size()][];
		for (int i = 0; i < iNewToOrignalList.size(); i++) {
			Set<Integer> iSet = iNewToOrignalList.get(i);
			iNewToOriginal[i] = new int[iSet.size()];
			int counter = 0;
			for (int j : iSet) {
				iNewToOriginal[i][counter] = j;
				counter++;
			}
		}
		
		unknowns = new UnknownParameter[iNewToOriginal.length];
	}
	
	public void make() {
		for (int i = 0; i < unknowns.length; i++) {
			double latmean = 0;
			double lonmean = 0;
			List<Double> lats = new ArrayList<>();
			List<Double> lons = new ArrayList<>();
			double rmean = 0;
			double weight = 0;
			for (int j = 0; j < iNewToOriginal[i].length; j++) {
				UnknownParameter unknown = originalUnknowns[iNewToOriginal[i][j]];
//				latmean += unknown.getLocation().getLatitude();
//				lonmean += unknown.getLocation().getLongitude();
				lats.add(unknown.getLocation().getLatitude());
				lons.add(unknown.getLocation().getLongitude());
				rmean += unknown.getLocation().getR();
				weight += unknown.getWeighting();
			}
//			latmean /= iNewToOriginal[i].length;
//			lonmean /= iNewToOriginal[i].length;
			latmean = getCenter(lats);
			lonmean = getCenter(lons);
			rmean /= iNewToOriginal[i].length;
			
			UnknownParameter refUnknown = originalUnknowns[iNewToOriginal[i][0]];
			PartialType type = refUnknown.getPartialType();
			Location location = new Location(latmean, lonmean, rmean);
			UnknownParameter newUnknown = new Physical3DParameter(type, location, weight);
			unknowns[i] = newUnknown;
		}
	}
	
	private double getCenter(List<Double> xs) {
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		for (double x : xs) {
			if (x < min)
				min =  x;
			if (x > max)
				max = x;
		}
		
		return (max + min) / 2.;
	}
	
	public int[][] getiNewToOriginal() {
		return iNewToOriginal;
	}
	
	public int[] getiNewToOriginal(int i) {
		return iNewToOriginal[i];
	}
	
	public UnknownParameter[] getNewUnknowns() {
		return unknowns;
	}
	
	public UnknownParameter[] getOriginalUnknowns() {
		return originalUnknowns;
	}
	
	public double[] getNewRadii() {
		return newRadii;
	}
	
	public double[] getNewLayerWidths() {
		return newLayerWidths;
	}

}
