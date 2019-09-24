package io.github.kensuke1984.kibrary.util.sac;

import java.io.IOException;

/**
 * Information in the header parts of a SAC file.
 * <p>This class is <b>IMMUTABLE</b></p>
 *
 * @author Kensuke Konishi
 * @version 2.0.1.1.1
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 */
class SACHeader implements SACHeaderData, Cloneable {

    private double delta = -12345;
    private double depmin = -12345;
    private double depmax = -12345;
    private double scale = -12345;
    private double odelta = -12345;
    private double b = -12345;
    private double e = -12345;
    private double o = -12345;
    private double a = -12345;
    private double num9 = -12345;
    // T marker
    private double t0 = -12345;
    private double t1 = -12345;
    private double t2 = -12345;
    private double t3 = -12345;
    private double t4 = -12345;
    private double t5 = -12345;
    private double t6 = -12345;
    private double t7 = -12345;
    private double t8 = -12345;
    private double t9 = -12345;
    //
    private double f = -12345;
    private double resp0 = -12345;
    private double resp1 = -12345;
    private double resp2 = -12345;
    private double resp3 = -12345;
    private double resp4 = -12345;
    private double resp5 = -12345;
    private double resp6 = -12345;
    private double resp7 = -12345;
    private double resp8 = -12345;
    private double resp9 = -12345;
    private double stla = -12345;
    private double stlo = -12345;
    private double stel = -12345;
    private double stdp = -12345;
    private double evla = -12345;
    private double evlo = -12345;
    private double evel = -12345;
    private double evdp = -12345;
    private double mag = -12345;
    private double user0 = -12345;
    private double user1 = -12345;
    private double user2 = -12345;
    private double user3 = -12345;
    private double user4 = -12345;
    private double user5 = -12345;
    private double user6 = -12345;
    private double user7 = -12345;
    private double user8 = -12345;
    private double user9 = -12345;
    private double dist = -12345;
    private double az = -12345;
    private double baz = -12345;
    private double gcarc = -12345;
    private double num54 = -12345;
    private double num55 = -12345;
    private double depmen = -12345;
    private double cmpaz = -12345;
    private double cmpinc = -12345;
    private double xminimum = -12345;
    private double xmaximum = -12345;
    private double yminimum = -12345;
    private double ymaximum = -12345;
    private double num63 = -12345;
    private double num64 = -12345;
    private double num65 = -12345;
    private double num66 = -12345;
    private double num67 = -12345;
    private double num68 = -12345;
    private double num69 = -12345;
    private int nzyear = -12345;
    private int nzjday = -12345;
    private int nzhour = -12345;
    private int nzmin = -12345;
    private int nzsec = -12345;
    private int nzmsec = -12345;
    /**
     * a version of header
     */
    private int nvhdr = 6; // ヘッダーのバージョン
    private int norid = -12345;
    private int nevid = -12345;
    private int npts = -12345;
    private int num80 = -12345;
    private int nwfid = -12345;
    private int nxsize = -12345;
    private int nysize = -12345;
    private int num84 = -12345;
    private int iftype = 1; // timeseries
    private int idep = 5; // Unknown
    private int iztype = -12345;
    private int num88 = -12345;
    private int iinst = -12345;
    private int istreg = -12345;
    private int ievreg = -12345;
    private int ievtyp = -12345;
    private int iqual = -12345;
    private int isynth = -12345;
    private int imagtyp = -12345;
    private int imagsrc = -12345;
    private int num97 = -12345;
    private int num98 = -12345;
    private int num99 = -12345;
    private int num100 = -12345;
    private int num101 = -12345;
    private int num102 = -12345;
    private int num103 = -12345;
    private int num104 = -12345;
    private boolean leven = true;
    private boolean lpspol = false;
    // write over ok or not
    private boolean lovrok = true;
    private boolean lcalda = true;
    private boolean num109 = false;
    /**
     * a name of station
     */
    private String kstnm = "-12345";
    private String kevnm = "-12345";
    private String khole = "-12345";
    private String ko = "-12345";
    private String ka = "-12345";
    private String kt0 = "-12345";
    private String kt1 = "-12345";
    private String kt2 = "-12345";
    private String kt3 = "-12345";
    private String kt4 = "-12345";
    private String kt5 = "-12345";
    private String kt6 = "-12345";
    private String kt7 = "-12345";
    private String kt8 = "-12345";
    private String kt9 = "-12345";
    private String kf = "-12345";
    private String kuser0 = "-12345";
    private String kuser1 = "-12345";
    private String kuser2 = "-12345";
    private String kcmpnm = "-12345";
    /**
     * a name of network
     */
    private String knetwk = "-12345";
    private String kdatrd = "-12345";
    private String kinst = "-12345";

