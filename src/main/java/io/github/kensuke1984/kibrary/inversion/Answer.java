package io.github.kensuke1984.kibrary.inversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.external.gmt.ColorPallet;
import io.github.kensuke1984.kibrary.external.gmt.CrossSection;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

/**
 * inversionの後処理
 * 
 * TODO gotta make methods to make scripts for gmt?
 * 
 * 
 * cross section information File center point(latitude, longitude), theta,
 * azimuth, deltaTheta, r[]
 * 
 * grid File latitude longitude radius
 *          TODO maks.dat
 * 
 * @version 0.0.3
 * 
 * @author kensuke
 * 
 */
public class Answer extends parameter.AnswerPa {

	private CrossSection[] crossSections;

	/**
	 * 解の順番
	 */
	private List<UnknownParameter> unknownList;

	/**
	 * 解ベクトル
	 */
	private double[] m;

	private Comparator<Location> locationComparator = new Comparator<Location>() {

		@Override
		public int compare(Location o1, Location o2) {
			if (o1.getLongitude() > o2.getLongitude())
				return 1;
			else if (o1.getLongitude() == o2.getLongitude())
				if (o1.getLatitude() > o2.getLatitude())
					return 1;
				else if (o1.getLatitude() == o2.getLatitude()) {
					// System.out.println(o1.getR()+" "+o2.getR());
					if (o1.getR() > o2.getR())
						return 1;
					else if (o1.getR() == o2.getR())
						return 0;
					else
						return -1;
				}
				// return 0;
				else
					return -1;
			else

				return -1;
		}

	};

	public CrossSection[] getCrossSections() {
		return crossSections;
	}


	public double[] getM() {
		return m;
	}

	public double[] getColumn(HorizontalPosition position, double[] r) {
		double[] column = new double[r.length];
		for (int i = 0; i < r.length; i++)
			column[i] = complement(4, 2, position.toLocation(r[i]), PartialType.MU);
		return column;
	}

	public Location[] getGrid() {
		return grid;
	}


	private Answer(Path parameterPath) throws IOException {
		super(parameterPath);
		setUnknownList();
		setGrid();
		// System.exit(0);
		setCrossSections();
		m = new double[unknownList.size()];
		readAnswer();
	}

