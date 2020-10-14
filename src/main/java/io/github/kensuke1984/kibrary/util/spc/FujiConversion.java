package io.github.kensuke1984.kibrary.util.spc;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversion of a partial derivative<br>
 * for &mu;<sub>0</sub> to q(&ne;Q<sub>&mu;</sub>) following Fuji <i>et al</i>.
 * (2010)
 *
 * @author Kensuke Konishi
 * @version 0.1.0
 */
public final class FujiConversion {

	private final PolynomialStructure STRUCTURE;

	/**
	 * @param structure STRUCTURE
	 */
	public FujiConversion(PolynomialStructure structure) {
		STRUCTURE = structure;
	}

	public DSMOutput convert(DSMOutput spectrum) {
		if (spectrum.getSpcFileType() != SPCType.PAR2) throw new RuntimeException();

		int nbody = spectrum.nbody();
		int np = spectrum.np();
		double tlen = spectrum.tlen();
		List<SPCBody> spcBodyList = new ArrayList<>(nbody);
		SPCFile spcFileName = spectrum.getSpcFileName();

		// data part
		double omegai = spectrum.omegai();
		HorizontalPosition observerPosition = spectrum.getObserverPosition();
		String observerID = spectrum.getObserverID();
		String observerNetwork = spectrum.getObserverNetwork();
		Location sourceLocation = spectrum.getSourceLocation();
		String sourceID = spectrum.getSourceID();
		double[] bodyR = spectrum.getBodyR();
		double domega = 1. / spectrum.tlen() * 2. * Math.PI;
		double omega0 = 1. * 2. * Math.PI; //Hz
//		double omega0 = spectrum.tlen(); // TODO
		for (int i = 0; i < spectrum.nbody(); i++) {
			double r = bodyR[i];
			double q = 1 / STRUCTURE.getQmuAt(r);
			double mu0 = STRUCTURE.computeMu(r);
			SPCBody body = spectrum.getSpcBodyList().get(i);
			SPCBody newBody = new SPCBody(3, np);
			for (int ip = 0; ip < np + 1; ip++) {
				Complex[] uQ = new Complex[body.getNumberOfComponent()];
				double omegaOverOmega0 = (ip + 1) * domega / omega0;
				for (int iComponent = 0; iComponent < body.getNumberOfComponent(); iComponent++) {
					Complex u = body.getSpcComponent(SACComponent.getComponent(iComponent + 1))
							.getValueInFrequencyDomain()[ip];
					
					
//					double log = 2 * FastMath.log(omegaOverOmega0) / Math.PI;
//					double dmudmu0Real = (1 + q * log);
//					Complex dmudmu0 = Complex.valueOf(dmudmu0Real, dmudmu0Real * q);
//					Complex dmudq = Complex.valueOf(mu0 * log, mu0 * (1 + 2 * log * q));
					
//					partials with respect to large Q
//					uQ[iComponent] = u.multiply(-q * q).multiply(dmudq).divide(dmudmu0);
					
					
					double log = FastMath.log(omegaOverOmega0);
					Complex dmudq = new Complex(2. / Math.PI * log, 1.)
							.multiply(new Complex(1. + q / Math.PI * log, q / 2.))
							.multiply(mu0);
					Complex tmp = new Complex(1. + q / Math.PI * log, q / 2.);
					Complex dmudmu0 = tmp.multiply(tmp);
					
					
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
			public SPCType getSpcFileType() {
				return SPCType.PARQ;
			}

			@Override
			public List<SPCBody> getSpcBodyList() {
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
			public String getObserverNetwork() {
				return observerNetwork;
			}

			@Override
			public double[] getBodyR() {
				return bodyR;
			}
			
			@Override
			public SPCFile getSpcFileName() {
				return spcFileName;
			}
			
			@Override
			public void setSpcBody(int i, SPCBody body) {
//				spcBody.set(i, body); //TODO
			}
		};

		return dsmoutput;
	}

}
