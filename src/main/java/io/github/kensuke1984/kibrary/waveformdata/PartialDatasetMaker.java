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
import org.apache.commons.math3.complex.Complex;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * shfp、shbp、psvfp,psvbp から作られたSPCファイルがあるディレクトリの操作
 * <p>
 * fpDIR, bpDIR(未定義の場合workDir)の下に イベントごとのフォルダ（FP）、ステーションごとのフォルダ(BP)があり その下の
 * modelNameにspcfileをおく
 * <p>
 * イベントフォルダはeventnameで定義されたもの ステーショフォルダは0000(stationname)
 * <p>
 * spcfileの名前は (point name).(station or eventname).(PB or PF)...(sh or psv).spc
 * <p>
 * halfDurationはevent informationファイルから読み取る
 * <p>
 * time window informationファイルの中からtime windowを見つける。 その中に入っている震源観測点成分の組み合わせのみ計算する
 * <p>
 * バンドパスをかけて保存する
 * <p>
 * TODO station とかの書き出し
 * <p>
 * 例： directory/19841006/*spc directory/0000KKK/*spc
 * <p>
 * 摂動点の情報がない摂動点に対しては計算しない
 * <p>
 * <b>Assume there are no stations with the same name and different networks</b>
 * TODO
 * <p>
 * Because of DSM condition, stations can not have the same name...
 *
 * @author Kensuke Konishi
 * @version 2.3.1.2
 */
public class PartialDatasetMaker implements Operation {

    private Set<SACComponent> components;

    /**
     * time length (DSM parameter)
     */
    private double tlen;

    /**
     * step of frequency domain (DSM parameter)
     */
    private int np;

    /**
     * BPinfo このフォルダの直下に 0000????を置く
     */
    private Path bpPath;
    /**
     * FPinfo このフォルダの直下に イベントフォルダ（FP）を置く
     */
    private Path fpPath;

    /**
     * bp, fp フォルダの下のどこにspcファイルがあるか 直下なら何も入れない（""）
     */
    private String modelName;

    /**
     * タイムウインドウ情報のファイル
     */
    private Path timewindowPath;
    /**
     * Information file about locations of perturbation points.
     */
    private Path perturbationPath;

    /**
     * set of partial type for computation
     */
    private Set<PartialType> partialTypes;

    /**
     * bandpassの最小周波数（Hz）
     */
    private double minFreq;

    /**
     * bandpassの最大周波数（Hz）
     */
    private double maxFreq;

    private Properties property;
    private Path workPath;
    /**
     * spcFileをコンボリューションして時系列にする時のサンプリングHz デフォルトは２０ TODOまだ触れない
     */
    private double partialSamplingHz = 20;
    /**
     * 最後に時系列で切り出す時のサンプリングヘルツ(Hz)
     */
    private double finalSamplingHz;
    /**
     * structure for Q partial
     */
    private PolynomialStructure structure;
    /**
     * 0:none, 1:boxcar, 2:triangle.
     */
    private int sourceTimeFunction;
    /**
     * The folder contains source time functions.
     */
    private Path sourceTimeFunctionPath;
    private ButterworthFilter filter;
    /**
     * バンドパスを安定させるためwindowを左右に ext = max period(s) ずつ伸ばす
     */
    private int ext;
    /**
     * sacdataを何ポイントおきに取り出すか
     */
    private int step;
    private Set<TimewindowInformation> timewindowInformation;
    private Set<GlobalCMTID> touchedSet = new HashSet<>();
    // TODO
    private Set<Station> stationSet;
    private Set<GlobalCMTID> idSet;
    private double[][] periodRanges;
    private Set<Location> perturbationLocationSet;
    private String dateString;
    private WaveformDataWriter partialDataWriter;
    private Path logPath;
    private long startTime = System.nanoTime();
    private long endTime;
    private Map<GlobalCMTID, SourceTimeFunction> userSourceTimeFunctions;

