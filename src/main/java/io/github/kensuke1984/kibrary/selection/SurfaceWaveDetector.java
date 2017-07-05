package io.github.kensuke1984.kibrary.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.kensuke1984.kibrary.math.HilbertTransform;
import io.github.kensuke1984.kibrary.timewindow.Timewindow;
import io.github.kensuke1984.kibrary.util.Trace;

public class SurfaceWaveDetector {
	Trace trace;
	Trace envelopeTrace;
	Trace twoBitTrace;
	Timewindow timewindow;
	final static double dt = 0.05;
	
	public SurfaceWaveDetector(Trace trace) {
		this.trace = new Trace(trace.getX(), trace.getY());
		
		HilbertTransform transform = new HilbertTransform(this.trace.getY());
		transform.computeAnalyticalHtransform();
		envelopeTrace = new Trace(this.trace.getX(), transform.getEnvelope());
		
		int[] points = detect();
		timewindow = new Timewindow(points[0] * dt, points[1] * dt);
	}
	
	private int[] detect() {
		final int nIteration = 0;
		double threshold = envelopeTrace.getMaxValue() / 2.;
		double[] y = envelopeTrace.getY();
		double[] twoBits = computeTwoBitArray(y, threshold);
		int[] iLongestSequence = detectLongestOneSequence(twoBits);
		int length = iLongestSequence[1] - iLongestSequence[0];
//		int addWidth = (int) (100 / dt);
//		y = Arrays.copyOfRange(y, iLongestSequence[0] - addWidth, iLongestSequence[1] + 1 + addWidth);
		for (int i = 0; i < nIteration; i++) {
			threshold /= 1.3;
			double[] twoBitsNew = computeTwoBitArray(y, threshold);
			int[] iLongestSequenceNew = detectLongestOneSequence(twoBitsNew);
			int lengthNew = iLongestSequenceNew[1] - iLongestSequenceNew[0];
			if (lengthNew / length < 1.1)
				break;
			else {
				length = lengthNew;
				iLongestSequence = iLongestSequenceNew;
			}
		}
		return iLongestSequence;
	}
	
	private static double[] computeTwoBitArray(double[] y, double threshold) {
		double[] twoBitY = new double[y.length];
		
		for (int i = 0; i < y.length; i++) {
			if (y[i] > threshold)
				twoBitY[i] = 1.;
			else
				twoBitY[i] = 0.;
		}
		
		return twoBitY;
	}
	
	private int[] detectLongestOneSequence(double[] y) {
		int[] indexesOfLongest = new int[2];
		List<Integer> ups = new ArrayList<>();
		List<Integer> downs = new ArrayList<>();
		for (int i = 0; i < y.length - 1; i++) {
			if (y[i] == 0 && y[i+1] == 1)
				ups.add(i);
			else if (y[i] == 1 && y[i+1] == 0)
				downs.add(i);
		}
		int maxLength = 0;
		for (int i = 0; i < downs.size(); i++) {
			int length = downs.get(i) - ups.get(i);
			if (length > maxLength) {
				maxLength = length;
				indexesOfLongest[0] = ups.get(i);
				indexesOfLongest[1] = downs.get(i);
			}
		}
//		System.out.println(ups.size() + " " + downs.size() + " : " + indexesOfLongest[0]*dt + " " + indexesOfLongest[1]*dt);
		return indexesOfLongest;
	}
	
	public Timewindow getSurfaceWaveWindow() {
		return timewindow;
	}
}