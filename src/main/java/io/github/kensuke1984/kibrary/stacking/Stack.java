package io.github.kensuke1984.kibrary.stacking;

import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;

/**
 * @author kensuke
 * @version 0.0.1 スタックするためのTraceを作る そのまま時間軸を同じにして足し合わせればいい
 *          　　　　　　　　　　（そのために時間軸をずらす（onsetを０にするとか。。））
 */
public interface Stack {
    /**
     * @param stationName ステーション名
     * @param globalCMTID 　イベント名
     * @param component   　波形の成分
     * @param type        観測波形か理論波形か
     * @param trace       元になる {@link Trace}
     * @return　traceから実装したスタックルールに乗っ取って、スタック用にnewした{@link Trace}
     * 　時間軸、振幅をいじってあるかもしれない
     */
    Trace stack(String stationName, GlobalCMTID globalCMTID, SACComponent component, WaveformType type, Trace trace);

}
