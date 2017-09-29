package io.github.kensuke1984.kibrary.inversion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WeightUnknownParameter {
	private UnknownParameterWeightType type;
	private Map<UnknownParameter, Double> weights;
	private List<UnknownParameter> unknowns;
	
	public WeightUnknownParameter(UnknownParameterWeightType type,
			List<UnknownParameter> unknowns) {
		this.type = type;
		this.unknowns = unknowns;
		this.weights = new HashMap<>();
		
		computeWeights();
	}
	
	public Map<UnknownParameter, Double> getWeights() {
		return weights;
	}
	
	public List<UnknownParameter> getUnknowns() {
		return unknowns;
	}
	
	public UnknownParameterWeightType getType() {
		return type;
	}
	
	private void computeWeights() {
		if (type.equals(UnknownParameterWeightType.TRANSITION_ZONE)) {
			for (UnknownParameter unknown : unknowns) {
				double weight = 1.;
				double r = unknown.getLocation().getR();
				if (r >= 5700 && r < 5750) {
					weight = 1.7;
				}
				else if (r < 5700)
					weight = 2.7;
				weights.put(unknown, weight);
			}
		}
	}
}
