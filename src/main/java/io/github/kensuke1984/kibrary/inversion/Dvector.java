package io.github.kensuke1984.kibrary.inversion;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import io.github.kensuke1984.kibrary.quick.taupModelMaker;
import io.github.kensuke1984.kibrary.selection.DataSelectionInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

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

	public static void main(String[] args) throws IOException {
		Path idPath = Paths.get(args[0]);
		Path dataPath = Paths.get(args[1]);
		BasicID[] basicIDs = BasicIDFile.readBasicIDandDataFile(idPath, dataPath);
		Predicate<BasicID> chooser = new Predicate<BasicID>() {
			public boolean test(BasicID id) {
//				double distance = id.getGlobalCMTID().getEvent().getCmtLocation()
//						.getEpicentralDistance(id.getStation().getPosition())
//						* 180. / Math.PI;
//				if (distance > 100 || distance < 70)
//					return false;
//				if (id.getGlobalCMTID().getEvent().getCmt().getMw() < 6.5)
//					return false;
//				Set<GlobalCMTID> badIds = Stream.of(new String[] {"200707060109A","200711160312A","200711291900A","201008121154A","201407291046A"})
//						.map(name -> new GlobalCMTID(name)).collect(Collectors.toSet());
//				Set<GlobalCMTID> goodIds = Stream.of(new String[] {"200509260155A","200808262100A","201005241618A","201108241746A","201209301631A","201302091416A","201404112029A","201511260545A"})
//						.map(name -> new GlobalCMTID(name)).collect(Collectors.toSet());
				
//				if (goodIds.contains(id.getGlobalCMTID()))
//					return true;
				return true;
			}
		};
		WeightingType weigthingType = WeightingType.RECIPROCAL;
		boolean atLeastThreeRecordsPerStation = false;
		List<DataSelectionInformation> selectionInfo = null;
		
		List<BasicID> idList = Stream.of(basicIDs).collect(Collectors.toList());
		
		List<GlobalCMTID> events = idList.stream().map(id -> id.getGlobalCMTID())
				.distinct().collect(Collectors.toList());
		
		Files.deleteIfExists(Paths.get("eventVariance.inf"));
		Files.createFile(Paths.get("eventVariance.inf"));
		for (GlobalCMTID event : events) {
			BasicID[] eventIDs = idList.parallelStream().filter(id -> id.getGlobalCMTID().equals(event))
					.collect(Collectors.toList()).toArray(new BasicID[0]);
			Dvector dvector = new Dvector(eventIDs, chooser, weigthingType, atLeastThreeRecordsPerStation, selectionInfo);
			Files.write(Paths.get("eventVariance.inf"), (event + " " + dvector.getVariance() + " " + dvector.getNTimeWindow() + "\n").getBytes(), StandardOpenOption.APPEND);
		}
		
		Dvector dvector = new Dvector(basicIDs, chooser, weigthingType, atLeastThreeRecordsPerStation, selectionInfo);
		
//		Path weightingPath = Paths.get("weighting" + Utilities.getTemporaryString() + ".inf");
//		dvector.outWeighting(Paths.get("."));
//		
		System.out.println("Total variance = " + dvector.getVariance());
	}
	
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
	public static boolean isPair(BasicID id0, BasicID id1) {
		boolean res = false;
		if (id0.getPhases() == null && id1.getPhases() == null) // for compatibility with old format of BasicID
			res = id0.getStation().equals(id1.getStation()) && id0.getGlobalCMTID().equals(id1.getGlobalCMTID())
					&& id0.getSacComponent() == id1.getSacComponent() && id0.getNpts() == id1.getNpts()
					&& id0.getSamplingHz() == id1.getSamplingHz() && Math.abs(id0.getStartTime() - id1.getStartTime()) < 20.
					&& id0.getMaxPeriod() == id1.getMaxPeriod() && id0.getMinPeriod() == id1.getMinPeriod();
		else {
			res = id0.getStation().equals(id1.getStation()) && id0.getGlobalCMTID().equals(id1.getGlobalCMTID())
				&& id0.getSacComponent() == id1.getSacComponent()
				&& id0.getSamplingHz() == id1.getSamplingHz() && new Phases(id0.getPhases()).equals(new Phases(id1.getPhases()))
				&& id0.getMaxPeriod() == id1.getMaxPeriod() && id0.getMinPeriod() == id1.getMinPeriod();
//				&& id0.getNpts() == id1.getNpts();
//			if (res == false && id0.getStation().getPosition()) {
//				String s = id0.getStation().equals(id1.getStation()) + " " + id0.getStation().getPosition() + " " + id1.getStation().getPosition() + " " 
//						+ (id0.getSamplingHz() == id1.getSamplingHz()) + " " + id0.getSamplingHz() + " " + id1.getSamplingHz() + " "
//						+ (id0.getMaxPeriod() == id1.getMaxPeriod()) + " " + id0.getMaxPeriod() + " " + id1.getMaxPeriod() + " "
//						+ (id0.getMinPeriod() == id1.getMinPeriod()) + " " + id0.getMinPeriod() + " " + id1.getMinPeriod() + " "
//						+ new Phases(id0.getPhases()).equals(new Phases(id1.getPhases()));
//				System.out.println(s);
//			}
			
		}
		return res;
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
	
	private boolean atLeastThreeRecordsPerStation;
	
	List<DataSelectionInformation> selectionInfo;

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
	
	private RealVector[] weightingVectors;

	/**
	 * Function for weighting of each timewindow with IDs.
	 */
	private ToDoubleBiFunction<BasicID, BasicID> weightingFunction;
	
	private double[] lowerUpperMantleWeighting;
	
	private WeightingType weightingType;
	
	private double[][] histogramDistance;
	private double[][] histogramAzimuth;

	/**
	 * Use all waveforms in the IDs Weighting factor is reciprocal of maximum
	 * value in each obs time window
	 * 
	 * @param basicIDs
	 *            must contain waveform data
	 */
	public Dvector(BasicID[] basicIDs) {
		this(basicIDs, id -> true, WeightingType.RECIPROCAL);
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
	public Dvector(BasicID[] basicIDs, Predicate<BasicID> chooser, WeightingType weigthingType, boolean atLeastThreeRecordsPerStation, List<DataSelectionInformation> selectionInfo) {
		this.atLeastThreeRecordsPerStation = atLeastThreeRecordsPerStation;
		ids = basicIDs;
		if (!check(ids))
			throw new RuntimeException("Input IDs do not have waveform data.");
		// System.exit(0);
		this.chooser = chooser;
		this.weightingType = weigthingType;
		switch (weigthingType) {
		case RECIPROCAL:
			this.weightingFunction = (obs, syn) -> {
				RealVector obsVec = new ArrayRealVector(obs.getData());
				if (Math.abs(obs.getStartTime() - syn.getStartTime()) >= 15.) {
					System.err.println(obs);
					return 0.;
				}
				if (obsVec.getLInfNorm() == 0 || Double.isNaN(obsVec.getLInfNorm()))
					throw new RuntimeException("Obs is 0 or NaN: " + obs + " " + obsVec.getLInfNorm());
				return 1. / obsVec.getLInfNorm(); 
//						Math.max(Math.abs(obsVec.getMinValue()), Math.abs(obsVec.getMaxValue()));
			};
			break;
		case RECIPROCAL_PcP:
			this.weightingFunction = (obs, syn) -> {
				RealVector obsVec = new ArrayRealVector(obs.getData());
				if (Math.abs(obs.getStartTime() - syn.getStartTime()) >= 15.) {
					System.err.println(obs);
					return 0.;
				}
				if (obsVec.getLInfNorm() == 0 || Double.isNaN(obsVec.getLInfNorm()))
					throw new RuntimeException("Obs is 0 or NaN: " + obs + " " + obsVec.getLInfNorm());
				double distance = Math.toDegrees(obs.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(obs.getStation().getPosition()));
				double a = 3.;
				double w = (a-1) / (91-67) * (91-distance) + 1.;
				return 1. / obsVec.getLInfNorm() * w;
			};
			break;
		case RECIPROCAL_COS:
			this.weightingFunction = (obs, syn) -> {
				RealVector obsVec = new ArrayRealVector(obs.getData());
				if (Math.abs(obs.getStartTime() - syn.getStartTime()) >= 10.) {
					System.err.println(obs);
					return 0.;
				}
				double d = Math.toDegrees(obs.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(obs.getStation().getPosition()));
				double w = 1. * Math.cos((d - 70) / (78 - d) * Math.PI / 2.) + 1.;
				if (d > 78 || d < 70) w = 1.;
				return 1. / obsVec.getLInfNorm() * w; 
			};
			break;
		case RECIPROCAL_AZED:
//			this.histogramDistance = new double[][] { {10, 2.5}, {15, 2.}, {20, 1.}, {25, 0.8}
//				, {30, .8}, {35, .8} };
//			this.histogramAzimuth = new double[][] { {295, 2.5}, {300, 2.5}, {305, 2.5}
//				, {310, 1.000}, {315, 0.8}, {320, 1.05}, {325, 0.8}
//				, {330, 1.}, {335, 1.2}, {340, 1.2}, {345, 0.8}
//				, {350, 0.7}, {355, 0.9}, {0, 1.05}, {5, 1.2}
//				, {10, 1.2}, {15, 1.2}, {20, 1.2}, {25, 1.2}
//				, {30, 1.2}, {35, 1.2} };
			this.histogramDistance = new double[][] { {10.0, 1.417}, {15.0, 1.306}, {20.0, 0.852}
				, {25.0, 0.708}, {30.0, 0.716} };
			this.histogramAzimuth = new double[][] { {0.0, 0.909}, {5.0, 0.938}, {10.0, 1.007}
			, {15.0, 0.912}, {20.0, 0.940}, {25.0, 0.787}, {30.0, 0.998}, {35.0, 1.759}
			, {40.0, 1.929}, {45.0, 0.772}, {50.0, 0.772}, {305.0, 0.772}, {310.0, 0.899}
			, {315.0, 0.772}, {320.0, 1.009}, {325.0, 0.873}, {330.0, 1.025}, {335.0, 1.025}
			, {340.0, 1.152}, {345.0, 1.007}, {350.0, 0.899}, {355.0, 0.845} };
			Set<String> networkSet = Stream.of(new String[] {"CU", "IU", "II"})
					.collect(Collectors.toSet());
			this.weightingFunction = (obs, syn) -> {
				RealVector obsVec = new ArrayRealVector(obs.getData(), false);
				if (Math.abs(obs.getStartTime() - syn.getStartTime()) >= 10.) {
					System.err.println(obs);
					return 0.;
				}
				double weight = 1. / Math.max(Math.abs(obsVec.getMinValue()), Math.abs(obsVec.getMaxValue()));
				if (obs.getStation().getPosition().getLongitude() <= -80)
					weight *= weightingEpicentralDistanceTZ(obs) * weightingAzimuthTZ(obs);
				if (networkSet.contains(obs.getStation().getNetwork()))
					weight *= 2;
				return weight;
			};
			break;
		case RECIPROCAL_AZED_DPP:
//			this.histogramDistance = new double[][] { {70.0, 1.00}, {75.0, 1.03}, {80.0, 1.39}, {85.0, 2.67}, {90.0, 3.}, {95.0, 3.}, {100.0, 1.} };
//			this.histogramAzimuth = new double[][] { {310.0, 1.}, {315.0, 1.605}, {320.0, 1.000}, {325.0, 1.156}
//				, {330.0, 1.640}, {335.0, 2.012}, {340.0, 3.}, {345.0, 3.}, {350.0, 3.}, {355.0, 3.}, {0.0, 1.}, {5.0, 1.} };
//			this.histogramDistance = new double[][] { {70.0, 1.000}, {75.0, 1.016}, {80.0, 1.179}, {85.0, 1.635}
//			, {90.0, 2.501}, {95.0, 4.000}, {100.0, 1.000} };
//			this.histogramAzimuth = new double[][] { {0.0, 1.000}, {5.0, 1.000}, {310.0, 1.000}, {315.0, 1.267}, {320.0, 1.000}
//				, {325.0, 1.075}, {330.0, 1.281}, {335.0, 1.418}, {340.0, 2.294}, {345.0, 3.795}, {350.0, 4.000}, {355.0, 4.000} };
//			this.histogramDistance = new double[][] { {70, 0.741}, {75, 0.777}, {80, 0.938}, {85, 1.187}, {90, 1.200}, {95, 1.157} };
//			this.histogramAzimuth = new double[][] { {310, 1.000}, {315, 0.642}, {320, 0.426}, {325, 0.538}, {330, 0.769}, {335, 0.939}, {340, 1.556}
//			, {345, 1.414}, {350, 1.4}, {355, 1.4}, {0, 1.000}, {5, 1.000} };
//			this.histogramDistance = new double[][] { {70.0, 1.000}, {75.0, 1.016}, {80.0, 1.179}, {85.0, 1.635}
//			, {90.0, 2.0}, {95.0, 2.0}, {100.0, 1.000} };
			this.histogramAzimuth = new double[][] { {0.0, 1.000}, {5.0, 1.000}, {310.0, 1.000}, {315.0, 1.267}, {320.0, 1.000}
				, {325.0, 1.075}, {330.0, 1.281}, {335.0, 1.418}, {340.0, 2.0}, {345.0, 2.0}, {350.0, 2.0}, {355.0, 2.0}, {360.0, 1.0}};
				
			this.histogramDistance = new double[][] { {65.0, 1.000}, {70.0, 1.000}, {75.0, 1.0}, {80.0, 1.}, {85.0, 1.}
				, {90.0, 1.0}, {95.0, 1.0}, {100.0, 1.000} };
				
			this.weightingFunction = (obs, syn) -> {
				RealVector obsVec = new ArrayRealVector(obs.getData(), false);
				if (Math.abs(obs.getStartTime() - syn.getStartTime()) >= 10.) {
					System.err.println(obs);
					return 0.;
				}
				
				double wtmp = 1.;
//				if (obs.getSacComponent().equals(SACComponent.Z))
//					wtmp = 6.;
				
				return 1. / obsVec.getLInfNorm()
						* weightingEpicentralDistanceDpp(obs)
						* weightingAzimuthDpp(obs)
						* wtmp;
			};
			break;
		case RECIPROCAL_AZED_DPP_V2:
//			this.histogramDistance = new double[][] { {70.00, 1.000}, {75.00, 1.045}, {80.00, 1.270}, {85.00, 1.750}
//				, {90.00, 2.000}, {95.00, 2.000}, {100.00, 1.000} };
//			this.histogramAzimuth = new double[][] { {0.00, 1.000}, {5.00, 1.000}, {310.00, 1.000}, {315.00, 1.270}, {320.00, 1.000}
//				, {325.00, 1.078}, {330.00, 1.375}, {335.00, 1.411}, {340.00, 2.000}, {345.00, 2.000}, {350.00, 2.000}, {355.00, 2.000} };
			this.histogramDistance = new double[][] { {70, 0.741}, {75, 0.777}, {80, 0.938}, {85, 1.187}, {90, 1.200}, {95, 1.157}, {100, 1.157} };
			this.histogramAzimuth = new double[][] { {0, 1.000}, {5, 1.000}, {310, 1.000}, {315, 0.642}, {320, 0.426}, {325, 0.538}, {330, 0.769}, {335, 0.939}, {340, 1.556}
			, {345, 1.414}, {350, 1.4}, {355, 1.4},  {360, 1.4} };
			this.weightingFunction = (obs, syn) -> {
				RealVector obsVec = new ArrayRealVector(obs.getData(), false);
				if (Math.abs(obs.getStartTime() - syn.getStartTime()) >= 10.) {
					System.err.println(obs);
					return 0.;
				}
				return 1. / Math.max(Math.abs(obsVec.getMinValue()), Math.abs(obsVec.getMaxValue()))
						* weightingEpicentralDistanceDpp(obs)
						* weightingAzimuthDpp(obs);
			};
			break;
		case IDENTITY:
			this.weightingFunction = (obs, syn) -> 1.;
			break;
		default:
			throw new RuntimeException("Weighting type for this constructor can only be IDENTITY or RECIPROCAL or RECIPROCAL_AZED or RECIPROCAL_AZED_DPP");
		}
		
		sort();
		read();
	}
	
	private double weightingEpicentralDistanceDpp(BasicID obs) {
		double weight = 1.;
		double distance = obs.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(obs.getStation().getPosition()) * 180. / Math.PI;
		
		double maxWeight = 2.;
		
//		double[][] histogram = new double[][] { {70, 1.}, {75, 1.09}, {80, 1.41}, {85, 2.5}, {90, 2.5}, {95, 2.5}, {100, 1.} };
		
//		histogramDistance = new double[][] { {70, 0.741}, {75, 0.777}, {80, 0.938}, {85, 1.187}, {90, 1.200}, {95, 1.157} };
//		histogramDistance = new double[][] { {70.0, 1.00}, {75.0, 1.03}, {80.0, 1.39}, {85.0, 2.67}, {90.0, 3.}, {95.0, 3.}, {100.0, 1.} };
		
		//D" CA joint P-S
//		if (obs.getSacComponent().equals(SACComponent.T))
//			histogramDistance = new double[][] { {70.0,1.01},{72.0,1.00},{74.0,1.02},{76.0,1.03},{78.0,1.08},{80.0,1.15},{82.0,1.19}
//				,{84.0,1.29},{86.0,1.53},{88.0,1.95},{90.0,1.91},{92.0,3.09},{94.0,3.04},{96.0,3.52} };
//		else if (obs.getSacComponent().equals(SACComponent.Z))
//			histogramDistance = new double[][] { {64.0,1.59},{66.0,1.06},{68.0,1.00},{70.0,1.05},{72.0,1.07},{74.0,1.09},{76.0,1.12},{78.0,1.14},{80.0,1.24},{82.0,1.31}
//				,{84.0,1.58},{86.0,1.81},{88.0,2.49},{90.0,2.25},{92.0,3.57},{94.0,3.72},{96.0,7.48} };
		
		this.histogramDistance = new double[][] { {60.0, 1.000}, {100.0, 1.000} };
		
		for (int i = 0; i < histogramDistance.length-1; i++)
			if (distance >= histogramDistance[i][0] && distance < histogramDistance[i+1][0])
				weight = histogramDistance[i][1];
		
		if (weight > maxWeight)
			weight = maxWeight;
		
		return weight;
	}
	
	public double weightingAzimuthDpp(BasicID obs) {
		double weight = 1.;
		double azimuth = obs.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(obs.getStation().getPosition()) * 180. / Math.PI;
		
		double maxWeight = 2.;
		
//		histogramAzimuth = new double[][] { {310, 1.000}, {315, 0.642}, {320, 0.426}, {325, 0.538}, {330, 0.769}, {335, 0.939}, {340, 1.556}
//			, {345, 1.414}, {350, 1.4}, {355, 1.4}, {0, 1.000}, {5, 1.000} };
//		histogramAzimuth = new double[][] { {310.0, 1.}, {315.0, 1.605}, {320.0, 1.000}, {325.0, 1.156}
//			, {330.0, 1.640}, {335.0, 2.012}, {340.0, 3.}, {345.0, 3.}, {350.0, 3.}, {355.0, 3.}, {0.0, 1.}, {5.0, 1.} };
		
//		D" CA joint P-S
		if (obs.getSacComponent().equals(SACComponent.T))
			histogramAzimuth = new double[][] { {0.0,7.89},{2.0,10.59},{4.0,10.59},{6.0,10.59},{314.0,3.35},{316.0,1.41},{318.0,1.23}
				,{320.0,1.25},{322.0,1.06},{324.0,1.00},{326.0,1.13},{328.0,1.36},{330.0,1.41},{332.0,1.34},{334.0,1.40},{336.0,1.59},{338.0,1.75}
				,{340.0,2.10},{342.0,3.15},{344.0,3.53},{346.0,4.06},{348.0,3.70},{350.0,4.51},{352.0,4.56},{354.0,3.49},{356.0,4.00},{358.0,4.29},{360.0,7.89} };
		else if (obs.getSacComponent().equals(SACComponent.Z))
			histogramAzimuth = new double[][] { {0.0,5.73},{2.0,9.07},{4.0,10.73},{6.0,15.17},{8.0,19.59},{312.0,33.93},{314.0,4.90}
				,{316.0,1.62},{318.0,1.19},{320.0,1.23},{322.0,1.09},{324.0,1.00},{326.0,1.00},{328.0,1.26},{330.0,1.34},{332.0,1.30}
				,{334.0,1.37},{336.0,1.39},{338.0,1.52},{340.0,1.88},{342.0,2.76},{344.0,3.48},{346.0,3.03},{348.0,3.10},{350.0,3.43}
				,{352.0,3.50},{354.0,2.79},{356.0,3.28},{358.0,3.79},{360.0,5.73} };
		
		for (int i = 0; i < histogramAzimuth.length-1; i++) {
			if (azimuth >= histogramAzimuth[i][0] && azimuth < histogramAzimuth[i+1][0])
				weight = histogramAzimuth[i][1];
		}
		
		if (weight > maxWeight)
			weight = maxWeight;
		
		return weight;
	}
	
	private double weightingEpicentralDistanceTZ(BasicID obs) {
		double weight = 1.;
		double distance = obs.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(obs.getStation().getPosition()) * 180. / Math.PI;
		
//		double[][] histogram = new double[][] { {70, 1.}, {75, 1.09}, {80, 1.41}, {85, 2.5}, {90, 2.5}, {95, 2.5}, {100, 1.} };
		
//		histogramDistance = new double[][] { {10, 2.5}, {15, 2.}, {20, 1.}, {25, 0.8}
//			, {30, .8}, {35, .8} };
		
		for (int i = 0; i < histogramDistance.length; i++)
			if (distance >= histogramDistance[i][0] && distance < histogramDistance[i][0] + 5.)
				weight = histogramDistance[i][1];
		
		return weight;
	}
	
	public double weightingAzimuthTZ(BasicID obs) {
		double weight = 1.;
		double azimuth = obs.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(obs.getStation().getPosition()) * 180. / Math.PI;
		
//		histogramAzimuth = new double[][] { {295, 2.5}, {300, 2.5}, {305, 2.5}
//			, {310, 1.000}, {315, 0.8}, {320, 1.05}, {325, 0.8}
//			, {330, 1.}, {335, 1.2}, {340, 1.2}, {345, 0.8}
//			, {350, 0.7}, {355, 0.9}, {0, 1.05}, {5, 1.2}
//			, {10, 1.2}, {15, 1.2}, {20, 1.2}, {25, 1.2}
//			, {30, 1.2}, {35, 1.2} };
		
		for (double[] p : histogramAzimuth) {
			if (azimuth >= p[0] && azimuth < p[0] + 5.)
				weight = p[1];
		}
		
		return weight;
	}
	
	private static double weightingEpicentralDistance(BasicID obs) {
		double weight = 1.;
		double distance = obs.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(obs.getStation().getPosition()) * 180. / Math.PI;
		Phases phases = new Phases(obs.getPhases());
		
//		double[][] histogram = new double[][] { {45, 0.741}, {70, 0.741}, {75, 0.777}, {80, 0.938}, {85, 1.187}, {90, 1.200}, {95, 1.157} };
//		double[][] histogram = new double[][] { {55, 2.07}, {65, 1.13}, {70, 1.03}, {75, 1.}, {80, 1.09}, {85, 1.59}, {90, 2.}, {95, 2.} };
		double[][] histogram = new double[][] { {55, 2.07}, {65, 1.2}, {70, 1.03}, {75, 1.}, {80, 1.19}, {85, 2.5}, {90, 2.5}, {95, 2.5} };
		
		double meanAmpli = 0;
		for (double[] bin : histogram)
			meanAmpli += bin[1];
		meanAmpli /= histogram.length;
		
		double tmpamp = 1./1.5;
		
		if (phases.equals(new Phases("S,ScS")) || phases.equals(new Phases("sS,sScS")) || phases.equals(new Phases("Sdiff")) || phases.equals(new Phases("sSdiff"))) {
			for (int i = 0; i < histogram.length - 1; i++) {
				if (distance >= histogram[i][0] && distance < histogram[i+1][0])
					weight = histogram[i][1];// * 2.;
			}
			if (distance >= histogram[histogram.length - 1][0])
				weight = histogram[histogram.length - 1][1];// * 2.;
		}
		
		double[][] histogram2 = new double[][] { {25., 1.}, {30., 1.}, {35., 3.}, {40., 3.}, {45., 3.}, {50., 2.}, {55., 1.4} };
		
		if (phases.equals(new Phases("S")))
			for (double[] bin : histogram2) {
				if (distance >= bin[0] && distance < bin[0] + 5.)
					weight *= 1.5 * tmpamp * bin[1];
			}
		else if (phases.equals(new Phases("SS")))
			for (double[] bin : histogram2) {
				if (distance >= bin[0] && distance < bin[0] + 5.)
					weight *= 1.5 * tmpamp * bin[1];
			}
		else if (phases.equals(new Phases("SSS,sSS")))
			for (double[] bin : histogram2) {
				if (distance >= bin[0] && distance < bin[0] + 5.)
					weight *= 1.5 * tmpamp * bin[1];
			}
		else if (phases.equals(new Phases("sSSS,SSS")))
			for (double[] bin : histogram2) {
				if (distance >= bin[0] && distance < bin[0] + 5.)
					weight *= 1.5 * tmpamp * bin[1];
			}
		
		
//		else if (phases.equals(new Phases("S"))) {
//			for (int i = 0; i < histogram.length - 1; i++) {
//				if (distance >= histogram[i][0] && distance < histogram[i+1][0])
//					weight = histogram[i][1] / meanAmpli * 3.;
//			}
//			if (distance >= histogram[histogram.length - 1][0])
//				weight = histogram[histogram.length - 1][1] / meanAmpli * 3.;
//		}
		
		return weight;
	}
	
	public Dvector(BasicID[] basicIDs, Predicate<BasicID> chooser, WeightingType weigthingType) {
		this(basicIDs, chooser, weigthingType, false, null);
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
			ToDoubleBiFunction<BasicID, BasicID> weightingFunction, boolean atLeastThreeRecordsPerStation) {
		this.atLeastThreeRecordsPerStation = atLeastThreeRecordsPerStation;
		ids = basicIDs;
		if (!check(ids))
			throw new RuntimeException("Input IDs do not have waveform data.");
		// System.exit(0);
		this.chooser = chooser;
		this.weightingFunction = weightingFunction;
		this.weightingType = weightingType.USERFUNCTION;
		
		sort();
		read();
	}
	
	public Dvector(BasicID[] basicIDs, Predicate<BasicID> chooser,
			ToDoubleBiFunction<BasicID, BasicID> weightingFunction) {
		this(basicIDs, chooser, weightingFunction, false);
	}
	
	public Dvector(BasicID[] basicIDs, Predicate<BasicID> chooser,
			WeightingType weightingType, double[] weighting, boolean atLeastThreeRecordsPerStation) {
		this.atLeastThreeRecordsPerStation = atLeastThreeRecordsPerStation;
		double minW = Double.MAX_VALUE;
		double maxW = Double.MIN_VALUE;
		for (double w : weighting) {
			if (w > maxW)
				maxW = w;
			if (w < minW)
				minW = w;
		}
		if (minW < 0)
			throw new RuntimeException("Weighting factors must be positive or null");
		if (minW == maxW && minW == 0)
			throw new RuntimeException("All weighting factors are null");
		ids = basicIDs;
		if (!check(ids))
			throw new RuntimeException("Input IDs do not have waveform data.");
		// System.exit(0);
		this.chooser = chooser;
		this.weightingType = weightingType;
		switch (weightingType) {
		case LOWERUPPERMANTLE:
			this.lowerUpperMantleWeighting = weighting.clone();
			break;
		case TAKEUCHIKOBAYASHI:
			this.weighting = weighting.clone();
			break;
		case FINAL:
			this.weighting = weighting.clone();
			break;
		default:
			throw new RuntimeException("Weighting type for this constructor can be only LOWERUPPERMANTLE or TAKEUCHIKOBAYASHI or FINAL");
		}
		this.weightingFunction = (obs, syn) -> {
			return 1.;
		};
		sort();
		read();
	}
	
	public Dvector(BasicID[] basicIDs, Predicate<BasicID> chooser,
			WeightingType weightingType, double[] weighting) {
		this(basicIDs, chooser, weightingType, weighting, false);
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
	
	public int getWindowNPTS(int i) {
		return obsIDs[i].getNpts();
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
	
	public RealVector getWeightingVector(int i) {
		return weightingVectors[i];
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
	
	public void outPhases(Path outPath) throws IOException {
		Path phases = outPath.resolve("phases.inf");
		Map<Phases, Integer> nums = new HashMap<>();
		for (BasicID id : obsIDs) {
			Phases p = new Phases(id.getPhases());
			Integer n = nums.get(p);
			if (n == null)
				nums.put(p, 1);
			else
				nums.put(p, n + 1);
		}
		
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(phases))) {
			pw.println("#Phase #timewindows");
			nums.entrySet().stream().sorted(Map.Entry.<Phases, Integer>comparingByValue().reversed())
				.forEach(e -> pw.println(e.getKey() + " " + e.getValue()));
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
	
	private double[] weightPcP(int iwin) {
		int n = obsIDs[iwin].getNpts();
		double[] weight = new double[n];
		
		double t0 = obsIDs[iwin].getStartTime();
		double dt = 1. / obsIDs[iwin].getSamplingHz();
		
		double tau1 = 3.;
		double tau2 = 9.;
		double delta = 2.;
		double a = 1.5;
				
		double depth = 6371. - obsIDs[iwin].getGlobalCMTID().getEvent().getCmtLocation().getR();
		double distance = Math.toDegrees(obsIDs[iwin].getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(obsIDs[iwin].getStation().getPosition()));
		try {
			TauP_Time timeTool = new TauP_Time("prem");
			timeTool.parsePhaseList("PcP");
			timeTool.setSourceDepth(depth);
			timeTool.calculate(distance);
			double tPcP = timeTool.getArrival(0).getTime();
			
			double t1 = tPcP - tau1;
			double t2 = tPcP + tau2;
			
			for (int i = 0; i < n; i++) {
				double t = t0 + i * dt;
				if (t <= t1 - delta || t > t2 + delta)
					weight[i] = 1.;
				else if (t1 - delta < t && t <= t1)
					weight[i] = 1 + a / delta * (t - t1 + delta);
				else if (t1 < t && t <= t2)
					weight[i] = 1 + a;
				else if (t2 < t && t <= t2 + delta)
					weight[i] = 1 + a / delta * (t2 - t + delta);
			}
		} catch (TauModelException e) {
			e.printStackTrace();
		}
		
		return weight;
	}
	
//	private double[] weightOnset(int iwin, double rearShift) {
//		int n = obsIDs[iwin].getNpts();
//		double[] weight = new double[n];
//		
//		double t0 = obsIDs[iwin].getStartTime();
//		double dt = 1. / obsIDs[iwin].getSamplingHz();
//		
//		double tau1 = 3.;
//		double tau2 = 9.;
//		double delta = 2.;
//		double a = 1.5;
//				
////		double depth = 6371. - obsIDs[iwin].getGlobalCMTID().getEvent().getCmtLocation().getR();
////		double distance = Math.toDegrees(obsIDs[iwin].getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(obsIDs[iwin].getStation().getPosition()));
//		try {
////			TauP_Time timeTool = new TauP_Time("prem");
////			timeTool.parsePhaseList("ScS");
////			timeTool.setSourceDepth(depth);
////			timeTool.calculate(distance);
////			double tPcP = timeTool.getArrival(0).getTime();
//			double tOnset = rearShift;
//			
//			double t1 = tOnset - tau1;
//			double t2 = tOnset + tau2;
//			
//			for (int i = 0; i < n; i++) {
//				double t = t0 + i * dt;
//				if (t <= t1 - delta || t > t2 + delta)
//					weight[i] = 1.;
//				else if (t1 - delta < t && t <= t1)
//					weight[i] = 1 + a / delta * (t - t1 + delta);
//				else if (t1 < t && t <= t2)
//					weight[i] = 1 + a;
//				else if (t2 < t && t <= t2 + delta)
//					weight[i] = 1 + a / delta * (t2 - t + delta);
//			}
//		} catch (TauModelException e) {
//			e.printStackTrace();
//		}
//		
//		return weight;
//	}
	
	private void read() {
		// double t = System.nanoTime();
		this.npts = 0;
		int start = 0;
		Map<Station, Double> stationDenominator = usedStationSet.stream().collect(Collectors.toMap(s -> s, s -> 0.0));
		Map<Station, Double> stationNumerator = usedStationSet.stream().collect(Collectors.toMap(s -> s, s -> 0.0));
		Map<GlobalCMTID, Double> eventDenominator = usedGlobalCMTIDset.stream()
				.collect(Collectors.toMap(id -> id, id -> 0d));
		Map<GlobalCMTID, Double> eventNumerator = usedGlobalCMTIDset.stream()
				.collect(Collectors.toMap(id -> id, id -> 0d));
		double obs2 = 0;
		
//		Map<Double, Double> periodLMW = new HashMap<>();
//		Map<Double, Double> periodUMW = new HashMap<>();
//		Map<Double, Double> periodTotalW = new HashMap<>();
//		double minTmin = Double.MAX_VALUE;
//		//same weight for different frequency bands
//		for (int i = 0; i < nTimeWindow; i++) {
//			Double T = obsIDs[i].getMinPeriod();
//			if (T < minTmin)
//				minTmin = T;
//			double w = new ArrayRealVector(obsIDs[i].getData()).getNorm();
//			double totalW = 0;
//			if (periodTotalW.containsKey(T))
//				totalW = periodTotalW.get(T) + w;
//			else
//				totalW = w;
//			periodTotalW.put(T, totalW);
//			
//			Phases phases = new Phases(obsIDs[i].getPhases());
//			// upper mantle
//			if (phases.isUpperMantle()) {
//				double tmpw = 0;
//				if (periodUMW.containsKey(T))
//					tmpw = periodUMW.get(T) + w;
//				else
//					tmpw = w;
//				periodUMW.put(T, tmpw);
//			}
//			else if (phases.isLowerMantle()) {
//				double tmpw = 0;
//				if (periodLMW.containsKey(T))
//					tmpw = periodLMW.get(T) + w;
//				else
//					tmpw = w;
//				periodLMW.put(T, tmpw);
//			}
//		}
//		//periodTotalW.forEach((T, w) -> System.out.println(T + "s " + w));
//		double[] maxW = new double[] {Double.MIN_VALUE};
//		periodTotalW.forEach((T, w) -> {
//			if (w > maxW[0])
//				maxW[0] = w;
//		});
//		for (Double T : periodTotalW.keySet())
//			periodTotalW.replace(T, periodTotalW.get(T) / maxW[0]);
//		Map<Double, Double> periodWeight = new HashMap<>();
//		final double minTmin_ = minTmin;
//		periodTotalW.forEach((T, w) -> periodWeight.put(T, 1. / w));
		
//		Map<Double, Double> periodUMWeight = new HashMap<>();
//		Map<Double, Double> periodLMWeight = new HashMap<>();
//		double T0LMUMAverage = 0;
//		int count = 0;
//		for (double T : periodUMW.keySet()) {
//			double max = periodUMW.get(T) > periodLMW.get(T) * 4. ? periodUMW.get(T) : periodLMW.get(T) * 4.;
//			if (count == 0) {
//				T0LMUMAverage = .5 * max * (1. / periodUMW.get(T) + 1. / periodLMW.get(T) * 4.);
//			}
//			double factor = T0LMUMAverage / ( .5 * max * (1. / periodUMW.get(T) + 1. / periodLMW.get(T) * 4.) );
//			periodUMWeight.put(T, max / periodUMW.get(T) * factor);
//			periodLMWeight.put(T, max / periodLMW.get(T) * factor * 4.);
//			System.out.println(T + "s um: " + max / periodUMW.get(T) * factor + " lm: " + max / periodLMW.get(T) * factor * 4.);
//			count++;
//		}
		
		for (int i = 0; i < nTimeWindow; i++) {
			startPoints[i] = start;
			int npts = obsIDs[i].getNpts();
			this.npts += npts;
			start += npts;
			
			DataSelectionInformation info = null;
			if (selectionInfo != null) {
				GlobalCMTID id = obsIDs[i].getGlobalCMTID();
				Station station = obsIDs[i].getStation();
				double startTime = obsIDs[i].getStartTime();
				SACComponent component = obsIDs[i].getSacComponent();
				System.out.println(obsIDs[i]);
				info = selectionInfo.stream().filter(selec -> {
					TimewindowInformation tw = selec.getTimewindow();
					if (tw.getStation().getStationName().equals("SAO")) {
						System.out.println(tw);
						System.out.println(tw.getStation().equals(station) + " " + tw.getGlobalCMTID().equals(id) + " " + tw.getComponent().equals(component) + " " + Math.abs(tw.getStartTime() - startTime));
					}
					return tw.getStation().equals(station) 
							&& tw.getGlobalCMTID().equals(id)
							&& tw.getComponent().equals(component)
							&& Math.abs(tw.getStartTime() - startTime) < 0.1;
				}).findFirst().get();
			}

			// 観測波形の読み込み
			obsVec[i] = new ArrayRealVector(obsIDs[i].getData(), false);
			
			// 観測波形の最大値の逆数で重み付け TODO 重み付けの方法を決める
			switch (weightingType) {
			case LOWERUPPERMANTLE:		
				weighting[i] = new Phases(obsIDs[i].getPhases()).isLowerMantle() ? 
						lowerUpperMantleWeighting[0] : lowerUpperMantleWeighting[1];
				break;
			case RECIPROCAL_PcP:
			case RECIPROCAL:
			case RECIPROCAL_COS:
				if (info != null) {
					System.err.println("Using Signal-to-Noise ratio from the data selection information file");
					weighting[i] = weightingFunction.applyAsDouble(obsIDs[i], synIDs[i]) * info.getSNratio(); //* periodTotalW.get(obsIDs[i].getMinPeriod());
				}
				else {
					weighting[i] = weightingFunction.applyAsDouble(obsIDs[i], synIDs[i]);// * periodWeight.get(obsIDs[i].getMinPeriod());
//					if (new Phases(obsIDs[i].getPhases()).isLowerMantle())
//						weighting[i] *= periodLMWeight.get(obsIDs[i].getMinPeriod());
//					else if (new Phases(obsIDs[i].getPhases()).isUpperMantle())
//						weighting[i] *= periodUMWeight.get(obsIDs[i].getMinPeriod());
				}
				break;
			case RECIPROCAL_AZED:
				weighting[i] = weightingFunction.applyAsDouble(obsIDs[i], synIDs[i]);
				break;
			case RECIPROCAL_AZED_DPP:
				weighting[i] = weightingFunction.applyAsDouble(obsIDs[i], synIDs[i]);
				break;
			case RECIPROCAL_AZED_DPP_V2:
				weighting[i] = weightingFunction.applyAsDouble(obsIDs[i], synIDs[i]);
				break;
			case IDENTITY:
				weighting[i] = weightingFunction.applyAsDouble(obsIDs[i], synIDs[i]);
				break;
			case USERFUNCTION:
				weighting[i] = weightingFunction.applyAsDouble(obsIDs[i], synIDs[i]);
				break;
			case TAKEUCHIKOBAYASHI: // double[] weighting already set in sort()
				break;
			case FINAL:
				Trace obstrace = obsIDs[i].getTrace();
				weighting[i] *= obstrace.getMaxValue() > -obstrace.getMinValue() ? 1. / obstrace.getMaxValue() : -1. / obstrace.getMinValue();
				break;
			default:
				break;
			}
			
			double[] ws = new double[obsVec[i].getDimension()];
//			if (weightingType.equals(WeightingType.RECIPROCAL_PcP)) {
//				ws = weightPcP(i);
//				for (int j = 0; j < ws.length; j++)
//					ws[j] *= weighting[i];
//			}
//			else {
				for (int j = 0; j < ws.length; j++)
					ws[j] = weighting[i];
//			}
			weightingVectors[i] = new ArrayRealVector(ws);

//			obsVec[i] = obsVec[i].mapMultiply(weighting[i]);

			// 理論波形の読み込み
			synVec[i] = new ArrayRealVector(synIDs[i].getData(), false);
//			synVec[i] = synVec[i].mapMultiply(weighting[i]);
			
			for (int j = 0; j < ws.length; j++) {
				obsVec[i].setEntry(j, obsVec[i].getEntry(j) * ws[j]);
				synVec[i].setEntry(j, synVec[i].getEntry(j) * ws[j]);
			}
			
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
	
	public RealVector getMask(GlobalCMTID id) {
		RealVector mask = new ArrayRealVector(npts);
		for (int i = 0; i < nTimeWindow-1; i++) {
			if (obsIDs[i].getGlobalCMTID().equals(id)) {
				for (int j = startPoints[i]; j < startPoints[i+1]; j++)
					mask.setEntry(j, 1.);
			}
		}
		return mask;
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
	
	private List<BasicID> moreThanThreeEventsPerStation(List<BasicID> ids) {
		List<BasicID> filteredIds = new ArrayList<>();
		Set<Station> stations = ids.stream().map(id -> id.getStation()).collect(Collectors.toSet());
		for (Station station : stations) {
			List<BasicID> tmps = ids.stream().filter(id -> id.getStation().equals(station)).collect(Collectors.toList());
			int numberOfGCMTId = (int) tmps.stream().map(id -> id.getGlobalCMTID()).distinct().count();
			if (numberOfGCMTId >= 2)
				tmps.forEach(tmp -> filteredIds.add(tmp));
		}
		return filteredIds;
	}
	
//	private boolean trimWindow;
//	private double trimmedLength;
	
	public void trimWindow(double trimPoint, boolean keepBefore) {
		System.out.println("Trim windows " + trimPoint + " " + keepBefore);
		for (int i = 0; i < obsIDs.length; i++) {
			BasicID id = obsIDs[i];
			BasicID synID = synIDs[i];
			int nStart = 0;
			int nEnd = 0;
			if (keepBefore) {
				nStart = 0;
				nEnd = (int) (trimPoint / id.getSamplingHz()) + 1;
				nEnd = nEnd > id.getNpts() ? id.getNpts() : nEnd;
			}
			else {
				nStart = (int) (trimPoint / id.getSamplingHz());
				nEnd = id.getNpts();
			}
			int n = nEnd - nStart;
			if (nStart > nEnd) {
				throw new RuntimeException("Too much trimming");
			}
			double[] data = Arrays.copyOfRange(id.getData(), nStart, nEnd);
			double[] synData = Arrays.copyOfRange(synID.getData(), nStart, nEnd);
			obsIDs[i] = new BasicID(id.getWaveformType(), id.getSamplingHz(), id.getStartTime(), n, id.getStation()
				, id.getGlobalCMTID(), id.getSacComponent(), id.getMinPeriod(), id.getMaxPeriod(), id.getPhases(), id.getStartByte(), id.isConvolute(), data);
			synIDs[i] = new BasicID(synID.getWaveformType(), synID.getSamplingHz(), synID.getStartTime(), n, synID.getStation()
					, synID.getGlobalCMTID(), synID.getSacComponent(), synID.getMinPeriod(), synID.getMaxPeriod(), synID.getPhases(), synID.getStartByte(), synID.isConvolute(), synData);
		}
		read();
	}
	
	/**
	 * データを選り分ける 観測波形 理論波形両方ともにあるものだけを採用する 重複があったときには終了
	 */
	private void sort() {
		// //////
		
		// 観測波形の抽出 list observed IDs
//		System.out.println("DEBUG1: " + ids.length);
		List<BasicID> obsList = Arrays.stream(ids).filter(id -> id.getWaveformType() == WaveformType.OBS)
				.filter(chooser::test).collect(Collectors.toList());
//		System.out.println("DEBUG2: " + obsList.size());

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
		System.out.println("Number of obs IDs before pairing with syn IDs = " + obsList.size());
		if (obsList.size() != synList.size())
			System.out.println("The numbers of observed IDs " + obsList.size() + " and " + " synthetic IDs "
					+ synList.size() + " are different ");
		int size = obsList.size() < synList.size() ? synList.size() : obsList.size();

		List<BasicID> useObsList = new ArrayList<>(size);
		List<BasicID> useSynList = new ArrayList<>(size);

		for (int i = 0; i < synList.size(); i++) {
			boolean foundPair = false;
			for (int j = 0; j < obsList.size(); j++) {
				if (isPair(synList.get(i), obsList.get(j))) {
					useObsList.add(obsList.get(j));
					useSynList.add(synList.get(i));
					foundPair = true;
					break;
				}
			}
			if (!foundPair)
				System.out.println("Didn't find OBS for " + synList.get(i));
		}

		if (useObsList.size() != useSynList.size())
			throw new RuntimeException("unanticipated");
//		 System.out.println(useObsList.size() + " observed and synthetic pairs are used.");
		
		// filter so that there is at least three records per stations (for time partials stability)
//		if (atLeastThreeRecordsPerStation) {
//			useObsList = moreThanThreeEventsPerStation(useObsList);
//			useSynList = moreThanThreeEventsPerStation(useSynList);
//		}

		nTimeWindow = useSynList.size();
		obsIDs = useObsList.toArray(new BasicID[0]);
		synIDs = useSynList.toArray(new BasicID[0]);
		
		weightingVectors = new ArrayRealVector[nTimeWindow];
		
		if (!(weightingType.equals(WeightingType.TAKEUCHIKOBAYASHI) || weightingType.equals(WeightingType.FINAL)))
			weighting = new double[nTimeWindow];
		else {
			if (weighting.length != nTimeWindow)
				throw new RuntimeException("Number of selected time windows and weighting factors differ " + weighting.length + " " + nTimeWindow);
		}
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
		
		switch (weightingType) {
		case LOWERUPPERMANTLE:
			System.out.println("Using weighting for lower mantle (" + lowerUpperMantleWeighting[0] 
					+ ") and upper mantle (" + lowerUpperMantleWeighting[1] +")");
			break;
		case TAKEUCHIKOBAYASHI:
			System.out.println("Using Takeuchi-Kobayashi weighting scheme");
			break;
		case RECIPROCAL:
			System.out.println("Using observed reciprocal amplitude as weighting");
			break;
		case RECIPROCAL_AZED:
			System.out.println("Using observed reciprocal amplitude as weighting and applying azimuthal and epicentral distance weighting");
			break;
		case RECIPROCAL_AZED_DPP:
			System.out.println("Using observed reciprocal amplitude as weighting and applying azimuthal and epicentral distance weighting for Dpp");
			break;
		case RECIPROCAL_AZED_DPP_V2:
			System.out.println("Using observed reciprocal amplitude as weighting and applying azimuthal and epicentral distance weighting for Dpp (v2)");
			break;
		case IDENTITY:
			System.out.println("Using identity weighting");
			break;
		case USERFUNCTION:
			System.out.println("Using user specified weighting function");
		case FINAL:
			System.out.println("Using Takeuchi-Kobayashi weighting * reciprocal weighting (final)");
		case RECIPROCAL_PcP:
			System.out.println("Using observed reciprocal amplitude as weighting and weighting around PcP");
		default:
			break;
		}
	}
	
	protected static final FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
	
//	private Complex[] equalizer(RealVector obsVec) {
//		Complex[] obsSpc = fft.transform(obsVec.toArray(), TransformType.FORWARD);
//		//smooth obsSpc doing a running average
//		
//	}
	
	public void outWeighting(Path root) {
		try {
			Path outpath = root.resolve("weighting.inf");
			PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			for (double wi : weighting) {
				pw.println(wi);
			}
			
			if (histogramAzimuth != null) {
				outpath = root.resolve("histogramAzimuth.txt");
				PrintWriter pwaz = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
				for (double[] p : histogramAzimuth)
					pwaz.println(p[0] + " " + p[1]);
				pwaz.close();
			}
			if (histogramDistance != null) {
				outpath = root.resolve("histogramEpicentralDistance.txt");
				PrintWriter pwed = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
				for (double[] p : histogramDistance)
					pwed.println(p[0] + " " + p[1]);
				pwed.close();
			}
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
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
