package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.WaveformDataWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DuplicateDataset {

	public static void main(String[] args) {
		Path idPath0 = Paths.get(args[0]);
		Path dataPath0 = Paths.get(args[1]);
		Path idPath1 = Paths.get(args[2]);
		Path dataPath1 = Paths.get(args[3]);
		String temporaryString = Utilities.getTemporaryString(); 
		Path idOutPath = Paths.get("waveformID" + temporaryString + ".dat");
		Path dataOutPath = Paths.get("waveform" + temporaryString + ".dat");
		
		try {
			BasicID[] ids0 = BasicIDFile.read(idPath0, dataPath0);
			BasicID[] ids1 = BasicIDFile.read(idPath1, dataPath1);
			Set<Station> stationSet = new HashSet<>();
			Set<GlobalCMTID> globalCMTIDSet = new HashSet<>();
			Set<Phase> phaseSet = new HashSet<>();
			
			Stream.of(ids1).forEach(id -> {
				stationSet.add(id.getStation());
				globalCMTIDSet.add(id.getGlobalCMTID());
				for (Phase p : id.getPhases()) {
					phaseSet.add(p);
				}
			});
			
			Phase[] phases = phaseSet.toArray(new Phase[0]);
			double[][] periodRanges = new double[1][];
			periodRanges[0] = new double[] {8., 200.};
			
			List<BasicID> outList = new ArrayList<>();
			Stream.of(ids0).forEach(id0 -> {
				List<BasicID> ids = Stream.of(ids1).filter(id1 -> id1.getStation().equals(id0.getStation())
						&& id1.getGlobalCMTID().equals(id0.getGlobalCMTID())
						&& id1.getSacComponent().equals(id0.getSacComponent())
						&& id1.getMinPeriod() == id0.getMinPeriod()
						&& id1.getMaxPeriod() == id0.getMaxPeriod()
						&& id1.getWaveformType().equals(id0.getWaveformType())
						&& Math.abs(id1.getStartTime() - id0.getStartTime()) < 20.)
					.collect(Collectors.toList());
				if (ids.size() == 1)
					outList.add(ids.get(0));
			});
			
			WaveformDataWriter writer = new WaveformDataWriter(idOutPath, dataOutPath, stationSet, globalCMTIDSet, periodRanges, phases);
			System.out.println("Outputting " + outList.size() + " BasicID");
			for (BasicID id : outList)
				writer.addBasicID(id);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
