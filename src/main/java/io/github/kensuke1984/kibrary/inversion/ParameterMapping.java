package io.github.kensuke1984.kibrary.inversion;

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

public class ParameterMapping {
	private UnknownParameter[] originalUnknowns;
	private UnknownParameter[] unknowns;
	private int[] iOriginalToNew;
	private double[] radii;
	private int[][] iNewToOriginal;
	private Path input;
	
	public static void main(String[] args) throws IOException {
		Path unknownParameterPath = Paths.get("unknowns.inf");
		Path parameterMappingPath = Paths.get("unknownMapping.inf");
		
		UnknownParameter[] unknowns = UnknownParameterFile.read(unknownParameterPath).toArray(new UnknownParameter[0]);
		ParameterMapping mapping = new ParameterMapping(unknowns, parameterMappingPath);
		
		for (UnknownParameter unknown: mapping.getUnknowns()) {
			System.out.println(unknown);
		}
	}
	
	public ParameterMapping(UnknownParameter[] originalUnknowns, Path input) {
		this.originalUnknowns = originalUnknowns;
		this.input = input;
		try {
			read(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
		make();
	}
	
	public ParameterMapping(UnknownParameter[] originalUnknowns) {
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
//		List<List<Integer>> tmpiNewToOriginal = new ArrayList<>();
//		for(int i = 0; i < originalUnknowns.length; i++) {
//			tmpiNewToOriginal.add(new ArrayList<>());
//		}
		
		List<UnknownParameter> originalUnknownList = new ArrayList<>();
		for (UnknownParameter p : originalUnknowns)
			originalUnknownList.add(p);
		
		List<Integer> tmpiOriginalToNewList = new ArrayList<>();
		List<Double> radii = new ArrayList<>();
		
		BufferedReader br = Files.newBufferedReader(input);
		String line;
		int iline = 0;
		int iNewUnknown = -1;
		while((line=br.readLine()) != null) {
			String[] s = line.trim().split("\\s+");
			double r = Double.parseDouble(s[0]);
			int itmp = Integer.parseInt(s[1]);
			if (iNewUnknown != itmp) {
				if (itmp != iNewUnknown + 1)
					throw new RuntimeException("Unexpected");
				iNewUnknown++;
				
//				List<Integer> tmplist = new ArrayList<>();
//				tmplist.add(iline);
//				tmpiNewToOriginal.add(tmplist);
			}
//			else {
//				List<Integer> tmplist = tmpiNewToOriginal.get(iNewUnknown);
//				tmplist.add(iline);
//				tmpiNewToOriginal.set(iline, tmplist);
//			}
			
			tmpiOriginalToNewList.add(iNewUnknown);
			radii.add(r);
//			iOriginalToNew[iline] = iNewUnknown;
			iline++;
		}
		int[] tmpiOriginalToNew = new int[tmpiOriginalToNewList.size()];
		for (int i = 0; i < tmpiOriginalToNew.length; i++)
			tmpiOriginalToNew[i] = tmpiOriginalToNewList.get(i);
		
		for (int i = 0; i < iNewToOriginal.length; i++) {
			
		}
		
		
		for (int i = 0; i < radii.size(); i++) {
			double r = radii.get(i);
			originalUnknownList.stream().filter(p -> Utilities.equalWithinEpsilon(p.getLocation().getR(), r, eps))
				.collect(Collectors.toList());
		}
		
//		iNewToOriginal = new int[tmpiNewToOriginal.size()][];
//		for (int i = 0; i < iNewToOriginal.length; i++) {
//			List<Integer> tmplist = tmpiNewToOriginal.get(i);
//			iNewToOriginal[i] = new int[tmplist.size()];
//			for (int j = 0; j < tmplist.size(); j++) {
//				iNewToOriginal[i][j] = tmplist.get(j);
//				System.out.print(iNewToOriginal[i][j] + " ");
//			}
//			System.out.println();
//		}
	}
	
	public void make() {
		
		
		
		for (int i = 0; i < unknowns.length; i++) {
			double rmean = 0;
			double weight = 0;
			for (int j = 0; j < iNewToOriginal[i].length; j++) {
				UnknownParameter unknown = originalUnknowns[iNewToOriginal[i][j]];
				rmean += unknown.getLocation().getR();
				weight += unknown.getWeighting();
			}
			rmean /= iNewToOriginal[i].length;
			
			UnknownParameter refUnknown = originalUnknowns[iNewToOriginal[i][0]];
			PartialType type = refUnknown.getPartialType();
			Location location = refUnknown.getLocation().toLocation(rmean);
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
	
	final double eps = 1e-6;
}
