package io.github.kensuke1984.kibrary.util.addons;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;

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
		
		Map<HorizontalPosition, Double> histogramAverage = new HashMap<>();
		Map<HorizontalPosition, Double> histogramVariance = new HashMap<>();
		Map<HorizontalPosition, Integer> histogramCount = new HashMap<>();
		Map<HorizontalPosition, Double> histogramRatio = new HashMap<>();
		
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
			
			HorizontalPosition pos = getBin(sta.getPosition(), 1., 1.);
			shift = correction.getTimeshift();
			Double ratio = correction.getAmplitudeRatio();
			if (histogramAverage.containsKey(pos)) {
				shift = shift + histogramAverage.get(pos);
				histogramAverage.replace(pos, shift);
				ratio = ratio + histogramRatio.get(pos);
				histogramRatio.replace(pos, ratio);
				histogramCount.replace(pos, histogramCount.get(pos) + 1);
			}
			else {
				histogramAverage.put(pos, shift);
				histogramRatio.put(pos, ratio);
				histogramCount.put(pos, 1);
			}
		}
		
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
			
			HorizontalPosition pos = getBin(sta.getPosition(), 1., 1.);
			double mu = histogramAverage.get(pos) / histogramCount.get(pos);
			Double var = (shift - mu) * (shift - mu);
			
			if (histogramVariance.containsKey(pos)) {
				var = var + histogramVariance.get(pos);
				histogramVariance.replace(pos, var);
			}
			else {
				histogramVariance.put(pos, var);
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
				Files.write(outpathP, (sta.getName() + " " + sta.getNetwork() + " " + sta.getPosition() + " " + shift + "\n").getBytes(), StandardOpenOption.APPEND);
			else
				Files.write(outpathM, (sta.getName() + " " + sta.getNetwork() + " " + sta.getPosition() + " " + shift + "\n").getBytes(), StandardOpenOption.APPEND);
		}
		
		Path outpathHistogramAverage = Paths.get("histogramStationAverageShift.inf");
		Path outpathHistogramVariance = Paths.get("histogramStationVariance.inf");
		Path outpathHistogramRatio = Paths.get("histogramStationRatio.inf");
		Files.deleteIfExists(outpathHistogramAverage);
		Files.createFile(outpathHistogramAverage);
		Files.deleteIfExists(outpathHistogramVariance);
		Files.createFile(outpathHistogramVariance);
		Files.deleteIfExists(outpathHistogramRatio);
		Files.createFile(outpathHistogramRatio);
		for (HorizontalPosition pos : histogramAverage.keySet()) {
			double shift = histogramAverage.get(pos) / histogramCount.get(pos);
			double ratio = histogramRatio.get(pos) / histogramCount.get(pos);
			double var = Double.NaN;
			if (histogramCount.get(pos) > 1)
				var = Math.sqrt(histogramVariance.get(pos) / (histogramCount.get(pos) - 1));
			Files.write(outpathHistogramAverage, (pos + " " + shift + "\n").getBytes(), StandardOpenOption.APPEND);
			Files.write(outpathHistogramVariance, (pos + " " + var + "\n").getBytes(), StandardOpenOption.APPEND);
			Files.write(outpathHistogramRatio, (pos + " " + ratio + "\n").getBytes(), StandardOpenOption.APPEND);
		}
	}
	
	
	private static HorizontalPosition getBin(HorizontalPosition pos, double dlat, double dlon) {
		double lat = 0;
		double lon = 0;
		if (pos.getLatitude() >= 0)
			lat = (int) (pos.getLatitude() / dlat) * dlat + dlat / 2.;
		else
			lat = (int) (pos.getLatitude() / dlat) * dlat - dlat / 2.;
		if (pos.getLongitude() >= 0)
			lon = (int) (pos.getLongitude() / dlon) * dlon + dlon / 2.;
		else
			lon = (int) (pos.getLongitude() / dlon) * dlon - dlon / 2.;
		
		return new HorizontalPosition(lat, lon);
	}
}
