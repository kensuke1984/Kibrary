package io.github.kensuke1984.kibrary.external;

import io.github.kensuke1984.anisotime.Phase;

/**
 * The write from taup_time.
 * <p>
 * This class is <b>immutable</b>
 *
 * @author Kensuke Konishi
 * @version 0.0.2.1.1
 */
public class TauPPhase {

    /**
     * [deg] epicentral DISTANCE
     */
    private final double DISTANCE;
    /**
     * [km] source DEPTH not radius
     */
    private final double DEPTH;
    private final Phase PHASE_NAME;
    /**
     * [s] travel time
     */
    private final double TRAVELTIME;
    /**
     * [s/deg] ray parameter
     */
    private final double RAY_PARAMETER;
    /**
     * [deg] TAKEOFF
     */
    private final double TAKEOFF;
    /**
     * [deg] INCIDENT
     */
    private final double INCIDENT;
    /**
     * purist DISTANCE
     */
    private final double PURIST_DISTANCE;
    /**
     * purist name
     */
    private final String PURIST_NAME;

    TauPPhase(double distance, double depth, Phase phaseName, double travelTime, double rayParameter, double takeoff,
              double incident, double puristDistance, String puristName) {
        DISTANCE = distance;
        DEPTH = depth;
        PHASE_NAME = phaseName;
        TRAVELTIME = travelTime;
        RAY_PARAMETER = rayParameter;
        TAKEOFF = takeoff;
        INCIDENT = incident;
        PURIST_DISTANCE = puristDistance;
        PURIST_NAME = puristName;
    }

    public double getDistance() {
        return DISTANCE;
    }

    public double getDepth() {
        return DEPTH;
    }

    public Phase getPhaseName() {
        return PHASE_NAME;
    }

    public double getTravelTime() {
        return TRAVELTIME;
    }

    public double getRayParameter() {
        return RAY_PARAMETER;
    }

    public double getTakeoff() {
        return TAKEOFF;
    }

    public double getIncident() {
        return INCIDENT;
    }

    public double getPuristDistance() {
        return PURIST_DISTANCE;
    }

    public String getPuristName() {
        return PURIST_NAME;
    }

}
