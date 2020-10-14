package io.github.kensuke1984.kibrary.quick;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.math3.complex.Complex;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.spc.DSMOutput;
import io.github.kensuke1984.kibrary.util.spc.FormattedSPCFile;
import io.github.kensuke1984.kibrary.util.spc.SPCFile;
import io.github.kensuke1984.kibrary.util.spc.SPCTensorComponent;
import io.github.kensuke1984.kibrary.util.spc.Spectrum;

public class LookAtFPspc {

	public static void main(String[] args) throws IOException {
		SPCFile spcName = new FormattedSPCFile(Paths.get(args[0]));
		
		DSMOutput dsmOutput = null;
		if (args.length == 1)
			dsmOutput = spcName.read();
		else if (args.length == 2) {
			double phi = Double.parseDouble(args[1]);
			System.out.println("Phi = " + phi);
			dsmOutput = Spectrum.getInstance(spcName, phi);
		}
		
		print(dsmOutput);
	}
	
	public static void print(DSMOutput dsmOutput) {
		String obsName = dsmOutput.getObserverID();
		String netwkName = dsmOutput.getObserverNetwork();
		String sourceID = dsmOutput.getSourceID();
		HorizontalPosition observerPosition = dsmOutput.getObserverPosition();
		Location sourceLocation = dsmOutput.getSourceLocation();
		
		System.out.println("#Observer: " + obsName + " " + netwkName + " " + observerPosition + " Source: " + sourceID + " " + sourceLocation);
		
		double r = dsmOutput.getBodyR()[0];
		System.out.println("perturbation radius=" + r);
		Complex[][] spcs = new Complex[9][];
		for(int i = 1; i <= 3; i++) {
			for(int j = 1; j <= 3; j++) {
				SPCTensorComponent comp = SPCTensorComponent.valueOfFP(i, j);
				spcs[3*(i-1)+j-1] = dsmOutput.getSpcBodyList().get(0).getSpcComponent(comp).getValueInFrequencyDomain();
			}
		}
		
		for (int k = 0; k < spcs[0].length; k++) {
			String real = "";
			String imag = "";
			for (int i = 0; i < 9; i++) {
				real += String.format(" %.16e", spcs[i][k].getReal());
				imag += String.format(" %.16e", spcs[i][k].getImaginary());
			}
			System.out.println("(Real) " + k + real);
			System.out.println("(Imag) " + k + imag);
		}
	}
	
	public static void printHeader(DSMOutput dsmOutput) {
		String obsName = dsmOutput.getObserverID();
		String netwkName = dsmOutput.getObserverNetwork();
		String sourceID = dsmOutput.getSourceID();
		HorizontalPosition observerPosition = dsmOutput.getObserverPosition();
		Location sourceLocation = dsmOutput.getSourceLocation();
		
		System.out.println("#Observer: " + obsName + " " + netwkName + " " + observerPosition + " Source: " + sourceID + " " + sourceLocation);
	}
}