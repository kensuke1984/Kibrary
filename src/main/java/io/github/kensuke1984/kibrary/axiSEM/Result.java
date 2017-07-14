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
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * @author anpan
 * @version 0.1
 */
public class Result {
	private String meshName;
	private List<BasicID> basicIDs;
	
	public static void main(String[] args) {
//		Result result = new Result("PREM_6s", 8., 200.);
		
		// test of resampling method
		double[] data = new double[100];
		for (int i = 0; i < data.length; i++)
			data[i] = Math.sin(i * 2 * Math.PI / 50);
		double newDelta = 0.05;
		double oldDelta = 0.047;
		double[] newData = resampleFFT(data, oldDelta, newDelta);
		for (int i = 0; i < data.length; i++)
			System.out.format("%.3f %.3f%n", i * oldDelta, data[i]);
		System.out.println("\nResampled");
		for (int i = 0; i < newData.length; i++)
			System.out.format("%.3f %.3f%n", i * newDelta, newData[i]);
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
						.resolve("SOLVER/" + meshName + "Data_Postprocessing/SEISMOGRAMS");
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
				Path meshHeaderFile = solverFolder.resolve("mesh_params.h");
				double timeStep = 0.;
				BufferedReader readerMesh = Files.newBufferedReader(meshHeaderFile);
				line = null;
				while ((line = readerMesh.readLine()) != null) {
					if (line.contains("Time step [s]"))
						timeStep = Double.parseDouble(line.split(":")[1].trim());
				}
				System.out.format("Time step = %.4f s%n", timeStep);
				
				// parse inparam_basic to get the simulation length (s)
				Path inparamFile = solverFolder.resolve("inparam_basic");
				double tlen = 0.;
				BufferedReader readerParam = Files.newBufferedReader(inparamFile);
				line = null;
				while ((line = readerParam.readLine()) != null) {
					if (line.contains("SEISMOGRAM_LENGTH"))
						tlen = Double.parseDouble(line.split(" ")[1].trim());
				}
				System.out.format("Length of the seismograms = %.1 s%n", tlen);
				
				// source time function
				double halfDuration = eventFolder.getGlobalCMTID().getEvent().getHalfDuration();
				double thisSamplingHz = 1. / timeStep;
				int np = (int) (tlen * timeStep);
				SourceTimeFunction stf = SourceTimeFunction
						.triangleSourceTimeFunction(np, tlen, thisSamplingHz, halfDuration);
				
				for (File file : resultFolder.toFile().listFiles()) {
					if (!file.getName().endsWith(".dat"))
						continue;
					System.out.println(file);
					List<Double> times = new ArrayList<>();
					List<Double> amps = new ArrayList<>();
					BufferedReader reader = Files.newBufferedReader(file.toPath());
					line = null;
					while ((line = reader.readLine()) != null) {
						String[] s = line.split(" ");
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
					System.out.println(station);
					
					// get data and resample to DELTA=0.05 s
					double[] data = new double[amps.size()];
					for (int i = 0; i < data.length; i++)
						data[i] = amps.get(i);
					data = resample(data, 1. / thisSamplingHz, DELTA);
					
					// convolve data
					stf.convolve(data);
					
					// filter data
					data = filter.applyFilter(data);
					
					BasicID basicID = new BasicID(WaveformType.SYN, DELTA, 0.0
							, times.size(), station, id, component
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
	
	private double[] ApplyTaper(double y[]) {
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
	
	public static double[] resampleFFT(double[] data, double oldDelta, double newDelta) {
		int npow2 = Integer.highestOneBit(data.length) < data.length 
				? Integer.highestOneBit(data.length) * 2 : Integer.highestOneBit(data.length);
		double[] dataPadded = Arrays.copyOf(data, npow2);
		Complex[] transformed = fft.transform(dataPadded, TransformType.FORWARD);
		
		int newLength = (int) (data.length * oldDelta / newDelta);
		double[] newData = new double[newLength];
		
		double domega = 2 * Math.PI / npow2;
		for (int i = 0; i < newData.length; i++) {
			Complex z = Complex.ZERO;
			for (int j = 0; j < data.length; j++) {
				double phase = domega * j * i * oldDelta;
				Complex tmp = transformed[j].multiply(new Complex(phase, phase));
				z = z.add(tmp);
			}
			newData[i] = z.getReal();
		}
		
		return newData;
	}
	
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
