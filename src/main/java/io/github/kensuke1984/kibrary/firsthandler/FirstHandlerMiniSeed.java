package io.github.kensuke1984.kibrary.firsthandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.util.Utilities;

/**
 * Java version First handler ported from the perl software.<br>
 * Processes extraction along the information file.
 * This extracts {@link SEEDFile}s under a
 * working golder
 * <p>
 * <p>
 * <p>
 * 解凍後の.seed ファイルをまとめる rdseed -fRd rtrend seed解凍後 channelが[BH]H[ENZ]のものだけから読み込む
 * <p>
 * <a href=http://ds.iris.edu/ds/nodes/dmc/manuals/rdseed/>rdseed</a> and <a
 * href=http://ds.iris.edu/ds/nodes/dmc/manuals/evalresp/>evalresp</a> must be
 * in PATH. </p> If you want to remove intermediate files.
 * <p>
 * <p>
 * TODO NPTSで合わないものを捨てる？
 * <p>
 * Even if a seed file contains both BH? and HH?, it will not throw errors,
 * however, no one knows which channel is used for extraction until you see the
 * intermediate files. If you want to see them, you have to leave the
 * intermediate files explicitly.
 *
 * @author Kensuke Konishi
 * @version 0.2.1.3
 */
public class FirstHandlerMiniSeed implements Operation {
    public static void writeDefaultPropertiesFile() throws IOException {
        Path outPath = Paths.get(FirstHandlerMiniSeed.class.getName() + Utilities.getTemporaryString() + ".properties");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
            pw.println("manhattan FirstHandler");
            pw.println("##Path of a working folder (.)");
            pw.println("#workPath");
            pw.println("##String a name of catalog to use from [cmt, pde]  (cmt)");
            pw.println("#catalog  CANT CHANGE NOW"); // TODO
            pw.println("##double Sampling Hz, can not be changed now (20)");
            pw.println("#samplingHz");
            pw.println("##epicentral distance range Min(0) Max(180)");
            pw.println("#epicentralDistanceMin");
            pw.println("#epicentralDistanceMax");
            pw.println("##boolean if it is true, remove intermediate files (true)");
            pw.println("#removeIntermediateFile");
        }
        System.err.println(outPath + " is created.");
    }

    public FirstHandlerMiniSeed(Properties property) {
        this.property = (Properties) property.clone();
        set();
    }

    private void checkAndPutDefaults() {
        if (!property.containsKey("workPath")) property.setProperty("workPath", "");
        if (!property.containsKey("catalog")) property.setProperty("catalog", "cmt");
        if (!property.containsKey("samplingHz")) property.setProperty("samplingHz", "20"); // TODO
        if (!property.containsKey("removeIntermediateFile")) property.setProperty("removeIntermediateFile", "true");
    }

    /**
     * parameterのセット
     */
    private void set() {
        checkAndPutDefaults();
        workPath = Paths.get(property.getProperty("workPath"));
        if (!Files.exists(workPath)) throw new RuntimeException("The workPath: " + workPath + " does not exist");
        switch (property.getProperty("catalog")) {
            case "cmt":
            case "CMT":
                catalog = 0;
                break;
            case "pde":
            case "PDE":
                catalog = 0;
                break;
            default:
                throw new RuntimeException("Invalid catalog name.");
        }
        removeIntermediateFile = Boolean.parseBoolean(property.getProperty("removeIntermediateFile"));
    }

    private double samplingHz;

    /**
     * which catalog to use 0:CMT 1: PDE
     */
    private int catalog;

    /**
     * if remove intermediate file
     */
    private boolean removeIntermediateFile;

    /**
     * output directory
     */
    private Path outPath;

    /**
     * @param args [parameter file name]
     */
    public static void main(String[] args) throws IOException {
        FirstHandlerMiniSeed fh = new FirstHandlerMiniSeed(Property.parse(args));
        long startT = System.nanoTime();
        System.err.println(FirstHandlerMiniSeed.class.getName() + " is going");
        fh.run();
        System.err.println(
                FirstHandlerMiniSeed.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - startT));
    }

    @Override
    public void run() throws IOException {
        System.err.println("Working directory is " + workPath);
        // check if conditions. if for example there are already existing output
        // files, this program starts here,
        if (!Files.exists(workPath)) throw new NoSuchFileException(workPath.toString());
        outPath = workPath.resolve("fh" + Utilities.getTemporaryString());
        Path goodSeedPath = outPath.resolve("goodSeeds");
        Path badSeedPath = outPath.resolve("badSeeds");
        Path ignoredSeedPath = outPath.resolve("ignoredSeeds");

        Set<Path> miniSeedPaths = findMiniSeedFiles();
        System.err.println(miniSeedPaths.size() + " miniSeeds are found.");
        if (miniSeedPaths.isEmpty()) return;

        // creates environment (make output folder ...)
        Files.createDirectories(outPath);
        System.err.println("Output directory is " + outPath);

        Set<MiniSeedSAC> miniSeedSacs = miniSeedPaths.stream().map(seedPath -> {
            try {
                return new MiniSeedSAC(seedPath, outPath);
            } catch (Exception e) {
                try {
                    System.err.println(seedPath + " has problem. " + e);
                    Utilities.moveToDirectory(seedPath, ignoredSeedPath, true);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());

        miniSeedSacs.forEach(ss -> ss.setRemoveIntermediateFiles(removeIntermediateFile));

        int threadNum = Runtime.getRuntime().availableProcessors();
        ExecutorService es = Executors.newFixedThreadPool(threadNum);

        miniSeedSacs.forEach(es::submit);

        es.shutdown();
        try {
            while (!es.isTerminated()) Thread.sleep(1000 * 5);
        } catch (Exception e2) {
            e2.printStackTrace();
        }

        for (MiniSeedSAC miniSeedSac : miniSeedSacs)
            try {
                if (miniSeedSac == null) continue;
                if (!miniSeedSac.hadRun()) Utilities.moveToDirectory(miniSeedSac.getSeedPath(), ignoredSeedPath, true);
                else if (miniSeedSac.hasProblem()) Utilities.moveToDirectory(miniSeedSac.getSeedPath(), badSeedPath, true);
                else Utilities.moveToDirectory(miniSeedSac.getSeedPath(), goodSeedPath, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private Set<Path> findMiniSeedFiles() throws IOException {
        try (Stream<Path> workPathStream = Files.list(workPath)) {
            return workPathStream.filter(path -> path.toString().endsWith(".mseed")).collect(Collectors.toSet());
        }

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

}
