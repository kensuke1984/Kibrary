package io.github.kensuke1984.kibrary.waveformdata;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACExtension;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;

/**
 * 
 * Creates dataset containing observed and synthetic waveforms. <br>
 * The output is a set of an ID and waveform files.
 * 
 * Observed and synthetic waveforms in SAC files are collected from the obsDir
 * and synDir, respectively. Only SAC files, which sample rates are
 * {@link parameter.ObservedSyntheticDatasetMaker#sacSamplingHz}, are used. Both
 * folders must have event folders inside which have waveforms.
 * 
 * The static correction is applied as described in {@link StaticCorrection}
 * 
 * 
 * The sample rates of the data is
 * {@link parameter.ObservedSyntheticDatasetMaker#finalSamplingHz}.<br>
 * Timewindow information in
 * {@link parameter.ObservedSyntheticDatasetMaker#timewindowInformationPath} is
 * used for cutting windows.
 * 
 * Only pairs of a seismic source and a receiver with both an observed and
 * synthetic waveform are collected.
 * 
 * This class does not apply a digital filter, but extract information about
 * passband written in SAC files.
 * 
 * TODO <b> Assume that there are no stations with same name but different
 * network in one event</b>
 * 
 * 
 * @version 0.2.1
 * 
 * @author Kensuke Konishi
 * 
 */
public class ObservedSyntheticDatasetMaker implements Operation {

	private Path workPath;
	private Properties property;

	/**
	 * components to be included in the dataset
	 */
	private Set<SACComponent> components;

