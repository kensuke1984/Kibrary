package io.github.kensuke1984.kibrary.inversion.addons;

public class PerturbationValue {
	private PerturbationType type;
	private double value;
	
	public PerturbationValue(PerturbationType type, double value) {
		this.type = type;
		this.value = value;
	}
	
	public PerturbationType getType() {
		return type;
	}
	
	public double getValue() {
		return value;
	}
	
	public PerturbationValue add(PerturbationValue other) {
		if (!other.type.equals(type))
			throw new RuntimeException("Types differ " + type + " " + other.type);
		return new PerturbationValue(type, other.value + value);
	}
}
