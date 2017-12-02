package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class testTimewindowSet {

	public static void main(String[] args) {
		Set<TimewindowInformation> timewindows = getTestwindows();
		Path outputPath = Paths.get("testTimewindow.inf");
		
		try {
			TimewindowInformationFile.write(timewindows, outputPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static Set<TimewindowInformation> getTestwindows() {
		GlobalCMTID id = new GlobalCMTID("200503070717A");
		Station station = new Station("340A", new HorizontalPosition(31.41670036315918, -93.88960266113281), "TA");
		
		Location loc = id.getEvent().getCmtLocation();
		double distance = loc.getEpicentralDistance(station.getPosition()) * 180. / Math.PI ;
		
//		System.out.println((Earth.EARTH_RADIUS - loc.getR()) + " " + distance);
		
		Set<TimewindowInformation> timewindows = new HashSet<>();
		TimewindowInformation window1 = new TimewindowInformation(415., 495., station, id, SACComponent.T, new Phase[] {Phase.S});
		TimewindowInformation window2 = new TimewindowInformation(816., 896., station, id, SACComponent.T, new Phase[] {Phase.ScS});
		TimewindowInformation window3 = new TimewindowInformation(1732., 1812., station, id, SACComponent.T, new Phase[] {Phase.create("ScSScS")});
		
		timewindows.add(window1);
		timewindows.add(window2);
		timewindows.add(window3);
		return timewindows;
	}
	
}
