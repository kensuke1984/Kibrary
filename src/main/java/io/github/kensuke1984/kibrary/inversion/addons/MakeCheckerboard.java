package io.github.kensuke1984.kibrary.inversion.addons;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;

public class MakeCheckerboard {

	public static void main(String[] args) throws IOException {
//		System.out.println("dV = " + dV(1., 3505., PolynomialStructure.PREM));
		
//		PolynomialStructure premdpp = new PolynomialStructure(Paths.get("/Users/Anselme/Dropbox/Kenji/FWICarib_PS/SYNTHETIC_TEST/ONE_REC/16LAYERS/lmi_2005_rec_kappa/stack/TRUE_MODEL/prem_dpp.poly"));
		PolynomialStructure prem = PolynomialStructure.PREM;
		
		
//		System.out.println(dMU(0.020487955855501883, 3505, PolynomialStructure.PREM));
//		System.out.println(dMU(0.02050610666137339, 3555, PolynomialStructure.PREM));
//		System.out.println(dMU(0.020524058158315792, 3605, PolynomialStructure.PREM));
//		System.out.println(( -prem.getVphAt(3630) + premdpp.getVphAt(3630)) / prem.getVphAt(3630));
//		System.out.println(( -prem.getVphAt(3605) + premdpp.getVphAt(3605)) / prem.getVphAt(3605));
//		System.out.println(( -prem.getVphAt(3555) + premdpp.getVphAt(3555)) / prem.getVphAt(3555));
//		System.out.println(( -prem.getVphAt(3505) + premdpp.getVphAt(3505)) / prem.getVphAt(3505));
//		System.out.println(prem.getVphAt(3530)*1.02);
//		System.out.println(prem.getVshAt(3530)*1.035);
//		System.out.println(prem.getVshAt(3530)*1.025);
//		System.out.println(prem.getVshAt(3530)*1.015);
//		System.out.println(prem.getVshAt(3530)*1.02);
//		double g=prem.getVshAt(3530)/prem.getVphAt(3530);
//		double gamma = 4./3. * g * g;
//		System.out.println(gamma + " " + 1./gamma);
//		System.exit(0);
		
		Path parameterPath = Paths.get(args[0]);
		Path horizontalSignFile = Paths.get(args[1]);
		Path verticalSignFile = Paths.get(args[2]);
//		int nR = Integer.parseInt(args[2]);
		double dv = 0.01;
		PolynomialStructure structure 
			= PolynomialStructure.PREM;
		Path outDvFile = Paths.get("dv" + Utilities.getTemporaryString() + ".inf");
		
		try {
			BufferedReader reader = Files.newBufferedReader(horizontalSignFile);
			String line = null;
			List<Double> horizontalSigns = new ArrayList<>();
			while ((line = reader.readLine()) != null) {
				horizontalSigns.add(Double.parseDouble(line));
			}
			reader.close();
			int nH = horizontalSigns.size();
			
			reader = Files.newBufferedReader(verticalSignFile);
			line = null;
			int nR = 0;
			Map<Double, Double> verticalSignMap = new HashMap<>();
			while ((line = reader.readLine()) != null) {
				String[] s = line.split("\\s+");
				verticalSignMap.put(Double.parseDouble(s[0]),
						Double.parseDouble(s[1]));
				nR++;
			}
			reader.close();
			
			List<UnknownParameter> unknowns 
				= UnknownParameterFile.read(parameterPath);
			BufferedWriter writer = Files.newBufferedWriter(outDvFile);
			for (int i = 0; i < unknowns.size(); i ++) {
				Location loc = (Location) unknowns.get(i)
						.getLocation();
				double dmu = dMU(dv, loc.getR(), structure);
//				double dKappa = dKappa(dv, loc.getR(), structure);
//				double dLambda2mu = dLambda2mu(dv, loc.getR(), structure);
//				double dLambda = dLambda(dv, loc.getR(), structure);
				
				// order 1
//				double tmp = (i % nR) % 2 == 0 ? 1 : -1;
//				double sign = horizontalSigns.get(i / nR) * tmp;
//				int iH = i / nR;
				
				// order 2
				int iH = i % nH;
				double sign = horizontalSigns.get(iH) * 
						verticalSignMap.get(loc.getR());
				
				System.out.format("%.4f%n", sign * dmu);
//				System.out.format("%.4f%n", sign * dKappa);
//				System.out.format("%.4f%n", sign * dLambda2mu);
//				System.out.format("%.4f%n", sign * dLambda);
				writer.write(String.format("%s %.4f%n", loc.toString(), sign * dv));
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static double dMU(double dv, double r
			, PolynomialStructure structure) {
		double v = structure.getVshAt(r);
		return structure.getRhoAt(r) * v * v 
				* (1 + dv) * (1 + dv) 
				- structure.computeMu(r);
	}
	
	private static double dLambda2mu(double dvp, double r, PolynomialStructure structure) {
		double v = structure.getVphAt(r);
		return structure.getRhoAt(r) * v * v 
				* (1 + dvp) * (1 + dvp) 
				- (structure.computeLambda(r) + 2 * structure.computeMu(r));
	}
	
	private static double dKappa(double dv, double r
			, PolynomialStructure structure) {
		double v = structure.getVbAt(r);
		return structure.getRhoAt(r) * v * v 
				* (1 + dv) * (1 + dv) 
				- structure.computeKappa(r);
	}
	
	private static double dLambda(double dv, double r
			, PolynomialStructure structure) {
		double v = structure.getVphAt(r);
		return structure.getRhoAt(r) * v * v 
				* ( (1 + dv) * (1 + dv) - 1 );
	}
	
	private static double dV(double dMU, double r, PolynomialStructure structure) {
		return Math.sqrt((structure.computeMu(r) + dMU) / structure.getRhoAt(r)) / structure.getVshAt(r) - 1;
	}
}
