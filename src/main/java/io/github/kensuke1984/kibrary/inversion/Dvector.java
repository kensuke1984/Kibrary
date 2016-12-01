package io.github.kensuke1984.kibrary.inversion;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;

/**
 * Am=d のdに対する情報 TODO 震源観測点ペア
 * 
 * basicDataFileから Dvectorを構築する
 * 
 * 
 * TODO 同じ震源観測点ペアの波形も周波数やタイムウインドウによってあり得るから それに対処 varianceも
 * 
 * @version 0.2.1.2
 * 
 * @author Kensuke Konishi
 */
public class Dvector {

	/**
	 * @param ids
	 *            for check
	 * @return if all the ids have waveform data.
	 */
	private static boolean check(BasicID[] ids) {
		return Arrays.stream(ids).parallel().allMatch(BasicID::containsData);
	}

	/**
	 * compare id0 and id1 if component npts sampling Hz start time max min
	 * period station global cmt id are same This method does NOT consider if
	 * the input ids are observed or synthetic. TODO start time
	 * 
	 * @param id0
	 *            {@link BasicID}
	 * @param id1
	 *            {@link BasicID}
	 * @return if the ids are same （理論波形と観測波形は違うけど＾＾＠）
	 */
	private static boolean isPair(BasicID id0, BasicID id1) {
		return id0.getStation().equals(id1.getStation()) && id0.getGlobalCMTID().equals(id1.getGlobalCMTID())
				&& id0.getSacComponent() == id1.getSacComponent() && id0.getNpts() == id1.getNpts()
				&& id0.getSamplingHz() == id1.getSamplingHz() && Math.abs(id0.getStartTime() - id1.getStartTime()) < 20
				&& id0.getMaxPeriod() == id1.getMaxPeriod() && id0.getMinPeriod() == id1.getMinPeriod();
	}

	/**
	 * Predicate for choosing dataset. Observed IDs are used for the choice.
	 */
	private Predicate<BasicID> chooser;

	/**
	 * 残差波形のベクトル（各IDに対するタイムウインドウ）
	 */
	private RealVector[] dVec;

	/**
	 * イベントごとのvariance
	 */
	private Map<GlobalCMTID, Double> eventVariance;

	private BasicID[] ids;

	/**
	 * dの長さ (トータルのポイント数)
	 */
	private int npts;

	/**
	 * 含まれるタイムウインドウ数
	 */
	private int nTimeWindow;

	/**
	 * 観測波形の波形情報
	 */
	private BasicID[] obsIDs;

	/**
	 * 観測波形のベクトル（各IDに対するタイムウインドウ）
	 */
	private RealVector[] obsVec;

	/**
	 * それぞれのタイムウインドウが,全体の中の何点目から始まるか
	 */
	private int[] startPoints;

	/**
	 * Map of variance of the dataset for a station
	 */
	private Map<Station, Double> stationVariance;

	/**
	 * @return map of variance of waveforms in each event
	 */
	public Map<GlobalCMTID, Double> getEventVariance() {
		return Collections.unmodifiableMap(eventVariance);
	}

	/**
	 * @return map of variance of waveforms for each station
	 */
	public Map<Station, Double> getStationVariance() {
		return Collections.unmodifiableMap(stationVariance);
	}

	/**
	 * 観測波形の波形情報
	 */
	private BasicID[] synIDs;

	/**
	 * 理論波形のベクトル（各IDに対するタイムウインドウ）
	 */
	private RealVector[] synVec;

	/**
	 * Set of global CMT IDs read in vector
	 */
	private Set<GlobalCMTID> usedGlobalCMTIDset;

	/**
	 * Set of stations read in vector.
	 */
	private Set<Station> usedStationSet;

	/**
	 * weighting for i th timewindow.
	 */
	private double[] weighting;

	/**
	 * Function for weighting of each timewindow with IDs.
	 */
	private ToDoubleBiFunction<BasicID, BasicID> weightingFunction;
	
	private double[] lowerUpperMantleWeighting;

