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
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

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
		double deltaHalfDuration = 0.25;
		int np = 512;
		double tlen = 3276.8;
		double samplingHz = 20.;
		Path timewindowFile = Paths.get(args[0]);
		
		SourceTimeFunctionByGridSearch stfSearch 
			= new SourceTimeFunctionByGridSearch(workingDir,
					minHalfDuration, maxHalfDuration,
					deltaHalfDuration, np, tlen, samplingHz,
					timewindowFile);
		
		stfSearch.run();
	}
	
	public void run() {
		try {
			Set<EventFolder> eventFolders 
				= Utilities.eventFolderSet(workingDir);
			
			Map<GlobalCMTID, double[]> outputMap = new HashMap<>();
			Map<GlobalCMTID, Integer> nTraceMap = new HashMap<>();
			Path outputinfoPath = workingDir.resolve("outputInfo" 
					+ Utilities.getTemporaryString() + ".astf.inf");
			
			Path catalogueFile = workingDir.resolve("astf" 
					+ Utilities.getTemporaryString() + ".catalogue");
			Files.deleteIfExists(catalogueFile);
			Files.createFile(catalogueFile);
			Path gcmtFile = workingDir.resolve("gcmtstf" 
					+ Utilities.getTemporaryString() + ".catalogue");
			Files.createFile(gcmtFile);
			
			for (EventFolder eventFolder : eventFolders) {
				System.out.println("> " + eventFolder.getGlobalCMTID());
				int n = Runtime.getRuntime().availableProcessors();
				ExecutorService es = Executors.newFixedThreadPool(n);
				System.out.println("Running reader on " + n + " processors");
				
				Reader reader = new Reader(timewindows, eventFolder);
				try {
					es.execute(reader);
					es.shutdown();
					es.awaitTermination(300, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				List<Trace> obsTraces = reader.getObsTraces();
				List<Trace> synTraces = reader.getSynTraces();
				List<TimewindowInformation> orderedTimewindows 
					= reader.getOrderedTimewindows();
				
				System.out.println("DEBUG1: " + orderedTimewindows.size()
						+ " " + obsTraces.size()
						+ " " + synTraces.size());
				
				n = Runtime.getRuntime().availableProcessors();
				es = Executors.newFixedThreadPool(n);
				System.out.println("Running worker on " + n + " processors");
				
				Worker worker = new Worker(eventFolder.getGlobalCMTID(),
						minHalfDuration, maxHalfDuration, deltaHalfDuration,
						np, tlen, samplingHz, obsTraces, synTraces,
						orderedTimewindows);
				try {
					es.execute(worker);
					es.shutdown();
					es.awaitTermination(600, TimeUnit.SECONDS);
					
					int nTrace = worker.getNtrace();
					nTraceMap.put(eventFolder.getGlobalCMTID(), nTrace);
					
					double halfDuration;
					
					if (nTrace > 0) {
						double[] variances = worker.getVariances();
						double[] halfDurations = worker.getHalfDurations();
						outputMap.put(eventFolder.getGlobalCMTID(), variances);
						int imin = new ArrayRealVector(variances).getMinIndex();
						halfDuration = halfDurations[imin];
					}
					else {
						halfDuration = eventFolder.getGlobalCMTID()
							.getEvent().getHalfDuration();
						double[] variances = new double[npts];
						for (int i = 0; i < variances.length; i++)
							variances[i] = Double.NaN;
						outputMap.put(eventFolder.getGlobalCMTID(), variances);
					}
					
//					System.out.println("GCMT half duration = " 
//							+ eventFolder.getGlobalCMTID().getEvent().getHalfDuration());
//					for (int i = 0; i < variances.length; i++)
//						System.out.println(variances[i] + " " + halfDurations[i]);
					
					Files.write(catalogueFile, String.format("%s %f\n"
							, eventFolder.getGlobalCMTID()
							, halfDuration).getBytes()
						, StandardOpenOption.APPEND);
					
					Files.write(gcmtFile, String.format("%s %f\n"
							, eventFolder.getGlobalCMTID()
							, eventFolder.getGlobalCMTID().getEvent().getHalfDuration())
								.getBytes()
						, StandardOpenOption.APPEND);
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outputinfoPath
					, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
				List<GlobalCMTID> idList = outputMap.keySet().stream()
						.collect(Collectors.toList());
				for (GlobalCMTID id : idList)
					pw.println("> " + id + " " + nTraceMap.get(id));
				for (int i = 0; i < npts; i++) {
					pw.printf("%.4f ", i * deltaHalfDuration);
					for (GlobalCMTID id : idList) {
						pw.printf("%.4f ", outputMap.get(id)[i]);
					}
					pw.println();
				}
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
			this.halfDurations = new double[npts];
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
					double[] synConvolved = stf.convolve(synTraceList.get(j).getY());
					
					TimewindowInformation timewindow = orderedTimewindows.get(j);
					
					Trace obsTrace = obsTraceList.get(j);
					Trace synTraceConvolved = new Trace(synTraceList.get(j).getX()
							, synConvolved);
					
					Trace tmpObsTrace = obsTrace.cutWindow(timewindow.getStartTime() - 10., timewindow.getEndTime() + 10.);
					synTraceConvolved = synTraceConvolved.cutWindow(timewindow);
					Trace tmpSynTrace = synTraceConvolved;
					
					double shift = tmpObsTrace.findBestShift(tmpSynTrace);
//					System.out.println("DEBUG2 : half-duration = " + halfDuration + "; best shift = " + shift);
					
					obsTrace = obsTrace.shiftX(-shift).cutWindow(timewindow);
//					obsTrace = obsTrace.cutWindow(timewindow);
					
					RealVector synVector = synTraceConvolved.getYVector();
					RealVector obsVector = obsTrace.getYVector();
					
					// DEBUG
//					for (int k = 0; i < synVector.getDimension(); i++)
//						System.out.println(i/samplingHz + " " + synVector.getEntry(i) + " " + obsVector.getEntry(i));
//					System.exit(0);
					//
					
					if (synVector.getDimension() == obsVector.getDimension() + 1)
						synVector = synVector.getSubVector(0, obsVector.getDimension());
					if (synVector.getDimension() == obsVector.getDimension() - 1)
						obsVector = obsVector.getSubVector(0, synVector.getDimension());
					
					variances[i] += synVector.subtract(obsVector)
							.getNorm() / obsVector.getNorm();
				}
				
				variances[i] /= orderedTimewindows.size();
			}
		}
		
		public double[] getVariances() {
			return variances;
		}
		
		public double[] getHalfDurations() {
			return halfDurations;
		}
		
		public int getNtrace() {
			return obsTraceList.size();
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
			this.timewindows = timewindows.parallelStream().filter(tw -> 
					tw.getGlobalCMTID().equals(eventFolder.getGlobalCMTID()))
					.collect(Collectors.toSet());
			this.eventFolder = eventFolder;
			this.obsTraceList = new ArrayList<>();
			this.synTraceList = new ArrayList<>();
			this.orderedTimewindows = new ArrayList<>();
		}
		
		public void run() {
			try {
				for (TimewindowInformation timewindow : timewindows) {
					Path sacObsPath = eventFolder.toPath().resolve(timewindow.getStation().getStationName() + "." +
							timewindow.getGlobalCMTID().toString() + "." +
							timewindow.getComponent());
					SACFileName obsfile = new SACFileName(sacObsPath);
					Trace obsTrace = obsfile.read().createTrace();
					
					Path sacSynPath = workingDir.resolve(sacObsPath.toString() + "s");
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
