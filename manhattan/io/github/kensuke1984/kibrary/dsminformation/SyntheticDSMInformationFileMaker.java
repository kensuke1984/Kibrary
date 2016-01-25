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
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * 作業フォルダ下のイベント群に対してDSM(tipsv, tish)のinformation fileを作る
 * 
 * @version 0.2
 * 
 * @author kensuke
 * 
 */
public class SyntheticDSMInformationFileMaker implements Operation {

	private SyntheticDSMInformationFileMaker(Properties property) {
		this.property = (Properties) property.clone();
		set();
	}

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths
				.get(SyntheticDSMInformationFileMaker.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("##Path of a work folder (.)");
			pw.println("#workPath");
			pw.println("##header for names of information files, header_[psv, sh].inf, (PREM)");
			pw.println("#header");
			pw.println("##Path of a structure file you want to use. ()");
			pw.println("#structureFile");
			pw.println("##tlen must be a power of 2 over 10 (3276.8)");
			pw.println("#tlen");
			pw.println("##np must be a power of 2 (1024)");
			pw.println("#np");
		}
		System.out.println(outPath + " is created.");
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
		np = Integer.parseInt(property.getProperty("np"));
		tlen = Double.parseDouble(property.getProperty("tlen"));
		header = property.getProperty("header");

		if (property.containsKey("structureFile"))
			structurePath = Paths.get(property.getProperty("structureFile"));

	}

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

		SyntheticDSMInformationFileMaker sdif = new SyntheticDSMInformationFileMaker(property);
		// System.exit(0);
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
				Set<Station> stations = eventDir.sacFileSet(sfn -> !sfn.isOBS()).stream()
						.filter(name -> components.contains(name.getComponent())).map(name -> {
							try {
								return name.readHeader();
							} catch (Exception e2) {
								return null;
							}
						}).filter(Objects::nonNull).map(Station::of).collect(Collectors.toSet());
				if (stations.isEmpty())
					continue;
				int numberOfStation = (int) stations.stream().map(s -> s.getStationName()).count();
				if (numberOfStation != stations.size())
					System.err.println("!Caution there are stations with the same name and different positions in "
							+ eventDir.getGlobalCMTID());
				Path eventOut = outPath.resolve(eventDir.toString());
				SyntheticDSMInfo info = new SyntheticDSMInfo(ps, eventDir.getGlobalCMTID(), stations, header, tlen, np);
				Files.createDirectories(eventOut.resolve(header));
				info.writePSV(eventOut.resolve(header + "_PSV.inf"));
				info.writeSH(eventOut.resolve(header + "_SH.inf"));
			} catch (Exception e) {
				System.err.println("Error on " + eventDir);
				e.printStackTrace();
			}
		}

	}
}
