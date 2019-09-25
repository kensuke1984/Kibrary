package io.github.kensuke1984.kibrary.inversion;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.math.Matrix;

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
public class NonlinearConjugateGradientMethod extends InverseProblem {

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
	
	private RealMatrix am;
	
	private RealVector u;
	
	private RealVector s0;

	/**
	 * AtAδm= AtD を解く
	 * 
	 * @param ata
	 *            AtA
	 * @param atd
	 *            AtD
	 */
	public NonlinearConjugateGradientMethod(RealMatrix ata, RealMatrix am, RealVector s0, RealVector u) {
		this.ata = ata;
		int column = ata.getColumnDimension();
		p = MatrixUtils.createRealMatrix(column, column);
		ans = MatrixUtils.createRealMatrix(column, column);
		a = new ArrayRealVector(column);
		this.am = am;
		this.s0 = s0;
		this.u = u;
	}


	public void compute() {
		int column = ata.getColumnDimension();
		ans = MatrixUtils.createRealMatrix(column, column);
		System.err.println("Solving by nonlinear CG method.");
		
		RealMatrix amt = am.transpose();
		
		RealVector x0 = new ArrayRealVector(column);
		System.out.println(costFunction(x0));
		RealVector dx = costFunctionGradient(x0, amt).mapMultiply(-1).mapMultiply(1.e-4);
		RealVector x1 = lineSearch(dx, x0);
		
		ans.setColumnVector(0, x1);
		
		int nmax = ata.getColumnDimension();
		
		RealVector s0 = dx;
		RealVector s1;
		RealVector dx0;
		
		// ///////
		for (int i = 1; i < nmax; i++) {
			x0 = x1;
			dx0 = dx;
			dx = costFunctionGradient(x0, amt).mapMultiply(-1).mapMultiply(1.e-4);
			
//			double beta = dx.dotProduct(dx.subtract(dx0)) / dx0.dotProduct(dx0);
//			if (beta < 0) beta = 0;
//			s1 = dx.add(s0.mapMultiply(beta));
//			x1 = lineSearch(s1, x0);
			
			x1 = lineSearch(dx, x0);
			
			ans.setColumnVector(i, x1);
//			s0 = s1;
		}
	}
	
	private RealVector costFunctionGradient(RealVector m, RealMatrix amt) {
		RealVector ans = null;
		
		RealVector s = s0.add(am.operate(m));
		RealVector p1 = amt.operate(u);
		RealVector p2 = ata.operate(m).add(amt.operate(s0));
		
		ans = p1.add(p2.mapMultiply(-u.dotProduct(s) / s.dotProduct(s))).mapDivide(u.getNorm() * s.getNorm())
				.mapMultiply(-1);
		
		return ans;
	}
	
	private double costFunction(RealVector m) {
		RealVector s = s0.add(am.operate(m));
		return 1. - u.dotProduct(s) / (u.getNorm() * s.getNorm());
	}
	
	private RealVector lineSearch(RealVector dx, RealVector x) {
		double alpha0 = 1e-2;
		double alpha1 = 1e2;
		double dalpha = 1e-2;
		int n = (int) ((alpha1 - alpha0) / dalpha);
		double cost = Double.MAX_VALUE;
		RealVector ans = null;
		double ansalpha = 0;
		for (int i = 0; i < n; i++) {
			double alpha = i * dalpha + alpha0;
			double costi = costFunction(x.add(dx.mapMultiply(alpha)));
			if (costi < cost) {
				cost = costi;
				ans = x.add(dx.mapMultiply(alpha));
				ansalpha = alpha;
			}
//			System.out.println(costi);
		}
		System.out.println("ansalpha = " + ansalpha);
		return ans;
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
