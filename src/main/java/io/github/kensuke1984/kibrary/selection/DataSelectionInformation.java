package io.github.kensuke1984.kibrary.selection;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;

public class DataSelectionInformation {
	
	private double variance;
	
	private double cc;
	
	private double maxRatio;
	
	private double minRatio;
	
	private double absRatio;
	
	private double SNratio;
	
	private TimewindowInformation timewindow;
	
	
	public DataSelectionInformation(TimewindowInformation timewindow, double variance, double cc, double maxRatio, double minRatio, double absRatio, double SNratio) {
		this.timewindow = timewindow;
		this.variance = variance;
		this.cc = cc;
		this.maxRatio = maxRatio;
		this.minRatio = minRatio;
		this.absRatio = absRatio;
		this.SNratio = SNratio;
	}
	
	public double getVariance() {
		return variance;
	}
	
	public double getCC() {
		return cc;
	}
	
	public double getMaxRatio() {
		return maxRatio;
	}
	
	public double getMinRatio() {
		return minRatio;
	}
	
	public double getAbsRatio() {
		return absRatio;
	}
	
	public double getSNratio() {
		return SNratio;
	}
	
	public TimewindowInformation getTimewindow() {
		return timewindow;
	}
	
	@Override
	public String toString() {
		List<String> phaseStrings = Stream.of(timewindow.getPhases()).filter(phase -> phase != null).map(Phase::toString).collect(Collectors.toList());
		String twString = timewindow.getStartTime() + " " + timewindow.getEndTime() + " " + timewindow.getStation().getName() + " " +
				timewindow.getStation().getPosition() + " " + timewindow.getStation().getNetwork() + " " + timewindow.getGlobalCMTID() + " " + timewindow.getComponent() + " " +
				String.join(",", phaseStrings);
		return twString + " " + maxRatio + " " + minRatio + " " + absRatio + " " +
				variance + " " + cc + " " + SNratio;
	}
}
