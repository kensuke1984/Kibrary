package io.github.kensuke1984.kibrary.util.addons;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;


public class ProfileWaveform {
	public static void main(String[] args) {
		Path WorkingDir = Paths.get("");
		Path waveformIDPath = Paths.get(args[0]);
		Path waveformPath = Paths.get(args[1]);
		try {
			BasicID[] waveforms = BasicIDFile.read(waveformIDPath, waveformPath);
			
			Path profilePath = WorkingDir.resolve("profile");
			Path stackPath = WorkingDir.resolve("stack");
			Files.createDirectories(profilePath);
			Files.createDirectories(stackPath);
			
			BasicID[] obsIDs = new BasicID[waveforms.length / 2];
			BasicID[] synIDs = new BasicID[waveforms.length / 2];
			
			int counter = 0;
			for (BasicID id : waveforms)
				if (id.getWaveformType().equals(WaveformType.OBS)) {
					obsIDs[counter] = id;
					counter++;
				}
			for (int i = 0; i < obsIDs.length; i++) {
				BasicID id = obsIDs[i];
				for (int j = 0; j < waveforms.length; j++) {
					BasicID tmpid = waveforms[j];
					if (!tmpid.getWaveformType().equals(WaveformType.SYN))
						continue;
					if (tmpid.getGlobalCMTID().equals(id.getGlobalCMTID())
							&& tmpid.getStation().equals(id.getStation())
							&& tmpid.getSacComponent().equals(id.getSacComponent())) {
						synIDs[i] = tmpid;
						break;
					}
				}
			}
			
			int maxDistance = 120;
			
			List<GlobalCMTID> events = Stream.of(obsIDs).map(id -> id.getGlobalCMTID()).distinct().collect(Collectors.toList());
			
			for (GlobalCMTID event : events) {
				Path eventProfilePath = profilePath.resolve(event.toString());
				Files.createDirectories(eventProfilePath);
				Path outpath = eventProfilePath.resolve(event.toString() + ".plt");
				PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
				pw.println("set terminal postscript enhanced color font \"Helvetica,14\"");
				pw.println("set output \"" + event.toString() + ".ps\"");
				pw.println("unset key");
				pw.println("set xlabel 'Time aligned on S-wave arrival (s)'");
				pw.println("set ylabel 'Distance (deg)'");
				pw.println("set size .5,1");
				pw.print("p ");
				
				RealVector[] obsStacks = new ArrayRealVector[maxDistance];
				RealVector[] synStacks = new ArrayRealVector[maxDistance];
				
				for (int i = 0; i < obsIDs.length; i++) {
					if (obsIDs[i].getGlobalCMTID().equals(event)) {
						BasicID obsID = obsIDs[i];
						BasicID synID = synIDs[i];
						
						double[] obsData = obsID.getData();
						double[] synData = synID.getData();
						RealVector obsDataVector = new ArrayRealVector(obsData);
						RealVector synDataVector = new ArrayRealVector(synData);
						String filename = obsID.getStation().getName() + "." + obsID.getGlobalCMTID() + "." + obsID.getSacComponent() + ".txt";
						Path tracePath = eventProfilePath.resolve(filename);
						
						PrintWriter pwTrace = new PrintWriter(Files.newBufferedWriter(tracePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
						
						double maxObs = obsDataVector.getLInfNorm();
						double distance = obsID.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(obsID.getStation().getPosition())
								* 180. / Math.PI;
						pw.println("\"" + filename + "\" " + String.format("u 1:($2/%.3e+%.2f) ", maxObs, distance) + "w lines lw 1 lc \"black\",\\");
						pw.println("\"" + filename + "\" " + String.format("u 1:($3/%.3e+%.2f) ", maxObs, distance) + "w lines lw 1 lc \"red\",\\");
						
						for (int j = 0; j < obsData.length; j++)
							pwTrace.printf("%d %.6e %.6e\n", j, obsData[j], synData[j]);
						
						pwTrace.close();
						
						int k = (int) distance;
						if (k < maxDistance) {
							obsStacks[k] = obsStacks[k] == null ? obsDataVector : add(obsStacks[k], obsDataVector);
							synStacks[k] = synStacks[k] == null ? synDataVector : add(synStacks[k], synDataVector);
						}
					}
				}
				pw.println();
				pw.close();
					
				Path stackEventPath = stackPath.resolve(event.toString());
				Files.createDirectories(stackEventPath);
				PrintWriter pwStack = new PrintWriter(Files.newBufferedWriter(stackEventPath.resolve(event.toString() + ".plt")
						, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
				pwStack.println("set terminal postscript enhanced color font \"Helvetica,14\"");
				pwStack.println("set output \"" + event + ".ps\"");
				pwStack.println("unset key");
				pwStack.println("set xlabel 'Time aligned on S-wave arrival (s)'");
				pwStack.println("set ylabel 'Distance (deg)'");
				pwStack.println("set size .5,1");
				pwStack.print("p ");
				
				for (int i = 0; i < maxDistance; i++) {
					if (obsStacks[i] != null) {
					
						// print traces
						String filename = i + "." + event.toString() + ".T" + ".txt";
						Path tracePath = stackEventPath.resolve(filename);
						PrintWriter pwTrace = new PrintWriter(Files.newBufferedWriter(tracePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
						
						for (int j = 0; j < obsStacks[i].getDimension(); j++)
							pwTrace.printf("%d %.6e %.6e\n", j, obsStacks[i].getEntry(j), synStacks[i].getEntry(j));
						
						pwTrace.close();
						
						// print gnuplot script
						double distance = i;
						double maxObs = obsStacks[i].getLInfNorm();
						double maxSyn = synStacks[i].getLInfNorm();
						pwStack.println("\"" + filename + "\" " + String.format("u 1:($2/%.3e+%.2f) ", maxObs, distance) + "w lines lw 1 lc \"black\",\\");
						pwStack.println("\"" + filename + "\" " + String.format("u 1:($3/%.3e+%.2f) ", maxObs, distance) + "w lines lw 1 lc \"red\",\\");
//						pwStack.println("\"" + filename + "\" " + String.format("u 1:($2/%.3e+%.2f) ", maxSyn, distance) + "w lines lw 1 lc \"black\",\\");
//						pwStack.println("\"" + filename + "\" " + String.format("u 1:($3/%.3e+%.2f) ", maxSyn, distance) + "w lines lw 1 lc \"red\",\\");
					}
				}
				pwStack.println();
				pwStack.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	private static Trace stackTrace(List<Trace> traces) {
//		if (traces.size() == 0)
//			return null;
//		if (traces.size() == 1)
//			return traces.get(0);
//		
//		Trace res = traces.get(0);
//		double xmin = 
//		for (int i = 1; i < traces.size(); i++) {
//			if ()
//		}
//		
//		return res;
//	}
	
	private static Trace add(Trace trace1, Trace trace2) {
		double[] x1 = trace1.getX();
		double[] x2 = trace2.getX();
		double start = x1[0] < x2[0] ? x2[0] : x1[0];
		double end = x1[x1.length] > x2[x2.length] ? x2[x2.length] : x1[x1.length];
		
		return trace1.cutWindow(start, end).add((trace2).cutWindow(start, end));
	}
	
	private static RealVector add(RealVector v1, RealVector v2) {
		RealVector res = null;
		
		if (v1.getDimension() == 0)
			res = v2;
		else if (v2.getDimension() == 0)
			res = v1;
		else
			res = v1.getDimension() > v2.getDimension() ? v2.add(v1.getSubVector(0, v2.getDimension())) 
					: v1.add(v2.getSubVector(0, v1.getDimension()));
		
		return res;
	}
	
	private static List<TimewindowInformation> findWindow(Set<TimewindowInformation> timewindows, SACFileName sacname) throws IOException {
		SACData data = sacname.read();
		Station station = data.getStation();
		GlobalCMTID id = data.getGlobalCMTID();
		SACComponent component = sacname.getComponent();
		return timewindows.stream().filter(tw -> tw.getGlobalCMTID().equals(id)
				&& tw.getStation().equals(station)
				&& tw.getComponent().equals(component))
				.collect(Collectors.toList());
	}

//	private static Trace concat(List<Trace> traces) {
//		Trace res;
//		
//		double timemax = Double.MIN_VALUE;
//		for (Trace tmp : traces) {
//			if (tmp.getX()[tmp.getLength() - 1] > timemax)
//				timemax = tmp.getX()[tmp.getLength() - 1];
//		}
//		double dt = traces.get(0).getXAt(1) - traces.get(0).getXAt(0);
//		if (dt != 0.05)
//			System.err.println("Warning: dt != 0.05");
//		
//		int n = (int) (timemax / dt) + 1;
//		double[] x = new double[n];
//		double[] y = new double[n];
//		
//		for (int i = 0; i < n; i++) {
//			x[i] = dt * i;
//			
//		}
//	}
}
