package io.github.kensuke1984.kibrary.inversion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;

/**
 * Utilities for output of {@link LetMeInvert}.
 * 
 * @author kensuke
 * 
 * @version 0.1.0.1
 */
public class InversionResult {

	private Path rootPath;

	/**
	 * variance of all traces (obs vs syn) for an event.
	 * 
	 */
	private Map<GlobalCMTID, Double> eventVarianceMap;

	/**
	 * variance of all traces (obs vs syn) for a station.
	 */
	private Map<Station, Double> stationVarianceMap;

	/**
	 * variance of all traces (obs vs born) using answer Each array of each
	 * method has variances for the answers. array[0] is the variance between
	 * obs vs initial model.
	 */
	private Map<InverseMethodEnum, Double[]> answerVarianceMap = new EnumMap<>(InverseMethodEnum.class);

	/**
	 * 
	 * @param rootPath
	 *            of an inversion
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public InversionResult(Path rootPath) throws IOException {
		this.rootPath = rootPath;
		readVarianceMap();
		readOrder();
		Path answerOrderPath = rootPath.resolve("unknownParameterOrder.inf");
		unknownParameterList = UnknownParameterFile.read(answerOrderPath);

	}

	public List<UnknownParameter> getUnknownParameterList() {
		return unknownParameterList;
	}

	/**
	 * @return {@link BasicID}s in order.
	 */
	public List<BasicID> getBasicIDList() {
		return basicIDList;
	}

	/**
	 * @return the number of timewindow
	 */
	public int getNumberOfWindows() {
		return basicIDList.size();
	}

	/**
	 * type is always obs
	 * 
	 * @param parts
	 * @return id
	 */
	private static BasicID toBasicID(String[] parts) {
		Station station = new Station(parts[1],
				new HorizontalPosition(Double.parseDouble(parts[3]), Double.parseDouble(parts[4])), parts[2]);
		return new BasicID(WaveformType.OBS, Double.parseDouble(parts[10]), Double.parseDouble(parts[8]),
				Integer.parseInt(parts[9]), station, new GlobalCMTID(parts[5]), SACComponent.valueOf(parts[6]),
				Double.parseDouble(parts[11]), Double.parseDouble(parts[12]), Long.parseLong(parts[13]), true);
	}

	/**
	 * List of BasicID in order in vectors.
	 */
	private List<BasicID> basicIDList;
	private int[] startPointOrder;
	private double[] synStartTimeOrder;

	/**
	 * @param i
	 *            index of the order in vectors
	 * @return Difference of Start time ( observed - synthetic )
	 */
	public double getStartTimeDifference(int i) {
		return basicIDList.get(i).getStartTime() - synStartTimeOrder[i];
	}

	/**
	 * Weighting of observed and synthetic. Raws can be ontained by /weighting.
	 */
	private double[] weightingOrder;
	private List<UnknownParameter> unknownParameterList;

	private void readOrder() throws IOException {
		Path orderPath = rootPath.resolve("order.inf");
		List<String> orderLines = Files.readAllLines(orderPath);
		int n = orderLines.size() - 1;
		basicIDList = new ArrayList<>(n);
		startPointOrder = new int[n];
		synStartTimeOrder = new double[n];
		weightingOrder = new double[n];
		IntStream.range(0, n).forEach(i -> {
			String[] parts = orderLines.get(i + 1).split("\\s+");
			basicIDList.add(toBasicID(parts));
			npts += Integer.parseInt(parts[9]);
			startPointOrder[i] = Integer.parseInt(parts[15]);
			synStartTimeOrder[i] = Double.parseDouble(parts[16]);
			weightingOrder[i] = Double.parseDouble(parts[17]);
		});
	}

	/**
	 * @param inverse
	 *            the method for the inverse problem
	 * @param n
	 *            the index for the answer 1 &le; n
	 * @return the variance between obs and born with the answer of the n
	 */
	public double varianceOf(InverseMethodEnum inverse, int n) {
		if (n < 1)
			throw new IllegalArgumentException("n must be 1 or more.");
		return answerVarianceMap.get(inverse)[n];
	}

