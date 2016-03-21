package io.github.kensuke1984.kibrary.util.spc;

import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.FastMath;

import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;

/**
 * 
 * Data for one element in one {@link SpcBody} in a {@link SpectrumFile}
 * 
 * @version 0.1.5.2
 * 
 * @author Kensuke Konishi
 * 
 */
public class SpcComponent {

	public static final int effectivDigit = 6;

	/**
	 * @return DEEP copy of this
	 */
	public SpcComponent copy() {
		SpcComponent s = new SpcComponent(np);
		s.nptsInTimeDomain = nptsInTimeDomain;
		System.arraycopy(uFreq, 0, s.uFreq, 0, uFreq.length);
		if (uTime != null)
			s.uTime = uTime.clone();
		return s;
	}

	/**
	 * number of step in frequency domain
	 */
	private final int np;

	/**
	 * number of datapoints in time domain
	 */
	private int nptsInTimeDomain;

	/**
	 * 周波数領域のデータ u[i] i=[0, np]
	 */
	private Complex[] uFreq;

	/**
	 * 時間領域のデータ u[i] i=[0,nptsInTimedomain-1]
	 */
	private Complex[] uTime;

	SpcComponent(int np) {
		this.np = np;
		uFreq = new Complex[np + 1];
	}

	/**
	 * 各ipの値に対するスペクトルを入力
	 * 
	 * set ip th step
	 * 
	 * @param ip
	 *            index of &omega;
	 * @param realPart
	 * @param imaginaryPart
	 */
	void set(int ip, Complex spec) {
		uFreq[ip] = spec;
	}

	/**
	 * body componentを足し合わせる
	 * 
	 * @param anotherComponent
	 *            additional {@link SpcComponent}
	 */
	public void addComponent(SpcComponent anotherComponent) {
		if (this.np != anotherComponent.getNp())
			throw new RuntimeException("Error: Size of body is not equal!");

		Complex[] another = anotherComponent.getValueInFrequencyDomain();
		for (int i = 0; i < np + 1; i++)
			uFreq[i] = uFreq[i].add(another[i]);

	}

	/**
	 * after toTimeDomain
	 * 
	 * @param tlen
	 *            time length
	 */
	public void amplitudeCorrection(double tlen) {
		double tmp = nptsInTimeDomain * 1e3 / tlen;
		for (int i = 0; i < nptsInTimeDomain; i++)
			uTime[i] = uTime[i].multiply(tmp);

	}

	/**
	 * after toTime tlen * (double) (i) / (double) nptsInTimeDomain;
	 * 
	 * @param omegai
	 *            &omega;<sub>i</sub>
	 * @param tlen
	 *            time length
	 */
	public void applyGrowingExponential(double omegai, double tlen) {
		double constant = omegai * tlen / nptsInTimeDomain;
		for (int i = 0; i < nptsInTimeDomain; i++)
			uTime[i] = uTime[i].multiply(FastMath.exp(constant * i));
	}

	/**
	 * before toTime This method applies ramped source time function.
	 * 
	 * @param sourceTimeFunction
	 *            to be applied
	 */
	public void applySourceTimeFunction(SourceTimeFunction sourceTimeFunction) {
		Complex[] sourceTimeFunctionU = sourceTimeFunction.getSourceTimeFunctionInFrequencyDomain();
		for (int i = 0; i < sourceTimeFunctionU.length; i++)
			uFreq[i + 1] = uFreq[i + 1].multiply(sourceTimeFunctionU[i]);
	}

	/**
	 * before toTime ufreq[i] * 2&times; &pi; &times i /tlen;
	 * 
	 * uFreq[i] = uFreq[i].multiply(new Complex(0, constant * i));
	 * @param tlen
	 *            time length
	 */
	void differentiate(double tlen) {
		final double constant = 2 * Math.PI / tlen;
		for (int i = 1; i <= np; i++)
			uFreq[i] = new Complex(-uFreq[i].getImaginary() * constant, uFreq[i].getReal() * constant);
	}

	/**
	 * @return 周波数領域のデータ
	 */
	public Complex[] getValueInFrequencyDomain() {
		return uFreq;
	}

	private int getNp() {
		return np;
	}

	/**
	 * @return the data in time_domain
	 */
	public double[] getTimeseries() {
		return Arrays.stream(uTime).mapToDouble(Complex::getReal).toArray();
	}

	private int getNPTS(int lsmooth) {
		int npts = np * lsmooth * 2;
		int pow2 = Integer.highestOneBit(npts);
		return pow2 < npts ? pow2 * 2 : npts;
	}

	public void toTimeDomain(int lsmooth) {
		nptsInTimeDomain = getNPTS(lsmooth);

		int nnp = nptsInTimeDomain / 2;

		FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

		// pack to temporary Complex array
		Complex[] data = new Complex[nptsInTimeDomain];
		System.arraycopy(uFreq, 0, data, 0, np + 1);

		// set blank due to lsmooth
		Arrays.fill(data, np + 1, nnp + 1, Complex.ZERO);

		// set values for imaginary frequency
		for (int i = 0; i < nnp - 1; i++)
			data[nnp + 1 + i] = data[nnp - 1 - i].conjugate();

		// fast fourier transformation
		data = fft.transform(data, TransformType.INVERSE);

		// put values in time domain into collections
		uTime = data;
	}

}
