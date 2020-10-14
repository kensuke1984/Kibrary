package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.util.Earth;
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


public class VelocityField_RND {
	
	public static void main(String[] args) throws IOException {
		String polynomialStructureString = null;
		String polynomialStructureStringAbsolute = null;
		Path polynomialStructurePath = null;
		String partialCombination = null;
		int nSample = 1;
		double amplifyPerturbation = 1.;
		if (args.length == 1) {
			amplifyPerturbation = Double.parseDouble(args[0]);
		}
		try {
			polynomialStructureString = JOptionPane.showInputDialog("Polynomial structure?", polynomialStructureString);
		} catch (Exception e) {
			System.out.println("Polynomial structure?");
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new CloseShieldInputStream(System.in)))) {
				polynomialStructureString = br.readLine().trim();
				if (!polynomialStructureString.startsWith("/"))
					polynomialStructureStringAbsolute = System.getProperty("user.dir") + "/" + polynomialStructureString;
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
			System.out.println("nSample?");
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new CloseShieldInputStream(System.in)))) {
				nSample = Integer.parseInt(br.readLine().trim());
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new RuntimeException();
			}
		}
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
		default:
			break;
		}
		if (structure == null) {
			polynomialStructurePath = Paths.get(polynomialStructureStringAbsolute);
			if (!Files.isRegularFile(polynomialStructurePath) || !Files.isReadable(polynomialStructurePath))
				throw new RuntimeException("Error: no such file " + polynomialStructureStringAbsolute);
			structure = new PolynomialStructure(polynomialStructurePath);
		}
		
		for (int isample = 0; isample < nSample; isample++) {
			Path inversionResultPath = Paths.get(String.format("lmi_RND%04d", isample));
			InversionResult ir = new InversionResult(inversionResultPath, true);
			List<UnknownParameter> unknowns = ir.getUnknownParameterList();
			List<UnknownParameter> originalUnknowns = ir.getOriginalUnknownParameterList();
			TriangleRadialSpline trs = null;
			if (partialCombination.equals("trs")) {
				Map<PartialType, Integer[]> nNewParameter = trs.parseNParameters(unknowns);
				trs = new TriangleRadialSpline(nNewParameter, originalUnknowns);
			}
			Set<PartialType> partialTypes = unknowns.stream().map(UnknownParameter::getPartialType).collect(Collectors.toSet());
			if (partialTypes.contains(PartialType.PAR2) || partialTypes.contains(PartialType.PARQ)) {
				for (InverseMethodEnum inverse : ir.getInverseMethods()) {
					Path outpath = inversionResultPath.resolve(inverse.simple() + "/" + "velocityInitialModel" + ".txt");
					try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
						pw.println("# perturbationR Vsh");
						for (int j = 0; j <= 1000; j++) {
							double r = 3480. + (Earth.EARTH_RADIUS - 3480.) / 1000. * j;
							pw.println(r + " " + structure.getVshAt(r));
						}
					}
					int n = unknowns.size();
					for (int i = 1; i <= n; i++) {
						outpath = inversionResultPath.resolve(inverse.simple() + "/" + "velocity" + inverse.simple() + i + ".txt");
						Path outpathIteration = inversionResultPath.resolve(inverse.simple() + "/" + "velocity" + inverse.simple() + i + "_iteration.txt");
						Path outpathQ = inversionResultPath.resolve(inverse.simple() + "/" + "Q" + inverse.simple() + i + ".txt");
						Map<UnknownParameter, Double> answerMap = ir.answerMapOf(inverse, i);
						Map<UnknownParameter, Double> zeroMap = new HashMap<>();
						answerMap.forEach((m, v) -> zeroMap.put(m, 0.));
						double[][] velocities = null;
						double[][] zeroVelocities = null;
						double[][] Qs = null;
						double[][] zeroQs = null;
						if (trs == null) {
							velocities = toVelocity(answerMap, unknowns, structure, 1.);
							zeroVelocities = toVelocity(zeroMap, unknowns, structure, 1.);
							if (partialTypes.contains(PartialType.PARQ)) {
								Qs = toQ(answerMap, unknowns, structure, amplifyPerturbation);
								zeroQs = toQ(zeroMap, unknowns, structure, amplifyPerturbation);
							}
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
							PrintWriter pwQ = null;
							PrintWriter pwIteration = new PrintWriter(Files.newBufferedWriter(outpathIteration, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
							if (partialTypes.contains(PartialType.PARQ)) {
								pwQ = new PrintWriter(Files.newBufferedWriter(outpathQ, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
								pwQ.println("#perturbationR final_Q intial_Q");
							}
							pw.println("#perturbationR final_Vsh initial_Vsh");
							if (trs == null) {
								for (int j = 0; j < velocities.length; j++) {
									pw.println(velocities[j][2] +  " " + velocities[j][0] + " " + zeroVelocities[j][0]);
									pw.println(velocities[j][1] +  " " + velocities[j][0] + " " + zeroVelocities[j][0]);
									pwIteration.println((6371. - velocities[j][1] - 10.) + " " + structure.getVphAt(velocities[j][1] + 10.) 
											+ " " + velocities[j][0] + " " + structure.getRhoAt(velocities[j][1] + 10.));
								}
								if (partialTypes.contains(PartialType.PARQ)) {
									for (int j = 0; j < Qs.length; j++) {
											pwQ.println(Qs[j][1] +  " " + Qs[j][0] + " " + zeroQs[j][0]);
											pwQ.println(Qs[j][2] +  " " + Qs[j][0] + " " + zeroQs[j][0]);
									}
								}
							}
							else {
								for (int j = 0; j < velocities.length; j++)
									pw.println(velocities[j][0] + " " + velocities[j][1] + " " + zeroVelocities[j][1]);
								if (partialTypes.contains(PartialType.PARQ)) {
									for (int j = 0; j < Qs.length; j++)
										pwQ.println(Qs[j][0] + " " + Qs[j][1] + " " + zeroQs[j][1]);
								}
							}
							
							pw.close();
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
	}
	
	private static double[] toVelocity(Map<UnknownParameter, Double> answerMap, List<UnknownParameter> parameterOrder, PolynomialStructure structure) {
		double[] velocities = new double[answerMap.size()];
		int n = parameterOrder.size();
		for (int i = 0; i < n; i++) {
			UnknownParameter m = parameterOrder.get(i);
			double rmin = 0;
			double rmax = 0;
			if (i > 0 && i < n - 1) {
				rmin = (m.getLocation().getR() - parameterOrder.get(i-1).getLocation().getR()) / 2. + parameterOrder.get(i-1).getLocation().getR();
				rmax = (parameterOrder.get(i+1).getLocation().getR() - m.getLocation().getR()) / 2. + m.getLocation().getR();
			}
			else if (i > 0) {
				rmin = (m.getLocation().getR() - parameterOrder.get(i-1).getLocation().getR()) / 2. + parameterOrder.get(i-1).getLocation().getR();
				rmax = m.getLocation().getR() + m.getWeighting() - (m.getLocation().getR() - parameterOrder.get(i-1).getLocation().getR()) / 2.;
			}
			else {
				rmin = m.getLocation().getR() - m.getWeighting() + (parameterOrder.get(i+1).getLocation().getR() - m.getLocation().getR()) / 2.;
				rmax = (parameterOrder.get(i+1).getLocation().getR() - m.getLocation().getR()) / 2. + m.getLocation().getR();
			}
			velocities[i] = toVelocity(answerMap.get(m), m.getLocation().getR(), rmin, rmax, structure);
		}
		return velocities;
	}
	
	private static double[][] toVelocity(Map<UnknownParameter, Double> answerMap, List<UnknownParameter> parameterOrder, PolynomialStructure structure
			, double amplifyPerturbation) {
		List<UnknownParameter> parameterForStructure = parameterOrder.stream()
				.filter(unknown -> unknown.getPartialType().equals(PartialType.PAR2))
				.collect(Collectors.toList());
		int n = parameterForStructure.size();
		double[][] velocities = new double[n][];
		for (int i = 0; i < n; i++) {
			velocities[i] = new double[3];
			UnknownParameter m = parameterForStructure.get(i);
			double rmin = 0;
			double rmax = 0;
//			if (i > 0 && i < n - 1) {
//				rmin = ((Double) m.getLocation() - (Double) parameterOrder.get(i-1).getLocation()) / 2. + (Double) parameterOrder.get(i-1).getLocation();
//				rmax = ((Double) parameterOrder.get(i+1).getLocation() - (Double) m.getLocation()) / 2. + (Double) m.getLocation();
//			}
//			else if (i > 0) {
//				rmin = ((Double) m.getLocation() - (Double) parameterOrder.get(i-1).getLocation()) / 2. + (Double) parameterOrder.get(i-1).getLocation();
//				rmax = (Double) m.getLocation() + m.getWeighting() - ((Double) m.getLocation() - (Double) parameterOrder.get(i-1).getLocation()) / 2.;
//			}
//			else if (i == 0) {
//				rmin = (Double) m.getLocation() - m.getWeighting() + ((Double) parameterOrder.get(i+1).getLocation() - (Double) m.getLocation()) / 2.;
//				rmax = ((Double) parameterOrder.get(i+1).getLocation() - (Double) m.getLocation()) / 2. + (Double) m.getLocation();
//			}
			rmin = m.getLocation().getR() - m.getWeighting() / 2.;
			rmax = m.getLocation().getR() + m.getWeighting() / 2.;
			velocities[i][0] = toVelocity(answerMap.get(m), m.getLocation().getR(), rmin, rmax, structure, amplifyPerturbation);
			velocities[i][1] = rmin;
			velocities[i][2] = rmax;
		}
		return velocities;
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
	
	private static double[][] toVelocity(Map<UnknownParameter, Double> answerMap, TriangleRadialSpline trs, PolynomialStructure structure) {
		int n = 200;
		double[][] velocities = new double[n][];
		for (int i = 0; i < n; i++) {
			velocities[i] = new double[2];
			double r = 3480 + (6371 - 3480) / (n - 1) * i;
			velocities[i][0] = r;
			double dMu = getTriangleSplineValue(r, PartialType.PAR2, trs, answerMap);
			velocities[i][1] = Math.sqrt((structure.computeMu(r) + dMu) / structure.getRhoAt(r));
		}
		
		return velocities;
	}
	
	private static double toVelocity(double deltaMu, double r, double rmin, double rmax, PolynomialStructure structure) {
		return getSimpsonVsh(rmin, rmax, structure, deltaMu);
	}
	
	private static double toVelocity(double deltaMu, double r, double rmin, double rmax, PolynomialStructure structure,
			double amplifyPerturbation) {
		return getSimpsonVsh(rmin, rmax, structure, deltaMu * amplifyPerturbation);
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
	
	public static double getSimpsonVsh (double r1, double r2, PolynomialStructure structure, double dMu) {
		double res = 0;
		double dr = (r2 - r1) / 40.;
		double vol = r2 - r1;
		for (int i=0; i < 40; i++) {
			double a = r1 + i * dr;
			double b = r1 + (i + 1) * dr;
			double ab = (a + b) / 2.;
			double v_a = Math.sqrt( (structure.computeN(a) + dMu) / structure.getRhoAt(a) );
			double v_ab = Math.sqrt( (structure.computeN(ab) + dMu) / structure.getRhoAt(ab) );
			double v_b = Math.sqrt( (structure.computeN(b) + dMu) / structure.getRhoAt(b) );
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
}
