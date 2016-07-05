/**
 * 
 */
package io.github.kensuke1984.kibrary.external.gmt;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

/**
 * This is like pathDrawer.pl The pathDrawer compute raypath coordinate. But
 * this class uses raypath by GMT.
 * 
 * event and station are necessary.
 * 
 * <b>Assume that there are no stations with the same name but different
 * networks in an event</b>
 * 
 * 
 * @author Kensuke Konishi
 * @version 0.1.1
 * 
 */
public class RaypathDistribution implements Operation {

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(RaypathDistribution.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan RaypathDistribution");
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("##Work folder (.)");
			pw.println("#workPath");
			pw.println("##boolean true if you want to draw raypath (false)");
			pw.println("#raypath");
			pw.println("##StationInformationFile a file containing station information must be set!!");
			pw.println("#stationInformationPath station.inf");
			pw.println("##Path of a time window information file.");
			pw.println("##If it exists, draw raypaths in the file");
			pw.println("#timeWindowInformationPath");
		}
		System.err.println(outPath + " is created.");
	}

	private Set<GlobalCMTID> ids;

	public RaypathDistribution(Properties properties) throws IOException {
		property = (Properties) properties.clone();
		set();
	}

	private Path workPath;

	/**
	 * parameterのセット
	 */
	private void set() throws IOException {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));

		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());

		drawsPath = Boolean.parseBoolean(property.getProperty("raypath"));
		Path stationPath = getPath("stationInformationPath");
		stationSet = StationInformationFile.read(stationPath);
		// drawsPoint = Boolean.parseBoolean(reader.getString("partial"));
		if (property.containsKey("timeWindowInformationPath")) {
			Path f = getPath("timeWindowInformationPath");
			if (Files.exists(f))
				timeWindowInformationFile = TimewindowInformationFile.read(f);
		}
	}

	private Properties property;

	/**
	 * components for path
	 */
	private Set<SACComponent> components;

	/**
	 * draw path
	 */
	protected boolean drawsPath;

	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("stationInformationPath"))
			throw new RuntimeException("There is no information of a station information file.");
	}

	/**
	 * draw points of partial TODO
	 */
	// protected boolean drawsPoint;

	private Set<Station> stationSet;

	private Set<TimewindowInformation> timeWindowInformationFile;

	/**
	 * @param args
	 *            [parameter file name]
	 */
	public static void main(String[] args) throws IOException {
		Properties property = Property.parse(args);
		long start = System.nanoTime();
		RaypathDistribution rd = new RaypathDistribution(property);
		System.out.println(RaypathDistribution.class.getName() + " is going.");
		rd.run();
		System.out.println(RaypathDistribution.class.getName() + " finished in "
				+ Utilities.toTimeString(System.nanoTime() - start));
	}

	private Path stationPath;
	private Path eventPath;
	private Path raypathPath;
	private Path psPath;
	private Path gmtPath;

	private void setName() {
		String date = Utilities.getTemporaryString();
		stationPath = workPath.resolve("rdStation" + date + ".inf");
		eventPath = workPath.resolve("rdEvent" + date + ".inf");
		raypathPath = workPath.resolve("rdRaypath" + date + ".inf");
		psPath = workPath.resolve("rd" + date + ".eps");
		gmtPath = workPath.resolve("rd" + date + ".sh");
	}

	private void outputEvent() throws IOException {
		List<String> lines = new ArrayList<>();
		for (GlobalCMTID id : ids) {
			Location loc = id.getEvent().getCmtLocation();
			double latitude = loc.getLatitude();
			double longitude = loc.getLongitude();
			longitude = 0 <= longitude ? longitude : longitude + 360;
			lines.add(id + " " + latitude + " " + longitude + " " + loc.getR());
		}
		Files.write(eventPath, lines);
	}

	private void outputStation() throws IOException {
		List<String> lines = stationSet.stream().map(station -> station + " " + station.getPosition())
				.collect(Collectors.toList());
		if (!lines.isEmpty())
			Files.write(stationPath, lines);
	}

	@Override
	public void run() throws IOException {
		setName();
		ids = Utilities.globalCMTIDSet(workPath);
		outputEvent();
		outputStation();
		if (drawsPath)
			outputRaypath();
		outputGMT();
	}

	/**
	 * @param name
	 *            Sacfile
	 * @return if the path of Sacfile should be drawn
	 */
	private boolean inTimeWindow(SACFileName name) {
		return timeWindowInformationFile == null ? true
				: timeWindowInformationFile.stream()
						.anyMatch(tw -> tw.getComponent() == name.getComponent()
								&& tw.getGlobalCMTID().equals(name.getGlobalCMTID())
								&& tw.getStation().getStationName().equals(name.getStationName()));
	}

	private void outputRaypath() throws IOException {
		List<String> lines = Utilities.eventFolderSet(workPath).stream().flatMap(eventDir -> {
			try {
				return eventDir.sacFileSet().stream();
			} catch (Exception e) {
				return Stream.empty();
			}
		}).filter(name -> name.isOBS() && components.contains(name.getComponent())).filter(this::inTimeWindow)
				.map(name -> {
					try {
						return name.readHeader();
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				}).filter(Objects::nonNull)
				.map(header -> header.getSACString(SACHeaderEnum.KSTNM) + " " + header.getSACString(SACHeaderEnum.KEVNM)
						+ " " + header.getEventLocation() + " " + Station.of(header).getPosition())
				.collect(Collectors.toList());

		Files.write(raypathPath, lines);
	}

	private GMTMap createBaseMap() {

		double minimumEventLatitude = ids.stream().mapToDouble(id -> id.getEvent().getCmtLocation().getLatitude()).min()
				.getAsDouble();
		double maximumEventLatitude = ids.stream().mapToDouble(id -> id.getEvent().getCmtLocation().getLatitude()).max()
				.getAsDouble();

		double minimumEventLongitude = ids.stream().mapToDouble(e -> e.getEvent().getCmtLocation().getLongitude())
				.map(d -> 0 <= d ? d : d + 360).min().getAsDouble();
		double maximumEventLongitude = ids.stream().mapToDouble(e -> e.getEvent().getCmtLocation().getLongitude())
				.map(d -> 0 <= d ? d : d + 360).max().getAsDouble();

		double minimumStationLatitude = stationSet.stream().mapToDouble(s -> s.getPosition().getLatitude()).min()
				.orElse(minimumEventLatitude);
		double maximumStationLatitude = stationSet.stream().mapToDouble(s -> s.getPosition().getLatitude()).max()
				.orElse(maximumEventLatitude);

		double minimumStationLongitude = stationSet.stream().mapToDouble(s -> s.getPosition().getLongitude())
				.map(d -> 0 <= d ? d : d + 360).min().orElse(minimumEventLongitude);
		double maximumStationLongitude = stationSet.stream().mapToDouble(s -> s.getPosition().getLongitude())
				.map(d -> 0 <= d ? d : d + 360).max().orElse(maximumEventLongitude);

		int minLatitude = (int) Math
				.round(minimumEventLatitude < minimumStationLatitude ? minimumEventLatitude : minimumStationLatitude)
				/ 5 * 5 - 10;
		int maxLatitude = (int) Math
				.round(maximumEventLatitude < maximumStationLatitude ? maximumStationLatitude : maximumEventLatitude)
				/ 5 * 5 + 10;
		int minLongitude = (int) Math.round(
				minimumEventLongitude < minimumStationLongitude ? minimumEventLongitude : minimumStationLongitude) / 5
				* 5 - 10;
		int maxLongitude = (int) Math.round(
				maximumEventLongitude < maximumStationLongitude ? maximumStationLongitude : maximumEventLongitude) / 5
				* 5 + 10;
		if (minLatitude < -90)
			minLatitude = -90;
		if (90 < maxLatitude)
			maxLatitude = 90;
		return new GMTMap("MAP", minLongitude, maxLongitude, minLatitude, maxLatitude);
	}

	private void outputGMT() throws IOException {
		GMTMap gmtmap = createBaseMap();
		List<String> gmtCMD = new ArrayList<>();
		gmtCMD.add("#!/bin/sh");
		gmtCMD.add("psname=\"" + psPath + "\"");
		gmtCMD.add(gmtmap.psStart());
		if (drawsPath) {
			gmtCMD.add("while  read line");
			gmtCMD.add("do");
			gmtCMD.add("echo $line |awk '{print $3, $4, \"\\n\", $6, $7}' | \\");
			gmtCMD.add("psxy -: -J -R -O -P -K  -W0.25,grey,.@100 >>$psname");
			gmtCMD.add("done < " + raypathPath);
			// draw over the path
		}

		gmtCMD.add(GMTMap.psCoast());
		gmtCMD.add("awk '{print $2, $3}' " + eventPath + " | psxy -V -: -J -R -O -P -Sa0.3 -G255/0/0 -W1  -K "
				+ " >> $psname");
		gmtCMD.add("awk '{print $2, $3}' " + stationPath + " |psxy -V -: -J -R -K -O -P -Si0.3 -G0/0/255 -W1 "
				+ " >> $psname");
		gmtCMD.add(gmtmap.psEnd());
		gmtCMD.add("#eps2eps $psname .$psname && mv .$psname $psname");
		Files.write(gmtPath, gmtCMD);
		gmtPath.toFile().setExecutable(true);
	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

}
