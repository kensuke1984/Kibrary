package io.github.kensuke1984.kibrary.waveformdata.convert;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.FrequencyRange;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;
import io.github.kensuke1984.kibrary.waveformdata.WaveformDataWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LambdaMuToLambda2MuMu_prime {
	
	public static void main(String[] args) throws IOException {
		Path partialIDPath = Paths.get(args[0]);
		Path partialPath = Paths.get(args[1]);
		
		PartialID[] partials = PartialIDFile.read(partialIDPath, partialPath);
		
		List<PartialID> partialsLambda = Stream.of(partials).filter(p -> p.getPartialType().equals(PartialType.LAMBDA)).collect(Collectors.toList());
		List<PartialID> partialsMU = Stream.of(partials).filter(p -> p.getPartialType().equals(PartialType.MU)).collect(Collectors.toList());
		
		System.out.println(partialsMU.size());
		
		List<PartialID> partialsMUPrime = new ArrayList<>();
		List<PartialID> partialsLambda2mu = new ArrayList<>();
		
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
		
//		for (PartialID partialMU : partialsMU) {
//			for (PartialID par : tmpPartialsLambda) {
//				if (par.getGlobalCMTID().equals(partialMU.getGlobalCMTID())
//					&& par.getStation().equals(partialMU.getStation())
//					&& par.getPerturbationLocation().equals(partialMU.getPerturbationLocation())
//					&& par.getSacComponent().equals(partialMU.getSacComponent())
//					&& new Phases(par.getPhases()).equals(new Phases(partialMU.getPhases()))) {
//						partialsLambda.add(par);
//						
//						PartialID parKappa = partialMU;
//						double[] muData = partialMU.getData();
//						double[] lambdaData = par.getData();
//						double[] kappaData = new double[muData.length];
//						for (int i = 0; i < muData.length; i++)
//							kappaData[i] = lambdaData[i] + 2. * muData[i];
//						
//						parKappa = parKappa.setData(kappaData);
//						partialsLambda2MU.add(parKappa);
//						
//						break;
//				}
//			}
//		}
		
		// fast version
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
			
			SACComponent component = partialLambda.getSacComponent();
			
			double[] muPrimeData = partialMU.getData();
			double[] lambdaData = partialLambda.getData();
			if (component.equals(SACComponent.Z)) {
				for (int j = 0; j < lambdaData.length; j++)
					lambdaData[j] = lambdaData[j] + .5 * muPrimeData[j];
				Arrays.fill(muPrimeData, 0.);
			}
			else if (component.equals(SACComponent.T)) {
				Arrays.fill(lambdaData, 0.);
			}
			
			PartialID parMuPrime = new PartialID(partialLambda.getStation(), partialLambda.getGlobalCMTID(), partialLambda.getSacComponent(), partialLambda.getSamplingHz(),
					partialLambda.getStartTime(), partialLambda.getNpts(), partialLambda.getMinPeriod(), partialLambda.getMaxPeriod(),
					partialLambda.getPhases(), partialLambda.getStartByte(), partialLambda.isConvolute(), partialLambda.getPerturbationLocation()
					, PartialType.MU, muPrimeData);
			
			PartialID parLambda2mu = new PartialID(partialLambda.getStation(), partialLambda.getGlobalCMTID(), partialLambda.getSacComponent(), partialLambda.getSamplingHz(),
					partialLambda.getStartTime(), partialLambda.getNpts(), partialLambda.getMinPeriod(), partialLambda.getMaxPeriod(),
					partialLambda.getPhases(), partialLambda.getStartByte(), partialLambda.isConvolute(), partialLambda.getPerturbationLocation()
					, PartialType.LAMBDA2MU, lambdaData);
			
			partialsMUPrime.add(parMuPrime);
			partialsLambda2mu.add(parLambda2mu);
		}
		
		String tmpString = Utilities.getTemporaryString();
		Path outID = Paths.get("partialID" + tmpString +".dat");
		Path out = Paths.get("partial" + tmpString + ".dat");
		
		Set<Station> stationSet = partialsMU.stream().map(p -> p.getStation()).collect(Collectors.toSet());
		Set<GlobalCMTID> globalCMTIDSet = partialsMU.stream().map(p -> p.getGlobalCMTID()).collect(Collectors.toSet());
		
//		double[][] periodRanges = new double[][] {{1./0.136, 100.}};
//		Phase[] phases = new Phase[] {Phase.P, Phase.PcP};
		
//		double[][] periodRanges = new double[][] {{1./0.08, 100.}};
//		Phase[] phases = new Phase[] {Phase.S, Phase.ScS};
		
		Set<Phase> phaseSet = new HashSet<>();
		Stream.of(partialsOrder).map(p -> new Phases(p.getPhases())).collect(Collectors.toSet()).forEach(p -> p.toSet().forEach(pp -> phaseSet.add(pp)));
		Phase[] phases = phaseSet.stream().collect(Collectors.toList()).toArray(new Phase[0]);
		
		List<double[]> periodRangeList = Stream.of(partialsOrder).map(p -> new FrequencyRange(1./p.getMaxPeriod(), 1./p.getMinPeriod())).distinct().map(f -> f.toPeriodRange()).collect(Collectors.toList());
		double[][] periodRanges = new double[periodRangeList.size()][];
		for (int i = 0; i < periodRangeList.size(); i++) {
			periodRanges[i] = periodRangeList.get(i);
		}
		
		Set<Location> locationSet = Stream.of(locations).collect(Collectors.toSet());
		
		WaveformDataWriter writer = new WaveformDataWriter(outID, out, stationSet, globalCMTIDSet, periodRanges, phases, locationSet);
		
		for (PartialID partial : partialsMUPrime)
			writer.addPartialID(partial);
		
		for (PartialID partial : partialsLambda2mu)
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
