package io.github.kensuke1984.kibrary.util.addons;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import edu.sc.seis.TauP.SphericalCoords;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

public class EventCluster {
	
	private GlobalCMTID id;
	
	private int index;
	
	private HorizontalPosition centerPosition;
	
	private List<Double> azimuthSlices;
	
	public EventCluster(GlobalCMTID id, int index, HorizontalPosition centerPosition, List<Double> azimuths) {
		this.id = id;
		this.index = index;
		this.centerPosition = centerPosition;
		this.azimuthSlices = azimuths;
	}
	
	public static void main(String[] args) throws IOException {
//		Set<GlobalCMTID> ids = Utilities.eventFolderSet(Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s")).stream().map(e -> e.getGlobalCMTID()).collect(Collectors.toSet());
		
//		HorizontalPosition pos1 = new HorizontalPosition(-23.47714285714286,-66.81357142857142);
//		HorizontalPosition pos2 = new HorizontalPosition(59.94971135353369,-120);
//		HorizontalPosition pos3 = new HorizontalPosition(65.4553190909262,-105);
//		HorizontalPosition pos4 = new HorizontalPosition(50,-126);
//
//		
//		System.out.println(pos1.getAzimuth(pos2) * 180 / Math.PI + " " + pos1.getAzimuth(pos3) * 180 / Math.PI + " " + pos1.getAzimuth(pos4) * 180 / Math.PI);
		
//		List<EventCluster> clusters = cluster(ids, 3.);
		
		List<EventCluster> clusters = readClusterFile(
				Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster-6deg.inf"));
//		List<EventCluster> clusters = readClusterFile(
//				Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_6-200s/map/cluster_pcp.inf"));
		
		clusters.stream().forEach(c -> System.out.println(c));
		
		Path outpath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/azimuthSeparation_cluster-6deg.inf");
//		Path outpath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_6-200s/map/azimuthSeparation_cluster_pcp.inf");
		writeAzimuthSeparation(clusters, outpath);
		
		Path waveformIDPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/waveformID_ScS_ext_70deg_semucbCorr_ampCorr_4hz.dat");
		BasicID[] waveformIDs = BasicIDFile.read(waveformIDPath);
		Map<Station, Set<Integer>> stationClusterMap = new HashMap<>();
		Arrays.stream(waveformIDs).forEach(id -> stationClusterMap.put(id.getStation(), new HashSet<>()));
		Arrays.stream(waveformIDs).forEach(id -> {
			int index = clusters.stream().filter(c -> c.getID().equals(id.getGlobalCMTID())).findFirst().get().getIndex();
			Set<Integer> tmpset = stationClusterMap.get(id.getStation());
			tmpset.add(index);
			stationClusterMap.replace(id.getStation(), tmpset);
		});
		outpath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/stationCluster.inf");
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (Station sta : stationClusterMap.keySet()) {
			String istring = "";
			for (int i : stationClusterMap.get(sta))
				istring += i + " ";
			pw.println(sta.getName() + " " + sta.getNetwork() + " " + sta.getPosition() + " " + istring);
		}
		pw.close();
	}
	
	public static List<EventCluster> readClusterFile(Path file) throws IOException {
		AtomicInteger imax = new AtomicInteger();
		List<EventCluster> clusters = Files.readAllLines(file).stream()
			.filter(line -> !line.startsWith("#"))
			.map(line -> {
			String[] ss = line.split("\\s+");
			GlobalCMTID id = new GlobalCMTID(ss[0].trim());
			HorizontalPosition centerPosition = new HorizontalPosition(Double.parseDouble(ss[3]), Double.parseDouble(ss[4]));
			int index = Integer.parseInt(ss[5]);
			if (index > imax.get()) imax.set(index);
			List<Double> azimuths = new ArrayList<>();
			for (int i = 6; i < ss.length; i++)
				azimuths.add(Double.parseDouble(ss[i]));
			return new EventCluster(id, index, centerPosition, azimuths);
		}).collect(Collectors.toList());
		
//		int nCluster = (int) clusters.stream().map(c -> c.index).distinct().count();
		for (int i = 0; i <= imax.get(); i++) {
			double[] latlon = new double[2];
			AtomicInteger count = new AtomicInteger();
			final int itmp = i;
			clusters.stream().filter(c -> c.index == itmp).forEach(c -> {
				latlon[0] += c.getID().getEvent().getCmtLocation().getLatitude();
				latlon[1] += c.getID().getEvent().getCmtLocation().getLongitude();
				count.incrementAndGet();
			});
			if (count.get() == 0)
				continue;
//			System.out.println(i + " " + latlon[0] + " " + latlon[1] + " " + count.intValue());
			HorizontalPosition centerPosition = new HorizontalPosition(latlon[0] / count.intValue(), latlon[1] / count.intValue());
			clusters.stream().filter(c -> c.index == itmp).forEach(c -> {
				c.centerPosition = centerPosition;
			});
		}
		
		return clusters;
	}
	
	public static List<EventCluster> cluster(Set<GlobalCMTID> ids, double dL) {
		List<EventCluster> clusterlist = new ArrayList<>();
		List<EventCluster> tmpclusterlist = new ArrayList<>();
		
		double latmin = 1000;
		double latmax = -1000;
		double lonmin = 1000;
		double lonmax = -1000;
		
		for (GlobalCMTID id : ids) {
			double lat = id.getEvent().getCmtLocation().getLatitude();
			double lon = id.getEvent().getCmtLocation().getLongitude();
			
			if (lat < latmin)
				latmin = lat;
			if (lat > latmax)
				latmax = lat;
			if (lon < lonmin)
				lonmin = lon;
			if (lon > lonmax)
				lonmax = lon;
		}
		
		
		lonmin = lonmin - .1;
		latmin = latmin - .1;
		
		int nlon = (int) ((lonmax - lonmin) / dL) + 2;
		int nlat = (int) ((latmax - latmin) / dL) + 2;
		
		for (GlobalCMTID id : ids) {
			double lat = id.getEvent().getCmtLocation().getLatitude();
			double lon = id.getEvent().getCmtLocation().getLongitude();
			
			int ilon = (int) ((lon - lonmin) / dL);
			int ilat = (int) ((lat - latmin) / dL);
			
//			System.out.println(ilon + " " + ilat);
			
			tmpclusterlist.add(new EventCluster(id, ilon * 10000 + ilat, null, null));
		}
		
		int count = 0;
		for (int ilon = 0; ilon < nlon; ilon++) {
			for (int ilat = 0; ilat < nlat; ilat++) {
				int index = ilon * 10000 + ilat;
				List<EventCluster> tmplist = tmpclusterlist.stream().filter(c -> c.index == index).collect(Collectors.toList());
				if (tmplist.size() > 0) {
					for (EventCluster c : tmplist)
						clusterlist.add(new EventCluster(c.id, count, null, null));
					count++;
				}
			}
		}
		
		
		return clusterlist;
	}
	
	public GlobalCMTID getID() {
		return id;
	}
	
	public int getIndex() {
		return index;
	}
	
	public List<Double> getAzimuthSlices() {
		return azimuthSlices;
	}
	
	public int getNAzimuthSlices() {
		if (azimuthSlices.size() == 0)
			return 0;
		else
			return azimuthSlices.size() + 1;
	}
	
	public double[] getAzimuthBound(int azimuthIndex) {
		double[] azs = new double[2];
		
		if (azimuthIndex == 0) {
			azs[0] = 0;
			azs[1] = azimuthSlices.get(azimuthIndex);
		}
		else if (azimuthIndex == azimuthSlices.size()) {
			azs[0] = azimuthSlices.get(azimuthIndex - 1);
			azs[1] = 360.;
		}
		else if (azimuthIndex > 0 && azimuthIndex < azimuthSlices.size()) {
			azs[0] = azimuthSlices.get(azimuthIndex - 1);
			azs[1] = azimuthSlices.get(azimuthIndex);
		}
		else
			throw new RuntimeException("Azimuth slice out of range " + azimuthIndex + " " + azimuthSlices.size());
		
		return azs;
	}
	
	public int getAzimuthIndex(HorizontalPosition pos) {
		int iaz = -1;
		double azimuth = Math.toDegrees(centerPosition.getAzimuth(pos));
		for (int i = 0; i < getNAzimuthSlices(); i++) {
			double[] bounds = getAzimuthBound(i);
			if (bounds[0] <= azimuth && bounds[1] > azimuth) iaz = i;
		}
		return iaz;
	}
	
	public HorizontalPosition getCenterPosition() {
		return centerPosition;
	}
	
	public static void writeAzimuthSeparation(List<EventCluster> clusters, Path outpath) throws IOException {
		PrintWriter pw = new PrintWriter(outpath.toFile());
		
		AtomicInteger idmax = new AtomicInteger(); 
		clusters.stream().map(c -> c.getIndex()).forEach(i ->  {
			if (idmax.get() < i) idmax.set(i);
		});
		for (int i = 0; i <= idmax.get(); i++) {
			for (EventCluster cluster : clusters) {
				if (cluster.getIndex() == i) {
					HorizontalPosition pos0 = cluster.getCenterPosition();
					for (double az : cluster.getAzimuthSlices()) {
						double lat = SphericalCoords.latFor(pos0.getLatitude(), pos0.getLongitude(), 100., az);
						double lon = SphericalCoords.lonFor(pos0.getLatitude(), pos0.getLongitude(), 100., az);
						double lat65 = SphericalCoords.latFor(pos0.getLatitude(), pos0.getLongitude(), 60./2., az);
						double lon65 = SphericalCoords.lonFor(pos0.getLatitude(), pos0.getLongitude(), 60./2., az);
						double lat85 = SphericalCoords.latFor(pos0.getLatitude(), pos0.getLongitude(), 80./2., az);
						double lon85 = SphericalCoords.lonFor(pos0.getLatitude(), pos0.getLongitude(), 80./2., az);
						HorizontalPosition pos1 = new HorizontalPosition(lat, lon);
						pw.println("> > > >cluster_" + cluster.getIndex());
						pw.println(pos0.getLongitude() + " " + pos0.getLatitude() + " " +
							lon65 + " " + lat65 + " " + "cluster_" + cluster.getIndex());
						pw.println(pos1.getLongitude() + " " + pos1.getLatitude() + " " +
							lon85 + " " + lat85 + " " + "cluster_" + cluster.getIndex());
					}
					continue;
				}
			}
		}
		
		pw.close();
	}
	
	public static void printRecordInformation(List<EventCluster> clusters, Set<TimewindowInformation> timewindows) {
		AtomicInteger idmax = new AtomicInteger(); 
		clusters.stream().map(c -> c.getIndex()).forEach(i ->  {
			if (idmax.get() < i) idmax.set(i);
		});
		for (int i = 0; i < idmax.get(); i++) {
			EventCluster tmpc = null;
			final int ifinal = i;
			try {
				tmpc = clusters.stream().filter(c -> c.getIndex() == ifinal).findFirst().get();
			} catch (Exception e) {
				continue;
			}
			Set<GlobalCMTID> tmpids = clusters.stream().filter(c -> c.getIndex() == ifinal).map(c -> c.getID())
					.collect(Collectors.toSet());
			Set<TimewindowInformation> tmpwindows = timewindows.stream().filter(tw -> tmpids.contains(tw.getGlobalCMTID())).collect(Collectors.toSet());
			for (int iaz = 0; iaz < tmpc.getAzimuthSlices().size(); iaz++) {
				int n = 0;
				for (TimewindowInformation window : tmpwindows) {
					if (window.getAzimuthDegree() >= tmpc.getAzimuthBound(iaz)[0] 
							&& window.getAzimuthDegree() < tmpc.getAzimuthBound(iaz)[1])
						n++;
				}
				
				System.out.println("cluster_" + i + " az_" + iaz + " " + n);
			}
		}
	}
	
	@Override
	public String toString() {
		String azimuthString = "";
		for (int i = 0; i < azimuthSlices.size() - 1; i++)
			azimuthString += String.format("%.4f ", azimuthSlices.get(i));
		if (azimuthSlices.size() > 0)
			azimuthString += String.format("%.4f", azimuthSlices.get(azimuthSlices.size() - 1));
		return id + " " + id.getEvent().getCmtLocation().toHorizontalPosition() + " " + centerPosition + " " + index + " " + azimuthString;
	}
	
}
