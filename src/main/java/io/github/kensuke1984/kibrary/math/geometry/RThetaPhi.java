package io.github.kensuke1984.kibrary.math.geometry;

/**
 * 3-D spherical coordinates
 * 
 * 3次元球座標 r (0, +∞), theta [0, pi](rad), phi [0, 2*pi)(rad)
 * 
 * if input phi is out of [0, 2*pi), it will be changed to a value in [0, 2*pi)
 * 
 * 
 * @version 0.0.2.2
 * 
 * @author Kensuke Konishi
 * 
 */
public class RThetaPhi {

	/**
	 * @return &phi;[rad]
	 */
	public double getPhi() {
		return phi;
	}

	public double getR() {
		return r;
	}

	/**
	 * @return &theta; [rad]
	 */
	public double getTheta() {
		return theta;
	}

	/**
	 * 入力されたデカルト座標（x, y)に対するφを返す
	 * 
	 * @param x
	 *            in cartesian
	 * @param y
	 *            in cartesian
	 * @return &phi; [rad]
	 */
	public static double toPHIfromCartesian(double x, double y) {
		if (x == 0 && y == 0) {
			System.out.println("Phi is set 0 for x=y=0. ");
			return 0;
		}
		double r = Math.sqrt(x * x + y * y);
		double cphi = Math.acos(x / r);
		return y < 0 ? 2 * Math.PI - cphi : cphi;
	}

	/**
	 * 入力されたデカルト座標（x, y, z)に対するφを返す zは本当はいらない
	 * 
	 * @param x
	 *            in cartesian
	 * @param y
	 *            in cartesian
	 * @param z
	 *            in cartesian
	 * @return φ [rad]
	 */
	public static double toPHIfromCartesian(double x, double y, double z) {
		if (x == 0 && y == 0) {
			System.out.println("Phi is set 0 for x=y=0. ");
			return 0;
		}
		double r = Math.sqrt(x * x + y * y);
		double cphi = Math.acos(x / r);
		return y < 0 ? 2 * Math.PI - cphi : cphi;
	}

	/**
	 * 
	 * 入力されたデカルト座標（x, y, z)に対するrを返す
	 * 
	 * @param x
	 *            in cartesian
	 * @param y
	 *            in cartesian
	 * @param z
	 *            in descartes
	 * @return r (x**2+y**2+z**2)**0.5
	 */
	public static double toRfromCartesian(double x, double y, double z) {
		// System.out.println(Math.sqrt((x * x + y * y + z * z))+"R");
		return Math.sqrt((x * x + y * y + z * z));
	}

	/**
	 * 入力されたデカルト座標（x, y, z)に対するθを返す
	 * 
	 * @param x
	 *            in cartesian
	 * @param y
	 *            in cartesian
	 * @param z
	 *            in cartesian
	 * @return &theta; Acos(z/r) [rad]
	 */
	public static double toTHETAfromCartesian(double x, double y, double z) {
		return Math.acos(z / toRfromCartesian(x, y, z));
	}

	@Override
	public String toString() {
		return r + " " + theta + " " + phi;
	}

	/**
	 * [0, 2*pi) [rad]
	 */
	private double phi;

	/**
	 * (0, +∞)
	 */
	private double r;

	/**
	 * [0, pi] [rad]
	 */
	private double theta;

	/**
	 * @param r
	 *            radius
	 * @param theta
	 *            [rad]
	 * @param phi
	 *            [rad]
	 */
	public RThetaPhi(double r, double theta, double phi) {
		if (!checkValidity(r, theta, phi))
			throw new IllegalArgumentException();
		this.r = r;
		this.theta = theta;
		this.phi = phi;
		fixPhi();
	}

	/**
	 * when phi does not satisfy [0, 360), phi will be changed
	 */
	private void fixPhi() {
		if (phi < 0 || 2 * Math.PI <= phi)
			System.err.println("Input phi " + phi + " (" + Math.toDegrees(phi) + " deg) is out of range");
		while (phi < 0)
			phi += 2 * Math.PI;

		while (2 * Math.PI <= phi)
			phi -= 2 * Math.PI;
		if (phi < 0 || 2 * Math.PI <= phi)
			System.err.println(phi + "(" + Math.toDegrees(phi) + "deg) is used");
	}

	/**
	 * @param r
	 *            radius
	 * @param theta
	 *            [rad]
	 * @param phi
	 *            [rad]
	 * @return r, theta, phiからCartesianCoordinateを作る
	 */
	public static XYZ toCartesian(double r, double theta, double phi) {
		double x = r * Math.cos(phi) * Math.sin(theta);
		double y = r * Math.sin(phi) * Math.sin(theta);
		double z = r * Math.cos(theta);
		return new XYZ(x, y, z);

	}

	public XYZ toCartesian() {
		return toCartesian(r, theta, phi);
	}

	/**
	 * 
	 * @param r
	 *            radius
	 * @param theta
	 *            [rad]
	 * @param phi
	 *            [rad]
	 * @return if r, theta, phi are valid
	 */
	private static boolean checkValidity(double r, double theta, double phi) {
		boolean validity = true;
		if (r < 0) {
			System.err.println("r :" + r + " must be positive.");
			validity = false;
		}
		if (theta < 0 || Math.PI < theta) {
			double degree = theta / Math.PI * 180.0;
			System.err.println("theta :" + theta + " (" + degree
					+ ") must be between 0 and 90 deg but you have to input by radian");
			validity = false;
		}
		return validity;
	}

}
