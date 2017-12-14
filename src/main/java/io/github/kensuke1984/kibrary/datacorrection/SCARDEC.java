package io.github.kensuke1984.kibrary.datacorrection;

import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Trace;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.TransformType;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class is <b>IMMUTABLE</b>.
 * <p>
 * Information of a seismic event in
 * <a href="http://scardec.projects.sismo.ipgp.fr/">SCARDEC</a>.
 *
 * The database is as of 20161115.
 * @author Kensuke Konishi
 * @version 0.1.1
 * @see <a href="http://scardec.projects.sismo.ipgp.fr/">SCARDEC</a>,
 * <a href="http://earthquake.usgs.gov/contactus/golden/neic.php">NEIC</a>
 */
public class SCARDEC {

    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy MM dd HH mm ss");
    private static final URL SCARDEC_ROOT_PATH = SCARDEC.class.getClassLoader().getResource("scardec20161115.zip");
    private static final Set<SCARDEC_ID> EXISTING_ID = Collections.synchronizedSet(new HashSet<>());

    static {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(SCARDEC_ROOT_PATH.openStream()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    Path path = Paths.get(entry.getName());
                    try {
                        SCARDEC_ID id = SCARDEC_ID.of(path.getFileName().toString());
                        EXISTING_ID.add(id);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Exception occurred in reading a SCARDEC catalog.");
        }
    }

    private final SCARDEC_ID ID;
    /**
     * Hypocenter is from
     * <a href="http://earthquake.usgs.gov/contactus/golden/neic.php">NEIC</a>.
     * Depth is from
     * <a href="http://scardec.projects.sismo.ipgp.fr/">SCARDEC</a>
     */
    private final Location EPICENTRAL_LOCATION;
    private final double MW;
    /**
     * [N&middot;m]
     */
    private final double M0;
    // deg
    private final double STRIKE1;
    private final double DIP1;
    private final double RAKE1;
    private final double STRIKE2;
    private final double DIP2;
    private final double RAKE2;
    /**
     * Trace of average moment rate(x: time[s], y: moment rate[N&middot;m/s])
     * average source time function
     */
    private final Trace AVERAGE_MOMENT_RATE_FUNCTION;
    /**
     * Trace of optimal moment rate(x: time[s], y: moment rate[N&middot;m/s])
     * optimal source time function
     */
    private final Trace OPTIMAL_MOMENT_RATE_FUNCTION;

    private SCARDEC(SCARDEC_ID id, Location epicentralLocation, double m0, double mw, double strike1, double dip1,
                    double rake1, double strike2, double dip2, double rake2, Trace averageSTF, Trace optimalSTF) {
        EPICENTRAL_LOCATION = epicentralLocation;
        DIP1 = dip1;
        DIP2 = dip2;
        STRIKE1 = strike1;
        STRIKE2 = strike2;
        RAKE1 = rake1;
        RAKE2 = rake2;
        M0 = m0;
        MW = mw;
        ID = id;
        AVERAGE_MOMENT_RATE_FUNCTION = averageSTF;
        OPTIMAL_MOMENT_RATE_FUNCTION = optimalSTF;
    }

    /**
     * Prints out all the events in the catalog.
     */
    public static void printList() {
        EXISTING_ID.stream().sorted().forEach(System.out::println);
    }

    public static SCARDEC getSCARDEC(SCARDEC_ID id) {
        if (!EXISTING_ID.contains(id))
            throw new RuntimeException("No information for " + id.ORIGIN_TIME + " " + id.REGION);
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(SCARDEC_ROOT_PATH.openStream()));
             DataInputStream dis = new DataInputStream(zis)) {
            ZipEntry entry;
            Location loc = null;
            double m0 = Double.NaN;
            double mw = Double.NaN;
            double strike1 = Double.NaN;
            double dip1 = Double.NaN;
            double rake1 = Double.NaN;
            double strike2 = Double.NaN;
            double dip2 = Double.NaN;
            double rake2 = Double.NaN;
            Trace averageSTF = null;
            Trace optimalSTF = null;
            while ((entry = zis.getNextEntry()) != null) {
                String path = Paths.get(entry.getName()).toString();
                if (entry.isDirectory()) continue;
                if (!path.contains(id.REGION) || !path.contains(id.getDateTimeString()) || path.contains("cmt.png"))
                    continue;
                for (int i = 0; i < 6; i++)
                    dis.readInt(); // origin time
                loc = new Location(dis.readDouble(), dis.readDouble(), dis.readDouble());
                m0 = dis.readDouble();
                mw = dis.readDouble();
                strike1 = dis.readDouble();
                dip1 = dis.readDouble();
                rake1 = dis.readDouble();
                strike2 = dis.readDouble();
                dip2 = dis.readDouble();
                rake2 = dis.readDouble();
                int n = dis.readInt();
                double[] time = new double[n];
                double[] stf = new double[n];
                for (int i = 0; i < n; i++) {
                    time[i] = dis.readDouble();
                    stf[i] = dis.readDouble();
                }
                if (path.contains("moysource")) averageSTF = new Trace(time, stf);
                else optimalSTF = new Trace(time, stf);
            }
            if (averageSTF == null || optimalSTF == null)
                throw new RuntimeException("Average of optimal stf is not found.");
            return new SCARDEC(id, loc, m0, mw, strike1, dip1, rake1, strike2, dip2, rake2, averageSTF, optimalSTF);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception in reading the SCARDEC archive.");
        }
    }

    /**
     * @param predicate Filter for IDs
     * @return Set of IDs in the cache
     */
    public static Set<SCARDEC_ID> search(Predicate<SCARDEC_ID> predicate) {
        return EXISTING_ID.stream().filter(predicate).collect(Collectors.toSet());
    }

    /**
     * Choose one ID from candidates satisfying the predicate.
     *
     * @param predicate filter for IDs
     * @return SCARDEC_ID chosen from IDs satisfying the predicate.
     */
    public static SCARDEC_ID pick(Predicate<SCARDEC_ID> predicate) {
        SCARDEC_ID[] ids = EXISTING_ID.stream().filter(predicate).sorted().toArray(SCARDEC_ID[]::new);
        if (ids.length == 0) throw new RuntimeException("No ID matches.");
        if (ids.length == 1) return ids[0];
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new CloseShieldInputStream(System.in)))) {
            System.out.println("Which ID do you want to use?");
            System.out.println("There are several candidates. Choose one.");
            for (int i = 0; i < ids.length; i++)
                System.out.println((i + 1) + " " + ids[i]);
            int k = -1;
            while (k < 0) {
                String numStr = br.readLine();
                if (NumberUtils.isCreatable(numStr)) k = Integer.parseInt(numStr);
                if (k < 1 || ids.length <= k - 1) {
                    System.out.println("... which one? [" + 0 + " - " + (ids.length - 1) + "] ");
                    k = -1;
                }
            }
            System.err.println(ids[k - 1] + " is chosen.");
            return ids[k - 1];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param folder a folder must have original name forms (e.g. FCTs_yyyymmdd_...)
     * @return SCARDEC in the folder
     */
    private static SCARDEC readAscii(Path folder) throws IOException {
        String name = folder.getFileName().toString();
        Path averagePath = folder.resolve(name.replace("FCTs", "fctmoysource"));
        Path optimalPath = folder.resolve(name.replace("FCTs", "fctoptsource"));
        return readAscii(averagePath, optimalPath);
    }

    /**
     * @param args -l for all the list. If ID(yyyyMMdd_HHmmss)s put, it shows
     *             their information.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printList();
            return;
        }

        for (String id : args) {
            Optional<SCARDEC_ID> sid = EXISTING_ID.stream().filter(i -> i.getDateTimeString().equals(id)).findAny();
            if (!sid.isPresent()) {
                System.err.println("No information for " + id + " (check if it is 'yyyyMMdd_HHmmss')");
                continue;
            }
            SCARDEC_ID scid = sid.get();
            scid.toSCARDEC().printInfo();
        }

    }

    /**
     * @param averagePath a PATH for a SCARDEC file of the average STF (moy)
     * @param optimalPath a PATH for a SCARDEC file of the optimal STF (opt)
     * @return SCARDEC written in the input paths
     * @throws IOException if any
     */
    public static SCARDEC readAscii(Path averagePath, Path optimalPath) throws IOException {
        List<String> averageLines = Files.readAllLines(averagePath);
        List<String> optimalLines = Files.readAllLines(optimalPath);
        SCARDEC_ID averageID = SCARDEC_ID.of(averagePath.getFileName().toString());
        SCARDEC_ID optimalID = SCARDEC_ID.of(optimalPath.getFileName().toString());
        if (!averageID.equals(optimalID) || !averageLines.get(0).equals(optimalLines.get(0)) ||
                !averageLines.get(1).equals(optimalLines.get(1)))
            throw new RuntimeException(averagePath + " and " + optimalPath + " is not a pair.");

        String first = averageLines.get(0);
        String date = first.substring(0, first.indexOf(".0"));
        LocalDateTime origin = LocalDateTime.parse(date, FORMAT);
        if (!optimalID.ORIGIN_TIME.equals(origin))
            throw new RuntimeException("ORIGIN time in the file name and the file are different!!");
        double[] latlon =
                Arrays.stream(first.substring(first.indexOf(".0") + 3).split("\\s+")).mapToDouble(Double::parseDouble)
                        .toArray();
        double[] values = Arrays.stream(averageLines.get(1).split("\\s+")).mapToDouble(Double::parseDouble).toArray();
        Location src = new Location(latlon[0], latlon[1], 6371 - values[0]);

        Function<List<String>, Trace> linesToSTF = lines -> {
            String[][] parts = lines.stream().skip(2).map(s -> s.trim().split("\\s+")).toArray(String[][]::new);
            double[] time = Arrays.stream(parts).mapToDouble(p -> Double.parseDouble(p[0])).toArray();
            double[] momentrates = Arrays.stream(parts).mapToDouble(p -> Double.parseDouble(p[1])).toArray();
            return new Trace(time, momentrates);
        };

        Trace averageSTF = linesToSTF.apply(averageLines);
        Trace optimalSTF = linesToSTF.apply(optimalLines);

        return new SCARDEC(optimalID, src, values[1], values[2], values[3], values[4], values[5], values[6], values[7],
                values[8], averageSTF, optimalSTF);
    }

    public static SCARDEC readBinary(Path averagePath, Path optimalPath, OpenOption... options) throws IOException {
        SCARDEC_ID averageID = SCARDEC_ID.of(averagePath.getFileName().toString());
        SCARDEC_ID optimalID = SCARDEC_ID.of(optimalPath.getFileName().toString());
        if (!averageID.equals(optimalID))
            throw new RuntimeException(averagePath + " and " + optimalPath + " is not a pair.");

        try (DataInputStream averageDIS = new DataInputStream(Files.newInputStream(averagePath, options));
             DataInputStream optimalDIS = new DataInputStream(Files.newInputStream(optimalPath, options))) {
            LocalDateTime aveLDT = LocalDateTime
                    .of(averageDIS.readInt(), averageDIS.readInt(), averageDIS.readInt(), averageDIS.readInt(),
                            averageDIS.readInt(), averageDIS.readInt());
            LocalDateTime optLDT = LocalDateTime
                    .of(optimalDIS.readInt(), optimalDIS.readInt(), optimalDIS.readInt(), optimalDIS.readInt(),
                            optimalDIS.readInt(), optimalDIS.readInt());

            Location aveloc = new Location(averageDIS.readDouble(), averageDIS.readDouble(), averageDIS.readDouble());
            if (!aveLDT.equals(averageID.ORIGIN_TIME))
                throw new RuntimeException("ORIGIN time in the file name and in the file are different!!");
            double avem0 = averageDIS.readDouble();
            double avemw = averageDIS.readDouble();
            double avestrike1 = averageDIS.readDouble();
            double avedip1 = averageDIS.readDouble();
            double averake1 = averageDIS.readDouble();
            double avestrike2 = averageDIS.readDouble();
            double avedip2 = averageDIS.readDouble();
            double averake2 = averageDIS.readDouble();
            int aven = averageDIS.readInt();
            double[] avetime = new double[aven];
            double[] avestf = new double[aven];
            for (int i = 0; i < aven; i++) {
                avetime[i] = averageDIS.readDouble();
                avestf[i] = averageDIS.readDouble();
            }

            Location optloc = new Location(optimalDIS.readDouble(), optimalDIS.readDouble(), optimalDIS.readDouble());
            double optm0 = optimalDIS.readDouble();
            double optmw = optimalDIS.readDouble();
            double optstrike1 = optimalDIS.readDouble();
            double optdip1 = optimalDIS.readDouble();
            double optrake1 = optimalDIS.readDouble();
            double optstrike2 = optimalDIS.readDouble();
            double optdip2 = optimalDIS.readDouble();
            double optrake2 = optimalDIS.readDouble();
            if (!aveLDT.equals(optLDT) || !aveloc.equals(optloc) || optm0 != avem0 || optmw != avemw ||
                    optstrike1 != avestrike1 || optdip1 != avedip1 || optrake1 != averake1 ||
                    optstrike2 != avestrike2 || optdip2 != avedip2 || optrake2 != averake2)
                throw new RuntimeException(averagePath + " and " + optimalPath + " is not a pair.");

            int optn = optimalDIS.readInt();
            double[] opttime = new double[optn];
            double[] optstf = new double[optn];
            for (int i = 0; i < optn; i++) {
                opttime[i] = optimalDIS.readDouble();
                optstf[i] = optimalDIS.readDouble();
            }

            Trace averageSTF = new Trace(avetime, avestf);
            Trace optimalSTF = new Trace(opttime, optstf);
            return new SCARDEC(optimalID, aveloc, avem0, avemw, avestrike1, avedip1, averake1, avestrike2, avedip2,
                    averake2, averageSTF, optimalSTF);
        }
    }

    /**
     * @return origin time
     */
    public LocalDateTime getOriginTime() {
        return ID.ORIGIN_TIME;
    }

    /**
     * @return String of the region
     */
    public String getRegion() {
        return ID.REGION;
    }

    /**
     * Interpolate to the samplingHz 20. and creates {@link SourceTimeFunction}
     *
     * @param np   steps of frequency [should be same as synthetics].
     * @param tlen [s] length of waveform [should be same as synthetics]
     * @return {@link SourceTimeFunction} for this.
     */
    public SourceTimeFunction getOptimalSTF(int np, double tlen) {
        return getSTF(false, np, tlen);
    }

    private SourceTimeFunction getSTF(boolean average, int np, double tlen) {
        double samplingHz = 20;
        Trace momentRateFunction = average ? AVERAGE_MOMENT_RATE_FUNCTION : OPTIMAL_MOMENT_RATE_FUNCTION;
        double start = momentRateFunction.getXAt(0);
        double end = momentRateFunction.getXAt(momentRateFunction.getLength() - 1);
        if (!SourceTimeFunction.checkValues(np, tlen, samplingHz)) throw new IllegalArgumentException();
        int nptsInTime = (int) (tlen * samplingHz);
        double[] stfForFFT = new double[nptsInTime];
        double deltaT = 1 / samplingHz;

        double stfSize = 0;
        for (int i = 0; i < stfForFFT.length; i++) {
            double t = i * deltaT;
            if (t < end) {
                stfForFFT[i] = momentRateFunction.toValue(2, t);
                stfSize += stfForFFT[i];
            } else if (start < t - tlen) {
                stfForFFT[i] = momentRateFunction.toValue(2, t - tlen);
                stfSize += stfForFFT[i];
            }
        }

        for (int i = 0; i < stfForFFT.length; i++)
            stfForFFT[i] /= stfSize;

        Complex[] stfFreq = SourceTimeFunction.fft.transform(stfForFFT, TransformType.FORWARD);

        // consider NP
        Complex[] cutSTF = new Complex[np];
        System.arraycopy(stfFreq, 0, cutSTF, 0, 1024);

        SourceTimeFunction stf = new SourceTimeFunction(np, tlen, samplingHz) {
            @Override
            public Complex[] getSourceTimeFunctionInFrequencyDomain() {
                return sourceTimeFunction;
            }
        };
        stf.sourceTimeFunction = cutSTF;
        return stf;
    }

    /**
     * Interpolate to the samplingHz 20. and creates {@link SourceTimeFunction}
     *
     * @param np   steps of frequency [should be same as synthetics].
     * @param tlen [s] length of waveform [should be same as synthetics]
     * @return {@link SourceTimeFunction} for this.
     */
    public SourceTimeFunction getAverageSTF(int np, double tlen) {
        return getSTF(true, np, tlen);
    }

    /**
     * Outputs the SCARDEC information in the binary format.
     *
     * @param folder  Path of the folder where SCARDEC files are written.
     * @param options if any
     * @throws IOException if any
     */
    public void write(Path folder, OpenOption... options) throws IOException {
        String dateTimeString = ID.getDateTimeString();
        Path averagePath = folder.resolve("fctmoysource_" + dateTimeString + "_" + ID.REGION);
        Path optimalPath = folder.resolve("fctoptsource_" + dateTimeString + "_" + ID.REGION);
        try (DataOutputStream aveDOS = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(averagePath, options)));
             DataOutputStream optDOS = new DataOutputStream(
                     new BufferedOutputStream(Files.newOutputStream(optimalPath, options)))) {
            aveDOS.writeInt(ID.ORIGIN_TIME.getYear());
            aveDOS.writeInt(ID.ORIGIN_TIME.getMonthValue());
            aveDOS.writeInt(ID.ORIGIN_TIME.getDayOfMonth());
            aveDOS.writeInt(ID.ORIGIN_TIME.getHour());
            aveDOS.writeInt(ID.ORIGIN_TIME.getMinute());
            aveDOS.writeInt(ID.ORIGIN_TIME.getSecond());
            aveDOS.writeDouble(EPICENTRAL_LOCATION.getLatitude());
            aveDOS.writeDouble(EPICENTRAL_LOCATION.getLongitude());
            aveDOS.writeDouble(EPICENTRAL_LOCATION.getR());
            aveDOS.writeDouble(M0);
            aveDOS.writeDouble(MW);
            aveDOS.writeDouble(STRIKE1);
            aveDOS.writeDouble(DIP1);
            aveDOS.writeDouble(RAKE1);
            aveDOS.writeDouble(STRIKE2);
            aveDOS.writeDouble(DIP2);
            aveDOS.writeDouble(RAKE2);
            double[] averageTime = AVERAGE_MOMENT_RATE_FUNCTION.getX();
            double[] averageSTF = AVERAGE_MOMENT_RATE_FUNCTION.getY();
            aveDOS.writeInt(averageTime.length);
            for (int i = 0; i < averageTime.length; i++) {
                aveDOS.writeDouble(averageTime[i]);
                aveDOS.writeDouble(averageSTF[i]);
            }
            optDOS.writeInt(ID.ORIGIN_TIME.getYear());
            optDOS.writeInt(ID.ORIGIN_TIME.getMonthValue());
            optDOS.writeInt(ID.ORIGIN_TIME.getDayOfMonth());
            optDOS.writeInt(ID.ORIGIN_TIME.getHour());
            optDOS.writeInt(ID.ORIGIN_TIME.getMinute());
            optDOS.writeInt(ID.ORIGIN_TIME.getSecond());
            optDOS.writeDouble(EPICENTRAL_LOCATION.getLatitude());
            optDOS.writeDouble(EPICENTRAL_LOCATION.getLongitude());
            optDOS.writeDouble(EPICENTRAL_LOCATION.getR());
            optDOS.writeDouble(M0);
            optDOS.writeDouble(MW);
            optDOS.writeDouble(STRIKE1);
            optDOS.writeDouble(DIP1);
            optDOS.writeDouble(RAKE1);
            optDOS.writeDouble(STRIKE2);
            optDOS.writeDouble(DIP2);
            optDOS.writeDouble(RAKE2);
            double[] optTime = OPTIMAL_MOMENT_RATE_FUNCTION.getX();
            double[] optSTF = OPTIMAL_MOMENT_RATE_FUNCTION.getY();
            optDOS.writeInt(optTime.length);
            for (int i = 0; i < optTime.length; i++) {
                optDOS.writeDouble(optTime[i]);
                optDOS.writeDouble(optSTF[i]);
            }
        }
    }

    /**
     * Prints out the origin time, epicentral location, M0, Mw, strikes, dips
     * and rakes.
     */
    public void printInfo() {
        System.out.println(ID);
        System.out.println("Epicentral location(lat lon radius):" + EPICENTRAL_LOCATION);
        System.out.println("M0:" + M0 + ", Mw:" + MW);
        System.out.println("strike1:" + STRIKE1 + ", dip1:" + DIP1 + ", rake1:" + RAKE1 + " [deg]");
        System.out.println("strike2:" + STRIKE2 + ", dip2:" + DIP2 + ", rake2:" + RAKE2 + " [deg]");
    }

    /**
     * Prints out moment rate function in the format "time[s] moment
     * rate[N&middot;m/s]"
     */
    public void printMomentRate() {
        System.out.println("time[s] moment rate[N\u00b7m/s]");
        double[] time = OPTIMAL_MOMENT_RATE_FUNCTION.getX();
        double[] stf = OPTIMAL_MOMENT_RATE_FUNCTION.getY();
        for (int i = 0; i < time.length; i++)
            System.out.println(time[i] + " " + stf[i]);
    }

    /**
     * ID for information
     */
    public static class SCARDEC_ID implements Comparable<SCARDEC_ID> {

        /**
         * Origin time from <a href=
         * "http://earthquake.usgs.gov/contactus/golden/neic.php">NEIC</a>
         */
        private final LocalDateTime ORIGIN_TIME;
        private final String REGION;

        SCARDEC_ID(LocalDateTime origin, String region) {
            ORIGIN_TIME = origin;
            REGION = region;
        }

        /**
         * @param string must be in the form FCTs_yyyyMMdd_HHmmss_(region)
         * @return SCARDEC_ID for the string
         */
        private static SCARDEC_ID of(String string) {
            int index = string.indexOf("_");
            String dateTime = string.substring(index + 1, index + 1 + 15);
            String region = string.substring(index + 1 + 16);
            return new SCARDEC_ID(LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
                    region);
        }

        @Override
        public String toString() {
            return "Origin time:" + ORIGIN_TIME + ", Region:" + REGION;
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = prime * result + ((ORIGIN_TIME == null) ? 0 : ORIGIN_TIME.hashCode());
            result = prime * result + ((REGION == null) ? 0 : REGION.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            SCARDEC_ID other = (SCARDEC_ID) obj;
            if (ORIGIN_TIME == null) {
                if (other.ORIGIN_TIME != null) return false;
            } else if (!ORIGIN_TIME.equals(other.ORIGIN_TIME)) return false;
            if (REGION == null) {
                if (other.REGION != null) return false;
            } else if (!REGION.equals(other.REGION)) return false;
            return true;
        }

        private String getDateTimeString() {
            return ORIGIN_TIME.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        }

        /**
         * @return origin time
         */
        public LocalDateTime getOriginTime() {
            return ORIGIN_TIME;
        }

        /**
         * @return string for the region
         */
        public String getRegion() {
            return REGION;
        }

        @Override
        public int compareTo(SCARDEC_ID o) {
            int c = ORIGIN_TIME.compareTo(o.ORIGIN_TIME);
            return c != 0 ? c : REGION.compareTo(o.REGION);
        }

        /**
         * @return SCARDEC with source time functions.
         */
        public SCARDEC toSCARDEC() {
            return getSCARDEC(this);
        }
    }

}
