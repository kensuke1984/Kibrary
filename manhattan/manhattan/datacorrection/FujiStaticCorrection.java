package manhattan.datacorrection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import filehandling.sac.SACComponent;
import filehandling.sac.SACData;
import filehandling.sac.SACExtension;
import filehandling.sac.SACFileName;
import filehandling.sac.SACHeaderEnum;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.EventFolder;
import manhattan.template.Station;
import manhattan.template.Trace;
import manhattan.template.Utilities;
import manhattan.timewindow.Timewindow;
import manhattan.timewindow.TimewindowInformation;
import manhattan.timewindow.TimewindowInformationFile;

/**
 * This class computes values of Static correction after Fuji <i>et al</i>.,
 * (2010) <br>
 * dataselectionした後のフォルダでやる<br>
 * 
 * The time shift value <i>t</i> for the ray path is for the observed
 * timewindow.<br>
 * (i.e. synthetic window [t1, t2], observed [t1-t, t2-t])
 * 
 * The time shift values are computed as follows:
 * <blockquote>ワーキングディレクトリ以下のイベントたちの中にいく<br>
 * 理論波形のstartMkからendMkまでに間で最大ピークを探し冨士さんの 感じで探す とりあえず±sLimit秒で探してみる <br>
 * 観測波形にマーカーがない場合書いてしまう <br>
 * マーカーはrenewパラメタがtrueなら観測波形のマーカーは上書き<br>
 * time shiftの値は小数点２位以下切捨て
 * Algorithm startMkからendMkまでの間で最大振幅を取り、
 * それ以前の時間からthreshold（最大振幅ベース）を超えた振幅の一番早いものをえらびstartMkから、
 * そこまでのタイムウインドウのコリレーションをあわせる <br>
 * </blockquote>
 * 
 * If something happens, move the sac file to Trash
 * 
 * timeshift fileを一つに統一
 * 
 * 
 * @version 0.1.3
 * @author Kensuke
 * 
 */
final class FujiStaticCorrection extends parameter.FujiStaticCorrection {

	private Set<StaticCorrection> staticCorrectionSet;

	private class Worker implements Runnable {
		private EventFolder obsEventDir;

		private Path synEventPath;

		private GlobalCMTID eventID;

		private Worker(EventFolder eventDirectory) {
			obsEventDir = eventDirectory;
			eventID = obsEventDir.getGlobalCMTID();
			synEventPath = synPath.resolve(eventID.toString());
		}

		@Override
		public void run() {
			if (!Files.exists(synEventPath)) {
				new NoSuchFileException(synEventPath.toString()).printStackTrace();
				return;
			}

			// observed fileを拾ってくる
			Set<SACFileName> obsFiles = null;
			try {
				obsFiles = obsEventDir.sacFileSet(h -> !h.isOBS());
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
			// TreeMap<String, Double> timeshiftMap = new TreeMap<>();
			for (SACFileName obsName : obsFiles) {
				SACComponent component = obsName.getComponent();
				// check a component
				if (!components.contains(component))
					continue;
				SACExtension synExt = isConvolved ? SACExtension.valueOfConvolutedSynthetic(component)
						: SACExtension.valueOfSynthetic(component);

				SACFileName synName = new SACFileName(
						synEventPath.resolve(obsName.getStationName() + "." + obsName.getGlobalCMTID() + "." + synExt));
				// System.out.println(obsFile.getName() + " " +
				// synFile.getName());
				if (!synName.exists()) {
					System.out.println(synName + " does not exist. ");
					continue;
				}
				SACData obsSac = null;
				SACData synSac = null;
				try {
					obsSac = obsName.read();
					synSac = synName.read();

				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}

				Station station = obsSac.getStation();
				double delta = 1 / sacSamplingHz;
				if (delta != obsSac.getValue(SACHeaderEnum.DELTA) || delta != synSac.getValue(SACHeaderEnum.DELTA)) {
					System.out.println("Deltas are invalid. " + obsSac + " " + obsSac.getValue(SACHeaderEnum.DELTA)
							+ " " + synSac + " " + synSac.getValue(SACHeaderEnum.DELTA) + " must be " + delta);
					continue;
				}
				// Pickup time windows of obsName
				Set<TimewindowInformation> windows = timewindowInformation.stream()
						.filter(info -> info.getStation().equals(station))
						.filter(info -> info.getGlobalCMTID().equals(eventID))
						.filter(info -> info.getComponent() == component).collect(Collectors.toSet());

				if (windows != null && windows.size() != 0)
					for (Timewindow window : windows)
						try {
							double shift = computeTimeshiftForBestCorrelation(obsSac, synSac, window);
							double ratio = computeMaxRatio(obsSac, synSac, shift, window);
							StaticCorrection t = new StaticCorrection(station, eventID, component,
									window.getStartTime(), shift, ratio);
							staticCorrectionSet.add(t);
						} catch (Exception e) {
							System.err.println(window + " is ignored because an error occurs");
							e.printStackTrace();
						}

			}
		}
	}

