package io.github.kensuke1984.kibrary.axiSEM;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.FastMath;

import io.github.kensuke1984.kibrary.datacorrection.MomentTensor;
import io.github.kensuke1984.kibrary.math.geometry.XYZ;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

/**
 * @author anpan
 * @version 0.1
 */
public class MakeRunFolders {
	public static void main(String[] args) {
		try {
			// test 1 rotation matrix
			/*
			RealVector axis = new ArrayRealVector(new double[] {0, 0, 1});
			RealVector x = new ArrayRealVector(new double[] {1, 0, 0});
			RealMatrix rotationMatrix = rotationMatrix(axis, Math.PI/4.);
			System.out.println(rotationMatrix.operate(x));
			System.exit(0);
			*/
			
			// test 2 rotate locations
			/*
			Location source = new Location(30, 30, 6271);
			Location receiver = new Location(60, 10, 6371);
			RealVector axis = getRotationAxisToNorthPole(source);
			double angle = source.getTheta();
			Location rotatedSource = rotateLocation(source, axis, angle);
			Location rotatedReceiver = rotateLocation(receiver, axis, angle);
			System.out.println(rotatedSource + "\n" + rotatedReceiver);
			System.out.println(source.getAzimuth(receiver) * 180 / Math.PI 
					+ " " + rotatedSource.getAzimuth(rotatedReceiver) * 180 / Math.PI);
			System.out.println(source.getEpicentralDistance(receiver) * 180 / Math.PI 
					+ " " + rotatedSource.getEpicentralDistance(rotatedReceiver) * 180 / Math.PI);
			System.exit(0);
			*/
			
			Path currentWorkingDir = Paths.get(System.getProperty("user.dir"));
			Path syntheticFolder = currentWorkingDir.resolve("synthetic" + Utilities.getTemporaryString());
			Path axiSEMFolder = Paths.get(args[0]);
			Path axiSEMSolverFolder = axiSEMFolder.resolve("SOLVER");
			
			if (!Files.isDirectory(axiSEMSolverFolder))
				throw new FileNotFoundException(axiSEMSolverFolder.toString());
			
			Set<EventFolder> eventFolderSet = Utilities.eventFolderSet(currentWorkingDir);
			
			Files.createDirectories(syntheticFolder);
			
			for (EventFolder eventFolder : eventFolderSet) {
				Path dirPath = syntheticFolder.resolve(eventFolder.getName());
				Path localDirPath = dirPath.resolve("SOLVER");
				try {
					Files.createDirectories(dirPath);
					FileUtils.copyDirectory(axiSEMSolverFolder.toFile(), localDirPath.toFile());
					
					Path eventFile = localDirPath.resolve("CMTSOLUTION");
					GlobalCMTID id = eventFolder.getGlobalCMTID();
					GlobalCMTData idData = id.getEvent();
					MomentTensor mt = idData.getCmt();
					double pow = Math.pow(10, mt.getMtExp());
					String s = String.format("PDE...%nevent name: %s%ntime shift: %.4f%nhalf duration: "
							+ "%.4f%nlatitude: %.4f%nlongitude: %.4f%ndepth: "
							+ "%.4f%nMrr: %.6e%nMtt: %.6e%nMpp: %.6e%nMrt: %.6e%nMrp: %.6e%nMtp: %.6e"
								, id.toString()
								, 0.
								, idData.getHalfDuration()
//								, idData.getCmtLocation().getLatitude()
//								, idData.getCmtLocation().getLongitude()
								, 90.
								, 0.
								, Earth.EARTH_RADIUS - idData.getCmtLocation().getR()
								, mt.getMrr() * pow
								, mt.getMtt() * pow
								, mt.getMpp() * pow
								, mt.getMrt() * pow
								, mt.getMrp() * pow
								, mt.getMtp() * pow
						);
					Files.write(eventFile, s.getBytes());
					
					Set<Station> stationSet = new HashSet<>();
					eventFolder.sacFileSet().parallelStream().forEach(sac -> {
						try {
							stationSet.add(sac.read().getStation());
						} catch (IOException e) {
							System.err.format("IOException: %s%n", e);
						}
					});
					Path stationFile = localDirPath.resolve("STATIONS");
					RealVector axis = getRotationAxisToNorthPole(idData.getCmtLocation());
					double angle = idData.getCmtLocation().getTheta();
					try (BufferedWriter writer = Files.newBufferedWriter(stationFile)) {
						for (Station sta : stationSet) {
							Location rotatedStation 
								= rotateLocation(sta.getPosition().toLocation(Earth.EARTH_RADIUS), axis, angle);
							writer.write(String.format("%s %s %.3f %.3f 0.0 0.0%n"
								, sta.getName()
								, sta.getNetwork()
								, rotatedStation.getLatitude()
								, rotatedStation.getLongitude())
							);
						}
					} catch (IOException e) {
						System.err.format("IOException: %s%n", e);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static RealMatrix rotationMatrix(RealVector axis, double angle) {
		RealMatrix m = new Array2DRowRealMatrix(3, 3);
		RealVector u = axis.unitVector();
		double cost = FastMath.cos(angle);
		double sint = FastMath.sin(angle);
		double ux = u.getEntry(0);
		double uy = u.getEntry(1);
		double uz = u.getEntry(2);
		m.setColumn(0, new double[] {cost + ux*ux*(1 - cost)
				,uy*ux*(1-cost) + uz*sint
				,uz*ux*(1-cost) - uy*sint});
		m.setColumn(1, new double[] {ux*uy*(1-cost) - uz*sint
				,cost + uy*uy*(1-cost)
				,uz*uy*(1-cost) + ux*sint});
		m.setColumn(2, new double[] {ux*uz*(1-cost) + uy*sint
				,uy*uz*(1-cost) - ux*sint
				,cost + uz*uz*(1-cost)});
		return m;
	}
	
	private static RealVector getRotationAxisToNorthPole(Location location) {
		double x = location.toXYZ().getX();
		double y = location.toXYZ().getY();
		double n = Math.sqrt(x*x + y*y);
		return new ArrayRealVector(new double[] {y/n, -x/n, 0.}).unitVector();
	}
	
	private static Location rotateLocation(Location loc, RealVector axis, double angle) {
		RealMatrix rotationMatrix = rotationMatrix(axis, angle);
		RealVector sourceVector = loc.toXYZ().toRealVector();
		RealVector rotatedSourceVector = rotationMatrix.operate(sourceVector);
		Location rotatedSourceLocation = new XYZ(rotatedSourceVector.getEntry(0), rotatedSourceVector.getEntry(1), rotatedSourceVector.getEntry(2))
			.toLocation();
		return rotatedSourceLocation;
	}
}
