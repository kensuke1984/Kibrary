package io.github.kensuke1984.kibrary.dsminformation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.ArithmeticUtils;

/**
 * Header part of a file for DSM.
 * 
 * @version 0.0.4.2
 * 
 * @author Kensuke Konishi
 * 
 */
class DSMheader {

	/**
	 * The default value is 3276.8 .
	 */
	private double tlen; // as default

	/**
	 * The default value is 256 .
	 */
	private int np;

	/**
	 * The default value is 0 .
	 */
	private int imin;

	/**
	 * The default value is 256 .
	 */
	private int imax;

	/**
	 * The default value is 1.e-2; re relative error (See GT95 eq. 6.2)
	 */
	private double relativeError;

	/**
	 * The default value is 1.e-10 ratc ampratio using in grid cut-off (1.d-10
	 * is recommended)
	 */
	private double ratc;
	/**
	 * The default value is 1.e-5 ratl ampratio using in l-cutoff
	 */
	private double ratl;
	/**
	 * The default value is 1.e-2 wrap-around attenuation for omegai
	 */
	private double artificialDampl;

	public double getTlen() {
		return tlen;
	}

	/**
	 * @param tlen
	 *            to be checked
	 * @return if tlen is 2<sup>n</sup>/10
	 */
	static boolean validTlen(double tlen) {
		long tlen10 = Math.round(10 * tlen);
		return ArithmeticUtils.isPowerOfTwo(tlen10) && tlen10 / 10.0 == tlen;
	}

	/**
	 * @param np
	 *            to be checked
	 * @return if np is 2<sup>n</sup>
	 */
	protected static boolean validNp(int np) {
		return 0 < np && (np & (np - 1)) == 0;
	}

	public int getNp() {
		return np;
	}

	public int getImin() {
		return imin;
	}

	public int getImax() {
		return imax;
	}

	public double getRe() {
		return relativeError;
	}

	public double getRatc() {
		return ratc;
	}

	public double getRatl() {
		return ratl;
	}

	public double getArtificialDampl() {
		return artificialDampl;
	}

	protected String[] outputDSMHeader() {
		// String output[] = new String[10];
		List<String> outputLines = new ArrayList<>();
		outputLines.add("c tlen npts");
		outputLines.add(String.valueOf(tlen) + " " + String.valueOf(np));
		outputLines.add("c relative error (see GT95 eq. 6.2)");
		outputLines.add(String.valueOf(relativeError) + " re");
		outputLines.add("c ampratio using in grid cut-off" + " (1.d-10 is recommended)");
		outputLines.add(String.valueOf(ratc) + " ratc");
		outputLines.add("c ampratio using in l-cutoff" + " (see KTG04 fig.8)");
		outputLines.add(String.valueOf(ratl) + " ratl");
		outputLines.add("c artificial damping " + "for wrap-around (see GO94 5.1)");
		outputLines.add(String.valueOf(artificialDampl));
		outputLines.add(String.valueOf(imin) + " " + String.valueOf(imax));

		return outputLines.toArray(new String[0]);
	}

	DSMheader() {
		setDefault();
	}

	/**
	 * DSMヘッダー re = 1.e-2; ratc = 1.e-10; ratl = 1.e-5; artificialDampl = 1.e-2
	 * imin = 0, imax = np
	 * 
	 * @param tlen
	 *            2<sup>n</sup>/10でないといけない nは整数
	 * @param np
	 *            2<sup>n</sup>でないといけない nは整数
	 */
	DSMheader(double tlen, int np) {
		if (!validNp(np) || !validTlen(tlen))
			throw new IllegalArgumentException("Input tlen:" + tlen + " or np:" + np + " is invalid");
		setDefault();
		this.tlen = tlen;
		this.np = np;
		this.imax = np;
	}

	/**
	 * @param tlen
	 *            must be 2**n /10
	 */
	void setTlen(double tlen) {
		if (validTlen(tlen))
			this.tlen = tlen;
		else
			throw new IllegalArgumentException("Tlen must be a power of 2 divided by 10");
	}

	/**
	 * @param np
	 *            must be a power of 2
	 */
	void setNp(int np) {
		if (!validNp(np))
			throw new IllegalArgumentException("Np must be a power of 2.");
		this.np = np;
		this.imax = np;
	}

	void setRe(double re) {
		relativeError = re;
	}

	void setRatc(double ratc) {
		this.ratc = ratc;
	}

	void setRatl(double ratl) {
		this.ratl = ratl;
	}

	void setArtificialDampl(double artificialDampl) {
		this.artificialDampl = artificialDampl;
	}

	private void setDefault() {
		tlen = 3276.8; // as default
		np = 1024; // as default
		imin = 0; // as default
		imax = np; // as default
		relativeError = 1.e-2;
		ratc = 1.e-10;
		ratl = 1.e-5;
		artificialDampl = 1.e-2;
	}

}
