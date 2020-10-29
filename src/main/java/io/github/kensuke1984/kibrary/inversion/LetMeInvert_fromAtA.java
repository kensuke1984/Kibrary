package io.github.kensuke1984.kibrary.inversion;

import java.awt.color.CMMException;
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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Precision;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionType;
import io.github.kensuke1984.kibrary.datacorrection.TakeuchiStaticCorrection;
import io.github.kensuke1984.kibrary.inversion.addons.DampingType;
import io.github.kensuke1984.kibrary.inversion.addons.ModelCovarianceMatrix;
import io.github.kensuke1984.kibrary.inversion.addons.VelocityField3D_deprec;
import io.github.kensuke1984.kibrary.inversion.addons.WeightingType;
import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.selection.DataSelectionInformation;
import io.github.kensuke1984.kibrary.selection.DataSelectionInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.FrequencyRange;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTCatalog;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.globalcmt.NDK;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtAEntry;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtAFile;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtdEntry;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtdFile;
import io.github.kensuke1984.kibrary.waveformdata.addons.ResidualVarianceFile;

/**
 * 
 * Let's invert
 * 
 * @version 2.0.3.3
 * 
 * @author Kensuke Konishi (original LetMeInvert class)
 * @author Anselme Borgeaud
 * 
 */
public class LetMeInvert_fromAtA implements Operation {
	/**
	 * 観測波形、理論波形の入ったファイル (BINARY)
	 */
	protected Path atdPath;

	/**
	 * 求めたい未知数を羅列したファイル (ASCII)
	 */
	protected Path unknownParameterListPath;

	/**
	 * partialIDの入ったファイル
	 */
	protected Path ataPath;

	/**
	 * ステーション位置情報のファイル
	 */
	protected Path stationInformationPath;

	protected Path residualVariancePath;
	
	/**
	 * どうやって方程式を解くか。 cg svd
	 */
	protected Set<InverseMethodEnum> inverseMethods;
	
	protected boolean time_source, time_receiver;
	
	private ObservationEquation[][][][] eq;
	
	private List<DataSelectionInformation> selectionInfo;
	
	private double minDistance;
	
	private double maxDistance;
	
	private double minMw;
	
	private double maxMw;
	
	private boolean linaInversion;
	
	private Path inputPath;
	
	private int maxNumVector;
	private double deltaDegree;

	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("stationInformationPath"))
			throw new IllegalArgumentException("There is no information about stationInformationPath.");
//		if (!property.containsKey("inputPath") && !property.containsKey("atdPath"))
//			throw new IllegalArgumentException("One of the path 'inputPath' or 'atdPath' must be set");
//		if (property.containsKey("inputPath") && property.containsKey("atdPath"))
//			throw new IllegalArgumentException("Only one of the path 'inputPath' or 'atdPath' can be set");
		if (!property.containsKey("ataPath"))
			throw new IllegalArgumentException("There is no information about 'ataPath'.");
		if (!property.containsKey("atdPath"))
			throw new IllegalArgumentException("There is no information about 'atdPath'.");
		if (!property.containsKey("residualVariancePath"))
			throw new IllegalArgumentException("There is no information about 'residualVariancePath'");
		if (!property.containsKey("inverseMethods"))
			property.setProperty("inverseMethods", "CG SVD");
		if (!property.containsKey("time_source"))
			property.setProperty("time_source", "false");
		if (!property.containsKey("time_receiver"))
			property.setProperty("time_receiver", "false");
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
		if (!property.containsKey("applyModelCovariance"))
			property.setProperty("applyModelCovariance", "false");
		if (!property.containsKey("applyDamping"))
			property.setProperty("applyDamping", "false");
		if (!property.containsKey("applyParameterWeight"))
			property.setProperty("applyParameterWeight", "false");
		if (!property.containsKey("applyRadialWeight"))
			property.setProperty("applyRadialWeight", "false");
		
		// additional unused info
		property.setProperty("CMTcatalogue", GlobalCMTCatalog.getCatalogPath().toString());
