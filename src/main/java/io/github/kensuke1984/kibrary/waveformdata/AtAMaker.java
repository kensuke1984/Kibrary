package io.github.kensuke1984.kibrary.waveformdata;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionType;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.HorizontalParameterMapping;
import io.github.kensuke1984.kibrary.inversion.ParameterMapping;
import io.github.kensuke1984.kibrary.inversion.ResampleGrid;
import io.github.kensuke1984.kibrary.inversion.ThreeDParameterMapping;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.inversion.Weighting;
import io.github.kensuke1984.kibrary.inversion.WeightingType;
import io.github.kensuke1984.kibrary.quick.LookAtBPspc;
import io.github.kensuke1984.kibrary.quick.LookAtFPspc;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.FrequencyRange;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.spc.DSMOutput;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.util.spc.SpcBody;
import io.github.kensuke1984.kibrary.util.spc.SpcComponent;
import io.github.kensuke1984.kibrary.util.spc.SpcFileName;
import io.github.kensuke1984.kibrary.util.spc.SpcSAC;
import io.github.kensuke1984.kibrary.util.spc.ThreeDPartialMaker;
import io.github.kensuke1984.kibrary.util.spc.SpectrumFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.management.RuntimeErrorException;

import opendap.dap.DPrimitive;

import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.util.FastMath;

import ucar.nc2.ft.point.standard.Table.CoordName;

/**
 * @author Anselme Borgeaud
 *
 */
public class AtAMaker implements Operation {
	private Properties property;
	
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
	
	Path verticalMappingFile;
	Path horizontalMappingFile;
	
	ParameterMapping mapping;
	HorizontalParameterMapping horizontalMapping;
	ThreeDParameterMapping threedMapping;
	
	private int workProgressCounter;
	private int progressStep;
	
//	private List<SpcFileName> bpnames;
//	private List<SpcFileName> bpnames_PSV;
//	private List<SpcFileName> fpnames;
//	private List<SpcFileName> fpnames_PSV;
	
	private final SpcFileName[] bpnames;
	private final SpcFileName[] bpnames_PSV;
	private List<SpcFileName> fpnames;
	private Map<GlobalCMTID, List<SpcFileName>> fpnameMap;
	private List<SpcFileName> fpnames_PSV;
	private Map<GlobalCMTID, List<SpcFileName>> fpnameMap_PSV;
	
	private double[][][][][] partials;
	
	public final BasicID[][] basicIDArray;
	
	private WaveformDataWriter writer;
	
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
		return property;
	}
	
	/**
	 * @param property
	 * @throws IOException
	 */
	public AtAMaker(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));
		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		
		mode = property.getProperty("mode").trim().toUpperCase();
		if (!(mode.equals("SH") || mode.equals("PSV") || mode.equals("BOTH")))
				throw new RuntimeException("Error: mode should be one of the following: SH, PSV, BOTH");
		System.out.println("Using mode " + mode);
		
		//frequency ranges
		double[] minFreqs = Stream.of(property.getProperty("minFreq").trim().split("\\s+")).mapToDouble(Double::parseDouble).toArray();
		double[] maxFreqs = Stream.of(property.getProperty("maxFreq").trim().split("\\s+")).mapToDouble(Double::parseDouble).toArray();
		if (minFreqs.length != maxFreqs.length)
			throw new RuntimeException("Error: number of entries for minFreq and maxFreq differ");
		frequencyRanges = new FrequencyRange[minFreqs.length];
		for (int i = 0; i < minFreqs.length; i++)
			frequencyRanges[i] = new FrequencyRange(minFreqs[i], maxFreqs[i]);
		
		//weighting types
		weightingTypes = Stream.of(property.getProperty("weightingTypes").trim().split("\\s+")).map(type -> WeightingType.valueOf(type))
			 	.collect(Collectors.toList()).toArray(new WeightingType[0]);
			if (weightingTypes.length < 0)
				throw new IllegalArgumentException("Error: weightingTypes must be set");
