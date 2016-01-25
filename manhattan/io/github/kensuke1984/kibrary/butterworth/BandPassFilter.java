package io.github.kensuke1984.kibrary.butterworth;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexUtils;
import org.apache.commons.math3.util.FastMath;

/**
 * 斎藤正徳 漸化式ディジタルフィルタ<br>
 * |B(σ)|<sup>2</sup> = 1/(1+σ<sup>2n</sup>)<br>
 * B(σ)= Π{1/i(σ-σ<sub>j</sub>)} <br>
 * バンドパス実装を目指す 透過域の振幅は1/(1+ap<sup>2</sup>)以上<br>
 * 遮断域の振幅は1/(1+as<sup>2</sup>)以下<br>
 * ω=2πfδt
 * 
 * 
 * @author Kensuke
 * 
 * @version 0.1.3.1
 * 
 */
public class BandPassFilter extends ButterworthFilter {

	/**
	 * @return &omega;<sub>H</sub>
	 */
	public double getOmegaH() {
		return omegaH;
	}

	/**
	 * @return &omega;<sub>L</sub>
	 */
	public double getOmegaL() {
		return omegaL;
	}

	/**
	 * @return &omega;<sub>Sh</sub>
	 */
	public double getOmegaSh() {
		return omegaSh;
	}

	/**
	 * @return &omega;<sub>Sl</sub>
	 */
	public double getOmegaSl() {
		return omegaSl;
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
	 * &omega; = 2&pi;f&Delta;t
	 * 
	 * @param ap
	 *            透過域の最小振幅 （1+A<sub>p</sub><sup>2</sup>）<sup>-1</sup>
	 * @param as
	 *            遮断域の最大振幅 （1+A<sub>s</sub><sup>2</sup>）<sup>-1</sup>
	 * @param omegaH
	 *            透過域の最大角周波数 maximum &omega; for permissible region
	 * @param omegaL
	 *            透過域の最小角周波数 minimum &omega; for permissible region
	 * @param omegaSh
	 *            大きい側の遮断域の最小角周波数 minimum &omega; for blocking region in higher
	 *            part
	 */
	public BandPassFilter(double ap, double as, double omegaH, double omegaL, double omegaSh) {
		this.ap = ap;
		this.as = as;
		this.omegaH = omegaH;
		this.omegaL = omegaL;
		this.omegaSh = omegaSh;
		if (!omegaValid())
			throw new IllegalArgumentException("Input omegas are invalid");
		if (omegaSh <= omegaH || 0.5 * Math.PI <= omegaSh)
			throw new IllegalArgumentException("Input parameters are invalid");
		setSigmaSoverSigmaP();
		setN();
		setC();
		setOmegaSl();
		setLambda02();
		createRecursiveFilter();
		// printParameters();
	}

	/**
	 * ap 透過域の最小振幅（1+A<sub>p</sub><sup>2</sup>）<sup>-1</sup>: 0.9<br>
	 * as 遮断域の最大振幅(1+A<sub>s</sub><sup>2</sup>)<sup>-1</sup>: 0.1
	 * 
	 * @param omegaH
	 *            透過域の最大角周波数
	 * @param omegaL
	 *            透過域の最小角周波
	 * @param n
	 *            フィルターの個数から
	 */
	public BandPassFilter(double omegaH, double omegaL, int n) {
		this.n = n;
		this.ap = 1 / 3.0;
		this.as = 3;
		this.omegaH = omegaH;
		this.omegaL = omegaL;
		if (!omegaValid())
			throw new IllegalArgumentException("Input parameters are invalid");
		nToSigmaSoverSigmaP(n);
		computeOmegaShSl();
		// setSigmaSvsSigmaP();
		// setN();
		setC();
		// setOmegaSl();
		setLambda02();
		createRecursiveFilter();
		// printParameters();
	}

	@Override
	public String toString() {
		double permeability = 1 / (1 + ap * ap);
		double cut = 1 / (1 + as * as);
		return "filter permiability, cut: " + permeability + "  " + cut + "\n" + "band pass (Hz):"
				+ omegaL / 2.0 / Math.PI / 0.05 + "  " + omegaH / 2.0 / Math.PI / 0.05 + "\n" + "cut region (Hz): "
				+ omegaSl / 2.0 / Math.PI / 0.05 + ",  " + omegaSh / 2.0 / Math.PI / 0.05 + "backword " + backward;
	}

	/**
	 * 透過帯域の最大|ω|<omegaHは透過
	 */
	private double omegaH;
	/**
	 * 透過帯域の最小|ω|>omegaLは透過
	 */
	private double omegaL;
	/**
	 * 遮断帯域のスタート|ω| >omegashは遮断
	 */
	private double omegaSh;
	/**
	 * 遮断帯域のスタート|ω|< omegaslは遮断
	 */
	private double omegaSl;

	/**
	 * &lambda;<sub>0</sub><sup>2</sup>
	 */
	private double lambda02;

	/**
	 * By eq. 2.25, computes {@link #sigmaSoverSigmaP}
	 */
	@Override
	void setSigmaSoverSigmaP() {
		double tanH = FastMath.tan(omegaH * 0.5);
		double tanL = FastMath.tan(omegaL * 0.5);
		double tanSh = FastMath.tan(omegaSh * 0.5);
		sigmaSoverSigmaP = FastMath.abs(tanSh - tanH * tanL / tanSh) / (tanH - tanL);
	}

	/**
	 * By {@link #n}, computes {@link #omegaSh} and {@link #omegaSl}.
	 */
	private void computeOmegaShSl() {
		double tanH = FastMath.tan(omegaH * 0.5);
		double tanL = FastMath.tan(omegaL * 0.5);
		double minus = tanH - tanL;
		double mul = tanH * tanL;
		// System.out.println(sigmaSvsSigmaP+"sigma "+minus);
		double x = sigmaSoverSigmaP * minus * 0.5
				+ 0.5 * FastMath.sqrt(sigmaSoverSigmaP * sigmaSoverSigmaP * minus * minus + 4 * mul);
		double y = sigmaSoverSigmaP * minus * 0.5
				- 0.5 * FastMath.sqrt(sigmaSoverSigmaP * sigmaSoverSigmaP * minus * minus + 4 * mul);
		omegaSh = FastMath.abs(FastMath.atan(x) * 2);
		omegaSl = FastMath.abs(FastMath.atan(y) * 2);

	}

	private void setC() {
		double c2 = FastMath.pow(ap * as, 1.0 / n) / (FastMath.tan(omegaH / 2) - FastMath.tan(omegaL / 2));
		c2 /= FastMath.abs(FastMath.tan(omegaSh / 2)
				- FastMath.tan(omegaH / 2) * FastMath.tan(omegaL / 2) / FastMath.tan(omegaSh / 2));
		c = FastMath.sqrt(c2);
	}

	private void setOmegaSl() {
		omegaSl = 2 * FastMath.atan(FastMath.tan(omegaH / 2) * FastMath.tan(omegaL / 2) / FastMath.tan(omegaSh / 2));
		// System.out.println(omegaSl);
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
			g *= c;
			g /= b0;
			b1[j] /= b0;
			b2[j] /= b0;
			// System.out.println(b1[j] + " " + b2[j]);
		}
		// System.out.println(n + " " + n / 2 * 2);
		if (n % 2 == 1) {
			// System.out.println("i");
			int j = n - 1;
			// Math.sin((2 * m + 1) / 2.0 / n * Math.PI)
			double t = FastMath.sin((n / 2 * 2 + 1) / 2.0 / n * Math.PI);
			double b0 = c * c + c * t + lambda02;
			g *= c;
			g /= b0;
			b1[j] = -2 * (c * c - lambda02);
			b2[j] = c * c - c * t + lambda02;
			// System.out.println(j + " b0 " + b0 + " " + getB0(0));
			// System.out.println(j + " b1 " + b1[j] + " " + getB1(0));
			// System.out.println(j + " b2 " + b2[j] + " " + getB2(0));
			b1[j] /= b0;
			b2[j] /= b0;
		}
	}