//		property.setProperty("STF catalogue", GlobalCMTCatalog.);
	}

	private void set() {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));
		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		stationInformationPath = getPath("stationInformationPath");
		ataPath = getPath("ataPath");
		
		unknownParameterListPath = getPath("unknownParameterListPath");
		residualVariancePath = getPath("residualVariancePath");
		if (property.containsKey("alpha"))
			alpha = Arrays.stream(property.getProperty("alpha").split("\\s+")).mapToDouble(Double::parseDouble)
					.toArray();
		inverseMethods = Arrays.stream(property.getProperty("inverseMethods").split("\\s+")).map(InverseMethodEnum::of)
				.collect(Collectors.toSet());
		time_source = Boolean.parseBoolean(property.getProperty("time_source"));
		time_receiver = Boolean.parseBoolean(property.getProperty("time_receiver"));

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
		
		minDistance = Double.parseDouble(property.getProperty("minDistance"));
		maxDistance = Double.parseDouble(property.getProperty("maxDistance"));
		minMw = Double.parseDouble(property.getProperty("minMw"));
		maxMw = Double.parseDouble(property.getProperty("maxMw"));
		
		linaInversion = Boolean.parseBoolean(property.getProperty("linaInversion"));
		
//		if (property.containsKey("inputPath") && property.containsKey("atdPath"))
//			throw new RuntimeException("Define either inputPath or atdPath, not both.");
		
		if (property.containsKey("inputPath"))
			inputPath = Paths.get(property.getProperty("inputPath"));
		else
			inputPath = null;
		
//		if (property.containsKey("atdPath"))
			atdPath = getPath("atdPath");
//		else
//			atdPath = null;
		
		if (property.containsKey("model")) {
			model = property.getProperty("model").trim().toLowerCase();
			perturbationLayerFile = workPath.resolve(property.getProperty("perturbationLayerFile"));
			deltaDegree = Double.parseDouble(property.getProperty("deltaDegree"));
			maxNumVector = Integer.parseInt(property.getProperty("maxNumVector"));
		}
		else
			model = null;
		
		applyModelCovariance = Boolean.parseBoolean(property.getProperty("applyModelCovariance"));
		if (applyModelCovariance) {
			cmH = Double.parseDouble(property.getProperty("cmH"));
			cmV = Double.parseDouble(property.getProperty("cmV"));
		}
		
		applyDamping = Boolean.parseBoolean(property.getProperty("applyDamping"));
//		if (applyDamping) {
		lambda = Double.parseDouble(property.getProperty("lambda"));
		dampingType = DampingType.valueOf(property.getProperty("dampingType"));
