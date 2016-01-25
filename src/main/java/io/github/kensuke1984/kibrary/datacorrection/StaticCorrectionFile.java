package io.github.kensuke1984.kibrary.datacorrection;

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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * Information File of {@link StaticCorrection}
 * 
 * 1 time shift {@value #oneCorrectionByte} byte The file contains<br>
 * Numbers of stations, events<br>
 * Each station information <br>
 * - name, network, position <br>
 * Each event <br>
 * - Global CMT ID Each period<br>
 * Each static correction information<br>
 * - see in {@link #read(Path)}<br>
 * 
 * 
 * @version 0.2.0.1
 * 
 * @author Kensuke
 * 
 */
public final class StaticCorrectionFile {
	/**
	 * The number of bytes for one time shift data
	 */
	public static final int oneCorrectionByte = 17;

	private StaticCorrectionFile() {
	}

	/**
	 * @param infoPath
	 *            of the correction must exist
	 * @return (<b>thread safe</b>) Set of StaticCorrection
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static Set<StaticCorrection> read(Path infoPath) throws IOException {
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(infoPath)))) {
			long fileSize = Files.size(infoPath);
			Station[] stations = new Station[dis.readShort()];
			GlobalCMTID[] cmtIDs = new GlobalCMTID[dis.readShort()];
			int headerBytes = 2 * 2 + (8 + 8 + 4 * 2) * stations.length + 15 * cmtIDs.length;
			long infoParts = fileSize - headerBytes;
			if (infoParts % oneCorrectionByte != 0)
				throw new RuntimeException(infoPath + " is not valid..");
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
			int nInfo = (int) (infoParts / oneCorrectionByte);
			byte[][] bytes = new byte[nInfo][17];
			for (int i = 0; i < nInfo; i++)
				dis.read(bytes[i]);
			Set<StaticCorrection> staticCorrectionSet = Arrays.stream(bytes).parallel()
					.map(b -> createCorrection(b, stations, cmtIDs)).collect(Collectors.toSet());
			System.err.println(staticCorrectionSet.size() + " static corrections are read.");
			return Collections.unmodifiableSet(staticCorrectionSet);
		}
	}

	/**
	 * Creates a static correction from the input bytes.
	 * 
	 * Station index(2)<br>
	 * GlobalCMTID index(2)<br>
	 * component(1)<br>
	 * Float start time(4) round off to the third decimal place<br>
	 * Float time shift(4) round off to the third decimal place.<br>
	 * Float amplitude ratio(obs/syn) (4) round off to the third decimal place
	 * 
	 * @param bytes
	 *            containing infomation above.
	 * @return created static correction
	 */
	private static StaticCorrection createCorrection(byte[] bytes, Station[] stations, GlobalCMTID[] ids) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		Station station = stations[bb.getShort()];
		GlobalCMTID id = ids[bb.getShort()];
		SACComponent comp = SACComponent.getComponent(bb.get());
		double start = bb.getFloat();
		double timeshift = bb.getFloat();
		double amplitude = bb.getFloat();
		return new StaticCorrection(station, id, comp, start, timeshift, amplitude);
	}

	/**
	 * @param correctionSet
	 *            of static correction to write
	 * @param outPath
	 *            of an output file.
	 * @param options
	 *            for output
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static void write(Set<StaticCorrection> correctionSet, Path outPath, OpenOption... options)
			throws IOException {
		Station[] stations = correctionSet.stream().map(sc -> sc.getStation()).distinct().sorted()
				.toArray(n -> new Station[n]);
		GlobalCMTID[] ids = correctionSet.stream().map(sc -> sc.getGlobalCMTID()).distinct().sorted()
				.toArray(n -> new GlobalCMTID[n]);

		Map<Station, Integer> stationMap = IntStream.range(0, stations.length).boxed()
				.collect(Collectors.toMap(i -> stations[i], i -> i));
		Map<GlobalCMTID, Integer> idMap = IntStream.range(0, ids.length).boxed()
				.collect(Collectors.toMap(i -> ids[i], i -> i));

		try (DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(Files.newOutputStream(outPath, options)))) {
			dos.writeShort(stations.length);
			dos.writeShort(ids.length);
			for (int i = 0; i < stations.length; i++) {
				dos.writeBytes(StringUtils.rightPad(stations[i].getStationName(), 8));
				dos.writeBytes(StringUtils.rightPad(stations[i].getNetwork(), 8));
				HorizontalPosition pos = stations[i].getPosition();
				dos.writeFloat((float) pos.getLatitude());
				dos.writeFloat((float) pos.getLongitude());
			}
			for (int i = 0; i < ids.length; i++)
				dos.writeBytes(StringUtils.rightPad(ids[i].toString(), 15));

			for (StaticCorrection correction : correctionSet) {
				dos.writeShort(stationMap.get(correction.getStation()));
				dos.writeShort(idMap.get(correction.getGlobalCMTID()));
				dos.writeByte(correction.getComponent().valueOf());
				dos.writeFloat((float) correction.getSynStartTime());
				dos.writeFloat((float) correction.getTimeshift());
				dos.writeFloat((float) correction.getAmplitudeRatio());
			}
		}

	}

	/**
	 * Shows all static corrections in a file
	 * 
	 * @param args
	 *            [static correction file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Set<StaticCorrection> scf = null;
		if (args.length != 0)
			scf = StaticCorrectionFile.read(Paths.get(args[0]));
		else {
			String s = JOptionPane.showInputDialog("file?");
			if (s == null || s.equals(""))
				return;
			scf = StaticCorrectionFile.read(Paths.get(s));
		}
		scf.stream().sorted().forEach(System.out::println);
	}

}
