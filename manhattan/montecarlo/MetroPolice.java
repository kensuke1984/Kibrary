/**
 * 
 */
package montecarlo;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import filehandling.sac.SACComponent;
import filehandling.sac.SACData;
import filehandling.sac.SACFileName;
import filehandling.spc.DSMOutput;
import filehandling.spc.SACMaker;
import filehandling.spc.SpcFileName;
import manhattan.butterworth.BandPassFilter;
import manhattan.butterworth.ButterworthFilter;
import manhattan.datacorrection.BoxcarSourceTimeFunction;
import manhattan.datacorrection.SourceTimeFunction;
import manhattan.dsminformation.PolynomialStructure;
import manhattan.dsminformation.SyntheticDSMInfo;
import manhattan.external.DSMMPI;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.inversion.StationInformationFile;
import manhattan.template.EventFolder;
import manhattan.template.Station;
import manhattan.template.Utilities;

/**
 * @author kensuke
 * @since 2015/06/30
 * @version 0.0.1
 * 
 * @version 0.0.2
 * @since 2015/8/5 run0を基準に 震源関数
 * 
 *        logFile is in run0.
 * 
 * @version 0.0.2.1
 * @since 2015/8/7 {@link IOException}
 * 
 * @version 0.0.2.2
 * @since 2015/8/8 {@link InterruptedException}
 */
class MetroPolice {
	private Path montePath;
	private Path psvPath;

	private MetroPolice(String path) throws IOException {
		montePath = Paths.get(path);
		psvPath = montePath.resolve("primePSV");
		stationSet = StationInformationFile.read(montePath.resolve("station.inf"));
		obsDir = montePath.resolve("obs");
		SacComparator.set(montePath);
		setFilter(0.005, 0.08, 4);
	}

	private Path run0Path;

	private boolean canGO() {
		boolean isOK = true;
		if (!Files.exists(montePath)) {
			new NoSuchFileException(montePath.toString()).printStackTrace();
			isOK = false;
		}
		run0Path = montePath.resolve("run0");
		if (!Files.exists(run0Path)) {
			new NoSuchFileException(run0Path.toString()).printStackTrace();
			isOK = false;
		}

		if (!Files.exists(obsDir)) {
			System.out.println(obsDir + " does not exist");
			isOK = false;
		}
		if (!Files.exists(psvPath)) {
			new NoSuchFileException(psvPath.toString()).printStackTrace();
			isOK = false;
		}
		return isOK;
	}

	private Set<Station> stationSet;

	private Path obsDir;

	private Station pickup(String stationName) {
		return stationSet.stream().filter(station -> station.getStationName().equals(stationName)).findAny()
				.orElseThrow(() -> new RuntimeException("No information about " + stationName));
	}

