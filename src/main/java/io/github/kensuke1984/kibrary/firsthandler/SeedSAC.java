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
 * @version 0.1.8.6
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
    private boolean cmpMod = true;
    /**
     * GlobalCMTData for the event in the seedfile
     */
    private GlobalCMTData event;
    /**
     * where output goes.
     */
    private EventFolder eventDir;
    /**
     * seed file to process
     */
    private SEEDFile seedFile;
    /**
     * event ID
     */
    private GlobalCMTID id;
    /**
     * [deg] Minimum epicentral distance of SAC files to be output
     */
    private double epicentralDistanceMin = 0;
    /**
     * [deg] Maximum epicentral distance of SAC files to be output
     */
    private double epicentralDistanceMax = 180;

    private boolean eventDirAlreadyExists;
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
        seedFile = new SEEDFile(seedPath);
        if (id != null) this.id = id;
        else setID();
        if (!idValidity()) throw new RuntimeException("The ID " + this.id + " is invalid for " + seedPath);

        if (!Files.exists(outputDirectoryPath)) Files.createDirectories(outputDirectoryPath);
        eventDir = new EventFolder(outputDirectoryPath.resolve(this.id.toString()));

        if (eventDir.exists()) eventDirAlreadyExists = false;
        else if (!eventDir.mkdirs()) throw new RuntimeException("Can not create " + eventDir);
        else eventDirAlreadyExists = true;
        seedFile.createLink(eventDir.toPath());
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
        return seedFile.getSeedPath();
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
        if (GlobalCMTID.isGlobalCMTID(seedFile.getVolumeLabel())) {
            id = new GlobalCMTID(seedFile.getVolumeLabel());
            return;
        }
        System.err.println("Dataset in this seed file starts " + seedFile.getStartingDate());
        GlobalCMTSearch sc = new GlobalCMTSearch(seedFile.getStartingDate(), seedFile.getEndingDate());
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
        String fileName = seedFile.getSeedPath().getFileName().toString();
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
        return event != null && id != null && seedFile.getStartingDate().isBefore(event.getPDETime()) &&
                seedFile.getEndingDate().isAfter(event.getCMTTime());
    }

    /**
     * Deconvolute instrument function for all the MOD files in the event folder.
     * 対応するRESPのevalrespに失敗したMODファイルはNOSPECTRAMODへ
     */
    private void deconvolute() {
        // System.out.println("Conducting deconvolution");
        Path noSpectraPath = eventDir.toPath().resolve("noSpectraOrInvalidMOD");
        Path duplicateChannelPath = eventDir.toPath().resolve("duplicateChannel");
        // evalresp後のRESP.*ファイルを移動する TODO メソッドを分ける
        Path respBoxPath = eventDir.toPath().resolve("resp");
        Path spectraBoxPath = eventDir.toPath().resolve("spectra");
        Path modBoxPath = eventDir.toPath().resolve("mod");
        try (DirectoryStream<Path> eventDirStream = Files.newDirectoryStream(eventDir.toPath(), "*.MOD")) {
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
                Path spectraPath = eventDir.toPath().resolve(spectraFileName);
                Path respPath = eventDir.toPath().resolve(respFileName);
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
                Path afterPath = eventDir.toPath().resolve(afterName);

                // evalresp を走らせる。
                // 失敗したらMOD, RESPファイルをtrashに
                if (!runEvalresp(headerMap)) {
                    // SPECTRAをつくれなかったMOD.*ファイルをnoSpectraに移す
                    Utilities.moveToDirectory(modPath, noSpectraPath, true);
                    // SPECTRAをつくれなかったRESP.*ファイルをnoSpectraに移す
                    Utilities.moveToDirectory(respPath, noSpectraPath, true);
                    continue;
                }

                // seedsacを行う
                try {
                    int npts = Integer.parseInt(headerMap.get(SACHeaderEnum.NPTS));
                    // duplication of channel
                    if (Files.exists(afterPath)) {
                        // SPECTRAをつくれなかった*.MODファイルをduplicateChannelPathに移す
                        Utilities.moveToDirectory(modPath, duplicateChannelPath, true);
                        // SPECTRAをつくれなかったSPECTRA.*ファイルをduplicateChannelPathに移す
                        Utilities.moveToDirectory(spectraPath, duplicateChannelPath, true);
                        // SPECTRAをつくれなかったRESP.*ファイルをduplicateChannelPathに移す
                        Utilities.moveToDirectory(respPath, duplicateChannelPath, true);
                        continue;
                    }
                    SACDeconvolution.compute(modPath, spectraPath, afterPath, samplingHz / npts, samplingHz);
                } catch (Exception e) {
                    // SPECTRAをつくれなかった*.MODファイルをnoSpectraに移す
                    Utilities.moveToDirectory(modPath, noSpectraPath, true);
                    // SPECTRAをつくれなかったSPECTRA.*ファイルをnoSpectraに移す
                    Utilities.moveToDirectory(spectraPath, noSpectraPath, true);
                    // SPECTRAをつくれなかったRESP.*ファイルをnoSpectraに移す
                    Utilities.moveToDirectory(respPath, noSpectraPath, true);
                    continue;
                }

                // 処理の終わったRESP.*ファイルをrespBoxに移す
                Utilities.moveToDirectory(respPath, respBoxPath, true);

                // 処理の終わったSPCTRA.*ファイルをspectraBoxに移す
                Utilities.moveToDirectory(spectraPath, spectraBoxPath, true);

                // 処理の終わったMOD.*ファイルをmodBoxに移す
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
        UnevenSACMerger u = new UnevenSACMerger(eventDir.toPath());
        u.merge();
        u.move();
    }

    /**
     * modify merged SAC files
     */
    private void modifySACs() throws IOException, InterruptedException {
        // System.out.println("Modifying sac files in "
        // + eventDir.getAbsolutePath());
        Path trashBoxPath = eventDir.toPath().resolve("trash");
        Path mergedBoxPath = eventDir.toPath().resolve("merged");

        try (DirectoryStream<Path> sacPathStream = Files.newDirectoryStream(eventDir.toPath(), "*.SAC")) {
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
                if (!sm.checkEpicentralDistance(epicentralDistanceMin, epicentralDistanceMax)) {
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
        Path trashBox = eventDir.toPath().resolve("nonRotatedNE");
        Path neDir = eventDir.toPath().resolve("rotatedNE");
        try (DirectoryStream<Path> eStream = Files.newDirectoryStream(eventDir.toPath(), "*.E")) {
            for (Path ePath : eStream) {
                String[] parts = ePath.getFileName().toString().split("\\.");
                Path nPath = eventDir.toPath().resolve(parts[0] + "." + parts[1] + ".N");
                Path rPath = eventDir.toPath().resolve(parts[0] + "." + parts[1] + ".R");
                Path tPath = eventDir.toPath().resolve(parts[0] + "." + parts[1] + ".T");

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
        try (DirectoryStream<Path> nPaths = Files.newDirectoryStream(eventDir.toPath(), "*.N")) {
            for (Path nPath : nPaths)
                Utilities.moveToDirectory(nPath, trashBox, true);
        }

    }

    /**
     * Adjusts delta, cmpinc and cmpaz in SAC files extracted by 'rdseed'
     * The SAC files are copied in 'rdseedOutputBackup' for backup.
     */
    private void preprocess() throws IOException {
        Path backupPath = eventDir.toPath().resolve("rdseedOutputBackup");
        Files.createDirectories(backupPath);
        try (DirectoryStream<Path> sacPaths = Files.newDirectoryStream(eventDir.toPath(), "*.SAC")) {
            for (Path sacPath : sacPaths) {
                Files.copy(sacPath, backupPath.resolve(sacPath.getFileName()));
                fixDelta(sacPath);
            }
        }

    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("It works only for one seed file.");
        Path seedPath = Paths.get(args[0]);
        if (!Files.exists(seedPath)) throw new NoSuchFileException(seedPath + " does not exist.");
        Path out = seedPath.resolveSibling("seedSAC" + Utilities.getTemporaryString());
        new SeedSAC(seedPath, out).run();
        System.out.println(seedPath + " is extracted in " + out);
    }

    @Override
    public void run() {
        if (!eventDirAlreadyExists) throw new RuntimeException("The condition is no good.");
        System.err.println("Opening " + seedFile + " in " + eventDir.getPath());
        // run rdseed -q [output directory] -fRd
        try {
            seedFile.extract(eventDir.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error on extracting " + seedFile, e);
        }

        try {
            if (cmpMod)
                // fix delta values
                preprocess();
            // merge uneven SAC files
            mergeUnevenSac();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error on pre-processing " + seedFile, e);
        }

        try {
            modifySACs();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error on modifying " + seedFile, e);
        }


        // Use only BH[12ENZ]
        // remove waveforms of .[~NEZ]
        try {
            selectChannels();
        } catch (IOException e) {
            throw new RuntimeException("Error on selecting channels " + seedFile, e);
        }

        // instrumentation function deconvolution
        deconvolute();

        // rotation (.N, .E -> .R, .T)
        try {
            rotate();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error on rotating " + seedFile, e);
        }

        // trash
        try {
            toTrash();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error on moving files to the trash box " + seedFile, e);
        }

        problem = check();

        hadRun = true;

        if (removeIntermediateFiles) removeIntermediateFiles();

        System.err.println("finish");
    }

    private void removeIntermediateFiles() {
        try {
            Path event = eventDir.toPath();
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
        Path eventPath = eventDir.toPath();
        Path rdseedOutput = eventPath.resolve("rdseedOutputBackup");
        Path unmerged = eventPath.resolve("nonMergedUnevendata");
        Path unrotated = eventPath.resolve("nonRotatedNE");
        return Files.exists(rdseedOutput) || Files.exists(unmerged) || Files.exists(unrotated);
    }

    /**
     * unused SPECTRA*, RESP* files ->trash
     */
    private void toTrash() throws IOException {
        Path trash = eventDir.toPath().resolve("trash");
        try (DirectoryStream<Path> files = Files.newDirectoryStream(eventDir.toPath())) {
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
        pb.directory(eventDir.getAbsoluteFile());
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
     */
    private void selectChannels() throws IOException {
        // System.out.println("Selecting Channels");
        Path trashBox = eventDir.toPath().resolve("invalidChannel");
        try (DirectoryStream<Path> modStream = Files.newDirectoryStream(eventDir.toPath(), "*.MOD")) {
            for (Path modPath : modStream) {
                String name = modPath.getFileName().toString();
                String channel = name.split("\\.")[3];
                if (channel.equals("BHZ") || channel.equals("BLZ") || channel.equals("BHN") || channel.equals("BHE") ||
                        channel.equals("BLN") || channel.equals("BLE") || channel.equals("HHZ") ||
                        channel.equals("HLZ") || channel.equals("HHN") || channel.equals("HHE") ||
                        channel.equals("HLN") || channel.equals("HLE")) continue;
                Utilities.moveToDirectory(modPath, trashBox, true);
            }
        }
    }

    @Override
    public String toString() {
        return seedFile.toString();
    }
}
