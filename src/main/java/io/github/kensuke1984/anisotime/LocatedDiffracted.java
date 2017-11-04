package io.github.kensuke1984.anisotime;

/**
 * @author Kensuke Konishi
 * @version 0.0.1b
 */
interface LocatedDiffracted extends Located, Diffracted {

    @Override
    default boolean isDiffraction() {
        return true;
    }

    /**
     * @param inMantle true if the diffraction occur at the mantle side
     * @param phase    phase of diffraction
     * @param angle    [rad] travel angle
     * @return diffraction of the angle at CMB
     */
    static LocatedDiffracted createCMBDiffraction(boolean inMantle, PhasePart phase, double angle) {
        return new LocatedDiffracted() {

            @Override
            public PassPoint getPassPoint() {
                return PassPoint.CMB;
            }

            @Override
            public boolean isPropagation() {
                return true;
            }

            @Override
            public String toString() {
                return Math.toDegrees(angle) + " deg DIFFRACTION AT CMB";
            }

            @Override
            public double getAngle() {
                return angle;
            }

            @Override
            public PhasePart getPhase() {
                return phase;
            }

            @Override
            public boolean isShallower() {
                return false;
            }
        };
    }
}
