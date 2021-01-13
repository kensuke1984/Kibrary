package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionType;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.inversion.addons.HorizontalParameterMapping;
import io.github.kensuke1984.kibrary.inversion.addons.ParameterMapping;
import io.github.kensuke1984.kibrary.inversion.addons.ResampleGrid;
import io.github.kensuke1984.kibrary.inversion.addons.ThreeDParameterMapping;
import io.github.kensuke1984.kibrary.inversion.addons.Weighting;
import io.github.kensuke1984.kibrary.inversion.addons.WeightingType;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.FrequencyRange;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.spc.DSMOutput;
import io.github.kensuke1984.kibrary.util.spc.FormattedSPCFile;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.util.spc.SPCBody;
import io.github.kensuke1984.kibrary.util.spc.SPCComponent;
import io.github.kensuke1984.kibrary.util.spc.SPCFile;
import io.github.kensuke1984.kibrary.util.spc.SPC_SAC;
import io.github.kensuke1984.kibrary.util.spc.ThreeDPartialMaker;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.WaveformDataWriter;
import io.github.kensuke1984.kibrary.util.spc.Spectrum;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.FastMath;

public class AtAMaker implements Operation {
	private Properties PROPERTY;
	
	private Path workPath;
	private Path fpPath;
	private Path bpPath;
	private Path timewindowPath;
	
	private Path[] waveformIDPath;
	private Path[] waveformPath;
	
	private final StaticCorrectionType[] correctionTypes;
	
	private Set<TimewindowInformation> timewindowInformation;
	private final PartialType[] partialTypes;
	private final SACComponent[] components;
	
	private int partialSamplingHz;
	private int[] ext;
	private double finalSamplingHz;
	private int[] step;
	
	private ButterworthFilter[] filter;
	private int filterNp;
	
	private boolean backward;
	
	private PolynomialStructure structure;
	
	private int sourceTimeFunction;
	
	private Map<GlobalCMTID, SourceTimeFunction> userSourceTimeFunctions;
	
	private String modelName;
	
	private Path sourceTimeFunctionPath;
	
	private double tlen;
	private int np;
	
	private Path unknownParameterPath;
	private final UnknownParameter[] originalUnknownParameters;
	private final UnknownParameter[] newUnknownParameters;
	private final Set<Double> originalUnkownRadii;
	
	private boolean testBP;
	
	private boolean testFP;
	
	private boolean outPartial;
	
	private final FrequencyRange[] frequencyRanges;
	
	private final WeightingType[] weightingTypes;
	
	private AtAEntry[][][][] ataBuffer;
	
	private AtdEntry[][][][][] atdEntries;
	
	private double[][][][] residualVarianceNumerator;
	
	private double[][][][] residualVarianceDenominator;
	
	private int numberOfBuffers;
	
	private Path[] bufferFiles;
	
	private int[] bufferStartIndex;
	
	private int nOriginalUnknown;
	
	private int nNewUnknown;
	
	private int nWeight;
	
	private int nFreq;
	
	private int n0AtA;
	
	private int n0AtABuffer;
	
	private int nwindowBuffer;
	private int nwindowBufferLastIteration;
	private int nInteration;
	
	private String thetaInfo;
	private double thetamin;
	private double thetamax;
	private double dtheta;
	
	private boolean correctionBootstrap;
	private int nSample;
	
	private int nproc;
	
	private Path outpartialDir;
	
	private int computationFlag;
	
	private boolean quickAndDirty;
	
	private boolean fastCompute;
	
	Path verticalMappingFile;
	Path horizontalMappingFile;
	
	ParameterMapping mapping;
	HorizontalParameterMapping horizontalMapping;
	ThreeDParameterMapping threedMapping;
	
	private int workProgressCounter;
	private int progressStep;
	private int progressStep1D;
	
	private final SPCFile[] bpnames;
	private final SPCFile[] bpnames_PSV;
	private List<SPCFile> fpnames;
	private Map<GlobalCMTID, List<SPCFile>> fpnameMap;
	private List<SPCFile> fpnames_PSV;
	private Map<GlobalCMTID, List<SPCFile>> fpnameMap_PSV;
	
	private double[][][][][] partials;
	
	private Complex[][][][][] partialFreqs;
	
	private double[][][][][][] partialCorrs;
	
	public final BasicID[][] basicIDArray;
	
	private WaveformDataWriter[] writers;
	
	private boolean threeD;
	
	/* (non-Javadoc)
	 * @see io.github.kensuke1984.kibrary.Operation#getWorkPath()
	 */
	@Override
	public Path getWorkPath() {
		return workPath;
	}
	
	/* (non-Javadoc)
	 * @see io.github.kensuke1984.kibrary.Operation#getProperties()
	 */
	@Override
	public Properties getProperties() {
		return PROPERTY;
	}
	
	/**
	 * @param property
	 * @throws IOException
	 */
	public AtAMaker(Properties property) throws IOException {
		this.PROPERTY = (Properties) property.clone();
		
		checkAndPutDefaults();
		workPath = Paths.get(PROPERTY.getProperty("workPath"));
		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		
		mode = PROPERTY.getProperty("mode").trim().toUpperCase();
		if (!(mode.equals("SH") || mode.equals("PSV") || mode.equals("BOTH")))
				throw new RuntimeException("Error: mode should be one of the following: SH, PSV, BOTH");
		System.out.println("Using mode " + mode);
		
		//frequency ranges
		double[] minFreqs = Stream.of(PROPERTY.getProperty("minFreq").trim().split("\\s+")).mapToDouble(Double::parseDouble).toArray();
		double[] maxFreqs = Stream.of(PROPERTY.getProperty("maxFreq").trim().split("\\s+")).mapToDouble(Double::parseDouble).toArray();
		if (minFreqs.length != maxFreqs.length)
			throw new RuntimeException("Error: number of entries for minFreq and maxFreq differ");
		frequencyRanges = new FrequencyRange[minFreqs.length];
		for (int i = 0; i < minFreqs.length; i++) {
			frequencyRanges[i] = new FrequencyRange(minFreqs[i], maxFreqs[i]);
			System.out.println(frequencyRanges[i]);
		}
		
		//weighting types
		weightingTypes = Stream.of(PROPERTY.getProperty("weightingTypes").trim().split("\\s+")).map(type -> WeightingType.valueOf(type))
			 	.collect(Collectors.toList()).toArray(new WeightingType[0]);
			if (weightingTypes.length < 0)
				throw new IllegalArgumentException("Error: weightingTypes must be set");
//			if (weightingTypes.length > 1)
//				throw new IllegalArgumentException("Error: only 1 weighting type can be set now");
			
		//sac components
		components = Arrays.stream(PROPERTY.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toList()).toArray(new SACComponent[0]);
		
		//partial types
		partialTypes = Arrays.stream(PROPERTY.getProperty("partialTypes").split("\\s+")).map(PartialType::valueOf)
				.collect(Collectors.toSet()).toArray(new PartialType[0]);
		
		System.out.print("Using partial types: ");
		for (PartialType type : partialTypes)
			System.out.print(type + " ");
		System.out.println();
		
		//correction types
		correctionBootstrap = Boolean.parseBoolean(PROPERTY.getProperty("correctionBootstrap"));
		
		if (!correctionBootstrap) {
			correctionTypes = Stream.of(PROPERTY.getProperty("correctionTypes").trim().split("\\s+")).map(s -> StaticCorrectionType.valueOf(s.trim()))
				.collect(Collectors.toList()).toArray(new StaticCorrectionType[0]);
		}
		else {
			correctionTypes = new StaticCorrectionType[nSample];
			for (int isample = 0; isample < nSample; isample++) {
				correctionTypes[isample] = StaticCorrectionType.valueOf(String.format("RND%4d", isample));
			}
		}
		
		//bpnames
		bpPath = getPath("bpPath");
		bpnames = Utilities.collectOrderedSHSpcFileName(bpPath).toArray(new FormattedSPCFile[0]);
		if (mode.equals("PSV") || mode.equals("BOTH"))
			bpnames_PSV = Utilities.collectOrderedPSVSpcFileName(bpPath).toArray(new FormattedSPCFile[0]);
		else
			bpnames_PSV = null;
		
		//basicIDs
		computationFlag = Integer.parseInt(PROPERTY.getProperty("computationFlag"));
		
		if (computationFlag != 3) {
			rootWaveformPath = Paths.get(PROPERTY.getProperty("rootWaveformPath").trim());
			
			if (!correctionBootstrap) {
				if (PROPERTY.getProperty("rootWaveformPath").trim().equals(".")) {
					waveformIDPath = Stream.of(PROPERTY.getProperty("waveformIDPath").trim().split("\\s+")).map(s -> Paths.get(s))
							.collect(Collectors.toList()).toArray(new Path[0]);
					waveformPath = Stream.of(PROPERTY.getProperty("waveformPath").trim().split("\\s+")).map(s -> Paths.get(s))
							.collect(Collectors.toList()).toArray(new Path[0]);
				}
				else {
					waveformIDPath = Stream.of(PROPERTY.getProperty("waveformIDPath").trim().split("\\s+")).map(s -> rootWaveformPath.resolve(s))
							.collect(Collectors.toList()).toArray(new Path[0]);
					waveformPath = Stream.of(PROPERTY.getProperty("waveformPath").trim().split("\\s+")).map(s -> rootWaveformPath.resolve(s))
								.collect(Collectors.toList()).toArray(new Path[0]);
				}
			}
			else {
				waveformIDPath = new Path[nSample];
				waveformPath = new Path[nSample];
				for (int isample = 0; isample < nSample; isample++) {
					waveformIDPath[isample] = rootWaveformPath.resolve(String.format("waveformID_RND%4d.dat", isample));
					waveformPath[isample] = rootWaveformPath.resolve(String.format("waveform_RND%4d.dat", isample));
				}
			}
			
			basicIDArray = new BasicID[waveformIDPath.length][];
			for (int i = 0; i < waveformIDPath.length; i++) {
				basicIDArray[i] = BasicIDFile.read(waveformIDPath[i], waveformPath[i]);
			}
		}
		else {
			basicIDArray = null;
		}
		
		// unknowns
		unknownParameterPath = getPath("unknownParameterPath");
		resamplingRate = Integer.parseInt(PROPERTY.getProperty("resamplingRate"));
		if (PROPERTY.containsKey("verticalMappingFile"))
			verticalMappingFile = Paths.get(PROPERTY.getProperty("verticalMappingFile").trim());
		else
			verticalMappingFile = null;
		if (PROPERTY.containsKey("horizontalMappingFile"))
			horizontalMappingFile = Paths.get(PROPERTY.getProperty("horizontalMappingFile").trim());
		else
			horizontalMappingFile = null;
		
		numberOfBuffers = Integer.parseInt(PROPERTY.getProperty("numberOfBuffers"));
		
			List<UnknownParameter> targetUnknowns = UnknownParameterFile.read(unknownParameterPath);
			List<Double> lats = targetUnknowns.stream().map(p -> p.getLocation().getLatitude()).distinct().collect(Collectors.toList());
			List<Double> lons = targetUnknowns.stream().map(p -> p.getLocation().getLatitude()).distinct().collect(Collectors.toList());
			Collections.sort(lats);
			Collections.sort(lons);
			double dlat = Math.abs(lats.get(1) - lats.get(0));
			double dlon = Math.abs(lons.get(1) - lons.get(0));
			System.out.println("Target grid increments lat, lon = " + dlat + ", " + dlon);
			
			if (resamplingRate >= 1) {
				ResampleGrid sampler = new ResampleGrid(targetUnknowns, dlat, dlon, resamplingRate);
				originalUnknownParameters = sampler.getResampledUnkowns().toArray(new UnknownParameter[0]);
				originalHorizontalPositions = Stream.of(originalUnknownParameters).map(p -> p.getLocation().toHorizontalPosition()).distinct()
						.collect(Collectors.toList());
				originalUnkownRadii = targetUnknowns.stream().map(p -> p.getLocation().getR())
						.collect(Collectors.toSet());
				
				if (verticalMappingFile == null) throw new RuntimeException("Please set a verticalMappingFile");
				System.out.println("Using 3-D mapping with resampler: " + verticalMappingFile + " " + resamplingRate);
				threedMapping = new ThreeDParameterMapping(sampler, verticalMappingFile);
				newUnknownParameters = threedMapping.getNewUnknowns();
				horizontalMapping = null;
			}
			else if (verticalMappingFile != null && horizontalMappingFile != null) {
				originalUnknownParameters = targetUnknowns.toArray(new UnknownParameter[0]);
				originalHorizontalPositions = Stream.of(originalUnknownParameters).map(p -> p.getLocation().toHorizontalPosition()).distinct()
						.collect(Collectors.toList());
				originalUnkownRadii = targetUnknowns.stream().map(p -> p.getLocation().getR())
						.collect(Collectors.toSet());
				System.out.println("Using 3-D mapping with " + verticalMappingFile + " " + horizontalMappingFile);
				threedMapping = new ThreeDParameterMapping(horizontalMappingFile, verticalMappingFile, originalUnknownParameters);
				newUnknownParameters = threedMapping.getNewUnknowns();
			}
			else {
				System.out.println("No mapping");
				originalUnknownParameters = targetUnknowns.toArray(new UnknownParameter[0]);
				mapping = new ParameterMapping(originalUnknownParameters);
				newUnknownParameters = mapping.getUnknowns();
				horizontalMapping = null;
				threedMapping = null;
				originalUnkownRadii = targetUnknowns.stream().map(p -> p.getLocation().getR())
						.collect(Collectors.toSet());
				originalHorizontalPositions = Stream.of(originalUnknownParameters).map(p -> p.getLocation().toHorizontalPosition()).distinct()
						.collect(Collectors.toList());
			}
//		else {
//			originalUnknownParameters = UnknownParameterFile.read(unknownParameterPath).toArray(new UnknownParameter[0]);
//			newUnknownParameters = originalUnknownParameters;
//			originalHorizontalPositions = null;
//			originalUnkownRadii = Arrays.stream(originalUnknownParameters).map(p -> p.getLocation().getR())
//					.collect(Collectors.toSet());
//		}
		
		nOriginalUnknown = originalUnknownParameters.length;
		nNewUnknown = newUnknownParameters.length;
		
		n0AtA = nNewUnknown * (nNewUnknown + 1) / 2;
		int ntmp = n0AtA / numberOfBuffers;
		n0AtABuffer = n0AtA - ntmp * (numberOfBuffers - 1);
		bufferStartIndex = new int[numberOfBuffers];
		for (int i = 0; i < numberOfBuffers; i++)
			bufferStartIndex[i] = i * ntmp;
		
		set();
	}
	