	/**
	 * @param a
	 *            assumed redundancy in data points. It is used as n/a, where n
	 *            is the number of data points.
	 * @param inverse
	 *            the method for the inverse problem
	 * @param n
	 *            the index for the answer 1 &le; n
	 * @return the Akaike Information criterion for the answer
	 */
	public double aicOf(double a, InverseMethodEnum inverse, int n) {
		return Utilities.computeAIC(varianceOf(inverse, n), npts / a, n);
	}

	/**
	 * @return the number of datapoints;
	 */
	public int getNumberOfDatapoints() {
		return npts;
	}

	/**
	 * the number of data points
	 */
	private int npts;

	/**
	 * @param id
	 *            ID of the partial
	 * @param parN
	 *            index of a partial in unknown parameters
	 * @return {@link Trace} of parN in nth time window. time axis is Synthetic
	 *         one
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public Trace partialOf(BasicID id, UnknownParameter parameter) throws IOException {
		int parN = unknownParameterList.indexOf(parameter);
		Path txtPath = rootPath.resolve("partial/" + getTxtName(id));
		List<String> lines = Files.readAllLines(txtPath);
		int npts = lines.size() - 1;
		double[] x = new double[npts];
		double[] y = new double[npts];
		IntStream.range(0, npts).forEach(j -> {
			String[] parts = lines.get(j + 1).split("\\s+");
			x[j] = Double.parseDouble(parts[0]);
			y[j] = Double.parseDouble(parts[parN + 1]);
		});
		return new Trace(x, y);
	}

	/**
	 * Example, if you want to get an answer of CG1. <br>
	 * inverse &rarr; CG, n &rarr; 1
	 * 
	 * Returning Map key is an unknown parameter.
	 * 
	 * @param inverse
	 *            used solver
	 * @param n
	 *            the number of used eigen vectors for the answer
	 * @return Map of answer values
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public Map<UnknownParameter, Double> answerOf(InverseMethodEnum inverse, int n) throws IOException {
		if (n <= 0)
			throw new IllegalArgumentException("n is out of range. must be 1, 2,.. ");
		double[] values = Files.readAllLines(rootPath.resolve(inverse + "/" + inverse + n + ".txt")).stream()
				.mapToDouble(Double::parseDouble).toArray();
		Map<UnknownParameter, Double> ansMap = new HashMap<>(values.length);
		for (int i = 0; i < unknownParameterList.size(); i++)
			ansMap.put(unknownParameterList.get(i), values[i]);
		return Collections.unmodifiableMap(ansMap);
	}

	/**
	 * If you want to have a trace of 3rd (from 0) timewindow and CG2.<br>
	 * i &rarr; 3, ans &rarr; 2 and method &rarr; CG
	 * 
	 * @param id
	 *            of the returning born
	 * @param method
	 *            {@link InverseMethodEnum} of the answer
	 * @param n
	 *            [1, parN] the number of used eigen vectors to obtain the
	 *            answer
	 * @return {@link Trace} of i th born waveform in timewindows by the answer
	 *         using method and n eigen vectors. Time axis is synthetic one.
	 * @throws IOException
	 *             if an I/O error occurs, no born for the answer
	 */
	private Trace readBORNTrace(BasicID id, InverseMethodEnum method, int n) throws IOException {
		Path txtPath = rootPath.resolve("born/" + method + n + "/" + getTxtName(id));
		List<String> lines = Files.readAllLines(txtPath);
		int npts = lines.size() - 1;
		double[] x = new double[npts];
		double[] y = new double[npts];
		IntStream.range(0, npts).forEach(j -> {
			String[] parts = lines.get(j + 1).split("\\s+");
			x[j] = Double.parseDouble(parts[0]);
			y[j] = Double.parseDouble(parts[1]);
		});
		return new Trace(x, y);
	}

