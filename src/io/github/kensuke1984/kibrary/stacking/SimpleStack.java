package io.github.kensuke1984.kibrary.stacking;

import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;

/**
 * 
 * @version 0.0.1
 * ただ、足し合わせるだけ。　タイムウインドウの始めの値を０にするだけ
 * 今の段階では、samplingHzは１秒に固定 
 * @author kensuke
 * 
 *
 */
public class SimpleStack implements Stack {

//	private double samplingHz = 1;
	
	
	@Override
	public Trace stack(String stationName, GlobalCMTID eventName,
			SACComponent component, WaveformType type, Trace trace) {
		
		
		double[] t = new double[trace.getLength()];
		for(int i =0;i<t.length;i++)
			t[i] = i;
		
		return new Trace(t, trace.getY());
	}

	
	


}