	/**
	 * @param args
	 *            [parameter file name]
	 */
	public static void main(String[] args) throws IOException {
		AtAMaker atam = new AtAMaker(Property.parse(args));
		long startTime = System.nanoTime();

		System.out.println(AtAMaker.class.getName() + " is going..");
		
		// verify memory requirements
//		AtAEntry entry = new AtAEntry();
//		long size = SizeOf.sizeOf(entry);
//		System.out.println("Size of one AtAEntry = " + SizeOf.humanReadable(size));
//		System.out.println("Size of the AtA matrix = (nWeightingType * nFrequencyRanges * nUnknowns * 30 (buffer for different phases))^2");
		//
		
		atam.run();
		
		System.out.println(AtAMaker.class.getName() + " finished in "
				+ Utilities.toTimeString(System.nanoTime() - startTime));
	}
	
	private Path rootWaveformPath;
	
	private Path[] partialIDPaths;
	
	private Path[] partialPaths;
	
	private boolean catalogueFP;
	
	private int resamplingRate;
	
	private String mode;
	
	/**
	 * @throws IOException
	 */
	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(AtAMaker.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan AtAMaker");
			pw.println("##Number of processors used for parallel computation (1)");
			pw.println("#nproc");
			pw.println("##Path of a working folder (.)");
			pw.println("#workPath");
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("#rootWaveformSpcPath");
			pw.println("##Path of the root folder for waveformIDPath and waveformPath. (.)");
			pw.println("#rootWaveformPath");
			pw.println("##Path of waveformID files, must be set. Multiple paths with different static correction can be set, separated by a space");
			pw.println("#waveformIDPath");
			pw.println("##Path a waveform files, must be set. Multiple paths with different static correction can be set, separated by a space");
			pw.println("#waveformPath");
			pw.println("##Static correction type used for corresponding waveform(ID)Path, must be set. Multiple types can be set, separated by a space");
			pw.println("#correctionTypes");
			pw.println("##Mode: PSV, SH, BOTH (SH)");
			pw.println("#mode");
			pw.println("##Path of the back propagate spc catalog folder (BPcat/PREM)");
			pw.println("#bpPath");
			pw.println("##Path of a forward propagate spc folder (FPinfo)");
			pw.println("#fpPath");
			pw.println("##Boolean interpolate FP from catalogue (false)");
			pw.println("#catalogueFP");
			pw.println("##Theta- range and sampling for the BP catalog in the format: thetamin thetamax thetasampling. (1. 50. 2e-2)");
			pw.println("#thetaInfo");
			pw.println("##Boolean use the closest grid point in the catalogue without interpolation (if the catalogue is dense enough) (false)");
			pw.println("#quickAndDirty");
			pw.println("## Consider only 30 deg distance around raypath (false)");
			pw.println("#fastCompute");
			pw.println("##Compute AtA and Atd (1), Atd only (2), or PartialID files (3). (3)");
			pw.println("#computationFlag");
			pw.println("##String if it is PREM spector file is in bpdir/PREM (PREM)");
			pw.println("#modelName");
			pw.println("##Type source time function 0:none, 1:boxcar, 2:triangle. (2)");
			pw.println("##or folder name containing *.stf if you want to your own GLOBALCMTID.stf ");
			pw.println("#sourceTimeFunction");
			pw.println("##Path of a time window file, must be set");
			pw.println("#timewindowPath timewindow.dat");
			pw.println("##Compute types. Can enter multiple values (separated by a space). (MU)");
			pw.println("#partialTypes");
			pw.println("##Path of the unknown parameter file for the target model, must be set");
			pw.println("#unknownParameterPath");
			pw.println("##Weighting scheme for data weighting. Choose among (RECIPROCAL, IDENTITY). Can enter multiple values (separated by a space). (RECIPROCAL)");
			pw.println("#weightingTypes");
			pw.println("##double time length DSM parameter tlen, must be set");
			pw.println("#tlen 3276.8");
			pw.println("##int step of frequency domain DSM parameter np, must be set");
			pw.println("#np 512");
			pw.println("##Compute 3D partials: true; Compute 1D partials: false. (true)");
			pw.println("#threeD");
			pw.println("#=========================================================");
			pw.println("#===================== Filter, Sampling ==================");
			pw.println("##double minimum value of passband (0.005). Can enter multiple values (separated by a space).");
			pw.println("#minFreq");
			pw.println("##double maximum value of passband (0.08). Can enter multiple values (separated by a space).");
			pw.println("#maxFreq");
			pw.println("##The value of np for the filter (4)");
			pw.println("#filterNp");
			pw.println("##Filter if backward filtering is applied (false)");
			pw.println("#backward");
			pw.println("##partialSamplingHz cant change now");
			pw.println("#double (20)");
			pw.println("##double SamplingHz in output dataset (1)");
			pw.println("#finalSamplingHz");
			pw.println("#=========================================================");
			pw.println("#===================== Combine voxels (3D) ===============");
			pw.println("##Int resampling rate of the target model for integration of the partials (1)");
			pw.println("#resamplingRate");
			pw.println("##Path of a file with the mapping to combine unknown parameters, ignored if not set");
			pw.println("#verticalMappingFile");
			pw.println("#horizontalMappingFile");
			pw.println("#=========================================================");
			pw.println("#===================== Options for computation ===========");
			pw.println("##Number of timewindow to store in the (temporary) partial vector (100)");
			pw.println("#nwindowBuffer");
			pw.println("##Number of buffers files for AtA matrix (higher number increases I/0) (1)");
			pw.println("#numberOfBuffers");
			pw.println("#=========================================================");
			pw.println("#===================== Time partials =====================");
			pw.println("##path of the time partials directory, must be set if PartialType containes TIME_SOURCE or TIME_RECEIVER");
			pw.println("#timePartialPath");
			pw.println("##File for Qstructure (if no file, then PREM)");
			pw.println("#qinf");
			pw.println("#=========================================================");
			pw.println("#===================== Bootstrap =========================");
			pw.println("#correctionBootstrap false");
			pw.println("#nSample 100");
			pw.println("#=========================================================");
			pw.println("#===================== Debug =============================");
			pw.println("##Output the back-propagated wavefield as time series (false)");
			pw.println("#testBP");
			pw.println("##Output the forward-propagated wavefield as time series (false)");
			pw.println("#testFP");
			pw.println("##Output the partial as time series (false)");
			pw.println("#outPartial");
			pw.println("##Compute a double difference Kernel (false)");
			pw.println("#doubledifference");
		}
		System.out.println(outPath + " is created.");
	}

	/**
	 * 
	 */
	private void checkAndPutDefaults() {
		if (!PROPERTY.containsKey("workPath"))
			PROPERTY.setProperty("workPath", ".");
		if (!PROPERTY.containsKey("rootWaveformPath"))
			PROPERTY.setProperty("rootWaveformPath", ".");
		if (!PROPERTY.containsKey("components"))
			PROPERTY.setProperty("components", "Z R T");
		if (!PROPERTY.containsKey("bpPath"))
			PROPERTY.setProperty("bpPath", "BPcat/PREM");
		if (!PROPERTY.containsKey("fpPath"))
			PROPERTY.setProperty("fpPath", "FPinfo");
		if (!PROPERTY.containsKey("modelName"))
			PROPERTY.setProperty("modelName", "PREM");
		if (!PROPERTY.containsKey("maxFreq"))
			PROPERTY.setProperty("maxFreq", "0.08");
		if (!PROPERTY.containsKey("minFreq"))
			PROPERTY.setProperty("minFreq", "0.005");
		if (!PROPERTY.containsKey("sourceTimeFunction"))
			PROPERTY.setProperty("sourceTimeFunction", "2");
		if (!PROPERTY.containsKey("partialTypes"))
			PROPERTY.setProperty("partialTypes", "MU");
		if (!PROPERTY.containsKey("partialSamplingHz"))
			PROPERTY.setProperty("partialSamplingHz", "20");
		if (!PROPERTY.containsKey("finalSamplingHz"))
			PROPERTY.setProperty("finalSamplingHz", "1");
		if (!PROPERTY.containsKey("filterNp"))
			PROPERTY.setProperty("filterNp", "4");
		if (!PROPERTY.containsKey("testBP")) PROPERTY.setProperty("testBP", "false");
		if (!PROPERTY.containsKey("testFP")) PROPERTY.setProperty("testFP", "false");
		if (!PROPERTY.containsKey("outPartial")) PROPERTY.setProperty("outPartial", "false");
		if (!PROPERTY.containsKey("weightingTypes")) PROPERTY.setProperty("weightingTypes", "RECIPROCAL");
		if(!PROPERTY.containsKey("thetaInfo")) PROPERTY.setProperty("thetaInfo", "1. 50. 1e-2");
		if(!PROPERTY.containsKey("numberOfBuffers")) PROPERTY.setProperty("numberOfBuffers", "1");
		if(!PROPERTY.containsKey("nproc")) PROPERTY.setProperty("nproc", "1");
		if(!PROPERTY.containsKey("nwindowBuffer")) PROPERTY.setProperty("nwindowBuffer", "100");
		if(!PROPERTY.containsKey("backward")) PROPERTY.setProperty("backward", "false");
		if(!PROPERTY.containsKey("computationFlag")) PROPERTY.setProperty("computationFlag", "3");
		if(!PROPERTY.containsKey("correctionBootstrap")) PROPERTY.setProperty("correctionBootstrap", "false");
		if (!PROPERTY.containsKey("nSample")) PROPERTY.setProperty("nSample", "100");
		if (!PROPERTY.containsKey("catalogueFP")) PROPERTY.setProperty("catalogueFP", "false");
		if (!PROPERTY.containsKey("resamplingRate")) PROPERTY.setProperty("resamplingRate", "1");
		if (!PROPERTY.containsKey("quickAndDirty")) PROPERTY.setProperty("quickAndDirty", "false");
		if (!PROPERTY.containsKey("fastCompute")) PROPERTY.setProperty("fastCompute", "false");
		if (!PROPERTY.containsKey("mode")) PROPERTY.setProperty("mode", "SH");
		if (!PROPERTY.containsKey("threeD")) PROPERTY.setProperty("threeD", "true");
		if (!PROPERTY.containsKey("doubledifference")) PROPERTY.setProperty("doubledifference", "false");
		if (!PROPERTY.containsKey("tlen")) PROPERTY.setProperty("tlen", "3276.8");
		if (!PROPERTY.containsKey("np")) PROPERTY.setProperty("np", "512");
	}

	/**
	 * @throws IOException
	 */
	private void set() throws IOException {
		fpPath = getPath("fpPath");
		timewindowPath = getPath("timewindowPath");
		
		if (PROPERTY.containsKey("qinf"))
			structure = new PolynomialStructure(getPath("qinf"));
		try {
			sourceTimeFunction = Integer.parseInt(PROPERTY.getProperty("sourceTimeFunction"));
		} catch (Exception e) {
			sourceTimeFunction = -1;
			sourceTimeFunctionPath = getPath("sourceTimeFunction");
		}
		modelName = PROPERTY.getProperty("modelName");

		tlen = Double.parseDouble(PROPERTY.getProperty("tlen"));
		np = Integer.parseInt(PROPERTY.getProperty("np"));
		
		partialSamplingHz = 20;
		// =Double.parseDouble(reader.getFirstValue("partialSamplingHz")); TODO

		finalSamplingHz = Double.parseDouble(PROPERTY.getProperty("finalSamplingHz"));
		
		filterNp = Integer.parseInt(PROPERTY.getProperty("filterNp"));
		
		testBP = Boolean.parseBoolean(PROPERTY.getProperty("testBP"));
		
		testFP = Boolean.parseBoolean(PROPERTY.getProperty("testFP"));
		
		outPartial = Boolean.parseBoolean(PROPERTY.getProperty("outPartial"));
		
		nSample = Integer.parseInt(PROPERTY.getProperty("nSample"));
		
		double[] tmpthetainfo = Stream.of(PROPERTY.getProperty("thetaInfo").trim().split("\\s+")).mapToDouble(Double::parseDouble)
				.toArray();
		thetamin = tmpthetainfo[0];
		thetamax = tmpthetainfo[1];
		dtheta = tmpthetainfo[2];
		
		bufferFiles = new Path[numberOfBuffers];
		for (int i = 0; i < numberOfBuffers; i++) {
			Path path = workPath.resolve("ata_tmp_buffer_" + i + ".dat");
			Files.deleteIfExists(path);
			bufferFiles[i] = path;
		}
		
		nWeight = weightingTypes.length;
		
		nFreq = frequencyRanges.length;
		
		nproc = Integer.parseInt(PROPERTY.getProperty("nproc"));
		
		nwindowBuffer = Integer.parseInt(PROPERTY.getProperty("nwindowBuffer"));
		
		outpartialDir = workPath.resolve("partials");
		
		backward = Boolean.parseBoolean(PROPERTY.getProperty("backward"));
		
//		if (computationFlag == 3)
//			partialIDs = new ArrayList<>();
		
		catalogueFP = Boolean.parseBoolean(PROPERTY.getProperty("catalogueFP"));
		
		quickAndDirty = Boolean.parseBoolean(PROPERTY.getProperty("quickAndDirty"));
		
		threeD = Boolean.parseBoolean(PROPERTY.getProperty("threeD"));
		
		fastCompute = Boolean.parseBoolean(PROPERTY.getProperty("fastCompute"));
		
		doubledifference = Boolean.parseBoolean(PROPERTY.getProperty("doubledifference"));
	}
	
	
	Map<Phases, Integer> phaseMap;
	
	AtomicInteger windowCounter;
