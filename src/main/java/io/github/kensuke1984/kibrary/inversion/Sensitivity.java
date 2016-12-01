package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;

import java.util.HashMap;
import java.util.Map;

public class Sensitivity {
	public static Map<Phases, Double> sensitivityPerWindowType(PartialID[] ids) {
		Map<Phases, Double> typeSensitivityMap = new HashMap<>();
		for (PartialID id : ids) {
			double s = Sensitivity1D.idToSensitivity(id);
			Phases phases = new Phases(id.getPhases());
			if (typeSensitivityMap.containsKey(phases)) {
				s += typeSensitivityMap.get(phases);
				typeSensitivityMap.replace(phases, s);
			}
			else
				typeSensitivityMap.put(phases, s);
		}
		return typeSensitivityMap;
	}
}
