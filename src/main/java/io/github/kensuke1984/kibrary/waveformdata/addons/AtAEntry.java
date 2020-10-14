package io.github.kensuke1984.kibrary.waveformdata.addons;

//import org.omg.CORBA.FREE_MEM;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.addons.WeightingType;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.addons.FrequencyRange;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

/**
 * @author Anselme Borgeaud
 *
 */
public class AtAEntry {
	
	private WeightingType weightingType;
	
	private FrequencyRange frequencyRange;
	
	private Phases phases;
	
	private UnknownParameter unknown1;
	
	private UnknownParameter unknown2;
	
	private double value;
	
	private int UID;
	
	public static void main(String[] args) {
		// verify memory requirements
//		System.out.println("Size of the AtA matrix = (nWeightingType * nFrequencyRanges * nUnknowns * 30 (buffer for different phases))^2");
		//
	}
	
	public boolean isDefault() {
		if (value == 0.)
			return true;
		else
			return false;
	}
	
	public AtAEntry(WeightingType weightingType, FrequencyRange frequencyRange, Phases phases,
			UnknownParameter unknown1, UnknownParameter unknown2, double value) {
		this.weightingType = weightingType;
		this.frequencyRange = frequencyRange;
		this.phases = phases;
		this.unknown1 = unknown1;
		this.unknown2 = unknown2;
		this.value = value;
		
		this.UID = 1;
	}
	
	public AtAEntry(WeightingType weightingType, FrequencyRange frequencyRange, Phases phases,
			UnknownParameter unknown1, UnknownParameter unknown2) {
		this(weightingType, frequencyRange, phases, unknown1, unknown2, 0);
	}
	
	public double getValue() {
		return value;
	}
	
	public UnknownParameter getUnknown1() {
		return unknown1;
	}
	
	public UnknownParameter getUnknown2() {
		return unknown2;
	}
	
	public Phases getPhases() {
		return phases;
	}
	
	public WeightingType getWeightingType() {
		return weightingType;
	}
	
	public FrequencyRange getFrequencyRange() {
		return frequencyRange;
	}
	
	public boolean isSameRange(AtAEntry another) {
		if (another.frequencyRange.equals(frequencyRange))
			return true;
		else
			return false;
	}
	
	public boolean isSameParameter(AtAEntry another) {
		if (!another.getUnknown1().equals(unknown1) || !another.getUnknown2().equals(unknown2))
			return false;
		return true;
	}
	
	public void add(AtAEntry another) {
		if (UID != another.UID)
			throw new IllegalArgumentException("");
		value += another.value;
	}
	
	public void setValue(double value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return weightingType.name() + " " + frequencyRange + " "
		+ phases.toString() + " " + unknown1 + " " + unknown2 + " " + value;
	}
}