    /**
     * Header values will be read in SAC named the input sacFileName
     *
     * @param sacFileName to be read
     * @throws IOException if an I/O error occurs.
     */
    SACHeader(SACFileName sacFileName) throws IOException {
        read(sacFileName);
    }

    /**
     * float &rarr; doubleの変換でゴミが着くので Stringを介して対処
     *
     * @param value in float
     * @return value in double
     */
    private static double toDouble(float value) {
        return Double.parseDouble(Float.toString(value));
    }

    @Override
    public SACHeader clone() {
        try {
            return (SACHeader) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("UNEXPecTEd");
        }
    }

    /**
     * 入力したsacファイルのヘッダーを読み込み上書きする
     *
     * @param sacFileName to read
     */
    private void read(SACFileName sacFileName) throws IOException {
        try (SACInputStream stream = new SACInputStream(sacFileName.toPath())) {
            delta = toDouble(stream.readFloat()); // 0
            depmin = toDouble(stream.readFloat()); // 1
            depmax = toDouble(stream.readFloat()); // 2
            scale = toDouble(stream.readFloat()); // 3
            odelta = toDouble(stream.readFloat());
            b = toDouble(stream.readFloat());
            e = toDouble(stream.readFloat());
            o = toDouble(stream.readFloat());
            a = toDouble(stream.readFloat());
            num9 = toDouble(stream.readFloat());
            t0 = toDouble(stream.readFloat());
            t1 = toDouble(stream.readFloat());
            t2 = toDouble(stream.readFloat());
            t3 = toDouble(stream.readFloat());
            t4 = toDouble(stream.readFloat());
            t5 = toDouble(stream.readFloat());
            t6 = toDouble(stream.readFloat());
            t7 = toDouble(stream.readFloat());
            t8 = toDouble(stream.readFloat());
            t9 = toDouble(stream.readFloat());
            f = toDouble(stream.readFloat());
            resp0 = toDouble(stream.readFloat());
            resp1 = toDouble(stream.readFloat());
            resp2 = toDouble(stream.readFloat());
            resp3 = toDouble(stream.readFloat());
            resp4 = toDouble(stream.readFloat());
            resp5 = toDouble(stream.readFloat());
            resp6 = toDouble(stream.readFloat());
            resp7 = toDouble(stream.readFloat());
            resp8 = toDouble(stream.readFloat());
            resp9 = toDouble(stream.readFloat());
            stla = toDouble(stream.readFloat());
            stlo = toDouble(stream.readFloat());
            stel = toDouble(stream.readFloat());
            stdp = toDouble(stream.readFloat());
            evla = toDouble(stream.readFloat());
            evlo = toDouble(stream.readFloat());
            evel = toDouble(stream.readFloat());
            evdp = toDouble(stream.readFloat());
            mag = toDouble(stream.readFloat());
            user0 = toDouble(stream.readFloat());
            user1 = toDouble(stream.readFloat());
            user2 = toDouble(stream.readFloat());
            user3 = toDouble(stream.readFloat());
            user4 = toDouble(stream.readFloat());
            user5 = toDouble(stream.readFloat());
            user6 = toDouble(stream.readFloat());
            user7 = toDouble(stream.readFloat());
            user8 = toDouble(stream.readFloat());
            user9 = toDouble(stream.readFloat());
            dist = toDouble(stream.readFloat());
            az = toDouble(stream.readFloat());
            baz = toDouble(stream.readFloat());
            gcarc = toDouble(stream.readFloat());
            num54 = toDouble(stream.readFloat());
            num55 = toDouble(stream.readFloat());
            depmen = toDouble(stream.readFloat());
            cmpaz = toDouble(stream.readFloat());
            cmpinc = toDouble(stream.readFloat());
            xminimum = toDouble(stream.readFloat());
            xmaximum = toDouble(stream.readFloat());
            yminimum = toDouble(stream.readFloat());
            ymaximum = toDouble(stream.readFloat());
            num63 = toDouble(stream.readFloat());
            num64 = toDouble(stream.readFloat());
            num65 = toDouble(stream.readFloat());
            num66 = toDouble(stream.readFloat());
            num67 = toDouble(stream.readFloat());
            num68 = toDouble(stream.readFloat());
            num69 = toDouble(stream.readFloat());
            nzyear = stream.readInt();
            nzjday = stream.readInt();
            nzhour = stream.readInt();
            nzmin = stream.readInt();
            nzsec = stream.readInt();
            nzmsec = stream.readInt();
            nvhdr = stream.readInt();
            norid = stream.readInt();
            nevid = stream.readInt();
            npts = stream.readInt();
            num80 = stream.readInt();
            nwfid = stream.readInt();
            nxsize = stream.readInt();
            nysize = stream.readInt();
            num84 = stream.readInt();
            iftype = stream.readInt();
            idep = stream.readInt();
            iztype = stream.readInt();
            num88 = stream.readInt();
            iinst = stream.readInt();
            istreg = stream.readInt();
            ievreg = stream.readInt();
            ievtyp = stream.readInt();
            iqual = stream.readInt();
            isynth = stream.readInt();
            imagtyp = stream.readInt();
            imagsrc = stream.readInt();
            num97 = stream.readInt();
            num98 = stream.readInt();
            num99 = stream.readInt();
            num100 = stream.readInt();
            num101 = stream.readInt();
            num102 = stream.readInt();
            num103 = stream.readInt();
            num104 = stream.readInt();
            leven = stream.readSACBoolean();
            lpspol = stream.readSACBoolean();
            lovrok = stream.readSACBoolean();
            lcalda = stream.readSACBoolean();
            num109 = stream.readSACBoolean();
            kstnm = stream.readString(8);
            kevnm = stream.readString(16);
            khole = stream.readString(8);
            ko = stream.readString(8);
            ka = stream.readString(8);
            kt0 = stream.readString(8);
            kt1 = stream.readString(8);
            kt2 = stream.readString(8);
            kt3 = stream.readString(8);
            kt4 = stream.readString(8);
            kt5 = stream.readString(8);
            kt6 = stream.readString(8);
            kt7 = stream.readString(8);
            kt8 = stream.readString(8);
            kt9 = stream.readString(8);
            kf = stream.readString(8);
            kuser0 = stream.readString(8);
            kuser1 = stream.readString(8);
            kuser2 = stream.readString(8);
            kcmpnm = stream.readString(8);
            knetwk = stream.readString(8);
            kdatrd = stream.readString(8);
            kinst = stream.readString(8);
        }
    }

