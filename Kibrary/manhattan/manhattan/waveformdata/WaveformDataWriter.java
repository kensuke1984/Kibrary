package manhattan.waveformdata;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.lang3.StringUtils;

import filehandling.sac.WaveformType;

/**
 * BasicDatasetやPartialDatasetの書き込み This class will not overwrite. The writing
 * is with APPEND option You cannot mix obs/syn and partial.
 * 
 * @since 2013/11/13
 * 
 * 
 * @version 0.3
 * 
 * @author kensuke
 * 
 */
public class WaveformDataWriter implements Closeable, Flushable {
	public Path getIdPath() {
		return idPath;
	}

	public Path getDataPath() {
		return dataPath;
	}

	/**
	 * id情報の書き出し
	 */
	private DataOutputStream idStream;

	/**
	 * 波形情報の書き出し
	 */
	private DataOutputStream dataStream;

	/**
	 * id information file
	 */
	private Path idPath;

	/**
	 * 波形情報ファイル
	 */
	private Path dataPath;

	@Override
	public void close() throws IOException {
		idStream.close();
		dataStream.close();
	}

	@Override
	public void flush() throws IOException {
		idStream.flush();
		dataStream.flush();
	}

	/**
	 * It becomes true, when one partial ID is written. In other words, if it is
	 * true, you cannot write obs/syn anymore.
	 */
	private boolean partial;
	/**
	 * @see #partial
	 */
	private boolean obs;

	public WaveformDataWriter(Path idPath, Path dataPath) throws IOException {
		this.idPath = idPath;
		this.dataPath = dataPath;
		idStream = new DataOutputStream(new BufferedOutputStream(
				Files.newOutputStream(idPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)));
		dataStream = new DataOutputStream(new BufferedOutputStream(
				Files.newOutputStream(dataPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)));
		dataLength = Files.size(dataPath);
	}

	/**
	 * The file size (byte). (should be StartByte)
	 */
	private long dataLength;

	/**
	 * 波形データを書き込む
	 * 
	 * @param data
	 */
	private void addWaveform(double[] data) throws IOException {
		for (int i = 0; i < data.length; i++)
			dataStream.writeDouble(data[i]);
		dataLength += 8 * data.length;
	}

	/**
	 * @param basicID
	 *            StartByte will be ignored and set properly in the output file.
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public synchronized void addBasicID(BasicID basicID) throws IOException {
		if (basicID.waveFormType == WaveformType.PARTIAL)
			throw new RuntimeException(
					"This is a partial derivative. " + Thread.currentThread().getStackTrace()[1].getMethodName());
		if (partial)
			throw new RuntimeException("Partial data is already written in data files");
		obs = true;
		long startByte = dataLength;
		addWaveform(basicID.getData());

		switch (basicID.getWaveformType()) {
		case OBS:
			idStream.writeBoolean(true); // if it is obs 1Byte
			break;
		case SYN:
			idStream.writeBoolean(false); // if it is obs
			break;
		default:
			break;
		}
		idStream.writeBytes(StringUtils.rightPad(basicID.station.getStationName(), 8));
		idStream.writeBytes(StringUtils.rightPad(basicID.station.getNetwork(), 8));
		idStream.writeFloat((float)basicID.station.getPosition().getLatitude());
		idStream.writeFloat((float)basicID.station.getPosition().getLongitude());
		idStream.writeBytes(StringUtils.rightPad(basicID.globalCMTID.toString(), 15));
		switch (basicID.getSacComponent()) {
		case Z:
			idStream.writeByte(0); // 1 Byte
			break;
		case R:
			idStream.writeByte(1);
			break;
		case T:
			idStream.writeByte(2);
			break;
		default:
			break;
		}
		idStream.writeFloat((float) basicID.getMinPeriod()); // minimum period
		// 4Byte
		idStream.writeFloat((float) basicID.getMaxPeriod()); // maximum period
		// 4Byte
		idStream.writeFloat((float) basicID.getStartTime()); // start time 4Byte
		idStream.writeInt(basicID.getNpts()); // データポイント数 4Byte
		idStream.writeFloat((float) basicID.getSamplingHz()); // sampling Hz 4
		// Byte
		// convolutionされているか 観測波形なら true
		idStream.writeBoolean(basicID.getWaveformType() == WaveformType.OBS || basicID.isConvolved); // 1Byte
		idStream.writeLong(startByte); // データの格納場所 8 Byte

	}

	/**
	 * @param partialID
	 *            {@link PartialID} must contain waveform data. StartByte will
	 *            be ignored and set properly in the output file.
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public synchronized void addPartialID(PartialID partialID) throws IOException {
		if (partialID.waveFormType != WaveformType.PARTIAL)
			throw new RuntimeException(
					"This is not a partial derivative. " + Thread.currentThread().getStackTrace()[1].getMethodName());
		if (obs)
			throw new RuntimeException("Obs/Syn is already written in data files.");
		partial = true;

		long startByte = dataLength;
		addWaveform(partialID.getData());
		idStream.writeBytes(StringUtils.rightPad(partialID.station.getStationName(), 8));
		idStream.writeBytes(StringUtils.rightPad(partialID.station.getNetwork(), 8));
		idStream.writeFloat((float)partialID.station.getPosition().getLatitude());
		idStream.writeFloat((float)partialID.station.getPosition().getLongitude());
		idStream.writeBytes(StringUtils.rightPad(partialID.globalCMTID.toString(), 15));
		switch (partialID.sacComponent) {
		case Z:
			idStream.writeByte(0); // 1 Byte
			break;
		case R:
			idStream.writeByte(1);
			break;
		case T:
			idStream.writeByte(2);
			break;
		default:
			break;
		}
		idStream.writeFloat((float) partialID.minPeriod); // minimum period
															// 4Byte
		idStream.writeFloat((float) partialID.maxPeriod); // maximum period
															// 4Byte
		idStream.writeFloat((float) partialID.startTime); // start time 4 Byte
		idStream.writeInt(partialID.npts); // データポイント数 4 Byte
		idStream.writeFloat((float) partialID.samplingHz); // sampling Hz 4 Byte
		// convolutionされているか
		idStream.writeBoolean(partialID.isConvolved); // 1Byte
		idStream.writeLong(startByte); // データの格納場所 8 Byte
		// partial type 1 Byte
		idStream.writeByte(partialID.getPartialType().getValue());
		// latitude 4 Byte
		idStream.writeFloat((float) partialID.pointLocation.getLatitude());
		// longitude 4 Byte
		idStream.writeFloat((float) partialID.pointLocation.getLongitude());
		// radius 4 Byte
		idStream.writeFloat((float) (partialID.pointLocation.getR()));
	}
}
