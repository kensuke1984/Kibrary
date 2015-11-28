package filehandling.sac;

import java.io.IOException;

/**
 * 
 * Information in the header parts of a SAC file.
 * <p>This class is <b>IMMUTABLE</b></p>
 * 
 * @version 2.0
 * 
 * @author Kensuke
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 */
class SACHeader implements SACHeaderData, Cloneable {

	@Override
	public SACHeader clone() {
		try {
			SACHeader sh = (SACHeader) super.clone();
			return sh;

		} catch (Exception e) {
			throw new RuntimeException("UNEXPecTEd");
		}
	}

	/**
	 * float &rarr; doubleの変換でゴミが着くので Stringを介して対処
	 * 
	 * @param value
	 *            in float
	 * @return value in double
	 */
	private final static double toDouble(float value) {
		return Double.parseDouble(Float.toString(value));
	}

	/**
	 * 入力したsacファイルのヘッダーを読み込み上書きする
	 * 
	 * @param fileName
	 *            to read
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
			T0 = toDouble(stream.readFloat());
			T1 = toDouble(stream.readFloat());
			T2 = toDouble(stream.readFloat());
			T3 = toDouble(stream.readFloat());
			T4 = toDouble(stream.readFloat());
			T5 = toDouble(stream.readFloat());
			T6 = toDouble(stream.readFloat());
			T7 = toDouble(stream.readFloat());
			T8 = toDouble(stream.readFloat());
			T9 = toDouble(stream.readFloat());
			F = toDouble(stream.readFloat());
			RESP0 = toDouble(stream.readFloat());
			RESP1 = toDouble(stream.readFloat());
			RESP2 = toDouble(stream.readFloat());
			RESP3 = toDouble(stream.readFloat());
			RESP4 = toDouble(stream.readFloat());
			RESP5 = toDouble(stream.readFloat());
			RESP6 = toDouble(stream.readFloat());
			RESP7 = toDouble(stream.readFloat());
			RESP8 = toDouble(stream.readFloat());
			RESP9 = toDouble(stream.readFloat());
			stla = toDouble(stream.readFloat());
			stlo = toDouble(stream.readFloat());
			STEL = toDouble(stream.readFloat());
			STDP = toDouble(stream.readFloat());
			evla = toDouble(stream.readFloat());
			evlo = toDouble(stream.readFloat());
			EVEL = toDouble(stream.readFloat());
			evdp = toDouble(stream.readFloat());
			MAG = toDouble(stream.readFloat());
			USER0 = toDouble(stream.readFloat());
			USER1 = toDouble(stream.readFloat());
			USER2 = toDouble(stream.readFloat());
			USER3 = toDouble(stream.readFloat());
			USER4 = toDouble(stream.readFloat());
			USER5 = toDouble(stream.readFloat());
			USER6 = toDouble(stream.readFloat());
			USER7 = toDouble(stream.readFloat());
			USER8 = toDouble(stream.readFloat());
			USER9 = toDouble(stream.readFloat());
			DIST = toDouble(stream.readFloat());
			AZ = toDouble(stream.readFloat());
			BAZ = toDouble(stream.readFloat());
			GCARC = toDouble(stream.readFloat());
			num54 = toDouble(stream.readFloat());
			num55 = toDouble(stream.readFloat());
			DEPMEN = toDouble(stream.readFloat());
			CMPAZ = toDouble(stream.readFloat());
			CMPINC = toDouble(stream.readFloat());
			XMINIMUM = toDouble(stream.readFloat());
			XMAXIMUM = toDouble(stream.readFloat());
			YMINIMUM = toDouble(stream.readFloat());
			YMAXIMUM = toDouble(stream.readFloat());
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
			NORID = stream.readInt();
			NEVID = stream.readInt();
			npts = stream.readInt();
			num80 = stream.readInt();
			NWFID = stream.readInt();
			nxsize = stream.readInt();
			NYSIZE = stream.readInt();
			num84 = stream.readInt();
			IFTYPE = stream.readInt();
			IDEP = stream.readInt();
			IZTYPE = stream.readInt();
			num88 = stream.readInt();
			IINST = stream.readInt();
			ISTREG = stream.readInt();
			IEVREG = stream.readInt();
			IEVTYP = stream.readInt();
			IQUAL = stream.readInt();
			ISYNTH = stream.readInt();
			IMAGTYP = stream.readInt();
			IMAGSRC = stream.readInt();
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
			KEVNM = stream.readString(16);
			KHOLE = stream.readString(8);
			KO = stream.readString(8);
			KA = stream.readString(8);
			KT0 = stream.readString(8);
			KT1 = stream.readString(8);
			KT2 = stream.readString(8);
			KT3 = stream.readString(8);
			KT4 = stream.readString(8);
			KT5 = stream.readString(8);
			KT6 = stream.readString(8);
			KT7 = stream.readString(8);
			KT8 = stream.readString(8);
			KT9 = stream.readString(8);
			KF = stream.readString(8);
			KUSER0 = stream.readString(8);
			KUSER1 = stream.readString(8);
			KUSER2 = stream.readString(8);
			KCMPNM = stream.readString(8);
			knetwk = stream.readString(8);
			KDATRD = stream.readString(8);
			KINST = stream.readString(8);

		}
		return;
	}

	/**
	 * Header values will be read in SAC named the input sacFileName
	 * 
	 * @param sacFileName
	 *            to be read
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	SACHeader(SACFileName sacFileName) throws IOException {
		read(sacFileName);
	}

	@Override
	public boolean getBoolean(SACHeaderEnum sacHeaderEnum) {
		if (sacHeaderEnum.typeOf() == 99 || sacHeaderEnum.typeOf() == -1)
			return getSpecialBoolean(sacHeaderEnum);
		if (sacHeaderEnum.typeOf() != 3)
			throw new IllegalArgumentException(sacHeaderEnum + " is not boolean");

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
		if (sacHeaderEnum.typeOf() == 99 || sacHeaderEnum.typeOf() == -1)
			return getSpecialSacEnumelated(sacHeaderEnum);
		if (sacHeaderEnum.typeOf() != 2)
			throw new IllegalArgumentException(sacHeaderEnum + " is not enumerated value");

		switch (sacHeaderEnum) {
		case IFTYPE:
			return IFTYPE;
		case IDEP:
			return IDEP;
		case IZTYPE:
			return IZTYPE;
		case IINST:
			return IINST;
		case ISTREG:
			return ISTREG;
		case IEVREG:
			return IEVREG;
		case IEVTYP:
			return IEVTYP;
		case IQUAL:
			return IQUAL;
		case ISYNTH:
			return ISYNTH;
		case IMAGTYP:
			return IMAGTYP;
		case IMAGSRC:
			return IMAGSRC;
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
		if (sacHeaderEnum.typeOf() != 3)
			throw new IllegalArgumentException(sacHeaderEnum + " is not boolean");
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
	 * unused かinternalパラメタ
	 * 
	 * @param sacHeaderEnum
	 *            ヘッダー名
	 * @return
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
	 * @param sacHeaderEnum
	 *            ヘッダー名
	 * @return
	 */
	private int getSpecialSacEnumelated(SACHeaderEnum sacHeaderEnum) {
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
	 * unused かinternalパラメタ
	 * 
	 * @param sacHeaderEnum
	 *            ヘッダー名
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
	 * @param sacHeaderEnum
	 *            ヘッダー名（インターナルかunused実数値）
	 * @return インターナル,unused指定の値を返す
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
		if (sacHeaderEnum.typeOf() == -1 || sacHeaderEnum.typeOf() == 99)
			return getSpecialValue(sacHeaderEnum);
		else if (sacHeaderEnum.typeOf() != 0)
			throw new IllegalArgumentException(sacHeaderEnum + " is not float");

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
			return T0;
		case T1:
			return T1;
		case T2:
			return T2;
		case T3:
			return T3;
		case T4:
			return T4;
		case T5:
			return T5;
		case T6:
			return T6;
		case T7:
			return T7;
		case T8:
			return T8;
		case T9:
			return T9;
		case F:
			return F;
		case RESP0:
			return RESP0;
		case RESP1:
			return RESP1;
		case RESP2:
			return RESP2;
		case RESP3:
			return RESP3;
		case RESP4:
			return RESP4;
		case RESP5:
			return RESP5;
		case RESP6:
			return RESP6;
		case RESP7:
			return RESP7;
		case RESP8:
			return RESP8;
		case RESP9:
			return RESP9;
		case STLA:
			return stla;
		case STLO:
			return stlo;
		case STEL:
			return STEL;
		case STDP:
			return STDP;
		case EVLA:
			return evla;
		case EVLO:
			return evlo;
		case EVEL:
			return EVEL;
		case EVDP:
			return evdp;
		case MAG:
			return MAG;
		case USER0:
			return USER0;
		case USER1:
			return USER1;
		case USER2:
			return USER2;
		case USER3:
			return USER3;
		case USER4:
			return USER4;
		case USER5:
			return USER5;
		case USER6:
			return USER6;
		case USER7:
			return USER7;
		case USER8:
			return USER8;
		case USER9:
			return USER9;
		case DIST:
			return DIST;
		case AZ:
			return AZ;
		case BAZ:
			return BAZ;
		case GCARC:
			return GCARC;
		case DEPMEN:
			return DEPMEN;
		case CMPAZ:
			return CMPAZ;
		case CMPINC:
			return CMPINC;
		case XMINIMUM:
			return XMINIMUM;
		case XMAXIMUM:
			return XMAXIMUM;
		case YMINIMUM:
			return YMINIMUM;
		case YMAXIMUM:
			return YMAXIMUM;
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
		// double value = Float.parseFloat(header.get(she));
		// value = Math.round(value * 10000) / 10000.0;
		// return value;
	}

