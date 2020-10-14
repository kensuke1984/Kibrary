package io.github.kensuke1984.kibrary.util.spc;

import java.util.Arrays;

/**
 * Tensor components of a spc file
 * <p>
 * R:Z, T(theta):R, P(phi): T <br>
 * <p>
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
 * @author Kensuke Konishi
 * @version 0.1.1.1 TODO
 */
public enum SPCTensorComponent {
    RR(1), RT(2), RP(3), TR(4), TT(5), TP(6), PR(7), PT(8), PP(9), //
    RRR(1), RRT(2), RRP(3), RTR(4), RTT(5), rtp(6), rpr(7), RPT(8), RPP(9), //
    TRR(10), TRT(11), TRP(12), TTR(13), TTT(14), TTP(15), TPR(16), TPT(17), TPP(18), //
    PRR(19), PRT(20), PRP(21), PTR(22), PTT(23), PTP(24), PPR(25), PPT(26), PPP(27);

    private int value;

    SPCTensorComponent(int n) {
        value = n;
    }
    
    /**
     * back propagate のETAri,sのコンポーネントを返す it returns rtp when i=1 r=2 s=3
     *
     * @param i 1, 2, 3
     * @param r 1, 2, 3
     * @param s 1, 2, 3
     * @return SPCTensorComponent for the input i r s
     */
    public static SPCTensorComponent valueOfBP(int i, int r, int s) {
        if (i < 1 || 3 < i || r < 1 || 3 < r || s < 1 || 3 < s) throw new IllegalArgumentException(
                "Input (i, r, s) = (" + i + ", " + r + ", " + s + ") must be 1, 2 or 3.");
        return Arrays.stream(values())
                .filter(stc -> stc.value == (i - 1) * 9 + 3 * (r - 1) + s && stc.name().length() == 3).findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        "input i, r, s: " + i + " " + r + " " + s + " are invalid."));
    }

    /**
     * forward propagate のUp,qのコンポーネントを返す p=1, q=2の時 rtを返す
     *
     * @param p 1, 2, 3
     * @param q 1, 2, 3
     * @return SPCTensorComponent for the input p q
     */
    public static SPCTensorComponent valueOfFP(int p, int q) {
        if (p < 1 || 3 < p || q < 1 || 3 < q)
            throw new IllegalArgumentException("Input (p, q) = (" + p + ", " + q + ") must be 1, 2 or 3.");
        return Arrays.stream(values()).filter(stc -> stc.value == (p - 1) * 3 + q && stc.name().length() == 2).findAny()
                .orElseThrow(() -> new IllegalArgumentException("input p, q: " + p + " " + q + " are invalid."));
    }

    public int valueOf() {
        return value;
    }
    
	/**
	 * @param n
	 * @return
	 * @author anselme
	 */
	public static boolean isBPSHCATzero(int n) {
		if (n < 1 || n > 27)
			throw new IndexOutOfBoundsException("Error: index of component for BP should be between 1-27");
		if (n >= 10 && n <= 27)
			return false;
		else
			return true;
	}
	
	/**
	 * @param n
	 * @return
	 * @author anselme
	 */
	public static boolean isFPSHzero(int n) {
		if (n < 1 || n > 9)
			throw new IndexOutOfBoundsException("Error: index of component for FP should be between 1-9");
		if (n == 1)
			return true;
		else return false;
	}

}
