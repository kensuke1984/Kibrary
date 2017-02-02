/**
 *
 */
package io.github.kensuke1984.kibrary.math.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author kensuke
 * @version 0.0.1
 * @since 2014/02/04
 */
public class BruteForceIntersectionDetector implements IntersectionDetector {

    /*
     * (non-Javadoc)
     *
     * @see mathtool.geometry.IntersectionDetector#execute(java.util.List)
     */
    @Override
    public Collection<Intersection> execute(List<LineSegment> lineSegments) {
        List<Intersection> result = new ArrayList<>();
        int size = lineSegments.size();
        for (int i = 0; i < size; i++) {
            LineSegment s1 = lineSegments.get(i);
            // j < iの場合は調査済み，またj = iの場合は2線分が同一となり交差を調べる
            // 必要がないため，j = i + 1からループを開始する
            for (int j = i + 1; j < size; j++) {
                LineSegment s2 = lineSegments.get(j);
                if (s1.intersects(s2)) {
                    result.add(new Intersection(s1, s2));
                }
            }
        }
        return result;
    }


}
