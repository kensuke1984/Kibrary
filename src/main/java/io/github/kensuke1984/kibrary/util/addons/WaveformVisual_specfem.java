package io.github.kensuke1984.kibrary.util.addons;

import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.inversion.addons.WeightingType;
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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class WaveformVisual_specfem {

	public static void main(String[] args) throws IOException {
		Path waveformIDinitPath = Paths.get(args[0]);
		Path waveforminitPath = Paths.get(args[1]);
		Path waveformID3DPath = Paths.get(args[2]);
		Path waveform3DPath = Paths.get(args[3]);
		
		Dvector dvectorInit = new Dvector(BasicIDFile.read(waveformIDinitPath, waveforminitPath), id -> true, WeightingType.RECIPROCAL);
		Dvector dvector3D = new Dvector(BasicIDFile.read(waveformID3DPath, waveform3DPath), id -> true, WeightingType.RECIPROCAL);
		
		List<BasicID> initSynListtmp = Arrays.stream(dvectorInit.getSynIDs()).collect(Collectors.toList());
		List<BasicID> specfemSynList = Arrays.stream(dvector3D.getSynIDs()).collect(Collectors.toList());
		List<BasicID> obsList = Arrays.stream(dvector3D.getObsIDs()).collect(Collectors.toList());
		
		List<BasicID> initSynList = new ArrayList<>();
		for (BasicID id : obsList) {
			BasicID idtmp = initSynListtmp.parallelStream().filter(id2 -> id2.getGlobalCMTID().equals(id.getGlobalCMTID())
					&& id2.getStation().equals(id.getStation())).findFirst().get();
			initSynList.add(idtmp);
		}
		
		Set<GlobalCMTID> events = obsList.stream().map(id -> id.getGlobalCMTID()).collect(Collectors.toSet());
		
		String tmpString = Utilities.getTemporaryString();
		
		Path stackDir = Paths.get("stack" + tmpString);
		Path profileDir = Paths.get("profile" + tmpString);
		Path mapPath = stackDir.resolve("map");
		Files.createDirectory(stackDir);
		Files.createDirectory(profileDir);
		Files.createDirectory(mapPath);
		
		SACComponent[] components = new SACComponent[] {SACComponent.T, SACComponent.R, SACComponent.Z};
		
		double dt = 1./ obsList.get(0).getSamplingHz();
		
		int dAz = 5;
		int nAz = 360 / dAz;
		
		String eachMisfitString = "";
		
		for (GlobalCMTID event : events) {
			Path eventDir = stackDir.resolve(event.toString());
			Files.createDirectory(eventDir);
			
			Path profileEventDir = profileDir.resolve(event.toString());
			Files.createDirectory(profileEventDir);
		
			for (SACComponent component : components) {
				double[][] obsStack = new double[120][0];
				double[][] synStack = new double[120][0];
				double[][] synSpecfemStack = new double[120][0];
				double[][][] obsAzimuthStack = new double[nAz][120][0];
				double[][][] synAzimuthStack = new double[nAz][120][0];
				double[][][] synSpecfemAzimuthStack = new double[nAz][120][0];
				List<List<Station>> stationAzimuthList = new ArrayList<>();
				for (int i = 0; i < nAz; i++)
					stationAzimuthList.add(new ArrayList<>());
				
				
				Path plotProfilePath = profileEventDir.resolve(Paths.get("plot_" + component + ".plt"));
				PrintWriter pwPlot = new PrintWriter(plotProfilePath.toFile());
				pwPlot.println("set terminal postscript enhanced color font \"Helvetica,14\"");
				pwPlot.println("set output \"" + event + "." + component + ".ps\"");
				pwPlot.println("unset key");
				pwPlot.println("#set yrange [63:102]"); 
				pwPlot.println("set xlabel 'Time aligned on S-wave arrival (s)'");
				pwPlot.println("set ylabel 'Distance (deg)'");
				pwPlot.println("set xtics 10");
				pwPlot.println("set ytics 5");
				pwPlot.println("set size .5,1");
				pwPlot.print("p ");
				
				for (int i = 0; i < obsList.size(); i++) {
					BasicID id = obsList.get(i);
					BasicID initID = initSynList.get(i);
					BasicID specfemID = specfemSynList.get(i);
					
//					if (id.getGlobalCMTID().equals(initID.getGlobalCMTID()) || id.getStation() != initID.getStation()) {
//						pwPlot.close();
//						throw new RuntimeException("obsID and synInitID differ " + id + " " + initID);
//					}
//					if (id.getGlobalCMTID() != specfemID.getGlobalCMTID() || id.getStation() != specfemID.getStation()) {
//						pwPlot.close();
//						throw new RuntimeException("obsID and specfemID differ " + id + " " + specfemID);
//					}
					
					if (!id.getGlobalCMTID().equals(event) || id.getSacComponent() != component)
						continue;
					
					double distance = Math.toDegrees(id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(id.getStation().getPosition()));
					int k = (int) distance;
					
					double azimuth = Math.toDegrees(id.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(id.getStation().getPosition()));
					int kaz = (int) (azimuth / dAz);
					
					double maxObs = new ArrayRealVector(id.getData()).getLInfNorm();
					double[] obsData = new ArrayRealVector(id.getData()).mapMultiply(1. / maxObs).toArray();
					double[] synData = new ArrayRealVector(initID.getData()).mapMultiply(1. / maxObs).toArray();
					double[] specfemData = new ArrayRealVector(specfemID.getData()).mapMultiply(1. / maxObs).toArray();
					
					obsStack[k] = add(obsStack[k], obsData);
					obsAzimuthStack[kaz][k] = add(obsAzimuthStack[kaz][k], obsData);
					
					List<Station> tmpList = stationAzimuthList.get(kaz);
					tmpList.add(id.getStation());
					stationAzimuthList.set(kaz, tmpList);
					
					synStack[k] = add(synStack[k], synData);
					synAzimuthStack[kaz][k] = add(synAzimuthStack[kaz][k], synData);
					
					synSpecfemStack[k] = add(synSpecfemStack[k], specfemData);
					synSpecfemAzimuthStack[kaz][k] = add(synSpecfemAzimuthStack[kaz][k], specfemData);
					
					String filename = id.getStation() + "." + event.toString() + "." + component + ".obs.txt";
					Path outpath = profileEventDir.resolve(filename);
					PrintWriter pw = new PrintWriter(outpath.toFile());
					Trace trace = id.getTrace();
					for (int j = 0; j < trace.getLength(); j++)
						pw.println(trace.getXAt(j) + " " + trace.getYAt(j));
					pw.close();
					
					String filenameInit = initID.getStation() + "." + event.toString() + "." + component + ".initSyn.txt";
					Path outpathInit = profileEventDir.resolve(filenameInit);
					pw = new PrintWriter(outpathInit.toFile());
					trace = initID.getTrace();
					for (int j = 0; j < trace.getLength(); j++)
						pw.println(trace.getXAt(j) + " " + trace.getYAt(j));
					pw.close();
					
					String filenameSpecfem = specfemID.getStation() + "." + event.toString() + "." + component + ".specfemSyn.txt";
					Path outpathSpecfem = profileEventDir.resolve(filenameSpecfem);
					pw = new PrintWriter(outpathSpecfem.toFile());
					trace = specfemID.getTrace();
					for (int j = 0; j < trace.getLength(); j++)
						pw.println(trace.getXAt(j) + " " + trace.getYAt(j));
					pw.close();
					
					double max = id.getTrace().getYVector().getLInfNorm();
					pwPlot.println("'" + filename + "' u 0:($2/" + max*2 + "+" + distance + ") w l lw .5 lc rgb 'red',\\");
					pwPlot.println("'" + filenameInit + "' u 0:($2/" + max*2 + "+" + distance + ") w l lw .5 lc rgb 'black',\\");
					pwPlot.println("'" + filenameSpecfem + "' u 0:($2/" + max*2 + "+" + distance + ") w l lw .5 lc rgb 'blue',\\");
					
					//write variance and misfit
					RealVector obsVector = new ArrayRealVector(id.getData());
					RealVector synVector = new ArrayRealVector(initID.getData());
					RealVector specfemVector = new ArrayRealVector(specfemID.getData());
					double tmpSynVariance = synVector.subtract(obsVector).dotProduct(synVector.subtract(obsVector));
					double tmpSpecfemVariance = specfemVector.subtract(obsVector).dotProduct(specfemVector.subtract(obsVector));
					double tmpObsNorm = obsVector.dotProduct(obsVector);
					
					double tmpSyn = tmpSynVariance / tmpObsNorm;
					double tmpSpecfem =  tmpSpecfemVariance / tmpObsNorm;
					
					double synCorr = synVector.dotProduct(obsVector) / (synVector.getNorm() * obsVector.getNorm());
					double bornCorr = specfemVector.dotProduct(obsVector) / (specfemVector.getNorm() * obsVector.getNorm());
					double synRatio = synVector.getLInfNorm() / obsVector.getLInfNorm();
					double bornRatio = specfemVector.getLInfNorm() / obsVector.getLInfNorm();
					eachMisfitString += id.getStation().getName() + " " + id.getStation().getNetwork() + " " + id.getStation().getPosition() + " "
							+ id.getGlobalCMTID() + " " + id.getSacComponent() + " " + (new Phases(id.getPhases())) + " " + synRatio + " " + bornRatio + " "
							+ tmpSyn + " " + tmpSpecfem + " " + synCorr + " " + bornCorr + "\n";
				}
				pwPlot.close();
				
				//full stacks
				Path plotPath = eventDir.resolve(Paths.get("plot_" + component + ".plt"));
				pwPlot = new PrintWriter(plotPath.toFile());
				pwPlot.println("set terminal postscript enhanced color font \"Helvetica,14\"");
				pwPlot.println("set output \"" + event + "." + component + ".ps\"");
				pwPlot.println("unset key");
				pwPlot.println("#set yrange [63:102]"); 
				pwPlot.println("set xlabel 'Time aligned on S-wave arrival (s)'");
				pwPlot.println("set ylabel 'Distance (deg)'");
				pwPlot.println("set xtics 10");
				pwPlot.println("set ytics 5");
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
						pw.println((j * dt) + " " + (synStack[i][j] / max / 2) + " " + (synSpecfemStack[i][j] / max/ 2) + " " + (obsStack[i][j] / max/ 2));
					pw.close();
					
					pwPlot.println("'" + filename + "' u 1:($2+" + i + ") w l lc rgb 'red',\\");
					pwPlot.println("'" + filename + "' u 1:($3+" + i + ") w l lc rgb 'blue',\\");
					pwPlot.println("'" + filename + "' u 1:($4+" + i + ") w l lc rgb 'black',\\");
				}
				
				pwPlot.close();
				
				//azimuth stacks
				for (int iaz = 0; iaz < nAz; iaz++) {
					if (stationAzimuthList.get(iaz).size() > 0) {
						writeGMT(mapPath, event, stationAzimuthList.get(iaz), iaz * dAz);
					
						plotPath = eventDir.resolve(Paths.get("plot_az" + (iaz * dAz) + "_" + component + ".plt"));
						pwPlot = new PrintWriter(plotPath.toFile());
						pwPlot.println("set terminal postscript enhanced color font \"Helvetica,14\"");
						pwPlot.println("set output \"" + event + ".az" + (iaz * dAz) + "." + component + ".ps\"");
						pwPlot.println("unset key");
						pwPlot.println("#set yrange [63:102]"); 
						pwPlot.println("set xlabel 'Time aligned on S-wave arrival (s)'");
						pwPlot.println("set ylabel 'Distance (deg)'");
						pwPlot.println("set xtics 10");
						pwPlot.println("set ytics 5");
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
								pw.println((j * dt) + " " + (synAzimuthStack[iaz][i][j] / max) + " " + (synSpecfemAzimuthStack[iaz][i][j] / max) 
										+ " " + (obsAzimuthStack[iaz][i][j] / max));
							pw.close();
							
							pwPlot.println("'" + filename + "' u 1:($2+" + i + ") w l lc rgb 'red',\\");
							pwPlot.println("'" + filename + "' u 1:($3+" + i + ") w l lc rgb 'blue',\\");
							pwPlot.println("'" + filename + "' u 1:($4+" + i + ") w l lc rgb 'black',\\");
						}
						
						pwPlot.close();
					}
				}
			}
		}
		
		//write misfit
		Path outpathEachMisfit = profileDir.resolve("eachMisfit.inf");
		PrintWriter pwEachMisfit = new PrintWriter(Files.newBufferedWriter(outpathEachMisfit, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
		pwEachMisfit.println("station, network, lat, lon, event, component, phase, synRatio, bornRatio, synVar, bornVar, synCC, bornCC");
		pwEachMisfit.print(eachMisfitString);
		pwEachMisfit.close();
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
	
	private static void writeGMT(Path rootpath, GlobalCMTID event, List<Station> stations, double azimuth) throws IOException {
		Path outpath = rootpath.resolve("plot_map_" + event + "_az" + (int) (azimuth) + ".gmt");
		String outpathps = "map_" + event + "_az" + (int) (azimuth) + ".ps";
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
