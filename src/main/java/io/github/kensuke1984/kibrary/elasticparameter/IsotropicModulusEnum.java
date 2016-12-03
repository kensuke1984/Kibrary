package io.github.kensuke1984.kibrary.elasticparameter;

/**
 * enum for elements in modulus
 *
 * @author Kensuke Konishi
 * @version 0.0.1.1
 */
public enum IsotropicModulusEnum {
    LAMBDA, MU, LAMBDAplus2MU, ZERO;

    public static IsotropicModulusEnum getIsotropic(ElasticMN mn) {
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
                        return LAMBDAplus2MU;
                    case 2:
                        return LAMBDA;
                    case 3:
                        return LAMBDA;
                    case 4:
                        return ZERO;
                    case 5:
                        return ZERO;
                    case 6:
                        return ZERO;
                }
            case 2:
                switch (n) {
                    case 2:
                        return LAMBDAplus2MU;
                    case 3:
                        return LAMBDA;
                    case 4:
                        return ZERO;
                    case 5:
                        return ZERO;
                    case 6:
                        return ZERO;
                }
            case 3:
                switch (n) {
                    case 3:
                        return LAMBDAplus2MU;
                    case 4:
                        return ZERO;
                    case 5:
                        return ZERO;
                    case 6:
                        return ZERO;
                }
            case 4:
                switch (n) {
                    case 4:
                        return MU;
                    case 5:
                        return ZERO;
                    case 6:
                        return ZERO;
                }
            case 5:
                switch (n) {
                    case 5:
                        return MU;
                    case 6:
                        return ZERO;
                }
            case 6:
                switch (n) {
                    case 6:
                        return MU;
                }
        }
        throw new RuntimeException("Invalid input");
    }

}