	/**
	 * Use all waveforms in the IDs Weighting factor is reciprocal of maximum
	 * value in each obs time window
	 * 
	 * @param basicIDs
	 *            must contain waveform data
	 */
	public Dvector(BasicID[] basicIDs) {
		this(basicIDs, id -> true, (ToDoubleBiFunction<BasicID, BasicID>) null);
	}

	/**
	 * chooserを通った波形のみを使う 観測波形を選別しその観測波形に対する理論波形を用いる Weighting factor is
	 * reciprocal of maximum value in each obs time window
	 * 
	 * @param basicIDs
	 *            must contain waveform data
	 * @param chooser
	 *            {@link Predicate}
	 */
	public Dvector(BasicID[] basicIDs, Predicate<BasicID> chooser) {
		this(basicIDs, chooser, (ToDoubleBiFunction<BasicID, BasicID>) null);
	}

	/**
	 * 
	 * chooserを通った観測波形のみを使う 観測波形を選別しその観測波形に対する理論波形を用いる
	 * 
	 * @param basicIDs
	 *            must contain waveform data
	 * @param chooser
	 *            {@link Predicate} used for filtering Observed (not synthetic)
	 *            ID if one ID is true, then the observed ID and the pair
	 *            synethetic are used.
	 * @param weightingFunction
	 *            {@link ToDoubleBiFunction} (observed, synthetic)
	 */
	public Dvector(BasicID[] basicIDs, Predicate<BasicID> chooser,
			ToDoubleBiFunction<BasicID, BasicID> weightingFunction) {
		ids = basicIDs;
		if (!check(ids))
			throw new RuntimeException("Input IDs do not have waveform data.");
		// System.exit(0);
		this.chooser = chooser;
		if (weightingFunction != null)
			this.weightingFunction = weightingFunction;
		else
			this.weightingFunction = (obs, syn) -> {
				RealVector obsVec = new ArrayRealVector(obs.getData(), false);
				return 1. / Math.max(Math.abs(obsVec.getMinValue()), Math.abs(obsVec.getMaxValue()));
			};
		sort();
		read();
	}
	
	public Dvector(BasicID[] basicIDs, Predicate<BasicID> chooser,
			double[] lowerUpperMantleWeighting) {
		ids = basicIDs;
		if (!check(ids))
			throw new RuntimeException("Input IDs do not have waveform data.");
		// System.exit(0);
		this.chooser = chooser;
		this.lowerUpperMantleWeighting = lowerUpperMantleWeighting;
		this.weightingFunction = (obs, syn) -> {
			return 1.;
		};
		sort();
		read();
	}

	/**
	 * vectorsがtimewindowの数とそれぞれの要素数を守っていないとerror
	 * 
	 * @param vectors
	 *            to combine
	 * @return vectorsをつなげる
	 */
	public RealVector combine(RealVector[] vectors) {
		if (vectors.length != nTimeWindow)
			throw new RuntimeException("the number of input vector is invalid");
		for (int i = 0; i < nTimeWindow; i++)
			if (vectors[i].getDimension() != obsVec[i].getDimension())
				throw new RuntimeException("input vector is invalid");

		RealVector v = new ArrayRealVector(npts);
		for (int i = 0; i < nTimeWindow; i++)
			v.setSubVector(startPoints[i], vectors[i]);

		return v;
	}

	/**
	 * @return Vectors consisting of dvector(obs-syn). Each vector is each
	 *         timewindow. If you want to get the vector D, you may use
	 *         {@link #combine(RealVector[])}
	 */
	public RealVector[] getdVec() {
		return dVec.clone();
	}

	/**
	 * @return vectors of residual between observed and synthetics (obs-syn)
	 */
	public RealVector getD() {
		return combine(dVec);
	}

	/**
	 * @return 各タイムウインドウの長さ
	 */
	public int[] getLengths() {
		return IntStream.range(0, nTimeWindow).map(i -> obsVec[i].getDimension()).toArray();
	}

	/**
	 * @return 総データポイント数
	 */
	public int getNpts() {
		return npts;
	}

	/**
	 * @return タイムウインドウの数
	 */
	public int getNTimeWindow() {
		return nTimeWindow;
	}

	public BasicID[] getObsIDs() {
		return obsIDs.clone();
	}

	public RealVector[] getObsVec() {
		return obsVec.clone();
	}

