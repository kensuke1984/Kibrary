package io.github.kensuke1984.kibrary.quick;

import java.io.IOException;
import java.nio.file.Paths;

import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

public class peakFinder {
	public static void main(String[] args) {
		SACFileName sacname = new SACFileName(Paths.get(args[0]));
		try {
			Trace trace = sacname.read().createTrace();
			int[] peakIndices = trace.robustPeakFinder();
			
			int j = 0;
			for (int i = 0; i < trace.getLength(); i++) {
				int peak = 0;
				if (peakIndices.length > 0 && j < peakIndices.length) {
					if (i == peakIndices[j]) {
						peak = 1;
						j++;
					}
				}
				System.out.println(trace.getXAt(i) + " " + trace.getYAt(i) + " " + peak);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
