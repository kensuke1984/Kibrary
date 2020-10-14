package io.github.kensuke1984.kibrary.selection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.addons.DistanceAzimuth;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

public class Bins2D {
	private Map<DistanceAzimuth, List<Station>> bins;
	private GlobalCMTID event;
	private double dAzimuth;
	private double dDistance;
	
	public Bins2D(GlobalCMTID event, double dAzimuth, double dDistance, Set<TimewindowInformation> timewindows) {
		bins = new HashMap<>();
		this.event = event;
		this.dAzimuth = dAzimuth;
		this.dDistance = dDistance;
		
		for (TimewindowInformation timewindow : timewindows) {
			Station station = timewindow.getStation();
			Location eventLocation = event.getEvent().getCmtLocation();
			double distance = station.getPosition().getEpicentralDistance(eventLocation)
					* 180. / Math.PI;
			double azimuth = eventLocation.getAzimuth(station.getPosition()) * 180. / Math.PI;
			DistanceAzimuth distance_azimuth = getBinPosition(distance, azimuth);
			
			if (bins.containsKey(distance_azimuth)) {
				List<Station> stations = bins.get(distance_azimuth);
				stations.add(station);
				bins.replace(distance_azimuth, stations);
			}
			else {
				List<Station> stations = new ArrayList<>();
				stations.add(station);
				bins.put(distance_azimuth, stations);
			}
		}
	}
	
	public Map<DistanceAzimuth, List<Station>> getBins() {
		return bins;
	}
	
	public GlobalCMTID getGlobalCMTID() {
		return event;
	}
	
	public DistanceAzimuth getBinPosition(double distance, double azimuth) {
		distance = (int) (distance / dDistance) * dDistance + dDistance / 2.;
		azimuth = (int) (azimuth / dAzimuth) * dAzimuth + dAzimuth / 2.;
		return new DistanceAzimuth(distance, azimuth);
	}
	
	public DistanceAzimuth getBinPosition(TimewindowInformation timewindow) {
		Station station = timewindow.getStation();
		Location eventLocation = event.getEvent().getCmtLocation();
		double distance = station.getPosition().getEpicentralDistance(eventLocation)
				* 180. / Math.PI;
		double azimuth = eventLocation.getAzimuth(station.getPosition()) * 180. / Math.PI;
		
		return getBinPosition(distance, azimuth);
	}
}
