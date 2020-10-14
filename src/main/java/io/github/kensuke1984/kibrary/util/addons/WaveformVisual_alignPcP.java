package io.github.kensuke1984.kibrary.util.addons;

import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;

public class WaveformVisual_alignPcP {

	public static void main(String[] args) throws IOException, TauModelException {
		Path waveformPath = Paths.get(args[0]);
		Path waveformIDPath = Paths.get(args[1]);
		
		BasicID[] ids = BasicIDFile.read(waveformIDPath, waveformPath);
		
		Set<GlobalCMTID> events = Stream.of(ids).map(id -> id.getGlobalCMTID()).collect(Collectors.toSet());
		
		String tmpString = Utilities.getTemporaryString();
		
		Path stackDir = Paths.get("stack" + tmpString);
		Path profileDir = Paths.get("profile" + tmpString);
		Path mapPath = stackDir.resolve("map");
		Files.createDirectory(stackDir);
		Files.createDirectory(profileDir);
		Files.createDirectory(mapPath);
		
		SACComponent[] components = new SACComponent[] {SACComponent.T, SACComponent.R, SACComponent.Z};
		
		double dt = 1./ ids[0].getSamplingHz();
		
		int dAz = 5;
		int nAz = 360 / dAz;
		
		int dEd = 5;
		int nEd = 180 / dEd;
		
		TauP_Time timetool = new TauP_Time("prem");
		timetool.parsePhaseList("PcP");
		
		for (GlobalCMTID event : events) {
			Path eventDir = stackDir.resolve(event.toString());
			Files.createDirectory(eventDir);
			
			Path profileEventDir = profileDir.resolve(event.toString());
			Files.createDirectory(profileEventDir);
			
			timetool.setSourceDepth(6371. - event.getEvent().getCmtLocation().getR());
		
			for (SACComponent component : components) {
				double[][] obsStack = new double[180][0];
				double[][] synStack = new double[180][0];
				double[][][] obsAzimuthStack = new double[nAz][180][0];
				double[][][] synAzimuthStack = new double[nAz][180][0];
				
				double[][][] obsAzimuthSectionStack = new double[nEd][360][0];
				double[][][] synAzimuthSectionStack = new double[nEd][360][0];
				
				List<List<Station>> stationAzimuthList = new ArrayList<>();
				for (int i = 0; i < nAz; i++)
					stationAzimuthList.add(new ArrayList<>());
				
				List<List<Station>> stationDistanceList = new ArrayList<>();
				for (int i = 0; i < nEd; i++)
					stationDistanceList.add(new ArrayList<>());
				
				Path plotProfilePath = profileEventDir.resolve(Paths.get("plot_" + component + ".plt"));
				PrintWriter pwPlot = new PrintWriter(plotProfilePath.toFile());
				pwPlot.println("set terminal postscript enhanced color font \"Helvetica,12\"");
				pwPlot.println("set output \"" + event + "." + component + ".ps\"");
				pwPlot.println("unset key");
				pwPlot.println("#set yrange [63:102]"); 
				pwPlot.println("set xlabel 'Time aligned on S-wave arrival (s)'");
				pwPlot.println("set ylabel 'Distance (deg)'");
				pwPlot.println("set size .5,1");
				pwPlot.print("p ");
				
				String[] azimuthSectionString = new String[nEd];
				for (int ied = 0; ied < nEd; ied++) {
					azimuthSectionString[ied] = "set terminal postscript enhanced color font \"Helvetica,12\"\n"
							+ "set output \"" + event + ".ed" + (int) (ied * dEd) + "." + component + ".ps\"\n"
							+ "unset key\n"
							+ "set xlabel 'Time aligned on S-wave arrival (s)'\n"
							+ "set ylabel 'Azimuth (deg)'\n"
							+ "set size .5,1\n"
							+ "p ";
				}
				
				Set<BasicID> thisObsIDs = Arrays.stream(ids)
					.filter(id -> id.getGlobalCMTID().equals(event) && id.getSacComponent() == component)
					.filter(id -> id.getWaveformType().equals(WaveformType.OBS))
					.collect(Collectors.toSet());
				Set<BasicID> thisSynIDs = Arrays.stream(ids)
						.filter(id -> id.getGlobalCMTID().equals(event) && id.getSacComponent() == component)
						.filter(id -> id.getWaveformType().equals(WaveformType.SYN))
						.collect(Collectors.toSet());
				
				for (BasicID id : thisObsIDs) {
					if (!id.getGlobalCMTID().equals(event) || id.getSacComponent() != component)
						continue;
					
					double distance = Math.toDegrees(id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(id.getStation().getPosition()));
					int k = (int) distance;
					int ked = (int) (distance / dEd);
					
					double azimuth = Math.toDegrees(id.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(id.getStation().getPosition()));
					int kaz = (int) (azimuth / dAz);
					int kazsec = (int) (azimuth);
					
					timetool.calculate(distance);
					if (timetool.getNumArrivals() != 1)
						continue;
					double timeScS = timetool.getArrival(0).getTime();
					
					BasicID synID = thisSynIDs.stream().filter(syn -> syn.getGlobalCMTID().equals(id.getGlobalCMTID()) 
							&& syn.getStation().equals(id.getStation()) && id.getSacComponent().equals(syn.getSacComponent()))
							.findFirst().get();
					
					Trace synTrace = synID.getTrace().shiftX(-timeScS);
					
					Trace trace = id.getTrace().shiftX(-timeScS + (synID.getStartTime() - id.getStartTime()));
					
					obsStack[k] = add(obsStack[k], id.getData());
					obsAzimuthStack[kaz][k] = add(obsAzimuthStack[kaz][k], id.getData());
					obsAzimuthSectionStack[ked][kazsec] = add(obsAzimuthSectionStack[ked][kazsec], id.getData());
					
					List<Station> tmpList = stationAzimuthList.get(kaz);
					tmpList.add(id.getStation());
					stationAzimuthList.set(kaz, tmpList);
					
					tmpList = stationDistanceList.get(ked);
					tmpList.add(id.getStation());
					stationDistanceList.set(ked, tmpList);
						
					synStack[k] = add(synStack[k], synID.getData());
					synAzimuthStack[kaz][k] = add(synAzimuthStack[kaz][k], synID.getData());
					synAzimuthSectionStack[ked][kazsec] = add(synAzimuthSectionStack[ked][kazsec], synID.getData());
					
					String filename = id.getStation() + "." + event.toString() + "." + component + "." + id.getWaveformType() + ".txt";
					Path outpath = profileEventDir.resolve(filename);
					PrintWriter pw = new PrintWriter(outpath.toFile());
					for (int i = 0; i < trace.getLength(); i++)
						pw.println(trace.getXAt(i) + " " + trace.getYAt(i));
					pw.close();
					
					String filenameSyn = synID.getStation() + "." + event.toString() + "." + component + "." + synID.getWaveformType() + ".txt";
					Path outpathSyn = profileEventDir.resolve(filenameSyn);
					PrintWriter pwSyn = new PrintWriter(outpathSyn.toFile());
					for (int i = 0; i < synTrace.getLength(); i++)
						pwSyn.println(synTrace.getXAt(i) + " " + synTrace.getYAt(i));
					pwSyn.close();
					
					double max = trace.getYVector().getLInfNorm();
					
					pwPlot.println("'" + filenameSyn + "' u 1:($2/" + max + "+" + distance + ") w l lw .5 lt 1 lc rgb 'red',\\");
					pwPlot.println("'" + filename + "' u 1:($2/" + max + "+" + distance + ") w l lw .5 lt 1 lc rgb 'black',\\");
				
					azimuthSectionString[ked] += "'" + filenameSyn + "' u 1:($2/" + max + "+" + azimuth + ") w l lt 1 lw .5 lc rgb 'red',\\\n";
					azimuthSectionString[ked] += "'" + filename + "' u 1:($2/" + max + "+" + azimuth + ") w l lt 1 lw .5 lc rgb 'black',\\\n";
				}
				pwPlot.close();
				
				//azimuth profile
				for (int ied = 0; ied < nEd; ied++) {
					Path outpath = profileEventDir.resolve(Paths.get("plot_ed" + (int) (ied * dEd) + "." + component + ".plt"));
					PrintWriter pw = new PrintWriter(outpath.toFile());
					pw.print(azimuthSectionString[ied]);
					pw.close();
				}
				
				//full stacks
				Path plotPath = eventDir.resolve(Paths.get("plot_" + component + ".plt"));
				pwPlot = new PrintWriter(plotPath.toFile());
				pwPlot.println("set terminal postscript enhanced color font \"Helvetica,12\"");
				pwPlot.println("set output \"" + event + "." + component + ".ps\"");
				pwPlot.println("unset key");
				pwPlot.println("#set yrange [63:102]"); 
				pwPlot.println("set xlabel 'Time aligned on S-wave arrival (s)'");
				pwPlot.println("set ylabel 'Distance (deg)'");
				pwPlot.println("set size .5,1");
				pwPlot.print("p ");
				
				for (int i = 0; i < obsStack.length; i++) {
					if (obsStack[i].length == 0)
						continue;
					
					double max = new ArrayRealVector(obsStack[i]).getLInfNorm();
					
					String filename = i + "." + event.toString() + "." + component + ".txt";
					Path outpath = eventDir.resolve(filename);
					PrintWriter pw = new PrintWriter(outpath.toFile());
					for (int j = 0; j < obsStack[i].length; j++)
						pw.println((j * dt) + " " + (synStack[i][j] / max) + " " + (obsStack[i][j] / max));
					pw.close();
					
					pwPlot.println("'" + filename + "' u 1:($2+" + i + ") w l lt 1 lc rgb 'red',\\");
					pwPlot.println("'" + filename + "' u 1:($3+" + i + ") w l lt 1 lc rgb 'black',\\");
				}
				
				pwPlot.close();
				
				//azimuth stacks
				for (int iaz = 0; iaz < nAz; iaz++) {
					if (stationAzimuthList.get(iaz).size() > 0) {
						writeGMT(mapPath, event, stationAzimuthList.get(iaz), "az", (int) (iaz * dAz));
					
						plotPath = eventDir.resolve(Paths.get("plot_az" + (iaz * dAz) + "_" + component + ".plt"));
						pwPlot = new PrintWriter(plotPath.toFile());
						pwPlot.println("set terminal postscript enhanced color font \"Helvetica,12\"");
						pwPlot.println("set output \"" + event + ".az" + (iaz * dAz) + "." + component + ".ps\"");
						pwPlot.println("unset key");
						pwPlot.println("#set yrange [63:102]"); 
						pwPlot.println("set xlabel 'Time aligned on S-wave arrival (s)'");
						pwPlot.println("set ylabel 'Distance (deg)'");
						pwPlot.println("set size .5,1");
						pwPlot.print("p ");
					
						for (int i = 0; i < obsAzimuthStack[iaz].length; i++) {
							if (obsAzimuthStack[iaz][i].length == 0)
								continue;
							
							double max = new ArrayRealVector(obsAzimuthStack[iaz][i]).getLInfNorm();
							
							String filename = i + ".az" + (iaz * dAz) + "." + event.toString() + "." + component + ".txt";
							Path outpath = eventDir.resolve(filename);
							PrintWriter pw = new PrintWriter(outpath.toFile());
							for (int j = 0; j < obsAzimuthStack[iaz][i].length; j++)
								pw.println((j * dt) + " " + (synAzimuthStack[iaz][i][j] / max) + " " + (obsAzimuthStack[iaz][i][j] / max));
							pw.close();
							
							pwPlot.println("'" + filename + "' u 1:($2+" + i + ") w l lt 1 lc rgb 'red',\\");
							pwPlot.println("'" + filename + "' u 1:($3+" + i + ") w l lt 1 lc rgb 'black',\\");
						}
						
						pwPlot.close();
					}
				}
				
				//azimuth-distance stack azimuth profile
				for (int ied = 0; ied < nEd; ied++) {
					if (stationDistanceList.get(ied).size() > 0) {
						writeGMT(mapPath, event, stationDistanceList.get(ied), "ed", (int) (ied * dEd));
					
						plotPath = eventDir.resolve(Paths.get("plot_ed" + (ied * dEd) + "_" + component + ".plt"));
						pwPlot = new PrintWriter(plotPath.toFile());
						pwPlot.println("set terminal postscript enhanced color font \"Helvetica,12\"");
						pwPlot.println("set output \"" + event + ".ed" + (ied * dEd) + "." + component + ".ps\"");
						pwPlot.println("unset key");
						pwPlot.println("#set yrange [63:102]"); 
						pwPlot.println("set xlabel 'Time aligned on S-wave arrival (s)'");
						pwPlot.println("set ylabel 'Azimuth (deg)'");
						pwPlot.println("set size .5,1");
						pwPlot.print("p ");
					
						for (int i = 0; i < obsAzimuthSectionStack[ied].length; i++) {
							if (obsAzimuthSectionStack[ied][i].length == 0)
								continue;
							
							double max = new ArrayRealVector(obsAzimuthSectionStack[ied][i]).getLInfNorm();
							
							String filename = i + ".ed" + (ied * dEd) + "." + event.toString() + "." + component + ".txt";
							Path outpath = eventDir.resolve(filename);
							PrintWriter pw = new PrintWriter(outpath.toFile());
							for (int j = 0; j < obsAzimuthSectionStack[ied][i].length; j++)
								pw.println((j * dt) + " " + (synAzimuthSectionStack[ied][i][j] / max) + " " + (obsAzimuthSectionStack[ied][i][j] / max));
							pw.close();
							
							pwPlot.println("'" + filename + "' u 1:($2+" + i + ") w l lt 1 lc rgb 'red',\\");
							pwPlot.println("'" + filename + "' u 1:($3+" + i + ") w l lt 1 lc rgb 'black',\\");
						}
						
						pwPlot.close();
					}
				}
				
			}
		}
	}
	
	
	private static double[] add(double[] y1, double[] y2) {
		if (y1.length == 0)
			return y2;
		else if (y2.length == 0)
			return y1;
		else {
			int n = Math.min(y1.length, y2.length);
			double[] tmp = new double[n];
			for (int i = 0; i < n; i++)
				tmp[i] = y1[i] + y2[i];
			return tmp;
		}
	}
	
