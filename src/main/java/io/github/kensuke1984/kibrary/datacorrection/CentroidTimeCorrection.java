package io.github.kensuke1984.kibrary.datacorrection;

import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CentroidTimeCorrection {
	
	private Map<GlobalCMTID, Double> centroidShiftMap;
	private Map<GlobalCMTID, Double> centroidMedianShiftMap;
	Set<StaticCorrection> corrections;
	
	public static void main(String[] args) throws IOException {
		Path staticCorrectionPath = Paths.get(args[0]);
		Set<StaticCorrection> corrections = StaticCorrectionFile.read(staticCorrectionPath);
		
//		Path staticCorrectionPath2 = Paths.get(args[1]);
//		Set<StaticCorrection> corrections2 = StaticCorrectionFile.read(staticCorrectionPath2);
		
		CentroidTimeCorrection corrector = new CentroidTimeCorrection(corrections);
		corrector.computeCentroidShift();
		
		Map<GlobalCMTID, Double> centroidShiftMap = corrector.getCentroidShiftMap();
		Map<GlobalCMTID, Double> centroidMedianShiftMap = corrector.getCentroidMedianShiftMap();
		
		Path outpath = Paths.get("centroidTimeCorrection.inf");
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (GlobalCMTID id : centroidShiftMap.keySet())
			pw.println(id + " " + centroidShiftMap.get(id) + " " + centroidMedianShiftMap.get(id));
		pw.close();
		
//		Set<StaticCorrection> newCorrections = corrector.addCentroidShift(corrections2);
		Set<StaticCorrection> centroidCorrections = new HashSet<>();
		for (StaticCorrection corr : corrections) {
			double shift = centroidShiftMap.get(corr.getGlobalCMTID());
			StaticCorrection tmpcorr = new StaticCorrection(corr.getStation(), corr.getGlobalCMTID()
					, corr.getComponent(), corr.getSynStartTime(), shift, corr.getAmplitudeRatio(), corr.getPhases());
			centroidCorrections.add(corr);
		}
		Path outpath2 = Paths.get("centroidTimeCorrection.dat");
		StaticCorrectionFile.write(centroidCorrections, outpath2);
	}
	
	public CentroidTimeCorrection(Set<StaticCorrection> corrections) {
		centroidShiftMap = new HashMap<>();
		centroidMedianShiftMap = new HashMap<>();
		this.corrections = corrections;
	}
	
	public void computeCentroidShift() throws IOException {
		
		Set<GlobalCMTID> events = corrections.stream().map(corr -> corr.getGlobalCMTID())
				.collect(Collectors.toSet());
		Set<Station> stations = corrections.stream().map(corr -> corr.getStation()).collect(Collectors.toSet());
		
		Map<GlobalCMTID, Integer> centroidCounter = new HashMap<>();
		for (GlobalCMTID event : events) {
			centroidShiftMap.put(event, 0.);
			centroidCounter.put(event, 0);
			
			List<Double> eventShifts = new ArrayList<>();
			for (StaticCorrection corr : corrections) {
				if (!corr.getGlobalCMTID().equals(event))
					continue;
				if (Math.abs(corr.getTimeshift()) < 10)
					eventShifts.add(corr.getTimeshift());
			}
			Collections.sort(eventShifts);
			
			double medianShift = 0;
			int n = eventShifts.size();
			if (n % 2 == 0)
				medianShift = .5 * (eventShifts.get(n/2-1) + eventShifts.get(n/2));
			else
				medianShift = eventShifts.get(n/2);
			
			centroidMedianShiftMap.put(event, medianShift);
		}
		
		for (StaticCorrection corr : corrections) {
			double tmpshift = centroidShiftMap.get(corr.getGlobalCMTID());
			tmpshift += corr.getTimeshift();
			centroidShiftMap.put(corr.getGlobalCMTID(), tmpshift);
			int counter = centroidCounter.get(corr.getGlobalCMTID());
			counter += 1;
			centroidCounter.put(corr.getGlobalCMTID(), counter);
		}
		
		for (GlobalCMTID event : events) {
			double shift = centroidShiftMap.get(event);
			int count = centroidCounter.get(event);
			centroidShiftMap.replace(event, shift / count);
		}
	}
	
	public Set<StaticCorrection> addCentroidShift(Set<StaticCorrection> corrections) {
		Set<StaticCorrection> newCorrections = new HashSet<>();
		
		for (StaticCorrection corr : corrections) {
			GlobalCMTID id = corr.getGlobalCMTID();
			double centroidShift = centroidShiftMap.get(id);
			double shift = corr.getTimeshift() + centroidShift;
			StaticCorrection newcorr = new StaticCorrection(corr.getStation(), corr.getGlobalCMTID()
					, corr.getComponent(), corr.getSynStartTime(), shift, corr.getAmplitudeRatio(), corr.getPhases());
			newCorrections.add(newcorr);
		}
		
		return newCorrections;
	}
	
	public Map<GlobalCMTID, Double> getCentroidShiftMap() {
		return centroidShiftMap;
	}
	
	public Map<GlobalCMTID, Double> getCentroidMedianShiftMap() {
		return centroidMedianShiftMap;
	}
}
