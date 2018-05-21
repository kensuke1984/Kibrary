/**
 * 
 */
package io.github.kensuke1984.kibrary.datacorrection;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * make a new static correction file which contains a time shift
 * for other component corresponding to T component
 * @version 0.0.1
 * @since 2018/05/09
 * @author Yuki
 *
 */
public class CorrespondingTimeshift4otherComponent {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO 自動生成されたメソッド・スタブ
		try {
			if (args.length != 3 || args[0]=="--help")
				System.err.println("usage - srcStaticCorrectionFile, component you want, outStaticCorrectionFile");
			Path src = Paths.get(args[0]);
			SACComponent component = SACComponent.valueOf(args[1]);
			Path out = Paths.get(args[2]);
			Set<StaticCorrection> shiftSet = StaticCorrectionFile.read(src);
			write(shiftSet, component, out, StandardOpenOption.CREATE_NEW);
//			StaticCorrectionFile.write(shiftSet, out, StandardOpenOption.APPEND);
			shiftSet.stream().forEachOrdered(sc -> {
				
			});
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
	
	/**
	 * @param correctionSet
	 *            of static correction to write
	 * @param outPath
	 *            of an output file.
	 * @param options
	 *            for output
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	private static void write(Set<StaticCorrection> correctionSet, SACComponent component, Path outPath, OpenOption... options)
			throws IOException {
		Station[] stations = correctionSet.stream().map(StaticCorrection::getStation).distinct().sorted()
				.toArray(Station[]::new);
		GlobalCMTID[] ids = correctionSet.stream().map(StaticCorrection::getGlobalCMTID).distinct().sorted()
				.toArray(GlobalCMTID[]::new);

		Map<Station, Integer> stationMap = IntStream.range(0, stations.length).boxed()
				.collect(Collectors.toMap(i -> stations[i], i -> i));
		Map<GlobalCMTID, Integer> idMap = IntStream.range(0, ids.length).boxed()
				.collect(Collectors.toMap(i -> ids[i], i -> i));

		try (DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(Files.newOutputStream(outPath, options)))) {
			dos.writeShort(stations.length);
			dos.writeShort(ids.length);
			for (Station station : stations) {
				dos.writeBytes(StringUtils.rightPad(station.getName(), 8));
				dos.writeBytes(StringUtils.rightPad(station.getNetwork(), 8));
				HorizontalPosition pos = station.getPosition();
				dos.writeFloat((float) pos.getLatitude());
				dos.writeFloat((float) pos.getLongitude());
			}
			for (GlobalCMTID id : ids) dos.writeBytes(StringUtils.rightPad(id.toString(), 15));
			
			correctionSet.stream().forEach(correction -> {
				try {
					dos.writeShort(stationMap.get(correction.getStation()));
					dos.writeShort(idMap.get(correction.getGlobalCMTID()));
					dos.writeByte(component.valueOf());
					dos.writeFloat((float) correction.getSynStartTime());
					dos.writeFloat((float) correction.getTimeshift());
					dos.writeFloat((float) correction.getAmplitudeRatio());
				}
				catch (IOException e) {e.printStackTrace();};
			});
		}	
	}	
}
