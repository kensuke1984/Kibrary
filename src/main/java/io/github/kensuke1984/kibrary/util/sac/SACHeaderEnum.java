package io.github.kensuke1984.kibrary.util.sac;

/**
 * Header values in a SAC file.
 * <p>
 * -1 internal( If the name is INTERNAL then that variable is internal to SAC
 * and not normally of interest to the user.)
 * <p>
 * それぞれの名前は、代入されている数値のタイプを記憶する<br>
 * 0:float, 1:integer, 2:enumerated(String), 3:logical(true or false),
 * 8:Alphanumeric(String 8letters), 16:Alphanumeric(String 16letters), 99 unused
 * (If the name is UNUSED then that variable is not currently being used)
 *
 * @author Kensuke Konishi
 * @version 0.0.1.1
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 * @see <a href=https://ds.iris.edu/files/sac-manual/manual/file_format.html>SAC data format</a>
 */
public enum SACHeaderEnum {
    /**
     * Increment between evenly spaced samples (nominal value). [required]
     */
    DELTA(0), // 0
    /**
     * Minimum value of dependent variable.
     */
    DEPMIN(0), // 1
    /**
     * Maximum value of dependent variable.
     */
    DEPMAX(0), // 2
    /**
     * Multiplying scale factor for dependent variable [not currently used]
     */
    SCALE(0), // 3
    /**
     * Observed increment if different from nominal value.
     */
    ODELTA(0), // 4
    /**
     * Beginning value of the independent variable. [required]
     */
    B(0), // 5
    /**
     * Ending value of the independent variable. [required]
     */
    E(0), // 6
    /**
     * Event origin time (seconds relative to reference time.)
     */
    O(0), // 7
    /**
     * First arrival time (seconds relative to reference time.)
     */
    A(0), // 8
    /**
     * INTERNAL (no use)
     */
    num9(-1), // 9
    /**
     * User defined time picks or markers, n = 0 - 9 (seconds relative to
     * reference time).
     */
    T0(0), // 10
    /**
     * User defined time picks or markers, n = 0 - 9 (seconds relative to
     * reference time).
     */
    T1(0), // 11
    /**
     * User defined time picks or markers, n = 0 - 9 (seconds relative to
     * reference time).
     */
    T2(0), // 12
    /**
     * User defined time picks or markers, n = 0 - 9 (seconds relative to
     * reference time).
     */
    T3(0), // 13
    /**
     * User defined time picks or markers, n = 0 - 9 (seconds relative to
     * reference time).
     */
    T4(0), // 14
    /**
     * User defined time picks or markers, n = 0 - 9 (seconds relative to
     * reference time).
     */
    T5(0), // 15
    /**
     * User defined time picks or markers, n = 0 - 9 (seconds relative to
     * reference time).
     */
    T6(0), // 16
    /**
     * User defined time picks or markers, n = 0 - 9 (seconds relative to
     * reference time).
     */
    T7(0), // 17
    /**
     * User defined time picks or markers, n = 0 - 9 (seconds relative to
     * reference time).
     */
    T8(0), // 18
    /**
     * User defined time picks or markers, n = 0 - 9 (seconds relative to
     * reference time).
     */
    T9(0), // 19
    /**
     * Fini or end of event time (seconds relative to reference time.)
     */
    F(0), // 20
    /**
     * Instrument response parameters, n=0,9. [not currently used]
     */
    RESP0(0), // 21
    /**
     * Instrument response parameters, n=0,9. [not currently used]
     */
    RESP1(0), // 22
    /**
     * Instrument response parameters, n=0,9. [not currently used]
     */
    RESP2(0), // 23
    /**
     * Instrument response parameters, n=0,9. [not currently used]
     */
    RESP3(0), // 24
    /**
     * Instrument response parameters, n=0,9. [not currently used]
     */
    RESP4(0), // 25
    /**
     * Instrument response parameters, n=0,9. [not currently used]
     */
    RESP5(0), // 26
    /**
     * Instrument response parameters, n=0,9. [not currently used]
     */
    RESP6(0), // 27
    /**
     * Instrument response parameters, n=0,9. [not currently used]
     */
    RESP7(0), // 28
    /**
     * Instrument response parameters, n=0,9. [not currently used]
     */
    RESP8(0), // 29
    /**
     * Instrument response parameters, n=0,9. [not currently used]
     */
    RESP9(0), // 30
    /**
     * Station latitude (degrees, north positive)
     */
    STLA(0), // 31
    /**
     * Station longitude (degrees, east positive).
     */
    STLO(0), // 32
    /**
     * Station elevation (meters). [not currently used]
     */
    STEL(0), // 33
    /**
     * Station depth below surface (meters). [not currently used]
     */
    STDP(0), // 34
    /**
     * Event latitude (degrees, north positive).
     */
    EVLA(0), // 35
    /**
     * Event longitude (degrees, east positive).
     */
    EVLO(0), // 36
    /**
     * Event elevation (meters). [not currently used]
     */
    EVEL(0), // 37
    /**
     * Event depth below surface (meters). [not currently used]
     */
    EVDP(0), // 38
    /**
     * Event magnitude.
     */
    MAG(0), // 39

