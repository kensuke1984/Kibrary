package io.github.kensuke1984.kibrary.util.spc;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * 
 * A name of a spectrum file made by DSM<br>
 * 
 * This class is <b>IMMUTABLE</b>.
 * 
 * <p>
 * Synthetic: station.GlobalCMTID(PSV, SV).spc
 * 
 * <p>
 * Partial derivatives: station.GlobalCMTID.type(par2, PF, PB .etc).x.y.(PSV,
 * SH).spc
 * 
 * <p>
 * 'PSV', 'SH' must be upper case. 'station' must be 8 or less letters.
 * 
 * 
 * @author Kensuke Konishi
 * @version 0.1.0.3
 * 
 */
public class SpcFileName extends File {

	private static final long serialVersionUID = -6340811322023603513L;

	/**
	 * spheroidal mode PSV, toroidal mode SH
	 */
	private SpcFileComponent mode;

	/**
	 * おかしいときがあるかも
	 * 
	 * @param fileName
	 * @return event id
	 */
	private static String getEventID(String fileName) {
		switch (fileName.split("\\.").length) {
		case 3:		//理論波形の場合
			String str = fileName.split("\\.")[1].replace("PSV", "").replace("SH", "");
			return str;
		case 7:		//偏微分係数波形の場合
			return fileName.split("\\.")[1];
		default:
			throw new RuntimeException("Unexpected");
		}
	}

	/**
	 * @return ID of source
	 */
	public String getSourceID() {
		return sourceID;
	}

	/**
	 * 入力ファイルのSpcFileTypeを返す
	 * 
	 * @param fileName
	 * @return which par or syn...なんのスペクトルファイルか
	 */
	private static SpcFileType getFileType(String fileName) {
		if (fileName.split("\\.").length != 7)
			return SpcFileType.SYNTHETIC;
		return SpcFileType.valueOf(fileName.split("\\.")[2].replace("par", "PAR"));
	}

	@Deprecated
	private static String getObserverID(String fileName) {
		return fileName.split("\\.")[0];
	}
	
	private static String getObserverName(String fileName) {
		return fileName.split("\\.")[0];
	}
	
	private static String getObserverNetwork(String fileName) {
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
	 * @param fileName
	 * @return PSV or SH
	 * @throws RuntimeException
	 *             if spc file has no indication of its mode.
	 */
	private static SpcFileComponent getMode(String fileName) {
		return fileName.endsWith("PSV.spc") ? SpcFileComponent.PSV : SpcFileComponent.SH;
	}

	@Deprecated
	private String observerID;
	
	/**
	 * Station name
	 */
	private String observerName;
	
	/**
	 * Network name
	 */
	private String observerNetwork;

	/**
	 * PB: backward or PF: forward, PAR2: mu
	 */
	private SpcFileType fileType;

	private String x, y;

	/**
	 * @param parent
	 *            {@link File} of a parent folder of the spectrum file
	 * @param child
	 *            a name of spectrum file
	 */
	public SpcFileName(File parent, String child) {
		super(parent, child);
		readName(getName());
	}

	/**
	 * @param parent
	 *            of a parent folder of the spectrum file
	 * @param child
	 *            a name of spectrum file
	 */
	public SpcFileName(String parent, String child) {
		super(parent, child);
		readName(getName());
	}

	/**
	 * @param pathname
	 *            path of a spectrum file
	 */
	public SpcFileName(String pathname) {
		super(pathname);
		readName(getName());
	}

	/**
	 * @param path
	 *            {@link Path} of a spectrum file
	 */
	public SpcFileName(Path path) {
		this(path.toString());
	}

	public SpcFileName(URI uri) {
		super(uri);
		readName(getName());
	}

	private String sourceID;

	private void readName(String fileName) {
		if (!isSpcFileName(fileName))
			throw new IllegalArgumentException(fileName + " is not a valid Spcfile name.");
		sourceID = getEventID(fileName);
		observerID = getObserverID(fileName);
		fileType = getFileType(fileName);
		observerName = getObserverName(fileName);
		if (fileType.equals(SpcFileType.PB) || fileType.equals(SpcFileType.PF))
			observerNetwork = null;
		else
			observerNetwork = getObserverNetwork(fileName);
		mode = getMode(fileName);
		x = getX(fileName);
		y = getY(fileName);
	}

	public DSMOutput read() throws IOException {
		return SpectrumFile.getInstance(this);
	}

	/**
	 * @return psv or sh
	 */
	public SpcFileComponent getMode() {
		return mode;
	}

	/**
	 * @param path
	 *            for check
	 * @return if the filePath is a valid {@link SpcFileName}
	 */
	public static boolean isSpcFileName(Path path) {
		return isSpcFileName(path.getFileName().toString());
	}

	/**
	 * 
	 * @param file
	 *            {@link File} for check
	 * @return if the file is a valid {@link SpcFileName}
	 */
	public static boolean isSpcFileName(File file) {
		return isSpcFileName(file.getName());
	}

	private static boolean isSpcFileName(String name) {
		if (!name.endsWith("PSV.spc") && !name.endsWith("SH.spc")) {
			System.err.println("A name of SPC file must end with PSV.spc or SH.spc (psv, sh not allowed anymore)");
			return false;
		}
		String[] parts = name.split("\\.");
		if (parts.length != 3 && parts.length != 7) {
			System.err.println("Name of a spcfile must be station.GlobalCMTID(PSV, SV).spc or "
					+ "station.GlobalCMTID.type(par2, PF, PB .etc).x.y.(PSV, SH).spc");
			return false;
		}

		if (parts.length == 3) {
			// synthetics have both station name and network name
			if (8 < getObserverName(name).length()) {
				System.err.println(getObserverName(name) + "Name of station cannot be over 8 characters");
			}
			if (8 < getObserverNetwork(name).length()) {
				System.err.println(getObserverNetwork(name) + "Name of network cannot be over 8 characters");
			}
		}
		else {
			// bp and fp waveforms have only a station name
			if (8 < getObserverName(name).length()) {
				System.err.println(getObserverName(name) + "Name of station cannot be over 8 characters");
			}
		}
		

		return true;
	}

	public SpcFileType getFileType() {
		return fileType;
	}

	public String getObserverID() {
		return observerID;
	}
	
	public String getObserverName() {
		return observerName;
	}
	
	public String getObserverNetwork() {
		if (fileType.equals(SpcFileType.PB) || fileType.equals(SpcFileType.PF))
			throw new RuntimeException("PB and PF waveforms have no network");
		return observerNetwork;
	}

	public String getObserverString() {
		if (fileType.equals(SpcFileType.PB) || fileType.equals(SpcFileType.PF))
			return observerName;
		else
			return observerName + "_" + observerNetwork;
	}
	
	public String getX() {
		return x;
	}

	public String getY() {
		return y;
	}

	/**
	 * @return 理論波形（非偏微分波形）かどうか
	 */
	public boolean isSynthetic() {
		return isSynthetic(getName());
	}

	/**
	 * @param fileName
	 *            file name for chack
	 * @return 理論波形（非偏微分波形）かどうか
	 */
	public static boolean isSynthetic(String fileName) {
		return fileName.split("\\.").length == 3;
	}

}