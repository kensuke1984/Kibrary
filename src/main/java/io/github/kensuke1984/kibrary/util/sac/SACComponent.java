package io.github.kensuke1984.kibrary.util.sac;


/**
 * Components of SAC<br>
 * Z(1), R(2), T(3)
 *
 * @author Kensuke Konishi
 * @version 0.0.3.3
 */
public enum SACComponent {
    Z(1), R(2), T(3);

    private int value;

    SACComponent(int i) {
        value = i;
    }

    /**
     * @return 1(Z), 2(R), 3(T)
     */
    public int valueOf() {
        return value;
    }

    /**
     * @param n index 1, 2, 3
     * @return Z(1) R(2) T(3)
     * @throws IllegalArgumentException if the input n is not 1,2,3
     */
    public static SACComponent getComponent(int n) {
        switch (n) {
            case 1:
                return Z;
            case 2:
                return R;
            case 3:
                return T;
            default:
                throw new IllegalArgumentException("Invalid component! Components are Z(1) R(2) T(3)");
        }
    }

    /**
     * @param sacHeaderData must contain KCMPNM (vertical, radial or trnsvers)
     * @return SACComponent of the input sacHeaderData
     */
    public static SACComponent of(SACHeaderData sacHeaderData) {
        return sacHeaderData.getComponent();
    }

}
