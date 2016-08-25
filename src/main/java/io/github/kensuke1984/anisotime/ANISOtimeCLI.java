/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.util.Precision;

import io.github.kensuke1984.kibrary.util.Utilities;

/**
 * 
 * This class is only for CLI use of ANISOtime.
 * 
 * TODO customize for catalog ddelta
 * 
 * @author Kensuke Konishi
 * @version 0.3.5.1b
 */
final class ANISOtimeCLI {

	/**
	 * @param args
	 *            arguments
	 * @throws ParseException
	 *             if any
	 */
	private ANISOtimeCLI(String[] args) throws ParseException {
		cmd = new DefaultParser().parse(options, args);
	}

	/**
	 * Input for ANISOtime
	 */
	private final CommandLine cmd;

	/**
	 * Options
	 */
	private final static Options options = new Options();

	/**
	 * Help Formatter
	 */
	private final static HelpFormatter helpFormatter = new HelpFormatter();

	static {
		// add options
		setBooleanOptions();
		setArgumentOptions();
	}

	/**
	 * @param args
	 *            [commands]
	 * @throws ParseException
	 */
	public static void main(String[] args) throws ParseException {
		if (args.length == 0) {
			About.main(null);
			return;
		}

		for (String o : args)
			if (o.equals("-help") || o.equals("--help")) {
				printHelp();
				return;
			}

		for (String o : args)
			if (o.equals("-v") || o.equals("--version") || o.equals("-version")) {
				About.main(null);
				return;
			}

		ANISOtimeCLI cli = new ANISOtimeCLI(args);
		cli.run();
	}

	private RaypathCatalog catalog;

	/**
	 * [rad]
	 */
	private double targetDelta;

	private VelocityStructure structure;

	private double eventR;

	private Phase[] targetPhases;

	private double dDelta;

	private double interval;

	private double rayParameter;

