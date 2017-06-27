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
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.waveformdata.Partial1DDatasetMaker;

/**
 * SpcSAC Converter from {@link SpectrumFile} to {@link SACData} file. According
 * to an information file: {@link parameter.SpcSAC}, it creates SAC files.
 * 
 * It converts all the SPC files in event folders/model under the workDir set by
 * the information file. If you leave 'model name' blank and each event folder
 * has only one folder, then model name will be set automatically the name of
 * the folder.
 * 
 * @version 0.2.4.2
 * 
 * @author Kensuke Konishi
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 */
public final class SpcSAC implements Operation {

	public SpcSAC(Properties properties) throws IOException {
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
		if (!property.containsKey("psvPath"))
			property.setProperty("psvPath", "");
		if (!property.containsKey("shPath"))
			property.setProperty("shPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("sourceTimeFunction"))
			property.setProperty("sourceTimeFunction", "0");
		if (!property.containsKey("timePartial"))
			property.setProperty("timePartial", "false");
		if (!property.containsKey("modelName"))
			property.setProperty("modelName", "");
		if (!property.containsKey("partialWaveform"))
			property.setProperty("partialWaveform", "false");
	}

	private void set() throws IOException {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));
		if (!property.getProperty("psvPath").equals("null"))
			psvPath = getPath("psvPath");
		if (!property.getProperty("shPath").equals("null"))
			shPath = getPath("shPath");

		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		if (psvPath != null && !Files.exists(psvPath))
			throw new RuntimeException("The psvPath: " + psvPath + " does not exist");
		if (shPath != null && !Files.exists(shPath))
			throw new RuntimeException("The shPath: " + shPath + " does not exist");

		modelName = property.getProperty("modelName");

