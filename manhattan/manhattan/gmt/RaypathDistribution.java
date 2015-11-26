/**
 * 
 */
package manhattan.gmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import filehandling.sac.SACFileName;
import filehandling.sac.SACHeaderEnum;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.Location;
import manhattan.template.Station;
import manhattan.template.Utilities;

/**
 * This is like pathDrawer.pl The pathDrawer compute raypath coordinate. But
 * this class uses raypath by GMT.
 * 
 * event and station are necessary.
 * 
 * <b>Assume that there are no stations with the same name but different networks in an event</b>
 * 
 * 
 * @author kensuke
 * @version 0.0.4
 * 
 */
final class RaypathDistribution extends parameter.RaypathDistribution {

	private Set<GlobalCMTID> ids;

	private RaypathDistribution(Path parameterPath) throws IOException {
		super(parameterPath);
	}

	/**
	 * @param args
	 *            [parameter file name]
	 */
	public static void main(String[] args) throws IOException {
		RaypathDistribution rd = null;
		if (0 < args.length) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			rd = new RaypathDistribution(parameterPath);
		} else
			rd = new RaypathDistribution(null);

		rd.run();

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

	private void run() {
		setName();
		try {
			ids = Utilities.globalCMTIDSet(workPath);
			outputEvent();
			outputStation();
			if (drawsPath)
				outputRaypath();
			outputGMT();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
								&& tw.getStation().getStationName().equals(name.getStationName()));
	}

	private void outputRaypath() throws IOException {
		List<String> lines = Utilities.eventFolderSet(workPath).stream().flatMap(eventDir -> {
			try {
				return eventDir.sacFileSet(sfn->!sfn.isOBS()).stream();
			} catch (Exception e) {
				return Stream.empty();
			}
		}).filter(name -> componentSet.contains(name.getComponent())).filter(this::inTimeWindow).map(name -> {
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

		double minimumEventLatitude = ids.stream().map(id -> id.getEvent())
				.mapToDouble(e -> e.getCmtLocation().getLatitude()).min().getAsDouble();

		double maximumEventLatitude = ids.stream().map(id -> id.getEvent())
				.mapToDouble(e -> e.getCmtLocation().getLatitude()).max().getAsDouble();

		double minimumEventLongitude = ids.stream().map(id -> id.getEvent())
				.mapToDouble(e -> e.getCmtLocation().getLongitude()).map(d -> 0 <= d ? d : d + 360).min().getAsDouble();

		double maximumEventLongitude = ids.stream().map(id -> id.getEvent())
				.mapToDouble(e -> e.getCmtLocation().getLongitude()).map(d -> 0 <= d ? d : d + 360).max().getAsDouble();

		double minimumStationLatitude = stationSet.stream().map(station -> station.getPosition())
				.mapToDouble(pos -> pos.getLatitude()).min().orElse(minimumEventLatitude);

		double maximumStationLatitude = stationSet.stream().map(station -> station.getPosition())
				.mapToDouble(pos -> pos.getLatitude()).max().orElse(maximumEventLatitude);

		double minimumStationLongitude = stationSet.stream().map(station -> station.getPosition())
				.mapToDouble(pos -> pos.getLongitude()).map(d -> 0 <= d ? d : d + 360).min()
				.orElse(minimumEventLongitude);

		double maximumStationLongitude = stationSet.stream().map(station -> station.getPosition())
				.mapToDouble(pos -> pos.getLongitude()).map(d -> 0 <= d ? d : d + 360).max()
				.orElse(maximumEventLongitude);

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
		// System.out.println(minLatitude + " " + maxLatitude + " " +
		// minLongitude + " " + maxLongitude);
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
		gmtCMD.add("#eps2eps $psname $psname.eps");
		gmtCMD.add("#mv $psname.eps $psname");
		Files.write(gmtPath, gmtCMD);
		gmtPath.toFile().setExecutable(true);
		// for (String s : gmtCMD)
		// System.out.println(s);
	}
}
