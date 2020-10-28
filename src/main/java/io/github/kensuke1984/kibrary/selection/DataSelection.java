package io.github.kensuke1984.kibrary.selection;


import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.timewindow.Timewindow;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.*;
import io.github.kensuke1984.anisotime.Phase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Precision;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 理論波形と観測波形の比較から使えるものを選択する。<br>
 * workDir以下にあるイベントディレクトリの中から選ぶ<br>
 * 振幅比、correlation、variance<br>
 * <p>
 * {@link TimewindowInformationFile} necessary.
 *
 * @author Kensuke Konishi
 * @version 0.1.2.1
 * @author anselme add additional selection critera
 */
public class DataSelection implements Operation {

	private Set<EventFolder> eventDirs;
	private String dateStr;
	private Set<SACComponent> components;
	private Path obsPath;
	private Path synPath;
	private boolean convolute;
	private Path timewindowInformationFilePath;
	private Path staticCorrectionInformationFilePath;
	private boolean excludeSurfaceWave;
	/**
	 * Minimum correlation coefficients
	 */
	private double minCorrelation;
	/**
	 * Maximum correlation coefficients
	 */
	private double maxCorrelation;
	/**
	 * Minimum variance
	 */
	private double minVariance;
	/**
	 * Maximum variance
	 */
	private double maxVariance;
	/**
	 * amplitude のしきい値
	 */
	private double ratio;
	private double maxStaticShift;
	private double minSNratio;
	private double minDistance;
	private boolean SnScSnPair;
	private Set<TimewindowInformation> sourceTimewindowInformationSet;
	private Set<TimewindowInformation> goodTimewindowInformationSet;
	private List<DataSelectionInformation> dataSelectionInfo;
	private Path outputGoodWindowPath;
	private Set<StaticCorrection> staticCorrectionSet;
	
	/**
	 * ID for static correction and time window information Default is station
	 * name, global CMT id, component, start time.
	 */
	private BiPredicate<StaticCorrection, TimewindowInformation> isPair = (s,
			t) -> s.getStation().equals(t.getStation()) && s.getGlobalCMTID().equals(t.getGlobalCMTID())
					&& s.getComponent() == t.getComponent() && t.getStartTime() < s.getSynStartTime() + 1.01 && t.getStartTime() > s.getSynStartTime() - 1.01;
	private BiPredicate<StaticCorrection, TimewindowInformation> isPair_isotropic = (s,
			t) -> s.getStation().equals(t.getStation()) && s.getGlobalCMTID().equals(t.getGlobalCMTID())
					&& (t.getComponent() == SACComponent.R ? s.getComponent() == SACComponent.T : s.getComponent() == t.getComponent()) 
					&& t.getStartTime() < s.getSynStartTime() + 1.01 && t.getStartTime() > s.getSynStartTime() - 1.01;
	private BiPredicate<StaticCorrection, TimewindowInformation> isPairRecord = (s,
			t) -> s.getStation().equals(t.getStation()) && s.getGlobalCMTID().equals(t.getGlobalCMTID())
					&& s.getComponent() == t.getComponent();
	private Path workPath;
    private Properties property;
			