	@Override
	public int getInt(SACHeaderEnum sacHeaderEnum) {
		if (sacHeaderEnum.typeOf() == 99 || sacHeaderEnum.typeOf() == -1)
			return getSpecialInt(sacHeaderEnum);
		if (sacHeaderEnum.typeOf() != 1)
			throw new IllegalArgumentException(sacHeaderEnum + " is not integer");

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
			return NORID;
		case NEVID:
			return NEVID;
		case NPTS:
			return npts;
		case NWFID:
			return NWFID;
		case NXSIZE:
			return nxsize;
		case NYSIZE:
			return NYSIZE;
		// case num80:
		// return num80;
		// case num84:
		// return num84;

		default:
			throw new RuntimeException("Unexpected happens.");
		}
		// return Integer.parseInt(header.get(she));
	}

	@Override
	public SACHeader setSACString(SACHeaderEnum sacHeaderEnum, String string) {
		int length = sacHeaderEnum.typeOf();
		if (length != 8 && length != 16)
			throw new IllegalArgumentException(sacHeaderEnum + " is not String value");
		if (length < string.length())
			throw new IllegalArgumentException(string + " is too long for " + sacHeaderEnum);
		SACHeader sh = clone();
		switch (sacHeaderEnum) {
		case KSTNM:
			sh.kstnm = string;
			return sh;
		case KEVNM:
			sh.KEVNM = string;
			return sh;
		case KHOLE:
			sh.KHOLE = string;
			return sh;
		case KO:
			sh.KO = string;
			return sh;
		case KA:
			sh.KA = string;
			return sh;
		case KT0:
			sh.KT0 = string;
			return sh;
		case KT1:
			sh.KT1 = string;
			return sh;
		case KT2:
			sh.KT2 = string;
			return sh;
		case KT3:
			sh.KT3 = string;
			return sh;
		case KT4:
			sh.KT4 = string;
			return sh;
		case KT5:
			sh.KT5 = string;
			return sh;
		case KT6:
			sh.KT6 = string;
			return sh;
		case KT7:
			sh.KT7 = string;
			return sh;
		case KT8:
			sh.KT8 = string;
			return sh;
		case KT9:
			sh.KT9 = string;
			return sh;
		case KF:
			sh.KF = string;
			return sh;
		case KUSER0:
			sh.KUSER0 = string;
			return sh;
		case KUSER1:
			sh.KUSER1 = string;
			return sh;
		case KUSER2:
			sh.KUSER2 = string;
			return sh;
		case KCMPNM:
			sh.KCMPNM = string;
			return sh;
		case KNETWK:
			sh.knetwk = string;
			return sh;
		case KDATRD:
			sh.KDATRD = string;
			return sh;
		case KINST:
			sh.KINST = string;
			return sh;
		default:
			throw new RuntimeException("Unanticipated happens on " + sacHeaderEnum);
			// return null;
		}

	}

	@Override
	public SACHeader setValue(SACHeaderEnum sacHeaderEnum, double value) {
		if (sacHeaderEnum.typeOf() != 0)
			throw new IllegalArgumentException(sacHeaderEnum + " is not float value");
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
			sh.T0 = value;
			return sh;
		case T1:
			sh.T1 = value;
			return sh;
		case T2:
			sh.T2 = value;
			return sh;
		case T3:
			sh.T3 = value;
			return sh;
		case T4:
			sh.T4 = value;
			return sh;
		case T5:
			sh.T5 = value;
			return sh;
		case T6:
			sh.T6 = value;
			return sh;
		case T7:
			sh.T7 = value;
			return sh;
		case T8:
			sh.T8 = value;
			return sh;
		case T9:
			sh.T9 = value;
			return sh;
		case F:
			sh.F = value;
			return sh;
		case RESP0:
			sh.RESP0 = value;
			return sh;
		case RESP1:
			sh.RESP1 = value;
			return sh;
		case RESP2:
			sh.RESP2 = value;
			return sh;
		case RESP3:
			sh.RESP3 = value;
			return sh;
		case RESP4:
			sh.RESP4 = value;
			return sh;
		case RESP5:
			sh.RESP5 = value;
			return sh;
		case RESP6:
			sh.RESP6 = value;
			return sh;
		case RESP7:
			sh.RESP7 = value;
			return sh;
		case RESP8:
			sh.RESP8 = value;
			return sh;
		case RESP9:
			sh.RESP9 = value;
			return sh;
		case STLA:
			sh.stla = value;
			return sh;
		case STLO:
			sh.stlo = value;
			return sh;
		case STEL:
			sh.STEL = value;
			return sh;
		case STDP:
			sh.STDP = value;
			return sh;
		case EVLA:
			sh.evla = value;
			return sh;
		case EVLO:
			sh.evlo = value;
			return sh;
		case EVEL:
			sh.EVEL = value;
			return sh;
		case EVDP:
			sh.evdp = value;
			return sh;
		case MAG:
			sh.MAG = value;
			return sh;
		case USER0:
			sh.USER0 = value;
			return sh;
		case USER1:
			sh.USER1 = value;
			return sh;
		case USER2:
			sh.USER2 = value;
			return sh;
		case USER3:
			sh.USER3 = value;
			return sh;
		case USER4:
			sh.USER4 = value;
			return sh;
		case USER5:
			sh.USER5 = value;
			return sh;
		case USER6:
			sh.USER6 = value;
			return sh;
		case USER7:
			sh.USER7 = value;
			return sh;
		case USER8:
			sh.USER8 = value;
			return sh;
		case USER9:
			sh.USER9 = value;
			return sh;
		case DIST:
			sh.DIST = value;
			return sh;
		case AZ:
			sh.AZ = value;
			return sh;
		case BAZ:
			sh.BAZ = value;
			return sh;
		case GCARC:
			sh.GCARC = value;
			return sh;
		case DEPMEN:
			sh.DEPMEN = value;
			return sh;
		case CMPAZ:
			sh.CMPAZ = value;
			return sh;
		case CMPINC:
			sh.CMPINC = value;
			return sh;
		case XMINIMUM:
			sh.XMINIMUM = value;
			return sh;
		case XMAXIMUM:
			sh.XMAXIMUM = value;
			return sh;
		case YMINIMUM:
			sh.YMINIMUM = value;
			return sh;
		case YMAXIMUM:
			sh.YMAXIMUM = value;
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
			sh.IFTYPE = value;
			return sh;
		case IDEP:
			sh.IDEP = value;
			return sh;
		case IZTYPE:
			sh.IZTYPE = value;
			return sh;
		case IINST:
			sh.IINST = value;
			return sh;
		case ISTREG:
			sh.ISTREG = value;
			return sh;
		case IEVREG:
			sh.IEVREG = value;
			return sh;
		case IEVTYP:
			sh.IEVTYP = value;
			return sh;
		case IQUAL:
			sh.IQUAL = value;
			return sh;
		case ISYNTH:
			sh.ISYNTH = value;
			return sh;
		case IMAGTYP:
			sh.IMAGTYP = value;
			return sh;
		case IMAGSRC:
			sh.IMAGSRC = value;
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
		if (sacHeaderEnum.typeOf() != 1)
			throw new IllegalArgumentException(sacHeaderEnum + " is not an integer value");
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
			sh.NORID = value;
			return sh;
		case NEVID:
			sh.NEVID = value;
			return sh;
		case NPTS:
			sh.npts = value;
			return sh;
		case NWFID:
			sh.NWFID = value;
			return sh;
		case NXSIZE:
			sh.nxsize = value;
			return sh;
		case NYSIZE:
			sh.NYSIZE = value;
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
			return KEVNM;
		case KHOLE:
			return KHOLE;
		case KO:
			return KO;
		case KA:
			return KA;
		case KT0:
			return KT0;
		case KT1:
			return KT1;
		case KT2:
			return KT2;
		case KT3:
			return KT3;
		case KT4:
			return KT4;
		case KT5:
			return KT5;
		case KT6:
			return KT6;
		case KT7:
			return KT7;
		case KT8:
			return KT8;
		case KT9:
			return KT9;
		case KF:
			return KF;
		case KUSER0:
			return KUSER0;
		case KUSER1:
			return KUSER1;
		case KUSER2:
			return KUSER2;
		case KCMPNM:
			return KCMPNM;
		case KNETWK:
			return knetwk;
		case KDATRD:
			return KDATRD;
		case KINST:
			return KINST;
		default:
			throw new RuntimeException("Unanticipated happens");
		}
	}

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
	// T マーカー
	private double T0 = -12345;
	private double T1 = -12345;
	private double T2 = -12345;
	private double T3 = -12345;
	private double T4 = -12345;
	private double T5 = -12345;
	private double T6 = -12345;
	private double T7 = -12345;
	private double T8 = -12345;
	private double T9 = -12345;
	//
	private double F = -12345;
	private double RESP0 = -12345;
	private double RESP1 = -12345;
	private double RESP2 = -12345;
	private double RESP3 = -12345;
	private double RESP4 = -12345;
	private double RESP5 = -12345;
	private double RESP6 = -12345;
	private double RESP7 = -12345;
	private double RESP8 = -12345;
	private double RESP9 = -12345;
	private double stla = -12345;
	private double stlo = -12345;
	private double STEL = -12345;
	private double STDP = -12345;
	private double evla = -12345;
	private double evlo = -12345;
	private double EVEL = -12345;
	private double evdp = -12345;
	private double MAG = -12345;
	private double USER0 = -12345;
	private double USER1 = -12345;
	private double USER2 = -12345;
	private double USER3 = -12345;
	private double USER4 = -12345;
	private double USER5 = -12345;
	private double USER6 = -12345;
	private double USER7 = -12345;
	private double USER8 = -12345;
	private double USER9 = -12345;
	private double DIST = -12345;
	private double AZ = -12345;
	private double BAZ = -12345;
	private double GCARC = -12345;
	private double num54 = -12345;
	private double num55 = -12345;
	private double DEPMEN = -12345;
	private double CMPAZ = -12345;
	private double CMPINC = -12345;
	private double XMINIMUM = -12345;
	private double XMAXIMUM = -12345;
	private double YMINIMUM = -12345;
	private double YMAXIMUM = -12345;
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
	private int NORID = -12345;
	private int NEVID = -12345;
	private int npts = -12345;
	private int num80 = -12345;
	private int NWFID = -12345;
	private int nxsize = -12345;
	private int NYSIZE = -12345;
	private int num84 = -12345;
	private int IFTYPE = 1; // timeseries
	private int IDEP = 5; // Unknown
	private int IZTYPE = -12345;
	private int num88 = -12345;
	private int IINST = -12345;
	private int ISTREG = -12345;
	private int IEVREG = -12345;
	private int IEVTYP = -12345;
	private int IQUAL = -12345;
	private int ISYNTH = -12345;
	private int IMAGTYP = -12345;
	private int IMAGSRC = -12345;
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
	private String KEVNM = "-12345";
	private String KHOLE = "-12345";
	private String KO = "-12345";
	private String KA = "-12345";
	private String KT0 = "-12345";
	private String KT1 = "-12345";
	private String KT2 = "-12345";
	private String KT3 = "-12345";
	private String KT4 = "-12345";
	private String KT5 = "-12345";
	private String KT6 = "-12345";
	private String KT7 = "-12345";
	private String KT8 = "-12345";
	private String KT9 = "-12345";
	private String KF = "-12345";
	private String KUSER0 = "-12345";
	private String KUSER1 = "-12345";
	private String KUSER2 = "-12345";
	private String KCMPNM = "-12345";
	/**
	 * a name of network
	 */
	private String knetwk = "-12345";
	private String KDATRD = "-12345";
	private String KINST = "-12345";

}
