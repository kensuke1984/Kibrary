package io.github.kensuke1984.anisotime;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.complex.Complex;

import io.github.kensuke1984.kibrary.dsminformation.TransverselyIsotropicParameter;
import io.github.kensuke1984.kibrary.math.LinearEquation;

/**
 * Polynomial structure.
 * 
 * 
 * @version 0.0.5.2
 * 
 * @author Kensuke Konishi
 *
 */
public class PolynomialStructure implements VelocityStructure {

	final io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure STRUCTURE;

	public PolynomialStructure(io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure structure) {
		STRUCTURE = structure;
	}

	/**
	 * Transversely isotropic (TI) PREM by Dziewonski &amp; Anderson 1981
	 */
	public static final PolynomialStructure PREM = new PolynomialStructure(
			io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure.PREM);

	/**
	 * isotropic PREM by Dziewonski &amp; Anderson 1981
	 */
	public static final PolynomialStructure ISO_PREM = new PolynomialStructure(
			io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure.ISO_PREM);

	/**
	 * AK135 by Kennett <i>et al</i>. (1995)
	 */
	public static final PolynomialStructure AK135 = new PolynomialStructure(
			io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure.AK135);

	public PolynomialStructure(Path path) throws IOException {
		STRUCTURE = new io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure(path);
	}

	/**
	 * @param i
	 *            zone number for the search
	 * @param eq
	 *            equation to solve
	 * @return turningR in the i-th zone or -1 if no valid R in the i-th zone
	 */
	private double findTurningR(int i, LinearEquation eq) {
		double rmin = STRUCTURE.getRmin()[i];
		double rmax = STRUCTURE.getRmax()[i];
		int anstype = eq.Discriminant();
		Complex[] answer = eq.compute();
		if (anstype == 1) {
			double radius = answer[0].getReal() * earthRadius();
			return rmin <= radius && radius < rmax ? radius : -1;
		}

		if (anstype < 19)
			return -1;

		if (anstype == 20 || anstype == 28 || anstype == 29 || anstype == 30) {
			double radius = answer[0].getReal() * earthRadius();
			return rmin <= radius && radius < rmax ? radius : -1;
		}
		double[] x = new double[answer.length];
		for (int j = 0; j < answer.length; j++)
			x[j] = answer[j].getReal() * earthRadius();
		java.util.Arrays.sort(x);
		for (int j = 0; j < eq.compute().length; j++)
			if (x[j] < rmax && rmin <= x[j])
				return x[j];

		return -1;
	}

	@Override
	public double earthRadius() {
		return STRUCTURE.getRmax()[STRUCTURE.getNzone() - 1];
	}

	@Override
	public double shTurningR(double p) {
		PolynomialFunction[] vsh = STRUCTURE.getVsh();
		for (int i = STRUCTURE.getNzone() - 1; -1 < i; i--) {
			double[] coef = new double[4];
			for (int j = 0; j < vsh[i].getCoefficients().length; j++)
				coef[j] = vsh[i].getCoefficients()[j];
			LinearEquation eq = new LinearEquation(p * coef[0], p * coef[1] - earthRadius(), p * coef[2], p * coef[3]);
			double r = findTurningR(i, eq);
			if (r != -1)
				return r;
		}
		return -1;
	}

	@Override
	public double innerCoreBoundary() {
		return 1221.5; // TODO
	}

	@Override
	public double pTurningR(double p) {
		PolynomialFunction[] vph = STRUCTURE.getVph();
		double[] coef = new double[4];
		for (int i = STRUCTURE.getNzone() - 1; -1 < i; i--) {
			for (int j = 0; j < vph[i].getCoefficients().length; j++)
				coef[j] = vph[i].getCoefficients()[j];
			if(i!=3) //TODO
				continue;
			LinearEquation eq = new LinearEquation(p * coef[0], p * coef[1] - earthRadius(), p * coef[2], p * coef[3]);
			double r = findTurningR(i, eq);
			if (r != -1)
				return r;
		}
		return -1;
	}

	@Override
	public double svTurningR(double p) {
		PolynomialFunction[] vsv = STRUCTURE.getVsv();
		for (int i = STRUCTURE.getNzone() - 1; i > -1; i--) {
			double[] coef = new double[4];
			for (int j = 0; j < vsv[i].getCoefficients().length; j++)
				coef[j] = vsv[i].getCoefficients()[j];
			LinearEquation eq = new LinearEquation(p * coef[0], p * coef[1] - earthRadius(), p * coef[2], p * coef[3]);
			double r = findTurningR(i, eq);
			if (r != -1)
				return r;
		}
		return -1;
	}

	@Override
	public double getRho(double r) {
		return STRUCTURE.getRho(r);
	}

	@Override
	public double getA(double r) {
		return STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.A, r);
	}

	@Override
	public double getC(double r) {
		return STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.C, r);
	}

	@Override
	public double getF(double r) {
		return STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.F, r);
	}

	@Override
	public double getL(double r) {
		return STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.L, r);
	}

	@Override
	public double getN(double r) {
		return STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.N, r);
	}

	@Override
	public double coreMantleBoundary() {
		return 3480; // TODO
	}
}
