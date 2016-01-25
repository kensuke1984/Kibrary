package io.github.kensuke1984.kibrary.waveformdata;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FilenameUtils;

import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;

/**
 * Utilities for a pair of an ID file and a waveform file. The files are for
 * observed and synthetic waveforms (NOT partial)<br>
 * 
 * The file contains<br>
 * Numbers of stations, events and period ranges <br>
 * Each station information <br>
 * - name, network, position <br>
 * Each event <br>
 * - Global CMT ID Each period<br>
 * Each period range<br>
 * - min period, max period<br>
 * Each BasicID information<br>
 * - see in {@link #readBasicIDFile(Path)}<br>
 * 
 * 
 * @see {@link BasicID}
 * 
 * @since 2013/12/1 or earlier
 * 
 * @version 0.3
 * 
 * @author kensuke
 * 
 */
public final class BasicIDFile {
	/**
	 * File size for an ID
	 */
	public static final int oneIDByte = 28;

	private BasicIDFile() {
	}

	private static void outputGlobalCMTID(String header, BasicID[] ids) throws IOException {
		Path outPath = Paths.get(header + ".globalCMTID");
		List<String> lines = Arrays.stream(ids).parallel().map(id -> id.globalCMTID.toString()).distinct().sorted()
				.collect(Collectors.toList());
		Files.write(outPath, lines, StandardOpenOption.CREATE_NEW);
		System.out.println(outPath + " is created as a list of global CMT IDs.");
	}

	private static void outputStations(String header, BasicID[] ids) throws IOException {
		Path outPath = Paths.get(header + ".station");
		List<String> lines = Arrays.stream(ids).parallel().map(id -> id.station).distinct().sorted()
				.map(s -> s.getStationName() + " " + s.getNetwork() + " " + s.getPosition())
				.collect(Collectors.toList());
		Files.write(outPath, lines, StandardOpenOption.CREATE_NEW);
		System.out.println(outPath + " is created as a list of stations.");
	}

	/**
	 * Options: -a: show all IDs
	 * 
	 * @param args
	 *            [option] [id file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			BasicID[] ids = readBasicIDFile(Paths.get(args[0]));
			// print(Paths.get(args[0]));
			String header = FilenameUtils.getBaseName(Paths.get(args[0]).getFileName().toString());
			try {
				outputStations(header, ids);
				outputGlobalCMTID(header, ids);
			} catch (Exception e) {
				System.err.println("Could not write information about " + args[0]);
				System.err.println("If you want to see all IDs inside, then use a '-a' option.");
			}
		} else if (args.length == 2 && args[0].equals("-a")) {
			BasicID[] ids = readBasicIDFile(Paths.get(args[1]));
			Arrays.stream(ids).forEach(System.out::println);
		} else {
			System.err.println("usage:[-a] [id file name]\n if \"-a\", show all IDs");
		}
	}

	/**
	 * @param idPath
	 *            {@link Path} of an ID file, if it does not exist, an
	 *            IOException
	 * @param dataPath
	 *            {@link Path} of an data file, if it does not exist, an
	 *            IOException
	 * @return Array of {@link BasicID} containing waveform data
	 * @throws IOException
	 *             if an I/O error happens,
	 */
	public static BasicID[] readBasicIDandDataFile(Path idPath, Path dataPath) throws IOException {
		BasicID[] ids = readBasicIDFile(idPath);
		long dataSize = Files.size(dataPath);
		long t = System.nanoTime();
		BasicID lastID = ids[ids.length - 1];
		if (dataSize != lastID.startByte + lastID.npts * 8)
			throw new RuntimeException(dataPath + " is not invalid for " + idPath);
		try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(dataPath))) {
			byte[][] bytes = new byte[ids.length][];
			Arrays.parallelSetAll(bytes, i -> new byte[ids[i].npts * 8]);
			for (int i = 0; i < ids.length; i++)
				bis.read(bytes[i]);
			IntStream.range(0, ids.length).parallel().forEach(i -> {
				BasicID id = ids[i];
				double[] data = new double[id.npts];
				ByteBuffer bb = ByteBuffer.wrap(bytes[i]);
				for (int j = 0; j < data.length; j++)
					data[j] = bb.getDouble();
				ids[i] = id.setData(data);
			});
		}
		System.err.println("Reading waveform done in " + Utilities.toTimeString(System.nanoTime() - t));
		return ids;
	}

	/**
	 * @param idPath
	 *            {@link Path} of an ID file
	 * @return Array of {@link BasicID} without waveform data
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static BasicID[] readBasicIDFile(Path idPath) throws IOException {
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
			if (idParts % oneIDByte != 0)
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

			int nid = (int) (idParts / oneIDByte);
			BasicID[] ids = new BasicID[nid];
			byte[][] bytes = new byte[nid][oneIDByte];
			for (int i = 0; i < nid; i++)
				dis.read(bytes[i]);
			IntStream.range(0, nid).parallel().forEach(i -> {
				ids[i] = createID(bytes[i], stations, cmtIDs, periodRanges);
			});
			System.err.println(
					"Reading " + ids.length + " basic IDs done in " + Utilities.toTimeString(System.nanoTime() - t));
			return ids;
		}
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
	private static BasicID createID(byte[] bytes, Station[] stations, GlobalCMTID[] ids, double[][] periodRanges) {
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
		BasicID bid = new BasicID(type, samplingHz, startTime, npts, station, id, component, period[0], period[1],
				startByte, isConvolved);
		return bid;
	}
}
