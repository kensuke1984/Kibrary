package io.github.kensuke1984.kibrary.util.spc;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * A <b>formatted</b> name of a spectrum file made by DSM<br>
 * <p>
 * This class is <b>IMMUTABLE</b>.
 * <p>
 * Synthetic: station.GlobalCMTID(PSV, SV).spc
 * <p>
 * Partial derivatives: station.GlobalCMTID.type(par2, PF, PB .etc).x.y.(PSV,
 * SH).spc
 * <p>
 * 'PSV', 'SH' must be upper case. 'station' must be 8 or less letters.
 *
 * @author Kensuke Konishi
 * @version 0.0.1.1
 * @author anselme add network
 */
public class FormattedSPCFile extends SPCFile {

    private static final long serialVersionUID = -6340811322023603513L;

    /**
     * spheroidal mode PSV, toroidal mode SH
     */
    private SPCMode mode;
    /**
     * PB: backward or PF: forward, PAR2: mu
     */
    private SPCType fileType;
    private String x, y;
    private String observerID;
    private String observerNetwork;
    private String sourceID;

    /**
     * @param parent {@link File} of a parent folder of the spectrum file
     * @param child  a name of spectrum file
     */
    public FormattedSPCFile(File parent, String child) {
        super(parent, child);
        readName(getName());
    }

    /**
     * @param parent of a parent folder of the spectrum file
     * @param child  a name of spectrum file
     */
    public FormattedSPCFile(String parent, String child) {
        super(parent, child);
        readName(getName());
    }

    /**
     * @param pathname path of a spectrum file
     */
    public FormattedSPCFile(String pathname) {
        super(pathname);
        readName(getName());
    }

    /**
     * @param path {@link Path} of a spectrum file
     */
    public FormattedSPCFile(Path path) {
        this(path.toString());
    }

    public FormattedSPCFile(URI uri) {
        super(uri);
        readName(getName());
    }

    /**
     * @param fileName name of spc file
     * @return event ID
     */
    private static String getEventID(String fileName) {
        switch (fileName.split("\\.").length) {
            case 3:
                return fileName.split("\\.")[1].replace("PSV", "").replace("SH", "");
            case 7:
                return fileName.split("\\.")[1];
            default:
                throw new RuntimeException("Unexpected");
        }
    }

    /**
     * 入力ファイルのSpcFileTypeを返す
     *
     * @param fileName name of SPC file
     * @return which par or syn...なんのスペクトルファイルか
     */
    private static SPCType getFileType(String fileName) {
        if (fileName.split("\\.").length != 7) return SPCType.SYNTHETIC;
        return SPCType.valueOf(fileName.split("\\.")[2].replace("par", "PAR"));
    }


    private static String getX(String fileName) {
        String[] parts = fileName.split("\\.");
        return parts.length != 7 ? null : parts[3];
    }

    private static String getY(String fileName) {
        String[] parts = fileName.split("\\.");
        return parts.length != 7 ? null : parts[4];
    }

    /**
     * @param fileName name of SPC file
     * @return PSV or SH
     * @throws RuntimeException if spc file has no indication of its mode.
     */
    private static SPCMode getMode(String fileName) {
        return fileName.endsWith("PSV.spc") ? SPCMode.PSV : SPCMode.SH;
    }

    /**
     * @param path for check
     * @return if the filePath is formatted.
     */
    public static boolean isFormatted(Path path) {
        return isFormatted(path.getFileName().toString());
    }

    @Override
    public String getSourceID() {
        return sourceID;
    }

    /**
     * @param fileName
     * @author anselme add network
     */
    private void readName(String fileName) {
        if (!isFormatted(fileName)) throw new IllegalArgumentException(fileName + " is not a valid Spcfile name.");
//        observerID = fileName.split("\\.")[0];
        observerID = fileName.split("\\.")[0].split("_")[0];
        sourceID = getEventID(fileName);
        fileType = getFileType(fileName);
        if (fileType.equals(SPCType.PB) || fileType.equals(SPCType.PF))
        	observerNetwork = null;
        else
        	observerNetwork = fileName.split("\\.")[0].split("_")[1];
        mode = getMode(fileName);
        x = getX(fileName);
        y = getY(fileName);
    }

    @Override
    public DSMOutput read() throws IOException {
        return Spectrum.getInstance(this);
    }

    @Override
    public SPCMode getMode() {
        return mode;
    }

    @Override
    public SPCType getFileType() {
        return fileType;
    }

    @Override
    public String getObserverID() {
        return observerID;
    }
    
    @Override
    public String getObserverNetwork() {
    	if (fileType.equals(SPCType.PB) || fileType.equals(SPCType.PF))
			throw new RuntimeException("PB and PF waveforms have no network");
    	return observerNetwork;
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    @Override
    public boolean isSynthetic() {
        return isSynthetic(getName());
    }

}
