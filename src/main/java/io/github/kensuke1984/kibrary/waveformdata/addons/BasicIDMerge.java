package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.WaveformDataWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

class BasicIDMerge {

	public static void main(String[] args) throws IOException, IllegalArgumentException {
		Path workingDir = Paths.get(".");
		Path src0ID = Paths.get(args[0]);
		Path src0Data = Paths.get(args[1]);
		Path src1ID = Paths.get(args[2]);
		Path src1Data = Paths.get(args[3]);
		BasicID[] src0 = BasicIDFile.read(src0ID, src0Data);
		BasicID[] src1 = BasicIDFile.read(src1ID, src1Data);
		String tmpstr = Utilities.getTemporaryString();
		Path idPath = workingDir.resolve("waveformID" + tmpstr + ".dat");
		Path dataPath = workingDir.resolve("waveform" + tmpstr + ".dat");
		
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
