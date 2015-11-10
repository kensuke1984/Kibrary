/**
 * 
 */
package mathtool.geometry;

/**
 * @since 2014/02/04
 * @version 0.0.1
 * 
 * @author kensuke
 * 
 */
public class Intersection {
	LineSegment lineSegment1;
	LineSegment lineSegment2;

	public Intersection(LineSegment lineSegment1, LineSegment lineSegment2) {
		this.lineSegment1 = lineSegment1;
		this.lineSegment2 = lineSegment2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		// final int prime = 31;
		int result = lineSegment1.hashCode() + lineSegment2.hashCode();

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Intersection other = (Intersection) obj;
		if (lineSegment1.equals(other.lineSegment1)
				&& lineSegment2.equals(other.lineSegment2)) {
			return true;
		} else if (lineSegment1.equals(other.lineSegment2)
				&& lineSegment2.equals(other.lineSegment1)) {
			return true;
		} else
			return false;
	}

	public Point2D getIntersectionPoint() {
		return lineSegment1.getIntersectionPoint(lineSegment2);
	}

	@Override
	public String toString() {
		return lineSegment1 + " : " + lineSegment2;
	}

}
