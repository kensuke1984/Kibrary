package io.github.kensuke1984.anisotime;

public class Test {

	public static void main(String[] args) {
		ComputationalMesh mesh = ComputationalMesh.simple(VelocityStructure.iprem());
		double rayParameterDegree = 15.662;
		double rayParameter = Math.toDegrees(rayParameterDegree);
		Raypath raypath = new Raypath(rayParameter, VelocityStructure.iprem(), mesh);
		raypath.compute();
		double delta = Math.toDegrees(raypath.computeDelta(6371., Phase.S));
		double t = raypath.computeT(6371., Phase.S);
		System.out.println(rayParameterDegree + " " + delta + " " + t);
	}

}
