/**
 * 
 */
package filehandling.spc;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.FastMath;

import filehandling.sac.SACComponent;
import manhattan.dsminformation.PolynomialStructure;
import manhattan.template.HorizontalPosition;
import manhattan.template.Location;

/**
 * Conversion of a partial derivative<br>
 * for &mu;<sub>0</sub> to Q<sub>&mu;</sub>(&ne;q) following Fuji <i>et al</i>.
 * (2010)
 * 
 * @author kensuke
 *
 * @version 0.0.1.1
 *
 */
public final class FujiConversion {

	private PolynomialStructure structure;

	/**
	 * @param structure
	 *            structure
	 */
	public FujiConversion(PolynomialStructure structure) {
		this.structure = structure == null ? PolynomialStructure.PREM : structure;
	}

	/**
	 * Conversion with respect to PREM
	 */
	public FujiConversion() {
		this(null);
	}

	/**
	 * @param spectrum
	 *            {@link PartialSpectrumFile}
	 * @return converted {@link PartialSpectrumFile}
	 */
	public DSMOutput convert(DSMOutput spectrum) {
		if (spectrum.getSpcFileType() != SpcFileType.PAR2)
			throw new RuntimeException();

		int nbody = spectrum.nbody();
		int np = spectrum.np();
		double tlen = spectrum.tlen();
		List<SpcBody> spcBodyList = new ArrayList<>(nbody);

		// data part
		double omegai = spectrum.omegai();
		HorizontalPosition observerPosition = spectrum.getObserverPosition();
		String observerID = spectrum.getObserverID();
		Location sourceLocation = spectrum.getSourceLocation();
		String sourceID = spectrum.getSourceID();
		double[] bodyR = spectrum.getBodyR();
		double omega0 = spectrum.tlen(); // TODO
		for (int i = 0; i < spectrum.nbody(); i++) {
			double r = bodyR[i];
			double q = 1 / structure.getQmu(r);
			double mu0 = structure.getMu(r);
			SpcBody body = spectrum.getSpcBodyList().get(i);
			SpcBody newBody = new SpcBody(3, np);
			// System.out.println(body.getNumberOfComponent());
			for (int ip = 0; ip < np + 1; ip++) {
				Complex[] uQ = new Complex[body.getNumberOfComponent()];
				double omegaOverOmega0 = (ip + 1) / omega0;
				for (int iComponent = 0; iComponent < body.getNumberOfComponent(); iComponent++) {
					Complex u = body.getSpcComponent(SACComponent.getComponent(iComponent + 1))
							.getValueInFrequencyDomain()[ip];
					double log = 2 * FastMath.log(omegaOverOmega0) / Math.PI;
					double dmudmu0Real = (1 + q * log);
					Complex dmudmu0 = Complex.valueOf(dmudmu0Real, dmudmu0Real * q);
					// System.out.println(dmudmu0);
					Complex dmudq = Complex.valueOf(mu0 * log, mu0 * (1 + 2 * log * q));
					// System.out.println(dmudq);
					uQ[iComponent] = u.multiply(-q * q).multiply(dmudq).divide(dmudmu0);
					// System.out.println(iComponent + " " + uQ[iComponent]);
				}
				newBody.add(ip, uQ);
				// System.exit(0);
			}
			spcBodyList.add(newBody);

		}
		DSMOutput dsmoutput = new DSMOutput() {

			@Override
			public double tlen() {
				return tlen;
			}

			@Override
			public double omegai() {
				return omegai;
			}

			@Override
			public int np() {
				return np;
			}

			@Override
			public int nbody() {
				return nbody;
			}

			@Override
			public SpcFileType getSpcFileType() {
				return SpcFileType.PARQ;
			}

			@Override
			public List<SpcBody> getSpcBodyList() {
				return spcBodyList;
			}

			@Override
			public Location getSourceLocation() {
				return sourceLocation;
			}

			@Override
			public String getSourceID() {
				return sourceID;
			}

			@Override
			public HorizontalPosition getObserverPosition() {
				return observerPosition;
			}

			@Override
			public String getObserverID() {
				return observerID;
			}

			@Override
			public double[] getBodyR() {
				return bodyR;
			}
		};

		return dsmoutput;
	}

}
