package filehandling.spc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import filehandling.sac.SACData;
import manhattan.datacorrection.SourceTimeFunction;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.EventFolder;
import manhattan.template.Utilities;

/**
 * SpcSac Convertor from {@link SpectrumFile} to {@link SACData} file. According
 * to an information file: {@link parameter.SpcSAC}, it creates SAC files.
 * 
 * @version 0.1.3
 * 
 * @author Kensuke Konishi
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 */
final class SpcSAC extends parameter.SpcSAC {

	private SpcSAC(Path parameterPath) throws IOException {
		super(parameterPath);
	}

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		SpcSAC ss = null;
		if (0 < args.length) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			ss = new SpcSAC(parameterPath);
		} else
			ss = new SpcSAC(null);

		long start = System.nanoTime();
		int nThread = Runtime.getRuntime().availableProcessors();
		ss.outPath = ss.workPath.resolve("spcsac" + Utilities.getTemporaryString());
		System.out.println("SpcSAC is going.");
		System.out.println("Converting SPC files in " + ss.workPath + " to SAC files " + ss.outPath);
		Files.createDirectories(ss.outPath);
		Set<EventFolder> eventDirs = Utilities.eventFolderSet(ss.workPath);

		if (ss.sourceTimeFunction == -1)
			ss.readUserSourceTimeFunctions();

		for (EventFolder eventDir : eventDirs) {
			Path modelDir = eventDir.toPath().resolve(ss.modelName);
			if (!Files.exists(modelDir))
				continue;
			Files.createDirectories(ss.outPath.resolve(eventDir.getGlobalCMTID().toString()));
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

			if (ss.psvsh == 0)
				for (SpcFileName oneFile : psvSet.size() < shSet.size() ? psvSet : shSet) {
					SpcFileName pairFile = pairFile(oneFile);
					if (!pairFile.exists()) {
						System.err.println(pairFile + " does not exist");
						continue;
					}
					// System.out.println(oneFile + " " + pairFile);
					SpectrumFile oneSPC = SpectrumFile.getInstance(oneFile);
					SpectrumFile pairSPC = SpectrumFile.getInstance(pairFile);
					SACMaker sacMaker = ss.SacMakerMaker(oneSPC, pairSPC);
					execs.execute(sacMaker);
				}
			else
				for (SpcFileName oneFile : ss.psvsh == 1 ? psvSet : shSet) {
					SpectrumFile oneSPC = SpectrumFile.getInstance(oneFile);
					execs.execute(ss.SacMakerMaker(oneSPC, null));
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
	private SACMaker SacMakerMaker(SpectrumFile primeSPC, SpectrumFile secondarySPC) {
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

}
