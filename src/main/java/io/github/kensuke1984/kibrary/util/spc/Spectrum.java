package io.github.kensuke1984.kibrary.util.spc;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import org.apache.commons.math3.complex.Complex;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Spectrum file by DSM.
 *
 * @author Kensuke Konishi
 * @version 0.1.2.1
 */
class Spectrum implements DSMOutput {

    private String observerID;
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

    private Spectrum(SPCFile spcFileName) {
        this.spcFileName = spcFileName; // TODO
    }

    /**
     * If the named file does not exist, is a directory rather than a regular
     * file, or for some other reason cannot be opened for reading then a
     * {@link IOException} is thrown.
     *
     * @param spcFileName must exist.
     * @return Spectrum of the spcFileName
     * @throws IOException If the spcFileName does not exist, or an I/O error occurs
     */
    static Spectrum getInstance(SPCFile spcFileName) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(spcFileName)))) {
            Spectrum specFile = new Spectrum(spcFileName);
            specFile.sourceID = spcFileName.getSourceID();
            specFile.observerID = spcFileName.getObserverID();
            // read header PF
            // TLEN
            double tlen = dis.readDouble();
            // NP
            int np = dis.readInt();
            // NBODY
            int nbody = dis.readInt();

            // ncomponents
            switch (dis.readInt()) {
                case 0: // isotropic 1D partial par2 (lambda)
                    specFile.spcFileType = spcFileName.getFileType();
                    specFile.nComponent = 3;
                    break;
                case 3: // normal synthetic
                    specFile.nComponent = 3;
                    specFile.spcFileType = SPCType.SYNTHETIC;
                    break;
                case 9: // forward propagation
                    specFile.nComponent = 9;
                    specFile.spcFileType = SPCType.PF;
                    break;
                case 27: // back propagation
                    specFile.nComponent = 27;
                    specFile.spcFileType = SPCType.PB;
                    break;
                default:
                    throw new RuntimeException("component can be only 3(synthetic), 9(fp) or 27(bp) right now");
            }
//			 System.out.println(NBODY);
            specFile.nbody = nbody;
            specFile.np = np;
            specFile.tlen = tlen;

            specFile.spcBody = new ArrayList<>(nbody);
            for (int i = 0; i < nbody; i++)
                specFile.spcBody.add(new SPCBody(specFile.nComponent, np));

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
            if (specFile.spcFileType != SPCType.SYNTHETIC) for (int i = 0; i < nbody; i++)
                specFile.bodyR[i] = dis.readDouble();

            // read body
            for (int i = 0; i < np + 1; i++)
                for (SPCBody body : specFile.spcBody) {
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
    public String getObserverID() {
        return observerID;
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

    SPCFile getSpcFileName() {
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

}
