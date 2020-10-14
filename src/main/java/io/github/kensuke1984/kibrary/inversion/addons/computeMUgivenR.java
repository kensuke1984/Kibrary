package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class computeMUgivenR {

	public static void main(String[] args) throws IOException {
		Path rootpath = Paths.get(".");
		InversionResult ir = new InversionResult(rootpath);
		
		PolynomialStructure structure = PolynomialStructure.PREM;
		System.out.println("Using structure " + structure);
		
		Map<Double, Double> rKRDH16 = new HashMap<>();
		rKRDH16.put(2541., 2.6);
		rKRDH16.put(2641., 2.6);
		rKRDH16.put(2741., 2.1);
		rKRDH16.put(2841., 1.4);
		
		Map<Double, Double> rMLBD00 = new HashMap<>();
		rMLBD00.put(2541., 2.1);
		rMLBD00.put(2641., 2.3);
		rMLBD00.put(2741., 2.4);
		rMLBD00.put(2841., 2.6);
		
		Map<Double, Double> rKK01ah = new HashMap<>();
		rKK01ah.put(2541., 2.1);
		rKK01ah.put(2641., 2.1);
		rKK01ah.put(2741., 2.2);
		rKK01ah.put(2841., 2.3);
		
		Map<Double, Double> rKK01ahae = new HashMap<>();
		rKK01ahae.put(2541., 2.5);
		rKK01ahae.put(2641., 2.5);
		rKK01ahae.put(2741., 2.6);
		rKK01ahae.put(2841., 2.7);
		
		Map<UnknownParameter, Double> perturbationMap = ir.answerMapOf(InverseMethodEnum.CONJUGATE_GRADIENT, 4);
		
		Path outpath1 = Paths.get("CG/CG4_KRDH16.txt");
		Path outpath2 = Paths.get("CG/CG4_MLBD00.txt");
		Path outpath3 = Paths.get("CG/CG4_KK01ah.txt");
		Path outpath4 = Paths.get("CG/CG4_KK01ahae.txt");
		
		PrintWriter writer1 = new PrintWriter(outpath1.toFile());
		PrintWriter writer2 = new PrintWriter(outpath2.toFile());
		PrintWriter writer3 = new PrintWriter(outpath3.toFile());
		PrintWriter writer4 = new PrintWriter(outpath4.toFile());
		
		for (UnknownParameter u : ir.getUnknownParameterList()) {
			if (u.getPartialType().equals(PartialType.MU)) {
				System.out.println(u);
				double m = perturbationMap.get(u);
				writer1.println(m);
				writer2.println(m);
				writer3.println(m);
				writer4.println(m);
			}
		}
		
		for (UnknownParameter u : ir.getUnknownParameterList()) {
			if (u.getPartialType().equals(PartialType.MU)) {
				System.out.println(u);
				double r = u.getLocation().getR();
				double depth = 6371. - r;
				double Rsp1 = rKRDH16.get(depth);
				double Rsp2 = rMLBD00.get(depth);
				double Rsp3 = rKK01ah.get(depth);
				double Rsp4 = rKK01ahae.get(depth);
				double vp = structure.getVphAt(r);
				double vs = structure.getVshAt(r);
				
				double m1 = 1./Rsp1*vp*vp/vs/vs*perturbationMap.get(u);
				double m2 = 1./Rsp2*vp*vp/vs/vs*perturbationMap.get(u);
				double m3 = 1./Rsp3*vp*vp/vs/vs*perturbationMap.get(u);
				double m4 = 1./Rsp4*vp*vp/vs/vs*perturbationMap.get(u);
				
				writer1.println(m1);
				writer2.println(m2);
				writer3.println(m3);
				writer4.println(m4);
			}
		}
		
		writer1.close();
		writer2.close();
		writer3.close();
		writer4.close();
	}

}
