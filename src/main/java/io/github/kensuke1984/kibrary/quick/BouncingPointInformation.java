package io.github.kensuke1984.kibrary.quick;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import edu.sc.seis.TauP.TimeDist;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.addons.EventCluster;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

public class BouncingPointInformation {

	public static void main(String[] args) throws TauModelException, IOException {
		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_6-200s");
		Path timewindowPath = workdir.resolve("selectedTimewindow_PcP_60deg.dat");
		
		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
		
//		Path clusterfilePath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster.inf");
		Path clusterfilePath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_6-200s/map/cluster_pcp.inf");
		
		List<EventCluster> clusters = EventCluster.readClusterFile(clusterfilePath);
		
		TauP_Time timetool = new TauP_Time("prem");
//		timetool.parsePhaseList("S, ScS");
		timetool.parsePhaseList("P, PcP");
		
		double maxdistance = 100.;
		
		Path outpath = workdir.resolve("bouncingPointInformation.dat");
		PrintWriter pw = new PrintWriter(outpath.toFile());
		
		for (TimewindowInformation timewindow : timewindows) {
			GlobalCMTID event = timewindow.getGlobalCMTID();
			double evtLat = event.getEvent().getCmtLocation().getLatitude();
			double evtLon = event.getEvent().getCmtLocation().getLongitude();
			
			int index = -1;
			if (clusters.stream().filter(c -> c.getID().equals(event)).count() > 0)
				index = clusters.stream().filter(c -> c.getID().equals(event)).findFirst().get().getIndex();
			else
				continue;
			
			double distance = Math.toDegrees(event.getEvent().getCmtLocation().getEpicentralDistance(timewindow.getStation().getPosition()));
			if (distance > maxdistance)
				continue;
			
			double azimuth = Math.toDegrees(event.getEvent().getCmtLocation().getAzimuth(timewindow.getStation().getPosition()));
			
			timetool.setSourceDepth(6371. - event.getEvent().getCmtLocation().getR());
			timetool.calculate(distance);
			Arrival arrivalS = timetool.getArrival(0);
			Arrival arrivalScS = timetool.getArrival(1);
//			if (!arrivalS.getPhase().getName().equals("S") || !arrivalScS.getPhase().getName().equals("ScS")) {
//				System.err.println("Problem computing S or ScS " + timewindow);
//				continue;
//			}
			if (!arrivalS.getPhase().getName().equals("P") || !arrivalScS.getPhase().getName().equals("PcP")) {
				System.err.println("Problem computing P or PcP " + timewindow);
				continue;
			}
			
			TimeDist[] pierces = arrivalScS.getPierce();
			double pierceDist = 0.;
			for (TimeDist p : pierces) {
				if (Math.abs(p.getDepth() - 2891.) < 0.001) {
					pierceDist = p.getDistDeg();
					break;
				}
			}
			
			double lat = SphericalCoords.latFor(evtLat, evtLon, pierceDist, azimuth);
			double lon = SphericalCoords.lonFor(evtLat, evtLon, pierceDist, azimuth);
			
			HorizontalPosition midPoint = new HorizontalPosition(lat, lon);
			
			pw.println(event + " " + timewindow.getStation() + " " + midPoint.getLatitude() + " " + midPoint.getLongitude() + " " + "index_" + index + " " + distance + " " + azimuth);
		}
		pw.close();
		
	}

}
