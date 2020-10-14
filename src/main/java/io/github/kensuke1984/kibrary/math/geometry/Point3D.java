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
 *          point (x, y, z)
 *
 * @author Anselme Borgeaud
 *
 */
public class Point3D {

	final double x;
	final double y;
	final double z;

	public Point3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}
	
	public double getZ() {
		return z;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + "," + z + ")";
	}

}
