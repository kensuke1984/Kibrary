/**
 * 
 */
package io.github.kensuke1984.kibrary.math.geometry;

import java.util.Arrays;

/**
 * 
 * Convex polygon.
 * 
 * {@link #contains(Point2D)} modified.
 * 
 * 
 * @version 0.0.2.1
 * 
 * @author Kensuke Konishi
 * 
 */
public class ConvexPolygon {

	/**
	 * the order is important vertices
	 */
	private Point2D[] vertices;

	/**
	 * the order is important edges
	 */
	private LineSegment[] edges;

	/**
	 * input vertices must be in order.
	 * 
	 * @param vertices
	 *            {@link Point2D}s for this. They must be in order and they must
	 *            not have redundant points in the same side.
	 */
	public ConvexPolygon(Point2D... vertices) {
		int size = vertices.length;
		if (size < 3)   // 角数が3未満の場合はエラー
			throw new IllegalArgumentException();
		 
		this.vertices = vertices;
		edges = new LineSegment[size];

		// 基準となるCCW値を計算
		double ccw0 = XY.ccw(vertices[0], vertices[1], vertices[2]);
		if (ccw0 == 0)  // ゼロの場合はエラー
			throw new IllegalArgumentException("Polygon is not convex.");
		 
		for (int i = 1; i < size; i++) {
			Point2D v1 = vertices[i]; // i番目の頂点
			Point2D v2 = vertices[(i + 1) % size]; // v1の次の頂点
			Point2D v3 = vertices[(i + 2) % size]; // v2の次の頂点
			double ccw = XY.ccw(v1, v2, v3); // CCW値を計算
			if (ccw0 * ccw < 0) // 基準値と符号が異なる，またはゼロの場合はエラー
				throw new IllegalArgumentException("Polygon is not convex at " + v1 + "," + v2 + "," + v3);
			if (ccw0 * ccw == 0)
				throw new IllegalArgumentException("Polygon may not need " + v2);
		}

		for (int i = 0; i < size; i++) {
			Point2D v1 = vertices[i]; // i th vertice i番目の頂点
			Point2D v2 = vertices[(i + 1) % size]; // v1の次の頂点
			// 2つの頂点から辺の線分を作成して登録
			edges[i] = new LineSegment(v1, v2);
		}
	}

	/**
	 * @param i
	 *            index for a vertex
	 * @return i th vertex
	 */
	public Point2D getVertex(int i) {
		return vertices[i];
	}

	/**
	 * @param i
	 *            index for an edge
	 * @return i th edge
	 */
	public LineSegment getEdge(int i) {
		return edges[i];
	}

	/**
	 * @return the area of this
	 */
	public double getArea() {
		double crossSum = 0; // 外積の合計
		int size = vertices.length;
		// 頂点を巡回
		for (int i = 0; i < size; i++) {
			Point2D v1 = vertices[i];
			Point2D v2 = vertices[(i + 1) % size];
			// 外積を計算
			double cross = XY.cross(v1, v2);
			crossSum += cross; // 外積を加算
		}
		return Math.abs(crossSum / 2);
	}

	/**
	 * @param point
	 *            to look for
	 * @return if point is inside this (true if the point is on the vertices)
	 */
	public boolean contains(Point2D point) {

		// 多角形のy座標範囲を求める
		double minY = Arrays.stream(vertices).mapToDouble(p->p.y).min().getAsDouble();
		double maxY = Arrays.stream(vertices).mapToDouble(p->p.y).max().getAsDouble();;

		double x = point.x;
		double y = point.y;

		// yが最小値-最大値の範囲外の場合はfalseを返す
		if (y < minY || maxY < y)
			return false;
		else if (y == minY || y == maxY) {
			for (LineSegment edge : edges)
				if (edge.contains(point))
					return true;
			return false;
		}
		// 与えられた座標を始点とし，右方向に十分長く延びる擬似的な半直線を作成
		LineSegment halfLine = new LineSegment(new Point2D(x, y), new Point2D(x + 10000000, y));
		int count = 0;
		for (LineSegment edge : edges)
			// 半直線が辺の終点とちょうど重なる場合，次の辺の始点とも交差が検出され，
			// 二重にカウントされてしまうため，カウントをスキップする
			if (edge.contains(point))
				return true;
			else if (edge.getPointB().y == y)
				continue;
			else if (edge.intersects(halfLine))
				count++;

		// 交差回数が奇数の場合は点を包含
		return count % 2 == 1;
	}

	/**
	 * @return the number of edges (vertices)
	 */
	public int getEdgeCount() {
		return edges.length;
	}
}
