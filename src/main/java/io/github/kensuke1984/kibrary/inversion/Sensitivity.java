package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;
import io.github.kensuke1984.kibrary.inversion.ObservationEquation;
import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
//import io.github.kensuke1984.kibrary.VelocityField;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

public class Sensitivity {
	
	private static double[][] histogramDistance;
	private static double[][] histogramAzimuth;
	
	public static void main(String[] args) throws IOException {
//		if (args.length != 8) {
//			System.err.println("usage: path to \"HOME\" directory, relative path from \"HOME\" directory to outputfile,"
//					+ "relative path from \"HOME\" to ");
//			throw new IllegalArgumentException();
//		}	
		double minDistance = 0;
		double maxDistance = 100;
		int[] npts = new int[] {131072, 262144};
		Path eventPath = Paths.get(args[0]);
		System.out.println(eventPath);
		try {
			Path outPath = eventPath.resolve("sensitivity.txt"); //Paths.get("/Users/Yuki/Dropbox/SKB_debug/Sensitivity_debug/sensitivity.txt");
			Set<Station> stationInformation = StationInformationFile.read(eventPath.resolve("station.inf"));
			PartialID[] partialIDs = PartialIDFile.readPartialIDandDataFile(eventPath.resolve("partialID.dat"),
					eventPath.resolve("partial.dat"));
			BasicID[] basicIDs = BasicIDFile.readBasicIDandDataFile(eventPath.resolve("waveformID.dat"), 
					eventPath.resolve("waveform.dat"));
			
			Path unknownsPath = eventPath.resolve("unknown.inf");
			
			List<UnknownParameter> parameterList = UnknownParameterFile.read(unknownsPath);
			Location[] parLoc = VelocityField.readPartialLocation(unknownsPath);
			
			Dvector dVector = new Dvector(basicIDs,
					id -> true, 
					(obs, syn) -> {
						double distance = obs.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
								stationInformation.stream().filter(sta -> sta.equals(obs.getStation())).findAny().get().getPosition())*180/Math.PI;
						
						if (distance < minDistance || distance > maxDistance) {
							return 0;
						}
//						else if (obs.getGlobalCMTID().equals(new GlobalCMTID("031704A")))
//							return 0;
						else if (obs.getStartTime() <= syn.getStartTime() - 10. || obs.getStartTime() >= syn.getStartTime() + 10.) {
							System.out.println("timeshift greater than 10 s for event " + obs.toString());
							return 0;
						}
//						else if (! DataSelection.selectNPTS(obs, npts, eventPath)) {
//							System.out.println("NPTS has some problem!");
//							return 0;
//						}
						else {
							if (obs.getSacComponent().equals(SACComponent.T) 
									&& syn.getSacComponent().equals(SACComponent.T) )
								return weightingReciprocalAmplitude(obs, syn);
							else {//if (obs.getSacComponent().equals(SACComponent.R)) {
								/**
								BasicID obsp = Arrays.stream(basicIDs)
										.filter(idp -> idp.getGlobalCMTID().equals(obs.getGlobalCMTID()))
										.filter(idp -> idp.getStation().getName().equals(obs.getStation().getName()))
										.filter(idp -> idp.getWaveformType().equals(obs.getWaveformType()))
										.filter(idp -> idp.getPhases().equals(obs.getPhases()))
										.filter(idp -> idp.getNpts() == obs.getNpts())
										.filter(idp -> idp.getSacComponent().equals(SACComponent.T))
										.findFirst().get();
								BasicID synp = Arrays.stream(basicIDs)
										.filter(idp -> idp.getGlobalCMTID().equals(syn.getGlobalCMTID()))
										.filter(idp -> idp.getStation().getName().equals(obs.getStation().getName()))
										.filter(idp -> idp.getWaveformType().equals(syn.getWaveformType()))
										.filter(idp -> idp.getPhases().equals(syn.getPhases()))
										.filter(idp -> idp.getNpts() == syn.getNpts())
										.filter(idp -> idp.getSacComponent().equals(SACComponent.T))
										.findAny().get();
								**/
								BasicID obsp = Arrays.stream(basicIDs)
										.filter(idp -> idp.getGlobalCMTID().equals(obs.getGlobalCMTID()))
										.findFirst().get();
								BasicID synp = Arrays.stream(basicIDs)
										.filter(idp -> idp.getGlobalCMTID().equals(syn.getGlobalCMTID()))
										.findFirst().get();
								System.out.print(obsp.toString()+"\n"+synp.toString());
								return weightingReciprocalAmplitude(obsp, synp);
//								return weightingEpicentralDistanceDpp(obs) * weightingAzimuthDpp(obs);
							}
						}
					});
			
			ObservationEquation equation = new ObservationEquation(partialIDs, parameterList, dVector, false, false, -1, null);
			
			double[] sensitivity = new double[equation.getMlength()];
			double max = 0;
			
			for (int i=0; i < sensitivity.length; i++) {
				sensitivity[i] = Math.sqrt(equation.getAtA().getEntry(i, i));
				
				if (sensitivity[i] > max)
					max = sensitivity[i];
			}
			
			Files.deleteIfExists(outPath);
			Files.createFile(outPath);
			
			for (int i=0; i < sensitivity.length; i++) {
				Files.write(outPath
						, String.format("%.2f %.2f %.2f %.5f\n"
								, parLoc[i].getLongitude()
								, parLoc[i].getLatitude()
								, parLoc[i].getR()
								, sensitivity[i] / max).getBytes()
						, StandardOpenOption.APPEND);
			}
			
		} catch (IOException e) {
			e.getStackTrace();
		}
	}
	
	public static double weightingReciprocalAmplitude(BasicID obs, BasicID syn) {
		double weight = 1.;
		RealVector obsVec = new ArrayRealVector(obs.getData());
		final double obsMax = Math.abs(obsVec.getMaxValue());
		final double obsMin = Math.abs(obsVec.getMinValue());
		weight = 1 / Math.max(obsMin, obsMax);
		return weight;
	}
	
	public static double weightingReciprocalAmplitudeOfTransverse(BasicID obs, BasicID syn) {
		double weight = 1.;
		RealVector obsVec = new ArrayRealVector(obs.getData());
		final double obsMax = Math.abs(obsVec.getMaxValue());
		final double obsMin = Math.abs(obsVec.getMinValue());
		weight = 1 / Math.max(obsMin, obsMax);
		return weight;
	}
	
	private static double weightingEpicentralDistanceDpp(BasicID obs) {
		double weight = 1.;
		double distance = obs.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(obs.getStation().getPosition()) * 180. / Math.PI;		
		histogramDistance = new double[][] { {70, 0.741}, {75, 0.777}, {80, 0.938}, {85, 1.187}, {90, 1.200}, {95, 1.157} };
		
		for (int i = 0; i < histogramDistance.length; i++)
			if (distance >= histogramDistance[i][0] && distance < histogramDistance[i][0] + 5.)
				weight = histogramDistance[i][1];		
		return weight;
	}
	
	private static double weightingAzimuthDpp(BasicID obs) {
		double weight = 1.;
		double azimuth = obs.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(obs.getStation().getPosition()) * 180. / Math.PI;
		
		histogramAzimuth = new double[][] { {310, 1.000}, {315, 0.642}, {320, 0.426}, {325, 0.538}, {330, 0.769}, {335, 0.939}, {340, 1.556}
											, {345, 1.414}, {350, 1.4}, {355, 1.4}, {0, 1.000}, {5, 1.000} };		
		for (double[] p : histogramAzimuth) {
			if (azimuth >= p[0] && azimuth < p[0] + 5.)
				weight = p[1];
		}		
		return weight;
	}
	
	//phase毎のsensitivityを足しあわせ
	public static Map<Phases, Double> sensitivityPerWindowType(PartialID[] ids) {
		Map<Phases, Double> typeSensitivityMap = new HashMap<>();
		for (PartialID id : ids) {
			double s = Sensitivity1D.idToSensitivity(id);
			Phases phases = new Phases(id.getPhases());
			if (typeSensitivityMap.containsKey(phases)) {
				s += typeSensitivityMap.get(phases);
				typeSensitivityMap.replace(phases, s);
			}
			else
				typeSensitivityMap.put(phases, s);
		}
		return typeSensitivityMap;
	}
	
	public static Map<UnknownParameter, Double> sensitivityMap(RealMatrix ata, List<UnknownParameter> parameterList) {
		Map<UnknownParameter, Double> sensitivity = new HashMap<>();
		if (ata == null)
			throw new RuntimeException("ata is undefined");
		if (parameterList == null)
			throw new RuntimeException("unknownParameterList is undefined");
		if(ata.getColumnDimension() != parameterList.size())
			throw new RuntimeException("Expect ata and unknownParameterList to have the same length " 
					+ ata.getColumnDimension() + " " + parameterList.size());
		for (int i = 0; i < parameterList.size(); i++) 
			sensitivity.put(parameterList.get(i), Math.sqrt(ata.getEntry(i, i)) * parameterList.get(i).getWeighting());
		return sensitivity;
	}
}