package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Am=d のdに対する情報 TODO 震源観測点ペア
 * <p>
 * basicDataFileから Dvectorを構築する
 * <p>
 * This class is <b>immutable</b>.
 * <p>
 * TODO 同じ震源観測点ペアの波形も周波数やタイムウインドウによってあり得るから それに対処 varianceも
 *
 * @author Kensuke Konishi
 * @version 0.2.2.2
 */
public class Dvector {

    /**
     * Predicate for choosing dataset. Observed IDs are used for the choice.
     */
    private final Predicate<BasicID> CHOOSER;
    private final BasicID[] BASICIDS;
    /**
     * Function for weighting of each timewindow with IDs.
     */
    private final ToDoubleBiFunction<BasicID, BasicID> WEIGHTING_FUNCTION;
    /**
     * 残差波形のベクトル（各IDに対するタイムウインドウ）
     */
    private RealVector[] dVectors;

    /**
     * 残差波形のベクトル Vector <i>&delta;d</i>
     */
    private RealVector dVector;

    //TODO
    RealVector computeJlnA(BasicID id) {
        RealVector u = synVectors[whichTimewindow(id)];
        double h = 1 / id.getSamplingHz();
        double denominator = u.dotProduct(u) * h;
        return u.mapDivide(denominator);
    }

    //TODO
    RealVector computeJtime(BasicID id) {
        RealVector u = synVectors[whichTimewindow(id)];
        double h = 1 / id.getSamplingHz();
        RealVector partial = derivative(u, h);
        double denominator = partial.dotProduct(partial) * h;
        return partial.mapDivide(-denominator);
    }

    //TODO
    private static RealVector derivative(RealVector u, double h) {
        int length = u.getDimension();
        RealVector par = new ArrayRealVector(length);
        //forward difference
        for (int i = 0; i < length - 1; i++)
            par.setEntry(i, (u.getEntry(i + 1) - u.getEntry(i)) / h);
        par.setEntry(length - 1, par.getEntry(length - 2));
        return par;
    }


    /**
     * イベントごとのvariance
     */
    private Map<GlobalCMTID, Double> eventVariance;
    /**
     * Number of data points
     */
    private int npts;
    /**
     * Number of timewindow
     */
    private int nTimeWindow;
    /**
     * 観測波形の波形情報
     */
    private BasicID[] obsIDs;
    /**
     * obs vector of each time window
     */
    private RealVector[] obsVectors;
    /**
     * Vector obs
     */
    private RealVector obsVector;
    /**
     * それぞれのタイムウインドウが,全体の中の何点目から始まるか
     */
    private int[] startPoints;
    /**
     * Map of variance of the dataset for a station
     */
    private Map<Station, Double> stationVariance;
    /**
     * Synthetic
     */
    private BasicID[] synIDs;
    /**
     * syn vector of each time window
     */
    private RealVector[] synVectors;
    /**
     * Vector syn
     */
    private RealVector synVector;
    /**
     * Set of global CMT IDs read in vector
     */
    private Set<GlobalCMTID> usedGlobalCMTIDset;
    /**
     * Set of stations used in vector.
     */
    private Set<Station> usedStationSet;
    /**
     * weighting for i th timewindow.
     */
    private double[] weighting;
    /**
     * Variance of dataset |obs-syn|<sup>2</sup>/|obs|<sup>2</sup>
     */
    private double variance;
    /**
     * L<sub>2</sub> norm of OBS
     */
    private double obsNorm;
    /**
     * L<sub>2</sub> norm of OBS-SYN
     */
    private double dNorm;

    /**
     * Use all waveforms in the IDs. Weighting factor is the reciprocal of the maximum
     * value in each obs time window.
     *
     * @param basicIDs must contain waveform data
     */
    public Dvector(BasicID[] basicIDs) {
        this(basicIDs, id -> true, null);
    }

    /**
     * Use selected waveforms.
     * Weighting factor is reciprocal of maximum value in each obs time window.
     *
     * @param basicIDs must contain waveform data
     * @param chooser  {@link Predicate} for selection of obs data to be used
     */
    public Dvector(BasicID[] basicIDs, Predicate<BasicID> chooser) {
        this(basicIDs, chooser, null);
    }

