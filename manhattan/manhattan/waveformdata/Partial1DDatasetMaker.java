package manhattan.waveformdata;

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

import filehandling.sac.SACComponent;
import filehandling.spc.DSMOutput;
import filehandling.spc.FujiConversion;
import filehandling.spc.PartialType;
import filehandling.spc.SpcFileName;
import filehandling.spc.SpcFileType;
import manhattan.butterworth.BandPassFilter;
import manhattan.butterworth.ButterworthFilter;
import manhattan.datacorrection.BoxcarSourceTimeFunction;
import manhattan.datacorrection.SourceTimeFunction;
import manhattan.datacorrection.TriangleSourceTimeFunction;
import manhattan.dsminformation.PolynomialStructure;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.EventFolder;
import manhattan.template.Location;
import manhattan.template.Station;
import manhattan.template.Utilities;
import manhattan.timewindow.TimewindowInformation;
import manhattan.timewindow.TimewindowInformationFile;

/**
 * Creates dataset containing 1-D partial derivatives
 * １次元偏微分係数波形のデータセットを作る
 * 
 * 
 * TODO shとpsvの曖昧さ 両方ある場合ない場合等 現状では combineして対処している
 * 
 * time length and np in DSM software must be same. Those values are set in a
 * parameter file.
 * 
 * 
 * @since 2013/11/8 
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
				return new BoxcarSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration);
			case 2:
				return new TriangleSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration);
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
						.filter(info -> info.getStationName().equals(stationName))
						.filter(info -> info.getGlobalCMTID().equals(id))
						.filter(info -> info.getComponent() == component).collect(Collectors.toSet());

				if (tw.isEmpty())
					continue;

				for (int k = 0; k < spectrum.nbody(); k++) {
					double[] ut = spectrum.getSpcBodyList().get(k).getSpcComponent(component).getTimeseries();
					// applying the filter
					double[] filteredUt = filter.applyFilter(ut);
					for (TimewindowInformation t : tw)
						cutAndWrite(station,filteredUt, t, spectrum.getBodyR()[k], partialType);
				}
				if (qSpectrum != null)
					for (int k = 0; k < spectrum.nbody(); k++) {
						double[] ut = qSpectrum.getSpcBodyList().get(k).getSpcComponent(component).getTimeseries();
						// applying the filter
						double[] filteredUt = filter.applyFilter(ut);
						for (TimewindowInformation t : tw)
							cutAndWrite(station, filteredUt, t, spectrum.getBodyR()[k], PartialType.PARQ);
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

		String dateString = Utilities.getTemporaryString();

		pdm.logPath = pdm.workPath.resolve("partial1D" + dateString + ".log");
		// System.exit(0);

		System.err.println("Patial1DDatasetMaker is going.");
		long startTime = System.nanoTime();

		// pdm.createStreams();
		int N_THREADS = Runtime.getRuntime().availableProcessors();
		// N_THREADS = 2;
		pdm.writeLog("going with " + N_THREADS + " threads");

		if (pdm.partialTypes.contains(PartialType.PARQ))
			pdm.fujiConversion = new FujiConversion(PolynomialStructure.PREM);

		pdm.setLsmooth();
		pdm.writeLog("Set lsmooth " + pdm.lsmooth);

		// タイムウインドウの情報を読み取る。
		System.err.print("Reading timewindow information ");
		pdm.timewindowInformationSet = TimewindowInformationFile.read(pdm.timewindowPath);
		System.err.println("done");

		if (pdm.sourceTimeFunction == -1)
			pdm.readSourceTimeFunctions();

		// filter設計
		System.out.println("Designing filter.");
		pdm.setBandPassFilter();
		pdm.writeLog(pdm.filter.toString());

		// information about output partial types
		pdm.writeLog(pdm.partialTypes.stream().map(type -> type.toString())
				.collect(Collectors.joining(" ", "Computing for ", "")));

		// sacdataを何ポイントおきに取り出すか
		pdm.step = (int) (pdm.partialSamplingHz / pdm.finalSamplingHz);

		// System.exit(0);

		Set<EventFolder> eventDirs = Utilities.eventFolderSet(pdm.workPath);

		// create ThreadPool
		ExecutorService execs = Executors.newFixedThreadPool(N_THREADS);
		// System.exit(0);

		Path idPath = pdm.workPath.resolve("partial1DID" + dateString + ".dat");
		Path datasetPath = pdm.workPath.resolve("partial1D" + dateString + ".dat");
		try (WaveformDataWriter pdw = new WaveformDataWriter(idPath, datasetPath)) {

			pdm.partialDataWriter = pdw;
			for (EventFolder eventDir : eventDirs)
				execs.execute(pdm.new Worker(eventDir));
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
		pdm.writeLog(endLine);
		pdm.writeLog(idPath + " " + datasetPath + " were created");
		pdm.writeLog(pdm.numberOfAddedID + " IDs are added.");

	}

	private void setBandPassFilter() {
		double omegaH = fmax * 2 * Math.PI / partialSamplingHz;
		double omegaL = fmin * 2 * Math.PI / partialSamplingHz;
		filter = new BandPassFilter(omegaH, omegaL, 4);
		filter.setBackward(backward);
		// System.out.println("bandpass filter " + fmin + " " + fmax
		// + " (Hz) was set");
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
