package io.github.kensuke1984.kibrary.math;

/**
 * 
 * Integrand utilities
 * 
 * @version 0.0.3
 * 
 * @author Kensuke Konishi
 *
 */
public final class Integrand {

	
	private Integrand(){}

	/**
	 * integral of f(x) (start &lt; x &lt; end) start = x[0], end =x[2], x[1] =
	 * (x[2]+x[0])/2, h = x[2]-x[0] answer = h/6*(f(x[0])+4*f(x[1])+f(x[2]))
	 * http
	 * ://ja.wikipedia.org/wiki/%E3%82%B7%E3%83%B3%E3%83%97%E3%82%BD%E3%83%B3
	 * %E3%81%AE%E5%85%AC%E5%BC%8F by the rule of Simpson integral of f(x) x[0]
	 * &le; x &le; x[2] is to be obtained.
	 * 
	 * @param f0
	 *            f(x[0])
	 * @param f1
	 *            f(x[1])
	 * @param f2
	 *            f(x[2])
	 * @param h
	 *            step length
	 * @return integrand of the f
	 */
	public static double bySimpsonRule(double f0, double f1, double f2, double h) {
		return (0.16666666666666666 * (f0 + 4 * f1 + f2)) * h;
	}

	/**
	 * integral of f(x) (a &lt; x &lt; c) answer = (c-a)/6 (f(a)+4*f(b)+f(c)) b
	 * = (a+c)/2
	 * 
	 * @param a
	 *            a
	 * @param c
	 *            c
	 * @param fa
	 *            f(a)
	 * @param fb
	 *            f(b)
	 * @param fc
	 *            f(c)
	 * @return the value of integral
	 */
	public static double bySimpsonRule(double a, double c, double fa,
			double fb, double fc) {
		return 0.16666666666666666 * (c - a) * (fa + 4 * fb + fc);
	}

	/**
	 * c-a = 1 S = &int; <sup>c</sup><sub>a</sub> f(x) dx = (c-a)/6
	 * (f(a)+4*f(b)+f(c)) b = (a+c)/2
	 * 
	 * @param fa f(a)
	 * @param fb f(b)
	 * @param fc f(c)
	 * @return S
	 */
	public static double bySimpsonRule(double fa, double fb, double fc) {
		return 0.16666666666666666 * (fa + 4 * fb + fc);
	}

	/**
	 * 1: integral from 0 to 2h (a x**-0.5 + b x**0.5) dx <br>
	 * 2: integral from 0 to 3h (a x**-0.5 + b x**0.5) dx<br>
	 * 3: integral from 0 to 3h (a x**-0.5 + b x**0.5 + c x**1.5) dx<br>
	 * 4: integral from 0 to 2h (a x**0.5 + b x**1.5) dx<br>
	 * 5: integral from 0 to 3h (a x**0.5 + b x**1.5) dx<br>
	 * 6: integral from 0 to 3h (a x**0.5 + b x**1.5 + c x**2.5) dx<br>
	 * page 302 Jeffereys Jeffreys
	 * @param y
	 *            for the approximation
	 * @return value by 1
	 *
	 */
	public static double jeffreysMethod1(double[] y) {
		// double a[] = new double [3];
		// a[0] = 8/3d*Math.sqrt(2);
		// a[1] = -4/3d; /2
		// a[2] = 0;
		return 1.8856180831641267 * y[0] - 0.6666666666666666 * y[1];
	}

	/**
	 * @param y
	 *            for the approximation
	 * @return value by 2
	 *
	 */
	public static double jeffreysMethod2(double[] y) {
		// double a[] = new double [3];
		// a[0] = 2*Math.sqrt(3); /3
		// a[1] = 0;
		// a[2] = 0;
		return 1.1547005383792515 * y[0];
	}

	/**
	 * @param y
	 *            for the approximation
	 * @return value by 3
	 *
	 */
	public static double jeffreysMethod3(double[] y) {
		// double a[] = new double [3];
		// a[0]= 14/5d*Math.sqrt(3); /3
		// a[1]= -8/5d*Math.sqrt(6);
		// a[2]= 12/5d;
		return 1.616580753730952 * y[0] - 1.3063945294843615 * y[1] + 0.8
				* y[2];
	}

	/**
	 * @param y
	 *            for the approximation
	 * @return value by 4
	 *
	 */
	public static double jeffreysMethod4(double[] y) {
		// double a[] = new double [3];
		// a[0] = 16/15d*Math.sqrt(2);
		// a[1] = 4/15d; /2
		// a[2] = 0;
		return 0.7542472332656507 * y[0] + 0.13333333333333333 * y[1];
	}

	/**
	 * 
	 * @param y
	 *            for the approximation
	 * @return value by 5
	 */
	public static double jeffreysMethod5(double[] y) {
		// double a[] = new double [3];
		// a[0]= 6/5d*Math.sqrt(3);
		// a[1]= 0; /3
		// a[2]= 4/5d;
		return 0.6928203230275509 * y[0] + 0.26666666666666666 * y[2];
	}

	/**
	 * @param y
	 *            for the approximation
	 * @return value by 6
	 */
	public static double jeffreysMethod6(double[] y) {
		// double a[] = new double [3];
		// a[0]= 6/7d*Math.sqrt(3); /3
		// a[1]= 12/35d*Math.sqrt(6);
		// a[2]= 16/35d;
		return 0.494871659305393454 * y[0] + 0.27994168488950605 * y[1]
				+ 0.15238095238095237 * y[2];
	}

}
