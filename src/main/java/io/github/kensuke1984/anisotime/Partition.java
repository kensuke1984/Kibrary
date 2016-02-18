package io.github.kensuke1984.anisotime;

/**
 * Enums of egion and boundaries
 * 
 * @author Kensuke Konishi
 * @version 0.0.2
 */
enum Partition {
	MANTLE(0), CORE_MANTLE_BOUNDARY(1), OUTERCORE(2), INNER_CORE_BAUNDARY(3), INNERCORE(4),;

	private int value;

	private Partition(int i) {
		value = i;
	}

	/**
	 * @param partition
	 * @return if this is shallower or same as partition
	 */
	boolean shallow(Partition partition) {
		return value <= partition.value;
	}

	/**
	 * @return if this is boundary
	 */
	boolean isBoundary() {
		return value == 1 || value == 3;
	}

}
