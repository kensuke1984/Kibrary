/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;

import java.io.IOException;
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

import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.WaveformDataWriter;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;

/**
 * @version 0.0.1
 * @since 2017/01/28
 * @author Yuki
 *
 */
public class JackKnifeTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		Path srcID = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/waveformID.dat");
		Path srcData = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/waveform.dat");
		Path idPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/waveformID_JK3.dat");
		Path dataPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/waveform_JK3.dat");
		BasicID[] src = BasicIDFile.readBasicIDandDataFile(srcID, srcData);
		
		Set<Station> stationSet = new HashSet<>();
		Set<GlobalCMTID> globalCMTIDSet = new HashSet<>();
		
		double minPeriod = 0;
		double maxPeriod = 0;
		minPeriod = src[0].getMinPeriod();
		maxPeriod = src[0].getMaxPeriod();
		
		double[][] periodRanges = new double[][] {{minPeriod, maxPeriod}};
		
		Arrays.stream(src).forEach(id -> {
			globalCMTIDSet.add(id.getGlobalCMTID());
			stationSet.add(id.getStation());
		});
		
		//TODO!!!
		try (WaveformDataWriter wdw = new WaveformDataWriter(idPath, dataPath, stationSet, globalCMTIDSet, periodRanges, null);
				Stream<BasicID> idStreamObs = Arrays.stream(src);
				Stream<BasicID> idStreamSyn = Arrays.stream(src);) {
			List<BasicID> idObsList = idStreamObs.filter(id -> id.getWaveformType().equals(WaveformType.OBS))
					.collect(Collectors.toList());
			Collections.shuffle(idObsList);
			List<BasicID> idSynList = idStreamSyn.filter(id -> id.getWaveformType().equals(WaveformType.SYN))
					.collect(Collectors.toList());
			Collections.shuffle(idSynList);
			IntStream.range(0, idObsList.size()).filter(e -> e % 2 == 0)
					.mapToObj(e -> idObsList.get(e))
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
							.forEach(idSyn -> {
								try {
									wdw.addBasicID(idSyn);
								} catch (IOException e1) {
									// TODO 自動生成された catch ブロック
									e1.printStackTrace();
								}
							});
					});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
