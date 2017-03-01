package io.github.kensuke1984.kibrary.waveformdata;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.spc.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Creates a pair of files containing 1-D partial derivatives
 * <p>
 * TODO shとpsvの曖昧さ 両方ある場合ない場合等 現状では combineして対処している
 * <p>
 * Time length (tlen) and the number of step in frequency domain (np) in DSM
 * software must be same. Those values are set in a parameter file.
 * <p>
 * Only partials for radius written in a parameter file are computed.
 * <p>
 * <b>Assume there are no station with the same name but different networks in
 * same events</b> TODO
 *
 * @author Kensuke Konishi
 * @version 0.2.0.5
 */
public class Partial1DDatasetMaker implements Operation {
    private boolean backward;
    private Set<SACComponent> components;
    private Path workPath;
    /**
     * bp, fp フォルダの下のどこにspcファイルがあるか 直下なら何も入れない（""）
     */
    private String modelName;
    /**
     * Path of a timewindow information file
     */
    private Path timewindowPath;

    private Set<PartialType> partialTypes;
    /**
     * bandpassの最小周波数（Hz）
     */
    private double minFreq;
    /**
     * bandpassの最大周波数（Hz）
     */
    private double maxFreq;
    /**
     * spcFileをコンボリューションして時系列にする時のサンプリングHz デフォルトは20
     * TODOまだ触れない
     */
    private double partialSamplingHz = 20;
    /**
     * 最後に時系列で切り出す時のサンプリングヘルツ(Hz)
     */
    private double finalSamplingHz;
    /**
     * The folder contains source time functions.
     */
    private Path sourceTimeFunctionPath;
    /**
     * 0:none, 1:boxcar, 2:triangle.
     */
    private int sourceTimeFunction;
    /**
     * time length (DSM parameter)
     */
    private double tlen;
    /**
     * number of steps in frequency domain (DSM parameter)
     */
    private int np;
    /**
     * radii of perturbation
     */
    private double[] bodyR;

    private int numberOfAddedID;
    private int lsmooth;
    private Properties property;
    /**
     * filter to be applied
     */
    private ButterworthFilter filter;
    /**
     * sacdataを何ポイントおきに取り出すか
     */
    private int step;

    private Set<TimewindowInformation> timewindowInformationSet;
    //
    private WaveformDataWriter partialDataWriter;
    private Path logPath;
    private FujiConversion fujiConversion;
    private Map<GlobalCMTID, SourceTimeFunction> userSourceTimeFunctions;
    private Set<GlobalCMTID> idSet;
    private Set<Station> stationSet;
    private Set<Location> perturbationLocationSet;
    private double[][] periodRanges;

    public Partial1DDatasetMaker(Properties property) throws IOException {
        this.property = (Properties) property.clone();
        set();
    }

