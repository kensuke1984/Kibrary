package filehandling.spc;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.complex.Complex;

import manhattan.template.HorizontalPosition;
import manhattan.template.Location;

/**
 * 新生spc File
 * 
 * @since 2014/10/9
 * @version 0.1.0
 * 
 * 
 * @version 0.1.1
 * @since 2015/4/9 {@link #observerPosition} : {@link HorizontalPosition}&rarr;
 *        {@link Location}
 * 
 * @version 0.1.1.1
 * @since 2015/8/5 {@link #getInstance(SpcFileName)} throws {@link IOException}
 * 
 * @version 0.1.2
 * @since 2015/8/15 {@link Path} base
 * 
 * @author Kensuke
 * 
 */
class SpectrumFile implements DSMOutput {

	private String observerID;
	private String sourceID;

	@Override
	public String getObserverID() {
		return observerID;
	}

	// /**
	// * thisとspc2の各成分を足し合わせる （psvとshの足し合わせ）
	// *
	// * @param newName
	// * name for new {@link PartialSpectrumFile}
	// * @param spc2
	// * additional {@link PartialSpectrumFile}
	// * @return new {@link PartialSpectrumFile}
	// */
	// PartialSpectrumFile combine(SpcFileName newName, PartialSpectrumFile
	// spc2) {
	// PartialSpectrumFile odps = new PartialSpectrumFile(newName);
	// boolean isOK = true;
	// if (nbody != spc2.nbody) {
	// System.out.println("nbodies are different." + nbody + " " + spc2.nbody);
	// isOK = false;
	// }
	// if (nComponent != spc2.nComponent) {
	// System.out.println("ncomponents are different." + nComponent + " " +
	// spc2.nComponent);
	// isOK = false;
	// }
	// if (np != spc2.np) {
	// System.out.println("nps are different." + np + " " + spc2.np);
	// isOK = false;
	// }
	// if (!observerPosition.equals(spc2.observerPosition)) {
	// System.out.println("observer locations are different." + observerPosition
	// + " " + spc2.observerPosition);
	// isOK = false;
	// }
	// if (omegai != spc2.omegai) {
	// System.out.println("omegais are different." + omegai + " " +
	// spc2.omegai);
	// isOK = false;
	// }
	// if (!sourceLocation.equals(spc2.sourceLocation)) {
	// System.out.println("source locations are different." + sourceLocation + "
	// " + spc2.sourceLocation);
	// isOK = false;
	// }
	// if (tlen != spc2.tlen) {
	// System.out.println("tlens are different" + tlen + " " + spc2.tlen);
	// isOK = false;
	// }
	// if (isOK) {
	// for (int i = 0; i < nbody; i++)
	// if (bodyR[i] != spc2.bodyR[i])
	// isOK = false;
	// if (!isOK)
	// System.out.println("bodyRs are different");
	// }
	// if (spcFileName.getFileType() != spc2.getSpcFileName().getFileType()) {
	// System.out.println("spcfiletypes are different " +
	// spcFileName.getFileType() + " "
	// + spc2.getSpcFileName().getFileType());
	// isOK = false;
	// }
	// if (!isOK)
	// return null;
	// odps.nbody = nbody;
	//
	// odps.nComponent = 3;
	//
	// odps.np = np;
	// odps.observerPosition = observerPosition;
	// odps.omegai = omegai;
	// odps.sourceLocation = sourceLocation;
	// odps.tlen = tlen;
	//
	// odps.bodyR = new double[nbody];
	// for (int i = 0; i < nbody; i++)
	// odps.bodyR[i] = bodyR[i];
	//
	// odps.initializeSPCBodies();
	// for (int i = 0; i < nbody; i++) {
	// odps.spcBody.get(i).addBody(spcBody.get(i));
	// odps.spcBody.get(i).addBody(spc2.spcBody.get(i));
	// }
	//
	// return odps;
	// }

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

	SpcFileName getSpcFileName() {
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

	SpectrumFile(SpcFileName spcFileName) {
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
	final static SpectrumFile getInstance(SpcFileName spcFileName) throws IOException {
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
			case 9: // forward propagation
				specFile.nComponent = 9;
				specFile.spcFileType = SpcFileType.PF;
				break;
			case 27: // back propagation
				specFile.nComponent = 27;
				specFile.spcFileType = SpcFileType.PB;
				break;
			default:
				throw new RuntimeException("component can be only 3(synthetic), 9(fp) or 27(bp) right now");
			}
			// System.out.println(nbody);
			specFile.nbody = nbody;
			specFile.np = np;
			specFile.tlen = tlen;
			specFile.spcBody = new ArrayList<>(nbody);
			for (int i = 0; i < nbody; i++)
				specFile.spcBody.add(new SpcBody(specFile.nComponent, np));

			// data part
			specFile.omegai = dis.readDouble();
			specFile.observerPosition = new HorizontalPosition(dis.readDouble(), dis.readDouble());

			//
			switch (specFile.nComponent) {
			case 3:
			case 9:
				specFile.sourceLocation = new Location(dis.readDouble(), dis.readDouble(), dis.readDouble());
				break;
			case 27:
				specFile.sourceLocation = new Location(dis.readDouble(), dis.readDouble(), 0); // TODO
				break;
			default:
				throw new RuntimeException("Unexpected");
			}

			specFile.bodyR = new double[nbody];
			if (specFile.spcFileType != SpcFileType.SYNTHETIC)
				for (int i = 0; i < nbody; i++)
					specFile.bodyR[i] = dis.readDouble();

			// read body
			for (int i = 0; i < np + 1; i++)
				for (SpcBody body : specFile.spcBody) {
					Complex[] u = new Complex[specFile.nComponent];
					int ip = dis.readInt();
					for (int k = 0; k < specFile.nComponent; k++)
						u[k] = new Complex(dis.readDouble(), dis.readDouble());
					body.add(ip, u);
				}
			return specFile;
		}

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
