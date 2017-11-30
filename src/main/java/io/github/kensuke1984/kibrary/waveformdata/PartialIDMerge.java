/**
 * 
 */
package io.github.kensuke1984.kibrary.waveformdata;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.Set;
import java.util.HashSet;


import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.waveformdata.WaveformDataWriter;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

/**
 * @version 0.0.1
 * @since 2017/05/11
 * @author Yuki
 *
 */

class PartialIDMerge {
	
	static Set<Location> ppLoc = new HashSet<>();

	public static void main(String[] args) throws IOException {
		if (args.length < 7)
			System.err.println("usage - src0ID, src1ID, src0data, src1data, outID, outData, ppfile");
		Path src0ID = Paths.get(args[0]);
		Path src1ID = Paths.get(args[1]);
		Path src0Data = Paths.get(args[2]);
		Path src1Data = Paths.get(args[3]);
		Path idPath = Paths.get(args[4]);
		Path dataPath = Paths.get(args[5]);
		Path ppfile = Paths.get(args[6]);
//		Path twPath = Paths.get(args[7]);
		
		PartialID[] src0 = PartialIDFile.readPartialIDandDataFile(src0ID, src0Data);
		PartialID[] src1 = PartialIDFile.readPartialIDandDataFile(src1ID, src1Data);
		readPartialLoc(ppfile);
		
		
		Set<Station> stationSet = new HashSet<>();
		Set<GlobalCMTID> globalCMTIDSet = new HashSet<>();
		
		double minPeriod = 0;
		double maxPeriod = 0;
		
		if (src0[0].getMinPeriod() != src1[0].getMinPeriod() 
				|| src0[0].getMaxPeriod() != src1[0].getMaxPeriod()) 
			throw new IllegalArgumentException("Error: The two dataset don't have the same frequency range.");
		else {
			minPeriod = src0[0].getMinPeriod();
			maxPeriod = src0[0].getMaxPeriod();
		}
		
		Stream.concat(Stream.of(src0), Stream.of(src1)).forEach(id -> {
			globalCMTIDSet.add(id.getGlobalCMTID());
			stationSet.add(id.getStation());
		});
		
		double[][] periodRanges = new double[][] {{minPeriod, maxPeriod}};
		
		Phase[] phases = Stream.of(src0).map(PartialID::getPhases).flatMap(p -> Stream.of(p)).distinct().toArray(Phase[]::new);
		
		try (WaveformDataWriter wdw = new WaveformDataWriter(idPath, dataPath, stationSet, globalCMTIDSet, periodRanges, phases, ppLoc);
			Stream<PartialID> idStream = Stream.concat(Stream.of(src0), Stream.of(src1))) {
			Set<PartialID> partials = new HashSet<>();
			idStream.forEach(id -> {
				partials.add(id);
			});
			System.out.println("adding the partial data!");
			partials.forEach(partial -> {
				try {
//					System.out.println("adding the partial data!");
					wdw.addPartialID(partial);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}	//main

	private static Location toLocation(String line) {
		String[] parts = line.split("\\s+");
		return new Location(Double.parseDouble(parts[0]),
				Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
	}
	
	private static Location[] readPartialLocation(Path parPath) {
		try {
			return Files.readAllLines(parPath).stream()
					.map(PartialIDMerge::toLocation)
					.toArray(n -> new Location[n]);
		} catch (Exception e) {
			throw new RuntimeException("par file has problems");
		}
	}
		private static void readPartialLoc(Path parPath) {
			try {
				Files.readAllLines(parPath).stream()
						.map(PartialIDMerge::toLocation)
						.forEach(n -> ppLoc.add(n));
//						.toArray(n -> new Location[n]);
			} catch (Exception e) {
				throw new RuntimeException("par file has problems");
			}
	}
}
