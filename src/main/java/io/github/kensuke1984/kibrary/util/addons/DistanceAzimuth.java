package io.github.kensuke1984.kibrary.util.addons;

public class DistanceAzimuth {
	public double distance;
	public double azimuth;
	
	public DistanceAzimuth(double distance, double azimuth) {
		this.distance = distance;
		this.azimuth = azimuth;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DistanceAzimuth))
			return false;
        if (obj == this)
            return true;

        DistanceAzimuth rhs = (DistanceAzimuth) obj;
        if (rhs.distance != distance)
        	return false;
        else if (rhs.azimuth != azimuth)
        	return false;
        return true;
	}
	
	@Override
	public int hashCode() {
		return (int) (distance * 1000) * 31 + (int) (azimuth * 1000) * 17;
	}
	
}
