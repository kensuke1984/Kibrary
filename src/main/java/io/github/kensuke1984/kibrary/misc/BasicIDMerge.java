/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.WaveformDataWriter;

/**
 * @version 0.0.1
 * @since 2017/05/11
 * @author Yuki
 *
 */


class BasicIDMerge {

	public static void main(String[] args) throws IOException, IllegalArgumentException {
		if (args.length < 6)
			System.err.println("usage - src0ID, src1ID, src0data, src1data, outID, outData");
		Path src0ID = Paths.get(args[0]);
		Path src1ID = Paths.get(args[1]);
		Path src0Data = Paths.get(args[2]);
		Path src1Data = Paths.get(args[3]);
		Path idPath = Paths.get(args[4]);
		Path dataPath = Paths.get(args[5]);
		
		BasicID[] src0 = BasicIDFile.readBasicIDandDataFile(src0ID, src0Data);
		BasicID[] src1 = BasicIDFile.readBasicIDandDataFile(src1ID, src1Data);
		
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
		
//		Phase[] phases = Stream.of(src0).map(BasicID::getPhases).flatMap(p -> Stream.of(p)).distinct().toArray(Phase[]::new);
		
		try (WaveformDataWriter wdw = new WaveformDataWriter(idPath, dataPath, stationSet, globalCMTIDSet, periodRanges);
				Stream<BasicID> idStream = Stream.concat(Stream.of(src0), Stream.of(src1));) {
//			Set<BasicID> waveforms = new HashSet
//			idStream.forEach(id -> {
//				waveforms.add(id);
//			});
			idStream.distinct().forEach(id -> {
//				waveforms.add(id);
				try {
					wdw.addBasicID(id);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			
//			idStream.filter(id->id.getStationName().equals("ABC")).forEach(id -> {
////				waveforms.add(id);
//				try {
//					wdw.addBasicID(id);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			});
//			waveforms.forEach(waveform -> {
//				try {
//					wdw.addBasicID(waveform);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
