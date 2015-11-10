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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.complex.Complex;

import filehandling.sac.SACComponent;
import filehandling.spc.DSMOutput;
import filehandling.spc.PartialType;
import filehandling.spc.SpcFileName;
import filehandling.spc.ThreeDPartialMaker;
import manhattan.butterworth.BandPassFilter;
import manhattan.butterworth.ButterworthFilter;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.Location;
import manhattan.template.Station;
import manhattan.template.Utilities;
import manhattan.timewindow.TimewindowInformation;
import manhattan.timewindow.TimewindowInformationFile;

/**
 * 
 * shfp、shbp、psvfp,psvbp から作られたSPCファイルがあるディレクトリの操作
 * 
 * fpDIR, bpDIR(未定義の場合workDir)の下に イベントごとのフォルダ（FP）、ステーションごとのフォルダ(BP)があり その下の
 * modelNameにspcfileをおく
 * 
 * イベントフォルダはeventnameで定義されたもの ステーショフォルダは0000(stationname)
 * 
 * spcfileの名前は (point name).(station or eventname).(PB or PF)...(sh or psv).spc
 * 
 * halfDurationはevent informationファイルから読み取る
 * 
 * time window informationファイルの中からtime windowを見つける。 その中に入っている震源観測点成分の組み合わせのみ計算する
 * 
 * バンドパスをかけて保存する
 * 
 * 
 * TODO station とかの書き出し
 * 
 * 例： directory/19841006/*spc directory/0000KKK/*spc
 * 
 * 摂動点の情報がない摂動点に対しては計算しない
 * 
 * 
 * @since 2013/12/17
 * 
 * @version 2.2
 * 
 * @author Kensuke Konishi
 */
class PartialDatasetMaker extends parameter.PartialDatasetMaker {

	/**
	 * 一つのBackPropagationに対して、あるFPを与えた時の計算をさせるスレッドを作る
	 * 
	 * @author Kensuke
	 * 
	 */
	private class PartialComputation implements Runnable {

		private DSMOutput bp;
		private SpcFileName fpname;
		private DSMOutput fp;

		/**
		 * @param fp
		 * @param bpFile
		 */
		private PartialComputation(DSMOutput bp, SpcFileName fpFile) {
			this.bp = bp;
			fpname = fpFile;
		}

		/**
		 * cut partial derivative in [start-ext, start+ext] The ext is for
		 * filtering .
		 * 
		 * @param u
		 * @param property
		 * @return
		 */
		private Complex[] cutPartial(double[] u, TimewindowInformation timewindowInformation) {
			int cutstart = (int) (timewindowInformation.getStartTime() * partialSamplingHz) - ext;
			// cutstartが振り切れた場合0 からにする
			if (cutstart < 0)
				return null;
			int cutend = (int) (timewindowInformation.getEndTime() * partialSamplingHz) + ext;
			Complex[] cut = new Complex[cutend - cutstart];
			Arrays.parallelSetAll(cut, i -> new Complex(u[i + cutstart]));

			return cut;
		}

		private double[] sampleOutput(Complex[] u, TimewindowInformation timewindowInformation) {
			// 書きだすための波形
			int outnpts = (int) ((timewindowInformation.getEndTime() - timewindowInformation.getStartTime())
					* finalSamplingHz);
			double[] sampleU = new double[outnpts];

			// cutting a waveform for outputting
			Arrays.parallelSetAll(sampleU, j -> u[ext + j * step].getReal());
			return sampleU;
		}

