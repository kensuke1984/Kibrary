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

public class DividePerAzimuth {
	
	private Set<TimewindowInformation> info;
	private HorizontalPosition averageEventPosition;
//	private double averageAzimuth = -1000.0;
	public double[] azimuthRange;
	private Path workPath;
	private boolean rotate = false;
	private int nSlices;
	
	public DividePerAzimuth(Path timewindowInformationPath, Path workPath, int nSlices) {
		try {
			this.info = TimewindowInformationFile.read(timewindowInformationPath);
			setAverageEventPosition();
			setRotation();
			setAzimuthRange();
			this.workPath = workPath;
			this.nSlices = nSlices;
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
		
//		List<Double> azimuths = new ArrayList<>();
//		azimuths.add(314.5);
//		azimuths.add(326.5);
//		azimuths.add(334.5);
//		azimuths.add(348.5);
//		azimuths.add(374.6);
		
		
		List<Double> azimuths = new ArrayList<>();
//		azimuths.add(337.716);
//		azimuths.add(349.716);
		azimuths.add(329.);
		azimuths.add(344.);
		
		List<Set<TimewindowInformation>> slices = dpa.divide(azimuths);
		
		outpath = Paths.get("azimuthSlices.inf");
		dpa.writeAzimuthSeparation(outpath, azimuths);
		
		//
		for (Set<TimewindowInformation> infos : slices) {
			if (infos.size() == 0)
				continue;
			
			double avgLat = 0;
			double avgLon = 0;
			for (TimewindowInformation tw : infos) {
				avgLat += tw.getStation().getPosition().getLatitude();
				avgLon += tw.getStation().getPosition().getLongitude();
			}
			avgLat /= infos.size();
			avgLon /= infos.size();
			HorizontalPosition hp = new HorizontalPosition(avgLat, avgLon);
			System.out.println(dpa.averageEventPosition + " : " + hp);
		}
		//
		
		String originalName = infoPath.getFileName().toString();
		originalName = originalName.substring(0, originalName.length() - 4);
		for (int i = 0; i < dpa.nSlices; i++) {
			String name = "";
			name = originalName + "-s" + i + ".dat";
			Set<TimewindowInformation> onePart = slices.get(i);
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
	
	private void writeAzimuthGrid(Path outpath, double dAz) throws IOException {
		System.out.println("Print azimth grid with increment " + dAz);
		int n = (int) ((azimuthRange[1] - azimuthRange[0]) * 180. / Math.PI / dAz);
		
		Files.deleteIfExists(outpath);
		Files.createFile(outpath);
		
		for (int i = 0; i <= n+1; i++) {
			double azimuth = azimuthRange[0] * 180 / Math.PI + i * dAz;
			HorizontalPosition endPosition = averageEventPosition.fromAzimuth(azimuth, 110.);
//			System.out.println(azimuth + " " + (averageEventPosition.getAzimuth(endPosition) * 180 / Math.PI));
			Files.write(outpath, (averageEventPosition + " " + endPosition + " " + azimuth + "\n").getBytes(), StandardOpenOption.APPEND);
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
