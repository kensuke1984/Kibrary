package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExcludeTimewindow {

	public static void main(String[] args) {
		Path timewindowFile = Paths.get(args[0]);
//		Path timewindowASCIIFile = Paths.get(args[1]);
		Path newTimewindowFile = Paths.get("timewindow" + Utilities.getTemporaryString() + ".dat");
		
		try {
			Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowFile);
//			Set<TimewindowInformation> newTimewindows = excludeStation(timewindows, timewindowASCIIFile);
			
//			Set<Phases> phaseSet = new HashSet<>();
//			phaseSet.add(new Phases("Sdiff"));
//			phaseSet.add(new Phases("S"));
//			Set<TimewindowInformation> newTimewindows = excludePhase(timewindows, phaseSet);
			
			
			if (args.length == 2) {
				Set<Station> stations = StationInformationFile.read(Paths.get(args[1]));
				
				Set<TimewindowInformation> newTimewindows = timewindows.parallelStream()
						.filter(tw -> stations.contains(tw.getStation()))
						.filter(tw -> tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition()) * 180. / Math.PI <= 35.)
						.collect(Collectors.toSet());
				
				TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
			}
			
			else {
				Set<TimewindowInformation> newTimewindows = timewindows.parallelStream()
						.filter(tw ->  {
							double distance = tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition()) * 180. / Math.PI;
							if (distance > 35. || distance < 5.)
								return false;
							else 
								return true;
						}).filter(tw -> !tw.getGlobalCMTID().equals(new GlobalCMTID("201102251307A")))
	//					.filter(tw -> tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition()) * 180. / Math.PI >= 18.)
						.collect(Collectors.toSet());
				
				
				Map<GlobalCMTID, Integer> nWindowEventMap = new HashMap<>();
				for (TimewindowInformation timewindow : newTimewindows) {
					GlobalCMTID id = timewindow.getGlobalCMTID();
					if (nWindowEventMap.containsKey(id)) {
						int n = nWindowEventMap.get(id) + 1;
						nWindowEventMap.replace(id, n);
					}
					else
						nWindowEventMap.put(id, 1);
				}
				
				Set<TimewindowInformation> newTimewindows2 = newTimewindows.parallelStream().filter(tw -> nWindowEventMap.get(tw.getGlobalCMTID()) >= 15)
					.collect(Collectors.toSet());
				TimewindowInformationFile.write(newTimewindows2, newTimewindowFile);
				
	//			TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
	}
	
	private static Set<TimewindowInformation> excludeStation(Set<TimewindowInformation> timewindows
			, Path timewindowASCIIFile) throws IOException {
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
		
		return newTimewindows;
	}
	
	private static Set<TimewindowInformation> excludePhase(Set<TimewindowInformation> timewindows,
			Set<Phases> phaseSet) throws IOException {
		return timewindows.stream().filter(tw -> 
			!phaseSet.contains(new Phases(tw.getPhases()))
		).collect(Collectors.toSet());
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
