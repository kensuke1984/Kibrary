package io.github.kensuke1984.kibrary.selection;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import edu.sc.seis.TauP.TimeDist;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;

public class SelectInBouncingRegion {

	public static void main(String[] args) {
		Path timewindowPath = Paths.get(args[0]);
		
		double lonmin = -77;
		double lonmax = -74;
		double latmin = 6;
		double latmax = 12;
		
		try {
			Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
			Set<TimewindowInformation> selectedWindows = selectRegion(timewindows, lonmin, lonmax, latmin, latmax);
			Path outpath = Paths.get("timewindows" + Utilities.getTemporaryString() + ".dat");
			TimewindowInformationFile.write(selectedWindows, outpath);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public static Set<TimewindowInformation> selectRegion(Set<TimewindowInformation> timewindows, double lonmin, double lonmax, double latmin, double latmax) {
		Set<TimewindowInformation> selectedWindows = new HashSet<>();
		try {
			TauP_Time timetool = new TauP_Time("prem");
			timetool.parsePhaseList("ScS");
			for (TimewindowInformation window : timewindows) {
				Location eloc = window.getGlobalCMTID().getEvent().getCmtLocation();
				timetool.setSourceDepth(6371. - eloc.getR());
				double distance = Math.toDegrees(eloc.getEpicentralDistance(window.getStation().getPosition()));
				double azimuth = Math.toDegrees(eloc.getAzimuth(window.getStation().getPosition()));
				timetool.calculate(distance);
				TimeDist timedist = Arrays.stream(timetool.getArrival(0).getPierce()).filter(td -> Math.abs(td.getDepth() - 2891) < 1e-3).findFirst().get();
				if (Math.abs(timedist.getDepth() - 2891) > 0.5)
					throw new RuntimeException("" + timedist.getDepth());
				double bouncingDist = timedist.getDistDeg();
				double lat = SphericalCoords.latFor(eloc.getLatitude(), eloc.getLongitude(), bouncingDist, azimuth);
				double lon = SphericalCoords.lonFor(eloc.getLatitude(), eloc.getLongitude(), bouncingDist, azimuth);
				if (lat >= latmin && lat <= latmax && lon >= lonmin && lon <= lonmax)
					selectedWindows.add(window);
			}
		} catch (TauModelException e) {
			e.printStackTrace();
		}
		
		return selectedWindows;
	}

}
