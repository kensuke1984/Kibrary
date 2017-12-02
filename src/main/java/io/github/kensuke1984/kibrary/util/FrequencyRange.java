package io.github.kensuke1984.kibrary.util;

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
	    if (!Utilities.equalWithinEpsilon(minFreq, other.minFreq, 1e-8))
	    	return false;
	    if (!Utilities.equalWithinEpsilon(maxFreq, other.maxFreq, 1e-8))
	    	return false;
	    return true;
	}

	@Override
	public int hashCode() {
	    int hash = 3;
	    hash = 53 * hash + (this.minFreq != 0 ? (int) (1. / this.minFreq) : 100000);
	    hash = 53 * hash + (int) (1. / this.maxFreq);
	    return hash;
	}
	
	public String toString() {
		return String.format("%.3f %.3f", minFreq, maxFreq);
	}
}
