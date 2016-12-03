package io.github.kensuke1984.kibrary.timewindow;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.external.TauPPhase;
import io.github.kensuke1984.kibrary.external.TauPTimeReader;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

/**
 * workingDirectory/イベントフォルダ内の波形に対して タイムウインドウをつけていく
 * 
 * Create an information file about timewindows. It looks for observed waveforms
 * in event folders under the working directory. For all the waveforms,
 * timewindows are computed by TauP.
 * 
 * 
 * It creates a window for each given phase and exphase with front and rear
 * parts. Overlapped part between those are abandoned. Start and end time of the
 * window is set to integer multiple of DELTA in SAC files.
 * 
 * @version 0.2.2.2
 * 
 * @author Kensuke Konishi
 * 
 */
public class TimewindowMaker implements Operation {

	private Properties property;

	public TimewindowMaker(Properties property) {
		this.property = (Properties) property.clone();
		set();
	}

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(TimewindowMaker.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan TimewindowMaker");
			pw.println("##Path of a working folder (.)");
			pw.println("#workPath");
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("##TauPPhases exPhases (sS)");
			pw.println("#exPhases");
			pw.println("##TauPPhases usePhases (S)");
			pw.println("#usePhases");
			pw.println("##time before first phase. If it is 10, then 10 s before arrival (0)");
			pw.println("#frontShift");
			pw.println("##double time after last phase. If it is 60, then 60 s after arrival (0)");
			pw.println("#rearShift");
		}
		System.err.println(outPath + " is created.");
	}

	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("frontShift"))
			property.setProperty("frontShift", "0");
		if (!property.containsKey("rearShift"))
			property.setProperty("rearShift", "0");
		if (!property.containsKey("exPhases"))
			property.setProperty("exPhases", "sS");
		if (!property.containsKey("usePhases"))
			property.setProperty("usePhases", "S");
	}

	private Path workPath;

	private void set() {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));

		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		String date = Utilities.getTemporaryString();
		outputPath = workPath.resolve("timewindow" + date + ".dat");
		invalidList = workPath.resolve("invalidTimewindow" + date + ".txt");
		timewindowSet = Collections.synchronizedSet(new HashSet<>());
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());
		usePhases = phaseSet(property.getProperty("usePhases"));
		exPhases = phaseSet(property.getProperty("exPhases"));

		frontShift = Double.parseDouble(property.getProperty("frontShift"));
		rearShift = Double.parseDouble(property.getProperty("rearShift"));

	}

	private static Set<Phase> phaseSet(String arg) {
		return arg == null || arg.isEmpty() ? Collections.emptySet()
				: Arrays.stream(arg.split("\\s+")).map(Phase::create).collect(Collectors.toSet());
	}

	/**
	 * set of {@link SACComponent}
	 */
	private Set<SACComponent> components;

	/**
	 * how many seconds it shifts the starting time [s] phase到着からどれだけずらすか if the
	 * value is 5(not -5), then basically, each timewindow starts 5 sec before
	 * each usePhase
	 */
	private double frontShift;

	/**
	 * phase到着から後ろ何秒を取るか if the value is 10, basically, each timewindow ends 10
	 * secs after each usephase arrival
	 */
	private double rearShift;

	/**
	 * 省きたいフェーズ
	 */
	private Set<Phase> exPhases;

	/**
	 * 使いたいフェーズ
	 */
	private Set<Phase> usePhases;

	private Path outputPath;
	private Set<TimewindowInformation> timewindowSet;

	/**
	 * タイムウインドウがおかしくて省いたリスト とりあえず startが０以下になるもの
	 */
	private Path invalidList;

	/**
	 * Run must finish within 10 hours.
	 * 
	 * @param args
	 *            [parameter file name]
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

		TimewindowMaker twm = new TimewindowMaker(property);
		System.err.println(TimewindowMaker.class.getName() + " is going.");
		long startT = System.nanoTime();
		twm.run();
		System.err.println(
				TimewindowMaker.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - startT));

	}

	@Override
	public void run() throws Exception {
		Utilities.runEventProcess(workPath, eventDir -> {
			try {
				eventDir.sacFileSet().stream().filter(sfn -> sfn.isSYN() && components.contains(sfn.getComponent()))
						.forEach(sfn -> {
					try {
						makeTimeWindow(sfn);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		} , 10, TimeUnit.HOURS);

		if (timewindowSet.isEmpty())
			System.err.println("No timewindow is created");
		else
			TimewindowInformationFile.write(timewindowSet, outputPath);
	}

	private void makeTimeWindow(SACFileName sacFileName) throws IOException {
		SACData sacFile = sacFileName.read();
		// 震源深さ radius
		double eventR = 6371 - sacFile.getValue(SACHeaderEnum.EVDP);
		// 震源観測点ペアの震央距離
		double epicentralDistance = sacFile.getValue(SACHeaderEnum.GCARC);

		Set<TauPPhase> usePhases = TauPTimeReader.getTauPPhase(eventR, epicentralDistance, this.usePhases);
		Set<TauPPhase> exPhases = this.exPhases == null ? Collections.emptySet()
				: TauPTimeReader.getTauPPhase(eventR, epicentralDistance, this.exPhases);

		if (usePhases.isEmpty()) {
			writeInvalid(sacFileName);
			return;
		}
		double[] phaseTime = toTravelTime(usePhases);
		double[] exPhaseTime = exPhases.isEmpty() ? null : toTravelTime(exPhases);
		Timewindow[] windows = createTimeWindows(phaseTime, exPhaseTime);
		// System.exit(0);
		if (windows == null) {
			writeInvalid(sacFileName);
			return;
		}
		// System.out.println(sacFile.getValue(SacHeaderEnum.E));
		// delta (time step) in SacFile
		double delta = sacFile.getValue(SACHeaderEnum.DELTA);
		double e = sacFile.getValue(SACHeaderEnum.E);
		// station of SacFile
		Station station = sacFile.getStation();
		// global cmt id of SacFile
		GlobalCMTID id = sacFileName.getGlobalCMTID();
		// component of SacFile
		SACComponent component = sacFileName.getComponent();

		// window fix
		Arrays.stream(windows).map(window -> fix(window, delta)).filter(window -> window.endTime <= e).map(
				window -> new TimewindowInformation(window.getStartTime(), window.getEndTime(), station, id, component))
				.forEach(timewindowSet::add);
	}

	/**
	 * fix start and end time by delta these time must be (int) * delta
	 * 
	 * @param window {@link Timewindow}
	 * @param delta time step
	 * @return fixed {@link Timewindow}
	 */
	private static Timewindow fix(Timewindow window, double delta) {
		double startTime = delta * (int) (window.startTime / delta);
		double endTime = delta * (int) (window.endTime / delta);
		return new Timewindow(startTime, endTime);

	}

	/**
	 * Creates timewindows. all phases have front and rear shifts. if exPhase
	 * with front and rear shifts is in the timewindows of use-phases, then the
	 * timewindow will be cut.
	 * 
	 * @param phaseTime
	 *            must be in order.
	 * @param exPhaseTime
	 *            must be in order.
	 * @return created {@link Timewindow} array
	 */
	private Timewindow[] createTimeWindows(double[] phaseTime, double[] exPhaseTime) {
		Timewindow[] windows = Arrays.stream(phaseTime)
				.mapToObj(time -> new Timewindow(time - frontShift, time + rearShift)).sorted()
				.toArray(Timewindow[]::new);
		Timewindow[] exWindows = exPhaseTime == null ? null
				: Arrays.stream(exPhaseTime).mapToObj(time -> new Timewindow(time - frontShift, time + rearShift))
						.sorted().toArray(Timewindow[]::new);

		windows = mergeWindow(windows);

		if (exWindows == null)
			return windows;
		exWindows = mergeWindow(exWindows);
		return considerExPhase(windows, exWindows);
	}

	/**
	 * @param useTimeWindow to use
	 * @param exTimeWindow to avoid
	 * @return useTimeWindowからexTimeWindowの重なっている部分を取り除く 何もなくなってしまったらnullを返す
	 */
	private static Timewindow cutWindow(Timewindow useTimeWindow, Timewindow exTimeWindow) {
		// System.out.println(useTimeWindow+" "+exTimeWindow);
		if (!useTimeWindow.overlap(exTimeWindow))
			return useTimeWindow;
		// System.out.println("hi");
		if (exTimeWindow.startTime <= useTimeWindow.startTime)
			return useTimeWindow.endTime <= exTimeWindow.endTime ? null
					: new Timewindow(exTimeWindow.endTime, useTimeWindow.endTime);
		return new Timewindow(useTimeWindow.startTime, exTimeWindow.startTime);
	}

	/**
	 * 
	 * eliminate exTimeWindows from useTimeWindows
	 * 
	 * @param useTimeWindows
	 *            must be in order by start time
	 * @param exTimeWindows
	 *            must be in order by start time
	 * @return timewindows to use
	 */
	private static Timewindow[] considerExPhase(Timewindow[] useTimeWindows, Timewindow[] exTimeWindows) {
		List<Timewindow> usable = new ArrayList<>();
		for (Timewindow window : useTimeWindows) {
			for (Timewindow ex : exTimeWindows) {
				window = cutWindow(window, ex);
				if (window == null)
					break;
			}
			if (window != null)
				usable.add(window);
		}

		return usable.isEmpty() ? null : usable.toArray(new Timewindow[0]);
	}

	/**
	 * if there are any overlapping timeWindows, merge them. the start times
	 * must be in order.
	 * 
	 * @param windows
	 *            to be merged
	 * @return windows containing all the input windows in order
	 */
	private static Timewindow[] mergeWindow(Timewindow[] windows) {
		if (windows.length == 1)
			return windows;
		List<Timewindow> windowList = new ArrayList<>();
		Timewindow windowA = windows[0];
		for (int i = 1; i < windows.length; i++) {
			Timewindow windowB = windows[i];
			if (windowA.overlap(windowB)) {
				windowA = windowA.merge(windowB);
				if (i == windows.length - 1)
					windowList.add(windowA);
			} else {
				windowList.add(windowA);
				windowA = windows[i];
				if (i == windows.length - 1)
					windowList.add(windows[i]);
			}
		}
		return windowList.toArray(new Timewindow[0]);
	}

	/**
	 * @param phases
	 *            Set of TauPPhases
	 * @return travel times in {@link TauPPhase}
	 */
	private static double[] toTravelTime(Set<TauPPhase> phases) {
		return phases.stream().mapToDouble(TauPPhase::getTravelTime).toArray();
	}

	private synchronized void writeInvalid(SACFileName sacFileName) throws IOException {
		try (PrintWriter pw = new PrintWriter(
				Files.newBufferedWriter(invalidList, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
			pw.println(sacFileName);
		}
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
