package io.github.kensuke1984.anisotime;

/**
 * P, SV, SH, K, I, JV and JH
 * 
 * 
 * @author Kensuke Konishi
 * @version 0.0.1
 */
public enum PhasePart {
	// P in the mantle
	P,
	// S in the mantle
	SV, SH,
	// P in tht outer-core
	K,
	// P in the inner-core
	I,
	// S in the inner-core
	JV, JH;

	/**
	 * @return P,S: Mantle, K: Outer core, I, J: Inner core
	 */
	Partition whichPartition() {
		switch (this) {
		case P:
		case SV:
		case SH:
			return Partition.MANTLE;
		case I:
		case JH:
		case JV:
			return Partition.INNERCORE;
		case K:
			return Partition.OUTERCORE;
		default:
			throw new RuntimeException("unexPecTed");
		}
	}

	int getFlag() {
		switch (this) {
		case P:
			return 1;
		case SV:
			return 2;
		case SH:
			return 4;
		case K:
			return 8;
		case I:
			return 16;
		case JV:
			return 32;
		case JH:
			return 64;
		default:
			throw new RuntimeException("unexPecTed");
		}
	}

}
