package io.github.kensuke1984.kibrary.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.math3.exception.DimensionMismatchException;

import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

public class CreateScriptWaveform {
	
	public static void main(String[] args) {
		String[] eventIDs = new String[] {args[0]};
		Path outPath = Paths.get(args[1]);
		String born = args[2];
		Path eventDirsPath = Paths.get(args[3]); // path to "trace"
		
		try {
			Set<EventFolder> events = Utilities.eventFolderSet(eventDirsPath);
			Set<Station> stationInformation = StationInformationFile.read(Paths.get(args[4]));
			BasicID[] basicIDs = BasicIDFile.readBasicIDandDataFile(Paths.get(args[5])
					, Paths.get(args[6]));
			InversionResult ir = new InversionResult(Paths.get(args[7]));
			
			
			events.stream()
				.filter(event -> true)
				.forEach(event -> {
				String eventID = event.getGlobalCMTID().toString();
				try {
					String[] s = new String[] {""};
					double[] extremalDistances = new double[] {360., 0.};
					double[] extremalTimes = new double[] {1e5, -1e5};
					Stream.of(basicIDs).filter(id -> id.getGlobalCMTID().toString().equals(eventID)).filter(id -> id.getWaveformType().equals(WaveformType.OBS))
						.forEach(id -> {
							String txtName = "/" + id.getStation().toString() + "." + id.getGlobalCMTID() + "." + id.getSacComponent()
							+ "." + ir.getBasicIDList().indexOf(id) + ".txt";
							
							double epicentralDistance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
									stationInformation.stream().filter(sta->sta.getName().equals(id.getStation().toString())).findAny().get().getPosition())*180/Math.PI;
							
							if (epicentralDistance >= 70.) {
								if (epicentralDistance > extremalDistances[1])
									extremalDistances[1] = epicentralDistance;
								if (epicentralDistance < extremalDistances[0])
									extremalDistances[0] = epicentralDistance;
								if (id.getStartTime() - 8.3 * epicentralDistance < extremalTimes[0])
									extremalTimes[0] = id.getStartTime() - 8.3 * epicentralDistance;
								if (id.getStartTime() + id.getNpts() / id.getSamplingHz() - 8.3 * epicentralDistance > extremalTimes[1])
									extremalTimes[1] = id.getStartTime() + id.getNpts() / id.getSamplingHz() - 8.3 * epicentralDistance;
							
								s[0] += String.join("/", "\"../../trace", eventID) + txtName + "\"" + " u ($2-8.3*" + String.valueOf(epicentralDistance) + "-shift" + "):($3+" + String.valueOf(epicentralDistance) + ") w lines lw .5 lc rgb \"black\", "
											  + String.join("/", "\"../../trace", eventID) + txtName + "\"" + " u ($2-8.3*" + String.valueOf(epicentralDistance) + "-shift" + "):($4+" + String.valueOf(epicentralDistance) + ") w lines lw .5 lc rgb \"blue\", "
											  + String.join("/", "\"../../born", born, eventID) + txtName + "\"" + " u ($1-8.3*" + String.valueOf(epicentralDistance) + "-shift" + "):($2+" + String.valueOf(epicentralDistance) + ") w lines lw .5 lc rgb \"red\",\\\n";
							}
						});
					
					Files.write(Paths.get(outPath.toString(), born, "profile" + eventID + ".plt")
							, String.join("\n", "set terminal postscript enhanced color font \"Helvetica, 28pt\""
							, "shift=" + extremalTimes[0]
							, "unset key"
							, "set size .5, 1"
							, "set yrange[" + (extremalDistances[0] - 2) + ":" + (extremalDistances[1] + 2) + "]"
							, "set xrange[" + -2 + ":" + (extremalTimes[1] - extremalTimes[0] + 2) + "]"
//							, "set xtics " + String.format("%.0f", (extremalTimes[1] - extremalTimes[0] + 4) / 2 - ((extremalTimes[1] - extremalTimes[0] + 4) / 2) % 10) + " nomirror"
							, "set xtics 60 nomirror"
							, "set ytics 10 nomirror"
//							, "set ytics " + String.format("%.0f", (extremalDistances[1] - extremalDistances[0] + 4) / 4) + " nomirror"
							, "set title \"" + eventID + "\""
							, "set output \"profile" + eventID + ".eps\""
							, "p " + s[0]).getBytes()
						, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DimensionMismatchException e) {
			e.printStackTrace();
		}

	}

}
