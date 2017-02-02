package io.github.kensuke1984.kibrary.math.geometry;

/**
 * @author Kensuke Konishi
 * @version 0.0.1.1
 */
public class Intersection {
    LineSegment lineSegment1;
    LineSegment lineSegment2;

    public Intersection(LineSegment lineSegment1, LineSegment lineSegment2) {
        this.lineSegment1 = lineSegment1;
        this.lineSegment2 = lineSegment2;
    }

    @Override
    public int hashCode() {
        return lineSegment1.hashCode() + lineSegment2.hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Intersection other = (Intersection) obj;
        return lineSegment1.equals(other.lineSegment1) && lineSegment2.equals(other.lineSegment2) ||
                lineSegment1.equals(other.lineSegment2) && lineSegment2.equals(other.lineSegment1);
    }

    public Point2D getIntersectionPoint() {
        return lineSegment1.getIntersectionPoint(lineSegment2);
    }

    @Override
    public String toString() {
        return lineSegment1 + " : " + lineSegment2;
    }

}
