package io.github.kensuke1984.kibrary.inversion;

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
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;

public class MakeCheckerboard {

	public static void main(String[] args) {
//		System.out.println("dV = " + dV(1., 3505., PolynomialStructure.PREM));
//		System.exit(0);
		
		Path parameterPath = Paths.get(args[0]);
		Path horizontalSignFile = Paths.get(args[1]);
		Path verticalSignFile = Paths.get(args[2]);
//		int nR = Integer.parseInt(args[2]);
		double dv = 0.02;
		PolynomialStructure structure 
			= PolynomialStructure.AK135;
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
				// order 1
				double tmp = (i % nR) % 2 == 0 ? 1 : -1;
				double sign = horizontalSigns.get(i / nR) * tmp;
//				int iH = i / nR;
				// order 2
//				int iH = i % nH;
//				double sign = horizontalSigns.get(iH) * 
//						verticalSignMap.get(loc.getR());
				System.out.format("%.4f%n", sign * dmu);
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
	
	private static double dV(double dMU, double r, PolynomialStructure structure) {
		return Math.sqrt((structure.computeMu(r) + dMU) / structure.getRhoAt(r)) / structure.getVshAt(r) - 1;
	}
}
