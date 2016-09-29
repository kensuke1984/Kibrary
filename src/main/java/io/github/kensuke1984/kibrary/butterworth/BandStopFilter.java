/**
 * 
 */
package io.github.kensuke1984.kibrary.butterworth;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexUtils;
import org.apache.commons.math3.util.FastMath;

/**
 * Bandstop filter
 * @author Kensuke Konishi
 * 
 * @version 0.0.5.1
 * 
 * 
 */
public class BandStopFilter extends ButterworthFilter {

	/**
	 * 高周波透過域の最小角周波数 minimum &omega; for higher permissible region
	 */
	private double omegaH;
	/**
	 * 低周波透過域の最大角周波数 maximum &omega; for lower permissible region
	 */
	private double omegaL;
	/**
	 * 遮断域の最大角周波数 maximum &omega; for blocking region
	 */
	private double omegaSh;
	/**
	 * 遮断域の最小角周波数 minimum &omega; for blocking region
	 */
	private double omegaSl;

	public double getOmegaH() {
		return omegaH;
	}

	public double getOmegaL() {
		return omegaL;
	}

	public double getOmegaSh() {
		return omegaSh;
	}

	public double getOmegaSl() {
		return omegaSl;
	}

	/**
	 * &omega; = 2&pi;f&Delta;t
	 * 
	 * @param ap
	 *            透過域の最小振幅 （1+A<sub>p</sub><sup>2</sup>）<sup>-1</sup>
	 * @param as
	 *            遮断域の最大振幅 （1+A<sub>s</sub><sup>2</sup>）<sup>-1</sup>
	 * @param omegaH
	 *            &omega;<sub>H</sub> 高周波透過域の最小角周波数 minimum &omega; for higher
	 *            permissible region
	 * @param omegaL
	 *            &omega;<sub>L</sub> 低周波透過域の最大角周波数 maximum &omega; for lower
	 *            permissible region
	 * @param omegaSh
	 *            &omega;<sub>Sh</sub> 遮断域の最大角周波数 maximum &omega; for blocking
	 *            region
	 */
	public BandStopFilter(double ap, double as, double omegaH, double omegaL, double omegaSh) {
		this.ap = ap;
		this.as = as;
		this.omegaH = omegaH;
		this.omegaL = omegaL;
		this.omegaSh = omegaSh;
		if (!omegaValid())
			throw new IllegalArgumentException("Input parameters are invalid");
		if (omegaH <= omegaSh || omegaSh <= omegaL)
			throw new IllegalArgumentException("Input parameters are invalid");
		setSigmaSoverSigmaP();
		setN();
		setC();
		setOmegaSl();
		setLambda02();
		createRecursiveFilter();
		printParameters();
	}

	/**
	 * @return if input &omega;<sub>H</sub> and &omega;<sub>L</sub> are valid
	 */
	private boolean omegaValid() {
		boolean valid = true;
		double halfPI = 0.5 * Math.PI;
		if (omegaH < 0 || halfPI <= omegaH) {
			System.out.println("omegaH: " + omegaH + " is invalid");
			valid = false;
		}
		if (omegaL < 0 || halfPI <= omegaL) {
			System.out.println("omegaL: " + omegaL + " is invalid");
			valid = false;
		}
		if (omegaH <= omegaL) {
			System.out.println("omegaH, omegaL: " + omegaH + ", " + omegaL + " are invalid");
			valid = false;
		}
		return valid;
	}

	/**
	 * &omega; = 2&pi;f&Delta;t <br>
	 * ap 透過域の最小振幅（1+A<sub>p</sub><sup>2</sup>）<sup>-1</sup>: 0.9<br>
	 * as 遮断域の最大振幅(1+A<sub>s</sub><sup>2</sup>)<sup>-1</sup>: 0.1
	 * 
	 * @param omegaH
	 *            &omega;<sub>H</sub> 高周波透過域の最小角周波数 minimum &omega; for higher
	 *            permissible region
	 * @param omegaL
	 *            &omega;<sub>L</sub> 低周波透過域の最大角周波数 maximum &omega; for lower
	 *            permissible region
	 * @param n
	 *            n pole
	 */
	public BandStopFilter(double omegaH, double omegaL, int n) {
		ap = 1 / 3.0;
		as = 3;
		this.omegaH = omegaH;
		this.omegaL = omegaL;
		if (!omegaValid())
			throw new IllegalArgumentException("Input parameters are invalid");
		this.n = n;
		nToSigmaSoverSigmaP(n);
		computeOmegaShSl();
		// System.out.println(n);
		// n=10;
		setC();
		setLambda02();
		createRecursiveFilter();
		printParameters();

	}

	public void printParameters() {
		double permeability = 1 / (1 + ap * ap);
		double cut = 1 / (1 + as * as);
		System.out.println("filter permiability, cut:" + permeability + "  " + cut);
		System.out.println("band stop (Hz):" + omegaL / 2.0 / Math.PI / 0.05 + "  " + omegaH / 2.0 / Math.PI / 0.05);
		System.out
				.println("cut region (Hz): " + omegaSl / 2.0 / Math.PI / 0.05 + ",  " + omegaSh / 2.0 / Math.PI / 0.05);
		System.out.println("backword " + backward);
	}

