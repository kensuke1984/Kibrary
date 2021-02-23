package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.util.*;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.anisotime.Phase;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Utilities for write of {@link LetMeInvert}.
 *
 * @author Kensuke Konishi
 * @version 0.1.2.1
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
	private Set<InverseMethodEnum> inverseMethods;
	/**
	 * List of BasicID in order in vectors.
	 */
	private List<BasicID> basicIDList;
	private int[] startPointOrder;
	private double[] synStartTimeOrder;
	/**
	 * Weighting of observed and synthetic. Raws can be ontained by /weighting.
	 */
	private double[] weightingOrder;
	private List<UnknownParameter> unknownParameterList;
	private List<UnknownParameter> originalUnknownParameterList;
    /**
     * the number of data points
     */
    private int npts;
    private double mul = 1.;
	
    /**
     * @param rootPath of an inversion
     * @throws IOException if an I/O error occurs
     */
	public InversionResult(Path rootPath) throws IOException {
		this.rootPath = rootPath;
		inverseMethods = Stream.of(InverseMethodEnum.values()).filter(ime -> rootPath.resolve(ime.simple()).toFile().exists())
			.collect(Collectors.toSet());
		readVarianceMap();
		readOrder();
		Path answerOrderPath = rootPath.resolve("unknownParameterOrder.inf");
		unknownParameterList = UnknownParameterFile.read(answerOrderPath);
		originalUnknownParameterList = UnknownParameterFile.read(rootPath.resolve("originalUnknownParameterOrder.inf"));
	}
	
	/**
	 * @param rootPath
	 * @param minimal
	 * @throws IOException
	 * @author anselme
	 */
	public InversionResult(Path rootPath, boolean minimal) throws IOException {
		this.rootPath = rootPath;
		inverseMethods = Stream.of(InverseMethodEnum.values()).filter(ime -> rootPath.resolve(ime.simple()).toFile().exists())
			.collect(Collectors.toSet());
//		readVarianceMap();
//		readOrder();
		Path answerOrderPath = rootPath.resolve("unknownParameterOrder.inf");
		unknownParameterList = UnknownParameterFile.read(answerOrderPath);
		originalUnknownParameterList = UnknownParameterFile.read(rootPath.resolve("originalUnknownParameterOrder.inf"));
	}
	
	/**
	 * @param rootPath
	 * @param inverseMethods
	 * @throws IOException
	 * @author anselme
	 */
	public InversionResult(Path rootPath, Set<InverseMethodEnum> inverseMethods) throws IOException {
		this.rootPath = rootPath;
		this.inverseMethods = inverseMethods;
		readVarianceMap();
		readOrder();
		Path answerOrderPath = rootPath.resolve("unknownParameterOrder.inf");
		unknownParameterList = UnknownParameterFile.read(answerOrderPath);
		originalUnknownParameterList = UnknownParameterFile.read(rootPath.resolve("originalUnknownParameterOrder.inf"));
	}

    /**
     * type is always obs
     *
     * @param parts string array for a basic id.
     * @return id
     */
	private static BasicID toBasicID(String[] parts) {
		Station station = new Station(parts[1],
				new HorizontalPosition(Double.parseDouble(parts[3]), Double.parseDouble(parts[4])), parts[2]);
		String[] phaseParts = parts[13].split(",");
		Phase[] phases = new Phase[phaseParts.length];
		for (int i = 0; i < phases.length; i++)
			phases[i] = Phase.create(phaseParts[i], false);
		return new BasicID(WaveformType.OBS, Double.parseDouble(parts[10]), Double.parseDouble(parts[8]),
				Integer.parseInt(parts[9]), station, new GlobalCMTID(parts[5]), SACComponent.valueOf(parts[6]),
				Double.parseDouble(parts[11]), Double.parseDouble(parts[12]), phases, Long.parseLong(parts[14]), true);
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
     * @param obs     vector of observed
     * @param another vector of waveform to compute with
     * @return (another-obs)<sup>2</sup>/obs<sup>2</sup>
     */
	private static double variance(RealVector obs, RealVector another) {
		RealVector del = obs.subtract(another);
		return del.dotProduct(del) / obs.dotProduct(obs);
	}
	
	/**
     * @param answer map of the answer
     * @param nPoints  number of points
     * @param nPower   距離の何乗で補間するか
     * @param location location for complement
     * @param type     {@link PartialType}
     * @return locationの直近nPoints点からの補間値
     */
	public static double complement(Map<UnknownParameter, Double> answer, int nPoints, int nPower, Location location,
			PartialType type) {
		if (!type.is3D())
			throw new RuntimeException(type + " is not 3d parameter"); // TODO
		Map<Location, Double> ansMap = answer.keySet().stream().filter(key -> key.getPartialType() == type).collect(
				Collectors.toMap(key -> ((Physical3DParameter) key).getPointLocation(), key -> answer.get(key)));

		if (type.equals(PartialType.TIME_RECEIVER) || type.equals(PartialType.TIME_SOURCE) )
			throw new RuntimeException("TIME PARTIAL MADADAMEEEEEEEE");
		if (ansMap.containsKey(location))
			return ansMap.get(location);

		Location[] nearLocations = location
				.getNearestLocation(answer.keySet().stream().filter(key -> key.getPartialType() == type)
						.map(key -> ((Physical3DParameter) key).getPointLocation()).toArray(Location[]::new));
		double[] r = new double[nPoints];
		double rTotal = 0;
		for (int iPoint = 0; iPoint < nPoints; iPoint++) {
			r[iPoint] = Math.pow(nearLocations[iPoint].getDistance(location), nPower);
			rTotal += 1 / r[iPoint];
		}
		double value = 0;
		for (int iPoint = 0; iPoint < nPoints; iPoint++)
			value += ansMap.get(nearLocations[iPoint]) / r[iPoint];

		return value / rTotal;
	}
	
	/**
	 * @return (unmodifiable) List of unknown parameters
	 */
	public List<UnknownParameter> getUnknownParameterList() {
		return unknownParameterList;
	}
	
	/**
	 * @return
	 * @author anselme
	 */
	public List<UnknownParameter> getOriginalUnknownParameterList() {
		return originalUnknownParameterList;
	}

	/**
	 * @return (unmodifiable) List of {@link BasicID} in order.
	 */
	public List<BasicID> getBasicIDList() {
		return basicIDList;
	}
	
	/**
	 * @return Number of timewindow
	 */
	public int getNumberOfWindows() {
		return basicIDList.size();
	}

	/**
	 * @return Number of unknown parameters (m)
	 */
	public int getNumberOfUnknowns() {
		return unknownParameterList.size();
	}
	
	public Set<InverseMethodEnum> getInverseMethods() {
		return inverseMethods;
	}

	/**
	 * @param i index of the order in vectors
	 * @return Difference of Start time ( observed - synthetic )
	 */
	public double getStartTimeDifference(int i) {
		return basicIDList.get(i).getStartTime() - synStartTimeOrder[i];
	}

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
			startPointOrder[i] = Integer.parseInt(parts[16]);
			synStartTimeOrder[i] = Double.parseDouble(parts[17]);
			weightingOrder[i] = Double.parseDouble(parts[18]);
		});
		basicIDList = Collections.unmodifiableList(basicIDList);
	}

    /**
     * @return vector of residual (observed-synthetic) waveforms in an observed
     * equation
     * @throws IOException if any
     */
	public RealVector getDVector() throws DimensionMismatchException, IOException {
		return getObservedVector().subtract(getSyntheticVector());
	}

	/**
	 * @return vector of observed waveforms in an observed equation
	 * @throws IOException if any
	 */
	public RealVector getObservedVector() throws IOException {
		double[] obsv = new double[npts];
		int i = 0;
		for (BasicID id : basicIDList) {
			double[] obs = observedOf(id).getY();
			System.arraycopy(obs, 0, obsv, i, obs.length);
			i += obs.length;
		}
		return new ArrayRealVector(obsv, false);
	}

    /**
     * @return vector of synthetic waveforms in an observed equation
     * @throws IOException if any
     */
	public RealVector getSyntheticVector() throws IOException {
		double[] synv = new double[npts];
		int i = 0;
		for (BasicID id : basicIDList) {
			double[] syn = syntheticOf(id).getY();
			System.arraycopy(syn, 0, synv, i, syn.length);
			i += syn.length;
		}
		return new ArrayRealVector(synv, false);
	}

    /**
     * @param inverse the method for the inverse problem
     * @param n       the index for the answer 1 &le; n
     * @return the variance between obs and born with the answer of the n
     */
	public double varianceOf(InverseMethodEnum inverse, int n) {
		if (n < 1)
			throw new IllegalArgumentException("n must be 1 or more.");
		return answerVarianceMap.get(inverse)[n];
	}

    /**
     * @param a       assumed redundancy in data points. It is used as n/a, where n
     *                is the number of data points, note that n/a will be
     *                (int)(n/a).
     * @param inverse the method for the inverse problem
     * @param n       the index for the answer 1 &le; n
     * @return the Akaike Information criterion for the answer
     */
	public double aicOf(double a, InverseMethodEnum inverse, int n) {
		return Utilities.computeAIC(varianceOf(inverse, n), (int) (npts / a), n);
	}

	/**
	 * @return the number of datapoints (NPTS)
	 */
	public int getNumberOfDatapoints() {
		return npts;
	}

    /**
     * @param id        ID of the partial
     * @param parameter index of a partial in unknown parameters
     * @return {@link Trace} of parN in nth time window. time axis is Synthetic
     * one
     * @throws IOException if an I/O error occurs
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
     * @param id        ID of the partial
     * @param parameter index of a partial in unknown parameters
     * @return {@link Trace} of parN in nth time window. time axis is Synthetic
     * one
     * @throws IOException if an I/O error occurs
     * @author anselme
     */
	public Trace partialOf_noorder(BasicID id, UnknownParameter parameter) throws IOException {
		int parN = unknownParameterList.indexOf(parameter);
		
		List<Path> tmplist;
		try (Stream<Path> stream = Files.list(rootPath.resolve("partial/" + id.getGlobalCMTID()))) {
			tmplist = stream.filter(p -> {
				String name = p.getFileName().toString();
				return name.startsWith(id.getStation() + "." + id.getGlobalCMTID() + "." + id.getSacComponent());
			}).collect(Collectors.toList());
		}
		if (tmplist.size() != 1) {
			System.err.println("Found no or more than 1 trace for " + getTxtName(id));
			return null;
		}
		Path txtPath = tmplist.get(0);

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
	 * @param id
	 * @param parameter
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	public Trace partialOf_spc(BasicID id, UnknownParameter parameter) throws IOException {
		int parN = unknownParameterList.indexOf(parameter);
		Path txtPath = rootPath.resolve("partial_spc/" + getTxtName(id));
		List<String> lines = Files.readAllLines(txtPath);
		int npts = lines.size() - 1;
		double[] x = new double[npts];
		double[] y = new double[npts];
		IntStream.range(0, npts).forEach(j -> {
			String[] parts = lines.get(j + 1).split("\\s+");
			x[j] = Double.parseDouble(parts[0]);
			y[j] = Double.parseDouble(parts[parN + 1]);
		});
		return new Trace(x, y).multiply(mul);
	}
	
	/**
	 * @param id
	 * @param parameter
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	public Trace partialOf_noorder_spc(BasicID id, UnknownParameter parameter) throws IOException {
		int parN = unknownParameterList.indexOf(parameter);
		
		List<Path> tmplist;
		try (Stream<Path> stream = Files.list(rootPath.resolve("partial_spc/" + id.getGlobalCMTID()))) {
			tmplist = stream.filter(p -> {
				String name = p.getFileName().toString();
				return name.startsWith(id.getStation() + "." + id.getGlobalCMTID() + "." + id.getSacComponent());
			}).collect(Collectors.toList());
		}
		if (tmplist.size() != 1) {
			System.err.println("Found no or more than 1 trace for " + getTxtName(id));
			return null;
		}
		Path txtPath = tmplist.get(0);
		
		List<String> lines = Files.readAllLines(txtPath);
		int npts = lines.size() - 1;
		double[] x = new double[npts];
		double[] y = new double[npts];
		IntStream.range(0, npts).forEach(j -> {
			String[] parts = lines.get(j + 1).split("\\s+");
			x[j] = Double.parseDouble(parts[0]);
			y[j] = Double.parseDouble(parts[parN + 1]);
		});
		return new Trace(x, y).multiply(mul);
	}

    /**
     * Example, if you want to get an answer of CG1. <br>
     * inverse &rarr; CG, n &rarr; 1
     * <p>
     * Keys of the returning map are unknown parameters.
     *
     * @param inverse used solver
     * @param n       the number of used eigen vectors for the answer
     * @return Map of answer values
     * @throws IOException if an I/O error occurs
     */
	public Map<UnknownParameter, Double> answerMapOf(InverseMethodEnum inverse, int n) throws IOException {
		if (n <= 0)
			throw new IllegalArgumentException("n is out of range. must be >= 0 " + n);
		double[] unknownParameterWeigths = getUnkownParameterWeights();
		double[] values = Files.readAllLines(rootPath.resolve(inverse.simple() + "/" + inverse.simple() + n + ".txt"))
				.stream().mapToDouble(Double::parseDouble).toArray();
		if (unknownParameterWeigths == null)
			return IntStream.range(0, values.length).boxed()
				.collect(Collectors.toMap(unknownParameterList::get, i -> values[i]));
		else
			return IntStream.range(0, values.length).boxed()
					.collect(Collectors.toMap(unknownParameterList::get, i -> values[i] * unknownParameterWeigths[i]));
	}
	
	public Map<UnknownParameter, Double> answerMapOfX(InverseMethodEnum inverse, int n) throws IOException {
		if (n <= 0)
			throw new IllegalArgumentException("n is out of range. must be >= 0 " + n);
		double[] unknownParameterWeigths = getUnkownParameterWeights();
		double[] values = Files.readAllLines(rootPath.resolve(inverse.simple() + "/" + inverse.simple() + "_x" + n + ".txt"))
				.stream().mapToDouble(Double::parseDouble).toArray();
		if (unknownParameterWeigths == null)
			return IntStream.range(0, values.length).boxed()
				.collect(Collectors.toMap(unknownParameterList::get, i -> values[i]));
		else
			return IntStream.range(0, values.length).boxed()
					.collect(Collectors.toMap(unknownParameterList::get, i -> values[i] * unknownParameterWeigths[i]));
	}
	
	public Map<UnknownParameter, Double> answerMapOf(InverseMethodEnum inverse, int n, int iRes) throws IOException {
		if (n <= 0)
			throw new IllegalArgumentException("n is out of range. must be >= 0 " + n);
		double[] unknownParameterWeigths = getUnkownParameterWeights();
		double[] values = Files.readAllLines(rootPath.resolve(inverse.simple() + iRes + "/" + inverse.simple() + n + ".txt"))
				.stream().mapToDouble(Double::parseDouble).toArray();
		if (unknownParameterWeigths == null)
			return IntStream.range(0, values.length).boxed()
				.collect(Collectors.toMap(unknownParameterList::get, i -> values[i]));
		else
			return IntStream.range(0, values.length).boxed()
					.collect(Collectors.toMap(unknownParameterList::get, i -> values[i] * unknownParameterWeigths[i]));
	}

    /**
     * If you want to have a trace of 3rd (from 0) timewindow and CG2.<br>
     * i &rarr; 3, ans &rarr; 2 and method &rarr; CG
     *
     * @param id     of the returning born
     * @param method {@link InverseMethodEnum} of the answer
     * @param n      [1, parN] the number of used eigen vectors to obtain the
     *               answer
     * @return {@link Trace} of i th born waveform in timewindows by the answer
     * using method and n eigen vectors. Time axis is synthetic one.
     * @throws IOException if an I/O error occurs, no born for the answer
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
	 * @param id
	 * @param method
	 * @param n
	 * @param type
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	private Trace readBORNTrace(BasicID id, InverseMethodEnum method, int n, PartialType type) throws IOException {
		Path txtPath = rootPath.resolve("born/" + method + n + "_" + type + "/" + getTxtName(id));
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
	 * @param id
	 * @param method
	 * @param n
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	private Trace readBORNTrace_spc(BasicID id, InverseMethodEnum method, int n) throws IOException {
		Path txtPath = rootPath.resolve("born_spc/" + method + n + "/" + getTxtName(id));
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
	 * @param id
	 * @param method
	 * @param n
	 * @param type
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	private Trace readBORNTrace_spc(BasicID id, InverseMethodEnum method, int n, PartialType type) throws IOException {
		Path txtPath = rootPath.resolve("born_spc/" + method + n + "_" + type + "/" + getTxtName(id));
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
     * @param id id of an observed waveform (This ID must be in the list by
     *           {@link #getBasicIDList()})
     * @return {@link Trace} of n th observed waveforms. Time axis is observed
     * one.
     * @throws IOException if an I/O error occurs
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
	 * @param id
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	public Trace observedOf_spc(BasicID id) throws IOException {
		Path txtPath = rootPath.resolve("trace_spc/" + getTxtName(id));
		List<String> lines = Files.readAllLines(txtPath);
		int npts = lines.size() - 1;
		double[] x = new double[npts];
		double[] y = new double[npts];
		IntStream.range(0, npts).forEach(j -> {
			String[] parts = lines.get(j + 1).split("\\s+");
			x[j] = Double.parseDouble(parts[0]);
			y[j] = Double.parseDouble(parts[2]);
		});
		return new Trace(x, y).multiply(mul);
	}
	
	/**
	 * @param id
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	public Trace observedOf_noorder_spc(BasicID id) throws IOException {
		List<Path> tmplist;
		try (Stream<Path> stream = Files.list(rootPath.resolve("trace_spc/" + id.getGlobalCMTID()))) {
			tmplist = stream.filter(p -> {
				String name = p.getFileName().toString();
				return name.startsWith(id.getStation() + "." + id.getGlobalCMTID() + "." + id.getSacComponent());
			}).collect(Collectors.toList());
		}
		if (tmplist.size() != 1) {
			System.err.println("Found no or more than 1 trace for " + getTxtName(id));
			return null;
		}
		Path txtPath = tmplist.get(0);
		
		List<String> lines = Files.readAllLines(txtPath);
		int npts = lines.size() - 1;
		double[] x = new double[npts];
		double[] y = new double[npts];
		IntStream.range(0, npts).forEach(j -> {
			String[] parts = lines.get(j + 1).split("\\s+");
			x[j] = Double.parseDouble(parts[0]);
			y[j] = Double.parseDouble(parts[2]);
		});
		return new Trace(x, y).multiply(mul);
	}

    /**
     * Text filename for accessing the related file will return.
     *
     * @param id of the target waveform
     * @return txt file name of the id including "eventID/" at head. e.g., if
     * its partial "partial/(txt file name)" is fine.
     */
	public String getTxtName(BasicID id) {
		return id.getGlobalCMTID() + "/" + id.getStation() + "." + id.getGlobalCMTID() + "." + id.getSacComponent()
				+ "." + basicIDList.indexOf(id) + ".txt";
	}
	
    /**
     * @param id ID of the order in vector (This ID must be in the list by
     *           {@link #getBasicIDList()})
     * @return {@link Trace} of n th synthetic waveforms. Time axis is syn
     * @throws IOException if an I/O error occurs
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
	
	public Trace syntheticOf_spc(BasicID id) throws IOException {
		Path txtPath = rootPath.resolve("trace_spc/" + getTxtName(id));
		
		List<String> lines = Files.readAllLines(txtPath);
		int npts = lines.size() - 1;
		double[] x = new double[npts];
		double[] y = new double[npts];
		IntStream.range(0, npts).forEach(j -> {
			String[] parts = lines.get(j + 1).split("\\s+");
			x[j] = Double.parseDouble(parts[1]);
			y[j] = Double.parseDouble(parts[3]);
		});
		return new Trace(x, y).multiply(mul);
	}
	
	/**
	 * @param id
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	public Trace syntheticOf_noorder(BasicID id) throws IOException {
		List<Path> tmplist;
		try (Stream<Path> stream = Files.list(rootPath.resolve("trace/" + id.getGlobalCMTID()))) {
			tmplist = stream.filter(p -> {
				String name = p.getFileName().toString();
				return name.startsWith(id.getStation() + "." + id.getGlobalCMTID() + "." + id.getSacComponent());
			}).collect(Collectors.toList());
		}
		if (tmplist.size() != 1) {
			System.err.println("Found no or more than 1 trace for " + getTxtName(id));
			return null;
		}
		Path txtPath = tmplist.get(0);
		
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
	
	/**
	 * @param id
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	public Trace syntheticOf_noorder_spc(BasicID id) throws IOException {
		List<Path> tmplist;
		try (Stream<Path> stream = Files.list(rootPath.resolve("trace_spc/" + id.getGlobalCMTID()))) {
			tmplist = stream.filter(p -> {
				String name = p.getFileName().toString();
				return name.startsWith(id.getStation() + "." + id.getGlobalCMTID() + "." + id.getSacComponent());
			}).collect(Collectors.toList());
		}
		if (tmplist.size() != 1) {
			System.err.println("Found no or more than 1 trace for " + getTxtName(id));
			return null;
		}
		Path txtPath = tmplist.get(0);
		
		List<String> lines = Files.readAllLines(txtPath);
		int npts = lines.size() - 1;
		double[] x = new double[npts];
		double[] y = new double[npts];
		IntStream.range(0, npts).forEach(j -> {
			String[] parts = lines.get(j + 1).split("\\s+");
			x[j] = Double.parseDouble(parts[1]);
			y[j] = Double.parseDouble(parts[3]);
		});
		return new Trace(x, y).multiply(mul);
	}

	private void readVarianceMap() throws IOException {
		Path eventPath = rootPath.resolve("trace/eventVariance.inf");
		Path stationPath = rootPath.resolve("trace/stationVariance.inf");
		eventVarianceMap = Collections
				.unmodifiableMap(Files.readAllLines(eventPath).stream().skip(1).map(line -> line.split("\\s+")).collect(
						Collectors.toMap(parts -> new GlobalCMTID(((String[]) parts)[0]), parts -> Double.parseDouble(((String[]) parts)[4]))));
		stationVarianceMap = Collections
				.unmodifiableMap(Files.readAllLines(stationPath).stream().skip(1).map(line -> line.split("\\s+"))
						.collect(Collectors.toMap(
								parts -> new Station(((String[]) parts)[0],
										new HorizontalPosition(Double.parseDouble(((String[]) parts)[2]),
												Double.parseDouble(((String[]) parts)[3])),
												((String[]) parts)[1]),
								parts -> Double.parseDouble(((String[]) parts)[4]))));
		for (InverseMethodEnum inverse : inverseMethods) {
			// TODO
			if (inverse == InverseMethodEnum.LEAST_SQUARES_METHOD)
				continue;
			Path path = rootPath.resolve(inverse.simple() + "/variance.txt");
			answerVarianceMap.put(inverse,
					Files.lines(path).mapToDouble(Double::parseDouble).boxed().toArray(Double[]::new));
		}
	}

	public Set<Station> stationSet() {
		return stationVarianceMap.keySet();
	}

	/**
	 * @return (unmodifiable) Set of used GlobalCMT ids
	 */
	public Set<GlobalCMTID> idSet() {
		return eventVarianceMap.keySet();
	}

	 /**
     * If the born waveform already is computed and in a file then, read and
     * return it. If not, this method computes a born waveform and returns it
     * and write in a certain folder.
     *
     * @param id     of the target raypath
     * @param method of inversion
     * @param n      number of eigen vectors(SVD) or CG vectors (CG) or...<br>
     *               if it is 1 and method is CG then CG1
     * @return Trace of Born
     * @throws IOException if any
     */
	public Trace bornOf(BasicID id, InverseMethodEnum method, int n) throws IOException {
		String txtname = getTxtName(id);
		Path bornPath = rootPath.resolve("born/" + method + n + "/" + txtname);
		if (Files.exists(bornPath))
			return readBORNTrace(id, method, n);

		Files.createDirectories(bornPath.getParent());
		Trace syn = syntheticOf_noorder(id); // TODO check the use of noorder
		if (syn == null) return null;
		Map<UnknownParameter, Double> answer = answerMapOfX(method, n);
		Trace born = syn;

		for (UnknownParameter par : unknownParameterList)
			born = born.add(partialOf(id, par).multiply(answer.get(par)));
		writeBorn(bornPath, born);
		
		return born;
	}
	
	/**
	 * @param id
	 * @param method
	 * @param n
	 * @param type
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	public Trace bornOf(BasicID id, InverseMethodEnum method, int n, PartialType type) throws IOException {
		String txtname = getTxtName(id);
		Path bornPath = rootPath.resolve("born/" + method + n + "_" + type + "/" + txtname);
		if (Files.exists(bornPath))
			return readBORNTrace(id, method, n, type);

		Files.createDirectories(bornPath.getParent());
		Trace syn = syntheticOf(id);
		Map<UnknownParameter, Double> answer = answerMapOfX(method, n);
		Trace born = syn;

		for (UnknownParameter par : unknownParameterList) {
			if (par.getPartialType().equals(type))
				born = born.add(partialOf(id, par).multiply(answer.get(par)));
		}
		writeBorn(bornPath, born);
		
		return born;
	}
	
	/**
	 * @param id
	 * @param method
	 * @param n
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	public Trace bornOf_spc(BasicID id, InverseMethodEnum method, int n) throws IOException {
		String txtname = getTxtName(id);
		Path bornPath = rootPath.resolve("born_spc/" + method + n + "/" + txtname);
		if (Files.exists(bornPath))
			return readBORNTrace_spc(id, method, n);

		Files.createDirectories(bornPath.getParent());
		Trace syn = syntheticOf_noorder_spc(id); // TODO check the use of noorder
		if (syn == null) return null;
		Map<UnknownParameter, Double> answer = answerMapOfX(method, n);
		Trace born = syn;

		for (UnknownParameter par : unknownParameterList)
			born = born.add(partialOf_noorder_spc(id, par).multiply(answer.get(par))); // TODO check noorder
		writeBorn(bornPath, born);
		
		return born;
	}
	
	/**
	 * @param id
	 * @param method
	 * @param n
	 * @param type
	 * @return
	 * @throws IOException
	 * @author anselme
	 */
	public Trace bornOf_spc(BasicID id, InverseMethodEnum method, int n, PartialType type) throws IOException {
		String txtname = getTxtName(id);
		Path bornPath = rootPath.resolve("born_spc/" + method + n + "_" + type + "/" + txtname);
		if (Files.exists(bornPath))
			return readBORNTrace_spc(id, method, n, type);

		Files.createDirectories(bornPath.getParent());
		Trace syn = syntheticOf_noorder_spc(id);
		if (syn == null) return null;
		Map<UnknownParameter, Double> answer = answerMapOfX(method, n);
		Trace born = syn;
		
		for (UnknownParameter par : unknownParameterList) {
			if (par.getPartialType().equals(type)) {
				born = born.add(partialOf_noorder_spc(id, par).multiply(answer.get(par)));
			}
		}
		writeBorn(bornPath, born);
		
		return born;
	}
	
    /**
     * Computes born waveforms of the answer by using n basis vector in the
     * method for all time windows. This method also creates 'eachVariance',
     * 'eventVariance', 'stationVariance' (.txt)
     *
     * @param method to solve an equation
     * @param n      the number of used basis vector
     * @throws IOException if an I/O error occurs
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
				RealVector obs = obsMap.get(s);
				RealVector del = bornMap.get(s).subtract(obs);
				stationLines.add(s + " " + del.dotProduct(del) / obs.dotProduct(obs));
			}
			Files.write(station, stationLines);
		}
	}

	public double[] getUnkownParameterWeights() throws IOException {
		if (!Files.exists(rootPath.resolve("unknownParameterWeigths.inf"))) {
//			System.out.println("Warning: no file unknownParameterWeigths.inf");
			return null;
		}
		return Files.readAllLines(rootPath.resolve("unknownParameterWeigths.inf"))
				.stream().mapToDouble(Double::parseDouble).toArray();
	}
	
	public Path getRootPath() {
		return rootPath;
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
	public double getInitialVariance() {
		return answerVarianceMap.get(InverseMethodEnum.CONJUGATE_GRADIENT)[0];
	}

    /**
     * @param a assumed redundancy in data points. It is used as n/a, where n
     *          is the number of data points, note that n/a will be
     *          (int)(n/a).
     * @return the Akaike Information criterion for the initial model. (k=0)
     */
	public double getInitialAIC(double a) {
		return Utilities.computeAIC(getInitialVariance(), (int) (npts / a), 0);
	}
	
	public void set_mul(double mul) {
		this.mul = Math.sqrt(mul);
	}
	
}
