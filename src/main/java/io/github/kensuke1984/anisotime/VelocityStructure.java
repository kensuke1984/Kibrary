package io.github.kensuke1984.anisotime;

import java.io.Serializable;

/**
 * Structure information for computing travel time.
 *
 * @author Kensuke Konishi
 * @version 0.0.9
 * @see <a href=
 * http://www.sciencedirect.com/science/article/pii/0031920181900479>Woodhouse,
 * 1981</a>
 */
public interface VelocityStructure extends Serializable {

    /**
     * @return Transversely isotropic (TI) PREM by Dziewonski &amp; Anderson 1981
     */
    static VelocityStructure prem() {
        return PolynomialStructure.PREM;
    }

    /**
     * @return isotropic PREM by Dziewonski &amp; Anderson 1981
     */
    static VelocityStructure iprem() {
        return PolynomialStructure.ISO_PREM;
    }

    /**
     * @return AK135 by Kennett, Engdahl &amp; Buland (1995)
     */
    static VelocityStructure ak135() {
        return PolynomialStructure.AK135;
    }

    default double getTurningR(PhasePart pp, double rayParameter) {
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
     * @param r [km] radius
     * @return [g/cm**3] density &rho;(r)
     */
    double getRho(double r);

    /**
     * The turning radius is r with which q<sub>&tau;</sub> = 0.
     * <p>
     * r = p(N/&rho;)<sup>1/2</sup>* r = p(N/&rho;)<sup>1/2</sup>
     *
     * @param rayParameter ray parameter
     * @return the K turning radius [km] for the raypath or {@link Double#NaN}
     * if there is no valid R. The radius must be in the outercore.
     * @see "Woodhouse (1981)"
     */
    double kTurningR(double rayParameter);

    /**
     * The turning radius is r with which q<sub>&tau;</sub> = 0.
     * <p>
     * r = p(N/&rho;)<sup>1/2</sup>
     *
     * @param rayParameter ray parameter
     * @return the SH turning radius [km] for the raypath or {@link Double#NaN}
     * if there is no valid R The radius must be in the mantle.
     * @see "Woodhouse (1981)"
     */
    double shTurningR(double rayParameter);

    /**
     * The turning radius is r with which q<sub>&tau;</sub> = 0.
     * <p>
     * r = p(L/&rho;)<sup>1/2</sup>
     *
     * @param rayParameter ray parameter
     * @return the SV turning radius [km] for the raypath or {@link Double#NaN}
     * if there is no valid R. The radius must be in the mantle.
     * @see "Woodhouse (1981)"
     */
    double svTurningR(double rayParameter);

    /**
     * The turning radius is r with which q<sub>&tau;</sub> = 0.
     * <p>
     * r = p(A/&rho;)<sup>1/2</sup>
     *
     * @param rayParameter ray parameter
     * @return the P turning radius [km] for the raypath or {@link Double#NaN}
     * if there is no valid radius. The radius must be in the mantle.
     * @see "Woodhouse (1981)"
     */
    double pTurningR(double rayParameter);

    /**
     * The turning radius is r with which q<sub>&tau;</sub> = 0.
     * <p>
     * r = p(A/&rho;)<sup>1/2</sup>
     *
     * @param rayParameter ray parameter
     * @return the P turning radius [km] for the raypath or {@link Double#NaN}
     * if there is no valid radius. The radius must be in the
     * inner-core.
     * @see "Woodhouse (1981)"
     */
    double iTurningR(double rayParameter);

    /**
     * The turning radius is r with which q<sub>&tau;</sub> = 0.
     * <p>
     * r = p(N/&rho;)<sup>1/2</sup>
     *
     * @param rayParameter ray parameter
     * @return the SH turning radius [km] for the raypath or {@link Double#NaN}
     * if there is no valid R The radius must be in the inner-core.
     * @see "Woodhouse (1981)"
     */
    double jhTurningR(double rayParameter);

    /**
     * The turning radius is r with which q<sub>&tau;</sub> = 0.
     * <p>
     * r = p(L/&rho;)<sup>1/2</sup>
     *
     * @param rayParameter ray parameter
     * @return the SV turning radius [km] for the raypath or {@link Double#NaN}
     * if there is no valid R. The radius must be in the inner-core.
     * @see "Woodhouse (1981)"
     */
    double jvTurningR(double rayParameter);

    /**
     * @param r [km] radius
     * @return [GPa] A(r) A at r
     */
    double getA(double r);

    /**
     * @param r [km] radius
     * @return [GPa] C(r) C at r
     */
    double getC(double r);

    /**
     * @param r [km] radius
     * @return F [GPa] at r
     */
    double getF(double r);

    /**
     * @param r [km] radius
     * @return L [GPa] at r
     */
    double getL(double r);

    /**
     * @param r [km] radius
     * @return N [GPa] at r
     */
    double getN(double r);

    /**
     * @return [km] radius of CMB
     */
    double coreMantleBoundary();

    /**
     * @return [km] radius of ICB
     */
    double innerCoreBoundary();

    /**
     * @return [km] radius of Earth
     */
    double earthRadius();

    /**
     * @return Array of radii [km] for additional boundaries.
     */
    default double[] additionalBoundaries() {
        return new double[]{earthRadius() - 660, earthRadius() - 410};
    }

    /**
     * Input point must be one of the boundaries in the structure.
     *
     * @param point for the radius
     * @return radius [km] for the point.
     */
    default double getROf(PassPoint point) {
        switch (point) {
            case EARTH_SURFACE:
                return earthRadius();
            case CMB:
                return coreMantleBoundary();
            case ICB:
                return innerCoreBoundary();
            default:
                throw new RuntimeException(point + " is not a boundary.");
        }
    }


}
