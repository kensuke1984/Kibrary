/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.io.Serializable;

/**
 * Structure information for computing traveltime.
 * 
 * @author Kensuke Konishi
 * @version 0.0.8.1
 * @see <a href=
 *      http://www.sciencedirect.com/science/article/pii/0031920181900479>Woodhouse,
 *      1981</a>
 */
public interface VelocityStructure extends Serializable {

	public default double getTurningR(PhasePart pp, double rayParameter){
		switch (pp) {
		case I:
			return iTurningR(rayParameter);
		case JV:
			return jvTurningR(rayParameter);
		case JH:
			return jhTurningR(rayParameter);
		case K:
			return kTurningR(rayParameter);
		case P:
			return pTurningR(rayParameter);
		case SV:
			return svTurningR(rayParameter);
		case SH:
			return shTurningR(rayParameter);
		default:
			throw new RuntimeException("anikusupekuted");
		}
	}
	
	
	/**
	 * @return Transversely isotropic (TI) PREM by Dziewonski & Anderson 1981
	 */
	public static VelocityStructure prem() {
		return PolynomialStructure.PREM;
	}

	/**
	 * @return isotropic PREM by Dziewonski & Anderson 1981
	 */
	public static VelocityStructure isoPREM() {
		return PolynomialStructure.ISO_PREM;
	}

	/**
	 * @return AK135 by Kennett, Engdahl & Buland (1995)
	 */
	public static VelocityStructure ak135(){
		return PolynomialStructure.AK135;
	}
	
	
	/**
	 * @param r
	 *            radius [km]
	 * @return density rho(r) [g/cm**3]
	 */
	double getRho(double r);

	/**
	 * The turning radius is r with which q<sub>&tau;</sub> = 0.
	 * <p>
	 * r = p(N/&rho;)<sup>1/2</sup>* r = p(N/&rho;)<sup>1/2</sup>
	 * 
	 * @param rayParameter
	 *            ray parameter
	 * @return the K turning radius [km] for the raypath or {@link Double#NaN}
	 *         if there is no valid R. The radius must be in the outercore.
	 * @see {@code Woodhouse (1981)}
	 */
	double kTurningR(double rayParameter);

	/**
	 * The turning radius is r with which q<sub>&tau;</sub> = 0.
	 * <p>
	 * r = p(N/&rho;)<sup>1/2</sup>
	 * 
	 * @param rayParameter
	 *            ray parameter
	 * @return the SH turning radius [km] for the raypath or {@link Double#NaN}
	 *         if there is no valid R The radius must be in the mantle.
	 * @see {@code Woodhouse (1981)}
	 */
	double shTurningR(double rayParameter);

	/**
	 * The turning radius is r with which q<sub>&tau;</sub> = 0.
	 * <p>
	 * r = p(L/&rho;)<sup>1/2</sup>
	 * 
	 * @param rayParameter
	 *            ray parameter
	 * @return the SV turning radius [km] for the raypath or {@link Double#NaN}
	 *         if there is no valid R. The radius must be in the mantle.
	 * @see {@code Woodhouse (1981)}
	 */
	double svTurningR(double rayParameter);

	/**
	 * The turning radius is r with which q<sub>&tau;</sub> = 0.
	 * <p>
	 * r = p(A/&rho;)<sup>1/2</sup>
	 * 
	 * @param rayParameter
	 *            ray parameter
	 * @return the P turning radius [km] for the raypath or {@link Double#NaN}
	 *         if there is no valid radius. The radius must be in the mantle.
	 * @see {@code Woodhouse (1981)}
	 */
	double pTurningR(double rayParameter);

	/**
	 * The turning radius is r with which q<sub>&tau;</sub> = 0.
	 * <p>
	 * r = p(A/&rho;)<sup>1/2</sup>
	 * 
	 * @param rayParameter
	 *            ray parameter
	 * @return the P turning radius [km] for the raypath or {@link Double#NaN}
	 *         if there is no valid radius. The radius must be in the
	 *         inner-core.
	 * @see {@code Woodhouse (1981)}
	 */
	double iTurningR(double rayParameter);

	/**
	 * The turning radius is r with which q<sub>&tau;</sub> = 0.
	 * <p>
	 * r = p(N/&rho;)<sup>1/2</sup>
	 * 
	 * @param rayParameter
	 *            ray parameter
	 * @return the SH turning radius [km] for the raypath or {@link Double#NaN}
	 *         if there is no valid R The radius must be in the inner-core.
	 * @see {@code Woodhouse (1981)}
	 */
	double jhTurningR(double rayParameter);

	/**
	 * The turning radius is r with which q<sub>&tau;</sub> = 0.
	 * <p>
	 * r = p(L/&rho;)<sup>1/2</sup>
	 * 
	 * @param rayParameter
	 *            ray parameter
	 * @return the SV turning radius [km] for the raypath or {@link Double#NaN}
	 *         if there is no valid R. The radius must be in the inner-core.
	 * @see {@code Woodhouse (1981)}
	 */
	double jvTurningR(double rayParameter);

	/**
	 * @param r
	 *            [km] radius
	 * @return A(r) [GPa] A at r
	 */
	double getA(double r);

	/**
	 * @param r
	 *            [km] radius
	 * @return C(r) [GPa] C at r
	 */
	double getC(double r);

	/**
	 * @param r
	 *            [km] radius
	 * @return F(r) F at r
	 */
	double getF(double r);

	/**
	 * @param r
	 *            [km] radius
	 * @return L(r) [GPa] L at r
	 */
	double getL(double r);

	/**
	 * @param r
	 *            [km] radius
	 * @return N(r) [GPa] N at r
	 */
	double getN(double r);

	/**
	 * @return radius[km] of CMB
	 */
	double coreMantleBoundary();

	/**
	 * @return radius[km] of ICB
	 */
	double innerCoreBoundary();

	/**
	 * @return radius[km] of Earth
	 */
	double earthRadius();

	/**
	 * If the distance between the radius r and a radius of a major boundary is
	 * within {@link ComputationalMesh#eps}, the boundary returns.
	 * 
	 * @param r
	 *            [km] must be inside the earth [0, surface+eps]
	 * @return Partition where r belong to
	 */
	default Partition whichPartition(double r) {
		if (Math.abs(r - earthRadius()) <= ComputationalMesh.eps)
			return Partition.SURFACE;
		if (Math.abs(r - innerCoreBoundary()) <= ComputationalMesh.eps)
			return Partition.INNER_CORE_BOUNDARY;
		if (Math.abs(r - coreMantleBoundary()) <= ComputationalMesh.eps)
			return Partition.CORE_MANTLE_BOUNDARY;
		if (earthRadius() < r || r < 0)
			throw new RuntimeException("Input radius " + r + "is out of the Earth.");

		if (coreMantleBoundary() < r)
			return Partition.MANTLE;
		else if (innerCoreBoundary() < r)
			return Partition.OUTERCORE;
		else
			return Partition.INNERCORE;
	}

	/**
	 * @return Array of radii [km] for additional boundaries.
	 */
	default double[] additionalBoundaries() {
		return new double[] { earthRadius() - 660, earthRadius() - 410 };
	}

	/**
	 * x&equiv;&rho;/L - N/L p<sup>2</sup>/r<sup>2</sup>
	 * 
	 * @param x
	 *            target value (q<sub>&tau;</sub><sup>2</sup>)
	 * @return radius where q<sub>&tau;</sub> = x<sup>2</sup>
	 */
	default double getRofSHfor(double x) {
		if (x < 0)
			throw new IllegalArgumentException("x must be positive.");

		throw new RuntimeException("could not find a radius.");
	}

}
