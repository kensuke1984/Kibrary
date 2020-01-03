package io.github.kensuke1984.anisotime;

public class TestAnalytical {

	public static void main(String[] args) {
		Woodhouse1981 woodhouse = new Woodhouse1981(VelocityStructure.iprem());
		PhasePart pp = PhasePart.I;
		double rayParameter = 0.00001;
		compareDelta(woodhouse, pp, rayParameter);
	}
	
	static void compareDelta(Woodhouse1981 woodhouse, PhasePart pp, double rayParameter) {
		for (int i = 0; i < 10; i++) {
			double r = 0.001 + i * 1;
			double qDelta = woodhouse.computeQDelta(pp, rayParameter, r);
			double qDeltaApprox = woodhouse.computeQdeltaNearZero(pp, rayParameter, r);
			System.out.println(qDelta + " " + qDeltaApprox);
		}
	}

}
