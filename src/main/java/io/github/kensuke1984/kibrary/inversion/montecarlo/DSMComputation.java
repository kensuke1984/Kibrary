package io.github.kensuke1984.kibrary.inversion.montecarlo;

import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.dsminformation.SyntheticDSMInfo;
import io.github.kensuke1984.kibrary.external.DSMMPI;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.spc.DSMOutput;
import io.github.kensuke1984.kibrary.util.spc.FormattedSPCFile;
import io.github.kensuke1984.kibrary.util.spc.SACMaker;
import io.github.kensuke1984.kibrary.util.spc.SPCFile;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes DSM(tish) write will be in outdir.
 *
 * @author Kensuke Konishi
 * @version 0.0.1.2
 */
class DSMComputation implements DataGenerator<PolynomialStructure, SACData[]> {


    private static final int NP = 256;
    private static final double SAMPLING_HZ = 20;
    private static final double TLEN = 1638.4;
    private final static Set<SACComponent> components = new HashSet<>(Collections.singletonList(SACComponent.T));
    private final SyntheticDSMInfo[] DSM_INFOS;
    private final Path outPath;
    private final Path PSVPATH;
    private ButterworthFilter filter;
    private Path hostFilePath;
    // MPI thread
    private ExecutorService pool = Executors.newSingleThreadExecutor();
    private int sequentialNumber;
    private Path obsDir;
    private Set<Station> stationSet;

    /**
     * @param obsDir     path of observed waveforms
     * @param outDir     path of write data
     * @param psvPath    path of PSV spectors
     * @param stationSet station information
     * @throws IOException if any
     */
    DSMComputation(Path obsDir, Path outDir, Path psvPath, Set<Station> stationSet) throws IOException {
        this.obsDir = obsDir;
        PSVPATH = psvPath;
        outPath = outDir;
        this.stationSet = stationSet;
        if (Files.exists(outDir)) throw new FileAlreadyExistsException(outDir.toString());
        Files.createDirectories(outDir);
        DSM_INFOS = init(obsDir, stationSet);
        setFilter(0.005, 0.08, 4);
    }

    private SyntheticDSMInfo[] init(Path obsDir, Set<Station> stationSet) throws IOException {
        return Utilities.eventFolderSet(obsDir).parallelStream().map(eventDir -> {
            try {
                Set<Station> stations =
                        eventDir.sacFileSet().stream().filter(SACFileName::isOBS).map(SACFileName::getStationName)
                                .distinct().map(this::pickup).collect(Collectors.toSet());
                GlobalCMTID id = eventDir.getGlobalCMTID();
                return new SyntheticDSMInfo(PolynomialStructure.PREM, id.getEvent(), stations, id.toString(), TLEN, NP);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).toArray(SyntheticDSMInfo[]::new);
    }

    /**
     * @param fMin 透過帯域 最小周波数
     * @param fMax 透過帯域 最大周波数
     * @param n    the value n (see Saito)
     */
    private void setFilter(double fMin, double fMax, int n) {
        double omegaH = fMax * 2 * Math.PI * 0.05;
        double omegaL = fMin * 2 * Math.PI * 0.05;
        filter = new BandPassFilter(omegaH, omegaL, n);
        filter.setBackward(true);
    }

    @Override
    public SACData[] generate(PolynomialStructure model) {
        try {
            Path root = Files.createDirectories(outPath.resolve("DSMComputation_" + sequentialNumber++));
            SyntheticDSMInfo[] infos = createDSMInfo(model);
            for (SyntheticDSMInfo info : infos) {
                Path infoPath = root.resolve(info.getGlobalCMTData() + ".inf");
                info.writeSH(infoPath);
                Path idPath = root.resolve(info.getGlobalCMTData().toString());
                Files.createDirectories(idPath);
                Callable<Integer> callable = DSMMPI.tish(8, infoPath);
                pool.submit(callable).get();
                EventFolder folder = new EventFolder(idPath);
                makeSacFiles(folder);
                applyFilter(folder);
            }
            Set<SACFileName> nameSet = new TreeSet<>(Utilities.sacFileNameSet(root));
            List<SACData> dataList = new ArrayList<>(nameSet.size());
            for (SACFileName sacFileName : nameSet) dataList.add(sacFileName.read());
            return dataList.toArray(new SACData[nameSet.size()]);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("unexpected");
        }
    }

    private Station pickup(String stationName) {
        return stationSet.stream().filter(station -> station.getName().equals(stationName)).findAny()
                .orElseThrow(() -> new RuntimeException("No information about " + stationName));
    }

    private SyntheticDSMInfo[] createDSMInfo(PolynomialStructure model) {
        return Arrays.stream(DSM_INFOS).map(info -> info.replaceStructure(model)).toArray(SyntheticDSMInfo[]::new);
    }

    private void applyFilter(EventFolder eventDir) throws IOException {
        eventDir.sacFileSet().forEach(name -> {
            if (!name.isSYN()) return;
            try {
                SACData sf = name.read().applyButterworthFilter(filter);
                sf.writeSAC(name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void makeSacFiles(EventFolder eventDir) throws IOException {
        Path spcPath = eventDir.toPath();
        SourceTimeFunction sourceTimeFunction = SourceTimeFunction.boxcarSourceTimeFunction(NP, TLEN, SAMPLING_HZ,
                eventDir.getGlobalCMTID().getEvent().getHalfDuration());
        try (Stream<Path> stream = Files.list(spcPath)) {
            stream.filter(path -> path.toString().endsWith("SH.spc")).forEach(shPath -> {
                SPCFile shName = new FormattedSPCFile(shPath);
                SPCFile psvName = toPSVname(shName);
                try {
                    DSMOutput shSPC = shName.read();
                    DSMOutput psvSPC = psvName.read();
                    SACMaker sm = new SACMaker(psvSPC, shSPC, sourceTimeFunction);
                    sm.setComponents(components);
                    sm.setOutPath(eventDir.toPath());
                    sm.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private SPCFile toPSVname(SPCFile shName) {
        String psvname = shName.getName().replace("SH.spc", "PSV.spc");
        GlobalCMTID id = new GlobalCMTID(shName.getSourceID());
        return new FormattedSPCFile(PSVPATH.resolve(id + "/" + psvname));
    }
}
