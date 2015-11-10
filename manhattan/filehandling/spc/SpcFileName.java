package filehandling.spc;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * 
 * A name of a spectrum file DSMより作られたSPCファイルの名前 This class is IMMUTABLE.
 * 
 * 理論 station.event(PSV, SV).spc
 * 
 * 偏微分係数波形 station(String8文字まで).event(GlobalCMTID).type(par2, PF, PB
 * .etc).x.y.(PSV, SH).spc
 * 
 * 'PSV', 'SH' must be upper case
 * 
 * @since 2013/12/17 
 * 
 * 
 * 
 * @author Kensuke
 * 
 */
public class SpcFileName extends File {

	private static final long serialVersionUID = -6340811322023603513L;

	/**
	 * spheroidal mode PSV, SH
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
	 * @return ID of source
	 */
	public String getSourceID(){
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
		String typeStr = fileName.split("\\.")[2];

		typeStr = typeStr.replace("par", "PAR");

		SpcFileType type = SpcFileType.valueOf(typeStr);
		// System.out.println(type);
		return type;
	}

	private static String getObserverID(String fileName) {
		return fileName.split("\\.")[0];
	}

	private static String getX(String fileName) {
		if (fileName.split("\\.").length != 7)
			return null;
		return fileName.split("\\.")[3];
	}

	private static String getY(String fileName) {
		if (fileName.split("\\.").length != 7)
			return null;
		return fileName.split("\\.")[4];
	}

	private static String getZ(String fileName) {
		if (fileName.split("\\.").length != 7)
			return null;
		return fileName.split("\\.")[5];
	}

	/**
	 * @param fileName
	 * @return PSV or SH
	 * @throws RuntimeException if spc file has no indication of its mode.
	 */
	private static SpcFileComponent getMode(String fileName) {
		if (fileName.endsWith("PSV.spc"))
			return SpcFileComponent.PSV;
		else if (fileName.endsWith("SH.spc"))
			return SpcFileComponent.SH;
		else
			throw new RuntimeException(
					"A name of SPC file must end with PSV.spc or SH.spc (psv, sh not allowed anymore)");
	}
	
	private String observerID;

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
		sourceID = getEventID(fileName);
		observerID = getObserverID(fileName);
		if (8 < observerID.length())
			throw new IllegalArgumentException("Station name must not be more than 8 letters.");
		fileType = getFileType(fileName);
		mode = getMode(fileName);
		x = getX(fileName);
		y = getY(fileName);
	}

	public DSMOutput read() throws IOException{
		return SpectrumFile.getInstance(this);
	}

	/**
	 * @return  psv or  sh
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
		return isSpcFileName(path.toFile());
	}

	/**
	 * 
	 * @param file
	 *            {@link File} for check
	 * @return if the file is a valid {@link SpcFileName}
	 */
	public static boolean isSpcFileName(File file) {
		String name = file.getName();
		String[] parts = name.split("\\.");
		if(!file.getName().endsWith("PSV.spc")&&!file.getName().endsWith("SH.spc"))
			return false;

		if (parts.length != 3 && parts.length != 7)
			return false;

		// station name can't has over 8 letters.
		if (8 < parts[0].length())
			return false;

		return true;
	}

	public SpcFileType getFileType() {
		return fileType;
	}

	public String getObserverID() {
		return observerID;
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
		return getName().split("\\.").length == 3;
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
