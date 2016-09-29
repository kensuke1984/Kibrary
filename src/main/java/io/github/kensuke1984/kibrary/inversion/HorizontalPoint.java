package io.github.kensuke1984.kibrary.inversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.NoSuchFileException;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;

/**
 * 
 * 
 * 摂動点情報 ポイント名に対するLocation XY?? lat lon
 * 
 * TODO DSM informationとして書き出す
 * 
 * 
 * @version 0.1.2.1
 * 
 * @author Kensuke Konishi
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

	public HorizontalPoint(File infoFile) throws NoSuchFileException {
		if (!infoFile.exists())
			throw new NoSuchFileException(infoFile.getPath());
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
		for (String str : perPointMap.keySet())
			if (loc.equals(perPointMap.get(str)))
				return str;
		System.err.println("could not find the point");
		return null;
	}

	/**
	 * @param position
	 *            target {@link HorizontalPosition}
	 * @return positionに近い順でポイントを返す
	 */
	public String[] getNearestPoints(HorizontalPosition position) {
		return perPointMap.keySet().stream()
				.sorted(Comparator.comparingDouble(o -> perPointMap.get(o).getEpicentralDistance(position)))
				.toArray(String[]::new);
	}
}
