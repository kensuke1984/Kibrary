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
import java.util.HashMap;
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
	
	private StaticCorrectionType[] correctionTypes;
	
	private Set<TimewindowInformation> timewindowInformation;
	private PartialType[] partialTypes;
	private Set<SACComponent> components;
	
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
	private UnknownParameter[] originalUnknownParameters;
	private UnknownParameter[] newUnknownParameters;
	private Set<Double> originalUnkownRadii;
	
	private boolean testBP;
	
	private boolean outPartial;
	
	private FrequencyRange[] frequencyRanges;
	
	private WeightingType[] weightingTypes;
	
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
	
	Path verticalMappingFile;
	Path horizontalMappingFile;
	
	ParameterMapping mapping;
	HorizontalParameterMapping horizontalMapping;
	ThreeDParameterMapping threedMapping;
	
	private int workProgressCounter;
	private int progressStep;
	
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
	
	private boolean writeA;
	
	private Path partialIDPath;
	
	private Path partialPath;
	
	private List<PartialID> partialIDs;
	
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
			pw.println("##Path of the back propagate spc catalog folder (BPcat/PREM)");
			pw.println("#bpPath");
			pw.println("##Theta- range and sampling for the BP catalog in the format: thetamin thetamax thetasampling. (1. 50. 2e-2)");
			pw.println("#thetaInfo");
			pw.println("##Path of a forward propagate spc folder (FPinfo)");
			pw.println("#fpPath");
			pw.println("##Compute AtA and Atd (1), or Atd only (2). (1)");
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
			pw.println("##Path of unknown parameter file, must be set");
			pw.println("#unknownParameterPath");
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
			pw.println("#writeA");
			pw.println("#---------------");
			pw.println("##File for Qstructure (if no file, then PREM)");
			pw.println("#qinf");
			pw.println("##path of the time partials directory, must be set if PartialType containes TIME_SOURCE or TIME_RECEIVER");
			pw.println("#timePartialPath");
			pw.println("##The following options are usually for DEBUG");
			pw.println("##output the back-propagated wavefield as time series");
			pw.println("#testBP");
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
			property.setProperty("computationFlag", "1");
		if(!property.containsKey("writeA"))
			property.setProperty("writeA", "false");
		if(!property.containsKey("correctionBootstrap"))
			property.setProperty("correctionBootstrap", "false");
		if (!property.containsKey("nSample"))
			property.setProperty("nSample", "100");
	}

	/**
	 * @throws IOException
	 */
	private void set() throws IOException {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));

		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");

		bpPath = getPath("bpPath");
		fpPath = getPath("fpPath");
		timewindowPath = getPath("timewindowPath");
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());

		if (property.containsKey("qinf"))
			structure = new PolynomialStructure(getPath("qinf"));
		try {
			sourceTimeFunction = Integer.parseInt(property.getProperty("sourceTimeFunction"));
		} catch (Exception e) {
			sourceTimeFunction = -1;
			sourceTimeFunctionPath = getPath("sourceTimeFunction");
		}
		modelName = property.getProperty("modelName");

		partialTypes = Arrays.stream(property.getProperty("partialTypes").split("\\s+")).map(PartialType::valueOf)
				.collect(Collectors.toSet()).toArray(new PartialType[0]);
		
		tlen = Double.parseDouble(property.getProperty("tlen"));
		np = Integer.parseInt(property.getProperty("np"));
		
		double[] minFreqs = Stream.of(property.getProperty("minFreq").trim().split("\\s+")).mapToDouble(Double::parseDouble).toArray();
		double[] maxFreqs = Stream.of(property.getProperty("maxFreq").trim().split("\\s+")).mapToDouble(Double::parseDouble).toArray();
		if (minFreqs.length != maxFreqs.length)
			throw new RuntimeException("Error: number of entries for minFreq and maxFreq differ");
		frequencyRanges = new FrequencyRange[minFreqs.length];
		for (int i = 0; i < minFreqs.length; i++)
			frequencyRanges[i] = new FrequencyRange(minFreqs[i], maxFreqs[i]);
		
		partialSamplingHz = 20;
		// =Double.parseDouble(reader.getFirstValue("partialSamplingHz")); TODO

		finalSamplingHz = Double.parseDouble(property.getProperty("finalSamplingHz"));
		
		filterNp = Integer.parseInt(property.getProperty("filterNp"));
		
		testBP = Boolean.parseBoolean(property.getProperty("testBP"));
		
		outPartial = Boolean.parseBoolean(property.getProperty("outPartial"));
		
		weightingTypes = Stream.of(property.getProperty("weightingTypes").trim().split("\\s+")).map(type -> WeightingType.valueOf(type))
		 	.collect(Collectors.toList()).toArray(new WeightingType[0]);
		if (weightingTypes.length < 0)
			throw new IllegalArgumentException("Error: weightingTypes must be set");
