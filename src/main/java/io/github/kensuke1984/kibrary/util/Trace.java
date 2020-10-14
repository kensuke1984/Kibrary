package io.github.kensuke1984.kibrary.util;

import io.github.kensuke1984.kibrary.timewindow.Timewindow;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.linear.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility for a function y = f(x)
 * <p>
 * This class is <b>IMMUTABLE</b>
 * </p>
 * TODO sorted
 *
 * @author Kensuke Konishi
 * @version 0.1.4
 */
public class Trace {

    /**
     * fit X, Y in this to  y<sub>j</sub> = &sum;<sub>i</sub> (a<sub>i</sub> f<sub>i</sub> (x<sub>j</sub>))
     * by the least-square method.
     *
     * @param operators functions f<sub>i</sub>(x)
     * @return a<sub>i</sub>
     */
    public double[] fit(DoubleUnaryOperator... operators) {
        int n = operators.length;
        if (X.length < n + 1) throw new IllegalArgumentException("Too many operators input.");
        if (n == 0) throw new IllegalArgumentException("Invalid use");
        RealMatrix matrix = new Array2DRowRealMatrix(getLength(), n); // {a_ij} fj(xi)
        for (int i = 0; i < matrix.getRowDimension(); i++)
            for (int j = 0; j < matrix.getColumnDimension(); j++)
                matrix.setEntry(i, j, operators[j].applyAsDouble(X[i]));
        RealVector bb = matrix.transpose().operate(new ArrayRealVector(Y, false));
        matrix = matrix.transpose().multiply(matrix);
        return new LUDecomposition(matrix).getSolver().solve(bb).toArray();
    }

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
        indexOfDownwardConvex = IntStream.range(1, X.length - 1)
                .filter(i -> Y[i] < Y[i - 1] && 0 < (Y[i + 1] - Y[i]) * (Y[i - 1] - Y[i])).boxed()
                .sorted(Comparator.comparingDouble(o -> -Y[o] * Y[o])).mapToInt(i -> i).toArray();
        indexOfUpwardConvex = IntStream.range(1, X.length - 1)
                .filter(i -> Y[i - 1] < Y[i] && 0 < (Y[i + 1] - Y[i]) * (Y[i - 1] - Y[i])).boxed()
                .sorted(Comparator.comparingDouble(o -> -Y[o] * Y[o])).mapToInt(i -> i).toArray();
        indexOfPeak = IntStream.range(1, X.length - 1).filter(i -> 0 < (Y[i + 1] - Y[i]) * (Y[i - 1] - Y[i])).boxed()
                .sorted(Comparator.comparingDouble(o -> -Y[o] * Y[o])).mapToInt(i -> i).toArray();
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
    
    public static int findBestShiftParallel(double[] base, double[] compare) {
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
//        IntStream.range(0, gap + 1).parallel()
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
        }

