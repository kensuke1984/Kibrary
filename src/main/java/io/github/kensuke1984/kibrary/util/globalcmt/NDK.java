package io.github.kensuke1984.kibrary.util.globalcmt;

import io.github.kensuke1984.kibrary.datacorrection.MomentTensor;
import io.github.kensuke1984.kibrary.util.Location;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * NDK format in Global CMT Catalog
 * This class is <b>IMMUTABLE</b> <br>
 * ==================================
 * This file contains an explanation of the "ndk" file format used to store and
 * distribute the Global Centroid-Moment-Tensor (CMT) catalog (formerly the
 * Harvard CMT catalog).
 * <p>
 * The "ndk" format replaces the earlier "dek" format.
 * ============================================================================
 * 12345678901234567890123456789012345678901234567890123456789012345678901234567890
 * <p>
 * The format is ASCII and uses five 80-character lines per earthquake.
 * ============================================================================
 * ==== <br>
 * Notes (additional information):
 * <p>
 * (1) CMT event names follow two conventions. Older events use an 8-character
 * name with the structure XMMDDYYZ, where MMDDYY represents the date of the
 * event, Z is a letter (A-Z followed by a-z) distinguishing different events on
 * the same day, and X is a letter (B,M,Z,C,...) used to identify the types of
 * data used in the inversion. Newer events use 14-character event names with
 * the structure XYYYYMMDDhhmmZ, in which the time is given to greater
 * precision, and the initial letter is limited to four possibilities: B - body
 * waves only, S - surface waves only, M - mantle waves only, C - a combination
 * of data types.
 * <p>
 * (2) The source duration is generally estimated using an empirically
 * determined relationship such that the duration increases as the cube root of
 * the scalar moment. Specifically, we currently use a relationship where the
 * half duration for an event with moment 10**24 is 1.05 seconds, and for an
 * event with moment 10**27 is 10.5 seconds.
 * <p>
 * (3) For some small earthquakes for which the azimuthal distribution of
 * stations with useful seismograms is poor, we constrain the epicenter of the
 * event to the reference location. This is reflected in the catalog by standard
 * errors of 0.0 for both the centroid latitude and the centroid longitude.
 * <p>
 * (4) For some very shallow earthquakes, the CMT inversion does not well
 * constrain the vertical-dip-slip components of the moment tensor (Mrt and
 * Mrp), and we constrain these components to zero in the inversion. The
 * standard errors for Mrt and Mrp are set to zero in this case.
 * ============================================================================
 *
 * @author Kensuke Konishi
 * @version 0.0.6.5
 * @see <a href=
 * http://www.ldeo.columbia.edu/~gcmt/projects/CMT/catalog/allorder.ndk_explained>official
 * guide</a>
 */
final public class NDK implements GlobalCMTData {

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.S");
    /**
     * Hypocenter reference catalog [1-4] (e.g., PDE for USGS location, ISC for ISC
     * catalog, SWE for surface-wave location, [Ekstrom, BSSA, 2006])
     */
    private String hypocenterReferenceCatalog;
    /**
     * [6-15] Date of reference event [17-26] Time of reference event
     */
    private LocalDateTime referenceDateTime;
    /**
     * hypocenter location [28-33] Latitude [35-41] Longitude [43-47] Depth
     */
    private Location hypocenterLocation;
    /**
     * [49-55] Reported magnitudes, usually mb and MS
     */
    private double mb;
    /**
     * [49-55] Reported magnitudes, usually mb and MS
     */
    private double ms;
    /**
     * [57-80] Geographical location (24 characters)
     */
    private String geographicalLocation;
    /**
     * [1-16] CMT event name.
     * This string is a unique CMT-event identifier. Older events have
     * 8-character names, current ones have 14-character names. See note (1)
     * below for the naming conventions used. (The first letter is ignored.)
     */
    private GlobalCMTID id;

