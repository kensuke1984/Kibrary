package io.github.kensuke1984.kibrary.selection;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DividePerAzimuth {
	
	private Set<TimewindowInformation> info;
	private HorizontalPosition averageEventPosition;
//	private double averageAzimuth = -1000.0;
	public double[] azimuthRange;
	private Path workPath;
	private boolean rotate = false;
	private double a1, a2;
	
	public DividePerAzimuth(Path timewindowInformationPath, Path workPath, double a1, double a2) {
		try {
			this.info = TimewindowInformationFile.read(timewindowInformationPath);
			setAverageEventPosition();
			setRotation();
			setAzimuthRange();
			this.workPath = workPath;
			this.a1 = a1;
			this.a2 = a2;
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
			this.a1 = 0.19;
			this.a2 = 0.34;
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
		else if (args.length == 3) {
			double a1 = Double.parseDouble(args[1]);
			double a2 = Double.parseDouble(args[2]);
			if (a1 <= 0. || a1 >= 1. || a2 <= 0 || a2 >= 1 || a2 == a1)
				throw new RuntimeException("a1, a2 must be (strictly) greater than 0, (strictly) smaller than 1, and not equal");
			if (a1 > a2) {
				double tmp = a2;
				a1 = tmp;
				a2 = a1;
			}
			dpa = new DividePerAzimuth(infoPath, workPath, a1, a2);
		}
		else
			throw new RuntimeException("Please input either a Path to a timewindow information file, or a path, a1, and a2");
		
		System.err.println("Going with a1, a2 = " + dpa.a1 + ", " + dpa.a2);
		
		List<Set<TimewindowInformation>> threeParts = dpa.divideInThree();
		
		String originalName = infoPath.getFileName().toString();
		originalName = originalName.substring(0, originalName.length() - 4);
		for (int i = 0; i < 3; i++) {
			String name = "";
			switch (i) {
			case 0:
				name = originalName + "-West.dat";
				break;
			case 1:
				name = originalName + "-Middle.dat";
				break;
			case 2:
				name = originalName + "-East.dat";
				break;
			default:
				break;
			}
			Set<TimewindowInformation> onePart = threeParts.get(i);
			Path outputPath = dpa.workPath.resolve(Paths.get(name));
			System.err.println("Write " + onePart.size() + " timewindows in " + name);
			TimewindowInformationFile.write(onePart, outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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
	}
	
	private double unfold(double azimuth) {
		if (0 <= azimuth && azimuth < Math.PI)
			return azimuth + 2 * Math.PI;
		else
			return azimuth;
	}
	
	private List<Set<TimewindowInformation>> divideInThree() {
		List<Set<TimewindowInformation>> threeParts = new ArrayList<>();
		for (int i = 0; i < 3; i++)
			threeParts.add(new HashSet<>());
		
		info.stream().forEach(tw -> {
			double azimuth = averageEventPosition.getAzimuth(tw.getStation().getPosition());
			if (rotate)
				azimuth = unfold(azimuth);
			double ratio = (azimuth - azimuthRange[0]) / (azimuthRange[1] - azimuthRange[0]);
			int i;
			if (ratio < a1)
				i = 0;
			else if (ratio >= a1 && ratio < a2)
				i = 1;
			else
				i = 2;
			Set<TimewindowInformation> tmp = threeParts.get(i);
			tmp.add(tw);
			threeParts.set(i, tmp);
		});
		
		return threeParts;
	}
	
}
