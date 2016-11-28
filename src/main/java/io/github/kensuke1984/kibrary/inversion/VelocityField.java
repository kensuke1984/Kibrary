package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.apache.commons.io.input.CloseShieldInputStream;


public class VelocityField {
	
	public static void main(String[] args) throws IOException {
		String inversionResultString = null;
		String polynomialStructureString = null;
		Path inversionResultPath = null;
		Path polynomialStructurePath = null;
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
				if (!polynomialStructureString.startsWith("/"))
					polynomialStructureString = System.getProperty("user.dir") + "/" + polynomialStructureString;
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new RuntimeException();
			}
		}
		if (inversionResultString == null || inversionResultString.isEmpty())
			return;
		inversionResultPath = Paths.get(inversionResultString);
		if (!Files.isDirectory(inversionResultPath))
			return;
		if (polynomialStructureString == null || polynomialStructureString.isEmpty())
			return;
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
		default:
			break;
		}
		if (structure == null) {
			polynomialStructurePath = Paths.get(polynomialStructureString);
			if (!Files.isRegularFile(polynomialStructurePath) || !Files.isReadable(polynomialStructurePath))
				return;
			structure = new PolynomialStructure(polynomialStructurePath);
		}
		
		Set<InverseMethodEnum> inverseMethods = Arrays.asList(InverseMethodEnum.CONJUGATE_GRADIENT).stream().collect(Collectors.toSet());
		InversionResult ir = new InversionResult(inversionResultPath, inverseMethods);
		List<UnknownParameter> unknowns = ir.getUnknownParameterList();
		Set<PartialType> partialTypes = unknowns.stream().map(UnknownParameter::getPartialType).collect(Collectors.toSet());
		if (partialTypes.contains(PartialType.PAR2)) {
			for (InverseMethodEnum inverse : inverseMethods) {
				Path outpath = inversionResultPath.resolve(inverse.simple() + "/" + "velocityInitialModel" + ".txt");
				try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
					pw.println("# perturbationR Vsh");
					for (int j = 0; j <= 1000; j++) {
						double r = 3480. + (Earth.EARTH_RADIUS - 3480.) / 1000. * j;
						pw.println(r + " " + structure.getVshAt(r));
					}
				}
				int n = 10 > unknowns.size() ? unknowns.size() : 10;
				for (int i = 1; i < n; i++) {
					outpath = inversionResultPath.resolve(inverse.simple() + "/" + "velocity" + inverse.simple() + i + ".txt");
					Map<UnknownParameter, Double> answerMap = ir.answerMapOf(inverse, i);
					double[] velocities = toVelocity(answerMap, unknowns, structure);
					try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
						pw.println("# perturbationR Vsh");
						for (int j = 0; j < velocities.length; j++)
							pw.println(unknowns.get(j).getLocation() +  " " + velocities[j]);
					}
				}
			}
		}
	}
	
	private static double[] toVelocity(Map<UnknownParameter, Double> answerMap, List<UnknownParameter> parameterOrder, PolynomialStructure structure) {
		double[] velocities = new double[answerMap.size()];
		AtomicInteger iatom = new AtomicInteger();
		parameterOrder.forEach(m -> {
			velocities[iatom.getAndIncrement()] = toVelocity(answerMap.get(m), (Double) m.getLocation(), 20., structure);
		});
		return velocities;
	}
	
	private static double toVelocity(double deltaMu, double r, double dr, PolynomialStructure structure) {
		double r1 = r - dr / 2.;
		double r2 = r + dr / 2.;
		return Math.sqrt((getSimpsonMu(r1, r2, structure) + deltaMu) / getSimpsonRho(r1, r2, structure));
	}
	
	public static double getSimpsonRho (double r1, double r2, PolynomialStructure structure) {
		double res = 0;
		double dr = (r2 - r1) / 40;
		for (int i=0; i < 40; i++) {
			double a = r1 + i * dr;
			double b = r1 + (i + 1) * dr;
			res += (b - a) / 6 * (structure.getRhoAt(a) + 4 * structure.getRhoAt((a + b) / 2) + structure.getRhoAt(b));
		}
		return res;
	}
	
	public static double getSimpsonMu (double r1, double r2, PolynomialStructure structure) {
		double res = 0;
		double dr = (r2 - r1) / 40;
		for (int i=0; i < 40; i++) {
			double a = r1 + i * dr;
			double b = r1 + (i + 1) * dr;
			res += (b - a) / 6 * (structure.computeMu(a) + 4 * structure.computeMu((a + b) / 2) + structure.computeMu(b));
		}
		return res;
	}
}
