package io.github.kensuke1984.anisotime;


/**
 * Enums of pass point
 *
 * @author Kensuke Konishi
 * @version 0.0.1.1b
 */
enum PassPoint {
    OTHER, //
    BOUNCE_POINT, //
    EARTH_SURFACE, //
    SEISMIC_SOURCE, //
    CMB, // core mantle boundary
    ICB; // inner core outercore boundary

    /**
     * @param point target point
     * @return if the point is the Earth surface, CMB or ICB.
     */
    static boolean isBoundary(PassPoint point) {
        return point == EARTH_SURFACE || point == CMB || point == ICB;
    }
}