    @Override
    public boolean getBoolean(SACHeaderEnum sacHeaderEnum) {
        if (sacHeaderEnum.typeOf() == 99 || sacHeaderEnum.typeOf() == -1) return getSpecialBoolean(sacHeaderEnum);
        if (sacHeaderEnum.typeOf() != 3) throw new IllegalArgumentException(sacHeaderEnum + " is not boolean");

        switch (sacHeaderEnum) {
            case LEVEN:
                return leven;
            case LPSPOL:
                return lpspol;
            case LOVROK:
                return lovrok;
            case LCALDA:
                return lcalda;
            // case num109:
            // return num109;
            default:
                System.out.println(sacHeaderEnum + " is unused now.");
                return false;
        }
        // return Boolean.parseBoolean(header.get(she));
    }

    @Override
    public int getSACEnumerated(SACHeaderEnum sacHeaderEnum) {
        if (sacHeaderEnum.typeOf() == 99 || sacHeaderEnum.typeOf() == -1) return getSpecialSacEnumerated(sacHeaderEnum);
        if (sacHeaderEnum.typeOf() != 2) throw new IllegalArgumentException(sacHeaderEnum + " is not enumerated value");

        switch (sacHeaderEnum) {
            case IFTYPE:
                return iftype;
            case IDEP:
                return idep;
            case IZTYPE:
                return iztype;
            case IINST:
                return iinst;
            case ISTREG:
                return istreg;
            case IEVREG:
                return ievreg;
            case IEVTYP:
                return ievtyp;
            case IQUAL:
                return iqual;
            case ISYNTH:
                return isynth;
            case IMAGTYP:
                return imagtyp;
            case IMAGSRC:
                return imagsrc;
            // case num88:
            // return num88;
            // case num97:
            // return num97;
            // case num98:
            // return num98;
            // case num99:
            // return num99;
            // case num100:
            // return num100;
            // case num101:
            // return num101;
            // case num102:
            // return num102;
            // case num103:
            // return num103;
            // case num104:
            // return num104;
            default:
                throw new RuntimeException(sacHeaderEnum + " is unaticipated...");
        }
    }

