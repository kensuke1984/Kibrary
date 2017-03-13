package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

/**
 * Elastic parameter
 * 弾性定数の偏微分情報 ３次元的な摂動点の位置、 摂動の種類(PartialType) Unknown parameter for 3D. location
 * type
 * 
 * <p>
 * This class is <b>IMMUTABLE</b>
 * 
 * 
 * @version 0.0.3.1
 * 
 * @author Kensuke Konishi
 *
 */
public class Physical3DParameter implements UnknownParameter {

	public Location getPointLocation() {
		return pointLocation;
	}

	@Override
	public String toString() {
		return partialType + " " + pointLocation + " " + weighting;
	}

	private final PartialType partialType;
	private final double weighting;

	public Physical3DParameter(PartialType partialType, Location pointLocation, double weighting) {
		this.partialType = partialType;
		this.weighting = weighting;
		this.pointLocation = pointLocation;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((partialType == null) ? 0 : partialType.hashCode());
		result = prime * result + ((pointLocation == null) ? 0 : pointLocation.hashCode());
		long temp;
		temp = Double.doubleToLongBits(weighting);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Physical3DParameter other = (Physical3DParameter) obj;
		if (partialType != other.partialType)
			return false;
		if (pointLocation == null) {
			if (other.pointLocation != null)
				return false;
		} else if (!pointLocation.equals(other.pointLocation))
			return false;
        return Double.doubleToLongBits(weighting) == Double.doubleToLongBits(other.weighting);
    }

	/**
	 * location of the perturbation
	 */
	private final Location pointLocation;

	@Override
	public double getWeighting() {
		return weighting;
	}

	@Override
	public PartialType getPartialType() {
		return partialType;
	}
	
	@Override
	public Location getLocation() {
		return pointLocation;
	}

}
