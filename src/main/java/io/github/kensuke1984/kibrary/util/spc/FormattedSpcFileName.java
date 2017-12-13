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
 * @version 0.0.1
 */
public class FormattedSpcFileName extends SPCFile {

    private static final long serialVersionUID = -6340811322023603513L;

    /**
     * spheroidal mode PSV, toroidal mode SH
     */
    private SpcFileComponent mode;
    /**
     * PB: backward or PF: forward, PAR2: mu
     */
    private SpcFileType fileType;
    private String x, y;
    private String observerID;
    private String sourceID;

    /**
     * @param parent {@link File} of a parent folder of the spectrum file
     * @param child  a name of spectrum file
     */
    public FormattedSpcFileName(File parent, String child) {
        super(parent, child);
        readName(getName());
    }

    /**
     * @param parent of a parent folder of the spectrum file
     * @param child  a name of spectrum file
     */
    public FormattedSpcFileName(String parent, String child) {
        super(parent, child);
        readName(getName());
    }

    /**
     * @param pathname path of a spectrum file
     */
    public FormattedSpcFileName(String pathname) {
        super(pathname);
        readName(getName());
    }

    /**
     * @param path {@link Path} of a spectrum file
     */
    public FormattedSpcFileName(Path path) {
        this(path.toString());
    }

    public FormattedSpcFileName(URI uri) {
        super(uri);
        readName(getName());
    }

    /**
     * @param fileName name of spc file
     * @return event id
     */
    private static String getEventID(String fileName) {
        switch (fileName.split("\\.").length) {
            case 3:
                String str = fileName.split("\\.")[1].replace("PSV", "").replace("SH", "");
                return str;
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
    private static SpcFileType getFileType(String fileName) {
        if (fileName.split("\\.").length != 7) return SpcFileType.SYNTHETIC;
        return SpcFileType.valueOf(fileName.split("\\.")[2].replace("par", "PAR"));
    }

    private static String getObserverID(String fileName) {
        return fileName.split("\\.")[0];
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
    private static SpcFileComponent getMode(String fileName) {
        return fileName.endsWith("PSV.spc") ? SpcFileComponent.PSV : SpcFileComponent.SH;
    }

    /**
     * @param path for check
     * @return if the filePath is formatted.
     */
    public static boolean isSpcFileName(Path path) {
        return isSpcFileName(path.getFileName().toString());
    }

    /**
     * @param file {@link File} for check
     * @return if the file is formatted.
     */
    public static boolean isSpcFileName(File file) {
        return isSpcFileName(file.getName());
    }

    private static boolean isSpcFileName(String name) {
        if (!name.endsWith(".spc")) return false;
        if (!name.endsWith("PSV.spc") && !name.endsWith("SH.spc")) {
            System.err.println("SPC file name must end with [PSV, SH].spc (psv, sh not allowed anymore).");
            return false;
        }
        String[] parts = name.split("\\.");
        if (parts.length != 3 && parts.length != 7) {
            System.err.println("SPC file name must be station.GlobalCMTID(PSV, SV).spc or " +
                    "station.GlobalCMTID.type(par2, PF, PB .etc).x.y.(PSV, SH).spc");
            return false;
        }

        if (8 < getObserverID(name).length()) {
            System.err.println("Name of station cannot be over 8 characters.");
            return false;
        }

        return true;
    }

    /**
     * @param fileName file name for chack
     * @return 理論波形（非偏微分波形）かどうか
     */
    public static boolean isSynthetic(String fileName) {
        return fileName.split("\\.").length == 3;
    }

    @Override
    public String getSourceID() {
        return sourceID;
    }

    private void readName(String fileName) {
        if (!isSpcFileName(fileName)) throw new IllegalArgumentException(fileName + " is not a valid Spcfile name.");
        observerID = getObserverID(fileName);
        sourceID = getEventID(fileName);
        fileType = getFileType(fileName);
        mode = getMode(fileName);
        x = getX(fileName);
        y = getY(fileName);
    }

    @Override
    public DSMOutput read() throws IOException {
        return Spectrum.getInstance(this);
    }

    @Override
    public SpcFileComponent getMode() {
        return mode;
    }

    @Override
    public SpcFileType getFileType() {
        return fileType;
    }

    @Override
    public String getObserverID() {
        return observerID;
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