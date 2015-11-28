package manhattan.datacorrection;

import org.apache.commons.math3.util.FastMath;

/**
 * Moment Tensor<br>
 * Global CMT 1:M<sub>rr</sub> 2:M<sub>tt</sub> 3:M<sub>pp</sub> <br>
 * 4:M<sub>rt</sub> 5:M<sub>rp</sub> 6 M<sub>tp</sub> <br>
 * DSM 1:M<sub>rr</sub> 2:M<sub>rt</sub> 3:M<sub>rp</sub> 4:M<sub>tt</sub> 5:M
 * <sub>tp</sub> 6:M<sub>pp</sub> <br>
 * <p>
 * This class is <b>IMMUTABLE</b>.
 * 
 * 
 * @version 0.0.6
 * @author Kensuke
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

	public MomentTensor(double mrr, double mtt, double mpp, double mrt, double mrp, double mtp, int mtEXP, double mw) {
		super();
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

		dsmMT[0] = FastMath.round(mrr * factor * 100000) / 100000.0;
		dsmMT[1] = FastMath.round(mrt * factor * 100000) / 100000.0;
		dsmMT[2] = FastMath.round(mrp * factor * 100000) / 100000.0;
		dsmMT[3] = FastMath.round(mtt * factor * 100000) / 100000.0;
		dsmMT[4] = FastMath.round(mtp * factor * 100000) / 100000.0;
		dsmMT[5] = FastMath.round(mpp * factor * 100000) / 100000.0;
		return dsmMT;
	}

	/**
	 * @param scalarMoment
	 *            (N*m)
	 * @return Mw for the scalar Moment
	 */
	public static final double toMw(double scalarMoment) {
		// double mw =0;
		double m0 = scalarMoment;
		// 10 ^5 dyne = N, 100 cm = 1m
		double mw = FastMath.round((FastMath.log10(m0) - 9.1) / 1.5 * 10) / 10.0;

		return mw;
	}

}
