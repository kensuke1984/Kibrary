package io.github.kensuke1984.kibrary.util.spc;

import java.util.Arrays;

/**
 * Partial types
 * 
 * 3D: A, C, F, L, N, LAMBDA, MU, Q<br>
 * 1D: PAR* (PAR1:LAMBDA PAR2:MU)<br>
 * TIME
 * 
 * 
 * @version 0.0.3
 * 
 * @author Kensuke Konishi
 * 
 */
public enum PartialType {

	A(0), C(1), F(2), L(3), N(4), MU(5), LAMBDA(6), Q(7), TIME_SOURCE(8), TIME_RECEIVER(9), PAR1(10), PAR2(11), PARA(12), PARC(13), PARF(
			14), PARL(15), PARN(16), PARQ(17), G1(18), G2(19), G3(20), G4(21), G5(22), G6(23);

	public boolean is3D() {
		return 8 < value;
	}
	
	public boolean isTimePartial() {
		return value == 8 || value == 9;
	}

	private int value;

	private PartialType(int n) {
		value = n;
	}

	public int getValue() {
		return value;
	}

	public static PartialType getType(int n) {
		return Arrays.stream(PartialType.values()).filter(type -> type.value == n).findAny()
				.orElseThrow(() -> new IllegalArgumentException("Input n " + n + " is invalid."));
	}

	/**
	 * 変微分係数波形を計算するときのCijklの重み A C F L N Mu lambda
	 * 
	 * @return weighting for {@link PartialType} to compute partials
	 */
	public WeightingFactor getWeightingFactor() {
		switch (this) {
		case A:
			return WeightingFactor.A;
		case C:
			return WeightingFactor.C;
		case F:
			return WeightingFactor.F;
		case L:
			return WeightingFactor.L;
		case N:
			return WeightingFactor.N;
		case MU:
			return WeightingFactor.MU;
		case LAMBDA:
			return WeightingFactor.LAMBDA;
		default:
			throw new RuntimeException("Unexpected happens");
		}
	}

	// TODO hmm...
	public SpcFileType toSpcFileType() {
		switch (this) {
		case A:
		case PARA:
			return SpcFileType.PARA;
		case LAMBDA:
		case PAR1:
			return SpcFileType.PAR1;
		case C:
		case PARC:
			return SpcFileType.PARC;
		case MU:
		case PAR2:
			return SpcFileType.PAR2;
		case F:
			return SpcFileType.PARF;
		case PARF:
			return SpcFileType.PAR3;
		case L:
		case PARL:
			return SpcFileType.PAR4;
		case N:
		case PARN:
			return SpcFileType.PAR5;
		case PARQ:
		case Q:
			return SpcFileType.PARQ;
		case G1:
			return SpcFileType.G1;
		case G2:
			return SpcFileType.G2;
		case G3:
			return SpcFileType.G3;
		case G4:
			return SpcFileType.G4;
		case G5:
			return SpcFileType.G5;
		case G6:
			return SpcFileType.G6;
		default:
			throw new RuntimeException("unexpected");
		}

	}

}
