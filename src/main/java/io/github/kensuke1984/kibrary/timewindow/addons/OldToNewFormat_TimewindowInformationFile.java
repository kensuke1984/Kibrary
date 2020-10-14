package io.github.kensuke1984.kibrary.timewindow.addons;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;

public class OldToNewFormat_TimewindowInformationFile {

	/**
	 * bytes for one time window information
	 */
	public static final int oneWindowByte_old = 13;
	
	public static void main(String[] args) {
		Path timewindowPath = Paths.get(args[0]);
		Path timewindowOldFormatPath = Paths.get("timewindow" + Utilities.getTemporaryString() + ".dat");
		
		try {
			Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
			OldToNewFormat_TimewindowInformationFile.write_old(timewindows, timewindowOldFormatPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void write_old(Set<TimewindowInformation> infoSet, Path outputPath, OpenOption... options)
			throws IOException {
		if (infoSet.isEmpty())
			throw new RuntimeException("Input information is empty..");
		try (DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(Files.newOutputStream(outputPath, options)))) {
			GlobalCMTID[] ids = infoSet.stream().map(TimewindowInformation::getGlobalCMTID).distinct().sorted()
					.toArray(GlobalCMTID[]::new);
			Station[] stations = infoSet.stream().map(TimewindowInformation::getStation).distinct().sorted()
					.toArray(Station[]::new);
			Map<GlobalCMTID, Integer> idMap = new HashMap<>();
			Map<Station, Integer> stationMap = new HashMap<>();
			dos.writeShort(stations.length);
			dos.writeShort(ids.length);
			for (int i = 0; i < stations.length; i++) {
				stationMap.put(stations[i], i);
				dos.writeBytes(StringUtils.rightPad(stations[i].getName(), 8));
				dos.writeBytes(StringUtils.rightPad(stations[i].getNetwork(), 8));
				HorizontalPosition pos = stations[i].getPosition();
				dos.writeFloat((float) pos.getLatitude());
				dos.writeFloat((float) pos.getLongitude());
			}
			for (int i = 0; i < ids.length; i++) {
				idMap.put(ids[i], i);
				dos.writeBytes(StringUtils.rightPad(ids[i].toString(), 15));
			}
			for (TimewindowInformation info : infoSet) {
				dos.writeShort(stationMap.get(info.getStation()));
				dos.writeShort(idMap.get(info.getGlobalCMTID()));
				dos.writeByte(info.getComponent().valueOf());
				float startTime = (float) Precision.round(info.getStartTime(), 3);
				float endTime = (float) Precision.round(info.getEndTime(), 3);
				dos.writeFloat(startTime);
				dos.writeFloat(endTime);
			}
		}
	}

	/**
	 * @param infoPath
	 *            of the information file to read
	 * @return (<b>unmodifiable</b>) Set of timewindow information
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static Set<TimewindowInformation> read_old(Path infoPath) throws IOException {
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(infoPath)));) {
			long t = System.nanoTime();
			long fileSize = Files.size(infoPath);
			// Read header
			Station[] stations = new Station[dis.readShort()];
			GlobalCMTID[] cmtIDs = new GlobalCMTID[dis.readShort()];
			int headerBytes = 2 * 2 + (8 + 8 + 4 * 2) * stations.length + 15 * cmtIDs.length;
			long windowParts = fileSize - headerBytes;
			if (windowParts % oneWindowByte_old != 0)
				throw new RuntimeException(infoPath + " has some problems.");
			// name(8),network(8),position(4*2)
			byte[] stationBytes = new byte[24];
			for (int i = 0; i < stations.length; i++) {
				dis.read(stationBytes);
				stations[i] = Station.createStation(stationBytes);
			}
			byte[] cmtIDBytes = new byte[15];
			for (int i = 0; i < cmtIDs.length; i++) {
				dis.read(cmtIDBytes);
				cmtIDs[i] = new GlobalCMTID(new String(cmtIDBytes).trim());
			}
			int nwindow = (int) (windowParts / oneWindowByte_old);
			byte[][] bytes = new byte[nwindow][oneWindowByte_old];
			for (int i = 0; i < nwindow; i++)
				dis.read(bytes[i]);
			Set<TimewindowInformation> infoSet = Arrays.stream(bytes).parallel().map(b -> create_old(b, stations, cmtIDs))
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
	 * @param bytes
	 * @param stations
	 * @param ids
	 * @return
	 */
	private static TimewindowInformation create_old(byte[] bytes, Station[] stations, GlobalCMTID[] ids) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		Station station = stations[bb.getShort()];
		GlobalCMTID id = ids[bb.getShort()];
		SACComponent component = SACComponent.getComponent(bb.get());
		double startTime = bb.getFloat();
		double endTime = bb.getFloat();
		return new TimewindowInformation(startTime, endTime, station, id, component, null);
	}
}
