package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.util.spc.PartialType;

/**
 * 
 * Elastic parameter (one dimension)
 * １次元として使う 摂動点の位置（半径位置）、 摂動の種類(PartialType) Unknown parameter for 1D weighting
 * should be a thickness of a layer
 * <p>
 * This class is <b>IMMUTABLE</b>
 * 
 * 
 * @version 0.0.3
 * 
 * @author Kensuke Konishi
 * 
 */
public class Physical1DParameter implements UnknownParameter {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((partialType == null) ? 0 : partialType.hashCode());
		long temp;
		temp = Double.doubleToLongBits(perturbationR);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		Physical1DParameter other = (Physical1DParameter) obj;
		if (partialType != other.partialType)
			return false;
		if (Double.doubleToLongBits(perturbationR) != Double.doubleToLongBits(other.perturbationR))
			return false;
		if (Double.doubleToLongBits(weighting) != Double.doubleToLongBits(other.weighting))
			return false;
		return true;
	}

	private final double perturbationR;

	@Override
	public String toString() {
		return partialType + " " + perturbationR + " " + getWeighting();
	}

	public double getPerturbationR() {
		return perturbationR;
	}

	private final double weighting;
	private final PartialType partialType;

	public Physical1DParameter(PartialType partialType, double perturbationR, double weighting) {
		this.partialType = partialType;
		this.weighting = weighting;
		this.perturbationR = perturbationR;
	}

	@Override
	public double getWeighting() {
		return weighting;
	}

	@Override
	public PartialType getPartialType() {
		return partialType;
	}

}
