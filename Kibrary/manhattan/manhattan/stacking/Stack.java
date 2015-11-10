package manhattan.stacking;

import filehandling.sac.SACComponent;
import filehandling.sac.WaveformType;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.Trace;

/**
 * @since 2013/7/18
 * @version 0.0.1 スタックするためのTraceを作る そのまま時間軸を同じにして足し合わせればいい
 * 　　　　　　　　　　（そのために時間軸をずらす（onsetを０にするとか。。））
 * @author kensuke
 * 
 */
public interface Stack {
	/**
	 * 
	 * @param stationName
	 *            ステーション名
	 * @param globalCMTID
	 *            　イベント名
	 * @param component
	 *            　波形の成分
	 * @param type
	 *            観測波形か理論波形か
	 * @param trace
	 *            元になる {@link Trace}
	 * @return　traceから実装したスタックルールに乗っ取って、スタック用にnewした{@link Trace}
	 *         　時間軸、振幅をいじってあるかもしれない
	 */
	Trace stack(String stationName, GlobalCMTID globalCMTID, SACComponent component,
			WaveformType type, Trace trace);

}
