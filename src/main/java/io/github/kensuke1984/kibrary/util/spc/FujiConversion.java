/**
 * 
 */
package io.github.kensuke1984.kibrary.util.spc;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.FastMath;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * Conversion of a partial derivative<br>
 * for &mu;<sub>0</sub> to Q<sub>&mu;</sub>(&ne;q) following Fuji <i>et al</i>.
 * (2010)
 * 
 * @author Kensuke Konishi
 *
 * @version 0.0.1.3
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

	public DSMOutput convert(DSMOutput spectrum) {
		if (spectrum.getSpcFileType() != SpcFileType.PAR2)
			throw new RuntimeException();

		final int nbody = spectrum.nbody();
		final int np = spectrum.np();
		final double tlen = spectrum.tlen();
		List<SpcBody> spcBodyList = new ArrayList<>(nbody);
		SpcFileName spcFileName = spectrum.getSpcFileName();

		// data part
		double omegai = spectrum.omegai();
		HorizontalPosition observerPosition = spectrum.getObserverPosition();
		String observerID = spectrum.getObserverID();
		String observerName = spectrum.getObserverName();
		String observerNetwork = spectrum.getObserverNetwork();
		Location sourceLocation = spectrum.getSourceLocation();
		String sourceID = spectrum.getSourceID();
		double[] bodyR = spectrum.getBodyR();
		double domega = 1. / spectrum.tlen() * 2. * Math.PI;
		double omega0 = 1. * 2. * Math.PI; //Hz
//		double omega0 = spectrum.tlen(); // TODO
		for (int i = 0; i < spectrum.nbody(); i++) {
			double r = bodyR[i];
			double q = 1 / structure.getQmuAt(r);
			double mu0 = structure.computeMu(r);
			SpcBody body = spectrum.getSpcBodyList().get(i);
			SpcBody newBody = new SpcBody(3, np);
			for (int ip = 0; ip < np + 1; ip++) {
				Complex[] uQ = new Complex[body.getNumberOfComponent()];
				double omegaOverOmega0 = (ip + 1) * domega / omega0;
				for (int iComponent = 0; iComponent < body.getNumberOfComponent(); iComponent++) {
					Complex u = body.getSpcComponent(SACComponent.getComponent(iComponent + 1))
							.getValueInFrequencyDomain()[ip];
					double log = 2 * FastMath.log(omegaOverOmega0) / Math.PI;
					double dmudmu0Real = (1 + q * log);
					Complex dmudmu0 = Complex.valueOf(dmudmu0Real, dmudmu0Real * q);
					Complex dmudq = Complex.valueOf(mu0 * log, mu0 * (1 + 2 * log * q));
					
//					partials with respect to large Q
//					uQ[iComponent] = u.multiply(-q * q).multiply(dmudq).divide(dmudmu0);
					
//					partials with respect to small q
					uQ[iComponent] = u.multiply(dmudq).divide(dmudmu0);
				}
				newBody.add(ip, uQ);
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
			public String getObserverName() {
				return observerName;
			}
			
			@Override
			public String getObserverNetwork() {
				return observerNetwork;
			}

			@Override
			public double[] getBodyR() {
				return bodyR;
			}
			
			@Override
			public SpcFileName getSpcFileName() {
				return spcFileName;
			}
		};

		return dsmoutput;
	}

}
