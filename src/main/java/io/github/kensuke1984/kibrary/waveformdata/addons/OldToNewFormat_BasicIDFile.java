package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.IntStream;

public class OldToNewFormat_BasicIDFile {

	public static final int oneIDByte_old = 28;
	
	public static void main(String[] args) {
		
	}
	
	public static BasicID[] readBasicIDFile_old(Path idPath) throws IOException {
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(idPath)))) {
			long t = System.nanoTime();
			long fileSize = Files.size(idPath);
			// Read header
			Station[] stations = new Station[dis.readShort()];
			GlobalCMTID[] cmtIDs = new GlobalCMTID[dis.readShort()];
			double[][] periodRanges = new double[dis.readShort()][2];
			int headerBytes = 2 * 3 + (8 + 8 + 4 * 2) * stations.length + 15 * cmtIDs.length
					+ 4 * 2 * periodRanges.length;
			long idParts = fileSize - headerBytes;
			if (idParts % oneIDByte_old != 0)
				throw new RuntimeException(idPath + " is not valid..");
			// name(8),network(8),position(4*2)
			byte[] stationBytes = new byte[24];
			for (int i = 0; i < stations.length; i++) {
				dis.read(stationBytes);
				stations[i] = Station.createStation(stationBytes);
			}
			byte[] cmtIDBytes = new byte[15];
			for (int i = 0; i < cmtIDs.length; i++) {
				dis.read(cmtIDBytes);
				cmtIDs[i] = new GlobalCMTID(new String(cmtIDBytes).trim());
			}
			for (int i = 0; i < periodRanges.length; i++) {
				periodRanges[i][0] = dis.readFloat();
				periodRanges[i][1] = dis.readFloat();
			}

			int nid = (int) (idParts / oneIDByte_old);
			BasicID[] ids = new BasicID[nid];
			byte[][] bytes = new byte[nid][oneIDByte_old];
			for (int i = 0; i < nid; i++)
				dis.read(bytes[i]);
			IntStream.range(0, nid).parallel().forEach(i -> {
				ids[i] = createID_old(bytes[i], stations, cmtIDs, periodRanges);
			});
			System.err.println(
					"Reading " + ids.length + " basic IDs done in " + Utilities.toTimeString(System.nanoTime() - t));
			return ids;
		}
	}
	
	public static BasicID[] readBasicIDandDataFile_old(Path idPath, Path dataPath) throws IOException {
		BasicID[] ids = readBasicIDFile_old(idPath);
		long dataSize = Files.size(dataPath);
		long t = System.nanoTime();
		BasicID lastID = ids[ids.length - 1];
		if (dataSize != lastID.START_BYTE + lastID.NPTS * 8)
			throw new RuntimeException(dataPath + " is not invalid for " + idPath);
		try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(dataPath))) {
			byte[][] bytes = new byte[ids.length][];
			Arrays.parallelSetAll(bytes, i -> new byte[ids[i].NPTS * 8]);
			for (int i = 0; i < ids.length; i++)
				bis.read(bytes[i]);
			IntStream.range(0, ids.length).parallel().forEach(i -> {
				BasicID id = ids[i];
				ByteBuffer bb = ByteBuffer.wrap(bytes[i]);
				double[] data = new double[id.NPTS];
				for (int j = 0; j < data.length; j++)
					data[j] = bb.getDouble();
				ids[i] = id.setData(data);
			});
		}
		System.err.println("Reading waveform done in " + Utilities.toTimeString(System.nanoTime() - t));
		return ids;
	}
	
	/**
	 * An ID information contains<br>
	 * obs or syn(1)<br>
	 * station number(2)<br>
	 * event number(2)<br>
	 * component(1)<br>
	 * period range(1) <br>
	 * start time(4)<br>
	 * number of points(4)<br>
	 * sampling hz(4) <br>
	 * convoluted(or observed) or not(1)<br>
	 * position of a waveform for the ID in the datafile(8)
	 * 
	 * @param bytes
	 *            for one ID
	 * @return an ID written in the bytes
	 */
	private static BasicID createID_old(byte[] bytes, Station[] stations, GlobalCMTID[] ids, double[][] periodRanges) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		WaveformType type = 0 < bb.get() ? WaveformType.OBS : WaveformType.SYN;
		Station station = stations[bb.getShort()];
		GlobalCMTID id = ids[bb.getShort()];
		SACComponent component = SACComponent.getComponent(bb.get());
		double[] period = periodRanges[bb.get()];
		double startTime = bb.getFloat(); // starting time
		int npts = bb.getInt(); // データポイント数
		double samplingHz = bb.getFloat();
		boolean isConvolved = 0 < bb.get();
		long startByte = bb.getLong();
		Phase[] phases = null;
		BasicID bid = new BasicID(type, samplingHz, startTime, npts, station, id, component, period[0], period[1]
				, phases, startByte, isConvolved);
		return bid;
	}
}
