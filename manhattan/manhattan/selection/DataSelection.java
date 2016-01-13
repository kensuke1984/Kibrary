package manhattan.selection;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import filehandling.sac.SACComponent;
import filehandling.sac.SACData;
import filehandling.sac.SACExtension;
import filehandling.sac.SACFileName;
import filehandling.sac.SACHeaderEnum;
import manhattan.datacorrection.StaticCorrection;
import manhattan.datacorrection.StaticCorrectionFile;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.EventFolder;
import manhattan.template.Station;
import manhattan.template.Trace;
import manhattan.template.Utilities;
import manhattan.timewindow.Timewindow;
import manhattan.timewindow.TimewindowInformation;
import manhattan.timewindow.TimewindowInformationFile;

/**
 * 
 * Information: {@link parameter.DataSelection} 理論波形と観測波形の比較から使えるものを選択する。<br>
 * workDir以下にあるイベントディレクトリの中から選ぶ<br>
 * 振幅比、correlation、variance<br>
 * 
 * {@link TimewindowInformationFile} necessary.
 * 
 * @version 0.0.8
 * 
 * 
 * @author Kensuke
 * 
 */
class DataSelection extends parameter.DataSelection {

	private Set<EventFolder> eventDirs;

	private String dateStr;

	private DataSelection(Path parameterPath) throws IOException {
		super(parameterPath);
		staticCorrectionSet = staticCorrectionInformationPath == null ? Collections.emptySet()
				: StaticCorrectionFile.read(staticCorrectionInformationPath);
		// eventDirs = WorkingDirectory.listEventDirs(workDir);
		eventDirs = Utilities.eventFolderSet(obsPath);
		sourceTimewindowInformationSet = TimewindowInformationFile.read(timewindowInformationPath);
		dateStr = Utilities.getTemporaryString();
		outputGoodWindowPath = workPath.resolve("selectedTimewindow" + dateStr + ".dat");
		goodTimewindowInformationSet = Collections.synchronizedSet(new HashSet<>());
	}

	private Set<TimewindowInformation> sourceTimewindowInformationSet;
	private Set<TimewindowInformation> goodTimewindowInformationSet;

	private Path outputGoodWindowPath;

	private Set<StaticCorrection> staticCorrectionSet;

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 *             if an I/O happens
	 */
	public static void main(String[] args) throws IOException {
		DataSelection ds = null;
		if (0 < args.length) {
			Path path = Paths.get(args[0]);
			if (!Files.exists(path))
				throw new NoSuchFileException(args[0]);
			ds = new DataSelection(path);
		} else
			ds = new DataSelection(null);
		long start = System.nanoTime();
		System.err.println("DataSelection is going");
		int N_THREADS = Runtime.getRuntime().availableProcessors();

		ExecutorService exec = Executors.newFixedThreadPool(N_THREADS);

		for (EventFolder eventDirectory : ds.eventDirs)
			exec.execute(ds.new Worker(eventDirectory));

		exec.shutdown();
		while (!exec.isTerminated())
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}

