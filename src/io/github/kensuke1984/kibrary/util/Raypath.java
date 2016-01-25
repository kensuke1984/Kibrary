package io.github.kensuke1984.kibrary.util;

import java.util.List;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.anisotime.RaypathSearch;
import io.github.kensuke1984.anisotime.VelocityStructure;
import io.github.kensuke1984.kibrary.math.geometry.RThetaPhi;
import io.github.kensuke1984.kibrary.math.geometry.XYZ;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderData;

/**
 * Raypath between a source at {@link #sourceLocation} and a receiver at
 * {@link #stationPosition} <br>
 * This class is <b>IMMUTABLE</b>
 * 
 * 
 * 
 * @version 0.0.5
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
	 * ある点を震源、観測点に与えた時に、
	 * 震源を北極に持って行って観測点をさらに標準時線に持っていった時のある点の座標
	 * 
	 * @param position
	 *            {@link HorizontalPosition} of target
	 * @return relative position when the source is shifted to the north pole
	 *         and station is on the Standard meridian
	 */
	public HorizontalPosition moveToNorthPole(HorizontalPosition position) {
		XYZ xyz = position.toXYZ(Earth.EARTH_RADIUS);
		xyz = xyz.rotateaboutZ(-sourceLocation.getPhi());
		xyz = xyz.rotateaboutY(-sourceLocation.getTheta());
		xyz = xyz.rotateaboutZ(-Math.PI + azimuth);
		return xyz.getLocation();
	}
	
	/**
	 * 震源を北極に持って行って観測点をさらに標準時線に持っていった時に、ある点を仮定する。
	 * その後震源、観測点を本来の位置に戻した時の、ある点の座標
	 * 
	 * @param position
	 *            {@link HorizontalPosition} of target
	 * @return relative position when the source is shifted from the north pole
	 *         and station is from the Standard meridian
	 */
	public HorizontalPosition moveFromNorthPole(HorizontalPosition position) {
		XYZ xyz = position.toXYZ(Earth.EARTH_RADIUS);
		xyz = xyz.rotateaboutZ(Math.PI - azimuth);
		xyz = xyz.rotateaboutY(sourceLocation.getTheta());
		xyz = xyz.rotateaboutZ(sourceLocation.getPhi());
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
		List<io.github.kensuke1984.anisotime.Raypath> rays = toANISOtime(phase, structure);
		if (rays.isEmpty())
			throw new RuntimeException("No raypath");
		if (1 < rays.size())
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
	public List<io.github.kensuke1984.anisotime.Raypath> toANISOtime(Phase phase, VelocityStructure structure) {
		double deltaR = 10;
		double eventR = sourceLocation.getR();
		return RaypathSearch.lookFor(phase, structure, eventR, epicentralDistance, deltaR);
	}

}
