package io.github.kensuke1984.kibrary.util.spc;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.FastMath;

import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;

/**
 * Spectrum file by DSM.
 * 
 * @version 0.1.2
 * @author Kensuke Konishi
 * 
 */
public class SpectrumFile implements DSMOutput {

	private String observerName;
	private String observerNetwork;
	private String observerID;
	private String sourceID;

	@Override
	@Deprecated
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
	public String getSourceID() {
		return sourceID;
	}

	@Override
	public Location getSourceLocation() {
		return sourceLocation;
	}

	@Override
	public HorizontalPosition getObserverPosition() {
		return observerPosition;
	}

	private SpcFileName spcFileName;

	private double tlen;
	private int np;

	/**
	 * 震源の位置
	 */
	private Location sourceLocation;

	/**
	 * 観測点の位置 深さの情報は含まない
	 */
	private HorizontalPosition observerPosition;

	private double omegai;

	private List<SpcBody> spcBody;

	private int nbody;

	public SpcFileName getSpcFileName() {
		return spcFileName;
	}

	@Override
	public double tlen() {
		return tlen;
	}

	@Override
	public int np() {
		return np;
	}

	@Override
	public double omegai() {
		return omegai;
	}

	@Override
	public List<SpcBody> getSpcBodyList() {
		return Collections.unmodifiableList(spcBody);
	}

	private int nComponent;

	private double[] bodyR;

	public SpectrumFile(SpcFileName spcFileName) {
		this.spcFileName = spcFileName; // TODO
	}
	
