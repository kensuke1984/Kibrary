/**
 * 
 */
package io.github.kensuke1984.kibrary.waveformdata;

/**
 * @version 0.0.1
 * @since 2018/05/18
 * @author Yuki
 *
 */
public class WaveID {
	private String stationName;
    private String gcmtID;

    public WaveID(String stnm, String evtnm) {
            this.stationName = stnm;
            this.gcmtID = evtnm;
            }

    public String toString() {
            return this.stationName + "," + this.gcmtID;
            }

    @Override
    public boolean equals(Object o) {
    if (o == this) return true;
    if (o.getClass() != this.getClass()) return false;
    WaveID wid = (WaveID)o;
    return this.stationName.equals(wid.stationName) && this.gcmtID.equals(wid.gcmtID);
}
}
