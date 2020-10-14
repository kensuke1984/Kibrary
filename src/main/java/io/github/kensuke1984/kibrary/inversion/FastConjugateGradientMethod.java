package io.github.kensuke1984.kibrary.inversion;

import java.util.Arrays;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.math.MatrixComputation;

/**
 * Conjugate gradient method
 * 
 * 
 * @version 0.0.3.2
 * @author Kensuke Konishi
 * @see <a
 *      href=https://ja.wikipedia.org/wiki/%E5%85%B1%E5%BD%B9%E5%8B%BE%E9%85%8D%E6%B3%95>Japanese wiki</a> <a
 *      href=https://en.wikipedia.org/wiki/Conjugate_gradient_method>English
 *      wiki</a>
 */
public class FastConjugateGradientMethod extends InverseProblem {

	public RealMatrix getP() {
		return p;
	}

	/**
	 * m = ai*pi
	 */
	private RealVector alpha;

	/**
	 * P = (p1, p2,....)
	 */
	private RealMatrix p;
	
	private RealMatrix a;
	
	private boolean damped;
	
	private RealVector conditioner;

	/**
	 * AtAδm= AtD を解く
	 * 
	 * @param ata
	 *            AtA
	 * @param atd
	 *            AtD
	 */
	public FastConjugateGradientMethod(RealMatrix a, RealVector atd, boolean damped) {
		this.ata = null;
		this.a = a;
		this.atd = atd;
		this.damped = damped;
		int column = atd.getDimension();
		p = MatrixUtils.createRealMatrix(column, column);
		ans = MatrixUtils.createRealMatrix(column, column);
		alpha = new ArrayRealVector(column);
	}
	
	public FastConjugateGradientMethod(RealMatrix a, RealVector atd, boolean damped, RealVector conditioner) {
		this.ata = null;
		this.a = a;
		this.atd = atd;
		this.damped = damped;
		int column = atd.getDimension();
		p = MatrixUtils.createRealMatrix(column, column);
		ans = MatrixUtils.createRealMatrix(column, column);
		alpha = new ArrayRealVector(column);
		this.conditioner = conditioner;
	}


