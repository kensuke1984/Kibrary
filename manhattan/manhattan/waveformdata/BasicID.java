package manhattan.waveformdata;

import filehandling.sac.SACComponent;
import filehandling.sac.WaveformType;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.Station;

/**
 * <p>
 * ID for observed and synthetic waveform
 * </p>
 * <b>This class is IMMUTABLE</b> <br>
 * 
 * Double values will be rounded off to 3rd decimal places. <br>
 * (Those are stored as Float in the file)<br>
 * 
 * type: String(s), Float(f), Integer(i), Long(l), Boolean(b)<br>
 * =Contents of information for one ID= <br>
 * If one is observed(true) or synthetic(false) 1Byte<br>
 * Name of station (must be 8 or less letters) 8Byte(s)<br>
 * Name of network (must be 8 or less letters) 8Byte(s)<br>
 * Horizontal position of station (latitude longitude) 4Byte*2(f*2)<br>
 * Global CMT ID 15Byte(s)<br>
 * Component (ZRT) 1Byte<br>
 * Period minimum and maximum 4Byte*2(f*2) <br>
 * Start time 4Byte(f)<br>
 * Number of points 4Byte(i)<br>
 * Sampling Hz 4Byte(f)<br>
 * If one is convoluted or observed, true 1Byte(b)<br>
 * Position of a waveform for the ID 8Byte(l)<br>
 * <p>
 * One ID volume {@link #oneIDbyte}: {@value #oneIDbyte}
 * </p>
 * 
 * @since 2013/11/13
 * 
 * @version 0.2
 * @author kensuke
 * 
 */
public class BasicID {

	/**
	 * File size for an ID
	 */
	public static final int oneIDbyte = 70;

	/**
	 * waveformData
	 */
	private final double[] data;

	/**
	 * @return Arrays of waveform data
	 */
	public double[] getData() {
		return data.clone();
	}

	@Override
	public String toString() {
		return station + " " + station.getNetwork() + " " + station.getPosition() + " " + globalCMTID + " "
				+ sacComponent + " " + waveFormType + " " + +startTime + " " + npts + " " + samplingHz + " " + minPeriod
				+ " " + maxPeriod + " " + startByte + " " + isConvolved;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((globalCMTID == null) ? 0 : globalCMTID.hashCode());
		result = prime * result + (isConvolved ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(maxPeriod);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minPeriod);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + npts;
		result = prime * result + ((sacComponent == null) ? 0 : sacComponent.hashCode());
		result = prime * result + ((waveFormType == null) ? 0 : waveFormType.hashCode());
		temp = Double.doubleToLongBits(samplingHz);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(startTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((station == null) ? 0 : station.hashCode());
		return result;
	}

	/**
	 * (non-Javadoc)startPoint は無視する
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 * 
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BasicID other = (BasicID) obj;
		if (globalCMTID == null) {
			if (other.globalCMTID != null)
				return false;
		} else if (!globalCMTID.equals(other.globalCMTID))
			return false;
		if (isConvolved != other.isConvolved)
			return false;
		if (Double.doubleToLongBits(maxPeriod) != Double.doubleToLongBits(other.maxPeriod))
			return false;
		if (Double.doubleToLongBits(minPeriod) != Double.doubleToLongBits(other.minPeriod))
			return false;
		if (npts != other.npts)
			return false;
		if (sacComponent != other.sacComponent)
			return false;
		if (waveFormType != other.waveFormType)
			return false;
		if (Double.doubleToLongBits(samplingHz) != Double.doubleToLongBits(other.samplingHz))
			return false;
		if (Double.doubleToLongBits(startTime) != Double.doubleToLongBits(other.startTime))
			return false;
		if (station == null) {
			if (other.station != null)
				return false;
		} else if (!station.equals(other.station))
			return false;
		return true;
	}

	/**
	 * 波形の種類(obs, syn, partial)
	 */
	protected final WaveformType waveFormType;

	/**
	 * 波形のサンプリング間隔 (Hz)
	 */
	protected final double samplingHz;

	/**
	 * 波形のスタート時刻 (s)
	 */
	protected final double startTime;

	/**
	 * データポイント数
	 */
	protected final int npts;

	protected final Station station;

	/**
	 * global cmt id
	 */
	protected final GlobalCMTID globalCMTID;

	/**
	 * 波形の成分
	 */
	protected final SACComponent sacComponent;

	/**
	 * 最短周期 ないときは ０
	 */
	protected final double minPeriod;

	/**
	 * 最長周期 ないときはinfinity
	 */
	protected final double maxPeriod;

	/**
	 * 波形データが何バイト目から始まるか
	 */
	protected final long startByte;

	/**
	 * コンボリューションされているか （観測波形の場合はtrue）
	 */
	protected final boolean isConvolved;

	public WaveformType getWaveformType() {
		return waveFormType;
	}

	public double getSamplingHz() {
		return samplingHz;
	}

	public double getStartTime() {
		return startTime;
	}

	public int getNpts() {
		return npts;
	}

	public Station getStation() {
		return station;
	}

	public GlobalCMTID getGlobalCMTID() {
		return globalCMTID;
	}

	public SACComponent getSacComponent() {
		return sacComponent;
	}

	public double getMinPeriod() {
		return minPeriod;
	}

	public double getMaxPeriod() {
		return maxPeriod;
	}

	public long getStartByte() {
		return startByte;
	}

	public boolean isConvolved() {
		return isConvolved;
	}

	public BasicID(WaveformType waveFormType, double samplingHz, double startTime, int npts, Station station,
			GlobalCMTID globalCMTID, SACComponent sacComponent, double minPeriod, double maxPeriod, long startByte,
			boolean isConvolved, double... waveformData) {
		this.waveFormType = waveFormType;
		this.samplingHz = Math.round(samplingHz * 1000) / 1000.0;
		this.startTime = Math.round(startTime * 1000) / 1000.0;
		this.npts = npts;
		this.station = station;
		this.globalCMTID = globalCMTID;
		this.sacComponent = sacComponent;
		this.minPeriod = Math.round(minPeriod * 1000) / 1000.0;
		this.maxPeriod = Math.round(maxPeriod * 1000) / 1000.0;
		this.startByte = startByte;
		this.isConvolved = isConvolved;
		if (waveformData.length != 0 && waveformData.length != npts)
			throw new IllegalArgumentException("Input waveform data length is invalid");
		data = waveformData.clone();
	}

	/**
	 * A new BasicID with the input data will be returned.
	 * 
	 * @param data
	 *            waveform to be replaced
	 * @return BasicID with the input data
	 */
	public BasicID setData(double[] data) {
		return new BasicID(waveFormType, samplingHz, startTime, npts, station, globalCMTID, sacComponent, minPeriod,
				maxPeriod, startByte, isConvolved, data);
	}

}