//	int windowCounter;
	
	AtomicInteger totalWindowCounter;
	
	Phases[] usedPhases;
	
	Map<HorizontalPosition, DSMOutput> bpMap;
	
	private final String stfcatName = "LSTF1.stfcat"; //LSTF1 ASTF1 ASTF2
	private final List<String> stfcat = readSTFCatalogue(stfcatName);
	
	private List<String> readSTFCatalogue(String STFcatalogue) throws IOException {
//		System.out.println("STF catalogue: " +  STFcatalogue);
		return IOUtils.readLines(AtAMaker.class.getClassLoader().getResourceAsStream(STFcatalogue)
					, Charset.defaultCharset());
	}
	
	private Path logfile;
	
	private TimewindowInformation[] timewindowOrder;
	
	private final int bufferMargin = 10;
	
	private boolean doubledifference;
	
	/* (non-Javadoc)
	 * @see io.github.kensuke1984.kibrary.Operation#run()
	 */
	@Override
	public void run() throws IOException {
		if (fastCompute)
			System.out.println("Using fast compute mode");
		
		setTimewindows();
		setBandPassFilter();
		
//		setUnknownParameters();
//		setUnknownParametersFromSampler();
		if (computationFlag != 3) {
//			setWaveformData();
			canGO();
		}
		
		totalWindowCounter = new AtomicInteger();
		
		String tempString = Utilities.getTemporaryString();
		
//		logfile = workPath.resolve("atam" + tempString + ".log");
//		Files.createFile(logfile);
		
		partialIDPaths = new Path[nFreq];
		partialPaths = new Path[nFreq];
		for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
			partialPaths[ifreq] = workPath.resolve("partial_" + frequencyRanges[ifreq] +"_" + tempString + ".dat");
			partialIDPaths[ifreq] = workPath.resolve("partialID_" + frequencyRanges[ifreq] +"_" + tempString + ".dat");
		}
		
		Path outUnknownPath = workPath.resolve("newUnknowns" + tempString + ".inf");
		Path outOriginalUnknownPath = workPath.resolve("originalUnknowns" + tempString + ".inf");
		Path outLayerPath = workPath.resolve("newPerturbationLayers" + tempString + ".inf");
		Path outHorizontalPoints = workPath.resolve("newHorizontalPositions" + tempString + ".inf");
		outputUnknownParameters(outUnknownPath, newUnknownParameters);
		outputUnknownParameters(outOriginalUnknownPath, originalUnknownParameters);
		outputPerturbationLayers(outLayerPath);
		outputHorizontalPoints(outHorizontalPoints, originalHorizontalPositions);
		
		// redefine nwindowBuffer so that it divides timewindowInformation.size()
		nInteration = timewindowInformation.size() / nwindowBuffer;
		int newNwindowBuffer = nInteration == 0 ? timewindowInformation.size() : timewindowInformation.size() / nInteration;
		nwindowBufferLastIteration = timewindowInformation.size() - nInteration * newNwindowBuffer;
		System.out.println("nWindowBuffer (new, new_lastIteration, previous) = " + newNwindowBuffer + " "
				+ nwindowBufferLastIteration + " " + nwindowBuffer);
		nwindowBuffer = newNwindowBuffer + bufferMargin;
		
		int iterationCount = 0;
		
		timewindowOrder = new TimewindowInformation[nwindowBuffer];
		
		usedPhases = timewindowInformation.stream().map(tw -> new Phases(tw.getPhases()))
			.collect(Collectors.toSet()).toArray(new Phases[0]);
		phaseMap = new HashMap<>();
		for (int i = 0; i < usedPhases.length; i++) {
			phaseMap.put(usedPhases[i], i);
		}
		
		Path outpartialDir = workPath.resolve("partials");
		if (outPartial)
			Files.createDirectories(outpartialDir);
		
		//--- initialize Atd
		if (computationFlag == 1 || computationFlag == 2) {
			atdEntries = new AtdEntry[nOriginalUnknown][][][][];
			for (int i = 0; i < nOriginalUnknown; i++) {
				atdEntries[i] = new AtdEntry[nWeight][][][];
				for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
					atdEntries[i][iweight] = new AtdEntry[nFreq][][];
					for (int ifreq = 0; ifreq < nFreq; ifreq++) {
						atdEntries[i][iweight][ifreq] = new AtdEntry[usedPhases.length][];
						for (int iphase = 0; iphase < usedPhases.length; iphase++) {
							atdEntries[i][iweight][ifreq][iphase] = new AtdEntry[correctionTypes.length];
							for (int icorr = 0; icorr < correctionTypes.length; icorr++) {
								atdEntries[i][iweight][ifreq][iphase][icorr] = new AtdEntry(weightingTypes[iweight], frequencyRanges[ifreq]
									, usedPhases[iphase], correctionTypes[icorr], originalUnknownParameters[i].getPartialType(), originalUnknownParameters[i].getLocation(), 0.);
							}
						}
					}
				}
			}
		}
		else
			atdEntries = null;
		
		ext = new int[frequencyRanges.length];
		step = new int[frequencyRanges.length];
		for (int i = 0; i < frequencyRanges.length; i++) {
			// バンドパスを安定させるためwindowを左右に ext = max period(s) ずつ伸ばす
			double minFreq = frequencyRanges[i].getMinFreq();
			ext[i] = (int) (1. / minFreq * partialSamplingHz);

			// sacdataを何ポイントおきに取り出すか
			step[i] = (int) (partialSamplingHz / finalSamplingHz);
		}
		
		if (computationFlag == 1 || computationFlag == 2) {
			//compute data variance
			computeVariance();
			//write data variance
			System.out.println("Writing residual variance...");
			Path outpath =  workPath.resolve("residualVariance" +  tempString + ".dat");
			ResidualVarianceFile.write(outpath, residualVarianceNumerator, residualVarianceDenominator, weightingTypes
					, frequencyRanges, usedPhases, correctionTypes, npts);
		}
		
		Set<GlobalCMTID> usedEvents = timewindowInformation.stream()
				.map(tw -> tw.getGlobalCMTID())
				.collect(Collectors.toSet());
		
		Set<Station> usedStations = timewindowInformation.stream()
				.map(tw -> tw.getStation())
				.collect(Collectors.toSet());
		
		List<GlobalCMTID> usedEventList = usedEvents.stream().collect(Collectors.toList());
		List<Station> usedStationList = usedStations.stream().collect(Collectors.toList());
		
		//--- initialize partials writer
		Set<Phase> tmpPhases = new HashSet<>();
		for (Phases ps : usedPhases)
			ps.toSet().stream().forEach(p -> tmpPhases.add(p));
		Phase[] phaseArray = tmpPhases.toArray(new Phase[0]);
		Set<Location> perturbationPoints = Stream.of(newUnknownParameters).map(p -> p.getLocation()).collect(Collectors.toSet());
		writers = new WaveformDataWriter[frequencyRanges.length];
		for (int i=0; i < frequencyRanges.length; i++) {
			double[][] periodRanges = new double[][] { {1./frequencyRanges[i].getMaxFreq(), 1./frequencyRanges[i].getMinFreq()} };
			writers[i] = new WaveformDataWriter(partialIDPaths[i], partialPaths[i], usedStations, usedEvents, periodRanges, phaseArray, perturbationPoints);
		}
		
		//--- initialize source time functions
		setSourceTimeFunctions(usedEvents);
		
		//--- for parallel computations
		int availabelProc = Runtime.getRuntime().availableProcessors();
		if (availabelProc < nproc)
			throw new RuntimeException("Insuficcient number of available processors " + nproc + " " + availabelProc);
		int N_THREADS = nproc;
		ExecutorService execs = Executors.newFixedThreadPool(N_THREADS);
		List<Callable<Object>> todo = new ArrayList<Callable<Object>>();
		
		//--- initialize counters
		windowCounter = new AtomicInteger(0);
		workProgressCounter = 0;
		progressStep = (int) (originalUnknownParameters.length * nwindowBuffer / 100.);
		if (progressStep == 0) progressStep = 1;
		
		progressStep1D = (int) (nwindowBuffer / 100.);
		if (progressStep1D == 0) progressStep1D = 1;
		
		//--- initialize partials
		partials = new double[nOriginalUnknown][][][][];
		for (int i = 0; i < nOriginalUnknown; i++) {
			partials[i] = new double[nWeight][][][];
			for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
				partials[i][iweight] = new double[nFreq][][];
				for (int ifreq = 0; ifreq < nFreq; ifreq++) {
					partials[i][iweight][ifreq] = new double[nwindowBuffer][];
//					for (int iphase = 0; iphase < usedPhases.length; iphase++) {
//						partials[i][iweight][ifreq][iphase] = new double[nwindowBuffer][];
						for (int iwin = 0; iwin < nwindowBuffer; iwin++) {
							partials[i][iweight][ifreq][iwin] = new double[0];
						}
//					}
				}
			}
		}
		
		//--- initialize partialFreqs
				partialFreqs = new Complex[nOriginalUnknown][][][][];
				for (int i = 0; i < nOriginalUnknown; i++) {
					partialFreqs[i] = new Complex[nWeight][][][];
					for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
						partialFreqs[i][iweight] = new Complex[nFreq][][];
						for (int ifreq = 0; ifreq < nFreq; ifreq++) {
							partialFreqs[i][iweight][ifreq] = new Complex[nwindowBuffer][];
//							for (int iphase = 0; iphase < usedPhases.length; iphase++) {
//								partials[i][iweight][ifreq][iphase] = new double[nwindowBuffer][];
								for (int iwin = 0; iwin < nwindowBuffer; iwin++) {
									partialFreqs[i][iweight][ifreq][iwin] = new Complex[0];
								}
//							}
						}
					}
				}
		
		//--- initialize fpnameMaps
		fpnameMap = Utilities.collectMapOfOrderedSHFpFileName(fpPath, modelName);
		if (mode.equals("PSV") || mode.equals("BOTH")) {
			fpnameMap_PSV = Utilities.collectMapOfOrderedPSVFpFileName(fpPath, modelName);
		}
		
		for (GlobalCMTID event : usedEventList) {
			Set<TimewindowInformation> eventTimewindows = timewindowInformation.stream()
					.filter(tw -> tw.getGlobalCMTID().equals(event))
					.collect(Collectors.toSet());
			
			Set<Station> eventStations = eventTimewindows.stream().filter(tw -> tw.getGlobalCMTID().equals(event))
				.map(tw -> tw.getStation()).collect(Collectors.toSet());
			
			List<List<Integer>> IndicesEventBasicID = new ArrayList<>();
			if (computationFlag == 1 || computationFlag == 2) {
				for (int icorr = 0; icorr < correctionTypes.length; icorr++) {
					final int finalIcorr = icorr;
					List<Integer> tmplist = IntStream.range(0, basicIDArray[icorr].length).filter(i -> basicIDArray[finalIcorr][i].getGlobalCMTID().equals(event))
							.boxed().collect(Collectors.toList());
					IndicesEventBasicID.add(tmplist);
				}
			}
			
			Path fpEventPath = fpPath.resolve(event.toString()).resolve(modelName);
//			List<SPCFile> fpnames = Utilities.collectOrderedSpcFileName(fpEventPath);
			fpnames = Utilities.collectOrderedSHSpcFileName(fpEventPath);
			if (mode.equals("PSV") || mode.equals("BOTH")) {
				fpnames_PSV = Utilities.collectOrderedPSVSpcFileName(fpEventPath);
			}
			
			for (Station station : eventStations) {
				System.out.println("Working for " + event + " " + station);
				
				Set<TimewindowInformation> recordTimewindows = eventTimewindows.stream().filter(tw -> tw.getStation().equals(station))
						.collect(Collectors.toSet());
				
				List<List<Integer>> IndicesRecordBasicID = new ArrayList<>();
				if (computationFlag == 1 || computationFlag == 2) {
					for (int icorr = 0; icorr < correctionTypes.length; icorr++) {
						final int finalIcorr = icorr;
						List<Integer> tmplist = IndicesEventBasicID.get(icorr).stream().filter(i -> basicIDArray[finalIcorr][i].getStation().equals(station))
								.collect(Collectors.toList());
						IndicesRecordBasicID.add(tmplist);
					}
				}
				
				List<TimewindowInformation> orderedRecordTimewindows = new ArrayList<>();
				for (SACComponent component : components) {
					for (TimewindowInformation timewindow : recordTimewindows) {
						if (timewindow.getComponent().equals(component))
							orderedRecordTimewindows.add(timewindow);
					}
				}
				
				//--- compute partials
//				System.out.println("Computing partials...");
				synchronized (this) {
					int currentWindowCounter = windowCounter.get();
					
	//				System.out.println(currentWindowCounter + " " + iterationCount + " " + nInteration + " " + nwindowBufferLastIteration);
					
//					if (threeD) {
						if (catalogueFP) {
							System.out.println("FP catalogue");
							for (HorizontalPosition position : originalHorizontalPositions) {
								if (fastCompute) {
									Location fpSourceLoc = event.getEvent().getCmtLocation();
									HorizontalPosition bpSourceLoc = station.getPosition();
									double distanceFP = fpSourceLoc.getEpicentralDistance(position);
									double az = fpSourceLoc.getAzimuth(bpSourceLoc) - fpSourceLoc.getAzimuth(position);
									double d = Math.toDegrees(Math.asin(distanceFP * Math.sin(az)));
									System.out.println(d);
									if (d < 10.)
										todo.add(Executors.callable(new FPWorker(position, station, event, IndicesRecordBasicID, orderedRecordTimewindows, currentWindowCounter)));
								}
								else
									todo.add(Executors.callable(new FPWorker(position, station, event, IndicesRecordBasicID, orderedRecordTimewindows, currentWindowCounter)));
							}
						}
						else {
							System.out.println("Exact FP");
							for (int ispc = 0; ispc < fpnames.size(); ispc++) {
								if (mode.equals("SH"))
									todo.add(Executors.callable(new FPWorker(fpnames.get(ispc)
											, station, event, IndicesRecordBasicID, orderedRecordTimewindows, currentWindowCounter)));
								else if (mode.equals("PSV"))
									todo.add(Executors.callable(new FPWorker(null, fpnames_PSV.get(ispc)
											, station, event, IndicesRecordBasicID, orderedRecordTimewindows, currentWindowCounter)));
								else if (mode.equals("BOTH")) {
									HorizontalPosition position = fpnames.get(ispc).read().getObserverPosition();
									if (fastCompute) {
										Location fpSourceLoc = event.getEvent().getCmtLocation();
										HorizontalPosition bpSourceLoc = station.getPosition();
										double distanceFP = fpSourceLoc.getEpicentralDistance(position);
										double az = fpSourceLoc.getAzimuth(bpSourceLoc) - fpSourceLoc.getAzimuth(position);
										double d = Math.toDegrees(Math.asin(distanceFP * Math.sin(az)));
//										System.out.println(d);
										if (Math.abs(d) < 10.)
											todo.add(Executors.callable(new FPWorker(fpnames.get(ispc), fpnames_PSV.get(ispc)
													, station, event, IndicesRecordBasicID, orderedRecordTimewindows, currentWindowCounter)));
									}
									else
										todo.add(Executors.callable(new FPWorker(fpnames.get(ispc), fpnames_PSV.get(ispc)
												, station, event, IndicesRecordBasicID, orderedRecordTimewindows, currentWindowCounter)));
//									todo.add(Executors.callable(new FPWorker(fpnames.get(ispc), fpnames_PSV.get(ispc)
//											, station, event, IndicesRecordBasicID, orderedRecordTimewindows, currentWindowCounter)));
								}
							}
						}
//					}
//					else {
//						todo.add(Executors.callable(
//								new worker1D(station, event, IndicesRecordBasicID
//										, orderedRecordTimewindows, currentWindowCounter)));
//					}
					
//					if (orderedRecordTimewindows.size() != 1)
//						throw new RuntimeException("More than one timewindow");
					
					for (int itmp = 0; itmp < orderedRecordTimewindows.size(); itmp++) {
						timewindowOrder[windowCounter.getAndIncrement()] = orderedRecordTimewindows.get(itmp);
					}
					totalWindowCounter.addAndGet(orderedRecordTimewindows.size());
				}
				
				System.out.println(windowCounter.get() + " " + totalWindowCounter.get() + " " + timewindowInformation.size());
				
				if ( (windowCounter.get() >= nwindowBuffer - bufferMargin && iterationCount < nInteration) 
						|| (timewindowInformation.size() - totalWindowCounter.get() == 0 && iterationCount == nInteration) ) {
					int nwindowfill = nwindowBuffer;
					if (windowCounter.get() == nwindowBufferLastIteration && iterationCount == nInteration)
						nwindowfill = nwindowBufferLastIteration;
					
					windowCounter.set(0);
					iterationCount++;
					try {
						System.out.println("Computing " + todo.size() + " tasks");
						long t1i = System.currentTimeMillis();
						List<Future<Object>> answers = execs.invokeAll(todo);
						long t1f = System.currentTimeMillis();
						System.out.println("Completed in " + (t1f-t1i)*1e-3 + " s");
						todo = new ArrayList<>();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				
					//--- compute AtA
					if (computationFlag == 1) {
					if (!testBP) {
						Runtime runtime = Runtime.getRuntime();
						long usedMemory = runtime.totalMemory() - runtime.freeMemory();
						System.out.println("--> Used memory = " + usedMemory*1e-9 + "Gb");
							
						System.out.println("Computing AtA...");
						
						for (int ibuff = 0; ibuff < numberOfBuffers; ibuff++) {
							if (Files.exists(bufferFiles[ibuff])) {
								ataBuffer = AtAFile.read(bufferFiles[ibuff]);
							}
							else {
								int n = ibuff < numberOfBuffers - 1 ? bufferStartIndex[ibuff + 1] : n0AtA;
								ataBuffer = new AtAEntry[n - bufferStartIndex[ibuff]][][][];
								int itmp = 0;
								for (int i0AtA = bufferStartIndex[ibuff]; i0AtA < n; i0AtA++) {
									ataBuffer[itmp] = new AtAEntry[nWeight][][];
									int iunknown = (int) (0.5 * (FastMath.sqrt(1 + 8 * i0AtA) - 1));
									int junknown = i0AtA - iunknown * (iunknown + 1) / 2;
									for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
										ataBuffer[itmp][iweight] = new AtAEntry[nFreq][];
										for (int ifreq = 0; ifreq < nFreq; ifreq++) {
											ataBuffer[itmp][iweight][ifreq] = new AtAEntry[usedPhases.length];
											for (int iphase = 0; iphase < usedPhases.length; iphase++) {
												ataBuffer[itmp][iweight][ifreq][iphase] = new AtAEntry(weightingTypes[iweight], frequencyRanges[ifreq]
														, usedPhases[iphase], newUnknownParameters[iunknown], newUnknownParameters[junknown]);
											}
										}
									}
									itmp++;
								}
							}
							
							usedMemory = runtime.totalMemory() - runtime.freeMemory();
							System.out.println("--> Used memory = " + usedMemory*1e-9 + "Gb");
							
							int n = ibuff < numberOfBuffers - 1 ? bufferStartIndex[ibuff + 1] : n0AtA;
							
//							System.out.println(ibuff + " " + bufferStartIndex[ibuff] + " " + n);
							
							System.out.println(Runtime.getRuntime().availableProcessors() + " available processors for computation of AtA");
							List<Callable<Object>> todo2 = new ArrayList<Callable<Object>>();
							for (int i0AtA = bufferStartIndex[ibuff]; i0AtA < n; i0AtA++) {
								if (!doubledifference)
									todo2.add(Executors.callable(new AtAWorker(bufferStartIndex[ibuff], i0AtA)));
								else
									todo2.add(Executors.callable(new AtADDWorker(bufferStartIndex[ibuff], i0AtA)));
							}
							try {
								System.out.println("Computing " + todo2.size() + " tasks");
								List<Future<Object>> answers = execs.invokeAll(todo2);
							} catch (InterruptedException e) {
									e.printStackTrace();
							}
							
							//--- write AtA
							System.out.println("Writing AtA buffer in " + bufferFiles[ibuff]);
							Files.deleteIfExists(bufferFiles[ibuff]);
							AtAFile.write(ataBuffer, weightingTypes, frequencyRanges, newUnknownParameters, usedPhases, bufferFiles[ibuff]);
							System.out.println("Finished writting");
						} // END AtA buffers loop
					} // END IF test BP
					} // END IF computation flag (compute AtA)
					
					// write partials
					if (computationFlag == 3) {
						System.out.println("Writing partials");
						fillA(timewindowOrder);
						System.out.println("Done!");
					}
					
					//reset partials
					for (int i = 0; i < nOriginalUnknown; i++) {
						for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
							for (int ifreq = 0; ifreq < nFreq; ifreq++) {
//								for (int iphase = 0; iphase < usedPhases.length; iphase++) {
									for (int iwin = 0; iwin < nwindowBuffer; iwin++) {
										partials[i][iweight][ifreq][iwin] = new double[0];
									}
//								}
							}
						}
					}
					
				} // END IF nwindowbuffer reached?
				
			} // END station loop
		} // END event loop
		
		execs.shutdown();
		while (!execs.isTerminated()) {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for (int i = 0; i < writers.length; i++)
			writers[i].close();
		
//		if (writeA)
//			writeA(partialIDPath, partialPath, usedStations, usedEvents);
		
		//--- write Atd
		if (computationFlag == 1 || computationFlag == 2) {
		if (!testBP) {
			System.out.println("Writing Atd...");
			Path outputPath = workPath.resolve("atd" + tempString + ".dat");
			
			List<AtdEntry> atdEntryList = new ArrayList<>();
			for (int i = 0; i < nNewUnknown; i++) {
				for (int iweight = 0; iweight < nWeight; iweight++) {
					for (int ifreq = 0; ifreq < nFreq; ifreq++) {
						for (int iphase = 0; iphase < usedPhases.length; iphase++) {
							for (int icorr = 0; icorr < correctionTypes.length; icorr++) {
								int[] iOriginalUnknowns; 
								
								if (horizontalMapping != null) {
									iOriginalUnknowns = horizontalMapping.getiNewToOriginal(i);
								}
								else if (threedMapping != null) {
									iOriginalUnknowns = threedMapping.getiNewToOriginal(i);
								}
								else {
									iOriginalUnknowns = mapping.getiNewToOriginal(i);
								}
								AtdEntry atdEntry =  atdEntries[iOriginalUnknowns[0]][iweight][ifreq][iphase][icorr];
								for (int k = 1; k < iOriginalUnknowns.length; k++)
									atdEntry.add(atdEntries[iOriginalUnknowns[k]][iweight][ifreq][iphase][icorr]);
	//							atdEntryList.add(atdEntries[i][iweight][ifreq][iphase]);
								atdEntryList.add(atdEntry);
							}
						}
					}
				}
			}
			
			AtdFile.write(atdEntryList, weightingTypes, frequencyRanges, partialTypes, correctionTypes, outputPath);
		}
		}
		
		//END
	}
	
	private void writeParialCorr(Path rootPath) throws IOException {
		int nsta = timewindowInformation.size();
		
		for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
			Path dir0 = rootPath.resolve(weightingTypes[iweight].name());
			Files.createDirectory(dir0);
			for (int ifreq = 0; ifreq < nFreq; ifreq++) {
				Path dir1 = dir0.resolve(frequencyRanges[ifreq].toString());
				Files.createDirectory(dir1);
				for (int ista = 0; ista < nsta; ista++) {
					Station stationI = timewindowOrder[ista].getStation();
					for (int jsta = 0; jsta < nsta; jsta++) {
						Station stationJ = timewindowOrder[jsta].getStation();
						Path outpath = dir1.resolve("partial_"  + stationI.getName() 
							+ "_" + stationJ.getName() + ".txt");
						PrintWriter pw = new PrintWriter(outpath.toFile());
						for (int i = 0; i < nOriginalUnknown; i++) {
							double[] partialCorr = partialCorrs[i][iweight][ifreq][ista][jsta];
							Location loc = originalUnknownParameters[i].getLocation();
							PartialType type = originalUnknownParameters[i].getPartialType();
							pw.print(type + " " + loc + " ");
							for (int it = 0; it < partialCorr.length; it++) {
								pw.print(partialCorr[it] + " ");
							}
							pw.println();
						}
						pw.close();
					}
				}
			}
		}
	}
	
	private void fillA(TimewindowInformation[] timewindows) {
		try {
			for (int iunknown = 0; iunknown < nNewUnknown; iunknown++) {
				int[] iOriginalUnknowns; 
				
				if (verticalMappingFile != null) {
					iOriginalUnknowns = threedMapping.getiNewToOriginal(iunknown);
				}
				else {
					iOriginalUnknowns = mapping.getiNewToOriginal(iunknown);
				}
				
				for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
					for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
							for (int iwin = 0; iwin < nwindowBuffer; iwin++) {
								if (partials[0][iweight][ifreq][iwin].length == 0)
									continue;
								
								TimewindowInformation window = timewindows[iwin];
								Phases phases = new Phases(window.getPhases());
								Phase[] phaseArray = phases.toSet().toArray(new Phase[0]);
								
								int it = partials[0][iweight][ifreq][iwin].length;
//								int it = (int) (window.getLength() * finalSamplingHz); 
//								if (it > 0) {
									double[] partiali = new double[it];
									for (int iOriginal = 0; iOriginal < iOriginalUnknowns.length; iOriginal++) {
										if (partials[iOriginalUnknowns[iOriginal]][iweight][ifreq][iwin].length > 0)
											for (int k = 0; k < it; k++) {
												partiali[k] += partials[iOriginalUnknowns[iOriginal]][iweight][ifreq][iwin][k];
											}
									}
									
									PartialType type = newUnknownParameters[iunknown].getPartialType();
									if (!threeD)
										type = to1D(type);
									
									PartialID partialID = new PartialID(window.getStation(), window.getGlobalCMTID(), window.getComponent()
											, finalSamplingHz, window.getStartTime(), it, 1./frequencyRanges[ifreq].getMaxFreq()
											, 1./frequencyRanges[ifreq].getMinFreq(), phaseArray, 0, true, newUnknownParameters[iunknown].getLocation()
											, type, partiali);
									
									writers[ifreq].addPartialID(partialID);
//								}
//								else
//									System.out.println(0);
							}
					}
				}
			}
			
			for (int iw = 0; iw < writers.length; iw++)
				writers[iw].flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Deprecated
	private void fillA1D(TimewindowInformation[] timewindows) {
		try {
			for (int iunknown = 0; iunknown < nNewUnknown; iunknown++) {
				for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
					for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
							for (int iwin = 0; iwin < nwindowBuffer; iwin++) {
								if (partials[0][iweight][ifreq][iwin].length == 0)
									continue;
								
								TimewindowInformation window = timewindows[iwin];
								Phases phases = new Phases(window.getPhases());
								Phase[] phaseArray = phases.toSet().toArray(new Phase[0]);
								
								int it = partials[0][iweight][ifreq][iwin].length;
								if (it > 0) {
									double[] partiali = partials[iunknown][iweight][ifreq][iwin];
									
									PartialID partialID = new PartialID(window.getStation(), window.getGlobalCMTID(), window.getComponent()
											, finalSamplingHz, window.getStartTime(), it, 1./frequencyRanges[ifreq].getMaxFreq()
											, 1./frequencyRanges[ifreq].getMinFreq(), phaseArray, 0, true, newUnknownParameters[iunknown].getLocation()
											, newUnknownParameters[iunknown].getPartialType(), partiali);
									
									writers[ifreq].addPartialID(partialID);
								}
								else
									System.out.println(0);
							}
					}
				}
			}
			
			for (int iw = 0; iw < writers.length; iw++)
				writers[iw].flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int getiWeight(WeightingType type) {
		for (int i = 0; i < weightingTypes.length; i++)
			if (type.equals(weightingTypes[i]))
				return i;
		return -1;
	}
	
	private int getiFreq(FrequencyRange range) {
		for (int i = 0; i < frequencyRanges.length; i++)
			if (range.equals(frequencyRanges[i]))
				return i;
		return -1;
	}
	
	private int getiPhase(Phases phases) {
		return phaseMap.get(phases);
	}
	
	private int npts;
	
	private void computeVariance() {
		npts = 0;
		
		residualVarianceNumerator = new double[nWeight][][][];
		residualVarianceDenominator = new double[nWeight][][][];
		for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
			residualVarianceNumerator[iweight] = new double[nFreq][][];
			residualVarianceDenominator[iweight] = new double[nFreq][][];
			for (int ifreq = 0; ifreq < nFreq; ifreq++) {
				residualVarianceNumerator[iweight][ifreq] = new double[usedPhases.length][];
				residualVarianceDenominator[iweight][ifreq] = new double[usedPhases.length][];
				for (int iphase = 0; iphase < usedPhases.length; iphase++) {
					residualVarianceNumerator[iweight][ifreq][iphase] = new double[correctionTypes.length];
					residualVarianceDenominator[iweight][ifreq][iphase] = new double[correctionTypes.length];
					for (int icorr = 0; icorr < correctionTypes.length; icorr++) {
						residualVarianceNumerator[iweight][ifreq][iphase][icorr] = 0.;
						residualVarianceDenominator[iweight][ifreq][iphase][icorr] = 0.;
					}
				}
			}
		}
		
		for (int icorr = 0; icorr < correctionTypes.length; icorr++) {
			List<BasicID> obsIDs = Stream.of(basicIDArray[icorr]).filter(id ->  id.getWaveformType().equals(WaveformType.OBS)).collect(Collectors.toList());
			List<BasicID> tmpSyns = Stream.of(basicIDArray[icorr]).filter(id -> id.getWaveformType().equals(WaveformType.SYN)).collect(Collectors.toList());
			List<BasicID> synIDs = new ArrayList<>();
			for (BasicID obsid : obsIDs) {
				Phases phases = new Phases(obsid.getPhases());
				BasicID synid = tmpSyns.stream().filter(id -> id.getGlobalCMTID().equals(obsid.getGlobalCMTID())
						&& id.getStation().equals(obsid.getStation())
						&& id.getSacComponent().equals(obsid.getSacComponent())
						&& id.getMinPeriod() == obsid.getMinPeriod()
						&& id.getMaxPeriod() == obsid.getMaxPeriod()
						&& new Phases(id.getPhases()).equals(phases)
						&& id.getSamplingHz() == obsid.getSamplingHz())
						.findFirst().get();
				synIDs.add(synid);
			}
			
			for (int i = 0; i < obsIDs.size(); i++) {
				BasicID obs = obsIDs.get(i);
				BasicID syn = synIDs.get(i);
				FrequencyRange range = new FrequencyRange(1./obs.getMaxPeriod(), 1./obs.getMinPeriod());
				int ifreq = getiFreq(range);
				int iphase = getiPhase(new Phases(obs.getPhases()));
				double[] obsdata = obs.getData();
				double[] syndata = syn.getData();
				double numerator = 0;
				double denominator = 0;
				for (int k = 0; k < obsdata.length; k++) {
					double tmp = obsdata[k] - syndata[k];
					numerator += tmp * tmp;
					denominator += obsdata[k] * obsdata[k];
				}
				
				for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
					double weight = computeWeight(weightingTypes[iweight], obs, syn);
					residualVarianceNumerator[iweight][ifreq][iphase][icorr] += numerator * weight * weight;
					residualVarianceDenominator[iweight][ifreq][iphase][icorr] += denominator * weight * weight;
				}
				
				if (icorr == 0)
					npts += obsdata.length;
			}
		}
	}
	
	/**
	 * @throws IOException
	 */
	private void setTimewindows() throws IOException {
		timewindowInformation = TimewindowInformationFile.read(timewindowPath);
	}
	
	/**
	 * cut partial derivative in [start-ext, start+ext] The ext is for
	 * filtering .
	 * 
	 * @param u
	 * @param property
	 * @return
	 */
	private Complex[] cutPartial(double[] u, TimewindowInformation timewindowInformation, int ifreq) {
		int cutstart = (int) (timewindowInformation.getStartTime() * partialSamplingHz) - ext[ifreq];
		// cutstartが振り切れた場合0 からにする
		if (cutstart < 0)
			return null;
		int cutend = (int) (timewindowInformation.getEndTime() * partialSamplingHz) + ext[ifreq];
		Complex[] cut = new Complex[cutend - cutstart];
//		Arrays.parallelSetAll(cut, i -> new Complex(u[i + cutstart]));
		Arrays.setAll(cut, i -> new Complex(u[i + cutstart]));

		return cut;
	}

	private double[] sampleOutput(Complex[] u, TimewindowInformation timewindowInformation, int ifreq) {
		// 書きだすための波形
		int outnpts = (int) ((timewindowInformation.getEndTime() - timewindowInformation.getStartTime())
				* finalSamplingHz);
		double[] sampleU = new double[outnpts];

		Arrays.setAll(sampleU, j -> u[ext[ifreq] + j * step[ifreq]].getReal());
		return sampleU;
	}
	
	private void setSourceTimeFunctions(Set<GlobalCMTID> idSet) throws IOException {
		if (sourceTimeFunction == 0) {
			System.out.println("No convolution");
			return;
		}
		
		userSourceTimeFunctions = new HashMap<>();
		idSet.forEach(id -> {
			double halfDuration = id.getEvent().getHalfDuration();
			SourceTimeFunction stf;
			switch (sourceTimeFunction) {
			case 1:
				System.out.println("Using boxcar STF");
				stf = SourceTimeFunction.boxcarSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration);
				break;
			case 2:
				System.out.println("Using triangle STF");
				stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration);
				break;
			case 3:
				System.out.println("Using asymmetric triangle STF with user catalog");
				double halfDuration1 = id.getEvent().getHalfDuration();
	        	double halfDuration2 = id.getEvent().getHalfDuration();
	        	boolean found = false;
		      	for (String str : stfcat) {
		      		String[] stflist = str.split("\\s+");
		      	    GlobalCMTID eventID = new GlobalCMTID(stflist[0]);
		      	    if(id.equals(eventID)) {
		      	    	if(Integer.valueOf(stflist[3]) >= 5.) {
		      	    		halfDuration1 = Double.valueOf(stflist[1]);
		      	    		halfDuration2 = Double.valueOf(stflist[2]);
		      	    		found = true;
		      	    	}
		      	    }
		      	}
		      	if (found)
		      		stf = SourceTimeFunction.asymmetrictriangleSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration1, halfDuration2);
		      	else
		      		stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, partialSamplingHz, id.getEvent().getHalfDuration());
	            break;
			case 4:
				throw new RuntimeException("Case 4 not implemented yet");
			case 5:
				System.out.println("Using triangle STF with user catalog");
				halfDuration = 0.;
				double amplitudeCorrection = 1.;
				found = false;
		      	for (String str : stfcat) {
		      		String[] stflist = str.split("\\s+");
		      	    GlobalCMTID eventID = new GlobalCMTID(stflist[0].trim());
		      	    if(id.equals(eventID)) {
		      	    	halfDuration = Double.valueOf(stflist[1].trim());
		      	    	amplitudeCorrection = Double.valueOf(stflist[2].trim());
		      	    	found = true;
		      	    }
		      	}
		      	if (found)
		      		stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration, 1. / amplitudeCorrection);
		      	else
		      		stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, partialSamplingHz, id.getEvent().getHalfDuration());
		      	break;
			default:
				throw new RuntimeException("Error: undefined source time function identifier (0: none, 1: boxcar, 2: triangle).");
			}
			userSourceTimeFunctions.put(id, stf);
		});
	}
	
	private SourceTimeFunction getSourceTimeFunction(GlobalCMTID id) {
		return sourceTimeFunction == 0 ? null : userSourceTimeFunctions.get(id);
	}
	
	private void setBandPassFilter() throws IOException {
		System.out.println("Designing filter.");
		filter = new ButterworthFilter[frequencyRanges.length];
		
		for (int i = 0; i < frequencyRanges.length; i++) {
			double minFreq = frequencyRanges[i].getMinFreq();
			double maxFreq = frequencyRanges[i].getMaxFreq();
			
			double omegaH = maxFreq * 2 * Math.PI / partialSamplingHz;
			double omegaL = minFreq * 2 * Math.PI / partialSamplingHz;
			BandPassFilter tmpfilter = new BandPassFilter(omegaH, omegaL, filterNp);
			tmpfilter.setBackward(backward);
			
			System.out.println(tmpfilter.toString());
			
			filter[i] = tmpfilter;
		}
	}
	
	private List<HorizontalPosition> originalHorizontalPositions;
	
	
	private void canGO() {
		if (basicIDArray[0].length < 2 * timewindowInformation.size())
			throw new RuntimeException("Not enough waveforms for the given timewindow file");
	}
	
	private void outputUnknownParameters(Path outpath, UnknownParameter[] parameters) throws IOException {
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outpath.toFile())));
		for (UnknownParameter p : parameters)
			pw.println(p);
		pw.close();
	}
	
	private void outputHorizontalPoints(Path outpath, List<HorizontalPosition> horizontalPositions) throws IOException {
		if (horizontalPositions == null)
			return;
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outpath.toFile())));
		for (HorizontalPosition p : horizontalPositions)
			pw.println(p);
		pw.close();
	}
	
	private void outputPerturbationLayers(Path outpath) throws IOException {
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outpath.toFile())));
		double[] newRadii = null;
		double[] newLayerWidths = null;
