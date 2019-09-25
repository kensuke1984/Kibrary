package io.github.kensuke1984.kibrary.inversion;

import java.io.BufferedReader;
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
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

















import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Precision;







import org.jfree.util.ArrayUtilities;


import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionType;
import io.github.kensuke1984.kibrary.datacorrection.TakeuchiStaticCorrection;
import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.selection.DataSelectionInformation;
import io.github.kensuke1984.kibrary.selection.DataSelectionInformationFile;
import io.github.kensuke1984.kibrary.util.EventCluster;
import io.github.kensuke1984.kibrary.util.FrequencyRange;
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
import io.github.kensuke1984.kibrary.waveformdata.AtAEntry;
import io.github.kensuke1984.kibrary.waveformdata.AtAFile;
import io.github.kensuke1984.kibrary.waveformdata.AtdEntry;
import io.github.kensuke1984.kibrary.waveformdata.AtdFile;
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
public class LetMeInvert implements Operation {
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
	
	private ObservationEquation eq;
	
	private Path verticalMapping;
	
	private String[] phases;
	
	private List<DataSelectionInformation> selectionInfo;
	
	private boolean modelCovariance;
	
	private double cm0, cmH, cmV;
	
	private double lambdaQ, lambdaMU, gammaQ, gammaMU, lambda00, gamma00, lambdaVp, gammaVp;
	
	private double correlationScaling;
	
	private double minDistance;
	
	private double maxDistance;
	
	private double minMw;
	
	private double maxMw;
	
	private UnknownParameterWeightType unknownParameterWeightType;
	
	private boolean linaInversion;
	
	private boolean jackknife;
	
	private int nRealisation;
	
	private boolean checkerboard;
	
	private Path checkerboardPerturbationPath;

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
		if (!property.containsKey("gammaQ"))
			property.setProperty("gammaQ", "0.3");
		if (!property.containsKey("gammaMU"))
			property.setProperty("gammaMU", "0.03");
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
		if (!property.containsKey("jackknife"))
			property.setProperty("jackknife", "false");
		if (!property.containsKey("conditioner"))
			property.setProperty("conditioner", "false");
		if (!property.containsKey("lowMemoryCost"))
			property.setProperty("lowMemoryCost", "false");
		if (!property.containsKey("nStepsForLowMemoryMode"))
			property.setProperty("nStepsForLowMemoryMode", "10");
		if (!property.containsKey("usePrecomputedAtA"))
			property.setProperty("usePrecomputedAtA", "false");
		if (!property.containsKey("checkerboard"))
			property.setProperty("checkerboard", "false");
		if (!property.containsKey("trimWindow"))
			property.setProperty("trimWindow", "false");
		if (!property.containsKey("regularizationMuQ"))
			property.setProperty("regularizationMuQ", "false");
		
