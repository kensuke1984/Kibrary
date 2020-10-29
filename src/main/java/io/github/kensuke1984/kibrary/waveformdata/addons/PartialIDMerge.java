package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;
import io.github.kensuke1984.kibrary.waveformdata.WaveformDataWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.Set;
import java.util.HashSet;

class PartialIDMerge {

	public static void main(String[] args) throws IOException {
		Path workingDir = Paths.get(".");
//		Path root0 = Paths.get(args[0]);
//		Path root1 = Paths.get(args[1]);
//		Path src0ID = root0.resolve("partialID.dat");
//		Path src1ID = root1.resolve("partialID.dat");
//		Path src0Data = root0.resolve("partial.dat");
//		Path src1Data = root1.resolve("partial.dat");
		
		Path src0ID = Paths.get(args[0]);
		Path src0Data = Paths.get(args[1]);
		Path src1ID = Paths.get(args[2]);
		Path src1Data = Paths.get(args[3]);
		
		PartialID[] src0 = PartialIDFile.read(src0ID, src0Data);
		PartialID[] src1 = PartialIDFile.read(src1ID, src1Data);
		String tmpstr = Utilities.getTemporaryString();
		Path idPath = workingDir.resolve("partialID" + tmpstr + ".dat");
		Path dataPath = workingDir.resolve("partial" + tmpstr + ".dat");
		
		Set<Location> perturbationPoints = new HashSet<>();
		Set<Station> stationSet = new HashSet<>();
		Set<GlobalCMTID> globalCMTIDSet = new HashSet<>();
		Set<double[]> periodSet = new HashSet<>();
		Set<Phase> phaseSet = new HashSet<>();
		
		Stream.concat(Stream.of(src0), Stream.of(src1)).forEach(id -> {
			globalCMTIDSet.add(id.getGlobalCMTID());
			stationSet.add(id.getStation());
			perturbationPoints.add(id.getPerturbationLocation());
			boolean add = true;
			for (double[] periods : periodSet) {
				if (id.getMinPeriod() == periods[0] && id.getMaxPeriod() == periods[1])
					add = false;
			}
			if (add)
				periodSet.add(new double[] {id.getMinPeriod(), id.getMaxPeriod()});
			for (Phase phase : id.getPhases())
				phaseSet.add(phase);
		});
		
		double[][] periodRanges = new double[periodSet.size()][];
		int j = 0;
		for (double[] periods : periodSet)
			periodRanges[j++] = periods;
		Phase[] phases = phaseSet.toArray(new Phase[phaseSet.size()]);
		
		try (WaveformDataWriter wdw = new WaveformDataWriter(idPath, dataPath, stationSet, globalCMTIDSet, periodRanges, phases, perturbationPoints);
				Stream<PartialID> idStream = Stream.concat(Stream.of(src0), Stream.of(src1))) {
			Set<PartialID> partials = new HashSet<>();
			idStream.forEach(id -> {
				partials.add(id);
			});
			partials.forEach(partial -> {
				try {
					if (! partial.getWaveformType().equals(WaveformType.PARTIAL)) {
						System.out.println(partial.toString() + "is not a partial, it is a " + partial.getWaveformType().toString());
						throw new RuntimeException();
					}
					wdw.addPartialID(partial);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
