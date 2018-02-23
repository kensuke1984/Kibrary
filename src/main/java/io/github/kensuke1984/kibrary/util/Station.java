package io.github.kensuke1984.kibrary.util;

import java.nio.ByteBuffer;

import org.apache.commons.math3.util.Precision;

import io.github.kensuke1984.kibrary.util.sac.SACHeaderData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

/**
 * <p>
 * Information of station
 * </p>
 * consisting of <br>
 * Station name, {@link HorizontalPosition}, Station network <br>
 * 
 * <p>
 * This class is <b>IMMUTABLE.</b>
 * </p>
 * 
 * Station name and network name must be 8 or less letters.
 * 
 * If the network name is 'DSM', comparison of networks between instances is not
 * done, station name and horizontal position is considered.
 * 
 * 
 * 
 * @version 0.0.5.2
 * 
 * @author Kensuke Konishi
 * 
 */
public class Station implements Comparable<Station> {

	@Override
	public int compareTo(Station o) {
		int name = stationName.compareTo(o.stationName);
		if (name != 0)
			return name;
		int net = network.compareTo(o.network);
//		int pos = comparePosition(o) == true ? 0 : 1;
		return net != 0 ? net : position.compareTo(o.getPosition());
	}

	/**
	 * @param sacHeaderData
	 *            header data
	 * @return Station of the input sacHeaderData
	 */
	public static Station of(SACHeaderData sacHeaderData) {
		return sacHeaderData.getSACString(SACHeaderEnum.KNETWK) == "-12345"
				? new Station(sacHeaderData.getSACString(SACHeaderEnum.KSTNM).trim(),
						new HorizontalPosition(sacHeaderData.getValue(SACHeaderEnum.STLA),
								sacHeaderData.getValue(SACHeaderEnum.STLO)),
						"DSM")
				: new Station(sacHeaderData.getSACString(SACHeaderEnum.KSTNM).trim(),
						new HorizontalPosition(sacHeaderData.getValue(SACHeaderEnum.STLA),
								sacHeaderData.getValue(SACHeaderEnum.STLO)),
						sacHeaderData.getSACString(SACHeaderEnum.KNETWK).trim());
	}

	/**
	 * network name
	 */
	private final String network;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
//		result = prime * result + ((position == null) ? 0 : position.hashCode());
//		result = prime * result + ((stationName == null) ? 0 : stationName.hashCode());
		result = 314159 * prime * stationName.hashCode() * network.hashCode();
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
		Station other = (Station) obj;
		if (position == null) {
			if (other.position != null)
				return false;
//		} else if (!position.equals(other.position))
//			return false;
		} else if (!equal(position, other.position))
			return false;
		if (stationName == null) {
			if (other.stationName != null)
				return false;
		} else if (!stationName.equals(other.stationName))
			return false;
		if (network == null)
			return other.network == null || other.network.equals("DSM");
		else if (network.equals("DSM"))
			return true;
		else if (other.network != null && !other.network.equals("DSM") && !network.equals(other.network))
			return false;
		return true;
	}
	
	private boolean equal(HorizontalPosition pos1, HorizontalPosition pos2) {
		double eps = 0.02;
		if (!Utilities.equalWithinEpsilon(pos1.getLatitude(), pos2.getLatitude(), eps))
			return false;
		else if (!Utilities.equalWithinEpsilon(pos1.getLongitude(), pos2.getLongitude(), eps))
			return false;
		else
			return true;
	}
	
//	private boolean comparePosition(Station other) {
//		boolean res = true;
//		boolean warning = false;
//		String thisLat = String.valueOf(position.getLatitude());
//		String thisLon = String.valueOf(position.getLongitude());
//		String otherLat = String.valueOf(other.getPosition().getLatitude());
//		String otherLon = String.valueOf(other.getPosition().getLongitude());
//		if (position.getLatitude() != other.getPosition().getLatitude()) {
//			if (!thisLat.split("\\.")[1].substring(0, 1).equals(otherLat.split(".")[1].substring(0, 1)))
//				res = false;
//			else
//				warning = true;
//		}
//		if (res && position.getLongitude() != other.getPosition().getLongitude()) {
//			if (!thisLon.split("\\.")[1].substring(0, 1).equals(otherLon.split(".")[1].substring(0, 1)))
//				res = false;
//			else
//				warning = true;
//		}
//		if (warning)
//			System.err.println("Warning: stations positions equals to the first decimal but second decimal differ. May need to check it " + this.toString());
//		
//		return res;
//	}

	/**
	 * the {@link HorizontalPosition} of the station
	 */
	private final HorizontalPosition position;

	/**
	 * the name of the station
	 */
	private final String stationName;

	/**
	 * @return the name of the station
	 */
	public String getStationName() {
		return stationName;
	}

	@Override
	public String toString() {
		return stationName + "_" + network;
	}

	/**
	 * @return the position of the station
	 */
	public HorizontalPosition getPosition() {
		return position;
	}

	/**
	 * @return the name of the network
	 */
	public String getNetwork() {
		return network;
	}

	/**
	 * @param stationName
	 *            Name of the station (must be 8 or less letters)
	 * @param network
	 *            Name of the network of the station (must be 8 or less letters)
	 * @param position
	 *            Horizontal position of the station
	 */
	public Station(String stationName, HorizontalPosition position, String network) {
		if (8 < stationName.length() || 8 < network.length())
			throw new IllegalArgumentException("Both station and network name must be 8 or less letters.");
		this.stationName = stationName;
		this.network = network;
		this.position = position;
	}
	
	public Station(Station station) {
		this.stationName = station.stationName;
		this.network = station.network;
		this.position = station.position;
	}

	/**
	 * Creates station from the input bytes.
	 * 
	 * The bytes must contain Name(8), network(8), latitude(4), longitude(4)
	 * 
	 * The bytes are written in header parts of BasicIDFile PartialIDFile
	 * TimewindowInformationFile.
	 * 
	 * @param bytes
	 *            for one station
	 * @return Station created from the input bytes
	 */
	public static Station createStation(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		byte[] str = new byte[8];
		bb.get(str);
		String name = new String(str).trim();
		bb.get(str);
		String network = new String(str).trim();
		return new Station(name, new HorizontalPosition(bb.getDouble(), bb.getDouble()), network);
	}

	
	public static Station createStation(String stationLine) {
		String[] ss = stationLine.trim().split("\\s+");
		String stationName = ss[0];
		String network = ss[1];
		double latitude = Double.parseDouble(ss[2]);
		double longitude = Double.parseDouble(ss[3]);
		HorizontalPosition position = new HorizontalPosition(latitude, longitude);
		return new Station(stationName, position, network);
	}
}
