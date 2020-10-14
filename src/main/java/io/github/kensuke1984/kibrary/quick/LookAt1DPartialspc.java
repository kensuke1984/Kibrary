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

public class LookAt1DPartialspc {

	public static void main(String[] args) throws IOException {
		SPCFile spcName = new FormattedSPCFile(Paths.get(args[0]));
		DSMOutput dsmOutput = spcName.read();
		print(dsmOutput);
	}
	
	public static void print(DSMOutput dsmOutput) {
		String obsName = dsmOutput.getObserverID();
		String netwkName = dsmOutput.getObserverNetwork();
		String sourceID = dsmOutput.getSourceID();
		HorizontalPosition observerPosition = dsmOutput.getObserverPosition();
		Location sourceLocation = dsmOutput.getSourceLocation();
		
		System.out.println("#Observer: " + obsName + " " + netwkName + " " + observerPosition + " Source: " + sourceID + " " + sourceLocation);
		
		Complex[][] spcs = new Complex[3][];
		for(int i = 1; i <= 3; i++) {
			spcs[i-1] = dsmOutput.getSpcBodyList().get(0).getSpcComponent(SACComponent.getComponent(i)).getValueInFrequencyDomain();
		}
		
		for (int k = 0; k < spcs[0].length; k++) {
			String real = "";
			String imag = "";
			for (int i = 0; i < 3; i++) {
				real += String.format(" %.16e", spcs[i][k].getReal());
				imag += String.format(" %.16e", spcs[i][k].getImaginary());
			}
			System.out.println("(Real) " + k + real);
			System.out.println("(Imag) " + k + imag);
		}
	}
	
	//TODO maybe
	public static void printHeader(DSMOutput dsmOutput) {
		String obsName = dsmOutput.getObserverID();
		String netwkName = dsmOutput.getObserverNetwork();
		String sourceID = dsmOutput.getSourceID();
		HorizontalPosition observerPosition = dsmOutput.getObserverPosition();
		Location sourceLocation = dsmOutput.getSourceLocation();
		
		System.out.println("#Observer: " + obsName + " " + netwkName + " " + observerPosition + " Source: " + sourceID + " " + sourceLocation);
	}
}