        return compare.length < base.length ? bestShift : -bestShift;
    }

    /**
     * @param i index for <i>x</i> [0, length -1]
     * @return x[i], second at the ith point
     */
    public double getXAt(int i) {
        return X[i];
    }

    /**
     * @param i index (NOT time) for y [0, length -1]
     * @return y[i], amplitude at the ith point (X[i] sec)
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
     * x:(3, 4, 5) &rarr; (0, 1, 2) <br>
     * f(3) &rarr; f'(0)
     * then the value 'shift' should be -3
     *
     * @param shift value of shift
     * @return f (x - shift), the values in y are deep copied.
     */
    public Trace shiftX(double shift) {
        return new Trace(Arrays.stream(X).map(d -> d + shift).toArray(), Y);
    }

    /**
     * Interpolates a value at <i>x = c</i>, only if the time series has no point at <i>x = c</i>,
     * assuming that nearest <i>n+1</i> points are on an <i>n</i> th order function.
     * f(x) = &Sigma; a<sub>i</sub> x<sup>i</sup>
     *
     * @param n degree of function for interpolation.
     *          If <i>n = 0</i>, then the value y at the closest point returns.
     * @param c point for the value. If the value c exists in the time series (x),
     *          no interpolation is performed.
     * @return interpolated y = f(c) = &Sigma; a<sub>i</sub> c<sup>i</sup>
     */
    public double toValue(int n, double c) {
        if (X.length < n + 1) throw new IllegalArgumentException("n is too big");
        if (n < 0) throw new IllegalArgumentException("n is invalid");

        int[] j = nearPoints(n + 1, c);
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
     * @return index of maximal and minimal points. The order follows the absolute values of the points.
     */
    public int[] getIndexOfPeak() {
        return indexOfPeak.clone();
    }

    /**
     * @return index of minimal points. The order follows the absolute values of the points.
     */
    public int[] getIndexOfDownwardConvex() {
        return indexOfDownwardConvex.clone();
    }

    /**
     * @return index of maximal points. The order follows the absolute values of the points.
     */
    public int[] getIndexOfUpwardConvex() {
        return indexOfUpwardConvex.clone();
    }

    /**
     * 0 &lt; (y(x[i])-y(x[i-1]))*(y(x[i])-y(x[i+1])) and y[i-1] &lt; y[i]
     * <p>
     * index of upward convex
     * index is ordered by the absolute values of the convex.
     */
    private int[] indexOfUpwardConvex;
    /**
     * 0 &lt; (y(x[i])-y(x[i-1])) * (y(x[i]) - y(x[i+1])) and y[i] &lt; y[i-1]
     * index of downward convex
     * index is ordered by the absolute values of the convex.
     */
    private int[] indexOfDownwardConvex;
    /**
     * peak is defined as 0 &lt; (y(x[i])-y(x[i-1]))*(y(x[i])-y(x[i+1]))
     * index of a peak
     * index is ordered by the absolute values of the convex.
     */
    private int[] indexOfPeak;


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
     * @author anselme RealVector.getNorm() returns a square-rooted norm
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
//            cor /= y2 * compY2;
            cor /= Math.sqrt(y2) * compY2;
            if (corMax < cor) {
                shift = X[i] - trace.X[0];
                corMax = cor;
            }
        }
        return shift;
    }
    
    /**
     * Assume the interval of x is same as that of this.
     *
     * @param trace which length must be shorter than this.
     * @return the shift value x0 in x direction for best correlation.
     */
    public double findBestShiftParallel(Trace trace) {
        int gapLength = X.length - trace.getLength();
        if (gapLength <= 0) throw new IllegalArgumentException("Input trace must be shorter.");
        double compY2 = trace.Y_VECTOR.getNorm();
        double[] shifts = new double[gapLength + 1];
        double[] cors = new double[gapLength + 1];
        IntStream.range(0, gapLength + 1).parallel().forEach(i -> {
            double cor = 0;
            double y2 = 0;
            for (int j = 0; j < trace.getLength(); j++) {
                cor += Y[i + j] * trace.Y[j];
                y2 += Y[i + j] * Y[i + j];
            }
            cor /= Math.sqrt(y2) * compY2;
            shifts[i] = X[i] - trace.X[0];
            cors[i] = cor;
        });
        return shifts[new ArrayRealVector(cors).getMaxIndex()];
    }
    
    public double findBestShiftConsiderAmplitude(Trace trace) {
        int gapLength = X.length - trace.getLength();
        if (gapLength <= 0) throw new IllegalArgumentException("Input trace must be shorter.");
        double corMax = -1;
        double compY2 = trace.Y_VECTOR.getNorm();
        double compMax = trace.Y_VECTOR.getLInfNorm();
        double shift = 0;
        for (int i = 0; i <= gapLength; i++) {
            double cor = 0;
            double y2 = 0;
            double max = Double.MIN_VALUE;
            for (int j = 0; j < trace.getLength(); j++) {
                cor += Y[i + j] * trace.Y[j];
                y2 += Y[i + j] * Y[i + j];
                if (Math.abs(Y[j+i]) > max)
                	max = Math.abs(Y[j+i]);
            }
            cor /= y2 * compY2;
            cor *= 2 * Math.abs(compMax - max) / (compMax + max);
            if (corMax < cor) {
                shift = X[i] - trace.X[0];
                corMax = cor;
            }
        }
        return shift;
    }
    
    public double findBestL1Shift(Trace trace) {
        int gapLength = X.length - trace.getLength();
        if (gapLength <= 0) throw new IllegalArgumentException("Input trace must be shorter.");
        double l1min = Double.MAX_VALUE;
        double compY2 = trace.Y_VECTOR.getNorm();
        double shift = 0;
        for (int i = 0; i <= gapLength; i++) {
            double l1 = 0;
            double y2 = 0;
            for (int j = 0; j < trace.getLength(); j++) {
                l1 += Math.abs(Y[i + j] - trace.Y[j]);
//                y2 += Y[i + j] * Y[i + j];
            }
//            cor /= y2 * compY2;
            if (l1 < l1min) {
                shift = X[i] - trace.X[0];
                l1min = l1;
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

    /**
     * Writes X and Y.
     * Each line has X<sub>i</sub> and Y<sub>i</sub>
     *
     * @param path    of the write file
     * @param options if any
     * @throws IOException if any
     */
    public void write(Path path, OpenOption... options) throws IOException {
        List<String> outLines = new ArrayList<>(X.length);
        for (int i = 0; i < X.length; i++) {
            outLines.add(X[i] + " " + Y[i]);
        }
        Files.write(path, outLines, options);
    }
    
    public Trace removeTrend() {
    	double mean = 0;
    	for (double y : Y)
    		mean += y;
    	mean /= Y.length;
    	return new Trace(X, Y_VECTOR.mapSubtract(mean).toArray());
    }
    
    public Trace truncateToLength(int length) {
    	return new Trace(Arrays.copyOfRange(X, 0, length), Arrays.copyOfRange(Y, 0, length));
    }
    
    public double correlation(Trace trace) {
    	return Y_VECTOR.dotProduct(trace.Y_VECTOR) / (Y_VECTOR.getNorm() * trace.Y_VECTOR.getNorm());
    }

    public int[] robustPeakFinder() {
		int lag = 300;
		double threshold = 3.5;
		double influence = 0.5;
		
		double[] signals = new double[getLength()];
		double[] filteredY = new double[getLength()];
		double[] avgFilter = new double[getLength()];
		double[] stdFilter = new double[getLength()];
		
		for (int i = 0; i < lag; i++)
			filteredY[i] = Y[i];
		avgFilter[lag - 1] = mean(Y_VECTOR.getSubVector(0, lag).toArray());
		stdFilter[lag - 1] = std(Y_VECTOR.getSubVector(0, lag).toArray());
		
		for (int i = lag; i < Y.length; i++) {
//			System.out.println(y[i] + " " + Math.abs(y[i] - avgFilter[i-1]) + " " + threshold * stdFilter[i-1]);
			 if (Math.abs(Y[i] - avgFilter[i-1]) > threshold * stdFilter[i-1]) {
			    if (Y[i] > avgFilter[i-1])
			    	signals[i] = 1;
			    else
			      signals[i] = -1;
			    filteredY[i] = influence * Y[i] + (1-influence) * filteredY[i-1];
			 }
			 else {
			    signals[i] = 0;
			    filteredY[i] = Y[i];
			 }
			 double[] cutFiltered = Arrays.copyOfRange(filteredY, i - lag, i + 1);
			 avgFilter[i] = mean(cutFiltered);
			 stdFilter[i] = std(cutFiltered);
		}
		
		List<Integer> indicesList = new ArrayList<>();
		for (int i = 0; i < signals.length - 1; i++) {
			if (signals[i] == 0 && Math.abs(signals[i + 1]) == 1)
				indicesList.add(i + 1);
		}
		
		int[] indices = new int[indicesList.size()];
		for (int i = 0; i < indices.length; i++)
			indices[i] = indicesList.get(i);
		
		return indices; 
	}

	private static double mean(double[] y) {
		double sum = 0;
		for (double yy : y)
			sum += yy;
		return sum / y.length;
	}
	
	private static double std(double[] y) {
		double std = 0;
		double mean = mean(y);
		for (double yy : y)
			std += (yy - mean) * (yy - mean);
		return Math.sqrt(std / y.length);
	}
	
}
