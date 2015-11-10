package manhattan.template;

import java.util.List;

import anisotime.Phase;
import anisotime.RaypathSearch;
import anisotime.VelocityStructure;
import filehandling.sac.SACHeaderData;
import mathtool.geometry.RThetaPhi;
import mathtool.geometry.XYZ;

/**
 * Raypath between a source at {@link #sourceLocation} and a receiver at
 * {@link #stationPosition} <br>
 * This class is <b>IMMUTABLE</b>
 * 
 * 
 * @since 2015/8/22 This class is immutable
 * 
 * @version 0.0.5
 * @since 2015/9/24 of installed
 * 
 * 
 * @author Kensuke
 *
 */
public class Raypath {

	/**
	 * {@link Location} of a seismic source
	 */
	private final Location sourceLocation;

	/**
	 * {@link HorizontalPosition} of a seismic station
	 */
	private final HorizontalPosition stationPosition;

	/**
	 * @return {@link Location} of the seismic source on the raypath
	 */
	public Location getSource() {
		return sourceLocation;
	}

	/**
	 * @return {@link HorizontalPosition} of the seismic station on the raypath
	 */
	public HorizontalPosition getStation() {
		return stationPosition;
	}

	/**
	 * source-to-receiver(station) Azimuth [rad] 震源から観測点をみた方位角
	 */
	protected final double azimuth;

	protected final double backAzimuth;

	/**
	 * 
	 * @return epicentral distance of this raypath [rad]
	 */
	public double getEpicentralDistance() {
		return epicentralDistance;
	}

	/**
	 * @return azimuth [rad]
	 */
	public double getAzimuth() {
		return azimuth;
	}

	/**
	 * @return back azimuth [rad]
	 */
	public double getBackAzimuth() {
		return backAzimuth;
	}

	/**
	 * epicentral distance [rad]
	 */
	protected final double epicentralDistance;

	/**
	 * Create a raypath for the source and station.
	 * 
	 * @param source
	 *            {@link Location} of a source
	 * @param station
	 *            {@link HorizontalPosition} of a station
	 */
	public Raypath(Location source, HorizontalPosition station) {
		this.sourceLocation = source;
		this.stationPosition = station;
		azimuth = source.getAzimuth(station);
		epicentralDistance = Earth.getEpicentralDistance(source, station);
		backAzimuth = source.getBackAzimuth(station);
	}

	/**
	 * Create a raypath for the input SAC.
	 * 
	 * @param sacHeaderData
	 *            of a raypath to create
	 */
	public Raypath(SACHeaderData sacHeaderData) {
		this(sacHeaderData.getEventLocation(), sacHeaderData.getStation().getPosition());
	}

	/**
	 * 
	 * @param theta
	 *            [rad]
	 * @return {@link HorizontalPosition} on the raypath which has epicentral
	 *         distance of theta from the source. 震源から観測点に向けての震央距離thetaでの座標
	 */
	public HorizontalPosition positionOnRaypathAt(double theta) {
		XYZ xyz = RThetaPhi.toCartesian(Earth.EARTH_RADIUS, theta, 0);
		// xyz = xyz.rotateaboutZ(station.getPhi()-source.getPhi());
		xyz = xyz.rotateaboutZ(Math.PI - azimuth);
		xyz = xyz.rotateaboutY(sourceLocation.getTheta());
		xyz = xyz.rotateaboutZ(sourceLocation.getPhi());

		return xyz.getLocation();
	}

	/**
	 * 
	 * @param position
	 *            {@link HorizontalPosition} of target
	 * @return relative position when the source is shifted to the north pole
	 *         and station is on the Standard meridian
	 *         震源を北極に持って行って観測点をさらに標準時線に持っていった時の座標に対する 相対座標
	 */
	public HorizontalPosition toRaypath(HorizontalPosition position) {
		// System.out.println(tmpLoc.isContainsR());
		// if(!tmpLoc.isContainsR())
		// tmpLoc.setR(Earth.EARTH_RADIUS);
		XYZ xyz = position.toXYZ(Earth.EARTH_RADIUS);
		// System.out.println(azimuth+" "+source.getTheta());
		// System.out.println(xyz);
		// System.exit(0);
		xyz = xyz.rotateaboutZ(sourceLocation.getPhi());
		xyz = xyz.rotateaboutY(-sourceLocation.getTheta());
		xyz = xyz.rotateaboutZ(-Math.PI + azimuth);
		// System.out.println(xyz);

		return xyz.getLocation();
	}

	/**
	 * Compensation is the raypath extension of the input phase to the surface
	 * at the source side.
	 * 
	 * @param phase
	 *            target phase to be extended
	 * @param structure
	 *            in which a raypath travels
	 * @return [rad] the delta of the extednded ray path
	 */
	public double computeCompensatedEpicentralDistance(Phase phase, VelocityStructure structure) {
		List<anisotime.Raypath> rays = toANISOtime(phase, structure);
		if(rays.isEmpty())
			throw new RuntimeException("No raypath");
		if(1<rays.size())
			throw new RuntimeException("multiples");
		return rays.get(0).computeExtendedDelta(phase);
	}

	/**
	 * @param phase
	 *            target phase
	 * @param structure
	 *            to compute raypath
	 * @return Raypath which phase travels this raypath
	 */
	public List<anisotime.Raypath> toANISOtime(Phase phase, VelocityStructure structure) {
		double deltaR = 10;
		double eventR = sourceLocation.getR();
		return RaypathSearch.lookFor(phase, structure, eventR, epicentralDistance, deltaR);
	}

}