	private FujiStaticCorrection(Path parameterPath) throws IOException {
		super(parameterPath);
		String date = Utilities.getTemporaryString();
		outPath = workPath.resolve("staticCorrection" + date + ".dat");
		// searchWidth = (int) (searchRange * sacSamplingHz);
		staticCorrectionSet = Collections.synchronizedSet(new HashSet<>());

	}

	private Path outPath;
	private Set<TimewindowInformation> timewindowInformation;

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// test();
		// args = new String[] { "data/carib/timeshift.prm" };
		FujiStaticCorrection tsm = null;
		if (0 < args.length) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			tsm = new FujiStaticCorrection(parameterPath);
		} else
			tsm = new FujiStaticCorrection(null);
		FujiStaticCorrection f = tsm;
		System.err.println("FujiStaticCorrection is going.");
		long startTime = System.nanoTime();
		Set<EventFolder> eventDirs = Utilities.eventFolderSet(tsm.obsPath);
		int n = Runtime.getRuntime().availableProcessors();
		ExecutorService es = Executors.newFixedThreadPool(n);
		tsm.timewindowInformation = TimewindowInformationFile.read(tsm.timewindowInformationPath);
		eventDirs.stream().map(ed -> f.new Worker(ed)).forEach(es::execute);
		es.shutdown();

