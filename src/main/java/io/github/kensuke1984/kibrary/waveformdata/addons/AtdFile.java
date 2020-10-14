package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionType;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.addons.WeightingType;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.FrequencyRange;
import io.github.kensuke1984.kibrary.util.addons.Phases;
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
import java.util.ArrayList;
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
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;

/**
 * @author Anselme Borgeaud
 *
 */
public final class AtdFile {
	
	public static final int oneWeightingTypeByte = 4;
	public static final int onePhasesByte = 20;
	public static final int onePartialTypeByte = 10;
	public static final int oneCorrectionTypeByte = 10;
	
	/**
	 * bytes for one AtA entry
	 * 
	 * weightingTypes 2
	 * frequencyRange 2
	 * phases 2
	 * correctionType 2
	 * partialType1 2
	 * location1 2
	 * value 8
	 */
	public static final int oneEntryByte = 20;

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
			PartialType[] partialTypes, StaticCorrectionType[] correctionTypes, Path outputPath, OpenOption... options)
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
			Map<StaticCorrectionType, Integer> correctionTypeMap = new HashMap<>();
			Map<PartialType, Integer> partialTypeMap = new HashMap<>();
			Map<Location, Integer> locationMap = new HashMap<>();
			
			dos.writeShort(weightingTypes.length);
			dos.writeShort(frequencyRanges.length);
			dos.writeShort(phases.length);
			dos.writeShort(correctionTypes.length);
			dos.writeShort(partialTypes.length);
			dos.writeShort(locations.length);
			
			for (int i = 0; i < weightingTypes.length; i++) {
				weightingTypeMap.put(weightingTypes[i], i);
//				dos.writeBytes(StringUtils.rightPad(weightingTypes[i].name(), oneWeightingTypeByte));
				dos.writeInt(weightingTypes[i].getValue());
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
			for (int i = 0; i < correctionTypes.length; i++) {
				correctionTypeMap.put(correctionTypes[i], i);
				if ((int) correctionTypes[i].name().chars().count() > oneCorrectionTypeByte)
					throw new RuntimeException("CorrectionType string should be 10 characters or less " + correctionTypes[i].name());
				dos.writeBytes(StringUtils.rightPad(correctionTypes[i].toString(), oneCorrectionTypeByte));
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
				dos.writeShort(correctionTypeMap.get(entry.getCorrectionType()));
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
	
	public static void write(RealVector atd, UnknownParameter[] unknowns, WeightingType weightingType, FrequencyRange frequencyRange
			, Phases phases, StaticCorrectionType correctionType, Path outputPath, OpenOption... options)
			throws IOException {
		WeightingType[] weightingTypes = new WeightingType[] {weightingType};
		FrequencyRange[] frequencyRanges = new FrequencyRange[] {frequencyRange};
		StaticCorrectionType[] correctionTypes = new StaticCorrectionType[] {correctionType};
		PartialType[] partialTypes = Stream.of(unknowns).map(u -> u.getPartialType()).collect(Collectors.toList()).toArray(new PartialType[0]);
		
		List<AtdEntry> atdEntries = new ArrayList<>();
		for (int i = 0; i < unknowns.length; i++) {
			AtdEntry entry = new AtdEntry(weightingType, frequencyRange, phases, correctionType, unknowns[i].getPartialType(), unknowns[i].getLocation(), atd.getEntry(i));
			atdEntries.add(entry);
		}
		
		write(atdEntries, weightingTypes, frequencyRanges, partialTypes, correctionTypes, outputPath, options);
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
			StaticCorrectionType[] correctionTypes = new StaticCorrectionType[dis.readShort()];
			PartialType[] partialTypes = new PartialType[dis.readShort()];
			Location[] locations = new Location[dis.readShort()];
			
			int headerBytes = 6 * 2 + weightingTypes.length * oneWeightingTypeByte 
					+ frequencyRanges.length * 2 * 8 + phases.length * onePhasesByte + correctionTypes.length * oneCorrectionTypeByte
					+ partialTypes.length * onePartialTypeByte + locations.length * 3 * 4;
			
			long entryPart = fileSize - headerBytes;
			if (entryPart % oneEntryByte != 0) {
				System.out.println(fileSize + " " + entryPart + " " + oneEntryByte);
				throw new RuntimeException(atdPath + " has some problems.");
			}
			
			for (int i = 0; i < weightingTypes.length; i++) {
				int n = dis.readInt();
				weightingTypes[i] = WeightingType.getType(n);
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
			byte[] correctionByte = new byte[oneCorrectionTypeByte];
			for (int i = 0; i < correctionTypes.length; i++) {
				dis.read(correctionByte);
				correctionTypes[i] = StaticCorrectionType.valueOf(new String(correctionByte).trim());
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
				create(b, weightingTypes, frequencyRanges, phases, correctionTypes, partialTypes, locations))
				.collect(Collectors.toList());
			System.err.println(
					ataEntries.size() + " AtA elements were found in " + Utilities.toTimeString(System.nanoTime() - t));
			return Collections.unmodifiableList(ataEntries);
		}
	}
	
	/**
	 * @param infoPath
	 *            of the information file to read
	 * @return (<b>unmodifiable</b>) Set of timewindow information
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static AtdEntry[][][][][] readArray(Path atdPath) throws IOException {
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(atdPath)));) {
			long t = System.nanoTime();
			long fileSize = Files.size(atdPath);
			// Read header
			WeightingType[] weightingTypes = new WeightingType[dis.readShort()];
			FrequencyRange[] frequencyRanges = new FrequencyRange[dis.readShort()];
			Phases[] phases = new Phases[dis.readShort()];
			StaticCorrectionType[] correctionTypes = new StaticCorrectionType[dis.readShort()];
			PartialType[] partialTypes = new PartialType[dis.readShort()];
			Location[] locations = new Location[dis.readShort()];
			
			int headerBytes = 6 * 2 + weightingTypes.length * oneWeightingTypeByte 
					+ frequencyRanges.length * 2 * 8 + phases.length * onePhasesByte + correctionTypes.length * oneCorrectionTypeByte
					+ partialTypes.length * onePartialTypeByte + locations.length * 3 * 4;
			
			long entryPart = fileSize - headerBytes;
			if (entryPart % oneEntryByte != 0) {
				System.out.println(fileSize + " " + entryPart + " " + oneEntryByte);
				throw new RuntimeException(atdPath + " has some problems.");
			}
			
			for (int i = 0; i < weightingTypes.length; i++) {
				int n = dis.readInt();
				weightingTypes[i] = WeightingType.getType(n);
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
			byte[] correctionByte = new byte[oneCorrectionTypeByte];
			for (int i = 0; i < correctionTypes.length; i++) {
				dis.read(correctionByte);
				correctionTypes[i] = StaticCorrectionType.valueOf(new String(correctionByte).trim());
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
			
			int n1 = weightingTypes.length * frequencyRanges.length * phases.length * correctionTypes.length;
			int n0Atd = nEntry / n1;
			
			AtdEntry[][][][][] atdEntries = new AtdEntry[n0Atd][weightingTypes.length][frequencyRanges.length][phases.length][correctionTypes.length];
			
			for (int i = 0; i < nEntry; i++) {
				byte[] tmpbytes = new byte[oneEntryByte];
				dis.read(tmpbytes);
				ByteBuffer bb = ByteBuffer.wrap(tmpbytes);
				int iWeight = bb.getShort();
				WeightingType weightingType = weightingTypes[iWeight];
				int iFreq = bb.getShort();
				FrequencyRange frequencyRange = frequencyRanges[iFreq];
				int iPhase = bb.getShort();
				Phases phase = phases[iPhase];
				int iCorr = bb.getShort();
				StaticCorrectionType correctionType = correctionTypes[iCorr];
				int iPartialType = bb.getShort();
				PartialType partialType = partialTypes[iPartialType];
				int iLocation = bb.getShort();
				Location location  = locations[iLocation];
				double value = bb.getDouble();
				
				int i0Atd = i / n1;
				
				atdEntries[i0Atd][iWeight][iFreq][iPhase][iCorr]
						= new AtdEntry(weightingType, frequencyRange, phase, correctionType, partialType, location, value);
			}
			System.err.println(
					nEntry + " Atd elements were found in " + Utilities.toTimeString(System.nanoTime() - t));
			return atdEntries;
		}
	}
	
	public static RealVector getAtdVector(AtdEntry[][][][][] atdEntries, int iweight, int ifreq, int iphase, int icorr) throws IOException {
		RealVector atdVector = new ArrayRealVector(atdEntries.length);
		
		for (int i = 0; i < atdEntries.length; i++) {
			double atdi = atdEntries[i][iweight][ifreq][iphase][icorr].getValue();
			atdVector.setEntry(i, atdi);
		}
		
		return atdVector;
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
			, Phases[] phases, StaticCorrectionType[] correctionTypes, PartialType[] partialTypes, Location[] locations) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		WeightingType weightingType = weightingTypes[bb.getShort()];
		FrequencyRange frequencyRange = frequencyRanges[bb.getShort()];
		Phases phase = phases[bb.getShort()];
		StaticCorrectionType correctionType = correctionTypes[bb.getShort()];
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
		
		return new AtdEntry(weightingType, frequencyRange, phase, correctionType, partialType, location, value);
	}
	
	public static AtdEntry[][][][][] add(AtdEntry[][][][][] atd1, AtdEntry[][][][][] atd2) {
		int n0Atd = atd1.length;
		int nWeight = atd1[0].length;
		int nFreq = atd1[0][0].length;
		int nPhase = atd1[0][0][0].length;
		int nCorr = atd1[0][0][0][0].length;
		AtdEntry[][][][][] atdSum = new AtdEntry[n0Atd][nWeight][nFreq][nPhase][nCorr];
		
		for (int i = 0; i < n0Atd; i++) {
			for (int iweight = 0; iweight < nWeight; iweight++) {
				for (int ifreq = 0; ifreq < nFreq; ifreq++) {
					for (int iphase = 0; iphase < nPhase; iphase++) {
						for (int icorr = 0; icorr < nCorr; icorr++) {
							AtdEntry atdEntry = atd1[i][iweight][ifreq][iphase][icorr];
							atdEntry.add(atd2[i][iweight][ifreq][iphase][icorr]);
							atdSum[i][iweight][ifreq][iphase][icorr] = atdEntry;
						}
					}
				}
			}
		}
		
		return atdSum;
	}
	
	public static AtdEntry[][][][][] multiply(AtdEntry[][][][][] atd, double[] w) {
		int n0Atd = atd.length;
		int nWeight = atd[0].length;
		int nFreq = atd[0][0].length;
		int nPhase = atd[0][0][0].length;
		int nCorr = atd[0][0][0][0].length;
		AtdEntry[][][][][] atdMul = new AtdEntry[n0Atd][nWeight][nFreq][nPhase][nCorr];
		
		for (int i = 0; i < n0Atd; i++) {
			for (int iweight = 0; iweight < nWeight; iweight++) {
				for (int ifreq = 0; ifreq < nFreq; ifreq++) {
					for (int iphase = 0; iphase < nPhase; iphase++) {
						for (int icorr = 0; icorr < nCorr; icorr++) {
							AtdEntry atdEntry = atd[i][iweight][ifreq][iphase][icorr];
							atdEntry.setValue(atdEntry.getValue() * w[i]);
							atdMul[i][iweight][ifreq][iphase][icorr] = atdEntry;
						}
					}
				}
			}
		}
		
		return atdMul;
	}
	
	public static RealVector multiply(RealVector atd, double[] w) {
		RealVector atdMul = new ArrayRealVector(atd.getDimension());
		for (int i = 0; i < atd.getDimension(); i++)
			atdMul.setEntry(i, atd.getEntry(i) * w[i]);
		return atdMul;
	}
	
}