		if (modelName.isEmpty())
			setModelName();

		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());

		try {
			sourceTimeFunction = Integer.parseInt(property.getProperty("sourceTimeFunction"));
			if (sourceTimeFunction != 0 && sourceTimeFunction != 1 && sourceTimeFunction != 2)
				throw new IllegalArgumentException(
						"The property for Source time function is invalid. It must be 0, 1 or a source time function folder path.");
		} catch (Exception e) {
			sourceTimeFunction = -1;
			sourceTimeFunctionPath = workPath.resolve(property.getProperty("sourceTimeFunction"));
			if (!Files.exists(sourceTimeFunctionPath))
				throw new RuntimeException(
						"Source time function folder: " + sourceTimeFunctionPath + " does not exist");
		}

		computesPartial = Boolean.parseBoolean(property.getProperty("timePartial"));
		
		partialWaveform = Boolean.parseBoolean(property.getProperty("partialWaveform"));

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

	private Path psvPath;
	private Path shPath;

	/**
	 * If it computes temporal partial or not.
	 */
	private boolean computesPartial;
	
	/**
	 * If it compute 1D partial waveforms
	 */
	private boolean partialWaveform;

	
	/**
	 * You should set partial type, if you want to compute 1D partial waveform.
	 */
	private PartialType partialType;


	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Properties property = Property.parse(args);
		SpcSAC ss = new SpcSAC(property);
		long start = System.nanoTime();
		System.err.println(SpcSAC.class.getName() + " is going.");
		ss.run();
		System.err
				.println(SpcSAC.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - start));
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

	private void setModelName() throws IOException {
		Set<EventFolder> eventFolders = new HashSet<>();
		if (psvPath != null)
			eventFolders.addAll(Utilities.eventFolderSet(psvPath));
		if (shPath != null)
			eventFolders.addAll(Utilities.eventFolderSet(shPath));
		Set<String> possibleNames = eventFolders.stream().flatMap(ef -> Arrays.stream(ef.listFiles(File::isDirectory)))
				.map(File::getName).collect(Collectors.toSet());
		if (possibleNames.size() != 1)
			throw new RuntimeException(
					"There are no model folder in event folders or more than one folder. You must specify 'modelName' in the case.");

		String modelName = possibleNames.iterator().next();
		if (eventFolders.stream().map(EventFolder::toPath).map(p -> p.resolve(modelName)).allMatch(Files::exists))
			this.modelName = modelName;
		else
			throw new RuntimeException("There are some events without model folder " + modelName);
	}

	private Set<SpcFileName> collectSHSPCs() throws IOException {
		Set<SpcFileName> shSet = new HashSet<>();
		Set<EventFolder> eventFolderSet = Utilities.eventFolderSet(shPath);
//		if (!partialWaveform) {
			for (EventFolder eventFolder : eventFolderSet) {
				Path modelFolder = eventFolder.toPath().resolve(modelName);
				Utilities.collectSpcFileName(modelFolder).stream()
					.filter(f -> !f.getName().contains("par") && f.getName().endsWith("SH.spc")).forEach(shSet::add);
			}
//		}	
		/**
		if (partialWaveform) {
			for (EventFolder eventFolder : eventFolderSet) {
				Path modelFolder = eventFolder.toPath().resolve(modelName);
				Utilities.collectSpcFileName(modelFolder).stream()
					.filter(f -> !f.getName().contains("par") && f.getName().endsWith("SH.spc") && f.getName().contains(partialType.name()))
					.forEach(shSet::add);
			}
		}
		**/
		return shSet;
	}

	private Set<SpcFileName> collectPSVSPCs() throws IOException {
		Set<SpcFileName> psvSet = new HashSet<>();
		Set<EventFolder> eventFolderSet = Utilities.eventFolderSet(psvPath);
		for (EventFolder eventFolder : eventFolderSet) {
			Path modelFolder = eventFolder.toPath().resolve(modelName);
			Utilities.collectSpcFileName(modelFolder).stream()
					.filter(f -> !f.getName().contains("par") && f.getName().endsWith("PSV.spc")).forEach(psvSet::add);
		}
		return psvSet;
	}

	private Set<SpcFileName> psvSPCs;
	private Set<SpcFileName> shSPCs;

	private SpcFileName pairFile(SpcFileName psvFileName) {
		if (psvFileName.getMode() == SpcFileComponent.SH)
			return null;
		return new SpcFileName(shPath.resolve(psvFileName.getSourceID() + "/" + modelName + "/"
				+ psvFileName.getName().replace("PSV.spc", "SH.spc")));
	}

	@Override
	public void run() throws IOException {
		int nThread = Runtime.getRuntime().availableProcessors();
		System.err.println("We use "+nThread+"threads.");
		outPath = workPath.resolve("spcsac" + Utilities.getTemporaryString());
		System.err.println("Work folder is " + workPath.toAbsolutePath());
		System.err.println("Converting SPC files in the work folder to SAC files in " + outPath);
		System.err.println("Model name is " + modelName);
		if (sourceTimeFunction == -1)
			readUserSourceTimeFunctions();
		Files.createDirectories(outPath);

		if (psvPath != null && (psvSPCs = collectPSVSPCs()).isEmpty())
			throw new RuntimeException("No PSV spector files are found.");

		if (shPath != null && (shSPCs = collectSHSPCs()).isEmpty())
			throw new RuntimeException("No SH spector files are found.");

		ExecutorService execs = Executors.newFixedThreadPool(nThread);
		// single
		if (psvPath == null || shPath == null)
			for (SpcFileName spc : psvSPCs != null ? psvSPCs : shSPCs) {
				SpectrumFile one = SpectrumFile.getInstance(spc);
				Files.createDirectories(outPath.resolve(spc.getSourceID()));
				if (partialWaveform)
					Files.createDirectories(outPath.resolve(spc.getSourceID()).resolve("partialWaveform"));
				execs.execute(createSACMaker(one, null));
			}

		// both
		else
			for (SpcFileName spc : psvSPCs) {
				SpectrumFile one = SpectrumFile.getInstance(spc);
				SpcFileName pair = pairFile(spc);
				if (!pair.exists()) {
					System.err.println(pair + " does not exist");
					continue;
				}
				SpectrumFile two = SpectrumFile.getInstance(pairFile(spc));
				Files.createDirectories(outPath.resolve(spc.getSourceID()));
				execs.execute(createSACMaker(one, two));
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

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(SpcSAC.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan SpcSAC");
			pw.println("##Path of a working folder (.)");
			pw.println("#workPath");
			pw.println("##SACComponents for output (Z R T)");
			pw.println("#components");
			pw.println("###If you do NOT want to use PSV or SH, you set the one 'null'.");
			pw.println("##Path of a PSV folder (.)");
			pw.println("#psvPath");
			pw.println("##Path of an SH folder (.)");
			pw.println("#shPath");
			pw.println("##String if it is PREM spector file is in eventDir/PREM ");
			pw.println("##if it is unset, then automatically set as the name of a folder in eventDir");
			pw.println("##but the eventDirs can have only one folder inside.");
			pw.println("#modelName");
			pw.println("##Type source time function 0:none, 1:boxcar, 2:triangle. (0)");
			pw.println("##or folder name containing *.stf if you want to your own GLOBALCMTID.stf ");
			pw.println("#sourceTimeFunction");
			pw.println("#SamplingHz (20) !You can not change yet!");
			pw.println("#samplingHz");
			pw.println("#timePartial If it is true, then temporal partial is computed. (false)");
			pw.println("#timePartial");
		}
		System.err.println(outPath + " is created.");
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
		sm.setTemporalDifferentiation(computesPartial);
		sm.setOutPath(outPath.resolve(primeSPC.getSourceID()));
		if (partialWaveform){
			try {
				sm.outputPAR(outPath.resolve(primeSPC.getSourceID()).resolve("partialWaveform"));
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
		return sm;
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
