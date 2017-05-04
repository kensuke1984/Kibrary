package io.github.kensuke1984.kibrary.external;

import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;


/**
 * .kml file for Google Earth.
 *
 * @author Kensuke Konishi
 *         <p>
 *         Created by kensuke on 2017/04/21.
 * @version 0.0.1a
 */
public class KeyHoleMarkupLanguage {


    public static String[] outPlaceMark(GlobalCMTID id) {
        Location loc = id.getEvent().getCmtLocation();
        String[] lines = new String[17];
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();
        lines[0] = "<Placemark>";
        lines[1] = "<name>" + id + "</name>";
        lines[2] = "<LookAt>";
        lines[3] = "<latitude>" + lat + "</latitude>";
        lines[4] = "<longitude>" + lon + "</longitude>";
        lines[5] = "<altitude>0</altitude>";
        lines[6] = "<heading>-6.681526587892506</heading>";
        lines[7] = "<tilt>0</tilt>";
        lines[8] = "<range>11008672.10992986</range>";
        lines[9] = "<gx:altitudeMode>relativeToSeaFloor</gx:altitudeMode>";
        lines[10] = "</LookAt>";
        lines[11] = "<styleUrl>#m_ylw-pushpin</styleUrl>";
        lines[12] = "<Point>";
        lines[13] = "<gx:drawOrder>1</gx:drawOrder>";
        lines[14] = "<coordinates>" + lon + "," + lat + ",0</coordinates>";
        lines[15] = "</Point>";
        lines[16] = "</Placemark>";

        return lines;
    }

    public static String[] outPlaceMark(Station station) {
        String[] lines = new String[17];
        double lat = station.getPosition().getLatitude();
        double lon = station.getPosition().getLongitude();
        lines[0] = "<Placemark>";
        lines[1] = "<name>" + station + "</name>";
        lines[2] = "<LookAt>";
        lines[3] = "<latitude>" + lat + "</latitude>";
        lines[4] = "<longitude>" + lon + "</longitude>";
        lines[5] = "<altitude>0</altitude>";
        lines[6] = "<heading>-6.681526587892506</heading>";
        lines[7] = "<tilt>0</tilt>";
        lines[8] = "<range>11008672.10992986</range>";
        lines[9] = "<gx:altitudeMode>relativeToSeaFloor</gx:altitudeMode>";
        lines[10] = "</LookAt>";
        lines[11] = "<styleUrl>#m_ylw-pushpin</styleUrl>";
        lines[12] = "<Point>";
        lines[13] = "<gx:drawOrder>1</gx:drawOrder>";
        lines[14] = "<coordinates>" + lon + "," + lat + ",0</coordinates>";
        lines[15] = "</Point>";
        lines[16] = "</Placemark>";

        return lines;
    }


}
