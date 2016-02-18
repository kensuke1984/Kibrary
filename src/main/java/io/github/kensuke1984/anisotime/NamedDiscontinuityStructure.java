/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.nio.file.Path;

/**
 * Named discontinuity structre
 * 
 * @author Kensuke Konishi
 * 
 * @version 0.0.4.1
 */
class NamedDiscontinuityStructure implements VelocityStructure {

	io.github.kensuke1984.kibrary.util.NamedDiscontinuityStructure structure;

	@Override
	public double shTurningR(double rayParameter) {
		for (int i = structure.getNzone() - 1; 0 <= i; i--) {
			double vsA = structure.getVsA(i);
			double vsB = structure.getVsB(i);
			double r = Math.pow(1 / (vsA * rayParameter), 1 / (vsB - 1));
			if (structure.getBoundary(i) <= r && r <= structure.getBoundary(i + 1))
				return r;
		}
		return -1;
	}

	@Override
	public double svTurningR(double rayParameter) {
		return shTurningR(rayParameter);
	}

	private NamedDiscontinuityStructure() {
	}

	public static NamedDiscontinuityStructure prem() {
		NamedDiscontinuityStructure nd = new NamedDiscontinuityStructure();
		nd.structure = io.github.kensuke1984.kibrary.util.NamedDiscontinuityStructure.prem();
		return nd;
	}

	@Override
	public double pTurningR(double rayParameter) {
		for (int i = structure.getNzone() - 1; 0 <= i; i--) {
			double vpA = structure.getVpA(i);
			double vpB = structure.getVpB(i);
			double r = Math.pow(1 / (vpA * rayParameter), 1 / (vpB - 1));
			if (structure.getBoundary(i) <= r && r < structure.getBoundary(i + 1))
				return r;
		}
		return -1;
	}

	NamedDiscontinuityStructure(Path path) {
		structure = new io.github.kensuke1984.kibrary.util.NamedDiscontinuityStructure(path);
	}

	@Override
	public double getA(double r) {
		double v = structure.getVp(r);
		return v * v * getRho(r);
	}

	@Override
	public double getC(double r) {
		return getA(r);
	}

	@Override
	public double getF(double r) {
		return getA(r) - 2 * getL(r);
	}

	@Override
	public double getL(double r) {
		double vs = structure.getVs(r);
		return vs * vs * getRho(r);
	}

	@Override
	public double getN(double r) {
		return getL(r);
	}

	@Override
	public double innerCoreBoundary() {
		return structure.getInnerCoreBoundary();
	}

	@Override
	public double coreMantleBoundary() {
		return structure.getCoreMantleBoundary();
	}

	@Override
	public double earthRadius() {
		return structure.getBoundary(structure.getNzone());
	}

	@Override
	public double getRho(double r) {
		return structure.getRho(r);
	}

}