//		}
		
		applyParameterWeight = Boolean.parseBoolean(property.getProperty("applyParameterWeight"));
		if (applyParameterWeight) {
			sensitivityFile = workPath.resolve(property.getProperty("sensitivityFile"));
		}
		
		applyRadialWeight = Boolean.parseBoolean(property.getProperty("applyRadialWeight"));
	}
	
	private boolean applyModelCovariance;
	private double cmH;
	private double cmV;

	/**
	 * AIC計算に用いるα 独立データ数はn/αと考える
	 */
	protected double[] alpha;

	private Properties property;
	
	private Path perturbationLayerFile;
	
	private String model;
	
	private boolean applyParameterWeight;
	
	private Path sensitivityFile;
	
	private boolean applyRadialWeight;

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(LetMeInvert_fromAtA.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan LetMeInvert");
			pw.println("##These properties for LetMeInvert");
			pw.println("##Path of a work folder (.)");
			pw.println("#workPath");
			pw.println("##Path for the input file for the checkerboard test");
			pw.println("#inputPath input.inf");
			pw.println("##Path of the Atd vector file, must be set");
			pw.println("#atdPath atd.dat");
			pw.println("##Path of the AtA matrix file, must be set");
			pw.println("#ataPath ata.dat");
			pw.println("##Path of a unknown parameter list file, must be set");
			pw.println("#unknownParameterListPath unknowns.inf");
			pw.println("##Path of the residual variance file, must be set");
			pw.println("#residualVariancePath residualVariance.dat");
			pw.println("##Path of a station information file, must be set");
			pw.println("#stationInformationPath station.inf");
			pw.println("##double[] alpha it self, if it is set, compute aic for each alpha.");
			pw.println("#alpha");
			pw.println("##inverseMethods[] names of inverse methods (CG SVD)");
			pw.println("#inverseMethods");
			pw.println("##=======================================================================================");
			pw.println("##=== Parameters for weighting of model parameters ======================================");
			pw.println("##Set true if you want to apply model parameters weight");
			pw.println("#applyParameterWeight");
			pw.println("##Path of sensitivity (AtA diagonal) file");
			pw.println("#sensitivityFile");
			pw.println("#applyRadialWeight");
			pw.println("##=======================================================================================");
			pw.println("##=== Parameters for Model Covariance matrix and damping ================================");
			pw.println("##Set true if you want to apply model covariance matrix (false)");
			pw.println("#applyModelCovariance");
			pw.println("##Horizontal correlation length");
			pw.println("#cmH");
			pw.println("##Vertical correlation length");
			pw.println("#cmV");
			pw.println("##Set true if you want to apply damping (false)");
			pw.println("#applyDamping");
			pw.println("##Damping type: L, LM. Must be set");
			pw.println("#dampingType");
			pw.println("##Damping coefficient. Must be set");
			pw.println("#lambda");
			pw.println("##=======================================================================================");
			pw.println("##=== Parameters for GMT output =========================================================");
			pw.println("##Name of initial model");
			pw.println("#model");
			pw.println("##Path of a perturbation layer file");
			pw.println("#perturbationLayerFile perturbationLayers.inf");
			pw.println("##Size of model grid in degree");
			pw.println("#deltaDegree");
			pw.println("##Number of CG vectors for which velocity perturbations are output (more is slower)");
			pw.println("#maxNumVector 20");
			pw.println("##=======================================================================================");
			pw.println("##=== Optional parameters ===============================================================");
			pw.println("##boolean time_source (false). Time partial for the source");
			pw.println("#time_source false");
			pw.println("##boolean time_receiver (false). Time partial for the receiver");
			pw.println("#time_receiver false");
			pw.println("##DataSelectionInformationFile (leave blank if not needed)");
			pw.println("#DataSelectionInformationFile");
			pw.println("##If want to select distance range: min distance (deg) of the data used in the inversion");
			pw.println("#minDistance ");
			pw.println("##If want to select distance range: max distance (deg) of the data used in the inversion");
			pw.println("#maxDistance ");
			pw.println("##minimum Mw (0.)");
			pw.println("#minMw");
			pw.println("##maximum Mw (10.)");
			pw.println("#maxMw");
			pw.println("##Set parameters for inversion for Yamaya et al. CMT paper (false)");
			pw.println("#linaInversion");
		}
		System.err.println(outPath + " is created.");
	}

	private Path workPath;

	public LetMeInvert_fromAtA(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
		if (!canGO())
			throw new RuntimeException();
		setEquation();
	}

