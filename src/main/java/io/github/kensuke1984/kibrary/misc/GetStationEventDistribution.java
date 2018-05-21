/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

/**
 * @version 0.0.1
 * @since 2016/08/30
 * @author Yuki
 *
 */
public class GetStationEventDistribution {
	
	public static void main(String[] args) throws IOException {
		if (args.length != 3 || args[0] == "--help")
			System.err.println("usage: waveID, waveData, output");
		Path waveIDPath = Paths.get(args[0]);
		Path waveformPath = Paths.get(args[1]);
		Path outPath = Paths.get(args[2]);
		System.out.println(waveIDPath+" read.");
		GetStationEventDistribution getSED = new GetStationEventDistribution();
		getSED.outputDistribution(waveIDPath, waveformPath, outPath);
		System.out.println("Done!");
	}
	
	/**
	 * station と 震源の位置関係の出力
	 * 
	 * @param outPath
	 *            {@link File} for output
	 * @param waveIDPath 
	 * @param waveformPath          
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void outputDistribution(Path waveIDPath, Path waveformPath, Path outPath) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			BasicID[] ids = BasicIDFile.readBasicIDandDataFile(waveIDPath, waveformPath);
			Dvector dVector = new Dvector(ids);
			BasicID[] obsIDs = dVector.getObsIDs();
			pw.println("#station(lat lon) event(lat lon r) EpicentralDistance Azimuth ");
			Arrays.stream(obsIDs).forEach(id -> {
				GlobalCMTData event = id.getGlobalCMTID().getEvent();
				Station station = id.getStation();
				double epicentralDistance = Math
						.toDegrees(station.getPosition().getEpicentralDistance(event.getCmtLocation()));
				double azimuth = Math.toDegrees(station.getPosition().getAzimuth(event.getCmtLocation()));
				pw.println(station + " " + station.getPosition() + " " + id.getGlobalCMTID() + " "
						+ event.getCmtLocation() + " " + Math.round(epicentralDistance * 100) / 100.0 + " "
						+ Math.round(100 * azimuth) / 100.0 + " "
						+ station.getNetwork());
			});
		}
	}

}
