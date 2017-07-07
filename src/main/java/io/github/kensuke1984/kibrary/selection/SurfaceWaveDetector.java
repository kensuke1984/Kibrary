package io.github.kensuke1984.kibrary.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.butterworth.LowPassFilter;
import io.github.kensuke1984.kibrary.math.HilbertTransform;
import io.github.kensuke1984.kibrary.timewindow.Timewindow;
import io.github.kensuke1984.kibrary.util.Trace;

public class SurfaceWaveDetector {
	private Trace trace;
	private double minPeriod;
	private Trace envelopeTrace;
	private Trace twoBitsTrace;
	private int[] upBits;
	private int[] downBits;
	private Timewindow timewindow;
	private final static double dt = 0.05;
	
	public SurfaceWaveDetector(Trace trace, double minPeriod) {
		this.trace = new Trace(trace.getX(), trace.getY());
		this.minPeriod = minPeriod;
		
		HilbertTransform transform = new HilbertTransform(this.trace.getY());
		transform.computeAnalyticalHtransform();
		envelopeTrace = new Trace(this.trace.getX(), transform.getEnvelope());
		
		int[] points = detect();
		if (points != null)
			timewindow = new Timewindow(points[0] * dt, points[1] * dt);
	}
	
	private int[] detect() {
		int[] surfaceWavePoints = null;
		// Initial rough detection using amplitude threshold
		double threshold = envelopeTrace.getMaxValue() * 0.1;
		if (threshold == 0) {
			System.out.println("Warning: zero trace; return null");
			return null;
		}
		double minLength = minPeriod / dt * 4;
		double[] y = envelopeTrace.getY();
		
		double[] twoBits = computeTwoBitArray(y, threshold);
		// detect first peak
		int iStart = -1;
		int iEnd = -1;
		for (int i = 0; i < twoBits.length; i++) {
			if (twoBits[i] == 0 && twoBits[i+1] == 1)
				iStart = i;
			if (iStart != -1)
				if (twoBits[i] == 1 && twoBits[i+1] == 0) {
					iEnd = i;
					break;
				}
		}
		double maxFirstPeak = envelopeTrace.cutWindow(envelopeTrace.getXAt(iStart), envelopeTrace.getXAt(iEnd))
				.getYVector().getLInfNorm();
		threshold = maxFirstPeak * 0.1;
		
		twoBits = computeTwoBitArray(y, threshold);
		
		twoBitsTrace = new Trace(envelopeTrace.getX(), twoBits);
		
		int iLongestUp = detectLongestOneSequence(twoBits, minLength);
		if (iLongestUp == -1)
			return null;
		
		double mergingTimelength = 4.;
		
		int i = iLongestUp;
		int startPoint = upBits[iLongestUp];
		if (i > 0) {
			boolean condition = (upBits[i] - downBits[i-1]) * dt < mergingTimelength; 
			while (condition && i > 1) {
				i--;
				condition = (upBits[i] - downBits[i-1]) * dt < mergingTimelength;
			}
			if (i == 1 && condition)
				i--;
			startPoint = upBits[i];
		}
		
		i = iLongestUp;
		int endPoint = downBits[iLongestUp];
		if (i < upBits.length - 1) {
			boolean condition = (upBits[i+1] - downBits[i]) * dt < mergingTimelength;
			while (condition && i < upBits.length - 2) {
				i++;
				condition = (upBits[i+1] - downBits[i]) * dt < mergingTimelength;
			}
			if (i == upBits.length - 2 && condition)
				i++;
			endPoint = downBits[i];
		}
		
		surfaceWavePoints = new int[] {startPoint, endPoint};
		
		return surfaceWavePoints;
	}
	
	private static double[] computeTwoBitArray(double[] y, double threshold) {
		double[] twoBitY = new double[y.length];
		
		for (int i = 0; i < y.length; i++) {
//			System.out.println(y[i] + " " + threshold);
			if (y[i] > threshold)
				twoBitY[i] = 1.;
			else
				twoBitY[i] = 0.;
		}
		
		return twoBitY;
	}
	
	private int detectLongestOneSequence(double[] y, double minLength) {
		int indexesOfLongestUp = -1;
		List<Integer> ups = new ArrayList<>();
		List<Integer> downs = new ArrayList<>();
		if (y[0] == 1)
			ups.add(0);
		for (int i = 0; i < y.length - 1; i++) {
			if ((y[i] == 0) && (y[i+1] == 1))
				ups.add(i);
			if ((y[i] == 1) && (y[i+1] == 0))
				downs.add(i+1);
		}
		
		int maxLength = 0;
		int n = ups.size();
		if (ups.size() != downs.size()) {
			System.out.println("Warning: different number of ups=" 
					+ ups.size() + " and downs=" + downs.size());
			n = Math.min(ups.size(), downs.size());
		}
		upBits = new int[n];
		downBits = new int[n];
		
		for (int i = 0; i < n; i++) {
			upBits[i] = ups.get(i);
			downBits[i] = downs.get(i);
			int length = downs.get(i) - ups.get(i) + 1;
//			System.out.println(length + " " + minLength);
			if (length >= minLength && length > maxLength) {
				maxLength = length;
				indexesOfLongestUp = i;
			}
		}
//		System.out.println(ups.size() + " " + downs.size() + " : " + indexesOfLongest[0]*dt + " " + indexesOfLongest[1]*dt);
		return indexesOfLongestUp;
	}
	
	public Timewindow getSurfaceWaveWindow() {
		return timewindow;
	}
	
	public Trace getTwoBitsTrace() {
		return twoBitsTrace;
	}
}