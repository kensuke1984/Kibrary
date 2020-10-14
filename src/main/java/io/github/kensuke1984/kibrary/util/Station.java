package io.github.kensuke1984.kibrary.util;


import io.github.kensuke1984.kibrary.util.sac.SACHeaderData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

import java.nio.ByteBuffer;

/**
 * <p>
 * Information of station
 * </p>
 * consisting of <br>
 * Station name, {@link HorizontalPosition}, Station NETWORK <br>
 * <p>
 * This class is <b>IMMUTABLE</b>
 * </p>
 * <p>
 * Station name and NETWORK name must be 8 or less letters.
 * <p>
 * If the NETWORK name is 'DSM', comparison of networks between instances is not
 * done, station name and horizontal POSITION is considered.
 *
 * @author Kensuke Konishi
 * @version 0.0.5.3
 * @author anselme changed toString() to NAME_NETWORK (unique IRIS identifier)
 */
public class Station implements Comparable<Station> {

    /**
     * NETWORK name
     */
    private final String NETWORK;
    /**
     * the {@link HorizontalPosition} of the station
     */
    private final HorizontalPosition POSITION;
    /**
     * name of the station
     */
    private final String NAME;
    
    /**
     * @param stationName Name of the station (must be 8 or less letters)
     * @param network     Name of the network of the station (must be 8 or less letters)
     * @param position    Horizontal POSITION of the station
     */
	public Station(String stationName, HorizontalPosition position, String network) {
		if (8 < stationName.length() || 8 < network.length())
			throw new IllegalArgumentException("Both station and network name must be 8 or less letters.");
		NAME = stationName;
		NETWORK = network;
		POSITION = position;
	}
	
	public Station(Station station) {
		NAME = station.NAME;
		NETWORK = station.NETWORK;
		POSITION = station.POSITION;
	}

    /**
     * @param sacHeaderData header data
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
     * Creates station from the input bytes.
     * <p>
     * The bytes must contain Name(8), NETWORK(8), latitude(4), longitude(4)
     * <p>
     * The bytes are written in header parts of BasicIDFile PartialIDFile
     * TimewindowInformationFile.
     *
     * @param bytes for one station
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
	
	@Override
	public int compareTo(Station o) {
		int name = NAME.compareTo(o.NAME);
		if (name != 0)
			return name;
		int net = NETWORK.compareTo(o.NETWORK);
//		int pos = comparePosition(o) == true ? 0 : 1;
		return net != 0 ? net : POSITION.compareTo(o.getPosition());
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
//		result = prime * result + ((position == null) ? 0 : position.hashCode());
//		result = prime * result + ((stationName == null) ? 0 : stationName.hashCode());
		result = 314159 * prime * NAME.hashCode() * NETWORK.hashCode();
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
		if (POSITION == null) {
			if (other.POSITION != null)
				return false;
//		} else if (!position.equals(other.position))
//			return false;
		} else if (!equal(POSITION, other.POSITION))
			return false;
		if (NAME == null) {
			if (other.NAME != null)
				return false;
		} else if (!NAME.equals(other.NAME))
			return false;
		if (NETWORK == null)
			return other.NETWORK == null || other.NETWORK.equals("DSM");
		else if (NETWORK.equals("DSM"))
			return true;
		else if (other.NETWORK != null && !other.NETWORK.equals("DSM") && !NETWORK.equals(other.NETWORK))
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
	
	/**
	 * @return the name of the station
	 */
	public String getName() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + "_" + NETWORK;
	}
	
	public String getStringID() {
		return NAME + "_" + NETWORK;
	}

	/**
	 * @return the position of the station
	 */
	public HorizontalPosition getPosition() {
		return POSITION;
	}

	/**
	 * @return the name of the network
	 */
	public String getNetwork() {
		return NETWORK;
	}
}
