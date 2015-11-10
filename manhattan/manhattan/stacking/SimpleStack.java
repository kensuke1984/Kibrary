package manhattan.stacking;

import filehandling.sac.SACComponent;
import filehandling.sac.WaveformType;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.Trace;

/**
 * 
 * @version 0.0.1
 * @since 2013/8/22
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
