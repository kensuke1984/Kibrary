package io.github.kensuke1984.kibrary.inversion.addons;


import java.util.Arrays;

public enum WeightingType {
	RECIPROCAL(1), LOWERUPPERMANTLE(2), TAKEUCHIKOBAYASHI(3), USERFUNCTION(4), IDENTITY(5), 
	FINAL(6), RECIPROCAL_AZED(7), RECIPROCAL_AZED_DPP(8), RECIPROCAL_AZED_TZCA(9),
	RECIPROCAL_STAEVT_TZCA(10), RECIPROCAL_AZED_DPP_V2(11), RECIPROCAL_PcP(12), RECIPROCAL_COS(13),
	RECIPROCAL_CC(14), RECIPROCAL_FREQ(15);
	
	private int value;
	
	private WeightingType(int n) {
		value = n;
	}
	
	public int getValue() {
		return value;
	}

	public static WeightingType getType(int n) {
		return Arrays.stream(WeightingType.values()).filter(type -> type.value == n).findAny()
				.orElseThrow(() -> new IllegalArgumentException("Input n " + n + " is invalid."));
	}
}
