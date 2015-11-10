package manhattan.selection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import filehandling.sac.SACData;
import filehandling.sac.SACFileName;
import manhattan.butterworth.BandPassFilter;
import manhattan.butterworth.BandStopFilter;
import manhattan.butterworth.ButterworthFilter;
import manhattan.butterworth.HighPassFilter;
import manhattan.butterworth.LowPassFilter;
import manhattan.template.EventFolder;
import manhattan.template.Utilities;

/**
 * 観測波形ディレクトリobsDir、理論波形ディレクトリsynDir双方の下に存在するイベントフォルダの理論波形と観測波形に フィルターを掛ける <br>
 * できたファイルはoutDir下にイベントフォルダを作りそこにつくる
 * 
 * @version 0.0.1 名前はそのまま、ただしsacのUSER0とUSER1に最短周期、最長周期の情報を書き込む
 * @version 0.0.2 フィルターをかける成分の指定
 * @version 0.0.3 観測、もしくは理論波形のみもする
 * @version 0.0.4
 * @since 2013/10/2 passesパラメタを追加
 * @since 2013
 * 
 * @version 0.0.5
 * @since 2014/9/16 small fix
 * 
 * @since 2014/12/26
 * @version 0.0.6 name of output folder is now automatically chosen.
 * 
 * @since 2015/3/11
 * @version 0.1.0 parallelized
 * 
 * @version 0.1.0.1
 * @since 2015/8/6 {@link IOException}
 * 
 * @version 0.1.1
 * @since 2015/8/8 {@link Path} base
 * 
 * @version 0.1.1.1
 * @since 2015/8/13
 * 
 * @version 0.1.1.2
 * @since 2015/9/12 sacfilename..
 * 
 * @version 0.1.2
 * @since 2015/9/14 Utilities
 * 
 * @author Kensuke
 * 
 */
class FilterDivider extends parameter.FilterDivider {

	private ButterworthFilter filter;

	private FilterDivider(Path parameterPath) throws IOException {
		super(parameterPath);
	}

	private Path outPath;

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws InterruptedException if any
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		FilterDivider divider = null;
		if (0 < args.length) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(parameterPath.toString());
			divider = new FilterDivider(parameterPath);
		} else
			divider = new FilterDivider(null);
		long startTime = System.nanoTime();
		divider.setFilter(divider.lowFreq, divider.highFreq, divider.np);
		Set<EventFolder> eventFolderSet = new HashSet<>();
		eventFolderSet.addAll(
				Files.exists(divider.obsPath) ? Utilities.eventFolderSet(divider.obsPath) : Collections.emptySet());
		eventFolderSet.addAll(
				Files.exists(divider.synPath) ? Utilities.eventFolderSet(divider.synPath) : Collections.emptySet());
		if (eventFolderSet.isEmpty())
			return;

		System.err.println("FilterDivider is going.");
		divider.outPath = divider.workPath.resolve("filtered" + Utilities.getTemporaryString());
		Files.createDirectories(divider.outPath);
		ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		eventFolderSet.stream().map(divider::process).forEach(es::submit);
		es.shutdown();
		while(!es.isTerminated()){
			Thread.sleep(100);
		}
		System.err.println("FilterDivider finished in " + Utilities.toTimeString(System.nanoTime() - startTime));
	}

	private Runnable process(EventFolder folder) {
		return () -> {
			String eventname = folder.getName();
			try {
				Files.createDirectories(outPath.resolve(eventname));
				folder.sacFileSet().stream().filter(sacFileName -> components.contains(sacFileName.getComponent()))
						.forEach(this::filterAndout);
			} catch (Exception e) {
				System.err.println("Error on " + folder);
				e.printStackTrace();
			}
		};
	}

	/**
	 * Apply the filter on the sacFile and output in the outDir
	 * 
	 * @param sacFileName
	 *            a sacfilename to be filtered
	 */
	private void filterAndout(SACFileName sacFileName) {
		try {
			SACData sacFile = sacFileName.read().applyButterworthFilter(filter);
			Path outPath = this.outPath.resolve(sacFileName.getGlobalCMTID() + "/" + sacFileName.getName());
			sacFile.writeSAC(outPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param fMin
	 *            透過帯域 最小周波数
	 * @param fMax
	 *            透過帯域 最大周波数
	 * @param delta
	 *            sampling dt
	 */
	private void setFilter(double fMin, double fMax, int n) {
		double omegaH = fMax * 2 * Math.PI * delta;
		double omegaL = fMin * 2 * Math.PI * delta;
		switch (super.filter) {
		case "lowpass":
			filter = new LowPassFilter(omegaL, n);
			break;
		case "highpass":
			filter = new HighPassFilter(omegaH, n);
			break;
		case "bandpass":
			filter = new BandPassFilter(omegaH, omegaL, n);
			break;
		case "bandstop":
			filter = new BandStopFilter(omegaH, omegaL, n);
			break;
		default:
			throw new RuntimeException("No such filter.");
		}
		filter.setBackward(backward);
	}

}
