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

import io.github.kensuke1984.kibrary.util.sac.SACExtension;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
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
 * @author Yuki
 *
 */
public class RecordSectionSac {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IllegalArgumentException {
		if (args.length < 2) {
			System.err.println("usage: path to SAC directory, path to timewindowInformationFile");
			throw new IllegalArgumentException();
		}
		try {
			File dirs[] = new File[args.length - 1];
			for (int i=0; i < args.length - 1; i++) {
				dirs[i] = new File(args[i]);
			}
			Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(Paths.get(args[args.length - 1]));
//			System.out.println(timewindows.size());
			double reducedVelocity = 8.4;
			double freqL = 0.008;
			double freqH = 0.125;
			boolean isFilter = false;
			double deltaAZ = 3;
			double AZref = 336;
			boolean isMap = false;
			double dt = 0.05;
			boolean isStack = false;
			
			for (File dir : dirs) {
				double[] distanceOfMaxR = {360.};
				double[] distanceOfMaxT = {100.};
				double[] maxR = {0.};
				double[] maxT = {0.};
				double distanceIncrement = 1;
				RealVector[] stacksT = new RealVector[(int) (120/distanceIncrement)];
				RealVector[] stacksR = new RealVector[(int) (120/distanceIncrement)];
				int[] numberOfStack = new int[(int) (120/distanceIncrement)];
//				System.out.println(dir.getName());
				Set<TimewindowInformation> thisEventTimewindows = new HashSet<>();
				timewindows.stream()
						.filter(tw -> tw.getGlobalCMTID().toString().equals(dir.getName()))
						.forEach(tw -> thisEventTimewindows.add(tw));
//				System.out.println(thisEventTimewindows.size());
				GMTMap gmtmap = new GMTMap(String.valueOf(AZref - deltaAZ) + "˚ < AZ < " + String.valueOf(AZref + deltaAZ) + "˚", 120, -30, 30, 85);
				String[] gmtString = {String.format("#!/bin/sh\npsname=\"map%.1f.ps\"\n", AZref) + gmtmap.psStart() + "\n"};
				Set<GlobalCMTID> events = new HashSet<GlobalCMTID>();
				Set<Station> stations = new HashSet<Station>();
				
				
				gmtString[0] += GMTMap.psCoast("-Gbeige") + "\n";
				
				Path outpathscript = Paths.get(dir.getAbsolutePath(), "profile.plt");
				Path outpathgmtmap = Paths.get(dir.getAbsolutePath(), "makemap.sh");
				Path outpathscriptstack = Paths.get(dir.getAbsolutePath(), "stackprofile.plt");
				try {
					Files.write(outpathscript, String.join("\n", "set terminal postscript enhanced color font 'Helvetica,18'"
							,"unset key"
							,"set size .5,1" 
							,"set xlabel r'T - " + String.valueOf(reducedVelocity) + "D (s)'"
							,"set ylabel 'distance (deg)'"
							,"set output 'profile" + String.format("%.1f", AZref) + ".eps'\np").getBytes()
							, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				SACExtension[] components = new SACExtension[] {SACExtension.Tsc, SACExtension.Rsc, SACExtension.Zsc};
//				components.
				List<SACFileName> sacfilenames = readDir(dir, components);
				sacfilenames.stream()
//				.filter(sacfilename -> isSameAzimuth(sacfilename, deltaAZ, AZref))
				.forEach(sacfilename -> {
					BandPassFilter filter = new BandPassFilter(freqH*2*Math.PI/20, freqL*2*Math.PI/20, 4);
					try {
						Path outpath = Paths.get(dir.getAbsolutePath(), sacfilename.toString() + ".txt");
//						System.out.println(outpath.toString());
						Files.write(outpath, "".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
						SACData sacdata = sacfilename.read();
//						System.out.println(sacdata.getStation());
						if (isFilter)
							sacdata.applyButterworthFilter(filter);
						double distance = sacfilename.readHeader().getValue(SACHeaderEnum.GCARC);
						List<Timewindow> tmptimewindow = new ArrayList<Timewindow>();
						
						thisEventTimewindows.stream()
								.filter(tw -> {
									boolean sameStation = false;
									try {
//										System.out.println(tw.getStation()+" "+sacfilename.readHeader().getStation());
										sameStation = tw.getStation().equals((sacfilename.readHeader().getStation()));
									} catch (IOException e) {
										e.printStackTrace();
									}
									return sameStation;
								})
								.filter(tw -> tw.getComponent().equals(SACComponent.T))
								.forEachOrdered(tw -> tmptimewindow.add(tw));
//								System.out.println("timewindow is empty? "+tmptimewindow.isEmpty());
								if (!tmptimewindow.isEmpty()) {
									tmptimewindow.stream().forEachOrdered(timewindow -> {
//										System.out.println(sacdata.getGlobalCMTID()+" "+sacdata.getStation()+" "+ timewindow);
										Trace trace = sacdata.createTrace().cutWindow(timewindow);
//										System.out.println("maxvalue is "+trace.getMaxValue());
										double max = Math.max(trace.getMaxValue(), Math.abs(trace.getMinValue()));
										System.out.println("maxvalue is "+max);
										try {
											for (int i=0; i < (int) trace.getLength()/4.; i++) {
												Files.write(outpath, (String.valueOf(trace.getX()[4*i])
														+ " "
														+ String.valueOf(trace.getY()[4*i] / max)
														+ "\n").getBytes()
														, StandardOpenOption.APPEND);
											}
										} catch (IOException e) {
											e.printStackTrace();
										}
										
										if (sacfilename.getComponent().equals(SACComponent.T)) {
											if (sacdata.getValue(SACHeaderEnum.GCARC) < distanceOfMaxT[0]) {
												maxT[0] = max;
												distanceOfMaxT[0] = sacdata.getValue(SACHeaderEnum.GCARC);
											}
										}
										else if (sacfilename.getComponent().equals(SACComponent.R)) {
											if (sacdata.getValue(SACHeaderEnum.GCARC) < distanceOfMaxR[0]) {
												maxR[0] = max;
												distanceOfMaxR[0] = sacdata.getValue(SACHeaderEnum.GCARC);
											}
										}
										
										if (isStack){
										int index = (int) (sacdata.getValue(SACHeaderEnum.GCARC) / distanceIncrement);
										if (sacfilename.getComponent().equals(SACComponent.T)) {
											if (stacksT[index] == null) {
												stacksT[index] = new ArrayRealVector(trace.getYVector().mapDivide(max));
											}
											else {
												for (int i=0; i < Math.min(stacksT[index].getDimension(), trace.getLength()); i++) {
													stacksT[index].addToEntry(i, trace.getY()[i] / max);
												}
											}
											numberOfStack[index] += 1;
										}
										else if (sacfilename.getComponent().equals(SACComponent.R)) {
											if (stacksR[index] == null) {
												stacksR[index] = new ArrayRealVector(trace.getYVector().mapDivide(max));
											}
											else {
												for (int i=0; i < Math.min(stacksR[index].getDimension(), trace.getLength()); i++) {
													stacksR[index].addToEntry(i, trace.getY()[i] / max);
												}
											}
										}
										}
									});
//									System.out.println(stacksT.length); //TODO
									
									String color;
									if (sacfilename.getComponent().equals(SACComponent.T))
										color = "black";
									else
										color = "red";
									
									Files.write(outpathscript, ("\'"
											+ outpath.toString()
											+ "\'"
											+ "u ($1-"
											+ String.format("%.2f", reducedVelocity * distance)
											+ "):($2+"
											+ String.format("%.2f", distance)
											+ ") "
											+ "w lines lt 1 lc rgb \""
											+ color
											+ "\" lw .01,\\"
											+ "\n").getBytes()
											, StandardOpenOption.APPEND);
								
								
									if (isMap) {
										if (!events.contains(sacfilename.getGlobalCMTID())) {
											gmtString[0] += GMTMap.psxy(Symbol.STAR
													, .2
													, new HorizontalPosition(sacfilename.readHeader().getValue(SACHeaderEnum.EVLA)
															, sacfilename.readHeader().getValue(SACHeaderEnum.EVLO)), "-Gred -Wthin,black")
														+ "\n";
											events.add(sacfilename.getGlobalCMTID());
										}
										if (!stations.contains(sacfilename.read().getStation())) {
											gmtString[0] += GMTMap.psxy(Symbol.INVERTED_TRIANGLE
													, .2
													, new HorizontalPosition(sacfilename.readHeader().getValue(SACHeaderEnum.STLA)
															, sacfilename.readHeader().getValue(SACHeaderEnum.STLO)), "-Gblue -Wthin,black")
														+ "\n";
											stations.add(sacfilename.read().getStation());
										}
									}
									
								}
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
				
				gmtString[0] += gmtmap.psEnd();
				Set<PosixFilePermission> perms =
				         EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE
				        		 , PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ);
				Files.deleteIfExists(outpathgmtmap);
				Files.createFile(outpathgmtmap, PosixFilePermissions.asFileAttribute(perms));
				Files.write(outpathgmtmap, gmtString[0].getBytes());
				
//				if (distanceOfMaxR[0] != distanceOfMaxT[0])
//					System.err.println(String.format("Error: R and T amplitude taken at different epicentral distances %.1f %.1f"
//							, distanceOfMaxR[0], distanceOfMaxT[0]));
				Files.write(outpathscriptstack, String.join("\n", "set terminal postscript enhanced color font 'Helvetica,18'"
						,"unset key"
						,"set size .5,1"
						,"set xlabel r'T - " + String.valueOf(reducedVelocity) + "D (s)'"
						,"set ylabel 'distance (deg)'"
//						,String.format("set title 'Amplitude T/R = %.2f'", maxT[0] / maxR[0])
						,"set output 'stackprofile " + dir.toString() + String.format("-%.1f", AZref) + ".eps'\np").getBytes()
						, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				
				for (int i=0; i < numberOfStack.length; i++) {
					if (numberOfStack[i] != 0) {
						Path outpathstack = Paths.get(dir.getAbsolutePath(), "stack" + String.format("%.1f", i * distanceIncrement) + ".T.txt");
						Files.deleteIfExists(outpathstack);
						Files.createFile(outpathstack);
						for (int j=0; j < stacksT[i].getDimension(); j++)
							Files.write(outpathstack
									, (String.valueOf(j * dt)
											+ " "
											+ String.valueOf(stacksT[i].getEntry(j) / numberOfStack[i])
											+ "\n").getBytes()
									, StandardOpenOption.APPEND);
						
						outpathstack = Paths.get(dir.getAbsolutePath(), "stack" + String.format("%.1f", i * distanceIncrement) + ".R.txt");
						Files.deleteIfExists(outpathstack);
						Files.createFile(outpathstack);
						for (int j=0; j < stacksR[i].getDimension(); j++)
							Files.write(outpathstack
									, (String.valueOf(j * dt)
											+ " "
											+ String.valueOf(stacksR[i].getEntry(j) / numberOfStack[i])
											+ "\n").getBytes()
									, StandardOpenOption.APPEND);
						
						Files.write(outpathscriptstack, ("\'"
								+ "stack" + i * distanceIncrement + ".T.txt"
								+ "\'"
								+ " u 1:($2+"
								+ String.valueOf(i * distanceIncrement)
								+ ") "
								+ "w lines lt 1 lc rgb \"black\" "
								+ "lw .01,\\"
								+ "\n").getBytes()
								, StandardOpenOption.APPEND);
//						Files.write(outpathscriptstack, ("\'"
//								+ "stack" + i * distanceIncrement + ".R.txt"
//								+ "\'"
//								+ " u 1:($2+"
//								+ String.valueOf(i * distanceIncrement)
//								+ ") "
//								+ "w lines lc rgb \"red\" "
//								+ "lw .2,\\"
//								+ "\n").getBytes()
//								, StandardOpenOption.APPEND);
					}
				}
				
				Files.write(outpathscriptstack, "''".getBytes(), StandardOpenOption.APPEND);
				Files.write(outpathscript, "''".getBytes(), StandardOpenOption.APPEND);
			}
		}catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public static List<SACFileName> readDir(File dir, SACExtension[] components) {
		List<SACFileName> sacfilenames = new ArrayList<SACFileName>();
		
			for (SACExtension component : components) {
				Arrays.stream(dir.listFiles((f, n) -> n.endsWith(component.toString())))
				.forEach((file) -> sacfilenames.add(new SACFileName(file.toPath())));
			}		
		return sacfilenames;
	}
	
//	public static List<SACFileName> readDir(File dir, SACExtension component) {
//	List<SACFileName> sacfilenames = new ArrayList<SACFileName>();			
//		for (SACExtension component : components) {
//			Arrays.stream(dir.listFiles((f, n) -> n.endsWith(component.toString())))
//			.forEach((file) -> sacfilenames.add(new SACFileName(file.toPath())));
//		}
	
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
	

//	public static 
}
