package io.github.kensuke1984.kibrary.inversion;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

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
public class ConstrainedConjugateGradientMethod extends InverseProblem {

	public RealMatrix getP() {
		return p;
	}

	/**
	 * m = ai*pi
	 */
	private RealVector a;

	/**
	 * P = (p1, p2,....)
	 */
	private RealMatrix p;
	
	private RealMatrix h;

	/**
	 * AtAδm= AtD を解く
	 * 
	 * @param ata
	 *            AtA
	 * @param atd
	 *            AtD
	 */
	public ConstrainedConjugateGradientMethod(RealMatrix ata, RealVector atd, RealMatrix h) {
		this.ata = ata;
		this.atd = atd;
		int column = ata.getColumnDimension();
		p = MatrixUtils.createRealMatrix(column, column);
		ans = MatrixUtils.createRealMatrix(column, column);
		a = new ArrayRealVector(column);
		this.h = h;
	}


	public void compute() {
		int column = ata.getColumnDimension();
		p = MatrixUtils.createRealMatrix(column, column);
		ans = MatrixUtils.createRealMatrix(column, column);
		a = new ArrayRealVector(column);
		System.err.println("Solving by Constrained CG method.");
		
		RealVector r0 = atd.mapMultiply(-1); // r_k = Atd -AtAm_k (A35)
		
		RealVector z0 = h.operate(r0);
		
		p.setColumnVector(0, z0.mapMultiply(-1));
		
		RealVector atap = ata.operate(p.getColumnVector(0));
		
		a.setEntry(0, -r0.dotProduct(p.getColumnVector(0)) / atap.dotProduct(p.getColumnVector(0))); // a0

		ans.setColumnVector(0, p.getColumnVector(0).mapMultiply(a.getEntry(0)));
		
		RealVector z;
		RealVector r;
		
		// ///////
		for (int i = 1; i < ata.getColumnDimension(); i++) {
			r = r0.add(atap.mapMultiply(a.getEntry(i - 1)));
			
			z = h.operate(r);

//			double atapr = atap.dotProduct(r); // p AtA r
//			double patap = p.getColumnVector(i - 1).dotProduct(atap); // ptatap
			
			double b = r.dotProduct(z) / r0.dotProduct(z0);
			
			p.setColumnVector(i, z.mapMultiply(-1).add(p.getColumnVector(i - 1).mapMultiply(b)));

			atap = ata.operate(p.getColumnVector(i));
			double paap = p.getColumnVector(i).dotProduct(atap);
			double rp = r.dotProduct(p.getColumnVector(i));

			a.setEntry(i, -rp / paap);

			ans.setColumnVector(i, p.getColumnVector(i).mapMultiply(a.getEntry(i)).add(ans.getColumnVector(i - 1)));
			
			r0 = r;
			z0 = z;
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
	
	public static RealMatrix projectorRectangle(int nUnknown, int nCombine) {
		RealMatrix h = MatrixUtils.createRealMatrix(nUnknown, nUnknown);
		
		int n = nUnknown / nCombine;
		
		for (int i = 0; i < n - 1; i++) {
			int itmp = i * nCombine;
			for (int j = 0; j < nCombine; j++) {
				for (int k = 0; k < nCombine; k++) {
					h.setEntry(itmp + j, itmp + k, 1. / nCombine);
				}
			}
		}
		
		return h;
	}
}
