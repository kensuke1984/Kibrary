package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.kibrary.inversion.Physical3DParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.addons.WeightingType;
import io.github.kensuke1984.kibrary.util.addons.FrequencyRange;
import io.github.kensuke1984.kibrary.util.addons.Phases;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class AtAHeader {
	WeightingType[] weightingTypes;
	FrequencyRange[] frequencyRanges;
	Phases[] phases;
	UnknownParameter[] unknownParameters;
	
	public static final int oneWeightingTypeByte = 4;
	public static final int onePhasesByte = 20;
	public static final int onePartialTypeByte = 10;
	public static final int oneUnknownParameterByte = Physical3DParameter.oneUnknownByte;
	
	public AtAHeader(WeightingType[] weightingTypes, FrequencyRange[] frequencyRanges
			, Phases[] phases, UnknownParameter[] unknownParameters) {
		this.weightingTypes = weightingTypes;
		this.frequencyRanges = frequencyRanges;
		this.phases = phases;
		this.unknownParameters = unknownParameters;
	}
	
	public static AtAHeader readHeader(Path ataPath) throws IOException {
		WeightingType[] weightingTypes;
		FrequencyRange[] frequencyRanges;
		Phases[] phases;
		UnknownParameter[] unknownParameters;
		
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(ataPath)));) {
			// Read header
			weightingTypes = new WeightingType[dis.readShort()];
			frequencyRanges = new FrequencyRange[dis.readShort()];
			phases = new Phases[dis.readShort()];
			unknownParameters = new UnknownParameter[dis.readShort()];
			
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
		}
		
		return new AtAHeader(weightingTypes, frequencyRanges, phases, unknownParameters);
	}
	
	public WeightingType[] getWeightingTypes() {
		return weightingTypes;
	}
	
	public FrequencyRange[] getFrequencyRanges() {
		return frequencyRanges;
	}
	
	public Phases[] getPhases() {
		return phases;
	}
	
	public UnknownParameter[] getUnknownParameters() {
		return unknownParameters;
	}
}