//		if (weightingTypes.length > 1)
//			throw new IllegalArgumentException("Error: only 1 weighting type can be set now");

		correctionBootstrap = Boolean.parseBoolean(property.getProperty("correctionBootstrap"));
		nSample = Integer.parseInt(property.getProperty("nSample"));
		
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
		
		double[] tmpthetainfo = Stream.of(property.getProperty("thetaInfo").trim().split("\\s+")).mapToDouble(Double::parseDouble)
				.toArray();
		thetamin = tmpthetainfo[0];
		thetamax = tmpthetainfo[1];
		dtheta = tmpthetainfo[2];
		
		numberOfBuffers = Integer.parseInt(property.getProperty("numberOfBuffers"));
		
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
		
		computationFlag = Integer.parseInt(property.getProperty("computationFlag"));

		unknownParameterPath = getPath("unknownParameterPath");
		
		if (property.containsKey("verticalMappingFile"))
			verticalMappingFile = Paths.get(property.getProperty("verticalMappingFile").trim());
		else
			verticalMappingFile = null;
		if (property.containsKey("horizontalMappingFile"))
			horizontalMappingFile = Paths.get(property.getProperty("horizontalMappingFile").trim());
		else
			horizontalMappingFile = null;
		
		writeA = Boolean.parseBoolean(property.getProperty("writeA"));
		if (writeA)
			partialIDs = new ArrayList<>();
		
	}
	
	
	Map<Phases, Integer> phaseMap;
	
	AtomicInteger windowCounter;
