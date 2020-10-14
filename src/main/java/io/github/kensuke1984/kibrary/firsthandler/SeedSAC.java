package io.github.kensuke1984.kibrary.firsthandler;

import io.github.kensuke1984.kibrary.external.SAC;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTSearch;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;
import io.github.kensuke1984.kibrary.util.sac.SACUtil;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

/**
 * Class for extracting a seed file. It creates SAC files from the seed file.
 * <p>
 * This class requires that rdseed, evalresp and sac exists in your PATH.
 * The software
 * <a href=https://ds.iris.edu/ds/nodes/dmc/software/downloads/rdseed/>rdseed</a>,
 * <a href=https://ds.iris.edu/ds/nodes/dmc/software/downloads/evalresp/>evalresp</a> and
 * <a href=https://ds.iris.edu/ds/nodes/dmc/software/downloads/sac/>SAC</a> can be found in IRIS.
 *
 * @author Kensuke Konishi
 * @version 0.1.9
 */
class SeedSAC implements Runnable {
	
    /**
     * [s] delta for SAC files. SAC files with different delta will be interpolated
     * or downsampled.
     */
    private static final double delta = 0.05;
    /**
     * [Hz] Sampling Hz in write SAC files
     */
    private static final double samplingHz = 20;
    /**
     * if remove intermediate files
     */
    private boolean removeIntermediateFiles = true;
    /**
     * If cmpMod is true, this modifies delta, cmpinc &amp; cmpaz
     */
    private final boolean CMPMOD = true;
    /**
     * GlobalCMTData for the event in the seedfile
     */
    private GlobalCMTData event;
    /**
     * where output goes.
     */
    private final EventFolder EVENT_DIR;
    /**
     * seed file to process
     */
    private final SEEDFile SEED_FILE;
    /**
     * event ID
     */
    private GlobalCMTID id;
    /**
     * [deg] Minimum epicentral distance of SAC files to be output
     */
    private final double MINIMUM_EPICENTRAL_DISTANCE = 0;
    /**
     * [deg] Maximum epicentral distance of SAC files to be output
     */
    private final double MAXIMUM_EPICENTRAL_DISTANCE = 180;

    private final boolean EVENT_DIR_ALREADY_EXIST;
    /**
     * true: the base time will be PDE time, false: CMT (default)
     */
    private boolean byPDE;
    private boolean hadRun;
    /**
     * true: exception has occurred, false: not
     */
    private boolean problem;

    /**
     * @param seedPath            to be extracted from
     * @param outputDirectoryPath Path where extracted files are placed
     * @throws IOException if the outputDirectoryPath already has events which also
     *                     exists in the seed file or an error occurs
     */
    SeedSAC(Path seedPath, Path outputDirectoryPath) throws IOException {
        this(seedPath, outputDirectoryPath, null);
    }

    /**
     * seed file to extract
     *
     * @param seedPath            SEED file to extract
     * @param outputDirectoryPath inside this folder, the seed file is extracted. If the folder
     *                            does not exist, it will be created.
     * @param id                  global cmt id
     * @throws IOException If the folder already has event folders which also exists in
     *                     the seed file.
     */
    SeedSAC(Path seedPath, Path outputDirectoryPath, GlobalCMTID id) throws IOException {
        SEED_FILE = new SEEDFile(seedPath);
        if (id != null) this.id = id;
        else setID();
        if (!idValidity()) throw new RuntimeException("The ID " + this.id + " is invalid for " + seedPath);

        Files.createDirectories(outputDirectoryPath);
        EVENT_DIR = new EventFolder(outputDirectoryPath.resolve(this.id.toString()));

        if (EVENT_DIR.exists()) EVENT_DIR_ALREADY_EXIST = false;
        else if (!EVENT_DIR.mkdirs()) throw new RuntimeException("Can't create " + EVENT_DIR);
        else EVENT_DIR_ALREADY_EXIST = true;
        SEED_FILE.createLink(EVENT_DIR.toPath());
    }