	public DataSelection(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
	}	
		
	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(DataSelection.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan DataSelection");
			pw.println("##Path of a working folder (.)");
			pw.println("#workPath");
			pw.println("##Sac components to be used (Z R T)");
			pw.println("#components");
			pw.println("##Path of a root folder containing observed dataset (.)");
			pw.println("#obsPath");
			pw.println("##Path of a root folder containing synthetic dataset (.)");
			pw.println("#synPath");
			pw.println("##boolean convolute (true)");
			pw.println("#convolute");
			pw.println("##Path of a time window information file, must be defined");
			pw.println("#timewindowInformationFilePath timewindow.dat");
			pw.println("##Path of a static correction file");
			pw.println("##If you do not want to consider static correction, then comment out the next line");
			pw.println("#staticCorrectionInformationFilePath staticCorrection.dat");
			pw.println("##Reject data with static correction greater than maxStaticShift (10.)");
			pw.println("#maxStaticShift");
			pw.println("##double sacSamplingHz (20)");
			pw.println("#sacSamplingHz cant change now");
			pw.println("##double minCorrelation (0)");
			pw.println("#minCorrelation");
			pw.println("##double maxCorrelation (1)");
			pw.println("#maxCorrelation");
			pw.println("##double minVariance (0)");
			pw.println("#minVariance");
			pw.println("##double maxVariance (2)");
			pw.println("#maxVariance");
			pw.println("##double ratio (2)");
			pw.println("#ratio");
			pw.println("##double minSNratio (0)");
			pw.println("#minSNratio");
			pw.println("#boolean SnScSnPair (false). Impose (s)ScSn in time window set if and only if (s)Sn is in the dataset");
			pw.println("#SnScSnPair false");
			pw.println("##exclude surface wave (false)");
			pw.println("#excludeSurfaceWave");
			pw.println("#minDistance");
		}
		System.err.println(outPath + " is created.");
	}

    /**
     * @param args [parameter file name]
     * @throws Exception if an I/O happens
     */
	public static void main(String[] args) throws Exception {
		DataSelection ds = new DataSelection(Property.parse(args));
		long start = System.nanoTime();
		System.err.println(DataSelection.class.getName() + " is going");
		ds.run();
		System.err.println(
				DataSelection.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - start));
	}
	
	/**
	 * @param sac        {@link SACData} to cut
	 * @param timeWindow time window
	 * @return new Trace for the timewindow [tStart:tEnd]
	 */
	private static RealVector cutSAC(SACData sac, Timewindow timeWindow) {
		Trace trace = sac.createTrace();
		double tStart = timeWindow.getStartTime();
		double tEnd = timeWindow.getEndTime();
		return new ArrayRealVector(trace.cutWindow(tStart, tEnd).getY(), false);
	}
	
	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath")) property.setProperty("workPath", "");
		if (!property.containsKey("components")) property.setProperty("components", "Z R T");
		if (!property.containsKey("obsPath")) property.setProperty("obsPath", "");
		if (!property.containsKey("synPath")) property.setProperty("synPath", "");
		if (!property.containsKey("minCorrelation")) property.setProperty("minCorrelation", "0");
		if (!property.containsKey("maxCorrelation")) property.setProperty("maxCorrelation", "1");
		if (!property.containsKey("minVariance")) property.setProperty("minVariance", "0");
		if (!property.containsKey("maxVariance")) property.setProperty("maxVariance", "2");
		if (!property.containsKey("ratio")) property.setProperty("ratio", "2");
		if (!property.containsKey("minSNratio")) property.setProperty("minSNratio", "0");
		if (!property.containsKey("convolute")) property.setProperty("convolute", "true");
		if (!property.containsKey("SnScSnPair")) property.setProperty("SnScSnPair", "false");
		if (!property.containsKey("excludeSurfaceWave")) property.setProperty("excludeSurfaceWave", "false");
		if (!property.containsKey("maxStaticShift")) property.setProperty("maxStaticShift", "10.");
		if (!property.containsKey("minDistance")) property.setProperty("minDistance", "0.");
	}
	
	private void set() throws IOException {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));
		if (!Files.exists(workPath)) throw new NoSuchFileException(workPath + " (workPath)");

		obsPath = getPath("obsPath");
		synPath = getPath("synPath");
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());

		convolute = Boolean.parseBoolean(property.getProperty("convolute"));
		minCorrelation = Double.parseDouble(property.getProperty("minCorrelation"));
		maxCorrelation = Double.parseDouble(property.getProperty("maxCorrelation"));
		minVariance = Double.parseDouble(property.getProperty("minVariance"));
		maxVariance = Double.parseDouble(property.getProperty("maxVariance"));
		ratio = Double.parseDouble(property.getProperty("ratio"));
		timewindowInformationFilePath = getPath("timewindowInformationFilePath");
		if (property.containsKey("staticCorrectionInformationFilePath"))
			staticCorrectionInformationFilePath = getPath("staticCorrectionInformationFilePath");
		// sacSamplingHz
		// =Double.parseDouble(reader.getFirstValue("sacSamplingHz")); TODO
		// sacSamplingHz = 20;
		staticCorrectionSet = staticCorrectionInformationFilePath == null ? Collections.emptySet()
				: StaticCorrectionFile.read(staticCorrectionInformationFilePath);
		eventDirs = Utilities.eventFolderSet(obsPath);
		sourceTimewindowInformationSet = TimewindowInformationFile.read(timewindowInformationFilePath);
		dateStr = Utilities.getTemporaryString();
		outputGoodWindowPath = workPath.resolve("selectedTimewindow" + dateStr + ".dat");
		goodTimewindowInformationSet = Collections.synchronizedSet(new HashSet<>());
		SnScSnPair = Boolean.parseBoolean(property.getProperty("SnScSnPair"));
		minSNratio = Double.parseDouble(property.getProperty("minSNratio"));
		excludeSurfaceWave = Boolean.parseBoolean(property.getProperty("excludeSurfaceWave"));
		dataSelectionInfo = new ArrayList<>();
		maxStaticShift = Double.parseDouble(property.getProperty("maxStaticShift"));
		minDistance = Double.parseDouble(property.getProperty("minDistance"));
	}
	
	private void output() throws IOException {
		TimewindowInformationFile.write(goodTimewindowInformationSet, outputGoodWindowPath);
	}
	
	private StaticCorrection getStaticCorrection(TimewindowInformation window) {
		List<StaticCorrection> corrs = staticCorrectionSet.stream().filter(s -> isPairRecord.test(s, window)).collect(Collectors.toList());
		if (corrs.size() != 1) throw new RuntimeException("Found no, or more than 1 static correction for window " + window);
		return corrs.get(0);
	}
	
    /**
     * @param timewindow timewindow to shift
     * @return if there is time shift information for the input timewindow, then
     * creates new timewindow and returns it, otherwise, just returns
     * the input one.
     */
	private TimewindowInformation shift(TimewindowInformation timewindow) {
		if (staticCorrectionSet.isEmpty())
			return timewindow;
		StaticCorrection foundShift = getStaticCorrection(timewindow);
		double value = foundShift.getTimeshift();
		return new TimewindowInformation(timewindow.getStartTime() - value, timewindow.getEndTime() - value,
				foundShift.getStation(), foundShift.getGlobalCMTID(), foundShift.getComponent(), timewindow.getPhases());
	}
	
	private boolean check(PrintWriter writer, String stationName, GlobalCMTID id, SACComponent component,
			TimewindowInformation window, RealVector obsU, RealVector synU, double SNratio) throws IOException {
		if (obsU.getDimension() < synU.getDimension())
			synU = synU.getSubVector(0, obsU.getDimension() - 1);
		else if (synU.getDimension() < obsU.getDimension())
			obsU = obsU.getSubVector(0, synU.getDimension() - 1);

		// check
		double synMax = synU.getMaxValue();
		double synMin = synU.getMinValue();
		double obsMax = obsU.getMaxValue();
		double obsMin = obsU.getMinValue();
		double obs2 = obsU.dotProduct(obsU);
		double syn2 = synU.dotProduct(synU);
		double cor = obsU.dotProduct(synU);
		cor /= Math.sqrt(obs2 * syn2);
		double var = obs2 + syn2 - 2 * obsU.dotProduct(synU);
		double maxRatio = Precision.round(synMax / obsMax, 2);
		double minRatio = Precision.round(synMin / obsMin, 2);
		double absRatio = (-synMin < synMax ? synMax : -synMin) / (-obsMin < obsMax ? obsMax : -obsMin);
		var /= obs2;

		absRatio = Precision.round(absRatio, 2);
		var = Precision.round(var, 2);
		cor = Precision.round(cor, 2);
		
		SNratio = Precision.round(SNratio, 2);
		
		boolean isok = !(ratio < minRatio || minRatio < 1 / ratio || ratio < maxRatio || maxRatio < 1 / ratio
				|| ratio < absRatio || absRatio < 1 / ratio || cor < minCorrelation || maxCorrelation < cor
				|| var < minVariance || maxVariance < var
				|| SNratio < minSNratio);
		
		Phases phases = new Phases(window.getPhases());
		
		if (window.getPhases().length == 0) {
			System.out.println(window);
		}
		else { 
			writer.println(stationName + " " + id + " " + component + " " + phases + " " + isok + " " + absRatio + " " + maxRatio + " "
				+ minRatio + " " + var + " " + cor + " " + SNratio);
		
			dataSelectionInfo.add(new DataSelectionInformation(window, var, cor, maxRatio, minRatio, absRatio, SNratio));
		}
		
		return isok;
	}
	
	/**
	 * @param sac
	 * @param component
	 * @return
	 * @author anselme
	 */
	private double noisePerSecond(SACData sac, SACComponent component) {
		double len = 50;
		double distance = sac.getValue(SACHeaderEnum.GCARC);
		double depth = sac.getValue(SACHeaderEnum.EVDP);
		double firstArrivalTime = 0;
		try {
			TauP_Time timeTool = new TauP_Time("prem");
			switch (component) {
			case T:
				timeTool.parsePhaseList("S, Sdiff, s");
				timeTool.setSourceDepth(depth);
				timeTool.calculate(distance);
				if (timeTool.getNumArrivals() == 0)
					throw new IllegalArgumentException("No arrivals for " + sac.getStation() + " " + sac.getGlobalCMTID() 
							+ " " + String.format("(%.2f deg, %.2f km)", distance, depth));
				firstArrivalTime = timeTool.getArrival(0).getTime();
				break;
			case Z:
			case R:
				timeTool.parsePhaseList("P, Pdiff, p");
				timeTool.setSourceDepth(depth);
				timeTool.calculate(distance);
				if (timeTool.getNumArrivals() == 0)
					throw new IllegalArgumentException("No arrivals for " + sac.getStation() + " " + sac.getGlobalCMTID()
							+ " " + String.format("(%.2f deg, %.2f km)", distance, depth));
				firstArrivalTime = timeTool.getArrival(0).getTime();
				break;
			default:
				break;
			}
		} catch (TauModelException e) {
			e.printStackTrace();
		}
	
		return sac.createTrace().cutWindow(firstArrivalTime - 20 - len, firstArrivalTime - 20).getYVector().getNorm() / len;
	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

	@Override
	public void run() throws Exception {
		int N_THREADS = Runtime.getRuntime().availableProcessors();

		ExecutorService exec = Executors.newFixedThreadPool(N_THREADS);
		
		for (EventFolder eventDirectory : eventDirs)
			exec.execute(new Worker(eventDirectory));

		exec.shutdown();
		while (!exec.isTerminated())
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		
		Path infoOutpath = workPath.resolve("dataSelection" + Utilities.getTemporaryString() + ".inf");
		try {
			DataSelectionInformationFile.write(infoOutpath, dataSelectionInfo);
		} catch (IOException e) {
			System.err.println("IOException: " + e.getMessage());
		}
		
		System.err.println();
		output();
	}
	
	private class Worker implements Runnable {

		private Set<SACFileName> obsFiles;
		private EventFolder obsEventDirectory;
		private EventFolder synEventDirectory;

		private GlobalCMTID id;

		private Worker(EventFolder ed) throws IOException {
			this.obsEventDirectory = ed;
			(obsFiles = ed.sacFileSet()).removeIf(n -> !n.isOBS());
			id = ed.getGlobalCMTID();
			synEventDirectory = new EventFolder(synPath.resolve(ed.getName()));
			if (!synEventDirectory.exists())
				return;
		}
		
		private Set<TimewindowInformation> imposeSn_ScSnPair(Set<TimewindowInformation> info) {
			Set<TimewindowInformation> infoNew = new HashSet<>();
			if (info.stream().map(tw -> tw.getStation()).distinct().count() > 1)
				throw new RuntimeException("Info should contain time windows for a unique record");
			if (info.stream().map(tw -> tw.getGlobalCMTID()).distinct().count() > 1)
				throw new RuntimeException("Info should contain time windows for a unique record");
			Map<Phases, TimewindowInformation> map = new HashMap<>();
			info.stream().forEach(tw -> map.put(new Phases(tw.getPhases()), tw));
			for (int i = 1; i <= 4; i++) {
				Phases phase = new Phases(new Phase[] {Phase.create(new String(new char[i]).replace("\0", "S"))});
				Phases cmbPhase = new Phases(new Phase[] {Phase.create(new String(new char[i]).replace("\0", "ScS"))});
				Phases depthPhase = new Phases(new Phase[] {Phase.create("s" + phase)});
				Phases cmbDepthPhase = new Phases(new Phase[] {Phase.create("s" + cmbPhase)});
				
				if (map.containsKey(phase) && map.containsKey(cmbPhase)) {
					infoNew.add(map.get(phase));
					infoNew.add(map.get(cmbPhase));
				}
				if (map.containsKey(depthPhase) && map.containsKey(cmbDepthPhase)) {
					infoNew.add(map.get(depthPhase));
					infoNew.add(map.get(cmbDepthPhase));
				}
			}
			Phases cmbMerge = new Phases(new Phase[] {Phase.ScS, Phase.S});
			Phases depthCMBMerge = new Phases(new Phase[] {Phase.create("sScS"), Phase.create("sS")});
			if (map.containsKey(cmbMerge))
				infoNew.add(map.get(cmbMerge));
			if (map.containsKey(depthCMBMerge))
				infoNew.add(map.get(depthCMBMerge));
			return infoNew;
		}

		@Override
		public void run() {
			if (!synEventDirectory.exists()) {
				try {
					FileUtils.moveDirectoryToDirectory(obsEventDirectory, workPath.resolve("withoutSyn").toFile(),
							true);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return;
			}
			try (PrintWriter lpw = new PrintWriter(
					Files.newBufferedWriter(obsEventDirectory.toPath().resolve("stationList" + dateStr + ".txt"),
							StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
				// all the observed files
				if (convolute)
					lpw.println("#convolved");
				else
					lpw.println("#not convolved");
				lpw.println("#s e c phase use ratio(syn/obs){abs max min} variance correlation SNratio");

				for (SACFileName obsName : obsFiles) {
					// check components
					if (!components.contains(obsName.getComponent()))
						continue;
					String stationName = obsName.getStationName();
					SACComponent component = obsName.getComponent();
					// double timeshift = 0;
					SACExtension synExt = convolute ? SACExtension.valueOfConvolutedSynthetic(component)
							: SACExtension.valueOfSynthetic(component);

					SACFileName synName = new SACFileName(synEventDirectory, stationName + "." + id + "." + synExt);
					if (!synName.exists()) {
						System.err.println("Ignoring non-existing synthetics " + synName);
						continue;
					}

					// synthetic sac
					SACData obsSac = obsName.read();
					SACData synSac = synName.read();
					
					stationName = obsSac.getStation().getName() + "_" + obsSac.getStation().getNetwork();

					Station station = obsSac.getStation();
					//
					if (synSac.getValue(SACHeaderEnum.DELTA) != obsSac.getValue(SACHeaderEnum.DELTA)) {
						System.err.println("Ignoring differing DELTA " + obsName);
						continue;
					}
					
					double distance = Math.toDegrees(obsSac.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(station.getPosition()));
					if (distance < minDistance)
						continue;

					// Pickup a time window of obsName
					Set<TimewindowInformation> windowInformations = sourceTimewindowInformationSet
							.stream().filter(info -> info.getStation().equals(station)
									&& info.getGlobalCMTID().equals(id) && info.getComponent() == component)
							.collect(Collectors.toSet());

					if (windowInformations.isEmpty())
						continue;
					
					// noise per second (in obs)
					double noise = noisePerSecond(obsSac, component);
					
					// Traces
					Trace synTrace = synSac.createTrace();
					
					Set<TimewindowInformation> tmpGoodWindows = new HashSet<>();
					for (TimewindowInformation window : windowInformations) {
						if (window.getEndTime() > synSac.getValue(SACHeaderEnum.E) - 10)
							continue;
						double shift = 0.;
						if (!staticCorrectionSet.isEmpty()) {
							StaticCorrection foundShift = getStaticCorrection(window);
							shift = foundShift.getTimeshift();
							//remove static shift of 10 s (maximum range of the static correction)
						}
						if (Math.abs(shift) > maxStaticShift)
							continue;
						// remove surface wave from window
						if (excludeSurfaceWave) {
							SurfaceWaveDetector detector = new SurfaceWaveDetector(synTrace, 20.);
							Timewindow surfacewaveWindow = detector.getSurfaceWaveWindow();
							
							if (surfacewaveWindow != null) {
								double endTime = window.getEndTime();
								double startTime = window.getStartTime();
								if (startTime >= surfacewaveWindow.getStartTime() && endTime <= surfacewaveWindow.getEndTime())
									continue;
								if (endTime > surfacewaveWindow.getStartTime() && startTime < surfacewaveWindow.getStartTime())
									endTime = surfacewaveWindow.getStartTime();
								if (startTime < surfacewaveWindow.getEndTime() && endTime > surfacewaveWindow.getEndTime())
									startTime = surfacewaveWindow.getEndTime();
								
								window = new TimewindowInformation(startTime
										, endTime, window.getStation(), window.getGlobalCMTID()
										, window.getComponent(), window.getPhases());
							}
						}
						
						TimewindowInformation shiftedWindow = new TimewindowInformation(window.getStartTime() - shift
								, window.getEndTime() - shift, window.getStation()
								, window.getGlobalCMTID(), window.getComponent(), window.getPhases());
						
						RealVector synU = cutSAC(synSac, window);
						RealVector obsU = cutSAC(obsSac, shiftedWindow);
						
						// signal-to-noise ratio
						double signal = obsU.getNorm() / (window.getEndTime() - window.getStartTime());
						double SNratio = signal / noise;
						
						if (check(lpw, stationName, id, component, window, obsU, synU, SNratio)) {
							if (Stream.of(window.getPhases()).filter(p -> p == null).count() > 0) {
								System.out.println(window);
							}
							tmpGoodWindows.add(window);
						}
					}
					if (SnScSnPair)
						tmpGoodWindows = imposeSn_ScSnPair(tmpGoodWindows);
					for (TimewindowInformation window : tmpGoodWindows)
						goodTimewindowInformationSet.add(window);
					
				}
				
				lpw.close();
				// spw.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("error on " + obsEventDirectory);
			}
			System.err.print(".");
			// System.out.println(obsEventDirectory + " is done");
		}
	}
	
}
