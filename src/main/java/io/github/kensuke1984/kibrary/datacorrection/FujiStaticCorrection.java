package io.github.kensuke1984.kibrary.datacorrection;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Precision;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.timewindow.Timewindow;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACExtension;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

/**
 * This class computes values of Static correction after Fuji <i>et al</i>.,
 * (2010) <br>
 * dataselectionした後のフォルダでやる<br>
 * <p>
 * The time shift value <i>t</i> for the ray path is for the observed
 * timewindow.<br>
 * (i.e. synthetic window [t1, t2], observed [t1-t, t2-t])
 * <p>
 * The time shift values are computed as follows:
 * <blockquote>ワーキングディレクトリ以下のイベントたちの中にいく<br>
 * 理論波形のstartMkからendMkまでに間で最大ピークを探し冨士さんの 感じで探す とりあえず±sLimit秒で探してみる <br>
 * 観測波形にマーカーがない場合書いてしまう <br>
 * マーカーはrenewパラメタがtrueなら観測波形のマーカーは上書き<br>
 * time shiftの値は小数点２位以下切捨て Algorithm startMkからendMkまでの間で最大振幅を取り、
 * それ以前の時間からthreshold（最大振幅ベース）を超えた振幅の一番早いものをえらびstartMkから、
 * そこまでのタイムウインドウのコリレーションをあわせる <br>
 * </blockquote>
 * <p>
 * If something happens, move the sac file to Trash
 * <p>
 * timeshift fileを一つに統一
 *
 * @author Kensuke Konishi
 * @version 0.2.1.4
 */
public class FujiStaticCorrection implements Operation {

