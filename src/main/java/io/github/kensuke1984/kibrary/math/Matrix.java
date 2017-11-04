package io.github.kensuke1984.kibrary.math;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.*;

/**
 * 行列計算 parallelized Matrix
 *
 * @author Kensuke Konishi
 * @version 0.1.0.1
 */
public class Matrix extends Array2DRowRealMatrix {

    private static final long serialVersionUID = 1L;

    public Matrix() {
        super();
    }

    public Matrix(double[] arg0) {
        super(arg0);
    }

    public Matrix(double[][] arg0, boolean arg1) throws IllegalArgumentException, NullPointerException {
        super(arg0, arg1);
    }

    public Matrix(double[][] d) throws IllegalArgumentException, NullPointerException {
        super(d);
    }

    public Matrix(int rowDimension, int columnDimension) throws IllegalArgumentException {
        super(rowDimension, columnDimension);
    }


    @Override
    public Matrix multiply(RealMatrix arg0) throws IllegalArgumentException {
        MatrixUtils.checkMultiplicationCompatible(this, arg0);
        return MatrixComputation.computeAB(this, arg0);
    }

    @Override
    public Matrix preMultiply(RealMatrix m) throws DimensionMismatchException {
        return MatrixComputation.computeAB(m, this);
    }

    public Matrix computeAtA() {
        return MatrixComputation.computeAtA(this);
    }

    @Override
    public RealVector preMultiply(RealVector v) throws DimensionMismatchException {
        if (v.getDimension() != getRowDimension())
            throw new DimensionMismatchException(v.getDimension(), getRowDimension());
        return MatrixComputation.premultiply(v, this);
    }

    @Override
    public RealVector operate(RealVector arg0) throws IllegalArgumentException {
        if (arg0.getDimension() != getColumnDimension())
            throw new DimensionMismatchException(arg0.getDimension(), getColumnDimension());
        return MatrixComputation.operate(this, arg0);
    }

    @Override
    public RealMatrix transpose() {
        RealMatrix out = new Matrix(getColumnDimension(), getRowDimension());
        walkInOptimizedOrder(new DefaultRealMatrixPreservingVisitor() {
            /** {@inheritDoc} */
            @Override
            public void visit(int row, int column, double value) {
                out.setEntry(column, row, value);
            }

        });
        return out;
    }
}
