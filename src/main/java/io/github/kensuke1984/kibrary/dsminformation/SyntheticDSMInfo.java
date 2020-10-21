package io.github.kensuke1984.kibrary.dsminformation;

import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Information file for TIPSV and TISH
 *
 * @author Kensuke Konishi
 * @version 0.1.8.1
 * @author anselme change station string to NAME_NETWORK
 */
public class SyntheticDSMInfo extends DSMheader {

    protected final PolynomialStructure STRUCTURE;

    protected final String OUTPUT;

    /**
     * <b>unmodifiable</b>
     */
    protected final Set<Station> STATIONS;

    protected final GlobalCMTData EVENT;

    /**
     * @param structure of velocity
     * @param event     {@link GlobalCMTID}
     * @param stations  station information
     * @param outputDir name of outputDir (relative PATH)
     * @param tlen      TLEN[s]
     * @param np        NP
     */
    public SyntheticDSMInfo(PolynomialStructure structure, GlobalCMTData event, Set<Station> stations, String outputDir,
                            double tlen, int np) {
        super(tlen, np);
        STRUCTURE = structure;
        EVENT = event;
        STATIONS = Collections.unmodifiableSet(new HashSet<>(stations));
        OUTPUT = outputDir;
    }

    /**
     * Creates a file for tipsv
     *
     * @param psvPath Path of an write file
     * @param options for write
     * @throws IOException if an I/O error occurs
     * @author Kensuke Konishi
     * @author anselme change station string to NAME_NETWORK
     */
    public void writePSV(Path psvPath, OpenOption... options) throws IOException {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(psvPath, options))) {
            // header
            String[] header = outputDSMHeader();
            Arrays.stream(header).forEach(pw::println);

            // structure
            String[] structurePart = STRUCTURE.toPSVlines();
            Arrays.stream(structurePart).forEach(pw::println);

            Location eventLocation = EVENT.getCmtLocation();
            // source
            pw.println("c parameter for the source");
            pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude() +
                    " r0(km), lat, lon (deg)");
            pw.println(Arrays.stream(EVENT.getCmt().getDSMmt()).mapToObj(Double::toString)
                    .collect(Collectors.joining(" ")) + " Moment Tensor (1.e25 dyne cm)");

            // station
            pw.println("c parameter for the station");
            pw.println("c the number of stations");
            pw.println(STATIONS.size() + " nsta");
            pw.println("c latitude longitude (deg)");

            STATIONS.stream().sorted().map(Station::getPosition)
                    .forEach(p -> pw.println(p.getLatitude() + " " + p.getLongitude()));

            // write
            pw.println("c parameter for the write file");
            STATIONS.stream().sorted().map(s -> s.getName() + "_" + s.getNetwork())
                    .forEach(n -> pw.println(OUTPUT + "/" + n + "." + EVENT + "PSV.spc"));
            pw.println("end");

        }
    }

    /**
     * Creates a file for tish
     *
     * @param outPath write path
     * @param options for write
     * @throws IOException if an I/O error occurs
     * @author Kensuke Konishi
     * @author anselme change station string to NAME_NETWORK
     */
    public void writeSH(Path outPath, OpenOption... options) throws IOException {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
            // header
            String[] header = outputDSMHeader();
            Arrays.stream(header).forEach(pw::println);

            // structure
            String[] structurePart = STRUCTURE.toSHlines();
            Arrays.stream(structurePart).forEach(pw::println);
            Location eventLocation = EVENT.getCmtLocation();
            // source
            pw.println("c parameter for the source");
            pw.println(eventLocation.getR() + " " + eventLocation.getLatitude() + " " + eventLocation.getLongitude() +
                    " r0(km), lat, lon (deg)");
            pw.println(Arrays.stream(EVENT.getCmt().getDSMmt()).mapToObj(Double::toString)
                    .collect(Collectors.joining(" ")) + " Moment Tensor (1.e25 dyne cm)");

            // station
            pw.println("c parameter for the station");
            pw.println("c the number of stations");
            pw.println(STATIONS.size() + " nsta");
            pw.println("c latitude longitude (deg)");
            STATIONS.stream().sorted().map(Station::getPosition)
                    .forEach(p -> pw.println(p.getLatitude() + " " + p.getLongitude()));

            // write
            pw.println("c parameter for the write file");
            STATIONS.stream().sorted().map(s -> s.getName() + "_" + s.getNetwork())
                    .forEach(n -> pw.println(OUTPUT + "/" + n + "." + EVENT + "SH.spc"));
            pw.println("end");
        }
    }

    public SyntheticDSMInfo replaceStructure(PolynomialStructure structure) {
        return new SyntheticDSMInfo(structure, EVENT, STATIONS, OUTPUT, getTlen(), getNp());
    }

    public GlobalCMTData getGlobalCMTData() {
        return EVENT;
    }
    
}
