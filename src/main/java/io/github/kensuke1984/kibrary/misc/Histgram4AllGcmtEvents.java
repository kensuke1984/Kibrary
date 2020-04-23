package io.github.kensuke1984.kibrary.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Set;

import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTSearch;

public class Histgram4AllGcmtEvents {

	private int interval;
	private int[] numberOfEvents;
	private double mean;

	public static void main(String[] args) {
		// TODO 自動生成されたメソッド・スタブ
		int interval = Integer.parseInt(args[0]);
//		String locationName = args[1];
		Path outPath = Paths.get(args[1]);
//		Histgram4AllGcmtEvents histogram = new Histgram4AllGcmtEvents(interval, locationName);
		Histgram4AllGcmtEvents histogram = new Histgram4AllGcmtEvents(interval);
		histogram.printHistogram(outPath);
	}


	private Histgram4AllGcmtEvents(int interval, String locationName) {
		this.interval = interval;
		this.numberOfEvents = new int[140 / interval];
		GlobalCMTSearch search = new GlobalCMTSearch(LocalDate.of(1976, 1, 1), LocalDate.of(2018, 12, 31));	//年月日の指定
		Set<GlobalCMTID> gcmtid = search.search();
		for (GlobalCMTID id : gcmtid) {
			double depth = 0;
			if (id.getEvent().getGeographicalLocationName().equals(locationName)) {
				depth = 6371. - id.getEvent().getCmtLocation().getR();
				this.numberOfEvents[(int) (depth / interval)]++;
				this.mean += (int) (depth / interval);
			}
		}
	}

	private Histgram4AllGcmtEvents(int interval) {
		this.interval = interval;
		this.numberOfEvents = new int[140 / interval];
		GlobalCMTSearch search = new GlobalCMTSearch(LocalDate.of(1976, 1, 1), LocalDate.of(2018, 12, 31));	//年月日の指定
		search.setLatitudeRange(-40, -10);	//緯度範囲の指定
		search.setLongitudeRange(170, 190);	//経度範囲の指定
		Set<GlobalCMTID> gcmtid = search.search();
		for (GlobalCMTID id : gcmtid) {
			double depth = 0;
			depth = 6371. - id.getEvent().getCmtLocation().getR();
			this.numberOfEvents[(int) (depth / interval)]++;
			this.mean += (int) (depth / interval);
		}
	}

	public void printHistogram(Path outPath) {
		try {
			Files.write(outPath, "".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < numberOfEvents.length; i++) {
			try {
				Files.write(outPath, (String.valueOf(i*interval) + " " + String.valueOf(numberOfEvents[i]) + "\n").getBytes(),
						StandardOpenOption.WRITE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
