package manhattan.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import manhattan.globalcmt.GlobalCMTData;
import manhattan.inversion.StationInformationFile;
import manhattan.template.EventFolder;
import manhattan.template.HorizontalPosition;
import manhattan.template.Station;
import manhattan.template.Utilities;

/**
 * This class makes information files for <br>
 * SHBP SHFP PSVBP PSVFP
 * 
 * TODO information of eliminated stations and events
 * 
 * @version 0.1.0
 * 
 * @author Kensuke
 * 
 */
class InformationFileMaker extends parameter.InformationFileMaker {

	private InformationFileMaker(Path parameterPath) throws IOException {
		super(parameterPath);
	}

	/**
	 * locations of perturbation points
	 * 
	 */
	private HorizontalPosition[] perturbationPointPositions;

	/**
	 * 摂動を与える深さ the dadius of perturbation points default values are
	 * double[]{3505, 3555, 3605, 3655, 3705, 3755, 3805, 3855}
	 * 
	 */
	private double[] perturbationPointR;

	private Path outputPath;

	/**
	 * 
	 * With static members, make two information files for back- and forward
	 * propagation.
	 * 
	 * @param args
	 *            [parameter file name]
	 * 
	 */
	public static void main(String[] args) throws IOException {
		InformationFileMaker ifm = null;
		if (args.length != 0) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath)) 
				throw new NoSuchFileException(args[0]);
			ifm = new InformationFileMaker(parameterPath);
		} else
			ifm = new InformationFileMaker(null);
		// ///
		Path workPath = ifm.workPath;
		if (!Files.exists(workPath))
			throw new NoSuchFileException(ifm.workPath.toString());
		if (!Files.exists(ifm.stationInformationPath))
			throw new NoSuchFileException(ifm.stationInformationPath.toString());
		ifm.outputPath = workPath.resolve("threedPartial" + Utilities.getTemporaryString());
		Files.createDirectories(ifm.outputPath);

		Path bpPath = ifm.outputPath.resolve("BPinfo");
		Path fpPath = ifm.outputPath.resolve("FPinfo");
		// String model = ifm.model;
		PolynomialStructure ps = ifm.ps;
		// double[] perturbationPointR = ifm.perturbationPointR;
		// horizontalPointFile = new File(workDir, "horizontalPoint.inf");
		// perturbationPointFile = new File(workDir,"perturbationPoint.inf");
		ifm.readParameterPointInformation();
		ifm.createPointInformationFile();
		// System.exit(0);

		//
		Set<EventFolder> eventDirs = Utilities.eventFolderSet(workPath);

		// reading station information
		Set<Station> stationSet = StationInformationFile.read(ifm.stationInformationPath);

		// System.exit(0);
		// //////////////////////////////////////
		System.out.println("making information files for the events(fp)");
		for (EventFolder ed : eventDirs) {
			// System.out.println(ed);
			GlobalCMTData ev = ed.getGlobalCMTID().getEvent();
			FPinfo fp = new FPinfo(ev, ifm.header, ps, ifm.tlen, ifm.np);
			fp.setPerturbationPointR(ifm.perturbationPointR);
			fp.setPerturbationPoint(ifm.perturbationPointPositions);
			// File parentDir = new File(ifm.outputDir, "FPinfo");
			Path infPath = fpPath.resolve(ev.toString());
			// System.out.println(infDir);
			// infDir.mkdir();
			Files.createDirectories(infPath.resolve(ifm.header));
			fp.outputSHFP(infPath.resolve(ifm.header + "_SH.inf"));
			fp.outputPSVFP(infPath.resolve(ifm.header + "_PSV.inf"));
			//
		}

		System.out.println("making information files for the stations(bp)");
		for (Station station : stationSet) {
			String str = station.getStationName();
			// System.out.println(str);
			BPinfo bp = new BPinfo(station.getPosition(), ifm.header, ps, ifm.tlen, ifm.np);
			bp.setPerturbationPointR(ifm.perturbationPointR);
			bp.setPerturbationPoint(ifm.perturbationPointPositions);
			bp.setStaName(str);

			Path infPath = bpPath.resolve("0000" + str);
			// infDir.mkdir();
			// System.out.println(infDir.getPath()+" was made");
			Files.createDirectories(infPath.resolve(ifm.header));
			bp.outputSHBP(infPath.resolve(ifm.header + "_SH.inf"));
			bp.outputPSVBP(infPath.resolve(ifm.header + "_PSV.inf"));

		}

		// TODO
		if (fpPath.toFile().delete() && bpPath.toFile().delete()) {
			Files.delete(ifm.outputPath.resolve("horizontalPoint.inf"));
			Files.delete(ifm.outputPath.resolve("perturbationPoint.inf"));
			Files.delete(ifm.outputPath);
		} else {
			FileUtils.moveFileToDirectory(ifm.getParameterPath().toFile(), ifm.outputPath.toFile(), false);
			FileUtils.copyFileToDirectory(ifm.locationsPath.toFile(), ifm.outputPath.toFile(), false);
		}
		System.out.println("end.");

	}


	/**
	 * read a file describing locations
	 * 
	 * file should be as below: <br>
	 * r1 r2 r3..... rn <br>
	 * lat1 lon1<br>
	 * lat2 lon2<br>
	 * .<br>
	 * .<br>
	 * .<br>
	 * latm lonm
	 */
	private void readParameterPointInformation() throws IOException {
		InformationFileReader reader = new InformationFileReader(locationsPath);
		String line0 = reader.next();
		String[] parts0 = line0.split("\\s+");
		perturbationPointR = new double[parts0.length];
		for (int i = 0; i < perturbationPointR.length; i++)
			perturbationPointR[i] = Double.parseDouble(parts0[i]);

		List<HorizontalPosition> positionList = new ArrayList<>();
		String line = null;
		while ((line = reader.next()) != null) {
			String[] part = line.split("\\s+");
			HorizontalPosition position = new HorizontalPosition(Double.parseDouble(part[0]),
					Double.parseDouble(part[1]));
			positionList.add(position);
		}

		perturbationPointPositions = positionList.toArray(new HorizontalPosition[positionList.size()]);

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
				for (int j = 0; j < perturbationPointR.length; j++)
					ppw.println(perturbationPointPositions[i] + " " + perturbationPointR[j]);
			}
		}

	}

}
