package io.github.kensuke1984.kibrary.dsminformation;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Information file for SSHSH
 *
 * @author Kensuke Konishi
 * @version 0.1.3
 */
public class SshDSMInformationFileMaker implements Operation {

	private Properties property;
	/**
	 * work folder
	 */
	private Path workPath;
	private double[] perturbationR;
	/**
	 * number of steps in frequency domain, must be a power of 2 (2<sup>n</sup>)
	 */
	private int np;
	/**
	 * [s] time length of data must be a power of 2 divided by 10 (2<sup>n</sup>/10)
	 */
	private double tlen;
	/**
	 * components to create an information file for
	 */
	private Set<SACComponent> components;
	/**
	 * Information file name is header_[psv,sh].inf (default:PREM)
	 */
	private String header;
	/**
	 * structure file instead of PREM
	 */
	private Path structurePath;
	private Path timewindowInformationPath;

	public SshDSMInformationFileMaker(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
	}
	
    /**
     * @param args [parameter file name]
     * @throws Exception if any
     */
	public static void main(String[] args) throws Exception {
		SshDSMInformationFileMaker sdif = new SshDSMInformationFileMaker(Property.parse(args));
		long start = System.nanoTime();
		System.err.println(SshDSMInformationFileMaker.class.getName() + " is going.");
		sdif.run();
		System.err.println(SshDSMInformationFileMaker.class.getName() + " finished in "
				+ Utilities.toTimeString(System.nanoTime() - start));
	}

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths
				.get(SshDSMInformationFileMaker.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan SshDSMInformationFileMaker");
			pw.println("##These properties for SshDSMInformationFileMaker");
			pw.println("##Path of a work folder (.)");
			pw.println("#workPath");
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("##header for names of information files, header_[psv, sh].inf, (PREM)");
			pw.println("#header");
			pw.println("##Path of a structure file you want to use. ()");
			pw.println("#structureFile");
			pw.println("##tlen must be a power of 2 over 10 (3276.8)");
			pw.println("#tlen");
			pw.println("##np must be a power of 2 (512)");
			pw.println("#np");
			pw.println("##Depths for computations, must be defined");
			pw.println("#perturbationR 3500 3600");
			pw.println("#timewindowInformationPath");
		}
		System.err.println(outPath + " is created.");
	}
	
	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("tlen"))
			property.setProperty("tlen", "3276.8");
		if (!property.containsKey("np"))
			property.setProperty("np", "512");
		if (!property.containsKey("header"))
			property.setProperty("header", "PREM");
		if (!property.containsKey("perturbationR") || property.getProperty("perturbationR").isEmpty())
			throw new RuntimeException("perturbationR must be defined.");
	}
	
	private void set() throws IOException {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));

		if (!Files.exists(workPath)) throw new NoSuchFileException("The workPath: " + workPath + " does not exist");
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());
		np = Integer.parseInt(property.getProperty("np"));
		tlen = Double.parseDouble(property.getProperty("tlen"));
		header = property.getProperty("header");

		if (property.containsKey("structureFile")) structurePath = Paths.get(property.getProperty("structureFile"));
		perturbationR = Arrays.stream(property.getProperty("perturbationR").split("\\s+"))
				.mapToDouble(Double::parseDouble).toArray();
		
		if (property.containsKey("timewindowInformationPath"))
			timewindowInformationPath = Paths.get(property.getProperty("timewindowInformationPath"));
		else
			timewindowInformationPath = null;
	}

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

	@Override
	public void run() throws Exception {
		Set<TimewindowInformation> tmpwindows = null;
		if (timewindowInformationPath != null)
			tmpwindows = TimewindowInformationFile.read(timewindowInformationPath);
		final Set<TimewindowInformation> timewindows = tmpwindows;
		
		Set<EventFolder> eventDirs = Utilities.eventFolderSet(workPath);
		PolynomialStructure ps = PolynomialStructure.PREM;
		if (structurePath.toString().trim().toUpperCase().equals("PREM")) {
			ps = PolynomialStructure.PREM;
		}
		else if (structurePath.toString().trim().toUpperCase().equals("AK135")) {
			ps = PolynomialStructure.AK135;
		}
		else if (structurePath.toString().trim().toUpperCase().equals("TBL50")) {
			ps = PolynomialStructure.TBL50;
		}
		else
			ps = new PolynomialStructure(structurePath);
		String temporaryString = Utilities.getTemporaryString();
		Path output = workPath.resolve("oneDPartial" + temporaryString);
		Files.createDirectories(output);
		Set<SACComponent> useComponents = components;
		for (EventFolder eventDir : eventDirs) {
			Set<Station> stations = eventDir.sacFileSet().stream()
					.filter(name -> name.isOBS() && useComponents.contains(name.getComponent())).map(name -> {
						try {
							return name.readHeader();
						} catch (Exception e2) {
							e2.printStackTrace();
							return null;
						}
					}).filter(Objects::nonNull).map(Station::of).collect(Collectors.toSet());
			
			//select stations in timewindows
			if (timewindowInformationPath != null) {
				stations.removeIf(sta -> timewindows.stream().filter(tw -> tw.getStation().equals(sta) 
						&& tw.getGlobalCMTID().equals(eventDir.getGlobalCMTID())
						&& useComponents.contains(tw.getComponent())).count() == 0);
			}
			
			if (stations.isEmpty())
				continue;
			int numberOfStation = (int) stations.stream().map(Station::getName).count();
			if (numberOfStation != stations.size())
				System.err.println("!Caution there are stations with the same name and different positions in "
						+ eventDir.getGlobalCMTID());
			SshDSMinfo info = new SshDSMinfo(ps, eventDir.getGlobalCMTID().getEvent(), stations, header, perturbationR,
					tlen, np);
			Path outEvent = output.resolve(eventDir.toString());
			Path modelPath = outEvent.resolve(header);
			Files.createDirectories(outEvent);
			Files.createDirectories(modelPath);
			info.writeTIPSV(outEvent.resolve("par5_" + header + "_PSV.inf"));
			info.writeTISH(outEvent.resolve("par5_" + header + "_SH.inf"));
			info.writeISOPSV(outEvent.resolve("par2_" + header + "_PSV.inf"));
			info.writeISOSH(outEvent.resolve("par2_" + header + "_SH.inf"));
		}
	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}
}
