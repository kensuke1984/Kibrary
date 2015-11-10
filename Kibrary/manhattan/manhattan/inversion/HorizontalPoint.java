package manhattan.inversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import manhattan.template.HorizontalPosition;

/**
 * 摂動点情報 ポイント名に対するLocation XY?? lat lon
 * 
 * TODO DSM informationとして書き出す
 * 
 * @version 0.1.0
 * @since 2014/4/29 Kepler change done. need check {@link Comparator}
 * 
 * @version 0.1.1
 * @since 2014/10/9 Java 8
 * 
 * @author Kensuke
 * 
 */
public class HorizontalPoint {

	/**
	 * 各ポイントの実際の位置
	 */
	private Map<String, HorizontalPosition> perPointMap;

	/**
	 * ファイル名
	 */
	private File infoFile;

	public Set<String> getHorizontalPointNameSet() {
		return perPointMap.keySet();
	}

	public HorizontalPoint(File infoFile) {
		if (!infoFile.exists()) {
			System.out.println(infoFile + " does not exist.");
			return;
		}
		this.infoFile = infoFile;
		readFile();
	}

	public Map<String, HorizontalPosition> getPerPointMap() {
		return perPointMap;
	}

	/**
	 * @return ポイントファイル名
	 */
	public File getInfoFile() {
		return infoFile;
	}

	private void readFile() {
		// Set<String> perPoint = new TreeSet<String>();
		Map<String, HorizontalPosition> perPointMap = new TreeMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(infoFile))) {
			String line;
			while (null != (line = br.readLine())) {
				line = line.trim();
				// skip when a line starts with #
				// skip when a line does not contain anything
				if (line.startsWith("#") || line.length() == 0)
					continue;
				HorizontalPosition loc = new HorizontalPosition(Double.parseDouble(line.trim().split("\\s+")[1]),
						Double.parseDouble(line.trim().split("\\s+")[2]));
				perPointMap.put(line.trim().split("\\s+")[0], loc);
				// perPoint.add(line.trim().split("\\s+")[0]);
			}
		} catch (Exception e) {
			throw new RuntimeException(infoFile + " is not valid");

		}
		// horizontalPointNameSet = perPoint;
		this.perPointMap = perPointMap;
	}

	/**
	 * @param horizontalPointName
	 *            (ex. XY???)
	 * @return 位置
	 */
	public HorizontalPosition getHorizontalPosition(String horizontalPointName) {
		return perPointMap.get(horizontalPointName);
	}

	/**
	 * @param loc
	 *            for searching the name
	 * @return locationに対するポイントの名前
	 */
	public String toString(HorizontalPosition loc) {
		// HorizontalPosition loc1 = new Location(loc.getLatitude(),
		// loc.getLongitude());
		for (String str : perPointMap.keySet())
			if (loc.equals(perPointMap.get(str)))
				return str;

		System.out.println("could not find the point");

		return null;
	}

	/**
	 * @param position
	 *            target {@link HorizontalPosition}
	 * @return positionに近い順でポイントを返す
	 */
	public String[] getNearestPoints(HorizontalPosition position) {
		return perPointMap.keySet().stream().sorted((o1, o2) -> {
			HorizontalPosition hp1 = perPointMap.get(o1);
			HorizontalPosition hp2 = perPointMap.get(o2);
			double dist1 = hp1.getEpicentralDistance(position);
			double dist2 = hp2.getEpicentralDistance(position);
			// System.out.println(dist1+" "+dist2);
			if (dist1 < dist2)
				return -1;
			else if (dist2 < dist1)
				return 1;
			return 0;

		}).toArray(n -> new String[perPointMap.size()]);
	}

}
