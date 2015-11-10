/**
 * 
 */
package manhattan.external;

/**
 * @author kensuke
 * @since 2014/05/15
 * @version 0.0.1
 */
public class TauPPhase {


	TauPPhase(double distance, double depth, TauPPhaseName phaseName,
			double travelTime, double rayParameter, double takeoff,
			double incident, double puristDistance, String puristName) {
		super();
		this.distance = distance;
		this.depth = depth;
		this.phaseName = phaseName;
		this.travelTime = travelTime;
		this.rayParameter = rayParameter;
		this.takeoff = takeoff;
		this.incident = incident;
		this.puristDistance = puristDistance;
		this.puristName = puristName;
	}

	/**
	 * epicentral distance (deg)
	 */
	private double distance;
	
	/**
	 * source depth not radius (km)
	 */
	private double depth;
	
	private TauPPhaseName phaseName;
	
	/**
	 * travel time (s) 
	 */
	private double travelTime;
	
	/**
	 * ray parameter (s/deg)
	 */
	private double rayParameter;
	
	/**
	 * takeoff (deg)
	 */
	private double takeoff;
	
	/**
	 * incident (deg)
	 */
	private double incident;
	
	
	/**
	 * purist distance
	 */
	private double puristDistance;
	
	/**
	 * purist name
	 */
	private String puristName;

	public double getDistance() {
		return distance;
	}

	public double getDepth() {
		return depth;
	}

	public TauPPhaseName getPhaseName() {
		return phaseName;
	}

	public double getTravelTime() {
		return travelTime;
	}

	public double getRayParameter() {
		return rayParameter;
	}

	public double getTakeoff() {
		return takeoff;
	}

	public double getIncident() {
		return incident;
	}

	public double getPuristDistance() {
		return puristDistance;
	}

	public String getPuristName() {
		return puristName;
	}
	
	
	
	
}
