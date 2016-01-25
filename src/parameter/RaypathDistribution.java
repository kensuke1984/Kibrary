package parameter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * Information for {@link io.github.kensuke1984.kibrary.external.gmt.RaypathDistribution}
 * 
 * 
 * @author kensuke
 * @since 2015/01/22
 * @version 0.0.1
 * 
 * 
 * @version 0.0.1.1
 * @since 2015/8/7 {@link IOException}
 * 
 * @version 0.0.2
 * @since 2015/8/8 {@link Path} base
 * 
 * @version 0.0.3
 * @since 2015/9/14
 * Timewindow information
 * Station information 
 *
 * 
 * 
 */
public class RaypathDistribution extends ParameterFile {

	/**
	 * components for path
	 */
	protected Set<SACComponent> componentSet;

	/**
	 * draw path
	 */
	protected boolean drawsPath;
	

	/**
	 * draw points of partial TODO
	 */
	// protected boolean drawsPoint;

	protected Set<Station> stationSet;

	protected RaypathDistribution(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException("Parameter information is not enough.");
		set();
	}

	protected Set<TimewindowInformation> timeWindowInformationFile;

	/**
	 * parameterのセット
	 */
	private void set() throws IOException {

		workPath = Paths.get(reader.getString("workPath"));
		// eventInfoFile = newFile(reader.getFirstValue("eventInfoFile"));
		String[] components = reader.getStringArray("components"); // SacComponent
		componentSet = Arrays.stream(components).map(SACComponent::valueOf).collect(Collectors.toSet());

		drawsPath = Boolean.parseBoolean(reader.getString("raypath"));
		Path stationPath = getPath("stationInformationPath");
		stationSet = StationInformationFile.read(stationPath);
		// drawsPoint = Boolean.parseBoolean(reader.getString("partial"));
		if (reader.containsKey("timeWindowInformationPath")) {
			Path f = getPath("timeWindowInformationPath");
			if (Files.exists(f))
				timeWindowInformationFile =  TimewindowInformationFile.read(f);
		}
	}

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Path tmp = 0 < args.length?Paths.get(args[0]).toAbsolutePath():readFileName();
		outputList(tmp, StandardOpenOption.CREATE_NEW);
	}

	public static void outputList(Path outPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.gmt.RaypathDistribution");
			pw.println("#SacComponent[] components to be used");
			pw.println("components Z R T");
			pw.println("#File working directory full path is required!");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#boolean true if you want to draw raypath");
			pw.println("raypath false");
			pw.println("#StationInformationFile a file containing station information");
			pw.println("stationInformationPath station.inf");
			pw.println("#Path of a time window information file.");
			pw.println("#If it exists, draw raypaths in the file");
			pw.println("timeWindowInformationPath timewindow.dat");
		}
		setExecutable(outPath);

	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("components");
		parameterSet.add("raypath");
		parameterSet.add("stationInformationPath");

		return reader.containsAll(parameterSet);

	}
}
