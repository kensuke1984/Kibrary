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
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * 作業フォルダ下のイベント群に対して、Stationファイルに対するDSM(tipsv, tish)のinformation fileを作る
 * 
 * @version 0.2.2
 * @author Kensuke Konishi
 */
public class SyntheticDSMInformationMaker_forStationFile implements Operation {

	private SyntheticDSMInformationMaker_forStationFile(Properties property) {
		this.property = (Properties) property.clone();
		set();
	}

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths
				.get(SyntheticDSMInformationMaker_forStationFile.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan SyntheticDSMInformationFileMaker");
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("##Path of a work folder (.)");
			pw.println("#workPath");
			pw.println("##Staion file path");
			pw.println("#stationPath");
			pw.println("##header for names of information files, header_[psv, sh].inf, (PREM)");
			pw.println("#header");
			pw.println("##Path of a structure file you want to use. ()");
			pw.println("#structureFile");
			pw.println("##tlen must be a power of 2 over 10 (3276.8)");
			pw.println("#tlen");
			pw.println("##np must be a power of 2 (1024)");
			pw.println("#np");
		}
		System.err.println(outPath + " is created.");
	}

	private Properties property;

	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("stationPath"))
			property.setProperty("stationPath", "station.inf");
		if (!property.containsKey("tlen"))
			property.setProperty("tlen", "3276.8");
		if (!property.containsKey("np"))
			property.setProperty("np", "1024");
		if (!property.containsKey("header"))
			property.setProperty("header", "PREM");
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
		stationPath = Paths.get(property.getProperty("stationPath"));
		np = Integer.parseInt(property.getProperty("np"));
		tlen = Double.parseDouble(property.getProperty("tlen"));
		header = property.getProperty("header");
		if (property.containsKey("structureFile"))
			structurePath = Paths.get(property.getProperty("structureFile"));
	}

	/**
	 * Number of steps in frequency domain.
	 * It must be a power of 2.
	 */
	private int np;

	/**
	 * Time length [s].
	 * It must be a power of 2 divided by 10.(2<sup>n</sup>/10)
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
	 * stationPath
	 */
	private Path stationPath;

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 *             if any
	 */
	public static void main(String[] args) throws Exception {
		Properties property = new Properties();
		if (args.length == 0)
			property.load(Files.newBufferedReader(Operation.findPath()));
		else if (args.length == 1)
			property.load(Files.newBufferedReader(Paths.get(args[0])));
		else
			throw new IllegalArgumentException("too many arguments. It should be 0 or 1(property file name)");

		SyntheticDSMInformationMaker_forStationFile sdif = new SyntheticDSMInformationMaker_forStationFile(property);
		sdif.run();
	}

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

	@Override
	public void run() throws Exception {
		Set<EventFolder> eventDirs = Utilities.eventFolderSet(workPath);
		PolynomialStructure ps = structurePath == null ? PolynomialStructure.PREM
				: new PolynomialStructure(structurePath);

		Path outPath = workPath.resolve("synthetic" + Utilities.getTemporaryString());
		Files.createDirectories(outPath);
		for (EventFolder eventDir : eventDirs) {
			try {
				Set<Station> stations = StationInformationFile.read(stationPath);
				if (stations.isEmpty())
					continue;
				int numberOfStation = (int) stations.stream().map(Station::getName).count();
				if (numberOfStation != stations.size())
					System.err.println("!Caution there are stations with the same name and different positions in "
							+ eventDir);
				Path eventOut = outPath.resolve(eventDir.toString());
				SyntheticDSMInfo info = new SyntheticDSMInfo(ps, eventDir.getGlobalCMTID().getEvent(), stations, header, tlen, np);
				Files.createDirectories(eventOut.resolve(header));
				info.writePSV(eventOut.resolve(header + "_PSV.inf"));
				info.writeSH(eventOut.resolve(header + "_SH.inf"));
			} catch (Exception e) {
				System.err.println("Error on " + eventDir);
				e.printStackTrace();
			}
		}
	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}
}