//	int windowCounter;
	
	Phases[] usedPhases;
	
	Map<HorizontalPosition, DSMOutput> bpMap;
	
	private final String stfcatName = "CATZ_STF.stfcat"; //LSTF1 ASTF1 ASTF2
	private final List<String> stfcat = readSTFCatalogue(stfcatName);
	
	private List<String> readSTFCatalogue(String STFcatalogue) throws IOException {
//		System.out.println("STF catalogue: " +  STFcatalogue);
		return IOUtils.readLines(SpcSAC.class.getClassLoader().getResourceAsStream(STFcatalogue)
					, Charset.defaultCharset());
	}
	
	/* (non-Javadoc)
	 * @see io.github.kensuke1984.kibrary.Operation#run()
	 */
	@Override
	public void run() throws IOException {
		setTimewindows();
		setBandPassFilter();
		setUnknownParameters();
		setWaveformData();
		canGO();
		
		String tempString = Utilities.getTemporaryString();
		
		partialPath = workPath.resolve("partial" + tempString + ".dat");
		partialIDPath = workPath.resolve("partialID" + tempString + ".dat");
		
		Path outUnknownPath = workPath.resolve("newUnknowns" + tempString + ".inf");
		Path outLayerPath = workPath.resolve("newPerturbationLayers" + tempString + ".inf");
		outputUnknownParameters(outUnknownPath);
		outputPerturbationLayers(outLayerPath);
		
		// redefine nwindowBuffer so that it divides timewindowInformation.size()
		nInteration = timewindowInformation.size() / nwindowBuffer;
		int newNwindowBuffer = nInteration == 0 ? timewindowInformation.size() : timewindowInformation.size() / nInteration;
		nwindowBufferLastIteration = timewindowInformation.size() - nInteration * newNwindowBuffer;
		System.out.println("nWindowBuffer (new, new_lastIteration, previous) = " + newNwindowBuffer + " "
				+ nwindowBufferLastIteration + " " + nwindowBuffer);
		nwindowBuffer = newNwindowBuffer;
		int iterationCount = 0;
		
		TimewindowInformation[] timewindowOrder = new TimewindowInformation[nwindowBuffer];
		
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
		
		ext = new int[frequencyRanges.length];
		step = new int[frequencyRanges.length];
		for (int i = 0; i < frequencyRanges.length; i++) {
			// バンドパスを安定させるためwindowを左右に ext = max period(s) ずつ伸ばす
			double minFreq = frequencyRanges[i].getMinFreq();
			ext[i] = (int) (1. / minFreq * partialSamplingHz);

			// sacdataを何ポイントおきに取り出すか
			step[i] = (int) (partialSamplingHz / finalSamplingHz);
		}
		
		//compute data variance
		computeVariance();
		//write data variance
		System.out.println("Writing residual variance...");
		Path outpath =  workPath.resolve("residualVariance" +  tempString + ".dat");
		ResidualVarianceFile.write(outpath, residualVarianceNumerator, residualVarianceDenominator, weightingTypes
				, frequencyRanges, usedPhases, correctionTypes, npts);
		
		Set<GlobalCMTID> usedEvents = timewindowInformation.stream()
				.map(tw -> tw.getGlobalCMTID())
				.collect(Collectors.toSet());
		
		Set<Station> usedStations = timewindowInformation.stream()
				.map(tw -> tw.getStation())
				.collect(Collectors.toSet());
		
		//--- initialize source time functions
		setSourceTimeFunctions(usedEvents);
		
		bpnames = Utilities.collectOrderedSpcFileName(bpPath);
		
		//--- for parallel computations
		int availabelProc = Runtime.getRuntime().availableProcessors();
		if (availabelProc < nproc)
			throw new RuntimeException("Insuficcient number of available processors " + nproc + " " + availabelProc);
		int N_THREADS = nproc;
		ExecutorService execs = Executors.newFixedThreadPool(N_THREADS);
		List<Callable<Object>> todo = new ArrayList<Callable<Object>>();
		
		//--- initialize counters
		windowCounter = new AtomicInteger();
		workProgressCounter = 0;
		progressStep = (int) (originalUnknownParameters.length * nwindowBuffer / 100.);
		
		for (GlobalCMTID event : usedEvents) {
			Set<TimewindowInformation> eventTimewindows = timewindowInformation.stream()
					.filter(tw -> tw.getGlobalCMTID().equals(event))
					.collect(Collectors.toSet());
			
			Set<Station> eventStations = eventTimewindows.stream().filter(tw -> tw.getGlobalCMTID().equals(event))
				.map(tw -> tw.getStation()).collect(Collectors.toSet());
			
			List<List<Integer>> IndicesEventBasicID = new ArrayList<>();
			for (int icorr = 0; icorr < correctionTypes.length; icorr++) {
				final int finalIcorr = icorr;
				List<Integer> tmplist = IntStream.range(0, basicIDArray[icorr].length).filter(i -> basicIDArray[finalIcorr][i].getGlobalCMTID().equals(event))
						.boxed().collect(Collectors.toList());
				IndicesEventBasicID.add(tmplist);
			}
			
			Path fpEventPath = fpPath.resolve(event.toString() + "/" + modelName);
			Set<SpcFileName> fpnames = Utilities.collectSpcFileName(fpEventPath);
			
			for (Station station : eventStations) {
				System.out.println("Working for " + event + " " + station);
				
				Set<TimewindowInformation> recordTimewindows = eventTimewindows.stream().filter(tw -> tw.getStation().equals(station))
						.collect(Collectors.toSet());
				
				List<List<Integer>> IndicesRecordBasicID = new ArrayList<>();
				for (int icorr = 0; icorr < correctionTypes.length; icorr++) {
					final int finalIcorr = icorr;
					List<Integer> tmplist = IndicesEventBasicID.get(icorr).stream().filter(i -> basicIDArray[finalIcorr][i].getStation().equals(station))
							.collect(Collectors.toList());
					IndicesRecordBasicID.add(tmplist);
				}
				
				List<TimewindowInformation> orderedRecordTimewindows = new ArrayList<>();
				for (SACComponent component : components) {
					for (TimewindowInformation timewindow : recordTimewindows) {
						if (timewindow.getComponent().equals(component))
							orderedRecordTimewindows.add(timewindow);
					}
				}
				
				//--- initialize Partial vector
				if (windowCounter.get() == 0) {
					partials = new double[nOriginalUnknown][][][][][];
					
					for (int i = 0; i < nOriginalUnknown; i++) {
						partials[i] = new double[nWeight][][][][];
						for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
							partials[i][iweight] = new double[nFreq][][][];
							for (int ifreq = 0; ifreq < nFreq; ifreq++) {
								partials[i][iweight][ifreq] = new double[usedPhases.length][][];
								for (int iphase = 0; iphase < usedPhases.length; iphase++) {
									partials[i][iweight][ifreq][iphase] = new double[nwindowBuffer][];
									for (int iwin = 0; iwin < nwindowBuffer; iwin++) {
										partials[i][iweight][ifreq][iphase][iwin] = new double[0];
									}
								}
							}
						}
					}
				}
				
				//--- compute partials
//				System.out.println("Computing partials...");
				int currentWindowCounter = windowCounter.get();
				
				for (SpcFileName fpname : fpnames) {
					todo.add(Executors.callable(new FPWorker(fpname, station, event, IndicesRecordBasicID, orderedRecordTimewindows, currentWindowCounter)));
				}
				
				if (orderedRecordTimewindows.size() != 1)
					throw new RuntimeException("More than one timewindow");
				timewindowOrder[currentWindowCounter] = orderedRecordTimewindows.get(0);
				
				if ( (windowCounter.incrementAndGet() == nwindowBuffer && iterationCount < nInteration) 
						|| (windowCounter.get() == nwindowBufferLastIteration && iterationCount == nInteration) ) {
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
					} // END IF computation flag (compute AtA?)
					
					if (writeA) // write A
						fillA(timewindowOrder);
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
		
		if (writeA)
			writeA(partialIDPath, partialPath, usedStations, usedEvents);
		
		if (!testBP) {
			//--- write Atd
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
	
	private void fillA(TimewindowInformation[] timewindows) {
		for (int iunknown = 0; iunknown < nNewUnknown; iunknown++) {
			int[] iOriginalUnknowns; 
			
			if (horizontalMapping != null) {
				iOriginalUnknowns = horizontalMapping.getiNewToOriginal(iunknown);
			}
			else if (threedMapping != null) {
				iOriginalUnknowns = threedMapping.getiNewToOriginal(iunknown);
			}
			else {
				iOriginalUnknowns = mapping.getiNewToOriginal(iunknown);
			}
			
			for (int iweight = 0; iweight < weightingTypes.length; iweight++) {
				for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
					for (int iphase = 0; iphase < usedPhases.length; iphase++) {
						for (int iwin = 0; iwin < nwindowBuffer; iwin++) {
		//					TimewindowInformation info = orderedRecordTimewindows.get(l);
		//					Phases phases = new Phases(info.getPhases());
							Phases phases = usedPhases[iphase];
							Phase[] phaseArray = phases.toSet().toArray(new Phase[0]);
							
		//					System.out.println(partials[iunknown][iweight][ifreq].length + " " + l);
		//					double[] partiali = partials[iunknown][iweight][ifreq][iphase][iwin];
		//					double[] partialj = partials[junknown][iweight][ifreq][iphase][iwin];
							
							int it = partials[iunknown][iweight][ifreq][iphase][iwin].length;
							if (it > 0) {
								double[] partiali = new double[it];
								for (int iOriginal = 0; iOriginal < iOriginalUnknowns.length; iOriginal++) {
									for (int k = 0; k < partiali.length; k++)
										partiali[k] += partials[iOriginalUnknowns[iOriginal]][iweight][ifreq][iphase][iwin][k];
								}
								
								TimewindowInformation window = timewindows[iwin];
								PartialID partialID = new PartialID(window.getStation(), window.getGlobalCMTID(), window.getComponent()
										, finalSamplingHz, window.getStartTime(), it, 1./frequencyRanges[ifreq].getMaxFreq()
										, 1./frequencyRanges[ifreq].getMinFreq(), phaseArray, 0, true, newUnknownParameters[iunknown].getLocation()
										, newUnknownParameters[iunknown].getPartialType(), partiali);
								
								partialIDs.add(partialID);
							}
							else
								System.out.println(0);
						}
					}
				}
			}
		}
	}
	
	private void writeA(Path idPath, Path dataPath, Set<Station> stations, Set<GlobalCMTID> events) throws IOException {
		double[][] periodRanges = new double[][] { {1./frequencyRanges[0].getMaxFreq(), 1./frequencyRanges[0].getMinFreq()} };
		Phase[] phaseArray = usedPhases[0].toSet().toArray(new Phase[0]);
		Set<Location> perturbationPoints = Stream.of(newUnknownParameters).map(p -> p.getLocation()).collect(Collectors.toSet());
		WaveformDataWriter writer = new WaveformDataWriter(idPath, dataPath, stations, events, periodRanges, phaseArray, perturbationPoints);
		
		for (PartialID partial : partialIDs)
			writer.addPartialID(partial);
		
		writer.close();
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
				double halfDuration1 = 0.;
	        	double halfDuration2 = 0.;
		      	for (String str : stfcat) {
		      		String[] stflist = str.split("\\s+");
		      	    GlobalCMTID eventID = new GlobalCMTID(stflist[0]);
		      	    if(id.equals(eventID)) {
		      	    	halfDuration1 = Double.valueOf(stflist[1]);
		      	    	halfDuration2 = Double.valueOf(stflist[2]);
		      	    	if(Integer.valueOf(stflist[3]) < 5.) {
		      	    		halfDuration1 = id.getEvent().getHalfDuration();
		      	    		halfDuration2 = id.getEvent().getHalfDuration();
		      	    	}
//		      	    	System.out.println( "DEBUG1: GET STF of " + eventID
//		      	    		+ " halfDuration 1 is " + halfDuration1 + " halfDuration 2 is " + halfDuration2 );
		      	    }
		      	}          	 
	            stf = SourceTimeFunction.asymmetrictriangleSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration1, halfDuration2);
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
				boolean found = false;
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
			
			filter[i] = tmpfilter;
		}
	}
	
	
	private void setUnknownParameters() throws IOException {
		originalUnknownParameters = UnknownParameterFile.read(unknownParameterPath).toArray(new UnknownParameter[0]);
		
		originalUnkownRadii = Stream.of(originalUnknownParameters).map(p -> p.getLocation().getR())
				.collect(Collectors.toSet());
		
		if (verticalMappingFile != null && horizontalMappingFile == null) {
			System.out.println("Using vertical mapping " + verticalMappingFile);
			mapping = new ParameterMapping(originalUnknownParameters, verticalMappingFile); 
			newUnknownParameters = mapping.getUnknowns();
			horizontalMapping = null;
			threedMapping = null;
		}
		else if (verticalMappingFile == null && horizontalMappingFile != null) {
			System.out.println("Using horizontal mapping " + horizontalMappingFile);
			horizontalMapping = new HorizontalParameterMapping(originalUnknownParameters, horizontalMappingFile);
			newUnknownParameters = mapping.getUnknowns();
			threedMapping = null;
		}
		else if (verticalMappingFile != null && horizontalMappingFile != null) {
			System.out.println("Using 3-D mapping " + verticalMappingFile + " " + horizontalMappingFile);
			threedMapping = new ThreeDParameterMapping(horizontalMappingFile, verticalMappingFile, originalUnknownParameters);
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
	}
	
	public BasicID[][] basicIDArray;
	
	private void setWaveformData() throws IOException {
		basicIDArray = new BasicID[waveformIDPath.length][];
		for (int i = 0; i < waveformIDPath.length; i++) {
			basicIDArray[i] = BasicIDFile.readBasicIDandDataFile(waveformIDPath[i], waveformPath[i]);
		}
	}
	
	private void canGO() {
		if (basicIDArray[0].length < 2 * timewindowInformation.size())
			throw new RuntimeException("Not enough waveforms for the given timewindow file");
	}
	
	private void outputUnknownParameters(Path outpath) throws IOException {
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outpath.toFile())));
		for (UnknownParameter p : newUnknownParameters)
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
	
	private List<SpcFileName> bpnames;
	private double[][][][][][] partials;
	
	public class FPWorker implements Runnable {
		
		SpcFileName fpname;
		Station station;
		GlobalCMTID event;
		List<List<Integer>> IndicesRecordBasicID;
		List<TimewindowInformation> orderedRecordTimewindows;
		int windowCounter;
		
		public FPWorker(SpcFileName fpname, Station station, GlobalCMTID event,
				List<List<Integer>> IndicesRecordBasicID, List<TimewindowInformation> orderedRecordTimewindows,  int windowCounter) {
			this.fpname = fpname;
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
			
			DSMOutput fpSpc = fpname.read();
			
			HorizontalPosition obsPos = fpSpc.getObserverPosition();
			double[] bodyR = fpSpc.getBodyR();
			
//			DSMOutput bpSpc = bpMap.get(obsPos);
			
//			Arrays.stream(bodyR).forEach(System.out::println);
		
			Location bpSourceLoc = station.getPosition().toLocation(Earth.EARTH_RADIUS);
			double distance = bpSourceLoc.getEpicentralDistance(obsPos) * 180. / Math.PI;
//			double distance = bpSourceLoc.getGeographicalDistance(obsPos) * 180. / Math.PI;
			double phi = Math.PI - bpSourceLoc.getAzimuth(obsPos);
			if (Double.isNaN(phi))
				throw new RuntimeException("Phi is NaN " + fpname + " " + station);
//			System.out.println("phi= " + phi);
		
//			System.out.println("geographic, geodetic distance = " + geocentricDistance + " " + distance);
			
			if (distance < thetamin || distance > thetamax)
				throw new RuntimeException("Error: cannot interpolate BP at epicentral distance " + distance + "(deg)");
			int ipointBP = (int) ((distance - thetamin) / dtheta);
			
			SpcFileName bpname1 = bpnames.get(ipointBP);
			SpcFileName bpname2 = bpnames.get(ipointBP + 1);
			SpcFileName bpname3 = bpnames.get(ipointBP + 2);
			
			double theta1 = thetamin + ipointBP * dtheta;
			double theta2 = theta1 + dtheta;
			double theta3 = theta2 + dtheta;
			double[] dh = new double[3];
			dh[0] = (distance - theta1) / dtheta;
			dh[1] = (distance - theta2) / dtheta;
			dh[2] = (distance - theta3) / dtheta;
			
//			System.out.println(obsPos + " " + distance + " " + distance*Math.PI/180. + " " + phi + " " + ipointBP + " " + theta1 + " " + theta2 + " " + dh[0] + " " + dh[1]);
//			System.out.println(bpname1);
//			System.out.println(bpname2);
//			System.out.println(bpname3);
			
			DSMOutput bpSpc1 = SpectrumFile.getInstance(bpname1, phi, obsPos, bpSourceLoc, fpname.getObserverName());
			DSMOutput bpSpc2 = SpectrumFile.getInstance(bpname2, phi, obsPos, bpSourceLoc, fpname.getObserverName());
			DSMOutput bpSpc3 = SpectrumFile.getInstance(bpname3, phi, obsPos, bpSourceLoc, fpname.getObserverName());
			
			t1f = System.currentTimeMillis();
//			System.out.println(Thread.currentThread().getName() + " Initialization took " + (t1f-t1i)*1e-3 + " s");
			
//			LookAtBPspc.printHeader(bpSpc);
//			LookAtFPspc.printHeader(fpSpc);

//------------------------------------- testBP ------------------------------
			if (testBP) {
				Path dirBP = workPath.resolve("bpwaves");
				Files.createDirectories(dirBP);
				for (int i = 0; i < bpSpc1.nbody(); i++) {
					
					SpcBody body1 = bpSpc1.getSpcBodyList().get(i);
					SpcBody body2 = bpSpc2.getSpcBodyList().get(i);
					SpcBody body3 = bpSpc3.getSpcBodyList().get(i);
					
					SpcBody body = SpcBody.interpolate(body1, body2, body3, dh);
					
//					SpcBody body = bpSpc.getSpcBodyList().get(i);
					
					int lsmooth = body.findLsmooth(tlen, partialSamplingHz);
					body.toTimeDomain(lsmooth);
					
					SpcComponent[] spcComponents = body.getSpcComponents();
					for (int j = 0; j < spcComponents.length; j++) {
						double[] bpserie = spcComponents[j].getTimeseries();
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
							}
						}
					}
				}
				// continue
			}
