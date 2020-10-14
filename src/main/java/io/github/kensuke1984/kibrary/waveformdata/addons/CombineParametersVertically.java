package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.inversion.addons.HorizontalParameterMapping;
import io.github.kensuke1984.kibrary.inversion.addons.ParameterMapping;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;
import io.github.kensuke1984.kibrary.waveformdata.WaveformDataWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;


public class CombineParametersVertically {

	public static void main(String[] args) {
		Path partialIDPath = Paths.get(args[0]);
		Path partialPath = Paths.get(args[1]);
		Path unknownPath = Paths.get(args[2]);
		Path verticalMappingPath = Paths.get(args[3]);
		
		String tmpString = Utilities.getTemporaryString();
		Path outIDPath = Paths.get("partialID" + tmpString + ".dat");
		Path outPath = Paths.get("partial" + tmpString + ".dat");
		
		try {
			PartialID[] partials = PartialIDFile.read(partialIDPath, partialPath);
			UnknownParameter[] originalUnknowns = UnknownParameterFile.read(unknownPath).toArray(new UnknownParameter[0]);
			
			ParameterMapping mapping = new ParameterMapping(originalUnknowns, verticalMappingPath);
			
			UnknownParameter[] newUnknowns = mapping.getUnknowns();
			
			int nOriginal = mapping.getNoriginal();
			int nNew = mapping.getNnew();
			
			Set<Station> stationSet = Stream.of(partials).parallel().map(par -> par.getStation()).collect(Collectors.toSet());
			Set<GlobalCMTID> globalCMTIDSet = Stream.of(partials).parallel().map(par -> par.getGlobalCMTID()).collect(Collectors.toSet());
			
			double[][] periodRanges = new double[][] {{partials[0].getMinPeriod(), partials[0].getMaxPeriod()}};
			Set<Location> perturbationPoints = Stream.of(newUnknowns).parallel().map(u -> u.getLocation()).collect(Collectors.toSet());
			
//			Phase[] phases = new Phase[] {Phase.ScS, Phase.S};
//			Phase[] phases = new Phase[] {Phase.PcP, Phase.P};
			
			Set<Phase> phaseSet = new HashSet<>();
			Stream.of(partials).parallel().map(par -> new Phases(par.getPhases())).distinct().forEach(ps -> {
				for (Phase p : ps.toSet())
					phaseSet.add(p);
			});
			Phase[] phases = phaseSet.toArray(new Phase[phaseSet.size()]);
			System.out.print("Found phases ");
			for (Phase p : phases)
				System.out.print(p + " ");
			System.out.println();
			
			WaveformDataWriter writer = new WaveformDataWriter(outIDPath, outPath, stationSet, globalCMTIDSet, periodRanges, phases, perturbationPoints);
			
			globalCMTIDSet.stream().forEach(event -> {
				List<PartialID> eventPartials = Stream.of(partials).filter(par -> par.getGlobalCMTID().equals(event)).collect(Collectors.toList());
				stationSet.stream().forEach(station -> {
					List<PartialID> stationPartials = eventPartials.stream().filter(par -> par.getStation().equals(station)).collect(Collectors.toList());
					if (stationPartials.size() > 0) {
						IntStream.range(0, nNew).parallel().forEach(inew -> {
							int[] iOriginals = mapping.getiNewToOriginal(inew);
							PartialID refID = stationPartials.get(0);
							RealVector dataVector = new ArrayRealVector(refID.getNpts());
							
							for (int iOriginal : iOriginals) {
								UnknownParameter unknown = originalUnknowns[iOriginal];
								
//								double weight = unknown.getWeighting();
								
								//weight in order to keep consistent definition of volume of unknown parameters
//								System.out.println(iOriginals.length);
								double weight = 1. / iOriginals.length;
								
	//							System.out.println("------\n" + unknown.getLocation());
	//							stationPartials.stream().forEach(par -> System.out.println(par.getPerturbationLocation()));
								
								PartialID tmpID = stationPartials.stream().parallel().filter(par -> par.getPerturbationLocation().equals(unknown.getLocation())
										&& par.getPartialType().equals(unknown.getPartialType())).findFirst().get();
								dataVector = dataVector.add(new ArrayRealVector(tmpID.getData()).mapMultiply(weight));
								
//								System.out.println(unknown.getWeighting() + " "  + dataVector.getNorm());
							}
							
							PartialID tmpPartial = new PartialID(refID.getStation(), refID.getGlobalCMTID(), refID.getSacComponent(), refID.getSamplingHz()
									, refID.getStartTime(), refID.getNpts(), refID.getMinPeriod(), refID.getMaxPeriod(), refID.getPhases(), refID.getStartByte()
									, refID.isConvolute(), newUnknowns[inew].getLocation(), newUnknowns[inew].getPartialType(), dataVector.toArray());
							try {
								writer.addPartialID(tmpPartial);
							} catch (IOException e) {
								e.printStackTrace();
							}
						});
					}
				});
			});
				
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

}
