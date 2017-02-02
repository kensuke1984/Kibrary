package io.github.kensuke1984.kibrary.util.sac;

import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Interface of SAC header data<br>
 * <p>
 * The SAC header is described
 * <A href=https://ds.iris.edu/files/sac-manual/manual/file_format.html>here</a>
 *
 * @author Kensuke Konishi
 * @version 0.0.2
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 */
public interface SACHeaderData {

    /**
     * 論理値を返す。 数値が整数、文字列のときはエラー
     *
     * @param sacHeaderEnum a key to a boolean value
     * @return true or false
     * @throws IllegalArgumentException if the input {@link SACHeaderEnum} is not of a boolean value
     */
    boolean getBoolean(SACHeaderEnum sacHeaderEnum);

    /**
     * @return {@link Location} of the source made from EVLA, EVLO and EVDP.
     * Earth radius is considered as 6371.
     */
    default Location getEventLocation() {
        return new Location(getValue(SACHeaderEnum.EVLA), getValue(SACHeaderEnum.EVLO),
                6371.0 - getValue(SACHeaderEnum.EVDP));
    }

    /**
     * Changes EVDP EVLO EVLA
     *
     * @param eventLocation {@link Location} to be set to EVLA, EVLO and EVDP. Earth
     *                      radius is considered as 6371.
     * @return {@link SACHeaderData} with the location
     */
    default SACHeaderData setEventLocation(Location eventLocation) {
        return setValue(SACHeaderEnum.EVLA, eventLocation.getLatitude())
                .setValue(SACHeaderEnum.EVLO, eventLocation.getLongitude())
                .setValue(SACHeaderEnum.EVDP, 6371 - eventLocation.getR());
    }

    /**
     * @return date and time of CMT.
     */
    default LocalDateTime getEventTime() {
        return LocalDateTime.of(LocalDate.ofYearDay(getInt(SACHeaderEnum.NZYEAR), getInt(SACHeaderEnum.NZJDAY)),
                LocalTime.of(getInt(SACHeaderEnum.NZHOUR), getInt(SACHeaderEnum.NZMIN), getInt(SACHeaderEnum.NZSEC),
                        getInt(SACHeaderEnum.NZMSEC) * 1000 * 1000));
    }

    /**
     * Set(Change) event time and date
     *
     * @param eventDateTime to set in SacHeader
     * @return {@link SACHeaderData} with the time
     */
    default SACHeaderData setEventTime(LocalDateTime eventDateTime) {
        return setInt(SACHeaderEnum.NZYEAR, eventDateTime.getYear())
                .setInt(SACHeaderEnum.NZJDAY, eventDateTime.getDayOfYear())
                .setInt(SACHeaderEnum.NZHOUR, eventDateTime.getHour())
                .setInt(SACHeaderEnum.NZMIN, eventDateTime.getMinute())
                .setInt(SACHeaderEnum.NZSEC, eventDateTime.getSecond())
                .setInt(SACHeaderEnum.NZMSEC, eventDateTime.getNano() / 1000 / 1000);
    }

    /**
     * @return Station of this header.
     */
    default Station getStation() {
        return Station.of(this);
    }

    /**
     * Changes KSTNM, KNETWK, STLA, STLO
     *
     * @param station to be set
     * @return {@link SACHeaderData} with the station
     */
    default SACHeaderData setStation(Station station) {
        SACHeaderData sd = setSACString(SACHeaderEnum.KSTNM, station.getName());
        sd = sd.setSACString(SACHeaderEnum.KNETWK, station.getNetwork());
        return sd.setValue(SACHeaderEnum.STLA, station.getPosition().getLatitude())
                .setValue(SACHeaderEnum.STLO, station.getPosition().getLongitude());
    }

    /**
     * KCMPNM (vertical, radial or trnsvers)
     * vertical:Z, radial:R, trnsvers:T
     *
     * @return component
     */
    default SACComponent getComponent() {
        switch (getSACString(SACHeaderEnum.KCMPNM)) {
            case "vertical":
                return SACComponent.Z;
            case "radial":
                return SACComponent.R;
            case "trnsvers":
                return SACComponent.T;
            default:
                throw new RuntimeException("KCMPNM is invalid. must be vertical, radial or trnsvers");
        }
    }

    /**
     * Returns an integer value
     *
     * @param sacHeaderEnum a key to an integer value
     * @return an integer value to the input {@link SACHeaderEnum}
     * @throws IllegalArgumentException if the input {@link SACHeaderEnum} is not of an integer value
     */
    int getInt(SACHeaderEnum sacHeaderEnum);

