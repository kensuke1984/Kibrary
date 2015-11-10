package manhattan.inversion;

import filehandling.spc.PartialType;
import manhattan.template.Location;

/**
 * 
 * 弾性定数の偏微分情報 ３次元的な摂動点の位置、 摂動の種類(PartialType) Unknown parameter for 3D. location
 * type
 * 
 * 
 * This class is <b>IMMUTABLE</b>
 * 
 * @version 0.0.2
 * @since 2014/11/18
 * 
 * @version 0.0.3
 * @since 2015/8/27
 * 
 * @author kensuke
 *
 */
public class ElasticParameter implements UnknownParameter {

	public Location getPointLocation() {
		return pointLocation;
	}

	@Override
	public String toString() {
		return partialType + " " + pointLocation + " " + getWeighting();
	}

	private final PartialType partialType;
	private final double weighting;

	public ElasticParameter(PartialType partialType, Location pointLocation, double weighting) {
		this.partialType = partialType;
		this.weighting = weighting;
		this.pointLocation = pointLocation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
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
		ElasticParameter other = (ElasticParameter) obj;
		if (partialType != other.partialType)
			return false;
		if (pointLocation == null) {
			if (other.pointLocation != null)
				return false;
		} else if (!pointLocation.equals(other.pointLocation))
			return false;
		if (Double.doubleToLongBits(weighting) != Double.doubleToLongBits(other.weighting))
			return false;
		return true;
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

}
