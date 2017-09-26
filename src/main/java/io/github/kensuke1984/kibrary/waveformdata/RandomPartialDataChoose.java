/**
 * 
 */
package io.github.kensuke1984.kibrary.waveformdata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.Location;

/**
 * @version 0.0.1
 * @since 2017/08/09
 * @author Yuki
 *
 */
public class RandomPartialDataChoose {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		if (args.length != 5)
			System.err.println("Usage: source PartialID path, source  PartialData path, output PartialID path, output PartialData path, globalCMTIDs");
		Path srcID = Paths.get(args[0]);
		Path srcData = Paths.get(args[1]);
		Path idPath = Paths.get(args[2]);
		Path dataPath = Paths.get(args[3]);
		Path gidPath = Paths.get(args[4]);
//		final int N_THREADS = Runtime.getRuntime().availableProcessors();
//		ExecutorService execs = Executors.newFixedThreadPool(N_THREADS);
		PartialID[] src = PartialIDFile.readPartialIDandDataFile(srcID, srcData);
		
		Set<Location> ppLoc = new HashSet<>();
		Set<Station> stationSet = new HashSet<>();
		Set<GlobalCMTID> globalCMTIDSet = new HashSet<>();
		
		List<String> lines = Files.readAllLines(gidPath, StandardCharsets.UTF_8);
		Set<GlobalCMTID> globalCMTIDSetRef = new HashSet<>();
		lines.stream().forEachOrdered(idstring -> {
			globalCMTIDSetRef.add(new GlobalCMTID(idstring));
		});
		
		double minPeriod = 0;
		double maxPeriod = 0;
		minPeriod = src[0].getMinPeriod();
		maxPeriod = src[0].getMaxPeriod();
		
		double[][] periodRanges = new double[][] {{minPeriod, maxPeriod}};
		
		List<Phase> phaseList = Arrays.asList(Phase.ScS, Phase.S);
		Phase[] phases = new Phase[phaseList.size()];
		for (int i=0; i<phases.length; i++) {
			phases[i] = phaseList.get(i);
		}
		
		Arrays.stream(src).forEach(id -> {
			globalCMTIDSet.add(id.getGlobalCMTID());
			stationSet.add(id.getStation());
			ppLoc.add(id.getPerturbationLocation());
		});		
		
		try (WaveformDataWriter wdw = new WaveformDataWriter(idPath, dataPath, stationSet, globalCMTIDSet, periodRanges, phases, ppLoc);
				Stream<PartialID> idStream = Arrays.stream(src)	;){
//						.filter(id -> globalCMTIDSetRef.contains(id.getGlobalCMTID()))
//						.filter(id -> !(id.getSacComponent().equals(SACComponent.Z)));) {
			Set<PartialID> partials = new HashSet<>();
			idStream.forEach(id -> {
				partials.add(id);
			});
			partials.stream().filter(id -> globalCMTIDSetRef.contains(id.getGlobalCMTID()))
				.filter(id -> !(id.getSacComponent().equals(SACComponent.Z)))
				.forEach(partial -> {
				try {
					if (! partial.getWaveformType().equals(WaveformType.PARTIAL)) {
						System.out.println(partial.toString() + "is not a partial, it is a " + partial.getWaveformType().toString());
						throw new RuntimeException();
					}
					
					System.out.println(partial.getGlobalCMTID());
					wdw.addPartialID(partial);	//	ここで書き込み
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static Location toLocation(String line) {
		String[] parts = line.split("\\s+");
		return new Location(Double.parseDouble(parts[0]),
				Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
	}

}
