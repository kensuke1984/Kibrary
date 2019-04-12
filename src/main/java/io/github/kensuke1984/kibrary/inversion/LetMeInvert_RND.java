package io.github.kensuke1984.kibrary.inversion;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Precision;

import ucar.nc2.iosp.misc.NmcObsLegacy;
import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.datacorrection.TakeuchiStaticCorrection;
import io.github.kensuke1984.kibrary.selection.DataSelectionInformation;
import io.github.kensuke1984.kibrary.selection.DataSelectionInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTCatalog;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.globalcmt.NDK;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

/**
 * 
 * Let's invert
 * 
 * @version 2.0.3.3
 * 
 * @author Kensuke Konishi
 * 
 */
public class LetMeInvert_RND implements Operation {
	/**
	 * 観測波形、理論波形の入ったファイル (BINARY)
	 */
	protected Path waveformPath;

	/**
	 * 求めたい未知数を羅列したファイル (ASCII)
	 */
	protected Path unknownParameterListPath;

	/**
	 * partialIDの入ったファイル
	 */
	protected Path partialIDPath;

	/**
	 * partial波形の入ったファイル
	 */
	protected Path partialPath;

	/**
	 * 観測、理論波形のID情報
	 */
	protected Path waveformIDPath;

	/**
	 * ステーション位置情報のファイル
	 */
	protected Path stationInformationPath;

	/**
	 * どうやって方程式を解くか。 cg svd
	 */
	protected Set<InverseMethodEnum> inverseMethods;
	
	protected WeightingType weightingType;
	
	protected boolean time_source, time_receiver;
	
	protected double gamma;
	
	private CombinationType combinationType;
	
	private Map<PartialType, Integer[]> nUnknowns;
	
	private ObservationEquation[] eq;
	
	private String[] phases;
	
	private List<DataSelectionInformation> selectionInfo;
	
	private boolean modelCovariance;
	
	private double lambdaQ, lambdaMU;
	
	private double correlationScaling;
	
	private double minDistance;
	
	private double maxDistance;
	
	private double minMw;
	
	private double maxMw;
	
	private UnknownParameterWeightType unknownParameterWeightType;
	
	private boolean linaInversion;
	
	private GlobalCMTID[] eventArray;
	private Path eventArrayPath;

	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("stationInformationPath"))
			throw new IllegalArgumentException("There is no information about stationInformationPath.");
		if (!property.containsKey("waveformIDPath"))
			throw new IllegalArgumentException("There is no information about 'waveformIDPath'.");
		if (!property.containsKey("waveformPath"))
			throw new IllegalArgumentException("There is no information about 'waveformPath'.");
		if (!property.containsKey("partialIDPath"))
			throw new IllegalArgumentException("There is no information about 'partialIDPath'.");
		if (!property.containsKey("partialPath"))
			throw new IllegalArgumentException("There is no information about 'partialPath'.");
		if (!property.containsKey("inverseMethods"))
			property.setProperty("inverseMethods", "CG SVD");
		if (!property.containsKey("weighting"))
			property.setProperty("weighting", "RECIPROCAL");
		if (!property.containsKey("time_source"))
			property.setProperty("time_source", "false");
		if (!property.containsKey("time_receiver"))
			property.setProperty("time_receiver", "false");
		if (!property.containsKey("modelCovariance"))
			property.setProperty("modelCovariance", "false");
		if (!property.containsKey("lambdaQ"))
			property.setProperty("lambdaQ", "0.3");
		if (!property.containsKey("lambdaMU"))
			property.setProperty("lambdaMU", "0.03");
		if (!property.containsKey("correlationScaling"))
			property.setProperty("correlationScaling", "1.");
		if (!property.containsKey("minDistance"))
			property.setProperty("minDistance", "0.");
		if (!property.containsKey("maxDistance"))
			property.setProperty("maxDistance", "360.");
		if (!property.containsKey("minMw"))
			property.setProperty("minMw", "0.");
		if (!property.containsKey("maxMw"))
			property.setProperty("maxMw", "10.");
		if (!property.containsKey("linaInversion"))
			property.setProperty("linaInversion", "false");
		if (!property.containsKey("eventPerEvent"))
			property.setProperty("eventPerEvent", "false");
		
		// additional unused info
		property.setProperty("CMTcatalogue", GlobalCMTCatalog.getCatalogID());
