package io.github.kensuke1984.kibrary.dsminformation;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Make DSM information files for event folders under a work folder.
 *
 * @author Kensuke Konishi
 * @version 0.2.3
 */
public class SyntheticDSMInformationFileMaker implements Operation {

    private final Properties PROPERTY;
    /**
     * work folder
     */
    private Path workPath;
    /**
     * Number of steps in frequency domain.
     * It must be a power of 2.
     */
    private int np;
    /**
     * Time length [s].
     * It must be a power of 2 divided by 10.(2<sup>n</sup>/10)
     */
    private double tlen;
    /**
     * components to be used
     */
    private Set<SACComponent> components;
    /**
     * Information file name is header_[psv,sh].inf (default:PREM)
     */
    private String header;
    /**
     * structure file instead of PREM
     */
    private Path structurePath;

    private SyntheticDSMInformationFileMaker(Properties property) throws IOException {
        this.PROPERTY = (Properties) property.clone();
        set();
    }

    public static void writeDefaultPropertiesFile() throws IOException {
        Path outPath = Paths.get(
                SyntheticDSMInformationFileMaker.class.getName() + Utilities.getTemporaryString() + ".properties");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
            pw.println("manhattan SyntheticDSMInformationFileMaker");
            pw.println("##SacComponents to be used (Z R T)");
            pw.println("#components");
            pw.println("##Path of a work folder (.)");
            pw.println("#workPath");
            pw.println("##header for names of information files, header_[psv, sh].inf, (PREM)");
            pw.println("#header");
            pw.println("##Path of a structure file you want to use. ()");
            pw.println("#structurePath");
            pw.println("##TLEN must be a power of 2 over 10 (3276.8)");
            pw.println("#TLEN");
            pw.println("##NP must be a power of 2 (1024)");
            pw.println("#NP");
        }
        System.err.println(outPath + " is created.");
    }

    /**
     * @param args [parameter file name]
     * @throws IOException if any
     */
    public static void main(String[] args) throws Exception {
        Properties property = new Properties();
        if (args.length == 0) property.load(Files.newBufferedReader(Operation.findPath()));
        else if (args.length == 1) property.load(Files.newBufferedReader(Paths.get(args[0])));
        else throw new IllegalArgumentException("too many arguments. It should be 0 or 1(property file name)");
        long start = System.nanoTime();
        System.err.println(SyntheticDSMInformationFileMaker.class.getName() + " is going.");
        SyntheticDSMInformationFileMaker sdif = new SyntheticDSMInformationFileMaker(property);
        sdif.run();
        System.err.println(SyntheticDSMInformationFileMaker.class.getName() + " finished in " +
                Utilities.toTimeString(System.nanoTime() - start));
    }

    private void checkAndPutDefaults() {
        if (!PROPERTY.containsKey("workPath")) PROPERTY.setProperty("workPath", "");
        if (!PROPERTY.containsKey("components")) PROPERTY.setProperty("components", "Z R T");
        if (!PROPERTY.containsKey("TLEN")) PROPERTY.setProperty("TLEN", "3276.8");
        if (!PROPERTY.containsKey("NP")) PROPERTY.setProperty("NP", "1024");
        if (!PROPERTY.containsKey("header")) PROPERTY.setProperty("header", "PREM");
    }

    private void set() throws IOException {
        checkAndPutDefaults();
        workPath = Paths.get(PROPERTY.getProperty("workPath"));
        if (!Files.exists(workPath)) throw new NoSuchFileException(workPath + " (workPath)");
        components = Arrays.stream(PROPERTY.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
                .collect(Collectors.toSet());
        np = Integer.parseInt(PROPERTY.getProperty("NP"));
        tlen = Double.parseDouble(PROPERTY.getProperty("TLEN"));
        header = PROPERTY.getProperty("header");
        if (PROPERTY.containsKey("structurePath"))
            structurePath = workPath.resolve(PROPERTY.getProperty("structurePath"));
    }

    @Override
    public Properties getProperties() {
        return (Properties) PROPERTY.clone();
    }

    @Override
    public void run() throws Exception {
        Set<EventFolder> eventDirs = Utilities.eventFolderSet(workPath);
        PolynomialStructure ps =
                structurePath == null ? PolynomialStructure.PREM : new PolynomialStructure(structurePath);
        Path outPath = workPath.resolve("synthetic" + Utilities.getTemporaryString());
        Files.createDirectories(outPath);
        for (EventFolder eventDir : eventDirs) {
            try {
                Set<Station> stations = eventDir.sacFileSet().stream()
                        .filter(name -> name.isOBS() && components.contains(name.getComponent())).map(name -> {
                            try {
                                return name.readHeader();
                            } catch (Exception e2) {
                                return null;
                            }
                        }).filter(Objects::nonNull).map(Station::of).collect(Collectors.toSet());
                if (stations.isEmpty()) continue;
                int numberOfStation = (int) stations.stream().map(Station::getName).count();
                if (numberOfStation != stations.size()) System.err.println(
                        "!Caution there are stations with the same name and different positions in " + eventDir);
                Path eventOut = outPath.resolve(eventDir.toString());
                SyntheticDSMInfo info =
                        new SyntheticDSMInfo(ps, eventDir.getGlobalCMTID().getEvent(), stations, header, tlen, np);
                Files.createDirectories(eventOut.resolve(header));
                info.writePSV(eventOut.resolve(header + "_PSV.inf"));
                info.writeSH(eventOut.resolve(header + "_SH.inf"));
            } catch (Exception e) {
                System.err.println("Error on " + eventDir);
                e.printStackTrace();
            }
        }
    }

    @Override
    public Path getWorkPath() {
        return workPath;
    }
}
