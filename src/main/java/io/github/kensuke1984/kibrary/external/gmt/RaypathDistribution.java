package io.github.kensuke1984.kibrary.external.gmt;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.external.TauPPierceReader;
import io.github.kensuke1984.kibrary.external.TauPPierceReader.Info;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.EventCluster;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is like pathDrawer.pl The pathDrawer compute raypath coordinate. But
 * this class uses raypath by GMT.
 * <p>
 * event and station are necessary.
 * <p>
 * <b>Assume that there are no stations with the same name but different
 * networks in an event</b>
 *
 * @author Kensuke Konishi
 * @version 0.1.2
 * @author anselme add methods to draw raypaths inside D''
 */
public class RaypathDistribution implements Operation {

	/**
	 * draw Path Mode; 0: don't draw, 1: quick draw, 2: detailed draw
	 */
	protected int drawsPathMode;
	private Set<GlobalCMTID> ids;
	private Path workPath;
	private Properties property;
	/**
	 * components for path
	 */
	private Set<SACComponent> components;
	/**
	 * draw points of partial TODO
	 */
	// protected boolean drawsPoint;

	private Set<Station> stationSet;
	private Set<TimewindowInformation> timeWindowInformationFile;
	private Path stationPath;
	private Path eventPath;
	private Path eventCSVPath;
	private Path raypathPath;
	private Path turningPointPath;
	private Path psPath;
	private Path gmtPath;
	private Path eventClusterPath;
	private String model;
	private double pierceDepth;
	Map<GlobalCMTID, Integer> eventClusterMap;
	