//----------------------------------------- END testBP ------------------------------------------------------------------------

//------------------------------------- compute partials ----------------------------------------------------------------
			t1i = System.currentTimeMillis();
			
			ThreeDPartialMaker threedPartialMaker = new ThreeDPartialMaker(fpSpc, bpSpc1, bpSpc2, bpSpc3, dh);
			
//			ThreeDPartialMaker threedPartialMaker = new ThreeDPartialMaker(fpSpc, bpSpc);
			
			SourceTimeFunction stf = getSourceTimeFunction(event);
			threedPartialMaker.setSourceTimeFunction(stf);
			
			t1f = System.currentTimeMillis();
//			System.out.println(Thread.currentThread().getName() + " Initialization2 (3DParMaker) took " + (t1f-t1i)*1e-3 + " s");
			
//			System.out.println(Thread.currentThread().getName() + " starts body loop...");
			t1i = System.currentTimeMillis();
			
			for (int ibody = 0; ibody < fpSpc.nbody(); ibody++) {
				Location parameterLoc = obsPos.toLocation(bodyR[ibody]);
				
				for (PartialType type : partialTypes) {
					int iunknown = getParameterIndex(parameterLoc, type);
					if (iunknown < 0) {
//						System.err.println("Warning: unkown in FP not found in the unknown parameter file");
						continue;
					}
					
//					int iNewUnknown = mapping.getiOriginalToNew(iunknown);
					
					double weightUnknown = originalUnknownParameters[iunknown].getWeighting();
					
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
									
									Phases phases = new Phases(info.getPhases());
									int iphase = phaseMap.get(phases);
									
									for (int icorr = 0; icorr < correctionTypes.length; icorr++) {
										
										final int finalIcorr = icorr;
										final int finalIfreq = ifreq;
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
										double[] residual = new double[obsData.length];
										
										double weight = computeWeight(weightingTypes[iweight], obsID, synID);
										
										if (Double.isNaN(weight))
											throw new RuntimeException("Weight is NaN " + info);
										
										for (int k = 0; k < obsData.length; k++) {
											residual[k] = (obsData[k] - synData[k]) * weight;
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
											tmpatd += cutU[k] * residual[k];
										}
										
//										System.out.println(iunknown + " " + new ArrayRealVector(cutU).getLInfNorm());
										partials[iunknown][iweight][ifreq][iphase][windowCounter] = cutU;
										
										if (cutU.length == 0) {
											throw new RuntimeException(Thread.currentThread().getName() + " Unexpected: cutU (partial) has length 0 "
													+ originalUnknownParameters[iunknown] + " " + weightingTypes[iweight] + " " + frequencyRanges[ifreq]
													+ " " + info);
										}
										 
//										parameterLoc = originalUnknownParameters[iunknown].getLocation();
										// add entry to Atd
//										AtdEntry entry = new AtdEntry(weightingTypes[iweight], frequencyRanges[ifreq]
//												, phases, type, parameterLoc, tmpatd); //TODO parameterLoc
										
										double value = atdEntries[iunknown][iweight][ifreq][iphase][icorr].getValue();
										value += tmpatd;
										
										if (Double.isNaN(value))
											throw new RuntimeException("Atd value is NaN" + originalUnknownParameters[iunknown] + " " + info);
										
										atdEntries[iunknown][iweight][ifreq][iphase][icorr].setValue(value);
										
//										AtdEntry tmp = atdEntries[iunknown][iweight][ifreq][iphase];
//										tmp.add(entry);
//										atdEntries[iunknown][iweight][ifreq][iphase] = tmp;
										// END add entry to Atd
									
									} // END correction type
								} // END timewindow
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
						Phases phases = usedPhases[iphase];
						
//						System.out.println(partials[iunknown][iweight][ifreq].length + " " + l);
//						double[] partiali = partials[iunknown][iweight][ifreq][iphase][iwin];
//						double[] partialj = partials[junknown][iweight][ifreq][iphase][iwin];
						
						int it = partials[iunknown][iweight][ifreq][iphase][iwin].length;
						
						double[] partiali = new double[it];
						double[] partialj = new double[it];
						for (int iOriginal = 0; iOriginal < iOriginalUnknowns.length; iOriginal++) {
							for (int k = 0; k < partiali.length; k++)
								partiali[k] += partials[iOriginalUnknowns[iOriginal]][iweight][ifreq][iphase][iwin][k];
						}
						for (int jOriginal = 0; jOriginal < jOriginalUnknowns.length; jOriginal++) {
							for (int k = 0; k < partialj.length; k++)
								partialj[k] += partials[jOriginalUnknowns[jOriginal]][iweight][ifreq][iphase][iwin][k];
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