//		if (horizontalMapping != null) {
//			System.out.println("Not implemented yet");
//		}
		if (threedMapping != null) {
			newRadii = threedMapping.getNewRadii();
			newLayerWidths = threedMapping.getNewLayerWidths();
		}
		else if (mapping == null ) {
			pw.close();
			return;
		}
		else {
			newRadii = mapping.getNewRadii();
			newLayerWidths = mapping.getNewLayerWidths();
		}
		for (int i = 0; i < newRadii.length; i++) {
			pw.println(newRadii[i] + " " + newLayerWidths[i]);
		}
		pw.close();
	}
	
	private int getParameterIndex(Location loc, PartialType type) {
		int i = 0;
		for (UnknownParameter p : originalUnknownParameters) {
			if (p.getLocation().equals(loc) && p.getPartialType().equals(type))
				return i;
			i++;
		}
		return -1;
	}
	
	private int getParameterIndex1D(double radius, PartialType type) {
		int i = 0;
		for (UnknownParameter p : originalUnknownParameters) {
			if (p.getLocation().getR() == radius && p.getPartialType().equals(type))
				return i;
			i++;
		}
		return -1;
	}
	
	private PartialType to1D(PartialType type3D) {
		PartialType type1D = null;
		switch (type3D) {
		case MU:
			type1D = PartialType.PAR2;
			break;
		case LAMBDA:
			type1D = PartialType.PAR1;
			break;
		default:
			throw new RuntimeException("PartialType not implemented yet " + type3D);
		}
		return type1D;
	}
	
	private final double epsilon = 1e-6;
	
	private boolean equalToEpsilon(double d1, double d2) {
		if (Math.abs(d1 - d2) < epsilon)
			return true;
		else
			return false;
	}
	
	public class FPWorker implements Runnable {
		
		SPCFile fpname;
		SPCFile fpname_PSV;
		HorizontalPosition voxelPosition;
		Station station;
		GlobalCMTID event;
		List<List<Integer>> IndicesRecordBasicID;
		private final List<TimewindowInformation> orderedRecordTimewindows;
		private int windowCounter;
		
		public FPWorker(SPCFile fpname, Station station, GlobalCMTID event,
				List<List<Integer>> IndicesRecordBasicID, List<TimewindowInformation> orderedRecordTimewindows,  int windowCounter) {
			if (mode.equals("SH")) {
				this.fpname = fpname;
				this.fpname_PSV = null;
			}
			else if (mode.equals("PSV")) {
				this.fpname = null;
				this.fpname_PSV = fpname;
			}
			this.station = station;
			this.event = event;
			this.IndicesRecordBasicID = IndicesRecordBasicID;
			this.orderedRecordTimewindows = orderedRecordTimewindows;
			this.windowCounter = windowCounter;
		}
		
		public FPWorker(SPCFile fpname, SPCFile fpname_PSV, Station station, GlobalCMTID event,
				List<List<Integer>> IndicesRecordBasicID, List<TimewindowInformation> orderedRecordTimewindows,  int windowCounter) {
			this.fpname = fpname;
			this.fpname_PSV = fpname_PSV;
			this.station = station;
			this.event = event;
			this.IndicesRecordBasicID = IndicesRecordBasicID;
			this.orderedRecordTimewindows = orderedRecordTimewindows;
			this.windowCounter = windowCounter;
		}
		
		public FPWorker(HorizontalPosition voxelPosition, Station station, GlobalCMTID event,
				List<List<Integer>> IndicesRecordBasicID, List<TimewindowInformation> orderedRecordTimewindows,  int windowCounter) {
			this.fpname = null;
			this.fpname_PSV = null;
			this.voxelPosition = voxelPosition;
			this.station = station;
			this.event = event;
			this.IndicesRecordBasicID = IndicesRecordBasicID;
			this.orderedRecordTimewindows = orderedRecordTimewindows;
			this.windowCounter = windowCounter;
		}
		
		@Override
		public void run() {
			long t1i = 0;
			long t1f = 0;
			try {
			t1i = System.currentTimeMillis();
			
			DSMOutput fpSpc = null;
			DSMOutput fpSpc_PSV = null;
			HorizontalPosition obsPos = null;
			double[] bodyR = null;
			String obsName = "";
			if (catalogueFP) {
				obsPos = voxelPosition;
			}
			else {
				if (mode.equals("SH")) {
					fpSpc = fpname.read();
					obsPos = fpSpc.getObserverPosition();
					bodyR = fpSpc.getBodyR();
					obsName = fpname.getObserverID();
				}
				else if (mode.equals("PSV")) {
					fpSpc_PSV = fpname_PSV.read();
					obsPos = fpSpc_PSV.getObserverPosition();
					bodyR = fpSpc_PSV.getBodyR();
					obsName = fpname_PSV.getObserverID();
				}
				else if (mode.equals("BOTH")) {
					fpSpc = fpname.read();
					fpSpc_PSV = fpname_PSV.read();
					obsPos = fpSpc.getObserverPosition();
					bodyR = fpSpc.getBodyR();
					obsName = fpname.getObserverID();
				}
			}
			
			if (!originalHorizontalPositions.contains(obsPos)) {
				System.out.println("Position not contained " + obsPos);
				return;
			}
			
			Location bpSourceLoc = station.getPosition().toLocation(Earth.EARTH_RADIUS);
			Location fpSourceLoc = event.getEvent().getCmtLocation();
			double distanceBP = bpSourceLoc.getEpicentralDistance(obsPos) * 180. / Math.PI;
			double distanceFP = fpSourceLoc.getEpicentralDistance(obsPos) * 180. / Math.PI;
//			double distance = bpSourceLoc.getGeographicalDistance(obsPos) * 180. / Math.PI;
			double phiBP = Math.PI - bpSourceLoc.getAzimuth(obsPos);
			double phiFP = Math.PI - fpSourceLoc.getAzimuth(obsPos);
			if (Double.isNaN(phiBP))
				throw new RuntimeException("PhiBP is NaN " + fpname + " " + station);
			if (Double.isNaN(phiFP))
				throw new RuntimeException("PhiFP is NaN " + fpname + " " + station);
//			System.out.println("phi= " + phi);
			
			System.out.println(fpSourceLoc + " " + obsPos);
		
//			System.out.println("geographic, geodetic distance = " + geocentricDistance + " " + distance);
			
			
//			if (fastCompute) {
//				double a = Math.toRadians(distanceFP);
//				double az = fpSourceLoc.getAzimuth(bpSourceLoc) - fpSourceLoc.getAzimuth(obsPos);
//				double d = Math.toDegrees(Math.asin(a * Math.sin(az)));
//				if (d > 10.)
//					return;
//			}
			
			int ipointBP = (int) ((distanceBP - thetamin) / dtheta);
//			if (distanceBP < thetamin || distanceBP > thetamax)
//				throw new RuntimeException("Error: cannot interpolate BP at epicentral distance " + distanceBP + "(deg)");
			if (ipointBP < 0) {
				System.err.println("Warning: BP distance smaller than thetamin " + distanceBP);
				ipointBP = 0;
			}
			else if (ipointBP > bpnames.length - 3) {
				System.err.println("Warning: BP distance greater than thetamax " + distanceBP);
				ipointBP = bpnames.length - 3;
			}
			
			if (distanceFP < thetamin || distanceFP > thetamax)
				throw new RuntimeException("Error: cannot interpolate FP at epicentral distance " + distanceFP + "(deg)");
			int ipointFP = (int) ((distanceFP - thetamin) / dtheta);
			
			SPCFile bpname1 = null;
			SPCFile bpname2 = null;
			SPCFile bpname3 = null;
			SPCFile bpname1_PSV = null;
			SPCFile bpname2_PSV = null;
			SPCFile bpname3_PSV = null;
			
			if (mode.equals("SH") || mode.equals("BOTH"))
				bpname1 = bpnames[ipointBP];
			if (mode.equals("PSV") || mode.equals("BOTH"))
				bpname1_PSV = bpnames_PSV[ipointBP];
			if (!quickAndDirty) {
				if (mode.equals("SH") || mode.equals("BOTH")) {
					bpname2 = bpnames[ipointBP + 1];
					bpname3 = bpnames[ipointBP + 2];
				}
				if (mode.equals("PSV") || mode.equals("BOTH")) {
					bpname2_PSV = bpnames_PSV[ipointBP + 1];
					bpname3_PSV = bpnames_PSV[ipointBP + 2];
				}
			}
			
			SPCFile fpname1 = null;
			SPCFile fpname2 = null;
			SPCFile fpname3 = null;
			SPCFile fpname1_PSV = null;
			SPCFile fpname2_PSV = null;
			SPCFile fpname3_PSV = null;
			if (catalogueFP) {
				if (mode.equals("SH") || mode.equals("BOTH")) {
					fpname1 = fpnameMap.get(event).get(ipointFP);
				}
				if (mode.equals("PSV") || mode.equals("BOTH")) {
					fpname1_PSV = fpnameMap_PSV.get(event).get(ipointFP);
				}
				if (!quickAndDirty) {
					if (mode.equals("SH") || mode.equals("BOTH")) {
						fpname2 = fpnameMap.get(event).get(ipointFP + 1);
						fpname3 = fpnameMap.get(event).get(ipointFP + 2);
					}
					if (mode.equals("PSV") || mode.equals("BOTH")) {
						fpname2_PSV = fpnameMap_PSV.get(event).get(ipointFP + 1);
						fpname3_PSV = fpnameMap_PSV.get(event).get(ipointFP + 2);
					}
				}
			}
			
			double[] dhBP = new double[3];
			if (!quickAndDirty) {
				double theta1 = thetamin + ipointBP * dtheta;
				double theta2 = theta1 + dtheta;
				double theta3 = theta2 + dtheta;
				dhBP[0] = (distanceBP - theta1) / dtheta;
				dhBP[1] = (distanceBP - theta2) / dtheta;
				dhBP[2] = (distanceBP - theta3) / dtheta;
			}
			
			double[] dhFP = new double[3];
			if (!quickAndDirty) {
				double theta1 = thetamin + ipointFP * dtheta;
				double theta2 = theta1 + dtheta;
				double theta3 = theta2 + dtheta;
				dhFP[0] = (distanceFP - theta1) / dtheta;
				dhFP[1] = (distanceFP - theta2) / dtheta;
				dhFP[2] = (distanceFP - theta3) / dtheta;
			}
			
			DSMOutput bpSpc1 = null;
			DSMOutput bpSpc2 = null; 
			DSMOutput bpSpc3 = null;
			DSMOutput fpSpc1 = null;
			DSMOutput fpSpc2 = null;
			DSMOutput fpSpc3 = null;
			DSMOutput bpSpc1_PSV = null;
			DSMOutput bpSpc2_PSV = null; 
			DSMOutput bpSpc3_PSV = null;
			DSMOutput fpSpc1_PSV = null;
			DSMOutput fpSpc2_PSV = null;
			DSMOutput fpSpc3_PSV = null;
			if (catalogueFP) {
				if (mode.equals("SH") || mode.equals("BOTH"))
					bpSpc1 = Spectrum.getInstance(bpname1, phiBP, obsPos, bpSourceLoc, "null");
				if (mode.equals("PSV") || mode.equals("BOTH"))
					bpSpc1_PSV = Spectrum.getInstance(bpname1_PSV, phiBP, obsPos, bpSourceLoc, "null");
				if (!quickAndDirty) {
					if (mode.equals("SH") || mode.equals("BOTH")) {
						bpSpc2 = Spectrum.getInstance(bpname2, phiBP, obsPos, bpSourceLoc, "null");
						bpSpc3 = Spectrum.getInstance(bpname3, phiBP, obsPos, bpSourceLoc, "null");
					}
					if (mode.equals("PSV") || mode.equals("BOTH")) {
						bpSpc2_PSV = Spectrum.getInstance(bpname2_PSV, phiBP, obsPos, bpSourceLoc, "null");
						bpSpc3_PSV = Spectrum.getInstance(bpname3_PSV, phiBP, obsPos, bpSourceLoc, "null");
					}
				}
				
				if (mode.equals("SH") || mode.equals("BOTH"))
					fpSpc1 = Spectrum.getInstance(fpname1, phiFP, obsPos, fpSourceLoc, "null");
				if (mode.equals("PSV") || mode.equals("BOTH"))
					fpSpc1_PSV = Spectrum.getInstance(fpname1_PSV, phiFP, obsPos, fpSourceLoc, "null");
				if (!quickAndDirty) {
					if (mode.equals("SH") || mode.equals("BOTH")) {
						fpSpc2 = Spectrum.getInstance(fpname2, phiFP, obsPos, fpSourceLoc, "null");
						fpSpc3 = Spectrum.getInstance(fpname3, phiFP, obsPos, fpSourceLoc, "null");
					}
					if (mode.equals("PSV") || mode.equals("BOTH")) {
						fpSpc2_PSV = Spectrum.getInstance(fpname2_PSV, phiFP, obsPos, fpSourceLoc, "null");
						fpSpc3_PSV = Spectrum.getInstance(fpname3_PSV, phiFP, obsPos, fpSourceLoc, "null");
					}
				}
				
				bodyR = bpSpc1.getBodyR();
			}
			else {
				if (mode.equals("SH") || mode.equals("BOTH"))
					bpSpc1 = Spectrum.getInstance(bpname1, phiBP, obsPos, bpSourceLoc, obsName);
				if (mode.equals("PSV") || mode.equals("BOTH"))
					bpSpc1_PSV = Spectrum.getInstance(bpname1_PSV, phiBP, obsPos, bpSourceLoc, obsName);
				if (!quickAndDirty) {
					if (mode.equals("SH") || mode.equals("BOTH")) {
						bpSpc2 = Spectrum.getInstance(bpname2, phiBP, obsPos, bpSourceLoc, obsName);
						bpSpc3 = Spectrum.getInstance(bpname3, phiBP, obsPos, bpSourceLoc, obsName);
					}
					if (mode.equals("PSV") || mode.equals("BOTH")) {
						bpSpc2_PSV = Spectrum.getInstance(bpname2_PSV, phiBP, obsPos, bpSourceLoc, obsName);
						bpSpc3_PSV = Spectrum.getInstance(bpname3_PSV, phiBP, obsPos, bpSourceLoc, obsName);
					}
				}
			}
			
			t1f = System.currentTimeMillis();
			
//------------------------------------- testBP ------------------------------
			if (testBP) {
				Path dirBP = workPath.resolve("bpwaves");
				Files.createDirectories(dirBP);
				for (int i = 0; i < bpSpc1.nbody(); i++) {
					
					if (!originalUnkownRadii.contains(bodyR[i]))
						continue;
					
					SPCBody body1 = bpSpc1.getSpcBodyList().get(i);
					SPCBody body2 = bpSpc2.getSpcBodyList().get(i);
					SPCBody body3 = bpSpc3.getSpcBodyList().get(i);
					
					SPCBody body = SPCBody.interpolate(body1, body2, body3, dhBP);
					
//					System.out.println("DEBUG BP test: " +  body.getSpcComponents()[20].getValueInFrequencyDomain()[10]);
					
//					SPCBody body = bpSpc.getSpcBodyList().get(i);
					
					int lsmooth = body.findLsmooth(tlen, partialSamplingHz);
					body.toTimeDomain(lsmooth);
					
					SPCComponent[] spcComponents = body.getSpcComponents();
					for (int j = 0; j < spcComponents.length; j++) {
						double[] bpserie = spcComponents[j].getTimeseries();
						Complex[] bpspectrum = spcComponents[j].getValueInFrequencyDomain();
						for (TimewindowInformation info : orderedRecordTimewindows) {
							for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
								Complex[] u = cutPartial(bpserie, info, ifreq);
								u = filter[ifreq].applyFilter(u);
								double[] cutU = sampleOutput(u, info, ifreq);
								
								String freqString = String.format("%.0f-%.0f", 1./frequencyRanges[ifreq].getMinFreq(),
										 1./frequencyRanges[ifreq].getMaxFreq());
								
								Phases phases = new Phases(info.getPhases());
								Path outpath = dirBP.resolve(station.getName() + "." 
										+ event + "." + "BP" + "." + (int) obsPos.getLatitude()
										+ "." + (int) obsPos.getLongitude() + "." + (int) bodyR[i] + "." + info.getComponent() 
										+ "." + freqString + "." + phases + "." + j + ".txt");
//								Files.deleteIfExists(outpath);
//								Files.createFile(outpath);
								try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE_NEW))) {
									for (double y : cutU)
										pw.println(String.format("%.16e", y));
								}
								
								Path outpath2 = dirBP.resolve(station.getName() + "." 
										+ event + "." + "BP" + "." + (int) obsPos.getLatitude()
										+ "." + (int) obsPos.getLongitude() + "." + (int) bodyR[i] + "." + info.getComponent() 
										+ "." + freqString + "." + phases + "." + j + ".spectrum.txt");
								try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath2, StandardOpenOption.CREATE_NEW))) {
									for (Complex y : bpspectrum)
										pw.println(String.format("%.16e", y.abs()));
								}
							}
						}
					}
				}
				// continue
			}