    @Override
    public SACHeader setBoolean(SACHeaderEnum sacHeaderEnum, boolean bool) {
        if (sacHeaderEnum.typeOf() == 99 || sacHeaderEnum.typeOf() == -1)
            throw new UnsupportedOperationException(sacHeaderEnum + " is a special boolean.");
        if (sacHeaderEnum.typeOf() != 3) throw new IllegalArgumentException(sacHeaderEnum + " is not boolean");
        SACHeader sh = clone();
        switch (sacHeaderEnum) {
            case LEVEN:
                sh.leven = bool;
                return sh;
            case LPSPOL:
                sh.lpspol = bool;
                return sh;
            case LOVROK:
                sh.lovrok = bool;
                return sh;
            case LCALDA:
                sh.lcalda = bool;
                return sh;
            // case num109:
            // return num109;
            default:
                throw new RuntimeException(sacHeaderEnum + " is unused now.");
        }
    }

    /**
     * unused or internal parameter
     *
     * @param sacHeaderEnum key for the value
     * @return boolean parameter
     */
    private boolean getSpecialBoolean(SACHeaderEnum sacHeaderEnum) {
        if (sacHeaderEnum.typeOf() != -1 && sacHeaderEnum.typeOf() != 99)
            throw new IllegalArgumentException(sacHeaderEnum + " is not Special Boolean");

        switch (sacHeaderEnum) {
            case num109:
                return num109;
            default:
                throw new RuntimeException(sacHeaderEnum + " is unaticipated...");
        }
    }

    /**
     * unused かinternalパラメタ
     *
     * @param sacHeaderEnum ヘッダー名
     * @return enumerated value
     */
    private int getSpecialSacEnumerated(SACHeaderEnum sacHeaderEnum) {
        if (sacHeaderEnum.typeOf() != -1 && sacHeaderEnum.typeOf() != 99)
            throw new IllegalArgumentException(sacHeaderEnum + " is not Special Boolean");

        switch (sacHeaderEnum) {
            case num88:
                return num88;
            case num97:
                return num97;
            case num98:
                return num98;
            case num99:
                return num99;
            case num100:
                return num100;
            case num101:
                return num101;
            case num102:
                return num102;
            case num103:
                return num103;
            case num104:
                return num104;
            default:
                throw new RuntimeException(sacHeaderEnum + " is unaticipated...");
        }
    }

