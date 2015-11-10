package manhattan.firsthandler;

import java.time.LocalDateTime;
import java.util.Calendar;

/**
 * @version 0.0.1
 * @since 2013/9/20 rdseedで解凍したファイルの名前 1993.052.07.01.12.4000.PS.OGS.(location
 *        ID).BHN.D.SAC
 * 
 * @version 0.0.2
 * @since 2014/1/13 use {@link Calendar} for a starting time
 * 
 * @version 0.0.5
 * @since 2015/2/12
 * {@link Calendar} &rarr; {@link LocalDateTime}
 * 
 * @author kensuke
 * 
 */
class SacFileName implements Comparable<SacFileName> {

	/**
	 * PS.OGS.(locationID).BHN.D.SAC の部分を返す
	 * 
	 * @return
	 */
	String getRelationString() {
		return name.substring(23);
	}

	/**
	 * @return (network).station.locationID.BHN.D.SAC
	 */
	String getNetwork() {
		return network;
	}

	/**
	 * @return network.station.locationID.(BHN).D.SAC
	 */
	String getChannel() {
		return channel;
	}

	/**
	 * @return network.station.locationID.BHN.D.SAC
	 */
	String getQualityControl() {
		return qualityControl;
	}

	/**
	 * @return network.station.(locationID).BHN.D.SAC
	 */
	String getLocationID() {
		return locationID;
	}

	/**
	 * @return network.(station).locationID.BHN.D.SAC
	 */
	String getStation() {
		return station;
	}

	LocalDateTime getStartTime() {
		return startTime;
	}

	private String name;

	@Override
	public int compareTo(SacFileName o) {
		int c = network.compareTo(o.network);
		if (c != 0)
			return c;
		else if ((c = station.compareTo(o.station)) != 0)
			return c;
		else if ((c = locationID.compareTo(o.locationID)) != 0)
			return c;
		else if ((c = channel.compareTo(o.channel)) != 0)
			return c;
		else if ((c = qualityControl.compareTo(o.qualityControl)) != 0)
			return c;
		else
			return startTime.compareTo(o.startTime);
		// return 0;
	}

	private LocalDateTime startTime;

	/**
	 * network identifier ネットワーク名
	 */
	private String network;
	/**
	 * components BHE BHZとか
	 */
	private String channel;

	/**
	 * quality control marker D=Data of Undetermined state, M=Merged Data, R=Raw
	 * waveform Data, Q=QC'd data
	 */
	private String qualityControl;
	/**
	 * location IDの部分
	 */
	private String locationID;
	/**
	 * station name
	 */
	private String station;

	SacFileName(String sacFileName) {
		String[] parts = sacFileName.split("\\.");
		// year = parts[0]; hour = parts[2];
		// min = parts[3]; sec = parts[4];
		// sec/10000 = parts[5]; jday = parts[1];
		startTime = LocalDateTime.of(Integer.parseInt(parts[0]), 1, 1,
				Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
				Integer.parseInt(parts[4]),
				Integer.parseInt(parts[1]) * 100 * 1000).withDayOfYear(Integer.parseInt(parts[1]));
		// System.out.println(msec+" "+millisec);
		network = parts[6];
		station = parts[7];
		locationID = parts[8];
		channel = parts[9];
		qualityControl = parts[10];
		name = sacFileName;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * ひとつなぎのファイルの一部か 分割されたものをつなげる際の判断基準
	 * 
	 * @param sacFileName
	 * @return
	 */
	boolean isRelated(SacFileName sacFileName) {
		return sacFileName.channel.equals(channel)
				&& sacFileName.network.equals(network)
				&& sacFileName.station.equals(station)
				&& sacFileName.locationID.equals(locationID)
				&& sacFileName.qualityControl.equals(qualityControl);
	}

}