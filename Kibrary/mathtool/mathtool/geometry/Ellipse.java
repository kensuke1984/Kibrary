package mathtool.geometry;


/**
 * 楕円
 * 
 * @author Kensuke
 * 
 */
public class Ellipse {

	/**
	 * 　長軸 extended shaft (0, ∞
	 */
	private double a;

	/**
	 * 　短軸 minor axis (0, ∞
	 */
	private double b;

	/**
	 * 離心率 eccentricity
	 */
	private double e;

	/**
	 * 扁平率 flattening
	 */
	private double f;

	public double getA() {
		return a;
	}

	public double getB() {
		return b;
	}

	public double getE() {
		return e;
	}

	public double getF() {
		return f;
	}

	/**
	 * @param a
	 *            major axis 長軸
	 * @param b
	 *            minor axis 短軸
	 */
	public Ellipse(double a, double b) {
		if (a < b) {
			System.out.println("a: " + a + "must be bigger than b: " + b);
			return;
		}
		if (!(a > 0) || !(b > 0)) {
			System.out.println("a, b :" + a + ", " + b + " must pe positive.");
			return;
		}
		this.a = a;
		this.b = b;

		f = 1 - b / a;
		e = Math.sqrt(1 - (b * b / a / a));
	}

	/**
	 * @param x
	 *            to look for
	 * @return y for the x (0 &le; y)
	 */
	public double toY(double x) {
		return Math.sqrt((1 - x * x / a / a) * b * b);
	}

	/**
	 * @param y
	 *            to look for
	 * @return y座標に対するx座標 (0 &le; x)
	 */
	public double toX(double y) {
		return Math.sqrt((1 - y * y / b / b) * a * a);
	}

	/**
	 * @param theta
	 *            to look for
	 * @return r for the &theta;
	 */
	public double toR(double theta) {
		return Math.sqrt(1 / (Math.cos(theta) * Math.cos(theta) / a / a + Math
				.sin(theta) * Math.sin(theta) / b / b));
	}

	/**
	 * @param r
	 *            radius
	 * @return &theta;[rad] with the r in [0, pi/2]
	 */
	public double toTheta(double r) {
		return Math.acos(Math.sqrt((r * r - b * b) / (a * a - b * b)) * a / r);
	}

	/**
	 * @return size of this
	 */
	public double getS() {
		return Math.PI * a * b;
	}

	/**
	 * @param theta0
	 *            [0, theta1]
	 * @param theta1
	 *            [theta0, 2*pi]
	 * @return theta0からtheta1までの間の面積(亜扇形？) theta0 &lt; theta1
	 */
	public double getS(double theta0, double theta1) {
		if (theta0 > theta1 || theta0 < 0 || theta1 > 2 * Math.PI) {
			System.out.println("theta0: " + theta0 + " must be [0, theta1]");
			System.out.println("theta1: " + theta1 + " must be [theta0, 2*pi]");
			return 0;
		}

		// (x0, y0) theta0
		double r0 = toR(theta0);
		double x0 = r0 * Math.cos(theta0);
		double y0 = r0 * Math.sin(theta0);
		// System.out.println(r0+"  "+x0+"  "+y0);

		// (x1, y1) theta1
		double r1 = toR(theta1);
		double x1 = r1 * Math.cos(theta1);
		double y1 = r1 * Math.sin(theta1);

		// y0, y1 を円上にのせる
		y0 = y0 * a / b;
		y1 = y1 * a / b;

		XY xy0 = new XY(x0, y0);
		XY xy1 = new XY(x1, y1);

		double theta = Math.acos(xy0.getInnerProduct(xy1) / xy0.getR()
				/ xy1.getR());
		if ((theta1 - theta0) > Math.PI)
			theta = 2 * Math.PI - theta;

		return a * b * theta / 2;
	}

}