//	public LetMeInvert_fromAtA(Path workPath, Set<Station> stationSet, ObservationEquation equation) throws IOException {
//		eq = equation;
//		this.stationSet = stationSet;
//		workPath.resolve("lmi" + Utilities.getTemporaryString());
//		inverseMethods = new HashSet<>(Arrays.asList(InverseMethodEnum.values()));
//	}

	private Path[][][][] outPaths;
	
	private WeightingType[] weights;
	
	private double[][] frequencyRanges;
	
	private Phases[] phases;
	
	private StaticCorrectionType[] corrections;
	
	double[][][][] residualVariances;
	double[][][][] obsNorms;
	
	private List<UnknownParameter> parameterList;
	
	private RealVector inputVector;
	
	private boolean applyDamping;
	private double lambda;
	private DampingType dampingType;
	
	private double[] parameterWeights;

	private void setEquation() throws IOException {
		// set AtA matrix
		System.out.println("Reading AtA...");
		AtAEntry[][][][] ataEntries = AtAFile.read(ataPath);
		
		// set unknown parameter
		System.err.println("Setting unknown parameter set...");
		parameterList = UnknownParameterFile.read(unknownParameterListPath);
		
		//set Atd vector, or read the input vector for checkerboard test
		AtdEntry[][][][][] atdEntries = null;
		if (inputPath == null) {
			System.out.println("Reading Atd...");
			atdEntries = AtdFile.readArray(atdPath);
		}
		else {
			System.out.println("Reading Atd (header info)");
			atdEntries = AtdFile.readArray(atdPath);
			System.out.println("Reading checkerboard input " + inputPath + "...");
			inputVector = readCheckerboardInput(inputPath);
			if (inputVector.getDimension() != parameterList.size())
				throw new RuntimeException("Length of input vector differs from number of parameters " + inputVector.getDimension() + " " + parameterList.size());
		}
		
		// set weights, freqency ranges, and phases
		int nweight = atdEntries[0].length;
		int nfreq = atdEntries[0][0].length;
		int nphase = atdEntries[0][0][0].length;
		int ncorr = atdEntries[0][0][0][0].length;
		weights = new WeightingType[nweight];
		frequencyRanges = new double[nfreq][];
		phases = new Phases[nphase];
		corrections = new StaticCorrectionType[ncorr];
		String s = "";
		for (int i = 0; i < weights.length; i++) {
			weights[i] = atdEntries[0][i][0][0][0].getWeightingType();
			s += weights[i] + ", ";
		}
		System.out.println("Weighting types: " + s);
		s = "";
		for (int i = 0; i < frequencyRanges.length; i++) {
			FrequencyRange range = atdEntries[0][0][i][0][0].getFrequencyRange();
			frequencyRanges[i] = new double[] {range.getMinFreq(), range.getMaxFreq()};
			s += range + ", ";
		}
		System.out.println("Frequency ranges: " + s);
		s = "";
		for (int i = 0; i < phases.length; i++) {
			phases[i] = atdEntries[0][0][0][i][0].getPhases();
			s += phases[i] + ", ";
		}
		System.out.println("Phases: " + s);
		s = "";
		for (int i = 0; i < corrections.length; i++) {
			corrections[i] = atdEntries[0][0][0][0][i].getCorrectionType();
			s += corrections[i] + ", ";
		}
		System.out.println("Correction types: " + s);
		
		// set output folders paths
		String modelCovarianceString = "";
		if (applyModelCovariance)
			modelCovarianceString = String.format("_cmH%.0f_cmV%.0f", cmH, cmV);
		String dampingString = "";
		if (applyDamping)
			dampingString = "_" + dampingType.name() + "_" + (int) (lambda * 100);
		String parameterWeightString = "";
		if (applyParameterWeight) {
			parameterWeightString = "_PW";
			parameterWeights = ModelCovarianceMatrix.readSensitivityFileAndComputeWeight(sensitivityFile);
		}
			
		outPaths = new Path[weights.length][][][];
		for (int iweight = 0; iweight < weights.length; iweight++) {
			outPaths[iweight] = new Path[frequencyRanges.length][][];
			for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
				outPaths[iweight][ifreq] = new Path[phases.length][];
				for (int iphase = 0; iphase < phases.length; iphase++) {
					outPaths[iweight][ifreq][iphase] = new Path[corrections.length];
					for (int icorr = 0; icorr < corrections.length; icorr++) {
						String freqString = String.format("%.1f-%.1f", 1./frequencyRanges[ifreq][1], 1./frequencyRanges[ifreq][0]);
						if (inputPath == null) {
							Path path = workPath.resolve("lmi_" + weights[iweight] + "_" + freqString + "_" + phases[iphase] + "_" + corrections[icorr] 
									+ modelCovarianceString + dampingString + parameterWeightString);
							if (path.toFile().exists())
								throw new RuntimeException("Path already exists " + path);
							outPaths[iweight][ifreq][iphase][icorr] = path;
						}
						else {
							Path path = workPath.resolve("lmiCheckerboard" + weights[iweight] + "_" + freqString + "_" + phases[iphase] + "_" + corrections[icorr] 
									+ modelCovarianceString + dampingString + parameterWeightString);
							if (path.toFile().exists())
								throw new RuntimeException("Path already exists " + path);
							outPaths[iweight][ifreq][iphase][icorr] = path;
						}
					}
				}
			}
		}
		
		// set residual variances
		npts = ResidualVarianceFile.readNPTS(residualVariancePath);
		residualVariances = ResidualVarianceFile.readVariance(residualVariancePath);
		obsNorms = ResidualVarianceFile.readObsNorm(residualVariancePath);
		if (inputPath != null)
			System.out.println("Warning: currently the output variance is meaningless for checkerboard test");
		
		//set model covariance matrix
