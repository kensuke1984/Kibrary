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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExcludeTimewindow {

	public static void main(String[] args) {
		Path timewindowFile = Paths.get(args[0]);
//		Path timewindowASCIIFile = Paths.get(args[1]);
		Path newTimewindowFile = Paths.get("timewindow" + Utilities.getTemporaryString() + ".dat");
		
		Set<GlobalCMTID> wellDefinedEvent = Stream.of(new String[] {"201104170158A","200911141944A","201409241116A","200809031125A"
				,"200707211327A","200808262100A","201009130715A","201106080306A","200608250044A","201509281528A","201205280507A"
				,"200503211223A","201111221848A","200511091133A","201005241618A","200810122055A","200705251747A","201502111857A"
				,"201206020752A","201502021049A","200506021056A","200511171926A","201101010956A","200707120523A","201109021347A"
				,"200711180540A","201302221201A","200609220232A","200907120612A","201211221307A","200707211534A","200611130126A"
				,"201208020938A","201203050746A","200512232147A"})
				.map(GlobalCMTID::new)
				.collect(Collectors.toSet());
		
//		Path eachVarianceFile = Paths.get("eachVariance.txt");
//		List<String> stationNames = new ArrayList<>();
//		List<String> networkNames = new ArrayList<>();
//		List<GlobalCMTID> recordIDList = new ArrayList<>();
//		try {
//			BufferedReader br = Files.newBufferedReader(eachVarianceFile);
//			String line = "";
//			while ((line = br.readLine()) != null) {
//				String[] ss = line.split("\\s+");
//				stationNames.add(ss[1].split("_")[0]);
//				networkNames.add(ss[2]);
//				recordIDList.add(new GlobalCMTID(ss[3]));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		System.out.println(recordIDList.size());
		
		try {
			Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowFile);
//			Set<TimewindowInformation> newTimewindows = excludeStation(timewindows, timewindowASCIIFile);
			
//			Set<Phases> phaseSet = new HashSet<>();
//			phaseSet.add(new Phases("Sdiff"));
//			phaseSet.add(new Phases("S"));
//			Set<TimewindowInformation> newTimewindows = excludePhase(timewindows, phaseSet);
			
			
			if (args.length == 3) { //2
				Set<Station> stations = StationInformationFile.read(Paths.get(args[1]));
				
				Set<TimewindowInformation> newTimewindows = timewindows.parallelStream()
						.filter(tw -> stations.contains(tw.getStation()))
//						.filter(tw -> tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition()) * 180. / Math.PI <= 35.)
						.collect(Collectors.toSet());
				
				TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
			}
			
			else {
//				Set<TimewindowInformation> newTimewindows = timewindows.parallelStream()
//						.filter(tw ->  {
//							double distance = tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition()) * 180. / Math.PI;
//							if (distance > 35. || distance < 5.)
//								return false;
//							else 
//								return true;
//						}).filter(tw -> !tw.getGlobalCMTID().equals(new GlobalCMTID("201102251307A")))
//	//					.filter(tw -> tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition()) * 180. / Math.PI >= 18.)
//						.collect(Collectors.toSet());
//				
//				
//				Map<GlobalCMTID, Integer> nWindowEventMap = new HashMap<>();
//				for (TimewindowInformation timewindow : newTimewindows) {
//					GlobalCMTID id = timewindow.getGlobalCMTID();
//					if (nWindowEventMap.containsKey(id)) {
//						int n = nWindowEventMap.get(id) + 1;
//						nWindowEventMap.replace(id, n);
//					}
//					else
//						nWindowEventMap.put(id, 1);
//				}
//				
//				Set<TimewindowInformation> newTimewindows2 = newTimewindows.parallelStream().filter(tw -> nWindowEventMap.get(tw.getGlobalCMTID()) >= 15)
//					.collect(Collectors.toSet());
//				TimewindowInformationFile.write(newTimewindows2, newTimewindowFile);
//				
//	//			TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
//				
				
				
//				Map<GlobalCMTID, Integer> nTransverseMap = new HashMap<>();
//				for (TimewindowInformation timewindow : timewindows) {
//					GlobalCMTID event = timewindow.getGlobalCMTID();
//					Integer itmp = new Integer(1);
//					if (nTransverseMap.containsKey(event)) {
//						itmp = nTransverseMap.get(event) + 1;
//					}
//					nTransverseMap.put(event, itmp);
//				}
//				
//				Set<TimewindowInformation> newTimewindows = timewindows.stream().filter(tw -> nTransverseMap.get(tw.getGlobalCMTID()) >= 20)
//						.collect(Collectors.toSet());
//				TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
				
				
				Set<TimewindowInformation> newTimewindows = timewindows.stream().filter(tw -> {
					String sta = tw.getStation().getStationName();
					if (sta.equals("FUR") || sta.equals("C03") || sta.equals("SHO"))
						return false;
					else if (!wellDefinedEvent.contains(tw.getGlobalCMTID()))
						return false;
					else 
						return true;
				}).collect(Collectors.toSet());
				TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
				
				
//				Set<TimewindowInformation> newTimewindows = new HashSet<>();
//				for (TimewindowInformation window : timewindows) {
//					String staName = window.getStation().getStationName();
//					String network = window.getStation().getNetwork();
//					GlobalCMTID id = window.getGlobalCMTID();
//					for (int i = 0; i < recordIDList.size(); i++) {
//						if (stationNames.get(i).equals(staName) && networkNames.get(i).equals(network)
//								&& recordIDList.get(i).equals(id)) {
//							newTimewindows.add(window);
//							break;
//						}
//					}
//				}
//				TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
				
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
