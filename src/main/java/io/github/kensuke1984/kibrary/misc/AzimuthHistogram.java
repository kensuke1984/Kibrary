/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;

/**
 * @version 0.0.1
 * @since 2016/02/09
 * @author Yuki
 *
 */
public class AzimuthHistogram {
	
	public AzimuthHistogram(BasicID[] basicIDs, Set<Station> stationSet, int interval, boolean centered, double minAZ, double maxAZ) {
		this.interval = interval;
		this.numberOfRecords = new int[360 / interval];
		
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
				if (stationSet.stream().filter(sta -> sta.equals(id.getStation())).count() == 0)
					System.out.println("Error: station " + id.getStation().getName() 
							+ " " + id.getStation().getNetwork() + " not found");
				double azimuth = id.getGlobalCMTID().getEvent().getCmtLocation().
						getAzimuth(stationSet.stream().filter(sta -> sta.equals(id.getStation())).findAny().get().getPosition())
						*180/Math.PI;
				System.out.println(stationSet.stream().filter(sta -> sta.equals(id.getStation())).findAny().get().getPosition()+" "+id.getGlobalCMTID().getEvent().getCmtLocation()+" "+azimuth);
				if (azimuth < minAZ)
					return false;
				else if (azimuth > maxAZ)
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
							if ( stations.get(0).getPosition().getAzimuth(stations.get(1).getPosition())*180/Math.PI > 0.5) {
								System.out.println(String.join(" ", "WARNING!! : station", stations.get(0).getName(), stations.get(0).getNetwork(), stations.get(0).getPosition().toString()
										, "and station", stations.get(1).getName(), stations.get(1).getNetwork(), stations.get(1).getPosition().toString(), "are more than 0.5 degrees apart"));
								
								skip = true;
							}
						}
						
					}
					
					if (!skip) {
					HorizontalPosition staLoc = station.getPosition();
					double az = 0;
					if (centered) {
						az = (new Location(this.averageLoc.getLatitude(),
								id.getGlobalCMTID().getEvent().getCmtLocation().getLongitude(),
								id.getGlobalCMTID().getEvent().getCmtLocation().getR()))
								.getAzimuth(staLoc)*180/Math.PI;
					}
					else
						az = id.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(staLoc)*180/Math.PI;
					this.numberOfRecords[(int) (az / interval)]++;
					this.mean += (int) (az / interval);
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
	
	public AzimuthHistogram(BasicID[] basicIDs, Set<Station> stationSet, int interval, boolean centered) {
		new AzimuthHistogram(basicIDs, stationSet, interval, centered, -180, 180);
	}
	
	
	public static void main(String[] args) throws IOException {
		Path srcID = Paths.get(args[0]);
		BasicID[] basicIDs = BasicIDFile.readBasicIDFile(srcID);
		Set<Station> stationSet = StationInformationFile.read(Paths.get(args[1]));
		Path outPath = Paths.get(args[2]);
		AzimuthHistogram histogram = new AzimuthHistogram(basicIDs, stationSet, 5, false, 0., 360.);
		Files.deleteIfExists(outPath);
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
				Files.write(outPath, (String.valueOf(i*interval) + " " + String.valueOf(numberOfRecords[Math.abs(i)]) + "\n").getBytes(),
						StandardOpenOption.WRITE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public int getValue(double azimuth) {
		return numberOfRecords[(int) (azimuth / interval)];
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
