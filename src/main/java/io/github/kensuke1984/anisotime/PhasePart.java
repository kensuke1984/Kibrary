/**
 * 
 */
package io.github.kensuke1984.anisotime;

/**
 * 
 * 
 * 
 * @author kensuke
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
	JH, JV;

	/**
	 * @return P,S: Mantle, K: Outer core, I, J: Inner core
	 */
	Partition whichPartition() {
		switch (this) {
		case I:
		case JH:
		case JV:
			return Partition.INNERCORE;
		case K:
			return Partition.OUTERCORE;
		case P:
		case SH:
		case SV:
			return Partition.MANTLE;
		default:
			throw new RuntimeException("unexPecTed");
		}
	}
}
