package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.inversion.LetMeInvert;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class aic {

	public static void main(String[] args) {
		Path varianceFile = Paths.get(args[0]);
		double alpha = Double.parseDouble(args[1]);
		double minPeriod = 8.;
		double npts = 647997;
		List<Double> varianceList = new ArrayList<>();
		try {
			BufferedReader reader = Files.newBufferedReader(varianceFile);
			String line = reader.readLine();
			while (line != null) {
				double var = Double.parseDouble(line.split("\\s+")[0]);
				varianceList.add(var);
				line = reader.readLine();
			}
			
			double[] variance = varianceList.stream().mapToDouble(p -> p).toArray();
			
			double[] aic = LetMeInvert.computeAIC(variance, alpha, minPeriod, npts);
			for (int i = 0; i < aic.length; i++)
				System.out.println(aic[i]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
