package io.github.kensuke1984.kibrary.timewindow;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.anisotime.Phase;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;

import javax.swing.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

/**
 * The file containing timewindow information.
 * <p>
 * The file contains<br>
 * Numbers of stations and events<br>
 * Each station information <br>
 * - name, network, position <br>
 * Each event <br>
 * - Global CMT ID Each period<br>
 * Each timewindoow information<br>
 * - see {@link #create(byte[], Station[], GlobalCMTID[])}
 *
 * @author Kensuke Konishi
 * @version 0.3.1
 * @author anselme add phase information
 */
public final class TimewindowInformationFile {

	/**
	 * bytes for one time window information
	 * @author anselme increased the byte size of a time window to add phase information
	 */
	public static final int oneWindowByte = 33;

    private TimewindowInformationFile() {
    }

	/**
	 * @param args [information file name]
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Set<TimewindowInformation> set;
		if (args.length == 1)
			set = TimewindowInformationFile.read(Paths.get(args[0]));
		else if (args.length == 2 && (args[0] == "--debug" || args[1] == "--debug")) {
			String timewindowname;
			if (args[0] == "--debug")
				timewindowname = args[1];
			else
				timewindowname = args[0];
			set = TimewindowInformationFile.read(Paths.get(timewindowname));
			
			Path outpathStation = Paths.get(timewindowname.split(".inf")[0] + "_station.inf");
			Path outpathEvent = Paths.get(timewindowname.split(".inf")[0] + "_event.inf");
			
		}
		else {
			String s = "";
			Path f;
			do {
				s = JOptionPane.showInputDialog("file?", s);
				if (s == null || s.isEmpty())
					return;
				f = Paths.get(s);
			} while (!Files.exists(f) || Files.isDirectory(f));
			set = TimewindowInformationFile.read(f);
		}
		
		set.stream().sorted().forEach(tw -> {System.out.println(tw + " " + tw.getStation().getPosition());});
		
		Set<Station> stations = set.stream().map(tw -> tw.getStation()).collect(Collectors.toSet());
		Path stationFile = Paths.get("timewindow.station");
		Files.deleteIfExists(stationFile);
		Files.createFile(stationFile);
		try {
			for (Station s : stations)
				Files.write(stationFile, (s.getName() + " " + s.getNetwork() + " " + s.getPosition() + "\n").getBytes()
						, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Output TimeWindowInformation
	 *
	 * @param outputPath to write the information on
	 * @param infoSet    Set of timewindow information
	 * @param options    for write
	 * @throws IOException if an I/O error occurs.
	 * @author Kensuke Konishi
	 * @author anselme add phase information
	 */
	public static void write(Set<TimewindowInformation> infoSet, Path outputPath, OpenOption... options)
			throws IOException {
		if (infoSet.isEmpty())
			throw new RuntimeException("Input information is empty..");
		try (DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(Files.newOutputStream(outputPath, options)))) {
			GlobalCMTID[] ids = infoSet.stream().map(TimewindowInformation::getGlobalCMTID).distinct().sorted()
					.toArray(GlobalCMTID[]::new);
			Station[] stations = infoSet.stream().map(TimewindowInformation::getStation).distinct().sorted()
					.toArray(Station[]::new);
			Phase[] phases = infoSet.stream().map(TimewindowInformation::getPhases).flatMap(p -> Stream.of(p))
				.distinct().toArray(Phase[]::new);
			
			Map<GlobalCMTID, Integer> idMap = new HashMap<>();
			Map<Station, Integer> stationMap = new HashMap<>();
			Map<Phase, Integer> phaseMap = new HashMap<>();
			dos.writeShort(stations.length);
			dos.writeShort(ids.length);
			dos.writeShort(phases.length);
			for (int i = 0; i < stations.length; i++) {
				stationMap.put(stations[i], i);
				dos.writeBytes(StringUtils.rightPad(stations[i].getName(), 8));
				dos.writeBytes(StringUtils.rightPad(stations[i].getNetwork(), 8));
				HorizontalPosition pos = stations[i].getPosition();
				dos.writeDouble(pos.getLatitude());
				dos.writeDouble(pos.getLongitude());
			}
			for (int i = 0; i < ids.length; i++) {
				idMap.put(ids[i], i);
				dos.writeBytes(StringUtils.rightPad(ids[i].toString(), 15));
			}
			for (int i = 0; i < phases.length; i++) {
				phaseMap.put(phases[i], i);
				if (phases[i] == null)
					throw new NullPointerException(i + " " + "phase is null");
				dos.writeBytes(StringUtils.rightPad(phases[i].toString(), 16));
			}
			for (TimewindowInformation info : infoSet) {
				dos.writeShort(stationMap.get(info.getStation()));
				dos.writeShort(idMap.get(info.getGlobalCMTID()));
				Phase[] Infophases = info.getPhases();
				for (int i = 0; i < 10; i++) {
					if (i < Infophases.length) {
						dos.writeShort(phaseMap.get(Infophases[i]));
					}
					else
						dos.writeShort(-1);
				}
				dos.writeByte(info.getComponent().valueOf());
				float startTime = (float) Precision.round(info.startTime, 3);
				float endTime = (float) Precision.round(info.endTime, 3);
				dos.writeFloat(startTime);
				dos.writeFloat(endTime);
			}
		}
	}

    /**
     * @param infoPath of the information file to read
     * @return <b>unmodifiable</b> Set of timewindow information
     * @throws IOException if an I/O error occurs
     * @author Kensuke Konishi
     * @author anselme add phase information
     */
	public static Set<TimewindowInformation> read(Path infoPath) throws IOException {
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(infoPath)));) {
			long t = System.nanoTime();
			long fileSize = Files.size(infoPath);
			// Read header
			Station[] stations = new Station[dis.readShort()];
			GlobalCMTID[] cmtIDs = new GlobalCMTID[dis.readShort()];
			Phase[] phases = new Phase[dis.readShort()];
			int headerBytes = 3 * 2 + (8 + 8 + 8 * 2) * stations.length + 15 * cmtIDs.length + 16 * phases.length;
			long windowParts = fileSize - headerBytes;
			if (windowParts % oneWindowByte != 0)
				throw new RuntimeException(infoPath + " has some problems.");
			// name(8),network(8),position(8*2)
			byte[] stationBytes = new byte[32];
			for (int i = 0; i < stations.length; i++) {
				dis.read(stationBytes);
				stations[i] = Station.createStation(stationBytes);
			}
			byte[] cmtIDBytes = new byte[15];
			for (int i = 0; i < cmtIDs.length; i++) {
				dis.read(cmtIDBytes);
				cmtIDs[i] = new GlobalCMTID(new String(cmtIDBytes).trim());
			}
			byte[] phaseBytes = new byte[16];
			for (int i = 0; i < phases.length; i++) {
				dis.read(phaseBytes);
				phases[i] = Phase.create(new String(phaseBytes).trim());
			}
			int nwindow = (int) (windowParts / oneWindowByte);
			byte[][] bytes = new byte[nwindow][oneWindowByte];
			for (int i = 0; i < nwindow; i++)
				dis.read(bytes[i]);
//			Set<TimewindowInformation> infoSet = Arrays.stream(bytes).parallel().map(b -> create(b, stations, cmtIDs, phases))
//					.collect(Collectors.toSet());
			Set<TimewindowInformation> infoSet = Arrays.stream(bytes).map(b -> create(b, stations, cmtIDs, phases))
					.collect(Collectors.toSet());
			System.err.println(
					infoSet.size() + " timewindow data were found in " + Utilities.toTimeString(System.nanoTime() - t));
			return Collections.unmodifiableSet(infoSet);
		}
	}

	/**
	 * 1 time window {@value #oneWindowByte} byte
	 * 
	 * Station index(2)<br>
	 * GlobalCMTID index(2)<br>
	 * component(1)<br>
	 * Float starting time (4) (Round off to the third decimal place.),<br>
	 * Float end time (4) (Round off to the third decimal place.), <br>
	 *
     * @param bytes    byte array
     * @param stations station array
     * @param ids      id array
     * @return TimewindowInformation
	 * @author anselme add phase information
	 */
	private static TimewindowInformation create(byte[] bytes, Station[] stations, GlobalCMTID[] ids, Phase[] phases) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		Station station = stations[bb.getShort()];
		GlobalCMTID id = ids[bb.getShort()];
		Set<Phase> tmpset = new HashSet<>();
		for (int i = 0; i < 10; i++) {
			short iphase = bb.getShort();
			if (iphase != -1)
				tmpset.add(phases[iphase]);
		}
		Phase[] usablephases = new Phase[tmpset.size()];
		usablephases = tmpset.toArray(usablephases);
		SACComponent component = SACComponent.getComponent(bb.get());
		double startTime = bb.getFloat();
		double endTime = bb.getFloat();
		return new TimewindowInformation(startTime, endTime, station, id, component, usablephases);
	}

}
