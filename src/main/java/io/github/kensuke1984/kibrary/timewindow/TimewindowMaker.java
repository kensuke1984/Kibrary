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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;

import ucar.jpeg.jj2000.j2k.util.ArrayUtil;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.external.TauPPhase;
import io.github.kensuke1984.kibrary.external.TauPTimeReader;
import io.github.kensuke1984.kibrary.util.Earth;
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
 * @version 0.2.2.1
 * 
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
			pw.println("##boolean majorArc (false). Use major arc phases?");
			pw.println("#majorArc");
			pw.println("##TauPPhases exPhases ()");
			pw.println("#exPhases");
			pw.println("##TauPPhases usePhases (S)");
			pw.println("#usePhases");
			pw.println("##time before first phase. If it is 10, then 10 s before arrival (0)");
			pw.println("#frontShift");
			pw.println("##double time after last phase. If it is 60, then 60 s after arrival (0)");
			pw.println("#rearShift");
			pw.println("##boolean corridor (false)");
			pw.println("#corridor ");
			pw.println("##String model to compute travel times using TauP (prem)");
			pw.println("#model ");
			pw.println("##double minLength for the time windows in seconds (40.)");
			pw.println("#minLength ");
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
			property.setProperty("exPhases", "");
		if (!property.containsKey("usePhases"))
			property.setProperty("usePhases", "S");
		if (!property.containsKey("majorArc"))
			property.setProperty("majorArc", "false");
		if (!property.containsKey("corridor"))
			property.setProperty("corridor", "false");
		if (!property.containsKey("model"))
			property.setProperty("model", "prem");
		if (!property.containsKey("minLength"))
			property.setProperty("minLength", "40");
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
		majorArc = Boolean.parseBoolean(property.getProperty("majorArc"));
		
		frontShift = Double.parseDouble(property.getProperty("frontShift"));
		rearShift = Double.parseDouble(property.getProperty("rearShift"));
		
		corridor = Boolean.parseBoolean(property.getProperty("corridor"));
		
		model = property.getProperty("model").trim().toLowerCase();
		
		minLength = Double.parseDouble(property.getProperty("minLength"));
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
	
	private boolean majorArc;
	
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
	
	private boolean corridor;
	
	private double minLength;
	
	String model;

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
				eventDir.sacFileSet().stream().filter(sfn -> sfn.isOBS() && components.contains(sfn.getComponent()))
						.forEach(sfn -> {
					try {
						if (corridor)
							makeTimeWindowForCorridor(sfn);
						else
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
	
	private void makeTimeWindowForCorridor(SACFileName sacFileName) throws IOException {
		SACData sacFile = sacFileName.read();
		// 震源深さ radius
		double eventR = 6371 - sacFile.getValue(SACHeaderEnum.EVDP);
		// 震源観測点ペアの震央距離
		double epicentralDistance = sacFile.getValue(SACHeaderEnum.GCARC);
		try {
			// group 1
//			this.frontShift = 20.;
//			this.rearShift = 50.;
//			Set<Phase> usePhasesCorridor = Arrays.stream("s S ScS Sdiff".split(" "))
//					.map(Phase::create).collect(Collectors.toSet());
//			Set<TauPPhase> usePhases = TauPTimeReader.getTauPPhase(eventR, epicentralDistance, usePhasesCorridor, model);
//			if (!majorArc) {
//				usePhases.removeIf(phase -> phase.getPuristDistance() >= 180.);
//			}
//			Set<Phase> exPhasesCorridor = Arrays.stream("sS sSdiff sScS SS".split(" "))
//					.map(Phase::create).collect(Collectors.toSet());
//			Set<TauPPhase> exPhases = exPhasesCorridor.size() == 0 ? Collections.emptySet()
//					: TauPTimeReader.getTauPPhase(eventR, epicentralDistance, exPhasesCorridor, model);
//			if (usePhases.isEmpty()) {
//				writeInvalid(sacFileName);
//				return;
//			}
//			double[] phaseTime = toTravelTime(usePhases);
//			double[] exPhaseTime = exPhases.isEmpty() ? null : toTravelTime(exPhases);
//			
//			Timewindow[] windows1 = createTimeWindows(phaseTime, exPhaseTime);
//			//
//			
//			// group 2
//			this.frontShift = 20.;
//			this.rearShift = 50.;
//			usePhasesCorridor = Arrays.stream("sS sScS sSdiff".split(" "))
//					.map(Phase::create).collect(Collectors.toSet());
//			usePhases = TauPTimeReader.getTauPPhase(eventR, epicentralDistance, usePhasesCorridor, model);
//			if (!majorArc) {
//				usePhases.removeIf(phase -> phase.getPuristDistance() >= 180.);
//			}
//			exPhasesCorridor = Arrays.stream("S ScS SS sSS".split(" "))
//					.map(Phase::create).collect(Collectors.toSet());
//			exPhases = exPhasesCorridor.size() == 0 ? Collections.emptySet()
//					: TauPTimeReader.getTauPPhase(eventR, epicentralDistance, exPhasesCorridor, model);
//			if (usePhases.isEmpty()) {
//				writeInvalid(sacFileName);
//				return;
//			}
//			phaseTime = toTravelTime(usePhases);
//			exPhaseTime = exPhases.isEmpty() ? null : toTravelTime(exPhases);
//			
//			Timewindow[] windows2 = createTimeWindows(phaseTime, exPhaseTime);
//			//
//			
//			// group 3
//			this.frontShift = 20.;
//			this.rearShift = 70.;
//			usePhasesCorridor = Arrays.stream("SS".split(" "))
//					.map(Phase::create).collect(Collectors.toSet());
//			usePhases = TauPTimeReader.getTauPPhase(eventR, epicentralDistance, usePhasesCorridor, model);
//			if (!majorArc) {
//				usePhases.removeIf(phase -> phase.getPuristDistance() >= 180.);
//			}
//			exPhasesCorridor = Arrays.stream("sScS sSS".split(" "))
//					.map(Phase::create).collect(Collectors.toSet());
//			exPhases = exPhasesCorridor.size() == 0 ? Collections.emptySet()
//					: TauPTimeReader.getTauPPhase(eventR, epicentralDistance, exPhasesCorridor, model);
//			if (usePhases.isEmpty()) {
//				writeInvalid(sacFileName);
//				return;
//			}
//			phaseTime = toTravelTime(usePhases);
//			exPhaseTime = exPhases.isEmpty() ? null : toTravelTime(exPhases);
//			
//			Timewindow[] windows3 = createTimeWindows(phaseTime, exPhaseTime);
//			//
//			
//			// group 4
//			this.frontShift = 20.;
//			this.rearShift = 70.;
//			usePhasesCorridor = Arrays.stream("sSS".split(" "))
//					.map(Phase::create).collect(Collectors.toSet());
//			usePhases = TauPTimeReader.getTauPPhase(eventR, epicentralDistance, usePhasesCorridor, model);
//			if (!majorArc) {
//				usePhases.removeIf(phase -> phase.getPuristDistance() >= 180.);
//			}
//			exPhasesCorridor = Arrays.stream("SS SSS".split(" "))
//					.map(Phase::create).collect(Collectors.toSet());
//			exPhases = exPhasesCorridor.size() == 0 ? Collections.emptySet()
//					: TauPTimeReader.getTauPPhase(eventR, epicentralDistance, exPhasesCorridor, model);
//			if (usePhases.isEmpty()) {
//				writeInvalid(sacFileName);
//				return;
//			}
//			phaseTime = toTravelTime(usePhases);
//			exPhaseTime = exPhases.isEmpty() ? null : toTravelTime(exPhases);
//			
//			Timewindow[] windows4 = createTimeWindows(phaseTime, exPhaseTime);
			//
			
			// group 1
			this.frontShift = 10.;
			this.rearShift = 60.;
//			double eventDepth = Earth.EARTH_RADIUS - sacFileName.getGlobalCMTID().getEvent().getCmtLocation().getR();
			Set<Phase> usePhasesCorridor = null;
//			if (eventDepth < 200)
//				usePhasesCorridor = Arrays.stream("s S ScS Sdiff sS sScS sSdiff SS".split(" "))
//					.map(Phase::create).collect(Collectors.toSet());
//			else
				usePhasesCorridor = Arrays.stream("s S ScS Sdiff sS sScS sSdiff SS sSS".split(" "))
				.map(Phase::create).collect(Collectors.toSet());
			List<TauPPhase> usePhasesList = TauPTimeReader.getTauPPhaseList(eventR, epicentralDistance, usePhasesCorridor, model);
			if (!majorArc) {
				usePhasesList.removeIf(phase -> phase.getPuristDistance() >= 180.);
			}
			Set<Phase> exPhasesCorridor = null;
//			if (eventDepth < 200)
//				exPhasesCorridor = Arrays.stream("sSS SSS sSSS SSSS sSSSS".split(" "))
//					.map(Phase::create).collect(Collectors.toSet());
//			else
				exPhasesCorridor = Arrays.stream("SSS sSSS SSSS sSSSS".split(" "))
				.map(Phase::create).collect(Collectors.toSet());
			Set<TauPPhase> exPhases = exPhasesCorridor.size() == 0 ? Collections.emptySet()
					: TauPTimeReader.getTauPPhase(eventR, epicentralDistance, exPhasesCorridor, model);
			if (usePhases.isEmpty()) {
				writeInvalid(sacFileName);
				return;
			}
			double[] phaseTime = toTravelTime(usePhasesList);
			Phase[] phaseNames = toPhaseName(usePhasesList);
			double[] exPhaseTime = exPhases.isEmpty() ? null : toTravelTime(exPhases);
			
			Timewindow[] windows1 = createTimeWindowsAndSplit(phaseTime, phaseNames, exPhaseTime);
//			//
			
			// group 5
			this.frontShift = 6.;
			this.rearShift = 70.;
			usePhasesCorridor = Arrays.stream("ScSScS sScSScS".split(" "))
					.map(Phase::create).collect(Collectors.toSet());
			Set<TauPPhase> usePhases = TauPTimeReader.getTauPPhase(eventR, epicentralDistance, usePhasesCorridor, model);
			if (!majorArc) {
				usePhases.removeIf(phase -> phase.getPuristDistance() >= 180.);
			}
			exPhasesCorridor = Arrays.stream("SSS sSSS SSSS sSSSS SSSSS sSSSSS SSSSSS sSSSSSS".split(" "))
					.map(Phase::create).collect(Collectors.toSet());
			exPhases = exPhasesCorridor.size() == 0 ? Collections.emptySet()
					: TauPTimeReader.getTauPPhase(eventR, epicentralDistance, exPhasesCorridor, model);
			if (usePhases.isEmpty()) {
				writeInvalid(sacFileName);
				return;
			}
			phaseTime = toTravelTime(usePhases);
			phaseNames = toPhaseName(usePhasesList);
			exPhaseTime = exPhases.isEmpty() ? null : toTravelTime(exPhases);
			
			Timewindow[] windows5 = createTimeWindowsAndSplit(phaseTime, phaseNames, exPhaseTime);
			//
			
			// group 6
			this.frontShift = 6.;
			this.rearShift = 75.;
			usePhasesCorridor = Arrays.stream("ScSScSScS sScSScSScS".split(" "))
					.map(Phase::create).collect(Collectors.toSet());
			usePhases = TauPTimeReader.getTauPPhase(eventR, epicentralDistance, usePhasesCorridor, model);
			if (!majorArc) {
				usePhases.removeIf(phase -> phase.getPuristDistance() >= 180.);
			}
			exPhasesCorridor = Collections.emptySet();
			exPhases = exPhasesCorridor.size() == 0 ? Collections.emptySet()
					: TauPTimeReader.getTauPPhase(eventR, epicentralDistance, exPhasesCorridor, model);
			if (usePhases.isEmpty()) {
				writeInvalid(sacFileName);
				return;
			}
			phaseTime = toTravelTime(usePhases);
			phaseNames = toPhaseName(usePhasesList);
			exPhaseTime = exPhases.isEmpty() ? null : toTravelTime(exPhases);
			
			Timewindow[] windows6 = createTimeWindowsAndSplit(phaseTime, phaseNames, exPhaseTime);
			//
			
			List<Timewindow> windowList = new ArrayList<>();
			if (windows1 != null) {
				for (Timewindow tw : windows1)
					windowList.add(tw);
			}
//			if (windows2 != null) {
//				for (Timewindow tw : windows2)
//					windowList.add(tw);
//			}
//			if (windows3 != null) {
//				for (Timewindow tw : windows3)
//					windowList.add(tw);
//			}
//			if (windows4 != null) {
//				for (Timewindow tw : windows4)
//					windowList.add(tw);
//			}
			if (windows5 != null) {
				for (Timewindow tw : windows5)
					windowList.add(tw);
			}
			if (windows6 != null) {
				for (Timewindow tw : windows6)
					windowList.add(tw);
			}
//			if (windows7 != null) {
//				for (Timewindow tw : windows7)
//					windowList.add(tw);
//			}
//			if (windows8 != null) {
//				for (Timewindow tw : windows8)
//					windowList.add(tw);
//			}
			Timewindow[] windows = windowList.toArray(new Timewindow[windowList.size()]);
			
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
			Set<Phase> tmpUsePhases = Arrays.stream("s S ScS Sdiff sS sScS sSdiff SS sSS SSS sSSS SSSS sSSSS ScSScS sScSScS ScSScSScS sScSScSScS".split(" "))
				.map(Phase::create).collect(Collectors.toSet());
			Set<TauPPhase> usePhases_ = TauPTimeReader.getTauPPhase(eventR, epicentralDistance, tmpUsePhases, model);
			Arrays.stream(windows).map(window -> fix(window, delta)).filter(window -> window.getEndTime() <= e).map(
					window -> new TimewindowInformation(window.getStartTime(), window.getEndTime(), station, id, component, containPhases(window, usePhases_)))
					.filter(tw -> tw.getPhases().length > 0)
					.forEach(tw -> {
						if (tw.endTime - tw.startTime >= 30.) {
							timewindowSet.add(tw);
						}
						else 
							System.out.println("Ignored length<30s " + tw);
					});
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	private void makeTimeWindow(SACFileName sacFileName) throws IOException {
		SACData sacFile = sacFileName.read();
		// 震源深さ radius
		double eventR = 6371 - sacFile.getValue(SACHeaderEnum.EVDP);
		// 震源観測点ペアの震央距離
		double epicentralDistance = sacFile.getValue(SACHeaderEnum.GCARC);
		
		try {
			Set<TauPPhase> usePhases = TauPTimeReader.getTauPPhase(eventR, epicentralDistance, this.usePhases, "ak135");
			
			// use only the first arrival in case of triplication
			Map<Phase, List<TauPPhase>> nameToPhase = new HashMap<>();
			for (TauPPhase phase : usePhases) {
				if (!nameToPhase.containsKey(phase.getPhaseName())) {
					List<TauPPhase> list = new ArrayList<>();
					list.add(phase);
					nameToPhase.put(phase.getPhaseName(), list);
				}
				else {
					List<TauPPhase> list = nameToPhase.get(phase.getPhaseName());
					list.add(phase);
					nameToPhase.put(phase.getPhaseName(), list);
				}
			}
			usePhases.clear();
			for (Phase phase : nameToPhase.keySet()) {
				TauPPhase firstArrival = nameToPhase.get(phase).get(0);
				for (TauPPhase taupphase : nameToPhase.get(phase)) {
					if (taupphase.getTravelTime() < firstArrival.getTravelTime())
						firstArrival = taupphase;
				}
				usePhases.add(firstArrival);
			}
			
			usePhases.forEach(phase -> phase.getDistance());
			
			if (!majorArc) {
				usePhases.removeIf(phase -> phase.getPuristDistance() >= 180.);
			}
			
			Set<TauPPhase> exPhases = this.exPhases.size() == 0 ? Collections.emptySet()
					: TauPTimeReader.getTauPPhase(eventR, epicentralDistance, this.exPhases);
			
			if (usePhases.isEmpty()) {
				writeInvalid(sacFileName);
				return;
			}
			double[] phaseTime = toTravelTime(usePhases);
			double[] exPhaseTime = exPhases.isEmpty() ? null : toTravelTime(exPhases);
			
			double exRearShift = 5.;
			Timewindow[] windows = createTimeWindows(phaseTime, exPhaseTime, exRearShift);
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
			Arrays.stream(windows).map(window -> fix(window, delta)).filter(window -> window.getEndTime() <= e).map(
					window -> new TimewindowInformation(window.getStartTime(), window.getEndTime(), station, id, component, containPhases(window, usePhases)))
					.filter(tw -> tw.getPhases().length > 0)
					.forEach(timewindowSet::add);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
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
	
	private Timewindow[] createTimeWindowsAndSplit(double[] phaseTime, Phase[] phaseNames, double[] exPhaseTime) {
//		Timewindow[] windows = Arrays.stream(phaseTime)
//				.mapToObj(time -> new Timewindow(time - frontShift, time + rearShift)).sorted()
//				.toArray(Timewindow[]::new);
		if (phaseTime.length == 0)
			return null;
		
		Map<Phase, Timewindow[]> phaseWindowMap = new HashMap<>();
		for (int i = 0; i < phaseTime.length; i++) {
			double time = phaseTime[i];
			Phase phase = phaseNames[i];
			if (phaseWindowMap.containsKey(phase)) {
				Timewindow[] windows = phaseWindowMap.get(phase);
				Timewindow[] newWindows = Arrays.copyOf(windows, windows.length + 1);
				newWindows[newWindows.length - 1] = new Timewindow(time - frontShift, time + rearShift);
				phaseWindowMap.replace(phase, newWindows);
			}
			else {
				Timewindow[] windows = new Timewindow[] { new Timewindow(time - frontShift, time + rearShift) };
				phaseWindowMap.put(phase, windows);
			}
		}
		
		List<Timewindow> list = new ArrayList<>();
		for (Phase phase : phaseWindowMap.keySet()) {
			Timewindow[] windows = Arrays.stream(phaseWindowMap.get(phase)).sorted().toArray(Timewindow[]::new);
			windows = mergeWindow(windows);
			for (Timewindow window : windows)
				list.add(window);
		}
		
		Timewindow[] windows = list.stream().sorted().toArray(Timewindow[]::new);
		
		Timewindow[] exWindows = exPhaseTime == null ? null
				: Arrays.stream(exPhaseTime).mapToObj(time -> new Timewindow(time - frontShift, time + rearShift))
						.sorted().toArray(Timewindow[]::new);
		
		if (exWindows != null) {
			exWindows = mergeWindow(exWindows);
			windows = considerExPhase(windows, exWindows, minLength);
		}
		
		return splitWindow(windows, minLength); 
	}
	
	private Timewindow[] createTimeWindows(double[] phaseTime, double[] exPhaseTime, double exFrontShift) {
		Timewindow[] windows = Arrays.stream(phaseTime)
				.mapToObj(time -> new Timewindow(time - frontShift, time + rearShift)).sorted()
				.toArray(Timewindow[]::new);
		Timewindow[] exWindows = exPhaseTime == null ? null
				: Arrays.stream(exPhaseTime).mapToObj(time -> new Timewindow(time - exFrontShift, time + rearShift))
						.sorted().toArray(Timewindow[]::new);

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
	private static Timewindow cutWindow(Timewindow useTimeWindow, Timewindow exTimeWindow, double minLength) {
		// System.out.println(useTimeWindow+" "+exTimeWindow);
		if (!useTimeWindow.overlap(exTimeWindow))
			return useTimeWindow;
		// System.out.println("hi");
		if (exTimeWindow.startTime <= useTimeWindow.startTime)
			return useTimeWindow.endTime <= exTimeWindow.endTime ? null
					: new Timewindow(exTimeWindow.endTime, useTimeWindow.endTime);
		Timewindow newWindow = new Timewindow(useTimeWindow.startTime, exTimeWindow.startTime);
		return newWindow.getLength() < minLength ? null : newWindow;
	}
	
	private static Timewindow cutWindow(Timewindow useTimeWindow, Timewindow exTimeWindow) {
		// System.out.println(useTimeWindow+" "+exTimeWindow);
		if (!useTimeWindow.overlap(exTimeWindow))
			return useTimeWindow;
		// System.out.println("hi");
		if (exTimeWindow.startTime <= useTimeWindow.startTime)
			return useTimeWindow.endTime <= exTimeWindow.endTime ? null
					: new Timewindow(exTimeWindow.endTime, useTimeWindow.endTime);
		Timewindow newWindow = new Timewindow(useTimeWindow.startTime, exTimeWindow.startTime);
		return newWindow;
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
	private static Timewindow[] considerExPhase(Timewindow[] useTimeWindows, Timewindow[] exTimeWindows, double minLength) {
		List<Timewindow> usable = new ArrayList<>();
		for (Timewindow window : useTimeWindows) {
			for (Timewindow ex : exTimeWindows) {
				window = cutWindow(window, ex, minLength);
				if (window == null)
					break;
			}
			if (window != null)
				usable.add(window);
		}

		return usable.size() == 0 ? null : usable.toArray(new Timewindow[0]);
	}
	
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

		return usable.size() == 0 ? null : usable.toArray(new Timewindow[0]);
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
	
	private static Timewindow[] splitWindow(Timewindow[] windows, double minLength) {
		boolean mergeIfshort = true;
		if (windows.length == 1)
			return windows;
		List<Timewindow> windowList = new ArrayList<>();
		Timewindow windowA = windows[0];
		for (int i = 1; i < windows.length; i++) {
			Timewindow windowB = windows[i];
			if (windowA.overlap(windowB)) {
				Timewindow newA = new Timewindow(windowA.startTime, windowB.startTime);
				if (newA.getLength() < minLength) {
					windowA = newA.merge(windowB);
				} else {
					windowList.add(newA);
					windowA = windowB;
				}
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
	
	private static double[] toTravelTime(List<TauPPhase> phases) {
		return phases.stream().mapToDouble(TauPPhase::getTravelTime).toArray();
	}
	
	private static Phase[] toPhaseName(List<TauPPhase> phases) {
		Phase[] names = new Phase[phases.size()];
		for (int i = 0; i < names.length; i++)
			names[i] = phases.get(i).getPhaseName();
		return names;
	}

	private synchronized void writeInvalid(SACFileName sacFileName) throws IOException {
		try (PrintWriter pw = new PrintWriter(
				Files.newBufferedWriter(invalidList, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
			pw.println(sacFileName);
		}
	}
	
	private Phase[] containPhases(Timewindow window, Set<TauPPhase> usePhases) {
		Set<Phase> phases = new HashSet<>();
		for (TauPPhase phase : usePhases) {
			double time = phase.getTravelTime();
			if (time <= window.endTime && time >= window.startTime)
				phases.add(phase.getPhaseName());
		}
		return phases.toArray(new Phase[phases.size()]);
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
