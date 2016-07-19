package io.github.kensuke1984.kibrary.datacorrection;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.TransformType;

import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Trace;

/**
 * This class is <b>IMMUTABLE</b>.
 * 
 * Information of a seismic event in
 * <a href="http://scardec.projects.sismo.ipgp.fr/">SCARDEC</a>.
 * 
 * @author kensuke
 * @version 0.0.2
 * 
 * @see <a href="http://scardec.projects.sismo.ipgp.fr/">SCARDEC</a>,
 *      <a href="http://earthquake.usgs.gov/contactus/golden/neic.php">NEIC</a>
 */
public class SCARDEC{
	
	/**
	 * @return origin time 
	 */
	public LocalDateTime getOriginTime(){
		return ID.ORIGIN_TIME;
	}
	
	/**
	 * @return String of the region
	 */
	public String getRegion(){
		return ID.REGION;
	}

	/**
	 * Prints out all the events in the catalog.
	 */
	public static void printList() {
		EXISTING_ID.stream().sorted().forEach(System.out::println);
	}

	/**
	 * ID for information
	 * 
	 * @author KENSUKE KONISHI
	 */
	public static class SCARDEC_ID implements Comparable<SCARDEC_ID> {

		@Override
		public String toString() {
			return "Origin time:" + ORIGIN_TIME + ", Region:" + REGION;
		}

		/**
		 * Origin time from <a href=
		 * "http://earthquake.usgs.gov/contactus/golden/neic.php">NEIC</a>
		 */
		private final LocalDateTime ORIGIN_TIME;

		private final String REGION;

		public SCARDEC_ID(LocalDateTime origin, String region) {
			ORIGIN_TIME = origin;
			REGION = region;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((ORIGIN_TIME == null) ? 0 : ORIGIN_TIME.hashCode());
			result = prime * result + ((REGION == null) ? 0 : REGION.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SCARDEC_ID other = (SCARDEC_ID) obj;
			if (ORIGIN_TIME == null) {
				if (other.ORIGIN_TIME != null)
					return false;
			} else if (!ORIGIN_TIME.equals(other.ORIGIN_TIME))
				return false;
			if (REGION == null) {
				if (other.REGION != null)
					return false;
			} else if (!REGION.equals(other.REGION))
				return false;
			return true;
		}

		private String getDateTimeString() {
			return ORIGIN_TIME.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		}

		/**
		 * @param string
		 *            must be in the form yyyyMMdd_HHmmss
		 * @return SCARDEC_ID for the string
		 */
		private static SCARDEC_ID of(String string) {
			int index = string.indexOf("_");
			String dateTime = string.substring(index + 1, index + 1 + 15);
			String region = string.substring(index + 1 + 16);
			return new SCARDEC_ID(LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
					region);
		}

		/**
		 * @return origin time
		 */
		public LocalDateTime getOriginTime() {
			return ORIGIN_TIME;
		}

		/**
		 * @return string for the region
		 */
		public String getRegion() {
			return REGION;
		}

		@Override
		public int compareTo(SCARDEC_ID o) {
			int c = ORIGIN_TIME.compareTo(o.ORIGIN_TIME);
			return c != 0 ? c : REGION.compareTo(o.REGION);
		}
	}

	private static final URL SCARDEC_ROOT_PATH = SCARDEC.class.getClassLoader().getResource("scardec_20141231.zip");

	private static final Set<SCARDEC_ID> EXISTING_ID = Collections.synchronizedSet(new HashSet<>());

