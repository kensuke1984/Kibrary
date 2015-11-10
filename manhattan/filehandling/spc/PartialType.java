package filehandling.spc;

import java.util.Arrays;

/**
 * 3D: A, C, F, L, N, LAMBDA, MU, Q<br>
 * 1D: PAR* (PAR1:LAMBDA PAR2:MU)<br>
 * TIME
 * 
 * 
 * @since 2014/11/3
 * @version 0.0.2 modified.
 * 
 * @since 2015/8/23
 * @version 0.0.3 modified
 * 
 * @author Kensuke
 * 
 */
public enum PartialType {

	A(0), C(1), F(2), L(3), N(4), MU(5), LAMBDA(6), Q(7), TIME(8), PAR1(9), PAR2(10), PARA(11), PARC(12), PARF(
			13), PARL(14), PARN(15), PARQ(16);

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
		default:
			throw new RuntimeException("unexpected");

		}

	}

}
