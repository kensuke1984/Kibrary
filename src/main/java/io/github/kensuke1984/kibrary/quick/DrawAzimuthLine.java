package io.github.kensuke1984.kibrary.quick;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import edu.sc.seis.TauP.SphericalCoords;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.addons.EventCluster;

public class DrawAzimuthLine {

	public static void main(String[] args) throws IOException {
		Path clusterFilePath = Paths.get(args[0]);
		List<EventCluster> clusters = EventCluster.readClusterFile(clusterFilePath);
		
		clusters.stream().map(c -> c.getIndex()).distinct().collect(Collectors.toList())
			.stream().map(i -> clusters.stream().filter(c -> c.getIndex() == i.intValue()).findFirst().get())
			.forEach(c -> {
				try (PrintWriter pw = new PrintWriter(Paths.get("azimuthSlices_cl" + c.getIndex() + ".inf").toFile())) {
					HorizontalPosition centerPos = c.getCenterPosition();
					for (Double azimuth : c.getAzimuthSlices()) {
						double lat = SphericalCoords.latFor( centerPos.getLatitude(), centerPos.getLongitude(), 110., azimuth);
						double lon = SphericalCoords.lonFor( centerPos.getLatitude(), centerPos.getLongitude(), 110., azimuth);
						HorizontalPosition pos = new HorizontalPosition(lat, lon);
						pw.println(centerPos.getLongitude() + " " + centerPos.getLatitude());
						pw.println(pos.getLongitude() + " " + pos.getLatitude());
						pw.println(">");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		
		
	}

}
