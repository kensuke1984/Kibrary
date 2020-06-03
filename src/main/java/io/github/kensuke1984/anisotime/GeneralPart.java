package io.github.kensuke1984.anisotime;


/**
 * This class describes a part of seismic phases.
 *
 * @author Kensuke Konishi
 * @version 0.0.1.2b
 */
class GeneralPart implements PathPart {

    /**
     * if the part goes downward.
     * if the part is a diffraction phase, DOWNWARD is false.
     */
    private final boolean DOWNWARD;

    /**
     * phase name of the part.
     */
    private final PhasePart PHASE;

    private final PassPoint INNER_POINT;
    private final PassPoint OUTER_POINT;

    /**
     * DEPTH [km] at the deeper point
     */
    private final double INNER_DEPTH;

    /**
     * DEPTH [km] at the shallower point
     */
    private final double OUTER_DEPTH;

    /**
     * Note that inner or outer depth is considered only when the corresponding point is OTHER.
     *
     * @param phase      phase name
     * @param downward   if the part goes downward
     * @param innerPoint inner passing point. If this is a special point such as CMB, input inner depth is ignored.
     * @param outerPoint outer passing point. If this is a special point such as CMB, input outer depth is ignored.
     * @param innerDepth [km] DEPTH at the inner point of the part. distance from the Earth surface
     * @param outerDepth [km] DEPTH at the outer point of the part. distance from the Earth surface
     */
    GeneralPart(PhasePart phase, boolean downward, double innerDepth, double outerDepth, PassPoint innerPoint,
                PassPoint outerPoint) {
        DOWNWARD = downward;
        PHASE = phase;
        INNER_POINT = innerPoint;
        OUTER_POINT = outerPoint;
        INNER_DEPTH = INNER_POINT == PassPoint.OTHER ? innerDepth : Double.NaN;
        OUTER_DEPTH = OUTER_POINT == PassPoint.OTHER ? outerDepth : Double.NaN;
    }

    boolean isDownward() {
        return DOWNWARD;
    }

    public PhasePart getPhase() {
        return PHASE;
    }

    PassPoint getInnerPoint() {
        return INNER_POINT;
    }

    PassPoint getOuterPoint() {
        return OUTER_POINT;
    }

    /**
     * @return DEPTH [km] of the deeper point, distance from the surface
     */
    double getInnerDepth() {
        return INNER_DEPTH;
    }

    /**
     * @return DEPTH [km], distance from the surface
     */
    double getOuterDepth() {
        return OUTER_DEPTH;
    }

    @Override
    public String toString() {
        String inner = "null";
        String outer = "null";
        if (INNER_POINT != null)
            inner = INNER_POINT == PassPoint.OTHER ? String.valueOf(INNER_DEPTH) : INNER_POINT.toString();
        if (OUTER_POINT != null)
            outer = OUTER_POINT == PassPoint.OTHER ? String.valueOf(OUTER_DEPTH) : OUTER_POINT.toString();
        String interval = isDownward() ? outer + " \u2198 " + inner : inner + " \u2197 " + outer;
        return PHASE + (isDownward() ? " down " : " up ") + interval;
    }

    @Override
    public boolean isPropagation() {
        return true;
    }

}
