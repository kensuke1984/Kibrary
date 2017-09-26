package io.github.kensuke1984.kibrary.datacorrection;

import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SourceTimeFunctionByGridSearch {
	private Path workingDir;
	private BasicID[] basicIDs;
	private double deltaHalfDuration;
	private double minHalfDuration;
	private double maxHalfDuration;
	private int npts;
	private int np;
	private double tlen;
	private double samplingHz;
	private Set<TimewindowInformation> timewindows;
	
	public SourceTimeFunctionByGridSearch(Path workingDir, double minHalfDuration,
			double maxHalfDuration, double deltaHalfDuration,
			int np, double tlen, double samplingHz, Path timewindowFile) {
		this.workingDir = workingDir;
		this.minHalfDuration = minHalfDuration;
		this.maxHalfDuration = maxHalfDuration;
		this.deltaHalfDuration = deltaHalfDuration;
		npts = (int) ((maxHalfDuration - minHalfDuration) /
				deltaHalfDuration) + 1;
		this.np = np;
		this.tlen = tlen;
		this.samplingHz = samplingHz;
		try {
			this.timewindows = TimewindowInformationFile.read(timewindowFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Path workingDir = Paths.get(".");
		double minHalfDuration = 1.;
		double maxHalfDuration = 12.;
		double deltaHalfDuration = 0.5;
		int np = 512;
		double tlen = 3276.8;
		double samplingHz = 20.;
		Path timewindowFile = Paths.get(args[0]);
		
		SourceTimeFunctionByGridSearch stfSearch 
			= new SourceTimeFunctionByGridSearch(workingDir,
					minHalfDuration, maxHalfDuration,
					deltaHalfDuration, np, tlen, samplingHz,
					timewindowFile);
	}
	
	public void run() {
		try {
			Set<EventFolder> eventFolders 
				= Utilities.eventFolderSet(workingDir);
			
			for (EventFolder eventFolder : eventFolders) {
				int n = Runtime.getRuntime().availableProcessors();
				ExecutorService es = Executors.newFixedThreadPool(n);
				
				Reader reader = new Reader(timewindows, eventFolder);
				es.execute(reader);
				
				List<Trace> obsTraces = reader.getObsTraces();
				List<Trace> synTraces = reader.getSynTraces();
				List<TimewindowInformation> orderedTimewindows 
					= reader.getOrderedTimewindows();
				es.shutdown();
				
				Worker worker = new Worker(eventFolder.getGlobalCMTID(),
						minHalfDuration, maxHalfDuration, deltaHalfDuration,
						np, tlen, samplingHz, obsTraces, synTraces,
						orderedTimewindows);
				es.execute(worker);
				
				double[] variances = worker.getVariances();
				double[] halfDurations = worker.getHalfDurations();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class Worker implements Runnable {
		private GlobalCMTID globalCMTID;
		private double deltaHalfDuration;
		private double minHalfDuration;
		private double maxHalfDuration;
		private int npts;
		private int np;
		private double tlen;
		private double samplingHz;
		private List<Trace> obsTraceList;
		private List<Trace> synTraceList;
		private List<TimewindowInformation> orderedTimewindows;
		private double[] variances;
		private double[] halfDurations;
		
		public Worker(GlobalCMTID globalCMTID, double minHalfDuration,
				double maxHalfDuration, double deltaHalfDuration,
				int np, double tlen, double samplingHz,
				List<Trace> obsTraceList,
				List<Trace> synTraceList, List<TimewindowInformation> orderedTimewindows) {
			this.globalCMTID = globalCMTID;
			this.minHalfDuration = minHalfDuration;
			this.maxHalfDuration = maxHalfDuration;
			this.deltaHalfDuration = deltaHalfDuration;
			npts = (int) ((maxHalfDuration - minHalfDuration) /
					deltaHalfDuration) + 1;
			this.np = np;
			this.tlen = tlen;
			this.samplingHz = samplingHz;
			this.obsTraceList = obsTraceList;
			this.synTraceList = synTraceList;
			this.orderedTimewindows = orderedTimewindows;
			this.variances = new double[npts];
		}
		
		@Override
		public void run() {
			for (int i = 0; i < npts; i++) {
				double halfDuration = minHalfDuration +
						i * deltaHalfDuration;
				SourceTimeFunction stf = SourceTimeFunction
						.triangleSourceTimeFunction(np, tlen, samplingHz, halfDuration);
				
				variances[i] = 0;
				halfDurations[i] = halfDuration;
				for (int j = 0; j < orderedTimewindows.size(); j++) {
					double[] obsConvolved = stf.convolve(obsTraceList.get(j).getY());
					double[] synConvolved = stf.convolve(synTraceList.get(j).getY());
					
					TimewindowInformation timewindow = orderedTimewindows.get(j);
					
					double[] x = synTraceList.get(j).getX();
					Trace obsTraceConvolved = new Trace(x, obsConvolved);
					Trace synTraceConvolved = new Trace(x, synConvolved);
					
					Trace tmpObsTrace = obsTraceConvolved.cutWindow(timewindow.getStartTime() - 10., timewindow.getEndTime() + 10.);
					synTraceConvolved = synTraceConvolved.cutWindow(timewindow);
					
					double shift = synTraceConvolved.findBestShift(tmpObsTrace);
					
					obsTraceConvolved = obsTraceConvolved.shiftX(shift).cutWindow(timewindow);
					
					variances[i] += Math.sqrt(synTraceConvolved.getYVector().subtract(obsTraceConvolved.getYVector())
							.getNorm() / obsTraceConvolved.getYVector().getNorm());
				}
			}
		}
		
		public double[] getVariances() {
			return variances;
		}
		
		public double[] getHalfDurations() {
			return halfDurations;
		}
	}
	
	private class Reader implements Runnable {
		EventFolder eventFolder;
		Set<TimewindowInformation> timewindows;
		List<Trace> obsTraceList;
		List<Trace> synTraceList;
		List<TimewindowInformation> orderedTimewindows;
		
		public Reader(Set<TimewindowInformation> timewindows, EventFolder eventFolder) {
			this.timewindows = timewindows;
			this.eventFolder = eventFolder;
			this.obsTraceList = new ArrayList<>();
			this.synTraceList = new ArrayList<>();
			this.orderedTimewindows = new ArrayList<>();
		}
		
		public void run() {
			Set<TimewindowInformation> thisWindows =
					timewindows.stream().filter(tw -> 
						tw.getGlobalCMTID().equals(eventFolder.getGlobalCMTID()))
					.collect(Collectors.toSet());
			try {
				for (TimewindowInformation timewindow : thisWindows) {
					Path sacObsPath = eventFolder.toPath().resolve(timewindow.getStation().getStationName() + "." +
							timewindow.getGlobalCMTID().toString() + "." +
							timewindow.getComponent());
					SACFileName obsfile = new SACFileName(sacObsPath);
					Trace obsTrace = obsfile.read().createTrace();
					
					Path sacSynPath = eventFolder.toPath().resolve(sacObsPath.toString() + "s");
					SACFileName synfile = new SACFileName(sacSynPath);
					Trace synTrace = synfile.read().createTrace();
					
					obsTraceList.add(obsTrace);
					synTraceList.add(synTrace);
					
					orderedTimewindows.add(timewindow);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public List<Trace> getSynTraces() {
			return synTraceList;
		}
		
		public List<Trace> getObsTraces() {
			return obsTraceList;
		}
		
		public List<TimewindowInformation> getOrderedTimewindows() {
			return orderedTimewindows;
		}
	}
}