//----------------------------------------- END testBP ------------------------------------------------------------------------
//
//----------------------------- test FP
			if (testFP) {
				Path dirFP = workPath.resolve("fpwaves");
				Files.createDirectories(dirFP);
				for (int i = 0; i < fpSpc1.nbody(); i++) {
					
					if (!originalUnkownRadii.contains(bodyR[i]))
						continue;
					
					SPCBody body1 = fpSpc1.getSpcBodyList().get(i);
					SPCBody body2 = fpSpc2.getSpcBodyList().get(i);
					SPCBody body3 = fpSpc3.getSpcBodyList().get(i);
					
					SPCBody body = SPCBody.interpolate(body1, body2, body3, dhFP);
					
//					System.out.println("DEBUG BP test: " +  body.getSpcComponents()[20].getValueInFrequencyDomain()[10]);
					
//					SPCBody body = bpSpc.getSpcBodyList().get(i);
					
					int lsmooth = body.findLsmooth(tlen, partialSamplingHz);
					body.toTimeDomain(lsmooth);
					
					SPCComponent[] spcComponents = body.getSpcComponents();
					for (int j = 0; j < spcComponents.length; j++) {
						double[] fpserie = spcComponents[j].getTimeseries();
						Complex[] fpspectrum = spcComponents[j].getValueInFrequencyDomain();
						for (TimewindowInformation info : orderedRecordTimewindows) {
							for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
								Complex[] u = cutPartial(fpserie, info, ifreq);
								u = filter[ifreq].applyFilter(u);
								double[] cutU = sampleOutput(u, info, ifreq);
								
								String freqString = String.format("%.0f-%.0f", 1./frequencyRanges[ifreq].getMinFreq(),
										 1./frequencyRanges[ifreq].getMaxFreq());
								
								Phases phases = new Phases(info.getPhases());
								Path outpath = dirFP.resolve(station.getName() + "." 
										+ event + "." + "FP" + "." + (int) obsPos.getLatitude()
										+ "." + (int) obsPos.getLongitude() + "." + (int) bodyR[i] + "." + info.getComponent() 
										+ "." + freqString + "." + phases + "." + j + ".txt");
//								Files.deleteIfExists(outpath);
//								Files.createFile(outpath);
								try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE_NEW))) {
									for (double y : cutU)
										pw.println(String.format("%.16e", y));
								}
								
								Path outpath2 = dirFP.resolve(station.getName() + "." 
										+ event + "." + "FP" + "." + (int) obsPos.getLatitude()
										+ "." + (int) obsPos.getLongitude() + "." + (int) bodyR[i] + "." + info.getComponent() 
										+ "." + freqString + "." + phases + "." + j + ".spectrum.txt");
								try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath2, StandardOpenOption.CREATE_NEW))) {
									for (Complex y : fpspectrum)
										pw.println(String.format("%.16e", y.abs()));
								}
							}
						}
					}
				}
				// continue
			}