	public boolean canGO() throws IOException {
		boolean canGO = true;
		if (Files.exists(outPath)) {
			System.err.println(outPath.toString());
			canGO = false;
		}
		if (!Files.exists(answerPath)) {
			System.err.println(answerPath.toString());
			canGO = false;
		}
		if (!Files.exists(unknownParameterSetPath)) {
			System.err.println(unknownParameterSetPath.toString());
			canGO = false;
		}
		if (crossSectionListPath != null && !Files.exists(crossSectionListPath)) {
			System.err.println(crossSectionListPath.toString());
			canGO = false;
		}
		if (gridPath != null && !Files.exists(gridPath)) {
			System.err.println(gridPath.toString());
			canGO = false;
		}
		if (canGO)
			Files.createDirectories(outPath);
		return canGO;
	}

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Answer answer = null;
		if (args.length != 0) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			answer = new Answer(parameterPath);
		} else
			answer = new Answer(null);
		// if (!answer.canGO())
		// System.exit(0);

		// System.out.println(answer.crossSections[0].getAzimuth());
		// System.exit(0);
		// System.out.println(args[0]);
		answer.complement(answer.grid, PartialType.Q, answer.outPath.resolve("complement.dat"));
		answer.output();
		// if (true)
		// return;
		System.exit(0);

		Path csPath = answer.outPath.resolve("crosssection");
		Files.createDirectories(csPath);
		for (int i = 0; i < answer.crossSections.length; i++) {
			Path outans = csPath.resolve(i + ".dat");
			answer.createCrossSection(answer.crossSections[i], PartialType.MU, outans);
		}
		// System.exit(0);
		//

	}

	private void output() throws IOException {
		outputGridMakerScript();
		outputMaskMakerScript();
		outputNeoScript();
		outputMapMakerScript();
	}

	private void setUnknownList() throws IOException {
		unknownList = UnknownParameterFile.read(unknownParameterSetPath);
	}

	private void computeGeometry() {
		double maxLat = -90;
		double minLat = 90;
		double maxLon = -180;
		double minLon = 180;
		for (UnknownParameter p : unknownList ) {
			HorizontalPosition pos = null;
			if (p instanceof ElasticParameter)
				pos = ((ElasticParameter) p).getPointLocation();
			else
				return;
			maxLat = pos.getLatitude() < maxLat ? maxLat : pos.getLatitude();
			maxLon = pos.getLongitude() < maxLon ? maxLon : pos.getLongitude();
			minLat = pos.getLatitude() < minLat ? pos.getLatitude() : minLat;
			minLon = pos.getLongitude() < minLon ? pos.getLongitude() : minLon;
		}
		this.maxLatitude = maxLat;
		this.minLatitude = minLat;
		this.maxLongitude = maxLon;
		this.minLongitude = minLon;
	}

	/**
	 * 
	 * the center value of longitudes in parameter Set
	 */
	private double minLongitude;
	/**
	 * the center value of latitudes in parameter Set
	 */
	private double minLatitude;
	/**
	 * 
	 * the center value of longitudes in parameter Set
	 */
	private double maxLongitude;
	/**
	 * the center value of latitudes in parameter Set
	 */
	private double maxLatitude;

	private Location[] grid;

	private void setGrid() throws IOException {
		if (gridPath == null) {
			System.out.println("grid File is not set");
			return;
		}
		List<Location> locList = new ArrayList<>();
		try (BufferedReader br = Files.newBufferedReader(gridPath)) {
			String line = null;
			while (null != (line = br.readLine())) {
				if (line.trim().startsWith("#"))
					continue;
				String[] parts = line.trim().split("\\s+");
				locList.add(new Location(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
						Double.parseDouble(parts[2])));

			}
		}
		grid = (Location[]) locList.toArray(new Location[0]);

	}

	/**
	 * TODO
	 */
	private void outputGridMakerScript() throws IOException {

		Set<Double> rSet = new TreeSet<>();
		for (int i = 0; i < grid.length; i++)
			rSet.add(grid[i].getR());
		double[] r = new double[rSet.size()];
		{
			int i = 0;
			for (double x : rSet)
				r[i++] = x;
		}

		Path gridPath = outPath.resolve("gridmaker.sh");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(gridPath))) {
			pw.println("#!/bin/sh");
			pw.print("for depth in ");
			for (int i = 0; i < r.length; i++)
				pw.print(r[i] + " ");
			pw.println();
			pw.println("do");
			pw.println("dep=${depth%.0}");
			pw.println("grep \"$depth\" complemented.dat | \\");
			pw.println("awk \'{print $1, $2, $4}\'  | \\");
			pw.println("surface -G$dep.grd -R" + minLongitude + "/" + maxLongitude + "/" + minLatitude + "/"
					+ maxLatitude + " -I5");
			pw.println("#xyz2grd -G$dep.grd -R145/180/-10/30 -I2.5 -N0");
			pw.println("grdsample $dep.grd -G${dep}comp.grd -I0.1");
			pw.println("done");
		}

		gridPath.toFile().setExecutable(true);
	}

	/**
	 * TODO mask.dat
	 */
	private void outputMaskMakerScript() throws IOException {

		Set<Double> rSet = new TreeSet<>();
		for (int i = 0; i < grid.length; i++)
			rSet.add(grid[i].getR());
		double[] r = new double[rSet.size()];
		{
			int i = 0;
			for (double x : rSet)
				r[i++] = x;
		}

		Path maskPath = outPath.resolve("masking.sh");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(maskPath))) {
			pw.println("#!/bin/sh");
			pw.println("grdmask mask.dat -Gmask.grd -I0.1 -R" + minLongitude + "/" + maxLongitude + "/" + minLatitude
					+ "/" + maxLatitude + " -NNAN/1/1");
			pw.print("for depth in ");
			for (int i = 0; i < r.length; i++)
				pw.print(r[i] + " ");
			pw.println();
			pw.println("do");
			pw.println("dep=${depth%.0}");
			pw.println("grdmath $dep\\comp.grd mask.grd OR = masked_data.grd");
			pw.println("mv masked_data.grd $dep\\comp.grd");
			pw.println("done");
		}
		maskPath.toFile().setExecutable(true);

	}

	private void outputNeoScript() throws IOException {
		Path neoPath = outPath.resolve("neo.sh");

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(neoPath))) {
			pw.println("#!/bin/sh");

			pw.println("# parameters for pscoast");
			pw.println("R='-R" + minLongitude + "/" + maxLongitude + "/" + minLatitude + "/" + maxLatitude + "';");
			pw.println("J='-JQ145/15';");
			pw.println("G='-G255/255/255';");
			pw.println("B='-BWeSna30f10';");
			pw.println("O='-A5000 -W1 -P';");

			// pw.println("gmtset BASEMAP_FRAME_RGB 0/0/0");
			// pw.println("gmtset ANOT_FONT_SIZE 25");
			// pw.println("gmtset LABEL_FONT_SIZE 25");
			// pw.println("gmtset PAPER_MEDIA a0+");
			// pw.println("gmtset PAGE_ORIENTATION landscape");
			pw.println("outputps=\"test.ps\"");
			pw.println("cpt=\"color.cpt\"");
			pw.println("scale=\"scale.cpt\"");
			pw.println("grdimage 3855\\comp.grd $J  $R -C$cpt $B -K -P -Y30 > $outputps");
			pw.println("psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K  -Y-1  -O >>$outputps");
			pw.println("pscoast -V  -R -J -B:.\"350 - 400 km (B)\":  $O -K -O -Y1   >> $outputps");

			pw.println("grdimage 3805\\comp.grd $J  $R -C$cpt $B -K -O -P   -X20 >> $outputps");
			pw.println("psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
			pw.println("pscoast -V -R -J -B:.\"300 - 350 km (A)\":  $O  -K -O -Y1 -X-1 >> $outputps");

			pw.println("grdimage 3755\\comp.grd $J  $R -C$cpt $B -K -O -P  -X20 >> $outputps");
			pw.println("psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
			pw.println("pscoast -V -R -J -B:.\"250 - 300 km (B)\":  $O -K -O -Y1 -X-1 >> $outputps");

			pw.println("grdimage 3705\\comp.grd $J  $R -C$cpt $B -K -O -P  -X20 >> $outputps");
			pw.println("psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
			pw.println("pscoast -V -R -J -B:.\"200 - 250 km (A)\":  $O -K -O -Y1 -X-1 >> $outputps");

			pw.println("grdimage 3655\\comp.grd $J  $R -C$cpt $B -K -P -Y-23 -X-60 -O >> $outputps");
			pw.println("psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
			pw.println("pscoast -V -R -J -B:.\"150 - 200 km (B)\":  $O -K -O -Y1 -X-1 >> $outputps");

			pw.println("grdimage 3605\\comp.grd $J  $R -C$cpt $B -K -O -P  -X20 >> $outputps");
			pw.println("psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
			pw.println("pscoast -V -R -J -B:.\"100 - 150 km (A)\":  $O  -K -O -Y1 -X-1 >> $outputps");

			pw.println("grdimage 3555\\comp.grd $J  $R -C$cpt $B -K -O -P  -X20 >> $outputps");
			pw.println("psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
			pw.println("pscoast -V -R -J -B:.\"50 - 100 km (B)\":  $O -K -O -Y1 -X-1 >> $outputps");

			pw.println("grdimage 3505\\comp.grd $J  $R -C$cpt $B -K -O -P  -X20 >> $outputps");
			pw.println("psscale -C$scale -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K -O -Y-1 -X1>>$outputps");
			pw.println("pscoast -V -R -J -B:.\"0 - 50 km (A)\":  $O  -O -K  -Y1 -X-1 >> $outputps");

			// pw.println("grdimage a.grd $J $R -Ccp12a.cpt -K -O -P -Y47 -X-40
			// >> $outputps");
			// pw.println("psscale -Ccp2.cpt -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K
			// -O -Y-1 -X1 -O >>$outputps");
			// pw.println("pscoast -V -R -J $B:.\"Pattern A\": $O -K -O -Y1 -X-1
			// >> $outputps");
			//
			// pw.println("grdimage b.grd $J $R -Ccp12a.cpt $B -K -O -P -X20 >>
			// $outputps");
			// pw.println("psscale -Ccp2.cpt -D7/-1/5/0.5h -B2:Vs\\(\\%\\): -K
			// -O -Y-1 -X1 -O >>$outputps");
			// pw.println("pscoast -V -R -J $B:.\"Pattern B\": $O -O -Y1 -X-1 >>
			// $outputps");

			pw.println("ps2epsi $outputps");

			pw.println("mv ${outputps%.ps}.epsi ${outputps%.ps}.eps");

			// pw.println("echo $B:a:");

		}
		neoPath.toFile().setExecutable(true);
	}

	/**
	 */
	private void outputMapMakerScript() throws IOException {
		Set<Double> rSet = new TreeSet<>();
		for (int i = 0; i < grid.length; i++)
			rSet.add(grid[i].getR());
		double[] r = new double[rSet.size()];
		{
			int i = 0;
			for (double x : rSet)
				r[i++] = x;
		}

		Path maskPath = outPath.resolve("mapMaker.sh");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(maskPath))) {
			pw.println("#!/bin/sh");
			pw.println("# parameters for pscoast");
			pw.println("R='-R" + minLongitude + "/" + maxLongitude + "/" + minLatitude + "/" + maxLatitude + "'");
			pw.println("J='-JQ145/15'");
			pw.println("G='-G255/255/255'");
			pw.println("B='-Bg30WeSna30f20'");
			pw.println("B='-Bg30WeSnf20'");
			pw.println("O='-A5000 -W1 -P'");

			pw.println("gmtset BASEMAP_FRAME_RGB 0/0/0");
			pw.println("gmtset LABEL_FONT_SIZE 15");
			pw.print("for depth in ");
			for (int i = 0; i < r.length; i++)
				pw.print(r[i] + " ");
			pw.println();
			pw.println("do");
			pw.println("depth=${depth%.0}");
			pw.println("outputps=\"$depth.ps\"");
			pw.println("grdimage $depth\\comp.grd $J  $R -Ccolor$depth.cpt  -K -P -Y5 > $outputps");
			// pw.println("psscale -Cscale$depth.cpt -D7/-1/5/0.5h -K -O
			// >>$outputps");
			pw.println("psscale -Cscale$depth.cpt -D7/-1/5/0.5h -Ba1g0.5 -K -O >>$outputps");
			pw.println("pscoast -V -R -J $B  $O  -O >> $outputps");
			pw.println("done");
		}
		maskPath.toFile().setExecutable(true);

	}

	/**
	 * 中心緯度、 中心経度、 角度、 方位、 角度の間隔
	 */
	private void setCrossSections() throws IOException {
		if (crossSectionListPath == null) {
			System.out.println("CrossSectionListFile is not set");
			return;
		}
		// crossSectionListFileHorizontalPosition centerLocation, double theta,
		// double azimuth, double deltaTheta, double[] r
		List<CrossSection> csList = new ArrayList<>();
		// System.exit(0);
		try (BufferedReader br = Files.newBufferedReader(crossSectionListPath)) {

			String line = null;
			while (null != (line = br.readLine())) {
				if (line.trim().startsWith("#"))
					continue;
				String[] parts = line.trim().split("\\s+");
				HorizontalPosition center = new HorizontalPosition(Double.parseDouble(parts[0]),
						Double.parseDouble(parts[1]));
				double theta = Double.parseDouble(parts[2]);
				theta = Math.toRadians(theta);
				double azimuth = Double.parseDouble(parts[3]);
				azimuth = Math.toRadians(azimuth);
				double deltaTheta = Double.parseDouble(parts[4]);
				deltaTheta = Math.toRadians(deltaTheta);
				double[] r = new double[parts.length - 5];
				for (int i = 0; i < r.length; i++)
					r[i] = Double.parseDouble(parts[5 + i]);
				// System.exit(0);
				csList.add(new CrossSection(center, theta, azimuth, deltaTheta, r));

			}
		} // System.exit(0);
		crossSections = (CrossSection[]) csList.toArray(new CrossSection[csList.size()]);

	}

	/**
	 * 解を読み込む
	 */
	private void readAnswer() throws IOException {
		if (!canGO())
			throw new RuntimeException();
		try (BufferedReader br = Files.newBufferedReader(answerPath)) {
			int n = unknownList.size();
			// System.out.println("hi");

			for (int i = 0; i < n; i++) {
				m[i] = Double.parseDouble(br.readLine());
				// Double d = Double.parseDouble(br.readLine());
				// double value = d.doubleValue();
				// ansMap.put(location[i], Double.parseDouble(br.readLine()));
				// ansMap.put(location[i], d);
				// if(i<100)
				// System.out.println(location[i]+" "+ansMap.get(location[i])+"
				// "+value+" "+location[i].getR());
				// if(i>100)
				// System.exit(0)
				// ;
			}
		}
		computeGeometry();
		// System.exit(0);
		createColorPallet();

	}

	private void createColorPallet() throws IOException {
		System.out.println("hi" + grid.length);

		Set<Double> rSet = new TreeSet<>();
		for (int i = 0; i < grid.length; i++)
			rSet.add(grid[i].getR());
		PolynomialStructure p = PolynomialStructure.PREM;
		for (double r : rSet) {
			// System.out.println(r);
			double max = maxOf(r);
			ColorPallet.oobayashi(max).output(outPath.resolve("color" + (int) r + ".cpt"));
			double scale = Math.sqrt(1 + max / p.getMu(r)) * 100 - 100;
			System.out.println(r + " " + scale);
			ColorPallet.oobayashi(scale).output(outPath.resolve("scale" + (int) r + ".cpt"));

		}

	}

	/**
	 * @param r
	 * @return the max absolute value of r
	 */
	private double maxOf(double r) {
		double max = 0;
		for (int i = 0; i < unknownList.size(); i++) {
			UnknownParameter p = unknownList.get(i);
			// System.out.println(p);
			if (p instanceof ElasticParameter)
				if (((ElasticParameter) p).getPointLocation().getR() == r)
					max = max < Math.abs(m[i]) ? Math.abs(m[i]) : max;
		}

		return max;
	}

	/**
	 * @param type
	 *            摂動の種類
	 * @return typeについての解のマップを作る
	 */
	private Map<Location, Double> createMap(PartialType type) {
		if (type == PartialType.TIME) {
			System.out.println("madda");
			return null;
		}

		Map<Location, Double> ansMap = new TreeMap<>(locationComparator);
		for (int i = 0, mlen = unknownList.size(); i < mlen; i++) {
			if (unknownList.get(i).getPartialType() != type)
				continue;
			ElasticParameter ep = (ElasticParameter) unknownList.get (i);
			ansMap.put(ep.getPointLocation(), m[i]);
		}

		return ansMap;
	}

	/**
	 * @param nPoints
	 *            number of points
	 * @param nPower
	 *            距離の何乗で補間するか
	 * @param location
	 *            location for complement
	 * @param type
	 *            {@link PartialType}
	 * @return locationの直近nPoints点からの補間値
	 */
	public double complement(int nPoints, int nPower, Location location, PartialType type) {
		double value = 0;
		Map<Location, Double> ansMap = createMap(type);

		if (type == PartialType.TIME) {
			System.out.println("madda");
			return 0;
		}
		if (ansMap.containsKey(location)) {
			return ansMap.get(location);
		}
		Location[] nearLocations = location.getNearestLocation(ansMap.keySet().toArray(new Location[0]));
		double[] r = new double[nPoints];
		double rTotal = 0;
		for (int iPoint = 0; iPoint < nPoints; iPoint++) {
			r[iPoint] = Math.pow(nearLocations[iPoint].getDistance(location), nPower);
			rTotal += 1 / r[iPoint];
			// System.out.println(nearLocations[iPoint]);
			// System.out.println(r[iPoint]);
		}
		for (int iPoint = 0; iPoint < nPoints; iPoint++) {
			value += ansMap.get(nearLocations[iPoint]) / r[iPoint];
			// values[i]= r[iPoint]* ansMap.get(arg0)
		}
		value /= rTotal;
		return value;
	}

	/**
	 * 与えたpointsに対して補間値を入れる
	 * 
	 * @param locations
	 *            {@link Location}s for complement
	 * @param type
	 *            for the value
	 * @param out
	 *            {@link File} for output
	 * @throws IOException if an I/O error occurs
	 */
	public void complement(Location[] locations, PartialType type, Path out) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out, StandardOpenOption.CREATE_NEW))) {
			for (int i = 0; i < locations.length; i++)
				pw.println(locations[i].getLongitude() + " " + locations[i].getLatitude() + " " + locations[i].getR()
						+ " " + complement(4, 2, locations[i], type));
			// System.out.println(locations[i] + " " + value);
		}
	}

	/**
	 * 与えたクロスセクションに対するデータを作る
	 * 
	 * @param cs
	 *            {@link CrossSection}
	 * @param type
	 *            {@link PartialType}
	 * @param out
	 *            {@link File} for out
	 * @throws IOException if an I/O error occurs
	 */
	public void createCrossSection(CrossSection cs, PartialType type, Path out) throws IOException {
		// HorizontalPosition[] points = cs.getPositions();
		Location[] locations = cs.getLocations();
		// double[] values = new double[points.length];
		double[] thetaX = cs.getThetaX();

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out, StandardOpenOption.CREATE_NEW));) {

			for (int i = 0; i < locations.length; i++) {
				pw.println(locations[i].getLongitude() + " " + locations[i].getLatitude() + " " + locations[i].getR()
						+ " " + Math.toDegrees(thetaX[i]) + " " + complement(4, 2, locations[i], type));
				// System.out.println(locations[i] + " " + thetaX[i]);
			}
		}
	}

}
