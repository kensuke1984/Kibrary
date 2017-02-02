package io.github.kensuke1984.kibrary.elasticparameter;

/**
 * ID for elastic parameter C<sub>mn</sub>
 *
 * @author Kensuke Konishi
 * @version 0.0.2.2
 */
public enum ElasticMN {
    C11(11), C12(12), C13(13), C14(14), C15(15), C16(16), C21(21), C22(22), C23(23), C24(24), C25(25), C26(26), C31(31),
    C32(32), C33(33), C34(34), C35(35), C36(36), C41(41), C42(42), C43(43), C44(44), C45(45), C46(46), C51(51), C52(52),
    C53(53), C54(54), C55(55), C56(56), C61(61), C62(62), C63(63), C64(64), C65(65), C66(66);

    private final int value;

    ElasticMN(int n) {
        value = n;
    }

    private static int toInt(int i) {
        int m = i / 10;
        int n = i % 10;
        if (n < m) {
            int k = m;
            m = n;
            n = k;
        }
        int mn = 10 * m + n;

        switch (mn) {
            case 11:
                return 1;
            case 22:
                return 2;
            case 33:
                return 3;
            case 23:
                return 4;
            case 13:
                return 5;
            case 12:
                return 6;
            default:
                throw new RuntimeException("unanticipated");
        }
    }

    /**
     * return Cmn for the input Cijkl
     *
     * @param ijkl target
     * @return ElasticMN for input Cijkl
     */
    public static ElasticMN getElasticMN(ElasticIJKL ijkl) {
        int value = ijkl.getValue();
        int m = value / 100;
        int n = value % 100;
        m = toInt(m);
        n = toInt(n);
        return valueOf(m, n);
    }

    /**
     * @param m index m
     * @param n index n
     * @return Cmn
     */
    public static ElasticMN valueOf(int m, int n) {
        int k = 10 * m + n;
        for (ElasticMN mn : values())
            if (mn.getValue() == k) return mn;
        throw new IllegalArgumentException("input (m,n) : (" + m + ", " + n + ") is invalid");
    }

    public int getM() {
        return value / 10;
    }

    public int getN() {
        return value % 10;
    }

    public int getValue() {
        return value;
    }

}
