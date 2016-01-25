package io.github.kensuke1984.kibrary.waveformdata;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.spc.DSMOutput;
import io.github.kensuke1984.kibrary.util.spc.FujiConversion;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.util.spc.SpcFileName;
import io.github.kensuke1984.kibrary.util.spc.SpcFileType;

/**
 * Creates a pair of files containing 1-D partial derivatives
 * 
 * TODO shとpsvの曖昧さ 両方ある場合ない場合等 現状では combineして対処している
 * 
 * Time length (tlen) and the number of step in frequency domain (np) in DSM
 * software must be same. Those values are set in a parameter file.
 * 
 * Only partials for radius written in a parameter file are computed.
 * 
 * <b>Assume there are no station with the same name but different networks in
 * same events</b> TODO
 * 
 * @version 0.2
 * 
 * 
 * @author Kensuke
 * 
 */
class Partial1DDatasetMaker extends parameter.Partial1DDatasetMaker {

	/**
	 * 追加したID数
	 */
	private int numberOfAddedID;

	private synchronized void add() {
		numberOfAddedID++;
	}

	private int lsmooth;

	private void setLsmooth() {
		int pow2np = Integer.highestOneBit(np);
		if (pow2np < np)
			pow2np *= 2;

		int lsmooth = (int) (0.5 * tlen * partialSamplingHz / pow2np);
		int ismooth = Integer.highestOneBit(lsmooth);
		this.lsmooth = ismooth == lsmooth ? lsmooth : ismooth * 2;
	}

	private class Worker implements Runnable {

		private SourceTimeFunction sourceTimeFunction;
		private GlobalCMTID id;

