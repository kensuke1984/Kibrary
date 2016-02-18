/**
 * 
 */
package io.github.kensuke1984.anisotime;

/**
 * Structure information for computing traveltime.
 * @author Kensuke Konishi
 * @version 0.0.1
 */
public interface VelocityStructure {

	/**
	 * @param r
	 *            radius [km]
	 * @return density rho(r) [g/cm**3]
	 */
	double getRho(double r);

	/**
	 * @param rayParameter ray parameter
	 * @return the SH turning radius [km] for the raypath or -1 if there is no
	 *         valid R
	 */
	double shTurningR(double rayParameter);

	/**
	 * @param rayParameter ray parameter
	 * @return the SV turning radius [km] for the raypath or -1 if there is no
	 *         valid R
	 */
	double svTurningR(double rayParameter);

	/**
	 * @param rayParameter ray parameter
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

	// /**
	// * regions must be in order
	// *
	// * r[][] = {{start, end}, {start,end}}
	// *
	// * @return radius region of zero shear
	// */
	// double[][] zeroShearBoundary();

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

}