    /**
     * unused or internal parameter
     *
     * @param sacHeaderEnum key
     * @return value for the sacHeaderEnum
     */
    private int getSpecialInt(SACHeaderEnum sacHeaderEnum) {
        if (sacHeaderEnum.typeOf() != -1 && sacHeaderEnum.typeOf() != 99)
            throw new RuntimeException(sacHeaderEnum + " is not special int");

        switch (sacHeaderEnum) {
            case num80:
                return num80;
            case num84:
                return num84;
            default:
                throw new RuntimeException(sacHeaderEnum + " is unaticipated...");
        }
    }

    /**
     * @param sacHeaderEnum key）
     * @return value for the sacHeaderEnum
     */
    private double getSpecialValue(SACHeaderEnum sacHeaderEnum) {
        if (sacHeaderEnum.typeOf() != -1 && sacHeaderEnum.typeOf() != 99)
            throw new RuntimeException(sacHeaderEnum + " is not Special value");
        switch (sacHeaderEnum) {
            case num54:
                return num54;
            case num55:
                return num55;
            case num63:
                return num63;
            case num64:
                return num64;
            case num65:
                return num65;
            case num66:
                return num66;
            case num67:
                return num67;
            case num68:
                return num68;
            case num69:
                return num69;
            case num9:
                return num9;
            default:
                throw new RuntimeException("Unanticipated happens");
        }
    }

    @Override
    public double getValue(SACHeaderEnum sacHeaderEnum) {
        if (sacHeaderEnum.typeOf() == -1 || sacHeaderEnum.typeOf() == 99) return getSpecialValue(sacHeaderEnum);
        else if (sacHeaderEnum.typeOf() != 0) throw new IllegalArgumentException(sacHeaderEnum + " is not float");

        switch (sacHeaderEnum) {
            case DELTA:
                return delta;
            case DEPMIN:
                return depmin;
            case DEPMAX:
                return depmax;
            case SCALE:
                return scale;
            case ODELTA:
                return odelta;
            case B:
                return b;
            case E:
                return e;
            case O:
                return o;
            case A:
                return a;
            case T0:
                return t0;
            case T1:
                return t1;
            case T2:
                return t2;
            case T3:
                return t3;
            case T4:
                return t4;
            case T5:
                return t5;
            case T6:
                return t6;
            case T7:
                return t7;
            case T8:
                return t8;
            case T9:
                return t9;
            case F:
                return f;
            case RESP0:
                return resp0;
            case RESP1:
                return resp1;
            case RESP2:
                return resp2;
            case RESP3:
                return resp3;
            case RESP4:
                return resp4;
            case RESP5:
                return resp5;
            case RESP6:
                return resp6;
            case RESP7:
                return resp7;
            case RESP8:
                return resp8;
            case RESP9:
                return resp9;
            case STLA:
                return stla;
            case STLO:
                return stlo;
            case STEL:
                return stel;
            case STDP:
                return stdp;
            case EVLA:
                return evla;
            case EVLO:
                return evlo;
            case EVEL:
                return evel;
            case EVDP:
                return evdp;
            case MAG:
                return mag;
            case USER0:
                return user0;
            case USER1:
                return user1;
            case USER2:
                return user2;
            case USER3:
                return user3;
            case USER4:
                return user4;
            case USER5:
                return user5;
            case USER6:
                return user6;
            case USER7:
                return user7;
            case USER8:
                return user8;
            case USER9:
                return user9;
            case DIST:
                return dist;
            case AZ:
                return az;
            case BAZ:
                return baz;
            case GCARC:
                return gcarc;
            case DEPMEN:
                return depmen;
            case CMPAZ:
                return cmpaz;
            case CMPINC:
                return cmpinc;
            case XMINIMUM:
                return xminimum;
            case XMAXIMUM:
                return xmaximum;
            case YMINIMUM:
                return yminimum;
            case YMAXIMUM:
                return ymaximum;
            // case num63:
            // return num63;
            // case num64:
            // return num64;
            // case num65:
            // return num65;
            // case num66:
            // return num66;
            // case num67:
            // return num67;
            // case num68:
            // return num68;
            // case num69:
            // return num69;
            // case num9:
            // return num9;
            // case num54:
            // return num54;
            // case num55:
            // return num55;

            default:
                throw new RuntimeException(sacHeaderEnum + " is unanticipated.");
        }
    }

