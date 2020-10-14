package io.github.kensuke1984.kibrary.axiSEM;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.dsminformation.SshDSMInformationFileMaker;
import io.github.kensuke1984.kibrary.timewindow.TimewindowMaker;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.http.annotation.Experimental;

/**
 * @author anpan
 * @version 0.1
 */
public class Result implements Operation {
	private String meshName;
	private List<BasicID> basicIDs;
	private double highFreq;
	private double lowFreq;
	private Path workPath;
	private Set<SACComponent> components;
	
	private final double DELTA = 0.05;
	
	public static void main(String[] args) throws IOException {
		Properties property = new Properties();
		if (args.length == 0)
			property.load(Files.newBufferedReader(Operation.findPath()));
		else if (args.length == 1)
			property.load(Files.newBufferedReader(Paths.get(args[0])));
		else
			throw new IllegalArgumentException("too many arguments. It should be 0 or 1 (property file name)");

		Result result = new Result(property);
		System.err.println(Result.class.getName() + " is going.");
		long startT = System.nanoTime();
		result.run();
		System.err.println(
				Result.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - startT));
	
//		 test of resampling method
//		/*
//		double[] data = new double[1000];
//		for (int i = 0; i < data.length; i++)
//			data[i] = Math.sin(i * 2 * Math.PI / 500.);
//		double newDelta = 0.05 * 2 * Math.PI / 500.;
//		double oldDelta = 0.047 * 2 * Math.PI / 500.;
//		double[] newData = resampleFFT(data, oldDelta, newDelta);
//		double maxOld = new ArrayRealVector(data).getLInfNorm();
//		for (int i = 0; i < data.length; i++)
//			System.out.format("%.4f %.4f%n", i * oldDelta, data[i]);
//		System.out.println("");
//		double max = new ArrayRealVector(newData).getLInfNorm();
//		for (int i = 0; i < newData.length; i++)
//			System.out.format("%.4f %.4f%n", i * newDelta, newData[i]);
//		
////		/*
//		double[] velData = displacementTOvelocityTimeDomain(data, 2 * Math.PI / 500.);
//		for (int i = 0; i < data.length; i++)
//			System.out.format("%d %.4f %.4f%n", i, data[i], velData[i]);
////		*/
//		*/
	}
	
	private Properties property;

	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("meshName"))
			throw new RuntimeException("meshName must be defined.");
		if (!property.containsKey("lowFreq"))
			property.setProperty("lowFreq", "0.005");
		if (!property.containsKey("highFreq"))
			property.setProperty("highFreq", "0.02");
	}
	
	private void set() {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));
		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());
		meshName = property.getProperty("meshName").trim();
		lowFreq = Double.parseDouble(property.getProperty("lowFreq"));
		highFreq = Double.parseDouble(property.getProperty("highFreq"));
		basicIDs = new ArrayList<>();
	}
	
	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths
				.get(SshDSMInformationFileMaker.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan Result");
			pw.println("##These are the properties for Result");
			pw.println("##Path of a work folder (.)");
			pw.println("#workPath");
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("##Name of the mesh (model) used for AxiSEM simulation (must be set)");
			pw.println("#meshName");
			pw.println("##Lower frequency for the bandpass filter (0.005)");
			pw.println("#lowFreq");
			pw.println("##Higher frequency for the bandpass filter (0.02)");
			pw.println("#highFreq");
		}
		System.err.println(outPath + " is created.");
	}
	
	@Override
	public void run() {
		for (BasicID id : basicIDs) {
			System.out.println(id);
			if (id.getSacComponent().equals(SACComponent.T)) {
				double[] data = id.getData();
				for (int i = 0; i < id.getNpts(); i++)
					System.out.format("%.4f %.4e%n", i / id.getSamplingHz()
							, data[i]);
			}
		}
	}
	
	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}
	
	@Override
	public Path getWorkPath() {
		return workPath;
	}
	
	public Result(Properties property) {
		this.property = (Properties) property.clone();
		set();
		
		try {
			Set<EventFolder> eventFolderSet = Utilities.eventFolderSet(workPath);
			for (EventFolder eventFolder : eventFolderSet) {
				Path resultFolder = eventFolder.toPath()
						.resolve("SOLVER/" + meshName + "/Data_Postprocessing/SEISMOGRAMS");
				Path solverFolder = eventFolder.toPath().resolve("SOLVER");
				
				// get stations
				Path stationFile = solverFolder.resolve("station.inf");
				Set<Station> stationSet = new HashSet<>();
				BufferedReader readerStation = Files.newBufferedReader(stationFile);
				String line = null;
				while ((line = readerStation.readLine()) != null) {
					String[] s = line.split(" ");
					String stationName = s[0];
					String network = s[1];
					double latitude = Double.parseDouble(s[2]);
					double longitude = Double.parseDouble(s[3]);
					Station station = new Station(stationName
							, new HorizontalPosition(latitude, longitude), network);
					stationSet.add(station);
				}
				
				// parse mesh header file to get the time step
				Path param_advancedFile = solverFolder.resolve(meshName + "/inparam_advanced");
				double timeStep = 0.;
				BufferedReader readerAdvanced = Files.newBufferedReader(param_advancedFile);
				line = null;
				while ((line = readerAdvanced.readLine()) != null) {
					if (line.startsWith("#"))
						continue;
					if (line.contains("TIME_STEP"))
						timeStep = Double.parseDouble(line.trim().split("\\s+")[1]);
				}
				readerAdvanced.close();
				if (timeStep == 0.) {
					Path meshHeaderFile = solverFolder.resolve("mesh_params.h");
					
					BufferedReader readerMesh = Files.newBufferedReader(meshHeaderFile);
					line = null;
					while ((line = readerMesh.readLine()) != null) {
						if (line.startsWith("#"))
							continue;
						if (line.contains("Time step [s]"))
							timeStep = Double.parseDouble(line.split(":")[1].trim());
					}
					readerMesh.close();
				}
				System.out.format("Time step = %.4f s%n", timeStep);
				if (timeStep / DELTA != (int) (timeStep / DELTA) || DELTA / timeStep != (int) (DELTA / timeStep))
							String.format("Illegal time step (should be a multiple of 0.05): "
									, timeStep);
				
				// filter
				double omegaH = highFreq * 2 * Math.PI * timeStep;
				double omegaL = lowFreq * 2 * Math.PI * timeStep;
				ButterworthFilter filter = new BandPassFilter(omegaH, omegaL, 4);
				filter.setBackward(false);
					
				// parse inparam_basic to get the simulation length (s)
				Path inparamFile = solverFolder.resolve("inparam_basic");
				double tlen = 0.;
				BufferedReader readerParam = Files.newBufferedReader(inparamFile);
				line = null;
				while ((line = readerParam.readLine()) != null) {
					if (line.startsWith("#"))
						continue;
					if (line.contains("SEISMOGRAM_LENGTH")) {
						tlen = Double.parseDouble(line.split("\\s+")[1]);
					}
				}
				System.out.format("Length of the seismograms = %.1f s%n", tlen);
				readerParam.close();
				
				for (File file : resultFolder.toFile().listFiles()) {
					if (!file.getName().endsWith(".dat"))
						continue;
					List<Double> times = new ArrayList<>();
					List<Double> amps = new ArrayList<>();
					BufferedReader reader = Files.newBufferedReader(file.toPath());
					line = null;
					while ((line = reader.readLine()) != null) {
						String[] s = line.trim().split("\\s+");
						times.add(Double.parseDouble(s[0]));
						amps.add(Double.parseDouble(s[1]));
					}
					reader.close();
					
					GlobalCMTID id = eventFolder.getGlobalCMTID();
					SACComponent component = getSACComponent(file.getName());
					Phase[] phases = new Phase[0];
					
					// get this file's station
					String thisStationName = file.getName().split("_")[0];
					String thisNetwork = file.getName().split("_")[1];
					Station station = null;
					for (Station sta : stationSet) {
						if (sta.getName().equals(thisStationName) 
								&& sta.getNetwork().equals(thisNetwork)) {
							station = new Station(sta);
							break;
						}
					}
					
					// get data
//					TODO resampling
					double thisSamplingHz = 1. / timeStep;
					double[] data = new double[amps.size()];
					for (int i = 0; i < data.length; i++)
						data[i] = amps.get(i);
//					data = resample(data, timeStep, DELTA);
					
					// source time function
					double halfDuration = eventFolder.getGlobalCMTID()
							.getEvent().getHalfDuration();
//					int npts = (int) (tlen / DELTA);
//					data = Arrays.copyOf(data, npts);
////					TODO think about a good value for np
//					int np = 128;
//					SourceTimeFunction stf = SourceTimeFunction
//							.triangleSourceTimeFunction(np, tlen
//									, 1. / DELTA, halfDuration);
					
					// convolve data
					data = convolveTriangleTimeDomainNonCausal(data, timeStep, halfDuration);
					
					// filter data
					data = filter.applyFilter(data);
					
					// convert synthetics from displacement to velocity
					data = displacementTOvelocityTimeDomain(data, timeStep);
					
					BasicID basicID = new BasicID(WaveformType.SYN, 1. / timeStep, 0.0
							, data.length, station, id, component
							, 1. / highFreq, 1. / lowFreq, phases, 0, true, data);
					basicIDs.add(basicID);
				}
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
		
	}
	
	public List<BasicID> getBasicIDs() {
		return basicIDs;
	}
	
	private static SACComponent getSACComponent(String filename) {
		SACComponent component = null;
		if (filename.endsWith("T.dat"))
			component = SACComponent.T;
		else if (filename.endsWith("R.dat"))
			component = SACComponent.R;
		else if (filename.endsWith("Z.dat"))
			component = SACComponent.Z;
		else
			System.err.format("Illegal SAC component: %s%n", filename);
		return component;
	}
	
	private final static FastFourierTransformer fft = 
			new FastFourierTransformer(DftNormalization.STANDARD);
	
	private static double[] applyTaper(double y[]) {
		double[] taped = y.clone();
		int width = 4;//(int) (y.length / 50.);
		if (width < 2)
			width = 2;
		for (int i = 0; i < y.length; i++) {
			double f = 1;
			if (i < width)
				f = (double) i / width;
			else if (i > y.length - width - 1)
				f = (double) (y.length - i - 1) / width;
			taped[i] *= f ;
		}
		return taped;
	}
	
//	TODO 
	/**
	 * DO NOT USE: buggy
	 * @param data
	 * @param oldDelta
	 * @param newDelta
	 * @return
	 */
	@Experimental
	public static double[] resampleFFT(double[] data, double oldDelta, double newDelta) {
		int npow2 = Integer.highestOneBit(data.length) < data.length 
				? Integer.highestOneBit(data.length) * 2 : Integer.highestOneBit(data.length);
		double[] dataPadded = Arrays.copyOf(data, npow2);
		dataPadded = applyTaper(dataPadded);
		Complex[] transformed = fft.transform(dataPadded, TransformType.FORWARD);
		
		int newLength = (int) (data.length * oldDelta / newDelta);
		double[] newData = new double[newLength];
		
		double domega = 2 * Math.PI / (npow2 * oldDelta);
		for (int i = 0; i < newData.length; i++) {
			Complex z = Complex.ZERO;
			for (int j = 0; j < data.length; j++) {
				double phase = domega * j * i * newDelta;
				Complex tmp = new Complex(Math.cos(phase), Math.sin(phase));
				z = z.add(transformed[j].multiply(tmp));
			}
			newData[i] = z.getReal() / newData.length;
		}
		
		return newData;
	}
	
//	TODO 
	/**
	 * DO NOT USE: not completed, does not work yet.
	 * @param data
	 * @param oldDelta
	 * @param newDelta
	 * @return
	 */
	@Experimental 
	public static double[] resampleTimeDomain(double[] data
			, double oldDelta, double newDelta) {
		if (newDelta == oldDelta)
			return data;
		
		double[] newData = new double[(int) (data.length 
		                              * oldDelta / newDelta)];
		newData[0] = data[0];
		int iOld = 1;
		for (int iNew = 1; iNew < newData.length; iNew++) {
			double t = iOld * newDelta;
			double oldt = iOld * oldDelta;
			int previousIOld = iOld;
			while (oldt < t) {
				iOld++;
				oldt += oldDelta;
			}
			double previousData = data[previousIOld];
			double currentData = data[iOld];
			
			newData[iNew] = newDelta * (currentData - previousData) 
					/ ((iOld - previousIOld) * oldDelta);
		}
		return newData;
	}
	
	/**
	 * Experimental
	 * DO NOT USE: buggy
	 * @param data
	 * @param delta
	 * @return
	 */
	@Experimental
	public static double[] displacementTOvelocityFFT(double[] data, double delta) {
		int npow2 = Integer.highestOneBit(data.length) 
				< data.length 
				? Integer.highestOneBit(data.length) * 2 
				: Integer.highestOneBit(data.length);
		double[] dataPadded = Arrays.copyOf(data, npow2);
		dataPadded = applyTaper(dataPadded);
		Complex[] transformed = fft.transform(dataPadded
				, TransformType.FORWARD);
		double domega = 2 * Math.PI / (transformed.length * delta);
		for (int i = 0; i < transformed.length; i++) {
			double omega = i * domega;
			transformed[i] = transformed[i]
					.multiply(new Complex(0., -omega));
			if (i >= data.length)
				transformed[i] = Complex.ZERO;
		}
		double[] velData = new double[data.length];
		Complex[] complexVelData = fft.transform(transformed, TransformType.INVERSE);
		for (int i = 0; i < data.length; i++)
			velData[i] = complexVelData[i].getReal();
		
		return velData;
	}
	
	/**
	 * Using five-point stencil https://en.wikipedia.org/wiki/Five-point_stencil
	 * @param data
	 * @return
	 */
	public static double[] displacementTOvelocityTimeDomain(double[] data, double delta) {
		int n = data.length;
		double[] velData = new double[n];
//		velData[0] = (-data[2] + 8 * data[1]) / (12. * delta);
//		velData[1] = (-data[3] + 8 * data[2] - 8 * data[0]) / (12. * delta);
//		velData[n-1] = (-8 * data[n-2] + data[n-3]) / 12.;
//		velData[n-2] = (8 * data[n-1] - 8 * data[n-3] + data[n-4]) / (12. * delta);
		velData[0] = (data[1] - data[0]) / delta;
		velData[1] = (data[2] - data[0]) / (2 * delta);
		velData[n-1] = (data[n-1] - data[n-2]) / delta;
		velData[n-2] = (data[n-1] - data[n-3]) / (2 * delta);
		for (int i = 2; i < n - 2; i++)
			velData[i] = (-data[i+2] + 8 * data[i+1] 
					- 8 * data[i-1] + data[i-2]) / (12. * delta);
		return velData;
	}
	
	public static double[] convolveTriangleTimeDomain(double[] data, double delta, double halfWidth) {
		double[] convolvedData = new double[data.length];
		int periodPoint = (int) (2 * halfWidth / delta) + 1;
		
		for (int i = 0; i < data.length; i++) {
			if (i < periodPoint)
				continue;
			double value = 0;
			for (int j = 0; j < periodPoint; j++) {
				value += data[i - j] * triangle(j, periodPoint, delta);
			}
			convolvedData[i] = value;
		}
		return convolvedData;
	}
	
	private static double triangle(int i, int n, double delta) {
		int nhalf = n / 2;
		if (i < nhalf)
			return 1. / nhalf * (double) i / nhalf / delta;
		else if (i > nhalf)
			return -1. / nhalf * (double) (i - n) / nhalf / delta;
		else
			return 1. / nhalf / delta;
	}
	
	private static double triangleNonCausal(int i, int n, double delta) {
		if (i < 0)
			return 1. / n * (double) (i + n) / n;
		else if (i > 0)
			return 1. / n * (double) (n - i) / n;
		else
			return 1. / n;
	}
	
	public static double[] convolveTriangleTimeDomainNonCausal(double[] data, double delta, double halfWidth) {
		double[] convolvedData = new double[data.length];
		int periodPoint = (int) (halfWidth / delta);
		
		for (int i = periodPoint; i < data.length - periodPoint; i++) {
			double value = 0;
			for (int j = -periodPoint; j <= periodPoint; j++) {
				value += data[i - j] * triangleNonCausal(j, periodPoint, delta);
			}
			convolvedData[i] = value;
		}
		return convolvedData;
	}
	
}
