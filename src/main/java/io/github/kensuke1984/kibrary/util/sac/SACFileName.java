package io.github.kensuke1984.kibrary.util.sac;

import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * Style of names of SAC files. (SAC: seismic analysis code)
 * <p>
 * This class is <b>IMMUTABLE.</b><br>
 * This class is a sub class of {@link File} {@link #equals(Object)} and
 * {@link #hashCode()} is under the {@link File}
 * <p>
 * station.{@link GlobalCMTID}.{@link PartialType}.x.y.z.{@link SACExtension}
 * <br>
 * <p>
 * station, x, y, z: String the length of station must be 8 or shorter.
 * <p>
 * station must be 8 or less letters
 *
 * @author Kensuke Konishi
 * @version 2.0.0.2
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 */
public class SACFileName extends File {

	private static final long serialVersionUID = 7479212925997274364L;
	private String stationName;
	private GlobalCMTID globalCMTID;
	private SACExtension extension;
	private PartialType partialType;
	private String x, y, z;
	private WaveformType sacType;

    /**
     * @param parent {@link File} of a parent folder
     * @param child  name of a file
     */
	public SACFileName(File parent, String child) {
		super(parent, child);
		readName(child);
	}

    /**
     * @param parent path of a parent folder
     * @param child file name
     */
	public SACFileName(String parent, String child) {
		super(parent, child);
		readName(child);
	}

	/**
	 * @param pathname path of a file
	 */
	public SACFileName(String pathname) {
		super(pathname);
		readName(getName());
	}

	/**
	 * @param uri of a file
	 */
	public SACFileName(URI uri) {
		super(uri);
		readName(getName());
	}

	/**
	 * @param path {@link Path} of a file
	 */
	public SACFileName(Path path) {
		this(path.toString());
	}
	
	private static String getEventID(String fileName) {
		return fileName.split("\\.")[1];
	}

    /**
     * 観測波形か理論波形（偏微分係数波形含まない）
     *
     * @param fileName to check
     * @return if the fileName is observed or synthetic (not partial
     */
	private static boolean isOBSorSYN(String fileName) {
		return isSacFileName(fileName) && fileName.split("\\.").length == 3;
	}

	public static boolean isOBS(String fileName) {
		return getSacExtension(fileName).isOBS();
	}

    /**
     * @param fileName to check
     * @return 理論波形かどうか（偏微分係数波形含まない）
     */
	public static boolean isSYN(String fileName) {
		if (!isOBSorSYN(fileName))
			return false;
		SACExtension extension = getSacExtension(fileName);
		return !extension.isOBS() && !extension.isTemporalPartial();
	}
	
	/**
	 * @param fileName
	 * @return
	 * @author anselme
	 */
	public static boolean isTemporalPartial(String fileName) {
		SACExtension extension = getSacExtension(fileName);
		return extension.isTemporalPartial();
	}

	private static SACExtension getSacExtension(String fileName) {
		String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
		return SACExtension.valueOf(suffix);
	}

	private static SACComponent getComponent(String fileName) {
		return getSacExtension(fileName).getComponent();
	}

	private static PartialType getSacFileType(String fileName) {
		String[] parts = fileName.split("\\.");
		if (parts.length != 7) return null;
		String typeStr = parts[2].replace("par", "PAR");
		return PartialType.valueOf(typeStr);
	}

	private static String getStationName(String fileName) {
		return fileName.split("\\.")[0];
	}
	
	private static String getX(String fileName) {
		String[] parts = fileName.split("\\.");
		return parts.length != 7 ? null : parts[3];
	}

	private static String getY(String fileName) {
		if (fileName.split("\\.").length != 7) return null;
		return fileName.split("\\.")[4];
	}

	private static String getZ(String fileName) {
		if (fileName.split("\\.").length != 7) return null;
		return fileName.split("\\.")[5];
	}

    /**
     * is an input filename sacfile or not?
     *
     * @param fileName for check can be a path of a file
     * @return if the fileName is {@link SACFileName}
     */
	public static boolean isSacFileName(Path path) {
		return isSacFileName(path.toFile());
	}
	
    /**
     * is an input filename sacfile or not?
     *
     * @param fileName for check can be a path of a file
     * @return if the fileName is {@link SACFileName}
     */
	public static boolean isSacFileName(String fileName) {
		return isSacFileName(new File(fileName));
	}
	
    /**
     * is an input filename sacfile or not?
     *
     * @param file for check
     * @return if the file is a valid {@link SACFileName}
     */
	public static boolean isSacFileName(File file) {
		String fileName = file.getName();
		String[] part = fileName.split("\\.");

		if (part.length != 7 && part.length != 3)
			return false;

		// station name must be 8 letters or shorter.
		if (8 < part[0].split("_")[0].length())
			return false;

		// test if it is a global cmt id
		if (!GlobalCMTID.isGlobalCMTID(part[1]))
			return false;

		try {
			SACExtension.valueOf(part[part.length - 1]);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return getName();
	}

	public boolean isConvolved() {
		return extension.isConvoluted();
	}
	
	/**
	 * @return {@link SACExtension} of this sacfile name
	 */
	public SACExtension getExtension() {
		return extension;
	}

	public WaveformType getSacType() {
		return sacType;
	}
	
	private void readName(String fileName) {
		if (!isSacFileName(fileName))
			throw new IllegalArgumentException(fileName + " is an invalid sac file name");
		extension = getSacExtension(fileName);
		String eventID = getEventID(fileName);
		if (!GlobalCMTID.isGlobalCMTID(eventID))
			throw new IllegalArgumentException(fileName + " contains an invalid Global CMT ID");
		globalCMTID = new GlobalCMTID(eventID);
		stationName = getStationName(fileName);
		partialType = getSacFileType(fileName);
		x = getX(fileName);
		y = getY(fileName);
		z = getZ(fileName);
		if (isOBSorSYN(fileName))
			sacType = isOBS(fileName) ? WaveformType.OBS : WaveformType.SYN;
		else
			sacType = WaveformType.PARTIAL;
	}

	/**
	 * @return if it is observed
	 */
	public boolean isOBS() {
		return isOBS(getName());
	}

	/**
	 * @return if it is synthetic (no partial included)
	 */
	public boolean isSYN() {
		return isSYN(getName());
	}
	
	/**
	 * @return if it is a time partial
	 * @author anselme
	 */
	public boolean isTemporalPartial() {
		return isTemporalPartial(getName());
	}

	public GlobalCMTID getGlobalCMTID() {
		return globalCMTID;
	}
	
	public PartialType getPartialType() {
		return partialType;
	}

	public String getStationName() {
		return stationName;
	}
	
	public SACComponent getComponent() {
		return getComponent(getName());
	}

	public String getX() {
		return x;
	}

	public String getY() {
		return y;
	}

	public String getZ() {
		return z;
	}

    /**
     * @return (immutable) SACHeaderData of this file
     * @throws IOException if an I/O error occurs
     */
	public SACHeaderData readHeader() throws IOException {
		return new SACHeader(this);
	}

    /**
     * @return (immutable) SACData of this file
     * @throws IOException if an I/O error occurs
     */
	public SACData read() throws IOException {
		return new SACFile(this);
	}

}
