/**
 * 
 */
package io.github.kensuke1984.kibrary.datacorrection;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.TransformType;

import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.butterworth.LowPassFilter;
import io.github.kensuke1984.kibrary.stacking.PeakStack;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

/**
 * Source time function estimation by stacked peaks.
 * 
 * @author Kensuke Konishi
 * @version 0.0.3.3
 *
 */
public final class SourceTimeFunctionByStackedPeaks extends SourceTimeFunction {

	private SACData[] obsSacs;
	private SACData[] synSacs;

	private double range = 10; // sec

	/**
	 * Ratio of peak to peak (Observed/Synthetic)
	 */
	private double[] ampRatio;
	
	private Trace obsFinalStack;
	
	/**
	 * @param np
	 *            the number of steps in frequency domain
	 * @param tlen
	 *            the time length
	 * @param samplingHz
	 *            must be 20 now
	 * @param obsSacs
	 *            same order as synSacs
	 * @param synSacs
	 *            same order as obsSacs
	 * @param timewindow
	 *            search region for Peaks
	 */
	public SourceTimeFunctionByStackedPeaks(int np, double tlen, double samplingHz, SACData[] obsSacs,
			SACData[] synSacs, Set<TimewindowInformation> timewindow) {
		super(np, tlen, samplingHz);
		if (!pairCheck(obsSacs, synSacs))
			throw new RuntimeException("Input sac files are invalid.");
		this.timewindow = timewindow;
		this.obsSacs = obsSacs;
		this.synSacs = synSacs;
	}

	private PeakStack ps = new PeakStack();
	private Set<TimewindowInformation> timewindow;

	private Trace toStackTrace(Trace trace) {
		return ps.stack(null, null, null, null, trace);
	}

	private Trace createTrace(SACData sacFile) {
		Station station = sacFile.getStation();
		GlobalCMTID id = new GlobalCMTID(sacFile.getSACString(SACHeaderEnum.KEVNM));
		SACComponent component = SACComponent.of(sacFile);

		TimewindowInformation window = null;
		
		try {
			window = timewindow.stream().filter(info -> info.getStation().equals(station)
				&& info.getGlobalCMTID().equals(id) && info.getComponent() == component).findAny().get();
		} catch (NoSuchElementException e) {
			return null;
		}

		return sacFile.createTrace().cutWindow(window);
	}

	/**
	 * @param obsSacs
	 *            Arrays of observed
	 * @param synSacs
	 *            Arrays of synthetics
	 * @return if the input pair is valid
	 */
	private static boolean pairCheck(SACData[] obsSacs, SACData[] synSacs) {
		if (obsSacs.length != synSacs.length) {
			System.out.println("The length of observed and synthetics is different.");
			return false;
		}
		for (int i = 0; i < obsSacs.length; i++) {
			if (!obsSacs[i].getSACString(SACHeaderEnum.KEVNM).equals(synSacs[i].getSACString(SACHeaderEnum.KEVNM)))
				return false;
			if (!Station.of(obsSacs[i]).equals(Station.of(synSacs[i])))
				return false;
			if (obsSacs[i].getValue(SACHeaderEnum.USER1) != synSacs[i].getValue(SACHeaderEnum.USER1)
					|| obsSacs[i].getValue(SACHeaderEnum.USER2) != synSacs[i].getValue(SACHeaderEnum.USER2))
				return false;
		}

		return true;
	}

	private int findLsmooth() {
		int lsmooth = (int) (0.5 * tlen * samplingHz / np);
		int i = Integer.highestOneBit(lsmooth);
		if (i < lsmooth)
			i *= 2;
		return i;
	}

