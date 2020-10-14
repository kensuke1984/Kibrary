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
 * @author anselme add content for BP/FP catalog
 */
public class Spectrum implements DSMOutput {

	private String observerID;
	private String observerNetwork;
	private String sourceID;
    private SPCFile spcFileName;
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
    private List<SPCBody> spcBody;
    private int nbody;
    private int nComponent;
    private double[] bodyR;
    private SPCType spcFileType;
    
    public Spectrum(SPCFile spcFileName) {
		this.spcFileName = spcFileName; // TODO
	}
    
	/**
	 * Interpolation for BP/FP catalog
	 * @param bp1
	 * @param bp2
	 * @param bp3
	 * @param dh
	 * @return
	 * @author anselme
	 */
	public static Spectrum interpolate(Spectrum bp1, Spectrum bp2, Spectrum bp3, double[] dh) {
		Spectrum bp = bp1;
		for (int ibody = 0; ibody < bp1.nbody; ibody++) {
			SPCBody body = SPCBody.interpolate(bp1.spcBody.get(ibody), bp2.spcBody.get(ibody), bp3.spcBody.get(ibody), dh);
			bp.spcBody.set(ibody, body);
		}
		
		return bp;
	}
	
    /**
     * If the named file does not exist, is a directory rather than a regular
     * file, or for some other reason cannot be opened for reading then a
     * {@link IOException} is thrown.
     *
     * @param spcFileName must exist.
     * @return Spectrum of the spcFileName
     * @throws IOException If the spcFileName does not exist, or an I/O error occurs
     * @author Kensuke Konishi
     * @author anselme add content for BP/FP catalog
     */
	final public static Spectrum getInstance(SPCFile spcFileName, double phi, HorizontalPosition observerPosition
			, Location sourceLocation, String observerName) throws IOException {
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(spcFileName)))) {
			Spectrum specFile = new Spectrum(spcFileName);
			specFile.sourceID = spcFileName.getSourceID();
			specFile.observerID = spcFileName.getObserverID();
			// read header PF
			// tlen
			double tlen = dis.readDouble();
			// np
			int np = dis.readInt();
			// nbody
			int nbody = dis.readInt();
			
			int ncomp = dis.readInt();
			System.out.println(np + " " + nbody + " " + ncomp);
			
			// ncomponents
			switch (ncomp) {
			case 0: // isotropic 1D partial par2 (lambda)
				specFile.spcFileType = spcFileName.getFileType();
				specFile.nComponent = 3;
				break;
			case 3: // normal synthetic
				specFile.nComponent = 3;
				specFile.spcFileType = SPCType.SYNTHETIC;
				break;
			case 7: // back propagation PSV catalog. 8 is an identifier. The actual number of component is 27 (27 non-zero component).
//				System.out.println("PBPSVCAT");
				specFile.nComponent = 27;
				specFile.spcFileType = SPCType.PBPSVCAT;
				break;
			case 8: // back propagation SH catalog. 8 is an identifier. The actual number of component is 27 (18 non-zero component).
				specFile.nComponent = 27;
				specFile.spcFileType = SPCType.PBSHCAT;
				break;
			case 9: // forward propagation
				specFile.nComponent = 9;
				specFile.spcFileType = SPCType.PF;
				break;
			case 10: // forward propagation SH catalog. 10 is an identifier. The actual number of component is 9.
				specFile.nComponent = 9;
				specFile.spcFileType = SPCType.PFSHCAT;
				break;
			case 11: // Optimized forward propagation SH catalog. 11 is an identifier. The actual number of component is 9. 
				specFile.nComponent = 9;
				specFile.spcFileType = SPCType.PFSHO;
				break;
			case 12: // forward propagation SH catalog. 10 is an identifier. The actual number of component is 9.
				specFile.nComponent = 9;
				specFile.spcFileType = SPCType.PFPSVCAT;
				break;
			case 27: // back propagation
				specFile.nComponent = 27;
				specFile.spcFileType = SPCType.PB;
				break;
			default:
				throw new RuntimeException("component can be only 3(synthetic), 7(bppsvcat), 8(bpshcat), 9(fp), 10(fpshcat), or 27(bp) right now");
			}
			
			if (observerName == null)
				specFile.observerID = spcFileName.getObserverID();
			else
				specFile.observerID = observerName;
			
			if (specFile.spcFileType.equals(SPCType.PB) || specFile.spcFileType.equals(SPCType.PF)
					|| specFile.spcFileType.equals(SPCType.PBSHCAT)
					|| specFile.spcFileType.equals(SPCType.PBPSVCAT)
					|| specFile.spcFileType.equals(SPCType.PFSHCAT)
					|| specFile.spcFileType.equals(SPCType.PFPSVCAT)
					|| specFile.spcFileType.equals(SPCType.PFSHO))
				specFile.observerNetwork = null;
			else
				specFile.observerNetwork = spcFileName.getObserverNetwork();
			
//			 System.out.println(nbody);
			specFile.nbody = nbody;
			specFile.np = np;
			specFile.tlen = tlen;
			
			specFile.spcBody = new ArrayList<>(nbody);
			for (int i = 0; i < nbody; i++)
				specFile.spcBody.add(new SPCBody(specFile.nComponent, np));

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
			case PAR0:
			case PAR1:
			case PAR2:
			case PARA:
			case PARC:
			case PARF:
			case PARL:
			case PARN:
			case SYNTHETIC:
				specFile.sourceLocation = new Location(dis.readDouble(), dis.readDouble(), dis.readDouble());
				break;
			case PBSHCAT:
			case PBPSVCAT:
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
//			case PBPSVCAT:
//				if (sourceLocation == null)
//					specFile.sourceLocation = new Location(dis.readDouble(), dis.readDouble(), 0);
//				else {
//					dis.readDouble();
//					dis.readDouble();
//					if (sourceLocation.getR() != Earth.EARTH_RADIUS)
//						throw new RuntimeException("Error: BP source depth should be 0. " + sourceLocation.getR() + " " + Earth.EARTH_RADIUS);
//					specFile.sourceLocation = sourceLocation;
//				}
//				break;
			case PF:
			case PFSHO:
				specFile.sourceLocation = new Location(dis.readDouble(), dis.readDouble(), dis.readDouble());
				break;
			case PFSHCAT:
			case PFPSVCAT:
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
			if (specFile.spcFileType != SPCType.SYNTHETIC)
				for (int i = 0; i < nbody; i++)
					specFile.bodyR[i] = dis.readDouble();
			
			double cosphi = FastMath.cos(phi);
			double sinphi = FastMath.sin(phi);
			double cos2phi = FastMath.cos(2 * phi);
			double sin2phi = FastMath.sin(2 * phi);

			// read body
			for (int i = 0; i < np + 1; i++)
				for (SPCBody body : specFile.spcBody) {
					Complex[] u = new Complex[specFile.nComponent];
					int ip = dis.readInt();
					if (specFile.spcFileType.equals(SPCType.PBSHCAT)) {
						for (int k = 0; k < specFile.nComponent; k++) {
							if (SPCTensorComponent.isBPSHCATzero(k+1)) 
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
					else if (specFile.spcFileType.equals(SPCType.PBPSVCAT)) {
						for (int k = 0; k < specFile.nComponent; k++) {
							double tmpReal_m1 = dis.readDouble();
							double tmpImag_m1 = dis.readDouble();
							double tmpReal_m0 = dis.readDouble();
							double tmpImag_m0 = dis.readDouble();
							double tmpReal_p1 = dis.readDouble();
							double tmpImag_p1 = dis.readDouble();
							
//								System.out.println(k + " " + tmpReal_m1 + " " + tmpReal_p1 + " " + tmpReal_m0 +  " " + tmpImag_m0 + " " + tmpImag_m1 + " " + tmpImag_p1);
							
//								double cosphi = FastMath.cos(phi);
//								double sinphi = FastMath.sin(phi);
							
							double tmpReal = tmpReal_m0 + tmpReal_m1*cosphi + tmpImag_m1*sinphi
									+ tmpReal_p1*cosphi - tmpImag_p1*sinphi;
							double tmpImag = tmpImag_m0 + -tmpReal_m1*sinphi + tmpImag_m1*cosphi
									+ tmpReal_p1*sinphi + tmpImag_p1*cosphi;
							
							u[k] = new Complex(tmpReal, tmpImag);
						}
					}
					else if (specFile.spcFileType.equals(SPCType.PFSHCAT) ) {
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
					else if (specFile.spcFileType.equals(SPCType.PFPSVCAT) ) {
						for (int k = 0; k < specFile.nComponent; k++) {
							double tmpReal_m2 = dis.readDouble();
							double tmpImag_m2 = dis.readDouble();
							double tmpReal_m1 = dis.readDouble();
							double tmpImag_m1 = dis.readDouble();
							double tmpReal_m0 = dis.readDouble();
							double tmpImag_m0 = dis.readDouble();
							double tmpReal_p1 = dis.readDouble();
							double tmpImag_p1 = dis.readDouble();
							double tmpReal_p2 = dis.readDouble();
							double tmpImag_p2 = dis.readDouble();
							
							double tmpReal = tmpReal_m0 + tmpReal_m2*cos2phi + tmpImag_m2*sin2phi
									+ tmpReal_m1*cosphi + tmpImag_m1*sinphi
									+ tmpReal_p1*cosphi - tmpImag_p1*sinphi
									+ tmpReal_p2*cos2phi - tmpImag_p2*sin2phi;
							double tmpImag = tmpImag_m0 - tmpReal_m2*sin2phi + tmpImag_m2*cos2phi 
									- tmpReal_m1*sinphi + tmpImag_m1*cosphi
									+ tmpReal_p1*sinphi + tmpImag_p1*cosphi
									+ tmpReal_p2*sin2phi + tmpImag_p2*cos2phi;
							
							u[k] = new Complex(tmpReal, tmpImag);
						}
					}
					else if (specFile.spcFileType.equals(SPCType.PFSHO)) {
						for (int k = 0; k < specFile.nComponent; k++) {
							if (SPCTensorComponent.isFPSHzero(k+1)) 
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
	
	/**
	 * @param spcFileName
	 * @param phi
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	public final static Spectrum getInstance(SPCFile spcFileName, double phi) throws IOException {
		return getInstance(spcFileName, phi, null, null, null);
	}
	
	/**
	 * @param spcFileName
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	public final static Spectrum getInstance(SPCFile spcFileName) throws IOException {
		return getInstance(spcFileName, 0., null, null, null);
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

	public SPCFile getSpcFileName() {
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
	public List<SPCBody> getSpcBodyList() {
		return Collections.unmodifiableList(spcBody);
	}
	
	@Override
	public int nbody() {
		return nbody;
	}

	@Override
	public SPCType getSpcFileType() {
		return spcFileType;
	}

	@Override
	public double[] getBodyR() {
		return bodyR.clone();
	}
	
	@Override
	public void setSpcBody(int i, SPCBody body) {
		spcBody.set(i, body.copy());
	}
}
