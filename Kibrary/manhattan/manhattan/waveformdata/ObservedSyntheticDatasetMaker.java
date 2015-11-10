package manhattan.waveformdata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import filehandling.sac.SACComponent;
import filehandling.sac.SACData;
import filehandling.sac.SACExtension;
import filehandling.sac.SACFileName;
import filehandling.sac.SACHeaderEnum;
import filehandling.sac.WaveformType;
import manhattan.datacorrection.StaticCorrection;
import manhattan.datacorrection.StaticCorrectionFile;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.EventFolder;
import manhattan.template.Station;
import manhattan.template.Trace;
import manhattan.template.Utilities;
import manhattan.timewindow.TimewindowInformation;
import manhattan.timewindow.TimewindowInformationFile;

/**
 * 
 * Creates dataset containing observed and synthetic waveforms. The output files
 * are one of ID and one of waveform.
 * 
 * 
 * workDir下の各イベントフォルダ内にある観測波形 理論波形からデータセットを構築する。 データセットは
 * それぞれの波形情報ファイルと波形データのファイル
 * 
 * タイムシフトインフォメーションの中のウインドウのデータを与えたsamplingHzで切り出す
 * 
 * 観測波形と理論波形の両方がない震源観測点成分の組み合わせはスキップする。
 * 
 * フィルター処理は行わない (先にフィルターをかけておく必要がある) TODO
 * 
 * TODO データ選定の手段？
 * 
 * タイムウインドウファイル内に記述があって、観測もしくは理論波形が存在しなくてもいい
 * 
 * @since 2013/11/12
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
		timewindowInformationSet = TimewindowInformationFile.read(timewindowInformationPath);
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

		// obsDirからイベントフォルダを指定
		Set<EventFolder> eventDirs = Utilities.eventFolderSet(osdm.obsPath);

		int n = Runtime.getRuntime().availableProcessors();
		ExecutorService execs = Executors.newFixedThreadPool(n);
		String dateStr = Utilities.getTemporaryString();
		Path waveIDPath = osdm.workPath.resolve("waveformID" + dateStr + ".dat");
		Path waveformPath = osdm.workPath.resolve("waveform" + dateStr + ".dat");
		try (WaveformDataWriter bdw = new WaveformDataWriter(waveIDPath, waveformPath)) {
			osdm.dataWriter = bdw;
			for (EventFolder eventDir : eventDirs)
				execs.execute(osdm.new Worker(eventDir));

			execs.shutdown();
			while (!execs.isTerminated())
				Thread.sleep(1000);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.err.println();
		System.out.println("ObservedSynthetic finished in " + Utilities.toTimeString(System.nanoTime() - startT));
		System.exit(0);

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
				String name = isConvolved
						? stationName + "." + id + "." + SACExtension.valueOfConvolutedSynthetic(component)
						: stationName + "." + id + "." + SACExtension.valueOfSynthetic(component);
				SACFileName synFileName = new SACFileName(synEventPath.resolve(name));
				// System.out.println(synFileName.getFile().getName());

				if (!synFileName.exists())
					continue;

				Set<TimewindowInformation> windows = timewindowInformationSet.stream()
						.filter(info -> info.getStationName().equals(stationName))
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
					double maxratio = ratio;

					obsData = Arrays.stream(obsData).map(d -> d / maxratio).toArray();
					BasicID synID = new BasicID(WaveformType.SYN, finalSamplingHz, startTime, npts, station, id,
							component, minPeriod, maxPeriod, 0, isConvolved, synData);
					BasicID obsID = new BasicID(WaveformType.OBS, finalSamplingHz, startTime - shift, npts, station,
							id, component, minPeriod, maxPeriod, 0, isConvolved, obsData);
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
			t) -> s.getStationName().equals(t.getStationName()) && s.getGlobalCMTID().equals(t.getGlobalCMTID())
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
