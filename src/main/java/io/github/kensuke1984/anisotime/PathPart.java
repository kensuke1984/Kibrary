package io.github.kensuke1984.anisotime;

/**
 * Describes a part of path.
 *
 * @author Kensuke Konishi
 * @version 0.0.1
 */
interface PathPart {
    /**
     * @return if it is emission of propagation.
     */
    default boolean isEmission() {
        return false;
    }

    /**
     * @return if it is a propagation part. diffraction phase is also included.
     */
    default boolean isPropagation() {
        return false;
    }

    /**
     * @return if it is a bounce point, neither penetration nor reflection.
     */
    default boolean isBounce() {
        return false;
    }

    /**
     * @return if it is a diffraction part
     */
    default boolean isDiffraction() {
        return false;
    }

    /**
     * Note that this is only for a transmission without a reflection.
     * e.g. P410S contains the transmission, but not PcS, P^410S...
     *
     * @return if it is a transmission.
     */
    default boolean isTransmission() {
        return false;
    }

    /**
     * @return if it is a topside reflection.
     */
    default boolean isTopsideReflection() {
        return false;
    }

    /**
     * @return if it is a topside reflection.
     */
    default boolean isBottomsideReflection() {
        return false;
    }

    /**
     * Note that this is only for a penetration across the CMB or ICB.
     *
     * @return if it is a penetration.
     */
    default boolean isPenetration() {
        return false;
    }

}
