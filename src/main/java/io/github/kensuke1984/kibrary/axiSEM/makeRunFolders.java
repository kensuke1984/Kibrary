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

import io.github.kensuke1984.kibrary.datacorrection.MomentTensor;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

/**
 * @author anpan
 * @version 0.1
 */
public class makeRunFolders {
	public static void main(String[] args) {
		try {
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
					
					Set<Station> stationSet = new HashSet<>();
					eventFolder.sacFileSet().parallelStream().forEach(sac -> {
						try {
							stationSet.add(sac.read().getStation());
						} catch (IOException e) {
							System.err.format("IOException: %s%n", e);
						}
					});
					Path stationFile = localDirPath.resolve("STATIONS");
					try (BufferedWriter writer = Files.newBufferedWriter(stationFile)) {
						for (Station sta : stationSet)
							writer.write(String.format("%s %s %.3f %.3f 0.0 0.0%n"
								, sta.getStationName()
								, sta.getNetwork()
								, sta.getPosition().getLatitude()
								, sta.getPosition().getLongitude())
							);
					} catch (IOException e) {
						System.err.format("IOException: %s%n", e);
					}
					
					Path eventFile = localDirPath.resolve("CMTSOLUTION");
					GlobalCMTID id = eventFolder.getGlobalCMTID();
					GlobalCMTData idData = id.getEvent();
					MomentTensor mt = idData.getCmt();
					double pow = Math.pow(10, mt.getMtExp());
					String s = String.format("event name: %s%ntime shift: %.4f%nhalf duration: "
							+ "%.4f%nlatitude: %.4f%nlongitude: %.4f%ndepth: "
							+ "%.4f%nMrr: %.6e%nMtt: %.6e%nMpp: %.6e%nMrt: %.6e%nMrp: %.6e%nMtp: %.6e"
								, id.toString()
								, 0.
								, idData.getHalfDuration()
								, idData.getCmtLocation().getLatitude()
								, idData.getCmtLocation().getLongitude()
								, Earth.EARTH_RADIUS - idData.getCmtLocation().getR()
								, mt.getMrr() * pow
								, mt.getMtt() * pow
								, mt.getMpp() * pow
								, mt.getMrt() * pow
								, mt.getMrp() * pow
								, mt.getMtp() * pow
						);
					Files.write(eventFile, s.getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
