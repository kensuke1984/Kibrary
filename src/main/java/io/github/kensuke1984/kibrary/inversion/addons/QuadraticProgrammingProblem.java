package io.github.kensuke1984.kibrary.inversion.addons;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class QuadraticProgrammingProblem {
	
	RealMatrix a;
	
	RealVector b;
	
	RealMatrix aPositive;
	
	RealMatrix aNegative;
	
	final int MAX_ITERATION = 100;
	
	final double tolerance = 1e-8;
	
	public QuadraticProgrammingProblem(RealMatrix a, RealVector b) {
		if (a.getColumnDimension() != b.getDimension())
			throw new IllegalArgumentException("Column dimension of a and length of b must be equal");
		if (!isSymmetric(a))
			throw new IllegalArgumentException("a must be symmetric (and positive definite)");
		
		this.a = a;
		this.b = b;
		
		this.aPositive = APositive();
		this.aNegative = ANegative();
	}
	
	public RealVector compute() {
		System.out.println("Solving QQP by Sha-Saul-Lee algorithm...");
		RealVector ans = new ArrayRealVector(a.getColumnDimension());
		
		// initialization
		ans.set(1.);
		boolean converged = false;
		double Fbefore = F(ans);
		
		// iterations
		System.out.println(Fbefore);
		int count = 0;
		do {
			// partials
			RealVector partialA = partialA(ans);
			RealVector partialC = partialC(ans);
			
			double maxTmp = Double.MIN_VALUE;
			for (int i = 0; i < ans.getDimension(); i++) {
				double bi = b.getEntry(i);
				double ai = partialA.getEntry(i);
				double det = Math.sqrt(bi * bi + 4 * ai * partialC.getEntry(i));
				double tmp = (-bi + det) / (2 * ai);
				ans.setEntry(i, ans.getEntry(i) * tmp);
				if (tmp > maxTmp)
					tmp = maxTmp;
			}
			
			double F = F(ans);
			if (Fbefore - F < 0)
				throw new RuntimeException("Warning: F increasing. There is a problem in the QQP algorithm");
			if ( (Fbefore - F) / F <= tolerance )
				converged = true;
			
			count++;
			System.out.println(F);
		} while (!converged && count <= MAX_ITERATION);
		
		System.out.println("Done!");
		return ans;
	}
	
	private RealMatrix APositive() {
		RealMatrix aPositive = new Array2DRowRealMatrix(a.getRowDimension(), a.getColumnDimension());
		for (int i = 0; i < a.getRowDimension(); i++) {
			for (int j = 0; j < a.getColumnDimension(); j++) {
				double tmp = a.getEntry(i, j);
				if (tmp > 0)
					aPositive.setEntry(i, j, tmp);
				else
					aPositive.setEntry(i, j, 0.);
			}
		}
		
		return aPositive;
	}
	
	private RealMatrix ANegative() {
		RealMatrix aNegative = new Array2DRowRealMatrix(a.getRowDimension(), a.getColumnDimension());
		for (int i = 0; i < a.getRowDimension(); i++) {
			for (int j = 0; j < a.getColumnDimension(); j++) {
				double tmp = a.getEntry(i, j);
				if (tmp < 0)
					aNegative.setEntry(i, j, -tmp);
				else
					aNegative.setEntry(i, j, 0.);
			}
		}
		
		return aNegative;
	}
	
	private double F(RealVector v) {
		return .5 * a.operate(v).dotProduct(v) + b.dotProduct(v);
	}
	
	private RealVector partialA(RealVector v) {
		return aPositive.operate(v);
	}
	
	private RealVector partialC(RealVector v) {
		return aNegative.operate(v);
	}
	
	private boolean isSymmetric(RealMatrix a) {
		boolean isSym = true;
		for (int i = 0; i < a.getColumnDimension(); i++) {
			for (int j = 0; j < i; j++) {
				if (a.getEntry(i, j) != a.getEntry(j, i)) {
					isSym = false;
					break;
				}
			}
		}
		
		return isSym;
	}
}
