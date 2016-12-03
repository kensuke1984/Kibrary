package io.github.kensuke1984.kibrary.dsminformation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.ArithmeticUtils;

/**
 * Header part of a file for DSM.
 * 
 * This class is <b>IMMUTABLE</b>
 * 
 * @version 0.0.5.1
 * @author Kensuke Konishi
 * 
 */
class DSMheader {

	/**
	 * Time length [s] (default:3276.8)
	 */
	private final double TLEN;

	/**
	 * Number of step in frequency domain (default 256) must be a power of 2.
	 */
	private final int NP;

	/**
	 * The default value is 0 .
	 */
	private final int IMIN;

	/**
	 * The default value is 256 .
	 */
	private final int IMAX;

	/**
	 * The default value is 1.e-2; re relative error (See GT95 eq. 6.2)
	 */
	private final double RELATIVE_ERROR;

	/**
	 * The default value is 1.e-10 ratc ampratio using in grid cut-off (1.d-10
	 * is recommended)
	 */
	private final double RATC;
	
	/**
	 * The default value is 1.e-5 ratl ampratio using in l-cutoff
	 */
	private final double RATL;
	
	/**
	 * The default value is 1.e-2 wrap-around attenuation for omegai
	 */
	private final double ARTIFICIAL_DAMPING;

	public double getTlen() {
		return TLEN;
	}

	/**
	 * @param tlen
	 *            to be checked
	 * @return if tlen is 2<sup>n</sup>/10
	 */
	private static boolean validTlen(double tlen) {
		long tlen10 = Math.round(10 * tlen);
		return ArithmeticUtils.isPowerOfTwo(tlen10) && tlen10 / 10.0 == tlen;
	}

	public int getNp() {
		return NP;
	}

	public int getImin() {
		return IMIN;
	}

	public int getImax() {
		return IMAX;
	}

	/**
	 * @return relative error
	 */
	public double getRe() {
		return RELATIVE_ERROR;
	}

	/**
	 * @return ratc
	 */
	public double getRatc() {
		return RATC;
	}

	public double getRatl() {
		return RATL;
	}

	public double getArtificialDamp() {
		return ARTIFICIAL_DAMPING;
	}

	protected String[] outputDSMHeader() {
		List<String> outputLines = new ArrayList<>();
		outputLines.add("c tlen npts");
		outputLines.add(TLEN + " " + NP);
		outputLines.add("c relative error (see GT95 eq. 6.2)");
		outputLines.add(RELATIVE_ERROR + " re");
		outputLines.add("c ampratio using in grid cut-off" + " (1.d-10 is recommended)");
		outputLines.add(RATC + " ratc");
		outputLines.add("c ampratio using in l-cutoff" + " (see KTG04 fig.8)");
		outputLines.add(RATL + " ratl");
		outputLines.add("c artificial damping " + "for wrap-around (see GO94 5.1)");
		outputLines.add(Double.toString(ARTIFICIAL_DAMPING));
		outputLines.add(IMIN + " " + IMAX);

		return outputLines.toArray(new String[11]);
	}

	DSMheader() {
		this(3276.8, 1024);
	}

	/**
	 * DSMヘッダー <br>
	 * re = 1.e-2; ratc = 1.e-10; ratl = 1.e-5; artificialDampl = 1.e-2 imin =
	 * 0, imax = np
	 * 
	 * @param tlen
	 *            2<sup>n</sup>/10でないといけない nは整数
	 * @param np
	 *            2<sup>n</sup>でないといけない nは整数
	 */
	DSMheader(double tlen, int np) {
		this(tlen, np, 0, np, 1.e-2, 1.e-10, 1.e-5, 1.e-2);
	}

	DSMheader(double tlen, int np, int imin, int imax, double relativeError, double ratc, double ratl,
			double artificialDampl) {
		if (!ArithmeticUtils.isPowerOfTwo(np) || !validTlen(tlen))
			throw new IllegalArgumentException("Input tlen:" + tlen + " or np:" + np + " is invalid");
		TLEN = tlen;
		NP = np;
		IMIN = imin;
		IMAX = imax;
		RELATIVE_ERROR = relativeError;
		RATC = ratc;
		RATL = ratl;
		ARTIFICIAL_DAMPING = artificialDampl;
	}

}
