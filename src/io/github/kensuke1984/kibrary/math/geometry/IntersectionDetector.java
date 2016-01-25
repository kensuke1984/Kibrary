/**
 * 
 */
package io.github.kensuke1984.kibrary.math.geometry;

import java.util.Collection;
import java.util.List;

/**
 * @since 2014/02/04
 * @version 0.0.1
 * 
 * @author kensuke
 * 
 */
interface IntersectionDetector {
	Collection<Intersection> execute(List<LineSegment> lineSegments);
}