	private void compute() {
		Complex[] sourceTimeFunction = new Complex[np];
		Trace[] obsTraces = Arrays.stream(obsSacs).map(this::createTrace).filter(trace -> trace != null).map(trace -> trace.removeTrend()).toArray(Trace[]::new);
		Trace[] synTraces = Arrays.stream(synSacs).map(this::createTrace).filter(trace -> trace != null).map(trace -> trace.removeTrend()).toArray(Trace[]::new);
		ampRatio = new double[obsTraces.length];
		for (int i = 0; i < obsTraces.length; i++) {
			double obsP2P = obsTraces[i].getMaxValue() - obsTraces[i].getMinValue();
			double synP2P = synTraces[i].getMaxValue() - synTraces[i].getMinValue();
			ampRatio[i] = obsP2P / synP2P;
		}

		Trace[] obsStackTraces = Arrays.stream(obsTraces).map(this::toStackTrace).toArray(Trace[]::new);
		Trace[] synStackTraces = Arrays.stream(synTraces).map(this::toStackTrace).toArray(Trace[]::new);

		int n = (int) (range * 2 * samplingHz) + 1;
		double[] obsStack = new double[n];
		double[] synStack = new double[n];
		for (int i = 0; i < n; i++) {
			double t = -range + i / samplingHz;
			for (int iTrace = 0; iTrace < obsStackTraces.length; iTrace++) {
				obsStack[i] += obsStackTraces[iTrace].toValue(0, t);
				synStack[i] += synStackTraces[iTrace].toValue(0, t) / ampRatio[iTrace];
			}
			// System.out.println(t + " " + obsStack[i] + " " + synStack[i]);
		}
		// System.exit(0);
		int npts = 1 * np * findLsmooth();
		// System.out.println(tlen * 20);
		double[] obsUt = new double[npts];
		double[] synUt = new double[npts];
		for (int i = 0; i < n; i++) {
			obsUt[i] = obsStack[i];
			synUt[i] = synStack[i];
		}

		//taper
		double[][] tmp = smoothEdge(obsUt, synUt);
		obsUt = tmp[0];
		synUt = tmp[1];

		Complex[] obsUf = fft.transform(obsUt, TransformType.FORWARD);
		Complex[] synUf = fft.transform(synUt, TransformType.FORWARD);
		
		double fmax = 1./12.5;
		double fmin = 1./200;
		double omegaH = fmax * 2 * Math.PI / samplingHz;
		double omegaL = fmin * 2 * Math.PI / samplingHz;
		ButterworthFilter filter = new BandPassFilter(omegaH, omegaL, 4);
		
		double halfDuration = obsSacs[0].getGlobalCMTID().getEvent().getHalfDuration();
		Complex[] triangle = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, samplingHz, halfDuration).getSourceTimeFunctionInFrequencyDomain();
		
		int length = (int) (tlen/8.);
		System.out.println(length);
		for (int i = 0; i < np; i++) {
			double omega = 2 * Math.PI * (i+1) / tlen / samplingHz;
			sourceTimeFunction[i] = obsUf[i + 1].divide(synUf[i + 1]);
//			sourceTimeFunction[i] = sourceTimeFunction[i].multiply(taper_hanning(i, np));
//			sourceTimeFunction[i] = sourceTimeFunction[i].multiply(filter.getFrequencyResponce(omega));
//			System.out.println(i + " " + filter.getFrequencyResponce(omega));
		}
//		sourceTimeFunction = filter.applyFilter(sourceTimeFunction);
		sourceTimeFunction = taperToOne(sourceTimeFunction, length);
//		length = 436;
		for (int i = 0; i < np; i++) {
			double omega = 2 * Math.PI * (i+1) / tlen / samplingHz;
//			sourceTimeFunction[i] = sourceTimeFunction[i].multiply(filter.getFrequencyResponce(omega));
//			sourceTimeFunction[i] = sourceTimeFunction[i];
		}
//		sourceTimeFunction = taper_right(sourceTimeFunction, length, np);
		
		double[] xs = IntStream.range(0, npts).mapToDouble(i -> i/samplingHz).toArray();
		this.obsFinalStack = new Trace(xs, obsUt);
		
		this.sourceTimeFunction = fitTriangleSourceTimeFunction();
		System.out.println(obsSacs[0].getGlobalCMTID() + " " + this.halfDuration + " " + obsSacs[0].getGlobalCMTID().getEvent().getHalfDuration());
		