	private static void writeGMT(Path rootpath, GlobalCMTID event, List<Station> stations, String name, int id) throws IOException {
		Path outpath = rootpath.resolve("plot_map_" + event + "_" + name + id + ".gmt");
		String outpathps = "map_" + event + "_az" + name + id + ".ps";
		PrintWriter pw = new PrintWriter(outpath.toFile());
		
		String ss = String.join("\n",
				"#!/bin/sh"
				, "outputps=" + outpathps
				, "gmt set FONT 12p,Helvetica,black"
				, "gmt set PS_MEDIA 5ix2.5i"
				, "gmt set MAP_FRAME_WIDTH 2p"
//				, "gmt pscoast -R-170/-52/-41/75 -JQ270/4.4i -P -G205 -K -Di -X.4i -Y.3i -BWSne -Bx60g30f30 -By60g30f30 > $outputps"
				, "gmt pscoast -Rg -JW4i -P -G205 -K -Di -X.4i -Y.3i -BWSne -Bx60g30f30 -By60g30f30 > $outputps"
				, ""
				);
		
		ss += "gmt psxy -Rg -JW4i -Wthinner,red -t0 -K -O >> $outputps <<END\n";
		double evLat = event.getEvent().getCmtLocation().getLatitude();
		double evLon = event.getEvent().getCmtLocation().getLongitude();
		for (Station station : stations)
			ss += String.format(">\n%.2f %.2f\n%.2f %.2f\n", evLon, evLat, station.getPosition().getLongitude(), station.getPosition().getLatitude());
		ss += "END\n";
		
		ss += "gmt psxy -R -J -Si0.11 -P -Groyalblue -Wthinnest,black -K -O >> $outputps <<END\n";
		for (Station station : stations)
			ss += String.format("%.2f %.2f\n", station.getPosition().getLongitude(), station.getPosition().getLatitude());
		ss += "END\n";
		
		ss += "gmt psxy -R -J -P -Sa0.22 -Gred -Wthinnest,black -K -O >> $outputps<<END\n";
		ss += String.format("%.2f %.2f\n", evLon, evLat);
		ss += "END\n";
		
		ss += "gmt pstext -R -J -P -O >> $outputps <<END\n"
			 + "END\n"
			 + "gmt ps2raster $outputps -Tf\n";
		
		pw.println(ss);
		
		pw.close();
	}
}
