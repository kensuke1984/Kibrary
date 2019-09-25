package io.github.kensuke1984.kibrary.timewindow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

public class makeWindow {

	public static void main(String[] args) throws IOException {		
		// S phase
//		double startTime = 538;
//		double endTime = 638;
//		Station station = new Station("109C", new HorizontalPosition(32.8889, -117.1051), "TA");
//		GlobalCMTID id = new GlobalCMTID("201407291046A");
//		Phase[] phases = new Phase[] {Phase.S};
//		
//		double startTime2 = 340.39;
//		double endTime2 = 540.39;
//		Station station2 = new Station("338A", new HorizontalPosition(31.3567, -95.3106), "TA");
//		GlobalCMTID id2 = new GlobalCMTID("080704C");
//		
//		double startTime3 = 629;
//		double endTime3 = 729;
//		Station station3 = new Station("K56A", new HorizontalPosition(42.6981, -77.3244), "TA");
//		GlobalCMTID id3 = new GlobalCMTID("201508050913A");
		
		
		// P phase
//		double startTime = 307;
//		double endTime = 407;
//		Station station = new Station("109C", new HorizontalPosition(32.8889, -117.1051), "TA");
//		GlobalCMTID id = new GlobalCMTID("201407291046A");
//		Phase[] phases = new Phase[] {Phase.P};
//		
//		double startTime2 = 175;
//		double endTime2 = 275;
//		Station station2 = new Station("338A", new HorizontalPosition(31.3567, -95.3106), "TA");
//		GlobalCMTID id2 = new GlobalCMTID("080704C");
//		
//		double startTime3 = 339;
//		double endTime3 = 439;
//		Station station3 = new Station("K56A", new HorizontalPosition(42.6981, -77.3244), "TA");
//		GlobalCMTID id3 = new GlobalCMTID("201508050913A");
//		
		Path outpath = Paths.get("timewindow" + Utilities.getTemporaryString() + ".dat");
//		
//		SACComponent component = SACComponent.Z;
//		
//		TimewindowInformation window = new TimewindowInformation(startTime, endTime, station, id, component, phases);
//		TimewindowInformation window2 = new TimewindowInformation(startTime2, endTime2, station2, id2, component, phases);
//		TimewindowInformation window3 = new TimewindowInformation(startTime3, endTime3, station3, id3, component, phases);
//		
		Set<TimewindowInformation> infoSet = new HashSet<>();
//		infoSet.add(window);
//		infoSet.add(window2);
//		infoSet.add(window3);
		
		double t0 = 0;
		double t1 = 3544;
		Path workdir = Paths.get(".");
		Set<SACFileName> sacnames = null;
		try (Stream<Path> stream = Files.list(workdir)) {
			sacnames = stream.filter(SACFileName::isSacFileName).map(SACFileName::new).collect(Collectors.toSet());
		}
		for (SACFileName sacname : sacnames) {
			SACData data = sacname.read();
			TimewindowInformation window = new TimewindowInformation(t0, t1, data.getStation(), data.getGlobalCMTID(),
					sacname.getComponent(), new Phase[0]);
			infoSet.add(window);
		}
		
		TimewindowInformationFile.write(infoSet, outpath, StandardOpenOption.CREATE);
	}

}
