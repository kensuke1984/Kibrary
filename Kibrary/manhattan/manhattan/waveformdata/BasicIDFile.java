package manhattan.waveformdata;

import java.io.BufferedInputStream;
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

import filehandling.sac.SACComponent;
import filehandling.sac.WaveformType;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.HorizontalPosition;
import manhattan.template.Station;
import manhattan.template.Utilities;

/**
 * Utilities for a pair of an ID file and a waveform file. The files are for
 * observed and synthetic waveforms (NOT partial)
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
		long fileSize = Files.size(idPath);
		if (fileSize % BasicID.oneIDbyte != 0)
			throw new RuntimeException(idPath + " is not valid..");
		long t = System.nanoTime();
		int nid = (int) (fileSize / BasicID.oneIDbyte);
		BasicID[] ids = new BasicID[nid];
		byte[][] bytes = new byte[nid][BasicID.oneIDbyte];
		try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(idPath))) {
			for (int i = 0; i < nid; i++)
				bis.read(bytes[i]);
		}
		IntStream.range(0, nid).parallel().forEach(i -> {
			ids[i] = createID(bytes[i]);
		});
		System.err.println(
				"Reading " + ids.length + " basic IDs done in " + Utilities.toTimeString(System.nanoTime() - t));
		return ids;
	}

	private static BasicID createID(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		WaveformType type = 0 < bb.get() ? WaveformType.OBS : WaveformType.SYN;

		byte[] stationName = new byte[8];
		bb.get(stationName);
		byte[] networkName = new byte[8];
		bb.get(networkName);
		String sta = new String(stationName).trim();
		String network = new String(networkName).trim();
		double latitude = bb.getFloat();
		double longitude = bb.getFloat();
		Station station = new Station(sta, new HorizontalPosition(latitude, longitude), network);
		byte[] eventNameByte = new byte[15];
		bb.get(eventNameByte);
		GlobalCMTID eventName = new GlobalCMTID(new String(eventNameByte).trim());
		SACComponent component = null;
		switch (bb.get()) {
		case 0:
			component = SACComponent.Z;
			break;
		case 1:
			component = SACComponent.R;
			break;
		case 2:
			component = SACComponent.T;
			break;
		default:
			throw new RuntimeException("Error occured in reading");
		}
		double minPeriod = bb.getFloat(); // minimum period
		double maxPeriod = bb.getFloat(); // max period
		double startTime = bb.getFloat(); // starting time
		int npts = bb.getInt(); // データポイント数
		double samplingHz = bb.getFloat();
		boolean isConvolved = 0 < bb.get();
		long startByte = bb.getLong();
		BasicID bid = new BasicID(type, samplingHz, startTime, npts, station, eventName, component, minPeriod,
				maxPeriod, startByte, isConvolved);
		return bid;
	}

}
