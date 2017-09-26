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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.WaveformDataWriter;

/**
 * @version 0.0.1
 * @since 2017/08/09
 * @author Yuki
 *
 */
public class RandomWaveDataChoose {
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		if (args.length != 5)
			System.err.println("Usage: source BasicID path, source  WaveData path, output BasicID path, output WaveData path, globalCMTIDs");
		Path srcID = Paths.get(args[0]);
		Path srcData = Paths.get(args[1]);
		Path idPath = Paths.get(args[2]);
		Path dataPath = Paths.get(args[3]);
		Path gidPath = Paths.get(args[4]);
//		Path partialIDPath = Paths.get(args[5]);
		BasicID[] src = BasicIDFile.readBasicIDandDataFile(srcID, srcData);
//		PartialID[] partialIDs = PartialIDFile.readPartialIDFile(partialIDPath);
		
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
		});
		
		try (WaveformDataWriter wdw = new WaveformDataWriter(idPath, dataPath, stationSet, globalCMTIDSet, periodRanges, phases);
				Stream<BasicID> idStreamObs = Arrays.stream(src);
				Stream<BasicID> idStreamSyn = Arrays.stream(src);) {
			List<BasicID> idObsList = idStreamObs.filter(id -> id.getWaveformType().equals(WaveformType.OBS))
					.collect(Collectors.toList());
			Collections.shuffle(idObsList);
			System.out.println("OBS list size is "+idObsList.size());
			List<BasicID> idSynList = idStreamSyn.filter(id -> id.getWaveformType().equals(WaveformType.SYN))
					.collect(Collectors.toList());
			System.out.println("SYN list size is "+idSynList.size());
			Collections.shuffle(idSynList);
			
			
			idObsList.stream().filter(id -> globalCMTIDSetRef.contains(id.getGlobalCMTID()))
			.filter(id -> id.getWaveformType().equals(WaveformType.OBS))
			.filter(id -> !(id.getSacComponent().equals(SACComponent.Z)))
			.filter(id -> !(id.getStation().equals(new Station("MLAC", new HorizontalPosition(37.6302, -118.8361), "CI"))))
			
			.forEachOrdered(id -> {
			try {
				wdw.addBasicID(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
			idSynList.stream()
				.filter(idSyn -> globalCMTIDSetRef.contains(idSyn.getGlobalCMTID()))
				.filter(idSyn -> idSyn.getWaveformType().equals(WaveformType.SYN))
				.filter(idSyn -> idSyn.getGlobalCMTID().equals(id.getGlobalCMTID()))
				.filter(idSyn -> idSyn.getStation().equals(id.getStation()))
				.filter(idSyn -> idSyn.getSacComponent().equals(id.getSacComponent()))
				.filter(idSyn -> idSyn.getPhases().length == id.getPhases().length)
				.filter(idSyn -> idSyn.getPhases()[0] == id.getPhases()[0])
				.forEachOrdered(idSyn -> {
					try {
//						System.out.println("syn phase "+idSyn.getPhases()[0]+" "+idSyn.getPhases()[1]);
						System.out.println(id + "\n" + idSyn);
						wdw.addBasicID(idSyn);
					} catch (IOException e1) {
						// TODO 自動生成された catch ブロック
						e1.printStackTrace();
					}
				});
			});
			
			/**
			IntStream.range(0, idObsList.size()).filter(e -> e % 2 == 0)
					.mapToObj(e -> idObsList.get(e))
					.filter(id -> globalCMTIDSetRef.contains(id.getGlobalCMTID()))
					.forEach(id -> {
						try {
							System.out.println(id);
							wdw.addBasicID(id);
						} catch (IOException e1) {
							// TODO 自動生成された catch ブロック
							e1.printStackTrace();
						}
						idSynList.stream()
							.filter(idSyn -> idSyn.getGlobalCMTID().equals(id.getGlobalCMTID()))
							.filter(idSyn -> idSyn.getStation().equals(id.getStation()))
							.filter(idSyn -> idSyn.getWaveformType().equals(WaveformType.SYN))
							.filter(idSyn -> idSyn.getSacComponent().equals(id.getSacComponent()))
							.filter(idSyn -> idSyn.getPhases().length == id.getPhases().length)
							.filter(idSyn -> idSyn.getPhases()[0] == id.getPhases()[0])
							.forEach(idSyn -> {
								try {
									wdw.addBasicID(idSyn);
								} catch (IOException e1) {
									// TODO 自動生成された catch ブロック
									e1.printStackTrace();
								}
							});
					});
				**/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
