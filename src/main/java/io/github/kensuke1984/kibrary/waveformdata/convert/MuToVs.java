package io.github.kensuke1984.kibrary.waveformdata.convert;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.FrequencyRange;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
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

public class MuToVs {
	
	public static void main(String[] args) throws IOException {
		Path partialIDPath = Paths.get(args[0]);
		Path partialPath = Paths.get(args[1]);
		
		PartialID[] partials = PartialIDFile.read(partialIDPath, partialPath);
		
		List<PartialID> partialsMU = Stream.of(partials).filter(p -> p.getPartialType().equals(PartialType.MU)).collect(Collectors.toList());
		
		System.out.println(partialsMU.size());
		
		PolynomialStructure structure = PolynomialStructure.PREM;
		
		List<PartialID> partialsVs = partialsMU.stream().map(p -> {
			double r = p.getPerturbationLocation().getR();
			double[] vsData = new ArrayRealVector(p.getData()).mapMultiply(2 * structure.getRhoAt(r) * structure.getVshAt(r)).toArray();
			
			PartialID parVs = new PartialID(p.getStation(), p.getGlobalCMTID(), p.getSacComponent(), p.getSamplingHz(),
					p.getStartTime(), p.getNpts(), p.getMinPeriod(), p.getMaxPeriod(),
					p.getPhases(), p.getStartByte(), p.isConvolute(), p.getPerturbationLocation()
					, PartialType.Vs, vsData);
			return parVs;
		}).collect(Collectors.toList());
		
		
		String tmpString = Utilities.getTemporaryString();
		Path outID = Paths.get("partialID" + tmpString +".dat");
		Path out = Paths.get("partial" + tmpString + ".dat");
		
		Set<Station> stationSet = partialsMU.stream().map(p -> p.getStation()).collect(Collectors.toSet());
		Set<GlobalCMTID> globalCMTIDSet = partialsMU.stream().map(p -> p.getGlobalCMTID()).collect(Collectors.toSet());
		
		Set<Phase> phaseSet = new HashSet<>();
		partialsMU.stream().map(p -> new Phases(p.getPhases())).collect(Collectors.toSet()).forEach(p -> p.toSet().forEach(pp -> phaseSet.add(pp)));
		Phase[] phases = phaseSet.stream().collect(Collectors.toList()).toArray(new Phase[0]);
		
		List<double[]> periodRangeList = partialsMU.stream().map(p -> new FrequencyRange(1./p.getMaxPeriod(), 1./p.getMinPeriod())).distinct().map(f -> f.toPeriodRange()).collect(Collectors.toList());
		double[][] periodRanges = new double[periodRangeList.size()][];
		for (int i = 0; i < periodRangeList.size(); i++) {
			periodRanges[i] = periodRangeList.get(i);
		}
		
		Set<Location> locationSet = partialsMU.stream().map(p -> p.getPerturbationLocation()).collect(Collectors.toSet());
		
		WaveformDataWriter writer = new WaveformDataWriter(outID, out, stationSet, globalCMTIDSet, periodRanges, phases, locationSet);
		
		for (PartialID partial : partialsVs)
			writer.addPartialID(partial);
		
		writer.close();
	}
	
	private static int whichTimewindow(PartialID partial, PartialID[] partialsOrder) {
		for (int i = 0; i < partialsOrder.length; i++) {
			PartialID par = partialsOrder[i];
			if (partial.getGlobalCMTID().equals(par.getGlobalCMTID())
					&& partial.getStation().equals(par.getStation())
					&& partial.getSacComponent().equals(par.getSacComponent())
					&& new Phases(partial.getPhases()).equals(new Phases(par.getPhases()))
					&& Math.abs(partial.getStartTime() - par.getStartTime()) < 1.01) {
				return i;
			}
		}
		return -1;
	}
	
	private static int whichUnknown(PartialID partial, Location[] locations) {
		for (int i = 0; i < locations.length; i++) {
			if (partial.getPerturbationLocation().equals(locations[i])) {
				return i;
			}
		}
		return -1;
	}
	
}