	/**
	 * Sets parameters according to the input arguments.
	 */
	private void setParameters() {
		dDelta = Math.toRadians(Double.parseDouble(cmd.getOptionValue("dD", "1")));

		if (cmd.hasOption("rc")) {
			try {
				Path catalogPath = Paths.get(cmd.getOptionValue("rc"));
				catalog = RaypathCatalog.read(catalogPath);
				structure = catalog.getStructure();
				eventR = structure.earthRadius() - Double.parseDouble(cmd.getOptionValue("h", "0"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			if (!cmd.hasOption("mod"))
				throw new RuntimeException("You must specfy a velocity model(e.g. -mod prem).");
			structure = createVelocityStructure();
			eventR = structure.earthRadius() - Double.parseDouble(cmd.getOptionValue("h", "0"));
			// option TODO
			ComputationalMesh mesh = ComputationalMesh.simple(structure);
			catalog = RaypathCatalog.computeCatalogue(structure, mesh, dDelta);
		}

		if (cmd.hasOption("ph"))
			targetPhases = Arrays.stream(cmd.getOptionValue("ph").split(","))
					.map(n -> Phase.create(n, cmd.hasOption("SV"))).toArray(Phase[]::new);
		else
			targetPhases = new Phase[] { Phase.P, Phase.PcP, Phase.PKiKP, Phase.S, Phase.ScS, Phase.SKiKS };

		targetDelta = Math.toRadians(Double.parseDouble(cmd.getOptionValue("deg", "NaN")));

		interval = Double.parseDouble(cmd.getOptionValue("dR", "10"));

		rayParameter = Double.parseDouble(cmd.getOptionValue("p", "NaN"));

	}

	/**
	 * @return if all the argument options have values.
	 */
	private boolean checkArgumentOption() {
		boolean hasProblem = false;
		if (Arrays.stream(cmd.getOptions()).filter(Option::hasArg).anyMatch(o -> o.getValue().startsWith("-"))) {
			System.err.println("Some options are missing arguments.");
			hasProblem = true;
		}
		return hasProblem;
	}

	private void printRecordSection() throws IOException {
		String timeStr = Utilities.getTemporaryString();
		Path outDir = Paths.get(cmd.getOptionValue("o", ""));
		Files.createDirectories(outDir);

		double[] ranges = Arrays.stream(cmd.getOptionValue("rs").split(",")).mapToDouble(Double::parseDouble).toArray();
		double min = ranges[0];
		double max = ranges[1];
		double interval = 2 < ranges.length ? ranges[2] : 1;
		double[] targets = new double[(int) Math.ceil((max - min) / interval) + 1];
		for (int i = 0; i < targets.length; i++)
			targets[i] = min + interval * i;
		targets[targets.length - 1] = max;
		for (Phase phase : targetPhases) {
			Path out = outDir.resolve(phase.toString() + "." + timeStr + ".rcs");
			Map<Raypath, Double> deltaPathMap = new HashMap<>();
			for (double d : targets)
				for (Raypath p : catalog.searchPath(phase, eventR, Math.toRadians(d)))
					deltaPathMap.put(p, d);
			try (PrintStream ps = new PrintStream(Files.newOutputStream(out, StandardOpenOption.CREATE_NEW))) {
				deltaPathMap.keySet().stream().sorted(Comparator.comparingDouble(Raypath::getRayParameter).reversed())
						.forEach(r -> {
							printResults(deltaPathMap.get(r), r, phase, ps);
						});
			}
		}
	}

	private void run() {

		try {
			if (checkArgumentOption())
				throw new RuntimeException("Input arguments have problems.");

			if (hasConflict())
				return;
			setParameters();

			if (cmd.hasOption("rs")) {
				printRecordSection();
				return;
			}

			// only create a catalog
			if (!cmd.hasOption("p") && !cmd.hasOption("deg"))
				return;
			Path outDir = Paths.get(cmd.getOptionValue("o", ""));
			Files.createDirectories(outDir);
			String tmpStr = Utilities.getTemporaryString();
			// When the ray parameter is given
			if (cmd.hasOption("p")) {
				Raypath raypath = new Raypath(rayParameter, structure);
				raypath.compute();
				for (Phase targetPhase : this.targetPhases) {
					if (!raypath.exists(eventR, targetPhase)) {
						System.err.println(targetPhase+" does not exist.");
						continue;
					}
					printResults(-1, raypath, targetPhase, System.out);
					if (cmd.hasOption("eps"))
						raypath.outputEPS(eventR, outDir.resolve(targetPhase + "." + tmpStr + ".eps"), targetPhase,
								StandardOpenOption.CREATE_NEW);
				}
				return;
			}
			for (Phase targetPhase : this.targetPhases) {
				Raypath[] raypaths = catalog.searchPath(targetPhase, eventR, targetDelta);

				// List<Raypath> raypaths = RaypathSearch.lookFor(targetPhase,
				// structure, eventR, targetDelta, interval);
				if (raypaths.length == 0) {
					System.err.println("No raypaths satisfying the input condition");
					continue;
				}
				if (targetPhase.isDiffracted()) {
					Raypath diffRay = raypaths[0];
					double deltaOnBoundary = Math
							.toDegrees(targetDelta - raypaths[0].computeDelta(eventR, targetPhase));
					if (deltaOnBoundary < 0) {
						System.err.println("Sdiff would have longer distance than "
								+ Precision.round(Math.toDegrees(diffRay.computeDelta(eventR, targetPhase)), 3)
								+ " (Your input:" + Precision.round(Math.toDegrees(targetDelta), 3) + ")");
						return;
					}
					targetPhase = Phase.create(targetPhase.toString() + deltaOnBoundary, targetPhase.isPSV());
					printResults(-1, diffRay, targetPhase, System.out);
					if (cmd.hasOption("eps")) {
						diffRay.outputEPS(eventR, outDir.resolve(targetPhase + "." + tmpStr + ".eps"), targetPhase,
								StandardOpenOption.CREATE_NEW);
					}
					return;
				}

				for (Raypath raypath : raypaths) {
					printResults(Math.toDegrees(targetDelta), raypath, targetPhase, System.out);
					int j = 0;
					if (cmd.hasOption("eps"))
						if (raypaths.length == 1)
							raypath.outputEPS(eventR, outDir.resolve(targetPhase.toString() + "." + tmpStr + ".eps"),
									targetPhase, StandardOpenOption.CREATE_NEW);
						else
							raypath.outputEPS(eventR,
									outDir.resolve(targetPhase.toString() + "." + j++ + "." + tmpStr + ".eps"),
									targetPhase, StandardOpenOption.CREATE_NEW);
				}
			}
		} catch (Exception e) {
			System.err.println("improper usage");
			e.printStackTrace();
			return;
		}
	}

	private static void printLine(Phase phase, PrintStream out, int decimalPlace, double... values) {
		out.println(phase + " " + Arrays.stream(values).mapToObj(d -> Utilities.fixDecimalPlaces(decimalPlace, d))
				.collect(Collectors.joining(" ")));
	}

	/**
	 * 
	 * TODO print result according to options
	 * 
	 * @param delta1
	 *            [deg]
	 * @param raypath
	 *            Raypath
	 * @param targetPhase
	 *            phase to be printed
	 * @param out
	 *            resource to print in
	 */
	private void printResults(double delta1, Raypath raypath, Phase targetPhase, PrintStream out) {
		double p0 = raypath.getRayParameter();
		double delta0 = raypath.computeDelta(eventR, targetPhase);
		double time0 = raypath.computeT(eventR, targetPhase);
		if (Double.isNaN(delta0) || Double.isNaN(time0)) {
			System.out.println(p0);
			return;
		}
		delta0 = Math.toDegrees(delta0);
		double time1 = 0;
		double pInterval = 0.1;

		if (0 < delta1) {
			try {
				while ((time1 = RaypathCatalog.travelTimeByThreePointInterpolate(delta1, raypath, eventR, targetPhase,
						pInterval)) < 0)
					pInterval *= 10;
			} catch (Exception e) {
			}
			if (0 < time1) {
				time0 = time1;
				delta0 = delta1;
			}
		}
		try {
			int n = Integer.parseInt(cmd.getOptionValue("dec", "2"));
			if (n < 0)
				throw new IllegalArgumentException("Invalid value for \"dec\"");
			if (cmd.hasOption("rayp"))
				printLine(targetPhase, out, n, p0);
			else if (cmd.hasOption("time"))
				printLine(targetPhase, out, n, time0);
			else if (cmd.hasOption("delta"))
				printLine(targetPhase, out, n, delta0);
			else
				printLine(targetPhase, out, n, p0, delta0, time0);
			return;
		} catch (Exception e) {
			System.err.println("Option digit only accepts a positive integer " + cmd.getOptionValue("digit"));
		}
	}

	static void printHelp() {
		helpFormatter.printHelp("ANISOtime (CLI)", options);
	}

	/**
	 * According to the arguments in the command line(cmd), a velocity structure
	 * is given.
	 * 
	 * @return velocity structure according to the input. Default: PREM
	 */
	private VelocityStructure createVelocityStructure() {
		Path modelPath;
		if (cmd.hasOption("mod"))
			switch (cmd.getOptionValue("mod")) {
			case "ak135":
			case "AK135":
				return PolynomialStructure.AK135;
			case "prem":
			case "PREM":
				return PolynomialStructure.PREM;
			case "iprem":
			case "iPREM":
				return PolynomialStructure.ISO_PREM;
			default:
				modelPath = Paths.get(cmd.getOptionValue("mod"));
			}
		else
			return PolynomialStructure.PREM;

		if (Files.exists(modelPath)) {
			try {
				return new PolynomialStructure(modelPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				return new NamedDiscontinuityStructure(modelPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
			throw new RuntimeException("Input model file is not acceptable.");
		}
		throw new RuntimeException("No input model file " + modelPath + " is found.");
	}

	private static void setBooleanOptions() {
		options.addOption("SV", false, "Computes travel time for SV (default: SH)");
		options.addOption("SH", false, "Computes travel time for SH (default: SH)");
		options.addOption("help", "Shows this message. This option has the highest priority.");
		options.addOption("eps", false, "output path figure");
		options.addOption(null, "rayp", false, "Shows only ray parameters");
		options.addOption(null, "time", false, "Shows only travel times");
		options.addOption(null, "delta", false, "Shows only epicentral distances");
		options.addOption("v", "version", false,
				"Shows information of the tool. This option has the 2nd highest priority.");
	}

	private static void setArgumentOptions() {
		options.addOption("h", true, "Depth of source [km] (default = 0)");
		options.addOption("deg", "epicentral-distance", true, "Epicentral distance \u0394 [deg]");
		options.addOption("ph", "phase", true, "Seismic phase (default:P,PCP,PKiKP,S,ScS,SKiKS)");
		options.addOption("mod", true, "Structure (default:prem)");
		options.addOption("dec", true, "Number of decimal places.");
		options.addOption("p", true, "Ray parameter");
		options.addOption("dR", true, "Integral interval [km] (default:10.0)");
		options.addOption("dD", true, "Parameter for a catalog creation (d\u0394).");
		options.addOption("rc", "read-catalog", true, "Path of a catalog for which travel times are computed.");
		options.addOption("rs", "record-section", true,
				"start,end(,interval) [deg]  \n Computes a table of a record section for the range.");
		options.addOption("o", true, "Directory for ray path figures or record sections.");
	}

	/**
	 * check if there are conflicts
	 * 
	 * @return true if there are some conflicts
	 */
	private boolean hasConflict() {

		if (cmd.hasOption("SH")) {
			boolean out = false;
			if (cmd.hasOption("SV")) {
				System.err.println("Either SV or SH can be chosen.");
				out = true;
			}

			if (cmd.hasOption("ph")) {
				String phase = cmd.getOptionValue("ph");
				if (phase.contains("P") || phase.contains("p") || phase.contains("K")) {
					System.err.println(phase + " should be P-SV mode.");
					out = true;
				}
			}

			if (out)
				return true;
		}

		if (cmd.hasOption("rayp")) {
			if (cmd.hasOption("time") || cmd.hasOption("delta")) {
				System.err.println("Only one option out of --rayp," + " --delta and --time can be used at once.");
				return true;
			}
		} else if (cmd.hasOption("time") && cmd.hasOption("delta")) {
			System.err.println("Only one option out of --rayp," + " --delta and --time can be used at once.");
			return true;
		}

		if (cmd.hasOption("p") && cmd.hasOption("deg")) {
			System.err.println("You can not use both option -p and -deg		 simultaneously.");
			return true;
		}

		if (cmd.hasOption("rs")) {
			if (cmd.hasOption("p") || cmd.hasOption("deg")) {
				System.err.println("When you compute record sections, neither option -p nor -deg can be specified.");
				return true;
			}
			if (!cmd.hasOption("ph")) {
				System.err.println("When you compute record sections, -ph must be specified.");
				return true;
			}
			if (!cmd.hasOption("eps")) {
				System.err.println("When you compute record sctions, -eps can not be set.");
			}
		} else if (cmd.hasOption("o") && !cmd.hasOption("eps")) {
			System.err.println("-o can be set, only when you compute record sections or make ray path figures.");
			return true;
		}

		if (cmd.hasOption("rc") && cmd.hasOption("mod")) {
			System.err.println("When you read a catalog, you cannot specify a velocity model.");
			return true;
		}

		return false;

	}
}
