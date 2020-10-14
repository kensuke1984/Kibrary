package io.github.kensuke1984.kibrary.util.statistics;

import io.github.kensuke1984.kibrary.external.gmt.CrossSectionLine;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;

public class Rose {

	public static void main(String[] args) throws IOException {
		Path waveformIDPath = Paths.get(args[0]);
		
		BasicID[] ids = BasicIDFile.read(waveformIDPath);
		
		double deltaThetaRad = Math.toRadians(0.2);
		double dL = 2.;
		double dAz = 10.;
		
		int nLat = (int) (180 / dL);
		int nLon = (int) (360 / dL);
		int nAz = (int) (360 / dAz);
		
		double[][][] gridAzimuthHist = new double[nLat][nLon][nAz];
		
		for (BasicID id : ids) {
			CrossSectionLine line = new CrossSectionLine(id.getGlobalCMTID().getEvent().getCmtLocation().toHorizontalPosition(), id.getStation().getPosition(), deltaThetaRad);
			Set<HorizontalPosition> indices = Stream.of(line.getPositions()).map(pos -> {
					int ilat = (int) ((pos.getLatitude() + 90) / dL);
					int ilon = (int) ((pos.getLongitude() + 180) / dL);
					return new HorizontalPosition(ilat, ilon);
				}
			).collect(Collectors.toSet());
			
			int iAz = (int) (Math.toDegrees(line.getAzimuth()) / dAz);
			
			for (HorizontalPosition ipos : indices)
				gridAzimuthHist[(int) ipos.getLatitude()][(int) ipos.getLongitude()][iAz] += 1;
		}
		
		int i = 0;
		for (int ilat = 0; ilat < nLat; ilat++) {
			for (int ilon = 0; ilon < nLon; ilon++) {
				if (new ArrayRealVector(gridAzimuthHist[ilat][ilon]).getLInfNorm() > 0) {
					Path outpath = Paths.get("rose" + i + ".dat");
					i++;
					PrintWriter pw = new PrintWriter(outpath.toFile());
					double lat = -90 + ilat * dL;
					double lon = ilon * dL;
					pw.println(lon + " " + lat);
					for (int iaz = 0; iaz < nAz; iaz++) {
						double az = iaz * dAz;
						pw.println(az + " " + gridAzimuthHist[ilat][ilon][iaz]);
					}
					pw.close();
				}
			}
		}
	
	}

}
