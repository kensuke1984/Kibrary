package io.github.kensuke1984.kibrary.util;

import io.github.kensuke1984.kibrary.timewindow.Timewindow;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.linear.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility for a function y = f(x)
 * <p>
 * <p>
 * This class is <b>IMMUTABLE</b>
 * </p>
 * <p>
 * TODO sorted
 *
 * @author Kensuke Konishi
 * @version 0.1.2.2.1
 */
public class Trace {

    private final double[] X;
    private final double[] Y;
    private final RealVector X_VECTOR;
    private final RealVector Y_VECTOR;

    /**
     * Deep copy
     *
     * @param x array for X
     * @param y array for Y
     */
    public Trace(double[] x, double[] y) {
        if (x.length != y.length) throw new IllegalArgumentException("Input arrays have different lengths");
        X = x.clone();
        Y = y.clone();
        X_VECTOR = new ArrayRealVector(x, false);
        Y_VECTOR = new ArrayRealVector(y, false);
    }

    /**
     * All lines are trimmed. Lines starting with 'c' '!' '#' are ignored.
     *
     * @param path    of the file you want to read
     * @param xColumn indicates which column is x (for the first column &rarr; 0)
     * @param yColumn indicates which column is y
     * @return Trace made by the file of the path
     * @throws IOException if any
     */
    public static Trace createTrace(Path path, int xColumn, int yColumn) throws IOException {
        List<String> lines = Files.readAllLines(path).stream().map(String::trim).filter(l -> {
            char first = l.charAt(0);
            return first != 'c' && first != '#' && first != '!';
        }).collect(Collectors.toList());
        int n = lines.size();
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            String[] parts = lines.get(i).split("\\s+");
            x[i] = Double.parseDouble(parts[xColumn]);
            y[i] = Double.parseDouble(parts[yColumn]);
        }
        return new Trace(x, y);
    }

    /**
     * @param path of a file
     * @return Trace of x in the first column and y in the second column
     * @throws IOException if any
     */
    public static Trace createTrace(Path path) throws IOException {
        return createTrace(path, 0, 1);
    }

    /**
     * 最も相関の高い位置を探す 探し方は、短い方をずらしていく 同じ長さだと探さない。
     *
     * @param base    array
     * @param compare array
     * @return compareを何ポイントずらすか 0だと先頭から
     */
    public static int findBestShift(double[] base, double[] compare) {
        double[] shorter;
        double[] longer;
        if (base.length == compare.length) return 0;
        if (base.length < compare.length) {
            shorter = base;
            longer = compare;
        } else {
            shorter = compare;
            longer = base;
        }
        int gap = longer.length - shorter.length;
        int bestShift = 0;
        double bestCorrelation = 0;
        for (int shift = 0; shift < gap + 1; shift++) {
            double[] partY = new double[shorter.length];
            System.arraycopy(longer, shift, partY, 0, shorter.length);
            RealVector partYVec = new ArrayRealVector(partY);
            RealVector shorterVec = new ArrayRealVector(shorter);
            double correlation = partYVec.dotProduct(shorterVec) / partYVec.getNorm() / shorterVec.getNorm();
            if (bestCorrelation < correlation) {
                bestCorrelation = correlation;
                bestShift = shift;
            }
            // System.out.println(correlation);
        }

        return compare.length < base.length ? bestShift : -bestShift;
    }

    /**
     * @param i index for x [0, length -1]
     * @return x[i]
     */
    public double getXAt(int i) {
        return X[i];
    }

    /**
     * @param i index for y [0, length -1]
     * @return y[i]
     */
    public double getYAt(int i) {
        return Y[i];
    }

    /**
     * @return the number of elements
     */
    public int getLength() {
        return X.length;
    }

    /**
     * compute n th polynomial functions for the trace
     *
     * @param n degree of polynomial
     * @return n th {@link PolynomialFunction} fitted to this
     */
    public PolynomialFunction toPolynomial(int n) {
        if (X.length <= n) throw new IllegalArgumentException("n is too big");
        if (n < 0) throw new IllegalArgumentException("n must be positive..(at least)");

        // (1,X,X**2,....)
        RealMatrix a = new Array2DRowRealMatrix(X.length, n + 1);
        for (int j = 0; j < X.length; j++)
            for (int i = 0; i <= n; i++)
                a.setEntry(j, i, Math.pow(X[j], i));
        RealMatrix at = a.transpose();
        a = at.multiply(a);
        RealVector b = at.operate(Y_VECTOR);
        RealVector coef = new LUDecomposition(a).getSolver().solve(b);
        return new PolynomialFunction(coef.toArray());
    }

    /**
     * f(x) &rarr; f(x-shift) Shifts "shift" in the direction of x axis. If you
     * want to change like below: <br>
     * x:(3, 4, 5) -> (0, 1, 2) <br>
     * then the value 'shift' should be -3
     *
     * @param shift value of shift
     * @return f(x-shift), the values in y are deep copied.
     */
    public Trace shiftX(double shift) {
        return new Trace(Arrays.stream(X).map(d -> d + shift).toArray(), Y);
    }

    /**
     * x=cの点でのyの値をfをn次関数として補完する 補完の際には直近のn+1点で補完
     * f(x) = &Sigma; a<sub>i</sub> x<sup>i</sup>
     *
     * @param n degree of function for interpolation
     * @param c point for the value
     * @return y = f(c) = &Sigma; a<sub>i</sub> c<sup>i</sup>
     */
    public double toValue(int n, double c) {
        if (X.length < n + 1) throw new IllegalArgumentException("n is too big");
        if (n < 0) throw new IllegalArgumentException("n is invalid");

        int[] j = nearPoints(n + 1, c);
        for (int i : j) {
            System.out.println(i + " " + X[i]);
        }
        if (n == 0) return Y[j[0]];

        double[] xi = Arrays.stream(j).parallel().mapToDouble(i -> X[i]).toArray();

        // (1, c, c**2...)
        RealVector cx = new ArrayRealVector(n + 1);

        RealMatrix matrix = new Array2DRowRealMatrix(n + 1, n + 1);
//      {a_i}
        RealVector bb = new ArrayRealVector(n + 1);
        for (int i = 0; i < n + 1; i++) {
            cx.setEntry(i, Math.pow(c, i));
            for (int k = 0; k < n + 1; k++)
                matrix.setEntry(i, k, Math.pow(xi[i], k));
            bb.setEntry(i, Y[j[i]]);
        }
        return cx.dotProduct(new LUDecomposition(matrix).getSolver().solve(bb));
    }

    /**
     * @param target value of x to look for the nearest x value to
     * @return the closest x to the target
     */
    public double getNearestX(double target) {
        return X[getNearestXIndex(target)];
    }

    /**
     * peak is defined as 0 &lt; (y(x[i])-y(x[i-1]))*(y(x[i])-y(x[i+1]))
     *
     * @return index of a peak (ordered)
     */
    public int[] indexOfPeaks() {
        return IntStream.range(1, X.length - 1).filter(i -> 0 < (Y[i + 1] - Y[i]) * (Y[i - 1] - Y[i])).toArray();
    }

    /**
     * 0 &lt; (y(x[i])-y(x[i-1])) * (y(x[i]) - y(x[i+1])) and y[i] &lt; y[i-1]
     *
     * @return index of downward convex
     */
    public int[] indexOfDownwardConvex() {
        return IntStream.range(1, X.length - 1)
                .filter(i -> Y[i] < Y[i - 1] && 0 < (Y[i + 1] - Y[i]) * (Y[i - 1] - Y[i])).toArray();
    }

    /**
     * 0 &lt; (y(x[i])-y(x[i-1]))*(y(x[i])-y(x[i+1])) and y[i-1] &lt; t[i]
     *
     * @return index of downward convex
     */
    public int[] indexOfUpwardConvex() {
        return IntStream.range(1, X.length - 1)
                .filter(i -> Y[i - 1] < Y[i] && 0 < (Y[i + 1] - Y[i]) * (Y[i - 1] - Y[i])).toArray();
    }

    /**
     * @param target value of x to look for the nearest x value to
     * @return the index of the closest x to the target
     */
    public int getNearestXIndex(double target) {
        return nearPoints(1, target)[0];
    }

    /**
     * Assume the interval of x is same as that of this.
     *
     * @param trace which length must be shorter than this.
     * @return the shift value x0 in x direction for best correlation.
     */
    public double findBestShift(Trace trace) {
        int gapLength = X.length - trace.getLength();
        if (gapLength <= 0) throw new IllegalArgumentException("Input trace must be shorter.");
        double corMax = -1;
        double compY2 = trace.Y_VECTOR.getNorm();
        double shift = 0;
        for (int i = 0; i <= gapLength; i++) {
            double cor = 0;
            double y2 = 0;
            for (int j = 0; j < trace.getLength(); j++) {
                cor += Y[i + j] * trace.Y[j];
                y2 += Y[i + j] * Y[i + j];
            }
            cor /= y2 * compY2;
            if (corMax < cor) {
                shift = X[i] - trace.X[0];
                corMax = cor;
            }
        }
        return shift;
    }

    /**
     * @param n must be a natural number
     * @param x x
     * @return index of n points closest to the value x
     */
    private int[] nearPoints(int n, double x) {
        if (n <= 0 || X.length < n) throw new IllegalArgumentException("n is invalid");
        int[] xi = new int[n];
        double[] res = new double[n];
        Arrays.fill(res, -1);
        for (int i = 0; i < X.length; i++) {
            double residual = Math.abs(X[i] - x);
            for (int j = 0; j < n; j++)
                if (res[j] < 0 || residual <= res[j]) {
                    for (int k = n - 1; j < k; k--) {
                        res[k] = res[k - 1];
                        xi[k] = xi[k - 1];
                    }
                    res[j] = residual;
                    xi[j] = i;
                    break;
                }
        }
        return xi;
    }

    /**
     * thisの start &le; x &le; endの部分を切り抜く
     *
     * @param start start x of window
     * @param end   end x of window
     * @return 対象部分のtraceを返す (deep copy)
     */
    public Trace cutWindow(double start, double end) {
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        IntStream.range(0, X.length).filter(i -> start <= X[i] && X[i] <= end).forEach(i -> {
            xList.add(X[i]);
            yList.add(Y[i]);
        });
        if (xList.isEmpty()) throw new RuntimeException("No data in [" + start + ", " + end + "]");
        return new Trace(xList.stream().mapToDouble(Double::doubleValue).toArray(),
                yList.stream().mapToDouble(Double::doubleValue).toArray());
    }

    /**
     * @param timeWindow {@link Timewindow} for cut
     * @return timeWindowの内部に属する部分を切り取ったものをnewして返す
     */
    public Trace cutWindow(Timewindow timeWindow) {
        return cutWindow(timeWindow.getStartTime(), timeWindow.getEndTime());
    }

    /**
     * @return DEEP copy of x
     */
    public double[] getX() {
        return X.clone();
    }

    /**
     * @return DEEP copy of y
     */
    public double[] getY() {
        return Y.clone();
    }

    /**
     * @return x which gives maximum y
     */
    public double getXforMaxValue() {
        return X[Y_VECTOR.getMaxIndex()];
    }

    /**
     * @return x which gives minimum y
     */
    public double getXforMinValue() {
        return X[Y_VECTOR.getMinIndex()];
    }

    /**
     * @return maximum value of y
     */
    public double getMaxValue() {
        return Y_VECTOR.getMaxValue();
    }

    /**
     * @return minimum value of y
     */
    public double getMinValue() {
        return Y_VECTOR.getMinValue();
    }

    /**
     * @return (deep) copy of x
     */
    public RealVector getXVector() {
        return X_VECTOR.copy();
    }

    /**
     * @return (deep) copy of y
     */
    public RealVector getYVector() {
        return Y_VECTOR.copy();
    }

    /**
     * x in this and trace must be same. i.e. all the x elements must be same
     *
     * @param trace to be added
     * @return new Trace after the addition
     */
    public Trace add(Trace trace) {
        if (!Arrays.equals(X, trace.X)) throw new IllegalArgumentException("Trace to be added has different X axis.");
        return new Trace(X, Y_VECTOR.add(trace.Y_VECTOR).toArray());
    }

    /**
     * @param d to be multiplied
     * @return Trace which y is multiplied d
     */
    public Trace multiply(double d) {
        return new Trace(X, Y_VECTOR.mapMultiply(d).toArray());
    }

    /**
     * @return the average value of y
     */
    public double average() {
        return Arrays.stream(Y).average().getAsDouble();
    }

    /**
     * 1/n&times;&Sigma;(y<sub>i</sub> - ymean)<sup>2</sup>
     *
     * @return standard deviation of y
     */
    public double standardDeviation() {
        double average = average();
        return Arrays.stream(Y).map(d -> d - average).map(d -> d * d).sum() / Y.length;
    }

}
