package io.github.kensuke1984.kibrary.waveformdata;

import io.github.kensuke1984.kibrary.inversion.Physical3DParameter;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.anisotime.Phase;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utilities for a pair of an ID file and a waveform file. <br>
 * The files are for partial waveforms.
 * <p>
 * The file contains<br>
 * Numbers of stations, events, period ranges and perturbation points<br>
 * Each station information <br>
 * - name, network, position <br>
 * Each event <br>
 * - Global CMT ID Each period<br>
 * Each period range<br>
 * - min period, max period<br>
 * Each perturbation points<br>
 * - latitude, longitude, radius Each PartialID information<br>
 * - see in {@link #read(Path)}<br>
 * <p>
 * TODO short to char
 * READing has problem. TODO
 *
 * @author Kensuke Konishi
 * @version 0.3.2
 * @author anselme Added phase information
 */
public final class PartialIDFile {

    /**
     * [byte] File size for an ID
     */
    public static final int oneIDByte = 50;

    private PartialIDFile() {
    }

	public static PartialID[] read(Path idPath, Path dataPath, Predicate<PartialID> chooser)
			throws IOException {
		PartialID[] ids = read(idPath);
		long t = System.nanoTime();
		long dataSize = Files.size(dataPath);
		PartialID lastID = ids[ids.length - 1];
		if (dataSize != lastID.START_BYTE + lastID.NPTS * 8)
			throw new RuntimeException(dataPath + " is invalid for " + idPath);
		int counter = 0;
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(dataPath)))) {
			for (int i = 0; i < ids.length; i++) {
				if (!chooser.test(ids[i])) {
					dis.skipBytes(ids[i].NPTS * 8);
					ids[i] = null;
					continue;
				}
				double[] data = new double[ids[i].NPTS];
				for (int j = 0; j < data.length; j++)
					data[j] = dis.readDouble();
				ids[i] = ids[i].setData(data);
				if (i % (ids.length / 20) == 0)
                    System.err.print("\rReading partial data ... " + Math.ceil(i * 100.0 / ids.length) + " %");
			}
			System.err.println("\rReading partial data ... 100.0 %");
		}
		if (chooser != null) ids = Arrays.stream(ids).parallel().filter(Objects::nonNull).toArray(PartialID[]::new);
		System.err.println("Partial data are read in " + Utilities.toTimeString(System.nanoTime() - t));
		return ids;
	}
	
	public static PartialID[] read(PartialID[] idsNoData, Path dataPath, int[] partialIndexes, int[] cumulativeNPTS)
			throws IOException {
		long t = System.nanoTime();
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(dataPath)))) {
			dis.skipBytes(cumulativeNPTS[partialIndexes[0]] * 8);
			for (int i = 0; i < partialIndexes.length; i++) {
				double[] data = new double[idsNoData[i].NPTS];
				for (int j = 0; j < data.length; j++)
					data[j] = dis.readDouble();
				idsNoData[i] = idsNoData[i].setData(data);
				
				if (i < partialIndexes.length - 1)
					dis.skipBytes((cumulativeNPTS[partialIndexes[i+1]] - cumulativeNPTS[partialIndexes[i] + 1]) * 8);
			}
		}
		System.err.println("Partial waveforms are read in " + Utilities.toTimeString(System.nanoTime() - t));
		return idsNoData;
	}

    /**
     * @param idPath {@link Path} of an ID file.
     * @return Array of {@link PartialID} without waveform data
     * @throws IOException if an I/O error occurs
     */
	public static PartialID[] read(Path idPath) throws IOException {
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(idPath)))) {
			long fileSize = Files.size(idPath);
			Station[] stations = new Station[dis.readShort()];
			GlobalCMTID[] cmtIDs = new GlobalCMTID[dis.readShort()];
			double[][] periodRanges = new double[dis.readShort()][2];
			Phase[] phases = new Phase[dis.readShort()];
			Location[] perturbationLocations = new Location[dis.readShort()];
			// 4 * short 
//			int headerBytes = 5 * 2 + 24 * stations.length + 15 * cmtIDs.length + 4 * 2 * periodRanges.length
//					+ 16 * phases.length + 4 * 3 * perturbationLocations.length;
			int headerBytes = 5 * 2 + (8 + 8 + 8 * 2) * stations.length + 15 * cmtIDs.length + 8 * 2 * periodRanges.length
					+ 16 * phases.length + 8 * 3 * perturbationLocations.length;
			long idParts = fileSize - headerBytes;
			if (idParts % oneIDByte != 0)
				throw new RuntimeException(idPath + " is not valid..");
			// name(8),network(8),position(8*2)
			byte[] stationBytes = new byte[32];
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
				periodRanges[i][0] = dis.readDouble();
				periodRanges[i][1] = dis.readDouble();
			}
			byte[] phaseBytes = new byte[16];
			for (int i = 0; i < phases.length; i++) {
				dis.read(phaseBytes);
				phases[i] = Phase.create(new String(phaseBytes).trim());
			}
			for (int i = 0; i < perturbationLocations.length; i++) {
//				perturbationLocations[i] = new Location(dis.readFloat(), dis.readFloat(), dis.readFloat());
				perturbationLocations[i] = new Location(dis.readDouble(), dis.readDouble(), dis.readDouble());
			}
			int nid = (int) (idParts / oneIDByte);
			System.err.println("Reading partialID file: " + idPath);
			
			long t = System.nanoTime();
			byte[][] bytes = new byte[nid][oneIDByte];
			System.out.println(nid);
			for (int i = 0; i < nid; i++)
				dis.read(bytes[i]);
			PartialID[] ids = new PartialID[nid];
			IntStream.range(0, nid).parallel()
				.forEach(i -> ids[i] = createID(bytes[i], stations, cmtIDs, periodRanges, phases, perturbationLocations));
			System.err
					.println(ids.length + " partial IDs are read in " + Utilities.toTimeString(System.nanoTime() - t));
			return ids;
		}
	}

	/**
	 * Creates lists of stations, events, partials.(if they don't exist) Options:
	 * -a: show all IDs
	 * --debug: create debug files
	 * 
	 * @param args [options] [parameter file name]
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			PartialID[] ids = read(Paths.get(args[0]));
			String header = FilenameUtils.getBaseName(Paths.get(args[0]).getFileName().toString());
			outputStations(header, ids);
			outputGlobalCMTID(header, ids);
			outputPerturbationPoints(header, ids);
		} else if (args.length == 2 && args[0].equals("-a")) {
			PartialID[] ids = read(Paths.get(args[1]));
			Arrays.stream(ids).forEach(System.out::println);
		} else if (args.length == 2 && args[0].equals("--debug")) {
			PartialID[] ids = read(Paths.get(args[1]));
			Set<PartialType> types = new HashSet<>();
			for (PartialID id : ids)
				types.add(id.getPartialType());
			for (PartialType type : types) {
				List<StationEvent> tmpList = Arrays.stream(ids).parallel().filter(id -> id.getPartialType().equals(type))
						.map(id -> new StationEvent(id.getStation(), id.getGlobalCMTID(), id.getStartTime()))
						.distinct().collect(Collectors.toList());
				Collections.sort(tmpList);
				Path outPath = Paths.get(type + ".inf");
				try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath))) {
					tmpList.forEach(tmp -> pw.println(tmp));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("usage:[-a | --debug] [id file name]\n if \"-a\", show all IDs\n if \"--debug\", makes station-event list for all partial types");
		}
	}

    private static void outputPerturbationPoints(String header, PartialID[] pids) throws IOException {
        Path outPath = Paths.get(header + ".par");
        if (Files.exists(outPath)) return;
        List<String> lines =
                Arrays.stream(pids).parallel().map(id -> new Physical3DParameter(id.PARTIAL_TYPE, id.POINT_LOCATION, 1))
                        .distinct().map(Physical3DParameter::toString).sorted().collect(Collectors.toList());
        Files.write(outPath, lines);
        System.err.println(outPath + " is created as a list of perturbation. (weighting values are just set 1)");
    }

    private static void outputStations(String header, PartialID[] ids) throws IOException {
        Path outPath = Paths.get(header + ".station");
        if (Files.exists(outPath)) return;
        List<String> lines = Arrays.stream(ids).parallel().map(id -> id.STATION).distinct()
                .map(s -> s.getName() + " " + s.getNetwork() + " " + s.getPosition()).collect(Collectors.toList());
        Files.write(outPath, lines);
        System.err.println(outPath + " is created as a list of stations.");
    }

	/**
	 * An ID information contains<br>
	 * station number(2)<br>
	 * event number(2)<br>
	 * component(1)<br>
	 * period range(1) <br>
	 * phases numbers (10*2)<br>
	 * start time(4)<br>
	 * number of points(4)<br>
	 * sampling hz(4) <br>
	 * convoluted(or observed) or not(1)<br>
	 * position of a waveform for the ID in the datafile(8)<br>
	 * type of partial(1)<br>
	 * point of perturbation(2)
	 * 
	 * @param bytes
	 *            for one ID
	 * @return an ID written in the bytes
	 */
	private static PartialID createID(byte[] bytes, Station[] stations, GlobalCMTID[] ids, double[][] periodRanges,
			 Phase[] phases, Location[] perturbationLocations) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		Station station = stations[bb.getShort()];
		GlobalCMTID eventID = ids[bb.getShort()];
		SACComponent component = SACComponent.getComponent(bb.get());
		double[] period = periodRanges[bb.get()];
		Set<Phase> tmpset = new HashSet<>();
		for (int i = 0; i < 10; i++) {
			short iphase = bb.getShort();
			if (iphase != -1)
				tmpset.add(phases[iphase]);
		}
		Phase[] usablephases = new Phase[tmpset.size()];
		usablephases = tmpset.toArray(usablephases);
		double startTime = bb.getFloat(); // starting time
		int npts = bb.getInt(); // データポイント数
		double samplingHz = bb.getFloat();
		boolean isConvolved = 0 < bb.get();
		long startByte = bb.getLong();
		PartialType partialType = PartialType.getType(bb.get());
		Location perturbationLocation = perturbationLocations[bb.getShort()];
		return new PartialID(station, eventID, component, samplingHz, startTime, npts, period[0], period[1],
				usablephases, startByte, isConvolved, perturbationLocation, partialType);
	}

    public static PartialID[] read(Path idPath, Path dataPath) throws IOException {
        return read(idPath, dataPath, id -> true);
    }

    private static void outputGlobalCMTID(String header, PartialID[] ids) throws IOException {
        Path outPath = Paths.get(header + ".globalCMTID");
        if (Files.exists(outPath)) return;
        List<String> lines = Arrays.stream(ids).parallel().map(id -> id.ID.toString()).distinct().sorted()
                .collect(Collectors.toList());
        Files.write(outPath, lines);
        System.err.println(outPath + " is created as a list of global CMT IDs.");
    }

	/**
	 * @author anselme
	 * Static class for debug informations
	 */
	public static class StationEvent implements Comparable<StationEvent> {
		public Station station;
		public GlobalCMTID event;
		public double startTime;
		public StationEvent(Station station, GlobalCMTID event, double startTime) {
			this.station = station;
			this.event = event;
			this.startTime = startTime;
		}
		@Override
		public int compareTo(StationEvent o) {
			int compareStation = station.compareTo(o.station);
			if (compareStation != 0)
				return compareStation;
			else if (event.compareTo(o.event) != 0)
				return event.compareTo(o.event);
			else
				return Double.compare(this.startTime, o.startTime);
		}
		@Override
		public String toString() {
			return station.toString() + " " + event.toString() + " " + String.format("%.2f", startTime);
		}
		@Override
		public int hashCode() {
			return station.hashCode() * event.hashCode() * 31 * (int) startTime;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StationEvent other = (StationEvent) obj;
			double otherStartTime = other.startTime;
			if (!station.equals(other.station))
				return false;
			if (!event.equals(other.event))
				return false;
			if (Math.abs(startTime - otherStartTime) > 0.1)
				return false;
			return true;
		}
	}
}
