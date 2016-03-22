package io.github.kensuke1984.kibrary.math;

import java.util.Arrays;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.complex.Complex;

/**
 * 
 * Solver for cubic equations with coefficients of real numbers.
 * 
 * @version 0.1.0.3
 * @author Kensuke Konishi
 *
 */

public class LinearEquation {

	public static void main(String[] args) {
		double[] coef = { 10375.513863539234, -23198.069259778826, 21407.688976409227, -11078.20626807574 };
		LinearEquation le = new LinearEquation(coef);
		System.out.println(le);
		for (Complex c : le.compute())
			System.out.println(c);
	}

	private PolynomialFunction pf = null;
	private double[] coef;

	/**
	 * coef[i] * x^ i (i=0,...)
	 * 
	 * @param coef
	 *            coefficients a[i] i=0,1,...
	 */
	public LinearEquation(double... coef) {
		pf = new PolynomialFunction(coef);
		this.coef = pf.getCoefficients();
	}

	/**
	 * @return max degree
	 */
	public int getDegree() {
		return pf.degree();
	}

	@Override
	public String toString() {
		return pf.toString();
	}

	/**
	 * @return answers of the equation.
	 * @throws RuntimeException
	 *             if cannot be solved
	 */
	public Complex[] compute() {
		switch (pf.degree()) {
		case 1:
			return OneDimensionEquation();
		case 2:
			return TwoDimensionEquation();
		case 3:
			return ThreeDimensionEquation();
		default:
			throw new RuntimeException("Only cubic equation can be soloved.");
		}
	}

	private Complex[] OneDimensionEquation() {
		return new Complex[] { Complex.valueOf(-coef[0] / coef[1]) };
	}

	/**
	 * @return the answers
	 */
	private Complex[] TwoDimensionEquation() {
		// double a = 1;
		double b = coef[1] / coef[2];
		double c = coef[0] / coef[2];
		// System.out.println("Calculating "+a+"x^2 + "+b+"x + "+c+" =0");
		double D = b * b - 4 * c;
		if (D == 0) {
			return new Complex[] { Complex.valueOf(-b / 2) };
		} else if (D < 0) {
			D = Math.sqrt(-D);
			return new Complex[] { Complex.valueOf(-b / 2, -D / 2), Complex.valueOf(-b / 2, D / 2) };
		} else {
			D = Math.sqrt(D);
			return new Complex[] { Complex.valueOf((-b - D) / 2), Complex.valueOf((-b + D) / 2) };
		}
	}

	/**
	 * @return the answers
	 */
	private Complex[] ThreeDimensionEquation() {
		// double a = 1;
		double b = coef[2] / coef[3];
		double c = coef[1] / coef[3];
		double d = coef[0] / coef[3];
		// System.out.println("Calculating "+a+"x^3 + "+b+"x^2 + "+c+"x + "+d+"
		// =0");
		Complex omega = new Complex(-0.5, Math.sqrt(3) / 2);
		Complex omega2 = omega.pow(2);
		double q = c - b * b / 3;
		double r = d - b * c / 3 + 2 * b * b * b / 27;
		double rr = r * r / 4 + q * q * q / 27;
		Complex zr = new Complex(rr, 0);
		Complex sr;
		if (rr == 0)
			sr = Complex.ZERO;
		else
			sr = zr.sqrt();
		if (0 < r && sr.abs() != 0)
			sr = sr.multiply(-1);
		sr = sr.add(-r / 2);
		Complex u;
		if (sr.abs() == 0)
			u = Complex.ZERO;
		else
			u = sr.pow(1.0 / 3);
		if (u.abs() == 0)
			return new Complex[] { Complex.valueOf(-b / 3) };
		Complex v = new Complex(-q / 3, 0);
		v = v.divide(u);
		Complex[] x = new Complex[3];
		double b3 = b / 3;
		x[0] = u.add(v).subtract(b3);
		x[1] = u.multiply(omega).add(v.multiply(omega2)).subtract(b3);
		x[2] = u.multiply(omega2).add(v.multiply(omega)).subtract(b3);
		Arrays.sort(x, (o1, o2) -> {
			return Double.compare(Math.abs(o1.getImaginary()), Math.abs(o2.getImaginary()));
		});

		return x;

	}

	/**
	 * @return type of answers<br>
	 *         19: two Imaginary, 20: Double root, 21: two Reals,<br>
	 *         28: one Real &amp; two Imaginary, 29: Double Root &amp; one Real,
	 *         30: Triple root, 31: three Reals
	 */
	public int Discriminant() {
		switch (pf.degree()) {
		case 1:
			return 1;
		case 2:
			return Discriminant2();
		case 3:
			return Discriminant3();
		default:
			return -1;
		}
	}

	/**
	 * @return type of answers 19: two Imaginary, 20: Double root, 21: two Reals
	 *
	 */
	private int Discriminant2() {
		if (pf.degree() != 2)
			throw new RuntimeException(pf.toString() + "is not a 2nd order equation.");
		double a = coef[2];
		double b = coef[1];
		double c = coef[0];
		double D = b * b - 4 * a * c;
		if (D < 0)
			return 19;
		else if (D == 0)
			return 20;
		else
			return 21;
	}

	/**
	 * @return the type of answers 28: one Real & two Imaginary, 29: Double Root
	 *         & one Real, 30: Triple root, 31: three Reals
	 */
	private int Discriminant3() {
		if (pf.degree() != 3)
			throw new RuntimeException(pf.toString() + "is not a cubic equation.");
		double a = coef[3];
		double b = coef[2];
		double c = coef[1];
		double d = coef[0];
		// System.out.println("What kind of answers is that of "+a+"x^3 +
		// "+b+"x^2 + "+c+"x + "+d+" =0");
		double D = b * b * c * c + 18 * a * b * c * d - 4 * a * c * c * c - 4 * b * b * b * d - 27 * a * a * d * d;
		if (0 < D)
			return 31; // 3 Real
		if (D < 0)
			return 28; // 1 Real 2 Imaginary
		return -2 * b * b * b + 9 * a * b * c - 27 * a * a * c == 0 ? 30 : 29;
	}

}