    /**
     * [18-61] Data used in the CMT inversion. Three data types may be used:
     * Long-period body waves (B), Intermediate-period surface waves (S), and
     * long-period mantle waves (M). For each data type, three values are given:
     * the number of stations used, the number of components used, and the
     * shortest period used.
     */
    private int[] b;
    private int[] s;
    private int[] m;
    /**
     * [63-68] Type of source inverted for: "CMT: 0" - general moment tensor;
     * "CMT: 1" - moment tensor with constraint of zero trace (standard);
     * "CMT: 2" - double-couple source.
     */
    private int cmtType;
    /**
     * [70-80] Type and duration of moment-rate function assumed in the
     * inversion. "TRIHD" indicates a triangular moment-rate function, "BOXHD"
     * indicates a boxcar moment-rate function. The value given is half the
     * duration of the moment-rate function. This value is assumed in the
     * inversion, following a standard scaling relationship (see note (2)
     * below), and is not derived from the analysis.
     */
    private String momentRateFunctionType;
    /**
     * half duration of the moment rate function
     */
    private double halfDurationMomentRateFunction;
    /**
     * Third line: CMT info (2) <br>
     * [1-58] Centroid parameters determined in the inversion. Centroid time,
     * given with respect to the reference time, centroid latitude, centroid
     * longitude, and centroid depth. The value of each variable is followed by
     * its estimated standard error. See note (3) below for cases in which the
     * hypocentral coordinates are held fixed. Centroidとreference Timeとの違い
     */
    private double timeDifference;

    private Location centroidLocation;
    /**
     * [60-63] Type of depth. "FREE" indicates that the depth was a result of
     * the inversion; "FIX " that the depth was fixed and not inverted for;
     * "BDY " that the depth was fixed based on modeling of broad-band P
     * waveforms.
     */
    private String depthType;
    /**
     * [65-80] Timestamp. This 16-character string identifies the type of
     * analysis that led to the given CMT results and, for recent events, the
     * date and time of the analysis. This is useful to distinguish Quick CMTs
     * ("Q-"), calculated within hours of an event, from Standard CMTs ("S-"),
     * which are calculated later. The format for this string should not be
     * considered fixed.
     */
    private String timeStamp;
    /**
     * [1-2] The exponent for all following moment values. For example, if the
     * exponent is given as 24, the moment values that follow, expressed in
     * dyne-cm, should be multiplied by 10**24. [3-80] The six moment-tensor
     * elements: Mrr, Mtt, Mpp, Mrt, Mrp, Mtp, where r is up, t is south, and p
     * is east. See Aki and Richards for conversions to other coordinate
     * systems. The value of each moment-tensor element is followed by its
     * estimated standard error. See note (4) below for cases in which some
     * elements are constrained in the inversion.
     */
    private int momentExponent;

    private MomentTensor momentTensor;
    /**
     * [1-3] Version code. This three-character string is used to track the
     * version of the program that generates the "ndk" file.
     */
    private String versionCode;

    /**
     * [4-48] Moment tensor expressed in its principal-axis system: eigenvalue,
     * plunge, and azimuth of the three eigenvectors. The eigenvalue should be
     * multiplied by 10**(exponent) as given on line four.
     */
    private double eigenValue0;
    private double eigenValue1;
    private double eigenValue2;
    private double plunge0;
    private double plunge1;
    private double plunge2;
    private double azimuth0;
    private double azimuth1;
    private double azimuth2;
    /**
     * [50-56] Scalar moment, to be multiplied by 10**(exponent) as given on
     * line four. dyne*cm
     */
    private double scalarMoment;
    /**
     * [58-80] Strike, dip, and rake for first nodal plane of the
     * best-double-couple mechanism, repeated for the second nodal plane. The
     * angles are defined as in Aki and Richards.
     */
    private int strike0;
    private int dip0;
    private int rake0;
    private int strike1;
    private int dip1;
    private int rake1;

    private NDK() {
    }

