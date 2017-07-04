package io.github.kensuke1984.kibrary.util;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StationAverageTimeshift {

	public static void main(String[] args) throws IOException {
		if (args.length != 2)
			System.err.println("StaticCorrectionPath timewindowPath");
		Path staticCorrectionPath = Paths.get(args[0]);
		Path timewindowPath = Paths.get(args[1]);

		Set<StaticCorrection> corrections = StaticCorrectionFile.read(staticCorrectionPath);
		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);

		Map<Station, Double> stationAverages = new HashMap<Station, Double>();
		Map<Station, Integer> stationCount = new HashMap<Station, Integer>();
		
		for (TimewindowInformation tw : timewindows) {
			boolean contin = true;
			for (Phase p : tw.getPhases()) {
				if (p.equals(Phase.S) || p.equals(Phase.s))
					contin = false;
			}
			if (contin)
				continue;
			
			List<StaticCorrection> correctionList = corrections.stream().filter(corr -> corr.getGlobalCMTID().equals(tw.getGlobalCMTID()) 
					&& corr.getStation().equals(tw.getStation())
					&& corr.getComponent().equals(tw.getComponent()))
//					&& corr.getSynStartTime() < tw.getStartTime() + 0.1 && corr.getSynStartTime() > tw.getStartTime() - 0.1)
					.collect(Collectors.toList());
			if (correctionList.size() == 0) {
				System.out.println("No correction found for " + tw);
				continue;
			}
			StaticCorrection correction = correctionList.get(0);
			
			Station sta = correction.getStation();
			Double shift = correction.getTimeshift();
			if (stationAverages.containsKey(sta)) {
				shift = shift + stationAverages.get(sta);
				stationAverages.replace(sta, shift);
				stationCount.replace(sta, stationCount.get(sta) + 1);
			}
			else {
				stationAverages.put(sta, shift);
				stationCount.put(sta, 1);
			}
		}
		
		Path outpathP = Paths.get("stationAverageShift_positive.inf");
		Path outpathM = Paths.get("stationAverageShift_negative.inf");
		Files.deleteIfExists(outpathP);
		Files.createFile(outpathP);
		Files.deleteIfExists(outpathM);
		Files.createFile(outpathM);
		for (Station sta : stationCount.keySet()) {
			double shift = stationAverages.get(sta) / stationCount.get(sta);
			if (shift >= 0)
				Files.write(outpathP, (sta.getStationName() + " " + sta.getNetwork() + " " + sta.getPosition() + " " + shift + "\n").getBytes(), StandardOpenOption.APPEND);
			else
				Files.write(outpathM, (sta.getStationName() + " " + sta.getNetwork() + " " + sta.getPosition() + " " + shift + "\n").getBytes(), StandardOpenOption.APPEND);
		}
	}

}
