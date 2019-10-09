/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACExtension;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.external.gmt.GMTMap;
import io.github.kensuke1984.kibrary.external.gmt.Symbol;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.timewindow.Timewindow;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;

/**
 * @version 0.0.1
 * @since 2016/08/05
 * @author Yuki
 *
 */
public class RecordSectionStack {
	
	
	final static double startT = 0;
	final static double endT = 0;
	final static double reducedV = 8.3;
	final static double freqL = 0.005;
	final static double freqH = 0.125;
	final static boolean isFilter = true;
	final static double deltaAZ = 3.;
	final static double AZref = 336;
	final double minDistance = 70;
	final double maxDistance = 100;
	final static boolean isMap = false;
	final static double dt = .05;
	final static int minLat = 120;
	final static int maxLat = 120;
	final static int minLon = 20;
	final static int maxLon = 85;
	final static SACComponent comp = SACComponent.T;
	final static SACExtension[] components = new SACExtension[] {SACExtension.T};
	
	public static void main(String[] args) throws IllegalAccessException, TauModelException {
		if (args.length < 2){
			System.err.println("usage: path to SAC directory(031791Y 192591S ...), path to timewindowInformationFile");
			throw new IllegalAccessException();
		}
		try {
			TauP_Time timeTool = new TauP_Time("prem");
			timeTool.parsePhaseList("S");
			File dirs[] = new File[args.length - 1];
			for (int i=0;i<args.length-1;i++){
				dirs[i] = new File(args[i]);
			}
			Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(Paths.get(args[args.length-1]));
			
			for (File dir : dirs) {
//				double[] distanceOfMaxT = {};
//				double[] maxT = {};
				double distanceIncrement = 1.;
				RealVector[] stacksT = new RealVector[(int) (120./distanceIncrement)];
				int[] numOFStack = new int[(int) (120./distanceIncrement)];
				
				Set<TimewindowInformation> thisEventTimewindow = new HashSet<>();
				timewindows.stream()
						.filter(tw -> tw.getGlobalCMTID().toString().equals(dir.toString()))
						.forEachOrdered(tw -> thisEventTimewindow.add(tw));
	
				Set<GlobalCMTID> gcmtID = new HashSet<>();
				Set<Station> stations = new HashSet<>();
				
				Path outpathscript = Paths.get(dir.getAbsolutePath(), "profile.plt");
				Path outpathgmtmap = Paths.get(dir.getAbsolutePath(), "makemap.sh");
				Path outpathscriptstack = Paths.get(dir.getAbsolutePath(), "stackprofile.plt");
				
				GMTMap gmtMap = new GMTMap(String.valueOf(AZref - deltaAZ)+"˚ < AZ < "+String.valueOf(AZref + deltaAZ) + "˚",
						minLon, maxLon, minLat, maxLat);
				String[] gmtHdrString = {String.format("#!/bin/sh\npsname=\"map%.1f.ps\"\n", AZref) + gmtMap.psStart() + "\n"};
				gmtHdrString[0] += GMTMap.psCoast("-Ggray") + "\n";
				plotScriptInitialize(outpathscript, outpathgmtmap, outpathscriptstack, minLat, maxLat, minLon, maxLon, dir);
				
				
				List<SACFileName> sacfilenames = readDir(dir, components);
				
				sacfilenames.stream()
//					.filter(sacfilename -> isSameAzimuth(sacfilename, deltaAZ, AZref))
					.forEachOrdered(sacfilename -> {
						BandPassFilter filter = new BandPassFilter(freqH * 2 * Math.PI / 20, freqL * 2 * Math.PI / 20, 4);
						try {
							Path outPath = Paths.get(dir.getAbsolutePath(), sacfilename.toString() + ".txt");
							Files.write(outPath, "".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
							SACData sacData = sacfilename.read();
							if (isFilter)
								sacData.applyButterworthFilter(filter);
							double distance = sacfilename.readHeader().getValue(SACHeaderEnum.GCARC);
							List<Timewindow> tmptimewindow = new ArrayList<>();
							
							thisEventTimewindow.stream()
								.filter(tw -> {
									boolean sameStation = false;
									try {
										sameStation = tw.getStation().equals(sacfilename.readHeader().getStation());
									} catch (IOException e) {e.printStackTrace();}

//									try {
//										timeTool.depthCorrect(Earth.EARTH_RADIUS - tw.getGlobalCMTID().getEvent().getCmtLocation().getR());
//										timeTool.calculate(distance);
//									} catch (TauModelException e) {
//										e.printStackTrace();
//									}
									return sameStation;
								})
								.filter(tw -> tw.getComponent().equals(comp))
								.forEachOrdered(tw -> tmptimewindow.add(tw));
							
							if (!tmptimewindow.isEmpty()) {
								tmptimewindow.stream().forEachOrdered(tw -> {
									Trace trace = sacData.createTrace();
									double max = Math.max(trace.getMaxValue(), Math.abs(trace.getMinValue()));
									
									try {
										for (int i=0; i<trace.getLength()/4.; i++) {	//TODO 4で割るのは？？
											Files.write(outPath, (String.valueOf(trace.getX()[4*i])
													+" "
													+ String.valueOf(trace.getY()[4*i] / max)
													+ "\n").getBytes()
													, StandardOpenOption.APPEND);
										}
									} catch (IOException e) {e.printStackTrace();}
									
									int index = (int) (sacData.getValue(SACHeaderEnum.GCARC) / distanceIncrement);
									if (sacfilename.getComponent().equals(comp)) {
										if (stacksT[index] == null) {
											stacksT[index] = new ArrayRealVector(trace.getYVector().mapDivide(max));
										} else {
											for (int i = 0; i < Math.min(stacksT[index].getDimension(), trace.getLength()); i++)
												stacksT[index].addToEntry(i, trace.getY()[i] / max);
										}
										numOFStack[index] += 1;
									}
								});
								
								String color;
								if (sacfilename.getComponent().equals(SACComponent.T))
									color = "black";
								else
									color = "red";
								
								Files.write(outpathscript, ("\'"
										+ outPath.toString()
										+ "\'"
										+ "u ($1-"
										+ String.format("%.2f", reducedV * distance)
										+ "):($2+"
										+ String.format("%.2f", distance)
										+ ") "
										+ "w lines lc rgb \""
										+ color
										+ "\" lw .2,\\"
										+ "\n").getBytes()
										, StandardOpenOption.APPEND);
							
							
								if (isMap) {
									if (!gcmtID.contains(sacfilename.getGlobalCMTID())) {
										gmtHdrString[0] += GMTMap.psxy(Symbol.STAR
												, .2
												, new HorizontalPosition(sacfilename.readHeader().getValue(SACHeaderEnum.EVLA)
														, sacfilename.readHeader().getValue(SACHeaderEnum.EVLO)), "-Gred -Wthin,black")
													+ "\n";
										gcmtID.add(sacfilename.getGlobalCMTID());
									}
									if (!stations.contains(sacfilename.read().getStation())) {
										gmtHdrString[0] += GMTMap.psxy(Symbol.INVERTED_TRIANGLE
												, .2
												, new HorizontalPosition(sacfilename.readHeader().getValue(SACHeaderEnum.STLA)
														, sacfilename.readHeader().getValue(SACHeaderEnum.STLO)), "-Gblue -Wthin,black") + "\n";
										stations.add(sacfilename.read().getStation());
									}
								}
								
							}
						} catch (IOException e) {e.printStackTrace();}
					});
				
				gmtHdrString[0] += gmtMap.psEnd();
				Set<PosixFilePermission> perms =
				         EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE
				        		 , PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ);
				Files.deleteIfExists(outpathgmtmap);
				Files.createFile(outpathgmtmap, PosixFilePermissions.asFileAttribute(perms));
				Files.write(outpathgmtmap, gmtHdrString[0].getBytes());
				
				for (int i=0; i < numOFStack.length; i++) {
					if (numOFStack[i] != 0) {
						Path outpathstack = Paths.get(dir.getAbsolutePath(), "stack" + String.format("%.1f", i * distanceIncrement) + ".T.txt");
						Files.deleteIfExists(outpathstack);
						Files.createFile(outpathstack);
						for (int j=0; j < stacksT[i].getDimension(); j++)
							Files.write(outpathstack
									, (String.valueOf(j * dt)
											+ " "
											+ String.valueOf(stacksT[i].getEntry(j) / numOFStack[i])
											+ "\n").getBytes()
									, StandardOpenOption.APPEND);					
							Files.write(outpathscriptstack, ("\'"
								+ "stack" + i * distanceIncrement + ".T.txt"
								+ "\'"
								+ " u 1:($2+"
								+ String.valueOf(i * distanceIncrement)
								+ ") "
								+ "w lines lc rgb \"black\" "
								+ "lw .2,\\"
								+ "\n").getBytes()
								, StandardOpenOption.APPEND);
					}
				}
			}
			
		} catch (IOException e){e.printStackTrace();}; 
	}
	
