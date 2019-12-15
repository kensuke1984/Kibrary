package io.github.kensuke1984.kibrary.selection;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import edu.sc.seis.TauP.TimeDist;

public class DividePerAzimuth {
	
	private Set<TimewindowInformation> info;
	private HorizontalPosition averageEventPosition;
//	private double averageAzimuth = -1000.0;
	public double[] azimuthRange;
	private Path workPath;
	private boolean rotate = true;
	private int nSlices;
	
	public DividePerAzimuth(Path timewindowInformationPath, Path workPath, int nSlices) {
		try {
			this.info = TimewindowInformationFile.read(timewindowInformationPath);
			setAverageEventPosition();
			setRotation();
			setAzimuthRange();
			this.workPath = workPath;
			this.nSlices = nSlices;
			this.bottomingPointInRegion = new ArrayList<>();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public DividePerAzimuth(Path timewindowInformationPath, Path workPath) {
		try {
			this.info = TimewindowInformationFile.read(timewindowInformationPath);
			setAverageEventPosition();
			setRotation();
			setAzimuthRange();
			this.workPath = workPath;
			this.nSlices = 1;
			this.bottomingPointInRegion = new ArrayList<>();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException {
		Path workPath = Paths.get(".");
		if (args.length == 0)
			throw new RuntimeException("Please input either a Path to a timewindow information file, or a path, a1, and a2");
		
		Path infoPath = workPath.resolve(args[0].trim());
		DividePerAzimuth dpa = null;
		if (args.length == 1)
			dpa = new DividePerAzimuth(infoPath, workPath);
		else if (args.length == 2) {
			int nSlices = Integer.parseInt(args[1]);
			if (nSlices < 1)
				throw new RuntimeException("nSlices must be >1");
			dpa = new DividePerAzimuth(infoPath, workPath, nSlices);
		}
		else
			throw new RuntimeException("Please input either a Path to a timewindow information file, or a path, a1, and a2");
		
		System.err.println("Going with nSlices = " + dpa.nSlices);
		
		Path outpath = Paths.get("azimuthGrid.inf");
		dpa.writeAzimuthGrid(outpath, 2.);
		
		Path outpathDist = Paths.get("distanceGrid.inf");
		double dEd = 2;
		dpa.writeDistanceGrid(outpathDist, dEd);
		
		//regions
		List<Double[]> regions = new ArrayList<>();
//		regions.add(new Double[] {310., 321., 20., 50.});
//		regions.add(new Double[] {321., 338., 20., 33.});
//		regions.add(new Double[] {321., 338., 33., 40.});
//		regions.add(new Double[] {321., 338., 40., 50.});
//		regions.add(new Double[] {338., 347., 20., 26.});
//		regions.add(new Double[] {338., 347., 26., 50.});
//		regions.add(new Double[] {347., 10., 20., 50.});
		
		

		
//		List<Double> azimuths = new ArrayList<>();
//		azimuths.add(314.5);
//		azimuths.add(326.5);
//		azimuths.add(334.5);
//		azimuths.add(348.5);
//		azimuths.add(374.6);
		
		
		List<Double> azimuths = new ArrayList<>();
		azimuths.add(321.);
		azimuths.add(338.);
		azimuths.add(347.);
		
//		List<Set<TimewindowInformation>> slices = dpa.divide(azimuths);
////		List<Set<TimewindowInformation>> slices = dpa.divide();
//		
////		List<Double> azimuths = dpa.getAzimuthList();
//		
//		System.out.println(azimuths.size());
		outpath = Paths.get("azimuthSlices.inf");
		dpa.writeAzimuthSeparation(outpath, azimuths);
//		
//		//
//		for (Set<TimewindowInformation> infos : slices) {
//			if (infos.size() == 0)
//				continue;
//			
//			double avgLat = 0;
//			double avgLon = 0;
//			for (TimewindowInformation tw : infos) {
//				avgLat += tw.getStation().getPosition().getLatitude();
//				avgLon += tw.getStation().getPosition().getLongitude();
//			}
//			avgLat /= infos.size();
//			avgLon /= infos.size();
//			HorizontalPosition hp = new HorizontalPosition(avgLat, avgLon);
//			System.out.println(dpa.averageEventPosition + " : " + hp);
//		}
		//
		
		List<Set<TimewindowInformation>> windowsInRegion = dpa.divide2D(regions);
		List<Set<HorizontalPosition>> bottomingPoints = dpa.getBottomingPointInRegion();
		Path outpathBottom = Paths.get("bottomingPointsRegions.inf");
		Files.deleteIfExists(outpathBottom);
		Files.createFile(outpathBottom);
		String[] colors = new String[] {"purple", "blue", "cyan", "green", "red", "brown", "pink"};
		for (int i = 0; i < regions.size(); i++) {
			Set<HorizontalPosition> points = bottomingPoints.get(i);
			for (HorizontalPosition p : points)
				Files.write(outpathBottom, (p.getLongitude() + " " + p.getLatitude() + " " + (i) + "\n").getBytes()
						, StandardOpenOption.APPEND);
		}
		
//		String originalName = infoPath.getFileName().toString();
//		originalName = originalName.substring(0, originalName.length() - 4);
//		for (int i = 0; i < dpa.nSlices; i++) {
//			String name = "";
//			name = originalName + "-s" + i + ".dat";
//			Set<TimewindowInformation> onePart = slices.get(i);
//			if (onePart.size() > 0) {
//				Path outputPath = dpa.workPath.resolve(Paths.get(name));
//				System.err.println("Write " + onePart.size() + " timewindows in " + name);
//				TimewindowInformationFile.write(onePart, outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//			}
//		}
		
		String originalName = infoPath.getFileName().toString();
		originalName = originalName.substring(0, originalName.length() - 4);
		for (int i = 0; i < regions.size(); i++) {
			String name = "";
			name = originalName + "-s" + i + ".dat";
			Set<TimewindowInformation> onePart = windowsInRegion.get(i);
			if (onePart.size() > 0) {
				Path outputPath = dpa.workPath.resolve(Paths.get(name));
				System.err.println("Write " + onePart.size() + " timewindows in " + name);
				TimewindowInformationFile.write(onePart, outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			}
		}
	}
	
	private void setRotation() {
		if (averageEventPosition == null)
			setAverageEventPosition();
		
		Set<Station> stations = info.stream().map(tw -> tw.getStation())
				.collect(Collectors.toSet());
		
		double[] minMax = new double[] {Double.MAX_VALUE, Double.MIN_VALUE};
		stations.stream().forEach(station -> {
			double azimuth = averageEventPosition.getAzimuth(station.getPosition());
			if (azimuth < minMax[0])
				minMax[0] = azimuth;
			if (azimuth > minMax[1])
				minMax[1] = azimuth;
		});
		
		if (minMax[1] - minMax[0] >= Math.PI)
			rotate = true;
		else
			rotate = false;
	}
	
	private void setAverageEventPosition() {
		double[] latLon = new double[] {0., 0.};
		Set<GlobalCMTID> events = info.stream().map(tw -> tw.getGlobalCMTID())
				.collect(Collectors.toSet());
		events.stream().forEach(id -> {
			Location loc = id.getEvent().getCmtLocation();
			latLon[0] += loc.getLatitude();
			latLon[1] += loc.getLongitude(); 
		});
		latLon[0] /= events.size();
		latLon[1] /= events.size();
		
		this.averageEventPosition = new HorizontalPosition(latLon[0], latLon[1]);
	}
	
	private List<Double> azimuthList;
	
	private void setAzimuthRange() {
		azimuthRange = new double[] {Double.MAX_VALUE, Double.MIN_VALUE};
		
		Set<Station> stations = info.stream().map(tw -> tw.getStation())
				.collect(Collectors.toSet());
		stations.stream().forEach(station -> {
			double azimuth = averageEventPosition.getAzimuth(station.getPosition());
			if (rotate)
				azimuth = unfold(azimuth);
			
			if (azimuth < azimuthRange[0])
				azimuthRange[0] = azimuth;
			if (azimuth > azimuthRange[1])
				azimuthRange[1] = azimuth;
		});
		
		azimuthRange[0] = Math.toRadians(311);
		azimuthRange[1] = Math.toRadians(363);
		
		azimuthList = new ArrayList<>();
		for (int i = 0; i < nSlices - 1; i++)
			azimuthList.add(azimuthRange[0] + (i + 1) * (azimuthRange[1] - azimuthRange[0]) / nSlices);
	}
	
	public List<Double> getAzimuthList() {
		return azimuthList;
	}
	
	private double unfold(double azimuth) {
		if (0 <= azimuth && azimuth < Math.PI)
			return azimuth + 2 * Math.PI;
		else
			return azimuth;
	}
	
	private double fold(double azimuth) {
		if (azimuth > 2 * Math.PI)
			return azimuth - 2 * Math.PI;
		else
			return azimuth;
	}
	
	private List<Set<TimewindowInformation>> divide() {
		List<Set<TimewindowInformation>> slices = new ArrayList<>();
		for (int i = 0; i < nSlices; i++)
			slices.add(new HashSet<>());
		
		info.stream().forEach(tw -> {
			double azimuth = averageEventPosition.getAzimuth(tw.getStation().getPosition());
			if (rotate)
				azimuth = unfold(azimuth);
			double ratio = (azimuth - azimuthRange[0]) / (azimuthRange[1] - azimuthRange[0]);
			int i = (int) (ratio * nSlices);
			if (i == nSlices)
				i -= 1;
			Set<TimewindowInformation> tmp = slices.get(i);
			tmp.add(tw);
			slices.set(i, tmp);
		});
		
		azimuthList = new ArrayList<>();
		for (int i = 0; i < nSlices ; i++)
			azimuthList.add(Math.toDegrees(azimuthRange[0] + i * (azimuthRange[1] - azimuthRange[0]) / nSlices));
		
		return slices;
	}
	
	private List<Set<TimewindowInformation>> divide(List<Double> azimuths) {
		List<Set<TimewindowInformation>> slices = new ArrayList<>();
		
		nSlices = azimuths.size() + 1;
		for (int i = 0; i < nSlices; i++)
			slices.add(new HashSet<>());
		
		info.stream().forEach(tw -> {
			double azimuth = averageEventPosition.getAzimuth(tw.getStation().getPosition());
			if (rotate)
				azimuth = unfold(azimuth);
			azimuth *= 180. / Math.PI;
			int i = 0;
			for (int j = 0; j < azimuths.size() - 1; j++) {
				if (azimuth < azimuths.get(j + 1) && azimuth >= azimuths.get(j))
					i = j + 1;
			}
			if (azimuth < azimuths.get(0))
				i = 0;
			if (azimuth > azimuths.get(azimuths.size() - 1))
				i = azimuths.size();
			
			Set<TimewindowInformation> tmp = slices.get(i);
			tmp.add(tw);
			slices.set(i, tmp);
		});
		
		return slices;
	}
	
	private List<Set<HorizontalPosition>> bottomingPointInRegion;
	
	private List<Set<TimewindowInformation>> divide2D(List<Double[]> regions) {
		List<Set<TimewindowInformation>> datasets = new ArrayList<>();
		
		int nRegions = regions.size();
		for (int i = 0; i < nRegions; i++) {
			datasets.add(new HashSet<>());
			bottomingPointInRegion.add(new HashSet<>());
		}
		
		try {
			TauP_Time timetool = new TauP_Time("prem");
			timetool.parsePhaseList("ScS");
			
			for (TimewindowInformation tw : info) {
				HorizontalPosition evtLoc = tw.getGlobalCMTID().getEvent().getCmtLocation();
				double azimuth = tw.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(tw.getStation().getPosition());
				double distance = tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition());
				
				//
				timetool.setSourceDepth(6371. - tw.getGlobalCMTID().getEvent().getCmtLocation().getR());
				timetool.calculate(distance);
				Arrival arrivalScS = timetool.getArrival(0);
				
				TimeDist[] pierces = arrivalScS.getPierce();
				double pierceDist = 0;
				for (TimeDist p : pierces) {
					if (Math.abs(p.getDepth() - 2891.) < 1) {
						pierceDist = p.getDistDeg();
						break;
					}
				}
				
				distance = pierceDist;
				
				distance *= 180 / Math.PI;
				azimuth *= 180 / Math.PI;
				
				double lat = SphericalCoords.latFor( evtLoc.getLatitude(),  evtLoc.getLongitude(), distance, azimuth);
				double lon = SphericalCoords.lonFor( evtLoc.getLatitude(),  evtLoc.getLongitude(), distance, azimuth);
				HorizontalPosition bottomingPosition = new HorizontalPosition(lat, lon);
				
				azimuth = averageEventPosition.getAzimuth(bottomingPosition);
				distance = averageEventPosition.getEpicentralDistance(bottomingPosition);
				
				if (rotate)
					azimuth = unfold(azimuth);
				distance *= 180 / Math.PI;
				int i = -1;
				for (int j = 0; j < nRegions; j++) {
					Double[] reg = regions.get(j);
					if (azimuth >= unfold(reg[0] * Math.PI / 180.) && azimuth < unfold(reg[1] * Math.PI / 180.) 
							&& distance >= reg[2] && distance < reg[3]) {
						i = j;
						break;
					}
				}
				if (i == -1)
					continue;
				
				Set<TimewindowInformation> tmp = datasets.get(i);
				tmp.add(tw);
				datasets.set(i, tmp);
				
				Set<HorizontalPosition> tmpBottom = bottomingPointInRegion.get(i);
				tmpBottom.add(bottomingPosition);
				bottomingPointInRegion.set(i, tmpBottom);
			}
		} catch (TauModelException e) {
			e.printStackTrace();
		}
		
		return datasets;
	}
	
	public List<Set<HorizontalPosition>> getBottomingPointInRegion() {
		return bottomingPointInRegion;
	}
	
	private void writeAzimuthGrid(Path outpath, double dAz) throws IOException {
		System.out.println("Print azimth grid with increment " + dAz);
		int n = (int) ((azimuthRange[1] - azimuthRange[0]) * 180. / Math.PI / dAz);
		
		Files.deleteIfExists(outpath);
		Files.createFile(outpath);
		
		for (int i = 0; i <= n+1; i++) {
			double azimuth = fold(azimuthRange[0] + i * Math.toRadians(dAz));
			azimuth = Math.toDegrees(azimuth);
			double lat = SphericalCoords.latFor(averageEventPosition.getLatitude(), averageEventPosition.getLongitude(), 70, azimuth);
			double lon = SphericalCoords.lonFor(averageEventPosition.getLatitude(), averageEventPosition.getLongitude(), 70, azimuth);
			HorizontalPosition endPosition = new HorizontalPosition(lat, lon);
//					averageEventPosition.fromAzimuth(azimuth, 70.);
			System.out.println(azimuth + " " + (averageEventPosition.getAzimuth(endPosition) * 180 / Math.PI));
			Files.write(outpath, (averageEventPosition + " " + endPosition + " " + azimuth + "\n").getBytes(), StandardOpenOption.APPEND);
		}
	}
	
	private void writeDistanceGrid(Path outpath, double dEd) throws IOException {
//		System.out.println("Print azimth grid with increment " + dAz);
		int n = (int) ((50 - 10) / dEd); 
		
		Files.deleteIfExists(outpath);
		Files.createFile(outpath);
		
		for (int i = 0; i <= n+1; i++) {
			double distance = 10 + i * dEd;
			Files.write(outpath, ">\n".getBytes(), StandardOpenOption.APPEND);
			for (int j = 0; j < 360; j++) {
				double azimuth = j;
				double lat = SphericalCoords.latFor(averageEventPosition.getLatitude(), averageEventPosition.getLongitude(), distance, azimuth);
				double lon = SphericalCoords.lonFor(averageEventPosition.getLatitude(), averageEventPosition.getLongitude(), distance, azimuth);
				HorizontalPosition endPosition = new HorizontalPosition(lat, lon);
//				System.out.println(averageEventPosition.getEpicentralDistance(endPosition) * 180 / Math.PI + " " + distance);
				Files.write(outpath, (endPosition.getLongitude() + " " + endPosition.getLatitude() + "\n").getBytes(), StandardOpenOption.APPEND);
			}
		}
	}
	
	private void writeAzimuthSeparation(Path outpath, List<Double> azimuths) throws IOException {
		Files.deleteIfExists(outpath);
		Files.createFile(outpath);
		
		for (int i = 0; i < azimuths.size(); i++) {
			double azimuth = azimuths.get(i);
			if (azimuth >= 360.)
				azimuth = azimuth - 360.;
			HorizontalPosition endPosition = averageEventPosition.fromAzimuth(azimuth, 90.);
//			System.out.println(azimuth + " " + (averageEventPosition.getAzimuth(endPosition) * 180 / Math.PI));
			Files.write(outpath, (averageEventPosition + " " + endPosition + "\n").getBytes(), StandardOpenOption.APPEND);
		}
	}
}