    @Override
    public int getInt(SACHeaderEnum sacHeaderEnum) {
        if (sacHeaderEnum.typeOf() == 99 || sacHeaderEnum.typeOf() == -1) return getSpecialInt(sacHeaderEnum);
        if (sacHeaderEnum.typeOf() != 1) throw new IllegalArgumentException(sacHeaderEnum + " is not integer");

        switch (sacHeaderEnum) {
            case NZYEAR:
                return nzyear;
            case NZJDAY:
                return nzjday;
            case NZHOUR:
                return nzhour;
            case NZMIN:
                return nzmin;
            case NZSEC:
                return nzsec;
            case NZMSEC:
                return nzmsec;
            case NVHDR:
                return nvhdr;
            case NORID:
                return norid;
            case NEVID:
                return nevid;
            case NPTS:
                return npts;
            case NWFID:
                return nwfid;
            case NXSIZE:
                return nxsize;
            case NYSIZE:
                return nysize;
            // case num80:
            // return num80;
            // case num84:
            // return num84;

            default:
                throw new RuntimeException("Unexpected happens.");
        }
    }

    @Override
    public SACHeader setSACString(SACHeaderEnum sacHeaderEnum, String string) {
        int length = sacHeaderEnum.typeOf();
        if (length != 8 && length != 16) throw new IllegalArgumentException(sacHeaderEnum + " is not String value");
        if (length < string.length()) throw new IllegalArgumentException(string + " is too long for " + sacHeaderEnum);
        SACHeader sh = clone();
        switch (sacHeaderEnum) {
            case KSTNM:
                sh.kstnm = string;
                return sh;
            case KEVNM:
                sh.kevnm = string;
                return sh;
            case KHOLE:
                sh.khole = string;
                return sh;
            case KO:
                sh.ko = string;
                return sh;
            case KA:
                sh.ka = string;
                return sh;
            case KT0:
                sh.kt0 = string;
                return sh;
            case KT1:
                sh.kt1 = string;
                return sh;
            case KT2:
                sh.kt2 = string;
                return sh;
            case KT3:
                sh.kt3 = string;
                return sh;
            case KT4:
                sh.kt4 = string;
                return sh;
            case KT5:
                sh.kt5 = string;
                return sh;
            case KT6:
                sh.kt6 = string;
                return sh;
            case KT7:
                sh.kt7 = string;
                return sh;
            case KT8:
                sh.kt8 = string;
                return sh;
            case KT9:
                sh.kt9 = string;
                return sh;
            case KF:
                sh.kf = string;
                return sh;
            case KUSER0:
                sh.kuser0 = string;
                return sh;
            case KUSER1:
                sh.kuser1 = string;
                return sh;
            case KUSER2:
                sh.kuser2 = string;
                return sh;
            case KCMPNM:
                sh.kcmpnm = string;
                return sh;
            case KNETWK:
                sh.knetwk = string;
                return sh;
            case KDATRD:
                sh.kdatrd = string;
                return sh;
            case KINST:
                sh.kinst = string;
                return sh;
            default:
                throw new RuntimeException("Unanticipated happens on " + sacHeaderEnum);
        }
    }

