package io.github.kensuke1984.kibrary.math.geometry;

import io.github.kensuke1984.kibrary.util.Location;

/**
 * 3次元直交座標 cartesian 右手系
 * 
 * 3-D Cartesian coordinates right-handed system
 * This class is <b>immutable</b>
 * @version 0.0.2.2
 * @author Kensuke Konishi
 * 
 */
public class XYZ extends XY {

	public XYZ(double x, double y, double z) {
		super(x, y);
		this.z = z;
	}

	private final double z;

	@Override
	public String toString() {
		return x + " " + y + " " + z;
	}

	/**
	 * @return distance from the origin
	 */
	@Override
	public double getR() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	public double getZ() {
		return z;
	}

	/**
	 * X軸中心にtheta回転させる
	 * 
	 * @param theta
	 *            &theta;[rad]
	 * @return {@link XYZ} created by rotating this by the theta about the x
	 *         axis
	 */
	public XYZ rotateaboutX(double theta) {
		double y = this.y * Math.cos(theta) - this.z * Math.sin(theta);
		double z = this.z * Math.cos(theta) + this.y * Math.sin(theta);
		return new XYZ(x, y, z);
	}

	/**
	 * Y軸中心にtheta回転させる
	 * 
	 * @param theta
	 *            &theta;[rad]
	 * @return {@link XYZ} created by rotating this by the theta about the y
	 *         axis
	 */
	public XYZ rotateaboutY(double theta) {
		double z = this.z * Math.cos(theta) - this.x * Math.sin(theta);
		double x = this.x * Math.cos(theta) + this.z * Math.sin(theta);
		return new XYZ(x, y, z);
	}

	/**
	 * Z軸中心にtheta回転させる
	 * 
	 * @param theta
	 *            &theta;[rad]
	 * @return {@link XYZ} created by rotating this by the theta about the z
	 *         axis
	 */
	public XYZ rotateaboutZ(double theta) {
		double x = this.x * Math.cos(theta) - this.y * Math.sin(theta);
		double y = this.y * Math.cos(theta) + this.x * Math.sin(theta);
		return new XYZ(x, y, z);
	}

	/**
	 * @param xyz
	 *            target
	 * @return xyzとの距離
	 */
	public double getDistance(XYZ xyz) {
		double dx = x - xyz.x;
		double dy = y - xyz.y;
		double dz = z - xyz.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public static RThetaPhi toSphericalCoordinate(double x, double y, double z) {
		if (x == 0 && y == 0 && z == 0)
			throw new IllegalArgumentException("input coordinate x, y, z locates at the origin point.");
		return new RThetaPhi(RThetaPhi.toRfromCartesian(x, y, z), RThetaPhi.toTHETAfromCartesian(x, y, z),
				RThetaPhi.toPHIfromCartesian(x, y, z));
	}

	public Location toLocation() {
		RThetaPhi rtp = toSphericalCoordinate(x, y, z);
		return new Location(Location.toLatitude(rtp.getTheta()), Math.toDegrees(rtp.getPhi()), rtp.getR());
	}

	public RThetaPhi toSphericalCoordinate() {
		return toSphericalCoordinate(x, y, z);
	}

}