	/**
	 * @return vector of observed waveforms
	 */
	public RealVector getObs() {
		return combine(obsVec);
	}

	/**
	 * i番目のウインドウが何ポイント目から始まるか
	 * 
	 * @param i
	 *            index of timewindow
	 * @return point where the i th timewindow starts
	 */
	public int getStartPoints(int i) {
		return startPoints[i];
	}

	public BasicID[] getSynIDs() {
		return synIDs.clone();
	}

	public RealVector[] getSynVec() {
		return synVec.clone();
	}

	/**
	 * @return vector of synthetic waveforms.
	 */
	public RealVector getSyn() {
		return combine(synVec);
	}

	public Set<GlobalCMTID> getUsedGlobalCMTIDset() {
		return Collections.unmodifiableSet(usedGlobalCMTIDset);
	}

	/**
	 * @return set of stations in vector
	 */
	public Set<Station> getUsedStationSet() {
		return Collections.unmodifiableSet(usedStationSet);
	}

	/**
	 * @return weighting for the i th timewindow.
	 */
	public double getWeighting(int i) {
		return weighting[i];
	}

	/**
	 * syn.dat del.dat obs.dat obsOrder synOrder.datを outDirectory下に書き込む
	 * 
	 * @param outPath
	 *            Path for output
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void outOrder(Path outPath) throws IOException {
		Path order = outPath.resolve("order.inf");
		try (PrintWriter pwOrder = new PrintWriter(Files.newBufferedWriter(order))) {
			pwOrder.println(
					"#num sta id comp type obsStartT npts samplHz minPeriod maxPeriod startByte conv startPointOfVector synStartT weight");
			for (int i = 0; i < nTimeWindow; i++)
				pwOrder.println(i + " " + obsIDs[i] + " " + getStartPoints(i) + " " + synIDs[i].getStartTime() + " "
						+ weighting[i]);
		}
	}

	/**
	 * vectors（各タイムウインドウ）に対して、観測波形とのvarianceを求めてファイルに書き出す
	 * outDir下にイベントフォルダを作りその下に書くステーションごとに書き込む
	 * 
	 * @param outPath
	 *            書き出すフォルダ
	 * @param vectors
	 *            {@link RealVector}s for output
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void outputVarianceOf(Path outPath, RealVector[] vectors) throws IOException {
		Files.createDirectories(outPath);
		Map<Station, Double> stationDenominator = usedStationSet.stream().collect(Collectors.toMap(s -> s, s -> 0.0));
		Map<Station, Double> stationNumerator = usedStationSet.stream().collect(Collectors.toMap(s -> s, s -> 0.0));
		Map<GlobalCMTID, Double> eventDenominator = usedGlobalCMTIDset.stream()
				.collect(Collectors.toMap(id -> id, id -> 0d));
		Map<GlobalCMTID, Double> eventNumerator = usedGlobalCMTIDset.stream()
				.collect(Collectors.toMap(id -> id, id -> 0d));
		usedStationSet.stream().collect(Collectors.toMap(s -> s, s -> 0d));

		Path eachVariancePath = outPath.resolve("eachVariance.txt");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(eachVariancePath))) {
			for (int i = 0; i < nTimeWindow; i++) {
				Station station = obsIDs[i].getStation();
				GlobalCMTID id = obsIDs[i].getGlobalCMTID();
				double obs2 = obsVec[i].dotProduct(obsVec[i]);
				RealVector del = vectors[i].subtract(obsVec[i]);
				double del2 = del.dotProduct(del);
				eventDenominator.put(id, eventDenominator.get(id) + obs2);
				stationDenominator.put(station, stationDenominator.get(station) + obs2);

				eventNumerator.put(id, eventNumerator.get(id) + del2);
				stationNumerator.put(station, stationNumerator.get(station) + del2);
				pw.println(i + " " + station + " " + id + " " + del2 / obs2);
			}
		}

		Path eventVariance = outPath.resolve("eventVariance.txt");
		Path stationVariance = outPath.resolve("stationVariance.txt");
		try (PrintWriter pwEvent = new PrintWriter(Files.newBufferedWriter(eventVariance));
				PrintWriter pwStation = new PrintWriter(Files.newBufferedWriter(stationVariance))) {
			usedGlobalCMTIDset
					.forEach(id -> pwEvent.println(id + " " + eventNumerator.get(id) / eventDenominator.get(id)));
			usedStationSet.forEach(station -> pwStation
					.println(station + " " + stationNumerator.get(station) / stationDenominator.get(station)));

		}
	}

	/**
	 * Variance of dataset |obs-syn|<sup>2</sup>/|obs|<sup>2</sup>
	 */
	private double variance;
	/**
	 * |obs|
	 */
	private double obsNorm;

