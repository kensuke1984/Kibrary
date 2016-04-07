/**
 * 
 */
package io.github.kensuke1984.kibrary.math.geometry;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

/**
 * @version 0.0.1.1
 * 
 * @author Kensuke Konishi
 * 
 */
public class PlaneSweepIntersectionDetector implements IntersectionDetector {
	// public class implements IntersectionDetector {
	@Override
	public Collection<Intersection> execute(List<LineSegment> lineSegments) {
		// イベントキューを作成
		TreeSet<Event> eventQueue = new TreeSet<>();

		for (LineSegment lineSegment : lineSegments) {
			// 線分の端点のうち上にある方を始点，下にある方を終点としてイベントを登録
			// 線分が水平な場合は左の端点を始点とする
			if (lineSegment.getPointA().y < lineSegment.getPointB().y
					|| (lineSegment.getPointA().y == lineSegment.getPointB().y
							&& lineSegment.getPointA().x < lineSegment.getPointB().x)) {
				eventQueue.add(new Event(SegmentType.SEGMENT_START, lineSegment.getPointA().x,
						lineSegment.getPointA().y, lineSegment, null));
				eventQueue.add(new Event(SegmentType.SEGMENT_END, lineSegment.getPointB().x, lineSegment.getPointB().y,
						lineSegment, null));
			} else {
				eventQueue.add(new Event(SegmentType.SEGMENT_START, lineSegment.getPointB().x,
						lineSegment.getPointB().y, lineSegment, null));
				eventQueue.add(new Event(SegmentType.SEGMENT_END, lineSegment.getPointA().x, lineSegment.getPointA().y,
						lineSegment, null));
			}
		}

		SweepLineBasedComparator sweepComparator = new SweepLineBasedComparator();
		// ステータスを作成。要素の順序関係はsweepComparatorにしたがう
		TreeSet<LineSegment> status = new TreeSet<>(sweepComparator);

		// 今回の実装では同一の交点が複数回検出される可能性があるため，HashSetを使って重複を防ぐ
		Collection<Intersection> result = new HashSet<>();

		Event event;
		// キューから先頭のイベントを取り出す
		while ((event = eventQueue.pollFirst()) != null) {
			double sweepY = event.y;
			switch (event.type) {
			case SEGMENT_START: // 始点イベントの場合
				sweepComparator.setY(sweepY); // 走査線を更新

				LineSegment newSegment = event.lineSegment1;
				status.add(newSegment); // ステータスに線分を追加

				LineSegment left = status.lower(newSegment);
				LineSegment right = status.higher(newSegment);
				// 左隣の線分との交差を調べる
				checkIntersection(left, newSegment, sweepY, eventQueue);
				// 右隣の線分との交差を調べる
				checkIntersection(newSegment, right, sweepY, eventQueue);
				break;
			case INTERSECTION: // 交点イベントの場合
				left = event.lineSegment1;
				right = event.lineSegment2;
				// 交点を戻り値に追加
				result.add(new Intersection(left, right));

				LineSegment moreLeft = status.lower(left);
				LineSegment moreRight = status.higher(right);

				// ステータス中のleftとrightの位置を交換するため，いったん削除する
				status.remove(left);
				status.remove(right);
				sweepComparator.setY(sweepY); // 走査線を更新
				// 計算誤差により，走査線の更新後も順序が交換されない場合は
				// 走査線を少し下げて順序が確実に変わるようにする
				if (sweepComparator.compare(left, right) < 0)
					sweepComparator.setY(sweepY + 0.001);

				// 更新後の走査線を基準としてleftとrightを再追加（位置が交換される）
				status.add(left);
				status.add(right);

				// right（位置交換によって新しく左側に来た線分）と，そのさらに左隣の線分の交差を調べる
				checkIntersection(moreLeft, right, sweepY, eventQueue);
				// left（位置交換によって新しく右側に来た線分）と，そのさらに右隣の線分の交差を調べる
				checkIntersection(left, moreRight, sweepY, eventQueue);
				break;
			case SEGMENT_END: // 終点イベントの場合
				LineSegment endSegment = event.lineSegment1;
				left = status.lower(endSegment);
				right = status.higher(endSegment);

				// 線分の削除によって新しく隣り合う2線分の交差を調べる
				checkIntersection(left, right, sweepY, eventQueue);
				status.remove(endSegment); // ステータスから線分を削除

				sweepComparator.setY(sweepY); // 走査線を更新
				break;
			}
		}

		return result;
	}

	// 線分leftとrightが走査線の下で交差するかどうか調べ，交差する場合は交点イベントを登録する
	private static void checkIntersection(LineSegment left, LineSegment right, double sweepY,
			TreeSet<Event> eventQueue) {
		// 2線分のうち少なくとも一方が存在しない場合，何もしない
		if (left == null || right == null)
			return;

		Point2D p = left.getIntersectionPoint(right);
		// 交点が走査線よりも下に存在するときのみ，キューに交点イベントを登録
		if (p != null && sweepY <= p.getY())
			eventQueue.add(new Event(SegmentType.INTERSECTION, p.getX(), p.getY(), left, right));

	}

}
