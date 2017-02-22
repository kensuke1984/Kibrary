/**
 * 
 */
package io.github.kensuke1984.kibrary.util;

import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import java.util.*;

/**
 * @version 0.0.1
 * @since 2017/02/21
 * @author Yuki
 *
 */
public interface Polyhedron {

	public Point3d[] getVertices();
	public List<int[]> getLineLoops();
	public double getCirumscribedRadius();
	public int getViewCount();
	public String getViewName(int index);
	public Matrix3d getViewMatrix(int index);
}