		while (!es.isTerminated()) {
			try {
				// System.out.println("waiting");
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		f.output();
		System.err.println("FujiStaticCorrection finished in " + Utilities.toTimeString(System.nanoTime() - startTime));

	}

	private void output() throws IOException {
		StaticCorrectionFile.write(staticCorrectionSet, outPath);
	}

	/**
	 * Search the max point of synthetic in the time window for a pair. and then
	 * search the max value within the search range and same positive and
	 * negative. Relative ratio of synthetic is 1;
	 * 
	 * @param obsSac
	 * @param synSac
	 * @param shift
	 *            time shift for correction
	 * @param window
	 * @return
	 */
	private double computeMaxRatio(SACData obsSac, SACData synSac, double shift, Timewindow window) {
		double delta = 1 / sacSamplingHz;

		// マーカーが何秒か
		double startSec = window.getStartTime();
		double endSec = window.getEndTime();

		// create synthetic timewindow
		double[] syn = cutSac(synSac, startSec, endSec);
		// which point gives the maximum value
		int maxPoint = getMaxPoint(syn);
		double maxSyn = syn[maxPoint];

		// create observed timewindow
		double[] obs = cutSac(obsSac, startSec - shift + maxPoint * delta - searchRange,
				startSec - shift + maxPoint * delta + searchRange);
		double maxObs = maxSyn < 0 ? Arrays.stream(obs).min().getAsDouble() : Arrays.stream(obs).max().getAsDouble();

		return maxObs / maxSyn;
	}

	/**
	 * 
	 * synthetic のウインドウが[t1, t2], observed [t1-t(returning value), t2-t]を用いる
	 * 
	 * @param obsSac
	 * @param synSac
	 * @param window
	 * @return value for time shift
	 */
	private double computeTimeshiftForBestCorrelation(SACData obsSac, SACData synSac, Timewindow window) {
		double delta = 1 / sacSamplingHz;

		// マーカーが何秒か
		double startSec = window.getStartTime();
		double endSec = window.getEndTime();

		// create synthetic timewindow
		double[] syn = cutSac(synSac, startSec, endSec);

		// which point gives the maximum value
		int maxPoint = getMaxPoint(syn);

		// startpointから、一番早くしきい値（割合が最大振幅のthreshold）を超える点までのポイント数
		int endPoint = getEndPoint(syn, maxPoint);

		// recreate synthetic timewindow
		syn = cutSac(synSac, startSec, startSec + endPoint * synSac.getValue(SACHeaderEnum.DELTA));

		// create observed timewindow
		double obsStartSec = startSec - searchRange;
		double obsEndSec = startSec + endPoint * synSac.getValue(SACHeaderEnum.DELTA) + searchRange;
		double[] obs = cutSac(obsSac, obsStartSec, obsEndSec);

		int pointshift = getBestPoint(obs, syn, delta);
		double timeshift = pointshift * delta;

		// System.out.println(Math.round(timeshift * 100) / 100.0);
		return Math.round(timeshift * 100) / 100.0;

	}

	//

	/**
	 * 
	 * @param obs
	 * @param syn
	 * @return the number of points we should shift so that obs(t-shift) and
	 *         syn(t) are good correlation
	 */
	private int getBestPoint(double[] obs, double[] syn, double delta) {
		int shift = 0;
		double cor = 0;

		// double synsum = 0;
		// for (int j = 0; j < syn.length; j++)
		// synsum += syn[j] * syn[j];
		// synsum = Math.sqrt(synsum);
		// searchWidthから 相関のいいshiftを探す
		int width = obs.length - syn.length; // searchWidth
		for (int shiftI = 0; shiftI < width; shiftI++) {
			// double variance = 0;
			double tmpcor = 0;
			double obssum = 0;
			for (int j = 0; j < syn.length; j++) {
				tmpcor += syn[j] * obs[j + shiftI];
				obssum += obs[j + shiftI] * obs[j + shiftI];
				// variance += (syn[j] - obs[j + shiftI])
				// * (syn[j] - obs[j + shiftI]);
			}
			obssum = Math.sqrt(obssum);
			// tmpcor /= synsum * obssum;
			tmpcor /= obssum;
			// variance /= obssum;
			// System.out.println(((int) (-searchRange / delta) + shiftI) + " "
			// + tmpcor + " " + variance);
			if (tmpcor > cor) {
				shift = shiftI;
				cor = tmpcor;
			}
		}
		// System.out.println(pointWidth+" "+shift);
		return (int) (searchRange / delta) - shift;
	}

	/**
	 * @param u
	 * @return uが最大絶対値を取る場所
	 */
	private static int getMaxPoint(double[] u) {
		int i = 0;
		double max = 0;
		double max2 = 0;
		for (int j = 0; j < u.length; j++)
			if (max2 < (max = u[j] * u[j])) {
				max2 = max;
				i = j;
			}
		return i;
	}

	/**
	 * 一番早い時刻にthresholdを超える点を返す
	 * 
	 * @param u
	 * @param maxPoint
	 * @return コリレーションを考える領域のおわり
	 */
	private int getEndPoint(double[] u, int maxPoint) {
		double max = u[maxPoint];
		// int endPoint = maxPoint;
		// ピークを探す
		int[] iPeaks = findPeaks(u);
		double minLimit = Math.abs(threshold * max);
		// System.out.println("Threshold is " + minLimit);
		for (int ipeak : iPeaks)
			if (minLimit < Math.abs(u[ipeak]))
				return ipeak;

		return maxPoint;

	}

	/**
	 * ピーク位置を探す (f[a] - f[a-1]) * (f[a+1] - f[a]) < 0 の点
	 * 
	 * @param u
	 * @return ピーク位置
	 */
	private static int[] findPeaks(double[] u) {
		List<Integer> peakI = new ArrayList<>();
		for (int i = 1; i < u.length - 1; i++) {
			double du1 = u[i] - u[i - 1];
			double du2 = u[i + 1] - u[i];
			if (du1 * du2 < 0)
				peakI.add(i);
		}
		int[] peaks = new int[peakI.size()];
		for (int i = 0; i < peaks.length; i++)
			peaks[i] = peakI.get(i);
		// System.out.println(peaks.length+" peaks are found");
		return peaks;
	}

	private static double[] cutSac(SACData sacData, double tStart, double tEnd) {
		Trace t = sacData.createTrace();
		t = t.cutWindow(tStart, tEnd);
		return t.getY();
	}

}
