package io.github.kensuke1984.kibrary.inversion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;

import io.github.kensuke1984.kibrary.util.Location;

/**
 * 
 * 深さを含んだ実際にインバージョンで求める摂動点情報
 * 
 * dr dlatitude dlongitude で dVを作る
 * 
 * TODO 名前のチェック validity
 * 
 * @version 0.1.2.1
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
			volumeMap.put(loc, GridMaker.getVolume(loc, dR, dLatitude, dLongitude));
			System.out.println(loc);
		}

	}

	public void printVolumes() {
		for (int i = 0; i < perturbationLocation.length; i++)
			System.out.println(perturbationLocation[i] + " " + volumeMap.get(perturbationLocation[i]));
	}

	/**
	 * 各点の体積を計算する
	 */
	public void computeVolumes() {
		volumeMap = new HashMap<>();
		ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		for (int i = 0; i < perturbationLocation.length; i++) {
			VolumeCalculator vc = new VolumeCalculator(perturbationLocation[i]);
			es.execute(vc);
		}
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

		try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));) {
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
		this.dR = dr;
	}

	public void setDlatitude(double dlatitude) {
		this.dLatitude = dlatitude;
	}

	public void setDlongitude(double dlongitude) {
		this.dLongitude = dlongitude;
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
	 * @throws FileAlreadyExistsException
	 * @throws NoSuchFileException
	 */
	public static void main(String[] args) throws FileAlreadyExistsException, NoSuchFileException {
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
	 * @throws NoSuchFileException
	 */
	public static void createPerturbationPoint(double[] r, File horizontalInfo, File out) throws NoSuchFileException {
		HorizontalPoint hp = new HorizontalPoint(horizontalInfo);
		Set<String> pointNameSet = hp.getHorizontalPointNameSet();
		// Map<String, HorizontalPosition> pointMap = hp.getPerPointMap();
		try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(out)))) {
			for (String name : pointNameSet)
				for (int ir = 0; ir < r.length; ir++)
					pw.println(name + " " + r[ir]);
		} catch (Exception e) {
		}

	}

	/**
	 * @param horizontalPointFile
	 *            {@link File} for {@link HorizontalPoint}
	 * @param perturbationPointFile
	 *            {@link File} for link PerturbationPoint}
	 * @throws NoSuchFileException
	 */
	public PerturbationPoint(File horizontalPointFile, File perturbationPointFile) throws NoSuchFileException {
		super(horizontalPointFile);
		this.perturbationPointFile = perturbationPointFile;
		readPerturbationPointFile();
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
			lines.removeIf(line -> line.trim().length() == 0 || line.trim().startsWith("#"));
			
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
		final Location location0 = location;
		Location[] locations = Arrays.copyOf(perturbationLocation, perturbationLocation.length);
		Arrays.sort(locations, (o1, o2) -> {
			return Double.compare(o1.getDistance(location0), o2.getDistance(location0));
		});
		return locations;

	}

}
