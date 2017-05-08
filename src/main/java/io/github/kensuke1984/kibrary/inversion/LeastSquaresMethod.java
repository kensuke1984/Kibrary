package io.github.kensuke1984.kibrary.inversion;

import org.apache.commons.math3.linear.*;

import java.util.Arrays;

/**
 * Least squares method.
 * <p>
 * m = (<b>A</b><sup>T</sup><b>A</b>+&lambda;<b>I</b>)<sup>-1</sup><b>A</b>
 * <sup>T</sup>d
 *
 * @author Kensuke Konishi
 * @version 0.0.2
 */
public class LeastSquaresMethod extends InverseProblem {

    private final double LAMBDA;

    /**
     * Solve A<sup>T</sup>A + &LAMBDA;I = A<sup>T</sup>d
     *
     * @param ata    Matrix A<sup>T</sup>A
     * @param atd    Vector A<sup>T</sup>d
     * @param lambda &lambda; for the equation
     */
    public LeastSquaresMethod(RealMatrix ata, RealVector atd, double lambda) {
        this.ata = ata;
        this.atd = atd;
        LAMBDA = lambda;
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
        Arrays.fill(diagonals, LAMBDA);
        RealMatrix j = ata.add(MatrixUtils.createRealDiagonalMatrix(diagonals));
        ans = new Array2DRowRealMatrix(MatrixUtils.inverse(j).operate(atd).toArray());
    }

    @Override
    public RealMatrix getBaseVectors() {
        throw new RuntimeException("No base vectors.");
    }

}
