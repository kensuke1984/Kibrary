package io.github.kensuke1984.kibrary.util.globalcmt;

import io.github.kensuke1984.kibrary.util.Location;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.util.Precision;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
/**
 * Query for search of Global CMT
 *
 * @author Kensuke Konishi
 * @version 0.1.12
 * TODO thread safe (immutable)
 */
public class GlobalCMTSearch {
	
    private static DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    /**
     * Added predicate set.
     */
    private Set<Predicate<GlobalCMTData>> predicateSet = new HashSet<>();
    /**
     * end date and time for CMT
     */
    private LocalDateTime endDate;
    /**
     * lower limit of centroid time shift [s] Default: -9999
     */
    private double lowerCentroidTimeShift = -9999;
    /**
     * lower limit of depth range [km] Default: 0
     */
    private double lowerDepth;
    /**
     * lower limit of latitude range [-90:90] (deg) Default: -90
     */
    private double lowerLatitude = -90;
    /**
     * lower limit of longitude range [-180:180] (deg) Default: -180
     */
    private double lowerLongitude = -180;
    /**
     * lower limit of body wave magnitude Default: 0
     */
    private double lowerMb;
    /**
     * lower limit of surface wave magnitude Default: 0
     */
    private double lowerMs;
    /**
     * lower limit of moment magnitude Default: 0
     */
    private double lowerMw;
    /**
     * lower limit of half duration
     */
    private double lowerHalfDuration;
    /**
     * lower limit of null axis plunge [0, 90] (deg) Default: 0
     */
    private int lowerNullAxisPlunge;
    /**
     * lower limit of tension axis plunge [0, 90] (deg) Default: 0
     */
    private int lowerTensionAxisPlunge;
    /**
     * start date and time for CMT
     */
    private LocalDateTime startDate;
    /**
     * upper limit of centroid time shift Default: 9999
     */
    private double upperCentroidTimeShift = 9999;
    /**
     * upper limit of depth range Default: 1000
     */
    private double upperDepth = 1000;
    /**
     * upper limit of latitude range [-90:90] Default: 90
     */
    private double upperLatitude = 90;
    /**
     * upper limit of longitude range [-180:180] Default: 180
     */
    private double upperLongitude = 180;
    /**
     * upper limit of body wave magnitude Default: 10
     */
    private double upperMb = 10;
    /**
     * upper limit of surface wave magnitude Default: 10
     */
    private double upperMs = 10;
    /**
     * upper limit of moment magnitude Default: 10
     */
    private double upperMw = 10;
    /**
     * upper limit of half duration Default: 20
     */
    private double upperHalfDuration = 20;
    /**
     * upper limit of null axis plunge [0, 90] (deg) Default: 90
     */
    private int upperNullAxisPlunge = 90;
    /**
     * upper limit of tension axis plunge [0, 90] (deg) Default: 90
     */
    private int upperTensionAxisPlunge = 90;

    /**
     * Search on 1 day.
     *
     * @param startDate on which this searches
     */
    public GlobalCMTSearch(LocalDate startDate) {
        this(startDate, startDate);
    }

    /**
     * Search from the startDate to endDate
     *
     * @param startDate starting date of the search (included)
     * @param endDate   end date of the search (included)
     */
    public GlobalCMTSearch(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate.atTime(0, 0);
        this.endDate = endDate.plusDays(1).atTime(0, 0);
    }