	/**
	 * @param i
	 *            index of the order in vector
	 * @return {@link Trace} of n th observed waveforms. Time axis is observed
	 *         one.
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public Trace observedOf(BasicID id) throws IOException {
		Path txtPath = rootPath.resolve("trace/" + getTxtName(id));
		List<String> lines = Files.readAllLines(txtPath);
		int npts = lines.size() - 1;
		double[] x = new double[npts];
		double[] y = new double[npts];
		IntStream.range(0, npts).forEach(j -> {
			String[] parts = lines.get(j + 1).split("\\s+");
			x[j] = Double.parseDouble(parts[0]);
			y[j] = Double.parseDouble(parts[2]);
		});
		return new Trace(x, y);
	}

	/**
	 * Text filename for accessing the related file will return.
	 * 
	 * @param id
	 *            of the target waveform
	 * @return txt file name of the id including "eventID/" at head. e.g., if
	 *         its partial "partial/(txt file name)" is fine.
	 */
	private String getTxtName(BasicID id) {
		return id.getGlobalCMTID() + "/" + id.getStation() + "." + id.getGlobalCMTID() + "." + id.getSacComponent()
				+ "." + basicIDList.indexOf(id) + ".txt";
	}

	/**
	 * @param id
	 *            ID of the order in vector
	 * @return {@link Trace} of n th synthetic waveforms. Time axis is syn
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public Trace syntheticOf(BasicID id) throws IOException {
		Path txtPath = rootPath.resolve("trace/" + getTxtName(id));
		List<String> lines = Files.readAllLines(txtPath);
		int npts = lines.size() - 1;
		double[] x = new double[npts];
		double[] y = new double[npts];
		IntStream.range(0, npts).forEach(j -> {
			String[] parts = lines.get(j + 1).split("\\s+");
			x[j] = Double.parseDouble(parts[1]);
			y[j] = Double.parseDouble(parts[3]);
		});
		return new Trace(x, y);
	}

	private void readVarianceMap() throws IOException {
		Path eventPath = rootPath.resolve("trace/eventVariance.inf");
		Path stationPath = rootPath.resolve("trace/stationVariance.inf");
		eventVarianceMap = Collections
				.unmodifiableMap(Files.readAllLines(eventPath).stream().skip(1).map(line -> line.split("\\s+")).collect(
						Collectors.toMap(parts -> new GlobalCMTID(parts[0]), parts -> Double.parseDouble(parts[4]))));
		stationVarianceMap = Collections
				.unmodifiableMap(Files.readAllLines(stationPath).stream().skip(1).map(line -> line.split("\\s+"))
						.collect(Collectors.toMap(
								parts -> new Station(parts[0],
										new HorizontalPosition(Double.parseDouble(parts[2]),
												Double.parseDouble(parts[3])),
										parts[1]),
								parts -> Double.parseDouble(parts[4]))));
		for (InverseMethodEnum inverse : InverseMethodEnum.values()) {
			Path path = rootPath.resolve(inverse.name() + "/variance.txt");
			answerVarianceMap.put(inverse,
					Files.lines(path).mapToDouble(Double::parseDouble).boxed().toArray(n -> new Double[n]));
		}
	}

	public Set<Station> stationSet() {
		return stationVarianceMap.keySet();
	}

	public Set<GlobalCMTID> idSet() {
		return eventVarianceMap.keySet();
	}

	/**
	 * If the born waveform already is computed and in a file then, read and
	 * return it. If not, this method computes a born waveform and returns it
	 * and write in a certain folder.
	 * 
	 * @param id
	 *            of the target raypath
	 * @param method
	 *            of inversion
	 * @param n
	 *            number of eigen vectors(SVD) or CG vectors (CG) or...<br>
	 *            if it is 1 and method is CG then CG1
	 * @return Trace of Born
	 * @throws IOException
	 */
	public Trace bornOf(BasicID id, InverseMethodEnum method, int n) throws IOException {
		String txtname = getTxtName(id);
		Path bornPath = rootPath.resolve("born/" + method + n + "/" + txtname);
		if (Files.exists(bornPath))
			return readBORNTrace(id, method, n);

		Files.createDirectories(bornPath.getParent());
		Trace syn = syntheticOf(id);
		Map<UnknownParameter, Double> answer = answerOf(method, n);
		Trace born = syn;

		for (UnknownParameter par : unknownParameterList)
			born = born.add(partialOf(id, par).multiply(answer.get(par)));
		writeBorn(bornPath, born);
		return born;

	}

	private static void writeBorn(Path outBornPath, Trace born) throws IOException {
		List<String> lines = new ArrayList<>(born.getLength() + 1);
		lines.add("#syntime synthetic+");
		double[] x = born.getX();
		double[] y = born.getY();
		for (int i = 0; i < born.getLength(); i++)
			lines.add(x[i] + " " + y[i]);
		Files.write(outBornPath, lines);
	}