    @Override
    public SACHeader setValue(SACHeaderEnum sacHeaderEnum, double value) {
        if (sacHeaderEnum.typeOf() != 0) throw new IllegalArgumentException(sacHeaderEnum + " is not float value");
        SACHeader sh = clone();
        switch (sacHeaderEnum) {
            case DELTA:
                sh.delta = value;
                return sh;
            case DEPMIN:
                sh.depmin = value;
                return sh;
            case DEPMAX:
                sh.depmax = value;
                return sh;
            case SCALE:
                sh.scale = value;
                return sh;
            case ODELTA:
                sh.odelta = value;
                return sh;
            case B:
                sh.b = value;
                return sh;
            case E:
                sh.e = value;
                return sh;
            case O:
                sh.o = value;
                return sh;
            case A:
                sh.a = value;
                return sh;
            case T0:
                sh.t0 = value;
                return sh;
            case T1:
                sh.t1 = value;
                return sh;
            case T2:
                sh.t2 = value;
                return sh;
            case T3:
                sh.t3 = value;
                return sh;
            case T4:
                sh.t4 = value;
                return sh;
            case T5:
                sh.t5 = value;
                return sh;
            case T6:
                sh.t6 = value;
                return sh;
            case T7:
                sh.t7 = value;
                return sh;
            case T8:
                sh.t8 = value;
                return sh;
            case T9:
                sh.t9 = value;
                return sh;
            case F:
                sh.f = value;
                return sh;
            case RESP0:
                sh.resp0 = value;
                return sh;
            case RESP1:
                sh.resp1 = value;
                return sh;
            case RESP2:
                sh.resp2 = value;
                return sh;
            case RESP3:
                sh.resp3 = value;
                return sh;
            case RESP4:
                sh.resp4 = value;
                return sh;
            case RESP5:
                sh.resp5 = value;
                return sh;
            case RESP6:
                sh.resp6 = value;
                return sh;
            case RESP7:
                sh.resp7 = value;
                return sh;
            case RESP8:
                sh.resp8 = value;
                return sh;
            case RESP9:
                sh.resp9 = value;
                return sh;
            case STLA:
                sh.stla = value;
                return sh;
            case STLO:
                sh.stlo = value;
                return sh;
            case STEL:
                sh.stel = value;
                return sh;
            case STDP:
                sh.stdp = value;
                return sh;
            case EVLA:
                sh.evla = value;
                return sh;
            case EVLO:
                sh.evlo = value;
                return sh;
            case EVEL:
                sh.evel = value;
                return sh;
            case EVDP:
                sh.evdp = value;
                return sh;
            case MAG:
                sh.mag = value;
                return sh;
            case USER0:
                sh.user0 = value;
                return sh;
            case USER1:
                sh.user1 = value;
                return sh;
            case USER2:
                sh.user2 = value;
                return sh;
            case USER3:
                sh.user3 = value;
                return sh;
            case USER4:
                sh.user4 = value;
                return sh;
            case USER5:
                sh.user5 = value;
                return sh;
            case USER6:
                sh.user6 = value;
                return sh;
            case USER7:
                sh.user7 = value;
                return sh;
            case USER8:
                sh.user8 = value;
                return sh;
            case USER9:
                sh.user9 = value;
                return sh;
            case DIST:
                sh.dist = value;
                return sh;
            case AZ:
                sh.az = value;
                return sh;
            case BAZ:
                sh.baz = value;
                return sh;
            case GCARC:
                sh.gcarc = value;
                return sh;
            case DEPMEN:
                sh.depmen = value;
                return sh;
            case CMPAZ:
                sh.cmpaz = value;
                return sh;
            case CMPINC:
                sh.cmpinc = value;
                return sh;
            case XMINIMUM:
                sh.xminimum = value;
                return sh;
            case XMAXIMUM:
                sh.xmaximum = value;
                return sh;
            case YMINIMUM:
                sh.yminimum = value;
                return sh;
            case YMAXIMUM:
                sh.ymaximum = value;
                return sh;
            // case num63:
            // // return num63;
            // case num64:
            // // return num64;
            // case num65:
            // // return num65;
            // case num66:
            // // return num66;
            // case num67:
            // // return num67;
            // case num68:
            // // return num68;
            // case num69:
            // // return num69;
            // case num9:
            // // return num9;
            // case num54:
            // // return num54;
            // case num55:
            // // return num55;
            default:
                throw new RuntimeException(sacHeaderEnum + " is unused now");
        }

    }

