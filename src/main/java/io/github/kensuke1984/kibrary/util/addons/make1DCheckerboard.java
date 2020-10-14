package io.github.kensuke1984.kibrary.util.addons;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;


public class make1DCheckerboard {
	
	public static void main(String[] args) throws IOException {
		Path initialStructurePath = Paths.get("/mnt/melonpan/anpan/inversion/Dpp/POLY/checkerboard/stw105_005Hzcb_6-1-2_0.00per.poly");
		PolynomialStructure structure = new PolynomialStructure(initialStructurePath);
		PolynomialStructure structure0 = new PolynomialStructure(initialStructurePath);
		int ilm = Integer.parseInt(args[0]);
		int itz = Integer.parseInt(args[1]);
		int ium = Integer.parseInt(args[2]);
		
		double dVper = Double.parseDouble(args[3]);
		double dq = 0.;//6e-4;
		
		double ttz = 5961.;
		double btz = 5721.;
		double bc = 6346.6;
		
		double dlm = (btz - 3480.) / ilm;
		double dtz = (ttz - btz) / itz;
		double dum = (bc - ttz) / ium;
		
		double imem = 0;
		// ---------------------------- LM ----------------
		// ADD BOUNDARIES
		for (int i = 0; i < ilm; i++) {
			double r = 3480. + i * dlm;
			structure = structure.addBoundaries(r);
		}
		// set vsh perturbations
		for (int i = 0; i < ilm; i++) {
			double r0 = i * dlm + 3480.;
			double r1 = r0 + dlm;
			
			List<Integer> izones = new ArrayList<>();
			int izone = -1;
			int oldizone = -1;
			for (int j = 0; j < 20; j++) {
				oldizone = izone;
				double r = r0 + dlm / 20. * j;
				izone = structure.zoneOf(r);
				if (oldizone != izone)
					izones.add(izone);
			}
			
			double vshAtR1 = structure.getVshAt(r1);
			double dVAtR1 = dVper * vshAtR1 * Math.pow(-1, imem % 2);
			if (izones.size() == 1) {
				PolynomialFunction newp = structure.getVshOf(izones.get(0));
				newp = newp.add(new PolynomialFunction(new double[] {dVAtR1}));
				structure = structure.setVsh(izones.get(0), newp);
			}
			else {
				for (int n = izones.size() - 1; n >= 0; n--) {
					izone = izones.get(n);
					PolynomialFunction newp = structure.getVshOf(izone);
					newp = newp.add(new PolynomialFunction(new double[] {dVAtR1}));
					structure = structure.setVsh(izone, newp);
					if (n > 0)
						dVAtR1 = (structure.getVshAt(structure.getRMinOf(izone) + 1e-3) - structure.getVshAt(structure.getRMaxOf(izone - 1) - 1e-3));
				}
			}
			
			// q
			double QAtR1 = structure.getQmuAt(r1 - 0.1);
			double dQAtR1 = -QAtR1 * QAtR1 * dq * Math.pow(-1, imem % 2);
			if (izones.size() == 1) {
				structure = structure.setQMu(izone, QAtR1 + dQAtR1);
			}
			else {
				for (int n = izones.size() - 1; n >= 0; n--) {
					izone = izones.get(n);
					structure = structure.setQMu(izone, QAtR1 + dQAtR1);
					if (n > 0) {
						dQAtR1 = (structure.getQmuAt(structure.getRMinOf(izone) + 1e-3) - structure.getQmuAt(structure.getRMaxOf(izone - 1) - 1e-3));
					}
				}
			}
			
			imem++;
		}
		
		// ---------------------------- TZ ----------------
		// ADD BOUNDARIES
		for (int i = 0; i < itz; i++) {
			double r = btz + i * dtz;
			structure = structure.addBoundaries(r);
		}
		// set vsh perturbations
		for (int i = 0; i < itz; i++) {
			double r0 = i * dtz + btz;
			double r1 = r0 + dtz;
			
			List<Integer> izones = new ArrayList<>();
			int izone = -1;
			int oldizone = -1;
			for (int j = 0; j < 20; j++) {
				oldizone = izone;
				double r = r0 + dtz / 20. * j;
				izone = structure.zoneOf(r);
				if (oldizone != izone)
					izones.add(izone);
			}
			
			double vshAtR1 = structure.getVshAt(r1);
			double dVAtR1 = dVper * vshAtR1 * Math.pow(-1, imem % 2);
			if (izones.size() == 1) {
				PolynomialFunction newp = structure.getVshOf(izones.get(0));
				newp = newp.add(new PolynomialFunction(new double[] {dVAtR1}));
				structure = structure.setVsh(izones.get(0), newp);
			}
			else {
				for (int n = izones.size() - 1; n >= 0; n--) {
					izone = izones.get(n);
					PolynomialFunction newp = structure.getVshOf(izone);
					newp = newp.add(new PolynomialFunction(new double[] {dVAtR1}));
					structure = structure.setVsh(izone, newp);
					if (n > 0)
						dVAtR1 = (structure.getVshAt(structure.getRMinOf(izone) + 1e-3) - structure.getVshAt(structure.getRMaxOf(izone - 1) - 1e-3));
				}
				
			}
			
			// q
			double QAtR1 = structure.getQmuAt(r1 - 0.1);
			double dQAtR1 = -QAtR1 * QAtR1 * dq * Math.pow(-1, imem % 2);
			if (izones.size() == 1) {
				structure = structure.setQMu(izone, QAtR1 + dQAtR1);
			}
			else {
				for (int n = izones.size() - 1; n >= 0; n--) {
					izone = izones.get(n);
					structure = structure.setQMu(izone, QAtR1 + dQAtR1);
					if (n > 0)
						dQAtR1 = (structure.getQmuAt(structure.getRMinOf(izone) + 1e-3) - structure.getQmuAt(structure.getRMaxOf(izone - 1) - 1e-3));
				}
			}
			
			imem++;
		}
		
		// ---------------------------- UM ----------------
		// ADD BOUNDARIES
		for (int i = 0; i < ium; i++) {
			double r = ttz + i * dum;
			structure = structure.addBoundaries(r);
		}
		// set vsh perturbations
		for (int i = 0; i < ium; i++) {
			double r0 = i * dum + ttz;
			double r1 = r0 + dum;
			
			List<Integer> izones = new ArrayList<>();
			int izone = -1;
			int oldizone = -1;
			for (int j = 0; j < 20; j++) {
				oldizone = izone;
				double r = r0 + dum / 20. * j;
				izone = structure.zoneOf(r);
				if (oldizone != izone)
					izones.add(izone);
			}
			
			double vshAtR1 = structure.getVshAt(r1);
			double dVAtR1 = dVper * vshAtR1 * Math.pow(-1, imem % 2);
			if (izones.size() == 1) {
				PolynomialFunction newp = structure.getVshOf(izones.get(0));
				newp = newp.add(new PolynomialFunction(new double[] {dVAtR1}));
				structure = structure.setVsh(izones.get(0), newp);
			}
			else {
				for (int n = izones.size() - 1; n >= 0; n--) {
					izone = izones.get(n);
					PolynomialFunction newp = structure.getVshOf(izone);
					newp = newp.add(new PolynomialFunction(new double[] {dVAtR1}));
					structure = structure.setVsh(izone, newp);
					if (n > 0)
						dVAtR1 = (structure.getVshAt(structure.getRMinOf(izone) + 1e-3) - structure.getVshAt(structure.getRMaxOf(izone - 1) - 1e-3));
				}
				
			}
			
			// q
			double QAtR1 = structure.getQmuAt(r1 - 0.1);
			double dQAtR1 = -QAtR1 * QAtR1 * dq * Math.pow(-1, imem % 2);
			if (izones.size() == 1) {
				structure = structure.setQMu(izone, QAtR1 + dQAtR1);
			}
			else {
				for (int n = izones.size() - 1; n >= 0; n--) {
					izone = izones.get(n);
					structure = structure.setQMu(izone, QAtR1 + dQAtR1);
					System.out.println(structure.getRMinOf(izone) + " " + (QAtR1 + dQAtR1));
					if (n > 0) {
						dQAtR1 = (structure.getQmuAt(structure.getRMinOf(izone) + 1e-3) - structure.getQmuAt(structure.getRMaxOf(izone - 1) - 1e-3));
						System.out.println(structure.getRMinOf(izone) + " " +  structure.getRMaxOf(izone - 1) + " " + dQAtR1 + " " + structure.getQmuAt(structure.getRMinOf(izone) + 1e-3));
					}
				}
			}
			
			imem++;
		}
		
		// output checkerboard structure
		String name = initialStructurePath.getFileName().toString().replace(".poly", "cb_" + ilm + "-" + itz + "-" + ium + String.format("_%.2fper.poly", dVper * 100));
		Path outpath = initialStructurePath.getParent().resolve(name);
		structure.writePSV(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		
		name = name.replace(".poly", ".txt");
		outpath = initialStructurePath.getParent().resolve(name);
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			for (int i = 0; i < 5000; i++) {
				double r = i * (6371. - 3480.) / 4999. + 3480.;
				double vsh0 = structure0.getVshAt(r);
				double vsh = structure.getVshAt(r);
				double Q0 = structure0.getQmuAt(r);
				double Q = structure.getQmuAt(r);
				pw.println(r + " " + (6371. - r) + " " + vsh0 + " " + vsh + " " + Q0 + " " + Q);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