	@Override
	public Complex getFrequencyResponce(double omega) {
		Complex responce = Complex.valueOf(g);
		Complex numerator = Complex.valueOf(0, 2 * FastMath.sin(omega));
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
		// System.out.println("G" + g);
		for (int i = 0; i < y.length; i++)
			y[i] = y[i].multiply(g);

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
	 * a0 =1, a1= 0, a2 = -1
	 * 
	 * @param a1
	 * @param a2
	 * @param b1
	 * @param b2
	 * @param x
	 * @return {@link Complex}[] y
	 */
	private static Complex[] computeRecursion(double b1, double b2, Complex[] x) {
		Complex[] y = new Complex[x.length];

		y[0] = x[0];
		// y[0] = x[0].multiply(a0);
		y[1] = x[1].subtract(y[0].multiply(b1));
		// y[1] = x[1].multiply(a0).add(x[0].multiply(a1))
		// .subtract(y[0].multiply(b1));
		for (int i = 2; i < x.length; i++)
			y[i] = x[i].subtract(x[i - 2]).subtract(y[i - 1].multiply(b1)).subtract(y[i - 2].multiply(b2));
		// y[i] = x[i].multiply(a0).add(x[i - 1].multiply(a1))
		// .add(x[i - 2].multiply(a2)).subtract(y[i - 1].multiply(b1))
		// .subtract(y[i - 2].multiply(b2));
		return y;
	}

	private void setLambda02() {
		lambda02 = c * c * FastMath.tan(omegaH / 2) * FastMath.tan(omegaL / 2);
	}

	/**
	 * A root of &lambda;<sup>2</sup>-&sigma;<sub>j</sub>&lambda;-&lambda;
	 * <sub>0</sub><sup>2</sup>=0
	 * 
	 * @param j
	 *            j =1, 2, ..., n
	 * @return
	 */
	private Complex computeLambdaJ(int j) {

		int jj = (n / 2) < j ? j - n / 2 : j;
		Complex sigmaJ = ComplexUtils.polar2Complex(1, Math.PI * (2 * jj - 1) / (2 * n));
		Complex lambdaJ = n / 2 < j ? sigmaJ.add(((sigmaJ.pow(2)).add(4 * lambda02)).sqrt())
				: sigmaJ.subtract(((sigmaJ.pow(2)).add(4 * lambda02)).sqrt());
		lambdaJ = lambdaJ.divide(2);

		// System.out.println(j+" compute "+lambdaJ);
		return lambdaJ;
	}

}
