package io.github.kensuke1984.kibrary.butterworth;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexUtils;
import org.apache.commons.math3.util.FastMath;

/**
 * High pass filter
 *
 * @author Kensuke Konishi
 * @version 0.0.5.1.1
 */
public class HighPassFilter extends ButterworthFilter {

    /**
     * 透過域の最小角周波数 minimum &omega; in permissible region
     */
    private double omegaP;
    /**
     * 遮断域の最大角周波数 maximum &omega; in shutting region
     */
    private double omegaS;

    /**
     * @return &omega;<sub>p</sub>
     */
    public double getOmegaP() {
        return omegaP;
    }

    /**
     * @return &omega;<sub>s</sub>
     */
    public double getOmegaS() {
        return omegaS;
    }

    /**
     * @param ap     透過域の最小振幅 （1+A<sub>p</sub><sup>2</sup>）<sup>-1</sup>
     * @param as     遮断域の最大振幅 （1+A<sub>s</sub><sup>2</sup>）<sup>-1</sup>
     * @param omegaP &omega;<sub>p</sub> 透過域の最小角周波数 minimum angular frequency in
     *               permissible region
     * @param omegaS &omega;<sub>s</sub> 遮断域の最大角周波数 maximum angular frequency in
     *               stopping region
     */
    public HighPassFilter(double ap, double as, double omegaP, double omegaS) {
        this.ap = ap;
        this.as = as;
        this.omegaP = omegaP;
        this.omegaS = omegaS;
        if (!omegaValid()) throw new IllegalArgumentException("Input parameters are invalid");
        if (omegaS < 0 || omegaP <= omegaS) throw new IllegalArgumentException("Input parameters are invalid");

        setSigmaSoverSigmaP();
        setN();
        // n=7;
        setC();
        createRecursiveFilter();
    }

    /**
     * @return if input &omega;<sub>S</sub> and &omega;<sub>P</sub> are valid
     */
    private boolean omegaValid() {
        boolean valid = true;
        double halfPI = 0.5 * Math.PI;
        if (omegaP < 0 || halfPI <= omegaP) {
            System.err.println("omegaP: " + omegaP + " is invalid");
            valid = false;
        }
        return valid;
    }

    /**
     * ap 透過域の最小振幅（1+A<sub>p</sub><sup>2</sup>）<sup>-1</sup>: 0.9<br>
     * as 遮断域の最大振幅(1+A<sub>s</sub><sup>2</sup>)<sup>-1</sup>: 0.1
     *
     * @param omegaP &omega;<sub>p</sub> 透過域の最小角周波数 minimum angular frequency in
     *               permissible region
     * @param n      n pole
     */
    public HighPassFilter(double omegaP, int n) {
        ap = 1 / 3.0;
        as = 3;
        this.omegaP = omegaP;
        if (!omegaValid()) throw new IllegalArgumentException("Input parameters are invalid");

        this.n = n;
        // setSigmaSoverSigmaP();
        nToSigmaSoverSigmaP(n);
        // setN();
        computeOmegaS();
        // n=7;
        setC();
        createRecursiveFilter();
    }

    /**
     * compute a0[] a1[] a2[] b1[] b2[]<br>
     * for all Recursive Filters H(z) = (a<sub>0</sub>+a<sub>1</sub>z+a
     * <sub>2</sub>z<sup>2</sup>)/ (1+b<sub>1</sub>z+b<sub>2</sub>z<sup>2</sup>)
     * Saito (1.1) <br>
     * For low-pass Filter, <br>
     * a0 is always 1<br>
     * a1 is -2 and -1 only at first term when n is odd.<br>
     * a2 is 1 and 0 only at first term when n is odd Saito (2.15)
     */
    private void createRecursiveFilter() {
        if (n % 2 == 0) {
            // b0 = new double[n / 2];
            b1 = new double[n / 2];
            b2 = new double[n / 2];
        } else {
            // b0 = new double[n / 2 + 1];
            b1 = new double[n / 2 + 1];
            b2 = new double[n / 2 + 1];
        }
        g = 1;
        for (int j = 0; j < n / 2; j++) {
            Complex sigmaJ = computeSigmaJ(j + 1);
            double s = sigmaJ.getReal();
            double t = sigmaJ.getImaginary();
            double b0 = (c + t) * (c + t) + s * s;
            b1[j] = 2 * (c * c - sigmaJ.abs() * sigmaJ.abs());
            b2[j] = (c - t) * (c - t) + s * s;
            g /= b0;
            b1[j] /= b0;
            b2[j] /= b0;
            // System.out.println(b1[j] + " " + b2[j]);
        }
        if (n % 2 == 1) {
            int j = n / 2;
            Complex sigmaJ = computeSigmaJ(j + 1);
            double t = sigmaJ.getImaginary();
            g /= c + t;
            b1[j] = (c - t) / (c + t);
        }
    }

    /**
     * &sigma;<sub>j</sub> = exp(i*(2j-1)*&pi;/2n) = s + t*i Saito (2.5)
     *
     * @param j j
     * @return &sigma;<sub>j</sub>
     */
    private Complex computeSigmaJ(int j) {
        double theta = Math.PI * (2 * j - 1) / (2 * n);
        return ComplexUtils.polar2Complex(1, theta);
    }

