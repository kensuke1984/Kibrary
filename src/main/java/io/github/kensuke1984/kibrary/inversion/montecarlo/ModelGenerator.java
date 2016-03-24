/**
 * 
 */
package io.github.kensuke1984.kibrary.inversion.montecarlo;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class ModelGenerator {

	private final static PolynomialStructure INITIAL_STRUCTURE;

	static {
		PolynomialStructure ps = PolynomialStructure.PREM;
		ps = ps.addBoundaries(3530, 3580, 3630, 3680, 3730, 3780, 3830, 3880);
		ps = setVs(ps, 2, 7.15);
		ps = setVs(ps, 3, 7.15);
		ps = setVs(ps, 4, 7.15);
		ps = setVs(ps, 5, 7.15);
		ps = setVs(ps, 6, 7.15);
		ps = setVs(ps, 7, 7.15);
		ps = setVs(ps, 8, 7.15);
		ps = setVs(ps, 9, 7.15);
		INITIAL_STRUCTURE = ps;
	}

	private static PolynomialStructure setVs(PolynomialStructure source, int iZone, double vs) {
		PolynomialStructure structure = source.setVsh(iZone, new PolynomialFunction(new double[] { vs, 0, 0, 0 }));
		structure = structure.setVsv(iZone, new PolynomialFunction(new double[] { vs, 0, 0, 0 }));
		return structure;
	}

	/*
	 * +-4% <br>
	 * 0:3480-3530 V<br>
	 * 1:3530-3580 V<br>
	 * .<br>
	 * 7:3830-3880 V<br>
	 * 
	 * +- 10%<br>
	 * 8:3480-3530 Q<br>
	 * 9:3530-3580 Q<br>
	 * .<br>
	 * 15:3830-3880 Q<br>
	 * 
	 */
	static PolynomialStructure nextStructure(PolynomialStructure former) {
		double[] percentage = extractPercentage(former);
		double[] changed = change(percentage);
		return createStructure(changed);
	}

	private static PolynomialStructure createStructure(double[] percentage) {
		PolynomialStructure structure = ModelGenerator.INITIAL_STRUCTURE;
		for (int i = 0; i < 8; i++)
			structure = setVs(structure, i + 2, 7.15 * (1 + percentage[i] / 100));
		for (int i = 8; i < 16; i++)
			structure = structure.setQMu(i - 6, 312 * (1 + percentage[i] / 100));
		return structure;
	}

	private static double[] change(double[] percentage) {
		double[] changed = new double[16];
		for (int i = 0; i < 8; i++) {
			double v = percentage[i] + rand.nextGaussian() * 4;
			if (v < -4)
				v = -8 - v;
			else if (4 < v)
				v = 8 - v;
			changed[i] = v;
		}
		for (int i = 8; i < 16; i++) {
			double q = percentage[i] + rand.nextGaussian() * 5;
			if (q < -10)
				q = -20 - q;
			else if (10 < q)
				q = 20 - q;
			changed[i] = q;
		}

		return changed;
	}

	private static double[] extractPercentage(PolynomialStructure structure) {
		double[] percentage = new double[16];
		PolynomialFunction[] vs = structure.getVsh();
		double[] q = structure.getQmu();
		for (int i = 0; i < 8; i++)
			percentage[i] = vs[i + 2].value(0) / 7.15 * 100 - 100;

		for (int i = 8; i < 16; i++)
			percentage[i] = q[i - 6] / 312 * 100 - 100;

		return percentage;
	}

	private final static Random rand = new Random();

	public static void main(String[] args) throws IOException {
		Path p = Paths.get("/home/kensuke/data/WesternPacific/anelasticity/NobuakiInversion/raw");
		double[] percentage = new double[16];
		double value = 1;
		for (int i = 0; i < 8; i++) {
			percentage[i++] = value;
			percentage[i] = value;
			value *= -1;
		}
		value = -50;
		for (int i = 8; i < 16; i++) {
			percentage[i++] = value;
			percentage[i] = value;
			value *= -1;
		}
		PolynomialStructure ps1 = createStructure(percentage);
		ps1.writePSV(p.resolve("opposite50.model"));
	}

}