	/**
	 * |obs-syn|
	 */
	private double dNorm;

	private void read() {
		// double t = System.nanoTime();
		int start = 0;
		Map<Station, Double> stationDenominator = usedStationSet.stream().collect(Collectors.toMap(s -> s, s -> 0.0));
		Map<Station, Double> stationNumerator = usedStationSet.stream().collect(Collectors.toMap(s -> s, s -> 0.0));
		Map<GlobalCMTID, Double> eventDenominator = usedGlobalCMTIDset.stream()
				.collect(Collectors.toMap(id -> id, id -> 0d));
		Map<GlobalCMTID, Double> eventNumerator = usedGlobalCMTIDset.stream()
				.collect(Collectors.toMap(id -> id, id -> 0d));
		double obs2 = 0;
		for (int i = 0; i < nTimeWindow; i++) {
			startPoints[i] = start;
			int npts = obsIDs[i].getNpts();
			this.npts += npts;
			start += npts;

			// 観測波形の読み込み
			obsVec[i] = new ArrayRealVector(obsIDs[i].getData(), false);

			// 観測波形の最大値の逆数で重み付け TODO 重み付けの方法を決める
			if (lowerUpperMantleWeighting != null)
				weighting[i] = new Phases(obsIDs[i].getPhases()).isLowerMantle() ? 
						lowerUpperMantleWeighting[0] : lowerUpperMantleWeighting[1];
			else
				weighting[i] = weightingFunction.applyAsDouble(obsIDs[i], synIDs[i]);

			obsVec[i] = obsVec[i].mapMultiply(weighting[i]);

			// 理論波形の読み込み
			synVec[i] = new ArrayRealVector(synIDs[i].getData(), false);
			synVec[i].mapMultiplyToSelf(weighting[i]);

			double denominator = obsVec[i].dotProduct(obsVec[i]);
			dVec[i] = obsVec[i].subtract(synVec[i]);
			double numerator = dVec[i].dotProduct(dVec[i]);
			stationDenominator.put(obsIDs[i].getStation(),
					stationDenominator.get(obsIDs[i].getStation()) + denominator);
			stationNumerator.put(obsIDs[i].getStation(), stationNumerator.get(obsIDs[i].getStation()) + numerator);
			eventDenominator.put(obsIDs[i].getGlobalCMTID(),
					eventDenominator.get(obsIDs[i].getGlobalCMTID()) + denominator);
			eventNumerator.put(obsIDs[i].getGlobalCMTID(), eventNumerator.get(obsIDs[i].getGlobalCMTID()) + numerator);

			variance += numerator;
			obs2 += denominator;
		}
		stationVariance = usedStationSet.stream()
				.collect(Collectors.toMap(s -> s, s -> stationNumerator.get(s) / stationDenominator.get(s)));
		eventVariance = usedGlobalCMTIDset.stream()
				.collect(Collectors.toMap(id -> id, id -> eventNumerator.get(id) / eventDenominator.get(id)));
		dNorm = Math.sqrt(variance);
		variance /= obs2;
		obsNorm = Math.sqrt(obs2);
		System.err.println("Vector D was created. The variance is " + variance + ". The number of points is " + npts);
	}

	/**
	 * @return |obs-syn|**2/|obs|**2
	 */
	public double getVariance() {
		return variance;
	}

	/**
	 * @return |obs|
	 */
	public double getObsNorm() {
		return obsNorm;
	}

	/**
	 * @return |obs-syn|
	 */
	public double getDNorm() {
		return dNorm;
	}

