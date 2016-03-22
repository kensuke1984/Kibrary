package io.github.kensuke1984.kibrary.math;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * 
 * 行列計算 parallelized Matrix
 * 
 * @version 0.1.0
 * 
 * @author Kensuke Konishi
 * 
 */
public class Matrix extends Array2DRowRealMatrix {

	private static final long serialVersionUID = 1L;

	public Matrix() {
		super();
	}

	public Matrix(double[] arg0) {
		super(arg0);
	}

	public Matrix(double[][] arg0, boolean arg1)
			throws IllegalArgumentException, NullPointerException {
		super(arg0, arg1);
	}

	public Matrix(double[][] d) throws IllegalArgumentException,
			NullPointerException {
		super(d);
	}

	public Matrix(int rowDimension, int columnDimension)
			throws IllegalArgumentException {
		super(rowDimension, columnDimension);
	}


	@Override
	public Matrix multiply(final RealMatrix arg0)
			throws IllegalArgumentException {
		MatrixUtils.checkMultiplicationCompatible(this, arg0);
		return MatrixComputation.computeAB(this, arg0);
	}

	@Override
	public Matrix preMultiply(final RealMatrix m)
			throws DimensionMismatchException {
		return MatrixComputation.computeAB(m, this);
	}

	public Matrix computeAtA() {
		return MatrixComputation.computeAtA(this);
	}
	
	
	
	

	@Override
	public RealVector preMultiply(RealVector v)
			throws DimensionMismatchException {
		if(v.getDimension() != getRowDimension())
			throw new DimensionMismatchException(v.getDimension(), getRowDimension());
		return MatrixComputation.premultiply(v, this);
	}

	@Override
	public RealVector operate(RealVector arg0) throws IllegalArgumentException {
		if (arg0.getDimension() != getColumnDimension())
			throw new DimensionMismatchException(arg0.getDimension(),
					getColumnDimension());
		return MatrixComputation.operate(this, arg0);
	}
}