    /**
     * Set DELTA in a sac file to {@link #delta}. (mostly it is down sampling.)
     * Change cmpaz cmpinc BHN BHE BHZ のときはcmpaz cmpincを変更する
     *
     * @param sacPath Path of a file to fix
     * @throws IOException if an I/O error occurs
     */
    private static void fixDelta(Path sacPath) throws IOException {
        try (SAC sacD = SAC.createProcess()) {
            String cwd = sacPath.getParent().toString();
            sacD.inputCMD("cd " + cwd);// set current directory
            sacD.inputCMD("r " + sacPath.getFileName());// read
            sacD.inputCMD("ch lovrok true");// overwrite permission
            if (sacPath.toString().contains(".BHN.") || sacPath.toString().contains(".BLN."))
                sacD.inputCMD("ch cmpaz 0 cmpinc 90");
            else if (sacPath.toString().contains(".BHE.") || sacPath.toString().contains(".BLE."))
                sacD.inputCMD("ch cmpaz 90 cmpinc 90");
            else if (sacPath.toString().contains(".BHZ.") || sacPath.toString().contains(".BLZ."))
                sacD.inputCMD("ch cmpinc 0");
            sacD.inputCMD("interpolate delta " + delta);
            sacD.inputCMD("w over");
        }
    }

    Path getSeedPath() {
        return SEED_FILE.getSeedPath();
    }

    /**
     * If true then all intermediate files will be removed at the end.
     *
     * @param b set {@link #removeIntermediateFiles}
     */
    void setRemoveIntermediateFiles(boolean b) {
        removeIntermediateFiles = b;
    }

    /**
     * set GlobalCMTID
     */
    private void setID() {
        // try to find id in the name of the file
        id = findIDinFilename();
        if (id != null) return;
        if (GlobalCMTID.isGlobalCMTID(SEED_FILE.getVolumeLabel())) {
            id = new GlobalCMTID(SEED_FILE.getVolumeLabel());
            return;
        }
        System.err.println("Dataset in this seed file starts " + SEED_FILE.getStartingDate());
        GlobalCMTSearch sc = new GlobalCMTSearch(SEED_FILE.getStartingDate(), SEED_FILE.getEndingDate());
        id = sc.select();
        Objects.requireNonNull(id, "There is no event in the global CMT catalog.");
    }

    /**
     * TODO use volumeID in seed files
     *
     * @return look for GlobalCMTID in the name of the seed file otherwise
     * returns null
     */
    private GlobalCMTID findIDinFilename() {
        String fileName = SEED_FILE.getSeedPath().getFileName().toString();
        Matcher m1 = GlobalCMTID.RECENT_GLOBALCMTID_PATTERN.matcher(fileName);
        if (m1.find()) return new GlobalCMTID(m1.group());

        Matcher m0 = GlobalCMTID.PREVIOUS_GLOBALCMTID_PATTERN.matcher(fileName);
        return m0.find() ? new GlobalCMTID(m0.group()) : null;
    }

    /**
     * @return global cmt id が日付的に合っているかどうか （startが発震時刻より前かつendがCMT時刻より後かどうか）
     */
    private boolean idValidity() {
        event = id.getEvent();
        return event != null && id != null && SEED_FILE.getStartingDate().isBefore(event.getPDETime()) &&
                SEED_FILE.getEndingDate().isAfter(event.getCMTTime());
    }

