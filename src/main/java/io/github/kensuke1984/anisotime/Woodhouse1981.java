package io.github.kensuke1984.anisotime;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The class is calculator of the formulation in Woodhouse (1981).
 * 
 * @author Kensuke Konishi
 * @version 0.0.2
 * @see <a href=
 *      http://www.sciencedirect.com/science/article/pii/0031920181900479>Woodhouse,
 *      1981</a>
 */
class Woodhouse1981 implements Serializable {

	private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
		stream.defaultReadObject();
		createCache();
	}

	private final VelocityStructure structure;

	/**
	 * @param structure
	 *            for Woodhouse computation
	 */
	public Woodhouse1981(VelocityStructure structure) {
		this.structure = structure;
		createCache();
	}

	/**
	 * @return VelocityStructure
	 */
	public VelocityStructure getStructure() {
		return structure;
	}

	/**
	 * @param rayParameter
	 *            to compute for
	 * @param r
	 *            [km]
	 * @return q<sub>&Delta;</sub> for P
	 * @see <a href=
	 *      http://www.sciencedirect.com/science/article/pii/0031920181900479>Woodhouse,
	 *      1981</a>
	 */
	double computeQDelta(PhasePart pp, double rayParameter, double r) {
		double r2 = r * r;
		switch (pp) {
		case P:
		case I:
			return rayParameter / r2 / computeQTau(pp, rayParameter, r) * (computeS3(r)
					+ (computeS4(r) * rayParameter * rayParameter / r2 + computeS5(r)) / computeR(rayParameter, r));
		case SV:
		case JV:
			return rayParameter / r2 / computeQTau(pp, rayParameter, r) * (computeS3(r)
					- (computeS4(r) * rayParameter * rayParameter / r2 + computeS5(r)) / computeR(rayParameter, r));
		case SH:
		case JH:
			return rayParameter * structure.getN(r) / structure.getL(r) / computeQTau(pp, rayParameter, r) / r2;
		case K:
			double v = Math.sqrt(structure.getA(r) / structure.getRho(r));
			double sin = rayParameter * v / r;
			double cos = Math.sqrt(1 - sin * sin);
			return sin / cos / r;
		default:
			throw new RuntimeException("unexpecTed");
		}
	}

	/**
	 * @param rayParameter
	 *            to compute for
	 * @param r
	 *            [km]
	 * @return Q<sub>T</sub> for P
	 */
	double computeQT(PhasePart pp, double rayParameter, double r) {
		switch (pp) {
		case K:
			double v = Math.sqrt(structure.getA(r) / structure.getRho(r));
			double sin = rayParameter * v / r;
			double cos = Math.sqrt(1 - sin * sin);
			return 1 / v / cos;
		case P:
		case I: {
			double s2 = computeS2(r);
			return (computeS1(r)
					- (computeS5(r) * rayParameter * rayParameter / r / r + s2 * s2) / computeR(rayParameter, r))
					/ computeQTau(pp, rayParameter, r);
		}
		case SH:
		case JH:
			return structure.getRho(r) / structure.getL(r) / computeQTau(pp, rayParameter, r);
		case SV:
		case JV:
			double s2 = computeS2(r);
			return (computeS1(r)
					+ (computeS5(r) * rayParameter * rayParameter / r / r + s2 * s2) / computeR(rayParameter, r))
					/ computeQTau(pp, rayParameter, r);
		default:
			throw new RuntimeException("souteigai");
		}
	}

	/**
	 * q<sub>&tau;</sub>= (s<sub>1</sub>-s<sub>3</sub>p<sup>2</sup>/r
	 * <sup>2</sup>-R)<sup>1/2</sup> for P, (s<sub>1</sub>-s<sub>3</sub>p
	 * <sup>2</sup>/r <sup>2</sup>+R)<sup>1/2</sup> for SV<br>
	 * 
	 * 
	 * (&rho;/L-N/L&middot;P<sup>2</sup>/r<sup>2</sup>)<sup>1/2</sup> for SH.
	 * 
	 * @param pp
	 *            target phase
	 * @param rayParameter
	 *            to compute for
	 * @param r
	 *            [km]
	 * @return q<sub>&tau;</sub> for pp
	 */
	double computeQTau(PhasePart pp, double rayParameter, double r) {
		double r2 = r * r;
		switch (pp) {
		case P:
		case I:
			return Math
					.sqrt(computeS1(r) - computeS3(r) * rayParameter * rayParameter / r2 - computeR(rayParameter, r));
		case SH:
		case JH:
			double L = structure.getL(r);
			return Math.sqrt(structure.getRho(r) / L - structure.getN(r) * rayParameter * rayParameter / L / r2);
		case SV:
		case JV:
			return Math
					.sqrt(computeS1(r) - computeS3(r) * rayParameter * rayParameter / r2 + computeR(rayParameter, r));
		case K:
		default:
			throw new RuntimeException("Unexpected");
		}
	}

	/**
	 * @param rayParameter
	 *            to compute for
	 * @param r
	 *            [km]
	 * @return R
	 */
	private double computeR(double rayParameter, double r) {
		double s2 = computeS2(r);
		double por = rayParameter / r;
		double por2 = por * por;
		return Math.sqrt(computeS4(r) * por2 * por2 + 2 * computeS5(r) * por2 + s2 * s2);
	}

	private transient Map<Double, Double> s1;
	private transient Map<Double, Double> s2;
	private transient Map<Double, Double> s3;
	private transient Map<Double, Double> s4;
	private transient Map<Double, Double> s5;

	private void createCache() {
		s1 = Collections.synchronizedMap(new HashMap<>());
		s2 = Collections.synchronizedMap(new HashMap<>());
		s3 = Collections.synchronizedMap(new HashMap<>());
		s4 = Collections.synchronizedMap(new HashMap<>());
		s5 = Collections.synchronizedMap(new HashMap<>());
	}

	/**
	 * @param r
	 *            [km]
	 * @return S<sub>1</sub>
	 */
	private double computeS1(double r) {
		return s1.computeIfAbsent(r, x -> 0.5 * structure.getRho(x) * (1 / structure.getL(x) + 1 / structure.getC(x)));
	}

	/**
	 * @param r
	 *            [km]
	 * @return S<sub>2</sub>
	 */
	private double computeS2(double r) {
		return s2.computeIfAbsent(r, x -> 0.5 * structure.getRho(x) * (1 / structure.getL(x) - 1 / structure.getC(x)));
	}

	/**
	 * @param r
	 *            [km]
	 * @return S<sub>3</sub>
	 */
	private double computeS3(double r) {
		return s3.computeIfAbsent(r, x -> {
			double c = structure.getC(x);
			double f = structure.getF(x);
			double l = structure.getL(x);
			return 0.5 / l / c * (structure.getA(x) * c - f * f - 2 * l * f);
		});
	}

	/**
	 * @param r
	 *            [km]
	 * @return S<sub>4</sub>
	 */
	private double computeS4(double r) {
		return s4.computeIfAbsent(r, x -> {
			double s3 = computeS3(r);
			return s3 * s3 - structure.getA(r) / structure.getC(r);
		});
	}

	/**
	 * @param r
	 *            [km]
	 * @return S<sub>5</sub>
	 */
	private double computeS5(double r) {
		return s5.computeIfAbsent(r,
				x -> 0.5 * structure.getRho(x) / structure.getC(x) * (1 + structure.getA(x) / structure.getL(x))
						- computeS1(x) * computeS3(x));
	}

}
