package manhattan.template;

import java.nio.ByteBuffer;

import filehandling.sac.SACHeaderData;
import filehandling.sac.SACHeaderEnum;

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
 * @version 0.0.5
 * 
 * @author kensuke
 * 
 */
public class Station implements Comparable<Station> {

	@Override
	public int compareTo(Station o) {
		int name = stationName.compareTo(o.stationName);
		if (name != 0)
			return stationName.compareTo(o.stationName);
		int net = network.compareTo(o.network);
		if (net != 0)
			return net;
		return position.compareTo(o.position);
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
		result = prime * result + ((position == null) ? 0 : position.hashCode());
		result = prime * result + ((stationName == null) ? 0 : stationName.hashCode());
		// result = prime * result + ((network == null) ? 0 :
		// network.hashCode());
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
		} else if (!position.equals(other.position))
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
		return stationName;
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
		return new Station(name, new HorizontalPosition(bb.getFloat(), bb.getFloat()), network);
	}

}
