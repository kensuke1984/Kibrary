package io.github.kensuke1984.kibrary.inversion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.Location;

public class MakeCheckerboard {

	public static void main(String[] args) {
		Path parameterPath = Paths.get(args[0]);
		Path horizontalSignFile = Paths.get(args[1]);
		int nR = Integer.parseInt(args[2]);
		double dv = 0.02;
		PolynomialStructure structure 
			= PolynomialStructure.AK135;
		Path outDvFile = Paths.get("dv.inf");
		
		try {
			BufferedReader reader = Files.newBufferedReader(horizontalSignFile);
			String line = null;
			List<Double> horizontalSigns = new ArrayList<>();
			while ((line = reader.readLine()) != null) {
				horizontalSigns.add(Double.parseDouble(line));
			}
			reader.close();
			
			
			List<UnknownParameter> unknowns 
				= UnknownParameterFile.read(parameterPath);
			BufferedWriter writer = Files.newBufferedWriter(outDvFile);
			for (int i = 0; i < unknowns.size(); i ++) {
				Location loc = (Location) unknowns.get(i)
						.getLocation();
				double dmu = dMU(dv, loc.getR(), structure);
				double tmp = (i % nR) % 2 == 0 ? 1 : -1;
				double sign = horizontalSigns.get(i / nR) * tmp;
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
}