    /**
     * Search from the startDate to endDate
     *
     * @param startDate starting date of the search (included)
     * @param endDate   end date of the search (included)
     */
    public GlobalCMTSearch(LocalDateTime startDate, LocalDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * show date and time of event id
     *
     * @param id Global CMT id
     */
    private static void printIDinformation(GlobalCMTID id) {
        GlobalCMTData event = id.getEvent();
        Location location = event.getCmtLocation();
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        double depth = Precision.round((6371 - location.getR()), 3);
        System.out.println(
                id + " " + event.getCMTTime().format(outputFormat) + " " + lat + " " + lon + " " + depth + " MW:" +
                        event.getCmt().getMw());
    }

    /**
     * @param predicate {@link Predicate} for Event data of global CMT IDs
     * @return all global CMT IDs satisfying the input predicate
     */
    public static Set<GlobalCMTID> search(Predicate<GlobalCMTData> predicate) {
        return GlobalCMTCatalog.allNDK().stream().filter(predicate).map(NDK::getGlobalCMTID)
                .collect(Collectors.toSet());
    }

    public static void setOutputFormat(DateTimeFormatter outputFormat) {
        GlobalCMTSearch.outputFormat = outputFormat;
    }

    /**
     * Adds the predicate for another condition.
     *
     * @param predicate {@link Predicate} for {@link GlobalCMTData}
     */
    public GlobalCMTSearch addPredicate(Predicate<GlobalCMTData> predicate) {
        predicateSet.add(predicate);
        return this;
    }

    /**
     * @return copy of predicate set
     */
    public Set<Predicate<GlobalCMTData>> getPredicateSet() {
        return new HashSet<>(predicateSet);
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public double getLowerCentroidTimeShift() {
        return lowerCentroidTimeShift;
    }

    public double getLowerDepth() {
        return lowerDepth;
    }

    public double getLowerLatitude() {
        return lowerLatitude;
    }

    public double getLowerLongitude() {
        return lowerLongitude;
    }

    public double getLowerMb() {
        return lowerMb;
    }

    public double getLowerMs() {
        return lowerMs;
    }

    public double getLowerMw() {
        return lowerMw;
    }

    public int getLowerNullAxisPlunge() {
        return lowerNullAxisPlunge;
    }

    public int getLowerTensionAxisPlunge() {
        return lowerTensionAxisPlunge;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public double getUpperCentroidTimeShift() {
        return upperCentroidTimeShift;
    }

    public double getUpperDepth() {
        return upperDepth;
    }

    public double getUpperLatitude() {
        return upperLatitude;
    }

    public double getUpperLongitude() {
        return upperLongitude;
    }

    public double getUpperMb() {
        return upperMb;
    }

    public double getUpperMs() {
        return upperMs;
    }

    public double getUpperMw() {
        return upperMw;
    }

    public int getUpperNullAxisPlunge() {
        return upperNullAxisPlunge;
    }

    public int getUpperTensionAxisPlunge() {
        return upperTensionAxisPlunge;
    }

    /**
     * @return Set of {@link GlobalCMTID} which fulfill queries
     */
    public Set<GlobalCMTID> search() {
        return GlobalCMTCatalog.allNDK().parallelStream().filter(ndk -> ndk.fulfill(this)).map(NDK::getGlobalCMTID)
                .collect(Collectors.toSet());
    }

    /**
     * @return select an id
     */
    public GlobalCMTID select() {
        GlobalCMTID[] ids = search().toArray(new GlobalCMTID[0]);
        if (ids.length == 0) throw new RuntimeException("No ID matches");
        if (ids.length == 1) return ids[0];
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new CloseShieldInputStream(System.in)))) {
            System.out.println("Which ID do you want to use?");
            System.out.println("# ID date time latitude longitude depth");
            for (int i = 0; i < ids.length; i++) {
                System.out.print((i + 1) + " ");
                printIDinformation(ids[i]);
            }
            // byte[] inputByte = new byte[4];
            int k = -1;
            while (k < 0) {
                String numStr = br.readLine();
                if (NumberUtils.isCreatable(numStr)) k = Integer.parseInt(numStr);
                if (k < 1 || ids.length <= k - 1) {
                    System.out.println("... which one? " + 0 + " - " + (ids.length - 1));
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
     * Set centroid timeshift range
     *
     * @param lowerCentroidTimeShift [s] lower limit of centroid time shift
     * @param upperCentroidTimeShift [s] upper limit of centroid time shift
     */
    public GlobalCMTSearch setCentroidTimeShiftRange(double lowerCentroidTimeShift, double upperCentroidTimeShift) {
        if (upperCentroidTimeShift < lowerCentroidTimeShift)
            throw new RuntimeException("Input centroid time shift range is invalid");
        this.lowerCentroidTimeShift = lowerCentroidTimeShift;
        this.upperCentroidTimeShift = upperCentroidTimeShift;
        return this;
    }

    /**
     * @param lowerHalfDuration [s] lower limit of half duration
     * @param upperHalfDuration [s] uppper limit of half duration
     */
    public GlobalCMTSearch setHalfDurationRange(double lowerHalfDuration, double upperHalfDuration) {
        if (upperHalfDuration < lowerHalfDuration)
            throw new IllegalArgumentException("Input halfDuration range is invalid.");
        this.lowerHalfDuration = lowerHalfDuration;
        this.upperHalfDuration = upperHalfDuration;
        return this;
    }

    /**
     * @return [s] lower limit of half duration
     */
    public double getLowerHalfDuration() {
        return lowerHalfDuration;
    }

    /**
     * @return [s] upper limit of half duration
     */
    public double getUpperHalfDuration() {
        return upperHalfDuration;
    }

    /**
     * Set depth range (<b>NOT</b> radius)
     *
     * @param lowerDepth [km] lower limit of depth
     * @param upperDepth [km] upper limit of depth
     */
    public GlobalCMTSearch setDepthRange(double lowerDepth, double upperDepth) {
        if (lowerDepth < 0 || upperDepth < lowerDepth)
            throw new IllegalArgumentException("input depth range is invalid");
        this.lowerDepth = lowerDepth;
        this.upperDepth = upperDepth;
        return this;
    }

    /**
     * Latitude range<br>
     * Default:[-90:90]<br>
     * If you do not want to set a min or max, -90 or 90
     * <p>
     * if invalid values are input, {@link IllegalArgumentException}
     *
     * @param lowerLatitude [deg] [-90, upperLatitude)
     * @param upperLatitude [deg] (lowerLatitude, 90]
     */
    public GlobalCMTSearch setLatitudeRange(double lowerLatitude, double upperLatitude) {
        if (lowerLatitude < -90 || upperLatitude < lowerLatitude || 90 < upperLatitude)
            throw new IllegalArgumentException("Input latitude range is invalid");
        this.lowerLatitude = lowerLatitude;
        this.upperLatitude = upperLatitude;
        return this;
    }

    /**
     * Longitude range<br>
     * Default:[-180:180]<br>
     *
     * @param lowerLongitude [-180, upperLongitude or 180)
     * @param upperLongitude (lowerLongitude, 360)
     */
    public GlobalCMTSearch setLongitudeRange(double lowerLongitude, double upperLongitude) {
        if (upperLongitude < lowerLongitude || 180 <= lowerLongitude || lowerLongitude < -180 || 360 < upperLongitude)
            throw new IllegalArgumentException("Invalid longitude range.");
        this.lowerLongitude = lowerLongitude;
        this.upperLongitude = upperLongitude;
        return this;
    }

    /**
     * Set mb range
     *
     * @param lowerMb lower limit of Mb
     * @param upperMb upper limit of Mb
     */
    public GlobalCMTSearch setMbRange(double lowerMb, double upperMb) {
        if (upperMb < lowerMb) throw new RuntimeException("Input Mb range is invalid");
        this.lowerMb = lowerMb;
        this.upperMb = upperMb;
        return this;
    }

    /**
     * Set Ms range
     *
     * @param lowerMs lower limit of Ms
     * @param upperMs upper limit of Ms
     */
    public GlobalCMTSearch setMsRange(double lowerMs, double upperMs) {
        if (upperMs < lowerMs) throw new RuntimeException("input Ms range is invalid");
        this.lowerMs = lowerMs;
        this.upperMs = upperMs;
        return this;
    }

    /**
     * Set Mw Range
     *
     * @param lowerMw lower limit of Mw range
     * @param upperMw upper limit of Mw range
     */
    public GlobalCMTSearch setMwRange(double lowerMw, double upperMw) {
        if (upperMw < lowerMw) throw new RuntimeException("input Mw range is invalid");
        this.lowerMw = lowerMw;
        this.upperMw = upperMw;
        return this;
    }

    /**
     * Set tension axis range [0:90]
     *
     * @param lowerNullAxisPlunge lower limit of Null axis plunge
     * @param upperNullAxisPlunge upper limit of Null axis plunge
     */
    public GlobalCMTSearch setNullAxisPlungeRange(int lowerNullAxisPlunge, int upperNullAxisPlunge) {
        if (upperNullAxisPlunge < lowerNullAxisPlunge || 90 < upperNullAxisPlunge || lowerNullAxisPlunge < 0)
            throw new RuntimeException("input null axis plunge range is invalid");
        this.lowerNullAxisPlunge = lowerNullAxisPlunge;
        this.upperNullAxisPlunge = upperNullAxisPlunge;
        return this;
    }

    /**
     * Set tension axis range [0:90]
     *
     * @param lowerTensionAxisPlunge [deg] lower limit of tension axis plunge
     * @param upperTensionAxisPlunge [deg] upper limit of tension axis plunge
     */
    public GlobalCMTSearch setTensionAxisPlungeRange(int lowerTensionAxisPlunge, int upperTensionAxisPlunge) {
        if (lowerTensionAxisPlunge < 0 || upperTensionAxisPlunge < lowerTensionAxisPlunge ||
                90 < upperTensionAxisPlunge) throw new RuntimeException("invalid tension axis plunge range");
        this.lowerTensionAxisPlunge = lowerTensionAxisPlunge;
        this.upperTensionAxisPlunge = upperTensionAxisPlunge;
        return this;
    }
    
}