    public static void writeDefaultPropertiesFile() throws IOException {
        Path outPath =
                Paths.get(Partial1DDatasetMaker.class.getName() + Utilities.getTemporaryString() + ".properties");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
            pw.println("manhattan Partial1DDatasetMaker");
            pw.println("##Path of a working directory (.)");
            pw.println("#workPath");
            pw.println("##SacComponents to be used(Z R T)");
            pw.println("#components");
            pw.println("##String if 'modelName' is PREM, spectrum files in 'eventDir/PREM' are used.");
            pw.println("##If it is unset, then automatically set as the name of the folder in the eventDirs");
            pw.println("##but the eventDirs can have only one folder inside and they must be same.");
            pw.println("#modelName");
            pw.println("##Type source time function 0:none, 1:boxcar, 2:triangle. (0)");
            pw.println("##or folder name containing *.stf if you want to your own GLOBALCMTID.stf ");
            pw.println("#sourceTimeFunction");
            pw.println("##Path of a timewindow information file, must be set");
            pw.println("#timewindowPath timewindow.dat");
            pw.println("##PartialType[] compute types (PAR2)");
            pw.println("#partialTypes");
            pw.println("##Filter if backward filtering is applied (true)");
            pw.println("#backward");
            pw.println("##double time length:DSM parameter tlen, must be set");
            pw.println("#tlen 3276.8");
            pw.println("##int step of frequency domain DSM parameter np, must be set");
            pw.println("#np 512");
            pw.println("##double minimum value of passband (0.005)");
            pw.println("#minFreq");
            pw.println("##double maximum value of passband (0.08)");
            pw.println("#maxFreq");
            pw.println("#double");
            pw.println("#partialSamplingHz cant change now");
            pw.println("##double sampling Hz in output dataset (1)");
            pw.println("#finalSamplingHz");
            pw.println("##radius for perturbation points, must be set");
            pw.println("#bodyR 3505 3555 3605");
        }
        System.err.println(outPath + " is created.");
    }

    /**
     * @param args [parameter file name]
     */
    public static void main(String[] args) throws IOException {
        Partial1DDatasetMaker pdm = new Partial1DDatasetMaker(Property.parse(args));
        if (!Files.exists(pdm.timewindowPath)) throw new NoSuchFileException(pdm.timewindowPath.toString());
        pdm.run();
    }

    private void checkAndPutDefaults() {
        if (!property.containsKey("workPath")) property.setProperty("workPath", "");
        if (!property.containsKey("components")) property.setProperty("components", "Z R T");
        if (!property.containsKey("modelName")) property.setProperty("modelName", "");
        if (!property.containsKey("sourceTimeFunction")) property.setProperty("sourceTimeFunction", "0");
        if (!property.containsKey("partialTypes")) property.setProperty("partialTypes", "PAR2");
        if (!property.containsKey("backward")) property.setProperty("backward", "true");
        if (!property.containsKey("minFreq")) property.setProperty("minFreq", "0.005");
        if (!property.containsKey("maxFreq")) property.setProperty("maxFreq", "0.08");
        if (!property.containsKey("finalSamplingHz")) property.setProperty("finalSamplingHz", "1");
        if (!property.containsKey("timewindowPath")) throw new IllegalArgumentException("There is no information about timewindowPath.");
        if (!property.containsKey("bodyR")) throw new IllegalArgumentException("There is no information about bodyR.");
    }

    private void setModelName() throws IOException {
        Set<EventFolder> eventFolders = Utilities.eventFolderSet(workPath);
        Set<String> possibleNames =
                eventFolders.stream().flatMap(ef -> Arrays.stream(ef.listFiles(File::isDirectory))).map(File::getName)
                        .collect(Collectors.toSet());
        if (possibleNames.size() != 1) throw new RuntimeException(
                "There are no model folder in event folders or more than one folder. You must specify 'modelName' in the case.");

        String modelName = possibleNames.iterator().next();
        if (eventFolders.stream().map(EventFolder::toPath).map(p -> p.resolve(modelName)).allMatch(Files::exists))
            this.modelName = modelName;
        else throw new RuntimeException("There are some events without model folder " + modelName);
    }

    private void setSourceTimeFunction() throws NoSuchFileException {
        String s = property.getProperty("sourceTimeFunction");
        if (s.length() == 1 && Character.isDigit(s.charAt(0)))
            sourceTimeFunction = Integer.parseInt(property.getProperty("sourceTimeFunction"));
        else {
            sourceTimeFunction = -1;
            sourceTimeFunctionPath = getPath("sourceTimeFunction");
            if (!Files.exists(sourceTimeFunctionPath)) throw new NoSuchFileException(sourceTimeFunctionPath.toString());
        }
        if (sourceTimeFunction < -1 || 2 < sourceTimeFunction)
            throw new RuntimeException("Integer for source time function is invalid.");
    }

    /**
     * set parameter
     */
    private void set() throws IOException {
        checkAndPutDefaults();
        workPath = Paths.get(property.getProperty("workPath"));

        if (!Files.exists(workPath)) throw new RuntimeException("The workPath: " + workPath + " does not exist");
        timewindowPath = getPath("timewindowPath");
        components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
                .collect(Collectors.toSet());

        setSourceTimeFunction();

        backward = Boolean.parseBoolean(property.getProperty("backward"));

        modelName = property.getProperty("modelName");
        if (modelName.isEmpty()) setModelName();

        partialTypes = Arrays.stream(property.getProperty("partialTypes").split("\\s+")).map(PartialType::valueOf)
                .collect(Collectors.toSet());

        tlen = Double.parseDouble(property.getProperty("tlen"));
        np = Integer.parseInt(property.getProperty("np"));
        minFreq = Double.parseDouble(property.getProperty("minFreq"));
        maxFreq = Double.parseDouble(property.getProperty("maxFreq"));
        bodyR = Arrays.stream(property.getProperty("bodyR").split("\\s+")).mapToDouble(Double::parseDouble).toArray();
        // partialSamplingHz
        // =Double.parseDouble(reader.getFirstValue("partialSamplingHz")); TODO
        finalSamplingHz = Double.parseDouble(property.getProperty("finalSamplingHz"));

    }

    private synchronized void add() {
        numberOfAddedID++;
    }

    private void setLsmooth() {
        int pow2np = Integer.highestOneBit(np);
        if (pow2np < np) pow2np *= 2;

        int lsmooth = (int) (0.5 * tlen * partialSamplingHz / pow2np);
        int ismooth = Integer.highestOneBit(lsmooth);
        this.lsmooth = ismooth == lsmooth ? lsmooth : ismooth * 2;
    }

    private void readSourceTimeFunctions() throws IOException {
        Set<GlobalCMTID> ids = timewindowInformationSet.stream().map(TimewindowInformation::getGlobalCMTID)
                .collect(Collectors.toSet());
        userSourceTimeFunctions = ids.stream().collect(Collectors.toMap(id -> id, id -> {
            try {
                Path sourceTimeFunctionPath = this.sourceTimeFunctionPath.resolve(id + ".stf");
                return SourceTimeFunction.readSourceTimeFunction(sourceTimeFunctionPath);
            } catch (Exception e) {
                throw new RuntimeException("Source time function file for " + id + " is broken.");
            }
        }));

    }

    private void setPerturbationLocation() {
        perturbationLocationSet = Arrays.stream(bodyR).mapToObj(r -> new Location(0, 0, r)).collect(Collectors.toSet());
    }

    @Override
    public void run() throws IOException {
        String dateString = Utilities.getTemporaryString();

        logPath = workPath.resolve("partial1D" + dateString + ".log");

        System.err.println(Partial1DDatasetMaker.class.getName() + " is going.");
        long startTime = System.nanoTime();

        // pdm.createStreams();
        int N_THREADS = Runtime.getRuntime().availableProcessors();
        // N_THREADS = 2;
        writeLog("going with " + N_THREADS + " threads");

        if (partialTypes.contains(PartialType.PARQ)) fujiConversion = new FujiConversion(PolynomialStructure.PREM);

        setLsmooth();
        writeLog("Set lsmooth " + lsmooth);

        // タイムウインドウの情報を読み取る。
        System.err.print("Reading timewindow information ");
        timewindowInformationSet = TimewindowInformationFile.read(timewindowPath);
        System.err.println("done");

        if (sourceTimeFunction == -1) readSourceTimeFunctions();

        // filter設計
        System.err.println("Designing filter.");
        setBandPassFilter();
        System.err.println("Model name is " + modelName);
        writeLog(filter.toString());
        setPerturbationLocation();
        stationSet = timewindowInformationSet.parallelStream().map(TimewindowInformation::getStation)
                .collect(Collectors.toSet());
        idSet = Utilities.globalCMTIDSet(workPath);
        // information about output partial types
        writeLog(partialTypes.stream().map(Object::toString).collect(Collectors.joining(" ", "Computing for ", "")));

        // sacdataを何ポイントおきに取り出すか
        step = (int) (partialSamplingHz / finalSamplingHz);

        Set<EventFolder> eventDirs = Utilities.eventFolderSet(workPath);

        // create ThreadPool
        ExecutorService execs = Executors.newFixedThreadPool(N_THREADS);

        Path idPath = workPath.resolve("partial1DID" + dateString + ".dat");
        Path datasetPath = workPath.resolve("partial1D" + dateString + ".dat");
        try (WaveformDataWriter pdw = new WaveformDataWriter(idPath, datasetPath, stationSet, idSet, periodRanges,
                perturbationLocationSet)) {

            partialDataWriter = pdw;
            for (EventFolder eventDir : eventDirs)
                execs.execute(new Worker(eventDir));
            // break;
            execs.shutdown();

            while (!execs.isTerminated()) Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println();
        String endLine = Partial1DDatasetMaker.class.getName() + " finished in " +
                Utilities.toTimeString(System.nanoTime() - startTime);
        System.err.println(endLine);
        writeLog(endLine);
        writeLog(idPath + " " + datasetPath + " were created");
        writeLog(numberOfAddedID + " IDs are added.");

    }

    private void setBandPassFilter() throws IOException {
        double omegaH = maxFreq * 2 * Math.PI / partialSamplingHz;
        double omegaL = minFreq * 2 * Math.PI / partialSamplingHz;
        filter = new BandPassFilter(omegaH, omegaL, 4);
        filter.setBackward(backward);
        periodRanges = new double[][]{{1 / maxFreq, 1 / minFreq}};
        writeLog(filter.toString());
    }

    private void writeLog(String line) throws IOException {
        Date now = new Date();
        synchronized (this) {
            try (PrintWriter pw = new PrintWriter(
                    Files.newBufferedWriter(logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
                pw.println(now + " : " + line);
            }
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

    private class Worker implements Runnable {

        private SourceTimeFunction sourceTimeFunction;
        private GlobalCMTID id;
        private EventFolder eventDir;

        private Worker(EventFolder eventDir) {
            this.eventDir = eventDir;
            id = eventDir.getGlobalCMTID();
        }

        @Override
        public void run() {
            try {
                writeLog("Running on " + id);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            Path spcFolder = eventDir.toPath().resolve(modelName); // SPCの入っているフォルダ

            if (!Files.exists(spcFolder)) {
                System.err.println(spcFolder + " does not exist...");
                return;
            }

            Set<SpcFileName> spcFileNameSet;
            try {
                spcFileNameSet = Utilities.collectSpcFileName(spcFolder);
            } catch (IOException e1) {
                e1.printStackTrace();
                return;
            }

            // compute source time function
            sourceTimeFunction = computeSourceTimeFunction();

            // すべてのspcファイルに対しての処理
            for (SpcFileName spcFileName : spcFileNameSet) {
                // 理論波形（非偏微分係数波形）ならスキップ
                if (spcFileName.isSynthetic()) continue;

                if (!spcFileName.getSourceID().equals(id.toString())) {
                    try {
                        writeLog(spcFileName + " has an invalid global CMT ID.");
                        continue;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                SpcFileType spcFileType = spcFileName.getFileType();

                // 3次元用のスペクトルなら省く
                if (spcFileType == SpcFileType.PB || spcFileType == SpcFileType.PF) continue;

                // check if the partialtype is included in computing list.
                PartialType partialType = PartialType.valueOf(spcFileType.toString());

                if (!(partialTypes.contains(partialType) ||
                        (partialTypes.contains(PartialType.PARQ) && spcFileType == SpcFileType.PAR2))) continue;

                try {
                    addPartialSpectrum(spcFileName);
                } catch (ClassCastException e) {
                    // 出来上がったインスタンスがOneDPartialSpectrumじゃない可能性
                    System.err.println(spcFileName + "is not 1D partial.");
                } catch (Exception e) {
                    System.err.println(spcFileName + " is invalid.");
                    e.printStackTrace();
                    try {
                        writeLog(spcFileName + " is invalid.");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            System.err.print(".");
        }

        private SourceTimeFunction computeSourceTimeFunction() {
            GlobalCMTID id = eventDir.getGlobalCMTID();
            double halfDuration = id.getEvent().getHalfDuration();
            switch (Partial1DDatasetMaker.this.sourceTimeFunction) {
                case -1:
                    return userSourceTimeFunctions.get(id);
                case 0:
                    return null;
                case 1:
                    return SourceTimeFunction.boxcarSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration);
                case 2:
                    return SourceTimeFunction.triangleSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration);
                default:
                    throw new RuntimeException("Integer for source time function is invalid.");
            }
        }

        private void cutAndWrite(Station station, double[] filteredUt, TimewindowInformation t, double bodyR,
                                 PartialType partialType) {

            double[] cutU = sampleOutput(filteredUt, t);

            PartialID pid = new PartialID(station, id, t.getComponent(), finalSamplingHz, t.getStartTime(), cutU.length,
                    1 / maxFreq, 1 / minFreq, 0, sourceTimeFunction != null, new Location(0, 0, bodyR), partialType,
                    cutU);
            try {
                partialDataWriter.addPartialID(pid);
                add();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void process(DSMOutput spectrum) {
            for (SACComponent component : components)
                spectrum.getSpcBodyList().stream().map(body -> body.getSpcComponent(component))
                        .forEach(spcComponent -> {
                            if (sourceTimeFunction != null) spcComponent.applySourceTimeFunction(sourceTimeFunction);
                            spcComponent.toTimeDomain(lsmooth);
                            spcComponent.applyGrowingExponential(spectrum.omegai(), tlen);
                            spcComponent.amplitudeCorrection(tlen);
                        });
        }

        private void addPartialSpectrum(SpcFileName spcname) throws IOException {
            DSMOutput spectrum = spcname.read();
            if (spectrum.tlen() != tlen || spectrum.np() != np) {
                System.err.println(spcname + " has different np or tlen.");
                writeLog(spcname + " has different np or tlen.");
                return;
            }

            String stationName = spcname.getObserverID();
            Station station = new Station(stationName, spectrum.getObserverPosition(), "DSM");
            PartialType partialType = PartialType.valueOf(spcname.getFileType().toString());
            DSMOutput qSpectrum = null;
            if (spcname.getFileType() == SpcFileType.PAR2 && partialTypes.contains(PartialType.PARQ)) {
                qSpectrum = fujiConversion.convert(spectrum);
                process(qSpectrum);
            }
            process(spectrum);

            for (SACComponent component : components) {
                Set<TimewindowInformation> tw = timewindowInformationSet.stream()
                        .filter(info -> info.getStation().getName().equals(stationName))
                        .filter(info -> info.getGlobalCMTID().equals(id))
                        .filter(info -> info.getComponent() == component).collect(Collectors.toSet());

                if (tw.isEmpty()) continue;

                for (int k = 0; k < spectrum.nbody(); k++) {
                    double bodyR = spectrum.getBodyR()[k];
                    boolean exists = false;
                    for (double r : Partial1DDatasetMaker.this.bodyR)
                        if (r == bodyR) exists = true;
                    if (!exists) continue;
                    double[] ut = spectrum.getSpcBodyList().get(k).getSpcComponent(component).getTimeseries();
                    // applying the filter
                    double[] filteredUt = filter.applyFilter(ut);
                    for (TimewindowInformation t : tw)
                        cutAndWrite(station, filteredUt, t, bodyR, partialType);
                }
                if (qSpectrum != null) for (int k = 0; k < spectrum.nbody(); k++) {
                    double bodyR = spectrum.getBodyR()[k];
                    boolean exists = false;
                    for (double r : Partial1DDatasetMaker.this.bodyR)
                        if (r == bodyR) exists = true;
                    if (!exists) continue;
                    double[] ut = qSpectrum.getSpcBodyList().get(k).getSpcComponent(component).getTimeseries();
                    // applying the filter
                    double[] filteredUt = filter.applyFilter(ut);
                    for (TimewindowInformation t : tw)
                        cutAndWrite(station, filteredUt, t, bodyR, PartialType.PARQ);
                }
            }
        }

        /**
         * @param u                     partial waveform
         * @param timewindowInformation cut information
         * @return u cut by considering sampling Hz
         */
        private double[] sampleOutput(double[] u, TimewindowInformation timewindowInformation) {
            int cutstart = (int) (timewindowInformation.getStartTime() * partialSamplingHz);
            // 書きだすための波形
            int outnpts = (int) ((timewindowInformation.getEndTime() - timewindowInformation.getStartTime()) *
                    finalSamplingHz);
            double[] sampleU = new double[outnpts];
            // cutting a waveform for outputting
            Arrays.setAll(sampleU, j -> u[cutstart + j * step]);

            return sampleU;
        }

    }

}
