package anisotime;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.complex.Complex;

import manhattan.dsminformation.TransverselyIsotropicParameter;
import mathtool.LinearEquation;

/**
 * Polynomial structure.
 * 
 * 
 * @version 0.0.5
 * 
 * @author kensuke
 *
 */
public class PolynomialStructure implements VelocityStructure {

	final manhattan.dsminformation.PolynomialStructure STRUCTURE;

	public PolynomialStructure(manhattan.dsminformation.PolynomialStructure structure) {
		STRUCTURE = structure;
	}

	/**
	 * transversely isotropic (TI) PREM by Dziewonski &amp; Anderson 1981
	 */
	public static final PolynomialStructure PREM = prem();
	/**
	 * isotropic (TI) PREM by Dziewonski &amp; Anderson 1981
	 */
	public static final PolynomialStructure ISO_PREM = iprem();

	/**
	 * AK135 by Kennett <i>et al</i>. (1995)
	 */
	public static final PolynomialStructure AK135 = ak135();

	private static PolynomialStructure iprem() {
		return new PolynomialStructure(manhattan.dsminformation.PolynomialStructure.ISO_PREM);
	}

	private static PolynomialStructure prem() {
		return new PolynomialStructure(manhattan.dsminformation.PolynomialStructure.PREM);
	}

	private static PolynomialStructure ak135() {
		return new PolynomialStructure(manhattan.dsminformation.PolynomialStructure.AK135);
	}

	public PolynomialStructure(Path path) throws IOException {
		STRUCTURE = new manhattan.dsminformation.PolynomialStructure(path);
	}

	/**
	 * @param i
	 * @param eq
	 * @return turningR in the i-th zone or -1 if no valid R in the i-th zone
	 */
	private double findTurningR(int i, LinearEquation eq) {
		double[] rmax = STRUCTURE.getRmax();
		double[] rmin = STRUCTURE.getRmin();
		int anstype = eq.Discriminant();
		Complex[] answer = eq.compute();
		// System.out.println("ans= "+anstype);//debug
		if (anstype == 1) {
			double radius = answer[0].getReal() * earthRadius();
			return rmin[i] <= radius && radius < rmax[i] ? radius : -1;
		}

		if (anstype < 19)
			return -1;

		if (anstype == 20 || anstype == 28 || anstype == 29 || anstype == 30) {
			double radius = answer[0].getReal() * earthRadius();
			// System.out.print(shTurningDepth);//debug
			return rmin[i] <= radius && radius < rmax[i] ? radius : -1;
		}
		double[] x = new double[eq.compute().length];
		for (int j = 0; j < answer.length; j++)
			x[j] = answer[j].getReal() * earthRadius();
		java.util.Arrays.sort(x);
		for (int j = 0; j < eq.compute().length; j++)
			if (x[j] < rmax[i] && rmin[i] <= x[j])
				return x[j];

		return -1;
		// System.out.println(x[j]);

	}

	@Override
	public double earthRadius() {
		double[] rmax = STRUCTURE.getRmax();
		return rmax[rmax.length - 1];
	}

	/*
	 * (non-Javadoc) The radius which gives the ray parameter p(r*sin(i)/v)=r/v
	 * 
	 * @see traveltime.manhattan.VelocityStructure#shTurningR(double)
	 */
	@Override
	public double shTurningR(double p) {
		PolynomialFunction[] vsh = STRUCTURE.getVsh();
		for (int i = STRUCTURE.getNzone() - 1; i > -1; i--) {
			// System.out.println(rmin[i]+"---"+rmax[i]);//debug
			double[] coef = new double[4];
			for (int j = 0; j < vsh[i].getCoefficients().length; j++)
				coef[j] = vsh[i].getCoefficients()[j];
			LinearEquation eq = new LinearEquation(p * coef[0], p * coef[1] - earthRadius(), p * coef[2], p * coef[3]);
			// System.out.println(eq+" "+p*vsh[i][0]);
			double r = findTurningR(i, eq);
			if (r != -1)
				return r;
		}
		return -1;
	}

	@Override
	public double innerCoreBoundary() {
		// TODO Auto-generated method stub
		return 1221.5;
	}

	@Override
	public double pTurningR(double p) {
		PolynomialFunction[] vph = STRUCTURE.getVph();
		double[] coef = new double[4];
		for (int i = STRUCTURE.getNzone() - 1; i > -1; i--) {
			for (int j = 0; j < vph[i].getCoefficients().length; j++)
				coef[j] = vph[i].getCoefficients()[j];

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
		// TODO
		return 3480;
	}
}