	public ObservedSyntheticDatasetMaker(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
	}

	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("obsPath"))
			property.setProperty("obsPath", "");
		if (!property.containsKey("synPath"))
			property.setProperty("synPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("convolute"))
			property.setProperty("convolute", "true");
		if (!property.containsKey("amplitudeCorrection"))
			property.setProperty("amplitudeCorrection", "false");
		if (!property.containsKey("timeCorrection"))
			property.setProperty("timeCorrection", "false");
		if (!property.containsKey("timewindowPath"))
			throw new IllegalArgumentException("There is no information about timewindowPath.");
		if (!property.containsKey("sacSamplingHz"))
			property.setProperty("sacSamplingHz", "20");
		if (!property.containsKey("finalSamplingHz"))
			property.setProperty("finalSamplingHz", "1");
	}

	private void set() throws NoSuchFileException {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));
		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		obsPath = getPath("obsPath");
		synPath = getPath("synPath");
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());
		timewindowPath = getPath("timewindowPath");
		timeCorrection = Boolean.parseBoolean(property.getProperty("timeCorrection"));
		amplitudeCorrection = Boolean.parseBoolean(property.getProperty("amplitudeCorrection"));

		if (timeCorrection || amplitudeCorrection) {
			if (!property.containsKey("staticCorrectionPath"))
				throw new RuntimeException("staticCorrectionPath is blank");
			staticCorrectionPath = getPath("staticCorrectionPath");
			if (!Files.exists(staticCorrectionPath))
				throw new NoSuchFileException(staticCorrectionPath.toString());
		}

		convolute = Boolean.parseBoolean(property.getProperty("convolute"));

		// sacSamplingHz
		// =Double.parseDouble(reader.getFirstValue("sacSamplingHz")); TODO
		sacSamplingHz = 20;
		finalSamplingHz = Double.parseDouble(property.getProperty("finalSamplingHz"));
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
			pw.println("##Path of a static correction file, ");
			pw.println("##if any of the corrections are true, the path must be defined");
			pw.println("#staticCorrectionPath staticCorrection.dat");
			pw.println("##double value of sac sampling Hz (20) can't be changed now");
			pw.println("#sacSamplingHz the value will be ignored");
			pw.println("##double value of sampling Hz in output files (1)");
			pw.println("#finalSamplingHz");
		}
		System.err.println(outPath + " is created.");
	}

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
	 * {@link Path} of a static correction file
	 */
	private Path staticCorrectionPath;

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

	private Set<StaticCorrection> staticCorrectionSet;

	private Set<TimewindowInformation> timewindowInformationSet;

	private WaveformDataWriter dataWriter;

	private Set<EventFolder> eventDirs;
	private Set<Station> stationSet;
	private Set<GlobalCMTID> idSet;
	private double[][] periodRanges;

	private void readPeriodRanges() {
		try {
			List<double[]> ranges = new ArrayList<>();
			for (SACFileName name : Utilities.sacFileNameSet(obsPath)) {
				if (!name.isOBS())
					continue;
				SACHeaderData header = name.readHeader();
				double[] range = new double[] { header.getValue(SACHeaderEnum.USER0),
						header.getValue(SACHeaderEnum.USER1) };
				boolean exists = false;
				if (ranges.size() == 0)
					ranges.add(range);
				for (int i = 0; !exists && i < ranges.size(); i++)
					if (Arrays.equals(range, ranges.get(i)))
						exists = true;
				if (!exists)
					ranges.add(range);
			}
			periodRanges = ranges.toArray(new double[0][]);
		} catch (Exception e) {
			throw new RuntimeException("Error in reading period ranges from SAC files.");
		}
	}

	/**
	 * 
	 * @param args
	 *            [a property file name]
	 * @throws Exception
	 *             if any
	 */
	public static void main(String[] args) throws Exception {

		Properties property = new Properties();
		if (args.length == 0)
			property.load(Files.newBufferedReader(Operation.findPath()));
		else if (args.length == 1)
			property.load(Files.newBufferedReader(Paths.get(args[0])));
		else
			throw new IllegalArgumentException("too many arguments. It should be 0 or 1(property file name)");
		ObservedSyntheticDatasetMaker osdm = new ObservedSyntheticDatasetMaker(property);

		long startT = System.nanoTime();
		System.err.println(ObservedSyntheticDatasetMaker.class.getName() + " is running.");
		osdm.run();
		System.err.println(ObservedSyntheticDatasetMaker.class.getName() + " finished in "
				+ Utilities.toTimeString(System.nanoTime() - startT));

	}

	@Override
	public void run() throws Exception {
		if (timeCorrection || amplitudeCorrection)
			staticCorrectionSet = StaticCorrectionFile.read(staticCorrectionPath);

		// obsDirからイベントフォルダを指定
		eventDirs = Utilities.eventFolderSet(obsPath);
		timewindowInformationSet = TimewindowInformationFile.read(timewindowPath);
		stationSet = timewindowInformationSet.parallelStream().map(TimewindowInformation::getStation)
				.collect(Collectors.toSet());
		idSet = timewindowInformationSet.parallelStream().map(TimewindowInformation::getGlobalCMTID)
				.collect(Collectors.toSet());
		readPeriodRanges();

		int n = Runtime.getRuntime().availableProcessors();
		ExecutorService execs = Executors.newFixedThreadPool(n);
		String dateStr = Utilities.getTemporaryString();
		Path waveIDPath = workPath.resolve("waveformID" + dateStr + ".dat");
		Path waveformPath = workPath.resolve("waveform" + dateStr + ".dat");
		try (WaveformDataWriter bdw = new WaveformDataWriter(waveIDPath, waveformPath, stationSet, idSet,
				periodRanges)) {
			dataWriter = bdw;
			for (EventFolder eventDir : eventDirs)
				execs.execute(new Worker(eventDir));

			execs.shutdown();
			while (!execs.isTerminated())
				Thread.sleep(1000);
			System.err.println("\n" + numberOfPairs.get() + " pairs of observed and synthetic waveforms are output.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * number of OUTPUT pairs. (excluding ignored traces)
	 */
	private AtomicInteger numberOfPairs = new AtomicInteger();

	/**
	 * 与えられたイベントフォルダの観測波形と理論波形を書き込む 両方ともが存在しないと書き込まない
	 * 
	 * @author kensuke
	 * 
	 */
	private class Worker implements Runnable {

		private EventFolder obsEventDir;

		private Worker(EventFolder eventDir) {
			obsEventDir = eventDir;
		}

		@Override
		public void run() {
			Path synEventPath = synPath.resolve(obsEventDir.getGlobalCMTID().toString());
			if (!Files.exists(synEventPath))
				throw new RuntimeException(synEventPath + " does not exist.");

			Set<SACFileName> obsFiles;
			try {
				(obsFiles = obsEventDir.sacFileSet()).removeIf(sfn -> !sfn.isOBS());
			} catch (IOException e2) {
				e2.printStackTrace();
				return;
			}

			for (SACFileName obsFileName : obsFiles) {
				// データセットに含める成分かどうか
				if (!components.contains(obsFileName.getComponent()))
					continue;
				String stationName = obsFileName.getStationName();
				GlobalCMTID id = obsFileName.getGlobalCMTID();
				SACComponent component = obsFileName.getComponent();
				String name = convolute
						? stationName + "." + id + "." + SACExtension.valueOfConvolutedSynthetic(component)
						: stationName + "." + id + "." + SACExtension.valueOfSynthetic(component);
				SACFileName synFileName = new SACFileName(synEventPath.resolve(name));

				if (!synFileName.exists())
					continue;

				Set<TimewindowInformation> windows = timewindowInformationSet.stream()
						.filter(info -> info.getStation().getStationName().equals(stationName))
						.filter(info -> info.getGlobalCMTID().equals(id))
						.filter(info -> info.getComponent() == component).collect(Collectors.toSet());

				// タイムウインドウの情報が入っていなければ次へ
				if (windows.isEmpty())
					continue;

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
					double startTime = window.getStartTime();
					double shift = 0;
					double ratio = 1;
					if (timeCorrection || amplitudeCorrection)
						try {
							StaticCorrection sc = getStaticCorrection(window);
							shift = timeCorrection ? sc.getTimeshift() : 0;
							ratio = amplitudeCorrection ? sc.getAmplitudeRatio() : 1;
						} catch (NoSuchElementException e) {
							System.err.println("There is no static correction information for\\n " + window);
							continue;
						}

					double[] obsData = cutDataSac(obsSac, startTime - shift, npts);
					double[] synData = cutDataSac(synSac, startTime, npts);
					double correctionRatio = ratio;

					obsData = Arrays.stream(obsData).map(d -> d / correctionRatio).toArray();
					BasicID synID = new BasicID(WaveformType.SYN, finalSamplingHz, startTime, npts, station, id,
							component, minPeriod, maxPeriod, 0, convolute, synData);
					BasicID obsID = new BasicID(WaveformType.OBS, finalSamplingHz, startTime - shift, npts, station, id,
							component, minPeriod, maxPeriod, 0, convolute, obsData);
					try {
						dataWriter.addBasicID(obsID);
						dataWriter.addBasicID(synID);
						numberOfPairs.incrementAndGet();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			System.err.print(".");
		}
	}

	/**
	 * ID for static correction and time window information Default is station
	 * name, global CMT id, component.
	 */
	private BiPredicate<StaticCorrection, TimewindowInformation> isPair = (s,
			t) -> s.getStation().equals(t.getStation()) && s.getGlobalCMTID().equals(t.getGlobalCMTID())
					&& s.getComponent() == t.getComponent();

	private StaticCorrection getStaticCorrection(TimewindowInformation window) {
		return staticCorrectionSet.stream().filter(s -> isPair.test(s, window)).findAny().get();
	}

	private double[] cutDataSac(SACData sac, double startTime, int npts) {
		Trace trace = sac.createTrace();
		int step = (int) (sacSamplingHz / finalSamplingHz);
		int startPoint = trace.getNearestXIndex(startTime);
		double[] waveData = trace.getY();
		return IntStream.range(0, npts).parallel().mapToDouble(i -> waveData[i * step + startPoint]).toArray();
	}

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}

}