    /**
     * Deconvolute instrument function for all the MOD files in the event folder.
     * 対応するRESPのevalrespに失敗したMODファイルはNOSPECTRAMODへ
     */
    private void deconvolute() {
        // System.out.println("Conducting deconvolution");
        Path noSpectraPath = EVENT_DIR.toPath().resolve("noSpectraOrInvalidMOD");
        Path duplicateChannelPath = EVENT_DIR.toPath().resolve("duplicateChannel");
        // evalresp後のRESP.*ファイルを移動する TODO メソッドを分ける
        Path respBoxPath = EVENT_DIR.toPath().resolve("resp");
        Path spectraBoxPath = EVENT_DIR.toPath().resolve("spectra");
        Path modBoxPath = EVENT_DIR.toPath().resolve("mod");
        try (DirectoryStream<Path> eventDirStream = Files.newDirectoryStream(EVENT_DIR.toPath(), "*.MOD")) {
            String resp = "RESP.";
            String spectra = "SPECTRA.";
            for (Path modPath : eventDirStream) {
                Map<SACHeaderEnum, String> headerMap = SACUtil.readHeader(modPath);
                String componentName = headerMap.get(SACHeaderEnum.KCMPNM);
                String respFileName =
                        resp + headerMap.get(SACHeaderEnum.KNETWK) + "." + headerMap.get(SACHeaderEnum.KSTNM) + "." +
                                headerMap.get(SACHeaderEnum.KHOLE) + "." + componentName;
                String spectraFileName =
                        spectra + headerMap.get(SACHeaderEnum.KNETWK) + "." + headerMap.get(SACHeaderEnum.KSTNM) + "." +
                                headerMap.get(SACHeaderEnum.KHOLE) + "." + componentName;
                Path spectraPath = EVENT_DIR.toPath().resolve(spectraFileName);
                Path respPath = EVENT_DIR.toPath().resolve(respFileName);
                String component;
                switch (componentName) {
                    case "BHE":
                    case "BLE":
                    case "HHE":
                    case "HLE":
                        component = "E";
                        break;
                    case "BHN":
                    case "BLN":
                    case "HHN":
                    case "HLN":
                        component = "N";
                        break;
                    case "BHZ":
                    case "BLZ":
                    case "HHZ":
                    case "HLZ":
                        component = "Z";
                        break;
                    case "BH1":
                    case "BL1":
                    case "HH1":
                    case "HL1":
                        component = "1";
                        break;
                    case "BH2":
                    case "BL2":
                    case "HH2":
                    case "HL2":
                        component = "2";
                        break;
                    default:
                        continue;
                }

                String afterName = headerMap.get(SACHeaderEnum.KSTNM) + "." + event + "." + component;
                Path afterPath = EVENT_DIR.toPath().resolve(afterName);

                // run evalresp
                // If it fails, throw MOD and RESP files to trash
                if (!runEvalresp(headerMap)) {
                    // throw MOD.* files which cannot produce SPECTRA to noSpectra
                    Utilities.moveToDirectory(modPath, noSpectraPath, true);
                    // throw RESP.* files which cannot produce SPECTRA to noSpectra
                    Utilities.moveToDirectory(respPath, noSpectraPath, true);
                    continue;
                }

                // run seedsac
                try {
                    int npts = Integer.parseInt(headerMap.get(SACHeaderEnum.NPTS));
                    // duplication of channel
                    if (Files.exists(afterPath)) {
                        // throw *.MOD files which cannot produce SPECTRA to duplicateChannelPath
                        Utilities.moveToDirectory(modPath, duplicateChannelPath, true);
                        // throw SPECTRA files which cannot produce SPECTRA to duplicateChannelPath
                        Utilities.moveToDirectory(spectraPath, duplicateChannelPath, true);
                        // throw RESP files which cannot produce SPECTRA to duplicateChannelPath
                        Utilities.moveToDirectory(respPath, duplicateChannelPath, true);
                        continue;
                    }
                    SACDeconvolution.compute(modPath, spectraPath, afterPath, samplingHz / npts, samplingHz);
                } catch (Exception e) {
                    // throw *.MOD files which cannot produce SPECTRA to noSpectraPath
                    Utilities.moveToDirectory(modPath, noSpectraPath, true);
                    // throw SPECTRA files which cannot produce SPECTRA to noSpectraPath
                    Utilities.moveToDirectory(spectraPath, noSpectraPath, true);
                    // throw RESP files which cannot produce SPECTRA to noSpectraPath
                    Utilities.moveToDirectory(respPath, noSpectraPath, true);
                    continue;
                }

                // move processed RESP files to respBox
                Utilities.moveToDirectory(respPath, respBoxPath, true);

                // move processed SPECTRA files to spectraBox
                Utilities.moveToDirectory(spectraPath, spectraBoxPath, true);

                // move processed MOD files to modBox
                Utilities.moveToDirectory(modPath, modBoxPath, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @return if already run or not
     */
    boolean hadRun() {
        return hadRun;
    }

    /**
     * Eliminates problematic files made by rdseed, such as ones with different delta, and merge split files.
     */
    private void mergeUnevenSac() throws IOException {
        // merge
        UnevenSACMerger u = new UnevenSACMerger(EVENT_DIR.toPath());
        u.merge();
        u.move();
    }

    /**
     * modify merged SAC files
     */
    private void modifySACs() throws IOException {
        // System.out.println("Modifying sac files in "
        // + eventDir.getAbsolutePath());
        Path trashBoxPath = EVENT_DIR.toPath().resolve("trash");
        Path mergedBoxPath = EVENT_DIR.toPath().resolve("merged");

        try (DirectoryStream<Path> sacPathStream = Files.newDirectoryStream(EVENT_DIR.toPath(), "*.SAC")) {
            for (Path sacPath : sacPathStream) {
                SACModifier sm = new SACModifier(event, sacPath, byPDE);

                // TODO 00 01 "" duplication detect
                // header check khole e.t.c
                if (!sm.canInterpolate() || !sm.checkHeader()) {
                    Utilities.moveToDirectory(sacPath, trashBoxPath, true);
                    continue;
                }

                // remove trends in SAC files interpolate the files .SAC > .MOD
                sm.preprocess();

                sm.interpolate();

                sm.rebuild();

                // filter by distance
                if (!sm.checkEpicentralDistance(MINIMUM_EPICENTRAL_DISTANCE, MAXIMUM_EPICENTRAL_DISTANCE)) {
                    Utilities.moveToDirectory(sacPath, trashBoxPath, true);
                    continue;
                }

                // move SAC files after treatment in the merged folder
                Utilities.moveToDirectory(sacPath, mergedBoxPath, true);
            }
        }
    }

    /**
     * Convert/rotate all files with (.E, .N), (.1, .2) to (.R, .T).
     * successful files are put in rotatedNE the others are in nonrotatedNE
     */
    private void rotate() throws IOException {
        Path trashBox = EVENT_DIR.toPath().resolve("nonRotatedNE");
        Path neDir = EVENT_DIR.toPath().resolve("rotatedNE");
        try (DirectoryStream<Path> eStream = Files.newDirectoryStream(EVENT_DIR.toPath(), "*.E")) {
            for (Path ePath : eStream) {
                String[] parts = ePath.getFileName().toString().split("\\.");
                Path nPath = EVENT_DIR.toPath().resolve(parts[0] + "." + parts[1] + ".N");
                Path rPath = EVENT_DIR.toPath().resolve(parts[0] + "." + parts[1] + ".R");
                Path tPath = EVENT_DIR.toPath().resolve(parts[0] + "." + parts[1] + ".T");

                if (!Files.exists(nPath)) {
                    Utilities.moveToDirectory(ePath, trashBox, true);
                    continue;
                }
                boolean rotated = SACUtil.rotate(ePath, nPath, rPath, tPath);
                if (rotated) {
                    Utilities.moveToDirectory(nPath, neDir, true);
                    Utilities.moveToDirectory(ePath, neDir, true);
                } else {
                    Utilities.moveToDirectory(ePath, trashBox, true);
                    Utilities.moveToDirectory(nPath, trashBox, true);
                }
            }
        }

        // If there are files (.N) which had no pairs (.E), move them to trash
        try (DirectoryStream<Path> nPaths = Files.newDirectoryStream(EVENT_DIR.toPath(), "*.N")) {
            for (Path nPath : nPaths)
                Utilities.moveToDirectory(nPath, trashBox, true);
        }

    }

    /**
     * Adjusts delta, cmpinc and cmpaz in SAC files extracted by 'rdseed'
     * The SAC files are copied in 'rdseedOutputBackup' for backup.
     */
    private void preprocess() throws IOException {
        Path backupPath = EVENT_DIR.toPath().resolve("rdseedOutputBackup");
        Files.createDirectories(backupPath);
        try (DirectoryStream<Path> sacPaths = Files.newDirectoryStream(EVENT_DIR.toPath(), "*.SAC")) {
            for (Path sacPath : sacPaths) {
                Files.copy(sacPath, backupPath.resolve(sacPath.getFileName()));
                fixDelta(sacPath);
            }
        }

    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("It works only for one seed file.");
        Path seedPath = Paths.get(args[0]);
        if (!Files.exists(seedPath)) throw new NoSuchFileException(seedPath.toString());
        Path out = seedPath.resolveSibling("seedSAC" + Utilities.getTemporaryString());
        new SeedSAC(seedPath, out).run();
        System.err.println(seedPath + " is extracted in " + out);
    }

    @Override
    public void run() {
        if (!EVENT_DIR_ALREADY_EXIST) throw new RuntimeException("The condition is no good.");
        System.err.println("Opening " + SEED_FILE + " in " + EVENT_DIR.getPath());
        // run rdseed -q [output directory] -fRd
        try {
            SEED_FILE.extract(EVENT_DIR.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error on extracting " + SEED_FILE, e);
        }

        try {
            if (CMPMOD)
                // fix delta values
                preprocess();
            // merge uneven SAC files
            mergeUnevenSac();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error on pre-processing " + SEED_FILE, e);
        }

        try {
            modifySACs();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error on modifying " + SEED_FILE, e);
        }


        // Use only BH[12ENZ]
        // remove waveforms of .[~NEZ]
        try {
            selectChannels();
        } catch (IOException e) {
            throw new RuntimeException("Error on selecting channels " + SEED_FILE, e);
        }

        // instrumentation function deconvolution
        deconvolute();

        // rotation (.N, .E -> .R, .T)
        try {
            rotate();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error on rotating " + SEED_FILE, e);
        }

        // trash
        try {
            toTrash();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error on moving files to the trash box " + SEED_FILE, e);
        }

        problem = check();

        hadRun = true;

        if (removeIntermediateFiles) removeIntermediateFiles();

        System.err.println("finish");
    }

    private void removeIntermediateFiles() {
        try {
            Path event = EVENT_DIR.toPath();
            FileUtils.deleteDirectory(event.resolve("merged").toFile());
            FileUtils.deleteDirectory(event.resolve("mod").toFile());
            FileUtils.deleteDirectory(event.resolve("rdseedOutputBackup").toFile());
            FileUtils.deleteDirectory(event.resolve("resp").toFile());
            FileUtils.deleteDirectory(event.resolve("rotatedNE").toFile());
            FileUtils.deleteDirectory(event.resolve("nonRotatedNE").toFile());
            FileUtils.deleteDirectory(event.resolve("spectra").toFile());
            FileUtils.deleteDirectory(event.resolve("trash").toFile());
            FileUtils.deleteDirectory(event.resolve("mergedUnevendata").toFile());
            FileUtils.deleteDirectory(event.resolve("invalidChannel").toFile());
            FileUtils.deleteDirectory(event.resolve("nonMergedUnevendata").toFile());
            FileUtils.deleteDirectory(event.resolve("noSpectraOrInvalidMOD").toFile());
            FileUtils.deleteDirectory(event.resolve("duplicateChannel").toFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return if there are no problems, returns true
     */
    boolean hasProblem() {
        return problem;
    }

    /**
     * @return if any problem
     */
    private boolean check() {
        Path eventPath = EVENT_DIR.toPath();
        Path rdseedOutput = eventPath.resolve("rdseedOutputBackup");
        Path unmerged = eventPath.resolve("nonMergedUnevendata");
        Path unrotated = eventPath.resolve("nonRotatedNE");
        return Files.exists(rdseedOutput) || Files.exists(unmerged) || Files.exists(unrotated);
    }

    /**
     * unused SPECTRA*, RESP* files ->trash
     */
    private void toTrash() throws IOException {
        Path trash = EVENT_DIR.toPath().resolve("trash");
        try (DirectoryStream<Path> files = Files.newDirectoryStream(EVENT_DIR.toPath())) {
            for (Path path : files) {
                String name = path.getFileName().toString();
                if (name.contains("SPECTRA.") || name.contains("RESP.")) Utilities.moveToDirectory(path, trash, true);
            }
        }
    }

    /**
     * evalresp station component year julian day minfreq maxfreq
     * npts -s lin -r cs -u vel
     *
     * @param headerMap header of sac file
     * @return if succeed
     */
    private boolean runEvalresp(Map<SACHeaderEnum, String> headerMap) {
        int npts = Integer.parseInt(headerMap.get(SACHeaderEnum.NPTS));
        double minFreq = samplingHz / npts;
        String command =
                "evalresp " + headerMap.get(SACHeaderEnum.KSTNM) + " " + headerMap.get(SACHeaderEnum.KCMPNM) + " " +
                        event.getCMTTime().getYear() + " " + event.getCMTTime().getDayOfYear() + " " + minFreq + " " +
                        samplingHz + " " + headerMap.get(SACHeaderEnum.NPTS) + " -s lin -r cs -u vel";

        ProcessBuilder pb = new ProcessBuilder(command.split("\\s"));
        pb.directory(EVENT_DIR.getAbsoluteFile());
        try {
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Remove files with suffixes other than [BH][HL][ENZ12]
     * @author anselme BH1 BH2 also kept
     */
    private void selectChannels() throws IOException {
        // System.out.println("Selecting Channels");
        Path trashBox = EVENT_DIR.toPath().resolve("invalidChannel");
        try (DirectoryStream<Path> modStream = Files.newDirectoryStream(EVENT_DIR.toPath(), "*.MOD")) {
            for (Path modPath : modStream) {
                String name = modPath.getFileName().toString();
                String channel = name.split("\\.")[3];
                if (channel.equals("BHZ") || channel.equals("BLZ") || channel.equals("BHN") || channel.equals("BHE") ||
                        channel.equals("BLN") || channel.equals("BLE") || channel.equals("HHZ") ||
                        channel.equals("HLZ") || channel.equals("HHN") || channel.equals("HHE") ||
                        channel.equals("HLN") || channel.equals("HLE") ||
                        channel.equals("BH1") || channel.equals("BH2")) continue;
                Utilities.moveToDirectory(modPath, trashBox, true);
            }
        }
    }

    @Override
    public String toString() {
        return SEED_FILE.toString();
    }
}
