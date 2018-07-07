package io.github.kensuke1984.kibrary.util;

import io.github.kensuke1984.kibrary.timewindow.Timewindow;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;


public class Profile {
	public static void main(String[] args) {
		Path WorkingDir = Paths.get("");
		Path timewindowPath = Paths.get("selectedTimewindow.dat");
		try {
			Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
			
			Path profilePath = WorkingDir.resolve("profile");
			Path stackPath = WorkingDir.resolve("stack");
			Files.createDirectories(profilePath);
			Files.createDirectories(stackPath);
			
			Set<EventFolder> events = Utilities.eventFolderSet(WorkingDir);
			
			int resampling = 10;
			for (EventFolder event : events) {
				Set<TimewindowInformation> eventtimewindows = timewindows.stream()
						.filter(tw -> tw.getGlobalCMTID().equals(event.getGlobalCMTID())).collect(Collectors.toSet());
				
				Path eventProfilePath = profilePath.resolve(event.getName());
				Files.createDirectories(eventProfilePath);
				Set<SACFileName> sacnames = event.sacFileSet().stream().filter(name -> name.isOBS()).collect(Collectors.toSet());
				
				int maxDistance = 120;
				Trace[] obsTraces = new Trace[maxDistance];
				Trace[] synTraces = new Trace[maxDistance];
				
				Path outpath = eventProfilePath.resolve(event.toString() + ".plt");
				
				PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
				
				pw.println("set terminal postscript enhanced color font \"Helvetica,12\"");
				pw.println("set output \"" + event + ".ps\"");
				pw.println("unset key");
				pw.println("set size .5,1");
				pw.print("p ");
				
				int n = sacnames.size();
				int i = 0;
				for (SACFileName sacname : sacnames) {
					i++;
					List<TimewindowInformation> timewindow = findWindow(eventtimewindows, sacname);
					
					if (timewindow.size() == 0)
						continue;
					
					SACData sacdata = sacname.read();
					String filename = sacname.getStationName() + "." + sacname.getGlobalCMTID() + "." + sacname.getComponent() + ".txt";
					Path tracePath = eventProfilePath.resolve(filename);
					
					PrintWriter pwTrace = new PrintWriter(Files.newBufferedWriter(tracePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
					
					Timewindow largerWindow = new Timewindow(timewindow.get(0).getStartTime() - 100, timewindow.get(0).getEndTime() + 200);
					
//					Trace obstrace = sacname.read().createTrace().cutWindow(700, 1800);
					SACData obsdata = sacname.read();
					Trace obstrace = obsdata.createTrace().cutWindow(largerWindow);
					String synname = sacname.getName().replace(".T", ".Tsc");
					SACData syndata = new SACFileName(event.getPath() + "/" + synname).read();
					Trace syntrace = syndata.createTrace().cutWindow(largerWindow);
					
					List<Trace> windowSyntraces = timewindow.stream().map(tw -> syndata.createTrace().cutWindow(tw)).collect(Collectors.toList());
					
					double maxObs = obsdata.createTrace().cutWindow(timewindow.get(0)).getYVector().getLInfNorm();
					double distance = sacdata.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(sacdata.getStation().getPosition())
							* 180. / Math.PI;
					
					double t0 = timewindow.get(0).getStartTime();
					
					pw.println("\"" + filename + "\" " + String.format("u ($1-%.3f):($2/%.3e+%.2f) ", t0, maxObs * 2, distance) + "w lines lw .6 lc \"black\",\\");
					pw.println("\"" + filename + "\" " + String.format("u ($1-%.3f):($3/%.3e+%.2f) ", t0, maxObs * 2, distance) + "w lines lw .6 lc \"red\",\\");
					for (int j = 0; j < windowSyntraces.size(); j++) {
						String name = sacname.getStationName() + "." + sacname.getGlobalCMTID() + "." + sacname.getComponent() + String.format("_tw%d", j) + ".txt";
						pw.println("\"" + name + "\" " + String.format("u ($1-%.3f):($2/%.3e+%.2f) ", t0, maxObs * 2, distance) + "w lines lw .8 lc \"cyan\",\\");
					}
					
					double[] time = obstrace.getX();
					double[] obsY = obstrace.getY();
					double[] synY = syntrace.getY();
					for (int j = 0; j < obstrace.getLength() / resampling; j++)
						pwTrace.printf("%.3f %.6e %.6e\n", time[j * resampling], obsY[j * resampling], synY[j * resampling]);
					
					pwTrace.close();
					
					int k = 0;
					for (Trace trace : windowSyntraces) {
						String name = sacname.getStationName() + "." + sacname.getGlobalCMTID() + "." + sacname.getComponent() + String.format("_tw%d", k) + ".txt";
						Path windowtracePath = eventProfilePath.resolve(name);
						PrintWriter pwWindow = new PrintWriter(Files.newBufferedWriter(windowtracePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
						for (int j = 0; j < trace.getLength() / resampling; j++)
							pwWindow.printf("%.3f %.6e\n", trace.getXAt(j * resampling), trace.getYAt(j * resampling));
						
						k++;
						pwWindow.close();
					}
					
					k = (int) distance;
					if (k >= maxDistance)
						continue;
					obsTraces[k] = obsTraces[k] == null ? obstrace : add(obsTraces[k], obstrace);
					synTraces[k] = synTraces[k] == null ? syntrace : add(synTraces[k], syntrace);
				}
				pw.close();
				
				Path stackEventPath = stackPath.resolve(event.toString());
				Files.createDirectories(stackEventPath);
				PrintWriter pwStack = new PrintWriter(Files.newBufferedWriter(stackEventPath.resolve(event.toString() + ".plt")
						, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
				pwStack.println("set terminal postscript enhanced color font \"Helvetica,12\"");
				pwStack.println("set output \"" + event + ".ps\"");
				pwStack.println("unset key");
				pwStack.println("set size .5,1");
				pwStack.print("p ");
				
				n = 0;
				for (i = 0; i < maxDistance; i++) {
					if (obsTraces[i] != null)
						n++;
				}
					
				int k = 0;
				for (i = 0; i < maxDistance; i++) {
					k++;
					if (obsTraces[i] == null)
						continue;
					
					// print traces
					String filename = i + "." + event.getName() + ".T" + ".txt";
					Path tracePath = stackEventPath.resolve(filename);
					PrintWriter pwTrace = new PrintWriter(Files.newBufferedWriter(tracePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
					
					double[] time = obsTraces[i].getX();
					double[] obsY = obsTraces[i].getY();
					double[] synY = synTraces[i].getY();
					
					for (int j = 0; j < obsTraces[i].getLength() / resampling; j++)
						pwTrace.printf("%.3f %.6e %.6e\n", time[j * resampling], synY[j * resampling], obsY[j * resampling]);
					
					pwTrace.close();
					
					// print gnuplot script
					double distance = i + .5;
					double maxObs = obsTraces[i].getYVector().getLInfNorm();
					pwStack.println("\"" + filename + "\" " + String.format("u ($1-8.4*%.2f):($2/%.3e+%.2f) ", distance, maxObs * 2, distance) + "w lines lw .6 lc \"black\",\\");
					if (k == n)
						pwStack.println("\"" + filename + "\" " + String.format("u ($1-8.4*%.2f):($3/%.3e+%.2f) ", distance, maxObs * 2, distance) + "w lines lw .6 lc \"red\"");
					else
						pwStack.println("\"" + filename + "\" " + String.format("u ($1-8.4*%.2f):($3/%.3e+%.2f) ", distance, maxObs * 2, distance) + "w lines lw .6 lc \"red\",\\");
				}
				
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
		double end = x1[x1.length-1] > x2[x2.length-1] ? x2[x2.length-1] : x1[x1.length-1];
		
		return trace1.cutWindow(start, end).add((trace2).cutWindow(start, end));
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
