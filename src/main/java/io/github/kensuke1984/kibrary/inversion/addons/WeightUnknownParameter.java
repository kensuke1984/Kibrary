package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.inversion.Physical3DParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeightUnknownParameter {
	private UnknownParameterWeightType type;
	private Map<UnknownParameter, Double> weights;
	private List<UnknownParameter> unknowns;
	private Map<UnknownParameter, Double> timeWeightMap;
	
	public WeightUnknownParameter(UnknownParameterWeightType type,
			List<UnknownParameter> unknowns) {
		if (unknowns.stream()
				.filter(p -> p.getPartialType().isTimePartial())
				.count() > 0)
			throw new RuntimeException("Detected time partials. "
					+ "You must specify their weigthings (use the other constructor).");
		this.type = type;
		this.unknowns = unknowns;
		this.weights = new HashMap<>();
		
		computeWeights();
	}
	
	public WeightUnknownParameter(UnknownParameterWeightType type,
			List<UnknownParameter> unknowns, Map<UnknownParameter, Double> timeWeightMap) {
		this.type = type;
		this.unknowns = unknowns;
		this.weights = new HashMap<>();
		this.timeWeightMap = timeWeightMap;
		
		computeWeights();
		applyTimeWeight();
	}
	
	public WeightUnknownParameter(UnknownParameterWeightType type,
			List<UnknownParameter> unknowns, Path sensitivityFile) {
		this.type = type;
		this.unknowns = unknowns;
		this.weights = new HashMap<>();
		
		computeWeights(sensitivityFile);
	}
	
	public Map<UnknownParameter, Double> getWeights() {
		return weights;
	}
	
	public List<UnknownParameter> getUnknowns() {
		return unknowns;
	}
	
	public UnknownParameterWeightType getType() {
		return type;
	}
	
	private void applyTimeWeight() {
		for (UnknownParameter unknown : unknowns) {
			if (unknown.getPartialType().isTimePartial())
				weights.replace(unknown,
						timeWeightMap.get(unknown));
		}
	}
	
	private void computeWeights() {
		if (type.equals(UnknownParameterWeightType.TRANSITION_ZONE)) {
			for (UnknownParameter unknown : unknowns) {
				if (unknown.getPartialType().isTimePartial())
					weights.put(unknown, 1.);
				else {
					double weight = 1.;
					double r = unknown.getLocation().getR();
					if (r >= 5700 && r < 5750) {
						weight = 1.7;
					}
					else if (r < 5700)
						weight = 2.7;
					weights.put(unknown, weight);
				}
			}
		}
	}
	
	private void computeWeights(Path inpath) {
		if (type.equals(UnknownParameterWeightType.AtA)) {
			Map<UnknownParameter, Double> sensitivityMap;
			try {
				sensitivityMap = readSensitivityFile(inpath);
				for (UnknownParameter p : sensitivityMap.keySet()) {
					double weight = 1. / sensitivityMap.get(p).doubleValue();
					weight = weight > 4 ? 4. : weight;
					weight = Math.sqrt(weight);
					
//					TODO find the corresponding unknown (with the good weight)
					UnknownParameter unknown = unknowns.stream().filter(tmp -> tmp.getLocation().equals(p.getLocation()) 
							&& tmp.getPartialType().equals(p.getPartialType())).findFirst().get();
					weights.put(unknown, weight);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public Map<UnknownParameter, Double> readSensitivityFile(Path inpath) throws IOException {
		Map<UnknownParameter, Double> sensitivityMap = new HashMap<>();
		
		BufferedReader br = Files.newBufferedReader(inpath);
		String line;
		while((line = br.readLine()) != null) {
			String[] ss = line.trim().split("\\s+");
			double lat = Double.parseDouble(ss[1]);
			double lon = Double.parseDouble(ss[2]);
			double r = Double.parseDouble(ss[3]);
			double sensitivity = Double.parseDouble(ss[4]);
			PartialType type = PartialType.valueOf(ss[0]);
			
			UnknownParameter p = new Physical3DParameter(type, new Location(lat, lon, r), 1.);
			sensitivityMap.put(p, sensitivity);
		}
		br.close();
		
		return sensitivityMap;
	}
	
	public static double[] readSensitivityFileAndComputeWeight(Path inpath) throws IOException {
		List<Double> weightList = new ArrayList<>();
		
		BufferedReader br = Files.newBufferedReader(inpath);
		String line;
		while((line = br.readLine()) != null) {
			String[] ss = line.trim().split("\\s+");
			double lat = Double.parseDouble(ss[1]);
			double lon = Double.parseDouble(ss[2]);
			double r = Double.parseDouble(ss[3]);
			double sensitivity = Double.parseDouble(ss[4]);
			PartialType type = PartialType.valueOf(ss[0]);
			
			double weight = 1. / sensitivity;
			weight = weight > 3 ? 3. : weight;
			weight = Math.sqrt(weight);
			weightList.add(weight);
		}
		br.close();
		
		double[] weights = new double[weightList.size()];
		for (int i = 0; i < weights.length; i++)
			weights[i] = weightList.get(i);
		
		return weights;
	}
}
