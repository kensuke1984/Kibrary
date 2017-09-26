/**
 * 
 */
package io.github.kensuke1984.kibrary.waveformdata;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.util.Precision;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

/**
 * @version 0.0.1
 * @since 2017/08/29
 * @author Yuki
 *
 */
public class MaxAmplitudeGet {
	
	private static Set<TimewindowInformation> timewindowInformationSet;
	private static double sacSamplingHz = 20. ;
	private static double finalSamplingHz = 1. ;
	private static Path synEventPath;
	private static Path timewindowPath;
	private static Path maxAmplitudeFile;
	private static SACComponent comp;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		Path homePath = Paths.get(args[0]);
		timewindowPath = homePath.resolve(args[1]);
		synEventPath = homePath.resolve(args[2]);
		maxAmplitudeFile = homePath.resolve(args[3]);
		comp= SACComponent.valueOf(args[4]);
		compute();
	}
	
	public static void compute() throws IOException {
		if (!Files.exists(synEventPath))
			throw new RuntimeException(synEventPath + " does not exist.");
		Set<SACFileName> synFiles;
		try {
			(synFiles = new EventFolder(synEventPath).sacFileSet()).removeIf(sfn -> !sfn.isSYN());
		} catch (IOException e2) {
			e2.printStackTrace();
			return;
		}
		
		timewindowInformationSet = TimewindowInformationFile.read(timewindowPath);
		
		try (PrintWriter pw1 = new PrintWriter(Files.newBufferedWriter(maxAmplitudeFile))) {
			synFiles.stream().filter(synFileName -> !synFileName.isConvolved())
				.forEach(synFileName -> {
//			for (SACFileName synFileName : synFiles) {
				String stationName = synFileName.getStationName();
				GlobalCMTID id = synFileName.getGlobalCMTID();
				SACComponent component = synFileName.getComponent();
				Set<TimewindowInformation> windows = timewindowInformationSet.stream()
					.filter(info -> info.getStation().getName().equals(stationName))
					.filter(info -> info.getGlobalCMTID().equals(id))
					.filter(info -> info.getComponent() == component)
					.filter(info -> info.getComponent().equals(comp))
					.collect(Collectors.toSet());
				try {
					Station station = synFileName.readHeader().getStation();
					double epiDis = Precision.round(Math.toDegrees(new HorizontalPosition(0, 0).getEpicentralDistance(station.getPosition())), 1);
					SACData synSac = synFileName.read();
					for (TimewindowInformation window : windows) {
						double maxAmplitude = computeMaxAmplitude(window, synSac);
						double maxAmplitude2 = Math.pow(maxAmplitude, 2);
						pw1.println( station.getName()+ " " 
								+ (int)(epiDis*1.)/1. + " " + maxAmplitude + " " + maxAmplitude2);
					}
				} catch (IOException e1) {
					System.err.println("error occured in reading " + synFileName);
					e1.printStackTrace();
				}			
			});//for
		}	
	}
	
	public static double computeMaxAmplitude (TimewindowInformation window, SACData synSac) {
		int npts = (int) ((window.getEndTime() - window.getStartTime()) * finalSamplingHz);
		double startTime = window.getStartTime();
		double[] synData = cutDataSac(synSac, startTime, npts);
		double min = minOfArray(synData);
		double max = maxOfArray(synData);
		return Math.max(Math.abs(max), Math.abs(min));		
	}
	
	public static double computeMaxAmplitude (BasicID basicID) {
		int npts = basicID.getNpts();
		double startTime = basicID.getStartTime();
		double[] synData = cutDataSac(basicID.getTrace(), startTime, npts);
		double min = minOfArray(synData);
		double max = maxOfArray(synData);
		return Math.max(Math.abs(max), Math.abs(min));
	}
	
	private static double[] cutDataSac(SACData sac, double startTime, int npts) {
		Trace trace = sac.createTrace();
		int step = (int) (sacSamplingHz / finalSamplingHz);
		int startPoint = trace.getNearestXIndex(startTime);
		double[] waveData = trace.getY();
		return IntStream.range(0, npts).parallel().mapToDouble(i -> waveData[i * step + startPoint]).toArray();
	}
	
	private static double[] cutDataSac(Trace trace, double startTime, int npts) {
		int step = (int) (sacSamplingHz / finalSamplingHz);
		int startPoint = trace.getNearestXIndex(startTime);
		double[] waveData = trace.getY();
		return IntStream.range(0, npts).parallel().mapToDouble(i -> waveData[i * step + startPoint]).toArray();
	}
	
	public static double maxOfArray(double[] a) {
		  Arrays.sort(a);
		  double max = a[a.length - 1];
		  return max;
		}
	
	public static double minOfArray(double[] a) {
		  Arrays.sort(a);
		  double min = a[0];
		  return min;
		}
	
}
