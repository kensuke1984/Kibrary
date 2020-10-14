package io.github.kensuke1984.kibrary.inversion.addons;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

public class MakeHorizontalMesh {

	public static void main(String[] args) throws IOException {
		double[] bounds = new double[4];
		double dl = 0.;
		Path workdir = Paths.get(".");
		
		if (args.length == 2) {
			Path stationFile = Paths.get(args[0]);
			dl = Double.parseDouble(args[1]);
		
			Set<Station> stations = StationInformationFile.read(stationFile);
		
			Set<GlobalCMTID> events = Utilities.eventFolderSet(workdir).stream().map(event -> event.getGlobalCMTID()).collect(Collectors.toSet());
		
			double lonborder = 10.;
			double latborder = 0;
			bounds = getBounds(stations, events, lonborder, latborder);
		}
		else {
			double latmin = Double.parseDouble(args[0]);
			double latmax = Double.parseDouble(args[1]);
			double lonmin = Double.parseDouble(args[2]);
			double lonmax = Double.parseDouble(args[3]);
			dl = Double.parseDouble(args[4]);
			bounds[0] = latmin;
			bounds[1] = latmax;
			bounds[2] = lonmin;
			bounds[3] = lonmax;
		}
		
		int nlat = (int) ((bounds[1] - bounds[0]) / dl) + 1;
		int nlon = (int) ((bounds[3] - bounds[2]) / dl) + 1;
		
		System.out.println(nlat*nlon + " points");
		
		Path outpath = workdir.resolve("horizontalPositions.inf");
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (int i = 0; i < nlat; i++) {
			for (int j = 0; j < nlon; j++) {
				HorizontalPosition position = new HorizontalPosition(bounds[0] + i * dl, bounds[2] + j * dl);
				pw.println(position);
			}
		}
		pw.close();
	}
	
	private static double[] getBounds(Set<Station> stations, Set<GlobalCMTID> events, double lonborder, double latborder) {
		double[] bounds = new double[] {Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_VALUE};
		Stream.concat(stations.stream().map(s -> s.getPosition()), events.stream().map(e -> e.getEvent().getCmtLocation().toHorizontalPosition()))
			.forEach(pos -> {
				double lat = pos.getLatitude();
				double lon = pos.getLongitude();
				if (lat < bounds[0])
					bounds[0] = lat;
				if (lat > bounds[1])
					bounds[1] = lat;
				if (lon < bounds[2])
					bounds[2] = lon;
				if (lon > bounds[3])
					bounds[3] = lon;
			});
		
		bounds[0] -= latborder;
		bounds[1] += latborder;
		bounds[2] -= lonborder;
		bounds[3] += lonborder;
		
		return bounds;
	}

}
