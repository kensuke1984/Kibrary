package io.github.kensuke1984.kibrary.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;

/**
 * This class makes information files for <br>
 * SHBP SHFP PSVBP PSVFP
 * 
 * TODO information of eliminated stations and events
 * 
 * @version 0.2.1.1
 * 
 * @author Kensuke Konishi
 * 
 */
public class InformationFileMaker implements Operation {
	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(InformationFileMaker.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan InformationFileMaker");
			pw.println("##Path of a working folder (.)");
			pw.println("#workPath");
			pw.println("##Path of an information file for locations of perturbation point, must be set");
			pw.println("#locationsPath pointLocations.inf");
			pw.println("##Path of a station information file, must be set");
			pw.println("#stationInformationPath station.inf");
			pw.println("##header for names of information files 'header'_[PSV, SH].inf (PREM)");
			pw.println("#header");
			pw.println("##int np must be a power of 2, must be set");
			pw.println("#np 1024");
			pw.println("##double tlen must be a power of 2/10, must be set");
			pw.println("#tlen 3276.8");
			pw.println("##polynomial structure file (can be blank)");
			pw.println("##if so or it doesn't exist model is an initial PREM");
			pw.println("#structureFile ");
		}
		System.err.println(outPath + " is created.");
	}

	public InformationFileMaker(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
	}

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
	 * np default: 1024
	 */
	protected int np;

	/**
	 * tlen default:3276.8
	 */
	protected double tlen;

	/**
	 * information file of locations of pertubation points.
	 */
	protected Path locationsPath;

	/**
	 * a structure to be used the default is PREM.
	 */
	protected PolynomialStructure ps;

	/**
	 * Information file name is header_[psv,sh].inf
	 */
	protected String header;

	protected Path stationInformationPath;

	/**
	 * work folder
	 */
	private Path workPath;

	private void set() throws IOException {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));

		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");

		locationsPath = getPath("locationsPath");

		stationInformationPath = getPath("stationInformationPath");
		// str = reader.getValues("partialTypes");
		np = Integer.parseInt(property.getProperty("np"));
		tlen = Double.parseDouble(property.getProperty("tlen"));
		header = property.getProperty("header");

		if (property.containsKey("structureFile")) {
			Path psPath = getPath("structureFile");
			ps = psPath == null ? PolynomialStructure.PREM : new PolynomialStructure(psPath);
		}
	}

	private Properties property;

	/**
	 * locations of perturbation points
	 * 
	 */
	private HorizontalPosition[] perturbationPointPositions;

	/**
	 * Radii of perturbation points default values are double[]{3505, 3555,
	 * 3605, 3655, 3705, 3755, 3805, 3855} Sorted. No duplication.
	 */
	private double[] perturbationR;

	private Path outputPath;

	/**
	 * 
	 * With static members, make two information files for back- and forward
	 * propagation.
	 * 
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 *             if any
	 */
	public static void main(String[] args) throws Exception {
		InformationFileMaker ifm = new InformationFileMaker(Property.parse(args));
		long start = System.nanoTime();
		System.err.println(InformationFileMaker.class.getName() + " is going.");
		ifm.run();
		System.err.println(InformationFileMaker.class.getName() + " finished in "
				+ Utilities.toTimeString(System.nanoTime() - start));
	}

	/**
	 * Reads a file describing locations
	 * 
	 * The file should be as below: <br>
	 * r1 r2 r3..... rn (Radii cannot have duplicate values, they will be
	 * sorted)<br>
	 * lat1 lon1<br>
	 * lat2 lon2<br>
	 * .<br>
	 * .<br>
	 * .<br>
	 * latm lonm
	 */
	private void readParameterPointInformation() throws IOException {
		InformationFileReader reader = new InformationFileReader(locationsPath);
		perturbationR = Arrays.stream(reader.next().split("\\s+")).mapToDouble(Double::parseDouble).sorted().distinct()
				.toArray();

		List<HorizontalPosition> positionList = new ArrayList<>();
		String line;
		while ((line = reader.next()) != null) {
			String[] part = line.split("\\s+");
			HorizontalPosition position = new HorizontalPosition(Double.parseDouble(part[0]),
					Double.parseDouble(part[1]));
			positionList.add(position);
		}
		perturbationPointPositions = positionList.toArray(new HorizontalPosition[0]);
	}

	/**
	 * horizontalPoint.inf と perturbationPointのインフォメーションファイルを作る
	 */
	private void createPointInformationFile() throws IOException {
		Path horizontalPointPath = outputPath.resolve("horizontalPoint.inf");
		Path perturbationPointPath = outputPath.resolve("perturbationPoint.inf");
		try (PrintWriter hpw = new PrintWriter(Files.newBufferedWriter(horizontalPointPath));
				PrintWriter ppw = new PrintWriter(Files.newBufferedWriter(perturbationPointPath))) {
			int figure = String.valueOf(perturbationPointPositions.length).length();
			for (int i = 0; i < perturbationPointPositions.length; i++) {
				hpw.println("XY" + String.format("%0" + figure + "d", i + 1) + " " + perturbationPointPositions[i]);
				for (int j = 0; j < perturbationR.length; j++)
					ppw.println(perturbationPointPositions[i] + " " + perturbationR[j]);
			}
		}

	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

	@Override
	public void run() throws Exception {
		// ///
		if (!Files.exists(workPath))
			throw new NoSuchFileException(workPath.toString());
		if (!Files.exists(stationInformationPath))
			throw new NoSuchFileException(stationInformationPath.toString());
		outputPath = workPath.resolve("threedPartial" + Utilities.getTemporaryString());
		Files.createDirectories(outputPath);

		Path bpPath = outputPath.resolve("BPinfo");
		Path fpPath = outputPath.resolve("FPinfo");
		// String model = model;
		// double[] perturbationPointR = perturbationPointR;
		// horizontalPointFile = new File(workDir, "horizontalPoint.inf");
		// perturbationPointFile = new File(workDir,"perturbationPoint.inf");
		readParameterPointInformation();
		createPointInformationFile();
		// System.exit(0);

		//
		Set<EventFolder> eventDirs = Utilities.eventFolderSet(workPath);

		// reading station information
		Set<Station> stationSet = StationInformationFile.read(stationInformationPath);

		// System.exit(0);
		// //////////////////////////////////////
		System.out.println("making information files for the events(fp)");
		for (EventFolder ed : eventDirs) {
			// System.out.println(ed);
			GlobalCMTData ev = ed.getGlobalCMTID().getEvent();
			FPinfo fp = new FPinfo(ev, header, ps, tlen, np);
			fp.setPerturbationPointR(perturbationR);
			fp.setPerturbationPoint(perturbationPointPositions);
			// File parentDir = new File(outputDir, "FPinfo");
			Path infPath = fpPath.resolve(ev.toString());
			// System.out.println(infDir);
			// infDir.mkdir();
			Files.createDirectories(infPath.resolve(header));
			fp.writeSHFP(infPath.resolve(header + "_SH.inf"));
			fp.writePSVFP(infPath.resolve(header + "_PSV.inf"));
			//
		}

		System.out.println("making information files for the stations(bp)");
		for (Station station : stationSet) {
			String str = station.getStationName();
			// System.out.println(str);
			BPinfo bp = new BPinfo(station, header, ps, tlen, np);
			bp.setPerturbationPointR(perturbationR);
			bp.setPerturbationPoint(perturbationPointPositions);

			Path infPath = bpPath.resolve("0000" + str);
			// infDir.mkdir();
			// System.out.println(infDir.getPath()+" was made");
			Files.createDirectories(infPath.resolve(header));
			bp.writeSHBP(infPath.resolve(header + "_SH.inf"));
			bp.writePSVBP(infPath.resolve(header + "_PSV.inf"));

		}

		// TODO
		if (fpPath.toFile().delete() && bpPath.toFile().delete()) {
			Files.delete(outputPath.resolve("horizontalPoint.inf"));
			Files.delete(outputPath.resolve("perturbationPoint.inf"));
			Files.delete(outputPath);
		} else {
			// FileUtils.moveFileToDirectory(getParameterPath().toFile(),
			// outputPath.toFile(), false);
			FileUtils.copyFileToDirectory(locationsPath.toFile(), outputPath.toFile(), false);
		}
	}

}
