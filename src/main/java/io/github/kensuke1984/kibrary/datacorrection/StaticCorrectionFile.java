package io.github.kensuke1984.kibrary.datacorrection;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.anisotime.Phase;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * Information File of {@link StaticCorrection}
 * <p>
 * 1 time shift {@value #oneCorrectionByte} byte The file contains<br>
 * Numbers of stations, events<br>
 * Each station information <br>
 * - name, network, position <br>
 * Each event <br>
 * - Global CMT ID Each period<br>
 * Each static correction information<br>
 * - see in {@link #read(Path)}<br>
 *
 * @author Kensuke Konishi
 * @version 0.2.2
 * @author anselme add phase information
 */
public final class StaticCorrectionFile {
	
	/**
	 * The number of bytes for one time shift data
	 */
	public static final int oneCorrectionByte = 37;

    private StaticCorrectionFile() {
    }

    /**
     * @param infoPath of the correction must exist
     * @return <b>Thread safe</b> set of StaticCorrection
     * @throws IOException if an I/O error occurs
     */
	public static Set<StaticCorrection> read(Path infoPath) throws IOException {
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(infoPath)))) {
			long fileSize = Files.size(infoPath);
			Station[] stations = new Station[dis.readShort()];
			GlobalCMTID[] cmtIDs = new GlobalCMTID[dis.readShort()];
			Phase[] phases = new Phase[dis.readShort()];
			int headerBytes = 3 * 2 + (8 + 8 + 8 * 2) * stations.length + 15 * cmtIDs.length + 16 * phases.length;;
			long infoParts = fileSize - headerBytes;
			if (infoParts % oneCorrectionByte != 0)
				throw new RuntimeException(infoPath + " is not valid.. " + (infoParts / (double) oneCorrectionByte));
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
			int nInfo = (int) (infoParts / oneCorrectionByte);
			byte[][] bytes = new byte[nInfo][oneCorrectionByte];
			for (int i = 0; i < nInfo; i++)
				dis.read(bytes[i]);
			Set<StaticCorrection> staticCorrectionSet = Arrays.stream(bytes).parallel()
					.map(b -> createCorrection(b, stations, cmtIDs, phases)).collect(Collectors.toSet());
			System.err.println(staticCorrectionSet.size() + " static corrections are read.");
			return Collections.unmodifiableSet(staticCorrectionSet);
		}
	}

    /**
     * Creates a static correction from the input bytes.
     * <p>
     * Station index(2)<br>
     * GlobalCMTID index(2)<br>
     * component(1)<br>
     * Float start time(4) round off to the third decimal place<br>
     * Float time shift(4) round off to the third decimal place.<br>
     * Float amplitude ratio(obs/syn) (4) round off to the third decimal place
     *
     * @param bytes containing infomation above.
     * @return created static correction
     */
	private static StaticCorrection createCorrection(byte[] bytes, Station[] stations, GlobalCMTID[] ids, Phase[] phases) {
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
		SACComponent comp = SACComponent.getComponent(bb.get());
		double start = bb.getFloat();
		double timeshift = bb.getFloat();
		double amplitude = bb.getFloat();
		return new StaticCorrection(station, id, comp, start, timeshift, amplitude, usablephases);
	}

    /**
     * @param outPath       of an write file.
     * @param correctionSet of static correction to write
     * @param options       for write
     * @throws IOException if an I/O error occurs.
     */
	public static void write(Set<StaticCorrection> correctionSet, Path outPath, OpenOption... options)
			throws IOException {
		Station[] stations = correctionSet.stream().map(StaticCorrection::getStation).distinct().sorted()
				.toArray(Station[]::new);
		GlobalCMTID[] ids = correctionSet.stream().map(StaticCorrection::getGlobalCMTID).distinct().sorted()
				.toArray(GlobalCMTID[]::new);
		Phase[] phases = correctionSet.stream().map(StaticCorrection::getPhases).flatMap(p -> Stream.of(p))
				.distinct().toArray(Phase[]::new);

		Map<Station, Integer> stationMap = IntStream.range(0, stations.length).boxed()
				.collect(Collectors.toMap(i -> stations[i], i -> i));
		Map<GlobalCMTID, Integer> idMap = IntStream.range(0, ids.length).boxed()
				.collect(Collectors.toMap(i -> ids[i], i -> i));
		Map<Phase, Integer> phaseMap = new HashMap<>();
		

		try (DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(Files.newOutputStream(outPath, options)))) {
			dos.writeShort(stations.length);
			dos.writeShort(ids.length);
			dos.writeShort(phases.length);
			for (int i = 0; i < stations.length; i++) {
				dos.writeBytes(StringUtils.rightPad(stations[i].getName(), 8));
				dos.writeBytes(StringUtils.rightPad(stations[i].getNetwork(), 8));
				HorizontalPosition pos = stations[i].getPosition();
				dos.writeDouble(pos.getLatitude());
				dos.writeDouble(pos.getLongitude());
			}
			for (int i = 0; i < ids.length; i++)
				dos.writeBytes(StringUtils.rightPad(ids[i].toString(), 15));
			for (int i = 0; i < phases.length; i++) {
				phaseMap.put(phases[i], i);
				if (phases[i] == null)
					throw new NullPointerException(i + " " + "phase is null");
				dos.writeBytes(StringUtils.rightPad(phases[i].toString(), 16));
			}
			
			for (StaticCorrection correction : correctionSet) {
				dos.writeShort(stationMap.get(correction.getStation()));
				dos.writeShort(idMap.get(correction.getGlobalCMTID()));
				Phase[] Infophases = correction.getPhases();
				for (int i = 0; i < 10; i++) {
					if (i < Infophases.length) {
						dos.writeShort(phaseMap.get(Infophases[i]));
					}
					else
						dos.writeShort(-1);
				}
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
	 * @param args [static correction file name]
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Set<StaticCorrection> scf;
		if (args.length != 0)
			scf = StaticCorrectionFile.read(Paths.get(args[0]));
		else {
			String s = JOptionPane.showInputDialog("file?");
			if (s == null || s.isEmpty()) return;
			scf = StaticCorrectionFile.read(Paths.get(s));
		}
		scf.stream().sorted().forEach(corr -> {
			double azimuth = Math.toDegrees(corr.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(corr.getStation().getPosition()));
			double distance = Math.toDegrees(corr.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(corr.getStation().getPosition()));
			System.out.println(corr.toString() + " " + azimuth + " " + distance);
		});
	}

}
