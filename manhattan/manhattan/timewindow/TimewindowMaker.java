package manhattan.timewindow;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import filehandling.sac.SACComponent;
import filehandling.sac.SACData;
import filehandling.sac.SACFileName;
import filehandling.sac.SACHeaderEnum;
import manhattan.external.TauPPhase;
import manhattan.external.TauPTimeReader;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.Station;
import manhattan.template.Utilities;

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
 * @version 0.1.7
 * 
 * 
 * @author Kensuke
 * 
 */
class TimewindowMaker extends parameter.TimewindowMaker {

	private TimewindowMaker(Path parameterPath) throws IOException {
		super(parameterPath);
		String date = Utilities.getTemporaryString();
		outputPath = workPath.resolve("timewindow" + date + ".dat");
		invalidList = workPath.resolve("invalidTimewindow" + date + ".txt");
		timewindowSet = Collections.synchronizedSet(new HashSet<>());
	}

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
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		TimewindowMaker twm = null;
		if (args.length == 1) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			twm = new TimewindowMaker(parameterPath);
		} else if (args.length == 0)
			twm = new TimewindowMaker(null);
		else
			throw new IllegalArgumentException("Usage: [parameter file name]");
		System.err.println("TimewindowMaker is going.");
		long startT = System.nanoTime();
		twm.run();
		System.err.println(Utilities.toTimeString(System.nanoTime() - startT));

	}

	private void run() throws IOException, InterruptedException {
		Utilities.runEventProcess(workPath, eventDir -> {
			try {
				eventDir.sacFileSet(sfn -> !sfn.isOBS() || !components.contains(sfn.getComponent())).forEach(sfn -> {
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
	 * @param window
	 * @param delta
	 * @return
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
	 * @return
	 */
	private Timewindow[] createTimeWindows(double[] phaseTime, double[] exPhaseTime) {
		Timewindow[] windows = Arrays.stream(phaseTime)
				.mapToObj(time -> new Timewindow(time - frontShift, time + rearShift)).sorted()
				.toArray(n -> new Timewindow[n]);
		Timewindow[] exWindows = exPhaseTime == null ? null
				: Arrays.stream(exPhaseTime).mapToObj(time -> new Timewindow(time - frontShift, time + rearShift))
						.sorted().toArray(n -> new Timewindow[n]);

		windows = mergeWindow(windows);

		if (exWindows == null)
			return windows;
		exWindows = mergeWindow(exWindows);
		return considerExPhase(windows, exWindows);
	}

	/**
	 * @param useTimeWindow
	 * @param exTimeWindow
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

		return usable.size() == 0 ? null : usable.toArray(new Timewindow[usable.size()]);
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
		return windowList.toArray(new Timewindow[windowList.size()]);
	}

	/**
	 * @param phases
	 *            Set of TauPPhases
	 * @return travel times in {@link TauPPhase}
	 */
	private static double[] toTravelTime(Set<TauPPhase> phases) {
		return phases.stream().mapToDouble(phase -> phase.getTravelTime()).toArray();
	}

	private synchronized void writeInvalid(SACFileName sacFileName) throws IOException {
		try (PrintWriter pw = new PrintWriter(
				Files.newBufferedWriter(invalidList, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
			pw.println(sacFileName);
		}
	}

}
