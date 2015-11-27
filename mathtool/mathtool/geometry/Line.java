/**
 * 
 */
package mathtool.geometry;

/**
 * 
 * This class is after <a
 * href=http://gihyo.jp/dev/serial/01/geometry/0001>here</a>
 * <p>
 * ax + by + c = 0
 * </p>
 * 
 * @version 0.0.2
 * 
 * 
 * @author kensuke
 * 
 */
class Line {

	final double a;
	final double b;
	final double c;

	/**
	 * @param x1
	 *            x of A
	 * @param y1
	 *            y of A
	 * @param x2
	 *            x of B
	 * @param y2
	 *            y of B
	 * @return line which goes through A(x1, y1) and B(x2, y2)
	 */
	public static Line pointsToLine(double x1, double y1, double x2, double y2) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		return new Line(dy, -dx, dx * y1 - dy * x1);
	}

	/**
	 * @param line
	 *            to look for an intersection point with
	 * @return intersection point of this and line or null if lines are parallel
	 */
	public Point2D getIntersectionPoint(Line line) {
		double d = a * line.b - line.a * b;
		if (d == 0.0)
			throw new RuntimeException("The input line is parallel to this"); // 直線が平行の場合はnullを返す

		double x = (b * line.c - line.b * c) / d;
		double y = (line.a * c - a * line.c) / d;
		return new Point2D(x, y);
	}

	/**
	 * creates a line ax + by + c = 0
	 * 
	 * @param a
	 *            a
	 * @param b
	 *            b
	 * @param c
	 *            c
	 */
	Line(double a, double b, double c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	/**
	 * @param point
	 *            to check
	 * @return if the point is on this.
	 */
	public boolean on(Point2D point) {
		return a * point.x + b * point.y + c == 0;
	}

}