	public RaypathDistribution(Properties properties) throws IOException {
		property = (Properties) properties.clone();
		set();
	}
	
	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(RaypathDistribution.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan RaypathDistribution");
			pw.println("##Work folder (.)");
			pw.println("#workPath");
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("##Integer if you want to draw raypath (0: don't draw, 1: quick draw, 2: detailed draw)");
			pw.println("#drawsPathMode");
			pw.println("##StationInformationFile a file containing station information; must be set");
			pw.println("#stationInformationPath station.inf");
			pw.println("##Path of a time window information file.");
			pw.println("##If it exists, draw raypaths in the file");
			pw.println("#timeWindowInformationPath");
			pw.println("#model");
			pw.println("#pierceDepth");
			pw.println("#eventClusterPath");
		}
		System.err.println(outPath + " is created.");
	}
	
	 /**
     * @param args [parameter file name]
     * @throws IOException if any
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

	private void set() throws IOException {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));
		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());
		drawsPathMode = Integer.parseInt(property.getProperty("drawsPathMode"));
		if (property.containsKey("timeWindowInformationPath")) {
			Path timewindowPath = getPath("timeWindowInformationPath");
			timeWindowInformationFile = TimewindowInformationFile.read(timewindowPath);
		}
		Path stationPath = getPath("stationInformationPath");
		if (timeWindowInformationFile == null) stationSet = StationInformationFile.read(stationPath);
		else stationSet = timeWindowInformationFile.stream().map(tw -> tw.getStation())
				.collect(Collectors.toSet());
		pierceDepth = Double.parseDouble(property.getProperty("pierceDepth"));
		model = property.getProperty("model");
		if (property.containsKey("eventClusterPath")) eventClusterPath = getPath("eventClusterPath");
		else eventClusterPath = null;
	}
	
	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath")) property.setProperty("workPath", "");
		if (!property.containsKey("components")) property.setProperty("components", "Z R T");
		if (!property.containsKey("stationInformationPath"))
			throw new RuntimeException("There is no information of a station information file.");
		if (!property.containsKey("pierceDepth")) property.setProperty("pierceDepth", "400");
		if (!property.containsKey("model")) property.setProperty("model", "prem");
		if (!property.containsKey("drawsPathMode")) property.setProperty("drawsPathMode", "0");
	}

	private void setName() {
		String date = Utilities.getTemporaryString();
		stationPath = workPath.resolve("rdStation" + date + ".inf");
		eventPath = workPath.resolve("rdEvent" + date + ".inf");
		raypathPath = workPath.resolve("rdRaypath" + date + ".inf");
		turningPointPath = workPath.resolve("rdTurningPoint" + date + ".inf");
		psPath = workPath.resolve("rd" + date + ".eps");
		gmtPath = workPath.resolve("rd" + date + ".sh");
		eventCSVPath = workPath.resolve("rdEvent" + date + ".csv");
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
	
	private void outputEventCSV() throws IOException {
		List<String> lines = new ArrayList<>();
		for (GlobalCMTID id : ids) {
			Location loc = id.getEvent().getCmtLocation();
			double latitude = loc.getLatitude();
			double longitude = loc.getLongitude();
			double depth = 6371. - loc.getR();
			double mw = id.getEvent().getCmt().getMw();
			double duration = id.getEvent().getHalfDuration() * 2;
			longitude = 0 <= longitude ? longitude : longitude + 360;
			lines.add(id + "," + latitude + "," + longitude + "," + depth + "," + mw + "," + duration);
		}
		Files.write(eventCSVPath, lines);
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
		if (timeWindowInformationFile == null)
			ids = Utilities.globalCMTIDSet(workPath);
		else
			ids = timeWindowInformationFile.stream().map(tw -> tw.getGlobalCMTID())
				.collect(Collectors.toSet());
		
		if (eventClusterPath != null) {
			eventClusterMap = new HashMap<GlobalCMTID, Integer>();
			EventCluster.readClusterFile(eventClusterPath).forEach(c -> eventClusterMap.put(c.getID(), c.getIndex()));
		}
		
		outputEvent();
		outputStation();
		outputEventCSV();
		switch (drawsPathMode) {
		case 1:
			outputRaypath();
			break;
		case 2:
			outputRaypathInside(pierceDepth);
			outputTurningPoint();
			break;
		default:
			break;
		}
		
		outputGMT();
	}

    /**
     * @param name Sacfile
     * @return if the path of Sacfile should be drawn
     */
	private boolean inTimeWindow(SACFileName name) {
		return timeWindowInformationFile == null ? true
				: timeWindowInformationFile.stream()
						.anyMatch(tw -> tw.getComponent() == name.getComponent()
								&& tw.getGlobalCMTID().equals(name.getGlobalCMTID())
								&& tw.getStation().getName().equals(name.getStationName()));
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
	
	private void outputTurningPoint() throws IOException {
		Phase phasetmp = Phase.ScS;
		if (components.size() == 1 && components.contains(SACComponent.Z))
			phasetmp = Phase.PcP;
		final Phase phase = phasetmp;
		
		List<String> lines = new ArrayList<>();
		Utilities.eventFolderSet(workPath).stream().flatMap(eventDir -> {
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
				.forEach(headerData -> {
					Location eventLocation = headerData.getEventLocation();
					HorizontalPosition stationPosition = headerData.getStation().getPosition();
					Info info = TauPPierceReader.getPierceInfo(eventLocation, stationPosition, model, phase)
						.get(0);
					Location turningPoint = info.getTurningPoint();
					lines.add(String.format("%.2f %.2f %.2f"
							, turningPoint.getLongitude()
							, turningPoint.getLatitude()
							, turningPoint.getR()));
				});
		
		Files.write(turningPointPath, lines);
	}
	
	private void outputRaypathInside(double pierceDepth) throws IOException {
		List<String> lines = new ArrayList<>();
		
		Phase phasetmp = Phase.ScS;
		if (components.size() == 1 && components.contains(SACComponent.Z))
			phasetmp = Phase.PcP;
		final Phase phase = phasetmp;
		
		Utilities.eventFolderSet(workPath).stream().flatMap(eventDir -> {
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
				.forEach(headerData -> {
					Location eventLocation = headerData.getEventLocation();
					HorizontalPosition stationPosition = headerData.getStation().getPosition();
					List<Info> infoList = TauPPierceReader.getPierceInfo(eventLocation, stationPosition, model, pierceDepth, phase);
					Info info = null;
					if (infoList.size() > 0) {
						info = infoList.get(0);
						Location enterPoint = info.getEnterPoint();
						Location leavePoint = info.getLeavePoint();
						if (eventClusterPath != null)
							lines.add(String.format("%.2f %.2f %.2f %.2f cluster%d"
								, enterPoint.getLatitude()
								, enterPoint.getLongitude()
								, leavePoint.getLatitude()
								, leavePoint.getLongitude()
								, eventClusterMap.get(headerData.getGlobalCMTID())
								));
						else
							lines.add(String.format("%.2f %.2f %.2f %.2f"
									, enterPoint.getLatitude()
									, enterPoint.getLongitude()
									, leavePoint.getLatitude()
									, leavePoint.getLongitude()
									));
					}
				});
		
		Path outpath = workPath.resolve("raypathInside.inf");
		Files.write(outpath, lines);
	}
	
	private void outputRaypathInside_divide() throws IOException {
		List<String> lines_western = new ArrayList<>();
		List<String> lines_central = new ArrayList<>();
		List<String> lines_eastern = new ArrayList<>();
		
		Utilities.eventFolderSet(workPath).stream().flatMap(eventDir -> {
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
				.forEach(headerData -> {
					Location eventLocation = headerData.getEventLocation();
					HorizontalPosition stationPosition = headerData.getStation().getPosition();
					List<Info> infoList = TauPPierceReader.getPierceInfo(eventLocation, stationPosition, "prem", Phase.ScS);
					Info info = null;
					if (infoList.size() > 0) {
						info = TauPPierceReader.getPierceInfo(eventLocation, stationPosition, "prem", Phase.ScS)
						.get(0);
						Location enterDpp = info.getEnterPoint();
						Location leaveDpp = info.getLeavePoint();
						if (stationPosition.getLongitude() >= -130 && stationPosition.getLongitude() <= -110)
							lines_western.add(String.format("%.2f %.2f %.2f %.2f"
								, enterDpp.getLatitude()
								, enterDpp.getLongitude()
								, leaveDpp.getLatitude()
								, leaveDpp.getLongitude()
								));
						else if (stationPosition.getLongitude() > -110 && stationPosition.getLongitude() <= -90)
							lines_central.add(String.format("%.2f %.2f %.2f %.2f"
									, enterDpp.getLatitude()
									, enterDpp.getLongitude()
									, leaveDpp.getLatitude()
									, leaveDpp.getLongitude()
									));
						else if (stationPosition.getLongitude() > -90 && stationPosition.getLongitude() <= -70)
							lines_eastern.add(String.format("%.2f %.2f %.2f %.2f"
									, enterDpp.getLatitude()
									, enterDpp.getLongitude()
									, leaveDpp.getLatitude()
									, leaveDpp.getLongitude()
									));
					}
				});
		
		Path path_western = workPath.resolve("raypathInside_western.inf");
		Path path_central = workPath.resolve("raypathInside_central.inf");
		Path path_eastern = workPath.resolve("raypathInside_eastern.inf");
		Files.write(path_western, lines_western);
		Files.write(path_central, lines_central);
		Files.write(path_eastern, lines_eastern);
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
		if (drawsPathMode == 1) {
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
