package io.github.kensuke1984.kibrary.util.addons;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StationAverageRatio {

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
//			if (!tw.getGlobalCMTID().equals(new GlobalCMTID("200608250044A")))
//				continue;
			
			boolean contin = true;
			for (Phase p : tw.getPhases()) {
				if (p.equals(Phase.S) || p.equals(Phase.s))
					contin = false;
			}
//			Phases phases = new Phases(tw.getPhases());
//			if (phases.equals(new Phases(new Phase[] {Phase.S})) || phases.equals(new Phases(new Phase[] {Phase.s})))
//				contin = false;
			
			if (contin)
				continue;
			
			StaticCorrection correction = corrections.stream().filter(corr -> corr.getGlobalCMTID().equals(tw.getGlobalCMTID()) 
					&& corr.getStation().equals(tw.getStation())
					&& corr.getComponent().equals(tw.getComponent())
					&& corr.getSynStartTime() == tw.getStartTime())
					.findAny().get();
			
			Station sta = correction.getStation();
			Double ratio = correction.getAmplitudeRatio();
			if (stationAverages.containsKey(sta)) {
				ratio = ratio + stationAverages.get(sta);
				stationAverages.replace(sta, ratio);
				stationCount.replace(sta, stationCount.get(sta) + 1);
			}
			else {
				stationAverages.put(sta, ratio);
				stationCount.put(sta, 1);
			}
		}
		
		Path outpathP = Paths.get("stationAverageRatio_greater.inf");
		Path outpathM = Paths.get("stationAverageRatio_smaller.inf");
		Files.deleteIfExists(outpathP);
		Files.createFile(outpathP);
		Files.deleteIfExists(outpathM);
		Files.createFile(outpathM);
		for (Station sta : stationCount.keySet()) {
			double ratio = stationAverages.get(sta) / stationCount.get(sta);
			if (ratio >= 1)
				Files.write(outpathP, (sta.getName() + " " + sta.getNetwork() + " " + sta.getPosition() + " " + ratio + "\n").getBytes(), StandardOpenOption.APPEND);
			else
				Files.write(outpathM, (sta.getName() + " " + sta.getNetwork() + " " + sta.getPosition() + " " + ratio + "\n").getBytes(), StandardOpenOption.APPEND);
		}
	}

}