	static {
		try (ZipInputStream zis = new ZipInputStream(SCARDEC_ROOT_PATH.openStream())) {
			ZipEntry entry;

			while ((entry = zis.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					Path path = Paths.get(entry.getName());
					try {
						SCARDEC_ID id = SCARDEC_ID.of(path.getFileName().toString());
						EXISTING_ID.add(id);
					} catch (Exception e) {
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Exception occured in reading a SCARDEC catalog.");
		}
	}

	public static SCARDEC getOPT(SCARDEC_ID id) {
		if (!EXISTING_ID.contains(id))
			throw new RuntimeException("No information for " + id.ORIGIN_TIME + " " + id.REGION);

		try (ZipInputStream zis = new ZipInputStream(SCARDEC_ROOT_PATH.openStream())) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				String path = Paths.get(entry.getName()).toString();
				if (entry.isDirectory())
					continue;
				if (!path.contains(id.REGION) || !path.contains(id.getDateTimeString()))
					continue;
				if (path.contains("optsource"))
					return readBinary(zis, id.REGION);
			}
			throw new RuntimeException("UNEKSPECTED");
		} catch (Exception e) {
			throw new RuntimeException("Exception in reading the SCARDEC archive.");
		}
	}

	/**
	 * @param predicate
	 *            Filter for IDs
	 * @return Set of IDs in the cache
	 */
	public static Set<SCARDEC_ID> search(Predicate<SCARDEC_ID> predicate) {
		return EXISTING_ID.stream().filter(predicate).collect(Collectors.toSet());
	}

	/**
	 * Choose one ID from candidates satisfying the predicate.
	 * 
	 * @param predicate
	 *            filter for IDs
	 * @return SCARDEC_ID chosen from IDs satisfying the predicate.
	 */
	public static SCARDEC_ID pick(Predicate<SCARDEC_ID> predicate) {
		SCARDEC_ID[] ids = EXISTING_ID.stream().filter(predicate).sorted().toArray(SCARDEC_ID[]::new);
		if (ids.length == 0)
			throw new RuntimeException("No ID matches.");
		if (ids.length == 1)
			return ids[0];
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new CloseShieldInputStream(System.in)))) {
			System.out.println("Which ID do you want to use?");
			System.out.println("There are several candidates. Choose one.");
			for (int i = 0; i < ids.length; i++)
				System.out.println((i + 1) + " " + ids[i]);
			int k = -1;
			while (k < 0) {
				String numStr = br.readLine();
				if (NumberUtils.isNumber(numStr))
					k = Integer.parseInt(numStr);
				if (k < 1 || ids.length <= k - 1) {
					System.out.println("... which one? " + 0 + " - " + (ids.length - 1));
					k = -1;
				}
			}
			System.err.println(ids[k - 1] + " is chosen.");
			return ids[k - 1];
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static SCARDEC getMOY(SCARDEC_ID id) {
		if (!EXISTING_ID.contains(id))
			throw new RuntimeException("No information for " + id.ORIGIN_TIME + " " + id.REGION);

		try (ZipInputStream zis = new ZipInputStream(SCARDEC_ROOT_PATH.openStream())) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				String path = Paths.get(entry.getName()).toString();
				if (entry.isDirectory())
					continue;
				if (!path.contains(id.REGION) || !path.contains(id.getDateTimeString()))
					continue;
				if (path.contains("moysource"))
					return readBinary(zis, id.REGION);
			}
			throw new RuntimeException("UNEKSPECTED");
		} catch (Exception e) {
			throw new RuntimeException("Exception in reading the SCARDEC archive.");
		}
	}

	/**
	 * Interpolate to the samplingHz 20. and creates {@link SourceTimeFunction}
	 * 
	 * @param np
	 *            steps of frequency [should be same as synthetics].
	 * @param tlen
	 *            [s] length of waveform [should be same as synthetics]
	 * @return {@link SourceTimeFunction} for this.
	 */
	public SourceTimeFunction toSourceTimeFunction(int np, double tlen) {
		double samplingHz = 20;
		double start = MOMENT_RATE_FUNCTION.getXAt(0);
		double end = MOMENT_RATE_FUNCTION.getXAt(MOMENT_RATE_FUNCTION.getLength() - 1);
		if (!SourceTimeFunction.checkValues(np, tlen, samplingHz))
			throw new IllegalArgumentException();
		int nptsInTime = (int) (tlen * samplingHz);
		double[] stfForFFT = new double[nptsInTime];
		double deltaT = 1 / samplingHz;

		double stfSize = 0;
		for (int i = 0; i < stfForFFT.length; i++) {
			double t = i * deltaT;
			if (t < end) {
				stfForFFT[i] = MOMENT_RATE_FUNCTION.toValue(2, t);
				stfSize += stfForFFT[i];
			} else if (start < t - tlen) {
				stfForFFT[i] = MOMENT_RATE_FUNCTION.toValue(2, t - tlen);
				stfSize += stfForFFT[i];
			}
		}

		for (int i = 0; i < stfForFFT.length; i++)
			stfForFFT[i] /= stfSize;

		Complex[] stfFreq = SourceTimeFunction.fft.transform(stfForFFT, TransformType.FORWARD);

		// consider np
		Complex[] cutSTF = new Complex[np];
		System.arraycopy(stfFreq, 0, cutSTF, 0, 1024);

		SourceTimeFunction stf = new SourceTimeFunction(np, tlen, samplingHz) {
			@Override
			public Complex[] getSourceTimeFunctionInFrequencyDomain() {
				return sourceTimeFunction;
			}
		};
		stf.sourceTimeFunction = cutSTF;
		return stf;
	}

	private SCARDEC(SCARDEC_ID id, Location epicentralLocation, double m0, double mw, double strike1, double dip1,
			double rake1, double strike2, double dip2, double rake2, Trace momentRate) {
		EPICENTRAL_LOCATION = epicentralLocation;
		DIP1 = dip1;
		DIP2 = dip2;
		STRIKE1 = strike1;
		STRIKE2 = strike2;
		RAKE1 = rake1;
		RAKE2 = rake2;
		M0 = m0;
		MW = mw;
		ID = id;
		MOMENT_RATE_FUNCTION = momentRate;
	}

	private final SCARDEC_ID ID;

	/**
	 * Hypocenter is from
	 * <a href="http://earthquake.usgs.gov/contactus/golden/neic.php">NEIC</a>.
	 * Depth is from
	 * <a href="http://scardec.projects.sismo.ipgp.fr/">SCARDEC</a>
	 */
	private final Location EPICENTRAL_LOCATION;

	private final double MW;

	/**
	 * [N&middot;m]
	 */
	private final double M0;

	// deg
	private final double STRIKE1;
	private final double DIP1;
	private final double RAKE1;
	private final double STRIKE2;
	private final double DIP2;
	private final double RAKE2;

	/**
	 * Trace of moment rate(x: time[s], y: moment rate[N&middot;m/s])
	 */
	private final Trace MOMENT_RATE_FUNCTION;

	/**
	 * @param args
	 *            -l for all the list. If ID(yyyyMMdd_HHmmss)s put, it shows
	 *            their information.
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			printList();
			return;
		}

		for (String id : args) {
			Optional<SCARDEC_ID> sid = EXISTING_ID.stream().filter(i -> i.getDateTimeString().equals(id)).findAny();
			if (!sid.isPresent()) {
				System.err.println("No information for " + id + " (check if it is 'yyyyMMdd_HHmmss')");
				continue;
			}
			SCARDEC_ID scid = sid.get();
			getOPT(scid).printInfo();
		}

		// printList();
	}

	public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy MM dd HH mm ss");

	public static SCARDEC readAscii(Path path) throws IOException {
		List<String> lines = Files.readAllLines(path);
		SCARDEC_ID id = SCARDEC_ID.of(path.getFileName().toString());
		String first = lines.get(0);
		String date = first.substring(0, first.indexOf(".0"));
		LocalDateTime origin = LocalDateTime.parse(date, FORMAT);
		if (!id.ORIGIN_TIME.equals(origin))
			throw new RuntimeException("ORIGIN time in the file name and the file are different!!");
		double[] latlon = Arrays.stream(first.substring(first.indexOf(".0") + 3).split("\\s+"))
				.mapToDouble(Double::parseDouble).toArray();
		double[] values = Arrays.stream(lines.get(1).split("\\s+")).mapToDouble(Double::parseDouble).toArray();
		Location src = new Location(latlon[0], latlon[1], 6371 - values[0]);
		String[][] moments = lines.stream().skip(2).map(s -> s.trim().split("\\s+")).toArray(String[][]::new);
		double[] time = Arrays.stream(moments).mapToDouble(p -> Double.parseDouble(p[0])).toArray();
		double[] momentrate = Arrays.stream(moments).mapToDouble(p -> Double.parseDouble(p[1])).toArray();
		return new SCARDEC(id, src, values[1], values[2], values[3], values[4], values[5], values[6], values[7],
				values[8], new Trace(time, momentrate));
	}

	/**
	 * 
	 * Outputs the SCARDEC information in the binary format.
	 * 
	 * @param path
	 *            Path of the output file
	 * @param options
	 *            if any
	 * @throws IOException
	 *             if any
	 */
	public void write(Path path, OpenOption... options) throws IOException {
		try (DataOutputStream dos = new DataOutputStream(Files.newOutputStream(path, options))) {
			dos.writeInt(ID.ORIGIN_TIME.getYear());
			dos.writeInt(ID.ORIGIN_TIME.getMonthValue());
			dos.writeInt(ID.ORIGIN_TIME.getDayOfMonth());
			dos.writeInt(ID.ORIGIN_TIME.getHour());
			dos.writeInt(ID.ORIGIN_TIME.getMinute());
			dos.writeInt(ID.ORIGIN_TIME.getSecond());
			dos.writeDouble(EPICENTRAL_LOCATION.getLatitude());
			dos.writeDouble(EPICENTRAL_LOCATION.getLongitude());
			dos.writeDouble(EPICENTRAL_LOCATION.getR());
			dos.writeDouble(M0);
			dos.writeDouble(MW);
			dos.writeDouble(STRIKE1);
			dos.writeDouble(DIP1);
			dos.writeDouble(RAKE1);
			dos.writeDouble(STRIKE2);
			dos.writeDouble(DIP2);
			dos.writeDouble(RAKE2);
			double[] time = MOMENT_RATE_FUNCTION.getX();
			double[] stf = MOMENT_RATE_FUNCTION.getY();
			dos.writeInt(time.length);
			for (int i = 0; i < time.length; i++) {
				dos.writeDouble(time[i]);
				dos.writeDouble(stf[i]);
			}
		}
	}

	public static SCARDEC readBinary(Path path, OpenOption... options) throws IOException {
		try (DataInputStream dis = new DataInputStream(Files.newInputStream(path, options))) {
			SCARDEC_ID id = SCARDEC_ID.of(path.getFileName().toString());
			SCARDEC sc = readBinary(dis, id.REGION);
			if (!sc.ID.ORIGIN_TIME.equals(id.ORIGIN_TIME))
				throw new RuntimeException("ORIGIN time in the file name and in the file are different!!");
			return sc;
		}
	}

	private static SCARDEC readBinary(InputStream is, String region) throws IOException {
		try (DataInputStream dis = new DataInputStream(is)) {
			LocalDateTime origin = LocalDateTime.of(dis.readInt(), dis.readInt(), dis.readInt(), dis.readInt(),
					dis.readInt(), dis.readInt());
			SCARDEC_ID id = new SCARDEC_ID(origin, region);
			Location loc = new Location(dis.readDouble(), dis.readDouble(), dis.readDouble());
			double m0 = dis.readDouble();
			double mw = dis.readDouble();
			double strike1 = dis.readDouble();
			double dip1 = dis.readDouble();
			double rake1 = dis.readDouble();
			double strike2 = dis.readDouble();
			double dip2 = dis.readDouble();
			double rake2 = dis.readDouble();
			int n = dis.readInt();
			double[] time = new double[n];
			double[] stf = new double[n];
			for (int i = 0; i < n; i++) {
				time[i] = dis.readDouble();
				stf[i] = dis.readDouble();
			}
			Trace momentRate = new Trace(time, stf);
			return new SCARDEC(id, loc, m0, mw, strike1, dip1, rake1, strike2, dip2, rake2, momentRate);
		}
	}

	/**
	 * Prints out the origin time, epicentral location, M0, Mw, strikes, dips
	 * and rakes.
	 */
	public void printInfo() {
		System.out.println(ID.toString());
		System.out.println("Epicentral location(lat lon radius):" + EPICENTRAL_LOCATION);
		System.out.println("M0:" + M0 + ", Mw:" + MW);
		System.out.println("strike1:" + STRIKE1 + ", dip1:" + DIP1 + ", rake1:" + RAKE1 + " [deg]");
		System.out.println("strike2:" + STRIKE2 + ", dip2:" + DIP2 + ", rake2:" + RAKE2 + " [deg]");
	}

	/**
	 * Prints out moment rate function in the format "time[s] moment
	 * rate[N&middot;m/s]"
	 */
	public void printMomentRate() {
		System.out.println("time[s] moment rate[N\u00b7m/s]");
		double[] time = MOMENT_RATE_FUNCTION.getX();
		double[] stf = MOMENT_RATE_FUNCTION.getY();
		for (int i = 0; i < time.length; i++)
			System.out.println(time[i] + " " + stf[i]);
	}

}
