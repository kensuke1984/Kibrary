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
import java.util.Collections;
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
import java.util.function.BiPredicate;
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
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
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
import io.github.kensuke1984.kibrary.util.addons.EventCluster;
import io.github.kensuke1984.kibrary.util.addons.FrequencyRange;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTCatalog;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.globalcmt.NDK;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtAEntry;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtAFile;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtdEntry;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtdFile;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;
import opendap.servlet.GetAsciiHandler;

/**
 * 
 * Let's invert
 * 
 * @version 2.0.3.3
 * 
 * @author Kensuke Konishi
 * 
 */
public class LetMeInvertCV implements Operation {
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
	
	Path spcAmpPath;
	
	Path spcAmpIDPath;
	
	Path partialSpcPath;
	
	Path partialSpcIDPath;
	
	private double scale_freq_ata;
	
	double minSNRatio;

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
		if (!property.containsKey("scale_freq_ata"))
			property.setProperty("scale_freq_ata", "1.");
		if (!property.containsKey("applyEventAmpCorr"))
			property.setProperty("applyEventAmpCorr", "false");
		if (!property.containsKey("correct3DFocusing"))
			property.setProperty("correct3DFocusing", "false");
		
		// additional unused info
		property.setProperty("CMTcatalogue", GlobalCMTCatalog.getCatalogPath().toString());
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
			clusterIndex = Arrays.stream(property.getProperty("clusterIndex").trim().split(" ")).mapToInt(Integer::parseInt).toArray();
			azimuthIndex = Arrays.stream(property.getProperty("azimuthIndex").trim().split(" ")).mapToInt(Integer::parseInt).toArray();
			System.out.println("Using cluster file with clusterIndex=" + clusterIndex[0] + " and azimuthIndex=" + azimuthIndex[0]);
		}
		
		try {
			partialSpcPath = getPath("partialSpcPath");
			partialSpcIDPath = getPath("partialSpcIDPath");
		
			spcAmpPath = getPath("spcAmpPath");
			spcAmpIDPath = getPath("spcAmpIDPath");
		} catch (Exception e) {
			
		}
		
		scale_freq_ata = Double.parseDouble(property.getProperty("scale_freq_ata"));
		
		dataErrorMap = null;
		
		applyEventAmpCorr = Boolean.parseBoolean(property.getProperty("applyEventAmpCorr"));
		
		correct3DFocusing = Boolean.parseBoolean(property.getProperty("correct3DFocusing"));
		
		if (property.containsKey("signalNoiseRatioFile")) {
			signalNoiseRatioFile = Paths.get(property.getProperty("signalNoiseRatioFile"));
			try {
				signalNoiseRatios = StaticCorrectionFile.read(signalNoiseRatioFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			minSNRatio = Double.parseDouble(property.getProperty("minSNRatio"));
			
			System.out.println("Filtering signal noise ratio smaller than " + minSNRatio);
		}
	}

	/**
	 * AIC計算に用いるα 独立データ数はn/αと考える
	 */
	protected double[] alpha;

	private Properties property;
	
	private boolean regularizationMuQ;

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(LetMeInvertCV.class.getName() + Utilities.getTemporaryString() + ".properties");
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
			pw.println("#correct3DFocusing");
			pw.println("#applyEventAmpCorr");
			pw.println("#signalNoiseRatioFile");
			pw.println("#minSNRatio 3");
		}
		System.err.println(outPath + " is created.");
	}

	private Path workPath;

	public LetMeInvertCV(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
		if (!canGO())
			throw new RuntimeException();
	}

	public LetMeInvertCV(Path workPath, Set<Station> stationSet, ObservationEquation equation) throws IOException {
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
	
	private int[] azimuthIndex;
	
	private int[] clusterIndex;
	
	private ObservationEquation eqA, eqB;
	
	private boolean applyEventAmpCorr;
	
	private boolean correct3DFocusing;
	
	private Path signalNoiseRatioFile;
	
	Set<StaticCorrection> signalNoiseRatios;
	
	List<UnknownParameter> parameterList;
	
	PartialID[] partialIDs;
	
	PartialID[] partialSpcIDs;
	
	Dvector dVectorSpc;
	
	Dvector dVector;
	
	BasicID[] ids;
	
	BasicID[] spcIds;
	
	private void readIDs() {
		try {
			ids = BasicIDFile.read(waveformIDPath, waveformPath);
			
			spcIds = null;
			if (spcAmpIDPath != null)
				spcIds = BasicIDFile.read(spcAmpIDPath, spcAmpPath);
			
			if (eventClusterPath != null)
				clusters = EventCluster.readClusterFile(eventClusterPath);
			
			processIDs();
			
			// set unknown parameter
			System.err.println("setting up unknown parameter set");
			parameterList = UnknownParameterFile.read(unknownParameterListPath);
			
			// read all partial IDs
			partialIDs = PartialIDFile.read(partialIDPath, partialPath);
			
			partialSpcIDs = null;
			if (spcIds != null)
				partialSpcIDs = PartialIDFile.read(partialSpcIDPath, partialSpcPath);
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void processIDs() {
		chooser = null;
		
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
			System.out.println("Setting chooser for well defined events");
//			System.out.println("DEBUG1: " + minDistance + " " + maxDistance + " " + minMw + " " + maxMw);
			
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
			
//			for (int azindex : azimuthIndex)
//				if (azindex > thisCluster.get(0).getAzimuthSlices().size()) {
//					System.out.println("No azimuth slice " + azimuthIndex + " for cluster " + clusterIndex);
//					System.exit(0);
//				}
			
			
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
//					
//					|| id.getStation().getName().equals("MG15") && id.getGlobalCMTID().equals(new GlobalCMTID("201109021347A"))
//					|| id.getStation().getName().equals("SM17") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
//					|| id.getStation().getName().equals("SS78") && id.getGlobalCMTID().equals(new GlobalCMTID("201205281150A"))
//					|| id.getStation().getName().equals("SN62") && id.getGlobalCMTID().equals(new GlobalCMTID("201205280507A"))
//					|| id.getStation().getName().equals("SS65") && id.getGlobalCMTID().equals(new GlobalCMTID("201302221201A"))
//					|| id.getStation().getName().equals("L40A") && id.getGlobalCMTID().equals(new GlobalCMTID("201302221201A"))
//					|| id.getStation().getName().equals("R42A") && id.getGlobalCMTID().equals(new GlobalCMTID("201302221201A"))
//					|| id.getStation().getName().equals("M37A") && id.getGlobalCMTID().equals(new GlobalCMTID("201104170158A"))
//					|| id.getStation().getName().equals("LB18") && id.getGlobalCMTID().equals(new GlobalCMTID("201302221201A"))
//					|| id.getStation().getName().equals("MG05") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
//					|| id.getStation().getName().equals("Q40A") && id.getGlobalCMTID().equals(new GlobalCMTID("201104170158A"))
//					|| id.getStation().getName().equals("SS68") && id.getGlobalCMTID().equals(new GlobalCMTID("201205280507A"))
//					
//					|| id.getStation().getName().equals("N40A") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
//					|| id.getStation().getName().equals("Q46A") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
//					|| id.getStation().getName().equals("MG03") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
//					|| id.getStation().getName().equals("K40A") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
//					|| id.getStation().getName().equals("R41A") && id.getGlobalCMTID().equals(new GlobalCMTID("201205280507A"))
//					|| id.getStation().getName().equals("S41A") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
//					|| id.getStation().getName().equals("SCIA") && id.getGlobalCMTID().equals(new GlobalCMTID("201205280507A"))
//					|| id.getStation().getName().equals("CCM") && id.getGlobalCMTID().equals(new GlobalCMTID("201205281150A"))
//					|| id.getStation().getName().equals("SM38") && id.getGlobalCMTID().equals(new GlobalCMTID("201302221201A"))
//					|| id.getStation().getName().equals("JFWS") && id.getGlobalCMTID().equals(new GlobalCMTID("201104170158A"))
//					|| id.getStation().getName().equals("P39B") && id.getGlobalCMTID().equals(new GlobalCMTID("201205281150A"))
//					|| id.getStation().getName().equals("L36A") && id.getGlobalCMTID().equals(new GlobalCMTID("201109021347A"))
//					|| id.getStation().getName().equals("R44A") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
//					|| id.getStation().getName().equals("SM41") && id.getGlobalCMTID().equals(new GlobalCMTID("201205281150A"))
//					|| id.getStation().getName().equals("SS76") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
//					|| id.getStation().getName().equals("MG15") && id.getGlobalCMTID().equals(new GlobalCMTID("201205280507A"))
//					|| id.getStation().getName().equals("SS78") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
//					|| id.getStation().getName().equals("H39A") && id.getGlobalCMTID().equals(new GlobalCMTID("201205280507A"))
//					|| id.getStation().getName().equals("SS80") && id.getGlobalCMTID().equals(new GlobalCMTID("201302221201A"))
//					|| id.getStation().getName().equals("MG07") && id.getGlobalCMTID().equals(new GlobalCMTID("201205280507A"))
//					|| id.getStation().getName().equals("LD18") && id.getGlobalCMTID().equals(new GlobalCMTID("201302221201A"))
//					|| id.getStation().getName().equals("I40A") && id.getGlobalCMTID().equals(new GlobalCMTID("201109021347A"))
//					|| id.getStation().getName().equals("K41A") && id.getGlobalCMTID().equals(new GlobalCMTID("201302221201A"))
					
//					|| id.getStation().getName().equals("SS78") && id.getGlobalCMTID().equals(new GlobalCMTID("201205280507A"))
					
//					|| id.getStation().getName().equals("SM18") && id.getGlobalCMTID().equals(new GlobalCMTID("201205281150A"))
//					|| id.getStation().getName().equals("Q47A") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
//					|| id.getStation().getName().equals("I38A") && id.getGlobalCMTID().equals(new GlobalCMTID("201104170158A"))
//					|| id.getStation().getName().equals("P44A") && id.getGlobalCMTID().equals(new GlobalCMTID("201205280507A"))
//					|| id.getStation().getName().equals("M40A") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
//					|| id.getStation().getName().equals("SS70") && id.getGlobalCMTID().equals(new GlobalCMTID("201203050746A"))
					
					|| id.getGlobalCMTID().equals(new GlobalCMTID("201205280507A"))
				)
					return false;
				
				return true;
			};
			
			chooser_snratio = id -> {
				if (!chooser.test(id)) return false;
				
				// SN ratio
				if (signalNoiseRatioFile != null) {
					double snRatio = signalNoiseRatios.parallelStream().filter(s ->
							s.getGlobalCMTID().equals(id.getGlobalCMTID())
							&& s.getStation().equals(id.getStation())
							&& s.getComponent().equals(id.getSacComponent()))
						.findFirst().get().getAmplitudeRatio();
					if (snRatio < minSNRatio) return false;
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
		
		//
		if (applyEventAmpCorr) {
//			Path path = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_nostf_8-200s/corrections/df_evt_amp_ratio.csv");
			Path path = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/STF/syntheticPREM_Q165/filtered_stf_6-200s/amp_corr_inv.txt");
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
		
		//
		boolean subtract1D = true;
		if (correct3DFocusing) {
			double alpha = 1.16; // 1.15
			System.out.println("Correcting for 3D focusing alpha=" + alpha);
			for (int i = 0; i < ids.length; i++) {
				BasicID id = ids[i];
				if (id.getWaveformType().equals(WaveformType.OBS)) {
					EventCluster cluster = clusters.stream().filter(c -> c.getID().equals(id.getGlobalCMTID())).findFirst().get();
					int icluster = cluster.getIndex();
					
					double azimuth = Math.toDegrees(cluster.getCenterPosition().getAzimuth(id.getStation().getPosition()));
					if (azimuth < 180) azimuth += 360;
					double tmpw = 1.;
					
					if (subtract1D) {
						if (icluster == 4) {
							if (azimuth < 323) tmpw = 1.0;
							else if (azimuth < 329) tmpw = 1.0; 
							else if (azimuth < 336) tmpw = 0.95; 
							else if (azimuth < 341) tmpw = 1.16 * alpha/1.16;
							else if (azimuth < 347) tmpw = 0.94; 
							else tmpw = 0.98; 
						}
						else if (icluster == 3) {
							if (azimuth < 321) tmpw = 1.07; 
							else if (azimuth < 327) tmpw = 1.07;
							else if (azimuth < 333) tmpw = 1.07; 
							else if (azimuth < 339) tmpw = 0.89; // 0.88
							else if (azimuth < 345) tmpw = 1.25;
							else if (azimuth < 351.6) tmpw = 0.90;
							else tmpw = 1.02;
						}
						else if (icluster == 5) {
							if (azimuth < 317.6) tmpw = 1.07;
							else if (azimuth < 323.3) tmpw = 1.07;
							else if (azimuth < 329.1) tmpw = 1.07;
							else if (azimuth < 334.9) tmpw = 0.89;
							else if (azimuth < 340.7) tmpw = 1.25;
							else if (azimuth < 347.2) tmpw = 0.90;
							else tmpw = 1.02;
						}
					}
					else {
						if (icluster == 4) {
							if (azimuth < 323) tmpw = 0.96; //1.03
							else if (azimuth < 329) tmpw = 0.93; //1.
							else if (azimuth < 336) tmpw = 0.93; // 0.89 measured on synthetics, but most likely too low //0.96
							else if (azimuth < 341) tmpw = 1.15 * alpha/1.15;
							else if (azimuth < 347) tmpw = 0.93; // 0.88 measured on synthetics, but most likely too low //0.95
							else tmpw = 0.93; // 0.88 measured on synthetics, but most likely to0 low //0.95
						}
						else if (icluster == 3) {
							if (azimuth < 321) tmpw = 1.00; //1.08
							else if (azimuth < 327) tmpw = 1.00; //1.08
							else if (azimuth < 333) tmpw = 1.01; //1.08
							else if (azimuth < 339) tmpw = 0.93; // 0.86 measured on synthetics, but most likely too low //0.92
							else if (azimuth < 345) tmpw = 1.24;
							else if (azimuth < 351.6) tmpw = 0.93; // 0.86 measured on synthetics, but most likely too low //0.92
							else tmpw = 0.92; //0.99
						}
						else if (icluster == 5) {
							if (azimuth < 317.6) tmpw = 1.00;
							else if (azimuth < 323.3) tmpw = 1.00;
							else if (azimuth < 329.1) tmpw = 1.01;
							else if (azimuth < 334.9) tmpw = 0.93; // 0.86 measured on synthetics, but most likely too low
							else if (azimuth < 340.7) tmpw = 1.24;
							else if (azimuth < 347.2) tmpw = 0.93; // 0.86 measured on synthetics, but most likely too low
							else tmpw = 0.92;
						}
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
						
						if (subtract1D) {
							if (icluster == 4) {
								if (azimuth < 323) tmpw = 1.0;
								else if (azimuth < 329) tmpw = 1.0; 
								else if (azimuth < 336) tmpw = 0.95; 
								else if (azimuth < 341) tmpw = 1.16 * alpha/1.16;
								else if (azimuth < 347) tmpw = 0.94; 
								else tmpw = 0.98; 
							}
							else if (icluster == 3) {
								if (azimuth < 321) tmpw = 1.07; 
								else if (azimuth < 327) tmpw = 1.07;
								else if (azimuth < 333) tmpw = 1.07; 
								else if (azimuth < 339) tmpw = 0.89; // 0.88
								else if (azimuth < 345) tmpw = 1.25;
								else if (azimuth < 351.6) tmpw = 0.90;
								else tmpw = 1.02;
							}
							else if (icluster == 5) {
								if (azimuth < 317.6) tmpw = 1.07;
								else if (azimuth < 323.3) tmpw = 1.07;
								else if (azimuth < 329.1) tmpw = 1.07;
								else if (azimuth < 334.9) tmpw = 0.89;
								else if (azimuth < 340.7) tmpw = 1.25;
								else if (azimuth < 347.2) tmpw = 0.90;
								else tmpw = 1.02;
							}
						}
						else {
							if (icluster == 4) {
								if (azimuth < 323) tmpw = 0.96; //1.03
								else if (azimuth < 329) tmpw = 0.93; //1.
								else if (azimuth < 336) tmpw = 0.93; // 0.89 measured on synthetics, but most likely too low //0.96
								else if (azimuth < 341) tmpw = 1.15 * alpha/1.15;
								else if (azimuth < 347) tmpw = 0.93; // 0.88 measured on synthetics, but most likely too low //0.95
								else tmpw = 0.93; // 0.88 measured on synthetics, but most likely to low //0.95
							}
							else if (icluster == 3) {
								if (azimuth < 321) tmpw = 1.00; //1.08
								else if (azimuth < 327) tmpw = 1.00; //1.08
								else if (azimuth < 333) tmpw = 1.01; //1.08
								else if (azimuth < 339) tmpw = 0.93; // 0.86 measured on synthetics, but most likely too low //0.92
								else if (azimuth < 345) tmpw = 1.24;
								else if (azimuth < 351.6) tmpw = 0.93; // 0.86 measured on synthetics, but most likely too low //0.92
								else tmpw = 0.92; //0.99
							}
							else if (icluster == 5) {
								if (azimuth < 317.6) tmpw = 1.00;
								else if (azimuth < 323.3) tmpw = 1.00;
								else if (azimuth < 329.1) tmpw = 1.01;
								else if (azimuth < 334.9) tmpw = 0.93; // 0.86 measured on synthetics, but most likely too low
								else if (azimuth < 340.7) tmpw = 1.24;
								else if (azimuth < 347.2) tmpw = 0.93; // 0.86 measured on synthetics, but most likely too low
								else tmpw = 0.92;
							}
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
		dVector =  null;
		dVectorSpc = null;
		boolean atLeastThreeRecordsPerStation = time_receiver || time_source;
		double[] weighting = null;
		List<UnknownParameter> parameterForStructure = new ArrayList<>();
		switch (weightingType) {
//				case LOWERUPPERMANTLE:
//					double[] lowerUpperMantleWeighting = Weighting.LowerUpperMantle1D(partialIDs);
//					dVector = new Dvector(ids, chooser, weightingType, lowerUpperMantleWeighting, atLeastThreeRecordsPerStation);
//					break;
		case RECIPROCAL:
		case RECIPROCAL_PcP:
		case RECIPROCAL_COS:
		case RECIPROCAL_CC:
		case RECIPROCAL_FREQ:
//					System.out.println(selectionInfo.size());
//					System.out.println(selectionInfo.get(0).getTimewindow());
			dVector = new Dvector(ids, chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
			if (spcIds != null)
				dVectorSpc = new Dvector(spcIds, chooser_snratio, WeightingType.RECIPROCAL_FREQ, atLeastThreeRecordsPerStation, selectionInfo);
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
//				case TAKEUCHIKOBAYASHI:
//					dVector = new Dvector(ids, chooser, WeightingType.IDENTITY, atLeastThreeRecordsPerStation, selectionInfo);
////					System.out.println(dVector.getObs().getLInfNorm() + " " + dVector.getSyn().getLInfNorm());
//					parameterForStructure = parameterList.stream()
//							.filter(unknonw -> unknonw.getPartialType().equals(PartialType.PAR2))
//							.collect(Collectors.toList());
////					double[] weighting = Weighting.CG(partialIDs, parameterForStructure, dVector, gamma);
//					weighting = Weighting.TakeuchiKobayashi1D(partialIDs, parameterForStructure, dVector, gamma);
//					dVector = new Dvector(ids, chooser, weightingType, weighting, atLeastThreeRecordsPerStation);
//					break;
//				case FINAL:
//					dVector = new Dvector(ids, chooser, WeightingType.IDENTITY, atLeastThreeRecordsPerStation, selectionInfo);
//					parameterForStructure = parameterList.stream()
//							.filter(unknonw -> unknonw.getPartialType().equals(PartialType.PAR2))
//							.collect(Collectors.toList());
//					weighting = Weighting.TakeuchiKobayashi1D(partialIDs, parameterForStructure, dVector, gamma);
//					dVector = new Dvector(ids, chooser, weightingType, weighting, atLeastThreeRecordsPerStation);
//					break;
		case IDENTITY:
			dVector = new Dvector(ids, chooser, WeightingType.IDENTITY, atLeastThreeRecordsPerStation, selectionInfo);
			break;
		default:
			throw new RuntimeException("Error: Weighting should be LOWERUPPERMANTLE, RECIPROCAL, TAKEUCHIKOBAYASHI, IDENTITY, or FINAL");
		}
		
		if (trimWindow)
			dVector.trimWindow(trimPoint, keepBefore);
	}
	
	Predicate<BasicID> chooser;
	
	Predicate<BasicID> chooser_snratio;
	
	Integer[] shuffle;
	
	private Dvector[][] trainTestSplit(int cv, Dvector dVector, double testSize, int randomSeed, boolean isSpc, boolean sameShuffle) {
		Dvector[][] dVectors = new Dvector[cv][2];
		
		Integer[] arr = new Integer[dVector.getNTimeWindow()];
		for (int i = 0; i < arr.length; i++)
			arr[i] = i;
		
		if (!sameShuffle) {
			Collections.shuffle(Arrays.asList(arr));
			shuffle = arr;
		}
		else
			arr = shuffle;

		int nTrain = (int) (dVector.getNTimeWindow() * (1 - testSize));
		int nTest = dVector.getNTimeWindow() - nTrain;
		
		for (int icv = 0; icv < cv; icv++) {
			BasicID[] ids_train = new BasicID[nTrain * 2];
			BasicID[] ids_test = new BasicID[nTest * 2];
			
			BasicID[] obsIds = dVector.getObsIDs();
			BasicID[] synIds = dVector.getSynIDs();
			
			int itest0 = icv * nTest;
			int itest1 = (icv + 1) * nTest;
			
			int j = 0;
			for (int i = 0; i < dVector.getNTimeWindow(); i++) {
				if (i >= itest0 && i < itest1) {
					ids_test[2*(i-itest0)] = obsIds[arr[i]];
					ids_test[2*(i-itest0)+1] = synIds[arr[i]];
				}
				else {
					ids_train[2*j] = obsIds[arr[i]];
					ids_train[2*j+1] = synIds[arr[i]];
					j++;
				}
			}
			
			Predicate<BasicID> chooser = id -> true;
			
			boolean atLeastThreeRecordsPerStation = false;
			switch (weightingType) {
			case RECIPROCAL:
			case RECIPROCAL_PcP:
			case RECIPROCAL_COS:
			case RECIPROCAL_CC:
			case RECIPROCAL_FREQ:
				if (!isSpc) {
					dVectors[icv][0] = new Dvector(ids_train, chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
					dVectors[icv][1] = new Dvector(ids_test, chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
				}
				else {
					dVectors[icv][0] = new Dvector(ids_train, chooser, WeightingType.RECIPROCAL_FREQ, atLeastThreeRecordsPerStation, selectionInfo);
					dVectors[icv][1] = new Dvector(ids_test, chooser, WeightingType.RECIPROCAL_FREQ, atLeastThreeRecordsPerStation, selectionInfo);
				}
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
		}
		
		return dVectors;
	}
	
	private Dvector[][] trainTestSplitCV(int cv, Dvector dVector, int randomSeed, boolean isSpc, boolean sameShuffle) {
		Dvector[][] dVectors = new Dvector[cv][2];
		
		if (cv == 1) {
			dVectors[0][0] = dVector.clone();
			dVectors[0][1] = dVector.clone();
			return dVectors;
		}
		
		Integer[] arr = new Integer[dVector.getNTimeWindow()];
		for (int i = 0; i < arr.length; i++)
			arr[i] = i;
		
		if (!sameShuffle) {
			Collections.shuffle(Arrays.asList(arr));
			shuffle = arr;
		}
		else
			arr = shuffle;
		
		double testSize = 1. / cv;
		
		int nTest = (int) (dVector.getNTimeWindow() * testSize);
		int nTrain = dVector.getNTimeWindow() - nTest;
		
		for (int icv = 0; icv < cv; icv++) {
			BasicID[] ids_train = new BasicID[nTrain * 2];
			BasicID[] ids_test = new BasicID[nTest * 2];
			
			BasicID[] obsIds = dVector.getObsIDs();
			BasicID[] synIds = dVector.getSynIDs();
			
			int itest0 = icv * nTest;
			int itest1 = (icv + 1) * nTest;
			
			System.out.println(icv + " " + nTrain + " " + nTest + " " + dVector.getNTimeWindow() + " " + itest0 + " " + itest1);
			
			int j = 0;
			int k = 0;
			for (int i = 0; i < dVector.getNTimeWindow(); i++) {
				if (i >= itest0 && i < itest1) {
					ids_test[2*(i-itest0)] = obsIds[arr[i]];
					ids_test[2*(i-itest0)+1] = synIds[arr[i]];
					k++;
				}
				else {
					ids_train[2*j] = obsIds[arr[i]];
					ids_train[2*j+1] = synIds[arr[i]];
					j++;
				}
			}
			
			Predicate<BasicID> chooser = id -> true;
			
			boolean atLeastThreeRecordsPerStation = false;
			switch (weightingType) {
			case RECIPROCAL:
			case RECIPROCAL_PcP:
			case RECIPROCAL_COS:
			case RECIPROCAL_CC:
			case RECIPROCAL_FREQ:
				if (!isSpc) {
					dVectors[icv][0] = new Dvector(ids_train, chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
					dVectors[icv][1] = new Dvector(ids_test, chooser, weightingType, atLeastThreeRecordsPerStation, selectionInfo);
				}
				else {
					dVectors[icv][0] = new Dvector(ids_train, chooser, WeightingType.RECIPROCAL_FREQ, atLeastThreeRecordsPerStation, selectionInfo);
					dVectors[icv][1] = new Dvector(ids_test, chooser, WeightingType.RECIPROCAL_FREQ, atLeastThreeRecordsPerStation, selectionInfo);
				}
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
		}
		
		return dVectors;
	}
	
	private void setEquation(Dvector dVector, Dvector dVectorSpc) {
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
			else {
				eq = new ObservationEquation(partialIDs, parameterList, dVector);
				
				eqA = new ObservationEquation(partialIDs, parameterList, dVector.clone());
				
//					dataErrorMap = new HashMap<PartialType, Double>();
//					dataErrorMap.put(PartialType.PARVS, dVector.getVariance());
				
				if (dVectorSpc != null) {
					System.out.println("Add spc equation");
					ObservationEquation eqSpc = new ObservationEquation(partialSpcIDs, parameterList, dVectorSpc);
					eqB = new ObservationEquation(partialSpcIDs, parameterList, dVectorSpc.clone());
					
//						eq = eq.setTypeToZero(PartialType.PARQ);
					
//						double d = eq.getDiagonalOfAtA().getLInfNorm();
//						double dSpc = eqSpc.getDiagonalOfAtA().getLInfNorm();
					double d = eq.getDiagonalOfAtA().getL1Norm();
					double dSpc = eqSpc.getDiagonalOfAtA().getL1Norm();
//						d = eq.getAtD().getL1Norm();
//						dSpc = eqSpc.getAtD().getL1Norm();
					
					System.out.println("d/dSpc=" + (d/dSpc));
					
//						RealMatrix atatmp = eq.getAtA().copy();
//						atatmp = atatmp.add(eqSpc.getAtA().scalarMultiply(scale_freq_ata * d / dSpc));
//						RealVector atdtmp = eq.getAtD().copy();
//						atdtmp = atdtmp.add(eqSpc.getAtD().mapMultiply(scale_freq_ata * d / dSpc));
//						eq = new ObservationEquation(atatmp, atdtmp, parameterList, dVector, eq.getA());
					
					mul = scale_freq_ata * d / dSpc;
					
					double varA_over_varB = eqA.getDVector().getVariance() * eqA.getDVector().getObsNormSquare() 
							/ (eqB.getDVector().getVariance() * eqB.getDVector().getObsNormSquare());
					
					mul = scale_freq_ata * varA_over_varB;
					System.out.println("mul=" + mul);
					
					eqSpc = eqSpc.scalarMultiply(mul);
					
					eqB = eqB.scalarMultiply(mul);
					eqB.setObsNormSquare(eqA.getDVector().getObsNormSquare());
					
					eq = eq.add(eqSpc);
					
//					double var_B = eqB.getDVector().getVariance() * eqB.getDVector().getObsNormSquare() / eqA.getDVector().getObsNormSquare();
					
					double var_B = eqB.getDVector().getVariance();
					eq.setVariance(eqA.getDVector().getVariance() + var_B);
					
//						dataErrorMap.put(PartialType.PARQ, dVectorSpc.getVariance());
					
//						for (PartialType type : dataErrorMap.keySet())
//							System.out.println(type + " data error = " + dataErrorMap.get(type));
				}
			}
		}
		
//		if (conditioner)
//			applyConditionner();
		
//		if (eqA != null && eqB != null) {
//			double var_B = eqB.getDVector().getVariance() * eqB.getDVector().getObsNormSquare() / eqA.getDVector().getObsNormSquare() * mul; 
//			System.out.println("eqA variance = " + eqA.getDVector().getVariance());
//			System.out.println("eqB variance = " + var_B);
//			System.out.println("eqA obs2 = " + eqA.getDVector().getObsNormSquare());
//			System.out.println("eqB obs2 = " + eqB.getDVector().getObsNormSquare());
//				System.out.println("eqA npts = " + eqA.getDVector().getNpts());
//				System.out.println("eqB npts = " + eqB.getDVector().getNpts());
//		}
			
	}
	
	private double mul;
	
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
	
	private Map<PartialType, Double> dataErrorMap;
	
	private RealMatrix regularizationVSQ(double lambdaMU, double gammaMU, double lambdaQ, double gammaQ) {
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
		
		double normMU_A = new ArrayRealVector(IntStream.range(0, eqA.getMlength())
				.filter(i -> eqA.getParameterList().get(i).getPartialType().equals(PartialType.PARVS))
				.mapToDouble(i -> eqA.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm();
		double normQ_A = new ArrayRealVector(IntStream.range(0, eqA.getMlength())
				.filter(i -> eqA.getParameterList().get(i).getPartialType().equals(PartialType.PARQ))
				.mapToDouble(i -> eqA.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm();
		double norm00_A = new ArrayRealVector(IntStream.range(0, eqA.getMlength())
				.filter(i -> eqA.getParameterList().get(i).getPartialType().equals(PartialType.PAR00))
				.mapToDouble(i -> eqA.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm();
		double normVP_A = new ArrayRealVector(IntStream.range(0, eqA.getMlength())
				.filter(i -> eqA.getParameterList().get(i).getPartialType().equals(PartialType.PARVP))
				.mapToDouble(i -> eqA.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm();
		
		double normMU_B = new ArrayRealVector(IntStream.range(0, eqB.getMlength())
				.filter(i -> eqB.getParameterList().get(i).getPartialType().equals(PartialType.PARVS))
				.mapToDouble(i -> eqB.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm();
		double normQ_B = new ArrayRealVector(IntStream.range(0, eqB.getMlength())
				.filter(i -> eqB.getParameterList().get(i).getPartialType().equals(PartialType.PARQ))
				.mapToDouble(i -> eqB.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm();
		double norm00_B = new ArrayRealVector(IntStream.range(0, eqB.getMlength())
				.filter(i -> eqB.getParameterList().get(i).getPartialType().equals(PartialType.PAR00))
				.mapToDouble(i -> eqB.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm();
		double normVP_B = new ArrayRealVector(IntStream.range(0, eqB.getMlength())
				.filter(i -> eqB.getParameterList().get(i).getPartialType().equals(PartialType.PARVP))
				.mapToDouble(i -> eqB.getDiagonalOfAtA().getEntry(i)).toArray()).getLInfNorm();
		
		double normMU = Math.sqrt(normMU_A + normMU_B); // before: Math.sqrt(normMU_A)
		double normQ = Math.sqrt(normQ_A + normQ_B);
		double norm00 = Math.sqrt(norm00_A + norm00_B);
		double normVP = Math.sqrt(normVP_A + normVP_B);
		
//		double normMU = Math.sqrt(normMU_A); // before: Math.sqrt(normMU_A)
//		double normQ = Math.sqrt(normQ_A);
//		double norm00 = Math.sqrt(norm00_A);
//		double normVP = Math.sqrt(normVP_A);
		
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
//		eq.addRegularization(D2tD2);
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
		
		double anchorFactor = 8*8; // 8*8
		
		PartialType tmptype = eq.getParameterList().get(0).getPartialType();
		for (int i = 1; i < eq.getMlength(); i++) {
			if (!tmptype.equals(eq.getParameterList().get(i).getPartialType())) {
				D.multiplyEntry(i - 1, i - 1, anchorFactor); // 9
				System.out.println(i + " " + tmptype + " " + eq.getParameterList().get(i).getPartialType());
				tmptype = eq.getParameterList().get(i).getPartialType();
			}
		}
		D.multiplyEntry(eq.getMlength()-1, eq.getMlength()-1, anchorFactor);
		
		RealMatrix Dout = D2tD2.add(D);
		
		System.out.println("Regularization norm = " + Dout.getNorm());
		
		return Dout;
		
//		eq.addRegularization(D);
//		eqA.addRegularization(D);
//		eqB.addRegularization(D);
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
	
	private void outputNow() {
		try {
			outputDistribution(outPath.resolve("stationEventDistribution.inf"));
			dVector.outOrder(outPath);
			dVector.outPhases(outPath);
			outEachTrace(outPath.resolve("trace"));
			UnknownParameterFile.write(outPath.resolve("unknownParameterOrder.inf"), eq.getParameterList());
			UnknownParameterFile.write(outPath.resolve("originalUnknownParameterOrder.inf"), eq.getOriginalParameterList());
			eq.outputA(outPath.resolve("partial"));
	//		eq.outputAtA(outPath.resolve("lmi_AtA.inf"));
			eq.outputUnkownParameterWeigths(outPath.resolve("unknownParameterWeigths.inf"));
			dVector.outWeighting(outPath);
			if (eqB != null) {
				eqB.outputA(outPath.resolve("partial_spc"));
				outEachTrace(outPath.resolve("trace_spc"), eqB.getDVector());
			}
			Path outpath_mul = outPath.resolve("mul.inf");
			try {
				Files.createFile(outpath_mul);
				Files.write(outpath_mul, String.valueOf(mul).getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		readIDs();
		
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
//		Future<Void> future;
//		try {
//			future = output();
//		} catch (Exception e) {
//			e.printStackTrace();
//			return;
//		}
		
//		double[] lambdaMUs = new double[] {0.01, 0.1, 0.2, 0.5, 1., 2.};
//		double[] gammaMUs = new double[] {0.01, 0.1, 0.2, 0.5, 1., 2.};
//		double[] lambdaQs = new double[] {0.01, 0.1, 0.2, 0.5, 1., 2.};
//		double[] gammaQs = new double[] {0.01, 0.1, 0.2, 0.5, 1., 2.};
		
//		double[] lambdaMUs = new double[] {0.1, 0.2, 0.5, 1};
//		double[] gammaMUs = new double[] {0.1, 0.2, 0.5, 1};
//		double[] lambdaQs = new double[] {0.1, 0.2, 0.5, 1};
//		double[] gammaQs = new double[] {0.1, 0.2, 0.5, 1};
		
		boolean plot = true;
		boolean lcurve = false;
		boolean checkerboard = false;
		
		final double[] values = new double[] {0.1, 0.2, 0.4, 0.6, 0.8, 1., 1.2, 1.5, 2., 3.};
		final double[] values_MU_normal = new double[] {0.1, 0.2, 0.4, 0.6, 0.8, 1.};
		final double[] values_MU_sparse = new double[] {0.2, 1.};
		final double[] values_Q_strong = new double[] {1.5, 2., 3., 5., 10.};
		final double[] values_Q_normal = new double[] {0.8, 1.2, 1.5, 3., 5};
//		final double[] values_Q = new double[] {0.8, 1., 1.2, 1.4, 1.6, 1.8, 2., 2.2, 2.4, 2.6, 2.8, 3., 4., 5, 8., 10., 15., 20.};
		final double[] values_Q = new double[] {0.45,0.55,0.63,0.71,0.77,0.84,0.89,0.95,1.0,1.05,1.1,1.26,1.41,1.55,1.67,1.79,1.9,2.0,2.45,2.83,3.16,3.46,3.74,4.0,4.24,4.47,4.69,4.9};
		//		final double[] values_for_plot_Q = new double[] {0.6, 0.9, 1.2, 1.5};
		final double[] values_for_plot_Q = new double[] {0.9, 1.2, 1.5};
		final double[] values_for_plot_MU = new double[] {0.1, 0.2};
		final double[] values_for_plot_MU_strong = new double[] {0.2, 0.4};
		
		int cv = 1;
		
		double[] lambdaMUs, gammaMUs, lambdaQs, gammaQs;
		if (plot) {
			lambdaMUs = new double[] {0.2}; // 0.2
			gammaMUs = new double[] {0.2}; // 0.2
			lambdaQs = new double[] {0.8};
			gammaQs = null;
			if (clusterIndex[0] == 3) {
//					lambdaQs = new double[] {1.};
				if (azimuthIndex[0] == 0)
//						gammaQs = new double[] {1.935, 2.5, 2.956};
					gammaQs = new double[] {1.26, 1.41, 1.54};
				else if (azimuthIndex[0] == 3) {
//					gammaQs = new double[] {1.39,1.55,1.7};
					gammaQs = new double[] {1.494,1.67,1.83};
					lambdaQs = new double[] {1.2};
				}
				else if (azimuthIndex[0] == 5) {
					gammaMUs = new double[] {0.4};
//					lambdaMUs = new double[] {0.2};
					lambdaQs = new double[] {1.6};
					gammaQs = new double[] {0.89,1,1.1};
				}
				else
					gammaQs = new double[] {0.89,1,1.1};
			}
			else if (clusterIndex[0] == 4) {
				if (azimuthIndex[0] == 3) {
					gammaQs = new double[] {0.8, 0.89, 0.97};
//						gammaQs = new double[] {0.894, 1, 1.095};
				}
				else {
					gammaQs = new double[] {0.8, 0.89, 0.97};
				}
			}
			else
				throw new RuntimeException("Cluster not expected " + clusterIndex[0]);
			cv = 1;
		}
		else if (checkerboard) {
			lambdaMUs = new double[] {0.2};
			gammaMUs = new double[] {0.2};
			lambdaQs = new double[] {0.8};
			gammaQs = new double[] {0.89};
		}
		else if (lcurve) {
			lambdaMUs = new double[] {0.2};
			gammaMUs = values_MU_sparse;
			lambdaQs = new double[] {0.8};
			gammaQs = values_Q;
//			gammaQs = new double[] {1.26, 500.};
		}
		else 
			throw new RuntimeException("Not expected");
		
		// to be consistent with the new normalization of coefficients
		for (int i = 0; i < gammaMUs.length; i++)
			gammaMUs[i] /= 2;
		for (int i = 0; i < lambdaMUs.length; i++)
			lambdaMUs[i] /= 2;
//		for (int i = 0; i < gammaQs.length; i++)
//			gammaQs[i] /= 2;
		for (int i = 0; i < lambdaQs.length; i++)
			lambdaQs[i] /= 2;
		
//		double[] gammaMUs = new double[] {0.};
//		double[] lambdaMUs = new double[] {0.};
//		double[] gammaQs = new double[] {0.};
//		double[] lambdaQs = new double[] {0.};
		
		Map<String, double[]> parameterMap = new HashMap<String, double[]>();
		parameterMap.put("lambdaMU", lambdaMUs);
		parameterMap.put("gammaMU", gammaMUs);
		parameterMap.put("lambdaQ", lambdaQs);
		parameterMap.put("gammaQ", gammaQs);
		
		int randomSeed = 42;
		
		final Dvector dVector_final = dVector.clone();
		final Dvector dVectorSpc_final = dVectorSpc != null ? dVectorSpc.clone() : null;
		
		setEquation(dVector, dVectorSpc);
		outputNow();
		
		Path recordPath = outPath.resolve("parameters.txt");
		try (PrintWriter pw = new PrintWriter(recordPath.toFile())) {
			pw.println("cv lambda_mu gamma_mu lambda_q gamma_q var_red_train var_red_test var_red_ratio solution_var solution_var_v solution_var_q var_red_train_A var_red_train_B"
					+ " var_red_train_vs_A var_red_train_vs_B var_red_train_q_A var_red_train_q_B var_0_train var_0_test");
			
			//TODO check train test split
			Dvector[][] dVectors_train_test = trainTestSplitCV(cv, dVector_final, randomSeed, false, false);
			Dvector[][] dVectors_train_test_spc = null;
			if (dVectorSpc != null)
				dVectors_train_test_spc = trainTestSplitCV(cv, dVectorSpc_final, randomSeed, true, true);
			
			int count = 0;
			for (int i = 0; i < cv; i++) {
				if (dVectorSpc != null)
					setEquation(dVectors_train_test[i][0], dVectors_train_test_spc[i][0]);
				else
					setEquation(dVectors_train_test[i][0], null);
				
				for (double lambdaMU : lambdaMUs) {
					for (double gammaMU : gammaMUs) {
						for (double lambdaQ : lambdaQs) {
							for (double gammaQ : gammaQs) {
//								double f = Math.sqrt(scale_freq_ata);
//								if (f != 1)
//									System.out.println("WARNING: using scale_freq_ata to normalize regularization parameters " + f); 
								RealMatrix D = regularizationVSQ(lambdaMU, gammaMU, lambdaQ, gammaQ);
								
								// 逆問題
								solve(count, D);
								
								// evaluate on test set
								ObservationEquation eqTestA = new ObservationEquation(partialIDs, parameterList, dVectors_train_test[i][1].clone());
								ObservationEquation eqTestB = null;
								if (dVectorSpc != null) {
									eqTestB = new ObservationEquation(partialSpcIDs, parameterList, dVectors_train_test_spc[i][1].clone());
									double varA_over_varB = eqTestA.getDVector().getVariance() * eqTestA.getDVector().getObsNormSquare() 
											/ (eqTestB.getDVector().getVariance() * eqTestB.getDVector().getObsNormSquare());
									double mul = scale_freq_ata * varA_over_varB;
									eqTestB = eqTestB.scalarMultiply(mul);
									eqTestB.setObsNormSquare(eqTestA.getDVector().getObsNormSquare());
									eqTestA = eqTestA.add(eqTestB);
									
//									double var_B = eqTestB.getDVector().getVariance() * eqTestB.getDVector().getObsNormSquare() / eqTestA.getDVector().getObsNormSquare() * mul;
//									double var_B = eqTestB.getDVector().getVariance();
//									eqTestA.setVariance(eqTestA.getDVector().getVariance() + var_B);
								}
								
								double varTestM = eqTestA.varianceOf(ansM);
								double varTest0 = eqTestA.getDVector().getVariance();
								
								double[] vq = new double[ansM.getDimension()];
								ArrayRealVector v = new ArrayRealVector(ansM.getDimension() / 2);
								ArrayRealVector q = new ArrayRealVector(ansM.getDimension() / 2);
								int count_v = 0;
								int count_q = 0;
								double partial_q_amp_factor = 1.3;
								for (int k = 0; k < vq.length; k++) {
									if (parameterList.get(k).getPartialType().equals(PartialType.PARQ)) {
//										double dQ = -312 * 312 * ansM.getEntry(k);
										double dQ = -312 * 312 * ansM.getEntry(k) * partial_q_amp_factor
												/ (1 + 312*ansM.getEntry(k) * partial_q_amp_factor);
										vq[k] = dQ / 50. * 100;
										q.setEntry(count_q++, dQ / 50 * 100);
									}
									else if (parameterList.get(k).getPartialType().equals(PartialType.PARVS)) {
										vq[k] = ansM.getEntry(k) * 100;
										v.setEntry(count_v++, ansM.getEntry(k) * 100);
									}
								}
								ArrayRealVector vq_vec = new ArrayRealVector(vq);
								double solVar = Math.sqrt(vq_vec.dotProduct(vq_vec)) / vq.length;
								
//								double solVar_v = v.getLInfNorm() * v.getLInfNorm();
//								double solVar_q = q.getLInfNorm() * q.getLInfNorm();
								double solVar_v = v.getNorm() * v.getNorm();
								double solVar_q = q.getNorm() * q.getNorm();
								
								// write
								double dvar_train = eq.getDVector().getVariance() - varianceM;
								double dvar_test = varTest0 - varTestM;
								double dvar_train_percent = dvar_train / eq.getDVector().getVariance() * 100;
								double dvar_test_percent = dvar_test / varTest0 * 100;
								
								double dvar_train_A = eqA.getDVector().getVariance() - eqA.varianceOf(ansM);
								double dvar_train_A_percent = dvar_train_A / eqA.getDVector().getVariance() * 100;
								
								double dvar_train_B = Double.NaN;
								double dvar_train_B_percent = Double.NaN;
								if (dVectorSpc != null) {
									 dvar_train_B = eqB.getDVector().getVariance() - eqB.varianceOf(ansM);
									 dvar_train_B_percent = dvar_train_B / eqB.getDVector().getVariance() * 100;
								}
								
								RealVector ansM_vs = new ArrayRealVector(ansM.getDimension());
								RealVector ansM_q = new ArrayRealVector(ansM.getDimension());
								for (int k = 0; k < ansM.getDimension(); k++) {
									if (parameterList.get(k).getPartialType().equals(PartialType.PARVS))
										ansM_vs.setEntry(k, ansM.getEntry(k));
									else if (parameterList.get(k).getPartialType().equals(PartialType.PARQ))
										ansM_q.setEntry(k, ansM.getEntry(k));
								}
								double dvar_train_vs_A = eqA.getDVector().getVariance() - eqA.varianceOf(ansM_vs);
								double dvar_train_q_A = eqA.getDVector().getVariance() - eqA.varianceOf(ansM_q);
								double dvar_train_vs_A_percent = dvar_train_vs_A / eqA.getDVector().getVariance() * 100;
								double dvar_train_q_A_percent = dvar_train_q_A / eqA.getDVector().getVariance() * 100;
								
								double dvar_train_vs_B = Double.NaN;
								double dvar_train_q_B = Double.NaN;
								double dvar_train_vs_B_percent = Double.NaN;
								double dvar_train_q_B_percent = Double.NaN;
								if (dVectorSpc != null) {
									 dvar_train_vs_B = eqB.getDVector().getVariance() - eqB.varianceOf(ansM_vs);
									 dvar_train_q_B = eqB.getDVector().getVariance() - eqB.varianceOf(ansM_q);
									 dvar_train_vs_B_percent = dvar_train_vs_B / eqB.getDVector().getVariance() * 100;
									 dvar_train_q_B_percent = dvar_train_q_B / eqB.getDVector().getVariance() * 100;
								}
								
								pw.println(i + " " + lambdaMU + " " + gammaMU + " " + lambdaQ + " " + gammaQ
										+ " " + dvar_train_percent
										+ " " + dvar_test_percent
										+ " " + dvar_train_percent / dvar_test_percent
										+ " " + solVar + " " + solVar_v + " " + solVar_q
										+ " " + dvar_train_A_percent + " " + dvar_train_B_percent
										+ " " + dvar_train_vs_A_percent + " " + dvar_train_vs_B_percent
										+ " " + dvar_train_q_A_percent + " " + dvar_train_q_B_percent
										+ " " + eq.getDVector().getVariance() + " " + varTest0);
								
								count++;
							}
						}
					}
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		try {
//			future.get();
//		} catch (InterruptedException | ExecutionException e) {
//			e.printStackTrace();
//		}
		
//		try {
//			if (!inverseMethods.contains(InverseMethodEnum.FAST_CONJUGATE_GRADIENT) && !inverseMethods.contains(InverseMethodEnum.FAST_CONJUGATE_GRADIENT_DAMPED))
//				eq.outputSensitivity(outPath.resolve("sensitivity.inf"));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
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
	
	public void outEachTrace(Path outPath) throws IOException {
		outEachTrace(outPath, eq.getDVector());
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

	private void solve(int count, RealMatrix D) {
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
						String outfile = method.simple() + String.valueOf(count);
						solve(outPath.resolve(outfile), method.getMethod(eq.getAtA().add(D), eq.getAtD()));
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
		
//		outVariancePerEvents(outPath, inverseProblem);
//		outLcurveEntry(inverseProblem);
		
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
		LetMeInvertCV lmi = new LetMeInvertCV(Property.parse(args));
		System.err.println(LetMeInvertCV.class.getName() + " is running.");
		long startT = System.nanoTime();
		lmi.run();
		System.err.println(
				LetMeInvertCV.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - startT));
	}

	double varianceM;
	
	RealVector ansM;
	
	/**
	 * outDirectory下にvarianceを書き込む
	 * 
	 * @param outPath
	 */
	private void outVariance(Path outPath, InverseProblem inverse) throws IOException {
		System.out.println("Outputting variance");
		Path out = outPath.resolve("variance.txt");
		Path out_AB = outPath.resolve("variance_AB.txt");
		if (Files.exists(out))
			throw new FileAlreadyExistsException(out.toString());
		double[][] variance = new double[eq.getMlength() + 1][3];
//		if (eqA != null && eqB != null) {
////			variance[0] = 2. / (1 + mul) * (eqA.getDVector().getVariance() + eqB.getDVector().getVariance());
//			double var_B = eqB.getDVector().getVariance() * eqB.getDVector().getObsNormSquare() / eqA.getDVector().getObsNormSquare() * mul;
//			variance[0] = eqA.getDVector().getVariance() + var_B;
//		}
//		else
//			double var_B = eqB.getDVector().getVariance() * eqB.getDVector().getObsNormSquare() / eqA.getDVector().getObsNormSquare() * mul;
			double var_B = eqB.getDVector().getVariance();
			double var_A = eqA.getDVector().getVariance();
			variance[0][0] = var_A + var_B;
			variance[0][1] = var_A;
			if (eqB != null)
				variance[0][2] = var_B;
			else
				variance[0][2] = Double.NaN;
		int tmpN = eq.getMlength();
//		for (int i = 0; i < eq.getMlength(); i++)
//		if (eqA != null && eqB != null) {
//			for (int i = 0; i < tmpN; i++) {
////				variance[i + 1] =  2. / (1 + mul) * (eqA.varianceOf(inverse.getANS().getColumnVector(i)) 
////					+ eqB.varianceOf(inverse.getANS().getColumnVector(i)));
//				double var_B = eqB.varianceOf(inverse.getANS().getColumnVector(i)) * eqB.getDVector().getObsNormSquare() / eqA.getDVector().getObsNormSquare() * mul;
//				variance[i + 1] = eqA.varianceOf(inverse.getANS().getColumnVector(i)) + var_B;
//			}
//		}
//		else {
			for (int i = 0; i < tmpN; i++) {
//				var_B = eqB.varianceOf(inverse.getANS().getColumnVector(i)) * eqB.getDVector().getObsNormSquare() / eqA.getDVector().getObsNormSquare() * mul;
				var_B = eqB.varianceOf(inverse.getANS().getColumnVector(i));
				var_A = eqA.varianceOf(inverse.getANS().getColumnVector(i));
				variance[i + 1][0] = var_A + var_B;
				variance[i + 1][1] = var_A;
				if (eqB != null)
					variance[i + 1][2] = var_B;
				else
					variance[i+1][2] = Double.NaN;
			}
//		}
		
		varianceM = variance[tmpN][0];
		ansM = inverse.getANS().getColumnVector(tmpN - 1);
		
		double[] vartmp = new double[variance.length];
		for (int i = 0; i < vartmp.length; i++)
			vartmp[i] = variance[i][0];
		
		writeDat(out, vartmp);
		writeDat(out_AB, variance);
		
		if (alpha == null)
			return;
		for (int i = 0; i < alpha.length; i++) {
			out = outPath.resolve("aic" + i + ".txt");
			double[] aic = computeAIC(vartmp, alpha[i]);
			writeDat(out, aic);
		}
		writeDat(outPath.resolve("aic.inf"), alpha);
	}
	
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
			pw.print(varA + " " + varB + " " + var + " " + solutionQL2Norm + " " + solutionQLinfNorm + " " + solutionVL2Norm + " " + solutionVLinfNorm + " " + varA0 + " " + varB0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
	
	/**
	 * @param out
	 * @param dat
	 */
	private static void writeDat(Path out, double[][] dat) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out, StandardOpenOption.CREATE_NEW))) {
			Arrays.stream(dat).forEach(row -> {
				for (double d : row)
					pw.print(d + " ");
				pw.println();
			});
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
