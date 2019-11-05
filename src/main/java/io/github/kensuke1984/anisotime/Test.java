package io.github.kensuke1984.anisotime;

public class Test {

	public static void main(String[] args) {
//		ComputationalMesh mesh = ComputationalMesh.simple(VelocityStructure.iprem());
//		double rayParameterDegree = 15.662;
//		double rayParameter = Math.toDegrees(rayParameterDegree);
//		Raypath raypath = new Raypath(rayParameter, VelocityStructure.iprem(), mesh);
//		raypath.compute();
//		double delta = Math.toDegrees(raypath.computeDelta(6371., Phase.S));
//		double t = raypath.computeT(6371., Phase.S);
//		System.out.println(rayParameterDegree + " " + delta + " " + t);
		
		double d = 94.;
		try {
			ANISOtimeCLI.main(new String[] {"-mod", "iprem", "-ph", "S", "-deg", String.format("%.2f", d), "-h", "0", "-dec", "9"});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void problem1() {
		double eventR = 6371. - 0;
		ComputationalMesh mesh = ComputationalMesh.simple(VelocityStructure.iprem());
		for (int i = 0; i < 600; i++) {
//			double rayParameter = Math.toDegrees(15.68 - i*0.00016);
			double rayParameter = Math.toDegrees(8.74 - i *0.0001);
			Raypath raypath = new Raypath(rayParameter, VelocityStructure.iprem(), mesh);
			raypath.compute();
			System.out.println(Math.toRadians(rayParameter) + " " + Math.toDegrees(raypath.computeDelta(eventR, Phase.S))+ " " + (raypath.getTurningR(PhasePart.SH))
					+ " ");
		}
	}

}
