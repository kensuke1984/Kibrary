package io.github.kensuke1984.kibrary.quick;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.math3.complex.Complex;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.spc.DSMOutput;
import io.github.kensuke1984.kibrary.util.spc.SpcFileName;
import io.github.kensuke1984.kibrary.util.spc.SpcTensorComponent;

public class LookAtFPspc {

	public static void main(String[] args) throws IOException {
		SpcFileName spcName = new SpcFileName(Paths.get(args[0]));
		DSMOutput dsmOutput = spcName.read();
		String obsName = dsmOutput.getObserverName();
		String netwkName = dsmOutput.getObserverNetwork();
		String sourceID = dsmOutput.getSourceID();
		HorizontalPosition observerPosition = dsmOutput.getObserverPosition();
		Location sourceLocation = dsmOutput.getSourceLocation();
		
		System.out.println("#Observer: " + obsName + " " + netwkName + " " + observerPosition + " Source: " + sourceID + " " + sourceLocation);
		
		double r = dsmOutput.getBodyR()[0];
		System.out.println("perturbation radius=" + r);
		String s = "";
		for(int i = 1; i <= 3; i++) {
			for(int j = 1; j <= 3; j++) {
				SpcTensorComponent comp = SpcTensorComponent.valueOfFP(i, j);
				Complex[] spc = dsmOutput.getSpcBodyList().get(0).getSpcComponent(comp).getValueInFrequencyDomain();
				double[] spcAbs = new double[spc.length];
				double maxAbs = Double.MIN_VALUE;
				for (int k = 0; k < spc.length; k++) {
					spcAbs[k] = spc[k].abs();
					if (spcAbs[k] > maxAbs)
						maxAbs = spcAbs[k];
				}
				s += String.format("%.5e ", maxAbs);
			}
		}
		System.out.println(s);
	}
}