package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

public class TimeSourceSideParameter implements UnknownParameter {
	public Location getPointLocation() {
		return pointLocation;
	}

	@Override
	public String toString() {
		return partialType + " " + id + " " + weighting;
	}

	private final PartialType partialType = PartialType.TIME_SOURCE;
	private final double weighting = 1.;

	public TimeSourceSideParameter(GlobalCMTID id) {
		this.id = id;
		this.pointLocation = id.getEvent().getCmtLocation();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((partialType == null) ? 0 : partialType.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		TimeSourceSideParameter other = (TimeSourceSideParameter) obj;
		if (partialType != other.partialType)
			return false;
		if (pointLocation == null) {
			if (other.pointLocation != null)
				return false;
		} else if (!pointLocation.equals(other.pointLocation))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (Double.doubleToLongBits(weighting) != Double.doubleToLongBits(other.weighting))
			return false;
		return true;
	}

	/**
	 * location of the perturbation
	 */
	private final Location pointLocation;
	
	private final GlobalCMTID id;
	
	public GlobalCMTID getGlobalCMTID() {
		return id;
	}

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
	
	@Override
	public byte[] getBytes() {
		return new byte[0];
		// TODO
	}
}