    /**
     * Use selected waveforms.
     *
     * @param basicIDs          must contain waveform data
     * @param chooser           {@link Predicate} used for filtering Observed (not synthetic)
     *                          ID. If one ID is true, then the observed ID and the pair
     *                          synthetic are used.
     * @param weightingFunction {@link ToDoubleBiFunction} (observed, synthetic). If null, the reciprocal of the max value in observed is a weighting value.
     */
    public Dvector(BasicID[] basicIDs, Predicate<BasicID> chooser,
                   ToDoubleBiFunction<BasicID, BasicID> weightingFunction) {
        if (!check(basicIDs)) throw new RuntimeException("Input IDs do not have waveform data.");
        BASICIDS = basicIDs.clone();
        CHOOSER = chooser;
        if (weightingFunction != null) WEIGHTING_FUNCTION = weightingFunction;
        else WEIGHTING_FUNCTION = (obs, syn) -> {
            RealVector obsVec = new ArrayRealVector(obs.getData(), false);
            return 1 / obsVec.getLInfNorm();
        };
        sort();
        read();
    }

    /**
     * @param ids for check
     * @return if all the BASICIDS have waveform data.
     */
    private static boolean check(BasicID[] ids) {
        return Arrays.stream(ids).parallel().allMatch(BasicID::containsData);
    }

    /**
     * compare id0 and id1 if component npts sampling Hz start time max min
     * period station global cmt id are same This method ignore if
     * the input IDs are observed or synthetic. TODO start time
     *
     * @param id0 {@link BasicID}
     * @param id1 {@link BasicID}
     * @return if the IDs are same
     */
    private static boolean isPair(BasicID id0, BasicID id1) {
        return id0.getStation().equals(id1.getStation()) && id0.getGlobalCMTID().equals(id1.getGlobalCMTID()) &&
                id0.getSacComponent() == id1.getSacComponent() && id0.getNpts() == id1.getNpts() &&
                id0.getSamplingHz() == id1.getSamplingHz() && Math.abs(id0.getStartTime() - id1.getStartTime()) < 20 &&
                id0.getMaxPeriod() == id1.getMaxPeriod() && id0.getMinPeriod() == id1.getMinPeriod();
    }

    /**
     * @return map of variance of waveforms in each event
     */
    public Map<GlobalCMTID, Double> getEventVariance() {
        return eventVariance;
    }

    /**
     * @return map of variance of waveforms for each station
     */
    public Map<Station, Double> getStationVariance() {
        return stationVariance;
    }

    /**
     * Every vector must have the same length as the corresponding timewindow.
     *
     * @param vectors to combine
     * @return combined vectors
     */
    public RealVector combine(RealVector[] vectors) {
        if (vectors.length != nTimeWindow) throw new RuntimeException("the number of input vector is invalid");
        for (int i = 0; i < nTimeWindow; i++)
            if (vectors[i].getDimension() != obsVectors[i].getDimension())
                throw new RuntimeException("input vector is invalid");

        RealVector v = new ArrayRealVector(npts);
        for (int i = 0; i < nTimeWindow; i++)
            v.setSubVector(startPoints[i], vectors[i]);

        return v;
    }

    /**
     * The returning vector is unmodifiable.
     *
     * @return Vectors consisting of dvector(obs-syn). Each vector is each
     * timewindow. If you want to get the vector D, you may use
     * {@link #combine(RealVector[])}
     */
    public RealVector[] getDVectors() {
        return dVectors.clone();
    }

    /**
     * @return vectors of residual between observed and synthetics (obs-syn)
     */
    public RealVector getD() {
        return dVector;
    }

    /**
     * @return an array of each time window length.
     */
    public int[] getLengths() {
        return Arrays.stream(obsVectors).mapToInt(RealVector::getDimension).toArray();
    }

    /**
     * @return number of total data.
     */
    public int getNpts() {
        return npts;
    }

    /**
     * @return number of timewindows
     */
    public int getNTimeWindow() {
        return nTimeWindow;
    }

    public BasicID[] getObsIDs() {
        return obsIDs.clone();
    }

    public RealVector[] getObsVectors() {
        return obsVectors.clone();
    }

    /**
     * @return vector of observed waveforms
     */
    public RealVector getObs() {
        return obsVector;
    }

    /**
     * @param i index of timewindow
     * @return the index of start point where the i th timewindow starts
     */
    public int getStartPoints(int i) {
        return startPoints[i];
    }

    public BasicID[] getSynIDs() {
        return synIDs.clone();
    }

    public RealVector[] getSynVectors() {
        return synVectors.clone();
    }

    /**
     * @return vector of synthetic waveforms.
     */
    public RealVector getSyn() {
        return synVector;
    }

    public Set<GlobalCMTID> getUsedGlobalCMTIDset() {
        return usedGlobalCMTIDset;
    }

