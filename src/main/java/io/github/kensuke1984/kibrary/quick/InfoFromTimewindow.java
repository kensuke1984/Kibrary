package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InfoFromTimewindow {

	public static void main(String[] args) throws IOException {
		Path timewindowpath = Paths.get(args[0]);
		Path stationFile = Paths.get("station" + Utilities.getTemporaryString() + ".inf");
		
		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowpath);
		
		Files.deleteIfExists(stationFile);
		Files.createFile(stationFile);
		
		Set<Station> usedStation = new HashSet<>();
		Map<GlobalCMTID, Integer> nTransverseMap = new HashMap<>();
		for (TimewindowInformation timewindow : timewindows) {
			GlobalCMTID event = timewindow.getGlobalCMTID();
			Integer itmp = new Integer(1);
			if (nTransverseMap.containsKey(event)) {
				itmp = nTransverseMap.get(event) + 1;
			}
			nTransverseMap.put(event, itmp);
			
			Station sta = timewindow.getStation();
			usedStation.add(sta);
		}
		
		for (Station sta : usedStation)
			Files.write(stationFile, (sta.getStationName() + " " + sta.getNetwork() + " " + sta.getPosition()+"\n").getBytes(), StandardOpenOption.APPEND);
		
		for (GlobalCMTID id : nTransverseMap.keySet())
			System.out.println(id + " " + nTransverseMap.get(id));
	}

}
