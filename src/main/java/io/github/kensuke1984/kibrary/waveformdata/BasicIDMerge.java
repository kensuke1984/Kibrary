package io.github.kensuke1984.kibrary.waveformdata;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

class BasicIDMerge {

	public static void main(String[] args) throws IOException, IllegalArgumentException {
		Path workingDir = Paths.get(".");
		Path root0 = Paths.get(args[0]);
		Path root1 = Paths.get(args[1]);
		Path src0ID = root0.resolve("waveformID.dat");
		Path src1ID = root1.resolve("waveformID.dat");
		Path src0Data = root0.resolve("waveform.dat");
		Path src1Data = root1.resolve("waveform.dat");
		BasicID[] src0 = BasicIDFile.readBasicIDandDataFile(src0ID, src0Data);
		BasicID[] src1 = BasicIDFile.readBasicIDandDataFile(src1ID, src1Data);
		Path idPath = workingDir.resolve("waveformID_merged.dat");
		Path dataPath = workingDir.resolve("waveform_merged.dat");
		
		Set<Station> stationSet = new HashSet<>();
		Set<GlobalCMTID> globalCMTIDSet = new HashSet<>();
		Set<double[]> periodSet = new HashSet<>();
		Set<Phase> phaseSet = new HashSet<>();
		
		Stream.concat(Stream.of(src0), Stream.of(src1)).forEach(id -> {
			globalCMTIDSet.add(id.getGlobalCMTID());
			stationSet.add(id.getStation());
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
		
		try (WaveformDataWriter wdw = new WaveformDataWriter(idPath, dataPath, stationSet, globalCMTIDSet, periodRanges, phases);
				Stream<BasicID> idStream = Stream.concat(Stream.of(src0), Stream.of(src1));) {
			idStream.distinct().forEach(id -> {
				try {
					wdw.addBasicID(id);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