//----------------------------- END test FP
//	
//------------------------------------- compute partials ----------------------------------------------------------------
			t1i = System.currentTimeMillis();
			
			ThreeDPartialMaker threedPartialMaker = null;
			if (catalogueFP) {
				if (!quickAndDirty) {
					if (mode.equals("SH")) {
						threedPartialMaker = new ThreeDPartialMaker(fpSpc1, fpSpc2, fpSpc3, bpSpc1, bpSpc2, bpSpc3, dhBP, dhFP);
					}
					else if (mode.equals("PSV"))
						threedPartialMaker = new ThreeDPartialMaker(fpSpc1_PSV, fpSpc2_PSV, fpSpc3_PSV, bpSpc1_PSV, bpSpc2_PSV, bpSpc3_PSV, dhBP, dhFP);
					else if (mode.equals("BOTH"))
						threedPartialMaker = new ThreeDPartialMaker(fpSpc1, fpSpc1_PSV, fpSpc2, fpSpc2_PSV, fpSpc3, fpSpc3_PSV, bpSpc1, bpSpc1_PSV,
								bpSpc2, bpSpc2_PSV, bpSpc3, bpSpc3_PSV, dhBP, dhFP);
				}
				else {
					if (mode.equals("SH"))
						threedPartialMaker = new ThreeDPartialMaker(fpSpc1, bpSpc1);
					else if (mode.equals("PSV"))
						threedPartialMaker = new ThreeDPartialMaker(fpSpc1_PSV, bpSpc1_PSV);
					else if (mode.equals("BOTH"))
						threedPartialMaker = new ThreeDPartialMaker(fpSpc1, fpSpc1_PSV, bpSpc1, bpSpc1_PSV);
				}
			}
			else {
				if (!quickAndDirty) {
					if (mode.equals("SH"))
						threedPartialMaker = new ThreeDPartialMaker(fpSpc, bpSpc1, bpSpc2, bpSpc3, dhBP);
					else if (mode.equals("PSV"))
						threedPartialMaker = new ThreeDPartialMaker(fpSpc_PSV, bpSpc1_PSV, bpSpc2_PSV, bpSpc3_PSV, dhBP);
					else if (mode.equals("BOTH"))
						threedPartialMaker = new ThreeDPartialMaker(fpSpc, fpSpc_PSV, bpSpc1, bpSpc1_PSV,
								bpSpc2, bpSpc2_PSV, bpSpc3, bpSpc3_PSV, dhBP);
				}
				else {
					if (mode.equals("SH"))
						threedPartialMaker = new ThreeDPartialMaker(fpSpc, bpSpc1);
					else if (mode.equals("PSV"))
						threedPartialMaker = new ThreeDPartialMaker(fpSpc_PSV, bpSpc1_PSV);
					else if (mode.equals("BOTH"))
						threedPartialMaker = new ThreeDPartialMaker(fpSpc, fpSpc_PSV, bpSpc1, bpSpc1_PSV);
				}
			}
			
			SourceTimeFunction stf = getSourceTimeFunction(event);
			if (stf == null)
				System.err.println("Null STF");
			threedPartialMaker.setSourceTimeFunction(stf);
			
			for (int ibody = 0; ibody < bodyR.length; ibody++) {
				Location parameterLoc = obsPos.toLocation(bodyR[ibody]);
				
				if (!originalUnkownRadii.contains(bodyR[ibody]))
					continue;
				
				for (int ipar = 0; ipar < partialTypes.length; ipar++) {
					PartialType type = partialTypes[ipar];
					int iunknown = getParameterIndex(parameterLoc, type);
					if (iunknown < 0) {
						continue;
					}
					
					double weightUnknown = originalUnknownParameters[iunknown].getWeighting();
					
					Map<SACComponent, double[]> partialmap = new HashMap<>();
					Map<SACComponent, Complex[]> partialFreqMap = new HashMap<>();
					for (SACComponent component : components) {
						double[] partial;
						partial = threedPartialMaker.createPartialSerial(component, ibody, type);
						partialmap.put(component, partial);
						
					}
					
					for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
						for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
							// find syn and obs
							double minFreq = frequencyRanges[ifreq].getMinFreq();
							double maxFreq = frequencyRanges[ifreq].getMaxFreq();
							
								for (TimewindowInformation info : orderedRecordTimewindows) {
									double[] partial = partialmap.get(info.getComponent());
									
									Phases phases = new Phases(info.getPhases());
									int iphase = phaseMap.get(phases);
									
									for (int icorr = 0; icorr < correctionTypes.length; icorr++) {
										
										final int finalIcorr = icorr;
										final int finalIfreq = ifreq;
										
										double[] residual = null;
										double weight = 1.;
										
										if (computationFlag == 1 || computationFlag == 2) {
											List<BasicID> obsSynIDs = IndicesRecordBasicID.get(icorr).stream().filter(i -> {
													BasicID id = basicIDArray[finalIcorr][i];
													return id.getSacComponent().equals(info.getComponent())
	//												&& equalToEpsilon(id.getStartTime(), info.getStartTime())
													&& new FrequencyRange(1./id.getMaxPeriod(), 1./id.getMinPeriod()).equals(frequencyRanges[finalIfreq])
													&& new Phases(id.getPhases()).equals(phases);
													}).map(i -> basicIDArray[finalIcorr][i])
													.collect(Collectors.toList());
											
											if (obsSynIDs.size() != 2) {
												synchronized (AtAMaker.class) {
													obsSynIDs.forEach(System.out::println);
													throw new RuntimeException("Unexpected: more, or less than two basicIDs (obs, syn) found (list above)");
												}
											}
											BasicID obsID = obsSynIDs.stream().filter(id -> id.getWaveformType().equals(WaveformType.OBS))
													.findAny().get();
											BasicID synID = obsSynIDs.stream().filter(id -> id.getWaveformType().equals(WaveformType.SYN))
													.findAny().get();
											double[] obsData = obsID.getData();
											double[] synData = synID.getData();
											residual = new double[obsData.length];
											
											weight = computeWeight(weightingTypes[iweight], obsID, synID);
											
											if (Double.isNaN(weight))
												throw new RuntimeException("Weight is NaN " + info);
											
											for (int k = 0; k < obsData.length; k++) {
												residual[k] = (obsData[k] - synData[k]) * weight;
											}
										}
									
										Complex[] u = cutPartial(partial, info, ifreq);
										
										u = filter[ifreq].applyFilter(u);
										double[] cutU = sampleOutput(u, info, ifreq);
										
										if (Double.isNaN(new ArrayRealVector(cutU).getLInfNorm()))
											throw new RuntimeException("cutU is NaN " + originalUnknownParameters[iunknown] + " " + info);
										
										//--- write partials (usually for DEBUG)
										//--- partials are written before being weighted by volume of voxel or data weighting
										if (outPartial) {
											String freqString = String.format("%.0f-%.0f", 1./frequencyRanges[ifreq].getMinFreq(),
													 1./frequencyRanges[ifreq].getMaxFreq());
											Path outpath = outpartialDir.resolve(station.getName() + "." 
													+ event + "." + info.getComponent() + "." + (int) obsPos.getLatitude()
													+ "." + (int) obsPos.getLongitude() + "." + (int) bodyR[ibody] + "." + type + "."
													+ weightingTypes[iweight] + "." + freqString + "."
													+ phases + ".txt");
											Files.deleteIfExists(outpath);
											Files.createFile(outpath);
											double t0 = info.getStartTime();
											double dt = 1. / finalSamplingHz;
											try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.APPEND))) {
												for (int k = 0; k < cutU.length; k++) {
													pw.println(String.format("%.6f %.16e", t0 + k*dt, cutU[k]));
												}
											}
										} // END write partials
										
										double tmpatd = 0;
										for (int k = 0; k < cutU.length; k++) {
											cutU[k] *= weight * weightUnknown;
											if (computationFlag != 3)
												tmpatd += cutU[k] * residual[k];
										}
										
										partials[iunknown][iweight][ifreq][windowCounter] = cutU;
										
										if (cutU.length == 0) {
											throw new RuntimeException(Thread.currentThread().getName() + " Unexpected: cutU (partial) has length 0 "
													+ originalUnknownParameters[iunknown] + " " + weightingTypes[iweight] + " " + frequencyRanges[ifreq]
													+ " " + info);
										}
										 
										if (computationFlag != 3) {
											double value = atdEntries[iunknown][iweight][ifreq][iphase][icorr].getValue();
											value += tmpatd;
											
											if (Double.isNaN(value))
												throw new RuntimeException("Atd value is NaN" + originalUnknownParameters[iunknown] + " " + info);
											
											atdEntries[iunknown][iweight][ifreq][iphase][icorr].setValue(value);
										}
										
									} // END correction type
									windowCounter++;
								} // END timewindow
								windowCounter -= orderedRecordTimewindows.size();
