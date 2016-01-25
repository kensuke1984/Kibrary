package io.github.kensuke1984.kibrary.math.geometry;

/**
 * xy plane
 * This class is <b>immutable</b>
 * @version 0.0.1
 * 
 * @author Kensuke
 * 
 */
class XY {

	final double x;

	final double y;

	XY(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * @param theta
	 *            [rad]
	 * @return XY rotated by theta counter-clockwisely
	 */
	XY rotate(double theta) {
		double x = Math.cos(theta) * this.x - Math.sin(theta) * this.y;
		double y = Math.sin(theta) * this.x + Math.cos(theta) * this.y;
		return new XY(x, y);
	}

	/**
	 * @param xy target
	 * @return xyとの内積 inner product with the xy
	 */
	double getInnerProduct(XY xy) {
		return this.x * xy.x + this.y * xy.y;
	}

	/**
	 * @return distance from the origin (0,0)
	 */
	double getR() {
		return Math.sqrt(x * x + y * y);
	}

	@Override
	public String toString() {
		return x + " " + y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	/**
	 * @param point1 point
	 * @param point2 point
	 * @return value of cross point1 * point2
	 */
	static double cross(Point2D point1, Point2D point2) {
		return point1.x * point2.y - point2.x * point1.y;
	}

	/**
	 * counter clock wise the line is p1 &rarr; p2 &rarr; p3
	 * 
	 * @param p1 point
	 * @param p2 point
	 * @param p3 point
	 * @return 0: straight, positive: counter clock, negative: clockwise
	 */
	static double ccw(Point2D p1, Point2D p2, Point2D p3) {
		Point2D p12 = new Point2D(p2.x - p1.x, p2.y - p1.y);
		Point2D p23 = new Point2D(p3.x - p2.x, p3.y - p2.y);
		return cross(p12, p23);
	}

	static double dotProduct(Point2D point1, Point2D point2) {
		return point1.x * point2.x + point1.y * point2.y;
	}

}
