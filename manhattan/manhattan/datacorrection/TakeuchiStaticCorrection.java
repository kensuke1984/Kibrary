/**
 * 
 */
package manhattan.datacorrection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import filehandling.sac.SACComponent;
import filehandling.sac.SACData;
import filehandling.sac.SACFileName;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.Trace;
import manhattan.template.Utilities;
import manhattan.timewindow.TimewindowInformation;
import manhattan.timewindow.TimewindowInformationFile;

/**
 * 
 * Maker of static correction suggested by Nozomu Takeuchi. It seeks up-and-down
 * two peaks in given {@link TimewindowInformation} for each path.
 * 
 * Values of the correction is by the average time of arrivals and amplitudes of
 * those peaks.
 * 
 * synthetic のウインドウが[t1, t2]の時 値 t の場合 [t1-t, t2-t]を用いる<br>
 * 
 * Start time for identification is a start time in the given
 * {@link TimewindowInformationFile}.
 * 
 * @author kensuke
 * @since 2015/08/07
 * @version 0.0.1
 * 
 * @version 0.0.1.1
 * @since 2015/8/14
 * 
 * @version 0.0.2
 * @since 2015/9/14 Timewindow information
 * 
 */
final class TakeuchiStaticCorrection extends parameter.TakeuchiStaticCorrection {
	private TakeuchiStaticCorrection(Path parameterPath) throws IOException {
		super(parameterPath);
		String date = Utilities.getTemporaryString();
		if (!Files.exists(timeWindowInformationPath))
			throw new NoSuchFileException(timeWindowInformationPath.toString());
		timewindow = TimewindowInformationFile.read(timeWindowInformationPath);
		outStaticCorrectionPath = workPath.resolve("takeuchiCorrection" + date + ".dat");
		outStaticCorrectionSet = Collections.synchronizedSet(new HashSet<>());
	}

	private Set<StaticCorrection> outStaticCorrectionSet;
	private Path outStaticCorrectionPath;
	private Set<TimewindowInformation> timewindow;

	/**
	 * @param args
	 *            [parameter file name]
	 */
	public static void main(String[] args) throws IOException {
		TakeuchiStaticCorrection tsm = null;
		if (0 < args.length) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			tsm = new TakeuchiStaticCorrection(parameterPath);
		} else
			tsm = new TakeuchiStaticCorrection(null);
		long time = System.nanoTime();
		System.err.println("TakeuchiStaticCorrection is going.");
		tsm.run();

		System.out.println("TakeuchiStaticCorrection finished in " + Utilities.toTimeString(System.nanoTime() - time));
	}

	private void run() throws IOException {
		Set<SACFileName> nameSet = null;
		try {
			nameSet = Utilities.sacFileNameSet(obsPath);
		} catch (Exception e3) {
			throw new RuntimeException(obsPath + " may have problems");
		}
		nameSet.parallelStream().filter(name -> components.contains(name.getComponent())).forEach(this::compare);
		StaticCorrectionFile.write(outStaticCorrectionSet, outStaticCorrectionPath);
	}

	private void compare(SACFileName obsName, SACFileName synName) throws IOException {
		String stationName = obsName.getStationName();
		GlobalCMTID id = obsName.getGlobalCMTID();
		SACComponent component = obsName.getComponent();
		Set<TimewindowInformation> timeWindowSet = timewindow.stream()
				.filter(info -> info.getStationName().equals(stationName))
				.filter(info -> info.getGlobalCMTID().equals(id)).filter(info -> info.getComponent() == component)
				.collect(Collectors.toSet());
		if (timeWindowSet.size() != 1)
			throw new RuntimeException(timeWindowInformationPath + " is invalid.");
		TimewindowInformation timeWindow = timeWindowSet.iterator().next();
		SACData obsSac = obsName.read();
		SACData synSac = synName.read();
		Trace obsTrace = obsSac.createTrace().cutWindow(timeWindow);
		Trace synTrace = synSac.createTrace().cutWindow(timeWindow);
		double obsT = (obsTrace.getXforMaxValue() + obsTrace.getXforMinValue()) / 2;
		double synT = (synTrace.getXforMaxValue() + synTrace.getXforMinValue()) / 2;
		double timeShift = synT - obsT;
		double obsAmp = (obsTrace.getMaxValue() - obsTrace.getMinValue()) / 2;
		double synAmp = (synTrace.getMaxValue() - synTrace.getMinValue()) / 2;
		double amplitudeRatio = obsAmp / synAmp;
		StaticCorrection sc = new StaticCorrection(stationName, id, component, timeWindow.getStartTime(), timeShift,
				amplitudeRatio);
		outStaticCorrectionSet.add(sc);
	}

	private void compare(SACFileName obsSacFileName) {
		try {
			compare(obsSacFileName, getPair(obsSacFileName));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private SACFileName getPair(SACFileName obsSacFileName) {
		String ext = obsSacFileName.getComponent() + (isConvolved ? "sc" : "s");
		String id = obsSacFileName.getGlobalCMTID().toString();
		String name = obsSacFileName.getStationName() + '.' + id + '.' + ext;
		return new SACFileName(synPath.resolve(id + "/" + name));
	}
}
