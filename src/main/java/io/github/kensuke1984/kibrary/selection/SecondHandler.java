package io.github.kensuke1984.kibrary.selection;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.firsthandler.FirstHandler;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Filtering for dataset extracted from seed files by {@link FirstHandler}. It
 * is only for observed waveforms. It perhaps should be done before computation
 * for synthetic ones.
 *
 * @author Kensuke Konishi
 * @version 1.2.0.2
 * @author anselme add checks on event depth and networks, and allow multiple NPTS values
 */
public class SecondHandler implements Consumer<EventFolder>, Operation {

	/**
	 * DELTA in SAC
	 */
	protected double delta;
	protected int[] npts;
	private Path workPath;
	private Properties property;
	private Set<String> networks;
	private Predicate<SACData> predicate;
	private String trashName;
	
	public SecondHandler(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
		String date = Utilities.getTemporaryString();
		trashName = "secondHandlerTrash" + date;
	}

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(SecondHandler.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan SecondHandler");
			pw.println("##Path of a working folder (.)");
			pw.println("#workPath");
			pw.println("####If the below values are set, then SecondHandler will check the values");
			pw.println("##double delta of SAC file");
			pw.println("#delta");
			pw.println("##int[] npts");
			pw.println("#npts");
			pw.println("####The below values are in [deg] gcarc [0:180] latitude [-90:90], longitude (0:360]");
			pw.println("#minGCARC");
			pw.println("#maxGCARC");
			pw.println("#minStationLatitude");
			pw.println("#maxStationLatitude");
			pw.println("#minStationLongitude");
			pw.println("#maxStationLongitude");
			pw.println("#minEventLatitude");
			pw.println("#maxEventLatitude");
			pw.println("#minEventLongitude");
			pw.println("#maxEventLongitude");
			pw.println("#minEventDepth");
			pw.println("#maxEventDepth");
			pw.println("#networks");
		}
		System.err.println(outPath + " is created.");
	}

    /**
     * @param args [parameter file name]
     * @throws Exception if any
     */
	public static void main(String[] args) throws Exception {
		SecondHandler s = new SecondHandler(Property.parse(args));
		System.err.println(SecondHandler.class.getName() + " is going");
		long time = System.nanoTime();
		s.run();
		System.err.println(
				SecondHandler.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - time));
	}

    /**
     * @param obsSac {@link SACHeaderData} to check
     * @return false if depmen depmax depmin has NaN.
     */
	private static boolean checkDEP(SACHeaderData obsSac) {
		double depmen = obsSac.getValue(SACHeaderEnum.DEPMEN);
		double depmax = obsSac.getValue(SACHeaderEnum.DEPMAX);
		double depmin = obsSac.getValue(SACHeaderEnum.DEPMIN);
		// System.out.println(depmen);
		return !Double.isNaN(depmax) && !Double.isNaN(depmen) && !Double.isNaN(depmin);
	}

	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
	}
	
	private void set() throws IOException {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));
		if (!Files.exists(workPath)) throw new NoSuchFileException(workPath + " (workPath)");
		predicate = createPredicate();
	}
	
	private Predicate<SACData> createPredicate() {

		double delta = property.containsKey("delta") ? Double.parseDouble(property.getProperty("delta")) : Double.NaN;
		int[] npts = property.containsKey("npts") ? Arrays.stream(property.getProperty("npts").trim().split(" ")).mapToInt(Integer::parseInt).toArray() : new int[0];

		double minGCARC = property.containsKey("minGCARC") ? Double.parseDouble(property.getProperty("minGCARC")) : 0;
		double maxGCARC = property.containsKey("maxGCARC") ? Double.parseDouble(property.getProperty("maxGCARC")) : 180;
		
		double minStationLatitude = property.containsKey("minStationLatitude")
				? Double.parseDouble(property.getProperty("minStationLatitude")) : -90;
		double maxStationLatitude = property.containsKey("maxStationLatitude")
				? Double.parseDouble(property.getProperty("maxStationLatitude")) : 90;

		double minEventLatitude = property.containsKey("minEventLatitude")
				? Double.parseDouble(property.getProperty("minEventLatitude")) : -90;
		double maxEventLatitude = property.containsKey("maxEventLatitude")
				? Double.parseDouble(property.getProperty("maxEventLatitude")) : 90;

		double minStationLongitude = property.containsKey("minStationLongitude")
				? Double.parseDouble(property.getProperty("minStationLongitude")) : 0;
		double maxStationLongitude = property.containsKey("maxStationLongitude")
				? Double.parseDouble(property.getProperty("maxStationLongitude")) : 360;

		double minEventLongitude = property.containsKey("minEventLongitude")
				? Double.parseDouble(property.getProperty("minEventLongitude")) : 0;
		double maxEventLongitude = property.containsKey("maxEventLongitude")
				? Double.parseDouble(property.getProperty("maxEventLongitude")) : 360;
				
		double maxEventDepth = property.containsKey("maxEventDepth") ?
				Double.parseDouble(property.getProperty("maxEventDepth")) : 800;
		double minEventDepth = property.containsKey("minEventDepth") ?
				Double.parseDouble(property.getProperty("minEventDepth")) : 0;
				
		Set<String> networks = property.containsKey("networks") ? 
				Stream.of(property.getProperty("networks").trim().split("\\s+")).collect(Collectors.toSet()) : null;
	
		return obsSac -> {
			// Check the value of B
			if (obsSac.getValue(SACHeaderEnum.B) != 0) return false;

			// DELTAのチェック
			if (!Double.isNaN(delta) && delta != obsSac.getValue(SACHeaderEnum.DELTA)) return false;

			// If DEPMEN, DEPMIN or DEPMAX has NAN
			if (!checkDEP(obsSac)) return false;

			// NPTS
			if (npts.length != 0) {
				boolean res = false;
				for (int n : npts) {
					if (obsSac.getInt(SACHeaderEnum.NPTS) == n) res = true;
				}
				if (!res) return false;
			}

			// GCARC
			double gcarc = obsSac.getValue(SACHeaderEnum.GCARC);
			if (gcarc < minGCARC || maxGCARC < gcarc) return false;

			// station Latitude
			double stationLatitude = obsSac.getValue(SACHeaderEnum.STLA);
			if (stationLatitude < minStationLatitude || maxStationLatitude < stationLatitude) return false;

			// station Longitude
			double stationLongitude = obsSac.getValue(SACHeaderEnum.STLO);
			if (stationLongitude < 0) stationLongitude = stationLongitude + 360.;
			if (stationLongitude < minStationLongitude || maxStationLongitude < stationLongitude) return false;

			// Event Latitude
			double eventLatitude = obsSac.getValue(SACHeaderEnum.EVLA);
			if (eventLatitude < minEventLatitude || maxEventLatitude < eventLatitude) return false;

			// Event Longitude
			double eventLongitude = obsSac.getValue(SACHeaderEnum.EVLO);
			if (eventLongitude < 0) eventLongitude += 360;
			if (eventLongitude < minEventLongitude || maxEventLongitude < eventLongitude) return false;
			
			// Event Depth
			double eventDepth = obsSac.getValue(SACHeaderEnum.EVDP);
			if (eventDepth > maxEventDepth || eventDepth < minEventDepth) return false;
			
			// Network
			if (networks != null) {
				String network = obsSac.getSACString(SACHeaderEnum.KNETWK);
				if (!networks.contains(network)) return false;
			}
			
			return true;
		};
	}
	
	@Override
	public void accept(EventFolder eventDir) {
		Path trashDir = eventDir.toPath().resolve(trashName);
		System.out.println(eventDir);
		// + " making trash box (" + trashFile + ")");
		// 観測波形ファイルを拾う
		Set<SACFileName> sacnames;
		try {
			sacnames = eventDir.sacFileSet();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}

		try {
			for (SACFileName sacName : sacnames) {
				// if the sacName is OK
				boolean isOK = true;
				if (!sacName.isOBS())
					continue;

				if (!sacName.getGlobalCMTID().equals(eventDir.getGlobalCMTID()))
					isOK = false;
				//TODO isok????
				// SacFileの読み込み
				SACData obsSac = sacName.read();
				isOK = predicate.test(obsSac);

				if (!isOK)
					try {
						FileUtils.moveFileToDirectory(sacName, trashDir.toFile(), true);
					} catch (Exception e) {
						e.printStackTrace();
					}
			} // obsfile
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

	@Override
	public void run() throws Exception {
		Utilities.runEventProcess(workPath, this, 2, TimeUnit.HOURS);
	}

}