    /**
     * creates an NDK from 5 lines
     *
     * @param lines Lines expressing one NDK
     */
    static NDK read(String... lines) {
        if (lines.length != 5) throw new IllegalArgumentException("Invalid input for an NDK");

        NDK ndk = new NDK();
        String[] parts;
        // line 1
        parts = lines[0].split("\\s+");
        ndk.hypocenterReferenceCatalog = parts[0];
        ndk.referenceDateTime = parseDateTime(parts[1], parts[2]);
        // System.out.println(referenceCalendar.get(Calendar.DAY_OF_YEAR));
        ndk.hypocenterLocation = new Location(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]),
                6371 - Double.parseDouble(parts[5]));
        ndk.mb = Double.parseDouble(parts[6]);
        ndk.ms = Double.parseDouble(parts[7]);
        ndk.geographicalLocation = lines[0].substring(56).trim();

        // line2
        parts = lines[1].split("\\s+");
        ndk.id = new GlobalCMTID(parts[0].substring(1));
        String[] bsmParts =
                lines[1].substring(17, 61).replace("B:", "").replace("S:", "").trim().replace("M:", "").split("\\s+");
        ndk.b = new int[3];
        ndk.b[0] = Integer.parseInt(bsmParts[0]);
        ndk.b[1] = Integer.parseInt(bsmParts[1]);
        ndk.b[2] = Integer.parseInt(bsmParts[2]);
        ndk.s = new int[3];
        ndk.s[0] = Integer.parseInt(bsmParts[3]);
        ndk.s[1] = Integer.parseInt(bsmParts[4]);
        ndk.s[2] = Integer.parseInt(bsmParts[5]);
        ndk.m = new int[3];
        ndk.m[0] = Integer.parseInt(bsmParts[6]);
        ndk.m[1] = Integer.parseInt(bsmParts[7]);
        ndk.m[2] = Integer.parseInt(bsmParts[8]);
        String[] cmtParts = lines[1].substring(61).trim().split("\\s+");
        ndk.cmtType = Integer.parseInt(cmtParts[1]);
        ndk.momentRateFunctionType = cmtParts[2].substring(0, 5);
        ndk.halfDurationMomentRateFunction = Double.parseDouble(cmtParts[3]);

        // line3
        parts = lines[2].split("\\s+");
        ndk.timeDifference = Double.parseDouble(parts[1]);
        ndk.centroidLocation = new Location(Double.parseDouble(parts[3]), Double.parseDouble(parts[5]),
                6371 - Double.parseDouble(parts[7]));
        ndk.depthType = parts[9];
        ndk.timeStamp = parts[10];

        // line 4
        parts = lines[3].split("\\s+");
        ndk.momentExponent = Integer.parseInt(parts[0]);
        double mrr = Double.parseDouble(parts[1]);
        double mtt = Double.parseDouble(parts[3]);
        double mpp = Double.parseDouble(parts[5]);
        double mrt = Double.parseDouble(parts[7]);
        double mrp = Double.parseDouble(parts[9]);
        double mtp = Double.parseDouble(parts[11]);

        // line5
        parts = lines[4].split("\\s+");
        ndk.versionCode = parts[0];
        ndk.scalarMoment = Double.parseDouble(parts[10]) * Math.pow(10, ndk.momentExponent);
        double m0 = ndk.scalarMoment / 100000 / 100;
        // 10 ^5 dyne = N, 100 cm = 1m
        double mw = MomentTensor.toMw(m0);
        ndk.momentTensor = new MomentTensor(mrr, mtt, mpp, mrt, mrp, mtp, ndk.momentExponent, mw);
        ndk.eigenValue0 = Double.parseDouble(parts[1]);
        ndk.eigenValue1 = Double.parseDouble(parts[4]);
        ndk.eigenValue2 = Double.parseDouble(parts[7]);
        ndk.plunge0 = Double.parseDouble(parts[2]);
        ndk.plunge1 = Double.parseDouble(parts[5]);
        ndk.plunge2 = Double.parseDouble(parts[8]);
        ndk.azimuth0 = Double.parseDouble(parts[3]);
        ndk.azimuth1 = Double.parseDouble(parts[6]);
        ndk.azimuth2 = Double.parseDouble(parts[9]);
        ndk.strike0 = Integer.parseInt(parts[11]);
        ndk.dip0 = Integer.parseInt(parts[12]);
        ndk.rake0 = Integer.parseInt(parts[13]);
        ndk.strike1 = Integer.parseInt(parts[14]);
        ndk.dip1 = Integer.parseInt(parts[15]);
        ndk.rake1 = Integer.parseInt(parts[16]);
        return ndk;
    }

    /**
     * @param date YYYY/MM/DD
     * @param time HH:MM:SS.MS
     */
    private static LocalDateTime parseDateTime(String date, String time) {
        return LocalDateTime.parse(date + " " + time, dateFormat);
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (id == null ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        NDK other = (NDK) obj;
        if (id == null) return other.id == null;
        else return id.equals(other.id);
    }

    @Override
    public GlobalCMTID getGlobalCMTID() {
        return id;
    }

    /**
     * @param search conditions for NDK
     * @return if this fulfills "search"
     */
    boolean fulfill(GlobalCMTSearch search) {
        LocalDateTime cmtDate = getCMTTime();
        if (search.getStartDate().isAfter(cmtDate) || search.getEndDate().isBefore(cmtDate)) return false;
        if (!search.getPredicateSet().stream().allMatch(p -> p.test(this))) return false;

        // latitude
        double latitude = centroidLocation.getLatitude();
        if (latitude < search.getLowerLatitude() || search.getUpperLatitude() < latitude) return false;
        // longitude
        double lowerLongitude = search.getLowerLongitude();
        double upperLongitude = search.getUpperLongitude();
        double longitude = centroidLocation.getLongitude();

        // longitude [-180, 180)
        if (upperLongitude < 180) if (upperLongitude < longitude || longitude < lowerLongitude) return false;
        // longitude [0, 360)
        if (180 <= upperLongitude) if (longitude < lowerLongitude && upperLongitude - 360 < longitude) return false;

        // depth
        double depth = 6371 - centroidLocation.getR();
        if (depth < search.getLowerDepth() || search.getUpperDepth() < depth) return false;

        // timeshift
        if (timeDifference < search.getLowerCentroidTimeShift() || search.getUpperCentroidTimeShift() < timeDifference)
            return false;
        // body wave magnitude
        if (mb < search.getLowerMb() || search.getUpperMb() < mb) return false;
        // surface wave magnitude
        if (ms < search.getLowerMs() || search.getUpperMs() < ms) return false;
        // moment magnitude
        if (momentTensor.getMw() < search.getLowerMw() || search.getUpperMw() < momentTensor.getMw()) return false;
        // half duration
        if (halfDurationMomentRateFunction < search.getLowerHalfDuration() ||
                search.getUpperHalfDuration() < halfDurationMomentRateFunction) return false;
        // tension axis plunge
        if (plunge0 < search.getLowerTensionAxisPlunge() || search.getUpperTensionAxisPlunge() < plunge0) return false;
        // null axis plunge
        return !(plunge1 < search.getLowerNullAxisPlunge() || search.getUpperNullAxisPlunge() < plunge1);
    }

    @Override
    public double getMb() {
        return mb;
    }

    @Override
    public double getMs() {
        return ms;
    }

    @Override
    public MomentTensor getCmt() {
        return momentTensor;
    }

    @Override
    public Location getCmtLocation() {
        return centroidLocation;
    }

    @Override
    public LocalDateTime getCMTTime() {
        int sec = (int) timeDifference;
        double ddiff = timeDifference - sec;
        long nanosec = Math.round(ddiff * 1000 * 1000 * 1000);
        return referenceDateTime.plusSeconds(sec).plusNanos(nanosec);
    }

    @Override
    public double getHalfDuration() {
        return halfDurationMomentRateFunction;
    }

    @Override
    public Location getPDELocation() {
        return hypocenterLocation;
    }

    @Override
    public LocalDateTime getPDETime() {
        return referenceDateTime;
    }

    @Override
    public String toString() {
        return id.toString();
    }
    
	@Override
	public void setCMT(MomentTensor mt) {
		momentTensor = mt;
	}
	
	@Override
	public double getTimeDifference() {
		return timeDifference;
	}
	
	@Override
	public String getHypocenterReferenceCatalog() {
		return hypocenterReferenceCatalog;
	}
	
	@Override
	public String getGeographicalLocationName() {
		return geographicalLocation;
	}

}
