package io.github.kensuke1984.kibrary.waveformdata;

import io.github.kensuke1984.kibrary.inversion.WeightingType;
import io.github.kensuke1984.kibrary.util.FrequencyRange;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

public class AtdEntry {
	
	private WeightingType weightingType;
	
	private FrequencyRange frequencyRange;
	
	private Phases phases;
	
	private PartialType partialType;
	
	private Location location;
	
	private double value;
	
	private int UID;
	
	public AtdEntry(WeightingType weightingType, FrequencyRange frequencyRange, Phases phases,
			PartialType partialType, Location location, double value) {
		this.weightingType = weightingType;
		this.frequencyRange = frequencyRange;
		this.phases = phases;
		this.partialType = partialType;
		this.location = location;
		this.value = value;
		
		this.UID = 1;
	}
	
	public double getValue() {
		return value;
	}
	
	public PartialType getPartialType() {
		return partialType;
	}
	
	public Location getLocation() {
		return location;
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
	
	public boolean isSameRange(AtdEntry another) {
		if (another.frequencyRange.equals(frequencyRange))
			return true;
		else
			return false;
	}
	
	public boolean isSameParameter(AtdEntry another) {
		if (!another.getPartialType().equals(partialType))
			return false;
		if (!another.getLocation().equals(location))
			return false;
		return true;
	}
	
	public boolean isSameLocations(AtdEntry another) {
		return another.getLocation().equals(location);
	}
	
	public void add(AtdEntry another) {
		if (UID != another.UID)
			throw new IllegalArgumentException("");
		value += another.value;
	}
	
	@Override
	public String toString() {
		return weightingType.name() + " " + frequencyRange + " "
		+ phases.toString() + " " + partialType.name() + " "
		+ location.toString() + " " + value;
	}
}
