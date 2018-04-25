package io.github.kensuke1984.kibrary.inversion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;

import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.Location;

/**
 * 
 * 深さを含んだ実際にインバージョンで求める摂動点情報
 * 
 * dr dlatitude dlongitude で dVを作る
 * 
 * TODO 名前のチェック validity
 * 
 * @version 0.1.2.2
 * 
 * @author Kensuke Konishi
 * 
 */
public class PerturbationPoint extends HorizontalPoint {

	private double dR;

	private double dLatitude;

	private double dLongitude;

	private Map<Location, Double> volumeMap;

	private class VolumeCalculator implements Runnable {

		private Location loc;

		private VolumeCalculator(Location loc) {
			this.loc = loc;
		}

		@Override
		public void run() {
			volumeMap.put(loc, Earth.getVolume(loc, dR, dLatitude, dLongitude));
			System.out.println(loc);
		}

	}

	public void printVolumes() {
		for (Location perLoc : perturbationLocation)
			System.out.println(perLoc + " " + volumeMap.get(perLoc));
	}

	/**
	 * 各点の体積を計算する
	 */
	public void computeVolumes() {
		volumeMap = new HashMap<>();
		ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		for (Location perLoc : perturbationLocation)
			es.execute(new VolumeCalculator(perLoc));

		es.shutdown();
		while (!es.isTerminated()) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	// TODO
	public void createUnknownParameterSetFile(File outFile) throws FileAlreadyExistsException {
		if (outFile.exists())
			throw new FileAlreadyExistsException(outFile.getPath());

		computeVolumes();

		try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outFile)))) {
			for (int i = 0; i < perturbationLocation.length; i++) {
				Location loc = perturbationLocation[i];
				System.out.println(loc);
				double volume = volumeMap.get(loc);
				pw.println("MU " + loc + " " + volume);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setDr(double dr) {
		dR = dr;
	}

	public void setDlatitude(double dlatitude) {
        dLatitude = dlatitude;
	}

	public void setDlongitude(double dlongitude) {
        dLongitude = dlongitude;
	}

	public Map<Location, Double> getVolumeMap() {
		return volumeMap;
	}

	/**
	 * （インバージョンに使う）摂動点の情報（中枢） 順番も保持
	 */
	private Location[] perturbationLocation;

	/**
	 * ポイント数
	 */
	private int pointN;

	/**
	 * i番目の摂動点の名前 （
	 */
	private String[] pointName;

	/**
	 * perturbation point file <br>
	 * 
	 * ex) XY??? r
	 * 
	 */
	private File perturbationPointFile;

	/**
	 * @param args
	 *            dir dR dLatitude dLongitude
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 4) {
			System.out.println("dir dR dLatitude dLongitude");
			return;
		}
		File dir = new File(args[0]);
		PerturbationPoint pp = new PerturbationPoint(new File(dir, "horizontalPoint.inf"),
				new File(dir, "perturbationPoint.inf"));
		pp.dR = Double.parseDouble(args[1]);
		pp.dLatitude = Double.parseDouble(args[2]);
		pp.dLongitude = Double.parseDouble(args[3]);
		pp.createUnknownParameterSetFile(new File(dir, "unknown.inf"));
		// System.out.println(pp.perturbationLocation[0]);
	}

	public String[] getPointName() {
		return pointName;
	}

	public File getPerturbationPointFile() {
		return perturbationPointFile;
	}

	/**
	 * 
	 * 全てのr、水平分布に対しての摂動点情報のperturbationInfoファイルを書く
	 * 
	 * @param r
	 *            array of radius
	 * @param horizontalInfo
	 *            file for {@link HorizontalPoint}
	 * @param out
	 *            for output
	 * @throws NoSuchFileException if any
	 */
	public static void createPerturbationPoint(double[] r, File horizontalInfo, File out) throws NoSuchFileException {
		HorizontalPoint hp = new HorizontalPoint(horizontalInfo);
		Set<String> pointNameSet = hp.getHorizontalPointNameSet();
		// Map<String, HorizontalPosition> pointMap = hp.getPerPointMap();
		try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(out)))) {
			for (String name : pointNameSet)
				for (double aR : r) pw.println(name + " " + aR);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param horizontalPointFile
	 *            {@link File} for {@link HorizontalPoint}
	 * @param perturbationPointFile
	 *            {@link File} for link PerturbationPoint}
	 * @throws IOException 
	 */
	public PerturbationPoint(File horizontalPointFile, File perturbationPointFile) throws IOException {
		super(horizontalPointFile);
		this.perturbationPointFile = perturbationPointFile;
		readPerturbationPointFile();
//		readPartialLocation(perturbationPointFile.toPath());
	}

	/**
	 * @return array of radius
	 */
	public double[] getR() {
		return Arrays.stream(perturbationLocation).mapToDouble(Location::getR).toArray();
	}

	/**
	 * 
	 * @param i
	 *            index
	 * @return i番目の深さ
	 */
	public double getR(int i) {
		return perturbationLocation[i].getR();
	}

	/**
	 * perturbation point file を読み込む<br>
	 * XY??? r1 XY??? r2 .....
	 * 
	 */
	private void readPerturbationPointFile() {
		try {
			List<String> lines = FileUtils.readLines(perturbationPointFile, Charset.defaultCharset());
			lines.removeIf(line -> line.trim().isEmpty() || line.trim().startsWith("#"));
			
			pointN = lines.size();
			perturbationLocation = new Location[pointN];
			pointName = new String[pointN];

			for (int i = 0; i < pointN; i++) {
				String[] parts = lines.get(i).split("\\s+");
				pointName[i] = parts[0];
				double r = Double.parseDouble(parts[1]);
				perturbationLocation[i] = new Location(getHorizontalPosition(pointName[i]).getLatitude(),
						getHorizontalPosition(pointName[i]).getLongitude(), r);
				// perturbationLocation[i].setR(r);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static Location toLocation(String line) {
		String[] parts = line.split("\\s+");
		if (Double.parseDouble(parts[1]) > 180.0){
//			System.out.println(Double.parseDouble(parts[2]));
		return new Location(Double.parseDouble(parts[0]),
				Double.parseDouble(parts[1])-360.0, Double.parseDouble(parts[2]));
		} else 
//			System.out.println(Double.parseDouble(parts[2]));
			return new Location(Double.parseDouble(parts[0]),
					Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
	}

	/**
	 * @param parPath
	 *            unknown file (MU lat lon radius volume)
	 * @return
	 * @throws IOException 
	 */
	private void readPartialLocation(Path parPath) throws IOException {
//		System.out.println(parPath);
		List<String> lines = FileUtils.readLines(parPath.toFile(), Charset.defaultCharset());
		lines.removeIf(line -> line.trim().isEmpty() || line.trim().startsWith("#"));
		
		pointN = lines.size();
		perturbationLocation = new Location[pointN];
		try {
			perturbationLocation = Files.readAllLines(parPath).stream()
					.map(PerturbationPoint::toLocation)
					.toArray(n -> new Location[n]);
		} catch (Exception e) {
			throw new RuntimeException("par file has problems");
		}

	}

	public Location[] getPerturbationLocation() {
		return perturbationLocation;
	}

	public int getPointN() {
		return pointN;
	}

	/**
	 * @param location
	 *            {@link Location} for target
	 * @return location に近い順でポイントのLocationを返す
	 */
	public Location[] getNearestLocation(Location location) {
		Location[] locations = Arrays.copyOf(perturbationLocation, perturbationLocation.length);
		Arrays.sort(locations, Comparator.comparingDouble(o -> o.getDistance(location)));
		return locations;
	}

}
