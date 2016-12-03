package io.github.kensuke1984.kibrary.datarequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

/**
 * 
 * 
 * BREAKFASTシステムへのデータリクエストの際のデータ部分
 * 
 * millisecond があいまい 強制的に0にする 各データのchannel は "1 BH?"に固定
 * <p>
 * STA NN YYYY MM DD HH MM SS.TTTT YYYY MM DD HH MM SS.TTTT #_CH CH1 CH2 CHn LI
 * <p>
 * where
 * 
 * #_CH is the number of channel designators in the immediately following list
 * CHn is a channel designator that can contain wildcards LI is location
 * identifier (optional)<br>
 * <p>
 * For OHP request, TTTT is now only one digit.
 * 
 * @version 0.0.5.1
 * 
 * @author kensuke
 * @see <a href=http://www.iris.edu/dms/nodes/dmc/manuals/breq_fast>official
 *      guide</a>
 */
public class Channel {

	/**
	 * station (STA)
	 */
	private String stationName;

	/**
	 * network code or virtual network (NN)
	 */
	private String networkName;

	/**
	 * starting time
	 */
	private LocalDateTime startTime;
	/**
	 * ending time
	 */
	private LocalDateTime endTime;

//	/**
//	 * LocationIdentifier
//	 */
	// private String locationIdentifier;

	/**
	 * 指定するチャンネル数（ワイルドカードは一つとして数える）
	 */
	private int channelNumber = 1;
	private String[] channel = { "BH?" };

	public Channel(String stationName, String networkName, LocalDateTime startTime, LocalDateTime endTime) {
		super();
		this.stationName = stationName;
		this.networkName = networkName;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	private static DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("yyyy MM dd HH mm ss");

	/**
	 * STA NN YYYY MM DD HH MM SS.TTTT YYYY MM DD HH MM SS.TTTT #_CH CH1 CH2 CHn
	 * LI
	 * 
	 * where
	 * 
	 * STA is the station NN is the network code or "virtual network" YYYY is
	 * the year - 2 digit entries will be rejected! MM is the month DD is the
	 * day HH is the hour MM is the minute SS.TTTT is the second and
	 * ten-thousandths of seconds #_CH is the number of channel designators in
	 * the immediately following list CHn is a channel designator that can
	 * contain wildcards LI is location identifier (optional)
	 * 
	 * @param time
	 *            to be formatted
	 * @return formatted line
	 */
	private static String toLine(LocalDateTime time) {
		return time.format(outputFormat) + "." + String.format("%01d", time.getNano() / 100 / 1000);
	}

	@Override
	public String toString() {
		String channels = Arrays.stream(channel).collect(Collectors.joining(" "));
		return stationName + " " + networkName + " " + toLine(startTime) + " " + toLine(endTime) + " " + channelNumber
				+ " " + channels;
	}

	/**
	 * 
	 * @param network
	 *            names of networks
	 * @param id
	 *            GlobalCMTID
	 * @param headUnit
	 *            {@link ChronoUnit} for the headAdjustment
	 * @param headAdjustment
	 *            if -5 and unit is {@link ChronoUnit#MINUTES}, then starts 5
	 *            minutes before the impact
	 * @param footUnit
	 *            {@link ChronoUnit} for the footAdjustment
	 * @param footAdjustment
	 *            if 2 and unit is {@link ChronoUnit#HOURS}, then ends 2 hours
	 *            after the impact
	 * @return channels for the input
	 */
	public static Channel[] listChannels(String[] network, GlobalCMTID id, ChronoUnit headUnit, int headAdjustment,
			ChronoUnit footUnit, int footAdjustment) {
		Channel[] channels = new Channel[network.length];

		LocalDateTime cmtTime = id.getEvent().getCMTTime();
		LocalDateTime startTime = cmtTime.plus(headAdjustment, headUnit);
		LocalDateTime endTime = cmtTime.plus(footAdjustment, footUnit);

		for (int i = 0; i < network.length; i++)
			channels[i] = new Channel("?", network[i], startTime, endTime);

		return channels;
	}

}
