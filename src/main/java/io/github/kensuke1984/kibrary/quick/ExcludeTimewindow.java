package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class ExcludeTimewindow {

	public static void main(String[] args) {
		Path timewindowFile = Paths.get(args[0]);
		Path timewindowASCIIFile = Paths.get(args[1]);
		
		Path newTimewindowFile = Paths.get("timewindow" + Utilities.getTemporaryString() + ".dat");
		
		try {
			Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowFile);
			
			BufferedReader reader = Files.newBufferedReader(timewindowASCIIFile);
			String line = null;
			Set<ReducedInfo> excludeTimewindows = new HashSet<>();
			while ((line = reader.readLine()) != null) {
				String[] s = line.trim().split(" ");
				String stationName = s[0].split("_")[0];
				GlobalCMTID id = new GlobalCMTID(s[1]);
				SACComponent component = SACComponent.valueOf(s[2]);
				double startTime = Double.parseDouble(s[3]);
				ReducedInfo info = new ReducedInfo(stationName, id, component, startTime);
				excludeTimewindows.add(info);
			}
			
			Set<TimewindowInformation> newTimewindows = new HashSet<>();
			for (TimewindowInformation tw : timewindows) {
				boolean exclude = false;
				for (ReducedInfo info : excludeTimewindows) {
					if (info.isPair(tw)) {
						exclude = true;
						break;
					}
				}
				if (exclude)
					continue;
				
				newTimewindows.add(tw);
			}
			
			TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
	}
	
	
	private static class ReducedInfo {
		public String stationName;
		public GlobalCMTID id;
		public SACComponent component;
		public double startTime;
		
		public ReducedInfo(String stationName, GlobalCMTID id, SACComponent component, double startTime) {
			this.stationName = stationName;
			this.id = id;
			this.component = component;
			this.startTime = startTime;
		}
		
		public boolean isPair(TimewindowInformation tw) {
			if (!tw.getStation().getStationName().equals(stationName))
				return false;
			else if (!tw.getGlobalCMTID().equals(id))
				return false;
			else if (!tw.getComponent().equals(component))
				return false;
			else if (Math.abs(tw.getStartTime() - startTime) > 0.1)
				return false;
			else
				return true;
		}
	}
}