    /**
     * @return set of stations in vector
     */
    public Set<Station> getUsedStationSet() {
        return usedStationSet;
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
     * @param outPath Path for write
     * @throws IOException if an I/O error occurs
     */
    public void outOrder(Path outPath) throws IOException {
        Path order = outPath.resolve("order.inf");
        try (PrintWriter pwOrder = new PrintWriter(Files.newBufferedWriter(order))) {
            pwOrder.println(
                    "#num sta id comp type obsStartT npts samplHz minPeriod maxPeriod startByte conv startPointOfVector synStartT weight");
            for (int i = 0; i < nTimeWindow; i++)
                pwOrder.println(i + " " + obsIDs[i] + " " + getStartPoints(i) + " " + synIDs[i].getStartTime() + " " +
                        weighting[i]);
        }
    }

    /**
     * vectors（各タイムウインドウ）に対して、観測波形とのvarianceを求めてファイルに書き出す
     * Create event folders under the outPath and variances are written for each path.
     *
     * @param outPath Root for the write
     * @param vectors {@link RealVector}s for write
     * @throws IOException if an I/O error occurs
     */
    public void outputVarianceOf(Path outPath, RealVector[] vectors) throws IOException {
        Files.createDirectories(outPath);
        Map<Station, Double> stationDenominator = usedStationSet.stream().collect(Collectors.toMap(s -> s, s -> 0d));
        Map<Station, Double> stationNumerator = usedStationSet.stream().collect(Collectors.toMap(s -> s, s -> 0d));
        Map<GlobalCMTID, Double> eventDenominator =
                usedGlobalCMTIDset.stream().collect(Collectors.toMap(id -> id, id -> 0d));
        Map<GlobalCMTID, Double> eventNumerator =
                usedGlobalCMTIDset.stream().collect(Collectors.toMap(id -> id, id -> 0d));

        Path eachVariancePath = outPath.resolve("eachVariance.txt");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(eachVariancePath))) {
            for (int i = 0; i < nTimeWindow; i++) {
                Station station = obsIDs[i].getStation();
                GlobalCMTID id = obsIDs[i].getGlobalCMTID();
                double obs2 = obsVectors[i].dotProduct(obsVectors[i]);
                RealVector del = vectors[i].subtract(obsVectors[i]);
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

    private void read() {
        // double t = System.nanoTime();
        int start = 0;
        Map<Station, Double> stationDenominator = usedStationSet.stream().collect(Collectors.toMap(s -> s, s -> 0.0));
        Map<Station, Double> stationNumerator = usedStationSet.stream().collect(Collectors.toMap(s -> s, s -> 0.0));
        Map<GlobalCMTID, Double> eventDenominator =
                usedGlobalCMTIDset.stream().collect(Collectors.toMap(id -> id, id -> 0d));
        Map<GlobalCMTID, Double> eventNumerator =
                usedGlobalCMTIDset.stream().collect(Collectors.toMap(id -> id, id -> 0d));
        double obs2 = 0;
        for (int i = 0; i < nTimeWindow; i++) {
            startPoints[i] = start;
            int npts = obsIDs[i].getNpts();
            this.npts += npts;
            start += npts;

            // read obs
            obsVectors[i] = new ArrayRealVector(obsIDs[i].getData(), false);

            // apply weighting
            weighting[i] = WEIGHTING_FUNCTION.applyAsDouble(obsIDs[i], synIDs[i]);

            obsVectors[i] = obsVectors[i].mapMultiply(weighting[i]);

            // read syn
            synVectors[i] = new ArrayRealVector(synIDs[i].getData(), false);
            synVectors[i].mapMultiplyToSelf(weighting[i]);

            double denominator = obsVectors[i].dotProduct(obsVectors[i]);
            dVectors[i] = obsVectors[i].subtract(synVectors[i]);
            double numerator = dVectors[i].dotProduct(dVectors[i]);
            stationDenominator
                    .put(obsIDs[i].getStation(), stationDenominator.get(obsIDs[i].getStation()) + denominator);
            stationNumerator.put(obsIDs[i].getStation(), stationNumerator.get(obsIDs[i].getStation()) + numerator);
            eventDenominator
                    .put(obsIDs[i].getGlobalCMTID(), eventDenominator.get(obsIDs[i].getGlobalCMTID()) + denominator);
            eventNumerator.put(obsIDs[i].getGlobalCMTID(), eventNumerator.get(obsIDs[i].getGlobalCMTID()) + numerator);


            obsVectors[i] = RealVector.unmodifiableRealVector(obsVectors[i]);
            synVectors[i] = RealVector.unmodifiableRealVector(synVectors[i]);
            dVectors[i] = RealVector.unmodifiableRealVector(dVectors[i]);

            variance += numerator;
            obs2 += denominator;
        }
        stationVariance = Collections.unmodifiableMap(usedStationSet.stream()
                .collect(Collectors.toMap(s -> s, s -> stationNumerator.get(s) / stationDenominator.get(s))));
        eventVariance = Collections.unmodifiableMap(usedGlobalCMTIDset.stream()
                .collect(Collectors.toMap(id -> id, id -> eventNumerator.get(id) / eventDenominator.get(id))));
        dNorm = Math.sqrt(variance);
        variance /= obs2;
        obsNorm = Math.sqrt(obs2);
        dVector = RealVector.unmodifiableRealVector(combine(dVectors));
        obsVector = RealVector.unmodifiableRealVector(combine(obsVectors));
        synVector = RealVector.unmodifiableRealVector(combine(synVectors));
        System.err.println(nTimeWindow + " timewindows were used to create a vector D. The variance is " + variance +
                ". The number of points is " + npts);
    }

    /**
     * @return &sum;|obs-syn|<sup>2</sup>/&sum;|obs|<sup>2</sup>
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
     * @param vector to separate
     * @return Separated vectors for each time window. Error occurs if the input is invalid.
     */
    public RealVector[] separate(RealVector vector) {
        if (vector.getDimension() != npts)
            throw new RuntimeException("the length of input vector is invalid." + " " + vector.getDimension());
        RealVector[] vectors = new RealVector[nTimeWindow];
        Arrays.setAll(vectors, i -> vector.getSubVector(startPoints[i], obsVectors[i].getDimension()));
        return vectors;
    }

    /**
     * Look for data which can be used. Existence of duplication throws an exception.
     */
    private void sort() {
        // list obs IDs
        List<BasicID> obsList =
                Arrays.stream(BASICIDS).filter(id -> id.getWaveformType() == WaveformType.OBS).filter(CHOOSER)
                        .collect(Collectors.toList());

        // Duplication check
        for (int i = 0; i < obsList.size(); i++)
            for (int j = i + 1; j < obsList.size(); j++)
                if (obsList.get(i).equals(obsList.get(j))) throw new RuntimeException("Duplicate observed detected");

        // list syn
        List<BasicID> synList =
                Arrays.stream(BASICIDS).filter(id -> id.getWaveformType() == WaveformType.SYN).filter(CHOOSER)
                        .collect(Collectors.toList());

        // Duplication check
        for (int i = 0; i < synList.size(); i++)
            for (int j = i + 1; j < synList.size(); j++)
                if (synList.get(i).equals(synList.get(j))) throw new RuntimeException("Duplicate synthetic detected");


        if (obsList.size() != synList.size()) System.err.println(
                "The numbers of observed IDs " + obsList.size() + " and synthetic IDs " + synList.size() +
                        " are different.");
        int size = obsList.size() < synList.size() ? synList.size() : obsList.size();

        List<BasicID> useObsList = new ArrayList<>(size);
        List<BasicID> useSynList = new ArrayList<>(size);

        for (BasicID syn : synList)
            for (BasicID obs : obsList)
                if (isPair(syn, obs)) {
                    useObsList.add(obs);
                    useSynList.add(syn);
                    break;
                }

        if (useObsList.size() != useSynList.size()) throw new RuntimeException("unanticipated");

        nTimeWindow = useSynList.size();
        obsIDs = useObsList.toArray(new BasicID[nTimeWindow]);
        synIDs = useSynList.toArray(new BasicID[nTimeWindow]);

        weighting = new double[nTimeWindow];
        startPoints = new int[nTimeWindow];
        obsVectors = new RealVector[nTimeWindow];
        synVectors = new RealVector[nTimeWindow];
        dVectors = new RealVector[nTimeWindow];
        usedGlobalCMTIDset = new HashSet<>();
        usedStationSet = new HashSet<>();
        for (BasicID obsID : obsIDs) {
            usedStationSet.add(obsID.getStation());
            usedGlobalCMTIDset.add(obsID.getGlobalCMTID());
        }
        usedStationSet = Collections.unmodifiableSet(usedStationSet);
        usedGlobalCMTIDset = Collections.unmodifiableSet(usedGlobalCMTIDset);
    }

    /**
     * Look for the index for the input ID.
     * If the input is obs, the search is for obs, while if the input is syn or partial, the search is in syn.
     *
     * @param id {@link BasicID}
     * @return index for the ID. -1 if no ID found.
     */
    int whichTimewindow(BasicID id) {
        BasicID[] ids = id.getWaveformType() == WaveformType.OBS ? obsIDs : synIDs;
        return IntStream.range(0, ids.length).filter(i -> isPair(id, ids[i])).findAny().orElse(-1);
    }
}
