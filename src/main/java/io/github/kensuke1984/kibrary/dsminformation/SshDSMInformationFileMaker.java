package io.github.kensuke1984.kibrary.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * Information file for SSHSH
 * 
 * @version 0.1.2.1
 * 
 * @author Kensuke Konishi
 *
 */
public class SshDSMInformationFileMaker implements Operation {

	public SshDSMInformationFileMaker(Properties property) {
		this.property = (Properties) property.clone();
		set();
	}

	private Properties property;

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

	/**
	 * work folder
	 */
	private Path workPath;

	private void set() {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));

		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());
		np = Integer.parseInt(property.getProperty("np"));
		tlen = Double.parseDouble(property.getProperty("tlen"));
		header = property.getProperty("header");

		if (property.containsKey("structureFile"))
			structurePath = Paths.get(property.getProperty("structureFile"));
		perturbationR = Arrays.stream(property.getProperty("perturbationR").split("\\s+"))
				.mapToDouble(Double::parseDouble).toArray();
	}

	private double[] perturbationR;
	/**
	 * 周波数ステップ数 2の累乗でないといけない
	 */
	private int np;

	/**
	 * 時間空間での長さ ２の累乗の１０分の一でないといけない
	 */
	private double tlen;

	/**
	 * 観測波形を選択する成分
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

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws Exception
	 *             if any
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
		}
		System.err.println(outPath + " is created.");
	}

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

	@Override
	public void run() throws Exception {
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
			// Event e = eventinfo.getEvent(eventDir.getEventName());
			Set<Station> stations = eventDir.sacFileSet().stream()
					.filter(name -> name.isOBS() && useComponents.contains(name.getComponent())).map(name -> {
						try {
							return name.readHeader();
						} catch (Exception e2) {
							e2.printStackTrace();
							return null;
						}
					}).filter(Objects::nonNull).map(Station::of).collect(Collectors.toSet());
			if (stations.isEmpty())
				continue;
			int numberOfStation = (int) stations.stream().map(Station::getStationName).count();
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
