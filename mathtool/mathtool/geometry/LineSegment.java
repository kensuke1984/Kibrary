/**
 * 
 */
package mathtool.geometry;

/**
 * @since 2014/02/04
 * @version 0.0.1
 * 
 *          line segment between {@link Point2D} pointA and point B
 * 
 * @version 0.0.2
 * @since 2014/7/16 capselization
 * 
 * @version 0.0.3
 * @since 2015/1/5
 * {@link #on(Point2D)} installed.
 * 
 * @author kensuke
 * 
 */
public class LineSegment {

	public Point2D getPointA() {
		return pointA;
	}

	public Point2D getPointB() {
		return pointB;
	}

	private Point2D pointA;

	private Point2D pointB;

	public LineSegment(Point2D pointA, Point2D pointB) {
		super();
		this.pointA = pointA;
		this.pointB = pointB;
	}

	public Line toLine() {
		return Line.pointsToLine(pointA.x, pointA.y, pointB.x, pointB.y);
	}

	/**
	 * ax1 + by1 + c &lt; 0
	 * 
	 * @param line to check
	 * @return if this intersects the line
	 */
	public boolean intersects(Line line) {
		double t1 = line.getA() * pointA.x + line.getB() * pointA.y
				+ line.getC(); // 端点1の座標を直線の式に代入
		double t2 = line.getA() * pointB.x + line.getB() * pointB.y
				+ line.getC(); // 端点2の座標を直線の式に代入
		return t1 * t2 <= 0;
		// return (t1 <= 0 && t2 >= 0) || (t1 >= 0 && t2 <= 0); // 不等式の判定
	}

	/**
	 * @param line {@link Line}
	 * @return an intersection point or null if they do not intersect
	 */
	public Point2D getIntersectionPoint(Line line) {
		if (!intersects(line)) {
			return null; // 交差しない場合はnullを返す
		}
		return line.getIntersectionPoint(toLine());
	}

	/**
	 * @param lineSegment {@link LineSegment}
	 * @return intersection point or null if they do not intersect
	 */
	public Point2D getIntersectionPoint(LineSegment lineSegment) {
		if (!intersects(lineSegment)) {
			return null; // 交差しない場合はnullを返す
		}
		return lineSegment.toLine().getIntersectionPoint(toLine());
	}

	/**
	 * sが自線分の「両側」にあるかどうかを調べる
	 * 
	 * @param s {@link LineSegment}
	 * @return sが自線分の「両側」にあるかどうか 
	 */
	private boolean bothSides(LineSegment s) {
		double ccw1 = XY.ccw(pointA, s.pointA, pointB);
		double ccw2 = XY.ccw(pointA, s.pointB, pointB);
		if (ccw1 == 0 && ccw2 == 0) { // sと自線分が一直線上にある場合
			// sのいずれか1つの端点が自線分を内分していれば，sは自線分と共有部分を持つので
			// trueを返す
			return internal(s.pointA.x, s.pointA.y)
					|| internal(s.pointB.x, s.pointB.y);
		} else { // それ以外の場合
			// CCW値の符号が異なる場合にtrueを返す
			return ccw1 * ccw2 <= 0;
		}
	}

	/**
	 * (x, y)が自線分を内分しているかどうかを調べる
	 * 
	 * @param x x
	 * @param y y
	 * @return (x, y)が自線分を内分しているかどうか
	 */
	private boolean internal(double x, double y) {
		// (x, y)から端点に向かうベクトルの内積がゼロ以下であれば内分と見なす
		Point2D point1 = new Point2D(pointA.x - x, pointA.y - y);
		Point2D point2 = new Point2D(pointB.x - x, pointB.y - y);
		return XY.dotProduct(point1, point2) <= 0;
	}

	/**
	 * @param lineSegment {@link LineSegment}
	 * @return if this and lineSegment intersect
	 */
	public boolean intersects(LineSegment lineSegment) {
		// return intersects(lineSegment.toLine())
		// && lineSegment.intersects(toLine());
		return bothSides(lineSegment) && lineSegment.bothSides(this);
	}

	public static void main(String[] args) {
		LineSegment s1 = new LineSegment(new Point2D(0, 0), new Point2D(100, 0));
		LineSegment s2 = new LineSegment(new Point2D(99, 0),
				new Point2D(200, 0));
		LineSegment s3 = new LineSegment(new Point2D(101, 0), new Point2D(200,
				0));
		System.out.println(s1.intersects(s2));
		System.out.println(s1.intersects(s3));
	}

	/**
	 * @param point {@link Point2D}
	 * @return if the point is on this(including the edges.).
	 */
	public boolean on(Point2D point) {
		if (pointA.x < pointB.x) {
			if (point.x < pointA.x || pointB.x < point.x)
				return false;
		} else if (point.x < pointB.x || pointA.x < point.x)
			return false;
		
		return toLine().on(point);		
		
	}

}