		// additional unused info
		property.setProperty("CMTcatalogue", GlobalCMTCatalog.getCatalogID());
//		property.setProperty("STF catalogue", GlobalCMTCatalog.);
	}

	private void set() {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));
		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		if (property.containsKey("outPath"))
			outPath = getPath("outPath");
		else
			outPath = workPath.resolve(Paths.get("lmi" + Utilities.getTemporaryString()));
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
		inverseMethods.stream().forEach(method -> System.out.println(method));
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
		
		if (modelCovariance) {
			if (!property.containsKey("cm0"))
				throw new RuntimeException("cm0 must be set when modelCovariance=true");
			if (!property.containsKey("cmH"))
				throw new RuntimeException("cmH must be set when modelCovariance=true");
			if (!property.containsKey("cmV"))
				throw new RuntimeException("cmV must be set when modelCovariance=true");
			
			cm0 = Double.parseDouble(property.getProperty("cm0"));
			cmH = Double.parseDouble(property.getProperty("cmH"));
			cmV = Double.parseDouble(property.getProperty("cmV"));
		}
		
		regularizationMuQ = Boolean.parseBoolean(property.getProperty("regularizationMuQ"));
		lambdaQ = Double.valueOf(property.getProperty("lambdaQ"));
		lambdaMU = Double.valueOf(property.getProperty("lambdaMU"));
		lambda00 = Double.valueOf(property.getProperty("lambda00"));
		lambdaVp = Double.valueOf(property.getProperty("lambdaVp"));
		gammaQ = Double.valueOf(property.getProperty("gammaQ"));
		gammaMU = Double.valueOf(property.getProperty("gammaMU"));
		gamma00 = Double.valueOf(property.getProperty("gamma00"));
		gammaVp = Double.valueOf(property.getProperty("gammaVp"));
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
		
		verticalMapping = property.containsKey("verticalMapping") ? Paths.get(property.getProperty("verticalMapping")) : null;
		
		jackknife = Boolean.parseBoolean(property.getProperty("jackknife"));
		if (jackknife)
			nRealisation = Integer.parseInt(property.getProperty("nRealisation"));
		
		conditioner = Boolean.parseBoolean(property.getProperty("conditioner"));
		
		lowMemoryCost = Boolean.parseBoolean(property.getProperty("lowMemoryCost"));
		
		nStepsForLowMemoryMode = Integer.parseInt(property.getProperty("nStepsForLowMemoryMode"));
		
		usePrecomputedAtA = Boolean.parseBoolean(property.getProperty("usePrecomputedAtA"));
		if (usePrecomputedAtA) {
			precomputedAtdPath = Stream.of(property.getProperty("precomputedAtdPath").split("\\s+")).map(p -> Paths.get(p.trim())).collect(Collectors.toList()).toArray(new Path[0]);
			precomputedAtAPath = Stream.of(property.getProperty("precomputedAtAPath").split("\\s+")).map(p -> Paths.get(p.trim())).collect(Collectors.toList()).toArray(new Path[0]);
		}
		
		checkerboard = Boolean.parseBoolean(property.getProperty("checkerboard"));
		if (checkerboard)
			checkerboardPerturbationPath = Paths.get(property.getProperty("checkerboardPerturbationPath"));
		
		trimWindow = Boolean.parseBoolean(property.getProperty("trimWindow"));
		if (trimWindow) {
			trimPoint = Double.parseDouble(property.getProperty("trimPoint"));
			keepBefore = Boolean.parseBoolean(property.getProperty("keepBefore"));
		}
		
		if (property.containsKey("eventClusterPath")) {
			eventClusterPath = Paths.get(property.getProperty("eventClusterPath"));
			clusterIndex = Integer.parseInt(property.getProperty("clusterIndex"));
			azimuthIndex = Integer.parseInt(property.getProperty("azimuthIndex"));
			System.out.println("Using cluster file with clusterIndex=" + clusterIndex + " and azimuthIndex=" + azimuthIndex);
		}
	}

	/**
	 * AIC計算に用いるα 独立データ数はn/αと考える
	 */
	protected double[] alpha;

	private Properties property;
	
	private boolean regularizationMuQ;

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(LetMeInvert.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan LetMeInvert");
			pw.println("##These properties for LetMeInvert");
			pw.println("##Path of a work folder (.)");
			pw.println("#workPath");
			pw.println("##Path of an output folder (workPath/lmiyymmddhhmmss)");
			pw.println("#outPath");
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
			pw.println("#verticalMapping");
			pw.println("##CombinationType to combine 1-D pixels or voxels (null)");
			pw.println("#combinationType");
			pw.println("#nUnknowns PAR2 9 9 PARQ 9 9");
			pw.println("##DataSelectionInformationFile (leave blank if not needed)");
			pw.println("#DataSelectionInformationFile");
			pw.println("##boolean modelCovariance (false)");
			pw.println("#modelCovariance");
			pw.println("##double cm0");
			pw.println("#cm0");
			pw.println("##double cmH");
			pw.println("#cmH");
			pw.println("##double cmV");
			pw.println("#cmV");
			pw.println("##boolean regularizationMuQ (false)");
			pw.println("#regularizationMuQ");
			pw.println("##double lambdaQ (0.3)");
			pw.println("#lambdaQ");
			pw.println("##double lambdaMU (0.03)");
			pw.println("#lambdaMU");
			pw.println("##double gammaQ (0.3)");
			pw.println("#gammaQ");
			pw.println("##double gammaMU (0.03)");
			pw.println("#gammaMU");
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
			pw.println("##Perform a jacknife test (false)");
			pw.println("#jackknife");
			pw.println("##Number of jacknife inversions");
			pw.println("#nRealisation 300");
			pw.println("##conditioner for preconditioned CG (false)");
			pw.println("#conditioner");
			pw.println("##build AtA iteratively for low memory cost (false)");
			pw.println("#lowMemoryCost");
			pw.println("#nStepsForLowMemoryMode");
			pw.println("#usePrecomputedAtA");
			pw.println("#precomputedAtAPath");
			pw.println("#precomputedAtdPath");
			pw.println("##Perform checkerboard test (false)");
			pw.println("#checkerboard");
			pw.println("#checkerboardPerturbationPath");
			pw.println("##Trim timewindows (false)");
			pw.println("#trimWindow");
			pw.println("#trimPoint");
			pw.println("#keepBefore");
		}
		System.err.println(outPath + " is created.");
	}

	private Path workPath;

	public LetMeInvert(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
		if (!canGO())
			throw new RuntimeException();
		setEquation();
	}

	public LetMeInvert(Path workPath, Set<Station> stationSet, ObservationEquation equation) throws IOException {
		eq = equation;
		this.stationSet = stationSet;
		workPath.resolve("lmi" + Utilities.getTemporaryString());
		inverseMethods = new HashSet<>(Arrays.asList(InverseMethodEnum.values()));
	}

	private Path outPath;
	
	private boolean conditioner;

	private boolean lowMemoryCost;
	
	private int nStepsForLowMemoryMode;
	
	private boolean usePrecomputedAtA;
	
	private Path[] precomputedAtAPath;
	
	private Path[] precomputedAtdPath;
	
	private boolean trimWindow;
	
	private double trimPoint;
	
	private boolean keepBefore;
	
	private boolean assumeRratio;
	
	private List<EventCluster> clusters;
	
	private Path eventClusterPath;
	
	private int azimuthIndex;
	
	private int clusterIndex;
	
	private void setEquation() throws IOException {
		BasicID[] ids = BasicIDFile.readBasicIDandDataFile(waveformIDPath, waveformPath);
		
		if (eventClusterPath != null)
			clusters = EventCluster.readClusterFile(eventClusterPath);
		
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
//			System.out.println("Setting chooser for Yamaya et al. CMT paper");
			System.out.println("Setting chooser for well defined events");
			System.out.println("DEBUG1: " + minDistance + " " + maxDistance + " " + minMw + " " + maxMw);
			
//			Set<GlobalCMTID> wellDefinedEvent = Stream.of(new String[] {"201104170158A","200911141944A","201409241116A","200809031125A"
//					,"200707211327A","200808262100A","201009130715A","201106080306A","200608250044A","201509281528A","201205280507A"
//					,"200503211223A","201111221848A","200511091133A","201005241618A","200810122055A","200705251747A","201502111857A"
//					,"201206020752A","201502021049A","200506021056A","200511171926A","201101010956A","200707120523A","201109021347A"
//					,"200711180540A","201302221201A","200609220232A","200907120612A","201211221307A","200707211534A","200611130126A"
//					,"201208020938A","201203050746A","200512232147A"})
//					.map(GlobalCMTID::new)
//					.collect(Collectors.toSet()); // LCMT
//			Set<GlobalCMTID> wellDefinedEvent = Stream.of(new String[] {"201104170158A","200911141944A","200506021056A","201409241116A"
//					,"200511171926A","200809031125A","200711180540A","201302221201A","201009130715A","201509281528A"
//					,"201205280507A","200707211534A","201005241618A","200705251747A","201502111857A","201203050746A"})
//					.map(GlobalCMTID::new)
//					.collect(Collectors.toSet()); // MTZ Carib
			
//			Set<GlobalCMTID> wellDefinedEvent = Stream.of(new String[] {"031704A","062003D","200503211243A","200506021056A","200511171926A","200608250044A"
//					,"200609220232A","200611130126A","200705251747A","200707120523A","200707211327A","200707211534A","200711180540A","200808262100A","200809031125A"
//					,"200810122055A","200907120612A","200909050358A","200909301903A","200911141944A","201001280804A","201005241618A","201009130715A","201101010956A"
//					,"201104170158A","201106080306A","201109021347A","201111221848A","201203050746A","201205280507A","201206020752A","201208020938A","201302221201A"
//					,"201409241116A","201502021049A","201502111857A","201509281528A","201511260545A"})
//					.map(GlobalCMTID::new)
//					.collect(Collectors.toSet()); // D" Carib P and S joint inversion
			
			Set<GlobalCMTID> wellDefinedEvent = Stream.of(new String[] {"031704A","200503211243A","200506021056A","200511171926A","200608250044A"
					,"200609220232A","200611130126A","200705251747A","200707120523A","200707211327A","200707211534A","200711180540A","200808262100A","200809031125A"
					,"200909050358A","200909301903A","200911141944A","201001280804A","201005241618A","201009130715A","201101010956A"
					,"201104170158A","201106080306A","201111221848A","201203050746A","201205280507A","201206020752A","201208020938A","201302221201A"
					,"201409241116A","201502021049A","201502111857A","201509281528A","201511260545A"})
					.map(GlobalCMTID::new)
					.collect(Collectors.toSet()); // D" Carib P and S joint inversion
			
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
		else if (eventClusterPath != null) {
			List<EventCluster> thisCluster = clusters.stream().filter(c -> c.getIndex() == clusterIndex).collect(Collectors.toList());
			
			if (azimuthIndex > thisCluster.get(0).getAzimuthSlices().size()) {
				System.out.println("No azimuth slice " + azimuthIndex + " for cluster " + clusterIndex);
				System.exit(0);
			}
			
			double[] azimuthRange = thisCluster.get(0).getAzimuthBound(azimuthIndex);
			
			Set<GlobalCMTID> thisClusterIDs = thisCluster.stream().map(c -> c.getID()).collect(Collectors.toSet());
			HorizontalPosition centerPosition = thisCluster.get(0).getCenterPosition();
			
			System.out.println(azimuthRange[0] + " " + azimuthRange[1]);
			
			chooser = id -> {
				double azimuth = centerPosition.getAzimuth(id.getStation().getPosition())
						* 180. / Math.PI;
				if (!(thisClusterIDs.contains(id.getGlobalCMTID()) && azimuth >= azimuthRange[0] && azimuth <= azimuthRange[1]))
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
		
		// Choose only one event
//		chooser = id -> {
//			double distance = id.getGlobalCMTID().getEvent()
//					.getCmtLocation().getEpicentralDistance(id.getStation().getPosition())
//					* 180. / Math.PI;
//			if (!id.getGlobalCMTID().equals(new GlobalCMTID("201206020752A"))) // 201005241618A 201205280507A 200907120612A 200909301903A
//				return false;
//			if (distance < minDistance || distance > maxDistance)
//				return false;
//			return true;
//		};
			
		
		// set Dvector
		System.err.println("Creating D vector");
		System.err.println("Going with weghting " + weightingType);
		Dvector dVector =  null;
		boolean atLeastThreeRecordsPerStation = time_receiver || time_source;
		double[] weighting = null;
		List<UnknownParameter> parameterForStructure = new ArrayList<>();
		switch (weightingType) {
//		case LOWERUPPERMANTLE:
//			double[] lowerUpperMantleWeighting = Weighting.LowerUpperMantle1D(partialIDs);
//			dVector = new Dvector(ids, chooser, weightingType, lowerUpperMantleWeighting, atLeastThreeRecordsPerStation);
//			break;
		case RECIPROCAL:
		case RECIPROCAL_PcP:
		case RECIPROCAL_COS:
		case RECIPROCAL_CC:
//			System.out.println(selectionInfo.size());
//			System.out.println(selectionInfo.get(0).getTimewindow());
			dVector = new Dvector(ids, chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
			break;
		case RECIPROCAL_AZED:
			dVector = new Dvector(ids, chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
			break;
		case RECIPROCAL_AZED_DPP:
			dVector = new Dvector(ids, chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
			break;
		case RECIPROCAL_AZED_DPP_V2:
			dVector = new Dvector(ids, chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
			break;
//		case TAKEUCHIKOBAYASHI:
//			dVector = new Dvector(ids, chooser, WeightingType.IDENTITY, atLeastThreeRecordsPerStation, selectionInfo);
////			System.out.println(dVector.getObs().getLInfNorm() + " " + dVector.getSyn().getLInfNorm());
//			parameterForStructure = parameterList.stream()
//					.filter(unknonw -> unknonw.getPartialType().equals(PartialType.PAR2))
//					.collect(Collectors.toList());
////			double[] weighting = Weighting.CG(partialIDs, parameterForStructure, dVector, gamma);
//			weighting = Weighting.TakeuchiKobayashi1D(partialIDs, parameterForStructure, dVector, gamma);
//			dVector = new Dvector(ids, chooser, weightingType, weighting, atLeastThreeRecordsPerStation);
//			break;
//		case FINAL:
//			dVector = new Dvector(ids, chooser, WeightingType.IDENTITY, atLeastThreeRecordsPerStation, selectionInfo);
//			parameterForStructure = parameterList.stream()
//					.filter(unknonw -> unknonw.getPartialType().equals(PartialType.PAR2))
//					.collect(Collectors.toList());
//			weighting = Weighting.TakeuchiKobayashi1D(partialIDs, parameterForStructure, dVector, gamma);
//			dVector = new Dvector(ids, chooser, weightingType, weighting, atLeastThreeRecordsPerStation);
//			break;
		case IDENTITY:
			dVector = new Dvector(ids, chooser, WeightingType.IDENTITY, atLeastThreeRecordsPerStation, selectionInfo);
			break;
		default:
			throw new RuntimeException("Error: Weighting should be LOWERUPPERMANTLE, RECIPROCAL, TAKEUCHIKOBAYASHI, IDENTITY, or FINAL");
		}
		
		if (trimWindow)
			dVector.trimWindow(trimPoint, keepBefore);
		
		if (usePrecomputedAtA) {
			System.out.println("Using " + precomputedAtAPath.length + " precomputed matrices");
			RealVector atd = null;
			RealMatrix ata = null;
			double[] ataNorms = new double[precomputedAtAPath.length];
			if (precomputedAtAPath.length != 2) {
				AtdEntry[][][][][] atdEntries = AtdFile.readArray(precomputedAtdPath[0]);
				double wz = 1.;
	//			System.out.println("Multiplying by " + wz + " " + precomputedAtAPath[0]);
				atd = AtdFile.getAtdVector(atdEntries, 0, 0, 0, 0).mapMultiply(wz);
				ata = AtAFile.getAtARealMatrixParallel(precomputedAtAPath[0], 0, 0, 0).scalarMultiply(wz);
				ataNorms[0] = ata.getTrace() / ata.getColumnDimension();
				for (int k = 1; k < precomputedAtAPath.length; k++) {
					atdEntries = AtdFile.readArray(precomputedAtdPath[k]);
					atd = atd.add(AtdFile.getAtdVector(atdEntries, 0, 0, 0, 0));
					ata = ata.add(AtAFile.getAtARealMatrixParallel(precomputedAtAPath[k], 0, 0, 0));
				}
			}
			else {
				// A_Z
				AtdEntry[][][][][] atdEntries = AtdFile.readArray(precomputedAtdPath[0]);
				atd = AtdFile.getAtdVector(atdEntries, 0, 0, 0, 0);
				ata = AtAFile.getAtARealMatrixParallel(precomputedAtAPath[0], 0, 0, 0);
				ataNorms[0] = ata.getTrace() / ata.getColumnDimension();
				System.out.println("Norm of AtA_Z = " + ataNorms[0]);
				
				// A_T
				atdEntries = AtdFile.readArray(precomputedAtdPath[1]);
				RealVector atd_T = atd.add(AtdFile.getAtdVector(atdEntries, 0, 0, 0, 0));
				RealMatrix ata_T = AtAFile.getAtARealMatrixParallel(precomputedAtAPath[1], 0, 0, 0);
				ataNorms[1] = ata_T.getTrace() / ata_T.getColumnDimension();
				System.out.println("Norm of AtA_T = " + ataNorms[1]);
				
				atd = atd.add(atd_T);
				ata = ata.add(ata_T);
			}
			
			eq = new ObservationEquation(ata, atd, parameterList, dVector);
			
			boolean writeTMPata = false;
			if (writeTMPata) {
				String tempString = Utilities.getTemporaryString();
				//write AtA for later use
				Path outputPath = workPath.resolve("ata" + tempString + ".dat");
				FrequencyRange frequencyRange = new FrequencyRange(1./ids[0].getMaxPeriod(), 1./ids[0].getMinPeriod());
				UnknownParameter[] unknownParameters = parameterList.toArray(new UnknownParameter[0]);
				Phases phase = new Phases(ids[0].getPhases());
				AtAFile.write(eq.getAtA(), weightingType, frequencyRange, unknownParameters, phase, outputPath);
				
				//write Atd for later use
				Path outputPathAtd = workPath.resolve("atd" + tempString + ".dat");
				StaticCorrectionType correctionType = StaticCorrectionType.S;
				AtdFile.write(eq.getAtD(), unknownParameters, weightingType, frequencyRange, phase, correctionType, outputPathAtd);
			}
			
			if (checkerboard) {
				System.out.println("Computing checkerboard input from " + checkerboardPerturbationPath);
				eq.setAtdForCheckerboard(readCheckerboardPerturbationVector());
			}
			
			if (modelCovariance) {
				System.out.println("Building covariance matrix");
//				double meanTrace = eq.getAtA().getTrace() / parameterList.size();
//				System.out.println("AtANormalizedTrace = " + meanTrace);
				double[] cm0s = new double[parameterList.size()];
				if (precomputedAtAPath.length == 2) {
					for (int i = 0; i < parameterList.size(); i++) {
						UnknownParameter par = parameterList.get(i);
						if (par.getPartialType().equals(PartialType.LAMBDA2MU))
							cm0s[i] = cm0 / ataNorms[0];
						else if (par.getPartialType().equals(PartialType.MU))
							cm0s[i] = cm0 / ataNorms[1];
						else 
							throw new RuntimeException("Smoothing for types other than MU and LAMBDA2MU not implemented yet");
					}
				}
				else if (precomputedAtAPath.length == 1) {
//					for (int i = 0; i < parameterList.size(); i++)
//						cm0s[i] = cm0 / ataNorms[0];
					for (int i = 0; i < parameterList.size(); i++) {
						UnknownParameter par = parameterList.get(i);
						if (par.getPartialType().equals(PartialType.LAMBDA2MU))
							cm0s[i] = cm0 / 3.253358218624619E-4;
						else if (par.getPartialType().equals(PartialType.MU))
							cm0s[i] = cm0 / 0.01200258499138781;
						else 
							throw new RuntimeException("Smoothing for types other than MU and LAMBDA2MU not implemented yet");
					}
				}
				ModelCovarianceMatrix cm = new ModelCovarianceMatrix(parameterList, cmV, cmH, cm0s, true);
				eq.applyModelCovarianceMatrix(cm);
			}
			
			if (conditioner) {
				applyConditionner();
//				applyConditionnerAll();
//				applyConditionnerBulk();
			}
		}
		else if (!lowMemoryCost) {
			// read all partial IDs
			PartialID[] partialIDs = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialPath);
			
			if (modelCovariance) {
	//			eq = new ObservationEquation(partialIDs, parameterList, dVector, time_source, time_receiver, nUnknowns, lambdaMU, lambdaQ, correlationScaling, verticalMapping);
				if (inverseMethods.contains(InverseMethodEnum.FAST_CONJUGATE_GRADIENT) || inverseMethods.contains(InverseMethodEnum.FAST_CONJUGATE_GRADIENT_DAMPED))
					eq = new ObservationEquation(partialIDs, parameterList, dVector, cm0, cmH, cmV, verticalMapping, false);
				else
					eq = new ObservationEquation(partialIDs, parameterList, dVector, cm0, cmH, cmV, verticalMapping);
			}
			else {
				if (inverseMethods.contains(InverseMethodEnum.FAST_CONJUGATE_GRADIENT) || inverseMethods.contains(InverseMethodEnum.FAST_CONJUGATE_GRADIENT_DAMPED))
					eq = new ObservationEquation(partialIDs, parameterList, dVector, time_source, time_receiver, combinationType, nUnknowns,
							unknownParameterWeightType, verticalMapping, false);
				else
					eq = new ObservationEquation(partialIDs, parameterList, dVector, time_source, time_receiver, combinationType, nUnknowns,
						unknownParameterWeightType, verticalMapping);
			}
			
			if (conditioner)
				applyConditionner();
			
			if (regularizationMuQ) {
				addRegularizationVSQ();
			}
		}
		else {
			Matrix atatmp = new Matrix(parameterList.size(), parameterList.size());
			RealVector atdtmp = new ArrayRealVector(parameterList.size());
			
			int nIDPerStep = ((int) (ids.length / 2 / nStepsForLowMemoryMode)) * 2;
			int nIDLastStep = ids.length - nIDPerStep * nStepsForLowMemoryMode + nIDPerStep;
			
			// read ids headers only
			PartialID[] partialIDsNoData = PartialIDFile.readPartialIDFile(partialIDPath);
			
			int[] cumulativeNPTS = new int[partialIDsNoData.length];
			cumulativeNPTS[0] = 0;
			for (int i = 1; i < cumulativeNPTS.length; i++)
				cumulativeNPTS[i] = cumulativeNPTS[i-1] + partialIDsNoData[i-1].getNpts();
			
			String tempString = Utilities.getTemporaryString();
			for (int istep = 0; istep < nStepsForLowMemoryMode; istep++) {
				System.out.println("Step " + istep);
				int startIndex = istep * nIDPerStep;
				int n = istep == nStepsForLowMemoryMode - 1 ? nIDLastStep : nIDPerStep;
				
				BasicID[] idstmp = Arrays.copyOfRange(ids, startIndex, startIndex + n);
				int[] partialIndexes = 
					IntStream.range(0, partialIDsNoData.length).parallel()
					.filter(i -> {
						boolean res = false;
						for (BasicID idtmp : idstmp)
							if (isPair(idtmp, partialIDsNoData[i])) {
								res = true;
								break;
							}
						return res;
					}).sorted().toArray();
				
				System.out.println(partialIndexes.length + " " + idstmp.length * parameterList.size() + " " + parameterList.size() + " " + idstmp.length);
				
				PartialID[] partialIDs = new PartialID[partialIndexes.length];
				for (int k = 0; k < partialIDs.length; k++)
					partialIDs[k] = partialIDsNoData[partialIndexes[k]];
				
				partialIDs = PartialIDFile.readPartialIDandDataFile(partialIDs, partialPath, partialIndexes, cumulativeNPTS);
				
				Dvector dVectortmp = new Dvector(idstmp, chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
				
				if (trimWindow) {
					dVectortmp.trimWindow(trimPoint, keepBefore);
				
					// trim partials
					for (int k = 0; k < partialIDs.length; k++) {
						int nStart = 0;
						int nEnd = 0;
						if (keepBefore) {
							nStart = 0;
							nEnd = (int) (trimPoint / partialIDs[k].getSamplingHz()) + 1;
							nEnd = nEnd > partialIDs[k].getNpts() ? partialIDs[k].getNpts() : nEnd;
						}
						else {
							nStart = (int) (trimPoint / partialIDs[k].getSamplingHz());
							nEnd = partialIDs[k].getNpts();
						}
						double[] trimmedData = Arrays.copyOfRange(partialIDs[k].getData(), nStart, nEnd);
						partialIDs[k].setData(trimmedData);
					}
				}
				
				if (modelCovariance) {
					eq = new ObservationEquation(partialIDs, parameterList, dVectortmp, atatmp, atdtmp);
				}
				else {
					eq = new ObservationEquation(partialIDs, parameterList, dVectortmp, atatmp, atdtmp);
//					throw new RuntimeException("Not implemented yet");
//					eq = new ObservationEquation(partialIDs, parameterList, dVector, time_source, time_receiver, combinationType, nUnknowns,
//							unknownParameterWeightType, verticalMapping);
				}
				
				//write AtA for later use
				Path outputPath = workPath.resolve("ata" + istep + "_" + tempString + ".dat");
				FrequencyRange frequencyRange = new FrequencyRange(1./ids[0].getMaxPeriod(), 1./ids[0].getMinPeriod());
				UnknownParameter[] unknownParameters = parameterList.toArray(new UnknownParameter[0]);
				Phases phase = new Phases(ids[0].getPhases());
				AtAFile.write(eq.getAtA(), weightingType, frequencyRange, unknownParameters, phase, outputPath);
				
				//write Atd for later use
				Path outputPathAtd = workPath.resolve("atd" + istep + "_" + tempString + ".dat");
				StaticCorrectionType correctionType = StaticCorrectionType.S;
				AtdFile.write(eq.getAtD(), unknownParameters, weightingType, frequencyRange, phase, correctionType, outputPathAtd);
			}
			
			//write AtA for later use
			Path outputPath = workPath.resolve("ata" + tempString + ".dat");
			FrequencyRange frequencyRange = new FrequencyRange(1./ids[0].getMaxPeriod(), 1./ids[0].getMinPeriod());
			UnknownParameter[] unknownParameters = parameterList.toArray(new UnknownParameter[0]);
			Phases phase = new Phases(ids[0].getPhases());
			AtAFile.write(eq.getAtA(), weightingType, frequencyRange, unknownParameters, phase, outputPath);
			
			//write Atd for later use
			Path outputPathAtd = workPath.resolve("atd" + tempString + ".dat");
			StaticCorrectionType correctionType = StaticCorrectionType.S;
			AtdFile.write(eq.getAtD(), unknownParameters, weightingType, frequencyRange, phase, correctionType, outputPathAtd);
			
			if (modelCovariance) {
				System.out.println("Building covariance matrix");
				double meanTrace = eq.getAtA().getTrace() / parameterList.size();
				System.out.println("AtANormalizedTrace = " + meanTrace);
				ModelCovarianceMatrix cm = new ModelCovarianceMatrix(parameterList, cmV, cmH, cm0 / meanTrace, true);
				eq.applyModelCovarianceMatrix(cm);
			}
			
			if (conditioner)
				applyConditionner();
		}
		
//		Path AtAPath = workPath.resolve("ata_stable_" + weightingType + ".dat");
//		Path AtdPath = workPath.resolve("atd_stable_" + weightingType + ".dat");
//		eq.outputAtA(AtAPath);
//		eq.outputAtd(AtdPath);
//		System.exit(0);
	}
	
	private void applyConditionner() {
		RealVector m = new ArrayRealVector(eq.getMlength());
		
		Set<Double> rs = eq.getParameterList().stream().map(p -> p.getLocation().getR()).collect(Collectors.toSet());
		Set<PartialType> types = eq.getParameterList().stream().map(p -> p.getPartialType()).collect(Collectors.toSet());
		
		System.out.print("Applying conditionner with partial types ");
		types.stream().forEach(t -> System.out.print(t + " "));
		System.out.println();
		
		RealVector diagonalOfAtA = eq.getDiagonalOfAtA();
		
//		double ataTrace = diagonalOfAtA.getLInfNorm();
		
		for (PartialType type : types) {
			for (double r : rs) {
				double mr = 0;
				for (int ip = 0; ip < m.getDimension(); ip++)
					if (eq.getParameterList().get(ip).getPartialType().equals(type))
						if (eq.getParameterList().get(ip).getLocation().getR() == r) {
//								mr += eq.getAtA().getEntry(ip, ip);
							mr += diagonalOfAtA.getEntry(ip);
						}
				mr = Math.pow(1. / mr, 0.375);
//				mr = Math.pow(1. / mr, 0.5);
//					System.out.println(r + " " + type + " " + mr);
				
//					if (type.equals(PartialType.LAMBDA))
//						mr /= Math.sqrt(2.);
				
				for (int ip = 0; ip < m.getDimension(); ip++)
					if (eq.getParameterList().get(ip).getPartialType().equals(type))
						if (eq.getParameterList().get(ip).getLocation().getR() == r)
							m.setEntry(ip, mr);
			}
		}
		

//		for (PartialType type : types) {
//			for (int ip = 0; ip < m.getDimension(); ip++)
//				if (eq.getParameterList().get(ip).getPartialType().equals(type))
//					if (eq.getParameterList().get(ip).getLocation().getR() == 3830)
//						m.setEntry(ip, m.getEntry(ip)*.8);
//		}
		
		eq.applyConditioner(m);
	}
	
	private void addRegularizationMUQ() {
		System.out.println("Adding regularization MU Q");
		List<PartialType> types = new ArrayList<>();
		Set<PartialType> usedTypes = eq.getParameterList().stream().map(p -> p.getPartialType()).collect(Collectors.toSet());
		if (usedTypes.contains(PartialType.PAR2))
			types.add(PartialType.PAR2);
		if (usedTypes.contains(PartialType.PARQ))
			types.add(PartialType.PARQ);
		
		double normMU = Math.sqrt(new ArrayRealVector(IntStream.range(0, eq.getMlength())
				.filter(i -> eq.getParameterList().get(i).getPartialType().equals(PartialType.PAR2))
				.mapToDouble(i -> eq.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm());
		double normQ = Math.sqrt(new ArrayRealVector(IntStream.range(0, eq.getMlength())
				.filter(i -> eq.getParameterList().get(i).getPartialType().equals(PartialType.PARQ))
				.mapToDouble(i -> eq.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm());
		
		// Second order differential operator
		List<Double> coeffs = new ArrayList<>();
		if (usedTypes.contains(PartialType.PAR2))
			coeffs.add(lambdaMU / normMU);
		if (usedTypes.contains(PartialType.PARQ))
			coeffs.add(lambdaQ / normQ);
		
		RadialSecondOrderDifferentialOperator D2 = new RadialSecondOrderDifferentialOperator(eq.getParameterList(), types, coeffs);
		eq.addRegularization(D2.getD2TD2());
		
		// Diagonal matrix
		coeffs = new ArrayList<>();
		if (usedTypes.contains(PartialType.PAR2))
			coeffs.add(gammaMU / normMU);
		if (usedTypes.contains(PartialType.PARQ))
			coeffs.add(gammaQ / normQ);
		
		RealMatrix D = MatrixUtils.createRealIdentityMatrix(eq.getMlength());
		List<UnknownParameter> parameters = eq.getParameterList();
		for (int i = 0; i < eq.getMlength(); i++) {
			if (parameters.get(i).getPartialType().equals(PartialType.PAR2))
				D.multiplyEntry(i, i, coeffs.get(0));
			else if (parameters.get(i).getPartialType().equals(PartialType.PARQ))
				D.multiplyEntry(i, i, coeffs.get(1));
		}
		eq.addRegularization(D);
	}
	
	private void addRegularizationVSQ() {
		System.out.println("Adding regularization VS Q");
		List<PartialType> types = new ArrayList<>();
		Map<PartialType, Integer> indexMap = new HashMap<>();
		Set<PartialType> usedTypes = eq.getParameterList().stream().map(p -> p.getPartialType()).collect(Collectors.toSet());
		int count = 0;
		if (usedTypes.contains(PartialType.PARVS)) {
			types.add(PartialType.PARVS);
			indexMap.put(PartialType.PARVS, count);
			count++;
		}
		if (usedTypes.contains(PartialType.PARQ)) {
			types.add(PartialType.PARQ);
			indexMap.put(PartialType.PARQ, count);
			count++;
		}
		if (usedTypes.contains(PartialType.PAR00)) {
			types.add(PartialType.PAR00);
			indexMap.put(PartialType.PAR00, count);
			count++;
		}
		if (usedTypes.contains(PartialType.PARVP)) {
			types.add(PartialType.PARVP);
			indexMap.put(PartialType.PARVP, count);
			count++;
		}
		
		double normMU = Math.sqrt(new ArrayRealVector(IntStream.range(0, eq.getMlength())
				.filter(i -> eq.getParameterList().get(i).getPartialType().equals(PartialType.PARVS))
				.mapToDouble(i -> eq.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm());
		double normQ = Math.sqrt(new ArrayRealVector(IntStream.range(0, eq.getMlength())
				.filter(i -> eq.getParameterList().get(i).getPartialType().equals(PartialType.PARQ))
				.mapToDouble(i -> eq.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm());
		double norm00 = Math.sqrt(new ArrayRealVector(IntStream.range(0, eq.getMlength())
				.filter(i -> eq.getParameterList().get(i).getPartialType().equals(PartialType.PAR00))
				.mapToDouble(i -> eq.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm());
		double normVP = Math.sqrt(new ArrayRealVector(IntStream.range(0, eq.getMlength())
				.filter(i -> eq.getParameterList().get(i).getPartialType().equals(PartialType.PARVP))
				.mapToDouble(i -> eq.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm());
		
		// Second order differential operator
		List<Double> coeffs = new ArrayList<>();
		if (usedTypes.contains(PartialType.PARVS))
			coeffs.add(lambdaMU * normMU);
//			coeffs.add(lambdaMU / normMU);
		if (usedTypes.contains(PartialType.PARQ))
			coeffs.add(lambdaQ * normQ);
		if(usedTypes.contains(PartialType.PAR00))
			coeffs.add(lambda00 * norm00);
		if(usedTypes.contains(PartialType.PARVP))
			coeffs.add(lambdaVp * normVP);
		
		RadialSecondOrderDifferentialOperator D2 = new RadialSecondOrderDifferentialOperator(eq.getParameterList(), types, coeffs);
		eq.addRegularization(D2.getD2TD2());
		
		// Diagonal matrix
		coeffs = new ArrayList<>();
		if (usedTypes.contains(PartialType.PARVS))
			coeffs.add(gammaMU * normMU);
//			coeffs.add(gammaMU / normMU);
		if (usedTypes.contains(PartialType.PARQ))
			coeffs.add(gammaQ * normQ);
		if(usedTypes.contains(PartialType.PAR00))
			coeffs.add(gamma00 * norm00);
		if(usedTypes.contains(PartialType.PARVP))
			coeffs.add(gammaVp * normVP);
		
		RealMatrix D = MatrixUtils.createRealIdentityMatrix(eq.getMlength());
		List<UnknownParameter> parameters = eq.getParameterList();
		for (int i = 0; i < eq.getMlength(); i++) {
			if (parameters.get(i).getPartialType().equals(PartialType.PARVS)) {
				int index = indexMap.get(PartialType.PARVS);
				D.multiplyEntry(i, i, coeffs.get(index) * coeffs.get(index));
			}
			else if (parameters.get(i).getPartialType().equals(PartialType.PARQ)) {
				int index = indexMap.get(PartialType.PARQ);
				D.multiplyEntry(i, i, coeffs.get(index) * coeffs.get(index));
			}
			else if (parameters.get(i).getPartialType().equals(PartialType.PAR00)) {
				int index = indexMap.get(PartialType.PAR00);
				D.multiplyEntry(i, i, coeffs.get(index) * coeffs.get(index));
			}
			else if (parameters.get(i).getPartialType().equals(PartialType.PARVP)) {
				int index = indexMap.get(PartialType.PARVP);
				D.multiplyEntry(i, i, coeffs.get(index) * coeffs.get(index));
			}
		}
		
		PartialType tmptype = eq.getParameterList().get(0).getPartialType();
		for (int i = 1; i < eq.getMlength(); i++) {
			if (!tmptype.equals(eq.getParameterList().get(i).getPartialType())) {
				D.multiplyEntry(i - 1, i - 1, 25.);
				System.out.println(i + " " + tmptype + " " + eq.getParameterList().get(i).getPartialType());
				tmptype = eq.getParameterList().get(i).getPartialType();
			}
		}
		D.multiplyEntry(eq.getMlength()-1, eq.getMlength()-1, 25.);

		eq.addRegularization(D);
	}
	
	private void addRegularizationSimpleQMU() {
		System.out.println("Adding simple regularization Q MU");
		List<PartialType> types = new ArrayList<>();
		types.add(PartialType.PAR2);
		types.add(PartialType.PARQ);
		
		double[][] sensitivity = new double[][] { {3490.0,1.11},{3510.0,0.86},{3530.0,0.58},{3550.0,0.42},{3570.0,0.43},{3590.0,0.50},{3610.0,0.55},{3630.0,0.58},{3650.0,0.61},{3670.0,0.62},{3690.0,0.62},{3710.0,0.61},{3730.0,0.61},{3750.0,0.60},{3770.0,0.59},{3790.0,0.57},{3810.0,0.55},{3830.0,0.53},{3850.0,0.50},{3870.0,0.48},{3890.0,0.47},{3910.0,0.45},{3930.0,0.45},{3950.0,0.44},{3970.0,0.44} };
		
		double normMU = Math.sqrt(new ArrayRealVector(IntStream.range(0, eq.getMlength())
				.filter(i -> eq.getParameterList().get(i).getPartialType().equals(PartialType.PAR2))
				.mapToDouble(i -> eq.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm());
		double normQ = Math.sqrt(new ArrayRealVector(IntStream.range(0, eq.getMlength())
				.filter(i -> eq.getParameterList().get(i).getPartialType().equals(PartialType.PARQ))
				.mapToDouble(i -> eq.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm());
		List<Double> coeffs = new ArrayList<>();
		coeffs.add(gammaMU / normMU);
		coeffs.add(gammaQ / normQ);
		
		RealMatrix D = MatrixUtils.createRealIdentityMatrix(eq.getMlength());
		List<UnknownParameter> parameters = eq.getParameterList();
		for (int i = 0; i < eq.getMlength(); i++) {
			
			if (parameters.get(i).getPartialType().equals(PartialType.PAR2))
				D.multiplyEntry(i, i, coeffs.get(0));
			else if (parameters.get(i).getPartialType().equals(PartialType.PARQ))
				D.multiplyEntry(i, i, coeffs.get(1));
		}
		
		eq.addRegularization(D);
	}
	
	private void applyConditionnerAll() {
		RealVector m = new ArrayRealVector(eq.getMlength());
		
		Set<Double> rs = eq.getParameterList().stream().map(p -> p.getLocation().getR()).collect(Collectors.toSet());
		Set<PartialType> types = eq.getParameterList().stream().map(p -> p.getPartialType()).collect(Collectors.toSet());
		
		System.out.print("Applying conditionner with partial types ");
		types.stream().forEach(t -> System.out.print(t + " "));
		System.out.println();
		
		RealVector diagonalOfAtA = eq.getDiagonalOfAtA();
		
		double npow = 0.5;
		
		for (PartialType type : types) {
			for (double r : rs) {
				double mr = 0;
				double maxSensitivityR = 0.;
				
				for (int ip = 0; ip < m.getDimension(); ip++)
					if (eq.getParameterList().get(ip).getPartialType().equals(type))
						if (eq.getParameterList().get(ip).getLocation().getR() == r)
							if (diagonalOfAtA.getEntry(ip) > maxSensitivityR)
								maxSensitivityR = diagonalOfAtA.getEntry(ip);
				
				for (int ip = 0; ip < m.getDimension(); ip++)
					if (eq.getParameterList().get(ip).getPartialType().equals(type))
						if (eq.getParameterList().get(ip).getLocation().getR() == r) {
							double alphai = Math.pow(maxSensitivityR / diagonalOfAtA.getEntry(ip), npow);
							if (alphai > 2.)
								alphai = 2.;
							m.setEntry(ip, alphai);
							mr += alphai * diagonalOfAtA.getEntry(ip);
						}
				
				mr = Math.pow(1. / mr, npow);
//				mr = Math.sqrt(1. / mr);
				
				for (int ip = 0; ip < m.getDimension(); ip++)
					if (eq.getParameterList().get(ip).getPartialType().equals(type))
						if (eq.getParameterList().get(ip).getLocation().getR() == r) {
							double alphai = m.getEntry(ip);
							m.setEntry(ip, mr * Math.pow(alphai, npow));
						}
			}
		}
		
		eq.applyConditioner(m);
	}
	
	private void applyConditionnerBulk() {
		RealVector m = new ArrayRealVector(eq.getMlength());
		
		Set<PartialType> types = eq.getParameterList().stream().map(p -> p.getPartialType()).collect(Collectors.toSet());
		
		System.out.print("Applying conditionner bulk with partial types ");
		types.stream().forEach(t -> System.out.print(t + " "));
		System.out.println();
		
		RealVector diagonalOfAtA = eq.getDiagonalOfAtA();
		
		for (PartialType type : types) {
			double mr = 0;
			for (int ip = 0; ip < m.getDimension(); ip++)
				if (eq.getParameterList().get(ip).getPartialType().equals(type))
					mr += diagonalOfAtA.getEntry(ip);
			
//				mr = Math.pow(1. / mr, 0.375);
			mr = Math.pow(1. / mr, 0.5);
//					System.out.println(r + " " + type + " " + mr);
			
//					if (type.equals(PartialType.LAMBDA))
//						mr /= Math.sqrt(2.);
			
			for (int ip = 0; ip < m.getDimension(); ip++)
				if (eq.getParameterList().get(ip).getPartialType().equals(type))
					m.setEntry(ip, mr);
		}
		
		for (PartialType type : types)
			for (int ip = 0; ip < m.getDimension(); ip++)
				if (eq.getParameterList().get(ip).getPartialType().equals(type))
					if (eq.getParameterList().get(ip).getLocation().getR() == 3830)
						m.setEntry(ip, m.getEntry(ip)*.5);
		
		eq.applyConditioner(m);
	}
	
	public boolean isPair(BasicID basicID, PartialID partialID) {
		return basicID.getGlobalCMTID().equals(partialID.getGlobalCMTID()) 
				&& basicID.getStation().equals(partialID.getStation()) 
				&& basicID.getSacComponent().equals(partialID.getSacComponent())
				&& Math.abs(basicID.getStartTime() - partialID.getStartTime()) < 1.;
	}
	
	/**
	 * Output information of observation equation
	 */
	private Future<Void> output() throws IOException {
		// // ステーションの情報の読み込み
		System.err.print("reading station Information");
		if (stationSet == null)
			stationSet = StationInformationFile.read(stationInformationPath);
		System.err.println(" done");
		Dvector dVector = eq.getDVector();
		Callable<Void> output = () -> {
			outputDistribution(outPath.resolve("stationEventDistribution.inf"));
			dVector.outOrder(outPath);
			dVector.outPhases(outPath);
			outEachTrace(outPath.resolve("trace"));
			UnknownParameterFile.write(eq.getParameterList(), outPath.resolve("unknownParameterOrder.inf"));
			UnknownParameterFile.write(eq.getOriginalParameterList(), outPath.resolve("originalUnknownParameterOrder.inf"));
			eq.outputA(outPath.resolve("partial"));
//			eq.outputAtA(outPath.resolve("lmi_AtA.inf"));
			eq.outputUnkownParameterWeigths(outPath.resolve("unknownParameterWeigths.inf"));
			dVector.outWeighting(outPath);
			return null;
		};
		FutureTask<Void> future = new FutureTask<>(output);

		new Thread(future).start();
		return future;
	}

	@Override
	public void run() {
		try {
			System.err.println("The output folder: " + outPath);
			Files.createDirectory(outPath);
			if (property != null)
				writeProperties(outPath.resolve("lmi.properties"));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Can not create " + outPath);
		}

		long start = System.nanoTime();

		// 観測方程式
		Future<Void> future;
		try {
			future = output();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		// 逆問題
		solve();
		try {
			future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		try {
			if (!inverseMethods.contains(InverseMethodEnum.FAST_CONJUGATE_GRADIENT) && !inverseMethods.contains(InverseMethodEnum.FAST_CONJUGATE_GRADIENT_DAMPED))
				eq.outputSensitivity(outPath.resolve("sensitivity.inf"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.err.println("Inversion is done in " + Utilities.toTimeString(System.nanoTime() - start));
	}

	/**
	 * outDirectory下にtraceフォルダを作りその下に理論波形と観測波形を書き込む
	 * 
	 * @param outPath
	 *            {@link Path} for output folder
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void outEachTrace(Path outPath) throws IOException {
		if (Files.exists(outPath))
			throw new FileAlreadyExistsException(outPath.toString());
		Files.createDirectories(outPath);
		Dvector d = eq.getDVector();

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
	public void outEachTrace(Path outPath, RealVector[] vectors) throws IOException {
		// if (outDirectory.exists())
		// return;
		int nTimeWindow = eq.getDVector().getNTimeWindow();
		if (vectors.length != nTimeWindow)
			return;
		for (int i = 0; i < nTimeWindow; i++)
			if (vectors[i].getDimension() != eq.getDVector().getSynVec()[i].getDimension())
				return;
		Files.createDirectories(outPath);
		for (GlobalCMTID id : eq.getDVector().getUsedGlobalCMTIDset()) {
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
		BasicID[] obsIDs = eq.getDVector().getObsIDs();
		BasicID[] synIDs = eq.getDVector().getSynIDs();
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

	private void solve() {
		inverseMethods.forEach(method -> {
			try {
				if (method == InverseMethodEnum.LEAST_SQUARES_METHOD)
					return; // TODO
				if (modelCovariance) {
//					solve(outPath.resolve(method.simple()), method.getMethod(eq.getCmAtA_1(), eq.getCmAtD()));
					if (method == InverseMethodEnum.FAST_CONJUGATE_GRADIENT || method == InverseMethodEnum.FAST_CONJUGATE_GRADIENT_DAMPED) {
						if (jackknife) {
							for (int ires = 0; ires < nRealisation; ires++) {
								System.out.println("Jackknife realisation " + ires);
								Random random = new Random();
//								List<Boolean> randomList = random.doubles(eq.getDVector().getNTimeWindow()).mapToObj(d -> new Boolean(d < 0.5)).collect(Collectors.toList());
								int[] shuffleIndexesWindows = random.ints(eq.getDVector().getNTimeWindow(), 0, eq.getDVector().getNTimeWindow()).toArray();
								List<Integer> shuffleIndexesPoints = new ArrayList<>();
//								boolean[] randomChooser = new boolean[eq.getDVector().getNpts()];
								int counter = 0;
//								int n = eq.getDVector().getNpts();
//								for (int iwin = 0; iwin < eq.getDVector().getNTimeWindow(); iwin++) {
//									if (randomList.get(iwin))
//										for (int j = 0; j < eq.getDVector().getLengths()[iwin]; j++) {
//											randomChooser[counter++] = true;
//										}
//									else
//										for (int j = 0; j < eq.getDVector().getLengths()[iwin]; j++)
//											randomChooser[counter++] = false;
//								}
								for (int iwin = 0; iwin < eq.getDVector().getNTimeWindow(); iwin++) {
									int iwinShuffled = shuffleIndexesWindows[iwin];
									int istart = eq.getDVector().getStartPoints(iwinShuffled);
									for (int j = 0; j < eq.getDVector().getLengths()[iwinShuffled]; j++) {
										shuffleIndexesPoints.add(istart + j);
									}
								}
								int n = shuffleIndexesPoints.size();
								RealMatrix a = eq.getA();
								RealMatrix ja = MatrixUtils.createRealMatrix(n, eq.getMlength());
								RealVector jd = new ArrayRealVector(n);
								RealVector d = eq.getDVector().getD();
								System.out.println(a.getRowDimension() + " " + ja.getRowDimension());
								for (int irow = 0; irow < n; irow++) {
									ja.setRowVector(irow, a.getRowVector(shuffleIndexesPoints.get(irow)));
									jd.setEntry(irow, d.getEntry(shuffleIndexesPoints.get(irow)));
								}
//								RealVector jatd = ja.preMultiply(eq.getDVector().getD());
								RealVector jatd = ja.preMultiply(jd);
								method.setConditioner(eq.getM());
								solveMinimalOutput(outPath.resolve(method.simple() + String.valueOf(ires)), method.getMethod(ja, jatd));
							}
						}
						else {
							solve(outPath.resolve(method.simple()), method.getMethod(eq.getA(), eq.getAtD()));
						}
					}
					else {
						solve(outPath.resolve(method.simple()), method.getMethod(eq.getAtA(), eq.getAtD()));
					}
				}
				else {
					if (method == InverseMethodEnum.FAST_CONJUGATE_GRADIENT || method == InverseMethodEnum.FAST_CONJUGATE_GRADIENT_DAMPED) {
						if (jackknife) {
							for (int ires = 0; ires < nRealisation; ires++) {
								System.out.println("Jackknife realisation " + ires);
								Random random = new Random();
//								List<Boolean> randomList = random.doubles(eq.getDVector().getNTimeWindow()).mapToObj(d -> new Boolean(d < 0.5)).collect(Collectors.toList());
								int[] shuffleIndexesWindows = random.ints(eq.getDVector().getNTimeWindow(), 0, eq.getDVector().getNTimeWindow()).toArray();
								List<Integer> shuffleIndexesPoints = new ArrayList<>();
//								boolean[] randomChooser = new boolean[eq.getDVector().getNpts()];
								int counter = 0;
//								int n = eq.getDVector().getNpts();
//								for (int iwin = 0; iwin < eq.getDVector().getNTimeWindow(); iwin++) {
//									if (randomList.get(iwin))
//										for (int j = 0; j < eq.getDVector().getLengths()[iwin]; j++) {
//											randomChooser[counter++] = true;
//										}
//									else
//										for (int j = 0; j < eq.getDVector().getLengths()[iwin]; j++)
//											randomChooser[counter++] = false;
//								}
								for (int iwin = 0; iwin < eq.getDVector().getNTimeWindow(); iwin++) {
									int iwinShuffled = shuffleIndexesWindows[iwin];
									int istart = eq.getDVector().getStartPoints(iwinShuffled);
									for (int j = 0; j < eq.getDVector().getLengths()[iwinShuffled]; j++) {
										shuffleIndexesPoints.add(istart + j);
									}
								}
								int n = shuffleIndexesPoints.size();
								RealMatrix a = eq.getA();
								RealMatrix ja = MatrixUtils.createRealMatrix(n, eq.getMlength());
								RealVector jd = new ArrayRealVector(n);
								RealVector d = eq.getDVector().getD();
								System.out.println(a.getRowDimension() + " " + ja.getRowDimension());
								for (int irow = 0; irow < n; irow++) {
									ja.setRowVector(irow, a.getRowVector(shuffleIndexesPoints.get(irow)));
									jd.setEntry(irow, d.getEntry(shuffleIndexesPoints.get(irow)));
								}
//								RealVector jatd = ja.preMultiply(eq.getDVector().getD());
								RealVector jatd = ja.preMultiply(jd);
								solveMinimalOutput(outPath.resolve(method.simple() + String.valueOf(ires)), method.getMethod(ja, jatd));
							}
						}
						else {
							solve(outPath.resolve(method.simple()), method.getMethod(eq.getA(), eq.getAtD()));
						}
					}
					else if (method == InverseMethodEnum.NONLINEAR_CONJUGATE_GRADIENT) {
//						eq.applyCombiner(3);
						solve(outPath.resolve(method.simple()), method.getMethod(eq.getAtA(), eq.getA(), eq.getDVector().getObs(), eq.getDVector().getSyn()));
					}
					else if (method == InverseMethodEnum.CONSTRAINED_CONJUGATE_GRADIENT) {
						RealMatrix h = ConstrainedConjugateGradientMethod.projectorRectangle(eq.getMlength(), 2);
						solve(outPath.resolve(method.simple()), method.getMethod(eq.getAtA(), eq.getAtD(), h));
					}
					else {
//						eq.applyCombiner2(2);
						solve(outPath.resolve(method.simple()), method.getMethod(eq.getAtA(), eq.getAtD()));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void solve(Path outPath, InverseProblem inverseProblem) throws IOException {
		// invOutDir.mkdir();
		inverseProblem.compute();
		Files.createDirectories(outPath);
		outVariance(outPath, inverseProblem);
		outVariancePerEvents(outPath, inverseProblem);
		
		inverseProblem.outputAnsX(outPath);
		
		if (eq.getCm() != null) {
			System.out.println("Computing model perturbation from the modified inverse problem's solution");
			computeDeltaM(inverseProblem);
		}
		
		if (eq.getM() != null) {
			System.out.println("Computing model perturbation for pre-conditionned CG");
			computeDeltaMFromConditionner(inverseProblem);
		}
		
		inverseProblem.outputAns(outPath);
		
		// 基底ベクトルの書き出し SVD: vt, CG: cg ベクトル
		RealMatrix p = inverseProblem.getBaseVectors();
		for (int j = 0; j < eq.getMlength(); j++)
			writeDat(outPath.resolve("p" + j + ".txt"), p.getColumn(j));
	}
	
	private void solveMinimalOutput(Path outPath, InverseProblem inverseProblem) throws IOException {
		// invOutDir.mkdir();
		inverseProblem.compute();
		Files.createDirectories(outPath);
//		outVariance(outPath, inverseProblem);
//		outVariancePerEvents(outPath, inverseProblem);
		
		if (eq.getCm() != null) {
			System.out.println("Computing model perturbation from the modified inverse problem's solution");
			computeDeltaM(inverseProblem);
		}
		
		if (eq.getM() != null) {
			System.out.println("Computing model perturbation for pre-conditionned CG");
			computeDeltaMFromConditionner(inverseProblem);
		}
		
		inverseProblem.outputAns(outPath);
		
		// 基底ベクトルの書き出し SVD: vt, CG: cg ベクトル
//		RealMatrix p = inverseProblem.getBaseVectors();
//		for (int j = 0; j < eq.getMlength(); j++)
//			writeDat(outPath.resolve("p" + j + ".txt"), p.getColumn(j));
	}
	
	private void computeDeltaM(InverseProblem inverseProblem) {
		int n = eq.getMlength();//Math.max(20, eq.getMlength());
		ModelCovarianceMatrix cm = eq.getCm();
		RealMatrix l = cm.getL();
		for (int i = 1; i <= n; i++) {
			RealVector deltaM = l.operate(inverseProblem.getAns(i));
			inverseProblem.setANS(i, deltaM);
		}
	}
	
	private void computeDeltaMFromConditionner(InverseProblem inverseProblem) {
		int n = eq.getMlength();//Math.max(20, eq.getMlength());
		RealVector m = eq.getM();
		for (int i = 1; i <= n; i++) {
			RealVector deltaM = inverseProblem.getAns(i);
			for (int k = 0; k < n; k++)
				deltaM.setEntry(k, deltaM.getEntry(k) * m.getEntry(k));
			inverseProblem.setANS(i, deltaM);
		}
	}
	
	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		LetMeInvert lmi = new LetMeInvert(Property.parse(args));
		System.err.println(LetMeInvert.class.getName() + " is running.");
		long startT = System.nanoTime();
		lmi.run();
		System.err.println(
				LetMeInvert.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - startT));
	}

	/**
	 * outDirectory下にvarianceを書き込む
	 * 
	 * @param outPath
	 */
	private void outVariance(Path outPath, InverseProblem inverse) throws IOException {
		System.out.println("Outputting variance");
		Path out = outPath.resolve("variance.txt");
		if (Files.exists(out))
			throw new FileAlreadyExistsException(out.toString());
		double[] variance = new double[eq.getMlength() + 1];
		variance[0] = eq.getDVector().getVariance();
		int tmpN = eq.getMlength();
//		for (int i = 0; i < eq.getMlength(); i++)
		for (int i = 0; i < tmpN; i++)
			variance[i + 1] = eq.varianceOf(inverse.getANS().getColumnVector(i));
		writeDat(out, variance);
		if (alpha == null)
			return;
		for (int i = 0; i < alpha.length; i++) {
			out = outPath.resolve("aic" + i + ".txt");
			double[] aic = computeAIC(variance, alpha[i]);
			writeDat(out, aic);
		}
		writeDat(outPath.resolve("aic.inf"), alpha);
	}
	
	private void outVariancePerEvents(Path outPath, InverseProblem inverse) throws IOException {
		if (eq.getA() == null) {
			System.out.println("a is null, cannot output variance per event");
			return;
		}
		Set<NDK> gcmtCat = GlobalCMTCatalog.readJar("globalcmt.catalog");
		
		Set<GlobalCMTID> eventSet = eq.getDVector().getUsedGlobalCMTIDset();
		Path out = outPath.resolve("eventVariance.txt");
		int n = 31 > eq.getMlength() ? eq.getMlength() : 31;
		Map<GlobalCMTID, double[]> varianceMap = new HashMap<>();
		for (GlobalCMTID id : eventSet) {
			if (Files.exists(out))
				throw new FileAlreadyExistsException(out.toString());
			double[] variance = new double[n];
			variance[0] = eq.getDVector().getEventVariance().get(id);
			RealVector residual = eq.getDVector().getD();
			RealVector obsVec = eq.getDVector().getObs();
			RealVector mask = eq.getDVector().getMask(id);
			
			for (int i = 0; i < residual.getDimension(); i++) {
				residual.setEntry(i, residual.getEntry(i) * mask.getEntry(i));
				obsVec.setEntry(i, obsVec.getEntry(i) * mask.getEntry(i));
			}
			
			for (int i = 0; i < n-1; i++) {
				RealVector adm = eq.getA().operate(inverse.getANS().getColumnVector(i));
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
	private double[] computeAIC(double[] variance, double alpha) {
		double[] aic = new double[variance.length];
		int independentN = (int) (eq.getDlength() / alpha);
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
		if (Files.exists(outPath)) {
			new FileAlreadyExistsException(outPath.toString()).printStackTrace();
			cango = false;
		}
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
		if (!Files.exists(waveformPath)) {
			new NoSuchFileException(waveformPath.toString()).printStackTrace();
			cango = false;
		}
		if (!Files.exists(waveformIDPath)) {
			new NoSuchFileException(waveformIDPath.toString()).printStackTrace();
			cango = false;
		}
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
	public void outputDistribution(Path outPath) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			BasicID[] obsIDs = eq.getDVector().getObsIDs();
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

	private RealVector readCheckerboardPerturbationVector() throws IOException {
		return new ArrayRealVector(Files.readAllLines(checkerboardPerturbationPath).stream().mapToDouble(s -> Double.parseDouble(s.trim())).toArray());
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
