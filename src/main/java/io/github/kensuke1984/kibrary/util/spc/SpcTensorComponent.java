package io.github.kensuke1984.kibrary.util.spc;

import java.util.Arrays;

/**
 * Tensor components of a spc file
 * <p>
 * R:Z, T(theta):R, P(phi): T <br>
 * 
 * <p>
 * ij &rarr; U<sub>ij</sub><br>
 * Forward propagation 1: rr(ZZ) 2: rt(ZR) 3: rp(ZT) 4: tr(RZ) 5: tt(RR) 6:
 * tp(RT) 7: pr(TZ) 8: pt(TR) 9: pp(TT)
 * <p>
 * ijk &rarr; f<sub>k</sub> による e<sub>ij</sub> <br>
 * Backward propagation 1: rrr 2: rrt 3: rrp 4: rtr 5: rtt 6: rtp 7: rpr 8: rpt
 * 9: rpp 10: trr 11: trt 12: trp 13: ttr 14: ttt 15: ttp 16: tpr 17: tpt 18:
 * tpp 19: prr 20: prt 21: prp 22: ptr 23: ptt 24: ptp 25: ppr 26: ppt 27: ppp
 * 
 * @version 0.1.1.1 TODO
 * 
 * @author Kensuke Konishi
 * 
 */
enum SpcTensorComponent {
	RR(1), RT(2), RP(3), TR(4), TT(5), TP(6), PR(7), PT(8), PP(9), //
	RRR(1), RRT(2), RRP(3), RTR(4), RTT(5), rtp(6), rpr(7), RPT(8), RPP(9), //
	TRR(10), TRT(11), TRP(12), TTR(13), TTT(14), TTP(15), TPR(16), TPT(17), TPP(18), //
	PRR(19), PRT(20), PRP(21), PTR(22), PTT(23), PTP(24), PPR(25), PPT(26), PPP(27);

	private int value;

	public int valueOf() {
		return value;
	}

	private SpcTensorComponent(int n) {
		value = n;
	}

	/**
	 * back propagate のETAri,sのコンポーネントを返す it returns rtp when i=1 r=2 s=3
	 * 
	 * @param i
	 *            1, 2, 3
	 * @param r
	 *            1, 2, 3
	 * @param s
	 *            1, 2, 3
	 * @return SpcTensorComponent for the input i r s
	 */
	public static SpcTensorComponent valueOfBP(int i, int r, int s) {
		return Arrays.stream(values())
				.filter(stc -> stc.value == (i - 1) * 9 + 3 * (r - 1) + s && stc.name().length() == 3).findAny()
				.orElseThrow(() -> new IllegalArgumentException(
						"input i, r, s: " + i + " " + r + " " + s + " are invalid."));
	}

	/**
	 * forward propagate のUp,qのコンポーネントを返す p=1, q=2の時 rtを返す
	 * 
	 * @param p
	 *            1, 2, 3
	 * @param q
	 *            1, 2, 3
	 * @return SpcTensorComponent for the input p q
	 */
	public static SpcTensorComponent valueOfFP(int p, int q) {
		return Arrays.stream(values()).filter(stc -> stc.value == (p - 1) * 3 + q && stc.name().length() == 2).findAny()
				.orElseThrow(() -> new IllegalArgumentException("input p, q: " + p + " " + q + " are invalid."));
	}

}