//							} // END SAC component
						} // END frequency range
					} // END weighting type
					
				} // partial[ipar] made
				
				t1f = System.currentTimeMillis();
				if ((++workProgressCounter) % progressStep == 0) {
					System.out.print(".");
					workProgressCounter = 0;
				}
			} // END bodyR loop 
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			Thread t = Thread.currentThread();
			t.getUncaughtExceptionHandler().uncaughtException(t, e);
		}
			
		}
		
	}
	
	public double computeWeight(WeightingType type, BasicID obs, BasicID syn) {
		double weight = 1.;
		if (type.equals(WeightingType.RECIPROCAL)) {
			weight = 1. / new ArrayRealVector(obs.getData()).getLInfNorm();
		}
		else if (type.equals(WeightingType.IDENTITY)) {
			weight = 1.;
		}
		else if (type.equals(WeightingType.RECIPROCAL_AZED_TZCA)) {
			weight = 1./ new ArrayRealVector(obs.getData()).getLInfNorm() *
					Weighting.weightingAzimuthTZCA(obs) *
					Weighting.weightingDistanceTZCA(obs);
		}
		else if (type.equals(WeightingType.RECIPROCAL_STAEVT_TZCA)) {
			weight = 1. / new ArrayRealVector(obs.getData()).getLInfNorm() *
					Weighting.weightingStationTZCA(obs) *
					Weighting.weightEventTZCA(obs);
		}
		else
			throw new RuntimeException("Weighting not yet implemented for " + type);
		
		return weight;
	}
	
	public class AtAWorker implements Runnable {
		
		private int i0AtA;
		private int i0counter;
		
		public AtAWorker(int iStartBuffer, int i0AtA) {
			this.i0AtA = i0AtA;
			this.i0counter = i0AtA - iStartBuffer;
		}
		
		@Override
		public void run() {
			try {
			int iunknown = (int) (0.5 * (FastMath.sqrt(1 + 8 * i0AtA) - 1));
			int junknown = i0AtA - iunknown * (iunknown + 1) / 2;
			
			int[] iOriginalUnknowns;
			int[] jOriginalUnknowns;
			
			if (horizontalMapping != null) {
				iOriginalUnknowns = horizontalMapping.getiNewToOriginal(iunknown);
				jOriginalUnknowns = horizontalMapping.getiNewToOriginal(junknown);
			}
			else if (threedMapping != null) {
				iOriginalUnknowns = threedMapping.getiNewToOriginal(iunknown);
				jOriginalUnknowns = threedMapping.getiNewToOriginal(junknown);
			}
			else {
				iOriginalUnknowns = mapping.getiNewToOriginal(iunknown);
				jOriginalUnknowns = mapping.getiNewToOriginal(junknown);
			}
			for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
				for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
					for (int iphase = 0; iphase < usedPhases.length; iphase++) {
						for (int iwin = 0; iwin < nwindowBuffer; iwin++) {
							int it = partials[iunknown][iweight][ifreq][iwin].length;
							
							double[] partiali = new double[it];
							double[] partialj = new double[it];
							for (int iOriginal = 0; iOriginal < iOriginalUnknowns.length; iOriginal++) {
								for (int k = 0; k < partiali.length; k++)
									partiali[k] += partials[iOriginalUnknowns[iOriginal]][iweight][ifreq][iwin][k];
							}
							for (int jOriginal = 0; jOriginal < jOriginalUnknowns.length; jOriginal++) {
								for (int k = 0; k < partialj.length; k++)
									partialj[k] += partials[jOriginalUnknowns[jOriginal]][iweight][ifreq][iwin][k];
							}
							
							if (it != partialj.length)
								throw new RuntimeException("Unexpected: timewindows differ " + it + " " + partialj.length);
							
							if (it == 0)
								continue;
							
							double ataij = 0;
							for (int k = 0; k < it; k++) {
								ataij += partiali[k] * partialj[k];
							}
							
							double value = ataBuffer[i0counter][iweight][ifreq][iphase].getValue();
							value += ataij;
							
							ataBuffer[i0counter][iweight][ifreq][iphase].setValue(value);
						}
					}
				}
			}
		} catch (RuntimeException e) {
			Thread t = Thread.currentThread();
			t.getUncaughtExceptionHandler().uncaughtException(t, e);
		}
		}
	}
	
	public class AtADDWorker implements Runnable {
		
		private int i0AtA;
		private int i0counter;
		
		public AtADDWorker(int iStartBuffer, int i0AtA) {
			this.i0AtA = i0AtA;
			this.i0counter = i0AtA - iStartBuffer;
		}
		
		@Override
		public void run() {
			try {
			int iunknown = (int) (0.5 * (FastMath.sqrt(1 + 8 * i0AtA) - 1));
			int junknown = i0AtA - iunknown * (iunknown + 1) / 2;
			
			int[] iOriginalUnknowns;
			int[] jOriginalUnknowns;
			
			if (horizontalMapping != null) {
				iOriginalUnknowns = horizontalMapping.getiNewToOriginal(iunknown);
				jOriginalUnknowns = horizontalMapping.getiNewToOriginal(junknown);
			}
			else if (threedMapping != null) {
				iOriginalUnknowns = threedMapping.getiNewToOriginal(iunknown);
				jOriginalUnknowns = threedMapping.getiNewToOriginal(junknown);
			}
			else {
				iOriginalUnknowns = mapping.getiNewToOriginal(iunknown);
				jOriginalUnknowns = mapping.getiNewToOriginal(junknown);
			}
			for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
				for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
					for (int iphase = 0; iphase < usedPhases.length; iphase++) {
						for (int iwin = 0; iwin < nwindowBuffer; iwin++) {
							int it = partials[iunknown][iweight][ifreq][iwin].length;
							int iit = it;
							
							if (it == 0)
								continue;
							
							double[] partiali_i = new double[it];
							double[] partialj_i = new double[it];
							for (int iOriginal = 0; iOriginal < iOriginalUnknowns.length; iOriginal++) {
								for (int k = 0; k < partiali_i.length; k++)
									partiali_i[k] += partials[iOriginalUnknowns[iOriginal]][iweight][ifreq][iwin][k];
							}
							for (int jOriginal = 0; jOriginal < jOriginalUnknowns.length; jOriginal++) {
								for (int k = 0; k < partialj_i.length; k++)
									partialj_i[k] += partials[jOriginalUnknowns[jOriginal]][iweight][ifreq][iwin][k];
							}
							
							if (it != partialj_i.length)
								throw new RuntimeException("Unexpected: timewindows differ " + it + " " + partialj_i.length);
							
							for (int jwin = 0; jwin < iwin; jwin++) {
								int jt = partials[iunknown][iweight][ifreq][jwin].length;
								
								if (Math.abs(it-jt) > 1)
									throw new RuntimeException("it != jt "  + it +  " " + jt);
								else if (it - jt == 1)
									iit = jt;
								else if (jt - it == 1)
									iit = it;
								
								double[] partiali_j = new double[jt];
								double[] partialj_j = new double[jt];
								for (int iOriginal = 0; iOriginal < iOriginalUnknowns.length; iOriginal++) {
									for (int k = 0; k < partiali_j.length; k++)
										partiali_j[k] += partials[iOriginalUnknowns[iOriginal]][iweight][ifreq][jwin][k];
								}
								for (int jOriginal = 0; jOriginal < jOriginalUnknowns.length; jOriginal++) {
									for (int k = 0; k < partialj_j.length; k++)
										partialj_j[k] += partials[jOriginalUnknowns[jOriginal]][iweight][ifreq][jwin][k];
								}
								
								if (jt != partialj_j.length)
									throw new RuntimeException("Unexpected: timewindows differ " + jt + " " + partialj_j.length);
								
								if (jt == 0)
									continue;
								
								double ataij = 0;
								for (int k = 0; k < iit; k++) {
									ataij += (partiali_i[k] - partiali_j[k]) * (partialj_i[k] - partialj_j[k]);
								}
								
								double value = ataBuffer[i0counter][iweight][ifreq][iphase].getValue();
								value += ataij;
								
								ataBuffer[i0counter][iweight][ifreq][iphase].setValue(value);
							}
						}
					}
				}
			}
		} catch (RuntimeException e) {
			Thread t = Thread.currentThread();
			t.getUncaughtExceptionHandler().uncaughtException(t, e);
		}
		}
	}

}
