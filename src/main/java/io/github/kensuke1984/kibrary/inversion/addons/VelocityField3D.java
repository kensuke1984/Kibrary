package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

public class VelocityField3D {

	public static void main(String[] args) throws IOException {
		Path inversionResultPath = Paths.get(".");
		
		Boolean jackknife = false;
		int nRes = 1;
		double dL = 2.;
		Path perturbationLayerPath = inversionResultPath.resolve("perturbationLayer.inf"); 
		int n = 25;
		
		if (args.length == 1) {
			dL = Double.parseDouble(args[0]);
		}
		else if (args.length == 2) {
			jackknife = true;
			dL = Double.parseDouble(args[0]);
			nRes = Integer.parseInt(args[1]);
		}
		
		System.out.println("dL = " + dL);
		
		PolynomialStructure structure = PolynomialStructure.PREM;
//		PolynomialStructure structure = PolynomialStructure.PREM_PRIME;
//		PolynomialStructure structure = PolynomialStructure.AK135;
		System.out.println("Using structure " + structure.toString());
		
		InversionResult ir = new InversionResult(inversionResultPath, true);
		List<UnknownParameter> unknowns = ir.getUnknownParameterList();
		
		// read layer thickness
		Map<Double, Double> layerMap = new HashMap<>();
		Files.readAllLines(perturbationLayerPath).stream().forEach(s -> {
			Double r = Double.parseDouble(s.trim().split(" ")[0]);
			Double d = Double.parseDouble(s.trim().split(" ")[1]);
			layerMap.put(r, d);
		});
		
		double[] perturbationRs = new double[layerMap.size()];
		int count = 0;
		for (double r : layerMap.keySet())
			perturbationRs[count++] = r;
		
		Set<Double> tmpLayers = new HashSet<>();
		layerMap.keySet().stream().forEach(r -> {
			double h = layerMap.get(r);
			double depth = 6371. - r;
			
			tmpLayers.add(new BigDecimal(depth - h/2.).setScale(4, RoundingMode.HALF_UP).doubleValue());
			tmpLayers.add(new BigDecimal(depth + h/2.).setScale(4, RoundingMode.HALF_UP).doubleValue());
		});
		double[] perturbationLayers = tmpLayers.stream().sorted().mapToDouble(d -> d).toArray();
		for (double depth : perturbationLayers)
			System.out.println(depth);
		
		for (InverseMethodEnum inverse : ir.getInverseMethods()) {
			if (!Files.exists(inversionResultPath.resolve(inverse.simple() + "_mean")))
					Files.createDirectory(inversionResultPath.resolve(inverse.simple() + "_mean"));
			
			System.out.println(inverse);
			
			if (!jackknife) {
				for (int i = 1; i <= n; i++) {
					Path outpath = inversionResultPath.resolve(inverse.simple() + "/" + "velocity" + inverse.simple() + i + ".txt");
					
					Map<UnknownParameter, Double> answerMap = ir.answerMapOf(inverse, i);
					
					Map<Location, List<PerturbationValue>> perturbations = toPerturbation(answerMap, layerMap, unknowns, structure);
					Map<Location, List<PerturbationValue>> extendedPerturbations = extendedPerturbationMap(perturbations, dL, perturbationRs);
//					Map<Location, List<PerturbationValue>> extendedPerturbations = perturbations;
					
					//write
					PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
					for (Location loc : extendedPerturbations.keySet()) {
						List<PerturbationValue> values = extendedPerturbations.get(loc);
						double lat = loc.getLatitude();
						double lon = loc.getLongitude();
						if (lon < 0)
							lon += 360;
						double r = loc.getR();
						pw.print(lat + " " + lon + " " + r + " ");
						for (PerturbationValue value : values)
							pw.print(value.getValue() + " ");
						pw.println();
					}
					pw.close();
					
					//write model for specfem
					Path outpahSpecfem = inversionResultPath.resolve(inverse.simple() + "/" + "specfem_velocity" + inverse.simple() + i + ".txt");
					writeSpecfemPerturbationMap(outpahSpecfem, perturbations, dL, perturbationLayers);		
				}
				
//				for (int i = n; i <= unknowns.size(); i+=10) {
//					Path outpath = inversionResultPath.resolve(inverse.simple() + "/" + "velocity" + inverse.simple() + i + ".txt");
//					
//					Map<UnknownParameter, Double> answerMap = ir.answerMapOf(inverse, i);
//					
//					Map<Location, List<PerturbationValue>> perturbations = toPerturbation(answerMap, layerMap, unknowns, structure);
//					Map<Location, List<PerturbationValue>> extendedPerturbations = extendedPerturbationMap(perturbations, dL, perturbationRs);
//					
//					
//					//write
//					PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
//					for (Location loc : extendedPerturbations.keySet()) {
//						List<PerturbationValue> values = extendedPerturbations.get(loc);
//						pw.print(loc + " ");
//						for (PerturbationValue value : values)
//							pw.print(value.getValue() + " ");
//						pw.println();
//					}
//					pw.close();
//				}
			}
			
			else {
				n = 12;
				for (int i = 1; i <= n; i++) {
					System.out.println("Computing " + i);
					List<Map<Location, List<PerturbationValue>>> perturbationMapArray = new ArrayList<>();
					Map<Location, List<PerturbationValue>> meanPerturbationMap = new HashMap<>();
					
					for (int ires = 0; ires < nRes; ires++) {
						Path outpath = inversionResultPath.resolve(inverse.simple() + ires + "/" + "velocity" + inverse.simple() + i + ".txt");
						
						Map<UnknownParameter, Double> answerMap = ir.answerMapOf(inverse, i, ires);
						
						Map<Location, List<PerturbationValue>> perturbations = toPerturbation(answerMap, layerMap, unknowns, structure);
						Map<Location, List<PerturbationValue>> extendedPerturbations = extendedPerturbationMap(perturbations, dL, perturbationRs);
						
						perturbationMapArray.add(extendedPerturbations);
						
						//write
						PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
						for (Location loc : extendedPerturbations.keySet()) {
							List<PerturbationValue> values = extendedPerturbations.get(loc);
							pw.print(loc + " ");
							for (PerturbationValue value : values)
								pw.print(value.getValue() + " ");
							pw.println();
							
							//compute mean perturbations
							if (meanPerturbationMap.containsKey(loc)) {
								List<PerturbationValue> meanValues = meanPerturbationMap.get(loc);
								for (int iv = 0; iv < values.size(); iv++)
									meanValues.set(iv, meanValues.get(iv).add(values.get(iv)));
								meanPerturbationMap.put(loc, meanValues);
							}
							else {
								List<PerturbationValue> tmpValues = values.stream().collect(Collectors.toList());
								meanPerturbationMap.put(loc, tmpValues);
							}
						}
						pw.close();
					}
				
					Path outpathMean = inversionResultPath.resolve(inverse.simple() + "_mean/" + "velocity" + inverse.simple() + i + "_mean" + ".txt");
					Path outpathVar = inversionResultPath.resolve(inverse.simple() + "_mean/" + "velocity" + inverse.simple() + i + "_var" + ".txt");
					PrintWriter pwMean = new PrintWriter(Files.newBufferedWriter(outpathMean, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
					PrintWriter pwVar = new PrintWriter(Files.newBufferedWriter(outpathVar, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
					
					for (Location loc : meanPerturbationMap.keySet()) {
						pwMean.print(loc + " ");
						pwVar.print(loc + " ");
						
						List<PerturbationValue> meanValues = meanPerturbationMap.get(loc);
						for (int iv = 0; iv < meanValues.size(); iv++) {
							PerturbationValue meanValue = meanValues.get(iv);
							double mean = meanValue.getValue() / nRes;
							
							double variance = 0;
							for (int ires = 0; ires < nRes; ires++) {
								PerturbationValue value = perturbationMapArray.get(ires).get(loc).get(iv);
								if (!value.getType().equals(meanValue.getType()))
									throw new RuntimeException("Types differ");
								
//								if (loc.equals(new Location(19.0,-95.0,5591.0)))
//									System.out.println(value.getValue() + " " + mean);
								
								variance += (value.getValue() - mean) * (value.getValue() - mean); 
							}
							variance = Math.sqrt(variance / (nRes - 1));
							
							pwMean.print(mean + " ");
							pwVar.print(variance + " ");
						}
						pwMean.println();
						pwVar.println();
					}
					
					pwMean.close();
					pwVar.close();
				
				} //END jacknife
			}
		}
	}
	
	private static Map<Location, List<PerturbationValue>> toPerturbation(Map<UnknownParameter, Double> answerMap, Map<Double, Double> layerMap,
			List<UnknownParameter> parameterOrder, PolynomialStructure structure) {
		Map<Location, List<PerturbationValue>> perturbationMap = new HashMap<>();
		List<Location> locations = parameterOrder.stream().map(p -> p.getLocation()).distinct().collect(Collectors.toList());
		
		for (Location location : locations) {
			List<UnknownParameter> tmpParameters = parameterOrder.stream().filter(p -> p.getLocation().equals(location)).collect(Collectors.toList());
			Map<PartialType, Double> typeValueMap = new HashMap<>();
			for (UnknownParameter p : tmpParameters)
				typeValueMap.put(p.getPartialType(), answerMap.get(p));
			
			double r = location.getR();
			double dR = 0;
			try {
				dR = layerMap.get(r);
			} catch (NullPointerException e) {
				System.out.println("NullPointerException: didn't find layer for radius " + r);
				e.printStackTrace();
			}
			double rmin = r - dR / 2.;
			double rmax = r + dR / 2.;
			
			if (structure.equals(PolynomialStructure.AK135)) {
				if (r == 6343.8) {
					rmin = 6336.61;
					rmax = Earth.EARTH_RADIUS;
				}
			}

			List<PerturbationValue> perturbations = toPerturbation(typeValueMap, rmin, rmax, structure);
			perturbationMap.put(location, perturbations);
		}
		return perturbationMap;
	}
	
	private static List<PerturbationValue> toPerturbation(Map<PartialType, Double> typeValueMap, double rmin, double rmax, PolynomialStructure structure) {
		List<PerturbationValue> perturbations = new ArrayList<>();
		Set<PartialType> partialTypes = typeValueMap.keySet();
		
		if (partialTypes.contains(PartialType.MU) && partialTypes.size() == 1) {
			double dMu = typeValueMap.get(PartialType.MU);
			double dvs = getSimpsonVsPerturbation(rmin, rmax, structure, dMu) * 100;
			double dvp = getSimpsonVpPerturbation(rmin, rmax, structure, dMu, 0.) * 100;
			perturbations.add(new PerturbationValue(PerturbationType.Vs, dvs));
			perturbations.add(new PerturbationValue(PerturbationType.Vp, dvp));
		}
		if (partialTypes.contains(PartialType.Vs) && partialTypes.size() == 1) {
			double dvs = typeValueMap.get(PartialType.Vs);
			dvs = getSimpsonVsPerturbationFromVs(rmin, rmax, structure, dvs) * 100;
			perturbations.add(new PerturbationValue(PerturbationType.Vs, dvs));
			perturbations.add(new PerturbationValue(PerturbationType.Vp, 0.));
			perturbations.add(new PerturbationValue(PerturbationType.Vb, 0.));
		}
		if (partialTypes.contains(PartialType.LAMBDA2MU) && partialTypes.size() == 1) {
			double dLambda2mu = typeValueMap.get(PartialType.LAMBDA2MU);
			double dvp = getSimpsonVpPerturbation(rmin, rmax, structure, 0., dLambda2mu) * 100;
			perturbations.add(new PerturbationValue(PerturbationType.Vs, 0.));
			perturbations.add(new PerturbationValue(PerturbationType.Vp, dvp));
			perturbations.add(new PerturbationValue(PerturbationType.Vb, 0.));
		}
		if (partialTypes.contains(PartialType.MU) && partialTypes.contains(PartialType.LAMBDA)) {
			double dMu = typeValueMap.get(PartialType.MU);
			double dLambda = typeValueMap.get(PartialType.LAMBDA);
			double dKappa = dLambda + 2./3. * dMu;
			double dvs = getSimpsonVsPerturbation(rmin, rmax, structure, dMu) * 100;
			double dvp = getSimpsonVpPerturbation(rmin, rmax, structure, dMu, dLambda) * 100;
			double dvb = getSimpsonVbPerturbation(rmin, rmax, structure, dKappa) * 100;
			perturbations.add(new PerturbationValue(PerturbationType.Vs, dvs));
			perturbations.add(new PerturbationValue(PerturbationType.Vp, dvp));
			perturbations.add(new PerturbationValue(PerturbationType.Vb, dvb));
		}
		if (partialTypes.contains(PartialType.MU) && partialTypes.contains(PartialType.KAPPA)) {
			double dMu = typeValueMap.get(PartialType.MU);
			double dKappa = typeValueMap.get(PartialType.KAPPA);
			double dLambda = dKappa - 2./3. * dMu;
			double dvs = getSimpsonVsPerturbation(rmin, rmax, structure, dMu) * 100;
			double dvp = getSimpsonVpPerturbation(rmin, rmax, structure, dMu, dLambda) * 100;
			double dvb = getSimpsonVbPerturbation(rmin, rmax, structure, dKappa) * 100;
			perturbations.add(new PerturbationValue(PerturbationType.Vs, dvs));
			perturbations.add(new PerturbationValue(PerturbationType.Vp, dvp));
			perturbations.add(new PerturbationValue(PerturbationType.Vb, dvb));
		}
		if (partialTypes.contains(PartialType.MU) && partialTypes.contains(PartialType.LAMBDA2MU)) {
			double dMu = typeValueMap.get(PartialType.MU);
			double dLambda2Mu = typeValueMap.get(PartialType.LAMBDA2MU);
			double dKappa = dLambda2Mu - 4./3. * dMu;
			double dvs = getSimpsonVsPerturbation(rmin, rmax, structure, dMu) * 100;
			double dvp = getSimpsonVpPerturbation(rmin, rmax, structure, 0., dLambda2Mu) * 100;
			double dvb = getSimpsonVbPerturbation(rmin, rmax, structure, dKappa) * 100;
			perturbations.add(new PerturbationValue(PerturbationType.Vs, dvs));
			perturbations.add(new PerturbationValue(PerturbationType.Vp, dvp));
			perturbations.add(new PerturbationValue(PerturbationType.Vb, dvb));
		}
		if (partialTypes.contains(PartialType.KAPPA) && partialTypes.size() == 1) {
			double dKappa = typeValueMap.get(PartialType.KAPPA);
			double dvb = getSimpsonVbPerturbation(rmin, rmax, structure, dKappa) * 100;
			double dvp = getSimpsonVpPerturbation(rmin, rmax, structure, 0, dKappa) * 100;
			
			perturbations.add(new PerturbationValue(PerturbationType.Vb, dvb));
			perturbations.add(new PerturbationValue(PerturbationType.Vp, dvp));
		}
		if (partialTypes.contains(PartialType.LAMBDA) && partialTypes.size() == 1) {
			double dlambda = typeValueMap.get(PartialType.LAMBDA);
			double dMu = 0;
			double dLambda = dlambda;
			double dvp = getSimpsonVpPerturbation(rmin, rmax, structure, dMu, dLambda) * 100;
			perturbations.add(new PerturbationValue(PerturbationType.Vp, dvp));
		}
		
//		if (partialTypes.contains(PartialType.MU)) {
//			double dMu = typeValueMap.get(PartialType.MU);
//			perturbations.add(new PerturbationValue(PerturbationType.MU, dMu));
//		}
//		if (partialTypes.contains(PartialType.MU) && partialTypes.contains(PartialType.LAMBDA)) {
//			double dLambda = typeValueMap.get(PartialType.LAMBDA);
//			perturbations.add(new PerturbationValue(PerturbationType.LAMBDA, dLambda));
//		}
//		if (partialTypes.contains(PartialType.MU) && partialTypes.contains(PartialType.KAPPA)) {
//			double dKappa = typeValueMap.get(PartialType.KAPPA);
//			perturbations.add(new PerturbationValue(PerturbationType.KAPPA, dKappa));
//		}
//		if (partialTypes.contains(PartialType.MU) && partialTypes.contains(PartialType.LAMBDA2MU)) {
//			double dLambda2Mu = typeValueMap.get(PartialType.LAMBDA2MU);
//			perturbations.add(new PerturbationValue(PerturbationType.LAMBDA2MU, dLambda2Mu));
//		}
		
		return perturbations;
	}
	
	public static double getSimpsonVsPerturbation(double r1, double r2, PolynomialStructure structure, double dMu) {
		double res = 0;
		double dr = (r2 - r1) / 40.;
		double vol = r2 - r1;
		for (int i=0; i < 40; i++) {
			double a = r1 + i * dr;
			double b = r1 + (i + 1) * dr;
			double ab = (a + b) / 2.;
			double v_a = dvs(a, dMu, structure);
			double v_ab = dvs(ab, dMu, structure);
			double v_b = dvs(b, dMu, structure);
			res += (b - a) / 6. * (v_a + 4 * v_ab + v_b);
		}
		
		return res / vol;
	}
	
	public static double getSimpsonVsPerturbationFromVs(double r1, double r2, PolynomialStructure structure, double dVs) {
		double res = 0;
		double dr = (r2 - r1) / 40.;
		double vol = r2 - r1;
		for (int i=0; i < 40; i++) {
			double a = r1 + i * dr;
			double b = r1 + (i + 1) * dr;
			double ab = (a + b) / 2.;
			double v_a = dvsFromVs(a, dVs, structure);
			double v_ab = dvsFromVs(ab, dVs, structure);
			double v_b = dvsFromVs(b, dVs, structure);
			res += (b - a) / 6. * (v_a + 4 * v_ab + v_b);
		}
		
		return res / vol;
	}
	
	public static double getSimpsonVpPerturbation(double r1, double r2, PolynomialStructure structure, double dMu, double dLambda) {
		double res = 0;
		double dr = (r2 - r1) / 40.;
		double vol = r2 - r1;
		for (int i=0; i < 40; i++) {
			double a = r1 + i * dr;
			double b = r1 + (i + 1) * dr;
			double ab = (a + b) / 2.;
			double v_a = dvp(a, dMu, dLambda, structure);
			double v_ab = dvp(ab, dMu, dLambda, structure);
			double v_b = dvp(b, dMu, dLambda, structure);
			res += (b - a) / 6. * (v_a + 4 * v_ab + v_b);
		}
		
		return res / vol;
	}
	
	public static double getSimpsonVbPerturbation(double r1, double r2, PolynomialStructure structure, double dKappa) {
		double res = 0;
		double dr = (r2 - r1) / 40.;
		double vol = r2 - r1;
		for (int i=0; i < 40; i++) {
			double a = r1 + i * dr;
			double b = r1 + (i + 1) * dr;
			double ab = (a + b) / 2.;
			double v_a = dvb(a, dKappa, structure);
			double v_ab = dvb(ab, dKappa, structure);
			double v_b = dvb(b, dKappa, structure);
			res += (b - a) / 6. * (v_a + 4 * v_ab + v_b);
		}
		
		return res / vol;
	}
	
	private static double dvs(double r, double dMu, PolynomialStructure structure) {
		double v0 = Math.sqrt(structure.computeMu(r) / structure.getRhoAt(r));
		double v1 = Math.sqrt((structure.computeMu(r) + dMu) / structure.getRhoAt(r));
		return (v1 - v0) / v0;
	}
	
	private static double dvsFromVs(double r, double dVs, PolynomialStructure structure) {
		double v0 = Math.sqrt(structure.computeMu(r) / structure.getRhoAt(r));
		return dVs / v0;
	}
	
	private static double dvp(double r, double dMu, double dLambda, PolynomialStructure structure) {
		double v0 = structure.getVphAt(r);
		double v1 = Math.sqrt((structure.computeLambda(r) + dLambda + 2 * (structure.computeMu(r) + dMu)) / structure.getRhoAt(r));
		return (v1 - v0) / v0;
	}
	
	private static double dvb(double r, double dKappa, PolynomialStructure structure) {
		double v0 = structure.getVbAt(r);
		double v1 = Math.sqrt((structure.computeKappa(r) + dKappa) / structure.getRhoAt(r));
		return (v1 - v0) / v0;
	}
	
	public static Map<Location, List<PerturbationValue>> extendedPerturbationMap(Map<Location, List<PerturbationValue>> perturbationMap, double dL, double[] perturbationRs) {
		Map<Location, List<PerturbationValue>> extended = new HashMap<>();
		double minLat = 1e3;
		double maxLat = -1e3;
		double minLon = 1e3;
		double maxLon = -1e3;
		
		Location tmpLoc = perturbationMap.keySet().stream().findFirst().get();
		List<PerturbationValue> tmpvalues = perturbationMap.get(tmpLoc);
		
		List<PerturbationValue> zeroValues = new ArrayList<>();
		List<PerturbationValue> nanValues = new ArrayList<>();
		for (PerturbationValue value : tmpvalues) {
			zeroValues.add(new PerturbationValue(value.getType(), 0.));
			nanValues.add(new PerturbationValue(value.getType(), Double.NaN));
		}
		
		Set<Location> locations = perturbationMap.keySet();
		for (Location loci : locations) {
			List<PerturbationValue> values = perturbationMap.get(loci);
			extended.put(loci, values);
			
			if (loci.getLatitude() < minLat)
				minLat = loci.getLatitude();
			if (loci.getLongitude() < minLon)
				minLon = loci.getLongitude();
			if (loci.getLatitude() > maxLat)
				maxLat = loci.getLatitude();
			if (loci.getLongitude() > maxLon)
				maxLon = loci.getLongitude();
			
			Location[] additionalLocs = new Location[] {new Location(loci.getLatitude(), loci.getLongitude() + dL, loci.getR())
			, new Location(loci.getLatitude(), loci.getLongitude() - dL, loci.getR())
			, new Location(loci.getLatitude() + dL, loci.getLongitude(), loci.getR())
			, new Location(loci.getLatitude() - dL, loci.getLongitude(), loci.getR())
			, new Location(loci.getLatitude() + dL, loci.getLongitude() + dL, loci.getR())
			, new Location(loci.getLatitude() + dL, loci.getLongitude() - dL, loci.getR())
			, new Location(loci.getLatitude() - dL, loci.getLongitude() + dL, loci.getR())
			, new Location(loci.getLatitude() - dL, loci.getLongitude() - dL, loci.getR())};
			
			Set<Location> thisRLocations = locations.stream()
					.filter(loc -> loc.getR() == loci.getR()).collect(Collectors.toSet());
			boolean[] isAdds = new boolean[additionalLocs.length];
			for (int j = 0; j < isAdds.length; j++)
				isAdds[j] = true;
			for (Location loc : thisRLocations) {
				for (int k = 0; k < additionalLocs.length; k++) {
					if (loc.equals(additionalLocs[k]))
						isAdds[k] = false;
				}
			}
			
				for (int j = 0; j < additionalLocs.length; j++) {
					if (isAdds[j] && !extended.containsKey(additionalLocs[j])) {
						extended.put(additionalLocs[j], zeroValues);
					}
				}
		}
		
		// fill remaining voxels with NaN
		int nLon = (int) ((maxLon - minLon) / dL) + 3;
		int nLat = (int) ((maxLat - minLat) / dL) + 3;
		for (int k=0; k < perturbationRs.length; k++) {
			double r = perturbationRs[k];
			for (int i=0; i < nLon; i++) {
				for (int j=0; j < nLat; j++) {
					double lon = minLon + (i - 1) * dL;
					double lat = minLat + (j - 1) * dL;
					Location loc = new Location(lat, lon, r);
					if (!extended.containsKey(loc))
						extended.put(loc, nanValues);
				}
			}
		}

		return extended;
	}
	
	public static void writeSpecfemPerturbationMap(Path outpath, Map<Location, List<PerturbationValue>> perturbationMap, double dL, double[] perturbationLayers) {
		double minLat = 1e3;
		double maxLat = -1e3;
		double minLon = 1e3;
		double maxLon = -1e3;
		
		try {
			PrintWriter pw = new PrintWriter(outpath.toFile());
			
			Location tmpLoc = perturbationMap.keySet().stream().findFirst().get();
			List<PerturbationValue> tmpvalues = perturbationMap.get(tmpLoc);
			
			List<PerturbationValue> zeroValues = new ArrayList<>();
			List<PerturbationValue> nanValues = new ArrayList<>();
			for (PerturbationValue value : tmpvalues) {
				zeroValues.add(new PerturbationValue(value.getType(), 0.));
				nanValues.add(new PerturbationValue(value.getType(), Double.NaN));
			}
			
			Set<Location> locations = perturbationMap.keySet();
			
			for (Location loci : locations) {
				if (loci.getLatitude() < minLat)
					minLat = loci.getLatitude();
				if (loci.getLongitude() < minLon)
					minLon = loci.getLongitude();
				if (loci.getLatitude() > maxLat)
					maxLat = loci.getLatitude();
				if (loci.getLongitude() > maxLon)
					maxLon = loci.getLongitude();
			}
			
			maxLat += dL;
			minLat -= dL;
			maxLon += dL;
			minLon -= dL;
			
			int nlat = (int) ((maxLat - minLat) / dL) + 1;
			int nlon = (int) ((maxLon - minLon) / dL) + 1;
			
			//depths
			double[] depthsVoxels = locations.stream().mapToDouble(loc -> 6371. - loc.getR()).distinct().sorted().toArray();
			double[] depths = perturbationLayers;
			double dR = 2.5;
			double minDepth = depths[0];
			double maxDepth = depths[depths.length - 1];
			int nR = (int) ((maxDepth - minDepth) / dR);
			
			System.out.println("nlat nlon ndepth = " + nlat + " " + nlon + " " + (nR+2));
			
			double[] lons = new double[nlon];
			double[] lats = new double[nlat];
			for (int ilon = 0; ilon < nlon; ilon++) {
				lons[ilon] = minLon + ilon * dL;
			}
			for (int ilat = 0; ilat < nlat; ilat++) {
				lats[ilat] = minLat + ilat * dL;
			}
			
			double depth = minDepth - dR;
			for (int ilon = 0; ilon < nlon; ilon++) {
				double lon = lons[ilon];
				for (int ilat = 0; ilat < nlat; ilat++) {
					double lat = lats[ilat];
					
					double dvs = 0;
//					Location location = new Location(lat, lon, depthOriginal + 6371.);
//					if (perturbationMap.containsKey(location))
//						dvs = perturbationMap.get(location).get(0).getValue();
					
					pw.println((lon-dL/2.) + " " + (lat-dL/2.) + " " + depth + " " + dvs + " " + 0.);
				}
			}
			
			for (int ir = 0; ir < nR; ir++) {
				depth = minDepth + ir * dR;
				double depthOriginal = findDepth(depth, depths, depthsVoxels);
				if (depthOriginal == -1)
					throw new RuntimeException("Depth not found " + depth);
				for (int ilon = 0; ilon < nlon; ilon++) {
					double lon = lons[ilon];
					for (int ilat = 0; ilat < nlat; ilat++) {
						double lat = lats[ilat];
						
						double dvs = 0;
						Location location = new Location(lat, lon, depthOriginal);
						if (perturbationMap.containsKey(location))
							dvs = perturbationMap.get(location).get(0).getValue();
						
						pw.println((lon-dL/2.) + " " + (lat-dL/2.) + " " + depth + " " + dvs + " " + 0.);
					}
				}
			}
			
			depth = maxDepth;
			for (int ilon = 0; ilon < nlon; ilon++) {
				double lon = lons[ilon];
				for (int ilat = 0; ilat < nlat; ilat++) {
					double lat = lats[ilat];
					
					double dvs = 0;
//					Location location = new Location(lat, lon, depthOriginal + 6371.);
//					if (perturbationMap.containsKey(location))
//						dvs = perturbationMap.get(location).get(0).getValue();
					
					pw.println((lon-dL/2.) + " " + (lat-dL/2.) + " " + depth + " " + dvs + " " + 0.);
				}
			}
			
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static double findDepth(double depth, double[] depthLayers, double[] voxelRadii) {
		for (int i = 0; i < depthLayers.length - 1; i++) {
			if (depth >= depthLayers[i] && depth < depthLayers[i+1]) {
				return 6371. - voxelRadii[i];
			}
		}
		return -1.;
	}
	
}
