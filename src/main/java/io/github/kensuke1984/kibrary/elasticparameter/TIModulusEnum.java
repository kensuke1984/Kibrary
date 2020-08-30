package io.github.kensuke1984.kibrary.elasticparameter;

/**
 * Enum for elements in modulus
 *
 * @author Kensuke Konishi
 * @version 0.0.2
 */
public enum TIModulusEnum {
    A, C, F, L, N, A_2N, ZERO;

    public static TIModulusEnum getTI(ElasticMN mn) {
        int m;
        int n;
        if (mn.getN() < mn.getM()) {
            m = mn.getN();
            n = mn.getM();
        } else {
            m = mn.getM();
            n = mn.getN();
        }

        switch (m) {
            case 1:
                switch (n) {
                    case 1:
                        return A;
                    case 2:
                        return A_2N;
                    case 3:
                        return F;
                    case 4:
                    case 5:
                    case 6:
                        return ZERO;
                }
            case 2:
                switch (n) {
                    case 2:
                        return A;
                    case 3:
                        return F;
                    case 4:
                    case 5:
                    case 6:
                        return ZERO;
                }
            case 3:
                switch (n) {
                    case 3:
                        return C;
                    case 4:
                    case 5:
                    case 6:
                        return ZERO;
                }
            case 4:
                switch (n) {
                    case 4:
                        return L;
                    case 5:
                    case 6:
                        return ZERO;
                }
            case 5:
                switch (n) {
                    case 5:
                        return L;
                    case 6:
                        return ZERO;
                }
            case 6:
                switch (n) {
                    case 6:
                        return N;
                }
        }
        throw new RuntimeException("Invalid input");
    }

}
