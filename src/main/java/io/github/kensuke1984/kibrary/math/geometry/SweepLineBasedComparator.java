/**
 * 
 */
package io.github.kensuke1984.kibrary.math.geometry;

import java.util.Comparator;

/**
 * 
 * @version 0.0.3
 * 
 * @author Kensuke Konishi
 * 
 */
class SweepLineBasedComparator implements Comparator<LineSegment> {
	private Line sweepLine;
	private Line belowLine;

	SweepLineBasedComparator() {
		setY(0);
	}

	// 走査線のy座標を更新
	void setY(double y) {
		// 走査線を更新
		sweepLine = Line.pointsToLine(0, y, 1, y);
		// 走査線の少し下を通る線を作成
		belowLine = Line.pointsToLine(0, y + 0.1, 1, y + 0.1);
	}

	// Comparator<LineSegment>の実装
	@Override
	public int compare(LineSegment s1, LineSegment s2) {
		int c = compareByLine(s1, s2, sweepLine);
		if (c == 0) // 走査線上の交点が等しい場合は，走査線の少し下の位置で比較
			c = compareByLine(s1, s2, belowLine);

		return c;
	}

	/**
	 * s1とs2を，lineとの交点のx座標にもとづいて比較
	 * 
	 * @param s1
	 * @param s2
	 * @param line
	 * @return
	 */
	private static int compareByLine(LineSegment s1, LineSegment s2, Line line) {
		Point2D p1 = s1.toLine().getIntersectionPoint(line);
		Point2D p2 = s2.toLine().getIntersectionPoint(line);
		// 交点がnull（線分とlineが平行）の場合は線分の1端点を比較値に採用
		double x1 = p1 != null ? p1.getX() : s1.getPointA().x;
		double x2 = p2 != null ? p2.getX() : s2.getPointA().x;
		return Double.compare(x1, x2);
	}
}
