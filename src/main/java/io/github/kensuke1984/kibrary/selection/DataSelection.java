package io.github.kensuke1984.kibrary.selection;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Precision;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
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
 * 理論波形と観測波形の比較から使えるものを選択する。<br>
 * workDir以下にあるイベントディレクトリの中から選ぶ<br>
 * 振幅比、correlation、variance<br>
 * <p>
 * {@link TimewindowInformationFile} necessary.
 *
 * @author Kensuke Konishi
 * @version 0.1.2
 */
public class DataSelection implements Operation {
    public static void writeDefaultPropertiesFile() throws IOException {
        Path outPath = Paths.get(DataSelection.class.getName() + Utilities.getTemporaryString() + ".properties");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
            pw.println("manhattan DataSelection");
            pw.println("##Path of a working folder (.)");
            pw.println("#workPath");
            pw.println("##Sac components to be used (Z R T)");
            pw.println("#components");
            pw.println("##Path of a root folder containing observed dataset (.)");
            pw.println("#obsPath");
            pw.println("##Path of a root folder containing synthetic dataset (.)");
            pw.println("#synPath");
            pw.println("##boolean convolute (true)");
            pw.println("#convolute");
            pw.println("##Path of a time window information file, must be defined");
            pw.println("#timewindowInformationFilePath timewindow.dat");
            pw.println("##Path of a static correction file");
            pw.println("##If you do not want to consider static correction, then comment out the next line");
            pw.println("#staticCorrectionInformationFilePath staticCorrection.dat");
            pw.println("##double sacSamplingHz (20)");
            pw.println("#sacSamplingHz cant change now");
            pw.println("##double minCorrelation (0)");
            pw.println("#minCorrelation");
            pw.println("##double maxCorrelation (1)");
            pw.println("#maxCorrelation");
            pw.println("##double minVariance (0)");
            pw.println("#minVariance");
            pw.println("##double maxVariance (2)");
            pw.println("#maxVariance");
            pw.println("##double ratio (2)");
            pw.println("#ratio");
        }
        System.err.println(outPath + " is created.");
    }

    private Set<EventFolder> eventDirs;

    private String dateStr;

    public DataSelection(Properties property) throws IOException {
        this.property = (Properties) property.clone();
        set();
    }

    private Set<SACComponent> components;
    private Path obsPath;
    private Path synPath;
    private boolean convolute;
    private Path timewindowInformationFilePath;
    private Path staticCorrectionInformationFilePath;

    /**
     * Minimum correlation coefficients
     */
    private double minCorrelation;

    /**
     * Maximum correlation coefficients
     */
    private double maxCorrelation;

    /**
     * Minimum variance
     */
    private double minVariance;

    /**
     * Maximum variance
     */
    private double maxVariance;

    /**
     * amplitude のしきい値
     */
    private double ratio;

    private void checkAndPutDefaults() {
        if (!property.containsKey("workPath")) property.setProperty("workPath", "");
        if (!property.containsKey("components")) property.setProperty("components", "Z R T");
        if (!property.containsKey("obsPath")) property.setProperty("obsPath", "");
        if (!property.containsKey("synPath")) property.setProperty("synPath", "");
        if (!property.containsKey("minCorrelation")) property.setProperty("minCorrelation", "0");
        if (!property.containsKey("maxCorrelation")) property.setProperty("maxCorrelation", "1");
        if (!property.containsKey("minVariance")) property.setProperty("minVariance", "0");
        if (!property.containsKey("maxVariance")) property.setProperty("maxVariance", "2");
        if (!property.containsKey("ratio")) property.setProperty("ratio", "2");
        if (!property.containsKey("convolute")) property.setProperty("convolute", "true");
    }

    private void set() throws IOException {
        checkAndPutDefaults();
        workPath = Paths.get(property.getProperty("workPath"));
        if (!Files.exists(workPath)) throw new RuntimeException("The workPath: " + workPath + " does not exist");

        obsPath = getPath("obsPath");
        synPath = getPath("synPath");
        components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
                .collect(Collectors.toSet());

        convolute = Boolean.parseBoolean(property.getProperty("convolute"));
        minCorrelation = Double.parseDouble(property.getProperty("minCorrelation"));
        maxCorrelation = Double.parseDouble(property.getProperty("maxCorrelation"));
        minVariance = Double.parseDouble(property.getProperty("minVariance"));
        maxVariance = Double.parseDouble(property.getProperty("maxVariance"));
        ratio = Double.parseDouble(property.getProperty("ratio"));
        timewindowInformationFilePath = getPath("timewindowInformationFilePath");
        if (property.containsKey("staticCorrectionInformationFilePath"))
            staticCorrectionInformationFilePath = getPath("staticCorrectionInformationFilePath");
        // sacSamplingHz
        // =Double.parseDouble(reader.getFirstValue("sacSamplingHz")); TODO
        // sacSamplingHz = 20;
        staticCorrectionSet = staticCorrectionInformationFilePath == null ? Collections.emptySet() :
                StaticCorrectionFile.read(staticCorrectionInformationFilePath);
        // eventDirs = WorkingDirectory.listEventDirs(workDir);
        eventDirs = Utilities.eventFolderSet(obsPath);
        sourceTimewindowInformationSet = TimewindowInformationFile.read(timewindowInformationFilePath);
        dateStr = Utilities.getTemporaryString();
        outputGoodWindowPath = workPath.resolve("selectedTimewindow" + dateStr + ".dat");
        goodTimewindowInformationSet = Collections.synchronizedSet(new HashSet<>());
    }

    private Set<TimewindowInformation> sourceTimewindowInformationSet;
    private Set<TimewindowInformation> goodTimewindowInformationSet;

    private Path outputGoodWindowPath;

    private Set<StaticCorrection> staticCorrectionSet;

    /**
     * @param args [parameter file name]
     * @throws Exception if an I/O happens
     */
    public static void main(String[] args) throws Exception {
        DataSelection ds = new DataSelection(Property.parse(args));
        long start = System.nanoTime();
        System.err.println(DataSelection.class.getName() + " is going");
        ds.run();
        System.err.println(
                DataSelection.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - start));
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
            obsEventDirectory = ed;
            (obsFiles = ed.sacFileSet()).removeIf(n -> !n.isOBS());
            id = ed.getGlobalCMTID();
            synEventDirectory = new EventFolder(synPath.resolve(ed.getName()));
            if (!synEventDirectory.exists()) return;
        }

        @Override
        public void run() {
            if (!synEventDirectory.exists()) {
                try {
                    FileUtils
                            .moveDirectoryToDirectory(obsEventDirectory, workPath.resolve("withoutSyn").toFile(), true);
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
                if (convolute) lpw.println("#convolved");
                else lpw.println("#not convolved");
                lpw.println("#s e c starttime use ratio(syn/obs){abs max min} variance correlation");

                for (SACFileName obsName : obsFiles) {
                    // check components
                    if (!components.contains(obsName.getComponent())) continue;
                    String stationName = obsName.getStationName();
                    SACComponent component = obsName.getComponent();
                    // double timeshift = 0;
                    SACExtension synExt = convolute ? SACExtension.valueOfConvolutedSynthetic(component) :
                            SACExtension.valueOfSynthetic(component);

                    SACFileName synName = new SACFileName(synEventDirectory, stationName + "." + id + "." + synExt);
                    if (!synName.exists()) continue;

                    // synthetic sac
                    SACData obsSac = obsName.read();
                    SACData synSac = synName.read();

                    Station station = obsSac.getStation();
                    //
                    if (synSac.getValue(SACHeaderEnum.DELTA) != obsSac.getValue(SACHeaderEnum.DELTA)) continue;

                    // Pickup a time window of obsName
                    Set<TimewindowInformation> windowInformations = sourceTimewindowInformationSet.stream()
                            .filter(info -> info.getStation().equals(station) && info.getGlobalCMTID().equals(id) &&
                                    info.getComponent() == component).sorted().collect(Collectors.toSet());

                    if (windowInformations.isEmpty()) continue;

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
                System.err.println("error on " + obsEventDirectory);
            }
            System.err.print(".");
            // System.out.println(obsEventDirectory + " is done");
        }
    }

    /**
     * ID for static correction and time window information Default is station
     * name, global CMT id, component.
     */
    private BiPredicate<StaticCorrection, TimewindowInformation> isPair =
            (s, t) -> s.getStation().equals(t.getStation()) && s.getGlobalCMTID().equals(t.getGlobalCMTID()) &&
                    s.getComponent() == t.getComponent();

    private StaticCorrection getStaticCorrection(TimewindowInformation window) {
        return staticCorrectionSet.stream().filter(s -> isPair.test(s, window)).findAny().get();
    }

    /**
     * @param timewindow timewindow to shift
     * @return if there is time shift information for the input timewindow, then
     * creates new timewindow and returns it, otherwise, just returns
     * the input one.
     */
    private TimewindowInformation shift(TimewindowInformation timewindow) {
        if (staticCorrectionSet.isEmpty()) return timewindow;
        StaticCorrection foundShift = getStaticCorrection(timewindow);
        double value = foundShift.getTimeshift();
        return new TimewindowInformation(timewindow.getStartTime() - value, timewindow.getEndTime() - value,
                foundShift.getStation(), foundShift.getGlobalCMTID(), foundShift.getComponent());
    }

    private boolean check(PrintWriter writer, String stationName, GlobalCMTID id, SACComponent component,
                          TimewindowInformation window, RealVector obsU, RealVector synU) throws IOException {
        if (obsU.getDimension() < synU.getDimension()) synU = synU.getSubVector(0, obsU.getDimension() - 1);
        else if (synU.getDimension() < obsU.getDimension()) obsU = obsU.getSubVector(0, synU.getDimension() - 1);

        // check
        double synMax = synU.getMaxValue();
        double synMin = synU.getMinValue();
        double obsMax = obsU.getMaxValue();
        double obsMin = obsU.getMinValue();
        double obs2 = obsU.dotProduct(obsU);
        double syn2 = synU.dotProduct(synU);
        double cor = obsU.dotProduct(synU);
        double var = obs2 + syn2 - 2 * cor;
        double maxRatio = Precision.round(synMax / obsMax, 2);
        double minRatio = Precision.round(synMin / obsMin, 2);
        double absRatio = (-synMin < synMax ? synMax : -synMin) / (-obsMin < obsMax ? obsMax : -obsMin);
        var /= obs2;
        cor /= Math.sqrt(obs2 * syn2);

        absRatio = Precision.round(absRatio, 2);
        var = Precision.round(var, 2);
        cor = Precision.round(cor, 2);
        boolean isok = !(ratio < minRatio || minRatio < 1 / ratio || ratio < maxRatio || maxRatio < 1 / ratio ||
                ratio < absRatio || absRatio < 1 / ratio || cor < minCorrelation || maxCorrelation < cor ||
                var < minVariance || maxVariance < var);

        writer.println(stationName + " " + id + " " + component + " " + window.getStartTime() + " " + isok + " " + absRatio + " " + maxRatio + " " +
                minRatio + " " + var + " " + cor);
        return isok;
    }

    /**
     * @param sac        {@link SACData} to cut
     * @param timeWindow time window
     * @return new Trace for the timewindow [tStart:tEnd]
     */
    private static RealVector cutSAC(SACData sac, Timewindow timeWindow) {
        Trace trace = sac.createTrace();
        double tStart = timeWindow.getStartTime();
        double tEnd = timeWindow.getEndTime();
        return new ArrayRealVector(trace.cutWindow(tStart, tEnd).getY(), false);
    }

    private Path workPath;

    @Override
    public Path getWorkPath() {
        return workPath;
    }

    private Properties property;

    @Override
    public Properties getProperties() {
        return (Properties) property.clone();
    }

    @Override
    public void run() throws Exception {
        int N_THREADS = Runtime.getRuntime().availableProcessors();

        ExecutorService exec = Executors.newFixedThreadPool(N_THREADS);

        for (EventFolder eventDirectory : eventDirs)
            exec.execute(new Worker(eventDirectory));

        exec.shutdown();
        while (!exec.isTerminated()) try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.err.println();
        output();
    }
}
