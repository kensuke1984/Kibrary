package io.github.kensuke1984.kibrary.math;

import io.github.kensuke1984.kibrary.util.Utilities;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.stream.IntStream;

/**
 * Computations of matrix and vector
 *
 * @author Kensuke Konishi
 * @version 0.0.2.1
 */
public class MatrixComputation {

    public static Matrix computeAtA(RealMatrix a) {
        long start = System.nanoTime();
        System.err.print("Computing matrix multiplication AtA");
        int n = a.getColumnDimension();
        Matrix ata = new Matrix(n, n);

        IntStream.range(0, n).parallel().forEach(i -> {
            IntStream.range(i, n).parallel().forEach(j -> {
                double value = computeMultiplication(i, j, a, null);
                ata.setEntry(i, j, value);
                ata.setEntry(j, i, value);
            });
        });
        System.err.println(", it took " + Utilities.toTimeString(System.nanoTime() - start));
        return ata;
    }

    public static RealVector operate(RealMatrix a, RealVector m) {
        int n = a.getRowDimension();
        int length = m.getDimension();
        if (a.getColumnDimension() != length) throw new RuntimeException("dimension invalid");
        RealVector vector = new ArrayRealVector(n);
        IntStream.range(0, n).parallel().forEach(i -> {
            double value = 0;
            for (int k = 0; k < length; k++)
                value += a.getEntry(i, k) * m.getEntry(k);
            vector.setEntry(i, value);
        });
        return vector;
    }

    public static RealVector premultiply(RealVector v, RealMatrix a) {
        int n = a.getColumnDimension();
        int length = v.getDimension();
        if (length != a.getRowDimension()) throw new RuntimeException("dimension invalid");
        RealVector vector = new ArrayRealVector(n);
        IntStream.range(0, n).parallel().forEach(i -> {
            double value = 0;
            for (int k = 0; k < length; k++)
                value += a.getEntry(k, i) * v.getEntry(k);
            vector.setEntry(i, value);
        });
        return vector;
    }

    public static Matrix computeAB(RealMatrix former, RealMatrix latter) throws DimensionMismatchException {
        long start = System.nanoTime();
        MatrixUtils.checkMultiplicationCompatible(former, latter);
        System.err.print("computing matrix multiplication");
        int m = former.getRowDimension();
        int n = latter.getColumnDimension();
        Matrix ab = new Matrix(m, n);
        IntStream.range(0, m).parallel().forEach(i -> IntStream.range(0, n).parallel()
                .forEach(j -> ab.setEntry(i, j, computeMultiplication(i, j, former, latter))));
        System.err.println(", it took " + Utilities.toTimeString(System.nanoTime() - start));
        return ab;
    }

    /**
     * Computation for an element at (i,j) of A(former)B(latter). If the latter
     * matrix is null, the return will be A<sup>T</sup> * B
     *
     * @param i      row index
     * @param j      column index
     * @param former former matrix A
     * @param latter latter matrix B
     * @return the value (i,j) of former*latter (sum of former(i,k)*latter(k,j)
     * (for all k))
     */
    private static double computeMultiplication(int i, int j, RealMatrix former, RealMatrix latter) {
        int n = former.getColumnDimension();
        if (latter != null && latter.getRowDimension() != n) throw new RuntimeException("can not multiply");
        double value = 0;
        if (latter == null) for (int k = 0, l = former.getRowDimension(); k < l; k++)
            value += former.getEntry(k, i) * former.getEntry(k, j);
        else for (int k = 0; k < n; k++)
            value += former.getEntry(i, k) * latter.getEntry(k, j);
        return value;

    }

}
