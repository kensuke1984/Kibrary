package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.MaxAmplitudeGet;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.ode.Parameterizable;
import org.apache.commons.math3.util.Precision;


public class Sensitivity1D {
	private Map<PerturbationR_distance, Double> sensitivityMap;
	private PartialID[] ids;
	Set<Phases> includePhases;
	private double max = 0;
	private double maxRef = 0;
	private PartialID[] idref;
	private Map<PerturbationR_distance, Double> sensitivityMapRef;
	private double maxSynthetic = 0;
	private BasicID[] basicIDs;
	private Set<TimewindowInformation> timewindowInformationSet;
	private Map<PerturbationDistance, Double> maxAmplitudeMap;
	
	public Sensitivity1D(PartialID[] ids, Set<Phases> includedPhases) {
		this.ids = ids;
		this.sensitivityMap = new HashMap<>();
		this.includePhases = includedPhases;
		this.compute();
	}
	
	public Sensitivity1D(PartialID[] ids, Set<Phases> includedPhases, BasicID[] basicIDs) {
		this.ids = ids;
		this.sensitivityMap = new HashMap<>();
		this.includePhases = includedPhases;
		this.basicIDs = basicIDs;
		this.maxAmplitudeMap = new HashMap<>();
		this.maxAmplitudeMapMaker();
		this.computeSyntheticNormalized();
	}
	
	public Sensitivity1D(PartialID[] ids, PartialID[] idref, Set<Phases> includedPhases) {
		this.ids = ids;
		this.idref = idref;
		this.sensitivityMap = new HashMap<>();
		this.sensitivityMapRef = new HashMap<>();
		this.includePhases = includedPhases;
		this.compute();
		this.compute4Ref();
	}
	
	public Sensitivity1D(PartialID[] ids, Set<Phases> includedPhases,
				Set<TimewindowInformation> timewindowInformationSet, Map<PerturbationDistance, Double> maxAmplitudeMap){
		this.ids = ids;
		this.sensitivityMap = new HashMap<>();
		this.includePhases = includedPhases;
		this.timewindowInformationSet = timewindowInformationSet;
		this.maxAmplitudeMap = maxAmplitudeMap;
		this.compute();
	}
	
	public Sensitivity1D(PartialID[] ids) {
		this(ids, null);
	}
	
	public Sensitivity1D(Sensitivity1D s) {
		this.ids = s.ids;
		this.sensitivityMap = s.sensitivityMap;
		this.includePhases = s.includePhases;
	}
	
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2)
			System.err.println("usage: path for partial.dat and partialID.dat are need.");
		Path partialPath = Paths.get(args[0]);
		Path partialIDPath = Paths.get(args[1]);
		Path waveformIDPath = Paths.get(args[2]);
		Path waveformDataPath = Paths.get(args[3]);
		
		PartialID[] ids = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialPath);
		BasicID[] basicIDs = BasicIDFile.readBasicIDandDataFile(waveformIDPath, waveformDataPath);
