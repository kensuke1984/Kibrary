package filehandling.sac;

/**
 * Extension of SAC
 * (SAC: seismic analysis code)
 * 
 * The basic extensions are Z, R, T (observed).<br>
 * ?s is synthetic waveform not convoluted. <br>
 * ?sc is convoluted synthetic waveform. <br>
 * ?st is a temporal partial derivative. <br>
 * ?sct is a convoluted temporal partial derivative. <br>
 * 
 * 
 * 
 * @since 2013/9/16
 * @version 0.0.2
 * 
 * @version 0.0.3
 * @since 2015/1/30 slightly modified.
 * 
 * @version 0.0.4
 * @since 2015/8/23 X is removed
 * 
 * @author Kensuke
 * 
 */
public enum SACExtension {
	Z(1), R(2), T(3), Zs(1), Rs(2), Ts(3), Zsc(1), Rsc(2), Tsc(3), Zst(1), Rst(2), Tst(3), Zsct(1), Rsct(2), Tsct(3),;

	/**
	 * If a waveform is an observed one, true.
	 * 
	 * @return if it is convoluted.
	 */
	public boolean isConvoluted() {
		return this == Z || this == R || this == T || this == Zsc || this == Rsc || this == Tsc || this == Zsct
				|| this == Rsct || this == Tsct;
	}

	private final int value;

	private SACExtension(int n) {
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
	 * @param component
	 *            {@link SACComponent} component of waveform
	 * @return {@link SACExtension} of observed
	 */
	public static SACExtension valueOfObserved(SACComponent component) {
		switch (component) {
		case R:
			return SACExtension.R;
		case T:
			return SACExtension.T;
		case Z:
			return SACExtension.Z;
		default:
			throw new RuntimeException("Unexpected happens.");
		}
	}

	/**
	 * @param component {@link SACComponent}
	 *            of extension
	 * @return Z:Zs, R:Rs, T:Ts
	 */
	public static SACExtension valueOfSynthetic(SACComponent component) {
		switch (component) {
		case Z:
			return SACExtension.Zs;
		case R:
			return SACExtension.Rs;
		case T:
			return SACExtension.Ts;
		default:
			throw new RuntimeException("Unexpected happens.");
		}
	}

	/**
	 * @param component {@link SACComponent}
	 *            of extension
	 * @return Z:Zsc, R:Rsc, T:Tsc
	 */
	public static SACExtension valueOfConvolutedSynthetic(SACComponent component) {
		switch (component) {
		case Z:
			return SACExtension.Zsc;
		case R:
			return SACExtension.Rsc;
		case T:
			return SACExtension.Tsc;
		default:
			throw new RuntimeException("Unexpected happens.");
		}
	}

	/**
	 * @param component {@link SACComponent}
	 *            of extension
	 * @return Z:Zsct, R:Rsct, T:Tsct
	 */
	public static SACExtension valueOfConvolutedTemporalPartial(SACComponent component) {
		switch (component) {
		case Z:
			return SACExtension.Zsct;
		case R:
			return SACExtension.Rsct;
		case T:
			return SACExtension.Tsct;
		default:
			throw new RuntimeException("Unexpected happens.");
		}
	}

	/**
	 * @param component
	 *            {@link SACComponent} of extension
	 * @return Z:Zst, R:Rst, T:Tst
	 */
	public static SACExtension valueOfTemporalPartial(SACComponent component) {
		switch (component) {
		case Z:
			return SACExtension.Zst;
		case R:
			return SACExtension.Rst;
		case T:
			return SACExtension.Tst;
		default:
			throw new RuntimeException("Unexpected happens.");
		}
	}

}