    public PartialDatasetMaker(Properties property) throws IOException {
        this.property = (Properties) property.clone();
        set();
    }

    public static void writeDefaultPropertiesFile() throws IOException {
        Path outPath = Paths.get(PartialDatasetMaker.class.getName() + Utilities.getTemporaryString() + ".properties");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
            pw.println("manhattan PartialDatasetMaker");
            pw.println("##Path of a working folder (.)");
            pw.println("#workPath");
            pw.println("##SacComponents to be used (Z R T)");
            pw.println("#components");
            pw.println("##Path of a back propagate spc folder (BPinfo)");
            pw.println("#bpPath");
            pw.println("##Path of a forward propagate spc folder (FPinfo)");
            pw.println("#fpPath");
            pw.println("##String if 'modelName' is PREM, spectrum files in 'eventDir/PREM' are used.");
            pw.println("##If it is unset, then automatically set as the name of the folder in the eventDirs");
            pw.println("##but the eventDirs can have only one folder inside and they must be same.");
            pw.println("#modelName");
            pw.println("##Type source time function 0:none, 1:boxcar, 2:triangle. (0)");
            pw.println("##or folder name containing *.stf if you want to your own GLOBALCMTID.stf ");
            pw.println("#sourceTimeFunction");
            pw.println("##Path of a time window file, must be set");
            pw.println("#timewindowPath timewindow.dat");
            pw.println("##PartialType[] compute types (MU)");
            pw.println("#partialTypes");
            pw.println("##double time length DSM parameter TLEN, must be set");
            pw.println("#TLEN 3276.8");
            pw.println("##int step of frequency domain DSM parameter NP, must be set");
            pw.println("#NP 512");
            pw.println("##double minimum value of passband (0.005)");
            pw.println("#minFreq");
            pw.println("##double maximum value of passband (0.08)");
            pw.println("#maxFreq");
            pw.println("#double (20)");
            pw.println("#partialSamplingHz cant change now");
            pw.println("##double SamplingHz in write dataset (1)");
            pw.println("#finalSamplingHz");
            pw.println("##perturbationPath, must be set");
            pw.println("#perturbationPath perturbationPoint.inf");
            pw.println("##File for Qstructure (if no file, then PREM)");
            pw.println("#qinf");
        }
        System.err.println(outPath + " is created.");
    }

    /**
     * @param args [parameter file name]
     * @throws IOException if any
     */
    public static void main(String[] args) throws IOException {
        PartialDatasetMaker pdm = new PartialDatasetMaker(Property.parse(args));
        long startTime = System.nanoTime();

        System.err.println(PartialDatasetMaker.class.getName() + " is going..");
        pdm.run();
        System.err.println(PartialDatasetMaker.class.getName() + " finished in " +
                Utilities.toTimeString(System.nanoTime() - startTime));
    }

    @Override
    public Properties getProperties() {
        return (Properties) property.clone();
    }

    @Override
    public Path getWorkPath() {
        return workPath;
    }

    private void checkAndPutDefaults() {
        if (!property.containsKey("workPath")) property.setProperty("workPath", "");
        if (!property.containsKey("components")) property.setProperty("components", "Z R T");
        if (!property.containsKey("bpPath")) property.setProperty("bpPath", "BPinfo");
        if (!property.containsKey("fpPath")) property.setProperty("fpPath", "FPinfo");
        if (!property.containsKey("modelName")) property.setProperty("modelName", "");
        if (!property.containsKey("maxFreq")) property.setProperty("maxFreq", "0.08");
        if (!property.containsKey("minFreq")) property.setProperty("minFreq", "0.005");
        // if (!property.containsKey("backward")) TODO allow user to change
        // property.setProperty("backward", "true");partialSamplingHz
        if (!property.containsKey("sourceTimeFunction")) property.setProperty("sourceTimeFunction", "0");
        if (!property.containsKey("partialTypes")) property.setProperty("partialTypes", "MU");
        if (!property.containsKey("partialSamplingHz")) property.setProperty("partialSamplingHz", "20");
        if (!property.containsKey("finalSamplingHz")) property.setProperty("finalSamplingHz", "1");
    }

    /**
     * set parameter
     */
    private void set() throws IOException {
        checkAndPutDefaults();
        workPath = Paths.get(property.getProperty("workPath"));

        if (!Files.exists(workPath)) throw new RuntimeException("The workPath: " + workPath + " does not exist");

        bpPath = getPath("bpPath");
        fpPath = getPath("fpPath");
        timewindowPath = getPath("timewindowPath");
        components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
                .collect(Collectors.toSet());

        if (property.containsKey("qinf")) structure = new PolynomialStructure(getPath("qinf"));
        try {
            sourceTimeFunction = Integer.parseInt(property.getProperty("sourceTimeFunction"));
        } catch (Exception e) {
            sourceTimeFunction = -1;
            sourceTimeFunctionPath = getPath("sourceTimeFunction");
        }

        partialTypes = Arrays.stream(property.getProperty("partialTypes").split("\\s+")).map(PartialType::valueOf)
                .collect(Collectors.toSet());
        tlen = Double.parseDouble(property.getProperty("TLEN"));
        np = Integer.parseInt(property.getProperty("NP"));
        minFreq = Double.parseDouble(property.getProperty("minFreq"));
        maxFreq = Double.parseDouble(property.getProperty("maxFreq"));
        perturbationPath = getPath("perturbationPath");
        // partialSamplingHz
        // =Double.parseDouble(reader.getFirstValue("partialSamplingHz")); TODO

        finalSamplingHz = Double.parseDouble(property.getProperty("finalSamplingHz"));
    }

    /**
     * TODO check
     * Automatically find a model folder.
     * Runtime exception occurs when fp or bp folders have more than 1 possible model folder.
     * If a model name is already set in an information file, check if the name is valid.
     *
     * @throws IOException if any
     */
    private void setModelName() throws IOException {
        modelName = property.getProperty("modelName");
        Set<Path> bpFolderSet =
                stationSet.stream().map(s -> bpPath.resolve("0000" + s.getName())).collect(Collectors.toSet());
        Set<EventFolder> eventFolders = Utilities.eventFolderSet(fpPath);
        if (modelName.isEmpty()) {
            Set<String> possible =
                    bpFolderSet.stream().flatMap(b -> Arrays.stream(b.toFile().listFiles(File::isDirectory)))
                            .map(File::getName).collect(Collectors.toSet());
            if (possible.size() != 1) throw new RuntimeException(
                    "There are no model folder in bp folders or more than one folder. You must specify 'modelName' in the case.");
            modelName = possible.iterator().next();
            Set<String> possibleNames =
                    eventFolders.stream().flatMap(ef -> Arrays.stream(ef.listFiles(File::isDirectory)))
                            .map(File::getName).collect(Collectors.toSet());
            if (possibleNames.size() != 1) throw new RuntimeException(
                    "There are no model folder in fp folders or more than one folder. You must specify 'modelName' in the case.");
            if (!modelName.equals(possibleNames.iterator().next()))
                throw new RuntimeException("No valid model folder.");
        }
        if (!eventFolders.stream().map(EventFolder::toPath).map(p -> p.resolve(modelName)).allMatch(Files::exists) ||
                !bpFolderSet.stream().map(p -> p.resolve(modelName)).allMatch(Files::exists))
            throw new RuntimeException("There are some events without model folder " + modelName);
        System.err.println("Model name is set " + modelName);
    }

    private void setLog() throws IOException {
        synchronized (PartialDatasetMaker.class) {
            do {
                dateString = Utilities.getTemporaryString();
                logPath = workPath.resolve("pdm" + dateString + ".log");
            } while (Files.exists(logPath));
            Files.createFile(logPath);
        }
    }

    private void setOutput() throws IOException {
        Path idPath = workPath.resolve("partialID" + dateString + ".dat");
        Path datasetPath = workPath.resolve("partial" + dateString + ".dat");

        partialDataWriter =
                new WaveformDataWriter(idPath, datasetPath, stationSet, idSet, periodRanges, perturbationLocationSet);
        writeLog("Creating " + idPath + " " + datasetPath);
        System.out.println("Creating " + idPath + " " + datasetPath);
    }

    private void readPerturbationPoints() throws IOException {
        try (Stream<String> lines = Files.lines(perturbationPath)) {
            perturbationLocationSet = lines.map(line -> line.split("\\s+"))
                    .map(parts -> new Location(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                            Double.parseDouble(parts[2]))).collect(Collectors.toSet());
        }
    }

    @Override
    public void run() throws IOException {
        setLog();
        int N_THREADS = Runtime.getRuntime().availableProcessors();
        writeLog("Running " + N_THREADS + " threads");
        setTimeWindow();
        setModelName();

        // filter設計
        setBandPassFilter();
        // read a file for perturbation points.
        readPerturbationPoints();

        // バンドパスを安定させるためwindowを左右に ext = max period(s) ずつ伸ばす
        ext = (int) (1 / minFreq * partialSamplingHz);

        // sacdataを何ポイントおきに取り出すか
        step = (int) (partialSamplingHz / finalSamplingHz);
        setOutput();
        int bpnum = 0;
        setSourceTimeFunctions();
        // bpフォルダごとにスタート
        for (Station station : stationSet) {
            Path bp0000Path = bpPath.resolve("0000" + station.getName());
            Path bpModelPath = bp0000Path.resolve(modelName);

            // Set of global cmt IDs for the station in the timewindow.
            Set<GlobalCMTID> idSet =
                    timewindowInformation.stream().filter(info -> components.contains(info.getComponent()))
                            .filter(info -> info.getStation().equals(station))
                            .map(TimewindowInformation::getGlobalCMTID).collect(Collectors.toSet());

            if (idSet.isEmpty()) continue;

            // spectral files in bpModelFolder
            Set<SPCFile> bpFiles = Utilities.collectSpcFileName(bpModelPath);
            System.out.println(bpFiles.size() + " bpfiles are found");

            // stationに対するタイムウインドウが存在するfp内のmodelフォルダ
            Path[] fpEventPaths = idSet.stream().map(id -> fpPath.resolve(id + "/" + modelName)).filter(Files::exists)
                    .toArray(Path[]::new);

            int donebp = 0;
            // foreach bpfile in bpfolder
            for (SPCFile bpname : bpFiles) {
                // create ThreadPool
                ExecutorService execs = Executors.newFixedThreadPool(N_THREADS);
                System.err.println("Working for " + bpname.getName() + " " + ++donebp + "/" + bpFiles.size());

                DSMOutput bp = bpname.read();
                String pointName = bp.getObserverID();

                // timewindowの存在するfpdirに対して
                // bpファイルに対する全てのfpファイルを
                for (Path fpEventPath : fpEventPaths) {
                    String eventName = fpEventPath.getParent().getFileName().toString();
                    SPCFile fpfile = new FormattedSPCFile(
                            fpEventPath.resolve(pointName + "." + eventName + ".PF..." + bpname.getMode() + ".spc"));
                    if (!fpfile.exists()) continue;
                    PartialComputation pc = new PartialComputation(bp, station, fpfile);
                    execs.execute(pc);
                }
                execs.shutdown();
                while (!execs.isTerminated()) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                partialDataWriter.flush();
                System.out.println();
            }
            writeLog(+bpnum++ + "th " + bp0000Path + " was done ");
        }
        terminate();
    }

    private void setSourceTimeFunctions() throws IOException {
        if (sourceTimeFunction == 0) return;
        if (sourceTimeFunction == -1) {
            readSourceTimeFunctions();
            return;
        }
        userSourceTimeFunctions = new HashMap<>();
        idSet.forEach(id -> {
            double halfDuration = id.getEvent().getHalfDuration();
            SourceTimeFunction stf = sourceTimeFunction == 1 ?
                    SourceTimeFunction.boxcarSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration) :
                    SourceTimeFunction.boxcarSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration);
            userSourceTimeFunctions.put(id, stf);
        });

    }

    private void readSourceTimeFunctions() throws IOException {
        userSourceTimeFunctions = idSet.stream().collect(Collectors.toMap(id -> id, id -> {
            try {
                Path sourceTimeFunctionPath = this.sourceTimeFunctionPath.resolve(id + ".stf");
                return SourceTimeFunction.readSourceTimeFunction(sourceTimeFunctionPath);
            } catch (Exception e) {
                throw new RuntimeException("Source time function file for " + id + " is broken.");
            }
        }));

    }

    private void terminate() throws IOException {
        partialDataWriter.close();
        endTime = System.nanoTime();
        long nanoSeconds = endTime - startTime;
        String endLine = "Everything is done in " + Utilities.toTimeString(nanoSeconds) + ". Over n out! ";
        System.err.println(endLine);
        writeLog(endLine);
        writeLog(partialDataWriter.getIDPath() + " " + partialDataWriter.getDataPath() + " were created");
    }

    private synchronized void writeLog(String line) throws IOException {
        try (PrintWriter pw = new PrintWriter(
                Files.newBufferedWriter(logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
            pw.print(new Date() + " : ");
            pw.println(line);
        }
    }

    private void setBandPassFilter() throws IOException {
        System.err.println("Designing filter.");
        double omegaH = maxFreq * 2 * Math.PI / partialSamplingHz;
        double omegaL = minFreq * 2 * Math.PI / partialSamplingHz;
        filter = new BandPassFilter(omegaH, omegaL, 4);
        writeLog(filter.toString());
        periodRanges = new double[][]{{1 / maxFreq, 1 / minFreq}};
    }

    /**
     * Reads timewindow information
     *
     * @throws IOException if any
     */
    private void setTimeWindow() throws IOException {
        // タイムウインドウの情報を読み取る。
        System.err.println("Reading timewindow information");
        timewindowInformation = TimewindowInformationFile.read(timewindowPath);
        idSet = new HashSet<>();
        stationSet = new HashSet<>();
        timewindowInformation.forEach(t -> {
            idSet.add(t.getGlobalCMTID());
            stationSet.add(t.getStation());
        });

        // TODO
        if (stationSet.size() != stationSet.stream().map(Station::getName).distinct().count())
            throw new RuntimeException("Station duplication...");

        boolean fpExistence = idSet.stream().allMatch(id -> Files.exists(fpPath.resolve(id.toString())));
        boolean bpExistence = stationSet.stream().allMatch(station -> Files.exists(bpPath.resolve("0000" + station)));
        System.out.println("fpPath is " + fpPath + "   existance is " + fpExistence);
        System.out.println("idSet " + idSet.size());
        System.out.println("bpPath is " + bpPath + "   existance is " + fpExistence);
        if (!fpExistence || !bpExistence)
            throw new RuntimeException("propagation spectors are not enough for " + timewindowPath);
        writeLog(timewindowInformation.size() + " timewindows are found in " + timewindowPath + ". " + idSet.size() +
                " events and " + stationSet.size() + " stations.");
    }

    /**
     * 一つのBackPropagationに対して、あるFPを与えた時の計算をさせるスレッドを作る
     */
    private class PartialComputation implements Runnable {

        private DSMOutput bp;
        private SPCFile fpname;
        private DSMOutput fp;
        private Station station;
        private GlobalCMTID id;

        /**
         * @param bp     back propagate
         * @param fpFile forward propagate
         */
        private PartialComputation(DSMOutput bp, Station station, SPCFile fpFile) {
            this.bp = bp;
            this.station = station;
            fpname = fpFile;
            id = new GlobalCMTID(fpname.getSourceID());
        }

        /**
         * cut partial derivative in [start-ext, start+ext] The ext is for
         * filtering .
         *
         * @param u                     waveform
         * @param timewindowInformation time window information
         * @return cut waveform
         */
        private Complex[] cutPartial(double[] u, TimewindowInformation timewindowInformation) {
            int cutstart = (int) (timewindowInformation.getStartTime() * partialSamplingHz) - ext;
            // cutstartが振り切れた場合0 からにする
            if (cutstart < 0) return null;
            int cutend = (int) (timewindowInformation.getEndTime() * partialSamplingHz) + ext;
            Complex[] cut = new Complex[cutend - cutstart];
            Arrays.parallelSetAll(cut, i -> new Complex(u[i + cutstart]));
            return cut;
        }

        private double[] sampleOutput(Complex[] u, TimewindowInformation timewindowInformation) {
            // 書きだすための波形
            int outnpts = (int) ((timewindowInformation.getEndTime() - timewindowInformation.getStartTime()) *
                    finalSamplingHz);
            double[] sampleU = new double[outnpts];

            // cutting a waveform for outputting
            Arrays.parallelSetAll(sampleU, j -> u[ext + j * step].getReal());
            return sampleU;
        }

        private SourceTimeFunction getSourceTimeFunction() {
            return sourceTimeFunction == 0 ? null : userSourceTimeFunctions.get(id);
        }

        @Override
        public void run() {
            String stationName = bp.getSourceID();
            if (!station.getPosition().toLocation(0).equals(bp.getSourceLocation()))
                throw new RuntimeException("There may be a station with the same name but other networks.");

            if (bp.tlen() != tlen || bp.np() != np)
                throw new RuntimeException("BP for " + station + " has invalid TLEN or NP.");
            GlobalCMTID id = new GlobalCMTID(fpname.getSourceID());

            touchedSet.add(id);

            // Pickup timewindows
            Set<TimewindowInformation> timewindowList =
                    timewindowInformation.stream().filter(info -> info.getStation().getName().equals(stationName))
                            .filter(info -> info.getGlobalCMTID().equals(id)).collect(Collectors.toSet());

            // timewindow情報のないときスキップ
            if (timewindowList.isEmpty()) return;

            // System.out.println("I am " + Thread.currentThread().getName());
            try {
                fp = fpname.read();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            ThreeDPartialMaker threedPartialMaker = new ThreeDPartialMaker(fp, bp);
            threedPartialMaker.setSourceTimeFunction(getSourceTimeFunction());
            if (structure != null) threedPartialMaker.setStructure(structure);

            // partial for ith radius
            for (int ibody = 0, nbody = fp.nbody(); ibody < nbody; ibody++) {
                // とりあえずtransverse（２）成分についての名前
                Location location = fp.getObserverPosition().toLocation(fp.getBodyR()[ibody]);
                for (PartialType type : partialTypes)
                    for (SACComponent component : components) {
                        if (timewindowList.stream().noneMatch(info -> info.getComponent() == component)) continue;
                        double[] partial = threedPartialMaker.createPartial(component, ibody, type);
                        timewindowList.stream().filter(info -> info.getComponent() == component).forEach(info -> {
                            Complex[] u = cutPartial(partial, info);
                            u = filter.applyFilter(u);
                            double[] cutU = sampleOutput(u, info);
                            PartialID pid = new PartialID(station, id, component, finalSamplingHz, info.getStartTime(),
                                    cutU.length, 1 / maxFreq, 1 / minFreq, 0, sourceTimeFunction != 0, location, type,
                                    cutU);
                            try {
                                partialDataWriter.addPartialID(pid);
                                System.err.print(".");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
            }
        }
    }
}
