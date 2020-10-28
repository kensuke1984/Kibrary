package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.inversion.Physical3DParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sleepycat.bind.tuple.IntegerBinding;

import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Anselme Borgeaud
 *
 */
public class HorizontalParameterMapping {
	private UnknownParameter[] originalUnknowns;
	private UnknownParameter[] unknowns;
	private int[] iOriginalToNew;
	private HorizontalPosition[] newPositions;
	private double[] newPixelWidths;
	private int[][] iNewToOriginal;
	private Path input;
	private Map<PartialType, Integer> typeIndexMap;
	
	public static void main(String[] args) throws IOException {
		Path unknownParameterPath = Paths.get("newUnknowns.inf");
		Path parameterMappingPath = Paths.get("horizontalPointMapping.inf");
		
		UnknownParameter[] originalUnknowns = UnknownParameterFile.read(unknownParameterPath).toArray(new UnknownParameter[0]);
//		System.out.println("originalUnknowns.length = " + originalUnknowns.length);
		HorizontalParameterMapping mapping = new HorizontalParameterMapping(originalUnknowns, parameterMappingPath);
		
		UnknownParameter[] newUnknowns = mapping.getUnknowns();
		
//		List<UnknownParameter> parameterList = Stream.of(newUnknowns).collect(Collectors.toList());
//		Path outPath = Paths.get("unknowns" + Utilities.getTemporaryString() + ".dat");
//		UnknownParameterFile.write(parameterList, outPath);
		
		for (int iNew = 0; iNew < newUnknowns.length; iNew++) {
			System.out.println(newUnknowns[iNew]);
			for (int iOriginal : mapping.getiNewToOriginal(iNew))
				System.out.println("--> " + originalUnknowns[iOriginal]);
		}
		
		for (UnknownParameter unknown: mapping.getUnknowns()) {
			System.out.println(unknown);
		}
	}
	