	/**
	 * If the named file does not exist, is a directory rather than a regular
	 * file, or for some other reason cannot be opened for reading then a
	 * {@link IOException} is thrown.
	 * 
	 * @param spcFileName
	 *            must exist.
	 * @return SpectrumFile of the spcFileName
	 * @throws IOException
	 *             If the spcFileName does not exist, or an I/O error occurs
	 */
	final public static SpectrumFile getInstance(SpcFileName spcFileName, double phi, HorizontalPosition observerPosition
			, Location sourceLocation, String observerName) throws IOException {
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(spcFileName)))) {
			SpectrumFile specFile = new SpectrumFile(spcFileName);
			specFile.sourceID = spcFileName.getSourceID();
			specFile.observerID = spcFileName.getObserverID();
			// read header PF
			// tlen
			double tlen = dis.readDouble();
			// np
			int np = dis.readInt();
			// nbody
			int nbody = dis.readInt();

			// ncomponents
			switch (dis.readInt()) {
			case 0: // isotropic 1D partial par2 (lambda)
				specFile.spcFileType = spcFileName.getFileType();
				specFile.nComponent = 3;
				break;
			case 3: // normal synthetic
				specFile.nComponent = 3;
				specFile.spcFileType = SpcFileType.SYNTHETIC;
				break;
			case 8: // back propagation SH catalog. 8 is an identifier. The actual number of component is 27 (18 non-zero component).
				specFile.nComponent = 27;
				specFile.spcFileType = SpcFileType.PBSHCAT;
				break;
			case 9: // forward propagation
				specFile.nComponent = 9;
				specFile.spcFileType = SpcFileType.PF;
				break;
			case 10: // forward propagation SH catalog. 10 is an identifier. The actual number of component is 9.
				specFile.nComponent = 9;
				specFile.spcFileType = SpcFileType.PFSHCAT;
				break;
			case 11: // Optimized forward propagation SH catalog. 11 is an identifier. The actual number of component is 9. 
				specFile.nComponent = 9;
				specFile.spcFileType = SpcFileType.PFSHO;
				break;
			case 27: // back propagation
				specFile.nComponent = 27;
				specFile.spcFileType = SpcFileType.PB;
				break;
			default:
				throw new RuntimeException("component can be only 3(synthetic), 8(bpshcat), 9(fp), 10(fpshcat), or 27(bp) right now");
			}
			
			if (observerName == null)
				specFile.observerName = spcFileName.getObserverName();
			else
				specFile.observerName = observerName;
			if (specFile.spcFileType.equals(SpcFileType.PB) || specFile.spcFileType.equals(SpcFileType.PF)
					|| specFile.spcFileType.equals(SpcFileType.PBSHCAT)
					|| specFile.spcFileType.equals(SpcFileType.PBPSVCAT)
					|| specFile.spcFileType.equals(SpcFileType.PFSHCAT)
					|| specFile.spcFileType.equals(SpcFileType.PFPSVCAT)
					|| specFile.spcFileType.equals(SpcFileType.PFSHO))
				specFile.observerNetwork = null;
			else
				specFile.observerNetwork = spcFileName.getObserverNetwork();
			
//			 System.out.println(nbody);
			specFile.nbody = nbody;
			specFile.np = np;
			specFile.tlen = tlen;
			
			specFile.spcBody = new ArrayList<>(nbody);
			for (int i = 0; i < nbody; i++)
				specFile.spcBody.add(new SpcBody(specFile.nComponent, np));

			// data part
			specFile.omegai = dis.readDouble();
			
			if (observerPosition == null)
				specFile.observerPosition = new HorizontalPosition(dis.readDouble(), dis.readDouble());
			else {
				dis.readDouble();
				dis.readDouble();
				specFile.observerPosition = observerPosition;
			}

			//
			switch (specFile.spcFileType) {
			case SYNTHETIC:
				specFile.sourceLocation = new Location(dis.readDouble(), dis.readDouble(), dis.readDouble());
				break;
			case PBSHCAT:
				if (sourceLocation == null)
					specFile.sourceLocation = new Location(dis.readDouble(), dis.readDouble(), 0);
				else {
					dis.readDouble();
					dis.readDouble();
					if (sourceLocation.getR() != Earth.EARTH_RADIUS)
						throw new RuntimeException("Error: BP source depth should be 0. " + sourceLocation.getR() + " " + Earth.EARTH_RADIUS);
					specFile.sourceLocation = sourceLocation;
				}
				break;
			case PBPSVCAT:
				specFile.sourceLocation = new Location(dis.readDouble(), dis.readDouble(), 0);
				break;
			case PF:
			case PFSHO:
				specFile.sourceLocation = new Location(dis.readDouble(), dis.readDouble(), dis.readDouble());
				break;
			case PFSHCAT:
				if (sourceLocation == null)
					specFile.sourceLocation = new Location(dis.readDouble(), dis.readDouble(), dis.readDouble());
				else {
					dis.readDouble();
					dis.readDouble();
					dis.readDouble();
					specFile.sourceLocation = sourceLocation;
				}
				break;
			case PB:
				specFile.sourceLocation = new Location(dis.readDouble(), dis.readDouble(), 0); // TODO
				break;
			default:
				throw new RuntimeException("Unexpected");
			}

			specFile.bodyR = new double[nbody];
			if (specFile.spcFileType != SpcFileType.SYNTHETIC)
				for (int i = 0; i < nbody; i++)
					specFile.bodyR[i] = dis.readDouble();
			
			double cosphi = FastMath.cos(phi);
			double sinphi = FastMath.sin(phi);
			double cos2phi = FastMath.cos(2 * phi);
			double sin2phi = FastMath.sin(2 * phi);

			// read body
			for (int i = 0; i < np + 1; i++)
				for (SpcBody body : specFile.spcBody) {
					Complex[] u = new Complex[specFile.nComponent];
					int ip = dis.readInt();
					if (specFile.spcFileType.equals(SpcFileType.PBSHCAT)) {
						for (int k = 0; k < specFile.nComponent; k++) {
							if (SpcTensorComponent.isBPSHCATzero(k+1)) 
								u[k] = Complex.ZERO;
							else {
								double tmpReal_m1 = dis.readDouble();
								double tmpImag_m1 = dis.readDouble();
								double tmpReal_p1 = dis.readDouble();
								double tmpImag_p1 = dis.readDouble();
								
//								System.out.println(k + " " + tmpReal_m1 + " " + tmpReal_p1 + " " + tmpImag_m1 + " " + tmpImag_p1);
								
//								double cosphi = FastMath.cos(phi);
//								double sinphi = FastMath.sin(phi);
								
								double tmpReal = tmpReal_m1*cosphi + tmpImag_m1*sinphi
										+ tmpReal_p1*cosphi - tmpImag_p1*sinphi;
								double tmpImag = -tmpReal_m1*sinphi + tmpImag_m1*cosphi
										+ tmpReal_p1*sinphi + tmpImag_p1*cosphi;
								
								u[k] = new Complex(tmpReal, tmpImag);
							}
						}
					}
					else if (specFile.spcFileType.equals(SpcFileType.PBPSVCAT)) {
						//TODO
					}
					else if (specFile.spcFileType.equals(SpcFileType.PFSHCAT)) {
						
						for (int k = 0; k < specFile.nComponent; k++) {
							double tmpReal_m2 = dis.readDouble();
							double tmpImag_m2 = dis.readDouble();
							double tmpReal_m1 = dis.readDouble();
							double tmpImag_m1 = dis.readDouble();
							double tmpReal_p1 = dis.readDouble();
							double tmpImag_p1 = dis.readDouble();
							double tmpReal_p2 = dis.readDouble();
							double tmpImag_p2 = dis.readDouble();
							
							double tmpReal = tmpReal_m2*cos2phi + tmpImag_m2*sin2phi
									+ tmpReal_m1*cosphi + tmpImag_m1*sinphi
									+ tmpReal_p1*cosphi - tmpImag_p1*sinphi
									+ tmpReal_p2*cos2phi - tmpImag_p2*sin2phi;
							double tmpImag = -tmpReal_m2*sin2phi + tmpImag_m2*cos2phi 
									- tmpReal_m1*sinphi + tmpImag_m1*cosphi
									+ tmpReal_p1*sinphi + tmpImag_p1*cosphi
									+ tmpReal_p2*sin2phi + tmpImag_p2*cos2phi;
							
							u[k] = new Complex(tmpReal, tmpImag);
						}
					}
					else if (specFile.spcFileType.equals(SpcFileType.PFSHO)) {
						for (int k = 0; k < specFile.nComponent; k++) {
							if (SpcTensorComponent.isFPSHzero(k+1)) 
								u[k] = Complex.ZERO;
							else
								u[k] = new Complex(dis.readDouble(), dis.readDouble());
						}
					}
					else {
						for (int k = 0; k < specFile.nComponent; k++)
							u[k] = new Complex(dis.readDouble(), dis.readDouble());
					}
					body.add(ip, u);
				}
			return specFile;
		}
	}
	
	public final static SpectrumFile getInstance(SpcFileName spcFileName, double phi) throws IOException {
		return getInstance(spcFileName, phi, null, null, null);
	}
	
	public final static SpectrumFile getInstance(SpcFileName spcFileName) throws IOException {
		return getInstance(spcFileName, 0., null, null, null);
	}
	
	public static SpectrumFile interpolate(SpectrumFile bp1, SpectrumFile bp2, SpectrumFile bp3, double[] dh) {
		SpectrumFile bp = bp1;
		for (int ibody = 0; ibody < bp1.nbody; ibody++) {
			SpcBody body = SpcBody.interpolate(bp1.spcBody.get(ibody), bp2.spcBody.get(ibody), bp3.spcBody.get(ibody), dh);
			bp.spcBody.set(ibody, body);
		}
		
		return bp;
	}
	
	@Override
	public int nbody() {
		return nbody;
	}

	private SpcFileType spcFileType;

	@Override
	public SpcFileType getSpcFileType() {
		return spcFileType;
	}

	@Override
	public double[] getBodyR() {
		return bodyR.clone();
	}
}
