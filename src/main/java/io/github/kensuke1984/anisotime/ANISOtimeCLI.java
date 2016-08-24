/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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
 * @version 0.3.4b
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

	/**
	 * @param args
	 *            [commands]
	 * @throws ParseException
	 */
	public static void main(String[] args) throws ParseException {
		if (args.length == 0 || Arrays.stream(args).anyMatch("-version"::equals)) {
			About.main(null);
			return;
		}

		// add options
		setBooleanOptions();
		setArgumentOptions();
		if (Arrays.stream(args).anyMatch("-help"::equals) || Arrays.stream(args).anyMatch("--help"::equals)) {
			printHelp();
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

	private void printRecordSection() {
		if (targetPhases.length != 1) {
			System.err.println("-ph can only be one phase");
			return;
		}

		double[] ranges = Arrays.stream(cmd.getOptionValue("rs").split(",")).mapToDouble(Double::parseDouble).toArray();
		double min = ranges[0];
		double max = ranges[1];
		double interval = 2 < ranges.length ? ranges[2] : 1;
		double[] targets = new double[(int) Math.ceil((max - min) / interval) + 1];
		for (int i = 0; i < targets.length; i++)
			targets[i] = min + interval * i;
		targets[targets.length - 1] = max;
		Map<Raypath, Double> deltaPathMap = new HashMap<>();
		for (double d : targets)
			for (Raypath p : catalog.searchPath(targetPhases[0], eventR, Math.toRadians(d)))
				deltaPathMap.put(p, d);

		deltaPathMap.keySet().stream().sorted(Comparator.comparingDouble(Raypath::getRayParameter).reversed())
		.forEach(r->{
			printResults(deltaPathMap.get(r), r, targetPhases[0]);
		});
		
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

			// When the ray parameter is given
			if (cmd.hasOption("p")) {
				Raypath raypath = new Raypath(rayParameter, structure);
				raypath.compute();
				for (Phase targetPhase : this.targetPhases) {
					printResults(-1, raypath, targetPhase);
					if (cmd.hasOption("eps"))
						raypath.outputEPS(eventR, Paths.get(targetPhase + ".eps"), targetPhase);
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
					printResults(-1, diffRay, targetPhase);
					if (cmd.hasOption("eps"))
						diffRay.outputEPS(eventR, Paths.get(targetPhase + ".eps"), targetPhase);
					return;
				}

				for (Raypath raypath : raypaths) {
					printResults(Math.toDegrees(targetDelta), raypath, targetPhase);
					int j = 0;
					if (cmd.hasOption("eps"))
						if (raypaths.length == 1)
							raypath.outputEPS(eventR, Paths.get(targetPhase.toString() + ".eps"), targetPhase);
						else
							raypath.outputEPS(eventR, Paths.get(targetPhase.toString() + "." + j++ + ".eps"),
									targetPhase);
				}
			}
		} catch (Exception e) {
			System.err.println("improper usage");
			e.printStackTrace();
			return;
		}

	}

	private static void printLine(Phase phase, int decimalPlace, double... values) {
		System.out.print(phase + " ");
		for (double value : values)
			System.out.print(Utilities.fixDecimalPlaces(decimalPlace, value) + " ");
		System.out.println();
	}

	/**
	 * 
	 * TODO print result according to options
	 * 
	 * @param delta1
	 *            [deg]
	 * @param raypath
	 *            Raypath
	 */
	private void printResults(double delta1, Raypath raypath, Phase targetPhase) {
		double p0 = raypath.getRayParameter();
		double delta0 = raypath.computeDelta(eventR, targetPhase);
		double time0 = raypath.computeT(eventR, targetPhase);
		if (Double.isNaN(delta0) || Double.isNaN(time0)) {
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
				printLine(targetPhase, n, p0);
			else if (cmd.hasOption("time"))
				printLine(targetPhase, n, time0);
			else if (cmd.hasOption("delta"))
				printLine(targetPhase, n, delta0);
			else
				printLine(targetPhase, n, p0, delta0, time0);
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
		options.addOption("SV", false, "compute travel time for SV (default: SH)");
		options.addOption("SH", false, "compute travel time for SH (default: SH)");
		options.addOption("help", "print this message");
		options.addOption("eps", false, "output path figure");
		options.addOption(null, "rayp", false, "show only ray parameters");
		options.addOption(null, "time", false, "show only travel times");
		options.addOption(null, "delta", false, "show only epicentral distances");
		options.addOption(null, "version", false, "show information of the tool.");
	}

	private static void setArgumentOptions() {
		options.addOption("h", true, "depth of source [km] (default = 0)");
		options.addOption("deg", "epicentral-distance", true, "epicentral distance \u0394 [deg]");
		options.addOption("ph", "phase", true, "seismic phase (default = S)");
		options.addOption("mod", true, "structure (default:PREM)");
		options.addOption("dec", true, "number of decimal places.");
		options.addOption("p", true, "ray parameter");
		options.addOption("dR", true, "Integral interval [km] (default = 10.0)");
		options.addOption("dD", true, "Parameter for a catalog creation (d\u0394).");
		options.addOption("rc", "read-catalog", true, "Computes travel times from a catalog.");
		options.addOption("rs", "record-section", true,
				"start,end(,interval) [deg]  \n computes a table of a record section for the range.");

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
				System.err.println("When you compute a record section, neither option -p nor -deg can be specified.");
				return true;
			}
			if (!cmd.hasOption("ph")) {
				System.err.println("When you compute a record section, -ph must be specified.");
				return true;
			}
		}

		if (cmd.hasOption("rc") && cmd.hasOption("mod")) {
			System.err.println("When you read a catalog, you cannot specify a velocity model.");
			return true;
		}

		return false;

	}
}
