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
public final class VSConversion {

	private PolynomialStructure structure;

	/**
	 * @param structure
	 *            structure
	 */
	public VSConversion(PolynomialStructure structure) {
		this.structure = structure == null ? PolynomialStructure.PREM : structure;
	}

	/**
	 * Conversion with respect to PREM
	 */
	public VSConversion() {
		this(null);
	}

	public DSMOutput convert(DSMOutput spectrum) {
		if (spectrum.getSpcFileType() != SPCType.PAR2)
			throw new RuntimeException();

		final int nbody = spectrum.nbody();
		final int np = spectrum.np();
		final double tlen = spectrum.tlen();
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
		for (int i = 0; i < spectrum.nbody(); i++) {
			double r = bodyR[i];
			double fact = 2. * structure.getRhoAt(r) * structure.getVshAt(r);
			SPCBody body = spectrum.getSpcBodyList().get(i);
			SPCBody newBody = new SPCBody(3, np);
			for (int ip = 0; ip < np + 1; ip++) {
				Complex[] uIm = new Complex[body.getNumberOfComponent()];
				for (int iComponent = 0; iComponent < body.getNumberOfComponent(); iComponent++) {
					Complex u = body.getSpcComponent(SACComponent.getComponent(iComponent + 1))
							.getValueInFrequencyDomain()[ip];
					uIm[iComponent] = u.multiply(fact);
				}
				newBody.add(ip, uIm);
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
				return SPCType.PARVS;
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
