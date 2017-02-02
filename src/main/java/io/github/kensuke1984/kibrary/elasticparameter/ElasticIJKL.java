package io.github.kensuke1984.kibrary.elasticparameter;

/**
 * ID for elastic parameter C<sub>ijkl</sub>
 *
 * @author Kensuke Konishi
 * @version 0.0.1.1
 */
public enum ElasticIJKL {
    C1111(1111), C1122(1122), C1133(1133), C1123(1123), C1113(1113), C1112(1112), C2211(2211), C2222(2222), C2233(2233),
    C2223(2223), C2213(2213), C2212(2212), C3311(3311), C3322(3322), C3333(3333), C3323(3323), C3313(3313), C3312(3312),
    C2311(2311), C2322(2322), C2333(2333), C2323(2323), C2313(2313), C2312(2312), C1311(1311), C1322(1322), C1333(1333),
    C1323(1323), C1313(1313), C1312(1312), C1211(1211), C1222(1222), C1233(1233), C1223(1223), C1213(1213), C1212(1212);
    //

    private final int value;

    ElasticIJKL(int n) {
        value = n;
    }

    private static boolean checkInt(int n) {
        return 1 <= n && n <= 3;
    }

    public static ElasticIJKL valueOf(int i, int j, int k, int l) {
        if (!checkInt(i) || !checkInt(j) || !checkInt(k) || !checkInt(l)) throw new IllegalArgumentException(
                "input (i, j, k, l) : (" + i + ", " + j + ", " + k + ", " + l + ") is invalid");
        int n = 1000 * i + 100 * j + 10 * k + l;
        for (ElasticIJKL ijkl : values())
            if (ijkl.getValue() == n) return ijkl;

        n = 1000 * i + 100 * j + 10 * l + k;
        for (ElasticIJKL ijkl : values())
            if (ijkl.getValue() == n) return ijkl;

        n = 1000 * j + 100 * i + 10 * k + l;
        for (ElasticIJKL ijkl : values())
            if (ijkl.getValue() == n) return ijkl;
        n = 1000 * j + 100 * i + 10 * l + k;
        for (ElasticIJKL ijkl : values())
            if (ijkl.getValue() == n) return ijkl;
        throw new RuntimeException("Ananticipated.");
    }

    public int getValue() {
        return value;
    }

}