    /**
     * @param sacHeaderEnum a key to a Enumerated value
     * @return a enumerated value to the input {@link SACHeaderEnum}
     * @throws IllegalArgumentException if the input {@link SACHeaderEnum} is not of an Emumerated
     *                                  value
     */
    int getSACEnumerated(SACHeaderEnum sacHeaderEnum);

    /**
     * @param sacHeaderEnum a key to a String value
     * @return a String value to the input {@link SACHeaderEnum}
     */
    String getSACString(SACHeaderEnum sacHeaderEnum);

    /**
     * Returns a double value.
     *
     * @param sacHeaderEnum a key to a float value
     * @return a double value of a float value to the input
     * {@link SACHeaderEnum}
     * @throws IllegalArgumentException if the input {@link SACHeaderEnum} is not of a float value
     */
    double getValue(SACHeaderEnum sacHeaderEnum);

    /**
     * Set a boolean value
     *
     * @param sacHeaderEnum a key to a boolean value
     * @param bool          to be set
     * @return {@link SACHeaderData} with the bool
     * @throws IllegalArgumentException if the input {@link SACHeaderEnum} is not of a boolean value
     *                                  of is a special boolean.
     */
    SACHeaderData setBoolean(SACHeaderEnum sacHeaderEnum, boolean bool);

    /**
     * If the value KEVNM is not valid for GlobalCMTID, then it will throw RuntimeException.
     *
     * @return GlobalCMTID by KEVNM
     */
    default GlobalCMTID getGlobalCMTID() {
        return new GlobalCMTID(getSACString(SACHeaderEnum.KEVNM));
    }

    /**
     * 整数値を代入する not enumerized TODO debug
     *
     * @param sacHeaderEnum a key to an integer value
     * @param value         an integer value to be set
     * @return {@link SACHeaderData} with the value
     */
    SACHeaderData setInt(SACHeaderEnum sacHeaderEnum, int value);

    /**
     * Enumeratedフィールドの代入 今は整数値で受け取る
     *
     * @param sacHeaderEnum a key to an Enumerated field
     * @param value         a integer value to input
     * @return {@link SACHeaderData} with the value
     * @throws IllegalArgumentException if the input {@link SACHeaderEnum} is not of an enumarated
     *                                  value
     */
    SACHeaderData setSACEnumerated(SACHeaderEnum sacHeaderEnum, int value);

    /**
     * Set a String value
     *
     * @param sacHeaderEnum a key to a String value
     * @param string        to be set
     * @return {@link SACHeaderData} with the string
     * @throws IllegalArgumentException if the input {@link SACHeaderEnum} is not of a String value,
     *                                  if the input string has a invalid length.
     */
    SACHeaderData setSACString(SACHeaderEnum sacHeaderEnum, String string);

    /**
     * マーカーに時間を設定する ぴっちりdelta * n の時刻に少し修正する round(time/delta)*delta Set a time
     * marker. Input time will be changed as the new one is on the SAC file
     * <p>
     * If a SAC file has values of time = 0.05, 0.10, 0.15 and the input time is
     * 0.09, then a marker will be set on 0.10(closest).
     *
     * @param marker must be Tn n=[0-9], A
     * @param time   to set in this
     * @return {@link SACHeaderData} with a time marker.
     * @throws IllegalArgumentException if marker is not Tn
     */
    default SACHeaderData setTimeMarker(SACHeaderEnum marker, double time) {
        if (marker != SACHeaderEnum.T0 && marker != SACHeaderEnum.T1 && marker != SACHeaderEnum.T2 &&
                marker != SACHeaderEnum.T3 && marker != SACHeaderEnum.T4 && marker != SACHeaderEnum.T5 &&
                marker != SACHeaderEnum.T6 && marker != SACHeaderEnum.T7 && marker != SACHeaderEnum.T8 &&
                marker != SACHeaderEnum.T9 && marker != SACHeaderEnum.A)
            throw new IllegalArgumentException("Only Tn n=[0-9] can be set");
        double b = getValue(SACHeaderEnum.B);
        // if(getValue(SacHeaderEnum.B)!=0)
        // throw new IllegalStateException("Value B is not 0.");
        double delta = getValue(SACHeaderEnum.DELTA);
        double inputTime = Math.round((time - b) / delta) * delta + b;
        // System.out.println(b + " " + inputTime);
        return setValue(marker, inputTime);
    }

    /**
     * Set a double value. Note that a SAC file just holds values as Float not
     * Double
     *
     * @param sacHeaderEnum a key to a float value
     * @param value         a double value to be set
     * @return {@link SACHeaderData} with the value
     */
    SACHeaderData setValue(SACHeaderEnum sacHeaderEnum, double value);

}
