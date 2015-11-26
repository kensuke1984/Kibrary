package filehandling.sac;

import java.util.Arrays;

/**
 * Components of SAC<br>
 * Z(1), R(2), T(3)
 * 
 * 
 * @author kensuke
 * @since 2014/09/05
 * @version 0.0.2
 * 
 * @version 0.0.3
 * @since 2015/1/30 becomes strict.
 * 
 * @version 0.0.3.1
 * @since 2015/8/15
 * 
 * 
 */
public enum SACComponent {
	Z(1), R(2), T(3);

	private int value;

	private SACComponent(int i) {
		value = i;
	}

	/**
	 * @return 1(Z), 2(R), 3(T)
	 */
	public int valueOf() {
		return value;
	}

	/**
	 * @param n
	 *            index 1, 2, 3
	 * @return Z(1) R(2) T(3)
	 * @throws IllegalArgumentException
	 *             if the input n is not 1,2,3
	 */
	public static SACComponent getComponent(int n) {
		return Arrays.stream(SACComponent.values()).filter(component -> component.valueOf() == n).findAny()
				.orElseThrow(() -> new IllegalArgumentException("Invalid component! Components are Z(1) R(2) T(3)"));
	}

	/**
	 * @param sacHeaderData
	 *            must contain KCMPNM (vertical, radial or trnsvers)
	 * @return SACComponent of the input sacHeaderData
	 */
	public static SACComponent of(SACHeaderData sacHeaderData) {
		String kcmpnm = sacHeaderData.getSACString(SACHeaderEnum.KCMPNM);
		switch (kcmpnm) {
		case "vertical":
			return Z;
		case "radial":
			return R;
		case "trnsvers":
			return T;
		default:
			throw new RuntimeException("KCMPNM is invalid. must be vertical, radial or trnsvers");
		}
	}

}