    @Override
    public SACHeader setSACEnumerated(SACHeaderEnum sacHeaderEnum, int value) {
        if (sacHeaderEnum.typeOf() != 2)
            throw new IllegalArgumentException(sacHeaderEnum + " is not an enumerized value");
        SACHeader sh = clone();
        switch (sacHeaderEnum) {
            case IFTYPE:
                sh.iftype = value;
                return sh;
            case IDEP:
                sh.idep = value;
                return sh;
            case IZTYPE:
                sh.iztype = value;
                return sh;
            case IINST:
                sh.iinst = value;
                return sh;
            case ISTREG:
                sh.istreg = value;
                return sh;
            case IEVREG:
                sh.ievreg = value;
                return sh;
            case IEVTYP:
                sh.ievtyp = value;
                return sh;
            case IQUAL:
                sh.iqual = value;
                return sh;
            case ISYNTH:
                sh.isynth = value;
                return sh;
            case IMAGTYP:
                sh.imagtyp = value;
                return sh;
            case IMAGSRC:
                sh.imagsrc = value;
                return sh;
            // case num88:
            // num88 = value;
            // return;
            // case num97:
            // num97 = value;
            // return;
            // case num98:
            // num98 = value;
            // return;
            // case num99:
            // num99 = value;
            // return;
            // case num100:
            // num100 = value;
            // return;
            // case num101:
            // num101 = value;
            // return;
            // case num102:
            // num102 = value;
            // return;
            // case num103:
            // num103 = value;
            // return;
            // case num104:
            // num104 = value;
            // return;
            default:
                throw new RuntimeException(sacHeaderEnum + " is unaticipated...");
        }

    }

    @Override
    public SACHeader setInt(SACHeaderEnum sacHeaderEnum, int value) {
        if (sacHeaderEnum.typeOf() != 1) throw new IllegalArgumentException(sacHeaderEnum + " is not an integer value");
        SACHeader sh = clone();
        switch (sacHeaderEnum) {
            case NZYEAR:
                sh.nzyear = value;
                return sh;
            case NZJDAY:
                sh.nzjday = value;
                return sh;
            case NZHOUR:
                sh.nzhour = value;
                return sh;
            case NZMIN:
                sh.nzmin = value;
                return sh;
            case NZSEC:
                sh.nzsec = value;
                return sh;
            case NZMSEC:
                sh.nzmsec = value;
                return sh;
            case NVHDR:
                sh.nvhdr = value;
                return sh;
            case NORID:
                sh.norid = value;
                return sh;
            case NEVID:
                sh.nevid = value;
                return sh;
            case NPTS:
                sh.npts = value;
                return sh;
            case NWFID:
                sh.nwfid = value;
                return sh;
            case NXSIZE:
                sh.nxsize = value;
                return sh;
            case NYSIZE:
                sh.nysize = value;
                return sh;

            default:
                throw new RuntimeException(sacHeaderEnum + " is unused now");
        }

    }

    @Override
    public String getSACString(SACHeaderEnum sacHeaderEnum) {
        if (sacHeaderEnum.typeOf() != 8 && sacHeaderEnum.typeOf() != 16)
            throw new IllegalArgumentException(sacHeaderEnum + " is not sac string");

        switch (sacHeaderEnum) {
            case KSTNM:
                return kstnm;
            case KEVNM:
                return kevnm;
            case KHOLE:
                return khole;
            case KO:
                return ko;
            case KA:
                return ka;
            case KT0:
                return kt0;
            case KT1:
                return kt1;
            case KT2:
                return kt2;
            case KT3:
                return kt3;
            case KT4:
                return kt4;
            case KT5:
                return kt5;
            case KT6:
                return kt6;
            case KT7:
                return kt7;
            case KT8:
                return kt8;
            case KT9:
                return kt9;
            case KF:
                return kf;
            case KUSER0:
                return kuser0;
            case KUSER1:
                return kuser1;
            case KUSER2:
                return kuser2;
            case KCMPNM:
                return kcmpnm;
            case KNETWK:
                return knetwk;
            case KDATRD:
                return kdatrd;
            case KINST:
                return kinst;
            default:
                throw new RuntimeException("Unanticipated happens");
        }
    }

}
