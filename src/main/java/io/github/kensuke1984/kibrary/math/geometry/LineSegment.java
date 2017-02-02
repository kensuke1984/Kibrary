/**
 *
 */
package io.github.kensuke1984.kibrary.math.geometry;

/**
 * Line segment<br>
 * <p>
 * This class is <b>immutable</b>.
 *
 * @author Kensuke Konishi
 * @version 0.0.4
 */
class LineSegment {

    private Point2D pointA;
    private Point2D pointB;

    LineSegment(Point2D pointA, Point2D pointB) {
        this.pointA = pointA;
        this.pointB = pointB;
    }

    public Point2D getPointA() {
        return pointA;
    }

    public Point2D getPointB() {
        return pointB;
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
        double t1 = line.a * pointA.x + line.b * pointA.y + line.c; // 端点1の座標を直線の式に代入
        double t2 = line.a * pointB.x + line.b * pointB.y + line.c; // 端点2の座標を直線の式に代入
        return t1 * t2 <= 0;
    }

    /**
     * @param line {@link Line}
     * @return an intersection point or null if they do not intersect
     */
    public Point2D getIntersectionPoint(Line line) {
        return intersects(line) ? line.getIntersectionPoint(toLine()) : null; // 交差しない場合はnullを返す
    }

    /**
     * @param lineSegment {@link LineSegment}
     * @return intersection point or null if they do not intersect
     */
    public Point2D getIntersectionPoint(LineSegment lineSegment) {
        // 交差しない場合はnullを返す
        return intersects(lineSegment) ? lineSegment.toLine().getIntersectionPoint(toLine()) : null;
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
        return (ccw1 == 0 && ccw2 == 0) ? // sと自線分が一直線上にある場合
                // sのいずれか1つの端点が自線分を内分していれば，sは自線分と共有部分を持つので
                // trueを返す
                internal(s.pointA.x, s.pointA.y) || internal(s.pointB.x, s.pointB.y) :
                // それ以外の場合 CCW値の符号が異なる場合にtrueを返す
                ccw1 * ccw2 <= 0;
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
        return bothSides(lineSegment) && lineSegment.bothSides(this);
    }

    /**
     * @param point {@link Point2D}
     * @return if the point is on this(including the edges.).
     */
    public boolean contains(Point2D point) {
        if (point.x < Double.min(pointA.x, pointB.x) || Double.max(pointA.x, pointB.x) < point.x) return false;
        if (point.y < Double.min(pointA.y, pointB.y) || Double.max(pointA.y, pointB.y) < point.y) return false;
        return toLine().on(point);
    }

}