	public void compute() {
		int column = atd.getDimension();
		p = MatrixUtils.createRealMatrix(column, column);
		ans = MatrixUtils.createRealMatrix(column, column);
		alpha = new ArrayRealVector(column);
		System.err.println("Solving by CG method.");
		p.setColumnVector(0, atd.mapMultiply(-1));
		RealVector r = atd; // r_k = Atd -AtAm_k (A35)
		
		long t1, t2;
		
//		t1 = System.nanoTime();
//		a.transpose().operate(a.operate(p.getColumnVector(0)));
//		t2 = System.nanoTime();
//		System.out.println((t2-t1)*1e-9 + "s");
//		
//		t1 = System.nanoTime();
//		a.preMultiply(a.operate(p.getColumnVector(0)));
//		t2 = System.nanoTime();
//		System.out.println((t2-t1)*1e-9 + "s");
		
		RealVector atap = null;
		if (damped) {
//			atap = a.transpose().operate(a.operate(p.getColumnVector(0))).add(p.getColumnVector(0)); //ata.operate(p.getColumnVector(0));
			double[] tmp = new double[atd.getDimension()];
			Arrays.setAll(tmp, i -> p.getColumnVector(0).getEntry(i) * conditioner.getEntry(i) * conditioner.getEntry(i));
			atap = a.preMultiply(a.operate(p.getColumnVector(0))).add(new ArrayRealVector(tmp));
		}
		else {
//			atap = a.transpose().operate(a.operate(p.getColumnVector(0))); //ata.operate(p.getColumnVector(0));
			atap = a.preMultiply(a.operate(p.getColumnVector(0)));
		}
		
		alpha.setEntry(0, r.dotProduct(p.getColumnVector(0)) / atap.dotProduct(p.getColumnVector(0))); // a0

		ans.setColumnVector(0, p.getColumnVector(0).mapMultiply(alpha.getEntry(0)));
		
		// ///////
		int tmpN = 20;
		for (int i = 1; i < tmpN; i++) {
			t1 = System.nanoTime();
			r = r.subtract(atap.mapMultiply(alpha.getEntry(i - 1)));

			double atapr = atap.dotProduct(r); // p AtA r
			double patap = p.getColumnVector(i - 1).dotProduct(atap); // ptatap
			double b = atapr / patap; // (A36)
			p.setColumnVector(i, r.subtract(p.getColumnVector(i - 1).mapMultiply(b)));
			
			if (damped) {
//				atap = MatrixComputation.operate(a.transpose(), MatrixComputation.operate(a, p.getColumnVector(i))).add(p.getColumnVector(i)); 
//				a.transpose().operate(a.operate(p.getColumnVector(i))).add(p.getColumnVector(i)); //ata.operate(p.getColumnVector(i));
				double[] tmp = new double[atd.getDimension()];
				final int finali = i;
				Arrays.setAll(tmp, j -> p.getColumnVector(finali).getEntry(j) * conditioner.getEntry(j) * conditioner.getEntry(j));
				atap = a.preMultiply(a.operate(p.getColumnVector(i))).add(new ArrayRealVector(tmp));
			}
			else {
//				atap = MatrixComputation.operate(a.transpose(), MatrixComputation.operate(a, p.getColumnVector(i)));
				atap = a.preMultiply(a.operate(p.getColumnVector(i)));
			}
			
			double paap = p.getColumnVector(i).dotProduct(atap);
			double rp = r.dotProduct(p.getColumnVector(i));

			alpha.setEntry(i, rp / paap);

			ans.setColumnVector(i, p.getColumnVector(i).mapMultiply(alpha.getEntry(i)).add(ans.getColumnVector(i - 1)));
			t2 = System.nanoTime();
//			System.out.println((t2-t1)*1e-9 + "s");
		}
	}

	@Override
	public RealMatrix computeCovariance(double sigmaD, int j) {
		RealMatrix covariance = MatrixUtils.createRealMatrix(getParN(), getParN());
		double sigmaD2 = sigmaD * sigmaD;
		for (int i = 0; i < j ; i++) {
			double paap = p.getColumnVector(i).dotProduct(ata.operate(p.getColumnVector(i)));
			RealMatrix p = this.p.getColumnMatrix(i);
			double sigmaD2paap = sigmaD2 / paap;
			covariance = covariance.add(p.multiply(p.transpose()).scalarMultiply(sigmaD2paap));
		}
		return covariance;
	}

	/**
	 * L<sub>i, j</sub> = p<sup>T</sup><sub>i</sub> A<sup>T</sup>A p<sub>i</sub>
	 * i=j 0 i≠j
	 * 
	 * @return L<sub>i, j</sub>
	 */
	public RealMatrix getL() {
		RealMatrix l = MatrixUtils.createRealMatrix(getParN(), getParN());
		for (int i = 0; i < getParN(); i++) {
			RealVector p = this.p.getColumnVector(i);
			double val = p.dotProduct(ata.operate(p));
			l.setEntry(i, i, val);
		}
		return l;
	}

	@Deprecated
	public RealMatrix computeCovariance() {
		// RealMatrix ata = this.ata;
		RealMatrix covariance = MatrixUtils.createRealMatrix(getParN(), getParN());

		// new LUDecomposition(getL()).getSolver().getInverse();
		// covariance = p.multiply(getL().inverse()).multiply(p.transpose());
		// //TODO
		covariance = p.multiply(new LUDecomposition(getL()).getSolver().getInverse()).multiply(p.transpose());

		return covariance;
	}
	
	@Override
	public RealMatrix getBaseVectors() {
		return p;
	}

	@Override
	InverseMethodEnum getEnum() {
		return InverseMethodEnum.CONJUGATE_GRADIENT;
	}
}
