package io.github.kensuke1984.kibrary.util.spc;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.FastMath;

import java.util.Arrays;

/**
 * Calculation of U<sub>j,q</sub> C<sub>jqrs</sub> &eta;<sub>ri,s</sub> in
 * Geller &amp; Hara (1993)
 *
 * @author Kensuke Konishi
 * @version 0.0.2.1
 */
class TensorCalculationUCE {

	/**
	 * Uj,q = u[j][q][(np)]
	 */
	private Complex[][][] u = new Complex[3][3][];

	/**
	 * i に対して都度計算するので iは引数に取らない Eta ri,s = eta （[i]）[r][s][(np)]
	 */
	private Complex[][][] eta = new Complex[3][3][];

	private WeightingFactor factor;

	private SPCBody fp;
	private SPCBody bp;

	private int np;

	/**
	 * bpのテンソル座標軸をfpの軸に合わせるための角度
	 */
	private double angle;

    /**
     * input cに対するテンソル積の和を計算する
     *
     * @param fp     forward propagation spc file
     * @param bp     back propagation spc file
     * @param factor どう重み付けするか
     * @param angle
     */
	TensorCalculationUCE(SPCBody fp, SPCBody bp, WeightingFactor factor, double angle) {
		this.fp = fp;
		this.bp = bp;
		np = fp.getNp();
		this.factor = factor;
		this.angle = angle;
	}

	/**
	 * (0, 1, 2) = (r, theta, phi) &rarr; (Z, X, Y) = (2, 0, 1)
	 * 
	 * @param i
	 * @return
	 */
	private static int switchCoordinateSystem(int i) {
		switch (i) {
		case 0:
			return 2;
		case 1:
			return 0;
		case 2:
			return 1;
		default:
			throw new IllegalArgumentException("Invalid integer");
		}

	}
	
    /**
     * Uj,q Cjqrs Eri,sのi成分の計算
     *
     * @param i (0: Z 1:R 2:T)
     * @return {@link Complex}[NP] i成分を返す
     */
	public Complex[] calc(int i) {
		Complex[] partial = new Complex[np + 1];
		Arrays.fill(partial, Complex.ZERO);

		for (int r = 0; r < 3; r++)
			for (int s = 0; s < 3; s++) {
				SPCTensorComponent irs = SPCTensorComponent.valueOfBP(i + 1, r + 1, s + 1);
				eta[r][s] = bp.getSpcComponent(irs).getValueInFrequencyDomain();
			}

		eta = rotateEta(eta);
		
		for (int p = 0; p < 3; p++)
			for (int q = 0; q < 3; q++) {
				SPCTensorComponent pq = SPCTensorComponent.valueOfFP(p + 1, q + 1);
				u[p][q] = fp.getSpcComponent(pq).getValueInFrequencyDomain();
				
				// u = rotate(u,anglefp);
				for (int r = 0; r < 3; r++)
					for (int s = 0; s < 3; s++) {
						// 球座標系とデカルト座標の調整
						double factor = getFactor(p, q, r, s);
						if (factor != 0)
							addPartial(partial, calcCrossCorrelation(u[p][q], eta[r][s]), factor);
					}
			}
		return partial;
	}
	
	public Complex[] calcSerial(int i) {
		Complex[] partial = new Complex[np + 1];
		Arrays.fill(partial, Complex.ZERO);

		for (int r = 0; r < 3; r++)
			for (int s = 0; s < 3; s++) {
				SPCTensorComponent irs = SPCTensorComponent.valueOfBP(i + 1, r + 1, s + 1);
				eta[r][s] = bp.getSpcComponent(irs).getValueInFrequencyDomain();
			}

		eta = rotateEta(eta);
		
		for (int p = 0; p < 3; p++)
			for (int q = 0; q < 3; q++) {
				SPCTensorComponent pq = SPCTensorComponent.valueOfFP(p + 1, q + 1);
				u[p][q] = fp.getSpcComponent(pq).getValueInFrequencyDomain();
				
				// u = rotate(u,anglefp);
				for (int r = 0; r < 3; r++)
					for (int s = 0; s < 3; s++) {
						// 球座標系とデカルト座標の調整
						double factor = getFactor(p, q, r, s);
						if (factor != 0)
							addPartial(partial, calcCrossCorrelationSerial(u[p][q], eta[r][s]), factor);
					}
			}
		return partial;
	}
	