//		PartialID[] idref = PartialIDFile.readPartialIDandDataFile(partialIDRef, partialRef);

		List<Phase> phaseList = Arrays.asList(Phase.S);
		
		for (Phase phase : phaseList) {
			Set<Phases> phaseSet = new HashSet<>();
			phaseSet.add(new Phases(new Phase[] {phase}));
//			Path outPath_src = Paths.get("sensitivity-"+ phase.toString() +"-"+args[0]+ ".inf");
//			Path outPath = Paths.get("sensitivity1D-" + phase.toString() +"-"+args[0]+ ".inf");
			Path outPath1 = Paths.get("sensitivityMap-" + phase.toString() +"-"+args[0]+ ".inf");
//			Path outPath2 = Paths.get("sensitivityMapDistanceNormalized-" + phase.toString()+"-"+args[0] + ".inf");
//			Path outPath3 = Paths.get("sensitivityDistanceNormalizedRef-" + phase.toString()+"-"+args[0] + ".inf");
			
			Sensitivity1D s1D = new Sensitivity1D(ids, phaseSet, basicIDs);
//			Sensitivity1D s1D = new Sensitivity1D(ids, phaseSet);
//			Sensitivity1D copy = new Sensitivity1D(s1D);
//			Sensitivity1D copy2 = new Sensitivity1D(s1D);
			s1D.write(outPath1);
//			s1D.writeComplemented(outPath_src, 10, 1., 4500.);
//			s1D.write1D(outPath);
//			s1D.normalize();
//			s1D.writeComplemented(outPath1, 10, 1., 4500.);		// writeComplemented(outPath, dR, dD, maxR)
//			copy.normalizePerDistance();
//			copy.writeComplemented(outPath2, 10, 1., 4500.);	// writeComplemented(outPath, dR, dD, maxR)
//			copy2.normalizePerDistancePerRef();
//			copy2.writeComplemented(outPath3, 10, 1., 4500.);
		}
	}
	
	public void write(Path outpath) throws IOException{
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))) {
			pw.println("#epicentralDistanceBin, perturbationR, NormalizedSensitivity");
			pw.println("#max " + getMax() + " (" + getMaxSensitivity() + " )");
			sensitivityMap.forEach((rDistance, sensitivity) -> {
				pw.println(rDistance.distance + " " + rDistance.r + " " + sensitivity);
			});
		}
	}
	
	public void writeComplemented(Path outpath, double deltaR, double deltaD, double maxR) throws IOException {
		double[][] complemented = complement(deltaR, deltaD, maxR);
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))) {
			pw.println("#epicentralDistanceBin, perturbationR, NormalizedSensitivity");
			pw.println("#Maxq" + getMax() + " (" + getMaxSensitivity() + " )");
			for (double[] c : complemented)
				pw.println(c[0] + " " + c[1] + " " + c[2]);
		}
	}
	
	public double[][] complement(double deltaR, double deltaD, double maxR) {
		List<Double> distanceList = sensitivityMap.keySet().stream().map(key -> key.distance).collect(Collectors.toList());
		Collections.sort(distanceList);
//		System.out.println(sensitivityMap.size());
		double minD = distanceList.get(0);
		double maxD = distanceList.get(distanceList.size() - 1);
		
//		int nR = (int) ((Earth.EARTH_RADIUS - 3480.) / deltaR) + 1;
//		deltaR = (Earth.EARTH_RADIUS - 3480.) / (nR-1);
		int nR = (int) ((maxR - 3480.) / deltaR) + 1;
		deltaR = (maxR - 3480.) / (nR-1);
		int nD = (int) ((maxD - minD) / deltaD) + 1;
		deltaD = (maxD - minD) / (nD-1);
		System.out.println(nR + " " + nD);
		
		double[][] complemented = new double[nR * nD][];
		
		
		Location[] locations = sensitivityMap.keySet().stream().map(key -> new Location(0., key.distance, key.r))
				.toArray(Location[]::new);
		
		Complementation c = new Complementation();
		for (int i = 0; i < nR; i++) {
			for (int j = 0; j < nD; j++) {
				Location loc = new Location(0., minD + j * deltaD, 3480. + i * deltaR);
				Location[] nearPoints = c.getNearest4(locations, loc);
				double[] nearpointsValue = new double[nearPoints.length];
				for (int k = 0; k < nearPoints.length; k++)	{
//					System.out.println(nearPoints[k].getEpicentralDistance(loc)* 180. / Math.PI);
					nearpointsValue[k] = sensitivityMap.get(new PerturbationR_distance(nearPoints[k].getR(), nearPoints[k].getLongitude()));	//TODO
//					System.out.println(nearPoints[k].getLongitude());
				}
				double value = c.complement(nearPoints, nearpointsValue, loc);
				
				complemented[i*nD + j] = new double[] {loc.getLongitude(), loc.getR(), value};
			}
		}
		
		return complemented;
	}
	
	public void write1D(Path outpath) throws IOException {
		double[][] rSensitvity = shrinkTo1D();
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))) {
			pw.println("#perturbationR, sensitivity");
			for (double[] rs : rSensitvity)
				pw.println(rs[0] + " " + rs[1]);
		}
	}
	
	public void compute() {
		for (PartialID id : ids) {		//ids はpartialIDのリスト
			if (id.getPartialType().isTimePartial()) {
				System.out.println("Ignoring for time partial...");
				continue;
			}	
			if (includePhases == null || includePhases.contains(new Phases(id.getPhases()))) {
				PerturbationR_distance rDistance = new PerturbationR_distance(id);
				if (sensitivityMap.containsKey(rDistance)) {
					double sensitivity = idToSensitivity(id)
							+ sensitivityMap.get(rDistance);
					sensitivityMap.replace(rDistance, sensitivity);
				}
				else {
					double sensitivity = idToSensitivity(id);
					sensitivityMap.put(rDistance, sensitivity);
				}
			}
		}
	}
	
	public void maxAmplitudeMapMaker() {
		Arrays.stream(basicIDs).filter(basicID -> basicID.getWaveformType().equals(WaveformType.SYN))
								.forEach(basicID -> {
			PerturbationDistance pDistance = new PerturbationDistance(basicID);
			double maxAmplitude = MaxAmplitudeGet.computeMaxAmplitude(basicID);
			maxAmplitudeMap.put(pDistance, maxAmplitude);
		});
	}
	
	public void computeSyntheticNormalized() {
		Arrays.stream(ids).filter(id -> !id.getPartialType().isTimePartial())
				.forEach(id -> {	//ids はpartialIDのリスト
			if (includePhases == null || includePhases.contains(new Phases(id.getPhases()))) {
				PerturbationR_distance rDistance = new PerturbationR_distance(id);
				PerturbationDistance pDistance = new PerturbationDistance(id);
				if (sensitivityMap.containsKey(rDistance)) {
					double maxAmpOfSynthetic = maxAmplitudeMap.get(pDistance);
					double sensitivity = idToNormalizedSensitivity(id, maxAmpOfSynthetic)
							+ sensitivityMap.get(rDistance);
					sensitivityMap.replace(rDistance, sensitivity);
				}
				else {
					double sensitivity = idToSensitivity(id);
					sensitivityMap.put(rDistance, sensitivity);
				}
			}
		});
	}
	
	public void compute4Ref() {
		for (PartialID id : ids) {		//ids はpartialIDのリスト
			if (id.getPartialType().isTimePartial()) {
				System.out.println("Ignoring for time partial...");
				continue;
			}	
			if (includePhases == null || includePhases.contains(new Phases(id.getPhases()))) {
				PerturbationR_distance rDistance = new PerturbationR_distance(id);
				if (sensitivityMapRef.containsKey(rDistance)) {
					double sensitivity = idToSensitivity(id)
							+ sensitivityMapRef.get(rDistance);
					sensitivityMapRef.replace(rDistance, sensitivity);
				}
				else {
					double sensitivity = idToSensitivity(id);
					sensitivityMapRef.put(rDistance, sensitivity);
				}
			}
		}
	}
	
	public double[][] shrinkTo1D() {
		Map<Double, Double> rSensitivityMap = new HashMap<>();
		sensitivityMap.forEach((rDist, s) -> {
			if (rSensitivityMap.containsKey(rDist.r)) {
				double tmpS = rSensitivityMap.get(rDist.r) + s;
				rSensitivityMap.put(rDist.r, tmpS);
			}
			else
				rSensitivityMap.put(rDist.r, s);
		});
		SortedSet<Double> keys = new TreeSet<>(rSensitivityMap.keySet());
		double[][] rSensitivity = new double[rSensitivityMap.size()][];
		int i = 0;
		for (Double key : keys) {
			rSensitivity[i] = new double[2];
			rSensitivity[i][0] = key;
			rSensitivity[i][1] = rSensitivityMap.get(key);
			i++;
		}
		return rSensitivity;
	}
	
	public PartialID[] getIds() {
		return ids;
	}
	
	private boolean containPhases(PartialID id, Set<Phase> includePhases) {
		boolean contains = false;
		for (Phase phase : id.getPhases()) {
			if (includePhases.contains(phase))
				contains = true;
		}
		return contains;
	}
	
	public double getMaxSensitivity() {
		double max = Double.MIN_VALUE;
		for(Map.Entry<PerturbationR_distance, Double> entry : sensitivityMap.entrySet()) {
			max = Math.max(max, entry.getValue());
		}
		this.max = max;
		return max;
	}

	public double getMaxRefSensitivity() {
		double max = Double.MIN_VALUE;
		double maxRef = Double.MIN_VALUE;
		for(Map.Entry<PerturbationR_distance, Double> entry : sensitivityMapRef.entrySet()) {
			maxRef = Math.max(maxRef, entry.getValue());
		}
		this.max = max;
		this.maxRef = maxRef;
		return maxRef;
	}	
	
	
	public double getMax() {
		return max;
	}
	
	public double getMaxRef(){
		return maxRef;
	}
	
	private Map<Double, Double> getMaxPerDistance() {
		Map<Double, Double> maxPerDistanceMap = new HashMap<>();
		for (int i = 0; i < 180; i+=2)
			maxPerDistanceMap.put((double) i, 0.);
		for(Map.Entry<PerturbationR_distance, Double> entry : sensitivityMap.entrySet()) {
			if (maxPerDistanceMap.get(entry.getKey().distance) < entry.getValue())
				maxPerDistanceMap.put(entry.getKey().distance, entry.getValue());
		}
		return maxPerDistanceMap;
	}
	
	private Map<Double, Double> getMaxPerDistanceRef() {
		Map<Double, Double> maxPerDistanceRefMap = new HashMap<>();
		for (int i = 0; i < 180; i+=2)
			maxPerDistanceRefMap.put((double) i, 0.);
		for(Map.Entry<PerturbationR_distance, Double> entry : sensitivityMapRef.entrySet()) {
			if (maxPerDistanceRefMap.get(entry.getKey().distance) < entry.getValue())
				maxPerDistanceRefMap.put(entry.getKey().distance, entry.getValue());
		}
		return maxPerDistanceRefMap;
	}
	
	private void normalize() {
		double max = getMaxSensitivity();
		for(Map.Entry<PerturbationR_distance, Double> entry : sensitivityMap.entrySet()) {
			sensitivityMap.replace(entry.getKey(), entry.getValue() / max);
		}
	}
	
	private void normalizePerDistance() {
		Map<Double, Double> maxPerDistanceMap = getMaxPerDistance();
		for(Map.Entry<PerturbationR_distance, Double> entry : sensitivityMap.entrySet()) {
			double max = maxPerDistanceMap.get(entry.getKey().distance);
			if (max > 0)
				sensitivityMap.replace(entry.getKey(), entry.getValue() / max);
			else
				sensitivityMap.replace(entry.getKey(), Double.NaN);
		}
	}
	
	private void normalizePerDistancePerRef() {
		Map<Double, Double> maxPerDistanceMapRef = getMaxPerDistanceRef();
		for(Map.Entry<PerturbationR_distance, Double> entry : sensitivityMap.entrySet()) {
			double maxRef = maxPerDistanceMapRef.get(entry.getKey().distance);
			if (maxRef > 0)
				sensitivityMap.replace(entry.getKey(), entry.getValue() / maxRef);
			else
				sensitivityMap.replace(entry.getKey(), Double.NaN);
		}
	}
	
	public static double idToSensitivity(PartialID id) {
//		return Math.sqrt(new ArrayRealVector(id.getData()).dotProduct(new ArrayRealVector(id.getData())));
		return new ArrayRealVector(id.getData()).dotProduct(new ArrayRealVector(id.getData()));
	}
	
	public static double idToNormalizedSensitivity (PartialID id, double maxAmpOfSynthetic) {
		double[] dataArray = Arrays.stream(id.getData()).map(data -> data / maxAmpOfSynthetic).toArray();
		return new ArrayRealVector(dataArray).dotProduct(new ArrayRealVector(dataArray));
	}
	
	public static class PerturbationR_distance {
		public double r;
		public double distance;
		public PerturbationR_distance(double r, double distance) {
			this.r = r;
			this.distance = (int) (distance / 1.) * 1;
		}
		public PerturbationR_distance(PartialID id) {
			this.r = id.getPerturbationLocation().getR();
			double distance = Precision.round(Math.toDegrees(id.getPerturbationLocation()
								.getEpicentralDistance(id.getStation().getPosition())), 1);
//			double distance = (id.getGlobalCMTID().getEvent().getCmtLocation()
//					.getEpicentralDistance(id.getStation().getPosition())
//					* 180. / Math.PI);
			this.distance = (int) (distance / 1.) * 1;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PerturbationR_distance other = (PerturbationR_distance) obj;
			if (r != other.r)
				return false;
			if (distance != other.distance)
				return false;
			return true;
		}
		@Override
		public int hashCode() {
			int prime = 31;
			return prime * (int) Double.doubleToLongBits(r) * (int) Double.doubleToLongBits(distance);
		}
	}
	
	public static class PerturbationDistance {
		public double distance;
		public PerturbationDistance(double distance) {
			this.distance = (int) (distance / 1.) * 1;
		}
		public PerturbationDistance(BasicID id) {
			//TODO source locationをどうするか
			double distance = Precision.round(Math.toDegrees(new HorizontalPosition(0.0, 0.0)
								.getEpicentralDistance(id.getStation().getPosition())), 1);
//			double distance = (id.getGlobalCMTID().getEvent().getCmtLocation()
//					.getEpicentralDistance(id.getStation().getPosition())
//					* 180. / Math.PI);
			this.distance = (int) (distance / 1.) * 1;
		}
		public PerturbationDistance(PartialID id) {
			//TODO source locationをどうするか
			double distance = Precision.round(Math.toDegrees(id.getPerturbationLocation()
								.getEpicentralDistance(id.getStation().getPosition())), 1);
//			double distance = (id.getGlobalCMTID().getEvent().getCmtLocation()
//					.getEpicentralDistance(id.getStation().getPosition())
//					* 180. / Math.PI);
			this.distance = (int) (distance / 1.) * 1;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PerturbationDistance other = (PerturbationDistance) obj;
			if (distance != other.distance)
				return false;
			return true;
		}
		@Override
		public int hashCode() {
			int prime = 31;
			return prime * (int) Double.doubleToLongBits(distance);
		}
	}
}