package io.github.kensuke1984.kibrary.math;

/**
 * 
 * Integrand utilities
 * 
 * @version 0.0.4.1.1
 * 
 * @author Kensuke Konishi
 *
 */
public final class Integrand {

	private Integrand() {
	}

	/**
	 * &int;f(x)dx (a &le; x &le; b), h = b-a, answer =
	 * h/6*(f(a)+4*f((a+b)/2)+f(b)) by Simpson's rule.
	 * 
	 * @param f0
	 *            f(a)
	 * @param f1
	 *            f((a+b)/2)
	 * @param f2
	 *            f(b)
	 * @param h
	 *            b-a
	 * @return &int;f(x)dx
	 * @see <a href=
	 *      http://ja.wikipedia.org/wiki/%E3%82%B7%E3%83%B3%E3%83%97%E3%82%BD%E3%83%B3%E3%81%AE%E5%85%AC%E5%BC%8F>Japanese</a>
	 *      or <a href= https://en.wikipedia.org/wiki/Simpson's_rule>English</a>
	 * 
	 */
	public static double bySimpsonRule(double f0, double f1, double f2, double h) {
		return (f0 + 4 * f1 + f2) * h / 6;
	}

	/**
	 * 1: &int;<sub>0</sub><sup>2h</sup>(ax<sup>-1/2</sup>+bx<sup>1/2</sup>)dx
	 * 
	 * @param h
	 *            where integral interval is 0 &rarr; 2h
	 * @param y
	 *           f(h), f(2h) for the approximation
	 * @return value by 1
	 * @see "page 289 eq.1 Jeffereys &amp; Jeffreys"
	 */
	public static double jeffreysMethod1(double h, double... y) {
		return (8 * Math.sqrt(2) / 3 * y[0] - y[1] * 4 / 3) * h;
	}

	/**
	 * 2:&int;<sub>0</sub><sup>3h</sup>(ax<sup>-1/2</sup>+bx<sup>1/2</sup>)dx
	 * 
	 * @param h
	 *            where integral interval is 0 &rarr; 3h
	 * @param y
	 *            f(h), f(2h), f(3h) for the approximation
	 * @return value by 2
	 * @see "page 289 eq.2 Jeffereys &amp; Jeffreys"
	 */
	public static double jeffreysMethod2(double h, double... y) {
		return 2 * Math.sqrt(3) * y[0] * h;
	}

	/**
	 * 3: &int;<sub>0</sub><sup>3h</sup> (a x<sup>-1/2</sup> + b x<sup>1/2</sup>
	 * + c x<sup>3/2</sup>) dx
	 * 
	 * @param h
	 *            where integral interval is 0 &rarr; 3h
	 * @param y
	 *           f(h), f(2h), f(3h) for the approximation
	 * @return value by 3
	 * @see "page 289 eq.3 Jeffereys &amp; Jeffreys"
	 */
	public static double jeffreysMethod3(double h, double... y) {
		return (14 * Math.sqrt(3) / 5 * y[0] - 8 * Math.sqrt(6) / 5 * y[1] + 12 / 5.0 * y[2]) * h;
	}

	/**
	 * 4: &int;<sub>0</sub><sup>2h</sup> (a x<sup>1/2</sup> + b x<sup>3/2</sup>)
	 * dx
	 * 
	 * @param h
	 *            where integral interval is 0 &rarr; 2h
	 * @param y
	 *            f(h), f(2h) for the approximation
	 * @return value by 4
	 * @see "page 289 eq.4 Jeffereys &amp; Jeffreys"
	 */
	public static double jeffreysMethod4(double h, double... y) {
		return (16 * Math.sqrt(2) / 15 * y[0] + 4 / 15.0 * y[1]) * h;
	}

	/**
	 * 5: &int;<sub>0</sub><sup>3h</sup> (a x<sup>1/2</sup> + b x<sup>3/2</sup>)
	 * dx
	 * 
	 * @param h
	 *            where integral interval is 0 &rarr; 3h
	 * @param y
	 *            f(h), f(2h), f(3h) for the approximation
	 * @return value by 5
	 * @see "page 289 eq.5 Jeffereys &amp; Jeffreys"
	 */
	public static double jeffreysMethod5(double h, double... y) {
		return (6 * Math.sqrt(3) / 5 * y[0] + 4 / 5.0 * y[2]) * h;
	}

	/**
	 * 6: &int;<sub>0</sub><sup>3h</sup>(a x<sup>1/2</sup> + b x<sup>3/2</sup> +
	 * c x<sup>5/2</sup>) dx
	 * 
	 * @param h
	 *            where integral interval is 0 &rarr; 3h
	 * @param y
	 *           f(h), f(2h), f(3h) for the approximation
	 * @return value by 6
	 * @see "page 289 eq.6 Jeffereys &amp; Jeffreys"
	 */
	public static double jeffreysMethod6(double h, double... y) {
		return (6 * Math.sqrt(3) / 7 * y[0] + 12 * Math.sqrt(6) / 35 * y[1] + 16 / 35 * y[2]) * h;
	}

}