    public static void writeDefaultPropertiesFile() throws IOException {
        Path outPath = Paths.get(FujiStaticCorrection.class.getName() + Utilities.getTemporaryString() + ".properties");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
            pw.println("manhattan FujiStaticCorrection");
            pw.println("##Path of a working folder (.)");
            pw.println("#workPath");
            pw.println("##SacComponents to be used (Z R T)");
            pw.println("#components");
            pw.println("##Path of a root directory containing observed dataset (.)");
            pw.println("#obsPath");
            pw.println("##Path of a root directory containing synthetic dataset (.)");
            pw.println("#synPath");
            pw.println("##Path of a timeWindowInformation file, must be set");
            pw.println("#timewindowInformationPath timewindow.dat");
            pw.println("##boolean convolute (false)");
            pw.println("#convolute");
            pw.println("##double sacSamplingHz(20)");
            pw.println("#sacSamplingHz cant change now");
            pw.println("##double threshold for peak finder (0.2)");
            pw.println("#threshold");
            pw.println("##double searchRange [s] (10)");
            pw.println("#searchRange");
        }
        System.err.println(outPath + " is created.");
    }

    /**
     * components for computation
     */
    private Set<SACComponent> components;

    /**
     * コンボリューションされている波形かそうでないか （両方は無理）
     */
    private boolean convolute;

    /**
     * range for searching [s] ±searchRange秒の中でコリレーション最大値探す
     */
    private double searchRange;

    /**
     * the directory for observed data
     */
    private Path obsPath;

    /**
     * the directory for synthetic data
     */
    private Path synPath;

    /**
     * sampling Hz [Hz] in sac files
     */
    private double sacSamplingHz;

    private Properties property;

    private void checkAndPutDefaults() {
        if (!property.containsKey("workPath")) property.setProperty("workPath", "");
        if (!property.containsKey("components")) property.setProperty("components", "Z R T");
        if (!property.containsKey("obsPath")) property.setProperty("obsPath", "");
        if (!property.containsKey("synPath")) property.setProperty("synPath", "");
        if (!property.containsKey("convolute")) property.setProperty("convolute", "false");
        if (!property.containsKey("threshold")) property.setProperty("threshold", "0.2");
        if (!property.containsKey("searchRange")) property.setProperty("searchRange", "10");
        if (!property.containsKey("sacSamplingHz")) property.setProperty("sacSamplingHz", "20");
    }

    /**
     * シグナルとみなすかどうかの最大振幅から見ての比率
     */
    private double threshold;

    private Path timewindowInformationPath;

    private Path workPath;

    private void set() {
        checkAndPutDefaults();
        workPath = Paths.get(property.getProperty("workPath"));
        if (!Files.exists(workPath)) throw new RuntimeException("The workPath: " + workPath + " does not exist");
        components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
                .collect(Collectors.toSet());
        synPath = getPath("synPath");
        obsPath = getPath("obsPath");
        timewindowInformationPath = getPath("timewindowInformationPath");
        convolute = Boolean.parseBoolean(property.getProperty("convolute"));
        sacSamplingHz = Double.parseDouble(property.getProperty("sacSamplingHz"));// TODO
        searchRange = Double.parseDouble(property.getProperty("searchRange"));
        threshold = Double.parseDouble(property.getProperty("threshold"));
    }

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
            Set<SACFileName> obsFiles;
            try {
                (obsFiles = obsEventDir.sacFileSet()).removeIf(s -> !s.isOBS());
            } catch (IOException e1) {
                e1.printStackTrace();
                return;
            }
            // TreeMap<String, Double> timeshiftMap = new TreeMap<>();
            for (SACFileName obsName : obsFiles) {
                SACComponent component = obsName.getComponent();
                // check a component
                if (!components.contains(component)) continue;
                SACExtension synExt = convolute ? SACExtension.valueOfConvolutedSynthetic(component) :
                        SACExtension.valueOfSynthetic(component);

                SACFileName synName = new SACFileName(
                        synEventPath.resolve(obsName.getStationName() + "." + obsName.getGlobalCMTID() + "." + synExt));
                // System.out.println(obsFile.getName() + " " +
                // synFile.getName());
                if (!synName.exists()) {
                    System.err.println(synName + " does not exist. ");
                    continue;
                }
                SACData obsSac;
                SACData synSac;
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
                    System.err.println(
                            "Deltas are invalid. " + obsSac + " " + obsSac.getValue(SACHeaderEnum.DELTA) + " " +
                                    synSac + " " + synSac.getValue(SACHeaderEnum.DELTA) + " must be " + delta);
                    continue;
                }
                // Pickup time windows of obsName
                Set<TimewindowInformation> windows =
                        timewindowInformation.stream().filter(info -> info.getStation().equals(station))
                                .filter(info -> info.getGlobalCMTID().equals(eventID))
                                .filter(info -> info.getComponent() == component).collect(Collectors.toSet());

                if (windows != null && !windows.isEmpty()) for (Timewindow window : windows)
                    try {
                        double shift = computeTimeshiftForBestCorrelation(obsSac, synSac, window);
                        double ratio = computeMaxRatio(obsSac, synSac, shift, window);
                        StaticCorrection t =
                                new StaticCorrection(station, eventID, component, window.getStartTime(), shift, ratio);
                        staticCorrectionSet.add(t);
                    } catch (Exception e) {
                        System.err.println(window + " is ignored because an error occurs");
                        e.printStackTrace();
                    }

            }
        }
    }

    private FujiStaticCorrection(Properties property) throws IOException {
        this.property = (Properties) property.clone();
        String date = Utilities.getTemporaryString();
        outPath = workPath.resolve("staticCorrection" + date + ".dat");
        staticCorrectionSet = Collections.synchronizedSet(new HashSet<>());
        set();
    }

    private Path outPath;
    private Set<TimewindowInformation> timewindowInformation;

    /**
     * @param args [parameter file name]
     * @throws Exception if any
     */
    public static void main(String[] args) throws Exception {
        FujiStaticCorrection fsc = new FujiStaticCorrection(Property.parse(args));
        long startTime = System.nanoTime();
        System.err.println(FujiStaticCorrection.class.getName() + " is going.");
        fsc.run();
        System.err.println(FujiStaticCorrection.class.getName() + " finished in " +
                Utilities.toTimeString(System.nanoTime() - startTime));
    }

    private void output() throws IOException {
        StaticCorrectionFile.write(staticCorrectionSet, outPath);
    }

    /**
     * Search the max point of synthetic in the time window for a pair. and then
     * search the max value within the search range and same positive and
     * negative. Relative ratio of synthetic is 1;
     *
     * @param obsSac observed sac data
     * @param synSac synthetic sac data
     * @param shift  time shift for correction
     * @param window time window
     * @return ratio of maximum values
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
     * synthetic のウインドウが[t1, t2], observed [t1-t(returning value), t2-t]を用いる
     *
     * @param obsSac observed sac data
     * @param synSac synthetic sac data
     * @param window time window
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
        return Precision.round(timeshift, 2);
    }

    //

    /**
     * @param obs observed waveform
     * @param syn synthetic waveform
     * @return the number of points we should shift so that obs(t-shift) and
     * syn(t) are good correlation
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
     * @param u u[]
     * @return uが最大絶対値を取る場所 a where |u[a]| is maximum value.
     */
    private static int getMaxPoint(double[] u) {
        int i = 0;
        double max;
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
     * @param u        u[x]
     * @param maxPoint u[maxPoint] is maximum
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
            if (minLimit < Math.abs(u[ipeak])) return ipeak;
        return maxPoint;
    }

    /**
     * ピーク位置を探す (f[a] - f[a-1]) * (f[a+1] - f[a]) < 0 の点
     *
     * @param u u[i]
     * @return ピーク位置 a where (f[a] - f[a-1]) * (f[a+1] - f[a]) < 0
     */
    private static int[] findPeaks(double[] u) {
        List<Integer> peakI = new ArrayList<>();
        for (int i = 1; i < u.length - 1; i++) {
            double du1 = u[i] - u[i - 1];
            double du2 = u[i + 1] - u[i];
            if (du1 * du2 < 0) peakI.add(i);
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
        Set<EventFolder> eventDirs = Utilities.eventFolderSet(obsPath);
        int n = Runtime.getRuntime().availableProcessors();
        ExecutorService es = Executors.newFixedThreadPool(n);
        timewindowInformation = TimewindowInformationFile.read(timewindowInformationPath);
        eventDirs.stream().map(Worker::new).forEach(es::execute);
        es.shutdown();

        while (!es.isTerminated()) {
            try {
                // System.out.println("waiting");
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        output();
    }

}
