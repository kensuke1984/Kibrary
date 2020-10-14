package io.github.kensuke1984.kibrary.util.statistics;

import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.datacorrection.TakeuchiStaticCorrection;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class AmplitudePolarDistribution {

	public static void main(String[] args) throws IOException {
		Path staticCorrectionPath = Paths.get(args[0]);
		Path timewindowPath = Paths.get(args[1]);
		
		Set<StaticCorrection> takeuchiCorrections = StaticCorrectionFile.read(staticCorrectionPath);
		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
		
		Path outpath = Paths.get("amplitudePolarDistribution.txt");
		
		double[] averages = new double[180];
		double[] averages_lm = new double[180];
		double[] averages_um = new double[180];
		int[] ns = new int[180];
		int[] ns_lm = new int[180];
		int[] ns_um = new int[180];
		
		Map<GlobalCMTID, Double> ratioPerId = new HashMap<>();
		Map<Phases, Double> ratioPerPhase = new HashMap<>();
		Map<GlobalCMTID, Integer> nPerId = new HashMap<>();
		Map<Phases, Integer> nPerPhase = new HashMap<>();
		
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			pw.println("#azimuth amplitude_ratio upper_mantle_ratio lower_mantle_ratio");
			for (StaticCorrection correction : takeuchiCorrections) {
				double azimuth = correction.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(correction.getStation().getPosition())
						* 180. / Math.PI;
				
				GlobalCMTID id = correction.getGlobalCMTID();
				Station station = correction.getStation();
				SACComponent component = correction.getComponent();
				double startTime = correction.getSynStartTime();
				Set<TimewindowInformation> tmpTimewindows = timewindows.parallelStream().filter(tw -> tw.getGlobalCMTID().equals(id)
						&& tw.getStation().equals(station)
						&& tw.getComponent() == component
						&& tw.getStartTime() == startTime)
						.collect(Collectors.toSet());
				
				if (tmpTimewindows.size() != 1) {
//					System.out.println("Ignoring non-unique timewindow " + id + " " + station + " " + startTime + " " + component);
					continue;
				}
				
				Phases phases = new Phases(tmpTimewindows.iterator().next().getPhases());
				
				int i = (int) (azimuth / 2.);
				if (i == 180)
					i = 179;
				if (phases.isUpperMantle()) {
					averages_um[i] += correction.getAmplitudeRatio();
					ns_um[i] += 1;
				}
				else if (phases.isLowerMantle()) {
					averages_lm[i] += correction.getAmplitudeRatio();
					ns_lm[i] += 1;
				}
				averages[i] += correction.getAmplitudeRatio();
				ns[i] += 1;
				
				if (azimuth > 180)
					azimuth -= 360.;
				
				if (phases.isUpperMantle())
					pw.println(azimuth + " " + correction.getAmplitudeRatio() + " " + correction.getAmplitudeRatio() + " NaN" );
				else if (phases.isLowerMantle())
					pw.println(azimuth + " " + correction.getAmplitudeRatio() + " NaN " + correction.getAmplitudeRatio());
				else
					pw.println(azimuth + " " + correction.getAmplitudeRatio() + " NaN NaN");
				
				Double idRatio = ratioPerId.get(id);
				if (ratioPerId.containsKey(id)) {
					idRatio += correction.getAmplitudeRatio();
					ratioPerId.replace(id, idRatio);
					Integer ntmp = nPerId.get(id) + 1;
					nPerId.replace(id, ntmp);
				}
				else {
					ratioPerId.put(id, new Double (correction.getAmplitudeRatio()));
					nPerId.put(id, new Integer(1));
				}
				
				Double phaseRatio = ratioPerPhase.get(phases);
				if (ratioPerPhase.containsKey(phases)) {
					phaseRatio += correction.getAmplitudeRatio();
					ratioPerPhase.replace(phases, phaseRatio);
					Integer ntmp = nPerPhase.get(phases) + 1;
					nPerPhase.replace(phases, ntmp);
				}
				else {
					ratioPerPhase.put(phases, new Double(correction.getAmplitudeRatio()));
					nPerPhase.put(phases, new Integer(1));
				}
			}
			
			pw.close();
		}
		
		Path outpathAverage = Paths.get("amplitudeAverage.txt");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpathAverage, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			pw.println("#azimuth average_amplitude_ratio");
			for (int i = 90; i < 180; i++) {
				if (ns[i] != 0) {
					double azimuth = i * 2. - 360.;
					pw.println(azimuth + " " + averages[i] / ns[i] + " " + averages_um[i] / ns_um[i] + " " + averages_lm[i] / ns_lm[i]);
				}
			}
			for (int i = 0; i < 90; i++) {
				if (ns[i] != 0) {
					double azimuth = i * 2.;
					pw.println(azimuth + " " + averages[i] / ns[i] + " " + averages_um[i] / ns_um[i] + " " + averages_lm[i] / ns_lm[i]);
				}
			}
			
			pw.close();
		}
		
		Path outpath2 = Paths.get("amplitudePolarDistribution.gmt");
		makeGMT(outpath2, outpath.toString());
		
		outpath2 = Paths.get("amplitudeDistribution.plt");
		makeGnuplot(outpath2, outpath.toString(), outpathAverage.toString());
		
		outpath2 = Paths.get("ratioPerId.inf");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath2, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			pw.println("#id Obs / Syn ratio nWindows");
			ratioPerId.forEach((id, ratio) -> {
				pw.println(id + " " + ratio / nPerId.get(id).intValue() + " " + nPerId.get(id).intValue());
			});
			pw.close();
		}
		
		outpath2 = Paths.get("ratioPerPhases.inf");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath2, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			pw.println("#Phases Obs / Syn ratio nWindows");
			ratioPerPhase.forEach((phase, ratio) -> {
				pw.println(phase + " " + ratio / nPerPhase.get(phase).intValue() + " " + nPerPhase.get(phase).intValue() );
			});
			pw.close();
		}
	}
	
	public static void makeGMT(Path outpath, String inputName) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			pw.println("#!/bin/sh\n"
			+ "outputps=\"amplitudePolarDistribution.ps\"\n"
			+ "gmt set FONT 20p,Helvetica,black\n"
			+ "gmt set PS_MEDIA 10x10\n"
			+ "gmt psxy " + inputName + " -R0/360/0/3 -JP7c -S+.3 -Gred -Bx90g45 -By1g.5 > $outputps\n"
			+ "gmt ps2raster $outputps -Tf"
			);
		}
	}
	
	public static void makeGnuplot(Path outpath, String inputName, String inputNameAverage) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			pw.println("set terminal postscript enhanced color font 'Helvetica,12'\n"
			+ "set xlabel \"Azimuth (deg)\"\n"
			+ "set ylabel \"Amplitude ratio (obs/syn)\"\n"
			+ "set ytics 1\n"
			+ "set mytics 2\n"
			+ "set xtics 20\n"
			+ "set mxtics 2\n"
			+ "set key top right\n"
			+ "set output 'amplitudeDistribution.ps'\n"
			+ "p \"" + inputName + "\" u 1:2 w points pt 1 lc rgb 'red' noti,\\\n"
			+ "\"" + inputName + "\" u 1:3 w points pt 1 lc rgb 'green' ti 'upper mantle',\\\n"
			+ "\"" + inputName + "\" u 1:4 w points pt 1 lc rgb 'blue' ti 'lower mantle',\\\n"
			+ "\"" + inputNameAverage + "\" u 1:2 w lines lc rgb 'cyan' lw 1. noti"
			);
		}
	}
	
}