//			if (weightingTypes.length > 1)
//				throw new IllegalArgumentException("Error: only 1 weighting type can be set now");
			
		//sac components
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toList()).toArray(new SACComponent[0]);
		
		//partial types
		partialTypes = Arrays.stream(property.getProperty("partialTypes").split("\\s+")).map(PartialType::valueOf)
				.collect(Collectors.toSet()).toArray(new PartialType[0]);
		
		System.out.print("Using partial types: ");
		for (PartialType type : partialTypes)
			System.out.print(type + " ");
		System.out.println();
		
		//correction types
		correctionBootstrap = Boolean.parseBoolean(property.getProperty("correctionBootstrap"));
		
		if (!correctionBootstrap) {
			correctionTypes = Stream.of(property.getProperty("correctionTypes").trim().split("\\s+")).map(s -> StaticCorrectionType.valueOf(s.trim()))
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
		bpnames = Utilities.collectOrderedSHSpcFileName(bpPath).toArray(new SpcFileName[0]);
		if (mode.equals("PSV") || mode.equals("BOTH"))
			bpnames_PSV = Utilities.collectOrderedPSVSpcFileName(bpPath).toArray(new SpcFileName[0]);
		else
			bpnames_PSV = null;
		
		//basicIDs
		computationFlag = Integer.parseInt(property.getProperty("computationFlag"));
		
		if (computationFlag != 3) {
			rootWaveformPath = Paths.get(property.getProperty("rootWaveformPath").trim());
			
			if (!correctionBootstrap) {
				if (property.getProperty("rootWaveformPath").trim().equals(".")) {
					waveformIDPath = Stream.of(property.getProperty("waveformIDPath").trim().split("\\s+")).map(s -> Paths.get(s))
							.collect(Collectors.toList()).toArray(new Path[0]);
					waveformPath = Stream.of(property.getProperty("waveformPath").trim().split("\\s+")).map(s -> Paths.get(s))
							.collect(Collectors.toList()).toArray(new Path[0]);
				}
				else {
					waveformIDPath = Stream.of(property.getProperty("waveformIDPath").trim().split("\\s+")).map(s -> rootWaveformPath.resolve(s))
							.collect(Collectors.toList()).toArray(new Path[0]);
					waveformPath = Stream.of(property.getProperty("waveformPath").trim().split("\\s+")).map(s -> rootWaveformPath.resolve(s))
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
				basicIDArray[i] = BasicIDFile.readBasicIDandDataFile(waveformIDPath[i], waveformPath[i]);
			}
		}
		else {
			basicIDArray = null;
		}
		
		// unknowns
		unknownParameterPath = getPath("unknownParameterPath");
		resamplingRate = Integer.parseInt(property.getProperty("resamplingRate"));
		if (property.containsKey("verticalMappingFile"))
			verticalMappingFile = Paths.get(property.getProperty("verticalMappingFile").trim());
		else
			verticalMappingFile = null;
		if (property.containsKey("horizontalMappingFile"))
			horizontalMappingFile = Paths.get(property.getProperty("horizontalMappingFile").trim());
		else
			horizontalMappingFile = null;
		
		numberOfBuffers = Integer.parseInt(property.getProperty("numberOfBuffers"));
		
		List<UnknownParameter> targetUnknowns = UnknownParameterFile.read(unknownParameterPath);
		List<Double> lats = targetUnknowns.stream().map(p -> p.getLocation().getLatitude()).distinct().collect(Collectors.toList());
		List<Double> lons = targetUnknowns.stream().map(p -> p.getLocation().getLatitude()).distinct().collect(Collectors.toList());
		Collections.sort(lats);
		Collections.sort(lons);
		double dlat = Math.abs(lats.get(1) - lats.get(0));
		double dlon = Math.abs(lons.get(1) - lons.get(0));
		System.out.println("Target grid increments lat, lon = " + dlat + ", " + dlon);
		
		ResampleGrid sampler = new ResampleGrid(targetUnknowns, dlat, dlon, resamplingRate);
		originalUnknownParameters = sampler.getResampledUnkowns().toArray(new UnknownParameter[0]);
		
		originalHorizontalPositions = Stream.of(originalUnknownParameters).map(p -> p.getLocation().toHorizontalPosition()).distinct()
				.collect(Collectors.toList());
		
		originalUnkownRadii = targetUnknowns.stream().map(p -> p.getLocation().getR())
				.collect(Collectors.toSet());
		
		if (verticalMappingFile != null) {
			System.out.println("Using 3-D mapping with resampler" + verticalMappingFile + " " + resamplingRate);
			threedMapping = new ThreeDParameterMapping(sampler, verticalMappingFile);
			newUnknownParameters = threedMapping.getNewUnknowns();
			horizontalMapping = null;
		}
		else {
			System.out.println("No mapping");
			mapping = new ParameterMapping(originalUnknownParameters);
			newUnknownParameters = mapping.getUnknowns();
			horizontalMapping = null;
			threedMapping = null;
		}
		
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

		System.err.println(AtAMaker.class.getName() + " is going..");
		
		// verify memory requirements
//		AtAEntry entry = new AtAEntry();
//		long size = SizeOf.sizeOf(entry);
//		System.out.println("Size of one AtAEntry = " + SizeOf.humanReadable(size));
//		System.out.println("Size of the AtA matrix = (nWeightingType * nFrequencyRanges * nUnknowns * 30 (buffer for different phases))^2");
		//
		
		atam.run();
		
		System.err.println(AtAMaker.class.getName() + " finished in "
				+ Utilities.toTimeString(System.nanoTime() - startTime));
	}
	
	private Path rootWaveformPath;
	
	private Path partialIDPath;
	
	private Path partialPath;
	
//	private List<PartialID> partialIDs;
	
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
			pw.println("##Boolean interpolate FP from catalogue");
			pw.println("#catalogueFP");
			pw.println("##Theta- range and sampling for the BP catalog in the format: thetamin thetamax thetasampling. (1. 50. 2e-2)");
			pw.println("#thetaInfo");
			pw.println("##Boolean use the closest grid point in the catalogue without interpolation (works if the catalogue is dense enough) (false)");
			pw.println("quickAndDirty");
			pw.println("##Compute AtA and Atd (1), Atd only (2), or PartialID files (3). (3)");
			pw.println("#computationFlag");
			pw.println("##String if it is PREM spector file is in bpdir/PREM  (PREM)");
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
			pw.println("##Int resampling rate of the target model for integration of the partials (1)");
			pw.println("#resamplingRate");
			pw.println("##Path of a file with the mapping to compine unknown parameters, ignored if not set");
			pw.println("#verticalMappingFile");
			pw.println("#horizontalMappingFile");
			pw.println("##Weighting scheme for data weighting. Choose among (RECIPROCAL, IDENTITY). Can enter multiple values (separated by a space). (RECIPROCAL)");
			pw.println("#weightingTypes");
			pw.println("##double time length DSM parameter tlen, must be set");
			pw.println("#tlen 3276.8");
			pw.println("##int step of frequency domain DSM parameter np, must be set");
			pw.println("#np 512");
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
			pw.println("##Number of buffers files for AtA matrix (higher number increases I/0) (1)");
			pw.println("#numberOfBuffers");
			pw.println("##Number of timewindow to store in the (temporary) partial vector (100)");
			pw.println("#nwindowBuffer");
			pw.println("#---------------");
			pw.println("#correctionBootstrap false");
			pw.println("#nSample 100");
			pw.println("#---------------");
			pw.println("##File for Qstructure (if no file, then PREM)");
			pw.println("#qinf");
			pw.println("##path of the time partials directory, must be set if PartialType containes TIME_SOURCE or TIME_RECEIVER");
			pw.println("#timePartialPath");
			pw.println("##The following options are usually for DEBUG");
			pw.println("##output the back-propagated wavefield as time series");
			pw.println("#testBP");
			pw.println("##output the forward-propagated wavefield as time series");
			pw.println("#testFP");
			pw.println("##output the partial as time series");
			pw.println("#outPartial");
		}
		System.err.println(outPath + " is created.");
	}

	/**
	 * 
	 */
	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", ".");
		if (!property.containsKey("rootWaveformPath"))
			property.setProperty("rootWaveformPath", ".");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("bpPath"))
			property.setProperty("bpPath", "BPinfo");
		if (!property.containsKey("fpPath"))
			property.setProperty("fpPath", "FPinfo");
		if (!property.containsKey("modelName"))
			property.setProperty("modelName", "PREM");
		if (!property.containsKey("maxFreq"))
			property.setProperty("maxFreq", "0.08");
		if (!property.containsKey("minFreq"))
			property.setProperty("minFreq", "0.005");
		if (!property.containsKey("sourceTimeFunction"))
			property.setProperty("sourceTimeFunction", "2");
		if (!property.containsKey("partialTypes"))
			property.setProperty("partialTypes", "MU");
		if (!property.containsKey("partialSamplingHz"))
			property.setProperty("partialSamplingHz", "20");
		if (!property.containsKey("finalSamplingHz"))
			property.setProperty("finalSamplingHz", "1");
		if (!property.containsKey("filterNp"))
			property.setProperty("filterNp", "4");
		if (!property.containsKey("testBP"))
			property.setProperty("testBP", "false");
		if (!property.containsKey("testFP"))
			property.setProperty("testFP", "false");
		if (!property.containsKey("outPartial"))
			property.setProperty("outPartial", "false");
		if (!property.containsKey("weightingTypes"))
			property.setProperty("weightingTypes", "RECIPROCAL");
		if(!property.containsKey("thetaInfo"))
			property.setProperty("thetaInfo", "1. 50. 1e-2");
		if(!property.containsKey("numberOfBuffers"))
			property.setProperty("numberOfBuffers", "1");
		if(!property.containsKey("nproc"))
			property.setProperty("nproc", "1");
		if(!property.containsKey("nwindowBuffer"))
			property.setProperty("nwindowBuffer", "100");
		if(!property.containsKey("backward"))
			property.setProperty("backward", "false");
		if(!property.containsKey("computationFlag"))
			property.setProperty("computationFlag", "3");
		if(!property.containsKey("correctionBootstrap"))
			property.setProperty("correctionBootstrap", "false");
		if (!property.containsKey("nSample"))
			property.setProperty("nSample", "100");
		if (!property.containsKey("catalogueFP"))
			property.setProperty("catalogueFP", "false");
		if (!property.containsKey("resamplingRate"))
			property.setProperty("resamplingRate", "1");
		if (!property.containsKey("quickAndDirty"))
			property.setProperty("quickAndDirty", "false");
		if (!property.containsKey("mode"))
			property.setProperty("mode", "SH");
	}

	/**
	 * @throws IOException
	 */
	private void set() throws IOException {
		fpPath = getPath("fpPath");
		timewindowPath = getPath("timewindowPath");
		
		if (property.containsKey("qinf"))
			structure = new PolynomialStructure(getPath("qinf"));
		try {
			sourceTimeFunction = Integer.parseInt(property.getProperty("sourceTimeFunction"));
		} catch (Exception e) {
			sourceTimeFunction = -1;
			sourceTimeFunctionPath = getPath("sourceTimeFunction");
		}
		modelName = property.getProperty("modelName");

		tlen = Double.parseDouble(property.getProperty("tlen"));
		np = Integer.parseInt(property.getProperty("np"));
		
		partialSamplingHz = 20;
		// =Double.parseDouble(reader.getFirstValue("partialSamplingHz")); TODO

		finalSamplingHz = Double.parseDouble(property.getProperty("finalSamplingHz"));
		
		filterNp = Integer.parseInt(property.getProperty("filterNp"));
		
		testBP = Boolean.parseBoolean(property.getProperty("testBP"));
		
		testFP = Boolean.parseBoolean(property.getProperty("testFP"));
		
		outPartial = Boolean.parseBoolean(property.getProperty("outPartial"));
		
		nSample = Integer.parseInt(property.getProperty("nSample"));
		
		double[] tmpthetainfo = Stream.of(property.getProperty("thetaInfo").trim().split("\\s+")).mapToDouble(Double::parseDouble)
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
		
		nproc = Integer.parseInt(property.getProperty("nproc"));
		
		nwindowBuffer = Integer.parseInt(property.getProperty("nwindowBuffer"));
		
		outpartialDir = workPath.resolve("partials");
		
		backward = Boolean.parseBoolean(property.getProperty("backward"));
		
//		if (computationFlag == 3)
//			partialIDs = new ArrayList<>();
		
		catalogueFP = Boolean.parseBoolean(property.getProperty("catalogueFP"));
		
		quickAndDirty = Boolean.parseBoolean(property.getProperty("quickAndDirty"));
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
		return IOUtils.readLines(SpcSAC.class.getClassLoader().getResourceAsStream(STFcatalogue)
					, Charset.defaultCharset());
	}
	
	private Path logfile;
	
	private TimewindowInformation[] timewindowOrder;
	
	private final int bufferMargin = 10;
	
	/* (non-Javadoc)
	 * @see io.github.kensuke1984.kibrary.Operation#run()
	 */
	@Override
	public void run() throws IOException {
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
		
		partialPath = workPath.resolve("partial" + tempString + ".dat");
		partialIDPath = workPath.resolve("partialID" + tempString + ".dat");
		
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
		double[][] periodRanges = new double[][] { {1./frequencyRanges[0].getMaxFreq(), 1./frequencyRanges[0].getMinFreq()} };
		Set<Phase> tmpPhases = new HashSet<>();
		for (Phases ps : usedPhases)
			ps.toSet().stream().forEach(p -> tmpPhases.add(p));
		Phase[] phaseArray = tmpPhases.toArray(new Phase[0]);
		Set<Location> perturbationPoints = Stream.of(newUnknownParameters).map(p -> p.getLocation()).collect(Collectors.toSet());
		writer = new WaveformDataWriter(partialIDPath, partialPath, usedStations, usedEvents, periodRanges, phaseArray, perturbationPoints);
		
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
//			List<SpcFileName> fpnames = Utilities.collectOrderedSpcFileName(fpEventPath);
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
				
				//--- initialize Partial vector
//				if (windowCounter.get() == 0) {
//						partials = new double[nOriginalUnknown][][][][][];
//						for (int i = 0; i < nOriginalUnknown; i++) {
//							partials[i] = new double[nWeight][][][][];
//							for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
//								partials[i][iweight] = new double[nFreq][][][];
//								for (int ifreq = 0; ifreq < nFreq; ifreq++) {
//									partials[i][iweight][ifreq] = new double[usedPhases.length][][];
//									for (int iphase = 0; iphase < usedPhases.length; iphase++) {
//										partials[i][iweight][ifreq][iphase] = new double[nwindowBuffer][];
//										for (int iwin = 0; iwin < nwindowBuffer; iwin++) {
//											partials[i][iweight][ifreq][iphase][iwin] = new double[0];
//										}
//									}
//								}
//							}
//						}
//				}
				
				//--- compute partials
//				System.out.println("Computing partials...");
				synchronized (this) {
					int currentWindowCounter = windowCounter.get();
					
	//				System.out.println(currentWindowCounter + " " + iterationCount + " " + nInteration + " " + nwindowBufferLastIteration);
					
					if (catalogueFP) {
						System.out.println("FP catalogue");
						for (HorizontalPosition position : originalHorizontalPositions) {
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
							else if (mode.equals("BOTH"))
								todo.add(Executors.callable(new FPWorker(fpnames.get(ispc), fpnames_PSV.get(ispc)
										, station, event, IndicesRecordBasicID, orderedRecordTimewindows, currentWindowCounter)));
						}
					}
					
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
							
							System.out.println(ibuff + " " + bufferStartIndex[ibuff] + " " + n);
							
							System.out.println(Runtime.getRuntime().availableProcessors() + " available processors for computation of AtA");
							List<Callable<Object>> todo2 = new ArrayList<Callable<Object>>();
							for (int i0AtA = bufferStartIndex[ibuff]; i0AtA < n; i0AtA++) {
								todo2.add(Executors.callable(new AtAWorker(bufferStartIndex[ibuff], i0AtA)));
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
					if (computationFlag == 3)
						fillA(timewindowOrder);
					
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
		
		writer.close();
		
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
	
	private void fillA(TimewindowInformation[] timewindows) {
		try {
			for (int iunknown = 0; iunknown < nNewUnknown; iunknown++) {
				int[] iOriginalUnknowns; 
				
//				if (horizontalMapping != null) {
//					iOriginalUnknowns = horizontalMapping.getiNewToOriginal(iunknown);
//				}
//				else if (threedMapping != null) {
//					iOriginalUnknowns = threedMapping.getiNewToOriginal(iunknown);
//				}
//				else {
//					iOriginalUnknowns = mapping.getiNewToOriginal(iunknown);
//	//				System.out.println("DEBUG mapping: " + iOriginalUnknowns.length);
//				}
				
				
				if (verticalMappingFile != null) {
					iOriginalUnknowns = threedMapping.getiNewToOriginal(iunknown);
//					System.out.println("--> " + newUnknownParameters[iunknown]);
//					for (int ii : iOriginalUnknowns) {
//						System.out.println(originalUnknownParameters[ii]);
//					}
				}
				else {
					iOriginalUnknowns = mapping.getiNewToOriginal(iunknown);
				}
				
//				Location locdebug = new Location(41.0, -113.0, 5804.75);
				
				for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
					for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
//						for (int iphase = 0; iphase < usedPhases.length; iphase++) {
							for (int iwin = 0; iwin < nwindowBuffer; iwin++) {
								if (partials[0][iweight][ifreq][iwin].length == 0)
									continue;
								
								TimewindowInformation window = timewindows[iwin];
								Phases phases = new Phases(window.getPhases());
//								Phases phases = usedPhases[iphase];
								Phase[] phaseArray = phases.toSet().toArray(new Phase[0]);
								
			//					System.out.println(partials[iunknown][iweight][ifreq].length + " " + l);
			//					double[] partiali = partials[iunknown][iweight][ifreq][iphase][iwin];
			//					double[] partialj = partials[junknown][iweight][ifreq][iphase][iwin];
								
//								int it = partials[iunknown][iweight][ifreq][iphase][iwin].length;
								int it = partials[0][iweight][ifreq][iwin].length;
								if (it > 0) {
									double[] partiali = new double[it];
									for (int iOriginal = 0; iOriginal < iOriginalUnknowns.length; iOriginal++) {
//										if (newUnknownParameters[iunknown].getLocation().equals(locdebug)) {
//											System.out.println(iwin + " " + String.valueOf(originalUnknownParameters[iOriginalUnknowns[iOriginal]]) + " " 
//													+ String.valueOf(partials[iOriginalUnknowns[iOriginal]][iweight][ifreq][iphase][iwin][0]));
//										}
										for (int k = 0; k < partiali.length; k++) {
											partiali[k] += partials[iOriginalUnknowns[iOriginal]][iweight][ifreq][iwin][k];
										}
									}
									
									PartialID partialID = new PartialID(window.getStation(), window.getGlobalCMTID(), window.getComponent()
											, finalSamplingHz, window.getStartTime(), it, 1./frequencyRanges[ifreq].getMaxFreq()
											, 1./frequencyRanges[ifreq].getMinFreq(), phaseArray, 0, true, newUnknownParameters[iunknown].getLocation()
											, newUnknownParameters[iunknown].getPartialType(), partiali);
									
	//								partialIDs.add(partialID);
									writer.addPartialID(partialID);
								}
								else
									System.out.println(0);
							}
//						}
					}
				}
			}
			
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	private void writeA(Path idPath, Path dataPath, Set<Station> stations, Set<GlobalCMTID> events) throws IOException {
//		double[][] periodRanges = new double[][] { {1./frequencyRanges[0].getMaxFreq(), 1./frequencyRanges[0].getMinFreq()} };
//		Phase[] phaseArray = usedPhases[0].toSet().toArray(new Phase[0]);
//		Set<Location> perturbationPoints = Stream.of(newUnknownParameters).map(p -> p.getLocation()).collect(Collectors.toSet());
//		WaveformDataWriter writer = new WaveformDataWriter(idPath, dataPath, stations, events, periodRanges, phaseArray, perturbationPoints);
//		
//		for (PartialID partial : partialIDs)
//			writer.addPartialID(partial);
//		
//		writer.close();
//	}

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

		// cutting a waveform for outputting
//		Arrays.parallelSetAll(sampleU, j -> u[ext[ifreq] + j * step[ifreq]].getReal());
		Arrays.setAll(sampleU, j -> u[ext[ifreq] + j * step[ifreq]].getReal());
		return sampleU;
	}
	
	private void setSourceTimeFunctions(Set<GlobalCMTID> idSet) throws IOException {
		if (sourceTimeFunction == 0) {
			System.out.println("No convolution");
			return;
		}
//		if (sourceTimeFunction == -1) {
//			readSourceTimeFunctions();
//			return;
//		}
		
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
//				double mw = id.getEvent().getCmt().getMw();
////				double duration = 9.60948E-05 * Math.pow(10, 0.6834 * mw);
//				double duration = 0.018084 * Math.pow(10, 0.3623 * mw);
//				halfDuration = duration / 2.;
////				System.out.println("DEBUG1: id, mw, half-duration = " + id + " " + mw + " " + halfDuration);
//				return SourceTimeFunction.triangleSourceTimeFunction(np, tlen, samplingHz, halfDuration);
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
		System.err.println("Designing filter.");
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
	
	@Deprecated
//	private void setUnknownParameters() throws IOException {
//		originalUnknownParameters = UnknownParameterFile.read(unknownParameterPath).toArray(new UnknownParameter[0]);
//		
//		originalHorizontalPositions = Stream.of(originalUnknownParameters).map(p -> p.getLocation().toHorizontalPosition()).distinct()
//				.collect(Collectors.toList());
//		
//		originalUnkownRadii = Stream.of(originalUnknownParameters).map(p -> p.getLocation().getR())
//				.collect(Collectors.toSet());
//		
//		if (verticalMappingFile != null && horizontalMappingFile == null) {
//			System.out.println("Using vertical mapping " + verticalMappingFile);
//			mapping = new ParameterMapping(originalUnknownParameters, verticalMappingFile); 
//			newUnknownParameters = mapping.getUnknowns();
//			horizontalMapping = null;
//			threedMapping = null;
//		}
//		else if (verticalMappingFile == null && horizontalMappingFile != null) {
//			System.out.println("Using horizontal mapping " + horizontalMappingFile);
//			horizontalMapping = new HorizontalParameterMapping(originalUnknownParameters, horizontalMappingFile);
//			newUnknownParameters = mapping.getUnknowns();
//			threedMapping = null;
//		}
//		else if (verticalMappingFile != null && horizontalMappingFile != null) {
//			System.out.println("Using 3-D mapping " + verticalMappingFile + " " + horizontalMappingFile);
//			threedMapping = new ThreeDParameterMapping(horizontalMappingFile, verticalMappingFile, originalUnknownParameters);
//			newUnknownParameters = threedMapping.getNewUnknowns();
//			horizontalMapping = null;
//		}
//		else {
//			System.out.println("No mapping");
//			mapping = new ParameterMapping(originalUnknownParameters);
//			newUnknownParameters = mapping.getUnknowns();
//			horizontalMapping = null;
//			threedMapping = null;
//		}
//		
//		nOriginalUnknown = originalUnknownParameters.length;
//		nNewUnknown = newUnknownParameters.length;
//		
//		n0AtA = nNewUnknown * (nNewUnknown + 1) / 2;
//		int ntmp = n0AtA / numberOfBuffers;
//		n0AtABuffer = n0AtA - ntmp * (numberOfBuffers - 1);
//		bufferStartIndex = new int[numberOfBuffers];
//		for (int i = 0; i < numberOfBuffers; i++)
//			bufferStartIndex[i] = i * ntmp;
//	}
	
//	private void setUnknownParametersFromSampler() throws IOException {
//		List<UnknownParameter> targetUnknowns = UnknownParameterFile.read(unknownParameterPath);
//		List<Double> lats = targetUnknowns.stream().map(p -> p.getLocation().getLatitude()).distinct().collect(Collectors.toList());
//		List<Double> lons = targetUnknowns.stream().map(p -> p.getLocation().getLatitude()).distinct().collect(Collectors.toList());
//		Collections.sort(lats);
//		Collections.sort(lons);
//		double dlat = Math.abs(lats.get(1) - lats.get(0));
//		double dlon = Math.abs(lons.get(1) - lons.get(0));
//		System.out.println("Target grid increments lat, lon = " + dlat + ", " + dlon);
//		
//		ResampleGrid sampler = new ResampleGrid(targetUnknowns, dlat, dlon, resamplingRate);
//		originalUnknownParameters = sampler.getResampledUnkowns().toArray(new UnknownParameter[0]);
//		
//		originalHorizontalPositions = Stream.of(originalUnknownParameters).map(p -> p.getLocation().toHorizontalPosition()).distinct()
//				.collect(Collectors.toList());
//		
//		originalUnkownRadii = targetUnknowns.stream().map(p -> p.getLocation().getR())
//				.collect(Collectors.toSet());
//		
//		if (verticalMappingFile != null) {
//			System.out.println("Using 3-D mapping with resampler" + verticalMappingFile + " " + resamplingRate);
//			threedMapping = new ThreeDParameterMapping(sampler, verticalMappingFile);
//			newUnknownParameters = threedMapping.getNewUnknowns();
//			horizontalMapping = null;
//		}
//		else {
//			System.out.println("No mapping");
//			mapping = new ParameterMapping(originalUnknownParameters);
//			newUnknownParameters = mapping.getUnknowns();
//			horizontalMapping = null;
//			threedMapping = null;
//		}
//		
//		nOriginalUnknown = originalUnknownParameters.length;
//		nNewUnknown = newUnknownParameters.length;
//		
//		n0AtA = nNewUnknown * (nNewUnknown + 1) / 2;
//		int ntmp = n0AtA / numberOfBuffers;
//		n0AtABuffer = n0AtA - ntmp * (numberOfBuffers - 1);
//		bufferStartIndex = new int[numberOfBuffers];
//		for (int i = 0; i < numberOfBuffers; i++)
//			bufferStartIndex[i] = i * ntmp;
//	}
	
//	private void setWaveformData() throws IOException {
//		basicIDArray = new BasicID[waveformIDPath.length][];
//		for (int i = 0; i < waveformIDPath.length; i++) {
//			basicIDArray[i] = BasicIDFile.readBasicIDandDataFile(waveformIDPath[i], waveformPath[i]);
//		}
//	}
	
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
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outpath.toFile())));
		for (HorizontalPosition p : horizontalPositions)
			pw.println(p);
		pw.close();
	}
	
	private void outputPerturbationLayers(Path outpath) throws IOException {
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outpath.toFile())));
		double[] newRadii = null;
		double[] newLayerWidths = null;
		if (horizontalMapping != null) {
			System.out.println("Not implemented yet");
		}
		else if (threedMapping != null) {
			newRadii = threedMapping.getNewRadii();
			newLayerWidths = threedMapping.getNewLayerWidths();
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
//			System.out.println(loc + " " + type + ":" + p.getLocation() + " " + p.getPartialType());
			if (p.getLocation().equals(loc) && p.getPartialType().equals(type))
				return i;
			i++;
		}
		return -1;
	}
	
	private final double epsilon = 1e-6;
	
	private boolean equalToEpsilon(double d1, double d2) {
		if (Math.abs(d1 - d2) < epsilon)
			return true;
		else
			return false;
	}
	
	public class FPWorker implements Runnable {
		
		SpcFileName fpname;
		SpcFileName fpname_PSV;
		HorizontalPosition voxelPosition;
		Station station;
		GlobalCMTID event;
		List<List<Integer>> IndicesRecordBasicID;
		private final List<TimewindowInformation> orderedRecordTimewindows;
		private int windowCounter;
		
		public FPWorker(SpcFileName fpname, Station station, GlobalCMTID event,
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
		
		public FPWorker(SpcFileName fpname, SpcFileName fpname_PSV, Station station, GlobalCMTID event,
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
					obsName = fpname.getObserverName();
				}
				else if (mode.equals("PSV")) {
					fpSpc_PSV = fpname_PSV.read();
					obsPos = fpSpc_PSV.getObserverPosition();
					bodyR = fpSpc_PSV.getBodyR();
					obsName = fpname_PSV.getObserverName();
				}
				else if (mode.equals("BOTH")) {
					fpSpc = fpname.read();
					fpSpc_PSV = fpname_PSV.read();
					obsPos = fpSpc.getObserverPosition();
					bodyR = fpSpc.getBodyR();
					obsName = fpname.getObserverName();
				}
			}
			
			
			if (!originalHorizontalPositions.contains(obsPos))
				return;
			
//			DSMOutput bpSpc = bpMap.get(obsPos);
			
//			Arrays.stream(bodyR).forEach(System.out::println);
		
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
		
//			System.out.println("geographic, geodetic distance = " + geocentricDistance + " " + distance);
			
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
			
			
//			System.out.println("Distance FP, BP: " + distanceFP + " " + distanceBP);
			
			SpcFileName bpname1 = null;
			SpcFileName bpname2 = null;
			SpcFileName bpname3 = null;
			SpcFileName bpname1_PSV = null;
			SpcFileName bpname2_PSV = null;
			SpcFileName bpname3_PSV = null;
			
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
			
			SpcFileName fpname1 = null;
			SpcFileName fpname2 = null;
			SpcFileName fpname3 = null;
			SpcFileName fpname1_PSV = null;
			SpcFileName fpname2_PSV = null;
			SpcFileName fpname3_PSV = null;
			if (catalogueFP) {
				if (mode.equals("SH") || mode.equals("BOTH")) {
//					fpname1 = fpnames.get(ipointFP);
					fpname1 = fpnameMap.get(event).get(ipointFP);
				}
				if (mode.equals("PSV") || mode.equals("BOTH")) {
//					fpname1_PSV = fpnames_PSV.get(ipointFP);
					fpname1_PSV = fpnameMap_PSV.get(event).get(ipointFP);
				}
				if (!quickAndDirty) {
					if (mode.equals("SH") || mode.equals("BOTH")) {
//						fpname2 = fpnames.get(ipointFP + 1);
//						fpname3 = fpnames.get(ipointFP + 2);
						fpname2 = fpnameMap.get(event).get(ipointFP + 1);
						fpname3 = fpnameMap.get(event).get(ipointFP + 2);
					}
					if (mode.equals("PSV") || mode.equals("BOTH")) {
//						fpname2_PSV = fpnames_PSV.get(ipointFP + 1);
//						fpname3_PSV = fpnames_PSV.get(ipointFP + 2);
						fpname2_PSV = fpnameMap_PSV.get(event).get(ipointFP + 1);
						fpname3_PSV = fpnameMap_PSV.get(event).get(ipointFP + 2);
					}
				}
			}
			
//			System.out.println(fpname1 + " " + fpname2 + " " + fpname3);
//			System.out.println(bpname1 + " " + bpname2 + " " + bpname3);
			
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
			
//			System.out.println(obsPos + " " + distance + " " + distance*Math.PI/180. + " " + phi + " " + ipointBP + " " + theta1 + " " + theta2 + " " + dh[0] + " " + dh[1]);
//			System.out.println(bpname1);
//			System.out.println(bpname2);
//			System.out.println(bpname3);
			
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
					bpSpc1 = SpectrumFile.getInstance(bpname1, phiBP, obsPos, bpSourceLoc, "null");
				if (mode.equals("PSV") || mode.equals("BOTH"))
					bpSpc1_PSV = SpectrumFile.getInstance(bpname1_PSV, phiBP, obsPos, bpSourceLoc, "null");
				if (!quickAndDirty) {
					if (mode.equals("SH") || mode.equals("BOTH")) {
						bpSpc2 = SpectrumFile.getInstance(bpname2, phiBP, obsPos, bpSourceLoc, "null");
						bpSpc3 = SpectrumFile.getInstance(bpname3, phiBP, obsPos, bpSourceLoc, "null");
					}
					if (mode.equals("PSV") || mode.equals("BOTH")) {
						bpSpc2_PSV = SpectrumFile.getInstance(bpname2_PSV, phiBP, obsPos, bpSourceLoc, "null");
						bpSpc3_PSV = SpectrumFile.getInstance(bpname3_PSV, phiBP, obsPos, bpSourceLoc, "null");
					}
				}
				
				if (mode.equals("SH") || mode.equals("BOTH"))
					fpSpc1 = SpectrumFile.getInstance(fpname1, phiFP, obsPos, fpSourceLoc, "null");
				if (mode.equals("PSV") || mode.equals("BOTH"))
					fpSpc1_PSV = SpectrumFile.getInstance(fpname1_PSV, phiFP, obsPos, fpSourceLoc, "null");
				if (!quickAndDirty) {
					if (mode.equals("SH") || mode.equals("BOTH")) {
						fpSpc2 = SpectrumFile.getInstance(fpname2, phiFP, obsPos, fpSourceLoc, "null");
						fpSpc3 = SpectrumFile.getInstance(fpname3, phiFP, obsPos, fpSourceLoc, "null");
					}
					if (mode.equals("PSV") || mode.equals("BOTH")) {
						fpSpc2_PSV = SpectrumFile.getInstance(fpname2_PSV, phiFP, obsPos, fpSourceLoc, "null");
						fpSpc3_PSV = SpectrumFile.getInstance(fpname3_PSV, phiFP, obsPos, fpSourceLoc, "null");
					}
				}
				
				bodyR = bpSpc1.getBodyR();
			}
			else {
				if (mode.equals("SH") || mode.equals("BOTH"))
					bpSpc1 = SpectrumFile.getInstance(bpname1, phiBP, obsPos, bpSourceLoc, obsName);
				if (mode.equals("PSV") || mode.equals("BOTH"))
					bpSpc1_PSV = SpectrumFile.getInstance(bpname1_PSV, phiBP, obsPos, bpSourceLoc, obsName);
				if (!quickAndDirty) {
					if (mode.equals("SH") || mode.equals("BOTH")) {
						bpSpc2 = SpectrumFile.getInstance(bpname2, phiBP, obsPos, bpSourceLoc, obsName);
						bpSpc3 = SpectrumFile.getInstance(bpname3, phiBP, obsPos, bpSourceLoc, obsName);
					}
					if (mode.equals("PSV") || mode.equals("BOTH")) {
						bpSpc2_PSV = SpectrumFile.getInstance(bpname2_PSV, phiBP, obsPos, bpSourceLoc, obsName);
						bpSpc3_PSV = SpectrumFile.getInstance(bpname3_PSV, phiBP, obsPos, bpSourceLoc, obsName);
					}
				}
			}
			
			
			t1f = System.currentTimeMillis();