//		RealMatrix cm = null;
		ModelCovarianceMatrix cmMaker = null;
		if (applyModelCovariance) {
			if (!applyParameterWeight) { 
				System.out.println("Using model covariance matrix with " + cmV + " " + cmH);
				if (applyRadialWeight)
					System.out.println("Using radial weight");
				cmMaker = new ModelCovarianceMatrix(parameterList, cmV, cmH, 1., applyRadialWeight);
			}
			else {
				System.out.println("Using model covariance matrix and parameter weight with " + cmV + " " + cmH + " " + sensitivityFile);
				cmMaker = new ModelCovarianceMatrix(parameterList, cmV, cmH, sensitivityFile);
			}
			
//			cm = cmMaker.getCm();
			
//			if (Double.isNaN(cm.getNorm())) {
//				int n = cm.getColumnDimension();
//				for (int i = 0; i < n; i++) {
//					for (int j = 0; j < n; j++) {
//						double cmij = cm.getEntry(i, j);
//						if (Double.isNaN(cmij))
//							System.out.println(parameterList.get(i) + " " + parameterList.get(j) + " " + cmij);
//					}
//				}
//				throw new RuntimeException("Model covariance matrix has NaN entries (see above)");
//			}
				
		}
		
		//set equations
		System.out.println("Setting inverse equations...");
		eq = new ObservationEquation[weights.length][][][];
		
		for (int iweight = 0; iweight < weights.length; iweight++) {
			eq[iweight] = new ObservationEquation[frequencyRanges.length][][];
			for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
				eq[iweight][ifreq] = new ObservationEquation[phases.length][];
				for (int iphase = 0; iphase < phases.length; iphase++) {
					eq[iweight][ifreq][iphase] = new ObservationEquation[corrections.length];
					for (int icorr = 0; icorr < corrections.length; icorr++) {
//						if (applyParameterWeight) {
//							ataEntries = AtAFile.multiply(ataEntries, parameterWeights);
//							atdEntries = AtdFile.multiply(atdEntries, parameterWeights);
//						}
						RealMatrix ata = AtAFile.getAtARealMatrix(ataEntries, iweight, ifreq, iphase);
						RealVector atd;
						
						if (inputPath == null)
							atd = AtdFile.getAtdVector(atdEntries, iweight, ifreq, iphase, icorr);
						else
							atd = ata.operate(inputVector);
						if (applyModelCovariance) {
							double ataScale = 0;
							for (int i = 0; i < ata.getColumnDimension(); i++)
								ataScale += Math.abs(ata.getEntry(i, i));
							ataScale /= ata.getColumnDimension();
							
//							ata = cm.multiply(ata);
							cmMaker.mapMultiply(1. / (ataScale * lambda));
							
							ata = cmMaker.leftMultiply(ata);
							
							for (int k = 0; k < ata.getColumnDimension(); k++)
								ata.addToEntry(k, k, 1.);
							
//							atd = cm.operate(atd);
							atd = cmMaker.operate(atd);
							
							if (Double.isNaN(ata.getNorm())) {
								int n = ata.getColumnDimension();
								for (int i = 0; i < n; i++) {
									for (int j = 0; j < n; j++) {
										double ataij = ata.getEntry(i, j);
										if (Double.isNaN(ataij))
											System.out.println(parameterList.get(i) + " " + parameterList.get(j) + " " + ataij);
									}
								}
								throw new RuntimeException("AtA matrix has NaN entries (see above)");
							}
						}
//						if (applyParameterWeight) {
//							ata = AtAFile.multiply(ata, parameterWeights);
//							atd = AtdFile.multiply(atd, parameterWeights);
//						}
						if (applyDamping) {
							int n = ata.getColumnDimension();
							RealMatrix g = new Array2DRowRealMatrix(n, n);
							if (dampingType.equals(DampingType.L)) {
								double ataScale = 0;
								for (int i = 0; i < n; i++)
									ataScale += Math.abs(ata.getEntry(i, i));
								ataScale /= n;
								for (int i = 0; i < n; i++)
									g.setEntry(i, i, lambda * ataScale);
							}
							else if (dampingType.equals(DampingType.LM)) {
								for (int i = 0; i < n; i++)
									g.setEntry(i, i, ata.getEntry(i, i) * lambda);
							}
							
							ata = ata.add(g);
						}
						
						eq[iweight][ifreq][iphase][icorr] = new ObservationEquation(ata, parameterList, atd);
					}
				}
			}
		}
	}
	
	private RealVector readCheckerboardInput(Path path) throws IOException {
		List<Double> inputList = new ArrayList<>();
		BufferedReader br = Files.newBufferedReader(path);
		String line;
		while ((line = br.readLine()) != null) {
			inputList.add(Double.parseDouble(line));
		}
		br.close();
		RealVector inputVector = new ArrayRealVector(inputList.size());
		for (int i = 0; i < inputList.size(); i++)
			inputVector.setEntry(i, inputList.get(i));
		
		return inputVector;
	}
	
	/**
	 * Output information of observation equation
	 */
	private Future<Void> output(int iweight, int ifreq, int iphase, int icorr) throws IOException {
		// // ステーションの情報の読み込み
//		System.err.println("reading station Information");
//		if (stationSet == null)
//			stationSet = StationInformationFile.read(stationInformationPath);
//		System.err.println(" done");
//		Dvector dVector = eq[iweight][ifreq][iphase].getDVector();
		Callable<Void> output = () -> {
//			outputDistribution(outPaths[iweight][ifreq][iphase].resolve("stationEventDistribution.inf"), iweight, ifreq, iphase);
//			dVector.outOrder(outPath);
//			dVector.outPhases(outPath);
//			outEachTrace(outPath.resolve("trace"));
			UnknownParameterFile.write(outPaths[iweight][ifreq][iphase][icorr].resolve("unknownParameterOrder.inf"),
					eq[iweight][ifreq][iphase][icorr].getParameterList());
			UnknownParameterFile.write(outPaths[iweight][ifreq][iphase][icorr].resolve("originalUnknownParameterOrder.inf"),
					eq[iweight][ifreq][iphase][icorr].getOriginalParameterList());
			if (applyParameterWeight) {
				PrintWriter pw = new PrintWriter(outPaths[iweight][ifreq][iphase][icorr].resolve("parameterWeight.inf").toFile());
				for (double pWeight : parameterWeights)
					pw.println(pWeight);
				pw.close();
			}
//			eq.outputA(outPath.resolve("partial"));
//			eq.outputAtA(outPath.resolve("lmi_AtA.inf"));
//			eq.outputUnkownParameterWeigths(outPath.resolve("unknownParameterWeigths.inf"));
//			dVector.outWeighting(outPath);
			return null;
		};
		FutureTask<Void> future = new FutureTask<>(output);

		new Thread(future).start();
		return future;
	}

	@Override
	public void run() {
		for (int iweight = 0; iweight < weights.length; iweight++) {
			for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
				for (int iphase = 0; iphase < phases.length; iphase++) {
					for (int icorr = 0; icorr < corrections.length; icorr++) {
						try {
							System.err.println("The output folder is " + outPaths[iweight][ifreq][iphase][icorr]);
							Files.createDirectory(outPaths[iweight][ifreq][iphase][icorr]);
							if (property != null)
								writeProperties(outPaths[iweight][ifreq][iphase][icorr].resolve("lmi.properties"));
						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException("Cannot create " + outPaths[iweight][ifreq][iphase][icorr]);
						}
				
						long start = System.nanoTime();
				
						// 観測方程式
						Future<Void> future;
						try {
							future = output(iweight, ifreq, iphase, icorr);
						} catch (Exception e) {
							e.printStackTrace();
							return;
						}
				
						// 逆問題
						solve(iweight, ifreq, iphase, icorr);
						try {
							future.get();
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
						try {
							eq[iweight][ifreq][iphase][icorr].outputSensitivity(outPaths[iweight][ifreq][iphase][icorr].resolve("sensitivity.inf"));
							
							if (model != null) {
								System.out.println("Outputting " + maxNumVector + " velocity perturbation vectors...");
								Path inversionResultPath = outPaths[iweight][ifreq][iphase][icorr];
								VelocityField3D_deprec.outputVelocity(model, perturbationLayerFile, maxNumVector, deltaDegree, inversionResultPath);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						System.err.println("Inversion is done in " + Utilities.toTimeString(System.nanoTime() - start));
					}
				}
			}
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
	public void outEachTrace(Path outPath, int iweight, int ifreq, int iphase, int icorr) throws IOException {
		if (Files.exists(outPath))
			throw new FileAlreadyExistsException(outPath.toString());
		Files.createDirectories(outPath);
		Dvector d = eq[iweight][ifreq][iphase][icorr].getDVector();

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
	public void outEachTrace(Path outPath, RealVector[] vectors, int iweight, int ifreq, int iphase, int icorr) throws IOException {
		// if (outDirectory.exists())
		// return;
		int nTimeWindow = eq[iweight][ifreq][iphase][icorr].getDVector().getNTimeWindow();
		if (vectors.length != nTimeWindow)
			return;
		for (int i = 0; i < nTimeWindow; i++)
			if (vectors[i].getDimension() != eq[iweight][ifreq][iphase][icorr].getDVector().getSynVec()[i].getDimension())
				return;
		Files.createDirectories(outPath);
		for (GlobalCMTID id : eq[iweight][ifreq][iphase][icorr].getDVector().getUsedGlobalCMTIDset()) {
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
		BasicID[] obsIDs = eq[iweight][ifreq][iphase][icorr].getDVector().getObsIDs();
		BasicID[] synIDs = eq[iweight][ifreq][iphase][icorr].getDVector().getSynIDs();
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

	private void solve(int iweight, int ifreq, int iphase, int icorr) {
		inverseMethods.forEach(method -> {
			try {
				if (method == InverseMethodEnum.LEAST_SQUARES_METHOD)
					return; // TODO
				solve(outPaths[iweight][ifreq][iphase][icorr].resolve(method.simple()), method.getMethod(eq[iweight][ifreq][iphase][icorr].getAtA(), eq[iweight][ifreq][iphase][icorr].getAtD())
						, iweight, ifreq, iphase, icorr);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void solve(Path outPath, InverseProblem inverseProblem, int iweight, int ifreq, int iphase, int icorr) throws IOException {
		// invOutDir.mkdir();
		inverseProblem.compute();
//		if (applyParameterWeight)
//			inverseProblem.outputAns(outPath, parameterWeights);
//		else
			inverseProblem.outputAns(outPath);
		outVariance(outPath, inverseProblem, iweight, ifreq, iphase, icorr);
//		outVariancePerEvents(outPath, inverseProblem, iweight, ifreq, iphase); // TODO make it work

		// 基底ベクトルの書き出し SVD: vt, CG: cg ベクトル
		RealMatrix p = inverseProblem.getBaseVectors();
		for (int j = 0; j < eq[iweight][ifreq][iphase][icorr].getMlength(); j++)
			writeDat(outPath.resolve("p" + j + ".txt"), p.getColumn(j));
	}

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		LetMeInvert_fromAtA lmi = new LetMeInvert_fromAtA(Property.parse(args));
		System.err.println(LetMeInvert_fromAtA.class.getName() + " is running.");
		long startT = System.nanoTime();
		lmi.run();
		System.err.println(
				LetMeInvert_fromAtA.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - startT));
	}

	/**
	 * outDirectory下にvarianceを書き込む
	 * 
	 * @param outPath
	 */
	private void outVariance(Path outPath, InverseProblem inverse, int iweight, int ifreq, int iphase, int icorr) throws IOException {
		Path out = outPath.resolve("variance.txt");
		if (Files.exists(out))
			throw new FileAlreadyExistsException(out.toString());
		double[] variance = new double[eq[iweight][ifreq][iphase][icorr].getMlength() + 1];
		variance[0] = residualVariances[iweight][ifreq][iphase][icorr];
		for (int i = 0; i < eq[iweight][ifreq][iphase][icorr].getMlength(); i++)
			variance[i + 1] = eq[iweight][ifreq][iphase][icorr].varianceOf(inverse.getANS().getColumnVector(i), residualVariances[iweight][ifreq][iphase][icorr]
					, obsNorms[iweight][ifreq][iphase][icorr]);
		writeDat(out, variance);
		if (alpha == null)
			return;
		for (int i = 0; i < alpha.length; i++) {
			out = outPath.resolve("aic" + i + ".txt");
			double[] aic = computeAIC(variance, alpha[i], iweight, ifreq, iphase);
			writeDat(out, aic);
		}
		writeDat(outPath.resolve("aic.inf"), alpha);
	}
	
	private void outVariancePerEvents(Path outPath, InverseProblem inverse, int iweight, int ifreq, int iphase, int icorr) throws IOException {
		
		Set<GlobalCMTID> eventSet = eq[iweight][ifreq][iphase][icorr].getDVector().getUsedGlobalCMTIDset();
		Path out = outPath.resolve("eventVariance.txt");
		int n = 31 > eq[iweight][ifreq][iphase][icorr].getMlength() ? eq[iweight][ifreq][iphase][icorr].getMlength() : 31;
		Map<GlobalCMTID, double[]> varianceMap = new HashMap<>();
		for (GlobalCMTID id : eventSet) {
			if (Files.exists(out))
				throw new FileAlreadyExistsException(out.toString());
			double[] variance = new double[n];
			variance[0] = eq[iweight][ifreq][iphase][icorr].getDVector().getEventVariance().get(id);
			RealVector residual = eq[iweight][ifreq][iphase][icorr].getDVector().getD();
			RealVector obsVec = eq[iweight][ifreq][iphase][icorr].getDVector().getObs();
			RealVector mask = eq[iweight][ifreq][iphase][icorr].getDVector().getMask(id);
			
			for (int i = 0; i < residual.getDimension(); i++) {
				residual.setEntry(i, residual.getEntry(i) * mask.getEntry(i));
				obsVec.setEntry(i, obsVec.getEntry(i) * mask.getEntry(i));
			}
			
			for (int i = 0; i < n-1; i++) {
				RealVector adm = eq[iweight][ifreq][iphase][icorr].getA().operate(inverse.getANS().getColumnVector(i));
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
	
	int npts;
	
	/**
	 * 自由度iに対してAICを計算する 独立データは n / alpha 各々のAIC群
	 * 
	 * @param variance
	 *            varianceの列
	 * @param alpha
	 * @return
	 */
	private double[] computeAIC(double[] variance, double alpha, int iweight, int ifreq, int iphase) {
		double[] aic = new double[variance.length];
//		int independentN = (int) (eq[iweight][ifreq][iphase].getDlength() / alpha);
		int independentN = (int) (npts / alpha);
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
		if (!Files.exists(ataPath)) {
			new NoSuchFileException(ataPath.toString()).printStackTrace();
			cango = false;
		}
		if (atdPath != null) {
			if (!Files.exists(atdPath)) {
				cango = false;
				new NoSuchFileException(atdPath.toString()).printStackTrace();
			}
		}
		if (inputPath != null) {
			if (!Files.exists(inputPath)) {
				cango = false;
				new NoSuchFileException(inputPath.toString()).printStackTrace();
			}
		}
		if (!Files.exists(stationInformationPath)) {
			new NoSuchFileException(stationInformationPath.toString()).printStackTrace();
			cango = false;
		}
		if (!Files.exists(unknownParameterListPath)) {
			new NoSuchFileException(unknownParameterListPath.toString()).printStackTrace();
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
	// TODO make it work
	public void outputDistribution(Path outPath, int iweight, int ifreq, int iphase, int icorr) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			BasicID[] obsIDs = eq[iweight][ifreq][iphase][icorr].getDVector().getObsIDs();
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