//		property.setProperty("STF catalogue", GlobalCMTCatalog.);
	}

	
	private void set() {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));
		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		nSample = Integer.parseInt(property.getProperty("nSample"));
		outPath = new Path[nSample];
		for (int isample = 0; isample < nSample; isample++)
			outPath[isample] = workPath.resolve(Paths.get("lmi" + String.format("_RND%04d", isample)));
		stationInformationPath = getPath("stationInformationPath");
		waveformIDPath = getPath("waveformIDPath");
		waveformPath = getPath("waveformPath");
		partialPath = getPath("partialPath");
		partialIDPath = getPath("partialIDPath");
		unknownParameterListPath = getPath("unknownParameterListPath");
		if (property.containsKey("alpha"))
			alpha = Arrays.stream(property.getProperty("alpha").split("\\s+")).mapToDouble(Double::parseDouble)
					.toArray();
		inverseMethods = Arrays.stream(property.getProperty("inverseMethods").split("\\s+")).map(InverseMethodEnum::of)
				.collect(Collectors.toSet());
		weightingType = WeightingType.valueOf(property.getProperty("weighting"));
		time_source = Boolean.parseBoolean(property.getProperty("time_source"));
		time_receiver = Boolean.parseBoolean(property.getProperty("time_receiver"));
		if (weightingType.equals(WeightingType.TAKEUCHIKOBAYASHI) || weightingType.equals(WeightingType.FINAL)) {
			if (!property.containsKey("gamma"))
				throw new RuntimeException("gamma must be set in oreder to use TAKEUCHIKOBAYASHI or FINAL weighting scheme");
			gamma = Double.parseDouble(property.getProperty("gamma"));
		}
		if (!property.containsKey("phases"))
			phases = null;
		else
			phases = Arrays.stream(property.getProperty("phases").trim().split("\\s+")).toArray(String[]::new);
		//
		if (!property.containsKey("combinationType"))
			combinationType = null;
		else
			combinationType = CombinationType.valueOf(property.getProperty("combinationType"));
		//
		if (!property.containsKey("nUnknowns"))
			nUnknowns = null;
		else if (combinationType == null) {
			throw new RuntimeException("Error: a combinationType "
					+ "must be specified when nUnknowns is specified");
		}
		else {
			nUnknowns = new HashMap<>();
			String[] args = property.getProperty("nUnknowns").trim().split("\\s+");
			if (combinationType.equals(CombinationType.CORRIDOR_TRIANGLE)
					|| combinationType.equals(CombinationType.CORRIDOR_BOXCAR)) {
				if (args.length % 3 != 0)
					throw new RuntimeException("Error: nUnknowns arguments must be in format: PartialType nUM nLM");
				for (int i = 0; i < args.length / 3; i++) {
					int j = i * 3;
					nUnknowns.put(PartialType.valueOf(args[j]), new Integer[] {Integer.parseInt(args[j + 1]), Integer.parseInt(args[j + 2])});
				}
			}
			else if (combinationType.equals(CombinationType.LOWERMANTLE_BOXCAR_3D)) {
				if (args.length % 2 != 0)
					throw new IllegalArgumentException("Error: nUnknowns arguments must be in format: PartialType nNewUnknowns");
				for (int i = 0; i < args.length / 2; i++) {
					int j = i * 2;
					nUnknowns.put(PartialType.valueOf(args[j]), new Integer[] {Integer.parseInt(args[j + 1])});
				}
			}
			else if (combinationType.equals(CombinationType.TRANSITION_ZONE_23)) {
			}
			else if (combinationType.equals(CombinationType.TRANSITION_ZONE_20)) {
			}
			else {
				throw new IllegalArgumentException("Error: unknown combinationType " + combinationType);
			}
		}
		if (property.containsKey("DataSelectionInformationFile")) {
			System.out.println("Using dataSelectionInformationFile " + property.getProperty("DataSelectionInformationFile"));
			try {
				selectionInfo = DataSelectionInformationFile.read(Paths.get(property.getProperty("DataSelectionInformationFile")));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
			selectionInfo = null;
		modelCovariance = Boolean.valueOf(property.getProperty("modelCovariance"));
		lambdaQ = Double.valueOf(property.getProperty("lambdaQ"));
		lambdaMU = Double.valueOf(property.getProperty("lambdaMU"));
		correlationScaling = Double.valueOf(property.getProperty("correlationScaling"));
		minDistance = Double.parseDouble(property.getProperty("minDistance"));
		maxDistance = Double.parseDouble(property.getProperty("maxDistance"));
		if (property.getProperty("unknownParameterWeightType") == null)
			unknownParameterWeightType = null;
		else {
			unknownParameterWeightType = UnknownParameterWeightType.valueOf(property.getProperty("unknownParameterWeightType"));
			System.out.println("--->Weighting unkown parameters using type " + unknownParameterWeightType);
		}
		minMw = Double.parseDouble(property.getProperty("minMw"));
		maxMw = Double.parseDouble(property.getProperty("maxMw"));
		
		linaInversion = Boolean.parseBoolean(property.getProperty("linaInversion"));
		
		eventPerEvent = Boolean.parseBoolean(property.getProperty("eventPerEvent"));
		if (eventPerEvent) {
			eventArrayPath = Paths.get(property.getProperty("eventArrayPath"));
			
		}
	}

	/**
	 * AIC計算に用いるα 独立データ数はn/αと考える
	 */
	protected double[] alpha;

	private Properties property;

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(LetMeInvert_RND.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan LetMeInvert");
			pw.println("##These properties for LetMeInvert");
			pw.println("##Path of a work folder (.)");
			pw.println("#workPath");
			pw.println("##Path of an output folder (workPath/lmiyymmddhhmmss)");
			pw.println("#outPath");
			pw.println("#nSample");
			pw.println("##Path of a waveID file, must be set");
			pw.println("#waveformIDPath waveID.dat");
			pw.println("##Path of a waveform file, must be set");
			pw.println("#waveformPath waveform.dat");
			pw.println("##Path of a partial id file, must be set");
			pw.println("#partialIDPath partialID.dat");
			pw.println("##Path of a partial waveform file must be set");
			pw.println("#partialPath partial.dat");
			pw.println("##Path of a unknown parameter list file, must be set");
			pw.println("#unknownParameterListPath unknowns.inf");
			pw.println("##Path of a station information file, must be set");
			pw.println("#stationInformationPath station.inf");
			pw.println("##double[] alpha it self, if it is set, compute aic for each alpha.");
			pw.println("#alpha");
			pw.println("##inverseMethods[] names of inverse methods (CG SVD)");
			pw.println("#inverseMethods");
			pw.println("##int weighting (RECIPROCAL); LOWERUPPERMANTLE, RECIPROCAL, TAKEUCHIKOBAYASHI, IDENTITY, or FINAL");
			pw.println("#weighting RECIPROCAL");
			pw.println("##double gamma. Must be set only if TAKEUCHIKOBAYASHI weigthing is used");
			pw.println("#gamma 30.");
			pw.println("##boolean time_source (false). Time partial for the source");
			pw.println("time_source false");
			pw.println("##boolean time_receiver (false). Time partial for the receiver");
			pw.println("time_receiver false");
			pw.println("##Use phases (blank = all phases)");
			pw.println("#phases");
			pw.println("##CombinationType to combine 1-D pixels or voxels (null)");
			pw.println("#combinationType");
			pw.println("#nUnknowns PAR2 9 9 PARQ 9 9");
			pw.println("##DataSelectionInformationFile (leave blank if not needed)");
			pw.println("#DataSelectionInformationFile");
			pw.println("##boolean modelCovariance (false)");
			pw.println("#modelCovariance");
			pw.println("##double lambdaQ (0.3)");
			pw.println("#lambdaQ");
			pw.println("##double lambdaMU (0.03)");
			pw.println("#lambdaMU");
			pw.println("##double correlationScaling (1.)");
			pw.println("#correlationScaling");
			pw.println("##If wish to select distance range: min distance (deg) of the data used in the inversion");
			pw.println("#minDistance ");
			pw.println("##If wish to select distance range: max distance (deg) of the data used in the inversion");
			pw.println("#maxDistance ");
			pw.println("#unknownParameterWeightType");
			pw.println("##minimum Mw (0.)");
			pw.println("#minMw");
			pw.println("##maximum Mw (10.)");
			pw.println("#maxMw");
			pw.println("##Set parameters for inversion for Yamaya et al. CMT paper (false)");
			pw.println("#linaInversion");
			pw.println("#eventPerEvent");
			pw.println("#eventArrayPath");
		}
		System.err.println(outPath + " is created.");
	}

	private Path workPath;

	public LetMeInvert_RND(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
		if (!canGO())
			throw new RuntimeException();
		setEquation();
	}

