package io.github.kensuke1984.kibrary.datacorrection;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;

/**
 * Moment Tensor<br>
 * Global CMT 1:M<sub>rr</sub> 2:M<sub>tt</sub> 3:M<sub>pp</sub> 4:M
 * <sub>rt</sub> 5:M<sub>rp</sub> 6 M<sub>tp</sub> <br>
 * DSM 1:M<sub>rr</sub> 2:M<sub>rt</sub> 3:M<sub>rp</sub> 4:M<sub>tt</sub> 5:M
 * <sub>tp</sub> 6:M<sub>pp</sub>
 * <p>
 * This class is <b>IMMUTABLE</b>.
 * 
 * 
 * @version 0.0.6.2
 * @author Kensuke Konishi
 * 
 */
public class MomentTensor {

	private final double mrr;
	private final double mtt;
	private final double mpp;
	private final double mrt;
	private final double mrp;
	private final double mtp;
	private final int mtEXP;
	private final double mw;

	/**
	 * The order is same as Global CMT project.
	 * 
	 * @param mrr
	 *            Mrr
	 * @param mtt
	 *            Mtt
	 * @param mpp
	 *            Mpp
	 * @param mrt
	 *            Mrt
	 * @param mrp
	 *            Mrp
	 * @param mtp
	 *            Mtp
	 * @param mtEXP
	 *            exponential number
	 * @param mw
	 *            Mw
	 */
	public MomentTensor(double mrr, double mtt, double mpp, double mrt, double mrp, double mtp, int mtEXP, double mw) {
		this.mrr = mrr;
		this.mtt = mtt;
		this.mpp = mpp;
		this.mrt = mrt;
		this.mrp = mrp;
		this.mtp = mtp;
		this.mtEXP = mtEXP;
		this.mw = mw;
	}

	public double getMrr() {
		return mrr;
	}

	public double getMtt() {
		return mtt;
	}

	public double getMpp() {
		return mpp;
	}

	public double getMrt() {
		return mrt;
	}

	public double getMrp() {
		return mrp;
	}

	public double getMtp() {
		return mtp;
	}
	
	public double getComponent(int i) {
		double mi = 0.;
		switch (i) {
		case 0:
			mi = mrr;
			break;
		case 1:
			mi = mtt;
			break;
		case 2:
			mi = mpp;
			break;
		case 3:
			mi = mrt;
			break;
		case 4:
			mi = mrp;
			break;
		case 5:
			mi = mtp;
			break;
		}
		return mi;
	}

	/**
	 * @return the value used for exponential in global CMT expression
	 */
	public int getMtExp() {
		return mtEXP;
	}

	/**
	 * @return Moment magnitude
	 */
	public double getMw() {
		return mw;
	}

	/**
	 * DSM情報ファイルに書く形式のモーメントテンソルを返す
	 * 
	 * @return moment tensor in the order used in DSM
	 */
	public double[] getDSMmt() {
		double[] dsmMT = new double[6];
		double factor = FastMath.pow(10, mtEXP - 25);
		dsmMT[0] = Precision.round(mrr * factor, 5);
		dsmMT[1] = Precision.round(mrt * factor, 5);
		dsmMT[2] = Precision.round(mrp * factor, 5);
		dsmMT[3] = Precision.round(mtt * factor, 5);
		dsmMT[4] = Precision.round(mtp * factor, 5);
		dsmMT[5] = Precision.round(mpp * factor, 5);
		return dsmMT;
	}

	/**
	 * @param scalarMoment
	 *            M<sub>0</sub> (N*m)
	 * @return Mw for the scalar Moment
	 */
	public static final double toMw(double scalarMoment) {
		// double mw =0;
		double m0 = scalarMoment;
		// 10 ^5 dyne = N, 100 cm = 1m
		double mw = (FastMath.log10(m0) - 9.1) / 1.5;
		return Precision.round(mw, 1);
	}

	@Override
	public String toString() {
		return "Moment Tensor (in Global CMT project order): Expo=" + mtEXP + " " + mrr + " " + mtt + " " + mpp + " "
				+ mrt + " " + mrp + " " + mtp;
	}
	
}
