package manhattan.gmt;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import manhattan.template.HorizontalPosition;

/**
 * Helper for use of GMT
 * 
 * 
 * @version 0.0.3.1
 * 
 * 
 * @author Kensuke
 * 
 */
public final class GMTMap {

	/**
	 * the minimum value of longitude range the default value is 0
	 */
	private int minLongitude = 0;

	/**
	 * the maximum value of longitude range the default value is 360
	 */
	private int maxLongitude = 360;

	/**
	 * the minimum value of latitude range the default value is -90
	 */
	private int minLatitude = -90;

	/**
	 * the maximum value of latitude range the default value is 90
	 */
	private int maxLatitude = 90;

	String rOption;

	private void setROption() {
		rOption = " -R" + minLongitude + "/" + maxLongitude + "/" + minLatitude + "/" + maxLatitude + " ";
	}

	String getrOption() {
		return rOption;
	}

	void setrOption(String rOption) {
		this.rOption = rOption;
	}

	String getbOption() {
		return bOption;
	}

	void setbOption(String bOption) {
		this.bOption = bOption;
	}

	private String bOption = " -Bpxy30f10g10 ";

	private String eventFile = "event.inf";

	private String stationFile = "station.inf";

	private String gridFile = "ans.grd";

	private String perturbationPointFile = "perturbationPoint.inf";

	private String mapName = "MAP";

	/**
	 * @param title
	 *            for the map
	 * @param minLongitude
	 *            [deg]
	 * @param maxLongitude
	 *            [deg]
	 * @param minLatitude
	 *            [deg]
	 * @param maxLatitude
	 *            [deg]
	 */
	public GMTMap(String title, int minLongitude, int maxLongitude, int minLatitude, int maxLatitude) {
		this.minLongitude = minLongitude;
		this.maxLongitude = maxLongitude;
		this.minLatitude = minLatitude;
		this.maxLatitude = maxLatitude;
		mapName = title;
		setROption();
	}

	/**
	 * @param symbol
	 *            type of marking
	 * @param symbolSize
	 *            size of symbols
	 * @param value
	 *            of plotting
	 * @param colorPalletPath
	 *            Path of a color pallet
	 * @param position
	 *            on the map
	 * @param additionalOptions
	 *            if any options
	 * @return echo latitude longitude value | psxy -V -: -J -R -P -K -O symbol
	 *         [additional] &gt;&gt; $psname
	 */
	public static String psxy(Symbol symbol, double symbolSize, double value, Path colorPalletPath,
			HorizontalPosition position, String additionalOptions) {
		String cpOption = " -C" + colorPalletPath.toString();
		return "echo " + position + " " + value + " " + symbolSize + " | " + "psxy -V -: -J -R " + symbol.getOption()
				+ cpOption + " " + additionalOptions + " -K -O -P  >> $psname";
	}

	/**
	 * @param symbol
	 *            type of marking
	 * @param symbolSize
	 *            size of symbols
	 * @param position
	 *            on the map
	 * @param additionalOptions
	 *            if any options
	 * @return echo latitude longitude | psxy -V -: -J -R -P -K -O symbol
	 *         [additional] &gt;&gt; $psname
	 */
	public static String psxy(Symbol symbol, double symbolSize, HorizontalPosition position,
			String... additionalOptions) {
		String additional = Arrays.stream(additionalOptions).collect(Collectors.joining(" "));
		return "echo " + position.getLatitude() + " " + position.getLongitude() + " " + symbolSize + " | "
				+ "psxy -V -: -J -R " + symbol.getOption() + " " + additional + " -P -K -O >> $psname";
	}

	/**
	 * Draw a line from start to end
	 * 
	 * @param start
	 *            start position of the line
	 * @param end
	 *            end position of the line
	 * @param additionalOptions
	 *            any other options
	 * @return psxy -V -: -J -R -P -K -O [additional] &gt;&gt; $psname
	 */
	public static String psxy(HorizontalPosition start, HorizontalPosition end, String... additionalOptions) {
		String echoPart = "echo -e " + start.getLatitude() + " " + start.getLongitude() + "\\\\n" + end.getLatitude()
				+ " " + end.getLongitude();
		String additional = Arrays.stream(additionalOptions).collect(Collectors.joining(" "));
		String psxyPart = "psxy -V -: -J -R -P -K -O " + additional + " >>$psname";
		// System.out.println(psxyPart);
		return echoPart + " | " + psxyPart;
	}

	/**
	 * 
	 * @param name
	 *            of scale
	 * @param interval
	 *            of tics
	 * @param xpos
	 *            x position of edge
	 * @param ypos
	 *            y position of edge
	 * @param length
	 *            of scale
	 * @param width
	 *            of scale
	 * @param cptPath
	 *            Path of pallet
	 * @param additionalOptions
	 *            if any
	 * @return psxy -V -K -O -P -Bname -Dxpos/ypos/length/width(h) -CcptPath >>
	 *         $psname
	 */
	public static String psscale(String name, int interval, int xpos, int ypos, int length, int width, Path cptPath,
			String... additionalOptions) {
		String dOption = "-D" + xpos + "/" + ypos + "/" + length + "/" + width + "h";
		String bOption = "-B" + interval + "+l\"" + name + "\"";
		String additional = Arrays.stream(additionalOptions).collect(Collectors.joining(" "));
		String cOption = "-C" + cptPath;
		String line = "psscale -V -K -O -P " + dOption + " " + bOption + cOption + " " + additional + " >>$psname";
		return line;
	}

