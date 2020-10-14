package io.github.kensuke1984.kibrary.util.addons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;

public class StructureConversion {
	private PolynomialStructure structure;
	
	public static void main(String[] args) throws IOException {
		PolynomialStructure structure = new PolynomialStructure(Paths.get("/Users/Anselme/Dropbox/Kenji/Corridor/initialModel/stw105_smallCoeff.poly"));
		StructureConversion convertor = new StructureConversion(structure);
		
		structure.writeVelocity(Paths.get("/Users/Anselme/Dropbox/Kenji/Corridor/stw105_1Hz.vel"));
		structure.writePSV(Paths.get("/Users/Anselme/Dropbox/Kenji/Corridor/stw105.poly"));
		
		PolynomialStructure newStructure = convertor.convert(0.1 * Math.PI * 2);
		newStructure.writeVelocity(Paths.get("/Users/Anselme/Dropbox/Kenji/Corridor/stw105_01Hz.vel"));
		newStructure.writePSV(Paths.get("/Users/Anselme/Dropbox/Kenji/Corridor/stw105_01Hz.poly"));
		
		newStructure = convertor.convert(0.05 * Math.PI * 2);
		newStructure.writeVelocity(Paths.get("/Users/Anselme/Dropbox/Kenji/Corridor/stw105_005Hz.vel"));
		newStructure.writePSV(Paths.get("/Users/Anselme/Dropbox/Kenji/Corridor/stw105_005Hz.poly"));
	}
	
	public StructureConversion(PolynomialStructure structure) {
		this.structure = structure;
	}
	
	public PolynomialStructure convert(double omega) {
		PolynomialStructure newStructure = structure;
		
		for (int i = 0; i < structure.getNzone(); i++) {
			double QcorrectionForS = 1. + 1. / (Math.PI * structure.getQMuOf(i)) * Math.log(omega / (2. * Math.PI));
			PolynomialFunction ph = structure.getVshOf(i).multiply(new PolynomialFunction(new double[] {QcorrectionForS}));
			PolynomialFunction pv = structure.getVsvOf(i).multiply(new PolynomialFunction(new double[] {QcorrectionForS}));
			newStructure = newStructure.setVsh(i, ph);
			newStructure = newStructure.setVsv(i, pv);
			
			double QcorrectionForP = 1. + 1. / (Math.PI * 9. / 4. * structure.getQMuOf(i)) * Math.log(omega / (2. * Math.PI));
			ph = structure.getVphOf(i).multiply(new PolynomialFunction(new double[] {QcorrectionForP}));
			pv = structure.getVpvOf(i).multiply(new PolynomialFunction(new double[] {QcorrectionForP}));
			newStructure = newStructure.setVph(i, ph);
			newStructure = newStructure.setVpv(i, pv);
		}
		
		return newStructure;
	}
}