//	public LetMeInvert_RND(Path workPath, Set<Station> stationSet, ObservationEquation equation) throws IOException {
//		eq = equation;
//		this.stationSet = stationSet;
//		workPath.resolve("lmi" + Utilities.getTemporaryString());
//		inverseMethods = new HashSet<>(Arrays.asList(InverseMethodEnum.values()));
//	}

	private Path[] outPath;
	
	private boolean eventPerEvent;

	private void setEquation() throws IOException {
		eq = new ObservationEquation[nSample];
		
		// set partial matrix
		PartialID[] partialIDs = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialPath);
		
		BasicID[][] ids = new BasicID[nSample][];
		for (int isample = 0; isample < nSample; isample++) {
			if (eventPerEvent) {
				String sampleID = "_" + eventArray[isample];
				Path tmpIDPath = Paths.get(waveformIDPath.toString() + sampleID);
				Path tmpPath = Paths.get(waveformPath.toString() + sampleID);
				ids[isample] = BasicIDFile.readBasicIDandDataFile(tmpIDPath, tmpPath);
			}
			else {
				String sampleID = String.format("_RND%04d.dat", isample);
				Path tmpIDPath = Paths.get(waveformIDPath.toString() + sampleID);
				Path tmpPath = Paths.get(waveformPath.toString() + sampleID);
				ids[isample] = BasicIDFile.readBasicIDandDataFile(tmpIDPath, tmpPath);
			}
		}
		
		// set unknown parameter
				System.err.println("setting up unknown parameter set");
				List<UnknownParameter> parameterList = UnknownParameterFile.read(unknownParameterListPath);
		
		Predicate<BasicID> chooser = null;
		
		if (phases != null) {
			chooser = new Predicate<BasicID>() {
				public boolean test(BasicID id) {
	//				GlobalCMTData event = id.getGlobalCMTID().getEvent();
	//				if (event.getCmtLocation().getR() > 5971.)
	//					return false;
	//				if (event.getCmt().getMw() < 6.3)
	//					return false;
					for (String phasename : phases) {
						if (new Phases(id.getPhases()).equals(new Phases(phasename)))
							return true;
					}
					return false;
				}
			};
		}
		else if (linaInversion) {
			System.out.println("Setting chooser for Yamaya et al. CMT paper");
			System.out.println("DEBUG1: " + minDistance + " " + maxDistance + " " + minMw + " " + maxMw);
			Set<GlobalCMTID> wellDefinedEvent = Stream.of(new String[] {"201104170158A","200911141944A","201409241116A","200809031125A"
					,"200707211327A","200808262100A","201009130715A","201106080306A","200608250044A","201509281528A","201205280507A"
					,"200503211223A","201111221848A","200511091133A","201005241618A","200810122055A","200705251747A","201502111857A"
					,"201206020752A","201502021049A","200506021056A","200511171926A","201101010956A","200707120523A","201109021347A"
					,"200711180540A","201302221201A","200609220232A","200907120612A","201211221307A","200707211534A","200611130126A"
					,"201208020938A","201203050746A","200512232147A"})
					.map(GlobalCMTID::new)
					.collect(Collectors.toSet());
			System.out.println("Using " + wellDefinedEvent.size() + " well defined events");
			chooser = id -> {
				if (!wellDefinedEvent.contains(id.getGlobalCMTID()))
					return false;
				double distance = id.getGlobalCMTID().getEvent()
						.getCmtLocation().getEpicentralDistance(id.getStation().getPosition())
						* 180. / Math.PI;
				if (distance < minDistance || distance > maxDistance)
					return false;
				double mw = id.getGlobalCMTID().getEvent()
						.getCmt().getMw();
				if (mw < minMw || mw > maxMw) {
					System.out.println(mw);
					return false;
				}
				
				return true;
			};
		}
		else {
			System.out.println("DEBUG1: " + minDistance + " " + maxDistance + " " + minMw + " " + maxMw);
			chooser = id -> {
				double distance = id.getGlobalCMTID().getEvent()
						.getCmtLocation().getEpicentralDistance(id.getStation().getPosition())
						* 180. / Math.PI;
				if (distance < minDistance || distance > maxDistance)
					return false;
				double mw = id.getGlobalCMTID().getEvent()
						.getCmt().getMw();
				if (mw < minMw || mw > maxMw) {
					System.out.println(mw);
					return false;
				}
				return true;
			};
		}
		
		// set Dvector
		System.err.println("Creating D vector");
		System.err.println("Going with weghting " + weightingType);
		Dvector[] dVector =  new Dvector[nSample];
		boolean atLeastThreeRecordsPerStation = time_receiver || time_source;
		double[][] weighting = new double[nSample][];
		List<UnknownParameter> parameterForStructure = new ArrayList<>();
		
		for (int isample = 0; isample < nSample; isample++) {
			switch (weightingType) {
			case LOWERUPPERMANTLE:
				double[] lowerUpperMantleWeighting = Weighting.LowerUpperMantle1D(partialIDs);
				dVector[isample] = new Dvector(ids[isample], chooser, weightingType, lowerUpperMantleWeighting, atLeastThreeRecordsPerStation);
				break;
			case RECIPROCAL:
	//			System.out.println(selectionInfo.size());
	//			System.out.println(selectionInfo.get(0).getTimewindow());
				dVector[isample] = new Dvector(ids[isample], chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
				break;
			case RECIPROCAL_AZED:
				dVector[isample] = new Dvector(ids[isample], chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
				break;
			case RECIPROCAL_AZED_DPP:
				dVector[isample] = new Dvector(ids[isample], chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
				break;
			case RECIPROCAL_AZED_DPP_V2:
				dVector[isample] = new Dvector(ids[isample], chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
				break;
			case TAKEUCHIKOBAYASHI:
				dVector[isample] = new Dvector(ids[isample], chooser, WeightingType.IDENTITY, atLeastThreeRecordsPerStation, selectionInfo);
	//			System.out.println(dVector.getObs().getLInfNorm() + " " + dVector.getSyn().getLInfNorm());
				parameterForStructure = parameterList.stream()
						.filter(unknonw -> unknonw.getPartialType().equals(PartialType.PAR2))
						.collect(Collectors.toList());
	//			double[] weighting = Weighting.CG(partialIDs, parameterForStructure, dVector, gamma);
				weighting[isample] = Weighting.TakeuchiKobayashi1D(partialIDs, parameterForStructure, dVector[isample], gamma);
				dVector[isample] = new Dvector(ids[isample], chooser, weightingType, weighting[isample], atLeastThreeRecordsPerStation);
				break;
			case FINAL:
				dVector[isample] = new Dvector(ids[isample], chooser, WeightingType.IDENTITY, atLeastThreeRecordsPerStation, selectionInfo);
				parameterForStructure = parameterList.stream()
						.filter(unknonw -> unknonw.getPartialType().equals(PartialType.PAR2))
						.collect(Collectors.toList());
				weighting[isample] = Weighting.TakeuchiKobayashi1D(partialIDs, parameterForStructure, dVector[isample], gamma);
				dVector[isample] = new Dvector(ids[isample], chooser, weightingType, weighting[isample], atLeastThreeRecordsPerStation);
				break;
			case IDENTITY:
				dVector[isample] = new Dvector(ids[isample], chooser, WeightingType.IDENTITY, atLeastThreeRecordsPerStation, selectionInfo);
				break;
			default:
				throw new RuntimeException("Error: Weighting should be LOWERUPPERMANTLE, RECIPROCAL, TAKEUCHIKOBAYASHI, IDENTITY, or FINAL");
			}
		
			if (modelCovariance)
				eq[isample] = new ObservationEquation(partialIDs, parameterList, dVector[isample], time_source, time_receiver, nUnknowns, lambdaMU, lambdaQ, correlationScaling, null);
			else
				eq[isample] = new ObservationEquation(partialIDs, parameterList, dVector[isample], time_source, time_receiver, combinationType, nUnknowns,
						unknownParameterWeightType, null);
		}
		
//		Path AtAPath = workPath.resolve("ata_stable_" + weightingType + ".dat");
//		Path AtdPath = workPath.resolve("atd_stable_" + weightingType + ".dat");
//		eq.outputAtA(AtAPath);
//		eq.outputAtd(AtdPath);
//		System.exit(0);
	}
	
	/**
	 * Output information of observation equation
	 */
	private Future<Void> output(int isample) throws IOException {
		// // ステーションの情報の読み込み
		System.err.print("reading station Information");
		if (stationSet == null)
			stationSet = StationInformationFile.read(stationInformationPath);
		System.err.println(" done");
		Dvector dVector = eq[isample].getDVector();
		Callable<Void> output = () -> {
//			outputDistribution(outPath.resolve("stationEventDistribution.inf"));
			dVector.outOrder(outPath[isample]);
//			dVector.outPhases(outPath);
//			outEachTrace(outPath.resolve("trace"));
			UnknownParameterFile.write(eq[isample].getParameterList(), outPath[isample].resolve("unknownParameterOrder.inf"));
			UnknownParameterFile.write(eq[isample].getOriginalParameterList(), outPath[isample].resolve("originalUnknownParameterOrder.inf"));
//			eq.outputA(outPath.resolve("partial"));
//			eq.outputAtA(outPath.resolve("lmi_AtA.inf"));
			eq[isample].outputUnkownParameterWeigths(outPath[isample].resolve("unknownParameterWeigths.inf"));
			dVector.outWeighting(outPath[isample]);
			return null;
		};
		FutureTask<Void> future = new FutureTask<>(output);

		new Thread(future).start();
		return future;
	}
	
	private int nSample;

	@Override
	public void run() {
		for (int isample = 0; isample < nSample; isample++) {
			try {
				System.err.println("The output folder: " + outPath[isample]);
				Files.createDirectory(outPath[isample]);
				if (property != null)
					writeProperties(outPath[isample].resolve("lmi.properties"));
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Can not create " + outPath[isample]);
			}
	
			long start = System.nanoTime();
	
			// 観測方程式
			Future<Void> future;
			try {
				future = output(isample);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
	
			// 逆問題
			solve(isample);
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			try {
				eq[isample].outputSensitivity(outPath[isample].resolve("sensitivity.inf"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.err.println("Inversion is done in " + Utilities.toTimeString(System.nanoTime() - start));
		}
	}

	/**
	 * outDirectory下にtraceフォルダを作りその下に理論波形と観測波形を書き込む
	 * 
	 * @param outPath
	 *            {@link Path} for output folder
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void outEachTrace(Path outPath, int isample) throws IOException {
		if (Files.exists(outPath))
			throw new FileAlreadyExistsException(outPath.toString());
		Files.createDirectories(outPath);
		Dvector d = eq[isample].getDVector();

		Path eventPath = outPath.resolve("eventVariance.inf");
		Path stationPath = outPath.resolve("stationVariance.inf");
		try (PrintWriter pwEvent = new PrintWriter(Files.newBufferedWriter(eventPath));
				PrintWriter pwStation = new PrintWriter(Files.newBufferedWriter(stationPath))) {
			pwEvent.println("#id latitude longitude radius variance");
			d.getEventVariance().entrySet().forEach(entry -> {
				pwEvent.println(
						entry.getKey() + " " + entry.getKey().getEvent().getCmtLocation() + " " + entry.getValue());
			});
			pwStation.println("#name network latitude longitude variance");
			d.getStationVariance().entrySet().forEach(entry -> {
				pwStation.println(entry.getKey() + " " + entry.getKey().getNetwork() + " "
						+ entry.getKey().getPosition() + " " + entry.getValue());
			});

		}
		for (GlobalCMTID id : d.getUsedGlobalCMTIDset()) {
			Path eventFolder = outPath.resolve(id.toString());
			Files.createDirectories(eventFolder);
			Path obs = eventFolder.resolve("recordOBS.plt");
			Path syn = eventFolder.resolve("recordSYN.plt");
			Path w = eventFolder.resolve("recordW.plt");
			Path wa = eventFolder.resolve("recordWa.plt");
			try (PrintWriter plotO = new PrintWriter(
					Files.newBufferedWriter(obs, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotS = new PrintWriter(
							Files.newBufferedWriter(syn, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotW = new PrintWriter(
							Files.newBufferedWriter(w, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotWa = new PrintWriter(
							Files.newBufferedWriter(wa, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {

				plotW.println("set title\"" + id + "\"");
				plotW.print("p ");
				plotO.println("set title\"" + id + "\"");
				plotO.print("p ");
				plotS.println("set title\"" + id + "\"");
				plotS.print("p ");
				plotWa.println("set title\"" + id + "\"");
				plotWa.print("p ");
			}

		}

		BasicID[] obsIDs = d.getObsIDs();
		BasicID[] synIDs = d.getSynIDs();
		RealVector[] obsVec = d.getObsVec();
		RealVector[] synVec = d.getSynVec();
		RealVector[] delVec = d.getdVec();
		// each trace variance
		Path eachVariancePath = outPath.resolve("eachVariance.txt");
		try (PrintWriter pw1 = new PrintWriter(Files.newBufferedWriter(eachVariancePath))) {
			pw1.println("#i station network EventID variance correlation");
			for (int i = 0; i < d.getNTimeWindow(); i++) {
				double variance = delVec[i].dotProduct(delVec[i]) / obsVec[i].dotProduct(obsVec[i]);
				double correlation = obsVec[i].dotProduct(synVec[i]) / obsVec[i].getNorm() / synVec[i].getNorm();
				pw1.println(i + " " + obsIDs[i].getStation() + " " + obsIDs[i].getStation().getNetwork() + " "
						+ obsIDs[i].getGlobalCMTID() + " " + variance + " " + correlation);
			}
		}
		for (int i = 0; i < d.getNTimeWindow(); i++) {
			String name = obsIDs[i].getStation() + "." + obsIDs[i].getGlobalCMTID() + "." + obsIDs[i].getSacComponent()
					+ "." + i + ".txt";

			HorizontalPosition eventLoc = obsIDs[i].getGlobalCMTID().getEvent().getCmtLocation();
			HorizontalPosition stationPos = obsIDs[i].getStation().getPosition();
			double gcarc = Precision.round(Math.toDegrees(eventLoc.getEpicentralDistance(stationPos)), 2);
			double azimuth = Precision.round(Math.toDegrees(eventLoc.getAzimuth(stationPos)), 2);
			Path eventFolder = outPath.resolve(obsIDs[i].getGlobalCMTID().toString());
			// eventFolder.mkdir();
			Path plotPath = eventFolder.resolve("recordOBS.plt");
			Path plotPath2 = eventFolder.resolve("recordSYN.plt");
			Path plotPath3 = eventFolder.resolve("recordW.plt");
			Path plotPath4 = eventFolder.resolve("recordWa.plt");
			Path tracePath = eventFolder.resolve(name);
			try (PrintWriter pwTrace = new PrintWriter(Files.newBufferedWriter(tracePath));
					PrintWriter plotO = new PrintWriter(
							Files.newBufferedWriter(plotPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotS = new PrintWriter(
							Files.newBufferedWriter(plotPath2, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotW = new PrintWriter(
							Files.newBufferedWriter(plotPath3, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotWa = new PrintWriter(
							Files.newBufferedWriter(plotPath4, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {

				if (i < (d.getNTimeWindow() - 1)) {
					plotO.println("\"" + name + "\" u 1:($3+" + gcarc + ") ti\"" + obsIDs[i].getStation() + "\", \\");
					plotS.println("\"" + name + "\" u 2:($4+" + gcarc + ") ti\"" + obsIDs[i].getStation() + "\", \\");
					plotW.println("\"" + name + "\" u 2:($3+" + gcarc + ") lc rgb \"red\" noti ,  \"" + name
							+ "\" u 2:($4+" + gcarc + ") lc rgb \"blue\" ti\"" + obsIDs[i].getStation() + "\", \\");
					plotWa.println("\"" + name + "\" u 2:($3+" + azimuth + ") lc rgb \"red\" noti ,  \"" + name
							+ "\" u 2:($4+" + azimuth + ") lc rgb \"blue\" ti\"" + obsIDs[i].getStation() + "\", \\");
				} else {

					plotO.println("\"" + name + "\" u 1:($3+" + gcarc + ") ti\"" + obsIDs[i].getStation() + "\"");
					plotS.println("\"" + name + "\" u 2:($4+" + gcarc + ") ti\"" + obsIDs[i].getStation() + "\"");
					plotW.println("\"" + name + "\" u 2:($3+" + gcarc + ") lc rgb \"red\" noti ,  \"" + name
							+ "\" u 2:($4+" + gcarc + ") lc rgb \"blue\" ti\"" + obsIDs[i].getStation() + "\"");
					plotWa.println("\"" + name + "\" u 2:($3+" + azimuth + ") lc rgb \"red\" noti ,  \"" + name
							+ "\" u 2:($4+" + azimuth + ") lc rgb \"blue\" ti\"" + obsIDs[i].getStation() + "\"");
				}
//				double maxObs = obsVec[i].getLInfNorm();
				double obsStart = obsIDs[i].getStartTime();
				double synStart = synIDs[i].getStartTime();
				double samplingHz = obsIDs[i].getSamplingHz();
				pwTrace.println("#obstime syntime obs syn");
				for (int j = 0; j < obsIDs[i].getNpts(); j++)
					pwTrace.println((obsStart + j / samplingHz) + " " + (synStart + j / samplingHz) + " "
							+ obsVec[i].getEntry(j) + " " + synVec[i].getEntry(j));
			}
		}
	}

	/**
	 * vectorsをidの順の波形だとしてファイルに書き込む
	 * 
	 * @param outPath
	 *            {@link File} for output
	 * @param vectors
	 *            {@link RealVector}s for output
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void outEachTrace(Path outPath, RealVector[] vectors, int isample) throws IOException {
		// if (outDirectory.exists())
		// return;
		int nTimeWindow = eq[isample].getDVector().getNTimeWindow();
		if (vectors.length != nTimeWindow)
			return;
		for (int i = 0; i < nTimeWindow; i++)
			if (vectors[i].getDimension() != eq[isample].getDVector().getSynVec()[i].getDimension())
				return;
		Files.createDirectories(outPath);
		for (GlobalCMTID id : eq[isample].getDVector().getUsedGlobalCMTIDset()) {
			Path eventPath = outPath.resolve(id.toString());
			Files.createDirectories(eventPath);
			Path record = eventPath.resolve("record.plt");
			Path recorda = eventPath.resolve("recorda.plt");
			try (PrintWriter pw = new PrintWriter(
					Files.newBufferedWriter(record, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter pwa = new PrintWriter(
							Files.newBufferedWriter(recorda, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
				pw.println("set title\"" + id + "\"");
				pw.print("p ");
				pwa.println("set title\"" + id + "\"");
				pwa.print("p ");
			}
		}
		BasicID[] obsIDs = eq[isample].getDVector().getObsIDs();
		BasicID[] synIDs = eq[isample].getDVector().getSynIDs();
		for (int i = 0; i < nTimeWindow; i++) {
			Path out = outPath.resolve(obsIDs[i].getGlobalCMTID() + "/" + obsIDs[i].getStation() + "."
					+ obsIDs[i].getGlobalCMTID() + "." + obsIDs[i].getSacComponent() + "." + i + ".txt"); // TODO
			Path plotFile = outPath.resolve(obsIDs[i].getGlobalCMTID() + "/record.plt");
			Path plotFilea = outPath.resolve(obsIDs[i].getGlobalCMTID() + "/recorda.plt");
			HorizontalPosition eventLoc = obsIDs[i].getGlobalCMTID().getEvent().getCmtLocation();
			HorizontalPosition stationPos = obsIDs[i].getStation().getPosition();
			double gcarc = Precision.round(Math.toDegrees(eventLoc.getEpicentralDistance(stationPos)), 2);
			double azimuth = Precision.round(Math.toDegrees(eventLoc.getAzimuth(stationPos)), 2);
			try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out));
					PrintWriter plotW = new PrintWriter(
							Files.newBufferedWriter(plotFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotWa = new PrintWriter(
							Files.newBufferedWriter(plotFilea, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {

				plotW.println("\"" + out.getFileName() + "\" u 2:($3+" + gcarc + ") ti\"" + obsIDs[i].getStation()
						+ "\", \\");
				plotWa.println("\"" + out.getFileName() + "\" u 2:($3+" + azimuth + ") ti\"" + obsIDs[i].getStation()
						+ "\", \\");
				pw.println("#syntime synthetic+");
				for (int j = 0; j < vectors[i].getDimension(); j++) {
					double step = j * obsIDs[i].getSamplingHz();
					pw.println((synIDs[i].getStartTime() + step) + " " + vectors[i].getEntry(j));
				}
			}

		}
	}

	private void solve(int isample) {
		inverseMethods.forEach(method -> {
			try {
				if (method == InverseMethodEnum.LEAST_SQUARES_METHOD)
					return; // TODO
				if (modelCovariance)
					solve(outPath[isample].resolve(method.simple()), method.getMethod(eq[isample].getCmAtA_1(), eq[isample].getCmAtD()), isample);
				else
					solve(outPath[isample].resolve(method.simple()), method.getMethod(eq[isample].getAtA(), eq[isample].getAtD()), isample);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void solve(Path outPath, InverseProblem inverseProblem, int isample) throws IOException {
		// invOutDir.mkdir();
		inverseProblem.compute();
		inverseProblem.outputAns(outPath);
		outVariance(outPath, inverseProblem, isample);
		outVariancePerEvents(outPath, inverseProblem, isample);

		// 基底ベクトルの書き出し SVD: vt, CG: cg ベクトル
		RealMatrix p = inverseProblem.getBaseVectors();
		for (int j = 0; j < eq[isample].getMlength(); j++)
			writeDat(outPath.resolve("p" + j + ".txt"), p.getColumn(j));
	}

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		LetMeInvert_RND lmi = new LetMeInvert_RND(Property.parse(args));
		System.err.println(LetMeInvert_RND.class.getName() + " is running.");
		long startT = System.nanoTime();
		lmi.run();
		System.err.println(
				LetMeInvert_RND.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - startT));
	}

	/**
	 * outDirectory下にvarianceを書き込む
	 * 
	 * @param outPath
	 */
	private void outVariance(Path outPath, InverseProblem inverse, int isample) throws IOException {

		Path out = outPath.resolve("variance.txt");
		if (Files.exists(out))
			throw new FileAlreadyExistsException(out.toString());
		double[] variance = new double[eq[isample].getMlength() + 1];
		variance[0] = eq[isample].getDVector().getVariance();
		for (int i = 0; i < eq[isample].getMlength(); i++)
			variance[i + 1] = eq[isample].varianceOf(inverse.getANS().getColumnVector(i));
		writeDat(out, variance);
		if (alpha == null)
			return;
		for (int i = 0; i < alpha.length; i++) {
			out = outPath.resolve("aic" + i + ".txt");
			double[] aic = computeAIC(variance, alpha[i], isample);
			writeDat(out, aic);
		}
		writeDat(outPath.resolve("aic.inf"), alpha);
	}
	
	private void outVariancePerEvents(Path outPath, InverseProblem inverse, int isample) throws IOException {
		Set<NDK> gcmtCat = GlobalCMTCatalog.readJar("globalcmt.catalog");
		
		Set<GlobalCMTID> eventSet = eq[isample].getDVector().getUsedGlobalCMTIDset();
		Path out = outPath.resolve("eventVariance.txt");
		int n = 31 > eq[isample].getMlength() ? eq[isample].getMlength() : 31;
		Map<GlobalCMTID, double[]> varianceMap = new HashMap<>();
		for (GlobalCMTID id : eventSet) {
			if (Files.exists(out))
				throw new FileAlreadyExistsException(out.toString());
			double[] variance = new double[n];
			variance[0] = eq[isample].getDVector().getEventVariance().get(id);
			RealVector residual = eq[isample].getDVector().getD();
			RealVector obsVec = eq[isample].getDVector().getObs();
			RealVector mask = eq[isample].getDVector().getMask(id);
			
			for (int i = 0; i < residual.getDimension(); i++) {
				residual.setEntry(i, residual.getEntry(i) * mask.getEntry(i));
				obsVec.setEntry(i, obsVec.getEntry(i) * mask.getEntry(i));
			}
			
			for (int i = 0; i < n-1; i++) {
				RealVector adm = eq[isample].getA().operate(inverse.getANS().getColumnVector(i));
				for (int j = 0; j < adm.getDimension(); j++)
					adm.setEntry(j, adm.getEntry(j) * mask.getEntry(j));
				variance[i + 1] = variance[0] + (-2 * adm.dotProduct(residual)
						+ adm.dotProduct(adm)) / obsVec.dotProduct(obsVec);
			}
			varianceMap.put(id, variance);
		}
		
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out, StandardOpenOption.CREATE_NEW))) {
			for (GlobalCMTID id : eventSet) {
				NDK idGCMTndk = gcmtCat.stream().filter(ndk -> ndk.getGlobalCMTID().equals(id))
						.findFirst().get();
				double GCMTMw = idGCMTndk.getCmt().getMw();
				
				String s = id.toString() + " " + String.format("%.2f", GCMTMw);
				double[] variance = varianceMap.get(id);
				for (int i = 0; i < n; i++)
					s += " " + String.format("%.5f", variance[i]);
				pw.println(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 自由度iに対してAICを計算する 独立データは n / alpha 各々のAIC群
	 * 
	 * @param variance
	 *            varianceの列
	 * @param alpha
	 * @return
	 */
	private double[] computeAIC(double[] variance, double alpha, int isample) {
		double[] aic = new double[variance.length];
		int independentN = (int) (eq[isample].getDlength() / alpha);
		for (int i = 0; i < aic.length; i++)
			aic[i] = Utilities.computeAIC(variance[i], independentN, i);
		return aic;
	}

	/**
	 * @param out
	 * @param dat
	 */
	private static void writeDat(Path out, double[] dat) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out, StandardOpenOption.CREATE_NEW))) {
			Arrays.stream(dat).forEach(pw::println);
		}
	}

	private boolean canGO() {
		boolean cango = true;
//		if (Files.exists(outPath)) {
//			new FileAlreadyExistsException(outPath.toString()).printStackTrace();
//			cango = false;
//		}
		if (!Files.exists(partialIDPath)) {
			new NoSuchFileException(partialIDPath.toString()).printStackTrace();
			cango = false;
		}
		if (!Files.exists(partialPath)) {
			cango = false;
			new NoSuchFileException(partialPath.toString()).printStackTrace();
		}
		if (!Files.exists(stationInformationPath)) {
			new NoSuchFileException(stationInformationPath.toString()).printStackTrace();
			cango = false;
		}
		if (!Files.exists(unknownParameterListPath)) {
			new NoSuchFileException(unknownParameterListPath.toString()).printStackTrace();
			cango = false;
		}
//		if (!Files.exists(waveformPath)) {
//			new NoSuchFileException(waveformPath.toString()).printStackTrace();
//			cango = false;
//		}
//		if (!Files.exists(waveformIDPath)) {
//			new NoSuchFileException(waveformIDPath.toString()).printStackTrace();
//			cango = false;
//		}
		if (!Files.exists(workPath)) {
			new NoSuchFileException(workPath.toString()).printStackTrace();
			cango = false;
		}

		return cango;
	}

	/**
	 * station と 震源の位置関係の出力
	 * 
	 * @param outPath
	 *            {@link File} for output
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void outputDistribution(Path outPath, int isample) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			BasicID[] obsIDs = eq[isample].getDVector().getObsIDs();
			pw.println("#station(lat lon) event(lat lon r) EpicentralDistance Azimuth ");
			Arrays.stream(obsIDs).forEach(id -> {
				GlobalCMTData event = id.getGlobalCMTID().getEvent();
				Station station = id.getStation();
				double epicentralDistance = Math
						.toDegrees(station.getPosition().getEpicentralDistance(event.getCmtLocation()));
				double azimuth = Math.toDegrees(station.getPosition().getAzimuth(event.getCmtLocation()));
				pw.println(
						station + " " + station.getPosition() + " " + id.getGlobalCMTID() + " " + event.getCmtLocation()
								+ " " + Precision.round(epicentralDistance, 2) + " " + Precision.round(azimuth, 2));
			});

		}
	}

	private Set<Station> stationSet;

	@Override
	public Path getWorkPath() {
		return workPath;
	}

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

}
