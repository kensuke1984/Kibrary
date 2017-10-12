package io.github.kensuke1984.kibrary.inversion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.RuntimeErrorException;

public class WeightUnknownParameter {
	private UnknownParameterWeightType type;
	private Map<UnknownParameter, Double> weights;
	private List<UnknownParameter> unknowns;
	private Map<UnknownParameter, Double> timeWeightMap;
	
	public WeightUnknownParameter(UnknownParameterWeightType type,
			List<UnknownParameter> unknowns) {
		if (unknowns.stream()
				.filter(p -> p.getPartialType().isTimePartial())
				.count() > 0)
			throw new RuntimeException("Detected time partials. "
					+ "You must specify their weigthings (use the other constructor).");
		this.type = type;
		this.unknowns = unknowns;
		this.weights = new HashMap<>();
		
		computeWeights();
	}
	
	public WeightUnknownParameter(UnknownParameterWeightType type,
			List<UnknownParameter> unknowns, Map<UnknownParameter, Double> timeWeightMap) {
		this.type = type;
		this.unknowns = unknowns;
		this.weights = new HashMap<>();
		this.timeWeightMap = timeWeightMap;
		
		computeWeights();
		applyTimeWeight();
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
	
	private void applyTimeWeight() {
		for (UnknownParameter unknown : unknowns) {
			if (unknown.getPartialType().isTimePartial())
				weights.replace(unknown,
						timeWeightMap.get(unknown));
		}
	}
	
	private void computeWeights() {
		if (type.equals(UnknownParameterWeightType.TRANSITION_ZONE)) {
			for (UnknownParameter unknown : unknowns) {
				if (unknown.getPartialType().isTimePartial())
					weights.put(unknown, 1.);
				else {
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
}