		System.out.println();
		ds.output();
		System.err.println("DataSelection is done in " + Utilities.toTimeString(System.nanoTime() - start));
	}
	
	
	
	
	

	private void output() throws IOException {
		TimewindowInformationFile.write(goodTimewindowInformationSet, outputGoodWindowPath);
	}

	private class Worker implements Runnable {

		private Set<SACFileName> obsFiles;
		private EventFolder obsEventDirectory;
		private EventFolder synEventDirectory;

		private GlobalCMTID id;

		private Worker(EventFolder ed) throws IOException {
			this.obsEventDirectory = ed;
			obsFiles = ed.sacFileSet(sfn -> !sfn.isOBS());
			id = ed.getGlobalCMTID();
			synEventDirectory = new EventFolder(DataSelection.super.synPath.resolve(ed.getName()));
			if (!synEventDirectory.exists())
				return;
		}

		@Override
		public void run() {
			if (!synEventDirectory.exists()) {
				try {
					FileUtils.moveDirectoryToDirectory(obsEventDirectory, workPath.resolve("withoutSyn").toFile(),
							true);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return;
			}

			// System.out.println("Checking " + obsEventDirectory);
			// System.out.println(obsEventDirectory + " checked all observeds");
			try (PrintWriter lpw = new PrintWriter(
					Files.newBufferedWriter(obsEventDirectory.toPath().resolve("stationList" + dateStr + ".txt"),
							StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
				// all the observed files
				if (isConvolved)
					lpw.println("#convolved");
				else
					lpw.println("#not convolved");
				lpw.println("#s e c use ratio(syn/obs){abs max min} variance correlation");

				for (SACFileName obsName : obsFiles) {
					// check components
					if (!components.contains(obsName.getComponent()))
						continue;
					String stationName = obsName.getStationName();
					SACComponent component = obsName.getComponent();
					// double timeshift = 0;
					SACExtension synExt = isConvolved ? SACExtension.valueOfConvolutedSynthetic(component)
							: SACExtension.valueOfSynthetic(component);

					SACFileName synName = new SACFileName(synEventDirectory, stationName + "." + id + "." + synExt);
					if (!synName.exists())
						continue;

					// synthetic sac
					SACData obsSac = obsName.read();
					SACData synSac = synName.read();

					Station station = obsSac.getStation();
					//
					if (synSac.getValue(SACHeaderEnum.DELTA) != obsSac.getValue(SACHeaderEnum.DELTA))
						continue;

					// Pickup a time window of obsName
					Set<TimewindowInformation> windowInformations = sourceTimewindowInformationSet
							.stream().filter(info -> info.getStation().equals(station)
									&& info.getGlobalCMTID().equals(id) && info.getComponent() == component)
							.collect(Collectors.toSet());

					if (windowInformations.isEmpty())
						continue;

					for (TimewindowInformation window : windowInformations) {
						RealVector synU = cutSAC(synSac, window);
						RealVector obsU = cutSAC(obsSac, shift(window));
						if (check(lpw, stationName, id, component, window, obsU, synU))
							goodTimewindowInformationSet.add(window);
					}
					// lpw.close();
				}
				// spw.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("error on " + obsEventDirectory);
			}
			System.out.print(".");
			// System.exit(0);
			// System.out.println(obsEventDirectory + " is done");
		}
	}

	/**
	 * ID for static correction and time window information Default is station
	 * name, global CMT id, component.
	 */
	private BiPredicate<StaticCorrection, TimewindowInformation> isPair = (s,
			t) -> s.getStation().equals(t.getStation()) && s.getGlobalCMTID().equals(t.getGlobalCMTID())
					&& s.getComponent() == t.getComponent();

	private StaticCorrection getStaticCorrection(TimewindowInformation window) {
		return staticCorrectionSet.stream().filter(s -> isPair.test(s, window)).findAny().get();
	}

	/**
	 * @param timewindow
	 *            timewindow to shift
	 * @return if there is time shift information for the input timewindow, then
	 *         creates new timewindow and returns it, otherwise, just returns
	 *         the input one.
	 */
	private TimewindowInformation shift(TimewindowInformation timewindow) {
		if (staticCorrectionSet.isEmpty())
			return timewindow;
		StaticCorrection foundShift = getStaticCorrection(timewindow);
		double value = foundShift.getTimeshift();
		return new TimewindowInformation(timewindow.getStartTime() - value, timewindow.getEndTime() - value,
				foundShift.getStation(), foundShift.getGlobalCMTID(), foundShift.getComponent());
	}

	private boolean check(PrintWriter writer, String stationName, GlobalCMTID id, SACComponent component,
			TimewindowInformation window, RealVector obsU, RealVector synU) throws IOException {
		if (obsU.getDimension() < synU.getDimension())
			synU = synU.getSubVector(0, obsU.getDimension() - 1);
		else if (synU.getDimension() < obsU.getDimension())
			obsU = obsU.getSubVector(0, synU.getDimension() - 1);

		// check
		double synMax = synU.getMaxValue();
		double synMin = synU.getMinValue();
		double obsMax = obsU.getMaxValue();
		double obsMin = obsU.getMinValue();
		double obs2 = obsU.dotProduct(obsU);
		double syn2 = synU.dotProduct(synU);
		double cor = obsU.dotProduct(synU);
		double var = obs2 + syn2 - 2 * cor;
		double maxRatio = Math.round(synMax / obsMax * 100) / 100.0;
		double minRatio = Math.round(synMin / obsMin * 100) / 100.0;
		double ampRatio = (-synMin < synMax ? synMax : -synMin) / (-obsMin < obsMax ? obsMax : -obsMin);
		var /= obs2;
		cor /= Math.sqrt(obs2 * syn2);

		ampRatio = Math.round(ampRatio * 100) / 100.0;
		var = Math.round(var * 100) / 100.0;
		cor = Math.round(cor * 100) / 100.0;
		// if (minRatio > ratio || minRatio < 1 / ratio ||
		boolean isok = !(ratio < minRatio || minRatio < 1 / ratio || ratio < maxRatio || maxRatio < 1 / ratio
				|| ratio < ampRatio || ampRatio < 1 / ratio || cor < minCorrelation || maxCorrelation < cor
				|| var < minVariance || maxVariance < var);

		writer.println(stationName + " " + id + " " + component + " " + isok + " " + ratio + " " + maxRatio + " "
				+ minRatio + " " + var + " " + cor);
		return isok;
	}

	/**
	 * @param sac
	 *            {@link SACFile} to cut
	 * @param tStart
	 *            starting time of window
	 * @param tEnd
	 *            end time of window
	 * @return new Trace for the timewindow [tStart:tEnd]
	 */
	private static RealVector cutSAC(SACData sac, Timewindow timeWindow) {
		Trace trace = sac.createTrace();
		double tStart = timeWindow.getStartTime();
		double tEnd = timeWindow.getEndTime();
		return new ArrayRealVector(trace.cutWindow(tStart, tEnd).getY());
	}
}
