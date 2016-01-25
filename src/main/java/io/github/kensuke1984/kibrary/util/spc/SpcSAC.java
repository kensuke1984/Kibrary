package io.github.kensuke1984.kibrary.util.spc;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;

/**
 * SpcSAC Converter from {@link SpectrumFile} to {@link SACData} file. According
 * to an information file: {@link parameter.SpcSAC}, it creates SAC files.
 * 
 * It converts all the SPC files in event folders/model under the workDir set by
 * the information file. If you leave 'model name' blank and each event folder
 * has only one folder, then model name will be set automatically the name of
 * the folder.
 * 
 * @version 0.2
 * 
 * @author Kensuke Konishi
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 */
public final class SpcSAC implements Operation {

	public SpcSAC(Properties properties) {
		property = (Properties) properties.clone();
		set();
	}

	private Properties property;

	/**
	 * Path for the work folder
	 */
	private Path workPath;

	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("sourceTimeFunction"))
			property.setProperty("sourceTimeFunction", "0");
		if (!property.containsKey("timePartial"))
			property.setProperty("timePartial", "false");
		if (!property.containsKey("psvsh"))
			property.setProperty("psvsh", "0");
		if (!property.containsKey("modelName"))
			property.setProperty("modelName", "");
	}

	private void set() {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));

		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");

		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());

		try {
			sourceTimeFunction = Integer.parseInt(property.getProperty("sourceTimeFunction"));
			if (sourceTimeFunction != 0 && sourceTimeFunction != 1)
				throw new IllegalArgumentException(
						"The property for Source time function is invalid. It must be 0, 1 or a source time function folder path.");
		} catch (Exception e) {
			sourceTimeFunction = -1;
			sourceTimeFunctionPath = workPath.resolve(property.getProperty("sourceTimeFunction"));
			if (!Files.exists(sourceTimeFunctionPath))
				throw new RuntimeException(
						"Source time function folder: " + sourceTimeFunctionPath + " does not exist");
		}

		timePartial = Boolean.parseBoolean(property.getProperty("timePartial"));
		psvsh = Integer.parseInt(property.getProperty("psvsh"));

		modelName = property.getProperty("modelName");

		samplingHz = 20; // TODO
	}

	private Path sourceTimeFunctionPath;

	/**
	 * 計算するcomponents
	 */
	private Set<SACComponent> components;

	/**
	 * bp, fp フォルダの下のどこにspcファイルがあるか 直下なら何も入れない（""）
	 */
	private String modelName;

	/**
	 * サンプリングヘルツ 当面は２０Hz固定
	 */
	private double samplingHz;

	/**
	 * source time function.-1:Users, 0: none, 1: boxcar, 2: triangle
	 */
	private int sourceTimeFunction;

	/**
	 * psv と sh のカップリングをどうするか 0:both(default), 1:psv, 2:sh
	 */
	private int psvsh;

	/**
	 * タイムパーシャルを計算するか？
	 */
	private boolean timePartial;

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Properties property = new Properties();
		if (args.length == 0)
			property.load(Files.newBufferedReader(Operation.findPath()));
		else if (args.length == 1)
			property.load(Files.newBufferedReader(Paths.get(args[0])));
		else
			throw new IllegalArgumentException("too many arguments. It should be 0 or 1(property file name)");

		SpcSAC ss = new SpcSAC(property);

		long start = System.nanoTime();
		System.out.println("SpcSAC is going.");
		ss.run();
		System.out.println("SpcSAC finished in " + Utilities.toTimeString(System.nanoTime() - start));
	}

	private void readUserSourceTimeFunctions() throws IOException {
		Set<GlobalCMTID> ids = Utilities.globalCMTIDSet(workPath);
		userSourceTimeFunctions = new HashMap<>(ids.size());
		for (GlobalCMTID id : ids)
			userSourceTimeFunctions.put(id,
					SourceTimeFunction.readSourceTimeFunction(sourceTimeFunctionPath.resolve(id + ".stf")));
	}

	private Map<GlobalCMTID, SourceTimeFunction> userSourceTimeFunctions;

	private SourceTimeFunction getSourceTimeFunction(int np, double tlen, double samplingHz, GlobalCMTID id) {
		double halfDuration = id.getEvent().getHalfDuration();
		switch (sourceTimeFunction) {
		case -1:
			return userSourceTimeFunctions.get(id);
		case 0:
			return null;
		case 1:
			return SourceTimeFunction.boxcarSourceTimeFunction(np, tlen, samplingHz, halfDuration);
		case 2:
			return SourceTimeFunction.triangleSourceTimeFunction(np, tlen, samplingHz, halfDuration);
		default:
			throw new RuntimeException("Integer for source time function is invalid.");
		}
	}

	/**
	 * @param eventFolder
	 *            in which search a model folder
	 * @return the model folder in the folder. If there are no folders in the
	 *         event folder or more than one folder in the event folder, null is
	 *         returned.
	 */
	private static Path searchModelDir(EventFolder eventFolder) {
		Path[] paths = Arrays.stream(eventFolder.listFiles()).filter(File::isDirectory).map(File::toPath)
				.toArray(n -> new Path[n]);
		if (paths.length == 0)
			System.err.println("No model folder in " + eventFolder);
		else if (1 < paths.length)
			System.err.println("More than one model folders in " + eventFolder);
		else
			return paths[0];
		return null;
	}

	@Override
	public void run() throws IOException {
		int nThread = Runtime.getRuntime().availableProcessors();
		outPath = workPath.resolve("spcsac" + Utilities.getTemporaryString());
		System.out.println("Work folder is " + workPath.toAbsolutePath());
		System.out.println("Converting SPC files in the work folder to SAC files in " + outPath);
		Set<EventFolder> eventDirs = Utilities.eventFolderSet(workPath);
		if (sourceTimeFunction == -1)
			readUserSourceTimeFunctions();
		Files.createDirectories(outPath);

		for (EventFolder eventDir : eventDirs) {
			Path modelDir = null;
			if (modelName.equals(""))
				modelDir = searchModelDir(eventDir);
			else
				modelDir = eventDir.toPath().resolve(modelName);

			if (modelDir == null || !Files.exists(modelDir))
				continue;
			Files.createDirectories(outPath.resolve(eventDir.getGlobalCMTID().toString()));
			ExecutorService execs = Executors.newFixedThreadPool(nThread);

			Set<SpcFileName> spcSet = Utilities.collectSpcFileName(modelDir);
			Set<SpcFileName> psvSet = new HashSet<>();
			Set<SpcFileName> shSet = new HashSet<>();
			spcSet.stream().filter(file -> !file.getName().contains("par")).forEach(file -> {
				if (file.getName().contains("PSV.spc"))
					psvSet.add(file);
				else if (file.getName().contains("SH.spc"))
					shSet.add(file);
			});

			if (psvsh == 0)
				for (SpcFileName oneFile : psvSet.size() < shSet.size() ? psvSet : shSet)
					try {
						SpcFileName pairFile = pairFile(oneFile);
						if (!pairFile.exists()) {
							System.err.println(pairFile + " does not exist");
							continue;
						}
						SpectrumFile oneSPC = SpectrumFile.getInstance(oneFile);
						SpectrumFile pairSPC = SpectrumFile.getInstance(pairFile);
						execs.execute(createSACMaker(oneSPC, pairSPC));
					} catch (Exception e) {
						e.printStackTrace();
					}

			else
				for (SpcFileName oneFile : psvsh == 1 ? psvSet : shSet)
					try {
						SpectrumFile oneSPC = SpectrumFile.getInstance(oneFile);
						execs.execute(createSACMaker(oneSPC, null));
					} catch (Exception e) {
						e.printStackTrace();
					}

			execs.shutdown();
			while (!execs.isTerminated()) {
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
	}

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(SpcSAC.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("#!java filehandling.spc.SpcSAC");
			pw.println("#SACComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("#Path of a working folder (.)");
			pw.println("#workPath");
			pw.println("#String if it is PREM spector file is in eventDir/PREM ");
			pw.println("#if it is unset, then automatically set as the name of a folder in eventDir");
			pw.println("#but the eventDirs can have only one folder inside.");
			pw.println("#modelName");
			pw.println("#Type source time function 0:none, 1:boxcar, 2:triangle. (0)");
			pw.println("#or folder name containing *.stf if you want to your own GLOBALCMTID.stf ");
			pw.println("#sourceTimeFunction");
			pw.println("#Mode summation 0:psv+sh 1:psv 2:sh (0)");
			pw.println("#psvsh");
			pw.println("#SamplingHz (20) !You can not change yet!");
			pw.println("#samplingHz");
			pw.println("#timePartial If it is true, then temporal partial is computed. (false)");
			pw.println("#timePartial");

		}
		System.out.println(outPath + " is created.");
	}

	private Path outPath;

	/**
	 * 二つのspc(sh, psv)から SacMakerを作る {@link SACMaker}
	 * 
	 * @param primeSPC
	 * 
	 * @param secondarySPC
	 *            null is ok
	 * @return {@link SACMaker}
	 */
	private SACMaker createSACMaker(SpectrumFile primeSPC, SpectrumFile secondarySPC) {
		SourceTimeFunction sourceTimeFunction = getSourceTimeFunction(primeSPC.np(), primeSPC.tlen(), samplingHz,
				new GlobalCMTID(primeSPC.getSourceID()));
		SACMaker sm = new SACMaker(primeSPC, secondarySPC, sourceTimeFunction);
		sm.setComponents(components);
		sm.setTemporalDifferentiation(timePartial);
		sm.setOutPath(outPath.resolve(primeSPC.getSourceID()));
		return sm;
	}

	private static SpcFileName pairFile(SpcFileName spcFile) {
		String name = spcFile.getName();
		String newName = name.endsWith("SH.spc") ? name.replace("SH.spc", "PSV.spc")
				: name.replace("PSV.spc", "SH.spc");
		return new SpcFileName(spcFile.toPath().resolveSibling(newName));

	}

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

}
