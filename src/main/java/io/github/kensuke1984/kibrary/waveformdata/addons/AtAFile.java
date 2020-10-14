package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.inversion.Physical3DParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.addons.WeightingType;
import io.github.kensuke1984.kibrary.math.Matrix;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;

import com.amazonaws.transform.SimpleTypeStaxUnmarshallers.IntegerStaxUnmarshaller;

/**
 * @author Anselme Borgeaud
 *
 */
public final class AtAFile {
	
	public static final int oneWeightingTypeByte = 4;
	public static final int onePhasesByte = 20;
	public static final int onePartialTypeByte = 10;
	public static final int oneUnknownParameterByte = Physical3DParameter.oneUnknownByte;
	
	/**
	 * bytes for one AtA entry
	 * 
	 * weightingTypes 2
	 * frequencyRange 2
	 * phases 2
	 * unknown1 2
	 * unknown2 2
	 * value 8
	 */
	public static final int oneEntryByte = 18;

	private AtAFile() {
	}

	/**
	 * @param args
	 *            [information file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		AtAEntry[][][][] ataEntries;
		if (args.length == 1)
			ataEntries = AtAFile.read(Paths.get(args[0]));
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
			ataEntries = AtAFile.read(f);
		}
		
		print(ataEntries);
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
//	public static void write(List<AtAEntry> ataEntries, WeightingType[] weightingTypes, double[][] frequencyRanges,
//			PartialType[] partialTypes, Path outputPath, OpenOption... options)
//			throws IOException {
//		if (ataEntries.isEmpty())
//			throw new RuntimeException("Input information is empty..");
//		try (DataOutputStream dos = new DataOutputStream(
//				new BufferedOutputStream(Files.newOutputStream(outputPath, options)))) {
//			
//			Phases[] phases = ataEntries.stream().map(e -> e.getPhases()).distinct().collect(Collectors.toList()).toArray(new Phases[0]);
//			UnknownParameter[] unknownParameters = ataEntries.stream().map(e -> e.getLocation1()).distinct().collect(Collectors.toList()).toArray(new Location[0]);
//			
//			Map<WeightingType, Integer> weightingTypeMap = new HashMap<>();
//			Map<double[], Integer> frequencyRangesMap = new HashMap<>();
//			Map<Phases, Integer> phasesMap = new HashMap<>();
//			Map<PartialType, Integer> partialTypeMap = new HashMap<>();
//			Map<Location, Integer> locationMap = new HashMap<>();
//			
//			dos.writeShort(weightingTypes.length);
//			dos.writeShort(frequencyRanges.length);
//			dos.writeShort(phases.length);
//			dos.writeShort(partialTypes.length);
//			dos.writeShort(locations.length);
//			
//			for (int i = 0; i < weightingTypes.length; i++) {
//				weightingTypeMap.put(weightingTypes[i], i);
//				if ((int) weightingTypes[i].name().chars().count() > oneWeightingTypeByte)
//					throw new RuntimeException("WeightingType string should be 10 characters or less" + weightingTypes[i].name());
//				dos.writeBytes(StringUtils.rightPad(weightingTypes[i].name(), oneWeightingTypeByte));
//			}
//			for (int i = 0; i < frequencyRanges.length; i++) {
//				frequencyRangesMap.put(frequencyRanges[i], i);
//				dos.writeDouble(frequencyRanges[i][0]);
//				dos.writeDouble(frequencyRanges[i][1]);
//			}
//			for (int i = 0; i < phases.length; i++) {
//				phasesMap.put(phases[i], i);
//				if ((int) phases[i].toString().chars().count() > onePhasesByte)
//					throw new RuntimeException("Phases string should be 20 characters or less " + phases[i].toString());
//				dos.writeBytes(StringUtils.rightPad(phases[i].toString(), onePhasesByte));
//			}
//			for (int i = 0; i < partialTypes.length; i++) {
//				partialTypeMap.put(partialTypes[i], i);
//				if ((int) partialTypes[i].name().chars().count() > onePartialTypeByte)
//					throw new RuntimeException("PartialType string should be 10 characters or less " + partialTypes[i].name());
//				dos.writeBytes(StringUtils.rightPad(partialTypes[i].toString(), onePartialTypeByte));
//			}
//			for (int i = 0; i < locations.length; i++) {
//				locationMap.put(locations[i], i);
//				dos.writeFloat((float) locations[i].getLatitude());
//				dos.writeFloat((float) locations[i].getLongitude());
//				dos.writeFloat((float) locations[i].getR());
//			}
//			
//			for (AtAEntry entry : ataEntries) {
//				dos.writeShort(weightingTypeMap.get(entry.getWeightingType()));
//				dos.writeShort(frequencyRangesMap.get(entry.getFrequencyRange()));
//				dos.writeShort(phasesMap.get(entry.getPhases()));
//				dos.writeShort(partialTypeMap.get(entry.getPartialType1()));
//				dos.writeShort(partialTypeMap.get(entry.getPartialType2()));
//				dos.writeShort(locationMap.get(entry.getLocation1()));
//				dos.writeShort(locationMap.get(entry.getLocation2()));
//				dos.writeDouble(entry.getValue());
//				
////				Phase[] Infophases = info.getPhases();
////				for (int i = 0; i < 10; i++) {
////					if (i < Infophases.length) {
////						dos.writeShort(phaseMap.get(Infophases[i]));
////					}
////					else
////						dos.writeShort(-1);
////				}
//				
//			}
//		}
//	}
	
	public static void write(AtAEntry[][][][] ataEntries, WeightingType[] weightingTypes, FrequencyRange[] frequencyRanges,
			UnknownParameter[] unknownParameters, Phases[] phases, Path outputPath, OpenOption... options)
			throws IOException {
//		if (ataEntries.isEmpty())
//			throw new RuntimeException("Input information is empty..");
		try (DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(Files.newOutputStream(outputPath, options)))) {
			
//			Phases[] phases = ataEntries.stream().map(e -> e.getPhases()).distinct().collect(Collectors.toList()).toArray(new Phases[0]);
//			Location[] locations = ataEntries.stream().map(e -> e.getLocation1()).distinct().collect(Collectors.toList()).toArray(new Location[0]);
			
			Map<WeightingType, Integer> weightingTypeMap = new HashMap<>();
			Map<FrequencyRange, Integer> frequencyRangesMap = new HashMap<>();
			Map<Phases, Integer> phasesMap = new HashMap<>();
			Map<UnknownParameter, Integer> unknownParameterMap = new HashMap<>();
			
			dos.writeShort(weightingTypes.length);
			dos.writeShort(frequencyRanges.length);
			dos.writeShort(phases.length);
			dos.writeShort(unknownParameters.length);
			
			for (int i = 0; i < weightingTypes.length; i++) {
				weightingTypeMap.put(weightingTypes[i], i);
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
			for (int i = 0; i < unknownParameters.length; i++) {
				unknownParameterMap.put(unknownParameters[i], i);
				dos.write(unknownParameters[i].getBytes());
			}
			
			for (int i0AtA = 0; i0AtA < ataEntries.length; i0AtA++) {
				for (int iweight = 0; iweight < ataEntries[i0AtA].length; iweight++) {
					for (int ifreq = 0; ifreq < ataEntries[i0AtA][iweight].length; ifreq++) {
						for (int iphase = 0; iphase < ataEntries[i0AtA][iweight][ifreq].length; iphase++) {
							AtAEntry entry = ataEntries[i0AtA][iweight][ifreq][iphase];
							
//							System.out.println(entry);
//							System.out.println(frequencyRangesMap.get(entry.getFrequencyRange()));
							
							dos.writeShort(weightingTypeMap.get(entry.getWeightingType()));
							dos.writeShort(frequencyRangesMap.get(entry.getFrequencyRange()));
							dos.writeShort(phasesMap.get(entry.getPhases()));
							dos.writeShort(unknownParameterMap.get(entry.getUnknown1()));
							dos.writeShort(unknownParameterMap.get(entry.getUnknown2()));
							dos.writeDouble(entry.getValue());
						}
					}
				}
			}
			
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
	
	public static void write(RealMatrix ata, WeightingType weightingType, FrequencyRange frequencyRange,
			UnknownParameter[] unknownParameters, Phases phase, Path outputPath, OpenOption... options) throws IOException {
		WeightingType[] weightingTypes = new WeightingType[] {weightingType};
		FrequencyRange[] frequencyRanges = new FrequencyRange[] {frequencyRange};
		Phases[] phases = new Phases[] {phase};
		
		int n0AtA = unknownParameters.length * (unknownParameters.length + 1) / 2;
		AtAEntry[][][][] ataEntries = new AtAEntry[n0AtA][1][1][1];
		for (int i = 0; i < n0AtA; i++) {
			int iunknown = (int) (0.5 * (FastMath.sqrt(1 + 8 * i) - 1));
			int junknown = i - iunknown * (iunknown + 1) / 2;
			AtAEntry entry = new AtAEntry(weightingType, frequencyRange, phase, unknownParameters[iunknown], unknownParameters[junknown], ata.getEntry(iunknown, junknown));
			ataEntries[i][0][0][0] = entry;
		}
		
		write(ataEntries, weightingTypes, frequencyRanges, unknownParameters, phases, outputPath, options);
	}
	
	public static void print(AtAEntry[][][][] ataEntries) {
		for (int i0AtA = 0; i0AtA < ataEntries.length; i0AtA++) {
			for (int iweight = 0; iweight < ataEntries[i0AtA].length; iweight++) {
				for (int ifreq = 0; ifreq < ataEntries[i0AtA][iweight].length; ifreq++) {
					for (int iphase = 0; iphase < ataEntries[i0AtA][iweight][ifreq].length; iphase++) {
						System.out.println(ataEntries[i0AtA][iweight][ifreq][iphase]);
					}
				}
			}
		}
	}


	/**
	 * @param ataPath
	 * @return AtAEntry[n0][n1][n2][n3]
	 * n0 = (number of unknowns) * (number of unknowns + 1) / 2
	 * n1 = number of weighting types
	 * n2 = number of frequency ranges
	 * n3 = number of phases
	 * @throws IOException
	 */
	public static AtAEntry[][][][] read(Path ataPath) throws IOException {
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(ataPath)));) {
			long t = System.nanoTime();
			long fileSize = Files.size(ataPath);
			// Read header
			WeightingType[] weightingTypes = new WeightingType[dis.readShort()];
			FrequencyRange[] frequencyRanges = new FrequencyRange[dis.readShort()];
			Phases[] phases = new Phases[dis.readShort()];
			UnknownParameter[] unknownParameters = new UnknownParameter[dis.readShort()];
			
			int headerBytes = 4 * 2 + weightingTypes.length * oneWeightingTypeByte 
					+ frequencyRanges.length * 2 * 8 + phases.length * onePhasesByte
					+ unknownParameters.length * oneUnknownParameterByte;
			
			long entryPart = fileSize - headerBytes;
			if (entryPart % oneEntryByte != 0) {
				System.err.println(fileSize + " " + entryPart + " " + oneEntryByte);
				throw new RuntimeException(ataPath + " has some problems.");
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
			byte[] unknownParameterByte = new byte[oneUnknownParameterByte];
			for (int i = 0; i < unknownParameters.length; i++) {
				dis.read(unknownParameterByte);
				unknownParameters[i] = Physical3DParameter.create(unknownParameterByte);
			}
			
			int nEntry = (int) (entryPart / oneEntryByte);
			byte[][] bytes = new byte[nEntry][oneEntryByte];
			for (int i = 0; i < nEntry; i++)
				dis.read(bytes[i]);
			
			AtAEntry[][][][] ataEntries = create(bytes, weightingTypes, frequencyRanges, phases, unknownParameters);
			System.err.println(
					bytes.length + " AtA elements were found in " + Utilities.toTimeString(System.nanoTime() - t));
			return ataEntries;
		}
	}
	
	public static RealMatrix getAtARealMatrix(AtAEntry[][][][] ataEntries, int iweight, int ifreq, int iphase) throws IOException {
		int nUnknown = (int) (0.5 * (FastMath.sqrt(1 + 8 * ataEntries.length) - 1));
		RealMatrix ata = new Array2DRowRealMatrix(nUnknown, nUnknown);
		
		for (int i = 0; i < ataEntries.length; i++) {
			int iunknown = (int) (0.5 * (FastMath.sqrt(1 + 8 * i) - 1));
			int junknown = i - iunknown * (iunknown + 1) / 2;
			
			double ataij = ataEntries[i][iweight][ifreq][iphase].getValue();
			ata.setEntry(iunknown, junknown, ataij);
			if (junknown != iunknown)
				ata.setEntry(junknown, iunknown, ataij);
		}
		
		return ata;
	}
	
	public static RealMatrix getAtARealMatrix(Path ataPath, int iweight, int ifreq, int iphase) throws IOException {
		AtAEntry[][][][] ataEntries = read(ataPath);
		int nUnknown = (int) (0.5 * (FastMath.sqrt(1 + 8 * ataEntries.length) - 1));
		RealMatrix ata = new Array2DRowRealMatrix(nUnknown, nUnknown);
		
		for (int i = 0; i < ataEntries.length; i++) {
			int iunknown = (int) (0.5 * (FastMath.sqrt(1 + 8 * i) - 1));
			int junknown = i - iunknown * (iunknown + 1) / 2;
			
			double ataij = ataEntries[i][iweight][ifreq][iphase].getValue();
			ata.setEntry(iunknown, junknown, ataij);
			if (junknown != iunknown)
				ata.setEntry(junknown, iunknown, ataij);
		}
		
		return ata;
	}
	
	public static RealMatrix getAtARealMatrixParallel(Path ataPath, int iweight, int ifreq, int iphase) throws IOException {
		AtAEntry[][][][] ataEntries = read(ataPath);
		int nUnknown = (int) (0.5 * (FastMath.sqrt(1 + 8 * ataEntries.length) - 1));
		RealMatrix ata = new Array2DRowRealMatrix(nUnknown, nUnknown);
		
		IntStream.range(0, ataEntries.length).parallel().forEach(i -> {
			int iunknown = (int) (0.5 * (FastMath.sqrt(1 + 8 * i) - 1));
			int junknown = i - iunknown * (iunknown + 1) / 2;
			
			double ataij = ataEntries[i][iweight][ifreq][iphase].getValue();
			ata.setEntry(iunknown, junknown, ataij);
			if (junknown != iunknown)
				ata.setEntry(junknown, iunknown, ataij);
		});
		
		return ata;
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
//	private static AtAEntry create(byte[] bytes, WeightingType[] weightingTypes, double[][] frequencyRanges
//			, Phases[] phases, PartialType[] partialTypes, Location[] locations) {
//		ByteBuffer bb = ByteBuffer.wrap(bytes);
//		WeightingType weightingType = weightingTypes[bb.getShort()];
//		double[] frequencyRange = frequencyRanges[bb.getShort()];
//		Phases phase = phases[bb.getShort()];
//		PartialType partialType1 = partialTypes[bb.getShort()];
//		PartialType partialType2 = partialTypes[bb.getShort()];
//		Location location1 = locations[bb.getShort()];
//		Location location2 = locations[bb.getShort()];
//		double value = bb.getDouble();
//		
////		Set<Phase> tmpset = new HashSet<>();
////		for (int i = 0; i < 10; i++) {
////			short iphase = bb.getShort();
////			if (iphase != -1)
////				tmpset.add(phases[iphase]);
////		}
////		Phase[] usablephases = new Phase[tmpset.size()];
////		usablephases = tmpset.toArray(usablephases);
//		
//		return new AtAEntry(weightingType, frequencyRange, phase, partialType1, partialType2, location1, location2, value);
//	}
	
	private static AtAEntry[][][][] create(byte[][] bytes, WeightingType[] weightingTypes, FrequencyRange[] frequencyRanges
			, Phases[] phases, UnknownParameter[] unknownParameters) {
		
//		int nUnknown = unknownParameters.length;
//		int n0AtA = (nUnknown + 1) * nUnknown / 2;
//		if (n0AtA * weightingTypes.length * frequencyRanges.length * phases.length != bytes.length)
//			throw new RuntimeException("Unexpected: size of byte array and calculated size of AtA mismatch");
//		AtAEntry[][][][] ataEntries = new AtAEntry[n0AtA][weightingTypes.length][frequencyRanges.length][phases.length];
		
		int n1 = weightingTypes.length * frequencyRanges.length * phases.length;
		int n0AtA = bytes.length / n1;
		
		AtAEntry[][][][] ataEntries = new AtAEntry[n0AtA][weightingTypes.length][frequencyRanges.length][phases.length];
		
		for (int i = 0; i < bytes.length; i++) {
			byte[] tmpbytes = bytes[i];
			ByteBuffer bb = ByteBuffer.wrap(tmpbytes);
			int iWeight = bb.getShort();
			WeightingType weightingType = weightingTypes[iWeight];
			int iFreq = bb.getShort();
			FrequencyRange frequencyRange = frequencyRanges[iFreq];
			int iPhase = bb.getShort();
			Phases phase = phases[iPhase];
			int iUnknonw1 = bb.getShort();
			UnknownParameter unknown1 = unknownParameters[iUnknonw1];
			int iUnknonw2 = bb.getShort();
			UnknownParameter unknown2 = unknownParameters[iUnknonw2];
			double value = bb.getDouble();
			
//			int i0AtA = iUnknonw1 * (iUnknonw1 + 1) / 2 + (iUnknonw2);
//			ataEntries[i0AtA][iWeight][iFreq][iPhase] 
//					= new AtAEntry(weightingType, frequencyRange, phase, unknown1, unknown2, value);
			
//			System.out.println(i0AtA + " " + i);
			
			int i0AtA = i / n1;
			
			ataEntries[i0AtA][iWeight][iFreq][iPhase]
					= new AtAEntry(weightingType, frequencyRange, phase, unknown1, unknown2, value);
			
		}
		
//		Set<Phase> tmpset = new HashSet<>();
//		for (int i = 0; i < 10; i++) {
//			short iphase = bb.getShort();
//			if (iphase != -1)
//				tmpset.add(phases[iphase]);
//		}
//		Phase[] usablephases = new Phase[tmpset.size()];
//		usablephases = tmpset.toArray(usablephases);
		
		return ataEntries;
	}
	
	public static AtAEntry[][][][] add(AtAEntry[][][][] ata1, AtAEntry[][][][] ata2) {
		int n0AtA = ata1.length;
		int nWeight = ata1[0].length;
		int nFreq = ata1[0][0].length;
		int nPhase = ata1[0][0][0].length;
		AtAEntry[][][][] ataSum = new AtAEntry[n0AtA][nWeight][nFreq][nPhase];
		
		for (int i0AtA = 0; i0AtA < n0AtA; i0AtA++) {
			for (int iweight = 0; iweight < nWeight; iweight++) {
				for (int ifreq = 0; ifreq < nFreq; ifreq++) {
					for (int iphase = 0; iphase < nPhase; iphase++) {
						AtAEntry ataEntry = ata1[i0AtA][iweight][ifreq][iphase];
						ataEntry.add(ata2[i0AtA][iweight][ifreq][iphase]);
						ataSum[i0AtA][iweight][ifreq][iphase] = ataEntry;
					}
				}
			}
		}
		
		return ataSum;
	}
	
	public static AtAEntry[][][][] multiply(AtAEntry[][][][] ata, double[] w) {
		int n0AtA = ata.length;
		int nWeight = ata[0].length;
		int nFreq = ata[0][0].length;
		int nPhase = ata[0][0][0].length;
		AtAEntry[][][][] ataMul = new AtAEntry[n0AtA][nWeight][nFreq][nPhase];
		
		for (int i0AtA = 0; i0AtA < n0AtA; i0AtA++) {
			int iunknown = (int) (0.5 * (FastMath.sqrt(1 + 8 * i0AtA) - 1));
			int junknown = i0AtA - iunknown * (iunknown + 1) / 2;
			for (int iweight = 0; iweight < nWeight; iweight++) {
				for (int ifreq = 0; ifreq < nFreq; ifreq++) {
					for (int iphase = 0; iphase < nPhase; iphase++) {
						AtAEntry ataEntry = ata[i0AtA][iweight][ifreq][iphase];
						ataEntry.setValue(ataEntry.getValue() * w[iunknown] * w[junknown]);
						ataMul[i0AtA][iweight][ifreq][iphase] = ataEntry;
					}
				}
			}
		}
		
		return ataMul;
	}
	
	public static RealMatrix multiply(RealMatrix ata, double[] w) {
		int n = ata.getColumnDimension();
		RealMatrix ataMul = new Array2DRowRealMatrix(n, n);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				ataMul.setEntry(i, j, ata.getEntry(i, j) * w[i] * w[j]);
			}
		}
		return ataMul;
	}
	
}
