package montecarlo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import manhattan.template.HorizontalPosition;
import manhattan.template.Trace;

class Correction {
	private static void center() {
		try {
			Path txtPath = Paths
					.get("/home/kensuke/data/WesternPacific/anelasticity/montecarlo/selection/correction.inf");
			List<String> lines = Files.readAllLines(txtPath);
			double[] x = new double[lines.size()];
			double[] y = new double[lines.size()];

			x = lines.stream().map(line -> line.split("\\s+")).mapToDouble(parts -> Double.parseDouble(parts[3]))
					.toArray();
			y = lines.stream().map(line -> line.split("\\s+")).mapToDouble(parts -> Double.parseDouble(parts[2]))
					.toArray();
			Trace trace = new Trace(x, y);

			PolynomialFunction pf = trace.toPolynomial(1);
			for (int i = 155; i < 168; i++)
				System.out.println(i + " " + pf.value(i));
			System.out.println(pf);
		} catch (Exception e) {
		}
	}

	public static void main(String[] args) {
		Path corPath = Paths.get("/home/kensuke/data/WesternPacific/anelasticity/montecarlo/selection/correction.inf");

		try {
			List<String> out = new ArrayList<>();
			List<String> lines = Files.readAllLines(corPath);
			lines.forEach(line -> {
				String[] parts = line.split("\\s+");
				HorizontalPosition pos = new HorizontalPosition(Double.parseDouble(parts[2]),
						Double.parseDouble(parts[3]));
				int i = separate(pos);
				out.add(line + " " + i);
			});
			Files.write(corPath.resolve("/home/kensuke/data/WesternPacific/anelasticity/montecarlo/selection/cc"), out);
		} catch (Exception e) {
		}

	}

	private static int separate(HorizontalPosition pos) {
		double x = pos.getLongitude();
		double y = pos.getLatitude();
		double a = -1 / 0.700611627034478;
		if (y < a * x + 232)
			return 1;
		if (y < a * x + 238)
			return 2;
		if (y < a * x + 244)
			return 3;

		return 4;
	}
}
