package io.github.kensuke1984.anisotime;

/**
 * @author Kensuke Konishi
 * @version 0.0.1b
 */
interface Diffracted {
    /**
     * @return travel angle [rad]
     */
    double getAngle();

    PhasePart getPhase();

    /**
     * @return If the diffraction occurs isShallower(true) or deeper(false) side at the boundary.
     */
    boolean isShallower();
}
