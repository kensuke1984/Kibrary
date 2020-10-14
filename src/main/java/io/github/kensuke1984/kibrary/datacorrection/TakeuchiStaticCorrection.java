package io.github.kensuke1984.kibrary.datacorrection;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Maker of static correction suggested by Nozomu Takeuchi. It seeks up-and-down
 * two peaks in given {@link TimewindowInformation} for each path.
 * <p>
 * Values of the correction is by the average time of arrivals and amplitudes of
 * those peaks.
 * <p>
 * Start time for identification is a start time in the given
 * {@link TimewindowInformationFile}.
 * <p>
 * <b>Assume that there are no stations with the same name but different
 * networks in an event</b>
 *
 * @author Kensuke Konishi
 * @version 0.1.1.4
 * @see StaticCorrection
 */
public class TakeuchiStaticCorrection implements Operation {
    /**
     * components for computation
     */
    protected Set<SACComponent> components;
    /**
     * コンボリューションされている波形かそうでないか （両方は無理）
     */
    protected boolean convolute;
    /**
     * {@link Path} for a root directory containing observed data
     */
    protected Path obsPath;
    /**
     * sampling Hz [Hz] in sac files
     */
    protected double sacSamplingHz;
    /**
     * {@link Path} for a root directory containing synthetic data
     */
    protected Path synPath;
    protected Path timewindowInformationPath;
    private Properties property;
    private Path workPath;
    private Set<StaticCorrection> outStaticCorrectionSet;
    private Path outStaticCorrectionPath;
    private Set<TimewindowInformation> timewindow;

    public TakeuchiStaticCorrection(Properties property) throws IOException {
        this.property = (Properties) property.clone();
        String date = Utilities.getTemporaryString();
        set();
        timewindow = TimewindowInformationFile.read(timewindowInformationPath);
        outStaticCorrectionPath = workPath.resolve("takeuchiCorrection" + date + ".dat");
        outStaticCorrectionSet = Collections.synchronizedSet(new HashSet<>());
    }

    public static void writeDefaultPropertiesFile() throws IOException {
        Path outPath =
                Paths.get(TakeuchiStaticCorrection.class.getName() + Utilities.getTemporaryString() + ".properties");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
            pw.println("manhattan TakeuchiStaticCorrection");
            pw.println("##Path of a work folder (.)");
            pw.println("#workPath");
            pw.println("#SacComponents to be used (Z R T)");
            pw.println("#components");
            pw.println("##Path of a root directory containing observed data (.)");
            pw.println("#obsPath");
            pw.println("##Path of a root directory containing synthetic data (.)");
            pw.println("#synPath");
            pw.println("##Path of a file for timeWindow information, must be set");
            pw.println("#timewindowInformationPath timewindow.dat");
            pw.println("##boolean convolute (false)");
            pw.println("#convolute");
            pw.println("#double sacSamplingHz (20)");
            pw.println("#sacSamplingHz cant change now");

        }
        System.err.println(outPath + " is created.");
    }

    /**
     * @param args [parameter file name]
     * @throws IOException if any
     */
    public static void main(String[] args) throws IOException {
        TakeuchiStaticCorrection tsm = new TakeuchiStaticCorrection(Property.parse(args));
        long time = System.nanoTime();
        System.err.println(TakeuchiStaticCorrection.class.getName() + " is going.");
        tsm.run();
        System.err.println(TakeuchiStaticCorrection.class.getName() + " finished in " +
                Utilities.toTimeString(System.nanoTime() - time));
    }

    private void checkAndPutDefaults() {
        if (!property.containsKey("workPath")) property.setProperty("workPath", "");
        if (!property.containsKey("components")) property.setProperty("components", "Z R T");
        if (!property.containsKey("obsPath")) property.setProperty("obsPath", "");
        if (!property.containsKey("synPath")) property.setProperty("synPath", "");
        if (!property.containsKey("convolute")) property.setProperty("convolute", "false");
        if (!property.containsKey("sacSamplingHz")) property.setProperty("sacSamplingHz", "20");
        if (!property.containsKey("timewindowInformationPath"))
            throw new IllegalArgumentException("There is no information about timewindowInformationPath");
    }

    private void set() throws IOException{
        checkAndPutDefaults();
        workPath = Paths.get(property.getProperty("workPath"));
        if (!Files.exists(workPath)) throw new NoSuchFileException(workPath + " (workPath)");
        synPath = getPath("synPath");
        obsPath = getPath("obsPath");
        timewindowInformationPath = getPath("timeWindowInformationPath");

        components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
                .collect(Collectors.toSet());

        convolute = Boolean.parseBoolean(property.getProperty("convolute"));
        sacSamplingHz = Double.parseDouble(property.getProperty("sacSamplingHz")); // TODO
    }

    @Override
    public void run() throws IOException {
        Set<SACFileName> nameSet;
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
        Set<TimewindowInformation> timeWindowSet =
                timewindow.stream().filter(info -> info.getStation().getName().equals(stationName))
                        .filter(info -> info.getGlobalCMTID().equals(id))
                        .filter(info -> info.getComponent() == component).collect(Collectors.toSet());
        if (timeWindowSet.size() != 1) throw new RuntimeException(timewindowInformationPath + " is invalid.");
        TimewindowInformation timeWindow = timeWindowSet.iterator().next();
        SACData obsSac = obsName.read();
        SACData synSac = synName.read();
        Station station = obsSac.getStation();
        Trace obsTrace = obsSac.createTrace().cutWindow(timeWindow);
        Trace synTrace = synSac.createTrace().cutWindow(timeWindow);
        double obsT = (obsTrace.getXforMaxValue() + obsTrace.getXforMinValue()) / 2;
        double synT = (synTrace.getXforMaxValue() + synTrace.getXforMinValue()) / 2;
        double timeShift = synT - obsT;
        double obsAmp = (obsTrace.getMaxValue() - obsTrace.getMinValue()) / 2;
        double synAmp = (synTrace.getMaxValue() - synTrace.getMinValue()) / 2;
        double amplitudeRatio = obsAmp / synAmp;
        StaticCorrection sc =
                new StaticCorrection(station, id, component, timeWindow.getStartTime(),
                		timeShift, amplitudeRatio, timeWindow.getPhases());
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
        String ext = obsSacFileName.getComponent() + (convolute ? "sc" : "s");
        String id = obsSacFileName.getGlobalCMTID().toString();
        String name = obsSacFileName.getStationName() + '.' + id + '.' + ext;
        return new SACFileName(synPath.resolve(id + "/" + name));
    }

    @Override
    public Path getWorkPath() {
        return workPath;
    }

    @Override
    public Properties getProperties() {
        return (Properties) property.clone();
    }
}
