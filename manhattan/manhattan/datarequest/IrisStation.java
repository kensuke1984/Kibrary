/**
 * 
 */
package manhattan.datarequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import manhattan.template.HorizontalPosition;
import manhattan.template.Station;

/**
 * Iris Station
 * 
 * 
 * @author kensuke
 * @since 2015/01/21
 * @version 0.0.1
 */
public class IrisStation extends Station {

	private IrisStation(String stationName, HorizontalPosition position,
			String network) {
		super(stationName, position, network);
	}

	public static void main(String[] args) {
		read(downloadHTML("http://ds.iris.edu/mda/S/AUJCS"));
//		read(downloadHTML("http://ds.iris.edu/mda/S/AUCAS"));
//		read(downloadHTML("http://ds.iris.edu/mda/S/AUCAR"));
//		read(downloadHTML("http://ds.iris.edu/mda/SY/AUMUL"));
//		read(downloadHTML("http://ds.iris.edu/mda/SY/AUTKS"));
	}

	private static String[] downloadHTML(String httpURL) {
		String[] html = null;
		try {

			URL url = new URL(httpURL);
			BufferedReader isr = new BufferedReader(new InputStreamReader(
					url.openStream()));
			String line = null;
			List<String> htmlLine = new ArrayList<>();
			while (null != (line = isr.readLine()))
				htmlLine.add(line);
			html = htmlLine.toArray(new String[htmlLine.size()]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return html;
	}

	/**
	 * tag pattren. 1 tagStr, 2 tagName, 3 tagAttribute, 4 tagText
	 */
	private static Pattern tagPattern = Pattern
			.compile("(<([^ >]+)([^>]*)>)([^<]*)");

	private double eleveation;

	private Calendar start;
	private Calendar end;
	private String instrument;
	private String epoch;

	/**
	 * read an html for a station TODO now read until Virtual network
	 * affiliations:
	 * 
	 * @param htmlLine
	 */
	private static IrisStation read(String[] htmlLine) {
		double latitude = 0;
		double longitude = 0;
		String stationName = null;
		String networkName = null;
		double elevation = 0;
		Calendar start = null;
		Calendar end = null;
		String instrument = null;
		Channel[] channels = null;
		Calendar metadataload = null;
		String epoch = null;
		for (String s : htmlLine) {
			if (s.contains("Virtual network affiliations:"))
				break;
			Matcher matcher = tagPattern.matcher(s);
			while (matcher.find())
				try {
					String element = matcher.group(4);
					if (element.length() == 0)
						continue;
					if (element.contains("Channels"))
						element = "Channels";
					// System.out.println(element);
					StationAttribute attribute = StationAttribute
							.valueOf(element.toUpperCase());
					switch (attribute) {
					case LATITUDE:
						matcher.find();
						matcher.find();
						latitude = Double.parseDouble(matcher.group(4).trim());
						// System.out.println("latitude "
						// + matcher.group(4).trim());
						break;
					case LONGITUDE:
						matcher.find();
						matcher.find();
						longitude = Double.parseDouble(matcher.group(4).trim());
						// System.out.println("longitude "
						// + matcher.group(4).trim());
						break;
					case CHANNELS:
						System.out.println(s);
//						getChannels(matcher);
						System.out.println("aaaaaaChannels "
								+ matcher.group(4).trim());
						break;
					case ELEVATION:
						matcher.find();
						matcher.find();
						elevation = Double.parseDouble(matcher.group(4).trim());
						// System.out.println("Elevation "+elevation);
						break;
					case END:
						matcher.find();
						matcher.find();
						end = toCalendar(matcher.group(4).trim());
						// System.out.println("END " + end);
						break;
					case EPOCH:
						matcher.find();
						matcher.find();
						epoch = matcher.group(4).trim();
						// System.out.println("EPOCH " + epoch);
						break;
					case INSTRUMENT:
						matcher.find();
						matcher.find();
						instrument = matcher.group(4).trim();
						// System.out.println("instrument " + instrument);
						break;
					case NETWORK:
						matcher.find();
						matcher.find();
						matcher.find();
						networkName = matcher.group(4).trim();
						// System.out.println("network " + networkName);
						break;
					case START:
						matcher.find();
						matcher.find();
						start = toCalendar(matcher.group(4).trim());
						// System.out.println("START " + start);
						break;
					case STATION:
						matcher.find();
						matcher.find();
						matcher.find();
						stationName = matcher.group(4).trim();
						// System.out.println("station " + stationName);
						break;
					default:
						break;
					}
				} catch (Exception e) {
					// e.printStackTrace();
					// TODO: handle exception
				}

		}
		HorizontalPosition position = new HorizontalPosition(latitude,
				longitude);
		IrisStation is = new IrisStation(stationName, position, networkName);
		is.eleveation = elevation;
		is.end = end;
		is.start = start;
		is.instrument = instrument;
		is.epoch = epoch;
		return is;

	}

	private static Channel[] getChannels(Matcher matcher) {
		Channel[] chs = null;
		for (int i = 0; i < 100; i++) {
			matcher.find();
			System.out.println(matcher.group(4));
		}

		return chs;

	}

	/**
	 * 2599/12/31 (365) 23:59:59 YYYY/MM/DD (JDD) HH:MM:SS
	 * 
	 * @param timeStamp
	 * @return
	 */
	private static Calendar toCalendar(String timeStamp) {
		Calendar calendar = Calendar.getInstance();
		String[] parts = timeStamp.split("\\s+");
		String[] dateParts = parts[0].split("/");
		String[] timeParts = parts[2].split(":");
		calendar.set(Integer.parseInt(dateParts[0]),
				Integer.parseInt(dateParts[1]) - 1,
				Integer.parseInt(dateParts[2]), Integer.parseInt(timeParts[0]),
				Integer.parseInt(timeParts[1]), Integer.parseInt(timeParts[2]));
		return calendar;
	}

}
