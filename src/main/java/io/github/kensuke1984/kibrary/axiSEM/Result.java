package io.github.kensuke1984.kibrary.axiSEM;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class Result {
	private String meshName;
	private List<BasicID> basicIDs;
	
	public static void main(String[] args) {
		Result result = new Result("PREM_50s", 50., 200.);
		List<BasicID> basicIDs = result.getBasicIDs();
		for (BasicID id : basicIDs) {
			System.out.println(id);
			if (id.getSacComponent().equals(SACComponent.T)) {
				double[] data = id.getData();
				for (int i = 0; i < id.getNpts(); i++)
					System.out.format("%.4f %.4e%n", i / id.getSamplingHz()
							, data[i]);
			}
		}
	
//		 test of resampling method
		/*
		double[] data = new double[1000];
		for (int i = 0; i < data.length; i++)
			data[i] = Math.sin(i * 2 * Math.PI / 500.) + 2 * Math.sin(i * 2 * Math.PI / 200.);
		double newDelta = 0.05;
		double oldDelta = 0.047;
		double[] newData = resampleFFT(data, oldDelta, newDelta);
		double maxOld = new ArrayRealVector(data).getLInfNorm();
		for (int i = 0; i < data.length; i++)
			System.out.format("%.4f %.4f%n", i * oldDelta, data[i]);
		System.out.println("");
		double max = new ArrayRealVector(newData).getLInfNorm();
		for (int i = 0; i < newData.length; i++)
			System.out.format("%.4f %.4f%n", i * newDelta, newData[i] / max * maxOld);
		*/
	}
	
	public Result(String meshName, double minPeriod, double maxPeriod) {
		this.meshName = meshName;
		this.basicIDs = new ArrayList<>();
		final double DELTA = 0.05;
		
		Path workingDir = Paths.get(".");
		try {
			Set<EventFolder> eventFolderSet = Utilities.eventFolderSet(workingDir);
			for (EventFolder eventFolder : eventFolderSet) {
				Path resultFolder = eventFolder.toPath()
						.resolve("SOLVER/" + meshName + "/Data_Postprocessing/SEISMOGRAMS");
				Path solverFolder = eventFolder.toPath().resolve("SOLVER");
				
				// get stations
				Path stationFile = solverFolder.resolve("STATIONS");
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
				
				// filter
				double omegaH = 1. / minPeriod * 2 * Math.PI * DELTA;
				double omegaL = 1. / maxPeriod * 2 * Math.PI * DELTA;
				ButterworthFilter filter = new BandPassFilter(omegaH, omegaL, 4);
				filter.setBackward(false);
				
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
				if (timeStep != DELTA)
					throw new IllegalArgumentException(
							String.format("Illegal time step (should be 0.05): "
									, timeStep));
					
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
						if (sta.getStationName().equals(thisStationName) 
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
					double halfDuration = eventFolder.getGlobalCMTID().getEvent().getHalfDuration();
					int npts = (int) (tlen / DELTA);
					data = Arrays.copyOf(data, npts);
//					TODO think about a good value for np
					int np = 512;
					SourceTimeFunction stf = SourceTimeFunction
							.triangleSourceTimeFunction(np, tlen, 1. / DELTA, halfDuration);
					
					// convolve data
					stf.convolve(data);
					
					// filter data
					data = filter.applyFilter(data);
					
					BasicID basicID = new BasicID(WaveformType.SYN, 1. / DELTA, 0.0
							, data.length, station, id, component
							, minPeriod, maxPeriod, phases, 0, true, data);
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
	
	private final static FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
	
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
			for (int j = 0; j < 512; j++) {
				double phase = domega * j * i * newDelta;
				Complex tmp = new Complex(Math.cos(phase), Math.sin(phase));
				z = z.add(transformed[j].multiply(tmp));
			}
			newData[i] = z.getReal();
		}
		
		return newData;
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
	public static double[] resample(double[] data
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
}
