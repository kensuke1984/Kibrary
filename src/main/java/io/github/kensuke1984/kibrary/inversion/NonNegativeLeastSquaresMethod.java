package io.github.kensuke1984.kibrary.inversion;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class NonNegativeLeastSquaresMethod extends InverseProblem {
	
	private final double lambda;
	
	public NonNegativeLeastSquaresMethod(RealMatrix ata, RealVector atd, double lambda) {
		this.ata = ata;
		this.atd = atd;
		this.lambda = lambda;
	}
	
	@Override
	public InverseMethodEnum getEnum() {
		return InverseMethodEnum.NON_NEGATIVE_LEAST_SQUARES_METHOD;
	}
	
	@Override
	public void compute() {
		ArrayRealVector x = new ArrayRealVector(atd.getDimension());
		x.set(1.);
		
	}
	
	@Override
	public RealMatrix getBaseVectors() {
		throw new RuntimeException("No base vectors.");
	}
	
	@Override
	public RealMatrix computeCovariance(double sigmaD, int j) {
		// TODO
		return null;
	}
}