package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionType;
import io.github.kensuke1984.kibrary.datacorrection.TakeuchiStaticCorrection;
import io.github.kensuke1984.kibrary.inversion.addons.CombinationType;
import io.github.kensuke1984.kibrary.inversion.addons.ModelCovarianceMatrix;
import io.github.kensuke1984.kibrary.inversion.addons.RadialSecondOrderDifferentialOperator;
import io.github.kensuke1984.kibrary.inversion.addons.UnknownParameterWeightType;
import io.github.kensuke1984.kibrary.inversion.addons.WeightingType;
import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.selection.DataSelectionInformation;
import io.github.kensuke1984.kibrary.selection.DataSelectionInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.EventCluster;
import io.github.kensuke1984.kibrary.util.addons.FrequencyRange;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTCatalog;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtAEntry;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtAFile;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtdEntry;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtdFile;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Precision;
import org.jfree.util.ArrayUtilities;

/**
 * Let's invert
 *
 * @author Kensuke Konishi
 * @version 2.0.3.6
 * @author anselme added regularization, ...
 */
public class LetMeInvert implements Operation {
	
    /**
     * path of waveform data
     */
	protected Path waveformPath;
    /**
     * Path of unknown parameter file
     */
	protected Path unknownParameterListPath;
    /**
     * path of partial ID file
     */
	protected Path partialIDPath;
    /**
     * path of partial data
     */
	protected Path partialPath;
    /**
     * path of basic ID file
     */
	protected Path waveformIDPath;
    /**
     * path of station file
     */
	protected Path stationInformationPath;
    /**
     * Solvers for equation
     */
	protected Set<InverseMethodEnum> inverseMethods;
	 /**
     * α for AIC 独立データ数:n/α
     */
	protected double[] alpha;
	private ObservationEquation eq;
	private Properties PROPERTY;
	private Path workPath;
	private Path outPath;
	private Set<Station> stationSet;
	
	private ObservationEquation eqA, eqB;
	Path spcAmpPath;
	Path spcAmpIDPath;
	Path partialSpcPath;
	Path partialSpcIDPath;
	
	protected WeightingType weightingType;
	protected boolean time_source, time_receiver;
	protected double gamma;
	private CombinationType combinationType;
	private Map<PartialType, Integer[]> nUnknowns;
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
	private double scale_freq_ata;
	private boolean regularizationMuQ;
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
	private int[] azimuthIndex;
	private int[] clusterIndex;
	private boolean applyEventAmpCorr;
	private boolean correct3DFocusing;
	private double mul;
	private Map<PartialType, Double> dataErrorMap;

	private void checkAndPutDefaults() {
		if (!PROPERTY.containsKey("workPath")) PROPERTY.setProperty("workPath", "");
		if (!PROPERTY.containsKey("stationInformationPath"))
			throw new IllegalArgumentException("There is no information about stationInformationPath.");
		if (!PROPERTY.containsKey("waveformIDPath"))
			throw new IllegalArgumentException("There is no information about 'waveformIDPath'.");
		if (!PROPERTY.containsKey("waveformPath"))
			throw new IllegalArgumentException("There is no information about 'waveformPath'.");
		if (!PROPERTY.containsKey("partialIDPath"))
			throw new IllegalArgumentException("There is no information about 'partialIDPath'.");
		if (!PROPERTY.containsKey("partialPath"))
			throw new IllegalArgumentException("There is no information about 'partialPath'.");
		if (!PROPERTY.containsKey("inverseMethods")) PROPERTY.setProperty("inverseMethods", "CG SVD");
		if (!PROPERTY.containsKey("weighting")) PROPERTY.setProperty("weighting", "RECIPROCAL");
		if (!PROPERTY.containsKey("time_source")) PROPERTY.setProperty("time_source", "false");
		if (!PROPERTY.containsKey("time_receiver")) PROPERTY.setProperty("time_receiver", "false");
		if (!PROPERTY.containsKey("modelCovariance")) PROPERTY.setProperty("modelCovariance", "false");
		if (!PROPERTY.containsKey("lambdaQ")) PROPERTY.setProperty("lambdaQ", "0.3");
		if (!PROPERTY.containsKey("lambdaMU")) PROPERTY.setProperty("lambdaMU", "0.03");
		if (!PROPERTY.containsKey("gammaQ")) PROPERTY.setProperty("gammaQ", "0.3");
		if (!PROPERTY.containsKey("gammaMU")) PROPERTY.setProperty("gammaMU", "0.03");
		if (!PROPERTY.containsKey("correlationScaling")) PROPERTY.setProperty("correlationScaling", "1.");
		if (!PROPERTY.containsKey("minDistance")) PROPERTY.setProperty("minDistance", "0.");
		if (!PROPERTY.containsKey("maxDistance")) PROPERTY.setProperty("maxDistance", "360.");
		if (!PROPERTY.containsKey("minMw")) PROPERTY.setProperty("minMw", "0.");
		if (!PROPERTY.containsKey("maxMw")) PROPERTY.setProperty("maxMw", "10.");
		if (!PROPERTY.containsKey("linaInversion")) PROPERTY.setProperty("linaInversion", "false");
		if (!PROPERTY.containsKey("jackknife"))
			PROPERTY.setProperty("jackknife", "false");
		if (!PROPERTY.containsKey("conditioner"))
			PROPERTY.setProperty("conditioner", "false");
		if (!PROPERTY.containsKey("lowMemoryCost"))
			PROPERTY.setProperty("lowMemoryCost", "false");
		if (!PROPERTY.containsKey("nStepsForLowMemoryMode"))
			PROPERTY.setProperty("nStepsForLowMemoryMode", "10");
		if (!PROPERTY.containsKey("usePrecomputedAtA"))
			PROPERTY.setProperty("usePrecomputedAtA", "false");
		if (!PROPERTY.containsKey("checkerboard"))
			PROPERTY.setProperty("checkerboard", "false");
		if (!PROPERTY.containsKey("trimWindow"))
			PROPERTY.setProperty("trimWindow", "false");
		if (!PROPERTY.containsKey("regularizationMuQ"))
			PROPERTY.setProperty("regularizationMuQ", "false");
		if (!PROPERTY.containsKey("scale_freq_ata"))
			PROPERTY.setProperty("scale_freq_ata", "1.");
		if (!PROPERTY.containsKey("applyEventAmpCorr"))
			PROPERTY.setProperty("applyEventAmpCorr", "false");
		if (!PROPERTY.containsKey("correct3DFocusing"))
			PROPERTY.setProperty("correct3DFocusing", "false");
		
		// additional unused info
		PROPERTY.setProperty("CMTcatalogue", GlobalCMTCatalog.getCatalogPath().toString());
//		PROPERTY.setProperty("STF catalogue", GlobalCMTCatalog.);
	}

