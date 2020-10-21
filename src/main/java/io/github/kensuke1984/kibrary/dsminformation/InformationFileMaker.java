package io.github.kensuke1984.kibrary.dsminformation;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.datacorrection.MomentTensor;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTCatalog;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * This class makes information files for <br>
 * SHBP SHFP PSVBP PSVFP
 * <p>
 * TODO information of eliminated stations and events
 *
 * @author Kensuke Konishi
 * @version 0.2.2.1
 * @author anselme add content for catalog
 */
public class InformationFileMaker implements Operation {
	
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
	
	private boolean jointCMT;
	private boolean catalogue;
	/**
	 * epicentral distances for catalog
	 */
	private double thetamin;
	private double thetamax;
	private double dtheta;
	
	public InformationFileMaker(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
	}

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
			pw.println("#np 512");
			pw.println("##double tlen must be a power of 2/10, must be set");
			pw.println("#tlen 3276.8");
			pw.println("##polynomial structure file (can be blank)");
			pw.println("##if so or it doesn't exist model is an initial PREM");
			pw.println("#structureFile ");
			pw.println("##Boolean compute 6 green functions for the FP wavefield to use for joint structure-CMT inversion (false)");
			pw.println("#jointCMT ");
			pw.println("##Boolean wavefield catalogue (false)");
			pw.println("#catalogue");
			pw.println("##Catalogue distance range: thetamin thetamax dtheta");
			pw.println("#thetaRange");
		}
		System.err.println(outPath + " is created.");
	}
	
    /**
     * With static members, make two information files for back- and forward
     * propagation.
     *
     * @param args [parameter file name]
     * @throws IOException if any
     */
	public static void main(String[] args) throws Exception {
		InformationFileMaker ifm = new InformationFileMaker(Property.parse(args));
		long start = System.nanoTime();
		System.err.println(InformationFileMaker.class.getName() + " is going.");
		ifm.run();
		System.err.println(InformationFileMaker.class.getName() + " finished in "
				+ Utilities.toTimeString(System.nanoTime() - start));
	}
	
	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("tlen"))
			property.setProperty("tlen", "6553.6");
		if (!property.containsKey("np"))
			property.setProperty("np", "1024");
		if (!property.containsKey("header"))
			property.setProperty("header", "PREM");
		if (!property.containsKey("jointCMT"))
			property.setProperty("jointCMT", "false");
		if (!property.containsKey("catalogue"))
			property.setProperty("catalogue", "false");
		
		// additional info
		property.setProperty("CMTcatalogue=", GlobalCMTCatalog.getCatalogPath().toString());
	}
	
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
			ps = PolynomialStructure.PREM;
			if (psPath.toString().equals("PREM")) {
				ps = PolynomialStructure.PREM;
			}
			else if (psPath.toString().trim().equals("AK135")) {
				ps = PolynomialStructure.AK135;
			}
			else if (psPath.toString().trim().equals("AK135_elastic")) {
				ps = PolynomialStructure.AK135_elastic;
			}
			else
				ps = new PolynomialStructure(psPath);
		}
		else
			ps = PolynomialStructure.PREM;
		
		jointCMT = Boolean.parseBoolean(property.getProperty("jointCMT"));
		
		catalogue = Boolean.parseBoolean(property.getProperty("catalogue"));
		if (catalogue) {
			double[] tmpthetainfo = Stream.of(property.getProperty("thetaRange").trim().split("\\s+")).mapToDouble(Double::parseDouble)
					.toArray();
			thetamin = tmpthetainfo[0];
			thetamax = tmpthetainfo[1];
			dtheta = tmpthetainfo[2];
		}
	}

    /**
     * Reads a file describing locations
     * <p>
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
     * Creates files, horizontalPoint.inf and information perturbationPoint
     */
	private void createPointInformationFile() throws IOException {
		Path horizontalPointPath = outputPath.resolve("horizontalPoint.inf");
		Path perturbationPointPath = outputPath.resolve("perturbationPoint.inf");
		try (PrintWriter hpw = new PrintWriter(Files.newBufferedWriter(horizontalPointPath));
				PrintWriter ppw = new PrintWriter(Files.newBufferedWriter(perturbationPointPath))) {
			int figure = String.valueOf(perturbationPointPositions.length).length();
			for (int i = 0; i < perturbationPointPositions.length; i++) {
				hpw.println("XY" + String.format("%0" + figure + "d", i + 1) + " " + perturbationPointPositions[i]);
				for (double aPerturbationR : perturbationR)
					ppw.println(perturbationPointPositions[i] + " " + aPerturbationR);
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
		readParameterPointInformation();
		outputPath = workPath.resolve("threedPartial" + Utilities.getTemporaryString());
		Files.createDirectories(outputPath);

		if (property != null)
			writeProperties(outputPath.resolve("ifm.properties"));
		
		Path bpPath = outputPath.resolve("BPinfo");
		Path fpPath = outputPath.resolve("FPinfo");
		Path fpCatPath = outputPath.resolve("FPcat");
		Path bpCatPath = outputPath.resolve("BPcat");
		createPointInformationFile();
		
		//
		Set<EventFolder> eventDirs = Utilities.eventFolderSet(workPath);

		// reading station information
		Set<Station> stationSet = StationInformationFile.read(stationInformationPath);

		// System.exit(0);
		// //////////////////////////////////////
		System.out.println("making information files for the events(fp)");
		for (EventFolder ed : eventDirs) {
			GlobalCMTData ev;
			try {
				ev = ed.getGlobalCMTID().getEvent();
				
				// joint CMT inversion
				if (jointCMT) {
					int mtEXP = 25;
					double mw = 1.;
					MomentTensor[] mts = new MomentTensor[6];
					mts[0] = new MomentTensor(1., 0., 0., 0., 0., 0., mtEXP, mw);
					mts[1] = new MomentTensor(0., 1., 0., 0., 0., 0., mtEXP, mw);
					mts[2] = new MomentTensor(0., 0., 1., 0., 0., 0., mtEXP, mw);
					mts[3] = new MomentTensor(0., 0., 0., 1., 0., 0., mtEXP, mw);
					mts[4] = new MomentTensor(0., 0., 0., 0., 1., 0., mtEXP, mw);
					mts[5] = new MomentTensor(0., 0., 0., 0., 0., 1., mtEXP, mw);
					
					for (int i = 0; i < 6; i++) {
						ev.setCMT(mts[i]);
						FPinfo fp = new FPinfo(ev, header, ps, tlen, np, perturbationR, perturbationPointPositions);
						Path infPath = fpPath.resolve(ev.toString() + "_mt" + i);
						Files.createDirectories(infPath.resolve(header));
						fp.writeSHFP(infPath.resolve(header + "_SH.inf"));
						fp.writePSVFP(infPath.resolve(header + "_PSV.inf"));
					}
				}
				else {
					FPinfo fp = new FPinfo(ev, header, ps, tlen, np, perturbationR, perturbationPointPositions);
					Path infPath = fpPath.resolve(ev.toString());
					Files.createDirectories(infPath.resolve(header));
					fp.writeSHFP(infPath.resolve(header + "_SH.inf"));
					fp.writePSVFP(infPath.resolve(header + "_PSV.inf"));
					
					Path catInfPath = fpCatPath.resolve(ev.toString());
					Files.createDirectories(catInfPath.resolve(header));
					fp.writeSHFPCAT(catInfPath.resolve(header + "_SH.inf"), thetamin, thetamax, dtheta);
					fp.writePSVFPCAT(catInfPath.resolve(header + "_PSV.inf"), thetamin, thetamax, dtheta);
				}
			} catch (RuntimeException e) {
				System.err.println(e.getMessage());
			}
		}

		System.out.println("making information files for the stations(bp)");
		for (Station station : stationSet) {
			// System.out.println(str);
			BPinfo bp = new BPinfo(station, header, ps, tlen, np, perturbationR, perturbationPointPositions);
			Path infPath = bpPath.resolve("0000" + station);
			// infDir.mkdir();
			// System.out.println(infDir.getPath()+" was made");
			Files.createDirectories(infPath.resolve(header));
			bp.writeSHBP(infPath.resolve(header + "_SH.inf"));
			bp.writePSVBP(infPath.resolve(header + "_PSV.inf"));
		}
		BPinfo bp = new BPinfo(header, ps, tlen, np, perturbationR, perturbationPointPositions);
		Path catInfPath = bpCatPath;
		Files.createDirectories(catInfPath.resolve(header));
		bp.writeSHBPCat(catInfPath.resolve(header + "_SH.inf"), thetamin, thetamax, dtheta);
		bp.writePSVBPCat(catInfPath.resolve(header + "_PSV.inf"), thetamin, thetamax, dtheta);

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