	/**
	 * 球座標系pqrs(0, 1, 2)に対して 係数を求める (0, 1, 2) = (r, theta, phi) (->) (Z, X, Y) =
	 * (2, 0, 1)
	 * 
	 * @param p
	 * @param q
	 * @param r
	 * @param s
	 * @return
	 */
	private double getFactor(int p, int q, int r, int s) {
		return factor.getFactor(switchCoordinateSystem(p), switchCoordinateSystem(q), switchCoordinateSystem(r),
				switchCoordinateSystem(s));
	}

    /**
     * back propagateのローカル座標をforwardのものにあわせる
     *
     * @param eta eta[3][3][NP+1]
     * @param r
     * @return ETAir, s（back propagation） をテンソルのZ軸中心に {@link #angle} 回す
     */
	private Complex[][][] rotateEta(Complex[][][] eta) {

        // double angle = this.angle+Math.toRadians(195);
        /*
         * テンソル（eta）をangleだけ回転させ新しいテンソル(reta)を返す。
		 * 
		 * reta = forwardMatrix eta backmatrix
		 * 
		 * 中間値として neweta = forwardmatrix eta
		 * 
		 * reta = neweta backmatrix
		 */
        // angle= 0;
		double cosine = FastMath.cos(angle);
		double sine = FastMath.sin(angle);

		// 回転行列 前から
		double[][] forwardMatrix = new double[][] { { 1, 0, 0 }, { 0, cosine, sine }, { 0, -sine, cosine } };

		// 回転行列 後ろから
		double[][] backMatrix = new double[][] { { 1, 0, 0 }, { 0, cosine, -sine }, { 0, sine, cosine } };

		Complex[][][] newETA = new Complex[3][3][np + 1];

		for (int ip = 0; ip < np + 1; ip++)
			for (int r = 0; r < 3; r++)
				for (int s = 0; s < 3; s++) {
					newETA[r][s][ip] = Complex.ZERO;
					for (int k = 0; k < 3; k++)
						newETA[r][s][ip] = newETA[r][s][ip].add(eta[k][s][ip].multiply(forwardMatrix[r][k]));
				}

		Complex[][][] rETA = new Complex[3][3][np + 1];

		for (int ip = 0; ip < np + 1; ip++)
			for (int r = 0; r < 3; r++)
				for (int s = 0; s < 3; s++) {
					rETA[r][s][ip] = Complex.ZERO;
					for (int k = 0; k < 3; k++)
						rETA[r][s][ip] = rETA[r][s][ip].add(newETA[r][k][ip].multiply(backMatrix[k][s]));
				}

		return newETA;
	}

	/**
	 * uとEtaの計算をする（積） cross correlation
	 * 
	 * @param u
	 * @param eta
	 * @return c[i] = u[i]* eta[i]
	 */
	private Complex[] calcCrossCorrelation(Complex[] u, Complex[] eta) {
		Complex[] c = new Complex[np + 1];
		Arrays.parallelSetAll(c, i -> u[i].multiply(eta[i]));
		return c;
	}
	
	/**
	 * uとEtaの計算をする（積） cross correlation
	 * 
	 * @param u
	 * @param eta
	 * @return c[i] = u[i]* eta[i]
	 */
	private Complex[] calcCrossCorrelationSerial(Complex[] u, Complex[] eta) {
		Complex[] c = new Complex[np + 1];
		Arrays.setAll(c, i -> u[i].multiply(eta[i]));
		return c;
	}

	/**
	 * 
	 * イメージとしては partial = partial+coef*uce
	 * 
	 * partialにcoef倍したuceをたす
	 * 
	 * @param partial
	 * @param uce
	 * @param coef
	 */
	private void addPartial(Complex[] partial, Complex[] uce, double coef) {
		for (int i = 0; i < np + 1; i++)
			partial[i] = partial[i].add(uce[i].multiply(coef));
		return;
	}

}