	/**
	 * Computes born waveforms of the answer by using n basis vector in the
	 * method for all time windows. This method also creates 'eachVariance',
	 * 'eventVariance', 'stationVariance' (.txt)
	 * 
	 * @param method
	 *            to solve an equation
	 * @param n
	 *            the number of used basis vector
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void createBorn(InverseMethodEnum method, int n) throws IOException {
		for (BasicID id : basicIDList)
			bornOf(id, method, n);
		Path each = rootPath.resolve("born/" + method + n + "/eachVariance.txt");
		if (!Files.exists(each)) {
			List<String> eachLines = new ArrayList<>(basicIDList.size());
			int j = 0;
			for (BasicID id : basicIDList)
				eachLines.add(j++ + " " + variance(observedOf(id).getYVector(), bornOf(id, method, n).getYVector()));
			Files.write(each, eachLines);
		}
		Path event = rootPath.resolve("born/" + method + n + "/eventVariance.txt");
		if (!Files.exists(event)) {
			List<String> eventLines = new ArrayList<>(basicIDList.size());
			Map<GlobalCMTID, RealVector> obsMap = new HashMap<>(idSet().size());
			Map<GlobalCMTID, RealVector> bornMap = new HashMap<>(idSet().size());
			for (BasicID bid : basicIDList) {
				GlobalCMTID id = bid.getGlobalCMTID();
				RealVector obs = obsMap.getOrDefault(id, new ArrayRealVector());
				RealVector born = bornMap.getOrDefault(id, new ArrayRealVector());
				obsMap.put(id, obs.append(observedOf(bid).getYVector()));
				bornMap.put(id, born.append(bornOf(bid, method, n).getYVector()));
			}
			for (GlobalCMTID id : idSet()) {
				RealVector obs = obsMap.get(id);
				RealVector del = bornMap.get(id).subtract(obs);
				eventLines.add(id + " " + del.dotProduct(del) / obs.dotProduct(obs));
			}
			Files.write(event, eventLines);
		}
		Path station = rootPath.resolve("born/" + method + n + "/stationVariance.txt");
		if (!Files.exists(station)) {
			List<String> stationLines = new ArrayList<>(basicIDList.size());
			Map<Station, RealVector> obsMap = new HashMap<>(idSet().size());
			Map<Station, RealVector> bornMap = new HashMap<>(idSet().size());
			for (BasicID bid : basicIDList) {
				RealVector obs = obsMap.getOrDefault(bid.getStation(), new ArrayRealVector());
				RealVector born = bornMap.getOrDefault(bid.getStation(), new ArrayRealVector());
				obsMap.put(bid.getStation(), obs.append(observedOf(bid).getYVector()));
				bornMap.put(bid.getStation(), born.append(bornOf(bid, method, n).getYVector()));
			}
			for (Station s : stationSet()) {
				RealVector obs = obsMap.get(s.getStationName());
				RealVector del = bornMap.get(s.getStationName()).subtract(obs);
				stationLines.add(s + " " + del.dotProduct(del) / obs.dotProduct(obs));
			}
			Files.write(station, stationLines);
		}
	}

	/**
	 * @param obs
	 *            vector of observed
	 * @param another
	 *            vector of waveform to compute with
	 * @return (another-obs)**2/obs**2
	 */
	private static double variance(RealVector obs, RealVector another) {
		RealVector del = obs.subtract(another);
		return del.dotProduct(del) / obs.dotProduct(obs);
	}

	/**
	 * @return (<b>unmodifiable</b>)Map of variance (obs vs syn) for a station.
	 */
	public Map<Station, Double> getStationVariance() {
		return stationVarianceMap;
	}

	/**
	 * @return (<b>unmodifiable</b>)Map of variance (obs vs syn) for an event.
	 */
	public Map<GlobalCMTID, Double> getEventVariance() {
		return eventVarianceMap;
	}

	/**
	 * @return the variance between obs and syn (initial model)
	 */
	public double getVariance() {
		return answerVarianceMap.get(InverseMethodEnum.CG)[0];
	}

}
