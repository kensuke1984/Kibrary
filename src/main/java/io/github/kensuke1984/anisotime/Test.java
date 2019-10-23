package io.github.kensuke1984.anisotime;

public class Test {

	public static void main(String[] args) {
		ComputationalMesh mesh = ComputationalMesh.simple(VelocityStructure.iprem());
		double rayParameter = Math.toDegrees(15.662);
		Raypath raypath = new Raypath(rayParameter, VelocityStructure.iprem(), mesh);
		raypath.compute();
	}

}
