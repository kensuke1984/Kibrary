package io.github.kensuke1984.kibrary.inversion;

import java.util.Arrays;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * 
 * Least squares method.
 * 
 * m = (<b>A</b><sup>T</sup><b>A</b>+&lambda;<b>I</b>)<sup>-1</sup><b>A</b>
 * <sup>T</sup>d
 * 
 * 
 * @version 0.0.1b
 * 
 * @author Kensuke Konishi
 *
 */
public class LeastSquaresMethod extends InverseProblem {

	private final double lambda;

	/**
	 * Solve A<sup>T</sup>A + &lambda;I = A<sup>T</sup>d
	 * 
	 * @param ata
	 *            Matrix A<sup>T</sup>A
	 * @param atd
	 *            Vector A<sup>T</sup>d
	 * @param lambda
	 *            &lambda; for the equation
	 */
	public LeastSquaresMethod(RealMatrix ata, RealVector atd, double lambda) {
		this.ata = ata;
		this.atd = atd;
		this.lambda = lambda;
	}

	@Override
	InverseMethodEnum getEnum() {
		return InverseMethodEnum.LEAST_SQUARES_METHOD;
	}

	@Override
	public RealMatrix computeCovariance(double sigmaD, int j) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void compute() {
		double[] diagonals = new double[ata.getColumnDimension()];
		Arrays.fill(diagonals, lambda);
		RealMatrix j = ata.add(MatrixUtils.createRealDiagonalMatrix(diagonals)); //TODO
		ans = new Array2DRowRealMatrix(ata.getRowDimension(), 1);
		ans.setRowVector(0, MatrixUtils.inverse(j).operate(atd));
	}

	@Override
	public RealMatrix getBaseVectors() {
		throw new RuntimeException("No base vectors.");
	}

}