		@Override
		public void run() {
			try {
				writeLog("Running on " + id);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			Path spcFolder = eventDir.toPath().resolve(Partial1DDatasetMaker.this.modelName); // SPCの入っているフォルダ

			if (!Files.exists(spcFolder)) {
				System.err.println(spcFolder + " does not exist...");
				return;
			}

			Set<SpcFileName> spcFileNameSet = null;
			try {
				spcFileNameSet = Utilities.collectSpcFileName(spcFolder);
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}

			// compute source time function
			sourceTimeFunction = computeSourceTimeFunction();

			// すべてのspcファイルに対しての処理
			for (SpcFileName spcFileName : spcFileNameSet) {
				// 理論波形（非偏微分係数波形）ならスキップ
				if (spcFileName.isSynthetic())
					continue;
				System.out.println(spcFileName);

				if (!spcFileName.getSourceID().equals(id.toString())) {
					try {
						writeLog(spcFileName + " has an invalid global CMT ID.");
						continue;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				SpcFileType spcFileType = spcFileName.getFileType();

				// 3次元用のスペクトルなら省く
				if (spcFileType == SpcFileType.PB || spcFileType == SpcFileType.PF)
					continue;

				// check if the partialtype is included in computing list.
				PartialType partialType = PartialType.valueOf(spcFileType.toString());

				if (!(partialTypes.contains(partialType)
						|| (partialTypes.contains(PartialType.PARQ) && spcFileType == SpcFileType.PAR2)))
					continue;

				try {
					addPartialSpectrum(spcFileName);
				} catch (ClassCastException e) {
					// 出来上がったインスタンスがOneDPartialSpectrumじゃない可能性
					System.err.println(spcFileName + "is not 1D partial.");
					continue;
				} catch (Exception e) {
					System.err.println(spcFileName + " is invalid.");
					e.printStackTrace();
					try {
						writeLog(spcFileName + " is invalid.");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					continue;
				}
			}
			System.out.print(".");

		}

		private SourceTimeFunction computeSourceTimeFunction() {
			GlobalCMTID id = eventDir.getGlobalCMTID();
			double halfDuration = id.getEvent().getHalfDuration();
			switch (Partial1DDatasetMaker.super.sourceTimeFunction) {
			case -1:
				return userSourceTimeFunctions.get(id);
			case 0:
				return null;
			case 1:
				return SourceTimeFunction.boxcarSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration);
			case 2:
				return SourceTimeFunction.triangleSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration);
			default:
				throw new RuntimeException("Integer for source time function is invalid.");
			}
		}

		private void cutAndWrite(Station station, double[] filteredUt, TimewindowInformation t, double bodyR,
				PartialType partialType) {

			double[] cutU = sampleOutput(filteredUt, t);

			PartialID pid = new PartialID(station, id, t.getComponent(), finalSamplingHz, t.getStartTime(), cutU.length,
					1 / fmax, 1 / fmin, 0, sourceTimeFunction != null, new Location(0, 0, bodyR), partialType, cutU);
			try {
				partialDataWriter.addPartialID(pid);
				add();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		private void process(DSMOutput spectrum) {
			for (SACComponent component : components)
				spectrum.getSpcBodyList().stream().map(body -> body.getSpcComponent(component))
						.forEach(spcComponent -> {
							if (sourceTimeFunction != null)
								spcComponent.applySourceTimeFunction(sourceTimeFunction);
							spcComponent.toTimeDomain(lsmooth);
							spcComponent.applyGrowingExponential(spectrum.omegai(), tlen);
							spcComponent.amplitudeCorrection(tlen);
						});
		}

		private void addPartialSpectrum(SpcFileName spcname) throws IOException {
			DSMOutput spectrum = spcname.read();
			if (spectrum.tlen() != tlen || spectrum.np() != np) {
				System.err.println(spcname + " has different np or tlen.");
				writeLog(spcname + " has different np or tlen.");
				return;
			}

			String stationName = spcname.getObserverID();
			Station station = new Station(stationName, spectrum.getObserverPosition(), "DSM");
			PartialType partialType = PartialType.valueOf(spcname.getFileType().toString());
			DSMOutput qSpectrum = null;
			if (spcname.getFileType() == SpcFileType.PAR2 && partialTypes.contains(PartialType.PARQ)) {
				qSpectrum = fujiConversion.convert(spectrum);
				process(qSpectrum);
			}
			process(spectrum);

			for (SACComponent component : components) {
				Set<TimewindowInformation> tw = timewindowInformationSet.stream()
						.filter(info -> info.getStation().getStationName().equals(stationName))
						.filter(info -> info.getGlobalCMTID().equals(id))
						.filter(info -> info.getComponent() == component).collect(Collectors.toSet());

				if (tw.isEmpty())
					continue;

				for (int k = 0; k < spectrum.nbody(); k++) {
					double bodyR = spectrum.getBodyR()[k];
					boolean exists = false;
					for (double r : Partial1DDatasetMaker.super.bodyR)
						if (r == bodyR)
							exists = true;
					if (!exists)
						continue;
					double[] ut = spectrum.getSpcBodyList().get(k).getSpcComponent(component).getTimeseries();
					// applying the filter
					double[] filteredUt = filter.applyFilter(ut);
					for (TimewindowInformation t : tw)
						cutAndWrite(station, filteredUt, t, bodyR, partialType);
				}
				if (qSpectrum != null)
					for (int k = 0; k < spectrum.nbody(); k++) {
						double bodyR = spectrum.getBodyR()[k];
						boolean exists = false;
						for (double r : Partial1DDatasetMaker.super.bodyR)
							if (r == bodyR)
								exists = true;
						if (!exists)
							continue;
						double[] ut = qSpectrum.getSpcBodyList().get(k).getSpcComponent(component).getTimeseries();
						// applying the filter
						double[] filteredUt = filter.applyFilter(ut);
						for (TimewindowInformation t : tw)
							cutAndWrite(station, filteredUt, t, bodyR, PartialType.PARQ);
					}
			}
		}

		/**
		 * @param u
		 *            partial waveform
		 * @param timewindowInformation
		 *            cut information
		 * @return u cut by considering sampling Hz
		 */
		private double[] sampleOutput(double[] u, TimewindowInformation timewindowInformation) {
			int cutstart = (int) (timewindowInformation.getStartTime() * partialSamplingHz);
			// 書きだすための波形
			int outnpts = (int) ((timewindowInformation.getEndTime() - timewindowInformation.getStartTime())
					* finalSamplingHz);
			double[] sampleU = new double[outnpts];
			// cutting a waveform for outputting
			Arrays.setAll(sampleU, j -> u[cutstart + j * step]);

			return sampleU;
		}

		private EventFolder eventDir;

		private Worker(EventFolder eventDir) {
			this.eventDir = eventDir;
			id = eventDir.getGlobalCMTID();
		};

	}

	private Partial1DDatasetMaker(Path parameterPath) throws IOException {
		super(parameterPath);
	}

	/**
	 * filter いじらなくていい
	 */
	private ButterworthFilter filter;

	/**
	 * sacdataを何ポイントおきに取り出すか
	 */
	private int step;

	/**
	 * タイムウインドウの情報
	 */
	private Set<TimewindowInformation> timewindowInformationSet;

	//
	private WaveformDataWriter partialDataWriter;

	private Path logPath;

	private FujiConversion fujiConversion;

	private Map<GlobalCMTID, SourceTimeFunction> userSourceTimeFunctions;

	private void readSourceTimeFunctions() throws IOException {
		Set<GlobalCMTID> ids = timewindowInformationSet.stream().map(t -> t.getGlobalCMTID())
				.collect(Collectors.toSet());
		userSourceTimeFunctions = ids.stream().collect(Collectors.toMap(id -> id, id -> {
			try {
				Path sourceTimeFunctionPath = this.sourceTimeFunctionPath.resolve(id + ".stf");
				return SourceTimeFunction.readSourceTimeFunction(sourceTimeFunctionPath);
			} catch (Exception e) {
				throw new RuntimeException("Source time function file for " + id + " is broken.");
			}
		}));

	}

	/**
	 * @param args
	 *            [parameter file name]
	 */
	public static void main(String[] args) throws IOException {
		Partial1DDatasetMaker pdm = null;
		if (args.length != 0) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(parameterPath.toString());
			pdm = new Partial1DDatasetMaker(parameterPath);
		} else
			pdm = new Partial1DDatasetMaker(null);

		// System.exit(0);
		if (!pdm.canGO())
			System.exit(0);

		pdm.run();
	}

	private Set<GlobalCMTID> idSet;
	private Set<Station> stationSet;
	private Set<Location> perturbationLocationSet;

	private void setPerturbationLocation() {
		perturbationLocationSet = Arrays.stream(bodyR).mapToObj(r -> new Location(0, 0, r)).collect(Collectors.toSet());
	}

	public void run() throws IOException {
		String dateString = Utilities.getTemporaryString();

		logPath = workPath.resolve("partial1D" + dateString + ".log");
		// System.exit(0);

		System.err.println("Patial1DDatasetMaker is going.");
		long startTime = System.nanoTime();

		// pdm.createStreams();
		int N_THREADS = Runtime.getRuntime().availableProcessors();
		// N_THREADS = 2;
		writeLog("going with " + N_THREADS + " threads");

		if (partialTypes.contains(PartialType.PARQ))
			fujiConversion = new FujiConversion(PolynomialStructure.PREM);

		setLsmooth();
		writeLog("Set lsmooth " + lsmooth);

		// タイムウインドウの情報を読み取る。
		System.err.print("Reading timewindow information ");
		timewindowInformationSet = TimewindowInformationFile.read(timewindowPath);
		System.err.println("done");

		if (sourceTimeFunction == -1)
			readSourceTimeFunctions();

		// filter設計
		System.out.println("Designing filter.");
		setBandPassFilter();
		writeLog(filter.toString());
		setPerturbationLocation();
		stationSet = StationInformationFile.read(stationInformationFilePath);
		idSet = Utilities.globalCMTIDSet(workPath);
		// information about output partial types
		writeLog(partialTypes.stream().map(type -> type.toString())
				.collect(Collectors.joining(" ", "Computing for ", "")));

		// sacdataを何ポイントおきに取り出すか
		step = (int) (partialSamplingHz / finalSamplingHz);

		// System.exit(0);

		Set<EventFolder> eventDirs = Utilities.eventFolderSet(workPath);

		// create ThreadPool
		ExecutorService execs = Executors.newFixedThreadPool(N_THREADS);
		// System.exit(0);

		Path idPath = workPath.resolve("partial1DID" + dateString + ".dat");
		Path datasetPath = workPath.resolve("partial1D" + dateString + ".dat");
		try (WaveformDataWriter pdw = new WaveformDataWriter(idPath, datasetPath, stationSet, idSet, periodRanges,
				perturbationLocationSet)) {

			partialDataWriter = pdw;
			for (EventFolder eventDir : eventDirs)
				execs.execute(new Worker(eventDir));
			// break;
			execs.shutdown();

			while (!execs.isTerminated())
				Thread.sleep(100);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println();
		String endLine = "Partial1DDatasetMaker finished in " + Utilities.toTimeString(System.nanoTime() - startTime);
		System.err.println(endLine);
		writeLog(endLine);
		writeLog(idPath + " " + datasetPath + " were created");
		writeLog(numberOfAddedID + " IDs are added.");

	}

	private double[][] periodRanges;

	private void setBandPassFilter() throws IOException {
		double omegaH = fmax * 2 * Math.PI / partialSamplingHz;
		double omegaL = fmin * 2 * Math.PI / partialSamplingHz;
		filter = new BandPassFilter(omegaH, omegaL, 4);
		filter.setBackward(backward);
		periodRanges = new double[][] { { 1 / fmax, 1 / fmin } };
		writeLog(filter.toString());
	}

	private void writeLog(String line) throws IOException {
		Date now = new Date();
		synchronized (this) {
			try (PrintWriter pw = new PrintWriter(
					Files.newBufferedWriter(logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
				pw.println(now + " : " + line);
			}
		}
	}

	private boolean canGO() {
		boolean cango = true;
		if (!Files.exists(timewindowPath)) {
			new NoSuchFileException(timewindowPath.toString()).printStackTrace();
			cango = false;
		}

		return cango;
	}

}