	public static void plotScriptInitialize (Path outPathPltScript, Path gmtMapScript, Path outStackScript,
			int minlat2, int maxlat2, int minlon2, int maxlon2, File dir) {
		try {
			Files.write(outPathPltScript, String.join("\n", "set terminal postscript enhanced color font 'Helvetica,18'"
					,"unset key"
					,"set size .5,1" 
					,"set xlabel r'T - " + String.valueOf(reducedV) + "D (s)'"
					,"set ylabel 'distance (deg)'"
					,"set output 'profile" + String.format("%.1f", AZref) + ".eps'\np").getBytes()
					, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			Files.write(outStackScript, String.join("\n", "set terminal postscript enhanced color font 'Helvetica,18'"
					,"unset key"
					,"set size .5,1"
					,"set xlabel r'T - " + String.valueOf(reducedV) + "D (s)'"
					,"set ylabel 'distance (deg)'"
					,"set output 'stackprofile" + dir.toString() + String.format("-%.1f", AZref) + ".eps'\np").getBytes()
					, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void gnuplotScriptInitialize () {
		
	}
	
	public static List<SACFileName> readDir(File dir, SACExtension[] components) {
		List<SACFileName> sacfilenames = new ArrayList<SACFileName>();
		
			for (SACExtension component : components) {
				Arrays.stream(dir.listFiles((f, n) -> n.endsWith(component.toString())))
				.forEach((file) -> sacfilenames.add(new SACFileName(file.toPath())));
			}
//		sacfilenames.stream().forEach(n -> System.out.println(n.toString()));
		return sacfilenames;
	}
	
	public static boolean isSameAzimuth(SACFileName sacfilename, double deltaAZ, double AZref) {
		double AZ = 0;
		try {
			AZ = sacfilename.readHeader().getValue(SACHeaderEnum.AZ);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (AZ <= ((AZref + deltaAZ) % 360) && AZ >= ((AZref - deltaAZ) % 360)) {
			return true;
		}
		else
			return false;
	}

}
