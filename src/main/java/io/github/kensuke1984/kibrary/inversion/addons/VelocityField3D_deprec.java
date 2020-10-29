package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;
import javax.swing.JOptionPane;

import org.apache.commons.io.input.CloseShieldInputStream;


public class VelocityField3D_deprec {
	
	public static void main(String[] args) throws IOException {
		String inversionResultString = null;
		String polynomialStructureString = null;
		String polynomialStructureStringAbsolute = null;
		Path inversionResultPath = null;
		Path polynomialStructurePath = null;
		String partialCombination = null;
		String perturbationLayerString = null;
		Path perturbationLayerPath = null;
		double amplifyPerturbation = 1.;
		if (args.length == 1) {
			amplifyPerturbation = Double.parseDouble(args[0]);
		}
		try {
			inversionResultString = JOptionPane.showInputDialog("Inversion result folder?", inversionResultString);
			polynomialStructureString = JOptionPane.showInputDialog("Polynomial structure?", polynomialStructureString);
		} catch (Exception e) {
			System.out.println("Inversion result folder?");
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new CloseShieldInputStream(System.in)))) {
				inversionResultString = br.readLine().trim();
				if (!inversionResultString.startsWith("/"))
					inversionResultString = System.getProperty("user.dir") + "/" + inversionResultString;
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new RuntimeException();
			}
			System.out.println("Polynomial structure?");
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new CloseShieldInputStream(System.in)))) {
				polynomialStructureString = br.readLine().trim();
//				if (!polynomialStructureString.startsWith("/"))
//					polynomialStructureStringAbsolute = System.getProperty("user.dir") + "/" + polynomialStructureString;
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new RuntimeException();
			}
			System.out.println("Partial combination (trs | sc | nc)?");
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new CloseShieldInputStream(System.in)))) {
				partialCombination = br.readLine().trim();
				if (!(partialCombination.equals("trs") || partialCombination.equals("sc") || partialCombination.equals("nc")))
					throw new RuntimeException("Syntax: trs | sc | nc");
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new RuntimeException();
			}
			System.out.println("Layer perturbation file?");
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new CloseShieldInputStream(System.in)))) {
				perturbationLayerString = br.readLine().trim();
				if (!perturbationLayerString.startsWith("/"))
					perturbationLayerString = System.getProperty("user.dir") + "/" + perturbationLayerString;
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new RuntimeException();
			}
		}
		if (inversionResultString == null || inversionResultString.isEmpty())
			return;
		inversionResultPath = Paths.get(inversionResultString);
		if (!Files.isDirectory(inversionResultPath))
			throw new RuntimeException("No such directory " + inversionResultPath.toString());
		if (polynomialStructureString == null || polynomialStructureString.isEmpty())
			throw new RuntimeException("Error: please input a path to a polynomial structure");
		PolynomialStructure structure = null;
		switch (polynomialStructureString) {
		case "ak135":
			structure = PolynomialStructure.AK135;
			break;
		case "AK135":
			structure = PolynomialStructure.AK135;
			break;
		case "prem":
			structure = PolynomialStructure.PREM;
			break;
		case "PREM":
			structure = PolynomialStructure.PREM;
			break;
		case "iso_prem":
			structure = PolynomialStructure.ISO_PREM;
			break;
		case "iprem":
			structure = PolynomialStructure.ISO_PREM;
			break;
		case "stw105":
			polynomialStructurePath = Paths.get("/mnt/melonpan/anpan/inversion/Dpp/POLY/stw105_smallCoeff.poly");
			if (!Files.isRegularFile(polynomialStructurePath) || !Files.isReadable(polynomialStructurePath))
				throw new RuntimeException("Error: no such file " + polynomialStructurePath.toString());
			structure = new PolynomialStructure(polynomialStructurePath);
			break;
		case "STW105":
			polynomialStructurePath = Paths.get("/mnt/melonpan/anpan/inversion/Dpp/POLY/stw105_smallCoeff.poly");
			if (!Files.isRegularFile(polynomialStructurePath) || !Files.isReadable(polynomialStructurePath))
				throw new RuntimeException("Error: no such file " + polynomialStructurePath.toString());
			structure = new PolynomialStructure(polynomialStructurePath);
			break;
		case "mak135":
			polynomialStructurePath = Paths.get("/mnt/doremi/anpan/inversion/upper_mantle/CA/NEW/POLYNOMIALS/mak135.poly");
			if (!Files.isRegularFile(polynomialStructurePath) || !Files.isReadable(polynomialStructurePath))
				throw new RuntimeException("Error: no such file " + polynomialStructurePath.toString());
			structure = new PolynomialStructure(polynomialStructurePath);
			break;
		default:
			break;
		}
		if (structure == null) {
			polynomialStructurePath = Paths.get(polynomialStructureStringAbsolute);
			if (!Files.isRegularFile(polynomialStructurePath) || !Files.isReadable(polynomialStructurePath))
				throw new RuntimeException("Error: no such file " + polynomialStructureStringAbsolute);
			structure = new PolynomialStructure(polynomialStructurePath);
		}
		
		// read layer thickness
			perturbationLayerPath = Paths.get(perturbationLayerString); 
			Map<Double, Double> layerMap = new HashMap<>();
			Files.readAllLines(perturbationLayerPath).stream().forEach(s -> {
				Double r = Double.parseDouble(s.trim().split(" ")[0]);
				Double d = Double.parseDouble(s.trim().split(" ")[1]);
				layerMap.put(r, d);
			});
		
		InversionResult ir = new InversionResult(inversionResultPath, true);
		List<UnknownParameter> unknowns = ir.getUnknownParameterList();
		List<UnknownParameter> originalUnknowns = ir.getOriginalUnknownParameterList();
		TriangleRadialSpline trs = null;
		List<HorizontalPosition> horizontalPoints = unknowns.stream().map(p -> p.getLocation().toHorizontalPosition())
				.distinct().collect(Collectors.toList());
		if (partialCombination.equals("trs")) {
			Map<PartialType, Integer[]> nNewParameter = trs.parseNParameters(unknowns);
			trs = new TriangleRadialSpline(nNewParameter, originalUnknowns);
		}
		Set<PartialType> partialTypes = unknowns.stream().map(UnknownParameter::getPartialType).collect(Collectors.toSet());
		if (partialTypes.contains(PartialType.MU)) {
			for (InverseMethodEnum inverse : ir.getInverseMethods()) {
				Path outpath = inversionResultPath.resolve(inverse.simple() + "/" + "velocityInitialModel" + ".txt");
				
				try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
					pw.println("# perturbationR Vsh");
					for (int j = 0; j <= 10000; j++) {
						double r = 3480. + (Earth.EARTH_RADIUS - 3480.) / 10000. * j;
						pw.println(r + " " + structure.getVshAt(r));
					}
				}
				int n = unknowns.size();
//				n = 15;
				for (int i = 1; i <= n; i+=1) {
					outpath = inversionResultPath.resolve(inverse.simple() + "/" + "velocity" + inverse.simple() + i + ".txt");
					Path outpathForCrosssection = inversionResultPath.resolve(inverse.simple() + "/" + "forCrosssection" + inverse.simple() + i + ".txt");
					Path outpathIteration = inversionResultPath.resolve(inverse.simple() + "/" + "velocity" + inverse.simple() + i + "_iteration.txt");
					Path outpathQ = inversionResultPath.resolve(inverse.simple() + "/" + "Q" + inverse.simple() + i + ".txt");
					Map<UnknownParameter, Double> answerMap = ir.answerMapOf(inverse, i);
					Map<Location, Double> locAnswerMap = new HashMap<>();
					answerMap.forEach((m, v) -> locAnswerMap.put(m.getLocation(), v));
					Map<UnknownParameter, Double> zeroMap = new HashMap<>();
					answerMap.forEach((m, v) -> zeroMap.put(m, 0.));
					Map<UnknownParameter, Double> velocities = null;
					Map<UnknownParameter, Double> zeroVelocities = null;
					Map<UnknownParameter, Double> perturbations = null;
					Map<UnknownParameter, Double> perturbations_toAK135 = null;
					Map<Location, Double> extendedPerturbationMap = null;
					Map<Location, Double> zeroMeanPerturbationMap = null;
					Map<Location, Double> extendedZeroMeanPerturbationMap = null;
					Map<Location, Double> perturbationMap = new HashMap<>();
					Map<Location, Double> perturbationMap_toAK135 = new HashMap<>();
					Map<Location, Double> extendedPerturbationMap_toAK135 = null;
					double[][] Qs = null;
					double[][] zeroQs = null;
					double[][] profile1D = null;
					Set<Double> rs = layerMap.keySet();
					double[] perturbationRs = new double[rs.size()];
					if (trs == null) {
						perturbations = toPerturbation(answerMap, layerMap, unknowns, structure, 1.);
						perturbations_toAK135 = toPerturbation(answerMap, layerMap, unknowns, structure, PolynomialStructure.AK135, 1);
//						Set<Double> rs = layerMap.keySet();
//						double[] perturbationRs = new double[rs.size()];
						int count = 0;
						for (double r : rs)
							perturbationRs[count++] = r;
						for (UnknownParameter unknown : perturbations.keySet())
							perturbationMap.put(unknown.getLocation(), perturbations.get(unknown));
						for (UnknownParameter unknown : perturbations_toAK135.keySet())
							perturbationMap_toAK135.put(unknown.getLocation(), perturbations_toAK135.get(unknown));
						zeroMeanPerturbationMap = zeroMeanMap(perturbationMap, perturbationRs);
						extendedPerturbationMap = extendedPerturbationMap(perturbationMap, 2., perturbationRs);
						extendedPerturbationMap_toAK135 = extendedPerturbationMap(perturbationMap_toAK135, 2., perturbationRs);
						extendedZeroMeanPerturbationMap = extendedPerturbationMap(zeroMeanPerturbationMap, 2., perturbationRs);
						zeroVelocities = toVelocity(zeroMap, layerMap, unknowns, structure, 1.);
						if (partialTypes.contains(PartialType.PARQ)) {
							Qs = toQ(answerMap, unknowns, structure, amplifyPerturbation);
							zeroQs = toQ(zeroMap, unknowns, structure, amplifyPerturbation);
						}
						
						profile1D = average1DProfile(perturbationMap, perturbationRs);
					}
					else {
						velocities = toVelocity(answerMap, trs, structure);
						zeroVelocities = toVelocity(zeroMap, trs, structure);
						if (partialTypes.contains(PartialType.PARQ)) {
							Qs = toQ(answerMap, trs, structure, amplifyPerturbation);
							zeroQs = toQ(zeroMap, trs, structure, 1.);
						}
					}
					try {
						PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
						PrintWriter pwCS = new PrintWriter(Files.newBufferedWriter(outpathForCrosssection, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
						PrintWriter pwQ = null;
						PrintWriter pwIteration = new PrintWriter(Files.newBufferedWriter(outpathIteration, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
						if (partialTypes.contains(PartialType.PARQ)) {
							pwQ = new PrintWriter(Files.newBufferedWriter(outpathQ, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
							pwQ.println("#perturbationR final_Q intial_Q");
						}
						//---------------
						if (trs == null) {
							for (Location loc : extendedPerturbationMap.keySet()) {
								double perturbation = extendedPerturbationMap.get(loc);
								double zeroMeanPerturbation = extendedZeroMeanPerturbationMap.get(loc);
								double perturbation_toAK135 = extendedPerturbationMap_toAK135.get(loc);
								pw.println(loc + " " + perturbation + " " + zeroMeanPerturbation + " " + perturbation_toAK135);
							}
							for (Location loc : perturbationMap.keySet()) {
								double perturbation = perturbationMap.get(loc);
								double zeroMeanPerturbation = zeroMeanPerturbationMap.get(loc);
								double perturbation_toAK135 = extendedPerturbationMap_toAK135.get(loc);
								pwCS.println(loc + " " + perturbation + " " + zeroMeanPerturbation + " " + perturbation_toAK135);
							}
							if (partialTypes.contains(PartialType.PARQ)) {
								for (int j = 0; j < Qs.length; j++) {
										pwQ.println(Qs[j][1] +  " " + Qs[j][0] + " " + zeroQs[j][0]);
										pwQ.println(Qs[j][2] +  " " + Qs[j][0] + " " + zeroQs[j][0]);
								}
							}
							
							//1D profile
							Path outpathProfile = inversionResultPath.resolve(inverse.simple() + "/" + "profile1D_" + inverse.simple() + i + ".txt");
							PrintWriter pw2 = new PrintWriter(Files.newBufferedWriter(outpathProfile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
							for (int j = 0; j < profile1D.length; j++) {
								pw2.println(profile1D[j][0] + " " + (6371.- profile1D[j][0]) + " " + profile1D[j][1]);
							}
							pw2.close();
							
							//local 1D profile at each horizontal point
							if (i == 7) {
								for (HorizontalPosition p : horizontalPoints) {
									Path outpath3 = inversionResultPath.resolve(inverse.simple() + "/" + "local1D_" + p.getLatitude() + "_" + p.getLongitude()
											+ "_" + inverse.simple() + i + ".txt");
									PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outpath3, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
									for (double r : perturbationRs) {
										Location loc = p.toLocation(r);
										double d = layerMap.get(r);
										double dmu = locAnswerMap.get(loc);
										double r1 = r - d/2.;
										double r2 = r + d/2.;
										for (int k = 1; k < 100; k++) {
											double rk = r1 + (r2 - r1) / 100. * k;
											double vs_prime = Math.sqrt((structure.computeMu(rk) + dmu) / structure.getRhoAt(rk));
											double vs = structure.getVshAt(rk);
											pw3.println(rk + " " + (6371.-rk) + " " + vs_prime + " " + vs);
										}
//										double vs_prime = getSimpsonVsh(r1, r2, structure, dmu);
//										double vs = structure.getVshAt(r);
//										pw3.println(r1 + " " + (6371.-r1) + " " + vs_prime + " " + vs);
//										pw3.println(r2 + " " + (6371.-r2) + " " + vs_prime + " " + vs);
									}
									pw3.close();
								}
							}
						}
						//---------------
						else {
							for (UnknownParameter m : unknowns) {
								Location loc = (Location) m.getLocation();
								double perturbation = perturbations.get(m);
								pw.println(loc + " " + perturbation);
							}
							if (partialTypes.contains(PartialType.PARQ)) {
								for (int j = 0; j < Qs.length; j++)
									pwQ.println(Qs[j][0] + " " + Qs[j][1] + " " + zeroQs[j][1]);
							}
						}
						
						pw.close();
						pwCS.close();
						if (partialTypes.contains(PartialType.PARQ))
							pwQ.close();
						pwIteration.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private static Map<UnknownParameter, Double> toVelocity(Map<UnknownParameter, Double> answerMap, Map<Double, Double> layerMap, List<UnknownParameter> parameterOrder, PolynomialStructure structure
			, double amplifyPerturbation) {
		Map<UnknownParameter, Double> velMap = new HashMap<>();
		for (UnknownParameter m : answerMap.keySet()) {
			if (m.getPartialType().isTimePartial())
				continue;
			Location loc = (Location) m.getLocation();
			double r = loc.getR();
			double dR = layerMap.get(r);
			double rmin = r - dR / 2.;
			double rmax = r + dR / 2.;
			velMap.put(m, toVelocity(answerMap.get(m), r, rmin, rmax, structure, amplifyPerturbation));
		}
		return velMap;
	}
	
	private static Map<UnknownParameter, Double> toPerturbation(Map<UnknownParameter
			, Double> answerMap, Map<Double, Double> layerMap
			, List<UnknownParameter> parameterOrder, PolynomialStructure structure
			, double amplifyPerturbation) {
		Map<UnknownParameter, Double> perturbationMap = new HashMap<>();
		for (UnknownParameter m : parameterOrder) {
			if (m.getPartialType().isTimePartial())
				continue;
			Location loc = (Location) m.getLocation();
			double r = loc.getR();
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
//			double v1 = toVelocity(answerMap.get(m), r, rmin
//					, rmax, structure, amplifyPerturbation);
//			double v0 = structure.getVshAt(r);
//			double perturbation = (1. - v1 / v0) * 100;
			double perturbation = toPerturbation(answerMap.get(m)
					,rmin, rmax, structure) * 100.;
			perturbationMap.put(m, perturbation);
		}
		return perturbationMap;
	}
	
	private static Map<UnknownParameter, Double> toPerturbation(Map<UnknownParameter
			, Double> answerMap, Map<Double, Double> layerMap
			, List<UnknownParameter> parameterOrder, PolynomialStructure structure0, PolynomialStructure structure1
			, double amplifyPerturbation) {
		Map<UnknownParameter, Double> perturbationMap = new HashMap<>();
		for (UnknownParameter m : parameterOrder) {
			if (m.getPartialType().isTimePartial())
				continue;
			Location loc = (Location) m.getLocation();
			double r = loc.getR();
			double dR = 0;
			try {
				dR = layerMap.get(r);
			} catch (NullPointerException e) {
				System.out.println("NullPointerException: didn't find layer for radius " + r);
				e.printStackTrace();
			}
			double rmin = r - dR / 2.;
			double rmax = r + dR / 2.;
			if (structure0.equals(PolynomialStructure.AK135)) {
				if (r == 6343.8) {
					rmin = 6336.61;
					rmax = Earth.EARTH_RADIUS;
				}
			}
//			double v1 = toVelocity(answerMap.get(m), r, rmin
//					, rmax, structure, amplifyPerturbation);
//			double v0 = structure.getVshAt(r);
//			double perturbation = (1. - v1 / v0) * 100;
			double perturbation = toPerturbation(answerMap.get(m)
					,rmin, rmax, structure0, structure1) * 100.;
			perturbationMap.put(m, perturbation);
		}
		return perturbationMap;
	}
	
	private static double[][] toQ(Map<UnknownParameter, Double> answerMap, List<UnknownParameter> parameterOrder, PolynomialStructure structure
			, double amplifyPerturbation) {
		List<UnknownParameter> parameterForStructure = parameterOrder.stream()
				.filter(unknown -> unknown.getPartialType().equals(PartialType.PARQ))
				.collect(Collectors.toList());
		int n = parameterForStructure.size();
		double[][] velocities = new double[n][];
		for (int i = 0; i < n; i++) {
			velocities[i] = new double[3];
			UnknownParameter m = parameterForStructure.get(i);
			double rmin = 0;
			double rmax = 0;
			rmin = m.getLocation().getR() - m.getWeighting() / 2.;
			rmax = m.getLocation().getR() + m.getWeighting() / 2.;
			velocities[i][0] = toQ(answerMap.get(m), m.getLocation().getR(), rmin, rmax, structure, amplifyPerturbation);
			velocities[i][1] = rmin;
			velocities[i][2] = rmax;
		}
		return velocities;
	}
	
	private static Map<UnknownParameter, Double> toVelocity(Map<UnknownParameter, Double> answerMap, TriangleRadialSpline trs, PolynomialStructure structure) {
		Map<UnknownParameter, Double> velMap = new HashMap<>();
		int n = 200;
		double[][] velocities = new double[n][];
		for (int i = 0; i < n; i++) {
			velocities[i] = new double[2];
			double r = 3480 + (6371 - 3480) / (n - 1) * i;
			velocities[i][0] = r;
			double dMu = getTriangleSplineValue(r, PartialType.PAR2, trs, answerMap);
			velocities[i][1] = Math.sqrt((structure.computeMu(r) + dMu) / structure.getRhoAt(r));
		}
		
		return velMap;
	}
	
	private static double toVelocity(double deltaMu, double r, double rmin, double rmax, PolynomialStructure structure) {
		return getSimpsonVsh(rmin, rmax, structure, deltaMu);
	}
	
	private static double toVelocity(double deltaMu, double r, double rmin, double rmax, PolynomialStructure structure,
			double amplifyPerturbation) {
		return getSimpsonVsh(rmin, rmax, structure, deltaMu * amplifyPerturbation);
	}
	
	private static double toPerturbation(double deltaMu
			,double rmin, double rmax, PolynomialStructure structure) {
		return getSimpsonVsPerturbation(rmin, rmax, structure, deltaMu);
	}
	
	private static double toPerturbation(double deltaMu
			,double rmin, double rmax, PolynomialStructure structure0, PolynomialStructure structure1) {
		return getSimpsonVsPerturbation(rmin, rmax, structure0, structure1, deltaMu);
	}
	
	private static double toQ(double dq, double r, double rmin, double rmax, PolynomialStructure structure,
			double amplifyPerturbation) {
		return getSimpsonQ(rmin, rmax, structure, dq, amplifyPerturbation);
	}
	
	private static double[][] toQ(Map<UnknownParameter, Double> answerMap, TriangleRadialSpline trs, PolynomialStructure structure, double amplifyPerturbation) {
		int n = 200;
		double[][] Qs = new double[n][];
		for (int i = 0; i < n; i++) {
			Qs[i] = new double[2];
			double r = 3480 + (6371 - 3480) / (n - 1) * i;
			Qs[i][0] = r;
//			double dQ = getTriangleSplineValue(r, PartialType.PARQ, trs, answerMap);
//			Qs[i][1] = structure.getQmuAt(r) + dQ * amplifyPerturbation;
			double dq = getTriangleSplineValue(r, PartialType.PARQ, trs, answerMap) * amplifyPerturbation;
			double dQ = -1 * structure.getQmuAt(r) * structure.getQmuAt(r) * dq;
			Qs[i][1] = structure.getQmuAt(r) + dQ;
		}
		
		return Qs;
	}
	
	public static double getSimpsonRho (double r1, double r2, PolynomialStructure structure) {
		double res = 0;
		double dr = (r2 - r1) / 40.;
		double vol = r2 - r1;
		for (int i=0; i < 40; i++) {
			double a = r1 + i * dr;
			double b = r1 + (i + 1) * dr;
			res += (b - a) / 6 * (structure.getRhoAt(a) + 4 * structure.getRhoAt((a + b) / 2) + structure.getRhoAt(b));
		}
		
		return res / vol;
	}
	
	public static double getSimpsonMu (double r1, double r2, PolynomialStructure structure) {
		double res = 0;
		double dr = (r2 - r1) / 40;
		double vol = r2 - r1;
		for (int i=0; i < 40; i++) {
			double a = r1 + i * dr;
			double b = r1 + (i + 1) * dr;
			res += (b - a) / 6 * (structure.computeMu(a) + 4 * structure.computeMu((a + b) / 2) + structure.computeMu(b));
		}
		
		return res / vol;
	}
	
	public static double getSimpsonVsPerturbation(double r1, double r2
			, PolynomialStructure structure, double dMu) {
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
	
	public static double getSimpsonVsPerturbation(double r1, double r2
			, PolynomialStructure structure0, PolynomialStructure structure1, double dMu) {
		double res = 0;
		double dr = (r2 - r1) / 40.;
		double vol = r2 - r1;
		for (int i=0; i < 40; i++) {
			double a = r1 + i * dr;
			double b = r1 + (i + 1) * dr;
			double ab = (a + b) / 2.;
			double v_a = dvs(a, dMu, structure0, structure1);
			double v_ab = dvs(ab, dMu, structure0, structure1);
			double v_b = dvs(b, dMu, structure0, structure1);
			res += (b - a) / 6. * (v_a + 4 * v_ab + v_b);
		}
		
		return res / vol;
	}
	
	public static double dvs(double r, double dMu, PolynomialStructure structure) {
		double v0 = Math.sqrt(structure.computeMu(r) / structure.getRhoAt(r));
		double v1 = Math.sqrt((structure.computeMu(r) + dMu) / structure.getRhoAt(r));
		return (v1 - v0) / v0;
	}
	
	private static double dvs(double r, double dMu, PolynomialStructure structure0, PolynomialStructure structure1) {
		double v0 = Math.sqrt(structure1.computeMu(r) / structure1.getRhoAt(r));
		double v1 = Math.sqrt((structure0.computeMu(r) + dMu) / structure0.getRhoAt(r));
		return (v1 - v0) / v0;
	}
	
	public static double getSimpsonVsh(double r1, double r2
			, PolynomialStructure structure, double dMu) {
		double res = 0;
		double dr = (r2 - r1) / 40.;
		double vol = r2 - r1;
		for (int i=0; i < 40; i++) {
			double a = r1 + i * dr;
			double b = r1 + (i + 1) * dr;
			double ab = (a + b) / 2.;
			double v_a = Math.sqrt( (structure.computeMu(a) + dMu) / structure.getRhoAt(a) );
			double v_ab = Math.sqrt( (structure.computeMu(ab) + dMu) / structure.getRhoAt(ab) );
			double v_b = Math.sqrt( (structure.computeMu(b) + dMu) / structure.getRhoAt(b) );
			res += (b - a) / 6. * (v_a + 4 * v_ab + v_b);
		}
		
		return res / vol;
	}
	
	public static double getSimpsonQ (double r1, double r2, PolynomialStructure structure, double dq, double amplifyPerturbation) {
		double res = 0;
		double dr = (r2 - r1) / 40.;
		double vol = r2 - r1;
		for (int i=0; i < 40; i++) {
			double a = r1 + i * dr;
			double b = r1 + (i + 1) * dr;
			double ab = (a + b) / 2.;
			double Q_a = structure.getQmuAt(a) -1 * structure.getQmuAt(a) * structure.getQmuAt(a) * dq * amplifyPerturbation;
			double Q_ab = structure.getQmuAt(ab) -1 * structure.getQmuAt(ab) * structure.getQmuAt(ab) * dq * amplifyPerturbation;
			double Q_b = structure.getQmuAt(b) -1 * structure.getQmuAt(b) * structure.getQmuAt(b) * dq * amplifyPerturbation;
			res += (b - a) / 6. * (Q_a + 4 * Q_ab + Q_b);
		}
		
		return res / vol;
	}
	
	private static double getTriangleSplineValue(double r, PartialType type, TriangleRadialSpline trs, Map<UnknownParameter, Double> answerMap) {
		List<UnknownParameter> newParameters = trs.getNewParameters();
		
		double res = 0;
		for (UnknownParameter p : newParameters.stream()
				.filter(unknown -> !unknown.getPartialType().isTimePartial()
						&& unknown.getPartialType().equals(type)).collect(Collectors.toList())) {
			double rp = p.getLocation().getR();
			double w = p.getWeighting();
			double value = answerMap.get(p);
			if (rp - w/2. < r && rp + w/2. >= r) {
				if (!trs.isBoundaryParameter(p)) {
					double dr = Math.abs(rp - r);
					res += value * (1 - 2 * dr / w);
				}
				else if (trs.isDownBoundary(p)) {
					double dr = rp - r;
					res += value * 0.5 * (1 + 2 * dr / w);
				}
				else {
					double dr = rp - r;
					res += value * 0.5 * (1 - 2 * dr / w);
				}
			}
		}
		
		return res;
	}
	
	public static Map<Location, Double> zeroMeanMap(Map<Location, Double> perturbationMap, double[] perturbationRs) {
		Map<Location, Double> map = new HashMap<>();
		Set<Location> locations = perturbationMap.keySet();
		for (double r : perturbationRs) {
			Set<Location> tmpLocations = locations.stream().filter(loc -> loc.getR() == r)
				.collect(Collectors.toSet());
			double average = 0.;
			for (Location tmploc : tmpLocations)
				average += perturbationMap.get(tmploc);
			average /= tmpLocations.size();
			
			for (Location tmploc : tmpLocations) {
				double tmpPerturbation = perturbationMap.get(tmploc)
						- average;
				map.put(tmploc, tmpPerturbation);
			}
		}
		return map;
	}
	
	public static double[][] average1DProfile(Map<Location, Double> perturbationMap, double[] perturbationRs) {
		double[][] profile = new double[perturbationRs.length][2];
		Set<Location> locations = perturbationMap.keySet();
		for (int i = 0; i < perturbationRs.length; i++) {
			double r = perturbationRs[i];
			Set<Location> tmpLocations = locations.stream().filter(loc -> loc.getR() == r)
				.collect(Collectors.toSet());
			double average = 0.;
			for (Location tmploc : tmpLocations)
				average += perturbationMap.get(tmploc);
			average /= tmpLocations.size();
			
			profile[i][0] = r;
			profile[i][1] = average;
		}
		return profile;
	}
	
	public static Map<Location, Double> extendedPerturbationMap(Map<Location, Double> perturbationMap, double dL, double[] perturbationRs) {
		Map<Location, Double> extended = new HashMap<>();
		double minLat = 1e3;
		double maxLat = -1e3;
		double minLon = 1e3;
		double maxLon = -1e3;
		
		Set<Location> locations = perturbationMap.keySet();
		for (Location loci : locations) {
			double dvs = perturbationMap.get(loci);
			extended.put(loci, dvs);
			
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
						extended.put(additionalLocs[j], 0.);
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
						extended.put(loc, Double.NaN);
				}
			}
		}

		return extended;
	}
	
	public static void outputVelocity(String modelName
			, Path perturbationLayerFile, int maxNumVector, double deltaDegree
			, Path inversionResultPath) throws IOException {
		PolynomialStructure structure = null;
		
		switch (modelName) {
		case "ak135":
			structure = PolynomialStructure.AK135;
			break;
		case "AK135":
			structure = PolynomialStructure.AK135;
			break;
		case "prem":
			structure = PolynomialStructure.PREM;
			break;
		case "PREM":
			structure = PolynomialStructure.PREM;
			break;
		case "iso_prem":
			structure = PolynomialStructure.ISO_PREM;
			break;
		case "iprem":
			structure = PolynomialStructure.ISO_PREM;
			break;
		case "stw105":
			Path polynomialStructurePath = Paths.get("/mnt/melonpan/anpan/inversion/Dpp/POLY/stw105_smallCoeff.poly");
			if (!Files.isRegularFile(polynomialStructurePath) || !Files.isReadable(polynomialStructurePath))
				throw new RuntimeException("Error: no such file " + polynomialStructurePath.toString());
			structure = new PolynomialStructure(polynomialStructurePath);
			break;
		case "STW105":
			polynomialStructurePath = Paths.get("/mnt/melonpan/anpan/inversion/Dpp/POLY/stw105_smallCoeff.poly");
			if (!Files.isRegularFile(polynomialStructurePath) || !Files.isReadable(polynomialStructurePath))
				throw new RuntimeException("Error: no such file " + polynomialStructurePath.toString());
			structure = new PolynomialStructure(polynomialStructurePath);
			break;
		default:
			break;
		}
		
		// read layer thickness
		Map<Double, Double> layerMap = new HashMap<>();
		Files.readAllLines(perturbationLayerFile).stream().forEach(s -> {
			Double r = Double.parseDouble(s.trim().split(" ")[0]);
			Double d = Double.parseDouble(s.trim().split(" ")[1]);
			layerMap.put(r, d);
		});
		
		InversionResult ir = new InversionResult(inversionResultPath);
		
		int n = ir.getNumberOfUnknowns() < maxNumVector ? ir.getNumberOfUnknowns() : maxNumVector;
		
		List<UnknownParameter> unknowns = ir.getUnknownParameterList();
		
		for (InverseMethodEnum inverse : ir.getInverseMethods()) {
			for (int i = 1; i < n; i++) {
				Path outpath = inversionResultPath.resolve(inverse.simple() + "/" + "velocity" + inverse.simple() + i + ".txt");
				Map<UnknownParameter, Double> answerMap = ir.answerMapOf(inverse, i);
				Map<UnknownParameter, Double> zeroMap = new HashMap<>();
				answerMap.forEach((m, v) -> zeroMap.put(m, 0.));
				Map<UnknownParameter, Double> perturbations = null;
				Map<Location, Double> extendedPerturbationMap = null;
				Map<Location, Double> zeroMeanPerturbationMap = null;
				Map<Location, Double> extendedZeroMeanPerturbationMap = null;
				
				perturbations = toPerturbation(answerMap, layerMap, unknowns, structure, 1.);
				Set<Double> rs = layerMap.keySet();
				double[] perturbationRs = new double[rs.size()];
				int count = 0;
				for (double r : rs)
					perturbationRs[count++] = r;
				Map<Location, Double> perturbationMap = new HashMap<>();
				for (UnknownParameter unknown : perturbations.keySet())
					perturbationMap.put(unknown.getLocation(), perturbations.get(unknown));
				zeroMeanPerturbationMap = zeroMeanMap(perturbationMap, perturbationRs);
				extendedPerturbationMap = extendedPerturbationMap(perturbationMap, deltaDegree, perturbationRs);
				extendedZeroMeanPerturbationMap = extendedPerturbationMap(zeroMeanPerturbationMap, deltaDegree, perturbationRs);
				
				PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
				for (Location loc : extendedPerturbationMap.keySet()) {
					double perturbation = extendedPerturbationMap.get(loc);
					double zeroMeanPerturbation = extendedZeroMeanPerturbationMap.get(loc);
					pw.println(loc + " " + perturbation + " " + zeroMeanPerturbation);
				}
			}
		}
	}
}
