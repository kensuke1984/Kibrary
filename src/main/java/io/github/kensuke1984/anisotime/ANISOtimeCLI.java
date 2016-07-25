/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.util.Precision;

import io.github.kensuke1984.kibrary.util.Utilities;

/**
 * 
 * This class is only for CLI use of ANISOtime.
 * 
 * @author Kensuke Konishi
 * @version 0.3b
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

	/**
	 * Creates a catalog according to the commands.
	 * 
	 * @throws IOException
	 *             if any
	 */
	private void createCatalog() throws IOException {
		Path catalogPath = Paths.get(cmd.getOptionValue("cc"));
		VelocityStructure structure = createVelocityStructure();
		ComputationalMesh mesh = ComputationalMesh.simple(); // TODO
		Files.createFile(catalogPath);
		RaypathCatalog rc = RaypathCatalog.computeCatalogue(structure, mesh, dDelta);
		rc.write(catalogPath);
	}

	private RaypathCatalog catalog;

	/**
	 * [rad]
	 */
	private double targetDelta;

	private VelocityStructure structure;

	private double eventR;

	private Phase targetPhase; // TODO phases

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
			structure = createVelocityStructure();
			eventR = structure.earthRadius() - Double.parseDouble(cmd.getOptionValue("h", "0"));
		}

		targetPhase = Phase.create(cmd.getOptionValue("ph", "S"), cmd.hasOption("SV"));

		targetDelta = Math.toRadians(Double.parseDouble(cmd.getOptionValue("deg", "NaN")));

		interval = Double.parseDouble(cmd.getOptionValue("dR", "10"));

		rayParameter = Double.parseDouble(cmd.getOptionValue("p", "NaN"));

	}

	private void run() {

		try {

			if (hasConflict())
				return;

			setParameters();

			// Catalog creation
			if (cmd.hasOption("cc")) {
				createCatalog();
				return;
			}

			// When the ray parameter is given
			if (cmd.hasOption("p")) {
				Raypath raypath = new Raypath(rayParameter, structure);
				raypath.compute();
				printResults(-1, raypath);
				if (cmd.hasOption("eps"))
					raypath.outputEPS(eventR, Paths.get(targetPhase + ".eps"), targetPhase);
				return;
			}

			// Compute a catalog
			if (!cmd.hasOption("rc")) {
				ComputationalMesh mesh = ComputationalMesh.simple(); // TODO
				catalog = RaypathCatalog.computeCatalogue(structure, mesh, dDelta);
			}

			Raypath[] raypaths = catalog.searchPath( targetPhase, eventR, targetDelta);

			// List<Raypath> raypaths = RaypathSearch.lookFor(targetPhase,
			// structure, eventR, targetDelta, interval);
			if (raypaths.length == 0) {
				System.err.println("No raypaths satisfying the input condition");
				return;
			}
			if (targetPhase.isDiffracted()) {
				Raypath diffRay = raypaths[0];
				double deltaOnBoundary = Math.toDegrees(targetDelta - raypaths[0].computeDelta(eventR, targetPhase));
				if (deltaOnBoundary < 0) {
					System.err.println("Sdiff would have longer distance than "
							+ Precision.round(Math.toDegrees(diffRay.computeDelta(eventR, targetPhase)), 3)
							+ " (Your input:" + Precision.round(Math.toDegrees(targetDelta), 3) + ")");
					return;
				}
				targetPhase = Phase.create(targetPhase.toString() + deltaOnBoundary, targetPhase.isPSV());
			}
			if (targetPhase.isDiffracted()) {
				Raypath raypath = raypaths[0];
				double delta = raypath.computeDelta(eventR, targetPhase);
				double dDelta = Math.toDegrees(targetDelta - delta);
				targetPhase = Phase.create(targetPhase.toString() + dDelta, targetPhase.isPSV());
				printResults(-1, raypath);
				if (cmd.hasOption("eps"))
					raypath.outputEPS(eventR, Paths.get(targetPhase + ".eps"), targetPhase);
				return;
			}

			for (Raypath raypath : raypaths) {
				printResults(Math.toDegrees(targetDelta), raypath);
				int j = 0;
				if (cmd.hasOption("eps"))
					if (raypaths.length == 1)
						raypath.outputEPS(eventR, Paths.get(targetPhase.toString() + ".eps"), targetPhase);
					else
						raypath.outputEPS(eventR, Paths.get(targetPhase.toString() + "." + j++ + ".eps"), targetPhase);
			}

		} catch (Exception e) {
			System.err.println("improper usage");
			e.printStackTrace();
			return;
		}

	}

	private static void printLine(int decimalPlace, double... values) {
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
	private void printResults(double delta1, Raypath raypath) {
		double p0 = raypath.getRayParameter();
		double delta0 = raypath.computeDelta(eventR, targetPhase);
		double time0 = raypath.computeT(eventR, targetPhase);
		if (Double.isNaN(delta0) || Double.isNaN(time0)) {
			System.out.println(delta0 + " " + time0);
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
			// System.out.println("Epicentral distance [deg] Travel time [s]"
			// );
			if (cmd.hasOption("rayp"))
				printLine(n, p0);
			else if (cmd.hasOption("time"))
				printLine(n, time0);
			else if (cmd.hasOption("delta"))
				printLine(n, delta0);
			else
				printLine(n, p0, delta0, time0);
			return;
		} catch (Exception e) {
			System.err.println("Option digit only accepts a positive integer " + cmd.getOptionValue("digit"));
		}
	}

	private static void printHelp() {
		helpFormatter.printHelp("Travel time", options);
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
		options.addOption("ph", true, "seismic phase (default = S)");
		options.addOption("mod", true, "structure (default:PREM)");
		options.addOption("dec", true, "number of decimal places.");
		options.addOption("p", true, "ray parameter");
		options.addOption("dR", true, "Integral interval [km] (default = 10.0)");
		options.addOption("dD", true, "Parameter for a catalog creation (d\u0394).");
		options.addOption("rc", "read-catalog", true, "Computes travel times from a catalog.");
		options.addOption("cc", "create-catalog", true, "Creates a catalog.");
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

		if (cmd.hasOption("cc")) {
			boolean out = false;
			if (cmd.hasOption("rc")) {
				System.err.println(
						"Cannot create and read a catalog simultaneously. Please create one first and then use it.");
				out = true;
			}
			if (cmd.hasOption("p") || cmd.hasOption("deg")) {
				System.err.println(
						"When you create a catalog, you cannot specify a ray parameter and an epicentral distance.");
				out = true;
			}
			if (!cmd.hasOption("mod")) {
				System.err.println("When you create a catalog, you must specfy a velocity model (e.g. -mod PREM).");
				out = true;
			}
			if (out)
				return true;
		} else {
			if (cmd.hasOption("dD")) {
				System.err.println("Option dD is used only when you create a catalog.");
				return true;
			}
			if (cmd.hasOption("p")) {
				if (cmd.hasOption("deg")) {
					System.err.println("You can not use both option -p and -deg simultaneously.");
					return true;
				}
			} else if (!cmd.hasOption("deg")) {
				System.err.println("Use the option -deg to specify the epicentral distance,"
						+ " or you can directly choose a raypath for your input ray parameter by the option -p");
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
