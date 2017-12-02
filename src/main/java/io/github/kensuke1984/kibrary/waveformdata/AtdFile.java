package io.github.kensuke1984.kibrary.waveformdata;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.inversion.WeightingType;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.FrequencyRange;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;

public final class AtdFile {
	
	public static final int oneWeightingTypeByte = 10;
	public static final int onePhasesByte = 20;
	public static final int onePartialTypeByte = 10;
	
	/**
	 * bytes for one AtA entry
	 * 
	 * weightingTypes 2
	 * frequencyRange 2
	 * phases 2
	 * partialType1 2
	 * location1 2
	 * value 8
	 */
	public static final int oneEntryByte = 18;

	private AtdFile() {
	}

	/**
	 * @param args
	 *            [information file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		List<AtdEntry> atdEntries;
		if (args.length == 1)
			atdEntries = AtdFile.read(Paths.get(args[0]));
//		else if (args.length == 2 && (args[0] == "--debug" || args[1] == "--debug")) {
//			String timewindowname;
//			if (args[0] == "--debug")
//				timewindowname = args[1];
//			else
//				timewindowname = args[0];
//			set = TimewindowInformationFile.read(Paths.get(timewindowname));
//			
//			Path outpathStation = Paths.get(timewindowname.split(".inf")[0] + "_station.inf");
//			Path outpathEvent = Paths.get(timewindowname.split(".inf")[0] + "_event.inf");
//			
//		}
		else {
			String s = "";
			Path f;
			do {
				s = JOptionPane.showInputDialog("file?", s);
				if (s == null || s.isEmpty())
					return;
				f = Paths.get(s);
			} while (!Files.exists(f) || Files.isDirectory(f));
			atdEntries = AtdFile.read(f);
		}
		
		atdEntries.stream().forEach(System.out::println);
	}

	/**
	 * Output TimeWindowInformation
	 * 
	 * @param infoSet
	 *            Set of timewindow information
	 * @param outputPath
	 *            to write the information on
	 * @param options
	 *            for output
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static void write(List<AtdEntry> atdEntries, WeightingType[] weightingTypes, FrequencyRange[] frequencyRanges,
			PartialType[] partialTypes, Path outputPath, OpenOption... options)
			throws IOException {
		if (atdEntries.isEmpty())
			throw new RuntimeException("Input information is empty..");
		try (DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(Files.newOutputStream(outputPath, options)))) {
			
			Phases[] phases = atdEntries.stream().map(e -> e.getPhases()).distinct().collect(Collectors.toList()).toArray(new Phases[0]);
			Location[] locations = atdEntries.stream().map(e -> e.getLocation()).distinct().collect(Collectors.toList()).toArray(new Location[0]);
			
			Map<WeightingType, Integer> weightingTypeMap = new HashMap<>();
			Map<FrequencyRange, Integer> frequencyRangesMap = new HashMap<>();
			Map<Phases, Integer> phasesMap = new HashMap<>();
			Map<PartialType, Integer> partialTypeMap = new HashMap<>();
			Map<Location, Integer> locationMap = new HashMap<>();
			
			dos.writeShort(weightingTypes.length);
			dos.writeShort(frequencyRanges.length);
			dos.writeShort(phases.length);
			dos.writeShort(partialTypes.length);
			dos.writeShort(locations.length);
			
			for (int i = 0; i < weightingTypes.length; i++) {
				weightingTypeMap.put(weightingTypes[i], i);
				if ((int) weightingTypes[i].name().chars().count() > oneWeightingTypeByte)
					throw new RuntimeException("WeightingType string should be 10 characters or less" + weightingTypes[i].name());
				dos.writeBytes(StringUtils.rightPad(weightingTypes[i].name(), oneWeightingTypeByte));
			}
			for (int i = 0; i < frequencyRanges.length; i++) {
				frequencyRangesMap.put(frequencyRanges[i], i);
				dos.writeDouble(frequencyRanges[i].getMinFreq());
				dos.writeDouble(frequencyRanges[i].getMaxFreq());
			}
			for (int i = 0; i < phases.length; i++) {
				phasesMap.put(phases[i], i);
				if ((int) phases[i].toString().chars().count() > onePhasesByte)
					throw new RuntimeException("Phases string should be 20 characters or less " + phases[i].toString());
				dos.writeBytes(StringUtils.rightPad(phases[i].toString(), onePhasesByte));
			}
			for (int i = 0; i < partialTypes.length; i++) {
				partialTypeMap.put(partialTypes[i], i);
				if ((int) partialTypes[i].name().chars().count() > onePartialTypeByte)
					throw new RuntimeException("PartialType string should be 10 characters or less " + partialTypes[i].name());
				dos.writeBytes(StringUtils.rightPad(partialTypes[i].toString(), onePartialTypeByte));
			}
			for (int i = 0; i < locations.length; i++) {
				locationMap.put(locations[i], i);
				dos.writeFloat((float) locations[i].getLatitude());
				dos.writeFloat((float) locations[i].getLongitude());
				dos.writeFloat((float) locations[i].getR());
			}
			
			for (AtdEntry entry : atdEntries) {
				dos.writeShort(weightingTypeMap.get(entry.getWeightingType()));
				dos.writeShort(frequencyRangesMap.get(entry.getFrequencyRange()));
				dos.writeShort(phasesMap.get(entry.getPhases()));
				dos.writeShort(partialTypeMap.get(entry.getPartialType()));
				dos.writeShort(locationMap.get(entry.getLocation()));
				dos.writeDouble(entry.getValue());
				
//				Phase[] Infophases = info.getPhases();
//				for (int i = 0; i < 10; i++) {
//					if (i < Infophases.length) {
//						dos.writeShort(phaseMap.get(Infophases[i]));
//					}
//					else
//						dos.writeShort(-1);
//				}
				
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
	public static List<AtdEntry> read(Path atdPath) throws IOException {
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(atdPath)));) {
			long t = System.nanoTime();
			long fileSize = Files.size(atdPath);
			// Read header
			WeightingType[] weightingTypes = new WeightingType[dis.readShort()];
			FrequencyRange[] frequencyRanges = new FrequencyRange[dis.readShort()];
			Phases[] phases = new Phases[dis.readShort()];
			PartialType[] partialTypes = new PartialType[dis.readShort()];
			Location[] locations = new Location[dis.readShort()];
			
			int headerBytes = 5 * 2 + weightingTypes.length * oneWeightingTypeByte 
					+ frequencyRanges.length * 2 * 8 + phases.length * onePhasesByte
					+ partialTypes.length * onePartialTypeByte + locations.length * 3 * 4;
			
			long entryPart = fileSize - headerBytes;
			if (entryPart % oneEntryByte != 0) {
				System.out.println(fileSize + " " + entryPart + " " + oneEntryByte);
				throw new RuntimeException(atdPath + " has some problems.");
			}
			
			byte[] weightingTypeByte = new byte[oneWeightingTypeByte];
			for (int i = 0; i < weightingTypes.length; i++) {
				dis.read(weightingTypeByte);
				weightingTypes[i] = WeightingType.valueOf(new String(weightingTypeByte).trim());
			}
			byte[] frequencyRangeByte = new byte[16];
			for (int i = 0; i < frequencyRanges.length; i++) {
				dis.read(frequencyRangeByte);
				ByteBuffer bb = ByteBuffer.wrap(frequencyRangeByte);
				frequencyRanges[i] = new FrequencyRange(bb.getDouble(), bb.getDouble());
			}
			byte[] phasesByte = new byte[onePhasesByte];
			for (int i = 0; i < phases.length; i++) {
				dis.read(phasesByte);
				phases[i] = new Phases(new String(phasesByte).trim());
			}
			byte[] partialTypeByte = new byte[onePartialTypeByte];
			for (int i = 0; i < partialTypes.length; i++) {
				dis.read(partialTypeByte);
				partialTypes[i] = PartialType.valueOf(new String(partialTypeByte).trim());
			}
			byte[] locationByte = new byte[12];
			for (int i = 0; i < locations.length; i++) {
				dis.read(locationByte);
				ByteBuffer bb = ByteBuffer.wrap(locationByte);
				locations[i] = new Location(bb.getFloat(), bb.getFloat(), bb.getFloat());
			}
			
			int nEntry = (int) (entryPart / oneEntryByte);
			byte[][] bytes = new byte[nEntry][oneEntryByte];
			for (int i = 0; i < nEntry; i++)
				dis.read(bytes[i]);
			List<AtdEntry> ataEntries = Arrays.stream(bytes).parallel().map(b -> 
				create(b, weightingTypes, frequencyRanges, phases, partialTypes, locations))
				.collect(Collectors.toList());
			System.err.println(
					ataEntries.size() + " AtA elements were found in " + Utilities.toTimeString(System.nanoTime() - t));
			return Collections.unmodifiableList(ataEntries);
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
	private static AtdEntry create(byte[] bytes, WeightingType[] weightingTypes, FrequencyRange[] frequencyRanges
			, Phases[] phases, PartialType[] partialTypes, Location[] locations) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		WeightingType weightingType = weightingTypes[bb.getShort()];
		FrequencyRange frequencyRange = frequencyRanges[bb.getShort()];
		Phases phase = phases[bb.getShort()];
		PartialType partialType = partialTypes[bb.getShort()];
		Location location = locations[bb.getShort()];
		double value = bb.getDouble();
		
//		Set<Phase> tmpset = new HashSet<>();
//		for (int i = 0; i < 10; i++) {
//			short iphase = bb.getShort();
//			if (iphase != -1)
//				tmpset.add(phases[iphase]);
//		}
//		Phase[] usablephases = new Phase[tmpset.size()];
//		usablephases = tmpset.toArray(usablephases);
		
		return new AtdEntry(weightingType, frequencyRange, phase, partialType, location, value);
	}
	
}
