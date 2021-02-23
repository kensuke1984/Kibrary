package io.github.kensuke1984.kibrary.waveformdata;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.anisotime.PhasePart;
import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.FujiStaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.inversion.addons.RandomNoiseMaker;
import io.github.kensuke1984.kibrary.math.FourierTransform;
import io.github.kensuke1984.kibrary.math.HilbertTransform;
import io.github.kensuke1984.kibrary.math.Interpolation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Creates dataset containing observed and synthetic waveforms. <br>
 * The write is a set of an ID and waveform files.
 * <p>
 * Observed and synthetic waveforms in SAC files are collected from the obsDir
 * and synDir, respectively. Only SAC files, which sample rates are
 * {@link #sacSamplingHz}, are used. Both
 * folders must have event folders inside which have waveforms.
 * <p>
 * The static correction is applied as described in {@link StaticCorrection}
 * <p>
 * The sample rates of the data is
 * {@link #finalSamplingHz}.<br>
 * Timewindow information in {@link #timewindowPath} is used for cutting windows.
 * <p>
 * Only pairs of a seismic source and a receiver with both an observed and
 * synthetic waveform are collected.
 * <p>
 * This class does not apply a digital filter, but extract information about
 * pass band written in SAC files.
 * <p>
 * TODO <b> Assume that there are no stations with same name but different
 * network in one event</b>
 *
 * @author Kensuke Konishi
 * @version 0.2.3
 */
/**
 * @author anselme
 *
 */
public class ObservedSyntheticDatasetMaker implements Operation {

	private Path workPath;
	private Properties PROPERTY;
	
	/**
	 * components to be included in the dataset
	 */
	private Set<SACComponent> components;
	/**
	 * {@link Path} of a root folder containing observed dataset
	 */
	private Path obsPath;
	/**
	 * {@link Path} of a root folder containing synthetic dataset
	 */
	private Path synPath;
	/**
	 * {@link Path} of a timewindow information file
	 */
	private Path timewindowPath;
	/**
	 * {@link Path} of a timewindow information file for a reference phase use to correct spectral amplitude
	 */
	private Path timewindowRefPath;
	/**
	 * {@link Path} of a static correction file
	 */
	private Path staticCorrectionPath;
	/**
	 * {@link Path} of time shifts due to the 3-D mantle
	 */
	private Path mantleCorrectionPath;
	/**
	 * Sacのサンプリングヘルツ （これと異なるSACはスキップ）
	 */
	private double sacSamplingHz;
	/**
	 * 切り出すサンプリングヘルツ
	 */
	private double finalSamplingHz;
	/**
	 * if it is true, the dataset will contain synthetic waveforms after
	 * convolution
	 */
	private boolean convolute;
	/**
	 * If it corrects time
	 */
	private boolean timeCorrection;
	/**
	 * if it corrects amplitude ratio
	 */
	private boolean amplitudeCorrection;
	/**
	 * [bool] time-shift data to correct for 3-D mantle
	 */
	private boolean correctMantle;
	private Set<StaticCorrection> staticCorrectionSet;
	private Set<StaticCorrection> mantleCorrectionSet;
	private Set<TimewindowInformation> timewindowInformationSet;
	private Set<TimewindowInformation> timewindowRefInformationSet;
	private Set<EventFolder> eventDirs;
	private Set<Station> stationSet;
	private Set<GlobalCMTID> idSet;
	private Phase[] phases;
	private double[][] periodRanges;
	private double noisePower;
	private int finalFreqSamplingHz;
	/**
	 * low frequency cut-off for spectrum data
	 */
	double lowFreq;
	/**
	 * high frequency cut-off for spectrum data
	 */
	double highFreq;
	/**
	 * minimum epicentral distance
	 */
	private double minDistance;
	/**
	 * [bool] add white noise to synthetics data (for synthetic tests)
	 */
	private boolean addNoise;
	/**
	 * event-averaged amplitude corrections, used if amplitudeCorrection is False
	 */
	private Map<GlobalCMTID, Double> amplitudeCorrEventMap;
	/**
	 * writers
	 */
	private WaveformDataWriter dataWriter;
	private WaveformDataWriter envelopeWriter;
	private WaveformDataWriter spcAmpWriter;
	private WaveformDataWriter spcReWriter;
	private WaveformDataWriter spcImWriter;
	private WaveformDataWriter hyWriter;
	/**
	 * number of OUTPUT pairs. (excluding ignored traces)
	 */
	private AtomicInteger numberOfPairs = new AtomicInteger();
	/**
	 * ID for static correction and time window information Default is station
	 * name, global CMT id, component.
	 */
	private BiPredicate<StaticCorrection, TimewindowInformation> isPair = (s,
			t) -> s.getStation().equals(t.getStation()) && s.getGlobalCMTID().equals(t.getGlobalCMTID())
					&& s.getComponent() == t.getComponent() && t.getStartTime() < s.getSynStartTime() + 1.01 && t.getStartTime() > s.getSynStartTime() - 1.01;
	
	private BiPredicate<StaticCorrection, TimewindowInformation> isPair2 = (s,
			t) -> s.getStation().equals(t.getStation()) && s.getGlobalCMTID().equals(t.getGlobalCMTID())
					&& s.getComponent() == t.getComponent();
			
	private BiPredicate<StaticCorrection, TimewindowInformation> isPair_isotropic = (s,
			t) -> s.getStation().equals(t.getStation()) && s.getGlobalCMTID().equals(t.getGlobalCMTID())
				&& (t.getComponent() == SACComponent.R ? s.getComponent() == SACComponent.T : s.getComponent() == t.getComponent()) 
				&& t.getStartTime() < s.getSynStartTime() + 1.01 && t.getStartTime() > s.getSynStartTime() - 1.01;

	private BiPredicate<StaticCorrection, TimewindowInformation> isPair_record = (s,
			t) -> s.getStation().equals(t.getStation()) && s.getGlobalCMTID().equals(t.getGlobalCMTID())
					&& s.getComponent() == t.getComponent();
	
	public ObservedSyntheticDatasetMaker(Properties property) throws IOException {
		this.PROPERTY = (Properties) property.clone();
		set();
	}

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths
				.get(ObservedSyntheticDatasetMaker.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan ObservedSyntheticDatasetMaker");
			pw.println("##Path of a working directory (.)");
			pw.println("#workPath");
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("##Path of a root folder containing observed dataset (.)");
			pw.println("#obsPath");
			pw.println("##Path of a root folder containing synthetic dataset (.)");
			pw.println("#synPath");
			pw.println("##boolean convolulte (true)");
			pw.println("#convolute");
			pw.println("##boolean timeCorrection (false)");
			pw.println("#timeCorrection");
			pw.println("##boolean amplitudeCorrection (false)");
			pw.println("#amplitudeCorrection");
			pw.println("##Path of a timewindow information file, must be defined");
			pw.println("#timewindowPath timewindow.dat");
			pw.println("##Path of a timewindow information file for a reference phase use to correct spectral amplitude, can be ignored");
			pw.println("#timewindowRefPath ");
			pw.println("##Path of a static correction file, ");
			pw.println("##if any of the corrections are true, the path must be defined");
			pw.println("#staticCorrectionPath staticCorrection.dat");
			pw.println("##double value of sac sampling Hz (20) can't be changed now");
			pw.println("#sacSamplingHz the value will be ignored");
			pw.println("##double value of sampling Hz in output files (1)");
			pw.println("#finalSamplingHz");
			pw.println("#minDistance");
			pw.println("#addNoise false");
			pw.println("#correctMantle false");
			pw.println("#mantleCorrectionPath mantleCorrectionPath.dat");
			pw.println("#lowFreq");
			pw.println("#highFreq");
			pw.println("##Add noise for synthetic test");
			pw.println("#addNoise");
			pw.println("#noisePower");
		}
		System.err.println(outPath + " is created.");
	}
	
	/**
	 * 
	 * @param args [a property file name]
	 * @throws Exception if any
	 */
	public static void main(String[] args) throws Exception {
		Properties property = new Properties();
		if (args.length == 0) property.load(Files.newBufferedReader(Operation.findPath()));
		else if (args.length == 1) property.load(Files.newBufferedReader(Paths.get(args[0])));
		else throw new IllegalArgumentException("too many arguments. It should be 0 or 1(property file name)");
		ObservedSyntheticDatasetMaker osdm = new ObservedSyntheticDatasetMaker(property);
		long startT = System.nanoTime();
		System.err.println(ObservedSyntheticDatasetMaker.class.getName() + " is running.");
		osdm.run();
		System.err.println(ObservedSyntheticDatasetMaker.class.getName() + " finished in "
				+ Utilities.toTimeString(System.nanoTime() - startT));
	}

	private void checkAndPutDefaults() {
		if (!PROPERTY.containsKey("workPath")) PROPERTY.setProperty("workPath", ".");
		if (!PROPERTY.containsKey("obsPath")) PROPERTY.setProperty("obsPath", "");
		if (!PROPERTY.containsKey("synPath")) PROPERTY.setProperty("synPath", "");
		if (!PROPERTY.containsKey("components")) PROPERTY.setProperty("components", "Z R T");
		if (!PROPERTY.containsKey("convolute")) PROPERTY.setProperty("convolute", "true");
		if (!PROPERTY.containsKey("amplitudeCorrection")) PROPERTY.setProperty("amplitudeCorrection", "false");
		if (!PROPERTY.containsKey("timeCorrection")) PROPERTY.setProperty("timeCorrection", "false");
		if (!PROPERTY.containsKey("timewindowPath"))
			throw new IllegalArgumentException("There is no information about timewindowPath.");
		if (!PROPERTY.containsKey("sacSamplingHz")) PROPERTY.setProperty("sacSamplingHz", "20");
		if (!PROPERTY.containsKey("finalSamplingHz")) PROPERTY.setProperty("finalSamplingHz", "1");
		if (!PROPERTY.containsKey("minDistance")) PROPERTY.setProperty("minDistance", "0.");
		if (!PROPERTY.containsKey("addNoise")) PROPERTY.setProperty("addNoise", "false");
		if (!PROPERTY.containsKey("correctMantle")) PROPERTY.setProperty("correctMantle", "false");
		if (!PROPERTY.containsKey("lowFreq")) PROPERTY.setProperty("lowFreq", "0.01");
		if (!PROPERTY.containsKey("highFreq")) PROPERTY.setProperty("highFreq", "0.08");
		if (!PROPERTY.containsKey("noisePower")) PROPERTY.setProperty("noisePower", "1");
	}

	private void set() throws NoSuchFileException {
		checkAndPutDefaults();
		workPath = Paths.get(PROPERTY.getProperty("workPath"));
		if (!Files.exists(workPath)) throw new RuntimeException("The workPath: " + workPath + " does not exist");
		obsPath = getPath("obsPath");
		synPath = getPath("synPath");
		components = Arrays.stream(PROPERTY.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());
		timewindowPath = getPath("timewindowPath");
		timeCorrection = Boolean.parseBoolean(PROPERTY.getProperty("timeCorrection"));
		amplitudeCorrection = Boolean.parseBoolean(PROPERTY.getProperty("amplitudeCorrection"));

		if (timeCorrection || amplitudeCorrection) {
			if (!PROPERTY.containsKey("staticCorrectionPath"))
				throw new RuntimeException("staticCorrectionPath is blank");
			staticCorrectionPath = getPath("staticCorrectionPath");
			if (!Files.exists(staticCorrectionPath))
				throw new NoSuchFileException(staticCorrectionPath.toString());
		}
		
		correctMantle = Boolean.parseBoolean(PROPERTY.getProperty("correctMantle"));
		if (correctMantle) mantleCorrectionPath = getPath("mantleCorrectionPath");

		convolute = Boolean.parseBoolean(PROPERTY.getProperty("convolute"));

		// sacSamplingHz
		// =Double.parseDouble(reader.getFirstValue("sacSamplingHz")); TODO
		sacSamplingHz = 20;
		finalSamplingHz = Double.parseDouble(PROPERTY.getProperty("finalSamplingHz"));
		
		minDistance = Double.parseDouble(PROPERTY.getProperty("minDistance"));
		
		addNoise = Boolean.parseBoolean(PROPERTY.getProperty("addNoise"));
		if (addNoise) System.out.println("Adding noise");
		
		if (PROPERTY.containsKey("timewindowRefPath")) timewindowRefPath = getPath("timewindowRefPath");
		lowFreq = Double.parseDouble(PROPERTY.getProperty("lowFreq"));
		highFreq = Double.parseDouble(PROPERTY.getProperty("highFreq"));
		
		noisePower = Double.parseDouble(PROPERTY.getProperty("noisePower"));
		System.out.println("Noise power: " + noisePower);
		finalFreqSamplingHz = 8;
	}

	private void readPeriodRanges() {
		try {
			List<double[]> ranges = new ArrayList<>();
			Set<SACFileName> sacfilenames = Utilities.sacFileNameSet(obsPath).stream().limit(20).collect(Collectors.toSet());
			for (SACFileName name : sacfilenames) {
				if (!name.isOBS()) continue;
				SACHeaderData header = name.readHeader();
				double[] range = new double[] { header.getValue(SACHeaderEnum.USER0),
						header.getValue(SACHeaderEnum.USER1) };
				boolean exists = false;
				if (ranges.size() == 0) ranges.add(range);
				for (int i = 0; !exists && i < ranges.size(); i++)
					if (Arrays.equals(range, ranges.get(i))) exists = true;
				if (!exists) ranges.add(range);
			}
			periodRanges = ranges.toArray(new double[0][]);
		} catch (Exception e) {
			throw new RuntimeException("Error in reading period ranges from SAC files.");
		}
	}

	@Override
	public void run() throws Exception {
		if (20 % finalSamplingHz != 0)
			throw new RuntimeException("Must choose a finalSamplingHz that divides 20");
		
		timewindowInformationSet = TimewindowInformationFile.read(timewindowPath)
				.stream().filter(tw -> {
					double distance = Math.toDegrees(tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition()));
					if (distance < minDistance)
						return false;
					return true;
				}).collect(Collectors.toSet());
		
		if (timeCorrection || amplitudeCorrection) {
			Set<StaticCorrection> tmpset = StaticCorrectionFile.read(staticCorrectionPath);
			staticCorrectionSet = tmpset.stream()
					.filter(c -> timewindowInformationSet.parallelStream()
							.map(t -> isPair_record.test(c, t)).distinct().collect(Collectors.toSet()).contains(true))
					.collect(Collectors.toSet());
			
			// average amplitude correction
			amplitudeCorrEventMap = new HashMap<>();
			for (GlobalCMTID event : staticCorrectionSet.stream().map(s -> s.getGlobalCMTID()).collect(Collectors.toSet())) {
				double avgCorr = 0;
				Set<StaticCorrection> eventCorrs = staticCorrectionSet.stream()
						.filter(s -> s.getGlobalCMTID().equals(event)).collect(Collectors.toSet());
				for (StaticCorrection corr : eventCorrs)
					avgCorr += corr.getAmplitudeRatio();
				avgCorr /= eventCorrs.size();
				amplitudeCorrEventMap.put(event, avgCorr);
			}
		}
		
		if (correctMantle) {
			System.out.println("Using mantle corrections");
			mantleCorrectionSet = StaticCorrectionFile.read(mantleCorrectionPath);
		}

		// obsDirからイベントフォルダを指定
		eventDirs = Utilities.eventFolderSet(obsPath);
		
		if (timewindowRefPath != null)
			timewindowRefInformationSet = TimewindowInformationFile.read(timewindowRefPath)
				.stream().filter(tw -> {
					double distance = Math.toDegrees(tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition()));
					if (distance < minDistance)
						return false;
					return true;
				}).collect(Collectors.toSet());
		
		stationSet = timewindowInformationSet.stream().map(TimewindowInformation::getStation)
				.collect(Collectors.toSet());
		idSet = timewindowInformationSet.stream().map(TimewindowInformation::getGlobalCMTID)
				.collect(Collectors.toSet());
		phases = timewindowInformationSet.stream().map(TimewindowInformation::getPhases).flatMap(p -> Arrays.stream(p))
				.distinct().toArray(Phase[]::new);
		
		readPeriodRanges();
		
		int n = Runtime.getRuntime().availableProcessors();
		System.out.println("Running on " + n + " processors");
		ExecutorService execs = Executors.newFixedThreadPool(n);
		String dateStr = Utilities.getTemporaryString();
		Path waveIDPath = null;
		Path waveformPath = null;
		Path envelopeIDPath = null;
		Path envelopePath = null;
		Path hyIDPath = null;
		Path hyPath = null;
		Path spcAmpIDPath = null;
		Path spcAmpPath = null;
		Path spcReIDPath = null;
		Path spcRePath = null;
		Path spcImIDPath = null;
		Path spcImPath = null;
		
		waveIDPath = workPath.resolve("waveformID" + dateStr + ".dat");
		waveformPath = workPath.resolve("waveform" + dateStr + ".dat");
		envelopeIDPath = workPath.resolve("envelopeID" + dateStr + ".dat");
		envelopePath = workPath.resolve("envelope" + dateStr + ".dat");
		hyIDPath = workPath.resolve("hyID" + dateStr + ".dat");
		hyPath = workPath.resolve("hy" + dateStr + ".dat");
		spcAmpIDPath = workPath.resolve("spcAmpID" + dateStr + ".dat");
		spcAmpPath = workPath.resolve("spcAmp" + dateStr + ".dat");
		spcReIDPath = workPath.resolve("spcReID" + dateStr + ".dat");
		spcRePath = workPath.resolve("spcRe" + dateStr + ".dat");
		spcImIDPath = workPath.resolve("spcImID" + dateStr + ".dat");
		spcImPath = workPath.resolve("spcIm" + dateStr + ".dat");
		
		try (WaveformDataWriter bdw = new WaveformDataWriter(waveIDPath, waveformPath, stationSet, idSet,
				periodRanges, phases)) {
			envelopeWriter = new WaveformDataWriter(envelopeIDPath, envelopePath, stationSet, idSet,
					periodRanges, phases);
			hyWriter = new WaveformDataWriter(hyIDPath, hyPath, stationSet, idSet,
					periodRanges, phases);
			spcAmpWriter = new WaveformDataWriter(spcAmpIDPath, spcAmpPath,
					stationSet, idSet, periodRanges, phases);
			spcReWriter = new WaveformDataWriter(spcReIDPath, spcRePath,
					stationSet, idSet, periodRanges, phases);
			spcImWriter = new WaveformDataWriter(spcImIDPath, spcImPath,
					stationSet, idSet, periodRanges, phases);
			dataWriter = bdw;
			for (EventFolder eventDir : eventDirs)
				execs.execute(new Worker(eventDir));
			execs.shutdown();
			while (!execs.isTerminated())
				Thread.sleep(1000);
			envelopeWriter.close();
			hyWriter.close();
			spcAmpWriter.close();
			spcImWriter.close();
			spcReWriter.close();
			System.err.println("\n" + numberOfPairs.get() + " pairs of observed and synthetic waveforms are output.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private StaticCorrection getStaticCorrection(TimewindowInformation window) {
		List<StaticCorrection> corrs = staticCorrectionSet.stream().filter(s -> isPair_record.test(s, window)).collect(Collectors.toList());
		if (corrs.size() > 1)
			throw new RuntimeException("Found more than 1 static correction for window " + window);
		if (corrs.size() == 0)
			throw new RuntimeException("Found no static correction for window " + window);
		return corrs.get(0);
	}
	/**
	 * @param window
	 * @author anselme
	 * @return
	 */
	private StaticCorrection getMantleCorrection(TimewindowInformation window) {
		List<StaticCorrection> corrs = mantleCorrectionSet.stream().filter(s -> isPair_record.test(s, window)).collect(Collectors.toList());
		if (corrs.size() > 1)
			throw new RuntimeException("Found more than 1 mantle correction for window " + window);
		if (corrs.size() == 0)
			throw new RuntimeException("Found no mantle correction for window " + window);
		return corrs.get(0);
	}

	private double[] cutDataSac(SACData sac, double startTime, int npts) {
		Trace trace = sac.createTrace();
		int step = (int) (sacSamplingHz / finalSamplingHz);
		int startPoint = trace.getNearestXIndex(startTime);
		double[] waveData = trace.getY();
		return IntStream.range(0, npts).parallel().mapToDouble(i -> waveData[i * step + startPoint]).toArray();
	}
	/**
	 * @param sac
	 * @param startTime
	 * @param npts
	 * @author anselme
	 * @return
	 */
	private double[] cutEnvelopeSac(SACData sac, double startTime, int npts) {
		Trace trace = sac.createTrace();
		int step = (int) (sacSamplingHz / finalSamplingHz);
		int startPoint = trace.getNearestXIndex(startTime);
		HilbertTransform hilbert = new HilbertTransform(trace.getY());
		double[] waveData = hilbert.getEnvelope();
		return IntStream.range(0, npts).parallel().mapToDouble(i -> waveData[i * step + startPoint]).toArray();
	}
	/**
	 * @param sac
	 * @param startTime
	 * @param npts
	 * @author anselme
	 * @return
	 */
	private double[] cutHySac(SACData sac, double startTime, int npts) {
		Trace trace = sac.createTrace();
		int step = (int) (sacSamplingHz / finalSamplingHz);
		int startPoint = trace.getNearestXIndex(startTime);
		HilbertTransform hilbert = new HilbertTransform(trace.getY());
		double[] waveData = hilbert.getHy();
		return IntStream.range(0, npts).parallel().mapToDouble(i -> waveData[i * step + startPoint]).toArray();
	}
	/**
	 * @param sac
	 * @param startTime
	 * @param npts
	 * @author anselme
	 * @return
	 */
	private Trace cutSpcAmpSac(SACData sac, double startTime, int npts) {
		Trace trace = sac.createTrace();
		int step = (int) (sacSamplingHz / finalSamplingHz);
		int startPoint = trace.getNearestXIndex(startTime);
		double[] cutY = trace.getYVector().getSubVector(startPoint, npts * step).toArray();
		FourierTransform fourier = new FourierTransform(cutY, finalFreqSamplingHz);
		double df = fourier.getFreqIncrement(sacSamplingHz);
		if (highFreq > sacSamplingHz)
			throw new RuntimeException("f1 must be <= sacSamplingHz");
		int iStart = (int) (lowFreq / df) - 1;
		int fnpts = (int) ((highFreq - lowFreq) / df);
		double[] spcAmp = fourier.getLogA();
		return new Trace(IntStream.range(0, fnpts).mapToDouble(i -> (i + iStart) * df).toArray(),
			IntStream.range(0, fnpts).mapToDouble(i -> spcAmp[i + iStart]).toArray());
	}
	/**
	 * @param sac
	 * @param startTime
	 * @param npts
	 * @author anselme
	 * @return
	 */
	private Trace cutSpcAmpSacAddNoise(SACData sac, double startTime, int npts) {
		Trace trace = sac.createTrace();
		int step = (int) (sacSamplingHz / finalSamplingHz);
		int startPoint = trace.getNearestXIndex(startTime);
		double[] cutY = trace.getYVector().getSubVector(startPoint, npts * step).toArray();
		Trace tmp = createNoiseTrace(new ArrayRealVector(cutY).getLInfNorm());
		Trace noiseTrace = new Trace(trace.getX(), Arrays.copyOf(tmp.getY(), trace.getLength()));
		trace = trace.add(noiseTrace);
		cutY = trace.getYVector().getSubVector(startPoint, npts * step).toArray();
		FourierTransform fourier = new FourierTransform(cutY, finalFreqSamplingHz);
		double df = fourier.getFreqIncrement(sacSamplingHz);
		if (highFreq > sacSamplingHz)
			throw new RuntimeException("f1 must be <= sacSamplingHz");
		int iStart = (int) (lowFreq / df) - 1;
		int fnpts = (int) ((highFreq - lowFreq) / df);
		double[] spcAmp = fourier.getLogA();
		return new Trace(IntStream.range(0, fnpts).mapToDouble(i -> (i + iStart) * df).toArray(),
			IntStream.range(0, fnpts).mapToDouble(i -> spcAmp[i + iStart]).toArray());
	}
	/**
	 * @param spcAmp
	 * @param refSpcAmp
	 * @author anselme
	 * @return
	 */
	private double[] correctSpcAmp(Trace spcAmp, Trace refSpcAmp) {
		double[] spcAmpCorr = new double[spcAmp.getLength()];
		for (int i = 0; i < spcAmp.getLength(); i++) {
			double x = spcAmp.getXAt(i);
			int j0 = refSpcAmp.getNearestXIndex(x);
			int j1;
			if (j0 == 0) {
				j1 = 1;
			}
			else if (j0 == refSpcAmp.getLength() - 1) {
				j1 = refSpcAmp.getLength() - 2;
			}
			else
				j1 = refSpcAmp.getXAt(j0) < x ? j0 + 1 : j0 - 1;
			double refSpcAmpAti = Interpolation.linear(x, new double[] {refSpcAmp.getXAt(j0), refSpcAmp.getXAt(j1)}
				, new double[] {refSpcAmp.getYAt(j0), refSpcAmp.getYAt(j1)});
			spcAmpCorr[i] = spcAmp.getYAt(i) - refSpcAmpAti;
		}
		return spcAmpCorr;
	}
	/**
	 * @param sac
	 * @param startTime
	 * @param npts
	 * @author anselme
	 * @return
	 */
	private Complex[] cutSpcFySac(SACData sac, double startTime, int npts) {
		Trace trace = sac.createTrace();
		int step = (int) (sacSamplingHz / finalSamplingHz);
		int startPoint = trace.getNearestXIndex(startTime);
		double[] cutY = trace.getYVector().getSubVector(startPoint, npts * step).toArray();
		FourierTransform fourier = new FourierTransform(cutY, finalFreqSamplingHz);
		double df = fourier.getFreqIncrement(sacSamplingHz);
		if (highFreq > sacSamplingHz)
			throw new RuntimeException("f1 must be <= sacSamplingHz");
		int iStart = (int) (lowFreq / df) - 1;
		int fnpts = (int) ((highFreq - lowFreq) / df);
		Complex[] Fy = fourier.getFy();
		return IntStream.range(0, fnpts).parallel().mapToObj(i -> Fy[i + iStart])
				.collect(Collectors.toList()).toArray(new Complex[0]);
	}
	/**
	 * @param sac
	 * @param startTime
	 * @param npts
	 * @author anselme
	 * @return
	 */
	private double[] cutDataSacAddNoise(SACData sac, double startTime, int npts) {
		Trace trace = sac.createTrace();
		int step = (int) (sacSamplingHz / finalSamplingHz);
		int startPoint = trace.getNearestXIndex(startTime);
		double[] waveData = trace.getY();
		RealVector vector = new ArrayRealVector(IntStream.range(0, npts).parallel().mapToDouble(i -> waveData[i * step + startPoint]).toArray());
		Trace tmp = createNoiseTrace(vector.getLInfNorm());
		Trace noiseTrace = new Trace(trace.getX(), Arrays.copyOf(tmp.getY(), trace.getLength()));
		trace = trace.add(noiseTrace);
		
		double signal = trace.getYVector().getNorm() / trace.getLength();
		double noise = noiseTrace.getYVector().getNorm() / noiseTrace.getLength();
		double snratio = signal / noise;
		System.out.println("snratio " + snratio + " noise " + noise);
		
		double[] waveDataNoise = trace.getY();
		return IntStream.range(0, npts).parallel().mapToDouble(i -> waveDataNoise[i * step + startPoint]).toArray();
	}
	
	/**
	 * @param normalize
	 * @author anselme
	 * @return
	 */
	private Trace createNoiseTrace(double normalize) {
		double maxFreq = 0.125;
		double minFreq = 0.005;
		int np = 4;
		ButterworthFilter bpf = new BandPassFilter(2 * Math.PI * 0.05 * maxFreq, 2 * Math.PI * 0.05 * minFreq, np);
		Trace tmp = RandomNoiseMaker.create(1., sacSamplingHz, 1638.4, 512);
		double[] u = tmp.getY();
		RealVector uvec = new ArrayRealVector(bpf.applyFilter(u));
		return new Trace(tmp.getX(), uvec.mapMultiply(noisePower * normalize / uvec.getLInfNorm()).toArray());
	}

	@Override
	public Properties getProperties() {
		return (Properties) PROPERTY.clone();
	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}

	/**
	 * 与えられたイベントフォルダの観測波形と理論波形を書き込む 両方ともが存在しないと書き込まない
	 */
	private class Worker implements Runnable {

		private EventFolder OBS_EVENT_DIR;

		private Worker(EventFolder eventDir) {
			OBS_EVENT_DIR = eventDir;
		}

		@Override
		public void run() {
			Path synEventPath = synPath.resolve(OBS_EVENT_DIR.getGlobalCMTID().toString());
			if (!Files.exists(synEventPath))
				throw new RuntimeException(synEventPath + " does not exist.");

			Set<SACFileName> obsFiles;
			try {
				(obsFiles = OBS_EVENT_DIR.sacFileSet()).removeIf(sfn -> !sfn.isOBS());
			} catch (IOException e2) {
				e2.printStackTrace();
				return;
			}

			for (SACFileName obsFileName : obsFiles) {
				// データセットに含める成分かどうか
				if (!components.contains(obsFileName.getComponent())) continue;
				String stationName = obsFileName.getStationName();
				GlobalCMTID id = obsFileName.getGlobalCMTID();
				SACComponent component = obsFileName.getComponent();
				String name = convolute
						? stationName + "." + id + "." + SACExtension.valueOfConvolutedSynthetic(component)
						: stationName + "." + id + "." + SACExtension.valueOfSynthetic(component);
				SACFileName synFileName = new SACFileName(synEventPath.resolve(name));

				if (!synFileName.exists()) continue;

				Set<TimewindowInformation> windows = timewindowInformationSet.stream()
						.filter(info -> info.getStation().getName().equals(stationName))
						.filter(info -> info.getGlobalCMTID().equals(id))
						.filter(info -> info.getComponent() == component).collect(Collectors.toSet());

				// タイムウインドウの情報が入っていなければ次へ
				if (windows.isEmpty()) continue;
				
				SACData obsSac;
				try {
					obsSac = obsFileName.read();
				} catch (IOException e1) {
					System.err.println("error occured in reading " + obsFileName);
					e1.printStackTrace();
					continue;
				}

				SACData synSac;
				try {
					synSac = synFileName.read();
				} catch (IOException e1) {
					System.err.println("error occured in reading " + synFileName);
					e1.printStackTrace();
					continue;
				}

				// Sampling Hz of observed and synthetic must be same as the
				// value declared in the input file
				if (obsSac.getValue(SACHeaderEnum.DELTA) != 1 / sacSamplingHz
						&& obsSac.getValue(SACHeaderEnum.DELTA) == synSac.getValue(SACHeaderEnum.DELTA)) {
					System.err.println("Values of sampling Hz of observed and synthetic "
							+ (1 / obsSac.getValue(SACHeaderEnum.DELTA)) + ", "
							+ (1 / synSac.getValue(SACHeaderEnum.DELTA)) + " are invalid, they should be "
							+ sacSamplingHz);
					continue;
				}

				// bandpassの読み込み 観測波形と理論波形とで違えばスキップ
				double minPeriod = 0;
				double maxPeriod = Double.POSITIVE_INFINITY;
				if (obsSac.getValue(SACHeaderEnum.USER0) != synSac.getValue(SACHeaderEnum.USER0)
						|| obsSac.getValue(SACHeaderEnum.USER1) != synSac.getValue(SACHeaderEnum.USER1)) {
					System.err.println("band pass filter difference");
					continue;
				}
				minPeriod = obsSac.getValue(SACHeaderEnum.USER0) == -12345 ? 0 : obsSac.getValue(SACHeaderEnum.USER0);
				maxPeriod = obsSac.getValue(SACHeaderEnum.USER1) == -12345 ? 0 : obsSac.getValue(SACHeaderEnum.USER1);

				Station station = obsSac.getStation();

				for (TimewindowInformation window : windows) {
					int npts = (int) ((window.getEndTime() - window.getStartTime()) * finalSamplingHz);
					if (window.getEndTime() > synSac.getValue(SACHeaderEnum.E) - 10) continue;
					double startTime = window.getStartTime();
					double shift = 0;
					double ratio = 1;
					if (timeCorrection || amplitudeCorrection)
						try {
							StaticCorrection sc = getStaticCorrection(window);
							shift = timeCorrection ? sc.getTimeshift() : 0;
//							ratio = amplitudeCorrection ? sc.getAmplitudeRatio() : 1;
							ratio = amplitudeCorrection ? sc.getAmplitudeRatio() : amplitudeCorrEventMap.get(window.getGlobalCMTID());
						} catch (NoSuchElementException e) {
							System.err.println("There is no static correction information for\\n " + window);
							continue;
						}
					
					if (correctMantle)
						try {
							StaticCorrection sc = getMantleCorrection(window);
							shift += sc.getTimeshift();
						} catch (NoSuchElementException e) {
							System.err.println("There is no mantle correction information for\\n " + window);
							continue;
						}
					
					TimewindowInformation windowRef = null;
					int nptsRef = 0;
					if (timewindowRefInformationSet != null) {
						List<TimewindowInformation> tmpwindows = timewindowRefInformationSet.stream().filter(tw -> tw.getGlobalCMTID().equals(window.getGlobalCMTID())
								&& tw.getStation().equals(window.getStation())
								&& tw.getComponent().equals(window.getComponent())).collect(Collectors.toList());
						if (tmpwindows.size() != 1) {
							System.err.println("Reference timewindow does not exist " + window);
							continue;
						}
						else 
							windowRef = tmpwindows.get(0); 
						
						nptsRef = (int) ((windowRef.getEndTime() - windowRef.getStartTime()) * finalSamplingHz);
					}
					
					double[] obsData = null;
					if (addNoise)
						obsData = cutDataSacAddNoise(obsSac, startTime - shift, npts);
					else
						obsData = cutDataSac(obsSac, startTime - shift, npts);
					double[] synData = cutDataSac(synSac, startTime, npts);

					double[] obsEnvelope = cutEnvelopeSac(obsSac, startTime - shift, npts);
					double[] synEnvelope = cutEnvelopeSac(synSac, startTime, npts);
					
					double[] obsHy = cutHySac(obsSac, startTime - shift, npts);
					double[] synHy = cutHySac(synSac, startTime, npts);
					
					Trace obsSpcAmpTrace = null;
					Trace synSpcAmpTrace = null;
					
					if (addNoise) {
						obsSpcAmpTrace = cutSpcAmpSacAddNoise(obsSac, startTime - shift, npts);
						synSpcAmpTrace = cutSpcAmpSac(synSac, startTime, npts);
					}
					else {
						obsSpcAmpTrace = cutSpcAmpSac(obsSac, startTime - shift, npts);
						synSpcAmpTrace = cutSpcAmpSac(synSac, startTime, npts);
					}
					
					Complex[] obsFy = cutSpcFySac(obsSac, startTime - shift, npts);
					Complex[] synFy = cutSpcFySac(synSac, startTime, npts);
					
					double[] obsSpcRe = Arrays.stream(obsFy).mapToDouble(Complex::getReal).toArray();
					double[] synSpcRe = Arrays.stream(synFy).mapToDouble(Complex::getReal).toArray();
					
					double[] obsSpcIm = Arrays.stream(obsFy).mapToDouble(Complex::getImaginary).toArray();
					double[] synSpcIm = Arrays.stream(synFy).mapToDouble(Complex::getImaginary).toArray();
					
					double[] obsSpcAmp = null;
					double[] synSpcAmp = null;
					
					Trace refObsSpcAmpTrace = null;
					Trace refSynSpcAmpTrace = null;
					if (windowRef != null) {
						if (addNoise) {
							refObsSpcAmpTrace = cutSpcAmpSacAddNoise(obsSac, windowRef.getStartTime(), nptsRef);
							refSynSpcAmpTrace = cutSpcAmpSac(synSac, windowRef.getStartTime(), nptsRef);
						}
						else {
							refObsSpcAmpTrace = cutSpcAmpSac(obsSac, windowRef.getStartTime(), nptsRef);
							refSynSpcAmpTrace = cutSpcAmpSac(synSac, windowRef.getStartTime(), nptsRef);
						}
						
						if (amplitudeCorrection) {
							obsSpcAmp = correctSpcAmp(obsSpcAmpTrace, refObsSpcAmpTrace);
							synSpcAmp = correctSpcAmp(synSpcAmpTrace, refSynSpcAmpTrace);
						}
						else {
							obsSpcAmp = obsSpcAmpTrace.getY();
							synSpcAmp = synSpcAmpTrace.getY();
							double corrratio = amplitudeCorrEventMap.get(window.getGlobalCMTID());
							obsSpcAmp = Arrays.stream(obsSpcAmp).map(d -> d - Math.log(corrratio)).toArray();
						}
					}
					else {
						obsSpcAmp = obsSpcAmpTrace.getY();
						synSpcAmp = synSpcAmpTrace.getY();
					}
					
					double correctionRatio = ratio;
					
					Phase[] includePhases = window.getPhases();
					
					obsData = Arrays.stream(obsData).map(d -> d / correctionRatio).toArray();
					BasicID synID = new BasicID(WaveformType.SYN, finalSamplingHz, startTime, npts, station, id,
							component, minPeriod, maxPeriod, includePhases, 0, convolute, synData);
					BasicID obsID = new BasicID(WaveformType.OBS, finalSamplingHz, startTime - shift, npts, station, id,
							component, minPeriod, maxPeriod, includePhases, 0, convolute, obsData);
					
					obsEnvelope = Arrays.stream(obsEnvelope).map(d -> d / correctionRatio).toArray();
					BasicID synEnvelopeID = new BasicID(WaveformType.SYN, finalSamplingHz, startTime, npts, station, id,
							component, minPeriod, maxPeriod, includePhases, 0, convolute, synEnvelope);
					BasicID obsEnvelopeID = new BasicID(WaveformType.OBS, finalSamplingHz, startTime - shift, npts, station, id,
							component, minPeriod, maxPeriod, includePhases, 0, convolute, obsEnvelope);
					
					obsHy = Arrays.stream(obsHy).map(d -> d / correctionRatio).toArray();
					BasicID synHyID = new BasicID(WaveformType.SYN, finalSamplingHz, startTime, npts, station, id,
							component, minPeriod, maxPeriod, includePhases, 0, convolute, synHy);
					BasicID obsHyID = new BasicID(WaveformType.OBS, finalSamplingHz, startTime - shift, npts, station, id,
							component, minPeriod, maxPeriod, includePhases, 0, convolute, obsHy);
					
					int fnpts = synSpcAmp.length;
					
					BasicID synSpcAmpID = new BasicID(WaveformType.SYN, finalSamplingHz, startTime, fnpts, station, id,
							component, minPeriod, maxPeriod, includePhases, 0, convolute, synSpcAmp);
					BasicID obsSpcAmpID = new BasicID(WaveformType.OBS, finalSamplingHz, startTime - shift, fnpts, station, id,
							component, minPeriod, maxPeriod, includePhases, 0, convolute, obsSpcAmp);
					
					BasicID synSpcReID = new BasicID(WaveformType.SYN, finalSamplingHz, startTime, fnpts, station, id,
							component, minPeriod, maxPeriod, includePhases, 0, convolute, synSpcRe);
					BasicID obsSpcReID = new BasicID(WaveformType.OBS, finalSamplingHz, startTime - shift, fnpts, station, id,
							component, minPeriod, maxPeriod, includePhases, 0, convolute, obsSpcRe);
					
					BasicID synSpcImID = new BasicID(WaveformType.SYN, finalSamplingHz, startTime, fnpts, station, id,
							component, minPeriod, maxPeriod, includePhases, 0, convolute, synSpcIm);
					BasicID obsSpcImID = new BasicID(WaveformType.OBS, finalSamplingHz, startTime - shift, fnpts, station, id,
							component, minPeriod, maxPeriod, includePhases, 0, convolute, obsSpcIm);
					
					try {
						dataWriter.addBasicID(obsID);
						dataWriter.addBasicID(synID);
						envelopeWriter.addBasicID(obsEnvelopeID);
						envelopeWriter.addBasicID(synEnvelopeID);
						hyWriter.addBasicID(obsHyID);
						hyWriter.addBasicID(synHyID);
						spcAmpWriter.addBasicID(obsSpcAmpID);
						spcAmpWriter.addBasicID(synSpcAmpID);
						spcReWriter.addBasicID(obsSpcReID);
						spcReWriter.addBasicID(synSpcReID);
						spcImWriter.addBasicID(obsSpcImID);
						spcImWriter.addBasicID(synSpcImID);
						numberOfPairs.incrementAndGet();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			System.err.print(".");
		}
	}
	
}