		@Override
		public void run() {
			String stationName = bp.getSourceID();
			Station station = new Station(stationName, bp.getSourceLocation(), "DSM");
			GlobalCMTID id = new GlobalCMTID(fpname.getSourceID());
			// eventを探す

			touchedSet.add(id);

			// Pickup timewindows
			Set<TimewindowInformation> timewindowList = timewindowInformation.stream()
					.filter(info -> info.getStationName().equals(stationName))
					.filter(info -> info.getGlobalCMTID().equals(id)).collect(Collectors.toSet());

			// timewindow情報のないときスキップ
			if (timewindowList.isEmpty())
				return;

			// System.out.println("I am " + Thread.currentThread().getName());
			try {
				fp = fpname.read();
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			ThreeDPartialMaker threedPartialMaker = new ThreeDPartialMaker(fp, bp);
			threedPartialMaker.setComputesSourceTimeFunction(convolve);
			if (structure != null)
				threedPartialMaker.setStructure(structure);

			// i番目の深さの偏微分波形を作る
			for (int ibody = 0, nbody = fp.nbody(); ibody < nbody; ibody++) {
				// とりあえずtransverse（２）成分についての名前
				Location location = fp.getObserverPosition().toLocation(fp.getBodyR()[ibody]);
				for (PartialType type : partialTypes)
					for (SACComponent component : components) {
						if (!timewindowList.stream().anyMatch(info -> info.getComponent() == component))
							continue;
						double[] partial = threedPartialMaker.createPartial(component, ibody, type);
						timewindowList.stream().filter(info -> info.getComponent() == component).forEach(info -> {
							Complex[] u = cutPartial(partial, info);

							u = filter.applyFilter(u);
							double[] cutU = sampleOutput(u, info);

							PartialID pid = new PartialID(station, id, component, finalSamplingHz, info.getStartTime(),
									cutU.length, 1 / fmax, 1 / fmin, 0, convolve, location, type, cutU);
							try {
								partialDataWriter.addPartialID(pid);
								System.out.print(".");
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
					}

			}
		}
	}

	private ButterworthFilter filter;

	/**
	 * バンドパスを安定させるためwindowを左右に ext = max period(s) ずつ伸ばす
	 */
	private int ext;

	/**
	 * sacdataを何ポイントおきに取り出すか
	 */
	private int step;

	private Set<TimewindowInformation> timewindowInformation;

	private Set<GlobalCMTID> touchedSet = new HashSet<>();

	private PartialDatasetMaker(Path parameterPath) throws IOException {
		super(parameterPath);
	}

	private void setLog() throws IOException {
		synchronized (PartialDatasetMaker.class) {
			do {
				dateString = Utilities.getTemporaryString();
				logPath = workPath.resolve("pdm" + dateString + ".log");
			} while (Files.exists(logPath));
			Files.createFile(logPath);
		}
	}

	private void setOutput() throws IOException {

		// 書き込み準備
		Path idPath = workPath.resolve("partialID" + dateString + ".dat");
		Path datasetPath = workPath.resolve("partial" + dateString + ".dat");

		partialDataWriter = new WaveformDataWriter(idPath, datasetPath);
		writeLog("Creating " + idPath + " " + datasetPath);
		System.out.println("Creating " + idPath + " " + datasetPath);

	}

	private String dateString;

	private WaveformDataWriter partialDataWriter;

	private Path logPath;

	private static PartialDatasetMaker parse(String[] args) throws IOException {
		if (args.length != 0) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			return new PartialDatasetMaker(parameterPath);
		}
		return new PartialDatasetMaker(null);
	}

	private long startTime = System.nanoTime();
	private long endTime;

	/**
	 * @param args
	 *            [parameter file name]
	 */
	public static void main(String[] args) throws IOException {
		PartialDatasetMaker pdm = parse(args);
		long startTime = System.nanoTime();
		pdm.setLog();
		pdm.setTimeWindow();
		System.out.println("PartialDatasetMaker is going..");
		final int N_THREADS = 1 < args.length ? Integer.parseInt(args[1]) : Runtime.getRuntime().availableProcessors();
		pdm.writeLog("Running " + N_THREADS + " threads");
		pdm.setOutput();
		// filter設計
		pdm.setBandPassFilter();

		// bpディレクトリの中のステーションフォルダ （0000????, 0000><><, 。。。。）
		Path[] bp0000Paths = pdm.listBPPaths();

		// バンドパスを安定させるためwindowを左右に ext = max period(s) ずつ伸ばす
		pdm.ext = (int) (1 / pdm.fmin * pdm.partialSamplingHz);

		// sacdataを何ポイントおきに取り出すか
		pdm.step = (int) (pdm.partialSamplingHz / pdm.finalSamplingHz);

		int bpnum = 0;

		// bpフォルダごとにスタート
		for (Path bp0000Path : bp0000Paths) {
			// System.out.println("Now on " + ++stationNum + " th stations /"
			// + bpFolderNames.length + " :" + bpFolderName);
			String stationName = bp0000Path.getFileName().toString().replace("0000", "");
			Path bpModelPath = bp0000Path.resolve(pdm.modelName);

			// Set of global cmt IDs for the station in the timewindow.
			Set<GlobalCMTID> idSet = pdm.timewindowInformation.stream()
					.filter(info -> pdm.components.contains(info.getComponent()))
					.filter(info -> info.getStationName().equals(stationName)).map(info -> info.getGlobalCMTID())
					.collect(Collectors.toSet());

			if (idSet.isEmpty())
				continue;

			// bpModelFolder内 spectorfile
			Set<SpcFileName> bpFiles = Utilities.collectSpcFileName(bpModelPath);
			System.out.println(bpFiles.size() + " bpfiles are found");

			// stationに対するタイムウインドウが存在するfp内のmodelフォルダ
			Path[] fpEventPaths = idSet.stream().map(id -> pdm.fpPath.resolve(id.toString() + "/" + pdm.modelName))
					.filter(Files::exists).toArray(nEvent -> new Path[nEvent]);

			int donebp = 0;
			// bpフォルダ内の各bpファイルに対して
			for (SpcFileName bpname : bpFiles) {
				// create ThreadPool
				ExecutorService execs = Executors.newFixedThreadPool(N_THREADS);
				System.out.println("Working for " + bpname.getPath() + " " + ++donebp + "/" + bpFiles.size());
				// 摂動点の名前
				DSMOutput bp = bpname.read();
				String pointName = bp.getObserverID();

				// timewindowの存在するfpdirに対して
				// ｂｐファイルに対する全てのfpファイルを
				for (Path fpEventPath : fpEventPaths) {
					String eventName = fpEventPath.getParent().getFileName().toString();
					SpcFileName fpfile = new SpcFileName(
							fpEventPath.resolve(pointName + "." + eventName + ".PF..." + bpname.getMode() + ".spc"));
					if (!fpfile.exists())
						continue;
					PartialComputation pc = pdm.new PartialComputation(bp, fpfile);
					execs.execute(pc);
				}
				execs.shutdown();
				while (!execs.isTerminated()) {
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				pdm.partialDataWriter.flush();
				System.out.println();
			}
			pdm.writeLog(+bpnum++ + "th " + bp0000Path + " was done ");
		}
		pdm.terminate();
		System.err.println("PartialDataset finished in " + Utilities.toTimeString(System.nanoTime() - startTime));
	}

	private void terminate() throws IOException {
		partialDataWriter.close();
		endTime = System.nanoTime();
		long nanoSeconds = endTime - startTime;
		String endLine = "Everything is done in " + Utilities.toTimeString(nanoSeconds) + ". Over n out! ";
		System.out.println(endLine);
		writeLog(endLine);
		writeLog(partialDataWriter.getIdPath() + " " + partialDataWriter.getDataPath() + " were created");
	}

	private synchronized void writeLog(String line) throws IOException {
		try (PrintWriter pw = new PrintWriter(
				Files.newBufferedWriter(logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
			pw.print(new Date() + " : ");
			pw.println(line);

		}
	}

	/**
	 * bpディレクトリ内に存在するステーションフォルダリスト List BPfolders
	 */
	private Path[] listBPPaths() throws IOException {
		System.out.print("making a list of BPFolders..");
		Path[] bpPaths = null;
		try (Stream<Path> bpStream = Files.list(bpPath)) {
			bpPaths = bpStream.filter(Files::isDirectory)
					.filter(path -> path.getFileName().toString().startsWith("0000")).toArray(n -> new Path[n]);
		}
		writeLog("BPfolder : " + bpPath + " containing " + bpPaths.length + " bpfolders");
		System.out.println("the number of station folders is " + bpPaths.length);
		return bpPaths;
	}

	private void setBandPassFilter() throws IOException {
		System.out.println("Designing filter.");
		double omegaH = fmax * 2 * Math.PI / partialSamplingHz;
		double omegaL = fmin * 2 * Math.PI / partialSamplingHz;
		filter = new BandPassFilter(omegaH, omegaL, 4);
		writeLog(filter.toString());
		System.out.println("bandpass filter " + fmin + " " + fmax + " (Hz) was set");
	}

	private void setTimeWindow() throws IOException {
		// タイムウインドウの情報を読み取る。
		System.out.println("Reading timewindow information");
		timewindowInformation = TimewindowInformationFile.read(timewindowPath);

		boolean fpExistence = timewindowInformation.parallelStream().map(window -> window.getGlobalCMTID()).distinct()
				.allMatch(id -> Files.exists(fpPath.resolve(id.toString())));
		boolean bpExistence = timewindowInformation.parallelStream().map(window -> window.getStationName()).distinct()
				.allMatch(station -> Files.exists(bpPath.resolve("0000" + station)));
		if (!fpExistence || !bpExistence)
			throw new RuntimeException("propagation spectors are not enough for " + timewindowPath);
		writeLog(timewindowInformation.size() + " timewindows are found in " + timewindowPath);

	}

}
