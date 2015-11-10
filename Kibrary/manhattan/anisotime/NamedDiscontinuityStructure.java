/**
 * 
 */
package anisotime;

import java.nio.file.Path;

/**
 * 
 * @author kensuke
 * @since 2014/05/21
 * @version 0.0.1
 * 
 * 
 * @version 0.0.2
 * @since 2014/7/28 operation verification Computation of turningRs installed.
 * 
 * @version 0.0.3
 * @since 2015/1/20
 * bug fix in turningRs 
 * 
 * @version 0.0.4
 * @since 2015/8/31
 * {@link Path} base
 */
class NamedDiscontinuityStructure implements VelocityStructure {

	manhattan.template.NamedDiscontinuityStructure structure;

	/*
	 * (non-Javadoc)
	 * 
	 * @see traveltime.manhattan.VelocityStructure#shTurningR(double)
	 */
	@Override
	public double shTurningR(double rayParameter) {
		// System.out.println(rayParameter);
		for (int i = structure.getNzone() - 1; 0 <= i; i--) {
			double vsA = structure.getVsA(i);
			double vsB = structure.getVsB(i);
			double r = Math.pow(1 / (vsA * rayParameter), 1 / (vsB - 1));
//			System.out.println(i + " " + structure.getBoundary(i) + " "
//					+ structure.getBoundary(i + 1) + " " + r);
			if (structure.getBoundary(i) <= r
					&& r <= structure.getBoundary(i + 1))
				return r;
		}

		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see traveltime.manhattan.VelocityStructure#svTurningR(double)
	 */
	@Override
	public double svTurningR(double rayParameter) {
		return shTurningR(rayParameter);
	}

	private NamedDiscontinuityStructure() {
	}

	public static NamedDiscontinuityStructure prem() {
		NamedDiscontinuityStructure nd = new NamedDiscontinuityStructure();
		nd.structure = manhattan.template.NamedDiscontinuityStructure.prem();
		return nd;
	}

	@Override
	public double pTurningR(double rayParameter) {
		for (int i = structure.getNzone() - 1; 0 <= i; i--) {
			double vpA = structure.getVpA(i);
			double vpB = structure.getVpB(i);
			double r = Math.pow(1 / (vpA * rayParameter), 1 / (vpB - 1));
			// System.out.println(i+" "+structure.getBoundary(i)+" "+r+" "+structure.getBoundary(i+1));

			if (structure.getBoundary(i) <= r
					&& r < structure.getBoundary(i + 1))
				return r;
		}
		return -1;
	}

	NamedDiscontinuityStructure(Path path) {
		structure = new manhattan.template.NamedDiscontinuityStructure(path);
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