	public HorizontalParameterMapping(UnknownParameter[] originalUnknowns, Path input) {
		this.originalUnknowns = originalUnknowns;
		this.input = input;
		typeIndexMap = new HashMap<>();
		List<PartialType> typeList = Stream.of(originalUnknowns).map(u -> u.getPartialType()).distinct().collect(Collectors.toList());
		for (int i = 0; i < typeList.size(); i++)
			typeIndexMap.put(typeList.get(i), i);
		try {
			read(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
		make();
	}
	
	public HorizontalParameterMapping(List<UnknownParameter> originalUnknowns, List<UnknownParameter> newUnknowns,
			int[][] iNewToOriginal, int[] iOriginalToNew) {
		this.originalUnknowns = originalUnknowns.toArray(new UnknownParameter[0]);
		this.unknowns = newUnknowns.toArray(new UnknownParameter[0]);
		this.iNewToOriginal = iNewToOriginal;
		this.iOriginalToNew = iOriginalToNew;
		this.input = null;
	}
	
	public HorizontalParameterMapping(UnknownParameter[] originalUnknowns) {
		this.originalUnknowns = originalUnknowns;
		this.unknowns = originalUnknowns;
		this.iOriginalToNew = new int[unknowns.length];
		this.iNewToOriginal = new int[unknowns.length][];
		
		for (int i = 0; i < unknowns.length; i++) {
			iOriginalToNew[i] = i;
			iNewToOriginal[i] = new int[] {i};
		}
		
		this.input = null;
	}
	
	public void read(Path input) throws IOException {
		iOriginalToNew = new int[originalUnknowns.length];
		
//		System.out.println("iOriginalToNew.length = " + iOriginalToNew.length);
		
		List<UnknownParameter> originalUnknownList = new ArrayList<>();
		for (UnknownParameter p : originalUnknowns)
			originalUnknownList.add(p);
		
		List<Integer> positionOriginalToNewIndex = new ArrayList<>();
		List<HorizontalPosition> positions = new ArrayList<>();
		List<Double> widths = new ArrayList<>();
		
		BufferedReader br = Files.newBufferedReader(input);
		String line;
		int iNewUnknown;
		int nNewPoints = 0;
		while((line=br.readLine()) != null) {
			String[] s = line.trim().split("\\s+");
			double lat = Double.parseDouble(s[0]);
			double lon = Double.parseDouble(s[1]);
			double width = Double.parseDouble(s[2]);
			int itmp = Integer.parseInt(s[3]);
//			if (iNewUnknown != itmp) {
//				if (itmp != iNewUnknown + 1)
//					throw new RuntimeException("Unexpected");
//				iNewUnknown++;
//			}
			
			iNewUnknown = itmp;
			if (itmp != -1)
				nNewPoints = itmp;
			positionOriginalToNewIndex.add(iNewUnknown);
			positions.add(new HorizontalPosition(lat, lon));
			widths.add(width);
		}
		nNewPoints = nNewPoints + 1; // indexing starts from 0
		
//		System.out.println("nNewPoints = " + nNewPoints);
		
		List<List<Integer>> positionNewToOriginalIndex = new ArrayList<>();
		for (int i = 0; i < nNewPoints; i++) {
			List<Integer> tmplist = new ArrayList<>();
			for (int j = 0; j < positionOriginalToNewIndex.size(); j++) {
				if (positionOriginalToNewIndex.get(j) == i)
					tmplist.add(j);
			}
			positionNewToOriginalIndex.add(tmplist);
		}
		
		// set new perturbation layers
		newPositions = new HorizontalPosition[positionNewToOriginalIndex.size()];
		newPixelWidths = new double[positionNewToOriginalIndex.size()];
		for (int i = 0; i < positionNewToOriginalIndex.size(); i++) {
			double lat = 0;
			double lon = 0;
			double width = 0;
			List<Double> lats = new ArrayList<>();
			List<Double> lons = new ArrayList<>();
			for (int index : positionNewToOriginalIndex.get(i)) {
//				lat += positions.get(index).getLatitude();
//				lon += positions.get(index).getLongitude();
				lats.add(positions.get(index).getLatitude());
				lons.add(positions.get(index).getLongitude());
				width += widths.get(index);
			}
//			lat /= positionNewToOriginalIndex.get(i).size();
//			lon /= positionNewToOriginalIndex.get(i).size();
			lat = getCenter(lats);
			lon = getCenter(lons);
			newPositions[i] = new HorizontalPosition(lat, lon);
			newPixelWidths[i] = width;
		}
		
		//---- For debug
//		for (int i = 0; i < radiiOriginalToNewIndex.size(); i++)
//			System.out.println(i + " " + radiiOriginalToNewIndex.get(i));
//		for (int i = 0; i < radiiNewToOriginalIndex.size(); i++) {
//			System.out.println(i);
//			for (int j = 0; j < radiiNewToOriginalIndex.get(i).size(); j++)
//				System.out.println("---> " + radiiNewToOriginalIndex.get(i).get(j));
//		}
		//----
		
		List<Double> radii = originalUnknownList.stream().map(p -> p.getLocation().getR())
			.distinct().collect(Collectors.toList());
		
//		System.out.println(radii.size());
		
		int nNewUnknownSingleType = radii.size() * nNewPoints;
		int nNewUnknownAllTypes = nNewUnknownSingleType * typeIndexMap.size();
		
//		System.out.println(nNewUnknownSingleType + " " + nNewUnknownAllTypes);
		
		for (int i = 0; i < radii.size(); i++) {
			double r = radii.get(i);
			for (int k = 0; k < originalUnknowns.length; k++) {
				if (Utilities.equalWithinEpsilon(originalUnknowns[k].getLocation().getR(), r, eps)) {
					for (int l = 0; l < positionOriginalToNewIndex.size(); l++) {
						HorizontalPosition position = positions.get(l);
						if (position.equals(originalUnknowns[k].getLocation().toHorizontalPosition())) {
							iOriginalToNew[k] = positionOriginalToNewIndex.get(l) * radii.size() + i + typeIndexMap.get(originalUnknowns[k].getPartialType()) * nNewUnknownSingleType;
						}
					}
				}
			}
		}
		
//		int countNew = -1;
//		for (int i = 0; i < iNewUnknown; i++) {
//			List<Integer> tmplist = radiiNewToOriginalIndex.get(i);
//			for (int j = 0; j < tmplist.size(); j++) {
//				int jj = tmplist.get(j);
//				double r = radii.get(jj);
//				for (int k = 0; k < originalUnknowns.length; k++) {
//					if (Utilities.equalWithinEpsilon(originalUnknowns[k].getLocation().getR(), r, eps)) {
//						if (j == 0)
//							countNew++;
//						iOriginalToNew[k] = countNew;
//					}
//				}
//			}
//		}
//		countNew++;
		
//		System.out.println("nNewUnknown = " + nNewUnknown);
		
		Map<Integer, List<Integer>> iNewToOriginalMap = new HashMap<>();
		for (int i = 0; i < iOriginalToNew.length; i++) {
			Integer key = iOriginalToNew[i];
			
			List<Integer> tmplist;
			if (iNewToOriginalMap.containsKey(key)) {
				tmplist = iNewToOriginalMap.get(key);
				tmplist.add(i);
				iNewToOriginalMap.replace(key, tmplist);
			}
			else {
				tmplist = new ArrayList<>();
				tmplist.add(i);
				iNewToOriginalMap.put(key, tmplist);
			}
		}
		
//		System.out.println(iNewToOriginalMap.size() + " " + nNewUnknown + " " + iOriginalToNew.length);
		
		iNewToOriginal = new int[nNewUnknownAllTypes][];
		for (int i = 0; i < nNewUnknownAllTypes; i++) {
			List<Integer> tmplist = iNewToOriginalMap.get(i);
			iNewToOriginal[i] = new int[tmplist.size()];
			for (int j = 0; j < tmplist.size(); j++)
				iNewToOriginal[i][j] = tmplist.get(j);
		}
		
		//--- For debug
//		for (int i = 0; i < originalUnknowns.length; i++) {
//			System.out.println(i + " " + iOriginalToNew[i]);
//		}
//		for (int i = 0; i < nNewUnknownAllTypes; i++) {
//			System.out.println(i);
//			for (int j = 0; j < iNewToOriginal[i].length; j++)
//				System.out.println("---> " + iNewToOriginal[i][j]);
//		}
		
		unknowns = new UnknownParameter[nNewUnknownAllTypes];
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
	
	public void make() {
		for (int i = 0; i < unknowns.length; i++) {
			double latmean = 0;
			double lonmean = 0;
			List<Double> lats = new ArrayList<>();
			List<Double> lons = new ArrayList<>();
			double weight = 0;
			for (int j = 0; j < iNewToOriginal[i].length; j++) {
				UnknownParameter unknown = originalUnknowns[iNewToOriginal[i][j]];
//				latmean += unknown.getLocation().getLatitude();
//				lonmean += unknown.getLocation().getLongitude();
				lats.add(unknown.getLocation().getLatitude());
				lons.add(unknown.getLocation().getLongitude());
				weight += unknown.getWeighting();
			}
//			latmean /= iNewToOriginal[i].length;
//			lonmean /= iNewToOriginal[i].length;
			latmean = getCenter(lats);
			lonmean = getCenter(lons);
			
			UnknownParameter refUnknown = originalUnknowns[iNewToOriginal[i][0]];
			PartialType type = refUnknown.getPartialType();
			Location location = new Location(latmean, lonmean, refUnknown.getLocation().getR());
			UnknownParameter newUnknown = new Physical3DParameter(type, location, weight);
			unknowns[i] = newUnknown;
		}
	}
	
	public UnknownParameter[] getOriginalUnknowns() {
		return originalUnknowns;
	}
	
	public UnknownParameter[] getUnknowns() {
		return unknowns;
	}
	
	public int[] getiOriginalToNew() {
		return iOriginalToNew;
	}
	
	public int getiOriginalToNew(int iOriginal) {
		return iOriginalToNew[iOriginal];
	}
	
	public int[][] getiNewToOriginal() {
		return iNewToOriginal;
	}
	
	public int[] getiNewToOriginal(int iNew) {
		return iNewToOriginal[iNew];
	}
	
	public Path getInput() {
		return input;
	}
	
	public HorizontalPosition[] getNewPositions() {
		return newPositions;
	}
	
	public double[] getNewPixelWidths() {
		return newPixelWidths;
	}
	
	public int getNoriginal() {
		return originalUnknowns.length;
	}
	
	public int getNnew() {
		return unknowns.length;
	}
	
	final double eps = 1e-6;
}
