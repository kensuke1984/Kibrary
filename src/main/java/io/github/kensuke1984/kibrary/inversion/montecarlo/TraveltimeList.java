package io.github.kensuke1984.kibrary.inversion.montecarlo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.external.TauPTimeReader;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

/**
 * @author Kensuke Konishi 
 * @version 0.0.2
 */
class TraveltimeList {

	private String[] stations;
	private GlobalCMTID[] ids;
	private double[] travelTimes;

	private Path listPath;

	TraveltimeList(Path listPath) {
		this.listPath = listPath;
		if (Files.exists(listPath))
			read(listPath);
	}

	void add(SACData sacFile, Phase phase) {
		double eventR = 6371 - sacFile.getValue(SACHeaderEnum.EVDP);
		double epicentralDistance = sacFile.getValue(SACHeaderEnum.GCARC);
		double time = TauPTimeReader.getTauPPhase(eventR, epicentralDistance, phase).iterator().next().getTravelTime();
		add(sacFile, time);
	}

	private void add(SACData sacFile, double time) {
		String station = Station.of(sacFile).getStationName();
		GlobalCMTID id = new GlobalCMTID(sacFile.getSACString(SACHeaderEnum.KEVNM));
		add(station, id, time);
	}

	private void add(String station, GlobalCMTID id, double time) {
		try {
			Files.write(listPath, Arrays.asList(station + " " + id.toString() + " " + time), StandardOpenOption.APPEND,
					StandardOpenOption.CREATE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void read(Path listPath) {
		try {
			List<String> lines = Files.readAllLines(listPath);
			stations = new String[lines.size()];
			ids = new GlobalCMTID[lines.size()];
			travelTimes = new double[lines.size()];
			for (int i = 0; i < lines.size(); i++) {
				String[] parts = lines.get(i).split("\\s+");
				stations[i] = parts[0];
				ids[i] = new GlobalCMTID(parts[1]);
				travelTimes[i] = Double.parseDouble(parts[2]);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(listPath + "is invalid.");
		}
	}

	double getTime(String station, GlobalCMTID id) {
		for (int i = 0; i < stations.length; i++)
			if (stations[i].equals(station) && ids[i].equals(id))
				return travelTimes[i];
		throw new RuntimeException("no time");
	}

}
