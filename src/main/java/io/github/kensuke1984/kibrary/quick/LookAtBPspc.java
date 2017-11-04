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

public class LookAtBPspc {

	public static void main(String[] args) throws IOException {
		SpcFileName spcName = new SpcFileName(Paths.get(args[0]));
		DSMOutput dsmOutput = spcName.read();
		String obsName = dsmOutput.getObserverName();
		String netwkName = dsmOutput.getObserverNetwork();
		String sourceID = dsmOutput.getSourceID();
		HorizontalPosition observerPosition = dsmOutput.getObserverPosition();
		Location sourceLocation = dsmOutput.getSourceLocation();
		
		System.out.println("#Observer: " + obsName + " " + netwkName + " " + observerPosition + " Source: " + sourceID + " " + sourceLocation);
		
		SpcTensorComponent three = SpcTensorComponent.valueOfBP(3, 3, 3);
		SpcTensorComponent two = SpcTensorComponent.valueOfBP(2, 2, 2);
		SpcTensorComponent one = SpcTensorComponent.valueOfBP(1, 1, 1);
		Complex[] spcT0 = dsmOutput.getSpcBodyList().get(0).getSpcComponent(three).getValueInFrequencyDomain(); 
		Complex[] spcR0 = dsmOutput.getSpcBodyList().get(0).getSpcComponent(two).getValueInFrequencyDomain();
		Complex[] spcZ0 = dsmOutput.getSpcBodyList().get(0).getSpcComponent(one).getValueInFrequencyDomain();
		double[] rs = dsmOutput.getBodyR();
		System.out.println("#perturbation radius= " + rs[0]);
		for (int i = 0; i < spcT0.length; i++) {
			System.out.printf("%d %.5e %.5e %.5e\n", i, spcT0[i].abs(), spcR0[i].abs(), spcZ0[i].abs());
		}
	}
}