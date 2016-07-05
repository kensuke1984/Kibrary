package io.github.kensuke1984.kibrary.external.gmt;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Utilities;

/**
 * Helper for use of GMT
 * 
 * 
 * @version 0.0.3.4
 * 
 * 
 * @author Kensuke Konishi
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

	private String mapName;

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
		mapName = title.isEmpty() ? " " : title;
		setROption();
		epsFileName = "gmt" + Utilities.getTemporaryString() + ".eps";
		scriptFileName = "gmt" + Utilities.getTemporaryString() + ".sh";
	}

	/**
	 * @return the name of the eps file name
	 */
	public String getEPSfilename() {
		return epsFileName;
	}

	/**
	 * $header.eps and $header.sh If it is not set, the default is
	 * gmt+'dateString'.eps and .sh
	 * 
	 * @param header
	 *            set the name of the eps and script files
	 */
	public void setFileNameHeader(String header) {
		epsFileName = header + ".eps";
		scriptFileName = header + ".sh";
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
	public static String psscale(String name, double interval, double xpos, double ypos, double length, double width,
			Path cptPath, String... additionalOptions) {
		String dOption = " -D" + xpos + "/" + ypos + "/" + length + "/" + width + "h";
		String bOption = " -B" + interval + "+l\"" + name + "\"";
		String additional = Arrays.stream(additionalOptions).collect(Collectors.joining(" "));
		String cOption = " -C" + cptPath;
		String line = "psscale -V -K -O -P " + dOption + bOption + cOption + " " + additional + " >>$psname";
		return line;
	}

	public String psHeader() {
		return "#!/bin/sh\npsname=" + epsFileName;
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

	public String fixEPS() {
		return "eps2eps $psname .$psname && mv .$psname $psname";
	}

	/**
	 * If you want to fill dry areas, then -Gcolor (e.g. -Gbrown)
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
	 * a name of an eps
	 */
	private String epsFileName;

	/**
	 * a name of a script
	 */
	private String scriptFileName;

	private String[] outputMap() {
		setROption();
		String[] out = new String[8];
		out[0] = "#!/bin/sh";
		out[1] = "psname=\"" + epsFileName + "\"";
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

		out[1] = "pscoast -K -JQ " + rOption + bOption + " >> " + epsFileName;
		out[2] = "psxy -V -: -JQ -R -O -P -Sa0.2 -G255/0/0 -W1  -K " + eventFile + " > " + epsFileName;
		out[3] = "psxy -V -: -JQ -R -O -P -Si0.2 -G255/0/0 -W1  -K -O " + stationFile + " >> " + epsFileName;
		out[4] = "psxy -V -: -JQ -R -O -P -Sx0.2 -G255/0/0 -W1  -O " + perturbationPointFile + " >> " + epsFileName;
		out[5] = "grdimage ans.grd -J -Ccp2.cpt -B -O -R >> " + epsFileName;
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

/*
 * private void outputGridMakerScript() throws IOException {
 * 
 * Set<Double> rSet = new TreeSet<>(); for (int i = 0; i < grid.length; i++)
 * rSet.add(grid[i].getR()); double[] r = new double[rSet.size()]; { int i = 0;
 * for (double x : rSet) r[i++] = x; }
 * 
 * Path gridPath = outPath.resolve("gridmaker.sh"); try (PrintWriter pw = new
 * PrintWriter(Files.newBufferedWriter(gridPath))) { pw.println("#!/bin/sh");
 * pw.print("for depth in "); for (int i = 0; i < r.length; i++) pw.print(r[i] +
 * " "); pw.println(); pw.println("do"); pw.println("dep=${depth%.0}");
 * pw.println("grep \"$depth\" complemented.dat | \\"); pw.println(
 * "awk \'{print $1, $2, $4}\'  | \\"); pw.println("surface -G$dep.grd -R" +
 * minLongitude + "/" + maxLongitude + "/" + minLatitude + "/" + maxLatitude +
 * " -I5"); pw.println("#xyz2grd -G$dep.grd -R145/180/-10/30 -I2.5 -N0");
 * pw.println("grdsample $dep.grd -G${dep}comp.grd -I0.1"); pw.println("done");
 * }
 * 
 * gridPath.toFile().setExecutable(true); }
 * 
 * private void outputMaskMakerScript() throws IOException {
 * 
 * Set<Double> rSet = new TreeSet<>(); for (int i = 0; i < grid.length; i++)
 * rSet.add(grid[i].getR()); double[] r = new double[rSet.size()]; { int i = 0;
 * for (double x : rSet) r[i++] = x; }
 * 
 * Path maskPath = outPath.resolve("masking.sh"); try (PrintWriter pw = new
 * PrintWriter(Files.newBufferedWriter(maskPath))) { pw.println("#!/bin/sh");
 * pw.println("grdmask mask.dat -Gmask.grd -I0.1 -R" + minLongitude + "/" +
 * maxLongitude + "/" + minLatitude + "/" + maxLatitude + " -NNAN/1/1");
 * pw.print("for depth in "); for (int i = 0; i < r.length; i++) pw.print(r[i] +
 * " "); pw.println(); pw.println("do"); pw.println("dep=${depth%.0}");
 * pw.println("grdmath $dep\\comp.grd mask.grd OR = masked_data.grd");
 * pw.println("mv masked_data.grd $dep\\comp.grd"); pw.println("done"); }
 * maskPath.toFile().setExecutable(true);
 * 
 * }
 * 
 * private void outputNeoScript() throws IOException { Path neoPath =
 * outPath.resolve("neo.sh");
 * 
 * try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(neoPath))) {
 * pw.println("#!/bin/sh");
 * 
 * pw.println("# parameters for pscoast"); pw.println("R='-R" + minLongitude +
 * "/" + maxLongitude + "/" + minLatitude + "/" + maxLatitude + "';");
 * pw.println("J='-JQ145/15';"); pw.println("G='-G255/255/255';");
 * pw.println("B='-BWeSna30f10';"); pw.println("O='-A5000 -W1 -P';");
 * 
 * // pw.println("gmtset BASEMAP_FRAME_RGB 0/0/0"); // pw.println(
 * "gmtset ANOT_FONT_SIZE 25"); // pw.println("gmtset LABEL_FONT_SIZE 25"); //
 * pw.println("gmtset PAPER_MEDIA a0+"); // pw.println(
 * "gmtset PAGE_ORIENTATION landscape"); pw.println("outputps=\"test.ps\"");
 * pw.println("cpt=\"color.cpt\""); pw.println("scale=\"scale.cpt\"");
 * pw.println("grdimage 3855\\comp.grd $J  $R -C$cpt $B -K -P -Y30 > $outputps"
 * ); pw.println(
 * "psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K  -Y-1  -O >>$outputps");
 * pw.println(
 * "pscoast -V  -R -J -B:.\"350 - 400 km (B)\":  $O -K -O -Y1   >> $outputps");
 * 
 * pw.println(
 * "grdimage 3805\\comp.grd $J  $R -C$cpt $B -K -O -P   -X20 >> $outputps");
 * pw.println(
 * "psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
 * pw.println(
 * "pscoast -V -R -J -B:.\"300 - 350 km (A)\":  $O  -K -O -Y1 -X-1 >> $outputps"
 * );
 * 
 * pw.println(
 * "grdimage 3755\\comp.grd $J  $R -C$cpt $B -K -O -P  -X20 >> $outputps");
 * pw.println(
 * "psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
 * pw.println(
 * "pscoast -V -R -J -B:.\"250 - 300 km (B)\":  $O -K -O -Y1 -X-1 >> $outputps"
 * );
 * 
 * pw.println(
 * "grdimage 3705\\comp.grd $J  $R -C$cpt $B -K -O -P  -X20 >> $outputps");
 * pw.println(
 * "psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
 * pw.println(
 * "pscoast -V -R -J -B:.\"200 - 250 km (A)\":  $O -K -O -Y1 -X-1 >> $outputps"
 * );
 * 
 * pw.println(
 * "grdimage 3655\\comp.grd $J  $R -C$cpt $B -K -P -Y-23 -X-60 -O >> $outputps"
 * ); pw.println(
 * "psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
 * pw.println(
 * "pscoast -V -R -J -B:.\"150 - 200 km (B)\":  $O -K -O -Y1 -X-1 >> $outputps"
 * );
 * 
 * pw.println(
 * "grdimage 3605\\comp.grd $J  $R -C$cpt $B -K -O -P  -X20 >> $outputps");
 * pw.println(
 * "psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
 * pw.println(
 * "pscoast -V -R -J -B:.\"100 - 150 km (A)\":  $O  -K -O -Y1 -X-1 >> $outputps"
 * );
 * 
 * pw.println(
 * "grdimage 3555\\comp.grd $J  $R -C$cpt $B -K -O -P  -X20 >> $outputps");
 * pw.println(
 * "psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
 * pw.println(
 * "pscoast -V -R -J -B:.\"50 - 100 km (B)\":  $O -K -O -Y1 -X-1 >> $outputps");
 * 
 * pw.println(
 * "grdimage 3505\\comp.grd $J  $R -C$cpt $B -K -O -P  -X20 >> $outputps");
 * pw.println(
 * "psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
 * pw.println(
 * "pscoast -V -R -J -B:.\"0 - 50 km (A)\":  $O  -O -K  -Y1 -X-1 >> $outputps");
 * 
 * // pw.println("grdimage a.grd $J $R -Ccp12a.cpt -K -O -P -Y47 -X-40 // >>
 * $outputps"); // pw.println("psscale -Ccp2.cpt -D7/-1/5/0.5h -B2:Vs\\(\\%\\):
 * -K // -O -Y-1 -X1 -O >>$outputps"); // pw.println(
 * "pscoast -V -R -J $B:.\"Pattern A\": $O -K -O -Y1 -X-1 // >> $outputps"); //
 * // pw.println("grdimage b.grd $J $R -Ccp12a.cpt $B -K -O -P -X20 >> //
 * $outputps"); // pw.println("psscale -Ccp2.cpt -D7/-1/5/0.5h -B2:Vs\\(\\%\\):
 * -K // -O -Y-1 -X1 -O >>$outputps"); // pw.println(
 * "pscoast -V -R -J $B:.\"Pattern B\": $O -O -Y1 -X-1 >> // $outputps");
 * 
 * pw.println("ps2epsi $outputps");
 * 
 * pw.println("mv ${outputps%.ps}.epsi ${outputps%.ps}.eps");
 * 
 * // pw.println("echo $B:a:");
 * 
 * } neoPath.toFile().setExecutable(true); }
 * 
 * private void outputMapMakerScript() throws IOException { Set<Double> rSet =
 * new TreeSet<>(); for (int i = 0; i < grid.length; i++)
 * rSet.add(grid[i].getR()); double[] r = new double[rSet.size()]; { int i = 0;
 * for (double x : rSet) r[i++] = x; }
 * 
 * Path maskPath = outPath.resolve("mapMaker.sh"); try (PrintWriter pw = new
 * PrintWriter(Files.newBufferedWriter(maskPath))) { pw.println("#!/bin/sh");
 * pw.println("# parameters for pscoast"); pw.println("R='-R" + minLongitude +
 * "/" + maxLongitude + "/" + minLatitude + "/" + maxLatitude + "'");
 * pw.println("J='-JQ145/15'"); pw.println("G='-G255/255/255'");
 * pw.println("B='-Bg30WeSna30f20'"); pw.println("B='-Bg30WeSnf20'");
 * pw.println("O='-A5000 -W1 -P'");
 * 
 * pw.println("gmtset BASEMAP_FRAME_RGB 0/0/0"); pw.println(
 * "gmtset LABEL_FONT_SIZE 15"); pw.print("for depth in "); for (int i = 0; i <
 * r.length; i++) pw.print(r[i] + " "); pw.println(); pw.println("do");
 * pw.println("depth=${depth%.0}"); pw.println("outputps=\"$depth.ps\"");
 * pw.println(
 * "grdimage $depth\\comp.grd $J  $R -Ccolor$depth.cpt  -K -P -Y5 > $outputps");
 * // pw.println("psscale -Cscale$depth.cpt -D7/-1/5/0.5h -K -O //
 * >>$outputps"); pw.println(
 * "psscale -Cscale$depth.cpt -D7/-1/5/0.5h -Ba1g0.5 -K -O >>$outputps");
 * pw.println("pscoast -V -R -J $B  $O  -O >> $outputps"); pw.println("done"); }
 * maskPath.toFile().setExecutable(true);
 * 
 * }
 * 
 */