		try {
			Path outputpath = Paths.get("/work/anselme/CA_ANEL_NEW/syntheticPREM/filtered_4-200s/stf/check_" + obsSacs[0].getGlobalCMTID());
			PrintWriter pw = new PrintWriter(outputpath.toFile());
			for (int i = 0; i < np; i++)
				pw.println(obsUf[i].abs() + " " + synUf[i].abs() + " " + obsUf[i].abs()/synUf[i].abs() + " " + sourceTimeFunction[i].abs() + " " + triangle[i].abs());
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private double[][] smoothEdge(double[] obs, double[] syn) {
		double[][] smoothed = new double[2][];
		final double eps = 1e-5;
		
		int width = 1 * (int) samplingHz;
		
		int iEnd = (int) samplingHz * 50;
		int iSmooth = -1;
		int iBuff = 0;
		for (int i = iEnd; i >= 0; i--) {
			if (Math.abs(obs[i]) > eps && Math.abs(syn[i]) > eps) {
//				System.out.println(i/samplingHz + " " + obs[i]);
				iBuff++;
			}
			if (iBuff == 3) {
				iSmooth = i + 3;
				break;
			}
		}
		System.out.println(iSmooth / samplingHz);
		
		smoothed[0] = new double[obs.length];
		smoothed[1] = new double[syn.length];
		
		for (int i = 0; i <= iSmooth; i++) {
//			double f = taper(i, iSmooth, width);
			double f = taper_hanning(i, iSmooth);
			smoothed[0][i] = obs[i] * f;
			smoothed[1][i] = syn[i] * f;
		}
		
		return smoothed;
	}
	
	private double taper(int i, int iEnd, int widthInPoints) {
		double f = 1.;
		if (i < widthInPoints)
			f = i * 1. / widthInPoints;
		else if (i > iEnd - widthInPoints)
			f = 1. + (iEnd - widthInPoints - i) / (double) widthInPoints;
		return f;
	}
	
	private double taper_right(int i, int iStart, int length) {
		double f = 1.;
		if (i > iStart) {
			f = 1. + (iStart - i) / (double) (length - iStart);
		}
		return f;
	}
	
	private Complex[] taper_right(Complex[] stf, int iStart, int length) {
		Complex[] res = stf.clone();
		for (int i = 0; i < length; i++) {
			if (i > iStart) {
				res[i] = stf[iStart].multiply(1 - (i - iStart) / (double) (length - iStart));
			}
		}
		return res;
	}
	
	private Complex[] taperToOne(Complex[] stf, int length) {
		int width = 70;
		double y = stf[length - width].abs();
		int i0 = 100;
		double y0 = stf[i0].abs();
		Complex[] res = stf.clone();
		for (int i = 0; i < stf.length; i++) {
			if (i < i0)
				res[i] = new Complex((1 - y0) * 0.5 * (1 - Math.cos(Math.PI / (i0) * (i+i0))) + y0);
			else if (i >= length - width && i <= length) {
//				res[i] = new Complex((1 - y)/width * (i-length+width) + y);
				res[i] = new Complex((y - 1) * 0.5 * (1 - Math.cos(Math.PI / (width) * (i - (length - width)+width))) + 1);
			}
			else if (i > length)
				res[i] = new Complex(1.);
		}
		return res;
	}
	
	private double taper_hanning(int i, int length) {
		return 0.5 * (1 - Math.cos(2*Math.PI / (length-1) * i));
	}
	
	private double taper_hanning_right(int i, int length) {
		double res = 0.5 * (1 - Math.cos(Math.PI / (length) * (i+length)));
		if (i > length)
			res = 0;
		return res;
	}
	
//	@Override
//	public Complex[] getSourceTimeFunctionInFrequencyDomain() {
//		if (sourceTimeFunction != null)
//			return sourceTimeFunction;
//		synchronized (this) {
//			if (sourceTimeFunction != null)
//				return sourceTimeFunction;
//			compute();
//		}
//		return sourceTimeFunction;
//	}
	
	@Override
	public Complex[] getSourceTimeFunctionInFrequencyDomain() {
		if (sourceTimeFunction != null)
			return sourceTimeFunction;
		synchronized (this) {
			if (sourceTimeFunction != null)
				return sourceTimeFunction;
			compute();
		}
		return sourceTimeFunction;
	}

	@Override
	public Trace getSourceTimeFunctionInTimeDomain() {
		if (sourceTimeFunction != null)
			return super.getSourceTimeFunctionInTimeDomain();
		synchronized (this) {
			if (sourceTimeFunction != null)
				return super.getSourceTimeFunctionInTimeDomain();
			compute();
		}
//		return super.getSourceTimeFunctionInTimeDomain();
		return this.obsFinalStack;
	}
	
	private double halfDuration;
	
	private Complex[] fitTriangleSourceTimeFunction() {
		halfDuration = Math.abs(obsFinalStack.getXforMaxValue() - obsFinalStack.getXforMinValue());
		Complex[] stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, samplingHz, halfDuration)
				.getSourceTimeFunctionInFrequencyDomain();
//		for (int i = 0; i < np; i++)
//			stf[i] = stf[i].multiply(1.);
		return stf;
	}
	
}
