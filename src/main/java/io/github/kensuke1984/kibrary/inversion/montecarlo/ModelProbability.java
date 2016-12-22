package io.github.kensuke1984.kibrary.inversion.montecarlo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;

/**
 * @author Kensuke Konishi
 * @version 0.0.1.1
 */
class ModelProbability {

	private Path workPath;
	private Path[] runPaths;

	public ModelProbability(Path workPath) {
		this.workPath = workPath;
		gatherRuns();

	}

	private void gatherRuns() {
		try (Stream<Path> paths = Files.list(workPath)) {
			runPaths = paths.filter(p -> p.getFileName().toString().startsWith("run") && Files.isDirectory(p)).sorted()
					.toArray(Path[]::new);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Path workPath = Paths
				.get("/home/kensuke/data/WesternPacific/anelasticity/montecarlo/selection/group2/sigmaHalflikelihood2");
		ModelProbability mp = new ModelProbability(workPath);
		Path[] modelPaths = mp.gatherModelPaths(workPath);
		PolynomialStructure[] models = Arrays.stream(modelPaths).map(p -> {
			try {
				return new PolynomialStructure(p);
			} catch (Exception e) {
			}
			return null;
		}).toArray(PolynomialStructure[]::new);
		modelsToDepths(models, workPath.resolve("test"));
		depthTohistGram(workPath.resolve("test"));
		forGMT(workPath.resolve("test"));
	}

	private static void depthTohistGram(Path depthsPath) {
		for (int i = 3505; i < 3856; i += 50)
			makeHistgram(depthsPath.resolve(i + ".inf"));

	}

	private static double vinterval = 0.1;

	private static int findValue(double target, List<String> lines) {
		double[] x = new double[lines.size()];
		int[] y = new int[lines.size()];
		for (int i = 0; i < x.length; i++) {
			String[] parts = lines.get(i).split("\\s+");
			x[i] = Double.parseDouble(parts[0]);
			y[i] = Integer.parseInt(parts[1]);
		}
		if (target < x[0])
			return 0;
		if (x[x.length - 1] < target)
			return 0;
		for (int i = 0; i < x.length - 1; i++)
			if (target <= x[i + 1])
				return y[i];

		return 0;
	}

	private static void forGMT(Path depthsPath) {
		double vstart = 6.8;
		double vend = 7.5;
		double vinterval = 0.005;
		int xN = (int) ((vend - vstart) / vinterval);
		List<String> outlines = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			int ir = 3505 + i * 50;
			try {
				Path path = depthsPath.resolve(ir + ".inf.v");
				List<String> lines = Files.readAllLines(path);
				for (int j = 0; j < xN; j++) {
					double x = vstart + vinterval * j;
					int y = findValue(x, lines);
					for (int k = -5; k < 6; k++)
						outlines.add(x + " " + (ir + k * 5) + " " + y);

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			Path out = depthsPath.resolve("xyz.txt");
			Files.write(out, outlines);
		} catch (Exception e) {
		}

	}

	private static void modelsToDepths(PolynomialStructure[] ps, Path outDir) {
		if (Files.exists(outDir))
			return;
		try {
			Files.createDirectories(outDir);
			for (int i = 0; i < 8; i++) {
				String name = (3480 + 25 + 50 * i) + ".inf";
				Path outPath = outDir.resolve(name);
				List<String> lines = new ArrayList<>();
				for (int j = 0; j < ps.length; j++) {
					double[] vs = readVs(ps[j]);
					double[] q = readQ(ps[j]);
					lines.add(vs[i] + " " + q[i]);
				}
				Files.write(outPath, lines);
			}
		} catch (Exception e) {
		} finally {
		}

	}

	private static Path getModelPath(Path runPath) {
		int i = Integer.parseInt(runPath.getFileName().toString().replace("run", ""));
		Path path = runPath.resolve(i + ".model");
		if (Files.exists(path))
			return path;
		for (int j = i; 0 <= j; j--) {
			path = runPath.resolve(i + "." + j + ".model");
			if (Files.exists(path))
				return path;
		}
		return null;

	}

	private Path[] gatherModelPaths(Path path) {
		Path[] modelPaths = Arrays.stream(runPaths).map(ModelProbability::getModelPath).toArray(Path[]::new);
		return modelPaths;
	}

	private static List<String> toOutLines(int[] hist, double[] boarder) {
		List<String> lines = new ArrayList<>();
		for (int i = 0; i < hist.length; i++) {
			lines.add(boarder[i] + " " + hist[i]);
			lines.add(boarder[i + 1] + " " + hist[i]);
		}
		return lines;
	}

	private static void makeHistgram(Path depthPath) {

		try {
			List<String> lines = Files.readAllLines(depthPath);
			double[] q = lines.stream().mapToDouble(line -> Double.parseDouble(line.split("\\s+")[1])).toArray();
			double[] vs = lines.stream().mapToDouble(line -> Double.parseDouble(line.split("\\s+")[0])).toArray();
			Path vHist = depthPath.getParent().resolve(depthPath.getFileName() + ".v");
			Path qHist = depthPath.getParent().resolve(depthPath.getFileName() + ".q");
			double[] vBoarder = makeBoarder(7.15 * 0.96, 7.15 * 1.04, 0.01);
			double[] qBoarder = makeBoarder(312 * 0.9, 312 * 1.1, 5);
			int[] qhist = makeHistgram(q, qBoarder);
			int[] vhist = makeHistgram(vs, vBoarder);
			List<String> qLines = toOutLines(qhist, qBoarder);
			List<String> vLines = toOutLines(vhist, vBoarder);
			Files.write(qHist, qLines);
			Files.write(vHist, vLines);
		} catch (Exception e) {
		}
	}

	private static double[] makeBoarder(double min, double max, double interval) {
		int n = (int) ((max - min) / interval);

		double[] boarder = new double[n + 1];
		for (int i = 0; i < n; i++)
			boarder[i] = min + i * interval;
		boarder[n] = max;
		return boarder;
	}

	private static int[] makeHistgram(double[] array, double[] boarder) {
		Arrays.sort(array);
		int n = boarder.length - 1;
		int[] histgram = new int[n];

		for (int i = 0; i < n; i++) {
			double left = boarder[i];
			double right = boarder[i + 1];
			for (double d : array)
				if (left <= d && d < right)
					histgram[i]++;
			// System.out.println(i + " " + left + " " + right + " " +
			// histgram[i]);
		}
		return histgram;
	}

	static double[] readVs(PolynomialStructure structure) {
		double[] vs = new double[8];
		for (int j = 0; j < 8; j++) {
			double r = 3480 + (j) * 50 + 25;
			vs[j] = structure.getVshAt(r);
		}
		return vs;
	}

	static double[] readQ(PolynomialStructure structure) {
		double[] q = new double[8];
		for (int j = 0; j < 8; j++) {
			double r = 3480 + (j) * 50 + 25;
			q[j] = structure.getQmuAt(r);
		}
		return q;
	}

}
