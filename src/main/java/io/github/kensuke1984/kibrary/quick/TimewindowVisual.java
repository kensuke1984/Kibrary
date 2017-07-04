package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.selection.Bins2D;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.DistanceAzimuth;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
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
	
	public static final double redVel = 8.4;
	public static final double t65 = 1080.;
	public static final double t100 = 1440.;
	
	public static void main(String[] args) throws IOException {
		Path root = Paths.get(".");
		Path timewindowPath = Paths.get("selectedTimewindow.dat");
		Set<GlobalCMTID> ids = new HashSet<>();
		if (args.length == 1)
			ids.add(new GlobalCMTID(args[0]));
		else
			ids = Utilities.eventFolderSet(root).stream()
				.map(event -> event.getGlobalCMTID()).collect(Collectors.toSet());
		
		for (GlobalCMTID id : ids) {
			final GlobalCMTID id_ = id;
			
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
				
				String[] gmtSets2 = new String[] {"PS_MEDIA 18cx30c"
						, "FONT 10p,Helvetica,black"
						, "PS_CHAR_ENCODING ISOLatin1"
						, "PS_PAGE_ORIENTATION portrait"};
				
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
				
				double slowness = 10.;
				
	//			Map<Integer, Trace> obsStacks = new HashMap<>();
	//			Map<Integer, Trace> synStacks = new HashMap<>();
	//			Map<Integer, double[]> mapWindows = new HashMap<>();
	//			
	//			timewindows.stream().filter(tw -> tw.getGlobalCMTID().equals(id))
	//				.forEach(tw -> {
	//					try {
	//						Path synPath = root.resolve(id.toString() + "/" + tw.getStation().getStationName() + "." + id.toString() + "." + "Tsc");
	//						SACFileName synName = new SACFileName(synPath);
	//						Path obsPath = root.resolve(id.toString() + "/" + tw.getStation().getStationName() + "." + id.toString() + "." + "T");
	//						
	//						boolean isObs = true;
	//						if (!Files.exists(obsPath))
	//							isObs = false;
	//						
	//						SACFileName obsName = null;
	//						if (isObs)
	//							obsName = new SACFileName(obsPath);
	//						
	//						double Y = iatom.getAndIncrement() * 4;
	//						
	//						double distance = tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition())
	//								* 180 / Math.PI;
	//						double azimuth = tw.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(tw.getStation().getPosition())
	//								* 180 / Math.PI;
	//						
	//						//stack bin
	//						int bin = (int) distance;
	//						
	//						text.add(String.format("gmt pstext -J -R -Xa-1c -Ya%.2fc -K -O -N -F+jLB >> $outputps << END\n", Y)
	//								+ "0 .3 " + tw.getStation().getStationName() + " " + tw.getStation().getNetwork() + "\n"
	//								+ String.format("0 0 %.1f\n0 -.3 %s", distance, new Phases(tw.getPhases()).toString()) + "\nEND");
	//						
	//						if (!usedStations.contains(tw.getStation())) {
	//							double normalize = 1.;
	//							Trace synFullTrace = synName.read().createTrace().cutWindow(slowness * distance, slowness * distance + 3500.);
	//							
	//							//stacks
	//							if (synStacks.containsKey(bin)) {
	//								Trace trace0 = synStacks.get(bin);
	//								Trace trace1 = new Trace(trace0.getX(), synFullTrace.getY());
	//								Trace trace = trace0.add(trace1);
	//								synStacks.replace(bin, trace);
	//							}
	//							else {
	//								synStacks.put(bin, synFullTrace);
	//								mapWindows.put(bin, new double[] {tw.getStartTime() - slowness * distance, tw.getEndTime() - slowness * distance});
	//							}
	//							
	//							if (isObs) {
	//								Trace obsFullTrace = obsName.read().createTrace().cutWindow(slowness * distance, slowness * distance + 3500.);
	//								
	//								//stacks
	//								if (obsStacks.containsKey(bin)) {
	//									Trace trace0 = obsStacks.get(bin);
	//									Trace trace1 = new Trace(trace0.getX(), obsFullTrace.getY());
	//									Trace trace = trace0.add(trace1);
	//									obsStacks.replace(bin, trace);
	//								}
	//								else {
	//									obsStacks.put(bin, obsFullTrace);
	//								}
	//								
	//								normalize = 1. / obsFullTrace.getYVector().getLInfNorm();
	//								obsFullTrace = obsFullTrace.multiply(normalize);
	//								textProfile.add(trace2GMT(obsFullTrace, "black", 0, 0, distance, slowness, 40));
	//							}
	//							else 
	//								normalize = 1. / synFullTrace.getYVector().getLInfNorm();
	//							synFullTrace = synFullTrace.multiply(normalize);
	//							textProfile.add(trace2GMT(synFullTrace, "red", 0, 0, distance, slowness, 40));
	//							
	//							usedStations.add(tw.getStation());
	//						}
	//						textProfile.add(verticalLine(tw.getStartTime() - slowness * distance, distance, 3, "green", 0, 0));
	//						textProfile.add(verticalLine(tw.getEndTime() - slowness * distance, distance, 3, "green", 0, 0));
	//						
	//						Trace synTrace = synName.read().createTrace().cutWindow(tw.getStartTime() - 40, tw.getEndTime() + 100);
	//						double normalize = 1.;
	//						if (isObs) {
	//							Trace obsTrace = obsName.read().createTrace().cutWindow(tw.getStartTime() - 40, tw.getEndTime() + 100);
	//							normalize = 1. / obsTrace.getYVector().getLInfNorm();
	//							obsTrace = obsTrace.multiply(normalize);
	//							text.add(trace2GMT(obsTrace, "black", 2, Y, 0, 0., 40));
	//						}
	//						else
	//							normalize = 1. / synTrace.getYVector().getLInfNorm();
	//						synTrace = synTrace.multiply(normalize);
	//						
	//						text.add(trace2GMT(synTrace, "red", 2, Y, 0, 0., 40));
	//						
	//						text.add(verticalLine(40, 0, 1, "green", 2, Y));
	//						text.add(verticalLine((tw.getEndTime() - tw.getStartTime() + 40), 0, 1, "green", 2, Y));
	//
	//						
	//					} catch (IOException e) {
	//						e.printStackTrace();
	//					}
	//				});
	//			
	//			text.add(endGMT());
	//			textProfile.add(endGMT());
	//			
	//			createGMTFile(outpath);
	//			
	//			for (String line : text)
	//				Files.write(outpath, (line + "\n").getBytes(), StandardOpenOption.APPEND);
	//			
	//			createGMTFile(outpathProfile);
	//			for (String line : textProfile)
	//				Files.write(outpathProfile, (line + "\n").getBytes(), StandardOpenOption.APPEND);
	//			
	//			//stack
	//			Files.deleteIfExists(outpathStack);
	//			Files.createFile(outpathStack);
	//			createGMTFile(outpathStack);
	//			String begin = beginGMT(dirPath.toString() + "stack." + id.toString() + ".ps"
	//					, "0/1000/0/75"
	//					, "X14c/21c"
	//					, gmtSets2
	//					, new String[] {"-BSW -Bx1000 -By20"});
	//			Files.write(outpathStack, (begin + "\n").getBytes(), StandardOpenOption.APPEND);
	//			for (int bin : synStacks.keySet()) {
	//				Trace obs = obsStacks.get(bin);
	//				Trace syn = synStacks.get(bin);
	//				double[] tw = mapWindows.get(bin);
	//				double normalize = 1. / syn.getYVector().getLInfNorm();
	//				obs = obs.multiply(normalize);
	//				syn = syn.multiply(normalize);
	//				String obsString = trace2GMT(obs, "black", 0, 0, bin, slowness, 10);
	//				String synString = trace2GMT(syn, "red", 0, 0, bin, slowness, 10);
	//				String twString0 = verticalLine(tw[0], bin, 3, "green", 0, 0);
	//				String twString1 = verticalLine(tw[1], bin, 3, "green", 0, 0);
	//				Files.write(outpathStack, (obsString + "\n").getBytes(), StandardOpenOption.APPEND);
	//				Files.write(outpathStack, (synString + "\n").getBytes(), StandardOpenOption.APPEND);
	//				Files.write(outpathStack, (twString0 + "\n" + twString1 + "\n").getBytes(), StandardOpenOption.APPEND);
	//			}
	//			Files.write(outpathStack, endGMT().getBytes(), StandardOpenOption.APPEND);
				
				
				//bins
				Bins2D bins = new Bins2D(id, 2., 1., timewindows);
				Map<DistanceAzimuth, Integer> usedBinsCount = new HashMap<>();
				Set<Path> outpathList = new HashSet<>();
				for (TimewindowInformation timewindow : timewindows) {
					if (usedStations.contains(timewindow.getStation()))
						continue;
					
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
						
						Path synPath = root.resolve(id.toString() + "/" + timewindow.getStation().getStationName() + "." + id.toString() + "." + "Tsc");
						SACFileName synName = new SACFileName(synPath);
						Path obsPath = root.resolve(id.toString() + "/" + timewindow.getStation().getStationName() + "." + id.toString() + "." + "T");
						
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
						double t0 = slowness * distance > 0. ? slowness * distance : 0.;
						double t1 = slowness * distance + 3500. < 4000. ? slowness * distance + 3500. : 4000.;
						Trace synFullTrace = synName.read().createTrace().cutWindow(t0, t1);
						normalize = 1. / synFullTrace.getYVector().getLInfNorm();
						Trace obsFullTrace = null;
						if (isObs) {
							obsFullTrace = obsName.read().createTrace().cutWindow(t0, t1);
							normalize = 1. / obsFullTrace.getYVector().getLInfNorm();
							obsFullTrace = obsFullTrace.multiply(normalize);
						}
						synFullTrace = synFullTrace.multiply(normalize);
						
						String obsString = null;
						if (isObs)
							obsString = trace2GMT(obsFullTrace, "black", 0, 0, distance_azimuth.distance, slowness, 40);
						String synString = trace2GMT(synFullTrace, "red", 0, 0, distance_azimuth.distance, slowness, 40);
						
						if (!Files.exists(outpathBins)) {
							createGMTFile(outpathBins);
							Files.write(outpathBins, (beginGMT(String.format("bins_az%.1f_", distance_azimuth.azimuth) + id.toString() + ".ps"
									, "0/800/0/40"
									, "X14c/28c"
									, gmtSets2
									, new String[] {"-BSW -Bx1000 -By20"}) + "\n").getBytes()
								, StandardOpenOption.APPEND);
							if (isObs)
								Files.write(outpathBins, (obsString + "\n").getBytes(), StandardOpenOption.APPEND);
							Files.write(outpathBins, (synString + "\n").getBytes(), StandardOpenOption.APPEND);
						}
						else {
							if (isObs)
								Files.write(outpathBins, (obsString + "\n").getBytes(), StandardOpenOption.APPEND);
							Files.write(outpathBins, (synString + "\n").getBytes(), StandardOpenOption.APPEND);
						}
					}
				}
				for (TimewindowInformation timewindow : timewindows) {
					if (!usedStations.contains(timewindow.getStation()))
						continue;
					double distance = timewindow.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(timewindow.getStation().getPosition())
							* 180 / Math.PI;
					DistanceAzimuth distance_azimuth = bins.getBinPosition(timewindow);
					double t0 = timewindow.getStartTime() - distance_azimuth.distance * slowness;
					double t1 = timewindow.getEndTime() - distance_azimuth.distance * slowness;
					Path outpathBins = dir1.resolve(String.format("twv_bins_az%.1f.gmt", distance_azimuth.azimuth));
					Files.write(outpathBins, (verticalLine(t0, distance_azimuth.distance, .6, .3, "purple", 0, 0) + "\n").getBytes(), StandardOpenOption.APPEND);
					Files.write(outpathBins, (verticalLine(t1, distance_azimuth.distance, .6, .2, "green", 0, 0) + "\n").getBytes(), StandardOpenOption.APPEND);
				}
				for (Path outpathBins : outpathList)
					Files.write(outpathBins, endGMT().getBytes(), StandardOpenOption.APPEND);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String verticalLine(double xpos, double ypos, double height, double thickness, String color, double X, double Y) {
		return "gmt psxy -J -R -B -W" + thickness + "p," + color + " -K -O " + "-Xa" + X + " -Ya" + Y + " >> $outputps << END\n"
				+ xpos + " " + (ypos - height / 2.) + "\n" + xpos + " " + (ypos + height / 2.) + "\nEND";
	}
	
	public static String trace2GMT(Trace trace, String color, double X, double Y, double GCARC, double slowness, int nSample) {
		String text = "";
		double dt = 0.05;
		int n = nSample;
		
		text += "gmt pswiggle -J -R -Z4. -B "
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

}
