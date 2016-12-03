package io.github.kensuke1984.kibrary.util.sac;

/**
 * Extension of SAC (SAC: seismic analysis code)
 * <p>
 * The basic extensions are Z, R, T (observed).<br>
 * ?s is synthetic waveform without <i>convolution</i>. <br>
 * ?sc is synthetic waveform after <i>convolution</i>. <br>
 * ?st is temporal partial derivative without <i>convolution</i>. <br>
 * ?sct is temporal partial derivative after <i>convolution</i>. <br>
 *
 * @author Kensuke Konishi
 * @version 0.0.4.1
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 */
public enum SACExtension {
    Z(1), R(2), T(3), Zs(1), Rs(2), Ts(3), Zsc(1), Rsc(2), Tsc(3), Zst(1), Rst(2), Tst(3), Zsct(1), Rsct(2), Tsct(3),;

    /**
     * If a waveform is an observed one, true.
     *
     * @return if it is convoluted.
     */
    public boolean isConvoluted() {
        return this == Z || this == R || this == T || this == Zsc || this == Rsc || this == Tsc || this == Zsct ||
                this == Rsct || this == Tsct;
    }

    private final int value;

    SACExtension(int n) {
        value = n;
    }

    public SACComponent getComponent() {
        return SACComponent.getComponent(value);
    }

    /**
     * 観測波形かは\.[ZRT]$で終わるかどうか Z, R or T.
     *
     * @return if this is observed or not
     */
    public boolean isOBS() {
        return this == Z || this == R || this == T;
    }

    /**
     * 時間の偏微分係数かどうか
     *
     * @return if it is temporal partial.
     */
    public boolean isTemporalPartial() {
        return this == Zsct || this == Rsct || this == Tsct || this == Zst || this == Rst || this == Tst;
    }

    /**
     * @param component {@link SACComponent} component of waveform
     * @return {@link SACExtension} of observed
     */
    public static SACExtension valueOfObserved(SACComponent component) {
        switch (component) {
            case R:
                return R;
            case T:
                return T;
            case Z:
                return Z;
            default:
                throw new RuntimeException("Unexpected happens.");
        }
    }

    /**
     * @param component {@link SACComponent} of extension
     * @return Z:Zs, R:Rs, T:Ts
     */
    public static SACExtension valueOfSynthetic(SACComponent component) {
        switch (component) {
            case Z:
                return Zs;
            case R:
                return Rs;
            case T:
                return Ts;
            default:
                throw new RuntimeException("Unexpected happens.");
        }
    }

    /**
     * @param component {@link SACComponent} of extension
     * @return Z:Zsc, R:Rsc, T:Tsc
     */
    public static SACExtension valueOfConvolutedSynthetic(SACComponent component) {
        switch (component) {
            case Z:
                return Zsc;
            case R:
                return Rsc;
            case T:
                return Tsc;
            default:
                throw new RuntimeException("Unexpected happens.");
        }
    }

    /**
     * @param component {@link SACComponent} of extension
     * @return Z:Zsct, R:Rsct, T:Tsct
     */
    public static SACExtension valueOfConvolutedTemporalPartial(SACComponent component) {
        switch (component) {
            case Z:
                return Zsct;
            case R:
                return Rsct;
            case T:
                return Tsct;
            default:
                throw new RuntimeException("Unexpected happens.");
        }
    }

    /**
     * @param component {@link SACComponent} of extension
     * @return Z:Zst, R:Rst, T:Tst
     */
    public static SACExtension valueOfTemporalPartial(SACComponent component) {
        switch (component) {
            case Z:
                return Zst;
            case R:
                return Rst;
            case T:
                return Tst;
            default:
                throw new RuntimeException("Unexpected happens.");
        }
    }

}
