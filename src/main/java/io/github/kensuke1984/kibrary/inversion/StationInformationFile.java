package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import org.apache.commons.io.input.CloseShieldInputStream;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * File containing information of stations .<br>
 * Each line: station name, station network, latitude, longitude.
 *
 * @author Kensuke Konishi
 * @version 0.2.0.4
 */
public final class StationInformationFile {

	private StationInformationFile() {
	}

	/**
	 * @param stationSet Set of station information
	 * @param outPath    of write file
	 * @param options    for write
	 * @throws IOException if an I/O error occurs
	 */
	public static void write(Set<Station> stationSet, Path outPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			stationSet.forEach(s -> {
				try {
					pw.println(s.getName() + " " + s.getNetwork() + " " + s.getPosition());
				} catch (Exception e) {
					pw.println(s.getName() + " " + s.getPosition());
				}
			});
		}
	}

    /**
     * @param infoPath of station information file
     * @return (<b>unmodifiable</b>) Set of stations
     * @throws IOException if an I/O error occurs
     */
	public static Set<Station> read(Path infoPath) throws IOException {
		Set<Station> stationSet = new HashSet<>();
		try (BufferedReader br = Files.newBufferedReader(infoPath)) {
			br.lines().map(String::trim).filter(line -> !line.startsWith("#")).forEach(line -> {
				String[] parts = line.split("\\s+");
				HorizontalPosition hp = new HorizontalPosition(Double.parseDouble(parts[2]),
						Double.parseDouble(parts[3]));
				Station st = new Station(parts[0], hp, parts[1]);
				if (!stationSet.add(st))
					throw new RuntimeException("There is duplication in " + infoPath + "\n" + st);
			});
		}
		if (stationSet.size() != stationSet.stream().map(Station::toString).distinct().count()){
			System.err.println("CAUTION!! Stations with same name and network but different positions detected!");
			Map<String, List<Station>> nameToStation = new HashMap<>();
			stationSet.forEach(sta -> {
				if (nameToStation.containsKey(sta.toString())) {
					List<Station> tmp = nameToStation.get(sta.toString());
					tmp.add(sta);
					nameToStation.put(sta.toString(), tmp);
				}
				else {
					List<Station> tmp = new ArrayList<>();
					tmp.add(sta);
					nameToStation.put(sta.toString(), tmp);
				}
			});
			nameToStation.forEach((name, sta) -> {
				if (sta.size() > 1) {
					sta.stream().forEach(s -> System.out.println(s + " " + s.getPosition()));
				}
			});
		}

		return Collections.unmodifiableSet(stationSet);
	}

    /**
     * ワーキングディレクトリ下のイベントフォルダ群からステーション情報を抽出して書き込む。
     *
     * @param workPath under which this looks for event folders and stations under
     *                 the folders
     * @param options  for write
     * @throws IOException if an I/O error occurs
     */
	public static void createStationInformationFile(Path workPath, OpenOption... options) throws IOException {
		Path out = workPath.resolve("station" + Utilities.getTemporaryString() + ".inf");

		Set<SACFileName> sacNameSet = Utilities.sacFileNameSet(workPath);
		Set<Station> stationSet = sacNameSet.stream().filter(sacname -> sacname.getComponent().equals(SACComponent.T)).map(sacName -> {
			try {
				return sacName.readHeader();
			} catch (Exception e) {
				System.err.println(sacName + " is an invalid SAC file.");
				return null;
			}
		}).filter(Objects::nonNull).map(Station::of).collect(Collectors.toSet());

		if (stationSet.size() != stationSet.stream().map(Station::toString).distinct().count()) {
			System.err.println("CAUTION!! Stations with same name and network but different positions detected!");
			Map<String, List<Station>> nameToStation = new HashMap<>();
			stationSet.forEach(sta -> {
				if (nameToStation.containsKey(sta.toString())) {
					List<Station> tmp = nameToStation.get(sta.toString());
					tmp.add(sta);
					nameToStation.put(sta.toString(), tmp);
				}
				else {
					List<Station> tmp = new ArrayList<>();
					tmp.add(sta);
					nameToStation.put(sta.toString(), tmp);
				}
			});
			nameToStation.forEach((name, sta) -> {
				if (sta.size() > 1) {
					sta.stream().forEach(s -> System.out.println(s + " " + s.getPosition()));
				}
			});
		}
		
		write(stationSet, out, options);
	}

    /**
     * ワーキングディレクトリ下のイベントフォルダ群からステーション情報を抽出して書き込む。 Creates a file for stations
     * under the working folder.
     *
     * @param args [folder: to look into for stations (containing event folders)]
     * @throws IOException if an I/O error occurs
     */
	public static void main(String[] args) throws IOException {
		if (0 < args.length) {
			String path = args[0];
			if (!path.startsWith("/"))
				path = System.getProperty("user.dir") + "/" + path;
			Path f = Paths.get(path);
			if (Files.exists(f) && Files.isDirectory(f))
				createStationInformationFile(f);
			else
				System.out.println(f + " does not exist or is not a directory.");
		} else {
			Path workPath;
			String path = "";
			do {
				try {
					path = JOptionPane.showInputDialog("Working folder?", path);
				} catch (Exception e) {
					System.out.println("Working folder?");
					try (BufferedReader br = new BufferedReader(
							new InputStreamReader(new CloseShieldInputStream(System.in)))) {
						path = br.readLine().trim();
						if (!path.startsWith("/"))
							path = System.getProperty("user.dir") + "/" + path;
					} catch (Exception e2) {
						e2.printStackTrace();
						throw new RuntimeException();
					}
				}
				if (path == null || path.isEmpty()) return;
				workPath = Paths.get(path);
				if (!Files.isDirectory(workPath)) continue;
			} while (!Files.exists(workPath) || !Files.isDirectory(workPath));
			createStationInformationFile(workPath);
		}

	}
	
}
