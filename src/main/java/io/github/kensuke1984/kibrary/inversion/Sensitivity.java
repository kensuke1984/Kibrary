package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;
import io.github.kensuke1984.kibrary.inversion.ObservationEquation;
import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.selection.DataSelection;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
//import io.github.kensuke1984.kibrary.VelocityField;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.RuntimeErrorException;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class Sensitivity {
	
	public static void main(String[] args) throws IOException {
		if (args.length != 8) {
			System.err.println("usage: path to \"HOME\" directory, relative path from \"HOME\" directory to outputfile,"
					+ "relative path from \"HOME\" to ");
			throw new IllegalArgumentException();
		}	
		double minDistance = 65;
		double maxDistance = 105;
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
							return weightingReciprocalAmplitude(obs, syn);
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