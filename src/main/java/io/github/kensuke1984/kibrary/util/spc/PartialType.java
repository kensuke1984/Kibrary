package io.github.kensuke1984.kibrary.util.spc;

import java.util.Arrays;

/**
 * Partial types
 * <p>
 * 3D: A, C, F, L, N, LAMBDA, MU, Q<br>
 * 1D: PAR* (PAR1:LAMBDA PAR2:MU)<br>
 * TIME
 *
 * @author Kensuke Konishi
 * @version 0.0.3.1
 */
public enum PartialType {

    A(0), C(1), F(2), L(3), N(4), MU(5), LAMBDA(6), Q(7), TIME(8), PAR1(9), PAR2(10), PARA(11), PARC(12), PARF(13),
    PARL(14), PARN(15), PARQ(16);

    private int value;

    PartialType(int n) {
        value = n;
    }

    public static PartialType getType(int n) {
        return Arrays.stream(PartialType.values()).filter(type -> type.value == n).findAny()
                .orElseThrow(() -> new IllegalArgumentException("Input n " + n + " is invalid."));
    }

    public boolean is3D() {
        return value < 8;
    }

    public int getValue() {
        return value;
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
    public SPCType toSpcFileType() {
        switch (this) {
            case A:
            case PARA:
                return SPCType.PARA;
            case LAMBDA:
            case PAR1:
                return SPCType.PAR1;
            case C:
            case PARC:
                return SPCType.PARC;
            case MU:
            case PAR2:
                return SPCType.PAR2;
            case F:
                return SPCType.PARF;
            case PARF:
                return SPCType.PAR3;
            case L:
            case PARL:
                return SPCType.PAR4;
            case N:
            case PARN:
                return SPCType.PAR5;
            case PARQ:
            case Q:
                return SPCType.PARQ;
            default:
                throw new RuntimeException("unexpected");

        }

    }

}
