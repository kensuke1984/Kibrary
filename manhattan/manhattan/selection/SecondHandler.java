package manhattan.selection;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;

import filehandling.sac.SACFileName;
import filehandling.sac.SACHeaderData;
import filehandling.sac.SACHeaderEnum;
import manhattan.template.EventFolder;
import manhattan.template.Utilities;
import parameter.FirstHandler;

/**
 * Filtering for dataset extracted from seed files by {@link FirstHandler}. It
 * is only for observed waveforms. It perhaps should be done before computation
 * for synthetic ones.
 * 
 * 
 * @version 1.1.3
 * 
 * 
 * @author Kensuke
 * 
 */
class SecondHandler extends parameter.SecondHandler implements Consumer<EventFolder> {

	private SecondHandler(Path parameterPath) throws IOException {
		super(parameterPath);
		String date = Utilities.getTemporaryString();
		trashName = "secondHandlerTrash" + date;
		logFileName = "secondHandler" + date + ".log";
	}

	private String trashName;
	private String logFileName;

	@Override
	public void accept(EventFolder eventDir) {
		Path trashDir = eventDir.toPath().resolve(trashName);
		// System.out.println();
		System.out.println(eventDir);
		// + " making trash box (" + trashFile + ")");
		// 観測波形ファイルを拾う
		Set<SACFileName> sacnames = null;
		try {
			sacnames = eventDir.sacFileSet();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}

		try (PrintWriter pw = new PrintWriter(eventDir.toPath().resolve(logFileName).toFile())) {
			for (SACFileName sacName : sacnames) {
				// if the sacName is OK
				boolean isOK = true;
				if (!sacName.isOBS())
					continue;

				if (!sacName.getGlobalCMTID().equals(eventDir.getGlobalCMTID())) {
					pw.println(sacName + " has the invalid eventname:" + sacName.getGlobalCMTID());
					isOK = false;
				}

				// SacFileの読み込み
				SACHeaderData obsSac = sacName.read();
				// System.exit(0);

				// Check the value of B
				double b = obsSac.getValue(SACHeaderEnum.B);
				if (b != 0) {
					isOK = false;
					pw.println(sacName + " has the invalid B:" + b);
				}

				// DELTAのチェック
				double delta = obsSac.getValue(SACHeaderEnum.DELTA);
				if (delta != this.delta) {
					isOK = false;
					pw.println(sacName + " has the invalid DELTA:" + delta);
				}

				// DEPMEN DEPMIN DEPMAXのチェック TODO どうなる？？
				if (checkNaNinDEPMS) {
					boolean depCheck = checkDEP(obsSac);
					if (!depCheck) {
						isOK = false;
						pw.println(sacName + " has some NaNs in DEP???");
					}
				}

				// NPTS のチェック
				if (checkNPTS) {
					int npts = obsSac.getInt(SACHeaderEnum.NPTS);
					if (npts != this.npts) {
						isOK = false;
						pw.println(sacName + " has the invalid NPTS:" + npts);
					}
				}

				// GCARCのチェック
				if (checkGCARC) {
					double gcarc = obsSac.getValue(SACHeaderEnum.GCARC);
					if (gcarc < minGCARC || maxGCARC < gcarc) {
						isOK = false;
						pw.println(sacName + " has the invalid GCARC:" + gcarc);
					}
				} // GCARC

				// station Latitudeのチェック
				if (checkStationLatitude) {
					double latitude = obsSac.getValue(SACHeaderEnum.STLA);
					if (latitude < minStationLatitude || maxStationLatitude < latitude) {
						isOK = false;
						pw.println(sacName + " has the invalid station latitude:" + latitude);
					}
				} // Latitude

				// station Longitudeのチェック
				if (checkStationLongitude) {
					double longitude = obsSac.getValue(SACHeaderEnum.STLO);
					if (longitude < minStationLongitude || maxStationLongitude < longitude) {
						isOK = false;
						pw.println(sacName + " has the invalid station longitude:" + longitude);
					}
				}

				// Event Latitudeのチェック
				if (checkEventLatitude) {
					double latitude = obsSac.getValue(SACHeaderEnum.EVLA);
					if (latitude < minEventLatitude || maxEventLatitude < latitude) {
						isOK = false;
						pw.println(sacName + " has the invalid event latitude:" + latitude);
					}
				} // Latitude

				// Event Longitudeのチェック
				if (checkEventLongitude) {
					double longitude = obsSac.getValue(SACHeaderEnum.EVLO);
					if (longitude < minEventLongitude || maxEventLongitude < longitude) {
						isOK = false;
						pw.println(sacName + " has the invalid event longitude:" + longitude);
					}
				} // Longitude

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

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		SecondHandler s = null;
		if (0 < args.length) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			s = new SecondHandler(parameterPath);
		} else
			s = new SecondHandler(null);

		if (s.maxGCARC <= s.minGCARC)
			throw new RuntimeException(".... minGCARC maxGCARC are invalid");

		System.err.println("SecondHandler is going");
		long elapsedTime = Utilities.runEventProcess(s.workPath, s, 2, TimeUnit.HOURS);
		System.err.println("SecondHandler finished in " + Utilities.toTimeString(elapsedTime));
	}

	/**
	 * @param obsSac
	 *            {@link SACHeader} to check
	 * @return false if depmen depmax depmin has NaN.
	 */
	private static boolean checkDEP(SACHeaderData obsSac) {
		double depmen = obsSac.getValue(SACHeaderEnum.DEPMEN);
		double depmax = obsSac.getValue(SACHeaderEnum.DEPMAX);
		double depmin = obsSac.getValue(SACHeaderEnum.DEPMIN);
		// System.out.println(depmen);
		return !Double.isNaN(depmax) && !Double.isNaN(depmen) && !Double.isNaN(depmin);
	}

}