	private void createRecursiveFilter() {
		// b0 = new double[n / 2];
		b1 = new double[n];
		b2 = new double[n];
		g = 1;
		for (int j = 0; j < n / 2 * 2; j++) {
			Complex lambdaJ = computeLambdaJ(j + 1);
			double muJ = lambdaJ.getReal();
			double nuJ = lambdaJ.getImaginary();
			double b0 = (c + nuJ) * (c + nuJ) + muJ * muJ;
			b1[j] = -2 * (c * c - lambdaJ.abs() * lambdaJ.abs());
			b2[j] = (c - nuJ) * (c - nuJ) + muJ * muJ;
			// System.out.println(j + " b0 " + b0 + " " + getB0(j + 1));
			// System.out.println(j + " b1 " + b1[j] + " " + getB1(j + 1));
			// System.out.println(j + " b2 " + b2[j] + " " + getB2(j + 1));
			g *= c * c + lambda02;
			g /= b0;
			b1[j] /= b0;
			b2[j] /= b0;
			System.out.println(g + " " + b1[j] + " " + b2[j]);
		}
		// System.out.println(n + " " + n / 2 * 2);
		if (n % 2 == 1) {
			// System.out.println("i");
			int j = n - 1;
			// Math.sin((2 * m + 1) / 2.0 / n * Math.PI)
			// double t = Math.sin((n / 2 * 2 + 1) / 2.0 / n * Math.PI);
			// double b0 = c * c + c * t + lambda02;
			double b0 = c * c + c + lambda02;
			g *= c * c + lambda02;
			g /= b0;
			b1[j] = -2 * (c * c - lambda02);
			b2[j] = c * c - c + lambda02;
			// b2[j] = c * c - c * t + lambda02;
			// System.out.println(j + " b0 " + b0 + " " + getB0(0));
			// System.out.println(j + " b1 " + b1[j] + " " + getB1(0));
			// System.out.println(j + " b2 " + b2[j] + " " + getB2(0));
			b1[j] /= b0;
			b2[j] /= b0;
		}
		a1 = -2 * (c * c - lambda02) / (c * c + lambda02);
	}

	private double a1;

	/**
	 * A root of &lambda;<sup>2</sup>+&sigma;<sub>j</sub><sup>-1</sup>
	 * &lambda;-&lambda; <sub>0</sub><sup>2</sup>=0
	 * 
	 * @param j
	 *            j =1, 2, ..., n
	 * @return
	 */
	private Complex computeLambdaJ(int j) {
		int jj = (n / 2) < j ? j - n / 2 : j;
		// sigmaJ**-1
		Complex sigmaJ = ComplexUtils.polar2Complex(1, -Math.PI * (2 * jj - 1) / (2 * n));
		// sigmaJ = sigmaJ.reciprocal();
		Complex lambdaJ = n / 2 < j ? sigmaJ.negate().add(((sigmaJ.pow(2)).add(4 * lambda02)).sqrt())
				: sigmaJ.negate().subtract(((sigmaJ.pow(2)).add(4 * lambda02)).sqrt());
		lambdaJ = lambdaJ.divide(2);

		// System.out.println(j+" compute "+lambdaJ);
		return lambdaJ;
	}

	/**
	 * &lambda;<sub>0</sub><sup>2</sup>
	 */
	private double lambda02;

	private void setLambda02() {
		lambda02 = c * c * FastMath.tan(omegaH / 2) * FastMath.tan(omegaL / 2);
	}

	private void setOmegaSl() {
		omegaSl = 2 * FastMath.atan(FastMath.tan(omegaH / 2) * FastMath.tan(omegaL / 2) / FastMath.tan(omegaSh / 2));
		// System.out.println(omegaSl);
	}

	/**
	 * computes (2.34)
	 */
	private void setC() {
		double c2 = FastMath.pow(ap * as, -1.0 / n) / (FastMath.tan(omegaH / 2) - FastMath.tan(omegaL / 2));
		c2 /= FastMath.abs(FastMath.tan(omegaSh / 2)
				- FastMath.tan(omegaH / 2) * FastMath.tan(omegaL / 2) / FastMath.tan(omegaSh / 2));
		c = FastMath.sqrt(c2);
	}

	@Override
	void setSigmaSoverSigmaP() {
		double tanH = FastMath.tan(0.5 * omegaH);
		double tanL = FastMath.tan(0.5 * omegaL);
		double tanSh = FastMath.tan(0.5 * omegaSh);
		sigmaSoverSigmaP = (tanH - tanL) / FastMath.abs(tanSh - tanH * tanL / tanSh);
		// System.out.println(sigmaSoverSigmaP);
	}

