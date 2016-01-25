package io.github.kensuke1984.kibrary.waveformdata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
 * @version 0.1
 * 
 * @author Kensuke
 * 
 */
class ObservedSyntheticDatasetMaker extends parameter.ObservedSyntheticDatasetMaker {

	private Set<StaticCorrection> staticCorrectionSet;

	private Set<TimewindowInformation> timewindowInformationSet;

	private WaveformDataWriter dataWriter;

	private ObservedSyntheticDatasetMaker() throws IOException {
		this(null);
	}

	private ObservedSyntheticDatasetMaker(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!canGO())
			System.exit(0);
		if (timeShift || amplitudeCorrection)
			staticCorrectionSet = StaticCorrectionFile.read(staticCorrectionPath);

		// obsDirからイベントフォルダを指定
		eventDirs = Utilities.eventFolderSet(obsPath);
		timewindowInformationSet = TimewindowInformationFile.read(timewindowInformationPath);
		stationSet = timewindowInformationSet.parallelStream().map(ti -> ti.getStation()).collect(Collectors.toSet());
		idSet = timewindowInformationSet.parallelStream().map(ti -> ti.getGlobalCMTID()).collect(Collectors.toSet());
		readPeriodRanges();
	}

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
			periodRanges = ranges.toArray(new double[ranges.size()][]);
		} catch (Exception e) {
			throw new RuntimeException("Error in reading period ranges from SAC files.");
		}
	}

	/**
	 * フィルター処理は行わない
	 * 
	 * @param args
	 *            [parameter file name]
	 */
	public static void main(String[] args) throws IOException {
		ObservedSyntheticDatasetMaker osdm = null;
		if (args.length != 0) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			osdm = new ObservedSyntheticDatasetMaker(parameterPath);
		} else
			osdm = new ObservedSyntheticDatasetMaker();

		long startT = System.nanoTime();

		osdm.run();
		System.err.println();
		System.out.println("ObservedSynthetic finished in " + Utilities.toTimeString(System.nanoTime() - startT));
		System.exit(0);

	}

	public void run() {
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

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean canGO() {
		boolean canGO = true;

		if (!Files.exists(timewindowInformationPath)) {
			new NoSuchFileException(timewindowInformationPath.toString()).printStackTrace();
			canGO = false;
		}
		if (timeShift && !Files.exists(staticCorrectionPath)) {
			new NoSuchFileException(staticCorrectionPath.toString()).printStackTrace();
			canGO = false;
		}

		return canGO;
	}

	/**
	 * 与えられたイベントフォルダの観測波形と理論波形を書き込む 両方ともが存在しないと書き込まない
	 * 
	 * @author kensuke
	 * 
	 */
	private class Worker implements Runnable {

		private EventFolder obsEventDir;

		private Worker(EventFolder eventDir) {
			this.obsEventDir = eventDir;
			// System.out.println("this is " + eventDir);
		}

		@Override
		public void run() {
			Path synEventPath = synPath.resolve(obsEventDir.getGlobalCMTID().toString());
			// System.out.println(synEventDir);
			if (!Files.exists(synEventPath))
				throw new RuntimeException(synEventPath + " does not exist.");

			Set<SACFileName> obsFiles = null;
			try {
				obsFiles = obsEventDir.sacFileSet(sfn -> !sfn.isOBS());
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
				// System.out.println(synFileName.getFile().getName());

				if (!synFileName.exists())
					continue;

				Set<TimewindowInformation> windows = timewindowInformationSet.stream()
						.filter(info -> info.getStation().getStationName().equals(stationName))
						.filter(info -> info.getGlobalCMTID().equals(id))
						.filter(info -> info.getComponent() == component).collect(Collectors.toSet());

				// タイムウインドウの情報が入っていなければ次へ
				if (windows.isEmpty())
					continue;

				SACData obsSac = null;
				try {
					obsSac = obsFileName.read();
				} catch (IOException e1) {
					System.err.println("error occured in reading " + obsFileName);
					e1.printStackTrace();
					continue;
				}

				// System.out.println("reading " + synFileName);
				SACData synSac = null;
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
					if (timeShift || amplitudeCorrection)
						try {
							StaticCorrection sc = getStaticCorrection(window);
							shift = timeShift ? sc.getTimeshift() : 0;
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

}
