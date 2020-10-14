package io.github.kensuke1984.kibrary.math;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVectorFormat;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class FourierTransform {
	double[] y;
	Complex[] Fy;
	double[] amp;
	double[] phase;
	double[] reFy;
	double[] imFy;
	double df_point;
	
	protected final FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
	
	public static void main(String[] args) {
		int n = 1024;
		double[] y = new double[n];
//		for (int i = 0; i < n; i++)
//			y[i] = Math.sin(2 * Math.PI / 50. * i / 10.) * Math.sin(2 * Math.PI / 5 * i / 10.);
		for (int i = 0; i < n; i++)
			y[i] = Math.sin(2 * Math.PI / 50. * i) + 2 * Math.sin(2 * Math.PI / 20. * i);
		
		FourierTransform transform = new FourierTransform(y);
		
		Complex[] Fy = transform.getFy();
		
		double a[] = transform.getAOfOmega();
		double phase[] = transform.getPhaseOfOmega();
		
		double df = transform.getFreqIncrement(1.);
		
		System.out.println(2*Math.PI / 50.);
		System.out.println(2*Math.PI / 20.);
		for (int i = 0; i < n; i++)
			System.out.println(i + " " + i*df + " " + y[i] + " " + a[i] +  " " + phase[i] / 90.);
	}
	
	public FourierTransform(double[] y) {
		this.y = y.clone();
		amp = new double[y.length];
		phase = new double[y.length];
		reFy = new double[y.length];
		imFy = new double[y.length];
		
		int npowOf2 = Integer.highestOneBit(y.length) * 2;
		double[] ytaped = ApplyTaper(y);
		double[] ypadded = Arrays.copyOf(ytaped, npowOf2);
		
		Complex[] Fypadded = fft.transform(ypadded, TransformType.FORWARD);
		
		df_point = 1. / npowOf2;
		
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
		
		this.Fy = Arrays.copyOf(Fypadded, y.length);
		
		for (int i = 0; i < y.length; i++) {
			amp[i] = Fy[i].abs();
			reFy[i] = Fy[i].getReal();
			imFy[i] = Fy[i].getImaginary();
		}
	}
	
	public FourierTransform(double[] y, int reSamplingHz) {
		amp = new double[y.length * reSamplingHz];
		phase = new double[y.length * reSamplingHz];
		reFy = new double[y.length * reSamplingHz];
		imFy = new double[y.length * reSamplingHz];
		
		int npowOf2 = Integer.highestOneBit(y.length) * 2;
		double[] ytaped = ApplyTaper(y);
		double[] ypadded = Arrays.copyOf(ytaped, npowOf2);
		
		ypadded = resample(ypadded, reSamplingHz);
		npowOf2 *= reSamplingHz;
		
		Complex[] Fypadded = fft.transform(ypadded, TransformType.FORWARD);
		
		df_point = 1. / npowOf2;
		
		this.Fy = Arrays.copyOf(Fypadded, y.length);
		
		for (int i = 0; i < y.length; i++) {
			amp[i] = Fy[i].abs();
			reFy[i] = Fy[i].getReal();
			imFy[i] = Fy[i].getImaginary();
		}
	}
	
	private static double[] resample(double[] y, int reSamplingHz) {
		return Arrays.copyOf(y, y.length * reSamplingHz);
	}
	
	private static double[] removeMean(double[] y) {
		double mean = new ArrayRealVector(y).dotProduct(new ArrayRealVector(y.length).mapAdd(1.)) / y.length;
		return new ArrayRealVector(y).mapAdd(-mean).toArray();
	}
	
	public double getFreqIncrement(double samplingHz) {
		return df_point * samplingHz;
	}
	
	private static final int SACSamplingHz = 20;
	
	private double[] ApplyTaper(double y[]) {
		double[] taped = y.clone();
//		int width = 4;
//		int width = (int) (y.length / 10.);
//		if (width < 4)
//			width = 4;
		int width = SACSamplingHz * 1;
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
	
	public double[] getAOfOmega() {
		return amp;
	}
	
	public double[] getLogA() {
		return new ArrayRealVector(amp).map(Math::log).toArray();
	}
	
	public double[] getRealFy() {
		return reFy;
	}
	
	public double[] getImFy() {
		return imFy;
	}
	
	public double[] getPhaseOfOmega() {
		return phase;
	}
	
	public Complex[] getFy() {
		return Fy;
	}
	
}