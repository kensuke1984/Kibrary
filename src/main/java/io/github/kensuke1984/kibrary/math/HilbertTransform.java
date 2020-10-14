package io.github.kensuke1984.kibrary.math;

import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class HilbertTransform {
	double[] y;
	double[] Hy;
	Complex[] AnalyticalEnvelope;
	
	protected final FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
	
	public static void main(String[] args) {
		int n = 1000;
		double[] y = new double[n];
		for (int i = 0; i < n; i++)
			y[i] = Math.sin(2 * Math.PI / 50. * i / 10.) * Math.sin(2 * Math.PI / 5 * i / 10.);
		
		HilbertTransform transform = new HilbertTransform(y);
		
		double[] Hy = transform.getHy();
		
		transform.computeAnalyticalHtransform();
		
		double envelope[] = transform.getEnvelope();
		double phase[] = transform.getInstantaneousPhase();
		
		for (int i = 0; i < n; i++)
			System.out.println(i + " " + y[i] + " " + envelope[i] +  " " + phase[i] / 90.);
	}
	
	public HilbertTransform(double[] y) {
		this.y = y.clone();
		this.Hy = new double[y.length];
		
		int npowOf2 = Integer.highestOneBit(y.length) * 2;
		double[] ytaped = ApplyTaper(y);
		double[] ypadded = Arrays.copyOf(ytaped, npowOf2);
		
		Complex[] Fy = fft.transform(ypadded, TransformType.FORWARD);
		
		double domega = 2 * Math.PI / npowOf2;
		
//		Fy[0] = Complex.ZERO;
		for (int i = 1; i < npowOf2; i++) {
			double a = 0;
			if (i < npowOf2 / 2)
				a = -1;
			else if (i > npowOf2 / 2)
				a = 1;
			Fy[i] = Fy[i].multiply(new Complex(0, -a));
		}
		
//		double[] RealFy = new double[npowOf2];
//		double[] ImagFy = new double[npowOf2];
//		for (int i = 0; i < npowOf2; i++) {
//			RealFy[i] = Fy[i].getReal();
//			ImagFy[i] = Fy[i].getImaginary();
//		}
//		RealFy = ApplySineTaper(RealFy);
//		ImagFy = ApplySineTaper(ImagFy);
//		Complex[] Fytapped = new Complex[npowOf2];
//		for (int i = 0; i < npowOf2; i++)
//			Fytapped[i] = new Complex(RealFy[i], ImagFy[i]);
		
		Complex[] Hypadded = fft.transform(Fy, TransformType.INVERSE);
		double[] RealHypadded = new double[npowOf2];
		for (int i = 0; i < npowOf2; i++)
			RealHypadded[i] = Hypadded[i].getReal();
		this.Hy = Arrays.copyOf(RealHypadded, y.length);
	}
	
	public void computeAnalyticalHtransform() {
		AnalyticalEnvelope = new Complex[y.length];
		for (int i = 0; i < y.length; i++)
			AnalyticalEnvelope[i] = new Complex(y[i], Hy[i]);
	}
	
	public double[] getEnvelope() {
		if (AnalyticalEnvelope == null)
			computeAnalyticalHtransform();
		double[] envelope = new double[y.length];
		for (int i = 0; i < y.length; i++)
			envelope[i] = AnalyticalEnvelope[i].abs();
		return envelope;
	}
	
	public double[] getNormalizedFourthPowEnvelope() {
		if (AnalyticalEnvelope == null)
			computeAnalyticalHtransform();
		double[] envelope = new double[y.length];
		double maxEnvelope = 0.;
		for (int i = 0; i < y.length; i++) {
			double a = AnalyticalEnvelope[i].abs();
			if (a > maxEnvelope)
				maxEnvelope = a;
			envelope[i] = a * a * a  * a;
		}
		if (maxEnvelope > 0) {
			for (int i = 0; i < y.length; i++)
				envelope[i] /= maxEnvelope;
		}
		return envelope;
	}
	
	public double[] getInstantaneousPhase() {
		if (AnalyticalEnvelope == null)
			computeAnalyticalHtransform();
		double[] phase = new double[y.length];
		for (int i = 0; i < y.length; i++) {
			Complex c = AnalyticalEnvelope[i];
			phase[i] = Math.atan(c.getImaginary() / c.getReal()) * 180 / Math.PI;
		}
		return phase;
	}
	
	private double[] ApplyTaper(double y[]) {
		double[] taped = y.clone();
		int width = 4;//(int) (y.length / 50.);
		if (width < 2)
			width = 2;
		for (int i = 0; i < y.length; i++) {
			double f = 1;
			if (i < width)
				f = (double) i / width;
			else if (i > y.length - width - 1)
				f = (double) (y.length - i - 1) / width;
			taped[i] *= f ;
		}
		return taped;
	}
	
	public double[] getHy() {
		return this.Hy;
	}
	
	public Complex[] getAy() {
		return this.AnalyticalEnvelope;
	}
}