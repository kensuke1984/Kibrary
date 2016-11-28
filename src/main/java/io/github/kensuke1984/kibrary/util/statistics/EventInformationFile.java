package io.github.kensuke1984.kibrary.util.statistics;

import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.io.input.CloseShieldInputStream;

public class EventInformationFile {
	
	public static void main (String[] args) throws IOException {
		if (0 < args.length) {
			String path = args[0];
			if (!path.startsWith("/"))
				path = System.getProperty("user.dir") + "/" + path;
			Path f = Paths.get(path);
			if (Files.exists(f) && Files.isDirectory(f))
				createEventInformationFile(f);
			else
				System.out.println(f + " does not exist or is not a directory.");
		} else {
			Path workPath;
			String path = "";
			do {
				try {
					path = JOptionPane.showInputDialog("Working folder?", path);
				} catch (Exception e) {
					System.out.println("Working folder?");
					try (BufferedReader br = new BufferedReader(
							new InputStreamReader(new CloseShieldInputStream(System.in)))) {
						path = br.readLine().trim();
						if (!path.startsWith("/"))
							path = System.getProperty("user.dir") + "/" + path;
					} catch (Exception e2) {
						e2.printStackTrace();
						throw new RuntimeException();
					}
				}
				if (path == null || path.isEmpty())
					return;
				workPath = Paths.get(path);
				if (!Files.isDirectory(workPath))
					continue;
				// System.out.println(tmp.getAbsolutePath());
			} while (!Files.exists(workPath) || !Files.isDirectory(workPath));
			createEventInformationFile(workPath);
		}
	}
	
	private static void createEventInformationFile(Path workPath, OpenOption... options) throws IOException {
		Path outpath = workPath.resolve("event" + Utilities.getTemporaryString() + ".inf");
		Set<EventFolder> eventFolderSet = Utilities.eventFolderSet(workPath);
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, options))) {
			pw.write("#GCMTID lat lon radius #transverse #vertical #radial Mw noiseToAmplitudeBeforeS noiseRatioVariance");
			for (EventFolder folder : eventFolderSet) {
				GlobalCMTID id = folder.getGlobalCMTID();
				Location location = id.getEvent().getCmtLocation();
				double Mw = id.getEvent().getCmt().getMw();
				Set<SACFileName> sacnames = folder.sacFileSet();
				int nT = (int) sacnames.stream().filter(sacname -> sacname.getComponent().equals(SACComponent.T)).count();
				int nZ = (int) sacnames.stream().filter(sacname -> sacname.getComponent().equals(SACComponent.Z)).count();
				int nR = (int) sacnames.stream().filter(sacname -> sacname.getComponent().equals(SACComponent.R)).count();
				double[] noiseToAmplitudeRatio = noiseToAmplitudeRatio(sacnames);
				pw.write(id + " " + location + " " + nT + " " + nZ + " " + nR + " " + Mw + " " + noiseToAmplitudeRatio[0] + noiseToAmplitudeRatio[1]);
			}
		}
	}
	
	private static double[] noiseToAmplitudeRatio(Set<SACFileName> sacnames) {
		double[] noise = new double[2];
		
		return noise;
	}
}
