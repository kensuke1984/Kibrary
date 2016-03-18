/**
 * 
 */
package io.github.kensuke1984.kibrary.stacking;

import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;

/**
 * 
 * Stacking by the peak-to-peak average arrival time and amplitude. ピーク２ピークの中間時刻
 * 平均絶対振幅で規格化
 * 
 * @version 0.0.1.1
 * @author Kensuke Konishi
 *
 */
public class PeakStack implements Stack {

	private double delta;

	public PeakStack() {
		delta = 0.05;
	}

	@Override
	public Trace stack(String stationName, GlobalCMTID globalCMTID, SACComponent component, WaveformType type,
			Trace trace) {
		RealVector x = trace.getXVector();
		for (int i = 1; i < x.getDimension(); i++) {
			double interval = x.getEntry(i) - x.getEntry(i - 1);
			double gap = Math.abs(interval - delta);
			if (10e-10 < gap)
				throw new RuntimeException("Input Trace has invalid x interval.");
		}
		RealVector y = trace.getYVector();
		double peakX = (trace.getXforMaxValue() + trace.getXforMinValue()) / 2;
		// System.out.println(trace.getXforMaxValue()+"
		// "+trace.getXforMinValue());
		double peakY = (trace.getMaxValue() - trace.getMinValue()) / 2;
		x = x.mapSubtract(peakX);
		y = y.mapDivide(peakY);
		return new Trace(x.toArray(), y.toArray());
	}

}
