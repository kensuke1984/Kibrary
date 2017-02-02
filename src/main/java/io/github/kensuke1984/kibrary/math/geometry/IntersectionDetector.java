/**
 *
 */
package io.github.kensuke1984.kibrary.math.geometry;

import java.util.Collection;
import java.util.List;

/**
 * @author kensuke
 * @version 0.0.1
 * @since 2014/02/04
 */
interface IntersectionDetector {
    Collection<Intersection> execute(List<LineSegment> lineSegments);
}
