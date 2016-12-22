package io.github.kensuke1984.kibrary.stacking;

import java.util.HashMap;
import java.util.Map;

import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;

/**
 * TODO まだ作成中 ピックした時刻など、ある時刻を基準にしたスタックをするためのStack
 * 
 * 設定されている時刻を０（基準時刻）にずらしたTraceを返す。 振幅はいじらない
 * @version 0.0.1
 * @author Kensuke Konishi
 * 
 */
public class BaseTimeStack implements Stack {

	@Override
	public Trace stack(String station, GlobalCMTID globalCMTID,
			SACComponent component, WaveformType type, Trace trace) {
		Key key = new Key(station, globalCMTID, component);
		if (!baseTimeMap.containsKey(key))
			throw new RuntimeException("The data for " + station + "."
					+ globalCMTID + "." + component + " doesn't exist.");
		return stack(trace, baseTimeMap.get(key));
	}

	/**
	 * Traceの時間軸を基準時刻=0にセットする（newする）
	 * 
	 * @param trace
	 *            　元になるTrace (変更されない)
	 * @param baseTime
	 *            基準時刻。　０になる
	 * @return trace for the stack
	 */
	public static Trace stack(Trace trace, double baseTime) {
		return trace.shiftX(-baseTime);
	}

	/**
	 * 各波形のベース時刻
	 */
	private Map<Key, Double> baseTimeMap = new HashMap<>();

	/**
	 * 基準時刻をセットする
	 * 
	 * @param station
	 *            station name
	 * @param id
	 *            {@link GlobalCMTID}
	 * @param component
	 *            {@link SACComponent}
	 * @param baseTime
	 *            base time
	 */
	public void addBaseTime(String station, GlobalCMTID id,
			SACComponent component, double baseTime) {
		Key key = new Key(station, id, component);
		if (baseTimeMap.containsKey(key))
			throw new RuntimeException("The data for " + station + "." + id
					+ "." + component + " already exists.");
		baseTimeMap.put(key, baseTime);
	}

	private class Key {
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((component == null) ? 0 : component.hashCode());
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result
					+ ((station == null) ? 0 : station.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (component != other.component)
				return false;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (station == null) {
				if (other.station != null)
					return false;
			} else if (!station.equals(other.station))
				return false;
			return true;
		}

		private String station;
		private GlobalCMTID id;
		private SACComponent component;

		// private WaveformType type;
		// private Trace trace;
		private Key(String station, GlobalCMTID id, SACComponent component) {
			this.station = station;
			this.id = id;
			this.component = component;
		}

		private BaseTimeStack getOuterType() {
			return BaseTimeStack.this;
		}

	}

}