	/**
	 * By input n, computes Sh
	 */
	private void computeOmegaShSl() {
		double tanH = FastMath.tan(0.5 * omegaH);
		double tanL = FastMath.tan(0.5 * omegaL);
		double mul = tanH * tanL;
		double minus = tanH - tanL;
		double x = (minus + FastMath.sqrt(minus * minus + 4 * sigmaSoverSigmaP * sigmaSoverSigmaP * mul)) / 2
				/ sigmaSoverSigmaP;
		double y = (minus - FastMath.sqrt(minus * minus + 4 * sigmaSoverSigmaP * sigmaSoverSigmaP * mul)) / 2
				/ sigmaSoverSigmaP;
		omegaSh = FastMath.atan(x) * 2;
		omegaSl = FastMath.atan(y) * 2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see manhattan.butterworth.ButterworthFilter#getFrequencyResponce(double)
	 */
	@Override
	public Complex getFrequencyResponce(double omega) {
		Complex responce = Complex.valueOf(g);
		Complex numerator = Complex.valueOf(a1 + 2 * FastMath.cos(omega));
		for (int j = 0; j < n / 2 * 2; j++) {
			// System.out.println("yo");
			// Saito 1.7
			// Hj = (a1j +(a2j+1) cos ω -i(a2j-1)sinω)
			// /(b2j+1) cos ω -i(b2j-1)sinω)
			Complex denominator = Complex.valueOf(b1[j] + FastMath.cos(omega) * (b2[j] + 1),
					-FastMath.sin(omega) * (b2[j] - 1));
			responce = responce.multiply(numerator).divide(denominator);
		}
		if (n % 2 == 1) {
			int j = n - 1;
			// Complex numerator = Complex.valueOf(1 + Math.cos(omega),
			// Math.sin(omega));
			Complex denominator = Complex.valueOf(b1[j] + FastMath.cos(omega) * (b2[j] + 1),
					-FastMath.sin(omega) * (b2[j] - 1));
			responce = responce.multiply(numerator).divide(denominator);
		}
		if (backward)
			responce = Complex.valueOf(responce.abs() * responce.abs());

		return responce;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * manhattan.butterworth.ButterworthFilter#applyFilter(org.apache.commons
	 * .math3.complex.Complex[])
	 */
	@Override
	public Complex[] applyFilter(Complex[] data) {
		Complex[] y = new Complex[data.length];

		System.arraycopy(data, 0, y, 0, data.length);
		// Complex[] x = data;
		for (int j = 0; j < n / 2 * 2; j++) {
			// System.out.println("yo");
			Complex[] x = y;
			y = computeRecursion(b1[j], b2[j], x);
		}
		if (n % 2 == 1) {
			// System.out.println("orz");
			int j = n - 1;
			Complex[] x = y;
			y = computeRecursion(b1[j], b2[j], x);
		}
		System.out.println("G" + g + " n" + n);
		for (int i = 0; i < y.length; i++)
			y[i] = y[i].multiply(g);
		// System.out.println(n);
		backward = false;
		if (backward) {
			Complex[] reverseY = new Complex[y.length];
			for (int i = 0; i < y.length; i++)
				reverseY[i] = y[y.length - i - 1];

			for (int j = 0; j < n / 2 * 2; j++) {
				// System.out.println("yo");
				Complex[] x = reverseY;
				reverseY = computeRecursion(b1[j], b2[j], x);
			}
			if (n % 2 == 1) {
				// System.out.println("orz");
				int j = n - 1;
				Complex[] x = reverseY;
				reverseY = computeRecursion(b1[j], b2[j], x);
			}
			for (int i = 0; i < reverseY.length; i++)
				reverseY[i] = reverseY[i].multiply(g);
			for (int i = 0; i < y.length; i++)
				y[i] = reverseY[y.length - i - 1];
		}

		return y;
	}

	/**
	 * y[t]=a0x[t]+a1x[t-1]+a2x[t-2]-b1y[t-1]-b2y[t-2] <br>
	 * a0 =1, a1, a2 = 1
	 * 
	 * @param a1
	 * @param a2
	 * @param b1
	 * @param b2
	 * @param x
	 * @return {@link Complex}[] y
	 */
	private Complex[] computeRecursion(double b1, double b2, Complex[] x) {
		Complex[] y = new Complex[x.length];

		y[0] = x[0];
		// y[0] = x[0].multiply(a0);
		y[1] = x[1].add(x[0].multiply(a1)).subtract(y[0].multiply(b1));
		// y[1] = x[1].multiply(a0).add(x[0].multiply(a1))
		// .subtract(y[0].multiply(b1));
		// System.out.println(a1+" "+b1+" "+b2);
		for (int i = 2; i < x.length; i++)
			y[i] = x[i].add(x[i - 1].multiply(a1)).add(x[i - 2]).subtract(y[i - 1].multiply(b1))
					.subtract(y[i - 2].multiply(b2));
		// y[i] = x[i].multiply(a0).add(x[i - 1].multiply(a1))
		// .add(x[i - 2].multiply(a2)).subtract(y[i - 1].multiply(b1))
		// .subtract(y[i - 2].multiply(b2));
		return y;
	}

}
