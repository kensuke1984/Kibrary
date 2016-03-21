/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import io.github.kensuke1984.kibrary.util.Utilities;

/**
 * 
 * This class is only for CLI use.
 * 
 * 
 * @author Kensuke Konishi
 * @version 0.1.2.3
 * 
 */
final class TravelTimeCLI {

	/**
	 * prohibits accesses from outside
	 */
	private TravelTimeCLI() {
	}

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
	 */
	public static void main(String[] args) {
		if (args.length == 0 || Arrays.stream(args).anyMatch("-version"::equals)) {
			About.main(null);
			return;
		}
		// add options
		setBooleanOptions();
		setArgumentOptions();
		if (Arrays.stream(args).anyMatch("-help"::equals)) {
			printHelp();
			return;
		}
		exec(args);
	}

	private final static void exec(String[] args) {
		// create the command line parser
		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine cmd = parser.parse(options, args);

			if (hasConflict(cmd))
				return;

			VelocityStructure structure = createVelocityStructure(cmd);
			Phase targetPhase = createPhase(cmd);

			boolean sv = cmd.hasOption("SV");
			double eventR = setDepth(cmd);
			double targetDelta = setDelta(cmd);
			if (cmd.hasOption("p")) {
				double rayParameter = readRayparameter(cmd);
				if (rayParameter < 0)
					return;
				Raypath raypath = new Raypath(rayParameter, eventR, structure, sv);
				raypath.compute();
				printResults(-1, cmd, raypath, targetPhase);
				if (cmd.hasOption("eps"))
					raypath.outputEPS(Paths.get(targetPhase + ".eps"), targetPhase);
				return;
			}

			double interval = 10;
			if (cmd.hasOption("dR"))
				try {
					interval = Double.parseDouble(cmd.getOptionValue("dR"));

				} catch (Exception e) {
					throw new RuntimeException("The value dR is invalid " + cmd.getOptionValue("dR"));
				}
			List<Raypath> raypaths = RaypathSearch.lookFor(targetPhase, structure, eventR, targetDelta, interval, sv);
			if (raypaths.isEmpty()) {
				System.out.println("No raypaths satisfying the input condition");
				return;
			}
			if (targetPhase.isDiffracted()) {
				Raypath raypath = raypaths.get(0);
				double delta = raypath.computeDelta(targetPhase);
				double dDelta = Math.toDegrees(targetDelta - delta);
				Phase diffPhase = Phase.create(targetPhase.toString() + dDelta);
				printResults(-1, cmd, raypath, diffPhase);
				if (cmd.hasOption("eps"))
					raypath.outputEPS(Paths.get(targetPhase + ".eps"), diffPhase);
				return;
			}
			for (Raypath raypath : raypaths) {
				printResults(Math.toDegrees(targetDelta), cmd, raypath, targetPhase);
				int j = 0;
				if (cmd.hasOption("eps"))
					if (raypaths.size() == 1)
						raypath.outputEPS(Paths.get(targetPhase.toString() + ".eps"), targetPhase);
					else
						raypath.outputEPS(Paths.get(targetPhase.toString() + "." + j++ + ".eps"), targetPhase);
			}

		} catch (Exception e) {
			System.out.println("improper usage");
			e.printStackTrace();
//			printHelp();
			return;
		}

	}

	/**
	 * @param cmd
	 * @return rayparameter in -p or -1 if it cannot be a double value.
	 */
	private static double readRayparameter(CommandLine cmd) {
		try {
			double p = Double.parseDouble(cmd.getOptionValue("p"));
			return p;
		} catch (Exception e) {
			System.out.println("ray parameter is invalid");
			return -1;
		}
	}

	private static void printLine(int decimalPlace, double... values) {
		for (double value : values)
			System.out.print(Utilities.fixDecimalPlaces(decimalPlace, value) + " ");
		System.out.println();

	}

	/**
	 * print result according to options
	 * 
	 * @param delta
	 *            [deg]
	 * @param cmd
	 * @param raypath
	 * @param phase
	 */
	private static void printResults(double delta1, CommandLine cmd, Raypath raypath, Phase phase) {
		double p0 = raypath.getRayParameter();
		double delta0 = raypath.computeDelta(phase);
		double time0 = raypath.computeTraveltime(phase);
		if (Double.isNaN(delta0) || Double.isNaN(time0)) {
			System.out.println(delta0 + " " + time0);
			return;
		}
		delta0 = Math.toDegrees(delta0);
		double time1 = 0;
		double pInterval = 0.1;

		if (0 < delta1) {
			try {
				while ((time1 = RaypathSearch.travelTimeByThreePointInterpolate(delta1, raypath, phase, pInterval)) < 0)
					pInterval *= 10;
			} catch (Exception e) {
			}
			if (0 < time1) {
				time0 = time1;
				delta0 = delta1;
			}
		}
		try {
			int n = cmd.hasOption("dec") ? Integer.parseInt(cmd.getOptionValue("dec")) : 2;
			if (n < 0)
				throw new RuntimeException("Invalid value for \"dec\"");
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

		// System.out.println("Epicentral distance [deg] Travel time [s]" );
	}

	private static Phase createPhase(CommandLine cmd) {
		Phase phase = null;
		if (cmd.hasOption("ph")) {
			phase = Phase.create(cmd.getOptionValue("ph"));
		} else
			phase = Phase.create("S");
		return phase;
	}

	/**
	 * @param cmd input options
	 * @return target delta [rad]
	 */
	private static double setDelta(CommandLine cmd) {
		return cmd.hasOption("deg") ? Math.toRadians(Double.parseDouble(cmd.getOptionValue("deg"))) : 0;
	}

	private static double setDepth(CommandLine cmd) {
		return cmd.hasOption("h") ? 6371 - Double.parseDouble(cmd.getOptionValue("h")) : 6371;
	}

	private static void printHelp() {
		helpFormatter.printHelp("Travel time", options);
	}

	private static VelocityStructure createVelocityStructure(CommandLine cmd) {
		VelocityStructure structure = null;
		Path modelPath = null;
		if (cmd.hasOption("mod")) {
			switch (cmd.getOptionValue("mod")) {
			case "AK135":
				structure = PolynomialStructure.AK135;
				break;
			case "PREM":
				structure = PolynomialStructure.PREM;
				break;
			case "iPREM":
				structure = PolynomialStructure.ISO_PREM;
				break;
			default:
				modelPath = Paths.get(cmd.getOptionValue("mod"));
			}
		} else
			structure = PolynomialStructure.PREM;

		if (modelPath != null) {
			if (Files.exists(modelPath)) {
				try {
					structure = new PolynomialStructure(modelPath);
					return structure;
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					structure = new NamedDiscontinuityStructure(modelPath);
					return structure;
				} catch (Exception e) {
					e.printStackTrace();
				}
				throw new RuntimeException("Input model file is not acceptable.");
			}
			throw new RuntimeException("No input model file " + modelPath);
		}
		return structure;
	}

	private static void setBooleanOptions() {
		options.addOption("SV", false, "compute travel time for SV (default: SH)");
		options.addOption("help", false, "print this message");
		options.addOption("eps", false, "output path figure");
		options.addOption(null, "rayp", false, "show only ray parameters");
		options.addOption(null, "time", false, "show only travel times");
		options.addOption(null, "delta", false, "show only epicentral distances");
		options.addOption(null, "version", false, "show information of the tool.");

	}

	private static void setArgumentOptions() {
		Option h = new Option("h", true, "depth of source [km]");
		options.addOption(h);
		Option deg = new Option("deg", true, "epicentral distance [deg]");
		options.addOption(deg);
		Option phase = new Option("ph", true, "seismic phase (necessary)");
		phase.setRequired(true);
		options.addOption(phase);
		Option model = new Option("mod", true, "structure (e.g. PREM) (default:PREM)");
		options.addOption(model);
		Option dec = new Option("dec", true, "number of decimal places.");
		options.addOption(dec);
		Option p = new Option("p", true, "ray parameter");
		options.addOption(p);

		Option dR = new Option("dR", true, "Integral interval(default = 10.0) [km]");
		options.addOption(dR);

	}

	/**
	 * check if there are conflicts
	 * 
	 * @param cmd
	 * @return true if there are some conflicts
	 */
	private static boolean hasConflict(CommandLine cmd) {
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

		if (cmd.hasOption("rayp")) {
			if (cmd.hasOption("time") || cmd.hasOption("delta")) {
				System.err.println("Only one option out of --rayp," + " --delta and --time can be used at once.");
				return true;
			}
		} else if (cmd.hasOption("time") && cmd.hasOption("delta")) {
			System.err.println("Only one option out of --rayp," + " --delta and --time can be used at once.");
			return true;
		}

		return false;

	}
}
