package io.github.kensuke1984.kibrary.datacorrection;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.selection.DataSelection;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class SourceTimeFunctionByGridSearch implements Operation {
	private Path workPath;
	private double deltaHalfDuration;
	private double minHalfDuration;
	private double maxHalfDuration;
	private double minSNratio;
	private double ratio;
	private double minDistance;
	private double maxDistance;
	private int npts;
	private int np;
	private double tlen;
	private double samplingHz;
	private Path timewindowInformationFilePath;
	private Set<TimewindowInformation> timewindows;
	private Set<SACComponent> components;
	double[] amplitudeCorrections;
	double[][] misfits;
	double[] halfDurations;
	RealVector[] obsStacks;
	RealVector[] synStacks;
	
	private Properties property;
	
	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(SourceTimeFunctionByGridSearch.class.getName() 
				+ Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan SourceTimeFunctionByGridSearch");
			pw.println("##Path of a working folder (.)");
			pw.println("#workPath");
			pw.println("##Sac components to be used (Z)");
			pw.println("#components");
//			pw.println("##Path of a root folder containing observed dataset (.)");
//			pw.println("#obsPath");
//			pw.println("##Path of a root folder containing synthetic dataset (.)");
//			pw.println("#synPath");
			pw.println("##Path of a time window information file, must be defined");
			pw.println("#timewindowInformationFilePath timewindow.dat");
			pw.println("##double minimum half duration for grid search (0.5)");
			pw.println("#minHalfDuration");
			pw.println("##double maximum half duration for grid search (16.)");
			pw.println("#maxHalfDuration");
			pw.println("##double half duration increment (.25)");
			pw.println("#deltaHalfDuration");
			pw.println("##double minimum signal-to-noise ratio of data used (2)");
			pw.println("#minSNratio");
			pw.println("##double amplitude ratio (3.)");
			pw.println("#ratio");
			pw.println("##double min epicentral distance (30.)");
			pw.println("#minDistance");
			pw.println("##double max epicentral distance (90.)");
			pw.println("#maxDistance");
			pw.println("##int np used in DSM (1024)");
			pw.println("#np");
			pw.println("##double tlen used in DSM (3276.8)");
			pw.println("#tlen");
			pw.println("##int sampling Hz (20) (cannot change now)");
			pw.print("#samplingHz");
		}
		System.err.println(outPath + " is created.");
	}
	
	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z");
