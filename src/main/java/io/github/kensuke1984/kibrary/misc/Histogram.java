package io.github.kensuke1984.kibrary.misc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.List;

import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;

public class Histogram {
	
	public Histogram(BasicID[] basicIDs, Set<Station> stationSet, int interval, boolean centered, double minED, double maxED) {
		this.interval = interval;
		this.numberOfRecords = new int[140 / interval];
		
		double tmpLat = 0;
		double tmpLon = 0;
		if (centered) {
			for (BasicID id : basicIDs) {
				tmpLat += id.getGlobalCMTID().getEvent().getCmtLocation().getLatitude();
				tmpLon += id.getGlobalCMTID().getEvent().getCmtLocation().getLongitude();
			}
			tmpLat /= basicIDs.length;
			tmpLon /= basicIDs.length;
		}
		else {
			tmpLat = 0;
			tmpLon = 0;
		}
		this.averageLoc = new Location(tmpLat, tmpLon, 0.);
		
		Set<BasicID> unique = new HashSet<>();
		try (Stream<BasicID> idStream = Stream.of(basicIDs);) {
			idStream.filter(id -> id.getWaveformType().equals(WaveformType.SYN))
			.filter(id -> { 
//				System.out.println(id);
				if (stationSet.stream().filter(sta -> sta.equals(id.getStation())).count() == 0)
					System.out.println("Error: station " + id.getStation().getName() 
							+ " " + id.getStation().getNetwork() + " not found"+" "+ id.getGlobalCMTID());
//				if (id.getStation().getNetwork() == "TA45")
//					System.out.println(id.getGlobalCMTID());
//				double distance = id.getGlobalCMTID().getEvent().getCmtLocation().
//						getEpicentralDistance(stationSet.stream()
//								.filter(sta -> sta.equals(id.getStation()))
//								.filter(sta -> sta.equals(id.getStation()))
//								.findAny().get().getPosition())*180/Math.PI;
				double distance = Math.toDegrees(id.getGlobalCMTID().getEvent().getCmtLocation().
						getEpicentralDistance(stationSet.stream()
								.filter(sta -> sta.equals(id.getStation()))
								.filter(sta -> sta.equals(id.getStation()))
								.findAny().get().getPosition()));
				System.out.println(id+" "+distance);
				if (distance < minED)
					return false;
				else if (distance > maxED)
					return false;
				else
					return true;
			})
			.forEach(id -> {
				boolean skip = false;
				if (unique.stream().filter(u -> u.getStation().equals(id.getStation()) && u.getGlobalCMTID().equals(id.getGlobalCMTID())).count() == 0) {
					unique.add(id);
					Station station = stationSet.stream().filter(s->s.getName().equals(id.getStation().toString())).findAny().get();
					if (stationSet.stream().filter(s->s.getName().equals(id.getStation().toString())).count() > 1) {
						List<Station> stations = new ArrayList<>();
						stationSet.stream().filter(s->s.getName().equals(id.getStation().toString())).forEach(sta -> { 
							stations.add(sta);
						});
						if (stations.size() == 2) {
							if ( stations.get(0).getPosition().getEpicentralDistance(stations.get(1).getPosition())*180/Math.PI > 0.5) {
								System.out.println(String.join(" ", "WARNING!! : station", stations.get(0).getName(), stations.get(0).getNetwork(), stations.get(0).getPosition().toString()
										, "and station", stations.get(1).getName(), stations.get(1).getNetwork(), stations.get(1).getPosition().toString(), "are more than 0.5 degrees apart"));
								skip = true;
							}
						}
					}
					if (!skip) {
					HorizontalPosition staLoc = station.getPosition();
					double ed = 0;
					if (centered) {
						ed = (new Location(this.averageLoc.getLatitude(),
								id.getGlobalCMTID().getEvent().getCmtLocation().getLongitude(),
								id.getGlobalCMTID().getEvent().getCmtLocation().getR()))
								.getEpicentralDistance(staLoc)*180/Math.PI;
					}
					else
						ed = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(staLoc)*180/Math.PI;
					this.numberOfRecords[(int) (ed / interval)]++;
					this.mean += (int) (ed / interval);
					}
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		this.maxValue = getMax();
		this.mean /= basicIDs.length;
		this.medianValue = basicIDs.length/2.;
	}
	
	public Histogram(BasicID[] basicIDs, Set<Station> stationSet, int interval, boolean centered) {
		new Histogram(basicIDs, stationSet, interval, centered, 0, 360);
	}
	
	
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.err.println("usage: path to waveID, path to station.inf, path to output.txt");
			throw new IllegalArgumentException();
		}
		Path srcID = Paths.get(args[0]);
		BasicID[] basicIDs = BasicIDFile.readBasicIDFile(srcID);
		Set<Station> stationSet = StationInformationFile.read(Paths.get(args[1]));
		Path outPath = Paths.get(args[2]);
		Histogram histogram = new Histogram(basicIDs, stationSet, 5, false, 60., 105.);
		
		histogram.printHistogram(outPath);
	}
	
	public void printHistogram(Path outPath) {
		try {
			Files.write(outPath, "".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < numberOfRecords.length; i++) {
			try {
				Files.write(outPath, (String.valueOf(i*interval) + " " + String.valueOf(numberOfRecords[i]) + "\n").getBytes(),
						StandardOpenOption.WRITE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public int getValue(double epicentralDistance) {
		return numberOfRecords[(int) (epicentralDistance / interval)];
	}
	
//	public int getValueSmoothed(double epicentralDistance, double maxRatio) {

//	}
	
	public Location getAverageLoc() {
		return averageLoc;
	}
	
	public int getMaxValue() {
		return maxValue;
	}
	
	public int getClosestAboveValue(double y) {
		int value = maxValue;
		for (int n : numberOfRecords) {
			if (n >= y && n < value)
				value = n;
		}
		return value;
	}
	
	public double getMedianValue() {
		return medianValue;
	}
	
	private int getMax() {
		int max = 0;
		for (int value : numberOfRecords) {
			if (max < value)
				max = value;
		}
		return max;
	}
	
	private int interval;
	private int[] numberOfRecords;
	private int maxValue;
	private Location averageLoc;
	private double mean;
	private double medianValue;
}