	private void createDSMInf(PolynomialStructure nextModel, Path runPath) throws InterruptedException, IOException {
		Utilities.runEventProcess(obsDir, eventDir -> {
			try {
				Set<Station> stations = eventDir.sacFileSet(sfn -> !sfn.isOBS()).stream()
						.map(name -> name.getStationName()).distinct().map(this::pickup).collect(Collectors.toSet());
				SyntheticDSMInfo dsmInfo = new SyntheticDSMInfo(nextModel, eventDir.getGlobalCMTID(), stations, "spc",
						1638.4, 256);
				GlobalCMTID id = eventDir.getGlobalCMTID();
				Files.createDirectories(runPath.resolve(id + "/spc"));
				// dsmInfo.outPSV(runPath.resolve(id + "/psv.inf"));
				dsmInfo.outSH(runPath.resolve(id + "/sh.inf"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} , 10, TimeUnit.MINUTES);
	}

	private SpcFileName toPSVname(SpcFileName shName) {
		String psvname = shName.getName().replace("SH.spc", "PSV.spc");
		GlobalCMTID id = new GlobalCMTID(shName.getSourceID());
		return new SpcFileName(psvPath.resolve(id.toString() + "/" + psvname));
	}

	private Map<GlobalCMTID, SourceTimeFunction> sourceTimeFunctionMap;

	private void makeSacFiles(EventFolder eventDir) throws IOException {
		Path spcPath = eventDir.toPath().resolve("spc");
		SourceTimeFunction sourceTimeFunction = sourceTimeFunctionMap.get(eventDir.getGlobalCMTID());
		try (Stream<Path> stream = Files.list(spcPath)) {
			stream.filter(path -> path.toString().endsWith("SH.spc")).forEach(shPath -> {
				SpcFileName shName = new SpcFileName(shPath);
				SpcFileName psvName = toPSVname(shName);
				try {
					DSMOutput shSPC = shName.read();
					DSMOutput psvSPC = psvName.read();
					SACMaker sm = new SACMaker(psvSPC, shSPC, sourceTimeFunction);
					sm.setComponents(components);
					sm.setOutPath(eventDir.toPath());
					sm.run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}

	private void setupRunpath(Path runPath) throws IOException {
		try (Stream<EventFolder> obsEventStream = Utilities.eventFolderSet(obsDir).stream()) {
			Files.createDirectories(runPath);
			Files.createDirectories(runPath.resolve("variances"));
			obsEventStream.map(e -> runPath.resolve(e.getGlobalCMTID().toString())).forEach(path -> {
				try {
					Files.createDirectories(path);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});
		} catch (Exception e) {
			System.err.println("Errors in creating " + runPath);
			e.printStackTrace();
			System.exit(1);
		}
	}

	private SACFileName toObs(SACFileName name) {
		GlobalCMTID id = name.getGlobalCMTID();
		String station = name.getStationName();
		String bottom = station + "." + id + ".T";
		return new SACFileName(obsDir.resolve(id.toString()).resolve(bottom));

	}

	private synchronized static void add(List<Double> list, double v) {
		list.add(v);
	}

	private static void computeValueforComparison(Path runPath) throws IOException {
		List<Double> variance = new ArrayList<>();
		List<Double> correlation = new ArrayList<>();

		Path variancePath = runPath.resolve("variances");
		try (Stream<Path> stream = Files.list(variancePath)) {
			stream.forEach(path -> {
				try {
					List<String> lines = Files.readAllLines(path);
					lines.parallelStream().map(line -> Double.parseDouble(line.split("\\s+")[1]))
							.forEach(d -> add(variance, d));
					lines.parallelStream().map(line -> Double.parseDouble(line.split("\\s+")[2]))
							.forEach(d -> add(correlation, d));

				} catch (Exception e) {
					e.printStackTrace();
				}

			});

		}

		double varAve = variance.stream().mapToDouble(d -> d.doubleValue()).average().getAsDouble();
		double corAve = correlation.stream().mapToDouble(d -> d.doubleValue()).average().getAsDouble();
		String line = varAve + " " + corAve;
		Files.write(runPath.resolve("variance.inf"), Arrays.asList(line));
	}

	private void compare(EventFolder eventDir) throws IOException {
		Set<SACFileName> synNameSet = eventDir.sacFileSet(name -> !name.isSYN());
		List<String> outLines = new ArrayList<>();
		synNameSet.forEach(synName -> {
			SACFileName obsName = toObs(synName);
			if (!obsName.exists() || !synName.exists())
				return;
			try {
				SACData obsSac = obsName.read();
				SACData synSac = synName.read();
				SacComparator comparator = new SacComparator(obsSac, synSac);
				double variance = comparator.getVarianceTS();
				double correlation = comparator.getCorrelationTS();
				double synScSS = comparator.getSynScSTime() - comparator.getSynSTime();
				double obsScSS = comparator.getObsScSTime() - comparator.getObsSTime();
				double scsS = obsScSS - synScSS;
				double scsSamp = comparator.getScSAmpRatio() / comparator.getSAmpRatio();
				outLines.add(
						obsName.getStationName() + " " + variance + " " + correlation + " " + scsS + " " + scsSamp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		try {
			if (0 < outLines.size())
				Files.write(eventDir.toPath().resolve("variance.inf"), outLines);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static boolean judge(Path current, Path candidate) {
		try {
			List<String> currentLines = Files.readAllLines(current.resolve("variance.inf"));
			List<String> candidateLines = Files.readAllLines(candidate.resolve("variance.inf"));
			double sigma = 0.5;// TODO sigma
			double currentVariance = Double.parseDouble(currentLines.get(0).split("\\s+")[0]);
			double candidateVariance = Double.parseDouble(candidateLines.get(0).split("\\s+")[0]);

			double currentLikelihood = Math.exp(-2 * currentVariance / sigma);
			double candidateLikelihood = Math.exp(-2 * candidateVariance / sigma);

			double percentage = candidateLikelihood / currentLikelihood;
			if (1 < percentage)
				return true;
			double rand = Math.random();
			return rand < percentage;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	// MPI thread
	private MPIControl mpi;

	private void setMPIEnvironment() throws IOException {
		if (!DSMMPI.shExists)
			throw new RuntimeException("mpi-sh does not exist in PATH.");
		mpi = hostFilePath == null ? new MPIControl(8) : new MPIControl(hostFilePath);
	}

	private Path hostFilePath;

	private void runDSM(Path runPath) {
		idSet.parallelStream().map(id -> id.toString()).map(runPath::resolve).map(e -> e.resolve("sh.inf")).map(mpi::tish)
				.map(this::postProcess).forEach(f -> {
					try {
						f.get();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
	}

	private Future<Integer> postProcess(Future<Path> future) {
		FutureTask<Integer> c = new FutureTask<>(() -> {
			Path event = future.get();
			if (event != null) {
				Path variancePath = event.resolveSibling("variances");
				processSac(new EventFolder(event));
				Files.move(event.resolve("variance.inf"), variancePath.resolve(event.getFileName() + ".inf"));
				Files.delete(event);
			}
			return 0;
		});
		new Thread(c).start();
		return c;
	}

	private void applyFilter(EventFolder eventDir) throws IOException {
		eventDir.sacFileSet(name -> !name.isSYN()).forEach(name -> {
			try {
				SACData sf = name.read().applyButterworthFilter(filter);
				sf.writeSAC(name.toPath());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void processSac(EventFolder eventDir) throws IOException {
		makeSacFiles(eventDir);
		applyFilter(eventDir);
		compare(eventDir);
		deleteIntermediate(eventDir);
	}

	private int findResumePoint() throws IOException {
		try (Stream<Path> stream = Files.list(montePath)) {
			return (int) stream.filter(Files::isDirectory)
					.filter(path -> path.getFileName().toString().startsWith("run")).count() - 1;
		}
	}

	private static void outputModelValue(Path runPath, PolynomialStructure structure) throws IOException {
		double[] vs = ModelProbability.readVs(structure);
		double[] q = ModelProbability.readQ(structure);
		List<String> lines = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			double r = 3505 + i * 50;
			lines.add(r + " " + vs[i] + " " + q[i]);
		}
		Files.write(runPath.resolve("candidate.txt"), lines);
	}

	private Set<GlobalCMTID> idSet;

	private void preProcess() throws IOException {
		idSet = Utilities.globalCMTIDSet(obsDir);
		readSetting();
		computeSourceTimeFunction();
	}

	private void readSetting() {
		np = 256;
		samplingHz = 20;
		tlen = 1638.4;
	}

	private int np;
	private double tlen;
	private double samplingHz;

	private void computeSourceTimeFunction() {
		sourceTimeFunctionMap = idSet.stream().collect(Collectors.toMap(id -> id,
				id -> new BoxcarSourceTimeFunction(np, tlen, samplingHz, id.getEvent().getHalfDuration())));
	}

	private static Path findModel(Path runPath) throws IOException {
		try (Stream<Path> pathStream = Files.list(runPath)) {
			return pathStream.filter(path -> !Files.isDirectory(path))
					.filter(path -> path.getFileName().toString().endsWith("model")).findFirst().get();
		}
	}

	private void prepareRun0() throws IOException {
		Path current = montePath.resolve("run0");
		Path variancePath = current.resolve("variances");
		if (Files.exists(variancePath))
			return;
		Files.createDirectories(variancePath);
		Utilities.eventFolderSet(current).forEach(ed -> {
			try {
				compare(ed);
				Files.move(ed.toPath().resolve("variance.inf"), variancePath.resolve(ed.getGlobalCMTID() + ".inf"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void run() throws IOException, InterruptedException {

		if (!canGO())
			return;

		int nRun = limit;
		boolean betterModel = false;
		int start = findResumePoint();
		System.out.println("MetroPolice is going.");
		Path current = montePath.resolve("run" + start);
		Path startModel = findModel(current);
		System.out.println("Starting from run" + start);
		int modelNumber = start;
		if (!Files.exists(startModel))
			throw new RuntimeException("Couldnt restart");
		preProcess();
		PolynomialStructure formerModel = new PolynomialStructure(startModel);
		if (modelNumber == 0)
			prepareRun0();
		computeValueforComparison(current);
		setMPIEnvironment();
		for (int iRun = start + 1; iRun < nRun + 1; iRun++) {
			Path candidate = montePath.resolve("run" + iRun);
			PolynomialStructure nextModel = ModelGenerator.nextStructure(formerModel);
			setupRunpath(candidate);
			createDSMInf(nextModel, candidate);

			// dsm run
			// make SacFile and apply filter and compare
			runDSM(candidate);
			computeValueforComparison(candidate);
			outputModelValue(candidate, nextModel);

			// 評価
			betterModel = judge(current, candidate);
			try {
				Path source = betterModel ? candidate.resolve("variance.inf") : current.resolve("variance.inf");
				Path target = candidate.resolve(iRun + ".var");
				Files.copy(source, target);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (betterModel) {
				nextModel.outPSV(candidate.resolve(iRun + ".model"));
				current = candidate;
				formerModel = nextModel;
				modelNumber = iRun;
			} else {
				formerModel.outPSV(candidate.resolve(iRun + "." + modelNumber + ".model"));
				nextModel.outPSV(candidate.resolve(iRun + ".model.botsu"));
			}
		}
	}

	private static void deleteIntermediate(EventFolder eventDir) throws IOException {
		eventDir.sacFileSet().forEach(name -> {
			try {
				Files.delete(name.toPath());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		Path eventPath = eventDir.toPath();
		try (DirectoryStream<Path> spcStream = Files.newDirectoryStream(eventPath.resolve("spc"))) {
			for (Path s : spcStream)
				Files.delete(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Files.delete(eventPath.resolve("sh.inf"));
		Files.delete(eventPath.resolve("spc"));
	}

	private static int limit = 100000;
	private final static Set<SACComponent> components = new HashSet<>(Arrays.asList(SACComponent.T));

	/**
	 * @param args
	 *            runpath, machinefile (option fo mpirun)
	 * @throws IOException if any
	 * @throws InterruptedException iff any
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		MetroPolice mp = new MetroPolice(args[0]);
		if (args.length == 2)
			mp.hostFilePath = Paths.get(args[1]);
		if (mp.hostFilePath != null)
			Files.copy(mp.hostFilePath, Paths.get("host.lst"));
		// MetroPolice mp = new
		// MetroPolice("/home/kensuke/data/WesternPacific/anelasticity/montecarlo/selection/group1");
		mp.run();

	}

	private ButterworthFilter filter;

	/**
	 * @param fMin
	 *            透過帯域 最小周波数
	 * @param fMax
	 *            透過帯域 最大周波数
	 * @param delta
	 *            sampling dt
	 */
	private void setFilter(double fMin, double fMax, int n) {
		double omegaH = fMax * 2 * Math.PI * 0.05;
		double omegaL = fMin * 2 * Math.PI * 0.05;
		filter = new BandPassFilter(omegaH, omegaL, n);
		filter.setBackward(true);
	}

}
