package io.github.kensuke1984.kibrary.inversion.addons;

import org.apache.commons.math3.linear.*;

import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InverseProblem;

import java.util.Arrays;

/**
 * Least squares method.
 * <p>
 * m = (<b>A</b><sup>T</sup><b>A</b>+&lambda;<b>I</b>)<sup>-1</sup><b>A</b>
 * <sup>T</sup>d
 *
 * @author Kensuke Konishi
 * @version 0.1.0
 */
public class LeastSquaresMethod extends InverseProblem {

    /**
     * &lambda; <b>T</b> + <b>&eta;</b> = <b>0</b>
     */
    private final double LAMBDA;
    private final double MU;

    /**
     * &lambda; <b>T</b> + <b>&eta;</b> = <b>0</b>
     */
    private RealMatrix T;

    /**
     * &lambda; <b>T</b> + <b>&eta;</b> = <b>0</b>
     */
    private RealVector ETA;


    public static void main(String[] args) {
        double[][] x = new double[][]{{1, 0.000, 0}, {0.000, 1, 0}, {0.000, 0, 1}};
        double[] d = new double[]{1, 2, 3};
        RealMatrix X = new Array2DRowRealMatrix(x);
        RealMatrix XtX = X.transpose().multiply(X);
        RealVector y = new ArrayRealVector(d);
        RealVector Xty = X.transpose().operate(y);
        double lambda = 100.0;
        RealMatrix w = new Array2DRowRealMatrix(new double[][]{{1, 0, -1}, {0, 2, 1}});
        RealMatrix wtw = w.transpose().multiply(w);
        double[][] t = new double[][]{{1, 1, 0}, {0, 1, 0}, {0, 1, 1}};
        RealMatrix T = new Array2DRowRealMatrix(t);
        double[] eta = new double[]{20, 100, -2.3};
        RealVector ETA = new ArrayRealVector(eta);
        LeastSquaresMethod lsm = new LeastSquaresMethod(XtX, Xty, lambda, T, ETA);
        lsm.compute();
        System.out.println(lsm.ans);
    }

    /**
     * Find m which gives minimum |d-Am|<sup>2</sup>.
     *
     * @param ata Matrix A<sup>T</sup>A
     * @param atd Vector A<sup>T</sup>d
     */
    public LeastSquaresMethod(RealMatrix ata, RealVector atd) {
        this(ata, atd, 0, null, null);
    }

    /**
     * Find m which gives minimum |d-Am|<sup>2</sup> + &lambda;|m|<sup>2</sup>.
     *
     * @param ata    Matrix A<sup>T</sup>A
     * @param atd    Vector A<sup>T</sup>d
     * @param lambda &lambda; for the equation
     */
    public LeastSquaresMethod(RealMatrix ata, RealVector atd, double lambda) {
        this(ata, atd, lambda, MatrixUtils.createRealIdentityMatrix(ata.getColumnDimension()), null);
    }

    /**
     * @param ata    A<sup>T</sup>A
     * @param atd    A<sup>T</sup>d
     * @param lambda &lambda;
     * @param t      t
     * @param eta    &eta;
     */
    public LeastSquaresMethod(RealMatrix ata, RealVector atd, double lambda, RealMatrix t, RealVector eta) {
        this.ata = ata;
        this.atd = atd;
        LAMBDA = lambda;
        MU = 0;
        T = t;
        ETA = eta;
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
        RealMatrix j = ata;
        RealVector k = atd;
        if (0 < LAMBDA) {
            RealMatrix tt = T.transpose();
            j = j.add(tt.multiply(T).scalarMultiply(LAMBDA));
            if (ETA != null) k = k.subtract(tt.operate(ETA).mapMultiply(LAMBDA));
        }
        ans = new Array2DRowRealMatrix(MatrixUtils.inverse(j).operate(k).toArray());
    }

    @Override
    public RealMatrix getBaseVectors() {
        throw new RuntimeException("No base vectors.");
    }

}
