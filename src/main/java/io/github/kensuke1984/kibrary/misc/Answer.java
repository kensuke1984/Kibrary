/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.inversion.Physical3DParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.external.gmt.CrossSection;
import io.github.kensuke1984.kibrary.util.Location;

/**
 * @version 0.0.1
 * @since 2018/05/21
 * @author Yuki
 *
 */
public class Answer {
	
	private static CrossSection[] crossSections;

	/**
	 * 解の順番
	 */
	private static List<UnknownParameter> unknownList;

	/**
	 * 解ベクトル
	 */
	private static double[] m;

	private static Comparator<Location> locationComparator = new Comparator<Location>() {

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
	
	public Location[] getGrid() {
		return grid;
	}
	
	private static Location[] grid;
	/**
	 * 
	 * the center value of longitudes in parameter Set
	 */
	private static double minLongitude;
	/**
	 * the center value of latitudes in parameter Set
	 */
	private static double minLatitude;
	/**
	 * 
	 * the center value of longitudes in parameter Set
	 */
	private static double maxLongitude;
	/**
	 * the center value of latitudes in parameter Set
	 */
	private static double maxLatitude;
	
	private static void setGrid() throws IOException {
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
	
	private static Path outPath;
	private static Path answerPath;
	private static Path unknownParameterSetPath;
	private static Path crossSectionListPath;
	private static Path gridPath;
	
	public CrossSection[] getCrossSections() {
		return crossSections;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
//		Answer answer = null;
		outPath = Paths.get(args[0]);
		answerPath = Paths.get(args[0]);
		unknownParameterSetPath = Paths.get(args[0]);
		crossSectionListPath = Paths.get(args[0]);
		gridPath = Paths.get(args[0]);
		setUnknownList();
		setGrid();
		setCrossSections();
		readAnswer();
		m = new double[unknownList.size()];
		complement(grid, PartialType.MU, outPath.resolve("complement.dat"));
//		answer.output();
		Path csPath = outPath.resolve("crosssection");
		Files.createDirectories(csPath);
		for (int i = 0; i < crossSections.length; i++) {
			Path outans = csPath.resolve(i + ".dat");
			createCrossSection(crossSections[i], PartialType.MU, outans);
		}
	}
	
	/**
	 * 解を読み込む
	 */
	private static void readAnswer() throws IOException {
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
//		createColorPallet();

	}
	
	private static void computeGeometry() {
		double maxLat = -90;
		double minLat = 90;
		double maxLon = -180;
		double minLon = 180;
		for (UnknownParameter p : unknownList ) {
			HorizontalPosition pos = null;
			if (p instanceof Physical3DParameter)
				pos = ((Physical3DParameter) p).getPointLocation();
			else
				return;
			maxLat = pos.getLatitude() < maxLat ? maxLat : pos.getLatitude();
			maxLon = pos.getLongitude() < maxLon ? maxLon : pos.getLongitude();
			minLat = pos.getLatitude() < minLat ? pos.getLatitude() : minLat;
			minLon = pos.getLongitude() < minLon ? pos.getLongitude() : minLon;
		}
		maxLatitude = maxLat;
		minLatitude = minLat;
		maxLongitude = maxLon;
		minLongitude = minLon;
	}
	
	/**
	 * 中心緯度、 中心経度、 角度、 方位、 角度の間隔
	 */
	private static void setCrossSections() throws IOException {
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
				System.out.println("Reading the cross section information file.");
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
	
	private static void setUnknownList() throws IOException {
		unknownList = UnknownParameterFile.read(unknownParameterSetPath);
	}
	
	public static boolean canGO() throws IOException {
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
	 * @param type
	 *            摂動の種類
	 * @return typeについての解のマップを作る
	 */
	private static Map<Location, Double> createMap(PartialType type) {
		if (type == PartialType.TIME) {
			System.out.println("madda");
			return null;
		}

		Map<Location, Double> ansMap = new TreeMap<>(locationComparator);
		for (int i = 0, mlen = unknownList.size(); i < mlen; i++) {
			if (unknownList.get(i).getPartialType() != type)
				continue;
			Physical3DParameter ep = (Physical3DParameter) unknownList.get (i);
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
	public static double complement(int nPoints, int nPower, Location location, PartialType type) {
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
	public static void complement(Location[] locations, PartialType type, Path out) throws IOException {
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
	public static void createCrossSection(CrossSection cs, PartialType type, Path out) throws IOException {
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