    /**
     * User defined variable storage area, n = 0,9.
     * (use for filter band0)
     */
    USER0(0), // 40
    /**
     * User defined variable storage area, n = 0,9.
     * (use for filter band1)
     */
    USER1(0), // 41
    /**
     * User defined variable storage area, n = 0,9.
     */
    USER2(0), // 42
    /**
     * User defined variable storage area, n = 0,9.
     */
    USER3(0), // 43
    /**
     * User defined variable storage area, n = 0,9.
     */
    USER4(0), // 44
    /**
     * User defined variable storage area, n = 0,9.
     */
    USER5(0), // 45
    /**
     * User defined variable storage area, n = 0,9.
     */
    USER6(0), // 46
    /**
     * User defined variable storage area, n = 0,9.
     */
    USER7(0), // 47
    /**
     * User defined variable storage area, n = 0,9.
     */
    USER8(0), // 48
    /**
     * User defined variable storage area, n = 0,9.
     */
    USER9(0), // 49

    /**
     * Station to event distance (km).
     */
    DIST(0), // 50
    /**
     * Event to station azimuth (degrees).
     */
    AZ(0), // 51
    /**
     * Station to event azimuth (degrees).
     */
    BAZ(0), // 52
    /**
     * Station to event great circle arc length (degrees).
     */
    GCARC(0), // 53
    /**
     * INTERNAL
     */
    num54(-1), // 54
    /**
     * INTERNAL
     */
    num55(-1), // 55
    /**
     * Mean value of dependent variable.
     */
    DEPMEN(0), // 56
    /**
     * Component azimuth (degrees clockwise from north).
     */
    CMPAZ(0), // 57
    /**
     * Component incident angle (degrees from vertical).
     */
    CMPINC(0), // 58
    /**
     * Minimum value of X (Spectral files only)
     */
    XMINIMUM(0), // 59
    /**
     * Maximum value of X (Spectral files only)
     */
    XMAXIMUM(0), // 60
    /**
     * Minimum value of Y (Spectral files only)
     */
    YMINIMUM(0), // 61
    /**
     * Maximum value of Y (Spectral files only)
     */
    YMAXIMUM(0), // 62
    /**
     * UNUSED
     */
    num63(99), // 63
    /**
     * UNUSED
     */
    num64(99), // 64
    /**
     * UNUSED
     */
    num65(99), // 65
    /**
     * UNUSED
     */
    num66(99), // 66
    /**
     * UNUSED
     */
    num67(99), // 67
    /**
     * UNUSED
     */
    num68(99), // 68
    /**
     * UNUSED
     */
    num69(99), // 69
    /**
     * GMT year corresponding to reference (zero) time in file.
     */
    NZYEAR(1), // 70
    /**
     * GMT julian day.
     */
    NZJDAY(1), // 71
    /**
     * GMT hour.
     */
    NZHOUR(1), // 72
    /**
     * GMT minute.
     */
    NZMIN(1), // 73
    /**
     * GMT second.
     */
    NZSEC(1), // 74
    /**
     * GMT millisecond.
     */
    NZMSEC(1), // 75
    /**
     * Header version number. Current value is the integer 6. Older version data
     * (NVHDR &gt; 6) are automatically updated when read into sac. [required]
     */
    NVHDR(1), // 76
    /**
     * Origin ID (CSS 3.0)
     */
    NORID(1), // 77
    /**
     * Event ID (CSS 3.0)
     */
    NEVID(1), // 78
    /**
     * Number of points per data component. [required]
     */
    NPTS(1), // 79
    /**
     * INTERNAL
     */
    num80(-1), // 80
    /**
     * Waveform ID (CSS 3.0)
     */
    NWFID(1), // 81
    /**
     * Spectral Length (Spectral files only)
     */
    NXSIZE(1), // 82
    /**
     * Spectral Width (Spectral files only)
     */
    NYSIZE(1), // 83
    /**
     * UNUSED
     */
    num84(99), // 84
    /**
     * Type of file [required]: 1 ITIME {Time series file} IRLIM {Spectral
     * file---real and imaginary} IAMPH {Spectral file---amplitude and phase}
     * IXY {General x versus y data} IXYZ {General XYZ (3-D) file}
     */
    IFTYPE(2), // 85
    /**
     * Type of dependent variable:
     * <p>
     * 5 IUNKN (Unknown), 6 IDISP (Displacement in nm), 7 IVEL (Velocity in
     * nm/sec) IVOLTS (Velocity in volts) IACC (Acceleration in nm/sec/sec)
     */
    IDEP(2), // 86
    /**
     * Reference time equivalence:
     * <p>
     * IUNKN (Unknown) IB (Begin time) IDAY (Midnight of refernece GMT day) IO
     * (Event origin time) IA (First arrival time) ITn (User defined time pick
     * n, n=0,9)
     */
    IZTYPE(2), // 87
    /**
     * UNUSED
     */
    num88(99), // 88
    /**
     * Type of recording instrument. [not currently used]
     */
    IINST(2), // 89 IINST???
    /**
     * Station geographic region. [not currently used]
     */
    ISTREG(2), // 90
    /**
     * Event geographic region. [not currently used]
     */
    IEVREG(2), // 91
    /**
     * Type of event:
     * <p>
     * IUNKN (Unknown) INUCL (Nuclear event) IPREN (Nuclear pre-shot event)
     * IPOSTN (Nuclear post-shot event) IQUAKE (Earthquake) IPREQ (Foreshock)
     * IPOSTQ (Aftershock) ICHEM (Chemical explosion) IQB (Quarry or mine blast
     * confirmed by quarry) IQB1 (Quarry/mine blast with designed shot
     * info-ripple fired) IQB2 (Quarry/mine blast with observed shot info-ripple
     * fired) IQBX (Quarry or mine blast - single shot) IQMT
     * (Quarry/mining-induced events: tremors and rockbursts) IEQ (Earthquake)
     * IEQ1 (Earthquakes in a swarm or aftershock sequence) IEQ2 (Felt
     * earthquake) IME (Marine explosion) IEX (Other explosion) INU (Nuclear
     * explosion) INC (Nuclear cavity collapse) IO (Other source of known
     * origin) IL (Local event of unknown origin) IR (Regional event of unknown
     * origin) IT (Teleseismic event of unknown origin) IU (Undetermined or
     * conflicting information) IOTHER (Other)
     */
    IEVTYP(2), // 92
    /**
     * Quality of data [not currently used]:
     * <p>
     * IGOOD (Good data) IGLCH (Glitches) IDROP (Dropouts) ILOWSN (Low signal to
     * noise ratio) IOTHER (Other)
     */
    IQUAL(2), // 93
    /**
     * Synthetic data flag [not currently used]:
     * <p>
     * IRLDTA (Real data) ????? (Flags for various synthetic seismogram codes)
     */
    ISYNTH(2), // 94
    /**
     * Magnitude type: IMB (Bodywave Magnitude) IMS (Surfacewave Magnitude) IML
     * (Local Magnitude) IMW (Moment Magnitude) IMD (Duration Magnitude) IMX
     * (User Defined Magnitude)
     */
    IMAGTYP(2), // 95
    /**
     * Source of magnitude information: INEIC (National Earthquake Information
     * Center) IPDE (Preliminary Determination of Epicenter) IISC (Internation
     * Seismological Centre) IREB (Reviewed Event Bulletin) IUSGS (US Geological
     * Survey) IBRK (UC Berkeley) ICALTECH (California Institute of Technology)
     * ILLNL (Lawrence Livermore National Laboratory) IEVLOC (Event Location
     * (computer program) ) IJSOP (Joint Seismic Observation Program) IUSER (The
     * individual using SAC2000) IUNKNOWN (unknown)
     */
    IMAGSRC(2), // 96
    /**
     * UNUSED
     */
    num97(99), // 97
    /**
     * UNUSED
     */
    num98(99), // 98
    /**
     * UNUSED
     */
    num99(99), // 99
    /**
     * UNUSED
     */
    num100(99), // 100
    /**
     * UNUSED
     */
    num101(99), // 101
    /**
     * UNUSED
     */
    num102(99), // 102
    /**
     * UNUSED
     */
    num103(99), // 103
    /**
     * UNUSED
     */
    num104(99), // 104
    /**
     * TRUE if data is evenly spaced. [required]
     */
    LEVEN(3), // 105
    /**
     * TRUE if station components have a positive polarity (left-hand rule).
     */
    LPSPOL(3), // 106
    /**
     * TRUE if it is okay to overwrite this file on disk.
     */
    LOVROK(3), // 107
    /**
     * TRUE if DIST, AZ, BAZ, and GCARC are to be calculated from station and
     * event coordinates.
     */
    LCALDA(3), // 108
    /**
     * UNUSED
     */
    num109(99), // 109
    /**
     * Station name.
     */
    KSTNM(8), // 110-111
    /**
     * Event name.
     */
    KEVNM(16), // 112-115
    /**
     * Hole identification if nuclear event.
     */
    KHOLE(8), // 116-117
    /**
     * Event origin time identification.
     */
    KO(8), // 118-119
    /**
     * First arrival time identification.
     */
    KA(8), // 120-121
    /**
     * A User defined time pick identifications, n = 0 - 9.
     */
    KT0(8), // 122-123
    /**
     * A User defined time pick identifications, n = 0 - 9.
     */
    KT1(8), // 124-125
    /**
     * A User defined time pick identifications, n = 0 - 9.
     */
    KT2(8), // 126-127
    /**
     * A User defined time pick identifications, n = 0 - 9.
     */
    KT3(8), // 128-129
    /**
     * A User defined time pick identifications, n = 0 - 9.
     */
    KT4(8), // 130-131
    /**
     * A User defined time pick identifications, n = 0 - 9.
     */
    KT5(8), // 132-133
    /**
     * A User defined time pick identifications, n = 0 - 9.
     */
    KT6(8), // 134-135
    /**
     * A User defined time pick identifications, n = 0 - 9.
     */
    KT7(8), // 136-137
    /**
     * A User defined time pick identifications, n = 0 - 9.
     */
    KT8(8), // 138-139
    /**
     * A User defined time pick identifications, n = 0 - 9.
     */
    KT9(8), // 140-141
    /**
     * Fini identification.
     */
    KF(8), // 142-143
    /**
     * User defined variable storage area, n = 0,2.
     */
    KUSER0(8), // 144-145
    /**
     * User defined variable storage area, n = 0,2.
     */
    KUSER1(8), // 146-147
    /**
     * User defined variable storage area, n = 0,2.
     */
    KUSER2(8), // 148-149
    /**
     * Component name.
     */
    KCMPNM(8), // 150-151
    /**
     * Name of seismic network.
     */
    KNETWK(8), // 152-153
    /**
     * Date data was read onto computer.
     */
    KDATRD(8), // 154-155
    /**
     * Generic name of recording instrument.
     */
    KINST(8); // 156-157

    /**
     * 0:float, 1:int, 2:enumerated(String), 3:logical(true or false),
     * 8:Alphanumeric(String 8letters), 16:Alphanumeric(String 16letters)
     */
    private int type;

    /**
     * @return 0:float, 1:int, 2:enumerated(String), 3:logical(true or false),
     * 8:Alphanumeric(String 8letters), 16:Alphanumeric(String
     * 16letters)
     */
    public int typeOf() {
        return type;
    }

    private SACHeaderEnum(int n) {
        type = n;
    }

}