	/**
	 * @param vector
	 *            to separate
	 * @return 入力したベクトルをタイムウインドウ毎に分ける 長さが違うとerror
	 */
	public RealVector[] separate(RealVector vector) {
		if (vector.getDimension() != npts)
			throw new RuntimeException("the length of input vector is invalid." + " " + vector.getDimension());
		RealVector[] vectors = new RealVector[nTimeWindow];
		Arrays.setAll(vectors, i -> vector.getSubVector(startPoints[i], obsVec[i].getDimension()));
		return vectors;
	}

	/**
	 * データを選り分ける 観測波形 理論波形両方ともにあるものだけを採用する 重複があったときには終了
	 */
	private void sort() {
		// //////
		// 観測波形の抽出 list observed IDs
		List<BasicID> obsList = Arrays.stream(ids).filter(id -> id.getWaveformType() == WaveformType.OBS)
				.filter(chooser::test).collect(Collectors.toList());

		// 重複チェック 重複が見つかればここから進まない
		for (int i = 0; i < obsList.size(); i++)
			for (int j = i + 1; j < obsList.size(); j++)
				if (obsList.get(i).equals(obsList.get(j)))
					throw new RuntimeException("Duplicate observed detected");

		// 理論波形の抽出
		List<BasicID> synList = Arrays.stream(ids).filter(id -> id.getWaveformType() == WaveformType.SYN)
				.filter(chooser::test).collect(Collectors.toList());

		// 重複チェック
		for (int i = 0; i < synList.size() - 1; i++)
			for (int j = i + 1; j < synList.size(); j++)
				if (synList.get(i).equals(synList.get(j)))
					throw new RuntimeException("Duplicate synthetic detected");

		// System.out.println("There are "+synList.size()+" synthetic IDs");

		// System.out.println(synList.size() +
		// " synthetic waveforms are found.");
		if (obsList.size() != synList.size())
			System.err.println("The numbers of observed IDs " + obsList.size() + " and " + " synthetic IDs "
					+ synList.size() + " are different ");
		int size = obsList.size() < synList.size() ? synList.size() : obsList.size();

		List<BasicID> useObsList = new ArrayList<>(size);
		List<BasicID> useSynList = new ArrayList<>(size);

		for (int i = 0; i < synList.size(); i++)
			for (int j = 0; j < obsList.size(); j++)
				if (isPair(synList.get(i), obsList.get(j))) {
					useObsList.add(obsList.get(j));
					useSynList.add(synList.get(i));
					break;
				}

		if (useObsList.size() != useSynList.size())
			throw new RuntimeException("unanticipated");
		// System.out.println(useObsList.size() + " observed and synthetic pairs
		// are used.");

		nTimeWindow = useSynList.size();
		obsIDs = useObsList.toArray(new BasicID[0]);
		synIDs = useSynList.toArray(new BasicID[0]);

		weighting = new double[nTimeWindow];
		startPoints = new int[nTimeWindow];
		obsVec = new RealVector[nTimeWindow];
		synVec = new RealVector[nTimeWindow];
		dVec = new RealVector[nTimeWindow];
		System.err.println(nTimeWindow + " timewindows are used");
		usedGlobalCMTIDset = new HashSet<>();
		usedStationSet = new HashSet<>();
		for (int i = 0; i < nTimeWindow; i++) {
			usedStationSet.add(obsIDs[i].getStation());
			usedGlobalCMTIDset.add(obsIDs[i].getGlobalCMTID());
		}
		
		if (lowerUpperMantleWeighting != null) {
			System.err.println("Using weighting for lower mantle (" + lowerUpperMantleWeighting[0] 
					+ ") and upper mantle (" + lowerUpperMantleWeighting[1] +")");
		}
	}

	/**
	 * idが何番目のタイムウインドウに等しいか 入力が観測波形なら観測波形のidとして理論か偏微分係数ならそっちから調べる なければ -1を返す
	 * 
	 * @param id
	 *            {@link BasicID}
	 * @return index for the id
	 */
	int whichTimewindow(BasicID id) {
		BasicID[] ids = id.getWaveformType() == WaveformType.OBS ? obsIDs : synIDs;
		return IntStream.range(0, ids.length).filter(i -> isPair(id, ids[i])).findAny().orElse(-1);
	}
}
