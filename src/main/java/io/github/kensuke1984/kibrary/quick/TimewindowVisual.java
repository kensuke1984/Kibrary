package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.butterworth.LowPassFilter;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.math.HilbertTransform;
import io.github.kensuke1984.kibrary.selection.Bins2D;
import io.github.kensuke1984.kibrary.selection.SurfaceWaveDetector;
import io.github.kensuke1984.kibrary.timewindow.Timewindow;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.DistanceAzimuth;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import ucar.nc2.dataset.transform.AzimuthalEquidistant;



public class TimewindowVisual {
	
	public static final double t65 = 1080.;
	public static final double t100 = 1440.;
	
	public static void main(String[] args) throws IOException {
		Path root = Paths.get(".");
		Path timewindowPath = Paths.get("selectedTimewindow.dat");
//		Path staticCorrectionPath = Paths.get("staticCorrection.dat");
		List<Path> staticCorrectionPaths = new ArrayList<>();
		Set<GlobalCMTID> ids = new HashSet<>();
		if (args.length > 0) {
			ids.add(new GlobalCMTID(args[0]));
			for (int i = 1; i < args.length; i++)
				staticCorrectionPaths.add(Paths.get(args[i]));
		}
		else
			ids = Utilities.eventFolderSet(root).stream()
				.map(event -> event.getGlobalCMTID()).collect(Collectors.toSet());
		
		List<Set<StaticCorrection>> correctionList = new ArrayList();
		for (Path path : staticCorrectionPaths)
			correctionList.add(StaticCorrectionFile.read(path));
//		Set<StaticCorrection> corrections = StaticCorrectionFile.read(staticCorrectionPath);
		
		for (GlobalCMTID id : ids) {
			final GlobalCMTID id_ = id;
			
			List<Set<StaticCorrection>> correctionListThisID = new ArrayList();
			for (Set<StaticCorrection> corrections : correctionList)
				correctionListThisID.add(corrections.stream().filter(corr -> corr.getGlobalCMTID().equals(id_))
					.collect(Collectors.toSet()));
//			Set<StaticCorrection> correctionThisID = corrections.stream().filter(corr -> corr.getGlobalCMTID().equals(id_))
//					.collect(Collectors.toSet());
			
			Path dirPath = root.resolve("timewindowVisual");
			Path dir1 = dirPath.resolve(id.toString());
			if (!Files.exists(dirPath))
				Files.createDirectories(dirPath);
			if (!Files.exists(dir1))
				Files.createDirectories(dir1);
			Path outpath = dir1.resolve("twvisual_" + id.toString() + ".gmt");
			Path outpathProfile = dir1.resolve("twprofile_" + id.toString() + ".gmt");
			Path outpathStack = dir1.resolve("twstack_" + id.toString() + ".gmt");
			
			Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath).stream()
					.filter(tw -> tw.getGlobalCMTID().equals(id_)).collect(Collectors.toSet());
			
			try {
				List<String> text = new ArrayList<>();
				List<String> textProfile = new ArrayList<>();
				
				String[] gmtSets = new String[] {"PS_MEDIA 18cx2000c"
						, "FONT 10p,Helvetica,black"
						, "PS_CHAR_ENCODING ISOLatin1"
						, "PS_PAGE_ORIENTATION portrait"};
				
				String[] gmtSets2 = new String[] {"PS_MEDIA 18cx32c"
						, "FONT 14p,Helvetica,black"
						, "PS_CHAR_ENCODING ISOLatin1"
						, "PS_PAGE_ORIENTATION portrait"
						, "MAP_FRAME_PEN .5p"};
				
				text.add(beginGMT(dir1.toString() + "/" + id.toString() + ".ps"
						, "0/260/-3/3"
						, "X6c/9c"
						, gmtSets
						, new String[] {"-Bsenw -Y-2c"}));
				
				textProfile.add(beginGMT(dir1.toString() + "/" + "profile." + id.toString() + ".ps"
						, "0/4000/0/75"
						, "X14c/28c"
						, gmtSets2
						, new String[] {"-BSW -Bx1000 -By20"}));
				
				AtomicInteger iatom = new AtomicInteger();
				
				Set<Station> usedStations = new HashSet<>();
				
				// MTZ
				double slowness = 15.;
				// D"
//				double slowness = 7.4;
				
//				Map<Integer, Trace> obsStacks = new HashMap<>();
//				Map<Integer, Trace> synStacks = new HashMap<>();
//				Map<Integer, double[]> mapWindows = new HashMap<>();
//				
//				timewindows.stream().filter(tw -> tw.getGlobalCMTID().equals(id))
//					.forEach(tw -> {
//						try {
//							Path synPath = root.resolve(id.toString() + "/" + tw.getStation().getStationName() + "." + id.toString() + "." + "Tsc");
//							SACFileName synName = new SACFileName(synPath);
//							Path obsPath = root.resolve(id.toString() + "/" + tw.getStation().getStationName() + "." + id.toString() + "." + "T");
//							
//							boolean isObs = true;
//							if (!Files.exists(obsPath))
//								isObs = false;
//							
//							SACFileName obsName = null;
//							if (isObs)
//								obsName = new SACFileName(obsPath);
//							
//							double Y = iatom.getAndIncrement() * 4;
//							
//							double distance = tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition())
//									* 180 / Math.PI;
//							double azimuth = tw.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(tw.getStation().getPosition())
//									* 180 / Math.PI;
//							
//							//stack bin
//							int bin = (int) distance;
//							
//							text.add(String.format("gmt pstext -J -R -Xa-1c -Ya%.2fc -K -O -N -F+jLB >> $outputps << END\n", Y)
//									+ "0 .3 " + tw.getStation().getStationName() + " " + tw.getStation().getNetwork() + "\n"
//									+ String.format("0 0 %.1f\n0 -.3 %s", distance, new Phases(tw.getPhases()).toString()) + "\nEND");
//							
//							if (!usedStations.contains(tw.getStation())) {
//								double normalize = 1.;
//								Trace synFullTrace = synName.read().createTrace().cutWindow(slowness * distance, slowness * distance + 3500.);
//								
//								//stacks
//								if (synStacks.containsKey(bin)) {
//									Trace trace0 = synStacks.get(bin);
//									Trace trace1 = new Trace(trace0.getX(), synFullTrace.getY());
//									Trace trace = trace0.add(trace1);
//									synStacks.replace(bin, trace);
//								}
//								else {
//									synStacks.put(bin, synFullTrace);
//									mapWindows.put(bin, new double[] {tw.getStartTime() - slowness * distance, tw.getEndTime() - slowness * distance});
//								}
//								
//								if (isObs) {
//									Trace obsFullTrace = obsName.read().createTrace().cutWindow(slowness * distance, slowness * distance + 3500.);
//									
//									//stacks
//									if (obsStacks.containsKey(bin)) {
//										Trace trace0 = obsStacks.get(bin);
//										Trace trace1 = new Trace(trace0.getX(), obsFullTrace.getY());
//										Trace trace = trace0.add(trace1);
//										obsStacks.replace(bin, trace);
//									}
//									else {
//										obsStacks.put(bin, obsFullTrace);
//									}
//									
//									normalize = 1. / obsFullTrace.getYVector().getLInfNorm();
//									obsFullTrace = obsFullTrace.multiply(normalize);
//									textProfile.add(trace2GMT(obsFullTrace, "black", 0, 0, distance, slowness, 40));
//								}
//								else 
//									normalize = 1. / synFullTrace.getYVector().getLInfNorm();
//								synFullTrace = synFullTrace.multiply(normalize);
//								textProfile.add(trace2GMT(synFullTrace, "red", 0, 0, distance, slowness, 40));
//								
//								usedStations.add(tw.getStation());
//							}
//							textProfile.add(verticalLine(tw.getStartTime() - slowness * distance, distance, 3, "green", 0, 0));
//							textProfile.add(verticalLine(tw.getEndTime() - slowness * distance, distance, 3, "green", 0, 0));
//							
//							Trace synTrace = synName.read().createTrace().cutWindow(tw.getStartTime() - 40, tw.getEndTime() + 100);
//							double normalize = 1.;
//							if (isObs) {
//								Trace obsTrace = obsName.read().createTrace().cutWindow(tw.getStartTime() - 40, tw.getEndTime() + 100);
//								normalize = 1. / obsTrace.getYVector().getLInfNorm();
//								obsTrace = obsTrace.multiply(normalize);
//								text.add(trace2GMT(obsTrace, "black", 2, Y, 0, 0., 40));
//							}
//							else
//								normalize = 1. / synTrace.getYVector().getLInfNorm();
//							synTrace = synTrace.multiply(normalize);
//							
//							text.add(trace2GMT(synTrace, "red", 2, Y, 0, 0., 40));
//							
//							text.add(verticalLine(40, 0, 1, "green", 2, Y));
//							text.add(verticalLine((tw.getEndTime() - tw.getStartTime() + 40), 0, 1, "green", 2, Y));
//	
//							
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
//					});
//				
//				text.add(endGMT());
//				textProfile.add(endGMT());
//				
//				createGMTFile(outpath);
//				
//				for (String line : text)
//					Files.write(outpath, (line + "\n").getBytes(), StandardOpenOption.APPEND);
//				
//				createGMTFile(outpathProfile);
//				for (String line : textProfile)
//					Files.write(outpathProfile, (line + "\n").getBytes(), StandardOpenOption.APPEND);
//				
//				//stack
//				Files.deleteIfExists(outpathStack);
//				Files.createFile(outpathStack);
//				createGMTFile(outpathStack);
//				String begin = beginGMT(dirPath.toString() + "stack." + id.toString() + ".ps"
//						, "0/1000/0/75"
//						, "X14c/21c"
//						, gmtSets2
//						, new String[] {"-BSW -Bx1000 -By20"});
//				Files.write(outpathStack, (begin + "\n").getBytes(), StandardOpenOption.APPEND);
//				for (int bin : synStacks.keySet()) {
//					Trace obs = obsStacks.get(bin);
//					Trace syn = synStacks.get(bin);
//					double[] tw = mapWindows.get(bin);
//					double normalize = 1. / syn.getYVector().getLInfNorm();
//					obs = obs.multiply(normalize);
//					syn = syn.multiply(normalize);
//					String obsString = trace2GMT(obs, "black", 0, 0, bin, slowness, 10);
//					String synString = trace2GMT(syn, "red", 0, 0, bin, slowness, 10);
//					String twString0 = verticalLine(tw[0], bin, 3, "green", 0, 0);
//					String twString1 = verticalLine(tw[1], bin, 3, "green", 0, 0);
//					Files.write(outpathStack, (obsString + "\n").getBytes(), StandardOpenOption.APPEND);
//					Files.write(outpathStack, (synString + "\n").getBytes(), StandardOpenOption.APPEND);
//					Files.write(outpathStack, (twString0 + "\n" + twString1 + "\n").getBytes(), StandardOpenOption.APPEND);
//				}
//				Files.write(outpathStack, endGMT().getBytes(), StandardOpenOption.APPEND);
				
				
				//bins
				Bins2D bins = new Bins2D(id, 5., .33, timewindows);
				Map<DistanceAzimuth, Integer> usedBinsCount = new HashMap<>();
				Set<Path> outpathList = new HashSet<>();
				for (TimewindowInformation timewindow : timewindows) {
					if (usedStations.contains(timewindow.getStation()))
						continue;
					
					List<Double> timeShiftList = new ArrayList();
					for (Set<StaticCorrection> corrections : correctionListThisID) {
						Phases phases = new Phases(timewindow.getPhases());
						timeShiftList.add(corrections.stream().filter(corr -> corr.getStation().equals(timewindow.getStation())
								&& new Phases(corr.getPhases()).equals(phases) ).findFirst().get().getTimeshift());
					}
//								&& Math.abs(corr.getSynStartTime() - timewindow.getStartTime()) < 2. ).findFirst().get().getTimeshift());
//					double timeShift = correctionThisID.stream().filter(corr -> corr.getStation().equals(timewindow.getStation())
//							&& Math.abs(corr.getSynStartTime() - timewindow.getStartTime()) < 2. ).findFirst().get().getTimeshift();
//					System.out.println(timeShift);
					
					DistanceAzimuth distance_azimuth = bins.getBinPosition(timewindow);
					Path outpathBins = dir1.resolve(String.format("twv_bins_az%.1f.gmt", distance_azimuth.azimuth));
					outpathList.add(outpathBins);
					if (usedBinsCount.containsKey(distance_azimuth)) {
						Integer count = usedBinsCount.get(distance_azimuth) + 1;
						usedBinsCount.replace(distance_azimuth, count);
					}
					else {
						Integer count = 1;
						usedBinsCount.put(distance_azimuth, count);
					}
					if (usedBinsCount.get(distance_azimuth) == 1)
					{
						usedStations.add(timewindow.getStation());
						
						Path synPath = root.resolve(id.toString() + "/" + timewindow.getStation().getName() + "." + id.toString() + "." + "Tsc");
						SACFileName synName = new SACFileName(synPath);
						Path obsPath = root.resolve(id.toString() + "/" + timewindow.getStation().getName() + "." + id.toString() + "." + "T");
						
						boolean isObs = true;
						if (!Files.exists(obsPath))
							isObs = false;
						
						SACFileName obsName = null;
						if (isObs)
							obsName = new SACFileName(obsPath);
						
						double distance = timewindow.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(timewindow.getStation().getPosition())
								* 180 / Math.PI;
		//				double azimuth = timewindow.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(timewindow.getStation().getPosition())
		//						* 180 / Math.PI;
						
						double normalize = 1.;
//						double t0 = slowness * distance > 0. ? slowness * distance : 0.;
//						double t1 = slowness * distance + 2000. < 4000. ? slowness * distance + 2000. : 2000.;
						double t0 = timewindow.getStartTime();
						double t1 = timewindow.getEndTime();
						Trace synFullTrace = synName.read().createTrace().cutWindow(t0, t1);
						normalize = 1. / synFullTrace.getYVector().getLInfNorm();
						Trace obsFullTrace = null;
						if (isObs) {
							obsFullTrace = obsName.read().createTrace().cutWindow(t0, t1);
							normalize = 1. / obsFullTrace.getYVector().getLInfNorm();
							obsFullTrace = obsFullTrace.multiply(normalize);
						}
						synFullTrace = synFullTrace.multiply(normalize);
						
						// get envelope
//						HilbertTransform H = new HilbertTransform(synFullTrace.getY());
//						double[] envelope = H.getEnvelope();
//						Trace synEnvelopeTrace = new Trace(synFullTrace.getX(), envelope);
//						
//						H = new HilbertTransform(obsFullTrace.getY());
//						envelope = H.getEnvelope();
//						Trace obsEnvelopeTrace = new Trace(obsFullTrace.getX(), envelope);
						
						// detect surface wave window
//						SurfaceWaveDetector detector = new SurfaceWaveDetector(synFullTrace, 20.0);
//						Timewindow tw = detector.getSurfaceWaveWindow();
//						if (tw != null)
//							tw = new Timewindow(tw.getStartTime(), tw.getEndTime());
//						Trace twoBitsTrace = detector.getTwoBitsTrace();
						
						String obsString = null;
						String obsEnvelopeString = null;
						String[] colors = new String[] {"black", "blue", "green"};
						List<String> obsStringList = new ArrayList<>();
						if (isObs) {
//							obsStringList.add(trace2GMT(obsFullTrace, "black", 0, 0, distance_azimuth.distance, slowness, 10)); uncomment for TZ
							for (int k = 0; k < timeShiftList.size(); k++)
								obsStringList.add(trace2GMT(obsFullTrace, colors[k], 0, 0, distance_azimuth.distance, slowness, 10, timeShiftList.get(k)));
//							obsEnvelopeString = trace2GMT(obsEnvelopeTrace, "cyan", 0, 0, distance_azimuth.distance, slowness, 10);
						}
						String synString = trace2GMT(synFullTrace, "red", 0, 0, distance_azimuth.distance, slowness, 10);
//						String synEnvelopeString = trace2GMT(synEnvelopeTrace, "magenta", 0, 0, distance_azimuth.distance, slowness, 10);
//						String twoBitsString = trace2GMT(twoBitsTrace, "black", 0, 0, distance_azimuth.distance, slowness, 10);
						
						if (!Files.exists(outpathBins)) {
							createGMTFile(outpathBins);
							Files.write(outpathBins, (beginGMT(String.format("bins_az%.1f_", distance_azimuth.azimuth) + id.toString() + ".ps"
									//MTZ
//									, "100/300/13/37"
									//D"
									, "650/850/69/101"
									, "X14c/28c"
									, gmtSets2
									, new String[] {"-BSWne -Bx50f25 -By5f2.5"}) + "\n").getBytes()
								, StandardOpenOption.APPEND);
							if (isObs) {
//								obsStringList.add(trace2GMT(obsFullTrace, "black", 0, 0, distance_azimuth.distance, slowness, 10));
								for (int k = 0; k < timeShiftList.size() + 1; k++)
									Files.write(outpathBins, (obsStringList.get(k) + "\n").getBytes(), StandardOpenOption.APPEND);
								Files.write(outpathBins, (obsEnvelopeString + "\n").getBytes(), StandardOpenOption.APPEND);
							}
							Files.write(outpathBins, (synString + "\n").getBytes(), StandardOpenOption.APPEND);
//							Files.write(outpathBins, (synEnvelopeString + "\n").getBytes(), StandardOpenOption.APPEND);
//							Files.write(outpathBins, (twoBitsString + "\n").getBytes(), StandardOpenOption.APPEND);
//							if (tw != null) {
//								Files.write(outpathBins, (verticalLine(tw.getStartTime(), distance_azimuth.distance, .6, .7, "magenta", 0, 0) + "\n").getBytes(), StandardOpenOption.APPEND);
//								Files.write(outpathBins, (verticalLine(tw.getEndTime(), distance_azimuth.distance, .6, .7, "magenta", 0, 0) + "\n").getBytes(), StandardOpenOption.APPEND);
//							}
						}
						else {
							if (isObs) {
								for (int k = 0; k < timeShiftList.size() + 1; k++)
									Files.write(outpathBins, (obsStringList.get(k) + "\n").getBytes(), StandardOpenOption.APPEND);
//								Files.write(outpathBins, (obsEnvelopeString + "\n").getBytes(), StandardOpenOption.APPEND);
							}
							Files.write(outpathBins, (synString + "\n").getBytes(), StandardOpenOption.APPEND);
//							Files.write(outpathBins, (synEnvelopeString + "\n").getBytes(), StandardOpenOption.APPEND);
//							Files.write(outpathBins, (twoBitsString + "\n").getBytes(), StandardOpenOption.APPEND);
//							if (tw != null) {
//								Files.write(outpathBins, (verticalLine(tw.getStartTime(), distance_azimuth.distance, .6, .7, "magenta", 0, 0) + "\n").getBytes(), StandardOpenOption.APPEND);
//								Files.write(outpathBins, (verticalLine(tw.getEndTime(), distance_azimuth.distance, .6, .7, "magenta", 0, 0) + "\n").getBytes(), StandardOpenOption.APPEND);
//							}
						}
					}
				}
				for (TimewindowInformation timewindow : timewindows) {
					if (!usedStations.contains(timewindow.getStation()))
						continue;
//					double distance = timewindow.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(timewindow.getStation().getPosition())
//							* 180 / Math.PI;
					DistanceAzimuth distance_azimuth = bins.getBinPosition(timewindow);
					double t0 = timewindow.getStartTime() - distance_azimuth.distance * slowness;
					double t1 = timewindow.getEndTime() - distance_azimuth.distance * slowness;
					Path outpathBins = dir1.resolve(String.format("twv_bins_az%.1f.gmt", distance_azimuth.azimuth));
//					Files.write(outpathBins, (verticalLine(t0, distance_azimuth.distance, .6, .3, "purple", 0, 0) + "\n").getBytes(), StandardOpenOption.APPEND);
//					Files.write(outpathBins, (verticalLine(t1, distance_azimuth.distance, .6, .2, "green", 0, 0) + "\n").getBytes(), StandardOpenOption.APPEND);
				}
				for (Path outpathBins : outpathList) {
					int depth = (int) (Earth.EARTH_RADIUS - id.getEvent().getCmtLocation().getR());
					System.out.println(depth);
					double[][] Scurve = StraveltimeCurveAK135(depth);
					double[][] S2curve = S2traveltimeCurveAK135(depth);
					if (Scurve != null) {
						Files.write(outpathBins, curve(Scurve, slowness, "blue", 0, 0).getBytes(), StandardOpenOption.APPEND);
//						Files.write(outpathBins, curve(S2curve, slowness, "pink", 0, 0).getBytes(), StandardOpenOption.APPEND);
					}
					Files.write(outpathBins, endGMT().getBytes(), StandardOpenOption.APPEND);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String verticalLine(double xpos, double ypos, double height, double thickness, String color, double X, double Y) {
		return "gmt psxy -J -R -B -W" + thickness + "p," + color + " -K -O " + "-Xa" + X + " -Ya" + Y + " >> $outputps << END\n"
				+ xpos + " " + (ypos - height / 2.) + "\n" + xpos + " " + (ypos + height / 2.) + "\nEND";
	}
	
	public static String curve(double[][] curve, double reddeg, String color, double X, double Y) {
		String gmtCurve = "gmt psxy -J -R -Wdefault," + color + " -K -O " + "-Xa" + X + " -Ya" + Y + " >> $outputps <<END\n";
		for (double[] tdeg : curve)
			gmtCurve += String.format("%.2f %.2f\n", tdeg[0] - tdeg[1] * reddeg, tdeg[1]);
		return gmtCurve + "\nEND\n";
	}
	
	public static String trace2GMT(Trace trace, String color, double X, double Y, double GCARC, double slowness, int nSample) {
		String text = "";
		double dt = 0.05;
		int n = nSample;
		
		text += "gmt pswiggle -J -R -Z1. -B "
				+ String.format("-Wdefault,%s ", color)
				+ String.format("-Xa%.2fc -Ya%.2fc ", X, Y)
				+ "-K -O >> $outputps <<END\n";
		
		double[] time = trace.getX();
		double[] amplitude = trace.getY();
		
//		double max = trace.getYVector().getMaxValue() > -trace.getYVector().getMinValue() ?
//				trace.getYVector().getMaxValue() : -trace.getYVector().getMinValue();
		
		for (int i=0; i < (int) (amplitude.length / n); i++) {
//			text += String.format("%.2f %.2f %.7f\n", time[i] - GCARC * redVel, GCARC, amplitude[i*n]);
			text += String.format("%.2f %.2f %.7f\n", time[i*n] - GCARC * slowness, GCARC, amplitude[i*n]);
		}
		
		return text += "END";
	}
	
	public static String trace2GMT(Trace trace, String color, double X, double Y, double GCARC, double slowness, int nSample, double shift) {
		String text = "";
		double dt = 0.05;
		int n = nSample;
		
		text += "gmt pswiggle -J -R -Z1. -B "
				+ String.format("-Wdefault,%s ", color)
				+ String.format("-Xa%.2fc -Ya%.2fc ", X, Y)
				+ "-K -O >> $outputps <<END\n";
		
		trace = trace.shiftX(shift);
		
		double[] time = trace.getX();
		double[] amplitude = trace.getY();
		
//		double max = trace.getYVector().getMaxValue() > -trace.getYVector().getMinValue() ?
//				trace.getYVector().getMaxValue() : -trace.getYVector().getMinValue();
		
		for (int i=0; i < (int) (amplitude.length / n); i++) {
//			text += String.format("%.2f %.2f %.7f\n", time[i] - GCARC * redVel, GCARC, amplitude[i*n]);
			text += String.format("%.2f %.2f %.7f\n", time[i*n] - GCARC * slowness, GCARC, amplitude[i*n]);
		}
		
		return text += "END";
	}
	
	public static String beginGMT(String psfilename, String R, String J, String[] gmtSets, String[] optionalArguments) {
		String text = "#!/bin/sh\noutputps=" + psfilename + "\n";
		
		if (gmtSets.length > 0) {
			for (String set : gmtSets) {
				text += "gmt set " + set + "\n";
			}
		}
		
		text +=	String.format("gmt pstext -R%s -J%s -K ", R, J);
		
		if (optionalArguments.length > 0) {
			for (String argument : optionalArguments) {
				text += argument + " ";
			}
		}
		
		text += "> $outputps <<END\nEND";
		
		return text;
	}
	
	public static String endGMT() {
		return "gmt pstext -R -J -O >> $outputps <<END\nEND\n"
				+ "gmt ps2raster -Tf $outputps";
	}
	
	public static void createGMTFile(Path pathgmt) throws IOException {
		Set<PosixFilePermission> perms =
		         EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE
		        		 , PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ);
		Files.deleteIfExists(pathgmt);
		Files.createFile(pathgmt, PosixFilePermissions.asFileAttribute(perms));
	}

	public static double[][] StraveltimeCurveAK135(int depth) {
		double[][] curve = null;
		switch (depth) {
		case 60:
			curve = new double[][] {{125.18,4.85},{196.20,7.74},{248.70,9.89},{288.88,11.53},{306.66,12.26},{323.31,12.94},{353.80,14.20},{381.63,15.34},{382.66,15.38},{383.69,15.43},{415.81,16.75},{442.98,17.88},{462.24,18.68},{491.49,19.89},{509.65,20.65},{527.11,21.38},{551.66,22.40},{575.07,23.39},{526.98,21.37},{508.39,20.58},{483.39,19.53},{461.67,18.62},{452.96,18.25},{451.93,18.20},{454.65,18.32},{459.29,18.52},{464.99,18.77},{471.48,19.05},{478.49,19.36},{489.07,19.84},{499.02,20.28},{501.99,20.42},{509.21,20.75},{511.46,20.85},{520.94,21.28},{530.54,21.73},{540.15,22.18},{501.89,20.38},{486.90,19.68},{466.54,18.72},{451.59,18.02},{439.43,17.45},{419.93,16.52},{404.36,15.78},{387.43,14.97},{373.33,14.29},{361.21,13.70},{405.57,15.86},{421.84,16.66},{433.63,17.24},{443.16,17.71},{456.94,18.39},{468.30,18.96},{478.16,19.45},{482.70,19.68},{487.00,19.89},{498.84,20.49},{504.37,20.77},{509.63,21.04},{519.53,21.54},{528.87,22.02},{532.57,22.22},{538.85,22.54},{539.15,22.56},{539.43,22.57},{539.98,22.60},{541.08,22.66},{542.55,22.74},{542.83,22.75},{543.11,22.77},{543.67,22.80},{544.79,22.85},{547.00,22.97},{551.36,23.20},{561.19,23.72},{571.47,24.27},{580.43,24.75},{587.38,25.13},{589.24,25.23},{591.48,25.36},{596.61,25.64},{599.29,25.79},{605.64,26.14},{606.98,26.21},{614.50,26.64},{574.19,24.37},{558.55,23.48},{546.95,22.83},{537.45,22.29},{522.05,21.41},{515.54,21.04},{509.59,20.70},{489.72,19.56},{473.93,18.64},{465.01,18.12},{464.68,18.10},{464.47,18.09},{464.26,18.07},{463.84,18.05},{463.00,18.00},{461.36,17.90},{461.05,17.89},{460.75,17.87},{460.14,17.83},{458.93,17.76},{456.56,17.62},{452.02,17.35},{447.25,17.07},{440.76,16.68},{479.91,19.03},{494.48,19.90},{513.69,21.06},{531.47,22.14},{533.82,22.28},{541.83,22.77},{549.04,23.22},{567.75,24.38},{581.97,25.27},{590.31,25.79},{624.63,27.97},{639.92,28.94},{652.06,29.72},{662.54,30.38},{681.81,31.62},{698.43,32.68},{721.57,34.18},{743.44,35.60},{765.31,37.03},{784.08,38.27},{803.38,39.55},{820.14,40.68},{837.71,41.86},{853.09,42.91},{869.31,44.02},{883.60,45.00},{898.88,46.06},{912.44,47.01},{926.40,48.00},{932.81,48.45},{939.08,48.90},{952.37,49.85},{964.70,50.73},{976.20,51.57},{976.61,51.60},{977.01,51.63},{977.80,51.68},{979.36,51.80},{980.91,51.91},{982.45,52.02},{983.54,52.10},{985.50,52.25},{988.52,52.47},{989.55,52.54},{993.15,52.81},{996.22,53.03},{1002.08,53.46},{1003.64,53.58},{1005.17,53.69},{1008.21,53.92},{1014.17,54.36},{1026.13,55.25},{1037.25,56.09},{1038.36,56.17},{1039.35,56.24},{1049.35,57.00},{1059.00,57.74},{1073.79,58.87},{1077.12,59.13},{1080.38,59.38},{1091.50,60.25},{1101.63,61.04},{1117.51,62.30},{1119.39,62.45},{1121.22,62.59},{1131.09,63.38},{1135.81,63.76},{1140.44,64.14},{1149.26,64.85},{1157.51,65.52},{1158.42,65.60},{1159.31,65.67},{1168.70,66.44},{1173.17,66.82},{1177.57,67.18},{1186.39,67.92},{1194.71,68.62},{1195.04,68.64},{1195.36,68.67},{1204.05,69.41},{1208.25,69.76},{1212.39,70.12},{1220.93,70.85},{1228.98,71.55},{1229.10,71.56},{1229.22,71.57},{1233.37,71.93},{1237.38,72.28},{1241.33,72.62},{1245.24,72.97},{1252.54,73.61},{1259.94,74.27},{1267.80,74.98},{1269.81,75.16},{1271.78,75.34},{1275.66,75.69},{1282.73,76.33},{1289.82,76.98},{1294.98,77.46},{1305.88,78.47},{1317.26,79.54},{1318.41,79.65},{1332.76,81.02},{1341.92,81.90},{1344.83,82.19},{1357.00,83.38},{1362.40,83.92},{1368.95,84.57},{1381.83,85.88},{1382.98,86.00},{1384.30,86.13},{1385.63,86.27},{1388.26,86.54},{1390.89,86.81},{1393.50,87.08},{1398.08,87.56},{1400.70,87.83},{1403.33,88.11},{1409.31,88.74},{1414.86,89.34},{1421.52,90.05},{1422.45,90.15},{1423.35,90.25},{1425.15,90.45},{1429.37,90.91},{1433.99,91.42},{1434.17,91.44},{1434.34,91.46},{1439.60,92.04},{1444.67,92.61},{1446.64,92.83},{1450.31,93.25},{1454.03,93.67},{1462.61,94.65},{1478.26,96.46},{1482.70,96.97},{1492.93,98.17},{1498.03,98.78},{1507.14,99.86}};
			break;
		case 80:
			curve = new double[][] {{162.05,6.35},{235.30,9.35},{260.38,10.38},{281.87,11.26},{318.58,12.77},{350.43,14.08},{351.59,14.13},{352.74,14.18},{387.79,15.62},{417.17,16.84},{437.79,17.70},{468.85,18.99},{487.99,19.78},{506.31,20.55},{531.96,21.62},{556.31,22.64},{508.31,20.63},{489.80,19.85},{464.97,18.80},{445.07,17.96},{437.60,17.65},{437.56,17.64},{441.09,17.80},{446.48,18.03},{452.74,18.30},{459.72,18.61},{467.17,18.94},{478.28,19.44},{488.65,19.90},{491.74,20.04},{499.21,20.38},{501.54,20.49},{511.32,20.94},{521.19,21.39},{531.05,21.85},{492.83,20.06},{477.87,19.36},{457.59,18.41},{442.70,17.71},{430.61,17.14},{411.25,16.22},{395.80,15.48},{379.03,14.68},{365.07,14.00},{353.09,13.42},{397.51,15.59},{413.82,16.39},{425.65,16.97},{435.23,17.44},{449.08,18.13},{460.52,18.70},{470.45,19.19},{475.03,19.42},{479.37,19.64},{491.31,20.24},{496.88,20.52},{502.19,20.79},{512.19,21.31},{521.62,21.79},{525.35,21.98},{531.70,22.31},{531.99,22.33},{532.28,22.35},{532.84,22.37},{533.95,22.43},{535.43,22.51},{535.71,22.52},{536.00,22.54},{536.56,22.57},{537.69,22.63},{539.93,22.75},{544.33,22.98},{554.25,23.50},{564.62,24.06},{573.66,24.55},{580.68,24.93},{582.56,25.03},{584.81,25.15},{589.99,25.44},{592.69,25.59},{599.09,25.94},{600.45,26.02},{608.03,26.44},{567.74,24.17},{552.11,23.29},{540.52,22.64},{531.04,22.10},{515.67,21.22},{509.17,20.85},{503.24,20.51},{483.42,19.37},{467.69,18.46},{458.80,17.94},{458.47,17.92},{458.26,17.91},{458.05,17.90},{457.64,17.87},{456.81,17.83},{455.17,17.73},{454.86,17.71},{454.56,17.69},{453.95,17.66},{452.75,17.59},{450.39,17.45},{445.87,17.18},{441.12,16.90},{434.66,16.51},{473.83,18.86},{488.42,19.73},{507.65,20.89},{525.47,21.98},{527.83,22.12},{535.87,22.61},{543.09,23.06},{561.85,24.22},{576.12,25.11},{584.48,25.64},{618.82,27.82},{634.11,28.79},{646.25,29.56},{656.75,30.23},{676.03,31.47},{692.66,32.53},{715.83,34.03},{737.73,35.45},{759.63,36.89},{778.42,38.13},{797.75,39.41},{814.53,40.54},{832.12,41.72},{847.53,42.77},{863.77,43.88},{878.08,44.87},{893.38,45.93},{906.97,46.88},{920.94,47.87},{927.36,48.32},{933.64,48.77},{946.95,49.72},{959.30,50.61},{970.82,51.44},{971.23,51.47},{971.63,51.50},{972.42,51.56},{973.98,51.67},{975.53,51.79},{977.07,51.90},{980.13,52.12},{983.16,52.34},{983.87,52.40},{984.18,52.42},{987.79,52.68},{990.86,52.91},{996.73,53.34},{998.29,53.46},{999.83,53.57},{1002.87,53.80},{1008.84,54.24},{1020.82,55.13},{1031.95,55.97},{1033.06,56.05},{1034.06,56.13},{1044.06,56.89},{1053.73,57.62},{1068.54,58.76},{1071.87,59.02},{1075.14,59.27},{1086.27,60.14},{1096.42,60.93},{1112.32,62.19},{1114.20,62.34},{1116.03,62.48},{1125.92,63.27},{1130.64,63.65},{1135.28,64.03},{1144.11,64.75},{1152.37,65.42},{1153.28,65.49},{1154.17,65.57},{1163.57,66.34},{1168.05,66.71},{1172.46,67.08},{1181.29,67.82},{1189.61,68.51},{1189.95,68.54},{1190.27,68.57},{1198.97,69.31},{1203.18,69.67},{1207.32,70.02},{1215.87,70.75},{1223.93,71.45},{1224.06,71.46},{1224.17,71.47},{1228.33,71.83},{1232.34,72.18},{1236.30,72.53},{1240.21,72.87},{1247.52,73.52},{1254.93,74.18},{1262.80,74.88},{1264.81,75.07},{1266.78,75.24},{1270.67,75.60},{1277.75,76.24},{1284.85,76.89},{1290.02,77.37},{1300.93,78.38},{1312.32,79.45},{1313.47,79.56},{1327.84,80.93},{1337.01,81.82},{1339.93,82.10},{1352.11,83.30},{1357.52,83.84},{1364.07,84.49},{1376.97,85.80},{1378.12,85.92},{1379.44,86.05},{1380.77,86.19},{1383.41,86.46},{1386.04,86.73},{1388.66,87.00},{1393.24,87.48},{1395.86,87.76},{1398.50,88.03},{1404.49,88.67},{1410.04,89.26},{1416.71,89.98},{1417.64,90.08},{1418.55,90.18},{1420.35,90.37},{1424.57,90.84},{1429.19,91.34},{1429.37,91.36},{1429.55,91.38},{1434.81,91.97},{1439.89,92.54},{1441.86,92.76},{1445.54,93.18},{1449.26,93.60},{1457.84,94.58},{1473.50,96.39},{1477.94,96.91},{1488.18,98.11},{1493.29,98.71},{1502.40,99.80}};
			break;
		case 100:
			curve = new double[][] {{191.18,7.55},{266.57,10.65},{309.67,12.42},{311.13,12.48},{312.57,12.54},{353.57,14.23},{386.82,15.61},{409.63,16.56},{443.38,17.96},{463.91,18.81},{483.41,19.63},{510.52,20.76},{536.08,21.83},{488.18,19.82},{469.78,19.05},{445.16,18.01},{427.51,17.27},{421.49,17.01},{422.59,17.06},{427.04,17.25},{433.28,17.52},{440.15,17.82},{447.66,18.15},{455.58,18.50},{467.27,19.02},{478.09,19.51},{481.30,19.65},{489.05,20.01},{491.45,20.12},{501.56,20.58},{511.71,21.05},{521.84,21.52},{483.65,19.73},{468.74,19.03},{448.52,18.08},{433.72,17.39},{421.69,16.82},{402.47,15.91},{387.15,15.18},{370.54,14.38},{356.74,13.71},{344.90,13.14},{389.37,15.31},{405.73,16.11},{417.61,16.70},{427.23,17.17},{441.17,17.86},{452.69,18.43},{462.69,18.93},{467.30,19.16},{471.68,19.38},{483.72,19.99},{489.35,20.27},{494.71,20.55},{504.80,21.06},{514.32,21.55},{518.09,21.75},{524.50,22.08},{524.80,22.10},{525.09,22.11},{525.65,22.14},{526.77,22.20},{528.27,22.28},{528.56,22.30},{528.84,22.31},{529.41,22.34},{530.55,22.40},{532.81,22.52},{537.25,22.75},{547.28,23.28},{557.74,23.84},{566.87,24.34},{573.95,24.72},{575.84,24.82},{578.12,24.95},{583.34,25.24},{586.06,25.39},{592.52,25.75},{593.89,25.82},{601.54,26.25},{561.26,23.98},{545.64,23.10},{534.08,22.45},{524.60,21.91},{509.27,21.04},{502.79,20.67},{496.87,20.33},{477.11,19.19},{461.43,18.28},{452.58,17.76},{452.25,17.74},{452.04,17.73},{451.83,17.72},{451.41,17.70},{450.59,17.65},{448.96,17.55},{448.65,17.53},{448.35,17.52},{447.74,17.48},{446.55,17.41},{444.20,17.27},{439.70,17.01},{434.97,16.72},{428.55,16.34},{467.73,18.69},{482.33,19.56},{501.60,20.73},{519.46,21.81},{521.82,21.95},{529.88,22.45},{537.13,22.89},{555.94,24.06},{570.26,24.96},{578.65,25.49},{612.99,27.66},{628.29,28.64},{640.44,29.41},{650.94,30.08},{670.23,31.31},{686.89,32.38},{710.08,33.88},{732.01,35.31},{753.93,36.74},{772.75,37.99},{792.11,39.27},{808.92,40.40},{826.53,41.59},{841.96,42.64},{858.22,43.75},{872.55,44.74},{887.88,45.80},{901.48,46.75},{915.48,47.74},{921.91,48.19},{928.20,48.64},{941.53,49.59},{953.90,50.48},{965.43,51.32},{965.84,51.35},{966.24,51.38},{967.03,51.44},{968.59,51.55},{970.15,51.66},{971.69,51.78},{974.75,52.00},{977.79,52.22},{978.81,52.30},{982.42,52.56},{985.33,52.78},{985.50,52.79},{991.38,53.22},{992.94,53.34},{994.48,53.45},{997.52,53.67},{1003.50,54.12},{1015.50,55.01},{1026.65,55.85},{1027.76,55.93},{1028.76,56.01},{1038.78,56.77},{1048.46,57.51},{1063.29,58.64},{1066.62,58.90},{1069.89,59.16},{1081.04,60.02},{1091.20,60.82},{1107.12,62.08},{1109.01,62.23},{1110.84,62.37},{1120.74,63.17},{1125.47,63.55},{1130.12,63.92},{1138.96,64.64},{1147.23,65.31},{1148.14,65.39},{1149.04,65.46},{1158.44,66.24},{1162.93,66.61},{1167.34,66.97},{1176.19,67.71},{1184.52,68.41},{1184.85,68.44},{1185.18,68.47},{1193.89,69.21},{1198.10,69.57},{1202.25,69.92},{1210.81,70.65},{1218.88,71.35},{1219.01,71.36},{1219.12,71.37},{1223.28,71.74},{1227.30,72.09},{1231.26,72.43},{1235.18,72.78},{1242.50,73.42},{1249.92,74.08},{1257.80,74.79},{1259.81,74.97},{1261.79,75.15},{1265.68,75.50},{1272.76,76.15},{1279.87,76.80},{1285.05,77.28},{1295.97,78.29},{1307.38,79.36},{1308.53,79.47},{1322.92,80.84},{1332.10,81.73},{1335.02,82.02},{1347.22,83.22},{1352.64,83.76},{1359.20,84.41},{1372.11,85.72},{1373.26,85.84},{1374.59,85.97},{1375.91,86.11},{1378.56,86.38},{1381.19,86.65},{1383.81,86.93},{1388.40,87.40},{1391.03,87.68},{1393.67,87.96},{1399.66,88.59},{1405.22,89.18},{1411.90,89.90},{1412.83,90.01},{1413.74,90.10},{1415.54,90.30},{1419.77,90.76},{1424.40,91.27},{1424.58,91.29},{1424.76,91.31},{1430.03,91.90},{1435.11,92.47},{1437.08,92.69},{1440.77,93.10},{1444.49,93.53},{1453.08,94.51},{1468.74,96.32},{1473.18,96.84},{1483.43,98.04},{1488.54,98.64},{1497.66,99.73}};
			break;
		case 150:
			curve = new double[][] {{251.44,10.03},{275.99,11.05},{351.21,14.18},{382.46,15.48},{409.38,16.61},{444.28,18.07},{475.51,19.38},{428.10,17.38},{410.17,16.63},{386.47,15.63},{377.79,15.27},{377.00,15.23},{381.94,15.44},{389.36,15.76},{398.21,16.15},{406.94,16.53},{416.04,16.93},{425.33,17.34},{438.69,17.94},{450.78,18.48},{454.32,18.64},{462.84,19.03},{465.47,19.15},{476.46,19.65},{487.41,20.16},{498.26,20.66},{460.18,18.88},{445.37,18.18},{425.36,17.24},{410.75,16.56},{398.92,16.00},{380.07,15.10},{365.10,14.39},{348.93,13.61},{335.54,12.97},{324.09,12.41},{368.69,14.59},{385.18,15.40},{397.19,15.99},{406.94,16.47},{421.09,17.17},{432.82,17.75},{443.02,18.26},{447.73,18.50},{452.20,18.72},{464.53,19.34},{470.29,19.64},{475.78,19.91},{486.12,20.44},{495.89,20.95},{499.75,21.15},{506.32,21.49},{506.63,21.51},{506.92,21.52},{507.50,21.55},{508.65,21.61},{510.18,21.69},{510.48,21.71},{510.77,21.72},{511.36,21.75},{512.53,21.81},{514.84,21.93},{519.40,22.17},{529.67,22.72},{540.40,23.29},{549.74,23.80},{556.98,24.19},{558.92,24.30},{561.25,24.43},{566.60,24.72},{569.38,24.87},{575.98,25.24},{577.37,25.32},{585.19,25.76},{544.95,23.49},{529.38,22.61},{517.85,21.96},{508.42,21.42},{493.16,20.55},{486.72,20.19},{480.84,19.85},{461.23,18.72},{445.69,17.82},{436.93,17.31},{436.61,17.29},{436.40,17.28},{436.19,17.27},{435.78,17.24},{434.96,17.19},{433.35,17.10},{433.05,17.08},{432.75,17.06},{432.15,17.03},{430.97,16.96},{428.65,16.82},{424.20,16.56},{419.53,16.28},{413.19,15.90},{452.41,18.25},{467.06,19.13},{486.40,20.30},{504.36,21.39},{506.74,21.53},{514.86,22.03},{522.17,22.48},{541.10,23.65},{555.54,24.56},{564.01,25.09},{598.37,27.27},{613.69,28.25},{625.86,29.02},{636.38,29.69},{655.71,30.93},{672.40,32.00},{695.67,33.50},{717.66,34.93},{739.66,36.37},{758.55,37.62},{777.97,38.91},{794.84,40.04},{812.52,41.23},{828.01,42.29},{844.33,43.40},{858.71,44.40},{874.09,45.46},{887.75,46.42},{901.80,47.41},{908.25,47.87},{914.57,48.31},{927.95,49.27},{940.36,50.16},{951.93,51.00},{952.35,51.03},{952.75,51.06},{953.54,51.12},{955.11,51.23},{956.67,51.35},{958.22,51.46},{961.29,51.69},{964.34,51.91},{965.37,51.98},{968.98,52.25},{972.07,52.48},{977.98,52.91},{979.54,53.03},{981.09,53.14},{984.14,53.37},{990.14,53.81},{1002.18,54.71},{1003.45,54.81},{1013.37,55.55},{1014.48,55.63},{1015.49,55.71},{1025.54,56.47},{1035.26,57.21},{1050.14,58.35},{1053.49,58.61},{1056.77,58.87},{1067.95,59.74},{1078.15,60.54},{1094.13,61.80},{1096.01,61.95},{1097.86,62.10},{1107.79,62.89},{1112.54,63.27},{1117.20,63.65},{1126.07,64.37},{1134.37,65.05},{1135.28,65.12},{1136.18,65.20},{1145.62,65.97},{1150.12,66.35},{1154.55,66.71},{1163.42,67.45},{1171.78,68.16},{1172.11,68.18},{1172.44,68.21},{1181.18,68.95},{1185.41,69.31},{1189.57,69.67},{1198.16,70.40},{1206.25,71.10},{1206.38,71.12},{1206.49,71.13},{1210.67,71.49},{1214.70,71.84},{1218.67,72.19},{1222.60,72.53},{1229.95,73.18},{1237.40,73.85},{1245.30,74.55},{1247.31,74.74},{1249.29,74.91},{1253.20,75.27},{1260.31,75.91},{1267.44,76.57},{1272.63,77.05},{1283.59,78.06},{1295.04,79.14},{1296.19,79.25},{1310.62,80.63},{1319.83,81.52},{1322.76,81.80},{1335.00,83.01},{1340.43,83.55},{1347.02,84.21},{1359.96,85.52},{1361.12,85.64},{1362.45,85.77},{1363.78,85.91},{1366.43,86.18},{1369.07,86.45},{1371.70,86.73},{1376.31,87.21},{1378.94,87.48},{1381.59,87.76},{1387.61,88.40},{1393.18,88.99},{1399.88,89.72},{1400.82,89.82},{1401.73,89.91},{1403.54,90.11},{1407.78,90.58},{1412.42,91.09},{1412.61,91.11},{1412.78,91.13},{1418.07,91.71},{1423.16,92.28},{1425.14,92.51},{1428.84,92.93},{1432.58,93.35},{1441.17,94.34},{1456.85,96.15},{1461.31,96.66},{1471.57,97.87},{1476.69,98.47},{1485.83,99.56}};
			break;
		case 200:
			curve = new double[][] {{297.93,11.97},{375.13,15.21},{330.31,13.33},{314.74,12.67},{295.19,11.85},{313.62,12.63},{323.35,13.04},{334.81,13.53},{346.79,14.05},{359.39,14.60},{370.67,15.09},{381.86,15.58},{392.90,16.07},{408.33,16.76},{421.98,17.37},{425.92,17.55},{435.36,17.98},{438.26,18.11},{450.28,18.66},{462.15,19.21},{473.82,19.75},{435.87,17.98},{421.17,17.29},{401.40,16.36},{387.01,15.68},{375.41,15.13},{356.97,14.26},{342.40,13.56},{326.72,12.81},{313.78,12.19},{302.76,11.65},{347.51,13.84},{364.14,14.65},{376.29,15.25},{386.18,15.74},{400.57,16.45},{412.52,17.04},{422.94,17.56},{427.76,17.80},{432.34,18.03},{444.97,18.67},{450.87,18.97},{456.50,19.26},{467.12,19.80},{477.15,20.32},{481.11,20.52},{487.86,20.87},{488.17,20.89},{488.47,20.91},{489.07,20.94},{490.25,21.00},{491.82,21.08},{492.13,21.10},{492.43,21.11},{493.03,21.14},{494.23,21.21},{496.60,21.33},{501.28,21.58},{511.82,22.14},{522.82,22.72},{532.40,23.24},{539.82,23.64},{541.80,23.75},{544.19,23.88},{549.66,24.19},{552.51,24.34},{559.26,24.72},{560.69,24.80},{568.68,25.24},{528.48,22.98},{512.95,22.10},{501.47,21.45},{492.08,20.92},{476.90,20.06},{470.50,19.69},{464.66,19.36},{445.20,18.24},{429.81,17.35},{421.15,16.84},{420.83,16.82},{420.63,16.81},{420.43,16.80},{420.02,16.77},{419.21,16.73},{417.62,16.63},{417.32,16.62},{417.02,16.60},{416.43,16.56},{415.26,16.49},{412.97,16.36},{408.58,16.10},{403.97,15.83},{397.72,15.45},{436.99,17.80},{451.67,18.69},{471.10,19.86},{489.17,20.96},{491.57,21.10},{499.75,21.60},{507.12,22.06},{526.18,23.24},{540.75,24.15},{549.29,24.69},{583.67,26.87},{599.01,27.84},{611.20,28.62},{621.73,29.29},{641.11,30.53},{657.83,31.60},{681.19,33.11},{703.25,34.55},{725.33,35.99},{744.28,37.24},{763.77,38.54},{780.71,39.68},{798.45,40.87},{814.00,41.93},{830.38,43.05},{844.82,44.05},{860.26,45.12},{873.97,46.08},{888.07,47.07},{894.55,47.53},{900.89,47.98},{914.32,48.94},{926.79,49.84},{938.40,50.68},{938.82,50.71},{939.22,50.74},{940.01,50.80},{941.59,50.91},{943.15,51.03},{944.71,51.14},{947.79,51.36},{950.85,51.59},{951.89,51.66},{955.51,51.93},{958.62,52.16},{964.54,52.59},{966.11,52.71},{967.66,52.82},{970.73,53.05},{976.75,53.50},{988.83,54.40},{1000.06,55.24},{1001.18,55.33},{1002.19,55.40},{1012.28,56.17},{1022.03,56.91},{1029.59,57.49},{1036.96,58.06},{1040.33,58.32},{1043.62,58.57},{1054.84,59.45},{1065.07,60.25},{1081.11,61.51},{1083.00,61.67},{1084.85,61.81},{1094.82,62.61},{1099.58,62.99},{1104.26,63.37},{1113.16,64.09},{1121.49,64.77},{1122.41,64.85},{1123.31,64.92},{1132.78,65.70},{1137.30,66.08},{1141.74,66.44},{1150.64,67.19},{1159.03,67.89},{1159.36,67.92},{1159.69,67.95},{1168.45,68.69},{1172.70,69.05},{1176.88,69.41},{1185.49,70.15},{1193.61,70.85},{1193.74,70.86},{1193.85,70.87},{1198.04,71.24},{1202.08,71.59},{1206.07,71.94},{1210.02,72.28},{1217.39,72.94},{1224.86,73.60},{1232.79,74.31},{1234.81,74.49},{1236.80,74.67},{1240.72,75.03},{1247.85,75.68},{1255.01,76.33},{1260.21,76.81},{1271.19,77.83},{1282.70,78.91},{1283.85,79.02},{1298.32,80.40},{1307.56,81.30},{1310.50,81.58},{1322.78,82.79},{1328.23,83.33},{1334.84,84.00},{1347.82,85.31},{1348.98,85.43},{1350.32,85.57},{1351.65,85.70},{1354.31,85.98},{1356.96,86.25},{1359.60,86.52},{1364.22,87.01},{1366.87,87.28},{1369.52,87.56},{1375.55,88.20},{1381.15,88.80},{1387.87,89.52},{1388.81,89.62},{1389.72,89.72},{1391.54,89.92},{1395.80,90.39},{1400.45,90.90},{1400.64,90.92},{1400.81,90.94},{1406.12,91.53},{1411.23,92.10},{1413.22,92.32},{1416.93,92.74},{1420.68,93.17},{1429.27,94.16},{1444.98,95.97},{1449.44,96.49},{1459.72,97.69},{1464.85,98.30},{1474.01,99.39}};
			break;
		case 250:
			curve = new double[][] {{242.21,9.63},{284.88,11.46},{308.99,12.51},{326.01,13.25},{341.22,13.92},{355.32,14.54},{374.08,15.38},{390.03,16.10},{394.56,16.30},{405.30,16.79},{408.55,16.94},{421.99,17.56},{435.07,18.16},{447.80,18.75},{410.00,16.98},{395.46,16.30},{375.98,15.39},{361.88,14.72},{350.55,14.19},{332.63,13.34},{318.54,12.67},{303.46,11.94},{291.08,11.34},{280.57,10.83},{325.48,13.03},{342.29,13.85},{354.60,14.46},{364.65,14.95},{379.32,15.68},{391.54,16.28},{402.22,16.82},{407.16,17.06},{411.85,17.30},{424.83,17.95},{430.90,18.26},{436.70,18.56},{447.63,19.12},{457.95,19.65},{462.02,19.86},{468.97,20.22},{469.30,20.24},{469.60,20.25},{470.22,20.29},{471.43,20.35},{473.05,20.43},{473.36,20.45},{473.67,20.47},{474.29,20.50},{475.53,20.56},{477.97,20.69},{482.78,20.95},{493.63,21.52},{504.93,22.13},{514.77,22.66},{522.38,23.07},{524.42,23.18},{526.86,23.32},{532.47,23.62},{535.39,23.79},{542.31,24.17},{543.77,24.25},{551.96,24.71},{511.81,22.45},{496.32,21.57},{484.89,20.93},{475.54,20.40},{460.45,19.54},{454.09,19.18},{448.30,18.84},{429.02,17.74},{413.79,16.85},{405.23,16.35},{404.92,16.33},{404.72,16.32},{404.51,16.31},{404.11,16.29},{403.31,16.24},{401.74,16.15},{401.45,16.13},{401.15,16.11},{400.57,16.08},{399.42,16.01},{397.16,15.88},{392.83,15.62},{388.28,15.35},{382.13,14.98},{421.44,17.34},{436.17,18.22},{455.69,19.40},{473.88,20.50},{476.29,20.65},{484.53,21.16},{491.97,21.61},{511.17,22.80},{525.88,23.72},{534.51,24.27},{568.91,26.45},{584.27,27.43},{596.47,28.20},{607.03,28.88},{626.44,30.12},{643.21,31.20},{666.65,32.71},{688.79,34.15},{710.95,35.60},{729.97,36.86},{749.53,38.16},{766.54,39.30},{784.35,40.50},{799.96,41.56},{816.41,42.68},{830.91,43.68},{846.41,44.76},{860.18,45.72},{874.34,46.72},{880.85,47.18},{887.21,47.64},{900.69,48.60},{913.21,49.50},{924.87,50.34},{925.28,50.37},{925.69,50.40},{926.49,50.46},{928.07,50.58},{929.64,50.69},{931.20,50.81},{934.30,51.03},{937.37,51.26},{938.41,51.33},{942.05,51.60},{945.16,51.83},{951.11,52.27},{952.68,52.38},{954.24,52.50},{957.32,52.73},{963.37,53.17},{975.49,54.08},{986.77,54.93},{987.89,55.01},{988.90,55.09},{999.03,55.85},{1008.82,56.60},{1023.81,57.75},{1027.18,58.01},{1030.49,58.27},{1041.75,59.14},{1052.02,59.95},{1059.97,60.57},{1068.11,61.22},{1070.02,61.37},{1071.87,61.52},{1081.88,62.32},{1086.66,62.71},{1091.35,63.08},{1100.28,63.81},{1108.64,64.49},{1109.56,64.57},{1110.47,64.64},{1119.97,65.42},{1124.50,65.80},{1128.96,66.17},{1137.89,66.91},{1146.31,67.62},{1146.65,67.65},{1146.97,67.68},{1155.77,68.42},{1160.03,68.78},{1164.22,69.14},{1172.86,69.88},{1181.02,70.59},{1181.14,70.60},{1181.26,70.61},{1185.46,70.98},{1189.52,71.33},{1193.52,71.68},{1197.48,72.03},{1204.87,72.68},{1212.37,73.35},{1220.32,74.06},{1222.35,74.25},{1224.35,74.43},{1228.28,74.78},{1235.44,75.43},{1242.62,76.09},{1247.83,76.57},{1258.86,77.59},{1270.40,78.68},{1271.56,78.79},{1286.07,80.17},{1295.35,81.07},{1298.30,81.36},{1310.62,82.57},{1316.09,83.12},{1322.72,83.78},{1335.74,85.10},{1336.90,85.22},{1338.24,85.36},{1339.58,85.49},{1342.25,85.77},{1344.91,86.04},{1347.55,86.32},{1352.20,86.80},{1354.85,87.08},{1357.52,87.36},{1363.57,88.00},{1369.18,88.60},{1375.93,89.32},{1376.86,89.43},{1377.78,89.52},{1379.60,89.72},{1383.88,90.19},{1388.55,90.70},{1388.74,90.73},{1388.91,90.74},{1394.23,91.34},{1399.36,91.91},{1401.36,92.13},{1405.08,92.56},{1408.84,92.99},{1417.44,93.97},{1433.17,95.78},{1437.64,96.30},{1447.94,97.51},{1453.08,98.12},{1462.26,99.21}};
			break;
		case 300:
			curve = new double[][] {{250.77,10.00},{294.55,11.94},{326.66,13.37},{348.87,14.37},{354.75,14.63},{368.32,15.25},{372.31,15.43},{388.46,16.17},{403.70,16.88},{418.20,17.55},{380.65,15.79},{366.34,15.12},{347.33,14.23},{333.68,13.59},{322.77,13.07},{305.64,12.26},{292.27,11.62},{278.06,10.94},{266.45,10.38},{256.64,9.90},{301.80,12.11},{318.84,12.94},{331.38,13.56},{341.64,14.07},{356.68,14.81},{369.25,15.43},{380.26,15.98},{385.36,16.24},{390.21,16.48},{403.64,17.16},{409.92,17.48},{415.92,17.78},{427.25,18.36},{437.93,18.91},{442.15,19.13},{449.35,19.51},{449.68,19.52},{450.00,19.54},{450.63,19.57},{451.89,19.64},{453.56,19.73},{453.89,19.74},{454.21,19.76},{454.85,19.80},{456.13,19.86},{458.65,19.99},{463.63,20.26},{474.84,20.85},{486.51,21.47},{496.66,22.02},{504.50,22.45},{506.60,22.56},{509.11,22.70},{514.89,23.02},{517.89,23.19},{525.01,23.58},{526.51,23.66},{534.92,24.14},{494.82,21.88},{479.39,21.01},{468.00,20.36},{458.71,19.83},{443.73,18.98},{437.42,18.62},{431.67,18.29},{412.59,17.20},{397.55,16.32},{389.11,15.83},{388.80,15.81},{388.60,15.80},{388.40,15.79},{388.01,15.77},{387.22,15.72},{385.67,15.63},{385.38,15.61},{385.09,15.59},{384.52,15.56},{383.38,15.49},{381.16,15.36},{376.89,15.11},{372.43,14.85},{366.37,14.48},{405.74,16.84},{420.52,17.73},{440.15,18.91},{458.46,20.02},{460.89,20.17},{469.21,20.68},{476.72,21.14},{496.08,22.34},{510.93,23.27},{519.66,23.82},{554.08,26.01},{569.46,26.99},{581.69,27.76},{592.27,28.44},{611.73,29.68},{628.55,30.76},{652.08,32.28},{674.31,33.73},{696.55,35.18},{715.65,36.45},{735.29,37.75},{752.38,38.90},{770.26,40.10},{785.94,41.17},{802.46,42.30},{817.03,43.30},{832.59,44.38},{846.42,45.35},{860.64,46.35},{867.18,46.82},{873.57,47.27},{887.11,48.24},{899.69,49.14},{911.39,49.99},{911.81,50.02},{912.22,50.05},{913.02,50.11},{914.61,50.23},{916.19,50.34},{917.76,50.46},{920.87,50.68},{923.95,50.91},{924.99,50.99},{928.65,51.25},{931.77,51.48},{937.74,51.92},{939.32,52.04},{940.89,52.16},{943.98,52.38},{950.05,52.83},{962.23,53.74},{973.55,54.59},{974.67,54.68},{975.69,54.75},{985.86,55.52},{995.69,56.27},{1010.74,57.43},{1014.13,57.69},{1017.45,57.95},{1028.75,58.83},{1039.06,59.64},{1055.22,60.91},{1057.13,61.07},{1058.99,61.21},{1069.03,62.02},{1073.83,62.40},{1078.55,62.78},{1087.51,63.51},{1088.22,63.57},{1095.91,64.20},{1096.83,64.27},{1097.73,64.35},{1107.27,65.13},{1111.82,65.51},{1116.30,65.88},{1125.26,66.63},{1133.71,67.34},{1134.05,67.37},{1134.38,67.39},{1143.21,68.14},{1147.48,68.51},{1151.69,68.86},{1160.36,69.61},{1168.54,70.32},{1168.67,70.33},{1168.79,70.34},{1173.00,70.70},{1177.07,71.06},{1181.09,71.41},{1185.06,71.76},{1192.49,72.42},{1200.02,73.09},{1207.99,73.80},{1210.03,73.99},{1212.03,74.17},{1215.98,74.52},{1223.16,75.18},{1230.37,75.84},{1235.60,76.32},{1246.66,77.35},{1258.26,78.44},{1259.41,78.55},{1273.97,79.94},{1283.28,80.84},{1286.24,81.13},{1298.61,82.34},{1304.10,82.89},{1310.75,83.55},{1323.82,84.88},{1324.98,85.00},{1326.33,85.13},{1327.67,85.27},{1330.35,85.55},{1333.01,85.82},{1335.67,86.10},{1340.33,86.58},{1342.99,86.86},{1345.67,87.14},{1351.74,87.79},{1357.37,88.39},{1364.14,89.12},{1365.08,89.22},{1366.00,89.32},{1367.83,89.52},{1372.13,89.99},{1376.81,90.50},{1377.00,90.52},{1377.18,90.54},{1382.52,91.14},{1387.66,91.71},{1389.66,91.94},{1393.40,92.36},{1397.18,92.79},{1405.78,93.78},{1421.53,95.59},{1426.01,96.12},{1436.33,97.33},{1441.48,97.94},{1450.68,99.03}};
			break;
		case 350:
			curve = new double[][] {{264.82,10.63},{308.37,12.61},{316.11,12.96},{342.26,14.16},{363.28,15.13},{381.72,15.99},{344.69,14.26},{330.88,13.61},{312.79,12.76},{299.98,12.15},{289.86,11.68},{274.14,10.93},{262.01,10.35},{249.22,9.74},{238.84,9.24},{230.11,8.81},{275.62,11.04},{293.00,11.89},{305.87,12.52},{316.45,13.04},{332.01,13.81},{345.07,14.46},{356.55,15.03},{361.87,15.30},{366.94,15.56},{380.97,16.26},{387.54,16.60},{393.81,16.92},{405.65,17.52},{416.81,18.10},{421.22,18.32},{428.72,18.72},{429.07,18.73},{429.40,18.75},{430.06,18.79},{431.37,18.85},{433.12,18.94},{433.46,18.96},{433.79,18.98},{434.46,19.02},{435.79,19.08},{438.42,19.22},{443.61,19.50},{455.27,20.11},{467.39,20.76},{477.91,21.33},{486.04,21.77},{488.20,21.89},{490.81,22.03},{496.78,22.36},{499.88,22.53},{507.23,22.94},{508.78,23.03},{517.45,23.51},{477.42,21.26},{462.05,20.39},{450.73,19.75},{441.50,19.23},{426.64,18.38},{420.39,18.03},{414.70,17.70},{395.84,16.62},{381.02,15.76},{372.72,15.27},{372.41,15.25},{372.22,15.24},{372.02,15.23},{371.63,15.21},{370.86,15.16},{369.34,15.07},{369.05,15.06},{368.77,15.04},{368.20,15.01},{367.09,14.94},{364.90,14.81},{360.72,14.56},{356.34,14.30},{350.41,13.95},{389.83,16.31},{404.67,17.20},{424.41,18.39},{442.87,19.51},{445.33,19.66},{453.73,20.18},{461.32,20.64},{480.85,21.85},{495.88,22.79},{504.71,23.35},{539.16,25.54},{554.57,26.52},{566.82,27.30},{577.42,27.97},{596.93,29.22},{613.80,30.30},{637.43,31.83},{659.76,33.28},{682.10,34.74},{701.29,36.01},{721.02,37.32},{738.19,38.47},{756.15,39.69},{771.91,40.76},{788.51,41.89},{803.15,42.90},{818.78,43.99},{832.68,44.96},{846.97,45.97},{853.54,46.43},{859.96,46.89},{873.57,47.86},{886.20,48.77},{897.96,49.62},{898.38,49.65},{898.78,49.68},{899.59,49.74},{901.19,49.86},{902.77,49.97},{904.35,50.09},{907.47,50.32},{910.57,50.54},{911.62,50.62},{915.29,50.89},{918.43,51.12},{924.42,51.56},{926.01,51.68},{927.58,51.79},{930.69,52.02},{936.79,52.48},{949.02,53.39},{960.39,54.24},{961.52,54.33},{962.54,54.41},{972.76,55.18},{982.63,55.93},{997.74,57.09},{1001.15,57.36},{1004.48,57.61},{1015.83,58.50},{1026.18,59.31},{1042.41,60.59},{1044.33,60.74},{1046.20,60.89},{1056.28,61.70},{1061.10,62.09},{1065.84,62.47},{1074.84,63.20},{1083.26,63.89},{1084.19,63.96},{1085.10,64.04},{1094.67,64.83},{1099.25,65.20},{1103.74,65.58},{1112.73,66.33},{1114.09,66.44},{1121.22,67.04},{1121.56,67.07},{1121.89,67.10},{1130.75,67.85},{1135.04,68.21},{1139.27,68.57},{1147.97,69.32},{1156.19,70.03},{1156.31,70.04},{1156.43,70.05},{1160.66,70.42},{1164.75,70.78},{1168.78,71.13},{1172.77,71.48},{1180.22,72.14},{1187.78,72.81},{1195.79,73.53},{1197.83,73.72},{1199.84,73.90},{1203.80,74.26},{1211.02,74.91},{1218.25,75.57},{1223.49,76.06},{1234.59,77.09},{1246.24,78.18},{1247.40,78.29},{1262.00,79.69},{1271.36,80.59},{1274.33,80.88},{1286.74,82.10},{1292.25,82.65},{1298.93,83.32},{1312.04,84.65},{1313.21,84.77},{1314.56,84.90},{1315.91,85.04},{1318.59,85.32},{1321.27,85.60},{1323.93,85.87},{1328.62,86.36},{1331.29,86.64},{1333.98,86.92},{1340.07,87.57},{1345.71,88.17},{1352.51,88.90},{1353.46,89.00},{1354.38,89.10},{1356.22,89.30},{1360.53,89.78},{1365.23,90.29},{1365.42,90.31},{1365.60,90.33},{1370.96,90.93},{1376.12,91.51},{1378.13,91.73},{1381.88,92.16},{1385.67,92.59},{1394.28,93.58},{1410.06,95.40},{1414.54,95.92},{1424.89,97.13},{1430.05,97.74},{1439.26,98.84}};
			break;
		case 400:
			curve = new double[][] {{279.61,11.31},{322.58,13.31},{288.21,11.70},{276.67,11.16},{262.29,10.49},{252.44,10.02},{244.75,9.66},{232.89,9.10},{223.72,8.66},{213.97,8.19},{205.98,7.81},{199.18,7.47},{245.31,9.73},{263.27,10.61},{276.67,11.27},{287.77,11.81},{304.17,12.63},{318.01,13.31},{330.19,13.92},{335.84,14.20},{341.23,14.47},{356.14,15.23},{363.12,15.58},{369.78,15.92},{382.33,16.56},{394.15,17.17},{398.80,17.41},{406.72,17.82},{407.09,17.84},{407.44,17.86},{408.13,17.90},{409.51,17.97},{411.35,18.07},{411.71,18.08},{412.06,18.10},{412.77,18.14},{414.17,18.21},{416.94,18.36},{422.40,18.65},{434.65,19.30},{447.34,19.97},{458.33,20.57},{466.80,21.03},{469.06,21.15},{471.77,21.30},{477.98,21.64},{481.20,21.82},{488.84,22.24},{490.44,22.33},{499.44,22.84},{459.48,20.59},{444.19,19.72},{432.94,19.09},{423.78,18.57},{409.07,17.73},{402.89,17.38},{397.27,17.06},{378.68,15.99},{364.11,15.14},{355.97,14.67},{355.67,14.65},{355.48,14.64},{355.29,14.63},{354.90,14.60},{354.15,14.56},{352.66,14.47},{352.38,14.46},{352.10,14.44},{351.55,14.41},{350.45,14.34},{348.32,14.22},{344.23,13.98},{339.94,13.72},{334.15,13.38},{373.65,15.74},{388.56,16.63},{408.43,17.83},{427.06,18.97},{429.54,19.12},{438.03,19.64},{445.71,20.11},{465.45,21.33},{480.67,22.28},{489.62,22.85},{524.10,25.04},{539.53,26.02},{551.81,26.80},{562.44,27.48},{582.01,28.73},{598.94,29.82},{622.69,31.35},{645.12,32.81},{667.56,34.28},{686.86,35.55},{706.68,36.87},{723.94,38.03},{742.00,39.24},{757.84,40.32},{774.52,41.46},{789.24,42.48},{804.95,43.57},{818.93,44.55},{833.29,45.56},{839.89,46.03},{846.35,46.49},{860.02,47.46},{872.72,48.38},{884.54,49.23},{884.96,49.26},{885.37,49.29},{886.18,49.35},{887.78,49.47},{889.38,49.59},{890.96,49.70},{894.11,49.93},{897.22,50.16},{898.28,50.24},{901.95,50.51},{905.11,50.74},{911.13,51.18},{912.73,51.30},{914.31,51.42},{917.43,51.65},{923.56,52.10},{935.84,53.02},{947.27,53.88},{948.41,53.96},{949.43,54.04},{959.70,54.82},{969.62,55.57},{984.80,56.74},{988.22,57.00},{991.58,57.26},{1002.97,58.15},{1013.37,58.96},{1029.67,60.25},{1031.60,60.41},{1033.48,60.56},{1043.60,61.37},{1048.45,61.76},{1053.20,62.14},{1062.24,62.87},{1070.70,63.56},{1071.63,63.64},{1072.55,63.71},{1082.16,64.51},{1086.75,64.89},{1091.26,65.26},{1100.30,66.01},{1108.82,66.73},{1109.16,66.76},{1109.49,66.79},{1118.39,67.54},{1122.70,67.91},{1126.94,68.27},{1135.68,69.02},{1137.30,69.16},{1143.93,69.73},{1144.05,69.74},{1144.17,69.76},{1148.42,70.12},{1152.53,70.48},{1156.57,70.84},{1160.58,71.19},{1168.07,71.85},{1175.66,72.53},{1183.69,73.25},{1185.75,73.43},{1187.76,73.61},{1191.74,73.97},{1198.98,74.63},{1206.25,75.30},{1211.51,75.78},{1222.65,76.82},{1234.35,77.92},{1235.51,78.03},{1250.17,79.43},{1259.56,80.34},{1262.54,80.63},{1275.01,81.85},{1280.54,82.40},{1287.24,83.07},{1300.40,84.41},{1301.57,84.53},{1302.93,84.67},{1304.28,84.80},{1306.97,85.08},{1309.66,85.36},{1312.34,85.64},{1317.04,86.13},{1319.73,86.41},{1322.43,86.69},{1328.53,87.34},{1334.20,87.94},{1341.03,88.68},{1341.98,88.78},{1342.90,88.88},{1344.75,89.08},{1349.08,89.55},{1353.80,90.07},{1353.99,90.10},{1354.17,90.12},{1359.54,90.71},{1364.73,91.29},{1366.75,91.52},{1370.51,91.95},{1374.32,92.38},{1382.93,93.37},{1398.74,95.19},{1403.23,95.71},{1413.60,96.93},{1418.77,97.54},{1428.01,98.64}};
			break;
		case 450:
			curve = new double[][] {{249.21,9.97},{265.76,10.79},{280.16,11.52},{290.55,12.04},{314.05,13.22},{323.75,13.71},{332.61,14.16},{348.53,14.98},{362.87,15.72},{368.38,16.00},{377.65,16.49},{378.07,16.51},{378.48,16.53},{379.28,16.57},{380.87,16.65},{382.99,16.77},{383.40,16.79},{383.81,16.81},{384.62,16.85},{386.22,16.93},{389.39,17.10},{395.58,17.43},{409.32,18.15},{423.35,18.90},{435.35,19.55},{444.54,20.05},{446.97,20.19},{449.89,20.35},{456.57,20.71},{460.02,20.90},{468.18,21.36},{469.88,21.45},{479.45,21.99},{439.62,19.74},{424.46,18.89},{413.34,18.26},{404.31,17.75},{389.83,16.92},{383.77,16.58},{378.27,16.26},{360.12,15.22},{345.95,14.40},{338.06,13.94},{337.77,13.92},{337.58,13.91},{337.40,13.90},{337.03,13.88},{336.30,13.83},{334.86,13.75},{334.59,13.73},{334.32,13.72},{333.78,13.69},{332.72,13.62},{330.66,13.50},{326.71,13.27},{322.57,13.02},{316.99,12.69},{356.59,15.06},{371.60,15.96},{391.66,17.17},{410.54,18.32},{413.06,18.47},{421.69,19.00},{429.50,19.48},{449.52,20.72},{465.01,21.69},{474.12,22.27},{508.63,24.45},{524.11,25.44},{536.43,26.22},{547.10,26.90},{566.75,28.16},{583.75,29.25},{607.65,30.79},{630.23,32.26},{652.81,33.74},{672.23,35.02},{692.19,36.35},{709.57,37.52},{727.74,38.74},{743.70,39.83},{760.48,40.97},{775.30,42.00},{791.11,43.09},{805.18,44.08},{819.63,45.10},{826.28,45.57},{832.79,46.03},{846.54,47.01},{859.33,47.93},{871.21,48.80},{871.64,48.83},{872.05,48.86},{872.86,48.92},{874.48,49.03},{876.08,49.15},{877.68,49.27},{880.84,49.50},{883.97,49.73},{885.03,49.80},{888.73,50.08},{891.90,50.31},{897.96,50.75},{899.57,50.87},{901.16,50.99},{904.30,51.22},{910.47,51.68},{922.82,52.60},{934.31,53.47},{935.46,53.55},{936.48,53.63},{946.81,54.41},{956.79,55.17},{972.06,56.34},{975.50,56.61},{978.87,56.87},{990.32,57.76},{1000.78,58.58},{1017.17,59.88},{1019.10,60.03},{1020.99,60.18},{1031.17,61.00},{1036.04,61.39},{1040.82,61.77},{1049.90,62.51},{1058.41,63.21},{1059.34,63.28},{1060.26,63.36},{1069.92,64.15},{1074.54,64.53},{1079.07,64.91},{1088.15,65.67},{1096.71,66.39},{1097.06,66.42},{1097.39,66.44},{1106.33,67.20},{1110.66,67.57},{1114.93,67.93},{1123.70,68.69},{1131.99,69.41},{1132.12,69.42},{1132.24,69.43},{1136.51,69.80},{1140.63,70.16},{1144.70,70.51},{1148.72,70.87},{1156.25,71.53},{1163.88,72.21},{1171.94,72.94},{1174.00,73.12},{1176.03,73.30},{1180.03,73.67},{1187.30,74.33},{1193.45,74.89},{1194.60,75.00},{1199.88,75.48},{1211.07,76.52},{1222.84,77.63},{1224.00,77.74},{1238.71,79.14},{1248.15,80.06},{1251.14,80.35},{1263.67,81.58},{1269.22,82.13},{1275.95,82.80},{1289.16,84.14},{1290.34,84.26},{1291.70,84.40},{1293.06,84.54},{1295.77,84.82},{1298.47,85.10},{1301.15,85.38},{1305.88,85.87},{1308.58,86.15},{1311.29,86.44},{1317.42,87.09},{1323.12,87.70},{1329.97,88.43},{1330.93,88.54},{1331.86,88.64},{1333.71,88.84},{1338.06,89.31},{1342.80,89.84},{1342.99,89.86},{1343.17,89.88},{1348.57,90.48},{1353.77,91.06},{1355.80,91.29},{1359.58,91.72},{1363.41,92.15},{1372.03,93.14},{1387.86,94.97},{1392.37,95.49},{1402.76,96.71},{1407.94,97.32},{1417.20,98.43}};
			break;
		case 500:
			curve = new double[][] {{271.66,11.10},{310.59,13.11},{320.64,13.63},{335.38,14.40},{336.00,14.43},{336.60,14.46},{337.78,14.52},{340.09,14.64},{343.12,14.80},{343.70,14.83},{344.27,14.86},{345.40,14.92},{347.63,15.04},{351.95,15.26},{360.14,15.70},{377.47,16.61},{394.30,17.51},{408.24,18.27},{418.70,18.84},{421.43,18.99},{424.72,19.17},{432.18,19.58},{436.01,19.79},{445.01,20.29},{446.88,20.39},{457.32,20.98},{417.70,18.75},{402.74,17.90},{391.81,17.28},{382.97,16.78},{368.85,15.98},{362.96,15.64},{357.62,15.34},{340.09,14.33},{326.50,13.54},{318.95,13.10},{318.68,13.08},{318.50,13.07},{318.32,13.06},{317.97,13.04},{317.27,13.00},{315.89,12.92},{315.64,12.90},{315.38,12.89},{314.87,12.86},{313.86,12.80},{311.89,12.68},{308.13,12.46},{304.20,12.23},{298.90,11.91},{338.63,14.29},{353.77,15.20},{374.09,16.42},{393.29,17.59},{395.85,17.75},{404.66,18.29},{412.63,18.78},{433.02,20.04},{448.84,21.03},{458.16,21.62},{492.72,23.81},{508.24,24.80},{520.60,25.58},{531.32,26.27},{551.07,27.53},{568.17,28.63},{592.26,30.18},{615.01,31.66},{637.76,33.15},{657.34,34.45},{677.45,35.78},{694.97,36.96},{713.28,38.19},{729.37,39.29},{746.28,40.44},{761.22,41.47},{777.15,42.58},{791.32,43.57},{805.89,44.60},{812.59,45.07},{819.14,45.54},{833.00,46.53},{845.87,47.46},{857.84,48.32},{858.27,48.35},{858.68,48.38},{859.50,48.44},{861.13,48.56},{862.74,48.68},{864.35,48.80},{867.53,49.03},{870.69,49.26},{871.76,49.34},{875.48,49.61},{878.67,49.85},{884.77,50.29},{886.39,50.41},{887.99,50.53},{891.15,50.77},{897.36,51.23},{909.79,52.15},{921.36,53.02},{922.51,53.11},{923.54,53.19},{933.94,53.98},{943.99,54.74},{959.35,55.92},{962.81,56.19},{966.20,56.45},{977.71,57.35},{988.24,58.17},{1004.73,59.47},{1006.67,59.63},{1008.58,59.78},{1018.81,60.60},{1023.71,60.99},{1028.52,61.38},{1037.65,62.12},{1046.21,62.82},{1047.15,62.90},{1048.07,62.97},{1057.78,63.77},{1062.43,64.16},{1066.98,64.54},{1076.11,65.30},{1084.72,66.02},{1085.06,66.05},{1085.40,66.08},{1094.39,66.84},{1098.74,67.21},{1103.03,67.58},{1111.85,68.34},{1120.18,69.06},{1120.31,69.07},{1120.43,69.08},{1124.72,69.45},{1128.86,69.81},{1132.95,70.17},{1137.00,70.53},{1144.57,71.19},{1152.24,71.88},{1160.34,72.60},{1162.41,72.79},{1164.45,72.97},{1168.47,73.34},{1175.78,74.00},{1183.12,74.68},{1188.42,75.16},{1199.65,76.21},{1207.58,76.95},{1211.49,77.32},{1212.66,77.43},{1227.43,78.84},{1236.92,79.76},{1239.93,80.05},{1252.51,81.29},{1258.09,81.84},{1264.86,82.52},{1278.12,83.87},{1279.31,83.99},{1280.68,84.13},{1282.04,84.27},{1284.76,84.55},{1287.47,84.83},{1290.17,85.11},{1294.93,85.60},{1297.64,85.89},{1300.36,86.17},{1306.52,86.82},{1312.24,87.43},{1319.13,88.18},{1320.08,88.28},{1321.02,88.38},{1322.88,88.58},{1327.25,89.06},{1332.02,89.59},{1332.21,89.61},{1332.39,89.63},{1337.81,90.23},{1343.04,90.81},{1345.08,91.04},{1348.87,91.47},{1352.71,91.91},{1361.35,92.90},{1377.21,94.73},{1381.73,95.26},{1392.15,96.48},{1397.34,97.09},{1406.63,98.20}};
			break;
		default:
			break;
		}
		return curve;
	}
	
	public static double[][] S2traveltimeCurveAK135(int depth) {
		double[][] curve = null;
		switch (depth) {
		case 60:
			curve = new double[][] {{375.53,14.54},{474.55,18.58},{564.63,22.26},{637.42,25.24},{670.29,26.58},{701.37,27.86},{758.80,30.22},{811.72,32.40},{813.70,32.48},{815.66,32.56},{877.66,35.12},{930.15,37.29},{967.47,38.84},{1024.30,41.20},{1059.66,42.68},{1093.72,44.10},{1141.69,46.11},{1187.52,48.03},{1091.25,43.98},{1053.97,42.42},{1003.80,40.31},{958.30,38.39},{939.42,37.58},{936.15,37.44},{940.55,37.63},{948.84,37.99},{959.48,38.46},{971.78,39.00},{985.20,39.59},{1005.57,40.50},{1024.83,41.37},{1030.61,41.63},{1044.64,42.27},{1049.03,42.47},{1067.52,43.31},{1086.27,44.18},{1105.09,45.06},{1028.51,41.47},{998.46,40.06},{957.63,38.14},{927.61,36.73},{903.17,35.58},{863.96,33.72},{832.60,32.22},{798.47,30.58},{770.00,29.21},{745.53,28.02},{834.17,32.35},{866.62,33.94},{890.11,35.10},{909.10,36.03},{936.50,37.39},{959.10,38.51},{978.67,39.49},{987.68,39.94},{996.23,40.37},{1019.72,41.55},{1030.68,42.11},{1041.11,42.64},{1060.74,43.64},{1079.25,44.60},{1086.57,44.98},{1099.03,45.62},{1099.61,45.65},{1100.17,45.68},{1101.27,45.74},{1103.44,45.85},{1106.35,46.01},{1106.91,46.04},{1107.47,46.06},{1108.58,46.12},{1110.80,46.24},{1115.18,46.47},{1123.82,46.92},{1143.30,47.96},{1163.65,49.04},{1181.41,50.00},{1195.19,50.75},{1198.88,50.95},{1203.32,51.20},{1213.49,51.76},{1218.79,52.05},{1231.38,52.75},{1234.04,52.90},{1248.96,53.73},{1168.30,49.19},{1136.98,47.42},{1113.76,46.11},{1094.72,45.03},{1063.87,43.27},{1050.82,42.53},{1038.89,41.85},{999.03,39.55},{967.33,37.72},{949.41,36.67},{948.75,36.63},{948.33,36.60},{947.91,36.58},{947.06,36.53},{945.39,36.43},{942.09,36.24},{941.47,36.20},{940.85,36.17},{939.63,36.09},{937.20,35.95},{932.45,35.67},{923.32,35.13},{913.72,34.56},{900.68,33.78},{978.95,38.47},{1008.06,40.22},{1046.41,42.53},{1081.87,44.68},{1086.56,44.97},{1102.55,45.95},{1116.92,46.83},{1154.22,49.14},{1182.57,50.92},{1199.17,51.97},{1267.81,56.32},{1298.37,58.26},{1322.62,59.81},{1343.58,61.14},{1382.08,63.61},{1415.29,65.74},{1461.50,68.72},{1505.18,71.56},{1548.86,74.42},{1586.34,76.90},{1624.88,79.46},{1658.36,81.70},{1693.43,84.07},{1724.15,86.16},{1756.54,88.37},{1785.06,90.34},{1815.58,92.46},{1842.66,94.35},{1870.52,96.32},{1883.33,97.22},{1895.84,98.11},{1922.38,100.01},{1947.00,101.78},{1969.97,103.45},{1970.79,103.51},{1971.58,103.56},{1973.15,103.68},{1976.26,103.91},{1979.36,104.13},{1982.43,104.36},{1984.61,104.51},{1988.52,104.80},{1994.56,105.24},{1996.61,105.39},{2003.80,105.92},{2009.93,106.37},{2021.64,107.23},{2024.75,107.46},{2027.81,107.69},{2033.87,108.14},{2045.78,109.02},{2069.67,110.80},{2091.86,112.47},{2094.07,112.64},{2096.06,112.79},{2116.01,114.30},{2135.29,115.76},{2164.82,118.03},{2171.47,118.55},{2177.98,119.05},{2200.18,120.78},{2220.41,122.36},{2252.12,124.87},{2255.87,125.17},{2259.52,125.46},{2279.24,127.04},{2288.66,127.80},{2297.91,128.54},{2315.53,129.97},{2332.00,131.32},{2333.82,131.47},{2335.59,131.61},{2354.33,133.15},{2363.28,133.89},{2372.06,134.62},{2389.67,136.09},{2406.27,137.49},{2406.94,137.54},{2407.58,137.60},{2424.93,139.07},{2433.33,139.78},{2441.59,140.49},{2458.64,141.95},{2474.72,143.34},{2474.97,143.36},{2475.20,143.38},{2483.49,144.11},{2491.48,144.80},{2499.38,145.49},{2507.18,146.18},{2521.75,147.47},{2536.54,148.78},{2552.24,150.19},{2556.24,150.55},{2560.18,150.91},{2567.94,151.61},{2582.04,152.89},{2596.21,154.19},{2606.52,155.14},{2628.29,157.16},{2651.00,159.30},{2653.30,159.52},{2681.97,162.25},{2700.25,164.02},{2706.06,164.59},{2730.37,166.98},{2741.15,168.05},{2754.22,169.36},{2779.95,171.97},{2782.24,172.20},{2784.89,172.47},{2787.53,172.74},{2792.79,173.28},{2798.04,173.83},{2803.26,174.37},{2812.39,175.32},{2817.62,175.87},{2822.88,176.42},{2834.83,177.68},{2845.90,178.86},{2859.21,179.70},{2861.06,179.50},{2862.87,179.31},{2866.46,178.91},{2874.89,177.99},{2884.10,176.98},{2884.46,176.94},{2884.81,176.90},{2895.31,175.73},{2905.43,174.60},{2909.36,174.16},{2916.70,173.33},{2924.13,172.48},{2941.28,170.52},{2972.55,166.91},{2981.42,165.88},{3001.87,163.48},{3012.07,162.27},{3030.26,160.10}};
			break;
		case 80:
			curve = new double[][] {{486.15,19.06},{583.84,23.06},{624.02,24.70},{659.92,26.18},{723.58,28.79},{780.53,31.14},{782.63,31.23},{784.71,31.31},{849.64,33.99},{904.34,36.26},{943.02,37.86},{1001.66,40.30},{1038.00,41.81},{1072.92,43.27},{1121.99,45.32},{1168.76,47.29},{1072.58,43.24},{1035.38,41.68},{985.38,39.58},{941.70,37.73},{924.06,36.98},{921.78,36.89},{926.99,37.11},{936.03,37.50},{947.23,37.99},{960.02,38.55},{973.87,39.17},{994.78,40.10},{1014.46,40.99},{1020.36,41.25},{1034.64,41.90},{1039.11,42.10},{1057.90,42.97},{1076.92,43.85},{1095.99,44.73},{1019.45,41.15},{989.44,39.74},{948.68,37.83},{918.72,36.42},{894.36,35.27},{855.27,33.41},{824.04,31.92},{790.07,30.29},{761.75,28.93},{737.41,27.74},{826.10,32.08},{858.60,33.67},{882.13,34.83},{901.17,35.76},{928.65,37.12},{951.32,38.25},{970.97,39.23},{980.02,39.68},{988.60,40.11},{1012.18,41.30},{1023.20,41.86},{1033.68,42.39},{1053.40,43.40},{1072.00,44.36},{1079.36,44.74},{1091.87,45.40},{1092.46,45.43},{1093.02,45.45},{1094.12,45.51},{1096.31,45.63},{1099.23,45.78},{1099.80,45.81},{1100.36,45.84},{1101.47,45.90},{1103.70,46.01},{1108.10,46.24},{1116.78,46.70},{1136.35,47.74},{1156.81,48.83},{1174.65,49.80},{1188.49,50.55},{1192.20,50.75},{1196.65,51.00},{1206.87,51.56},{1212.20,51.85},{1224.83,52.55},{1227.51,52.70},{1242.49,53.54},{1161.85,49.00},{1130.54,47.23},{1107.33,45.92},{1088.31,44.84},{1057.49,43.09},{1044.45,42.34},{1032.54,41.66},{992.73,39.37},{961.09,37.54},{943.21,36.49},{942.55,36.45},{942.12,36.43},{941.70,36.40},{940.86,36.35},{939.19,36.26},{935.90,36.06},{935.28,36.03},{934.66,35.99},{933.44,35.92},{931.02,35.78},{926.27,35.50},{917.17,34.96},{907.59,34.39},{894.58,33.61},{972.87,38.30},{1001.99,40.05},{1040.37,42.36},{1075.88,44.52},{1080.57,44.81},{1096.58,45.79},{1110.97,46.67},{1148.33,48.98},{1176.72,50.76},{1193.35,51.81},{1261.99,56.17},{1292.56,58.11},{1316.82,59.66},{1337.79,60.99},{1376.30,63.46},{1409.52,65.59},{1455.76,68.57},{1499.47,71.42},{1543.18,74.28},{1580.68,76.76},{1619.25,79.32},{1652.75,81.56},{1687.85,83.93},{1718.59,86.02},{1751.00,88.24},{1779.54,90.21},{1810.08,92.33},{1837.18,94.22},{1865.06,96.19},{1877.88,97.10},{1890.40,97.99},{1916.97,99.89},{1941.60,101.66},{1964.58,103.32},{1965.40,103.38},{1966.19,103.44},{1967.77,103.56},{1970.88,103.78},{1973.98,104.01},{1977.06,104.23},{1983.15,104.68},{1989.20,105.12},{1990.62,105.22},{1991.24,105.27},{1998.43,105.80},{2004.57,106.25},{2016.29,107.11},{2019.40,107.34},{2022.47,107.57},{2028.53,108.02},{2040.45,108.90},{2064.35,110.68},{2086.56,112.35},{2088.78,112.52},{2090.77,112.67},{2110.73,114.18},{2130.02,115.65},{2159.57,117.92},{2166.22,118.43},{2172.74,118.94},{2194.95,120.67},{2215.20,122.25},{2246.93,124.76},{2250.68,125.06},{2254.34,125.35},{2274.07,126.93},{2283.50,127.69},{2292.75,128.44},{2310.38,129.87},{2326.86,131.21},{2328.68,131.36},{2330.46,131.51},{2349.21,133.05},{2358.16,133.79},{2366.94,134.52},{2384.57,135.99},{2401.18,137.39},{2401.85,137.44},{2402.49,137.50},{2419.85,138.97},{2428.25,139.68},{2436.52,140.39},{2453.59,141.85},{2469.67,143.25},{2469.92,143.27},{2470.15,143.29},{2478.44,144.01},{2486.45,144.71},{2494.34,145.40},{2502.15,146.09},{2516.74,147.37},{2531.53,148.69},{2547.24,150.10},{2551.24,150.46},{2555.18,150.82},{2562.95,151.52},{2577.06,152.80},{2591.23,154.10},{2601.56,155.05},{2623.33,157.07},{2646.06,159.21},{2648.36,159.43},{2677.04,162.17},{2695.34,163.94},{2701.16,164.50},{2725.48,166.89},{2736.27,167.97},{2749.35,169.28},{2775.09,171.89},{2777.39,172.12},{2780.03,172.39},{2782.67,172.66},{2787.94,173.20},{2793.19,173.75},{2798.41,174.29},{2807.55,175.24},{2812.79,175.79},{2818.05,176.34},{2830.00,177.61},{2841.08,178.79},{2854.40,179.78},{2856.25,179.58},{2858.06,179.38},{2861.66,178.99},{2870.09,178.07},{2879.30,177.05},{2879.67,177.01},{2880.01,176.97},{2890.52,175.81},{2900.65,174.67},{2904.58,174.23},{2911.93,173.40},{2919.36,172.55},{2936.51,170.59},{2967.79,166.98},{2976.66,165.94},{2997.12,163.55},{3007.32,162.34},{3025.52,160.17}};
			break;
		case 100:
			curve = new double[][] {{573.54,22.64},{671.56,26.67},{739.77,29.48},{742.16,29.58},{744.54,29.68},{815.42,32.60},{873.99,35.03},{914.86,36.72},{976.19,39.27},{1013.92,40.84},{1050.03,42.35},{1100.55,44.46},{1148.53,46.48},{1052.45,42.44},{1015.36,40.88},{965.57,38.79},{924.14,37.04},{907.95,36.35},{906.81,36.30},{912.95,36.56},{922.83,36.99},{934.64,37.51},{947.96,38.09},{962.28,38.73},{983.77,39.69},{1003.90,40.59},{1009.92,40.86},{1024.48,41.53},{1029.03,41.73},{1048.13,42.61},{1067.44,43.50},{1086.78,44.40},{1010.27,40.82},{980.30,39.42},{939.61,37.50},{909.73,36.10},{885.44,34.95},{846.49,33.10},{815.39,31.62},{781.58,30.00},{753.41,28.64},{729.22,27.46},{817.96,31.80},{850.51,33.39},{874.09,34.55},{893.17,35.49},{920.74,36.85},{943.48,37.98},{963.21,38.97},{972.29,39.42},{980.91,39.86},{1004.60,41.05},{1015.66,41.61},{1026.19,42.15},{1046.01,43.16},{1064.70,44.13},{1072.10,44.51},{1084.68,45.16},{1085.27,45.19},{1085.83,45.22},{1086.94,45.28},{1089.13,45.40},{1092.07,45.55},{1092.64,45.58},{1093.20,45.61},{1094.32,45.67},{1096.56,45.78},{1100.99,46.02},{1109.71,46.48},{1129.38,47.52},{1149.93,48.62},{1167.85,49.58},{1181.76,50.34},{1185.48,50.55},{1189.95,50.79},{1200.22,51.36},{1205.57,51.65},{1218.26,52.36},{1220.95,52.51},{1236.00,53.35},{1155.37,48.81},{1124.08,47.04},{1100.89,45.73},{1081.88,44.65},{1051.09,42.90},{1038.06,42.16},{1026.17,41.47},{986.42,39.19},{954.82,37.36},{936.98,36.31},{936.32,36.27},{935.90,36.25},{935.48,36.23},{934.64,36.18},{932.97,36.08},{929.69,35.89},{929.07,35.85},{928.46,35.81},{927.23,35.74},{924.82,35.60},{920.09,35.32},{911.00,34.78},{901.45,34.22},{888.47,33.44},{966.77,38.13},{995.91,39.88},{1034.32,42.20},{1069.87,44.35},{1074.57,44.64},{1090.60,45.62},{1105.01,46.51},{1142.41,48.82},{1170.86,50.61},{1187.51,51.66},{1256.17,56.01},{1286.74,57.96},{1311.01,59.50},{1331.98,60.84},{1370.51,63.30},{1403.74,65.44},{1450.02,68.43},{1493.75,71.27},{1537.49,74.13},{1575.02,76.61},{1613.61,79.18},{1647.13,81.42},{1682.26,83.79},{1713.02,85.88},{1745.45,88.10},{1774.02,90.07},{1804.58,92.20},{1831.70,94.09},{1859.60,96.06},{1872.43,96.97},{1884.96,97.86},{1911.54,99.76},{1936.20,101.53},{1959.19,103.20},{1960.01,103.26},{1960.81,103.32},{1962.38,103.43},{1965.50,103.66},{1968.60,103.88},{1971.68,104.11},{1977.78,104.55},{1983.82,105.00},{1985.88,105.15},{1993.07,105.67},{1998.87,106.10},{1999.21,106.13},{2010.94,106.99},{2014.05,107.22},{2017.12,107.45},{2023.19,107.90},{2035.11,108.78},{2059.03,110.56},{2081.25,112.23},{2083.47,112.40},{2085.46,112.55},{2105.44,114.06},{2124.74,115.53},{2154.31,117.81},{2160.97,118.32},{2167.49,118.82},{2189.72,120.55},{2209.98,122.14},{2241.73,124.65},{2245.49,124.95},{2249.15,125.24},{2268.89,126.82},{2278.33,127.58},{2287.59,128.33},{2305.22,129.76},{2321.72,131.11},{2323.54,131.26},{2325.32,131.40},{2344.08,132.95},{2353.04,133.69},{2361.83,134.42},{2379.47,135.89},{2396.09,137.29},{2396.75,137.34},{2397.40,137.40},{2414.77,138.87},{2423.18,139.58},{2431.45,140.29},{2448.52,141.76},{2464.62,143.15},{2464.87,143.17},{2465.10,143.19},{2473.40,143.91},{2481.41,144.61},{2489.31,145.30},{2497.12,145.99},{2511.72,147.28},{2526.52,148.60},{2542.24,150.01},{2546.25,150.37},{2550.19,150.72},{2557.96,151.43},{2572.08,152.71},{2586.26,154.01},{2596.59,154.96},{2618.38,156.98},{2641.13,159.12},{2643.42,159.34},{2672.12,162.08},{2690.43,163.85},{2696.25,164.42},{2720.59,166.81},{2731.39,167.88},{2744.47,169.20},{2770.23,171.81},{2772.53,172.04},{2775.17,172.31},{2777.82,172.58},{2783.09,173.13},{2788.34,173.67},{2793.57,174.21},{2802.71,175.16},{2807.95,175.71},{2813.22,176.27},{2825.18,177.53},{2836.26,178.71},{2849.59,179.85},{2851.44,179.65},{2853.25,179.45},{2856.85,179.06},{2865.29,178.14},{2874.51,177.13},{2874.87,177.08},{2875.22,177.05},{2885.74,175.88},{2895.87,174.74},{2899.80,174.30},{2907.15,173.47},{2914.59,172.62},{2931.74,170.66},{2963.03,167.05},{2971.91,166.01},{2992.37,163.61},{3002.58,162.41},{3020.79,160.23}};
			break;
		case 150:
			curve = new double[][] {{754.33,30.10},{781.22,31.22},{884.02,35.49},{932.47,37.51},{975.99,39.33},{1034.31,41.77},{1087.96,44.02},{992.37,40.00},{955.76,38.46},{906.88,36.41},{874.42,35.04},{863.46,34.57},{866.16,34.69},{875.26,35.08},{887.75,35.62},{901.43,36.22},{916.34,36.87},{932.03,37.57},{955.18,38.60},{976.59,39.56},{982.94,39.85},{998.28,40.55},{1003.05,40.76},{1023.04,41.68},{1043.15,42.61},{1063.20,43.55},{986.80,39.97},{956.93,38.57},{916.45,36.66},{886.77,35.27},{862.67,34.13},{824.09,32.30},{793.34,30.83},{759.97,29.23},{732.22,27.89},{708.41,26.73},{797.29,31.08},{829.96,32.68},{853.67,33.84},{872.88,34.79},{900.66,36.16},{923.61,37.30},{943.54,38.30},{952.72,38.76},{961.43,39.20},{985.40,40.40},{996.60,40.97},{1007.26,41.52},{1027.33,42.54},{1046.27,43.52},{1053.76,43.91},{1066.50,44.57},{1067.10,44.60},{1067.66,44.63},{1068.79,44.69},{1071.01,44.80},{1073.98,44.96},{1074.56,44.99},{1075.13,45.02},{1076.27,45.08},{1078.53,45.20},{1083.02,45.43},{1091.85,45.90},{1111.77,46.95},{1132.58,48.07},{1150.72,49.05},{1164.79,49.81},{1168.56,50.02},{1173.09,50.27},{1183.47,50.84},{1188.88,51.14},{1201.72,51.85},{1204.44,52.00},{1219.65,52.85},{1139.06,48.31},{1107.81,46.55},{1084.66,45.24},{1065.69,44.16},{1034.98,42.42},{1021.99,41.68},{1010.13,41.00},{970.53,38.72},{939.09,36.90},{921.33,35.86},{920.68,35.82},{920.26,35.80},{919.84,35.77},{919.00,35.72},{917.35,35.62},{914.08,35.43},{913.47,35.40},{912.86,35.36},{911.64,35.29},{909.24,35.15},{904.53,34.87},{895.50,34.34},{886.00,33.77},{873.11,33.00},{951.45,37.69},{980.63,39.45},{1019.12,41.77},{1054.77,43.93},{1059.49,44.22},{1075.58,45.20},{1090.05,46.09},{1127.58,48.42},{1156.14,50.21},{1172.88,51.26},{1241.55,55.62},{1272.14,57.57},{1296.43,59.11},{1317.42,60.45},{1355.98,62.92},{1389.26,65.05},{1435.60,68.05},{1479.41,70.89},{1523.22,73.76},{1560.81,76.25},{1599.47,78.82},{1633.06,81.07},{1668.24,83.44},{1699.07,85.53},{1731.55,87.76},{1760.18,89.73},{1790.79,91.86},{1817.97,93.76},{1845.92,95.73},{1858.77,96.64},{1871.33,97.53},{1897.96,99.44},{1922.66,101.21},{1945.70,102.88},{1946.52,102.94},{1947.31,103.00},{1948.89,103.12},{1952.01,103.34},{1955.12,103.57},{1958.20,103.79},{1964.32,104.24},{1970.37,104.68},{1972.43,104.83},{1979.63,105.36},{1985.79,105.81},{1997.53,106.68},{2000.65,106.91},{2003.72,107.14},{2009.80,107.59},{2021.75,108.47},{2045.71,110.26},{2048.25,110.45},{2067.98,111.93},{2070.20,112.10},{2072.19,112.25},{2092.21,113.77},{2111.54,115.24},{2141.17,117.51},{2147.84,118.03},{2154.37,118.54},{2176.63,120.27},{2196.93,121.86},{2228.74,124.37},{2232.50,124.67},{2236.16,124.96},{2255.94,126.55},{2265.39,127.31},{2274.67,128.06},{2292.33,129.49},{2308.86,130.84},{2310.68,130.99},{2312.46,131.13},{2331.26,132.68},{2340.22,133.42},{2349.03,134.15},{2366.70,135.63},{2383.35,137.03},{2384.01,137.08},{2384.66,137.14},{2402.06,138.61},{2410.48,139.33},{2418.77,140.04},{2435.87,141.51},{2451.99,142.90},{2452.24,142.92},{2452.47,142.94},{2460.78,143.66},{2468.80,144.36},{2476.72,145.06},{2484.55,145.75},{2499.16,147.04},{2514.00,148.36},{2529.73,149.77},{2533.75,150.13},{2537.69,150.49},{2545.48,151.19},{2559.63,152.48},{2573.83,153.78},{2584.17,154.73},{2605.99,156.76},{2628.78,158.90},{2631.08,159.12},{2659.82,161.86},{2678.16,163.64},{2683.99,164.20},{2708.37,166.60},{2719.18,167.68},{2732.29,168.99},{2758.08,171.60},{2760.39,171.84},{2763.04,172.11},{2765.69,172.38},{2770.96,172.93},{2776.22,173.47},{2781.46,174.01},{2790.62,174.97},{2795.87,175.52},{2801.14,176.07},{2813.12,177.34},{2824.22,178.52},{2837.57,179.96},{2839.43,179.84},{2841.24,179.64},{2844.85,179.25},{2853.30,178.33},{2862.53,177.31},{2862.90,177.27},{2863.25,177.23},{2873.78,176.06},{2883.92,174.92},{2887.87,174.48},{2895.23,173.65},{2902.68,172.80},{2919.84,170.83},{2951.15,167.22},{2960.03,166.19},{2980.51,163.78},{2990.73,162.58},{3008.95,160.40}};
			break;
		case 200:
			curve = new double[][] {{893.78,35.92},{987.58,39.86},{894.58,35.95},{860.33,34.51},{815.60,32.63},{810.25,32.40},{809.81,32.38},{819.03,32.77},{832.69,33.36},{848.94,34.07},{865.16,34.78},{882.16,35.52},{899.60,36.30},{924.83,37.42},{947.79,38.45},{954.54,38.76},{970.80,39.50},{975.83,39.73},{996.86,40.69},{1017.89,41.66},{1038.76,42.64},{962.49,39.07},{932.74,37.67},{892.49,35.78},{863.03,34.39},{839.15,33.26},{800.99,31.45},{770.64,30.01},{737.76,28.43},{710.46,27.11},{687.08,25.97},{776.10,30.32},{808.92,31.93},{832.77,33.10},{852.12,34.06},{880.13,35.44},{903.32,36.59},{923.46,37.60},{932.75,38.07},{941.57,38.51},{965.84,39.73},{977.19,40.31},{987.99,40.86},{1008.33,41.90},{1027.53,42.89},{1035.12,43.28},{1048.04,43.95},{1048.64,43.98},{1049.22,44.01},{1050.35,44.07},{1052.61,44.19},{1055.62,44.35},{1056.21,44.38},{1056.79,44.41},{1057.94,44.47},{1060.23,44.59},{1064.78,44.83},{1073.74,45.30},{1093.93,46.37},{1115.01,47.50},{1133.38,48.49},{1147.63,49.27},{1151.44,49.47},{1156.02,49.73},{1166.54,50.30},{1172.01,50.61},{1185.00,51.33},{1187.75,51.48},{1203.14,52.34},{1122.60,47.81},{1091.39,46.04},{1068.28,44.74},{1049.35,43.66},{1018.72,41.92},{1005.77,41.18},{993.96,40.51},{954.51,38.24},{923.21,36.42},{905.56,35.39},{904.91,35.35},{904.49,35.33},{904.07,35.30},{903.24,35.25},{901.59,35.16},{898.35,34.97},{897.74,34.93},{897.13,34.90},{895.92,34.82},{893.53,34.68},{888.86,34.41},{879.88,33.88},{870.45,33.32},{857.64,32.55},{936.03,37.25},{965.25,39.00},{1003.82,41.33},{1039.58,43.50},{1044.31,43.79},{1060.46,44.78},{1074.99,45.67},{1112.65,48.00},{1141.35,49.80},{1158.16,50.86},{1226.85,55.22},{1257.46,57.16},{1281.77,58.71},{1302.77,60.05},{1341.38,62.52},{1374.69,64.66},{1421.12,67.66},{1465.00,70.51},{1508.88,73.39},{1546.54,75.87},{1585.27,78.45},{1618.92,80.70},{1654.18,83.08},{1685.06,85.18},{1717.61,87.41},{1746.29,89.38},{1776.96,91.51},{1804.19,93.42},{1832.19,95.39},{1845.07,96.30},{1857.65,97.20},{1884.33,99.10},{1909.08,100.89},{1932.16,102.56},{1932.99,102.62},{1933.78,102.68},{1935.36,102.79},{1938.49,103.02},{1941.60,103.25},{1944.69,103.47},{1950.82,103.92},{1956.89,104.36},{1958.95,104.51},{1966.16,105.04},{1972.33,105.50},{1984.10,106.36},{1987.22,106.59},{1990.30,106.82},{1996.39,107.27},{2008.36,108.16},{2032.36,109.95},{2054.67,111.63},{2056.90,111.79},{2058.89,111.94},{2078.94,113.46},{2098.32,114.94},{2113.34,116.09},{2127.99,117.22},{2134.67,117.73},{2141.22,118.24},{2163.52,119.98},{2183.85,121.57},{2215.72,124.09},{2219.48,124.39},{2223.16,124.68},{2242.97,126.27},{2252.44,127.03},{2261.73,127.78},{2279.43,129.21},{2295.98,130.57},{2297.80,130.71},{2299.59,130.86},{2318.41,132.41},{2327.40,133.15},{2336.22,133.89},{2353.91,135.36},{2370.59,136.77},{2371.26,136.82},{2371.91,136.88},{2389.34,138.35},{2397.77,139.07},{2406.08,139.78},{2423.20,141.25},{2439.35,142.65},{2439.60,142.67},{2439.83,142.69},{2448.16,143.41},{2456.19,144.11},{2464.12,144.81},{2471.96,145.50},{2486.61,146.79},{2501.46,148.11},{2517.22,149.53},{2521.25,149.89},{2525.20,150.25},{2532.99,150.95},{2547.17,152.24},{2561.39,153.54},{2571.75,154.50},{2593.60,156.53},{2616.44,158.68},{2618.74,158.89},{2647.52,161.64},{2665.89,163.42},{2671.73,163.99},{2696.15,166.39},{2706.98,167.46},{2720.11,168.78},{2745.94,171.40},{2748.25,171.63},{2750.90,171.90},{2753.56,172.18},{2758.84,172.72},{2764.11,173.26},{2769.36,173.81},{2778.54,174.76},{2783.79,175.32},{2789.07,175.87},{2801.07,177.14},{2812.19,178.33},{2825.56,179.77},{2827.42,179.97},{2829.24,179.83},{2832.85,179.44},{2841.31,178.52},{2850.56,177.50},{2850.93,177.46},{2851.28,177.42},{2861.83,176.25},{2871.99,175.11},{2875.94,174.66},{2883.31,173.83},{2890.77,172.98},{2907.94,171.01},{2939.27,167.40},{2948.16,166.36},{2968.66,163.96},{2978.89,162.75},{2997.13,160.57}};
			break;
		case 250:
			curve = new double[][] {{726.64,28.88},{770.78,30.78},{798.54,31.98},{820.50,32.94},{841.52,33.86},{862.02,34.77},{890.57,36.04},{915.85,37.18},{923.18,37.51},{940.73,38.31},{946.12,38.56},{968.57,39.59},{990.81,40.61},{1012.74,41.64},{936.62,38.07},{907.02,36.68},{867.07,34.81},{837.90,33.43},{814.29,32.32},{776.65,30.53},{746.78,29.11},{714.50,27.56},{687.76,26.27},{664.88,25.16},{754.08,29.51},{787.07,31.13},{811.09,32.31},{830.59,33.27},{858.88,34.67},{882.33,35.84},{902.73,36.85},{912.14,37.33},{921.08,37.77},{945.71,39.02},{957.22,39.60},{968.18,40.16},{988.84,41.22},{1008.33,42.22},{1016.03,42.62},{1029.15,43.30},{1029.76,43.33},{1030.35,43.36},{1031.50,43.42},{1033.79,43.54},{1036.85,43.70},{1037.45,43.74},{1038.03,43.77},{1039.20,43.83},{1041.53,43.95},{1046.15,44.19},{1055.24,44.67},{1075.73,45.76},{1097.12,46.90},{1115.75,47.91},{1130.19,48.69},{1134.06,48.90},{1138.70,49.16},{1149.35,49.74},{1154.90,50.05},{1168.05,50.78},{1170.84,50.94},{1186.42,51.81},{1105.92,47.27},{1074.76,45.52},{1051.69,44.21},{1032.81,43.14},{1002.27,41.40},{989.37,40.67},{977.60,39.99},{938.32,37.73},{907.19,35.93},{889.64,34.90},{888.99,34.86},{888.58,34.84},{888.16,34.82},{887.34,34.77},{885.70,34.67},{882.47,34.48},{881.86,34.45},{881.26,34.41},{880.06,34.34},{877.69,34.20},{873.04,33.93},{864.13,33.40},{854.76,32.84},{842.05,32.09},{920.48,36.78},{949.75,38.54},{988.41,40.87},{1024.28,43.05},{1029.03,43.34},{1045.25,44.33},{1059.84,45.23},{1097.65,47.57},{1126.48,49.37},{1143.37,50.44},{1212.08,54.80},{1242.72,56.75},{1267.04,58.30},{1288.07,59.64},{1326.71,62.11},{1360.07,64.25},{1406.58,67.25},{1450.54,70.11},{1494.50,72.99},{1532.24,75.48},{1571.04,78.06},{1604.76,80.32},{1640.08,82.70},{1671.03,84.81},{1703.64,87.04},{1732.38,89.02},{1763.11,91.16},{1790.39,93.06},{1818.46,95.04},{1831.36,95.96},{1843.97,96.85},{1870.70,98.76},{1895.51,100.55},{1918.63,102.22},{1919.46,102.28},{1920.25,102.34},{1921.84,102.46},{1924.97,102.69},{1928.09,102.91},{1931.19,103.14},{1937.32,103.59},{1943.41,104.03},{1945.47,104.18},{1952.70,104.71},{1958.87,105.17},{1970.66,106.04},{1973.79,106.27},{1976.88,106.49},{1982.98,106.95},{1994.98,107.84},{2019.02,109.63},{2041.37,111.31},{2043.60,111.48},{2045.61,111.63},{2065.70,113.15},{2085.11,114.63},{2114.84,116.91},{2121.53,117.43},{2128.09,117.94},{2150.43,119.67},{2170.80,121.27},{2186.57,122.51},{2202.72,123.79},{2206.50,124.10},{2210.18,124.39},{2230.03,125.98},{2239.51,126.74},{2248.82,127.49},{2266.55,128.93},{2283.13,130.28},{2284.96,130.43},{2286.75,130.58},{2305.61,132.13},{2314.61,132.88},{2323.44,133.61},{2341.17,135.09},{2357.88,136.49},{2358.55,136.55},{2359.19,136.61},{2376.65,138.08},{2385.10,138.80},{2393.42,139.51},{2410.58,140.99},{2426.76,142.39},{2427.00,142.41},{2427.23,142.43},{2435.57,143.15},{2443.62,143.86},{2451.56,144.55},{2459.42,145.24},{2474.09,146.54},{2488.97,147.86},{2504.76,149.28},{2508.79,149.64},{2512.75,150.00},{2520.56,150.70},{2534.76,152.00},{2549.01,153.30},{2559.37,154.26},{2581.26,156.29},{2604.15,158.44},{2606.45,158.66},{2635.27,161.41},{2653.68,163.19},{2659.53,163.76},{2683.99,166.17},{2694.84,167.24},{2707.99,168.56},{2733.86,171.18},{2736.17,171.42},{2738.83,171.69},{2741.49,171.97},{2746.78,172.51},{2752.06,173.06},{2757.31,173.60},{2766.51,174.56},{2771.78,175.11},{2777.07,175.67},{2789.08,176.94},{2800.22,178.13},{2813.62,179.57},{2815.48,179.77},{2817.30,179.97},{2820.91,179.64},{2829.39,178.71},{2838.66,177.69},{2839.03,177.65},{2839.38,177.61},{2849.94,176.44},{2860.12,175.30},{2864.08,174.85},{2871.47,174.02},{2878.94,173.16},{2896.11,171.20},{2927.46,167.58},{2936.36,166.55},{2956.88,164.14},{2967.12,162.93},{2985.38,160.75}};
			break;
		case 300:
			curve = new double[][] {{752.31,30.00},{801.26,32.16},{843.16,34.03},{874.68,35.45},{883.37,35.84},{903.75,36.77},{909.88,37.05},{935.04,38.20},{959.43,39.33},{983.14,40.44},{907.27,36.88},{877.91,35.51},{838.42,33.65},{809.70,32.30},{786.51,31.20},{749.66,29.45},{720.51,28.06},{689.10,26.56},{663.13,25.30},{640.96,24.23},{730.40,28.60},{763.62,30.22},{787.86,31.42},{807.58,32.39},{836.24,33.80},{860.04,34.99},{880.77,36.02},{890.35,36.50},{899.44,36.96},{924.51,38.22},{936.24,38.82},{947.41,39.39},{968.46,40.46},{988.31,41.49},{996.16,41.89},{1009.52,42.59},{1010.15,42.62},{1010.74,42.65},{1011.92,42.71},{1014.25,42.83},{1017.37,43.00},{1017.97,43.03},{1018.57,43.06},{1019.76,43.12},{1022.13,43.25},{1026.83,43.49},{1036.09,43.98},{1056.94,45.09},{1078.70,46.25},{1097.64,47.27},{1112.31,48.07},{1116.23,48.28},{1120.95,48.54},{1131.77,49.14},{1137.39,49.45},{1150.75,50.19},{1153.57,50.35},{1169.37,51.23},{1088.93,46.70},{1057.82,44.95},{1034.81,43.64},{1015.99,42.58},{985.55,40.85},{972.70,40.11},{960.97,39.44},{921.89,37.19},{890.95,35.40},{873.52,34.38},{872.87,34.34},{872.46,34.32},{872.05,34.29},{871.23,34.25},{869.60,34.15},{866.40,33.96},{865.80,33.93},{865.20,33.89},{864.01,33.82},{861.65,33.68},{857.04,33.41},{848.19,32.89},{838.90,32.34},{826.29,31.59},{904.78,36.28},{934.10,38.04},{972.86,40.38},{1008.87,42.57},{1013.64,42.86},{1029.93,43.86},{1044.59,44.76},{1082.55,47.11},{1111.53,48.92},{1128.52,50.00},{1197.26,54.36},{1227.92,56.31},{1252.26,57.86},{1273.31,59.20},{1312.00,61.67},{1345.41,63.82},{1392.01,66.83},{1436.05,69.69},{1480.10,72.58},{1517.92,75.07},{1556.80,77.66},{1590.59,79.92},{1625.99,82.31},{1657.01,84.42},{1689.69,86.65},{1718.49,88.64},{1749.29,90.78},{1776.64,92.69},{1804.76,94.67},{1817.69,95.59},{1830.34,96.49},{1857.13,98.40},{1881.98,100.19},{1905.16,101.87},{1905.98,101.93},{1906.78,101.99},{1908.37,102.11},{1911.51,102.34},{1914.64,102.56},{1917.74,102.79},{1923.89,103.24},{1929.99,103.68},{1932.06,103.84},{1939.30,104.37},{1945.48,104.82},{1957.30,105.69},{1960.43,105.92},{1963.53,106.15},{1969.64,106.60},{1981.66,107.50},{2005.76,109.29},{2028.16,110.98},{2030.39,111.14},{2032.39,111.30},{2052.53,112.82},{2071.98,114.30},{2101.77,116.59},{2108.48,117.11},{2115.05,117.62},{2137.43,119.36},{2157.84,120.96},{2189.83,123.49},{2193.61,123.79},{2197.30,124.08},{2217.19,125.67},{2226.69,126.44},{2236.02,127.19},{2253.78,128.63},{2255.18,128.75},{2270.39,129.99},{2272.22,130.14},{2274.02,130.29},{2292.91,131.84},{2301.93,132.59},{2310.78,133.32},{2328.54,134.80},{2345.28,136.21},{2345.95,136.27},{2346.60,136.32},{2364.09,137.80},{2372.56,138.52},{2380.89,139.24},{2398.07,140.71},{2414.28,142.11},{2414.53,142.13},{2414.76,142.15},{2423.12,142.88},{2431.18,143.59},{2439.14,144.28},{2447.01,144.97},{2461.71,146.27},{2476.62,147.60},{2492.43,149.02},{2496.47,149.38},{2500.43,149.74},{2508.26,150.45},{2522.48,151.74},{2536.76,153.05},{2547.14,154.01},{2569.06,156.04},{2592.00,158.20},{2594.30,158.42},{2623.17,161.17},{2641.61,162.96},{2647.47,163.53},{2671.97,165.94},{2682.85,167.02},{2696.02,168.34},{2721.94,170.96},{2724.25,171.20},{2726.91,171.47},{2729.58,171.75},{2734.88,172.29},{2740.16,172.84},{2745.43,173.38},{2754.65,174.34},{2759.92,174.90},{2765.22,175.45},{2777.26,176.73},{2788.41,177.92},{2801.83,179.36},{2803.70,179.56},{2805.52,179.76},{2809.14,179.84},{2817.64,178.92},{2826.93,177.89},{2827.29,177.85},{2827.64,177.81},{2838.23,176.64},{2848.42,175.50},{2852.38,175.05},{2859.79,174.21},{2867.27,173.36},{2884.45,171.39},{2915.83,167.77},{2924.74,166.73},{2945.27,164.33},{2955.52,163.11},{2973.80,160.93}};
			break;
		case 350:
			curve = new double[][] {{794.45,31.88},{843.81,34.13},{853.68,34.58},{888.84,36.19},{919.02,37.58},{946.66,38.87},{871.31,35.34},{842.44,33.99},{803.88,32.18},{776.00,30.86},{753.60,29.81},{718.17,28.13},{690.25,26.80},{660.26,25.36},{635.52,24.16},{614.43,23.14},{704.22,27.52},{737.78,29.17},{762.35,30.38},{782.38,31.36},{811.58,32.81},{835.87,34.01},{857.06,35.07},{866.86,35.56},{876.17,36.03},{901.85,37.33},{913.85,37.93},{925.30,38.52},{946.86,39.62},{967.19,40.67},{975.22,41.08},{988.90,41.80},{989.54,41.83},{990.15,41.86},{991.35,41.92},{993.73,42.05},{996.92,42.21},{997.54,42.25},{998.15,42.28},{999.37,42.34},{1001.79,42.47},{1006.60,42.72},{1016.06,43.22},{1037.37,44.35},{1059.58,45.54},{1078.89,46.58},{1093.85,47.39},{1097.84,47.61},{1102.64,47.87},{1113.66,48.48},{1119.39,48.80},{1132.97,49.55},{1135.84,49.71},{1151.91,50.61},{1071.53,46.09},{1040.49,44.33},{1017.54,43.03},{998.77,41.97},{968.46,40.24},{955.66,39.51},{944.00,38.85},{905.15,36.61},{874.42,34.83},{857.12,33.82},{856.49,33.78},{856.08,33.76},{855.67,33.73},{854.86,33.69},{853.24,33.59},{850.06,33.41},{849.47,33.37},{848.87,33.34},{847.69,33.27},{845.36,33.13},{840.79,32.86},{832.02,32.34},{822.81,31.79},{810.33,31.05},{888.87,35.75},{918.25,37.52},{957.13,39.86},{993.28,42.06},{998.07,42.35},{1014.45,43.35},{1029.19,44.26},{1067.33,46.62},{1096.48,48.44},{1113.58,49.52},{1182.33,53.89},{1213.02,55.84},{1237.39,57.39},{1258.46,58.73},{1297.21,61.21},{1330.66,63.36},{1377.37,66.37},{1421.50,69.24},{1465.65,72.14},{1503.56,74.64},{1542.53,77.23},{1576.40,79.50},{1611.88,81.89},{1642.98,84.01},{1675.73,86.25},{1704.61,88.24},{1735.48,90.38},{1762.90,92.30},{1791.09,94.29},{1804.05,95.21},{1816.73,96.11},{1843.58,98.03},{1868.50,99.82},{1891.72,101.50},{1892.55,101.56},{1893.35,101.62},{1894.94,101.74},{1898.09,101.97},{1901.22,102.19},{1904.33,102.42},{1910.50,102.87},{1916.61,103.32},{1918.68,103.47},{1925.94,104.00},{1932.14,104.46},{1943.98,105.33},{1947.12,105.56},{1950.22,105.79},{1956.35,106.25},{1968.40,107.14},{1992.55,108.94},{2015.00,110.63},{2017.24,110.79},{2019.24,110.95},{2039.42,112.47},{2058.92,113.96},{2088.77,116.25},{2095.50,116.77},{2102.08,117.28},{2124.51,119.03},{2144.97,120.63},{2177.02,123.16},{2180.81,123.47},{2184.51,123.76},{2204.43,125.36},{2213.96,126.12},{2223.31,126.88},{2241.10,128.32},{2257.75,129.68},{2259.58,129.83},{2261.38,129.98},{2280.31,131.53},{2289.35,132.28},{2298.22,133.02},{2316.01,134.50},{2318.69,134.73},{2332.78,135.91},{2333.46,135.97},{2334.11,136.03},{2351.63,137.51},{2360.12,138.23},{2368.47,138.94},{2385.68,140.42},{2401.93,141.83},{2402.17,141.85},{2402.40,141.87},{2410.78,142.60},{2418.86,143.30},{2426.83,144.00},{2434.71,144.69},{2449.44,146.00},{2464.38,147.33},{2480.23,148.75},{2484.27,149.11},{2488.24,149.47},{2496.08,150.18},{2510.33,151.47},{2524.64,152.79},{2535.03,153.74},{2557.00,155.78},{2579.98,157.94},{2582.29,158.16},{2611.21,160.92},{2629.69,162.71},{2635.56,163.28},{2660.11,165.70},{2671.00,166.78},{2684.20,168.10},{2710.16,170.73},{2712.47,170.97},{2715.14,171.24},{2717.81,171.52},{2723.12,172.06},{2728.42,172.61},{2733.69,173.16},{2742.93,174.12},{2748.22,174.67},{2753.53,175.23},{2765.58,176.51},{2776.76,177.70},{2790.20,179.15},{2792.07,179.35},{2793.90,179.55},{2797.53,179.94},{2806.04,179.13},{2815.35,178.10},{2815.71,178.06},{2816.06,178.02},{2826.67,176.85},{2836.88,175.70},{2840.85,175.25},{2848.27,174.41},{2855.77,173.56},{2872.95,171.59},{2904.35,167.97},{2913.27,166.93},{2933.83,164.52},{2944.09,163.31},{2962.39,161.12}};
			break;
		case 400:
			curve = new double[][] {{838.84,33.92},{887.52,36.19},{814.83,32.79},{788.24,31.54},{753.38,29.91},{728.46,28.73},{708.50,27.79},{676.92,26.29},{651.96,25.10},{625.01,23.81},{602.65,22.73},{583.50,21.80},{673.91,26.22},{708.05,27.89},{733.15,29.12},{753.70,30.13},{783.74,31.62},{808.80,32.87},{830.70,33.96},{840.83,34.47},{850.46,34.95},{877.02,36.29},{889.43,36.92},{901.26,37.52},{923.54,38.66},{944.53,39.74},{952.81,40.17},{966.90,40.90},{967.55,40.94},{968.18,40.97},{969.42,41.04},{971.88,41.16},{975.16,41.34},{975.79,41.37},{976.42,41.40},{977.68,41.47},{980.17,41.60},{985.12,41.86},{994.85,42.37},{1016.75,43.53},{1039.53,44.75},{1059.31,45.82},{1074.61,46.65},{1078.70,46.87},{1083.60,47.14},{1094.86,47.76},{1100.71,48.08},{1114.58,48.85},{1117.51,49.02},{1133.90,49.94},{1053.60,45.41},{1022.63,43.66},{999.75,42.37},{981.06,41.31},{950.89,39.59},{938.17,38.87},{926.57,38.20},{887.99,35.99},{857.51,34.22},{840.38,33.22},{839.74,33.18},{839.34,33.16},{838.93,33.13},{838.13,33.08},{836.53,32.99},{833.39,32.81},{832.80,32.77},{832.21,32.74},{831.04,32.67},{828.73,32.53},{824.20,32.26},{815.52,31.75},{806.42,31.21},{794.07,30.48},{872.69,35.18},{902.13,36.95},{941.14,39.30},{977.47,41.51},{982.28,41.80},{998.75,42.81},{1013.59,43.72},{1051.93,46.10},{1081.27,47.93},{1098.49,49.02},{1167.27,53.39},{1197.98,55.34},{1222.38,56.89},{1243.48,58.24},{1282.28,60.72},{1315.80,62.87},{1362.62,65.89},{1406.86,68.77},{1451.11,71.67},{1489.12,74.18},{1528.19,76.78},{1562.16,79.05},{1597.72,81.45},{1628.91,83.57},{1661.75,85.82},{1690.71,87.81},{1721.65,89.96},{1749.14,91.89},{1777.41,93.88},{1790.41,94.80},{1803.12,95.70},{1830.04,97.63},{1855.02,99.43},{1878.30,101.11},{1879.13,101.17},{1879.94,101.23},{1881.53,101.35},{1884.69,101.58},{1887.83,101.81},{1890.95,102.03},{1897.13,102.49},{1903.26,102.93},{1905.34,103.09},{1912.60,103.62},{1918.82,104.08},{1930.69,104.95},{1933.84,105.18},{1936.95,105.41},{1943.09,105.87},{1955.17,106.76},{1979.38,108.57},{2001.88,110.26},{2004.12,110.43},{2006.14,110.58},{2026.36,112.11},{2045.91,113.60},{2075.83,115.90},{2082.57,116.42},{2089.17,116.93},{2111.65,118.68},{2132.15,120.29},{2164.28,122.83},{2168.08,123.13},{2171.78,123.42},{2191.75,125.02},{2201.30,125.79},{2210.67,126.55},{2228.50,127.99},{2245.19,129.36},{2247.03,129.51},{2248.83,129.65},{2267.80,131.21},{2276.85,131.96},{2285.75,132.70},{2303.58,134.19},{2320.38,135.60},{2321.06,135.66},{2321.71,135.72},{2339.27,137.20},{2347.77,137.93},{2356.14,138.64},{2373.39,140.12},{2376.59,140.40},{2389.67,141.53},{2389.92,141.55},{2390.15,141.57},{2398.54,142.30},{2406.63,143.01},{2414.62,143.71},{2422.52,144.40},{2437.29,145.71},{2452.26,147.04},{2468.13,148.46},{2472.18,148.83},{2476.16,149.19},{2484.02,149.90},{2498.30,151.20},{2512.64,152.51},{2523.05,153.47},{2545.05,155.51},{2568.09,157.68},{2570.40,157.90},{2599.37,160.66},{2617.89,162.45},{2623.77,163.03},{2648.37,165.45},{2659.29,166.53},{2672.51,167.86},{2698.52,170.49},{2700.84,170.73},{2703.51,171.00},{2706.18,171.28},{2711.51,171.82},{2716.81,172.37},{2722.09,172.92},{2731.36,173.88},{2736.65,174.44},{2741.98,175.00},{2754.05,176.28},{2765.25,177.47},{2778.72,178.92},{2780.59,179.13},{2782.42,179.32},{2786.06,179.72},{2794.59,179.35},{2803.91,178.32},{2804.28,178.28},{2804.63,178.24},{2815.25,177.06},{2825.49,175.92},{2829.47,175.47},{2836.90,174.63},{2844.41,173.77},{2861.60,171.80},{2893.03,168.18},{2901.96,167.14},{2922.54,164.72},{2932.81,163.51},{2951.13,161.32}};
			break;
		case 450:
			curve = new double[][] {{747.63,29.90},{766.28,30.83},{785.15,31.78},{799.78,32.51},{834.92,34.28},{850.06,35.05},{864.09,35.77},{889.74,37.08},{913.25,38.29},{922.39,38.76},{937.83,39.57},{938.54,39.60},{939.22,39.64},{940.57,39.71},{943.24,39.85},{946.80,40.03},{947.49,40.07},{948.17,40.11},{949.53,40.18},{952.23,40.32},{957.57,40.60},{968.04,41.15},{991.42,42.39},{1015.54,43.68},{1036.34,44.80},{1052.34,45.67},{1056.60,45.91},{1061.72,46.19},{1073.45,46.83},{1079.52,47.17},{1093.92,47.97},{1096.94,48.14},{1113.90,49.09},{1033.74,44.57},{1002.90,42.83},{980.15,41.54},{961.58,40.49},{931.65,38.79},{919.05,38.07},{907.57,37.41},{869.42,35.22},{839.35,33.47},{822.47,32.49},{821.84,32.45},{821.45,32.43},{821.05,32.40},{820.25,32.36},{818.68,32.26},{815.58,32.08},{815.00,32.05},{814.42,32.01},{813.27,31.95},{811.00,31.81},{806.54,31.55},{798.01,31.04},{789.05,30.51},{776.91,29.79},{855.63,34.50},{885.18,36.28},{924.38,38.64},{960.95,40.86},{965.80,41.16},{982.41,42.17},{997.37,43.09},{1036.00,45.49},{1065.60,47.34},{1082.99,48.44},{1151.81,52.80},{1182.56,54.76},{1206.99,56.31},{1228.13,57.66},{1267.02,60.15},{1300.61,62.31},{1347.59,65.34},{1391.97,68.23},{1436.36,71.13},{1474.50,73.65},{1513.70,76.26},{1547.78,78.54},{1583.47,80.95},{1614.76,83.07},{1647.71,85.33},{1676.77,87.33},{1707.81,89.49},{1735.40,91.42},{1763.76,93.42},{1776.80,94.34},{1789.55,95.25},{1816.56,97.18},{1841.63,98.98},{1864.98,100.67},{1865.81,100.74},{1866.61,100.79},{1868.21,100.91},{1871.38,101.14},{1874.53,101.37},{1877.66,101.60},{1883.86,102.05},{1890.01,102.50},{1892.10,102.65},{1899.38,103.19},{1905.61,103.65},{1917.52,104.52},{1920.68,104.76},{1923.80,104.99},{1929.96,105.44},{1942.07,106.34},{1966.35,108.15},{1988.92,109.85},{1991.17,110.02},{1993.19,110.17},{2013.48,111.71},{2033.08,113.20},{2063.09,115.50},{2069.84,116.03},{2076.46,116.54},{2099.00,118.29},{2119.56,119.90},{2151.78,122.45},{2155.58,122.75},{2159.30,123.05},{2179.32,124.65},{2188.89,125.42},{2198.29,126.18},{2216.16,127.63},{2232.90,129.00},{2234.74,129.15},{2236.54,129.30},{2255.56,130.86},{2264.64,131.61},{2273.56,132.35},{2291.43,133.84},{2308.28,135.26},{2308.95,135.32},{2309.61,135.37},{2327.21,136.86},{2335.74,137.59},{2344.12,138.31},{2361.42,139.79},{2377.73,141.20},{2377.98,141.22},{2378.21,141.24},{2386.62,141.97},{2394.74,142.68},{2402.74,143.38},{2410.66,144.08},{2425.46,145.39},{2440.48,146.72},{2456.38,148.15},{2460.44,148.52},{2464.43,148.88},{2472.30,149.59},{2486.62,150.89},{2498.73,152.00},{2500.99,152.21},{2511.42,153.17},{2533.47,155.22},{2556.58,157.39},{2558.89,157.61},{2587.91,160.38},{2606.48,162.17},{2612.38,162.75},{2637.03,165.17},{2647.97,166.26},{2661.23,167.59},{2687.28,170.23},{2689.61,170.47},{2692.29,170.74},{2694.97,171.02},{2700.30,171.56},{2705.62,172.11},{2710.91,172.66},{2720.20,173.63},{2725.51,174.19},{2730.84,174.75},{2742.94,176.03},{2754.16,177.23},{2767.66,178.68},{2769.54,178.88},{2771.37,179.08},{2775.02,179.48},{2783.57,179.59},{2792.92,178.56},{2793.28,178.52},{2793.64,178.48},{2804.28,177.30},{2814.53,176.15},{2818.52,175.70},{2825.97,174.86},{2833.50,174.00},{2850.70,172.03},{2882.15,168.40},{2891.09,167.36},{2911.70,164.94},{2921.98,163.73},{2940.33,161.54}};
			break;
		case 500:
			curve = new double[][] {{814.98,33.31},{860.97,35.68},{874.65,36.39},{895.56,37.48},{896.46,37.52},{897.34,37.57},{899.06,37.66},{902.46,37.84},{906.93,38.07},{907.78,38.12},{908.63,38.16},{910.31,38.25},{913.63,38.42},{920.12,38.76},{932.60,39.42},{959.57,40.85},{986.48,42.29},{1009.23,43.52},{1026.51,44.46},{1031.07,44.71},{1036.55,45.01},{1049.06,45.69},{1055.52,46.05},{1070.75,46.90},{1073.94,47.08},{1091.78,48.07},{1011.82,43.57},{981.17,41.84},{958.62,40.56},{940.24,39.52},{910.67,37.84},{898.23,37.13},{886.92,36.48},{849.40,34.33},{819.90,32.61},{803.36,31.65},{802.75,31.61},{802.36,31.59},{801.97,31.57},{801.19,31.52},{799.65,31.43},{796.62,31.25},{796.05,31.22},{795.49,31.19},{794.36,31.12},{792.13,30.99},{787.77,30.73},{779.43,30.24},{770.67,29.72},{758.82,29.01},{837.67,33.73},{867.35,35.51},{906.81,37.89},{943.70,40.13},{948.60,40.43},{965.37,41.46},{980.51,42.39},{1019.49,44.80},{1049.44,46.68},{1067.02,47.79},{1135.89,52.16},{1166.69,54.12},{1191.17,55.68},{1212.36,57.03},{1251.34,59.52},{1285.03,61.68},{1332.19,64.73},{1376.75,67.63},{1421.31,70.55},{1459.60,73.07},{1498.96,75.69},{1533.18,77.98},{1569.01,80.40},{1600.43,82.53},{1633.51,84.80},{1662.68,86.81},{1693.85,88.97},{1721.54,90.91},{1750.01,92.92},{1763.10,93.85},{1775.90,94.76},{1803.01,96.69},{1828.17,98.50},{1851.60,100.20},{1852.44,100.26},{1853.25,100.32},{1854.85,100.44},{1858.03,100.67},{1861.19,100.90},{1864.34,101.13},{1870.56,101.58},{1876.73,102.04},{1878.82,102.19},{1886.13,102.72},{1892.38,103.18},{1904.33,104.06},{1907.50,104.30},{1910.63,104.53},{1916.81,104.99},{1928.97,105.89},{1953.32,107.71},{1975.97,109.41},{1978.23,109.58},{1980.25,109.73},{2000.61,111.27},{2020.28,112.77},{2050.37,115.08},{2057.16,115.60},{2063.80,116.12},{2086.40,117.88},{2107.02,119.49},{2139.34,122.05},{2143.16,122.35},{2146.88,122.65},{2166.96,124.26},{2176.56,125.03},{2185.99,125.79},{2203.91,127.24},{2220.70,128.61},{2222.54,128.77},{2224.35,128.91},{2243.42,130.48},{2252.53,131.24},{2261.47,131.98},{2279.39,133.47},{2296.29,134.89},{2296.96,134.95},{2297.62,135.01},{2315.27,136.50},{2323.82,137.23},{2332.23,137.95},{2349.57,139.44},{2365.92,140.85},{2366.17,140.87},{2366.40,140.89},{2374.83,141.63},{2382.97,142.34},{2391.00,143.04},{2398.94,143.74},{2413.79,145.05},{2428.84,146.39},{2444.78,147.82},{2448.85,148.19},{2452.85,148.55},{2460.74,149.26},{2475.10,150.57},{2489.50,151.89},{2499.96,152.85},{2522.06,154.90},{2537.56,156.36},{2545.23,157.08},{2547.55,157.30},{2576.63,160.08},{2595.25,161.88},{2601.16,162.45},{2625.88,164.88},{2636.84,165.97},{2650.13,167.30},{2676.24,169.95},{2678.58,170.19},{2681.26,170.46},{2683.95,170.74},{2689.29,171.29},{2694.62,171.84},{2699.93,172.39},{2709.24,173.36},{2714.56,173.92},{2719.91,174.48},{2732.04,175.76},{2743.28,176.96},{2756.82,178.42},{2758.70,178.62},{2760.53,178.82},{2764.19,179.22},{2772.76,179.84},{2782.13,178.81},{2782.50,178.77},{2782.85,178.73},{2793.52,177.55},{2803.80,176.39},{2807.80,175.94},{2815.26,175.10},{2822.81,174.24},{2840.01,172.27},{2871.50,168.63},{2880.45,167.59},{2901.08,165.17},{2911.38,163.96},{2929.75,161.77}};
			break;
		default:
			break;
		}
		return curve;
	}
	
}
