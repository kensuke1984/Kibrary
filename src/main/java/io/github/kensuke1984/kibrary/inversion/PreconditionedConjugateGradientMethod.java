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
public class PreconditionedConjugateGradientMethod extends InverseProblem {

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
	
	private RealVector m;
	
	private RealMatrix z;
	
	private RealMatrix r;

	/**
	 * AtAδm= AtD を解く
	 * 
	 * @param ata
	 *            AtA
	 * @param atd
	 *            AtD
	 */
	public PreconditionedConjugateGradientMethod(RealMatrix ata, RealVector atd, RealVector m) {
		this.ata = ata;
		this.atd = atd;
		this.m = m;
		int column = ata.getColumnDimension();
		p = MatrixUtils.createRealMatrix(column, column);
		ans = MatrixUtils.createRealMatrix(column, column);
		a = new ArrayRealVector(column);
		z = MatrixUtils.createRealMatrix(column, column);
		r = MatrixUtils.createRealMatrix(column, column);
	}
	
	private RealVector multiply(RealVector a, RealVector b) {
		RealVector c = new ArrayRealVector(a.getDimension());
		for (int i = 0; i < a.getDimension(); i++)
			c.setEntry(i, a.getEntry(i) * b.getEntry(i));
		return c;
	}

	public void compute() {
		int column = ata.getColumnDimension();
		p = MatrixUtils.createRealMatrix(column, column);
		ans = MatrixUtils.createRealMatrix(column, column);
		a = new ArrayRealVector(column);
		System.err.println("Solving by CG method.");
		r.setColumnVector(0, atd); // r_k = Atd -AtAm_k (A35)
		
		z.setColumnVector(0, multiply(m, r.getColumnVector(0)));
		p.setColumnVector(0, z.getColumnVector(0));
		
//		RealVector atap = ata.operate(p.getColumnVector(0));
		
//		a.setEntry(0, r.dotProduct(p.getColumnVector(0)) / atap.dotProduct(p.getColumnVector(0))); // a0

//		ans.setColumnVector(0, new ArrayRealVector());
		
		// ///////
		for (int i = 0; i < ata.getColumnDimension() - 1; i++) {
			RealVector atap = ata.operate(p.getColumnVector(i));
			
			a.setEntry(i, r.getColumnVector(i).dotProduct(z.getColumnVector(i)) / p.getColumnVector(i).dotProduct(atap));
			ans.setColumnVector(i + 1, ans.getColumnVector(i).add(p.getColumnVector(i).mapMultiply(a.getEntry(i))));
			r.setColumnVector(i + 1, r.getColumnVector(i).subtract(atap.mapMultiply(a.getEntry(i))));
			
			z.setColumnVector(i + 1, multiply(m, r.getColumnVector(i + 1)));
			double b = z.getColumnVector(i + 1).dotProduct(r.getColumnVector(i + 1)) / z.getColumnVector(i).dotProduct(r.getColumnVector(i));
			p.setColumnVector(i + 1, z.getColumnVector(i + 1).add(p.getColumnVector(i).mapMultiply(b)));
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
