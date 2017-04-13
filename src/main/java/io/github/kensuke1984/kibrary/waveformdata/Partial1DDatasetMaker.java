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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
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
 * @version 0.2.0.3
 * 
 * @author Kensuke Konishi
 * 
 */
public class Partial1DDatasetMaker implements Operation {
	private boolean backward;

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths
				.get(Partial1DDatasetMaker.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan Partial1DDatasetMaker");
			pw.println("##Path of a working directory (.)");
			pw.println("#workPath");
			pw.println("##SacComponents to be used(Z R T)");
			pw.println("#components");
			pw.println("##String if it is PREM, spector files are found in [event folder]/PREM (PREM)");
			pw.println("#modelName");
			pw.println("##Type source time function 0:none, 1:boxcar, 2:triangle. (0)");
			pw.println("##or folder name containing *.stf if you want to your own GLOBALCMTID.stf ");
			pw.println("#sourceTimeFunction");
			pw.println("##Path of a timewindow information file, must be set");
			pw.println("#timewindowPath timewindow.dat");
			pw.println("##PartialType[] compute types (PAR2)");
			pw.println("#partialTypes");
			pw.println("##Filter if backward filtering is applied (true)");
			pw.println("#backward");
			pw.println("##double time length:DSM parameter tlen (6553.6)");
			pw.println("#tlen 6553.6");
			pw.println("##int step of frequency domain DSM parameter np (1024)");
			pw.println("#np 1024");
			pw.println("##double minimum value of passband (0.005)");
			pw.println("#minFreq");
			pw.println("##double maximum value of passband (0.08)");
			pw.println("#maxFreq");
			pw.println("#double");
			pw.println("#partialSamplingHz cant change now");
			pw.println("##double sampling Hz in output dataset (1)");
			pw.println("#finalSamplingHz");
			pw.println("##radius for perturbation points, must be set");
			pw.println("#bodyR 3505 3555 3605");
			pw.println("##path of the time partials directory, must be set if PartialType containes TIME_SOURCE or TIME_RECEIVER");
			pw.println("#timePartialPath");
			pw.println("##Polynomial structure file (leave blank if PREM)");
			pw.println("#ps");
		}
		System.err.println(outPath + " is created.");
	}

	private Set<SACComponent> components;

	private Path workPath;
	
	private Path timePartialPath;

	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("modelName"))
			property.setProperty("modelName", "PREM");
		if (!property.containsKey("sourceTimeFunction"))
			property.setProperty("sourceTimeFunction", "0");
		if (!property.containsKey("partialTypes"))
			property.setProperty("partialTypes", "PAR2");
		if (!property.containsKey("backward"))
			property.setProperty("backward", "true");
		if (!property.containsKey("minFreq"))
			property.setProperty("minFreq", "0.005");
		if (!property.containsKey("maxFreq"))
			property.setProperty("maxFreq", "0.08");
		if (!property.containsKey("finalSamplingHz"))
			property.setProperty("finalSamplingHz", "1");
		if (!property.containsKey("timewindowPath"))
			throw new IllegalArgumentException("There is no information about timewindowPath.");
		if (!property.containsKey("tlen"))
			property.setProperty("tlen", "6553.6");
		if (!property.containsKey("np"))
			property.setProperty("np", "1024");
		if (!property.containsKey("ps"))
			property.setProperty("ps", "PREM");
	}

	/**
	 * parameterのセット
	 */
	private void set() {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));

		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		timewindowPath = getPath("timewindowPath");
		// pointFile = newFile(reader.getFirstValue("pointFile"));
		// System.out.println(reader.getFirstValue("pointFile"));
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());

		try {
			sourceTimeFunction = Integer.parseInt(property.getProperty("sourceTimeFunction"));
		} catch (Exception e) {
			sourceTimeFunction = -1;
			sourceTimeFunctionPath = getPath("sourceTimeFunction");
		}
		backward = Boolean.parseBoolean(property.getProperty("backward"));

		modelName = property.getProperty("modelName");

		partialTypes = Arrays.stream(property.getProperty("partialTypes").split("\\s+")).map(PartialType::valueOf)
				.collect(Collectors.toSet());
		
		if (partialTypes.contains(PartialType.TIME_RECEIVER) || partialTypes.contains(PartialType.TIME_SOURCE)) {
				timePartialPath = Paths.get(property.getProperty("timePartialPath"));
				if (!Files.exists(timePartialPath))
					throw new RuntimeException("The timePartialPath: " + timePartialPath + " does not exist");
		}
		
		tlen = Double.parseDouble(property.getProperty("tlen"));
		np = Integer.parseInt(property.getProperty("np"));
		minFreq = Double.parseDouble(property.getProperty("minFreq"));
		maxFreq = Double.parseDouble(property.getProperty("maxFreq"));
		bodyR = Arrays.stream(property.getProperty("bodyR").split("\\s+")).mapToDouble(Double::parseDouble).toArray();
		// partialSamplingHz
		// =Double.parseDouble(reader.getFirstValue("partialSamplingHz")); TODO
		finalSamplingHz = Double.parseDouble(property.getProperty("finalSamplingHz"));
		
		structurePath = property.getProperty("ps");
	}

	/**
	 * bp, fp フォルダの下のどこにspcファイルがあるか 直下なら何も入れない（""）
	 */
	private String modelName;
	
	private String structurePath;

	/**
	 * Path of a timewindow information file
	 */
	private Path timewindowPath;

	/**
	 * Partial types
	 */
	private Set<PartialType> partialTypes;

	/**
	 * bandpassの最小周波数（Hz）
	 */
	private double minFreq;

	/**
	 * bandpassの最大周波数（Hz）
	 */
	private double maxFreq;

	/**
	 * spcFileをコンボリューションして時系列にする時のサンプリングHz デフォルトは２０ TODOまだ触れない
	 */
	private double partialSamplingHz = 20;

	/**
	 * 最後に時系列で切り出す時のサンプリングヘルツ(Hz)
	 */
	private double finalSamplingHz;

	/**
	 * The folder contains source time functions.
	 */
	private Path sourceTimeFunctionPath;

	/**
	 * 0:none, 1:boxcar, 2:triangle.
	 */
	private int sourceTimeFunction;

	/**
	 * time length (DSM parameter)
	 */
	private double tlen;

	/**
	 * step of frequency domain (DSM parameter)
	 */
	private int np;

	/**
	 * radius of perturbation
	 */
	private double[] bodyR;
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
			Path spcFolder = eventDir.toPath().resolve(modelName); // SPCの入っているフォルダ

			if (!Files.exists(spcFolder)) {
				System.err.println(spcFolder + " does not exist...");
				return;
			}

			Set<SpcFileName> spcFileNames;
			try {
				spcFileNames = Utilities.collectSpcFileName(spcFolder);
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}

			// compute source time function
			sourceTimeFunction = computeSourceTimeFunction();
			
			Set<TimewindowInformation> timewindowCurrentEvent = timewindowInformationSet
					.stream()
					.filter(tw -> tw.getGlobalCMTID().equals(id))
					.collect(Collectors.toSet());
			
			// すべてのspcファイルに対しての処理
			for (SpcFileName spcFileName : spcFileNames) {
				// 理論波形（非偏微分係数波形）ならスキップ
				if (spcFileName.isSynthetic())
					continue;

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
					addPartialSpectrum(spcFileName, timewindowCurrentEvent);
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
			switch (Partial1DDatasetMaker.this.sourceTimeFunction) {
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
					1 / maxFreq, 1 / minFreq, t.getPhases(), 0, sourceTimeFunction != null, new Location(0, 0, bodyR), partialType,
					cutU);
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

		private void addPartialSpectrum(SpcFileName spcname, Set<TimewindowInformation> timewindowCurrentEvent) throws IOException {
			Set<TimewindowInformation> tmpTws = timewindowCurrentEvent.stream()
					.filter(info -> info.getStation().toString().equals(spcname.getObserverString()))
					.collect(Collectors.toSet());
			if (tmpTws.size() == 0) {
				return;
			}
			
			System.out.println(spcname);
			DSMOutput spectrum = spcname.read();
			if (spectrum.tlen() != tlen || spectrum.np() != np) {
				System.err.println(spcname + " has different np or tlen.");
				writeLog(spcname + " has different np or tlen.");
				return;
			}

			String stationName = spcname.getObserverName();
			String network = spcname.getObserverNetwork();
			Station station = new Station(stationName, spectrum.getObserverPosition(), network);
			PartialType partialType = PartialType.valueOf(spcname.getFileType().toString());
			DSMOutput qSpectrum = null;
			if (spcname.getFileType() == SpcFileType.PAR2 && partialTypes.contains(PartialType.PARQ)) {
				qSpectrum = fujiConversion.convert(spectrum);
				process(qSpectrum);
			}
			process(spectrum);

			for (SACComponent component : components) {
				Set<TimewindowInformation> tw = tmpTws.stream()
						.filter(info -> info.getStation().equals(station))
						.filter(info -> info.getGlobalCMTID().equals(id))
						.filter(info -> info.getComponent().equals(component)).collect(Collectors.toSet());

				if (tw.isEmpty()) {
					tmpTws.forEach(window -> {
						System.out.println(window);
						System.out.println(window.getStation().getPosition());
					});
					System.err.println(station.getPosition());
					System.err.println("Ignoring empty timewindow " + spcname + " " + station);
					continue;
				}

				for (int k = 0; k < spectrum.nbody(); k++) {
					double bodyR = spectrum.getBodyR()[k];
					boolean exists = false;
					for (double r : Partial1DDatasetMaker.this.bodyR)
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
						for (double r : Partial1DDatasetMaker.this.bodyR)
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
	
	private class WorkerTimePartial implements Runnable {
		
		private EventFolder eventDir;
		private GlobalCMTID id;
		
		@Override
		public void run() {
			try {
				writeLog("Running on " + id);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			Path timePartialFolder = eventDir.toPath();

			if (!Files.exists(timePartialFolder)) {
				throw new RuntimeException(timePartialFolder + " does not exist...");
			}
			
			Set<SACFileName> sacnameSet;
			try {
				sacnameSet = eventDir.sacFileSet()
						.stream()
						.filter(sacname -> sacname.isTemporalPartial())
						.collect(Collectors.toSet());
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
			
//			System.out.println(sacnameSet.size());
//			sacnameSet.forEach(name -> System.out.println(name));
			
			Set<TimewindowInformation> timewindowCurrentEvent = timewindowInformationSet
					.stream()
					.filter(tw -> tw.getGlobalCMTID().equals(id))
					.collect(Collectors.toSet());
			
			// すべてのsacファイルに対しての処理
			for (SACFileName sacname : sacnameSet) {
				try {
					addTemporalPartial(sacname, timewindowCurrentEvent);
				} catch (ClassCastException e) {
					// 出来上がったインスタンスがOneDPartialSpectrumじゃない可能性
					System.err.println(sacname + "is not 1D partial.");
					continue;
				} catch (Exception e) {
					System.err.println(sacname + " is invalid.");
					e.printStackTrace();
					try {
						writeLog(sacname + " is invalid.");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					continue;
				}
			}
			System.out.print(".");
//			
		}
		
		private void addTemporalPartial(SACFileName sacname, Set<TimewindowInformation> timewindowCurrentEvent) throws IOException {
			Set<TimewindowInformation> tmpTws = timewindowCurrentEvent.stream()
					.filter(info -> info.getStation().getStationName().equals(sacname.getStationName()))
					.collect(Collectors.toSet());
			if (tmpTws.size() == 0) {
				return;
			}
			
			System.out.println(sacname + " (time partials)");
			
			SACData sacdata = sacname.read();
			Station station = sacdata.getStation();
			
			for (SACComponent component : components) {
				Set<TimewindowInformation> tw = tmpTws.stream()
						.filter(info -> info.getStation().equals(station))
						.filter(info -> info.getGlobalCMTID().equals(id))
						.filter(info -> info.getComponent().equals(component)).collect(Collectors.toSet());

				if (tw.isEmpty()) {
					tmpTws.forEach(window -> {
						System.out.println(window);
						System.out.println(window.getStation().getPosition());
					});
					System.err.println(station.getPosition());
					System.err.println("Ignoring empty timewindow " + sacname + " " + station);
					continue;
				}
				
				for (TimewindowInformation t : tw) {
					double[] filteredUt = sacdata.createTrace().getY();
					cutAndWrite(station, filteredUt, t);
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
		
		private void cutAndWrite(Station station, double[] filteredUt, TimewindowInformation t) {

			double[] cutU = sampleOutput(filteredUt, t);
			Location stationLocation = new Location(station.getPosition().getLatitude(), station.getPosition().getLongitude(), Earth.EARTH_RADIUS);
			
			if (sourceTimeFunction == -1)
				System.err.println("Warning: check that the source time function used for the time partial is the same as the one used here.");
			
			PartialID PIDReceiverSide = new PartialID(station, id, t.getComponent(), finalSamplingHz, t.getStartTime(), cutU.length,
					1 / maxFreq, 1 / minFreq, t.getPhases(), 0, true, stationLocation, PartialType.TIME_RECEIVER,
					cutU);
			PartialID PIDSourceSide = new PartialID(station, id, t.getComponent(), finalSamplingHz, t.getStartTime(), cutU.length,
					1 / maxFreq, 1 / minFreq, t.getPhases(), 0, true, id.getEvent().getCmtLocation(), PartialType.TIME_SOURCE,
					cutU);
			
			try {
				partialDataWriter.addPartialID(PIDReceiverSide);
				add();
				partialDataWriter.addPartialID(PIDSourceSide);
				add();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		
		private WorkerTimePartial(EventFolder eventDir) {
			this.eventDir = eventDir;
			id = eventDir.getGlobalCMTID();
		};
	}

	public Partial1DDatasetMaker(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
	}

	private Properties property;

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
		Partial1DDatasetMaker pdm = new Partial1DDatasetMaker(Property.parse(args));

		if (!Files.exists(pdm.timewindowPath))
			throw new NoSuchFileException(pdm.timewindowPath.toString());

		pdm.run();
	}

	private Set<GlobalCMTID> idSet;
	private Set<Station> stationSet;
	private Set<Location> perturbationLocationSet;
	private Phase[] phases;

	private void setPerturbationLocation() {
		perturbationLocationSet = Arrays.stream(bodyR).mapToObj(r -> new Location(0, 0, r)).collect(Collectors.toSet());
		if (timePartialPath != null) {
			if (stationSet.isEmpty() || idSet.isEmpty())
				throw new RuntimeException("stationSet and idSet must be set before perturbationLocation");
			stationSet.forEach(station -> perturbationLocationSet.add(new Location(station.getPosition().getLatitude(),
					station.getPosition().getLongitude(), Earth.EARTH_RADIUS)));
			idSet.forEach(id -> perturbationLocationSet.add(id.getEvent().getCmtLocation()));
		}
	}

	@Override
	public void run() throws IOException {
		String dateString = Utilities.getTemporaryString();

		logPath = workPath.resolve("partial1D" + dateString + ".log");

		System.err.println(Partial1DDatasetMaker.class.getName() + " is going.");
		long startTime = System.nanoTime();

		// pdm.createStreams();
		int N_THREADS = Runtime.getRuntime().availableProcessors();
		// N_THREADS = 2;
		writeLog("going with " + N_THREADS + " threads");
		
		PolynomialStructure structure = null;
		switch (structurePath) {
		case "PREM":
		case "prem":
			structure = PolynomialStructure.PREM;
			break;
		default:
			try {
				structure = new PolynomialStructure(Paths.get(structurePath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		}
		
		if (partialTypes.contains(PartialType.PARQ))
			fujiConversion = new FujiConversion(structure);
		
		System.err.println("going with the structure " + structurePath);
		writeLog("going with the structure " + structurePath);
		
		setLsmooth();
		writeLog("Set lsmooth " + lsmooth);

		// タイムウインドウの情報を読み取る。
		System.err.print("Reading timewindow information ");
		timewindowInformationSet = TimewindowInformationFile.read(timewindowPath);
		System.err.println("done");

		if (sourceTimeFunction == -1)
			readSourceTimeFunctions();

		// filter設計
		System.err.println("Designing filter.");
		setBandPassFilter();
		writeLog(filter.toString());
		stationSet = timewindowInformationSet.parallelStream().map(TimewindowInformation::getStation)
				.collect(Collectors.toSet());
		idSet = Utilities.globalCMTIDSet(workPath);
		setPerturbationLocation();
		phases = timewindowInformationSet.parallelStream().map(TimewindowInformation::getPhases).flatMap(p -> Stream.of(p))
				.distinct().toArray(Phase[]::new);
		// information about output partial types
		writeLog(partialTypes.stream().map(Object::toString).collect(Collectors.joining(" ", "Computing for ", "")));

		// sacdataを何ポイントおきに取り出すか
		step = (int) (partialSamplingHz / finalSamplingHz);

		Set<EventFolder> eventDirs = Utilities.eventFolderSet(workPath);
		Set<EventFolder> timePartialEventDirs = new HashSet<>();
		if (timePartialPath != null)
			timePartialEventDirs = Utilities.eventFolderSet(timePartialPath);

		// create ThreadPool
		ExecutorService execs = Executors.newFixedThreadPool(N_THREADS);

		Path idPath = workPath.resolve("partial1DID" + dateString + ".dat");
		Path datasetPath = workPath.resolve("partial1D" + dateString + ".dat");
		try (WaveformDataWriter pdw = new WaveformDataWriter(idPath, datasetPath, stationSet, idSet, periodRanges,
				phases, perturbationLocationSet)) {

			partialDataWriter = pdw;
			for (EventFolder eventDir : eventDirs)
				execs.execute(new Worker(eventDir));
			// break;
			for (EventFolder eventDir2 : timePartialEventDirs)
				execs.execute(new WorkerTimePartial(eventDir2));
			execs.shutdown();

			while (!execs.isTerminated())
				Thread.sleep(100);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.err.println();
		String endLine = Partial1DDatasetMaker.class.getName() + " finished in "
				+ Utilities.toTimeString(System.nanoTime() - startTime);
		System.err.println(endLine);
		writeLog(endLine);
		writeLog(idPath + " " + datasetPath + " were created");
		writeLog(numberOfAddedID + " IDs are added.");

	}

	private double[][] periodRanges;

	private void setBandPassFilter() throws IOException {
		double omegaH = maxFreq * 2 * Math.PI / partialSamplingHz;
		double omegaL = minFreq * 2 * Math.PI / partialSamplingHz;
		filter = new BandPassFilter(omegaH, omegaL, 4);
		filter.setBackward(backward);
		periodRanges = new double[][] { { 1 / maxFreq, 1 / minFreq } };
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

	@Override
	public Path getWorkPath() {
		return workPath;
	}

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

}
