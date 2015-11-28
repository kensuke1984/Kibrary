package filehandling.spc;


/**
 * Tensor components of a spc file
 * <p>
 * r:Z, t(theta):R, p(phi): T
 * <br>
 * rtp &rarr; f_r による　e_tp
 * <p>
 * Forward propagation 1: rr(ZZ) 2: rt(ZR) 3: rp(ZT) 4: tr(RZ) 5: tt(RR) 6: tp(RT) 7: pr(TZ) 8: pt(TR) 9: pp(TT)
 * <p>
 * Backward propagation 1: rrr 2: rrt 3: rrp 4: rtr 5: rtt 6: rtp 7: rpr 8: rpt 9: rpp 10: trr 11:
 * trt 12: trp 13: ttr 14: ttt 15: ttp 16: tpr 17: tpt 18: tpp 19: prr 20: prt
 * 21: prp 22: ptr 23: ptt 24: ptp 25: ppr 26: ppt 27: ppp
 * 
 * 
 * 
 * @author Kensuke
 * 
 */
enum SpcTensorComponent {
	rr(1), rt(2), rp(3), tr(4), tt(5), tp(6), pr(7), pt(8), pp(9),
	rrr(1), rrt(2), rrp(3), rtr(4), rtt(5), rtp(6), rpr(7), rpt(8), rpp(9),
	trr(10), trt(11), trp(12), ttr(13), ttt(14), ttp(15), tpr(16), tpt(17), tpp(18),
	prr(19), prt(20), prp(21), ptr(22), ptt(23), ptp(24), ppr(25), ppt(26), ppp(27);

	private int value;

	private int ij;

	
	public int valueOf(){
		return value;
	}
	
	private SpcTensorComponent(int n) {
		value = n;
		switch (n%9) {
		case 1:
			ij = 11;
			break;
		case 2:
			ij = 12;
			break;
		case 3:
			ij = 13;
			break;
		case 4:
			ij = 21;
			break;
		case 5:
			ij = 22;
			break;
		case 6:
			ij = 23;
			break;
		case 7:
			ij=31;
			break;
		case 8:
			ij =32;
			break;
		case 0:
			ij= 33;
			break;
		}

	}
	
	public int getIJ(){
		return ij;
	}

	
	/**
	 * back propagate のn番目のコンポーネントを返す
	 * @param n n=1,..27
	 * @return SpcTensorComponent for the input n
	 */
	public static SpcTensorComponent valueOfBP(int n) {
		for (SpcTensorComponent stc : values()) 
			if (stc.name().length() == 2)
				continue;
			else if (stc.value == n)
				return stc;
			// System.out.println(stc.name());

		System.out.println("input n: " + n + " is invalid.");
		return null;
	}
	
	/**
	 * back propagate のETAri,sのコンポーネントを返す
	 * i=1 r=2 s=3の時 rtpを返す
	 * @param i 1, 2, 3
	 * @param r 1, 2, 3
	 * @param s 1, 2, 3
	 * @return SpcTensorComponent for the input i r s
	 */
	public static SpcTensorComponent valueOfBP(int i, int r, int s) {
		for (SpcTensorComponent stc : values()) 
			if (stc.name().length() == 2)
				continue;
			else if (stc.value == (i-1)*9+3*(r-1)+s)
				return stc;
			// System.out.println(stc.name());
		

		System.out.println("input i, r, s: " + i+" "+r+" "+s + " are invalid.");
		return null;
	}

	/**
	 * forward propagate のn番目のコンポーネントを返す
	 * @param n n=1,..9
	 * @return SpcTensorComponent for the input n
	 */
	/**
	 * forward propagate のUpqのコンポーネントを返す
	 * p=1, q=2の時 rtを返す
	 * @param p 1, 2, 3
	 * @param q 1, 2, 3
	 * @return SpcTensorComponent for the input p q
	 */
	public static SpcTensorComponent valueOfFP(int p, int q) {
		for (SpcTensorComponent stc : values()) {
			if (stc.name().length() == 3)
				continue;
			if (stc.value == (p-1)*3+q)
				return stc;
			// System.out.println(stc.name());
		}
		System.out.println("input p, q: " + p+" "+q + " are invalid.");
		return null;
	}

	/**
	 * forward propagate のn番目のコンポーネントを返す
	 * @param n n=1,..9
	 * @return SpcTensorComponent for the input n
	 */
	public static SpcTensorComponent valueOfFP(int n) {
		for (SpcTensorComponent stc : values()) {
			if (stc.name().length() == 3)
				continue;
			if (stc.value == n)
				return stc;
			// System.out.println(stc.name());
		}
		System.out.println("input n: " + n + " is invalid.");
		return null;
	}

	



}
