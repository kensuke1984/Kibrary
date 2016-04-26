/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.io.Serializable;

/**
 * Structure information for computing traveltime.
 * 
 * @author Kensuke Konishi
 * @version 0.0.3.1
 */
public interface VelocityStructure extends Serializable {

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
	 * @param r
	 *            radius [km]
	 * @return density rho(r) [g/cm**3]
	 */
	double getRho(double r);

	/**
	 * @param rayParameter
	 *            ray parameter
	 * @return radius [km] at which K bounces.
	 */
	double kTurningR(double rayParameter);

	/**
	 * @param rayParameter
	 *            ray parameter
	 * @return the SH turning radius [km] for the raypath or -1 if there is no
	 *         valid R
	 */
	double shTurningR(double rayParameter);

	/**
	 * @param rayParameter
	 *            ray parameter
	 * @return the SV turning radius [km] for the raypath or -1 if there is no
	 *         valid R
	 */
	double svTurningR(double rayParameter);

	/**
	 * @param rayParameter
	 *            ray parameter
	 * @return the P turning radius [km] for the raypath or -1 if there is no
	 *         valid R
	 */
	double pTurningR(double rayParameter);

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
	 * @param r
	 *            radius [km]
	 * @return which part r belong to
	 */
	default Partition whichPartition(double r) {
		if (r < 0)
			return null;
		else if (r < innerCoreBoundary())
			return Partition.INNERCORE;
		else if (r == innerCoreBoundary())
			return Partition.INNER_CORE_BAUNDARY;
		else if (r < coreMantleBoundary())
			return Partition.OUTERCORE;
		else if (r == coreMantleBoundary())
			return Partition.CORE_MANTLE_BOUNDARY;
		else
			return Partition.MANTLE;
	}

}
