/**
 * 
 */
package io.github.kensuke1984.kibrary.timewindow;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * @version 0.0.1
 * @since 2018/05/16
 * @author Yuki
 *
 */
public class CorrecpondingTimewindow4otherComponent {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO 自動生成されたメソッド・スタブ
		try {
			if (args.length != 3 || args[0]=="--help")
				System.err.println("usage - srcTimewindowFile, component you want, outTimewindowFile");
			Path src = Paths.get(args[0]);
			SACComponent component = SACComponent.valueOf(args[1]);
			Path out = Paths.get(args[2]);
			Set<TimewindowInformation> windowInfSet = TimewindowInformationFile.read(src);
			write(windowInfSet, component, out, StandardOpenOption.CREATE_NEW);
			windowInfSet.stream().forEachOrdered(sc -> {
				
			});
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
	
	 /** Output TimeWindowInformation
	  * 
	  * @param infoSet
	  *            Set of timewindow information
	  * @param outputPath
	  *            to write the information on
	  * @param options
	  *            for output
	  * @throws IOException
	  *             if an I/O error occurs.
	  */
	public static void write(Set<TimewindowInformation> infoSet, SACComponent component, Path outputPath, OpenOption... options)
			throws IOException {
		if (infoSet.isEmpty())
			throw new RuntimeException("Input information is empty..");
		try (DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(Files.newOutputStream(outputPath, options)))) {
			GlobalCMTID[] ids = infoSet.stream().map(TimewindowInformation::getGlobalCMTID).distinct().sorted()
					.toArray(GlobalCMTID[]::new);
			Station[] stations = infoSet.stream().map(TimewindowInformation::getStation).distinct().sorted()
					.toArray(Station[]::new);
			Phase[] phases = infoSet.stream().map(TimewindowInformation::getPhases).flatMap(p -> Stream.of(p))
				.distinct().toArray(Phase[]::new);
			
			Map<GlobalCMTID, Integer> idMap = new HashMap<>();
			Map<Station, Integer> stationMap = new HashMap<>();
			Map<Phase, Integer> phaseMap = new HashMap<>();
			dos.writeShort(stations.length);
			dos.writeShort(ids.length);
			dos.writeShort(phases.length);
			for (int i = 0; i < stations.length; i++) {
				stationMap.put(stations[i], i);
				dos.writeBytes(StringUtils.rightPad(stations[i].getName(), 8));
				dos.writeBytes(StringUtils.rightPad(stations[i].getNetwork(), 8));
				HorizontalPosition pos = stations[i].getPosition();
				dos.writeFloat((float) pos.getLatitude());
				dos.writeFloat((float) pos.getLongitude());
			}
			for (int i = 0; i < ids.length; i++) {
				idMap.put(ids[i], i);
				dos.writeBytes(StringUtils.rightPad(ids[i].toString(), 15));
			}
			for (int i = 0; i < phases.length; i++) {
				phaseMap.put(phases[i], i);
				if (phases[i] == null)
					throw new NullPointerException(i + " " + "phase is null");
				dos.writeBytes(StringUtils.rightPad(phases[i].toString(), 16));
			}
			for (TimewindowInformation info : infoSet) {
				dos.writeShort(stationMap.get(info.getStation()));
				dos.writeShort(idMap.get(info.getGlobalCMTID()));
				Phase[] Infophases = info.getPhases();
				for (int i = 0; i < 10; i++) {
					if (i < Infophases.length) {
						dos.writeShort(phaseMap.get(Infophases[i]));
					}
					else
						dos.writeShort(-1);
				}
				dos.writeByte(component.valueOf());
				float startTime = (float) Precision.round(info.startTime, 3);
				float endTime = (float) Precision.round(info.endTime, 3);
				dos.writeFloat(startTime);
				dos.writeFloat(endTime);
			}
		}
	}

}