//			System.out.println(Thread.currentThread().getName() + " Initialization took " + (t1f-t1i)*1e-3 + " s");
			
//			LookAtBPspc.printHeader(bpSpc);
//			LookAtFPspc.printHeader(fpSpc);

//------------------------------------- testBP ------------------------------
			if (testBP) {
				Path dirBP = workPath.resolve("bpwaves");
				Files.createDirectories(dirBP);
				for (int i = 0; i < bpSpc1.nbody(); i++) {
					
					if (!originalUnkownRadii.contains(bodyR[i]))
						continue;
					
					SpcBody body1 = bpSpc1.getSpcBodyList().get(i);
					SpcBody body2 = bpSpc2.getSpcBodyList().get(i);
					SpcBody body3 = bpSpc3.getSpcBodyList().get(i);
					
					SpcBody body = SpcBody.interpolate(body1, body2, body3, dhBP);
					
//					System.out.println("DEBUG BP test: " +  body.getSpcComponents()[20].getValueInFrequencyDomain()[10]);
					
//					SpcBody body = bpSpc.getSpcBodyList().get(i);
					
					int lsmooth = body.findLsmooth(tlen, partialSamplingHz);
					body.toTimeDomain(lsmooth);
					
					SpcComponent[] spcComponents = body.getSpcComponents();
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
								Path outpath = dirBP.resolve(station.getStationName() + "." 
										+ event + "." + "BP" + "." + (int) obsPos.getLatitude()
										+ "." + (int) obsPos.getLongitude() + "." + (int) bodyR[i] + "." + info.getComponent() 
										+ "." + freqString + "." + phases + "." + j + ".txt");
//								Files.deleteIfExists(outpath);
//								Files.createFile(outpath);
								try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE_NEW))) {
									for (double y : cutU)
										pw.println(String.format("%.16e", y));
								}
								
								Path outpath2 = dirBP.resolve(station.getStationName() + "." 
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
					
					SpcBody body1 = fpSpc1.getSpcBodyList().get(i);
					SpcBody body2 = fpSpc2.getSpcBodyList().get(i);
					SpcBody body3 = fpSpc3.getSpcBodyList().get(i);
					
					SpcBody body = SpcBody.interpolate(body1, body2, body3, dhFP);
					
//					System.out.println("DEBUG BP test: " +  body.getSpcComponents()[20].getValueInFrequencyDomain()[10]);
					
//					SpcBody body = bpSpc.getSpcBodyList().get(i);
					
					int lsmooth = body.findLsmooth(tlen, partialSamplingHz);
					body.toTimeDomain(lsmooth);
					
					SpcComponent[] spcComponents = body.getSpcComponents();
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
								Path outpath = dirFP.resolve(station.getStationName() + "." 
										+ event + "." + "FP" + "." + (int) obsPos.getLatitude()
										+ "." + (int) obsPos.getLongitude() + "." + (int) bodyR[i] + "." + info.getComponent() 
										+ "." + freqString + "." + phases + "." + j + ".txt");
//								Files.deleteIfExists(outpath);
//								Files.createFile(outpath);
								try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE_NEW))) {
									for (double y : cutU)
										pw.println(String.format("%.16e", y));
								}
								
								Path outpath2 = dirFP.resolve(station.getStationName() + "." 
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
//						if (voxelPosition.equals(new HorizontalPosition(40.5,-113.5)))
//							System.out.println(windowCounter + " " + fpSpc1.getSpcFileName() + " " + bpSpc2.getSpcFileName());
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
			
//			ThreeDPartialMaker threedPartialMaker = new ThreeDPartialMaker(fpSpc, bpSpc);
			
			SourceTimeFunction stf = getSourceTimeFunction(event);
			if (stf == null)
				System.err.println("Null STF");
			threedPartialMaker.setSourceTimeFunction(stf);
			
			t1f = System.currentTimeMillis();
//			System.out.println(Thread.currentThread().getName() + " Initialization2 (3DParMaker) took " + (t1f-t1i)*1e-3 + " s");
			
//			System.out.println(Thread.currentThread().getName() + " starts body loop...");
			t1i = System.currentTimeMillis();
			
			for (int ibody = 0; ibody < bodyR.length; ibody++) {
				Location parameterLoc = obsPos.toLocation(bodyR[ibody]);
				
				if (!originalUnkownRadii.contains(bodyR[ibody]))
					continue;
				
				for (int ipar = 0; ipar < partialTypes.length; ipar++) {
					PartialType type = partialTypes[ipar];
					int iunknown = getParameterIndex(parameterLoc, type);
					if (iunknown < 0) {
//						System.err.println("Warning: unkown in FP not found in the unknown parameter file");
						continue;
					}
					
//					int iNewUnknown = mapping.getiOriginalToNew(iunknown);
					
					double weightUnknown = originalUnknownParameters[iunknown].getWeighting();
//					System.out.println(weightUnknown);
					
					Map<SACComponent, double[]> partialmap = new HashMap<>();
					for (SACComponent component : components) {
						double[] partial = threedPartialMaker.createPartialSerial(component, ibody, type);
						partialmap.put(component, partial);
					}
					
					for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
						
						for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
							
//							AtomicInteger iwindow = new AtomicInteger(0);
							
							// find syn and obs
							double minFreq = frequencyRanges[ifreq].getMinFreq();
							double maxFreq = frequencyRanges[ifreq].getMaxFreq();
							
								for (TimewindowInformation info : orderedRecordTimewindows) {
									double[] partial = partialmap.get(info.getComponent());
									
//									if (originalUnknownParameters[iunknown].getLocation().equals(new Location(40.5,-113.5,5804.75))) {
//										System.out.println(iunknown + " " + windowCounter + " " + originalUnknownParameters[iunknown] + " " + partial[0]);
//									}
									
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
										
//										if (originalUnknownParameters[iunknown].getLocation().equals(new Location(40.5,-113.5,5804.75))) {
//											System.out.println(iunknown + " " + windowCounter + " " + originalUnknownParameters[iunknown] + " " + u[0]);
//										}
										
										u = filter[ifreq].applyFilter(u);
										double[] cutU = sampleOutput(u, info, ifreq);
										
//										if (originalUnknownParameters[iunknown].getLocation().equals(new Location(40.5,-113.5,5804.75))) {
//											System.out.println(iunknown + " " + windowCounter + " " + originalUnknownParameters[iunknown] + " " + cutU[0] + " " + info);
//										}
										
//										System.out.println(type + " " + new ArrayRealVector(cutU).getLInfNorm());
										
										if (Double.isNaN(new ArrayRealVector(cutU).getLInfNorm()))
											throw new RuntimeException("cutU is NaN " + originalUnknownParameters[iunknown] + " " + info);
										
										//--- write partials (usually for DEBUG)
										//--- partials are written before being weighted by volume of voxel or data weighting
										if (outPartial) {
											String freqString = String.format("%.0f-%.0f", 1./frequencyRanges[ifreq].getMinFreq(),
													 1./frequencyRanges[ifreq].getMaxFreq());
											Path outpath = outpartialDir.resolve(station.getStationName() + "." 
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
										
//										System.out.println(iunknown + " " + new ArrayRealVector(cutU).getLInfNorm());
//										System.out.println(windowCounter);
										partials[iunknown][iweight][ifreq][windowCounter] = cutU;
//										if (originalUnknownParameters[iunknown].getLocation().equals(new Location(40.5,-113.5,5804.75))) {
//											System.out.println(iunknown + " " + windowCounter + " " + originalUnknownParameters[iunknown] + " " + cutU[0]);
//										}
										
										if (cutU.length == 0) {
											throw new RuntimeException(Thread.currentThread().getName() + " Unexpected: cutU (partial) has length 0 "
													+ originalUnknownParameters[iunknown] + " " + weightingTypes[iweight] + " " + frequencyRanges[ifreq]
													+ " " + info);
										}
										 
//										parameterLoc = originalUnknownParameters[iunknown].getLocation();
										// add entry to Atd
//										AtdEntry entry = new AtdEntry(weightingTypes[iweight], frequencyRanges[ifreq]
//												, phases, type, parameterLoc, tmpatd); //TODO parameterLoc
										
										if (computationFlag != 3) {
											double value = atdEntries[iunknown][iweight][ifreq][iphase][icorr].getValue();
											value += tmpatd;
											
											if (Double.isNaN(value))
												throw new RuntimeException("Atd value is NaN" + originalUnknownParameters[iunknown] + " " + info);
											
											atdEntries[iunknown][iweight][ifreq][iphase][icorr].setValue(value);
										}
										
//										AtdEntry tmp = atdEntries[iunknown][iweight][ifreq][iphase];
//										tmp.add(entry);
//										atdEntries[iunknown][iweight][ifreq][iphase] = tmp;
										// END add entry to Atd
									
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
			
			
//			System.out.println(Thread.currentThread().getName() + " All loops took " + (t1f-t1i)*1e-3 + " s");
//			System.out.println(Thread.currentThread().getName() + " finished body loop in " + (t1f-t1i)*1e-3 + " s");
			
//			Files.write(logfile, (event.toString() + " " + station.getStationName() + " done\n").getBytes(), StandardOpenOption.APPEND);
			
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
			
			//--- for debug
//			String s = String.valueOf(newUnknownParameters[iunknown]) + "\n";
//			for (int i = 0; i < iOriginalUnknowns.length; i++)
//				s += "--> " + String.valueOf(originalUnknownParameters[iOriginalUnknowns[i]]) + "\n";
//			s += String.valueOf(newUnknownParameters[junknown]) + "\n";
//			for (int i = 0; i < jOriginalUnknowns.length; i++)
//				s += "--> " + String.valueOf(originalUnknownParameters[jOriginalUnknowns[i]]) + "\n";
//			System.out.print(s);
			//---
			
//			System.out.println(Thread.currentThread().getName() + " " + iunknown + " " + junknown);
			
			for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
//				
				for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
					
					for (int iphase = 0; iphase < usedPhases.length; iphase++) {
						for (int iwin = 0; iwin < nwindowBuffer; iwin++) {
	//						TimewindowInformation info = orderedRecordTimewindows.get(l);
	//						Phases phases = new Phases(info.getPhases());
//							Phases phases = usedPhases[iphase];
							
	//						System.out.println(partials[iunknown][iweight][ifreq].length + " " + l);
	//						double[] partiali = partials[iunknown][iweight][ifreq][iphase][iwin];
	//						double[] partialj = partials[junknown][iweight][ifreq][iphase][iwin];
							
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
	//						s += "2: " + new ArrayRealVector(partiali).getLInfNorm() + " " + new ArrayRealVector(partialj).getLInfNorm() + "\n";
							
	//						if (it == 0) {
	//							throw new RuntimeException("Unexpected: partial i has lenght 0 " + iunknown + " " + unknownParameters[iunknown] + " " 
	//									+ weightingTypes[iweight] + " " + frequencyRanges[ifreq]);
	//						}
	//						if (partialj.length == 0) {
	//							throw new RuntimeException("Unexpected: partial j has lenght 0 " + junknown + " " + unknownParameters[iunknown] + " " 
	//									+ weightingTypes[iweight] + " " + frequencyRanges[ifreq]);
	//						}
							
							if (it != partialj.length)
								throw new RuntimeException("Unexpected: timewindows differ " + it + " " + partialj.length);
							
							if (it == 0)
								continue;
							
							double ataij = 0;
							for (int k = 0; k < it; k++) {
	//							System.out.println(k + " " + partiali[k] + " " + partialj[k]);
								ataij += partiali[k] * partialj[k];
							}
							
	//						AtAEntry entry = new AtAEntry(weightingTypes[iweight], frequencyRanges[ifreq]
	//								, phases, newUnknownParameters[iunknown]
	//								, newUnknownParameters[junknown], ataij);
							
							double value = ataBuffer[i0counter][iweight][ifreq][iphase].getValue();
							value += ataij;
							
							ataBuffer[i0counter][iweight][ifreq][iphase].setValue(value);
							
	//						AtAEntry tmp = ataBuffer[i0counter][iweight][ifreq][iphase];
	//						tmp.add(entry);
	//						ataBuffer[i0counter][iweight][ifreq][iphase] = tmp;
						}
					}
				}
			}
			
//			System.out.println(s);
			
//			i0counter++;
		} catch (RuntimeException e) {
			Thread t = Thread.currentThread();
			t.getUncaughtExceptionHandler().uncaughtException(t, e);
		}
		}
	}
}