	private void set() {
		checkAndPutDefaults();
		workPath = Paths.get(PROPERTY.getProperty("workPath"));
		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		if (PROPERTY.containsKey("outPath"))
			outPath = getPath("outPath");
		else
			outPath = workPath.resolve(Paths.get("lmi" + Utilities.getTemporaryString()));
		stationInformationPath = getPath("stationInformationPath");
		waveformIDPath = getPath("waveformIDPath");
		waveformPath = getPath("waveformPath");
		partialPath = getPath("partialPath");
		partialIDPath = getPath("partialIDPath");
		unknownParameterListPath = getPath("unknownParameterListPath");
		if (PROPERTY.containsKey("alpha"))
			alpha = Arrays.stream(PROPERTY.getProperty("alpha").split("\\s+")).mapToDouble(Double::parseDouble)
					.toArray();
		inverseMethods = Arrays.stream(PROPERTY.getProperty("inverseMethods").split("\\s+")).map(InverseMethodEnum::of)
				.collect(Collectors.toSet());
		inverseMethods.stream().forEach(method -> System.out.println(method));
		weightingType = WeightingType.valueOf(PROPERTY.getProperty("weighting"));
		time_source = Boolean.parseBoolean(PROPERTY.getProperty("time_source"));
		time_receiver = Boolean.parseBoolean(PROPERTY.getProperty("time_receiver"));
		if (weightingType.equals(WeightingType.TAKEUCHIKOBAYASHI) || weightingType.equals(WeightingType.FINAL)) {
			if (!PROPERTY.containsKey("gamma"))
				throw new RuntimeException("gamma must be set in oreder to use TAKEUCHIKOBAYASHI or FINAL weighting scheme");
			gamma = Double.parseDouble(PROPERTY.getProperty("gamma"));
		}
		if (!PROPERTY.containsKey("phases"))
			phases = null;
		else
			phases = Arrays.stream(PROPERTY.getProperty("phases").trim().split("\\s+")).toArray(String[]::new);
		//
		if (!PROPERTY.containsKey("combinationType"))
			combinationType = null;
		else
			combinationType = CombinationType.valueOf(PROPERTY.getProperty("combinationType"));
		//
		if (!PROPERTY.containsKey("nUnknowns"))
			nUnknowns = null;
		else if (combinationType == null) {
			throw new RuntimeException("Error: a combinationType "
					+ "must be specified when nUnknowns is specified");
		}
		else {
			nUnknowns = new HashMap<>();
			String[] args = PROPERTY.getProperty("nUnknowns").trim().split("\\s+");
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
		if (PROPERTY.containsKey("DataSelectionInformationFile")) {
			System.out.println("Using dataSelectionInformationFile " + PROPERTY.getProperty("DataSelectionInformationFile"));
			try {
				selectionInfo = DataSelectionInformationFile.read(Paths.get(PROPERTY.getProperty("DataSelectionInformationFile")));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
			selectionInfo = null;
		
		modelCovariance = Boolean.valueOf(PROPERTY.getProperty("modelCovariance"));
		
		if (modelCovariance) {
			if (!PROPERTY.containsKey("cm0"))
				throw new RuntimeException("cm0 must be set when modelCovariance=true");
			if (!PROPERTY.containsKey("cmH"))
				throw new RuntimeException("cmH must be set when modelCovariance=true");
			if (!PROPERTY.containsKey("cmV"))
				throw new RuntimeException("cmV must be set when modelCovariance=true");
			
			cm0 = Double.parseDouble(PROPERTY.getProperty("cm0"));
			cmH = Double.parseDouble(PROPERTY.getProperty("cmH"));
			cmV = Double.parseDouble(PROPERTY.getProperty("cmV"));
		}
		
		regularizationMuQ = Boolean.parseBoolean(PROPERTY.getProperty("regularizationMuQ"));
		if (regularizationMuQ) {
			lambdaQ = Double.valueOf(PROPERTY.getProperty("lambdaQ"));
			lambdaMU = Double.valueOf(PROPERTY.getProperty("lambdaMU"));
			lambda00 = Double.valueOf(PROPERTY.getProperty("lambda00"));
			lambdaVp = Double.valueOf(PROPERTY.getProperty("lambdaVp"));
			gammaQ = Double.valueOf(PROPERTY.getProperty("gammaQ"));
			gammaMU = Double.valueOf(PROPERTY.getProperty("gammaMU"));
			gamma00 = Double.valueOf(PROPERTY.getProperty("gamma00"));
			gammaVp = Double.valueOf(PROPERTY.getProperty("gammaVp"));
		}
		
		correlationScaling = Double.valueOf(PROPERTY.getProperty("correlationScaling"));
		minDistance = Double.parseDouble(PROPERTY.getProperty("minDistance"));
		maxDistance = Double.parseDouble(PROPERTY.getProperty("maxDistance"));
		if (PROPERTY.getProperty("unknownParameterWeightType") == null)
			unknownParameterWeightType = null;
		else {
			unknownParameterWeightType = UnknownParameterWeightType.valueOf(PROPERTY.getProperty("unknownParameterWeightType"));
			System.out.println("--->Weighting unkown parameters using type " + unknownParameterWeightType);
		}
		minMw = Double.parseDouble(PROPERTY.getProperty("minMw"));
		maxMw = Double.parseDouble(PROPERTY.getProperty("maxMw"));
		
		linaInversion = Boolean.parseBoolean(PROPERTY.getProperty("linaInversion"));
		
		verticalMapping = PROPERTY.containsKey("verticalMapping") ? Paths.get(PROPERTY.getProperty("verticalMapping")) : null;
		
		jackknife = Boolean.parseBoolean(PROPERTY.getProperty("jackknife"));
		if (jackknife)
			nRealisation = Integer.parseInt(PROPERTY.getProperty("nRealisation"));
		
		conditioner = Boolean.parseBoolean(PROPERTY.getProperty("conditioner"));
		
		lowMemoryCost = Boolean.parseBoolean(PROPERTY.getProperty("lowMemoryCost"));
		
		nStepsForLowMemoryMode = Integer.parseInt(PROPERTY.getProperty("nStepsForLowMemoryMode"));
		
		usePrecomputedAtA = Boolean.parseBoolean(PROPERTY.getProperty("usePrecomputedAtA"));
		if (usePrecomputedAtA) {
			precomputedAtdPath = Stream.of(PROPERTY.getProperty("precomputedAtdPath").split("\\s+")).map(p -> Paths.get(p.trim())).collect(Collectors.toList()).toArray(new Path[0]);
			precomputedAtAPath = Stream.of(PROPERTY.getProperty("precomputedAtAPath").split("\\s+")).map(p -> Paths.get(p.trim())).collect(Collectors.toList()).toArray(new Path[0]);
		}
		
		checkerboard = Boolean.parseBoolean(PROPERTY.getProperty("checkerboard"));
		if (checkerboard)
			checkerboardPerturbationPath = Paths.get(PROPERTY.getProperty("checkerboardPerturbationPath"));
		
		trimWindow = Boolean.parseBoolean(PROPERTY.getProperty("trimWindow"));
		if (trimWindow) {
			trimPoint = Double.parseDouble(PROPERTY.getProperty("trimPoint"));
			keepBefore = Boolean.parseBoolean(PROPERTY.getProperty("keepBefore"));
		}
		
		if (PROPERTY.containsKey("eventClusterPath")) {
			eventClusterPath = Paths.get(PROPERTY.getProperty("eventClusterPath"));
			clusterIndex = Arrays.stream(PROPERTY.getProperty("clusterIndex").trim().split(" ")).mapToInt(Integer::parseInt).toArray();
			azimuthIndex = Arrays.stream(PROPERTY.getProperty("azimuthIndex").trim().split(" ")).mapToInt(Integer::parseInt).toArray();
			System.out.println("Using cluster file with clusterIndex=" + clusterIndex[0] + " and azimuthIndex=" + azimuthIndex[0]);
		}
		
		try {
			partialSpcPath = getPath("partialSpcPath");
			partialSpcIDPath = getPath("partialSpcIDPath");
		
			spcAmpPath = getPath("spcAmpPath");
			spcAmpIDPath = getPath("spcAmpIDPath");
		} catch (Exception e) {
			
		}
		
		scale_freq_ata = Double.parseDouble(PROPERTY.getProperty("scale_freq_ata"));
		
		dataErrorMap = null;
		
		applyEventAmpCorr = Boolean.parseBoolean(PROPERTY.getProperty("applyEventAmpCorr"));
		
		correct3DFocusing = Boolean.parseBoolean(PROPERTY.getProperty("correct3DFocusing"));
	}

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
			pw.println("##Path of a spcAmpID file");
			pw.println("#spcAmpIDPath ");
			pw.println("##Path of a spcAmp file");
			pw.println("#spcAmpPath ");
			pw.println("##Path of a partial id file, must be set");
			pw.println("#partialIDPath partialID.dat");
			pw.println("##Path of a partial waveform file must be set");
			pw.println("#partialPath partial.dat");
			pw.println("##Path of a partial spc id file");
			pw.println("#partialSpcIDPath ");
			pw.println("##Path of a partial spc waveform file");
			pw.println("#partialSpcPath ");
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
			pw.println("#time_source");
			pw.println("##boolean time_receiver (false). Time partial for the receiver");
			pw.println("#time_receiver");
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
			pw.println("#correct3DFocusing");
			pw.println("#applyEventAmpCorr");
		}
		System.err.println(outPath + " is created.");
	}

	public LetMeInvert(Properties property) throws IOException {
		this.PROPERTY = (Properties) property.clone();
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

	private void setEquation() throws IOException {
		BasicID[] ids = BasicIDFile.read(waveformIDPath, waveformPath);
		
		BasicID[] spcIds = null;
		if (spcAmpIDPath != null)
			spcIds = BasicIDFile.read(spcAmpIDPath, spcAmpPath);
		
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
			List<List<EventCluster>> thisCluster = new ArrayList<>();
			for (int i = 0; i < clusterIndex.length; i++) {
				final int ifinal = i;
				thisCluster.add(clusters.stream().filter(c -> c.getIndex() == clusterIndex[ifinal]
						).collect(Collectors.toList()));
			}
			
			double[][] azimuthRange = new double[clusterIndex.length][2];
			List<Set<GlobalCMTID>> thisClusterIDs = new ArrayList<>();
			HorizontalPosition[] centerPosition = new HorizontalPosition[clusterIndex.length];
			for (int i = 0; i < clusterIndex.length; i++) {
				azimuthRange[i] = thisCluster.get(i).get(0).getAzimuthBound(azimuthIndex[i]);
				thisClusterIDs.add(thisCluster.get(i).stream().map(c -> c.getID()).collect(Collectors.toSet()));
				centerPosition[i] = thisCluster.get(i).get(0).getCenterPosition();
			}
			
//			System.out.println(azimuthRange[0] + " " + azimuthRange[1]);
			
			chooser = id -> {
				boolean clusterKeep = false;
				for (int i = 0; i < clusterIndex.length; i++) {
					double azimuth = centerPosition[i].getAzimuth(id.getStation().getPosition())
							* 180. / Math.PI;
					if (thisClusterIDs.get(i).contains(id.getGlobalCMTID()) && azimuth >= azimuthRange[i][0] && azimuth <= azimuthRange[i][1])
						clusterKeep = true;
				}
				if (!clusterKeep)
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
				
				//TODO
				if (id.getStation().getName().equals("Y14A") && id.getGlobalCMTID().equals(new GlobalCMTID("200809031125A"))
					|| id.getStation().getName().equals("216A") && id.getGlobalCMTID().equals(new GlobalCMTID("200704180108A"))
					|| id.getGlobalCMTID().equals(new GlobalCMTID("201608041415A"))
					|| id.getGlobalCMTID().equals(new GlobalCMTID("201702181210A"))
					|| id.getStation().getName().equals("MONP2") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("RRX") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("BC3") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("TPNV") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("VOG") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("TPFO") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("AGMN") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("E39A") && id.getGlobalCMTID().equals(new GlobalCMTID("201306081225A"))
					|| id.getStation().getName().equals("F43A") && id.getGlobalCMTID().equals(new GlobalCMTID("201302221201A"))
					|| id.getStation().getName().equals("E47A") && id.getGlobalCMTID().equals(new GlobalCMTID("201404180746A"))
					|| id.getStation().getName().equals("G42A") && id.getGlobalCMTID().equals(new GlobalCMTID("201205281150A"))
					|| id.getStation().getName().equals("E45A") && id.getGlobalCMTID().equals(new GlobalCMTID("201302221201A"))
					|| id.getStation().getName().equals("COWI") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("JFWS") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("SFIN") && id.getGlobalCMTID().equals(new GlobalCMTID("201306081225A"))
					|| id.getStation().getName().equals("P43A") && id.getGlobalCMTID().equals(new GlobalCMTID("201306081225A"))
					|| id.getStation().getName().equals("SUSD") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("KSCO") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("ECSD") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("LBNH") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("PKME") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("GLMI") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("LONY") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("M50A") && id.getGlobalCMTID().equals(new GlobalCMTID("201306081225A"))
					|| id.getStation().getName().equals("SM38") && id.getGlobalCMTID().equals(new GlobalCMTID("201205281150A"))
					|| id.getStation().getName().equals("Q44A") && id.getGlobalCMTID().equals(new GlobalCMTID("201205281150A"))
					|| id.getStation().getName().equals("K35A") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
					|| id.getStation().getName().equals("SS67") && id.getGlobalCMTID().equals(new GlobalCMTID("201302221201A"))
					|| id.getStation().getName().equals("JFWS") && id.getGlobalCMTID().equals(new GlobalCMTID("201205281150A"))
					
					|| id.getStation().getName().equals("SM28") && id.getGlobalCMTID().equals(new GlobalCMTID("201205281150A"))
					
					|| id.getStation().getName().equals("RSSD") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("MDND") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("ISCO") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("BOZ") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("BW06") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("K22A") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("ISCO") && id.getGlobalCMTID().equals(new GlobalCMTID("200610232100A"))
					|| id.getStation().getName().equals("WUAZ") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("MVCO") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("SRU") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("R11A") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("DUG") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("CMB") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("NEE2") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("GSC") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					|| id.getStation().getName().equals("GMR") && id.getGlobalCMTID().equals(new GlobalCMTID("201604132001A"))
					
					|| id.getStation().getName().equals("SS77") && id.getGlobalCMTID().equals(new GlobalCMTID("201302221201A"))
					|| id.getStation().getName().equals("SS80") && id.getGlobalCMTID().equals(new GlobalCMTID("201205281150A"))
					|| id.getStation().getName().equals("J38A") && id.getGlobalCMTID().equals(new GlobalCMTID("201109021347A"))
					|| id.getStation().getName().equals("K36A") && id.getGlobalCMTID().equals(new GlobalCMTID("201205280507A"))
					
					|| id.getStation().getName().equals("V03C") && id.getGlobalCMTID().equals(new GlobalCMTID("200610232100A"))
					|| id.getStation().getName().equals("H17A") && id.getGlobalCMTID().equals(new GlobalCMTID("201405140338A"))
					
					|| id.getStation().getName().equals("EYMN") && id.getGlobalCMTID().equals(new GlobalCMTID("200503211243A"))
					
					|| id.getStation().getName().equals("MVCO") && id.getGlobalCMTID().equals(new GlobalCMTID("200809031125A"))
					|| id.getStation().getName().equals("T19A") && id.getGlobalCMTID().equals(new GlobalCMTID("200809031125A"))
					|| id.getStation().getName().equals("S21A") && id.getGlobalCMTID().equals(new GlobalCMTID("200809031125A"))
					
//					|| id.getGlobalCMTID().equals(new GlobalCMTID("201205280507A"))
				)
					return false;
				
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
		
		//
		if (applyEventAmpCorr) {
			Path path = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_nostf_8-200s/corrections/df_evt_amp_ratio.csv");
			System.out.println("Using event amplitude corrections from " + path);
			HashMap<GlobalCMTID, Double> eventAmpCorr = new HashMap<GlobalCMTID, Double>();
			try {
				Files.readAllLines(path)
					.stream().forEach(line -> {
						if (line.contains("evt"))
							return;
						String[] ss = line.split(",");
						double y = 0;
						if (ss[2] == "")
							 y = 1.;
						else
							y = Double.parseDouble(ss[4]);
//						System.out.println(y);
						eventAmpCorr.put(new GlobalCMTID(ss[0]), y);
					});
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (int i = 0; i < ids.length; i++) {
				BasicID id = ids[i];
				if (id.getWaveformType().equals(WaveformType.OBS)) {
					double w = eventAmpCorr.get(id.getGlobalCMTID());
					double[] data_corr = Arrays.stream(id.getData()).map(d -> d / w).toArray();
					ids[i] = id.setData(data_corr);
				}
			}
			if (spcIds != null) {
				for (int i = 0; i < spcIds.length; i++) {
					BasicID id = spcIds[i];
					if (id.getWaveformType().equals(WaveformType.OBS)) {
						double w = eventAmpCorr.get(id.getGlobalCMTID());
						double[] data_corr = Arrays.stream(id.getData()).map(d -> d - Math.log(w)).toArray();
						spcIds[i] = id.setData(data_corr);
					}
				}
			}
		}
		
		if (correct3DFocusing) {
			System.out.println("Correcting for 3D focusing");
			for (int i = 0; i < ids.length; i++) {
				BasicID id = ids[i];
				if (id.getWaveformType().equals(WaveformType.OBS)) {
					EventCluster cluster = clusters.stream().filter(c -> c.getID().equals(id.getGlobalCMTID())).findFirst().get();
					int icluster = cluster.getIndex();
					
					double azimuth = Math.toDegrees(cluster.getCenterPosition().getAzimuth(id.getStation().getPosition()));
					if (azimuth < 180) azimuth += 360;
					double tmpw = 1.;
					
					if (icluster == 4) {
						if (azimuth < 323) tmpw = 0.96;
						else if (azimuth < 329) tmpw = 0.93;
						else if (azimuth < 336) tmpw = 0.89;
						else if (azimuth < 341) tmpw = 1.15;
						else if (azimuth < 347) tmpw = 0.88;
						else tmpw = 0.88;
					}
					else if (icluster == 3) {
						if (azimuth < 321) tmpw = 1.00;
						else if (azimuth < 327) tmpw = 1.00;
						else if (azimuth < 333) tmpw = 1.01;
						else if (azimuth < 339) tmpw = 0.86;
						else if (azimuth < 345) tmpw = 1.24;
						else if (azimuth < 351.6) tmpw = 0.86;
						else tmpw = 0.92;
					}
					else if (icluster == 5) {
						if (azimuth < 317.6) tmpw = 1.00;
						else if (azimuth < 323.3) tmpw = 1.00;
						else if (azimuth < 329.1) tmpw = 1.01;
						else if (azimuth < 334.9) tmpw = 0.86;
						else if (azimuth < 340.7) tmpw = 1.24;
						else if (azimuth < 347.2) tmpw = 0.86;
						else tmpw = 0.92;
					}
					
					double w = tmpw;
					
					double[] data_corr = Arrays.stream(id.getData()).map(d -> d / w).toArray();
					ids[i] = id.setData(data_corr);
				}
			}
			if (spcIds != null) {
				for (int i = 0; i < spcIds.length; i++) {
					BasicID id = spcIds[i];
					if (id.getWaveformType().equals(WaveformType.OBS)) {
						EventCluster cluster = clusters.stream().filter(c -> c.getID().equals(id.getGlobalCMTID())).findFirst().get();
						int icluster = cluster.getIndex();
						
						double azimuth = Math.toDegrees(cluster.getCenterPosition().getAzimuth(id.getStation().getPosition()));
						if (azimuth < 180) azimuth += 360;
						double tmpw = 1.;
						
						if (icluster == 4) {
							if (azimuth < 323) tmpw = 0.96;
							else if (azimuth < 329) tmpw = 0.93;
							else if (azimuth < 336) tmpw = 0.89;
							else if (azimuth < 341) tmpw = 1.15;
							else if (azimuth < 347) tmpw = 0.88;
							else tmpw = 0.88;
						}
						else if (icluster == 3) {
							if (azimuth < 321) tmpw = 1.00;
							else if (azimuth < 327) tmpw = 1.00;
							else if (azimuth < 333) tmpw = 1.01;
							else if (azimuth < 339) tmpw = 0.86;
							else if (azimuth < 345) tmpw = 1.24;
							else if (azimuth < 351.6) tmpw = 0.86;
							else tmpw = 0.92;
						}
						else if (icluster == 5) {
							if (azimuth < 317.6) tmpw = 1.00;
							else if (azimuth < 323.3) tmpw = 1.00;
							else if (azimuth < 329.1) tmpw = 1.01;
							else if (azimuth < 334.9) tmpw = 0.86;
							else if (azimuth < 340.7) tmpw = 1.24;
							else if (azimuth < 347.2) tmpw = 0.86;
							else tmpw = 0.92;
						}
						
						double w = tmpw;
						
						double[] data_corr = Arrays.stream(id.getData()).map(d -> d - Math.log(w)).toArray();
						spcIds[i] = id.setData(data_corr);
					}
				}
			}
		}
		
		// set Dvector
		System.err.println("Creating D vector");
		System.err.println("Going with weghting " + weightingType);
		Dvector dVector =  null;
		Dvector dVectorSpc = null;
		boolean atLeastThreeRecordsPerStation = time_receiver || time_source;
		double[] weighting = null;
		List<UnknownParameter> parameterForStructure = new ArrayList<>();
		switch (weightingType) {
		case RECIPROCAL:
		case RECIPROCAL_PcP:
		case RECIPROCAL_COS:
		case RECIPROCAL_CC:
		case RECIPROCAL_FREQ:
			dVector = new Dvector(ids, chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
			if (spcIds != null)
				dVectorSpc = new Dvector(spcIds, chooser, WeightingType.RECIPROCAL_FREQ, atLeastThreeRecordsPerStation, selectionInfo);
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
			PartialID[] partialIDs = PartialIDFile.read(partialIDPath, partialPath);
			
			PartialID[] partialSpcIDs = null;
			if (spcIds != null)
				partialSpcIDs = PartialIDFile.read(partialSpcIDPath, partialSpcPath);
			
			if (modelCovariance) {
				if (inverseMethods.contains(InverseMethodEnum.FAST_CONJUGATE_GRADIENT) || inverseMethods.contains(InverseMethodEnum.FAST_CONJUGATE_GRADIENT_DAMPED))
					eq = new ObservationEquation(partialIDs, parameterList, dVector, cm0, cmH, cmV, verticalMapping, false);
				else
					eq = new ObservationEquation(partialIDs, parameterList, dVector, cm0, cmH, cmV, verticalMapping);
			}
			else {
				if (inverseMethods.contains(InverseMethodEnum.FAST_CONJUGATE_GRADIENT) || inverseMethods.contains(InverseMethodEnum.FAST_CONJUGATE_GRADIENT_DAMPED))
					eq = new ObservationEquation(partialIDs, parameterList, dVector, time_source, time_receiver, combinationType, nUnknowns,
							unknownParameterWeightType, verticalMapping, false);
				else {
					eq = new ObservationEquation(partialIDs, parameterList, dVector);
					
					eqA = new ObservationEquation(partialIDs, parameterList, dVector.clone());
					
					if (spcIds != null) {
						System.out.println("Add spc equation");
						ObservationEquation eqSpc = new ObservationEquation(partialSpcIDs, parameterList, dVectorSpc);
						eqB = new ObservationEquation(partialSpcIDs, parameterList, dVectorSpc.clone());
						
						double d = eq.getDiagonalOfAtA().getL1Norm();
						double dSpc = eqSpc.getDiagonalOfAtA().getL1Norm();
						
						System.out.println("d/dSpc=" + (d/dSpc));
						
						mul = scale_freq_ata * d / dSpc;
						
						double varA_over_varB = eqA.getDVector().getVariance() * eqA.getDVector().getObsNormSquare() 
								/ (eqB.getDVector().getVariance() * eqB.getDVector().getObsNormSquare());
						
						mul = scale_freq_ata * varA_over_varB;
						System.out.println("mul=" + mul);
						
						eqSpc = eqSpc.scalarMultiply(mul);
						
						eq = eq.add(eqSpc);
						
						eqB = eqB.scalarMultiply(mul);
						eqB.setObsNormSquare(eqA.getDVector().getObsNormSquare());
					}
				}
			}
			
			if (conditioner)
				applyConditionner();
			
			if (regularizationMuQ) {
				addRegularizationVSQ();
			}
			
			if (eqA != null && eqB != null) {
				System.out.println("eqA variance = " + eqA.getDVector().getVariance());
				System.out.println("eqB variance = " + eqB.getDVector().getVariance());
				System.out.println("eqA obs2 = " + eqA.getDVector().getObsNormSquare());
				System.out.println("eqB obs2 = " + eqB.getDVector().getObsNormSquare());
			}
			
		}
		else {
			Matrix atatmp = new Matrix(parameterList.size(), parameterList.size());
			RealVector atdtmp = new ArrayRealVector(parameterList.size());
			
			int nIDPerStep = ((int) (ids.length / 2 / nStepsForLowMemoryMode)) * 2;
			int nIDLastStep = ids.length - nIDPerStep * nStepsForLowMemoryMode + nIDPerStep;
			
			// read ids headers only
			PartialID[] partialIDsNoData = PartialIDFile.read(partialIDPath);
			
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
				
				partialIDs = PartialIDFile.read(partialIDs, partialPath, partialIndexes, cumulativeNPTS);
				
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
	
	/**
	 * @author anselme
	 * TODO check if still needed
	 */
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
	
	/**
	 * @author anselme
	 */
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
		
		double normMU = Math.sqrt(new ArrayRealVector(IntStream.range(0, eqA.getMlength())
				.filter(i -> eqA.getParameterList().get(i).getPartialType().equals(PartialType.PARVS))
				.mapToDouble(i -> eqA.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm());
//				 / eqA.getParameterList().stream().filter(p -> p.getPartialType().equals(PartialType.PARVS)).count();
		double normQ = Math.sqrt(new ArrayRealVector(IntStream.range(0, eqA.getMlength())
				.filter(i -> eqA.getParameterList().get(i).getPartialType().equals(PartialType.PARQ))
				.mapToDouble(i -> eqA.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm());
//				 / eqA.getParameterList().stream().filter(p -> p.getPartialType().equals(PartialType.PARQ)).count();
		double norm00 = Math.sqrt(new ArrayRealVector(IntStream.range(0, eqA.getMlength())
				.filter(i -> eqA.getParameterList().get(i).getPartialType().equals(PartialType.PAR00))
				.mapToDouble(i -> eqA.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm());
//				 / eqA.getParameterList().stream().filter(p -> p.getPartialType().equals(PartialType.PAR00)).count();
		double normVP = Math.sqrt(new ArrayRealVector(IntStream.range(0, eqA.getMlength())
				.filter(i -> eqA.getParameterList().get(i).getPartialType().equals(PartialType.PARVP))
				.mapToDouble(i -> eqA.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm());
//				 / eqA.getParameterList().stream().filter(p -> p.getPartialType().equals(PartialType.PARVP)).count();
		
		System.out.printf("Regularization: norm_vs=%.3e, norm_q=%.3e\n", normMU, normQ);
		
		// Second order differential operator
		List<Double> coeffs = new ArrayList<>();
		if (usedTypes.contains(PartialType.PARVS)) {
			double coeff = lambdaMU * normMU;
			if (dataErrorMap != null)
				coeff *= dataErrorMap.get(PartialType.PARVS);
			coeffs.add(coeff);
		}
		if (usedTypes.contains(PartialType.PARQ)) {
			double coeff = lambdaQ * normQ;
			if (dataErrorMap != null)
				coeff *= dataErrorMap.get(PartialType.PARQ);
			coeffs.add(coeff);
		}
		if(usedTypes.contains(PartialType.PAR00))
			coeffs.add(lambda00 * norm00);
		if(usedTypes.contains(PartialType.PARVP))
			coeffs.add(lambdaVp * normVP);
		
		RadialSecondOrderDifferentialOperator D2 = new RadialSecondOrderDifferentialOperator(eq.getParameterList(), types, coeffs);
		RealMatrix D2tD2 = D2.getD2TD2();
		eq.addRegularization(D2tD2);
//		eqA.addRegularization(D2tD2);
//		eqB.addRegularization(D2tD2);
		
		// Diagonal matrix
		coeffs = new ArrayList<>();
		if (usedTypes.contains(PartialType.PARVS)) {
			double coeff = gammaMU * normMU;
			if (dataErrorMap != null)
				coeff *= dataErrorMap.get(PartialType.PARVS);
			coeffs.add(coeff);
		}
		if (usedTypes.contains(PartialType.PARQ)) {
			double coeff = gammaQ * normQ;
			if (dataErrorMap != null)
				coeff *= dataErrorMap.get(PartialType.PARQ);
			coeffs.add(coeff);
		}
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
		
		double anchor_factor = 8*8;
		
		PartialType tmptype = eq.getParameterList().get(0).getPartialType();
		for (int i = 1; i < eq.getMlength(); i++) {
			if (!tmptype.equals(eq.getParameterList().get(i).getPartialType())) {
				D.multiplyEntry(i - 1, i - 1, anchor_factor); // 9
				System.out.println(i + " " + tmptype + " " + eq.getParameterList().get(i).getPartialType());
				tmptype = eq.getParameterList().get(i).getPartialType();
			}
		}
		D.multiplyEntry(eq.getMlength()-1, eq.getMlength()-1, anchor_factor);
		
		eq.addRegularization(D);
//		eqA.addRegularization(D);
//		eqB.addRegularization(D);
	}
	
	/**
	 * @author anselme
	 * TODO check if still needed
	 */
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
	
	/**
	 * @author anselme
	 * TODO check if still needed
	 */
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
	
	/**
	 * @author anselme
	 * TODO check if still needed
	 */
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
	
	/**
	 * @author anselme
	 * TODO check if still needed
	 */
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
			UnknownParameterFile.write(outPath.resolve("unknownParameterOrder.inf"), eq.getParameterList());
			UnknownParameterFile.write(outPath.resolve("originalUnknownParameterOrder.inf"), eq.getOriginalParameterList());
			eq.outputA(outPath.resolve("partial"));
//			eq.outputAtA(outPath.resolve("lmi_AtA.inf"));
			eq.outputUnkownParameterWeigths(outPath.resolve("unknownParameterWeigths.inf"));
			dVector.outWeighting(outPath);
			
			if (eqB != null) {
				eqB.outputA(outPath.resolve("partial_spc"));
				outEachTrace(outPath.resolve("trace_spc"), eqB.getDVector());
			}
			
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
			if (PROPERTY != null)
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
     * @param outPath {@link Path} for write folder
     * @throws IOException if an I/O error occurs
     */
	public void outEachTrace(Path outpath) throws IOException {
		outEachTrace(outpath, eq.getDVector());
	}
	
	/**
	 * @param outPath
	 * @param d
	 * @throws IOException
	 * @author anselme
	 */
	public void outEachTrace(Path outPath, Dvector d) throws IOException {
		if (Files.exists(outPath))
			throw new FileAlreadyExistsException(outPath.toString());
		Files.createDirectories(outPath);

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
     * @param outPath {@link File} for write
     * @param vectors {@link RealVector}s for write
     * @throws IOException if an I/O error occurs
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
	
	private void solve(Path outPath, InverseProblem inverseProblem) throws IOException {
		// invOutDir.mkdir();
		inverseProblem.compute();
		Files.createDirectories(outPath);
		outVariance(outPath, inverseProblem);
		outVariancePerEvents(outPath, inverseProblem);
		outLcurveEntry(inverseProblem);
		
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

	/**
	 * @author anselme
	 */
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

	/**
	 * @param outPath
	 * @param inverseProblem
	 * @throws IOException
	 * @author anselme
	 */
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
	
	/**
	 * @param inverseProblem
	 * @author anselme
	 */
	private void computeDeltaM(InverseProblem inverseProblem) {
		int n = eq.getMlength();//Math.max(20, eq.getMlength());
		ModelCovarianceMatrix cm = eq.getCm();
		RealMatrix l = cm.getL();
		for (int i = 1; i <= n; i++) {
			RealVector deltaM = l.operate(inverseProblem.getAns(i));
			inverseProblem.setANS(i, deltaM);
		}
	}
	
	/**
	 * @param inverseProblem
	 * @author anselme
	 */
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
	 * @param inverse
	 * @throws IOException
	 * @author anselme
	 */
	private void outLcurveEntry(InverseProblem inverse) throws IOException {
//		if (eqA == null || eqB == null)
//			return;
		System.out.println("Outputting L-curve entry");
		Path out = outPath.resolve("lcurve.txt");
		if (Files.exists(out))
			throw new FileAlreadyExistsException(out.toString());
		int m = inverse.getParN();
		
		double varA = 0;
		double varB = 0;
		double varA0 = 0;
		double varB0 = 0;
		
		if (eqA != null) {
			varA = eqA.varianceOf(inverse.getANS().getColumnVector(m - 1));
			varA0 = eqA.getDVector().getVariance();
		}
		if (eqB != null) {
//			varB = eqB.varianceOf(inverse.getANS().getColumnVector(m - 1));
//			varB0 = eqB.getDVector().getVariance();
			varB = eqB.varianceOf(inverse.getANS().getColumnVector(m - 1)) * eqB.getDVector().getObsNormSquare() / eqA.getDVector().getObsNormSquare() * mul;
			varB0 = eqB.getDVector().getVariance() * eqB.getDVector().getObsNormSquare() / eqA.getDVector().getObsNormSquare() * mul;
		}
		double var = eq.varianceOf(inverse.getANS().getColumnVector(m - 1));
		double solutionQLinfNorm = inverse.getANS().getColumnVector(m - 1).getSubVector(m/2, m/2).getLInfNorm();
		double solutionQL2Norm = inverse.getANS().getColumnVector(m - 1).getSubVector(m/2, m/2).getNorm();
		double solutionVLinfNorm = inverse.getANS().getColumnVector(m - 1).getSubVector(0, m/2).getLInfNorm();
		double solutionVL2Norm = inverse.getANS().getColumnVector(m - 1).getSubVector(0, m/2).getNorm();
		try (PrintWriter pw = new PrintWriter(out.toFile())) {
			pw.println("#varA varB var L2_deltaQ Linf_deltaQ L2_deltaV Linf_deltaV");
			pw.print(varA + " " + varB + " " + var + " " + solutionQL2Norm + " " + solutionQLinfNorm + " " + solutionVL2Norm + " " + solutionVLinfNorm 
					+ " " + varA0 + " " + varB0);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		if (eqA != null && eqB != null) {
//			variance[0] = 2. / (1 + mul) * (eqA.getDVector().getVariance() + eqB.getDVector().getVariance());
//			double var_B = eqB.getDVector().getVariance() * eqB.getDVector().getObsNormSquare() / eqA.getDVector().getObsNormSquare() * mul;
			double var_B = eqB.getDVector().getVariance();
			variance[0] = eqA.getDVector().getVariance() + var_B;
		}
		else
			variance[0] = eq.getDVector().getVariance();
		int tmpN = eq.getMlength();
//		for (int i = 0; i < eq.getMlength(); i++)
		if (eqA != null && eqB != null) {
			for (int i = 0; i < tmpN; i++) {
//				variance[i + 1] =  2. / (1 + mul) * (eqA.varianceOf(inverse.getANS().getColumnVector(i)) 
//					+ eqB.varianceOf(inverse.getANS().getColumnVector(i)));
//				double var_B = eqB.varianceOf(inverse.getANS().getColumnVector(i)) * eqB.getDVector().getObsNormSquare() / eqA.getDVector().getObsNormSquare() * mul;
				double var_B = eqB.varianceOf(inverse.getANS().getColumnVector(i));
				variance[i + 1] = eqA.varianceOf(inverse.getANS().getColumnVector(i)) + var_B;
			}
		}
		else {
			for (int i = 0; i < tmpN; i++)
				variance[i + 1] = eq.varianceOf(inverse.getANS().getColumnVector(i));
		}
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
	
	/**
	 * @param outPath
	 * @param inverse
	 * @throws IOException
	 * @author anselme
	 */
	private void outVariancePerEvents(Path outPath, InverseProblem inverse) throws IOException {
		if (eq.getA() == null) {
			System.out.println("a is null, cannot output variance per event");
			return;
		}
		
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
				double GCMTMw = id.getEvent().getCmt().getMw();
				
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
     * @param variance varianceの列
     * @param alpha    alpha redundancy
     * @return array of aic
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
     * @param outPath {@link File} for write
     * @throws IOException if an I/O error occurs
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

	/**
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	private RealVector readCheckerboardPerturbationVector() throws IOException {
		return new ArrayRealVector(Files.readAllLines(checkerboardPerturbationPath).stream().mapToDouble(s -> Double.parseDouble(s.trim())).toArray());
	}
	
	@Override
	public Path getWorkPath() {
		return workPath;
	}

	@Override
	public Properties getProperties() {
		return (Properties) PROPERTY.clone();
	}
	
}
