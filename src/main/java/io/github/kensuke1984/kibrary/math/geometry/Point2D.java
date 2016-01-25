/**
 * 
 */
package io.github.kensuke1984.kibrary.math.geometry;

/**
 * Point representation
 * <p>
 * This class is <b>immutable </b>
 * 
 * @version 0.0.1
 *
 *          point (x, y)
 *
 * @author kensuke
 *
 */
public class Point2D {

	final double x;
	final double y;

	public Point2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

}
