package io.github.kensuke1984.kibrary.util.addons;

import io.github.kensuke1984.kibrary.util.Utilities;

public class FrequencyRange {
	private double minFreq;
	private double maxFreq;
	
	public FrequencyRange(double minFreq, double maxFreq) {
		this.minFreq = minFreq;
		this.maxFreq = maxFreq;
	}
	
	public double getMinFreq() {
		return minFreq;
	}
	
	public double getMaxFreq() {
		return maxFreq;
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (obj == null) {
	        return false;
	    }
	    if (!FrequencyRange.class.isAssignableFrom(obj.getClass())) {
	        return false;
	    }
	    final FrequencyRange other = (FrequencyRange) obj;
	    if (!Utilities.equalWithinEpsilon(minFreq, other.minFreq, eps))
	    	return false;
	    if (!Utilities.equalWithinEpsilon(maxFreq, other.maxFreq, eps))
	    	return false;
	    return true;
	}
	
	final static double eps = 1e-3;

	@Override
	public int hashCode() {
	    int hash = 3;
	    hash = 53 * hash + (this.minFreq != 0 ? (int) (1. / this.minFreq) : 100000);
	    hash = 53 * hash + (int) (1. / this.maxFreq);
	    return hash;
	}
	
	public String toString() {
		return String.format("%.3f-%.3f", minFreq, maxFreq);
	}
	
	public double[] toPeriodRange() {
		return new double[] {1./maxFreq, 1./minFreq};
	}
}
