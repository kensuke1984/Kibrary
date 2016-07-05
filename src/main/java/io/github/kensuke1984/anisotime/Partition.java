package io.github.kensuke1984.anisotime;

/**
 * Enums of egion and boundaries
 * 
 * @author Kensuke Konishi
 * @version 0.0.2.3
 */
enum Partition {
	SURFACE(0), MANTLE(1), CORE_MANTLE_BOUNDARY(2), OUTERCORE(3), INNER_CORE_BOUNDARY(4), //
	INNERCORE(5), OUTSIDE(6);

	private final int value;

	private Partition(int i) {
		value = i;
	}

	/**
	 * @param partition
	 *            to check
	 * @return If this is shallower or same as partition
	 */
	boolean isShallowerOrIn(Partition partition) {
		return value <= partition.value;
	}

	/**
	 * @return If this is the inner-core, core-mantle boundary or the surface.
	 */
	boolean isBoundary() {
		return value == 2 || value == 4 || value == 0;
	}

}
