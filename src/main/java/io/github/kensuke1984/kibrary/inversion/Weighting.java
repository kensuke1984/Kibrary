package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Weighting {
	public static double[] LowerUpperMantle1D(PartialID[] ids) {
		double[] lowerUpperW = new double[2];
		Map<Phases, Double> phasesSensitivityMap = Sensitivity.sensitivityPerWindowType(ids);
		Set<Phases> keySet = phasesSensitivityMap.keySet();
		Set<Phases> lowerMantle = keySet.stream().filter(phases -> phases.isLowerMantle())
				.collect(Collectors.toSet());
		Set<Phases> upperMantle = keySet.stream().filter(phases -> phases.isUpperMantle())
				.collect(Collectors.toSet());
		double upperMantleSensitivity = 0;
		double lowerMantleSensitivity = 0;
		for (Map.Entry<Phases, Double> entry : phasesSensitivityMap.entrySet()) {
			Phases p = entry.getKey();
			double s = entry.getValue();
			if (upperMantle.contains(p))
				upperMantleSensitivity += s;
			else if (lowerMantle.contains(p))
				lowerMantleSensitivity += s;
		}
		lowerUpperW[0] = 1. / lowerMantleSensitivity;
		lowerUpperW[1] = 1. / upperMantleSensitivity;
		return lowerUpperW;
	}
}