    private void setC() {
        double c2 = FastMath.pow(ap * as, 1.0 / n) * FastMath.tan(omegaP / 2) * FastMath.tan(omegaS / 2);
        c = FastMath.sqrt(c2);
    }

    /**
     * computes 2.19
     */
    @Override
    void setSigmaSoverSigmaP() {
        sigmaSoverSigmaP = FastMath.tan(omegaP / 2) / FastMath.tan(omegaS / 2);
    }

    /**
     * By input n, computes &omega;<sub>s</sub>
     */
    private void computeOmegaS() {
        omegaS = FastMath.atan(FastMath.tan(omegaP / 2) / sigmaSoverSigmaP) * 2;
    }

    @Override
    public Complex getFrequencyResponse(double omega) {
        Complex response = Complex.valueOf(g);
        for (int j = 0; j < n / 2; j++) {
            // System.out.println("yo");
            // Saito 1.7 (a1j +(a2j+1) cos ω -i(a2j-1)sinω)
            Complex numerator = Complex.valueOf(-2 + 2 * FastMath.cos(omega));
            Complex denominator =
                    Complex.valueOf(b1[j] + FastMath.cos(omega) * (b2[j] + 1), -FastMath.sin(omega) * (b2[j] - 1));
            response = response.multiply(numerator).divide(denominator);
        }
        if (n % 2 == 1) {
            int j = n / 2;
            Complex numerator = Complex.valueOf(-1 + FastMath.cos(omega), FastMath.sin(omega));
            Complex denominator = Complex.valueOf(b1[j] + FastMath.cos(omega), FastMath.sin(omega));
            response = response.multiply(numerator).divide(denominator);
        }
        if (backward) response = Complex.valueOf(response.abs() * response.abs());

        return response;
    }

    @Override
    public Complex[] applyFilter(Complex[] data) {
        // data length
        Complex[] y = new Complex[data.length];
        System.arraycopy(data, 0, y, 0, data.length);

        // Complex[] x = data;
        for (int j = 0; j < n / 2; j++) {
            // System.out.println("yo");
            Complex[] x = y;
            y = computeRecursion(b1[j], b2[j], x);
        }
        if (n % 2 == 1) {
            int j = n / 2;
            Complex[] x = y;
            // y = computeRecursion(b1[j], b2[j], x);
            y = new Complex[x.length];
            y[0] = x[0];
            y[1] = x[1].subtract(x[0]).subtract(y[0].multiply(b1[j]));
            for (int i = 2; i < x.length; i++)
                y[i] = x[i].subtract(x[i - 1]).subtract(y[i - 1].multiply(b1[j]));
        }
        // System.out.println(g);
        for (int i = 0; i < y.length; i++)
            y[i] = y[i].multiply(g);
        if (backward) {
            Complex[] reverseY = new Complex[y.length];
            for (int i = 0; i < y.length; i++)
                reverseY[i] = y[y.length - i - 1];

            for (int j = 0; j < n / 2; j++) {
                // System.out.println("yo");
                Complex[] x = reverseY;
                reverseY = computeRecursion(b1[j], b2[j], x);
            }
            if (n % 2 == 1) {
                // System.out.println("orz");
                int j = n / 2;
                Complex[] x = reverseY;
                reverseY = new Complex[x.length];
                reverseY[0] = x[0];
                reverseY[1] = x[1].subtract(x[0]).subtract(reverseY[0].multiply(b1[j]));
                for (int i = 2; i < x.length; i++)
                    reverseY[i] = x[i].subtract(x[i - 1]).subtract(reverseY[i - 1].multiply(b1[j]));
            }
            for (int i = 0; i < reverseY.length; i++)
                reverseY[i] = reverseY[i].multiply(g);
            for (int i = 0; i < y.length; i++)
                y[i] = reverseY[y.length - i - 1];
        }
        return y;
    }

    /**
     * <sub></sub>
     * y[t]=a<sub>0</sub>x[t]+a<sub>1</sub>x[t-1]+a2<sub>2</sub>x[t-2]-b<sub>1</sub>y[t-1]-b<sub>2</sub>y[t-2] <br>
     * a<sub>0</sub> =1, a<sub>1</sub>= 0, a<sub>2</sub> = -1
     *
     * @param b1 b<sub>1</sub>
     * @param b2 b<sub>2</sub>
     * @param x  x
     * @return {@link Complex}[] y
     */
    private static Complex[] computeRecursion(double b1, double b2, Complex[] x) {
        Complex[] y = new Complex[x.length];
        y[0] = x[0];
        y[1] = x[1].subtract(x[0].multiply(2)).subtract(y[0].multiply(b1));
        for (int i = 2; i < x.length; i++)
            y[i] = x[i].subtract(x[i - 1].multiply(2)).add(x[i - 2]).subtract(y[i - 1].multiply(b1))
                    .subtract(y[i - 2].multiply(b2));
        return y;
    }

}
