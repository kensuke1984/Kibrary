package io.github.kensuke1984.kibrary.util.statistics;

import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.List;

public class HistogramAzimuth {
	
	public HistogramAzimuth(BasicID[] basicIDs, Set<Station> stationSet, double interval, boolean centered, double minAz, double maxAz) {
		this.interval = interval;
		this.numberOfRecords = new int[(int) (360 / interval)];
		
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
		
		try (Stream<BasicID> idStream = Stream.of(basicIDs);) {
			idStream.filter(id -> id.getWaveformType().equals(WaveformType.OBS))
			.forEach(id -> {
				Station station = stationSet.stream().filter(s->s.equals(id.getStation())).findAny().get();
				
				HorizontalPosition staLoc = station.getPosition();
				double az = 0;
				if (centered) {
					az = 	this.averageLoc.
								getAzimuth(staLoc)*180/Math.PI;
				}
				else
					az = id.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(staLoc)*180/Math.PI;
				
				this.numberOfRecords[(int) (az / interval)]++;
				this.mean += (int) (az / interval);
		});
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		this.maxValue = getMax();
		this.mean /= basicIDs.length;
		this.medianValue = basicIDs.length/2.;
	}
	
	public HistogramAzimuth(BasicID[] basicIDs, Set<Station> stationSet, double interval, boolean centered) {
		this(basicIDs, stationSet, interval, centered, 0, 360);
	}
	
	
	public static void main(String[] args) throws IOException {
		Path root = Paths.get(".");
		Path srcID = root.resolve(args[0]);
		Phases phases_ = null;
		if (args.length == 2)
			phases_ = new Phases(args[1]);
		final Phases phases = phases_;
		BasicID[] basicIDs = BasicIDFile.readBasicIDFile(srcID);
		Path outPath = root.resolve("azimuthHistogram.txt");
		Path scriptPath = root.resolve("azimuthHistogram.plt");
		
		Set<Station> stationSet = new HashSet<>();
		
		for (int i=0; i < basicIDs.length; i++) {
				stationSet.add(basicIDs[i].getStation());
		}
		
		List<BasicID> idList = new ArrayList<>();
		if (phases != null)
			idList = Stream.of(basicIDs).filter(id -> id.getWaveformType().equals(WaveformType.OBS))
				.filter(id -> new Phases(id.getPhases()).equals(phases)).collect(Collectors.toList());
		else
			idList = Stream.of(basicIDs).filter(id -> id.getWaveformType().equals(WaveformType.OBS))
				.collect(Collectors.toList());
		BasicID[] usedIds = idList.toArray(new BasicID[idList.size()]);
		
		HistogramAzimuth histogram = new HistogramAzimuth(usedIds, stationSet, 5., false);
		
		histogram.printHistogram(outPath);
		histogram.createScript(scriptPath);
	}
	
	public void printHistogram(Path outPath) {
		try {
			Files.deleteIfExists(outPath);
			Files.createFile(outPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < numberOfRecords.length; i++) {
			try {
				Files.write(outPath
						, String.format("%.2f %d\n", i*interval, numberOfRecords[i]).getBytes()
						, StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void createScript(Path outpath) throws IOException {
		Files.deleteIfExists(outpath);
		Files.createFile(outpath);
		Files.write(outpath, String.join("\n"
				, "set term postscript enhanced color font 'Helvetica,14'"
				, "set xlabel 'Azimtuh (deg)'"
				, "set ylabel 'Number of records'"
				, "set xrange [0:360]"
				, "set xtics 20 nomirror"
				, "set ytics nomirror"
				, "set style fill pattern 1 border lc 'red'"
				, "set sample 11"
				, "#set logscale y 10"
				, "set output 'azimuthHistogram.ps'"
				, "plot 'azimuthHistogram.txt' u ($1+2.5):2 w boxes lw 2.5 lc 'red' notitle"
				).getBytes(), StandardOpenOption.APPEND);
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
	
	private double interval;
	private int[] numberOfRecords;
	private int maxValue;
	private Location averageLoc;
	private double mean;
	private double medianValue;
}


