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
class Event implements Comparable<Event> {

	SegmentType type;

	double x;
	double y;

	/**
	 * 点に関連する線分1
	 */
	LineSegment lineSegment1;

	/**
	 * 点に関連する線分2（type = INTERSECTIONのときのみ使用）
	 */
	LineSegment lineSegment2;

	Event(SegmentType type, double x, double y, LineSegment lineSegment1,
			LineSegment lineSegment2) {
		this.type = type;
		this.x = x;
		this.y = y;
		this.lineSegment1 = lineSegment1;
		this.lineSegment2 = lineSegment2;
	}

	// Comparable<Event>>の実装
	public int compareTo(Event event) {
		int c = Double.compare(y, event.y); // イベント点のy座標を比較
		if (c == 0) { // y座標が等しい場合はx座標を比較
			c = Double.compare(x, event.x);
		}
		return c;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
