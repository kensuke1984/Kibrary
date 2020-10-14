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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class LambdaMuToMuGivenR {
	
	public static void main(String[] args) throws IOException {
		Path partialIDPath = Paths.get(args[0]);
		Path partialPath = Paths.get(args[1]);
		
		PolynomialStructure structure = PolynomialStructure.PREM;
		System.out.println("Using structure " + structure);
		
		Map<Double, Double> rKRDH16 = new HashMap<>();
		rKRDH16.put(2541., 2.6);
		rKRDH16.put(2641., 2.6);
		rKRDH16.put(2741., 2.1);
		rKRDH16.put(2841., 1.4);
		
		Map<Double, Double> rMLBD00 = new HashMap<>();
		rMLBD00.put(2541., 2.1);
		rMLBD00.put(2641., 2.3);
		rMLBD00.put(2741., 2.4);
		rMLBD00.put(2841., 2.6);
		
		Map<Double, Double> rKK01ah = new HashMap<>();
		rKK01ah.put(2541., 2.1);
		rKK01ah.put(2641., 2.1);
		rKK01ah.put(2741., 2.2);
		rKK01ah.put(2841., 2.3);
		
		Map<Double, Double> rKK01ahae = new HashMap<>();
		rKK01ahae.put(2541., 2.5);
		rKK01ahae.put(2641., 2.5);
		rKK01ahae.put(2741., 2.6);
		rKK01ahae.put(2841., 2.7);
		
		PartialID[] partials = PartialIDFile.read(partialIDPath, partialPath);
		
		List<PartialID> partialsLambda = Stream.of(partials).filter(p -> p.getPartialType().equals(PartialType.LAMBDA)).collect(Collectors.toList());
		List<PartialID> partialsMU = Stream.of(partials).filter(p -> p.getPartialType().equals(PartialType.MU)).collect(Collectors.toList());
		
		System.out.println(partialsMU.size());
		
		final Location[] locations = partialsMU.stream().map(p -> p.getPerturbationLocation()).distinct().collect(Collectors.toList()).toArray(new Location[0]);
		final PartialID[] partialsOrder = partialsMU.stream().parallel().filter(p -> p.getPerturbationLocation().equals(locations[0])).collect(Collectors.toList()).toArray(new PartialID[0]);
		
		int[] indexOrderedMU = new int[partialsMU.size()];
		int[] indexOrderedLambda = new int[partialsMU.size()];
		
		IntStream.range(0, partialsMU.size()).parallel().forEach(i -> {
			int index = whichTimewindow(partialsMU.get(i), partialsOrder) * locations.length + whichUnknown(partialsMU.get(i), locations);
			indexOrderedMU[index] = i;
		});
		IntStream.range(0, partialsLambda.size()).parallel().forEach(i -> {
			int index = whichTimewindow(partialsLambda.get(i), partialsOrder) * locations.length + whichUnknown(partialsLambda.get(i), locations);
			indexOrderedLambda[index] = i; 
		});

		
		Path outID1 = Paths.get("partialID_KRDH16.dat");
		Path out1 = Paths.get("partial_KRDH16.dat");
		
		Path outID2 = Paths.get("partialID_MLBD00.dat");
		Path out2 = Paths.get("partial_MLBD00.dat");
		
		Path outID3 = Paths.get("partialID_K01ah.dat");
		Path out3 = Paths.get("partial_K01ah.dat");
		
		Path outID4 = Paths.get("partialID_KK01ahae.dat");
		Path out4 = Paths.get("partial_KK01ahae.dat");
		
		Set<Station> stationSet = partialsMU.stream().map(p -> p.getStation()).collect(Collectors.toSet());
		Set<GlobalCMTID> globalCMTIDSet = partialsMU.stream().map(p -> p.getGlobalCMTID()).collect(Collectors.toSet());
		
		Set<Phase> phaseSet = new HashSet<>();
		Stream.of(partialsOrder).map(p -> new Phases(p.getPhases())).collect(Collectors.toSet()).forEach(p -> p.toSet().forEach(pp -> phaseSet.add(pp)));
		Phase[] phases = phaseSet.stream().collect(Collectors.toList()).toArray(new Phase[0]);
		
		List<double[]> periodRangeList = Stream.of(partialsOrder).map(p -> new FrequencyRange(1./p.getMaxPeriod(), 1./p.getMinPeriod())).distinct().map(f -> f.toPeriodRange()).collect(Collectors.toList());
		double[][] periodRanges = new double[periodRangeList.size()][];
		for (int i = 0; i < periodRangeList.size(); i++) {
			periodRanges[i] = periodRangeList.get(i);
		}
		
		Set<Location> locationSet = Stream.of(locations).collect(Collectors.toSet());
		
		WaveformDataWriter writer1 = new WaveformDataWriter(outID1, out1, stationSet, globalCMTIDSet, periodRanges, phases, locationSet);
		WaveformDataWriter writer2 = new WaveformDataWriter(outID2, out2, stationSet, globalCMTIDSet, periodRanges, phases, locationSet);
		WaveformDataWriter writer3 = new WaveformDataWriter(outID3, out3, stationSet, globalCMTIDSet, periodRanges, phases, locationSet);
		WaveformDataWriter writer4 = new WaveformDataWriter(outID4, out4, stationSet, globalCMTIDSet, periodRanges, phases, locationSet);
		
		
		for (int i = 0; i < partialsMU.size(); i++) {
			PartialID partialMU = partialsMU.get(indexOrderedMU[i]);
			PartialID partialLambda = partialsLambda.get(indexOrderedLambda[i]);
			if (!(partialLambda.getGlobalCMTID().equals(partialMU.getGlobalCMTID())
					&& partialLambda.getStation().equals(partialMU.getStation())
					&& partialLambda.getPerturbationLocation().equals(partialMU.getPerturbationLocation())
					&& partialLambda.getSacComponent().equals(partialMU.getSacComponent())
					&& new Phases(partialLambda.getPhases()).equals(new Phases(partialMU.getPhases())))) {
				System.out.println(partialMU + " ::: " + partialLambda);
				throw new RuntimeException("Partials order differ");
			}
			
			double r = partialMU.getPerturbationLocation().getR();
			double depth = 6371. - r;
			double Rsp1 = rKRDH16.get(depth);
			double Rsp2 = rMLBD00.get(depth);
			double Rsp3 = rKK01ah.get(depth);
			double Rsp4 = rKK01ahae.get(depth);
			
			double vp = structure.getVphAt(r);
			double vs = structure.getVshAt(r);
			
			if (vs == 0)
				throw new RuntimeException("Unexpected zero Vs");
			if (Rsp1 == 0)
				throw new RuntimeException("Unexpected zero Rsp1");
			
			double[] muData = partialMU.getData();
			double[] lambdaData = partialLambda.getData();
			double[] muPrimeData1 = new double[muData.length];
			double[] muPrimeData2 = new double[muData.length];
			double[] muPrimeData3 = new double[muData.length];
			double[] muPrimeData4 = new double[muData.length];
			for (int j = 0; j < muData.length; j++) {
				muPrimeData1[j] = (muData[j] - 2 * lambdaData[j]) + 1./Rsp1*vp*vp/vs/vs * lambdaData[j];
				muPrimeData2[j] = (muData[j] - 2 * lambdaData[j]) + 1./Rsp2*vp*vp/vs/vs * lambdaData[j];
				muPrimeData3[j] = (muData[j] - 2 * lambdaData[j]) + 1./Rsp3*vp*vp/vs/vs * lambdaData[j];
				muPrimeData4[j] = (muData[j] - 2 * lambdaData[j]) + 1./Rsp4*vp*vp/vs/vs * lambdaData[j];
			}
			
			PartialID parMuPrime1 = new PartialID(partialLambda.getStation(), partialLambda.getGlobalCMTID(), partialLambda.getSacComponent(), partialLambda.getSamplingHz(),
					partialLambda.getStartTime(), partialLambda.getNpts(), partialLambda.getMinPeriod(), partialLambda.getMaxPeriod(),
					partialLambda.getPhases(), partialLambda.getStartByte(), partialLambda.isConvolute(), partialLambda.getPerturbationLocation()
					, PartialType.MU, muPrimeData1);
			PartialID parMuPrime2 = new PartialID(partialLambda.getStation(), partialLambda.getGlobalCMTID(), partialLambda.getSacComponent(), partialLambda.getSamplingHz(),
					partialLambda.getStartTime(), partialLambda.getNpts(), partialLambda.getMinPeriod(), partialLambda.getMaxPeriod(),
					partialLambda.getPhases(), partialLambda.getStartByte(), partialLambda.isConvolute(), partialLambda.getPerturbationLocation()
					, PartialType.MU, muPrimeData2);
			PartialID parMuPrime3 = new PartialID(partialLambda.getStation(), partialLambda.getGlobalCMTID(), partialLambda.getSacComponent(), partialLambda.getSamplingHz(),
					partialLambda.getStartTime(), partialLambda.getNpts(), partialLambda.getMinPeriod(), partialLambda.getMaxPeriod(),
					partialLambda.getPhases(), partialLambda.getStartByte(), partialLambda.isConvolute(), partialLambda.getPerturbationLocation()
					, PartialType.MU, muPrimeData3);
			PartialID parMuPrime4 = new PartialID(partialLambda.getStation(), partialLambda.getGlobalCMTID(), partialLambda.getSacComponent(), partialLambda.getSamplingHz(),
					partialLambda.getStartTime(), partialLambda.getNpts(), partialLambda.getMinPeriod(), partialLambda.getMaxPeriod(),
					partialLambda.getPhases(), partialLambda.getStartByte(), partialLambda.isConvolute(), partialLambda.getPerturbationLocation()
					, PartialType.MU, muPrimeData4);
			
			writer1.addPartialID(parMuPrime1);
			writer2.addPartialID(parMuPrime2);
			writer3.addPartialID(parMuPrime3);
			writer4.addPartialID(parMuPrime4);
		}
		
		writer1.close();
		writer2.close();
		writer3.close();
		writer4.close();
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
