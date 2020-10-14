package io.github.kensuke1984.kibrary.inversion;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

/**
 * Elastic parameter
 * 弾性定数の偏微分情報 ３次元的な摂動点の位置、 摂動の種類(PartialType) Unknown parameter for 3D. location
 * type
 * <p>
 * This class is <b>IMMUTABLE</b>
 *
 * @author Kensuke Konishi
 * @version 0.0.3.1
 */
public class Physical3DParameter implements UnknownParameter {

    private final PartialType partialType;
    private final double weighting;
    /**
     * location of the perturbation
     */
    private final Location pointLocation;
    public final static int oneUnknownByte = 42;
    
	public static void main(String[] args) {
		UnknownParameter p = new Physical3DParameter(PartialType.MU, new Location(0, 0, Earth.EARTH_RADIUS), 1.);
		byte[] bytes = p.getBytes();
		System.out.println(p);	
		System.out.println(create(bytes));
	}
	
    public Physical3DParameter(PartialType partialType, Location pointLocation, double weighting) {
        this.partialType = partialType;
        this.weighting = weighting;
        this.pointLocation = pointLocation;
    }

    public Location getPointLocation() {
        return pointLocation;
    }

    @Override
    public String toString() {
        return partialType + " " + pointLocation + " " + weighting;
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
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Physical3DParameter other = (Physical3DParameter) obj;
        if (partialType != other.partialType) return false;
        if (pointLocation == null) {
            if (other.pointLocation != null) return false;
        } else if (!pointLocation.equals(other.pointLocation)) return false;
        return Double.doubleToLongBits(weighting) == Double.doubleToLongBits(other.weighting);
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
	
	public byte[] getBytes() {
		byte[] part1 = StringUtils.rightPad(partialType.name(), 10).getBytes();
		byte[] loc1 = new byte[8];
		byte[] loc2 = new byte[8];
		byte[] loc3 = new byte[8];
		ByteBuffer.wrap(loc1).putDouble(pointLocation.getLatitude());
		ByteBuffer.wrap(loc2).putDouble(pointLocation.getLongitude());
		ByteBuffer.wrap(loc3).putDouble(pointLocation.getR());
		byte[] weightByte = new byte[8];
		ByteBuffer.wrap(weightByte).putDouble(weighting);
		byte[] bytes = new byte[oneUnknownByte];
		
		for (int i = 0; i < 10; i++)
			bytes[i] = part1[i];
		for (int i = 0; i < 8; i++)
			bytes[i + 10] = loc1[i];
		for (int i = 0; i < 8; i++)
			bytes[i + 18] = loc2[i];
		for (int i = 0; i < 8; i++)
			bytes[i + 26] = loc3[i];
		for (int i = 0; i < 8; i++)
			bytes[i + 34] = weightByte[i];
		
		return bytes;
	}
	
	public static UnknownParameter create(byte[] bytes) {
		byte[] part1 = Arrays.copyOfRange(bytes, 0, 10);
		byte[] loc1 = Arrays.copyOfRange(bytes, 10, 18);
		byte[] loc2 = Arrays.copyOfRange(bytes, 18, 26);
		byte[] loc3 = Arrays.copyOfRange(bytes, 26, 34);
		byte[] weightByte = Arrays.copyOfRange(bytes, 34, 42);
		
		PartialType partialType = PartialType.valueOf(new String(part1).trim());
		double latitude = ByteBuffer.wrap(loc1).getDouble();
		double longitude = ByteBuffer.wrap(loc2).getDouble();
		double r = ByteBuffer.wrap(loc3).getDouble();
		double weight = ByteBuffer.wrap(weightByte).getDouble();
		
		return new Physical3DParameter(partialType, new Location(latitude, longitude, r), weight);
	}

}
