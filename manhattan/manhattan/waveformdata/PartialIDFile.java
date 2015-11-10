package manhattan.waveformdata;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FilenameUtils;

import filehandling.sac.SACComponent;
import filehandling.spc.PartialType;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.inversion.ElasticParameter;
import manhattan.template.HorizontalPosition;
import manhattan.template.Location;
import manhattan.template.Station;
import manhattan.template.Utilities;

/**
 * Utilities for a pair of an ID file and a waveform file. <br>
 * The files are for partial waveforms.
 * 
 * @since 2013/12/1 or earlier
 * 
 * @version 0.3
 * 
 * @author Kensuke
 * 
 * 
 */
public final class PartialIDFile {

	public static PartialID[] readPartialIDandDataFile(Path idPath, Path dataPath, Predicate<PartialID> chooser)
			throws IOException {
		PartialID[] ids = readPartialIDFile(idPath);
		long t = System.nanoTime();
		long dataSize = Files.size(dataPath);
		PartialID lastID = ids[ids.length - 1];
		if (dataSize != lastID.startByte + lastID.npts * 8)
			throw new RuntimeException(dataPath + " is not invalid for " + idPath);
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(dataPath)))) {
			for (int i = 0; i < ids.length; i++) {
				if (!chooser.test(ids[i])) {
					dis.skipBytes(ids[i].npts * 8);
					ids[i] = null;
					continue;
				}
				double[] data = new double[ids[i].npts];
				for (int j = 0; j < data.length; j++)
					data[j] = dis.readDouble();
				ids[i] = ids[i].setData(data);
			}
		}
		if (chooser != null)
			ids = Arrays.stream(ids).parallel().filter(Objects::nonNull).toArray(n -> new PartialID[n]);
		System.err.println("Partial waveforms are read in " + Utilities.toTimeString(System.nanoTime() - t));
		return ids;
	}

	/**
	 * @param idPath
	 *            {@link Path} of an ID file.
	 * @return Array of {@link PartialID} without waveform data
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static PartialID[] readPartialIDFile(Path idPath) throws IOException {
		long fileSize = Files.size(idPath);
		if (fileSize % PartialID.oneIDbyte != 0)
			throw new RuntimeException(idPath + " is not valid..");
		int nid = (int) (fileSize / PartialID.oneIDbyte);
		System.err.println("Reading partialID file: " + idPath);
		long t = System.nanoTime();
		byte[][] bytes = new byte[nid][PartialID.oneIDbyte];
		try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(idPath));) {
			for (int i = 0; i < nid; i++)
				bis.read(bytes[i]);
		}
		PartialID[] ids = new PartialID[nid];
		IntStream.range(0, nid).parallel().forEach(i -> {
			ids[i] = createID(bytes[i]);
		});
		System.err.println(ids.length + " partial IDs are read in " + Utilities.toTimeString(System.nanoTime() - t));
		return ids;
	}

	/**
	 * Creates lists of stations, events, partials.(if they dont exist) Options:
	 * -a: show all IDs
	 * 
	 * @param args
	 *            [options] [parameter file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			PartialID[] ids = readPartialIDFile(Paths.get(args[0]));
			// print(Paths.get(args[0]));
			String header = FilenameUtils.getBaseName(Paths.get(args[0]).getFileName().toString());
			outputStations(header, ids);
			outputGlobalCMTID(header, ids);
			outputPerturbationPoints(header, ids);
		} else if (args.length == 2 && args[0].equals("-a")) {
			PartialID[] ids = readPartialIDFile(Paths.get(args[1]));
			Arrays.stream(ids).forEach(System.out::println);
		} else {
			System.out.println("usage:[-a] [id file name]\n if \"-a\", show all IDs");
		}

	}

	private static void outputPerturbationPoints(String header, PartialID[] pids) throws IOException {
		Path outPath = Paths.get(header + ".par");
		if (Files.exists(outPath))
			return;
		List<String> lines = Arrays.stream(pids).parallel()
				.map(id -> new ElasticParameter(id.partialType, id.pointLocation, 1)).distinct()
				.map(ep -> ep.toString()).sorted().collect(Collectors.toList());
		Files.write(outPath, lines);
		System.out.println(outPath + " is created as a list of perturbation. (weighting values are just set 1)");
	}

	private static void outputStations(String header, PartialID[] ids) throws IOException {
		Path outPath = Paths.get(header + ".station");
		if (Files.exists(outPath))
			return;
		List<String> lines = Arrays.stream(ids).parallel().map(id -> id.station).distinct().sorted()
				.map(s -> s.getStationName() + " " + s.getNetwork() + " " + s.getPosition())
				.collect(Collectors.toList());
		Files.write(outPath, lines);
		System.out.println(outPath + " is created as a list of stations.");
	}

	private static PartialID createID(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
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
		GlobalCMTID eventID = new GlobalCMTID(new String(eventNameByte).trim());
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
			throw new RuntimeException("Error occured in reading ");
		}
		double minPeriod = bb.getFloat(); // minimum period
		double maxPeriod = bb.getFloat(); // max period
		double startTime = bb.getFloat(); // starting time
		int npts = bb.getInt(); // データポイント数
		double samplingHz = bb.getFloat();
		boolean isConvolved = 0 < bb.get();
		long startByte = bb.getLong();
		PartialType partialType = PartialType.getType(bb.get());
		Location perturbationLocation = new Location(bb.getFloat(), bb.getFloat(), bb.getFloat());
		PartialID id = new PartialID(station, eventID, component, samplingHz, startTime, npts, minPeriod, maxPeriod,
				startByte, isConvolved, perturbationLocation, partialType);
		return id;
	}

	public static PartialID[] readPartialIDandDataFile(Path idPath, Path dataPath) throws IOException {
		return readPartialIDandDataFile(idPath, dataPath, id -> true);
	}

	private PartialIDFile() {
	}

	private static void outputGlobalCMTID(String header, PartialID[] ids) throws IOException {
		Path outPath = Paths.get(header + ".globalCMTID");
		if (Files.exists(outPath))
			return;
		List<String> lines = Arrays.stream(ids).parallel().map(id -> id.globalCMTID.toString()).distinct().sorted()
				.collect(Collectors.toList());
		Files.write(outPath, lines);
		System.out.println(outPath + " is created as a list of global CMT IDs.");
	}

}