	/**
	 * Create postscript by psbasemap
	 * 
	 * @param additionalOptions
	 *            if you want to add options
	 * @return psbasemap -L -R??/??/??/?? -JQ15 -B+t"hoge" -K
	 */
	public String psStart(String... additionalOptions) {
		String additional = Arrays.stream(additionalOptions).collect(Collectors.joining(" "));
		return "psbasemap" + rOption + "-JQ15 -B+t\"" + mapName + "\" " + additional + " -K -V -P > $psname";
	}

	/**
	 * @param additionalOptions
	 *            if you want to add options
	 * @return psbasemap -R -Bhoge -O -V -P -J
	 */
	public String psEnd(String... additionalOptions) {
		String additional = Arrays.stream(additionalOptions).collect(Collectors.joining(" "));
		return "psbasemap -R -J" + bOption + additional + " -O -V -P >> $psname";

	}

	/**
	 * 
	 * @param additionalOptions
	 *            if any the return will have them. ex) National boundaries -N1
	 * @return pscoast -J -R -Bs -Dc -V -W -K -O (additional) &gt;&gt; $psname
	 */
	public static String psCoast(String... additionalOptions) {
		String additional = Arrays.stream(additionalOptions).collect(Collectors.joining(" "));
		return "pscoast -J -R -B -Dc -W " + additional + " -V -P -K -O >> $psname";
	}

	/**
	 * 書き出すpsファイルの名前
	 */
	private String outputFile = "map.ps";

	private String[] outputMap() {
		setROption();
		String[] out = new String[8];
		out[0] = "#!/bin/sh";
		out[1] = "psname=\"" + outputFile + "\"";
		out[2] = "gmtset BASEMAP_FRAME_RGB 0/0/0";
		out[3] = "gmtset LABEL_FONT_SIZE 15";
		out[4] = "";// TODO
		out[5] = "awk '{print $1, $2}' " + eventFile + " | psxy -V -: -JQ -R -O -P -Sa0.2 -G255/0/0 -W1  -K "
				+ " > $psname";
		out[6] = "awk '{print $1, $2}' " + stationFile + " |psxy -V -: -JQ -R -O -P -Si0.2 -G255/0/0 -W1  -K -O "
				+ " >> $psname";
		out[7] = "awk '{print $1, $2}' " + perturbationPointFile + " |psxy -V -: -JQ -R -O -P -Sx0.2 -G255/0/0 -W1  -O "
				+ " >> $psname";

		return out;
	}

	public String[] outputMapwGrid() {
		setROption();
		String[] out = new String[6];
		out[0] = "#!/bin/sh";

		out[1] = "pscoast -K -JQ " + rOption + bOption + " >> " + outputFile;
		out[2] = "psxy -V -: -JQ -R -O -P -Sa0.2 -G255/0/0 -W1  -K " + eventFile + " > " + outputFile;
		out[3] = "psxy -V -: -JQ -R -O -P -Si0.2 -G255/0/0 -W1  -K -O " + stationFile + " >> " + outputFile;
		out[4] = "psxy -V -: -JQ -R -O -P -Sx0.2 -G255/0/0 -W1  -O " + perturbationPointFile + " >> " + outputFile;
		out[5] = "grdimage ans.grd -J -Ccp2.cpt -B -O -R >> " + outputFile;
		return out;
	}

	private void createGrid() {
		String cmd;
		String fileName = " hoge.dat ";
		String gridFileName = " -G hoge.grd "; //
		String region = " -R0/10/20/30 "; // xmin/xmax/ymin/ymax -F
		String increment = " -I1/2 "; // x/y
		cmd = "xyz2grd " + fileName + gridFileName + " -I0.4 " + region;
		// surface test -Greal.grd -I1 -R-100/-85/0/40
		// psxy g.txt -R-4/4/-4/4 -Jx1 -Ba1 -Sc0.6 -Ccp.cpt > g.eps
		// grdsample
		// xyz2grd 3505.dat -G3505.grd -R0/40/-100/-85 -I5
		// xyz2grd 3505.dat -G3505.grd -R-105/-75/-10/40 -I5 -N0
		// grdsample 3505.grd -G3505comp.grd -I1
		// psscale -Ccp2.cpt -B5 -D7/3/8/1h -K -O >>$outputps
		// project 3505.dat -Fxyzpqrs -C-90/0 -E-90/30 -L0/0 -W0/0
		// grdimage cs.grd -Jx1 -Ccp2.cpt -Ba1 > test
		// grdimage cs.grd -Jx0.5/0.1 -Ccp2.cpt -Ba10/a10 > test.ps
		// surface cs.dat -Gcs.grd -I1 -R0/35/3505/3605 -N10
		// grdimage 3505cs.grd -Jx0.5/0.1 -Ccp2.cpt -Ba10/a10 > test.ps

	}
}