//		if (!property.containsKey("obsPath"))
//			property.setProperty("obsPath", "");
//		if (!property.containsKey("synPath"))
//			property.setProperty("synPath", "");
		if (!property.containsKey("minHalfDuration"))
			property.setProperty("minHalfDuration", "0.25");
		if (!property.containsKey("maxHalfDuration"))
			property.setProperty("maxHalfDuration", "16.");
		if (!property.containsKey("deltaHalfDuration"))
			property.setProperty("deltaHalfDuration", "0.25");
		if (!property.containsKey("minSNratio"))
			property.setProperty("minSNratio", "2");
		if (!property.containsKey("np"))
			property.setProperty("np", "1024");
		if (!property.containsKey("tlen"))
			property.setProperty("tlen", "3276.8");
		if (!property.containsKey("samplingHz"))
			property.setProperty("samplingHz", "20");
		if (!property.containsKey("ratio"))
			property.setProperty("ratio", "3.");
		if (!property.containsKey("minDistance"))
			property.setProperty("minDistance", "30.");
		if (!property.containsKey("maxDistance"))
			property.setProperty("maxDistance", "90.");
	}
	
	private void set() throws IOException {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));
		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		minHalfDuration = Double.parseDouble(property.getProperty("minHalfDuration"));
		maxHalfDuration = Double.parseDouble(property.getProperty("maxHalfDuration"));
		deltaHalfDuration = Double.parseDouble(property.getProperty("deltaHalfDuration"));
		minSNratio = Double.parseDouble(property.getProperty("minSNratio"));
		ratio = Double.parseDouble(property.getProperty("ratio"));
		minDistance = Double.parseDouble(property.getProperty("minDistance"));
		maxDistance = Double.parseDouble(property.getProperty("maxDistance"));
		np = Integer.parseInt(property.getProperty("np"));
		tlen = Double.parseDouble(property.getProperty("tlen"));
		samplingHz = Integer.parseInt(property.getProperty("samplingHz"));
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());
		
		Path timewindowInformationFilePath = 
				Paths.get(property.getProperty("timewindowInformationFilePath"));
		try {
			timewindows = TimewindowInformationFile.read(timewindowInformationFilePath)
					.parallelStream()
					.filter(tw -> components.contains(tw.getComponent()))
					.collect(Collectors.toSet());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		npts = (int) ((maxHalfDuration - minHalfDuration) /
				deltaHalfDuration) + 2;
		
		halfDurations = new double[npts];
		
		double maxAmpcorr = 3.;
		double deltaAmpcorr = .1;
		int nampcorr = 2 * (int) ((maxAmpcorr - 1) / deltaAmpcorr) + 1;
		amplitudeCorrections = new double[nampcorr];
		for (int k = 0; k < nampcorr / 2; k++)
			amplitudeCorrections[k] = 1. / (maxAmpcorr - k * deltaAmpcorr);
		amplitudeCorrections[nampcorr / 2] = 1.;
		for (int k = 0; k < nampcorr / 2; k++)
			amplitudeCorrections[k + nampcorr / 2 + 1] = 1. + (k + 1) * deltaAmpcorr;
		
		misfits = new double[npts][nampcorr];
	}
	
	@Override
	public Properties getProperties() {
		return property;
	}
	
	@Override
	public Path getWorkPath() {
		return workPath;
	}
	
	public SourceTimeFunctionByGridSearch(Path workingDir, double minHalfDuration,
			double maxHalfDuration, double deltaHalfDuration,
			int np, double tlen, double samplingHz, Path timewindowFile) {
		this.workPath = workingDir;
		this.minHalfDuration = minHalfDuration;
		this.maxHalfDuration = maxHalfDuration;
		this.deltaHalfDuration = deltaHalfDuration;
		npts = (int) ((maxHalfDuration - minHalfDuration) /
				deltaHalfDuration) + 2;
		this.np = np;
		this.tlen = tlen;
		this.samplingHz = samplingHz;
		try {
			this.timewindows = TimewindowInformationFile.read(timewindowFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public SourceTimeFunctionByGridSearch(Properties property) 
			throws IOException {
		this.property = (Properties) property.clone();
		set();
	}
	
	public static void main(String[] args) throws IOException {
		
		SourceTimeFunctionByGridSearch stfSearch =
				new SourceTimeFunctionByGridSearch(Property.parse(args));
		long start = System.nanoTime();
		System.err.println(SourceTimeFunctionByGridSearch.class.getName() 
				+ " is going");
		stfSearch.run();
		System.err.println(
				SourceTimeFunctionByGridSearch.class.getName() + " finished in " 
			  + Utilities.toTimeString(System.nanoTime() - start));
	}
	
	public void run() {
		try {
			Set<EventFolder> eventFolders 
				= Utilities.eventFolderSet(workPath);
			
			Map<GlobalCMTID, double[]> outputMap = new HashMap<>();
			Map<GlobalCMTID, Integer> nTraceMap = new HashMap<>();
			Path outputinfoPath = workPath.resolve("outputInfo" 
					+ Utilities.getTemporaryString() + ".astf.inf");
			
			Path catalogueFile = workPath.resolve("astf" 
					+ Utilities.getTemporaryString() + ".catalogue");
			Files.deleteIfExists(catalogueFile);
			Files.createFile(catalogueFile);
			Path gcmtFile = workPath.resolve("gcmtstf" 
					+ Utilities.getTemporaryString() + ".catalogue");
			Files.createFile(gcmtFile);
			
			Path stackDir = workPath.resolve("qcStack" + Utilities.getTemporaryString());
			Files.createDirectories(stackDir);
			
			Files.write(catalogueFile, ">id, Mw, half-duration, amp. corr., num. traces, misfit, gcmt misfit\n".getBytes()
					, StandardOpenOption.APPEND);
			
			
			for (EventFolder eventFolder : eventFolders) {
				System.out.println("> " + eventFolder.getGlobalCMTID());
				int n = Runtime.getRuntime().availableProcessors();
				ExecutorService es = Executors.newFixedThreadPool(n);
				System.out.println("Running reader on " + n + " processors");
				
				Reader reader = new Reader(timewindows, eventFolder,
						minSNratio, ratio, minDistance, maxDistance);
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
				int excludedObsLength = reader.getExcludedObsLength();
				
				System.out.println("Used: " + orderedTimewindows.size()
						+ " " + obsTraces.size()
						+ " " + synTraces.size());
				System.out.println("Excluded: " + excludedObsLength);
				
				double gcmtHalfDuration = eventFolder.getGlobalCMTID().getEvent().getHalfDuration();
				
				for (int i = 0; i < npts; i++) {
					halfDurations[i] = minHalfDuration +
						i * deltaHalfDuration;
					if (halfDurations[i] < gcmtHalfDuration
							&& halfDurations[i] + deltaHalfDuration > gcmtHalfDuration)
						halfDurations[i] = gcmtHalfDuration;
				}
				
				n = Runtime.getRuntime().availableProcessors();
				es = Executors.newFixedThreadPool(n);
				System.out.println("Running worker on " + n + " processors");
				
				
				for (int i = 0; i < npts; i++)
					for (int j = 0; j < amplitudeCorrections.length; j++)
						misfits[i][j] = 0.;
				try {
					for (int i = 0; i < npts; i++) {
//						System.out.println(i + "/" + (npts-1));
						Worker worker = new Worker(halfDurations[i], amplitudeCorrections, orderedTimewindows, i
								, obsTraces, synTraces);
						es.execute(worker);
					}
					
					es.shutdown();
					es.awaitTermination(600, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				double halfDurationMinMisfit;
				double ampCorrMinMisfit;
				double minMisfit = Double.MAX_VALUE;
				double gcmtMisfit = 0.;
				if (obsTraces.size() > 0) {
					int iDurationMin = 0;
					int iAmpcorrMin = 0;
					for (int i = 0; i < npts; i++) {
						for (int j = 0; j < amplitudeCorrections.length; j++) {
							if (misfits[i][j] < minMisfit) {
								minMisfit = misfits[i][j];
								iDurationMin = i;
								iAmpcorrMin = j;
							}
						}
						if (halfDurations[i] == gcmtHalfDuration)
							gcmtMisfit = misfits[i][amplitudeCorrections.length / 2];
					}
					
					System.out.println(minMisfit);
					
					double[] misfitMinAmpcorr = new double[npts];
					for (int i = 0; i < npts; i++)
						misfitMinAmpcorr[i] = misfits[i][iAmpcorrMin];
					
					outputMap.put(eventFolder.getGlobalCMTID(), misfitMinAmpcorr);
//						int imin = new ArrayRealVector(misfits).getMinIndex();
					halfDurationMinMisfit = halfDurations[iDurationMin];
					ampCorrMinMisfit = amplitudeCorrections[iAmpcorrMin];
				}
				else {
					halfDurationMinMisfit = Double.NaN;
					ampCorrMinMisfit = Double.NaN;
					minMisfit = Double.NaN;
					double[] misfits = new double[npts];
					for (int i = 0; i < misfits.length; i++)
						misfits[i] = Double.NaN;
					outputMap.put(eventFolder.getGlobalCMTID(), misfits);
				}
				
				// stack for visual quality control
				double[] obsStack;
				double[] bestSynStack;
				double[] gcmtSynStack;
				
				SourceTimeFunction stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, samplingHz, halfDurationMinMisfit);
				RealVector[] obsSynStacks = stack(stf, orderedTimewindows, synTraces, obsTraces);
				obsStack = obsSynStacks[0].toArray();
				bestSynStack = obsSynStacks[1].toArray();
				
				stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, samplingHz, gcmtHalfDuration);
				gcmtSynStack = stack(stf, orderedTimewindows, synTraces, obsTraces)[1].toArray();
						
					
//					System.out.println("GCMT half duration = " 
//							+ eventFolder.getGlobalCMTID().getEvent().getHalfDuration());
//					for (int i = 0; i < misfits.length; i++)
//						System.out.println(misfits[i] + " " + halfDurations[i]);
					
				Files.write(catalogueFile, String.format("%s %.1f %f %f %d %.4e %.4e\n"
						, eventFolder.getGlobalCMTID()
						, eventFolder.getGlobalCMTID().getEvent().getCmt().getMw()
						, halfDurationMinMisfit, ampCorrMinMisfit
						, obsTraces.size(), minMisfit, gcmtMisfit).getBytes()
					, StandardOpenOption.APPEND);
				
				Files.write(gcmtFile, String.format("%s %f\n"
						, eventFolder.getGlobalCMTID()
						, eventFolder.getGlobalCMTID().getEvent().getHalfDuration())
							.getBytes()
					, StandardOpenOption.APPEND);
					
					
					// Write quality control stack (qcStack) to files.
					Path stackPath = stackDir.resolve(eventFolder.getGlobalCMTID() + ".qcStack");
					int lendata = Integer.min(bestSynStack.length, Integer.min(obsStack.length, gcmtSynStack.length));
					if (lendata != obsStack.length)
						System.out.println("Warning: lenght of obs, gcmtsyn, bestsyn stacked vector differ: " 
						+ bestSynStack.length + " " + gcmtSynStack.length + " " + obsStack.length);
					try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(stackPath
							, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
						pw.println("# t (s), obsStack, bestSynStack, gcmtSynStack");
						for (int i = 0; i < lendata; i++)
							pw.printf("%.3f %.5e %.5e %.5e\n"
									, i / samplingHz
									, obsStack[i]
									, bestSynStack[i]
									, gcmtSynStack[i]);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					// Write Gnuplot scripts for qcStack
					Path scriptPath = stackDir.resolve(eventFolder.getGlobalCMTID() + ".plt");
					String psfileName = eventFolder.getGlobalCMTID() + ".qcStack.ps";
					String datafileName = stackPath.getFileName().toString();
					writeForGnuplot(scriptPath, psfileName, datafileName);
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
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class Worker implements Runnable {
	private double halfDuration;
	private List<TimewindowInformation> orderedTimewindows;
	private int i;
	private List<Trace> obsTraceList;
	private List<Trace> synTraceList;
	
	public Worker(double halfDuration, double[] amplitudeCorrections, List<TimewindowInformation> orderedTimewindows, int i
			, List<Trace> obsTraceList, List<Trace> synTraceList) {
		this.halfDuration = halfDuration;
		this.orderedTimewindows = orderedTimewindows;
		this.i = i;
		this.obsTraceList = obsTraceList;
		this.synTraceList = synTraceList;
	}
	
	@Override
	public void run() {
			SourceTimeFunction stf = SourceTimeFunction
					.triangleSourceTimeFunction(np, tlen, samplingHz, halfDuration);
			
//			RealVector obsStack = new ArrayRealVector((int) (100. * samplingHz));
//			RealVector synStack = new ArrayRealVector((int) (100. * samplingHz));
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
//				System.out.println("DEBUG2 : half-duration = " + halfDuration + "; best shift = " + shift);
				
				obsTrace = obsTrace.shiftX(-shift).cutWindow(timewindow);
//				obsTrace = obsTrace.cutWindow(timewindow);
				
				RealVector synVector = synTraceConvolved.getYVector();
				RealVector obsVector = obsTrace.getYVector();
				
//				synVector = synVector.mapMultiply(obsVector.getLInfNorm() 
//						/ synVector.getLInfNorm());
				
				// DEBUG
//				for (int k = 0; i < synVector.getDimension(); i++)
//					System.out.println(i/samplingHz + " " + synVector.getEntry(i) + " " + obsVector.getEntry(i));
//				System.exit(0);
				//
				
				if (synVector.getDimension() == obsVector.getDimension() + 1)
					synVector = synVector.getSubVector(0, obsVector.getDimension());
				if (synVector.getDimension() == obsVector.getDimension() - 1)
					obsVector = obsVector.getSubVector(0, synVector.getDimension());
				
				// variance
				for (int k = 0; k < amplitudeCorrections.length; k++) {
					RealVector correctedObs = obsVector.mapMultiply(amplitudeCorrections[k]);
					double tmpMisfit = synVector.subtract(correctedObs)
							.getNorm() / correctedObs.getNorm();
					misfits[i][k] += tmpMisfit;
				}
				
				// normalized variance
//				misfits[i] += synVector.mapDivide(synVector.getLInfNorm())
//						.subtract(obsVector.mapDivide(obsVector.getLInfNorm()))
//						.getNorm()
//						/ obsVector.mapDivide(obsVector.getLInfNorm()).getNorm();
				
				// Stack waveforms and later keep only the one with best variance.
				// For visual check only.
//				double linfinv = 1. / obsVector.getLInfNorm();
//				obsVector = obsVector.mapMultiply(linfinv);
//				synVector = synVector.mapMultiply(linfinv);
//				if (obsStacks[i].getDimension() < obsVector.getDimension()) {
//					obsStacks[i] = obsStacks[i].add(obsVector.getSubVector(0, obsStacks[i].getDimension()));
//					synStacks[i] = synStacks[i].add(synVector.getSubVector(0, synStacks[i].getDimension()));
//				}
//				else if (obsStacks[i].getDimension() > obsVector.getDimension()) {
//					obsStacks[i] = obsStacks[i].getSubVector(0, obsVector.getDimension())
//							.add(obsVector);
//					synStacks[i] = synStacks[i].getSubVector(0, obsVector.getDimension())
//							.add(synVector);
//				}
//				else {
//					obsStacks[i] = obsStacks[i].add(obsVector);
//					synStacks[i] = synStacks[i].add(synVector);
//				}
			}
			
			for (int k = 0; k < amplitudeCorrections.length; k++)
				misfits[i][k] = misfits[i][k] / orderedTimewindows.size();
		}
	}
	
	private class Reader implements Runnable {
		EventFolder eventFolder;
		Set<TimewindowInformation> timewindows;
		List<Trace> obsTraceList;
		List<Trace> synTraceList;
		List<TimewindowInformation> orderedTimewindows;
		private double minSNratio;
		private double ratio;
		private List<Trace> excludedObsTrace;
		private double minDistance;
		private double maxDistance;
		
		public Reader(Set<TimewindowInformation> timewindows, EventFolder eventFolder,
				double minSNratio, double ratio, double minDistance, double maxDistance) {
			this.timewindows = timewindows.parallelStream().filter(tw -> 
					tw.getGlobalCMTID().equals(eventFolder.getGlobalCMTID()))
					.collect(Collectors.toSet());
			this.eventFolder = eventFolder;
			this.obsTraceList = new ArrayList<>();
			this.synTraceList = new ArrayList<>();
			this.excludedObsTrace = new ArrayList<>();
			this.orderedTimewindows = new ArrayList<>();
			this.minSNratio = minSNratio;
			this.ratio = ratio;
			this.minDistance = minDistance;
			this.maxDistance = maxDistance;
		}
		
		public void run() {
			try {
				for (TimewindowInformation timewindow : timewindows) {
					double distance = timewindow.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
							timewindow.getStation().getPosition()) * 180. / Math.PI;
					if (distance < minDistance || distance > maxDistance)
						continue;
					Path sacObsPath = eventFolder.toPath().resolve(timewindow.getStation().getStationName() + "." +
							timewindow.getGlobalCMTID().toString() + "." +
							timewindow.getComponent());
					SACFileName obsfile = new SACFileName(sacObsPath);
					Trace obsTrace = obsfile.read().createTrace();
					
					// estimate SN ratio
					double signalValue = obsTrace.cutWindow(timewindow)
							.getYVector().getNorm();
					double tNoiseEnd = timewindow.getStartTime() - 30.;
					double tNoiseStart = tNoiseEnd - timewindow.getLength() * 3.;
					double noiseValue = obsTrace.cutWindow(tNoiseStart, tNoiseEnd)
							.getYVector().getNorm()
							/ 3.;
					if (noiseValue == 0) {
						System.err.println("Ignorign timewindow with noise value 0: " + timewindow);
						continue;
					}
					double snratio = signalValue / noiseValue;
//					System.out.println("DEBUG1: " + tNoiseStart + " " + tNoiseEnd + " " + snratio);
					
					if (snratio >= minSNratio) {
						Path sacSynPath = workPath.resolve(sacObsPath.toString() + "s");
						SACFileName synfile = new SACFileName(sacSynPath);
						Trace synTrace = synfile.read().createTrace();
						
						double amplitudeRatio = obsTrace.cutWindow(timewindow).getYVector().getLInfNorm() /
								synTrace.cutWindow(timewindow).getYVector().getLInfNorm();
						
						if (amplitudeRatio <= ratio && amplitudeRatio >= 1. / ratio) {
							obsTraceList.add(obsTrace);
							synTraceList.add(synTrace);
							
							orderedTimewindows.add(timewindow);
						}
						else
							excludedObsTrace.add(obsTrace);
					}
					else {
						excludedObsTrace.add(obsTrace);
					}
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
		
		public List<Trace> getExcludedObs() {
			return excludedObsTrace;
		}
		
		public int getExcludedObsLength() {
			return excludedObsTrace.size();
		}
		
		public List<TimewindowInformation> getOrderedTimewindows() {
			return orderedTimewindows;
		}
	}
	
	private void writeForGnuplot(Path outpath, String psfileName, String datafileName) throws IOException {
		PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath
				, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
		pw.print("set terminal postscript enhanced color font 'Helvetica,14'\n"
				+ "set xlabel 'Time (s)'\n"
				+ "set output '" + psfileName + "'\n"
				+ "p '" + datafileName + "' u 1:2 w l lc rgb 'black' ti 'Obs',\\\n"
				+ "'" + datafileName + "' u 1:3 w l lc rgb 'red' ti 'Best syn',\\\n"
				+ "'" + datafileName + "' u 1:4 w l lc rgb 'blue' ti 'GCMT syn'\n"
				);
		pw.close();
	}
	
	private RealVector[] stack(SourceTimeFunction stf, List<TimewindowInformation> orderedTimewindows
			, List<Trace> synTraces, List<Trace> obsTraces) {
		RealVector[] obsSynStacks = new RealVector[2];
		obsSynStacks[0] = new ArrayRealVector((int) (100 * samplingHz));
		obsSynStacks[1] = new ArrayRealVector((int) (100 * samplingHz));
		RealVector obsStack = new ArrayRealVector((int) (100 * samplingHz));
		RealVector synStack = new ArrayRealVector((int) (100 * samplingHz));
		for (int i = 0; i < orderedTimewindows.size(); i++) {
			double[] synConvolved = stf.convolve(synTraces.get(i).getY());
			
			TimewindowInformation timewindow = orderedTimewindows.get(i);
			
			Trace obsTrace = obsTraces.get(i);
			Trace synTraceConvolved = new Trace(synTraces.get(i).getX()
					, synConvolved);
			
			Trace tmpObsTrace = obsTrace.cutWindow(timewindow.getStartTime() - 10., timewindow.getEndTime() + 10.);
			synTraceConvolved = synTraceConvolved.cutWindow(timewindow);
			Trace tmpSynTrace = synTraceConvolved;
			
			double shift = tmpObsTrace.findBestShift(tmpSynTrace);
			
			obsTrace = obsTrace.shiftX(-shift).cutWindow(timewindow);
			
			RealVector synVector = synTraceConvolved.getYVector();
			RealVector obsVector = obsTrace.getYVector();
			
			
			if (synVector.getDimension() == obsVector.getDimension() + 1)
				synVector = synVector.getSubVector(0, obsVector.getDimension());
			if (synVector.getDimension() == obsVector.getDimension() - 1)
				obsVector = obsVector.getSubVector(0, synVector.getDimension());
			
			double linfinv = 1. / obsVector.getLInfNorm();
			obsVector = obsVector.mapMultiply(linfinv);
			synVector = synVector.mapMultiply(linfinv);
			if (obsStack.getDimension() < obsVector.getDimension()) {
				obsStack = obsStack.add(obsVector.getSubVector(0, obsStack.getDimension()));
				synStack = synStack.add(synVector.getSubVector(0, synStack.getDimension()));
			}
			else if (obsStack.getDimension() > obsVector.getDimension()) {
				obsStack = obsStack.getSubVector(0, obsVector.getDimension())
						.add(obsVector);
				synStack = synStack.getSubVector(0, obsVector.getDimension())
						.add(synVector);
			}
			else {
				obsStack = obsStack.add(obsVector);
				synStack = synStack.add(synVector);
			}
		}
		
		obsSynStacks[0] = obsStack;
		obsSynStacks[1] = synStack;
		
		return obsSynStacks;
	}
	
}
